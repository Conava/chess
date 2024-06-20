package ptp.project.logic.pieces;

import ptp.project.logic.Player;

public abstract class Piece {
    private final Player player;

    public Piece(Player player) {
        this.player = player;

    }
    public Player getPlayer() {
        return player;
    }
}
