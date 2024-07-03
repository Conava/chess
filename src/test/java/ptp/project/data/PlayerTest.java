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
        assertEquals(player1.getName(), "player1");
        assertEquals(player2.getName(), "player2");
        assertEquals(player3.getName(), "player3");
        player1.setName("NotPlayer1");
        assertEquals(player1.getName(), "NotPlayer1");
    }

    @Test
    void testSetName() {
        player1.setName("NotPlayer1");
        assertEquals(player1.getName(), "NotPlayer1");
        assertNotEquals(player1.getName(), "player1");
        player1.setName("Player1");
        assertEquals(player1.getName(), "Player1");
        assertNotEquals(player1.getName(), "NotPlayer1");
        assertNotEquals(player1.getName(), "player1");
    }

    @Test
    void testGetColor() {
        assertEquals(player1.getColor(), PlayerColor.WHITE);
        assertEquals(player2.getColor(), PlayerColor.BLACK);
        assertEquals(player3.getColor(), PlayerColor.WHITE);

        assertNotEquals(player1.getColor(), PlayerColor.BLACK);
        assertNotEquals(player2.getColor(), PlayerColor.WHITE);
        assertNotEquals(player3.getColor(), PlayerColor.BLACK);
    }
}
