package ptp.project.window;

import ptp.project.window.components.ColorScheme;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    private final MainMenu mainMenu;
    private final ChessGame chessGame;

    public MainFrame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 600);
        setTitle("Chess");

        mainMenu = new MainMenu(this);
        chessGame = new ChessGame(this);

        setColorScheme(new ColorScheme(
                new Color(0x2b2d30), // Background color
                new Color(0xECF0F1), // Font color
                new Color(0x1e1f22), // Button color
                new Color(0x27AE60) // Accent color
        ));

        setContentPane(mainMenu);

        setLocationRelativeTo(null);

        setVisible(true);
    }

    private void setColorScheme(ColorScheme colorScheme) {
        mainMenu.applyColorScheme(colorScheme);
        chessGame.applyColorScheme(colorScheme);
    }

    public void switchToGame() {
        setContentPane(chessGame);
        validate();
    }

    public void switchToMenu() {
        setContentPane(mainMenu);
        validate();
    }
}