package io.github.conava.chess.application.menu;

import io.github.conava.chess.application.theme.ThemeColorResolver;
import io.github.conava.chess.application.theme.ThemeManager;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.List;

/**
 * Standalone cinematic background component for the main menu and other
 * cinematic screens (settings, offline setup).
 *
 * <p>Owns all background rendering layers: floating particles and drifting
 * chess piece silhouettes. A {@link Timeline} drives particle
 * rendering at approximately 20 FPS (50ms frame interval) on a half-resolution
 * canvas that is scaled up 2x, reducing GPU pixel-fill work by approximately 75%.</p>
 *
 * <p>The component is designed to be managed by {@code SceneManager} as a
 * persistent layer shared across cinematic screens. Use {@link #getRoot()}
 * to obtain the renderable {@link StackPane}, then call {@link #start()} to
 * begin animations and {@link #stop()} to pause them.</p>
 *
 * <h3>Layer Architecture</h3>
 * <pre>
 * StackPane (mouse-transparent)
 *   +-- Canvas: particles at 50% resolution, scaled 2x (Timeline at ~20 FPS)
 *   +-- MenuSilhouetteLayer (own transitions)
 *   +-- Pane: hidden probe host (for ThemeColorResolver)
 * </pre>
 */
public class CinematicBackground {

    /** CSS color tokens required by the animated background layers. */
    private static final List<String> COLOR_TOKENS = List.of(
            "app-primary", "app-dot", "app-text", "app-bg");

    /**
     * Fraction of parent dimensions used for the particle canvas.
     *
     * <p>The canvas is rendered at half resolution and then scaled up 2x via
     * {@code scaleX}/{@code scaleY}, reducing pixel-fill work by 75%. Particles
     * are soft, low-opacity bokeh dots whose intentional blur makes the slight
     * upscaling artifacts visually indistinguishable from full-resolution output.</p>
     */
    private static final double CANVAS_SCALE = 0.5;

    /** Target frame interval for particle animation (~20 FPS). */
    private static final Duration FRAME_DURATION = Duration.millis(50);

    private final ThemeManager themeManager;

    private final StackPane root;
    private final Canvas particleCanvas;
    private final MenuParticleSystem particleSystem;
    private final MenuSilhouetteLayer silhouetteLayer;
    private final ThemeColorResolver colorResolver;

    private Timeline animationTimeline;
    private boolean running;

    /**
     * Creates a new cinematic background component.
     *
     * <p>Always creates particle and silhouette layers. Call {@link #start()}
     * to begin the animation loop.</p>
     *
     * @param themeManager the theme manager for CSS color resolution and theme change listening
     */
    public CinematicBackground(ThemeManager themeManager) {
        this.themeManager = themeManager;
        this.running = false;

        // Build the root StackPane
        root = new StackPane();
        root.setMouseTransparent(true);

        // Set up the hidden probe host for ThemeColorResolver
        Pane probeHost = new Pane();
        probeHost.setManaged(false);
        probeHost.setVisible(false);

        // Create color resolver (may return fallback colors until CSS is applied)
        boolean hasThemeSupport = themeManager.currentThemeProperty() != null;
        if (hasThemeSupport) {
            colorResolver = new ThemeColorResolver(probeHost, themeManager, COLOR_TOKENS);
        } else {
            colorResolver = null;
        }

        // Resolve initial colors
        Color primary = resolveColor("app-primary");
        Color dot = resolveColor("app-dot");
        Color textColor = resolveColor("app-text");

        // Create all layers
        particleCanvas = createHalfResCanvas(root);
        particleSystem = new MenuParticleSystem(60, primary, dot);
        silhouetteLayer = new MenuSilhouetteLayer(textColor);
        silhouetteLayer.setMouseTransparent(true);

        root.getChildren().addAll(particleCanvas, silhouetteLayer, probeHost);

        // Listen for theme changes
        if (hasThemeSupport) {
            themeManager.currentThemeProperty().addListener((obs, oldTheme, newTheme) ->
                    updateThemeColors());
        }
    }

    /**
     * Returns the root {@link StackPane} containing all background layers.
     *
     * <p>The root is mouse-transparent so that UI elements layered on top
     * can receive input events normally.</p>
     *
     * @return the root StackPane, never {@code null}
     */
    public StackPane getRoot() {
        return root;
    }

