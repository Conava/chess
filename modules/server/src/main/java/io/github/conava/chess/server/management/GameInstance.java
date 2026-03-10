package io.github.conava.chess.server.management;

import io.github.conava.chess.core.data.io.Message;
import io.github.conava.chess.core.data.io.MessageType;
import io.github.conava.chess.core.exceptions.IllegalMoveException;
import io.github.conava.chess.core.logic.game.Game;
import io.github.conava.chess.core.logic.game.GameState;
import io.github.conava.chess.core.logic.moves.Move;
import io.github.conava.chess.core.logic.moves.PromotionMove;
import io.github.conava.chess.core.logic.observer.GameObserver;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import io.github.conava.chess.core.logic.ruleset.chess960Ruleset.Chess960Ruleset;
import io.github.conava.chess.server.persistence.GameRepository;

import java.sql.SQLException;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a single server-side game session, managing two player slots (white and black),
 * deferred game creation, in-game message routing, disconnect/reconnect handling, mutual
 * save-for-later, and chat relay.
 *
 * <p>Game creation is deferred until both players have connected via
 * {@link #connectPlayer(ClientHandler, String)} or
 * {@link #connectPlayer(ClientHandler, PlayerSession)}. Until that point the internal
 * {@link Game} reference is {@code null} and move messages are silently ignored.</p>
 *
 * <p>This class implements {@link GameObserver} to receive state-change notifications from
 * the underlying {@link Game}. The observer is used exclusively for terminal state transitions.
 * Move relay is handled explicitly in {@link #handleMove(ClientHandler, Message)}.</p>
 *
 * <p>Both {@link #connectPlayer(ClientHandler, String)} and
 * {@link #processMessage(ClientHandler, Message)} are {@code synchronized} on this instance
 * to prevent race conditions between the two player threads sharing the same game.</p>
 *
 * <p>When a player disconnects during a non-terminal game, the game is set to
 * {@link GameState#PAUSED} rather than awarding a resignation. A disconnect timer is
 * started; if the disconnected player does not reconnect within
 * {@code disconnectTimeoutSeconds} seconds, the remaining player wins by timeout.</p>
 *
 * <p>If a {@link GameRepository} is provided (non-null), all significant state changes
 * (game creation, moves, chat, disconnect, reconnect, save) are persisted. When
 * {@code gameRepository} is {@code null} all persistence calls are silently skipped,
 * which is the default for the legacy no-args constructor used in tests.</p>
 *
 * @since 0.9
 */
public class GameInstance implements GameObserver {

    private static final Logger LOGGER = Logger.getLogger(GameInstance.class.getName());

    /** Maximum allowed length for a chat message content string. */
    private static final int MAX_CHAT_LENGTH = 500;

    private final int gameId;
    private final RulesetOptions ruleset;

    /**
     * Scharnagl index for Chess960 games; {@code -1} for standard chess.
     */
    private final int positionIndex;

    /**
     * Optional persistence repository. {@code null} when no DB backing is configured.
     */
    private final GameRepository gameRepository;

    /**
     * The database primary key for the persisted game row, or {@code -1} when no
     * repository is available.
     */
    private final int dbGameId;

    /**
     * Seconds before an absent player forfeits the game. Only used when
     * {@code gameRepository} is non-null.
     */
    private final int disconnectTimeoutSeconds;

    private Game game;
    private GameState previousState;

    private String whitePlayerName;
    private String blackPlayerName;

    private ClientHandler whitePlayerHandler;
    private ClientHandler blackPlayerHandler;

    /** Session for the white player; may be {@code null} when no auth context is available. */
    private PlayerSession whitePlayerSession;

    /** Session for the black player; may be {@code null} when no auth context is available. */
    private PlayerSession blackPlayerSession;

    /** Sequential move counter (1-based). Incremented on each successfully persisted move. */
    private int moveNo;

    /** Whether the white player has requested a mutual save. */
    private boolean whiteSaveRequested;

    /** Whether the black player has requested a mutual save. */
    private boolean blackSaveRequested;

    /** Lazy-initialised scheduler used for the disconnect timeout. */
    private ScheduledExecutorService scheduler;

    /** Cancellable handle for the active disconnect countdown timer. */
    private ScheduledFuture<?> disconnectTimer;

    // ── Constructors ──────────────────────────────────────────────────────────

    /**
     * Constructs a new {@code GameInstance} in the {@code WAITING_FOR_PLAYER} state,
     * without persistence.
     *
     * <p>This constructor keeps the existing two-argument signature for backward
     * compatibility with tests and callers that do not require DB integration.
     * All persistence operations are skipped when using this constructor.</p>
     *
     * @param gameId  the unique numeric identifier for this game session
     * @param ruleset the ruleset variant to use when the game is eventually created
     */
    public GameInstance(int gameId, RulesetOptions ruleset) {
        this(gameId, ruleset, null, null, 0, -1);
    }

    /**
     * Constructs a new {@code GameInstance} with full persistence and disconnect-timeout
     * support.
     *
     * <p>No {@link Game} object is created at construction time. Game creation is deferred
     * until both players have connected so that both player names are available before the
     * {@link Game} constructor runs.</p>
     *
     * @param gameId                   the unique numeric identifier for this game session
     * @param ruleset                  the ruleset variant to use when the game is eventually created
     * @param gameRepository           the persistence repository; {@code null} disables all DB calls
     * @param gameManager              the server-wide game manager (reserved for future use); may be {@code null}
     * @param disconnectTimeoutSeconds seconds before an absent player forfeits; ignored when
     *                                 {@code gameRepository} is {@code null}
     * @param dbGameId                 the database primary key for the game row, or {@code -1}
     *                                 when {@code gameRepository} is {@code null}
     */
    /**
     * Stored reference to the server-wide {@link GameManager}; may be {@code null}.
     * Reserved for future use (e.g. removing the game from the active map on save/end).
     */
    private final GameManager gameManager;

    public GameInstance(int gameId, RulesetOptions ruleset, GameRepository gameRepository,
                        GameManager gameManager, int disconnectTimeoutSeconds, int dbGameId) {
        this.gameId = gameId;
        this.ruleset = ruleset;
        this.positionIndex = (ruleset == RulesetOptions.CHESS960) ? new Random().nextInt(960) : -1;
        this.game = null;
        this.previousState = null;
        this.gameRepository = gameRepository;
        this.gameManager = gameManager;
        this.disconnectTimeoutSeconds = disconnectTimeoutSeconds;
        this.dbGameId = dbGameId;
        this.moveNo = 0;
        this.whiteSaveRequested = false;
        this.blackSaveRequested = false;
    }

    // ── Player connection ─────────────────────────────────────────────────────

    /**
     * Connects a player using a name string only (no session context).
     *
     * <p>The first call fills the white (creator) slot; the second call fills the black
     * (joiner) slot and starts the game.</p>
     *
     * @param clientHandler the handler for the connecting player; must not be {@code null}
     * @param playerName    display name; a blank or {@code null} value substitutes a default
     */
    public synchronized void connectPlayer(ClientHandler clientHandler, String playerName) {
        if (whitePlayerHandler == null) {
            whitePlayerHandler = clientHandler;
            whitePlayerName = (playerName != null && !playerName.isBlank()) ? playerName : "Player 1";
            String whiteSuccessContent = (positionIndex >= 0) ? "player=white position=" + positionIndex + " ruleset=CHESS960" : "player=white";
            clientHandler.sendMessage(new Message(MessageType.SUCCESS, whiteSuccessContent));
        } else if (blackPlayerHandler == null) {
            blackPlayerHandler = clientHandler;
            blackPlayerName = (playerName != null && !playerName.isBlank()) ? playerName : "Player 2";
            String successContent = (positionIndex >= 0) ? "player=black position=" + positionIndex + " ruleset=CHESS960" : "player=black";
            clientHandler.sendMessage(new Message(MessageType.SUCCESS, successContent));
            startGame();
        }
    }

    /**
     * Connects a player using a {@link PlayerSession} for identity and DB operations.
     *
     * <p>The username from the session is used as the display name. If the session is
     * {@code null} or has a blank username a default name is substituted.</p>
     *
     * <p>The first call fills the white slot; the second call fills the black slot and
     * creates the game. When both sessions are available and a {@link GameRepository} is
     * configured, the game row is created in the database on the second call.</p>
     *
     * @param clientHandler the handler for the connecting player; must not be {@code null}
     * @param session       the authenticated session for the connecting player; may be {@code null}
     */
    public synchronized void connectPlayer(ClientHandler clientHandler, PlayerSession session) {
        String playerName = (session != null && session.getUsername() != null && !session.getUsername().isBlank())
                ? session.getUsername() : null;
        if (whitePlayerHandler == null) {
            whitePlayerHandler = clientHandler;
            whitePlayerSession = session;
            whitePlayerName = (playerName != null) ? playerName : "Player 1";
            String whiteSuccessContent = (positionIndex >= 0) ? "player=white position=" + positionIndex + " ruleset=CHESS960" : "player=white";
            clientHandler.sendMessage(new Message(MessageType.SUCCESS, whiteSuccessContent));
        } else if (blackPlayerHandler == null) {
            blackPlayerHandler = clientHandler;
            blackPlayerSession = session;
            blackPlayerName = (playerName != null) ? playerName : "Player 2";
            String successContent = (positionIndex >= 0) ? "player=black position=" + positionIndex + " ruleset=CHESS960" : "player=black";
            clientHandler.sendMessage(new Message(MessageType.SUCCESS, successContent));
            startGame();
        }
    }

    // ── Game lifecycle ────────────────────────────────────────────────────────

    /**
     * Creates the {@link Game} via the factory, registers this instance as observer, and
     * notifies both players that the game is now {@code RUNNING}.
     */
    private void startGame() {
        if (positionIndex >= 0) {
            this.game = Game.createServerGame(new Chess960Ruleset(positionIndex), whitePlayerName, blackPlayerName);
        } else {
            this.game = Game.createServerGame(ruleset, whitePlayerName, blackPlayerName);
        }
        this.game.addObserver(this);
        this.game.startGame();
        this.previousState = this.game.getState();
        sendMessageToPlayers(new Message(MessageType.GAME_STATUS, "gameState=RUNNING"));
    }

    // ── GameObserver ──────────────────────────────────────────────────────────

    /**
     * Observer callback invoked by the underlying {@link Game} whenever its state changes.
     *
     * <p>Handles terminal state transitions only. If the new game state is terminal and
     * differs from the previously observed state, a {@code GAME_STATUS} message is sent
     * to both connected players.</p>
     */
    @Override
    public void onGameStateChanged() {
        if (game == null) {
            return;
        }
        GameState currentState = game.getState();
        if (currentState != previousState && isTerminalState(currentState)) {
            sendMessageToPlayers(new Message(MessageType.GAME_STATUS, "gameState=" + currentState.name()));
        }
        previousState = currentState;
    }

    /**
     * Returns {@code true} if the given {@link GameState} represents a terminal game outcome.
     *
     * <p>Note: {@link GameState#PAUSED} and {@link GameState#SAVED} are explicitly NOT
     * terminal — they represent suspended states from which play can resume.</p>
     *
     * @param state the state to test; must not be {@code null}
     * @return {@code true} if {@code state} is a terminal state, {@code false} otherwise
     */
    private boolean isTerminalState(GameState state) {
        return switch (state) {
            case WHITE_WON_BY_CHECKMATE, BLACK_WON_BY_CHECKMATE, WHITE_WON_BY_RESIGNATION, BLACK_WON_BY_RESIGNATION,
                 WHITE_WON_BY_TIMEOUT, BLACK_WON_BY_TIMEOUT, DRAW_BY_STALEMATE, DRAW_BY_INSUFFICIENT_MATERIAL,
                 DRAW_BY_THREEFOLD_REPETITION, DRAW_BY_FIFTY_MOVE_RULE -> true;
            default -> false;
        };
    }

    // ── Message dispatch ──────────────────────────────────────────────────────

    /**
     * Dispatches an in-game message from a client to the appropriate handler.
     *
     * <p>This method is {@code synchronized} on this instance so that concurrent messages
     * from both player threads are processed serially, preventing race conditions on the
     * underlying {@link Game} object.</p>
     *
     * @param clientHandler the handler that sent the message
     * @param message       the message to process; must not be {@code null}
     */
    public synchronized void processMessage(ClientHandler clientHandler, Message message) {
        switch (message.type()) {
            case JOIN_CODE -> handleJoinCode(message);
            case MOVE -> handleMove(clientHandler, message);
            case GAME_STATUS -> handleGameStatus(clientHandler, message);
            case CHAT -> handleChat(clientHandler, message);
            case SAVE_GAME -> handleSaveGame(clientHandler, message);
            case SUCCESS -> handleSuccess(message);
            case ERROR -> handleError(message);
            case FAILURE -> handleFailure(message);
            default -> LOGGER.log(Level.WARNING, "Unsupported message type: {0}", message.type());
        }
    }

    // ── Handlers ─────────────────────────────────────────────────────────────

    /**
     * Handles a {@code JOIN_CODE} message by logging its content.
     *
     * @param message the message containing the join code
     */
    private void handleJoinCode(Message message) {
        LOGGER.log(Level.INFO, "Join code received: {0}", message.content());
    }

    /**
     * Handles a {@code MOVE} message: parses the move, executes it on the game, then
     * relays the normalized protocol string to both players on success, or sends an
     * {@code ERROR} to the sender on failure.
     *
     * <p>For {@link PromotionMove} instances, {@code game.promoteMove()} is called instead
     * of {@code game.movePiece()} so the pawn is promoted to the correct piece type.</p>
     *
     * <p>After a successful move the serialized form is persisted via the repository (if
     * available) and the move counter is incremented.</p>
     *
     * @param clientHandler the handler that sent the move
     * @param message       the message carrying the move in {@code move=<notation> playerColor=<COLOR>} format
     */
    private void handleMove(ClientHandler clientHandler, Message message) {
        if (game == null) {
            LOGGER.log(Level.WARNING, "MOVE received but game not yet created (gameId={0})", gameId);
            return;
        }
        try {
            var player = Objects.equals(message.getParameterValue("playerColor"), "WHITE") ? game.getPlayerWhite() : game.getPlayerBlack();
            Move move = game.getRuleset().deserializeMove(Objects.requireNonNull(message.getParameterValue("move")), game.getBoard(), player);

            if (move instanceof PromotionMove pm) {
                game.promoteMove(pm.getStart(), pm.getEnd(), pm.getTargetPiece().getType());
            } else {
                game.movePiece(move.getStart(), move.getEnd());
            }

            String playerColor = message.getParameterValue("playerColor");
            String relayContent = "move=" + move.toProtocolString() + " playerColor=" + playerColor;
            LOGGER.log(Level.INFO, "Move executed: {0}", relayContent);
            sendMessageToPlayers(new Message(MessageType.MOVE, relayContent));

            // Persist the move if a repository is available
            if (gameRepository != null && dbGameId > 0) {
                moveNo++;
                try {
                    gameRepository.addMove(dbGameId, moveNo, move.toProtocolString());
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Failed to persist move " + moveNo, e);
                }
            }
        } catch (IllegalMoveException e) {
            LOGGER.log(Level.WARNING, "Illegal move received: {0}", message.content());
            if (clientHandler != null) {
                clientHandler.sendMessage(new Message(MessageType.ERROR, "Illegal move: " + message.content()));
            }
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Failed to parse move: " + message.content(), e);
            if (clientHandler != null) {
                clientHandler.sendMessage(new Message(MessageType.ERROR, "Malformed move: " + message.content()));
            }
        }
    }

    /**
     * Handles a {@code GAME_STATUS} message from a client.
     *
     * <p>Non-empty content is treated as a resignation request. If the body is empty,
     * the current game state is returned to the requesting client.</p>
     *
     * @param clientHandler the handler requesting status or submitting a resignation
     * @param message       the {@code GAME_STATUS} message
     */
    private void handleGameStatus(ClientHandler clientHandler, Message message) {
        if (game == null) {
            LOGGER.log(Level.WARNING, "GAME_STATUS received but game not yet created (gameId={0})", gameId);
            return;
        }
        if (!message.content().isEmpty()) {
            LOGGER.log(Level.INFO, "Game status update from player: {0}", message.content());
            try {
                String gameStateParam = message.getParameterValue("gameState");
                GameState newGameState = GameState.valueOf(gameStateParam);
                if (clientHandler == whitePlayerHandler && newGameState == GameState.BLACK_WON_BY_RESIGNATION) {
                    game.setGameState(newGameState);
                } else if (clientHandler == blackPlayerHandler && newGameState == GameState.WHITE_WON_BY_RESIGNATION) {
                    game.setGameState(newGameState);
                }
            } catch (IllegalArgumentException | NullPointerException e) {
                LOGGER.log(Level.WARNING, "Invalid gameState parameter in GAME_STATUS message: " + message.content(), e);
                if (clientHandler != null) {
                    clientHandler.sendMessage(new Message(MessageType.ERROR, "Malformed gameState parameter: " + message.getParameterValue("gameState")));
                }
            }
        } else {
            LOGGER.log(Level.INFO, "Current game status requested");
            clientHandler.sendMessage(new Message(MessageType.GAME_STATUS, "gameState=" + game.getState()));
        }
    }

    /**
     * Handles a {@code CHAT} message: sanitizes the content, prepends the sender's
     * username as {@code from=<username>}, persists the message via the repository (if
     * available), and relays the result to both players.
     *
     * <p>Newlines are stripped from the content to prevent protocol framing attacks.
     * Content is truncated to {@value #MAX_CHAT_LENGTH} characters.</p>
     *
     * @param clientHandler the handler that sent the chat message
     * @param message       the incoming {@code CHAT} message
     */
    private void handleChat(ClientHandler clientHandler, Message message) {
        if (game == null) {
            return;
        }

        // Sanitize: strip newlines, truncate
        String rawContent = message.content().replace("\n", "").replace("\r", "");
        if (rawContent.length() > MAX_CHAT_LENGTH) {
            rawContent = rawContent.substring(0, MAX_CHAT_LENGTH);
        }

        // Identify sender username
        String senderUsername;
        int senderUserId = 0;
        if (clientHandler == whitePlayerHandler) {
            senderUsername = (whitePlayerSession != null) ? whitePlayerSession.getUsername() : whitePlayerName;
            senderUserId = (whitePlayerSession != null) ? whitePlayerSession.getUserId() : 0;
        } else {
            senderUsername = (blackPlayerSession != null) ? blackPlayerSession.getUsername() : blackPlayerName;
            senderUserId = (blackPlayerSession != null) ? blackPlayerSession.getUserId() : 0;
        }
        if (senderUsername == null) {
            senderUsername = "unknown";
        }

        String relayContent = "from=" + senderUsername + " " + rawContent;

        // Persist if repository is available
        if (gameRepository != null && dbGameId > 0 && senderUserId > 0) {
            try {
                gameRepository.addChatMessage(dbGameId, senderUserId, rawContent);
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to persist chat message", e);
            }
        }

        sendMessageToPlayers(new Message(MessageType.CHAT, relayContent));
    }

    /**
     * Handles a {@code SAVE_GAME} request from a player.
     *
     * <p>Sets the requesting player's save flag and forwards the {@code SAVE_GAME} message
     * to the opponent so they know their partner wants to save. When both flags are set,
     * the game state is set to {@link GameState#SAVED}, the repository is updated, and
     * {@link MessageType#SAVE_ACCEPTED} is sent to both players. The disconnect timer (if
     * running) is cancelled.</p>
     *
     * @param clientHandler the handler requesting the save
     * @param message       the {@code SAVE_GAME} message
     */
    private void handleSaveGame(ClientHandler clientHandler, Message message) {
        if (game == null) {
            return;
        }

        // Record that this player wants to save
        ClientHandler opponent;
        if (clientHandler == whitePlayerHandler) {
            whiteSaveRequested = true;
            opponent = blackPlayerHandler;
        } else {
            blackSaveRequested = true;
            opponent = whitePlayerHandler;
        }

        // Forward the request to the opponent so they can respond
        if (opponent != null) {
            opponent.sendMessage(new Message(MessageType.SAVE_GAME, ""));
        }

        // If both agreed, finalize the save
        if (whiteSaveRequested && blackSaveRequested) {
            cancelDisconnectTimer();
            game.setGameState(GameState.SAVED);
            if (gameRepository != null && dbGameId > 0) {
                try {
                    gameRepository.updateGameState(dbGameId, "SAVED");
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Failed to persist SAVED state", e);
                }
            }
            sendMessageToPlayers(new Message(MessageType.SAVE_ACCEPTED, ""));
        }
    }

    /**
     * Handles a {@code SUCCESS} message by logging it.
     *
     * @param message the success message
     */
    private void handleSuccess(Message message) {
        LOGGER.log(Level.INFO, "Success: {0}", message);
    }

    /**
     * Handles an {@code ERROR} message by logging it.
     *
     * @param message the error message
     */
    private void handleError(Message message) {
        LOGGER.log(Level.SEVERE, "Error: {0}", message.content());
    }

    /**
     * Handles a {@code FAILURE} message by logging it.
     *
     * @param message the failure message
     */
    private void handleFailure(Message message) {
        LOGGER.log(Level.SEVERE, "Failure: {0}", message.content());
    }

    // ── Disconnect / reconnect ────────────────────────────────────────────────

    /**
     * Handles a player disconnecting from this game session.
     *
     * <p>If the game exists and is in a non-terminal state, the game is paused rather
     * than ended. The remaining connected player receives a
     * {@code GAME_STATUS gameState=PAUSED} message. A countdown timer is started; if the
     * player does not reconnect within {@code disconnectTimeoutSeconds}, the remaining
     * player wins by timeout.</p>
     *
     * <p>If no repository is configured the DB operations are skipped but the game is
     * still paused and the in-memory disconnect timer is still started.</p>
     *
     * <p>This method is {@code synchronized} on this instance.</p>
     *
     * @param clientHandler the handler of the disconnecting player; must not be {@code null}
     */
    public synchronized void disconnectPlayer(ClientHandler clientHandler) {
        if (game != null && !isTerminalState(game.getState())) {
            int disconnectedUserId = 0;
            final GameState forfeitState;

            if (clientHandler == whitePlayerHandler) {
                disconnectedUserId = (whitePlayerSession != null) ? whitePlayerSession.getUserId() : 0;
                forfeitState = GameState.BLACK_WON_BY_TIMEOUT;
            } else if (clientHandler == blackPlayerHandler) {
                disconnectedUserId = (blackPlayerSession != null) ? blackPlayerSession.getUserId() : 0;
                forfeitState = GameState.WHITE_WON_BY_TIMEOUT;
            } else {
                forfeitState = null;
            }

            if (forfeitState != null) {
                // Pause the game instead of awarding resignation immediately
                game.setGameState(GameState.PAUSED);
                sendMessageToOpponent(clientHandler, new Message(MessageType.GAME_STATUS, "gameState=PAUSED"));

                // Persist disconnect info
                if (gameRepository != null && dbGameId > 0) {
                    try {
                        if (disconnectedUserId > 0) {
                            gameRepository.setDisconnect(dbGameId, disconnectedUserId);
                        }
                        gameRepository.updateGameState(dbGameId, "PAUSED");
                    } catch (SQLException e) {
                        LOGGER.log(Level.WARNING, "Failed to persist disconnect state", e);
                    }
                }

                // Start forfeit countdown
                final int finalDisconnectedUserId = disconnectedUserId;
                if (disconnectTimeoutSeconds > 0) {
                    if (scheduler == null) {
                        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                            Thread t = new Thread(r, "disconnect-timer-game-" + gameId);
                            t.setDaemon(true);
                            return t;
                        });
                    }
                    disconnectTimer = scheduler.schedule(() -> {
                        synchronized (GameInstance.this) {
                            if (game != null && game.getState() == GameState.PAUSED) {
                                game.setGameState(forfeitState);
                                sendMessageToPlayers(new Message(MessageType.GAME_STATUS, "gameState=" + forfeitState.name()));
                                if (gameRepository != null && dbGameId > 0) {
                                    try {
                                        gameRepository.updateGameState(dbGameId, forfeitState.name());
                                    } catch (SQLException e) {
                                        LOGGER.log(Level.WARNING, "Failed to persist timeout state", e);
                                    }
                                }
                            }
                        }
                    }, disconnectTimeoutSeconds, TimeUnit.SECONDS);
                }
            }
        }

        if (clientHandler == whitePlayerHandler) {
            whitePlayerHandler = null;
        } else if (clientHandler == blackPlayerHandler) {
            blackPlayerHandler = null;
        }

        if (game != null) {
            game.removeObserver(this);
        }
    }

    /**
     * Reconnects a previously disconnected player to this game session.
     *
     * <p>Cancels the disconnect timer, clears the disconnect record in the database,
     * updates the appropriate player handler, and sets the game state back to
     * {@link GameState#RUNNING}. Both players (the reconnecting player and the one who
     * stayed) receive a {@code GAME_STATUS gameState=RUNNING} message.</p>
     *
     * <p>Re-registers this instance as a {@link GameObserver} if it was removed during
     * the disconnect.</p>
     *
     * <p>This method is {@code synchronized} on this instance.</p>
     *
     * @param handler the new {@link ClientHandler} for the reconnecting player
     * @param session the authenticated session of the reconnecting player; may be {@code null}
     */
    public synchronized void reconnectPlayer(ClientHandler handler, PlayerSession session) {
        cancelDisconnectTimer();

        // Clear DB disconnect record
        if (gameRepository != null && dbGameId > 0) {
            try {
                gameRepository.clearDisconnect(dbGameId);
                gameRepository.updateGameState(dbGameId, "RUNNING");
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to clear disconnect on reconnect", e);
            }
        }

        // Determine which slot the reconnecting player fills. We use the session to match
        // by userId if available; otherwise fill the first empty slot.
        boolean reconnectedAsWhite = false;
        if (whitePlayerHandler == null) {
            whitePlayerHandler = handler;
            whitePlayerSession = session;
            reconnectedAsWhite = true;
        } else if (blackPlayerHandler == null) {
            blackPlayerHandler = handler;
            blackPlayerSession = session;
        } else {
            LOGGER.log(Level.WARNING, "reconnectPlayer called but both slots are filled — ignoring (gameId={0})", gameId);
            return;
        }

        // Re-register as observer if needed
        if (game != null) {
            // Avoid double-registration: remove first, then add
            game.removeObserver(this);
            game.addObserver(this);
            game.setGameState(GameState.RUNNING);
            previousState = GameState.RUNNING;
        }

        sendMessageToPlayers(new Message(MessageType.GAME_STATUS, "gameState=RUNNING"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Sends a message to both connected players. Silently skips a slot if the handler
     * for that slot is {@code null}.
     *
     * @param message the message to broadcast; must not be {@code null}
     */
    private void sendMessageToPlayers(Message message) {
        if (whitePlayerHandler != null) {
            whitePlayerHandler.sendMessage(message);
        }
        if (blackPlayerHandler != null) {
            blackPlayerHandler.sendMessage(message);
        }
    }

    /**
     * Sends a message to the player who is NOT {@code sender}.
     *
     * @param sender  the handler that sent the original message; used to identify the opponent
     * @param message the message to send to the opponent
     */
    private void sendMessageToOpponent(ClientHandler sender, Message message) {
        if (sender == whitePlayerHandler && blackPlayerHandler != null) {
            blackPlayerHandler.sendMessage(message);
        } else if (sender == blackPlayerHandler && whitePlayerHandler != null) {
            whitePlayerHandler.sendMessage(message);
        }
    }

    /**
     * Cancels the active disconnect timer if one is running.
     */
    private void cancelDisconnectTimer() {
        if (disconnectTimer != null && !disconnectTimer.isDone()) {
            disconnectTimer.cancel(false);
            disconnectTimer = null;
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /**
     * Returns the numeric identifier for this game session.
     *
     * @return the game ID assigned at construction time
     */
    public int getGameId() {
        return gameId;
    }

    /**
     * Returns the Scharnagl position index for Chess960 games, or {@code -1} for standard chess.
     *
     * @return the position index in [0, 959] for Chess960 games, or {@code -1} for standard chess
     */
    public int getPositionIndex() {
        return positionIndex;
    }

    /**
     * Returns the {@link ClientHandler} for the white player, or {@code null} if the
     * white slot has not yet been filled.
     *
     * @return the white player's handler, or {@code null}
     */
    public ClientHandler getWhitePlayerHandler() {
        return whitePlayerHandler;
    }

    /**
     * Returns the {@link ClientHandler} for the black player, or {@code null} if the
     * black slot has not yet been filled.
     *
     * @return the black player's handler, or {@code null}
     */
    public ClientHandler getBlackPlayerHandler() {
        return blackPlayerHandler;
    }
}
