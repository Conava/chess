package ptp.project.window.components;

import ptp.project.window.MainFrame;

import javax.swing.*;
import java.awt.*;

public class BottomPanel extends JPanel {
    private final ColorScheme colorScheme;
    private final MainFrame mainFrame;
    private final JButton backButton;

    public BottomPanel(ColorScheme colorScheme, MainFrame mainFrame) {
        this.colorScheme = colorScheme;
        this.mainFrame = mainFrame;
        this.setBackground(colorScheme.getAccentColor());
        this.setLayout(new FlowLayout(FlowLayout.RIGHT));
        this.setOpaque(true);

        backButton = new ExitButton("Spiel Verlassen", colorScheme);
        backButton.addActionListener(e -> mainFrame.switchToMenu());
        backButton.setPreferredSize(new Dimension(backButton.getPreferredSize().width, 50));
        this.add(backButton);
    }
}
