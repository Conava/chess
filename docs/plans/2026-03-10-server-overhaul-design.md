# Design: Server Overhaul
Date: 2026-03-10

## Overview

Transform the experimental TCP chess server into a production-ready, always-running service.
Six concerns addressed together because they share a new persistence layer:

1. **Tech debt cleanup** — refactor server internals, fix known bugs, normalize protocol
2. **User identity & auth** — username/password accounts, PBKDF2 hashing, session tokens
3. **Persistence** — SQLite DB: users, sessions, games, moves, chat
4. **Disconnect/pause** — game pauses on TCP drop; forfeit after configurable timeout; mutual "save for later" allows both players to resume another day
5. **In-game chat** — messages relayed and persisted per game; chat panel on left in UI
6. **Matchmaking** — per-ruleset FIFO queue; auto-pair when two players queue for same ruleset

---

## Architecture

### Module changes at a glance

| Module | What changes |
|--------|-------------|
| `core` | Add new `MessageType` values; add `GameState.PAUSED` and `GameState.SAVED` |
| `server` | New `persistence/`, `auth/`, `matchmaking/` packages; refactor `Server` + `ClientHandler` + `GameInstance`; fix promotion bug; config file |
| `application` | New login/register screen; chat panel in `GameController`; matchmaking UI; resume-game flow; handle new message types |

### Server package structure (after)

```
io.github.conava.chess.server
├── Server.java                        # Accept loop only; wires services together
├── config/
│   └── ServerConfig.java              # Loads server.properties (port, timeout, DB path, max games)
├── auth/
│   └── AuthService.java               # register, login, verifyToken, PBKDF2 hashing
├── persistence/
│   ├── DatabaseManager.java           # Single SQLite connection, schema init, migration
│   ├── UserRepository.java            # CRUD for users
│   ├── GameRepository.java            # CRUD for games, moves, chat
│   └── SessionRepository.java         # CRUD for auth tokens
├── matchmaking/
│   └── MatchmakingService.java        # Per-ruleset ConcurrentLinkedDeque; tryMatch()
└── management/
    ├── GameManager.java               # NEW: owns game lifecycle (semaphore, ID counter, map)
    ├── PlayerSession.java             # NEW: auth context per connection (userId, username, token)
    ├── ClientHandler.java             # REFACTORED: I/O + dispatch only; delegates to services
    └── GameInstance.java              # REFACTORED: pause/resume/save; promotion fix; chat relay
```

### Application new/modified files

```
application/
├── controllers/
│   ├── LoginController.java           # NEW: username/password form (login + register link)
│   ├── RegisterController.java        # NEW: register new account
│   ├── GameController.java            # MODIFIED: add chat panel (left side) + "Save & Exit" btn
│   ├── MainMenuController.java        # MODIFIED: gate on auth; show username in header
│   ├── OnlineSetupController.java     # MODIFIED: add "Find Match" button
│   └── WaitingForMatchController.java # NEW: "searching for opponent..." overlay
├── network/
│   └── ServerCommunicationTask.java   # MODIFIED: handle new message types
└── Chess.java                         # MODIFIED: auth state; matchmaking; resume-game entry points
```

---

## Data Model

### SQLite schema

```sql
CREATE TABLE users (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,       -- PBKDF2-HMAC-SHA256, base64-encoded
    password_salt TEXT NOT NULL,
    created_at    INTEGER NOT NULL     -- epoch seconds
);

CREATE TABLE sessions (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id    INTEGER NOT NULL REFERENCES users(id),
    token      TEXT UNIQUE NOT NULL,   -- UUID v4
    created_at INTEGER NOT NULL,
    expires_at INTEGER NOT NULL        -- epoch seconds; default: created_at + 30 days
);

CREATE TABLE games (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    white_user_id   INTEGER REFERENCES users(id),
    black_user_id   INTEGER REFERENCES users(id),
    ruleset         TEXT NOT NULL,                   -- 'STANDARD' | 'CHESS960'
    position_index  INTEGER NOT NULL DEFAULT -1,     -- Chess960 Scharnagl index
    state           TEXT NOT NULL DEFAULT 'RUNNING', -- GameState.name()
    created_at      INTEGER NOT NULL,
    updated_at      INTEGER NOT NULL,
    disconnect_user_id INTEGER,                      -- user who disconnected (nullable)
    disconnect_at   INTEGER                          -- epoch seconds of disconnect (nullable)
);

CREATE TABLE moves (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    game_id    INTEGER NOT NULL REFERENCES games(id),
    move_no    INTEGER NOT NULL,       -- 0-indexed
    notation   TEXT NOT NULL,          -- Move.toProtocolString()
    played_at  INTEGER NOT NULL
);

CREATE TABLE chat_messages (
    id       INTEGER PRIMARY KEY AUTOINCREMENT,
    game_id  INTEGER NOT NULL REFERENCES games(id),
    user_id  INTEGER NOT NULL REFERENCES users(id),
    content  TEXT NOT NULL,
    sent_at  INTEGER NOT NULL
);
```

---

## Wire Protocol Changes

New `MessageType` enum values added to `core`:

