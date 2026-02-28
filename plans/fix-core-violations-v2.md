# Implementation Plan: fix/core-violations v2

**Date:** 2026-02-28
**Branch:** `fix/core-violations`
**Scope:** Address 14 reviewer remarks plus known debt items from module CLAUDE.md files.

---

## Decision Table

| # | Remark Summary | Decision | Rationale |
|---|---|---|---|
| 1 | `ServerCommunicationTask` — unbounded `processedMessages` HashSet | **ACCEPT** | The `HashSet<String>` grows without bound for the lifetime of a connection. In a long game with many messages, this is a memory leak. However, the deduplication logic itself is suspect — the server protocol does not define message IDs, and raw string dedup means legitimate repeated messages (e.g., two identical GAME_STATUS responses) get silently dropped. The fix is to remove deduplication entirely; the protocol uses TCP which is ordered and reliable, and `OnlineGame.handleMessage()` is idempotent for all message types. |
| 2 | `Chess.java` — no `isConnected()` check after `connectionLatch.await()` | **ACCEPT** | If the socket connect fails, the catch block in `ServerCommunicationTask.run()` counts down the latch but leaves `connected = false`. `createOnlineGame()` then proceeds to construct an `OnlineGame` that will silently fail on every `sendMessage` (the `out` field is null). The fix is to check `task.isConnected()` after `await()` and set the game to `SERVER_ERROR` state. |
| 3 | `ChessGame.java` — `onGameStateChanged()` calls `update()` without EDT dispatch | **ACCEPT** | The `onGameStateChanged()` callback calls `update()` directly. When triggered from the network thread (via `ServerCommunicationTask` → `OnlineGame.handleMessage` → `notifyObservers()`), this mutates Swing UI components off the EDT. Wrap in `SwingUtilities.invokeLater`. |
| 4 | `OnlineGame.java` — `handleMove()` only catches `IllegalMoveException` | **ACCEPT** | `Move.fromString()` can throw `IllegalArgumentException` (bad enum value), `StringIndexOutOfBoundsException` (malformed string), or `NullPointerException`. Any unchecked exception on the network handler thread will terminate message processing and desync the UI. Catch `RuntimeException` as a protocol error. |
| 5 | `Game.java` — `getNewPiece()` returns null for KING/PAWN | **ACCEPT** | If `Pieces.KING` or `Pieces.PAWN` is passed to `promoteMove()`, `getNewPiece()` returns null. The null flows into `PromotionMove`, and `Board.executeMove()` sets the destination square's piece to null, effectively deleting the pawn. Throw `IllegalArgumentException` for non-promotable values. |
| 6 | `ServerCommunicationTask.java` — socket/stream resources not closed on normal termination | **ACCEPT** | When the `while` loop exits normally (server closes connection, `running` set to false), the socket and streams are not closed. Closing depends entirely on callers calling `closeConnection()`. Add a `finally` block to `run()` that closes resources and sets `connected = false`. |
| 7 | `ServerCommunicationTask.java` — messages dropped before handler is set but tracked as processed | **ACCEPT** | Between socket connect and `setMessageHandler()` being called, messages can arrive. With deduplication removed (remark 1), the "tracked as processed" part goes away, but messages are still dropped if `messageHandler == null`. Resolved together with remark 8 by requiring the handler in the constructor. |
| 8 | `Chess.java` — race: `OnlineGame` sends messages in constructor before handler is registered | **ACCEPT** | `OnlineGame` constructor calls `connectToServerGame()` which sends `CREATE_GAME`/`JOIN_GAME`. The server may respond (e.g., with `JOIN_CODE`) before the handler is registered. Fix: require `messageHandler` in `ServerCommunicationTask` constructor, and defer `connectToServerGame()` from `OnlineGame`'s constructor to an explicit call from the facade after handler registration. |
| 9 | `Move.java` — `fromString()`/`toString()` protocol mismatch | **ACCEPT** | `toString()` emits algebraic notation (`"e4"`, `"Nf3"`, `"a7-a8=Q"`) but `fromString()` expects `"a7=a8=QUEEN"` format. Introduce `toProtocolString()` for unambiguous wire serialization and update `fromString()` to parse that format. Keep `toString()` for display. |
| 10 | `GameInstance.java` — `handleMove()` only catches `IllegalMoveException` | **ACCEPT** | Same root cause as remark 4 on the server side. Unchecked exceptions from `Move.fromString()` will crash the client handler thread. Catch `RuntimeException` and log; send error response when possible. |
| 11 | `Observable.java` — `ArrayList` is not thread-safe | **ACCEPT** | `notifyObservers()` can run on background/network threads while `addObserver`/`removeObserver` run on the EDT → `ConcurrentModificationException`. Replace with `CopyOnWriteArrayList`. |
| 12 | `Observable.java` — `addObserver()` Javadoc says null forbidden but no enforcement | **ACCEPT** | Add `Objects.requireNonNull(observer)` in `addObserver()` and `removeObserver()`. |
| 13 | `Move.java` — promotion switch returns null for non-promotable enum values | **ACCEPT** | Same root cause as remark 5 but in `Move.fromString()`. Throw `IllegalArgumentException` for KING/PAWN so malformed/hostile move strings cannot corrupt board state. |
| 14 | Known debt from module CLAUDE.md files | **PARTIALLY ACCEPT** | See sub-items below. |

