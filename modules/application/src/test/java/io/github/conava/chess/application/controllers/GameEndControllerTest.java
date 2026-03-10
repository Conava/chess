package io.github.conava.chess.application.controllers;

import io.github.conava.chess.core.logic.game.GameState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameEndControllerTest {

    private static final String WHITE = "Alice";
    private static final String BLACK = "Bob";

    @Test
    void resolveWinnerName_returnsWhiteName_forWhiteWonByCheckmate() {
        assertEquals(WHITE, GameEndController.resolveWinnerName(GameState.WHITE_WON_BY_CHECKMATE, WHITE, BLACK));
    }

    @Test
    void resolveWinnerName_returnsWhiteName_forWhiteWonByResignation() {
        assertEquals(WHITE, GameEndController.resolveWinnerName(GameState.WHITE_WON_BY_RESIGNATION, WHITE, BLACK));
    }

    @Test
    void resolveWinnerName_returnsWhiteName_forWhiteWonByTimeout() {
        assertEquals(WHITE, GameEndController.resolveWinnerName(GameState.WHITE_WON_BY_TIMEOUT, WHITE, BLACK));
    }

    @Test
    void resolveWinnerName_returnsBlackName_forBlackWonByCheckmate() {
        assertEquals(BLACK, GameEndController.resolveWinnerName(GameState.BLACK_WON_BY_CHECKMATE, WHITE, BLACK));
    }

    @Test
    void resolveWinnerName_returnsBlackName_forBlackWonByResignation() {
        assertEquals(BLACK, GameEndController.resolveWinnerName(GameState.BLACK_WON_BY_RESIGNATION, WHITE, BLACK));
    }

    @Test
    void resolveWinnerName_returnsBlackName_forBlackWonByTimeout() {
        assertEquals(BLACK, GameEndController.resolveWinnerName(GameState.BLACK_WON_BY_TIMEOUT, WHITE, BLACK));
    }
}
