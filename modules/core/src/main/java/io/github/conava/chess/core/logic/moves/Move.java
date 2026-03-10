package io.github.conava.chess.core.logic.moves;

import io.github.conava.chess.core.data.player.Player;
import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.pieces.Bishop;
import io.github.conava.chess.core.data.pieces.Knight;
import io.github.conava.chess.core.data.pieces.Piece;
import io.github.conava.chess.core.data.pieces.Pieces;
import io.github.conava.chess.core.data.pieces.Queen;
import io.github.conava.chess.core.data.pieces.Rook;
import io.github.conava.chess.core.data.player.PlayerColor;

public class Move {
    private final Square start;
    private final Square end;
    private final Pieces pieceType;
    private final boolean isCapture;

    /**
     * Constructor for the Move class.
     *
     * @param start The starting square of the move.
     * @param end   The ending square of the move.
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
     * Gets the type of piece that made this move, as recorded at construction time.
     * <p>
     * This value is captured when the {@code Move} is constructed, before the piece is
     * removed from the start square by {@code Board.executeMove}. It is safe to use even
     * after the move has been committed (when the start square's piece reference is null).
     *
     * @return the {@link Pieces} enum constant identifying the moving piece type
     */
    public Pieces getPieceType() {
        return pieceType;
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
     * Produces an unambiguous wire-safe representation of this move for network transmission.
     * <p>
     * Format:
     * <ul>
     *   <li>Castling kingside: {@code "O-O"}</li>
     *   <li>Castling queenside: {@code "O-O-O"}</li>
     *   <li>Regular move: {@code "<startFile><startRank>-<endFile><endRank>"} e.g. {@code "e2-e4"}</li>
     *   <li>Promotion: {@code "<start>-<end>=<PIECES_ENUM_NAME>"} e.g. {@code "a7-a8=QUEEN"}</li>
     * </ul>
     * This format round-trips through {@link #fromString(String, Player)}.
     * Do NOT use {@link #toString()} for wire transmission — it produces algebraic notation for UI display.
     * <p>
     * Coordinate convention: {@code getX()} = column/file (a=0 … h=7),
     * {@code getY()} = row/rank (rank 1 = 0 … rank 8 = 7).
     *
     * @return the wire-safe protocol string
     */
    public String toProtocolString() {
        if (this instanceof CastleMove) {
            // Kingside: end file (getX()) is greater than start file; queenside: less.
            return this.end.getX() > this.start.getX() ? "O-O" : "O-O-O";
        }
        String startProto = squareToProtocol(this.start);
        String endProto = squareToProtocol(this.end);
        if (this instanceof PromotionMove promotionMove) {
            return startProto + "-" + endProto + "=" + promotionMove.getTargetPiece().getType().name();
        }
        return startProto + "-" + endProto;
    }

    /**
     * Converts a square to a two-character protocol string that round-trips through
     * {@link #convertToSquare(String)}.
     * <p>
     * Coordinate convention: {@code square.getX()} = column/file (a=0 … h=7),
     * {@code square.getY()} = row/rank (rank 1 = 0 … rank 8 = 7).
     * The first character of the result is the file letter ({@code 'a' + square.getX()})
     * and the second character is the rank digit ({@code '1' + square.getY()}).
     * For example, {@code new Square(1, 4)} (rank 2, e-file) encodes as {@code "e2"}.
     *
     * @param square the square to encode
     * @return a two-character string such as {@code "e2"} representing the square
     */
    private static String squareToProtocol(Square square) {
        return "" + (char) ('a' + square.getX()) + (char) ('1' + square.getY());
    }

    /**
     * Reconstructs a {@link Move} from its protocol string representation as produced by
     * {@link #toProtocolString()}.
     *
     * <p>Recognised formats:
     * <ul>
     *   <li>{@code "O-O"} — kingside castling</li>
     *   <li>{@code "O-O-O"} — queenside castling</li>
     *   <li>{@code "<startFile><startRank>-<endFile><endRank>"} — regular move, e.g. {@code "e2-e4"}</li>
     *   <li>{@code "<start>-<end>=<PIECES_ENUM_NAME>"} — promotion, e.g. {@code "a7-a8=QUEEN"}</li>
     * </ul>
     * The promotion piece name must match a {@link Pieces} enum constant exactly. Passing
     * {@code "KING"} or {@code "PAWN"} throws {@link IllegalArgumentException} because neither
     * is a valid promotion target.
     *
     * @param moveString the protocol string to parse, as returned by {@link #toProtocolString()}
     * @param movePlayer the {@link Player} who is making the move (used to construct piece instances)
     * @return the reconstructed {@link Move} (may be a {@link CastleMove} or {@link PromotionMove}
     * subtype)
     * @throws IllegalArgumentException        if the promotion piece name is {@code KING} or {@code PAWN},
     *                                         or if {@code pieceName} is not a valid {@link Pieces} enum name
     * @throws NullPointerException            if {@code moveString} is {@code null}
     * @throws StringIndexOutOfBoundsException if {@code moveString} is too short to parse
     */
    public static Move fromString(String moveString, Player movePlayer) {
        if (moveString.equals("O-O")) {
            int rank = movePlayer.color() == PlayerColor.WHITE ? 0 : 7;
            Square start = new Square(rank, 4); // e1 (white) or e8 (black): row=rank, col=4
            Square end = new Square(rank, 6); // g1 (white) or g8 (black): row=rank, col=6
            return new CastleMove(start, end);
        } else if (moveString.equals("O-O-O")) {
            int rank = movePlayer.color() == PlayerColor.WHITE ? 0 : 7;
            Square start = new Square(rank, 4); // e1 (white) or e8 (black): row=rank, col=4
            Square end = new Square(rank, 2); // c1 (white) or c8 (black): row=rank, col=2
            return new CastleMove(start, end);
        } else if (moveString.contains("=")) {
            // Protocol format: "<start>-<end>=<PIECES_ENUM_NAME>" e.g. "a7-a8=QUEEN"
            String[] equalParts = moveString.split("=", 2);
            String squarePart = equalParts[0]; // "a7-a8"
            String pieceName = equalParts[1];  // "QUEEN"
            Square start = convertToSquare(squarePart.substring(0, 2));
            Square end = convertToSquare(squarePart.substring(3, 5));
            Pieces targetPiece = Pieces.valueOf(pieceName);
            Piece targetPieceInstance = switch (targetPiece) {
                case QUEEN -> new Queen(movePlayer);
                case ROOK -> new Rook(movePlayer);
                case BISHOP -> new Bishop(movePlayer);
                case KNIGHT -> new Knight(movePlayer);
                default ->
                        throw new IllegalArgumentException("Cannot promote to " + targetPiece + "; only QUEEN, ROOK, BISHOP, KNIGHT are valid");
            };
            return new PromotionMove(start, end, targetPieceInstance);
        } else {
            Square start = convertToSquare(moveString.substring(0, 2));
            Square end = convertToSquare(moveString.substring(3, 5));
            return new Move(start, end);
        }
    }

    /**
     * Parses a two-character protocol square string (e.g. {@code "e2"}) into a {@link Square}.
     * <p>
     * Coordinate convention: the first character is the file letter (a=0 … h=7), stored as
     * {@code x} (column/file); the second character is the rank digit (1=0 … 8=7), stored as
     * {@code y} (row/rank). For example, {@code "e2"} produces {@code new Square(1, 4)}
     * (y=1 = rank 2, x=4 = e-file).
     *
     * @param substring a two-character string such as {@code "e2"}
     * @return the corresponding {@link Square}
     */
    private static Square convertToSquare(String substring) {
        return new Square(substring.charAt(1) - '1', substring.charAt(0) - 'a');
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