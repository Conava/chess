package ptp.project.data;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ptp.project.data.board.Board;
import ptp.project.data.enums.PlayerColor;
import ptp.project.data.pieces.Knight;

import static org.junit.jupiter.api.Assertions.*;

public class BoardTest {
    private Board board;
    private Square[][] boardSquares;
    private Player player1;

    @BeforeEach
    public void setUp() {
        player1 = new Player("p1", PlayerColor.WHITE);
        boardSquares = new Square[8][8];

        boardSquares[4][1].setPiece(new Knight(player1));

        board = new Board(boardSquares);
    }

    @Test
    void testBoard() {
    }



}
