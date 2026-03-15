package io.github.conava.chess.application.css;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that the scoresheet move-list CSS classes are present and correctly
 * configured in {@code base.css}, the two base theme files ({@code dark.css},
 * {@code light.css}), and all six theme-variant CSS files.
 *
 * <p>The scoresheet is a two-column move list (move-number, white move, black
 * move) rendered via a custom {@code ListCell}. Each column has a dedicated CSS
 * class for font, alignment, and color:
 * <ul>
 *   <li>{@code .scoresheet-row} — the HBox container per row</li>
 *   <li>{@code .scoresheet-move-number} — the move number label (right-aligned, monospace)</li>
 *   <li>{@code .scoresheet-white-move} — the white player's move label</li>
 *   <li>{@code .scoresheet-black-move} — the black player's move label</li>
 *   <li>{@code .scoresheet-header} — the header row HBox</li>
 *   <li>{@code .scoresheet-header-label} — labels in the header row</li>
 * </ul>
 *
 * <p>Tests use plain text parsing — no JavaFX toolkit is required.
 */
class CssScoresheetTest {

    private static String baseCss;
    private static String darkCss;
    private static String lightCss;
    /** Map of theme file name to content, for parametric tests. */
    private static Map<String, String> themeCssFiles;

    @BeforeAll
    static void loadCssFiles() throws IOException {
        baseCss  = loadResource("/css/base.css");
        darkCss  = loadResource("/css/dark.css");
        lightCss = loadResource("/css/light.css");

        themeCssFiles = new LinkedHashMap<>();
        for (String name : new String[]{
                "dark-abyss", "dark-charcoal", "dark-purple",
                "light-arctic", "light-paper", "light-sakura"
        }) {
            themeCssFiles.put(name, loadResource("/css/themes/" + name + ".css"));
        }
    }

    // ── base.css structural selector tests ───────────────────────────────────

    /**
     * Verifies that {@code .scoresheet-move-number} selector block exists in
     * {@code base.css}.
     */
    @Test
    void baseCss_scoresheetMoveNumber_exists() {
        String block = extractSelectorBlock(baseCss, ".scoresheet-move-number");
        assertNotNull(block,
                ".scoresheet-move-number selector block must exist in base.css");
    }

    /**
     * Verifies that {@code .scoresheet-white-move} selector block exists in
     * {@code base.css}.
     */
    @Test
    void baseCss_scoresheetWhiteMove_exists() {
        String block = extractSelectorBlock(baseCss, ".scoresheet-white-move");
        assertNotNull(block,
                ".scoresheet-white-move selector block must exist in base.css");
    }

    /**
     * Verifies that {@code .scoresheet-black-move} selector block exists in
     * {@code base.css}.
     */
    @Test
    void baseCss_scoresheetBlackMove_exists() {
        String block = extractSelectorBlock(baseCss, ".scoresheet-black-move");
        assertNotNull(block,
                ".scoresheet-black-move selector block must exist in base.css");
    }

    /**
     * Verifies that {@code .scoresheet-move-number} in {@code base.css} declares
     * a monospace font family (Consolas, Menlo, or Courier New).
     */
    @Test
    void baseCss_scoresheetMoveNumber_hasMonospaceFont() {
        String block = extractSelectorBlock(baseCss, ".scoresheet-move-number");
        assertNotNull(block,
                ".scoresheet-move-number selector block must exist in base.css");
        assertTrue(
                block.contains("-fx-font-family") && (
                        block.contains("Consolas") ||
                        block.contains("Menlo") ||
                        block.contains("Courier New") ||
                        block.contains("monospace")),
                ".scoresheet-move-number must declare a monospace -fx-font-family");
    }

    /**
     * Verifies that {@code .scoresheet-header} selector block exists in
     * {@code base.css}.
     */
    @Test
    void baseCss_scoresheetHeader_exists() {
        String block = extractSelectorBlock(baseCss, ".scoresheet-header");
        assertNotNull(block,
                ".scoresheet-header selector block must exist in base.css");
    }

    // ── dark.css / light.css color token tests ────────────────────────────────

