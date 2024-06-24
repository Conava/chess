package ptp.project.data.pieces;

import ptp.project.data.Player;

public class Rook extends Piece {
    boolean hasMoved = false;

    public Rook(Player player) {
        super(player);
    }

    public void setHasMoved() {
        this.hasMoved = true;
    }

    public boolean getHasNotMoved() {
        return !this.hasMoved;
    }
}