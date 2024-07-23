package ptp.project.data.pieces;

import ptp.project.data.Player;
import ptp.project.data.enums.PlayerColor;

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