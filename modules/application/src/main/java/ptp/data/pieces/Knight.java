package ptp.data.pieces;

import ptp.data.Player;
import ptp.data.enums.PlayerColor;

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