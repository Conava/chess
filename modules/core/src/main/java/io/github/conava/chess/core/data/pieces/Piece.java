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

    /**
     * Returns a new {@code Piece} of the same concrete type, owning the same {@link Player}.
     * Stateful subclasses (e.g. {@link King}, {@link Rook}) must copy all relevant state.
     *
     * @return a distinct but semantically equivalent piece instance
     */
    public abstract Piece copy();
}
