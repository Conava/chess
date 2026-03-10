package io.github.conava.chess.server.db;

import java.sql.*;
import java.util.logging.Logger;

/**
 * Opens (or creates) the SQLite database and ensures the schema is up to date.
 *
 * <p>Call {@link #initialize(String)} once at server startup, then use
 * {@link #getConnection()} to borrow a connection for each operation.
 * This class keeps a single connection open for the lifetime of the server.
 * SQLite serialises writes natively so a shared connection is safe here.</p>
 */
public class DatabaseManager {

    private static final Logger LOG = Logger.getLogger(DatabaseManager.class.getName());

    private Connection connection;

    /**
     * Opens the database at {@code dbPath} and creates all tables if they do not exist.
     *
     * @param dbPath path to the SQLite file, e.g. {@code "chess.db"} or an absolute path
     * @throws SQLException if the database cannot be opened or the schema cannot be created
     */
    public void initialize(String dbPath) throws SQLException {
        String url = "jdbc:sqlite:" + dbPath;
        connection = DriverManager.getConnection(url);
        // Enable WAL mode for better concurrent read performance
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL");
            st.execute("PRAGMA foreign_keys=ON");
        }
        createSchema();
        LOG.info("Database initialised at: " + dbPath);
    }

    /** Returns the shared connection. Always non-null after {@link #initialize}. */
    public Connection getConnection() {
        return connection;
    }

    /** Closes the database connection gracefully. */
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                LOG.warning("Error closing database connection: " + e.getMessage());
            }
            connection = null;
        }
    }

    // ── Schema ────────────────────────────────────────────────────────────────

    private void createSchema() throws SQLException {
        try (Statement st = connection.createStatement()) {

            // Users — identity store
            st.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id            INTEGER PRIMARY KEY AUTOINCREMENT,
                    username      TEXT    NOT NULL UNIQUE COLLATE NOCASE,
                    password_hash TEXT    NOT NULL,
                    created_at    INTEGER NOT NULL DEFAULT (strftime('%s','now'))
                )
                """);

            // Auth sessions — token-based authentication
            st.execute("""
                CREATE TABLE IF NOT EXISTS auth_sessions (
                    token       TEXT    PRIMARY KEY,
                    user_id     INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    created_at  INTEGER NOT NULL DEFAULT (strftime('%s','now')),
                    expires_at  INTEGER NOT NULL
                )
                """);

            // Games — persisted game records
            // state values: WAITING, RUNNING, PAUSED, SAVED, <terminal GameState name>
            st.execute("""
                CREATE TABLE IF NOT EXISTS games (
                    id              INTEGER PRIMARY KEY AUTOINCREMENT,
                    ruleset         TEXT    NOT NULL,
                    ruleset_param   TEXT,
                    white_user_id   INTEGER REFERENCES users(id),
                    black_user_id   INTEGER REFERENCES users(id),
                    state           TEXT    NOT NULL DEFAULT 'WAITING',
                    move_history    TEXT    NOT NULL DEFAULT '',
                    created_at      INTEGER NOT NULL DEFAULT (strftime('%s','now')),
                    updated_at      INTEGER NOT NULL DEFAULT (strftime('%s','now'))
                )
                """);

            // Index for fast lookup of games by player
            st.execute("""
                CREATE INDEX IF NOT EXISTS idx_games_white ON games(white_user_id)
                """);
            st.execute("""
                CREATE INDEX IF NOT EXISTS idx_games_black ON games(black_user_id)
                """);

            // Chat messages — stored per game
            st.execute("""
                CREATE TABLE IF NOT EXISTS chat_messages (
                    id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    game_id    INTEGER NOT NULL REFERENCES games(id) ON DELETE CASCADE,
                    user_id    INTEGER NOT NULL REFERENCES users(id),
                    content    TEXT    NOT NULL,
                    sent_at    INTEGER NOT NULL DEFAULT (strftime('%s','now'))
                )
                """);

            st.execute("""
                CREATE INDEX IF NOT EXISTS idx_chat_game ON chat_messages(game_id)
                """);
        }
        LOG.fine("Schema verified/created successfully");
    }
}
