# Server Module Comprehensive Cleanup

Branch: `refactor/server-cleanup`
Date: 2026-03-01

---

## Decision Table

| # | Question | Decision | Decided |
|---|----------|----------|---------|
| Q1 | Should we modify core's Game.java? | Yes. Add `Game.createServerGame()` static factory method. | 2026-03-01 |
| Q2 | What should the observer callback relay? | Hybrid: move relay stays explicit in `handleMove()`; observer handles terminal state transitions only (checkmate, resignation, draw). | 2026-03-01 |
| Q3 | How to handle player names? | Extend protocol: CREATE_GAME and JOIN_GAME messages include a `playerName` parameter. Pass names to `Game.createServerGame()`. Defer game creation until both players connect. | 2026-03-01 |

---

## Problem Statement

The `modules/server` module has two architecture law violations and multiple tech debt items that collectively make it unreliable, untestable, and non-compliant with the project's design principles.

**Law 2 violation (Facade bypass):** `GameInstance` directly instantiates `new ServerGame(ruleset)` and calls methods on the `Game` object without going through any facade. Direct `Game` subclass instantiation from outside `core` is banned.

**Law 3 violation (No Observer pattern):** The server does not implement `GameObserver` or register any observers. Game state changes (moves, game status) are communicated via ad-hoc message passing, not through the Observer pattern.

**Bugs:** `joinGame()` has a field-shadow bug that leaves `this.gameInstance` null after joining. Move errors are silently swallowed without client notification. The `processMessage()` method lacks synchronization. Multiple disconnect/cleanup race conditions exist.

**Tech debt:** The `Server` class is entirely static (untestable). The POM references a stale artifact name. The console command thread is not a daemon. No input validation for port numbers or malformed messages.

---

## Architecture Law Impact

| Law | Status | Risk | Notes |
|-----|--------|------|-------|
| Law 1: Module boundaries | COMPLIANT | AT RISK | Fixing Law 2 requires a small, additive change to `core`'s `Game.java`. Approved by human. |
| Law 2: Facade is the only API surface | VIOLATED | Will fix | `GameInstance` directly instantiates `ServerGame` and calls `Game` methods. Fix: add `Game.createServerGame()` factory to core; server uses factory exclusively. |
| Law 3: Observer for state propagation | VIOLATED | Will fix | No `GameObserver` usage. Fix: `GameInstance` implements `GameObserver`, registers on the game, relays terminal state transitions to clients. |
| Law 4: Core is logic-only | COMPLIANT | No risk | Not affected by this task. |
| Law 5: Strategy pattern for rulesets | COMPLIANT | No risk | Server already delegates via `RulesetOptions`. |

---

## Affected Files

| File Path | What Changes | Why | Cascade Risk |
|-----------|-------------|-----|--------------|
| `modules/core/src/main/java/io/github/conava/chess/core/logic/game/Game.java` | Add `createServerGame(RulesetOptions, String, String)` static factory method | Law 2: sanctioned factory path for server-side game creation | CROSS-MODULE: server depends on this factory |
| `modules/core/src/main/java/io/github/conava/chess/core/logic/game/ServerGame.java` | Update constructor to accept `(RulesetOptions, String, String)` forwarding player names to `Game` superclass | Support player-name passthrough from factory | DEPENDENT: must compile with Game.java change |
| `modules/server/pom.xml` | Fix stale `ptp:core` artifact reference to `io.github.conava:core` | Tech debt: leftover from package rebrand | NONE |
| `modules/server/src/main/java/io/github/conava/chess/server/Server.java` | Refactor from static to instance-based, make console thread daemon, add port validation, add getter methods for gamesList/gameSemaphore/gameIdCounter | Testability, correctness, robustness | DEPENDENT: ClientHandler references Server methods |
| `modules/server/src/main/java/io/github/conava/chess/server/management/GameInstance.java` | Major rewrite: implement `GameObserver` (terminal state transitions only), use `Game.createServerGame()` factory, defer game creation until both players connect, accept player names, add synchronization, fix error handling, send error responses for illegal moves, fix duplicate GAME_STATUS, modernize switch | Laws 2+3, Q2 (hybrid observer), Q3 (player names), bug fixes, robustness | DEPENDENT: must compile after Game.java and ServerGame.java changes |
| `modules/server/src/main/java/io/github/conava/chess/server/management/ClientHandler.java` | Fix `joinGame()` shadow bug, extract playerName from CREATE_GAME and JOIN_GAME messages, change JOIN_GAME content from raw integer to key=value format, add malformed-message error handling, synchronize `sendMessage()`, use Server instance reference, modernize switch | Q3 (player names), bug fix, robustness, code quality | DEPENDENT: must compile with Server and GameInstance changes |
| `modules/server/src/test/java/io/github/conava/chess/server/management/GameInstanceTest.java` | Update constructor calls to match new GameInstance signature (gameId, ruleset removed; replaced with gameId, ruleset, playerNames or similar) | Existing tests must stay green | DEPENDENT: follows GameInstance changes |