| Value | Direction | Content example |
|-------|-----------|----------------|
| `REGISTER` | C→S | `username=alice password=secret` |
| `LOGIN` | C→S | `username=alice password=secret` |
| `AUTH_TOKEN` | S→C | `token=<uuid> userId=42` |
| `RESUME_GAME` | C→S | `token=<uuid> gameId=7` |
| `GAME_HISTORY` | S→C | `moves=e2-e4,e7-e5,... ruleset=STANDARD position=-1` |
| `CHAT` | C↔S | `from=alice message=Good game!` |
| `QUEUE` | C→S | `ruleset=STANDARD token=<uuid>` |
| `DEQUEUE` | C→S | `token=<uuid>` |
| `MATCHED` | S→C | same payload as current `JOIN_CODE` |
| `SAVE_GAME` | C↔S | `gameId=7` (request); server echoes to opponent |
| `SAVE_ACCEPTED` | S→C | `gameId=7` (both sides confirmed; game saved) |

Existing types are unchanged. `CREATE_GAME` and `JOIN_GAME` now require an extra `token=<uuid>` parameter for auth validation.

---

## Key Behaviours

### Auth flow
1. Client connects → must send `LOGIN` or `REGISTER` before any other message.
2. Server replies `AUTH_TOKEN:token=<uuid>` on success; `ERROR` on failure.
3. All subsequent messages from this client are associated with the token's user.
4. Token stored locally by the app in `SettingsService` — survives app restarts.

### Reconnect after disconnect
1. Player A drops TCP → `GameInstance` sets game state to `PAUSED`, stores `disconnect_at`.
2. Server sends `GAME_STATUS:gameState=PAUSED` to Player B.
3. Disconnect timeout timer starts (`ServerConfig.disconnectTimeoutSeconds`, default 300).
4. Player A reconnects within timeout: sends `RESUME_GAME:token=... gameId=...`.
   - Server sends `GAME_HISTORY:moves=... ruleset=... position=...` to Player A.
   - Both clients get `GAME_STATUS:gameState=RUNNING` → game resumes.
5. Timeout fires before reconnect: server sends `GAME_STATUS:gameState=WHITE/BLACK_WON_BY_TIMEOUT` to Player B; game saved as terminal in DB.

### Mutual save for later
1. Either player sends `SAVE_GAME`.
2. Server forwards `SAVE_GAME` to the opponent.
3. Opponent confirms by also sending `SAVE_GAME`.
4. Server saves game to DB as `PAUSED`, sends `SAVE_ACCEPTED` to both, closes both connections gracefully.
5. Either player can later reconnect, log in, and send `RESUME_GAME:gameId=N` to restore the game.

### Matchmaking
1. Player sends `QUEUE:ruleset=STANDARD token=<uuid>`.
2. `MatchmakingService` adds user to per-ruleset queue (FIFO `ConcurrentLinkedDeque`).
3. On each queue addition, `tryMatch()` checks if ≥ 2 players are in the same ruleset queue.
4. If match found: dequeue both, create a `GameInstance`, send `MATCHED:...` to both (same payload as `JOIN_CODE`).
5. Player sends `DEQUEUE` to leave the queue.

### Chat
- During an active game, either player sends `CHAT:message=hello` (no `from` field on outbound).
- Server validates, prepends `from=<username>`, stores in `chat_messages`, relays to both players.
- On `RESUME_GAME`, server sends all chat history as sequential `CHAT` messages before `GAME_STATUS:RUNNING`.

### Promotion bug fix (current bug in server)
`GameInstance.handleMove` currently calls `game.movePiece(start, end)` even for promotion moves.
`Move.fromString` correctly returns a `PromotionMove` — its target piece is ignored by `movePiece`.
Fix: after `deserializeMove`, check `instanceof PromotionMove` and call `game.promoteMove(start, end, piece)` instead.

### Server config (`server.properties`)
```
port=54321
max_games=40
disconnect_timeout_seconds=300
db_path=chess.db
session_expiry_days=30
```

### Logging
Replace `System.out.println` in stats/stop with `LOGGER`. Add a file handler writing to `logs/chess-server.log` with rotation.

---

## Tech Debt Fixes

| Item | Fix |
|------|-----|
| S-001: `Server` leaks internal Semaphore/AtomicInteger | Extract `GameManager` class; `ClientHandler` uses `gameManager` not raw server getters |
| S-002: `connectionsList` drift on early exception | Add `finally` block in `ClientHandler.run()` to always call `cleanup()` |
| S-003: Move relay echoes client string (not normalized) | After `game.movePiece`, send `new Message(MOVE, "move=" + move.toProtocolString() + " ...")` |
| S-004: Promotion not handled server-side | See promotion bug fix above |
| S-005: No config file | `ServerConfig` reads `server.properties` at startup |
| S-006: `getGameSemaphore()`/`getGameIdCounter()` public on `Server` | Move to `GameManager`; remove public getters from `Server` |
| C-008: German default player names in `Game` | Fix in `Game.getDefaultPlayerName` → English defaults |

---

## Open Questions

None — all design choices resolved by user input.

---

## Scope: What is NOT in this design

- TLS/HTTPS (future; reverse-proxy with nginx + Let's Encrypt is the deployment recommendation)
- ELO ranking / match history stats UI
- Spectator mode
- Time controls / chess clocks
