package ptp.project.data.pieces;

import ptp.project.data.Player;

public class Knight extends Piece {
    public Knight(Player player) {
        super(player);
        if (player.getColor().equals("white")) {
            iconPath = "/icon/knight_white.png";
        } else {
            iconPath = "/icon/knight_black.png";
        }
    }
}