package io.github.conava.chess.application.navigation;

/**
 * Identifies the panel currently hosted inside the main menu shell's right-side
 * {@code panelContainer}.
 *
 * <p>Each value corresponds to one of the sub-screens that can be loaded as an
 * in-place panel without replacing the main menu scene. The {@link PanelHost}
 * interface uses these identifiers for type-safe panel dispatch.</p>
 *
 * <p>Game navigation (e.g. to the board) is <em>not</em> represented here because
 * the game screen performs a full scene swap via {@link SceneManager}, not a panel
 * load.</p>
 */
public enum PanelId {

    /** The offline (local) game setup panel. */
    OFFLINE_SETUP,

    /** The online game setup panel (server connection and ruleset selection). */
    ONLINE_SETUP,

    /** The application settings panel (theme, language, etc.). */
    SETTINGS,

    /** The account login panel. */
    LOGIN,

    /** The new account registration panel. */
    REGISTER,

    /** The matchmaking waiting panel shown while searching for an opponent. */
    WAITING_FOR_MATCH
}
