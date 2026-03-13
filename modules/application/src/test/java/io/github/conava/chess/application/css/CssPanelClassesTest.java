package io.github.conava.chess.application.css;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the expected CSS class definitions are present in the project's
 * stylesheet resources. These are content-verification tests that read the CSS
 * files as text and assert the presence of the required class selectors.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>base.css — structural classes for panel container and nav button states</li>
 * </ul>
 */
class CssPanelClassesTest {

    private static String baseCss;

    @BeforeAll
    static void loadCssFiles() throws IOException {
        baseCss = loadResource("/css/base.css");
    }

    // ── base.css structural class tests ──────────────────────────────────────

    /**
     * Verifies that base.css contains the panel-container class that hosts the
     * right-side 60% panel area in the cinematic main menu layout.
     */
    @Test
    void baseCss_containsPanelContainerClass() {
        assertTrue(baseCss.contains(".cinematic-panel-container"),
                "base.css must define .cinematic-panel-container");
    }

    /**
     * Verifies that base.css contains the dimmed nav button state class applied
     * to non-active nav buttons while a panel is open.
     */
    @Test
    void baseCss_containsNavDimmedClass() {
        assertTrue(baseCss.contains(".cinematic-nav-dimmed"),
                "base.css must define .cinematic-nav-dimmed");
    }

    /**
     * Verifies that base.css contains the active nav button state class applied
     * to the button that corresponds to the currently-open panel.
     */
    @Test
    void baseCss_containsNavActiveClass() {
        assertTrue(baseCss.contains(".cinematic-nav-active"),
                "base.css must define .cinematic-nav-active");
    }

    /**
     * Verifies that base.css contains the form panel class used for setup and
     * settings panels, and also for login, register, and waiting-for-match panels.
     */
    @Test
    void baseCss_containsFormPanelClass() {
        assertTrue(baseCss.contains(".cinematic-form-panel"),
                "base.css must define .cinematic-form-panel");
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Loads a classpath resource as a UTF-8 string.
     *
     * @param path the resource path, relative to the classpath root (e.g. "/css/base.css")
     * @return the full contents of the resource as a string
     * @throws IOException if the resource cannot be found or read
     */
    private static String loadResource(String path) throws IOException {
        try (InputStream is = CssPanelClassesTest.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new IOException("Resource not found on classpath: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
