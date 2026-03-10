# Server Overhaul - Implementation Plan

Date: 2026-03-10
Design doc: `/home/marlon/source/chess/docs/plans/2026-03-10-server-overhaul-design.md`
Branch: `feat/server-overhaul`

---

<!-- SCHEDULING -->
| id | name | group | touches | depends_on |
|----|------|-------|---------|------------|
| T01 | Extend MessageType enum | 1 | modules/core/src/main/java/io/github/conava/chess/core/data/io/MessageType.java, modules/core/src/test/java/io/github/conava/chess/core/data/io/MessageTypeTest.java | -- |
| T02 | Extend GameState enum | 1 | modules/core/src/main/java/io/github/conava/chess/core/logic/game/GameState.java, modules/core/src/test/java/io/github/conava/chess/core/logic/game/GameStateTest.java | -- |
| T03 | Add sqlite-jdbc dependency to server pom.xml | 1 | modules/server/pom.xml | -- |
| T10 | PlayerSession data class | 1 | modules/server/src/main/java/io/github/conava/chess/server/management/PlayerSession.java | -- |
| T04 | ServerConfig: load server.properties | 2 | modules/server/src/main/java/io/github/conava/chess/server/config/ServerConfig.java, modules/server/src/main/resources/server.properties, modules/server/src/test/java/io/github/conava/chess/server/config/ServerConfigTest.java | T03 |
| T05 | DatabaseManager + schema init | 2 | modules/server/src/main/java/io/github/conava/chess/server/persistence/DatabaseManager.java, modules/server/src/test/java/io/github/conava/chess/server/persistence/DatabaseManagerTest.java | T03 |
| T15 | SettingsService: auth token persistence | 2 | modules/application/src/main/java/io/github/conava/chess/application/settings/SettingsService.java | T01 |
| T22 | OnlineGame: handle PAUSED state + SAVE_GAME negotiation + GAME_HISTORY replay | 2 | modules/core/src/main/java/io/github/conava/chess/core/logic/game/OnlineGame.java, modules/core/src/test/java/io/github/conava/chess/core/logic/game/OnlineGameServerConnectionTest.java | T01, T02 |
| T06 | UserRepository | 3 | modules/server/src/main/java/io/github/conava/chess/server/persistence/UserRepository.java, modules/server/src/test/java/io/github/conava/chess/server/persistence/UserRepositoryTest.java | T05 |
| T07 | SessionRepository | 3 | modules/server/src/main/java/io/github/conava/chess/server/persistence/SessionRepository.java, modules/server/src/test/java/io/github/conava/chess/server/persistence/SessionRepositoryTest.java | T05 |
| T08 | GameRepository | 3 | modules/server/src/main/java/io/github/conava/chess/server/persistence/GameRepository.java, modules/server/src/test/java/io/github/conava/chess/server/persistence/GameRepositoryTest.java | T05 |
| T11 | Extract GameManager from Server | 3 | modules/server/src/main/java/io/github/conava/chess/server/management/GameManager.java, modules/server/src/main/java/io/github/conava/chess/server/Server.java, modules/server/src/test/java/io/github/conava/chess/server/management/GameManagerTest.java | T04 |
| T16 | Chess.java facade: auth + matchmaking + resume methods | 3 | modules/application/src/main/java/io/github/conava/chess/application/Chess.java | T01, T15 |
| T09 | AuthService (PBKDF2 hashing, register, login, verify) | 4 | modules/server/src/main/java/io/github/conava/chess/server/auth/AuthService.java, modules/server/src/test/java/io/github/conava/chess/server/auth/AuthServiceTest.java | T06, T07 |
| T17 | LoginController + login.fxml | 4 | modules/application/src/main/java/io/github/conava/chess/application/controllers/LoginController.java, modules/application/src/main/resources/fxml/login.fxml | T16 |
| T18 | RegisterController + register.fxml | 4 | modules/application/src/main/java/io/github/conava/chess/application/controllers/RegisterController.java, modules/application/src/main/resources/fxml/register.fxml | T16 |
| T21 | ServerCommunicationTask: handle new message types | 4 | modules/application/src/main/java/io/github/conava/chess/application/network/ServerCommunicationTask.java | T01, T16 |
| T13 | Refactor GameInstance: pause/resume/save, promotion fix, chat relay | 4 | modules/server/src/main/java/io/github/conava/chess/server/management/GameInstance.java, modules/server/src/test/java/io/github/conava/chess/server/management/GameInstanceTest.java | T01, T02, T08, T11 |
| T12 | Refactor ClientHandler: auth gating + new message dispatch | 5 | modules/server/src/main/java/io/github/conava/chess/server/management/ClientHandler.java, modules/server/src/test/java/io/github/conava/chess/server/management/ClientHandlerIntegrationTest.java | T01, T09, T10, T11 |
| T19 | SceneManager: showLogin + showRegister | 5 | modules/application/src/main/java/io/github/conava/chess/application/navigation/SceneManager.java | T17, T18 |
| T23 | OnlineSetupController: "Find Match" button + matchmaking settings | 5 | modules/application/src/main/java/io/github/conava/chess/application/controllers/OnlineSetupController.java, modules/application/src/main/resources/fxml/online-setup.fxml | T16, T21 |
| T25 | GameController: chat panel + Save & Exit button | 5 | modules/application/src/main/java/io/github/conava/chess/application/controllers/GameController.java, modules/application/src/main/resources/fxml/game.fxml | T16, T21, T22 |
| T14 | MatchmakingService | 6 | modules/server/src/main/java/io/github/conava/chess/server/matchmaking/MatchmakingService.java, modules/server/src/test/java/io/github/conava/chess/server/matchmaking/MatchmakingServiceTest.java | T11, T12 |
| T20 | MainMenuController: auth gating + username display | 6 | modules/application/src/main/java/io/github/conava/chess/application/controllers/MainMenuController.java, modules/application/src/main/resources/fxml/main-menu.fxml | T16, T19 |
| T24 | WaitingForMatchController + waiting-for-match.fxml | 6 | modules/application/src/main/java/io/github/conava/chess/application/controllers/WaitingForMatchController.java, modules/application/src/main/resources/fxml/waiting-for-match.fxml | T19, T21 |
| T26 | i18n keys for chat and auth screens | 6 | modules/application/src/main/resources/i18n/messages_en.properties, modules/application/src/main/resources/i18n/messages_de.properties | T17, T18, T25 |
<!-- /SCHEDULING -->

---

## Task T01: Extend MessageType enum

### Context
The wire protocol needs new message types for authentication, matchmaking, chat, game saving, and game history replay. All new message types must exist in `core`'s `MessageType` enum before any server or application code can reference them. This is a foundational change with no dependencies.

### Requirements
1. Add the following values to the `MessageType` enum: `REGISTER`, `LOGIN`, `AUTH_TOKEN`, `RESUME_GAME`, `GAME_HISTORY`, `CHAT`, `QUEUE`, `DEQUEUE`, `MATCHED`, `SAVE_GAME`, `SAVE_ACCEPTED`.
2. Existing values (`CREATE_GAME`, `JOIN_GAME`, `JOIN_CODE`, `MOVE`, `GAME_STATUS`, `SUCCESS`, `ERROR`, `FAILURE`) must remain unchanged and in their current order.
3. The new values must be appended after the existing values.

### Implementation Details
- File: `modules/core/src/main/java/io/github/conava/chess/core/data/io/MessageType.java`
- The enum currently has 8 values on lines 4-11. Append 11 new values after `FAILURE`.
- No constructor or fields needed -- these are plain enum constants like the existing ones.

### Edge Cases and Pitfalls
- Do not reorder existing values; serialization via `name()` is order-independent but reordering is unnecessarily risky.
- `MessageParser.parse()` uses `MessageType.valueOf()` which will automatically resolve the new names; no changes needed there.

### Test Approach
`tdd`

Test file: `modules/core/src/test/java/io/github/conava/chess/core/data/io/MessageTypeTest.java`

- `allExpectedValuesExist`: Assert `MessageType.values().length == 19` (8 old + 11 new). Verify each new value can be resolved via `MessageType.valueOf("REGISTER")`, etc.
- `parseRoundTrip_newTypes`: For each new type, create a `Message` with that type, serialize via `MessageParser.serialize()`, parse back via `MessageParser.parse()`, assert the type matches.

### Acceptance Criteria
- [ ] `MessageType.valueOf("REGISTER")` (and all 10 other new values) returns the correct enum constant
- [ ] `MessageType.values().length == 19`
- [ ] Existing `MessageParser` round-trip tests still pass
- [ ] All new tests pass

---

## Task T02: Extend GameState enum

### Context
The server overhaul adds pause/save-for-later functionality. Games can be paused (on disconnect or mutual save) and need new terminal states for timeout wins. The `GameState` enum in `core` must be extended.

### Requirements
1. Add `PAUSED("Game paused")` to the `GameState` enum.
2. Add `SAVED("Game saved for later")` to the `GameState` enum.
3. Existing values and their `message` strings must remain unchanged.

### Implementation Details
- File: `modules/core/src/main/java/io/github/conava/chess/core/logic/game/GameState.java`
- Add `PAUSED` and `SAVED` after the existing draw states, before the semicolon on line 18.
- The existing `WHITE_WON_BY_TIMEOUT` and `BLACK_WON_BY_TIMEOUT` values already exist (lines 11-12), so no additional timeout states are needed.

### Edge Cases and Pitfalls
- `GameInstance.isTerminalState()` in the server module currently lists all terminal states in a switch. After this change, `PAUSED` and `SAVED` are NOT terminal -- they must not be added to that switch. That is handled separately in T13.
- `Game.executeMove()` checks `gameState != GameState.RUNNING`; `PAUSED` and `SAVED` will correctly block moves since they are not `RUNNING`.

### Test Approach
`tdd`

Test file: `modules/core/src/test/java/io/github/conava/chess/core/logic/game/GameStateTest.java`

- `pausedStateExists`: Assert `GameState.valueOf("PAUSED")` returns a valid constant with message `"Game paused"`.
- `savedStateExists`: Assert `GameState.valueOf("SAVED")` returns a valid constant with message `"Game saved for later"`.
- `totalStateCount`: Assert `GameState.values().length == 16` (14 existing + 2 new).

