package ptp.project.logic;

public class Move {
    private final Square start;
    private final Square end;
    private Piece piece;

    public Move(Piece piece, Square end) {
        this.piece = piece;
        this.start = piece.getPosition();
        this.end = end;
    }

    public Square getStart() {
        return start;
    }

    public Square getEnd() {
        return end;
    }

    public Piece getPiece() {
        return piece;
    }
}
