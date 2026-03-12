package io.github.conava.chess.server.persistence;

import io.github.conava.chess.server.db.DatabaseManager;

import java.sql.*;
import java.util.Optional;

/**
 * Provides CRUD operations for the {@code auth_sessions} table.
 *
 * <p>All SQL is executed via {@link PreparedStatement} — no string concatenation
 * of caller-supplied data ever appears in a query string.</p>
 *
 * <p>Instances are not thread-safe by themselves; callers are responsible for
 * external synchronisation if multiple threads share one repository.</p>
 *
 * @since 0.9
 */
public class SessionRepository {

    private final DatabaseManager db;

    /**
     * Constructs a {@code SessionRepository} backed by the given database manager.
     *
     * @param db the initialised {@link DatabaseManager}; must not be {@code null}
     */
    public SessionRepository(DatabaseManager db) {
        this.db = db;
    }

    // ── Write operations ──────────────────────────────────────────────────────

    /**
     * Inserts a new session row for the given user.
     *
     * <p>{@code created_at} is set to the current Unix epoch-second.
     * {@code expires_at} is set to {@code created_at + expiryDays * 86400}.</p>
     *
     * @param userId     the id of the authenticated user
     * @param token      the UUID session token
     * @param expiryDays number of days until the session expires; must be &gt;= 0
     * @throws SQLException if the insert fails (e.g., duplicate token, foreign key violation)
     */
    public void createSession(int userId, String token, int expiryDays) throws SQLException {
        long now = System.currentTimeMillis() / 1000L;
        long expiresAt = now + (long) expiryDays * 86_400L;

        String sql = "INSERT INTO auth_sessions (token, user_id, created_at, expires_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setInt(2, userId);
            ps.setLong(3, now);
            ps.setLong(4, expiresAt);
            ps.executeUpdate();
        }
    }

    /**
     * Looks up a session by its token.
     *
     * @param token the token to search for; {@code null} returns {@link Optional#empty()}
     * @return an {@link Optional} containing the {@link SessionRecord} if found, or empty
     * @throws SQLException if the query fails
     */
    public Optional<SessionRecord> findByToken(String token) throws SQLException {
        if (token == null) {
            return Optional.empty();
        }
        String sql = "SELECT rowid, user_id, token, created_at, expires_at FROM auth_sessions WHERE token = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new SessionRecord(
                            rs.getLong("rowid"),
                            rs.getInt("user_id"),
                            rs.getString("token"),
                            rs.getLong("created_at"),
                            rs.getLong("expires_at")
                    ));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Returns {@code true} if the token exists in the database and has not yet expired.
     *
     * <p>Uses a single query: {@code SELECT 1 FROM auth_sessions WHERE token = ? AND expires_at > ?}
     * where the second parameter is the current Unix epoch-second.</p>
     *
     * @param token the token to validate; {@code null} always returns {@code false}
     * @return {@code true} if the token is present and its {@code expires_at} is in the future
     * @throws SQLException if the query fails
     */
    public boolean isTokenValid(String token) throws SQLException {
        if (token == null) {
            return false;
        }
        long now = System.currentTimeMillis() / 1000L;
        String sql = "SELECT 1 FROM auth_sessions WHERE token = ? AND expires_at > ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setLong(2, now);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Removes the session row identified by {@code token} (logout).
     *
     * <p>If no such token exists, this method is a no-op.</p>
     *
     * @param token the token to delete; {@code null} is silently ignored
     * @throws SQLException if the delete fails
     */
    public void deleteByToken(String token) throws SQLException {
        if (token == null) {
            return;
        }
        String sql = "DELETE FROM auth_sessions WHERE token = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, token);
            ps.executeUpdate();
        }
    }

    /**
     * Removes all session rows whose {@code expires_at} is less than or equal to the
     * current Unix epoch-second.
     *
     * <p>Intended to be called periodically (e.g., at server startup) to keep the
     * table free of stale rows.</p>
     *
     * @throws SQLException if the delete fails
     */
    public void deleteExpired() throws SQLException {
        long now = System.currentTimeMillis() / 1000L;
        String sql = "DELETE FROM auth_sessions WHERE expires_at <= ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setLong(1, now);
            ps.executeUpdate();
        }
    }

    // ── Inner record ──────────────────────────────────────────────────────────

    /**
     * Immutable snapshot of an {@code auth_sessions} row.
     *
     * @param id        the row id (SQLite {@code rowid})
     * @param userId    the id of the owning user
     * @param token     the UUID session token (primary key)
     * @param createdAt creation time as Unix epoch seconds
     * @param expiresAt expiry time as Unix epoch seconds
     */
    public record SessionRecord(
            long id,
            int userId,
            String token,
            long createdAt,
            long expiresAt) {}
}
