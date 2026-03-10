package io.github.conava.chess.server.persistence;

import io.github.conava.chess.server.db.DatabaseManager;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Provides CRUD operations for the {@code games}, {@code moves}, and
 * {@code chat_messages} tables.
 *
 * <p>All SQL statements use parameterized {@link PreparedStatement}s; no
 * user-supplied strings are concatenated into query text.</p>
 */
public class GameRepository {

    /**
     * Immutable snapshot of a {@code games} row.
     *
     * @param id               auto-generated primary key
     * @param ruleset          the ruleset name (e.g., {@code "STANDARD"}, {@code "CHESS960"})
     * @param positionIndex    Chess960 Scharnagl index, or {@code -1} for standard chess
     * @param whiteUserId      the white player's user id (nullable)
     * @param blackUserId      the black player's user id (nullable)
     * @param state            the current game state string (e.g., {@code "RUNNING"})
     * @param createdAt        Unix epoch-second creation timestamp
     * @param updatedAt        Unix epoch-second last-update timestamp
     * @param disconnectUserId user id of the player who disconnected, or {@code null}
     * @param disconnectAt     epoch-second when the disconnect occurred, or {@code null}
     */
    public record GameRecord(
            int id,
            String ruleset,
            int positionIndex,
            Integer whiteUserId,
            Integer blackUserId,
            String state,
            long createdAt,
            long updatedAt,
            Integer disconnectUserId,
            Long disconnectAt) {}

    /**
     * Immutable snapshot of a {@code moves} row.
     *
     * @param moveNo   the sequential move number (1-based)
     * @param notation the move notation string
     */
    public record MoveRecord(int moveNo, String notation) {}

    /**
     * Immutable snapshot of a {@code chat_messages} row.
     *
     * @param userId  the sender's user id
     * @param content the message text
     * @param sentAt  Unix epoch-second send timestamp
     */
    public record ChatRecord(int userId, String content, long sentAt) {}

    private final DatabaseManager db;

    /**
     * Constructs a repository backed by the given {@link DatabaseManager}.
     *
     * <p>The required schema ({@code games}, {@code moves}, and {@code chat_messages} tables
     * including the {@code position_index} and {@code disconnect_*} columns) is created by
     * {@link DatabaseManager#initialize(String)} and does not require any additional
     * DDL from this class.</p>
     *
     * @param db the database manager providing the shared {@link Connection};
     *           must already be initialized via {@link DatabaseManager#initialize(String)}
     */
    public GameRepository(DatabaseManager db) {
        this.db = db;
    }

    // ── createGame ────────────────────────────────────────────────────────────