---

## Design Decisions

### Decision 1: Add `Game.createServerGame()` factory method to core (Q1 -- APPROVED)

**What:** Add a new static factory method `Game.createServerGame(RulesetOptions, String, String)` to `Game.java` in `core`. Update `ServerGame` constructor to accept player names.

**Why chosen:** The factory method pattern is already established (`Game.createGame()` exists). This is a small, additive, non-breaking change. It provides a sanctioned API surface for server-side game creation so `new ServerGame()` is never called outside `core`.

**Rejected:** Extending `createGame()` with a third boolean/enum -- pollutes existing API. Keeping `new ServerGame()` -- violates Law 2.

### Decision 2: Hybrid observer strategy (Q2 -- APPROVED)

**What:** `GameInstance` implements `GameObserver` and registers on the `Game`. The observer handles terminal state transitions only (checkmate, resignation, draw). Move relay remains explicit in `handleMove()`.

**Why chosen:** The `onGameStateChanged()` callback is signal-only (no parameters), so it cannot know which move triggered it. Move relay requires the move data, which is only available in `handleMove()`. Terminal state detection works by reading `game.getState()` in the callback and comparing against a cached previous state. This hybrid approach uses the Observer pattern where it adds value (automatic state-transition notification) without forcing an awkward workaround for move relay.

**Implementation detail:** `GameInstance` maintains a `GameState previousState` field. In `onGameStateChanged()`, it reads `game.getState()`, compares to `previousState`, and if the new state is terminal, sends `GAME_STATUS` to both players. It then updates `previousState`.

### Decision 3: Extend protocol for player names (Q3 -- APPROVED)

**What:** Extend the CREATE_GAME and JOIN_GAME message formats to include a `playerName` parameter. Defer `Game` creation until both players have connected so both names are available.

**Protocol changes:**
- CREATE_GAME content: `ruleset=STANDARD playerName=Alice` (was: `ruleset=STANDARD`)
- JOIN_GAME content: `gameId=5 playerName=Bob` (was: `5` as raw integer)

