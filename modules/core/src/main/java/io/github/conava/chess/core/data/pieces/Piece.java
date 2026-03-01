package io.github.conava.chess.core.data.pieces;

import io.github.conava.chess.core.data.player.Player;

public abstract class Piece {
    protected final Player player;

    public Piece(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public Pieces getType() {
        return Pieces.valueOf(this.getClass().getSimpleName().toUpperCase());
    }
}
