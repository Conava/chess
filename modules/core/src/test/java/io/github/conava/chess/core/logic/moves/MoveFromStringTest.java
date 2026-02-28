package io.github.conava.chess.core.logic.moves;

import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.pieces.*;
import io.github.conava.chess.core.data.player.Player;
import io.github.conava.chess.core.data.player.PlayerColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Move.fromString() after the Task 2 fix:
 * stale Class.forName() reflection replaced with an enum switch.
 *
 * The promotion format that fromString() accepts uses '=' as the field separator
 * and the full Pieces enum name as the piece token, e.g. "a7=a8=QUEEN".
 * The key regression being tested is that the code now uses a switch expression
 * on the Pieces enum rather than Class.forName(), so it never throws
 * ClassNotFoundException regardless of which promotable piece is requested.
 *
 * Castling and regular-move parsing are also covered here for completeness.
 */
class MoveFromStringTest {

    private static final Player WHITE = new Player("White", PlayerColor.WHITE);
    private static final Player BLACK = new Player("Black", PlayerColor.BLACK);

    // ---- castling round-trip ----

    @Test
    void fromString_kingsideCastle_isInstanceOfCastleMove() {
        Move move = Move.fromString("O-O", WHITE);
        assertInstanceOf(CastleMove.class, move,
                "O-O must deserialise to a CastleMove");
    }

    @Test
    void fromString_queensideCastle_isInstanceOfCastleMove() {
        Move move = Move.fromString("O-O-O", WHITE);
        assertInstanceOf(CastleMove.class, move,
                "O-O-O must deserialise to a CastleMove");
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

    // ---- promotion parsing — no ClassNotFoundException after Task 2 ----
    //
    // fromString() expects the format: "<startAlg>=<endAlg>=<PIECES_ENUM_NAME>"
    // e.g. "a7=a8=QUEEN".  The '=' character acts as the field separator.
    // convertToSquare() uses only chars 0 and 1 of each field, so "a7" → Square(0,6)
    // and "a8" → Square(0,7).

    @Test
    void fromString_promotionToQueen_returnsPromotionMove() {
        Move move = Move.fromString("a7=a8=QUEEN", WHITE);
        assertInstanceOf(PromotionMove.class, move,
                "Promotion notation must deserialise to a PromotionMove");
    }

    @Test
    void fromString_promotionToQueen_targetPieceIsQueen() {
        Move move = Move.fromString("a7=a8=QUEEN", WHITE);
        PromotionMove pm = (PromotionMove) move;
        assertInstanceOf(Queen.class, pm.getTargetPiece(),
                "Target piece for QUEEN must be a Queen instance");
    }

    @Test
    void fromString_promotionToRook_targetPieceIsRook() {
        Move move = Move.fromString("a7=a8=ROOK", WHITE);
        PromotionMove pm = (PromotionMove) move;
        assertInstanceOf(Rook.class, pm.getTargetPiece(),
                "Target piece for ROOK must be a Rook instance");
    }

    @Test
    void fromString_promotionToBishop_targetPieceIsBishop() {
        Move move = Move.fromString("a7=a8=BISHOP", WHITE);
        PromotionMove pm = (PromotionMove) move;
        assertInstanceOf(Bishop.class, pm.getTargetPiece(),
                "Target piece for BISHOP must be a Bishop instance");
    }

    @Test
    void fromString_promotionToKnight_targetPieceIsKnight() {
        Move move = Move.fromString("a7=a8=KNIGHT", WHITE);
        PromotionMove pm = (PromotionMove) move;
        assertInstanceOf(Knight.class, pm.getTargetPiece(),
                "Target piece for KNIGHT must be a Knight instance");
    }

    @Test
    void fromString_promotionToQueen_targetPieceBelongsToCorrectPlayer() {
        Move move = Move.fromString("a7=a8=QUEEN", WHITE);
        PromotionMove pm = (PromotionMove) move;
        assertEquals(WHITE, pm.getTargetPiece().getPlayer(),
                "Promoted piece must belong to the player passed to fromString");
    }

    @Test
    void fromString_promotionToQueen_black_targetPieceBelongsToBlack() {
        Move move = Move.fromString("a2=a1=QUEEN", BLACK);
        PromotionMove pm = (PromotionMove) move;
        assertEquals(BLACK, pm.getTargetPiece().getPlayer(),
                "Promoted piece must belong to the black player");
    }

    @Test
    void fromString_promotionToKing_returnsNullTargetPiece() {
        // KING and PAWN are non-promotable values; the switch returns null for them.
        Move move = Move.fromString("a7=a8=KING", WHITE);
        PromotionMove pm = (PromotionMove) move;
        assertNull(pm.getTargetPiece(),
                "KING is not a valid promotion target; getNewPiece must return null");
    }

    @Test
    void fromString_promotionToPawn_returnsNullTargetPiece() {
        Move move = Move.fromString("a7=a8=PAWN", WHITE);
        PromotionMove pm = (PromotionMove) move;
        assertNull(pm.getTargetPiece(),
                "PAWN is not a valid promotion target; getNewPiece must return null");
    }
}
