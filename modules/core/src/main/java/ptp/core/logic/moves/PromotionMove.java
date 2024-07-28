package ptp.core.logic.moves;

import ptp.core.data.Square;
import ptp.core.data.pieces.Piece;

/**
 * Represents a promotion move in a chess game.
 * A promotion move occurs when a pawn reaches the opposite end of the board and is promoted to another piece.
 */
public class PromotionMove extends Move {
    /**
     * The piece to which the pawn is promoted.
     */
    public final Piece targetPiece;

    /**
     * Constructs a PromotionMove.
     *
     * @param start The starting square of the move.
     * @param end The ending square of the move.
     * @param targetPiece The piece to which the pawn is promoted.
     */
    public PromotionMove(Square start, Square end, Piece targetPiece) {
        super(start, end);
        this.targetPiece = targetPiece;
    }

    /**
     * Gets the piece to which the pawn is promoted.
     *
     * @return The promoted piece.
     */
    public Piece getTargetPiece() {
        return targetPiece;
    }
}