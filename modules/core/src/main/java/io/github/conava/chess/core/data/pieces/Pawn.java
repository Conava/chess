package io.github.conava.chess.core.data.pieces;

import io.github.conava.chess.core.data.player.Player;

public class Pawn extends Piece {
    public Pawn(Player player) {
        super(player);
    }

    @Override
    public Pawn copy() {
        return new Pawn(this.player);
    }
}