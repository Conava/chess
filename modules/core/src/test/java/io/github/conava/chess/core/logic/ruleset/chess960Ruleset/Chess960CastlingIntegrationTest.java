package io.github.conava.chess.core.logic.ruleset.chess960Ruleset;

import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.board.Board;
import io.github.conava.chess.core.data.pieces.*;
import io.github.conava.chess.core.data.player.Player;
import io.github.conava.chess.core.data.player.PlayerColor;
import io.github.conava.chess.core.exceptions.IllegalMoveException;
import io.github.conava.chess.core.logic.game.Game;
import io.github.conava.chess.core.logic.moves.CastleMove;
import io.github.conava.chess.core.logic.moves.Move;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration tests for Chess960 castling.
 *
 * <p>Exercises the full pipeline:
 * {@code Chess960Ruleset} → {@code PossibleChess960KingMoves} → {@code Board.handleCastleMove960}.
 *
 * <p>Test coverage:
 * <ol>
 *   <li>Kingside castle with rook at file 5 ({@code rookOriginFile=5}, {@code kingDestFile=6}):
 *       king lands on g-file (x=6), rook lands on f-file (x=5).</li>
 *   <li>Queenside castle with rook at file 1 and file 3: king lands on c-file (x=2),
 *       rook lands on d-file (x=3).</li>
 *   <li>Castle where king destination equals rook origin (swap scenario) — tested at
 *       {@code Board} level directly; the {@code getLegalSquares} path is covered by T08-8.</li>
 *   <li>Castle blocked by a piece in the post-castle corridor.</li>
 *   <li>Castle valid when corridor is clear but king/rook are not at standard positions.</li>
 *   <li>En passant still works in a Chess960 game (non-castling rule parity).</li>
 *   <li>Check detection still works — a move that leaves the king in check is illegal.</li>
 *   <li>{@code getLegalSquares} in {@code Chess960Ruleset} includes the rook's square as a
 *       castling target (end-to-end, combining with the {@code getLegalSquares} override).</li>
 * </ol>
 *
 * <p><b>Test design notes:</b>
 * <ul>
 *   <li>All tests that invoke {@link Chess960Ruleset#getLegalSquares} use the white king at
 *       file 4 (e-file). For a queenside castle, the king's destination is file 2 (c-file)
 *       which is less than the king's starting file 4 — the direction detection in
 *       {@code Board.handleCastleMove960} and in {@code Chess960Ruleset.getLegalSquares}
 *       agree, avoiding the direction-mismatch corner case that affects king positions
 *       left of the c-file.</li>
 *   <li>The swap scenario (king destination == rook origin) is tested at the
 *       {@code Board.executeMove} level, where piece placement is directly verifiable.</li>
 *   <li>Full-pipeline tests use {@link Game#createServerGame(io.github.conava.chess.core.logic.ruleset.Ruleset, String, String)}
 *       with {@code Chess960Ruleset(518)} (position 518 = standard RNBQKBNR back rank) to
 *       ensure deterministic piece layout.</li>
 * </ul>
 */
class Chess960CastlingIntegrationTest {

    private Player white;
    private Player black;

    @BeforeEach
    void setUp() {
        white = new Player("White", PlayerColor.WHITE);
        black = new Player("Black", PlayerColor.BLACK);
    }

    // =========================================================================
    // Helper utilities
    // =========================================================================

    /**
     * Builds an 8×8 board of empty squares.
     */
    private Square[][] emptyBoard() {
        Square[][] squares = new Square[8][8];
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                squares[y][x] = new Square(y, x);
            }
        }
        return squares;
    }

    /**
     * Convenience helper to get the piece at (y, x) from a board.
     */
    private Piece pieceAt(Board board, int y, int x) {
        return board.getSquare(y, x).getPiece();
    }

    // =========================================================================
    // T08-1: Kingside castle with rook at file 5 (rookOriginFile=5, kingDestFile=6)
    //
    // Board setup (white, rank 0):
    //   x=3: King (unmoved)    x=5: Rook (unmoved, non-standard position)
    //
    // After kingside castle: King lands on x=6 (g-file), Rook lands on x=5 (f-file).
    // This covers the T03-deferred scenario: rookOriginFile=5 vs the usual rookOriginFile=6.
    // =========================================================================

    @Test
    void kingsideCastle_rookAtFile5_kingLandsOnGFile_rookLandsOnFFile_viaBoard() {
        // Build a board: white king at x=3, white rook (unmoved) at x=5, black king at y=7,x=4
        Square[][] squares = emptyBoard();
        King king = new King(white);
        Rook kingsideRook = new Rook(white);
        King blackKing = new King(black);
        squares[0][3].setPiece(king);
        squares[0][5].setPiece(kingsideRook);
        squares[7][4].setPiece(blackKing);
        Board board = new Board(squares);

        // Execute Chess960 kingside castle: king "moves to" rook at x=5
        // rookOriginFile=5, kingDestFile=6 → standard FIDE outcome
        board.executeMove(new CastleMove(board.getSquare(0, 3), board.getSquare(0, 5),
                /*rookOriginFile=*/ 5, /*kingDestFile=*/ 6));

        assertInstanceOf(King.class, pieceAt(board, 0, 6), "King must land on g-file (x=6) after kingside castle with rookOriginFile=5");
        assertInstanceOf(Rook.class, pieceAt(board, 0, 5), "Rook must land on f-file (x=5) after kingside castle with rookOriginFile=5");
        assertNull(pieceAt(board, 0, 3), "King's original square (x=3) must be empty after castle");
    }

    @Test
    void kingsideCastle_rookAtFile5_isLegalTarget_viaLegalSquares() {
        // King at x=3, kingside rook at x=5, no obstacles.
        // getLegalSquares must include the rook's square (x=5) as the castling target.
        Square[][] squares = emptyBoard();
        King king = new King(white);
        Rook kingsideRook = new Rook(white);
        King blackKing = new King(black);
        squares[0][3].setPiece(king);
        squares[0][5].setPiece(kingsideRook);
        squares[7][4].setPiece(blackKing);
        Board board = new Board(squares);

        Chess960Ruleset ruleset = new Chess960Ruleset(518);
        List<Square> legal = ruleset.getLegalSquares(board.getSquare(0, 3), board, new ArrayList<>(), white, black);

        assertTrue(legal.stream().anyMatch(s -> s.getY() == 0 && s.getX() == 5), "getLegalSquares must include rook's file (x=5) as the castling target " + "(T03 deferred scenario: rookOriginFile=5, kingDestFile=6)");
    }

    // =========================================================================
    // T08-2: Queenside castle with rook at file 1 and file 3
    //
    // Non-standard Chess960 rook positions. King always at x=4 to avoid the
    // direction-mismatch corner case.
    // =========================================================================

    @Test
    void queensideCastle_rookAtFile1_kingLandsOnCFile_rookLandsOnDFile_viaBoard() {
        // King at x=4, queenside rook at x=1 (not the standard a-file).
        // After castle: king at x=2 (c-file), rook at x=3 (d-file).
        Square[][] squares = emptyBoard();
        Rook queensideRook = new Rook(white);
        King king = new King(white);
        King blackKing = new King(black);
        squares[0][1].setPiece(queensideRook);
        squares[0][4].setPiece(king);
        squares[7][4].setPiece(blackKing);
        Board board = new Board(squares);

        board.executeMove(new CastleMove(board.getSquare(0, 4), board.getSquare(0, 1),
                /*rookOriginFile=*/ 1, /*kingDestFile=*/ 2));

        assertInstanceOf(King.class, pieceAt(board, 0, 2), "King must land on c-file (x=2) after queenside castle with rookOriginFile=1");
        assertInstanceOf(Rook.class, pieceAt(board, 0, 3), "Rook must land on d-file (x=3) after queenside castle with rookOriginFile=1");
        assertNull(pieceAt(board, 0, 4), "King's original square (x=4) must be empty after castle");
        assertNull(pieceAt(board, 0, 1), "Rook's original square (x=1) must be empty after castle");
    }

    @Test
    void queensideCastle_rookAtFile3_kingLandsOnCFile_rookLandsOnDFile_viaBoard() {
        // King at x=4, queenside rook at x=3 (immediately to king's left).
        // After castle: king at x=2, rook at x=3 (rook stays — already at dest).
        Square[][] squares = emptyBoard();
        Rook queensideRook = new Rook(white);
        King king = new King(white);
        King blackKing = new King(black);
        squares[0][3].setPiece(queensideRook);
        squares[0][4].setPiece(king);
        squares[7][4].setPiece(blackKing);
        Board board = new Board(squares);

        board.executeMove(new CastleMove(board.getSquare(0, 4), board.getSquare(0, 3),
                /*rookOriginFile=*/ 3, /*kingDestFile=*/ 2));

        assertInstanceOf(King.class, pieceAt(board, 0, 2), "King must land on c-file (x=2) after queenside castle with rookOriginFile=3");
        assertInstanceOf(Rook.class, pieceAt(board, 0, 3), "Rook must remain on d-file (x=3) after queenside castle with rookOriginFile=3");
        assertNull(pieceAt(board, 0, 4), "King's original square (x=4) must be empty after castle");
    }

    @Test
    void queensideCastle_rookAtFile1_isLegalTarget_viaLegalSquares() {
        // King at x=4, queenside rook at x=1. No pieces between them.
        // getLegalSquares must include the rook's square (x=1).
        Square[][] squares = emptyBoard();
        Rook queensideRook = new Rook(white);
        King king = new King(white);
        King blackKing = new King(black);
        squares[0][1].setPiece(queensideRook);
        squares[0][4].setPiece(king);
        squares[7][4].setPiece(blackKing);
        Board board = new Board(squares);

        Chess960Ruleset ruleset = new Chess960Ruleset(518);
        List<Square> legal = ruleset.getLegalSquares(board.getSquare(0, 4), board, new ArrayList<>(), white, black);

        assertTrue(legal.stream().anyMatch(s -> s.getY() == 0 && s.getX() == 1), "getLegalSquares must include rook's square (x=1) as queenside castling target");
    }

    @Test
    void queensideCastle_rookAtFile3_isLegalTarget_viaLegalSquares() {
        // King at x=4, queenside rook at x=3. Adjacent left.
        // getLegalSquares must include the rook's square (x=3).
        Square[][] squares = emptyBoard();
        Rook queensideRook = new Rook(white);
        King king = new King(white);
        King blackKing = new King(black);
        squares[0][3].setPiece(queensideRook);
        squares[0][4].setPiece(king);
        squares[7][4].setPiece(blackKing);
        Board board = new Board(squares);

        Chess960Ruleset ruleset = new Chess960Ruleset(518);
        List<Square> legal = ruleset.getLegalSquares(board.getSquare(0, 4), board, new ArrayList<>(), white, black);

        assertTrue(legal.stream().anyMatch(s -> s.getY() == 0 && s.getX() == 3), "getLegalSquares must include adjacent rook's square (x=3) as queenside castling target");
    }

    // =========================================================================
    // T08-3: Swap scenario — king destination equals rook origin.
    //
    // In Chess960, "king moves to rook's square" is the encoding for ALL castling.
    // The "swap scenario" specifically means the king's final destination (g/c-file)
    // happens to be the same file the rook started on.
    //
    // Tested at Board.executeMove level to verify piece placement directly.
    //
    // Kingside swap: king at x=5, rook at x=6 → king lands on x=6, rook on x=5.
    // Queenside swap: king at x=3, rook at x=2 → king lands on x=2, rook on x=3.
    // =========================================================================

    @Test
    void kingsideCastle_swapScenario_boardExecution_kingAtDestinationEqualsRookOrigin() {
        // King at x=5, kingside rook at x=6. Castle: king ends on x=6 (= rook origin).
        // After: king on g-file (x=6), rook on f-file (x=5) — they swap places.
        Square[][] squares = emptyBoard();
        King king = new King(white);
        Rook kingsideRook = new Rook(white);
        King blackKing = new King(black);
        squares[0][5].setPiece(king);
        squares[0][6].setPiece(kingsideRook);
        squares[7][4].setPiece(blackKing);
        Board board = new Board(squares);

        board.executeMove(new CastleMove(board.getSquare(0, 5), board.getSquare(0, 6),
                /*rookOriginFile=*/ 6, /*kingDestFile=*/ 6));

        assertInstanceOf(King.class, pieceAt(board, 0, 6), "King must be on g-file (x=6) after kingside swap castle");
        assertInstanceOf(Rook.class, pieceAt(board, 0, 5), "Rook must be on f-file (x=5) after kingside swap castle");
        assertNull(pieceAt(board, 0, 4), "File x=4 must remain empty");
    }

    @Test
    void queensideCastle_swapScenario_boardExecution_rookAtFile2_kingLandsOnCFile() {
        // King at x=4, queenside rook at x=2. Castle: king ends on x=2 (= rook origin).
        // After: king on c-file (x=2), rook on d-file (x=3).
        Square[][] squares = emptyBoard();
        King king = new King(white);
        Rook queensideRook = new Rook(white);
        King blackKing = new King(black);
        squares[0][4].setPiece(king);
        squares[0][2].setPiece(queensideRook);
        squares[7][4].setPiece(blackKing);
        Board board = new Board(squares);

        board.executeMove(new CastleMove(board.getSquare(0, 4), board.getSquare(0, 2),
                /*rookOriginFile=*/ 2, /*kingDestFile=*/ 2));

        assertInstanceOf(King.class, pieceAt(board, 0, 2), "King must be on c-file (x=2) after queenside swap castle with rook at x=2");
        assertInstanceOf(Rook.class, pieceAt(board, 0, 3), "Rook must be on d-file (x=3) after queenside swap castle");
        assertNull(pieceAt(board, 0, 4), "King's original square (x=4) must be empty after castle");
    }

    // =========================================================================
    // T08-3b: Queenside castle — king at file 1 (b1), rook at file 0 (a1).
    //
    // Previously avoided because the direction-mismatch bug in handleCastleMove960
    // caused wrong queenside detection when kingDestFile (2) > kingStartFile (1).
    // With the fix (using rookFile instead of kingDestFile), rookFile=0 < kingStartFile=1
    // → queenside=true. King lands at c1 (x=2), rook at d1 (x=3).
    // =========================================================================

    @Test
    void queensideCastle_kingAtFile1_rookAtFile0_kingLandsOnCFile_rookLandsOnDFile() {
        // King at x=1 (b1), queenside rook at x=0 (a1). No pieces between them.
        // rookOriginFile=0 < kingStartFile=1 → queenside direction correctly detected.
        // After castle: king at x=2 (c1), rook at x=3 (d1).
        Square[][] squares = emptyBoard();
        King king = new King(white);
        Rook queensideRook = new Rook(white);
        King blackKing = new King(black);
        squares[0][1].setPiece(king);
        squares[0][0].setPiece(queensideRook);
        squares[7][4].setPiece(blackKing);
        Board board = new Board(squares);

        board.executeMove(new CastleMove(board.getSquare(0, 1), board.getSquare(0, 0),
                /*rookOriginFile=*/ 0, /*kingDestFile=*/ 2));

        assertInstanceOf(King.class, pieceAt(board, 0, 2), "King must land on c1 (x=2) after queenside castle with king at b1 and rook at a1");
        assertInstanceOf(Rook.class, pieceAt(board, 0, 3), "Rook must land on d1 (x=3) after queenside castle with king at b1 and rook at a1");
        assertNull(pieceAt(board, 0, 1), "King's original square (x=1, b1) must be empty after castle");
        assertNull(pieceAt(board, 0, 0), "Rook's original square (x=0, a1) must be empty after castle");
    }

    // =========================================================================
    // T08-4: Castle blocked by a piece in the post-castle corridor.
    //
    // King at x=4, rooks at x=0 and x=7.
    // Blocked: a friendly piece sits on the king's destination or rook's destination.
    // =========================================================================

    @Test
    void kingsideCastle_blockedByPieceOnKingDestination_gFile_isIllegal() {
        // King at x=4, kingside rook at x=7. Knight blocks g-file (x=6 = king's destination).
        Square[][] squares = emptyBoard();
        King king = new King(white);
        Rook kingsideRook = new Rook(white);
        Knight blocker = new Knight(white);
        King blackKing = new King(black);
        squares[0][4].setPiece(king);
        squares[0][7].setPiece(kingsideRook);
        squares[0][6].setPiece(blocker); // blocks g-file (king destination)
        squares[7][4].setPiece(blackKing);
        Board board = new Board(squares);

        Chess960Ruleset ruleset = new Chess960Ruleset(518);
        List<Square> legal = ruleset.getLegalSquares(board.getSquare(0, 4), board, new ArrayList<>(), white, black);

        assertFalse(legal.stream().anyMatch(s -> s.getY() == 0 && s.getX() == 7), "Kingside castling must be illegal when g-file (x=6, king destination) is blocked");
    }

    @Test
    void queensideCastle_blockedByPieceOnRookDestination_dFile_isIllegal() {
        // King at x=4, queenside rook at x=0. Knight blocks d-file (x=3 = rook's destination).
        Square[][] squares = emptyBoard();
        King king = new King(white);
        Rook queensideRook = new Rook(white);
        Knight blocker = new Knight(white);
        King blackKing = new King(black);
        squares[0][4].setPiece(king);
        squares[0][0].setPiece(queensideRook);
        squares[0][3].setPiece(blocker); // blocks d-file (rook destination)
        squares[7][4].setPiece(blackKing);
        Board board = new Board(squares);

        Chess960Ruleset ruleset = new Chess960Ruleset(518);
        List<Square> legal = ruleset.getLegalSquares(board.getSquare(0, 4), board, new ArrayList<>(), white, black);

        assertFalse(legal.stream().anyMatch(s -> s.getY() == 0 && s.getX() == 0), "Queenside castling must be illegal when d-file (x=3, rook destination) is blocked");
    }

    @Test
    void kingsideCastle_blockedByPieceBetweenKingAndRook_isIllegal() {
        // King at x=4, rook at x=7. A piece blocks the path at x=5 (between king and rook).
        Square[][] squares = emptyBoard();
        King king = new King(white);
        Rook kingsideRook = new Rook(white);
        Bishop blocker = new Bishop(white);
        King blackKing = new King(black);
        squares[0][4].setPiece(king);
        squares[0][7].setPiece(kingsideRook);
        squares[0][5].setPiece(blocker); // between king and rook
        squares[7][4].setPiece(blackKing);
        Board board = new Board(squares);

        Chess960Ruleset ruleset = new Chess960Ruleset(518);
        List<Square> legal = ruleset.getLegalSquares(board.getSquare(0, 4), board, new ArrayList<>(), white, black);

        assertFalse(legal.stream().anyMatch(s -> s.getY() == 0 && s.getX() == 7), "Kingside castling must be illegal when path between king and rook is blocked");
    }

    // =========================================================================
    // T08-5: Castle valid when corridor is clear but king/rook not at standard positions.
    //
    // King at x=4, kingside rook at x=6 (non-standard), queenside rook at x=1 (non-standard).
    // Corridors are clear. Both castling moves must be legal.
    // =========================================================================

    @Test
    void castling_nonStandardKingAtFile5_kingsideRookAtFile7_legalWhenCorridorClear() {
        // King at x=5 (non-standard position, not e-file), kingside rook at x=7 (h-file).
        // x=6 is empty → corridor from king (5) to rook (7) is clear.
        // rookOriginFile=7, kingDestFile=6 → no swap (7 != 6).
        Square[][] squares = emptyBoard();
        King king = new King(white);
        Rook kingsideRook = new Rook(white);
        King blackKing = new King(black);
        squares[0][5].setPiece(king);
        squares[0][7].setPiece(kingsideRook);
        squares[7][7].setPiece(blackKing);
        Board board = new Board(squares);

        Chess960Ruleset ruleset = new Chess960Ruleset(518);
        List<Square> legal = ruleset.getLegalSquares(board.getSquare(0, 5), board, new ArrayList<>(), white, black);

        assertTrue(legal.stream().anyMatch(s -> s.getY() == 0 && s.getX() == 7), "King at non-standard file 5 with rook at h-file must have rook's square as legal castling target");
    }

    @Test
    void castling_nonStandardQueensideRookAtFile1_legalWhenCorridorClear() {
        // King at x=4, queenside rook at x=1. Squares x=2,3 are empty.
        // Post-castle: king to x=2, rook to x=3. rookOriginFile=1 ≠ kingDestFile=2 → no swap.
        Square[][] squares = emptyBoard();
        King king = new King(white);
        Rook queensideRook = new Rook(white);
        King blackKing = new King(black);
        squares[0][4].setPiece(king);
        squares[0][1].setPiece(queensideRook);
        squares[7][7].setPiece(blackKing);
        Board board = new Board(squares);

        Chess960Ruleset ruleset = new Chess960Ruleset(518);
        List<Square> legal = ruleset.getLegalSquares(board.getSquare(0, 4), board, new ArrayList<>(), white, black);

        assertTrue(legal.stream().anyMatch(s -> s.getY() == 0 && s.getX() == 1), "Non-standard queenside rook at file 1 must be a legal castling target when corridor is clear");
    }

    @Test
    void castling_nonStandardPositions_boardStateCorrect_afterExecution() {
        // King at x=4, kingside rook at x=5 (rookOriginFile=5 ≠ kingDestFile=6, no swap).
        // After castle: king at x=6 (g-file), rook at x=5 (f-file).
        Square[][] squares = emptyBoard();
        King king = new King(white);
        Rook kingsideRook = new Rook(white);
        King blackKing = new King(black);
        squares[0][4].setPiece(king);
        squares[0][5].setPiece(kingsideRook);
        squares[7][7].setPiece(blackKing);
        Board board = new Board(squares);

        board.executeMove(new CastleMove(board.getSquare(0, 4), board.getSquare(0, 5),
                /*rookOriginFile=*/ 5, /*kingDestFile=*/ 6));

        assertInstanceOf(King.class, pieceAt(board, 0, 6), "King must be on g-file (x=6) after kingside castle with rook at x=5");
        assertInstanceOf(Rook.class, pieceAt(board, 0, 5), "Rook must be on f-file (x=5) after kingside castle with rook at x=5");
        assertNull(pieceAt(board, 0, 4), "King's original square (x=4) must be empty");
    }

    // =========================================================================
    // T08-6: En passant still works in a Chess960 game (rule parity).
    //
    // Chess960 inherits all pawn rules from StandardChessRuleset.
    // Verifies that the Chess960 ruleset does not break en passant.
    // =========================================================================

    @Test
    void enPassant_worksInChess960Ruleset() {
        // Setup: white pawn at (y=4, x=4), black pawn just double-pushed to (y=4, x=3).
        // En passant capture: white pawn at (4,4) captures at (5,3).
        // After: white pawn at (5,3), black pawn at (4,3) removed.
        Square[][] squares = emptyBoard();
        Pawn whitePawn = new Pawn(white);
        Pawn blackPawn = new Pawn(black);
        King whiteKing = new King(white);
        King blackKing = new King(black);
        squares[4][4].setPiece(whitePawn);
        squares[4][3].setPiece(blackPawn);  // just double-pushed
        squares[0][7].setPiece(whiteKing);
        squares[7][7].setPiece(blackKing);
        Board board = new Board(squares);

        // The last move was black's double pawn push: d7-d5 (y=6,x=3 → y=4,x=3)
        List<Move> moveHistory = new ArrayList<>();
        moveHistory.add(new Move(new Square(6, 3), new Square(4, 3)));

        Chess960Ruleset ruleset = new Chess960Ruleset(518);
        List<Square> pawnLegal = ruleset.getLegalSquares(board.getSquare(4, 4), board, moveHistory, white, black);

        assertTrue(pawnLegal.stream().anyMatch(s -> s.getY() == 5 && s.getX() == 3), "En passant capture square (y=5, x=3) must be legal in a Chess960 game");

        // Execute en passant
        board.executeMove(new Move(board.getSquare(4, 4), board.getSquare(5, 3)));

        assertInstanceOf(Pawn.class, pieceAt(board, 5, 3), "White pawn must be at (y=5, x=3) after en passant capture");
        assertNull(pieceAt(board, 4, 3), "Captured black pawn must be removed from (y=4, x=3) after en passant");
        assertNull(pieceAt(board, 4, 4), "White pawn's original square must be empty after en passant");
    }

    // =========================================================================
    // T08-7: Check detection — a move that leaves the king in check is illegal.
    //
    // Tests that Chess960Ruleset.getLegalSquares correctly filters moves that
    // would leave the moving player's king in check.
    // =========================================================================

    @Test
    void checkDetection_pinnedPiece_cannotMoveLaterally() {
        // White king at (0,4), white rook at (0,2) pinned by black rook at (0,0).
        // Moving the white rook off rank 0 would expose the king to the black rook.
        Square[][] squares = emptyBoard();
        King whiteKing = new King(white);
        Rook whiteRook = new Rook(white);
        Rook blackRook = new Rook(black);
        King blackKing = new King(black);
        squares[0][4].setPiece(whiteKing);
        squares[0][2].setPiece(whiteRook);  // pinned
        squares[0][0].setPiece(blackRook);  // pinning
        squares[7][7].setPiece(blackKing);
        Board board = new Board(squares);

        Chess960Ruleset ruleset = new Chess960Ruleset(518);
        List<Square> rookLegal = ruleset.getLegalSquares(board.getSquare(0, 2), board, new ArrayList<>(), white, black);

        // Pinned rook must not be able to move to rank 2 (would expose king)
        boolean hasOffRankMove = rookLegal.stream().anyMatch(s -> s.getY() != 0);
        assertFalse(hasOffRankMove, "Pinned rook must not be allowed to move off rank 0 (would expose king to check)");
    }

    @Test
    void checkDetection_movingKingIntoCheck_isIllegal() {
        // White king at (0,4). Black rook at (0,7) attacks everything east of king on rank 0.
        // King moving east to (0,5) or (0,6) would walk into check — illegal.
        Square[][] squares = emptyBoard();
        King whiteKing = new King(white);
        Rook blackRook = new Rook(black);
        King blackKing = new King(black);
        squares[0][4].setPiece(whiteKing);
        squares[0][7].setPiece(blackRook);
        squares[7][4].setPiece(blackKing);
        Board board = new Board(squares);

        Chess960Ruleset ruleset = new Chess960Ruleset(518);
        List<Square> kingLegal = ruleset.getLegalSquares(board.getSquare(0, 4), board, new ArrayList<>(), white, black);

        assertFalse(kingLegal.stream().anyMatch(s -> s.getY() == 0 && s.getX() == 5), "King must not be able to move to (0,5) — attacked by black rook on rank 0");
        assertFalse(kingLegal.stream().anyMatch(s -> s.getY() == 0 && s.getX() == 6), "King must not be able to move to (0,6) — attacked by black rook on rank 0");
        assertTrue(kingLegal.stream().anyMatch(s -> s.getY() == 1), "King must be able to move to rank 1 (safe from black rook)");
    }

    @Test
    void castling_illegal_whenKingIsCurrentlyInCheck() {
        // White king at (0,4), white rook at (0,7). Black rook attacks king on rank 0.
        // Castling must be refused when king is in check.
        Square[][] squares = emptyBoard();
        King whiteKing = new King(white);
        Rook whiteRook = new Rook(white);
        Rook blackRook = new Rook(black);
        King blackKing = new King(black);
        squares[0][4].setPiece(whiteKing);
        squares[0][7].setPiece(whiteRook);
        squares[0][0].setPiece(blackRook); // gives check
        squares[7][7].setPiece(blackKing);
        Board board = new Board(squares);

        Chess960Ruleset ruleset = new Chess960Ruleset(518);
        List<Square> kingLegal = ruleset.getLegalSquares(board.getSquare(0, 4), board, new ArrayList<>(), white, black);

        assertFalse(kingLegal.stream().anyMatch(s -> s.getY() == 0 && s.getX() == 7), "Castling must be illegal when king is currently in check");
    }

    // =========================================================================
    // T08-8: getLegalSquares includes the rook's square as castling target (end-to-end).
    //
    // This is the deferred T07 finding: the existing getLegalSquares test only asserted
    // board dimensions. These tests verify the complete Chess960Ruleset.getLegalSquares
    // pipeline, including PossibleChess960KingMoves and transit-square checks.
    // =========================================================================

    @Test
    void getLegalSquares_kingsideRookAtFile7_isIncluded_endToEnd() {
        // Standard-ish position: king at x=4, kingside rook at x=7, nothing between them.
        // getLegalSquares must return (0,7) — the rook's actual file, not (0,6).
        Square[][] squares = emptyBoard();
        King king = new King(white);
        Rook kingsideRook = new Rook(white);
        King blackKing = new King(black);
        squares[0][4].setPiece(king);
        squares[0][7].setPiece(kingsideRook);
        squares[7][4].setPiece(blackKing);
        Board board = new Board(squares);

        Chess960Ruleset ruleset = new Chess960Ruleset(518);
        List<Square> legal = ruleset.getLegalSquares(board.getSquare(0, 4), board, new ArrayList<>(), white, black);

        assertTrue(legal.stream().anyMatch(s -> s.getY() == 0 && s.getX() == 7), "getLegalSquares must include rook's square (x=7) as castling target in Chess960");
        assertFalse(legal.stream().anyMatch(s -> s.getY() == 0 && s.getX() == 6), "getLegalSquares must NOT include the standard g-file (x=6) as a separate target — " + "Chess960 uses king-to-rook encoding, not 2-square delta");
    }

    @Test
    void getLegalSquares_queensideRookAtFile0_isIncluded_endToEnd() {
        // King at x=4, queenside rook at x=0. Nothing between them.
        // getLegalSquares must return (0,0) — the rook's file, not (0,2).
        Square[][] squares = emptyBoard();
        King king = new King(white);
        Rook queensideRook = new Rook(white);
        King blackKing = new King(black);
        squares[0][4].setPiece(king);
        squares[0][0].setPiece(queensideRook);
        squares[7][4].setPiece(blackKing);
        Board board = new Board(squares);

        Chess960Ruleset ruleset = new Chess960Ruleset(518);
        List<Square> legal = ruleset.getLegalSquares(board.getSquare(0, 4), board, new ArrayList<>(), white, black);

        assertTrue(legal.stream().anyMatch(s -> s.getY() == 0 && s.getX() == 0), "getLegalSquares must include rook's square (x=0) as queenside castling target in Chess960");
    }

    @Test
    void getLegalSquares_kingsideRookAtFile5_isIncluded_endToEnd_t03DeferredScenario() {
        // T03 deferred finding: tests explicitly require a scenario with rookOriginFile=5.
        // King at x=3, kingside rook at x=5. getLegalSquares must return (0,5).
        Square[][] squares = emptyBoard();
        King king = new King(white);
        Rook kingsideRook = new Rook(white);
        King blackKing = new King(black);
        squares[0][3].setPiece(king);
        squares[0][5].setPiece(kingsideRook);
        squares[7][4].setPiece(blackKing);
        Board board = new Board(squares);

        Chess960Ruleset ruleset = new Chess960Ruleset(518);
        List<Square> legal = ruleset.getLegalSquares(board.getSquare(0, 3), board, new ArrayList<>(), white, black);

        assertTrue(legal.stream().anyMatch(s -> s.getY() == 0 && s.getX() == 5), "getLegalSquares must include file 5 (rookOriginFile=5) as castling target — T03 deferred scenario");
    }

    @Test
    void getLegalSquares_movedRook_cannotCastle() {
        // King at x=4, rook at x=7 but rook has already moved.
        Square[][] squares = emptyBoard();
        King king = new King(white);
        Rook movedRook = new Rook(white);
        movedRook.setHasMoved();
        King blackKing = new King(black);
        squares[0][4].setPiece(king);
        squares[0][7].setPiece(movedRook);
        squares[7][4].setPiece(blackKing);
        Board board = new Board(squares);

        Chess960Ruleset ruleset = new Chess960Ruleset(518);
        List<Square> legal = ruleset.getLegalSquares(board.getSquare(0, 4), board, new ArrayList<>(), white, black);

        assertFalse(legal.stream().anyMatch(s -> s.getY() == 0 && s.getX() == 7), "Castling must be illegal when the rook has already moved");
    }

    // =========================================================================
    // T08-8b: Castling through check — king transit square is attacked.
    //
    // Plan scenario 5: king at file 4 (e1), rook at file 7 (h1).
    // Enemy rook controls file 5 (f1) — one of the king's transit squares to g1.
    // Castling must be illegal because the king would pass through f1 under attack.
    // =========================================================================

    @Test
    void castling_illegal_whenKingTransitSquareIsAttacked_kingsideThroughFile5() {
        // White king at (0,4), white rook at (0,7). Black rook at (1,5) controls f1 (x=5),
        // which is the king's transit square on the way to g1 (x=6).
        // The rook's square (x=7) must NOT appear in getLegalSquares.
        Square[][] squares = emptyBoard();
        King whiteKing = new King(white);
        Rook whiteRook = new Rook(white);
        Rook blackRook = new Rook(black);
        King blackKing = new King(black);
        squares[0][4].setPiece(whiteKing);
        squares[0][7].setPiece(whiteRook);
        squares[1][5].setPiece(blackRook); // controls f1 (file 5, rank 0)
        squares[7][4].setPiece(blackKing);
        Board board = new Board(squares);

        Chess960Ruleset ruleset = new Chess960Ruleset(518);
        List<Square> kingLegal = ruleset.getLegalSquares(board.getSquare(0, 4), board, new ArrayList<>(), white, black);

        assertFalse(kingLegal.stream().anyMatch(s -> s.getY() == 0 && s.getX() == 7), "Kingside castling must be illegal when the king's transit square (f1, x=5) " + "is controlled by an enemy rook — isKingTransitAttacked must fire");
    }

    @Test
    void getLegalSquares_movedKing_cannotCastle() {
        // King at x=4 but king has moved. Rook at x=7 is unmoved.
        Square[][] squares = emptyBoard();
        King movedKing = new King(white);
        movedKing.setHasMoved();
        Rook kingsideRook = new Rook(white);
        King blackKing = new King(black);
        squares[0][4].setPiece(movedKing);
        squares[0][7].setPiece(kingsideRook);
        squares[7][4].setPiece(blackKing);
        Board board = new Board(squares);

        Chess960Ruleset ruleset = new Chess960Ruleset(518);
        List<Square> legal = ruleset.getLegalSquares(board.getSquare(0, 4), board, new ArrayList<>(), white, black);

        assertFalse(legal.stream().anyMatch(s -> s.getY() == 0 && s.getX() == 7), "Castling must be illegal when the king has already moved");
    }

    // =========================================================================
    // Full pipeline: Chess960Ruleset → PossibleChess960KingMoves → Board.handleCastleMove960
    // via Game.movePiece (uses isCastlingMove + buildCastleMove dispatch).
    //
    // Uses Chess960Ruleset(518) = position 518 = standard RNBQKBNR for deterministic layout.
    // Uses Game.createServerGame(Ruleset, ...) to inject the specific ruleset.
    // =========================================================================

    @Test
    void fullPipeline_chess960Position518_kingsideCastle_viaGameMovePiece() throws IllegalMoveException {
        // Position 518 = RNBQKBNR (standard back rank).
        // Use createServerGame to inject Chess960Ruleset(518) deterministically.
        Chess960Ruleset ruleset518 = new Chess960Ruleset(518);
        Game game = Game.createServerGame(ruleset518, "W", "B");
        game.startGame();

        // Clear the kingside path: 1. e4 e5  2. Nf3 Nc6  3. Bc4 Bc5
        // Standard-chess moves that clear f1 (bishop) and g1 (knight) in position 518.
        game.movePiece(new Square(1, 4), new Square(3, 4)); // e2-e4
        game.movePiece(new Square(6, 4), new Square(4, 4)); // e7-e5
        game.movePiece(new Square(0, 6), new Square(2, 5)); // Ng1-f3
        game.movePiece(new Square(7, 1), new Square(5, 2)); // Nb8-c6
        game.movePiece(new Square(0, 5), new Square(3, 2)); // Bf1-c4
        game.movePiece(new Square(7, 5), new Square(4, 2)); // Bf8-c5

        // In Chess960, the king's castling target is the ROOK'S square (x=7), not g-file (x=6)
        List<Square> kingLegal = game.getLegalSquares(new Square(0, 4));
        assertTrue(kingLegal.stream().anyMatch(s -> s.getY() == 0 && s.getX() == 7), "In Chess960, king's castling target must be the rook's square (x=7)");

        // Execute the castling move: king moves to the rook's square (x=7)
        game.movePiece(new Square(0, 4), new Square(0, 7));

        // After the castle: king on g1 (x=6), rook on f1 (x=5)
        assertInstanceOf(King.class, game.getPieceAt(new Square(0, 6)), "King must be on g1 (x=6) after Chess960 kingside castle");
        assertInstanceOf(Rook.class, game.getPieceAt(new Square(0, 5)), "Rook must be on f1 (x=5) after Chess960 kingside castle");
        assertNull(game.getPieceAt(new Square(0, 7)), "h1 (x=7) must be empty after Chess960 kingside castle");
        assertNull(game.getPieceAt(new Square(0, 4)), "King's original square e1 (x=4) must be empty after castle");
    }

    @Test
    void fullPipeline_chess960Position518_queensideCastle_viaGameMovePiece() throws IllegalMoveException {
        Chess960Ruleset ruleset518 = new Chess960Ruleset(518);
        Game game = Game.createServerGame(ruleset518, "W", "B");
        game.startGame();

        // Clear the queenside path: 1. d4 d5  2. Nc3 Nc6  3. Bf4 Bf5  4. Qd3 Qd6
        game.movePiece(new Square(1, 3), new Square(3, 3)); // d2-d4
        game.movePiece(new Square(6, 3), new Square(4, 3)); // d7-d5
        game.movePiece(new Square(0, 1), new Square(2, 2)); // Nb1-c3
        game.movePiece(new Square(7, 1), new Square(5, 2)); // Nb8-c6
        game.movePiece(new Square(0, 2), new Square(3, 5)); // Bc1-f4
        game.movePiece(new Square(7, 2), new Square(4, 5)); // Bc8-f5
        game.movePiece(new Square(0, 3), new Square(2, 3)); // Qd1-d3
        game.movePiece(new Square(7, 3), new Square(5, 3)); // Qd8-d6

        // In Chess960, queenside castling target is the rook's square (x=0)
        List<Square> kingLegal = game.getLegalSquares(new Square(0, 4));
        assertTrue(kingLegal.stream().anyMatch(s -> s.getY() == 0 && s.getX() == 0), "In Chess960, king's queenside castling target must be the rook's square (x=0)");

        // Execute: king moves to the rook's square (x=0)
        game.movePiece(new Square(0, 4), new Square(0, 0));

        // After castle: king on c1 (x=2), rook on d1 (x=3)
        assertInstanceOf(King.class, game.getPieceAt(new Square(0, 2)), "King must be on c1 (x=2) after Chess960 queenside castle");
        assertInstanceOf(Rook.class, game.getPieceAt(new Square(0, 3)), "Rook must be on d1 (x=3) after Chess960 queenside castle");
        assertNull(game.getPieceAt(new Square(0, 0)), "a1 (x=0) must be empty after Chess960 queenside castle");
        assertNull(game.getPieceAt(new Square(0, 4)), "King's original square e1 (x=4) must be empty after castle");
    }
}
