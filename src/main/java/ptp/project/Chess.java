package ptp.project;

import ptp.project.exceptions.IllegalMoveException;
import ptp.project.logic.PlayerTemp;
import ptp.project.logic.SquareTemp;
import ptp.project.logic.gameTemp.GameTemp;
import ptp.project.logic.gameTemp.OfflineGameTemp;
import ptp.project.logic.gameTemp.OnlineGameTemp;
import ptp.project.logic.movesTemp.MoveTemp;
import ptp.project.logic.piecesTemp.PieceTemp;
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
    GameTemp gameTemp;

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
    public GameTemp getGame() {
        return gameTemp;
    }

    /**
     * Creates an online game.
     */
    public void createOnlineGame() {
        gameTemp = new OnlineGameTemp();
    }

    /**
     * Creates an offline game.
     */
    public void createOfflineGame() {
        gameTemp = new OfflineGameTemp();
    }

    /**
     * Starts the game.
     */
    public void startGame() {
        gameTemp.start();
    }

    /**
     * Ends the game.
     */
    public void endGame() {
        gameTemp = null;
    }

    /**
     * Returns the current player.
     * @return The current player
     */
    public PlayerTemp getCurrentPlayer() {
        return gameTemp.getCurrentPlayer();
    }

    /**
     * Returns the white player.
     * @return The white player
     */
    public PlayerTemp getPlayerWhite() {
        return gameTemp.getPlayerWhite();
    }

    /**
     * Returns the black player.
     * @return The black player
     */
    public PlayerTemp getPlayerBlack() {
        return gameTemp.getPlayerBlack();
    }

    /**
     * Returns the piece at a given position.
     * @param position The position of the piece
     * @return The piece at the given position
     */
    public PieceTemp getPieceAt(SquareTemp position) {
        return gameTemp.getPieceAt(position);
    }

    /**
     * Returns the legal squares for a Piece on a given position.
     * @param position The position of the piece
     * @return The legal squares for the given piece
     */
    public List<SquareTemp> getLegalSquares(SquareTemp position) {
        return gameTemp.getLegalSquares(position);
    }

    /**
     * Returns the list of moves made in the game.
     * @return The list of moves made in the game
     */
    public List<MoveTemp> getMoveList() {
        return gameTemp.getMoveList();
    }

    /**
     * Moves a piece in the game.
     *
     * @param start the starting position of the players piece
     * @param end the end position of the players piece
     * @throws IllegalMoveException if the move is not allowed
     */
    public void movePiece(SquareTemp start, SquareTemp end) throws IllegalMoveException {
        gameTemp.movePiece(start, end);
    }
}