**Game creation flow change:** Currently, `GameInstance` creates the `ServerGame` in its constructor (when only white's name is known). With this change, `GameInstance` stores the ruleset and player names, and creates the `Game` via `Game.createServerGame()` only when the second player connects in `connectPlayer()`. This avoids needing a player-name setter on `Game` and is cleaner separation of concerns (WAITING_FOR_PLAYER is managed by `GameInstance`, not by the `Game` object).

**Rejected:** Hardcoded defaults -- human explicitly chose to add names now for better UX. Creating game with placeholder name then updating -- `Game`/`Player` are not designed for name mutation.

### Decision 4: Thread safety for sendMessage()

**What:** `ClientHandler.sendMessage()` writes to a `PrintWriter` from multiple threads.

**Chosen:** Synchronize `sendMessage()` on the ClientHandler instance. `PrintWriter` is not thread-safe by contract.

### Decision 5: processMessage() synchronization

**What:** `GameInstance.connectPlayer()` is `synchronized` but `processMessage()` is not.

**Chosen:** Make `processMessage()` synchronized on the `GameInstance` monitor, matching `connectPlayer()`. Coarse-grained but correct for current throughput.

### Decision 6: Server refactoring from static to instance-based

**What:** Convert `Server` from all-static to instance-based.

**Chosen:** `main()` creates a `Server` instance and calls `start(port)`. Fields become instance fields. `ClientHandler` receives a `Server` reference. Server exposes getters for `gamesList`, `gameSemaphore`, `gameIdCounter`.

**Why:** Static design prevents unit testing, creates hidden global state, prevents multiple instances in tests.

---

## Ordered Implementation Tasks (EXECUTOR ONLY)

### Task 1: Fix stale POM artifact reference

**Files:** `/home/marlon/source/chess/modules/server/pom.xml`

**Changes:**
- Change `<artifact>ptp:core</artifact>` (line 66) to `<artifact>io.github.conava:core</artifact>`

**Acceptance criteria:**
- `mvn clean package -pl modules/server -am` succeeds with no warnings about unmatched shade filter

**Required Javadoc:** None

---

### Task 2: Add ServerGame factory method to Game.java (CROSS-MODULE)

**Files:**
- `/home/marlon/source/chess/modules/core/src/main/java/io/github/conava/chess/core/logic/game/Game.java`
- `/home/marlon/source/chess/modules/core/src/main/java/io/github/conava/chess/core/logic/game/ServerGame.java`

**Changes:**

In `ServerGame.java`:
- Change constructor signature from `ServerGame(RulesetOptions selectedRuleset)` to `ServerGame(RulesetOptions selectedRuleset, String playerWhiteName, String playerBlackName)`
- Forward all three parameters to `super(selectedRuleset, playerWhiteName, playerBlackName)`
- Remove the hardcoded "Player 1" / "Player 2" strings

In `Game.java`:
- Add a new static factory method:
  ```
  public static Game createServerGame(RulesetOptions selectedRuleset, String playerWhiteName, String playerBlackName)
  ```
  that returns `new ServerGame(selectedRuleset, playerWhiteName, playerBlackName)`

**Acceptance criteria:**
- `Game.createServerGame(RulesetOptions.STANDARD, "Alice", "Bob")` returns a non-null `Game` instance
- The returned game's `getPlayerWhite().name()` equals "Alice" and `getPlayerBlack().name()` equals "Bob"
- `mvn clean compile -pl modules/core` succeeds
- Existing tests in core still pass: `mvn test -pl modules/core`

**Required Javadoc:**
- `Game.createServerGame()` -- new public static method
- `ServerGame(RulesetOptions, String, String)` -- updated constructor

---

### Task 3: Refactor Server from static to instance-based

**Files:** `/home/marlon/source/chess/modules/server/src/main/java/io/github/conava/chess/server/Server.java`

**Changes:**
- Convert static fields to instance fields: `gameSemaphore`, `gamesList`, `connectionsList`, `gameIdCounter`, `running` (keep `LOGGER` as static final and `MAX_GAMES` as static final)
- Convert static methods to instance methods: `startConsoleCommandListener`, `acceptClientConnections`, `printServerStatus`, `stopServer`, `addClientHandler`, `removeClientHandler` (keep `main()` and `getPort()` as static)
- `main()` creates a `Server` instance and calls `start(port)`
- Add `public void start(int port)` containing the server socket creation and accept loop (logic currently in `main()` after port parsing)
- Make console command thread a daemon thread: `thread.setDaemon(true)` before `thread.start()`
- Add port validation in `getPort()`: wrap `Integer.parseInt()` in try-catch for `NumberFormatException`, validate range 1-65535, fall back to default port 54321 on any error
- Add getter methods: `getGamesList()`, `getGameSemaphore()`, `getGameIdCounter()` for use by ClientHandler
- Constructor initializes instance fields (`gameSemaphore`, `gamesList`, `connectionsList`, `gameIdCounter`, `running = true`)

**Acceptance criteria:**
- `Server.main(new String[]{})` starts the server as before
- Console thread is daemon (`thread.isDaemon() == true`)
- Invalid port arguments (negative, >65535, non-numeric) log a warning and use default port 54321
- `addClientHandler()` and `removeClientHandler()` are instance methods
- `mvn clean compile -pl modules/server -am` succeeds

**Required Javadoc:**
- `Server` class -- update class-level Javadoc to reflect instance-based design
- `Server()` constructor -- new
- `Server.start(int)` -- new public method
- `Server.getGamesList()` -- new
- `Server.getGameSemaphore()` -- new
- `Server.getGameIdCounter()` -- new

---

### Task 4: Refactor GameInstance -- implement GameObserver + use factory + defer game creation + accept player names + synchronize + fix errors

**Files:** `/home/marlon/source/chess/modules/server/src/main/java/io/github/conava/chess/server/management/GameInstance.java`

**Changes:**

**Law 2 fix -- Use factory, defer creation (Q3):**
- Remove import of `ServerGame`
- Add import of `Game`
- Change field type from `ServerGame game` to `Game game`
- Do NOT create the game in the constructor. Instead, store the `RulesetOptions` and the creator's (white's) player name as fields
- Add fields: `private final RulesetOptions ruleset`, `private String whitePlayerName`, `private String blackPlayerName`
- Constructor signature changes to: `GameInstance(int gameId, RulesetOptions ruleset, String creatorPlayerName)`
- Constructor stores `gameId`, `ruleset`, `whitePlayerName = creatorPlayerName`, sets `game = null`
- Remove `this.game.setGameState(GameState.WAITING_FOR_PLAYER)` from constructor (game does not exist yet)

