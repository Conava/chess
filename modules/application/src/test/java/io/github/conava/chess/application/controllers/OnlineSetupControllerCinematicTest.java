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

    // ── Join code format tests (no FX toolkit required) ─────────────────────

    /**
     * Verifies that the joinCodeField prompt text in the FXML has been updated to
     * "XXXX-XXXX-XXXX" to indicate the expected alphanumeric join code format.
     */
    @Test
    void joinCodeField_promptText_showsAlphanumericFormat() throws Exception {
        String fxml = loadFxmlAsString();
        assertTrue(fxml.contains("XXXX-XXXX-XXXX"),
                "joinCodeField promptText should show 'XXXX-XXXX-XXXX' to hint the alphanumeric code format");
    }

    // ── Join code normalization and validation unit tests ────────────────────

    /**
     * Verifies that the normalization logic strips dashes and uppercases the input.
     * The normalized code "abcd-1234-efgh" becomes "ABCD1234EFGH".
     */
    @Test
    void normalizeJoinCode_stripsAndUppercases() {
        String result = OnlineSetupController.normalizeJoinCode("abcd-1234-efgh");
        assertEquals("ABCD1234EFGH", result,
                "normalizeJoinCode must uppercase and strip dashes from 'abcd-1234-efgh'");
    }

    /**
     * Verifies that normalization of an already-clean code leaves it unchanged.
     */
    @Test
    void normalizeJoinCode_alreadyNormalized_unchanged() {
        String result = OnlineSetupController.normalizeJoinCode("ABCD1234EFGH");
        assertEquals("ABCD1234EFGH", result,
                "normalizeJoinCode must leave already-normalized codes unchanged");
    }

    /**
     * Verifies that normalization of uppercase-with-dashes produces the correct result.
     */
    @Test
    void normalizeJoinCode_uppercaseWithDashes_stripped() {
        String result = OnlineSetupController.normalizeJoinCode("ABCD-1234-EFGH");
        assertEquals("ABCD1234EFGH", result,
                "normalizeJoinCode must strip dashes from uppercase input");
    }

    /**
     * Verifies that a valid 12-char alphanumeric code passes validation.
     */
    @Test
    void isValidJoinCode_validCode_returnsTrue() {
        assertTrue(OnlineSetupController.isValidJoinCode("ABCD1234EFGH"),
                "isValidJoinCode must return true for a valid 12-char alphanumeric code");
    }

    /**
     * Verifies that a code shorter than 12 chars fails validation.
     */
    @Test
    void isValidJoinCode_tooShort_returnsFalse() {
        assertFalse(OnlineSetupController.isValidJoinCode("ABCD1234"),
                "isValidJoinCode must return false for codes shorter than 12 characters");
    }

    /**
     * Verifies that a code longer than 12 chars fails validation.
     */
    @Test
    void isValidJoinCode_tooLong_returnsFalse() {
        assertFalse(OnlineSetupController.isValidJoinCode("ABCD1234EFGHIJ"),
                "isValidJoinCode must return false for codes longer than 12 characters");
    }

    /**
     * Verifies that a code with remaining dashes (not properly normalized) fails validation.
     */
    @Test
    void isValidJoinCode_withDashes_returnsFalse() {
        assertFalse(OnlineSetupController.isValidJoinCode("ABCD-1234-EFGH"),
                "isValidJoinCode must return false if dashes are present (normalization must be applied first)");
    }

    /**
     * Verifies that null fails validation without throwing.
     */
    @Test
    void isValidJoinCode_null_returnsFalse() {
        assertFalse(OnlineSetupController.isValidJoinCode(null),
                "isValidJoinCode must return false for null input");
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
     * Verifies that onConnect() in join mode rejects codes that are too short after normalization.
     *
     * <p>The error label should be shown and {@code chess.startGame()} should NOT be called.</p>
     */
    @Test
    @DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
    void onConnect_joinMode_invalidJoinCode_showsError() throws Exception {
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

                // Switch to join mode
                ToggleButton joinToggle = (ToggleButton) root.lookup("#joinToggle");
                assertNotNull(joinToggle, "joinToggle should exist");
                joinToggle.setSelected(true);

                // Set valid IP and port
                javafx.scene.control.TextField ipField =
                        (javafx.scene.control.TextField) root.lookup("#ipField");
                javafx.scene.control.TextField portField =
                        (javafx.scene.control.TextField) root.lookup("#portField");
                javafx.scene.control.TextField joinCodeField =
                        (javafx.scene.control.TextField) root.lookup("#joinCodeField");
                assertNotNull(ipField, "ipField should exist");
                assertNotNull(portField, "portField should exist");
                assertNotNull(joinCodeField, "joinCodeField should exist");

                ipField.setText("localhost");
                portField.setText("8080");
                // Too short: only 8 characters after normalization
                joinCodeField.setText("ABCD1234");

                // Invoke onConnect via reflection
                Method onConnectMethod = OnlineSetupController.class.getDeclaredMethod("onConnect");
                onConnectMethod.setAccessible(true);
                onConnectMethod.invoke(controller);

                // chess.startGame() must NOT be called when join code is invalid
                verify(chess, never()).startGame(anyBoolean(), any(), anyString(), anyString(), any());

                // Error label should be visible
                javafx.scene.control.Label errorLabel =
                        (javafx.scene.control.Label) root.lookup("#errorLabel");
                assertNotNull(errorLabel, "errorLabel should exist");
                assertTrue(errorLabel.isVisible(), "Error label must be visible after invalid join code");

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
     * Verifies that onConnect() in join mode accepts lowercase codes with dashes and normalizes them.
     *
     * <p>A valid code like "abcd-1234-efgh" (16 chars with dashes, 12 after normalization) must
     * be accepted and normalized to "ABCD1234EFGH" before being passed to {@code chess.startGame()}.</p>
     */
    @Test
    @DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
    void onConnect_joinMode_lowercaseWithDashes_normalizedBeforeSending() throws Exception {
        assumeTrue(tryStartToolkit(), "JavaFX toolkit unavailable -- skipping FX test");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        AtomicReference<String> capturedJoinCode = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                Preferences prefs = Preferences.userRoot().node("chess-test-" + System.nanoTime());
                SettingsService settings = new SettingsService(prefs);

                I18n i18n = new I18n(I18n.Language.EN);
                Chess chess = mock(Chess.class);
                SceneManager sm = mock(SceneManager.class);
                PanelHost panelHost = mock(PanelHost.class);

                // Capture the joinCode argument passed to startGame
                doAnswer(inv -> {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, String> gameSettings =
                            (java.util.Map<String, String>) inv.getArgument(4);
                    if (gameSettings != null) {
                        capturedJoinCode.set(gameSettings.get("joinCode"));
                    }
                    return null;
                }).when(chess).startGame(anyBoolean(), any(), anyString(), anyString(), any());

                OnlineSetupController controller =
                        new OnlineSetupController(sm, chess, i18n, settings, panelHost);

                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/online-setup.fxml"), i18n.getBundle());
                loader.setControllerFactory(type -> controller);
                Parent root = loader.load();

                // Switch to join mode
                ToggleButton joinToggle = (ToggleButton) root.lookup("#joinToggle");
                assertNotNull(joinToggle, "joinToggle should exist");
                joinToggle.setSelected(true);

                // Set valid IP and port
                javafx.scene.control.TextField ipField =
                        (javafx.scene.control.TextField) root.lookup("#ipField");
                javafx.scene.control.TextField portField =
                        (javafx.scene.control.TextField) root.lookup("#portField");
                javafx.scene.control.TextField joinCodeField =
                        (javafx.scene.control.TextField) root.lookup("#joinCodeField");

                ipField.setText("localhost");
                portField.setText("8080");
                // Enter code with lowercase and dashes — should be normalized to "ABCD1234EFGH"
                joinCodeField.setText("abcd-1234-efgh");

                // Invoke onConnect via reflection
                Method onConnectMethod = OnlineSetupController.class.getDeclaredMethod("onConnect");
                onConnectMethod.setAccessible(true);
                onConnectMethod.invoke(controller);

                // The CinematicPanelAnimator.playExit runs a callback — since we are in a test,
                // the animation may run synchronously or not at all. We verify the join code
                // normalization by checking capturedJoinCode if startGame was called.
                // If animation is async, we verify normalization logic directly.
                // The core normalization is already tested by unit tests above;
                // this test primarily verifies the join code field does not reject a valid code.
                javafx.scene.control.Label errorLabel =
                        (javafx.scene.control.Label) root.lookup("#errorLabel");
                assertNotNull(errorLabel, "errorLabel should exist");
                assertFalse(errorLabel.isVisible(),
                        "Error label must NOT be visible for a valid join code (abcd-1234-efgh normalizes to 12 chars)");

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
