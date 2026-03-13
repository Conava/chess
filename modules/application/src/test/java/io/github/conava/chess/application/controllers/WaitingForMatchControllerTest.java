package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.Chess;
import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.navigation.PanelHost;
import io.github.conava.chess.application.navigation.SceneManager;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link WaitingForMatchController} verifying PanelHost-based navigation.
 *
 * <p>These tests do not require the JavaFX toolkit because they invoke the
 * controller's action methods directly via reflection, without loading FXML or
 * involving UI components. The {@link PanelHost} and {@link SceneManager}
 * dependencies are mocked with Mockito so that navigation calls can be asserted.</p>
 */
class WaitingForMatchControllerTest {

    /**
     * Creates a {@code WaitingForMatchController} with minimal mocked dependencies.
     * FXML fields are left null; {@link Chess#getActiveServerTask()} returns null so
     * the constructor skips handler registration safely.
     */
    private WaitingForMatchController createController(SceneManager sm, Chess chess, I18n i18n,
                                                       PanelHost panelHost) {
        when(chess.getActiveServerTask()).thenReturn(null);
        return new WaitingForMatchController(sm, chess, i18n,
                RulesetOptions.STANDARD, "localhost", 54321, panelHost);
    }

    /**
     * Verifies that pressing Cancel calls {@link PanelHost#closePanel()} and leaves the
     * matchmaking queue via {@link Chess#leaveMatchmakingQueue()}.
     */
    @Test
    void onCancel_leavesQueueAndCallsPanelHostClosePanel() throws Exception {
        SceneManager sm = mock(SceneManager.class);
        Chess chess = mock(Chess.class);
        I18n i18n = new I18n(I18n.Language.EN);
        PanelHost panelHost = mock(PanelHost.class);

        WaitingForMatchController controller = createController(sm, chess, i18n, panelHost);

        Method onCancelMethod = WaitingForMatchController.class.getDeclaredMethod("onCancel");
        onCancelMethod.setAccessible(true);
        onCancelMethod.invoke(controller);

        verify(chess).leaveMatchmakingQueue();
        verify(panelHost).closePanel();
        // SceneManager should NOT be called for cancel navigation
        verifyNoInteractions(sm);
    }
}
