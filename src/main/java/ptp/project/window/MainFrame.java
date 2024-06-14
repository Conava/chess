package ptp.project.window;

import ptp.project.window.components.ColorScheme;

import java.awt.geom.RoundRectangle2D;
import java.util.logging.*;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;

public class MainFrame extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(MainFrame.class.getName());
    private ColorScheme colorScheme;
    private ChessGame chessGame;

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

    public void switchToGame(int online) {
        LOGGER.log(Level.INFO, "Switching to game");
        this.setMinimumSize(new Dimension(1000, 800));
        chessGame = new ChessGame(this, colorScheme, online);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setContentPane(chessGame);
        validate();
    }

    public void switchToMenu() {
        LOGGER.log(Level.INFO, "Switching to menu");
        this.setMinimumSize(new Dimension(600, 600));
        setExtendedState(JFrame.NORMAL);
        this.setSize(600, 600);
        MainMenu mainMenu = new MainMenu(this, colorScheme);
        setContentPane(mainMenu);
        validate();
    }

    public void openSettings() {
        LOGGER.log(Level.INFO, "Opening settings");
        //Open a new settings window
        new Settings(this, "Einstellungen", colorScheme);
        validate();
    }

    public void demo() {
        LOGGER.log(Level.INFO, "Executing demo");
        chessGame.demo();
    }
}