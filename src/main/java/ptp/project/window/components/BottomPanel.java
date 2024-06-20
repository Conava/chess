package ptp.project.window.components;

import ptp.project.window.MainFrame;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BottomPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(BottomPanel.class.getName());
    private final ColorScheme colorScheme;
    private final MainFrame mainFrame;

    private JPanel leftContainer;
    private JPanel rightContainer;

    private JLabel player2Label;
    private JPanel rounded2Panel;
    private boolean active = false;
    private JLabel status2Label;
    private JLabel free2Label;

    public BottomPanel(ColorScheme colorScheme, MainFrame mainFrame) {
        this.colorScheme = colorScheme;
        this.mainFrame = mainFrame;
        this.setBackground(colorScheme.getDarkerBackgroundColor());
        initializeLayout();
        addComponents();
        this.setOpaque(true);
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
        status2Label = new CustomLabel("", colorScheme);
        status2Label.setFont(colorScheme.getFont());
        status2Label.setHorizontalAlignment(SwingConstants.CENTER);
        status2Label.setForeground(colorScheme.getFontColor());
        leftPanel1.add(status2Label, BorderLayout.EAST);

        //Add the content to the leftPanel2
        player2Label = new CustomLabel("Spieler 2", colorScheme);
        player2Label.setFont(colorScheme.getFont());
        player2Label.setHorizontalAlignment(SwingConstants.CENTER);
        player2Label.setForeground(colorScheme.getFontColor());
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
        rounded2Panel.setOpaque(true);
        rounded2Panel.setFocusable(false);
        rounded2Panel.setLayout(new BorderLayout());
        rounded2Panel.add(player2Label, BorderLayout.CENTER);

        JPanel leftPlaceholder = new JPanel();
        JPanel rightPlaceholder = new JPanel();
        Dimension placeholderSize = new Dimension(100, this.getHeight());
        leftPlaceholder.setPreferredSize(placeholderSize);
        rightPlaceholder.setPreferredSize(placeholderSize);
        leftPlaceholder.setOpaque(false);
        rightPlaceholder.setOpaque(false);
        leftPanel2.add(leftPlaceholder, BorderLayout.WEST);
        leftPanel2.add(rounded2Panel);
        leftPanel2.add(rightPlaceholder, BorderLayout.EAST);

        leftPanel2.add(leftPlaceholder, BorderLayout.WEST);
        leftPanel2.add(rounded2Panel);
        leftPanel2.add(rightPlaceholder, BorderLayout.EAST);

        //Add the content to the leftPanel3
        free2Label = new CustomLabel("", colorScheme);
        free2Label.setFont(colorScheme.getFont());
        free2Label.setHorizontalAlignment(SwingConstants.CENTER);
        free2Label.setForeground(colorScheme.getFontColor());
        leftPanel3.add(free2Label);

        JButton backButton = new ExitButton("Spiel Verlassen", colorScheme);
        backButton.addActionListener(e -> {
            ConfirmDialog confirmDialog = new ConfirmDialog(mainFrame, "Spiel beenden?", "Verlassen bestätigen", colorScheme);
            confirmDialog.setVisible(true);
            if (confirmDialog.isConfirmed()) {
                mainFrame.switchToMenu();
            }
        });
        backButton.setPreferredSize(new Dimension(rightContainer.getPreferredSize().width -10, 40));
        rightContainer.add(backButton);
    }

    public void setPlayer2(boolean active) {
        this.active = active;
        rounded2Panel.repaint();
        status2Label.setText(active ? "Am Zug" : "Warten");
        LOGGER.log(Level.INFO, "Player 1 is active: " + active);
    }
    //set player name
    public void setPlayer2Name(String name) {
        player2Label.setText(name);
    }
}
