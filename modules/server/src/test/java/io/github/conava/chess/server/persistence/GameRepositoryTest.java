package io.github.conava.chess.server.persistence;

import io.github.conava.chess.server.db.DatabaseManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GameRepository} using an in-memory SQLite database.
 *
 * <p>Each test gets a fresh {@link DatabaseManager} initialised with {@code ":memory:"}
 * so there is no state leakage between tests. User rows are inserted directly via SQL
 * to satisfy foreign key constraints without depending on {@code UserRepository}.</p>
 */
class GameRepositoryTest {

    private DatabaseManager db;
    private GameRepository repo;

    /** Insert a minimal user row and return the generated user id. */
    private int insertUser(String username) throws SQLException {
        try (PreparedStatement ps = db.getConnection().prepareStatement(
                "INSERT INTO users (username, password_hash, created_at) VALUES (?, 'hash', strftime('%s','now'))",
                PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.executeUpdate();
            try (var rs = ps.getGeneratedKeys()) {
                assertTrue(rs.next(), "Expected generated key for user insert");
                return rs.getInt(1);
            }
        }
    }

    @BeforeEach
    void setUp() throws SQLException {
        db = new DatabaseManager();
        db.initialize(":memory:");
        repo = new GameRepository(db);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    // ── createGame ────────────────────────────────────────────────────────────

    @Test
    void createGame_returnsPositiveId() throws SQLException {
        int whiteId = insertUser("white_player");
        int blackId = insertUser("black_player");

        int id = repo.createGame(whiteId, blackId, "STANDARD", -1);

        assertTrue(id > 0, "createGame must return a positive auto-generated id");
    }

    @Test
    void createGame_setsStateToRunning() throws SQLException {
        int whiteId = insertUser("white1");
        int blackId = insertUser("black1");

        int id = repo.createGame(whiteId, blackId, "STANDARD", -1);
        Optional<GameRepository.GameRecord> record = repo.findGameById(id);

        assertTrue(record.isPresent(), "Game must be findable after creation");
        assertEquals("RUNNING", record.get().state(), "Initial state must be RUNNING");
    }

    // ── updateGameState ───────────────────────────────────────────────────────

    @Test
    void updateGameState_changesState() throws SQLException {
        int whiteId = insertUser("white2");
        int blackId = insertUser("black2");
        int id = repo.createGame(whiteId, blackId, "STANDARD", -1);

        repo.updateGameState(id, "PAUSED");

        Optional<GameRepository.GameRecord> record = repo.findGameById(id);
        assertTrue(record.isPresent());
        assertEquals("PAUSED", record.get().state(), "State must be updated to PAUSED");
    }

    // ── addMove / getMoves ────────────────────────────────────────────────────

    @Test
    void addAndGetMoves() throws SQLException {
        int whiteId = insertUser("white3");
        int blackId = insertUser("black3");
        int gameId = repo.createGame(whiteId, blackId, "STANDARD", -1);

        repo.addMove(gameId, 1, "e2-e4");
        repo.addMove(gameId, 2, "e7-e5");
        repo.addMove(gameId, 3, "g1-f3");

        List<GameRepository.MoveRecord> moves = repo.getMoves(gameId);

        assertEquals(3, moves.size(), "getMoves must return all 3 inserted moves");
        assertEquals(1, moves.get(0).moveNo(), "First move must have moveNo=1");
        assertEquals("e2-e4", moves.get(0).notation(), "First move notation must match");
        assertEquals(2, moves.get(1).moveNo(), "Second move must have moveNo=2");
        assertEquals("e7-e5", moves.get(1).notation(), "Second move notation must match");
        assertEquals(3, moves.get(2).moveNo(), "Third move must have moveNo=3");
        assertEquals("g1-f3", moves.get(2).notation(), "Third move notation must match");
    }

    @Test
    void getMoves_returnsEmptyList_whenNoMoves() throws SQLException {
        int whiteId = insertUser("white4");
        int blackId = insertUser("black4");
        int gameId = repo.createGame(whiteId, blackId, "STANDARD", -1);

        List<GameRepository.MoveRecord> moves = repo.getMoves(gameId);

        assertTrue(moves.isEmpty(), "getMoves must return empty list for a game with no moves");
    }

    // ── addChatMessage / getChatMessages ──────────────────────────────────────

    @Test
    void addAndGetChatMessages() throws SQLException {
        int whiteId = insertUser("white5");
        int blackId = insertUser("black5");
        int gameId = repo.createGame(whiteId, blackId, "STANDARD", -1);

        repo.addChatMessage(gameId, whiteId, "Hello!");
        repo.addChatMessage(gameId, blackId, "Hi there!");

        List<GameRepository.ChatRecord> messages = repo.getChatMessages(gameId);

        assertEquals(2, messages.size(), "getChatMessages must return both inserted messages");
        assertEquals("Hello!", messages.get(0).content(), "First message content must match");
        assertEquals(whiteId, messages.get(0).userId(), "First message userId must match");
        assertEquals("Hi there!", messages.get(1).content(), "Second message content must match");
        assertEquals(blackId, messages.get(1).userId(), "Second message userId must match");
    }

    @Test
    void getChatMessages_returnsEmptyList_whenNoMessages() throws SQLException {
        int whiteId = insertUser("white6");
        int blackId = insertUser("black6");
        int gameId = repo.createGame(whiteId, blackId, "STANDARD", -1);

        List<GameRepository.ChatRecord> messages = repo.getChatMessages(gameId);

        assertTrue(messages.isEmpty(), "getChatMessages must return empty list for a game with no chat");
    }

    // ── findSavedGamesByUserId ────────────────────────────────────────────────

    @Test
    void findSavedGamesByUserId_filtersCorrectly() throws SQLException {
        int userId = insertUser("persistent_user");
        int otherId = insertUser("other_user");

        // Game 1: RUNNING — must NOT appear in results
        int runningGame = repo.createGame(userId, otherId, "STANDARD", -1);
        // state stays RUNNING

        // Game 2: PAUSED — must appear
        int pausedGame = repo.createGame(userId, otherId, "STANDARD", -1);
        repo.updateGameState(pausedGame, "PAUSED");

        // Game 3: SAVED — must appear
        int savedGame = repo.createGame(otherId, userId, "STANDARD", -1);
        repo.updateGameState(savedGame, "SAVED");

        List<GameRepository.GameRecord> result = repo.findSavedGamesByUserId(userId);

        assertEquals(2, result.size(), "findSavedGamesByUserId must return only PAUSED and SAVED games");
        assertTrue(result.stream().anyMatch(g -> g.id() == pausedGame),
                "PAUSED game must be in results");
        assertTrue(result.stream().anyMatch(g -> g.id() == savedGame),
                "SAVED game must be in results");
        assertTrue(result.stream().noneMatch(g -> g.id() == runningGame),
                "RUNNING game must not be in results");
    }

    @Test
    void findSavedGamesByUserId_returnsEmpty_whenNoSavedGames() throws SQLException {
        int userId = insertUser("no_saved_user");
        int otherId = insertUser("other_no_saved");

        // Only RUNNING game — must not appear
        repo.createGame(userId, otherId, "STANDARD", -1);

        List<GameRepository.GameRecord> result = repo.findSavedGamesByUserId(userId);

        assertTrue(result.isEmpty(), "Must return empty list when no PAUSED/SAVED games exist");
    }

    // ── setDisconnect / clearDisconnect ───────────────────────────────────────

    @Test
    void setAndClearDisconnect() throws SQLException {
        int whiteId = insertUser("white7");
        int blackId = insertUser("black7");
        int gameId = repo.createGame(whiteId, blackId, "STANDARD", -1);

        repo.setDisconnect(gameId, whiteId);

        Optional<GameRepository.GameRecord> afterSet = repo.findGameById(gameId);
        assertTrue(afterSet.isPresent());
        assertEquals(whiteId, afterSet.get().disconnectUserId(),
                "disconnectUserId must be set to whiteId after setDisconnect");
        assertNotNull(afterSet.get().disconnectAt(),
                "disconnectAt must be set after setDisconnect");

        repo.clearDisconnect(gameId);

        Optional<GameRepository.GameRecord> afterClear = repo.findGameById(gameId);
        assertTrue(afterClear.isPresent());
        assertNull(afterClear.get().disconnectUserId(),
                "disconnectUserId must be null after clearDisconnect");
        assertNull(afterClear.get().disconnectAt(),
                "disconnectAt must be null after clearDisconnect");
    }

    // ── findGameById ─────────────────────────────────────────────────────────

    @Test
    void findGameById_returnsEmpty_whenNotFound() throws SQLException {
        Optional<GameRepository.GameRecord> result = repo.findGameById(99999);
        assertFalse(result.isPresent(), "findGameById must return empty for unknown id");
    }

    @Test
    void findGameById_storesPositionIndex() throws SQLException {
        int whiteId = insertUser("white8");
        int blackId = insertUser("black8");

        int gameId = repo.createGame(whiteId, blackId, "CHESS960", 42);
        Optional<GameRepository.GameRecord> record = repo.findGameById(gameId);

        assertTrue(record.isPresent());
        assertEquals(42, record.get().positionIndex(),
                "positionIndex 42 must be persisted and retrieved correctly");
    }
}
