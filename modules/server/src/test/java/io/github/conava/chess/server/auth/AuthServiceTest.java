package io.github.conava.chess.server.auth;

import io.github.conava.chess.server.db.DatabaseManager;
import io.github.conava.chess.server.management.PlayerSession;
import io.github.conava.chess.server.persistence.SessionRepository;
import io.github.conava.chess.server.persistence.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AuthService} using an in-memory SQLite database.
 *
 * <p>Each test gets a fresh {@link DatabaseManager} initialised with {@code ":memory:"}
 * so there is no state leakage between tests.</p>
 */
class AuthServiceTest {

    private DatabaseManager db;
    private AuthService authService;

    @BeforeEach
    void setUp() throws SQLException {
        db = new DatabaseManager();
        db.initialize(":memory:");
        UserRepository userRepo = new UserRepository(db);
        SessionRepository sessionRepo = new SessionRepository(db);
        authService = new AuthService(userRepo, sessionRepo, 30);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    void register_returnsToken() throws SQLException {
        String token = authService.register("alice", "password123");
        assertNotNull(token, "register must return a non-null token");
        // UUID format: 8-4-4-4-12
        assertTrue(token.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"),
                "register must return a UUID-format token, got: " + token);
    }

    @Test
    void register_fails_withShortPassword() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("bob", "abc"),
                "register must throw IllegalArgumentException when password is shorter than 8 characters");
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    void login_succeeds_withCorrectPassword() throws SQLException {
        authService.register("carol", "correctPass1");
        String token = authService.login("carol", "correctPass1");
        assertNotNull(token, "login must return a non-null token for correct credentials");
        assertTrue(token.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"),
                "login must return a UUID-format token");
    }

    @Test
    void login_fails_withWrongPassword() throws SQLException {
        authService.register("dave", "correctPass1");
        assertThrows(IllegalArgumentException.class,
                () -> authService.login("dave", "wrongPass99"),
                "login must throw IllegalArgumentException for incorrect password");
    }

    @Test
    void login_fails_withUnknownUsername() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.login("nonexistent", "somePass12"),
                "login must throw IllegalArgumentException for unknown username");
    }

    // ── verifyToken ───────────────────────────────────────────────────────────

    @Test
    void verifyToken_returnsSession_whenValid() throws SQLException {
        String token = authService.register("eve", "securePass1");
        Optional<PlayerSession> session = authService.verifyToken(token);
        assertTrue(session.isPresent(), "verifyToken must return a present Optional for a valid token");
        assertTrue(session.get().getUserId() > 0, "PlayerSession.userId must be positive");
        assertEquals("eve", session.get().getUsername(), "PlayerSession.username must match the registered username");
        assertEquals(token, session.get().getAuthToken(), "PlayerSession.authToken must match the verified token");
    }

    @Test
    void verifyToken_returnsEmpty_whenInvalid() throws SQLException {
        Optional<PlayerSession> session = authService.verifyToken("00000000-0000-0000-0000-000000000000");
        assertFalse(session.isPresent(), "verifyToken must return Optional.empty() for an unknown token");
    }

    // ── logout ────────────────────────────────────────────────────────────────

    @Test
    void logout_invalidatesToken() throws SQLException {
        String token = authService.register("frank", "securePass9");
        authService.logout(token);
        Optional<PlayerSession> session = authService.verifyToken(token);
        assertFalse(session.isPresent(), "verifyToken must return Optional.empty() after logout");
    }
}
