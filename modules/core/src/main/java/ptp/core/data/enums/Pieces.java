package ptp.core.data.enums;

public enum Pieces {
    PAWN("Pawn"),
    ROOK("Rook"),
    KNIGHT("Knight"),
    BISHOP("Bishop"),
    QUEEN("Queen"),
    KING("King");

    private final String className;

    Pieces(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}