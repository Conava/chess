package io.github.conava.chess.application;

import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
}