### Remark 14 Sub-Items

| Debt Item | Decision | Rationale |
|---|---|---|
| `Chess.java:353` unsafe cast in `getJoinCode()` | **ACCEPT** | Add `instanceof` check; return null for offline games. |
| `Chess.java:235` `endGame()` not closing connection | **ACCEPT** | Call `game.endGame()` before nulling the reference so `OnlineGame.endGame()` closes the connection. |
| `OnlineGame.java:147` `handleGameStatus` missing `notifyObservers()` | **ACCEPT** | State changes from GAME_STATUS messages are not propagated to the UI. Add `notifyObservers()` call. |
| `server/CLAUDE.md` debt item 8: stale reflection reference | **REJECT** | Inspecting `GameInstance.handleMove()`, the catch block only catches `IllegalMoveException` — reflection was already removed. The debt entry is stale. Update the CLAUDE.md to remove it; no code change needed. |

---

## Architecture Law Compliance

All proposed changes comply with the five architecture laws:

1. **Module boundaries** — No cross-module dependencies introduced. All `core` changes stay in `core`; `application` changes stay in `application`; `server` changes stay in `server`.
2. **Chess facade only API** — `OnlineGame` construction order changes stay inside `Chess.java`. No new direct subclass instantiation outside the facade.
3. **Observer pattern** — Remark 3 ensures observers fire on EDT. Remark 11 makes observer list thread-safe. Remark 14c adds missing `notifyObservers()`.
4. **core is logic-only** — No UI, I/O, Swing, or JavaFX imports added to `core`. `CopyOnWriteArrayList` is a pure JDK collection.
5. **Strategy pattern** — No ruleset branching introduced.

---

## Human-Confirmed Decisions (2026-02-28)

1. **Move display format:** Keep `toString()` for algebraic display. Use `toProtocolString()` for wire only.
2. **OnlineGame construction:** Use a **static factory method** (`OnlineGame.create()`). Private constructor.
3. **Deduplication:** **Remove entirely.** TCP guarantees ordering; no message IDs in protocol.
4. **Server relay:** **Include in this branch.**
5. **Invalid promotion:** **Throw `IllegalArgumentException`** — do not silently drop the pawn. Add proper error handling at call sites.

---

## Ordered Implementation Tasks (EXECUTOR ONLY)

Production code changes only. Each task leaves the project compilable after completion.

---

### Task 1 ✅ (Priority: CRITICAL) — `Observable` thread-safety + null-safety; `getNewPiece` and promotion switch fail-fast

**Remarks addressed:** 5, 11, 12, 13

**Files modified:**
- `modules/core/src/main/java/io/github/conava/chess/core/logic/observer/Observable.java`
- `modules/core/src/main/java/io/github/conava/chess/core/logic/game/Game.java`
- `modules/core/src/main/java/io/github/conava/chess/core/logic/moves/Move.java`

**Changes:**

