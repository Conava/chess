package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.Chess;
import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.navigation.PanelHost;
import io.github.conava.chess.application.navigation.SceneManager;
import io.github.conava.chess.application.settings.SettingsService;
import io.github.conava.chess.application.theme.Theme;
import io.github.conava.chess.application.theme.ThemeManager;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
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
 * Tests that verify responsive bindings are applied in all sub-panel controllers.
 *
 * <p>FXML structure tests (no toolkit) verify that hardcoded spacing/padding attributes
 * have been removed. FX-thread tests verify that after FXML load the panel VBoxes have
 * their spacing property bound and key labels have non-null style bindings.</p>
 *
 * <p>All FX-thread tests are disabled when the JVM is headless, since the JavaFX
 * toolkit is unavailable in that environment.</p>
 */
class SubPanelResponsiveBindingsTest {

    // ── Toolkit helper ───────────────────────────────────────────────────────

    /**
     * Starts the JavaFX toolkit if not already running.
     *
     * @return {@code true} if the toolkit is available, {@code false} otherwise
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

    // ── FXML string helpers ──────────────────────────────────────────────────

    private String loadFxmlAsString(String path) throws Exception {
        try (var is = getClass().getResourceAsStream(path)) {
            assertNotNull(is, path + " should be loadable as a resource");
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // ── FXML structure tests: offline-setup.fxml ─────────────────────────────

    /**
     * Verifies that the main form panel VBox in offline-setup.fxml does not have
     * a hardcoded {@code spacing} attribute — spacing is now managed by Java bindings.
     */
    @Test
    void offlineSetup_fxml_noHardcodedSpacingOnSetupPanel() throws Exception {
        String fxml = loadFxmlAsString("/fxml/offline-setup.fxml");
        // The setupPanel VBox should not have spacing="12" or any hardcoded spacing
        // It may have a VBox with fx:id="setupPanel" — check that fx:id line has no spacing
        int setupPanelIdx = fxml.indexOf("fx:id=\"setupPanel\"");
        assertTrue(setupPanelIdx > 0, "setupPanel VBox should exist in offline-setup.fxml");
        // Extract the opening tag of the setupPanel VBox
        int tagStart = fxml.lastIndexOf("<VBox", setupPanelIdx);
        int tagEnd = fxml.indexOf(">", setupPanelIdx);
        String tag = fxml.substring(tagStart, tagEnd + 1);
        assertFalse(tag.contains("spacing="),
                "setupPanel VBox should not have a hardcoded spacing= attribute (now bound in Java)");
    }

    /**
     * Verifies that the main form panel VBox in offline-setup.fxml does not have
     * a hardcoded {@code <padding>} child element — padding is now managed by Java bindings.
     */
    @Test
    void offlineSetup_fxml_noHardcodedPaddingElement() throws Exception {
        String fxml = loadFxmlAsString("/fxml/offline-setup.fxml");
        // Locate the setupPanel VBox block and confirm it has no <padding> child
        int setupPanelIdx = fxml.indexOf("fx:id=\"setupPanel\"");
        assertTrue(setupPanelIdx > 0, "setupPanel VBox should exist in offline-setup.fxml");
        // Check that there is no <padding> element on the setupPanel
        // (there may be valid <StackPane.margin> which is different)
        // Search between setupPanel opening and the closing </VBox> for the first </VBox>
        int closingIdx = fxml.indexOf("</VBox>", setupPanelIdx);
        String panelContent = fxml.substring(setupPanelIdx, closingIdx);
        assertFalse(panelContent.contains("<padding>"),
                "setupPanel VBox should not contain a <padding> child element (now bound in Java)");
    }

    // ── FXML structure tests: online-setup.fxml ──────────────────────────────

    /**
     * Verifies that the main form panel VBox in online-setup.fxml does not have
     * a hardcoded {@code spacing} attribute.
     */
    @Test
    void onlineSetup_fxml_noHardcodedSpacingOnSetupPanel() throws Exception {
        String fxml = loadFxmlAsString("/fxml/online-setup.fxml");
        int setupPanelIdx = fxml.indexOf("fx:id=\"setupPanel\"");
        assertTrue(setupPanelIdx > 0, "setupPanel VBox should exist in online-setup.fxml");
        int tagStart = fxml.lastIndexOf("<VBox", setupPanelIdx);
        int tagEnd = fxml.indexOf(">", setupPanelIdx);
        String tag = fxml.substring(tagStart, tagEnd + 1);
        assertFalse(tag.contains("spacing="),
                "setupPanel VBox should not have a hardcoded spacing= attribute (now bound in Java)");
    }

