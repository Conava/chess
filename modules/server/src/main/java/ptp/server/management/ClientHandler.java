package ptp.server.management;

import ptp.core.logic.game.ServerGame;
import ptp.core.data.io.Message;
import ptp.server.io.MessageHandler;
import ptp.core.data.io.MessageParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final MessageParser messageParser;
    private final MessageHandler messageHandler;
    private PrintWriter out;
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());

    public ClientHandler(Socket clientSocket, MessageParser messageParser, Map<GameInstance, ClientHandler[]> games) {
        this.clientSocket = clientSocket;
        this.messageParser = messageParser;
        this.messageHandler = new MessageHandler(games);
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                Message message = messageParser.parse(inputLine);
                messageHandler.handleMessage(this, message);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error handling client connection", e);
        }
    }

    public void sendMessage(Message message) {
        if (out != null) {
            out.println(messageParser.serialize(message));
        } else {
            logger.log(Level.WARNING, "Output stream is not initialized");
        }
    }
}