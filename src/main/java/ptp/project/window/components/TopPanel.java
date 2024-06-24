package ptp.project.window.components;

import ptp.project.window.MainFrame;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TopPanel extends ControlPanel {
    private static final Logger LOGGER = Logger.getLogger(TopPanel.class.getName());
    private final ColorScheme colorScheme;
    private final MainFrame mainFrame;

    private JLabel blackLabel;
    private JPanel rounded1Panel;
    private boolean active = false;
    private JLabel status1Label;
    private JLabel free1Label;

    public TopPanel(ColorScheme colorScheme, MainFrame mainFrame) {
        this.colorScheme = colorScheme;
        this.mainFrame = mainFrame;
        this.setBackground(colorScheme.getDarkerBackgroundColor());
        addComponents();
    }

    private void addComponents() {
        this.setLayout(new GridLayout(1, 3));

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
        status1Label = new CustomLabel("", colorScheme);
        status1Label.setFont(colorScheme.getFont());
        status1Label.setHorizontalAlignment(SwingConstants.CENTER);
        status1Label.setForeground(colorScheme.getFontColor());
        leftPanel.add(status1Label, BorderLayout.EAST);

        //Add the content to the leftPanel2
        blackLabel = new CustomLabel("Schwarz", colorScheme);
        blackLabel.setFont(colorScheme.getFont());
        blackLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        blackLabel.setHorizontalAlignment(SwingConstants.CENTER);
        blackLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        blackLabel.setHorizontalAlignment(SwingConstants.CENTER);
        blackLabel.setForeground(colorScheme.getFontColor());
        rounded1Panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (active) {
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setColor(colorScheme.getAccentColor()); // Set the color of the square
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20); // Draw a rounded rectangle
                }
            }
        };
        rounded1Panel.setOpaque(false);
        rounded1Panel.setFocusable(false);
        rounded1Panel.setLayout(new BorderLayout());
        rounded1Panel.add(blackLabel, BorderLayout.CENTER);

        JPanel leftPlaceholder = new JPanel();
        JPanel rightPlaceholder = new JPanel();
        Dimension placeholderSize = new Dimension(100, this.getHeight());
        leftPlaceholder.setPreferredSize(placeholderSize);
        rightPlaceholder.setPreferredSize(placeholderSize);
        leftPlaceholder.setOpaque(false);
        rightPlaceholder.setOpaque(false);
        middlePanel.add(leftPlaceholder, BorderLayout.WEST);
        middlePanel.add(rounded1Panel);
        middlePanel.add(rightPlaceholder, BorderLayout.EAST);

        //Add the content to the leftPanel3
        free1Label = new CustomLabel("", colorScheme);
        free1Label.setFont(colorScheme.getFont());
        free1Label.setHorizontalAlignment(SwingConstants.CENTER);
        free1Label.setForeground(colorScheme.getFontColor());
        rightPanel.add(free1Label);
    }

    public void setBlack(boolean active) {
        this.active = active;
        rounded1Panel.repaint();
        status1Label.setText(active ? "Am Zug" : "Warten");
        LOGGER.log(Level.INFO, "Player 1 / Black has active status: " + active);
    }

    //change player name
    public void setBlack(String name) {
        blackLabel.setText(name);
    }
}