package io.github.conava.chess.application.theme;

public enum Theme {
    DARK_PURPLE, DARK_CHARCOAL, DARK_ABYSS, LIGHT_PAPER, LIGHT_ARCTIC, LIGHT_SAKURA;

    public String cssFile() {
        return switch (this) {
            case DARK_PURPLE -> "/css/themes/dark-purple.css";
            case DARK_CHARCOAL -> "/css/themes/dark-charcoal.css";
            case DARK_ABYSS -> "/css/themes/dark-abyss.css";
            case LIGHT_PAPER -> "/css/themes/light-paper.css";
            case LIGHT_ARCTIC -> "/css/themes/light-arctic.css";
            case LIGHT_SAKURA -> "/css/themes/light-sakura.css";
        };
    }

    public String displayName() {
        return switch (this) {
            case DARK_PURPLE -> "Midnight";
            case DARK_CHARCOAL -> "Ember";
            case DARK_ABYSS -> "Abyss";
            case LIGHT_PAPER -> "Manuscript";
            case LIGHT_ARCTIC -> "Fjord";
            case LIGHT_SAKURA -> "Sakura";
        };
    }
}
