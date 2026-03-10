package io.github.conava.chess.core.logic.ruleset.possibleMovesTest;

import io.github.conava.chess.core.data.pieces.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.board.Board;
import io.github.conava.chess.core.data.player.Player;
import io.github.conava.chess.core.data.player.PlayerColor;
import io.github.conava.chess.core.logic.ruleset.possibleMoves.PossibleStandardKingMoves;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PossibleStandardKingMovesTest {

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
    // Legacy test — kept to ensure no regression in normal king movement
    // -----------------------------------------------------------------------

    @Test
    public void testGetPossibleSquares() {
        Square[][] startBoard = emptyBoard();
        King kingW = new King(playerW);
        kingW.setHasMoved();
        King kingB = new King(playerB);
        kingB.setHasMoved();

        startBoard[0][1].setPiece(kingW);
        startBoard[0][3].setPiece(new Rook(playerW));
        startBoard[0][5].setPiece(new Bishop(playerW));
        startBoard[0][7].setPiece(new Rook(playerW));
        startBoard[1][1].setPiece(new Pawn(playerW));
        startBoard[1][3].setPiece(new Queen(playerW));
        startBoard[2][0].setPiece(new Knight(playerW));
        startBoard[3][3].setPiece(new Bishop(playerW));
        startBoard[3][4].setPiece(new Pawn(playerW));
        startBoard[3][7].setPiece(new Pawn(playerW));
        startBoard[4][5].setPiece(new Pawn(playerW));
        startBoard[4][6].setPiece(new Pawn(playerW));

        startBoard[2][1].setPiece(new Pawn(playerB));
        startBoard[3][0].setPiece(new Rook(playerB));
        startBoard[4][4].setPiece(new Knight(playerB));
        startBoard[5][3].setPiece(new Pawn(playerB));
        startBoard[6][4].setPiece(new Bishop(playerB));
        startBoard[6][5].setPiece(new Pawn(playerB));
        startBoard[6][6].setPiece(new Pawn(playerB));
        startBoard[6][7].setPiece(new Pawn(playerB));
        startBoard[7][0].setPiece(new Queen(playerB));
        startBoard[7][4].setPiece(new Knight(playerB));
        startBoard[7][5].setPiece(new Rook(playerB));
        startBoard[7][6].setPiece(kingB);

        Board board = new Board(startBoard);

        PossibleStandardKingMoves movesKB = new PossibleStandardKingMoves(startBoard[7][6], board);
        List<Square> possibleSquares = movesKB.getPossibleSquares();

        assertFalse(possibleSquares.isEmpty());
        assertTrue(possibleSquares.contains(startBoard[7][7]));
        assertFalse(possibleSquares.contains(startBoard[6][6]));
        assertEquals(1, possibleSquares.size());
    }

    // -----------------------------------------------------------------------
    // Castling tests
    // -----------------------------------------------------------------------

    /**
     * From the standard start position, white king on e1 (x=4), unmoved rooks
     * on a1 (x=0) and h1 (x=7), all intervening squares empty: both castling
     * moves must be in the possible squares list.
     */
    @Test
    public void testCastlingKingsideAvailableFromStartPosition() {
        Square[][] grid = emptyBoard();
        King king = new King(playerW);          // unmoved
        grid[0][4].setPiece(king);
        grid[0][7].setPiece(new Rook(playerW)); // unmoved rook on h-file

        Board board = new Board(grid);
        PossibleStandardKingMoves moves = new PossibleStandardKingMoves(grid[0][4], board);
        List<Square> squares = moves.getPossibleSquares();

        // g1 (x=6) is the king's castling destination for kingside
        assertTrue(squares.contains(grid[0][6]),
                "Kingside castling destination (g1) should be available");
    }

    @Test
    public void testCastlingQueensideAvailableFromStartPosition() {
        Square[][] grid = emptyBoard();
        King king = new King(playerW);          // unmoved
        grid[0][4].setPiece(king);
        grid[0][0].setPiece(new Rook(playerW)); // unmoved rook on a-file

        Board board = new Board(grid);
        PossibleStandardKingMoves moves = new PossibleStandardKingMoves(grid[0][4], board);
        List<Square> squares = moves.getPossibleSquares();

        // c1 (x=2) is the king's castling destination for queenside
        assertTrue(squares.contains(grid[0][2]),
                "Queenside castling destination (c1) should be available");
    }

    /**
     * With a Knight on f1 (x=5) blocking the kingside path, kingside castling
     * must not be available.
     */
    @Test
    public void testCastlingKingsideBlockedByInterveningPiece() {
        Square[][] grid = emptyBoard();
        King king = new King(playerW);
        grid[0][4].setPiece(king);
        grid[0][7].setPiece(new Rook(playerW));
        grid[0][5].setPiece(new Knight(playerW)); // blocks f1

        Board board = new Board(grid);
        PossibleStandardKingMoves moves = new PossibleStandardKingMoves(grid[0][4], board);
        List<Square> squares = moves.getPossibleSquares();

        assertFalse(squares.contains(grid[0][6]),
                "Kingside castling must be blocked by piece on f1");
    }

    /**
     * With a Bishop on b1 (x=1) blocking the queenside path, queenside castling
     * must not be available.
     */
    @Test
    public void testCastlingQueensideBlockedByInterveningPiece() {
        Square[][] grid = emptyBoard();
        King king = new King(playerW);
        grid[0][4].setPiece(king);
        grid[0][0].setPiece(new Rook(playerW));
        grid[0][1].setPiece(new Bishop(playerW)); // blocks b1

        Board board = new Board(grid);
        PossibleStandardKingMoves moves = new PossibleStandardKingMoves(grid[0][4], board);
        List<Square> squares = moves.getPossibleSquares();

        assertFalse(squares.contains(grid[0][2]),
                "Queenside castling must be blocked by piece on b1");
    }

    /**
     * After the king has moved, neither castling option is available even if
     * both rooks are still on their starting squares.
     */
    @Test
    public void testCastlingBlockedAfterKingMoved() {
        Square[][] grid = emptyBoard();
        King king = new King(playerW);
        king.setHasMoved();                        // king has moved
        grid[0][4].setPiece(king);
        grid[0][0].setPiece(new Rook(playerW));
        grid[0][7].setPiece(new Rook(playerW));

        Board board = new Board(grid);
        PossibleStandardKingMoves moves = new PossibleStandardKingMoves(grid[0][4], board);
        List<Square> squares = moves.getPossibleSquares();

        assertFalse(squares.contains(grid[0][2]),
                "Queenside castling must be unavailable after king has moved");
        assertFalse(squares.contains(grid[0][6]),
                "Kingside castling must be unavailable after king has moved");
    }

    /**
     * After the queenside rook has moved, queenside castling is blocked even
     * if the king has not moved. Kingside castling with an unmoved rook must
     * still be available.
     */
    @Test
    public void testCastlingQueensideBlockedAfterRookMoved() {
        Square[][] grid = emptyBoard();
        King king = new King(playerW);          // unmoved
        grid[0][4].setPiece(king);
        Rook rookA = new Rook(playerW);
        rookA.setHasMoved();                    // queenside rook has moved
        grid[0][0].setPiece(rookA);
        grid[0][7].setPiece(new Rook(playerW)); // kingside rook unmoved

        Board board = new Board(grid);
        PossibleStandardKingMoves moves = new PossibleStandardKingMoves(grid[0][4], board);
        List<Square> squares = moves.getPossibleSquares();

        assertFalse(squares.contains(grid[0][2]),
                "Queenside castling must be unavailable after queenside rook has moved");
        assertTrue(squares.contains(grid[0][6]),
                "Kingside castling must still be available when only queenside rook moved");
    }

    /**
     * After the kingside rook has moved, kingside castling is blocked. Queenside
     * with an unmoved rook must still be available.
     */
    @Test
    public void testCastlingKingsideBlockedAfterRookMoved() {
        Square[][] grid = emptyBoard();
        King king = new King(playerW);          // unmoved
        grid[0][4].setPiece(king);
        grid[0][0].setPiece(new Rook(playerW)); // queenside rook unmoved
        Rook rookH = new Rook(playerW);
        rookH.setHasMoved();                    // kingside rook has moved
        grid[0][7].setPiece(rookH);

        Board board = new Board(grid);
        PossibleStandardKingMoves moves = new PossibleStandardKingMoves(grid[0][4], board);
        List<Square> squares = moves.getPossibleSquares();

        assertFalse(squares.contains(grid[0][6]),
                "Kingside castling must be unavailable after kingside rook has moved");
        assertTrue(squares.contains(grid[0][2]),
                "Queenside castling must still be available when only kingside rook moved");
    }
}
