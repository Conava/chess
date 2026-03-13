package io.github.conava.chess.application.menu;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MenuLayoutTransition}.
 *
 * <p>Tests operate on the synchronous skipToOpen/skipToClose methods and reduced-motion
 * mode to avoid Timeline timing dependencies. All property assertions are deterministic.</p>
 *
 * <p>The v2 translate+scale model keeps {@code contentLayer} permanently in its original
 * parent (rootPane) and animates it to the compact position via translateX/Y and
 * scaleX/Y. No reparenting ever occurs. Assertions verify translate/scale values and
 * the absence of old reparenting-era CSS classes.</p>
 */
class MenuLayoutTransitionTest {

    private static final double NAV_TRANSLATE_TARGET   = -280.0;
    private static final double COMPACT_TRANSLATE_X    = 50.0;
    private static final double COMPACT_TRANSLATE_Y    = -200.0;
    private static final double COMPACT_SCALE          = 0.5;

    private VBox navPanel;
    private VBox contentLayer;
    private StackPane rootPane;
    private DoubleBinding navTranslateBinding;
    private DoubleBinding compactTranslateXBinding;
    private DoubleBinding compactTranslateYBinding;
    private DoubleBinding compactScaleBinding;

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
        navPanel = new VBox();
        contentLayer = new VBox();
        rootPane = new StackPane();
        // contentLayer starts in rootPane — parent must be set before constructing the transition
        rootPane.getChildren().add(contentLayer);

