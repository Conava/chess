package io.github.conava.chess.application.navigation;

import io.github.conava.chess.core.logic.ruleset.RulesetOptions;

/**
 * Implemented by the main menu controller to host panel lifecycle management.
 *
 * <p>Sub-screen controllers (offline setup, online setup, settings, login, register,
 * waiting-for-match) receive a {@code PanelHost} reference at construction time and
 * use it to navigate back to the clean main menu state or to transition to a sibling
 * panel. This decouples controllers from the concrete {@code MainMenuController} and
 * makes them independently testable with mock implementations.</p>
 *
 * <h2>Panel lifecycle</h2>
 * <pre>
 *   panelHost.showPanel(PanelId.OFFLINE_SETUP);   // open from clean menu
 *   panelHost.switchPanel(PanelId.SETTINGS);       // transition to another panel
 *   panelHost.closePanel();                        // return to clean menu
 * </pre>
 *
 * <p>Only one panel is active at a time. Calling {@link #showPanel} when a panel
 * is already open is equivalent to calling {@link #switchPanel}.</p>
 */
public interface PanelHost {

    /**
     * Opens the specified panel inside the main menu's panel container.
     *
     * <p>If a panel is already open, the implementation transitions from the
     * current panel to the new one (equivalent to {@link #switchPanel(PanelId)}).
     * If the menu is in its clean state (no panel open), the panel is animated in.</p>
     *
     * @param panelId the panel to open; must not be {@code null}
     */
    void showPanel(PanelId panelId);

    /**
     * Closes the currently active panel and returns the main menu to its clean state.
     *
     * <p>The panel is animated out, then removed from the container. All nav button
     * dimming is reset. If no panel is open, this method is a no-op.</p>
     */
    void closePanel();

    /**
     * Transitions from the currently active panel to the specified panel.
     *
     * <p>The current panel is animated out; once the exit animation completes, the
     * new panel is loaded and animated in. Nav button highlighting is updated to
     * reflect the new active panel.</p>
     *
     * @param panelId the panel to transition to; must not be {@code null}
     */
    void switchPanel(PanelId panelId);

    /**
     * Returns the {@link PanelId} of the currently open panel.
     *
     * @return the active panel identifier, or {@code null} if the menu is in its
     *         clean state (no panel open)
     */
    PanelId getActivePanel();

    /**
     * Transitions from the currently active panel to the matchmaking waiting panel,
     * passing the server connection parameters required to construct the controller.
     *
     * <p>This method exists because {@link PanelId#WAITING_FOR_MATCH} requires an
     * active server connection (ruleset, IP, port) that is not available at the time
     * the panel host is constructed. Callers must use this method instead of
     * {@link #switchPanel(PanelId)} when navigating to the waiting panel.</p>
     *
     * @param ruleset    the ruleset to use for the matched game; must not be {@code null}
     * @param serverIp   the server IP address or hostname; must not be {@code null}
     * @param serverPort the server port number (1–65535)
     */
    void showWaitingForMatch(RulesetOptions ruleset, String serverIp, int serverPort);
}
