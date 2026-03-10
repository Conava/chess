# Architecture Overview

This document describes the high-level structure of the Chess application: what it does, how
the three modules fit together, and which classes carry the most responsibility.

## What the system does

Chess is a Java 17 desktop application that supports local two-player chess and TCP-based
online multiplayer. It is packaged as three Maven modules that communicate through strict,
enforced boundaries:

- `core` — pure game logic with no I/O or UI
- `application` — JavaFX 21 desktop client
- `server` — TCP multiplayer server

## Module diagram

```
┌─────────────────────────────────────────────────────┐
│                   application                       │
│  JavaFX 21 UI · Controllers · Theming · i18n · Net  │
│           depends on → core only                    │
└──────────────────────┬──────────────────────────────┘
                       │
              ┌────────▼────────┐
              │      core       │
              │  Game engine ·  │
              │  Pieces · Rules │
              │  Observer · API │
              │  (zero deps)    │
              └────────▲────────┘
                       │
┌──────────────────────┴──────────────────────────────┐
│                    server                           │
│       TCP server · Lobby · Move relay               │
│           depends on → core only                    │
└─────────────────────────────────────────────────────┘
```

`core` has zero Maven dependencies on any other module. Both `application` and `server`
declare a compile-scope dependency on `core`. They do not depend on each other.

## Module responsibilities

### core (`modules/core`)

The game engine. Owns everything related to chess rules and game state:

- **Board** — 8×8 grid of `Square` objects, mutable piece occupancy, deep-copy support via `getCopy()`
- **Pieces** — six concrete piece classes (`Pawn`, `Rook`, `Knight`, `Bishop`, `Queen`, `King`)
  plus an abstract `Piece` base class. All pieces implement `copy()` for deep cloning (preserving
  state such as `hasMoved` on `King` and `Rook`).
- **Move types** — `Move`, `CastleMove`, `PromotionMove`
- **Ruleset** — `Ruleset` interface, `AbstractChessRuleset` shared base class, and two
  concrete implementations: `StandardChessRuleset` and `Chess960Ruleset`. Per-piece move
  generators are shared. `AbstractChessRuleset.getLegalSquares` filters pseudo-legal moves
  through deep-copy simulation to enforce check legality. Includes en passant generation.
- **Game lifecycle** — abstract `Game` class plus three concrete subtypes:
  `OfflineGame`, `OnlineGame`, `ServerGame`. Game-end detection (`evaluateGameEnd`) handles
  checkmate, stalemate, 50-move rule, threefold repetition, and insufficient material.
- **Observer contract** — `GameObserver` interface and `Observable` abstract class
- **Wire protocol data types** — `Message`, `MessageParser`, `MessageType` (used by both
  `OnlineGame` and `server`)

No JavaFX, no Swing, no socket I/O, no file I/O lives here.

### application (`modules/application`)

The JavaFX desktop client. Owns the UI layer:

- **Entry point** — `Chess extends Application`; also acts as the façade between UI and `core`
- **Controllers** — 8 FXML controllers covering every screen and overlay
- **Navigation** — `SceneManager` (scene lifecycle) and `OverlayManager` (modal overlays)
- **Network** — `ServerCommunicationTask` implements core's `ServerConnection` interface to
  manage the TCP socket for online games
- **Theming** — `ThemeManager`, `Theme` enum (4 UI themes), `BoardTheme` enum (3 board schemes)
- **Localization** — `I18n` wraps `ResourceBundle`; English and German bundles
- **Settings persistence** — `SettingsService` via `java.util.prefs.Preferences`
- **Background tasks** — `ExecuteMove extends Task<Void>` keeps move execution off the FX thread

### server (`modules/server`)

The TCP multiplayer server. Owns the network session management:

- **Entry point** — `Server.main(String[])`, configurable port (default 54321)
- **Connection handling** — `ClientHandler implements Runnable`, one per connected client
- **Game session management** — `GameInstance implements GameObserver`, one per active game
- **Concurrency** — `Semaphore(40)` limits concurrent games; `ConcurrentHashMap` and
  `CopyOnWriteArraySet` protect shared state

## Key classes by responsibility

