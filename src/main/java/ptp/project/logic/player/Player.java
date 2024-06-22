package ptp.project.logic.player;

public class Player {
    private String name;
    private final int color;

    /**
     * Creates a new player.
     *
     * @param name  Name of the color.
     * @param color Color of the pieces of the player.
     *              #Entries for values of colors: {@link ptp.project.logic.docs}
     */
    public Player(String name, int color) {
        this.name = name;
        this.color = color;
    }

    /**
     * Creates a new player with default name.
     *
     * @param color Color of the pieces of the player.
     *              #Entries for values of colors: {@link ptp.project.logic.docs}
     */
    public Player(int color) {
        this.name = "default";
        this.color = color;
    }

    /**
     * Gets name of the player.
     * @return String of the name.
     */
    public String getName() {
        return name;
    }

    /**
     * Changes the name of the player.
     * @param name New name of the player.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets color of the pieces of the player.
     * @return Returns int corresponding to a color:
     *         #Entries for values of colors: {@link ptp.project.logic.docs}
     */
    public int getColor() {
        return color;
    }

    /**
     * Gets the color of the pieces of the opponent.
     * @return Return int corresponding to color:
     *         #Entries for values of colors: {@link ptp.project.logic.docs}
     */
    public int getOpponentColor() {
        return (color + 1) % 2;
    }
}
