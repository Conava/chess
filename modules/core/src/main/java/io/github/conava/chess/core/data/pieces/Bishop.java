package io.github.conava.chess.core.data.pieces;

import io.github.conava.chess.core.data.player.Player;

public class Bishop extends Piece {
    public Bishop(Player player) {
        super(player);
    }

    @Override
    public Bishop copy() {
        return new Bishop(this.player);
    }
}