package ptp.window.components;

import javax.swing.*;

public class CustomLabel extends JLabel {
    public CustomLabel(String text, ColorScheme colorScheme) {
        super(text);
        setFont(colorScheme.getFont());
        setForeground(colorScheme.getFontColor());
    }
}
