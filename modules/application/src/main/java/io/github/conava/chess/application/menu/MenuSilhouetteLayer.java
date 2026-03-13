package io.github.conava.chess.application.menu;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A decorative layer of large, faint chess piece silhouettes that drift slowly
 * across the screen. Intended as atmospheric decoration for the cinematic main menu.
 *
 * <p>Each silhouette is a {@link Text} node displaying a random chess piece Unicode
 * character (U+2654 through U+265F) at very low opacity (0.04-0.12). The pieces
 * drift predominantly upward or diagonally using combined translate and fade
 * transitions.</p>
 *
 * <p>The layer itself is mouse-transparent and does not intercept input events.</p>
 */
public class MenuSilhouetteLayer extends Pane {

    /** Chess piece Unicode characters: U+2654 (white king) through U+265F (black pawn). */
    private static final String[] CHESS_PIECES = {
            "\u2654", "\u2655", "\u2656", "\u2657", "\u2658", "\u2659",
            "\u265A", "\u265B", "\u265C", "\u265D", "\u265E", "\u265F"
    };

    private static final int MIN_COUNT = 3;
    private static final int MAX_COUNT = 5;
    private static final double MIN_FONT_SIZE = 80.0;
    private static final double MAX_FONT_SIZE = 160.0;
    private static final double MIN_OPACITY = 0.04;
    private static final double MAX_OPACITY = 0.12;
    private static final double MIN_DURATION_SECONDS = 15.0;
    private static final double MAX_DURATION_SECONDS = 30.0;

    private final Random random = new Random();
    private final List<Text> silhouettes = new ArrayList<>();
    private final List<ParallelTransition> transitions = new ArrayList<>();
    private Color textColor;
    private double speedMultiplier = 1.0;

    /**
     * Creates a new silhouette layer with the given text color.
     *
     * @param textColor the color used for silhouette text nodes (rendered at very low opacity)
     */
    public MenuSilhouetteLayer(Color textColor) {
        this.textColor = textColor;
        setMouseTransparent(true);
        setPickOnBounds(false);

        int count = MIN_COUNT + random.nextInt(MAX_COUNT - MIN_COUNT + 1);
        for (int i = 0; i < count; i++) {
            Text text = createSilhouette();
            silhouettes.add(text);
            getChildren().add(text);
        }
    }

    /**
     * Creates a single silhouette {@link Text} node with randomized chess piece character,
     * font size, opacity, and initial position.
     *
     * @return a configured Text node ready for animation
     */
    private Text createSilhouette() {
        String piece = CHESS_PIECES[random.nextInt(CHESS_PIECES.length)];
        double fontSize = MIN_FONT_SIZE + random.nextDouble() * (MAX_FONT_SIZE - MIN_FONT_SIZE);
        double opacity = MIN_OPACITY + random.nextDouble() * (MAX_OPACITY - MIN_OPACITY);

        Text text = new Text(piece);
        text.setFont(Font.font(fontSize));
        text.setOpacity(opacity);
        text.setFill(textColor);
        text.setMouseTransparent(true);

        // Random initial position — uses pane dimensions if available, else defaults
        double w = getWidth() > 0 ? getWidth() : 900;
        double h = getHeight() > 0 ? getHeight() : 700;
        text.setTranslateX(random.nextDouble() * w - w * 0.1);
        text.setTranslateY(random.nextDouble() * h + h * 0.3);

        return text;
    }

    /**
     * Starts all drift animations. Each silhouette drifts upward (and slightly
     * diagonally) using a combined {@link TranslateTransition} and {@link FadeTransition}
     * wrapped in a {@link ParallelTransition}. When a cycle completes, the silhouette
     * is repositioned to a random off-screen location and the animation restarts.
     */
    public void startAnimations() {
        for (Text text : silhouettes) {
            ParallelTransition pt = createDriftAnimation(text);
            transitions.add(pt);
            pt.play();
        }
    }

