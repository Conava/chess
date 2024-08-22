package ptp.core.logic.ruleset.possibleMovesTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ptp.core.data.Square;
import ptp.core.data.board.Board;
import ptp.core.data.pieces.*;
import ptp.core.data.player.Player;
import ptp.core.data.player.PlayerColor;
import ptp.core.logic.moves.Move;
import ptp.core.logic.ruleset.possibleMoves.PossibleStandardPawnMoves;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PossibleStandardPawnMovesTest {
    @BeforeEach
    public void setUp() {
    }

    @Test
    public void testGetPossibleSquares() {
        Player playerW = new Player("W", PlayerColor.WHITE);
        Player playerB = new Player("B", PlayerColor.BLACK);

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
}
