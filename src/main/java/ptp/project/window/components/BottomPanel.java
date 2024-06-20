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
    private JPanel rounded2Panel;
    private boolean active = false;
    private JLabel status2Label;
    private JLabel free2Label;

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
        status2Label = new CustomLabel("", colorScheme);
        status2Label.setFont(colorScheme.getFont());
        status2Label.setHorizontalAlignment(SwingConstants.CENTER);
        status2Label.setForeground(colorScheme.getFontColor());
        leftPanel.add(status2Label, BorderLayout.EAST);

        //Add the content to the leftPanel2
        whiteLabel = new CustomLabel("Weiß", colorScheme);
        whiteLabel.setFont(colorScheme.getFont());
        whiteLabel.setHorizontalAlignment(SwingConstants.CENTER);
        whiteLabel.setForeground(colorScheme.getFontColor());
        rounded2Panel = new JPanel() {
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
        rounded2Panel.setOpaque(false);
        rounded2Panel.setLayout(new BorderLayout());
        rounded2Panel.add(whiteLabel, BorderLayout.CENTER);

        JPanel leftPlaceholder = new JPanel();
        JPanel rightPlaceholder = new JPanel();
        Dimension placeholderSize = new Dimension(100, this.getHeight());
        leftPlaceholder.setPreferredSize(placeholderSize);
        rightPlaceholder.setPreferredSize(placeholderSize);
        leftPlaceholder.setOpaque(false);
        rightPlaceholder.setOpaque(false);
        middlePanel.add(leftPlaceholder, BorderLayout.WEST);
        middlePanel.add(rounded2Panel);
        middlePanel.add(rightPlaceholder, BorderLayout.EAST);

        middlePanel.add(leftPlaceholder, BorderLayout.WEST);
        middlePanel.add(rounded2Panel);
        middlePanel.add(rightPlaceholder, BorderLayout.EAST);

        //Add the content to the leftPanel3
        free2Label = new CustomLabel("", colorScheme);
        free2Label.setFont(colorScheme.getFont());
        free2Label.setHorizontalAlignment(SwingConstants.CENTER);
        free2Label.setForeground(colorScheme.getFontColor());
        rightPanel.add(free2Label);

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

    public void setPlayer2(boolean active) {
        this.active = active;
        rounded2Panel.repaint();
        status2Label.setText(active ? "Am Zug" : "Warten");
        LOGGER.log(Level.INFO, "Player 1 is active: " + active);
    }
    //set player name
    public void setPlayer2Name(String name) {
        whiteLabel.setText(name);
    }
}
