package io.github.conava.chess.application.controllers;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GameController#pairMoves(List)}, the {@link GameController.MoveRow} record,
 * and the static sizing math helpers {@link GameController#computeSquareSize} and
 * {@link GameController#computePanelWidth}.
 *
 * <p>These tests verify the pure data-transformation logic that converts a flat list of
 * individual move strings into paired scoresheet rows, and the pure sizing math that
 * drives board and panel layout. None of these methods have JavaFX dependencies, so
 * these tests run without the FX toolkit.
 *
 * <p>Each {@code MoveRow} contains a 1-based move number, the white move, and (optionally)
 * the black move. When the total number of moves is odd the last row's {@code blackMove()}
 * is {@code null}, indicating that black has not yet responded.
 */
class GameControllerMoveListTest {

    // ── pairMoves: edge cases ─────────────────────────────────────────────────

    /**
     * An empty input list must produce an empty output list — no rows at all.
     */
    @Test
    void pairMoves_emptyList_returnsEmptyList() {
        List<GameController.MoveRow> result = GameController.pairMoves(List.of());
        assertNotNull(result, "Result must not be null");
        assertTrue(result.isEmpty(), "Expected empty list for empty input");
    }

    /**
     * A single move (white only, no black response yet) must produce exactly one row
     * with the white move set and {@code blackMove()} as {@code null}.
     */
    @Test
    void pairMoves_singleMove_returnsOneRowWithNullBlack() {
        List<GameController.MoveRow> result = GameController.pairMoves(List.of("e4"));

        assertEquals(1, result.size(), "Expected exactly one row");
        GameController.MoveRow row = result.get(0);
        assertEquals(1, row.moveNumber(), "Move number must be 1");
        assertEquals("e4", row.whiteMove(), "White move must be 'e4'");
        assertNull(row.blackMove(), "Black move must be null when white has no response yet");
    }

    /**
     * Two moves (white and black) must produce exactly one complete row.
     */
    @Test
    void pairMoves_twoMoves_returnsOneCompleteRow() {
        List<GameController.MoveRow> result = GameController.pairMoves(List.of("e4", "e5"));

        assertEquals(1, result.size(), "Expected exactly one row");
        GameController.MoveRow row = result.get(0);
        assertEquals(1, row.moveNumber(), "Move number must be 1");
        assertEquals("e4", row.whiteMove(), "White move must be 'e4'");
        assertEquals("e5", row.blackMove(), "Black move must be 'e5'");
    }

    /**
     * Three moves must produce two rows: row 1 with both moves, row 2 with white only.
     * This simulates white having just made their second move before black responds.
     */
    @Test
    void pairMoves_threeMoves_returnsTwoRows() {
        List<GameController.MoveRow> result = GameController.pairMoves(List.of("e4", "e5", "Nf3"));

        assertEquals(2, result.size(), "Expected two rows");

        GameController.MoveRow row1 = result.get(0);
        assertEquals(1, row1.moveNumber(), "First row must have move number 1");
        assertEquals("e4", row1.whiteMove(), "First row white move must be 'e4'");
        assertEquals("e5", row1.blackMove(), "First row black move must be 'e5'");

        GameController.MoveRow row2 = result.get(1);
        assertEquals(2, row2.moveNumber(), "Second row must have move number 2");
        assertEquals("Nf3", row2.whiteMove(), "Second row white move must be 'Nf3'");
        assertNull(row2.blackMove(), "Second row black move must be null");
    }

    /**
     * Four moves must produce two complete rows with all fields populated.
     */
    @Test
    void pairMoves_fourMoves_returnsTwoCompleteRows() {
        List<GameController.MoveRow> result =
                GameController.pairMoves(List.of("e4", "e5", "Nf3", "Nc6"));

        assertEquals(2, result.size(), "Expected two rows");

        GameController.MoveRow row1 = result.get(0);
        assertEquals(1, row1.moveNumber(), "First row must have move number 1");
        assertEquals("e4", row1.whiteMove());
        assertEquals("e5", row1.blackMove());

        GameController.MoveRow row2 = result.get(1);
        assertEquals(2, row2.moveNumber(), "Second row must have move number 2");
        assertEquals("Nf3", row2.whiteMove());
        assertEquals("Nc6", row2.blackMove());
    }

    /**
     * Twenty moves (a typical opening sequence) must produce 10 correctly numbered rows,
     * all with both white and black moves populated.
     */
    @Test
    void pairMoves_largeMoveList_correctlyNumbered() {
        // Build 20 synthetic move strings: "w1", "b1", "w2", "b2", ..., "w10", "b10"
        List<String> moves = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            moves.add("w" + i);
            moves.add("b" + i);
        }

        List<GameController.MoveRow> result = GameController.pairMoves(moves);

        assertEquals(10, result.size(), "Expected 10 rows for 20 moves");
        for (int i = 0; i < 10; i++) {
            GameController.MoveRow row = result.get(i);
            int expectedNumber = i + 1;
            assertEquals(expectedNumber, row.moveNumber(),
                    "Row " + i + " must have move number " + expectedNumber);
            assertEquals("w" + expectedNumber, row.whiteMove(),
                    "Row " + i + " white move must be 'w" + expectedNumber + "'");
            assertEquals("b" + expectedNumber, row.blackMove(),
                    "Row " + i + " black move must be 'b" + expectedNumber + "'");
        }
    }

    /**
     * A {@code null} input must return an empty list — defensive handling so callers
     * do not need to null-check before calling.
     */
    @Test
    void pairMoves_nullInput_returnsEmptyList() {
        List<GameController.MoveRow> result = GameController.pairMoves(null);
        assertNotNull(result, "Result must not be null even for null input");
        assertTrue(result.isEmpty(), "Expected empty list for null input");
    }

    // ── MoveRow record accessors ──────────────────────────────────────────────

    /**
     * Verifies that the {@code MoveRow} record's generated accessor methods return
     * exactly the values passed to the canonical constructor.
     */
    @Test
    void MoveRow_recordAccessors_returnCorrectValues() {
        GameController.MoveRow row = new GameController.MoveRow(7, "Bb5", "a6");

        assertEquals(7, row.moveNumber(), "moveNumber() must return 7");
        assertEquals("Bb5", row.whiteMove(), "whiteMove() must return 'Bb5'");
        assertEquals("a6", row.blackMove(), "blackMove() must return 'a6'");
    }

    // ── Sizing math helpers ───────────────────────────────────────────────────

    /**
     * Board square size is derived from scene height: {@code (height - padding) / 8}.
     *
     * <p>Given a scene height of 900px and 40px of vertical padding (20px top + 20px bottom),
     * each square should be {@code (900 - 40) / 8 = 107.5} pixels.
     */
    @Test
    void computeSquareSize_returnsHeightBasedValue() {
        double result = GameController.computeSquareSize(900.0, 40.0);
        assertEquals(107.5, result, 1e-9,
                "computeSquareSize(900, 40) must return (900-40)/8 = 107.5");
    }

    /**
     * Panel width is half the remaining horizontal space after subtracting board width.
     *
     * <p>Given a scene width of 1920px and a board width of 860px,
     * each panel should be {@code (1920 - 860) / 2 = 530} pixels.
     */
    @Test
    void computePanelWidth_returnsHalfRemainingWidth() {
        double result = GameController.computePanelWidth(1920.0, 860.0, 160.0);
        assertEquals(530.0, result, 1e-9,
                "computePanelWidth(1920, 860, 160) must return (1920-860)/2 = 530");
    }

    /**
     * Panel width must never go below the configured minimum.
     *
     * <p>Given a scene width of 400px and a board width of 360px, the raw calculation
     * would be {@code (400 - 360) / 2 = 20}, which is below the 160px minimum —
     * so the result must be clamped to 160.
     */
    @Test
    void computePanelWidth_respectsMinimum() {
        double result = GameController.computePanelWidth(400.0, 360.0, 160.0);
        assertEquals(160.0, result, 1e-9,
                "computePanelWidth(400, 360, 160) must return 160 (minimum), not 20");
    }

    /**
     * Board square size must never go below 40px regardless of window height.
     *
     * <p>Given a scene height of 200px and 40px of vertical padding, the raw calculation
     * would be {@code (200 - 40) / 8 = 20}, which is below the 40px minimum —
     * so the result must be clamped to 40.
     */
    @Test
    void computeSquareSize_respectsMinimum() {
        double result = GameController.computeSquareSize(200.0, 40.0);
        assertEquals(40.0, result, 1e-9,
                "computeSquareSize(200, 40) must return 40 (minimum), not 20");
    }
}
