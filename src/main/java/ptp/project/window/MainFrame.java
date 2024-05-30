package ptp.project.window;

import ptp.project.window.components.ColorScheme;
import java.util.logging.*;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;

public class MainFrame extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(MainFrame.class.getName());
    private ColorScheme colorScheme;

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
                new Color(0x2b2d30), // Background color
                new Color(0xECF0F1), // Font color
                new Color(0x1e1f22), // Button color
                new Color(0x3c3f41) // Accent color
        );
    }

    public void switchToGame(int online) {
        LOGGER.log(Level.INFO, "Switching to game");
        ChessGame chessGame = new ChessGame(this, colorScheme, online);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setContentPane(chessGame);
        validate();
    }

    public void switchToMenu() {
        LOGGER.log(Level.INFO, "Switching to menu");
        MainMenu mainMenu = new MainMenu(this, colorScheme);
        setExtendedState(JFrame.NORMAL);
        setContentPane(mainMenu);
        validate();
    }
}