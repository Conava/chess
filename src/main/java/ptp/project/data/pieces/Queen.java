package ptp.project.data.pieces;

import ptp.project.data.Player;
import ptp.project.data.enums.PlayerColor;

public class Queen extends Piece {
    public Queen(Player player) {
        super(player);
        if (player.getColor().equals(PlayerColor.WHITE)) {
            iconPath = "/icon/crown_white.png";
        } else {
            iconPath = "/icon/crown_black.png";
        }
    }
}