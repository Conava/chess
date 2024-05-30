package ptp.project.window;

import ptp.project.window.components.ColorScheme;
import ptp.project.window.components.ContentPane;
import ptp.project.window.components.DefaultButton;
import java.util.logging.*;


import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.logging.Logger;

public class MainMenu extends ContentPane {
    private static final Logger LOGGER = Logger.getLogger(MainMenu.class.getName());
    private final JPanel logoPanel;
    private final JPanel buttonPanel;

    public MainMenu(MainFrame mainFrame, ColorScheme colorScheme) {
        super(mainFrame);

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        logoPanel = new JPanel();
        addLogoPanelContent();
        buttonPanel = new JPanel();
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
        this.applyColorScheme(colorScheme);
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

        JButton startButton = new DefaultButton("Singleplayer");
        startButton.setMaximumSize(maxButtonSize);
        startButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        startButton.addActionListener(e -> mainFrame.switchToGame(0));
        buttonPanel.add(startButton);

        // Add space
        buttonPanel.add(Box.createVerticalStrut(10));

        JButton multiplayerButton = new DefaultButton("Multiplayer");
        multiplayerButton.setMaximumSize(maxButtonSize);
        multiplayerButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        multiplayerButton.addActionListener(e -> JOptionPane.showMessageDialog(mainFrame, "Multiplayer is not implemented yet."));
        buttonPanel.add(multiplayerButton);

        // Add space
        buttonPanel.add(Box.createVerticalStrut(10));

        JButton settingsButton = new DefaultButton("Settings");
        settingsButton.setMaximumSize(maxButtonSize);
        settingsButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        settingsButton.addActionListener(e -> JOptionPane.showMessageDialog(mainFrame, "Settings is not implemented yet."));
        buttonPanel.add(settingsButton);

        // Add space
        buttonPanel.add(Box.createVerticalStrut(10));

        JButton exitButton = new DefaultButton("Exit");
        exitButton.setMaximumSize(maxButtonSize);
        exitButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        exitButton.addActionListener(e -> System.exit(0));
        buttonPanel.add(exitButton);

        // Add space
        buttonPanel.add(Box.createVerticalStrut(10));
    }
}