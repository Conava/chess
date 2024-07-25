package ptp.data;

import ptp.data.enums.PlayerColor;

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
        assertEquals("player1", player1.name());
        assertEquals("player2", player2.name());
        assertEquals("player3", player3.name());
    }

    @Test
    void testGetColor() {
        assertEquals(PlayerColor.WHITE, player1.color());
        assertEquals(PlayerColor.BLACK, player2.color());
        assertEquals(PlayerColor.WHITE, player3.color());

        assertNotEquals(PlayerColor.BLACK, player1.color());
        assertNotEquals(PlayerColor.WHITE, player2.color());
        assertNotEquals(PlayerColor.BLACK, player3.color());
    }
}
