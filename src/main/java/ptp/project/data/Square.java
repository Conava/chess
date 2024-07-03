package ptp.project.data;

import ptp.project.data.pieces.Piece;

public class Square {
    private final int x;
    private final int y;
    private Piece piece;

    /**
     * Initiates a square.
     *
     * @param y Column of the square, starting at 0
     * @param x Row of the square, starting at 0
     */
    public Square(int y, int x) {
        this.x = x;
        this.y = y;
        this.piece = null;
    }

    /**
     * Initiates a square.
     *
     * @param y     Column of the square, starting at 0
     * @param x     Row of the square, starting at 0
     * @param piece Piece pre-placed on the square
     */
    public Square(int y, int x, Piece piece) { //this should not be a thing (only the board should know where what piece is)
        this.x = x;
        this.y = y;
        this.piece = piece;
    }

    /**
     * Returns row of the square.
     *
     * @return Int of the row of the square, starts at 0
     */
    public int getX() {
        return x;
    }

    /**
     * Returns column of the square.
     *
     * @return Int of the column of the square, starts at 0
     */
    public int getY() {
        return y;
    }

    /**
     * Returns the piece on the square, if set.
     *
     * @return Returns the piece on the square or null if empty.
     */
    public Piece getPiece() {
        return piece;
    }

    /**
     * Puts a piece on a square.
     *
     * @param piece Piece to be on the square, replaces old one.
     */
    public void setPiece(Piece piece) {
        this.piece = piece;
    }

    /**
     * Checks if square has no piece.
     *
     * @return true if no piece, false if there is a piece on the square.
     */
    public boolean isEmpty() {
        return piece == null;
    }

    /**
     * Checks what occupies the square
     *
     * @return Returns null if unoccupied
     * Returns Player if occupied
     */
    public Player isOccupiedBy() {
        if (this.getPiece() == null) return null;
        return this.getPiece().getPlayer();
    }

    /**
     * Checks for equality of squares.
     *
     * @param obj Object to compare to.
     * @return true, if they have the same row and column, ignores pieces.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof Square square)) return false;
        return square.getX() == this.getX() && square.getY() == this.getY();
    }
}
