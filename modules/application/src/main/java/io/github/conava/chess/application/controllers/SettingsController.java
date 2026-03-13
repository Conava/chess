package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.menu.ResponsiveMenuLayout;
import io.github.conava.chess.application.navigation.PanelHost;
import io.github.conava.chess.application.navigation.SceneManager;
import io.github.conava.chess.application.settings.SettingsService;
import io.github.conava.chess.application.theme.Theme;
import io.github.conava.chess.application.theme.ThemeManager;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Controller for the settings panel.
 *
 * <p>The settings panel uses a cinematic frosted-glass layout hosted inside the
 * main menu shell's panel container. The panel is rendered on top of the shared
 * cinematic background. Save and cancel navigation are delegated to
 * {@link PanelHost} so that the main menu shell can animate the panel out and
 * restore its clean state.</p>
 *
 * <h2>Responsive scaling</h2>
 * <p>Font sizes and panel spacing are bound to the stage width via
 * {@link ResponsiveMenuLayout} in {@link #initialize()}. A {@link SimpleDoubleProperty}
 * is used as the binding root so bindings are active immediately. Theme swatch
 * {@link ToggleButton}s are explicitly excluded from font-size binding because
 * their {@code styleProperty} already carries background-color gradient definitions
 * that must be preserved; their text scales acceptably at the CSS-defined size.
 * Binding references are stored as instance fields to prevent garbage collection.</p>
 */
public class SettingsController {

    /** Default stage width used as initial value for responsive bindings before the stage is known. */
    private static final double DEFAULT_STAGE_WIDTH = 1280.0;

    private final SceneManager sceneManager;
    private final ThemeManager themeManager;
    private final I18n i18n;
    private final SettingsService settingsService;
    private final PanelHost panelHost;

    @FXML
    private StackPane rootPane;
    @FXML
    private VBox settingsPanel;
    @FXML
    private ToggleButton midnightSwatch;
    @FXML
    private ToggleButton emberSwatch;
    @FXML
    private ToggleButton abyssSwatch;
    @FXML
    private ToggleButton manuscriptSwatch;
    @FXML
    private ToggleButton fjordSwatch;
    @FXML
    private ToggleButton sakuraSwatch;
    @FXML
    private ToggleGroup themeGroup;
    @FXML
    private ToggleButton enToggle;
    @FXML
    private ToggleButton deToggle;
    @FXML
    private ToggleGroup langGroup;
    @FXML
    private TextField whiteNameField;
    @FXML
    private TextField blackNameField;

    // ── Responsive binding infrastructure ───────────────────────────────────

    /**
     * Mutable width source for all responsive bindings. Initialized to a sensible default
     * and updated to the actual stage width once the panel is in the scene graph.
     */
    private final DoubleProperty stageWidthSource = new SimpleDoubleProperty(DEFAULT_STAGE_WIDTH);

    /** Dialog title font-size binding — stored to prevent GC. */
    @SuppressWarnings("FieldCanBeLocal")
    private DoubleBinding titleSizeBinding;

    /** Section heading font-size binding — stored to prevent GC. */
    @SuppressWarnings("FieldCanBeLocal")
    private DoubleBinding headingSizeBinding;

    /** Body / label font-size binding — stored to prevent GC. */
    @SuppressWarnings("FieldCanBeLocal")
    private DoubleBinding bodySizeBinding;

    /** Button font-size binding — stored to prevent GC. */
    @SuppressWarnings("FieldCanBeLocal")
    private DoubleBinding btnSizeBinding;

    /** Panel spacing binding — stored to prevent GC. */
    @SuppressWarnings("FieldCanBeLocal")
    private DoubleBinding panelSpaceBinding;

    /** Text field padding binding — stored to prevent GC. */
    @SuppressWarnings("FieldCanBeLocal")
    private DoubleBinding fieldPadBinding;

    /**
     * Constructs a {@code SettingsController}.
     *
     * @param sceneManager    the navigation manager (kept for potential future use).
     * @param themeManager    the theme manager used to read and change the active theme.
     * @param i18n            the internationalisation service for language selection.
     * @param settingsService the settings service used to persist and load preferences.
     * @param panelHost       the panel host used to close this panel when save or cancel is pressed.
     */
    public SettingsController(SceneManager sceneManager, ThemeManager themeManager, I18n i18n,
                              SettingsService settingsService, PanelHost panelHost) {
        this.sceneManager = sceneManager;
        this.themeManager = themeManager;
        this.i18n = i18n;
        this.settingsService = settingsService;
        this.panelHost = panelHost;
    }

    /**
     * Initialises the panel after FXML injection.
     *
     * <p>Sets the current theme and language selections, populates player name fields,
     * attaches listeners for live preview of theme and language changes, wires responsive
     * bindings to {@link #stageWidthSource}, and attaches a scene listener to sync
     * {@code stageWidthSource} with the real stage width once available.</p>
     */
    @FXML
    public void initialize() {
        switch (themeManager.getTheme()) {
            case DARK_PURPLE -> midnightSwatch.setSelected(true);
            case DARK_CHARCOAL -> emberSwatch.setSelected(true);
            case DARK_ABYSS -> abyssSwatch.setSelected(true);
            case LIGHT_PAPER -> manuscriptSwatch.setSelected(true);
            case LIGHT_ARCTIC -> fjordSwatch.setSelected(true);
            case LIGHT_SAKURA -> sakuraSwatch.setSelected(true);
        }

        if (i18n.getLanguage() == I18n.Language.EN) enToggle.setSelected(true);
        else deToggle.setSelected(true);

        whiteNameField.setText(settingsService.loadPlayerWhite());
        blackNameField.setText(settingsService.loadPlayerBlack());

        themeGroup.selectedToggleProperty().addListener((o, old, sel) -> {
            if (sel == midnightSwatch) themeManager.setTheme(Theme.DARK_PURPLE);
            else if (sel == emberSwatch) themeManager.setTheme(Theme.DARK_CHARCOAL);
            else if (sel == abyssSwatch) themeManager.setTheme(Theme.DARK_ABYSS);
            else if (sel == manuscriptSwatch) themeManager.setTheme(Theme.LIGHT_PAPER);
            else if (sel == fjordSwatch) themeManager.setTheme(Theme.LIGHT_ARCTIC);
            else if (sel == sakuraSwatch) themeManager.setTheme(Theme.LIGHT_SAKURA);
        });

        langGroup.selectedToggleProperty().addListener((o, old, sel) -> {
            if (sel == enToggle) i18n.setLanguage(I18n.Language.EN);
            else if (sel == deToggle) i18n.setLanguage(I18n.Language.DE);
        });

        // Wire all bindings to stageWidthSource (immediately active at DEFAULT_STAGE_WIDTH).
        wireBindings();

        // Sync stageWidthSource to real stage width once the panel enters the scene graph.
        // A window listener is chained inside the scene listener to handle the case where
        // the scene exists but its window is not yet attached (scene created but not shown).
        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                if (newScene.getWindow() != null) {
                    stageWidthSource.bind(newScene.getWindow().widthProperty());
                } else {
                    newScene.windowProperty().addListener((wObs, oldW, newW) -> {
                        if (newW != null) {
                            stageWidthSource.bind(newW.widthProperty());
                        }
                    });
                }
            }
        });
        if (rootPane.getScene() != null) {
            if (rootPane.getScene().getWindow() != null) {
                stageWidthSource.bind(rootPane.getScene().getWindow().widthProperty());
            } else {
                rootPane.getScene().windowProperty().addListener((wObs, oldW, newW) -> {
                    if (newW != null) {
                        stageWidthSource.bind(newW.widthProperty());
                    }
                });
            }
        }
    }

    /**
     * Creates all responsive bindings from {@link #stageWidthSource} and applies
     * them to UI elements. Called once from {@link #initialize()}.
     *
     * <p>Theme swatch {@link ToggleButton}s are explicitly skipped because their
     * {@code styleProperty} carries gradient background-color definitions that must
     * not be replaced. They remain at the CSS-defined font size.</p>
     */
    private void wireBindings() {
        titleSizeBinding  = ResponsiveMenuLayout.panelDialogTitleFontSize(stageWidthSource);
        headingSizeBinding = ResponsiveMenuLayout.panelSectionHeadingFontSize(stageWidthSource);
        bodySizeBinding   = ResponsiveMenuLayout.panelBodyFontSize(stageWidthSource);
        btnSizeBinding    = ResponsiveMenuLayout.panelButtonFontSize(stageWidthSource);
        panelSpaceBinding = ResponsiveMenuLayout.panelSpacing(stageWidthSource);
        fieldPadBinding   = ResponsiveMenuLayout.panelFieldPadding(stageWidthSource);

        // ── Dialog title ──────────────────────────────────────────────────
        Label dialogTitle = (Label) rootPane.lookup(".dialog-title");
        if (dialogTitle != null) {
            dialogTitle.styleProperty().bind(
                    Bindings.concat("-fx-font-size: ", titleSizeBinding, "px; -fx-font-weight: bold;"));
        }

        // ── Section headings ──────────────────────────────────────────────
        for (var node : rootPane.lookupAll(".section-heading")) {
            if (node instanceof Label heading) {
                heading.styleProperty().bind(
                        Bindings.concat("-fx-font-size: ", headingSizeBinding,
                                "px; -fx-font-weight: bold;"));
            }
        }

        // ── Action buttons (btn-primary, btn-ghost) ───────────────────────
        for (var node : rootPane.lookupAll(".btn-primary")) {
            if (node instanceof Button btn) {
                btn.styleProperty().bind(
                        Bindings.concat("-fx-font-size: ", btnSizeBinding, "px;"));
            }
        }
        for (var node : rootPane.lookupAll(".btn-ghost")) {
            if (node instanceof Button btn) {
                btn.styleProperty().bind(
                        Bindings.concat("-fx-font-size: ", btnSizeBinding, "px;"));
            }
        }

        // ── Language toggle buttons (enToggle, deToggle) ──────────────────
        // These are plain ToggleButtons with no special inline styles — safe to bind.
        if (enToggle != null) {
            enToggle.styleProperty().bind(
                    Bindings.concat("-fx-font-size: ", btnSizeBinding, "px;"));
        }
        if (deToggle != null) {
            deToggle.styleProperty().bind(
                    Bindings.concat("-fx-font-size: ", btnSizeBinding, "px;"));
        }

        // ── Theme swatch buttons: explicitly skipped ──────────────────────
        // midnightSwatch, emberSwatch, abyssSwatch, manuscriptSwatch, fjordSwatch, sakuraSwatch
        // all carry gradient background-color inline styles. Binding their styleProperty
        // would erase the background-color definition. They remain at CSS-defined size.

        // ── Text fields ───────────────────────────────────────────────────
        if (whiteNameField != null) {
            whiteNameField.styleProperty().bind(
                    Bindings.concat("-fx-font-size: ", bodySizeBinding, "px;",
                            " -fx-padding: ", fieldPadBinding, "px;"));
        }
        if (blackNameField != null) {
            blackNameField.styleProperty().bind(
                    Bindings.concat("-fx-font-size: ", bodySizeBinding, "px;",
                            " -fx-padding: ", fieldPadBinding, "px;"));
        }

        // ── Panel spacing ─────────────────────────────────────────────────
        // Bind the outer settingsPanel VBox spacing.
        settingsPanel.spacingProperty().bind(panelSpaceBinding);
    }

    /**
     * Saves all settings and closes the panel.
     *
     * <p>Settings are persisted before the panel is closed so data is never lost.
     * {@link PanelHost#closePanel()} handles the exit animation.</p>
     */
    @FXML
    private void onSave() {
        settingsService.saveTheme(themeManager.getTheme());
        settingsService.saveLanguage(i18n.getLanguage());
        settingsService.savePlayerWhite(whiteNameField.getText().trim());
        settingsService.savePlayerBlack(blackNameField.getText().trim());
        panelHost.closePanel();
    }

    /**
     * Reverts unsaved theme and language changes, then closes the panel.
     *
     * <p>{@link PanelHost#closePanel()} handles the exit animation and returns
     * to the clean main menu state.</p>
     */
    @FXML
    private void onCancel() {
        themeManager.setTheme(settingsService.loadTheme());
        i18n.setLanguage(settingsService.loadLanguage());
        panelHost.closePanel();
    }
}
