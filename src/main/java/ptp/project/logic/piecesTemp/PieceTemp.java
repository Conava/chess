package ptp.project.logic.piecesTemp;

import ptp.project.logic.PlayerTemp;

public abstract class PieceTemp {
    private final PlayerTemp playerTemp;

    public PieceTemp(PlayerTemp playerTemp) {
        this.playerTemp = playerTemp;

    }
    public PlayerTemp getPlayer() {
        return playerTemp;
    }
}
