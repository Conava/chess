package ptp.project.window;

import ptp.project.logic.Player;
import ptp.project.logic.game.Game;
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

    private TopPanel topPanel;
    private BottomPanel bottomPanel;
    private SidePanel sidePanel;

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
        LOGGER.log(Level.INFO, "GameWindow GUI initialized");
        initializeGame();
        LOGGER.log(Level.INFO, "Game started");

    }

    private void initializeGUI() {
        this.setLayout(new BorderLayout());
        // Create the top panel
        topPanel = new TopPanel(colorScheme, mainFrame);
        topPanel.setPreferredSize(new Dimension(this.getWidth(), 50));

        // Create the bottom panel
        bottomPanel = new BottomPanel(colorScheme, mainFrame);
        bottomPanel.setPreferredSize(new Dimension(this.getWidth(), 50));

        // Create the side panel
        sidePanel = new SidePanel(colorScheme, mainFrame);
        sidePanel.setPreferredSize(new Dimension(300, sidePanel.getPreferredSize().height));

        // Add the panels to the frame
        this.add(topPanel, BorderLayout.NORTH);
        this.add(bottomPanel, BorderLayout.SOUTH);
        this.add(sidePanel, BorderLayout.EAST);
    }

    private void initializeGame() {
        game.start();
        topPanel.setPlayer1Name(game.getPlayerWhite().getName());
        bottomPanel.setPlayer2Name(game.getPlayerBlack().getName());
        updateActivePlayerHighlight(game.getCurrentPlayer());
    }

    public void update() {
        updateActivePlayerHighlight(game.getCurrentPlayer());
    }

    /**
     * Method for testing only
     */
    public void demo() {
        updateActivePlayerHighlight(game.getPlayerBlack());
    }

    private void updateActivePlayerHighlight(Player player) {
        if (player == game.getPlayerWhite()) {
            topPanel.setPlayer1(true);
            bottomPanel.setPlayer2(false);
        } else {
            topPanel.setPlayer1(false);
            bottomPanel.setPlayer2(true);
        }
    }
}