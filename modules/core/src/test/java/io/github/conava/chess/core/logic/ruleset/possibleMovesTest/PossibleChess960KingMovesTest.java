package io.github.conava.chess.core.logic.ruleset.possibleMovesTest;

import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.board.Board;
import io.github.conava.chess.core.data.pieces.*;
import io.github.conava.chess.core.data.player.Player;
import io.github.conava.chess.core.data.player.PlayerColor;
import io.github.conava.chess.core.logic.ruleset.possibleMoves.PossibleChess960KingMoves;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link PossibleChess960KingMoves}.
 *
 * <p>The key Chess960 difference: the castling candidate square added to the list
 * is the rook's actual file (not the king's post-castle destination g-file/c-file).
 * That is, the king "moves to the rook" rather than "moving two squares".
 */
public class PossibleChess960KingMovesTest {

    private Player playerW;
    private Player playerB;

    @BeforeEach
    public void setUp() {
        playerW = new Player("W", PlayerColor.WHITE);
        playerB = new Player("B", PlayerColor.BLACK);
    }

    /**
     * Builds an 8x8 board with all squares empty.
     */
    private Square[][] emptyBoard() {
        Square[][] board = new Square[8][8];
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                board[y][x] = new Square(y, x);
            }
        }
        return board;
    }

    // -----------------------------------------------------------------------
    // Core Chess960 property: castling candidate is the rook's file, not g/c
    // -----------------------------------------------------------------------

    /**
     * Chess960 position: king on b-file (x=1), rook on h-file (x=7).
     * Castling candidate must be the rook's square (x=7), NOT the standard g1 (x=6).
     */
    @Test
    void kingsideCastling_candidateIsRookSquare_notGFile() {
        Square[][] grid = emptyBoard();
        King king = new King(playerW);      // unmoved
        grid[0][1].setPiece(king);
        grid[0][7].setPiece(new Rook(playerW)); // unmoved rook on h-file

        Board board = new Board(grid);
        PossibleChess960KingMoves moves = new PossibleChess960KingMoves(grid[0][1], board);
        List<Square> squares = moves.getPossibleSquares();

        // In Chess960 mode the king "moves to the rook": candidate = x=7 (h-file)
        assertTrue(squares.contains(grid[0][7]),
                "Kingside castling candidate should be the rook's square (h-file, x=7)");
        // The standard g1 square (x=6) is NOT the castling candidate
        assertFalse(squares.contains(grid[0][6]),
                "Standard g1 (x=6) must not be added as the castling candidate");
    }

    /**
     * Chess960 position: king on g-file (x=6), rook on a-file (x=0).
     * Castling candidate must be the rook's square (x=0), NOT the standard c1 (x=2).
     */
    @Test
    void queensideCastling_candidateIsRookSquare_notCFile() {
        Square[][] grid = emptyBoard();
        King king = new King(playerW);      // unmoved
        grid[0][6].setPiece(king);
        grid[0][0].setPiece(new Rook(playerW)); // unmoved rook on a-file

        Board board = new Board(grid);
        PossibleChess960KingMoves moves = new PossibleChess960KingMoves(grid[0][6], board);
        List<Square> squares = moves.getPossibleSquares();

        // In Chess960 mode the king "moves to the rook": candidate = x=0 (a-file)
        assertTrue(squares.contains(grid[0][0]),
                "Queenside castling candidate should be the rook's square (a-file, x=0)");
        // The standard c1 square (x=2) is NOT the castling candidate
        assertFalse(squares.contains(grid[0][2]),
                "Standard c1 (x=2) must not be added as the castling candidate");
    }

    // -----------------------------------------------------------------------
    // King-moved guard
    // -----------------------------------------------------------------------

    /**
     * After the king has moved, no castling candidates are added regardless of rook state.
     */
    @Test
    void nocastling_whenKingHasMoved() {
        Square[][] grid = emptyBoard();
        King king = new King(playerW);
        king.setHasMoved();                     // king has moved
        grid[0][3].setPiece(king);
        grid[0][0].setPiece(new Rook(playerW)); // unmoved rook queenside
        grid[0][7].setPiece(new Rook(playerW)); // unmoved rook kingside

        Board board = new Board(grid);
        PossibleChess960KingMoves moves = new PossibleChess960KingMoves(grid[0][3], board);
        List<Square> squares = moves.getPossibleSquares();

        assertFalse(squares.contains(grid[0][0]),
                "Queenside castling must be blocked after king has moved");
        assertFalse(squares.contains(grid[0][7]),
                "Kingside castling must be blocked after king has moved");
    }

    // -----------------------------------------------------------------------
    // Rook-moved guard
    // -----------------------------------------------------------------------

    /**
     * If the queenside rook has moved, the queenside candidate is not added.
     * The kingside candidate with an unmoved rook must still be present.
     */
    @Test
    void queensideCastling_blockedWhenRookHasMoved() {
        Square[][] grid = emptyBoard();
        King king = new King(playerW);          // unmoved
        grid[0][3].setPiece(king);
        Rook rookQ = new Rook(playerW);
        rookQ.setHasMoved();                    // queenside rook has moved
        grid[0][0].setPiece(rookQ);
        grid[0][7].setPiece(new Rook(playerW)); // kingside rook unmoved

        Board board = new Board(grid);
        PossibleChess960KingMoves moves = new PossibleChess960KingMoves(grid[0][3], board);
        List<Square> squares = moves.getPossibleSquares();

        assertFalse(squares.contains(grid[0][0]),
                "Queenside castling must be blocked after queenside rook has moved");
        assertTrue(squares.contains(grid[0][7]),
                "Kingside castling must still be available when only queenside rook moved");
    }

    @Test
    void kingsideCastling_blockedWhenRookHasMoved() {
        Square[][] grid = emptyBoard();
        King king = new King(playerW);          // unmoved
        grid[0][3].setPiece(king);
        grid[0][0].setPiece(new Rook(playerW)); // queenside rook unmoved
        Rook rookK = new Rook(playerW);
        rookK.setHasMoved();                    // kingside rook has moved
        grid[0][7].setPiece(rookK);

        Board board = new Board(grid);
        PossibleChess960KingMoves moves = new PossibleChess960KingMoves(grid[0][3], board);
        List<Square> squares = moves.getPossibleSquares();

        assertFalse(squares.contains(grid[0][7]),
                "Kingside castling must be blocked after kingside rook has moved");
        assertTrue(squares.contains(grid[0][0]),
                "Queenside castling must still be available when only kingside rook moved");
    }

    // -----------------------------------------------------------------------
    // Intervening-piece guard
    // -----------------------------------------------------------------------

    /**
     * If a piece stands between the king and the kingside rook, the rook's square
     * must not be added as a castling candidate.
     */
    @Test
    void kingsideCastling_blockedByInterveningPiece() {
        Square[][] grid = emptyBoard();
        King king = new King(playerW);
        grid[0][1].setPiece(king);
        grid[0][7].setPiece(new Rook(playerW));
        grid[0][4].setPiece(new Knight(playerW)); // blocks the path

        Board board = new Board(grid);
        PossibleChess960KingMoves moves = new PossibleChess960KingMoves(grid[0][1], board);
        List<Square> squares = moves.getPossibleSquares();

        assertFalse(squares.contains(grid[0][7]),
                "Kingside castling must be blocked by piece on x=4");
    }

    @Test
    void queensideCastling_blockedByInterveningPiece() {
        Square[][] grid = emptyBoard();
        King king = new King(playerW);
        grid[0][6].setPiece(king);
        grid[0][0].setPiece(new Rook(playerW));
        grid[0][3].setPiece(new Bishop(playerW)); // blocks the path

        Board board = new Board(grid);
        PossibleChess960KingMoves moves = new PossibleChess960KingMoves(grid[0][6], board);
        List<Square> squares = moves.getPossibleSquares();

        assertFalse(squares.contains(grid[0][0]),
                "Queenside castling must be blocked by piece on x=3");
    }

    // -----------------------------------------------------------------------
    // Enemy rook — must not trigger castling
    // -----------------------------------------------------------------------

    @Test
    void nocastling_withEnemyRookOnSideFile() {
        Square[][] grid = emptyBoard();
        King king = new King(playerW);
        grid[0][3].setPiece(king);
        grid[0][7].setPiece(new Rook(playerB)); // enemy rook on h-file

        Board board = new Board(grid);
        PossibleChess960KingMoves moves = new PossibleChess960KingMoves(grid[0][3], board);
        List<Square> squares = moves.getPossibleSquares();

        assertFalse(squares.contains(grid[0][7]),
                "Enemy rook must not be a castling candidate");
    }

    // -----------------------------------------------------------------------
    // Standard position compatibility: king e-file, rooks a/h files
    // -----------------------------------------------------------------------

    /**
     * When the king is on e1 (x=4) and rooks are on a1 (x=0) and h1 (x=7),
     * the castling candidates should be the rook squares themselves (a1 and h1),
     * consistent with Chess960 encoding.
     */
    @Test
    void standardPositionKingOnEFile_candidatesAreRookSquares() {
        Square[][] grid = emptyBoard();
        King king = new King(playerW);      // unmoved
        grid[0][4].setPiece(king);
        grid[0][0].setPiece(new Rook(playerW)); // a-file
        grid[0][7].setPiece(new Rook(playerW)); // h-file

        Board board = new Board(grid);
        PossibleChess960KingMoves moves = new PossibleChess960KingMoves(grid[0][4], board);
        List<Square> squares = moves.getPossibleSquares();

        assertTrue(squares.contains(grid[0][0]),
                "Queenside castling candidate must be a1 (x=0) in Chess960 encoding");
        assertTrue(squares.contains(grid[0][7]),
                "Kingside castling candidate must be h1 (x=7) in Chess960 encoding");
    }

    // -----------------------------------------------------------------------
    // Normal king moves still work (one-step adjacency)
    // -----------------------------------------------------------------------

    @Test
    void normalMoves_areIncluded() {
        Square[][] grid = emptyBoard();
        King king = new King(playerW);
        king.setHasMoved();
        grid[0][4].setPiece(king);

        Board board = new Board(grid);
        PossibleChess960KingMoves moves = new PossibleChess960KingMoves(grid[0][4], board);
        List<Square> squares = moves.getPossibleSquares();

        // King can move to d1 (x=3), f1 (x=5), d2 (y=1,x=3), e2 (y=1,x=4), f2 (y=1,x=5)
        assertTrue(squares.contains(grid[0][3]), "King should be able to move to d1");
        assertTrue(squares.contains(grid[0][5]), "King should be able to move to f1");
        assertTrue(squares.contains(grid[1][4]), "King should be able to move to e2");
    }

    /**
     * The king may not move to a square occupied by a friendly piece.
     */
    @Test
    void normalMoves_blockedByFriendlyPiece() {
        Square[][] grid = emptyBoard();
        King king = new King(playerW);
        king.setHasMoved();
        grid[0][4].setPiece(king);
        grid[0][5].setPiece(new Knight(playerW)); // friendly blocks f1

        Board board = new Board(grid);
        PossibleChess960KingMoves moves = new PossibleChess960KingMoves(grid[0][4], board);
        List<Square> squares = moves.getPossibleSquares();

        assertFalse(squares.contains(grid[0][5]),
                "King must not move to a square occupied by a friendly piece");
    }
}
