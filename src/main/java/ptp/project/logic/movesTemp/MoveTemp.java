package ptp.project.logic.movesTemp;

import ptp.project.logic.SquareTemp;

public class MoveTemp {
    private final SquareTemp start;
    private final SquareTemp end;

    public MoveTemp(SquareTemp start, SquareTemp end) {
        this.start = start;
        this.end = end;
    }

    public SquareTemp getStart() {
        return start;
    }

    public SquareTemp getEnd() {
        return end;
    }
}
