package ptp.project.logic;

import ptp.project.logic.piecesTemp.PieceTemp;

public class SquareTemp {
    private final int x;
    private final int y;
    private PieceTemp pieceTemp;

    public SquareTemp(int y, int x) {
        this.x = x;
        this.y = y;
        this.pieceTemp = null;
    }

    public SquareTemp(int x, int y, PieceTemp pieceTemp) {
        this.x = x;
        this.y = y;
        this.pieceTemp = pieceTemp;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public PieceTemp getPiece() {
        return pieceTemp;
    }

    public void setPiece(PieceTemp pieceTemp) {
        this.pieceTemp = pieceTemp;
    }

    public boolean isEmpty() {
        return pieceTemp == null;
    }

    /**
     * Checks what occupies the square
     * @return Returns null if unoccupied
     *         Returns Player if occupied
     */
    public PlayerTemp isOccupiedBy() {
        if(this.getPiece() == null) return null;
        return this.getPiece().getPlayer();
    }
}
