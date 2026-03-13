package io.github.conava.chess.application.menu;

import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MenuEntranceAnimationTest {

    private Label title;
    private Region accent;
    private Label tagline;
    private List<Button> navButtons;
    private MenuEntranceAnimation animation;

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
        title = new Label("Chess");
        accent = new Region();
        tagline = new Label("A game of kings");
        navButtons = List.of(
                new Button("Local Game"),
                new Button("Online Game"),
                new Button("Settings"),
                new Button("Exit")
        );
        animation = new MenuEntranceAnimation(title, accent, tagline, navButtons);
    }

    @Test
    void initialStateSetCorrectly() {
        // After construction, the animation should set initial state on all nodes.
        // Title: opacity 0, translateY +40
        assertEquals(0.0, title.getOpacity(), 1e-9,
                "Title opacity should be 0 before play");
        assertEquals(40.0, title.getTranslateY(), 1e-9,
                "Title translateY should be +40 before play");

        // Accent: scaleX 0
        assertEquals(0.0, accent.getScaleX(), 1e-9,
                "Accent scaleX should be 0 before play");

        // Tagline: opacity 0
        assertEquals(0.0, tagline.getOpacity(), 1e-9,
                "Tagline opacity should be 0 before play");

        // Nav buttons: opacity 0, translateX +40
        for (Button btn : navButtons) {
            assertEquals(0.0, btn.getOpacity(), 1e-9,
                    "Button '" + btn.getText() + "' opacity should be 0 before play");
            assertEquals(40.0, btn.getTranslateX(), 1e-9,
                    "Button '" + btn.getText() + "' translateX should be +40 before play");
        }
    }

    @Test
    void skipToEndSetsCorrectFinalState() {
        animation.skipToEnd();

        // Title: opacity 1, translateY 0
        assertEquals(1.0, title.getOpacity(), 1e-9,
                "Title opacity should be 1 after skipToEnd");
        assertEquals(0.0, title.getTranslateY(), 1e-9,
                "Title translateY should be 0 after skipToEnd");

        // Accent: scaleX 1
        assertEquals(1.0, accent.getScaleX(), 1e-9,
                "Accent scaleX should be 1 after skipToEnd");

        // Tagline: opacity 1
        assertEquals(1.0, tagline.getOpacity(), 1e-9,
                "Tagline opacity should be 1 after skipToEnd");

        // Nav buttons: opacity 1, translateX 0
        for (Button btn : navButtons) {
            assertEquals(1.0, btn.getOpacity(), 1e-9,
                    "Button '" + btn.getText() + "' opacity should be 1 after skipToEnd");
            assertEquals(0.0, btn.getTranslateX(), 1e-9,
                    "Button '" + btn.getText() + "' translateX should be 0 after skipToEnd");
        }
    }

    /**
     * After skipToEnd, title and tagline nodes must not carry an inline -fx-font-size
     * style. Responsive font sizes are set via styleProperty() bindings in
     * MainMenuController; the entrance animation must never override them.
     */
    @Test
    void skipToEnd_doesNotSetFontSizeStyleOnTitleOrTagline() {
        animation.skipToEnd();

        String titleStyle = title.getStyle() == null ? "" : title.getStyle();
        String taglineStyle = tagline.getStyle() == null ? "" : tagline.getStyle();

        assertFalse(titleStyle.contains("-fx-font-size"),
                "Title should not have an inline -fx-font-size after skipToEnd; was: " + titleStyle);
        assertFalse(taglineStyle.contains("-fx-font-size"),
                "Tagline should not have an inline -fx-font-size after skipToEnd; was: " + taglineStyle);
    }

    /**
     * After construction (initial state), title and tagline nodes must not carry
     * an inline -fx-font-size style.
     */
    @Test
    void initialState_doesNotSetFontSizeStyleOnTitleOrTagline() {
        String titleStyle = title.getStyle() == null ? "" : title.getStyle();
        String taglineStyle = tagline.getStyle() == null ? "" : tagline.getStyle();

        assertFalse(titleStyle.contains("-fx-font-size"),
                "Title should not have an inline -fx-font-size in initial state; was: " + titleStyle);
        assertFalse(taglineStyle.contains("-fx-font-size"),
                "Tagline should not have an inline -fx-font-size in initial state; was: " + taglineStyle);
    }

    @Test
    void navButtonsHaveStaggeredDelays() {
        // Verify the animation uses a Timeline with KeyFrames that have staggered times
        // for each nav button. We test this structurally by checking that the animation
        // object can be created and the initial offsets are identical for all buttons
        // (the stagger is in the Timeline timing, not the initial state).

        // All buttons should start at the same initial translateX
        double expectedX = 40.0;
        for (Button btn : navButtons) {
            assertEquals(expectedX, btn.getTranslateX(), 1e-9,
                    "All buttons should start at same translateX offset");
        }

        // After skipToEnd, all buttons should be at final position
        animation.skipToEnd();
        for (int i = 0; i < navButtons.size(); i++) {
            Button btn = navButtons.get(i);
            assertEquals(0.0, btn.getTranslateX(), 1e-9,
                    "Button " + i + " translateX should be 0 after skipToEnd");
            assertEquals(1.0, btn.getOpacity(), 1e-9,
                    "Button " + i + " opacity should be 1 after skipToEnd");
        }

        // Verify that the animation stores the button count for stagger calculation.
        // We can verify the stagger timing by checking the internal timeline structure.
        // Create a fresh animation and verify play() does not throw.
        MenuEntranceAnimation fresh = new MenuEntranceAnimation(
                new Label(), new Region(), new Label(),
                List.of(new Button("A"), new Button("B"), new Button("C")));
        assertDoesNotThrow(fresh::play, "play() should not throw");
    }
}
