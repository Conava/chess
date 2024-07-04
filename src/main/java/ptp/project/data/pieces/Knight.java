package ptp.project.data.pieces;

import ptp.project.data.Player;
import ptp.project.data.enums.PlayerColor;

public class Knight extends Piece {
    public Knight(Player player) {
        super(player);
        if (player.color().equals(PlayerColor.WHITE)) {
            iconPath = "/icon/knight_white.png";
        } else {
            iconPath = "/icon/knight_black.png";
        }
    }
}