package io.github.conava.chess.application.menu;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Orchestrates the exit transition when the user navigates away from the main menu.
 *
 * <p>The exit sequence consists of:</p>
 * <ol>
 *   <li>Disable all buttons in the navigation panel to prevent double-clicks.</li>
 *   <li>Optionally call accelerate runnables to speed up background renderers.</li>
 *   <li>Animate the nav panel sliding out to the right ({@code translateX +60}) and fading out,
 *       while the content layer fades out. Both run in parallel over 500ms.</li>
 *   <li>On completion, invoke the navigation callback to proceed to the target screen.</li>
 * </ol>
 *
 * <p>For reduced-motion mode, call {@link #playImmediate()} which skips animation entirely
 * and invokes the callback directly.</p>
 */
public class MenuExitTransition {

    private static final Duration TRANSITION_DURATION = Duration.millis(500);
    private static final double NAV_PANEL_EXIT_X = 60.0;

    private final VBox navPanel;
    private final VBox contentLayer;
    private final Runnable navigationCallback;
    private final NavigationTarget target;
    private final Runnable accelerateBackground;
    private final Runnable accelerateParticles;
    private final Runnable accelerateSilhouettes;

    /**
     * Creates an exit transition without background acceleration hooks.
     *
     * @param navPanel           the navigation button panel
     * @param contentLayer       the content layer containing title and tagline
     * @param navigationCallback the callback to invoke after the transition completes
     * @param target             the navigation target the user selected
     */
    public MenuExitTransition(VBox navPanel, VBox contentLayer,
                              Runnable navigationCallback, NavigationTarget target) {
        this(navPanel, contentLayer, navigationCallback, target, null, null, null);
    }

    /**
     * Creates an exit transition with optional background acceleration hooks.
     *
     * @param navPanel              the navigation button panel
     * @param contentLayer          the content layer containing title and tagline
     * @param navigationCallback    the callback to invoke after the transition completes
     * @param target                the navigation target the user selected
     * @param accelerateBackground  optional runnable to accelerate the background renderer
     * @param accelerateParticles   optional runnable to accelerate the particle system
     * @param accelerateSilhouettes optional runnable to accelerate the silhouette layer
     */
    public MenuExitTransition(VBox navPanel, VBox contentLayer,
                              Runnable navigationCallback, NavigationTarget target,
                              Runnable accelerateBackground,
                              Runnable accelerateParticles,
                              Runnable accelerateSilhouettes) {
        this.navPanel = navPanel;
        this.contentLayer = contentLayer;
        this.navigationCallback = navigationCallback;
        this.target = target;
        this.accelerateBackground = accelerateBackground;
        this.accelerateParticles = accelerateParticles;
        this.accelerateSilhouettes = accelerateSilhouettes;
    }

    /**
     * Plays the animated exit transition.
     *
     * <p>Disables all buttons, calls accelerate hooks, fires {@link #onTransitionStart(NavigationTarget)},
     * runs a 500ms parallel animation, then fires {@link #onTransitionEnd(NavigationTarget)}
     * and invokes the navigation callback.</p>
     */
    public void play() {
        disableAllButtons();
        callAccelerateHooks();
        onTransitionStart(target);

        // Nav panel: slide right + fade out.
        // Use setByX (relative) instead of setToX (absolute) so the slide works
        // correctly regardless of the current translateX -- for example, when a
        // sub-panel was open and the navPanel is already translated left.
        TranslateTransition navSlide = new TranslateTransition(TRANSITION_DURATION, navPanel);
        navSlide.setByX(NAV_PANEL_EXIT_X);
        navSlide.setInterpolator(Interpolator.EASE_IN);

        FadeTransition navFade = new FadeTransition(TRANSITION_DURATION, navPanel);
        navFade.setToValue(0.0);

        // Content layer: fade out
        FadeTransition contentFade = new FadeTransition(TRANSITION_DURATION, contentLayer);
        contentFade.setToValue(0.0);

        ParallelTransition parallel = new ParallelTransition(navSlide, navFade, contentFade);
        parallel.setOnFinished(e -> {
            onTransitionEnd(target);
            navigationCallback.run();
        });
        parallel.play();
    }

    /**
     * Immediately invokes the navigation callback without any animation.
     *
     * <p>Intended for reduced-motion accessibility mode. Disables buttons and
     * calls the callback synchronously.</p>
     */
    public void playImmediate() {
        disableAllButtons();
        onTransitionStart(target);
        onTransitionEnd(target);
        navigationCallback.run();
    }

    /**
     * Hook called when the exit transition starts.
     *
     * <p>Subclasses may override this to trigger sound effects.
     * The default implementation is a no-op.</p>
     *
     * @param target the navigation target being transitioned to
     */
    protected void onTransitionStart(NavigationTarget target) {
        // no-op — override for sound hooks
    }

    /**
     * Hook called when the exit transition ends, just before the navigation callback.
     *
     * <p>Subclasses may override this to trigger sound effects.
     * The default implementation is a no-op.</p>
     *
     * @param target the navigation target that was transitioned to
     */
    protected void onTransitionEnd(NavigationTarget target) {
        // no-op — override for sound hooks
    }

    /**
     * Disables all {@link Button} children in the navigation panel.
     */
    private void disableAllButtons() {
        for (Node child : navPanel.getChildren()) {
            if (child instanceof Button btn) {
                btn.setDisable(true);
            }
        }
    }

    /**
     * Calls the optional accelerate runnables if they were provided.
     */
    private void callAccelerateHooks() {
        if (accelerateBackground != null) {
            accelerateBackground.run();
        }
        if (accelerateParticles != null) {
            accelerateParticles.run();
        }
        if (accelerateSilhouettes != null) {
            accelerateSilhouettes.run();
        }
    }
}
