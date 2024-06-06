package ptp.project.window;

import ptp.project.logic.Game;
import ptp.project.logic.game.OfflineGame;
import ptp.project.logic.game.OnlineGame;
import ptp.project.window.components.*;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChessGame extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(ChessGame.class.getName());
    private final Game game;
    private final MainFrame mainFrame;
    private ColorScheme colorScheme;

    public ChessGame(MainFrame mainFrame, ColorScheme colorScheme, int online) {
        this.mainFrame = mainFrame;
        this.colorScheme = colorScheme;
        this.setBackground(colorScheme.getBackgroundColor());
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
        JPanel topPanel = new TopPanel(colorScheme, mainFrame);
        topPanel.setPreferredSize(new Dimension(this.getWidth(), 60));
        topPanel.setBackground(colorScheme.getAccentColor()); // Change to desired color

        // Create the bottom panel
        JPanel bottomPanel = new BottomPanel(colorScheme, mainFrame);
        bottomPanel.setPreferredSize(new Dimension(this.getWidth(), 60));

        // Create the side panel
        JPanel sidePanel = new SidePanel(colorScheme, mainFrame);
        sidePanel.setPreferredSize(new Dimension(300, sidePanel.getPreferredSize().height));

        // Add the panels to the frame
        this.add(topPanel, BorderLayout.NORTH);
        this.add(bottomPanel, BorderLayout.SOUTH);
        this.add(sidePanel, BorderLayout.EAST);
    }
}