### Acceptance Criteria
- [ ] `GameState.PAUSED` and `GameState.SAVED` exist with correct messages
- [ ] `GameState.values().length == 16`
- [ ] All existing tests pass

---

## Task T03: Add sqlite-jdbc dependency to server pom.xml

### Context
The server needs SQLite for persistence (users, sessions, games, moves, chat). The `sqlite-jdbc` driver must be added as a compile-scope dependency to the server module's POM.

### Requirements
1. Add `org.xerial:sqlite-jdbc` version `3.45.3.0` (or latest stable 3.45.x) as a compile-scope dependency in `modules/server/pom.xml`.
2. The dependency must appear in the `<dependencies>` section alongside the existing `core` and `junit-jupiter` dependencies.

### Implementation Details
- File: `modules/server/pom.xml`
- Add after the `core` dependency block (around line 29):
  ```xml
  <dependency>
      <groupId>org.xerial</groupId>
      <artifactId>sqlite-jdbc</artifactId>
      <version>3.45.3.0</version>
  </dependency>
  ```
- Also add `org.xerial:sqlite-jdbc` to the `maven-shade-plugin` filter if needed, though the current shade config only excludes core's `MANIFEST.MF` -- the SQLite JAR should be included by default.

### Edge Cases and Pitfalls
- Ensure the shade plugin includes the SQLite native libraries. The `xerial/sqlite-jdbc` fat JAR bundles platform-specific binaries; the shade plugin will include them automatically.
- Do not add this dependency to the root POM or the core/application POMs -- only the server needs it.

### Test Approach
`dry-run`

Run `mvn clean compile -pl modules/server -am` to verify the dependency resolves and the server module compiles.

### Acceptance Criteria
- [ ] `mvn clean compile -pl modules/server -am` succeeds
- [ ] `sqlite-jdbc` JAR appears in the server's classpath
- [ ] No changes to core or application modules

---

## Task T04: ServerConfig: load server.properties

### Context
The server currently hardcodes `MAX_GAMES = 40` and `DEFAULT_PORT = 54321`. A config file allows operators to tune these values without recompilation. `ServerConfig` loads `server.properties` from the classpath or filesystem.

### Requirements
1. Create `ServerConfig` class in `io.github.conava.chess.server.config` package.
2. The class loads properties from a file path (default: `server.properties` in the working directory).
3. Supported keys with defaults: `port` (54321), `max_games` (40), `disconnect_timeout_seconds` (300), `db_path` (`chess.db`), `session_expiry_days` (30).
4. Each property has a typed getter: `getPort()` returns `int`, `getDbPath()` returns `String`, etc.
5. If the properties file does not exist, all defaults are used (no exception thrown, log a warning).
6. Invalid numeric values fall back to defaults with a warning log.

### Implementation Details
- Create: `modules/server/src/main/java/io/github/conava/chess/server/config/ServerConfig.java`
- Create: `modules/server/src/main/resources/server.properties` (the default config file bundled in the JAR)
- The constructor accepts a `String path` parameter. Use `java.util.Properties` to load from `new FileInputStream(path)`. Catch `FileNotFoundException`/`IOException`, log warning, proceed with defaults.
- Store parsed values in `final` fields set during construction.
- Provide a no-arg constructor that delegates to the single-arg constructor with path `"server.properties"`.

### Edge Cases and Pitfalls
- `port` must be validated: 1-65535 range. Out-of-range values fall back to default.
- `max_games` must be > 0. Zero or negative falls back to 40.
- `disconnect_timeout_seconds` must be > 0. Zero means no timeout (forfeit disabled).
- `db_path` is a string; if blank, fall back to `"chess.db"`.

### Test Approach
`tdd`

Test file: `modules/server/src/test/java/io/github/conava/chess/server/config/ServerConfigTest.java`

- `defaultValues_whenNoFile`: Construct with a nonexistent path. Assert all defaults.
- `customValues_whenFileExists`: Write a temp properties file with custom values, construct, assert each getter returns the custom value.
- `invalidPort_fallsBackToDefault`: Write a file with `port=99999`, assert `getPort()` returns 54321.
- `invalidMaxGames_fallsBackToDefault`: Write `max_games=-1`, assert 40.

### Acceptance Criteria
- [ ] `new ServerConfig("nonexistent.properties")` returns all defaults without throwing
- [ ] Custom values are correctly parsed from a real file
- [ ] Invalid values produce warnings and fall back to defaults
- [ ] All tests pass

---

## Task T05: DatabaseManager + schema init

### Context
The server needs a SQLite database for users, sessions, games, moves, and chat. `DatabaseManager` owns the single JDBC connection, runs schema creation DDL on startup, and provides `getConnection()` for repositories.

### Requirements
1. Create `DatabaseManager` in `io.github.conava.chess.server.persistence` package.
2. Constructor accepts `String dbPath` (from `ServerConfig.getDbPath()`).
3. On construction, open a SQLite JDBC connection: `DriverManager.getConnection("jdbc:sqlite:" + dbPath)`.
4. Run schema init: execute the 5 `CREATE TABLE IF NOT EXISTS` statements from the design doc (users, sessions, games, moves, chat_messages).
5. Enable WAL mode: `PRAGMA journal_mode=WAL;` for concurrent read performance.
6. Provide `Connection getConnection()` method.
7. Implement `Closeable` with a `close()` method that closes the JDBC connection.

### Implementation Details
- Create: `modules/server/src/main/java/io/github/conava/chess/server/persistence/DatabaseManager.java`
- Use `java.sql.Connection`, `java.sql.Statement` for DDL.
- Schema DDL is exactly as specified in the design doc. Use `CREATE TABLE IF NOT EXISTS` for idempotency.
- For testing, use `":memory:"` as the db path to get an in-memory SQLite database.
- The `Connection` returned by `getConnection()` is the single shared connection. Callers must not close it.

### Edge Cases and Pitfalls
- SQLite is single-writer. The shared connection plus server-side synchronization (via `GameInstance` and `ClientHandler` synchronized blocks) prevents concurrent writes.
- If the connection cannot be opened (bad path, permissions), throw a `RuntimeException` wrapping the `SQLException` -- the server cannot start without a database.
- Foreign key enforcement: execute `PRAGMA foreign_keys = ON;` after connection opens.

### Test Approach
`tdd`

Test file: `modules/server/src/test/java/io/github/conava/chess/server/persistence/DatabaseManagerTest.java`

- `schemaCreated_onConstruction`: Create with `":memory:"`, query `sqlite_master` for all 5 table names, assert they exist.
- `walModeEnabled`: Query `PRAGMA journal_mode;`, assert returns `"wal"` (note: in-memory DBs may return `"memory"` -- this test should use a temp file instead).
- `foreignKeysEnabled`: Query `PRAGMA foreign_keys;`, assert returns 1.
- `close_closesConnection`: Call `close()`, then `getConnection().isClosed()` returns `true`.

### Acceptance Criteria
- [ ] All 5 tables are created on construction
- [ ] WAL mode and foreign keys are enabled
- [ ] `getConnection()` returns a live connection
- [ ] `close()` properly releases resources
- [ ] All tests pass

---

## Task T06: UserRepository

### Context
The server needs to create and look up user accounts. `UserRepository` provides CRUD operations against the `users` table, used by `AuthService` for registration and login.

### Requirements
1. Create `UserRepository` in `io.github.conava.chess.server.persistence` package.
2. Constructor accepts a `DatabaseManager` instance.
3. Methods:
   - `createUser(String username, String passwordHash, String passwordSalt)`: Inserts a new row, returns the auto-generated `id`. Throws `IllegalArgumentException` if username already exists (catch `SQLException` with unique constraint violation).
   - `findByUsername(String username)`: Returns an `Optional<UserRecord>` where `UserRecord` is a record/inner class with fields `id`, `username`, `passwordHash`, `passwordSalt`, `createdAt`.
   - `findById(int userId)`: Returns an `Optional<UserRecord>`.
4. All SQL uses parameterized `PreparedStatement` -- never string concatenation.

### Implementation Details
- Create: `modules/server/src/main/java/io/github/conava/chess/server/persistence/UserRepository.java`
- `UserRecord` can be a public static inner record: `record UserRecord(int id, String username, String passwordHash, String passwordSalt, long createdAt) {}`
- For `createUser`, use `Statement.RETURN_GENERATED_KEYS` on the `PreparedStatement` to retrieve the auto-increment ID.
- `createdAt` is set to `Instant.now().getEpochSecond()` inside `createUser`.

### Edge Cases and Pitfalls
- Username uniqueness is enforced by the DB schema (`UNIQUE` constraint). Catch `SQLException` and check for constraint violation; rethrow as `IllegalArgumentException("Username already exists")`.
- Usernames should be trimmed and validated: not blank, max 50 characters. Throw `IllegalArgumentException` for invalid input before hitting the DB.

### Test Approach
`tdd`

Test file: `modules/server/src/test/java/io/github/conava/chess/server/persistence/UserRepositoryTest.java`

Use an in-memory `DatabaseManager(":memory:")` for all tests.

- `createUser_returnsPositiveId`: Create a user, assert returned ID > 0.
- `findByUsername_returnsUser`: Create a user, find by username, assert all fields match.
- `findByUsername_returnsEmpty_whenNotFound`: Find a nonexistent username, assert `Optional.empty()`.
- `createUser_throwsOnDuplicateUsername`: Create a user twice with the same username, assert `IllegalArgumentException`.
- `createUser_throwsOnBlankUsername`: Pass blank username, assert `IllegalArgumentException`.

### Acceptance Criteria
- [ ] Users can be created and retrieved by username or ID
- [ ] Duplicate usernames are rejected with a clear exception
- [ ] Blank/invalid usernames are rejected
- [ ] All SQL uses parameterized queries
- [ ] All tests pass

---

## Task T07: SessionRepository

### Context
Auth tokens (sessions) are stored in the `sessions` table. `SessionRepository` creates, looks up, and validates session tokens, used by `AuthService`.

