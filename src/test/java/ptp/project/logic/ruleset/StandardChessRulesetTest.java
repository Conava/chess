package ptp.project.logic.ruleset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import ptp.project.data.Player;
import ptp.project.data.Square;
import ptp.project.data.board.Board;
import ptp.project.data.enums.PlayerColor;
import ptp.project.logic.ruleset.standardChessRuleset.StandardChessRuleset;

import java.util.ArrayList;
import java.util.List;

class StandardChessRulesetTest {

    Ruleset ruleset;
    Player playerW;
    Player playerB;
    Board board;
    List<Square> squares;
    List<Square> squaresExpected;

    @BeforeEach
    void setUp() {
        ruleset = new StandardChessRuleset();
        playerW = new Player("pw", PlayerColor.WHITE);
        playerB = new Player("pb", PlayerColor.BLACK);
        board = new Board(ruleset.getStartBoard(playerW, playerB));
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
        squaresExpected = new ArrayList<>();
        squares = ruleset.getLegalSquares(board.getSquare(0, 0), board, new ArrayList<>(), playerW, playerB);
        assertTrue(squares.isEmpty());

        squares = ruleset.getLegalSquares(board.getSquare(1, 0), board, new ArrayList<>(), playerW, playerB);
        assertNotNull(squares);
        squaresExpected.add(board.getSquare(0, 2));
        squaresExpected.add(board.getSquare(2, 2));
        System.out.println(squares);
        System.out.println(squaresExpected);
        assertTrue(compareLists(squares, squaresExpected));
    }

    @Test
    void testGetBoard() {

    }

    private boolean compareLists(List<Square> squares, List<Square> squaresExpected) {
        if (squares.isEmpty() && squaresExpected.isEmpty()) {
            return true;
        }
        if (squares.isEmpty() || squaresExpected.isEmpty()) {
            return false;
        }
        for (Square squareExpected : squaresExpected) {
            Square tempSquare = null;
            boolean squaresFound = false;
            for (Square square : squares) {
                if (square.equals(squareExpected)) {
                    tempSquare = square;
                }
            }
            if (tempSquare == null) {
                return false;
            }
            squares.remove(tempSquare);
        }
        return squares.isEmpty();
    }
}