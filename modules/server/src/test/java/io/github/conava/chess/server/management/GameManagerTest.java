package io.github.conava.chess.server.management;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GameManager}.
 *
 * <p>Covered behaviours:
 * <ul>
 *   <li>{@code tryAcquireGameSlot} respects the configured maximum number of game slots</li>
 *   <li>{@code releaseGameSlot} frees a slot so a subsequent acquire succeeds</li>
 *   <li>{@code nextGameId} increments sequentially starting from 1</li>
 *   <li>{@code addGame} / {@code getGame} store and retrieve a {@link GameInstance}</li>
 *   <li>{@code removeGame} removes the entry so {@code getGame} returns {@code null}</li>
 * </ul>
 * </p>
 */
class GameManagerTest {

    // ---- tryAcquireGameSlot ----

    /**
     * With maxGames=2, the first two acquires should succeed and the third should fail.
     */
    @Test
    void tryAcquireGameSlot_respectsLimit() {
        GameManager manager = new GameManager(2);

        assertTrue(manager.tryAcquireGameSlot(), "First acquire (slot 1 of 2) must succeed");
        assertTrue(manager.tryAcquireGameSlot(), "Second acquire (slot 2 of 2) must succeed");
        assertFalse(manager.tryAcquireGameSlot(), "Third acquire (no slots remaining) must return false");
    }

    // ---- releaseGameSlot ----

    /**
     * After acquiring all slots, releasing one should allow a new acquire to succeed.
     */
    @Test
    void releaseGameSlot_freesCapacity() {
        GameManager manager = new GameManager(1);

        assertTrue(manager.tryAcquireGameSlot(), "Acquire the only slot");
        assertFalse(manager.tryAcquireGameSlot(), "No slots remaining; must return false");

        manager.releaseGameSlot();

        assertTrue(manager.tryAcquireGameSlot(), "After release, acquire must succeed again");
    }

    // ---- nextGameId ----

    /**
     * Sequential calls must return 1, 2, 3 in order.
     */
    @Test
    void nextGameId_incrementsSequentially() {
        GameManager manager = new GameManager(10);

        assertEquals(1, manager.nextGameId(), "First nextGameId call must return 1");
        assertEquals(2, manager.nextGameId(), "Second nextGameId call must return 2");
        assertEquals(3, manager.nextGameId(), "Third nextGameId call must return 3");
    }

    // ---- addGame / getGame ----

    /**
     * A {@link GameInstance} added under a given ID should be retrievable by the same ID.
     */
    @Test
    void addAndGetGame() {
        GameManager manager = new GameManager(10);
        GameInstance game = new GameInstance(42, io.github.conava.chess.core.logic.ruleset.RulesetOptions.STANDARD);

        manager.addGame(42, game);

        assertNotNull(manager.getGame(42), "getGame must return the added GameInstance");
        assertSame(game, manager.getGame(42), "getGame must return the exact same object that was added");
    }

    // ---- removeGame ----

    /**
     * After adding and then removing a game, {@code getGame} must return {@code null}.
     */
    @Test
    void removeGame_removesEntry() {
        GameManager manager = new GameManager(10);
        GameInstance game = new GameInstance(7, io.github.conava.chess.core.logic.ruleset.RulesetOptions.STANDARD);

        manager.addGame(7, game);
        assertNotNull(manager.getGame(7), "getGame should not be null after addGame");

        manager.removeGame(7);

        assertNull(manager.getGame(7), "getGame must return null after removeGame");
    }

    // ---- getActiveGames ----

    /**
     * {@code getActiveGames} must return an unmodifiable view.
     */
    @Test
    void getActiveGames_returnsUnmodifiableView() {
        GameManager manager = new GameManager(10);
        GameInstance game = new GameInstance(1, io.github.conava.chess.core.logic.ruleset.RulesetOptions.STANDARD);
        manager.addGame(1, game);

        Map<Integer, GameInstance> view = manager.getActiveGames();

        assertEquals(1, view.size(), "Active games map must contain the added game");
        assertThrows(UnsupportedOperationException.class, () -> view.put(99, game),
                "getActiveGames must return an unmodifiable view");
    }
}
