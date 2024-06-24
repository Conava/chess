package ptp.project.data.pieces;

import ptp.project.data.Player;

public class Bishop extends Piece {
    public Bishop(Player player) {
        super(player);
        if (player.getColor().equals("white")) {
            iconPath = "/icon/bishop_white.png";
        } else {
            iconPath = "/icon/bishop_black.png";
        }
    }
}