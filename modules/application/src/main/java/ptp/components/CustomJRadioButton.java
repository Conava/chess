package ptp.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class CustomJRadioButton extends JRadioButton {
    protected static final int ARC_WIDTH  = 15;
    protected static final int ARC_HEIGHT = 15;
    protected final ColorScheme colorScheme;

    public CustomJRadioButton(String text, ColorScheme colorScheme) {
        super(text);
        this.colorScheme = colorScheme;
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFont(colorScheme.getFont());
        setForeground(colorScheme.getFontColor());
        setBackground(colorScheme.getButtonColor());
        setFocusPainted(false);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setBackground(colorScheme.getButtonHoverColor());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setBackground(colorScheme.getButtonColor());
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        int width = getWidth();
        int height = getHeight();
        Graphics2D graphics = (Graphics2D) g;
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draws the rounded opaque panel with borders.
        graphics.setColor(getBackground());
        graphics.fillRoundRect(0, 0, width, height, ARC_WIDTH, ARC_HEIGHT);
        super.paintComponent(graphics);
    }
}