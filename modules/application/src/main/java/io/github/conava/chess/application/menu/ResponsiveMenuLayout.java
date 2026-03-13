package io.github.conava.chess.application.menu;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.value.ObservableDoubleValue;

/**
 * Stateless utility that produces JavaFX {@link DoubleBinding} instances for
 * all responsive sizing and layout metrics used by the cinematic main menu.
 *
 * <p>Every method is <b>static</b> and returns a reactive binding that
 * automatically recomputes whenever the supplied property changes. The class
 * carries no mutable state and has no dependency on any controller or scene.</p>
 *
 * <h2>Design</h2>
 * <p>JavaFX CSS cannot express {@code clamp()}-style viewport-relative units, so
 * all font-size and spacing values are computed here and applied as inline styles
 * via {@code label.styleProperty().bind(...)}. Removing fixed sizes from CSS
 * (Package 2) ensures inline styles always take precedence without conflict.</p>
 *
 * <h2>Clamp helper</h2>
 * <p>Every binding is essentially {@code clamp(min, value * factor, max)}
 * implemented as {@code Math.max(min, Math.min(max, value * factor))}.</p>
 */
public final class ResponsiveMenuLayout {

    /** Gap between the nav panel and the panel container, in pixels. */
    static final double NAV_PANEL_GAP = 16.0;

    private ResponsiveMenuLayout() {
        // utility class — no instances
    }

    // -------------------------------------------------------------------------
    // Font size bindings
    // -------------------------------------------------------------------------

    /**
     * Responsive title font size: {@code clamp(28, width * 0.05, 96)}.
     *
     * <p>At 1920px width, this produces 96px. The maximum was raised from 64
     * to 96 to allow proper scaling at 1920px+ displays.</p>
     *
     * @param width observable root-pane width
     * @return binding in the range [28, 96] pixels
     */
    public static DoubleBinding titleFontSize(ObservableDoubleValue width) {
        return Bindings.createDoubleBinding(
                () -> clamp(28.0, width.get() * 0.05, 96.0),
                width);
    }

    /**
     * Responsive tagline font size: {@code clamp(11, width * 0.014, 28)}.
     *
     * <p>At 1920px width, this produces 26.88px. The maximum was raised from 17
     * to 28 to allow proper scaling at 1920px+ displays.</p>
     *
     * @param width observable root-pane width
     * @return binding in the range [11, 28] pixels
     */
    public static DoubleBinding taglineFontSize(ObservableDoubleValue width) {
        return Bindings.createDoubleBinding(
                () -> clamp(11.0, width.get() * 0.014, 28.0),
                width);
    }

    /**
     * Responsive nav-button font size: {@code clamp(12, width * 0.012, 22)}.
     *
     * <p>At 1920px width, this produces 23.04px, clamped to 22. The maximum
     * was raised from 15 to 22 to allow proper scaling at 1920px+ displays.</p>
     *
     * @param width observable root-pane width
     * @return binding in the range [12, 22] pixels
     */
    public static DoubleBinding navButtonFontSize(ObservableDoubleValue width) {
        return Bindings.createDoubleBinding(
                () -> clamp(12.0, width.get() * 0.012, 22.0),
                width);
    }

    // -------------------------------------------------------------------------
    // Layout size bindings
    // -------------------------------------------------------------------------

    /**
     * Reactive width for the nav panel: always {@code rootWidth * 0.25}.
     *
     * <p>The nav panel occupies a fixed 25% of the root pane width at all window
     * sizes, keeping a consistent 25/75 split between the nav panel and the
     * remaining space available to the panel container.</p>
     *
     * @param rootWidth observable root-pane width
     * @return binding whose value is exactly 25% of {@code rootWidth}
     */
    public static DoubleBinding navPanelWidth(ObservableDoubleValue rootWidth) {
        return Bindings.createDoubleBinding(
                () -> rootWidth.get() * 0.25,
                rootWidth);
    }

    /**
     * Responsive nav-button minimum height: {@code clamp(40, height * 0.065, 72)}.
     *
     * <p>At 1080px height, this produces 70.2px. The maximum was raised from 52
     * to 72 to allow proper scaling at 1080p+ displays.</p>
     *
     * @param height observable root-pane height
     * @return binding in the range [40, 72] pixels
     */
    public static DoubleBinding navButtonMinHeight(ObservableDoubleValue height) {
        return Bindings.createDoubleBinding(
                () -> clamp(40.0, height.get() * 0.065, 72.0),
                height);
    }

