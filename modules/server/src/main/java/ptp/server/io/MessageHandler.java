package ptp.server.io;

import ptp.core.data.io.Message;
import ptp.server.management.ClientHandler;
import ptp.server.management.GameInstance;

public class MessageHandler {

    public MessageHandler() {
    }

    public void handleMessage(GameInstance gameInstance, Message message, ClientHandler clientHandler) {
           gameInstance.processMessage(clientHandler, message);
    }
}