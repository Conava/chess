package io.github.conava.chess.server.management;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

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
 * <p>In addition, {@code GameManager} maintains a <em>join-code index</em> — a
 * secondary lookup table that maps human-readable, user-shareable join codes to game
 * instances. This is separate from the integer-keyed primary map so that the internal
 * identifier space (used for logging, DB references, and stats) is not disturbed.
 *
 * <p>Join codes are 12-character uppercase alphanumeric strings (characters {@code [A-Z0-9]})
 * generated with {@link SecureRandom}. For display they are formatted as
 * {@code XXXX-XXXX-XXXX}, but the dashes are stripped before any lookup.
 *
 * <p>A {@link ScheduledExecutorService} running a single daemon thread periodically
 * sweeps the join-code map and removes entries whose creation timestamp is older than
 * {@link #joinCodeExpirySeconds}. This acts as a backstop — the primary expiry path is
 * an inline check in {@link #lookupByJoinCode(String)}.
 *
 * <p>All methods on this class are thread-safe: the semaphore and atomic counter are
 * inherently thread-safe, and all maps are {@link ConcurrentHashMap}s.
 *
 * @since 0.9
 */
public class GameManager {

    private static final Logger LOG = Logger.getLogger(GameManager.class.getName());

    /**
     * Shared cryptographically-strong random source for join code generation.
     * {@link java.security.SecureRandom} is thread-safe after construction.
     */
    private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom();

    /** Characters available for join code generation — uppercase letters and digits. */
    private static final char[] JOIN_CODE_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

    /** Length of raw join codes (without dashes). */
    private static final int JOIN_CODE_LENGTH = 12;

    /** Default expiry in seconds when no value is provided. */
    private static final int DEFAULT_EXPIRY_SECONDS = 600;

    // ---- Primary game management ----

    private final Semaphore gameSemaphore;
    private final AtomicInteger gameIdCounter;
    private final ConcurrentHashMap<Integer, GameInstance> gamesMap;

    // ---- Join-code index ----

    /**
     * Maps raw (no-dash, uppercase) join codes to their corresponding {@link GameInstance}.
     * Entries are added by {@link #registerJoinCode} and removed by {@link #removeJoinCode}
     * or during cleanup.
     */
    private final ConcurrentHashMap<String, GameInstance> joinCodeMap;

    /**
     * Tracks the {@link Instant} at which each join code was created, keyed by the raw
     * uppercase code. Used by the cleanup thread to detect expired codes.
     */
    private final ConcurrentHashMap<String, Instant> joinCodeCreationTimes;

    /**
     * Seconds after which an unused join code is considered expired.
     * Configurable via {@code ServerConfig.getJoinCodeExpirySeconds()}.
     */
    private final int joinCodeExpirySeconds;

    /** Background thread that periodically sweeps expired join codes. */
    private final ScheduledExecutorService codeCleanupScheduler;

    // ========================================================================
    // Constructors
    // ========================================================================

    /**
     * Constructs a {@code GameManager} with the given maximum number of concurrent games
     * and the default join-code expiry of {@value #DEFAULT_EXPIRY_SECONDS} seconds.
     *
     * @param maxGames the maximum number of game slots; must be positive
     * @throws IllegalArgumentException if {@code maxGames} is not positive
     */
    public GameManager(int maxGames) {
        this(maxGames, DEFAULT_EXPIRY_SECONDS);
    }

    /**
     * Constructs a {@code GameManager} with the given maximum number of concurrent games
     * and a custom join-code expiry duration.
     *
     * @param maxGames             the maximum number of game slots; must be positive
     * @param joinCodeExpirySeconds seconds until an unused join code expires; must be positive
     * @throws IllegalArgumentException if either argument is not positive
     */
    public GameManager(int maxGames, int joinCodeExpirySeconds) {
        if (maxGames <= 0) {
            throw new IllegalArgumentException("maxGames must be positive, got: " + maxGames);
        }
        if (joinCodeExpirySeconds <= 0) {
            throw new IllegalArgumentException("joinCodeExpirySeconds must be positive, got: " + joinCodeExpirySeconds);
        }

        this.gameSemaphore          = new Semaphore(maxGames);
        this.gameIdCounter          = new AtomicInteger(0);
        this.gamesMap               = new ConcurrentHashMap<>();
        this.joinCodeMap            = new ConcurrentHashMap<>();
        this.joinCodeCreationTimes  = new ConcurrentHashMap<>();
        this.joinCodeExpirySeconds  = joinCodeExpirySeconds;

        // Daemon thread so it does not prevent JVM shutdown.
        this.codeCleanupScheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread t = new Thread(runnable, "join-code-cleanup");
            t.setDaemon(true);
            return t;
        });
        codeCleanupScheduler.scheduleAtFixedRate(this::cleanupExpiredCodes, 30, 30, TimeUnit.SECONDS);
    }

    // ========================================================================
    // Primary game-slot management
    // ========================================================================

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

    // ========================================================================
    // Join-code management
    // ========================================================================

    /**
     * Generates a cryptographically random 12-character join code using characters from
     * {@code [A-Z0-9]}.
     *
     * <p>The method loops until a code that does not already exist in {@link #joinCodeMap}
     * is found (collision avoidance). With a 36^12 ≈ 4.7 × 10^18 keyspace this loop
     * will almost never execute more than once in practice.</p>
     *
     * <p>The returned code contains no dashes. For display purposes, insert dashes at
     * positions 4 and 8 to produce the canonical {@code XXXX-XXXX-XXXX} format.</p>
     *
     * @return a 12-character uppercase alphanumeric join code; never {@code null}
     */
    public String generateJoinCode() {
        String code;
        do {
            char[] chars = new char[JOIN_CODE_LENGTH];
            for (int i = 0; i < JOIN_CODE_LENGTH; i++) {
                chars[i] = JOIN_CODE_ALPHABET[SECURE_RANDOM.nextInt(JOIN_CODE_ALPHABET.length)];
            }
            code = new String(chars);
        } while (joinCodeMap.containsKey(code));
        return code;
    }

    /**
     * Associates the given join code with a {@link GameInstance} and records its
     * creation timestamp for expiry tracking.
     *
     * <p>The code must be a raw (no-dash, uppercase) string as returned by
     * {@link #generateJoinCode()}.</p>
     *
     * @param code the raw 12-character join code; must not be {@code null}
     * @param game the game instance waiting for a second player; must not be {@code null}
     */
    public void registerJoinCode(String code, GameInstance game) {
        joinCodeMap.put(code, game);
        joinCodeCreationTimes.put(code, Instant.now());
    }

    /**
     * Looks up a {@link GameInstance} by join code, normalizing the input first.
     *
     * <p>Normalization converts the input to uppercase and strips all dash ({@code -})
     * characters, so users may enter the code in any case and with or without the
     * display dashes (e.g., {@code "abcd-1234-efgh"} finds the same game as
     * {@code "ABCD1234EFGH"}).</p>
     *
     * <p>Returns {@code null} if the normalized code is not found in the join-code map
     * or if the code has expired. An inline expiry check is performed on every lookup:
     * if the code's creation timestamp is missing or older than {@link #joinCodeExpirySeconds},
     * the entry is removed immediately and {@code null} is returned. The background cleanup
     * thread serves as a fallback sweep for codes that are never looked up after expiry.</p>
     *
     * @param code the join code provided by the user; may include dashes and any case
     * @return the matching {@link GameInstance}, or {@code null} if not found or expired
     */
    public GameInstance lookupByJoinCode(String code) {
        if (code == null) {
            return null;
        }
        String normalized = code.toUpperCase().replace("-", "");
        Instant createdAt = joinCodeCreationTimes.get(normalized);
        if (createdAt == null || createdAt.plusSeconds(joinCodeExpirySeconds).isBefore(Instant.now())) {
            joinCodeMap.remove(normalized);
            joinCodeCreationTimes.remove(normalized);
            return null;
        }
        return joinCodeMap.get(normalized);
    }

    /**
     * Removes the join code from both the code-to-game map and the creation-time map.
     *
     * <p>Callers should invoke this method after a second player successfully joins
     * (the code is "consumed" and should not be reused) or when the game is cancelled.</p>
     *
     * @param code the raw (or user-formatted) join code to remove; normalization is applied
     */
    public void removeJoinCode(String code) {
        if (code == null) {
            return;
        }
        String normalized = code.toUpperCase().replace("-", "");
        joinCodeMap.remove(normalized);
        joinCodeCreationTimes.remove(normalized);
    }

    /**
     * Atomically validates, removes, and returns the {@link GameInstance} associated with
     * the given join code in a single operation, eliminating any TOCTOU race between a
     * lookup and a subsequent remove.
     *
     * <p>The same normalization applied by {@link #lookupByJoinCode(String)} is used here:
     * the input is converted to uppercase and all dash ({@code -}) characters are stripped
     * before the lookup. An inline expiry check is performed: if the creation timestamp is
     * missing or older than {@link #joinCodeExpirySeconds}, both maps are cleaned up and
     * {@code null} is returned without touching {@link #joinCodeMap} via the atomic remove.</p>
     *
     * <p>When the code is valid and unexpired, {@link ConcurrentHashMap#remove(Object)}
     * is called on {@link #joinCodeMap}. Because {@code ConcurrentHashMap.remove} is
     * itself atomic, at most one concurrent caller will receive a non-{@code null} return
     * value; all others will receive {@code null} and must treat the code as already
     * consumed. This design prevents two simultaneous {@code JOIN_GAME} requests from
     * both succeeding on the same join code.</p>
     *
     * @param code the join code provided by the user; may include dashes and any case
     * @return the matching {@link GameInstance} if the code was found, unexpired, and had
     *         not yet been consumed by another caller; {@code null} otherwise
     */
    public GameInstance removeAndGetByJoinCode(String code) {
        if (code == null) {
            return null;
        }
        String normalized = code.toUpperCase().replace("-", "");

        // Inline expiry check — mirrors the logic in lookupByJoinCode.
        Instant createdAt = joinCodeCreationTimes.get(normalized);
        if (createdAt == null || createdAt.plusSeconds(joinCodeExpirySeconds).isBefore(Instant.now())) {
            // Code is absent or expired; clean up both maps and signal not found.
            joinCodeMap.remove(normalized);
            joinCodeCreationTimes.remove(normalized);
            return null;
        }

        // Atomically remove and return the game instance.  Only one concurrent caller
        // will receive a non-null value; all others see null (code already consumed).
        GameInstance game = joinCodeMap.remove(normalized);
        if (game != null) {
            joinCodeCreationTimes.remove(normalized);
        }
        return game;
    }

    /**
     * Sweeps the join-code maps and removes entries whose creation timestamp is older
     * than {@link #joinCodeExpirySeconds}.
     *
     * <p>This method is invoked by the {@link #codeCleanupScheduler} every 30 seconds.
     * It acts as a fallback — the primary expiry path is the inline check in
     * {@link #lookupByJoinCode(String)}. The slot released here corresponds only to
     * codes that were never consumed via {@link #removeJoinCode} or the inline path.</p>
     *
     * <p>The game slot is released only when the cleanup thread is the one that actually
     * removes the entry from {@link #joinCodeMap}. If a concurrent caller has already
     * removed the entry (via {@link #removeJoinCode} or the inline expiry path),
     * {@link ConcurrentHashMap#remove} returns {@code null} and no slot is released,
     * avoiding a double-release that would corrupt the semaphore.</p>
     *
     * <p>Each expired code is logged at INFO level for operator visibility.</p>
     */
    private void cleanupExpiredCodes() {
        Instant cutoff = Instant.now().minusSeconds(joinCodeExpirySeconds);
        for (Map.Entry<String, Instant> entry : joinCodeCreationTimes.entrySet()) {
            if (entry.getValue().isBefore(cutoff)) {
                String expiredCode = entry.getKey();
                if (joinCodeMap.remove(expiredCode) != null) {
                    releaseGameSlot();
                    LOG.info("Join code expired and removed: " + expiredCode);
                }
                joinCodeCreationTimes.remove(expiredCode);
            }
        }
    }

    /**
     * Shuts down the background join-code cleanup scheduler.
     *
     * <p>Should be called from {@code Server.stopServer()} during graceful shutdown so
     * that no daemon threads outlive the server's logical lifecycle. The method does not
     * wait for in-progress cleanup tasks to finish.</p>
     */
    public void shutdown() {
        codeCleanupScheduler.shutdown();
    }
}