    /**
     * Creates a drift animation for a single silhouette node.
     *
     * @param text the silhouette text node to animate
     * @return a ParallelTransition combining translate and fade effects
     */
    private ParallelTransition createDriftAnimation(Text text) {
        double durationSeconds = MIN_DURATION_SECONDS
                + random.nextDouble() * (MAX_DURATION_SECONDS - MIN_DURATION_SECONDS);
        Duration duration = Duration.seconds(durationSeconds / speedMultiplier);

        // Drift upward with slight horizontal offset — scale to pane dimensions
        double h = getHeight() > 0 ? getHeight() : 700;
        double w = getWidth() > 0 ? getWidth() : 900;
        TranslateTransition translate = new TranslateTransition(duration, text);
        translate.setByY(-h * 1.15 - random.nextDouble() * h * 0.55);
        translate.setByX((random.nextDouble() - 0.5) * w * 0.33);

        // Subtle fade: starts at current opacity, fades to near-zero
        FadeTransition fade = new FadeTransition(duration, text);
        fade.setFromValue(text.getOpacity());
        fade.setToValue(text.getOpacity() * 0.3);

        ParallelTransition parallel = new ParallelTransition(translate, fade);
        parallel.setOnFinished(e -> {
            resetPosition(text);
            ParallelTransition next = createDriftAnimation(text);
            // Replace the old transition reference
            int idx = transitions.indexOf(parallel);
            if (idx >= 0) {
                transitions.set(idx, next);
            } else {
                transitions.add(next);
            }
            next.play();
        });

        return parallel;
    }

    /**
     * Resets a silhouette to a random off-screen starting position (bottom or sides)
     * and restores its opacity for the next traversal cycle.
     *
     * @param text the silhouette text node to reposition
     */
    private void resetPosition(Text text) {
        double opacity = MIN_OPACITY + random.nextDouble() * (MAX_OPACITY - MIN_OPACITY);
        text.setOpacity(opacity);

        double w = getWidth() > 0 ? getWidth() : 900;
        double h = getHeight() > 0 ? getHeight() : 700;

        // Reset to bottom or side off-screen position
        if (random.nextBoolean()) {
            // Start from bottom
            text.setTranslateX(random.nextDouble() * w - w * 0.1);
            text.setTranslateY(h + random.nextDouble() * h * 0.3);
        } else {
            // Start from side
            text.setTranslateX(random.nextBoolean() ? -200 : w + random.nextDouble() * 100);
            text.setTranslateY(random.nextDouble() * h);
        }
    }

    /**
     * Stops all running transitions and clears the transition list.
     * Silhouette nodes remain in the scene graph but are no longer animated.
     */
    public void stopAll() {
        for (ParallelTransition pt : transitions) {
            pt.stop();
        }
        transitions.clear();
    }

    /**
     * Increases the animation speed by approximately 3x for use during exit transitions.
     * Any currently running animations are restarted at the new speed.
     */
    public void accelerate() {
        speedMultiplier = 3.0;
        // Restart animations at increased speed
        List<ParallelTransition> oldTransitions = new ArrayList<>(transitions);
        transitions.clear();
        for (ParallelTransition pt : oldTransitions) {
            pt.stop();
        }
        for (Text text : silhouettes) {
            ParallelTransition pt = createDriftAnimation(text);
            transitions.add(pt);
            pt.play();
        }
    }

    /**
     * Resets the animation speed back to normal (1x) after acceleration.
     * Any currently running animations are restarted at normal speed.
     */
    public void resetSpeed() {
        speedMultiplier = 1.0;
        List<ParallelTransition> oldTransitions = new ArrayList<>(transitions);
        transitions.clear();
        for (ParallelTransition pt : oldTransitions) {
            pt.stop();
        }
        for (Text text : silhouettes) {
            ParallelTransition pt = createDriftAnimation(text);
            transitions.add(pt);
            pt.play();
        }
    }

    /**
     * Updates the fill color of all silhouette text nodes, for example when
     * the application theme changes.
     *
     * @param textColor the new color to apply to all silhouettes
     */
    public void updateColor(Color textColor) {
        this.textColor = textColor;
        for (Text text : silhouettes) {
            text.setFill(textColor);
        }
    }
}
