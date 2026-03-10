package io.github.conava.chess.core.logic.ruleset.chess960Ruleset;

import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.pieces.*;
import io.github.conava.chess.core.data.player.Player;
import io.github.conava.chess.core.data.player.PlayerColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Chess960StartPosition}.
 *
 * <p>Covers:
 * <ul>
 *   <li>Constraint validation for random generation (bishops on opposite colors, king between rooks)</li>
 *   <li>Known position: index 518 = standard chess RNBQKBNR</li>
 *   <li>Round-trip codec: {@code toIndex(fromIndex(i)) == i} for all 0–959</li>
 *   <li>Error handling for out-of-range indices</li>
 *   <li>Random generation produces valid positions across many iterations</li>
 * </ul>
 */
class Chess960StartPositionTest {

    private Player white;
    private Player black;

    @BeforeEach
    void setUp() {
        white = new Player("White", PlayerColor.WHITE);
        black = new Player("Black", PlayerColor.BLACK);
    }

    // -------------------------------------------------------------------------
    // Helper: extract white's back rank (row 0) as Piece[]
    // -------------------------------------------------------------------------

    private Piece[] backRank(Square[][] board) {
        Piece[] rank = new Piece[8];
        for (int x = 0; x < 8; x++) {
            rank[x] = board[0][x].getPiece();
        }
        return rank;
    }

    // -------------------------------------------------------------------------
    // Single-generation constraint tests
    // -------------------------------------------------------------------------

    @Test
    void generate_producesEightPiecesOnBackRank() {
        Square[][] board = Chess960StartPosition.generate(white, black);
        Piece[] rank = backRank(board);
        for (int x = 0; x < 8; x++) {
            assertNotNull(rank[x], "Back rank file " + x + " must not be null");
        }
    }

    @Test
    void generate_exactPieceTypeCounts() {
        Square[][] board = Chess960StartPosition.generate(white, black);
        Piece[] rank = backRank(board);

        int kings = 0, queens = 0, rooks = 0, bishops = 0, knights = 0;
        for (Piece p : rank) {
            if (p instanceof King) kings++;
            else if (p instanceof Queen) queens++;
            else if (p instanceof Rook) rooks++;
            else if (p instanceof Bishop) bishops++;
            else if (p instanceof Knight) knights++;
        }

        assertEquals(1, kings,   "Must have exactly 1 king");
        assertEquals(1, queens,  "Must have exactly 1 queen");
        assertEquals(2, rooks,   "Must have exactly 2 rooks");
        assertEquals(2, bishops, "Must have exactly 2 bishops");
        assertEquals(2, knights, "Must have exactly 2 knights");
    }

    @Test
    void generate_bishopsOnOppositeColors() {
        Square[][] board = Chess960StartPosition.generate(white, black);
        Piece[] rank = backRank(board);

        int bishop1File = -1, bishop2File = -1;
        for (int x = 0; x < 8; x++) {
            if (rank[x] instanceof Bishop) {
                if (bishop1File == -1) bishop1File = x;
                else bishop2File = x;
            }
        }

        assertNotEquals(-1, bishop1File, "Must find first bishop");
        assertNotEquals(-1, bishop2File, "Must find second bishop");
        // Opposite colors: one on even file (dark), one on odd file (light)
        assertNotEquals(bishop1File % 2, bishop2File % 2,
                "Bishops must be on opposite color squares");
    }

    @Test
    void generate_kingBetweenRooks() {
        Square[][] board = Chess960StartPosition.generate(white, black);
        Piece[] rank = backRank(board);

        int kingFile = -1;
        int rookLeft = -1, rookRight = -1;
        for (int x = 0; x < 8; x++) {
            if (rank[x] instanceof King) kingFile = x;
        }
        for (int x = 0; x < 8; x++) {
            if (rank[x] instanceof Rook) {
                if (x < kingFile) rookLeft = x;
                else rookRight = x;
            }
        }

        assertNotEquals(-1, kingFile, "Must find king");
        assertNotEquals(-1, rookLeft,  "Must find rook to the left of king");
        assertNotEquals(-1, rookRight, "Must find rook to the right of king");
        assertTrue(rookLeft < kingFile, "Left rook must be left of king");
        assertTrue(rookRight > kingFile, "Right rook must be right of king");
    }

    @Test
    void generate_piecesHaveCorrectPlayerOwnership() {
        Square[][] board = Chess960StartPosition.generate(white, black);
        for (int x = 0; x < 8; x++) {
            assertEquals(white, board[0][x].getPiece().getPlayer(),
                    "White back rank pieces must belong to white player");
            assertEquals(black, board[7][x].getPiece().getPlayer(),
                    "Black back rank pieces must belong to black player");
        }
    }

