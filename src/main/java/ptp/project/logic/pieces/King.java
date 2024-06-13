package ptp.project.logic.pieces;

import ptp.project.logic.Player;

public class King extends Piece {
    boolean hasMoved = false;

    public King(Player player) {
        super(player);
    }

    public void setHasMoved() {
        this.hasMoved = true;
    }

    public boolean getHasMoved() {
        return this.hasMoved;
    }
}
