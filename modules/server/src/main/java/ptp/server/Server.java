package ptp.server;

import ptp.server.io.MessageParser;
import ptp.server.management.ClientHandler;
import ptp.server.management.GameManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Server class is responsible for starting the server, accepting client connections,
 * and managing the lifecycle of the server.
 */
public class Server {
    private static final int THREAD_POOL_SIZE = 40; // Maximum of 40 clients connected at the same time
    private static final GameManager gameManager = new GameManager();
    private static volatile boolean running = true;
    private static final Logger logger = Logger.getLogger(Server.class.getName());

    /**
     * The main method to start the server.
     *
     * @param args Command line arguments, expects a single argument for the port number.
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java ptp.server.Server <port number>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        MessageParser messageParser = new MessageParser(gameManager);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            logger.info("Server started on port " + port);

            // Thread for listening to console input
            new Thread(() -> {
                Scanner scanner = new Scanner(System.in);
                while (running) {
                    if (scanner.nextLine().trim().equalsIgnoreCase("stop")) {
                        running = false;
                        try {
                            serverSocket.close();
                        } catch (IOException e) {
                            logger.log(Level.SEVERE, "Error closing server socket", e);
                        }
                        executorService.shutdown();
                        logger.info("Server is shutting down...");
                    }
                }
            }).start();

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    executorService.execute(new ClientHandler(clientSocket, messageParser));
                } catch (IOException e) {
                    if (running) {
                        logger.log(Level.SEVERE, "Error accepting client connection", e);
                    }
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Server encountered an error", e);
        } finally {
            executorService.shutdown();
        }
    }
}