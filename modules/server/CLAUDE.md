# modules/server CLAUDE.md

## Status
Experimental. The server hosts multiplayer chess games over TCP sockets. Architecture law violations that were previously present in this module have been resolved. No production-ready hardening (TLS, authentication, reconnection) exists.

## Responsibility
This module owns the multiplayer server: accepting TCP client connections, creating and joining game lobbies, relaying moves between two players, and managing server lifecycle (start/stop/stats). It does **not** own game logic, rulesets, or the message serialization format — those belong to `core`. It does **not** own any UI — that belongs to `application`.

## Package Structure
- `io.github.conava.chess.server` — Server entry point and top-level lifecycle management (main method, accept loop, console commands).
- `io.github.conava.chess.server.management` — Per-client and per-game session management (ClientHandler, GameInstance).

## Key Classes

### `Server` (io.github.conava.chess.server)
- **Responsibility:** Entry point (`main`). Instance-based: `main()` creates a `Server` and calls `start(int port)`. Opens a `ServerSocket`, accepts connections in a cached thread pool, and dispatches each to a `ClientHandler`. Provides console commands (`stop`, `stats`). Owns all mutable server state as instance fields: a `ConcurrentHashMap` of active `GameInstance` objects (`gamesList`), a `CopyOnWriteArraySet` of connected `ClientHandler` objects (`connectionsList`), a `Semaphore` limiting concurrent games to 40 (`gameSemaphore`), and an `AtomicInteger` for game ID generation (`gameIdCounter`). Console listener thread is a daemon thread.
- **Collaborators:** `ClientHandler`, `GameInstance`, core's `Message` / `MessageType`.
- **Public methods (beyond `main`):** `start(int port)`, `addClientHandler(ClientHandler)`, `removeClientHandler(ClientHandler)`, `getGamesList()`, `getGameSemaphore()`, `getGameIdCounter()`.

### `ClientHandler` (io.github.conava.chess.server.management)
- **Responsibility:** Implements `Runnable`. Handles one TCP client connection: reads newline-delimited messages via `MessageParser.parse()`, dispatches `CREATE_GAME` and `JOIN_GAME` locally, and forwards all other message types to the associated `GameInstance`. Sends responses back to the client via `MessageParser.serialize()`. `sendMessage(Message)` is `synchronized` on this instance so that two player threads within a `GameInstance` can both write to this client without interleaving output on the underlying `PrintWriter`. Extracts `playerName` from `CREATE_GAME` and `JOIN_GAME` message content; falls back to `"Player 1"` / `"Player 2"` when the parameter is absent. Malformed messages produce an `ERROR` response; the handler loop continues.
- **Collaborators:** `Server` (registers/unregisters itself, accesses shared state via getters), `GameInstance` (delegates game-scoped messages), core's `Message`, `MessageParser`, `MessageType`, `RulesetOptions`.

### `GameInstance` (io.github.conava.chess.server.management)
- **Responsibility:** Manages a single server-side game session. Implements `GameObserver` to receive state-change notifications from the underlying `Game`. Game creation is deferred: the internal `Game` reference is `null` until both players connect via `connectPlayer(ClientHandler, String)`. On the second call, the game is created via `Game.createServerGame(RulesetOptions, String, String)` and this instance registers itself as an observer. The observer handles terminal state transitions only (checkmate, resignation, timeout, draw); it sends a `GAME_STATUS` message to both players when the state changes to a terminal value. Move relay is explicit in `handleMove()` — the observer callback carries no move data. Both `connectPlayer` and `processMessage` are `synchronized` on this instance. On player disconnect, `disconnectPlayer(ClientHandler)` awards a resignation to the remaining player, nulls the handler reference, and removes the observer to prevent memory leaks.
- **Collaborators:** `ClientHandler` (the two connected players), core's `Game`, `GameObserver`, `Move`, `GameState`, `RulesetOptions`, `Message`, `MessageType`, `IllegalMoveException`.

## Design Patterns Identified

### Observer
- **Classes:** `GameInstance` (implements `GameObserver`), core's `Game` (extends `Observable`).
- **How it works:** `GameInstance` registers itself via `game.addObserver(this)` when the game is created in `startGame()`. `onGameStateChanged()` fires after every successful move. The implementation checks whether the new state is terminal; if so, it sends a `GAME_STATUS` message to both players. Non-terminal state changes (RUNNING) are ignored in the observer path.

### Concurrency via Semaphore
- **Classes:** `Server` (owns the `Semaphore`), `ClientHandler` (acquires on `CREATE_GAME`, releases on disconnect).
- **How it works:** A `Semaphore(40)` limits the total number of concurrent game instances. `ClientHandler.createGame()` calls `tryAcquire()`; `ClientHandler.releaseGameSlot()` calls `release()`.

### Thread-per-Connection
- **Classes:** `Server` (cached thread pool), `ClientHandler` (implements `Runnable`).
- **How it works:** Each accepted socket is handed to a new `ClientHandler` and submitted to `Executors.newCachedThreadPool()`.

