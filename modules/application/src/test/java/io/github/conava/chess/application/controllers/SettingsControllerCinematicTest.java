package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.navigation.PanelHost;
import io.github.conava.chess.application.navigation.SceneManager;
import io.github.conava.chess.application.settings.SettingsService;
import io.github.conava.chess.application.theme.Theme;
import io.github.conava.chess.application.theme.ThemeManager;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.*;

/**
 * Tests for the redesigned cinematic settings screen.
 *
 * <p>The first group of tests verify the FXML structure by reading it as a plain
 * string resource -- no JavaFX toolkit required. The second group uses the FX
 * toolkit to verify controller behaviour (save/cancel with animations).</p>
 */
class SettingsControllerCinematicTest {

    // ── FXML structure tests (no FX toolkit required) ────────────────────────

    private String loadFxmlAsString() throws Exception {
        try (var is = getClass().getResourceAsStream("/fxml/settings.fxml")) {
            assertNotNull(is, "settings.fxml should be loadable as a resource");
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void fxmlRoot_isStackPane() throws Exception {
        String fxml = loadFxmlAsString();
        assertTrue(fxml.contains("<StackPane"),
                "Root element should be a StackPane, not HBox");
        assertFalse(fxml.contains("<HBox") && fxml.indexOf("<HBox") < fxml.indexOf("<StackPane"),
                "HBox should not appear before StackPane as root element");
    }

    @Test
    void noBrandPanel() throws Exception {
        String fxml = loadFxmlAsString();
        assertFalse(fxml.contains("menu-brand-panel"),
                "FXML should not contain the old menu-brand-panel style class");
    }

    @Test
    void formContent_hasCinematicFormPanelClass() throws Exception {
        String fxml = loadFxmlAsString();
        assertTrue(fxml.contains("cinematic-form-panel"),
                "Settings panel should have the cinematic-form-panel style class");
    }

    @Test
    void formContent_wrappedInScrollPane() throws Exception {
        String fxml = loadFxmlAsString();
        assertTrue(fxml.contains("<ScrollPane"),
                "Form content should be wrapped in a ScrollPane");
    }

    @Test
    void rootHasTransparentBackground() throws Exception {
        String fxml = loadFxmlAsString();
        assertTrue(fxml.contains("-fx-background-color: transparent"),
                "Root StackPane should have transparent background style");
    }

    // ── FX toolkit helper ───────────────────────────────────────────────────

    /**
     * Starts the JavaFX toolkit if not already running. Returns true on success.
     */
    private static boolean tryStartToolkit() {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicBoolean started = new AtomicBoolean(false);
            try {
                Platform.startup(() -> {
                    started.set(true);
                    latch.countDown();
                });
                latch.await(5, TimeUnit.SECONDS);
                return started.get();
            } catch (IllegalStateException e) {
                // Toolkit already running
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    // ── FX-thread tests ─────────────────────────────────────────────────────

    /**
     * Verifies that save persists settings and calls panelHost.closePanel().
     */
    @Test
    @DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
    void onSave_savesSettingsAndCallsPanelHostClosePanel() throws Exception {
        assumeTrue(tryStartToolkit(), "JavaFX toolkit unavailable -- skipping FX test");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                Preferences prefs = Preferences.userRoot().node("chess-test-" + System.nanoTime());
                SettingsService settings = new SettingsService(prefs);

                I18n i18n = new I18n(I18n.Language.EN);
                ThemeManager themeManager = new ThemeManager();
                themeManager.setTheme(Theme.DARK_PURPLE);

                SceneManager sm = mock(SceneManager.class);
                PanelHost panelHost = mock(PanelHost.class);
                // Count down the latch when closePanel fires (after exit animation completes)
                doAnswer(inv -> { latch.countDown(); return null; }).when(panelHost).closePanel();

                SettingsController controller = new SettingsController(sm, themeManager, i18n, settings, panelHost);

                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/settings.fxml"), i18n.getBundle());
                loader.setControllerFactory(type -> controller);
                Parent root = loader.load();

                assertNotNull(root, "FXML should load successfully");
                assertInstanceOf(StackPane.class, root, "Root should be a StackPane");

                // Invoke onSave via reflection (method is private @FXML)
                Method onSaveMethod = SettingsController.class.getDeclaredMethod("onSave");
                onSaveMethod.setAccessible(true);
                onSaveMethod.invoke(controller);

                // Verify settings were persisted (happens synchronously before animation)
                assertEquals(Theme.DARK_PURPLE.name(),
                        prefs.get("theme", null),
                        "Theme should be saved");
                assertEquals(I18n.Language.EN.name(),
                        prefs.get("language", null),
                        "Language should be saved");

            } catch (Throwable t) {
                error.set(t);
                latch.countDown();
            }
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS), "FX task did not complete in time");
        if (error.get() != null) {
            throw new AssertionError("FX-thread assertion failed", error.get());
        }
    }

    /**
     * Verifies that cancel reverts settings and calls panelHost.closePanel().
     */
    @Test
    @DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
    void onCancel_callsPanelHostClosePanel() throws Exception {
        assumeTrue(tryStartToolkit(), "JavaFX toolkit unavailable -- skipping FX test");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                Preferences prefs = Preferences.userRoot().node("chess-test-" + System.nanoTime());
                SettingsService settings = new SettingsService(prefs);

                I18n i18n = new I18n(I18n.Language.EN);
                ThemeManager themeManager = new ThemeManager();
                themeManager.setTheme(Theme.DARK_PURPLE);

                SceneManager sm = mock(SceneManager.class);
                PanelHost panelHost = mock(PanelHost.class);
                // Count down the latch when closePanel fires (after exit animation completes)
                doAnswer(inv -> { latch.countDown(); return null; }).when(panelHost).closePanel();

                SettingsController controller = new SettingsController(sm, themeManager, i18n, settings, panelHost);

                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/settings.fxml"), i18n.getBundle());
                loader.setControllerFactory(type -> controller);
                Parent root = loader.load();

                assertNotNull(root, "FXML should load successfully");

                // Invoke onCancel via reflection (method is private @FXML)
                Method onCancelMethod = SettingsController.class.getDeclaredMethod("onCancel");
                onCancelMethod.setAccessible(true);
                onCancelMethod.invoke(controller);

            } catch (Throwable t) {
                error.set(t);
                latch.countDown();
            }
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS), "FX task did not complete in time");
        if (error.get() != null) {
            throw new AssertionError("FX-thread assertion failed", error.get());
        }
    }
}
