package ptp.project.window.components;

import ptp.project.window.MainFrame;

import javax.swing.*;
import java.awt.*;

public abstract class ContentPane extends JPanel {
    protected final MainFrame mainFrame;

    protected ContentPane(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    protected void applyColorScheme(ColorScheme colorScheme) {
        applyBackgroundColor(this, colorScheme.getBackgroundColor());
        applyFontColor(this, colorScheme.getFontColor());
        applyButtonColor(this, colorScheme.getButtonColor());
    }

    private void applyFontColor(Component component, Color fontColor) {
        if (component instanceof JLabel) {
            ((JLabel) component).setForeground(fontColor);
        } else if (component instanceof JButton) {
            ((JButton) component).setForeground(fontColor);
        } else if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                applyFontColor(child, fontColor);
            }
        }
    }

    private void applyBackgroundColor(Component component, Color backgroundColor) {
        if (component instanceof JPanel) {
            ((JPanel) component).setBackground(backgroundColor);
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                applyBackgroundColor(child, backgroundColor);
            }
        }
    }

    private void applyButtonColor(Component component, Color buttonColor) {
        if (component instanceof JButton) {
            ((JButton) component).setBackground(buttonColor);
        } else if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                applyButtonColor(child, buttonColor);
            }
        }
    }
}