        navTranslateBinding       = Bindings.createDoubleBinding(() -> NAV_TRANSLATE_TARGET);
        compactTranslateXBinding  = Bindings.createDoubleBinding(() -> COMPACT_TRANSLATE_X);
        compactTranslateYBinding  = Bindings.createDoubleBinding(() -> COMPACT_TRANSLATE_Y);
        compactScaleBinding       = Bindings.createDoubleBinding(() -> COMPACT_SCALE);
    }

    /**
     * Creates a {@link MenuLayoutTransition} using the v2 constructor (translate+scale model).
     *
     * @param reducedMotion whether to enable reduced-motion mode
     * @return a new transition wired to the test fixtures
     */
    private MenuLayoutTransition makeTransition(boolean reducedMotion) {
        return new MenuLayoutTransition(
                navPanel,
                contentLayer,
                navTranslateBinding,
                compactTranslateXBinding,
                compactTranslateYBinding,
                compactScaleBinding,
                Duration.millis(300),
                reducedMotion
        );
    }

    // ------------------------------------------------------------------
    // 1. isOpen state tests
    // ------------------------------------------------------------------

    /** Test 1: isOpen returns false on a fresh instance. */
    @Test
    void isOpen_initiallyFalse() {
        MenuLayoutTransition transition = makeTransition(false);
        assertFalse(transition.isOpen(), "isOpen should be false before any animation");
    }

    /** Test 2: isOpen returns true after skipToOpen. */
    @Test
    void isOpen_trueAfterSkipToOpen() {
        MenuLayoutTransition transition = makeTransition(false);
        transition.skipToOpen();
        assertTrue(transition.isOpen(), "isOpen should be true after skipToOpen");
    }

    /** Test 3: isOpen returns false after skipToOpen then skipToClose. */
    @Test
    void isOpen_falseAfterSkipToClose() {
        MenuLayoutTransition transition = makeTransition(false);
        transition.skipToOpen();
        transition.skipToClose();
        assertFalse(transition.isOpen(), "isOpen should be false after skipToClose");
    }

    // ------------------------------------------------------------------
    // 2. skipToOpen property assertions
    // ------------------------------------------------------------------

    /** Test 4: navPanel.translateX equals navTranslateTarget after skipToOpen. */
    @Test
    void skipToOpen_navPanelTranslateX_matchesTarget() {
        MenuLayoutTransition transition = makeTransition(false);
        transition.skipToOpen();
        assertEquals(NAV_TRANSLATE_TARGET, navPanel.getTranslateX(), 1e-9,
                "navPanel translateX should match the nav translate target after skipToOpen");
    }

    /** Test 5: contentLayer.translateX equals compactTranslateX after skipToOpen. */
    @Test
    void skipToOpen_contentLayerTranslateX_matchesCompactTarget() {
        MenuLayoutTransition transition = makeTransition(false);
        transition.skipToOpen();
        assertEquals(COMPACT_TRANSLATE_X, contentLayer.getTranslateX(), 1e-9,
                "contentLayer translateX should match compactTranslateX after skipToOpen");
    }

    /** Test 6: contentLayer.translateY equals compactTranslateY after skipToOpen. */
    @Test
    void skipToOpen_contentLayerTranslateY_matchesCompactTarget() {
        MenuLayoutTransition transition = makeTransition(false);
        transition.skipToOpen();
        assertEquals(COMPACT_TRANSLATE_Y, contentLayer.getTranslateY(), 1e-9,
                "contentLayer translateY should match compactTranslateY after skipToOpen");
    }

    /** Test 7: contentLayer.scaleX and scaleY both equal compactScale after skipToOpen. */
    @Test
    void skipToOpen_contentLayerScale_matchesCompactTarget() {
        MenuLayoutTransition transition = makeTransition(false);
        transition.skipToOpen();
        assertEquals(COMPACT_SCALE, contentLayer.getScaleX(), 1e-9,
                "contentLayer scaleX should match compactScale after skipToOpen");
        assertEquals(COMPACT_SCALE, contentLayer.getScaleY(), 1e-9,
                "contentLayer scaleY should match compactScale after skipToOpen");
    }

    // ------------------------------------------------------------------
    // 3. skipToClose property assertions
    // ------------------------------------------------------------------

    /** Test 8: navPanel.translateX is 0 after skipToClose. */
    @Test
    void skipToClose_navPanelTranslateX_isZero() {
        MenuLayoutTransition transition = makeTransition(false);
        transition.skipToOpen();
        transition.skipToClose();
        assertEquals(0.0, navPanel.getTranslateX(), 1e-9,
                "navPanel translateX should be 0 after skipToClose");
    }

    /** Test 9: contentLayer.translateX is 0 after skipToClose. */
    @Test
    void skipToClose_contentLayerTranslateX_isZero() {
        MenuLayoutTransition transition = makeTransition(false);
        transition.skipToOpen();
        transition.skipToClose();
        assertEquals(0.0, contentLayer.getTranslateX(), 1e-9,
                "contentLayer translateX should be 0 after skipToClose");
    }

    /** Test 10: contentLayer.translateY is 0 after skipToClose. */
    @Test
    void skipToClose_contentLayerTranslateY_isZero() {
        MenuLayoutTransition transition = makeTransition(false);
        transition.skipToOpen();
        transition.skipToClose();
        assertEquals(0.0, contentLayer.getTranslateY(), 1e-9,
                "contentLayer translateY should be 0 after skipToClose");
    }

    /** Test 11: contentLayer.scaleX and scaleY are 1.0 after skipToClose. */
    @Test
    void skipToClose_contentLayerScale_isOne() {
        MenuLayoutTransition transition = makeTransition(false);
        transition.skipToOpen();
        transition.skipToClose();
        assertEquals(1.0, contentLayer.getScaleX(), 1e-9,
                "contentLayer scaleX should be 1.0 after skipToClose");
        assertEquals(1.0, contentLayer.getScaleY(), 1e-9,
                "contentLayer scaleY should be 1.0 after skipToClose");
    }

    // ------------------------------------------------------------------
    // 4. Critical regression: no reparenting
    // ------------------------------------------------------------------

    /**
     * Test 12: contentLayer.getParent() must never change.
     *
     * <p>This is the primary regression guard for the v2 model. The old reparenting
     * model moved contentLayer from rootPane into navPanel when open. The v2 model
     * must never do this.</p>
     */
    @Test
    void skipToOpen_contentLayerParent_unchanged() {
        MenuLayoutTransition transition = makeTransition(false);
        assertSame(rootPane, contentLayer.getParent(),
                "contentLayer should start in rootPane");
        transition.skipToOpen();
        assertSame(rootPane, contentLayer.getParent(),
                "contentLayer must still be in rootPane after skipToOpen — no reparenting in v2");
        transition.skipToClose();
        assertSame(rootPane, contentLayer.getParent(),
                "contentLayer must still be in rootPane after skipToClose");
    }

    // ------------------------------------------------------------------
    // 5. Idempotency tests
    // ------------------------------------------------------------------

    /** Test 13: calling skipToOpen twice does not throw and state is correct. */
    @Test
    void doubleOpen_isIdempotent() {
        MenuLayoutTransition transition = makeTransition(false);
        assertDoesNotThrow(() -> {
            transition.skipToOpen();
            transition.skipToOpen();
        }, "Calling skipToOpen twice should not throw");
        assertTrue(transition.isOpen(), "isOpen should still be true after double skipToOpen");
        assertEquals(NAV_TRANSLATE_TARGET, navPanel.getTranslateX(), 1e-9,
                "navPanel translateX should still match target after double skipToOpen");
        assertSame(rootPane, contentLayer.getParent(),
                "contentLayer parent must remain rootPane after double skipToOpen");
    }

    /** Test 14: calling skipToClose twice does not throw and state is correct. */
    @Test
    void doubleClose_isIdempotent() {
        MenuLayoutTransition transition = makeTransition(false);
        assertDoesNotThrow(() -> {
            transition.skipToClose();
            transition.skipToClose();
        }, "Calling skipToClose twice should not throw");
        assertFalse(transition.isOpen(), "isOpen should still be false after double skipToClose");
        assertEquals(0.0, navPanel.getTranslateX(), 1e-9,
                "navPanel translateX should remain 0 after double skipToClose");
    }

    // ------------------------------------------------------------------
    // 6. Opacity invariant
    // ------------------------------------------------------------------

    /** Test 15: contentLayer opacity stays at 1.0 through the full open/close cycle. */
    @Test
    void contentLayerOpacity_neverChanges() {
        MenuLayoutTransition transition = makeTransition(false);
        assertEquals(1.0, contentLayer.getOpacity(), 1e-9,
                "contentLayer opacity should start at 1.0");
        transition.skipToOpen();
        assertEquals(1.0, contentLayer.getOpacity(), 1e-9,
                "contentLayer opacity should remain 1.0 after skipToOpen");
        transition.skipToClose();
        assertEquals(1.0, contentLayer.getOpacity(), 1e-9,
                "contentLayer opacity should remain 1.0 after skipToClose");
    }

    // ------------------------------------------------------------------
    // 7. Reduced motion tests
    // ------------------------------------------------------------------

    /**
     * Test 16: reduced motion animateOpen sets all properties synchronously.
     *
     * <p>In reduced-motion mode, animateOpen must delegate to skipToOpen synchronously,
     * so all translate/scale values reflect the compact targets immediately.</p>
     */
    @Test
    void animateOpen_reducedMotion_setsPropertiesImmediately() {
        MenuLayoutTransition transition = makeTransition(true);
        transition.animateOpen(null);
        assertEquals(NAV_TRANSLATE_TARGET, navPanel.getTranslateX(), 1e-9,
                "navPanel translateX should match target immediately in reduced motion");
        assertEquals(COMPACT_TRANSLATE_X, contentLayer.getTranslateX(), 1e-9,
                "contentLayer translateX should match compact target immediately in reduced motion");
        assertEquals(COMPACT_TRANSLATE_Y, contentLayer.getTranslateY(), 1e-9,
                "contentLayer translateY should match compact target immediately in reduced motion");
        assertEquals(COMPACT_SCALE, contentLayer.getScaleX(), 1e-9,
                "contentLayer scaleX should match compact scale immediately in reduced motion");
        assertEquals(COMPACT_SCALE, contentLayer.getScaleY(), 1e-9,
                "contentLayer scaleY should match compact scale immediately in reduced motion");
        assertTrue(transition.isOpen(), "isOpen should be true after animateOpen in reduced motion");
    }

    /**
     * Test 17: reduced motion animateClose restores all properties to defaults synchronously.
     */
    @Test
    void animateClose_reducedMotion_restoresProperties() {
        MenuLayoutTransition transition = makeTransition(true);
        transition.animateOpen(null);
        transition.animateClose(null);
        assertEquals(0.0, navPanel.getTranslateX(), 1e-9,
                "navPanel translateX should be 0 after animateClose in reduced motion");
        assertEquals(0.0, contentLayer.getTranslateX(), 1e-9,
                "contentLayer translateX should be 0 after animateClose in reduced motion");
        assertEquals(0.0, contentLayer.getTranslateY(), 1e-9,
                "contentLayer translateY should be 0 after animateClose in reduced motion");
        assertEquals(1.0, contentLayer.getScaleX(), 1e-9,
                "contentLayer scaleX should be 1.0 after animateClose in reduced motion");
        assertEquals(1.0, contentLayer.getScaleY(), 1e-9,
                "contentLayer scaleY should be 1.0 after animateClose in reduced motion");
        assertFalse(transition.isOpen(), "isOpen should be false after animateClose in reduced motion");
    }

    // ------------------------------------------------------------------
    // 8. Regression guards — no legacy CSS classes
    // ------------------------------------------------------------------

    /**
     * Test 18: the {@code cinematic-nav-centered} CSS class must never appear on any
     * Button child of navPanel after any operation.
     *
     * <p>This is a regression guard: the v1 model added this class to center nav buttons
     * when the panel was open. v2 removes this behaviour entirely.</p>
     */
    @Test
    void noNavCenteredStyleClass_afterAnyOperation() {
        VBox panelWithButtons = new VBox();
        panelWithButtons.getChildren().addAll(new Button("Play"), new Button("Settings"));
        StackPane localRootPane = new StackPane();
        VBox localContentLayer = new VBox();
        localRootPane.getChildren().add(localContentLayer);

        MenuLayoutTransition transition = new MenuLayoutTransition(
                panelWithButtons,
                localContentLayer,
                navTranslateBinding,
                compactTranslateXBinding,
                compactTranslateYBinding,
                compactScaleBinding,
                Duration.millis(300),
                true  // reduced motion so calls are synchronous
        );

        // skipToOpen
        transition.skipToOpen();
        panelWithButtons.getChildren().stream()
                .filter(n -> n instanceof Button)
                .map(n -> (Button) n)
                .forEach(btn -> assertFalse(
                        btn.getStyleClass().contains("cinematic-nav-centered"),
                        "Button '" + btn.getText() + "' must not have 'cinematic-nav-centered' after skipToOpen (v2 removed this)"));

        // skipToClose
        transition.skipToClose();
        panelWithButtons.getChildren().stream()
                .filter(n -> n instanceof Button)
                .map(n -> (Button) n)
                .forEach(btn -> assertFalse(
                        btn.getStyleClass().contains("cinematic-nav-centered"),
                        "Button '" + btn.getText() + "' must not have 'cinematic-nav-centered' after skipToClose (v2 removed this)"));

        // animateOpen (reduced motion)
        transition.animateOpen(null);
        panelWithButtons.getChildren().stream()
                .filter(n -> n instanceof Button)
                .map(n -> (Button) n)
                .forEach(btn -> assertFalse(
                        btn.getStyleClass().contains("cinematic-nav-centered"),
                        "Button '" + btn.getText() + "' must not have 'cinematic-nav-centered' after animateOpen (v2 removed this)"));

        // animateClose (reduced motion)
        transition.animateClose(null);
        panelWithButtons.getChildren().stream()
                .filter(n -> n instanceof Button)
                .map(n -> (Button) n)
                .forEach(btn -> assertFalse(
                        btn.getStyleClass().contains("cinematic-nav-centered"),
                        "Button '" + btn.getText() + "' must not have 'cinematic-nav-centered' after animateClose (v2 removed this)"));
    }

    /**
     * Test 19: the {@code cinematic-content-compact} CSS class must never appear on
     * contentLayer after any operation.
     *
     * <p>This is a regression guard: the v1 model added this class to contentLayer when
     * it was reparented into the nav panel header. v2 never reparents and never applies
     * this class.</p>
     */
    @Test
    void noContentCompactStyleClass_afterAnyOperation() {
        MenuLayoutTransition transition = makeTransition(true);

        // skipToOpen
        transition.skipToOpen();
        assertFalse(contentLayer.getStyleClass().contains("cinematic-content-compact"),
                "contentLayer must not have 'cinematic-content-compact' after skipToOpen (v2 removed this)");

        // skipToClose
        transition.skipToClose();
        assertFalse(contentLayer.getStyleClass().contains("cinematic-content-compact"),
                "contentLayer must not have 'cinematic-content-compact' after skipToClose (v2 removed this)");

        // animateOpen (reduced motion)
        transition.animateOpen(null);
        assertFalse(contentLayer.getStyleClass().contains("cinematic-content-compact"),
                "contentLayer must not have 'cinematic-content-compact' after animateOpen (v2 removed this)");

        // animateClose (reduced motion)
        transition.animateClose(null);
        assertFalse(contentLayer.getStyleClass().contains("cinematic-content-compact"),
                "contentLayer must not have 'cinematic-content-compact' after animateClose (v2 removed this)");
    }
}
