package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.Chess;
import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.navigation.PanelHost;
import io.github.conava.chess.application.navigation.PanelId;
import io.github.conava.chess.application.navigation.SceneManager;
import io.github.conava.chess.application.settings.SettingsService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.*;

/**
 * Unit and FX-thread tests for {@link RegisterController}.
 *
 * <p>The unit tests verify PanelHost-based navigation without loading FXML or
 * requiring the JavaFX toolkit. The FX-thread tests verify IP/port pre-fill
 * behaviour from {@link SettingsService} settings.</p>
 */
class RegisterControllerTest {

    /**
     * Creates a {@code RegisterController} with minimal mocked dependencies.
     * FXML fields are left null since we only test navigation logic that does
     * not touch them.
     */
    private RegisterController createController(SceneManager sm, Chess chess, I18n i18n,
                                                PanelHost panelHost) {
        return new RegisterController(sm, chess, i18n, panelHost, mock(SettingsService.class));
    }

    /**
     * Verifies that pressing Cancel calls {@link PanelHost#closePanel()} and
     * does NOT call any SceneManager navigation method.
     */
    @Test
    void onCancel_callsPanelHostClosePanel() throws Exception {
        SceneManager sm = mock(SceneManager.class);
        Chess chess = mock(Chess.class);
        I18n i18n = new I18n(I18n.Language.EN);
        PanelHost panelHost = mock(PanelHost.class);

        RegisterController controller = createController(sm, chess, i18n, panelHost);

        Method onCancelMethod = RegisterController.class.getDeclaredMethod("onCancel");
        onCancelMethod.setAccessible(true);
        onCancelMethod.invoke(controller);

        verify(panelHost).closePanel();
        verifyNoInteractions(sm);
    }

    /**
     * Verifies that the "Already have an account? Login" link calls
     * {@link PanelHost#switchPanel(PanelId)} with {@link PanelId#LOGIN}.
     */
    @Test
    void onLogin_callsSwitchPanelToLogin() throws Exception {
        SceneManager sm = mock(SceneManager.class);
        Chess chess = mock(Chess.class);
        I18n i18n = new I18n(I18n.Language.EN);
        PanelHost panelHost = mock(PanelHost.class);

        RegisterController controller = createController(sm, chess, i18n, panelHost);

        Method onLoginMethod = RegisterController.class.getDeclaredMethod("onLogin");
        onLoginMethod.setAccessible(true);
        onLoginMethod.invoke(controller);

        verify(panelHost).switchPanel(PanelId.LOGIN);
        verifyNoInteractions(sm);
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

    // ── FX-thread tests: IP/port pre-fill ────────────────────────────────────

    /**
     * Verifies that when saved server host and port exist in settings, the
     * {@code ipField} and {@code portField} are pre-filled with those values on
     * panel initialization.
     */
    @Test
    @DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
    void registerController_prefillsIpPortFromSettings() throws Exception {
        assumeTrue(tryStartToolkit(), "JavaFX toolkit unavailable -- skipping FX test");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                Preferences prefs = Preferences.userRoot().node("chess-test-" + System.nanoTime());
                SettingsService settings = new SettingsService(prefs);
                settings.saveServerHost("10.0.0.5");
                settings.saveServerPort(7777);

                I18n i18n = new I18n(I18n.Language.EN);
                Chess chess = mock(Chess.class);
                SceneManager sm = mock(SceneManager.class);
                PanelHost panelHost = mock(PanelHost.class);

                RegisterController controller =
                        new RegisterController(sm, chess, i18n, panelHost, settings);

                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/register.fxml"), i18n.getBundle());
                loader.setControllerFactory(type -> controller);
                Parent root = loader.load();
                assertNotNull(root, "FXML should load successfully");

                TextField ipField = (TextField) root.lookup("#ipField");
                TextField portField = (TextField) root.lookup("#portField");
                assertNotNull(ipField, "ipField should exist");
                assertNotNull(portField, "portField should exist");

                assertEquals("10.0.0.5", ipField.getText(),
                        "ipField should be pre-filled from saved server host");
                assertEquals("7777", portField.getText(),
                        "portField should be pre-filled from saved server port");

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