    /**
     * Verifies that the main form panel VBox in online-setup.fxml does not have
     * a hardcoded {@code <padding>} child element.
     */
    @Test
    void onlineSetup_fxml_noHardcodedPaddingElement() throws Exception {
        String fxml = loadFxmlAsString("/fxml/online-setup.fxml");
        int setupPanelIdx = fxml.indexOf("fx:id=\"setupPanel\"");
        assertTrue(setupPanelIdx > 0, "setupPanel VBox should exist in online-setup.fxml");
        int closingIdx = fxml.indexOf("</VBox>", setupPanelIdx);
        String panelContent = fxml.substring(setupPanelIdx, closingIdx);
        assertFalse(panelContent.contains("<padding>"),
                "setupPanel VBox should not contain a <padding> child element (now bound in Java)");
    }

    // ── FXML structure tests: settings.fxml ──────────────────────────────────

    /**
     * Verifies that the main form panel VBox in settings.fxml does not have
     * a hardcoded {@code spacing} attribute.
     */
    @Test
    void settings_fxml_noHardcodedSpacingOnSettingsPanel() throws Exception {
        String fxml = loadFxmlAsString("/fxml/settings.fxml");
        int settingsPanelIdx = fxml.indexOf("fx:id=\"settingsPanel\"");
        assertTrue(settingsPanelIdx > 0, "settingsPanel VBox should exist in settings.fxml");
        int tagStart = fxml.lastIndexOf("<VBox", settingsPanelIdx);
        int tagEnd = fxml.indexOf(">", settingsPanelIdx);
        String tag = fxml.substring(tagStart, tagEnd + 1);
        assertFalse(tag.contains("spacing="),
                "settingsPanel VBox should not have a hardcoded spacing= attribute (now bound in Java)");
    }

    // ── FXML structure tests: login.fxml ─────────────────────────────────────

    /**
     * Verifies that the main form panel VBox in login.fxml does not have
     * a hardcoded {@code spacing} attribute.
     */
    @Test
    void login_fxml_noHardcodedSpacingOnLoginPanel() throws Exception {
        String fxml = loadFxmlAsString("/fxml/login.fxml");
        int loginPanelIdx = fxml.indexOf("fx:id=\"loginPanel\"");
        assertTrue(loginPanelIdx > 0, "loginPanel VBox should exist in login.fxml");
        int tagStart = fxml.lastIndexOf("<VBox", loginPanelIdx);
        int tagEnd = fxml.indexOf(">", loginPanelIdx);
        String tag = fxml.substring(tagStart, tagEnd + 1);
        assertFalse(tag.contains("spacing="),
                "loginPanel VBox should not have a hardcoded spacing= attribute (now bound in Java)");
    }

    /**
     * Verifies that the main form panel VBox in login.fxml does not have
     * a hardcoded {@code <padding>} child element.
     */
    @Test
    void login_fxml_noHardcodedPaddingElement() throws Exception {
        String fxml = loadFxmlAsString("/fxml/login.fxml");
        int loginPanelIdx = fxml.indexOf("fx:id=\"loginPanel\"");
        assertTrue(loginPanelIdx > 0, "loginPanel VBox should exist in login.fxml");
        int closingIdx = fxml.indexOf("</VBox>", loginPanelIdx);
        String panelContent = fxml.substring(loginPanelIdx, closingIdx);
        assertFalse(panelContent.contains("<padding>"),
                "loginPanel VBox should not contain a <padding> child element (now bound in Java)");
    }

    // ── FXML structure tests: register.fxml ──────────────────────────────────

