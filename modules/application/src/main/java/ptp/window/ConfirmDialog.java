package ptp.window;

import ptp.components.ColorScheme;
import ptp.components.CustomButton;
import ptp.components.CustomLabel;
import ptp.components.ExitButton;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class ConfirmDialog extends JDialog {
    private boolean confirmed = false;

    public ConfirmDialog(JFrame parent, String message, String title, ColorScheme colorScheme) {
        super(parent, title, true);
        this.setSize(500, 300);
        this.setResizable(false);
        this.setUndecorated(true);
        Color dialogBackgroundColor = colorScheme.getDarkerBackgroundColor();
        this.getContentPane().setBackground(dialogBackgroundColor);

        this.setLayout(new BorderLayout());

        // Title panel with margin
        JPanel topLabelPanel = new JPanel(new BorderLayout());
        topLabelPanel.setBackground(colorScheme.getDarkerBackgroundColor());
        CustomLabel titleLabel = new CustomLabel(title, colorScheme);
        titleLabel.setFont(new Font(titleLabel.getFont().getName(), Font.BOLD, 26));
        topLabelPanel.add(titleLabel, BorderLayout.CENTER);
        topLabelPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, colorScheme.getBorderColor()),
                BorderFactory.createEmptyBorder(10, 20, 10, 20))); // Add margin
        this.add(topLabelPanel, BorderLayout.NORTH);

        // Message label with margin
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setBackground(dialogBackgroundColor);
        messagePanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20)); // Add margin
        String htmlMessage = convertToHtml(message);
        JLabel label = new CustomLabel(htmlMessage, colorScheme);
        label.setHorizontalAlignment(JLabel.CENTER);
        messagePanel.add(label, BorderLayout.CENTER);
        this.add(messagePanel, BorderLayout.CENTER);

        // Button panel with margin
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(dialogBackgroundColor);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20)); // Add margin
        JButton yesButton = new ExitButton("Ja", colorScheme);
        yesButton.setPreferredSize(new Dimension(100, 50));
        yesButton.addActionListener(e -> {
            confirmed = true;
            dispose();
        });
        buttonPanel.add(yesButton);

        JButton noButton = new CustomButton("Nein", colorScheme);
        noButton.setPreferredSize(new Dimension(100, 50));
        noButton.addActionListener(e -> {
            confirmed = false;
            dispose();
        });
        buttonPanel.add(noButton);

        this.add(buttonPanel, BorderLayout.SOUTH);

        this.setLocationRelativeTo(parent);
    }

    private String convertToHtml(String text) {
        return "<html>" + text.replace("\n", "<br>") + "</html>";
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    @Override
    public void setVisible(boolean b) {
        if (b) {
            SwingUtilities.invokeLater(() -> setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 30, 30)));
        }
        super.setVisible(b);
    }
}