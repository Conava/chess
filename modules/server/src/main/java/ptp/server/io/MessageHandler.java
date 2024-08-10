package ptp.server.io;

import ptp.core.data.io.Message;
import ptp.server.management.ClientHandler;
import ptp.server.management.GameInstance;

import java.util.Map;

public class MessageHandler {
    private final Map<Integer, GameInstance> gamesList;
    private final Map<ClientHandler, Integer> connectionsList;

    public MessageHandler(Map<Integer, GameInstance> gamesList, Map<ClientHandler, Integer> connectionsList) {
        this.gamesList = gamesList;
        this.connectionsList = connectionsList;
    }

    public void handleMessage(ClientHandler clientHandler, Message message) {
        Integer gameId = connectionsList.get(clientHandler);
        if (gameId != null) {
            GameInstance gameInstance = gamesList.get(gameId);
            if (gameInstance != null) {
                gameInstance.processMessage(message);
            } else {
                // Handle case where no game exists for the client handler
            }
        } else {
            // Handle case where client handler is not part of any game
        }
    }
}