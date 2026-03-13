# Chess

> A modular, multi-layer Java chess application — offline play, TCP online multiplayer,
> and a headless API mode, all built around a strict façade API and enforced module boundaries.

![Java](https://img.shields.io/badge/Java-17-blue?logo=openjdk)
![JavaFX](https://img.shields.io/badge/JavaFX-21-orange)
![Maven](https://img.shields.io/badge/build-Maven-red?logo=apachemaven)
![License](https://img.shields.io/badge/license-MIT-green)


A three-module Maven project (Java 17 + JavaFX 21) implementing a complete chess platform:
a JavaFX desktop client, a pure-logic game engine, and a TCP multiplayer server — each module
with hard dependency boundaries enforced by architecture law.

## Features

- **Offline play** — two players on the same machine, no network needed
- **Online multiplayer** — TCP-based lobby system; host or join a game by code
- **Chess960 (Fischer Random Chess)** — randomized back-rank starting positions (960 variants), with full castling support and online play
- **Headless / API mode** — run without a GUI for programmatic game control (`nogui` flag)
- **Pawn promotion** — interactive piece-selection dialog mid-game
- **Castling** — king-side and queen-side with move-history tracking
- **En passant** — automatic detection and capture
- **Check-legality filtering** — prevents moving into check, castling through/out of check
- **Game-end detection** — checkmate, stalemate, 50-move rule, threefold repetition, insufficient material
- **Cinematic main menu** — full-screen animated launcher with a perspective chessboard background, floating particle effects, drifting chess piece silhouettes, and staggered entrance animations — all procedurally generated (zero static image assets); fully responsive layout with font and spacing scaling driven by JavaFX property bindings; GPU-optimized particle rendering (half-resolution canvas at 20 FPS with 2x scale-up)
- **6 visual themes** — Midnight, Ember, Abyss (dark) and Manuscript, Fjord, Sakura (light), each with integrated board colors and CSS-driven styling; the main menu adapts its entire visual identity to the active theme
- **Reduced Motion accessibility** — a toggle in Settings disables all main menu animations for users with motion sensitivity or lower-end hardware
- **Localization** — English and German (`i18n` properties files)
- **Concurrent server** — up to 40 simultaneous online games

## Architecture

### Module Structure

```text
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
              │  (no deps)      │
              └────────▲────────┘
                       │
┌──────────────────────┴──────────────────────────────┐
│                    server                           │
│       TCP server · Lobby · Move relay               │
│           depends on → core only                    │
└─────────────────────────────────────────────────────┘
```

### Architecture Laws

These rules are enforced across all modules — no exceptions without explicit approval:

1. **Hard module boundaries** — `core` has zero dependencies on `application` or `server`. `application` and `server` depend on `core` only, never on each other.
2. **Façade-only API** — all external interaction with the game engine goes through `Chess.java`. Direct instantiation of `Game` subclasses from outside `core` is banned.
3. **Observer for state propagation** — UI components implement `GameObserver` and register via `chess.addObserver()`. Polling loops are banned.
4. **Core is logic-only** — no UI imports, no JavaFX, no I/O in `core`.
5. **Strategy pattern for ruleset variants** — new rule sets implement the `Ruleset` interface; branching inside `Game` is banned.

### Design Patterns

| Pattern | Implementation | Purpose |
|---------|----------------|---------|
| **Façade** | `Chess.java` | Single API surface for all game interaction |
| **Observer** | `GameObserver` / `Observable` | Decoupled, push-based state propagation to UI |
| **Strategy** | `Ruleset` interface + `AbstractChessRuleset` + `StandardChessRuleset` / `Chess960Ruleset` | Pluggable rule variants without conditionals |
| **Factory Method** | `Game.createGame()` / `Game.createServerGame()` | Enforces module boundaries; keeps constructors package-private |
| **Template Method** | `Game.executeMove()` | Defines move sequence; `OnlineGame` overrides for network relay |

### Key Classes

| Class | Module | Role |
|-------|--------|------|
| `Chess` | application | Entry point (`Application`), façade, scene lifecycle |
| `Game` (abstract) | core | Board state, ruleset, observer list, move history |
| `OfflineGame` | core | Concrete local two-player game |
| `OnlineGame` | core | Concrete networked game; delegates I/O to `ServerConnection` |
| `ServerGame` | core | Server-side game; created only via `Game.createServerGame()` |
| `GameObserver` | core | Observer interface — `onGameStateChanged()` |
| `Ruleset` | core | Strategy interface — `getLegalMoves()`, `getLegalSquares()`, `isCheck()` |
| `MainMenuController` | application | FXML controller; builds layered cinematic menu with responsive layout, Canvas backgrounds, and animations |
| `ResponsiveMenuLayout` | application | Static utility producing JavaFX `DoubleBinding`s for font/spacing scaling and compact title position (translateX/Y, scale) based on window dimensions |
| `MenuLayoutTransition` | application | Single-phase panel open/close animation: smooth translate+scale on the content layer (title/accent/tagline) with reactive `DoubleBinding` targets for translateX/Y and scaleX/Y; slides navPanel via reactive `translateX` binding; no reparenting |
| `CinematicBackground` | application | Composite background layer: half-resolution particle canvas (20 FPS, 2x scale-up) + silhouettes + color probe host |
| `MenuParticleSystem` | application | Canvas rendering: floating particle effects |
| `MenuSilhouetteLayer` | application | Drifting chess piece silhouette animations |
| `MenuEntranceAnimation` | application | Orchestrates title + nav staggered entrance animations |
| `CinematicPanelAnimator` | application | Static utility for slide+fade entrance/exit animations on frosted-glass form panels |
| `MenuExitTransition` | application | Orchestrates cinematic exit animation before scene swap (uses relative `translateX`) |
| `PanelHost` | application | Interface for panel lifecycle (show/switch/close); decouples sub-panel controllers from `MainMenuController` |
| `PanelId` | application | Enum identifying each sub-panel (offline setup, online setup, settings, login, register, waiting-for-match) |
| `ThemeColorResolver` | application | Reads CSS looked-up colors at runtime for Canvas rendering |
| `GameController` | application | FXML controller; implements `GameObserver`; renders board |
| `ServerCommunicationTask` | application | `javafx.concurrent.Task`; implements `ServerConnection` |
| `GameInstance` | server | Per-game session; holds two `ClientHandler` refs; implements `GameObserver` |

## Project Structure

```text
chess/
├── modules/
│   ├── core/                          # Pure game logic — no UI, no I/O
│   │   └── src/main/java/io/github/conava/chess/
│   │       ├── data/                  # Board, Square, Piece, Player, Message types
│   │       ├── exceptions/            # IllegalMoveException
│   │       └── logic/
│   │           ├── game/              # Game (abstract), OfflineGame, OnlineGame, ServerGame
│   │           ├── moves/             # Move, CastleMove, PromotionMove
│   │           ├── observer/          # GameObserver, Observable
│   │           └── ruleset/           # Ruleset, AbstractChessRuleset, Standard/Chess960 rulesets
│   │
│   ├── application/                   # JavaFX desktop client
│   │   └── src/main/
│   │       ├── java/io/github/conava/chess/application/
│   │       │   ├── Chess.java         # Entry point + façade
│   │       │   ├── controllers/       # 11 FXML controllers (game, menus, dialogs, settings)
│   │       │   ├── navigation/        # SceneManager, OverlayManager, PanelHost, PanelId
│   │       │   ├── network/           # ServerCommunicationTask
│   │       │   ├── menu/              # Cinematic menu: responsive layout, background, particles, silhouettes, animations
│   │       │   ├── settings/          # SettingsService (includes reduced motion)
│   │       │   ├── tasks/             # ExecuteMove (background Task)
│   │       │   ├── theme/             # ThemeManager, Theme, ThemeColorResolver
│   │       │   └── i18n/              # I18n localization helper
│   │       └── resources/
│   │           ├── fxml/              # 11 screen layouts
│   │           ├── css/               # base.css + 6 theme stylesheets + dark/light base
│   │           ├── icon/              # 12 piece PNGs (6 pieces × 2 colors)
│   │           └── i18n/              # messages_en.properties, messages_de.properties
│   │
│   └── server/                        # TCP multiplayer server
│       └── src/main/java/io/github/conava/chess/server/
│           ├── Server.java            # Entry point; accept loop; console commands
│           └── management/
│               ├── ClientHandler.java # Runnable per connected client
│               └── GameInstance.java  # Per-game session manager
│
├── docs/
│   ├── decisions/                     # Architecture Decision Records (ADRs)
│   └── plans/                         # Design and implementation plans
│
└── Releases/                          # Built fat JARs
```

## Getting Started

### Prerequisites

- **Java 17+** — [Download](https://adoptium.net/)
- **Maven 3.8+** — [Download](https://maven.apache.org/)
- JavaFX SDK is **not** required for development mode (bundled via Maven plugin)

### Build

```bash
# Build all modules
mvn clean install

# Build only the GUI application (and its dependencies)
mvn clean package -pl modules/application -am

# Build only the server
mvn clean package -pl modules/server -am
```

### Run the GUI (Development)

```bash
mvn javafx:run -pl modules/application -am
```

### Run the GUI (Fat JAR)

Requires JavaFX SDK 21 on the module path:

```bash
java --module-path /path/to/javafx-sdk-21/lib \
     --add-modules javafx.controls,javafx.fxml \
     -jar modules/application/target/application-0.9.jar
```

### Run Headless (No-GUI / API Mode)

```bash
mvn javafx:run -pl modules/application -am -Djavafx.args=nogui
```

### Run the Server

```bash
# Default port 54321
java -jar modules/server/target/server-0.9.jar

# Custom port
java -jar modules/server/target/server-0.9.jar 8080
```

Server console commands (type while running):
- `stats` — show active game count and connection list
- `stop` — gracefully shut down the server

## API Reference

The `Chess` class (`io.github.conava.chess.application.Chess`) is the sole façade for all game interaction. No internal `Game` subclass is accessible from outside `core`.

### Game Lifecycle

| Method | Description |
|--------|-------------|
| `startGame(boolean online, RulesetOptions, String playerWhite, String playerBlack, Map<String, String> settings)` | Start a game. Pass `online=false` for local play; `online=true` for networked play with `settings` containing `"ip"` and `"port"` keys. |
| `endGame()` | Terminate the current game and release resources. |

**Online game settings map:**
```java
Map<String, String> settings = Map.of("ip", "192.168.1.10", "port", "54321");
chess.startGame(true, RulesetOptions.STANDARD, "Alice", "Bob", settings);
```

**Offline game (settings map is ignored):**
```java
chess.startGame(false, RulesetOptions.STANDARD, "Alice", "Bob", null);
```

### Move Execution

| Method | Description |
|--------|-------------|
| `movePiece(Square, Square)` | Execute a standard move from source to destination. Throws `IllegalMoveException`. |
| `promoteMove(Square, Square, Pieces)` | Execute a pawn promotion move. `Pieces` is an enum (`QUEEN`, `ROOK`, `BISHOP`, `KNIGHT`). Throws `IllegalMoveException`. |

### State Queries

| Method | Returns | Description |
|--------|---------|-------------|
| `getState()` | `GameState` | Current game state (RUNNING, CHECKMATE, etc.) |
| `getBoard()` | `Board` | Snapshot of current board |
| `getCurrentPlayer()` | `Player` | Player whose turn it is |
| `getPlayerWhite()` | `Player` | White player info |
| `getPlayerBlack()` | `Player` | Black player info |
| `getLegalSquares(Square)` | `List<Square>` | Legal destination squares for a piece |
| `getPieceAt(Square)` | `Piece` | Piece on the given square (null if empty) |
| `getMoveList()` | `List<String>` | Full move history as protocol strings (e.g. `"e2-e4"`) |
| `getJoinCode()` | `String` | Join code for the hosted online game (null if offline) |

### Observer Registration

```java
chess.addObserver(observer);    // Register a GameObserver
chess.removeObserver(observer); // Unregister
```

Implement `GameObserver`:

```java
public interface GameObserver {
    void onGameStateChanged();
}
```

All UI updates triggered by observer callbacks must be wrapped in `Platform.runLater()`.

### Game States

```java
public enum GameState {
    NO_GAME, WAITING_FOR_PLAYER, RUNNING, SERVER_ERROR,
    WHITE_WON_BY_CHECKMATE, BLACK_WON_BY_CHECKMATE,
    WHITE_WON_BY_RESIGNATION, BLACK_WON_BY_RESIGNATION,
    WHITE_WON_BY_TIMEOUT, BLACK_WON_BY_TIMEOUT,
    DRAW_BY_STALEMATE, DRAW_BY_INSUFFICIENT_MATERIAL,
    DRAW_BY_THREEFOLD_REPETITION, DRAW_BY_FIFTY_MOVE_RULE
}
```

## Server & Network Protocol

### Overview

The server accepts TCP connections on port `54321` (configurable). Each client connection runs in its own thread. Games are managed as isolated `GameInstance` sessions; the server relays moves between the two players and forwards terminal game states.

- **Max concurrent games:** 40 (semaphore-limited)
- **Protocol:** Newline-delimited plain-text messages
- **No TLS, authentication, or reconnection** (see [Roadmap](#roadmap))

### Connection Flow

```text
Client A                    Server                   Client B
   │                           │                         │
   │── CREATE_GAME ───────────>│                         │
   │<─ JOIN_CODE (gameId) ─────│                         │
   │                           │<──────── JOIN_GAME ─────│
   │<─ GAME_START ─────────────│─────────── GAME_START ──>│
   │                           │                         │
   │── MOVE (e2-e4) ──────────>│                         │
   │                           │─────────── MOVE ────────>│
   │<── MOVE (e7-e5) ──────────│<──────── MOVE ──────────│
   │                           │                         │
   │<─ GAME_STATUS (terminal) ─│──────── GAME_STATUS ────>│
```

### Message Format

| Message | Direction | Format |
|---------|-----------|--------|
| `CREATE_GAME` | Client → Server | `CREATE_GAME ruleset=STANDARD playerName=<name>` (or `ruleset=CHESS960`) |
| `JOIN_CODE` | Server → Client | `JOIN_CODE joinCode=<gameId>` (Chess960 adds `position=<0-959> ruleset=CHESS960`) |
| `JOIN_GAME` | Client → Server | `JOIN_GAME gameId=<id> playerName=<name>` |
| `MOVE` | Client ↔ Server | `MOVE <from>-<to>` (e.g. `MOVE e2-e4`) |
| `GAME_STATUS` | Server → Client | `GAME_STATUS status=<GameState>` |

If a player disconnects, the server awards a resignation win to the remaining player.

## Tech Stack

| Layer | Technology | Notes |
|-------|-----------|-------|
| Language | Java 17 | Records, sealed classes available but not yet used |
| UI Framework | JavaFX 21 | FXML layouts, CSS theming, `javafx.concurrent.Task` |
| Build | Maven 4.0.0 | Multi-module, maven-shade-plugin for fat JARs |
| Testing | JUnit 5.10.1 (core), JUnit 5.8.1 (app/server) | |
| Mocking | Mockito 5.5.0 | Application module tests only |
| Networking | Java standard library | `ServerSocket`, `Socket`, `ConcurrentHashMap`, `Semaphore` |
| Localization | Java `ResourceBundle` | English and German property files |
| Theming | CSS | base.css + 6 theme stylesheets (board colors integrated per theme) |

## Testing

### Coverage Summary

| Module | Test Classes | Focus |
|--------|-------------|-------|
| `core` | 26 | Board state, piece construction, move generation, observer notifications, standard and Chess960 rulesets, castling integration, deferred init, move parsing, game factory |
| `application` | 29 | Chess façade, i18n, settings service, theme manager, background move task, scene manager, cinematic menu components (responsive layout, layout transitions, background, particles, silhouettes, entrance, exit, panel animator), theme color resolver, reduced motion, main menu controller, sub-panel responsive bindings, CSS responsive validation, FXML panel structure, panel navigation |
| `server` | 4 | Server startup, game instance lifecycle (standard + Chess960), client handler integration |

### Run Tests

```bash
# All modules
mvn test

# Specific module
mvn test -pl modules/core
mvn test -pl modules/application
mvn test -pl modules/server

# Specific test class
mvn -pl modules/core -Dtest=BoardTest test
```

### Notable Test Classes

- `StandardChessRulesetTest` — validates legal move generation for all piece types
- `Chess960CastlingIntegrationTest` — full-stack Chess960 castling (27 tests)
- `Chess960StartPositionTest` — position generation constraints and Scharnagl round-trip
- `ObserverNotificationTest` — verifies Observer pattern wiring
- `GameFactoryTest` — verifies façade-enforced game creation constraints
- `GameDeferredInitTest` — deferred board initialization for online games
- `ResponsiveMenuLayoutTest` — font/spacing scaling bindings across window sizes
- `MenuLayoutTransitionTest` — smooth translate+scale animation with reactive bindings for panel open/close
- `CinematicBackgroundTest` — half-resolution particle canvas, GPU-optimized rendering at 20 FPS
- `MenuParticleSystemTest` — particle system lifecycle and rendering
- `MenuExitTransitionTest` — cinematic exit animation with relative translateX
- `CssResponsiveTest` — validates cinematic CSS has no hardcoded font sizes
- `ThemeColorResolverTest` — runtime CSS color resolution for Canvas layers
- `ClientHandlerIntegrationTest` — end-to-end server message flow

## Roadmap

### Chess Rules
- [x] En passant
- [x] Check-legality filtering (prevent moving into check)
- [x] Threefold repetition detection
- [x] Fifty-move rule enforcement
- [x] Insufficient material detection
- [x] Fix castling move validation

### UI / UX
- [x] Cinematic main menu with procedural animations
- [x] Responsive main menu layout (font/spacing scaling via property bindings)
- [x] Reduced Motion accessibility setting
- [ ] Keyboard shortcuts
- [ ] In-game clock / time controls
- [ ] Board coordinate labels (a-h, 1-8)

### Server
- [ ] TLS/SSL encryption
- [ ] Reconnection support after disconnect
- [ ] Player authentication
- [ ] Configurable game limit (currently hardcoded at 40)
- [ ] Move validation on the server side
- [ ] Persistent game history / replay

### Developer Experience
- [ ] CI/CD pipeline (GitHub Actions)
- [ ] Code coverage reporting
- [ ] Docker image for server

## Documentation

Full technical documentation lives in the [`docs/`](docs/) folder:

| Document | Description |
|----------|-------------|
| [Architecture Overview](docs/architecture/overview.md) | Module diagram, key classes, request flow traces |
| [Module Boundaries](docs/architecture/module-boundaries.md) | Dependency rules, what belongs where, what is banned |
| [Design Patterns](docs/architecture/design-patterns.md) | Façade, Observer, Strategy — how they are implemented |
| [Chess Façade API](docs/api/chess-facade.md) | Complete reference for all `Chess` public methods |
| [Getting Started](docs/guides/getting-started.md) | Prerequisites, build commands, first run |
| [Contributing](docs/guides/contributing.md) | Branch strategy, conventions, PR checklist |
| [Adding a Ruleset](docs/guides/adding-a-ruleset.md) | Step-by-step guide to implementing a new ruleset |
| [Swing → JavaFX Migration](docs/migration/swing-to-javafx.md) | Migration status and architecture decisions |
| [Architecture Decisions (ADRs)](docs/decisions/) | Why key decisions were made |
| [Deferred Findings](docs/deferred-findings.md) | Unresolved review findings tracked for future work |

## Contributing

### Branch Strategy

All work happens in feature branches — no direct commits to `main`.

**Branch naming:** `<type>/<short-slug>`

Types: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`

Example: `feat/en-passant`, `fix/castling-validation`

### Rules

1. Architecture Laws (listed above) apply to every PR — no exceptions.
2. Every new public class in `core` must have a corresponding unit test.
3. No UI code, JavaFX imports, or I/O in `core`.
4. All game state changes must propagate via the Observer pattern.
5. Run `mvn test` before opening a PR; all tests must pass.

### Development Flow

```bash
git checkout -b feat/your-feature
# implement + test
mvn test
git push origin feat/your-feature
# open PR against main
```

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
