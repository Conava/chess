package io.github.conava.chess.application.menu;

import io.github.conava.chess.application.theme.ThemeManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link CinematicBackground}.
 *
 * <p>These tests require the JavaFX toolkit to be initialized because
 * the component creates Canvas and Pane nodes. All tests use a mocked
 * {@link ThemeManager} dependency.</p>
 */
class CinematicBackgroundTest {

    private ThemeManager themeManager;

    @BeforeAll
    static void initToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Toolkit already initialized
        }
    }

    @BeforeEach
    void setUp() {
        themeManager = mock(ThemeManager.class);
        when(themeManager.currentThemeProperty()).thenReturn(new SimpleObjectProperty<>());
    }

    @Test
    void constructor_createsRootWithExpectedLayers() {
        CinematicBackground bg = new CinematicBackground(themeManager);
        StackPane root = bg.getRoot();
        // particleCanvas, silhouetteLayer, probeHost = 3
        assertEquals(3, root.getChildren().size(),
                "Root should have 3 children (particle canvas, silhouette layer, probe host)");
    }

    @Test
    void start_setsRunningTrue() {
        CinematicBackground bg = new CinematicBackground(themeManager);
        bg.start();
        assertTrue(bg.isRunning(), "isRunning() should be true after start()");
        bg.stop(); // cleanup
    }

    @Test
    void stop_setsRunningFalse() {
        CinematicBackground bg = new CinematicBackground(themeManager);
        bg.start();
        bg.stop();
        assertFalse(bg.isRunning(), "isRunning() should be false after stop()");
    }

    @Test
    void stop_isIdempotent() {
        CinematicBackground bg = new CinematicBackground(themeManager);
        bg.start();
        bg.stop();
        assertDoesNotThrow(bg::stop, "Calling stop() twice should not throw");
        assertFalse(bg.isRunning());
    }

    @Test
    void getRoot_returnsMouseTransparentStackPane() {
        CinematicBackground bg = new CinematicBackground(themeManager);
        StackPane root = bg.getRoot();
        assertNotNull(root, "getRoot() should not return null");
        assertTrue(root.isMouseTransparent(), "Root StackPane should be mouse-transparent");
    }

    @Test
    void accelerate_isForwardedToSubsystems() {
        CinematicBackground bg = new CinematicBackground(themeManager);
        // Should not throw even before start
        assertDoesNotThrow(() -> bg.setAccelerated(true),
                "setAccelerated should not throw");
        // Verify the component continues to function
        bg.start();
        bg.setAccelerated(true);
        assertTrue(bg.isRunning(), "Should still be running after setAccelerated");
        bg.stop();
    }

    @Test
    void dispose_stopsAndCleansUp() {
        CinematicBackground bg = new CinematicBackground(themeManager);
        bg.start();
        assertTrue(bg.isRunning(), "Should be running before dispose");
        bg.dispose();
        assertFalse(bg.isRunning(), "isRunning() should be false after dispose()");
    }

    // ---- Package 6: GPU Optimization tests ----

    /**
     * Verifies that {@code FRAME_DURATION} is set to 50ms (~20 FPS) rather
     * than the previous 33ms (~30 FPS), reducing GPU load by skipping frames.
     */
    @Test
    void frameDuration_is50ms() throws Exception {
        Field field = CinematicBackground.class.getDeclaredField("FRAME_DURATION");
        field.setAccessible(true);
        Duration frameDuration = (Duration) field.get(null);
        assertEquals(50.0, frameDuration.toMillis(), 0.001,
                "FRAME_DURATION should be 50ms for ~20 FPS GPU-optimized rendering");
    }

    /**
     * Verifies that the particle canvas dimensions are bound to half the parent's
     * dimensions. Given a parent resized to 1000x800, the canvas should report
     * 500x400, saving 75% of pixel-fill work compared to full-resolution rendering.
     */
    @Test
    void particleCanvas_isHalfResolution() {
        // Create the background — this triggers createHalfResCanvas internally
        CinematicBackground bg = new CinematicBackground(themeManager);
        StackPane root = bg.getRoot();

        // The first child of root is the particleCanvas
        Canvas particleCanvas = (Canvas) root.getChildren().get(0);

        // Verify the canvas properties are bound at all
        assertTrue(particleCanvas.widthProperty().isBound(),
                "Canvas widthProperty should be bound (to half parent width)");
        assertTrue(particleCanvas.heightProperty().isBound(),
                "Canvas heightProperty should be bound (to half parent height)");

        // Resize the root to a known dimension so the bindings produce predictable values.
        // StackPane.resize() sets the layout bounds, which the widthProperty reflects.
        root.resize(1000, 800);

        // Canvas should be half the parent: 1000*0.5 = 500, 800*0.5 = 400
        assertEquals(500.0, particleCanvas.getWidth(), 0.001,
                "Canvas width should be half the parent width (1000 * 0.5 = 500)");
        assertEquals(400.0, particleCanvas.getHeight(), 0.001,
                "Canvas height should be half the parent height (800 * 0.5 = 400)");
    }

    /**
     * Verifies that the particle canvas has a fixed scale transform of 2.0 on
     * both axes, which doubles the half-resolution canvas back up to fill the
     * parent, making the GPU optimization invisible to the user.
     */
    @Test
    void particleCanvas_hasDoubleScale() {
        CinematicBackground bg = new CinematicBackground(themeManager);
        StackPane root = bg.getRoot();

        // The first child of root is the particleCanvas
        Canvas particleCanvas = (Canvas) root.getChildren().get(0);

        assertEquals(2.0, particleCanvas.getScaleX(), 0.001,
                "Canvas scaleX should be 2.0 to upscale the half-res canvas to full size");
        assertEquals(2.0, particleCanvas.getScaleY(), 0.001,
                "Canvas scaleY should be 2.0 to upscale the half-res canvas to full size");
    }
}
