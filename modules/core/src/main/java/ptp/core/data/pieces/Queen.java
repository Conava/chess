package ptp.core.data.pieces;

import ptp.core.data.Player;
import ptp.core.data.enums.PlayerColor;

public class Queen extends Piece {
    public Queen(Player player) {
        super(player);
        if (player.color().equals(PlayerColor.WHITE)) {
            iconPath = "/icon/queen_white.png";
        } else {
            iconPath = "/icon/queen_black.png";
        }
    }
}