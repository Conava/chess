package ptp.project.window;

import ptp.project.window.components.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.logging.Logger;
import java.util.logging.*;

/**
 * MainMenu class represents the main menu of the application.
 * It extends JPanel and contains the logo and buttons for starting the game,
 * accessing settings, and exiting the game.
 */
public class MainMenu extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(MainMenu.class.getName());
    private static final String CHESS_ICON_PATH = "/icon/chess-green.png";
    private static final String LOCAL_PLAY_BUTTON_TEXT = "Lokal Spielen";
    private static final String ONLINE_PLAY_BUTTON_TEXT = "Online Spielen";
    private static final String SETTINGS_BUTTON_TEXT = "Einstellungen";
    private static final String EXIT_BUTTON_TEXT = "Spiel Verlassen";
    private static final String ONLINE_PLAY_ERROR_MESSAGE = "Online spielen ist noch nicht verfügbar";
    private static final String ONLINE_PLAY_ERROR_TITLE = "Fehler";

    private final MainFrame mainFrame;
    private JPanel logoPanel;
    private JPanel buttonPanel;
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
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        logoPanel = createLogoPanel();
        buttonPanel = createButtonPanel();

        gbc.fill = GridBagConstraints.BOTH;

        addLogoPanelToLayout(gbc);
        addButtonPanelToLayout(gbc);
    }

    /**
     * Creates the logo panel and adds it to the layout.
     *
     * @return JPanel The created logo panel.
     */
    private JPanel createLogoPanel() {
        JPanel logoPanel = new ControlPanel();
        addContentToLogoPanel(logoPanel);
        return logoPanel;
    }

    /**
     * Adds the logo to the logo panel.
     * If the logo image is not found, a text error message is displayed instead.
     *
     * @param logoPanel The logo panel to which the logo is added.
     */
    private void addContentToLogoPanel(JPanel logoPanel) {
        try {
            URL url = getClass().getResource(CHESS_ICON_PATH);
            ImageIcon imageIcon = new ImageIcon(url);
            JLabel label = new JLabel(imageIcon);
            logoPanel.add(label);
        } catch (NullPointerException e) {
            JLabel label = new JLabel("Willkommen zum Schachspiel");
            logoPanel.add(label);
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

        addButtonToPanel(buttonPanel, LOCAL_PLAY_BUTTON_TEXT, maxButtonSize, e -> mainFrame.switchToGame(0));
        addButtonToPanel(buttonPanel, ONLINE_PLAY_BUTTON_TEXT, maxButtonSize, e -> new MessageWindow(mainFrame, ONLINE_PLAY_ERROR_MESSAGE, ONLINE_PLAY_ERROR_TITLE, colorScheme).setVisible(true));
        addButtonToPanel(buttonPanel, SETTINGS_BUTTON_TEXT, maxButtonSize, e -> mainFrame.openSettings());
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
            button = new ExitButton(text, colorScheme); // Create an ExitButton for the exit button
        } else {
            button = new CustomButton(text, colorScheme); // Create a CustomButton for other buttons
        }
        button.setMaximumSize(maxSize);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.addActionListener(actionListener);
        panel.add(button);
        panel.add(Box.createVerticalStrut(10));
    }

    /**
     * Adds the logo panel to the layout.
     *
     * @param gbc The GridBagConstraints object used for the layout.
     */
    private void addLogoPanelToLayout(GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.weighty = 0.5;
        add(logoPanel, gbc);
    }

    /**
     * Adds the button panel to the layout.
     *
     * @param gbc The GridBagConstraints object used for the layout.
     */
    private void addButtonPanelToLayout(GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        gbc.weighty = 0.5;
        add(buttonPanel, gbc);
    }
}