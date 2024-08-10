package ptp.components;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

public class CustomScrollPane extends JScrollPane {
    public CustomScrollPane(Component view, ColorScheme colorScheme) {
        super(view);
        this.getViewport().setBackground(colorScheme.getDarkerBackgroundColor());
        this.setBackground(colorScheme.getDarkerBackgroundColor());
        this.setForeground(colorScheme.getFontColor());
        this.getVerticalScrollBar().setUnitIncrement(14);


        customizeScrollBar(this.getVerticalScrollBar(), colorScheme);
        customizeScrollBar(this.getHorizontalScrollBar(), colorScheme);

        this.setBorder(BorderFactory.createEmptyBorder());
    }

    private void customizeScrollBar(JScrollBar scrollBar, ColorScheme colorScheme) {
        scrollBar.setUI(new BasicScrollBarUI() {
            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroSizeButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroSizeButton();
            }

            private JButton createZeroSizeButton() {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                button.setMinimumSize(new Dimension(0, 0));
                button.setMaximumSize(new Dimension(0, 0));
                return button;
            }

            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = colorScheme.getAccentColor();
                this.trackColor = colorScheme.getDarkerBackgroundColor();
            }

            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                if (!thumbBounds.isEmpty() && this.scrollbar.isEnabled()) {
                    int w = thumbBounds.width - 8; // Decrease the width
                    int h = thumbBounds.height - 8; // Decrease the height if necessary
                    int x = thumbBounds.x + 4; // Center the thumb
                    int y = thumbBounds.y + 4;
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(thumbColor);
                    g2.fillRoundRect(x, y, w, h, 10, 10); // Rounded corners
                    g2.dispose();
                }
            }
        });

        scrollBar.setBackground(colorScheme.getDarkerBackgroundColor());
    }
}