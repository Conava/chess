package io.github.conava.chess.core.logic.game;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GameStateTest {

    @Test
    void pausedStateExists() {
        GameState paused = GameState.valueOf("PAUSED");
        assertNotNull(paused);
        assertEquals("Game paused", paused.getMessage());
    }

    @Test
    void savedStateExists() {
        GameState saved = GameState.valueOf("SAVED");
        assertNotNull(saved);
        assertEquals("Game saved for later", saved.getMessage());
    }

    @Test
    void totalStateCount() {
        assertEquals(16, GameState.values().length);
    }
}
