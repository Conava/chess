package io.github.conava.chess.core.data.pieces;

import io.github.conava.chess.core.data.player.Player;

public class Knight extends Piece {
    public Knight(Player player) {
        super(player);
    }

    @Override
    public Knight copy() {
        return new Knight(this.player);
    }
}