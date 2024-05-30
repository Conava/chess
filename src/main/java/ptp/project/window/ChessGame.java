package ptp.project.window;

import ptp.project.logic.Game;
import ptp.project.logic.game.OfflineGame;
import ptp.project.logic.game.OnlineGame;
import ptp.project.window.components.ColorScheme;
import ptp.project.window.components.ContentPane;
import ptp.project.window.components.DefaultButton;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChessGame extends ContentPane {
    private static final Logger LOGGER = Logger.getLogger(ChessGame.class.getName());
    private final Game game;
    private ColorScheme colorScheme;

    public ChessGame(MainFrame mainFrame, ColorScheme colorScheme, int online) {
        super(mainFrame);
        this.colorScheme = colorScheme;
        this.applyColorScheme(colorScheme);
        if (online == 1) {
            LOGGER.log(Level.INFO, "Online game initiated");
            game = new OnlineGame();
        } else {
            LOGGER.log(Level.INFO, "Local game initiated");
            game = new OfflineGame();
        }
        initializeGUI();
        game.start();
    }

    private void initializeGUI() {
        this.setLayout(new BorderLayout());
        // Create the top panel
        JPanel topPanel = new JPanel();
        topPanel.setPreferredSize(new Dimension(this.getWidth(), 100));
        topPanel.setBackground(colorScheme.getAccentColor()); // Change to desired color

        // Create the "Back to Main Menu" button
        JButton backButton = new DefaultButton("Back to Main Menu", colorScheme);
        backButton.addActionListener(e -> mainFrame.switchToMenu());
        topPanel.add(backButton);

        // Create the bottom panel
        JPanel bottomPanel = new JPanel();
        bottomPanel.setPreferredSize(new Dimension(this.getWidth(), 100));
        bottomPanel.setBackground(colorScheme.getAccentColor()); // Change to desired color

        // Create the side panel
        JPanel sidePanel = new JPanel();
        sidePanel.setPreferredSize(new Dimension(300, this.getHeight()));
        sidePanel.setBackground(colorScheme.getAccentColor()); // Change to desired color

        // Add the panels to the frame
        this.add(topPanel, BorderLayout.NORTH);
        this.add(bottomPanel, BorderLayout.SOUTH);
        this.add(sidePanel, BorderLayout.EAST);
    }
}