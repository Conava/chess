# Implementer Agent Memory

## Project: chess (Java, Maven multi-module)

### Worktree Pattern
Tasks are executed in isolated worktrees under `.claude/worktrees/<id>/`.
Always use absolute paths under the worktree root, never relative paths.

### Build Commands
- Test core module: `mvn test -pl modules/core -am` from worktree root
- Full build: `mvn clean install` from worktree root

### Key File Paths (core module)
- Ruleset: `modules/core/src/main/java/io/github/conava/chess/core/logic/ruleset/standardChessRuleset/StandardChessRuleset.java`
- Board: `modules/core/src/main/java/io/github/conava/chess/core/data/board/Board.java`
- Pieces (abstract + subclasses): `modules/core/src/main/java/io/github/conava/chess/core/data/pieces/`
- Tests: `modules/core/src/test/java/io/github/conava/chess/core/logic/ruleset/StandardChessRulesetTest.java`

### Coordinate Convention
`Square(y, x)` where y=0 is white's back rank (rank 1), x=0 is a-file.
`board.getSquare(y, x)` — first arg is row/rank, second is column/file.

### Board Deep Copy (T02 complete)
`Board.getCopy()` returns a true deep copy. Safe for move simulation.
Use `boardCopy.getSquare(square.getY(), square.getX())` to get the copy's square, not original references.

### Castling Identification
Castling candidate: piece instanceof King AND Math.abs(targetX - sourceX) == 2.
Transit square X: `sourceX + Integer.signum(targetX - sourceX)`.

### Check Detection
`isCheck(board, player, moves)` — player is the one whose king we're checking.
Implemented via reverse-attack scan from king's position.

### getLegalSquares Filter (T06)
Simulate each pseudo-legal move on a board copy. Filter out moves leaving the moving player in check.
For castling: also check in-check guard and transit-square guard before the final-square simulation.
Use plain `new Move(copyStart, copyEnd)` for simulation — NOT CastleMove (rook relocation not needed for check detection).
