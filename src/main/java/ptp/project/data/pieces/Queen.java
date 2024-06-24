package ptp.project.data.pieces;

import ptp.project.data.Player;

public class Queen extends Piece {
    public Queen(Player player) {
        super(player);
        if (player.getColor().equals("white")) {
            iconPath = "/icon/crown_white.png";
        } else {
            iconPath = "/icon/crown_black.png";
        }
    }
}