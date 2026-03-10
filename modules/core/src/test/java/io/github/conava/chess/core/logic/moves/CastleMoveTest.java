package io.github.conava.chess.core.logic.moves;

import io.github.conava.chess.core.data.Square;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CastleMove}, focusing on the optional Chess960 fields
 * {@code rookOriginFile} and {@code kingDestFile}.
 */
class CastleMoveTest {

    private Square square(int y, int x) {
        return new Square(y, x);
    }

    // ---------------------------------------------------------------
    // Existing constructor (square, square) — backward-compatible
    // ---------------------------------------------------------------

    @Test
    void defaultConstructor_rookOriginFile_isMinusOne() {
        CastleMove move = new CastleMove(square(0, 4), square(0, 6));
        assertEquals(-1, move.getRookOriginFile(),
                "rookOriginFile should default to -1 for standard castle");
    }

    @Test
    void defaultConstructor_kingDestFile_isMinusOne() {
        CastleMove move = new CastleMove(square(0, 4), square(0, 2));
        assertEquals(-1, move.getKingDestFile(),
                "kingDestFile should default to -1 for standard castle");
    }

    @Test
    void defaultConstructor_startAndEnd_arePreserved() {
        Square start = square(0, 4);
        Square end   = square(0, 6);
        CastleMove move = new CastleMove(start, end);
        assertSame(start, move.getStart());
        assertSame(end,   move.getEnd());
    }

    // ---------------------------------------------------------------
    // Chess960 constructor (square, square, int, int)
    // ---------------------------------------------------------------

    @Test
    void chess960Constructor_rookOriginFile_isStored() {
        CastleMove move = new CastleMove(square(0, 4), square(0, 7), 7, 6);
        assertEquals(7, move.getRookOriginFile());
    }

    @Test
    void chess960Constructor_kingDestFile_isStored() {
        CastleMove move = new CastleMove(square(0, 4), square(0, 7), 7, 6);
        assertEquals(6, move.getKingDestFile());
    }

    @Test
    void chess960Constructor_queenside_storedCorrectly() {
        CastleMove move = new CastleMove(square(0, 4), square(0, 0), 0, 2);
        assertEquals(0, move.getRookOriginFile());
        assertEquals(2, move.getKingDestFile());
    }

    @Test
    void chess960Constructor_startAndEnd_arePreserved() {
        Square start = square(0, 4);
        Square end   = square(0, 7);
        CastleMove move = new CastleMove(start, end, 7, 6);
        assertSame(start, move.getStart());
        assertSame(end,   move.getEnd());
    }

    // ---------------------------------------------------------------
    // Sentinel semantics — confirm -1 fields mean "standard path"
    // ---------------------------------------------------------------

    @Test
    void chess960Constructor_withMinusOneFields_behavesLikeDefault() {
        CastleMove standard = new CastleMove(square(0, 4), square(0, 6));
        CastleMove explicit = new CastleMove(square(0, 4), square(0, 6), -1, -1);
        assertEquals(standard.getRookOriginFile(), explicit.getRookOriginFile());
        assertEquals(standard.getKingDestFile(),   explicit.getKingDestFile());
    }
}