### Requirements
1. Create `SessionRepository` in `io.github.conava.chess.server.persistence` package.
2. Constructor accepts a `DatabaseManager` instance.
3. Methods:
   - `createSession(int userId, String token, int expiryDays)`: Inserts a new session row. `created_at` = now (epoch seconds), `expires_at` = `created_at + expiryDays * 86400`.
   - `findByToken(String token)`: Returns an `Optional<SessionRecord>` with fields `id`, `userId`, `token`, `createdAt`, `expiresAt`.
   - `isTokenValid(String token)`: Returns `true` if the token exists and `expires_at > now`.
   - `deleteByToken(String token)`: Removes the session row (for logout).
   - `deleteExpired()`: Removes all sessions where `expires_at <= now`.
4. `SessionRecord` is a public static inner record.

### Implementation Details
- Create: `modules/server/src/main/java/io/github/conava/chess/server/persistence/SessionRepository.java`
- All SQL uses `PreparedStatement`.
- `isTokenValid` can be a single query: `SELECT 1 FROM sessions WHERE token = ? AND expires_at > ?` with current epoch seconds.

### Edge Cases and Pitfalls
- Token is a UUID string. `findByToken` must handle null input gracefully (return empty).
- `deleteExpired()` should be called periodically or at server startup to clean stale sessions.

### Test Approach
`tdd`

Test file: `modules/server/src/test/java/io/github/conava/chess/server/persistence/SessionRepositoryTest.java`

Use in-memory `DatabaseManager`. Pre-create a user via `UserRepository` for foreign key compliance.

- `createAndFindSession`: Create a session, find by token, assert fields match.
- `isTokenValid_returnsTrue_whenNotExpired`: Create session with 30-day expiry, assert valid.
- `isTokenValid_returnsFalse_whenExpired`: Create session with 0-day expiry (expires_at = created_at), assert invalid.
- `deleteByToken_removesSession`: Create, delete, assert `findByToken` returns empty.
- `deleteExpired_removesOnlyExpired`: Create two sessions (one expired, one not), call `deleteExpired`, assert only the non-expired one remains.

### Acceptance Criteria
- [ ] Sessions can be created, found, validated, and deleted
- [ ] Expired tokens are correctly identified as invalid
- [ ] `deleteExpired()` removes only expired sessions
- [ ] All tests pass

---

## Task T08: GameRepository

### Context
Games, moves, and chat messages are persisted to SQLite. `GameRepository` manages the `games`, `moves`, and `chat_messages` tables, used by `GameInstance` for pause/resume/save and by `ClientHandler` for game history.

### Requirements
1. Create `GameRepository` in `io.github.conava.chess.server.persistence` package.
2. Constructor accepts a `DatabaseManager` instance.
3. Methods:
   - `createGame(int whiteUserId, int blackUserId, String ruleset, int positionIndex)`: Inserts into `games`, returns auto-generated `id`. Sets `state` = `"RUNNING"`, `created_at` and `updated_at` = now.
   - `updateGameState(int gameId, String state)`: Updates `state` and `updated_at`.
   - `setDisconnect(int gameId, int userId)`: Sets `disconnect_user_id` and `disconnect_at`.
   - `clearDisconnect(int gameId)`: Nulls out `disconnect_user_id` and `disconnect_at`.
   - `findGameById(int gameId)`: Returns `Optional<GameRecord>`.
   - `findSavedGamesByUserId(int userId)`: Returns a `List<GameRecord>` where `state = 'PAUSED'` or `state = 'SAVED'` and the user is white or black.
   - `addMove(int gameId, int moveNo, String notation)`: Inserts into `moves`.
   - `getMoves(int gameId)`: Returns `List<MoveRecord>` ordered by `move_no`.
   - `addChatMessage(int gameId, int userId, String content)`: Inserts into `chat_messages`.
   - `getChatMessages(int gameId)`: Returns `List<ChatRecord>` ordered by `sent_at`.
4. Record types: `GameRecord`, `MoveRecord`, `ChatRecord` as public static inner records.

### Implementation Details
- Create: `modules/server/src/main/java/io/github/conava/chess/server/persistence/GameRepository.java`
- `GameRecord`: `record GameRecord(int id, int whiteUserId, int blackUserId, String ruleset, int positionIndex, String state, long createdAt, long updatedAt, Integer disconnectUserId, Long disconnectAt) {}`
- `MoveRecord`: `record MoveRecord(int id, int gameId, int moveNo, String notation, long playedAt) {}`
- `ChatRecord`: `record ChatRecord(int id, int gameId, int userId, String content, long sentAt) {}`
- All timestamps use `Instant.now().getEpochSecond()`.

### Edge Cases and Pitfalls
- `findSavedGamesByUserId` query: `WHERE (white_user_id = ? OR black_user_id = ?) AND state IN ('PAUSED', 'SAVED')`.
- `positionIndex` defaults to -1 for standard games. Must be stored correctly for Chess960 resume.
- `getMoves` must order by `move_no ASC` to ensure correct replay order.

### Test Approach
`tdd`

Test file: `modules/server/src/test/java/io/github/conava/chess/server/persistence/GameRepositoryTest.java`

Use in-memory `DatabaseManager`. Create users first for FK compliance.

- `createGame_returnsPositiveId`: Create a game, assert ID > 0.
- `updateGameState_changesState`: Create game, update to `"PAUSED"`, find by ID, assert state.
- `addAndGetMoves`: Add 3 moves, get moves, assert 3 returned in order.
- `addAndGetChatMessages`: Add 2 chat messages, get them, assert 2 returned in order.
- `findSavedGamesByUserId_filtersCorrectly`: Create 3 games (one RUNNING, one PAUSED, one SAVED for the user), assert only PAUSED and SAVED returned.
- `setAndClearDisconnect`: Set disconnect, verify fields, clear, verify nulled.

### Acceptance Criteria
- [ ] Games, moves, and chat messages can be created and retrieved
- [ ] Game state updates work correctly
- [ ] Saved games can be found by user ID
- [ ] Disconnect tracking works
- [ ] All tests pass

---

## Task T09: AuthService (PBKDF2 hashing, register, login, verify)

### Context
The server needs user authentication. `AuthService` provides registration (username + password hashing), login (password verification + token generation), and token validation. It uses PBKDF2-HMAC-SHA256 for password hashing.

### Requirements
1. Create `AuthService` in `io.github.conava.chess.server.auth` package.
2. Constructor accepts `UserRepository`, `SessionRepository`, and `int sessionExpiryDays` (from `ServerConfig`).
3. Methods:
   - `register(String username, String password)`: Validates input (non-blank, password >= 8 chars), generates a random 16-byte salt, hashes with PBKDF2, creates user via `UserRepository`, creates a session, returns the session token.
   - `login(String username, String password)`: Finds user by username, verifies password hash, creates a session, returns the token. Throws `IllegalArgumentException` on invalid credentials.
   - `verifyToken(String token)`: Returns `Optional<PlayerSession>` (userId + username) if the token is valid and not expired.
   - `logout(String token)`: Deletes the session via `SessionRepository`.
4. PBKDF2 params: 600,000 iterations, SHA-256, 256-bit key length. Salt and hash stored as Base64 strings.
5. Password hashing uses `javax.crypto.SecretKeyFactory` with `PBKDF2WithHmacSHA256`.

### Implementation Details
- Create: `modules/server/src/main/java/io/github/conava/chess/server/auth/AuthService.java`
- Use `java.security.SecureRandom` for salt generation.
- Use `javax.crypto.spec.PBEKeySpec` and `javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")`.
- Token generation: `UUID.randomUUID().toString()`.
- `verifyToken` calls `sessionRepository.isTokenValid(token)`, then `sessionRepository.findByToken(token)` to get `userId`, then `userRepository.findById(userId)` to get `username`. Returns a `PlayerSession` (from T10).

### Edge Cases and Pitfalls
- Registration with a duplicate username: let the `UserRepository.createUser` throw `IllegalArgumentException`, which propagates to the caller.
- Login with wrong password: compute hash with stored salt, compare Base64 strings. Use constant-time comparison (`MessageDigest.isEqual`) to prevent timing attacks.
- Empty/null token in `verifyToken` returns `Optional.empty()`.

### Test Approach
`tdd`

Test file: `modules/server/src/test/java/io/github/conava/chess/server/auth/AuthServiceTest.java`

Use in-memory `DatabaseManager` with real `UserRepository` and `SessionRepository`.

- `register_returnsToken`: Register a user, assert returned token is non-null UUID format.
- `login_succeeds_withCorrectPassword`: Register, then login with same password, assert token returned.
- `login_fails_withWrongPassword`: Register, login with wrong password, assert `IllegalArgumentException`.
- `login_fails_withUnknownUsername`: Login with nonexistent user, assert `IllegalArgumentException`.
- `verifyToken_returnsSession_whenValid`: Register, verify the token, assert `Optional` is present with correct userId.
- `verifyToken_returnsEmpty_whenInvalid`: Verify a random UUID, assert `Optional.empty()`.
- `logout_invalidatesToken`: Register, logout, verify token returns empty.
- `register_fails_withShortPassword`: Register with 3-char password, assert `IllegalArgumentException`.

### Acceptance Criteria
- [ ] Passwords are hashed with PBKDF2, never stored in plaintext
- [ ] Registration creates user + session, returns token
- [ ] Login verifies password and creates session
- [ ] Token verification works for valid and invalid tokens
- [ ] Logout invalidates the token
- [ ] All tests pass

---

## Task T10: PlayerSession data class

### Context
When a client authenticates, the server needs to associate the connection with a user identity. `PlayerSession` is a simple data carrier holding `userId`, `username`, and `token` for the authenticated connection.

### Requirements
1. Create `PlayerSession` in `io.github.conava.chess.server.management` package.
2. Use a Java record: `public record PlayerSession(int userId, String username, String token) {}`.
3. No additional methods beyond what record provides.

### Implementation Details
- Create: `modules/server/src/main/java/io/github/conava/chess/server/management/PlayerSession.java`
- Single-line record declaration.

### Edge Cases and Pitfalls
- None -- this is a simple data carrier.

### Test Approach
`peer-review`

