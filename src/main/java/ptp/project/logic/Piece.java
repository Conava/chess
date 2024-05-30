package ptp.project.logic;

import java.util.List;

public abstract class Piece {
    private Player player;
    private Game game;
    private boolean active;

    public Piece(Player player, Game game) {
        this.player = player;
        this.game = game;
        active = true;
    }
    public Player getPlayer() {
        return player;
    }

    public List<Square> getLegalMoves() {
        return game.getLegalMoves(this);
    }

    public void kill() {
        active = false;
    }

    boolean getStatus() {
        return active;
    }
}
