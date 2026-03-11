# Review — server-overhaul
Generated: 2026-03-10
Base commit: 07617ca7f9cd7aa1f7559b2972d6a99859ef392f

## Batch Reviews

### Group 1: T01, T02, T03, T10

**Result: PASS**

Reviewed commit range: `07617ca...4de2378`
All tests pass (core: 0 failures, server: 0 failures).

---

#### Findings

**F-G1-01** [OPEN]
- **Severity**: Important
- **Confidence**: 0.95
- **Scope**: in-scope
- **Location**: `modules/server/src/main/java/io/github/conava/chess/server/management/PlayerSession.java`
- **Description**: T10 plan specifies `PlayerSession` as a Java `record` with exactly three components `(int userId, String username, String token)` and no additional methods. The implementation delivers a mutable class with four fields (`userId`, `username`, `authToken`, `activeGameId`), full getters/setters, `isAuthenticated()`, and `clear()`. This is a deliberate deviation from the plan spec. The field name `token` becomes `authToken`, and an entirely new field `activeGameId` is added (which belongs to a later task, T12, where `ClientHandler` is refactored). While the richer design is arguably correct for the final goal of T12, it constitutes scope creep relative to T10's stated requirements and acceptance criteria. The plan's acceptance criteria are: "PlayerSession record exists with `userId`, `username`, `token` components." None of those are met as specified.
- **Suggested fix**: Either update the plan to document this intentional deviation and confirm the `activeGameId` field and mutability were approved, or revert to the record form specified in the plan and add the extra fields in T12 when `ClientHandler` is actually wired to use them.

---

#### Non-findings (verified correct)

- **T01**: All 11 new `MessageType` values are present and appended after `FAILURE`, preserving ordinal positions 0-7 for existing values. Total count is 19. Tests cover all new values plus round-trip serialization. `existingValuesUnchanged` correctly uses `assertSame` on ordinal positions.
- **T02**: `PAUSED` and `SAVED` are added as the 15th and 16th enum values with correct message strings. The trailing semicolon placement is valid Java. Tests verify both values and the total count of 16.
- **T03**: `org.xerial:sqlite-jdbc:3.45.3.0` is added to `modules/server/pom.xml` only. Root, core, and application POMs are untouched. No `<scope>` tag means compile scope (the default), which is correct; the shade plugin will include the JAR in the fat JAR automatically.
- **T10 (module boundary)**: `PlayerSession.java` has no imports from `io.github.conava.chess.core.*`, satisfying Architecture Law 1.
- **Commit format**: Commit message `feat(core,server): Group 1 — protocol enums, persistence dep, session class` follows the project's conventional-commits style.

---

## Stage 4: Deep Review