1. **`Observable.java`**:
   - Replace `new ArrayList<>()` with `new CopyOnWriteArrayList<>()`.
   - Add imports: `java.util.concurrent.CopyOnWriteArrayList`, `java.util.Objects`.
   - In `addObserver()`: add `Objects.requireNonNull(observer, "observer must not be null")` as first line.
   - In `removeObserver()`: add `Objects.requireNonNull(observer, "observer must not be null")` as first line.

2. **`Game.java` `getNewPiece()`**:
   - Replace `default -> null` with `default -> throw new IllegalArgumentException("Cannot promote to " + targetPiece)`.

3. **`Move.java` promotion switch in `fromString()`**:
   - Replace `default -> null` with `default -> throw new IllegalArgumentException("Cannot promote to " + targetPiece)`.

**Acceptance criteria:**
- `Observable` uses `CopyOnWriteArrayList` internally.
- `addObserver(null)` and `removeObserver(null)` throw `NullPointerException`.
- `Game.getNewPiece(Pieces.KING)` and `Game.getNewPiece(Pieces.PAWN)` throw `IllegalArgumentException`.
- `Move.fromString()` with `KING` or `PAWN` as promotion target throws `IllegalArgumentException`.
- `mvn compile -pl modules/core` succeeds.

**Required Javadoc:**
- `Observable.addObserver(Observer)` — update to document `NullPointerException`.
- `Observable.removeObserver(Observer)` — update to document `NullPointerException`.
- `Game.getNewPiece(Pieces)` — update to document `IllegalArgumentException` for non-promotable pieces.

**Verify:** `mvn compile -pl modules/core`

---

### Task 2 ✅ (Priority: HIGH) — `Move` protocol format + `OnlineGame.handleGameStatus` observer

**Remarks addressed:** 9, 14c

**Files modified:**
- `modules/core/src/main/java/io/github/conava/chess/core/logic/moves/Move.java`
- `modules/core/src/main/java/io/github/conava/chess/core/logic/game/OnlineGame.java`

**Changes:**

1. **`Move.java`**: Add `public String toProtocolString()` method:
   - Castling: `"O-O"` / `"O-O-O"` (same as current toString castling output).
   - Regular: `"<startFile><startRank>-<endFile><endRank>"` e.g. `"e2-e4"`.
   - Promotion: `"<start>-<end>=<PIECES_ENUM_NAME>"` e.g. `"a7-a8=QUEEN"`.

2. **`Move.java` `fromString()`**: Update to parse `toProtocolString()` output:
   - For regular moves: already uses `substring(0,2)` and `substring(3,5)` — correct for `"xx-xx"` format.
   - For promotions: split on `"="` with limit 2. Left side is `"xx-xx"`, right side is `"QUEEN"` etc. Split left on `"-"` for start/end squares.
   - Castling strings unchanged.

3. **`OnlineGame.java` `sendMoveToServer()`**: Change `move + " "` (calls `toString()`) to `move.toProtocolString() + " "`.

4. **`OnlineGame.java` `handleGameStatus()`**: Add `notifyObservers()` as last line.

**Acceptance criteria:**
- `Move.fromString(move.toProtocolString(), player)` round-trips for regular, promotion, and castling moves.
- `toProtocolString()` output matches the documented wire format.
- `sendMoveToServer()` uses `toProtocolString()`, not `toString()`.
- `handleGameStatus()` calls `notifyObservers()`.
- `mvn compile -pl modules/core` succeeds.

**Required Javadoc:**
- `Move.toProtocolString()` — new method, document wire format contract.
- `Move.fromString(String, Player)` — update to document expected input format (protocol format).

**Verify:** `mvn compile -pl modules/core`

---

### Task 3 (Priority: HIGH) — `OnlineGame.handleMove()` robust exception handling

**Remarks addressed:** 4

**Files modified:**
- `modules/core/src/main/java/io/github/conava/chess/core/logic/game/OnlineGame.java`

**Changes:**

Widen the catch in `handleMove()` to also catch `RuntimeException`:

