package ptp.project.window.components;

import ptp.project.data.Square;
import ptp.project.data.pieces.Piece;

import javax.swing.*;
import java.awt.*;

public class BoardButton extends JButton {
    private final int row;
    private final int col;
    private final Color backgroundColor;

    public BoardButton(int row, int col, ColorScheme colorScheme) {
        this.row = row;
        this.col = col;
        this.backgroundColor = (row + col) % 2 == 0 ? colorScheme.getBrighterBackgroundColor() : colorScheme.getDarkerBackgroundColor();
        this.setBackground(backgroundColor);
        this.setFocusable(false);
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public Square getSquare() {
        return new Square(row, col);
    }

    public void setMarker() {
        this.setBackground(Color.GREEN); //todo: Change to a dot
    }

    public void removeMarker() {
        this.setBackground(backgroundColor);
    }

    public void setPieceIcon(Piece piece) {
        if (piece == null) {
            this.setIcon(null);
            return;
        }
        this.setIcon(piece.getIcon());
    }
}
