package ptp.project;

import ptp.project.window.MainFrame;
import java.util.logging.*;

import javax.swing.*;

public class Chess {
    private static final Logger LOGGER = Logger.getLogger(Chess.class.getName());

    public static void main(String[] args) {
        LOGGER.log(Level.INFO, "Starting Chess game");
        SwingUtilities.invokeLater(MainFrame::new);
    }
}