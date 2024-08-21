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

public class OnlineGame extends Game {
    private static final Logger LOGGER = Logger.getLogger(OnlineGame.class.getName());
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

    private void handleJoinCode(Message message) {
        String joinCode = message.getParameterValue("joinCode=");
        System.out.println("Join code received: " + joinCode + " - Please share this code with your friend to join the game");
    }

    private void handleMove(Message message) {
        if (Objects.equals(message.getParameterValue("playerColor="), localPlayerColor.toString())) {
            return;
        }

        try {
            Move move = Move.fromString(Objects.requireNonNull(message.getParameterValue("move=")), Objects.equals(message.getParameterValue("playerColor="), "WHITE") ? player0 : player1);
            executeMoveFromRemote(move);
        } catch (IllegalMoveException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
                 InstantiationException | IllegalAccessException e) {
            LOGGER.log(Level.SEVERE, "Illegal move received: " + message.content(), e);
        }
    }

    private void handleGameStatus(Message message) {
        LOGGER.log(Level.INFO, "Game status update: " + message.getParameterValue("gameState="));
        this.gameState = GameState.valueOf(message.getParameterValue("gameState="));
    }

    private void handleSuccess(Message message) {
        LOGGER.log(Level.INFO, "Success: " + message);
        // todo: Needed? probably remove the success message type
        if(Objects.equals(message.getParameterValue("move="), "accepted")) {
            LOGGER.log(Level.INFO, "Move accepted by server");
        }
    }

    private void handleError(Message message) {
        LOGGER.log(Level.SEVERE, "Error: " + message.content());
    }

    private void handleFailure(Message message) {
        LOGGER.log(Level.SEVERE, "Failure: " + message.content());
        if (Objects.equals(message.getParameterValue("move="), "rejected")) {
            restoreGameState();
            notifyObservers();
        }
    }

    public void sendMessageToServer(Message message) {
        if (serverTask != null) {
            serverTask.sendMessage(message);
        }
    }

    @Override
    public void endGame() {
        gameState = localPlayerColor == PlayerColor.WHITE ? GameState.WHITE_WON_BY_RESIGNATION : GameState.BLACK_WON_BY_RESIGNATION;
        if (serverTask != null) {
            Message endGameMessage = new Message(MessageType.GAME_STATUS, "gameState=" + gameState);
            sendMessageToServer(endGameMessage);
            serverTask.closeConnection();
        }
    }

    @Override
    protected void executeMove(Move move) throws IllegalMoveException {
        if(localPlayerColor == PlayerColor.WHITE && getCurrentPlayer() == player0 || localPlayerColor == PlayerColor.BLACK && getCurrentPlayer() == player1) {
            backupGameState();
            super.executeMove(move);
            Message moveMessage = new Message(MessageType.MOVE, "move=" + move + ",playerColor=" + localPlayerColor);
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