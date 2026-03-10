package io.github.conava.chess.core.logic.game;

import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.board.Board;
import io.github.conava.chess.core.data.pieces.Piece;
import io.github.conava.chess.core.data.io.Message;
import io.github.conava.chess.core.data.io.MessageParser;
import io.github.conava.chess.core.data.player.PlayerColor;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import io.github.conava.chess.core.exceptions.IllegalMoveException;
import io.github.conava.chess.core.logic.moves.CastleMove;
import io.github.conava.chess.core.logic.moves.Move;
import io.github.conava.chess.core.logic.moves.PromotionMove;
import io.github.conava.chess.core.data.io.MessageType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The OnlineGame class represents an online game session.
 * It handles communication with the server, game state management, and player interactions.
 * The networking connection is provided externally via the {@link ServerConnection} interface,
 * keeping I/O out of the core module.
 */
public class OnlineGame extends Game {
    private static final Logger LOGGER = Logger.getLogger(OnlineGame.class.getName());
    private static final String JOIN_CODE_PARAM = "joinCode";
    private static final String PLAYER_COLOR_PARAM = "playerColor";
    private static final String MOVE_PARAM = "move";
    private static final String GAME_STATE_PARAM = "gameState";

    private final ServerConnection connection;
    private String joinCode;
    private PlayerColor localPlayerColor;
    private final RulesetOptions selectedRuleset;

    private Board backupBoard;
    private List<Move> backupMoves;
    private GameState backupGameState;
    private int backupHalfMoveClock;
    private Map<String, Integer> backupPositionHistory;

    /**
     * Private constructor — use {@link #create} to obtain an instance.
     *
     * @param selectedRuleset    The selected ruleset for the game.
     * @param playerWhiteName    The name of the white player.
     * @param playerBlackName    The name of the black player.
     * @param onlineGameSettings The settings for the online game, including join code.
     * @param connection         An already-established {@link ServerConnection} to the game server.
     */
    private OnlineGame(RulesetOptions selectedRuleset,
                       String playerWhiteName,
                       String playerBlackName,
                       Map<String, String> onlineGameSettings,
                       ServerConnection connection) {
        super(selectedRuleset, playerWhiteName, playerBlackName);
        this.gameState = GameState.NO_GAME;
        this.joinCode = onlineGameSettings.get("joinCode");
        this.selectedRuleset = selectedRuleset;
        this.connection = connection;
    }

    /**
     * Static factory method that constructs an {@link OnlineGame} without sending any network
     * messages. The caller must invoke {@link #connectToServerGame()} separately once the
     * connection has been confirmed to be live.
     *
     * @param selectedRuleset    The selected ruleset for the game.
     * @param playerWhiteName    The name of the white player.
     * @param playerBlackName    The name of the black player.
     * @param onlineGameSettings The settings for the online game, including join code.
     * @param connection         An already-established {@link ServerConnection} to the game server.
     * @return A newly constructed {@link OnlineGame} instance.
     */
    public static OnlineGame create(RulesetOptions selectedRuleset,
                                    String playerWhiteName,
                                    String playerBlackName,
                                    Map<String, String> onlineGameSettings,
                                    ServerConnection connection) {
        return new OnlineGame(selectedRuleset, playerWhiteName, playerBlackName, onlineGameSettings, connection);
    }

    /**
     * Sends the initial handshake message to the server: either {@code CREATE_GAME} (when no
     * join code is present) or {@code JOIN_GAME} (when a join code was supplied).
     *
     * <p>This method must be called by the application facade <em>after</em>:
     * <ol>
     *   <li>The {@link ServerConnection} has been confirmed live (i.e. {@code isConnected()} is true).</li>
     *   <li>The message handler that forwards server responses to {@link #handleMessage} has been
     *       registered with the underlying transport so that no server replies are lost.</li>
     * </ol>
     * It is intentionally not called from the constructor — see {@link #create} for the
     * two-phase construction contract.
     */
    public void connectToServerGame() {
        Message connectMessage;
        if (joinCode != null && !joinCode.isEmpty()) {
            connectMessage = new Message(MessageType.JOIN_GAME, JOIN_CODE_PARAM + "=" + joinCode);
            localPlayerColor = PlayerColor.BLACK;
            gameState = GameState.RUNNING;
        } else {
            connectMessage = new Message(MessageType.CREATE_GAME, "ruleset=" + selectedRuleset);
            localPlayerColor = PlayerColor.WHITE;
            gameState = GameState.WAITING_FOR_PLAYER;
        }
        sendMessageToServer(connectMessage);
    }

