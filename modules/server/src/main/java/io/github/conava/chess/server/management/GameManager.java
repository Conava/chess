package io.github.conava.chess.server.management;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages the lifecycle of active game sessions on the chess server.
 *
 * <p>{@code GameManager} owns the three pieces of shared game state that were
 * previously scattered across {@link io.github.conava.chess.server.Server}:
 * <ul>
 *   <li>a {@link Semaphore} that limits the number of concurrently active games;</li>
 *   <li>an {@link AtomicInteger} that generates monotonically increasing game IDs; and</li>
 *   <li>a {@link ConcurrentHashMap} that maps each live game ID to its
 *       {@link GameInstance}.</li>
 * </ul>
 *
 * <p>All methods on this class are thread-safe: the semaphore and atomic counter are
 * inherently thread-safe, and the map is a {@link ConcurrentHashMap}.
 *
 * @since 0.9
 */
public class GameManager {

    private final Semaphore gameSemaphore;
    private final AtomicInteger gameIdCounter;
    private final ConcurrentHashMap<Integer, GameInstance> gamesMap;

    /**
     * Constructs a {@code GameManager} with the given maximum number of concurrent games.
     *
     * @param maxGames the maximum number of game slots; must be positive
     */
    public GameManager(int maxGames) {
        this.gameSemaphore = new Semaphore(maxGames);
        this.gameIdCounter = new AtomicInteger(0);
        this.gamesMap = new ConcurrentHashMap<>();
    }

    /**
     * Attempts to acquire a game slot without blocking.
     *
     * <p>Returns {@code true} if a permit was acquired (i.e., a new game may be
     * created), or {@code false} if the server has reached its game limit.</p>
     *
     * @return {@code true} if the slot was acquired; {@code false} if the server is full
     */
    public boolean tryAcquireGameSlot() {
        return gameSemaphore.tryAcquire();
    }

    /**
     * Releases a previously acquired game slot back to the pool.
     *
     * <p>Must be called exactly once for every successful {@link #tryAcquireGameSlot()}
     * call, typically when a game ends or a player disconnects.</p>
     */
    public void releaseGameSlot() {
        gameSemaphore.release();
    }

    /**
     * Returns the next unique game ID by incrementing the internal counter.
     *
     * <p>The first call returns {@code 1}; each subsequent call returns the previous
     * value plus one.</p>
     *
     * @return a positive, monotonically increasing game identifier
     */
    public int nextGameId() {
        return gameIdCounter.incrementAndGet();
    }

    /**
     * Registers a {@link GameInstance} under the given ID.
     *
     * @param id   the unique game identifier; must be positive
     * @param game the game instance to register; must not be {@code null}
     */
    public void addGame(int id, GameInstance game) {
        gamesMap.put(id, game);
    }

    /**
     * Removes the {@link GameInstance} associated with the given ID.
     *
     * <p>This method is a no-op if no entry exists for {@code id}.</p>
     *
     * @param id the unique game identifier of the game to remove
     */
    public void removeGame(int id) {
        gamesMap.remove(id);
    }

    /**
     * Returns the {@link GameInstance} associated with the given ID, or {@code null} if
     * no such game is currently active.
     *
     * @param id the unique game identifier to look up
     * @return the corresponding {@link GameInstance}, or {@code null} if not found
     */
    public GameInstance getGame(int id) {
        return gamesMap.get(id);
    }

    /**
     * Returns an unmodifiable view of the active-games map.
     *
     * <p>The returned map reflects live changes to the underlying
     * {@link ConcurrentHashMap} but does not support mutation — any attempt to call
     * {@code put}, {@code remove}, or other mutating operations will throw
     * {@link UnsupportedOperationException}. Use {@link #addGame} and
     * {@link #removeGame} to modify the map.</p>
     *
     * @return an unmodifiable view of the game ID to {@link GameInstance} map
     */
    public Map<Integer, GameInstance> getActiveGames() {
        return Collections.unmodifiableMap(gamesMap);
    }
}
