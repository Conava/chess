# Module Boundaries

This document defines which code belongs where, what the dependency rules are, and what is
explicitly banned. These rules are enforced for every change — no exceptions without explicit
human approval.

## Dependency graph

```
application  ──depends on──>  core
server       ──depends on──>  core
core         ──depends on──>  (nothing)
```

`application` and `server` never depend on each other. There is no shared library between them
other than `core`. These constraints are enforced at the Maven level: each `pom.xml` declares
only the approved dependency.

## What goes in each module

### `modules/core`

**Owns:** all chess domain logic.

Permitted content:
- Board state (`Board`, `Square`)
- Piece hierarchy (`Piece`, `Bishop`, `King`, `Knight`, `Pawn`, `Queen`, `Rook`, `Pieces` enum)
- Player types (`Player`, `PlayerColor`)
- Move types (`Move`, `CastleMove`, `PromotionMove`)
- Game lifecycle (`Game`, `OfflineGame`, `OnlineGame`, `ServerGame`, `GameState`)
- Observer contract (`GameObserver`, `Observable`)
- Ruleset abstraction (`Ruleset`, `RulesetOptions`, `StandardChessRuleset`)
- Per-piece move generators (`PossibleStandard*Moves`)
- Starting position builder (`PossibleStandardPosition`)
- Wire protocol data types (`Message`, `MessageParser`, `MessageType`)
- Exceptions (`IllegalMoveException`)
- The `ServerConnection` interface (defined here so `OnlineGame` can call it without importing application classes)

Banned content:
- JavaFX imports (`javafx.*`)
- Swing imports (`javax.swing.*`, `java.awt.*`)
- Socket or file I/O (`java.net.*`, `java.io.*` beyond pure data record usage)
- Any reference to `application` or `server` packages

### `modules/application`

**Owns:** the JavaFX desktop GUI and the executable entry point.

Permitted content:
- `Chess.java` — extends `javafx.application.Application`; wraps a `core.Game` instance as
  the sole façade
- FXML controllers (`controllers/` package)
- Scene and overlay management (`navigation/` package)
- Theme system (`theme/` package — `ThemeManager`, `Theme`, `BoardTheme`)
- Localization (`i18n/` package — `I18n`, language bundles)
- Settings persistence (`settings/` package — `SettingsService`)
- Network client (`network/` package — `ServerCommunicationTask implements ServerConnection`)
- Background tasks (`tasks/` package — `ExecuteMove extends Task<Void>`)
- FXML layouts, CSS files, PNG piece icons, i18n property files

Banned content:
- Direct instantiation of `Game` subclasses (`OfflineGame`, `OnlineGame`, `ServerGame`)
- Any import from `io.github.conava.chess.server.*`
- Polling loops on game state (use the `GameObserver` callback instead)
- UI mutations on non-FX threads without `Platform.runLater()`

### `modules/server`

**Owns:** the TCP multiplayer server.

Permitted content:
- `Server.java` — entry point; accept loop; console commands
- `ClientHandler` — one `Runnable` per connected TCP client
- `GameInstance` — per-game session manager; implements `GameObserver`

Banned content:
- Direct instantiation of `ServerGame` or any other `Game` subclass (use
  `Game.createServerGame()` from the `core` factory)
- Any import from `io.github.conava.chess.application.*`
- Ruleset logic, move validation, or board manipulation (all belong in `core`)

## The façade rule

All interaction between the `application` module and the `core` module must go through the
`Chess` class in `io.github.conava.chess.application.Chess`.

Concretely:
- Controllers call `chess.movePiece(...)`, `chess.getBoard()`, `chess.addObserver(...)`, etc.
- No controller or service holds a reference to `Game`, `OfflineGame`, `OnlineGame`, or any
  other `core` class beyond the data types (`Square`, `Board`, `Piece`, `Player`, `GameState`,
  `Pieces`, `RulesetOptions`).

The same rule applies on the server side, but there is no façade class there — `GameInstance`
calls `Game.createServerGame()` and then calls methods on the returned `Game` reference
directly. This is correct: `GameInstance` is the server-side equivalent of a controller.

## The `ServerConnection` bridge

`OnlineGame` (in `core`) needs to send TCP messages but must not import application classes.
The solution is the `ServerConnection` interface, defined in `core`:

```java
// core/src/main/java/.../logic/game/ServerConnection.java
public interface ServerConnection {
    void sendMessage(String message);
    void closeConnection();
    boolean isConnected();
}
```

`ServerCommunicationTask` in the `application` module implements this interface. It is passed
to `Game.createGame(online=true, ..., connection)` and stored by `OnlineGame`. This keeps the
dependency arrow pointing from `application` to `core`, never the reverse.

## Game creation rules

Two static factory methods on `Game` control construction. No other construction path is
permitted.

| Factory method | Used by | Creates |
|---------------|---------|---------|
| `Game.createGame(false, ...)` | `Chess.startGame(online=false, ...)` | `OfflineGame` |
| `Game.createGame(true, ...)` | `Chess.createOnlineGame(...)` | `OnlineGame` |
| `Game.createServerGame(...)` | `GameInstance.startGame()` | `ServerGame` |

The constructors of `OfflineGame`, `OnlineGame`, and `ServerGame` are package-private or
accessed only through these factories. External callers in `application` or `server` that
attempt to instantiate subclasses directly will either get a compile error or violate the
architecture law.

## What is explicitly banned (summary)

| What | Where | Why |
|------|-------|-----|
| `import javafx.*` | `core` | Core is logic-only |
| `import javax.swing.*` | anywhere | Swing is replaced by JavaFX |
| `import io.github.conava.chess.server.*` | `application` | Modules cannot depend on each other |
| `import io.github.conava.chess.application.*` | `server` | Modules cannot depend on each other |
| `new OfflineGame(...)` outside `core` | `application`, `server` | Must use factory |
| `new OnlineGame(...)` outside `core` | `application`, `server` | Must use factory |
| `new ServerGame(...)` outside `core` | `application`, `server` | Must use factory |
| Polling `game.getState()` in a loop | `application` | Must use `GameObserver` |
| UI mutations off the FX thread | `application` | Must use `Platform.runLater()` |
| Ruleset branching inside `Game` | `core` | Must use `Ruleset` strategy |

## Verification checklist for new code

Before opening a pull request, verify:

1. `mvn dependency:tree -pl modules/core` shows no dependencies.
2. `mvn dependency:tree -pl modules/application` shows only `core`, JavaFX, and test deps.
3. `mvn dependency:tree -pl modules/server` shows only `core` and test deps.
4. No `import javafx.*` appears in any file under `modules/core/src/main/`.
5. No `import io.github.conava.chess.server.*` appears under `modules/application/src/main/`.
6. No `import io.github.conava.chess.application.*` appears under `modules/server/src/main/`.
7. All game state changes in `application` originate from `GameObserver.onGameStateChanged()`.