    /**
     * Verifies that the main form panel VBox in register.fxml does not have
     * a hardcoded {@code spacing} attribute.
     */
    @Test
    void register_fxml_noHardcodedSpacingOnRegisterPanel() throws Exception {
        String fxml = loadFxmlAsString("/fxml/register.fxml");
        int registerPanelIdx = fxml.indexOf("fx:id=\"registerPanel\"");
        assertTrue(registerPanelIdx > 0, "registerPanel VBox should exist in register.fxml");
        int tagStart = fxml.lastIndexOf("<VBox", registerPanelIdx);
        int tagEnd = fxml.indexOf(">", registerPanelIdx);
        String tag = fxml.substring(tagStart, tagEnd + 1);
        assertFalse(tag.contains("spacing="),
                "registerPanel VBox should not have a hardcoded spacing= attribute (now bound in Java)");
    }

    /**
     * Verifies that the main form panel VBox in register.fxml does not have
     * a hardcoded {@code <padding>} child element.
     */
    @Test
    void register_fxml_noHardcodedPaddingElement() throws Exception {
        String fxml = loadFxmlAsString("/fxml/register.fxml");
        int registerPanelIdx = fxml.indexOf("fx:id=\"registerPanel\"");
        assertTrue(registerPanelIdx > 0, "registerPanel VBox should exist in register.fxml");
        int closingIdx = fxml.indexOf("</VBox>", registerPanelIdx);
        String panelContent = fxml.substring(registerPanelIdx, closingIdx);
        assertFalse(panelContent.contains("<padding>"),
                "registerPanel VBox should not contain a <padding> child element (now bound in Java)");
    }

    // ── FXML structure tests: waiting-for-match.fxml ─────────────────────────

    /**
     * Verifies that the main form panel VBox in waiting-for-match.fxml does not have
     * a hardcoded {@code spacing} attribute.
     */
    @Test
    void waitingForMatch_fxml_noHardcodedSpacingOnWaitingPanel() throws Exception {
        String fxml = loadFxmlAsString("/fxml/waiting-for-match.fxml");
        int waitingPanelIdx = fxml.indexOf("fx:id=\"waitingPanel\"");
        assertTrue(waitingPanelIdx > 0, "waitingPanel VBox should exist in waiting-for-match.fxml");
        int tagStart = fxml.lastIndexOf("<VBox", waitingPanelIdx);
        int tagEnd = fxml.indexOf(">", waitingPanelIdx);
        String tag = fxml.substring(tagStart, tagEnd + 1);
        assertFalse(tag.contains("spacing="),
                "waitingPanel VBox should not have a hardcoded spacing= attribute (now bound in Java)");
    }

    /**
     * Verifies that the main form panel VBox in waiting-for-match.fxml does not have
     * a hardcoded {@code <padding>} child element.
     */
    @Test
    void waitingForMatch_fxml_noHardcodedPaddingElement() throws Exception {
        String fxml = loadFxmlAsString("/fxml/waiting-for-match.fxml");
        int waitingPanelIdx = fxml.indexOf("fx:id=\"waitingPanel\"");
        assertTrue(waitingPanelIdx > 0, "waitingPanel VBox should exist in waiting-for-match.fxml");
        int closingIdx = fxml.indexOf("</VBox>", waitingPanelIdx);
        String panelContent = fxml.substring(waitingPanelIdx, closingIdx);
        assertFalse(panelContent.contains("<padding>"),
                "waitingPanel VBox should not contain a <padding> child element (now bound in Java)");
    }

    /**
     * Verifies that rulesetLabel in waiting-for-match.fxml does not have an inline
     * {@code style="-fx-font-size: 14px;"} attribute — that was removed and is now
     * bound via Java bindings.
     */
    @Test
    void waitingForMatch_rulesetLabel_noInlineFontSizeStyle() throws Exception {
        String fxml = loadFxmlAsString("/fxml/waiting-for-match.fxml");
        int rulesetLabelIdx = fxml.indexOf("fx:id=\"rulesetLabel\"");
        assertTrue(rulesetLabelIdx > 0, "rulesetLabel should exist in waiting-for-match.fxml");
        // Extract the rulesetLabel tag line
        int tagStart = fxml.lastIndexOf("<Label", rulesetLabelIdx);
        int tagEnd = fxml.indexOf("/>", rulesetLabelIdx);
        String tag = fxml.substring(tagStart, tagEnd + 2);
        assertFalse(tag.contains("-fx-font-size"),
                "rulesetLabel should not have a hardcoded -fx-font-size in its style attribute "
                        + "(now bound in Java). Tag: " + tag);
    }