    /**
     * Inserts a new game row with state {@code "RUNNING"} and returns the
     * auto-generated primary key.
     *
     * @param whiteUserId  the white player's user id
     * @param blackUserId  the black player's user id
     * @param ruleset      the ruleset identifier string
     * @param positionIndex Chess960 Scharnagl index, or {@code -1} for non-Chess960
     * @return the auto-generated game id (always &gt; 0)
     * @throws SQLException if the insert fails
     */
    public int createGame(int whiteUserId, int blackUserId, String ruleset, int positionIndex)
            throws SQLException {
        long now = Instant.now().getEpochSecond();
        String sql = """
                INSERT INTO games (white_user_id, black_user_id, ruleset, position_index,
                                   state, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'RUNNING', ?, ?)
                """;
        try (PreparedStatement ps = db.getConnection().prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, whiteUserId);
            ps.setInt(2, blackUserId);
            ps.setString(3, ruleset);
            ps.setInt(4, positionIndex);
            ps.setLong(5, now);
            ps.setLong(6, now);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
                throw new SQLException("Insert succeeded but no generated key was returned");
            }
        }
    }

    // ── findGameById ─────────────────────────────────────────────────────────

    /**
     * Looks up a game by its primary key.
     *
     * @param id the game id to search for
     * @return an {@link Optional} containing the matching {@link GameRecord}, or empty
     * @throws SQLException if the query fails
     */
    public Optional<GameRecord> findGameById(int id) throws SQLException {
        String sql = """
                SELECT id, ruleset, position_index, white_user_id, black_user_id, state,
                       created_at, updated_at, disconnect_user_id, disconnect_at
                FROM games WHERE id = ?
                """;
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapGameRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    // ── updateGameState ───────────────────────────────────────────────────────

    /**
     * Updates the {@code state} column for the given game and refreshes
     * {@code updated_at} to the current time.
     *
     * @param gameId the game to update
     * @param state  the new state string
     * @throws SQLException if the update fails
     */
    public void updateGameState(int gameId, String state) throws SQLException {
        long now = Instant.now().getEpochSecond();
        String sql = "UPDATE games SET state = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, state);
            ps.setLong(2, now);
            ps.setInt(3, gameId);
            ps.executeUpdate();
        }
    }

    // ── moves ─────────────────────────────────────────────────────────────────

    /**
     * Appends a move to the {@code moves} table for the given game.
     *
     * @param gameId  the game that the move belongs to
     * @param moveNo  the sequential move number (1-based)
     * @param notation the move notation string
     * @throws SQLException if the insert fails
     */
    public void addMove(int gameId, int moveNo, String notation) throws SQLException {
        long now = Instant.now().getEpochSecond();
        String sql = "INSERT INTO moves (game_id, move_no, notation, played_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, gameId);
            ps.setInt(2, moveNo);
            ps.setString(3, notation);
            ps.setLong(4, now);
            ps.executeUpdate();
        }
    }

    /**
     * Returns all moves for the given game in ascending move-number order.
     *
     * @param gameId the game to query
     * @return an unmodifiable list of {@link MoveRecord} objects, possibly empty
     * @throws SQLException if the query fails
     */
    public List<MoveRecord> getMoves(int gameId) throws SQLException {
        String sql = "SELECT move_no, notation FROM moves WHERE game_id = ? ORDER BY move_no ASC";
        List<MoveRecord> result = new ArrayList<>();
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, gameId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new MoveRecord(rs.getInt("move_no"), rs.getString("notation")));
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    // ── chat messages ─────────────────────────────────────────────────────────

    /**
     * Inserts a chat message for the given game.
     *
     * @param gameId  the game the message belongs to
     * @param userId  the sender's user id
     * @param content the message text
     * @throws SQLException if the insert fails
     */
    public void addChatMessage(int gameId, int userId, String content) throws SQLException {
        long now = Instant.now().getEpochSecond();
        String sql = "INSERT INTO chat_messages (game_id, user_id, content, sent_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, gameId);
            ps.setInt(2, userId);
            ps.setString(3, content);
            ps.setLong(4, now);
            ps.executeUpdate();
        }
    }

    /**
     * Returns all chat messages for the given game in ascending send-time order.
     *
     * @param gameId the game to query
     * @return an unmodifiable list of {@link ChatRecord} objects, possibly empty
     * @throws SQLException if the query fails
     */
    public List<ChatRecord> getChatMessages(int gameId) throws SQLException {
        String sql = "SELECT user_id, content, sent_at FROM chat_messages WHERE game_id = ? ORDER BY sent_at ASC";
        List<ChatRecord> result = new ArrayList<>();
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, gameId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new ChatRecord(
                            rs.getInt("user_id"),
                            rs.getString("content"),
                            rs.getLong("sent_at")));
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    // ── findSavedGamesByUserId ────────────────────────────────────────────────

    /**
     * Returns all games in {@code PAUSED} or {@code SAVED} state where the given
     * user is either the white or black player.
     *
     * @param userId the user id to filter by
     * @return an unmodifiable list of matching {@link GameRecord} objects, possibly empty
     * @throws SQLException if the query fails
     */
    public List<GameRecord> findSavedGamesByUserId(int userId) throws SQLException {
        String sql = """
                SELECT id, ruleset, position_index, white_user_id, black_user_id, state,
                       created_at, updated_at, disconnect_user_id, disconnect_at
                FROM games
                WHERE (white_user_id = ? OR black_user_id = ?)
                  AND state IN ('PAUSED', 'SAVED')
                ORDER BY updated_at DESC
                """;
        List<GameRecord> result = new ArrayList<>();
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapGameRow(rs));
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    // ── disconnect tracking ───────────────────────────────────────────────────

    /**
     * Records that the given user disconnected from the game.
     *
     * @param gameId the game affected by the disconnect
     * @param userId the user who disconnected
     * @throws SQLException if the update fails
     */
    public void setDisconnect(int gameId, int userId) throws SQLException {
        long now = Instant.now().getEpochSecond();
        String sql = "UPDATE games SET disconnect_user_id = ?, disconnect_at = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setLong(2, now);
            ps.setLong(3, now);
            ps.setInt(4, gameId);
            ps.executeUpdate();
        }
    }

    /**
     * Clears the disconnect record for the given game (e.g., after reconnection).
     *
     * @param gameId the game to clear the disconnect for
     * @throws SQLException if the update fails
     */
    public void clearDisconnect(int gameId) throws SQLException {
        long now = Instant.now().getEpochSecond();
        String sql = "UPDATE games SET disconnect_user_id = NULL, disconnect_at = NULL, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setLong(1, now);
            ps.setInt(2, gameId);
            ps.executeUpdate();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Maps the current row of {@code rs} to a {@link GameRecord}.
     *
     * @param rs a {@link ResultSet} positioned on a valid row
     * @return a new {@link GameRecord} populated from the current row
     * @throws SQLException if any column read fails
     */
    private GameRecord mapGameRow(ResultSet rs) throws SQLException {
        int disconnectUserIdRaw = rs.getInt("disconnect_user_id");
        Integer disconnectUserId = rs.wasNull() ? null : disconnectUserIdRaw;

        long disconnectAtRaw = rs.getLong("disconnect_at");
        Long disconnectAt = rs.wasNull() ? null : disconnectAtRaw;

        int whiteIdRaw = rs.getInt("white_user_id");
        Integer whiteUserId = rs.wasNull() ? null : whiteIdRaw;

        int blackIdRaw = rs.getInt("black_user_id");
        Integer blackUserId = rs.wasNull() ? null : blackIdRaw;

        return new GameRecord(
                rs.getInt("id"),
                rs.getString("ruleset"),
                rs.getInt("position_index"),
                whiteUserId,
                blackUserId,
                rs.getString("state"),
                rs.getLong("created_at"),
                rs.getLong("updated_at"),
                disconnectUserId,
                disconnectAt
        );
    }
}
