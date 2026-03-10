package io.github.conava.chess.server.management;

import io.github.conava.chess.core.data.io.Message;
import io.github.conava.chess.core.data.io.MessageType;
import io.github.conava.chess.core.exceptions.IllegalMoveException;
import io.github.conava.chess.core.logic.game.Game;
import io.github.conava.chess.core.logic.game.GameState;
import io.github.conava.chess.core.logic.moves.Move;
import io.github.conava.chess.core.logic.observer.GameObserver;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import io.github.conava.chess.core.logic.ruleset.chess960Ruleset.Chess960Ruleset;

import java.util.Objects;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a single server-side game session, managing two player slots (white and black),
 * deferred game creation, and in-game message routing.
 *
 * <p>Game creation is deferred until both players have connected via
 * {@link #connectPlayer(ClientHandler, String)}. Until that point the internal {@link Game}
 * reference is {@code null} and move messages are silently ignored. This avoids the need to
 * store placeholder player names and keeps the {@link Game} superclass in a consistent state
 * from the moment it is constructed.</p>
 *
 * <p>This class implements {@link GameObserver} to receive state-change notifications from
 * the underlying {@link Game}. The observer is used exclusively for terminal state transitions
 * (checkmate, resignation, timeout, draw). Move relay is handled explicitly in
 * {@link #handleMove(ClientHandler, Message)} because the move data is only available at the
 * call site, not in the signal-only {@link #onGameStateChanged()} callback.</p>
 *
 * <p>Both {@link #connectPlayer(ClientHandler, String)} and {@link #processMessage(ClientHandler, Message)}
 * are {@code synchronized} on this instance to prevent race conditions between the two player
 * threads sharing the same game.</p>
 */
public class GameInstance implements GameObserver {

    private static final Logger LOGGER = Logger.getLogger(GameInstance.class.getName());

    private final int gameId;
    private final RulesetOptions ruleset;

    /** Scharnagl index for Chess960 games; {@code -1} for standard chess. */
    private final int positionIndex;

    private Game game;
    private GameState previousState;

    private String whitePlayerName;
    private String blackPlayerName;

    private ClientHandler whitePlayerHandler;
    private ClientHandler blackPlayerHandler;

    /**
     * Constructs a new {@code GameInstance} in the {@code WAITING_FOR_PLAYER} state.
     *
     * <p>No {@link Game} object is created at this point. Game creation is deferred until
     * both players have connected via {@link #connectPlayer(ClientHandler, String)}, so that
     * both player names are available before the {@link Game} superclass constructor runs.</p>
     *
     * @param gameId  The unique numeric identifier for this game session.
     * @param ruleset The ruleset variant to use when the game is eventually created.
     */
    public GameInstance(int gameId, RulesetOptions ruleset) {
        this.gameId = gameId;
        this.ruleset = ruleset;
        this.positionIndex = (ruleset == RulesetOptions.CHESS960)
                ? new Random().nextInt(960)
                : -1;
        this.game = null;
        this.previousState = null;
    }

    /**
     * Connects a player to this game instance and, when both slots are filled, creates the
     * underlying {@link Game} and starts play.
     *
     * <p>The first call fills the white (creator) slot; the second call fills the black
     * (joiner) slot. On the second call, the {@link Game} is created via
     * {@link Game#createServerGame(RulesetOptions, String, String)}, this instance is
     * registered as a {@link GameObserver}, and a single {@code GAME_STATUS gameState=RUNNING}
     * message is broadcast to both players.</p>
     *
     * @param clientHandler The {@link ClientHandler} for the connecting player; must not be {@code null}.
     * @param playerName    The display name for the connecting player. A blank or {@code null}
     *                      value causes this {@link GameInstance} to substitute a default name
     *                      ({@code "Player 1"} for white, {@code "Player 2"} for black).
     */
    public synchronized void connectPlayer(ClientHandler clientHandler, String playerName) {
        if (whitePlayerHandler == null) {
            whitePlayerHandler = clientHandler;
            whitePlayerName = (playerName != null && !playerName.isBlank()) ? playerName : "Player 1";
            clientHandler.sendMessage(new Message(MessageType.SUCCESS, "player=white"));
        } else if (blackPlayerHandler == null) {
            blackPlayerHandler = clientHandler;
            blackPlayerName = (playerName != null && !playerName.isBlank()) ? playerName : "Player 2";
            String successContent = (positionIndex >= 0)
                    ? "player=black position=" + positionIndex + " ruleset=CHESS960"
                    : "player=black";
            clientHandler.sendMessage(new Message(MessageType.SUCCESS, successContent));
            startGame();
        }
    }

    /**
     * Creates the {@link Game} via the factory, registers this instance as observer, and
     * notifies both players that the game is now {@code RUNNING}.
     *
     * <p>Only one {@code GAME_STATUS gameState=RUNNING} message is sent. The observer
     * callback {@link #onGameStateChanged()} is NOT triggered for the RUNNING transition
     * because RUNNING is not a terminal state, so there is no risk of a duplicate message
     * from the observer path.</p>
     */
    private void startGame() {
        if (positionIndex >= 0) {
            this.game = Game.createServerGame(
                    new Chess960Ruleset(positionIndex), whitePlayerName, blackPlayerName);
        } else {
            this.game = Game.createServerGame(ruleset, whitePlayerName, blackPlayerName);
        }
        this.game.addObserver(this);
        this.game.startGame();
        this.previousState = this.game.getState();
        sendMessageToPlayers(new Message(MessageType.GAME_STATUS, "gameState=RUNNING"));
    }

    /**
     * Observer callback invoked by the underlying {@link Game} whenever its state changes.
     *
     * <p>This method handles terminal state transitions only. If the new game state differs
     * from the previously observed state and is a terminal state (checkmate, resignation,
     * timeout, or draw), a {@code GAME_STATUS} message is sent to both connected players.
     * The RUNNING state is intentionally excluded: its notification is sent once in
     * {@link #startGame()} so that this observer never sends a duplicate.</p>
     *
     * <p>This method is safe to call from any thread because {@link #processMessage} is
     * synchronized on this instance, and {@code notifyObservers()} is called only from
     * within synchronized contexts.</p>
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
     * Returns {@code true} if the given {@link GameState} represents the end of the game
     * (checkmate, resignation, timeout, or draw of any variant).
     *
     * @param state The state to test; must not be {@code null}.
     * @return {@code true} if {@code state} is a terminal state, {@code false} otherwise.
     */
    private boolean isTerminalState(GameState state) {
        return switch (state) {
            case WHITE_WON_BY_CHECKMATE,
                 BLACK_WON_BY_CHECKMATE,
                 WHITE_WON_BY_RESIGNATION,
                 BLACK_WON_BY_RESIGNATION,
                 WHITE_WON_BY_TIMEOUT,
                 BLACK_WON_BY_TIMEOUT,
                 DRAW_BY_STALEMATE,
                 DRAW_BY_INSUFFICIENT_MATERIAL,
                 DRAW_BY_THREEFOLD_REPETITION,
                 DRAW_BY_FIFTY_MOVE_RULE -> true;
            default -> false;
        };
    }

    /**
     * Dispatches an in-game message from a client to the appropriate handler.
     *
     * <p>This method is {@code synchronized} on this instance so that concurrent messages
     * from both player threads are processed serially, preventing race conditions on the
     * underlying {@link Game} object.</p>
     *
     * @param clientHandler The {@link ClientHandler} that sent the message.
     * @param message       The message to process; must not be {@code null}.
     */
    public synchronized void processMessage(ClientHandler clientHandler, Message message) {
        switch (message.type()) {
            case JOIN_CODE -> handleJoinCode(message);
            case MOVE -> handleMove(clientHandler, message);
            case GAME_STATUS -> handleGameStatus(clientHandler, message);
            case SUCCESS -> handleSuccess(message);
            case ERROR -> handleError(message);
            case FAILURE -> handleFailure(message);
            default -> LOGGER.log(Level.WARNING, "Unsupported message type: {0}", message.type());
        }
    }

    /**
     * Sends a message to both connected players. Silently skips a slot if the handler
     * for that slot is {@code null} (player not yet connected or already disconnected).
     *
     * @param message The message to broadcast; must not be {@code null}.
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
     * Handles a {@code JOIN_CODE} message by logging its content.
     *
     * @param message The message containing the join code.
     */
    private void handleJoinCode(Message message) {
        LOGGER.log(Level.INFO, "Join code received: {0}", message.content());
    }

    /**
     * Handles a {@code MOVE} message: parses the move, executes it on the game, then
     * relays it to both players on success, or sends an {@code ERROR} to the sender on
     * failure.
     *
     * <p>Move relay is explicit (not observer-driven) because the move payload is only
     * available at this call site -- the {@link GameObserver#onGameStateChanged()} callback
     * carries no parameters.</p>
     *
     * <p>If the game has not yet been created (no players connected yet), the message is
     * silently ignored to keep the handler thread alive during setup.</p>
     *
     * @param clientHandler The {@link ClientHandler} that sent the move; used to send
     *                      an error response on illegal or malformed input.
     * @param message       The message carrying the move in {@code move=<notation> playerColor=<COLOR>} format.
     */
    private void handleMove(ClientHandler clientHandler, Message message) {
        if (game == null) {
            LOGGER.log(Level.WARNING, "MOVE received but game not yet created (gameId={0})", gameId);
            return;
        }
        try {
            var player = Objects.equals(message.getParameterValue("playerColor"), "WHITE")
                    ? game.getPlayerWhite()
                    : game.getPlayerBlack();
            Move move = game.getRuleset().deserializeMove(
                    Objects.requireNonNull(message.getParameterValue("move")),
                    game.getBoard(),
                    player);
            game.movePiece(move.getStart(), move.getEnd());
            LOGGER.log(Level.INFO, "Move executed: {0}", message.content());
            sendMessageToPlayers(new Message(MessageType.MOVE, message.content()));
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
     * <p>If the message body is non-empty, it is treated as a resignation request.
     * White may resign (sending {@code BLACK_WON_BY_RESIGNATION}) and black may resign
     * (sending {@code WHITE_WON_BY_RESIGNATION}). If the body is empty, the current
     * game state is returned to the requesting client.</p>
     *
     * @param clientHandler The {@link ClientHandler} requesting status or submitting a resignation.
     * @param message       The {@code GAME_STATUS} message; content may be empty for a status query.
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
                LOGGER.log(Level.WARNING,
                        "Invalid gameState parameter in GAME_STATUS message: " + message.content(), e);
                if (clientHandler != null) {
                    clientHandler.sendMessage(new Message(
                            MessageType.ERROR,
                            "Malformed gameState parameter: " + message.getParameterValue("gameState")));
                }
            }
        } else {
            LOGGER.log(Level.INFO, "Current game status requested");
            clientHandler.sendMessage(new Message(MessageType.GAME_STATUS, "gameState=" + game.getState()));
        }
    }

    /**
     * Handles a {@code SUCCESS} message by logging it.
     *
     * @param message The success message.
     */
    private void handleSuccess(Message message) {
        LOGGER.log(Level.INFO, "Success: {0}", message);
    }

    /**
     * Handles an {@code ERROR} message by logging it.
     *
     * @param message The error message.
     */
    private void handleError(Message message) {
        LOGGER.log(Level.SEVERE, "Error: {0}", message.content());
    }

    /**
     * Handles a {@code FAILURE} message by logging it.
     *
     * @param message The failure message.
     */
    private void handleFailure(Message message) {
        LOGGER.log(Level.SEVERE, "Failure: {0}", message.content());
    }

    /**
     * Handles a player disconnecting from this game session.
     *
     * <p>If the game exists and is in a non-terminal state, the disconnecting player's
     * side is awarded a resignation loss: white's disconnect results in
     * {@link GameState#BLACK_WON_BY_RESIGNATION} and black's disconnect results in
     * {@link GameState#WHITE_WON_BY_RESIGNATION}. A {@code GAME_STATUS} message is then
     * sent to the remaining connected player so they are informed of the outcome.</p>
     *
     * <p>After notification, the disconnected player's handler reference is nulled out and
     * the observer is removed from the underlying {@link Game} (if one exists) to prevent
     * memory leaks from dangling observer registrations.</p>
     *
     * <p>This method is {@code synchronized} on this instance to prevent concurrent
     * disconnect and move-processing races.</p>
     *
     * @param clientHandler The {@link ClientHandler} of the player who disconnected;
     *                      must not be {@code null}.
     */
    public synchronized void disconnectPlayer(ClientHandler clientHandler) {
        if (game != null && !isTerminalState(game.getState())) {
            if (clientHandler == whitePlayerHandler) {
                game.setGameState(GameState.BLACK_WON_BY_RESIGNATION);
                if (blackPlayerHandler != null) {
                    blackPlayerHandler.sendMessage(new Message(MessageType.GAME_STATUS,
                            "gameState=" + GameState.BLACK_WON_BY_RESIGNATION.name()));
                }
            } else if (clientHandler == blackPlayerHandler) {
                game.setGameState(GameState.WHITE_WON_BY_RESIGNATION);
                if (whitePlayerHandler != null) {
                    whitePlayerHandler.sendMessage(new Message(MessageType.GAME_STATUS,
                            "gameState=" + GameState.WHITE_WON_BY_RESIGNATION.name()));
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
     * Returns the numeric identifier for this game session.
     *
     * @return The game ID assigned at construction time.
     */
    public int getGameId() {
        return gameId;
    }

    /**
     * Returns the Scharnagl position index for Chess960 games, or {@code -1} for standard chess.
     *
     * @return The position index in [0, 959] for Chess960 games, or {@code -1} for standard chess.
     */
    public int getPositionIndex() {
        return positionIndex;
    }

    /**
     * Returns the {@link ClientHandler} for the white player, or {@code null} if the
     * white slot has not yet been filled.
     *
     * @return The white player's handler, or {@code null}.
     */
    public ClientHandler getWhitePlayerHandler() {
        return whitePlayerHandler;
    }

    /**
     * Returns the {@link ClientHandler} for the black player, or {@code null} if the
     * black slot has not yet been filled.
     *
     * @return The black player's handler, or {@code null}.
     */
    public ClientHandler getBlackPlayerHandler() {
        return blackPlayerHandler;
    }
}
