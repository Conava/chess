package io.github.conava.chess.application;

import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.logic.game.GameState;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChessTest {

    private Chess chess;

    @BeforeEach
    void setUp() {
        chess = new Chess();
        chess.startGame(false, RulesetOptions.STANDARD, "White", "Black", null);
    }

    @Test
    void getStateIsRunningAfterStart() {
        assertEquals(GameState.RUNNING, chess.getState());
    }

    @Test
    void getBoardIsNotNullAfterStart() {
        assertNotNull(chess.getBoard());
    }

    @Test
    void getCurrentPlayerIsNotNullAfterStart() {
        assertNotNull(chess.getCurrentPlayer());
    }

    @Test
    void getPlayerWhiteIsNotNull() {
        assertNotNull(chess.getPlayerWhite());
    }

    @Test
    void getPlayerBlackIsNotNull() {
        assertNotNull(chess.getPlayerBlack());
    }

    @Test
    void getLegalSquaresReturnsList() {
        // White pawns start at row 1 (player0 = white moves first)
        Square whitePawnE = new Square(1, 4);
        assertNotNull(chess.getLegalSquares(whitePawnE));
        assertFalse(chess.getLegalSquares(whitePawnE).isEmpty());
    }

    @Test
    void getMoveListIsEmptyBeforeAnyMove() {
        assertTrue(chess.getMoveList().isEmpty());
    }

    @Test
    void endGameSetsStateToNull() {
        chess.endGame();
        assertNull(chess.getState());
    }

    @Test
    void addObserverThrowsWhenNoGame() {
        chess.endGame();
        assertThrows(IllegalStateException.class, () -> chess.addObserver(() -> {
        }));
    }

    @Test
    void movePieceThrowsWhenNoGame() {
        chess.endGame();
        assertThrows(IllegalStateException.class, () -> chess.movePiece(new Square(6, 4), new Square(4, 4)));
    }

    @Test
    void getLocalPlayerColor_returnsNull_whenNoGameActive() {
        Chess freshChess = new Chess();
        assertNull(freshChess.getLocalPlayerColor(),
                "getLocalPlayerColor() must return null when no game has been started");
    }

    @Test
    void getLocalPlayerColor_returnsNull_forOfflineGame() {
        // chess was started offline in setUp()
        assertNull(chess.getLocalPlayerColor(),
                "getLocalPlayerColor() must return null for an offline game");
    }
}
