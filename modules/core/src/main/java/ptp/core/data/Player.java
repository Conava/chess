package ptp.core.data;

import ptp.core.data.enums.PlayerColor;

public record Player(String name, PlayerColor color) {
    /**
     * Initiate a new player.
     *
     * @param name  Name of the player.
     * @param color Enum PlayerColor of the player.
     */
    public Player {
    }

    /**
     * Gets name of the player.
     *
     * @return String of the name of the player.
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * Returns the color of the player.
     *
     * @return Enum PlayerColor of the player.
     */
    @Override
    public PlayerColor color() {
        return color;
    }
}
