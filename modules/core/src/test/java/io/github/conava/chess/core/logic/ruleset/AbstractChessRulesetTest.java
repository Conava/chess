package io.github.conava.chess.core.logic.ruleset;

import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.pieces.King;
import io.github.conava.chess.core.data.pieces.Rook;
import io.github.conava.chess.core.data.player.Player;
import io.github.conava.chess.core.data.player.PlayerColor;
import io.github.conava.chess.core.logic.ruleset.standardChessRuleset.StandardChessRuleset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for shared behaviour provided by {@link AbstractChessRuleset}.
 *
 * <p>Uses {@link StandardChessRuleset} as a concrete subclass.  Because
 * {@link AbstractChessRuleset#isCastlingCandidate} is {@code protected} this test
 * class uses a thin anonymous subclass defined in the same package so the
 * compiler permits access to the protected member.
 */
class AbstractChessRulesetTest {

    /**
     * Minimal concrete subclass that exposes the protected
     * {@code isCastlingCandidate} hook for direct testing.
     */
    private static class TestableRuleset extends StandardChessRuleset {
        boolean isCastlingCandidatePublic(Square source, Square target) {
            return isCastlingCandidate(source, target);
        }
    }

    private TestableRuleset ruleset;
    private Player white;

    @BeforeEach
    void setUp() {
        ruleset = new TestableRuleset();
        white = new Player("White", PlayerColor.WHITE);
    }

    // -----------------------------------------------------------------------
    // isCastlingCandidate — Fix 1 hook
    // -----------------------------------------------------------------------

    /**
     * A king moving exactly 2 squares horizontally (to the right) is a castling candidate.
     */
    @Test
    void isCastlingCandidate_kingMovesTwoSquaresRight_returnsTrue() {
        Square source = new Square(0, 4);
        source.setPiece(new King(white));
        Square target = new Square(0, 6); // 2 squares right

        assertTrue(ruleset.isCastlingCandidatePublic(source, target), "King moving 2 squares right should be a castling candidate");
    }

    /**
     * A king moving exactly 2 squares horizontally (to the left) is a castling candidate.
     */
    @Test
    void isCastlingCandidate_kingMovesTwoSquaresLeft_returnsTrue() {
        Square source = new Square(0, 4);
        source.setPiece(new King(white));
        Square target = new Square(0, 2); // 2 squares left

        assertTrue(ruleset.isCastlingCandidatePublic(source, target), "King moving 2 squares left should be a castling candidate");
    }

    /**
     * A king moving only 1 square is NOT a castling candidate.
     */
    @Test
    void isCastlingCandidate_kingMovesOneSquare_returnsFalse() {
        Square source = new Square(0, 4);
        source.setPiece(new King(white));
        Square target = new Square(0, 5); // only 1 square

        assertFalse(ruleset.isCastlingCandidatePublic(source, target), "King moving 1 square should not be a castling candidate");
    }

    /**
     * A non-King piece (Rook) moving 2 squares is NOT a castling candidate.
     */
    @Test
    void isCastlingCandidate_rookMovesTwoSquares_returnsFalse() {
        Square source = new Square(0, 0);
        source.setPiece(new Rook(white));
        Square target = new Square(0, 2); // 2 squares, but not a king

        assertFalse(ruleset.isCastlingCandidatePublic(source, target), "A Rook moving 2 squares should not be a castling candidate");
    }

    // -----------------------------------------------------------------------
    // getGameLabel — inherited default from Ruleset interface
    // -----------------------------------------------------------------------

    /**
     * {@code AbstractChessRuleset} inherits the default {@code getGameLabel()} from
     * {@link Ruleset}, which returns an empty string.
     */
    @Test
    void getGameLabel_inheritedDefault_returnsEmptyString() {
        assertEquals("", ruleset.getGameLabel(), "AbstractChessRuleset should inherit getGameLabel() returning \"\"");
    }
}
