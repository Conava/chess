package io.github.conava.chess.core.data.pieces;

import io.github.conava.chess.core.data.player.Player;
import io.github.conava.chess.core.data.player.PlayerColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the contract for {@code Piece.copy()}.
 * <p>
 * Rules under test:
 * <ul>
 *   <li>Every concrete subclass must return a non-null copy that is a distinct object.</li>
 *   <li>The copy's {@code getPlayer()} must be the same {@link Player} instance as the original.</li>
 *   <li>The copy's {@code getType()} must equal the original's type.</li>
 *   <li>{@link King#copy()} and {@link Rook#copy()} must carry over the {@code hasMoved} boolean.</li>
 * </ul>
 */
class PieceCopyTest {

    private final Player white = new Player("White", PlayerColor.WHITE);
    private final Player black = new Player("Black", PlayerColor.BLACK);

    // ---- Bishop ----

    @Test
    void bishop_copy_isNotSameInstance() {
        Bishop original = new Bishop(white);
        assertNotSame(original, original.copy());
    }

    @Test
    void bishop_copy_preservesPlayer() {
        Bishop original = new Bishop(white);
        assertSame(white, original.copy().getPlayer());
    }

    @Test
    void bishop_copy_preservesType() {
        Bishop original = new Bishop(black);
        assertEquals(Pieces.BISHOP, original.copy().getType());
    }

    // ---- Knight ----

    @Test
    void knight_copy_isNotSameInstance() {
        Knight original = new Knight(white);
        assertNotSame(original, original.copy());
    }

    @Test
    void knight_copy_preservesPlayer() {
        Knight original = new Knight(white);
        assertSame(white, original.copy().getPlayer());
    }

    @Test
    void knight_copy_preservesType() {
        Knight original = new Knight(black);
        assertEquals(Pieces.KNIGHT, original.copy().getType());
    }

    // ---- Queen ----

    @Test
    void queen_copy_isNotSameInstance() {
        Queen original = new Queen(white);
        assertNotSame(original, original.copy());
    }

    @Test
    void queen_copy_preservesPlayer() {
        Queen original = new Queen(white);
        assertSame(white, original.copy().getPlayer());
    }

    @Test
    void queen_copy_preservesType() {
        Queen original = new Queen(black);
        assertEquals(Pieces.QUEEN, original.copy().getType());
    }

    // ---- Pawn ----

    @Test
    void pawn_copy_isNotSameInstance() {
        Pawn original = new Pawn(white);
        assertNotSame(original, original.copy());
    }

    @Test
    void pawn_copy_preservesPlayer() {
        Pawn original = new Pawn(white);
        assertSame(white, original.copy().getPlayer());
    }

    @Test
    void pawn_copy_preservesType() {
        Pawn original = new Pawn(black);
        assertEquals(Pieces.PAWN, original.copy().getType());
    }

    // ---- King (must preserve hasMoved) ----

    @Test
    void king_copy_isNotSameInstance() {
        King original = new King(white);
        assertNotSame(original, original.copy());
    }

    @Test
    void king_copy_preservesPlayer() {
        King original = new King(white);
        assertSame(white, original.copy().getPlayer());
    }

    @Test
    void king_copy_preservesType() {
        King original = new King(black);
        assertEquals(Pieces.KING, original.copy().getType());
    }

    @Test
    void king_copy_preservesHasMoved_false() {
        King original = new King(white);
        King copied = (King) original.copy();
        assertFalse(copied.getHasMoved());
    }

    @Test
    void king_copy_preservesHasMoved_true() {
        King original = new King(white);
        original.setHasMoved();
        King copied = (King) original.copy();
        assertTrue(copied.getHasMoved());
    }

    // ---- Rook (must preserve hasMoved) ----

    @Test
    void rook_copy_isNotSameInstance() {
        Rook original = new Rook(white);
        assertNotSame(original, original.copy());
    }

    @Test
    void rook_copy_preservesPlayer() {
        Rook original = new Rook(white);
        assertSame(white, original.copy().getPlayer());
    }

    @Test
    void rook_copy_preservesType() {
        Rook original = new Rook(black);
        assertEquals(Pieces.ROOK, original.copy().getType());
    }

    @Test
    void rook_copy_preservesHasNotMoved_whenNotMoved() {
        Rook original = new Rook(white);
        Rook copied = (Rook) original.copy();
        assertTrue(copied.getHasNotMoved(), "copy of unmoved rook must also report not-moved");
    }

    @Test
    void rook_copy_preservesHasMoved_whenMoved() {
        Rook original = new Rook(white);
        original.setHasMoved();
        Rook copied = (Rook) original.copy();
        assertFalse(copied.getHasNotMoved(), "copy of moved rook must also report moved");
    }
}
