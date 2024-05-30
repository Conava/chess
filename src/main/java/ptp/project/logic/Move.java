package ptp.project.logic;

public class Move {
    private final Square start;
    private final Square end;
    private Piece piece;

    public Move(Square start, Square end) {
        this.start = start;
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
