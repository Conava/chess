package io.github.conava.chess.core.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.github.conava.chess.core.data.board.Board;
import io.github.conava.chess.core.data.player.PlayerColor;
import io.github.conava.chess.core.data.pieces.King;
import io.github.conava.chess.core.data.pieces.Knight;
import io.github.conava.chess.core.data.pieces.Pawn;
import io.github.conava.chess.core.data.pieces.Rook;
import io.github.conava.chess.core.data.player.Player;
import io.github.conava.chess.core.logic.moves.CastleMove;
import io.github.conava.chess.core.logic.moves.Move;

import static org.junit.jupiter.api.Assertions.*;

public class BoardTest {
    private Board board;
    private Player player1;

    @BeforeEach
    public void setUp() {
        player1 = new Player("p1", PlayerColor.WHITE);
        Square[][] boardSquares = new Square[8][8];

        // Initialize each Square in the array
        for (int i = 0; i < boardSquares.length; i++) {
            for (int j = 0; j < boardSquares[i].length; j++) {
                boardSquares[i][j] = new Square(i, j);
            }
        }

        // Now you can safely set a piece on a Square
        boardSquares[4][1].setPiece(new Knight(player1));

        board = new Board(boardSquares);
    }

    @Test
    void testBoard() {
        // Board must expose the knight placed in setUp at (y=4, x=1)
        assertNotNull(board.getSquare(4, 1).getPiece(), "Square (4,1) must contain the Knight placed during setUp");
        assertInstanceOf(Knight.class, board.getSquare(4, 1).getPiece(), "Piece at (4,1) must be a Knight");
        // All other squares in this row must be empty
        assertNull(board.getSquare(4, 0).getPiece(), "Square (4,0) must be empty");
    }

    // ---- Deep copy tests ----

