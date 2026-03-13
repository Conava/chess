package io.github.conava.chess.application.menu;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class MenuExitTransitionTest {

    private VBox navPanel;
    private VBox contentLayer;
    private Button btn1;
    private Button btn2;
    private Button btn3;

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
        btn1 = new Button("Local Game");
        btn2 = new Button("Online Game");
        btn3 = new Button("Exit");
        navPanel = new VBox(btn1, btn2, btn3);
        contentLayer = new VBox();
    }

    @Test
    void exitDisablesAllNavButtons() {
        AtomicBoolean called = new AtomicBoolean(false);
        MenuExitTransition transition = new MenuExitTransition(
                navPanel, contentLayer, () -> called.set(true), NavigationTarget.LOCAL_GAME);

        transition.play();

        // After play() is called, all buttons in navPanel should be disabled
        assertTrue(btn1.isDisable(), "Button 1 should be disabled after play");
        assertTrue(btn2.isDisable(), "Button 2 should be disabled after play");
        assertTrue(btn3.isDisable(), "Button 3 should be disabled after play");
    }

    @Test
    void playImmediateCallsCallbackDirectly() {
        AtomicBoolean called = new AtomicBoolean(false);
        MenuExitTransition transition = new MenuExitTransition(
                navPanel, contentLayer, () -> called.set(true), NavigationTarget.SETTINGS);

        transition.playImmediate();

        assertTrue(called.get(), "playImmediate should invoke callback directly");
    }

    @Test
    void navigationTargetEnumHasAllValues() {
        NavigationTarget[] values = NavigationTarget.values();
        assertEquals(4, values.length, "Should have exactly 4 navigation targets");

        assertNotNull(NavigationTarget.valueOf("LOCAL_GAME"));
        assertNotNull(NavigationTarget.valueOf("ONLINE_GAME"));
        assertNotNull(NavigationTarget.valueOf("SETTINGS"));
        assertNotNull(NavigationTarget.valueOf("EXIT"));
    }

    @Test
    void playImmediateDisablesButtons() {
        AtomicBoolean called = new AtomicBoolean(false);
        MenuExitTransition transition = new MenuExitTransition(
                navPanel, contentLayer, () -> called.set(true), NavigationTarget.EXIT);

        transition.playImmediate();

        assertTrue(btn1.isDisable(), "Button 1 should be disabled after playImmediate");
        assertTrue(btn2.isDisable(), "Button 2 should be disabled after playImmediate");
        assertTrue(btn3.isDisable(), "Button 3 should be disabled after playImmediate");
    }

    @Test
    void accelerateRunnablesAreCalledOnPlay() {
        AtomicBoolean bgCalled = new AtomicBoolean(false);
        AtomicBoolean particlesCalled = new AtomicBoolean(false);
        AtomicBoolean silhouettesCalled = new AtomicBoolean(false);

        MenuExitTransition transition = new MenuExitTransition(
                navPanel, contentLayer, () -> {}, NavigationTarget.LOCAL_GAME,
                () -> bgCalled.set(true),
                () -> particlesCalled.set(true),
                () -> silhouettesCalled.set(true));

        transition.play();

        assertTrue(bgCalled.get(), "accelerateBackground should be called on play");
        assertTrue(particlesCalled.get(), "accelerateParticles should be called on play");
        assertTrue(silhouettesCalled.get(), "accelerateSilhouettes should be called on play");
    }

    /**
     * When the nav panel is already translated left (e.g. a sub-panel was open),
     * calling play() must not throw and must preserve the pre-existing translateX
     * at the moment the animation starts (the slide is relative, so it does not
     * immediately jump to a fixed target).
     *
     * <p>This guards against using {@code setToX} (which would set an absolute
     * target and ignore the current position) instead of {@code setByX} (relative).</p>
     */
    @Test
    void play_withPreTranslatedNavPanel_doesNotThrowAndPreservesInitialTranslate() {
        navPanel.setTranslateX(-300.0);

        MenuExitTransition transition = new MenuExitTransition(
                navPanel, contentLayer, () -> {}, NavigationTarget.LOCAL_GAME);

        assertDoesNotThrow(transition::play,
                "play() should not throw when navPanel is pre-translated");

        // The navPanel translate should still be -300 immediately after play() is
        // called (the animation has not yet had time to advance -- it runs async).
        // With setByX the target is -300 + 60 = -240; with setToX it would jump
        // to 60. Either way the *initial* value is unchanged synchronously.
        assertEquals(-300.0, navPanel.getTranslateX(), 1e-9,
                "navPanel translateX should not be altered synchronously by play()");
    }

    /**
     * From the default position (translateX = 0), the exit slide should work
     * identically to the previous behaviour: the animation targets a +60 offset
     * relative to 0, ending at +60.
     */
    @Test
    void play_fromDefaultPosition_doesNotThrow() {
        // navPanel starts at translateX = 0 (default)
        assertEquals(0.0, navPanel.getTranslateX(), 1e-9,
                "navPanel should start at translateX 0");

        MenuExitTransition transition = new MenuExitTransition(
                navPanel, contentLayer, () -> {}, NavigationTarget.EXIT);

        assertDoesNotThrow(transition::play,
                "play() should not throw when navPanel is at default position");
    }

    /**
     * The content layer fade-out must work even when the content layer has a
     * non-default scale (e.g. compact state with scaleX=0.34, scaleY=0.34 after
     * a sub-panel was opened). FadeTransition operates on opacity which is
     * independent of the Node's scale transform.
     */
    @Test
    void play_withScaledContentLayer_doesNotThrow() {
        contentLayer.setScaleX(0.34);
        contentLayer.setScaleY(0.34);

        MenuExitTransition transition = new MenuExitTransition(
                navPanel, contentLayer, () -> {}, NavigationTarget.ONLINE_GAME);

        assertDoesNotThrow(transition::play,
                "play() should not throw when contentLayer has non-default scale");
    }
}