This is a one-line record; no unit test needed. Verified by usage in `AuthService` and `ClientHandler` tests.

### Acceptance Criteria
- [ ] `PlayerSession` record exists with `userId`, `username`, `token` components
- [ ] Compiles without errors

---

## Task T11: Extract GameManager from Server

### Context
`Server` currently owns the game semaphore, game ID counter, and games map directly. This is tech debt item S-001. `GameManager` extracts this responsibility into a dedicated class, making `Server` a thin accept-loop wrapper.

### Requirements
1. Create `GameManager` in `io.github.conava.chess.server.management` package.
2. Move from `Server` to `GameManager`:
   - `Semaphore gameSemaphore` (initialized from `ServerConfig.getMaxGames()`)
   - `AtomicInteger gameIdCounter`
   - `ConcurrentHashMap<Integer, GameInstance> gamesList`
3. `GameManager` methods:
   - `boolean tryAcquireGameSlot()`: calls `gameSemaphore.tryAcquire()`
   - `void releaseGameSlot()`: calls `gameSemaphore.release()`
   - `int nextGameId()`: calls `gameIdCounter.incrementAndGet()`
   - `void addGame(int id, GameInstance game)`
   - `void removeGame(int id)`
   - `GameInstance getGame(int id)`
   - `Map<Integer, GameInstance> getActiveGames()`: returns unmodifiable view
4. Remove `getGameSemaphore()`, `getGameIdCounter()`, `getGamesList()` from `Server`.
5. `Server` constructor accepts `ServerConfig` and creates a `GameManager` from it.
6. `Server.start()` now reads port from `ServerConfig`.
7. Replace `System.out.println` in `printServerStatus()` and `stopServer()` with `LOGGER.info()`.

### Implementation Details
- Create: `modules/server/src/main/java/io/github/conava/chess/server/management/GameManager.java`
- Modify: `modules/server/src/main/java/io/github/conava/chess/server/Server.java`
  - Remove `MAX_GAMES`, `gameSemaphore`, `gameIdCounter`, `gamesList` fields.
  - Add `ServerConfig config` and `GameManager gameManager` fields.
  - `Server()` becomes `Server(ServerConfig config)`.
  - `main()` creates a `ServerConfig()` then `new Server(config).start()`.
  - `start()` reads `config.getPort()` instead of accepting a port parameter. Change signature to `start()` (no args) or keep it and read from config.
  - `acceptClientConnections` passes `gameManager` (or the whole `Server`) to `ClientHandler`. Since T12 refactors `ClientHandler` heavily, for now pass `gameManager` as a new field via `Server.getGameManager()`.
  - `printServerStatus()`: replace all `System.out.println` with `LOGGER.info`.
  - `stopServer()`: use `LOGGER.info` for all output.
- `Server` still owns `connectionsList` since that is connection management, not game management.
- Provide `Server.getGameManager()` for `ClientHandler` to use during the transition (T12 will refactor this further).

### Edge Cases and Pitfalls
- The existing `ClientHandler` directly calls `server.getGameSemaphore()` and `server.getGameIdCounter()`. Since `ClientHandler` is being refactored in T12 (which depends on T11), the approach here is: add `Server.getGameManager()` as a temporary bridge. T12 will switch `ClientHandler` to use `GameManager` directly.
- Existing `ServerTest.java` may reference removed methods. Update or delete broken test code.

### Test Approach
`tdd`

Test file: `modules/server/src/test/java/io/github/conava/chess/server/management/GameManagerTest.java`

- `tryAcquireGameSlot_respectsLimit`: Create `GameManager` with maxGames=2. Acquire twice (both true), third returns false.
- `releaseGameSlot_freesCapacity`: Acquire all slots, release one, acquire again succeeds.
- `nextGameId_incrementsSequentially`: Call 3 times, assert returns 1, 2, 3.
- `addAndGetGame`: Add a `GameInstance`, get by ID, assert not null.
- `removeGame_removesEntry`: Add then remove, assert `getGame` returns null.

### Acceptance Criteria
- [ ] `GameManager` owns semaphore, counter, and games map
- [ ] `Server` no longer exposes `getGameSemaphore()` or `getGameIdCounter()`
- [ ] `Server` reads config from `ServerConfig`
- [ ] `System.out.println` replaced with `LOGGER.info` in Server
- [ ] All tests pass
- [ ] `mvn clean compile -pl modules/server -am` succeeds

---

## Task T12: Refactor ClientHandler: auth gating + new message dispatch

### Context
The current `ClientHandler` allows any client to create/join games without authentication. This task adds auth gating (LOGIN/REGISTER must come first), integrates `PlayerSession`, delegates game lifecycle to `GameManager`, and adds dispatch for new message types (CHAT, QUEUE, DEQUEUE, SAVE_GAME, RESUME_GAME).

### Requirements
1. `ClientHandler` constructor now accepts: `Socket`, `Server`, `GameManager`, `AuthService`, `GameRepository`, `MatchmakingService` (null initially -- wired in T14).
2. Add a `PlayerSession playerSession` field. Initially null (unauthenticated).
3. Auth gating: if `playerSession == null`, only `LOGIN`, `REGISTER`, and `ERROR` messages are accepted. All others return `ERROR "Not authenticated"`.
4. `handleMessage` dispatch table expands:
   - `LOGIN` -> call `authService.login()`, create `PlayerSession`, send `AUTH_TOKEN` response.
   - `REGISTER` -> call `authService.register()`, create `PlayerSession`, send `AUTH_TOKEN` response.
   - `CREATE_GAME` -> require auth, add `token` validation, delegate to `gameManager`.
   - `JOIN_GAME` -> require auth, add `token` validation, delegate to `gameManager`.
   - `CHAT` -> delegate to `gameInstance.handleChat()`.
   - `QUEUE` -> delegate to `matchmakingService.enqueue()`.
   - `DEQUEUE` -> delegate to `matchmakingService.dequeue()`.
   - `SAVE_GAME` -> delegate to `gameInstance.handleSaveGame()`.
   - `RESUME_GAME` -> look up saved game, reconstruct `GameInstance`, send `GAME_HISTORY`.
5. `createGame` and `joinGame` now use `gameManager.tryAcquireGameSlot()`, `gameManager.nextGameId()`, etc. instead of `server.getGameSemaphore()`.
6. Fix S-002: add `server.removeClientHandler(this)` in a `finally` block in `run()` (already present, but verify the cleanup path is robust even if `addClientHandler` was never called).
7. Fix S-003: after `game.movePiece`, relay `move.toProtocolString()` instead of raw client string. This is actually handled in T13 (`GameInstance.handleMove`), but `ClientHandler` must pass the authenticated user context.

### Implementation Details
- Modify: `modules/server/src/main/java/io/github/conava/chess/server/management/ClientHandler.java`
- Login handling:
  ```
  String username = message.getParameterValue("username");
  String password = message.getParameterValue("password");
  String token = authService.login(username, password);
  playerSession = new PlayerSession(userId, username, token);
  sendMessage(new Message(MessageType.AUTH_TOKEN, "token=" + token + " userId=" + userId));
  ```
