package ptp.project.logic;

public class PlayerTemp {
    private final String name;
    private final String color;

    public PlayerTemp(String name, String color) {
        this.name = name;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public String getEnemyColor() {
        if (color.equals("white")) {
            return "black";
        } else {
            return "white";
        }
    }
}
