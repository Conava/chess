package io.github.conava.chess.application.menu;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Canvas-based floating particle system for the cinematic main menu background.
 *
 * <p>Renders 40-80 bokeh-like dots that drift slowly upward, creating an ambient
 * atmosphere behind the menu UI. All particle data is stored in parallel primitive
 * arrays to avoid per-frame object allocations.</p>
 *
 * <p>Particles use normalised coordinates in the range [0, 1]. The {@link #render}
 * method scales them to the actual canvas dimensions, so the system adapts to any
 * canvas size including zero.</p>
 */
public class MenuParticleSystem {

    private static final int DEFAULT_PARTICLE_COUNT = 60;
    private static final double BOKEH_PROBABILITY = 0.15;

    /** Normalised X positions [0, 1]. */
    private final double[] x;
    /** Normalised Y positions [0, 1]. Values decrease as particles drift upward. */
    private final double[] y;
    /** Normalised X velocities (units per second). */
    private final double[] vx;
    /** Normalised Y velocities (units per second). Negative = upward drift. */
    private final double[] vy;
    /** Particle radii in pixels. Normal: 2-6, bokeh: 8-12. */
    private final double[] size;
    /** Per-particle opacity. Normal: 0.15-0.6, bokeh: 0.05-0.1. */
    private final double[] opacity;
    /** Index into the two-element colour palette (0 = primary, 1 = dot). */
    private final int[] colorIndex;

    private final int count;
    private Color[] colors;
    private long lastNanos;
    private boolean accelerated;

    /**
     * Creates a particle system with the default count of 60 particles and
     * translucent white colours.
     */
    public MenuParticleSystem() {
        this(DEFAULT_PARTICLE_COUNT, Color.gray(0.8), Color.gray(0.9));
    }

    /**
     * Creates a particle system.
     *
     * @param particleCount number of particles to simulate (typically 40-80)
     * @param primary       first colour for particles (derived from theme primary)
     * @param dot           second colour for particles (derived from theme dot/accent)
     */
    public MenuParticleSystem(int particleCount, Color primary, Color dot) {
        this.count = particleCount;
        this.colors = new Color[]{primary, dot};

        this.x = new double[count];
        this.y = new double[count];
        this.vx = new double[count];
        this.vy = new double[count];
        this.size = new double[count];
        this.opacity = new double[count];
        this.colorIndex = new int[count];

        initParticles();
    }

    /**
     * Advances all particle positions based on elapsed time since the last update.
     *
     * <p>Uses delta-time calculation so movement speed is frame-rate independent.
     * The first call to this method establishes the time baseline and performs no
     * movement.</p>
     *
     * @param now current timestamp in nanoseconds (e.g. from {@code AnimationTimer})
     */
    public void update(long now) {
        if (lastNanos == 0) {
            lastNanos = now;
            return;
        }
        double dt = (now - lastNanos) / 1_000_000_000.0;
        lastNanos = now;

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            // Apply slight horizontal wander
            vx[i] += rng.nextDouble(-0.002, 0.002);

            x[i] += vx[i] * dt;
            y[i] += vy[i] * dt;

            // Wrap horizontally
            if (x[i] < 0) x[i] += 1.0;
            if (x[i] > 1.0) x[i] -= 1.0;

            // Respawn at bottom when leaving top
            if (y[i] < 0) {
                respawn(i, rng, true);
            }
        }
    }

    /**
     * Renders all particles onto the given {@link GraphicsContext}.
     *
     * <p>Clears the full canvas area first, then draws each particle as a filled
     * oval with per-particle opacity. Does nothing if either dimension is zero.</p>
     *
     * @param gc     the graphics context of the target canvas
     * @param width  canvas width in pixels
     * @param height canvas height in pixels
     */
    public void render(GraphicsContext gc, double width, double height) {
        gc.clearRect(0, 0, width, height);
        if (width <= 0 || height <= 0) return;

        for (int i = 0; i < count; i++) {
            gc.setGlobalAlpha(opacity[i]);
            gc.setFill(colors[colorIndex[i]]);
            double px = x[i] * width;
            double py = y[i] * height;
            double r = size[i];
            gc.fillOval(px - r / 2, py - r / 2, r, r);
        }
        gc.setGlobalAlpha(1.0);
    }

    /**
     * Toggles accelerated mode for exit transitions.
     *
     * <p>When enabled, all velocities are multiplied by 3. When disabled, they
     * are divided back to their original magnitudes.</p>
     *
     * @param accelerated {@code true} to engage 3x speed, {@code false} to restore normal speed
     */
    public void setAccelerated(boolean accelerated) {
        if (this.accelerated == accelerated) return;
        double factor = accelerated ? 3.0 : (1.0 / 3.0);
        for (int i = 0; i < count; i++) {
            vx[i] *= factor;
            vy[i] *= factor;
        }
        this.accelerated = accelerated;
    }

    /**
     * Updates the two-colour palette used for particle rendering.
     *
     * <p>Call this when the application theme changes so that particles
     * match the new colour scheme.</p>
     *
     * @param primary new primary colour
     * @param dot     new dot/accent colour
     */
    public void updateColors(Color primary, Color dot) {
        this.colors = new Color[]{primary, dot};
    }

    // --- Private helpers ---

    private void initParticles() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            respawn(i, rng, false);
            // Distribute initial Y across full canvas (not just bottom)
            y[i] = rng.nextDouble();
        }
    }

    /**
     * Initialises or re-initialises a single particle with randomised properties.
     *
     * @param i          particle index
     * @param rng        random source
     * @param atBottom   if {@code true}, place particle at the bottom edge (y near 1.0);
     *                   otherwise assign a random Y
     */
    private void respawn(int i, ThreadLocalRandom rng, boolean atBottom) {
        x[i] = rng.nextDouble();
        y[i] = atBottom ? (0.95 + rng.nextDouble() * 0.05) : rng.nextDouble();

        boolean bokeh = rng.nextDouble() < BOKEH_PROBABILITY;

        if (bokeh) {
            size[i] = rng.nextDouble(8.0, 12.0);
            opacity[i] = rng.nextDouble(0.05, 0.1);
        } else {
            size[i] = rng.nextDouble(2.0, 6.0);
            opacity[i] = rng.nextDouble(0.15, 0.6);
        }

        // Slow upward drift: -0.02 to -0.08 normalised units/sec
        vy[i] = -rng.nextDouble(0.02, 0.08);
        // Slight horizontal wander
        vx[i] = rng.nextDouble(-0.01, 0.01);

        if (accelerated) {
            vy[i] *= 3.0;
            vx[i] *= 3.0;
        }

        colorIndex[i] = rng.nextInt(2);
    }
}
