package io.github.conava.chess.core.logic.game;

import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.pieces.*;
import io.github.conava.chess.core.data.player.Player;
import io.github.conava.chess.core.data.player.PlayerColor;
import io.github.conava.chess.core.exceptions.IllegalMoveException;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Game.getNewPiece() after the Task 2 fix:
 * the method now uses a switch on the Pieces enum instead of
 * stale Class.forName() reflection that silently returned null.
 * <p>
 * getNewPiece() is private; it is exercised via the public promoteMove() API.
 * We set up a controlled board position — white pawn at a7 (y=6, x=0),
 * destination a8 (y=7, x=0) cleared — so that promoteMove() succeeds and we
 * can inspect the resulting piece on the board.
 * <p>
 * Board coordinate convention used by Game / Board:
 * board[y][x]:  y=0 is white's back rank, y=7 is black's back rank.
 * Square(y, x) is the constructor used throughout the production code.
 */
class GetNewPieceTest {

    /**
     * Test-only subclass of OfflineGame that exposes the protected 'board'
     * field so individual squares can be arranged for promotion tests.
     */
    static class PromotionTestGame extends OfflineGame {
        PromotionTestGame() {
            super(RulesetOptions.STANDARD, "White", "Black");
        }

        /**
         * Clears a square directly on the underlying board.
         * Needed to create a path for promotion without playing a full game.
         */
        void clearSquare(int y, int x) {
            board.getSquare(y, x).setPiece(null);
        }

        /**
         * Places a piece directly on the underlying board.
         */
        void placePiece(int y, int x, Piece piece) {
            board.getSquare(y, x).setPiece(piece);
        }
    }

    private PromotionTestGame game;
    private Player whitePlayer;

    @BeforeEach
    void setUp() {
        game = new PromotionTestGame();
        game.startGame();

        // Retrieve the white player that Game created internally
        whitePlayer = game.getPlayerWhite();

        // Clear black's back row (y=7) and second row (y=6) at column 0
        // so we can place a white pawn ready to promote.
        game.clearSquare(7, 0); // clear a8 (destination)
        game.clearSquare(6, 0); // clear a7 (where black pawn was)

        // Place white pawn at a7 (y=6, x=0)
        game.placePiece(6, 0, new Pawn(whitePlayer));

        // We also need to advance the turn counter to black's turn first,
        // then back to white's turn. Actually the game starts with white to move
        // (turnCount=0), so after any number of white/black pairs, it's still
        // white's turn. We are at turnCount=0 → white to move. Good.
    }

    @Test
    void promoteToQueen_pieceOnBoardIsQueen() throws IllegalMoveException {
        game.promoteMove(new Square(6, 0), new Square(7, 0), Pieces.QUEEN);
        Piece result = game.getPieceAt(new Square(7, 0));
        assertInstanceOf(Queen.class, result, "After promoting to QUEEN, piece at a8 must be a Queen");
    }

    @Test
    void promoteToRook_pieceOnBoardIsRook() throws IllegalMoveException {
        game.promoteMove(new Square(6, 0), new Square(7, 0), Pieces.ROOK);
        Piece result = game.getPieceAt(new Square(7, 0));
        assertInstanceOf(Rook.class, result, "After promoting to ROOK, piece at a8 must be a Rook");
    }

    @Test
    void promoteToBishop_pieceOnBoardIsBishop() throws IllegalMoveException {
        game.promoteMove(new Square(6, 0), new Square(7, 0), Pieces.BISHOP);
        Piece result = game.getPieceAt(new Square(7, 0));
        assertInstanceOf(Bishop.class, result, "After promoting to BISHOP, piece at a8 must be a Bishop");
    }

    @Test
    void promoteToKnight_pieceOnBoardIsKnight() throws IllegalMoveException {
        game.promoteMove(new Square(6, 0), new Square(7, 0), Pieces.KNIGHT);
        Piece result = game.getPieceAt(new Square(7, 0));
        assertInstanceOf(Knight.class, result, "After promoting to KNIGHT, piece at a8 must be a Knight");
    }

    @Test
    void promotedPiece_belongsToWhitePlayer() throws IllegalMoveException {
        game.promoteMove(new Square(6, 0), new Square(7, 0), Pieces.QUEEN);
        Piece result = game.getPieceAt(new Square(7, 0));
        assertEquals(PlayerColor.WHITE, result.getPlayer().color(), "Promoted piece must belong to the white player");
    }

    @Test
    void promoteToQueen_turnAdvances() throws IllegalMoveException {
        Player beforeMove = game.getCurrentPlayer();
        game.promoteMove(new Square(6, 0), new Square(7, 0), Pieces.QUEEN);
        Player afterMove = game.getCurrentPlayer();

        assertNotEquals(beforeMove, afterMove, "After a promotion move the active player must switch");
    }

    @Test
    void promoteToKing_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> game.promoteMove(new Square(6, 0), new Square(7, 0), Pieces.KING),
                "KING is not a valid promotion target; promoteMove must throw IllegalArgumentException");
    }

    @Test
    void promoteToPawn_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> game.promoteMove(new Square(6, 0), new Square(7, 0), Pieces.PAWN),
                "PAWN is not a valid promotion target; promoteMove must throw IllegalArgumentException");
    }
}
