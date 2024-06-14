package ptp.project.window;

import ptp.project.window.components.ColorScheme;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;
import java.util.logging.*;

/**
 * MainFrame is the main application window for the chess game.
 * It extends JFrame and handles the switching between different panels (like game and menu),
 * and the initialization of the color scheme.
 * It is the counterpart for all API Methods
 */
public class MainFrame extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(MainFrame.class.getName());
    private ColorScheme colorScheme;
    private ChessGame chessGame;

    /**
     * Constructor for MainFrame.
     * Initializes the frame with default settings, initializes the color scheme, and switches to the MainMenu.
     */
    public MainFrame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 600);
        setTitle("Chess");
        initializeColorScheme();
        switchToMenu();
        setLocationRelativeTo(null);
        setVisible(true);
        LOGGER.log(Level.INFO, "MainFrame initialized");
    }

    /**
     * Initializes the color scheme with colors and the default font. Gets applied to all components.
     */
    private void initializeColorScheme() {
        colorScheme = new ColorScheme(
                new Font("Arial", Font.PLAIN, 20), // Font
                new Color(0x2b2d30), // Background color
                new Color(0x3B3F42), // Brighter background color
                new Color(0x27272B), // Darker background color
                new Color(0xECF0F1), // Font color
                new Color(0x1e1f22), // Button color
                new Color(0x31709A), // Accent color
                new Color(0xA31717)  // Exit button color
        );
        LOGGER.log(Level.INFO, "Color scheme initialized");
    }

    /**
     * Switches the content pane to the game panel.
     * @param online The online status of the game. 0 for offline, 1 for online.
     */
    public void switchToGame(int online) {
        LOGGER.log(Level.INFO, "Switching to game with online status " + online);
        this.setMinimumSize(new Dimension(1000, 800));
        chessGame = new ChessGame(this, colorScheme, online);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setContentPane(chessGame);
        validate();
        LOGGER.log(Level.INFO, "Loaded game window successfully");
    }

    /**
     * Switches the content pane to the main menu panel.
     */
    public void switchToMenu() {
        LOGGER.log(Level.INFO, "Switching to menu");
        this.setMinimumSize(new Dimension(600, 600));
        setExtendedState(JFrame.NORMAL);
        this.setSize(600, 600);
        MainMenu mainMenu = new MainMenu(this, colorScheme);
        chessGame = null;
        setContentPane(mainMenu);
        validate();
        LOGGER.log(Level.INFO, "Loaded main menu successfully");
    }

    /**
     * Opens the settings window.
     */
    public void openSettings() {
        LOGGER.log(Level.INFO, "Opening settings");
        //Open a new settings window
        new Settings(this, "Einstellungen", colorScheme);
        validate();
        LOGGER.log(Level.INFO, "Opened settings successfully");
    }

    public void demo() {
        LOGGER.log(Level.INFO, "Executing demo");
        chessGame.demo();
    }
}