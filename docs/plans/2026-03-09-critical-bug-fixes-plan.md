# Critical Bug Fixes -- Execution Plan

Date: 2026-03-09
Design doc: `docs/plans/2026-03-09-critical-bug-fixes-design.md`
Branch: `fix/critical-bugs`
Module: `application` only (`core` and `server` untouched)

---

## Task Summary

| ID | Name | Parallel Group | Depends On |
|----|------|:-:|------------|
| task-1 | Fix showErrorAndReturnToMenu ignoring user response | 1 | -- |
| task-3 | Fix game-end title for local play | 1 | -- |
| task-5 | Rewrite modules/application/CLAUDE.md | 1 | -- |
| task-2 | Fix rematch hardcoding RulesetOptions.STANDARD | 2 | task-1 |
| task-4 | Add SceneManagerTest | 3 | task-2 |

---

## Parallel Group 1 (run concurrently)

### task-1: Fix showErrorAndReturnToMenu ignoring user response

**Touches:** `GameController.java`

In `GameController.java`, method `showErrorAndReturnToMenu` (line 374-378): capture the boolean return value of `sceneManager.showConfirm(message)` and only call `chess.endGame()` + `sceneManager.showMainMenu()` when `confirmed` is true. Remove the unused `title` parameter from the method signature. Update the single call site at line 205-206 to pass only `i18n.get("error.server")`.

### task-3: Fix game-end title for local play

**Touches:** `GameEndController.java`, `messages_en.properties`, `messages_de.properties`

In `GameEndController.java` initialize() (lines 70-77), when `isWin` is true, branch on `isOnline`: if online, keep existing text; if local, derive the winner name from the `state` enum name (`WHITE_WON` prefix means white won) and use `MessageFormat.format(i18n.get("game.end.title.win.local"), winnerName)`.

Add new i18n key to both properties files:
- EN: `game.end.title.win.local={0} wins!`
- DE: `game.end.title.win.local={0} gewinnt!`

### task-5: Rewrite modules/application/CLAUDE.md

**Touches:** `modules/application/CLAUDE.md`

Complete rewrite from Swing documentation to JavaFX. Cover: Status, Responsibility, Package Structure, Key Classes, Design Patterns, Architecture Law Compliance, Known Debt. Remove all Swing references.

---

## Parallel Group 2 (after group 1)

### task-2: Fix rematch hardcoding RulesetOptions.STANDARD

**Touches:** `GameController.java`, `SceneManager.java`, `MainMenuController.java`
**Depends on:** task-1 (overlapping file: GameController.java)

Thread the original ruleset through the constructor chain:

1. `SceneManager.showGame()` gains a `RulesetOptions` parameter, passes it to GameController constructor.
2. `GameController` stores `RulesetOptions` as a field, uses it in `showGameEndDialog` instead of `RulesetOptions.STANDARD`. Also uses the already-computed `isOnline` variable instead of hardcoded `false`.
3. `MainMenuController.onLocalGame()` and `onOnlineGame()` pass `setup.getRuleset()` to `sceneManager.showGame(ruleset)`.

---

## Parallel Group 3 (after group 2)

### task-4: Add SceneManagerTest

**Touches:** `SceneManagerTest.java` (new file)
**Depends on:** task-2 (SceneManager signature changes)

Create `modules/application/src/test/java/io/github/conava/chess/application/navigation/SceneManagerTest.java` with 4-5 tests:

1. `accessors_returnInjectedDependencies` -- no JavaFX toolkit needed
2. `showConfirm_beforeSceneShown_throwsIllegalStateException`
3. `dismissOverlay_beforeSceneShown_throwsIllegalStateException`
4. `showMainMenu_setsStageProperties` (requires FX toolkit)
5. `showGame_maximizesStage` (requires FX toolkit)

Use JUnit 5 + Mockito. Note: `showGame()` now requires a `RulesetOptions` parameter.

---

## Dependency Graph

```
task-1 (GameController bug 1) ----\
                                    +--> task-2 (rematch fix) --> task-4 (SceneManager test)
task-3 (game-end title)       ----/  (no dependency, just same group boundary)
task-5 (CLAUDE.md rewrite)    ----/
```

Tasks 1, 3, 5 have zero file overlap and run in parallel.
Task 2 must wait for task 1 (both touch GameController.java).
Task 4 must wait for task 2 (tests the new SceneManager.showGame(RulesetOptions) signature).

---

## Complexity Estimate

**Small.** Five focused, well-scoped changes. Three are single-method fixes, one is a new test file, one is a documentation rewrite. No architectural changes, no new dependencies, no core module modifications.

---

## Concerns

None. All five bugs have clear, self-contained fixes as described in the design document. The only coordination point is the file overlap between task-1 and task-2 on GameController.java, handled by sequencing them.
