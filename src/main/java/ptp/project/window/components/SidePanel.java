package ptp.project.window.components;

import ptp.project.window.MainFrame;

import javax.swing.*;

public class SidePanel extends JPanel {
    private final ColorScheme colorScheme;
    private final MainFrame mainFrame;

    public SidePanel(ColorScheme colorScheme, MainFrame mainFrame) {
        this.colorScheme = colorScheme;
        this.mainFrame = mainFrame;
        this.setBackground(colorScheme.getDarkerBackgroundColor());

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(true);
    }

    public void updateList() {
    }
}
