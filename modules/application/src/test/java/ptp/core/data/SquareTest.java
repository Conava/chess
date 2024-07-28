package ptp.core.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ptp.core.data.enums.PlayerColor;
import ptp.core.data.pieces.Bishop;
import ptp.core.data.pieces.Pawn;
import ptp.core.data.pieces.Rook;

import static org.junit.jupiter.api.Assertions.*;

public class SquareTest {
    Square square1;
    Square square2;
    Square square3;
    Player player1;
    Player player2;

    @BeforeEach
    void setUp() {
        player1 = new Player("p1", PlayerColor.WHITE);
        player2 = new Player("p2", PlayerColor.BLACK);
        square1 = new Square(0, 0);
        square2 = new Square(0, 7, new Pawn(player1));
        square3 = new Square(5, 3, new Bishop(player2));
    }

    @Test
    void testGetX() {
        assertEquals(0, square1.getX());
        assertEquals(7, square2.getX());
        assertEquals(3, square3.getX());

        assertEquals(5, square1.getX() + 5);
        assertEquals(5, square2.getX() - 2);
        assertEquals(5, square3.getX() + 2);
    }

    @Test
    void testGetY() {
        assertEquals(0, square1.getY());
        assertEquals(0, square2.getY());
        assertEquals(5, square3.getY());

        assertEquals(3, square1.getY() + 3);
        assertEquals(3, square2.getY() + 3);
        assertEquals(3, square3.getY() - 2);
    }

    @Test
    void testGetPiece() {
        assertNull(square1.getPiece());
        assertEquals(Pawn.class, square2.getPiece().getClass());
        assertNotEquals(Rook.class, square2.getPiece().getClass());
        assertEquals(Bishop.class, square3.getPiece().getClass());
        assertNotEquals(Rook.class, square3.getPiece().getClass());

        square1.setPiece(new Rook(player1));
        assertNotNull(square1.getPiece());
        assertEquals(Rook.class, square1.getPiece().getClass());
        assertEquals(player1, square1.getPiece().getPlayer());

        square2.setPiece(new Bishop(player2));
        assertNotNull(square2.getPiece());
        assertEquals(Bishop.class, square2.getPiece().getClass());
        assertEquals(player2, square2.getPiece().getPlayer());
    }

    @Test
    void testSetPiece() {

    }

    @Test
    void testIsEmpty() {

    }

    @Test
    void testIsOccupiedBy() {

    }

    @Test
    void testEquals() {

    }
}
