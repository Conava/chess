package ptp.core.logic.moves;

import ptp.core.data.Square;

/**
 * Represents a castling move in a chess game.
 * Castling is a special move involving the king and a rook.
 */
public class CastleMove extends Move {
    /**
     * Constructs a CastleMove.
     *
     * @param start The starting square of the move.
     * @param end The ending square of the move.
     */
    public CastleMove(Square start, Square end) {
        super(start, end);
    }
}