## Public API
The server module is a standalone executable, not a library consumed by other modules. Its "API" is the TCP message protocol defined by core's `MessageType` enum.

### Message Protocol Extensions (this module's conventions)
- `CREATE_GAME` content: `ruleset=STANDARD playerName=<name>` — `playerName` is optional; defaults to `"Player 1"`.
- `JOIN_GAME` content: `gameId=<id> playerName=<name>` — `playerName` is optional; defaults to `"Player 2"`. `gameId` is key-value format (not raw integer).
- `JOIN_CODE` response content: `joinCode=<id>` — sent to the game creator on successful `CREATE_GAME`.

### Server (instance methods, accessed within the module)
- `new Server()` — constructor; initialises all instance fields.
- `void start(int port)` — opens server socket and blocks until stopped.
- `void addClientHandler(ClientHandler)` — registers a connection.
- `void removeClientHandler(ClientHandler)` — unregisters a connection.
- `Map<Integer, GameInstance> getGamesList()` — returns the live games map.
- `Semaphore getGameSemaphore()` — returns the concurrency semaphore.
- `AtomicInteger getGameIdCounter()` — returns the game ID counter.
- `static void main(String[])` — entry point; parses optional port argument.

### ClientHandler
- `ClientHandler(Socket, Server)` — constructor; takes the client socket and owning Server instance.
- `void run()` — Runnable entry point.
- `synchronized void sendMessage(Message)` — sends a serialized message to the connected client; thread-safe.
- `void releaseGameSlot()` — notifies the game of disconnect, removes game from map, releases semaphore.

### GameInstance
- `GameInstance(int gameId, RulesetOptions ruleset)` — constructor; no Game created yet.
- `synchronized void connectPlayer(ClientHandler, String playerName)` — assigns white or black slot; creates game and starts when both connected.
- `synchronized void disconnectPlayer(ClientHandler)` — awards resignation to remaining player, removes observer.
- `synchronized void processMessage(ClientHandler, Message)` — dispatches an in-game message.
- `void onGameStateChanged()` — observer callback; sends GAME_STATUS to both players on terminal state transitions.
- `int getGameId()` — returns the game's numeric ID.
- `ClientHandler getWhitePlayerHandler()` — returns white player's handler (nullable).
- `ClientHandler getBlackPlayerHandler()` — returns black player's handler (nullable).

## Internal Dependencies
```
server
  +-- Server
        |-- uses --> ClientHandler (creates on accept, passes Server reference)
        |-- uses --> GameInstance (referenced in stats/stop)
        |-- uses --> core: Message, MessageType
  +-- management
        |-- ClientHandler
        |     |-- uses --> Server (instance reference: add/remove, getters)
        |     |-- uses --> GameInstance (creates on CREATE_GAME, delegates messages)
        |     |-- uses --> core: Message, MessageParser, MessageType, RulesetOptions
        |-- GameInstance
              |-- implements --> core: GameObserver
              |-- uses --> ClientHandler (sends messages to players)
              |-- uses --> core: Game (via Game.createServerGame() factory only),
              |            Move, GameState, RulesetOptions,
              |            Message, MessageType, IllegalMoveException
```

## Architecture Law Compliance

### Law 1 — Module boundaries are hard
COMPLIANT

### Law 2 — Chess facade is the only API surface
COMPLIANT. `GameInstance` creates games exclusively via `Game.createServerGame(RulesetOptions, String, String)`. No direct instantiation of `ServerGame` or any other `Game` subclass occurs outside `core`.

### Law 3 — Observer pattern for all state propagation
COMPLIANT. `GameInstance` implements `GameObserver` and registers on the `Game` instance at game-start time. Terminal state transitions (checkmate, resignation, timeout, draw) are propagated to clients via the observer callback. Move relay is explicit in `handleMove()` because the observer callback is signal-only and carries no move data.

### Law 4 — Core is logic-only
COMPLIANT

### Law 5 — Strategy pattern for rulesets
COMPLIANT

## Known Debt / Gotchas

1. **No TLS/SSL support.** All TCP traffic is plaintext. Unsuitable for production deployment over untrusted networks.

2. **No authentication or authorization.** Any client that can reach the TCP port can create or join a game. There is no identity verification.

3. **No reconnection support.** A dropped connection ends the game for that player. The remaining player receives a resignation notification, but the disconnected player cannot reconnect to the same game.

4. **Message protocol has no versioning.** There is no protocol version header. Clients and servers must agree on the exact message format out-of-band.

5. **Hardcoded 40-game limit.** `MAX_GAMES = 40` is a compile-time constant with no external configuration.

6. **Player name validation is absent.** Names are accepted as-is from the protocol with no length limit, no character sanitization, and no uniqueness enforcement.

7. **`connectionsList` drift under error conditions.** `ClientHandler` registers itself in `run()` and deregisters in `cleanup()`. If `run()` throws before reaching `cleanup()`, the handler stays in the set indefinitely.

8. **Move relay sends the client-supplied string, not a normalized form.** After `game.movePiece()` succeeds, the raw move string from the incoming message is echoed to both players. If the client sends a non-canonical but parseable format, both players receive that non-canonical form.
