package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.Chess;
import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.navigation.PanelHost;
import io.github.conava.chess.application.navigation.SceneManager;
import io.github.conava.chess.application.settings.SettingsService;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

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
 * Tests for the redesigned cinematic offline-setup screen.
 *
 * <p>The first group of tests verify the FXML structure by reading it as a plain
 * string resource -- no JavaFX toolkit required. The second group uses the FX
 * toolkit to verify controller behaviour (start/cancel with animations).</p>
 */
class OfflineSetupControllerCinematicTest {

    // ── FXML structure tests (no FX toolkit required) ────────────────────────

    private String loadFxmlAsString() throws Exception {
        try (var is = getClass().getResourceAsStream("/fxml/offline-setup.fxml")) {
            assertNotNull(is, "offline-setup.fxml should be loadable as a resource");
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
                "Setup panel should have the cinematic-form-panel style class");
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

    @Test
    @DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
    void onStart_startsGameAndNavigates() throws Exception {
        assumeTrue(tryStartToolkit(), "JavaFX toolkit unavailable -- skipping FX test");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                Preferences prefs = Preferences.userRoot().node("chess-test-" + System.nanoTime());
                SettingsService settings = new SettingsService(prefs);

                I18n i18n = new I18n(I18n.Language.EN);
                Chess chess = mock(Chess.class);
                SceneManager sm = mock(SceneManager.class);
                PanelHost panelHost = mock(PanelHost.class);
                when(sm.getChess()).thenReturn(chess);
                // Count down the latch when showGame fires (after exit animation completes)
                doAnswer(inv -> { latch.countDown(); return null; }).when(sm).showGame(any(RulesetOptions.class));

                OfflineSetupController controller = new OfflineSetupController(sm, i18n, settings, panelHost);

                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/offline-setup.fxml"), i18n.getBundle());
                loader.setControllerFactory(type -> controller);
                Parent root = loader.load();

                assertNotNull(root, "FXML should load successfully");
                assertInstanceOf(StackPane.class, root, "Root should be a StackPane");

                // Fill in player names
                TextField whiteField = (TextField) root.lookup("#whiteField");
                TextField blackField = (TextField) root.lookup("#blackField");
                assertNotNull(whiteField, "whiteField should exist");
                assertNotNull(blackField, "blackField should exist");
                whiteField.setText("Alice");
                blackField.setText("Bob");

                // Fire start action via button lookup
                Button startBtn = (Button) root.lookup(".btn-primary");
                assertNotNull(startBtn, "Start button should exist");
                startBtn.fire();

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
     * Verifies that cancel calls panelHost.closePanel() instead of sceneManager.showMainMenu().
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
                SceneManager sm = mock(SceneManager.class);
                PanelHost panelHost = mock(PanelHost.class);
                // Count down the latch when closePanel fires (after exit animation completes)
                doAnswer(inv -> { latch.countDown(); return null; }).when(panelHost).closePanel();

                OfflineSetupController controller = new OfflineSetupController(sm, i18n, settings, panelHost);

                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/offline-setup.fxml"), i18n.getBundle());
                loader.setControllerFactory(type -> controller);
                Parent root = loader.load();

                assertNotNull(root, "FXML should load successfully");

                // Fire cancel action via button lookup
                Button cancelBtn = (Button) root.lookup(".btn-ghost");
                assertNotNull(cancelBtn, "Cancel button should exist");
                cancelBtn.fire();

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

    @Test
    @DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
    void playerNameDefaults_preserved() throws Exception {
        assumeTrue(tryStartToolkit(), "JavaFX toolkit unavailable -- skipping FX test");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                Preferences prefs = Preferences.userRoot().node("chess-test-" + System.nanoTime());
                SettingsService settings = new SettingsService(prefs);
                settings.savePlayerWhite("Saved White");
                settings.savePlayerBlack("Saved Black");

                I18n i18n = new I18n(I18n.Language.EN);
                SceneManager sm = mock(SceneManager.class);
                PanelHost panelHost = mock(PanelHost.class);

                OfflineSetupController controller = new OfflineSetupController(sm, i18n, settings, panelHost);

                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/offline-setup.fxml"), i18n.getBundle());
                loader.setControllerFactory(type -> controller);
                Parent root = loader.load();

                // Verify fields are pre-filled from settings
                TextField whiteField = (TextField) root.lookup("#whiteField");
                TextField blackField = (TextField) root.lookup("#blackField");
                assertEquals("Saved White", whiteField.getText(),
                        "White field should be pre-filled from settings");
                assertEquals("Saved Black", blackField.getText(),
                        "Black field should be pre-filled from settings");

            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS), "FX task did not complete in time");
        if (error.get() != null) {
            throw new AssertionError("FX-thread assertion failed", error.get());
        }
    }

    @Test
    @DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
    void rulesetSelection_preserved() throws Exception {
        assumeTrue(tryStartToolkit(), "JavaFX toolkit unavailable -- skipping FX test");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                Preferences prefs = Preferences.userRoot().node("chess-test-" + System.nanoTime());
                SettingsService settings = new SettingsService(prefs);

                I18n i18n = new I18n(I18n.Language.EN);
                SceneManager sm = mock(SceneManager.class);
                PanelHost panelHost = mock(PanelHost.class);

                OfflineSetupController controller = new OfflineSetupController(sm, i18n, settings, panelHost);

                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/offline-setup.fxml"), i18n.getBundle());
                loader.setControllerFactory(type -> controller);
                Parent root = loader.load();

                // Verify ComboBox is populated
                @SuppressWarnings("unchecked")
                ComboBox<RulesetOptions> rulesetBox =
                        (ComboBox<RulesetOptions>) root.lookup("#rulesetBox");
                assertNotNull(rulesetBox, "rulesetBox should exist");
                assertFalse(rulesetBox.getItems().isEmpty(),
                        "Ruleset ComboBox should be populated");
                assertEquals(RulesetOptions.values().length, rulesetBox.getItems().size(),
                        "ComboBox should contain all RulesetOptions values");
                assertNotNull(rulesetBox.getValue(),
                        "A default ruleset should be selected");

            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS), "FX task did not complete in time");
        if (error.get() != null) {
            throw new AssertionError("FX-thread assertion failed", error.get());
        }
    }
}
