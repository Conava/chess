package ptp.server.management;

import ptp.core.data.io.Message;
import ptp.core.data.io.MessageType;
import ptp.core.logic.ruleset.RulesetOptions;
import ptp.server.Server;
import ptp.core.data.io.MessageParser;

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
    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());
    private final Socket clientSocket;
    private final Semaphore gameSemaphore;
    private final Map<Integer, GameInstance> gamesList;
    private final AtomicInteger gameIdCounter;
    private PrintWriter out;
    private GameInstance gameInstance;

    public ClientHandler(Socket clientSocket, Map<Integer, GameInstance> gamesList, Semaphore gameSemaphore, AtomicInteger gameIdCounter) {
        this.clientSocket = clientSocket;
        this.gameSemaphore = gameSemaphore;
        this.gamesList = gamesList;
        this.gameIdCounter = gameIdCounter;
    }

    @Override
    public void run() {
        LOGGER.info("Client connected: " + clientSocket.getInetAddress());
        Server.addClientHandler(this);
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                Message message = MessageParser.parse(inputLine);
                LOGGER.info("Received message: " + message.type() + "\n" + message.content());
                if (message.type() == MessageType.CREATE_GAME) {
                    createGame(message);
                } else if (message.type() == MessageType.JOIN_GAME) {
                    joinGame(message);
                } else {
                    gameInstance.processMessage(this, message);
                }
            }
            LOGGER.info("Connection closed by client " + clientSocket.getInetAddress());
        } catch (
                IOException e) {
            LOGGER.log(Level.SEVERE, "Error handling client connection", e);
        } finally {
            LOGGER.log(Level.INFO, "Client disconnected: " + clientSocket.getInetAddress());
            Server.removeClientHandler(this);
            releaseGameSlot();
        }
    }

    public void sendMessage(Message message) {
        if (out != null) {
            out.println(MessageParser.serialize(message));
        } else {
            LOGGER.log(Level.WARNING, "Output stream is not initialized");
        }
    }

    private void createGame(Message message) {
        if (gameSemaphore.tryAcquire()) {
            int gameId = gameIdCounter.incrementAndGet();
            RulesetOptions ruleset = RulesetOptions.valueOf(message.getParameterValue("ruleset"));
            gameInstance = new GameInstance(gameId, ruleset);
            gameInstance.connectPlayer(this, message);
            gamesList.put(gameId, gameInstance);}
    }

    private void joinGame(Message message) {
        int gameId = Integer.parseInt(message.content());
        GameInstance gameInstance = gamesList.get(gameId);
        if (gameInstance != null) {
            gameInstance.connectPlayer(this, message);
        } else {
            sendMessage(new Message(MessageType.ERROR, "Invalid join code"));
        }

    }

    public void releaseGameSlot() {
        if (gameInstance != null) {
            Integer gameId = gameInstance.getGameId();
            gamesList.remove(gameId);
            gameSemaphore.release();
        }
    }
}