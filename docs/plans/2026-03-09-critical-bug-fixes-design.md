# Critical Bug Fixes Design

Status: proposed
Date: 2026-03-09

## Overview

Five bugs identified in the JavaFX chess application after the Swing-to-JavaFX migration.
Three are logic errors in controllers, one is a missing test, and one is stale documentation.
All fixes are confined to the `application` module; `core` and `server` are not affected.

---

## Bug 1: showErrorAndReturnToMenu ignores user response

**File:** `GameController.java:374-378`

**Current behavior:**
```java
private void showErrorAndReturnToMenu(String title, String message) {
    sceneManager.showConfirm(message);   // return value discarded
    chess.endGame();                      // always runs
    sceneManager.showMainMenu();          // always runs
}
```
`showConfirm()` returns `boolean` via `OverlayManager.showConfirm()` (which uses
`Platform.enterNestedEventLoop` to block until the user clicks Yes or No). The return
value is discarded, so the game always ends regardless of what the user clicks.

**Fix:**
Capture the return value. Only call `chess.endGame()` and `sceneManager.showMainMenu()`
when the user confirms. The `title` parameter is unused (the confirm overlay does not
display a title) and should be removed from the method signature. The single call site
at line 205-206 should be updated accordingly.

```java
private void showErrorAndReturnToMenu(String message) {
    boolean confirmed = sceneManager.showConfirm(message);
    if (confirmed) {
        chess.endGame();
        sceneManager.showMainMenu();
    }
}
```

The call site at line 205-206 changes from:
```java
case SERVER_ERROR -> showErrorAndReturnToMenu(
        i18n.get("error.server.title"), i18n.get("error.server"));
```
to:
```java
case SERVER_ERROR -> showErrorAndReturnToMenu(i18n.get("error.server"));
```

**Impact:** Minimal. One method, one call site.

---

## Bug 2: Rematch hardcodes RulesetOptions.STANDARD

**File:** `GameController.java:364-369`

**Current behavior:**
```java
chess.startGame(false, RulesetOptions.STANDARD,
        whitePlayerName, blackPlayerName, Map.of());
```
When the user clicks "Rematch", a new game is started with `RulesetOptions.STANDARD`
regardless of what ruleset was originally chosen. The original ruleset is never stored
anywhere accessible to `GameController`.

**Root cause:** The `Chess` facade does not expose the ruleset of the current game, and
`GameController` does not receive or store the ruleset at construction time.

**Fix approach -- store ruleset in GameController:**

The simplest fix is to have `GameController` query the ruleset at construction or
initialization time. However, `Chess` (the facade) does not expose the current game's
ruleset. Two sub-options:

**Option A -- Add `getRuleset()` to the Chess facade.**
Add a method `public RulesetOptions getRuleset()` to `Chess.java` that delegates to
`game.getRuleset()`. The `Game` class in `core` already holds the `Ruleset` but does not
expose which `RulesetOptions` enum value created it. This would require adding a
`RulesetOptions` field to `Game` and a getter -- a change to `core`.

**Option B -- Pass ruleset through the constructor chain (recommended).**
`GameController` already receives `Chess` and `SceneManager` at construction time. The
ruleset is known at the point where `showGame()` is called (in `MainMenuController.onLocalGame()`
and `onOnlineGame()`). The information flow is:

1. `MainMenuController` calls `chess.startGame(false, setup.getRuleset(), ...)` then
   `sceneManager.showGame()`.
2. `SceneManager.showGame()` constructs `GameController`.

The cleanest path: add a `RulesetOptions` field to `SceneManager` that gets set before
`showGame()` is called, and read it in `GameController`. However, `SceneManager` should
not accumulate game-specific state.

Better: add an overload `showGame(RulesetOptions ruleset)` to `SceneManager`, and pass
it through to the `GameController` constructor. `GameController` stores it in a field
and uses it in the rematch call.

Changes required:
- `SceneManager.showGame()` gains a `RulesetOptions` parameter.
- `GameController` constructor gains a `RulesetOptions` parameter and stores it as a field.
- `showGameEndDialog()` uses the stored `ruleset` field instead of `RulesetOptions.STANDARD`.
- `MainMenuController.onLocalGame()` and `onOnlineGame()` pass `setup.getRuleset()` to
  `sceneManager.showGame(ruleset)`.
- The rematch `showGame()` call inside `showGameEndDialog` also passes the stored ruleset.

