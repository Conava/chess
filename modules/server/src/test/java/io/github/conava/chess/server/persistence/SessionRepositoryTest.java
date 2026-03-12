package io.github.conava.chess.server.persistence;

import io.github.conava.chess.server.db.DatabaseManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SessionRepository} using an in-memory SQLite database.
 */
class SessionRepositoryTest {

    private DatabaseManager db;
    private SessionRepository repo;

    /** Seed user ID used for all session tests — inserted directly to satisfy FK. */
    private static final int USER_ID = 1;
    private static final String TOKEN_A = "token-aaaa-1111";
    private static final String TOKEN_B = "token-bbbb-2222";

    @BeforeEach
    void setUp() throws SQLException {
        db = new DatabaseManager();
        db.initialize(":memory:");

        // Insert a user directly so FK references satisfy (foreign_keys=ON in DatabaseManager)
        try (Statement st = db.getConnection().createStatement()) {
            st.execute("INSERT INTO users (id, username, password_hash) VALUES (1, 'alice', 'hash')");
        }

        repo = new SessionRepository(db);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    // ─── createAndFindSession ─────────────────────────────────────────────────

    @Test
    void createAndFindSession() throws SQLException {
        repo.createSession(USER_ID, TOKEN_A, 30);

        Optional<SessionRepository.SessionRecord> result = repo.findByToken(TOKEN_A);

        assertTrue(result.isPresent(), "Session should be present after creation");
        SessionRepository.SessionRecord rec = result.get();
        assertEquals(USER_ID, rec.userId());
        assertEquals(TOKEN_A, rec.token());
        assertTrue(rec.createdAt() > 0, "created_at should be positive epoch seconds");
        assertTrue(rec.expiresAt() > rec.createdAt(), "expires_at should be after created_at");
    }

    // ─── findByToken with null input ──────────────────────────────────────────

    @Test
    void findByToken_returnsEmpty_whenTokenIsNull() throws SQLException {
        Optional<SessionRepository.SessionRecord> result = repo.findByToken(null);
        assertFalse(result.isPresent(), "findByToken(null) should return empty");
    }

    // ─── isTokenValid ─────────────────────────────────────────────────────────

    @Test
    void isTokenValid_returnsTrue_whenNotExpired() throws SQLException {
        repo.createSession(USER_ID, TOKEN_A, 30);
        assertTrue(repo.isTokenValid(TOKEN_A), "Fresh 30-day token should be valid");
    }

    @Test
    void isTokenValid_returnsFalse_whenExpired() throws SQLException {
        // Insert directly with expires_at = created_at (already expired at creation time)
        long now = System.currentTimeMillis() / 1000L;
        try (var ps = db.getConnection().prepareStatement(
                "INSERT INTO auth_sessions (token, user_id, created_at, expires_at) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, TOKEN_A);
            ps.setInt(2, USER_ID);
            ps.setLong(3, now - 10);   // created 10s ago
            ps.setLong(4, now - 1);    // expired 1s ago
            ps.executeUpdate();
        }
        assertFalse(repo.isTokenValid(TOKEN_A), "Expired token should not be valid");
    }

    @Test
    void isTokenValid_returnsFalse_whenTokenDoesNotExist() throws SQLException {
        assertFalse(repo.isTokenValid("no-such-token"), "Non-existent token should not be valid");
    }

    // ─── deleteByToken ────────────────────────────────────────────────────────

    @Test
    void deleteByToken_removesSession() throws SQLException {
        repo.createSession(USER_ID, TOKEN_A, 30);
        repo.deleteByToken(TOKEN_A);
        assertFalse(repo.findByToken(TOKEN_A).isPresent(), "Session should be gone after deletion");
    }

    // ─── deleteExpired ────────────────────────────────────────────────────────

    @Test
    void deleteExpired_removesOnlyExpired() throws SQLException {
        // Live session
        repo.createSession(USER_ID, TOKEN_A, 30);

        // Expired session inserted directly
        long now = System.currentTimeMillis() / 1000L;
        try (var ps = db.getConnection().prepareStatement(
                "INSERT INTO auth_sessions (token, user_id, created_at, expires_at) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, TOKEN_B);
            ps.setInt(2, USER_ID);
            ps.setLong(3, now - 100);
            ps.setLong(4, now - 1);
            ps.executeUpdate();
        }

        repo.deleteExpired();

        assertTrue(repo.findByToken(TOKEN_A).isPresent(), "Live session should survive deleteExpired");
        assertFalse(repo.findByToken(TOKEN_B).isPresent(), "Expired session should be removed by deleteExpired");
    }
}
