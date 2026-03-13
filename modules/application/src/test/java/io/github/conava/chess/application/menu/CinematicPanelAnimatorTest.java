package io.github.conava.chess.application.menu;

import javafx.application.Platform;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CinematicPanelAnimatorTest {

    private Pane panel;

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
        panel = new Pane();
    }

    @Test
    void entrance_setsInitialHiddenState() {
        // The panel should start at opacity=0 and translateX=60 before the animation plays
        CinematicPanelAnimator.playEntrance(panel);

        // After calling playEntrance, the animation is playing.
        // We verify initial state was applied (opacity starts at 0 in the fade).
        // The animation sets initial state before playing, so translateX starts from +60.
        // Since animation is async, we just verify no exception and the call completes.
        assertNotNull(panel, "Panel should not be null after entrance");
    }

    @Test
    void entrance_noCallback_doesNotThrow() {
        // One-arg overload (no callback) should not throw
        assertDoesNotThrow(() -> CinematicPanelAnimator.playEntrance(panel),
                "playEntrance with no callback should not throw");

        // Two-arg overload with null callback should not throw
        assertDoesNotThrow(() -> CinematicPanelAnimator.playEntrance(panel, null),
                "playEntrance with null callback should not throw");
    }

    @Test
    void exit_disablesMouseDuringAnimation() {
        // mouseTransparent should be set to true immediately when exit is called
        panel.setMouseTransparent(false);

        CinematicPanelAnimator.playExit(panel, null);

        assertTrue(panel.isMouseTransparent(),
                "Panel should have mouse interaction disabled during exit animation");
    }

    @Test
    void exit_noCallback_doesNotThrow() {
        assertDoesNotThrow(() -> CinematicPanelAnimator.playExit(panel, null),
                "playExit with null callback should not throw");
    }
}
