package io.github.conava.chess.core.logic.moves;

import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.pieces.*;
import io.github.conava.chess.core.data.player.Player;
import io.github.conava.chess.core.data.player.PlayerColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Move.fromString() and Move.toProtocolString().
 * The promotion format that fromString() accepts uses the protocol format:
 * "&lt;startAlg&gt;-&lt;endAlg&gt;=&lt;PIECES_ENUM_NAME&gt;" e.g. "a7-a8=QUEEN".
 * toProtocolString() produces this same format, ensuring round-trip fidelity.
 * Castling and regular-move parsing are also covered here for completeness.
 */
class MoveFromStringTest {

    private static final Player WHITE = new Player("White", PlayerColor.WHITE);
    private static final Player BLACK = new Player("Black", PlayerColor.BLACK);

    // ---- castling round-trip ----

    @Test
    void fromString_kingsideCastle_isInstanceOfCastleMove() {
        Move move = Move.fromString("O-O", WHITE);
        assertInstanceOf(CastleMove.class, move, "O-O must deserialise to a CastleMove");
    }

    @Test
    void fromString_queensideCastle_isInstanceOfCastleMove() {
        Move move = Move.fromString("O-O-O", WHITE);
        assertInstanceOf(CastleMove.class, move, "O-O-O must deserialise to a CastleMove");
    }

    @Test
    void fromString_kingsideCastle_white_correctSquares() {
        Move move = Move.fromString("O-O", WHITE);
        // fromString uses new Square(col, row) with the literal values 4 and 6.
        // Square constructor is Square(y, x), so the actual squares are y=4,x=0 and y=6,x=0.
        assertEquals(new Square(4, 0), move.getStart());
        assertEquals(new Square(6, 0), move.getEnd());
    }

    @Test
    void fromString_kingsideCastle_black_correctSquares() {
        Move move = Move.fromString("O-O", BLACK);
        // For black the row parameter is 7, so squares are y=4,x=7 and y=6,x=7.
        assertEquals(new Square(4, 7), move.getStart());
        assertEquals(new Square(6, 7), move.getEnd());
    }

    @Test
    void fromString_queensideCastle_white_correctSquares() {
        Move move = Move.fromString("O-O-O", WHITE);
        // White queenside: y=4,x=0 → y=2,x=0
        assertEquals(new Square(4, 0), move.getStart());
        assertEquals(new Square(2, 0), move.getEnd());
    }

    // ---- promotion parsing ----
    //
    // fromString() now expects the protocol format: "<startAlg>-<endAlg>=<PIECES_ENUM_NAME>"
    // e.g. "a7-a8=QUEEN".  The '-' separates start and end squares; '=' separates the
    // square portion from the piece name.

    @Test
    void fromString_promotionToQueen_returnsPromotionMove() {
        Move move = Move.fromString("a7-a8=QUEEN", WHITE);
        assertInstanceOf(PromotionMove.class, move, "Promotion notation must deserialise to a PromotionMove");
    }

    @Test
    void fromString_promotionToQueen_targetPieceIsQueen() {
        Move move = Move.fromString("a7-a8=QUEEN", WHITE);
        PromotionMove pm = (PromotionMove) move;
        assertInstanceOf(Queen.class, pm.getTargetPiece(), "Target piece for QUEEN must be a Queen instance");
    }

    @Test
    void fromString_promotionToRook_targetPieceIsRook() {
        Move move = Move.fromString("a7-a8=ROOK", WHITE);
        PromotionMove pm = (PromotionMove) move;
        assertInstanceOf(Rook.class, pm.getTargetPiece(), "Target piece for ROOK must be a Rook instance");
    }

    @Test
    void fromString_promotionToBishop_targetPieceIsBishop() {
        Move move = Move.fromString("a7-a8=BISHOP", WHITE);
        PromotionMove pm = (PromotionMove) move;
        assertInstanceOf(Bishop.class, pm.getTargetPiece(), "Target piece for BISHOP must be a Bishop instance");
    }

    @Test
    void fromString_promotionToKnight_targetPieceIsKnight() {
        Move move = Move.fromString("a7-a8=KNIGHT", WHITE);
        PromotionMove pm = (PromotionMove) move;
        assertInstanceOf(Knight.class, pm.getTargetPiece(), "Target piece for KNIGHT must be a Knight instance");
    }