    // ── FX-thread tests: OfflineSetupController ──────────────────────────────

    /**
     * Verifies that after FXML load, {@code OfflineSetupController} has bound the
     * {@code setupPanel} spacing property and the dialog title label has a style binding.
     */
    @Test
    @DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
    void OfflineSetup_initialize_appliesResponsiveBindings() throws Exception {
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

                OfflineSetupController controller =
                        new OfflineSetupController(sm, i18n, settings, panelHost);

                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/offline-setup.fxml"), i18n.getBundle());
                loader.setControllerFactory(type -> controller);
                Parent root = loader.load();
                assertNotNull(root, "FXML should load successfully");

                // setupPanel spacing should be bound (bound means its spacingProperty has a binding)
                VBox setupPanel = (VBox) root.lookup("#setupPanel");
                assertNotNull(setupPanel, "setupPanel should exist");
                assertTrue(setupPanel.spacingProperty().isBound(),
                        "setupPanel spacing should be bound to a responsive binding");

                // Dialog title label should have an inline style containing -fx-font-size
                Label dialogTitle = (Label) root.lookup(".dialog-title");
                assertNotNull(dialogTitle, "dialog-title label should exist");
                assertNotNull(dialogTitle.getStyle(),
                        "dialog title should have a style string after binding");
                assertFalse(dialogTitle.getStyle().isBlank(),
                        "dialog title style should not be blank (responsive binding should set it)");
                assertTrue(dialogTitle.getStyle().contains("-fx-font-size"),
                        "dialog title style should contain -fx-font-size from responsive binding. "
                                + "Actual style: " + dialogTitle.getStyle());

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

    // ── FX-thread tests: OnlineSetupController ───────────────────────────────

    /**
     * Verifies that after FXML load, {@code OnlineSetupController} has bound the
     * {@code setupPanel} spacing property.
     */
    @Test
    @DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
    void OnlineSetup_initialize_appliesResponsiveBindings() throws Exception {
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

                VBox setupPanel = (VBox) root.lookup("#setupPanel");
                assertNotNull(setupPanel, "setupPanel should exist");
                assertTrue(setupPanel.spacingProperty().isBound(),
                        "setupPanel spacing should be bound to a responsive binding");

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

    // ── FX-thread tests: SettingsController ──────────────────────────────────

    /**
     * Verifies that after FXML load, {@code SettingsController} has bound the
     * {@code settingsPanel} spacing property.
     */
    @Test
    @DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
    void Settings_initialize_appliesResponsiveBindings() throws Exception {
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

                SettingsController controller =
                        new SettingsController(sm, themeManager, i18n, settings, panelHost);

                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/settings.fxml"), i18n.getBundle());
                loader.setControllerFactory(type -> controller);
                Parent root = loader.load();
                assertNotNull(root, "FXML should load successfully");

                // The settingsPanel is the outer VBox; its spacing should be bound
                VBox settingsPanel = (VBox) root.lookup("#settingsPanel");
                assertNotNull(settingsPanel, "settingsPanel should exist");
                assertTrue(settingsPanel.spacingProperty().isBound(),
                        "settingsPanel spacing should be bound to a responsive binding");

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

    // ── FX-thread tests: LoginController ─────────────────────────────────────

    /**
     * Verifies that after FXML load, {@code LoginController} has bound the
     * {@code loginPanel} spacing property.
     */
    @Test
    @DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
    void Login_initialize_appliesResponsiveBindings() throws Exception {
        assumeTrue(tryStartToolkit(), "JavaFX toolkit unavailable -- skipping FX test");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                I18n i18n = new I18n(I18n.Language.EN);
                Chess chess = mock(Chess.class);
                SceneManager sm = mock(SceneManager.class);
                PanelHost panelHost = mock(PanelHost.class);

                LoginController controller =
                        new LoginController(sm, chess, i18n, panelHost);

                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/login.fxml"), i18n.getBundle());
                loader.setControllerFactory(type -> controller);
                Parent root = loader.load();
                assertNotNull(root, "FXML should load successfully");

                VBox loginPanel = (VBox) root.lookup("#loginPanel");
                assertNotNull(loginPanel, "loginPanel should exist");
                assertTrue(loginPanel.spacingProperty().isBound(),
                        "loginPanel spacing should be bound to a responsive binding");

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

    // ── FX-thread tests: RegisterController ──────────────────────────────────

    /**
     * Verifies that after FXML load, {@code RegisterController} has bound the
     * {@code registerPanel} spacing property.
     */
    @Test
    @DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
    void Register_initialize_appliesResponsiveBindings() throws Exception {
        assumeTrue(tryStartToolkit(), "JavaFX toolkit unavailable -- skipping FX test");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                I18n i18n = new I18n(I18n.Language.EN);
                Chess chess = mock(Chess.class);
                SceneManager sm = mock(SceneManager.class);
                PanelHost panelHost = mock(PanelHost.class);

                RegisterController controller =
                        new RegisterController(sm, chess, i18n, panelHost);

                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/register.fxml"), i18n.getBundle());
                loader.setControllerFactory(type -> controller);
                Parent root = loader.load();
                assertNotNull(root, "FXML should load successfully");

                VBox registerPanel = (VBox) root.lookup("#registerPanel");
                assertNotNull(registerPanel, "registerPanel should exist");
                assertTrue(registerPanel.spacingProperty().isBound(),
                        "registerPanel spacing should be bound to a responsive binding");

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

    // ── FX-thread tests: WaitingForMatchController ───────────────────────────

    /**
     * Verifies that after FXML load, {@code WaitingForMatchController} has bound the
     * {@code waitingPanel} spacing property.
     */
    @Test
    @DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
    void WaitingForMatch_initialize_appliesResponsiveBindings() throws Exception {
        assumeTrue(tryStartToolkit(), "JavaFX toolkit unavailable -- skipping FX test");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                I18n i18n = new I18n(I18n.Language.EN);
                Chess chess = mock(Chess.class);
                SceneManager sm = mock(SceneManager.class);
                PanelHost panelHost = mock(PanelHost.class);
                when(chess.getActiveServerTask()).thenReturn(null);

                WaitingForMatchController controller =
                        new WaitingForMatchController(sm, chess, i18n,
                                RulesetOptions.STANDARD, "localhost", 54321, panelHost);

                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/waiting-for-match.fxml"), i18n.getBundle());
                loader.setControllerFactory(type -> controller);
                Parent root = loader.load();
                assertNotNull(root, "FXML should load successfully");

                VBox waitingPanel = (VBox) root.lookup("#waitingPanel");
                assertNotNull(waitingPanel, "waitingPanel should exist");
                assertTrue(waitingPanel.spacingProperty().isBound(),
                        "waitingPanel spacing should be bound to a responsive binding");

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
     * Verifies that after FXML load, the {@code rulesetLabel} in
     * {@code WaitingForMatchController} has a style binding for font size
     * (i.e., not a hardcoded inline style from FXML).
     */
    @Test
    @DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
    void WaitingForMatch_rulesetLabel_hasStyleBinding() throws Exception {
        assumeTrue(tryStartToolkit(), "JavaFX toolkit unavailable -- skipping FX test");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                I18n i18n = new I18n(I18n.Language.EN);
                Chess chess = mock(Chess.class);
                SceneManager sm = mock(SceneManager.class);
                PanelHost panelHost = mock(PanelHost.class);
                when(chess.getActiveServerTask()).thenReturn(null);

                WaitingForMatchController controller =
                        new WaitingForMatchController(sm, chess, i18n,
                                RulesetOptions.STANDARD, "localhost", 54321, panelHost);

                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/waiting-for-match.fxml"), i18n.getBundle());
                loader.setControllerFactory(type -> controller);
                Parent root = loader.load();
                assertNotNull(root, "FXML should load successfully");

                Label rulesetLabel = (Label) root.lookup("#rulesetLabel");
                assertNotNull(rulesetLabel, "rulesetLabel should exist");
                // The label's style should come from the binding, not from the FXML inline style
                // A bound style is set by the binding at binding time; it should contain -fx-font-size
                // OR the style property itself should be bound (which is verified by isBound() after attach)
                assertTrue(rulesetLabel.styleProperty().isBound()
                                || (rulesetLabel.getStyle() != null
                                && rulesetLabel.getStyle().contains("-fx-font-size")),
                        "rulesetLabel should have a responsive style binding or a style with -fx-font-size. "
                                + "Actual style: " + rulesetLabel.getStyle());

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
