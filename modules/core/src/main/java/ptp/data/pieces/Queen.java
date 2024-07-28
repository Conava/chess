package ptp.data.pieces;

import ptp.data.Player;
import ptp.data.enums.PlayerColor;

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