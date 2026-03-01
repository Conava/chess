package io.github.conava.chess.application;

import io.github.conava.chess.core.data.board.Board;
import io.github.conava.chess.core.data.pieces.Piece;
import io.github.conava.chess.core.data.pieces.Pieces;
import io.github.conava.chess.core.data.player.Player;
import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.exceptions.IllegalMoveException;
import io.github.conava.chess.core.logic.game.Game;
import io.github.conava.chess.core.logic.game.GameState;
import io.github.conava.chess.core.logic.observer.GameObserver;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import io.github.conava.chess.application.network.ServerCommunicationTask;
import io.github.conava.chess.application.window.MainFrame;
import io.github.conava.chess.application.components.ColorScheme;

import io.github.conava.chess.core.data.io.Message;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.logging.*;
import javax.swing.*;

/**
 * Entry point and façade for the Chess application.
 *
 * <p>This class is the single approved API surface between the UI layer and the {@code core}
 * module. All game interaction (start, move, query, observe) must go through this class.
 * Direct instantiation of {@code Game} subclasses by other application code is prohibited
 * (Architecture Law 2). Game instances are created exclusively via
 * {@link Game#createGame(boolean, RulesetOptions, String, String, Map, io.github.conava.chess.core.logic.game.ServerConnection)}.
 *
 * <p>Usage examples:</p>
 * <pre>
 * // Launch with GUI
 * java -jar chess.jar
 *
 * // Launch without GUI (console only)
 * java -jar chess.jar nogui
 * </pre>
 */
public class Chess {
    private static final Logger LOGGER = Logger.getLogger(Chess.class.getName());
    private Game game;

    /**
     * Main method. Determines GUI mode based on first argument.
     *
     * @param args program arguments; pass "nogui" to disable the Swing GUI
     *
     * <p>Example:</p>
     * <pre>
     * Chess.main(new String[] { "nogui" });
     * </pre>
     */
    public static void main(String[] args) {
        boolean guiMode = args.length == 0 || !args[0].equals("nogui");
        SwingUtilities.invokeLater(() -> new Chess(guiMode));
    }

    /**
     * Creates the application instance and launches GUI if requested.
     *
     * @param gui {@code true} to initialize Swing GUI; {@code false} for console-only mode
     *
     * <p>Example:</p>
     * <pre>
     * Chess chessApp = new Chess(true);
     * </pre>
     */
    public Chess(boolean gui) {
        if (gui) {
            LOGGER.log(Level.INFO, "Chess application started with GUI");
            ColorScheme scheme = new ColorScheme(
                    new Font("Arial", Font.PLAIN, 20),
                    new Color(0x2b2d30),
                    new Color(0x3B3F42),
                    new Color(0x27272B),
                    new Color(0xECF0F1),
                    new Color(0x1e1f22),
                    new Color(0x31709A),
                    new Color(0xA31717),
                    new Color(0x808080),
                    new Color(0x762D9A)
            );
            new MainFrame(this, scheme);
        } else {
            LOGGER.log(Level.INFO, "Chess application started without GUI");
        }
    }

    /**
     * Initializes and starts a new game instance.
     *
     * <p>Game construction is delegated to
     * {@link Game#createGame(boolean, RulesetOptions, String, String, Map, io.github.conava.chess.core.logic.game.ServerConnection)}.
     * No {@code Game} subclass is instantiated directly in this method.
     *
     * @param online             {@code true} for online play, {@code false} for offline play
     * @param selectedRuleset    configuration options for the game rules
     * @param playerWhiteName    display name of the white player
     * @param playerBlackName    display name of the black player
     * @param onlineGameSettings key-value settings for online matchmaking or server connection;
     *                           may be {@code null} when {@code online} is {@code false}
     *
     * <p>Example for offline:</p>
     * <pre>
     * RulesetOptions opts = RulesetOptions.STANDARD;
     * chess.startGame(false, opts, "Alice", "Bob", null);
     * </pre>
     *
     * <p>Example for online:</p>
     * <pre>
     * Map&lt;String,String&gt; settings = Map.of("ip","game.example.com","port","1234");
     * chess.startGame(true, opts, "Alice", "Bob", settings);
     * </pre>
     */
    public void startGame(boolean online,
                          RulesetOptions selectedRuleset,
                          String playerWhiteName,
                          String playerBlackName,
                          Map<String, String> onlineGameSettings) {
        if (game == null) {
            if (online) {
                game = createOnlineGame(selectedRuleset, playerWhiteName, playerBlackName, onlineGameSettings);
            } else {
                game = Game.createGame(false, selectedRuleset, playerWhiteName, playerBlackName, null, null);
            }
            game.startGame();
            LOGGER.log(Level.INFO, "Game started");
        } else {
            LOGGER.log(Level.WARNING, "Game is already running");
        }
    }

