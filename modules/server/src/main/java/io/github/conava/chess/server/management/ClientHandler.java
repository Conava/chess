package io.github.conava.chess.server.management;

import io.github.conava.chess.core.data.io.Message;
import io.github.conava.chess.core.data.io.MessageParser;
import io.github.conava.chess.core.data.io.MessageType;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import io.github.conava.chess.server.Server;
import io.github.conava.chess.server.auth.AuthService;
import io.github.conava.chess.server.matchmaking.MatchmakingService;
import io.github.conava.chess.server.persistence.GameRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * The ClientHandler class handles communication with a single connected client.
 *
 * <p>Each instance is created for one accepted TCP socket and is submitted to a thread pool
 * as a {@link Runnable}. It reads newline-delimited messages via {@link MessageParser#parse},
 * dispatches {@code LOGIN}, {@code REGISTER}, {@code CREATE_GAME}, and {@code JOIN_GAME}
 * locally, and forwards all other message types to the associated {@link GameInstance}.</p>
 *
 * <p>Auth gating: if the {@link #playerSession} field is {@code null} (unauthenticated state),
 * only {@code LOGIN} and {@code REGISTER} messages are accepted. All other message types
 * result in an {@code ERROR:Not authenticated} response sent to the client.</p>
 *
 * <p>Thread safety: {@link #sendMessage(Message)} is {@code synchronized} on this instance
 * so that the white-player thread and the black-player thread inside a {@link GameInstance}
 * can both safely call it without interleaving output on the underlying {@link PrintWriter}.</p>
 *
 * @since 0.9
 */
public class ClientHandler implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());

    private final Socket clientSocket;
    private final Server server;
    private final GameManager gameManager;
    private final AuthService authService;
    private final GameRepository gameRepository;

    /**
     * Matchmaking service reference, wired in via {@link #setMatchmakingService}.
     * Guarded by null-checks at all usage sites.
     */
    private MatchmakingService matchmakingService;

    /**
     * The authenticated session for the connected client. {@code null} means the client
     * has not yet completed a {@code LOGIN} or {@code REGISTER} handshake.
     */
    private volatile PlayerSession playerSession;

    private PrintWriter out;
    private GameInstance gameInstance;

    /**
     * Constructs a {@code ClientHandler} for the given client socket.
     *
     * <p>The handler stores references to the owning {@link Server} instance, the shared
     * {@link GameManager}, the {@link AuthService} for credential validation, and the
     * {@link GameRepository} for game history lookups. No I/O is performed in this
     * constructor; all network interaction begins in {@link #run()}.</p>
     *
     * @param clientSocket   the socket connected to the remote client; must not be {@code null}
     * @param server         the {@link Server} instance that owns the connection registry;
     *                       must not be {@code null}
     * @param gameManager    the shared {@link GameManager} that owns the semaphore, ID counter,
     *                       and games map; must not be {@code null}
     * @param authService    the {@link AuthService} used to authenticate clients; may be
     *                       {@code null} to disable authentication (all clients allowed)
     * @param gameRepository the {@link GameRepository} used for game-history lookups;
     *                       may be {@code null} to disable persistence
     */
    public ClientHandler(Socket clientSocket, Server server, GameManager gameManager,
                         AuthService authService, GameRepository gameRepository) {
        this.clientSocket = clientSocket;
        this.server = server;
        this.gameManager = gameManager;
        this.authService = authService;
        this.gameRepository = gameRepository;
    }

    /**
     * Wires in the {@link MatchmakingService} used to queue this client for automatic
     * game pairing.
     *
     * <p>Must be called before the client sends any {@code QUEUE} or {@code DEQUEUE}
     * messages. If not called (or called with {@code null}), those message types result
     * in an {@code ERROR:Matchmaking not available} response.</p>
     *
     * @param matchmakingService the matchmaking service instance; may be {@code null}
     *                           to disable matchmaking for this connection
     */
    public void setMatchmakingService(MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    /**
     * The run method listens for messages from the client and processes them.
     */
    @Override
    public void run() {
        LOGGER.info("Client connected: " + clientSocket.getInetAddress());
        server.addClientHandler(this);
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            processClientMessages(in);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error handling client connection", e);
        } finally {
            cleanup();
        }
    }

    /**
     * Processes messages from the client in a loop until the connection is closed.
     *
     * <p>Each line read from the client is parsed via {@link MessageParser#parse}. If
     * parsing fails for any reason, an {@code ERROR} message is sent to the client and
     * the loop continues so the handler thread does not crash on a single bad message.</p>
     *
     * @param in the {@link BufferedReader} wrapping the client's input stream
     * @throws IOException if an unrecoverable I/O error occurs on the underlying socket
     */
    private void processClientMessages(BufferedReader in) throws IOException {
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            Message message;
            try {
                message = MessageParser.parse(inputLine);
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "Failed to parse message from client: " + inputLine, e);
                sendMessage(new Message(MessageType.ERROR, "Malformed message: " + inputLine));
                continue;
            }
            LOGGER.info("Received message: " + message.type() + "\n" + message.content());
            handleMessage(message);
        }
        LOGGER.info("Connection closed by client " + clientSocket.getInetAddress());
    }

    /**
     * Dispatches a successfully parsed message to the appropriate handler.
     *
     * <p>Auth gating is enforced here: if {@link #playerSession} is {@code null}, only
     * {@code LOGIN} and {@code REGISTER} messages are passed through. All other types
     * result in an {@code ERROR:Not authenticated} response.</p>
     *
     * <p>{@code CREATE_GAME} and {@code JOIN_GAME} are handled locally; game-scoped types
     * ({@code CHAT}, {@code SAVE_GAME}) delegate to the active {@link GameInstance}.
     * {@code RESUME_GAME} queries the repository and returns game history without
     * re-joining a live game session. All other types are forwarded to the associated
     * {@link GameInstance}, or result in an error if no game session is active.</p>
     *
     * @param message the parsed message to dispatch; must not be {@code null}
     */
    private void handleMessage(Message message) {
        switch (message.type()) {
            case LOGIN    -> handleLogin(message);
            case REGISTER -> handleRegister(message);
            default -> {
                // Auth gate: all other message types require an authenticated session
                if (playerSession == null) {
                    sendMessage(new Message(MessageType.ERROR, "Not authenticated"));
                    return;
                }
                dispatchAuthenticatedMessage(message);
            }
        }
    }

    /**
     * Dispatches a message from an authenticated client to the appropriate handler.
     *
     * @param message the authenticated client's message; must not be {@code null}
     */
    private void dispatchAuthenticatedMessage(Message message) {
        switch (message.type()) {
            case CREATE_GAME  -> createGame(message);
            case JOIN_GAME    -> joinGame(message);
            case CHAT         -> {
                if (gameInstance != null) {
                    gameInstance.handleChat(this, message);
                } else {
                    sendMessage(new Message(MessageType.ERROR, "No game instance available"));
                }
            }
            case SAVE_GAME    -> {
                if (gameInstance != null) {
                    gameInstance.handleSaveGame(this, message);
                } else {
                    sendMessage(new Message(MessageType.ERROR, "No game instance available"));
                }
            }
            case RESUME_GAME  -> handleResumeGame(message);
            case QUEUE        -> {
                if (matchmakingService != null) {
                    RulesetOptions ruleset;
                    try {
                        ruleset = RulesetOptions.valueOf(message.getParameterValue("ruleset"));
                    } catch (IllegalArgumentException | NullPointerException e) {
                        sendMessage(new Message(MessageType.ERROR, "Invalid or missing ruleset"));
                        return;
                    }
                    matchmakingService.enqueue(this, playerSession, ruleset);
                } else {
                    sendMessage(new Message(MessageType.ERROR, "Matchmaking not available"));
                }
            }
            case DEQUEUE      -> {
                if (matchmakingService != null) {
                    matchmakingService.dequeue(this);
                } else {
                    sendMessage(new Message(MessageType.ERROR, "Matchmaking not available"));
                }
            }
            default -> {
                if (gameInstance != null) {
                    gameInstance.processMessage(this, message);
                } else {
                    sendMessage(new Message(MessageType.ERROR, "No game instance available"));
                }
            }
        }
    }

    // ── Auth handlers ─────────────────────────────────────────────────────────

    /**
     * Handles a {@code LOGIN} message: parses {@code username} and {@code password} from
     * the message content, calls {@link AuthService#login}, then verifies the returned
     * token to populate the {@link #playerSession}.
     *
     * <p>On success, sends {@code AUTH_TOKEN:token=<token> userId=<userId>} to the client.
     * On {@link IllegalArgumentException} (wrong credentials), sends
     * {@code ERROR:<exception message>}.</p>
     *
     * @param message the {@code LOGIN} message; must not be {@code null}
     */
    private void handleLogin(Message message) {
        if (authService == null) {
            sendMessage(new Message(MessageType.ERROR, "Authentication service not available"));
            return;
        }
        String username = message.getParameterValue("username");
        String password = message.getParameterValue("password");
        try {
            String token = authService.login(username, password);
            Optional<PlayerSession> sessionOpt = authService.verifyToken(token);
            if (sessionOpt.isEmpty()) {
                sendMessage(new Message(MessageType.ERROR, "Login succeeded but session could not be verified"));
                return;
            }
            playerSession = sessionOpt.get();
            String content = "token=" + token + " userId=" + playerSession.getUserId();
            sendMessage(new Message(MessageType.AUTH_TOKEN, content));
        } catch (IllegalArgumentException e) {
            sendMessage(new Message(MessageType.ERROR, e.getMessage()));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error during login", e);
            sendMessage(new Message(MessageType.ERROR, "Internal server error during login"));
        }
    }

    /**
     * Handles a {@code REGISTER} message: parses {@code username} and {@code password}
     * from the message content, calls {@link AuthService#register}, then verifies the
     * returned token to populate the {@link #playerSession}.
     *
     * <p>On success, sends {@code AUTH_TOKEN:token=<token> userId=<userId>} to the client.
     * On {@link IllegalArgumentException} (validation failure or duplicate username),
     * sends {@code ERROR:<exception message>}.</p>
     *
     * @param message the {@code REGISTER} message; must not be {@code null}
     */
    private void handleRegister(Message message) {
        if (authService == null) {
            sendMessage(new Message(MessageType.ERROR, "Authentication service not available"));
            return;
        }
        String username = message.getParameterValue("username");
        String password = message.getParameterValue("password");
        try {
            String token = authService.register(username, password);
            Optional<PlayerSession> sessionOpt = authService.verifyToken(token);
            if (sessionOpt.isEmpty()) {
                sendMessage(new Message(MessageType.ERROR, "Registration succeeded but session could not be verified"));
                return;
            }
            playerSession = sessionOpt.get();
            String content = "token=" + token + " userId=" + playerSession.getUserId();
            sendMessage(new Message(MessageType.AUTH_TOKEN, content));
        } catch (IllegalArgumentException e) {
            sendMessage(new Message(MessageType.ERROR, e.getMessage()));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error during registration", e);
            sendMessage(new Message(MessageType.ERROR, "Internal server error during registration"));
        }
    }

    // ── Game management handlers ───────────────────────────────────────────────

    /**
     * Handles a {@code RESUME_GAME} message: parses {@code gameId}, fetches the game
     * record and its move list from the repository, then sends a {@code GAME_HISTORY}
     * response with the move list serialized as a pipe-delimited string.
     *
     * <p>If no {@link GameRepository} is configured, or the game is not found, an
     * {@code ERROR} is returned.</p>
     *
     * @param message the {@code RESUME_GAME} message; must not be {@code null}
     */
    private void handleResumeGame(Message message) {
        if (gameRepository == null) {
            sendMessage(new Message(MessageType.ERROR, "Game history not available"));
            return;
        }
        String gameIdParam = message.getParameterValue("gameId");
        int gameId;
        try {
            gameId = Integer.parseInt(gameIdParam);
        } catch (NumberFormatException | NullPointerException e) {
            sendMessage(new Message(MessageType.ERROR, "Invalid or missing gameId"));
            return;
        }
        try {
            var gameOpt = gameRepository.findGameById(gameId);
            if (gameOpt.isEmpty()) {
                sendMessage(new Message(MessageType.ERROR, "Game not found: " + gameId));
                return;
            }
            List<GameRepository.MoveRecord> moves = gameRepository.getMoves(gameId);
            String serializedMoves = moves.stream()
                    .map(GameRepository.MoveRecord::notation)
                    .collect(Collectors.joining("|"));
            String content = "gameId=" + gameId + " moves=" + serializedMoves;
            sendMessage(new Message(MessageType.GAME_HISTORY, content));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error during RESUME_GAME", e);
            sendMessage(new Message(MessageType.ERROR, "Internal server error during resume"));
        }
    }

    /**
     * Handles a {@code CREATE_GAME} message: acquires a game slot, creates a new
     * {@link GameInstance}, and connects the requesting client as the white player.
     *
     * <p>The message content may include a {@code playerName} key-value parameter. If
     * the parameter is absent or blank, the default name {@code "Player 1"} is used.
     * The {@code ruleset} parameter is mandatory; if it is absent or not a valid
     * {@link RulesetOptions} name an {@code ERROR} message is sent to the client.</p>
     *
     * <p>If no game slot is available (semaphore exhausted), an {@code ERROR} message is
     * returned and no game is created.</p>
     *
     * @param message the {@code CREATE_GAME} message; must not be {@code null}
     */
    private void createGame(Message message) {
        if (!gameManager.tryAcquireGameSlot()) {
            sendMessage(new Message(MessageType.ERROR, "Failed to create game: server is full"));
            return;
        }
        RulesetOptions ruleset;
        try {
            ruleset = RulesetOptions.valueOf(message.getParameterValue("ruleset"));
        } catch (IllegalArgumentException | NullPointerException e) {
            gameManager.releaseGameSlot();
            LOGGER.log(Level.WARNING, "Invalid or missing ruleset in CREATE_GAME: " + message.content(), e);
            sendMessage(new Message(MessageType.ERROR, "Invalid or missing ruleset"));
            return;
        }
        String playerName = message.getParameterValue("playerName");
        if (playerName == null || playerName.isBlank()) {
            playerName = (playerSession != null && playerSession.getUsername() != null
                    && !playerSession.getUsername().isBlank())
                    ? playerSession.getUsername() : "Player 1";
        }
        int gameId = gameManager.nextGameId();
        gameInstance = new GameInstance(gameId, ruleset);
        gameInstance.connectPlayer(this, playerName);
        gameManager.addGame(gameId, gameInstance);
        String joinCodeContent = "joinCode=" + gameId;
        if (gameInstance.getPositionIndex() >= 0) {
            joinCodeContent += " position=" + gameInstance.getPositionIndex() + " ruleset=CHESS960";
        }
        sendMessage(new Message(MessageType.JOIN_CODE, joinCodeContent));
    }

    /**
     * Handles a {@code JOIN_GAME} message: locates an existing {@link GameInstance} by
     * its game ID and connects the requesting client as the black player.
     *
     * <p>The message content must include a {@code gameId} key-value parameter containing
     * the numeric game identifier (as returned in the {@code JOIN_CODE} message sent to
     * the game creator). An optional {@code playerName} parameter names the joining
     * player; if absent or blank, {@code "Player 2"} is used as a default.</p>
     *
     * <p>An {@code ERROR} message is sent if the {@code gameId} parameter is missing,
     * non-numeric, or does not match any active game.</p>
     *
     * @param message the {@code JOIN_GAME} message; must not be {@code null}
     */
    private void joinGame(Message message) {
        String gameIdParam = message.getParameterValue("gameId");
        int gameId;
        try {
            gameId = Integer.parseInt(gameIdParam);
        } catch (NumberFormatException | NullPointerException e) {
            LOGGER.log(Level.WARNING, "Invalid or missing gameId in JOIN_GAME: " + message.content(), e);
            sendMessage(new Message(MessageType.ERROR, "Invalid or missing gameId"));
            return;
        }
        GameInstance foundGame = gameManager.getGame(gameId);
        if (foundGame == null) {
            sendMessage(new Message(MessageType.ERROR, "Invalid join code"));
            return;
        }
        String playerName = message.getParameterValue("playerName");
        if (playerName == null || playerName.isBlank()) {
            playerName = (playerSession != null && playerSession.getUsername() != null
                    && !playerSession.getUsername().isBlank())
                    ? playerSession.getUsername() : "Player 2";
        }
        foundGame.connectPlayer(this, playerName);
        this.gameInstance = foundGame;
    }

    // ── Output ────────────────────────────────────────────────────────────────

    /**
     * Sends a serialized message to the connected client.
     *
     * <p>This method is {@code synchronized} on this {@code ClientHandler} instance.
     * Synchronization is required because both the client-reader thread (this handler's
     * own thread) and the opposing player's {@link ClientHandler} thread may call
     * {@code sendMessage} concurrently via {@link GameInstance#sendMessageToPlayers}.
     * {@link PrintWriter} provides no thread-safety guarantees; this lock prevents
     * interleaved output.</p>
     *
     * <p>If the output stream has not yet been initialised (e.g., before {@link #run()}
     * opens the socket), the call is a no-op and a warning is logged.</p>
     *
     * @param message the message to send; must not be {@code null}
     */
    public synchronized void sendMessage(Message message) {
        if (out != null) {
            out.println(MessageParser.serialize(message));
        } else {
            LOGGER.log(Level.WARNING, "Output stream is not initialized");
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Releases the game slot, removes this client from any matchmaking queue, and cleans
     * up resources on client disconnect.
     *
     * <p>Always called from the {@code finally} block in {@link #run()} so that
     * connection registration and game resources are reclaimed even on error paths.</p>
     */
    private void cleanup() {
        LOGGER.log(Level.INFO, "Client disconnected: " + clientSocket.getInetAddress());
        server.removeClientHandler(this);
        if (matchmakingService != null) {
            matchmakingService.dequeue(this);
        }
        releaseGameSlot();
    }

    /**
     * Releases the game slot if a game instance exists.
     *
     * <p>Before removing the game from the active games map and releasing the semaphore
     * permit, {@link GameInstance#disconnectPlayer(ClientHandler)} is called so that the
     * remaining connected player is notified of the disconnection and the observer
     * registration is cleaned up to prevent memory leaks.</p>
     */
    public void releaseGameSlot() {
        GameInstance instance = this.gameInstance;
        if (instance == null) {
            return;
        }
        this.gameInstance = null;
        instance.disconnectPlayer(this);
        Integer gameId = instance.getGameId();
        gameManager.removeGame(gameId);
        gameManager.releaseGameSlot();
    }
}
