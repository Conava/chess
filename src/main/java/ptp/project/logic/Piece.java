package ptp.project.logic;

import java.util.List;

public abstract class Piece {
    private Player player;
    private boolean active;

    public Piece(Player player) {
        this.player = player;
        active = true;
    }
    public Player getPlayer() {
        return player;
    }

    public void kill() {
        active = false;
    }

    boolean getStatus() {
        return active;
    }
}
