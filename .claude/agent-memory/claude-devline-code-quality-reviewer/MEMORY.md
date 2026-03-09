# Code Quality Reviewer Memory

## Project Structure
- Java 17, Maven multi-module: core, application, server
- Application module: JavaFX UI (migration from Swing complete)
- Entry point: `io.github.conava.chess.application.Chess` extends `javafx.application.Application`
- Facade pattern: all UI code interacts with core through `Chess` class only

## Key Files
- GameController: `modules/application/src/main/java/.../controllers/GameController.java`
- SceneManager: `modules/application/src/main/java/.../navigation/SceneManager.java`
- GameState enum: `modules/core/src/main/java/.../logic/game/GameState.java`
- i18n bundles: `modules/application/src/main/resources/i18n/messages_{en,de}.properties`

## Recurring Patterns to Watch
- GameController has multiple exit paths that should all clean up the ExecutorService
- `isOnline` detection via `getJoinCode() != null` is fragile -- consider constructor param
- GameState naming: `WHITE_WON_BY_*`, `BLACK_WON_BY_*`, `DRAW_*`
- JavaFX tests need `Platform.startup()` and headless guards
