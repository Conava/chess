# Chess

> A modular, multi-layer Java chess application — offline play, TCP online multiplayer,
> and a headless API mode, all built around a strict facade API and enforced module boundaries.

![Java](https://img.shields.io/badge/Java-17-blue?logo=openjdk)
![JavaFX](https://img.shields.io/badge/JavaFX-21-orange)
![Maven](https://img.shields.io/badge/build-Maven-red?logo=apachemaven)
![Tests](https://img.shields.io/badge/tests-75_classes-brightgreen)
![License](https://img.shields.io/badge/license-MIT-green)

## Why This Project

This is a portfolio project that demonstrates clean architecture, design pattern fluency, and full-stack Java development — from a GPU-optimized cinematic UI to a concurrent multiplayer server with authentication and persistence. Every module enforces strict dependency boundaries, and the entire codebase is covered by 75 test classes across all layers.

## Highlights

- **Three-module architecture** with hard dependency boundaries — `core` (pure logic, zero dependencies), `application` (JavaFX desktop client), and `server` (TCP multiplayer) — each independently buildable and testable
- **Five GoF design patterns** applied where they solve real problems: Facade, Observer, Strategy, Factory Method, and Template Method
- **75 test classes** covering game logic, UI components, animations, server lifecycle, authentication, and network protocol
- **Cinematic main menu** — procedurally generated animated launcher with particle effects, drifting piece silhouettes, responsive layout scaling, and GPU-optimized half-resolution Canvas rendering at 20 FPS
- **Complete chess engine** — all standard rules plus Chess960 (Fischer Random) with full castling support across 960 starting positions
- **Multiplayer server** — concurrent TCP server with authentication, SQLite persistence, matchmaking, join-code lobby system, and configurable game limits

## Features

### Gameplay
- **Offline play** — two players on the same machine
- **Online multiplayer** — TCP lobby system; host or join by code; board auto-flips per player
- **Chess960 (Fischer Random)** — 960 randomized starting positions with full castling support
- **Headless / API mode** — run without a GUI for programmatic game control
- **Complete rule enforcement** — castling, en passant, pawn promotion, check-legality filtering, checkmate, stalemate, 50-move rule, threefold repetition, insufficient material

### UI / UX
- **Cinematic main menu** — full-screen animated launcher with a perspective chessboard background, floating particle effects, drifting chess piece silhouettes, and staggered entrance animations — all procedurally generated with zero static image assets
- **Fully responsive layout** — board sizes from window height with side panels filling remaining space; font and spacing scaling driven by JavaFX property bindings
- **Two-column scoresheet** — in-game move list with move numbers, white moves, and black moves
- **6 visual themes** — Midnight, Ember, Abyss (dark) and Manuscript, Fjord, Sakura (light), with integrated board colors and CSS-driven styling
- **Reduced Motion accessibility** — disables all menu animations for users with motion sensitivity
- **Localization** — English and German via `ResourceBundle` property files

### Server
- **Concurrent game hosting** — up to 40 simultaneous games (semaphore-limited)
- **Authentication** — login, register, and token-based session persistence
- **SQLite persistence** — users, sessions, and game data
- **Matchmaking** — automatic opponent pairing
- **Reconnection handling** — game pauses on disconnect, resumes on reconnect
- **Server-side move validation** — every move is validated through the full chess engine before relay; illegal moves are rejected
- **Configurable** — port, game limits, timeouts, and expiry via `server.properties`

## Architecture

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

1. **Hard module boundaries** — `core` has zero dependencies on `application` or `server`. `application` and `server` depend on `core` only, never on each other.
2. **Facade-only API** — all external interaction with the game engine goes through `Chess.java`. Direct instantiation of `Game` subclasses from outside `core` is banned.
3. **Observer for state propagation** — UI components implement `GameObserver` and register via `chess.addObserver()`. No polling.
4. **Core is logic-only** — no UI imports, no JavaFX, no I/O in `core`.
5. **Strategy pattern for rulesets** — new rule variants implement the `Ruleset` interface; branching inside `Game` is banned.

### Design Patterns

| Pattern | Where | Why |
|---------|-------|-----|
| **Facade** | `Chess.java` | Single API surface — isolates all game interaction behind one entry point |
| **Observer** | `GameObserver` / `Observable` | Push-based state propagation; decouples core from UI and network |
| **Strategy** | `Ruleset` → `StandardChessRuleset` / `Chess960Ruleset` | Pluggable rule variants without conditionals in game logic |
| **Factory Method** | `Game.createGame()` / `Game.createServerGame()` | Enforces module boundaries; constructors stay package-private |
| **Template Method** | `Game.executeMove()` | Defines move sequence; `OnlineGame` overrides for network relay |

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| UI | JavaFX 21 (FXML layouts, CSS theming, `javafx.concurrent.Task`) |
| Build | Maven (multi-module, shade plugin for fat JARs) |
| Database | SQLite (server-side user/session/game persistence) |
| Testing | JUnit 5, Mockito |
| Networking | Java `ServerSocket` / `Socket`, `ConcurrentHashMap`, `Semaphore` |
| Localization | `ResourceBundle` (English, German) |

## Project Structure

```text
chess/
├── modules/
│   ├── core/                          # Pure game logic — no UI, no I/O
│   │   └── src/main/java/.../
│   │       ├── data/                  # Board, Square, Piece, Player, Message types
│   │       ├── exceptions/            # IllegalMoveException
│   │       └── logic/
│   │           ├── game/              # Game (abstract), OfflineGame, OnlineGame, ServerGame
│   │           ├── moves/             # Move, CastleMove, PromotionMove
│   │           ├── observer/          # GameObserver, Observable
│   │           └── ruleset/           # Ruleset, Standard/Chess960 rulesets
│   │
│   ├── application/                   # JavaFX desktop client
│   │   └── src/main/
│   │       ├── java/.../
│   │       │   ├── Chess.java         # Entry point + facade
│   │       │   ├── controllers/       # 11 FXML controllers
│   │       │   ├── navigation/        # SceneManager, OverlayManager, PanelHost
│   │       │   ├── network/           # ServerCommunicationTask
│   │       │   ├── menu/              # Cinematic menu system (9 classes)
│   │       │   ├── settings/          # SettingsService
│   │       │   ├── theme/             # ThemeManager, Theme enum, ThemeColorResolver
│   │       │   └── i18n/              # Localization helper
│   │       └── resources/
│   │           ├── fxml/              # 11 screen layouts
│   │           ├── css/               # base.css + 6 theme stylesheets
│   │           ├── icon/              # 12 piece PNGs (6 × 2 colors)
│   │           └── i18n/              # messages_en/de.properties
│   │
│   └── server/                        # TCP multiplayer server
│       └── src/main/java/.../
│           ├── Server.java            # Entry point, accept loop, console commands
│           ├── config/                # ServerConfig (properties loader)
│           ├── management/            # ClientHandler, GameInstance, GameManager
│           ├── auth/                  # AuthService (login/register/token)
│           ├── db/                    # DatabaseManager (SQLite)
│           ├── persistence/           # Game, Session, User repositories
│           └── matchmaking/           # MatchmakingService
│
└── Releases/                          # Pre-built fat JARs
```

## Getting Started

### Prerequisites

- **Java 17+** — [adoptium.net](https://adoptium.net/)
- **Maven 3.8+** — [maven.apache.org](https://maven.apache.org/)

### Build & Run

```bash
# Build all modules
mvn clean install

# Run the desktop client (development mode)
mvn javafx:run -pl modules/application -am

# Run headless / API mode
mvn javafx:run -pl modules/application -am -Djavafx.args=nogui

# Run the multiplayer server (default port 54321)
java -jar modules/server/target/server-0.9.jar

# Run the client from fat JAR (requires JavaFX SDK on module path)
java --module-path /path/to/javafx-sdk-21/lib \
     --add-modules javafx.controls,javafx.fxml \
     -jar modules/application/target/application-0.9.jar
```

### Server Configuration

The server reads `server.properties` from the working directory (falls back to defaults):

```properties
port=54321
max_games=40
disconnect_timeout_seconds=300
db_path=chess.db
session_expiry_days=30
join_code_expiry_seconds=600
```

Console commands: `stats` (active games/connections), `stop` (graceful shutdown).

## Testing

75 test classes across all three modules:

| Module | Tests | Coverage |
|--------|-------|----------|
| `core` | 34 | Board state, move generation, all piece types, standard + Chess960 rulesets, castling integration, observer notifications, game factory, online game protocol |
| `application` | 31 | Facade API, i18n, settings, theme manager, cinematic menu components (responsive layout, transitions, particles, silhouettes, animations), controller logic, CSS validation |
| `server` | 10 | Server lifecycle, config, game instance management, client handler integration, matchmaking, auth service, all three persistence repositories |

```bash
mvn test                        # All modules
mvn test -pl modules/core       # Core only
mvn test -pl modules/server     # Server only
```

## Roadmap

- [ ] In-game clock / time controls
- [ ] Board coordinate labels (a-h, 1-8)
- [ ] TLS/SSL encryption
- [ ] Persistent game history / replay
- [ ] CI/CD pipeline
- [ ] Docker image for server

## License

MIT — see [LICENSE](LICENSE) for details.
