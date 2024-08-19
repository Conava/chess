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
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OnlineGame extends Game {
    private static final Logger LOGGER = Logger.getLogger(OnlineGame.class.getName());
    private final String serverIP;
    private final int serverPort;
    private String joinCode;
    private ServerCommunicationTask serverTask;
    private PlayerColor localPlayerColor;
    private final RulesetOptions selectedRuleset;

    private Board backupBoard;
    private List<Move> backupMoves;
    private GameState backupGameState;
    private final CountDownLatch connectionLatch = new CountDownLatch(1);


    public OnlineGame(RulesetOptions selectedRuleset, String playerWhiteName, String playerBlackName, Map<String, String> onlineGameSettings) {
        super(selectedRuleset, playerWhiteName, playerBlackName);
        this.gameState = GameState.NO_GAME;
        this.serverIP = onlineGameSettings.get("ip");
        this.serverPort = Integer.parseInt(onlineGameSettings.get("port"));
        this.joinCode = onlineGameSettings.get("joinCode");
        this.selectedRuleset = selectedRuleset;
        if(startServerCommunication()) {
            connectToServerGame();
        }
    }

    private boolean startServerCommunication() {
        serverTask = new ServerCommunicationTask(serverIP, serverPort,  this, connectionLatch);
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

    private void connectToServerGame() {
        Message connectMessage;
        if(joinCode != null && !joinCode.isEmpty()) {
            connectMessage = new Message(MessageType.JOIN_GAME, "joinCode=" + joinCode);
            localPlayerColor = PlayerColor.BLACK;
            gameState = GameState.RUNNING;
        } else {
            connectMessage= new Message(MessageType.CREATE_GAME, "ruleset=" + selectedRuleset);
            localPlayerColor = PlayerColor.WHITE;
            gameState = GameState.WAITING_FOR_PLAYER;
        }
        sendMessageToServer(connectMessage);
    }

    public void handleMessage(Message message) {
        // todo: implement message handling
        String messageString = message.content();
        switch (message.type()) {
            case JOIN_CODE:
                handleJoinCode(messageString);
                break;
            case MOVE:
                handleMove(messageString);
                break;
            case GAME_STATUS:
                handleGameStatus(messageString);
                break;
            case SUCCESS:
                handleSuccess(messageString);
                break;
            case ERROR:
                handleError(messageString);
                break;
            case FAILURE:
                handleFailure(messageString);
                break;
            default:
                LOGGER.log(Level.WARNING, "Unsupported message type: " + message.type());
        }
    }

    private void handleJoinCode(String message) {
        joinCode = message;
        System.out.println("Join code received: " + joinCode + " - Please share this code with your friend to join the game");
    }

    private void handleMove(String message) {

    }

    private void handleGameStatus(String message) {
        LOGGER.log(Level.INFO, "Game status update: " + message);
        this.gameState = GameState.valueOf(message);
    }

    private void handleGameEnd(String message) {
        LOGGER.log(Level.INFO, "Game ended: " + message);
        endGame();
    }

    private void handleSuccess(String message) {
        LOGGER.log(Level.INFO, "Success: " + message);
    }

    private void handleError(String message) {
        LOGGER.log(Level.SEVERE, "Error: " + message);
    }

    private void handleFailure(String message) {
        LOGGER.log(Level.SEVERE, "Failure: " + message);
    }

    public void sendMessageToServer(Message message) {
        if (serverTask != null) {
            serverTask.sendMessage(message);
        }
    }

    @Override
    public void endGame() {
        if (serverTask != null) {
            Message endGameMessage = new Message(MessageType.GAME_STATUS, "gameState=" + gameState);
            sendMessageToServer(endGameMessage);
            serverTask.closeConnection();
        }
    }

    public String getJoinCode() {
        return joinCode;
    }

    @Override
    protected void executeMove(Move move) throws IllegalMoveException {
        if(localPlayerColor == PlayerColor.WHITE && getCurrentPlayer() == player0 || localPlayerColor == PlayerColor.BLACK && getCurrentPlayer() == player1) {
            backupGameState();
            super.executeMove(move);
            Message moveMessage = new Message(MessageType.MOVE, move.toString());
            sendMessageToServer(moveMessage);
        } else {
            throw new IllegalMoveException(move);
        }
    }

    private void executeMoveFromRemote(Move move) throws IllegalMoveException {
        super.executeMove(move);
        notifyObservers();
    }

    @Override
    public List<Square> getLegalSquares(Square position) {
        // Does not return legal squares for opponent's pieces
        if(board.getSquare(position.getY(), position.getX()).getPiece().getPlayer().color() == localPlayerColor) {
            return super.getLegalSquares(position);
        }
        return new ArrayList<>();
    }

    @Override
    public void startGame() {

    }

    public void backupGameState() {
        this.backupBoard = this.getBoard().getCopy();
        this.backupMoves = new ArrayList<>(this.moves);
        this.backupGameState = this.getState();
    }

    public void restoreGameState() {
        this.board = this.backupBoard.getCopy();
        this.moves = new ArrayList<>(this.backupMoves);
        this.setGameState(this.backupGameState);
    }
}