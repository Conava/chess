package ptp.core.logic.game;

import ptp.core.data.Square;
import ptp.core.data.board.Board;
import ptp.core.data.io.Message;
import ptp.core.data.player.PlayerColor;
import ptp.core.logic.ruleset.RulesetOptions;
import ptp.core.exceptions.IllegalMoveException;
import ptp.core.logic.moves.Move;
import ptp.core.data.io.MessageType;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The OnlineGame class represents an online game session.
 * It handles communication with the server, game state management, and player interactions.
 */
public class OnlineGame extends Game {
    private static final Logger LOGGER = Logger.getLogger(OnlineGame.class.getName());
    private static final String JOIN_CODE_PARAM = "joinCode";
    private static final String PLAYER_COLOR_PARAM = "playerColor";
    private static final String MOVE_PARAM = "move";
    private static final String GAME_STATE_PARAM = "gameState";

    private final String serverIP;
    private final int serverPort;
    private final String joinCode;
    private ServerCommunicationTask serverTask;
    private PlayerColor localPlayerColor;
    private final RulesetOptions selectedRuleset;

    private Board backupBoard;
    private List<Move> backupMoves;
    private GameState backupGameState;
    private final CountDownLatch connectionLatch = new CountDownLatch(1);

    /**
     * Constructs an OnlineGame instance.
     *
     * @param selectedRuleset The selected ruleset for the game.
     * @param playerWhiteName The name of the white player.
     * @param playerBlackName The name of the black player.
     * @param onlineGameSettings The settings for the online game, including server IP, port, and join code.
     */
    public OnlineGame(RulesetOptions selectedRuleset, String playerWhiteName, String playerBlackName, Map<String, String> onlineGameSettings) {
        super(selectedRuleset, playerWhiteName, playerBlackName);
        this.gameState = GameState.NO_GAME;
        this.serverIP = onlineGameSettings.get("ip");
        this.serverPort = Integer.parseInt(onlineGameSettings.get("port"));
        this.joinCode = onlineGameSettings.get("joinCode");
        this.selectedRuleset = selectedRuleset;
        if (startServerCommunication()) {
            connectToServerGame();
        }
    }

    /**
     * Starts the server communication task.
     *
     * @return true if the server communication was successfully started, false otherwise.
     */
    private boolean startServerCommunication() {
        serverTask = new ServerCommunicationTask(serverIP, serverPort, this, connectionLatch);
        Thread serverThread = new Thread(serverTask);
        serverThread.start();

        try {
            connectionLatch.await(); // Wait for the connection to be established
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.SEVERE, "Thread interrupted while waiting for server connection", e);
            gameState = GameState.SERVER_ERROR;
            return false;
        }

        if (!serverTask.isConnected()) {
            gameState = GameState.SERVER_ERROR;
            return false;
        }
        return true;
    }

    /**
     * Connects to the server game using the join code or creates a new game.
     */
    private void connectToServerGame() {
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
        String joinCode = message.getParameterValue(JOIN_CODE_PARAM);
        System.out.println("Join code received: " + joinCode + " - Please share this code with your friend to join the game");
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
        } catch (IllegalMoveException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
                 InstantiationException | IllegalAccessException e) {
            LOGGER.log(Level.SEVERE, "Illegal move received: " + message.content(), e);
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
        if (serverTask != null) {
            serverTask.sendMessage(message);
        }
    }

    /**
     * Ends the game and notifies the server.
     */
    @Override
    public void endGame() {
        gameState = localPlayerColor == PlayerColor.WHITE ? GameState.WHITE_WON_BY_RESIGNATION : GameState.BLACK_WON_BY_RESIGNATION;
        if (serverTask != null) {
            Message endGameMessage = new Message(MessageType.GAME_STATUS, GAME_STATE_PARAM + "=" + gameState);
            sendMessageToServer(endGameMessage);
            serverTask.closeConnection();
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
        Message moveMessage = new Message(MessageType.MOVE, MOVE_PARAM + "=" + move + " " + PLAYER_COLOR_PARAM + "=" + localPlayerColor);
        sendMessageToServer(moveMessage);
    }

    /**
     * Executes a move received from the server.
     *
     * @param move The move to be executed.
     * @throws IllegalMoveException If the move is illegal.
     */
    private void executeMoveFromRemote(Move move) throws IllegalMoveException {
        super.executeMove(move);
        notifyObservers();
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
        return board.getSquare(position.getY(), position.getX()).getPiece().getPlayer().color() == localPlayerColor;
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
     */
    public void backupGameState() {
        this.backupBoard = this.getBoard().getCopy();
        this.backupMoves = new ArrayList<>(this.moves);
        this.backupGameState = this.getState();
    }

    /**
     * Restores the game state from the backup.
     */
    public void restoreGameState() {
        this.board = this.backupBoard.getCopy();
        this.moves = new ArrayList<>(this.backupMoves);
        this.setGameState(this.backupGameState);
    }
}