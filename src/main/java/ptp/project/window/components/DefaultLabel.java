package ptp.project.window.components;

import javax.swing.*;

public class DefaultLabel extends JLabel {
    public DefaultLabel(String text, ColorScheme colorScheme) {
        super(text);
        setFont(colorScheme.getFont());
        setForeground(colorScheme.getFontColor());
    }
}
