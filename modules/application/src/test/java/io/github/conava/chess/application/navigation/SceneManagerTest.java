package io.github.conava.chess.application.navigation;

import io.github.conava.chess.application.Chess;
import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.settings.SettingsService;
import io.github.conava.chess.application.theme.ThemeManager;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.*;

class SceneManagerTest {

    // ── Accessor tests (no FX toolkit required) ───────────────────────────────

    @Test
    void accessors_returnInjectedDependencies() {
        Stage mockStage = mock(Stage.class);
        Chess mockChess = mock(Chess.class);
        ThemeManager mockTheme = mock(ThemeManager.class);
        I18n mockI18n = mock(I18n.class);
        SettingsService mockSettings = mock(SettingsService.class);

        SceneManager sm = new SceneManager(mockStage, mockChess, mockTheme, mockI18n, mockSettings);

        assertSame(mockStage, sm.getPrimaryStage(), "getPrimaryStage");
        assertSame(mockChess, sm.getChess(), "getChess");
        assertSame(mockTheme, sm.getThemeManager(), "getThemeManager");
        assertSame(mockI18n, sm.getI18n(), "getI18n");
        assertSame(mockSettings, sm.getSettingsService(), "getSettingsService");
    }

    // ── IllegalStateException before any scene is shown ─────────────────────

    @Test
    void showConfirm_beforeSceneShown_throwsIllegalStateException() {
        SceneManager sm = new SceneManager(mock(Stage.class), mock(Chess.class), mock(ThemeManager.class), mock(I18n.class), mock(SettingsService.class));

        assertThrows(IllegalStateException.class, () -> sm.showConfirm("test"), "showConfirm should throw before any show* method is called");
    }

    @Test
    void dismissOverlay_beforeSceneShown_throwsIllegalStateException() {
        SceneManager sm = new SceneManager(mock(Stage.class), mock(Chess.class), mock(ThemeManager.class), mock(I18n.class), mock(SettingsService.class));

        assertThrows(IllegalStateException.class, sm::dismissOverlay, "dismissOverlay should throw before any show* method is called");
    }

    // ── FX-thread tests ───────────────────────────────────────────────────────

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

    @Test
    @DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
    void showGame_maximizesStage() throws Exception {
        assumeTrue(tryStartToolkit(), "JavaFX toolkit unavailable — skipping FX test");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                Stage stage = new Stage();
                ThemeManager themeManager = mock(ThemeManager.class);
                // registerScene is a no-op by default in Mockito (void method)
                I18n i18n = new I18n(I18n.Language.EN);
                SettingsService settings = new SettingsService(Preferences.userRoot().node("chess-test-" + System.nanoTime()));

                SceneManager sm = new SceneManager(stage, mock(Chess.class), themeManager, i18n, settings);

                sm.showGame(RulesetOptions.STANDARD);

                assertTrue(stage.isMaximized(), "showGame should maximize the stage");
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
    void showMainMenu_setsStageNonMaximizedWithExpectedDimensions() throws Exception {
        assumeTrue(tryStartToolkit(), "JavaFX toolkit unavailable — skipping FX test");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                Stage stage = new Stage();
                ThemeManager themeManager = mock(ThemeManager.class);
                I18n i18n = new I18n(I18n.Language.EN);
                SettingsService settings = new SettingsService(Preferences.userRoot().node("chess-test-" + System.nanoTime()));

                SceneManager sm = new SceneManager(stage, mock(Chess.class), themeManager, i18n, settings);

                sm.showMainMenu();

                assertFalse(stage.isMaximized(), "showMainMenu should not maximize the stage");
                // The SceneManager sets 980 x 700 on first show
                assertEquals(980.0, stage.getScene().getWidth(), 1.0, "scene width");
                assertEquals(700.0, stage.getScene().getHeight(), 1.0, "scene height");
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