    /**
     * Establishes a server connection and constructs a {@link Game} for online play via the
     * {@link Game#createGame(boolean, RulesetOptions, String, String, Map, io.github.conava.chess.core.logic.game.ServerConnection)}
     * factory.
     *
     * <p>Networking setup is performed here in the application layer so that the {@code core}
     * module remains I/O-free. The {@link ServerCommunicationTask} is started on a background
     * thread, and this method blocks until the connection is confirmed (or fails). The task
     * is then passed into the game factory as a
     * {@link io.github.conava.chess.core.logic.game.ServerConnection}.</p>
     *
     * @param selectedRuleset    The ruleset to use.
     * @param playerWhiteName    Name of the white player.
     * @param playerBlackName    Name of the black player.
     * @param onlineGameSettings Map containing at minimum {@code "ip"} and {@code "port"} keys.
     * @return A fully initialised {@link Game} for online play, or one in {@code SERVER_ERROR}
     *         state if the connection could not be established.
     */
    private Game createOnlineGame(RulesetOptions selectedRuleset,
                                  String playerWhiteName,
                                  String playerBlackName,
                                  Map<String, String> onlineGameSettings) {
        String serverIP = onlineGameSettings.get("ip");
        int serverPort = Integer.parseInt(onlineGameSettings.get("port"));

        CountDownLatch connectionLatch = new CountDownLatch(1);

        // Create a temporary holder so that the message handler lambda can reference the game
        // once it is constructed. The array trick allows effective-final capture.
        Game[] gameHolder = new Game[1];
        Consumer<Message> handler = msg -> gameHolder[0].handleMessage(msg);

        ServerCommunicationTask task = new ServerCommunicationTask(serverIP, serverPort,
                connectionLatch, handler);

        Thread serverThread = new Thread(task);
        serverThread.setDaemon(true);
        serverThread.start();

        try {
            connectionLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.SEVERE, "Thread interrupted while waiting for server connection", e);
        }

        Game onlineGame = Game.createGame(true, selectedRuleset, playerWhiteName, playerBlackName,
                onlineGameSettings, task);
        gameHolder[0] = onlineGame;

        if (!task.isConnected()) {
            onlineGame.setGameState(GameState.SERVER_ERROR);
            return onlineGame;
        }

