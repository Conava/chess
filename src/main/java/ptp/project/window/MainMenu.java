package ptp.project.window;

import ptp.project.window.components.ContentPane;


import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class MainMenu extends ContentPane {
    private JPanel logoPanel;
    private JPanel buttonPanel;

    public MainMenu(MainFrame mainFrame) {
        super(mainFrame);

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        logoPanel = new JPanel();
        addLogoPanelContent();
        buttonPanel = new JPanel();

        gbc.fill = GridBagConstraints.BOTH;

        // LogoPanel occupies the two top rows
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.gridheight = 2;
        gbc.weightx = 1;
        gbc.weighty = 0.66; // 66% height
        add(logoPanel, gbc);

        // Reset gridwidth and gridheight
        gbc.gridwidth = 1;
        gbc.gridheight = 1;

        // ButtonPanel occupies the lowest column
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        gbc.weighty = 0.33; // 33% height
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
}