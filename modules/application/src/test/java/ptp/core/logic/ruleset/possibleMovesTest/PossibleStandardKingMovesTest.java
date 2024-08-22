package ptp.core.logic.ruleset.possibleMovesTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ptp.core.data.Square;
import ptp.core.data.board.Board;
import ptp.core.data.pieces.*;
import ptp.core.data.player.Player;
import ptp.core.data.player.PlayerColor;
import ptp.core.logic.ruleset.possibleMoves.PossibleStandardKingMoves;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PossibleStandardKingMovesTest {
    @BeforeEach
    public void setUp() {
    }

    @Test
    public void testGetPossibleSquares() {
        Player playerW = new Player("W", PlayerColor.WHITE);
        Player playerB = new Player("B", PlayerColor.BLACK);

        Square[][] startBoard = new Square[8][8];
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                startBoard[y][x] = new Square(y, x);
            }
        }
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

        List<Square> possibleSquares;

        PossibleStandardKingMoves movesKB = new PossibleStandardKingMoves(startBoard[7][6], board);
        possibleSquares = movesKB.getPossibleSquares();

        assertFalse(possibleSquares.isEmpty());
        assertTrue(possibleSquares.contains(startBoard[7][7]));
        assertFalse(possibleSquares.contains(startBoard[6][6]));
        assertEquals(1, possibleSquares.size());
    }
}
