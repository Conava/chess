package ptp.project.logic.moves;

import ptp.project.data.Square;

public class Move {
    private final Square start;
    private final Square end;
    private final String pieceType;
    private final boolean isCapture;

    public Move(Square start, Square end) {
        this.start = start;
        this.end = end;
        this.pieceType = start.getPiece() != null ? start.getPiece().getClass().getSimpleName() : "Pawn";
        this.isCapture = end.getPiece() != null && !end.getPiece().getPlayer().equals(start.getPiece().getPlayer());


    }

    public Square getStart() {
        return start;
    }

    public Square getEnd() {
        return end;
    }

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
            char promotedTo = Character.toUpperCase(promotionMove.getTargetPiece().getClass().getSimpleName().charAt(0));
            return convertToAlgebraic(this.start) + "-" + convertToAlgebraic(this.end) + "=" + promotedTo;
        }
        // Regular move
        char pieceChar = Character.toUpperCase(this.pieceType.charAt(0));
        if ("Pawn".equals(this.pieceType) && this.isCapture) { // Pawn capture
            sb.append((char) ('a' + this.start.getX())).append("x");
        } else if (!"Pawn".equals(this.pieceType)) { // Non-pawn pieces
            sb.append(pieceChar);
            if (this.isCapture) sb.append("x");
        }
        sb.append(convertToAlgebraic(this.end));
        return sb.toString();
    }

    private String convertToAlgebraic(Square square) {
        return "" + (char) ('a' + square.getX()) + (square.getY() + 1);
    }
}