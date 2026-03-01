# 0002: Player Name Protocol Extension for CREATE_GAME and JOIN_GAME

Status: accepted
Date: 2026-03-01
Deciders: human, architect

## Context and Problem Statement

The server-side game creation flow needs player names in order to pass them to `Game.createServerGame()`. Before this decision, `ServerGame` used hardcoded default names ("Player 1" / "Player 2") and there was no protocol mechanism to supply real names. Additionally, the `JOIN_GAME` message previously carried the game ID as a raw integer in the content field, which was fragile and inconsistent with the key=value format used by all other message types.

The question is: how should player names reach the server, and how should the join-game message identify the target game?

## Considered Options

1. Extend `CREATE_GAME` and `JOIN_GAME` message content with `playerName` and `gameId` key-value parameters.
2. Use hardcoded default names and defer name support to a later protocol version.
3. Add a separate `SET_PLAYER_NAME` message type sent after connection setup.

## Decision Outcome

Chosen option: Option 1 — extend `CREATE_GAME` and `JOIN_GAME` with key-value parameters.

**Protocol changes:**

| Message | Before | After |
|---------|--------|-------|
| `CREATE_GAME` | `ruleset=STANDARD` | `ruleset=STANDARD playerName=<name>` |
| `JOIN_GAME` | `<raw integer gameId>` | `gameId=<id> playerName=<name>` |

Both parameters are optional for backward compatibility. When `playerName` is absent or blank, the server uses `"Player 1"` or `"Player 2"` as the default. When `gameId` is absent or non-numeric in `JOIN_GAME`, the server responds with an `ERROR`.

### Consequences

- Good: Player names are available at game-creation time, so `Game` and `Player` objects are correctly initialised from the start with no need for post-construction mutation.
- Good: `JOIN_GAME` now uses the same key=value format as all other messages, removing the special-case raw-integer parsing.
- Good: Backward compatibility is preserved — clients that omit `playerName` continue to work with default names.
- Bad: Clients that send `JOIN_GAME` with a raw integer game ID (old format) will receive an `ERROR` instead of joining. This is a protocol breaking change for old clients.
- Neutral: Game creation is deferred inside `GameInstance` until both players have connected, so both names are available before the `Game` superclass constructor runs.

## Pros and Cons of the Options

### Option 1 — Extend CREATE_GAME and JOIN_GAME

- Good: Names available at the right moment.
- Good: Consistent message format.
- Bad: JOIN_GAME format change breaks old clients that use raw integer content.

### Option 2 — Hardcoded defaults

- Good: No protocol change.
- Bad: Human explicitly requires real player names for better UX. Names cannot be recovered later without mutation of immutable `Player` records.

### Option 3 — Separate SET_PLAYER_NAME message

- Good: No change to existing messages.
- Bad: Requires two-phase setup per player. The game cannot start until names are received, which adds protocol complexity and a new message type with no other use.
