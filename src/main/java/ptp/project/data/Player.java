package ptp.project.data;

import ptp.project.data.enums.PlayerColor;

public class Player {
    private String name;
    private final PlayerColor color;

    public Player(String name, PlayerColor color) {
        this.name = name;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PlayerColor getColor() {
        return color;
    }
}
