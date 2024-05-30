package ptp.project.window;

import ptp.project.window.components.ColorScheme;
import ptp.project.window.components.ContentPane;

import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChessGame extends ContentPane {
    private static final Logger LOGGER = Logger.getLogger(ChessGame.class.getName());

    public ChessGame(MainFrame mainFrame, ColorScheme colorScheme, int online) {
        super(mainFrame);
        this.applyColorScheme(colorScheme);
        if (online == 1) {
            LOGGER.log(Level.INFO, "Online game initiated");
        } else {
            LOGGER.log(Level.INFO, "Local game initiated");
        }
    }
}