# Contributing

This guide covers the conventions, workflow, and rules for contributing to this project.
Read the full [architecture overview](../architecture/overview.md) before making any changes.

## Prerequisites

- Understand the [module boundary rules](../architecture/module-boundaries.md) — violations
  will be rejected in review.
- Understand the three design patterns in use: [Façade, Observer, Strategy](../architecture/design-patterns.md).
- Read `CLAUDE.md` at the repo root for the build commands and architecture laws.

## Branch strategy

All work happens in feature branches. No direct commits to `main`.

### Branch naming

```
<type>/<short-slug>
```

Types:
- `feat` — new feature
- `fix` — bug fix
- `refactor` — code restructuring without behavior change
- `test` — test additions or fixes
- `docs` — documentation only
- `chore` — build, tooling, dependency updates

Examples: `feat/en-passant`, `fix/castling-validation`, `docs/api-reference`

### Workflow

```bash
git checkout main
git pull
git checkout -b feat/your-feature

# implement + test
mvn test

git push origin feat/your-feature
# open a pull request against main
```

Human review is required before merge. Branches are deleted after merge.

## Architecture laws — no exceptions

Every pull request must comply with all five laws:

1. **Module boundaries are hard.** `core` has zero dependencies on `application` or `server`.
   `application` and `server` depend on `core` only.
2. **The `Chess` façade is the only API surface.** Application and server code must go
   through `Chess.java`. Direct instantiation of `Game` subclasses from outside `core` is
   banned.
3. **Observer pattern for all state propagation.** UI components must implement `GameObserver`
   and register via `chess.addObserver()`. Polling loops are banned.
4. **`core` is logic-only.** No UI imports, no JavaFX, no Swing, no I/O in `core`.
5. **Strategy pattern owns ruleset variation.** New rule variants must implement `Ruleset`,
   not branch inside `Game` or `Chess`.

## Code conventions

### General

- Java 17. Records, switch expressions, and text blocks are all available.
- All packages under `io.github.conava.chess`.
- No test code in `src/main`. No production logic in `src/test`.
- Every new public class in `core` must have a corresponding unit test.
- `CLAUDE.md` files are living documents — update them when architecture decisions change.

### Javadoc

All public methods and classes in `core` must have Javadoc. Use present tense. Document
the contract (what it returns, what throws, what nulls mean), not the implementation.

### Naming

- Classes: `UpperCamelCase`
- Methods and fields: `lowerCamelCase`
- Constants: `UPPER_SNAKE_CASE`
- FXML files: `kebab-case.fxml`
- CSS files: `kebab-case.css`
- i18n property files: `messages_en.properties`, `messages_de.properties`

### JavaFX conventions

- Every screen has an FXML file in `src/main/resources/fxml/` and a controller in
  `src/main/java/.../controllers/`.
- Controllers are constructed with dependencies by the caller and injected via
  `FXMLLoader.setControllerFactory`. Do not rely on FXML-based reflection construction.
- All JavaFX node mutations from observer callbacks must be wrapped in `Platform.runLater()`.
- CSS stylesheets go in `src/main/resources/css/`.
- Piece icons go in `src/main/resources/icon/` using the pattern `{piece}_{color}.png`.

### i18n

All user-visible strings must use the `I18n` helper:

```java
label.setText(i18n.get("key.from.bundle"));
```

Add new keys to both `messages_en.properties` and `messages_de.properties`.

## Testing

### Requirements

- Run `mvn test` before opening a PR. All tests must pass.
- Every new public class in `core` needs at least one test class.
- Application tests that require a running JavaFX runtime should use Mockito to mock the
  façade or use headless mode.
- Server tests should test the protocol message flow, not the TCP socket directly (use
  `ClientHandlerIntegrationTest` as a reference).

### Test class locations

| Source module | Test location |
|--------------|---------------|
| `core` | `modules/core/src/test/java/io/github/conava/chess/core/` |
| `application` | `modules/application/src/test/java/io/github/conava/chess/application/` |
| `server` | `modules/server/src/test/java/io/github/conava/chess/server/` |

### Notable existing tests

- `StandardChessRulesetTest` — validates legal-move generation for all piece types
- `ObserverNotificationTest` — verifies Observer pattern wiring
- `GameFactoryTest` — verifies that `Game.createGame()` creates the correct subtypes
- `ClientHandlerIntegrationTest` — end-to-end server message flow

## Pull request checklist

Before opening a PR, confirm:

- [ ] `mvn test` passes (all modules)
- [ ] No new `import javafx.*` in `modules/core/src/main/`
- [ ] No new `import io.github.conava.chess.server.*` in `modules/application/src/main/`
- [ ] No new `import io.github.conava.chess.application.*` in `modules/server/src/main/`
- [ ] No direct instantiation of `OfflineGame`, `OnlineGame`, or `ServerGame` outside `core`
- [ ] No polling loops on game state in `application`
- [ ] All user-visible strings go through `I18n`
- [ ] All `GameObserver` callbacks in `application` use `Platform.runLater()`
- [ ] Every new public class in `core` has a unit test
- [ ] `docs/` is updated if the public API, architecture, or a documented pattern changed
- [ ] `CLAUDE.md` is updated if architecture decisions changed

## Extending the codebase

| Task | Where to start |
|------|---------------|
| Add a new ruleset | [guides/adding-a-ruleset.md](adding-a-ruleset.md) |
| Add a new screen | Create FXML + controller; register in `SceneManager` |
| Add a new overlay | Create FXML + controller; call `overlayManager.showOverlay(...)` |
| Add a server command | Extend `Server.startConsoleCommandListener` |
| Add a wire protocol message | Add to `MessageType` enum; handle in `ClientHandler` and `OnlineGame` |
