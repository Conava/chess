package io.github.conava.chess.application.menu;

/**
 * Represents the possible navigation destinations from the main menu.
 *
 * <p>Used by {@link MenuExitTransition} to communicate which screen
 * the user is navigating to, allowing sound hooks and transition
 * customisation per target.</p>
 */
public enum NavigationTarget {
    /** Start a local (same-device) game. */
    LOCAL_GAME,
    /** Start or join an online game. */
    ONLINE_GAME,
    /** Open the settings screen. */
    SETTINGS,
    /** Exit the application. */
    EXIT
}
