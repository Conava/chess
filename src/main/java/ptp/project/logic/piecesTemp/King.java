package ptp.project.logic.piecesTemp;

import ptp.project.logic.PlayerTemp;

public class King extends PieceTemp {
    boolean hasMoved = false;

    public King(PlayerTemp playerTemp) {
        super(playerTemp);
    }

    public void setHasMoved() {
        this.hasMoved = true;
    }

    public boolean getHasMoved() {
        return this.hasMoved;
    }
}
