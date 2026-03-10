package io.github.conava.chess.core.data.pieces;

import io.github.conava.chess.core.data.player.Player;

public class King extends Piece {
    boolean hasMoved = false;

    public King(Player player) {
        super(player);
    }

    public void setHasMoved() {
        this.hasMoved = true;
    }

    public boolean getHasMoved() {
        return this.hasMoved;
    }

    @Override
    public King copy() {
        King copy = new King(this.player);
        if (this.hasMoved) {
            copy.setHasMoved();
        }
        return copy;
    }
}
