package ptp.project.window.components;

import ptp.project.window.MainFrame;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TopPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(TopPanel.class.getName());
    private final ColorScheme colorScheme;
    private final MainFrame mainFrame;

    private JPanel leftContainer;
    private JPanel rightContainer;

    private JLabel player1Label;
    private JPanel rounded1Panel;
    private boolean active = false;
    private JLabel status1Label;
    private JLabel free1Label;

    public TopPanel(ColorScheme colorScheme, MainFrame mainFrame) {
        this.colorScheme = colorScheme;
        this.mainFrame = mainFrame;
        this.setBackground(colorScheme.getDarkerBackgroundColor());
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
        leftPanel1.setLayout(new BorderLayout());
        JPanel leftPanel2 = new ControlPanel();
        leftPanel2.setLayout(new BorderLayout());
        JPanel leftPanel3 = new ControlPanel();
        leftPanel3.setLayout(new BorderLayout());

        //Add the 3 JPanels to the leftContainer
        leftContainer.add(leftPanel1);
        leftContainer.add(leftPanel2);
        leftContainer.add(leftPanel3);

        //Add the content to the leftPanel1
        status1Label = new CustomLabel("", colorScheme);
        status1Label.setFont(colorScheme.getFont());
        status1Label.setHorizontalAlignment(SwingConstants.CENTER);
        status1Label.setForeground(colorScheme.getFontColor());
        leftPanel1.add(status1Label, BorderLayout.EAST);

        //Add the content to the leftPanel2
        player1Label = new CustomLabel("Spieler 1", colorScheme);
        player1Label.setFont(colorScheme.getFont());
        player1Label.setAlignmentX(Component.CENTER_ALIGNMENT);
        player1Label.setHorizontalAlignment(SwingConstants.CENTER);
        player1Label.setHorizontalTextPosition(SwingConstants.CENTER);
        player1Label.setHorizontalAlignment(SwingConstants.CENTER);
        player1Label.setForeground(colorScheme.getFontColor());
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
        rounded1Panel.add(player1Label, BorderLayout.CENTER);

        JPanel leftPlaceholder = new JPanel();
        JPanel rightPlaceholder = new JPanel();
        Dimension placeholderSize = new Dimension(100, this.getHeight());
        leftPlaceholder.setPreferredSize(placeholderSize);
        rightPlaceholder.setPreferredSize(placeholderSize);
        leftPlaceholder.setOpaque(false);
        rightPlaceholder.setOpaque(false);
        leftPanel2.add(leftPlaceholder, BorderLayout.WEST);
        leftPanel2.add(rounded1Panel);
        leftPanel2.add(rightPlaceholder, BorderLayout.EAST);

        //Add the content to the leftPanel3
        free1Label = new CustomLabel("", colorScheme);
        free1Label.setFont(colorScheme.getFont());
        free1Label.setHorizontalAlignment(SwingConstants.CENTER);
        free1Label.setForeground(colorScheme.getFontColor());
        leftPanel3.add(free1Label);

        // In the TopPanel class
        JButton demoButton = new CustomButton("Demo", colorScheme);
        demoButton.addActionListener(e -> {
            mainFrame.demo();
        });
        rightContainer.add(demoButton);
    }

    public void setPlayer1(boolean active) {
        this.active = active;
        rounded1Panel.repaint();
        status1Label.setText(active ? "Am Zug" : "Warten");
        LOGGER.log(Level.INFO, "Player 1 is active: " + active);
    }

    //change player name
    public void setPlayer1Name(String name) {
        player1Label.setText(name);
    }
}