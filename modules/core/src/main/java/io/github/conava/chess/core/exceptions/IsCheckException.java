package io.github.conava.chess.core.exceptions;

import io.github.conava.chess.core.data.Square;

public class IsCheckException extends Exception {
    public IsCheckException(Square source) {
        super();
    }
}