    /**
     * Handles incoming messages from the server.
     *
     * @param message The message received from the server.
     */
    public void handleMessage(Message message) {
        switch (message.type()) {
            case JOIN_CODE:
                handleJoinCode(message);
                break;
            case MOVE:
                handleMove(message);
                break;
            case GAME_STATUS:
                handleGameStatus(message);
                break;
            case SUCCESS:
                handleSuccess(message);
                break;
            case ERROR:
                handleError(message);
                break;
            case FAILURE:
                handleFailure(message);
                break;
            default:
                LOGGER.log(Level.WARNING, "Unsupported message type: " + message.type());
        }
    }

    /**
     * Handles the JOIN_CODE message type.
     *
     * @param message The message containing the join code.
     */
    private void handleJoinCode(Message message) {
        joinCode = message.getParameterValue(JOIN_CODE_PARAM);
        LOGGER.info("Join code received: " + joinCode + " - Please share this code with your friend to join the game");
    }

    /**
     * Handles the MOVE message type.
     *
     * @param message The message containing the move information.
     */
    private void handleMove(Message message) {
        if (Objects.equals(message.getParameterValue(PLAYER_COLOR_PARAM), localPlayerColor.toString())) {
            return;
        }

        try {
            Move move = Move.fromString(Objects.requireNonNull(message.getParameterValue(MOVE_PARAM)),
                    Objects.equals(message.getParameterValue(PLAYER_COLOR_PARAM), "WHITE") ? player0 : player1);
            executeMoveFromRemote(move);
        } catch (IllegalMoveException e) {
            LOGGER.log(Level.SEVERE, "Illegal move received: " + message.content(), e);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Failed to parse move from server: " + message.content(), e);
        }
    }

    /**
     * Handles the GAME_STATUS message type.
     *
     * @param message The message containing the game status.
     */
    private void handleGameStatus(Message message) {
        LOGGER.log(Level.INFO, "Game status update: " + message.getParameterValue(GAME_STATE_PARAM));
        this.gameState = GameState.valueOf(message.getParameterValue(GAME_STATE_PARAM));
        notifyObservers();
    }

    /**
     * Handles the SUCCESS message type.
     *
     * @param message The message indicating success.
     */
    private void handleSuccess(Message message) {
        LOGGER.log(Level.INFO, "Success: " + message);
        if (Objects.equals(message.getParameterValue(MOVE_PARAM), "accepted")) {
            LOGGER.log(Level.INFO, "Move accepted by server");
        }
    }

    /**
     * Handles the ERROR message type.
     *
     * @param message The message indicating an error.
     */
    private void handleError(Message message) {
        LOGGER.log(Level.SEVERE, "Error: " + message.content());
    }

    /**
     * Handles the FAILURE message type.
     *
     * @param message The message indicating a failure.
     */
    private void handleFailure(Message message) {
        LOGGER.log(Level.SEVERE, "Failure: " + message.content());
        if (Objects.equals(message.getParameterValue(MOVE_PARAM), "rejected")) {
            restoreGameState();
            notifyObservers();
        }
    }

    /**
     * Sends a message to the server.
     *
     * @param message The message to be sent to the server.
     */
    public void sendMessageToServer(Message message) {
        if (connection != null) {
            connection.sendMessage(MessageParser.serialize(message));
        }
    }

    /**
     * Ends the game and notifies the server.
     */
    @Override
    public void endGame() {
        gameState = localPlayerColor == PlayerColor.WHITE ? GameState.WHITE_WON_BY_RESIGNATION : GameState.BLACK_WON_BY_RESIGNATION;
        if (connection != null) {
            Message endGameMessage = new Message(MessageType.GAME_STATUS, GAME_STATE_PARAM + "=" + gameState);
            sendMessageToServer(endGameMessage);
            connection.closeConnection();
        }
    }

    /**
     * Executes a move by the local player and sends it to the server.
     * It backs up the current game state to quickly revert if the server rejects the move.
     *
     * @param move The move to be executed.
     * @throws IllegalMoveException If the move is illegal.
     */
    @Override
    protected void executeMove(Move move) throws IllegalMoveException {
        if (isLocalPlayerTurn()) {
            backupGameState();
            super.executeMove(move);
            sendMoveToServer(move);
        } else {
            throw new IllegalMoveException(move);
        }
    }