```java
private void handleMove(Message message) {
    if (Objects.equals(message.getParameterValue(PLAYER_COLOR_PARAM), localPlayerColor.toString())) {
        return;
    }
    try {
        Move move = Move.fromString(
            Objects.requireNonNull(message.getParameterValue(MOVE_PARAM)),
            Objects.equals(message.getParameterValue(PLAYER_COLOR_PARAM), "WHITE") ? player0 : player1);
        executeMoveFromRemote(move);
    } catch (IllegalMoveException e) {
        LOGGER.log(Level.SEVERE, "Illegal move received: " + message.content(), e);
    } catch (RuntimeException e) {
        LOGGER.log(Level.SEVERE, "Failed to parse move from server: " + message.content(), e);
    }
}
```

**Acceptance criteria:**
- Malformed move strings (e.g., `""`, `"ZZ-99"`, `null` MOVE_PARAM) do not crash the message handler thread.
- `IllegalMoveException` and `RuntimeException` are both logged at SEVERE level.
- `mvn compile -pl modules/core` succeeds.

**Required Javadoc:** None (private method).

**Verify:** `mvn compile -pl modules/core`

---

### Task 4 (Priority: HIGH) — `ServerCommunicationTask` resource management + handler race fix + `OnlineGame` static factory

**Remarks addressed:** 1, 6, 7, 8

**Files modified:**
- `modules/application/src/main/java/io/github/conava/chess/application/network/ServerCommunicationTask.java`
- `modules/application/src/main/java/io/github/conava/chess/application/Chess.java`
- `modules/core/src/main/java/io/github/conava/chess/core/logic/game/OnlineGame.java`

**Changes:**

1. **`ServerCommunicationTask.java`**:
   - Remove `processedMessages` field entirely (remark 1). Remove all deduplication logic — TCP guarantees ordering and the protocol defines no message IDs.
   - Change constructor to require `Consumer<Message> messageHandler` parameter. Remove `setMessageHandler()` method. Mark field `final`.
   - Wrap `run()` body in try-finally: the `finally` block sets `connected = false` and closes `socket`, `in`, `out` safely (remark 6).
   - Promote `BufferedReader in` to a field so it can be closed in `finally`.

2. **`OnlineGame.java`** — Convert to static factory method pattern (Human Decision 2):
   - Make the constructor **private**.
   - Add `public static OnlineGame create(...)` static factory method that:
     1. Constructs the instance via the private constructor (without calling `connectToServerGame()`).
     2. Returns the fully constructed but not-yet-connected instance.
   - Remove the `connectToServerGame()` call from the constructor body.
   - Make `connectToServerGame()` a **public** method callable by the facade after handler registration.

3. **`Chess.java` `createOnlineGame()`**:
   - Create `OnlineGame` via `OnlineGame.create(...)` (not `new OnlineGame(...)`).
   - Pass `onlineGame::handleMessage` to `ServerCommunicationTask` constructor.
   - After `connectionLatch.await()`, check `task.isConnected()` (remark 2). If not connected, call `onlineGame.setGameState(GameState.SERVER_ERROR)` and return early without calling `connectToServerGame()`.
   - Call `onlineGame.connectToServerGame()` only after confirming connection.

**Acceptance criteria:**
- `ServerCommunicationTask` has no `processedMessages` field or dedup logic.
- `messageHandler` is `final` and set via constructor; no `setMessageHandler()` method exists.
- `run()` uses try-finally to close socket/streams on all exit paths.
- `OnlineGame` constructor is private; only `OnlineGame.create(...)` is available.
- `OnlineGame.create(...)` does not send any messages (no `connectToServerGame()` call in construction).
- `Chess.createOnlineGame()` checks `isConnected()` and sets `SERVER_ERROR` on failure.
- `mvn compile -pl modules/core,modules/application` succeeds.

**Required Javadoc:**
- `OnlineGame.create(...)` — new static factory method, document parameters and two-phase construction contract.
- `OnlineGame.connectToServerGame()` — update visibility, document that it must be called after handler registration.
- `ServerCommunicationTask` constructor — update to document `messageHandler` parameter.

**Verify:** `mvn compile -pl modules/core,modules/application`

---

### Task 5 (Priority: MEDIUM) — `Chess.java` facade: `getJoinCode`, `endGame`, `isConnected` check

**Remarks addressed:** 2, 14a, 14b

