package ptp.server.management;

import ptp.core.data.io.Message;
import ptp.core.data.io.MessageType;
import ptp.core.logic.game.GameState;
import ptp.core.logic.game.ServerGame;
import ptp.core.logic.ruleset.RulesetOptions;
import ptp.core.logic.game.Game;

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
    public GameInstance(int gameId) {
        game = null;
        whitePlayerHandler = null;
        blackPlayerHandler = null;
        this.gameId = gameId;
    }

    public void startGame(RulesetOptions ruleset) {
        game = new ServerGame(ruleset);
        game.setGameState(GameState.RUNNING);
        sendMessageToPlayers(new Message(MessageType.GAME_STATUS, "gameState=running"));
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
            startGame(ruleset);
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
            throw new IllegalArgumentException("Unknown message type: " + message.type());
    }
}

private void handleJoinCode(ClientHandler clientHandler, Message message) {
        LOGGER.info("Received join code request.\nSending response: " + message.content());
        clientHandler.sendMessage(new Message(MessageType.JOIN_CODE, String.valueOf(gameId)));
}

private void handleMove(Message message) {
    // Implement logic to handle MOVE message
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
