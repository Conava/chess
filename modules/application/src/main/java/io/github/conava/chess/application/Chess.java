package io.github.conava.chess.application;

import io.github.conava.chess.core.data.board.Board;
import io.github.conava.chess.core.logic.game.GameState;
import io.github.conava.chess.core.data.pieces.Pieces;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import io.github.conava.chess.core.exceptions.IllegalMoveException;
import io.github.conava.chess.core.data.player.Player;
import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.logic.game.Game;
import io.github.conava.chess.core.logic.observer.GameObserver;
import io.github.conava.chess.core.logic.game.OfflineGame;
import io.github.conava.chess.core.logic.game.OnlineGame;
import io.github.conava.chess.core.data.pieces.Piece;
import io.github.conava.chess.application.network.ServerCommunicationTask;
import io.github.conava.chess.application.window.MainFrame;
import io.github.conava.chess.application.components.ColorScheme;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.logging.*;
import javax.swing.*;

/**
 * Entry point and façade for the Chess application.
 * Manages game lifecycle, GUI initialization, and delegates core logic to {@code OfflineGame} or {@code OnlineGame}.
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
     * @param online             0 for offline play, 1 for online play
     * @param selectedRuleset    configuration options for the game rules
     * @param playerWhiteName    display name of the white player
     * @param playerBlackName    display name of the black player
     * @param onlineGameSettings key-value settings for online matchmaking or server connection
     *
     * <p>Example for offline:</p>
     * <pre>
     * RulesetOptions opts = RulesetOptions.standard();
     * chess.startGame(0, opts, "Alice", "Bob", Collections.emptyMap());
     * </pre>
     *
     * <p>Example for online:</p>
     * <pre>
     * Map<String,String> settings = Map.of("host","game.example.com","port","1234");
     * chess.startGame(1, opts, "Alice", "Bob", settings);
     * </pre>
     */
    public void startGame(int online,
                          RulesetOptions selectedRuleset,
                          String playerWhiteName,
                          String playerBlackName,
                          Map<String, String> onlineGameSettings) {
        if (game == null) {
            if (online == 1) {
                game = createOnlineGame(selectedRuleset, playerWhiteName, playerBlackName, onlineGameSettings);
            } else {
                game = new OfflineGame(selectedRuleset, playerWhiteName, playerBlackName);
            }
            game.startGame();
            LOGGER.log(Level.INFO, "Game started");
        } else {
            LOGGER.log(Level.WARNING, "Game is already running");
        }
    }

    /**
     * Establishes a server connection and constructs an {@link OnlineGame}.
     *
     * <p>Networking setup is performed here in the application layer so that the {@code core}
     * module remains I/O-free. The {@link ServerCommunicationTask} is started on a background
     * thread, and this method blocks until the connection is confirmed (or fails). The task
     * is then passed into {@link OnlineGame} as a {@link io.github.conava.chess.core.logic.game.ServerConnection}.</p>
     *
     * @param selectedRuleset    The ruleset to use.
     * @param playerWhiteName    Name of the white player.
     * @param playerBlackName    Name of the black player.
     * @param onlineGameSettings Map containing at minimum {@code "ip"} and {@code "port"} keys.
     * @return A fully initialised {@link OnlineGame}, or one in {@code SERVER_ERROR} state if
     *         the connection could not be established.
     */
    private OnlineGame createOnlineGame(RulesetOptions selectedRuleset,
                                        String playerWhiteName,
                                        String playerBlackName,
                                        Map<String, String> onlineGameSettings) {
        String serverIP = onlineGameSettings.get("ip");
        int serverPort = Integer.parseInt(onlineGameSettings.get("port"));

        CountDownLatch connectionLatch = new CountDownLatch(1);
        ServerCommunicationTask task = new ServerCommunicationTask(serverIP, serverPort, connectionLatch);

        Thread serverThread = new Thread(task);
        serverThread.setDaemon(true);
        serverThread.start();

        try {
            connectionLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.SEVERE, "Thread interrupted while waiting for server connection", e);
        }

        OnlineGame onlineGame = new OnlineGame(selectedRuleset, playerWhiteName, playerBlackName,
                onlineGameSettings, task);
        task.setMessageHandler(onlineGame::handleMessage);
        return onlineGame;
    }

    /**
     * Retrieves the current {@link GameState}.
     *
     * @return current state of the running game
     *
     * <p>Example:</p>
     * <pre>
     * GameState state = chess.getState();
     * </pre>
     */
    public GameState getState() {
        return game.getState();
    }

    /**
     * Retrieves the current {@link Board}.
     *
     * @return board representation of the game
     *
     * <p>Example:</p>
     * <pre>
     * Board board = chess.getBoard();
     * </pre>
     */
    public Board getBoard() {
        return game.getBoard();
    }

    /**
     * Attaches an observer to receive game updates.
     *
     * @param observer implementation of {@link GameObserver}
     *
     * <p>Example:</p>
     * <pre>
     * chess.addObserver(myObserver);
     * </pre>
     */
    public void addObserver(GameObserver observer) {
        game.addObserver(observer);
    }

    /**
     * Detaches a previously added observer.
     *
     * @param observer the observer to remove
     *
     * <p>Example:</p>
     * <pre>
     * chess.removeObserver(myObserver);
     * </pre>
     */
    public void removeObserver(GameObserver observer) {
        game.removeObserver(observer);
    }

    /**
     * Terminates the current game session and clears state.
     *
     * <p>Example:</p>
     * <pre>
     * chess.endGame();
     * </pre>
     */
    public void endGame() {
        game = null;
    }

    /**
     * Returns the player whose turn it is.
     *
     * @return current {@link Player}
     *
     * <p>Example:</p>
     * <pre>
     * Player current = chess.getCurrentPlayer();
     * </pre>
     */
    public Player getCurrentPlayer() {
        return game.getCurrentPlayer();
    }

    /**
     * Returns the white player.
     *
     * @return white {@link Player}
     */
    public Player getPlayerWhite() {
        return game.getPlayerWhite();
    }

    /**
     * Returns the black player.
     *
     * @return black {@link Player}
     */
    public Player getPlayerBlack() {
        return game.getPlayerBlack();
    }

    /**
     * Retrieves the {@link Piece} at a given board position.
     *
     * @param position target {@link Square}
     * @return piece occupying that square, or {@code null} if empty
     *
     * <p>Example:</p>
     * <pre>
     * Piece p = chess.getPieceAt(new Square("e4"));
     * </pre>
     */
    public Piece getPieceAt(Square position) {
        return game.getPieceAt(position);
    }

    /**
     * Computes all legal target squares for a piece at the given position.
     *
     * @param position start {@link Square} of the piece
     * @return list of legal {@link Square} destinations
     *
     * <p>Example:</p>
     * <pre>
     * List<Square> moves = chess.getLegalSquares(new Square("d2"));
     * </pre>
     */
    public List<Square> getLegalSquares(Square position) {
        return game.getLegalSquares(position);
    }

    /**
     * Returns the list of moves made so far in algebraic notation.
     *
     * @return move list as {@link List} of {@link String}
     */
    public List<String> getMoveList() {
        return game.getMoveList();
    }

    /**
     * Executes a move from start to end square.
     *
     * @param start source {@link Square}
     * @param end   destination {@link Square}
     * @throws IllegalMoveException if the move violates game rules
     *
     * <p>Example:</p>
     * <pre>
     * chess.movePiece(new Square("e2"), new Square("e4"));
     * </pre>
     */
    public void movePiece(Square start, Square end) throws IllegalMoveException {
        game.movePiece(start, end);
    }

    /**
     * Executes a pawn promotion move.
     *
     * @param start       source {@link Square}
     * @param end         destination {@link Square}
     * @param targetPiece piece type to promote to (e.g., {@link Pieces#QUEEN})
     * @throws IllegalMoveException if promotion is invalid
     *
     * <p>Example:</p>
     * <pre>
     * chess.promoteMove(new Square("e7"), new Square("e8"), Pieces.QUEEN);
     *</pre>
     */
    public void promoteMove(Square start, Square end, Pieces targetPiece) throws IllegalMoveException {
        game.promoteMove(start, end, targetPiece);
    }

    /**
     * Retrieves the join code for an online game session.
     *
     * @return join code string, or {@code null} if offline
     *
     * <p>Example:</p>
     * <pre>
     * String code = chess.getJoinCode();
     * </pre>
     */
    public String getJoinCode() {
        return ((OnlineGame) game).getJoinCode();
    }
}