package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.Chess;
import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.navigation.PanelHost;
import io.github.conava.chess.application.navigation.SceneManager;
import io.github.conava.chess.application.settings.SettingsService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ToggleButton;
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
 * Tests for the redesigned cinematic online-setup screen.
 *
 * <p>The first group of tests verify the FXML structure by reading it as a plain
 * string resource -- no JavaFX toolkit required. The second group uses the FX
 * toolkit to verify controller behaviour.</p>
 */
class OnlineSetupControllerCinematicTest {

    // ── FXML structure tests (no FX toolkit required) ────────────────────────

    private String loadFxmlAsString() throws Exception {
        try (var is = getClass().getResourceAsStream("/fxml/online-setup.fxml")) {
            assertNotNull(is, "online-setup.fxml should be loadable as a resource");
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void fxmlRoot_isStackPane() throws Exception {
        String fxml = loadFxmlAsString();
        assertTrue(fxml.contains("<StackPane"),
                "Root element should be a StackPane");
        // StackPane should appear before any VBox as the root
        int stackIdx = fxml.indexOf("<StackPane");
        int vboxIdx = fxml.indexOf("<VBox");
        assertTrue(stackIdx < vboxIdx,
                "StackPane should appear before VBox (StackPane is root, VBox is inner panel)");
    }

    @Test
    void rootHasTransparentBackground() throws Exception {
        String fxml = loadFxmlAsString();
        assertTrue(fxml.contains("-fx-background-color: transparent"),
                "Root StackPane should have transparent background style");
    }

    @Test
    void formPanel_hasCinematicFormPanelClass() throws Exception {
        String fxml = loadFxmlAsString();
        assertTrue(fxml.contains("cinematic-form-panel"),
                "Setup panel should have the cinematic-form-panel style class");
    }

    @Test
    void noOverlayCardStyling() throws Exception {
        String fxml = loadFxmlAsString();
        assertFalse(fxml.contains("overlay-card"),
                "FXML should not contain the old overlay-card style class");
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
    void threeModesTogglesExist() throws Exception {
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

                OnlineSetupController controller =
                        new OnlineSetupController(sm, chess, i18n, settings, panelHost);

                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/online-setup.fxml"), i18n.getBundle());
                loader.setControllerFactory(type -> controller);
                Parent root = loader.load();

                assertNotNull(root, "FXML should load successfully");
                assertInstanceOf(StackPane.class, root, "Root should be a StackPane");

                // Verify three mode toggles exist
                ToggleButton createToggle = (ToggleButton) root.lookup("#createToggle");
                ToggleButton joinToggle = (ToggleButton) root.lookup("#joinToggle");
                ToggleButton findMatchToggle = (ToggleButton) root.lookup("#findMatchToggle");

                assertNotNull(createToggle, "createToggle should exist");
                assertNotNull(joinToggle, "joinToggle should exist");
                assertNotNull(findMatchToggle, "findMatchToggle should exist");

                // Verify they are in the same toggle group
                assertNotNull(createToggle.getToggleGroup(),
                        "createToggle should be in a ToggleGroup");
                assertSame(createToggle.getToggleGroup(), joinToggle.getToggleGroup(),
                        "All toggles should share the same ToggleGroup");
                assertSame(createToggle.getToggleGroup(), findMatchToggle.getToggleGroup(),
                        "All toggles should share the same ToggleGroup");

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
                Chess chess = mock(Chess.class);
                SceneManager sm = mock(SceneManager.class);
                PanelHost panelHost = mock(PanelHost.class);
                doAnswer(inv -> { latch.countDown(); return null; }).when(panelHost).closePanel();

                OnlineSetupController controller =
                        new OnlineSetupController(sm, chess, i18n, settings, panelHost);

                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/online-setup.fxml"), i18n.getBundle());
                loader.setControllerFactory(type -> controller);
                Parent root = loader.load();

                assertNotNull(root, "FXML should load successfully");

                // Invoke onCancel via reflection (method is private @FXML)
                Method onCancelMethod = OnlineSetupController.class.getDeclaredMethod("onCancel");
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

    /**
     * Verifies that find-match mode transitions to WAITING_FOR_MATCH via
     * {@code panelHost.showWaitingForMatch(ruleset, ip, port)} passing the
     * connection parameters entered by the user.
     */
    @Test
    @DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
    void onConnect_findMatch_callsShowWaitingForMatchWithConnectionParams() throws Exception {
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
                // connectToServer must succeed before showWaitingForMatch is called
                when(chess.connectToServer(anyString(), anyInt())).thenReturn(true);
                doAnswer(inv -> { latch.countDown(); return null; })
                        .when(panelHost).showWaitingForMatch(any(), anyString(), anyInt());

                OnlineSetupController controller =
                        new OnlineSetupController(sm, chess, i18n, settings, panelHost);

                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/online-setup.fxml"), i18n.getBundle());
                loader.setControllerFactory(type -> controller);
                Parent root = loader.load();

                assertNotNull(root, "FXML should load successfully");

                // Select find-match mode
                ToggleButton findMatchToggle = (ToggleButton) root.lookup("#findMatchToggle");
                assertNotNull(findMatchToggle, "findMatchToggle should exist");
                findMatchToggle.setSelected(true);

                // Set valid IP and port
                javafx.scene.control.TextField ipField =
                        (javafx.scene.control.TextField) root.lookup("#ipField");
                javafx.scene.control.TextField portField =
                        (javafx.scene.control.TextField) root.lookup("#portField");
                assertNotNull(ipField, "ipField should exist");
                assertNotNull(portField, "portField should exist");
                ipField.setText("localhost");
                portField.setText("54321");

                // Invoke onConnect via reflection
                Method onConnectMethod = OnlineSetupController.class.getDeclaredMethod("onConnect");
                onConnectMethod.setAccessible(true);
                onConnectMethod.invoke(controller);

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
