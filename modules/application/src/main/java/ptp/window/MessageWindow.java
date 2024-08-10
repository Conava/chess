package ptp.window;

import ptp.components.ColorScheme;
import ptp.components.CustomButton;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class MessageWindow extends JDialog {

    public MessageWindow(Frame parent, String message, String title, ColorScheme colorScheme) {
        super(parent, title, true);
        this.setResizable(false);
        this.setUndecorated(true);
        this.getContentPane().setBackground(colorScheme.getDarkerBackgroundColor());

        this.setLayout(new BorderLayout());

        // Create a title label with a larger font
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(colorScheme.getFont().deriveFont(colorScheme.getFont().getSize2D() + 4));
        titleLabel.setForeground(colorScheme.getFontColor());
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        titleLabel.setBorder(new MatteBorder(0, 0, 1, 0, Color.GRAY));
        this.add(titleLabel, BorderLayout.NORTH);

        // Create a message label with a smaller font
        JTextArea messageArea = new JTextArea(message);
        messageArea.setFont(colorScheme.getFont().deriveFont(colorScheme.getFont().getSize2D() - 2));
        messageArea.setForeground(colorScheme.getFontColor());
        messageArea.setBackground(colorScheme.getDarkerBackgroundColor());
        messageArea.setEditable(false);
        messageArea.setWrapStyleWord(true);
        messageArea.setLineWrap(true);
        messageArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        this.add(messageArea, BorderLayout.CENTER);

        // Adjust window size based on message content
        int width = 400;
        int height = Math.max(200, messageArea.getPreferredSize().height + 100);
        this.setSize(width, height);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(colorScheme.getDarkerBackgroundColor());

        JButton okButton = new CustomButton("OK", colorScheme);
        okButton.setPreferredSize(new Dimension(this.getWidth() - 40, 50));
        okButton.addActionListener(e -> dispose());
        buttonPanel.add(okButton);

        this.add(buttonPanel, BorderLayout.SOUTH);

        this.setLocationRelativeTo(parent);
    }

    @Override
    public void setVisible(boolean b) {
        if (b) {
            // Wait until the window is visible to set the shape
            SwingUtilities.invokeLater(() -> {
                // Set the shape of the window
                setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 30, 30));
            });
        }
        super.setVisible(b);
    }
}