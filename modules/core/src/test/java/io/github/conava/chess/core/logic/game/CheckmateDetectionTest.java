package io.github.conava.chess.core.logic.game;

import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.exceptions.IllegalMoveException;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for checkmate and stalemate detection through {@link Game#evaluateGameEnd()}.
 * All tests use {@link OfflineGame} via the standard constructor.
 */
class CheckmateDetectionTest {

    private Game createAndStartGame() {
        Game game = new OfflineGame(RulesetOptions.STANDARD, "White", "Black");
        game.startGame();
        return game;
    }

    private void move(Game game, int startY, int startX, int endY, int endX) throws IllegalMoveException {
        game.movePiece(new Square(startY, startX), new Square(endY, endX));
    }

    /**
     * Fool's Mate: the fastest possible checkmate (4 half-moves).
     * 1. f3 e5  2. g4 Qh4#
     *
     * Coordinate mapping (y=0 is white's back rank, x=0 is a-file):
     * f2-f3 = (1,5)-(2,5)
     * e7-e5 = (6,4)-(4,4)
     * g2-g4 = (1,6)-(3,6)
     * Qd8-h4 = (7,3)-(3,7)
     */
    @Test
    void foolsMate_blackWinsByCheckmate() throws IllegalMoveException {
        Game game = createAndStartGame();

        move(game, 1, 5, 2, 5); // f2-f3
        move(game, 6, 4, 4, 4); // e7-e5
        move(game, 1, 6, 3, 6); // g2-g4
        move(game, 7, 3, 3, 7); // Qd8-h4#

        assertEquals(GameState.BLACK_WON_BY_CHECKMATE, game.getState());
    }

    /**
     * Scholar's Mate (4 moves for white):
     * 1. e4 e5  2. Bc4 Nc6  3. Qh5 Nf6  4. Qxf7#
     *
     * e2-e4 = (1,4)-(3,4)
     * e7-e5 = (6,4)-(4,4)
     * Bf1-c4 = (0,5)-(3,2)
     * Nb8-c6 = (7,1)-(5,2)
     * Qd1-h5 = (0,3)-(4,7)
     * Ng8-f6 = (7,6)-(5,5)
     * Qh5xf7 = (4,7)-(6,5)
     */
    @Test
    void scholarsMate_whiteWinsByCheckmate() throws IllegalMoveException {
        Game game = createAndStartGame();

        move(game, 1, 4, 3, 4); // e2-e4
        move(game, 6, 4, 4, 4); // e7-e5
        move(game, 0, 5, 3, 2); // Bf1-c4
        move(game, 7, 1, 5, 2); // Nb8-c6
        move(game, 0, 3, 4, 7); // Qd1-h5
        move(game, 7, 6, 5, 5); // Ng8-f6
        move(game, 4, 7, 6, 5); // Qh5xf7#

        assertEquals(GameState.WHITE_WON_BY_CHECKMATE, game.getState());
    }

    /**
     * Stalemate position: a minimal position where the side to move has no legal move
     * but is not in check.
     *
     * This is the Sam Loyd 10-move stalemate:
     * 1. e3 a5  2. Qh5 Ra6  3. Qxa5 h5  4. Qxc7 Rah6  5. h4 f6
     * 6. Qxd7+ Kf7  7. Qxb7 Qd3  8. Qxb8 Qh7  9. Qxc8 Kg6
     * 10. Qe6 (stalemate)
     *
     * Coordinate convention: y=0 is rank 1 (white back rank), x=0 is a-file.
     */
    @Test
    void stalemate_drawByStalemate() throws IllegalMoveException {
        Game game = createAndStartGame();

        // 1. e3 a5
        move(game, 1, 4, 2, 4); // e2-e3
        move(game, 6, 0, 4, 0); // a7-a5
        // 2. Qh5 Ra6
        move(game, 0, 3, 4, 7); // Qd1-h5
        move(game, 7, 0, 5, 0); // Ra8-a6
        // 3. Qxa5 h5
        move(game, 4, 7, 4, 0); // Qh5xa5
        move(game, 6, 7, 4, 7); // h7-h5
        // 4. Qxc7 Rah6
        move(game, 4, 0, 6, 2); // Qa5xc7
        move(game, 5, 0, 5, 7); // Ra6-h6
        // 5. h4 f6
        move(game, 1, 7, 3, 7); // h2-h4
        move(game, 6, 5, 5, 5); // f7-f6
        // 6. Qxd7+ Kf7
        move(game, 6, 2, 6, 3); // Qc7xd7+
        move(game, 7, 4, 6, 5); // Ke8-f7
        // 7. Qxb7 Qd3
        move(game, 6, 3, 6, 1); // Qd7xb7
        move(game, 7, 3, 2, 3); // Qd8-d3
        // 8. Qxb8 Qh7
        move(game, 6, 1, 7, 1); // Qb7xb8
        move(game, 2, 3, 6, 7); // Qd3-h7
        // 9. Qxc8 Kg6
        move(game, 7, 1, 7, 2); // Qb8xc8
        move(game, 6, 5, 5, 6); // Kf7-g6
        // 10. Qe6 (stalemate)
        move(game, 7, 2, 5, 4); // Qc8-e6

        assertEquals(GameState.DRAW_BY_STALEMATE, game.getState());
    }
}
