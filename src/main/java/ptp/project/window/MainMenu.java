package ptp.project.window;

import ptp.project.window.components.*;


import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.logging.Logger;

public class MainMenu extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(MainMenu.class.getName());
    private final MainFrame mainFrame;
    private final JPanel logoPanel;
    private final JPanel buttonPanel;
    private final ColorScheme colorScheme;

    public MainMenu(MainFrame mainFrame, ColorScheme colorScheme) {
        this.mainFrame = mainFrame;
        this.colorScheme = colorScheme;
        this.setBackground(colorScheme.getBackgroundColor());

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        logoPanel = new ControlPanel();
        addLogoPanelContent();
        buttonPanel = new ControlPanel();
        addButtonPanelContent();

        gbc.fill = GridBagConstraints.BOTH;

        // LogoPanel occupies the top half
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.weighty = 0.5;
        add(logoPanel, gbc);

        // Reset gridwidth and gridheight
        gbc.gridwidth = 1;
        gbc.gridheight = 1;

        // ButtonPanel occupies the bottom half
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        gbc.weighty = 0.5;
        add(buttonPanel, gbc);
    }

    private void addLogoPanelContent() {
        try {
            URL url = getClass().getResource("/icon/chess-green.png");
            ImageIcon imageIcon = new ImageIcon(url);
            JLabel label = new JLabel(imageIcon);
            logoPanel.add(label);
        } catch (NullPointerException e) {
            // If the image is not found, display a text instead
            JLabel label = new JLabel("Error loading icon");
            logoPanel.add(label);
        }
    }

    private void addButtonPanelContent() {
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        Dimension maxButtonSize = new Dimension(560, 100);

        JButton startButton = new CustomButton("Lokal Spielen", colorScheme);
        startButton.setMaximumSize(maxButtonSize);
        startButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        startButton.addActionListener(e -> mainFrame.switchToGame(0));
        buttonPanel.add(startButton);

        // Add space
        buttonPanel.add(Box.createVerticalStrut(10));

        JButton multiplayerButton = new CustomButton("Online Spielen", colorScheme);
        multiplayerButton.setMaximumSize(maxButtonSize);
        multiplayerButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        multiplayerButton.addActionListener(e -> new MessageWindow(mainFrame, "Online spielen ist noch nicht verfügbar", "Fehler", colorScheme).setVisible(true));
        buttonPanel.add(multiplayerButton);

        // Add space
        buttonPanel.add(Box.createVerticalStrut(10));

        JButton settingsButton = new CustomButton("Einstellungen", colorScheme);
        settingsButton.setMaximumSize(maxButtonSize);
        settingsButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        settingsButton.addActionListener(e -> mainFrame.openSettings());
        buttonPanel.add(settingsButton);

        // Add space
        buttonPanel.add(Box.createVerticalStrut(10));

        JButton exitButton = new ExitButton("Spiel Verlassen", colorScheme);
        exitButton.setMaximumSize(maxButtonSize);
        exitButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        exitButton.addActionListener(e -> System.exit(0));
        buttonPanel.add(exitButton);

        // Add space
        buttonPanel.add(Box.createVerticalStrut(10));
    }
}