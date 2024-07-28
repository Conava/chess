package ptp.core.logic.moves;

import ptp.core.data.Square;
import ptp.core.data.enums.Pieces;

public class Move {
    private final Square start;
    private final Square end;
    private final Pieces pieceType;
    private final boolean isCapture;

    /**
     * Constructor for the Move class.
     *
     * @param start The starting square of the move.
     * @param end The ending square of the move.
     */
    public Move(Square start, Square end) {
        this.start = start;
        this.end = end;
        this.pieceType = start.getPiece() != null ? start.getPiece().getType() : Pieces.PAWN;
        this.isCapture = end.getPiece() != null && !end.getPiece().getPlayer().equals(start.getPiece().getPlayer());
    }

    /**
     * Gets the starting square of the move.
     *
     * @return The starting square.
     */
    public Square getStart() {
        return start;
    }

    /**
     * Gets the ending square of the move.
     *
     * @return The ending square.
     */
    public Square getEnd() {
        return end;
    }

    /**
     * Converts the move to a string representation.
     *
     * @return The string representation of the move.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // Handle castling
        if (this instanceof CastleMove) {
            int moveDistance = this.end.getX() - this.start.getX();
            return moveDistance > 0 ? "O-O" : "O-O-O";
        }
        // Handle promotion
        if (this instanceof PromotionMove promotionMove) {
            char promotedTo = Character.toUpperCase(promotionMove.getTargetPiece().getType().name().charAt(0));
            return convertToAlgebraic(this.start) + "-" + convertToAlgebraic(this.end) + "=" + promotedTo;
        }
        // Regular move
        char pieceChar = Character.toUpperCase(this.pieceType.name().charAt(0));
        if (this.pieceType == Pieces.PAWN && this.isCapture) { // Pawn capture
            sb.append((char) ('a' + this.start.getX())).append("x");
        } else if (this.pieceType != Pieces.PAWN) { // Non-pawn pieces
            sb.append(pieceChar);
            if (this.isCapture) sb.append("x");
        }
        sb.append(convertToAlgebraic(this.end));
        return sb.toString();
    }

    /**
     * Converts a square to its algebraic notation.
     *
     * @param square The square to convert.
     * @return The algebraic notation of the square.
     */
    private String convertToAlgebraic(Square square) {
        return "" + (char) ('a' + square.getX()) + (square.getY() + 1);
    }
}