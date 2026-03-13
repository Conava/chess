package io.github.conava.chess.application.menu;

import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.SimpleDoubleProperty;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ResponsiveMenuLayout}.
 *
 * <p>All tests use {@link SimpleDoubleProperty} instances to drive the bindings,
 * so no JavaFX scene or stage is required — only the toolkit needs to be
 * initialised via {@link Platform#startup(Runnable)}.</p>
 */
class ResponsiveMenuLayoutTest {

    @BeforeAll
    static void initToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Toolkit already initialised by another test class
        }
    }

    // -------------------------------------------------------------------------
    // titleFontSize (updated max: 64 → 96)
    // -------------------------------------------------------------------------

    @Test
    void titleFontSize_clampsToMinimumAt400pxWidth() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(400.0);
        DoubleBinding binding = ResponsiveMenuLayout.titleFontSize(width);

        // 400 * 0.05 = 20, but min is 28 → expect 28
        assertEquals(28.0, binding.get(), 1e-9,
                "titleFontSize should clamp to 28 at 400px width");
    }

    @Test
    void titleFontSize_scalesProportionallyAt1000pxWidth() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(1000.0);
        DoubleBinding binding = ResponsiveMenuLayout.titleFontSize(width);

        // 1000 * 0.05 = 50, within [28, 96]
        assertEquals(50.0, binding.get(), 1e-9,
                "titleFontSize should be 50 at 1000px width");
    }

    @Test
    void titleFontSize_clampsToNewMaximumAt2000pxWidth() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(2000.0);
        DoubleBinding binding = ResponsiveMenuLayout.titleFontSize(width);

        // 2000 * 0.05 = 100, but new max is 96 → expect 96
        assertEquals(96.0, binding.get(), 1e-9,
                "titleFontSize should clamp to 96 (new max) at 2000px width");
    }

    @Test
    void titleFontSize_scalesAt1920pxWidth() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(1920.0);
        DoubleBinding binding = ResponsiveMenuLayout.titleFontSize(width);

        // 1920 * 0.05 = 96, exactly at new max
        assertEquals(96.0, binding.get(), 1e-9,
                "titleFontSize should be 96 at 1920px width (at new max)");
    }

    @Test
    void titleFontSize_reactsToWidthChanges() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(1000.0);
        DoubleBinding binding = ResponsiveMenuLayout.titleFontSize(width);

        assertEquals(50.0, binding.get(), 1e-9);

        width.set(400.0);
        assertEquals(28.0, binding.get(), 1e-9,
                "titleFontSize binding should update when width property changes");
    }

    // -------------------------------------------------------------------------
    // taglineFontSize (updated max: 17 → 28)
    // -------------------------------------------------------------------------

    @Test
    void taglineFontSize_clampsToMinimum() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(400.0);
        DoubleBinding binding = ResponsiveMenuLayout.taglineFontSize(width);

        // 400 * 0.014 = 5.6, but min is 11 → expect 11
        assertEquals(11.0, binding.get(), 1e-9,
                "taglineFontSize should clamp to 11 at 400px width");
    }

    @Test
    void taglineFontSize_scalesProportionally() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(1000.0);
        DoubleBinding binding = ResponsiveMenuLayout.taglineFontSize(width);

        // 1000 * 0.014 = 14, within [11, 28]
        assertEquals(14.0, binding.get(), 1e-9,
                "taglineFontSize should be 14 at 1000px width");
    }

    @Test
    void taglineFontSize_clampsToNewMaximum() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(2500.0);
        DoubleBinding binding = ResponsiveMenuLayout.taglineFontSize(width);

        // 2500 * 0.014 = 35, but new max is 28 → expect 28
        assertEquals(28.0, binding.get(), 1e-9,
                "taglineFontSize should clamp to 28 (new max) at 2500px width");
    }

    @Test
    void taglineFontSize_clampsCorrectly() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(400.0);
        DoubleBinding binding = ResponsiveMenuLayout.taglineFontSize(width);

        // 400 * 0.014 = 5.6, but min is 11 → expect 11
        assertEquals(11.0, binding.get(), 1e-9,
                "taglineFontSize should clamp to 11 at 400px width");

        // 1000 * 0.014 = 14, within [11, 28]
        width.set(1000.0);
        assertEquals(14.0, binding.get(), 1e-9,
                "taglineFontSize should be 14 at 1000px width");

        // 2500 * 0.014 = 35, but new max is 28 → expect 28
        width.set(2500.0);
        assertEquals(28.0, binding.get(), 1e-9,
                "taglineFontSize should clamp to 28 at 2500px width");
    }

    // -------------------------------------------------------------------------
    // navButtonFontSize (updated max: 15 → 22)
    // -------------------------------------------------------------------------

    @Test
    void navButtonFontSize_clampsToMinimum() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(400.0);
        DoubleBinding binding = ResponsiveMenuLayout.navButtonFontSize(width);

        // 400 * 0.012 = 4.8, but min is 12 → expect 12
        assertEquals(12.0, binding.get(), 1e-9,
                "navButtonFontSize should clamp to 12 at 400px width");
    }

    @Test
    void navButtonFontSize_scalesProportionally() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(1400.0);
        DoubleBinding binding = ResponsiveMenuLayout.navButtonFontSize(width);

        // 1400 * 0.012 = 16.8, within [12, 22]
        assertEquals(16.8, binding.get(), 1e-6,
                "navButtonFontSize should be 16.8 at 1400px width");
    }

    @Test
    void navButtonFontSize_clampsToNewMaximum() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(2500.0);
        DoubleBinding binding = ResponsiveMenuLayout.navButtonFontSize(width);

        // 2500 * 0.012 = 30, but new max is 22 → expect 22
        assertEquals(22.0, binding.get(), 1e-9,
                "navButtonFontSize should clamp to 22 (new max) at 2500px width");
    }

    @Test
    void navButtonFontSize_scalesWithWidth() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(400.0);
        DoubleBinding binding = ResponsiveMenuLayout.navButtonFontSize(width);

        // 400 * 0.012 = 4.8, but min is 12 → expect 12
        assertEquals(12.0, binding.get(), 1e-9,
                "navButtonFontSize should clamp to 12 at 400px width");

        // 1000 * 0.012 = 12, at lower bound → expect 12
        width.set(1000.0);
        assertEquals(12.0, binding.get(), 1e-9,
                "navButtonFontSize should be 12 at 1000px width");

        // 2500 * 0.012 = 30, but new max is 22 → expect 22
        width.set(2500.0);
        assertEquals(22.0, binding.get(), 1e-9,
                "navButtonFontSize should clamp to 22 (new max) at 2500px width");
    }

    // -------------------------------------------------------------------------
    // navButtonMinHeight (updated max: 52 → 72)
    // -------------------------------------------------------------------------

    @Test
    void navButtonMinHeight_clampsToMinimum() {
        SimpleDoubleProperty height = new SimpleDoubleProperty(400.0);
        DoubleBinding binding = ResponsiveMenuLayout.navButtonMinHeight(height);

        // 400 * 0.065 = 26, but min is 40 → expect 40
        assertEquals(40.0, binding.get(), 1e-9,
                "navButtonMinHeight should clamp to 40 at 400px height");
    }

    @Test
    void navButtonMinHeight_scalesProportionally() {
        SimpleDoubleProperty height = new SimpleDoubleProperty(700.0);
        DoubleBinding binding = ResponsiveMenuLayout.navButtonMinHeight(height);

        // 700 * 0.065 = 45.5, within [40, 72]
        assertEquals(45.5, binding.get(), 1e-9,
                "navButtonMinHeight should be 45.5 at 700px height");
    }

    @Test
    void navButtonMinHeight_clampsToNewMaximum() {
        SimpleDoubleProperty height = new SimpleDoubleProperty(1200.0);
        DoubleBinding binding = ResponsiveMenuLayout.navButtonMinHeight(height);

        // 1200 * 0.065 = 78, but new max is 72 → expect 72
        assertEquals(72.0, binding.get(), 1e-9,
                "navButtonMinHeight should clamp to 72 (new max) at 1200px height");
    }

    @Test
    void navButtonMinHeight_scalesWithHeight() {
        SimpleDoubleProperty height = new SimpleDoubleProperty(400.0);
        DoubleBinding binding = ResponsiveMenuLayout.navButtonMinHeight(height);

        // 400 * 0.065 = 26, but min is 40 → expect 40
        assertEquals(40.0, binding.get(), 1e-9,
                "navButtonMinHeight should clamp to 40 at 400px height");

        // 700 * 0.065 = 45.5, within [40, 72]
        height.set(700.0);
        assertEquals(45.5, binding.get(), 1e-9,
                "navButtonMinHeight should be 45.5 at 700px height");

        // 1200 * 0.065 = 78, but new max is 72 → expect 72
        height.set(1200.0);
        assertEquals(72.0, binding.get(), 1e-9,
                "navButtonMinHeight should clamp to 72 (new max) at 1200px height");
    }

    // -------------------------------------------------------------------------
    // contentLayerLeftPadding (updated max: 72 → 120)
    // -------------------------------------------------------------------------

    @Test
    void contentLayerLeftPadding_clampsToMinimum() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(400.0);
        DoubleBinding binding = ResponsiveMenuLayout.contentLayerLeftPadding(width);

        // 400 * 0.05 = 20, but min is 32 → expect 32
        assertEquals(32.0, binding.get(), 1e-9,
                "contentLayerLeftPadding should clamp to 32 at 400px width");
    }

    @Test
    void contentLayerLeftPadding_scalesProportionally() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(800.0);
        DoubleBinding binding = ResponsiveMenuLayout.contentLayerLeftPadding(width);

        // 800 * 0.05 = 40, within [32, 120]
        assertEquals(40.0, binding.get(), 1e-9,
                "contentLayerLeftPadding should be 40 at 800px width");
    }

    @Test
    void contentLayerLeftPadding_clampsToNewMaximum() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(2600.0);
        DoubleBinding binding = ResponsiveMenuLayout.contentLayerLeftPadding(width);

        // 2600 * 0.05 = 130, but new max is 120 → expect 120
        assertEquals(120.0, binding.get(), 1e-9,
                "contentLayerLeftPadding should clamp to 120 (new max) at 2600px width");
    }

    @Test
    void contentLayerLeftPadding_scalesWithWidth() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(400.0);
        DoubleBinding binding = ResponsiveMenuLayout.contentLayerLeftPadding(width);

        // 400 * 0.05 = 20, but min is 32 → expect 32
        assertEquals(32.0, binding.get(), 1e-9,
                "contentLayerLeftPadding should clamp to 32 at 400px width");

        // 800 * 0.05 = 40, within [32, 120]
        width.set(800.0);
        assertEquals(40.0, binding.get(), 1e-9,
                "contentLayerLeftPadding should be 40 at 800px width");

        // 2600 * 0.05 = 130, but new max is 120 → expect 120
        width.set(2600.0);
        assertEquals(120.0, binding.get(), 1e-9,
                "contentLayerLeftPadding should clamp to 120 (new max) at 2600px width");
    }

    // -------------------------------------------------------------------------
    // navPanelRightMargin (updated max: 48 → 72)
    // -------------------------------------------------------------------------

    @Test
    void navPanelRightMargin_clampsToMinimum() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(400.0);
        DoubleBinding binding = ResponsiveMenuLayout.navPanelRightMargin(width);

        // 400 * 0.035 = 14, but min is 16 → expect 16
        assertEquals(16.0, binding.get(), 1e-9,
                "navPanelRightMargin should clamp to 16 at 400px width");
    }

    @Test
    void navPanelRightMargin_scalesProportionally() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(1000.0);
        DoubleBinding binding = ResponsiveMenuLayout.navPanelRightMargin(width);

        // 1000 * 0.035 = 35, within [16, 72]
        assertEquals(35.0, binding.get(), 1e-9,
                "navPanelRightMargin should be 35 at 1000px width");
    }

    @Test
    void navPanelRightMargin_clampsToNewMaximum() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(2500.0);
        DoubleBinding binding = ResponsiveMenuLayout.navPanelRightMargin(width);

        // 2500 * 0.035 = 87.5, but new max is 72 → expect 72
        assertEquals(72.0, binding.get(), 1e-9,
                "navPanelRightMargin should clamp to 72 (new max) at 2500px width");
    }

    @Test
    void navPanelRightMargin_scalesWithWidth() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(400.0);
        DoubleBinding binding = ResponsiveMenuLayout.navPanelRightMargin(width);

        // 400 * 0.035 = 14, but min is 16 → expect 16
        assertEquals(16.0, binding.get(), 1e-9,
                "navPanelRightMargin should clamp to 16 at 400px width");

        // 1000 * 0.035 = 35, within [16, 72]
        width.set(1000.0);
        assertEquals(35.0, binding.get(), 1e-9,
                "navPanelRightMargin should be 35 at 1000px width");

        // 2500 * 0.035 = 87.5, but new max is 72 → expect 72
        width.set(2500.0);
        assertEquals(72.0, binding.get(), 1e-9,
                "navPanelRightMargin should clamp to 72 (new max) at 2500px width");
    }

    // -------------------------------------------------------------------------
    // navPanelWidth
    // -------------------------------------------------------------------------

    @Test
    void navPanelWidth_isTwentyFivePercentOfRootWidth() {
        SimpleDoubleProperty rootWidth = new SimpleDoubleProperty(1280.0);
        DoubleBinding binding = ResponsiveMenuLayout.navPanelWidth(rootWidth);

        // 1280 * 0.25 = 320
        assertEquals(320.0, binding.get(), 1e-9,
                "navPanelWidth should be exactly 25% of root width at 1280px");
    }

    @Test
    void navPanelWidth_reactsToRootWidthChanges() {
        SimpleDoubleProperty rootWidth = new SimpleDoubleProperty(1280.0);
        DoubleBinding binding = ResponsiveMenuLayout.navPanelWidth(rootWidth);

        assertEquals(320.0, binding.get(), 1e-9);

        rootWidth.set(1920.0);
        // 1920 * 0.25 = 480
        assertEquals(480.0, binding.get(), 1e-9,
                "navPanelWidth binding should update when root width changes");
    }

    @Test
    void navPanelWidth_atSmallSize_isStillTwentyFivePercent() {
        SimpleDoubleProperty rootWidth = new SimpleDoubleProperty(800.0);
        DoubleBinding binding = ResponsiveMenuLayout.navPanelWidth(rootWidth);

        // 800 * 0.25 = 200
        assertEquals(200.0, binding.get(), 1e-9,
                "navPanelWidth should be 25% at 800px root width");
    }

    // -------------------------------------------------------------------------
    // panelContainerWidth
    // -------------------------------------------------------------------------

    @Test
    void panelContainerWidth_usesNavTwentyFivePercentFormula() {
        SimpleDoubleProperty rootWidth = new SimpleDoubleProperty(1280.0);
        DoubleBinding binding = ResponsiveMenuLayout.panelContainerWidth(rootWidth);

        // rw=1280, navWidth=320, edgeGap=16, NAV_PANEL_GAP=16
        // raw = 1280 - 320 - 16 - 16 = 928
        assertEquals(928.0, binding.get(), 1e-9,
                "panelContainerWidth should equal rootWidth - 25% - NAV_PANEL_GAP - edgeGap at 1280px");
    }

    @Test
    void panelContainerWidth_atSmallSizes_doesNotGoNegative() {
        SimpleDoubleProperty rootWidth = new SimpleDoubleProperty(30.0);
        DoubleBinding binding = ResponsiveMenuLayout.panelContainerWidth(rootWidth);

        double panelWidth = binding.get();
        assertTrue(panelWidth >= 0,
                "panelContainerWidth should never be negative; got " + panelWidth);
    }

    @Test
    void panelContainerWidth_reactsToWidthChanges() {
        SimpleDoubleProperty rootWidth = new SimpleDoubleProperty(1000.0);
        DoubleBinding binding = ResponsiveMenuLayout.panelContainerWidth(rootWidth);

        double initial = binding.get();
        rootWidth.set(1500.0);
        double updated = binding.get();

        assertNotEquals(initial, updated,
                "panelContainerWidth binding should update when root width changes");
    }

    @Test
    void panelContainerWidth_atNormalSize_leavesCorrectSpaceForNav() {
        SimpleDoubleProperty rootWidth = new SimpleDoubleProperty(1920.0);
        DoubleBinding binding = ResponsiveMenuLayout.panelContainerWidth(rootWidth);

        // rw=1920, navWidth=480, edgeGap=16, NAV_PANEL_GAP=16
        // raw = 1920 - 480 - 16 - 16 = 1408
        assertEquals(1408.0, binding.get(), 1e-9,
                "panelContainerWidth should equal rootWidth - 25% - NAV_PANEL_GAP - edgeGap at 1920px");
    }

    // -------------------------------------------------------------------------
    // navPanelOpenTranslateX
    // -------------------------------------------------------------------------

    @Test
    void navPanelOpenTranslateX_computedCorrectly() {
        SimpleDoubleProperty rootWidth = new SimpleDoubleProperty(1000.0);
        SimpleDoubleProperty panelContainerWidth = new SimpleDoubleProperty(600.0);
        SimpleDoubleProperty navPanelWidth = new SimpleDoubleProperty(280.0);
        DoubleBinding binding = ResponsiveMenuLayout.navPanelOpenTranslateX(
                rootWidth, panelContainerWidth, navPanelWidth);

        // The nav panel slides left to be to the left of the panel container.
        // translateX should be negative (slides left).
        double tx = binding.get();
        assertTrue(tx < 0,
                "navPanelOpenTranslateX should be negative (slide left); got " + tx);
    }

    @Test
    void navPanelOpenTranslateX_largerPanelMeansMoreLeftSlide() {
        SimpleDoubleProperty rootWidth = new SimpleDoubleProperty(1000.0);
        SimpleDoubleProperty panelContainerWidth = new SimpleDoubleProperty(400.0);
        SimpleDoubleProperty navPanelWidth = new SimpleDoubleProperty(280.0);
        DoubleBinding bindingSmall = ResponsiveMenuLayout.navPanelOpenTranslateX(
                rootWidth, panelContainerWidth, navPanelWidth);
        double txSmallPanel = bindingSmall.get();

        panelContainerWidth.set(700.0);
        double txLargePanel = bindingSmall.get();

        // Larger panel container → nav panel must slide further left (more negative)
        assertTrue(txLargePanel < txSmallPanel,
                "Larger panel container should require larger (more negative) translateX; "
                + "small=" + txSmallPanel + " large=" + txLargePanel);
    }

    // -------------------------------------------------------------------------
    // accentLineWidth (updated max: 72 → 110)
    // -------------------------------------------------------------------------

    @Test
    void accentLineWidth_clampsToMinimum() {
        SimpleDoubleProperty titleFontSize = new SimpleDoubleProperty(28.0);
        DoubleBinding binding = ResponsiveMenuLayout.accentLineWidth(titleFontSize);

        // 28 * 1.1 = 30.8, clamped to [40, 110] → expect 40
        assertEquals(40.0, binding.get(), 1e-9,
                "accentLineWidth should clamp to 40 for titleFontSize=28");
    }

    @Test
    void accentLineWidth_scalesProportionally() {
        SimpleDoubleProperty titleFontSize = new SimpleDoubleProperty(50.0);
        DoubleBinding binding = ResponsiveMenuLayout.accentLineWidth(titleFontSize);

        // 50 * 1.1 = 55, within [40, 110]
        assertEquals(55.0, binding.get(), 1e-9,
                "accentLineWidth should be 55 for titleFontSize=50");
    }

    @Test
    void accentLineWidth_clampsToNewMaximum() {
        SimpleDoubleProperty titleFontSize = new SimpleDoubleProperty(110.0);
        DoubleBinding binding = ResponsiveMenuLayout.accentLineWidth(titleFontSize);

        // 110 * 1.1 = 121, but new max is 110 → expect 110
        assertEquals(110.0, binding.get(), 1e-9,
                "accentLineWidth should clamp to 110 (new max) for titleFontSize=110");
    }

    @Test
    void accentLineWidth_proportionalToTitleSize() {
        SimpleDoubleProperty titleFontSize = new SimpleDoubleProperty(28.0);
        DoubleBinding binding = ResponsiveMenuLayout.accentLineWidth(titleFontSize);

        // 28 * 1.1 = 30.8, clamped to [40, 110] → expect 40
        assertEquals(40.0, binding.get(), 1e-9,
                "accentLineWidth should clamp to 40 for titleFontSize=28");

        // 50 * 1.1 = 55, within [40, 110]
        titleFontSize.set(50.0);
        assertEquals(55.0, binding.get(), 1e-9,
                "accentLineWidth should be 55 for titleFontSize=50");

        // 110 * 1.1 = 121, but new max is 110 → expect 110
        titleFontSize.set(110.0);
        assertEquals(110.0, binding.get(), 1e-9,
                "accentLineWidth should clamp to 110 (new max) for titleFontSize=110");
    }

    @Test
    void accentLineWidth_tracksChangesInTitleFontSize() {
        SimpleDoubleProperty titleFontSize = new SimpleDoubleProperty(50.0);
        DoubleBinding binding = ResponsiveMenuLayout.accentLineWidth(titleFontSize);

        double initial = binding.get();
        titleFontSize.set(60.0);
        double updated = binding.get();

        assertNotEquals(initial, updated,
                "accentLineWidth binding should react to titleFontSize changes");
    }

    // -------------------------------------------------------------------------
    // navButtonIconSize (new): clamp(14, width * 0.014, 24)
    // -------------------------------------------------------------------------

    @Test
    void navButtonIconSize_clampsCorrectly() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(500.0);
        DoubleBinding binding = ResponsiveMenuLayout.navButtonIconSize(width);

        // 500 * 0.014 = 7, but min is 14 → expect 14
        assertEquals(14.0, binding.get(), 1e-9,
                "navButtonIconSize should clamp to min 14 at 500px width");

        // 1400 * 0.014 = 19.6, within [14, 24]
        width.set(1400.0);
        assertEquals(19.6, binding.get(), 1e-6,
                "navButtonIconSize should be 19.6 at 1400px width");

        // 2000 * 0.014 = 28, but max is 24 → expect 24
        width.set(2000.0);
        assertEquals(24.0, binding.get(), 1e-9,
                "navButtonIconSize should clamp to max 24 at 2000px width");
    }

    @Test
    void navButtonIconSize_reactsToWidthChanges() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(1000.0);
        DoubleBinding binding = ResponsiveMenuLayout.navButtonIconSize(width);

        double initial = binding.get();
        width.set(1500.0);
        double updated = binding.get();

        assertNotEquals(initial, updated,
                "navButtonIconSize binding should react to width property changes");
    }

    // -------------------------------------------------------------------------
    // navPanelPadding (new): clamp(16, width * 0.018, 36)
    // -------------------------------------------------------------------------

    @Test
    void navPanelPadding_clampsCorrectly() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(500.0);
        DoubleBinding binding = ResponsiveMenuLayout.navPanelPadding(width);

        // 500 * 0.018 = 9, but min is 16 → expect 16
        assertEquals(16.0, binding.get(), 1e-9,
                "navPanelPadding should clamp to min 16 at 500px width");

        // 1500 * 0.018 = 27, within [16, 36]
        width.set(1500.0);
        assertEquals(27.0, binding.get(), 1e-9,
                "navPanelPadding should be 27 at 1500px width");

        // 2500 * 0.018 = 45, but max is 36 → expect 36
        width.set(2500.0);
        assertEquals(36.0, binding.get(), 1e-9,
                "navPanelPadding should clamp to max 36 at 2500px width");
    }

    @Test
    void navPanelPadding_reactsToWidthChanges() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(1000.0);
        DoubleBinding binding = ResponsiveMenuLayout.navPanelPadding(width);

        double initial = binding.get();
        width.set(1800.0);
        double updated = binding.get();

        assertNotEquals(initial, updated,
                "navPanelPadding binding should react to width property changes");
    }

    // -------------------------------------------------------------------------
    // navPanelSpacing (new): clamp(6, width * 0.007, 14)
    // -------------------------------------------------------------------------

    @Test
    void navPanelSpacing_clampsCorrectly() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(500.0);
        DoubleBinding binding = ResponsiveMenuLayout.navPanelSpacing(width);

        // 500 * 0.007 = 3.5, but min is 6 → expect 6
        assertEquals(6.0, binding.get(), 1e-9,
                "navPanelSpacing should clamp to min 6 at 500px width");

        // 1400 * 0.007 = 9.8, within [6, 14]
        width.set(1400.0);
        assertEquals(9.8, binding.get(), 1e-6,
                "navPanelSpacing should be 9.8 at 1400px width");

        // 2500 * 0.007 = 17.5, but max is 14 → expect 14
        width.set(2500.0);
        assertEquals(14.0, binding.get(), 1e-9,
                "navPanelSpacing should clamp to max 14 at 2500px width");
    }

    @Test
    void navPanelSpacing_reactsToWidthChanges() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(1000.0);
        DoubleBinding binding = ResponsiveMenuLayout.navPanelSpacing(width);

        double initial = binding.get();
        width.set(1800.0);
        double updated = binding.get();

        assertNotEquals(initial, updated,
                "navPanelSpacing binding should react to width property changes");
    }

    // -------------------------------------------------------------------------
    // panelDialogTitleFontSize (new): clamp(18, width * 0.016, 32)
    // -------------------------------------------------------------------------

    @Test
    void panelDialogTitleFontSize_clampsCorrectly() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(500.0);
        DoubleBinding binding = ResponsiveMenuLayout.panelDialogTitleFontSize(width);

        // 500 * 0.016 = 8, but min is 18 → expect 18
        assertEquals(18.0, binding.get(), 1e-9,
                "panelDialogTitleFontSize should clamp to min 18 at 500px width");

        // 1500 * 0.016 = 24, within [18, 32]
        width.set(1500.0);
        assertEquals(24.0, binding.get(), 1e-9,
                "panelDialogTitleFontSize should be 24 at 1500px width");

        // 2500 * 0.016 = 40, but max is 32 → expect 32
        width.set(2500.0);
        assertEquals(32.0, binding.get(), 1e-9,
                "panelDialogTitleFontSize should clamp to max 32 at 2500px width");
    }

    @Test
    void panelDialogTitleFontSize_reactsToWidthChanges() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(1000.0);
        DoubleBinding binding = ResponsiveMenuLayout.panelDialogTitleFontSize(width);

        double initial = binding.get();
        width.set(1800.0);
        double updated = binding.get();

        assertNotEquals(initial, updated,
                "panelDialogTitleFontSize binding should react to width property changes");
    }

    // -------------------------------------------------------------------------
    // panelSectionHeadingFontSize (new): clamp(11, width * 0.009, 18)
    // -------------------------------------------------------------------------

    @Test
    void panelSectionHeadingFontSize_clampsCorrectly() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(500.0);
        DoubleBinding binding = ResponsiveMenuLayout.panelSectionHeadingFontSize(width);

        // 500 * 0.009 = 4.5, but min is 11 → expect 11
        assertEquals(11.0, binding.get(), 1e-9,
                "panelSectionHeadingFontSize should clamp to min 11 at 500px width");

        // 1500 * 0.009 = 13.5, within [11, 18]
        width.set(1500.0);
        assertEquals(13.5, binding.get(), 1e-6,
                "panelSectionHeadingFontSize should be 13.5 at 1500px width");

        // 2500 * 0.009 = 22.5, but max is 18 → expect 18
        width.set(2500.0);
        assertEquals(18.0, binding.get(), 1e-9,
                "panelSectionHeadingFontSize should clamp to max 18 at 2500px width");
    }

    @Test
    void panelSectionHeadingFontSize_reactsToWidthChanges() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(1000.0);
        DoubleBinding binding = ResponsiveMenuLayout.panelSectionHeadingFontSize(width);

        double initial = binding.get();
        width.set(1800.0);
        double updated = binding.get();

        assertNotEquals(initial, updated,
                "panelSectionHeadingFontSize binding should react to width property changes");
    }

    // -------------------------------------------------------------------------
    // panelBodyFontSize (new): clamp(12, width * 0.01, 18)
    // -------------------------------------------------------------------------

    @Test
    void panelBodyFontSize_clampsCorrectly() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(500.0);
        DoubleBinding binding = ResponsiveMenuLayout.panelBodyFontSize(width);

        // 500 * 0.01 = 5, but min is 12 → expect 12
        assertEquals(12.0, binding.get(), 1e-9,
                "panelBodyFontSize should clamp to min 12 at 500px width");

        // 1500 * 0.01 = 15, within [12, 18]
        width.set(1500.0);
        assertEquals(15.0, binding.get(), 1e-9,
                "panelBodyFontSize should be 15 at 1500px width");

        // 2500 * 0.01 = 25, but max is 18 → expect 18
        width.set(2500.0);
        assertEquals(18.0, binding.get(), 1e-9,
                "panelBodyFontSize should clamp to max 18 at 2500px width");
    }

    @Test
    void panelBodyFontSize_reactsToWidthChanges() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(1000.0);
        DoubleBinding binding = ResponsiveMenuLayout.panelBodyFontSize(width);

        double initial = binding.get();
        width.set(1800.0);
        double updated = binding.get();

        assertNotEquals(initial, updated,
                "panelBodyFontSize binding should react to width property changes");
    }

    // -------------------------------------------------------------------------
    // panelButtonFontSize (new): clamp(12, width * 0.01, 18)
    // -------------------------------------------------------------------------

    @Test
    void panelButtonFontSize_clampsCorrectly() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(500.0);
        DoubleBinding binding = ResponsiveMenuLayout.panelButtonFontSize(width);

        // 500 * 0.01 = 5, but min is 12 → expect 12
        assertEquals(12.0, binding.get(), 1e-9,
                "panelButtonFontSize should clamp to min 12 at 500px width");

        // 1500 * 0.01 = 15, within [12, 18]
        width.set(1500.0);
        assertEquals(15.0, binding.get(), 1e-9,
                "panelButtonFontSize should be 15 at 1500px width");

        // 2500 * 0.01 = 25, but max is 18 → expect 18
        width.set(2500.0);
        assertEquals(18.0, binding.get(), 1e-9,
                "panelButtonFontSize should clamp to max 18 at 2500px width");
    }

    @Test
    void panelButtonFontSize_reactsToWidthChanges() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(1000.0);
        DoubleBinding binding = ResponsiveMenuLayout.panelButtonFontSize(width);

        double initial = binding.get();
        width.set(1800.0);
        double updated = binding.get();

        assertNotEquals(initial, updated,
                "panelButtonFontSize binding should react to width property changes");
    }

    // -------------------------------------------------------------------------
    // panelPadding (new): clamp(20, width * 0.022, 48)
    // -------------------------------------------------------------------------

    @Test
    void panelPadding_clampsCorrectly() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(500.0);
        DoubleBinding binding = ResponsiveMenuLayout.panelPadding(width);

        // 500 * 0.022 = 11, but min is 20 → expect 20
        assertEquals(20.0, binding.get(), 1e-9,
                "panelPadding should clamp to min 20 at 500px width");

        // 1500 * 0.022 = 33, within [20, 48]
        width.set(1500.0);
        assertEquals(33.0, binding.get(), 1e-6,
                "panelPadding should be 33 at 1500px width");

        // 2500 * 0.022 = 55, but max is 48 → expect 48
        width.set(2500.0);
        assertEquals(48.0, binding.get(), 1e-9,
                "panelPadding should clamp to max 48 at 2500px width");
    }

    @Test
    void panelPadding_reactsToWidthChanges() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(1000.0);
        DoubleBinding binding = ResponsiveMenuLayout.panelPadding(width);

        double initial = binding.get();
        width.set(1800.0);
        double updated = binding.get();

        assertNotEquals(initial, updated,
                "panelPadding binding should react to width property changes");
    }

    // -------------------------------------------------------------------------
    // panelSpacing (new): clamp(8, width * 0.009, 18)
    // -------------------------------------------------------------------------

    @Test
    void panelSpacing_clampsCorrectly() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(500.0);
        DoubleBinding binding = ResponsiveMenuLayout.panelSpacing(width);

        // 500 * 0.009 = 4.5, but min is 8 → expect 8
        assertEquals(8.0, binding.get(), 1e-9,
                "panelSpacing should clamp to min 8 at 500px width");

        // 1500 * 0.009 = 13.5, within [8, 18]
        width.set(1500.0);
        assertEquals(13.5, binding.get(), 1e-6,
                "panelSpacing should be 13.5 at 1500px width");

        // 2500 * 0.009 = 22.5, but max is 18 → expect 18
        width.set(2500.0);
        assertEquals(18.0, binding.get(), 1e-9,
                "panelSpacing should clamp to max 18 at 2500px width");
    }

    @Test
    void panelSpacing_reactsToWidthChanges() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(1000.0);
        DoubleBinding binding = ResponsiveMenuLayout.panelSpacing(width);

        double initial = binding.get();
        width.set(1800.0);
        double updated = binding.get();

        assertNotEquals(initial, updated,
                "panelSpacing binding should react to width property changes");
    }

    // -------------------------------------------------------------------------
    // panelFieldPadding (new): clamp(7, width * 0.007, 14)
    // -------------------------------------------------------------------------

    @Test
    void panelFieldPadding_clampsCorrectly() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(500.0);
        DoubleBinding binding = ResponsiveMenuLayout.panelFieldPadding(width);

        // 500 * 0.007 = 3.5, but min is 7 → expect 7
        assertEquals(7.0, binding.get(), 1e-9,
                "panelFieldPadding should clamp to min 7 at 500px width");

        // 1400 * 0.007 = 9.8, within [7, 14]
        width.set(1400.0);
        assertEquals(9.8, binding.get(), 1e-6,
                "panelFieldPadding should be 9.8 at 1400px width");

        // 2500 * 0.007 = 17.5, but max is 14 → expect 14
        width.set(2500.0);
        assertEquals(14.0, binding.get(), 1e-9,
                "panelFieldPadding should clamp to max 14 at 2500px width");
    }

    @Test
    void panelFieldPadding_reactsToWidthChanges() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(1000.0);
        DoubleBinding binding = ResponsiveMenuLayout.panelFieldPadding(width);

        double initial = binding.get();
        width.set(1800.0);
        double updated = binding.get();

        assertNotEquals(initial, updated,
                "panelFieldPadding binding should react to width property changes");
    }

    // -------------------------------------------------------------------------
    // compactTitleScale: clamp(0.3, navPanelWidth / (rootWidth * 0.5), 0.7)
    // -------------------------------------------------------------------------

    @Test
    void compactTitleScale_at1920px_returns0_5() {
        // navPanelWidth = 1920 * 0.25 = 480; scale = 480 / (1920 * 0.5) = 480/960 = 0.5
        SimpleDoubleProperty rootWidth    = new SimpleDoubleProperty(1920.0);
        SimpleDoubleProperty navPanelWidth = new SimpleDoubleProperty(480.0);
        DoubleBinding binding = ResponsiveMenuLayout.compactTitleScale(rootWidth, navPanelWidth);

        assertEquals(0.5, binding.get(), 1e-9,
                "compactTitleScale should be 0.5 at 1920px root / 480px nav");
    }

    @Test
    void compactTitleScale_clampsToMin_atVerySmallWidth() {
        // navPanel=10, rootWidth=1000 → 10/(1000*0.5) = 0.02 < 0.3 → clamped to 0.3
        SimpleDoubleProperty rootWidth    = new SimpleDoubleProperty(1000.0);
        SimpleDoubleProperty navPanelWidth = new SimpleDoubleProperty(10.0);
        DoubleBinding binding = ResponsiveMenuLayout.compactTitleScale(rootWidth, navPanelWidth);

        assertEquals(0.3, binding.get(), 1e-9,
                "compactTitleScale should clamp to 0.3 when formula < 0.3");
    }

    @Test
    void compactTitleScale_clampsToMax() {
        // navPanel=600, rootWidth=500 → 600/(500*0.5) = 2.4 > 0.7 → clamped to 0.7
        SimpleDoubleProperty rootWidth    = new SimpleDoubleProperty(500.0);
        SimpleDoubleProperty navPanelWidth = new SimpleDoubleProperty(600.0);
        DoubleBinding binding = ResponsiveMenuLayout.compactTitleScale(rootWidth, navPanelWidth);

        assertEquals(0.7, binding.get(), 1e-9,
                "compactTitleScale should clamp to 0.7 when formula > 0.7");
    }

    @Test
    void compactTitleScale_reactsToWidthChanges() {
        SimpleDoubleProperty rootWidth    = new SimpleDoubleProperty(1920.0);
        SimpleDoubleProperty navPanelWidth = new SimpleDoubleProperty(480.0);
        DoubleBinding binding = ResponsiveMenuLayout.compactTitleScale(rootWidth, navPanelWidth);

        double initial = binding.get(); // 0.5

        // Increase navPanelWidth to push scale up (but still within range)
        navPanelWidth.set(600.0);
        double updated = binding.get(); // 600/(1920*0.5) = 600/960 ≈ 0.625

        assertNotEquals(initial, updated,
                "compactTitleScale binding must react to property changes");
        assertEquals(0.625, updated, 1e-9,
                "compactTitleScale should be 0.625 when navPanelWidth=600 and rootWidth=1920");
    }

    @Test
    void compactTitleScale_handlesZeroWidth() {
        SimpleDoubleProperty rootWidth    = new SimpleDoubleProperty(0.0);
        SimpleDoubleProperty navPanelWidth = new SimpleDoubleProperty(480.0);
        DoubleBinding binding = ResponsiveMenuLayout.compactTitleScale(rootWidth, navPanelWidth);

        assertEquals(0.5, binding.get(), 1e-9,
                "compactTitleScale should return safe default 0.5 when rootWidth is 0");
    }

    // -------------------------------------------------------------------------
    // compactTitleTranslateX
    // -------------------------------------------------------------------------

    @Test
    void compactTitleTranslateX_reactsToInputChanges() {
        SimpleDoubleProperty rootWidth          = new SimpleDoubleProperty(1280.0);
        SimpleDoubleProperty navPanelWidth      = new SimpleDoubleProperty(320.0);
        SimpleDoubleProperty navRightMargin     = new SimpleDoubleProperty(44.8);
        SimpleDoubleProperty navTranslateTarget = new SimpleDoubleProperty(-912.0);
        SimpleDoubleProperty contentLeftPadding = new SimpleDoubleProperty(64.0);
        SimpleDoubleProperty contentWidth       = new SimpleDoubleProperty(800.0);
        SimpleDoubleProperty compactScale       = new SimpleDoubleProperty(0.5);
        DoubleBinding binding = ResponsiveMenuLayout.compactTitleTranslateX(
                rootWidth, navPanelWidth, navRightMargin, navTranslateTarget,
                contentLeftPadding, contentWidth, compactScale);

        double initial = binding.get();

        // Change an input and verify the binding recomputes
        rootWidth.set(1920.0);
        double updated = binding.get();

        assertNotEquals(initial, updated,
                "compactTitleTranslateX binding must react to input property changes");
    }

    @Test
    void compactTitleTranslateX_returnsZero_whenContentWidthIsZero() {
        SimpleDoubleProperty rootWidth          = new SimpleDoubleProperty(1280.0);
        SimpleDoubleProperty navPanelWidth      = new SimpleDoubleProperty(320.0);
        SimpleDoubleProperty navRightMargin     = new SimpleDoubleProperty(44.8);
        SimpleDoubleProperty navTranslateTarget = new SimpleDoubleProperty(-912.0);
        SimpleDoubleProperty contentLeftPadding = new SimpleDoubleProperty(64.0);
        SimpleDoubleProperty contentWidth       = new SimpleDoubleProperty(0.0);
        SimpleDoubleProperty compactScale       = new SimpleDoubleProperty(0.5);
        DoubleBinding binding = ResponsiveMenuLayout.compactTitleTranslateX(
                rootWidth, navPanelWidth, navRightMargin, navTranslateTarget,
                contentLeftPadding, contentWidth, compactScale);

        assertEquals(0.0, binding.get(), 1e-9,
                "compactTitleTranslateX should return 0 when contentWidth is 0 (before first layout pass)");
    }

    // -------------------------------------------------------------------------
    // compactTitleTranslateY: -(rootHeight * 0.45) + contentHeight * scale / 2
    // -------------------------------------------------------------------------

    @Test
    void compactTitleTranslateY_isNegative_atNormalSize() {
        // At 1080px height, scale 0.5, contentHeight 150px:
        // -(1080*0.45) + 150*0.5/2 = -486.0 + 37.5 = -448.5 (negative → moves up)
        SimpleDoubleProperty rootHeight   = new SimpleDoubleProperty(1080.0);
        SimpleDoubleProperty contentHeight = new SimpleDoubleProperty(150.0);
        SimpleDoubleProperty compactScale  = new SimpleDoubleProperty(0.5);
        DoubleBinding binding = ResponsiveMenuLayout.compactTitleTranslateY(
                rootHeight, contentHeight, compactScale);

        double ty = binding.get();
        assertTrue(ty < 0,
                "compactTitleTranslateY should be negative at normal sizes (moves title up); got " + ty);
    }

    @Test
    void compactTitleTranslateY_reactsToHeightChanges() {
        SimpleDoubleProperty rootHeight   = new SimpleDoubleProperty(1080.0);
        SimpleDoubleProperty contentHeight = new SimpleDoubleProperty(150.0);
        SimpleDoubleProperty compactScale  = new SimpleDoubleProperty(0.5);
        DoubleBinding binding = ResponsiveMenuLayout.compactTitleTranslateY(
                rootHeight, contentHeight, compactScale);

        double initial = binding.get();
        rootHeight.set(768.0);
        double updated = binding.get();

        assertNotEquals(initial, updated,
                "compactTitleTranslateY binding must react to rootHeight changes");
    }

    @Test
    void compactTitleTranslateY_returnsZero_whenHeightIsZero() {
        SimpleDoubleProperty rootHeight   = new SimpleDoubleProperty(0.0);
        SimpleDoubleProperty contentHeight = new SimpleDoubleProperty(150.0);
        SimpleDoubleProperty compactScale  = new SimpleDoubleProperty(0.5);
        DoubleBinding binding = ResponsiveMenuLayout.compactTitleTranslateY(
                rootHeight, contentHeight, compactScale);

        assertEquals(0.0, binding.get(), 1e-9,
                "compactTitleTranslateY should return 0 when rootHeight is 0");
    }

    // -------------------------------------------------------------------------
    // navPanelOpenTranslateX — left margin addition
    // -------------------------------------------------------------------------

    @Test
    void navPanelOpenTranslateX_includesLeftMargin() {
        // At 1280px: panelContainerWidth = 1280 - 320 - 16 - 16 = 928
        // Without margin: -(928 + 16) = -944
        // With margin: -944 + clamp(16, 1280*0.015, 40) = -944 + 19.2 = -924.8
        // So the new value should be LESS negative than -944
        SimpleDoubleProperty rootWidth          = new SimpleDoubleProperty(1280.0);
        SimpleDoubleProperty panelContainerWidth = new SimpleDoubleProperty(928.0);
        SimpleDoubleProperty navPanelWidth      = new SimpleDoubleProperty(320.0);
        DoubleBinding binding = ResponsiveMenuLayout.navPanelOpenTranslateX(
                rootWidth, panelContainerWidth, navPanelWidth);

        double txWithMargin = binding.get();
        double txWithoutMargin = -(928.0 + ResponsiveMenuLayout.NAV_PANEL_GAP);

        assertTrue(txWithMargin > txWithoutMargin,
                "navPanelOpenTranslateX with margin should be less negative than without margin; "
                + "withMargin=" + txWithMargin + " withoutMargin=" + txWithoutMargin);
    }

    @Test
    void navPanelOpenTranslateX_leftMarginScalesWithWidth() {
        // At 1920px: margin = clamp(16, 1920*0.015, 40) = clamp(16, 28.8, 40) = 28.8
        // At 1280px: margin = clamp(16, 1280*0.015, 40) = clamp(16, 19.2, 40) = 19.2
        // So the (negative) translateX at 1920px should be less negative relative to the
        // no-margin formula than at 1280px. In other words: difference at 1920 (28.8) > at 1280 (19.2).

        // 1280px case:
        SimpleDoubleProperty rootWidth          = new SimpleDoubleProperty(1280.0);
        SimpleDoubleProperty panelContainerWidth = new SimpleDoubleProperty(928.0);
        SimpleDoubleProperty navPanelWidth      = new SimpleDoubleProperty(320.0);
        DoubleBinding binding = ResponsiveMenuLayout.navPanelOpenTranslateX(
                rootWidth, panelContainerWidth, navPanelWidth);
        double noMargin1280 = -(928.0 + ResponsiveMenuLayout.NAV_PANEL_GAP);
        double marginContribution1280 = binding.get() - noMargin1280;

        // 1920px case:
        rootWidth.set(1920.0);
        panelContainerWidth.set(1408.0);
        navPanelWidth.set(480.0);
        double noMargin1920 = -(1408.0 + ResponsiveMenuLayout.NAV_PANEL_GAP);
        double marginContribution1920 = binding.get() - noMargin1920;

        assertTrue(marginContribution1920 > marginContribution1280,
                "Left margin contribution should be larger at wider viewport; "
                + "at1280=" + marginContribution1280 + " at1920=" + marginContribution1920);
    }

    @Test
    void navPanelOpenTranslateX_leftMarginClampsToMin() {
        // At 800px: margin = clamp(16, 800*0.015, 40) = clamp(16, 12, 40) = 16 (clamped to min)
        SimpleDoubleProperty rootWidth          = new SimpleDoubleProperty(800.0);
        SimpleDoubleProperty panelContainerWidth = new SimpleDoubleProperty(584.0); // 800-200-16-0 roughly
        SimpleDoubleProperty navPanelWidth      = new SimpleDoubleProperty(200.0);
        DoubleBinding binding = ResponsiveMenuLayout.navPanelOpenTranslateX(
                rootWidth, panelContainerWidth, navPanelWidth);

        double noMargin = -(584.0 + ResponsiveMenuLayout.NAV_PANEL_GAP);
        double marginContribution = binding.get() - noMargin;

        assertEquals(16.0, marginContribution, 1e-9,
                "Left margin should be exactly 16px (minimum) at 800px viewport where 800*0.015=12 < 16");
    }

    // -------------------------------------------------------------------------
    // allNewBindings_reactToWidthChanges (omnibus reactivity test)
    // -------------------------------------------------------------------------

    @Test
    void allNewBindings_reactToWidthChanges() {
        SimpleDoubleProperty width = new SimpleDoubleProperty(1000.0);

        DoubleBinding iconSize       = ResponsiveMenuLayout.navButtonIconSize(width);
        DoubleBinding navPadding     = ResponsiveMenuLayout.navPanelPadding(width);
        DoubleBinding navSpacing     = ResponsiveMenuLayout.navPanelSpacing(width);
        DoubleBinding dialogTitle    = ResponsiveMenuLayout.panelDialogTitleFontSize(width);
        DoubleBinding sectionHeading = ResponsiveMenuLayout.panelSectionHeadingFontSize(width);
        DoubleBinding bodyFont       = ResponsiveMenuLayout.panelBodyFontSize(width);
        DoubleBinding btnFont        = ResponsiveMenuLayout.panelButtonFontSize(width);
        DoubleBinding padding        = ResponsiveMenuLayout.panelPadding(width);
        DoubleBinding spacing        = ResponsiveMenuLayout.panelSpacing(width);
        DoubleBinding fieldPadding   = ResponsiveMenuLayout.panelFieldPadding(width);

        double iconSizeInitial       = iconSize.get();
        double navPaddingInitial     = navPadding.get();
        double navSpacingInitial     = navSpacing.get();
        double dialogTitleInitial    = dialogTitle.get();
        double sectionHeadingInitial = sectionHeading.get();
        double bodyFontInitial       = bodyFont.get();
        double btnFontInitial        = btnFont.get();
        double paddingInitial        = padding.get();
        double spacingInitial        = spacing.get();
        double fieldPaddingInitial   = fieldPadding.get();

        width.set(1800.0);

        assertNotEquals(iconSizeInitial,       iconSize.get(),       "navButtonIconSize must react to width change");
        assertNotEquals(navPaddingInitial,     navPadding.get(),     "navPanelPadding must react to width change");
        assertNotEquals(navSpacingInitial,     navSpacing.get(),     "navPanelSpacing must react to width change");
        assertNotEquals(dialogTitleInitial,    dialogTitle.get(),    "panelDialogTitleFontSize must react to width change");
        assertNotEquals(sectionHeadingInitial, sectionHeading.get(), "panelSectionHeadingFontSize must react to width change");
        assertNotEquals(bodyFontInitial,       bodyFont.get(),       "panelBodyFontSize must react to width change");
        assertNotEquals(btnFontInitial,        btnFont.get(),        "panelButtonFontSize must react to width change");
        assertNotEquals(paddingInitial,        padding.get(),        "panelPadding must react to width change");
        assertNotEquals(spacingInitial,        spacing.get(),        "panelSpacing must react to width change");
        assertNotEquals(fieldPaddingInitial,   fieldPadding.get(),   "panelFieldPadding must react to width change");
    }
}
