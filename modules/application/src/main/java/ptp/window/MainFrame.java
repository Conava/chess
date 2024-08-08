package ptp.window;

import ptp.Chess;
import ptp.core.logic.ruleset.RulesetOptions;
import ptp.components.ColorScheme;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
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
    private static final int MINIMUM_WINDOW_WIDTH = 600;
    private static final int MINIMUM_WINDOW_HEIGHT = 600;
    private static final int GAME_WINDOW_WIDTH = 1000;
    private static final int GAME_WINDOW_HEIGHT = 800;

    private final ColorScheme colorScheme;
    private ptp.window.ChessGame chessGame;
    private final Chess chess;

    /**
     * Constructor for MainFrame.
     * Initializes the frame with default settings, initializes the color scheme, and switches to the MainMenu.
     */
    public MainFrame(Chess chess, ColorScheme colorScheme) {
        this.colorScheme = colorScheme;
        this.chess = chess;
        initializeWindow();
        switchToMenu();
    }

    /**
     * Initializes the main frame with default settings.
     */
    private void initializeWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(MINIMUM_WINDOW_WIDTH, MINIMUM_WINDOW_HEIGHT);
        setTitle("ptp-do09 Schachspiel");
        setLocationRelativeTo(null);
        setVisible(true);
        LOGGER.log(Level.INFO, "MainFrame initialized");
    }

    /**
     * Switches the content pane to the game panel in offline mode.
     */
    public void switchToOfflineGame() {
        InputDialog dialog = new InputDialog(this, "Spielerinformationen", colorScheme, "Name Spieler Weiß", "Name Spieler Schwarz");
        dialog.setVisible(true);
        if(dialog.isConfirmed())
        {
            String playerWhiteName = dialog.getInput1();
            String playerBlackName = dialog.getInput2();
            RulesetOptions selectedRuleset = dialog.getRulesetSelection();
            LOGGER.log(Level.INFO, "Switching to offline game");
            this.setMinimumSize(new Dimension(GAME_WINDOW_WIDTH, GAME_WINDOW_HEIGHT));
            chessGame = new ChessGame(this, chess, colorScheme, 0, selectedRuleset, playerWhiteName, playerBlackName, null);
            setExtendedState(JFrame.MAXIMIZED_BOTH);
            setContentPane(chessGame);
            validate();
            LOGGER.log(Level.INFO, "Loaded game window successfully");
        }
        LOGGER.log(Level.WARNING, "Switching to game aborted");
    }

    /**
     * Switches the content pane to the game panel in online mode.
     */
    public void switchToOnlineGame() {
        OnlineGameInputDialog dialog = new OnlineGameInputDialog(this, "Spielerinformationen", colorScheme);
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            String ip = dialog.getIp();
            String port = dialog.getPort();
            String joinCode = dialog.getJoinCode();
            RulesetOptions selectedRuleset = dialog.getRulesetSelection();
            String playerWhiteName = joinCode.isEmpty() ? "Online Gegner" : "Du";
            String playerBlackName = joinCode.isEmpty() ? "Du" : "Online Gegner";

            Map<String, String> onlineGameOptions = Map.of(
                    "ip", ip,
                    "port", port,
                    "joinCode", joinCode
            );

            LOGGER.log(Level.INFO, "Switching to online game");
            this.setMinimumSize(new Dimension(GAME_WINDOW_WIDTH, GAME_WINDOW_HEIGHT));
            chessGame = new ChessGame(this, chess, colorScheme, 2, selectedRuleset, playerWhiteName, playerBlackName, onlineGameOptions);
            setExtendedState(JFrame.MAXIMIZED_BOTH);
            setContentPane(chessGame);
            validate();
            LOGGER.log(Level.INFO, "Loaded game window successfully");
        }
        LOGGER.log(Level.WARNING, "Switching to game aborted");
    }

    /**
     * Switches the content pane to the main menu panel.
     */
    public void switchToMenu() {
        LOGGER.log(Level.INFO, "Switching to menu");
        this.setMinimumSize(new Dimension(MINIMUM_WINDOW_WIDTH, MINIMUM_WINDOW_HEIGHT));
        setExtendedState(JFrame.NORMAL);
        this.setSize(MINIMUM_WINDOW_WIDTH, MINIMUM_WINDOW_HEIGHT);
        MainMenu mainMenu = new MainMenu(this, colorScheme);
        setContentPane(mainMenu);
        validate();
        LOGGER.log(Level.INFO, "Loaded main menu successfully");
    }

    /**
     * Opens the settings window.
     */
    public void openSettingsWindow() {
        LOGGER.log(Level.INFO, "Opening settings");
        new Settings(this, "Einstellungen", colorScheme);
        validate();
        LOGGER.log(Level.INFO, "Opened settings successfully");
    }
}