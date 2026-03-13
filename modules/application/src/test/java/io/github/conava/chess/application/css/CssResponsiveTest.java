package io.github.conava.chess.application.css;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that cinematic main-menu CSS selectors in {@code base.css} do not
 * define hardcoded pixel font sizes or fixed width constraints. These values
 * must be supplied at runtime via JavaFX style-property bindings so that the
 * layout is fully responsive.
 *
 * <p>Also acts as a regression guard to confirm that non-cinematic selectors
 * (buttons, cards, dialogs, etc.) retain their existing {@code -fx-font-size}
 * declarations, and that the theme files ({@code dark.css}, {@code light.css})
 * do not introduce font sizes into the cinematic selectors.
 *
 * <p>Tests use plain text parsing — no JavaFX toolkit is required.
 */
class CssResponsiveTest {

    private static String baseCss;
    private static String darkCss;
    private static String lightCss;

    @BeforeAll
    static void loadCssFiles() throws IOException {
        baseCss = loadResource("/css/base.css");
        darkCss = loadResource("/css/dark.css");
        lightCss = loadResource("/css/light.css");
    }

    // ── cinematic-title ───────────────────────────────────────────────────────

    /**
     * Verifies that the {@code .cinematic-title} block in {@code base.css} does
     * not contain a {@code -fx-font-size} declaration. The font size is supplied
     * at runtime by the responsive Java binding.
     */
    @Test
    void baseCss_cinematicTitle_noHardcodedFontSize() {
        String block = extractSelectorBlock(baseCss, ".cinematic-title");
        assertNotNull(block, ".cinematic-title selector block must exist in base.css");
        assertFalse(block.contains("-fx-font-size"),
                ".cinematic-title must not declare -fx-font-size (it is set by Java binding)");
    }

    // ── cinematic-tagline ─────────────────────────────────────────────────────

    /**
     * Verifies that the {@code .cinematic-tagline} block in {@code base.css} does
     * not contain a {@code -fx-font-size} declaration. The font size is supplied
     * at runtime by the responsive Java binding.
     */
    @Test
    void baseCss_cinematicTagline_noHardcodedFontSize() {
        String block = extractSelectorBlock(baseCss, ".cinematic-tagline");
        assertNotNull(block, ".cinematic-tagline selector block must exist in base.css");
        assertFalse(block.contains("-fx-font-size"),
                ".cinematic-tagline must not declare -fx-font-size (it is set by Java binding)");
    }

    // ── cinematic-nav-button ──────────────────────────────────────────────────

    /**
     * Verifies that the {@code .cinematic-nav-button} block in {@code base.css}
     * does not contain a {@code -fx-font-size} declaration. The font size is
     * supplied at runtime by the responsive Java binding.
     *
     * <p>Note: the {@code .cinematic-nav-button:hover} and
     * {@code .cinematic-nav-button:pressed} pseudo-class blocks (which live in
     * theme files) are not targeted by this test.
     */
    @Test
    void baseCss_cinematicNavButton_noHardcodedFontSize() {
        String block = extractSelectorBlock(baseCss, ".cinematic-nav-button");
        assertNotNull(block, ".cinematic-nav-button selector block must exist in base.css");
        assertFalse(block.contains("-fx-font-size"),
                ".cinematic-nav-button must not declare -fx-font-size (it is set by Java binding)");
    }

    // ── cinematic-nav-panel ───────────────────────────────────────────────────

    /**
     * Verifies that the {@code .cinematic-nav-panel} block in {@code base.css}
     * does not define {@code -fx-min-width} or {@code -fx-max-width}. Width
     * constraints are managed by Java bindings to allow the nav panel to adapt
     * to the current window size.
     */
    @Test
    void baseCss_cinematicNavPanel_noFixedWidthConstraints() {
        String block = extractSelectorBlock(baseCss, ".cinematic-nav-panel");
        assertNotNull(block, ".cinematic-nav-panel selector block must exist in base.css");
        assertFalse(block.contains("-fx-min-width"),
                ".cinematic-nav-panel must not declare -fx-min-width (managed by Java binding)");
        assertFalse(block.contains("-fx-max-width"),
                ".cinematic-nav-panel must not declare -fx-max-width (managed by Java binding)");
    }

    // ── cinematic-form-panel .section-heading ─────────────────────────────────

