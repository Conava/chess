# CLAUDE.md

## Project Overview
Modularized chess game. Java 17, Maven multi-module. JavaFX 21 UI.
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

**Run the GUI Application (development)**
Uses the JavaFX Maven plugin — recommended during development.
```bash
mvn javafx:run -pl modules/application -am
```

**Run the GUI Application (fat JAR)**
Requires JavaFX SDK 21 on the module path.
```bash
java --module-path /path/to/javafx-sdk-21/lib \
     --add-modules javafx.controls,javafx.fxml \
     -jar modules/application/target/application-0.9.jar
```

**Run the Application (No-GUI Mode)**
Useful for testing API or headless operations.
```bash
mvn javafx:run -pl modules/application -am -Djavafx.args=nogui
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
- Strategy: `Ruleset` interface — rule variants (`StandardChessRuleset`, `Chess960Ruleset`)
- Template Method: `AbstractChessRuleset` — shared chess logic with hooks for castling and king moves
- Façade: `Chess.java` — single entry point for all game interaction

## Current State
- Swing-to-JavaFX migration is complete. The `application` module uses JavaFX 21 exclusively.
- JavaFX target: 21. FXML + Controller pattern. Entry point extends `Application`.
- All JavaFX UI updates via `Platform.runLater()`. No direct UI mutation from observer callbacks.
- **Chess960 (Fischer Random Chess) is implemented** as a second ruleset variant.
  - `AbstractChessRuleset` is the shared base class; both `StandardChessRuleset` and
    `Chess960Ruleset` extend it.
  - `Chess960Ruleset` owns position generation (`Chess960StartPosition`), Chess960-specific
    castling logic (`PossibleChess960KingMoves`), and move deserialization for castling moves.
  - `CastleMove` carries optional `rookOriginFile`/`kingDestFile` fields for Chess960 castling.
  - `Board.handleCastleMove` branches on these fields to handle both standard and Chess960 castling.
  - The `Ruleset` interface has default methods: `getGameLabel()`, `deserializeMove()`,
    `isCastlingMove()`, `buildCastleMove()`. Chess960 overrides what it needs.
  - `RulesetOptions` has a `displayName` field; `toString()` returns it (e.g., `"Chess 960"`).
  - Online Chess960: the server generates the position, sends the Scharnagl index (0-959) to
    both clients via `JOIN_CODE` and `SUCCESS` messages. `OnlineGame` uses deferred board
    initialization (`deferBoardInit=true`) and calls `initializeBoard(Ruleset)` once the server
    provides the position. This pattern is generic and works for any future ruleset needing
    server-provided parameters.
  - `Game.getRuleset()` accessor exposes the active ruleset.
  - `Chess.getGameLabel()` facade method delegates to `game.getRuleset().getGameLabel()`.
  - `GameController` displays a position label (e.g., "Chess 960 — Position 518") when present.
- `StandardChessRuleset` enforces check legality via deep-copy simulation (see ADR 0006).
- En passant, castling, checkmate, stalemate, 50-move rule, threefold repetition, and
  insufficient material detection are all implemented in `core`.
- `Piece.copy()` and `Board.getCopy()` provide deep-copy support used by the check-legality filter.
- Known performance debt: `hasAnyLegalMove` is O(moves x pieces) per turn due to deep-copy
  simulation. Chess960 amplifies this for castling candidates (up to 6 deep copies per candidate).

## Conventions
- No test code in `src/main`. No production logic in `src/test`.
- Every new public class in `core` needs a unit test.
- CLAUDE.md files are living documents — update them when architecture decisions are made.
- Javadoc is owned by the executor agent. Docs-keeper never writes or modifies Javadoc.

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

## Project Documentation (`docs/`)

All project documentation beyond code-level Javadoc lives in the `docs/` folder
at the repo root. The root README.md links into each section. Docs-keeper owns
this folder and keeps it accurate.

### Folder Structure
```
docs/
├── architecture/
│   ├── overview.md              # High-level system architecture, module diagram
│   ├── module-boundaries.md     # Dependency rules, what goes where, what is banned
│   └── design-patterns.md       # Observer, Strategy, Façade — how they work here
├── decisions/                   # Architecture Decision Records (MADR format)
│   ├── NNNN-short-title.md      # One file per decision, numbered sequentially
│   └── ...
├── api/
│   └── chess-facade.md          # Public API reference for the Chess façade
├── guides/
│   ├── getting-started.md       # Prerequisites, build, run, first steps
│   ├── contributing.md          # Conventions, workflow, how to submit changes
│   └── adding-a-ruleset.md     # How to extend via the Strategy pattern
├── deferred-findings.md           # Unresolved review findings tracked for future work
└── migration/
    └── swing-to-javafx.md       # Current migration status, what's done, what's left
```

### What goes where
- **architecture/**: Explains HOW the system is built. For developers and AI agents
  who need to understand the structure before making changes.
- **decisions/**: Explains WHY choices were made. ADRs capture design decisions,
  technology choices, and trade-offs. Uses MADR format (see below).
- **api/**: Reference documentation for public interfaces. What methods exist,
  what they accept, what they return, what can go wrong.
- **guides/**: Step-by-step instructions for common tasks. For new developers,
  users, and AI agents who need to DO something.
- **migration/**: Temporary section for the Swing→JavaFX migration. Remove
  when migration is complete.

### ADR Format (MADR-based)
Each ADR is a file in `docs/decisions/` named `NNNN-short-title.md`:
```markdown
# NNNN: Short Title

Status: proposed | accepted | deprecated | superseded by NNNN
Date: YYYY-MM-DD
Deciders: [who was involved]

## Context and Problem Statement
What is the issue? What forces are at play?

## Considered Options
1. Option A
2. Option B
3. Option C

## Decision Outcome
Chosen option: "Option B", because [justification].

### Consequences
- Good: [positive outcome]
- Bad: [negative outcome or trade-off]
- Neutral: [side effect]

## Pros and Cons of the Options

### Option A
- Good: ...
- Bad: ...

### Option B
- Good: ...
- Bad: ...
```

ADR statuses:
- **proposed**: Under discussion, not yet decided.
- **accepted**: Decision is active and applies.
- **deprecated**: Decision is no longer relevant (e.g., feature removed).
- **superseded by NNNN**: Replaced by a newer decision. Link to replacement.

ADRs are append-only: never delete or rewrite an accepted ADR. To change a
decision, create a new ADR that supersedes the old one, and update the old
ADR's status to "superseded by NNNN".

### Documentation Principles
- Write for three audiences: new developers, end users, AI agents.
- Present tense only. Describe what IS, not what WAS.
- Keep each document focused on one topic. Link instead of duplicating.
- Every document must be reachable from the root README.md.
- Only create documents that are relevant to this project. Not every template
  section needs to exist — create it when there's content worth documenting.

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
- docs-keeper: "Implement the Documentation Updates & ADR section of [plan path] on branch [branch]. Ensure everything in /docs is accurate and up-to-date."

Wait for both. If test-writer reports implementation bugs:
- Invoke `executor` to fix each bug (one per invocation).
- Re-invoke `test-writer` after fixes.

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
