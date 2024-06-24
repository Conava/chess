package ptp.project.window.components;

import ptp.project.window.MainFrame;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BottomPanel extends ControlPanel {
    private static final Logger LOGGER = Logger.getLogger(BottomPanel.class.getName());
    private final ColorScheme colorScheme;
    private final MainFrame mainFrame;

    private JLabel whiteLabel;
    private JPanel roundedWhitePanel;
    private boolean active = false;
    private JLabel statusWhiteLabel;
    private JLabel freeWhiteLabel;

    public BottomPanel(ColorScheme colorScheme, MainFrame mainFrame) {
        this.colorScheme = colorScheme;
        this.mainFrame = mainFrame;
        this.setBackground(colorScheme.getDarkerBackgroundColor());
        addComponents();
    }

    private void addComponents() {
        setLayout(new GridLayout(1,3));
        //Create 3 JPanels in the leftContainer to hold our content
        JPanel leftPanel = new ControlPanel();
        leftPanel.setLayout(new BorderLayout());
        JPanel middlePanel = new ControlPanel();
        middlePanel.setLayout(new BorderLayout());
        JPanel rightPanel = new ControlPanel();
        rightPanel.setLayout(new BorderLayout());

        //Add the 3 JPanels to the leftContainer
        add(leftPanel);
        add(middlePanel);
        add(rightPanel);

        //Add the content to the leftPanel1
        statusWhiteLabel = new CustomLabel("", colorScheme);
        statusWhiteLabel.setFont(colorScheme.getFont());
        statusWhiteLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusWhiteLabel.setForeground(colorScheme.getFontColor());
        leftPanel.add(statusWhiteLabel, BorderLayout.EAST);

        //Add the content to the leftPanel2
        whiteLabel = new CustomLabel("Weiß", colorScheme);
        whiteLabel.setFont(colorScheme.getFont());
        whiteLabel.setHorizontalAlignment(SwingConstants.CENTER);
        whiteLabel.setForeground(colorScheme.getFontColor());
        roundedWhitePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if(active) {
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setColor(colorScheme.getAccentColor()); // Set the color of the square
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20); // Draw a rounded rectangle
                }
            }
        };
        roundedWhitePanel.setOpaque(false);
        roundedWhitePanel.setLayout(new BorderLayout());
        roundedWhitePanel.add(whiteLabel, BorderLayout.CENTER);

        JPanel leftPlaceholder = new JPanel();
        JPanel rightPlaceholder = new JPanel();
        Dimension placeholderSize = new Dimension(100, this.getHeight());
        leftPlaceholder.setPreferredSize(placeholderSize);
        rightPlaceholder.setPreferredSize(placeholderSize);
        leftPlaceholder.setOpaque(false);
        rightPlaceholder.setOpaque(false);
        middlePanel.add(leftPlaceholder, BorderLayout.WEST);
        middlePanel.add(roundedWhitePanel);
        middlePanel.add(rightPlaceholder, BorderLayout.EAST);

        middlePanel.add(leftPlaceholder, BorderLayout.WEST);
        middlePanel.add(roundedWhitePanel);
        middlePanel.add(rightPlaceholder, BorderLayout.EAST);

        //Add the content to the leftPanel3
        freeWhiteLabel = new CustomLabel("", colorScheme);
        freeWhiteLabel.setFont(colorScheme.getFont());
        freeWhiteLabel.setHorizontalAlignment(SwingConstants.CENTER);
        freeWhiteLabel.setForeground(colorScheme.getFontColor());
        rightPanel.add(freeWhiteLabel);

        JButton backButton = new ExitButton("Spiel Verlassen", colorScheme);
        backButton.addActionListener(e -> {
            ConfirmDialog confirmDialog = new ConfirmDialog(mainFrame, "Spiel beenden?", "Verlassen bestätigen", colorScheme);
            confirmDialog.setVisible(true);
            if (confirmDialog.isConfirmed()) {
                mainFrame.switchToMenu();
            }
        });
        rightPanel.add(backButton, BorderLayout.EAST);
    }

    public void setWhiteActive (boolean active) {
        this.active = active;
        roundedWhitePanel.repaint();
        statusWhiteLabel.setText(active ? "Am Zug" : "Warten");
        LOGGER.log(Level.INFO, "Player 1 is active: " + active);
    }
    //set player name
    public void setWhiteName (String name) {
        whiteLabel.setText(name);
    }
}