| Class | Module | File | Role |
|-------|--------|------|------|
| `Chess` | application | `Chess.java` | JavaFX entry point; sole façade to `core` |
| `Game` | core | `logic/game/Game.java` | Abstract game session; owns board, ruleset, observers, move history |
| `OfflineGame` | core | `logic/game/OfflineGame.java` | Local two-player game |
| `OnlineGame` | core | `logic/game/OnlineGame.java` | Networked game; forwards moves to server |
| `ServerGame` | core | `logic/game/ServerGame.java` | Server-side game session |
| `GameObserver` | core | `logic/observer/GameObserver.java` | Observer interface; `onGameStateChanged()` |
| `Observable` | core | `logic/observer/Observable.java` | Observer list management (CopyOnWriteArrayList) |
| `Ruleset` | core | `logic/ruleset/Ruleset.java` | Strategy interface for ruleset variants |
| `AbstractChessRuleset` | core | `logic/ruleset/AbstractChessRuleset.java` | Shared base class for chess variants (move dispatch, check filter) |
| `StandardChessRuleset` | core | `logic/ruleset/standardChessRuleset/StandardChessRuleset.java` | Standard chess -- overrides `getStartBoard` only |
| `Chess960Ruleset` | core | `logic/ruleset/chess960Ruleset/Chess960Ruleset.java` | Chess960 -- randomized start position, dynamic castling |
| `Chess960StartPosition` | core | `logic/ruleset/chess960Ruleset/Chess960StartPosition.java` | Random position generator + Scharnagl index codec |
| `Board` | core | `data/board/Board.java` | 8×8 grid; executes moves (incl. en passant capture removal); maintains piece lists; deep-copy via `getCopy()` |
| `GameController` | application | `controllers/GameController.java` | In-game screen; implements `GameObserver` |
| `SceneManager` | application | `navigation/SceneManager.java` | Loads FXML; swaps full scenes |
| `ServerCommunicationTask` | application | `network/ServerCommunicationTask.java` | TCP client; implements `ServerConnection` |
| `Server` | server | `Server.java` | Accept loop; console commands; shared state |
| `ClientHandler` | server | `management/ClientHandler.java` | One thread per TCP client |
| `GameInstance` | server | `management/GameInstance.java` | Per-game session; observer; move relay |

## Request flow: offline move

```
User clicks square
  → GameController detects click
  → ExecuteMove (Task<Void>) submitted to single-thread executor
    → chess.movePiece(start, end)
      → game.movePiece(start, end)          [OfflineGame / Game]
        → Game.isMoveValid(move)
          → ruleset.getLegalSquares(...)     [filters via deep-copy + isCheck]
        → board.executeMove(move)           [handles en passant capture removal]
        → Game.evaluateGameEnd()            [checkmate/stalemate/draw detection]
        → turnCount++
        → notifyObservers()
          → GameController.onGameStateChanged()
            → Platform.runLater(this::update)
              → GameController re-reads board, redraws UI
```

## Request flow: online move

```
User clicks square
  → ExecuteMove → chess.movePiece(start, end)
    → OnlineGame.executeMove(move)
      → backup board state
      → isMoveValid → board.executeMove → turnCount++
      → sendMessageToServer("MOVE e2-e4")   [ServerCommunicationTask]
        → TCP → Server
          → ClientHandler receives "MOVE e2-e4"
            → GameInstance.processMessage → handleMove
              → game.movePiece(start, end)  [ServerGame]
              → sendMessageToPlayers("MOVE e2-e4")
      → notifyObservers()                   [OnlineGame]

Server relays move to opponent client:
  → ClientHandler reads "MOVE e2-e4"
    → GameInstance.handleMove on opponent's game
      → OnlineGame.handleMessage("MOVE e2-e4")
        → OnlineGame.handleMove
          → game.movePiece(start, end)
          → notifyObservers()
            → GameController.onGameStateChanged()
              → Platform.runLater → UI update
```

## Tech stack summary

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 17 |
| UI | JavaFX | 21 |
| Build | Maven | 4.0.0 (multi-module) |
| Testing | JUnit Jupiter | 5.10.1 (core), 5.8.1 (app/server) |
| Mocking | Mockito | 5.5.0 (application tests only) |
| Networking | Java standard library | `ServerSocket`, `Socket` |
| Localization | Java `ResourceBundle` | — |
| Theming | CSS | via JavaFX scene graph |
| Fat JARs | maven-shade-plugin | 3.2.4 |
