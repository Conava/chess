package ptp.project.data.pieces;

import ptp.project.data.Player;

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
