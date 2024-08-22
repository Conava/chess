package ptp.server;

import ptp.core.data.io.Message;
import ptp.core.data.io.MessageType;
import ptp.server.management.ClientHandler;
import ptp.server.management.GameInstance;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Server class is responsible for managing client connections and game instances.
 * It uses a semaphore to limit the number of concurrent games and an executor service
 * to handle client connections in separate threads.
 */
public class Server {
    private static final int MAX_GAMES = 40;
    private static final Semaphore gameSemaphore = new Semaphore(MAX_GAMES);
    private static final Map<Integer, GameInstance> gamesList = new ConcurrentHashMap<>();
    private static final Set<ClientHandler> connectionsList = new CopyOnWriteArraySet<>();
    private static final AtomicInteger gameIdCounter = new AtomicInteger(0);
    private static volatile boolean running = true;
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());

    /**
     * The main method starts the server and listens for client connections.
     *
     * @param args Command line arguments, expects a single argument for the port number.
     */
    public static void main(String[] args) {
        int port = getPort(args);
        ExecutorService executorService = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            LOGGER.info("Server started on port " + port);
            startConsoleCommandListener(executorService, serverSocket);
            acceptClientConnections(executorService, serverSocket);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Server encountered an error", e);
        } finally {
            executorService.shutdown();
        }
    }

    /**
     * Retrieves the port number from the command line arguments.
     *
     * @param args Command line arguments.
     * @return The port number.
     */
    private static int getPort(String[] args) {
        if (args.length != 1) {
            LOGGER.info("Starting server on default port 54321");
            return 54321;
        } else {
            int port = Integer.parseInt(args[0]);
            LOGGER.info("Starting server on port " + port);
            return port;
        }
    }

    /**
     * Starts a thread to listen for console commands.
     *
     * @param executorService The executor service to manage threads.
     * @param serverSocket The server socket.
     */
    private static void startConsoleCommandListener(ExecutorService executorService, ServerSocket serverSocket) {
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (running) {
                String command = scanner.nextLine().trim();
                if (command.equalsIgnoreCase("stop")) {
                    stopServer(executorService, serverSocket);
                } else if (command.equalsIgnoreCase("stats")) {
                    printServerStatus();
                }
            }
        }).start();
    }

    /**
     * Accepts client connections and assigns them to a new ClientHandler.
     *
     * @param executorService The executor service to manage threads.
     * @param serverSocket The server socket.
     */
    private static void acceptClientConnections(ExecutorService executorService, ServerSocket serverSocket) {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                executorService.execute(new ClientHandler(clientSocket, gamesList, gameSemaphore, gameIdCounter));
            } catch (IOException e) {
                if (running) {
                    LOGGER.log(Level.SEVERE, "Error accepting client connection", e);
                }
            }
        }
    }

    /**
     * Prints the current status information of the server.
     */
    private static void printServerStatus() {
        System.out.println("Running: " + running);
        System.out.println("Total Games: " + gamesList.size());
        System.out.println("Active Connections: " + connectionsList.size());
        for (Map.Entry<Integer, GameInstance> entry : gamesList.entrySet()) {
            Integer gameId = entry.getKey();
            GameInstance gameInstance = entry.getValue();
            System.out.println("Game ID: " + gameId);
            System.out.println("  White Player: " + (gameInstance.getWhitePlayerHandler() != null));
            System.out.println("  Black Player: " + (gameInstance.getBlackPlayerHandler() != null));
        }
    }

    /**
     * Stops the server and releases all resources.
     *
     * @param executorService The executor service to manage threads.
     * @param serverSocket The server socket.
     */
    private static void stopServer(ExecutorService executorService, ServerSocket serverSocket) {
        running = false;
        LOGGER.info("Stopping server...");
        for (ClientHandler clientHandler : connectionsList) {
            clientHandler.sendMessage(new Message(MessageType.ERROR, "Server is shutting down"));
            clientHandler.releaseGameSlot();
        }
        try {
            serverSocket.close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error closing server socket", e);
        }
        executorService.shutdown();
        LOGGER.info("Server stopped");
    }

    /**
     * Adds a client handler to the list of active connections.
     *
     * @param clientHandler The client handler to add.
     */
    public static void addClientHandler(ClientHandler clientHandler) {
        connectionsList.add(clientHandler);
    }

    /**
     * Removes a client handler from the list of active connections.
     *
     * @param clientHandler The client handler to remove.
     */
    public static void removeClientHandler(ClientHandler clientHandler) {
        connectionsList.remove(clientHandler);
    }
}