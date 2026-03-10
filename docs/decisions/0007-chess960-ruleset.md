# 0007: Chess960 Ruleset as a Strategy Extension

Status: accepted
Date: 2026-03-10
Deciders: Project maintainer

## Context and Problem Statement

Chess960 (Fischer Random Chess) uses a randomized back-rank starting position (one of 960
legal positions defined by the Scharnagl numbering system) but otherwise follows standard
chess rules. The project needed to support Chess960 as a second playable variant without
modifying the existing `StandardChessRuleset`, branching inside `Game`, or duplicating move
logic.

The key sub-problems:

1. **Starting position** — Chess960 uses a randomly generated back rank. The standard
   `PossibleStandardPosition` class hard-codes the traditional layout and cannot be reused.
2. **Castling** — In Chess960 the king and rooks begin on arbitrary files. The castling
   target squares (king moves to the c- or g-file; rook moves to the d- or f-file) are
   independent of the pieces' starting files. Standard castling detection (king moves exactly
   two squares) does not generalize.
3. **Online play** — In an online game, both clients must use the same randomly generated
   position. The position must be generated once (on the server) and transmitted to both
   clients before the board is initialized.
4. **Move serialization** — Chess960 castling moves need to encode the rook's origin file and
   the king's destination file for correct round-trip serialization over the wire protocol.

## Considered Options

1. **Strategy extension** — add a `Chess960Ruleset` class extending `AbstractChessRuleset`
   that overrides only the methods that differ, plus deferred board initialization for online
   games.
2. **Branching inside `Game`** — detect the active ruleset type in `Game.movePiece`,
   `Game.evaluateGameEnd`, and `Board.handleCastleMove`, and branch on ruleset type.
3. **Copy-and-modify** — duplicate `StandardChessRuleset` as `Chess960Ruleset` and modify
   the copied class.

## Decision Outcome

Chosen option: "Strategy extension", because it is the only option that complies with
Architecture Law 5 ("Strategy pattern owns ruleset variation — new rule variants must
implement `Ruleset`, not branch inside `Game`") and minimizes duplication.

### Consequences

- Good: All shared chess logic (move dispatch, check filtering, game-end detection) is
  inherited from `AbstractChessRuleset` and `Game`. `Chess960Ruleset` overrides only five
  methods: `getStartBoard`, `getPseudoLegalKingSquares`, `getLegalSquares`, `getGameLabel`,
  and `deserializeMove`.
- Good: Adding further variants (e.g., Crazyhouse, King of the Hill) follows the same
  pattern: extend `AbstractChessRuleset`, add an enum value, wire the switch case.
- Good: The deferred board initialization pattern (`deferBoardInit=true` in the `Game`
  constructor) is generic and reusable for any future ruleset that needs server-provided
  parameters at runtime.
- Bad: Chess960 castling detection requires `PossibleChess960KingMoves` — a new move-generator
  that cannot reuse `PossibleStandardKingMoves` because the standard generator hard-codes
  two-square king movement as the castling signal. This is a small but real duplication.
- Neutral: `CastleMove` was extended with optional `rookOriginFile` and `kingDestFile` fields.
  These are `null` for standard castling, preserving backward compatibility. `Board.handleCastleMove`
  branches on whether these fields are set.

## Pros and Cons of the Options

### Strategy extension (chosen)

- Good: Complies with Architecture Law 5.
- Good: Minimal override surface — only methods that truly differ.
- Good: Deferred initialization pattern is generic and documented.
- Bad: New move-generator class (`PossibleChess960KingMoves`) required for castling detection.

### Branching inside `Game`

- Good: No new classes required.
- Bad: Violates Architecture Law 5 explicitly.
- Bad: Scatters Chess960-specific logic across `Game`, `Board`, and move generators.
- Bad: Every future variant requires further branching, growing the switch surfaces unboundedly.

### Copy-and-modify

- Good: Full isolation — changes to `StandardChessRuleset` cannot affect `Chess960Ruleset`.
- Bad: All shared logic (check filtering, en passant, promotion, game-end detection) is
  duplicated. Any bug fix or enhancement must be applied twice.
- Bad: Massive duplication violates the DRY principle and makes maintenance expensive.

## Implementation Notes

### Position generation

`Chess960StartPosition` generates a random valid back-rank layout using the Scharnagl
numbering system (index 0–959). For online games, the server generates the index once in
`GameInstance`, stores it, and injects it into both the `JOIN_CODE` message (sent to the
game creator) and the `SUCCESS` message (sent to the joiner) as a `chess960Position` key.

### Deferred board initialization

`OnlineGame` passes `deferBoardInit=true` to the `Game` superclass constructor. Board and
ruleset remain `null` until `initializeBoard(Ruleset)` is called. `OnlineGame.handleJoinCode`
and `OnlineGame.handleSuccess` read the `chess960Position` parameter from the server message,
construct a `Chess960Ruleset` with the correct Scharnagl index, and call `initializeBoard`.
This ensures both clients use identical board layouts.

### Castling in Chess960

`PossibleChess960KingMoves` scans the back rank to locate the king's actual rooks (not
assumed to be on files 0 and 7). For each eligible rook it generates a castling target:
the king's destination file (c=2 for queenside, g=6 for kingside) and records the rook's
actual origin file in the `CastleMove`. `Board.handleCastleMove` reads these fields to place
the rook on the correct intermediate file (d=3 or f=5) regardless of where the rook started.