    /**
     * Responsive left padding for the content layer: {@code clamp(32, width * 0.05, 120)}.
     *
     * <p>At 1920px width, this produces 96px. The maximum was raised from 72
     * to 120 to allow proper scaling at 1920px+ displays.</p>
     *
     * @param width observable root-pane width
     * @return binding in the range [32, 120] pixels
     */
    public static DoubleBinding contentLayerLeftPadding(ObservableDoubleValue width) {
        return Bindings.createDoubleBinding(
                () -> clamp(32.0, width.get() * 0.05, 120.0),
                width);
    }

    /**
     * Responsive right margin for the nav panel: {@code clamp(16, width * 0.035, 72)}.
     *
     * <p>At 1920px width, this produces 67.2px. The maximum was raised from 48
     * to 72 to allow proper scaling at 1920px+ displays.</p>
     *
     * @param width observable root-pane width
     * @return binding in the range [16, 72] pixels
     */
    public static DoubleBinding navPanelRightMargin(ObservableDoubleValue width) {
        return Bindings.createDoubleBinding(
                () -> clamp(16.0, width.get() * 0.035, 72.0),
                width);
    }

    /**
     * Responsive width for the sliding panel container (offline/online/settings).
     *
     * <p>Under the 25/75 split model, the nav panel always occupies 25% of root
     * width. The panel container fills the remaining space minus a gap to the
     * nav panel and a small edge gap:</p>
     * <pre>
     *   navWidth  = rootWidth * 0.25
     *   edgeGap   = 16px
     *   raw       = rootWidth - navWidth - NAV_PANEL_GAP - edgeGap
     *   result    = max(raw, 0)   // never negative
     * </pre>
     *
     * @param rootWidth observable root-pane width
     * @return binding that fills remaining space after the 25% nav panel and gaps;
     *         always non-negative
     */
    public static DoubleBinding panelContainerWidth(ObservableDoubleValue rootWidth) {
        return Bindings.createDoubleBinding(() -> {
            double rw = rootWidth.get();
            double navWidth = rw * 0.25;
            double edgeGap = 16.0;
            double raw = rw - navWidth - NAV_PANEL_GAP - edgeGap;
            return Math.max(raw, 0.0);
        }, rootWidth);
    }

    /**
     * Computes the {@code translateX} offset to apply to the nav panel when a
     * sub-panel is open, positioning it to the left of the panel container.
     *
     * <p>Under the 25/75 split model, the nav panel's layout position is anchored
     * at CENTER_RIGHT of the root StackPane. When open, it is translated left so
     * that it sits to the left of the panel container. The panel container is
     * placed at CENTER_RIGHT, so the nav panel must move left by the
     * panel-container width plus a small gap. A responsive left margin is then
     * added to leave breathing room between the nav panel's left edge and the
     * window edge:</p>
     * <pre>
     *   leftMargin = clamp(16, rootWidth * 0.015, 40)
     *   translateX = -(panelContainerWidth + NAV_PANEL_GAP) + leftMargin
     * </pre>
     * <p>At 1920px: margin = 28.8px. At 1280px: margin = 19.2px. At 800px:
     * margin is clamped to the minimum 16px.</p>
     *
     * @param rootWidth            observable root-pane width (used for responsive left margin)
     * @param panelContainerWidth  observable panel-container width (from {@link #panelContainerWidth})
     * @param navPanelWidth        observable nav-panel width (from {@link #navPanelWidth}, i.e. 25% of root;
     *                             kept for API compatibility and as a reactive dependency signal)
     * @return binding whose value is a negative offset (slide left) with a left margin applied
     */
    public static DoubleBinding navPanelOpenTranslateX(ObservableDoubleValue rootWidth,
                                                        ObservableDoubleValue panelContainerWidth,
                                                        ObservableDoubleValue navPanelWidth) {
        return Bindings.createDoubleBinding(
                () -> {
                    double leftMargin = clamp(16.0, rootWidth.get() * 0.015, 40.0);
                    return -(panelContainerWidth.get() + NAV_PANEL_GAP) + leftMargin;
                },
                rootWidth, panelContainerWidth, navPanelWidth);
    }

    // -------------------------------------------------------------------------
    // Compact title position bindings (translate+scale model)
    // -------------------------------------------------------------------------

