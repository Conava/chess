package ptp.window.components;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;

public class CustomComboBox<T> extends JComboBox<T> {
    private final ColorScheme colorScheme;
    private static final int ARC_WIDTH = 15;
    private static final int ARC_HEIGHT = 15;

    public CustomComboBox(T[] items, ColorScheme colorScheme) {
        super(items);
        this.colorScheme = colorScheme;
        init();
    }

    private void init() {
        setOpaque(false);
        setForeground(colorScheme.getFontColor());
        setBackground(colorScheme.getBackgroundColor());
        setRenderer(new CustomComboBoxRenderer());
        setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), ARC_WIDTH, ARC_HEIGHT);
        super.paintComponent(g2);
        g2.dispose();
    }

    @Override
    protected void paintBorder(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(1.2f));
        g2.setColor(colorScheme.getBorderColor());
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC_WIDTH, ARC_HEIGHT);
        g2.dispose();
    }

    private class CustomComboBoxRenderer extends BasicComboBoxRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (isSelected) {
                setBackground(colorScheme.getBrighterBackgroundColor());
                setForeground(colorScheme.getFontColor());
            } else {
                setBackground(colorScheme.getBackgroundColor());
                setForeground(colorScheme.getFontColor());
            }
            setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            return this;
        }
    }
}