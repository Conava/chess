package io.github.conava.chess.application.controllers;

import io.github.conava.chess.core.logic.game.GameState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link GameController#isGameEndState(GameState)} helper method.
 *
 * <p>These tests verify that only terminal game states (win and draw outcomes) trigger
 * the game-end overlay dialog. Non-terminal states like PAUSED and SAVED must not trigger
 * the overlay — doing so would incorrectly show "You Lost" when an opponent disconnects.</p>
 *
 * <p>These tests intentionally avoid initialising the JavaFX toolkit — {@code isGameEndState}
 * is a pure static helper that does not touch any JavaFX APIs.</p>
 */
class GameControllerUpdateTest {

    // ---- Bug 2a: PAUSED/SAVED must not be treated as game-end states ----

    /**
     * PAUSED state (opponent disconnected) must NOT trigger the game-end dialog.
     * The pre-fix bug: PAUSED falls to the default branch and shows "You Lost".
     */
    @Test
    void isGameEndState_returnsFalse_forPAUSED() {
        assertFalse(GameController.isGameEndState(GameState.PAUSED),
                "PAUSED must not be treated as a game-end state — it should show a 'waiting for reconnection' indicator instead");
    }

    /**
     * SAVED state must NOT trigger the game-end dialog.
     * A saved game should return to the menu, not show a loss.
     */
    @Test
    void isGameEndState_returnsFalse_forSAVED() {
        assertFalse(GameController.isGameEndState(GameState.SAVED),
                "SAVED must not be treated as a game-end state — it should navigate to the main menu instead");
    }

    /**
     * RUNNING state must NOT trigger the game-end dialog.
     * An in-progress game is normal play.
     */
    @Test
    void isGameEndState_returnsFalse_forRUNNING() {
        assertFalse(GameController.isGameEndState(GameState.RUNNING),
                "RUNNING must not be treated as a game-end state");
    }

    /**
     * NO_GAME state must NOT trigger the game-end dialog.
     */
    @Test
    void isGameEndState_returnsFalse_forNO_GAME() {
        assertFalse(GameController.isGameEndState(GameState.NO_GAME),
                "NO_GAME must not be treated as a game-end state");
    }

    /**
     * WAITING_FOR_PLAYER state must NOT trigger the game-end dialog.
     */
    @Test
    void isGameEndState_returnsFalse_forWAITING_FOR_PLAYER() {
        assertFalse(GameController.isGameEndState(GameState.WAITING_FOR_PLAYER),
                "WAITING_FOR_PLAYER must not be treated as a game-end state");
    }

    // ---- Terminal states that SHOULD trigger game-end dialog ----

    /**
     * WHITE_WON_BY_CHECKMATE is a terminal state — must trigger the game-end dialog.
     */
    @Test
    void isGameEndState_returnsTrue_forWhiteWonByCheckmate() {
        assertTrue(GameController.isGameEndState(GameState.WHITE_WON_BY_CHECKMATE),
                "WHITE_WON_BY_CHECKMATE must be treated as a game-end state");
    }

    /**
     * BLACK_WON_BY_CHECKMATE is a terminal state — must trigger the game-end dialog.
     */
    @Test
    void isGameEndState_returnsTrue_forBlackWonByCheckmate() {
        assertTrue(GameController.isGameEndState(GameState.BLACK_WON_BY_CHECKMATE),
                "BLACK_WON_BY_CHECKMATE must be treated as a game-end state");
    }

    /**
     * WHITE_WON_BY_RESIGNATION is a terminal state — must trigger the game-end dialog.
     */
    @Test
    void isGameEndState_returnsTrue_forWhiteWonByResignation() {
        assertTrue(GameController.isGameEndState(GameState.WHITE_WON_BY_RESIGNATION),
                "WHITE_WON_BY_RESIGNATION must be treated as a game-end state");
    }

    /**
     * BLACK_WON_BY_RESIGNATION is a terminal state — must trigger the game-end dialog.
     */
    @Test
    void isGameEndState_returnsTrue_forBlackWonByResignation() {
        assertTrue(GameController.isGameEndState(GameState.BLACK_WON_BY_RESIGNATION),
                "BLACK_WON_BY_RESIGNATION must be treated as a game-end state");
    }

    /**
     * WHITE_WON_BY_TIMEOUT is a terminal state — must trigger the game-end dialog.
     */
    @Test
    void isGameEndState_returnsTrue_forWhiteWonByTimeout() {
        assertTrue(GameController.isGameEndState(GameState.WHITE_WON_BY_TIMEOUT),
                "WHITE_WON_BY_TIMEOUT must be treated as a game-end state");
    }

    /**
     * BLACK_WON_BY_TIMEOUT is a terminal state — must trigger the game-end dialog.
     */
    @Test
    void isGameEndState_returnsTrue_forBlackWonByTimeout() {
        assertTrue(GameController.isGameEndState(GameState.BLACK_WON_BY_TIMEOUT),
                "BLACK_WON_BY_TIMEOUT must be treated as a game-end state");
    }

    /**
     * DRAW_BY_STALEMATE is a terminal draw state — must trigger the game-end dialog.
     */
    @Test
    void isGameEndState_returnsTrue_forDrawByStalemate() {
        assertTrue(GameController.isGameEndState(GameState.DRAW_BY_STALEMATE),
                "DRAW_BY_STALEMATE must be treated as a game-end state");
    }

    /**
     * DRAW_BY_INSUFFICIENT_MATERIAL is a terminal draw state — must trigger the game-end dialog.
     */
    @Test
    void isGameEndState_returnsTrue_forDrawByInsufficientMaterial() {
        assertTrue(GameController.isGameEndState(GameState.DRAW_BY_INSUFFICIENT_MATERIAL),
                "DRAW_BY_INSUFFICIENT_MATERIAL must be treated as a game-end state");
    }

    /**
     * DRAW_BY_THREEFOLD_REPETITION is a terminal draw state — must trigger the game-end dialog.
     */
    @Test
    void isGameEndState_returnsTrue_forDrawByThreefoldRepetition() {
        assertTrue(GameController.isGameEndState(GameState.DRAW_BY_THREEFOLD_REPETITION),
                "DRAW_BY_THREEFOLD_REPETITION must be treated as a game-end state");
    }

    /**
     * DRAW_BY_FIFTY_MOVE_RULE is a terminal draw state — must trigger the game-end dialog.
     */
    @Test
    void isGameEndState_returnsTrue_forDrawByFiftyMoveRule() {
        assertTrue(GameController.isGameEndState(GameState.DRAW_BY_FIFTY_MOVE_RULE),
                "DRAW_BY_FIFTY_MOVE_RULE must be treated as a game-end state");
    }
}