**Rematch also needs to pass the correct `isOnline` flag.** Currently rematch always passes
`false` for the online parameter. For online games rematch is disabled
(`rematchBtn.setDisable(true)` in `GameEndController`), so this is not a live bug, but
the code should still use the stored value for correctness. The `isOnline` boolean can be
derived from `chess.getJoinCode() != null` which is already computed on line 350.

**Impact:** Moderate. Touches `SceneManager`, `GameController`, `MainMenuController`.
No `core` changes needed.

---

## Bug 3: Game-end title is ambiguous for local play

**File:** `GameEndController.java:70-77`, `messages_en.properties:61`

**Current behavior:**
```java
if (isWin) {
    outcomeTitle.setText(i18n.get("game.end.title.win"));    // "You Win!"
} else if (isDraw) {
    outcomeTitle.setText(i18n.get("game.end.title.draw"));   // "Draw"
} else {
    outcomeTitle.setText(i18n.get("game.end.title.loss"));   // "You Lose"
}
```
For local 2-player games, "You Win!" and "You Lose" are meaningless -- there is no "you".
The controller already has the `isOnline` flag and both player names.

**Fix:**
Branch on `isOnline` when selecting the title:

- **Online games:** Keep the existing "You Win!" / "You Lose" / "Draw" strings.
- **Local games:** Use new i18n keys that include the winning color/player name.

The `GameState` enum names already encode which color won (`WHITE_WON_*`, `BLACK_WON_*`).
The controller can derive the winner's name:

```java
boolean whiteWon = state.name().startsWith("WHITE_WON");
String winnerName = whiteWon ? whiteName : blackName;
```

**New i18n keys:**

| Key | English | German |
|-----|---------|--------|
| `game.end.title.win.local` | `{0} wins!` | `{0} gewinnt!` |

For draws, the existing `game.end.title.draw` works for both modes.

For losses in local mode, there is no "loser" perspective -- the game simply ends with
a winner. Only the `game.end.title.win.local` key is needed.

**Updated logic in GameEndController.initialize():**

```java
if (isWin) {
    if (isOnline) {
        outcomeTitle.setText(i18n.get("game.end.title.win"));
    } else {
        boolean whiteWon = state.name().startsWith("WHITE_WON");
        String winnerName = whiteWon ? whiteName : blackName;
        outcomeTitle.setText(MessageFormat.format(i18n.get("game.end.title.win.local"), winnerName));
    }
} else if (isDraw) {
    outcomeTitle.setText(i18n.get("game.end.title.draw"));
} else {
    outcomeTitle.setText(i18n.get("game.end.title.loss"));
}
```

The `else` (loss) branch only triggers for online games (in local play, every win state
starts with `WHITE_WON` or `BLACK_WON` -- there is no separate loss state for the other
player). So the loss text remains online-only and is correct as-is.

**Impact:** Small. One controller method, two properties files, one new key each.

---

## Bug 4: Missing SceneManager unit test

**File:** `SceneManagerTest.java` does not exist.

**Current test files in application module:**
- `ChessTest.java` (exists, previously had empty test bodies)
- `I18nTest.java`
- `ExecuteMoveTest.java`
- `SettingsServiceTest.java`
- `ThemeManagerTest.java`

**Challenge:** `SceneManager` is tightly coupled to JavaFX (`Stage`, `Scene`, `FXMLLoader`,
`Platform.enterNestedEventLoop`). Testing it requires either:

1. Running a JavaFX Application Thread (via TestFX or JUnit5 JavaFX extension).
2. Mocking the JavaFX classes (difficult -- `Stage` and `Scene` are not interfaces).

**Test strategy:**

Use the **TestFX** library (`org.testfx:testfx-junit5`) or the simpler approach of
initializing a minimal JavaFX toolkit in a `@BeforeAll`. The project already has JavaFX 21
as a dependency, so toolkit initialization via `Platform.startup(() -> {})` is available.

However, full FXML loading requires the FXML files on the classpath and valid controller
wiring. A pragmatic approach:

**What to test (unit-level, no full FXML loading):**

1. **Constructor stores dependencies correctly.** Verify accessors (`getPrimaryStage()`,
   `getChess()`, `getThemeManager()`, `getI18n()`, `getSettingsService()`) return the
   injected objects. This requires no JavaFX toolkit at all.

2. **`requireOverlay()` throws before any scene is shown.** Call `showConfirm()` or
   `dismissOverlay()` before `showMainMenu()` and assert `IllegalStateException`.

