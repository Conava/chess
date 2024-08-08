package ptp.window;

import ptp.components.ColorScheme;
import ptp.components.ControlPanel;
import ptp.components.CustomButton;
import ptp.components.ExitButton;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.logging.*;

/**
 * MainMenu class represents the main menu of the application.
 * It extends JPanel and contains the logo and buttons for starting the game,
 * accessing settings, and exiting the game.
 */
public class MainMenu extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(MainMenu.class.getName());
    private static final String CHESS_ICON_PATH = "/titleImage/chessTitleImage.jpg";
    private static final String LOCAL_PLAY_BUTTON_TEXT = "Lokal Spielen";
    private static final String ONLINE_PLAY_BUTTON_TEXT = "Online Spielen";
    private static final String SETTINGS_BUTTON_TEXT = "Einstellungen";
    private static final String EXIT_BUTTON_TEXT = "Spiel Verlassen";

    private final MainFrame mainFrame;
    private final ColorScheme colorScheme;

    /**
     * Constructor for MainMenu.
     * Initializes the main menu with a logo and buttons.
     *
     * @param mainFrame   The main frame of the application.
     * @param colorScheme The color scheme of the application.
     */
    public MainMenu(MainFrame mainFrame, ColorScheme colorScheme) {
        this.mainFrame = mainFrame;
        this.colorScheme = colorScheme;
        this.setBackground(colorScheme.getBackgroundColor());
        initializeGUI();
    }

    /**
     * Initializes the GUI of the main menu.
     * The GUI consists of a logo panel at the top and a button panel at the bottom.
     */
    public void initializeGUI() {
        try {
            URL url = getClass().getResource(CHESS_ICON_PATH);
            ImageIcon imageIcon = new ImageIcon(Objects.requireNonNull(url));

            // Scale the image
            Image image = imageIcon.getImage();
            Image scaledImage = image.getScaledInstance(600, 600, Image.SCALE_SMOOTH);
            ImageIcon scaledIcon = new ImageIcon(scaledImage);

            // Set the image as background
            JLabel background = new JLabel(scaledIcon);
            setLayout(new BorderLayout());
            add(background);
            background.setLayout(new BorderLayout());

            // Create and add the button panel
            JPanel buttonPanel = createButtonPanel();
            buttonPanel.setOpaque(false); // Make the button panel transparent
            background.add(buttonPanel, BorderLayout.SOUTH);

        } catch (NullPointerException e) {
            JLabel label = new JLabel("Willkommen zum Schachspiel");
            add(label);
            LOGGER.log(Level.WARNING, "Error loading icon");
        }
    }

    /**
     * Creates the button panel.
     *
     * @return JPanel The created button panel.
     */
    private JPanel createButtonPanel() {
        JPanel buttonPanel = new ControlPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        addButtonPanelContent(buttonPanel);
        return buttonPanel;
    }

    /**
     * Adds the buttons to the button panel.
     *
     * @param buttonPanel The button panel to which the buttons are added.
     */
    private void addButtonPanelContent(JPanel buttonPanel) {
        Dimension maxButtonSize = new Dimension(560, 100);

        addButtonToPanel(buttonPanel, LOCAL_PLAY_BUTTON_TEXT, maxButtonSize, e -> mainFrame.switchToOfflineGame());
        addButtonToPanel(buttonPanel, ONLINE_PLAY_BUTTON_TEXT, maxButtonSize, e -> mainFrame.switchToOnlineGame());
        addButtonToPanel(buttonPanel, SETTINGS_BUTTON_TEXT, maxButtonSize, e -> mainFrame.openSettingsWindow());
        addButtonToPanel(buttonPanel, EXIT_BUTTON_TEXT, maxButtonSize, e -> System.exit(0));
    }

    /**
     * Adds a button to the panel.
     *
     * @param panel The panel to which the button is added.
     * @param text The text of the button.
     * @param maxSize The maximum size of the button.
     * @param actionListener The action listener of the button.
     */
    private void addButtonToPanel(JPanel panel, String text, Dimension maxSize, ActionListener actionListener) {
        JButton button;
        if (text.equals(EXIT_BUTTON_TEXT)) {
            button = new ExitButton(text, colorScheme);
        } else {
            button = new CustomButton(text, colorScheme);
        }
        button.setMaximumSize(maxSize);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.addActionListener(actionListener);

        panel.add(button);
        panel.add(Box.createVerticalStrut(10));
    }
}