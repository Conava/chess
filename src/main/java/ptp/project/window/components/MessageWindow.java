package ptp.project.window.components;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;

public class MessageWindow extends JDialog {

    public MessageWindow(JFrame parent, String message, String title, ColorScheme colorScheme) {
        super(parent, title, true);
        this.setSize(400, 200);
        this.setResizable(false);
        this.setUndecorated(true);
        this.getContentPane().setBackground(colorScheme.getDarkerBackgroundColor());

        this.setLayout(new BorderLayout());

        // Create a title label with a larger font
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(colorScheme.getFont().deriveFont(colorScheme.getFont().getSize2D() +4));
        titleLabel.setForeground(colorScheme.getFontColor());
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        this.add(titleLabel, BorderLayout.NORTH);

        // Add a grey border under the title
        titleLabel.setBorder(new MatteBorder(0, 0, 1, 0, Color.GRAY));
        this.add(titleLabel, BorderLayout.NORTH);

        // Create a message label with a smaller font
        JLabel messageLabel = new JLabel(message);
        messageLabel.setFont(colorScheme.getFont().deriveFont(colorScheme.getFont().getSize2D() - 2));
        messageLabel.setHorizontalAlignment(JLabel.CENTER);
        messageLabel.setForeground(colorScheme.getBorderColor());
        this.add(messageLabel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(colorScheme.getDarkerBackgroundColor());

        JButton okButton = new CustomButton("OK", colorScheme);
        okButton.setPreferredSize(new Dimension(this.getWidth()-40 , 50));
        okButton.addActionListener(e -> dispose());
        buttonPanel.add(okButton);

        this.add(buttonPanel, BorderLayout.SOUTH);

        this.setLocationRelativeTo(parent);
    }

    @Override
    public void setVisible(boolean b) {
        if (b) {
            // Wait until the window is visible to set the shape
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    // Set the shape of the window
                    setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 30, 30));
                }
            });
        }
        super.setVisible(b);
    }
}