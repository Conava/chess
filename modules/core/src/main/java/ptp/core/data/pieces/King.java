package ptp.core.data.pieces;

import ptp.core.data.Player;
import ptp.core.data.enums.PlayerColor;

public class King extends Piece {
    boolean hasMoved = false;

    public King(Player player) {
        super(player);
        if (player.color().equals(PlayerColor.WHITE)) {
            iconPath = "/icon/king_white.png";
        } else {
            iconPath = "/icon/king_black.png";
        }
    }

    public void setHasMoved() {
        this.hasMoved = true;
    }

    public boolean getHasMoved() {
        return this.hasMoved;
    }
}
