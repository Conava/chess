package io.github.conava.chess.core.data;

import io.github.conava.chess.core.data.Square;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.github.conava.chess.core.data.board.Board;
import io.github.conava.chess.core.data.player.PlayerColor;
import io.github.conava.chess.core.data.pieces.Knight;
import io.github.conava.chess.core.data.pieces.Pawn;
import io.github.conava.chess.core.data.player.Player;
import io.github.conava.chess.core.logic.moves.Move;

import static org.junit.jupiter.api.Assertions.*;

public class BoardTest {
    private Board board;
    private Square[][] boardSquares;
    private Player player1;

@BeforeEach
public void setUp() {
    player1 = new Player("p1", PlayerColor.WHITE);
    boardSquares = new Square[8][8];

    // Initialize each Square in the array
    for (int i = 0; i < boardSquares.length; i++) {
        for (int j = 0; j < boardSquares[i].length; j++) {
            boardSquares[i][j] = new Square(i, j);
        }
    }

    // Now you can safely set a piece on a Square
    boardSquares[4][1].setPiece(new Knight(player1));

    board = new Board(boardSquares);
}

    @Test
    void testBoard() {
    }

    /**
     * En passant scenario: white pawn on e5 (y=4, x=4), black pawn on d5 (y=4, x=3).
     * White executes the en passant capture by moving diagonally to d6 (y=5, x=3),
     * which is currently an empty square. After execution the black pawn at d5 must
     * be removed from the board and white pawn must be at d6.
     */
    @Test
    void enPassantCaptureRemovesOpponentPawn() {
        Player white = new Player("White", PlayerColor.WHITE);
        Player black = new Player("Black", PlayerColor.BLACK);

        Square[][] squares = new Square[8][8];
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                squares[y][x] = new Square(y, x);
            }
        }

        // White pawn on e5: y=4, x=4
        Pawn whitePawn = new Pawn(white);
        squares[4][4].setPiece(whitePawn);

        // Black pawn on d5: y=4, x=3
        Pawn blackPawn = new Pawn(black);
        squares[4][3].setPiece(blackPawn);

        // d6 (y=5, x=3) is empty — en passant destination
        Board enPassantBoard = new Board(squares);

        // Execute the diagonal pawn move from e5 to d6
        Move enPassantMove = new Move(squares[4][4], squares[5][3]);
        enPassantBoard.executeMove(enPassantMove);

        // White pawn must now be on d6
        assertNotNull(enPassantBoard.getSquare(5, 3).getPiece(),
                "White pawn should be at d6 after en passant");
        assertInstanceOf(Pawn.class, enPassantBoard.getSquare(5, 3).getPiece(),
                "Piece at d6 should be a Pawn");
        assertEquals(white, enPassantBoard.getSquare(5, 3).getPiece().getPlayer(),
                "Piece at d6 should belong to white");

        // Black pawn must have been removed from d5
        assertNull(enPassantBoard.getSquare(4, 3).getPiece(),
                "Black pawn should have been removed from d5 by en passant");

        // e5 must be empty
        assertNull(enPassantBoard.getSquare(4, 4).getPiece(),
                "e5 should be empty after white pawn moved away");
    }

}
