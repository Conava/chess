package ptp.server.management;

import ptp.core.data.io.Message;
import ptp.core.data.io.MessageParser;
import ptp.core.data.io.MessageType;
import ptp.core.logic.ruleset.RulesetOptions;
import ptp.server.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The ClientHandler class handles communication with a client.
 * It processes messages from the client and manages game creation and joining.
 */
public class ClientHandler implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());
    private final Socket clientSocket;
    private final Semaphore gameSemaphore;
    private final Map<Integer, GameInstance> gamesList;
    private final AtomicInteger gameIdCounter;
    private PrintWriter out;
    private GameInstance gameInstance;

    /**
     * Constructs a ClientHandler instance.
     *
     * @param clientSocket The socket connected to the client.
     * @param gamesList The list of active game instances.
     * @param gameSemaphore The semaphore to control game creation.
     * @param gameIdCounter The counter for generating unique game IDs.
     */
    public ClientHandler(Socket clientSocket, Map<Integer, GameInstance> gamesList, Semaphore gameSemaphore, AtomicInteger gameIdCounter) {
        this.clientSocket = clientSocket;
        this.gameSemaphore = gameSemaphore;
        this.gamesList = gamesList;
        this.gameIdCounter = gameIdCounter;
    }

    /**
     * The run method listens for messages from the client and processes them.
     */
    @Override
    public void run() {
        LOGGER.info("Client connected: " + clientSocket.getInetAddress());
        Server.addClientHandler(this);
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            processClientMessages(in);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error handling client connection", e);
        } finally {
            cleanup();
        }
    }

    /**
     * Processes messages from the client.
     *
     * @param in The BufferedReader to read messages from the client.
     * @throws IOException If an I/O error occurs.
     */
    private void processClientMessages(BufferedReader in) throws IOException {
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            Message message = MessageParser.parse(inputLine);
            LOGGER.info("Received message: " + message.type() + "\n" + message.content());
            handleMessage(message);
        }
        LOGGER.info("Connection closed by client " + clientSocket.getInetAddress());
    }

    /**
     * Handles a message from the client.
     *
     * @param message The message to handle.
     */
    private void handleMessage(Message message) {
        switch (message.type()) {
            case CREATE_GAME:
                createGame(message);
                break;
            case JOIN_GAME:
                joinGame(message);
                break;
            default:
                if (gameInstance != null) {
                    gameInstance.processMessage(this, message);
                } else {
                    sendMessage(new Message(MessageType.ERROR, "No game instance available"));
                }
                break;
        }
    }

    /**
     * Sends a message to the client.
     *
     * @param message The message to send.
     */
    public void sendMessage(Message message) {
        if (out != null) {
            out.println(MessageParser.serialize(message));
        } else {
            LOGGER.log(Level.WARNING, "Output stream is not initialized");
        }
    }

    /**
     * Creates a new game instance.
     *
     * @param message The message containing game creation details.
     */
    private void createGame(Message message) {
        if (gameSemaphore.tryAcquire()) {
            int gameId = gameIdCounter.incrementAndGet();
            RulesetOptions ruleset = RulesetOptions.valueOf(message.getParameterValue("ruleset"));
            gameInstance = new GameInstance(gameId, ruleset);
            gameInstance.connectPlayer(this);
            gamesList.put(gameId, gameInstance);
        } else {
            sendMessage(new Message(MessageType.ERROR, "Failed to create game"));
        }
    }

    /**
     * Joins an existing game instance.
     *
     * @param message The message containing game join details.
     */
    private void joinGame(Message message) {
        int gameId = Integer.parseInt(message.content());
        GameInstance gameInstance = gamesList.get(gameId);
        if (gameInstance != null) {
            gameInstance.connectPlayer(this);
        } else {
            sendMessage(new Message(MessageType.ERROR, "Invalid join code"));
        }
    }

    /**
     * Releases the game slot and cleans up resources.
     */
    private void cleanup() {
        LOGGER.log(Level.INFO, "Client disconnected: " + clientSocket.getInetAddress());
        Server.removeClientHandler(this);
        releaseGameSlot();
    }

    /**
     * Releases the game slot if a game instance exists.
     */
    public void releaseGameSlot() {
        if (gameInstance != null) {
            Integer gameId = gameInstance.getGameId();
            gamesList.remove(gameId);
            gameSemaphore.release();
        }
    }
}