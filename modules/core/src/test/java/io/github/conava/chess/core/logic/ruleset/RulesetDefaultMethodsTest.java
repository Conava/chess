package io.github.conava.chess.core.logic.ruleset;

import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.board.Board;
import io.github.conava.chess.core.data.player.Player;
import io.github.conava.chess.core.data.player.PlayerColor;
import io.github.conava.chess.core.logic.moves.CastleMove;
import io.github.conava.chess.core.logic.moves.Move;
import io.github.conava.chess.core.logic.ruleset.standardChessRuleset.StandardChessRuleset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for default methods added to the {@link Ruleset} interface:
 * {@link Ruleset#getGameLabel()} and {@link Ruleset#deserializeMove(String, Board, Player)}.
 */
class RulesetDefaultMethodsTest {

    private StandardChessRuleset ruleset;
    private Player white;

    @BeforeEach
    void setUp() {
        ruleset = new StandardChessRuleset();
        white = new Player("White", PlayerColor.WHITE);
    }

    // --- getGameLabel tests ---

    @Test
    void getGameLabel_returnsEmptyString() {
        assertEquals("", ruleset.getGameLabel(),
                "Default getGameLabel should return an empty string");
    }

    @Test
    void getGameLabel_returnType_isString() {
        String label = ruleset.getGameLabel();
        assertNotNull(label, "getGameLabel must not return null");
    }

    // --- deserializeMove tests ---

    @Test
    void deserializeMove_regularMove_roundTrips() {
        // "e2-e4" parsed via the default: delegates to Move.fromString
        Move move = ruleset.deserializeMove("e2-e4", null, white);
        assertNotNull(move);
        assertEquals(1, move.getStart().getY());  // rank 2 (y=1)
        assertEquals(4, move.getStart().getX());  // e-file (x=4)
        assertEquals(3, move.getEnd().getY());    // rank 4 (y=3)
        assertEquals(4, move.getEnd().getX());    // e-file (x=4)
    }

    @Test
    void deserializeMove_kingsideCastle_returnsCastleMove() {
        Move move = ruleset.deserializeMove("O-O", null, white);
        assertInstanceOf(CastleMove.class, move,
                "O-O should deserialize to a CastleMove");
    }

    @Test
    void deserializeMove_queensideCastle_returnsCastleMove() {
        Move move = ruleset.deserializeMove("O-O-O", null, white);
        assertInstanceOf(CastleMove.class, move,
                "O-O-O should deserialize to a CastleMove");
    }

    @Test
    void deserializeMove_kingsideCastle_correctSquares_white() {
        Move move = ruleset.deserializeMove("O-O", null, white);
        // White kingside castle: e1 (y=0, x=4) -> g1 (y=0, x=6)
        assertEquals(0, move.getStart().getY());
        assertEquals(4, move.getStart().getX());
        assertEquals(0, move.getEnd().getY());
        assertEquals(6, move.getEnd().getX());
    }

    @Test
    void deserializeMove_queensideCastle_correctSquares_white() {
        Move move = ruleset.deserializeMove("O-O-O", null, white);
        // White queenside castle: e1 (y=0, x=4) -> c1 (y=0, x=2)
        assertEquals(0, move.getStart().getY());
        assertEquals(4, move.getStart().getX());
        assertEquals(0, move.getEnd().getY());
        assertEquals(2, move.getEnd().getX());
    }

    @Test
    void deserializeMove_boardParameterIgnored_byDefault() {
        // Default implementation ignores the Board parameter; passing null should work
        Move move = ruleset.deserializeMove("d7-d5", null, white);
        assertNotNull(move);
        assertEquals(6, move.getStart().getY());  // rank 7 (y=6)
        assertEquals(3, move.getStart().getX());  // d-file (x=3)
    }
}
