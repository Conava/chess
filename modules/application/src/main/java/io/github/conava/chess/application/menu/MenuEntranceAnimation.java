package io.github.conava.chess.application.menu;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the choreographed entrance animation for the cinematic main menu.
 *
 * <p>The animation sequence is as follows:</p>
 * <ol>
 *   <li><b>Title</b> (0-700ms): slides up 40px from below final position and fades from 0 to 1
 *       with {@link Interpolator#EASE_OUT}.</li>
 *   <li><b>Accent line</b> (700-1100ms): scales horizontally from 0 to 1 after the title arrives.</li>
 *   <li><b>Tagline</b> (300-800ms): fades from 0 to 1, starting 300ms after the title begins.</li>
 *   <li><b>Nav buttons</b> (400ms+ staggered): each button slides in from the right
 *       ({@code translateX +40 -> 0}) and fades in, staggered 100ms apart. The first button
 *       starts 400ms after the title begins.</li>
 * </ol>
 *
 * <p>All nodes are set to their initial hidden state upon construction. Call {@link #play()} to
 * start the animation, or {@link #skipToEnd()} for reduced-motion mode.</p>
 */
public class MenuEntranceAnimation {

    private static final double TITLE_OFFSET_Y = 40.0;
    private static final double BUTTON_OFFSET_X = 40.0;
    private static final Duration TITLE_DURATION = Duration.millis(700);
    private static final Duration ACCENT_DURATION = Duration.millis(400);
    private static final Duration TAGLINE_DURATION = Duration.millis(500);
    private static final Duration BUTTON_DURATION = Duration.millis(400);
    private static final Duration TAGLINE_DELAY = Duration.millis(300);
    private static final Duration BUTTON_BASE_DELAY = Duration.millis(400);
    private static final Duration BUTTON_STAGGER = Duration.millis(100);

    private final Label title;
    private final Region accent;
    private final Label tagline;
    private final List<Button> navButtons;
    private final Timeline timeline;

    /**
     * Creates a new entrance animation for the given menu elements.
     *
     * <p>All nodes are immediately set to their initial hidden state (opacity 0,
     * positional offsets applied).</p>
     *
     * @param title      the title label that slides up and fades in
     * @param accent     the accent region (line) that scales horizontally
     * @param tagline    the tagline label that fades in
     * @param navButtons the navigation buttons that slide in from the right, staggered
     */
    public MenuEntranceAnimation(Label title, Region accent, Label tagline, List<Button> navButtons) {
        this.title = title;
        this.accent = accent;
        this.tagline = tagline;
        this.navButtons = new ArrayList<>(navButtons);

        setInitialState();
        this.timeline = buildTimeline();
    }

    /**
     * Starts the full entrance choreography.
     *
     * <p>Calls {@link #onEntranceStart()} before the animation begins and
     * {@link #onEntranceComplete()} when it finishes.</p>
     */
    public void play() {
        onEntranceStart();
        timeline.setOnFinished(e -> onEntranceComplete());
        timeline.playFromStart();
    }

    /**
     * Immediately places all nodes at their final positions without animation.
     *
     * <p>Intended for reduced-motion accessibility mode. Sets opacity to 1,
     * clears all translate offsets, and resets scale to 1.</p>
     */
    public void skipToEnd() {
        timeline.stop();

        title.setOpacity(1.0);
        title.setTranslateY(0.0);

        accent.setScaleX(1.0);

        tagline.setOpacity(1.0);

        for (Button btn : navButtons) {
            btn.setOpacity(1.0);
            btn.setTranslateX(0.0);
        }
    }

    /**
     * Hook called immediately before the entrance animation starts.
     *
     * <p>Subclasses may override this to trigger sound effects or other side effects.
     * The default implementation is a no-op.</p>
     */
    protected void onEntranceStart() {
        // no-op — override for sound hooks
    }

    /**
     * Hook called when the entrance animation completes.
     *
     * <p>Subclasses may override this to trigger sound effects or other side effects.
     * The default implementation is a no-op.</p>
     */
    protected void onEntranceComplete() {
        // no-op — override for sound hooks
    }

    /**
     * Sets all nodes to their pre-animation hidden state.
     */
    private void setInitialState() {
        title.setOpacity(0.0);
        title.setTranslateY(TITLE_OFFSET_Y);

        accent.setScaleX(0.0);

        tagline.setOpacity(0.0);

        for (Button btn : navButtons) {
            btn.setOpacity(0.0);
            btn.setTranslateX(BUTTON_OFFSET_X);
        }
    }

    /**
     * Builds the {@link Timeline} containing all choreographed keyframes.
     *
     * @return the fully configured timeline (not yet started)
     */
    private Timeline buildTimeline() {
        List<KeyFrame> frames = new ArrayList<>();

        // Title: slide up 40px + fade in over 700ms with EASE_OUT
        frames.add(new KeyFrame(Duration.ZERO,
                new KeyValue(title.opacityProperty(), 0.0),
                new KeyValue(title.translateYProperty(), TITLE_OFFSET_Y)));
        frames.add(new KeyFrame(TITLE_DURATION,
                new KeyValue(title.opacityProperty(), 1.0, Interpolator.EASE_OUT),
                new KeyValue(title.translateYProperty(), 0.0, Interpolator.EASE_OUT)));

        // Accent line: scaleX from 0 to 1, starts after title arrives (700ms)
        Duration accentStart = TITLE_DURATION;
        frames.add(new KeyFrame(accentStart,
                new KeyValue(accent.scaleXProperty(), 0.0)));
        frames.add(new KeyFrame(accentStart.add(ACCENT_DURATION),
                new KeyValue(accent.scaleXProperty(), 1.0, Interpolator.EASE_OUT)));

        // Tagline: fade in over 500ms, starts 300ms after title begins
        frames.add(new KeyFrame(TAGLINE_DELAY,
                new KeyValue(tagline.opacityProperty(), 0.0)));
        frames.add(new KeyFrame(TAGLINE_DELAY.add(TAGLINE_DURATION),
                new KeyValue(tagline.opacityProperty(), 1.0, Interpolator.EASE_OUT)));

        // Nav buttons: staggered slide-in from right + fade
        for (int i = 0; i < navButtons.size(); i++) {
            Button btn = navButtons.get(i);
            Duration btnStart = BUTTON_BASE_DELAY.add(BUTTON_STAGGER.multiply(i));

            frames.add(new KeyFrame(btnStart,
                    new KeyValue(btn.opacityProperty(), 0.0),
                    new KeyValue(btn.translateXProperty(), BUTTON_OFFSET_X)));
            frames.add(new KeyFrame(btnStart.add(BUTTON_DURATION),
                    new KeyValue(btn.opacityProperty(), 1.0, Interpolator.EASE_OUT),
                    new KeyValue(btn.translateXProperty(), 0.0, Interpolator.EASE_OUT)));
        }

        Timeline tl = new Timeline();
        tl.getKeyFrames().addAll(frames);
        return tl;
    }
}
