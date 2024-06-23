package ptp.project.window.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;

public class ConfirmDialog extends JDialog {
    private boolean confirmed = false;

    public ConfirmDialog(JFrame parent, String message, String title, ColorScheme colorScheme) {
        super(parent, title, true);
        this.setSize(400, 200);
        this.setResizable(false);
        this.setUndecorated(true);
        Color dialogBackgroundColor = colorScheme.getBackgroundColor().darker();
        this.getContentPane().setBackground(dialogBackgroundColor);

        this.setLayout(new BorderLayout());

        JLabel label = new CustomLabel(message, colorScheme);
        label.setHorizontalAlignment(JLabel.CENTER);
        this.add(label, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(dialogBackgroundColor);

        JButton yesButton = new ExitButton("Ja", colorScheme);
        yesButton.setPreferredSize(new Dimension(100, 50));
        yesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmed = true;
                dispose();
            }
        });
        buttonPanel.add(yesButton);
        JButton noButton = new CustomButton("Nein", colorScheme);
        noButton.setPreferredSize(new Dimension(100, 50));
        noButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmed = false;
                dispose();
            }
        });
        buttonPanel.add(noButton);

        this.add(buttonPanel, BorderLayout.SOUTH);

        this.setLocationRelativeTo(parent);
    }

    public boolean isConfirmed() {
        return confirmed;
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