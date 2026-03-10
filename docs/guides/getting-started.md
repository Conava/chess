# Getting Started

This guide walks through prerequisites, building the project, and running the application
or server for the first time.

## Prerequisites

| Tool | Minimum version | Notes |
|------|----------------|-------|
| Java (JDK) | 17 | [Adoptium](https://adoptium.net/) or any JDK 17+ distribution |
| Maven | 3.8 | [Download](https://maven.apache.org/download.cgi) |
| JavaFX SDK | Not required for development | Only needed for the fat-JAR launch path |

Verify your installation:

```bash
java -version    # should print 17 or higher
mvn -version     # should print 3.8 or higher
```

## Clone the repository

```bash
git clone https://github.com/Conava/chess.git
cd chess
```

## Build

### Build everything (recommended first step)

```bash
mvn clean install
```

This compiles `core`, `application`, and `server`, runs all tests, and produces fat JARs
in `modules/application/target/` and `modules/server/target/`. Built JARs are also copied
to `Releases/0.9/`.

### Build only the GUI application

```bash
mvn clean package -pl modules/application -am
```

The `-am` ("also make") flag ensures `core` is compiled and included even when only
targeting `application`.

### Build only the server

```bash
mvn clean package -pl modules/server -am
```

## Run the GUI application

### Development mode (recommended)

Uses the JavaFX Maven plugin. JavaFX is downloaded and wired automatically — no separate
SDK installation required.

```bash
mvn javafx:run -pl modules/application -am
```

### Fat JAR mode

Requires JavaFX SDK 21 installed locally:

```bash
java --module-path /path/to/javafx-sdk-21/lib \
     --add-modules javafx.controls,javafx.fxml \
     -jar modules/application/target/application-0.9.jar
```

### Headless / no-GUI mode

Starts the application without displaying a window. Useful for testing the API layer or
running in headless environments.

```bash
mvn javafx:run -pl modules/application -am -Djavafx.args=nogui
```

## Run the server

```bash
# Default port 54321
java -jar modules/server/target/server-0.9.jar

# Custom port
java -jar modules/server/target/server-0.9.jar 8080
```

The server logs to standard output. Console commands (type while the server is running):

| Command | Effect |
|---------|--------|
| `stats` | Print active game count and connection list |
| `stop` | Gracefully shut down the server |

## Run the tests

```bash
# All modules
mvn test

# Specific module
mvn test -pl modules/core
mvn test -pl modules/application
mvn test -pl modules/server

# Specific test class (example)
mvn -pl modules/core -Dtest=BoardTest test
```

## First steps in the application

1. Launch the application with `mvn javafx:run -pl modules/application -am`.
2. The main menu appears. Select **Play Offline** for a local two-player game.
3. Enter player names (optional) and click **Start**.
4. Click a piece to see its legal destination squares highlighted (only legal moves are
   shown — moves that would leave your king in check are excluded). Click a highlighted
   square to move.
5. Pawn promotion triggers an overlay — select the desired piece.
6. The game ends on checkmate, stalemate, threefold repetition, fifty-move rule, or
   insufficient material. En passant and castling are fully supported.

## Playing online

Both players need access to a running server instance.

**Host:**
1. Start the server: `java -jar modules/server/target/server-0.9.jar`
2. In the application, select **Play Online** → **Create Game**.
3. A join code is displayed in the waiting overlay. Share it with the other player.

**Join:**
1. In the application, select **Play Online** → **Join Game**.
2. Enter the server IP, port, and the join code provided by the host.
3. Click **Join**.

## Theming and settings

Open **Settings** from the main menu to change:

- **UI theme** — Midnight, Ember, Manuscript, Fjord (dark/light variants)
- **Board theme** — Classic, Ocean, Walnut
- **Language** — English, German
- **Default player names** — prefilled in setup dialogs

Settings are persisted via `java.util.prefs.Preferences` and survive application restarts.

## Project layout quick reference

```
chess/
├── modules/
│   ├── core/        # Pure game logic (no UI, no I/O)
│   ├── application/ # JavaFX desktop client
│   └── server/      # TCP multiplayer server
├── docs/            # This documentation
├── Releases/        # Built fat JARs
└── pom.xml          # Root multi-module POM
```

For a deeper understanding of the architecture, start with
[architecture/overview.md](../architecture/overview.md).