**connectPlayer() changes (Q3):**
- When white connects (first player): already handled by constructor storing the name. `connectPlayer(ClientHandler clientHandler)` assigns the handler and sends SUCCESS.
- When black connects (second player): accept player name parameter. Signature changes to `connectPlayer(ClientHandler clientHandler, String playerName)`
  - For white (first call): `whitePlayerHandler = clientHandler` (name already stored from constructor)
  - For black (second call): `blackPlayerName = playerName`, then create the game:
    ```
    this.game = Game.createServerGame(ruleset, whitePlayerName, blackPlayerName);
    this.game.addObserver(this);
    ```
    Then call `startGame()`.
- Alternative simpler approach: `connectPlayer(ClientHandler clientHandler, String playerName)` -- first call sets white name + handler, second call sets black name + handler + creates game. This means the constructor does NOT take the creator name; instead the first `connectPlayer` call provides it. This is cleaner because the ClientHandler calls `connectPlayer()` in both create and join paths.

**Revised constructor + connectPlayer design:**
- Constructor: `GameInstance(int gameId, RulesetOptions ruleset)` -- stores gameId and ruleset only. No game created. No player names.
- `connectPlayer(ClientHandler clientHandler, String playerName)`:
  - First call (white): `whitePlayerHandler = clientHandler; whitePlayerName = playerName;` send SUCCESS with player=white
  - Second call (black): `blackPlayerHandler = clientHandler; blackPlayerName = playerName;` send SUCCESS with player=black, create game, register observer, start game

**Law 3 fix -- Implement GameObserver (Q2 hybrid):**
- `GameInstance implements GameObserver`
- Add field: `private GameState previousState = null`
- After creating the game in `connectPlayer()`: `this.game.addObserver(this);` and set `previousState = game.getState()`
- Implement `onGameStateChanged()`:
  1. Read `GameState currentState = game.getState()`
  2. If `currentState` differs from `previousState` AND `currentState` is a terminal state (checkmate, resignation, draw, timeout -- any state that ends the game): send `GAME_STATUS` message with `gameState=<currentState>` to both players via `sendMessageToPlayers()`
  3. Update `previousState = currentState`
- Terminal states to check for: `WHITE_WON_BY_CHECKMATE`, `BLACK_WON_BY_CHECKMATE`, `WHITE_WON_BY_RESIGNATION`, `BLACK_WON_BY_RESIGNATION`, `WHITE_WON_BY_TIMEOUT`, `BLACK_WON_BY_TIMEOUT`, `DRAW_BY_STALEMATE`, `DRAW_BY_AGREEMENT`, `DRAW_BY_REPETITION` (all non-RUNNING, non-WAITING terminal states in `GameState` enum)

**Move relay stays explicit in handleMove() (Q2 hybrid):**
- After successful `game.movePiece()`, send `MOVE` message to BOTH players (the opponent needs the move to update their local board; the sender gets confirmation)
- Forward `clientHandler` parameter to `handleMove()` so that on `IllegalMoveException`, an ERROR message can be sent back to the sender
- On `RuntimeException` (malformed move), also send ERROR back to sender

**Fix duplicate GAME_STATUS:**
- Remove the manual `sendMessageToPlayers(new Message(MessageType.GAME_STATUS, "gameState=RUNNING"))` from `connectPlayer()`. After `startGame()`, the game state changes to RUNNING, but since RUNNING is not a terminal state, the observer will NOT send a message. Instead, send a single explicit `GAME_STATUS gameState=RUNNING` message in `startGame()` (or after `game.startGame()` completes).
- Actually, since `game.startGame()` just sets `gameState = RUNNING` and does not call `notifyObservers()` (checking ServerGame.startGame() -- it only sets the field), we need an explicit send. Keep ONE explicit `sendMessageToPlayers(new Message(MessageType.GAME_STATUS, "gameState=RUNNING"))` after `game.startGame()`. Remove the other duplicate.

