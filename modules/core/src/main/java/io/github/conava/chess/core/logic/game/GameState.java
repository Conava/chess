package io.github.conava.chess.core.logic.game;

public enum GameState {
    NO_GAME("No game found"),
    SERVER_ERROR("Server error"),
    WAITING_FOR_PLAYER("Waiting for player"),
    RUNNING("Game in progress"),
    WHITE_WON_BY_CHECKMATE("White won by checkmate"),
    WHITE_WON_BY_RESIGNATION("White won by resignation"),
    WHITE_WON_BY_TIMEOUT("White won by timeout"),
    BLACK_WON_BY_CHECKMATE("Black won by checkmate"),
    BLACK_WON_BY_RESIGNATION("Black won by resignation"),
    BLACK_WON_BY_TIMEOUT("Black won by timeout"),
    DRAW_BY_STALEMATE("Draw by stalemate"),
    DRAW_BY_INSUFFICIENT_MATERIAL("Draw by insufficient material"),
    DRAW_BY_THREEFOLD_REPETITION("Draw by threefold repetition"),
    DRAW_BY_FIFTY_MOVE_RULE("Draw by the fifty-move rule"),
    ;

    private final String message;

    GameState(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}