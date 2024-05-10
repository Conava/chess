package ptp.project.logic;

import java.util.List;

public abstract class Piece {
    private Player player;
    private Square position;
    private Game game;
    private boolean active;

    public Piece(Player player, Square position, Game game) {
        this.player = player;
        this.position = position;
        this.game = game;
        active = true;
    }
    public Player getPlayer() {
        return player;
    }

    public Square getPosition() {
        return position;
    }

    public List<Square> getLegalMoves() {
        return game.getLegalMoves(this);
    }

    public void kill() {
        active = false;
    }

    void move(Square newPosition) {
        if (getLegalMoves().contains(newPosition)) {
            position = newPosition;
        }
    }

    boolean getStatus() {
        return active;
    }

    Board getBoard() {
        return game.getBoard();
    }
}
