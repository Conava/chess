package ptp.server;

import ptp.core.data.io.Message;
import ptp.core.data.io.MessageParser;
import ptp.core.data.io.MessageType;
import ptp.server.management.ClientHandler;
import ptp.server.management.GameInstance;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The Server class is responsible for managing client connections and game instances.
 * It uses a semaphore to limit the number of concurrent games and an executor service
 * to handle client connections in separate threads.
 */
public class Server {
    private static final int MAX_GAMES = 40;
    private static final Semaphore gameSemaphore = new Semaphore(MAX_GAMES);
    private static final Map<Integer, GameInstance> gamesList = new HashMap<>();
    private static final Set<ClientHandler> connectionsList = new CopyOnWriteArraySet<>();
    private static final AtomicInteger gameIdCounter = new AtomicInteger(0);
    private static volatile boolean running = true;
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());

    /**
     * The main method to start the server.
     *
     * @param args Command line arguments, expects a single argument for the port number.
     *             Default fallback port is 54321.
     */
    public static void main(String[] args) {
        int port;
        if (args.length != 1) {
            port = 54321;
            LOGGER.info("Starting server on default port 54321");
        }
        else {
            port = Integer.parseInt(args[0]);
            LOGGER.info("Starting server on port " + port);
        }

        ExecutorService executorService = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            LOGGER.info("Server started on port " + port);

            // Thread to handle server commands from the console
            new Thread(() -> {
                Scanner scanner = new Scanner(System.in);
                while (running) {
                    String command = scanner.nextLine().trim();
                    if (command.equalsIgnoreCase("stop")) {
                        stopServer(executorService, serverSocket);
                    } else if (command.equalsIgnoreCase("stat")) {
                        printServerStatus();
                    }
                }
            }).start();

            // Main server loop to accept client connections
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
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Server encountered an error", e);
        } finally {
            executorService.shutdown();
        }
    }

    /**
     * Prints the current status of the server and active games to the console.
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
     * Stops the server, closes all client connections, and releases resources.
     * All clients will receive an error message before the server is stopped.
     *
     * @param executorService The executor service managing client handler threads.
     * @param serverSocket The server socket to be closed.
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

    public static void addClientHandler(ClientHandler clientHandler) {
        connectionsList.add(clientHandler);
    }

    public static void removeClientHandler(ClientHandler clientHandler) {
        connectionsList.remove(clientHandler);
    }
}