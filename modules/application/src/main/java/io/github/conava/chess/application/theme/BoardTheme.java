package io.github.conava.chess.application.theme;

public enum BoardTheme {
    CLASSIC, OCEAN, WALNUT;

    public String cssFile() {
        return switch (this) {
            case CLASSIC -> "/css/board/classic.css";
            case OCEAN -> "/css/board/ocean.css";
            case WALNUT -> "/css/board/walnut.css";
        };
    }
}
