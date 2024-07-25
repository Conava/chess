package ptp.logic.ruleset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import ptp.data.Player;
import ptp.data.Square;
import ptp.data.board.Board;
import ptp.data.enums.PlayerColor;
import ptp.logic.ruleset.standardChessRuleset.StandardChessRuleset;

import java.util.ArrayList;
import java.util.List;

class StandardChessRulesetTest {

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void getWidth() {
    }

    @Test
    void getHeight() {
    }

    @Test
    void getStartBoard() {
    }

    @Test
    void getSudoLegalSquares() {
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
        Ruleset ruleset = new StandardChessRuleset();
        Player playerW = new Player("pw", PlayerColor.WHITE);
        Player playerB = new Player("pb", PlayerColor.BLACK);
        Board board = new Board(ruleset.getStartBoard(playerW, playerB));
        List<Square> squares;

        squares = ruleset.getLegalSquares(board.getSquare(0,0), board, new ArrayList<>(), playerW, playerB);
        if (!squares.isEmpty()) {
            for (Square square : squares) {
                System.out.println("X=" + square.getX() + " Y=" + square.getY());
            }
        } else {System.out.println("Piece has no moves");}

        squares = ruleset.getLegalSquares(board.getSquare(1,0), board, new ArrayList<>(), playerW, playerB);
        if (!squares.isEmpty()) {
            for (Square square : squares) {
                System.out.println("X=" + square.getX() + " Y=" + square.getY());
            }
        } else {System.out.println("Piece has no moves");}
    }
}