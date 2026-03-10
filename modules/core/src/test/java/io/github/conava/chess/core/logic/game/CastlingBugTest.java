package io.github.conava.chess.core.logic.game;

import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.pieces.*;
import io.github.conava.chess.core.exceptions.IllegalMoveException;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

/**
 * Regression tests for castling through {@link Game#movePiece}.
 * Verifies that {@code movePiece} creates a {@link io.github.conava.chess.core.logic.moves.CastleMove}
 * so that {@link io.github.conava.chess.core.data.board.Board#executeMove} relocates the rook.
 */
class CastlingBugTest {
    private void move(Game game, int sy, int sx, int ey, int ex) throws IllegalMoveException {
        game.movePiece(new Square(sy, sx), new Square(ey, ex));
    }

    @Test
    void kingsideCastlingMovesRook() throws IllegalMoveException {
        Game game = new OfflineGame(RulesetOptions.STANDARD, "W", "B");
        game.startGame();

        // Clear the way for kingside castling: 1. e4 e5 2. Nf3 Nc6 3. Bc4 Bc5
        move(game, 1, 4, 3, 4); // e2-e4
        move(game, 6, 4, 4, 4); // e7-e5
        move(game, 0, 6, 2, 5); // Ng1-f3
        move(game, 7, 1, 5, 2); // Nb8-c6
        move(game, 0, 5, 3, 2); // Bf1-c4
        move(game, 7, 5, 4, 2); // Bf8-c5

        // Verify castling is in legal squares
        List<Square> kingLegal = game.getLegalSquares(new Square(0, 4));
        assertTrue(kingLegal.stream().anyMatch(s -> s.getY() == 0 && s.getX() == 6),
                "King should be able to castle kingside");

        // Castle: Ke1-g1
        move(game, 0, 4, 0, 6);

        // Verify king moved to g1
        assertInstanceOf(King.class, game.getPieceAt(new Square(0, 6)),
                "King should be on g1");
        // Verify rook moved from h1 to f1
        assertInstanceOf(Rook.class, game.getPieceAt(new Square(0, 5)),
                "Rook should be on f1 after kingside castling");
        assertNull(game.getPieceAt(new Square(0, 7)),
                "h1 should be empty after castling");
    }

    @Test
    void queensideCastlingMovesRook() throws IllegalMoveException {
        Game game = new OfflineGame(RulesetOptions.STANDARD, "W", "B");
        game.startGame();

        // Clear the way for queenside castling: 1. d4 d5 2. Nc3 Nc6 3. Bf4 Bf5 4. Qd3 Qd6
        move(game, 1, 3, 3, 3); // d2-d4
        move(game, 6, 3, 4, 3); // d7-d5
        move(game, 0, 1, 2, 2); // Nb1-c3
        move(game, 7, 1, 5, 2); // Nb8-c6
        move(game, 0, 2, 3, 5); // Bc1-f4
        move(game, 7, 2, 4, 5); // Bc8-f5
        move(game, 0, 3, 2, 3); // Qd1-d3
        move(game, 7, 3, 5, 3); // Qd8-d6

        // Verify castling is in legal squares
        List<Square> kingLegal = game.getLegalSquares(new Square(0, 4));
        assertTrue(kingLegal.stream().anyMatch(s -> s.getY() == 0 && s.getX() == 2),
                "King should be able to castle queenside");

        // Castle: Ke1-c1
        move(game, 0, 4, 0, 2);

        // Verify king moved to c1
        assertInstanceOf(King.class, game.getPieceAt(new Square(0, 2)),
                "King should be on c1");
        // Verify rook moved from a1 to d1
        assertInstanceOf(Rook.class, game.getPieceAt(new Square(0, 3)),
                "Rook should be on d1 after queenside castling");
        assertNull(game.getPieceAt(new Square(0, 0)),
                "a1 should be empty after castling");
    }
}
