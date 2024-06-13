package ptp.project.logic.moves;

import ptp.project.logic.Square;

public class Move {
    private final Square start;
    private final Square end;

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
}//hier fehlt die rochade und die umwandlung an information
