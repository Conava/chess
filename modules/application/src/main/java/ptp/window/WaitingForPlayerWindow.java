package ptp.window;

import ptp.components.ColorScheme;
import ptp.components.CustomButton;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class WaitingForPlayerWindow extends JDialog {
    public WaitingForPlayerWindow(Frame parent, ColorScheme colorScheme, String joinCode) {
        super(parent, "Waiting for Player", true);
        initializeDialog(colorScheme);
        addComponentsToDialog(colorScheme, joinCode);
    }

    private void initializeDialog(ColorScheme colorScheme) {
        setSize(350, 200);
        setResizable(false);
        setUndecorated(true);
        getContentPane().setBackground(colorScheme.getDarkerBackgroundColor());
        setLayout(new BorderLayout());
        setLocationRelativeTo(getParent());
    }

    private void addComponentsToDialog(ColorScheme colorScheme, String joinCode) {
        JTextArea messageArea = new JTextArea("Warte auf Gegner zum beitreten...\nJoin-Code: " + joinCode);
        messageArea.setFont(colorScheme.getFont().deriveFont(colorScheme.getFont().getSize2D() + 2));
        messageArea.setForeground(colorScheme.getFontColor());
        messageArea.setBackground(colorScheme.getDarkerBackgroundColor());
        messageArea.setEditable(false);
        messageArea.setWrapStyleWord(true);
        messageArea.setLineWrap(true);
        messageArea.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(messageArea, BorderLayout.CENTER);

        CustomButton cancelButton = new CustomButton("Schließen", colorScheme);
        cancelButton.addActionListener(e -> dispose());
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(colorScheme.getDarkerBackgroundColor());
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    @Override
    public void setVisible(boolean b) {
        if (b) {
            SwingUtilities.invokeLater(() -> {
                setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 30, 30));
            });
        }
        super.setVisible(b);
    }
}