        onlineGame.connectToServerGame();
        return onlineGame;
    }

    /**
     * Retrieves the current {@link GameState}.
     *
     * @return current state of the running game, or {@code null} if no game is active
     *
     * <p>Example:</p>
     * <pre>
     * GameState state = chess.getState();
     * </pre>
     */
    public GameState getState() {
        if (game == null) return null;
        return game.getState();
    }

    /**
     * Retrieves the current {@link Board}.
     *
     * @return board representation of the game, or {@code null} if no game is active
     *
     * <p>Example:</p>
     * <pre>
     * Board board = chess.getBoard();
     * </pre>
     */
    public Board getBoard() {
        if (game == null) return null;
        return game.getBoard();
    }

    /**
     * Attaches an observer to receive game updates.
     *
     * @param observer implementation of {@link GameObserver}
     * @throws IllegalStateException if no game is currently active
     *
     * <p>Example:</p>
     * <pre>
     * chess.addObserver(myObserver);
     * </pre>
     */
    public void addObserver(GameObserver observer) {
        if (game == null) throw new IllegalStateException("No active game");
        game.addObserver(observer);
    }

    /**
     * Detaches a previously added observer.
     *
     * @param observer the observer to remove
     * @throws IllegalStateException if no game is currently active
     *
     * <p>Example:</p>
     * <pre>
     * chess.removeObserver(myObserver);
     * </pre>
     */
    public void removeObserver(GameObserver observer) {
        if (game == null) throw new IllegalStateException("No active game");
        game.removeObserver(observer);
    }

    /**
     * Terminates the current game session and clears state.
     *
     * <p>Calls {@link Game#endGame()} on the active game before nulling the reference.
     * For an online game this closes the server connection and sends a resignation
     * status message. This method is a no-op when no game is currently active ({@code game == null}).</p>
     *
     * <p>Example:</p>
     * <pre>
     * chess.endGame();
     * </pre>
     */
    public void endGame() {
        if (game != null) {
            game.endGame();
            game = null;
        }
    }

    /**
     * Returns the player whose turn it is.
     *
     * @return current {@link Player}, or {@code null} if no game is active
     *
     * <p>Example:</p>
     * <pre>
     * Player current = chess.getCurrentPlayer();
     * </pre>
     */
    public Player getCurrentPlayer() {
        if (game == null) return null;
        return game.getCurrentPlayer();
    }

    /**
     * Returns the white player.
     *
     * @return white {@link Player}, or {@code null} if no game is active
     */
    public Player getPlayerWhite() {
        if (game == null) return null;
        return game.getPlayerWhite();
    }

    /**
     * Returns the black player.
     *
     * @return black {@link Player}, or {@code null} if no game is active
     */
    public Player getPlayerBlack() {
        if (game == null) return null;
        return game.getPlayerBlack();
    }

    /**
     * Retrieves the {@link Piece} at a given board position.
     *
     * @param position target {@link Square}
     * @return piece occupying that square, {@code null} if empty, or {@code null} if no game is active
     *
     * <p>Example:</p>
     * <pre>
     * Piece p = chess.getPieceAt(new Square(1, 4));
     * </pre>
     */
    public Piece getPieceAt(Square position) {
        if (game == null) return null;
        return game.getPieceAt(position);
    }

    /**
     * Computes all legal target squares for a piece at the given position.
     *
     * @param position start {@link Square} of the piece
     * @return list of legal {@link Square} destinations, or an empty list if no game is active
     *
     * <p>Example:</p>
     * <pre>
     * List&lt;Square&gt; moves = chess.getLegalSquares(new Square(6, 4));
     * </pre>
     */
    public List<Square> getLegalSquares(Square position) {
        if (game == null) return Collections.emptyList();
        return game.getLegalSquares(position);
    }

    /**
     * Returns the list of moves made so far in algebraic notation.
     *
     * @return move list as {@link List} of {@link String}, or an empty list if no game is active
     */
    public List<String> getMoveList() {
        if (game == null) return Collections.emptyList();
        return game.getMoveList();
    }

    /**
     * Executes a move from start to end square.
     *
     * @param start source {@link Square}
     * @param end   destination {@link Square}
     * @throws IllegalMoveException  if the move violates game rules
     * @throws IllegalStateException if no game is currently active
     *
     * <p>Example:</p>
     * <pre>
     * chess.movePiece(new Square(6, 4), new Square(4, 4));
     * </pre>
     */
    public void movePiece(Square start, Square end) throws IllegalMoveException {
        if (game == null) throw new IllegalStateException("No active game");
        game.movePiece(start, end);
    }

    /**
     * Executes a pawn promotion move.
     *
     * @param start       source {@link Square}
     * @param end         destination {@link Square}
     * @param targetPiece piece type to promote to (e.g., {@link Pieces#QUEEN})
     * @throws IllegalMoveException  if promotion is invalid
     * @throws IllegalStateException if no game is currently active
     *
     * <p>Example:</p>
     * <pre>
     * chess.promoteMove(new Square(1, 4), new Square(0, 4), Pieces.QUEEN);
     * </pre>
     */
    public void promoteMove(Square start, Square end, Pieces targetPiece) throws IllegalMoveException {
        if (game == null) throw new IllegalStateException("No active game");
        game.promoteMove(start, end, targetPiece);
    }

    /**
     * Retrieves the join code for an online game session.
     *
     * <p>Delegates to {@link Game#getJoinCode()}, which returns {@code null} for offline
     * games by default and the actual join code for online games.</p>
     *
     * @return join code string if this is an online game, or {@code null} if offline or
     *         if no game is currently active
     *
     * <p>Example:</p>
     * <pre>
     * String code = chess.getJoinCode();
     * </pre>
     */
    public String getJoinCode() {
        return game != null ? game.getJoinCode() : null;
    }
}
