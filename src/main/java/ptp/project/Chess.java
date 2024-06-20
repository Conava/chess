package ptp.project;

import ptp.project.exceptions.IllegalMoveException;
import ptp.project.logic.Board;
import ptp.project.logic.Player;
import ptp.project.logic.Square;
import ptp.project.logic.game.Game;
import ptp.project.logic.game.OfflineGame;
import ptp.project.logic.game.OnlineGame;
import ptp.project.logic.moves.Move;
import ptp.project.logic.pieces.Piece;
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
    MainFrame mainFrame;
    Game game;

    /**
     * Main method of the application. Starts the application.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Chess::new);
    }

    /**
     * Constructor for the Chess class.
     * Initializes the main frame and sets the color scheme.
     */
    public Chess() {
        LOGGER.log(Level.INFO, "Chess application started");
        ColorScheme colorScheme = new ColorScheme(
                new Font("Arial", Font.PLAIN, 20), // Font
                new Color(0x2b2d30), // Background color
                new Color(0x3B3F42), // Brighter background color
                new Color(0x27272B), // Darker background color
                new Color(0xECF0F1), // Font color
                new Color(0x1e1f22), // Button color
                new Color(0x31709A), // Accent color
                new Color(0xA31717)  // Exit button color
        );
        mainFrame = new MainFrame(this, colorScheme);
    }

    /**
     * Switches the content pane to the game panel.
     *
     * @param online The online status of the game. 0 for offline, 1 for online.
     */
    public void switchToGame(int online) {
        mainFrame.switchToGame(online);
    }

    /**
     * Switches the content pane to the main menu.
     */
    public void switchToMenu() {
        mainFrame.switchToMenu();
    }

    /**
     * Opens the settings window.
     */
    public void openSettingsWindow() {
        mainFrame.openSettingsWindow();
    }

    /**
     * Returns the current game.
     * @return The current game
     */
    public Game getGame() {
        return game;
    }

    /**
     * Creates an online game.
     */
    public void createOnlineGame() {
        game = new OnlineGame();
    }

    /**
     * Creates an offline game.
     */
    public void createOfflineGame() {
        game = new OfflineGame();
    }

    /**
     * Starts the game.
     */
    public void startGame() {
        game.start();
    }

    /**
     * Ends the game.
     */
    public void endGame() {
        game = null;
    }

    /**
     * Returns the current player.
     * @return The current player
     */
    public Player getCurrentPlayer() {
        return game.getCurrentPlayer();
    }

    /**
     * Returns the white player.
     * @return The white player
     */
    public Player getPlayerWhite() {
        return game.getPlayerWhite();
    }

    /**
     * Returns the black player.
     * @return The black player
     */
    public Player getPlayerBlack() {
        return game.getPlayerBlack();
    }

    /**
     * Returns the piece at a given position.
     * @param position The position of the piece
     * @return The piece at the given position
     */
    public Piece getPieceAt(Square position) {
        return game.getPieceAt(position);
    }

    /**
     * Returns the board of the game.
     * @return The board of the game
     */
    public Board getBoard() {
        return game.getBoard();
    }

    /**
     * Returns the legal squares for a Piece on a given position.
     * @param position The position of the piece
     * @return The legal squares for the given piece
     */
    public List<Square> getLegalSquares(Square position) {
        return game.getLegalSquares(position);
    }

    /**
     * Returns the list of moves made in the game.
     * @return The list of moves made in the game
     */
    public List<Move> getMoveList() {
        return game.getMoveList();
    }

    /**
     * Moves a piece in the game.
     *
     * @param start the starting position of the players piece
     * @param end the end position of the players piece
     * @throws IllegalMoveException if the move is not allowed
     */
    public void movePiece(Square start, Square end) throws IllegalMoveException {
        game.movePiece(start, end);
    }
}