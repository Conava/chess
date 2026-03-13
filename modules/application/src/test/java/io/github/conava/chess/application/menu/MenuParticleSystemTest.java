package io.github.conava.chess.application.menu;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MenuParticleSystemTest {

    private static final int PARTICLE_COUNT = 60;
    private static final Color PRIMARY = Color.web("#7B2FF2");
    private static final Color DOT = Color.web("#C084FC");

    private MenuParticleSystem system;

    @BeforeEach
    void setUp() {
        system = new MenuParticleSystem(PARTICLE_COUNT, PRIMARY, DOT);
    }

    @Test
    void particleCountWithinConfiguredRange() throws Exception {
        double[] x = getDoubleArray("x");
        double[] y = getDoubleArray("y");
        double[] vx = getDoubleArray("vx");
        double[] vy = getDoubleArray("vy");
        double[] size = getDoubleArray("size");
        double[] opacity = getDoubleArray("opacity");
        int[] colorIndex = getIntArray("colorIndex");

        assertEquals(PARTICLE_COUNT, x.length);
        assertEquals(PARTICLE_COUNT, y.length);
        assertEquals(PARTICLE_COUNT, vx.length);
        assertEquals(PARTICLE_COUNT, vy.length);
        assertEquals(PARTICLE_COUNT, size.length);
        assertEquals(PARTICLE_COUNT, opacity.length);
        assertEquals(PARTICLE_COUNT, colorIndex.length);
    }

    @Test
    void particlesDriftUpwardOverTime() throws Exception {
        // Initialize positions to known middle-of-canvas values so they won't respawn
        double[] y = getDoubleArray("y");
        double[] vy = getDoubleArray("vy");

        // Record initial average Y (particles initialized with random y in [0,1] normalized)
        double initialAvgY = 0;
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            // Place particles in the middle so they won't wrap
            y[i] = 0.5;
            initialAvgY += y[i];
        }
        initialAvgY /= PARTICLE_COUNT;

        // Ensure all vy are negative (upward drift)
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            assertTrue(vy[i] < 0, "Particle " + i + " should have negative vy (upward drift)");
        }

        // Simulate time passing: call update with delta
        long startTime = 1_000_000_000L; // 1 second in nanos
        system.update(startTime);
        // Second update 1 second later
        system.update(startTime + 1_000_000_000L);

        double afterAvgY = 0;
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            afterAvgY += y[i];
        }
        afterAvgY /= PARTICLE_COUNT;

        assertTrue(afterAvgY < initialAvgY,
                "Average Y should decrease (upward drift): was " + initialAvgY + ", now " + afterAvgY);
    }

    @Test
    void particlesRespawnWhenLeavingTop() throws Exception {
        double[] y = getDoubleArray("y");

        // Set a particle above the top boundary
        y[0] = -0.1;

        long startTime = 1_000_000_000L;
        system.update(startTime);
        // Trigger second update to process the out-of-bounds particle
        system.update(startTime + 16_000_000L); // ~16ms frame

        assertTrue(y[0] >= 0.0,
                "Particle that left the top should respawn at bottom (y >= 0), but was " + y[0]);
    }

    @Test
    void particlePropertiesWithinSpec() throws Exception {
        double[] size = getDoubleArray("size");
        double[] opacity = getDoubleArray("opacity");

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            // Size: normal 2-6px OR bokeh 8-12px
            assertTrue(size[i] >= 2.0 && size[i] <= 12.0,
                    "Particle " + i + " size " + size[i] + " out of range [2, 12]");

            // Opacity: normal 0.15-0.6 OR bokeh 0.05-0.1
            assertTrue(opacity[i] >= 0.05 && opacity[i] <= 0.6,
                    "Particle " + i + " opacity " + opacity[i] + " out of range [0.05, 0.6]");
        }
    }

    @Test
    void renderFrameDoesNotAllocateObjects() throws Exception {
        // Structural test: verify parallel arrays are pre-allocated, not created per frame
        Field xField = MenuParticleSystem.class.getDeclaredField("x");
        Field yField = MenuParticleSystem.class.getDeclaredField("y");
        Field vxField = MenuParticleSystem.class.getDeclaredField("vx");
        Field vyField = MenuParticleSystem.class.getDeclaredField("vy");
        Field sizeField = MenuParticleSystem.class.getDeclaredField("size");
        Field opacityField = MenuParticleSystem.class.getDeclaredField("opacity");
        Field colorIndexField = MenuParticleSystem.class.getDeclaredField("colorIndex");

        // All fields should be final (pre-allocated)
        assertTrue(java.lang.reflect.Modifier.isFinal(xField.getModifiers()), "x array should be final");
        assertTrue(java.lang.reflect.Modifier.isFinal(yField.getModifiers()), "y array should be final");
        assertTrue(java.lang.reflect.Modifier.isFinal(vxField.getModifiers()), "vx array should be final");
        assertTrue(java.lang.reflect.Modifier.isFinal(vyField.getModifiers()), "vy array should be final");
        assertTrue(java.lang.reflect.Modifier.isFinal(sizeField.getModifiers()), "size array should be final");
        assertTrue(java.lang.reflect.Modifier.isFinal(opacityField.getModifiers()), "opacity array should be final");
        assertTrue(java.lang.reflect.Modifier.isFinal(colorIndexField.getModifiers()), "colorIndex array should be final");

        // Verify types are primitive arrays, not object arrays
        assertEquals(double[].class, xField.getType());
        assertEquals(double[].class, yField.getType());
        assertEquals(int[].class, colorIndexField.getType());
    }

    @Test
    void accelerateModeMultipliesVelocity() throws Exception {
        double[] vx = getDoubleArray("vx");
        double[] vy = getDoubleArray("vy");

        // Record velocities before acceleration
        double[] vxBefore = vx.clone();
        double[] vyBefore = vy.clone();

        system.setAccelerated(true);

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            assertEquals(vxBefore[i] * 3.0, vx[i], 1e-9,
                    "Particle " + i + " vx should be 3x after acceleration");
            assertEquals(vyBefore[i] * 3.0, vy[i], 1e-9,
                    "Particle " + i + " vy should be 3x after acceleration");
        }

        // Decelerate back
        system.setAccelerated(false);

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            assertEquals(vxBefore[i], vx[i], 1e-9,
                    "Particle " + i + " vx should return to original after deceleration");
            assertEquals(vyBefore[i], vy[i], 1e-9,
                    "Particle " + i + " vy should return to original after deceleration");
        }
    }

    @Test
    void renderOnZeroSizeCanvasDoesNotThrow() {
        GraphicsContext gc = mock(GraphicsContext.class);
        assertDoesNotThrow(() -> system.render(gc, 0.0, 0.0));
    }

    @Test
    void updateColorsDoesNotThrow() {
        assertDoesNotThrow(() -> system.updateColors(Color.RED, Color.BLUE));
    }

    @Test
    void defaultConstructorCreates60Particles() throws Exception {
        MenuParticleSystem defaultSystem = new MenuParticleSystem();
        double[] x = getDoubleArray(defaultSystem, "x");
        assertEquals(60, x.length);
    }

    // --- Reflection helpers ---

    private double[] getDoubleArray(String fieldName) throws Exception {
        return getDoubleArray(system, fieldName);
    }

    private double[] getDoubleArray(Object target, String fieldName) throws Exception {
        Field field = MenuParticleSystem.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (double[]) field.get(target);
    }

    private int[] getIntArray(String fieldName) throws Exception {
        Field field = MenuParticleSystem.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (int[]) field.get(system);
    }
}
