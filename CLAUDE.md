# CLAUDE.md

## Project Overview
Modularized chess game. Java 17, Maven multi-module. Migrating UI from Swing to JavaFX.
All packages under `io.github.conava.chess`.

## Build & Run Commands

### Build Commands

**Build Everything**
Clean and install all modules (Core, Application, Server).
```bash
mvn clean install
```

**Build Only the Application (GUI)**
Builds the `application` module and its required dependencies (like `core`).
```bash
mvn clean package -pl modules/application -am
```

**Build Only the Server**
Builds the `server` module and its required dependencies.
```bash
mvn clean package -pl modules/server -am
```

*Note: The `-am` flag ("also make") ensures that changes in `core` are compiled and included even when you are only targeting a sub-module.*

---

### Test Commands

**Run All Tests**
```bash
mvn test
```

**Run Tests for a Specific Module**
```bash
mvn test -pl modules/core
mvn test -pl modules/application
mvn test -pl modules/server
```

**Run a Specific Test Class**
```bash
# Example: Run only BoardTest in the core module
mvn -pl modules/core -Dtest=BoardTest test
```

---

### Run Commands

After building, the executable JAR files are located in the `target` directory of their respective modules.

**Run the GUI Application**
```bash
java -jar modules/application/target/application-0.9.jar
```

**Run the Application (No-GUI Mode)**
Useful for testing API or headless operations.
```bash
java -jar modules/application/target/application-0.9.jar nogui
```

**Run the Server**
```bash
java -jar modules/server/target/server-0.9.jar
```

## Architecture Laws (Non-Negotiable)
These apply to ALL tasks. Never violate without explicit human approval.

1. **Module boundaries are hard.** `core` has zero dependencies on `application` or `server`.
   `application` and `server` depend on `core` only — never on each other.
2. **The `Chess` façade is the only API surface.** Application and server code must go
   through `Chess.java`. Direct instantiation of `Game` subclasses from outside `core` is banned.
3. **Observer pattern for all state propagation.** UI components must implement `GameObserver`
   and register via `chess.addObserver()`. Polling game state in loops is banned.
4. **`core` is logic-only.** No UI imports, no JavaFX, no Swing, no I/O in `core`.
5. **`Strategy pattern` owns ruleset variation.** New rule variants must implement `Ruleset`,
   not branch inside `Game` or `Chess`.

## Design Patterns in Use
- Observer: `GameObserver` / `Observable` — all state change notifications
- Strategy: `Ruleset` interface — rule variants
- Façade: `Chess.java` — single entry point for all game interaction

## Current Migration Context
- Replacing `modules/application/` Swing UI with JavaFX
- `core` and `server` must not change during UI migration
- JavaFX target: 21. Use FXML + Controller pattern. Entry point extends `Application`.
- All JavaFX UI updates via `Platform.runLater()`. No direct UI mutation from observer callbacks.

## Conventions
- No test code in `src/main`. No production logic in `src/test`.
- Every new public class in `core` needs a unit test.
- CLAUDE.md files are living documents — update them when architecture decisions are made.

## Branch Strategy
All work happens in feature branches. No agent ever commits to `main` directly.

### Naming Pattern
`<type>/<short-slug>`
Types: `feat`, `refactor`, `fix`, `test`, `docs`, `chore`

### Rules
- Architect creates the branch at the start of every task
- All agents (executor, test-writer, docs-keeper, reviewer) use that same branch
- Human reviews the branch and merges manually when satisfied
- Branch is deleted after merge

## Context7 MCP — Correct Usage
All agents that need to verify library APIs MUST use Context7 with two steps:
1. Call `mcp__context7__resolve-library-id` with `libraryName` and `query` to get the library ID.
2. Call `mcp__context7__query-docs` with the returned `libraryId` and your `query`.
Never guess API signatures. Never call `query-docs` without first resolving the library ID.

---

## Development Pipeline

The root agent (you, Claude) drives the full pipeline directly.
You NEVER implement, test, document, or review code yourself — you delegate
to specialized agents via the Task tool.

### Starting a Task — Full Pipeline
When the human describes a task, follow this pipeline automatically:

#### Phase 1 — Planning
1. Invoke `architect` with the human's task description.
2. Read the plan file the architect produces.
3. Present to the human: the plan summary, Open Questions, and the
   "Proactive Improvements" table.
4. **HUMAN GATE — Wait for answers + approval.** Do not proceed without it.
5. If Open Questions were answered: invoke `architect` again to update the plan
   with the human's answers. Present the updated task list for final approval.

#### Phase 2 — Execution (auto after approval)
Read the plan's "Ordered Implementation Tasks" and "Affected Files" table.

**Parallelism rules:**
- Tasks with NONE cascade risk and NO overlapping files → run in parallel.
- Tasks with DEPENDENT or CROSS-MODULE risk, or overlapping files → run
  sequentially in plan order.
- When in doubt, run sequentially. Wrong parallelism is worse than slow.

For each task (or parallel batch):
- Invoke `executor` with: "Execute Task [N] from [plan path]. Do not execute any other task."
- Wait for the executor's summary.
- If executor escalates: STOP, present the escalation to the human, wait for decision.
- If executor completes with warnings: note them, continue to next task.
- Proceed to Phase 3 only after ALL tasks report success.

#### Phase 3 — Quality (auto, parallel)
Invoke `test-writer` and `docs-keeper` in parallel:
- test-writer: "Implement the Testing Requirements section of [plan path] on branch [branch]."
- docs-keeper: "Implement the Documentation & Javadoc Requirements section of [plan path] on branch [branch]."

Wait for both. If test-writer reports implementation bugs:
- Invoke `executor` to fix each bug (one per invocation).
- Re-invoke `test-writer` after fixes.

If test-writer reports logic errors or executor can't fix it the first time:
- Invoke `architect` to assess the bug and contine the pipeline from Phase 1 again.

#### Phase 4 — Review (auto)
Invoke `reviewer`: "Review branch [branch] against main."

Present to the human:
- The verdict (APPROVED / NEEDS WORK / BLOCKED)
- The review file path
- If NEEDS WORK: every issue grouped by responsible agent

If NEEDS WORK and human approves auto-fix: route each issue to the correct
agent, then re-invoke reviewer. If BLOCKED: present to human and stop.

### Manual Agent Invocation
The human can also ask to run any single agent directly:
```
Use the architect agent to [plan a task]
Use the executor agent to execute Task [N] from [plan path]
Use the test-writer agent to implement tests from [plan path]
Use the docs-keeper agent to update docs from [plan path]
Use the reviewer agent to review branch [branch] against main
```
When the human invokes a single agent, run ONLY that agent — do not
continue the pipeline unless the human asks.

### Skipping Planning
If the human provides an existing plan file and says to skip planning,
go directly to Phase 2. Read the plan, check `git log` for completed
tasks, and start from the first incomplete task.

### Pipeline Rules for the Root Agent
- You NEVER write code, tests, docs, or Javadoc yourself.
- You NEVER modify source files directly.
- You ONLY use the Task tool to delegate to agents.
- You ONLY read files to relay information or check agent results.
- If an agent invocation fails, report the error and suggest the human
  invoke that agent manually as a workaround.
