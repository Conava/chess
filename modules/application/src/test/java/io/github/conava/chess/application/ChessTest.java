package io.github.conava.chess.application;

import io.github.conava.chess.application.tasks.ExecuteMove;
import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.pieces.Pieces;
import io.github.conava.chess.core.logic.observer.GameObserver;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class ChessTest {
    Chess chess;

    @BeforeEach
    void setUp() {
        chess = new Chess(false);
    }

    @Test
    void switchToGame() {
    }

    @Test
    void switchToMenu() {
    }

    @Test
    void openSettingsWindow() {
    }

    @Test
    void getGame() {
    }

    @Test
    void startGame() {
    }

    @Test
    void endGame() {
    }

    @Test
    void getCurrentPlayer() {
    }

    @Test
    void getPlayerWhite() {
    }

    @Test
    void getPlayerBlack() {
    }

    @Test
    void getPieceAt() {
    }

    @Test
    void getLegalSquares() {
    }

    @Test
    void getMoveList() {
    }

    @Test
    void movePiece() {
    }

    // ---- Task 5: getJoinCode() returns null for offline games ----

    @Test
    void getJoinCode_offlineGame_returnsNull() {
        chess.startGame(false, RulesetOptions.STANDARD, "Alice", "Bob", Collections.emptyMap());
        assertNull(chess.getJoinCode(),
                "getJoinCode() must return null for an offline game without throwing ClassCastException");
    }

    // ---- Task 5: endGame() is null-safe when no game is active ----

    @Test
    void endGame_noActiveGame_doesNotThrow() {
        // No startGame() has been called; chess.game is null
        assertDoesNotThrow(chess::endGame,
                "endGame() must not throw when no game is active (game field is null)");
    }

    // ---- Null-guard tests: query methods return safe defaults when no game is active ----

    @Test
    void testFacadeQueryMethodsReturnNullWhenNoGame() {
        // chess.game is null — no startGame() called
        assertNull(chess.getState(),
                "getState() must return null when no game is active");
        assertNull(chess.getBoard(),
                "getBoard() must return null when no game is active");
        assertNull(chess.getCurrentPlayer(),
                "getCurrentPlayer() must return null when no game is active");
        assertNull(chess.getPlayerWhite(),
                "getPlayerWhite() must return null when no game is active");
        assertNull(chess.getPlayerBlack(),
                "getPlayerBlack() must return null when no game is active");
        assertNull(chess.getPieceAt(new Square(0, 0)),
                "getPieceAt() must return null when no game is active");
        assertTrue(chess.getLegalSquares(new Square(0, 0)).isEmpty(),
                "getLegalSquares() must return an empty list when no game is active");
        assertTrue(chess.getMoveList().isEmpty(),
                "getMoveList() must return an empty list when no game is active");
    }

    // ---- Null-guard tests: action methods throw IllegalStateException when no game is active ----

    @Test
    void testFacadeActionMethodsThrowWhenNoGame() {
        // movePiece
        assertThrows(IllegalStateException.class,
                () -> chess.movePiece(new Square(1, 0), new Square(2, 0)),
                "movePiece() must throw IllegalStateException when no game is active");

        // promoteMove
        assertThrows(IllegalStateException.class,
                () -> chess.promoteMove(new Square(6, 0), new Square(7, 0), Pieces.QUEEN),
                "promoteMove() must throw IllegalStateException when no game is active");

        // addObserver
        GameObserver stubObserver = () -> {};
        assertThrows(IllegalStateException.class,
                () -> chess.addObserver(stubObserver),
                "addObserver() must throw IllegalStateException when no game is active");

        // removeObserver
        assertThrows(IllegalStateException.class,
                () -> chess.removeObserver(stubObserver),
                "removeObserver() must throw IllegalStateException when no game is active");
    }

    // ---- Reflection test: ExecuteMove constructor has exactly (Chess, Square, Square, Pieces) ----

    @Test
    void testExecuteMoveConstructorSignature() {
        Constructor<?>[] constructors = ExecuteMove.class.getDeclaredConstructors();

        // There must be exactly one constructor
        assertEquals(1, constructors.length,
                "ExecuteMove must have exactly one constructor");

        Constructor<?> ctor = constructors[0];
        Class<?>[] params = ctor.getParameterTypes();

        // The constructor must have exactly 4 parameters
        assertEquals(4, params.length,
                "ExecuteMove constructor must have exactly 4 parameters: (Chess, Square, Square, Pieces)");

        // Verify the parameter types in order
        assertEquals(Chess.class, params[0],
                "First parameter of ExecuteMove constructor must be Chess");
        assertEquals(Square.class, params[1],
                "Second parameter of ExecuteMove constructor must be Square");
        assertEquals(Square.class, params[2],
                "Third parameter of ExecuteMove constructor must be Square");
        assertEquals(Pieces.class, params[3],
                "Fourth parameter of ExecuteMove constructor must be Pieces");

        // Verify no constructor has a ChessGame parameter
        boolean hasChessGameParam = false;
        for (Constructor<?> c : constructors) {
            for (Class<?> paramType : c.getParameterTypes()) {
                if (paramType.getSimpleName().equals("ChessGame")) {
                    hasChessGameParam = true;
                    break;
                }
            }
        }
        assertFalse(hasChessGameParam,
                "No ExecuteMove constructor must accept a ChessGame parameter");
    }
}
