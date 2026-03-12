package io.github.conava.chess.server.persistence;

import io.github.conava.chess.server.db.DatabaseManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link UserRepository} using an in-memory SQLite database.
 *
 * <p>Each test gets a fresh {@link DatabaseManager} initialised with {@code ":memory:"}
 * so there is no state leakage between tests.</p>
 */
class UserRepositoryTest {

    private DatabaseManager db;
    private UserRepository repo;

    @BeforeEach
    void setUp() throws SQLException {
        db = new DatabaseManager();
        db.initialize(":memory:");
        repo = new UserRepository(db);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    // ── createUser ────────────────────────────────────────────────────────────

    @Test
    void createUser_returnsPositiveId() throws SQLException {
        int id = repo.createUser("alice", "hash_abc", "salt_abc");
        assertTrue(id > 0, "createUser must return a positive auto-generated id");
    }

    @Test
    void createUser_throwsOnDuplicateUsername() throws SQLException {
        repo.createUser("bob", "hash1", "salt1");
        assertThrows(IllegalArgumentException.class,
                () -> repo.createUser("bob", "hash2", "salt2"),
                "createUser must throw IllegalArgumentException for duplicate username");
    }

    @Test
    void createUser_throwsOnBlankUsername() {
        assertThrows(IllegalArgumentException.class,
                () -> repo.createUser("   ", "hash", "salt"),
                "createUser must throw IllegalArgumentException for blank username");
    }

    @Test
    void createUser_throwsOnEmptyUsername() {
        assertThrows(IllegalArgumentException.class,
                () -> repo.createUser("", "hash", "salt"),
                "createUser must throw IllegalArgumentException for empty username");
    }

    @Test
    void createUser_throwsOnUsernameTooLong() {
        String longUsername = "a".repeat(51);
        assertThrows(IllegalArgumentException.class,
                () -> repo.createUser(longUsername, "hash", "salt"),
                "createUser must throw IllegalArgumentException when username exceeds 50 characters");
    }

    // ── findByUsername ────────────────────────────────────────────────────────

    @Test
    void findByUsername_returnsUser() throws SQLException {
        int id = repo.createUser("carol", "hash_carol", "salt_carol");

        Optional<UserRepository.UserRecord> result = repo.findByUsername("carol");

        assertTrue(result.isPresent(), "findByUsername must return a non-empty Optional for an existing user");
        UserRepository.UserRecord record = result.get();
        assertEquals(id, record.id(), "UserRecord.id must match the id returned by createUser");
        assertEquals("carol", record.username(), "UserRecord.username must match the stored username");
        assertEquals("hash_carol", record.passwordHash(), "UserRecord.passwordHash must match the stored hash");
        assertEquals("salt_carol", record.passwordSalt(), "UserRecord.passwordSalt must match the stored salt");
        assertTrue(record.createdAt() > 0, "UserRecord.createdAt must be a positive epoch-second timestamp");
    }

    @Test
    void findByUsername_returnsEmpty_whenNotFound() throws SQLException {
        Optional<UserRepository.UserRecord> result = repo.findByUsername("nonexistent");
        assertFalse(result.isPresent(), "findByUsername must return Optional.empty() for an unknown username");
    }

    // ── findById ─────────────────────────────────────────────────────────────

    @Test
    void findById_returnsUser() throws SQLException {
        int id = repo.createUser("dave", "hash_dave", "salt_dave");

        Optional<UserRepository.UserRecord> result = repo.findById(id);

        assertTrue(result.isPresent(), "findById must return a non-empty Optional for an existing user id");
        assertEquals("dave", result.get().username(), "UserRecord.username must match the stored username");
    }

    @Test
    void findById_returnsEmpty_whenNotFound() throws SQLException {
        Optional<UserRepository.UserRecord> result = repo.findById(99999);
        assertFalse(result.isPresent(), "findById must return Optional.empty() for an unknown id");
    }
}
