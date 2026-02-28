package io.github.conava.chess.core.data.pieces;

import io.github.conava.chess.core.data.player.Player;
import io.github.conava.chess.core.data.player.PlayerColor;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that the Swing/AWT removal (Task 1) is in effect:
 * none of the concrete Piece subclasses expose a getIcon() method,
 * and that constructors set only the player field (no icon-related state).
 */
class PieceConstructorTest {

    private final Player whitePlayer = new Player("White", PlayerColor.WHITE);
    private final Player blackPlayer = new Player("Black", PlayerColor.BLACK);

    // ---- no getIcon() method exists on any piece class ----

    @Test
    void bishop_hasNoGetIconMethod() {
        assertFalse(hasMethod(new Bishop(whitePlayer)),
                "Bishop must not expose getIcon() — Swing removed from core");
    }

    @Test
    void king_hasNoGetIconMethod() {
        assertFalse(hasMethod(new King(whitePlayer)),
                "King must not expose getIcon() — Swing removed from core");
    }

    @Test
    void knight_hasNoGetIconMethod() {
        assertFalse(hasMethod(new Knight(whitePlayer)),
                "Knight must not expose getIcon() — Swing removed from core");
    }

    @Test
    void pawn_hasNoGetIconMethod() {
        assertFalse(hasMethod(new Pawn(whitePlayer)),
                "Pawn must not expose getIcon() — Swing removed from core");
    }

    @Test
    void queen_hasNoGetIconMethod() {
        assertFalse(hasMethod(new Queen(whitePlayer)),
                "Queen must not expose getIcon() — Swing removed from core");
    }

    @Test
    void rook_hasNoGetIconMethod() {
        assertFalse(hasMethod(new Rook(whitePlayer)),
                "Rook must not expose getIcon() — Swing removed from core");
    }

    // ---- constructors still wire the player correctly ----

    @Test
    void bishop_returnsCorrectPlayer() {
        assertEquals(whitePlayer, new Bishop(whitePlayer).getPlayer());
        assertEquals(blackPlayer, new Bishop(blackPlayer).getPlayer());
    }

    @Test
    void king_returnsCorrectPlayer() {
        assertEquals(whitePlayer, new King(whitePlayer).getPlayer());
        assertEquals(blackPlayer, new King(blackPlayer).getPlayer());
    }

    @Test
    void knight_returnsCorrectPlayer() {
        assertEquals(whitePlayer, new Knight(whitePlayer).getPlayer());
        assertEquals(blackPlayer, new Knight(blackPlayer).getPlayer());
    }

    @Test
    void pawn_returnsCorrectPlayer() {
        assertEquals(whitePlayer, new Pawn(whitePlayer).getPlayer());
        assertEquals(blackPlayer, new Pawn(blackPlayer).getPlayer());
    }

    @Test
    void queen_returnsCorrectPlayer() {
        assertEquals(whitePlayer, new Queen(whitePlayer).getPlayer());
        assertEquals(blackPlayer, new Queen(blackPlayer).getPlayer());
    }

    @Test
    void rook_returnsCorrectPlayer() {
        assertEquals(whitePlayer, new Rook(whitePlayer).getPlayer());
        assertEquals(blackPlayer, new Rook(blackPlayer).getPlayer());
    }

    // ---- getType() returns the correct enum value ----

    @Test
    void bishop_getTypeReturnsBishop() {
        assertEquals(Pieces.BISHOP, new Bishop(whitePlayer).getType());
    }

    @Test
    void king_getTypeReturnsKing() {
        assertEquals(Pieces.KING, new King(whitePlayer).getType());
    }

    @Test
    void knight_getTypeReturnsKnight() {
        assertEquals(Pieces.KNIGHT, new Knight(whitePlayer).getType());
    }

    @Test
    void pawn_getTypeReturnsPawn() {
        assertEquals(Pieces.PAWN, new Pawn(whitePlayer).getType());
    }

    @Test
    void queen_getTypeReturnsQueen() {
        assertEquals(Pieces.QUEEN, new Queen(whitePlayer).getType());
    }

    @Test
    void rook_getTypeReturnsRook() {
        assertEquals(Pieces.ROOK, new Rook(whitePlayer).getType());
    }

    // ---- helper ----

    private boolean hasMethod(Object obj) {
        for (Method m : obj.getClass().getMethods()) {
            if (m.getName().equals("getIcon")) {
                return true;
            }
        }
        return false;
    }
}
