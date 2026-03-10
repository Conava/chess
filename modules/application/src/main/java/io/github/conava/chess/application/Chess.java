package io.github.conava.chess.application;

import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.navigation.SceneManager;
import io.github.conava.chess.application.settings.SettingsService;
import io.github.conava.chess.application.theme.ThemeManager;
import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.board.Board;
import io.github.conava.chess.core.data.io.Message;
import io.github.conava.chess.core.data.io.MessageParser;
import io.github.conava.chess.core.data.io.MessageType;
import io.github.conava.chess.core.data.pieces.Piece;
import io.github.conava.chess.core.data.pieces.Pieces;
import io.github.conava.chess.core.data.player.Player;
import io.github.conava.chess.core.exceptions.IllegalMoveException;
import io.github.conava.chess.core.logic.game.Game;
import io.github.conava.chess.core.logic.game.GameState;
import io.github.conava.chess.core.logic.observer.GameObserver;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import io.github.conava.chess.application.network.ServerCommunicationTask;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entry point and façade for the Chess application.
 *
 * <p>Extends {@link Application} for JavaFX lifecycle management while also serving
 * as the single approved API surface between the UI layer and the {@code core} module
 * (Architecture Law 2). All game interaction must go through this class.</p>
 *
 * <p>Authentication state (token, user ID, username) is loaded from {@link SettingsService}
 * on startup and updated on login/logout. The {@link #login} and {@link #register} methods
 * open a short-lived TCP connection to the server on a daemon thread, send the auth request,
 * read one response, and invoke the callback on the FX Application Thread.</p>
 */
public class Chess extends Application {

    private static final Logger LOGGER = Logger.getLogger(Chess.class.getName());

    /** Active game, or {@code null} when no game is running. */
    private Game game;

    /** Active server communication task used for matchmaking messages, or {@code null}. */
    private ServerCommunicationTask activeServerTask;

    /** Persistent settings service elevated to a field so all facade methods can access it. */
    private SettingsService settingsService;

    // ── Auth state ────────────────────────────────────────────────────────────

    /** Current auth token, or {@code null} when not authenticated. */
    private String authToken;

    /** Current authenticated user's ID, or {@code 0} when not authenticated. */
    private int userId;

    /** Current authenticated user's username, or an empty string when not authenticated. */
    private String username;

    /**
     * No-arg constructor required by JavaFX Application and for unit tests.
     */
    public Chess() {
    }

    // ── JavaFX entry point ────────────────────────────────────────────────────

    /**
     * Application entry point. Launches the JavaFX runtime.
     *
     * @param args command-line arguments; passing {@code "nogui"} starts without a window.
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * JavaFX lifecycle method. Initialises services, loads persisted auth state, and shows
     * the main menu (unless the application was started in no-GUI mode).
     *
     * @param primaryStage the primary stage provided by the JavaFX runtime.
     */
    @Override
    public void start(Stage primaryStage) {
        List<String> params = getParameters().getUnnamed();
        if (params.contains("nogui")) {
            LOGGER.log(Level.INFO, "Chess application started without GUI");
            return;
        }
        LOGGER.log(Level.INFO, "Chess application started with GUI");

        settingsService = new SettingsService();
        ThemeManager themeManager = new ThemeManager();
        I18n i18n = new I18n(settingsService.loadLanguage());

        themeManager.setTheme(settingsService.loadTheme());
        themeManager.setBoardTheme(settingsService.loadBoardTheme());

        // Restore persisted auth state
        String storedToken = settingsService.loadAuthToken();
        if (storedToken != null && !storedToken.isEmpty()) {
            authToken = storedToken;
            userId = settingsService.loadUserId();
            username = settingsService.loadUsername();
            LOGGER.log(Level.INFO, "Restored auth session for user: {0}", username);
        }

        SceneManager sceneManager = new SceneManager(
                primaryStage, this, themeManager, i18n, settingsService);

        primaryStage.setTitle("Chess");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(650);
        sceneManager.showMainMenu();
    }

    // ── Auth facade ───────────────────────────────────────────────────────────

    /**
     * Authenticates the user against the server.
     *
     * <p>Opens a short-lived TCP connection on a background daemon thread, sends a
     * {@code LOGIN} message, waits for one response, and invokes {@code callback} on the
     * FX Application Thread with {@code true} on success or {@code false} on failure.
     * On success, the auth token, user ID, and username are persisted to {@link SettingsService}.</p>
     *
     * @param serverIp   IP address of the chess server.
     * @param serverPort TCP port of the chess server.
     * @param username   Username to authenticate with.
     * @param password   Password to authenticate with.
     * @param callback   Invoked on the FX Application Thread with {@code true} on success,
     *                   {@code false} on failure or network error.
     */
    public void login(String serverIp, int serverPort, String username, String password,
                      Consumer<Boolean> callback) {
        String body = "username=" + username + " password=" + password;
        Message request = new Message(MessageType.LOGIN, body);
        sendAuthRequest(serverIp, serverPort, request, response -> {
            if (response != null && response.type() == MessageType.AUTH_TOKEN) {
                applyAuthToken(response, username);
                Platform.runLater(() -> callback.accept(true));
            } else {
                Platform.runLater(() -> callback.accept(false));
            }
        });
    }

    /**
     * Registers a new user account on the server.
     *
     * <p>Opens a short-lived TCP connection on a background daemon thread, sends a
     * {@code REGISTER} message, waits for one response, and invokes {@code callback} on the
     * FX Application Thread with {@code true} on success or {@code false} on failure.
     * On success, the auth token, user ID, and username are persisted to {@link SettingsService}.</p>
     *
     * @param serverIp   IP address of the chess server.
     * @param serverPort TCP port of the chess server.
     * @param username   Desired username.
     * @param password   Desired password.
     * @param callback   Invoked on the FX Application Thread with {@code true} on success,
     *                   {@code false} on failure or network error.
     */
    public void register(String serverIp, int serverPort, String username, String password,
                         Consumer<Boolean> callback) {
        String body = "username=" + username + " password=" + password;
        Message request = new Message(MessageType.REGISTER, body);
        sendAuthRequest(serverIp, serverPort, request, response -> {
            if (response != null && response.type() == MessageType.AUTH_TOKEN) {
                applyAuthToken(response, username);
                Platform.runLater(() -> callback.accept(true));
            } else {
                Platform.runLater(() -> callback.accept(false));
            }
        });
    }

    /**
     * Clears all auth state and removes persisted auth data from {@link SettingsService}.
     */
    public void logout() {
        authToken = null;
        userId = 0;
        username = "";
        if (settingsService != null) {
            settingsService.clearAuthToken();
        }
        LOGGER.log(Level.INFO, "User logged out");
    }

    /**
     * Returns the current auth token.
     *
     * @return the auth token, or {@code null} when not authenticated.
     */
    public String getAuthToken() {
        return authToken;
    }

    /**
     * Returns the current authenticated user's ID.
     *
     * @return the user ID, or {@code 0} when not authenticated.
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Returns the current authenticated user's username.
     *
     * @return the username, or an empty string when not authenticated.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns whether the user is currently authenticated.
     *
     * @return {@code true} if an auth token is present and non-empty.
     */
    public boolean isAuthenticated() {
        return authToken != null && !authToken.isEmpty();
    }

    // ── Matchmaking facade ────────────────────────────────────────────────────

    /**
     * Returns the active {@link ServerCommunicationTask} for the current online game, or
     * {@code null} when no online game is in progress.
     *
     * @return the active {@link ServerCommunicationTask}, or {@code null}.
     */
    public ServerCommunicationTask getActiveServerTask() {
        return activeServerTask;
    }

    /**
     * Sends a {@code CHAT} message to the opponent via the active server connection.
     *
     * <p>The message body is encoded as {@code "content=<text>"}. Callers should validate that
     * {@code content} is non-blank before invoking this method.</p>
     *
     * @param content the chat message text to send; must not be {@code null}.
     * @throws IllegalStateException if there is no active server connection.
     */
    public void sendChat(String content) {
        if (activeServerTask == null || !activeServerTask.isConnected()) {
            throw new IllegalStateException("Not connected to server");
        }
        Message msg = new Message(MessageType.CHAT, "content=" + content);
        activeServerTask.sendMessage(MessageParser.serialize(msg));
    }

    /**
     * Sends a {@code SAVE_GAME} message to the server, requesting that the current game be
     * saved and ended. The opponent receives the request and may accept or decline.
     *
     * @throws IllegalStateException if there is no active server connection.
     */
    public void requestSaveGame() {
        if (activeServerTask == null || !activeServerTask.isConnected()) {
            throw new IllegalStateException("Not connected to server");
        }
        Message msg = new Message(MessageType.SAVE_GAME, "token=" + authToken);
        activeServerTask.sendMessage(MessageParser.serialize(msg));
    }

    /**
     * Opens a persistent TCP connection to the chess server for matchmaking or other
     * pre-game communication and stores the resulting task as the active server task.
     *
     * <p>The connection is established synchronously on a background daemon thread.
     * This method blocks until the connection is confirmed or fails. After this call
     * {@link #getActiveServerTask()} returns the live task (or {@code null} if the
     * connection failed).</p>
     *
     * <p>If an active server task already exists it is closed and replaced.</p>
     *
     * @param serverIp   IP address of the chess server.
     * @param serverPort TCP port of the chess server.
     * @return {@code true} if the connection was established successfully, {@code false} otherwise.
     */
    public boolean connectToServer(String serverIp, int serverPort) {
        if (activeServerTask != null && activeServerTask.isConnected()) {
            activeServerTask.closeConnection();
        }
        CountDownLatch latch = new CountDownLatch(1);
        ServerCommunicationTask task = new ServerCommunicationTask(
                serverIp, serverPort, latch, msg -> { /* matchmaking messages handled via setMatchHandler */ });
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (task.isConnected()) {
            activeServerTask = task;
            return true;
        }
        return false;
    }

    /**
     * Joins a matched online game identified by a server-provided join code.
     *
     * <p>This convenience method is used by the matchmaking waiting screen after the server
     * responds with a {@code MATCHED} message. It calls
     * {@link #startGame(boolean, RulesetOptions, String, String, Map)} with the join-code
     * connection details and the {@code "You"} / {@code "Opponent"} player names.</p>
     *
     * @param serverIp   IP address of the chess server.
     * @param serverPort TCP port of the chess server.
     * @param joinCode   the join code provided by the server in the {@code MATCHED} message.
     * @param ruleset    the ruleset agreed upon during matchmaking.
     */
    public void joinOnlineGame(String serverIp, int serverPort, String joinCode, RulesetOptions ruleset) {
        Map<String, String> opts = Map.of(
                "ip", serverIp,
                "port", String.valueOf(serverPort),
                "joinCode", joinCode);
        startGame(true, ruleset, "You", "Opponent", opts);
    }

    /**
     * Sends a {@code QUEUE} message to the server via the active server connection,
     * requesting to join the matchmaking queue for the given ruleset.
     *
     * @param ruleset the ruleset to queue for.
     * @throws IllegalStateException if there is no active server connection.
     */
    public void joinMatchmakingQueue(RulesetOptions ruleset) {
        if (activeServerTask == null || !activeServerTask.isConnected()) {
            throw new IllegalStateException("Not connected to server");
        }
        String body = "token=" + authToken + " ruleset=" + ruleset.name();
        Message msg = new Message(MessageType.QUEUE, body);
        activeServerTask.sendMessage(MessageParser.serialize(msg));
    }

    /**
     * Sends a {@code DEQUEUE} message to the server via the active server connection,
     * requesting to leave the matchmaking queue.
     *
     * @throws IllegalStateException if there is no active server connection.
     */
    public void leaveMatchmakingQueue() {
        if (activeServerTask == null || !activeServerTask.isConnected()) {
            throw new IllegalStateException("Not connected to server");
        }
        Message msg = new Message(MessageType.DEQUEUE, "token=" + authToken);
        activeServerTask.sendMessage(MessageParser.serialize(msg));
    }

    /**
     * Sends a {@code RESUME_GAME} message to the server to resume a previously saved game.
     *
     * @param gameId the ID of the saved game to resume.
     * @throws IllegalStateException if there is no active server connection.
     */
    public void resumeSavedGame(int gameId) {
        if (activeServerTask == null || !activeServerTask.isConnected()) {
            throw new IllegalStateException("Not connected to server");
        }
        String body = "token=" + authToken + " gameId=" + gameId;
        Message msg = new Message(MessageType.RESUME_GAME, body);
        activeServerTask.sendMessage(MessageParser.serialize(msg));
    }

    // ── Game facade ───────────────────────────────────────────────────────────

    /**
     * Starts a new game (online or offline).
     *
     * <p>If a game is already running, logs a warning and does nothing. For online games,
     * the active {@link ServerCommunicationTask} is stored so matchmaking methods can
     * send messages over the same connection.</p>
     *
     * @param online              {@code true} for an online game, {@code false} for offline.
     * @param selectedRuleset     the ruleset to use.
     * @param playerWhiteName     white player's display name.
     * @param playerBlackName     black player's display name.
     * @param onlineGameSettings  additional settings (IP, port, join code, etc.) for online games;
     *                            ignored for offline games.
     */
    public void startGame(boolean online,
                          RulesetOptions selectedRuleset,
                          String playerWhiteName,
                          String playerBlackName,
                          Map<String, String> onlineGameSettings) {
        if (game == null) {
            if (online) {
                game = createOnlineGame(selectedRuleset, playerWhiteName,
                        playerBlackName, onlineGameSettings);
            } else {
                game = Game.createGame(false, selectedRuleset,
                        playerWhiteName, playerBlackName, null, null);
            }
            game.startGame();
        } else {
            LOGGER.log(Level.WARNING, "Game is already running");
        }
    }

    private Game createOnlineGame(RulesetOptions selectedRuleset,
                                  String playerWhiteName,
                                  String playerBlackName,
                                  Map<String, String> onlineGameSettings) {
        String serverIP = onlineGameSettings.get("ip");
        int serverPort = Integer.parseInt(onlineGameSettings.get("port"));

        CountDownLatch connectionLatch = new CountDownLatch(1);
        CountDownLatch gameReadyLatch = new CountDownLatch(1);

        Game[] gameHolder = new Game[1];
        Consumer<Message> handler = msg -> {
            try {
                gameReadyLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            gameHolder[0].handleMessage(msg);
        };

        ServerCommunicationTask task = new ServerCommunicationTask(
                serverIP, serverPort, connectionLatch, handler);
        Thread serverThread = new Thread(task);
        serverThread.setDaemon(true);
        serverThread.start();

        try {
            connectionLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Game onlineGame = Game.createGame(true, selectedRuleset,
                playerWhiteName, playerBlackName, onlineGameSettings, task);
        gameHolder[0] = onlineGame;
        gameReadyLatch.countDown();

        if (!task.isConnected()) {
            onlineGame.setGameState(GameState.SERVER_ERROR);
            return onlineGame;
        }
        activeServerTask = task;
        onlineGame.connectToServerGame();
        return onlineGame;
    }

    /**
     * Returns the current game state.
     *
     * @return the current {@link GameState}, or {@code null} when no game is active.
     */
    public GameState getState() {
        return game == null ? null : game.getState();
    }

    /**
     * Returns the current board.
     *
     * @return the {@link Board}, or {@code null} when no game is active.
     */
    public Board getBoard() {
        return game == null ? null : game.getBoard();
    }

    /**
     * Returns the player whose turn it currently is.
     *
     * @return the current {@link Player}, or {@code null} when no game is active.
     */
    public Player getCurrentPlayer() {
        return game == null ? null : game.getCurrentPlayer();
    }

    /**
     * Returns the white player.
     *
     * @return the white {@link Player}, or {@code null} when no game is active.
     */
    public Player getPlayerWhite() {
        return game == null ? null : game.getPlayerWhite();
    }

    /**
     * Returns the black player.
     *
     * @return the black {@link Player}, or {@code null} when no game is active.
     */
    public Player getPlayerBlack() {
        return game == null ? null : game.getPlayerBlack();
    }

    /**
     * Returns the join code for the current online game.
     *
     * @return the join code string, or {@code null} when no game is active or for offline games.
     */
    public String getJoinCode() {
        return game != null ? game.getJoinCode() : null;
    }

    /**
     * Returns a human-readable label for the current game's ruleset.
     *
     * @return the game label, or an empty string when no game is active.
     */
    public String getGameLabel() {
        return game != null ? game.getRuleset() != null ? game.getRuleset().getGameLabel() : "" : "";
    }

    /**
     * Returns the piece at the given board position.
     *
     * @param position the target square.
     * @return the {@link Piece} at that position, or {@code null} when no game is active.
     */
    public Piece getPieceAt(Square position) {
        return game == null ? null : game.getPieceAt(position);
    }

    /**
     * Returns the list of legal destination squares for the piece at the given position.
     *
     * @param position the source square.
     * @return list of legal target squares; empty when no game is active.
     */
    public List<Square> getLegalSquares(Square position) {
        return game == null ? Collections.emptyList() : game.getLegalSquares(position);
    }

    /**
     * Returns the move list in algebraic notation.
     *
     * @return list of move strings; empty when no game is active.
     */
    public List<String> getMoveList() {
        return game == null ? Collections.emptyList() : game.getMoveList();
    }

    /**
     * Registers a {@link GameObserver} to receive game state change notifications.
     *
     * @param observer the observer to register.
     * @throws IllegalStateException if no game is active.
     */
    public void addObserver(GameObserver observer) {
        if (game == null) throw new IllegalStateException("No active game");
        game.addObserver(observer);
    }

    /**
     * Ends the current game and releases the game reference.
     * No-op if no game is active.
     */
    public void endGame() {
        if (game != null) {
            game.endGame();
            game = null;
            activeServerTask = null;
        }
    }

    /**
     * Moves a piece from {@code start} to {@code end}.
     *
     * @param start the source square.
     * @param end   the destination square.
     * @throws IllegalMoveException  if the move is illegal.
     * @throws IllegalStateException if no game is active.
     */
    public void movePiece(Square start, Square end) throws IllegalMoveException {
        if (game == null) throw new IllegalStateException("No active game");
        game.movePiece(start, end);
    }

    /**
     * Executes a promotion move.
     *
     * @param start       the source square.
     * @param end         the destination square.
     * @param targetPiece the piece type to promote to.
     * @throws IllegalMoveException  if the move is illegal.
     * @throws IllegalStateException if no game is active.
     */
    public void promoteMove(Square start, Square end, Pieces targetPiece)
            throws IllegalMoveException {
        if (game == null) throw new IllegalStateException("No active game");
        game.promoteMove(start, end, targetPiece);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * Opens a short-lived TCP connection on a background daemon thread, sends the given
     * {@code request} message, reads one response line, and forwards the parsed
     * {@link Message} (or {@code null} on error) to {@code responseHandler}.
     *
     * @param serverIp        server IP address.
     * @param serverPort      server TCP port.
     * @param request         the auth request message to send.
     * @param responseHandler receives the server's response {@link Message}, or {@code null}
     *                        if a network error occurred.
     */
    private void sendAuthRequest(String serverIp, int serverPort, Message request,
                                 Consumer<Message> responseHandler) {
        Thread t = new Thread(() -> {
            try (Socket socket = new Socket(serverIp, serverPort);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(socket.getInputStream()))) {

                out.println(MessageParser.serialize(request));
                String raw = reader.readLine();
                if (raw != null) {
                    try {
                        responseHandler.accept(MessageParser.parse(raw));
                    } catch (RuntimeException e) {
                        LOGGER.log(Level.SEVERE, "Failed to parse auth response: " + raw, e);
                        responseHandler.accept(null);
                    }
                } else {
                    responseHandler.accept(null);
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Auth request failed", e);
                responseHandler.accept(null);
            }
        });
        t.setDaemon(true);
        t.start();
    }

    /**
     * Applies a successful {@code AUTH_TOKEN} server response to the local auth state
     * and persists it via {@link SettingsService}.
     *
     * <p>The response body is expected to contain space-separated {@code key=value} pairs.
     * Required keys: {@code token}, {@code userId}, {@code username} (optional — falls back
     * to the supplied {@code fallbackUsername}).</p>
     *
     * @param response        the server's {@link MessageType#AUTH_TOKEN} message.
     * @param fallbackUsername the username supplied by the caller, used if the server response
     *                         does not include a {@code username} field.
     */
    private void applyAuthToken(Message response, String fallbackUsername) {
        String token = response.getParameterValue("token");
        String userIdStr = response.getParameterValue("userId");
        String serverUsername = response.getParameterValue("username");

        authToken = token;
        userId = 0;
        if (userIdStr != null) {
            try {
                userId = Integer.parseInt(userIdStr);
            } catch (NumberFormatException e) {
                LOGGER.log(Level.WARNING, "Could not parse userId from auth response: " + userIdStr);
            }
        }
        username = (serverUsername != null && !serverUsername.isEmpty())
                ? serverUsername : fallbackUsername;

        if (settingsService != null) {
            settingsService.saveAuthToken(authToken);
            settingsService.saveUserId(userId);
            settingsService.saveUsername(username);
        }

        LOGGER.log(Level.INFO, "Auth token applied for user: {0}", username);
    }
}
