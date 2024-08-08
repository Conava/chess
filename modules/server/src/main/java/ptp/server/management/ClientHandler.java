package ptp.server.management;

import ptp.core.data.io.Message;
import ptp.core.data.io.MessageType;
import ptp.server.io.MessageHandler;
import ptp.core.data.io.MessageParser;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final MessageParser messageParser;
    private final MessageHandler messageHandler;
    private final Semaphore gameSemaphore;
    private final Map<Integer, GameInstance> gamesList;
    private final Map<ClientHandler, Integer> connectionsList;
    private final AtomicInteger gameIdCounter;
    private PrintWriter out;
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());

    public ClientHandler(Socket clientSocket, MessageParser messageParser, Map<Integer, GameInstance> gamesList, Map<ClientHandler, Integer> connectionsList, Semaphore gameSemaphore, AtomicInteger gameIdCounter) {
        this.clientSocket = clientSocket;
        this.messageParser = messageParser;
        this.messageHandler = new MessageHandler(gamesList, connectionsList);
        this.gameSemaphore = gameSemaphore;
        this.gamesList = gamesList;
        this.connectionsList = connectionsList;
        this.gameIdCounter = gameIdCounter;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                Message message = messageParser.parse(inputLine);
                if (message.getType() == MessageType.CREATE_GAME) {
                    if (tryCreateGame()) {
                        sendMessage(new Message(MessageType.SUCCESS, "Game created successfully"));
                    } else {
                        sendMessage(new Message(MessageType.ERROR, "Failed to create game. Maximum limit reached."));
                    }
                } else {
                    messageHandler.handleMessage(this, message);
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error handling client connection", e);
        } finally {
            releaseGameSlot();
        }
    }

    public void sendMessage(Message message) {
        if (out != null) {
            out.println(messageParser.serialize(message));
        } else {
            logger.log(Level.WARNING, "Output stream is not initialized");
        }
    }

    public boolean tryCreateGame() {
        if (gameSemaphore.tryAcquire()) {
            int gameId = gameIdCounter.incrementAndGet();
            GameInstance gameInstance = new GameInstance();
            gamesList.put(gameId, gameInstance);
            connectionsList.put(this, gameId);
            return true;
        }
        return false;
    }

    public void releaseGameSlot() {
        Integer gameId = connectionsList.remove(this);
        if (gameId != null) {
            gamesList.remove(gameId);
            gameSemaphore.release();
        }
    }
}