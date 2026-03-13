package io.github.conava.chess.application.menu;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.DoubleBinding;
import javafx.scene.CacheHint;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Encapsulates the coordinated open/close layout animation for the cinematic main menu.
 *
 * <p>Uses a single-phase translate+scale animation to move the content layer (title/accent/tagline
 * VBox) from its center-left hero position into a compact position above the nav buttons:</p>
 * <ul>
 *   <li><strong>Open:</strong> simultaneously slides the nav panel left to
 *       {@code navTranslateTarget} and translates+scales the content layer to its compact
 *       position defined by {@code compactTranslateX}, {@code compactTranslateY}, and
 *       {@code compactScale}. On completion, all animated properties are <em>bound</em>
 *       reactively so resize events track the correct position without any snap.</li>
 *   <li><strong>Close:</strong> unbinds all properties and simultaneously slides the nav
 *       panel right back to 0 and restores the content layer to its default position
 *       (translateX=0, translateY=0, scaleX=1.0, scaleY=1.0).</li>
 * </ul>
 *
 * <h3>No reparenting</h3>
 * <p>{@code contentLayer} stays in its original parent at all times. The v1 reparenting
 * model (moving contentLayer between rootPane and navPanel) is entirely absent. This
 * eliminates the visible jump caused by the parent-change layout recalculation.</p>
 *
 * <h3>Nav panel translate management</h3>
 * <p>After the open animation completes, {@code navPanel.translateXProperty()} is
 * <em>bound</em> to {@code navTranslateTarget}. This eliminates any snap/jump when
 * the window is resized while a panel is open, because the position updates reactively.
 * Before any animation (or before any Timeline writes to {@code translateX}), all
 * bindings are explicitly <em>unbound</em>.</p>
 *
 * <h3>Reduced motion</h3>
 * <p>When {@code reducedMotion} is {@code true}, {@link #animateOpen(Runnable)} and
 * {@link #animateClose(Runnable)} behave like {@link #skipToOpen()} and
 * {@link #skipToClose()} respectively — all properties are applied synchronously with
 * no timeline.</p>
 *
 * <h3>Idempotency</h3>
 * <p>Calling {@link #skipToOpen()} (or {@link #animateOpen(Runnable)}) when already
 * open is a no-op. The same applies to the close direction.</p>
 */
public class MenuLayoutTransition {

    private final VBox navPanel;
    private final VBox contentLayer;
    private final DoubleBinding navTranslateTarget;
    private final DoubleBinding compactTranslateX;
    private final DoubleBinding compactTranslateY;
    private final DoubleBinding compactScale;
    private final Duration slideDuration;
    private final boolean reducedMotion;

    /** Whether the transition is currently in the "open" (panel-visible) state. */
    private boolean open;

    /**
     * The currently playing timeline (open or close animation).
     * Stopped before starting a new animation. May be {@code null} when idle.
     */
    private Timeline currentTimeline;

    /**
     * Creates a new {@code MenuLayoutTransition} using the v2 translate+scale model.
     *
     * <p>{@code contentLayer} is never reparented; it stays in its original parent
     * throughout the entire lifetime of this object.</p>
     *
     * @param navPanel            the navigation button panel whose {@code translateX} is animated;
     *                            slides left to {@code navTranslateTarget} when open
     * @param contentLayer        the title/tagline/accent VBox that is translated and scaled
     *                            in place to the compact position when open
     * @param navTranslateTarget  target {@code translateX} for {@code navPanel} in the open state;
     *                            a {@link DoubleBinding} so the value updates reactively when the
     *                            window is resized; the property is <em>bound</em> to this after
     *                            the open animation completes
     * @param compactTranslateX   reactive target for {@code contentLayer.translateX} in the open
     *                            state; bound after the open animation completes
     * @param compactTranslateY   reactive target for {@code contentLayer.translateY} in the open
     *                            state; bound after the open animation completes
     * @param compactScale        reactive target for both {@code contentLayer.scaleX} and
     *                            {@code contentLayer.scaleY} in the open state; bound after the
     *                            open animation completes
     * @param duration            duration of the single-phase nav-slide + translate/scale animation;
     *                            ignored when {@code reducedMotion} is {@code true}
     * @param reducedMotion       when {@code true} all transitions skip to end-state instantly
     */
    public MenuLayoutTransition(VBox navPanel,
                                VBox contentLayer,
                                DoubleBinding navTranslateTarget,
                                DoubleBinding compactTranslateX,
                                DoubleBinding compactTranslateY,
                                DoubleBinding compactScale,
                                Duration duration,
                                boolean reducedMotion) {
        this.navPanel           = navPanel;
        this.contentLayer       = contentLayer;
        this.navTranslateTarget = navTranslateTarget;
        this.compactTranslateX  = compactTranslateX;
        this.compactTranslateY  = compactTranslateY;
        this.compactScale       = compactScale;
        this.slideDuration      = duration;
        this.reducedMotion      = reducedMotion;
        this.open               = false;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Animates the menu into the "panel open" state.
     *
     * <p>Runs a single-phase timeline that simultaneously animates:</p>
     * <ul>
     *   <li>{@code navPanel.translateX} from its current value to {@code navTranslateTarget}</li>
     *   <li>{@code contentLayer.translateX} from its current value to {@code compactTranslateX}</li>
     *   <li>{@code contentLayer.translateY} from its current value to {@code compactTranslateY}</li>
     *   <li>{@code contentLayer.scaleX} from its current value to {@code compactScale}</li>
     *   <li>{@code contentLayer.scaleY} from its current value to {@code compactScale}</li>
     * </ul>
     * <p>All properties use {@link Interpolator#EASE_BOTH}. On completion, all five
     * properties are <em>bound</em> to their respective target bindings so that window
     * resize events track the correct position without any snap.</p>
     *
     * <p>If {@code reducedMotion} is enabled, behaves like {@link #skipToOpen()} and calls
     * {@code onComplete} immediately. If already open, this is a no-op.</p>
     *
     * @param onComplete optional callback invoked after the transition completes; may be {@code null}
     */
    public void animateOpen(Runnable onComplete) {
        if (open) return;
        if (reducedMotion) {
            skipToOpen();
            if (onComplete != null) onComplete.run();
            return;
        }

        stopCurrentTimeline();
        // Defensive unbind: all properties must be unbound before Timeline writes to them
        unbindAll();
        open = true;

        // Capture current values for the start KeyFrame
        double navStartX   = navPanel.getTranslateX();
        double clStartX    = contentLayer.getTranslateX();
        double clStartY    = contentLayer.getTranslateY();
        double clStartScX  = contentLayer.getScaleX();
        double clStartScY  = contentLayer.getScaleY();

        // Capture target values from reactive bindings
        double navEndX     = navTranslateTarget.get();
        double clEndX      = compactTranslateX.get();
        double clEndY      = compactTranslateY.get();
        double clEndScale  = compactScale.get();

        KeyFrame start = new KeyFrame(Duration.ZERO,
                new KeyValue(navPanel.translateXProperty(),       navStartX),
                new KeyValue(contentLayer.translateXProperty(),   clStartX),
                new KeyValue(contentLayer.translateYProperty(),   clStartY),
                new KeyValue(contentLayer.scaleXProperty(),       clStartScX),
                new KeyValue(contentLayer.scaleYProperty(),       clStartScY));

        KeyFrame end = new KeyFrame(slideDuration,
                new KeyValue(navPanel.translateXProperty(),       navEndX,    Interpolator.EASE_BOTH),
                new KeyValue(contentLayer.translateXProperty(),   clEndX,     Interpolator.EASE_BOTH),
                new KeyValue(contentLayer.translateYProperty(),   clEndY,     Interpolator.EASE_BOTH),
                new KeyValue(contentLayer.scaleXProperty(),       clEndScale, Interpolator.EASE_BOTH),
                new KeyValue(contentLayer.scaleYProperty(),       clEndScale, Interpolator.EASE_BOTH));

        Timeline timeline = new Timeline(start, end);
        currentTimeline = timeline;
        timeline.setOnFinished(e -> {
            currentTimeline = null;
            // Bind all properties reactively so resize events track correct positions
            navPanel.translateXProperty().bind(navTranslateTarget);
            contentLayer.translateXProperty().bind(compactTranslateX);
            contentLayer.translateYProperty().bind(compactTranslateY);
            contentLayer.scaleXProperty().bind(compactScale);
            contentLayer.scaleYProperty().bind(compactScale);
            // Enable quality cache hint: fractional scale values cause text blur without it
            contentLayer.setCache(true);
            contentLayer.setCacheHint(CacheHint.QUALITY);
            if (onComplete != null) onComplete.run();
        });
        timeline.play();
    }

    /**
     * Animates the menu back to the "panel closed" state.
     *
     * <p>Unbinds all reactive bindings, then runs a single-phase timeline that
     * simultaneously restores all properties to their defaults:</p>
     * <ul>
     *   <li>{@code navPanel.translateX} → 0</li>
     *   <li>{@code contentLayer.translateX} → 0</li>
     *   <li>{@code contentLayer.translateY} → 0</li>
     *   <li>{@code contentLayer.scaleX} → 1.0</li>
     *   <li>{@code contentLayer.scaleY} → 1.0</li>
     * </ul>
     * <p>All properties use {@link Interpolator#EASE_BOTH}. On completion, cache hinting
     * is disabled (no fractional scale in the default state).</p>
     *
     * <p>If {@code reducedMotion} is enabled, behaves like {@link #skipToClose()} and calls
     * {@code onComplete} immediately. If already closed, this is a no-op.</p>
     *
     * @param onComplete optional callback invoked after the transition completes; may be {@code null}
     */
    public void animateClose(Runnable onComplete) {
        if (!open) return;
        if (reducedMotion) {
            skipToClose();
            if (onComplete != null) onComplete.run();
            return;
        }

        stopCurrentTimeline();
        // Unbind all before Timeline writes to the properties
        unbindAll();
        open = false;

        // Capture current values for start KeyFrame
        double navStartX   = navPanel.getTranslateX();
        double clStartX    = contentLayer.getTranslateX();
        double clStartY    = contentLayer.getTranslateY();
        double clStartScX  = contentLayer.getScaleX();
        double clStartScY  = contentLayer.getScaleY();

        KeyFrame start = new KeyFrame(Duration.ZERO,
                new KeyValue(navPanel.translateXProperty(),       navStartX),
                new KeyValue(contentLayer.translateXProperty(),   clStartX),
                new KeyValue(contentLayer.translateYProperty(),   clStartY),
                new KeyValue(contentLayer.scaleXProperty(),       clStartScX),
                new KeyValue(contentLayer.scaleYProperty(),       clStartScY));

        KeyFrame end = new KeyFrame(slideDuration,
                new KeyValue(navPanel.translateXProperty(),       0.0, Interpolator.EASE_BOTH),
                new KeyValue(contentLayer.translateXProperty(),   0.0, Interpolator.EASE_BOTH),
                new KeyValue(contentLayer.translateYProperty(),   0.0, Interpolator.EASE_BOTH),
                new KeyValue(contentLayer.scaleXProperty(),       1.0, Interpolator.EASE_BOTH),
                new KeyValue(contentLayer.scaleYProperty(),       1.0, Interpolator.EASE_BOTH));

        Timeline timeline = new Timeline(start, end);
        currentTimeline = timeline;
        timeline.setOnFinished(e -> {
            currentTimeline = null;
            // Disable cache: no fractional scale in closed state, so no blur to compensate
            contentLayer.setCache(false);
            contentLayer.setCacheHint(CacheHint.DEFAULT);
            if (onComplete != null) onComplete.run();
        });
        timeline.play();
    }

    /**
     * Instantly transitions all properties to the "open" state without animation.
     *
     * <p>Binds all five animated properties ({@code navPanel.translateX},
     * {@code contentLayer.translateX/Y}, {@code contentLayer.scaleX/Y}) to their
     * respective target bindings and enables cache hinting.</p>
     *
     * <p>If already open, this is a no-op.</p>
     */
    public void skipToOpen() {
        if (open) return;
        applyOpenState();
        open = true;
    }

    /**
     * Instantly restores all properties to the "closed" (default) state without animation.
     *
     * <p>Unbinds all reactive bindings and resets:
     * {@code navPanel.translateX=0}, {@code contentLayer.translateX=0},
     * {@code contentLayer.translateY=0}, {@code contentLayer.scaleX=1.0},
     * {@code contentLayer.scaleY=1.0}, {@code contentLayer.opacity=1.0}.
     * Disables cache hinting.</p>
     *
     * <p>If already closed, this is a no-op.</p>
     */
    public void skipToClose() {
        if (!open) return;
        applyClosedState();
        open = false;
    }

    /**
     * Returns whether the transition is currently in the open state.
     *
     * @return {@code true} if the panel is open (or transitioning to open),
     *         {@code false} otherwise
     */
    public boolean isOpen() {
        return open;
    }

    // -------------------------------------------------------------------------
    // Private helpers — state application
    // -------------------------------------------------------------------------

    /**
     * Applies all open-state property values synchronously.
     *
     * <p>Calls {@link #unbindAll()} defensively before binding to ensure the properties
     * are writable. Binds all five animated properties to their reactive target bindings
     * and enables cache hinting for quality rendering at fractional scale.</p>
     */
    private void applyOpenState() {
        unbindAll();
        navPanel.translateXProperty().bind(navTranslateTarget);
        contentLayer.translateXProperty().bind(compactTranslateX);
        contentLayer.translateYProperty().bind(compactTranslateY);
        contentLayer.scaleXProperty().bind(compactScale);
        contentLayer.scaleYProperty().bind(compactScale);
        contentLayer.setOpacity(1.0);
        contentLayer.setCache(true);
        contentLayer.setCacheHint(CacheHint.QUALITY);
    }

    /**
     * Restores all closed-state (default) property values synchronously.
     *
     * <p>Calls {@link #unbindAll()} to release all reactive bindings, then sets all
     * animated properties to their default values and disables cache hinting.</p>
     */
    private void applyClosedState() {
        unbindAll();
        navPanel.setTranslateX(0.0);
        contentLayer.setTranslateX(0.0);
        contentLayer.setTranslateY(0.0);
        contentLayer.setScaleX(1.0);
        contentLayer.setScaleY(1.0);
        contentLayer.setOpacity(1.0);
        contentLayer.setCache(false);
        contentLayer.setCacheHint(CacheHint.DEFAULT);
    }

    // -------------------------------------------------------------------------
    // Private helpers — binding management
    // -------------------------------------------------------------------------

    /**
     * Unbinds all five animated properties so they can be set by a Timeline or directly.
     *
     * <p>Safe to call when properties are not currently bound (unbind is a no-op in that case).</p>
     */
    private void unbindAll() {
        navPanel.translateXProperty().unbind();
        contentLayer.translateXProperty().unbind();
        contentLayer.translateYProperty().unbind();
        contentLayer.scaleXProperty().unbind();
        contentLayer.scaleYProperty().unbind();
    }

    // -------------------------------------------------------------------------
    // Private helpers — timeline management
    // -------------------------------------------------------------------------

    /**
     * Stops the currently playing timeline, if any.
     *
     * <p>Sets {@code currentTimeline} to {@code null} after stopping. Safe to call
     * when no timeline is playing.</p>
     */
    private void stopCurrentTimeline() {
        if (currentTimeline != null) {
            currentTimeline.stop();
            currentTimeline = null;
        }
    }
}
