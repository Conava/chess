package ptp.window;

import ptp.components.ColorScheme;
import ptp.components.CustomButton;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * A dialog window that displays a message indicating that the player is waiting for an opponent to join.
 */
public class WaitingForPlayerWindow extends JDialog {

    /**
     * Constructs a new WaitingForPlayerWindow.
     *
     * @param parent      The parent frame of the dialog.
     * @param colorScheme The color scheme to be used for the dialog.
     * @param joinCode    The join code to be displayed in the dialog.
     */
    public WaitingForPlayerWindow(Frame parent, ColorScheme colorScheme, String joinCode) {
        super(parent, "Waiting for Player", true);
        initializeDialog(colorScheme);
        addComponentsToDialog(colorScheme, joinCode);
    }

    /**
     * Initializes the dialog properties.
     *
     * @param colorScheme The color scheme to be used for the dialog.
     */
    private void initializeDialog(ColorScheme colorScheme) {
        setSize(350, 200);
        setResizable(false);
        setUndecorated(true);
        getContentPane().setBackground(colorScheme.getDarkerBackgroundColor());
        setLayout(new BorderLayout());
        setLocationRelativeTo(getParent());
    }

    /**
     * Adds components to the dialog.
     *
     * @param colorScheme The color scheme to be used for the dialog.
     * @param joinCode    The join code to be displayed in the dialog.
     */
    private void addComponentsToDialog(ColorScheme colorScheme, String joinCode) {
        JTextArea messageArea = createMessageArea(colorScheme, joinCode);
        add(messageArea, BorderLayout.CENTER);

        JPanel buttonPanel = createButtonPanel(colorScheme);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * Creates the message area component.
     *
     * @param colorScheme The color scheme to be used for the message area.
     * @param joinCode    The join code to be displayed in the message area.
     * @return The configured JTextArea component.
     */
    private JTextArea createMessageArea(ColorScheme colorScheme, String joinCode) {
        JTextArea messageArea = new JTextArea("Warte auf Gegner zum beitreten...\nJoin-Code: " + joinCode);
        messageArea.setFont(colorScheme.getFont().deriveFont(colorScheme.getFont().getSize2D() + 2));
        messageArea.setForeground(colorScheme.getFontColor());
        messageArea.setBackground(colorScheme.getDarkerBackgroundColor());
        messageArea.setEditable(false);
        messageArea.setWrapStyleWord(true);
        messageArea.setLineWrap(true);
        messageArea.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        return messageArea;
    }

    /**
     * Creates the button panel component.
     *
     * @param colorScheme The color scheme to be used for the button panel.
     * @return The configured JPanel component.
     */
    private JPanel createButtonPanel(ColorScheme colorScheme) {
        CustomButton cancelButton = new CustomButton("Schließen", colorScheme);
        cancelButton.addActionListener(e -> dispose());

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(colorScheme.getDarkerBackgroundColor());
        buttonPanel.add(cancelButton);
        return buttonPanel;
    }

    /**
     * Sets the visibility of the dialog and applies a rounded rectangle shape when visible.
     *
     * @param isVisible true to make the dialog visible, false to hide it.
     */
    @Override
    public void setVisible(boolean isVisible) {
        if (isVisible) {
            SwingUtilities.invokeLater(() -> setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 30, 30)));
        }
        super.setVisible(isVisible);
    }
}