    /**
     * Starts the background animations.
     *
     * <p>Starts the {@link Timeline} that drives particle rendering,
     * and starts the silhouette drift animations.</p>
     *
     * <p>This method is a no-op if the background is already running.</p>
     */
    public void start() {
        if (running) {
            return;
        }

        // Create and start the animation timeline (~20 FPS at 50ms interval).
        // Unlike AnimationTimer, a Timeline does not trigger JavaFX pulses at the
        // monitor's refresh rate — it only fires at the configured interval.
        animationTimeline = new Timeline(new KeyFrame(FRAME_DURATION, event -> {
            long now = System.nanoTime();
            double pW = particleCanvas.getWidth();
            double pH = particleCanvas.getHeight();
            GraphicsContext pGc = particleCanvas.getGraphicsContext2D();
            particleSystem.update(now);
            particleSystem.render(pGc, pW, pH);
        }));
        animationTimeline.setCycleCount(Animation.INDEFINITE);
        animationTimeline.play();
        silhouetteLayer.startAnimations();
        running = true;
    }

    /**
     * Stops all background animations.
     *
     * <p>Stops the {@link Timeline} and silhouette transitions. This
     * method is idempotent -- calling it when already stopped is a no-op.</p>
     */
    public void stop() {
        if (animationTimeline != null) {
            animationTimeline.stop();
            animationTimeline = null;
        }
        silhouetteLayer.stopAll();
        running = false;
    }

    /**
     * Returns whether the background animations are currently active.
     *
     * @return {@code true} if the animation timer is running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Enables or disables accelerated mode (approximately 3x speed) on all
     * animated sub-systems.
     *
     * <p>Intended for use during exit transitions to create a sense of
     * forward momentum as the screen fades out. Affects particle velocities
     * and silhouette drift speed.</p>
     *
     * @param accelerated {@code true} to engage 3x speed, {@code false} to
     *                    restore normal speed
     */
    public void setAccelerated(boolean accelerated) {
        particleSystem.setAccelerated(accelerated);
        if (accelerated) {
            silhouetteLayer.accelerate();
        } else {
            silhouetteLayer.resetSpeed();
        }
    }

    /**
     * Re-resolves CSS colors from the current theme and updates all renderers.
     *
     * <p>Call this after the root StackPane has been added to the scene graph
     * and CSS has been applied, or whenever the theme changes. The method is
     * also called automatically when the {@link ThemeManager} fires a theme
     * change event.</p>
     */
    public void updateThemeColors() {
        if (colorResolver == null) {
            return;
        }

        Color primary = colorResolver.resolve("app-primary");
        Color dot = colorResolver.resolve("app-dot");
        Color textColor = colorResolver.resolve("app-text");

        particleSystem.updateColors(primary, dot);
        silhouetteLayer.updateColor(textColor);
    }

    /**
     * Stops all animations and disposes the {@link ThemeColorResolver}.
     *
     * <p>After calling this method, the component should not be reused.
     * Calling {@link #isRunning()} will return {@code false}.</p>
     */
    public void dispose() {
        stop();
        if (colorResolver != null) {
            colorResolver.dispose();
        }
    }

    // ---- Private helpers ----

    /**
     * Creates a {@link Canvas} that renders at half the parent's resolution and
     * is scaled up 2x via {@code scaleX}/{@code scaleY} to fill the parent.
     *
     * <p>GPU optimization: rendering at {@value CANVAS_SCALE}x resolution reduces
     * pixel-fill work by 75% (half width × half height). The 2x scale transform
     * bilinearly stretches the rendered content back to full size. Because the
     * particle system renders soft, semi-transparent bokeh dots, the slight
     * blurriness introduced by upscaling is visually indistinguishable from
     * full-resolution output — the particles are blurry by design.</p>
     *
     * @param parent the parent StackPane whose dimensions drive the canvas size
     * @return a new half-resolution Canvas scaled to fill the parent
     * @since GPU optimization (Package 6)
     */
    private Canvas createHalfResCanvas(StackPane parent) {
        Canvas canvas = new Canvas();
        // Bind canvas to half the parent dimensions for reduced pixel-fill cost
        canvas.widthProperty().bind(parent.widthProperty().multiply(CANVAS_SCALE));
        canvas.heightProperty().bind(parent.heightProperty().multiply(CANVAS_SCALE));
        // Scale the canvas back up to fill the parent (2x when CANVAS_SCALE = 0.5)
        canvas.setScaleX(1.0 / CANVAS_SCALE);
        canvas.setScaleY(1.0 / CANVAS_SCALE);
        canvas.setMouseTransparent(true);
        return canvas;
    }

    /**
     * Resolves a CSS color token via the color resolver, or returns a sensible
     * fallback if no resolver is available (e.g. before scene attachment).
     *
     * @param token the CSS color token name
     * @return the resolved {@link Color}
     */
    private Color resolveColor(String token) {
        if (colorResolver != null) {
            return colorResolver.resolve(token);
        }
        return switch (token) {
            case "app-bg" -> Color.web("#080812");
            case "app-primary" -> Color.web("#8b5cf6");
            case "app-dot" -> Color.web("#a78bfa");
            case "app-text" -> Color.web("#ededf8");
            default -> Color.MAGENTA;
        };
    }
}
