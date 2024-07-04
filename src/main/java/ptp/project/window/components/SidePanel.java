package ptp.project.window.components;

import ptp.project.window.MainFrame;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class SidePanel extends JPanel {
    private final ColorScheme colorScheme;
    private final MainFrame mainFrame;
    private JPanel movesContainer; // Container for move labels

    public SidePanel(ColorScheme colorScheme, MainFrame mainFrame) {
        this.colorScheme = colorScheme;
        this.mainFrame = mainFrame;
        this.setBackground(colorScheme.getDarkerBackgroundColor());
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(true);
    }

    public void activateList() {
        movesContainer = new JPanel();
        movesContainer.setLayout(new BoxLayout(movesContainer, BoxLayout.Y_AXIS));
        movesContainer.setBackground(colorScheme.getDarkerBackgroundColor());
        JScrollPane scrollPane = new JScrollPane(movesContainer);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane);
    }

    public void updateList(List<String> moveList) {
        movesContainer.removeAll(); // Clear existing content
        for (String move : moveList) {
            JLabel moveLabel = new JLabel(move); // Assuming Move has a meaningful toString() method
            moveLabel.setForeground(Color.WHITE); // Set text color
            movesContainer.add(moveLabel);
        }
        movesContainer.revalidate();
        movesContainer.repaint();
    }
}