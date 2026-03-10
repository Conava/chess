package io.github.conava.chess.core.data.pieces;

import io.github.conava.chess.core.data.player.Player;

public class Queen extends Piece {
    public Queen(Player player) {
        super(player);
    }

    @Override
    public Queen copy() {
        return new Queen(this.player);
    }
}