    /**
     * Verifies that the {@code .cinematic-form-panel .section-heading} compound
     * selector in {@code base.css} does not declare {@code -fx-font-size}. Font
     * size for this element is supplied at runtime by a JavaFX style-property
     * binding, so a hardcoded value would create a conflicting declaration.
     *
     * <p>Note: the base {@code .section-heading} selector retains its font-size
     * for non-cinematic contexts; only the compound cinematic-scoped rule is
     * cleaned up here.
     */
    @Test
    void baseCss_cinematicFormPanelSectionHeading_noHardcodedFontSize() {
        String block = extractCompoundSelectorBlock(baseCss, ".cinematic-form-panel .section-heading");
        assertNotNull(block,
                ".cinematic-form-panel .section-heading selector block must exist in base.css");
        assertFalse(block.contains("-fx-font-size"),
                ".cinematic-form-panel .section-heading must not declare -fx-font-size "
                        + "(it is set by Java binding)");
    }

    // ── cinematic-form-panel padding/spacing ──────────────────────────────────

    /**
     * Verifies that the {@code .cinematic-form-panel} selector in {@code base.css}
     * does not declare {@code -fx-padding} or {@code -fx-spacing}. These layout
     * values are supplied at runtime by JavaFX style-property bindings so that the
     * panel can scale responsively with the window width.
     */
    @Test
    void baseCss_cinematicFormPanel_noHardcodedPaddingOrSpacing() {
        String block = extractSelectorBlock(baseCss, ".cinematic-form-panel");
        assertNotNull(block, ".cinematic-form-panel selector block must exist in base.css");
        assertFalse(block.contains("-fx-padding"),
                ".cinematic-form-panel must not declare -fx-padding (managed by Java binding)");
        assertFalse(block.contains("-fx-spacing"),
                ".cinematic-form-panel must not declare -fx-spacing (managed by Java binding)");
    }

    // ── cinematic-nav-panel padding/spacing ───────────────────────────────────

    /**
     * Verifies that the {@code .cinematic-nav-panel} selector in {@code base.css}
     * does not declare {@code -fx-padding} or {@code -fx-spacing}. These layout
     * values are supplied at runtime by JavaFX style-property bindings.
     */
    @Test
    void baseCss_cinematicNavPanel_noHardcodedPaddingOrSpacing() {
        String block = extractSelectorBlock(baseCss, ".cinematic-nav-panel");
        assertNotNull(block, ".cinematic-nav-panel selector block must exist in base.css");
        assertFalse(block.contains("-fx-padding"),
                ".cinematic-nav-panel must not declare -fx-padding (managed by Java binding)");
        assertFalse(block.contains("-fx-spacing"),
                ".cinematic-nav-panel must not declare -fx-spacing (managed by Java binding)");
    }

    // ── cinematic-nav-centered ────────────────────────────────────────────────

    /**
     * Verifies that {@code .cinematic-nav-centered} has been removed from
     * {@code base.css}. This rule was used by the v1 reparenting model to center
     * nav buttons when the panel was open. It is no longer needed in v2 (translate
     * +scale model) and must not be reintroduced.
     */
    @Test
    void baseCss_cinematicNavCentered_doesNotExist() {
        assertFalse(baseCss.contains(".cinematic-nav-centered"),
                ".cinematic-nav-centered must not exist in base.css (removed in v2)");
    }

    /**
     * Verifies that {@code .cinematic-content-compact} has been removed from
     * {@code base.css}. This rule was used by the v1 reparenting model to style
     * the content layer when it was reparented into the nav panel. It is no longer
     * needed in v2 (translate+scale model) and must not be reintroduced.
     */
    @Test
    void baseCss_cinematicContentCompact_doesNotExist() {
        assertFalse(baseCss.contains(".cinematic-content-compact"),
                ".cinematic-content-compact must not exist in base.css (removed in v2)");
    }

    // ── .dialog-title regression guard ────────────────────────────────────────

    /**
     * Verifies that {@code .dialog-title} in {@code base.css} retains its
     * {@code -fx-font-size} declaration. This selector is used in non-cinematic
     * contexts (game-end overlay, waiting screen) where no Java binding is active,
     * so the CSS value serves as the only font-size source for those screens.
     */
    @Test
    void baseCss_dialogTitle_stillHasFontSize() {
        String block = extractSelectorBlock(baseCss, ".dialog-title");
        assertNotNull(block, ".dialog-title block must exist in base.css");
        assertTrue(block.contains("-fx-font-size"),
                ".dialog-title must retain -fx-font-size for non-cinematic screen contexts");
    }

    // ── non-cinematic regression guard ────────────────────────────────────────

