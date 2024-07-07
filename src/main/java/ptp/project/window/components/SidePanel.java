package ptp.project.window.components;

import ptp.project.window.MainFrame;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.util.List;

public class SidePanel extends JPanel {
    private final ColorScheme colorScheme;
    private JPanel movesContainer; // Container for move labels

    public SidePanel(ColorScheme colorScheme) {
        this.colorScheme = colorScheme;
        this.setBackground(colorScheme.getDarkerBackgroundColor());
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(true);
    }

    public void activateList() {
        CustomLabel titleLabel = new CustomLabel("Spielzüge", colorScheme);
        titleLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, titleLabel.getPreferredSize().height));
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        titleLabel.setFont(new Font(titleLabel.getFont().getName(), Font.BOLD, 30));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Center the title
        titleLabel.setBorder(new MatteBorder(0, 0, 2, 0, colorScheme.getBorderColor()));

        add(titleLabel);

        movesContainer = new JPanel();
        movesContainer.setLayout(new BoxLayout(movesContainer, BoxLayout.Y_AXIS));
        movesContainer.setBackground(colorScheme.getDarkerBackgroundColor());

        JScrollPane scrollPane = new CustomScrollPane(movesContainer, colorScheme);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null); // Remove the border
        add(scrollPane);
    }

    public void updateList(List<String> moveList) {
        movesContainer.removeAll(); // Clear existing content
        for (int i = moveList.size() - 1; i >= 0; i--) {
            String move = moveList.get(i);
            JLabel moveLabel = new CustomLabel(move, colorScheme);
            moveLabel.setFont(new Font(moveLabel.getFont().getName(), Font.PLAIN, 22));
            moveLabel.setHorizontalAlignment(JLabel.CENTER); // Align label text to the center
            moveLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, moveLabel.getPreferredSize().height)); // Maximize width

            movesContainer.add(moveLabel);
        }
        movesContainer.revalidate();
        movesContainer.repaint();
    }
}