package ptp.project.logic;

import ptp.project.logic.pieces.Piece;

public class Square {
    private int x;
    private int y;
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

    public boolean isOccupiedByPlayer(Player player) {
        return piece != null && piece.getPlayer() == player;
    }

}
