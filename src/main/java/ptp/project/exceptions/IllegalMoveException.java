package ptp.project.exceptions;

import ptp.project.logic.moves.Move;
import ptp.project.logic.movesTemp.MoveTemp;

public class IllegalMoveException extends Exception {
    public IllegalMoveException(Move move) {
        super();
    }
    public IllegalMoveException(MoveTemp moveTemp) {
        super();
    }
}