    /**
     * Scale factor for the content layer when in compact (open) state.
     * Fits the title proportionally within the nav panel width.
     *
     * <p>Formula: {@code clamp(0.3, navPanelWidth / (rootWidth * 0.5), 0.7)}</p>
     *
     * <p>The 0.5 factor accounts for the title text typically using about half
     * the root pane width visually. At 1920px (navPanel=480), this gives
     * 480/960 = 0.5. At 1280px (navPanel=320), 320/640 = 0.5.</p>
     *
     * @param rootWidth     observable root-pane width
     * @param navPanelWidth observable nav-panel width (25% of root)
     * @return binding in the range [0.3, 0.7]
     */
    public static DoubleBinding compactTitleScale(ObservableDoubleValue rootWidth,
                                                   ObservableDoubleValue navPanelWidth) {
        return Bindings.createDoubleBinding(
                () -> {
                    double rw = rootWidth.get();
                    if (rw <= 0.0) {
                        return 0.5; // safe default before first layout pass
                    }
                    return clamp(0.3, navPanelWidth.get() / (rw * 0.5), 0.7);
                },
                rootWidth, navPanelWidth);
    }

    /**
     * Horizontal translation for the content layer in compact (open) state.
     * Positions the scaled content layer so its visual left edge aligns with the
     * nav panel's left edge when the nav panel is in its open (translated) position.
     *
     * <p>Since JavaFX scales around the node's center, the visual left edge shifts
     * right when scaleX &lt; 1. This method compensates for that shift.</p>
     *
     * <p>Calculation:</p>
     * <pre>
     *   navLeftEdge              = rootWidth - navRightMargin - navPanelWidth + navTranslateTarget
     *   targetLeft               = navLeftEdge + 16   // 16px inset for button-text alignment
     *   scalePivotCompensation   = contentWidth * (1 - scale) / 2
     *   translateX               = targetLeft - scalePivotCompensation - contentLeftPadding * scale
     * </pre>
     *
     * <p>The additional 16&nbsp;px inset ensures the title text aligns with the button
     * text inside the nav panel rather than with the bare panel edge.</p>
     *
     * <p>The two compensation terms derive from the JavaFX center-pivot scale model.
     * When {@code scaleX=S} is applied from the center pivot at {@code x=W/2}, the
     * visual left edge shifts right by {@code W*(1-S)/2} (= scalePivotCompensation),
     * and the left padding is visually reduced to {@code leftPadding*S}. Subtracting
     * both terms cancels the shift so the title text lands exactly at
     * {@code targetLeft}.</p>
     *
     * @param rootWidth           observable root-pane width
     * @param navPanelWidth       observable nav-panel width
     * @param navRightMargin      observable nav-panel right margin
     * @param navTranslateTarget  observable nav translateX target (negative when open)
     * @param contentLeftPadding  observable content layer left padding
     * @param contentWidth        observable content layer layout width
     * @param compactScale        observable scale factor for compact state
     * @return binding whose value positions the content layer horizontally;
     *         returns 0 when {@code contentWidth &le; 0}
     */
    public static DoubleBinding compactTitleTranslateX(ObservableDoubleValue rootWidth,
                                                        ObservableDoubleValue navPanelWidth,
                                                        ObservableDoubleValue navRightMargin,
                                                        ObservableDoubleValue navTranslateTarget,
                                                        ObservableDoubleValue contentLeftPadding,
                                                        ObservableDoubleValue contentWidth,
                                                        ObservableDoubleValue compactScale) {
        return Bindings.createDoubleBinding(
                () -> {
                    double cw = contentWidth.get();
                    if (cw <= 0.0) {
                        return 0.0; // before first layout pass
                    }
                    double scale = compactScale.get();
                    // Nav panel's visual left edge in the root pane coordinate space
                    double navLeftEdge = rootWidth.get()
                            - navRightMargin.get()
                            - navPanelWidth.get()
                            + navTranslateTarget.get();
                    // Add ~16px inset so title text aligns with button text
                    double targetLeft = navLeftEdge + 16.0;
                    // Scale-pivot compensation: center-pivot scale shifts visual left rightward
                    double scalePivotCompensation = cw * (1.0 - scale) / 2.0;
                    return targetLeft - scalePivotCompensation - contentLeftPadding.get() * scale;
                },
                rootWidth, navPanelWidth, navRightMargin, navTranslateTarget,
                contentLeftPadding, contentWidth, compactScale);
    }

