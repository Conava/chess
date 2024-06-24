package ptp.project.data.pieces;

import ptp.project.data.Player;

public abstract class Piece {
    private final Player player;

    public Piece(Player player) {
        this.player = player;

    }
    public Player getPlayer() {
        return player;
    }
}