3. **`showMainMenu` / `showGame` / `showSettings` load correct FXML and set stage properties.**
   These require the JavaFX toolkit. Use `Platform.startup()` in `@BeforeAll`, create a
   mock or real `Stage`, and verify:
   - After `showMainMenu()`: stage width/height are 900/650, stage is not maximized.
   - After `showGame()`: stage is maximized.
   - After `showSettings()`: stage is not maximized, dimensions are 760/920.

**What to mock:**
- `Chess` -- mock or stub (no-arg constructor exists, or use Mockito).
- `ThemeManager` -- mock; `registerScene()` should be verifiable.
- `I18n` -- real instance with a test bundle, or mock `getBundle()`.
- `SettingsService` -- mock.
- `Stage` -- real JavaFX `Stage` (must be created on the FX thread).

**Test file location:**
`modules/application/src/test/java/io/github/conava/chess/application/navigation/SceneManagerTest.java`

**Minimum assertions (4-5 tests):**

| Test | Assertion |
|------|-----------|
| `accessors_returnInjectedDependencies` | All five getters return the objects passed to the constructor. |
| `showConfirm_beforeSceneShown_throwsISE` | `IllegalStateException` from `requireOverlay()`. |
| `showMainMenu_setsStageProperties` | Stage shown, not maximized, scene is set. |
| `showGame_maximizesStage` | Stage is maximized after call. |
| `showSettings_setsCorrectDimensions` | Stage not maximized, dimensions match. |

The FXML-loading tests (last three) need the toolkit and real FXML files. If the test
environment cannot support JavaFX toolkit initialization (e.g., headless CI), these tests
should be annotated with a condition (`@EnabledIf`) or use Monocle as the headless GL
pipeline (`-Dglass.platform=Monocle -Dprism.order=sw`).

**Impact:** New test file only. No production code changes.

---

## Bug 5: modules/application/CLAUDE.md is completely stale

**File:** `modules/application/CLAUDE.md`

**Current state:** The file describes a Swing-based application in exhaustive detail.
It references `JFrame`, `JPanel`, `JDialog`, `SwingWorker`, `ColorScheme`, `BoardPanel`,
`BoardButton`, `MainFrame`, and many other classes that no longer exist. The JavaFX
migration is complete.

**What the updated file should describe:**

1. **Status:** Active JavaFX application. Migration from Swing is complete.

2. **Responsibility:** Same high-level responsibility (desktop GUI, entry point), but
   now using JavaFX with FXML + Controller pattern.

3. **Package structure (current):**
   - `io.github.conava.chess.application` -- Entry point (`Chess.java`, extends `Application`).
   - `io.github.conava.chess.application.controllers` -- FXML controllers: `GameController`,
     `GameEndController`, `MainMenuController`, `OfflineSetupController`,
     `OnlineSetupController`, `PromotionController`, `SettingsController`,
     `WaitingController`.
   - `io.github.conava.chess.application.navigation` -- `SceneManager`, `OverlayManager`.
   - `io.github.conava.chess.application.i18n` -- `I18n` (ResourceBundle wrapper).
   - `io.github.conava.chess.application.theme` -- `ThemeManager`, `Theme`, `BoardTheme`.
   - `io.github.conava.chess.application.settings` -- `SettingsService`.
   - `io.github.conava.chess.application.network` -- `ServerCommunicationTask`.
   - `io.github.conava.chess.application.tasks` -- `ExecuteMove` (now a `Runnable`, not
     `SwingWorker`).

4. **Key classes:** Brief descriptions of `Chess`, `SceneManager`, `OverlayManager`,
   `GameController`, `GameEndController`, `MainMenuController`, and the setup controllers.

5. **Design patterns:** Facade (`Chess`), Observer (`GameController` implements
   `GameObserver`), FXML+Controller (all UI screens).

6. **Architecture law compliance:** Same five laws, all compliant.

7. **Known debt:** Remove all Swing-era debt items. Add current items if any.

The rewrite should follow the same section structure as the `core` module's CLAUDE.md for
consistency, but be proportionally shorter since the application module is simpler.

**Impact:** Documentation only. No code changes.

---

## Open Questions

None. All five bugs have clear, self-contained fixes.

---

## Summary of Changes by File

| File | Bugs addressed | Type |
|------|---------------|------|
| `GameController.java` | 1, 2 | Logic fix |
| `GameEndController.java` | 3 | Logic fix |
| `SceneManager.java` | 2 | Signature change |
| `MainMenuController.java` | 2 | Pass ruleset to showGame |
| `messages_en.properties` | 3 | New key |
| `messages_de.properties` | 3 | New key |
| `SceneManagerTest.java` | 4 | New file |
| `modules/application/CLAUDE.md` | 5 | Full rewrite |
