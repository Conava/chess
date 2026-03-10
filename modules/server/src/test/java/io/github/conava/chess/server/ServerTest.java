package io.github.conava.chess.server;

import io.github.conava.chess.server.config.ServerConfig;
import io.github.conava.chess.server.management.GameInstance;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the instance-based {@link Server} class.
 *
 * <p>The {@code start()} method blocks on a server socket accept loop and
 * therefore cannot be unit-tested without a full integration harness. These tests
 * focus on the observable state of a freshly constructed {@code Server} instance
 * and on the independence of multiple instances (no shared static state).</p>
 *
 * <p>Covered behaviours:
 * <ul>
 *   <li>Constructor creates a Server with a non-null games map and game manager</li>
 *   <li>Initial games map is empty</li>
 *   <li>Two Server instances do not share the same games map or game manager</li>
 *   <li>Mutating one Server's state does not affect another Server's state</li>
 * </ul>
 * </p>
 *
 * <p>Not covered here (require blocking network I/O):
 * <ul>
 *   <li>{@code start()} accept loop and console daemon thread</li>
 * </ul>
 * </p>
 */
class ServerTest {

    private static final int MAX_GAMES = 40;

    private static Server createServer() {
        Properties props = new Properties();
        props.setProperty("max_games", String.valueOf(MAX_GAMES));
        return new Server(new ServerConfig(props));
    }

    // ---- Constructor initialises all fields ----

    @Test
    void server_constructor_gamesListIsNonNull() {
        Server server = createServer();
        assertNotNull(server.getGamesList(), "getGamesList() must return a non-null map immediately after construction");
    }

    @Test
    void server_constructor_gamesListIsEmpty() {
        Server server = createServer();
        assertTrue(server.getGamesList().isEmpty(), "getGamesList() must return an empty map immediately after construction");
    }

    @Test
    void server_constructor_gameManagerIsNonNull() {
        Server server = createServer();
        assertNotNull(server.getGameManager(), "getGameManager() must return a non-null GameManager immediately after construction");
    }

    // ---- Instance-based design: no shared static state ----

    @Test
    void server_instanceBased_separateGamesLists() {
        Server server1 = createServer();
        Server server2 = createServer();

        assertNotSame(server1.getGamesList(), server2.getGamesList(),
                "Two Server instances must own separate games maps (not the same object)");
    }

    @Test
    void server_instanceBased_separateGameManagers() {
        Server server1 = createServer();
        Server server2 = createServer();

        assertNotSame(server1.getGameManager(), server2.getGameManager(),
                "Two Server instances must own separate GameManager objects");
    }

    @Test
    void server_instanceBased_mutatingOneDoesNotAffectOther() {
        Server server1 = createServer();
        Server server2 = createServer();

        // Mutate server1's games map.
        server1.addGame(1, new GameInstance(1, io.github.conava.chess.core.logic.ruleset.RulesetOptions.STANDARD));

        assertTrue(server2.getGamesList().isEmpty(),
                "Adding to server1's games map must not affect server2's games map");
    }

    @Test
    void server_instanceBased_semaphorePermitsIndependent() {
        Server server1 = createServer();
        Server server2 = createServer();

        // Acquire a slot from server1's game manager; server2 must still have MAX_GAMES.
        server1.getGameManager().tryAcquireGameSlot();

        // We can't directly query available permits, but we can verify that server2
        // can still acquire MAX_GAMES slots by filling then failing.
        for (int i = 0; i < MAX_GAMES; i++) {
            assertTrue(server2.getGameManager().tryAcquireGameSlot(),
                    "server2 must have " + MAX_GAMES + " independent slots");
        }
        assertFalse(server2.getGameManager().tryAcquireGameSlot(),
                "server2 must be full after " + MAX_GAMES + " acquires");
    }
}