- Register handling: similar but calls `authService.register()`.
- On `LOGIN`/`REGISTER` error (IllegalArgumentException), send `ERROR` message with the exception message.
- `cleanup()` method: if `playerSession != null`, call `authService.logout(playerSession.token())` -- or not, since tokens persist across sessions. Actually, per the design doc, tokens survive restarts. So do NOT logout on disconnect. Just clean up the game instance.
- `RESUME_GAME` handling: parse `gameId` from message, call `gameRepository.findGameById()`, call `gameRepository.getMoves()`, send `GAME_HISTORY` with serialized moves list and ruleset info, then reconnect to the existing `GameInstance` (or create a new one if it's a cold resume).

### Edge Cases and Pitfalls
- A client that sends `CREATE_GAME` before `LOGIN` must get an `ERROR` response, not a crash.
- If `authService` is null (server not configured with auth), skip auth gating entirely -- but per the design doc, auth is always required. So `authService` must never be null.
- `RESUME_GAME` for a game that is still live in memory vs. a cold resume from DB are different paths. Live resume: reconnect the `ClientHandler` to the existing `GameInstance`. Cold resume: create a new `GameInstance`, replay moves, wait for opponent.
- Thread safety: `playerSession` is set once and read many times. Volatile or synchronized access is sufficient since it's set only in the message-processing thread.

### Test Approach
`tdd`

Update: `modules/server/src/test/java/io/github/conava/chess/server/management/ClientHandlerIntegrationTest.java`

- `unauthenticatedClient_getsError_onCreateGame`: Send CREATE_GAME without LOGIN, assert ERROR response.
- `login_succeeds_andReturnsToken`: Send LOGIN with valid credentials, assert AUTH_TOKEN response.
- `register_succeeds_andReturnsToken`: Send REGISTER, assert AUTH_TOKEN response.
- `authenticatedClient_canCreateGame`: LOGIN, then CREATE_GAME, assert JOIN_CODE response.
- `login_fails_withWrongPassword`: Send LOGIN with bad password, assert ERROR response.

### Acceptance Criteria
- [ ] Unauthenticated clients cannot create/join games
- [ ] LOGIN and REGISTER work correctly
- [ ] AUTH_TOKEN response contains token and userId
- [ ] New message types are dispatched correctly
- [ ] `GameManager` is used instead of raw `Server` getters for game lifecycle
- [ ] All tests pass

---

## Task T13: Refactor GameInstance: pause/resume/save, promotion fix, chat relay

### Context
`GameInstance` currently handles only basic move relay and resignation. This task adds pause-on-disconnect (instead of immediate resignation), mutual save-for-later, chat message relay, promotion bug fix (S-004), and normalized move relay (S-003).

### Requirements
1. **Promotion fix (S-004):** In `handleMove`, after deserializing the move, check `if (move instanceof PromotionMove pm)` and call `game.promoteMove(pm.getStart(), pm.getEnd(), pm.getTargetPiece().getType())` instead of `game.movePiece(start, end)`. For non-promotion moves, continue using `game.movePiece(start, end)`.
2. **Normalized move relay (S-003):** After successful move execution, relay `move.toProtocolString()` instead of the raw client-supplied `message.content()`.
3. **Pause on disconnect:** When `disconnectPlayer` is called and the game is non-terminal:
   - Do NOT award resignation. Instead, set game state to `PAUSED`.
   - Store `disconnect_at` and `disconnect_user_id` via `gameRepository.setDisconnect()`.
   - Update game state in DB via `gameRepository.updateGameState(gameId, "PAUSED")`.
   - Send `GAME_STATUS gameState=PAUSED` to the remaining player.
   - Start a `ScheduledExecutorService` timer for `disconnectTimeoutSeconds`. On timeout, award forfeit to the remaining player (set state to `WHITE_WON_BY_TIMEOUT` or `BLACK_WON_BY_TIMEOUT`), update DB, notify remaining player.
4. **Reconnect:** Add `reconnectPlayer(ClientHandler handler, PlayerSession session)` method. Called by `ClientHandler` on `RESUME_GAME`. Cancels the disconnect timer, clears disconnect in DB, sets state back to `RUNNING`, sends `GAME_STATUS gameState=RUNNING` to both players.
5. **Mutual save-for-later:**
   - Track `boolean whiteSaveRequested` and `boolean blackSaveRequested` flags.
   - On receiving `SAVE_GAME` from one player: set their flag, forward `SAVE_GAME` to opponent.
   - When both flags are set: save game to DB as `SAVED`, send `SAVE_ACCEPTED` to both, set game state to `SAVED`.
6. **Chat relay:** On `CHAT` message from a player:
   - Prepend `from=<username>` to the message.
   - Store via `gameRepository.addChatMessage()`.
   - Relay to both players.
7. **Move persistence:** After each successful move, call `gameRepository.addMove(gameId, moveNo, move.toProtocolString())`.
8. Constructor now accepts additional parameters: `GameRepository`, `GameManager`, `int disconnectTimeoutSeconds`.
9. Add `isTerminalState` to also return `false` for `PAUSED` and `SAVED` (they are non-terminal but not RUNNING either). Actually, `PAUSED` and `SAVED` should not trigger the terminal notification. The current implementation already handles this correctly since the switch only matches specific terminal states.

### Implementation Details
- Modify: `modules/server/src/main/java/io/github/conava/chess/server/management/GameInstance.java`
- For the disconnect timer: use a `ScheduledFuture<?>` field. Create via `Executors.newSingleThreadScheduledExecutor()`. The timer task runs `handleDisconnectTimeout()` which sets the game state to the appropriate timeout state.
- For promotion fix: in `handleMove`, after deserializing the move:
  ```java
  if (move instanceof PromotionMove pm) {
      game.promoteMove(move.getStart(), move.getEnd(), pm.getTargetPiece().getType());
  } else {
      game.movePiece(move.getStart(), move.getEnd());
  }
  ```
- For normalized relay: build the relay message from the executed move:
  ```java
  String normalizedContent = "move=" + move.toProtocolString() + " playerColor=" + playerColor;
  sendMessageToPlayers(new Message(MessageType.MOVE, normalizedContent));
  ```
- Store the `dbGameId` (database game ID, separate from the in-memory `gameId`) as a field. Set it when the game is created or resumed.
- The `whitePlayerSession` and `blackPlayerSession` fields (type `PlayerSession`) are needed for chat messages (to get the username) and for DB operations (to get the userId). These are set in `connectPlayer`.

### Edge Cases and Pitfalls
- If both players disconnect simultaneously, neither gets a timeout -- the game stays PAUSED until one reconnects or the timeout fires.
- The disconnect timer must be cancelled on reconnect AND on mutual save.
- `PromotionMove.getTargetPiece()` returns a `Piece` instance. To get the `Pieces` enum for `game.promoteMove()`, use `pm.getTargetPiece().getType()`.
- Chat content validation: strip newlines (they would break the line-delimited protocol). Max length 500 chars.
- When the game is created, persist it to DB via `gameRepository.createGame()` so that `dbGameId` is available for move/chat persistence.

### Test Approach
`tdd`

Update: `modules/server/src/test/java/io/github/conava/chess/server/management/GameInstanceTest.java`

- `promotionMove_executesCorrectly`: Send a promotion move string (e.g., `a7-a8=QUEEN`), assert the promoted piece is a Queen on a8.
- `moveRelay_usesNormalizedString`: Execute a move, capture the relayed message, assert it uses `toProtocolString()` format.
- `disconnect_pausesGame_insteadOfResignation`: Disconnect white player, assert game state is `PAUSED`, assert black receives `GAME_STATUS gameState=PAUSED`.
- `reconnect_resumesGame`: Disconnect then reconnect, assert game state is `RUNNING`.
- `disconnectTimeout_awardsForfeit`: Disconnect with a 1-second timeout, wait, assert terminal state.
- `mutualSave_savesGame`: Both players send `SAVE_GAME`, assert both receive `SAVE_ACCEPTED`, game state is `SAVED`.
- `chat_relayedToBothPlayers`: Player sends CHAT, assert both players receive it with `from=` prepended.

### Acceptance Criteria
- [ ] Promotion moves execute correctly on the server
- [ ] Move relay uses normalized `toProtocolString()` format
- [ ] Disconnect pauses the game instead of resigning
- [ ] Reconnect resumes a paused game
- [ ] Disconnect timeout awards forfeit
- [ ] Mutual save works (both confirm, game saved)
- [ ] Chat messages are relayed with username and persisted
- [ ] Moves are persisted to DB
- [ ] All tests pass

---

## Task T14: MatchmakingService

### Context
Players can queue for automatic matchmaking instead of using join codes. `MatchmakingService` maintains per-ruleset FIFO queues and pairs players when two are queued for the same ruleset.

### Requirements
1. Create `MatchmakingService` in `io.github.conava.chess.server.matchmaking` package.
2. Constructor accepts `GameManager`.
3. Internal state: `Map<RulesetOptions, ConcurrentLinkedDeque<QueueEntry>>` where `QueueEntry` holds `ClientHandler` and `PlayerSession`.
4. Methods:
   - `void enqueue(ClientHandler handler, PlayerSession session, RulesetOptions ruleset)`: Adds to the queue for the given ruleset. Calls `tryMatch(ruleset)` immediately after.
   - `void dequeue(ClientHandler handler)`: Removes the handler from whichever queue it's in.
   - `void tryMatch(RulesetOptions ruleset)`: If the queue for this ruleset has >= 2 entries, dequeue two, create a `GameInstance` via `GameManager`, connect both players, send `MATCHED` to both.
5. `MATCHED` message payload is identical to `JOIN_CODE` payload (contains `joinCode=<gameId>` and optionally `position=N ruleset=CHESS960`).
6. Thread safety: `enqueue`, `dequeue`, and `tryMatch` must be synchronized or use concurrent structures to prevent race conditions.

### Implementation Details
- Create: `modules/server/src/main/java/io/github/conava/chess/server/matchmaking/MatchmakingService.java`
- Use `ConcurrentLinkedDeque` for each queue. The `tryMatch` method polls two entries atomically (synchronized on the deque or the service).
- `QueueEntry` can be a private inner record: `record QueueEntry(ClientHandler handler, PlayerSession session) {}`.
- On match: `gameManager.tryAcquireGameSlot()`, `gameManager.nextGameId()`, create `GameInstance`, connect both players, `gameManager.addGame()`.
- If `tryAcquireGameSlot()` fails, put both entries back in the queue and send an `ERROR "Server full"` to both (or just wait). For simplicity, send ERROR to both and do not re-enqueue.
- `dequeue` must handle the case where the handler is not in any queue (no-op).

### Edge Cases and Pitfalls
- A player disconnecting while in the queue: `ClientHandler.cleanup()` must call `matchmakingService.dequeue(this)`.
- Chess960 matching: the server generates the position index when creating the GameInstance, same as current behavior.
- If a player is already in a game, they should not be able to queue. This validation can happen in `ClientHandler` before calling `enqueue`.

### Test Approach
`tdd`

Test file: `modules/server/src/test/java/io/github/conava/chess/server/matchmaking/MatchmakingServiceTest.java`

Use mock `ClientHandler` objects (or minimal stubs) and a real `GameManager`.

- `enqueue_andMatch_whenTwoPlayersQueue`: Enqueue two players for STANDARD, assert both receive `MATCHED`, assert a `GameInstance` exists in `GameManager`.
- `enqueue_noMatch_whenOnlyOnePlayer`: Enqueue one player, assert no match, no MATCHED message.
- `dequeue_removesPlayerFromQueue`: Enqueue one, dequeue, enqueue another, assert no match (only one in queue).
- `separateQueues_perRuleset`: Enqueue one for STANDARD and one for CHESS960, assert no match. Enqueue a second STANDARD player, assert match.

### Acceptance Criteria
- [ ] Two players queueing for the same ruleset are matched
- [ ] Different rulesets have separate queues
- [ ] Dequeue removes the player correctly
- [ ] Matched players receive MATCHED messages
- [ ] All tests pass

---

## Task T15: SettingsService: auth token persistence

### Context
The application needs to persist the user's auth token and userId across restarts so they don't have to log in every time. `SettingsService` already uses `java.util.prefs.Preferences` for settings; auth credentials are added to the same mechanism.

### Requirements
1. Add two new keys to `SettingsService`: `KEY_AUTH_TOKEN = "authToken"` and `KEY_USER_ID = "userId"`.
2. New methods:
   - `void saveAuthToken(String token)`: persists the token.
   - `String loadAuthToken()`: returns the stored token, or `""` if none.
   - `void saveUserId(int userId)`: persists the user ID.
   - `int loadUserId()`: returns the stored user ID, or `-1` if none.
   - `void clearAuth()`: removes both `authToken` and `userId` keys.

### Implementation Details
- Modify: `modules/application/src/main/java/io/github/conava/chess/application/settings/SettingsService.java`
- Use `prefs.put(KEY_AUTH_TOKEN, token)` and `prefs.get(KEY_AUTH_TOKEN, "")`.
- Use `prefs.putInt(KEY_USER_ID, userId)` and `prefs.getInt(KEY_USER_ID, -1)`.
- `clearAuth()`: `prefs.remove(KEY_AUTH_TOKEN); prefs.remove(KEY_USER_ID);`

### Edge Cases and Pitfalls
- `loadAuthToken()` returning `""` means no token is stored -- the caller must check.
- `loadUserId()` returning `-1` means no user ID is stored.
- `clearAuth()` should be called on explicit logout.

### Test Approach
`tdd`

Add tests to the existing `SettingsService` test file or create one.

- `saveAndLoadAuthToken`: Save a token, load it, assert matches.
- `loadAuthToken_returnsEmpty_whenNotSet`: Assert returns `""`.
- `clearAuth_removesBothKeys`: Save auth data, clear, assert both return defaults.

### Acceptance Criteria
- [ ] Auth token and userId can be saved, loaded, and cleared
- [ ] Default values are returned when nothing is stored
- [ ] All tests pass

---

## Task T16: Chess.java facade: auth + matchmaking + resume methods

### Context
The `Chess` facade must expose new methods for authentication, matchmaking, and game resumption. These are called by the UI controllers and delegate to the appropriate services and server communication.

### Requirements
1. Add auth state fields: `String authToken`, `int userId`, `String username`.
2. New facade methods:
   - `void login(String serverIp, int serverPort, String username, String password, Consumer<Boolean> callback)`: Connects to server, sends LOGIN, receives AUTH_TOKEN, stores token/userId in `SettingsService`, calls callback with success/failure. Runs network I/O on a background thread.
   - `void register(String serverIp, int serverPort, String username, String password, Consumer<Boolean> callback)`: Same pattern for REGISTER.
   - `void logout()`: Clears auth state, calls `settingsService.clearAuth()`.
   - `String getAuthToken()`: Returns the current auth token.
   - `int getUserId()`: Returns the current user ID.
   - `String getUsername()`: Returns the current username.
   - `boolean isAuthenticated()`: Returns `authToken != null && !authToken.isEmpty()`.
   - `void joinMatchmakingQueue(RulesetOptions ruleset)`: Sends `QUEUE` message to server.
   - `void leaveMatchmakingQueue()`: Sends `DEQUEUE` message to server.
   - `void resumeSavedGame(int gameId)`: Sends `RESUME_GAME` message to server.
3. On application start, check `settingsService.loadAuthToken()` -- if non-empty, set auth state fields.

### Implementation Details
- Modify: `modules/application/src/main/java/io/github/conava/chess/application/Chess.java`
- The `login` and `register` methods need a temporary TCP connection to the server to send the auth message and receive the response. This is separate from the game connection. Use a simple socket connection on a daemon thread, send the message, read one response, close.
- Store `SettingsService` as a field in `Chess` (it's currently a local variable in `start()`). It needs to be accessible from the facade methods.
- `joinMatchmakingQueue` and `leaveMatchmakingQueue` use the active `ServerCommunicationTask` connection. They require an active connection (throw `IllegalStateException` if not connected).
- `resumeSavedGame` sends `RESUME_GAME:token=<authToken> gameId=<gameId>`.

### Edge Cases and Pitfalls
- `login`/`register` callbacks must run on a background thread since they involve network I/O. The callback itself should be invoked on the FX Application Thread via `Platform.runLater`.
- If the auth token stored in preferences is expired (server rejects it), the app must clear it and prompt for login.
- `SettingsService` must be elevated from a local variable in `start()` to a field in `Chess` so it can be accessed by facade methods.

### Test Approach
`smoke-test`

The auth flow involves real network I/O. Test manually with a running server. Unit-test only the state management (setting/clearing auth fields).

### Acceptance Criteria
- [ ] `Chess.login()` and `Chess.register()` connect, authenticate, and store token
- [ ] `Chess.logout()` clears auth state
- [ ] `Chess.isAuthenticated()` reflects current state
- [ ] `SettingsService` is a field accessible to facade methods
- [ ] Auth token is loaded from preferences on startup

---

## Task T17: LoginController + login.fxml

### Context
Users need a login screen to authenticate before accessing online features. The login screen presents username and password fields, a login button, and a link to the registration screen.

### Requirements
1. Create `LoginController` in `io.github.conava.chess.application.controllers`.
2. Create `login.fxml` in `modules/application/src/main/resources/fxml/`.
3. UI elements:
   - Title label: "Login" (i18n key: `login.title`)
   - TextField for username (`fx:id="usernameField"`)
   - PasswordField for password (`fx:id="passwordField"`)
   - TextField for server IP (`fx:id="ipField"`, default "localhost")
   - TextField for server port (`fx:id="portField"`, default "54321")
   - Button "Login" (`onAction="#onLogin"`)
   - Hyperlink "Create an account" (`onAction="#onRegister"`) -- navigates to register screen
   - Label for error messages (`fx:id="errorLabel"`, initially hidden)
4. `onLogin()`: Validate inputs (non-blank), call `chess.login(ip, port, username, password, callback)`. On success, navigate to main menu. On failure, show error message.
5. `onRegister()`: Navigate to register screen via `sceneManager.showRegister()`.
6. Constructor accepts `SceneManager`, `Chess`, `I18n`.

### Implementation Details
- Create: `modules/application/src/main/java/io/github/conava/chess/application/controllers/LoginController.java`
- Create: `modules/application/src/main/resources/fxml/login.fxml`
- FXML layout: a centered VBox with the fields, similar style to the existing setup overlays.
- The controller calls `chess.login()` which does the network I/O on a background thread. On callback (FX thread), either navigate to main menu or show error.
- Use `styleClass="root"` on the root element for consistent theming.

### Edge Cases and Pitfalls
- Disable the login button during the network call to prevent double-submission.
- If the server is unreachable, the error callback should display "Cannot connect to server".
- IP and port validation: reuse the same logic from `OnlineSetupController`.

### Test Approach
`manual-verification`

Run the application, navigate to login screen, verify UI layout and login flow.

### Acceptance Criteria
- [ ] Login screen displays with all required fields
- [ ] Successful login navigates to main menu
- [ ] Failed login shows error message
- [ ] "Create an account" link navigates to register screen
- [ ] Button is disabled during network call

---

## Task T18: RegisterController + register.fxml

### Context
New users need to create an account. The register screen collects username, password (with confirmation), and server connection details.

### Requirements
1. Create `RegisterController` in `io.github.conava.chess.application.controllers`.
2. Create `register.fxml` in `modules/application/src/main/resources/fxml/`.
3. UI elements:
   - Title label: "Register" (i18n key: `register.title`)
   - TextField for username
   - PasswordField for password
   - PasswordField for password confirmation
   - TextField for server IP (default "localhost")
   - TextField for server port (default "54321")
   - Button "Register" (`onAction="#onRegister"`)
   - Hyperlink "Already have an account? Login" (`onAction="#onLogin"`)
   - Label for error messages
4. `onRegister()`: Validate inputs (non-blank, passwords match, password >= 8 chars), call `chess.register()`. On success, navigate to main menu. On failure, show error.
5. `onLogin()`: Navigate to login screen.
6. Constructor accepts `SceneManager`, `Chess`, `I18n`.

### Implementation Details
- Create: `modules/application/src/main/java/io/github/conava/chess/application/controllers/RegisterController.java`
- Create: `modules/application/src/main/resources/fxml/register.fxml`
- Same layout style as login screen.
- Password confirmation validation: compare the two fields, show error if mismatch.
- Password length validation: minimum 8 characters.

### Edge Cases and Pitfalls
- "Username already exists" error from the server must be displayed clearly.
- The register button should be disabled during network call.

### Test Approach
`manual-verification`

### Acceptance Criteria
- [ ] Register screen displays with all required fields
- [ ] Password mismatch shows error before network call
- [ ] Short password shows error before network call
- [ ] Successful registration navigates to main menu
- [ ] "Already have an account?" navigates to login screen

---

## Task T19: SceneManager: showLogin + showRegister

### Context
`SceneManager` needs new navigation methods to show the login and register screens, used by the main menu and auth controllers.

### Requirements
1. Add `showLogin()` method: loads `login.fxml` with a `LoginController`.
2. Add `showRegister()` method: loads `register.fxml` with a `RegisterController`.
3. Both use the same `swapScene` pattern as `showMainMenu()`.

### Implementation Details
- Modify: `modules/application/src/main/java/io/github/conava/chess/application/navigation/SceneManager.java`
- Add constants: `FXML_LOGIN = "/fxml/login.fxml"`, `FXML_REGISTER = "/fxml/register.fxml"`.
- `showLogin()`:
  ```java
  var controller = new LoginController(this, chess, i18n);
  swapScene(FXML_LOGIN, controller, 900, 650);
  ```
- `showRegister()`:
  ```java
  var controller = new RegisterController(this, chess, i18n);
  swapScene(FXML_REGISTER, controller, 900, 650);
  ```

### Edge Cases and Pitfalls
- The login/register screens use the same window size as the main menu (900x650).
- `primaryStage.setMaximized(false)` should be called, same as `showMainMenu()`.

### Test Approach
`manual-verification`

### Acceptance Criteria
- [ ] `showLogin()` navigates to the login screen
- [ ] `showRegister()` navigates to the register screen
- [ ] Theme is applied correctly to both screens

---

## Task T20: MainMenuController: auth gating + username display

### Context
The main menu must gate online play behind authentication. If the user is not authenticated, clicking "Online Game" should redirect to the login screen. The username should be displayed in the header.

### Requirements
1. If `chess.isAuthenticated()`, show a welcome label: "Welcome, <username>".
2. If `chess.isAuthenticated()`, the online game button works as before.
3. If NOT authenticated, clicking "Online Game" navigates to the login screen instead of showing the online setup overlay.
4. Add a "Logout" button (visible only when authenticated) that calls `chess.logout()` and refreshes the menu.
5. Modify `main-menu.fxml` to include the welcome label and logout button.

### Implementation Details
- Modify: `modules/application/src/main/java/io/github/conava/chess/application/controllers/MainMenuController.java`
- Modify: `modules/application/src/main/resources/fxml/main-menu.fxml`
- Add `@FXML Label welcomeLabel` and `@FXML Button logoutBtn` to the controller.
- In `initialize()`: if `chess.isAuthenticated()`, set `welcomeLabel.setText("Welcome, " + chess.getUsername())` and show the logout button. Otherwise, hide both.
- Modify `onOnlineGame()`: add `if (!chess.isAuthenticated()) { sceneManager.showLogin(); return; }` at the top.
- `onLogout()`: call `chess.logout()`, then `sceneManager.showMainMenu()` to refresh.

### Edge Cases and Pitfalls
- After logout, the menu should refresh to hide the welcome label and logout button.
- The welcome label should use an i18n key: `menu.welcome` with `{0}` placeholder for username.
- Local (offline) games should always be available regardless of auth status.

### Test Approach
`manual-verification`

### Acceptance Criteria
- [ ] Authenticated users see their username and a logout button
- [ ] Unauthenticated users clicking "Online Game" are redirected to login
- [ ] Logout clears auth and refreshes the menu
- [ ] Local game button always works regardless of auth

---

## Task T21: ServerCommunicationTask: handle new message types

### Context
`ServerCommunicationTask` reads messages from the server and dispatches them to the `OnlineGame` message handler. New message types (`CHAT`, `SAVE_GAME`, `SAVE_ACCEPTED`, `MATCHED`, `GAME_HISTORY`) need to be routed to new callbacks.

### Requirements
1. The existing `Consumer<Message> messageHandler` continues to handle all game-related messages (MOVE, GAME_STATUS, SUCCESS, ERROR, FAILURE, JOIN_CODE).
2. Add additional optional callbacks for new message types:
   - `Consumer<Message> chatHandler`: called on `CHAT` messages.
   - `Consumer<Message> matchHandler`: called on `MATCHED` messages.
   - `Runnable saveAcceptedHandler`: called on `SAVE_ACCEPTED` messages.
   - `Consumer<Message> gameHistoryHandler`: called on `GAME_HISTORY` messages.
   - `Consumer<Message> saveGameHandler`: called on `SAVE_GAME` messages (opponent requested save).
3. These callbacks are set via setter methods (not constructor params, to avoid a massive constructor).
4. In the message loop, check the message type and route to the appropriate handler.

### Implementation Details
- Modify: `modules/application/src/main/java/io/github/conava/chess/application/network/ServerCommunicationTask.java`
- Add fields:
  ```java
  private volatile Consumer<Message> chatHandler;
  private volatile Consumer<Message> matchHandler;
  private volatile Consumer<Message> gameHistoryHandler;
  private volatile Consumer<Message> saveGameHandler;
  private volatile Runnable saveAcceptedHandler;
  ```
- Add setter methods for each.
- In `run()`, before dispatching to the general `messageHandler`, check the type:
  ```java
  switch (decoded.type()) {
      case CHAT -> { if (chatHandler != null) chatHandler.accept(decoded); }
      case MATCHED -> { if (matchHandler != null) matchHandler.accept(decoded); }
      case SAVE_ACCEPTED -> { if (saveAcceptedHandler != null) saveAcceptedHandler.run(); }
      case GAME_HISTORY -> { if (gameHistoryHandler != null) gameHistoryHandler.accept(decoded); }
      case SAVE_GAME -> { if (saveGameHandler != null) saveGameHandler.accept(decoded); }
      default -> messageHandler.accept(decoded);
  }
  ```

### Edge Cases and Pitfalls
- Handlers may be set after the task starts (they're set when the game screen opens). Use `volatile` fields and null-check before calling.
- If no handler is set for a type, fall through to the default `messageHandler` so the message isn't silently dropped.
- `AUTH_TOKEN` responses should NOT go through this path -- they're handled by the auth-specific connection in `Chess.login()`/`Chess.register()`.

### Test Approach
`smoke-test`

Test with a running server. Verify that chat messages reach the chat handler and not the game handler.

### Acceptance Criteria
- [ ] New message types are routed to the correct handlers
- [ ] Unset handlers fall through to the default handler
- [ ] Existing message routing is unchanged
- [ ] No null pointer exceptions when handlers are not set

---

## Task T22: OnlineGame: handle PAUSED state + SAVE_GAME negotiation + GAME_HISTORY replay

### Context
The `OnlineGame` class in core needs to understand the new `PAUSED` and `SAVED` game states, handle `SAVE_GAME` and `SAVE_ACCEPTED` messages, and support game history replay for reconnection.

### Requirements
1. In `handleMessage`, add cases for:
   - `SAVE_GAME`: set a flag `saveRequested = true`, notify observers (so UI can show "opponent wants to save").
   - `SAVE_ACCEPTED`: set game state to `SAVED`, notify observers.
   - `GAME_HISTORY`: parse the moves list, replay them on the board to reconstruct game state.
2. In `handleGameStatus`: already handles `PAUSED` via `GameState.valueOf()`. No change needed -- it will automatically set `gameState = PAUSED` and notify observers.
3. Add `sendSaveGameRequest()` method: sends `SAVE_GAME` message to server.
4. Add `boolean isSaveRequested()` getter for the UI to query.
5. Add `replayGameHistory(Message message)` method: parses `moves=e2-e4,e7-e5,...`, `ruleset=STANDARD`, `position=-1` from the message, initializes the board if not yet initialized, and replays each move.

### Implementation Details
- Modify: `modules/core/src/main/java/io/github/conava/chess/core/logic/game/OnlineGame.java`
- Add field: `private boolean saveRequested = false;`
- In `handleMessage`, add cases:
  ```java
  case SAVE_GAME -> handleSaveGame(message);
  case SAVE_ACCEPTED -> handleSaveAccepted(message);
  case GAME_HISTORY -> replayGameHistory(message);
  ```
- `handleSaveGame`: set `saveRequested = true`, call `notifyObservers()`.
- `handleSaveAccepted`: set `gameState = GameState.SAVED`, call `notifyObservers()`.
- `replayGameHistory`: parse the message content, build the ruleset from params, initialize the board if needed, then for each move in the comma-separated list: deserialize and execute via `super.executeMove()`.
- `sendSaveGameRequest()`: `sendMessageToServer(new Message(MessageType.SAVE_GAME, "gameId=" + joinCode));`

### Edge Cases and Pitfalls
- `GAME_HISTORY` with an empty moves list (game was paused before any moves): just initialize the board, don't try to replay.
- The `replayGameHistory` method must set `gameState = RUNNING` after replaying all moves and call `notifyObservers()`.
- Move replay must use `super.executeMove()` (bypassing the local-player-turn check in `OnlineGame.executeMove()`).
- The turn count and move list must be correct after replay.

### Test Approach
`tdd`

Update: `modules/core/src/test/java/io/github/conava/chess/core/logic/game/OnlineGameServerConnectionTest.java`

- `handleSaveGame_setsFlagAndNotifies`: Send a `SAVE_GAME` message, assert `isSaveRequested()` is true and observer was notified.
- `handleSaveAccepted_setsStateSaved`: Send a `SAVE_ACCEPTED` message, assert game state is `SAVED`.
- `replayGameHistory_reconstructsBoard`: Send a `GAME_HISTORY` message with 4 moves, assert the board state matches after replaying, turn count is 4.
- `replayGameHistory_emptyMoves_justInitializesBoard`: Send `GAME_HISTORY` with empty moves, assert board is initialized and state is `RUNNING`.
- `handleGameStatus_paused_setsState`: Send `GAME_STATUS gameState=PAUSED`, assert game state is `PAUSED`.

### Acceptance Criteria
- [ ] `PAUSED` game state is handled correctly
- [ ] Save request flag is set and observers notified
- [ ] `SAVE_ACCEPTED` transitions to `SAVED` state
- [ ] Game history replay correctly reconstructs the board
- [ ] All tests pass

---

## Task T23: OnlineSetupController: "Find Match" button + matchmaking settings

### Context
The online setup screen needs a third option beyond "Create" and "Join": "Find Match" for automatic matchmaking. This queues the player for a match with a random opponent.

### Requirements
1. Add a third `ToggleButton` "Find Match" (`fx:id="findMatchToggle"`) to the mode group.
2. When "Find Match" is selected:
   - Hide the join code field (same as create mode).
   - Show the ruleset selector.
   - The connect button text changes to "Find Match".
3. `OnlineSetupController` exposes a `getMode()` method returning `CREATE`, `JOIN`, or `FIND_MATCH`.
4. `onConnect()` validates and confirms as before. The caller (`MainMenuController`) reads `getMode()` to decide whether to start a direct game or queue for matchmaking.

### Implementation Details
- Modify: `modules/application/src/main/java/io/github/conava/chess/application/controllers/OnlineSetupController.java`
- Modify: `modules/application/src/main/resources/fxml/online-setup.fxml`
- Add an enum or string constant for the mode.
- Update `updateJoinCodeVisibility()` to handle three modes: create (show ruleset, hide join code), join (hide ruleset, show join code), find match (show ruleset, hide join code).
- The caller in `MainMenuController.onOnlineGame()` must be updated to check the mode and either start a direct game or navigate to the waiting-for-match screen.

### Edge Cases and Pitfalls
- "Find Match" requires authentication. The auth check is already done in `MainMenuController.onOnlineGame()` (from T20).
- IP and port are still needed for "Find Match" since the client connects to a server.

### Test Approach
`manual-verification`

### Acceptance Criteria
- [ ] "Find Match" toggle appears in the online setup overlay
- [ ] Selecting "Find Match" shows the correct fields
- [ ] `getMode()` returns the correct value for each toggle
- [ ] Caller handles all three modes

---

## Task T24: WaitingForMatchController + waiting-for-match.fxml

### Context
After clicking "Find Match", the user sees a waiting screen until the server matches them with an opponent. This screen shows a spinner and a cancel button.

### Requirements
1. Create `WaitingForMatchController` in `io.github.conava.chess.application.controllers`.
2. Create `waiting-for-match.fxml` in `modules/application/src/main/resources/fxml/`.
3. UI elements:
   - Title: "Searching for opponent..." (i18n key: `matchmaking.searching`)
   - ProgressIndicator (indeterminate spinner)
   - Label showing the selected ruleset
   - Button "Cancel" that leaves the queue and returns to main menu
4. Constructor accepts `SceneManager`, `Chess`, `I18n`, `RulesetOptions`.
5. On construction, call `chess.joinMatchmakingQueue(ruleset)`.
6. Register a match handler on the `ServerCommunicationTask` that triggers navigation to the game screen when `MATCHED` is received.
7. Cancel button: calls `chess.leaveMatchmakingQueue()` and navigates to main menu.

### Implementation Details
- Create: `modules/application/src/main/java/io/github/conava/chess/application/controllers/WaitingForMatchController.java`
- Create: `modules/application/src/main/resources/fxml/waiting-for-match.fxml`
- The match handler callback runs on the FX thread (via `Platform.runLater`): it starts the game and navigates to the game screen.
- When `MATCHED` arrives, it has the same payload as `JOIN_CODE`. The controller must extract the game info and call `chess.startGame()` with the match details.

### Edge Cases and Pitfalls
- If the user closes the window while waiting, ensure `leaveMatchmakingQueue` is called.
- If the server connection drops while waiting, show an error and return to menu.
- The cancel button must be responsive even while waiting (no blocking on the FX thread).

### Test Approach
`manual-verification`

### Acceptance Criteria
- [ ] Waiting screen shows spinner and ruleset name
- [ ] Cancel button leaves queue and returns to menu
- [ ] Matched message triggers game start
- [ ] Server disconnect shows error

---

## Task T25: GameController: chat panel + Save & Exit button

### Context
The in-game screen needs a chat panel on the left side and a "Save & Exit" button for online games. This is the main UI change for the chat feature.

### Requirements
1. Add a chat panel to the left VBox in `game.fxml`:
   - A `ListView<String>` for chat messages (`fx:id="chatList"`)
   - A `TextField` for input (`fx:id="chatInput"`)
   - A "Send" button (`onAction="#onSendChat"`)
2. Add a "Save & Exit" button below the leave button (visible only for online games):
   - `onAction="#onSaveAndExit"`
3. Chat panel is only visible for online games. Hide for offline games.
4. `onSendChat()`: Read the text field, send `CHAT` message via `chess` facade, clear the field.
5. Register a chat handler on the `ServerCommunicationTask` (via `Chess`) that adds incoming chat messages to the `chatList`.
6. `onSaveAndExit()`: Call `chess.requestSaveGame()`, show a label "Waiting for opponent to accept..." The save confirmation is handled by the server.
7. When `SAVE_ACCEPTED` is received (via observer or callback), show a confirmation and return to menu.
8. When `SAVE_GAME` is received from opponent, show a confirmation dialog: "Opponent wants to save the game. Accept?" If yes, send `SAVE_GAME` back.

### Implementation Details
- Modify: `modules/application/src/main/java/io/github/conava/chess/application/controllers/GameController.java`
- Modify: `modules/application/src/main/resources/fxml/game.fxml`
- In `game.fxml`, add to the left VBox (after the black player card):
  ```xml
  <VBox fx:id="chatPanel" spacing="8" VBox.vgrow="ALWAYS" visible="false" managed="false">
      <Label text="%game.chat" styleClass="section-heading"/>
      <ListView fx:id="chatList" styleClass="move-list-view" VBox.vgrow="ALWAYS"/>
      <HBox spacing="4">
          <TextField fx:id="chatInput" HBox.hgrow="ALWAYS" promptText="%game.chat.placeholder"/>
          <Button text="%game.chat.send" onAction="#onSendChat"/>
      </HBox>
  </VBox>
  ```
- In `initialize()`, check if the game is online (via `chess.getJoinCode() != null`). If online, show the chat panel and save button.
- The "Save & Exit" button goes in the right panel, above or below the leave button.
- Chat incoming messages: the `GameController` registers a chat callback that appends to `chatList` via `Platform.runLater`.

### Edge Cases and Pitfalls
- Chat messages must be displayed with the sender's name: "alice: Hello!".
- The chat input field should have a max length (500 chars matching server validation).
- The save flow is async: player requests save, waits for opponent confirmation. The UI should reflect this state.
- If the opponent declines (disconnects without accepting), the save is cancelled and the game continues.
- `onSendChat` must validate that the input is non-blank before sending.

### Test Approach
`manual-verification`

### Acceptance Criteria
- [ ] Chat panel is visible for online games, hidden for offline
- [ ] Messages appear in the chat list with sender name
- [ ] Sending a message clears the input field
- [ ] "Save & Exit" sends save request
- [ ] Save confirmation dialog works for both initiator and receiver
- [ ] Opponent's save request shows a confirmation dialog

---

## Task T26: i18n keys for chat and auth screens

### Context
All new UI text needs i18n keys in both English and German properties files.

### Requirements
1. Add the following keys to both `messages_en.properties` and `messages_de.properties`:

**Auth:**
- `login.title` = Login / Anmelden
- `login.username` = Username / Benutzername
- `login.password` = Password / Passwort
- `login.button` = Login / Anmelden
- `login.register_link` = Create an account / Konto erstellen
- `login.error.invalid` = Invalid username or password / Benutzername oder Passwort ungueltig
- `login.error.connection` = Cannot connect to server / Verbindung zum Server fehlgeschlagen
- `register.title` = Register / Registrieren
- `register.password_confirm` = Confirm password / Passwort bestaetigen
- `register.button` = Register / Registrieren
- `register.login_link` = Already have an account? Login / Bereits ein Konto? Anmelden
- `register.error.mismatch` = Passwords do not match / Passwoerter stimmen nicht ueberein
- `register.error.short_password` = Password must be at least 8 characters / Passwort muss mindestens 8 Zeichen lang sein
- `register.error.username_taken` = Username already exists / Benutzername bereits vergeben

**Menu:**
- `menu.welcome` = Welcome, {0} / Willkommen, {0}
- `menu.logout` = Logout / Abmelden

**Chat:**
- `game.chat` = Chat
- `game.chat.placeholder` = Type a message... / Nachricht eingeben...
- `game.chat.send` = Send / Senden

**Save:**
- `game.save_exit` = Save & Exit / Speichern & Beenden
- `game.save.waiting` = Waiting for opponent... / Warte auf Gegner...
- `game.save.opponent_request` = Opponent wants to save. Accept? / Gegner moechte speichern. Akzeptieren?
- `game.save.accepted` = Game saved successfully / Spiel erfolgreich gespeichert

**Matchmaking:**
- `matchmaking.find` = Find Match / Spiel finden
- `matchmaking.searching` = Searching for opponent... / Suche Gegner...
- `matchmaking.cancel` = Cancel / Abbrechen

### Implementation Details
- Modify: `modules/application/src/main/resources/i18n/messages_en.properties`
- Modify: `modules/application/src/main/resources/i18n/messages_de.properties`
- Append all new keys at the end of each file, grouped by section with a comment header.

### Edge Cases and Pitfalls
- German umlauts: use proper Unicode characters (ae, oe, ue replacements are acceptable in .properties files, but actual Unicode like `\u00e4` is cleaner).
- The `{0}` placeholder in `menu.welcome` requires `MessageFormat.format()` in the `I18n.get()` method. Check if `I18n` already supports parameterized messages. If not, add an overload `I18n.get(String key, Object... args)` that wraps `MessageFormat.format()`.

### Test Approach
`dry-run`

Verify the application loads without missing key errors. Check each screen shows the correct text.

### Acceptance Criteria
- [ ] All new keys exist in both EN and DE files
- [ ] No missing key warnings at runtime
- [ ] German translations are reasonable (not machine-translated gibberish)

---

## Dependency Graph

```
Group 1:  T01   T02   T03   T10
           |\_   |     |
Group 2:  T15  T22   T04   T05
           |          |     |\_____
Group 3:  T16        T11   T06 T07 T08
           |\_        |     \   |  /
Group 4: T17 T18 T21 T13   T09
          \   /    |         |
Group 5: T19  T23 T25      T12
          |\_               |
Group 6: T20 T24 T26      T14
```

**Critical path (server):** T03 -> T05 -> T06/T07 -> T09 -> T12 -> T14
**Critical path (app):** T01 -> T15 -> T16 -> T17/T18 -> T19 -> T20
**Convergence:** T25 (chat panel, group 5) depends on T16, T21, T22 from groups 2-4

---

## Complexity Estimate

**Extra-large.** This overhaul touches all three modules, introduces 4 new server packages (config, auth, persistence, matchmaking), 2 new application controllers with FXML, modifies 6 existing files significantly, and adds a SQLite persistence layer. Estimated 20+ source files changed or created, 26 tasks across 6 parallel groups. The server refactoring (GameManager extraction, ClientHandler rewrite, GameInstance overhaul) is the highest-risk area.

---

## Risks and Concerns

1. **SQLite single-writer bottleneck.** SQLite allows only one writer at a time. With many concurrent games writing moves and chat messages, this could become a bottleneck. Mitigation: WAL mode helps reads, and the server is expected to handle tens of games, not thousands.

2. **ClientHandler refactoring scope.** T12 is the largest single task. The `ClientHandler` is being rewritten to add auth gating, new message dispatch, and `GameManager` integration all at once. If this task proves too large, it could be split into sub-tasks.

3. **Game resume complexity.** Reconstructing a game from a move history requires replaying all moves, which exercises the full move validation and board execution pipeline. Any bugs in move serialization or deserialization will surface here.

4. **Two connection patterns in the application.** Auth uses a one-shot TCP connection (send login, read response, close). Game play uses a persistent connection via `ServerCommunicationTask`. These are different patterns and could cause confusion. The `Chess` facade must cleanly separate them.

5. **FXML and CSS integration.** New screens (login, register, waiting-for-match) and the chat panel need to integrate with the existing theme system. Missing style classes will produce visually broken screens.

6. **German default player names (C-008).** The design doc lists this as a fix, but the current `Game.getDefaultPlayerName()` already returns English strings ("Player 1 (White)" / "Player 2 (Black)"). This fix appears to have been completed in a prior commit. No task needed.

7. **Existing test coverage.** The server module has 3 test files; extensive refactoring may break them. T11, T12, and T13 must update existing tests alongside adding new ones.
