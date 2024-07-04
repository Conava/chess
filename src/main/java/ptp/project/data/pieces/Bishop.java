package ptp.project.data.pieces;

import ptp.project.data.Player;
import ptp.project.data.enums.PlayerColor;

public class Bishop extends Piece {
    public Bishop(Player player) {
        super(player);
        if (player.color().equals(PlayerColor.WHITE)) {
            iconPath = "/icon/bishop_white.png";
        } else {
            iconPath = "/icon/bishop_black.png";
        }
    }
}