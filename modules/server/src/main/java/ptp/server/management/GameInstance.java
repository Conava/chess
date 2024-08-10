package ptp.server.management;

import ptp.core.data.io.Message;
import ptp.core.data.io.MessageType;
import ptp.core.logic.game.GameState;
import ptp.core.logic.game.ServerGame;
import ptp.core.logic.ruleset.RulesetOptions;
import ptp.core.logic.game.Game;

// todo: this class has to use the ServerGame and manage it
/**
 * The GameInstance class represents an instance of a game.
 * It manages the connection state of players and initializes the game state based on the provided ruleset.
 */
public class GameInstance {
    private Game game;

    private ClientHandler whitePlayerHandler;
    private ClientHandler blackPlayerHandler;

    /**
     * Constructs a GameInstance with the specified ruleset.
     */
    public GameInstance() {}

    public void startGame(RulesetOptions ruleset) {
        game = new ServerGame(ruleset);
    }

    public synchronized int connectPlayer(ClientHandler clientHandler) {
        if (whitePlayerHandler == null) {
            whitePlayerHandler = clientHandler;
            clientHandler.sendMessage(new Message(MessageType.SUCCESS, "Player=White"));
            game.setGameState(GameState.WAITING_FOR_PLAYER);
            return 1;
        } else if (blackPlayerHandler == null) {
            blackPlayerHandler = clientHandler;
            clientHandler.sendMessage(new Message(MessageType.SUCCESS, "Player=Black"));
            game.startGame();
            return 2;
        }
        return 0;
    }

    public ClientHandler getWhitePlayerHandler() {
        return whitePlayerHandler;
    }

    public ClientHandler getBlackPlayerHandler() {
        return blackPlayerHandler;
    }

    public void processMessage(Message message) {
        //todo: implement message processing
    }
}
