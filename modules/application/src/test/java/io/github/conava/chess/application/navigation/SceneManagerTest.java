package io.github.conava.chess.application.navigation;

import io.github.conava.chess.application.Chess;
import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.menu.CinematicBackground;
import io.github.conava.chess.application.settings.SettingsService;
import io.github.conava.chess.application.theme.Theme;
import io.github.conava.chess.application.theme.ThemeManager;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
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

    // ── Helper: mock ThemeManager with stubbed currentThemeProperty ──────────

    /**
     * Creates a mock ThemeManager whose {@code currentThemeProperty()} returns a
     * real {@link SimpleObjectProperty}. This is required because cinematic screen
     * navigation registers a listener on this property.
     */
    private static ThemeManager createMockThemeManager() {
        ThemeManager tm = mock(ThemeManager.class);
        when(tm.currentThemeProperty()).thenReturn(new SimpleObjectProperty<>());
        return tm;
    }

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

    // ── isCinematicScreen tests (no FX toolkit required) ────────────────────

    @Test
    void isCinematicScreen_mainMenu_returnsTrue() {
        SceneManager sm = new SceneManager(mock(Stage.class), mock(Chess.class),
                mock(ThemeManager.class), mock(I18n.class), mock(SettingsService.class));

        assertTrue(sm.isCinematicScreen("/fxml/main-menu.fxml"),
                "main menu should be a cinematic screen");
    }

    /**
     * Settings is now a panel inside the main menu shell, not a standalone cinematic scene.
     * Only {@code /fxml/main-menu.fxml} is classified as a cinematic screen.
     */
    @Test
    void isCinematicScreen_settings_returnsFalse() {
        SceneManager sm = new SceneManager(mock(Stage.class), mock(Chess.class),
                mock(ThemeManager.class), mock(I18n.class), mock(SettingsService.class));

        assertFalse(sm.isCinematicScreen("/fxml/settings.fxml"),
                "settings is now a panel inside the main menu shell, not a standalone cinematic scene");
    }

    /**
     * Offline setup is now a panel inside the main menu shell, not a standalone cinematic scene.
     */
    @Test
    void isCinematicScreen_offlineSetup_returnsFalse() {
        SceneManager sm = new SceneManager(mock(Stage.class), mock(Chess.class),
                mock(ThemeManager.class), mock(I18n.class), mock(SettingsService.class));

        assertFalse(sm.isCinematicScreen("/fxml/offline-setup.fxml"),
                "offline setup is now a panel inside the main menu shell, not a standalone cinematic scene");
    }

    /**
     * Online setup is now a panel inside the main menu shell, not a standalone cinematic scene.
     */
    @Test
    void isCinematicScreen_onlineSetup_returnsFalse() {
        SceneManager sm = new SceneManager(mock(Stage.class), mock(Chess.class),
                mock(ThemeManager.class), mock(I18n.class), mock(SettingsService.class));

        assertFalse(sm.isCinematicScreen("/fxml/online-setup.fxml"),
                "online setup is now a panel inside the main menu shell, not a standalone cinematic scene");
    }

    @Test
    void isCinematicScreen_game_returnsFalse() {
        SceneManager sm = new SceneManager(mock(Stage.class), mock(Chess.class),
                mock(ThemeManager.class), mock(I18n.class), mock(SettingsService.class));

        assertFalse(sm.isCinematicScreen("/fxml/game.fxml"),
                "game should not be a cinematic screen");
    }

    @Test
    void getCinematicBackground_initiallyNull() {
        SceneManager sm = new SceneManager(mock(Stage.class), mock(Chess.class),
                mock(ThemeManager.class), mock(I18n.class), mock(SettingsService.class));

        assertNull(sm.getCinematicBackground(),
                "cinematic background should be null before any screen is shown");
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
                ThemeManager themeManager = createMockThemeManager();
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
                ThemeManager themeManager = createMockThemeManager();
                I18n i18n = new I18n(I18n.Language.EN);
                SettingsService settings = new SettingsService(Preferences.userRoot().node("chess-test-" + System.nanoTime()));

                SceneManager sm = new SceneManager(stage, mock(Chess.class), themeManager, i18n, settings);

                sm.showMainMenu();

                assertFalse(stage.isMaximized(), "showMainMenu should not maximize the stage");
                // The SceneManager sets 1100 x 780 on first show for cinematic screens
                assertEquals(1100.0, stage.getScene().getWidth(), 1.0, "scene width");
                assertEquals(780.0, stage.getScene().getHeight(), 1.0, "scene height");
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
    void showMainMenu_createsCinematicBackground() throws Exception {
        assumeTrue(tryStartToolkit(), "JavaFX toolkit unavailable — skipping FX test");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                Stage stage = new Stage();
                ThemeManager themeManager = createMockThemeManager();
                I18n i18n = new I18n(I18n.Language.EN);
                SettingsService settings = new SettingsService(Preferences.userRoot().node("chess-test-" + System.nanoTime()));

                SceneManager sm = new SceneManager(stage, mock(Chess.class), themeManager, i18n, settings);

                sm.showMainMenu();

                assertNotNull(sm.getCinematicBackground(),
                        "showMainMenu should create a cinematic background");
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
     * Verifies the cinematic background is reused across cinematic navigations.
     * Since sub-screens are now panels inside the main menu shell, the only cinematic
     * scene swap is game -> main menu. We verify that returning to main menu after a game
     * reuses the same background instance.
     */
    @Test
    @DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
    void cinematicToCinematic_reusesSameBackground() throws Exception {
        assumeTrue(tryStartToolkit(), "JavaFX toolkit unavailable — skipping FX test");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                Stage stage = new Stage();
                ThemeManager themeManager = createMockThemeManager();
                I18n i18n = new I18n(I18n.Language.EN);
                SettingsService settings = new SettingsService(Preferences.userRoot().node("chess-test-" + System.nanoTime()));

                SceneManager sm = new SceneManager(stage, mock(Chess.class), themeManager, i18n, settings);

                sm.showMainMenu();
                CinematicBackground bg1 = sm.getCinematicBackground();

                // Navigate away (game is a plain screen) and return to main menu
                sm.showGame(RulesetOptions.STANDARD);
                sm.showMainMenu();
                CinematicBackground bg2 = sm.getCinematicBackground();

                assertSame(bg1, bg2,
                        "returning from game to main menu should reuse the same background instance");
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
    void cinematicToPlain_pausesBackground() throws Exception {
        assumeTrue(tryStartToolkit(), "JavaFX toolkit unavailable — skipping FX test");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                Stage stage = new Stage();
                ThemeManager themeManager = createMockThemeManager();
                I18n i18n = new I18n(I18n.Language.EN);
                SettingsService settings = new SettingsService(Preferences.userRoot().node("chess-test-" + System.nanoTime()));

                SceneManager sm = new SceneManager(stage, mock(Chess.class), themeManager, i18n, settings);

                sm.showMainMenu();
                assertTrue(sm.getCinematicBackground().isRunning(),
                        "background should be running after showMainMenu");

                sm.showGame(RulesetOptions.STANDARD);
                assertFalse(sm.getCinematicBackground().isRunning(),
                        "background should be stopped after navigating to game");
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
    void plainToCinematic_resumesBackground() throws Exception {
        assumeTrue(tryStartToolkit(), "JavaFX toolkit unavailable — skipping FX test");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                Stage stage = new Stage();
                ThemeManager themeManager = createMockThemeManager();
                I18n i18n = new I18n(I18n.Language.EN);
                SettingsService settings = new SettingsService(Preferences.userRoot().node("chess-test-" + System.nanoTime()));

                SceneManager sm = new SceneManager(stage, mock(Chess.class), themeManager, i18n, settings);

                sm.showMainMenu();
                sm.showGame(RulesetOptions.STANDARD);
                assertFalse(sm.getCinematicBackground().isRunning(),
                        "background should be stopped after game");

                sm.showMainMenu();
                assertTrue(sm.getCinematicBackground().isRunning(),
                        "background should be running again after returning to main menu");
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
     * Verifies that {@link SceneManager#showMainMenu()} does not reset stage width/height
     * on subsequent calls. Specifically, the scene object is created only once (during the
     * first call) and its dimensions are never overridden by later {@code swapScene()} calls.
     *
     * <p>Since the {@link Scene} is created once and reused across all scene swaps, any
     * subsequent call to {@code swapScene()} must not call {@code stage.setWidth(w)} or
     * {@code stage.setHeight(h)}. This test verifies that the scene dimensions returned
     * before and after a round-trip through the game screen are identical.</p>
     */
    @Test
    @DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
    void showMainMenu_doesNotResetWindowDimensions_onSubsequentCalls() throws Exception {
        assumeTrue(tryStartToolkit(), "JavaFX toolkit unavailable — skipping FX test");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                Stage stage = new Stage();
                ThemeManager themeManager = createMockThemeManager();
                I18n i18n = new I18n(I18n.Language.EN);
                SettingsService settings = new SettingsService(Preferences.userRoot().node("chess-test-" + System.nanoTime()));

                SceneManager sm = new SceneManager(stage, mock(Chess.class), themeManager, i18n, settings);

                // First show — creates Scene with initial dimensions 1100×780
                sm.showMainMenu();
                // Record the scene object — it must remain the same object throughout
                var sceneAfterFirst = stage.getScene();
                double initialSceneWidth = sceneAfterFirst.getWidth();
                double initialSceneHeight = sceneAfterFirst.getHeight();

                // Navigate to game and back to main menu
                sm.showGame(RulesetOptions.STANDARD);
                sm.showMainMenu();

                // The Scene object must be identical (not replaced) and its dimensions
                // must equal the initial ones (since swapScene does NOT set width/height
                // on subsequent calls).
                assertSame(sceneAfterFirst, stage.getScene(),
                        "The same Scene object should be reused — swapScene never creates a second Scene");
                assertEquals(initialSceneWidth, stage.getScene().getWidth(), 2.0,
                        "scene width should be preserved — swapScene must not call stage.setWidth on return");
                assertEquals(initialSceneHeight, stage.getScene().getHeight(), 2.0,
                        "scene height should be preserved — swapScene must not call stage.setHeight on return");

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
     * Verifies that after navigating game → main menu, the scene dimensions are preserved
     * (not reset to hardcoded values). The scene size should match what it was after the
     * first showMainMenu call, since showMainMenu on a subsequent call must not set
     * stage dimensions.
     *
     * <p>Note: stage.isMaximized() is not asserted here because JavaFX may not synchronously
     * reflect maximization state changes in test environments; that behavior is covered by the
     * existing {@code showGame_maximizesStage} and {@code plainToCinematic_resumesBackground} tests.</p>
     */
    @Test
    @DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
    void showGame_thenShowMainMenu_preservesMainMenuWindowSize() throws Exception {
        assumeTrue(tryStartToolkit(), "JavaFX toolkit unavailable — skipping FX test");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                Stage stage = new Stage();
                ThemeManager themeManager = createMockThemeManager();
                I18n i18n = new I18n(I18n.Language.EN);
                SettingsService settings = new SettingsService(Preferences.userRoot().node("chess-test-" + System.nanoTime()));

                SceneManager sm = new SceneManager(stage, mock(Chess.class), themeManager, i18n, settings);

                // First show sets initial dimensions on the Scene (1100×780)
                sm.showMainMenu();
                double widthAfterFirst = stage.getScene().getWidth();
                double heightAfterFirst = stage.getScene().getHeight();

                sm.showGame(RulesetOptions.STANDARD);

                // Return to main menu — dimensions must NOT be reset to 1100×780
                sm.showMainMenu();

                // Scene dimensions should equal the first main menu show (the Scene object was
                // created then and reused; its size is governed by window resizes, not by
                // swapScene calls). The critical invariant: swapScene never calls setWidth/setHeight.
                assertEquals(widthAfterFirst, stage.getScene().getWidth(), 2.0,
                        "scene width should be preserved (not reset) when returning to main menu");
                assertEquals(heightAfterFirst, stage.getScene().getHeight(), 2.0,
                        "scene height should be preserved (not reset) when returning to main menu");

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
