package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.Chess;
import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.navigation.PanelHost;
import io.github.conava.chess.application.navigation.SceneManager;
import io.github.conava.chess.application.settings.SettingsService;
import io.github.conava.chess.application.theme.Theme;
import io.github.conava.chess.application.theme.ThemeManager;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
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

    // ── New FXML structure tests for Online section ───────────────────────────

    /**
     * Verifies the Online section heading appears in the FXML and the Player Names
     * section heading does not.
     */
    @Test
    void fxmlHasOnlineSection() throws Exception {
        String fxml = loadFxmlAsString();
        assertTrue(fxml.contains("%settings.online"),
                "FXML should contain %settings.online section heading");
        assertFalse(fxml.contains("%settings.players"),
                "FXML should not contain the old %settings.players section heading");
    }

    /**
     * Verifies the server IP text field is declared in the FXML.
     */
    @Test
    void fxmlHasServerIpField() throws Exception {
        String fxml = loadFxmlAsString();
        assertTrue(fxml.contains("fx:id=\"serverIpField\""),
                "FXML should contain serverIpField");
    }

    /**
     * Verifies the server port text field is declared in the FXML.
     */
    @Test
    void fxmlHasServerPortField() throws Exception {
        String fxml = loadFxmlAsString();
        assertTrue(fxml.contains("fx:id=\"serverPortField\""),
                "FXML should contain serverPortField");
    }

    /**
     * Verifies the login status label is declared in the FXML.
     */
    @Test
    void fxmlHasLoginStatusLabel() throws Exception {
        String fxml = loadFxmlAsString();
        assertTrue(fxml.contains("fx:id=\"loginStatusLabel\""),
                "FXML should contain loginStatusLabel");
    }

    /**
     * Verifies the logout button is declared in the FXML.
     */
    @Test
    void fxmlHasLogoutButton() throws Exception {
        String fxml = loadFxmlAsString();
        assertTrue(fxml.contains("fx:id=\"logoutBtn\""),
                "FXML should contain logoutBtn");
    }

    /**
     * Verifies the old player name fields are no longer in the FXML.
     */
    @Test
    void fxmlDoesNotContainPlayerNameFields() throws Exception {
        String fxml = loadFxmlAsString();
        assertFalse(fxml.contains("whiteNameField"),
                "FXML should not contain whiteNameField");
        assertFalse(fxml.contains("blackNameField"),
                "FXML should not contain blackNameField");
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
     * Player name fields are no longer present -- only theme, language, and server settings.
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
                Chess chess = mock(Chess.class);
                // Count down the latch when closePanel fires (after exit animation completes)
                doAnswer(inv -> { latch.countDown(); return null; }).when(panelHost).closePanel();

                SettingsController controller = new SettingsController(sm, themeManager, i18n, settings, panelHost, chess);

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
     * Verifies that save persists server IP and port fields.
     */
    @Test
    @DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
    void onSave_savesServerSettings() throws Exception {
        assumeTrue(tryStartToolkit(), "JavaFX toolkit unavailable -- skipping FX test");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                Preferences prefs = Preferences.userRoot().node("chess-test-" + System.nanoTime());
                SettingsService settings = new SettingsService(prefs);

                I18n i18n = new I18n(I18n.Language.EN);
                ThemeManager themeManager = new ThemeManager();
                SceneManager sm = mock(SceneManager.class);
                PanelHost panelHost = mock(PanelHost.class);
                Chess chess = mock(Chess.class);

                SettingsController controller = new SettingsController(sm, themeManager, i18n, settings, panelHost, chess);

                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/settings.fxml"), i18n.getBundle());
                loader.setControllerFactory(type -> controller);
                loader.load();

                // Set IP and port fields
                TextField serverIpField = (TextField) loader.getNamespace().get("serverIpField");
                TextField serverPortField = (TextField) loader.getNamespace().get("serverPortField");
                assertNotNull(serverIpField, "serverIpField should exist");
                assertNotNull(serverPortField, "serverPortField should exist");
                serverIpField.setText("10.0.0.1");
                serverPortField.setText("9999");

                // Invoke onSave
                Method onSaveMethod = SettingsController.class.getDeclaredMethod("onSave");
                onSaveMethod.setAccessible(true);
                onSaveMethod.invoke(controller);

                // Verify server settings were persisted
                assertEquals("10.0.0.1", settings.loadServerHost(),
                        "Server host should be saved");
                assertEquals(9999, settings.loadServerPort(),
                        "Server port should be saved");

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
     * Verifies that server IP and port fields are pre-filled from settings on initialize.
     */
    @Test
    @DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
    void initialize_prefillsServerFieldsFromSettings() throws Exception {
        assumeTrue(tryStartToolkit(), "JavaFX toolkit unavailable -- skipping FX test");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                Preferences prefs = Preferences.userRoot().node("chess-test-" + System.nanoTime());
                SettingsService settings = new SettingsService(prefs);
                settings.saveServerHost("myhost");
                settings.saveServerPort(8080);

                I18n i18n = new I18n(I18n.Language.EN);
                ThemeManager themeManager = new ThemeManager();
                SceneManager sm = mock(SceneManager.class);
                PanelHost panelHost = mock(PanelHost.class);
                Chess chess = mock(Chess.class);

                SettingsController controller = new SettingsController(sm, themeManager, i18n, settings, panelHost, chess);

                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/settings.fxml"), i18n.getBundle());
                loader.setControllerFactory(type -> controller);
                loader.load();

                TextField serverIpField = (TextField) loader.getNamespace().get("serverIpField");
                TextField serverPortField = (TextField) loader.getNamespace().get("serverPortField");
                assertNotNull(serverIpField, "serverIpField should exist");
                assertNotNull(serverPortField, "serverPortField should exist");

                assertEquals("myhost", serverIpField.getText(),
                        "serverIpField should be pre-filled with saved host");
                assertEquals("8080", serverPortField.getText(),
                        "serverPortField should be pre-filled with saved port");

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
     * Verifies that a fresh {@link SettingsService} (no explicitly saved port) causes
     * the server port field to display the default value "54321" on initialization.
     *
     * <p>The default port is 54321 as returned by {@link SettingsService#loadServerPort()}
     * when no port has been persisted. The field must show this value immediately after
     * the FXML is loaded so the user can see the current effective default.</p>
     */
    @Test
    @DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
    void initialize_showsDefaultPortOnFreshSettings() throws Exception {
        assumeTrue(tryStartToolkit(), "JavaFX toolkit unavailable -- skipping FX test");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                // Fresh prefs node -- no port has been explicitly saved
                Preferences prefs = Preferences.userRoot().node("chess-test-" + System.nanoTime());
                SettingsService settings = new SettingsService(prefs);

                I18n i18n = new I18n(I18n.Language.EN);
                ThemeManager themeManager = new ThemeManager();
                SceneManager sm = mock(SceneManager.class);
                PanelHost panelHost = mock(PanelHost.class);
                Chess chess = mock(Chess.class);

                SettingsController controller = new SettingsController(sm, themeManager, i18n, settings, panelHost, chess);

                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/settings.fxml"), i18n.getBundle());
                loader.setControllerFactory(type -> controller);
                loader.load();

                TextField serverPortField = (TextField) loader.getNamespace().get("serverPortField");
                assertNotNull(serverPortField, "serverPortField should exist");

                assertEquals("54321", serverPortField.getText(),
                        "serverPortField should display the default port 54321 on a fresh SettingsService");

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
     * Verifies that the login status label shows the username when authenticated.
     */
    @Test
    @DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
    void initialize_showsLoggedInStatus_whenAuthenticated() throws Exception {
        assumeTrue(tryStartToolkit(), "JavaFX toolkit unavailable -- skipping FX test");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                Preferences prefs = Preferences.userRoot().node("chess-test-" + System.nanoTime());
                SettingsService settings = new SettingsService(prefs);

                I18n i18n = new I18n(I18n.Language.EN);
                ThemeManager themeManager = new ThemeManager();
                SceneManager sm = mock(SceneManager.class);
                PanelHost panelHost = mock(PanelHost.class);
                Chess chess = mock(Chess.class);
                when(chess.isAuthenticated()).thenReturn(true);
                when(chess.getUsername()).thenReturn("Alice");

                SettingsController controller = new SettingsController(sm, themeManager, i18n, settings, panelHost, chess);

                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/settings.fxml"), i18n.getBundle());
                loader.setControllerFactory(type -> controller);
                loader.load();

                Label loginStatusLabel = (Label) loader.getNamespace().get("loginStatusLabel");
                Button logoutBtn = (Button) loader.getNamespace().get("logoutBtn");
                assertNotNull(loginStatusLabel, "loginStatusLabel should exist");
                assertNotNull(logoutBtn, "logoutBtn should exist");

                assertTrue(loginStatusLabel.getText().contains("Alice"),
                        "Login status label should contain username 'Alice'. Actual: " + loginStatusLabel.getText());
                assertFalse(logoutBtn.isDisabled(),
                        "Logout button should be enabled when authenticated");

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
     * Verifies that the login status label shows "Not logged in" when not authenticated
     * and the logout button is disabled.
     */
    @Test
    @DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
    void initialize_showsNotLoggedIn_whenNotAuthenticated() throws Exception {
        assumeTrue(tryStartToolkit(), "JavaFX toolkit unavailable -- skipping FX test");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                Preferences prefs = Preferences.userRoot().node("chess-test-" + System.nanoTime());
                SettingsService settings = new SettingsService(prefs);

                I18n i18n = new I18n(I18n.Language.EN);
                ThemeManager themeManager = new ThemeManager();
                SceneManager sm = mock(SceneManager.class);
                PanelHost panelHost = mock(PanelHost.class);
                Chess chess = mock(Chess.class);
                when(chess.isAuthenticated()).thenReturn(false);

                SettingsController controller = new SettingsController(sm, themeManager, i18n, settings, panelHost, chess);

                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/settings.fxml"), i18n.getBundle());
                loader.setControllerFactory(type -> controller);
                loader.load();

                Label loginStatusLabel = (Label) loader.getNamespace().get("loginStatusLabel");
                Button logoutBtn = (Button) loader.getNamespace().get("logoutBtn");
                assertNotNull(loginStatusLabel, "loginStatusLabel should exist");
                assertNotNull(logoutBtn, "logoutBtn should exist");

                assertTrue(logoutBtn.isDisabled(),
                        "Logout button should be disabled when not authenticated");
                // Status label should show a non-empty "not logged in" message
                assertFalse(loginStatusLabel.getText().isBlank(),
                        "Login status label should not be blank when not authenticated");

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
     * Verifies that onLogout calls chess.logout() and updates the UI to disabled logout button.
     */
    @Test
    @DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
    void onLogout_callsChessLogoutAndUpdatesUI() throws Exception {
        assumeTrue(tryStartToolkit(), "JavaFX toolkit unavailable -- skipping FX test");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                Preferences prefs = Preferences.userRoot().node("chess-test-" + System.nanoTime());
                SettingsService settings = new SettingsService(prefs);

                I18n i18n = new I18n(I18n.Language.EN);
                ThemeManager themeManager = new ThemeManager();
                SceneManager sm = mock(SceneManager.class);
                PanelHost panelHost = mock(PanelHost.class);
                Chess chess = mock(Chess.class);
                // Initially authenticated
                when(chess.isAuthenticated()).thenReturn(true).thenReturn(false);
                when(chess.getUsername()).thenReturn("Alice");

                SettingsController controller = new SettingsController(sm, themeManager, i18n, settings, panelHost, chess);

                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/settings.fxml"), i18n.getBundle());
                loader.setControllerFactory(type -> controller);
                loader.load();

                Button logoutBtn = (Button) loader.getNamespace().get("logoutBtn");
                assertNotNull(logoutBtn, "logoutBtn should exist");

                // Invoke onLogout via reflection
                Method onLogoutMethod = SettingsController.class.getDeclaredMethod("onLogout");
                onLogoutMethod.setAccessible(true);
                onLogoutMethod.invoke(controller);

                // chess.logout() should have been called
                verify(chess).logout();

                // Logout button should now be disabled
                assertTrue(logoutBtn.isDisabled(),
                        "Logout button should be disabled after logout");

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
                Chess chess = mock(Chess.class);
                // Count down the latch when closePanel fires (after exit animation completes)
                doAnswer(inv -> { latch.countDown(); return null; }).when(panelHost).closePanel();

                SettingsController controller = new SettingsController(sm, themeManager, i18n, settings, panelHost, chess);

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
