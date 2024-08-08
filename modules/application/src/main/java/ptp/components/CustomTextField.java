package ptp.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class CustomTextField extends JTextField implements FocusListener {
    private ColorScheme colorScheme;
    private static final int ARC_WIDTH = 15;
    private static final int ARC_HEIGHT = 15;
    private boolean isFieldEnabled = true;
    private boolean isFieldActive = false;

    public CustomTextField(ColorScheme colorScheme) {
        super();
        this.colorScheme = colorScheme;
        init();
    }

    private void init() {
        setOpaque(false);
        setForeground(colorScheme.getFontColor());
        setBackground(colorScheme.getBackgroundColor());
        setCaretColor(colorScheme.getFontColor());
        setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        addFocusListener(this);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        isFieldEnabled = enabled;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), ARC_WIDTH, ARC_HEIGHT);
        if (!isFieldEnabled) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        }
        super.paintComponent(g2);
        g2.dispose();
    }

    @Override
    protected void paintBorder(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(1.2f));
        g2.setColor(isFieldActive ? colorScheme.getAccentColor() : colorScheme.getBorderColor());
        if (!isFieldEnabled) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f));
        }
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC_WIDTH, ARC_HEIGHT);
        g2.dispose();
    }

    @Override
    public void focusGained(FocusEvent e) {
        isFieldActive = true;
        repaint();
    }

    @Override
    public void focusLost(FocusEvent e) {
        isFieldActive = false;
        repaint();
    }
}