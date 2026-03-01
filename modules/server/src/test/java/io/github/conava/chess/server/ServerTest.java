package io.github.conava.chess.server;

import io.github.conava.chess.server.management.GameInstance;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the instance-based {@link Server} class.
 *
 * <p>The {@code start(int port)} method blocks on a server socket accept loop and
 * therefore cannot be unit-tested without a full integration harness. These tests
 * focus on the observable state of a freshly constructed {@code Server} instance
 * and on the independence of multiple instances (no shared static state).</p>
 *
 * <p>Covered behaviours:
 * <ul>
 *   <li>Constructor creates a Server with a non-null games map, semaphore, and ID counter</li>
 *   <li>Initial games map is empty</li>
 *   <li>Initial game ID counter is zero</li>
 *   <li>Semaphore is initialised with 40 permits (MAX_GAMES)</li>
 *   <li>Two Server instances do not share the same games map, semaphore, or ID counter</li>
 *   <li>Mutating one Server's state does not affect another Server's state</li>
 * </ul>
 * </p>
 *
 * <p>Not covered here (require blocking network I/O):
 * <ul>
 *   <li>{@code start(int port)} accept loop and console daemon thread</li>
 *   <li>Port validation (private static method; fallback behaviour is confirmed
 *       by Server starting on the default port when no args are supplied)</li>
 * </ul>
 * </p>
 */
class ServerTest {

    private static final int MAX_GAMES = 40;

    // ---- Constructor initialises all fields ----

    @Test
    void server_constructor_gamesListIsNonNull() {
        Server server = new Server();
        assertNotNull(server.getGamesList(),
                "getGamesList() must return a non-null map immediately after construction");
    }

    @Test
    void server_constructor_gamesListIsEmpty() {
        Server server = new Server();
        assertTrue(server.getGamesList().isEmpty(),
                "getGamesList() must return an empty map immediately after construction");
    }

    @Test
    void server_constructor_gameSemaphoreIsNonNull() {
        Server server = new Server();
        assertNotNull(server.getGameSemaphore(),
                "getGameSemaphore() must return a non-null Semaphore immediately after construction");
    }

    @Test
    void server_constructor_gameSemaphoreHasFortyPermits() {
        Server server = new Server();
        Semaphore semaphore = server.getGameSemaphore();
        assertEquals(MAX_GAMES, semaphore.availablePermits(),
                "The game semaphore must be initialised with " + MAX_GAMES + " permits");
    }

    @Test
    void server_constructor_gameIdCounterIsNonNull() {
        Server server = new Server();
        assertNotNull(server.getGameIdCounter(),
                "getGameIdCounter() must return a non-null AtomicInteger immediately after construction");
    }

    @Test
    void server_constructor_gameIdCounterInitiallyZero() {
        Server server = new Server();
        assertEquals(0, server.getGameIdCounter().get(),
                "The game ID counter must start at 0");
    }

    // ---- Instance-based design: no shared static state ----

    @Test
    void server_instanceBased_separateGamesLists() {
        Server server1 = new Server();
        Server server2 = new Server();

        assertNotSame(server1.getGamesList(), server2.getGamesList(),
                "Two Server instances must own separate games maps (not the same object)");
    }

    @Test
    void server_instanceBased_separateSemaphores() {
        Server server1 = new Server();
        Server server2 = new Server();

        assertNotSame(server1.getGameSemaphore(), server2.getGameSemaphore(),
                "Two Server instances must own separate Semaphore objects");
    }

    @Test
    void server_instanceBased_separateIdCounters() {
        Server server1 = new Server();
        Server server2 = new Server();

        assertNotSame(server1.getGameIdCounter(), server2.getGameIdCounter(),
                "Two Server instances must own separate AtomicInteger objects");
    }

    @Test
    void server_instanceBased_mutatingOneDoesNotAffectOther() {
        Server server1 = new Server();
        Server server2 = new Server();

        // Mutate server1's game ID counter and games map.
        server1.getGameIdCounter().incrementAndGet();
        server1.getGamesList().put(1, new GameInstance(1, io.github.conava.chess.core.logic.ruleset.RulesetOptions.STANDARD));

        assertEquals(0, server2.getGameIdCounter().get(),
                "Incrementing server1's ID counter must not affect server2's counter");
        assertTrue(server2.getGamesList().isEmpty(),
                "Adding to server1's games map must not affect server2's games map");
    }

    @Test
    void server_instanceBased_semaphorePermitsIndependent() {
        Server server1 = new Server();
        Server server2 = new Server();

        // Acquire a permit from server1; server2 must still have MAX_GAMES permits.
        server1.getGameSemaphore().tryAcquire();

        assertEquals(MAX_GAMES, server2.getGameSemaphore().availablePermits(),
                "Acquiring from server1's semaphore must not reduce server2's available permits");
        assertEquals(MAX_GAMES - 1, server1.getGameSemaphore().availablePermits(),
                "server1's semaphore must reflect the acquired permit");
    }
}
