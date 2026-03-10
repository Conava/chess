package io.github.conava.chess.core.logic.ruleset.possibleMovesTest;

import io.github.conava.chess.core.data.pieces.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.board.Board;
import io.github.conava.chess.core.data.player.Player;
import io.github.conava.chess.core.data.player.PlayerColor;
import io.github.conava.chess.core.logic.moves.Move;
import io.github.conava.chess.core.logic.ruleset.possibleMoves.PossibleStandardPawnMoves;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PossibleStandardPawnMovesTest {

    private Player playerW;
    private Player playerB;

    @BeforeEach
    public void setUp() {
        playerW = new Player("W", PlayerColor.WHITE);
        playerB = new Player("B", PlayerColor.BLACK);
    }

    @Test
    public void testGetPossibleSquares() {
        //q3nrk1/4bppp/3p4/4nPP1/r2BP2P/Np6/1P1Q4/1K1R1B1R w - - 0 1
        Square[][] startBoard = new Square[8][8];
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                startBoard[y][x] = new Square(y, x);
            }
        }
        startBoard[0][1].setPiece(new King(playerW));
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
        startBoard[7][6].setPiece(new King(playerB));

        Board board = new Board(startBoard);

        List<Square> possibleSquares;
        List<Move> lastMoves = null;

        PossibleStandardPawnMoves movesP1 = new PossibleStandardPawnMoves(startBoard[2][1], board, lastMoves);
        possibleSquares = movesP1.possibleMoves();

        assertTrue(possibleSquares.isEmpty());

        PossibleStandardPawnMoves movesP2 = new PossibleStandardPawnMoves(startBoard[6][7], board, lastMoves);
        possibleSquares = movesP2.possibleMoves();

        assertFalse(possibleSquares.isEmpty());
        assertTrue(possibleSquares.contains(startBoard[4][7]));
        assertEquals(2, possibleSquares.size());
    }

    /**
     * White pawn on e5 (y=4, x=4). Black pawn just double-pushed from d7 to d5
     * (y=6 to y=4, x=3). En passant left (toward d-file) must be available.
     * Expected: the square d6 (y=5, x=3) is included in possible moves.
     */
    @Test
    public void testEnPassantLeftAvailableForWhite() {
        Square[][] grid = buildEmptyGrid();

        // White pawn on e5 (y=4, x=4)
        Square whitePawnSquare = grid[4][4];
        whitePawnSquare.setPiece(new Pawn(playerW));

        // Black pawn on d5 (y=4, x=3) — just double-pushed from d7 (y=6, x=3)
        Square blackPawnSquare = grid[4][3];
        blackPawnSquare.setPiece(new Pawn(playerB));

        Board board = new Board(grid);

        // Simulate: last move was black pawn d7 -> d5
        Square moveStart = new Square(6, 3);
        moveStart.setPiece(new Pawn(playerB));
        Square moveEnd = new Square(4, 3);
        Move lastMove = new Move(moveStart, moveEnd);
        List<Move> moves = List.of(lastMove);

        PossibleStandardPawnMoves generator = new PossibleStandardPawnMoves(whitePawnSquare, board, moves);
        List<Square> result = generator.possibleMoves();

        // En passant target square: d6 (y=5, x=3)
        Square enPassantTarget = new Square(5, 3);
        assertTrue(result.contains(enPassantTarget),
                "En passant left should be available: d6 expected in possible moves");
    }

    /**
     * White pawn on e5 (y=4, x=4). Black pawn just double-pushed from f7 to f5
     * (y=6 to y=4, x=5). En passant right (toward f-file) must be available.
     * Expected: the square f6 (y=5, x=5) is included in possible moves.
     */
    @Test
    public void testEnPassantRightAvailableForWhite() {
        Square[][] grid = buildEmptyGrid();

        // White pawn on e5 (y=4, x=4)
        Square whitePawnSquare = grid[4][4];
        whitePawnSquare.setPiece(new Pawn(playerW));

        // Black pawn on f5 (y=4, x=5) — just double-pushed from f7 (y=6, x=5)
        Square blackPawnSquare = grid[4][5];
        blackPawnSquare.setPiece(new Pawn(playerB));

        Board board = new Board(grid);

        // Simulate: last move was black pawn f7 -> f5
        Square moveStart = new Square(6, 5);
        moveStart.setPiece(new Pawn(playerB));
        Square moveEnd = new Square(4, 5);
        Move lastMove = new Move(moveStart, moveEnd);
        List<Move> moves = List.of(lastMove);

        PossibleStandardPawnMoves generator = new PossibleStandardPawnMoves(whitePawnSquare, board, moves);
        List<Square> result = generator.possibleMoves();

        // En passant target square: f6 (y=5, x=5)
        Square enPassantTarget = new Square(5, 5);
        assertTrue(result.contains(enPassantTarget),
                "En passant right should be available: f6 expected in possible moves");
    }

    /**
     * Black pawn on d4 (y=3, x=3). White pawn just double-pushed from e2 to e4
     * (y=1 to y=3, x=4). En passant right (toward e-file, which is +1 for black because
     * black's x-axis is the same) must be available.
     * Expected: the square e3 (y=2, x=4) is included in possible moves.
     */
    @Test
    public void testEnPassantRightAvailableForBlack() {
        Square[][] grid = buildEmptyGrid();

        // Black pawn on d4 (y=3, x=3)
        Square blackPawnSquare = grid[3][3];
        blackPawnSquare.setPiece(new Pawn(playerB));

        // White pawn on e4 (y=3, x=4) — just double-pushed from e2 (y=1, x=4)
        Square whitePawnSquare = grid[3][4];
        whitePawnSquare.setPiece(new Pawn(playerW));

        Board board = new Board(grid);

        // Simulate: last move was white pawn e2 -> e4
        Square moveStart = new Square(1, 4);
        moveStart.setPiece(new Pawn(playerW));
        Square moveEnd = new Square(3, 4);
        Move lastMove = new Move(moveStart, moveEnd);
        List<Move> moves = List.of(lastMove);

        PossibleStandardPawnMoves generator = new PossibleStandardPawnMoves(blackPawnSquare, board, moves);
        List<Square> result = generator.possibleMoves();

        // En passant target square: e3 (y=2, x=4)
        Square enPassantTarget = new Square(2, 4);
        assertTrue(result.contains(enPassantTarget),
                "En passant right should be available for black: e3 expected in possible moves");
    }

    /**
     * White pawn on e5 (y=4, x=4). Last move was a black rook moving (not a double pawn push).
     * En passant must NOT be available.
     */
    @Test
    public void testEnPassantNotAvailableIfLastMoveWasNotDoublePawnPush() {
        Square[][] grid = buildEmptyGrid();

        // White pawn on e5 (y=4, x=4)
        Square whitePawnSquare = grid[4][4];
        whitePawnSquare.setPiece(new Pawn(playerW));

        // Black pawn on d5 (adjacent, but did NOT just double-push)
        grid[4][3].setPiece(new Pawn(playerB));

        Board board = new Board(grid);

        // Last move was a rook move (not a double pawn push)
        Square rookStart = new Square(7, 0);
        rookStart.setPiece(new Rook(playerB));
        Square rookEnd = new Square(5, 0);
        Move lastMove = new Move(rookStart, rookEnd);
        List<Move> moves = List.of(lastMove);

        PossibleStandardPawnMoves generator = new PossibleStandardPawnMoves(whitePawnSquare, board, moves);
        List<Square> result = generator.possibleMoves();

        // En passant target d6 (y=5, x=3) must NOT be in result
        Square enPassantTarget = new Square(5, 3);
        assertFalse(result.contains(enPassantTarget),
                "En passant should NOT be available when last move was not a double pawn push");
    }

    /**
     * White pawn on e5 (y=4, x=4). Black pawn double-pushed to d5 two moves ago,
     * but an intervening move by another piece occurred since then.
     * En passant must NOT be available.
     */
    @Test
    public void testEnPassantNotAvailableAfterInterveningMove() {
        Square[][] grid = buildEmptyGrid();

        // White pawn on e5 (y=4, x=4)
        Square whitePawnSquare = grid[4][4];
        whitePawnSquare.setPiece(new Pawn(playerW));

        // Black pawn on d5 (y=4, x=3)
        grid[4][3].setPiece(new Pawn(playerB));

        Board board = new Board(grid);

        // Two moves: first the double pawn push, then an intervening rook move
        Square pawnMoveStart = new Square(6, 3);
        pawnMoveStart.setPiece(new Pawn(playerB));
        Square pawnMoveEnd = new Square(4, 3);
        Move doublePush = new Move(pawnMoveStart, pawnMoveEnd);

        Square rookStart = new Square(7, 0);
        rookStart.setPiece(new Rook(playerW));
        Square rookEnd = new Square(6, 0);
        Move rookMove = new Move(rookStart, rookEnd);

        // The intervening rook move is the LAST move — en passant opportunity is gone
        List<Move> moves = new ArrayList<>();
        moves.add(doublePush);
        moves.add(rookMove);

        PossibleStandardPawnMoves generator = new PossibleStandardPawnMoves(whitePawnSquare, board, moves);
        List<Square> result = generator.possibleMoves();

        // En passant target d6 (y=5, x=3) must NOT be in result
        Square enPassantTarget = new Square(5, 3);
        assertFalse(result.contains(enPassantTarget),
                "En passant should NOT be available after an intervening move");
    }

    /**
     * White pawn on e5 (y=4, x=4). Last move was a single-square pawn push (not double).
     * En passant must NOT be available.
     */
    @Test
    public void testEnPassantNotAvailableAfterSingleSquarePawnPush() {
        Square[][] grid = buildEmptyGrid();

        // White pawn on e5 (y=4, x=4)
        Square whitePawnSquare = grid[4][4];
        whitePawnSquare.setPiece(new Pawn(playerW));

        // Black pawn on d5 (adjacent)
        grid[4][3].setPiece(new Pawn(playerB));

        Board board = new Board(grid);

        // Last move was black pawn moving only 1 square: d6 -> d5 (single push)
        Square pawnStart = new Square(5, 3);
        pawnStart.setPiece(new Pawn(playerB));
        Square pawnEnd = new Square(4, 3);
        Move singlePush = new Move(pawnStart, pawnEnd);
        List<Move> moves = List.of(singlePush);

        PossibleStandardPawnMoves generator = new PossibleStandardPawnMoves(whitePawnSquare, board, moves);
        List<Square> result = generator.possibleMoves();

        // En passant target d6 (y=5, x=3) must NOT be in result
        Square enPassantTarget = new Square(5, 3);
        assertFalse(result.contains(enPassantTarget),
                "En passant should NOT be available after a single-square pawn push");
    }

    // Helper to build an 8x8 empty board grid
    private Square[][] buildEmptyGrid() {
        Square[][] grid = new Square[8][8];
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                grid[y][x] = new Square(y, x);
            }
        }
        return grid;
    }
}
