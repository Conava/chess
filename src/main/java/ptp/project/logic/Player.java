package ptp.project.logic;

public class Player {
    private final String name;
    private final String color;

    public Player(String name, String color) {
        this.name = name;
        this.color = color;
    }

    String getName() {
        return name;
    }

    String getColor() {
        return color;
    }
}
