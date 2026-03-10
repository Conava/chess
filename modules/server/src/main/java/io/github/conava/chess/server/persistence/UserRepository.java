package io.github.conava.chess.server.persistence;

import io.github.conava.chess.server.db.DatabaseManager;

import java.sql.*;
import java.time.Instant;
import java.util.Optional;

/**
 * Provides CRUD operations for the {@code users} table.
 *
 * <p>All SQL statements use parameterized {@link PreparedStatement}s; user-supplied
 * strings are never concatenated into query text.</p>
 *
 * <p>Instances are not thread-safe by themselves; callers are responsible for
 * external synchronization if multiple threads share one repository.</p>
 */
public class UserRepository {

    /**
     * Immutable snapshot of a {@code users} row.
     *
     * @param id           auto-generated primary key
     * @param username     the player's login name
     * @param passwordHash BCrypt (or similar) hash of the password
     * @param passwordSalt random salt used when hashing the password
     * @param createdAt    Unix epoch-second timestamp set at row creation
     */
    public record UserRecord(int id, String username, String passwordHash, String passwordSalt, long createdAt) {}

    private static final String INSERT_USER =
            "INSERT INTO users (username, password_hash, password_salt, created_at) VALUES (?, ?, ?, ?)";

    private static final String SELECT_BY_USERNAME =
            "SELECT id, username, password_hash, password_salt, created_at FROM users WHERE username = ?";

    private static final String SELECT_BY_ID =
            "SELECT id, username, password_hash, password_salt, created_at FROM users WHERE id = ?";

    private final DatabaseManager db;

    /**
     * Constructs a repository backed by the given {@link DatabaseManager}.
     *
     * <p>The required {@code users} schema (including the {@code password_salt} column)
     * is created by {@link DatabaseManager#initialize(String)} and does not require
     * any additional DDL from this class.</p>
     *
     * @param db the database manager providing the shared {@link Connection};
     *           must already be initialized via {@link DatabaseManager#initialize(String)}
     */
    public UserRepository(DatabaseManager db) {
        this.db = db;
    }

    // ── Write operations ──────────────────────────────────────────────────────

    /**
     * Inserts a new user row and returns the auto-generated primary key.
     *
     * <p>The {@code createdAt} timestamp is set to {@link Instant#now()} at call time.</p>
     *
     * @param username     the player's login name; must not be blank and must be at most
     *                     50 characters after trimming
     * @param passwordHash the pre-computed password hash to store
     * @param passwordSalt the salt that was used when computing {@code passwordHash}
     * @return the auto-generated {@code id} of the newly inserted row (always &gt; 0)
     * @throws IllegalArgumentException if {@code username} is blank, exceeds 50 characters,
     *                                  or a user with that name already exists
     * @throws SQLException             if the insert fails for any other database reason
     */
    public int createUser(String username, String passwordHash, String passwordSalt) throws SQLException {
        validateUsername(username);
        long createdAt = Instant.now().getEpochSecond();

        try (PreparedStatement ps = db.getConnection().prepareStatement(
                INSERT_USER, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, passwordSalt);
            ps.setLong(4, createdAt);

            try {
                ps.executeUpdate();
            } catch (SQLException e) {
                if (isUniqueConstraintViolation(e)) {
                    throw new IllegalArgumentException("Username already exists: " + username, e);
                }
                throw e;
            }

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
                throw new SQLException("Insert succeeded but no generated key was returned");
            }
        }
    }

    // ── Read operations ───────────────────────────────────────────────────────

    /**
     * Looks up a user by their login name.
     *
     * @param username the login name to search for (case-insensitive, per schema collation)
     * @return an {@link Optional} containing the matching {@link UserRecord}, or
     *         {@link Optional#empty()} if no such user exists
     * @throws SQLException if the query fails
     */
    public Optional<UserRecord> findByUsername(String username) throws SQLException {
        try (PreparedStatement ps = db.getConnection().prepareStatement(SELECT_BY_USERNAME)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Looks up a user by their primary key.
     *
     * @param userId the auto-generated user id
     * @return an {@link Optional} containing the matching {@link UserRecord}, or
     *         {@link Optional#empty()} if no such user exists
     * @throws SQLException if the query fails
     */
    public Optional<UserRecord> findById(int userId) throws SQLException {
        try (PreparedStatement ps = db.getConnection().prepareStatement(SELECT_BY_ID)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Maps the current row of {@code rs} to a {@link UserRecord}.
     *
     * @param rs a {@link ResultSet} positioned on a valid row
     * @return a new {@link UserRecord} populated from the current row
     * @throws SQLException if any column read fails
     */
    private UserRecord mapRow(ResultSet rs) throws SQLException {
        return new UserRecord(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("password_salt"),
                rs.getLong("created_at")
        );
    }

    /**
     * Validates that the supplied username is non-blank and within the 50-character limit.
     *
     * @param username the username to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        if (username.trim().length() > 50) {
            throw new IllegalArgumentException(
                    "Username must not exceed 50 characters (got " + username.trim().length() + ")");
        }
    }

    /**
     * Returns {@code true} when the given {@link SQLException} represents a UNIQUE
     * constraint violation in SQLite.
     *
     * <p>SQLite uses error code 19 (SQLITE_CONSTRAINT) and the message contains
     * the token {@code "UNIQUE"} for uniqueness failures.</p>
     *
     * @param e the exception to inspect
     * @return {@code true} if the exception is a unique constraint violation
     */
    private boolean isUniqueConstraintViolation(SQLException e) {
        // SQLite error code 19 = SQLITE_CONSTRAINT; message contains "UNIQUE"
        return e.getErrorCode() == 19
                || (e.getMessage() != null && e.getMessage().toUpperCase().contains("UNIQUE"));
    }
}
