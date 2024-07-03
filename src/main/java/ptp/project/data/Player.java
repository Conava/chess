package ptp.project.data;

import ptp.project.data.enums.PlayerColor;

public class Player {
    private String name;
    private final PlayerColor color;

    /**
     * Initiate a new player.
     * @param name Name of the player.
     * @param color Enum PlayerColor of the player.
     */
    public Player(String name, PlayerColor color) {
        this.name = name;
        this.color = color;
    }

    /**
     *  Gets name of the player.
     * @return String of the name of the player.
     */
    public String getName() {
        return name;
    }

    /**
     * Changes the name.
     * @param name New name for the player.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the color of the player.
     * @return Enum PlayerColor of the player.
     */
    public PlayerColor getColor() {
        return color;
    }
}
