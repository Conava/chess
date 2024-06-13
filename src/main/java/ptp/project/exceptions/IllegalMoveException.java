package ptp.project.exceptions;

import ptp.project.logic.moves.Move;

public class IllegalMoveException extends Exception{
    public IllegalMoveException(Move move) {
        super();
    }
}
