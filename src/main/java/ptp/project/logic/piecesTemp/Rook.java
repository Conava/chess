package ptp.project.logic.piecesTemp;

import ptp.project.logic.PlayerTemp;

public class Rook extends PieceTemp {
    boolean hasMoved = false;

    public Rook(PlayerTemp playerTemp) {
        super(playerTemp);
    }

    public void setHasMoved() {
        this.hasMoved = true;
    }

    public boolean getHasNotMoved() {
        return !this.hasMoved;
    }
}