    /**
     * Verifies that non-cinematic selectors that define font sizes in
     * {@code base.css} have not been accidentally modified. These selectors use
     * fixed pixel font sizes and must remain unchanged.
     */
    @Test
    void baseCss_nonCinematicSelectors_unchanged() {
        // .btn-primary still has a font size
        String btnPrimaryBlock = extractSelectorBlock(baseCss, ".btn-primary");
        assertNotNull(btnPrimaryBlock, ".btn-primary block must exist");
        assertTrue(btnPrimaryBlock.contains("-fx-font-size"),
                ".btn-primary must still declare -fx-font-size");

        // .dialog-title still has a font size
        String dialogTitleBlock = extractSelectorBlock(baseCss, ".dialog-title");
        assertNotNull(dialogTitleBlock, ".dialog-title block must exist");
        assertTrue(dialogTitleBlock.contains("-fx-font-size"),
                ".dialog-title must still declare -fx-font-size");

        // .card must still be present (structural class, no font size needed)
        assertTrue(baseCss.contains(".card"), ".card selector must still be present in base.css");
    }

    // ── theme CSS guard ───────────────────────────────────────────────────────

    /**
     * Verifies that {@code dark.css} and {@code light.css} do not introduce
     * {@code -fx-font-size} declarations into any of the cinematic selector
     * blocks. Theme files are only permitted to define colors and effects for
     * cinematic elements.
     */
    @Test
    void themeCss_noFontSizeInCinematicSelectors() {
        for (String[] entry : new String[][]{
                {"dark.css", darkCss},
                {"light.css", lightCss}
        }) {
            String fileName = entry[0];
            String css = entry[1];

            for (String selector : new String[]{
                    ".cinematic-title",
                    ".cinematic-tagline",
                    ".cinematic-nav-button",
                    ".cinematic-nav-panel"
            }) {
                String block = extractSelectorBlock(css, selector);
                if (block != null) {
                    assertFalse(block.contains("-fx-font-size"),
                            fileName + " must not add -fx-font-size to " + selector);
                }
            }
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /**
     * Extracts the first CSS declaration block that begins with exactly the given
     * selector token (not a pseudo-class or child selector variant). Returns the
     * content between the opening {@code '{'} and the matching closing {@code '}'}.
     *
     * <p>The match is performed on the plain selector string, anchored so that
     * {@code .cinematic-nav-button} does not accidentally match
     * {@code .cinematic-nav-button:hover}.
     *
     * @param css      the full CSS text to search
     * @param selector the exact selector to find, e.g. {@code ".cinematic-title"}
     * @return the content of the declaration block (between braces), or
     *         {@code null} if the selector is not found
     */
    static String extractSelectorBlock(String css, String selector) {
        // Match the selector followed immediately by optional whitespace then '{'
        // The selector must not be followed by ':', '.', '#', '[', or '>' (pseudo/child selectors)
        String escapedSelector = Pattern.quote(selector);
        Pattern pattern = Pattern.compile(
                escapedSelector + "\\s*\\{([^}]*)}",
                Pattern.DOTALL
        );
        Matcher matcher = pattern.matcher(css);

        while (matcher.find()) {
            // Verify this is not a pseudo-class or compound selector by checking
            // the character immediately before the opening brace in the full match
            String fullMatch = matcher.group(0);
            // The full match starts with our selector; check what follows it
            String afterSelector = fullMatch.substring(selector.length()).stripLeading();
            if (afterSelector.startsWith("{")) {
                return matcher.group(1);
            }
        }
        return null;
    }

    /**
     * Extracts the CSS declaration block for a compound selector such as
     * {@code ".cinematic-form-panel .section-heading"}. Unlike
     * {@link #extractSelectorBlock(String, String)}, this method does not apply
     * an anchor check after the selector text — it simply looks for the selector
     * followed by optional whitespace and an opening brace, which is sufficient
     * for compound (descendant) selectors.
     *
     * @param css      the full CSS text to search
     * @param selector the compound selector to find, e.g.
     *                 {@code ".cinematic-form-panel .section-heading"}
     * @return the content of the declaration block (between braces), or
     *         {@code null} if the selector is not found
     */
    static String extractCompoundSelectorBlock(String css, String selector) {
        String escapedSelector = Pattern.quote(selector);
        Pattern pattern = Pattern.compile(
                escapedSelector + "\\s*\\{([^}]*)}",
                Pattern.DOTALL
        );
        Matcher matcher = pattern.matcher(css);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Loads a classpath resource as a UTF-8 string.
     *
     * @param path the resource path, relative to the classpath root
     *             (e.g. {@code "/css/base.css"})
     * @return the full contents of the resource as a string
     * @throws IOException if the resource cannot be found or read
     */
    private static String loadResource(String path) throws IOException {
        try (InputStream is = CssResponsiveTest.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new IOException("Resource not found on classpath: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