    private static Square[][] emptyBoard() {
        Square[][] squares = new Square[8][8];
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                squares[y][x] = new Square(y, x);
            }
        }
        return squares;
    }

    /**
     * getCopy() must allocate new Square instances — not reuse the originals.
     */
    @Test
    void getCopy_doesNotShareSquareInstances() {
        Square[][] squares = emptyBoard();
        squares[3][3].setPiece(new Knight(player1));
        Board original = new Board(squares);

        Board copy = original.getCopy();

        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                assertNotSame(original.getSquare(y, x), copy.getSquare(y, x), "Square at [" + y + "][" + x + "] must be a new instance in the copy");
            }
        }
    }

    /**
     * getCopy() must allocate new Piece instances — not share piece references.
     */
    @Test
    void getCopy_doesNotSharePieceInstances() {
        Square[][] squares = emptyBoard();
        Knight knight = new Knight(player1);
        squares[3][3].setPiece(knight);
        Board original = new Board(squares);

        Board copy = original.getCopy();

        assertNotSame(knight, copy.getSquare(3, 3).getPiece(), "Piece in the copy must be a distinct instance from the original");
    }

    /**
     * Mutating a square's piece in the copy must not affect the original.
     */
    @Test
    void getCopy_mutatingCopyDoesNotAffectOriginal() {
        Square[][] squares = emptyBoard();
        squares[3][3].setPiece(new Knight(player1));
        Board original = new Board(squares);

        Board copy = original.getCopy();
        copy.getSquare(3, 3).setPiece(null);

        assertNotNull(original.getSquare(3, 3).getPiece(), "Original square must still have its piece after the copy's square is cleared");
    }

    /**
     * King.hasMoved == true must be preserved in the copy.
     */
    @Test
    void getCopy_preservesKingHasMoved() {
        Player white = new Player("White", PlayerColor.WHITE);
        Square[][] squares = emptyBoard();
        King king = new King(white);
        king.setHasMoved();
        squares[0][4].setPiece(king);
        Board original = new Board(squares);

        Board copy = original.getCopy();

        King copiedKing = (King) copy.getSquare(0, 4).getPiece();
        assertTrue(copiedKing.getHasMoved(), "King.hasMoved must be true in the copied board");
    }

    /**
     * Rook.hasMoved == false must be preserved in the copy (rook not yet moved).
     */
    @Test
    void getCopy_preservesRookHasNotMoved() {
        Player white = new Player("White", PlayerColor.WHITE);
        Square[][] squares = emptyBoard();
        Rook rook = new Rook(white);
        // hasMoved defaults to false — do not call setHasMoved
        squares[0][0].setPiece(rook);
        Board original = new Board(squares);

        Board copy = original.getCopy();

        Rook copiedRook = (Rook) copy.getSquare(0, 0).getPiece();
        assertTrue(copiedRook.getHasNotMoved(), "Rook.hasMoved must be false in the copied board when the original rook has not moved");
    }

    /**
     * Rook.hasMoved == true must be preserved in the copy.
     */
    @Test
    void getCopy_preservesRookHasMoved() {
        Player white = new Player("White", PlayerColor.WHITE);
        Square[][] squares = emptyBoard();
        Rook rook = new Rook(white);
        rook.setHasMoved();
        squares[0][0].setPiece(rook);
        Board original = new Board(squares);

        Board copy = original.getCopy();

        Rook copiedRook = (Rook) copy.getSquare(0, 0).getPiece();
        assertFalse(copiedRook.getHasNotMoved(), "Rook.hasMoved must be true in the copied board when the original rook has moved");
    }

    /**
     * En passant scenario: white pawn on e5 (y=4, x=4), black pawn on d5 (y=4, x=3).
     * White executes the en passant capture by moving diagonally to d6 (y=5, x=3),
     * which is currently an empty square. After execution the black pawn at d5 must
     * be removed from the board and white pawn must be at d6.
     */
    @Test
    void enPassantCaptureRemovesOpponentPawn() {
        Player white = new Player("White", PlayerColor.WHITE);
        Player black = new Player("Black", PlayerColor.BLACK);

        Square[][] squares = new Square[8][8];
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                squares[y][x] = new Square(y, x);
            }
        }

        // White pawn on e5: y=4, x=4
        Pawn whitePawn = new Pawn(white);
        squares[4][4].setPiece(whitePawn);

        // Black pawn on d5: y=4, x=3
        Pawn blackPawn = new Pawn(black);
        squares[4][3].setPiece(blackPawn);

        // d6 (y=5, x=3) is empty — en passant destination
        Board enPassantBoard = new Board(squares);

        // Execute the diagonal pawn move from e5 to d6
        Move enPassantMove = new Move(squares[4][4], squares[5][3]);
        enPassantBoard.executeMove(enPassantMove);

        // White pawn must now be on d6
        assertNotNull(enPassantBoard.getSquare(5, 3).getPiece(), "White pawn should be at d6 after en passant");
        assertInstanceOf(Pawn.class, enPassantBoard.getSquare(5, 3).getPiece(), "Piece at d6 should be a Pawn");
        assertEquals(white, enPassantBoard.getSquare(5, 3).getPiece().getPlayer(), "Piece at d6 should belong to white");

        // Black pawn must have been removed from d5
        assertNull(enPassantBoard.getSquare(4, 3).getPiece(), "Black pawn should have been removed from d5 by en passant");

        // e5 must be empty
        assertNull(enPassantBoard.getSquare(4, 4).getPiece(), "e5 should be empty after white pawn moved away");
    }

    // ---- Chess960 castling tests (T03) ----

    private static Square[][] emptyBoardStatic() {
        Square[][] squares = new Square[8][8];
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                squares[y][x] = new Square(y, x);
            }
        }
        return squares;
    }

    /**
     * Chess960 kingside castle: king on b1 (x=1), rook on g1 (x=6).
     * After castling, king must be on g-file (x=6) and rook on f-file (x=5).
     * rookOriginFile=6, kingDestFile=6 → kingside path.
     */
    @Test
    void chess960KingsideCastle_placesKingOnGFileAndRookOnFFile() {
        Player white = new Player("White", PlayerColor.WHITE);
        Square[][] squares = emptyBoardStatic();

        King king = new King(white);
        Rook rook = new Rook(white);
        squares[0][1].setPiece(king);   // king on b1 (x=1)
        squares[0][6].setPiece(rook);   // rook on g1 (x=6)

        Board board = new Board(squares);

        // Chess960 castle: king "moves to" the rook's square (g1), rookOriginFile=6, kingDestFile=6
        CastleMove castleMove = new CastleMove(squares[0][1], squares[0][6], 6, 6);
        board.executeMove(castleMove);

        // King must be on g1 (x=6)
        assertNotNull(board.getSquare(0, 6).getPiece(), "King must be on g-file (x=6) after kingside castle");
        assertInstanceOf(King.class, board.getSquare(0, 6).getPiece(), "Piece on g-file must be the King");

        // Rook must be on f1 (x=5)
        assertNotNull(board.getSquare(0, 5).getPiece(), "Rook must be on f-file (x=5) after kingside castle");
        assertInstanceOf(Rook.class, board.getSquare(0, 5).getPiece(), "Piece on f-file must be the Rook");

        // Original king square (b1) must be empty
        assertNull(board.getSquare(0, 1).getPiece(), "b1 must be empty after king castled away");
    }

    /**
     * Chess960 queenside castle: king on e1 (x=4), rook on b1 (x=1).
     * After castling, king must be on c-file (x=2) and rook on d-file (x=3).
     * rookOriginFile=1, kingDestFile=2 → queenside path (kingDestFile < kingStartFile).
     */
    @Test
    void chess960QueensideCastle_placesKingOnCFileAndRookOnDFile() {
        Player white = new Player("White", PlayerColor.WHITE);
        Square[][] squares = emptyBoardStatic();

        King king = new King(white);
        Rook rook = new Rook(white);
        squares[0][4].setPiece(king);   // king on e1 (x=4)
        squares[0][1].setPiece(rook);   // rook on b1 (x=1)

        Board board = new Board(squares);

        // Chess960 castle: king "moves to" the rook's square (b1), rookOriginFile=1, kingDestFile=2
        CastleMove castleMove = new CastleMove(squares[0][4], squares[0][1], 1, 2);
        board.executeMove(castleMove);

        // King must be on c1 (x=2)
        assertNotNull(board.getSquare(0, 2).getPiece(), "King must be on c-file (x=2) after queenside castle");
        assertInstanceOf(King.class, board.getSquare(0, 2).getPiece(), "Piece on c-file must be the King");

        // Rook must be on d1 (x=3)
        assertNotNull(board.getSquare(0, 3).getPiece(), "Rook must be on d-file (x=3) after queenside castle");
        assertInstanceOf(Rook.class, board.getSquare(0, 3).getPiece(), "Piece on d-file must be the Rook");

        // Original king square (e1) must be empty
        assertNull(board.getSquare(0, 4).getPiece(), "e1 must be empty after king castled away");
    }

    /**
     * Chess960 castle where king and rook destination squares overlap with their origins.
     * King on c1 (x=2), rook on d1 (x=3). Queenside castle: king moves to c1, rook to d1.
     * This is a "no-op" position but the board must remain consistent.
     * kingDestFile=2, rookOriginFile=3 → queenside (kingDestFile < kingStart? No, king IS on x=2).
     * Actually test the case where rookOriginFile > kingStartFile (kingside) with
     * king on f1 (x=5), rook on g1 (x=6), resulting in king on g1 (x=6) and rook on f1 (x=5) — a swap.
     */
    @Test
    void chess960KingsideCastle_kingAndRookSwap_correctResult() {
        Player white = new Player("White", PlayerColor.WHITE);
        Square[][] squares = emptyBoardStatic();

        King king = new King(white);
        Rook rook = new Rook(white);
        squares[0][5].setPiece(king);   // king on f1 (x=5)
        squares[0][6].setPiece(rook);   // rook on g1 (x=6)

        Board board = new Board(squares);

        // Chess960 kingside castle: rookOriginFile=6, kingDestFile=6
        CastleMove castleMove = new CastleMove(squares[0][5], squares[0][6], 6, 6);
        board.executeMove(castleMove);

        // King must be on g1 (x=6)
        assertNotNull(board.getSquare(0, 6).getPiece(), "King must be on g-file after kingside castle");
        assertInstanceOf(King.class, board.getSquare(0, 6).getPiece());

        // Rook must be on f1 (x=5)
        assertNotNull(board.getSquare(0, 5).getPiece(), "Rook must be on f-file after kingside castle");
        assertInstanceOf(Rook.class, board.getSquare(0, 5).getPiece());
    }

    /**
     * Chess960 kingside castle: king on e1 (x=4), rook on f1 (x=5).
     * rookOriginFile=5, kingDestFile=6: king moves to g1 (x=6), rook moves to f1 (x=5).
     * This tests the scenario where the rook's origin is adjacent to the king's destination.
     */
    @Test
    void chess960KingsideCastle_rookOriginFile5_kingDestFile6() {
        Player white = new Player("White", PlayerColor.WHITE);
        Square[][] squares = emptyBoardStatic();

        King king = new King(white);
        Rook rook = new Rook(white);
        squares[0][4].setPiece(king);   // king on e1 (x=4)
        squares[0][5].setPiece(rook);   // rook on f1 (x=5)

        Board board = new Board(squares);

        // Chess960 castle: king "moves to" the rook's square (f1), rookOriginFile=5, kingDestFile=6
        CastleMove castleMove = new CastleMove(squares[0][4], squares[0][5], 5, 6);
        board.executeMove(castleMove);

        // King must be on g1 (x=6)
        assertNotNull(board.getSquare(0, 6).getPiece(), "King must be on g-file (x=6) after kingside castle");
        assertInstanceOf(King.class, board.getSquare(0, 6).getPiece(), "Piece on g-file must be the King");

        // Rook must be on f1 (x=5)
        assertNotNull(board.getSquare(0, 5).getPiece(), "Rook must be on f-file (x=5) after kingside castle");
        assertInstanceOf(Rook.class, board.getSquare(0, 5).getPiece(), "Piece on f-file must be the Rook");

        // Original king square (e1) must be empty
        assertNull(board.getSquare(0, 4).getPiece(), "e1 must be empty after king castled away");
    }

    /**
     * Standard castle move (rookOriginFile == -1) must still work correctly via the
     * existing hardcoded path — backward compatibility check.
     * King on e1 (x=4) castles kingside: king moves to g1 (x=6), rook from h1 (x=7) to f1 (x=5).
     */
    @Test
    void standardCastle_backwardCompatible_kingsideStillWorks() {
        Player white = new Player("White", PlayerColor.WHITE);
        Square[][] squares = emptyBoardStatic();

        King king = new King(white);
        Rook rook = new Rook(white);
        squares[0][4].setPiece(king);   // king on e1 (x=4)
        squares[0][7].setPiece(rook);   // rook on h1 (x=7)

        Board board = new Board(squares);

        // Standard castle move — rookOriginFile defaults to -1
        CastleMove castleMove = new CastleMove(squares[0][4], squares[0][6]);
        board.executeMove(castleMove);

        // King on g1 (x=6)
        assertNotNull(board.getSquare(0, 6).getPiece(), "King must be on g-file after standard kingside castle");
        assertInstanceOf(King.class, board.getSquare(0, 6).getPiece());

        // Rook on f1 (x=5)
        assertNotNull(board.getSquare(0, 5).getPiece(), "Rook must be on f-file after standard kingside castle");
        assertInstanceOf(Rook.class, board.getSquare(0, 5).getPiece());
    }

}
