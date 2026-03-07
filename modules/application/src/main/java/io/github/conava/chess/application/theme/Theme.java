package io.github.conava.chess.application.theme;

public enum Theme {
    DARK, LIGHT;

    public String cssFile() {
        return switch (this) {
            case DARK  -> "/css/dark.css";
            case LIGHT -> "/css/light.css";
        };
    }
}