**Synchronization fix:**
- Add `synchronized` keyword to `processMessage()` method

**Modernize switch:**
- Convert `processMessage()` switch to enhanced switch expression

**Acceptance criteria:**
- `GameInstance` never imports `ServerGame` directly
- `GameInstance` implements `GameObserver`
- Game is created only when both players have connected
- Player names from the protocol are passed to the game factory
- After `game.movePiece()`, both clients receive the MOVE message (explicit relay in handleMove)
- When game state transitions to a terminal state, both clients receive GAME_STATUS via observer
- Illegal moves result in ERROR messages sent to the sender
- `processMessage()` is synchronized
- Only one GAME_STATUS message on game start (not duplicated)
- `mvn clean compile -pl modules/server -am` succeeds

**Required Javadoc:**
- `GameInstance` class -- update to mention GameObserver implementation and deferred game creation
- `GameInstance(int, RulesetOptions)` -- updated constructor
- `GameInstance.connectPlayer(ClientHandler, String)` -- updated signature with playerName
- `GameInstance.onGameStateChanged()` -- new method (observer callback for terminal state transitions)
- `GameInstance.processMessage(ClientHandler, Message)` -- update to note synchronization

---

### Task 5: Refactor ClientHandler -- use Server instance + fix joinGame shadow bug + player name extraction + robustness

**Files:** `/home/marlon/source/chess/modules/server/src/main/java/io/github/conava/chess/server/management/ClientHandler.java`

**Changes:**

**Server instance reference (depends on Task 3):**
- Change constructor to accept a `Server` reference: `ClientHandler(Socket clientSocket, Server server)`
- Store `server` as a field
- Access gamesList, gameSemaphore, gameIdCounter via server getters: `server.getGamesList()`, `server.getGameSemaphore()`, `server.getGameIdCounter()`
- Replace `Server.addClientHandler(this)` with `server.addClientHandler(this)` and same for removeClientHandler

**Fix joinGame() shadow bug:**
- Change `GameInstance gameInstance = gamesList.get(gameId);` to `GameInstance foundGame = gamesList.get(gameId);`
- After `foundGame.connectPlayer(this, playerName)` succeeds, add `this.gameInstance = foundGame;`

**Player name extraction from protocol (Q3):**
- In `createGame(Message message)`:
  - Extract playerName: `String playerName = message.getParameterValue("playerName");` -- use "Player 1" as default if null
  - Create GameInstance: `gameInstance = new GameInstance(gameId, ruleset);`
  - Connect with name: `gameInstance.connectPlayer(this, playerName);`
- In `joinGame(Message message)`:
  - Change from `Integer.parseInt(message.content())` to `message.getParameterValue("gameId")`
  - Parse gameId: `int gameId = Integer.parseInt(message.getParameterValue("gameId"));`
  - Extract playerName: `String playerName = message.getParameterValue("playerName");` -- use "Player 2" as default if null
  - Call: `foundGame.connectPlayer(this, playerName);`

**Malformed message handling:**
- Wrap `MessageParser.parse(inputLine)` in `processClientMessages()` with try-catch for `RuntimeException`. On failure, send ERROR message and `continue` the loop.
- In `joinGame()`: wrap `Integer.parseInt(...)` in try-catch for `NumberFormatException`, send ERROR on failure
- In `createGame()`: wrap `RulesetOptions.valueOf(...)` in try-catch for `IllegalArgumentException`, send ERROR on failure

**Synchronize sendMessage():**
- Add `synchronized` keyword to `sendMessage()` method

**Modernize switch:**
- Convert `handleMessage()` switch to enhanced switch expression

**Acceptance criteria:**
- After `joinGame()`, `this.gameInstance` is correctly set (shadow bug fixed)
- Player names are extracted from CREATE_GAME (`playerName` param) and JOIN_GAME (`playerName` param) messages
- Default names ("Player 1" / "Player 2") are used when `playerName` param is absent (backward compatibility)
- JOIN_GAME now uses `gameId` key-value param instead of raw integer content
- Malformed messages produce ERROR responses instead of crashing the handler thread
- `sendMessage()` is thread-safe (synchronized)
- `mvn clean compile -pl modules/server -am` succeeds

