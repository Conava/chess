package ptp.project.data;

import ptp.project.data.pieces.Piece;

public class Square {
    private final int x;
    private final int y;
    private Piece piece;

    public Square(int y, int x) {
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

    public Piece getPiece() {
        return piece;
    }

    public void setPiece(Piece piece) {
        this.piece = piece;
    }

    public boolean isEmpty() {
        return piece == null;
    }

    /**
     * Checks what occupies the square
     * @return Returns null if unoccupied
     *         Returns Player if occupied
     */
    public Player isOccupiedBy() {
        if(this.getPiece() == null) return null;
        return this.getPiece().getPlayer();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof Square square)) return false;
        return square.getX() == this.getX() && square.getY() == this.getY();
    }
}