    /**
     * Checks if it is the local player's turn.
     *
     * @return true if it is the local player's turn, false otherwise.
     */
    private boolean isLocalPlayerTurn() {
        return (localPlayerColor == PlayerColor.WHITE && getCurrentPlayer() == player0) ||
                (localPlayerColor == PlayerColor.BLACK && getCurrentPlayer() == player1);
    }

    /**
     * Sends a move to the server.
     *
     * @param move The move to be sent to the server.
     */
    private void sendMoveToServer(Move move) {
        Message moveMessage = new Message(MessageType.MOVE, MOVE_PARAM + "=" + move.toProtocolString() + " " + PLAYER_COLOR_PARAM + "=" + localPlayerColor);
        sendMessageToServer(moveMessage);
    }

    /**
     * Executes a move received from the server.
     * <p>
     * {@link Move#fromString} produces fresh {@link Square} instances that are not the same
     * objects as the squares held in the board's grid. Passing those disconnected squares
     * directly to {@link Game#executeMove} would mutate the wrong objects and leave the
     * board state unchanged. This method therefore translates the move's start and end
     * squares to the board's canonical {@link Square} instances via {@link #toBoardSquare},
     * then reconstructs the correct {@link Move} subtype ({@link CastleMove},
     * {@link PromotionMove}, or plain {@link Move}) before delegating to
     * {@link Game#executeMove}.
     *
     * @param move The move received from the server (with fresh, non-canonical squares).
     * @throws IllegalMoveException If the move is illegal according to the current board state.
     */
    private void executeMoveFromRemote(Move move) throws IllegalMoveException {
        Square boardStart = toBoardSquare(move.getStart());
        Square boardEnd   = toBoardSquare(move.getEnd());
        Move canonical;
        if (move instanceof CastleMove) {
            canonical = new CastleMove(boardStart, boardEnd);
        } else if (move instanceof PromotionMove pm) {
            canonical = new PromotionMove(boardStart, boardEnd, pm.getTargetPiece());
        } else {
            canonical = new Move(boardStart, boardEnd);
        }
        super.executeMove(canonical);
    }

    /**
     * Gets the legal squares for a given position.
     * Does not return the legal square for the online opponent if clicked on their piece.
     *
     * @param position The position to get legal squares for.
     * @return A list of legal squares.
     */
    @Override
    public List<Square> getLegalSquares(Square position) {
        if (isLocalPlayerPiece(position)) {
            return super.getLegalSquares(position);
        }
        return new ArrayList<>();
    }

    /**
     * Checks if a piece at a given position belongs to the local player.
     *
     * @param position The position to check.
     * @return true if the piece belongs to the local player, false otherwise.
     */
    private boolean isLocalPlayerPiece(Square position) {
        Piece p = board.getSquare(position.getY(), position.getX()).getPiece();
        return p != null && p.getPlayer().color() == localPlayerColor;
    }

    /**
     * Starts the game.
     */
    @Override
    public void startGame() {
        // Implementation for starting the game not needed in online game as the start is handled by the server
    }

    /**
     * Backs up the current game state.
     *
     * <p>Saves the board, move list, game state, halfmove clock, and position history.
     * The position history map is deep-copied so that subsequent mutations to the live map
     * cannot corrupt the backup.
     */
    public void backupGameState() {
        this.backupBoard = this.getBoard().getCopy();
        this.backupMoves = new ArrayList<>(this.moves);
        this.backupGameState = this.getState();
        this.backupHalfMoveClock = this.halfMoveClock;
        this.backupPositionHistory = new HashMap<>(this.positionHistory);
    }

    /**
     * Restores the game state from the backup.
     *
     * <p>Restores the board, move list, game state, halfmove clock, and position history
     * to the values captured by the most recent call to {@link #backupGameState()}.
     */
    public void restoreGameState() {
        this.board = this.backupBoard.getCopy();
        this.moves = new ArrayList<>(this.backupMoves);
        this.setGameState(this.backupGameState);
        this.halfMoveClock = this.backupHalfMoveClock;
        this.positionHistory = new HashMap<>(this.backupPositionHistory);
    }

    /**
     * Gets the join code for the game.
     *
     * @return The join code.
     */
    public String getJoinCode() {
        return joinCode;
    }
}