**Required Javadoc:**
- `ClientHandler(Socket, Server)` -- updated constructor
- `ClientHandler.sendMessage(Message)` -- update to note thread safety guarantee

---

### Task 6: Fix releaseGameSlot to notify remaining player on disconnect

**Files:**
- `/home/marlon/source/chess/modules/server/src/main/java/io/github/conava/chess/server/management/GameInstance.java`
- `/home/marlon/source/chess/modules/server/src/main/java/io/github/conava/chess/server/management/ClientHandler.java`

**Changes:**

In `GameInstance.java`:
- Add `public synchronized void disconnectPlayer(ClientHandler clientHandler)`:
  1. Determine which player disconnected (compare `clientHandler` to `whitePlayerHandler` / `blackPlayerHandler`)
  2. If the game exists and is in a non-terminal state:
     - Set the appropriate resignation state (`WHITE_WON_BY_RESIGNATION` if black disconnected, `BLACK_WON_BY_RESIGNATION` if white disconnected) via `game.setGameState()`
     - Send GAME_STATUS message to the remaining player indicating opponent disconnected
  3. Null out the disconnected player's handler reference
  4. If game exists, remove observer: `game.removeObserver(this)`

In `ClientHandler.java`:
- In `releaseGameSlot()`: call `gameInstance.disconnectPlayer(this)` before removing from gamesList and releasing semaphore

**Acceptance criteria:**
- When one player disconnects, the remaining player receives a GAME_STATUS message with the resignation state
- Game state is updated to reflect the disconnection
- Observer is removed to prevent memory leaks
- `mvn clean compile -pl modules/server -am` succeeds

**Required Javadoc:**
- `GameInstance.disconnectPlayer(ClientHandler)` -- new public method

---

### Task 7: Update existing GameInstanceTest for new API

**Files:** `/home/marlon/source/chess/modules/server/src/test/java/io/github/conava/chess/server/management/GameInstanceTest.java`

**Changes:**
- Update `GameInstance` constructor call from `new GameInstance(1, RulesetOptions.STANDARD)` to `new GameInstance(1, RulesetOptions.STANDARD)` (signature stays `(int, RulesetOptions)`)
- Since game is now deferred until both players connect, and existing tests call `processMessage(null, ...)` with MOVE messages on a game that has not been started: the tests need to either:
  - Connect two mock/stub players first (so the game gets created), OR
  - Verify that processMessage gracefully handles the case where `game == null` (no game yet because no players connected)
- The existing tests verify that malformed MOVE messages do not throw. With deferred game creation, the `game` field is null until both players connect. `processMessage()` must guard against `game == null` in the MOVE handler. The tests should still pass because a null game means the move cannot be processed (and should not throw).
- If the guard is: "if game is null, send ERROR and return", then existing tests need a `ClientHandler` mock (currently null is passed). Alternatively, the guard can silently return when game is null and clientHandler is null (test-safe).
- This is a compilation-and-green-test fix only; new test authoring belongs in Testing Requirements.

**Acceptance criteria:**
- `mvn test -pl modules/server` passes with all existing test assertions still valid
- No new tests added in this task

**Required Javadoc:** None

---

## Testing Requirements (TEST-WRITER ONLY)

