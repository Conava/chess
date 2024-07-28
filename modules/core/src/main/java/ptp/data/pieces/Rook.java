package ptp.data.pieces;

import ptp.data.Player;
import ptp.data.enums.PlayerColor;

public class Rook extends Piece {
    boolean hasMoved = false;

    public Rook(Player player) {
        super(player);
        if (player.color().equals(PlayerColor.WHITE)) {
            iconPath = "/icon/rook_white.png";
        } else {
            iconPath = "/icon/rook_black.png";
        }
    }

    public void setHasMoved() {
        this.hasMoved = true;
    }

    public boolean getHasNotMoved() {
        return !this.hasMoved;
    }
}