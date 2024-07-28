package ptp.core.exceptions;

import ptp.core.logic.moves.Move;

public class IllegalMoveException extends Exception{
    public IllegalMoveException(Move move) {
        super();
    }
}
