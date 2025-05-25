package io.github.conava.chess.application.core.data;

import io.github.conava.chess.core.data.Square;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.github.conava.chess.core.data.board.Board;
import io.github.conava.chess.core.data.player.PlayerColor;
import io.github.conava.chess.core.data.pieces.Knight;
import io.github.conava.chess.core.data.player.Player;

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



}
