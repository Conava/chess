package io.github.conava.chess.core.exceptions;

import io.github.conava.chess.core.logic.moves.Move;

public class IllegalMoveException extends Exception{
    public IllegalMoveException(Move move) {
        super();
    }
}
