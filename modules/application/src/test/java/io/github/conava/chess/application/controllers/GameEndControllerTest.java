package io.github.conava.chess.application.controllers;

import io.github.conava.chess.core.data.player.PlayerColor;
import io.github.conava.chess.core.logic.game.GameState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameEndControllerTest {

    private static final String WHITE = "Alice";
    private static final String BLACK = "Bob";

    // ---- resolveWinnerName regression tests (offline mode) ----

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

    // ---- isLocalPlayerWinner tests (Finding 3: online mode win/loss determination) ----

    /**
     * Local player is WHITE and WHITE won -- should show "You Win!".
     */
    @Test
    void isLocalPlayerWinner_returnsTrue_whenWhiteLocalAndWhiteWon() {
        assertTrue(GameEndController.isLocalPlayerWinner(GameState.WHITE_WON_BY_CHECKMATE, PlayerColor.WHITE),
                "White local player should be the winner when WHITE_WON_BY_CHECKMATE");
    }

    /**
     * Local player is WHITE and BLACK won -- should show "You Lose".
     * This was the bug: before the fix this always showed "You Win!".
     */
    @Test
    void isLocalPlayerWinner_returnsFalse_whenWhiteLocalAndBlackWon() {
        assertFalse(GameEndController.isLocalPlayerWinner(GameState.BLACK_WON_BY_CHECKMATE, PlayerColor.WHITE),
                "White local player should NOT be the winner when BLACK_WON_BY_CHECKMATE");
    }

    /**
     * Local player is BLACK and BLACK won -- should show "You Win!".
     */
    @Test
    void isLocalPlayerWinner_returnsTrue_whenBlackLocalAndBlackWon() {
        assertTrue(GameEndController.isLocalPlayerWinner(GameState.BLACK_WON_BY_CHECKMATE, PlayerColor.BLACK),
                "Black local player should be the winner when BLACK_WON_BY_CHECKMATE");
    }

    /**
     * Local player is BLACK and WHITE won -- should show "You Lose".
     */
    @Test
    void isLocalPlayerWinner_returnsFalse_whenBlackLocalAndWhiteWon() {
        assertFalse(GameEndController.isLocalPlayerWinner(GameState.WHITE_WON_BY_CHECKMATE, PlayerColor.BLACK),
                "Black local player should NOT be the winner when WHITE_WON_BY_CHECKMATE");
    }

    /**
     * Resignation variant: WHITE resigns means BLACK_WON_BY_RESIGNATION.
     * Local player is WHITE (the resigning side) -- should show "You Lose".
     */
    @Test
    void isLocalPlayerWinner_returnsFalse_whenWhiteLocalResigned() {
        assertFalse(GameEndController.isLocalPlayerWinner(GameState.BLACK_WON_BY_RESIGNATION, PlayerColor.WHITE),
                "White local player should NOT be winner when black won by resignation (white resigned)");
    }

    /**
     * Resignation variant: BLACK resigns means WHITE_WON_BY_RESIGNATION.
     * Local player is BLACK (the resigning side) -- should show "You Lose".
     */
    @Test
    void isLocalPlayerWinner_returnsFalse_whenBlackLocalResigned() {
        assertFalse(GameEndController.isLocalPlayerWinner(GameState.WHITE_WON_BY_RESIGNATION, PlayerColor.BLACK),
                "Black local player should NOT be winner when white won by resignation (black resigned)");
    }

    /**
     * Null localColor (e.g., offline game passed to online path accidentally) -- safe default is false.
     */
    @Test
    void isLocalPlayerWinner_returnsFalse_whenLocalColorIsNull() {
        assertFalse(GameEndController.isLocalPlayerWinner(GameState.WHITE_WON_BY_CHECKMATE, null),
                "isLocalPlayerWinner must return false when localColor is null (unknown side)");
    }

    /**
     * Timeout variant: local player WHITE wins by opponent timeout.
     */
    @Test
    void isLocalPlayerWinner_returnsTrue_whenWhiteLocalAndWhiteWonByTimeout() {
        assertTrue(GameEndController.isLocalPlayerWinner(GameState.WHITE_WON_BY_TIMEOUT, PlayerColor.WHITE),
                "White local player should be the winner when WHITE_WON_BY_TIMEOUT");
    }
}
