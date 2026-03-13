package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.Chess;
import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.navigation.PanelHost;
import io.github.conava.chess.application.navigation.PanelId;
import io.github.conava.chess.application.navigation.SceneManager;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RegisterController} verifying PanelHost-based navigation.
 *
 * <p>These tests do not require the JavaFX toolkit because they invoke the
 * controller's action methods directly via reflection, without loading FXML or
 * involving UI components. The {@link PanelHost} dependency is mocked with
 * Mockito so that navigation calls can be asserted.</p>
 */
class RegisterControllerTest {

    /**
     * Creates a {@code RegisterController} with minimal mocked dependencies.
     * FXML fields are left null since we only test navigation logic that does
     * not touch them.
     */
    private RegisterController createController(SceneManager sm, Chess chess, I18n i18n,
                                                PanelHost panelHost) {
        return new RegisterController(sm, chess, i18n, panelHost);
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
}
