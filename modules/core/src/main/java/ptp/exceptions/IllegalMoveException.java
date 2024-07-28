package ptp.exceptions;

import ptp.logic.moves.Move;

public class IllegalMoveException extends Exception{
    public IllegalMoveException(Move move) {
        super();
    }
}