    @Test
    void generate_pawnsOnRows1And6() {
        Square[][] board = Chess960StartPosition.generate(white, black);
        for (int x = 0; x < 8; x++) {
            assertNotNull(board[1][x].getPiece(), "Row 1 must have a pawn");
            assertInstanceOf(Pawn.class, board[1][x].getPiece(), "Row 1 must be pawns");
            assertNotNull(board[6][x].getPiece(), "Row 6 must have a pawn");
            assertInstanceOf(Pawn.class, board[6][x].getPiece(), "Row 6 must be pawns");
        }
    }

    @Test
    void generate_middleRowsAreEmpty() {
        Square[][] board = Chess960StartPosition.generate(white, black);
        for (int y = 2; y < 6; y++) {
            for (int x = 0; x < 8; x++) {
                assertNull(board[y][x].getPiece(),
                        "Row " + y + " file " + x + " must be empty");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Known position test
    // -------------------------------------------------------------------------

    @Test
    void fromIndex_518_isStandardChessPosition() {
        Square[][] board = Chess960StartPosition.fromIndex(518, white, black);
        Piece[] rank = backRank(board);

        assertInstanceOf(Rook.class,   rank[0], "File a must be Rook");
        assertInstanceOf(Knight.class, rank[1], "File b must be Knight");
        assertInstanceOf(Bishop.class, rank[2], "File c must be Bishop");
        assertInstanceOf(Queen.class,  rank[3], "File d must be Queen");
        assertInstanceOf(King.class,   rank[4], "File e must be King");
        assertInstanceOf(Bishop.class, rank[5], "File f must be Bishop");
        assertInstanceOf(Knight.class, rank[6], "File g must be Knight");
        assertInstanceOf(Rook.class,   rank[7], "File h must be Rook");
    }

    // -------------------------------------------------------------------------
    // Round-trip codec
    // -------------------------------------------------------------------------

    @Test
    void roundTrip_computeIndex_fromIndex_allPositions() {
        for (int i = 0; i < 960; i++) {
            Square[][] board = Chess960StartPosition.fromIndex(i, white, black);
            Piece[] rank = backRank(board);
            int computed = Chess960StartPosition.computeIndex(rank);
            assertEquals(i, computed,
                    "Round-trip failed for index " + i);
        }
    }

    // -------------------------------------------------------------------------
    // Constraint validation across many random generations
    // -------------------------------------------------------------------------

    @Test
    void generate_1000Times_constraintsAlwaysHold() {
        for (int iter = 0; iter < 1000; iter++) {
            Square[][] board = Chess960StartPosition.generate(white, black);
            Piece[] rank = backRank(board);

            // All 8 slots filled
            for (int x = 0; x < 8; x++) {
                assertNotNull(rank[x], "Iteration " + iter + ": file " + x + " must not be null");
            }

            // Bishops on opposite colors
            int b1 = -1, b2 = -1;
            for (int x = 0; x < 8; x++) {
                if (rank[x] instanceof Bishop) {
                    if (b1 == -1) b1 = x;
                    else b2 = x;
                }
            }
            assertNotEquals(b1 % 2, b2 % 2,
                    "Iteration " + iter + ": bishops must be on opposite color squares");

            // King between rooks
            int kf = -1;
            for (int x = 0; x < 8; x++) {
                if (rank[x] instanceof King) kf = x;
            }
            int rl = -1, rr = -1;
            for (int x = 0; x < 8; x++) {
                if (rank[x] instanceof Rook) {
                    if (x < kf) rl = x;
                    else rr = x;
                }
            }
            assertTrue(rl != -1 && rr != -1 && rl < kf && rr > kf,
                    "Iteration " + iter + ": king must be between rooks");
        }
    }

    // -------------------------------------------------------------------------
    // Error handling
    // -------------------------------------------------------------------------

    @Test
    void fromIndex_negativeIndex_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> Chess960StartPosition.fromIndex(-1, white, black));
    }

    @Test
    void fromIndex_index960_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> Chess960StartPosition.fromIndex(960, white, black));
    }

    @Test
    void fromIndex_index959_doesNotThrow() {
        assertDoesNotThrow(() -> Chess960StartPosition.fromIndex(959, white, black));
    }

    @Test
    void fromIndex_index0_doesNotThrow() {
        assertDoesNotThrow(() -> Chess960StartPosition.fromIndex(0, white, black));
    }

    // -------------------------------------------------------------------------
    // computeIndex range check
    // -------------------------------------------------------------------------

    @Test
    void computeIndex_returnsValueInValidRange() {
        Square[][] board = Chess960StartPosition.generate(white, black);
        Piece[] rank = backRank(board);
        int index = Chess960StartPosition.computeIndex(rank);
        assertTrue(index >= 0 && index <= 959,
                "computeIndex must return a value in [0, 959], got: " + index);
    }
}
