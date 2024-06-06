package ptp.project.window.components;

import ptp.project.window.MainFrame;

import javax.swing.*;
import java.awt.*;

public class TopPanel extends JPanel {
    private final ColorScheme colorScheme;
    private final MainFrame mainFrame;

    private JPanel leftContainer;
    private JPanel rightContainer;

    public TopPanel(ColorScheme colorScheme, MainFrame mainFrame) {
        this.colorScheme = colorScheme;
        this.mainFrame = mainFrame;
        this.setBackground(colorScheme.getAccentColor());
        initializeLayout();
        addComponents();

        setOpaque(true);
    }

    private void initializeLayout() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // Create the left container
        leftContainer = new ControlPanel();
        leftContainer.setLayout(new GridLayout(1, 3));

        // Add the left container to the panel
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        add(leftContainer, gbc);

        // Create the right container
        rightContainer = new ControlPanel();
        rightContainer.setPreferredSize(new Dimension(300, rightContainer.getPreferredSize().height));

        // Add the right container to the panel
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.VERTICAL;
        add(rightContainer, gbc);
    }

    private void addComponents() {
        //Create 3 JPanels in the leftContainer to hold our content
        JPanel leftPanel1 = new ControlPanel();
        JPanel leftPanel2 = new ControlPanel();
        JPanel leftPanel3 = new ControlPanel();

        //Add the 3 JPanels to the leftContainer
        leftContainer.add(leftPanel1);
        leftContainer.add(leftPanel2);
        leftContainer.add(leftPanel3);

        //Add the content to the leftPanel1
        JLabel titleLabel = new DefaultLabel("Schach", colorScheme);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(colorScheme.getFontColor());
        leftPanel1.add(titleLabel);

        //Add the content to the leftPanel2
        JLabel playerLabel = new DefaultLabel("Player 1", colorScheme);
        playerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        playerLabel.setForeground(colorScheme.getFontColor());
        leftPanel2.add(playerLabel);

        //Add the content to the leftPanel3
        JLabel testLabel = new DefaultLabel("Test Label", colorScheme);
        testLabel.setFont(new Font("Arial", Font.BOLD, 24));
        testLabel.setForeground(colorScheme.getFontColor());
        leftPanel3.add(testLabel);
    }
}