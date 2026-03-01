# modules/server CLAUDE.md

## Status
Experimental / early-stage. The server has basic structure for hosting multiplayer chess games over TCP sockets, but there are no tests, several architectural law violations (see below), and incomplete message handling. Not production-ready.

## Responsibility
This module owns the multiplayer server: accepting TCP client connections, creating and joining game lobbies, relaying moves between two players, and managing server lifecycle (start/stop/stats). It does **not** own game logic, rulesets, or the message serialization format -- those belong to `core`. It does **not** own any UI -- that belongs to `application`.

## Package Structure
- `io.github.conava.chess.server` -- Server entry point and top-level lifecycle management (main method, accept loop, console commands).
- `io.github.conava.chess.server.management` -- Per-client and per-game session management (ClientHandler, GameInstance).

## Key Classes

### `Server` (io.github.conava.chess.server)
- **Responsibility:** Entry point (`main`). Opens a `ServerSocket`, accepts connections in a cached thread pool, and dispatches each to a `ClientHandler`. Provides console commands (`stop`, `stats`). Maintains static global state: a `ConcurrentHashMap` of active `GameInstance` objects, a `CopyOnWriteArraySet` of connected `ClientHandler` objects, a `Semaphore` limiting concurrent games to 40.
- **Collaborators:** `ClientHandler`, `GameInstance`, core's `Message` / `MessageType`.
- **Notes:** Entirely static. No instance is ever created. Default port is 54321; overridden by a single CLI argument.

### `ClientHandler` (io.github.conava.chess.server.management)
- **Responsibility:** Implements `Runnable`. Handles one TCP client connection: reads newline-delimited messages via `MessageParser.parse()`, dispatches `CREATE_GAME` and `JOIN_GAME` locally, and forwards all other message types to the associated `GameInstance`. Sends responses back to the client via `MessageParser.serialize()`.
- **Collaborators:** `Server` (registers/unregisters itself), `GameInstance` (delegates game-scoped messages), core's `Message`, `MessageParser`, `MessageType`, `RulesetOptions`.

### `GameInstance` (io.github.conava.chess.server.management)
- **Responsibility:** Wraps a single `ServerGame` from core. Manages two player slots (white/black). When both players connect, starts the game. Processes in-game messages: `MOVE` (deserializes via `Move.fromString()`, executes, then relays the executed move to both players), `GAME_STATUS` (handles resignation and status queries), and logs `SUCCESS`/`ERROR`/`FAILURE`/`JOIN_CODE` messages. `handleMove()` catches both `IllegalMoveException` and `RuntimeException` so that malformed move strings from a client do not crash the handler thread.
- **Collaborators:** `ClientHandler` (the two connected players), core's `ServerGame`, `Move`, `GameState`, `RulesetOptions`, `Message`, `MessageType`.

## Design Patterns Identified

### Concurrency via Semaphore
- **Classes:** `Server` (owns the `Semaphore`), `ClientHandler` (acquires on `CREATE_GAME`, releases on disconnect).
- **How it works:** A `Semaphore(40)` limits the total number of concurrent game instances. `ClientHandler.createGame()` calls `tryAcquire()`; `ClientHandler.releaseGameSlot()` calls `release()`.

### Thread-per-Connection
- **Classes:** `Server` (cached thread pool), `ClientHandler` (implements `Runnable`).
- **How it works:** Each accepted socket is handed to a new `ClientHandler` and submitted to `Executors.newCachedThreadPool()`.

No Observer pattern usage was found in this module (see Architecture Law Compliance).

## Public API
The server module is a standalone executable, not a library consumed by other modules. Its "API" is the TCP message protocol defined by core's `MessageType` enum.

### Server (static methods exposed to other classes within the module)
- `Server.addClientHandler(ClientHandler)` -- registers a connection.
- `Server.removeClientHandler(ClientHandler)` -- unregisters a connection.
- `Server.main(String[])` -- entry point. Accepts optional port argument.

### ClientHandler
- `ClientHandler(Socket, Map<Integer, GameInstance>, Semaphore, AtomicInteger)` -- constructor.
- `void run()` -- Runnable entry point.
- `void sendMessage(Message)` -- sends a serialized message to the connected client.
- `void releaseGameSlot()` -- releases the semaphore permit and removes the game from the global map.

