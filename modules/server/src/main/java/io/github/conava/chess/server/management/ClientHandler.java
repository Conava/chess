package io.github.conava.chess.server.management;

import io.github.conava.chess.core.data.io.Message;
import io.github.conava.chess.core.data.io.MessageParser;
import io.github.conava.chess.core.data.io.MessageType;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import io.github.conava.chess.server.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The ClientHandler class handles communication with a single connected client.
 *
 * <p>Each instance is created for one accepted TCP socket and is submitted to a thread pool
 * as a {@link Runnable}. It reads newline-delimited messages via {@link MessageParser#parse},
 * dispatches {@code CREATE_GAME} and {@code JOIN_GAME} locally, and forwards all other
 * message types to the associated {@link GameInstance}.</p>
 *
 * <p>Thread safety: {@link #sendMessage(Message)} is {@code synchronized} on this instance
 * so that the white-player thread and the black-player thread inside a {@link GameInstance}
 * can both safely call it without interleaving output on the underlying {@link PrintWriter}.</p>
 */
public class ClientHandler implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());
    private final Socket clientSocket;
    private final Server server;
    private PrintWriter out;
    private GameInstance gameInstance;

    /**
     * Constructs a {@code ClientHandler} for the given client socket.
     *
     * <p>The handler stores a reference to the owning {@link Server} instance so it can
     * call {@link Server#addClientHandler}/{@link Server#removeClientHandler} and access
     * shared state (games map, semaphore, ID counter) via the server's getter methods.
     * No I/O is performed in this constructor; all network interaction begins in
     * {@link #run()}.</p>
     *
     * @param clientSocket the socket connected to the remote client; must not be {@code null}
     * @param server       the {@link Server} instance that owns the shared game state and
     *                     connection registry; must not be {@code null}
     */
    public ClientHandler(Socket clientSocket, Server server) {
        this.clientSocket = clientSocket;
        this.server = server;
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
     * <p>Uses an enhanced switch expression: {@code CREATE_GAME} and {@code JOIN_GAME}
     * are handled locally; all other types are forwarded to the associated
     * {@link GameInstance}, or result in an error if no game session is active.</p>
     *
     * @param message the parsed message to dispatch; must not be {@code null}
     */
    private void handleMessage(Message message) {
        switch (message.type()) {
            case CREATE_GAME -> createGame(message);
            case JOIN_GAME -> joinGame(message);
            default -> {
                if (gameInstance != null) {
                    gameInstance.processMessage(this, message);
                } else {
                    sendMessage(new Message(MessageType.ERROR, "No game instance available"));
                }
            }
        }
    }

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
        if (!server.getGameSemaphore().tryAcquire()) {
            sendMessage(new Message(MessageType.ERROR, "Failed to create game: server is full"));
            return;
        }
        RulesetOptions ruleset;
        try {
            ruleset = RulesetOptions.valueOf(message.getParameterValue("ruleset"));
        } catch (IllegalArgumentException | NullPointerException e) {
            server.getGameSemaphore().release();
            LOGGER.log(Level.WARNING, "Invalid or missing ruleset in CREATE_GAME: " + message.content(), e);
            sendMessage(new Message(MessageType.ERROR, "Invalid or missing ruleset"));
            return;
        }
        String playerName = message.getParameterValue("playerName");
        if (playerName == null || playerName.isBlank()) {
            playerName = "Player 1";
        }
        int gameId = server.getGameIdCounter().incrementAndGet();
        gameInstance = new GameInstance(gameId, ruleset);
        gameInstance.connectPlayer(this, playerName);
        server.getGamesList().put(gameId, gameInstance);
        sendMessage(new Message(MessageType.JOIN_CODE, "joinCode=" + gameId));
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
        GameInstance foundGame = server.getGamesList().get(gameId);
        if (foundGame == null) {
            sendMessage(new Message(MessageType.ERROR, "Invalid join code"));
            return;
        }
        String playerName = message.getParameterValue("playerName");
        if (playerName == null || playerName.isBlank()) {
            playerName = "Player 2";
        }
        foundGame.connectPlayer(this, playerName);
        this.gameInstance = foundGame;
    }

    /**
     * Releases the game slot and cleans up resources.
     */
    private void cleanup() {
        LOGGER.log(Level.INFO, "Client disconnected: " + clientSocket.getInetAddress());
        server.removeClientHandler(this);
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
        if (gameInstance != null) {
            gameInstance.disconnectPlayer(this);
            Integer gameId = gameInstance.getGameId();
            server.getGamesList().remove(gameId);
            server.getGameSemaphore().release();
        }
    }
}
