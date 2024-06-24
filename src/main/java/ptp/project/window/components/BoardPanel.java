package ptp.project.window.components;

import ptp.project.data.Square;
import ptp.project.window.ChessGame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;

public class BoardPanel extends JPanel {
    private final ColorScheme colorScheme;
    private final ChessGame chessGame;
    private final JPanel board;
    private JPanel leftPanel;
    private JPanel topPanel;

    public BoardPanel(ColorScheme colorScheme, ChessGame chessGame) {
        this.colorScheme = colorScheme;
        this.chessGame = chessGame;
        this.setBackground(colorScheme.getBrighterBackgroundColor());

        setLayout(new BorderLayout());
        setOpaque(false);

        addBoardLabels();
        board = addBoard();
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                adjustPanelSizes();
                System.out.println("Top Panel Size: " + topPanel.getSize());
                System.out.println("Left Panel Size: " + leftPanel.getSize());
                System.out.println("Board Size: " + board.getSize());
            }
        });
    }

    public void setLegalSquares(List<Square> legalSquares) {
        for (Component component : board.getComponents()) {
            if (component instanceof BoardButton button) {
                Square square = button.getSquare();
                if (legalSquares.contains(square)) {
                    button.setMarker();
                }
            }
        }
    }

    private void addBoardLabels() {
        // 2 panels on the left and top of the board displaying the row and column numbers
        leftPanel = new JPanel(new GridLayout(8, 1));
        leftPanel.setOpaque(false);
        topPanel = new JPanel(new GridLayout(1, 8));
        topPanel.setOpaque(false);

        // Add the row and column numbers to the panels
        for (int i = 8; i > 0; i--) {
            JLabel rowLabel = new JLabel(String.valueOf(i), SwingConstants.CENTER);
            rowLabel.setFont(colorScheme.getFont());
            rowLabel.setForeground(colorScheme.getFontColor());
            leftPanel.add(rowLabel);

            JLabel columnLabel = new JLabel(String.valueOf((char) ('A' + 8 - i)), SwingConstants.CENTER);
            columnLabel.setFont(colorScheme.getFont());
            columnLabel.setForeground(colorScheme.getFontColor());
            topPanel.add(columnLabel);
        }

        // Add the panels to the board
        add(leftPanel, BorderLayout.WEST);
        add(topPanel, BorderLayout.NORTH);
    }

    private JPanel addBoard() {
        JPanel board = new JPanel(new GridLayout(8, 8));
        board.setOpaque(true);

        // Create the board squares
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                BoardButton square = new BoardButton(i, j, colorScheme);
                square.setBorder(null);
                square.addActionListener(e -> {
                    clickedOn(square.getSquare());
                });
                board.add(square);
            }
        }

        add(board, BorderLayout.CENTER);
        return board;
    }

    private void clickedOn(Square square) {
        chessGame.clickedOn(square);
    }

    private void adjustPanelSizes() {
        Dimension topPanelSize = topPanel.getPreferredSize();
        topPanelSize.height = topPanelSize.height + (this.getHeight() - topPanelSize.height) % 8;
        topPanel.setPreferredSize(topPanelSize);

        Dimension leftPanelSize = leftPanel.getPreferredSize();
        leftPanelSize.width = leftPanelSize.width + (this.getWidth() - leftPanelSize.width) % 8;
        leftPanel.setPreferredSize(leftPanelSize);

        this.revalidate();
        this.repaint();
    }
}
