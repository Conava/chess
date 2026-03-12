package io.github.conava.chess.server.management;

/**
 * Holds per-connection authentication and game context for a single connected client.
 *
 * <p>A {@code PlayerSession} is created when a client connects and updated as the
 * client authenticates and joins games. It is owned exclusively by the
 * {@link ClientHandler} that manages that client's TCP socket.</p>
 */
public class PlayerSession {

    private int userId;
    private String username;
    private String authToken;
    private int activeGameId;

    /**
     * Constructs an unauthenticated session with no active game.
     */
    public PlayerSession() {
        this.userId = 0;
        this.username = null;
        this.authToken = null;
        this.activeGameId = -1;
    }

    /**
     * Returns {@code true} if this session has a valid authenticated identity.
     *
     * @return {@code true} when {@code userId > 0} and {@code authToken} is non-null
     */
    public boolean isAuthenticated() {
        return userId > 0 && authToken != null;
    }

    /**
     * Resets the session to its initial unauthenticated, out-of-game state.
     */
    public void clear() {
        this.userId = 0;
        this.username = null;
        this.authToken = null;
        this.activeGameId = -1;
    }

    // ── Getters and setters ────────────────────────────────────────────────────

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAuthToken() { return authToken; }
    public void setAuthToken(String authToken) { this.authToken = authToken; }

    public int getActiveGameId() { return activeGameId; }
    public void setActiveGameId(int activeGameId) { this.activeGameId = activeGameId; }
}
