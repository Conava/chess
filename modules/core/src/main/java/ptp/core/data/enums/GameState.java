package ptp.core.data.enums;

public enum GameState {
    NO_GAME("Kein Spiel gefunden"),
    RUNNING("Spiel läuft"),
    WHITE_WON_BY_CHECKMATE("Weiß hat durch Schachmatt gewonnen"),
    WHITE_WON_BY_RESIGNATION("Weiß hat druch Aufgabe gewonnen"),
    WHITE_WON_BY_TIMEOUT("Weiß hat durch Zeitüberschreitung gewonnen"),
    BLACK_WON_BY_CHECKMATE("Schwarz hat durch Schachmatt gewonnen"),
    BLACK_WON_BY_RESIGNATION("Schwarz hat durch Aufgabe gewonnen"),
    BLACK_WON_BY_TIMEOUT("Schwarz hat durch Zeitüberschreitung gewonnen"),
    DRAW_BY_STALEMATE("Untentschieden durch Patt"),
    DRAW_BY_INSUFFICIENT_MATERIAL("Uneintschieden durch unzureichendes Material"),
    DRAW_BY_THREEFOLD_REPETITION("Untentschieden durch dreifache Stellungswiederholung"),
    DRAW_BY_FIFTY_MOVE_RULE("Unentschieden durch die 50-Züge-Regel"),;

    private final String message;

    GameState(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}