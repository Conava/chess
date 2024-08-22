package ptp.server.management;

import ptp.core.data.io.Message;
import ptp.core.data.io.MessageType;
import ptp.core.exceptions.IllegalMoveException;
import ptp.core.logic.game.GameState;
import ptp.core.logic.game.ServerGame;
import ptp.core.logic.moves.Move;
import ptp.core.logic.ruleset.RulesetOptions;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The GameInstance class represents an instance of a game.
 * It manages the connection state of players and initializes the game state based on the provided ruleset.
 */
public class GameInstance {
    private static final Logger LOGGER = Logger.getLogger(GameInstance.class.getName());
    private final ServerGame game;
    private final int gameId;
    private ClientHandler whitePlayerHandler;
    private ClientHandler blackPlayerHandler;

    /**
     * Constructs a GameInstance with the specified ruleset.
     */
    public GameInstance(int gameId, RulesetOptions ruleset) {
        this.gameId = gameId;
        this.game = new ServerGame(ruleset);
        this.game.setGameState(GameState.WAITING_FOR_PLAYER);
    }

    public synchronized void connectPlayer(ClientHandler clientHandler, Message message) {
        if (whitePlayerHandler == null) {
            whitePlayerHandler = clientHandler;
            clientHandler.sendMessage(new Message(MessageType.SUCCESS, "player=white"));
        } else if (blackPlayerHandler == null) {
            blackPlayerHandler = clientHandler;
            clientHandler.sendMessage(new Message(MessageType.SUCCESS, "player=black"));
            game.startGame();
            sendMessageToPlayers(new Message(MessageType.GAME_STATUS, "gameState=running"));
        }
    }

    public ClientHandler getWhitePlayerHandler() {
        return whitePlayerHandler;
    }

    public ClientHandler getBlackPlayerHandler() {
        return blackPlayerHandler;
    }

    public void processMessage(ClientHandler clientHandler, Message message) {
        switch (message.type()) {
            case JOIN_CODE:
                handleJoinCode(message);
                break;
            case MOVE:
                handleMove(message);
                break;
            case GAME_STATUS:
                handleGameStatus(message, clientHandler);
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

    public int getGameId() {
        return gameId;
    }

    private void sendMessageToPlayers(Message message) {
        if (whitePlayerHandler != null) {
            whitePlayerHandler.sendMessage(message);
        }
        if (blackPlayerHandler != null) {
            blackPlayerHandler.sendMessage(message);
        }
    }

    private void handleJoinCode(Message message) {
        LOGGER.log(Level.INFO, "Join code received: " + message.content());
    }

    private void handleMove(Message message) {
        try {
            Move move = Move.fromString(Objects.requireNonNull(message.getParameterValue("move")),
                    Objects.equals(message.getParameterValue("playerColor"), "WHITE") ? game.getPlayerWhite() : game.getPlayerBlack());
            game.movePiece(move.getStart(), move.getEnd());
            LOGGER.log(Level.INFO, "Move executed: " + message.content());
        } catch (IllegalMoveException | ClassNotFoundException | NoSuchMethodException |
                 InstantiationException | IllegalAccessException | InvocationTargetException e) {
            LOGGER.log(Level.SEVERE, "Illegal move received: " + message.content(), e);
        }
    }

    private void handleGameStatus(Message message, ClientHandler clientHandler) {
        LOGGER.log(Level.INFO, "Game status requested");
        clientHandler.sendMessage(new Message(MessageType.GAME_STATUS, "gameState=" + game.getState()));
    }

    private void handleSuccess(Message message) {
        LOGGER.log(Level.INFO, "Success: " + message);
    }

    private void handleError(Message message) {
        LOGGER.log(Level.SEVERE, "Error: " + message.content());
    }

    private void handleFailure(Message message) {
        LOGGER.log(Level.SEVERE, "Failure: " + message.content());
    }
}