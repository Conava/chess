package io.github.conava.chess.application.theme;

public enum Theme {
    DARK_PURPLE, DARK_CHARCOAL, LIGHT_PAPER, LIGHT_ARCTIC;

    public String cssFile() {
        return switch (this) {
            case DARK_PURPLE -> "/css/themes/dark-purple.css";
            case DARK_CHARCOAL -> "/css/themes/dark-charcoal.css";
            case LIGHT_PAPER -> "/css/themes/light-paper.css";
            case LIGHT_ARCTIC -> "/css/themes/light-arctic.css";
        };
    }

    public String displayName() {
        return switch (this) {
            case DARK_PURPLE -> "Midnight";
            case DARK_CHARCOAL -> "Ember";
            case LIGHT_PAPER -> "Manuscript";
            case LIGHT_ARCTIC -> "Fjord";
        };
    }
}