**Files modified:**
- `modules/application/src/main/java/io/github/conava/chess/application/Chess.java`

**Changes:**

1. **`getJoinCode()`**: Replace unsafe cast with pattern match:
   ```java
   public String getJoinCode() {
       if (game instanceof OnlineGame onlineGame) {
           return onlineGame.getJoinCode();
       }
       return null;
   }
   ```

2. **`endGame()`**: Guard null and call `game.endGame()` before nulling:
   ```java
   public void endGame() {
       if (game != null) {
           game.endGame();
           game = null;
       }
   }
   ```

(The `isConnected()` fail-fast check is part of Task 4.)

**Acceptance criteria:**
- `getJoinCode()` returns `null` for offline games without throwing `ClassCastException`.
- `endGame()` calls `game.endGame()` before setting `game = null`.
- `endGame()` is safe to call when `game` is already `null`.
- `mvn compile -pl modules/application` succeeds.

**Required Javadoc:**
- `Chess.getJoinCode()` — update to document null return for offline games.
- `Chess.endGame()` — update to document null-safety and connection cleanup.

**Verify:** `mvn compile -pl modules/application`

---

### Task 6 (Priority: MEDIUM) — `ChessGame.java` EDT dispatch for observer callback

**Remarks addressed:** 3

**Files modified:**
- `modules/application/src/main/java/io/github/conava/chess/application/window/ChessGame.java`

**Changes:**

```java
@Override
public void onGameStateChanged() {
    SwingUtilities.invokeLater(this::update);
}
```

**Acceptance criteria:**
- `onGameStateChanged()` dispatches `update()` via `SwingUtilities.invokeLater`, never calling it directly.
- `mvn compile -pl modules/application` succeeds.

**Required Javadoc:** None (override of existing documented interface method).

**Verify:** `mvn compile -pl modules/application`

---

### Task 7 (Priority: MEDIUM) — `GameInstance.java` robust exception handling

**Remarks addressed:** 10

**Files modified:**
- `modules/server/src/main/java/io/github/conava/chess/server/management/GameInstance.java`

**Changes:**

Add `catch (RuntimeException e)` to `handleMove()`:
```java
} catch (RuntimeException e) {
    LOGGER.log(Level.SEVERE, "Failed to parse move: " + message.content(), e);
    // TODO: send ERROR response to sender when server protocol supports it
}
```

**Acceptance criteria:**
- Malformed move strings do not crash the client handler thread.
- `RuntimeException` from `Move.fromString()` is logged at SEVERE level.
- `mvn compile -pl modules/server` succeeds.

**Required Javadoc:** None (private method).

**Verify:** `mvn compile -pl modules/server`

---

## Testing Requirements (TEST-WRITER ONLY)

List of every behavior that must be covered by tests. Organized by source task.

### From Task 1 — `Observable`, `getNewPiece`, promotion switch

| Behavior | Suggested Test Method | Asserts |
|---|---|---|
| `addObserver(null)` throws NPE | `ObservableTest.addObserver_nullThrowsNPE` | `assertThrows(NullPointerException.class, ...)` |
| `removeObserver(null)` throws NPE | `ObservableTest.removeObserver_nullThrowsNPE` | `assertThrows(NullPointerException.class, ...)` |
| Concurrent add during iteration does not throw | `ObservableTest.notifyObservers_concurrentAddDoesNotThrow` | Add observer from second thread while iterating; no `ConcurrentModificationException` |
| `getNewPiece(KING)` throws IAE | `GetNewPieceTest.promoteToKing_throwsIllegalArgumentException` | `assertThrows(IllegalArgumentException.class, ...)` |
| `getNewPiece(PAWN)` throws IAE | `GetNewPieceTest.promoteToPawn_throwsIllegalArgumentException` | `assertThrows(IllegalArgumentException.class, ...)` |
| `Move.fromString` with KING promotion throws IAE | `MoveFromStringTest.fromString_kingPromotion_throwsIllegalArgumentException` | `assertThrows(IllegalArgumentException.class, ...)` (update existing null-asserting test) |
| `Move.fromString` with PAWN promotion throws IAE | `MoveFromStringTest.fromString_pawnPromotion_throwsIllegalArgumentException` | `assertThrows(IllegalArgumentException.class, ...)` (update existing null-asserting test) |

