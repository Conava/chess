package ptp.server;

import ptp.core.data.io.MessageParser;
import ptp.core.logic.ruleset.RulesetOptions;
import ptp.server.management.ClientHandler;
import ptp.core.logic.game.ServerGame;
import ptp.server.management.GameInstance;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
    private static final int THREAD_POOL_SIZE = 40;
    private static final Map<GameInstance, ClientHandler[]> games = new HashMap<>();
    private static volatile boolean running = true;
    private static final Logger logger = Logger.getLogger(Server.class.getName());

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java ptp.server.Server <port number>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        MessageParser messageParser = new MessageParser();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            logger.info("Server started on port " + port);

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
                    executorService.execute(new ClientHandler(clientSocket, messageParser, games));
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