package ptp.project.logic;

public class Square {
    private int x;
    private int y;
    private Piece piece;

    public Square(int x, int y) {
        this.x = x;
        this.y = y;
        this.piece = null;
    }

    public Square(int x, int y, Piece piece) {
        this.x = x;
        this.y = y;
        this.piece = piece;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public String getCoordinates() {
        return "(" + x + ", " + y + ")";
    }

    public Piece getPiece() {
        return piece;
    }

    public void setPiece(Piece piece) {
        this.piece = piece;
    }

    public boolean isEmpty() {
        return piece == null;
    }

    public boolean isOccupied() {
        return piece != null;
    }

    public boolean isOccupiedByPlayer(Player player) {
        return piece != null && piece.getPlayer() == player;
    }

    /**
     * Returns 0 if the square is empty, 1 if the square is occupied by the player, and 2 if the square is occupied by the opponent.
     * @param player
     * @return 0 if the square is empty, 1 if the square is occupied by the player, and 2 if the square is occupied by the opponent.
     */
    public int isOccupiedBy(Player player) {
        if (piece != null && piece.getPlayer() == player) {
            return 1;
        } else if (piece != null && piece.getPlayer() != player) {
            return 2;
        } else {
            return 0;
        }
    }
}