    /**
     * Vertical translation for the content layer in compact (open) state.
     * Positions the content layer above the nav buttons, near the top of the
     * screen. The content layer starts vertically centered (CENTER_LEFT alignment).
     *
     * <p>Since JavaFX scales around the node's center, the visual top edge shifts
     * down when scaleY &lt; 1. This method compensates for that shift.</p>
     *
     * <p>Formula:</p>
     * <pre>
     *   translateY = -(rootHeight * 0.45) + contentHeight * scale / 2
     * </pre>
     *
     * <p>At 1080px height with scale 0.5 and contentHeight 150px:
     * -(1080 * 0.45) + 150 * 0.25 = -486.0 + 37.5 = -448.5. The title moves up
     * 449px from center, landing at roughly y=91px from the top (5% of height),
     * well above the nav buttons.</p>
     *
     * @param rootHeight     observable root-pane height
     * @param contentHeight  observable content layer layout height
     * @param compactScale   observable scale factor for compact state
     * @return binding whose value is a negative offset (moves title up from center);
     *         returns 0 when {@code rootHeight &le; 0} or {@code contentHeight &le; 0}
     */
    public static DoubleBinding compactTitleTranslateY(ObservableDoubleValue rootHeight,
                                                        ObservableDoubleValue contentHeight,
                                                        ObservableDoubleValue compactScale) {
        return Bindings.createDoubleBinding(
                () -> {
                    double rh = rootHeight.get();
                    double ch = contentHeight.get();
                    if (rh <= 0.0 || ch <= 0.0) {
                        return 0.0; // safe default before first layout pass
                    }
                    double scale = compactScale.get();
                    // Move up from center position; compensate for scale-pivot shift
                    return -(rh * 0.45) + ch * scale / 2.0;
                },
                rootHeight, contentHeight, compactScale);
    }

    /**
     * Responsive accent-line max-width: {@code clamp(40, titleFontSize * 1.1, 110)}.
     *
     * <p>This keeps the decorative accent line visually proportional to the title
     * text at all window sizes. The maximum was raised from 72 to 110 to track
     * with the larger title font sizes now possible at 1920px+.</p>
     *
     * @param titleFontSize observable title font size (from {@link #titleFontSize})
     * @return binding in the range [40, 110] pixels
     */
    public static DoubleBinding accentLineWidth(ObservableDoubleValue titleFontSize) {
        return Bindings.createDoubleBinding(
                () -> clamp(40.0, titleFontSize.get() * 1.1, 110.0),
                titleFontSize);
    }

    // -------------------------------------------------------------------------
    // Sub-panel content sizing bindings
    // -------------------------------------------------------------------------

    /**
     * Responsive icon font size for nav button graphics: {@code clamp(14, width * 0.014, 24)}.
     *
     * <p>Applied to the chess-piece {@code Label} used as a graphic on each nav
     * button. At 1280px width, this gives 17.92px; at 1920px, 26.88px clamped
     * to 24.</p>
     *
     * @param width observable root-pane (or stage) width
     * @return binding in the range [14, 24] pixels
     * @since 2.0
     */
    public static DoubleBinding navButtonIconSize(ObservableDoubleValue width) {
        return Bindings.createDoubleBinding(
                () -> clamp(14.0, width.get() * 0.014, 24.0),
                width);
    }

    /**
     * Responsive padding for the nav panel VBox: {@code clamp(16, width * 0.018, 36)}.
     *
     * <p>Replaces the hardcoded {@code padding="24 16"} in FXML. Callers typically
     * use this value as the horizontal side padding and multiply by 1.5 for
     * top/bottom padding to match the original 24/16 ratio.</p>
     *
     * @param width observable root-pane (or stage) width
     * @return binding in the range [16, 36] pixels
     * @since 2.0
     */
    public static DoubleBinding navPanelPadding(ObservableDoubleValue width) {
        return Bindings.createDoubleBinding(
                () -> clamp(16.0, width.get() * 0.018, 36.0),
                width);
    }

    /**
     * Responsive spacing for the nav panel VBox: {@code clamp(6, width * 0.007, 14)}.
     *
     * <p>Replaces the hardcoded {@code spacing="8"} in FXML. Applied directly to
     * {@code navPanel.spacingProperty()}.</p>
     *
     * @param width observable root-pane (or stage) width
     * @return binding in the range [6, 14] pixels
     * @since 2.0
     */
    public static DoubleBinding navPanelSpacing(ObservableDoubleValue width) {
        return Bindings.createDoubleBinding(
                () -> clamp(6.0, width.get() * 0.007, 14.0),
                width);
    }

