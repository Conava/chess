package io.github.conava.chess.server;

import io.github.conava.chess.core.data.io.Message;
import io.github.conava.chess.core.data.io.MessageType;
import io.github.conava.chess.server.auth.AuthService;
import io.github.conava.chess.server.config.ServerConfig;
import io.github.conava.chess.server.management.ClientHandler;
import io.github.conava.chess.server.management.GameInstance;
import io.github.conava.chess.server.management.GameManager;
import io.github.conava.chess.server.db.DatabaseManager;
import io.github.conava.chess.server.persistence.GameRepository;
import io.github.conava.chess.server.persistence.SessionRepository;
import io.github.conava.chess.server.persistence.UserRepository;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Server class manages all client connections and game instances for the chess server.
 *
 * <p>This class is instance-based. A single {@code Server} instance owns a
 * {@link GameManager} (which holds the semaphore, ID counter, and games map), a
 * connected-clients set, and the running flag. The static {@code main} method creates a
 * {@link ServerConfig}, then a {@code Server}, and calls {@link #start()} to begin
 * accepting connections.
 *
 * <p>Lifecycle:
 * <ol>
 *   <li>{@code main} loads config, creates a {@code Server}.</li>
 *   <li>{@link #start()} reads the port from {@link ServerConfig}, opens the server socket,
 *       spawns the console-listener daemon thread, and enters the accept loop.</li>
 *   <li>Each accepted socket is handed to a {@link ClientHandler} that receives a
 *       reference to this {@code Server} instance so it can call
 *       {@link #addClientHandler}/{@link #removeClientHandler} and access the
 *       {@link GameManager} via {@link #getGameManager()}.</li>
 * </ol>
 */
public class Server {

    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());

    private final ServerConfig config;
    private final GameManager gameManager;
    private final Set<ClientHandler> connectionsList;
    private volatile boolean running;

    /**
     * Optional authentication service. {@code null} when no auth is configured (e.g., tests
     * that call the no-arg constructor before wiring auth dependencies).
     */
    private AuthService authService;

    /**
     * Optional game persistence repository. {@code null} when no DB backing is configured.
     */
    private GameRepository gameRepository;

    /**
     * Constructs a new {@code Server} instance with a default {@link ServerConfig}.
     *
     * <p>Loads configuration from {@code server.properties} on the classpath (or uses
     * built-in defaults if the file is absent). Equivalent to
     * {@code new Server(new ServerConfig())}.</p>
     *
     * <p>After construction the server is not yet listening; call {@link #start()} to
     * open the server socket and begin accepting client connections.</p>
     */
    public Server() {
        this(new ServerConfig());
    }

    /**
     * Constructs a new {@code Server} instance from the given configuration.
     *
     * <p>After construction the server is not yet listening; call {@link #start()} to
     * open the server socket and begin accepting client connections.</p>
     *
     * @param config the server configuration; must not be {@code null}
     */
    public Server(ServerConfig config) {
        this.config = config;
        this.gameManager = new GameManager(config.getMaxGames());
        this.connectionsList = new CopyOnWriteArraySet<>();
        this.running = true;
    }

    /**
     * Wires the authentication service into this server.
     *
     * <p>When set, every {@link ClientHandler} created by this server will use the given
     * service to validate {@code LOGIN} and {@code REGISTER} requests. If not set (or
     * {@code null}), authentication is unavailable and clients cannot log in.</p>
     *
     * @param authService the {@link AuthService} to use; may be {@code null}
     */
    public void setAuthService(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Wires the game repository into this server.
     *
     * <p>When set, every {@link ClientHandler} created by this server will use the given
     * repository for {@code RESUME_GAME} requests. If not set (or {@code null}), game
     * history is unavailable.</p>
     *
     * @param gameRepository the {@link GameRepository} to use; may be {@code null}
     */
    public void setGameRepository(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    /**
     * Entry point for the chess server process.
     *
     * <p>Loads {@link ServerConfig}, creates a {@code Server} instance, and calls
     * {@link #start()}.</p>
     *
     * @param args optional single element containing the port number as a decimal string;
     *             if absent or invalid, the default port from {@link ServerConfig} is used
     */
    public static void main(String[] args) {
        ServerConfig config = new ServerConfig();
        Server server = new Server(config);

        DatabaseManager dbManager = new DatabaseManager();
        try {
            dbManager.initialize(config.getDbPath());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize database at: " + config.getDbPath(), e);
            return;
        }

        UserRepository userRepository = new UserRepository(dbManager);
        SessionRepository sessionRepository = new SessionRepository(dbManager);
        AuthService authService = new AuthService(userRepository, sessionRepository, config.getSessionExpiryDays());
        GameRepository gameRepository = new GameRepository(dbManager);

        server.setAuthService(authService);
        server.setGameRepository(gameRepository);

        server.start();
    }

    /**
     * Opens the server socket on the port specified by {@link ServerConfig} and begins
     * accepting client connections.
     *
     * <p>This method blocks until the server is stopped (via the {@code stop} console
     * command or an unrecoverable {@link IOException}). Before entering the accept loop
     * a daemon thread is started to listen for console commands ({@code stop}, {@code stats}).</p>
     */
    public void start() {
        int port = config.getPort();
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
     * Starts a daemon thread that reads console commands from {@code System.in}.
     *
     * <p>Recognised commands:
     * <ul>
     *   <li>{@code stop} &mdash; shuts down the server gracefully.</li>
     *   <li>{@code stats} &mdash; logs current connection and game counts.</li>
     * </ul>
     *
     * <p>The thread is set as a daemon so that it does not prevent the JVM from exiting
     * once all non-daemon threads have terminated.
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
                executorService.execute(new ClientHandler(clientSocket, this, gameManager, authService, gameRepository));
            } catch (IOException e) {
                if (running) {
                    LOGGER.log(Level.SEVERE, "Error accepting client connection", e);
                }
            }
        }
    }

    /**
     * Logs the current server status (running flag, total games, active connections).
     */
    private void printServerStatus() {
        LOGGER.info("Running: " + running);
        LOGGER.info("Total Games: " + gameManager.getActiveGames().size());
        LOGGER.info("Active Connections: " + connectionsList.size());
        for (Map.Entry<Integer, GameInstance> entry : gameManager.getActiveGames().entrySet()) {
            Integer gameId = entry.getKey();
            GameInstance gameInstance = entry.getValue();
            LOGGER.info("Game ID: " + gameId);
            LOGGER.info("  White Player: " + (gameInstance.getWhitePlayerHandler() != null));
            LOGGER.info("  Black Player: " + (gameInstance.getBlackPlayerHandler() != null));
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
     * accepted and I/O streams are ready.</p>
     *
     * @param clientHandler the handler to register; must not be {@code null}
     */
    public void addClientHandler(ClientHandler clientHandler) {
        connectionsList.add(clientHandler);
    }

    /**
     * Unregisters a {@link ClientHandler} from the active-connections set.
     *
     * <p>Called by {@link ClientHandler} during cleanup when the client disconnects.</p>
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
     * Delegates to {@link GameManager#getActiveGames()}.</p>
     *
     * @return an unmodifiable view of the game ID to {@link GameInstance} map
     */
    public Map<Integer, GameInstance> getGamesList() {
        return gameManager.getActiveGames();
    }

    /**
     * Adds a {@link GameInstance} to the active-games map.
     *
     * <p>Delegates to {@link GameManager#addGame(int, GameInstance)}.</p>
     *
     * @param gameId the unique numeric identifier for the game; must be positive
     * @param game   the {@link GameInstance} to register; must not be {@code null}
     */
    public void addGame(int gameId, GameInstance game) {
        gameManager.addGame(gameId, game);
    }

    /**
     * Removes a {@link GameInstance} from the active-games map.
     *
     * <p>Delegates to {@link GameManager#removeGame(int)}.</p>
     *
     * @param gameId the unique numeric identifier of the game to remove
     */
    public void removeGame(int gameId) {
        gameManager.removeGame(gameId);
    }

    /**
     * Returns the {@link GameInstance} associated with the given game ID, or
     * {@code null} if no such game is currently active.
     *
     * <p>Delegates to {@link GameManager#getGame(int)}.</p>
     *
     * @param gameId the unique numeric identifier of the game to retrieve
     * @return the {@link GameInstance} for that ID, or {@code null} if not found
     */
    public GameInstance getGame(int gameId) {
        return gameManager.getGame(gameId);
    }

    /**
     * Returns the {@link GameManager} that owns the game-slot semaphore, ID counter,
     * and active-games map.
     *
     * <p>This accessor is provided as a bridge for {@link ClientHandler} until it is
     * fully refactored in T12 to depend directly on {@code GameManager}.</p>
     *
     * @return the {@link GameManager} instance; never {@code null}
     */
    public GameManager getGameManager() {
        return gameManager;
    }
}
