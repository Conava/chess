package ptp.project;

import ptp.project.data.board.Board;
import ptp.project.data.enums.GameState;
import ptp.project.data.enums.Pieces;
import ptp.project.data.enums.RulesetOptions;
import ptp.project.exceptions.IllegalMoveException;
import ptp.project.data.Player;
import ptp.project.data.Square;
import ptp.project.logic.game.Game;
import ptp.project.logic.game.GameObserver;
import ptp.project.logic.game.OfflineGame;
import ptp.project.logic.game.OnlineGame;
import ptp.project.data.pieces.Piece;
import ptp.project.window.MainFrame;
import ptp.project.window.components.ColorScheme;

import java.awt.*;
import java.util.List;
import java.util.logging.*;

import javax.swing.*;

/**
 * Main class of the chess application. This class is responsible for starting the application and managing the game.
 */
public class Chess {
    private static final Logger LOGGER = Logger.getLogger(Chess.class.getName());
    private final MainFrame mainFrame;
    private Game game;

    /**
     * Main method of the application. Starts the application.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("nogui")) {
            // Start the game without GUI
            SwingUtilities.invokeLater(() -> new Chess(false));
        } else {
            // Start the game with GUI
            SwingUtilities.invokeLater(() -> new Chess(true));
        }
    }

    /**
     * Constructor of the Chess class. Starts the application with or without GUI.
     *
     * @param gui True if the application should start with GUI, false otherwise
     */
    public Chess(boolean gui) {
        if (gui) {
            LOGGER.log(Level.INFO, "Chess application started");
            ColorScheme colorScheme = new ColorScheme(
                    new Font("Arial", Font.PLAIN, 20), // Font
                    new Color(0x2b2d30), // Background color
                    new Color(0x3B3F42), // Brighter background color
                    new Color(0x27272B), // Darker background color
                    new Color(0xECF0F1), // Font color
                    new Color(0x1e1f22), // Button color
                    new Color(0x31709A), // Accent color
                    new Color(0xA31717),  // Exit button color
                    new Color(0x808080),  // Border color
                    new Color(0x762D9A) // Board dot color
            );
            mainFrame = new MainFrame(this, colorScheme);
        } else {
            LOGGER.log(Level.INFO, "Chess application started without GUI");
            mainFrame = null;
        }
    }

    /**
     * Starts the game.
     *
     * @param online The online status of the game. 0 for offline, 1 for online.
     */
    public void startGame(int online, RulesetOptions selectedRuleset, String playerWhiteName, String playerBlackName) {
        if (game == null) {
            game = online == 1 ? new OnlineGame(selectedRuleset, playerWhiteName, playerBlackName) : new OfflineGame(selectedRuleset, playerWhiteName, playerBlackName);
            game.startGame();
            LOGGER.log(Level.INFO, "Game started");
        } else {
            LOGGER.log(Level.WARNING, "Game is already running");
        }
    }

    /**
     * Returns the state of the game.
     *
     * @return The state of the game
     */
    public GameState getState() {
        return game.getState();
    }

    public Board getBoard() {
        return game.getBoard();
    }

    /**
     * Adds an observer to the game.
     *
     * @param observer The observer to add
     */
    public void addObserver(GameObserver observer) {
        game.addObserver(observer);
    }

    /**
     * Removes an observer from the game.
     *
     * @param observer The observer to remove
     */
    public void removeObserver(GameObserver observer) {
        game.removeObserver(observer);
    }

    /**
     * Ends the game.
     */
    public void endGame() {
        game.endGame();
        game = null;
    }

    /**
     * Returns the current player.
     *
     * @return The current player
     */
    public Player getCurrentPlayer() {
        return game.getCurrentPlayer();
    }

    /**
     * Returns the white player.
     *
     * @return The white player
     */
    public Player getPlayerWhite() {
        return game.getPlayerWhite();
    }

    /**
     * Returns the black player.
     *
     * @return The black player
     */
    public Player getPlayerBlack() {
        return game.getPlayerBlack();
    }

    /**
     * Returns the piece at a given position.
     *
     * @param position The position of the piece
     * @return The piece at the given position
     */
    public Piece getPieceAt(Square position) {
        return game.getPieceAt(position);
    }

    /**
     * Returns the legal squares for a Piece on a given position.
     *
     * @param position The position of the piece
     * @return The legal squares for the given piece
     */
    public List<Square> getLegalSquares(Square position) {
        return game.getLegalSquares(position);
    }

    /**
     * Returns the list of moves made in the game.
     *
     * @return The list of moves made in the game
     */
    public List<String> getMoveList() {
        return game.getMoveList();
    }

    /**
     * Moves a piece in the game.
     *
     * @param start the starting position of the players piece
     * @param end   the end position of the players piece
     * @throws IllegalMoveException if the move is not allowed
     */
    public void movePiece(Square start, Square end) throws IllegalMoveException {
        game.movePiece(start, end);
    }

    public void promoteMove(Square start, Square end, Pieces targetPiece) throws IllegalMoveException {
        game.promoteMove(start, end, targetPiece);
    }
}