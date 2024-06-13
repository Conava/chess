package ptp.project.window;

import ptp.project.window.components.ColorScheme;
import ptp.project.window.components.CustomButton;
import ptp.project.window.components.CustomLabel;

import javax.swing.*;
import java.awt.*;
import java.util.logging.*;
import java.util.logging.Logger;
import javax.swing.BorderFactory;

public class Settings extends JFrame {
    private MainFrame mainFrame;
    private ColorScheme colorScheme;
    public static final Logger LOGGER = Logger.getLogger(Settings.class.getName());

    public Settings(MainFrame mainFrame, ColorScheme colorScheme) {
        LOGGER.log(Level.INFO, "Settings initialized");
        this.mainFrame = mainFrame;
        this.colorScheme = colorScheme;
        this.setTitle("Settings");
        //this.getContentPane().setBackground(colorScheme.getBackgroundColor());

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(500, 400);
        this.setVisible(true);
        this.setLocationRelativeTo(null);

        addUIComponents();
    }

    private void addUIComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(colorScheme.getBackgroundColor());

        JLabel label = new CustomLabel("Die Einstellungen sind noch nicht implementiert", colorScheme);
        label.setVerticalAlignment(SwingConstants.CENTER);
        mainPanel.add(label, BorderLayout.CENTER);

        JButton backToMenu = new CustomButton("Back to menu", colorScheme);
        backToMenu.addActionListener(e -> {
            mainFrame.switchToMenu();
            dispose();
        });

        // Set padding to the button
        backToMenu.setBorder(BorderFactory.createCompoundBorder(
                backToMenu.getBorder(),
                BorderFactory.createEmptyBorder(10, 30, 10, 30) // top, left, bottom, right padding
        ));

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setBackground(colorScheme.getBackgroundColor());
        bottomPanel.add(backToMenu);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        this.add(mainPanel);
    }
}