| Behavior | Suggested Test Method | What to Assert |
|----------|----------------------|----------------|
| GameInstance uses factory (no direct ServerGame instantiation) | `gameInstance_usesFactory_createsValidGame` | After two players connect, game is created successfully, `getState()` returns RUNNING |
| Deferred game creation: game is null before both players connect | `gameInstance_beforeBothConnect_gameIsNull` | After only one player connects, game-dependent operations are guarded |
| GameInstance observer detects terminal state transitions | `onGameStateChanged_terminalState_notifiesClients` | When game state becomes checkmate/resignation, both mock ClientHandlers receive GAME_STATUS |
| Observer does NOT fire for non-terminal state changes | `onGameStateChanged_nonTerminalState_noMessage` | RUNNING state does not trigger observer-based GAME_STATUS send |
| Move relay is explicit (not via observer) | `handleMove_validMove_relaysToPlayers` | After successful movePiece, both players receive MOVE message |
| Illegal move sends ERROR to sender | `handleMove_illegalMove_sendsErrorToSender` | Sender ClientHandler receives ERROR message |
| Malformed move does not crash handler | `handleMove_malformedMove_doesNotThrow` | No exception propagates, ERROR sent to sender |
| Player names from CREATE_GAME protocol | `createGame_withPlayerName_passedToGame` | Game's `getPlayerWhite().name()` matches the name from the message |
| Player names from JOIN_GAME protocol | `joinGame_withPlayerName_passedToGame` | Game's `getPlayerBlack().name()` matches the name from the message |
| Default player names when param absent | `createGame_noPlayerName_usesDefault` | Game's `getPlayerWhite().name()` equals "Player 1" |
| joinGame sets this.gameInstance correctly (shadow bug fix) | `joinGame_setsFieldCorrectly` | After joinGame, subsequent messages are dispatched to the game |
| JOIN_GAME uses key=value format | `joinGame_keyValueFormat_parsesCorrectly` | `gameId=5 playerName=Bob` is parsed correctly |
| Malformed message parse does not crash handler | `processClientMessages_malformedInput_sendsError` | ERROR message sent, handler continues processing |
| Invalid join code sends error | `joinGame_invalidCode_sendsError` | ERROR message sent to client |
| Invalid ruleset name sends error | `createGame_invalidRuleset_sendsError` | ERROR message sent to client |
| sendMessage is thread-safe | `sendMessage_concurrentCalls_noCorruption` | Multiple threads calling sendMessage do not produce interleaved output |
| processMessage synchronized | `processMessage_concurrent_serialized` | Two messages processed sequentially within same GameInstance |
| Disconnect notifies remaining player | `disconnect_notifiesOpponent` | Remaining player receives GAME_STATUS resignation message |
| Disconnect removes observer | `disconnect_removesObserver` | Observer is removed, no further notifications after disconnect |
| Port validation rejects bad values | `getPort_invalidArgs_returnsDefault` | Negative, >65535, non-numeric all return 54321 |
| Console thread is daemon | `consoleThread_isDaemon` | Thread.isDaemon() is true |
| Server instance-based: no shared static state | `server_instanceBased_noStaticState` | Two Server instances do not share state |
| Game.createServerGame factory method | `createServerGame_returnsValidGame` | Returns non-null Game, players match provided names |
| Game.createServerGame with blank names uses defaults | `createServerGame_blankNames_usesDefaults` | Default names applied for blank input |

---

## Documentation & Javadoc Requirements (DOCS-KEEPER ONLY)

### CLAUDE.md Updates

**`modules/server/CLAUDE.md` -- full rewrite of affected sections:**
- Architecture Law Compliance: mark Laws 2 and 3 as COMPLIANT with description of how they are now satisfied
- Update Key Classes section:
  - `Server`: now instance-based, constructor + `start(int port)`, getter methods
  - `ClientHandler`: constructor takes `(Socket, Server)`, `sendMessage` is synchronized, extracts playerName from protocol
  - `GameInstance`: implements `GameObserver`, deferred game creation via `Game.createServerGame()`, `connectPlayer(ClientHandler, String)`, `disconnectPlayer(ClientHandler)`, `onGameStateChanged()` for terminal state transitions, `processMessage` is synchronized
- Update Design Patterns section: add Observer pattern usage
- Update Public API section to reflect new signatures
- Update Internal Dependencies diagram
- Update Known Debt: remove fixed items (shadow bug, no tests, stale POM, sync issues, etc.), add new items

**`modules/core/CLAUDE.md` -- targeted updates:**
- Add `Game.createServerGame(RulesetOptions, String, String)` to Public API section
- Update `ServerGame` entry in Key Classes: constructor now accepts `(RulesetOptions, String, String)`

### Known Debt Additions (for server CLAUDE.md)
- No TLS/SSL support for TCP connections
- No authentication or authorization for clients
- No reconnection support for dropped connections
- Message protocol has no versioning
- Hardcoded max 40 games limit with no configuration
- Player name validation is absent (names accepted as-is, no length limit, no sanitization)

### ADR
- ADR for "Server game creation via Game.createServerGame() factory method" -- documents the decision to add a core factory method for server-side games instead of alternatives (direct instantiation, extending createGame, moving facade to core)
- ADR for "Player name protocol extension" -- documents the decision to extend CREATE_GAME and JOIN_GAME message formats with a `playerName` parameter

---

## Open Questions

All open questions have been answered. See Decision Table at top of document.
