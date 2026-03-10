package io.github.conava.chess.core.logic.ruleset;

/**
 * Identifies the available rulesets for a chess game.
 *
 * <p>Each constant carries a human-readable {@code displayName} returned by
 * {@link #toString()}.  Server-side parsing must use {@link #valueOf(String)}
 * with the constant name (e.g. {@code "STANDARD"}, {@code "CHESS960"}) so that
 * the overridden {@code toString()} does not interfere with deserialization.
 */
public enum RulesetOptions {
    STANDARD("Standard Chess"),
    CHESS960("Chess 960");

    private final String displayName;

    RulesetOptions(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the human-readable display name suitable for UI components such as
     * a {@code ComboBox}.  Do <em>not</em> use this value for server-side
     * deserialization — use {@link #valueOf(String)} with the constant name instead.
     */
    @Override
    public String toString() {
        return displayName;
    }
}
