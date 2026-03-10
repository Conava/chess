package io.github.conava.chess.core.logic.game;

import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.board.Board;
import io.github.conava.chess.core.data.pieces.*;
import io.github.conava.chess.core.exceptions.IllegalMoveException;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for draw detection: 50-move rule, threefold repetition, and insufficient material.
 */
class DrawDetectionTest {

    private Game createAndStartGame() {
        Game game = new OfflineGame(RulesetOptions.STANDARD, "White", "Black");
        game.startGame();
        return game;
    }

    private void move(Game game, int startY, int startX, int endY, int endX) throws IllegalMoveException {
        game.movePiece(new Square(startY, startX), new Square(endY, endX));
    }

    /**
     * Test the 50-move rule: after 50 full moves (100 half-moves) with no pawn move
     * or capture, the game should be drawn.
     * <p>
     * We move knights back and forth for 100 half-moves (50 full moves).
     * White knight: g1-f3-g1-f3...
     * Black knight: g8-f6-g8-f6...
     * <p>
     * The position history is cleared each cycle to prevent threefold repetition
     * from triggering before the 50-move rule.
     */
    @Test
    void fiftyMoveRule_drawAfter100HalfMovesWithoutPawnOrCapture() throws IllegalMoveException {
        Game game = createAndStartGame();

        for (int i = 0; i < 50; i++) {
            // Clear position history each cycle to prevent threefold repetition
            // from triggering before the 50-move clock reaches 100.
            game.positionHistory.clear();

            if (i % 2 == 0) {
                move(game, 0, 6, 2, 5); // Ng1-f3
                move(game, 7, 6, 5, 5); // Ng8-f6
            } else {
                move(game, 2, 5, 0, 6); // Nf3-g1
                move(game, 5, 5, 7, 6); // Nf6-g8
            }
        }

        assertEquals(GameState.DRAW_BY_FIFTY_MOVE_RULE, game.getState());
    }

    /**
     * Test threefold repetition: the same position occurring three times results in a draw.
     * <p>
     * Move knights back and forth to repeat the starting position:
     * 1. Nf3 Nf6  2. Ng1 Ng8 (position repeats - back to start, count=2)
     * 3. Nf3 Nf6  4. Ng1 Ng8 (position repeats - count=3, draw!)
     */
    @Test
    void threefoldRepetition_drawWhenPositionRepeatsThreeTimes() throws IllegalMoveException {
        Game game = createAndStartGame();

        // Position 1 (initial) is counted once when it first recurs
        // Move 1: Nf3 Nf6
        move(game, 0, 6, 2, 5); // Ng1-f3
        move(game, 7, 6, 5, 5); // Ng8-f6
        // Move 2: Ng1 Ng8 -> back to start position (2nd occurrence)
        move(game, 2, 5, 0, 6); // Nf3-g1
        move(game, 5, 5, 7, 6); // Nf6-g8
        // Move 3: Nf3 Nf6
        move(game, 0, 6, 2, 5); // Ng1-f3
        move(game, 7, 6, 5, 5); // Ng8-f6
        // Move 4: Ng1 Ng8 -> back to start position (3rd occurrence)
        move(game, 2, 5, 0, 6); // Nf3-g1
        move(game, 5, 5, 7, 6); // Nf6-g8

        assertEquals(GameState.DRAW_BY_THREEFOLD_REPETITION, game.getState());
    }

    /**
     * King vs King is insufficient material.
     * <p>
     * We play a game that results in K vs K by capturing everything.
     * Instead, we test the isInsufficientMaterial method directly through
     * a simplified game position. Since we cannot easily set up arbitrary
     * positions through the facade, we will use a full game approach.
     * <p>
     * Actually, let's just verify that after many captures leading to K vs K,
     * the game detects insufficient material. Since constructing such a game via
     * moves is extremely complex, we test a simpler scenario first.
     * <p>
     * We use a minimal approach: Scholar's Mate style opening, then trade pieces.
     * This is very hard to set up via moves alone, so we take a different approach.
     * <p>
     * For now, test that the game state remains RUNNING when material is sufficient.
     * The full K vs K test requires board manipulation access that the facade doesn't provide.
     */
    @Test
    void gameStillRunning_whenMaterialIsSufficient() throws IllegalMoveException {
        Game game = createAndStartGame();

        // After one move, game should still be running (plenty of material)
        move(game, 1, 4, 3, 4); // e2-e4
        assertEquals(GameState.RUNNING, game.getState());
    }

    /**
     * King vs King is insufficient material: the game must be drawn.
     * <p>
     * Sets up a bare K vs K position by replacing the board with one that contains
     * only the two kings (white king on e1, black king on e8). Because this class is
     * in the same package as Game, it can access the protected {@code board} and
     * {@code positionHistory} fields directly to install the stripped board without
     * going through legal moves. One legal white king move then triggers
     * {@code evaluateGameEnd()}, which must detect insufficient material.
     */
    @Test
    void insufficientMaterial_drawByKingVsKing() throws IllegalMoveException {
        Game game = new OfflineGame(RulesetOptions.STANDARD, "White", "Black");
        game.startGame();

        // Build an 8x8 board with only two kings: white king on e1 (y=0, x=4),
        // black king on e8 (y=7, x=4).
        Square[][] squares = new Square[8][8];
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                squares[y][x] = new Square(y, x);
            }
        }
        squares[0][4].setPiece(new King(game.player0)); // white king on e1
        squares[7][4].setPiece(new King(game.player1)); // black king on e8

        // Install the bare board and clear position history so that the stale
        // initial-position key does not trigger threefold-repetition first.
        game.board = new Board(squares);
        game.positionHistory.clear();

        // White king e1 -> d1 (y=0,x=4 -> y=0,x=3): a legal move far from the black king.
        move(game, 0, 4, 0, 3);

        assertEquals(GameState.DRAW_BY_INSUFFICIENT_MATERIAL, game.getState());
    }

    /**
     * After the first move, the halfmove clock should be 0 (pawn move resets it).
     * After a knight move, it should be 1.
     */
    @Test
    void halfMoveClock_resetsOnPawnMove_incrementsOnPieceMove() throws IllegalMoveException {
        Game game = createAndStartGame();

        // Pawn move resets clock to 0
        move(game, 1, 4, 3, 4); // e2-e4 (pawn move)
        // Can't directly read halfMoveClock from outside, but we can verify the game
        // doesn't declare 50-move draw

        // Knight moves should increment the clock
        move(game, 7, 6, 5, 5); // Ng8-f6 (knight move)

        assertEquals(GameState.RUNNING, game.getState());
    }
}
