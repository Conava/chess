# Docs Architect Memory — Chess Project

## Project facts (verified from source)

- Java 17, Maven multi-module: `core`, `application`, `server` under `modules/`
- All packages: `io.github.conava.chess`
- Version: 0.9 across all modules
- Entry points: `Chess.java` (application), `Server.java` (server)
- JavaFX version: 21 (javafx-controls, javafx-fxml)
- JUnit: 5.10.1 in core, 5.8.1 in application/server; Mockito 5.5.0 in application tests only

## Docs folder — created from scratch

The `docs/` folder did not exist before this session. All files were created new:
- `docs/architecture/overview.md`
- `docs/architecture/module-boundaries.md`
- `docs/architecture/design-patterns.md`
- `docs/api/chess-facade.md`
- `docs/guides/getting-started.md`
- `docs/guides/contributing.md`
- `docs/guides/adding-a-ruleset.md`
- `docs/migration/swing-to-javafx.md`
- `docs/decisions/0001` through `0005`
- README.md updated with Documentation section

## Key architectural facts to preserve across sessions

- `Chess.java` in application module IS ALSO the JavaFX `Application` subclass (entry point)
- `Game.executeMove` detects king capture (not true checkmate) for game-end — known debt
- `Board.getCopy()` is a SHALLOW copy — same Square references — known fragility
- `getLegalSquares` returns pseudo-legal moves only; check-legality filtering is NOT implemented
- `GameState` enum display strings are in German (known debt)
- Default player names in `Game.getDefaultPlayerName()` are in German
- `Board.getRowCount()` / `getColCount()` names are SWAPPED relative to what they return
- Castling is likely broken due to logic bug in `PossibleStandardKingMoves`
- En passant is not implemented despite `Pawn.hasMoveJustMovedTwoSquares()` existing
- Server max concurrent games: 40 (hardcoded `MAX_GAMES` constant)
- Server default port: 54321

## Module CLAUDE.md files exist and are detailed

Each module has its own CLAUDE.md with deep class-level documentation:
- `modules/core/CLAUDE.md` — full package structure, known debt list
- `modules/application/CLAUDE.md` — full controller/service documentation
- `modules/server/CLAUDE.md` — full server architecture documentation

These are excellent sources for future documentation updates.

## ADR numbering

Next ADR should be 0006. Existing: 0001-0005.
