package io.github.conava.chess.core.logic.ruleset.chess960Ruleset;

import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.board.Board;
import io.github.conava.chess.core.data.pieces.*;
import io.github.conava.chess.core.data.player.Player;
import io.github.conava.chess.core.data.player.PlayerColor;
import io.github.conava.chess.core.logic.moves.CastleMove;
import io.github.conava.chess.core.logic.moves.Move;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Chess960Ruleset}.
 *
 * <p>Covers:
 * <ul>
 *   <li>Random constructor produces a valid Chess960 position (bishops on opposite colors,
 *       king between rooks)</li>
 *   <li>Scharnagl index constructor: index 518 produces standard back rank</li>
 *   <li>{@code getGameLabel()} returns {@code "Chess 960 — Position N"} for known N</li>
 *   <li>{@code getIndex()} returns the stored Scharnagl index</li>
 *   <li>{@code deserializeMove("O-O", board, player)} returns a {@link CastleMove} with
 *       correct {@code rookOriginFile} and {@code kingDestFile}</li>
 *   <li>{@code deserializeMove("O-O-O", board, player)} similarly for queenside</li>
 *   <li>Non-castling wire strings are delegated to {@code Move.fromString}</li>
 * </ul>
 */
class Chess960RulesetTest {

    private Player white;
    private Player black;

    @BeforeEach
    void setUp() {
        white = new Player("White", PlayerColor.WHITE);
        black = new Player("Black", PlayerColor.BLACK);
    }

    // -------------------------------------------------------------------------
    // Random constructor — valid Chess960 position
    // -------------------------------------------------------------------------

    @Test
    void randomConstructor_producesValidPosition_bishopsOnOppositeColors() {
        Chess960Ruleset ruleset = new Chess960Ruleset();
        Square[][] startBoard = ruleset.getStartBoard(white, black);
        Piece[] backRank = new Piece[8];
        for (int x = 0; x < 8; x++) {
            backRank[x] = startBoard[0][x].getPiece();
        }

        int b1 = -1, b2 = -1;
        for (int x = 0; x < 8; x++) {
            if (backRank[x] instanceof Bishop) {
                if (b1 == -1) b1 = x;
                else b2 = x;
            }
        }

        assertNotEquals(-1, b1, "Must find first bishop");
        assertNotEquals(-1, b2, "Must find second bishop");
        assertNotEquals(b1 % 2, b2 % 2, "Bishops must be on opposite color squares");
    }

    @Test
    void randomConstructor_producesValidPosition_kingBetweenRooks() {
        Chess960Ruleset ruleset = new Chess960Ruleset();
        Square[][] startBoard = ruleset.getStartBoard(white, black);
        Piece[] backRank = new Piece[8];
        for (int x = 0; x < 8; x++) {
            backRank[x] = startBoard[0][x].getPiece();
        }

        int kingFile = -1;
        for (int x = 0; x < 8; x++) {
            if (backRank[x] instanceof King) kingFile = x;
        }
        int rookLeft = -1, rookRight = -1;
        for (int x = 0; x < 8; x++) {
            if (backRank[x] instanceof Rook) {
                if (x < kingFile) rookLeft = x;
                else if (x > kingFile) rookRight = x;
            }
        }

        assertNotEquals(-1, kingFile,  "Must find king");
        assertNotEquals(-1, rookLeft,  "Must find rook to the left of king");
        assertNotEquals(-1, rookRight, "Must find rook to the right of king");
        assertTrue(rookLeft < kingFile && rookRight > kingFile,
                "King must be between both rooks");
    }

    @Test
    void randomConstructor_indexIsInValidRange() {
        Chess960Ruleset ruleset = new Chess960Ruleset();
        int index = ruleset.getIndex();
        assertTrue(index >= 0 && index <= 959,
                "Scharnagl index must be in [0, 959], got: " + index);
    }

    // -------------------------------------------------------------------------
    // Scharnagl index constructor
    // -------------------------------------------------------------------------