### GameInstance
- `GameInstance(int gameId, RulesetOptions ruleset)` -- constructor, creates a `ServerGame`.
- `synchronized void connectPlayer(ClientHandler)` -- assigns white or black slot; starts game when both connected.
- `void processMessage(ClientHandler, Message)` -- dispatches an in-game message.
- `int getGameId()` -- returns the game's numeric ID.
- `ClientHandler getWhitePlayerHandler()` -- returns white player's handler (nullable).
- `ClientHandler getBlackPlayerHandler()` -- returns black player's handler (nullable).

## Internal Dependencies
```
server
  +-- Server
        |-- uses --> ClientHandler (creates on accept)
        |-- uses --> GameInstance (referenced in stats/stop)
        |-- uses --> core: Message, MessageType
  +-- management
        |-- ClientHandler
        |     |-- uses --> Server (static add/remove)
        |     |-- uses --> GameInstance (creates on CREATE_GAME, delegates messages)
        |     |-- uses --> core: Message, MessageParser, MessageType, RulesetOptions
        |-- GameInstance
              |-- uses --> ClientHandler (sends messages to players)
              |-- uses --> core: ServerGame, Move, GameState, RulesetOptions,
              |            Message, MessageType, IllegalMoveException
```

## Architecture Law Compliance

### VIOLATION: Law 2 -- Chess facade bypass
`GameInstance` directly instantiates `ServerGame` (line 35: `new ServerGame(ruleset)`) and calls `game.movePiece()`, `game.startGame()`, `game.setGameState()`, `game.getPlayerWhite()`, `game.getPlayerBlack()`, `game.getState()` directly. The root CLAUDE.md states: "Application and server code must go through `Chess.java`. Direct instantiation of `Game` subclasses from outside `core` is banned." The `Chess` facade is never imported anywhere in this module.

### VIOLATION: Law 3 -- No Observer pattern usage
The server does not implement `GameObserver` or register any observers. Game state changes (moves, game status) are communicated via ad-hoc message passing, not through the Observer pattern. After a move is executed on the `ServerGame`, there is no mechanism to notify clients of the resulting board state -- the move is executed server-side but the result is never relayed back.

### COMPLIANT: Law 1 -- Module boundaries
The server depends only on `core` (verified in `pom.xml`). No dependency on `application`.

### COMPLIANT: Law 4 -- Core is logic-only
Not applicable to this module (this law constrains `core`, not `server`).

### COMPLIANT: Law 5 -- Strategy pattern for rulesets
The server delegates ruleset selection via `RulesetOptions`, which is the core-defined enum. No ruleset branching inside server code.

## Known Debt / Gotchas

1. **No tests.** The `src/test` directory does not exist. Zero test coverage.

2. **Move relay is present but not fully validated.** `handleMove()` now relays the executed move to both players after a successful `game.movePiece()` call. However, the relay sends the raw move string received from the client rather than a normalized form, and the relay to the sending client is redundant (the client already applied the move locally).

3. **Stale pom.xml artifact reference.** The maven-shade-plugin filter references `ptp:core` (line 66: `<artifact>ptp:core</artifact>`) which appears to be a leftover from the pre-rebrand package name. The current group/artifact is `io.github.conava:core`.

4. **`connectionsList` is populated but partially unused.** `Server.connectionsList` is a `CopyOnWriteArraySet` that tracks all connected clients. It is used in `stopServer()` and `printServerStatus()`, but `printServerStatus()` reads `connectionsList.size()` while the actual connection count may drift -- `ClientHandler` registers itself in `run()` but if `run()` throws before reaching `cleanup()`, the handler stays in the set.

5. **`releaseGameSlot()` removes the entire game on any disconnect.** When either player disconnects, the `GameInstance` is removed from `gamesList` and the semaphore is released. This means if white disconnects, black's `gameInstance` reference still exists but the game is gone from the server map. There is no notification to the remaining player.

6. **`joinGame()` does not set `this.gameInstance`.** In `ClientHandler.joinGame()`, the local variable `gameInstance` shadows the field. After joining, `this.gameInstance` remains null, so subsequent messages from the joining player will get "No game instance available" errors. This is a bug.

7. **No graceful handling of malformed messages.** `MessageParser.parse()` failures, `Integer.parseInt()` in `joinGame()`, and `RulesetOptions.valueOf()` in `createGame()` will throw unchecked exceptions that crash the handler thread.

8. **Synchronization inconsistency.** `connectPlayer()` on `GameInstance` is `synchronized`, but `processMessage()` is not. Concurrent message processing for the same game instance could cause race conditions on the `ServerGame`.

9. **Console command thread is a raw `Thread`, not submitted to the executor.** It will not be shut down by `executorService.shutdown()` and could keep the JVM alive after stop. It is also not a daemon thread.
