package ptp.project.window.components;

import ptp.project.data.Square;

import javax.swing.*;
import java.awt.*;

public class BoardButton extends JButton {
    private int row;
    private int col;

    public BoardButton(int row, int col, ColorScheme colorScheme) {
        this.row = row;
        this.col = col;
        this.setBackground((row + col) % 2 == 0 ? colorScheme.getBrighterBackgroundColor() : colorScheme.getDarkerBackgroundColor());
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
}
