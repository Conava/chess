package ptp.project.data;

import ptp.project.data.enums.PlayerColor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class PlayerTest {
    Player player1;
    Player player2;
    Player player3;


    @BeforeEach
    public void setUp() {
        player1 = new Player("player1", PlayerColor.WHITE);
        player2 = new Player("player2", PlayerColor.BLACK);
        player3 = new Player("player3", PlayerColor.WHITE);
    }

    @Test
    void testGetName() {
        assertEquals("player1", player1.getName());
        assertEquals("player2", player2.getName());
        assertEquals("player3", player3.getName());
        player1.setName("NotPlayer1");
        assertEquals("NotPlayer1", player1.getName());
    }

    @Test
    void testSetName() {
        player1.setName("NotPlayer1");
        assertEquals("NotPlayer1", player1.getName());
        assertNotEquals("player1", player1.getName());
        player1.setName("Player1");
        assertEquals("Player1", player1.getName());
        assertNotEquals("NotPlayer1", player1.getName());
        assertNotEquals("player1", player1.getName());
    }

    @Test
    void testGetColor() {
        assertEquals(PlayerColor.WHITE, player1.getColor());
        assertEquals(PlayerColor.BLACK, player2.getColor());
        assertEquals(PlayerColor.WHITE, player3.getColor());

        assertNotEquals(PlayerColor.BLACK, player1.getColor());
        assertNotEquals(PlayerColor.WHITE, player2.getColor());
        assertNotEquals(PlayerColor.BLACK, player3.getColor());
    }
}
