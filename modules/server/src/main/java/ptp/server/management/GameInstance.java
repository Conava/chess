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
    private RulesetOptions ruleset;
    private ClientHandler whitePlayerHandler;
    private ClientHandler blackPlayerHandler;

    /**
     * Constructs a GameInstance with the specified ruleset.
     */
    public GameInstance() {
        game = null;
        whitePlayerHandler = null;
        blackPlayerHandler = null;

    }

    public void startGame(RulesetOptions ruleset) {
        game = new ServerGame(ruleset);
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

    public void processMessage(Message message) {
        //todo: implement message processing
    }
}
