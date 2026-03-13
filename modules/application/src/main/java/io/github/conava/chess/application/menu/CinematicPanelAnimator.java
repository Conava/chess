package io.github.conava.chess.application.menu;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * Reusable static utility for animating frosted-glass form panels in and out.
 *
 * <p>Both the settings screen and the offline-setup screen share the same cinematic
 * entrance/exit behaviour: the panel slides in from the right while fading in,
 * and slides back out to the right while fading out.</p>
 *
 * <h3>Entrance animation</h3>
 * <ul>
 *   <li>{@code translateX}: +60 &rarr; 0 (slides from the right)</li>
 *   <li>{@code opacity}: 0 &rarr; 1</li>
 *   <li>Duration: 450&nbsp;ms with {@link Interpolator#EASE_OUT}</li>
 * </ul>
 *
 * <h3>Exit animation</h3>
 * <ul>
 *   <li>{@code translateX}: 0 &rarr; +60 (slides to the right)</li>
 *   <li>{@code opacity}: 1 &rarr; 0</li>
 *   <li>Duration: 350&nbsp;ms with {@link Interpolator#EASE_IN}</li>
 *   <li>Mouse interaction is disabled for the duration of the exit to prevent
 *       double-click issues.</li>
 * </ul>
 */
public final class CinematicPanelAnimator {

    /** Horizontal offset (in pixels) that the panel slides from/to. */
    private static final double SLIDE_OFFSET_X = 60.0;

    /** Duration of the entrance animation. */
    private static final Duration ENTRANCE_DURATION = Duration.millis(450);

    /** Duration of the exit animation. */
    private static final Duration EXIT_DURATION = Duration.millis(350);

    /** Prevent instantiation. */
    private CinematicPanelAnimator() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Plays the entrance animation on the given panel without a completion callback.
     *
     * @param panel the JavaFX node to animate
     * @see #playEntrance(Node, Runnable)
     */
    public static void playEntrance(Node panel) {
        playEntrance(panel, null);
    }

    /**
     * Plays the entrance animation on the given panel.
     *
     * <p>The panel is first placed at its initial hidden state ({@code opacity 0},
     * {@code translateX +60}), then a {@link ParallelTransition} containing a
     * {@link TranslateTransition} and a {@link FadeTransition} is played to
     * slide and fade the panel into view.</p>
     *
     * @param panel      the JavaFX node to animate
     * @param onComplete optional callback invoked when the animation finishes (may be {@code null})
     */
    public static void playEntrance(Node panel, Runnable onComplete) {
        // Set initial hidden state
        panel.setOpacity(0);
        panel.setTranslateX(SLIDE_OFFSET_X);
        panel.setMouseTransparent(false);

        TranslateTransition slide = new TranslateTransition(ENTRANCE_DURATION, panel);
        slide.setFromX(SLIDE_OFFSET_X);
        slide.setToX(0);
        slide.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fade = new FadeTransition(ENTRANCE_DURATION, panel);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition parallel = new ParallelTransition(slide, fade);
        if (onComplete != null) {
            parallel.setOnFinished(e -> onComplete.run());
        }
        parallel.play();
    }

    /**
     * Plays the exit animation on the given panel.
     *
     * <p>Mouse interaction is disabled immediately to prevent double-click issues.
     * The panel then slides to the right and fades out over 350&nbsp;ms.</p>
     *
     * @param panel      the JavaFX node to animate
     * @param onComplete optional callback invoked when the animation finishes (may be {@code null})
     */
    public static void playExit(Node panel, Runnable onComplete) {
        panel.setMouseTransparent(true);

        TranslateTransition slide = new TranslateTransition(EXIT_DURATION, panel);
        slide.setFromX(0);
        slide.setToX(SLIDE_OFFSET_X);
        slide.setInterpolator(Interpolator.EASE_IN);

        FadeTransition fade = new FadeTransition(EXIT_DURATION, panel);
        fade.setFromValue(1);
        fade.setToValue(0);
        fade.setInterpolator(Interpolator.EASE_IN);

        ParallelTransition parallel = new ParallelTransition(slide, fade);
        parallel.setOnFinished(e -> {
            if (onComplete != null) {
                onComplete.run();
            }
        });
        parallel.play();
    }
}
