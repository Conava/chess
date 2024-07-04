package ptp.project.window.components;

import ptp.project.data.Square;
import ptp.project.data.board.Board;
import ptp.project.window.ChessGame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;

public class BoardPanel extends JPanel {
    private final ColorScheme colorScheme;
    private final ChessGame chessGame;
    private JPanel leftPanel;
    private JPanel topPanel;

    private final BoardButton[][] boardButtons = new BoardButton[8][8];
    private final List<BoardButton> markedSquares = new ArrayList<>();

    public BoardPanel(ColorScheme colorScheme, ChessGame chessGame) {
        this.colorScheme = colorScheme;
        this.chessGame = chessGame;
        this.setBackground(colorScheme.getBrighterBackgroundColor());

        setLayout(new BorderLayout());
        setOpaque(false);

        addBoardLabels();
        addBoard();
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                adjustPanelSizes();
            }
        });
    }

    public void setLegalSquares(List<Square> legalSquares) {
        System.out.println("Setting legal squares with size: " + legalSquares.size());
        unsetLegalSquares();
        for (Square square : legalSquares) {
            boardButtons[square.getY()][square.getX()].setMarker();
            markedSquares.add(boardButtons[square.getY()][square.getX()]);
        }
    }

    public void unsetLegalSquares() {
        for (BoardButton square : markedSquares) {
            square.removeMarker();
        }
        markedSquares.clear();
    }

    public void placePieces(Board gameBoard) {
        for (BoardButton[] boardButton : boardButtons) {
            for (BoardButton button : boardButton) {
                button.setPieceIcon(gameBoard.getPieceAt(button.getSquare()));
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

private void addBoard() {
    JPanel board = new JPanel(new GridLayout(8, 8));
    board.setOpaque(true);

    // Create the board squares
    for (int x = 7; x >= 0; x--) {
        for (int y = 0; y < 8; y++) {
            BoardButton bardSquare = new BoardButton(x, y, colorScheme);
            bardSquare.setBorder(null);
            bardSquare.addActionListener(e -> clickedOn(bardSquare.getSquare()));
            boardButtons[x][y] = bardSquare;
            board.add(bardSquare);
        }
    }
    add(board, BorderLayout.CENTER);
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
