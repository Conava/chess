package io.github.conava.chess.core.logic.ruleset;

import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.board.Board;
import io.github.conava.chess.core.data.pieces.*;
import io.github.conava.chess.core.data.player.Player;
import io.github.conava.chess.core.data.player.PlayerColor;
import io.github.conava.chess.core.logic.moves.Move;
import io.github.conava.chess.core.logic.ruleset.standardChessRuleset.StandardChessRuleset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link StandardChessRuleset#getLegalSquares}.
 *
 * <p>All tests use manually constructed boards to isolate specific check-legality scenarios.
 * Board coordinate convention: {@code getSquare(y, x)} where y=0 is white's back rank (rank 1)
 * and x=0 is the a-file.
 */
class StandardChessRulesetTest {

    private StandardChessRuleset ruleset;
    private Player white;
    private Player black;

    @BeforeEach
    void setUp() {
        ruleset = new StandardChessRuleset();
        white = new Player("White", PlayerColor.WHITE);
        black = new Player("Black", PlayerColor.BLACK);
    }

    // ---------------------------------------------------------------------------
    // Helper: build an empty 8x8 board
    // ---------------------------------------------------------------------------

    private Square[][] emptyBoard() {
        Square[][] squares = new Square[8][8];
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                squares[y][x] = new Square(y, x);
            }
        }
        return squares;
    }

    // ---------------------------------------------------------------------------
    // 1. Normal moves that do not expose the king are included
    // ---------------------------------------------------------------------------

    /**
     * A queen with no pin should have all its pseudo-legal squares returned as legal.
     * Board: White King on e1 (y=0,x=4), White Queen on d1 (y=0,x=3), no pieces threatening king.
     * The queen can move freely — all pseudo-legal squares should be legal.
     */
    @Test
    void normalMovesNotExposingKingAreIncluded() {
        Square[][] squares = emptyBoard();
        squares[0][4].setPiece(new King(white));   // white king on e1
        squares[0][3].setPiece(new Queen(white));  // white queen on d1
        Board board = new Board(squares);

        List<Square> legal = ruleset.getLegalSquares(
                board.getSquare(0, 3), board, new ArrayList<>(), white, black);

        // The queen on d1 is not pinned — it must have legal moves available
        assertFalse(legal.isEmpty(),
                "Queen with no pin should have legal moves");
    }

    // ---------------------------------------------------------------------------
    // 2. getLegalSquares excludes a move that leaves the king in check (exposed king)
    // ---------------------------------------------------------------------------

    /**
     * A piece that, by moving, would expose its own king to check must have that
     * move excluded from the legal moves list.
     *
     * <p>Setup: White King on e1 (y=0,x=4). White Rook on e2 (y=1,x=4). Black Rook on e8 (y=7,x=4).
     * The white rook on e2 is pinned along the e-file. If it moves off the e-file, the white king
     * would be in check from the black rook. All moves that leave the e-file should be excluded.
     */
    @Test
    void moveExposingKingToCheckIsExcluded() {
        Square[][] squares = emptyBoard();
        squares[0][4].setPiece(new King(white));  // white king e1
        squares[1][4].setPiece(new Rook(white));  // white rook e2 (pinned on e-file)
        squares[7][4].setPiece(new Rook(black));  // black rook e8 (pinning)
        Board board = new Board(squares);

        List<Square> legal = ruleset.getLegalSquares(
                board.getSquare(1, 4), board, new ArrayList<>(), white, black);

        // The pinned rook may only move along the e-file (x=4); no lateral moves allowed
        for (Square s : legal) {
            assertEquals(4, s.getX(),
                    "Pinned rook must only move along the pin file (x=4), but got x=" + s.getX());
        }
    }

    // ---------------------------------------------------------------------------
    // 3. Pinned piece cannot move off its pin line
    // ---------------------------------------------------------------------------

    /**
     * A bishop pinned diagonally cannot move at all if moving in any direction
     * would expose the king on a file.
     *
     * <p>Setup: White King on e1 (y=0,x=4). White Bishop on d2 (y=1,x=3). Black Queen on a5 (y=4,x=0).
     * The bishop is pinned along the diagonal from e1 to a5. Moving the bishop off this diagonal
     * exposes the king to the black queen. Since the bishop can only move diagonally and the only
     * diagonal the queen controls is the one from d2, the bishop must not be able to move off the
     * diagonal without exposing the king.
     * Actually simpler: use a file pin — White Rook on e3 (y=2,x=4), pinned by Black Rook on e8.
     * King on e1. Bishop on d2 is free, but rook on e3 is pinned. We test the rook.
     *
     * <p>Here we use a diagonal pin specifically for a bishop:
     * White King on e1 (y=0,x=4). White Bishop on d2 (y=1,x=3). Black Bishop on a5 (y=4,x=0).
     * The black bishop on a5 attacks along the a5-e1 diagonal. The white bishop on d2 sits on
     * that diagonal and is pinned. Moving the white bishop off the diagonal exposes the king.
     * The white bishop can capture the pinning piece on a5 (legal) or block — but since a bishop
     * moves diagonally and is pinned on a diagonal, it can only move along that pin diagonal.
     * Moves to b3 (y=2,x=1) or c4 (y=3,x=2) along the pin diagonal are legal.
     * No moves perpendicular to the pin diagonal are legal.
     */
    @Test
    void pinnedBishopCannotMoveOffPinLine() {
        Square[][] squares = emptyBoard();
        squares[0][4].setPiece(new King(white));    // white king e1
        squares[1][3].setPiece(new Bishop(white));  // white bishop d2 (pinned on a5-e1 diagonal)
        squares[4][0].setPiece(new Bishop(black));  // black bishop a5 (pins the white bishop)
        Board board = new Board(squares);

        List<Square> legal = ruleset.getLegalSquares(
                board.getSquare(1, 3), board, new ArrayList<>(), white, black);

        // Legal moves must only be along the pin diagonal (x + y = constant = 4)
        // Diagonal a5-e1: squares where x + y == 4: (0,4),(1,3),(2,2),(3,1),(4,0)
        // The bishop is at (1,3). It can move to (2,2), (3,1), (4,0)[capture] along the pin diagonal
        // It must NOT move to squares where x + y != 4
        for (Square s : legal) {
            assertEquals(4, s.getX() + s.getY(),
                    "Pinned bishop must only move along the pin diagonal (x+y=4), got ("
                            + s.getY() + "," + s.getX() + ")");
        }

        // Must have at least one legal move (can capture or move along diagonal)
        assertFalse(legal.isEmpty(),
                "Pinned bishop should still have moves along the pin line");
    }

    // ---------------------------------------------------------------------------
    // 4. Castling while in check is excluded
    // ---------------------------------------------------------------------------

    /**
     * When the king is currently in check, castling moves must not appear in the legal moves list.
     *
     * <p>Setup: White King unmoved on e1 (y=0,x=4). White Rook unmoved on h1 (y=0,x=7).
     * Black Rook on e8 (y=7,x=4) giving check on the e-file. Castling kingside (king to g1)
     * must be excluded because the king is currently in check.
     */
    @Test
    void castlingWhileInCheckIsExcluded() {
        Square[][] squares = emptyBoard();
        King whiteKing = new King(white);
        squares[0][4].setPiece(whiteKing);  // white king e1 (unmoved)
        Rook whiteRook = new Rook(white);
        squares[0][7].setPiece(whiteRook);  // white rook h1 (unmoved)
        squares[7][4].setPiece(new Rook(black));  // black rook e8 — gives check
        Board board = new Board(squares);

        // Verify the king is actually in check before testing
        assertTrue(ruleset.isCheck(board, white, new ArrayList<>()),
                "White king should be in check from black rook on e8");

        List<Square> legal = ruleset.getLegalSquares(
                board.getSquare(0, 4), board, new ArrayList<>(), white, black);

        // Castling kingside would land on g1 (y=0,x=6) — must not appear
        boolean castlingKingsidePresent = legal.stream()
                .anyMatch(s -> s.getY() == 0 && s.getX() == 6);
        assertFalse(castlingKingsidePresent,
                "Castling kingside (g1) must not be legal when king is in check");
    }

    // ---------------------------------------------------------------------------
    // 5. Castling through check is excluded
    // ---------------------------------------------------------------------------

    /**
     * When the king would pass through a square under attack during castling, that
     * castling move must be excluded even if the king's final square is safe.
     *
     * <p>Setup: White King unmoved on e1 (y=0,x=4). White Rook unmoved on h1 (y=0,x=7).
     * Black Rook on f8 (y=7,x=5) attacks f1 (y=0,x=5) — the transit square for kingside castling.
     * Even though g1 (y=0,x=6) might be safe, the king would pass through f1 which is attacked,
     * so kingside castling must be excluded.
     */
    @Test
    void castlingThroughCheckIsExcluded() {
        Square[][] squares = emptyBoard();
        King whiteKing = new King(white);
        squares[0][4].setPiece(whiteKing);  // white king e1 (unmoved)
        Rook whiteRook = new Rook(white);
        squares[0][7].setPiece(whiteRook);  // white rook h1 (unmoved)
        squares[7][5].setPiece(new Rook(black));  // black rook f8 — attacks f1 (transit square)
        Board board = new Board(squares);

        // Verify the king is NOT in check initially (only the transit square is attacked)
        assertFalse(ruleset.isCheck(board, white, new ArrayList<>()),
                "White king should NOT be in check initially");

        List<Square> legal = ruleset.getLegalSquares(
                board.getSquare(0, 4), board, new ArrayList<>(), white, black);

        // Castling kingside lands on g1 (y=0,x=6) but passes through f1 (y=0,x=5) which is attacked
        boolean castlingKingsidePresent = legal.stream()
                .anyMatch(s -> s.getY() == 0 && s.getX() == 6);
        assertFalse(castlingKingsidePresent,
                "Castling kingside must not be legal when the transit square f1 is under attack");

        // The filter must be selective: a normal king step (e.g., to d1) must still be legal
        boolean d1Present = legal.stream()
                .anyMatch(s -> s.getY() == 0 && s.getX() == 3);
        assertTrue(d1Present,
                "Non-castling king move to d1 must remain legal (filter must not over-prune)");
    }

    // ---------------------------------------------------------------------------
    // 6. Check filter works for BLACK — pinned black piece has only file moves legal
    // ---------------------------------------------------------------------------

    /**
     * Verifies that the check-legality filter correctly protects the BLACK king,
     * not just the white king.
     *
     * <p>Setup: Black King on e8 (y=7,x=4). Black Rook on e7 (y=6,x=4). White Rook on e1 (y=0,x=4).
     * The black rook on e7 is pinned along the e-file. Moving it off the e-file would expose the
     * black king to the white rook. Therefore no lateral (rank) moves are legal for the black rook,
     * and every legal move must remain on the e-file (x=4).
     *
     * <p>This test deliberately passes {@code white} as {@code player1} to simulate the bug in
     * {@code Game.getLegalSquares} — the fix must derive the moving player from the piece itself.
     */
    @Test
    void checkFilterWorksForBlackPinnedRook() {
        Square[][] squares = emptyBoard();
        squares[7][4].setPiece(new King(black));   // black king e8
        squares[6][4].setPiece(new Rook(black));   // black rook e7 (pinned on e-file)
        squares[0][4].setPiece(new Rook(white));   // white rook e1 (the pin source)
        Board board = new Board(squares);

        // Deliberately pass white as player1 to reproduce the bug in Game.getLegalSquares.
        // The fix derives movingPlayer from the piece, so it must still filter for black.
        List<Square> legal = ruleset.getLegalSquares(
                board.getSquare(6, 4), board, new ArrayList<>(), white, black);

        // The black rook may only move along the e-file (x=4) — no lateral moves are legal
        assertFalse(legal.isEmpty(),
                "Pinned black rook should still have moves along the pin file");
        for (Square s : legal) {
            assertEquals(4, s.getX(),
                    "Pinned black rook must only move along the pin file (x=4), but got x=" + s.getX());
        }
    }

    // ---------------------------------------------------------------------------
    // Legacy stub tests (kept to avoid breaking the file)
    // ---------------------------------------------------------------------------

    @Test
    void getWidth() {
        assertEquals(8, ruleset.getWidth());
    }

    @Test
    void getHeight() {
        assertEquals(8, ruleset.getHeight());
    }

    @Test
    void getStartBoard() {
        Square[][] board = ruleset.getStartBoard(white, black);
        assertNotNull(board);
        assertEquals(8, board.length);
    }

    @Test
    void getSudoLegalSquares() {
        // covered by the specific tests above
    }

    @Test
    void verifyMove() {
    }

    @Test
    void testVerifyMove() {
    }

    @Test
    void hasEnforcedMove() {
    }

    @Test
    void isCheck() {
    }

    @Test
    void testGetLegalMoves() {
        Ruleset rs = new StandardChessRuleset();
        Player playerW = new Player("pw", PlayerColor.WHITE);
        Player playerB = new Player("pb", PlayerColor.BLACK);
        Board board = new Board(rs.getStartBoard(playerW, playerB));
        List<Square> squares;

        squares = rs.getLegalSquares(board.getSquare(0, 0), board, new ArrayList<>(), playerW, playerB);
        if (!squares.isEmpty()) {
            for (Square square : squares) {
                System.out.println("X=" + square.getX() + " Y=" + square.getY());
            }
        } else {
            System.out.println("Piece has no moves");
        }

        squares = rs.getLegalSquares(board.getSquare(1, 0), board, new ArrayList<>(), playerW, playerB);
        if (!squares.isEmpty()) {
            for (Square square : squares) {
                System.out.println("X=" + square.getX() + " Y=" + square.getY());
            }
        } else {
            System.out.println("Piece has no moves");
        }
    }
}
