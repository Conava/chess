package io.github.conava.chess.server;

import io.github.conava.chess.core.data.io.Message;
import io.github.conava.chess.core.data.io.MessageType;
import io.github.conava.chess.server.management.ClientHandler;
import io.github.conava.chess.server.management.GameInstance;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Server class manages all client connections and game instances for the chess server.
 *
 * <p>This class is instance-based. A single {@code Server} instance owns all mutable
 * server state: the game semaphore, the active-games map, the connected-clients set, and
 * the game-ID counter. The static {@code main} method creates one instance and delegates
 * to {@link #start(int)} to begin accepting connections.
 *
 * <p>Lifecycle:
 * <ol>
 *   <li>{@code main} parses the port and creates a {@code Server}.</li>
 *   <li>{@link #start(int)} opens the server socket, spawns the console-listener daemon
 *       thread, and enters the accept loop.</li>
 *   <li>Each accepted socket is handed to a {@link ClientHandler} that receives a
 *       reference to this {@code Server} instance so it can call
 *       {@link #addClientHandler}/{@link #removeClientHandler} and access shared state
 *       via the getter methods.</li>
 * </ol>
 */
public class Server {

    private static final int MAX_GAMES = 40;
    private static final int DEFAULT_PORT = 54321;
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());

    private final Semaphore gameSemaphore;
    private final Map<Integer, GameInstance> gamesList;
    private final Set<ClientHandler> connectionsList;
    private final AtomicInteger gameIdCounter;
    private volatile boolean running;

    /**
     * Constructs a new {@code Server} instance and initialises all instance fields.
     *
     * <p>After construction the server is not yet listening; call {@link #start(int)} to
     * open the server socket and begin accepting client connections.
     */
    public Server() {
        this.gameSemaphore = new Semaphore(MAX_GAMES);
        this.gamesList = new ConcurrentHashMap<>();
        this.connectionsList = new CopyOnWriteArraySet<>();
        this.gameIdCounter = new AtomicInteger(0);
        this.running = true;
    }

    /**
     * Entry point for the chess server process.
     *
     * <p>Parses an optional port argument, creates a {@code Server} instance, and calls
     * {@link #start(int)}.
     *
     * @param args optional single element containing the port number as a decimal string;
     *             if absent or invalid, the default port {@value #DEFAULT_PORT} is used
     */
    public static void main(String[] args) {
        int port = getPort(args);
        new Server().start(port);
    }

    /**
     * Opens the server socket on the given port and begins accepting client connections.
     *
     * <p>This method blocks until the server is stopped (via the {@code stop} console
     * command or an unrecoverable {@link IOException}). Before entering the accept loop
     * a daemon thread is started to listen for console commands ({@code stop}, {@code stats}).
     *
     * @param port TCP port to listen on; must be in the range 1&ndash;65535
     */
    public void start(int port) {
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
     * Parses and validates a port number from the command-line arguments.
     *
     * <p>If no arguments are supplied, the argument is non-numeric, negative, or greater
     * than 65535, a warning is logged and the default port {@value #DEFAULT_PORT} is
     * returned.
     *
     * @param args command-line arguments as passed to {@code main}
     * @return a valid TCP port number in the range 1&ndash;65535
     */
    private static int getPort(String[] args) {
        if (args.length != 1) {
            LOGGER.info("Starting server on default port " + DEFAULT_PORT);
            return DEFAULT_PORT;
        }
        try {
            int port = Integer.parseInt(args[0]);
            if (port < 1 || port > 65535) {
                LOGGER.warning("Port " + port + " is out of range (1-65535); using default port " + DEFAULT_PORT);
                return DEFAULT_PORT;
            }
            LOGGER.info("Starting server on port " + port);
            return port;
        } catch (NumberFormatException e) {
            LOGGER.warning("Invalid port argument '" + args[0] + "'; using default port " + DEFAULT_PORT);
            return DEFAULT_PORT;
        }
    }

    /**
     * Starts a daemon thread that reads console commands from {@code System.in}.
     *
     * <p>Recognised commands:
     * <ul>
     *   <li>{@code stop} &mdash; shuts down the server gracefully.</li>
     *   <li>{@code stats} &mdash; prints current connection and game counts.</li>
     * </ul>
     *
     * <p>The thread is set as a daemon so that it does not prevent the JVM from exiting
     * once all non-daemon threads (i.e., the accept loop and client handlers) have
     * terminated.
     *
     * @param executorService the executor used to shut down client-handler threads
     * @param serverSocket    the open server socket (closed on {@code stop})
     */
    private void startConsoleCommandListener(ExecutorService executorService, ServerSocket serverSocket) {
        Thread thread = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (running) {
                String command = scanner.nextLine().trim();
                if (command.equalsIgnoreCase("stop")) {
                    stopServer(executorService, serverSocket);
                } else if (command.equalsIgnoreCase("stats")) {
                    printServerStatus();
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Enters the accept loop and dispatches each accepted connection to a new
     * {@link ClientHandler} submitted to the given executor.
     *
     * @param executorService the thread pool used to run each {@link ClientHandler}
     * @param serverSocket    the server socket to accept connections from
     */
    private void acceptClientConnections(ExecutorService executorService, ServerSocket serverSocket) {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                executorService.execute(new ClientHandler(clientSocket, this));
            } catch (IOException e) {
                if (running) {
                    LOGGER.log(Level.SEVERE, "Error accepting client connection", e);
                }
            }
        }
    }

    /**
     * Prints the current server status (running flag, total games, active connections)
     * to standard output.
     */
    private void printServerStatus() {
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
     * Stops the server: notifies all connected clients, closes the server socket, and
     * shuts down the executor service.
     *
     * @param executorService the executor managing client-handler threads
     * @param serverSocket    the server socket to close
     */
    private void stopServer(ExecutorService executorService, ServerSocket serverSocket) {
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
     * Registers a {@link ClientHandler} as an active connection.
     *
     * <p>Called by {@link ClientHandler#run()} immediately after the client socket is
     * accepted and I/O streams are ready.
     *
     * @param clientHandler the handler to register; must not be {@code null}
     */
    public void addClientHandler(ClientHandler clientHandler) {
        connectionsList.add(clientHandler);
    }

    /**
     * Unregisters a {@link ClientHandler} from the active-connections set.
     *
     * <p>Called by {@link ClientHandler} during cleanup when the client disconnects.
     *
     * @param clientHandler the handler to remove; no-op if not present
     */
    public void removeClientHandler(ClientHandler clientHandler) {
        connectionsList.remove(clientHandler);
    }

    /**
     * Returns an unmodifiable view of the active-games map.
     *
     * <p>Keys are game IDs; values are the corresponding {@link GameInstance} objects.
     * The returned map is an unmodifiable wrapper around the internal
     * {@link ConcurrentHashMap}; callers may safely iterate or look up entries but
     * cannot call {@code put}, {@code remove}, or any other mutating operation (those
     * throw {@link UnsupportedOperationException}).</p>
     *
     * <p>To add or remove games, use {@link #addGame(int, GameInstance)} and
     * {@link #removeGame(int)} respectively. To look up a single game by ID, prefer
     * {@link #getGame(int)}.</p>
     *
     * @return an unmodifiable view of the game ID to {@link GameInstance} map
     */
    public Map<Integer, GameInstance> getGamesList() {
        return Collections.unmodifiableMap(gamesList);
    }

    /**
     * Adds a {@link GameInstance} to the active-games map.
     *
     * <p>This method is the only sanctioned way to insert a new entry into the internal
     * games map. It must be called after a game is created and before the join code is
     * sent to the client so that a second client can look up the game via
     * {@link #getGame(int)}.</p>
     *
     * @param gameId the unique numeric identifier for the game; must be positive
     * @param game   the {@link GameInstance} to register; must not be {@code null}
     */
    public void addGame(int gameId, GameInstance game) {
        gamesList.put(gameId, game);
    }

    /**
     * Removes a {@link GameInstance} from the active-games map.
     *
     * <p>This method is the only sanctioned way to remove an entry from the internal
     * games map. It is called during client cleanup after the game has ended or the
     * player has disconnected. If no entry exists for the given {@code gameId} this
     * method is a no-op.</p>
     *
     * @param gameId the unique numeric identifier of the game to remove
     */
    public void removeGame(int gameId) {
        gamesList.remove(gameId);
    }

    /**
     * Returns the {@link GameInstance} associated with the given game ID, or
     * {@code null} if no such game is currently active.
     *
     * <p>This method is the preferred alternative to
     * {@code getGamesList().get(gameId)} because it does not expose the internal map
     * reference to callers.</p>
     *
     * @param gameId the unique numeric identifier of the game to retrieve
     * @return the {@link GameInstance} for that ID, or {@code null} if not found
     */
    public GameInstance getGame(int gameId) {
        return gamesList.get(gameId);
    }

    /**
     * Returns the {@link Semaphore} that limits the number of concurrent games.
     *
     * <p>The semaphore is initialised with {@value #MAX_GAMES} permits. A permit is
     * acquired in {@link ClientHandler#createGame(io.github.conava.chess.core.data.io.Message)}
     * and released in {@link ClientHandler#releaseGameSlot()}.
     *
     * @return the game-slot semaphore
     */
    public Semaphore getGameSemaphore() {
        return gameSemaphore;
    }

    /**
     * Returns the {@link AtomicInteger} used to generate unique game IDs.
     *
     * <p>Each call to {@link ClientHandler#createGame(io.github.conava.chess.core.data.io.Message)}
     * increments this counter to obtain a unique ID for the new {@link GameInstance}.
     *
     * @return the game-ID counter
     */
    public AtomicInteger getGameIdCounter() {
        return gameIdCounter;
    }
}
