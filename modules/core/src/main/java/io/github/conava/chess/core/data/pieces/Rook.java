package io.github.conava.chess.core.data.pieces;

import io.github.conava.chess.core.data.player.Player;

public class Rook extends Piece {
    boolean hasMoved = false;

    public Rook(Player player) {
        super(player);
    }

    public void setHasMoved() {
        this.hasMoved = true;
    }

    public boolean getHasNotMoved() {
        return !this.hasMoved;
    }
}