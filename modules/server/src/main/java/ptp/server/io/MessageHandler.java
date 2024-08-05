package ptp.server.io;

import ptp.core.data.io.Message;
import ptp.core.logic.game.ServerGame;
import ptp.server.management.ClientHandler;
import ptp.server.management.GameInstance;

import java.util.Map;

public class MessageHandler {
    private final Map<String, ServerGame> games;

    public MessageHandler(Map<GameInstance, ClientHandler[]> games) {
        this.games = games;
    }

    public void handleMessage(ClientHandler clientHandler, Message message) {
        ServerGame game = games.get(clientHandler);
        if (game != null) {
            game.processMessage(message);
        } else {
            // Handle case where no game exists for the IP address
        }
    }
}