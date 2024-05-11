package ptp.project.window.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class DefaultButton extends JButton {
    private static final int ARC_WIDTH  = 15;
    private static final int ARC_HEIGHT = 15;
    private Color originalColor;

    public DefaultButton(String text) {
        super(text);
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFont(new Font("Arial", Font.BOLD, 20));
        setForeground(Color.WHITE);
        setFocusPainted(false);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                originalColor = getBackground();
                setBackground(originalColor.brighter().brighter());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setBackground(originalColor);
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