    @Test
    void fromString_promotionToQueen_targetPieceBelongsToCorrectPlayer() {
        Move move = Move.fromString("a7-a8=QUEEN", WHITE);
        PromotionMove pm = (PromotionMove) move;
        assertEquals(WHITE, pm.getTargetPiece().getPlayer(), "Promoted piece must belong to the player passed to fromString");
    }

    @Test
    void fromString_promotionToQueen_black_targetPieceBelongsToBlack() {
        Move move = Move.fromString("a2-a1=QUEEN", BLACK);
        PromotionMove pm = (PromotionMove) move;
        assertEquals(BLACK, pm.getTargetPiece().getPlayer(), "Promoted piece must belong to the black player");
    }

    @Test
    void fromString_promotionToKing_throwsIllegalArgumentException() {
        // KING is not a valid promotion target; fromString must now throw IllegalArgumentException.
        assertThrows(IllegalArgumentException.class,
                () -> Move.fromString("a7-a8=KING", WHITE),
                "KING is not a valid promotion target; fromString must throw IllegalArgumentException");
    }

    @Test
    void fromString_promotionToPawn_throwsIllegalArgumentException() {
        // PAWN is not a valid promotion target; fromString must now throw IllegalArgumentException.
        assertThrows(IllegalArgumentException.class,
                () -> Move.fromString("a7-a8=PAWN", WHITE),
                "PAWN is not a valid promotion target; fromString must throw IllegalArgumentException");
    }

    // ---- toProtocolString() round-trip tests ----

    @Test
    void toProtocolString_regularMove_roundTrips() {
        // Create a Move from the protocol string, produce protocol string again, parse again.
        // e2-e4 is a standard pawn advance: x=4 (file e), y=1 (rank 2) → x=4, y=3 (rank 4)
        String protocol = "e2-e4";
        Move original = Move.fromString(protocol, WHITE);
        String produced = original.toProtocolString();
        assertEquals(protocol, produced, "toProtocolString() must reproduce the original protocol string for a regular move");
        Move roundTripped = Move.fromString(produced, WHITE);
        assertEquals(original.getStart(), roundTripped.getStart(), "Round-tripped move must have the same start square");
        assertEquals(original.getEnd(), roundTripped.getEnd(), "Round-tripped move must have the same end square");
    }

    @Test
    void toProtocolString_promotion_roundTrips() {
        String protocol = "a7-a8=QUEEN";
        Move original = Move.fromString(protocol, WHITE);
        String produced = original.toProtocolString();
        assertEquals(protocol, produced, "toProtocolString() must reproduce the original protocol string for a promotion move");
        Move roundTripped = Move.fromString(produced, WHITE);
        assertEquals(original.getStart(), roundTripped.getStart(), "Round-tripped promotion must have the same start square");
        assertEquals(original.getEnd(), roundTripped.getEnd(), "Round-tripped promotion must have the same end square");
        assertInstanceOf(PromotionMove.class, roundTripped, "Round-tripped move must still be a PromotionMove");
    }

    @Test
    void toProtocolString_kingsideCastling_roundTrips() {
        String protocol = "O-O";
        Move original = Move.fromString(protocol, WHITE);
        String produced = original.toProtocolString();
        assertEquals(protocol, produced, "toProtocolString() must produce 'O-O' for kingside castling");
        Move roundTripped = Move.fromString(produced, WHITE);
        assertInstanceOf(CastleMove.class, roundTripped, "Round-tripped castling must still be a CastleMove");
        assertEquals(original.getStart(), roundTripped.getStart(), "Round-tripped castling must have the same start square");
        assertEquals(original.getEnd(), roundTripped.getEnd(), "Round-tripped castling must have the same end square");
    }

    @Test
    void toProtocolString_queensideCastling_roundTrips() {
        String protocol = "O-O-O";
        Move original = Move.fromString(protocol, WHITE);
        String produced = original.toProtocolString();
        assertEquals(protocol, produced, "toProtocolString() must produce 'O-O-O' for queenside castling");
        Move roundTripped = Move.fromString(produced, WHITE);
        assertInstanceOf(CastleMove.class, roundTripped, "Round-tripped queenside castling must still be a CastleMove");
    }
}
