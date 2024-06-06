package ptp.project.logic;


//white is player 1, black is player 2
public class Player {
    private final String name;
    private final String color;

    public Player(String name, String color) {
        this.name = name;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }
}
