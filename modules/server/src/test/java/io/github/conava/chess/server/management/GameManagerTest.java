package io.github.conava.chess.server.management;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
 *   <li>{@code generateJoinCode} produces a 12-character uppercase alphanumeric string</li>
 *   <li>{@code generateJoinCode} produces unique codes on repeated calls</li>
 *   <li>{@code registerJoinCode} and {@code lookupByJoinCode} store and retrieve a game by code</li>
 *   <li>{@code lookupByJoinCode} is case-insensitive</li>
 *   <li>{@code lookupByJoinCode} strips dashes from input</li>
 *   <li>{@code lookupByJoinCode} returns {@code null} for unknown codes</li>
 *   <li>{@code removeJoinCode} removes entries from both internal maps</li>
 *   <li>{@code cleanupExpiredCodes} removes entries older than the expiry threshold</li>
 *   <li>{@code cleanupExpiredCodes} does not remove entries that are still active</li>
 *   <li>{@code shutdown} does not throw when called on an active manager</li>
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

    // ========================================================================
    // generateJoinCode
    // ========================================================================

    /**
     * A generated join code must be exactly 12 characters, all uppercase alphanumeric ([A-Z0-9]).
     */
    @Test
    void generateJoinCode_returns12CharAlphanumericString() {
        GameManager manager = new GameManager(10);

        String code = manager.generateJoinCode();

        assertNotNull(code, "generateJoinCode must not return null");
        assertEquals(12, code.length(), "Join code must be exactly 12 characters");
        assertTrue(code.matches("[A-Z0-9]{12}"),
                "Join code must consist only of uppercase letters and digits, but was: " + code);
    }

    /**
     * Generating 100 codes must produce all distinct values, and each code is registered
     * immediately after generation so that the collision-avoidance loop inside
     * {@code generateJoinCode} is exercised against a growing set of live codes.
     */
    @Test
    void generateJoinCode_returnsUniqueCodesOnRepeatedCalls() {
        GameManager manager = new GameManager(200);
        GameInstance game = new GameInstance(99, io.github.conava.chess.core.logic.ruleset.RulesetOptions.STANDARD);
        Set<String> codes = new HashSet<>();

        for (int i = 0; i < 100; i++) {
            String code = manager.generateJoinCode();
            manager.registerJoinCode(code, game);
            codes.add(code);
        }

        assertEquals(100, codes.size(), "All 100 generated join codes must be unique");
    }

    // ========================================================================
    // registerJoinCode / lookupByJoinCode
    // ========================================================================

    /**
     * A code registered via {@code registerJoinCode} must be retrievable by the same code.
     */
    @Test
    void registerAndLookupByJoinCode_returnsGame() {
        GameManager manager = new GameManager(10);
        GameInstance game = new GameInstance(1, io.github.conava.chess.core.logic.ruleset.RulesetOptions.STANDARD);
        String code = manager.generateJoinCode();

        manager.registerJoinCode(code, game);

        GameInstance found = manager.lookupByJoinCode(code);
        assertSame(game, found, "lookupByJoinCode must return the exact same GameInstance that was registered");
    }

    /**
     * {@code lookupByJoinCode} must be case-insensitive: a code registered in uppercase
     * must be found when looked up in lowercase.
     */
    @Test
    void lookupByJoinCode_caseInsensitive() {
        GameManager manager = new GameManager(10);
        GameInstance game = new GameInstance(2, io.github.conava.chess.core.logic.ruleset.RulesetOptions.STANDARD);
        // generateJoinCode always returns uppercase; we register that directly.
        String code = "ABCD1234EFGH";
        manager.registerJoinCode(code, game);

        GameInstance found = manager.lookupByJoinCode("abcd1234efgh");

        assertSame(game, found, "lookupByJoinCode must find the game regardless of input case");
    }

    /**
     * {@code lookupByJoinCode} must strip dashes from the input before the lookup.
     * Registering "ABCD1234EFGH" and looking up "ABCD-1234-EFGH" must succeed.
     */
    @Test
    void lookupByJoinCode_stripsDashes() {
        GameManager manager = new GameManager(10);
        GameInstance game = new GameInstance(3, io.github.conava.chess.core.logic.ruleset.RulesetOptions.STANDARD);
        manager.registerJoinCode("ABCD1234EFGH", game);

        GameInstance found = manager.lookupByJoinCode("ABCD-1234-EFGH");

        assertSame(game, found, "lookupByJoinCode must find the game when the lookup code contains dashes");
    }

    /**
     * Looking up a code that was never registered must return {@code null}.
     */
    @Test
    void lookupByJoinCode_returnsNullForUnknownCode() {
        GameManager manager = new GameManager(10);

        GameInstance found = manager.lookupByJoinCode("UNKNOWNXYZABC");

        assertNull(found, "lookupByJoinCode must return null for a code that was never registered");
    }

    // ========================================================================
    // removeJoinCode
    // ========================================================================

    /**
     * After removing a registered join code, {@code lookupByJoinCode} must return {@code null}.
     */
    @Test
    void removeJoinCode_removesFromBothMaps() {
        GameManager manager = new GameManager(10);
        GameInstance game = new GameInstance(4, io.github.conava.chess.core.logic.ruleset.RulesetOptions.STANDARD);
        String code = "ABCD1234EFGH";
        manager.registerJoinCode(code, game);

        assertNotNull(manager.lookupByJoinCode(code), "Game must be found before removal");

        manager.removeJoinCode(code);

        assertNull(manager.lookupByJoinCode(code),
                "lookupByJoinCode must return null after the code has been removed");
    }

    // ========================================================================
    // cleanupExpiredCodes
    // ========================================================================

    /**
     * After back-dating the creation timestamp of a registered code to beyond the expiry
     * threshold, running cleanup must remove it so that lookup returns {@code null}.
     *
     * <p>Uses reflection to manipulate the {@code joinCodeCreationTimes} map directly
     * so that the test does not need to sleep.</p>
     */
    @Test
    void cleanupExpiredCodes_removesExpiredEntries() throws Exception {
        // Use a very short expiry (1 second) so we can back-date easily.
        GameManager manager = new GameManager(10, 1);
        GameInstance game = new GameInstance(5, io.github.conava.chess.core.logic.ruleset.RulesetOptions.STANDARD);
        String code = "EXPIREDCODE1";
        manager.registerJoinCode(code, game);

        // Back-date the creation time to 10 seconds in the past via reflection.
        backdateJoinCodeCreationTime(manager, code, 10);

        // Invoke the cleanup method via reflection.
        invokeCleanupExpiredCodes(manager);

        assertNull(manager.lookupByJoinCode(code),
                "Expired join code must be removed after cleanupExpiredCodes runs");
    }

    /**
     * A freshly registered code must survive the cleanup because it is within the expiry window.
     */
    @Test
    void cleanupExpiredCodes_doesNotRemoveActiveEntries() throws Exception {
        GameManager manager = new GameManager(10, 600);
        GameInstance game = new GameInstance(6, io.github.conava.chess.core.logic.ruleset.RulesetOptions.STANDARD);
        String code = "ACTIVECODE12";
        manager.registerJoinCode(code, game);

        // Invoke cleanup immediately — the code was just registered, so it is fresh.
        invokeCleanupExpiredCodes(manager);

        assertNotNull(manager.lookupByJoinCode(code),
                "A recently registered join code must still be present after cleanupExpiredCodes runs");
    }

    // ========================================================================
    // removeAndGetByJoinCode
    // ========================================================================

    /**
     * The first caller of {@code removeAndGetByJoinCode} on a valid, unexpired code must
     * receive the associated {@link GameInstance}; a second caller with the same code must
     * receive {@code null} because the code was atomically consumed by the first call.
     *
     * <p>This test verifies the TOCTOU fix: two concurrent {@code JOIN_GAME} requests
     * cannot both succeed on the same join code.</p>
     */
    @Test
    void removeAndGetByJoinCode_firstCallerGetsGame_secondCallerGetsNull() {
        GameManager manager = new GameManager(10);
        GameInstance game = new GameInstance(10, io.github.conava.chess.core.logic.ruleset.RulesetOptions.STANDARD);
        String code = "ATOMICTEST12";
        manager.registerJoinCode(code, game);

        GameInstance first = manager.removeAndGetByJoinCode(code);
        GameInstance second = manager.removeAndGetByJoinCode(code);

        assertSame(game, first, "First caller must receive the registered GameInstance");
        assertNull(second, "Second caller must receive null — the code was already consumed");
    }

    /**
     * {@code removeAndGetByJoinCode} must return {@code null} when the code is expired
     * (creation time is back-dated beyond the expiry threshold).
     */
    @Test
    void removeAndGetByJoinCode_returnsNullForExpiredCode() throws Exception {
        GameManager manager = new GameManager(10, 1);
        GameInstance game = new GameInstance(11, io.github.conava.chess.core.logic.ruleset.RulesetOptions.STANDARD);
        String code = "EXPIREDATOM1";
        manager.registerJoinCode(code, game);

        // Back-date the creation time so the code appears expired.
        backdateJoinCodeCreationTime(manager, code, 10);

        GameInstance result = manager.removeAndGetByJoinCode(code);

        assertNull(result, "removeAndGetByJoinCode must return null for an expired code");
    }

    /**
     * {@code removeAndGetByJoinCode} must return {@code null} for a code that was never
     * registered.
     */
    @Test
    void removeAndGetByJoinCode_returnsNullForUnknownCode() {
        GameManager manager = new GameManager(10);

        GameInstance result = manager.removeAndGetByJoinCode("DOESNTEXIST1");

        assertNull(result, "removeAndGetByJoinCode must return null for an unknown code");
    }

    /**
     * {@code removeAndGetByJoinCode} must be case-insensitive and strip dashes, just like
     * {@code lookupByJoinCode}.
     */
    @Test
    void removeAndGetByJoinCode_normalizesInput() {
        GameManager manager = new GameManager(10);
        GameInstance game = new GameInstance(12, io.github.conava.chess.core.logic.ruleset.RulesetOptions.STANDARD);
        manager.registerJoinCode("ABCD1234EFGH", game);

        // Look up using lower-case, dash-formatted input.
        GameInstance result = manager.removeAndGetByJoinCode("abcd-1234-efgh");

        assertSame(game, result, "removeAndGetByJoinCode must normalize input before lookup");
    }

    // ========================================================================
    // shutdown
    // ========================================================================

    /**
     * Calling {@code shutdown()} must not throw an exception.
     */
    @Test
    void shutdown_doesNotThrow() {
        GameManager manager = new GameManager(10, 600);
        assertDoesNotThrow(manager::shutdown, "shutdown() must not throw any exception");
    }

    // ========================================================================
    // Reflection helpers
    // ========================================================================

    /**
     * Uses reflection to overwrite the creation timestamp for {@code code} in the
     * {@code joinCodeCreationTimes} map with a value {@code secondsAgo} seconds in the past.
     */
    @SuppressWarnings("unchecked")
    private static void backdateJoinCodeCreationTime(GameManager manager, String code, long secondsAgo)
            throws Exception {
        Field field = GameManager.class.getDeclaredField("joinCodeCreationTimes");
        field.setAccessible(true);
        ConcurrentHashMap<String, Instant> times = (ConcurrentHashMap<String, Instant>) field.get(manager);
        times.put(code, Instant.now().minusSeconds(secondsAgo));
    }

    /**
     * Uses reflection to invoke the private {@code cleanupExpiredCodes()} method.
     */
    private static void invokeCleanupExpiredCodes(GameManager manager) throws Exception {
        java.lang.reflect.Method method = GameManager.class.getDeclaredMethod("cleanupExpiredCodes");
        method.setAccessible(true);
        method.invoke(manager);
    }
}
