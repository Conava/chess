package ptp.server.management;

import ptp.core.data.io.Message;
import ptp.core.data.io.MessageType;
import ptp.core.data.player.Player;
import ptp.core.exceptions.IllegalMoveException;
import ptp.core.logic.game.GameState;
import ptp.core.logic.game.ServerGame;
import ptp.core.logic.moves.Move;
import ptp.core.logic.ruleset.RulesetOptions;
import ptp.core.logic.game.Game;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;

// todo: this class has to use the ServerGame and manage it
/**
 * The GameInstance class represents an instance of a game.
 * It manages the connection state of players and initializes the game state based on the provided ruleset.
 */
public class GameInstance {
    private static final Logger LOGGER = Logger.getLogger(GameInstance.class.getName());
    private Game game;
    private RulesetOptions ruleset;
    private final int gameId;
    private ClientHandler whitePlayerHandler;
    private ClientHandler blackPlayerHandler;

    /**
     * Constructs a GameInstance with the specified ruleset.
     */
    public GameInstance(int gameId, RulesetOptions ruleset) {
        game = null;
        whitePlayerHandler = null;
        blackPlayerHandler = null;
        this.gameId = gameId;
        game = new ServerGame(ruleset);
        game.setGameState(GameState.RUNNING);
    }

    public synchronized void connectPlayer(ClientHandler clientHandler, Message message) {
        if (whitePlayerHandler == null) {
            whitePlayerHandler = clientHandler;
            clientHandler.sendMessage(new Message(MessageType.SUCCESS, "player=white"));
            game.setGameState(GameState.WAITING_FOR_PLAYER);
            ruleset = RulesetOptions.valueOf(message.getParameterValue("ruleset"));
        } else if (blackPlayerHandler == null) {
            blackPlayerHandler = clientHandler;
            clientHandler.sendMessage(new Message(MessageType.SUCCESS, "player=plack"));
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
            handleJoinCode(clientHandler, message);
            break;
        case MOVE:
            handleMove(message, clientHandler == whitePlayerHandler ? game.getPlayerWhite() : game.getPlayerBlack(), clientHandler);
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
            throw new IllegalArgumentException("Unknown message type: " + message.type());
    }
}

/**
 * Handles the JOIN_CODE message type.
 * Sends the join code to the client.
 *
 * @param clientHandler The client handler that sent the message.
 * @param message The message to be processed.
 */
private void handleJoinCode(ClientHandler clientHandler, Message message) {
        LOGGER.info("Received join code request.\nSending response: " + message.content());
        clientHandler.sendMessage(new Message(MessageType.JOIN_CODE, String.valueOf(gameId)));
}

/**
 * Handles the MOVE message type.
 * Processes the move and sends the updated game state to both players.
 *
 * @param message The message to be processed.
 */
private void handleMove(Message message, Player player, ClientHandler clientHandler) {
    try {
        Move move = Move.fromString(message.content(), player);
        game.movePiece(move.getStart(), move.getEnd());
        LOGGER.info("Move processed: " + message.content());
        sendMessageToPlayers(new Message(MessageType.MOVE, message.content()));
    } catch (InvocationTargetException | ClassNotFoundException | NoSuchMethodException | InstantiationException |
             IllegalAccessException e) {
        LOGGER.warning("Invalid move: " + message.content());
        clientHandler.sendMessage(new Message(MessageType.ERROR, "Invalid move: " + message.content()));
    } catch (IllegalMoveException e) {
        LOGGER.warning("Illegal move rejected: " + message.content());
        clientHandler.sendMessage(new Message(MessageType.ERROR, "Illegal move rejected: " + message.content()));
    }
}

    private void handleGameStatus(Message message) {
    // Implement logic to handle GAME_STATUS message
}

private void handleSuccess(Message message) {
    // Implement logic to handle SUCCESS message
}

private void handleError(Message message) {
    // Implement logic to handle ERROR message
}

private void handleFailure(Message message) {
    // Implement logic to handle FAILURE message
}

    public int getGameId() {
        return gameId;
    }

    private void sendMessageToPlayers(Message message) {
        whitePlayerHandler.sendMessage(message);
        blackPlayerHandler.sendMessage(message);
    }
}