    /**
     * Responsive dialog title font size for sub-panels: {@code clamp(18, width * 0.016, 32)}.
     *
     * <p>Applied to {@code .dialog-title} labels inside cinematic form panels
     * (e.g., "Offline Game", "Online Game", "Settings"). At 1280px, this gives
     * 20.48px; at 1920px, 30.72px.</p>
     *
     * @param width observable stage width
     * @return binding in the range [18, 32] pixels
     * @since 2.0
     */
    public static DoubleBinding panelDialogTitleFontSize(ObservableDoubleValue width) {
        return Bindings.createDoubleBinding(
                () -> clamp(18.0, width.get() * 0.016, 32.0),
                width);
    }

    /**
     * Responsive section heading font size for sub-panels: {@code clamp(11, width * 0.009, 18)}.
     *
     * <p>Applied to {@code .section-heading} labels inside cinematic form panels.
     * At 1280px, this gives 11.52px; at 1920px, 17.28px.</p>
     *
     * @param width observable stage width
     * @return binding in the range [11, 18] pixels
     * @since 2.0
     */
    public static DoubleBinding panelSectionHeadingFontSize(ObservableDoubleValue width) {
        return Bindings.createDoubleBinding(
                () -> clamp(11.0, width.get() * 0.009, 18.0),
                width);
    }

    /**
     * Responsive body font size for form labels and field text inside sub-panels:
     * {@code clamp(12, width * 0.01, 18)}.
     *
     * <p>Applied to general-purpose text labels and form field text inside
     * cinematic form panels. At 1280px, this gives 12.8px; at 1920px, 18px.</p>
     *
     * @param width observable stage width
     * @return binding in the range [12, 18] pixels
     * @since 2.0
     */
    public static DoubleBinding panelBodyFontSize(ObservableDoubleValue width) {
        return Bindings.createDoubleBinding(
                () -> clamp(12.0, width.get() * 0.01, 18.0),
                width);
    }

    /**
     * Responsive button font size for {@code .btn-primary} and {@code .btn-ghost}
     * inside sub-panels: {@code clamp(12, width * 0.01, 18)}.
     *
     * <p>Uses the same scale factor as {@link #panelBodyFontSize} to maintain
     * consistent typographic hierarchy between body text and button labels.</p>
     *
     * @param width observable stage width
     * @return binding in the range [12, 18] pixels
     * @since 2.0
     */
    public static DoubleBinding panelButtonFontSize(ObservableDoubleValue width) {
        return Bindings.createDoubleBinding(
                () -> clamp(12.0, width.get() * 0.01, 18.0),
                width);
    }

    /**
     * Responsive padding for {@code .cinematic-form-panel} VBoxes: {@code clamp(20, width * 0.022, 48)}.
     *
     * <p>Replaces the hardcoded {@code -fx-padding: 32 28} in CSS. Callers
     * typically use this value for vertical padding and multiply by 0.875 for
     * horizontal padding to match the original 32/28 ratio.</p>
     *
     * @param width observable stage width
     * @return binding in the range [20, 48] pixels
     * @since 2.0
     */
    public static DoubleBinding panelPadding(ObservableDoubleValue width) {
        return Bindings.createDoubleBinding(
                () -> clamp(20.0, width.get() * 0.022, 48.0),
                width);
    }

    /**
     * Responsive spacing for VBoxes inside sub-panels: {@code clamp(8, width * 0.009, 18)}.
     *
     * <p>Replaces the hardcoded {@code spacing="12"} in sub-panel FXML files.
     * Applied directly to the form panel VBox's {@code spacingProperty()}.</p>
     *
     * @param width observable stage width
     * @return binding in the range [8, 18] pixels
     * @since 2.0
     */
    public static DoubleBinding panelSpacing(ObservableDoubleValue width) {
        return Bindings.createDoubleBinding(
                () -> clamp(8.0, width.get() * 0.009, 18.0),
                width);
    }

    /**
     * Responsive internal padding for {@code TextField} nodes inside sub-panels:
     * {@code clamp(7, width * 0.007, 14)}.
     *
     * <p>Replaces the hardcoded {@code 9px} padding in CSS. At 1280px, this
     * gives 8.96px; at 1920px, 13.44px.</p>
     *
     * @param width observable stage width
     * @return binding in the range [7, 14] pixels
     * @since 2.0
     */
    public static DoubleBinding panelFieldPadding(ObservableDoubleValue width) {
        return Bindings.createDoubleBinding(
                () -> clamp(7.0, width.get() * 0.007, 14.0),
                width);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Clamps {@code value} to the inclusive range {@code [min, max]}.
     *
     * @param min   lower bound
     * @param value value to clamp
     * @param max   upper bound
     * @return {@code Math.max(min, Math.min(max, value))}
     */
    private static double clamp(double min, double value, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