    @Test
    void indexConstructor_518_producesStandardBackRank() {
        Chess960Ruleset ruleset = new Chess960Ruleset(518);
        Square[][] startBoard = ruleset.getStartBoard(white, black);

        assertInstanceOf(Rook.class,   startBoard[0][0].getPiece(), "File a must be Rook");
        assertInstanceOf(Knight.class, startBoard[0][1].getPiece(), "File b must be Knight");
        assertInstanceOf(Bishop.class, startBoard[0][2].getPiece(), "File c must be Bishop");
        assertInstanceOf(Queen.class,  startBoard[0][3].getPiece(), "File d must be Queen");
        assertInstanceOf(King.class,   startBoard[0][4].getPiece(), "File e must be King");
        assertInstanceOf(Bishop.class, startBoard[0][5].getPiece(), "File f must be Bishop");
        assertInstanceOf(Knight.class, startBoard[0][6].getPiece(), "File g must be Knight");
        assertInstanceOf(Rook.class,   startBoard[0][7].getPiece(), "File h must be Rook");
    }

    @Test
    void indexConstructor_518_indexIs518() {
        Chess960Ruleset ruleset = new Chess960Ruleset(518);
        assertEquals(518, ruleset.getIndex());
    }

    @Test
    void indexConstructor_invalidNegative_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Chess960Ruleset(-1));
    }

    @Test
    void indexConstructor_invalid960_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Chess960Ruleset(960));
    }

    // -------------------------------------------------------------------------
    // getGameLabel
    // -------------------------------------------------------------------------

    @Test
    void getGameLabel_returnsChess960LabelWithIndex() {
        Chess960Ruleset ruleset = new Chess960Ruleset(518);
        assertEquals("Chess 960 \u2014 Position 518", ruleset.getGameLabel());
    }

    @Test
    void getGameLabel_index0_containsZero() {
        Chess960Ruleset ruleset = new Chess960Ruleset(0);
        assertEquals("Chess 960 \u2014 Position 0", ruleset.getGameLabel());
    }

    @Test
    void getGameLabel_index959_contains959() {
        Chess960Ruleset ruleset = new Chess960Ruleset(959);
        assertEquals("Chess 960 \u2014 Position 959", ruleset.getGameLabel());
    }

    // -------------------------------------------------------------------------
    // deserializeMove — "O-O" (kingside castling)
    // -------------------------------------------------------------------------

    /**
     * Builds a board from Chess960 position 518 (standard back rank) so the king is
     * at file 4 and the kingside rook is at file 7.
     * Expects deserializeMove("O-O") to return a CastleMove with rookOriginFile=7, kingDestFile=6.
     */
    @Test
    void deserializeMove_OO_returnsCastleMoveWithCorrectRookOriginAndKingDest_standard() {
        Chess960Ruleset ruleset = new Chess960Ruleset(518); // standard back rank
        Square[][] startSquares = ruleset.getStartBoard(white, black);
        Board board = new Board(startSquares);

        Move move = ruleset.deserializeMove("O-O", board, white);

        assertInstanceOf(CastleMove.class, move, "O-O must return a CastleMove");
        CastleMove castle = (CastleMove) move;
        // King is at file 4 (e-file), kingside rook is at file 7 (h-file)
        assertEquals(7, castle.getRookOriginFile(),
                "Kingside rook is at file 7 for position 518");
        assertEquals(6, castle.getKingDestFile(),
                "King lands on file 6 (g-file) for kingside castling");
    }

    /**
     * Kingside castling with a non-standard Chess960 back rank where the kingside rook
     * is not at h-file. Position 0: BBQNNRKR — king at file 5, kingside rook at file 7.
     *
     * <p>Index 0: n1=0 (light bishop at file 1), n2=0 (dark bishop at file 0),
     * n3=0 (queen at first remaining), n4=0 (knights at positions 0,1 of remaining 5).
     * Remaining after bishops: [2,3,4,5,6,7].
     * Queen at index 0 → file 2. Remaining: [3,4,5,6,7].
     * Knights at positions 0,1 → files 3,4. Remaining: [5,6,7].
     * Rook at 5, King at 6, Rook at 7.
     */
    @Test
    void deserializeMove_OO_chess960NonStandardPosition() {
        // Use a board where we manually place king and rooks in Chess960 positions
        // King at file 1, rooks at files 0 and 2 (a minimal scenario)
        Square[][] squares = buildEmptyBoard();
        // White: king at e1 (y=0, x=1), unmoved rooks at x=0 and x=2
        King king = new King(white);
        Rook queenRook = new Rook(white);
        Rook kingRook = new Rook(white);
        squares[0][1].setPiece(king);
        squares[0][0].setPiece(queenRook);
        squares[0][2].setPiece(kingRook);
        Board board = new Board(squares);

        Chess960Ruleset ruleset = new Chess960Ruleset(518); // index doesn't matter for deserialization
        Move move = ruleset.deserializeMove("O-O", board, white);

        assertInstanceOf(CastleMove.class, move, "O-O must return a CastleMove");
        CastleMove castle = (CastleMove) move;
        // King at file 1, kingside rook is at file 2 (the rook to the right of king)
        assertEquals(2, castle.getRookOriginFile(),
                "Kingside rook is the unmoved rook to the right of king (file 2)");
        assertEquals(6, castle.getKingDestFile(),
                "King always lands on g-file (6) for kingside castling");
    }

    // -------------------------------------------------------------------------
    // deserializeMove — "O-O-O" (queenside castling)
    // -------------------------------------------------------------------------

    @Test
    void deserializeMove_OOO_returnsCastleMoveWithCorrectRookOriginAndKingDest_standard() {
        Chess960Ruleset ruleset = new Chess960Ruleset(518); // standard back rank
        Square[][] startSquares = ruleset.getStartBoard(white, black);
        Board board = new Board(startSquares);

        Move move = ruleset.deserializeMove("O-O-O", board, white);

        assertInstanceOf(CastleMove.class, move, "O-O-O must return a CastleMove");
        CastleMove castle = (CastleMove) move;
        // King is at file 4 (e-file), queenside rook is at file 0 (a-file)
        assertEquals(0, castle.getRookOriginFile(),
                "Queenside rook is at file 0 for position 518");
        assertEquals(2, castle.getKingDestFile(),
                "King lands on file 2 (c-file) for queenside castling");
    }

    @Test
    void deserializeMove_OOO_chess960NonStandardPosition() {
        // King at file 5, rooks at files 3 and 7
        Square[][] squares = buildEmptyBoard();
        King king = new King(white);
        Rook queenRook = new Rook(white);
        Rook kingRook = new Rook(white);
        squares[0][5].setPiece(king);
        squares[0][3].setPiece(queenRook);
        squares[0][7].setPiece(kingRook);
        Board board = new Board(squares);

        Chess960Ruleset ruleset = new Chess960Ruleset(518);
        Move move = ruleset.deserializeMove("O-O-O", board, white);

        assertInstanceOf(CastleMove.class, move, "O-O-O must return a CastleMove");
        CastleMove castle = (CastleMove) move;
        // Queenside rook is at file 3 (to the left of king at file 5)
        assertEquals(3, castle.getRookOriginFile(),
                "Queenside rook is the unmoved rook to the left of king (file 3)");
        assertEquals(2, castle.getKingDestFile(),
                "King always lands on c-file (2) for queenside castling");
    }

    // -------------------------------------------------------------------------
    // deserializeMove — non-castling moves delegated to Move.fromString
    // -------------------------------------------------------------------------

    @Test
    void deserializeMove_regularMove_delegatesToMoveFromString() {
        Chess960Ruleset ruleset = new Chess960Ruleset(518);
        Square[][] startSquares = ruleset.getStartBoard(white, black);
        Board board = new Board(startSquares);

        Move move = ruleset.deserializeMove("e2-e4", board, white);

        assertNotNull(move, "Regular move must not be null");
        assertFalse(move instanceof CastleMove, "Regular move must not be a CastleMove");
        // e2 = y=1, x=4; e4 = y=3, x=4
        assertEquals(1, move.getStart().getY());
        assertEquals(4, move.getStart().getX());
        assertEquals(3, move.getEnd().getY());
        assertEquals(4, move.getEnd().getX());
    }

    @Test
    void deserializeMove_blackKingsideCastle_usesBlackRank() {
        Chess960Ruleset ruleset = new Chess960Ruleset(518); // standard: black king at e8 (y=7, x=4)
        Square[][] startSquares = ruleset.getStartBoard(white, black);
        Board board = new Board(startSquares);

        Move move = ruleset.deserializeMove("O-O", board, black);

        assertInstanceOf(CastleMove.class, move, "O-O for black must return a CastleMove");
        CastleMove castle = (CastleMove) move;
        assertEquals(7, castle.getRookOriginFile(),
                "Black's kingside rook is at file 7 for position 518");
        assertEquals(6, castle.getKingDestFile(),
                "King lands on g-file (6) for kingside castling");
    }

    @Test
    void deserializeMove_blackQueensideCastle_usesBlackRank() {
        Chess960Ruleset ruleset = new Chess960Ruleset(518);
        Square[][] startSquares = ruleset.getStartBoard(white, black);
        Board board = new Board(startSquares);

        Move move = ruleset.deserializeMove("O-O-O", board, black);

        assertInstanceOf(CastleMove.class, move, "O-O-O for black must return a CastleMove");
        CastleMove castle = (CastleMove) move;
        assertEquals(0, castle.getRookOriginFile(),
                "Black's queenside rook is at file 0 for position 518");
        assertEquals(2, castle.getKingDestFile(),
                "King lands on c-file (2) for queenside castling");
    }

    // -------------------------------------------------------------------------
    // isCastlingCandidate — king moves to rook file
    // -------------------------------------------------------------------------

    @Test
    void getLegalSquares_includesRookSquare_forUnmovedRook() {
        // Build a minimal Chess960 board: king at file 4, unmoved rooks at files 0 and 7,
        // no pieces in between, and a lone black king somewhere far away so the board is valid.
        Square[][] squares = buildEmptyBoard();

        King whiteKing = new King(white);
        Rook queenRook = new Rook(white);
        Rook kingRook  = new Rook(white);
        King blackKing = new King(black);

        squares[0][4].setPiece(whiteKing);  // white king at e1 (file 4)
        squares[0][0].setPiece(queenRook);  // queenside rook at a1 (file 0)
        squares[0][7].setPiece(kingRook);   // kingside rook at h1 (file 7)
        squares[7][4].setPiece(blackKing);  // black king far away, no threat

        Board board = new Board(squares);
        Chess960Ruleset ruleset = new Chess960Ruleset(518);
        List<Move> emptyHistory = new ArrayList<>();

        List<Square> legalSquares = ruleset.getLegalSquares(
                board.getSquare(0, 4), board, emptyHistory, white, black);

        // The kingside rook square (file 7) must be a legal castling target
        boolean containsKingsideRook = legalSquares.stream()
                .anyMatch(s -> s.getY() == 0 && s.getX() == 7);
        assertTrue(containsKingsideRook,
                "Kingside rook square (file 7) must be in king's legal squares for castling");

        // The queenside rook square (file 0) must also be a legal castling target
        boolean containsQueensideRook = legalSquares.stream()
                .anyMatch(s -> s.getY() == 0 && s.getX() == 0);
        assertTrue(containsQueensideRook,
                "Queenside rook square (file 0) must be in king's legal squares for castling");

        // A square the king cannot move to (e.g. file 4 itself — the king's own square)
        boolean containsOwnSquare = legalSquares.stream()
                .anyMatch(s -> s.getY() == 0 && s.getX() == 4);
        assertFalse(containsOwnSquare,
                "The king's own square must not be in legal squares");
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private Square[][] buildEmptyBoard() {
        Square[][] squares = new Square[8][8];
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                squares[y][x] = new Square(y, x);
            }
        }
        return squares;
    }
}
