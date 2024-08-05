package ptp.core.logic.moves;

import ptp.core.data.player.Player;
import ptp.core.data.Square;
import ptp.core.data.pieces.Pieces;
import ptp.core.data.player.PlayerColor;
import ptp.core.data.pieces.Piece;

import java.lang.reflect.InvocationTargetException;

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

    public static Move fromString(String moveString, Player movePlayer) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
    if (moveString.equals("O-O")) {
        Square start = new Square(4, movePlayer.color() == PlayerColor.WHITE ? 0 : 7); // e1 or e8
        Square end = new Square(6, movePlayer.color() == PlayerColor.WHITE ? 0 : 7);   // g1 or g8
        return new CastleMove(start, end);
    } else if (moveString.equals("O-O-O")) {
        Square start = new Square(4, movePlayer.color() == PlayerColor.WHITE ? 0 : 7); // e1 or e8
        Square end = new Square(2, movePlayer.color() == PlayerColor.WHITE ? 0 : 7);   // c1 or c8
        return new CastleMove(start, end);
    } else if (moveString.contains("=")) {
        String[] parts = moveString.split("=");
        Square start = convertToSquare(parts[0]);
        Square end = convertToSquare(parts[1]);
        Pieces targetPiece = Pieces.valueOf(parts[2]);
        Piece targetPieceInstance = (Piece) Class.forName("ptp.core.data.pieces." + targetPiece.getClassName()).getConstructor(Player.class).newInstance(movePlayer);
        return new PromotionMove(start, end, targetPieceInstance);
    } else {
        Square start = convertToSquare(moveString.substring(0, 2));
        Square end = convertToSquare(moveString.substring(3, 5));
        return new Move(start, end);
    }
}

    private static Square convertToSquare(String substring) {
        return new Square(substring.charAt(0) - 'a', substring.charAt(1) - '1');
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