    /**
     * Verifies that {@code dark.css} declares {@code app-subtext} as the text
     * fill for {@code .scoresheet-move-number}.
     */
    @Test
    void darkCss_scoresheetMoveNumber_hasSubtextColor() {
        String block = extractSelectorBlock(darkCss, ".scoresheet-move-number");
        assertNotNull(block,
                ".scoresheet-move-number must be declared in dark.css");
        assertTrue(block.contains("app-subtext"),
                ".scoresheet-move-number in dark.css must use app-subtext as -fx-text-fill");
    }

    /**
     * Verifies that {@code light.css} declares {@code app-subtext} as the text
     * fill for {@code .scoresheet-move-number}.
     */
    @Test
    void lightCss_scoresheetMoveNumber_hasSubtextColor() {
        String block = extractSelectorBlock(lightCss, ".scoresheet-move-number");
        assertNotNull(block,
                ".scoresheet-move-number must be declared in light.css");
        assertTrue(block.contains("app-subtext"),
                ".scoresheet-move-number in light.css must use app-subtext as -fx-text-fill");
    }

    // ── Theme variant files ────────────────────────────────────────────────────

    /**
     * Verifies that every theme-variant CSS file (dark-abyss, dark-charcoal,
     * dark-purple, light-arctic, light-paper, light-sakura) contains a
     * {@code .scoresheet-move-number} selector.
     */
    @Test
    void allThemes_scoresheetMoveNumber_exists() {
        for (Map.Entry<String, String> entry : themeCssFiles.entrySet()) {
            String themeName = entry.getKey();
            String css       = entry.getValue();
            assertTrue(css.contains(".scoresheet-move-number"),
                    "Theme file " + themeName + ".css must contain .scoresheet-move-number");
        }
    }

    /**
     * Verifies that every theme-variant CSS file contains all four scoresheet
     * selector blocks: {@code .scoresheet-move-number}, {@code .scoresheet-white-move},
     * {@code .scoresheet-black-move}, and {@code .scoresheet-header-label}.
     *
     * <p>This companion to {@link #allThemes_scoresheetMoveNumber_exists()} ensures
     * that adding a new theme file does not accidentally omit any of the four required
     * selectors. If any selector is missing, the test names the failing theme and the
     * missing selector so the cause is immediately obvious.
     */
    @Test
    void allThemes_allScoresheetSelectors_exist() {
        String[] selectors = {
            ".scoresheet-move-number",
            ".scoresheet-white-move",
            ".scoresheet-black-move",
            ".scoresheet-header-label"
        };
        for (Map.Entry<String, String> entry : themeCssFiles.entrySet()) {
            String themeName = entry.getKey();
            String css       = entry.getValue();
            for (String selector : selectors) {
                assertTrue(css.contains(selector),
                        "Theme file " + themeName + ".css must contain " + selector);
            }
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /**
     * Extracts the first CSS declaration block that begins with exactly the given
     * selector token (not a pseudo-class or child selector variant). Returns the
     * content between the opening {@code '{'} and the matching closing {@code '}'}.
     *
     * <p>The match is performed on the plain selector string so that
     * {@code .scoresheet-move-number} does not accidentally match a compound
     * selector like {@code .scoresheet-move-number:hover}.
     *
     * @param css      the full CSS text to search
     * @param selector the exact selector to find, e.g. {@code ".scoresheet-move-number"}
     * @return the content of the declaration block (between braces), or
     *         {@code null} if the selector is not found
     */
    static String extractSelectorBlock(String css, String selector) {
        String escapedSelector = Pattern.quote(selector);
        Pattern pattern = Pattern.compile(
                escapedSelector + "\\s*\\{([^}]*)}",
                Pattern.DOTALL
        );
        Matcher matcher = pattern.matcher(css);
        while (matcher.find()) {
            String fullMatch   = matcher.group(0);
            String afterSelector = fullMatch.substring(selector.length()).stripLeading();
            if (afterSelector.startsWith("{")) {
                return matcher.group(1);
            }
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
        try (InputStream is = CssScoresheetTest.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new IOException("Resource not found on classpath: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
