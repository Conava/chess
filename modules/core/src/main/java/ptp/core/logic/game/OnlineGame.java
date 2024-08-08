package ptp.core.logic.game;

import ptp.core.data.Square;
import ptp.core.data.io.Message;
import ptp.core.logic.ruleset.RulesetOptions;
import ptp.core.exceptions.IllegalMoveException;
import ptp.core.logic.moves.Move;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OnlineGame extends Game {
    private static final Logger LOGGER = Logger.getLogger(OnlineGame.class.getName());
    private final String serverIP;
    private final int serverPort;
    private final String joinCode;
    private ServerCommunicationTask serverTask;

    public OnlineGame(RulesetOptions selectedRuleset, String playerWhiteName, String playerBlackName, Map<String, String> onlineGameSettings) {
        super(selectedRuleset, playerWhiteName, playerBlackName);
        this.serverIP = onlineGameSettings.get("ip");
        this.serverPort = Integer.parseInt(onlineGameSettings.get("port"));
        this.joinCode = onlineGameSettings.get("joinCode");
        startServerCommunication();
    }

    private void startServerCommunication() {
        serverTask = new ServerCommunicationTask(serverIP, serverPort, joinCode, this);
        Thread serverThread = new Thread(serverTask);
        serverThread.start();
    }

    public void handleMessage(Message message) {
        // Implement logic to handle incoming messages
    }

    public void sendMessageToServer(Message message) {
        if (serverTask != null) {
            serverTask.sendMessage(message);
        }
    }

    @Override
    public void endGame() {
        if (serverTask != null) {
            serverTask.closeConnection();
        }
    }

    @Override
    public void movePiece(Square squareStart, Square squareEnd) throws IllegalMoveException {
        // Implement move logic
    }

    @Override
    protected void executeMove(Square squareStart, Square squareEnd, Move move) throws IllegalMoveException {
        // Implement move execution logic
    }
}