### From Task 2 — `Move` protocol format, `handleGameStatus` observer

| Behavior | Suggested Test Method | Asserts |
|---|---|---|
| `toProtocolString` regular move round-trips | `MoveFromStringTest.toProtocolString_regularMove_roundTrips` | `fromString(move.toProtocolString(), player)` produces equivalent move |
| `toProtocolString` promotion round-trips | `MoveFromStringTest.toProtocolString_promotion_roundTrips` | Same round-trip for promotion moves |
| `toProtocolString` castling round-trips | `MoveFromStringTest.toProtocolString_castling_roundTrips` | Same round-trip for O-O and O-O-O |
| `handleGameStatus` notifies observers | `OnlineGameServerConnectionTest.handleMessage_gameStatus_notifiesObservers` | Observer callback is invoked after GAME_STATUS message |

### From Task 3 — `OnlineGame.handleMove` exception handling

| Behavior | Suggested Test Method | Asserts |
|---|---|---|
| Malformed move does not crash handler | `OnlineGameServerConnectionTest.handleMessage_malformedMove_doesNotThrow` | `assertDoesNotThrow` when message has garbled MOVE_PARAM |

### From Task 4 — `ServerCommunicationTask`, `OnlineGame` static factory

| Behavior | Suggested Test Method | Asserts |
|---|---|---|
| `OnlineGame.create()` does not send messages | `OnlineGameServerConnectionTest.construction_doesNotSendMessage` | No messages sent before `connectToServerGame()` |
| Existing `OnlineGameServerConnectionTest` setUp uses `create()` | Update `setUp()` | Use `OnlineGame.create(...)` + explicit `connectToServerGame()` call |

### From Task 5 — `Chess.java` facade

| Behavior | Suggested Test Method | Asserts |
|---|---|---|
| `getJoinCode()` returns null for offline game | `ChessTest.getJoinCode_offlineGame_returnsNull` | `assertNull(chess.getJoinCode())` |
| `endGame()` safe when game is null | `ChessTest.endGame_noActiveGame_doesNotThrow` | `assertDoesNotThrow(chess::endGame)` |

### From Task 7 — `GameInstance.java` exception handling

| Behavior | Suggested Test Method | Asserts |
|---|---|---|
| Malformed move does not crash server handler | `GameInstanceTest.handleMove_malformedMove_doesNotThrow` | `assertDoesNotThrow` when message has garbled MOVE_PARAM |

---

## Documentation & Javadoc Requirements (DOCS-KEEPER ONLY)

### Javadoc Updates

| Class/Method | Action |
|---|---|
| `Observable.addObserver(Observer)` | Update: document `@throws NullPointerException` |
| `Observable.removeObserver(Observer)` | Update: document `@throws NullPointerException` |
| `Game.getNewPiece(Pieces)` | Update: document `@throws IllegalArgumentException` for KING/PAWN |
| `Move.toProtocolString()` | New: document wire format contract and relationship to `fromString()` |
| `Move.fromString(String, Player)` | Update: document expected protocol format input |
| `OnlineGame.create(...)` | New: document static factory, parameters, two-phase construction |
| `OnlineGame.connectToServerGame()` | Update: document public visibility, must-call-after-handler contract |
| `ServerCommunicationTask` constructor | Update: document `messageHandler` parameter |
| `Chess.getJoinCode()` | Update: document null return for offline games |
| `Chess.endGame()` | Update: document null-safety and connection cleanup |

### CLAUDE.md Updates

- `modules/core/CLAUDE.md`: Update `Observable` description (CopyOnWriteArrayList), `Move` description (`toProtocolString()`), `OnlineGame` description (static factory method `OnlineGame.create()`, private constructor, two-phase construction).
- `modules/application/CLAUDE.md`: Remove debt items 2 and 3 (fixed); update `ServerCommunicationTask` description (handler in constructor, no dedup, try-finally); update `Chess.java` description (uses `OnlineGame.create()`, not direct constructor).
- `modules/server/CLAUDE.md`: Remove stale debt item 8 (reflection reference already removed).

