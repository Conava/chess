# JavaFX Migration Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace the entire Swing `application` module with a JavaFX 21 UI that supports on-the-fly light/dark theming, board color presets, EN/DE language switching, and player name persistence via Java Preferences API.

**Architecture:** Single-Stage FXML navigation — one `Stage` persists; screens are FXML + Controller pairs swapped via `SceneManager`. CSS stylesheet swapping drives theming. `Chess.java` extends `javafx.application.Application` and remains the game facade. `core` and `server` are untouched.

**Tech Stack:** Java 17, JavaFX 21 (openjfx), FXML, JUnit 5, Maven.

---

## Branch

```bash
git checkout -b feat/javafx-migration
```

---

## Affected Files

| Task | Files Created | Files Modified | Cascade Risk |
|------|--------------|----------------|--------------|
| 1 | — | `modules/application/pom.xml` | NONE |
| 2 | `Theme.java`, `BoardTheme.java`, `ThemeManager.java`, `ThemeManagerTest.java` | — | NONE |
| 3 | `I18n.java`, `messages_en.properties`, `messages_de.properties`, `I18nTest.java` | — | NONE |
| 4 | `SettingsService.java`, `SettingsServiceTest.java` | — | NONE |
| 5 | `SceneManager.java` | — | DEPENDENT on 2,3,4 |
| 6 | `base.css`, `dark.css`, `light.css`, `classic.css`, `ocean.css`, `walnut.css` | — | NONE |
| 7 | `Chess.java` (rewrite) | — | DEPENDENT on 5 |
| 8 | `main-menu.fxml`, `MainMenuController.java` | — | DEPENDENT on 7 |
| 9 | `settings.fxml`, `SettingsController.java` | — | DEPENDENT on 7 |
| 10 | `offline-setup.fxml`, `OfflineSetupController.java` | — | DEPENDENT on 7 |
| 11 | `online-setup.fxml`, `OnlineSetupController.java` | — | DEPENDENT on 7 |
| 12 | `promotion.fxml`, `PromotionController.java` | — | DEPENDENT on 7 |
| 13 | `waiting.fxml`, `WaitingController.java` | — | DEPENDENT on 7 |
| 14 | `ExecuteMove.java` (rewrite) | — | NONE |
| 15 | `game.fxml`, `GameController.java` | — | DEPENDENT on 12,13,14 |
| 16 | `ChessTest.java`, `ExecuteMoveTest.java` | — | DEPENDENT on 15 |
| 17 | delete all old Swing files | `Chess.java` (remove AWT/Swing imports) | DEPENDENT on 16 |

---

## Ordered Implementation Tasks

### Task 1: Update pom.xml with JavaFX dependencies

**Files:**
- Modify: `modules/application/pom.xml`

**Step 1: Add JavaFX dependencies inside `<dependencies>`**

```xml
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-controls</artifactId>
    <version>21</version>
</dependency>
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-fxml</artifactId>
    <version>21</version>
</dependency>
```

**Step 2: Add the JavaFX Maven plugin inside `<plugins>` (after existing plugins)**

```xml
<plugin>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-maven-plugin</artifactId>
    <version>0.0.8</version>
    <configuration>
        <mainClass>io.github.conava.chess.application.Chess</mainClass>
    </configuration>
</plugin>
```

**Step 3: Verify the project compiles (no Java files changed yet)**

```bash
mvn compile -pl modules/application -am -q
```
Expected: BUILD SUCCESS (existing Swing files still compile alongside new deps)

**Step 4: Commit**

```bash
git add modules/application/pom.xml
git commit -m "chore(application): add JavaFX 21 dependencies"
```

---

### Task 2: Theme enums and ThemeManager

**Files:**
- Create: `modules/application/src/main/java/io/github/conava/chess/application/theme/Theme.java`
- Create: `modules/application/src/main/java/io/github/conava/chess/application/theme/BoardTheme.java`
- Create: `modules/application/src/main/java/io/github/conava/chess/application/theme/ThemeManager.java`
- Test: `modules/application/src/test/java/io/github/conava/chess/application/theme/ThemeManagerTest.java`

**Step 1: Write the failing tests**

`ThemeManagerTest.java`:
```java
package io.github.conava.chess.application.theme;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ThemeManagerTest {

    @Test
    void defaultThemeIsDark() {
        ThemeManager tm = new ThemeManager();
        assertEquals(Theme.DARK, tm.getTheme());
    }

    @Test
    void defaultBoardThemeIsClassic() {
        ThemeManager tm = new ThemeManager();
        assertEquals(BoardTheme.CLASSIC, tm.getBoardTheme());
    }

    @Test
    void setThemeUpdatesProperty() {
        ThemeManager tm = new ThemeManager();
        tm.setTheme(Theme.LIGHT);
        assertEquals(Theme.LIGHT, tm.getTheme());
        assertEquals(Theme.LIGHT, tm.currentThemeProperty().get());
    }

    @Test
    void setBoardThemeUpdatesProperty() {
        ThemeManager tm = new ThemeManager();
        tm.setBoardTheme(BoardTheme.OCEAN);
        assertEquals(BoardTheme.OCEAN, tm.getBoardTheme());
        assertEquals(BoardTheme.OCEAN, tm.currentBoardThemeProperty().get());
    }

    @Test
    void themeCssFileReturnsCorrectPath() {
        assertEquals("/css/dark.css", Theme.DARK.cssFile());
        assertEquals("/css/light.css", Theme.LIGHT.cssFile());
    }

    @Test
    void boardThemeCssFileReturnsCorrectPath() {
        assertEquals("/css/board/classic.css", BoardTheme.CLASSIC.cssFile());
        assertEquals("/css/board/ocean.css", BoardTheme.OCEAN.cssFile());
        assertEquals("/css/board/walnut.css", BoardTheme.WALNUT.cssFile());
    }
}
```

**Step 2: Run tests to verify they fail**

```bash
mvn test -pl modules/application -Dtest=ThemeManagerTest -q
```
Expected: FAIL — classes not found.

**Step 3: Implement Theme.java**

```java
package io.github.conava.chess.application.theme;

public enum Theme {
    DARK, LIGHT;

    public String cssFile() {
        return switch (this) {
            case DARK  -> "/css/dark.css";
            case LIGHT -> "/css/light.css";
        };
    }
}
```

**Step 4: Implement BoardTheme.java**

```java
package io.github.conava.chess.application.theme;

public enum BoardTheme {
    CLASSIC, OCEAN, WALNUT;

    public String cssFile() {
        return switch (this) {
            case CLASSIC -> "/css/board/classic.css";
            case OCEAN   -> "/css/board/ocean.css";
            case WALNUT  -> "/css/board/walnut.css";
        };
    }

    public String displayName() {
        return switch (this) {
            case CLASSIC -> "Classic";
            case OCEAN   -> "Ocean";
            case WALNUT  -> "Walnut";
        };
    }
}
```

**Step 5: Implement ThemeManager.java**

```java
package io.github.conava.chess.application.theme;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import java.util.ArrayList;
import java.util.List;

public class ThemeManager {

    private final ObjectProperty<Theme> currentTheme =
            new SimpleObjectProperty<>(Theme.DARK);
    private final ObjectProperty<BoardTheme> currentBoardTheme =
            new SimpleObjectProperty<>(BoardTheme.CLASSIC);
    private final List<Scene> managedScenes = new ArrayList<>();

    public void registerScene(Scene scene) {
        managedScenes.add(scene);
        applyTheme(scene);
    }

    public void unregisterScene(Scene scene) {
        managedScenes.remove(scene);
    }

    public void setTheme(Theme theme) {
        currentTheme.set(theme);
        managedScenes.forEach(this::applyTheme);
    }

    public void setBoardTheme(BoardTheme boardTheme) {
        currentBoardTheme.set(boardTheme);
        managedScenes.forEach(this::applyTheme);
    }

    private void applyTheme(Scene scene) {
        var base      = getClass().getResource("/css/base.css");
        var themeCss  = getClass().getResource(currentTheme.get().cssFile());
        var boardCss  = getClass().getResource(currentBoardTheme.get().cssFile());
        if (base == null || themeCss == null || boardCss == null) return;
        scene.getStylesheets().setAll(
            base.toExternalForm(),
            themeCss.toExternalForm(),
            boardCss.toExternalForm()
        );
    }

    public ObjectProperty<Theme> currentThemeProperty()           { return currentTheme; }
    public ObjectProperty<BoardTheme> currentBoardThemeProperty() { return currentBoardTheme; }
    public Theme getTheme()           { return currentTheme.get(); }
    public BoardTheme getBoardTheme() { return currentBoardTheme.get(); }
}
```

**Step 6: Run tests to verify they pass**

```bash
mvn test -pl modules/application -Dtest=ThemeManagerTest
```
Expected: Tests PASS. (CSS resource loading is skipped because `applyTheme` null-checks resources; the property tests pass without needing a real Scene.)

**Step 7: Commit**

```bash
git add modules/application/src/main/java/io/github/conava/chess/application/theme/ \
        modules/application/src/test/java/io/github/conava/chess/application/theme/
git commit -m "feat(application): add Theme, BoardTheme enums and ThemeManager"
```

---

### Task 3: I18n service and properties files

**Files:**
- Create: `modules/application/src/main/java/io/github/conava/chess/application/i18n/I18n.java`
- Create: `modules/application/src/main/resources/i18n/messages_en.properties`
- Create: `modules/application/src/main/resources/i18n/messages_de.properties`
- Test: `modules/application/src/test/java/io/github/conava/chess/application/i18n/I18nTest.java`

**Step 1: Write the failing tests**

```java
package io.github.conava.chess.application.i18n;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class I18nTest {

    @Test
    void englishKeyReturnsEnglishValue() {
        I18n i18n = new I18n(I18n.Language.EN);
        assertEquals("Local Game", i18n.get("menu.local"));
    }

    @Test
    void germanKeyReturnsGermanValue() {
        I18n i18n = new I18n(I18n.Language.DE);
        assertEquals("Lokales Spiel", i18n.get("menu.local"));
    }

    @Test
    void switchingLanguageUpdatesBundle() {
        I18n i18n = new I18n(I18n.Language.EN);
        i18n.setLanguage(I18n.Language.DE);
        assertEquals("Lokales Spiel", i18n.get("menu.local"));
        assertEquals(I18n.Language.DE, i18n.getLanguage());
    }

    @Test
    void languagePropertyReflectsCurrentLanguage() {
        I18n i18n = new I18n(I18n.Language.EN);
        assertEquals(I18n.Language.EN, i18n.currentLanguageProperty().get());
        i18n.setLanguage(I18n.Language.DE);
        assertEquals(I18n.Language.DE, i18n.currentLanguageProperty().get());
    }
}
```

**Step 2: Run tests to verify they fail**

```bash
mvn test -pl modules/application -Dtest=I18nTest -q
```
Expected: FAIL.

**Step 3: Create messages_en.properties**

Path: `modules/application/src/main/resources/i18n/messages_en.properties`

```properties
menu.title=Chess
menu.local=Local Game
menu.online=Online Game
menu.settings=Settings
menu.exit=Exit

game.leave=Leave Game
game.moves=Moves
game.active=Active
game.waiting=Waiting...

dialog.offline.title=Game Setup
dialog.offline.white=White Player
dialog.offline.black=Black Player
dialog.offline.ruleset=Ruleset
dialog.offline.start=Start Game
dialog.offline.cancel=Cancel

dialog.online.title=Online Game
dialog.online.ip=Server IP
dialog.online.port=Port
dialog.online.joincode=Join Code
dialog.online.create=Create Game
dialog.online.join=Join Game
dialog.online.start=Connect
dialog.online.cancel=Cancel

dialog.confirm.yes=Yes
dialog.confirm.no=No
dialog.confirm.ok=OK

settings.title=Settings
settings.appearance=Appearance
settings.theme=Theme
settings.theme.dark=Dark
settings.theme.light=Light
settings.board=Board Theme
settings.language=Language
settings.players=Player Names
settings.player.white=White Player Name
settings.player.black=Black Player Name
settings.save=Save
settings.cancel=Cancel

promotion.title=Promote Pawn
waiting.title=Waiting for Opponent
waiting.code=Join Code:
waiting.instruction=Share this code with your opponent.
waiting.cancel=Cancel

error.server=Connection to server lost.\nReturn to menu?
error.server.title=Server Error
game.end.return=Return to menu?

state.WHITE_WON_BY_CHECKMATE=White wins by checkmate
state.WHITE_WON_BY_RESIGNATION=White wins by resignation
state.WHITE_WON_BY_TIMEOUT=White wins by timeout
state.BLACK_WON_BY_CHECKMATE=Black wins by checkmate
state.BLACK_WON_BY_RESIGNATION=Black wins by resignation
state.BLACK_WON_BY_TIMEOUT=Black wins by timeout
state.DRAW_BY_STALEMATE=Draw by stalemate
state.DRAW_BY_INSUFFICIENT_MATERIAL=Draw by insufficient material
state.DRAW_BY_THREEFOLD_REPETITION=Draw by threefold repetition
state.DRAW_BY_FIFTY_MOVE_RULE=Draw by fifty-move rule
```

**Step 4: Create messages_de.properties**

Path: `modules/application/src/main/resources/i18n/messages_de.properties`

```properties
menu.title=Schach
menu.local=Lokales Spiel
menu.online=Online Spiel
menu.settings=Einstellungen
menu.exit=Beenden

game.leave=Spiel verlassen
game.moves=Z\u00fcge
game.active=Am Zug
game.waiting=Warten...

dialog.offline.title=Spielaufstellung
dialog.offline.white=Spieler Wei\u00df
dialog.offline.black=Spieler Schwarz
dialog.offline.ruleset=Regelwerk
dialog.offline.start=Spiel starten
dialog.offline.cancel=Abbrechen

dialog.online.title=Online Spiel
dialog.online.ip=Server IP
dialog.online.port=Port
dialog.online.joincode=Beitrittscode
dialog.online.create=Spiel erstellen
dialog.online.join=Spiel beitreten
dialog.online.start=Verbinden
dialog.online.cancel=Abbrechen

dialog.confirm.yes=Ja
dialog.confirm.no=Nein
dialog.confirm.ok=OK

settings.title=Einstellungen
settings.appearance=Erscheinungsbild
settings.theme=Design
settings.theme.dark=Dunkel
settings.theme.light=Hell
settings.board=Brettstil
settings.language=Sprache
settings.players=Spielernamen
settings.player.white=Spieler Wei\u00df
settings.player.black=Spieler Schwarz
settings.save=Speichern
settings.cancel=Abbrechen

promotion.title=Bauernumwandlung
waiting.title=Warte auf Gegner
waiting.code=Beitrittscode:
waiting.instruction=Teile diesen Code mit deinem Gegner.
waiting.cancel=Abbrechen

error.server=Verbindung zum Server verloren.\nZur\u00fcck zum Men\u00fc?
error.server.title=Serverfehler
game.end.return=Zur\u00fcck zum Men\u00fc?

state.WHITE_WON_BY_CHECKMATE=Wei\u00df gewinnt durch Schachmatt
state.WHITE_WON_BY_RESIGNATION=Wei\u00df gewinnt durch Aufgabe
state.WHITE_WON_BY_TIMEOUT=Wei\u00df gewinnt durch Zeitablauf
state.BLACK_WON_BY_CHECKMATE=Schwarz gewinnt durch Schachmatt
state.BLACK_WON_BY_RESIGNATION=Schwarz gewinnt durch Aufgabe
state.BLACK_WON_BY_TIMEOUT=Schwarz gewinnt durch Zeitablauf
state.DRAW_BY_STALEMATE=Remis durch Patt
state.DRAW_BY_INSUFFICIENT_MATERIAL=Remis durch unzureichendes Material
state.DRAW_BY_THREEFOLD_REPETITION=Remis durch dreifache Stellungswiederholung
state.DRAW_BY_FIFTY_MOVE_RULE=Remis durch F\u00fcfzig-Z\u00fcge-Regel
```

**Step 5: Implement I18n.java**

```java
package io.github.conava.chess.application.i18n;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import java.util.Locale;
import java.util.ResourceBundle;

public class I18n {

    public enum Language { EN, DE }

    private final ObjectProperty<Language> currentLanguage =
            new SimpleObjectProperty<>(Language.EN);
    private ResourceBundle bundle;

    public I18n(Language language) {
        setLanguage(language);
    }

    public void setLanguage(Language language) {
        currentLanguage.set(language);
        Locale locale = language == Language.EN ? Locale.ENGLISH : Locale.GERMAN;
        bundle = ResourceBundle.getBundle("i18n/messages", locale);
    }

    public String get(String key) {
        return bundle.getString(key);
    }

    public ResourceBundle getBundle()                            { return bundle; }
    public Language getLanguage()                                { return currentLanguage.get(); }
    public ObjectProperty<Language> currentLanguageProperty()   { return currentLanguage; }
}
```

**Step 6: Run tests to verify they pass**

```bash
mvn test -pl modules/application -Dtest=I18nTest
```
Expected: PASS.

**Step 7: Commit**

```bash
git add modules/application/src/main/java/io/github/conava/chess/application/i18n/ \
        modules/application/src/main/resources/i18n/ \
        modules/application/src/test/java/io/github/conava/chess/application/i18n/
git commit -m "feat(application): add I18n service with EN/DE properties"
```

---

### Task 4: SettingsService

**Files:**
- Create: `modules/application/src/main/java/io/github/conava/chess/application/settings/SettingsService.java`
- Test: `modules/application/src/test/java/io/github/conava/chess/application/settings/SettingsServiceTest.java`

**Step 1: Write the failing tests**

```java
package io.github.conava.chess.application.settings;

import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.theme.BoardTheme;
import io.github.conava.chess.application.theme.Theme;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.prefs.Preferences;
import static org.junit.jupiter.api.Assertions.*;

class SettingsServiceTest {

    private SettingsService service;

    @BeforeEach
    void setUp() {
        // Use an isolated test node so real prefs are not affected
        Preferences testPrefs = Preferences.userRoot().node("chess-test-" + System.nanoTime());
        service = new SettingsService(testPrefs);
    }

    @Test
    void defaultThemeIsDark() {
        assertEquals(Theme.DARK, service.loadTheme());
    }

    @Test
    void saveAndLoadTheme() {
        service.saveTheme(Theme.LIGHT);
        assertEquals(Theme.LIGHT, service.loadTheme());
    }

    @Test
    void defaultBoardThemeIsClassic() {
        assertEquals(BoardTheme.CLASSIC, service.loadBoardTheme());
    }

    @Test
    void saveAndLoadBoardTheme() {
        service.saveBoardTheme(BoardTheme.WALNUT);
        assertEquals(BoardTheme.WALNUT, service.loadBoardTheme());
    }

    @Test
    void defaultLanguageIsEn() {
        assertEquals(I18n.Language.EN, service.loadLanguage());
    }

    @Test
    void saveAndLoadLanguage() {
        service.saveLanguage(I18n.Language.DE);
        assertEquals(I18n.Language.DE, service.loadLanguage());
    }

    @Test
    void defaultPlayerNamesAreEmpty() {
        assertEquals("", service.loadPlayerWhite());
        assertEquals("", service.loadPlayerBlack());
    }

    @Test
    void saveAndLoadPlayerNames() {
        service.savePlayerWhite("Alice");
        service.savePlayerBlack("Bob");
        assertEquals("Alice", service.loadPlayerWhite());
        assertEquals("Bob", service.loadPlayerBlack());
    }
}
```

**Step 2: Run tests to verify they fail**

```bash
mvn test -pl modules/application -Dtest=SettingsServiceTest -q
```
Expected: FAIL.

**Step 3: Implement SettingsService.java**

```java
package io.github.conava.chess.application.settings;

import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.theme.BoardTheme;
import io.github.conava.chess.application.theme.Theme;
import java.util.prefs.Preferences;

public class SettingsService {

    private static final String KEY_THEME        = "theme";
    private static final String KEY_BOARD_THEME  = "boardTheme";
    private static final String KEY_LANGUAGE     = "language";
    private static final String KEY_PLAYER_WHITE = "playerWhite";
    private static final String KEY_PLAYER_BLACK = "playerBlack";

    private final Preferences prefs;

    public SettingsService() {
        this(Preferences.userNodeForPackage(SettingsService.class));
    }

    public SettingsService(Preferences prefs) {
        this.prefs = prefs;
    }

    public Theme loadTheme() {
        try { return Theme.valueOf(prefs.get(KEY_THEME, Theme.DARK.name())); }
        catch (IllegalArgumentException e) { return Theme.DARK; }
    }

    public void saveTheme(Theme theme) { prefs.put(KEY_THEME, theme.name()); }

    public BoardTheme loadBoardTheme() {
        try { return BoardTheme.valueOf(prefs.get(KEY_BOARD_THEME, BoardTheme.CLASSIC.name())); }
        catch (IllegalArgumentException e) { return BoardTheme.CLASSIC; }
    }

    public void saveBoardTheme(BoardTheme theme) { prefs.put(KEY_BOARD_THEME, theme.name()); }

    public I18n.Language loadLanguage() {
        try { return I18n.Language.valueOf(prefs.get(KEY_LANGUAGE, I18n.Language.EN.name())); }
        catch (IllegalArgumentException e) { return I18n.Language.EN; }
    }

    public void saveLanguage(I18n.Language lang) { prefs.put(KEY_LANGUAGE, lang.name()); }

    public String loadPlayerWhite() { return prefs.get(KEY_PLAYER_WHITE, ""); }
    public void savePlayerWhite(String name) { prefs.put(KEY_PLAYER_WHITE, name); }

    public String loadPlayerBlack() { return prefs.get(KEY_PLAYER_BLACK, ""); }
    public void savePlayerBlack(String name) { prefs.put(KEY_PLAYER_BLACK, name); }
}
```

**Step 4: Run tests to verify they pass**

```bash
mvn test -pl modules/application -Dtest=SettingsServiceTest
```
Expected: PASS.

**Step 5: Commit**

```bash
git add modules/application/src/main/java/io/github/conava/chess/application/settings/ \
        modules/application/src/test/java/io/github/conava/chess/application/settings/
git commit -m "feat(application): add SettingsService with Preferences persistence"
```

---

### Task 5: SceneManager

**Files:**
- Create: `modules/application/src/main/java/io/github/conava/chess/application/navigation/SceneManager.java`

No unit tests — SceneManager wraps JavaFX Stage/FXMLLoader which requires the FX toolkit. Tested manually via Task 8.

**Step 1: Implement SceneManager.java**

```java
package io.github.conava.chess.application.navigation;

import io.github.conava.chess.application.Chess;
import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.settings.SettingsService;
import io.github.conava.chess.application.theme.ThemeManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;

public class SceneManager {

    private static final String FXML_MAIN_MENU = "/fxml/main-menu.fxml";
    private static final String FXML_GAME      = "/fxml/game.fxml";
    private static final String FXML_SETTINGS  = "/fxml/settings.fxml";

    private final Stage primaryStage;
    private final Chess chess;
    private final ThemeManager themeManager;
    private final I18n i18n;
    private final SettingsService settingsService;

    public SceneManager(Stage primaryStage, Chess chess, ThemeManager themeManager,
                        I18n i18n, SettingsService settingsService) {
        this.primaryStage   = primaryStage;
        this.chess          = chess;
        this.themeManager   = themeManager;
        this.i18n           = i18n;
        this.settingsService = settingsService;
    }

    // ── Main screens ──────────────────────────────────────────────────────────

    public void showMainMenu() {
        var controller = new io.github.conava.chess.application.controllers.MainMenuController(this, i18n);
        swapScene(FXML_MAIN_MENU, controller, 900, 650);
        primaryStage.setMaximized(false);
    }

    public void showGame() {
        var controller = new io.github.conava.chess.application.controllers.GameController(
                this, chess, themeManager, i18n);
        swapScene(FXML_GAME, controller, 1280, 860);
        primaryStage.setMaximized(true);
    }

    public void showSettings() {
        var controller = new io.github.conava.chess.application.controllers.SettingsController(
                this, themeManager, i18n, settingsService);
        swapScene(FXML_SETTINGS, controller, 900, 650);
    }

    // ── Dialog helper ─────────────────────────────────────────────────────────

    /**
     * Opens a modal dialog and returns its controller so the caller can read results.
     * The dialog stage is styled with the active theme.
     */
    public <C> C showDialog(String fxmlResourcePath, C controller) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(fxmlResourcePath), i18n.getBundle());
            loader.setController(controller);
            Parent root = loader.load();

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(primaryStage);
            dialog.setResizable(false);

            Scene scene = new Scene(root);
            themeManager.registerScene(scene);
            dialog.setScene(scene);
            dialog.showAndWait();
            themeManager.unregisterScene(scene);

            return controller;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load dialog: " + fxmlResourcePath, e);
        }
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private void swapScene(String fxmlPath, Object controller, double w, double h) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(fxmlPath), i18n.getBundle());
            loader.setController(controller);
            Parent root = loader.load();

            Scene scene = primaryStage.getScene();
            if (scene == null) {
                scene = new Scene(root, w, h);
                themeManager.registerScene(scene);
                primaryStage.setScene(scene);
            } else {
                scene.setRoot(root);
            }
            primaryStage.show();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load FXML: " + fxmlPath, e);
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public Stage getPrimaryStage()          { return primaryStage; }
    public Chess getChess()                 { return chess; }
    public ThemeManager getThemeManager()   { return themeManager; }
    public I18n getI18n()                   { return i18n; }
    public SettingsService getSettingsService() { return settingsService; }
}
```

**Step 2: Commit**

```bash
git add modules/application/src/main/java/io/github/conava/chess/application/navigation/
git commit -m "feat(application): add SceneManager for single-stage FXML navigation"
```

---

### Task 6: CSS files

**Files:**
- Create: `modules/application/src/main/resources/css/base.css`
- Create: `modules/application/src/main/resources/css/dark.css`
- Create: `modules/application/src/main/resources/css/light.css`
- Create: `modules/application/src/main/resources/css/board/classic.css`
- Create: `modules/application/src/main/resources/css/board/ocean.css`
- Create: `modules/application/src/main/resources/css/board/walnut.css`

**Step 1: Create base.css** — layout, typography, component structure (theme-neutral)

```css
/* base.css — structure and layout, no color values */

.root {
    -fx-font-family: "Segoe UI", "Helvetica Neue", Arial, sans-serif;
    -fx-font-size: 14px;
}

/* ── Navigation buttons ── */
.nav-button {
    -fx-font-size: 16px;
    -fx-padding: 14 40 14 40;
    -fx-background-radius: 8;
    -fx-cursor: hand;
    -fx-min-width: 220px;
}

.nav-button:hover {
    -fx-scale-x: 1.03;
    -fx-scale-y: 1.03;
}

/* ── Danger/exit button ── */
.danger-button {
    -fx-font-size: 14px;
    -fx-padding: 10 24 10 24;
    -fx-background-radius: 6;
    -fx-cursor: hand;
}

/* ── Primary action button ── */
.primary-button {
    -fx-font-size: 14px;
    -fx-padding: 10 28 10 28;
    -fx-background-radius: 6;
    -fx-cursor: hand;
}

/* ── Card panel ── */
.card {
    -fx-background-radius: 12;
    -fx-padding: 16;
}

/* ── Section heading ── */
.section-heading {
    -fx-font-size: 13px;
    -fx-font-weight: bold;
    -fx-padding: 0 0 8 0;
}

/* ── Text field ── */
.text-field {
    -fx-background-radius: 6;
    -fx-padding: 8 12 8 12;
    -fx-font-size: 14px;
}

/* ── Combo box ── */
.combo-box {
    -fx-background-radius: 6;
}

/* ── Player name label ── */
.player-name {
    -fx-font-size: 18px;
    -fx-font-weight: bold;
}

.player-active-indicator {
    -fx-font-size: 12px;
}

/* ── Move list ── */
.move-list-view {
    -fx-background-insets: 0;
    -fx-padding: 4;
    -fx-background-radius: 6;
}

/* ── Board ── */
.chess-board {
    -fx-background-color: transparent;
}

.board-square {
    -fx-min-width: 0;
    -fx-min-height: 0;
    -fx-cursor: hand;
}

.board-label {
    -fx-font-size: 11px;
    -fx-alignment: center;
}

/* Legal move dot overlay */
.legal-move-dot {
    -fx-opacity: 0.55;
}

/* ── Settings ── */
.settings-section {
    -fx-spacing: 10;
    -fx-padding: 12 0 4 0;
}

.theme-swatch {
    -fx-min-width: 36px;
    -fx-min-height: 36px;
    -fx-background-radius: 6;
    -fx-cursor: hand;
    -fx-border-radius: 6;
    -fx-border-width: 2;
}

.theme-swatch:selected {
    -fx-border-width: 3;
}

/* ── Dialog ── */
.dialog-title {
    -fx-font-size: 20px;
    -fx-font-weight: bold;
    -fx-padding: 0 0 12 0;
}

/* ── Separator ── */
.separator {
    -fx-padding: 8 0 8 0;
}
```

**Step 2: Create dark.css**

```css
/* dark.css — dark theme color variables */

.root {
    app-bg:           #121212;
    app-surface:      #1e1e2e;
    app-card:         #252538;
    app-border:       #3a3a5c;
    app-primary:      #7c3aed;
    app-primary-dark: #5b21b6;
    app-danger:       #e53e3e;
    app-danger-dark:  #c53030;
    app-text:         #f0f0f5;
    app-subtext:      #9090a8;
    app-dot:          #a0a0ff;

    -fx-background: app-bg;
    -fx-base: app-surface;
    -fx-control-inner-background: app-card;
    -fx-text-fill: app-text;
    -fx-accent: app-primary;
}

.nav-button {
    -fx-background-color: app-card;
    -fx-text-fill: app-text;
    -fx-border-color: app-border;
    -fx-border-radius: 8;
    -fx-border-width: 1;
}

.nav-button:hover {
    -fx-background-color: app-primary;
    -fx-text-fill: white;
    -fx-border-color: app-primary;
}

.primary-button {
    -fx-background-color: app-primary;
    -fx-text-fill: white;
}

.primary-button:hover {
    -fx-background-color: app-primary-dark;
}

.danger-button {
    -fx-background-color: app-danger;
    -fx-text-fill: white;
}

.danger-button:hover {
    -fx-background-color: app-danger-dark;
}

.card {
    -fx-background-color: app-card;
    -fx-border-color: app-border;
    -fx-border-width: 1;
    -fx-border-radius: 12;
}

.section-heading {
    -fx-text-fill: app-subtext;
}

.player-name {
    -fx-text-fill: app-text;
}

.player-active-indicator {
    -fx-text-fill: app-primary;
}

.text-field {
    -fx-background-color: app-surface;
    -fx-text-fill: app-text;
    -fx-border-color: app-border;
    -fx-border-width: 1;
    -fx-border-radius: 6;
    -fx-prompt-text-fill: app-subtext;
}

.text-field:focused {
    -fx-border-color: app-primary;
}

.move-list-view {
    -fx-background-color: app-surface;
}

.move-list-view .list-cell {
    -fx-background-color: transparent;
    -fx-text-fill: app-text;
    -fx-padding: 4 8 4 8;
}

.move-list-view .list-cell:odd {
    -fx-background-color: app-card;
}

.legal-move-dot {
    -fx-fill: app-dot;
}

.board-label {
    -fx-text-fill: app-subtext;
}

.dialog-title {
    -fx-text-fill: app-text;
}

.theme-swatch {
    -fx-border-color: app-border;
}

.theme-swatch:selected {
    -fx-border-color: app-primary;
}
```

**Step 3: Create light.css**

```css
/* light.css — light theme color variables */

.root {
    app-bg:           #f5f5f7;
    app-surface:      #ffffff;
    app-card:         #ebebf0;
    app-border:       #d0d0dc;
    app-primary:      #7c3aed;
    app-primary-dark: #5b21b6;
    app-danger:       #e53e3e;
    app-danger-dark:  #c53030;
    app-text:         #1a1a2e;
    app-subtext:      #6060808;
    app-dot:          #7c3aed;

    -fx-background: app-bg;
    -fx-base: app-surface;
    -fx-control-inner-background: app-surface;
    -fx-text-fill: app-text;
    -fx-accent: app-primary;
}

.nav-button {
    -fx-background-color: app-surface;
    -fx-text-fill: app-text;
    -fx-border-color: app-border;
    -fx-border-radius: 8;
    -fx-border-width: 1;
}

.nav-button:hover {
    -fx-background-color: app-primary;
    -fx-text-fill: white;
    -fx-border-color: app-primary;
}

.primary-button {
    -fx-background-color: app-primary;
    -fx-text-fill: white;
}

.primary-button:hover {
    -fx-background-color: app-primary-dark;
}

.danger-button {
    -fx-background-color: app-danger;
    -fx-text-fill: white;
}

.danger-button:hover {
    -fx-background-color: app-danger-dark;
}

.card {
    -fx-background-color: app-surface;
    -fx-border-color: app-border;
    -fx-border-width: 1;
    -fx-border-radius: 12;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);
}

.section-heading {
    -fx-text-fill: app-subtext;
}

.player-name {
    -fx-text-fill: app-text;
}

.player-active-indicator {
    -fx-text-fill: app-primary;
}

.text-field {
    -fx-background-color: app-surface;
    -fx-text-fill: app-text;
    -fx-border-color: app-border;
    -fx-border-width: 1;
    -fx-border-radius: 6;
    -fx-prompt-text-fill: app-subtext;
}

.text-field:focused {
    -fx-border-color: app-primary;
}

.move-list-view {
    -fx-background-color: app-surface;
}

.move-list-view .list-cell {
    -fx-background-color: transparent;
    -fx-text-fill: app-text;
    -fx-padding: 4 8 4 8;
}

.move-list-view .list-cell:odd {
    -fx-background-color: app-card;
}

.legal-move-dot {
    -fx-fill: app-dot;
}

.board-label {
    -fx-text-fill: app-subtext;
}

.dialog-title {
    -fx-text-fill: app-text;
}

.theme-swatch {
    -fx-border-color: app-border;
}

.theme-swatch:selected {
    -fx-border-color: app-primary;
}
```

**Step 4: Create board/classic.css**

```css
/* classic.css — standard chess board colors */

.root {
    board-light: #f0d9b5;
    board-dark:  #b58863;
    board-highlight: rgba(0, 120, 200, 0.5);
    board-selected: rgba(0, 200, 80, 0.45);
}

.light-square { -fx-background-color: board-light; }
.dark-square  { -fx-background-color: board-dark; }
.highlighted-square { -fx-background-color: board-highlight; }
.selected-square    { -fx-background-color: board-selected; }
```

**Step 5: Create board/ocean.css**

```css
/* ocean.css — cool blue-teal board */

.root {
    board-light: #dee3e6;
    board-dark:  #4a90a4;
    board-highlight: rgba(255, 200, 0, 0.5);
    board-selected: rgba(0, 200, 80, 0.45);
}

.light-square { -fx-background-color: board-light; }
.dark-square  { -fx-background-color: board-dark; }
.highlighted-square { -fx-background-color: board-highlight; }
.selected-square    { -fx-background-color: board-selected; }
```

**Step 6: Create board/walnut.css**

```css
/* walnut.css — warm wood-tone board */

.root {
    board-light: #d4b896;
    board-dark:  #7a4f2c;
    board-highlight: rgba(0, 120, 200, 0.5);
    board-selected: rgba(0, 200, 80, 0.45);
}

.light-square { -fx-background-color: board-light; }
.dark-square  { -fx-background-color: board-dark; }
.highlighted-square { -fx-background-color: board-highlight; }
.selected-square    { -fx-background-color: board-selected; }
```

**Step 7: Commit**

```bash
git add modules/application/src/main/resources/css/
git commit -m "feat(application): add JavaFX CSS themes (dark/light/board variants)"
```

---

### Task 7: Chess.java — rewrite as JavaFX Application

**Files:**
- Overwrite: `modules/application/src/main/java/io/github/conava/chess/application/Chess.java`

This file extends `javafx.application.Application` AND keeps all existing facade methods intact. Only the `main()` and the GUI bootstrap change.

**Step 1: Rewrite Chess.java**

Replace the entire file content with:

```java
package io.github.conava.chess.application;

import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.navigation.SceneManager;
import io.github.conava.chess.application.settings.SettingsService;
import io.github.conava.chess.application.theme.ThemeManager;
import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.board.Board;
import io.github.conava.chess.core.data.io.Message;
import io.github.conava.chess.core.data.pieces.Piece;
import io.github.conava.chess.core.data.pieces.Pieces;
import io.github.conava.chess.core.data.player.Player;
import io.github.conava.chess.core.exceptions.IllegalMoveException;
import io.github.conava.chess.core.logic.game.Game;
import io.github.conava.chess.core.logic.game.GameState;
import io.github.conava.chess.core.logic.observer.GameObserver;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import io.github.conava.chess.application.network.ServerCommunicationTask;
import javafx.application.Application;
import javafx.stage.Stage;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entry point and façade for the Chess application.
 *
 * <p>Extends {@link Application} for JavaFX lifecycle management while also serving
 * as the single approved API surface between the UI layer and the {@code core} module.
 * All game interaction must go through this class (Architecture Law 2).
 */
public class Chess extends Application {

    private static final Logger LOGGER = Logger.getLogger(Chess.class.getName());
    private Game game;

    // ── JavaFX entry point ────────────────────────────────────────────────────

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        List<String> params = getParameters().getUnnamed();
        if (params.contains("nogui")) {
            LOGGER.log(Level.INFO, "Chess application started without GUI");
            return;
        }
        LOGGER.log(Level.INFO, "Chess application started with GUI");

        SettingsService settings = new SettingsService();
        ThemeManager themeManager = new ThemeManager();
        I18n i18n = new I18n(settings.loadLanguage());

        themeManager.setTheme(settings.loadTheme());
        themeManager.setBoardTheme(settings.loadBoardTheme());

        SceneManager sceneManager = new SceneManager(
                primaryStage, this, themeManager, i18n, settings);

        primaryStage.setTitle("Chess");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(650);
        sceneManager.showMainMenu();
    }

    // ── Game facade (all methods from the old Chess.java — unchanged) ─────────

    public void startGame(boolean online,
                          RulesetOptions selectedRuleset,
                          String playerWhiteName,
                          String playerBlackName,
                          Map<String, String> onlineGameSettings) {
        if (game == null) {
            if (online) {
                game = createOnlineGame(selectedRuleset, playerWhiteName,
                        playerBlackName, onlineGameSettings);
            } else {
                game = Game.createGame(false, selectedRuleset,
                        playerWhiteName, playerBlackName, null, null);
            }
            game.startGame();
        } else {
            LOGGER.log(Level.WARNING, "Game is already running");
        }
    }

    private Game createOnlineGame(RulesetOptions selectedRuleset,
                                   String playerWhiteName,
                                   String playerBlackName,
                                   Map<String, String> onlineGameSettings) {
        String serverIP   = onlineGameSettings.get("ip");
        int    serverPort = Integer.parseInt(onlineGameSettings.get("port"));

        CountDownLatch connectionLatch = new CountDownLatch(1);
        CountDownLatch gameReadyLatch  = new CountDownLatch(1);

        Game[] gameHolder = new Game[1];
        Consumer<Message> handler = msg -> {
            try { gameReadyLatch.await(); }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            gameHolder[0].handleMessage(msg);
        };

        ServerCommunicationTask task = new ServerCommunicationTask(
                serverIP, serverPort, connectionLatch, handler);
        Thread serverThread = new Thread(task);
        serverThread.setDaemon(true);
        serverThread.start();

        try { connectionLatch.await(); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        Game onlineGame = Game.createGame(true, selectedRuleset,
                playerWhiteName, playerBlackName, onlineGameSettings, task);
        gameHolder[0] = onlineGame;
        gameReadyLatch.countDown();

        if (!task.isConnected()) {
            onlineGame.setGameState(GameState.SERVER_ERROR);
            return onlineGame;
        }
        onlineGame.connectToServerGame();
        return onlineGame;
    }

    public GameState getState()               { return game == null ? null : game.getState(); }
    public Board     getBoard()               { return game == null ? null : game.getBoard(); }
    public Player    getCurrentPlayer()       { return game == null ? null : game.getCurrentPlayer(); }
    public Player    getPlayerWhite()         { return game == null ? null : game.getPlayerWhite(); }
    public Player    getPlayerBlack()         { return game == null ? null : game.getPlayerBlack(); }
    public String    getJoinCode()            { return game != null ? game.getJoinCode() : null; }

    public Piece getPieceAt(Square position) {
        return game == null ? null : game.getPieceAt(position);
    }

    public List<Square> getLegalSquares(Square position) {
        return game == null ? Collections.emptyList() : game.getLegalSquares(position);
    }

    public List<String> getMoveList() {
        return game == null ? Collections.emptyList() : game.getMoveList();
    }

    public void addObserver(GameObserver observer) {
        if (game == null) throw new IllegalStateException("No active game");
        game.addObserver(observer);
    }

    public void removeObserver(GameObserver observer) {
        if (game == null) throw new IllegalStateException("No active game");
        game.removeObserver(observer);
    }

    public void endGame() {
        if (game != null) { game.endGame(); game = null; }
    }

    public void movePiece(Square start, Square end) throws IllegalMoveException {
        if (game == null) throw new IllegalStateException("No active game");
        game.movePiece(start, end);
    }

    public void promoteMove(Square start, Square end, Pieces targetPiece)
            throws IllegalMoveException {
        if (game == null) throw new IllegalStateException("No active game");
        game.promoteMove(start, end, targetPiece);
    }
}
```

**Step 2: Verify compilation**

```bash
mvn compile -pl modules/application -am -q
```
Expected: BUILD SUCCESS. (No FXML files or controllers exist yet, but Chess.java should compile.)

**Step 3: Commit**

```bash
git add modules/application/src/main/java/io/github/conava/chess/application/Chess.java
git commit -m "feat(application): rewrite Chess.java as JavaFX Application entry point"
```

---

### Task 8: Main menu FXML + controller

**Files:**
- Create: `modules/application/src/main/resources/fxml/main-menu.fxml`
- Create: `modules/application/src/main/java/io/github/conava/chess/application/controllers/MainMenuController.java`

**Step 1: Create main-menu.fxml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.control.*?>
<?import javafx.scene.image.*?>
<?import javafx.geometry.*?>

<HBox xmlns:fx="http://javafx.com/fxml/1"
      fx:controller="io.github.conava.chess.application.controllers.MainMenuController"
      styleClass="root" stylesheets="">

    <!-- Left: branding panel -->
    <VBox styleClass="card" alignment="CENTER" HBox.hgrow="ALWAYS" minWidth="300">
        <padding><Insets top="60" right="40" bottom="60" left="40"/></padding>
        <VBox.margin><Insets right="1"/></VBox.margin>
        <ImageView fx:id="titleImage" fitWidth="260" preserveRatio="true"
                   pickOnBounds="true"/>
        <Label fx:id="appTitle" text="%menu.title" style="-fx-font-size:42px; -fx-font-weight:bold;"
               wrapText="true"/>
    </VBox>

    <!-- Right: navigation buttons -->
    <VBox alignment="CENTER" spacing="16" HBox.hgrow="ALWAYS">
        <padding><Insets top="80" right="60" bottom="80" left="60"/></padding>
        <Button fx:id="localBtn"  text="%menu.local"    onAction="#onLocalGame"
                styleClass="nav-button" maxWidth="Infinity"/>
        <Button fx:id="onlineBtn" text="%menu.online"   onAction="#onOnlineGame"
                styleClass="nav-button" maxWidth="Infinity"/>
        <Button fx:id="settingsBtn" text="%menu.settings" onAction="#onSettings"
                styleClass="nav-button" maxWidth="Infinity"/>
        <Button fx:id="exitBtn"   text="%menu.exit"     onAction="#onExit"
                styleClass="nav-button danger-button" maxWidth="Infinity"/>
    </VBox>
</HBox>
```

**Step 2: Create MainMenuController.java**

```java
package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.navigation.SceneManager;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import java.util.Map;
import java.util.Objects;

public class MainMenuController {

    private final SceneManager sceneManager;
    private final I18n i18n;

    @FXML private ImageView titleImage;
    @FXML private Button localBtn;
    @FXML private Button onlineBtn;
    @FXML private Button settingsBtn;
    @FXML private Button exitBtn;

    public MainMenuController(SceneManager sceneManager, I18n i18n) {
        this.sceneManager = sceneManager;
        this.i18n         = i18n;
    }

    @FXML
    public void initialize() {
        var imgUrl = getClass().getResource("/titleImage/chessTitleImage.jpg");
        if (imgUrl != null) {
            titleImage.setImage(new Image(imgUrl.toExternalForm()));
        }
    }

    @FXML
    private void onLocalGame() {
        OfflineSetupController setup = sceneManager.showDialog(
                "/fxml/offline-setup.fxml",
                new OfflineSetupController(i18n, sceneManager.getSettingsService()));
        if (!setup.isConfirmed()) return;

        sceneManager.getChess().startGame(
                false,
                setup.getRuleset(),
                setup.getPlayerWhite(),
                setup.getPlayerBlack(),
                null);
        sceneManager.showGame();
    }

    @FXML
    private void onOnlineGame() {
        OnlineSetupController setup = sceneManager.showDialog(
                "/fxml/online-setup.fxml",
                new OnlineSetupController(i18n, sceneManager.getSettingsService()));
        if (!setup.isConfirmed()) return;

        Map<String, String> opts = Map.of(
                "ip",       setup.getIp(),
                "port",     setup.getPort(),
                "joinCode", setup.getJoinCode());

        sceneManager.getChess().startGame(
                true,
                setup.getRuleset(),
                setup.getPlayerWhite(),
                setup.getPlayerBlack(),
                opts);
        sceneManager.showGame();
    }

    @FXML
    private void onSettings() {
        sceneManager.showSettings();
    }

    @FXML
    private void onExit() {
        ((Stage) exitBtn.getScene().getWindow()).close();
    }
}
```

**Step 3: Compile and verify**

```bash
mvn compile -pl modules/application -am -q
```
Expected: BUILD SUCCESS.

**Step 4: Commit**

```bash
git add modules/application/src/main/resources/fxml/main-menu.fxml \
        modules/application/src/main/java/io/github/conava/chess/application/controllers/MainMenuController.java
git commit -m "feat(application): add main menu FXML and controller"
```

---

### Task 9: Settings FXML + controller

**Files:**
- Create: `modules/application/src/main/resources/fxml/settings.fxml`
- Create: `modules/application/src/main/java/io/github/conava/chess/application/controllers/SettingsController.java`

**Step 1: Create settings.fxml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.control.*?>
<?import javafx.geometry.*?>

<VBox xmlns:fx="http://javafx.com/fxml/1"
      fx:controller="io.github.conava.chess.application.controllers.SettingsController"
      spacing="0" minWidth="500" maxWidth="700" alignment="CENTER">
    <padding><Insets top="40" right="60" bottom="40" left="60"/></padding>

    <Label text="%settings.title" styleClass="dialog-title"/>

    <!-- Appearance section -->
    <Label text="%settings.appearance" styleClass="section-heading"/>
    <VBox styleClass="card" spacing="14">
        <padding><Insets top="16" right="16" bottom="16" left="16"/></padding>

        <!-- Light / Dark toggle -->
        <HBox alignment="CENTER_LEFT" spacing="16">
            <Label text="%settings.theme"/>
            <ToggleGroup fx:id="themeGroup"/>
            <ToggleButton fx:id="darkToggle"  text="%settings.theme.dark"
                          toggleGroup="$themeGroup"/>
            <ToggleButton fx:id="lightToggle" text="%settings.theme.light"
                          toggleGroup="$themeGroup"/>
        </HBox>

        <!-- Board theme swatches -->
        <HBox alignment="CENTER_LEFT" spacing="12">
            <Label text="%settings.board"/>
            <ToggleGroup fx:id="boardGroup"/>
            <ToggleButton fx:id="classicSwatch" text="Classic"
                          styleClass="theme-swatch" toggleGroup="$boardGroup"
                          style="-fx-background-color: #b58863;"/>
            <ToggleButton fx:id="oceanSwatch"   text="Ocean"
                          styleClass="theme-swatch" toggleGroup="$boardGroup"
                          style="-fx-background-color: #4a90a4;"/>
            <ToggleButton fx:id="walnutSwatch"  text="Walnut"
                          styleClass="theme-swatch" toggleGroup="$boardGroup"
                          style="-fx-background-color: #7a4f2c;"/>
        </HBox>
    </VBox>

    <!-- Language section -->
    <Label text="%settings.language" styleClass="section-heading"
           style="-fx-padding: 16 0 0 0;"/>
    <VBox styleClass="card" spacing="14">
        <padding><Insets top="16" right="16" bottom="16" left="16"/></padding>
        <HBox spacing="12" alignment="CENTER_LEFT">
            <ToggleGroup fx:id="langGroup"/>
            <ToggleButton fx:id="enToggle" text="English" toggleGroup="$langGroup"/>
            <ToggleButton fx:id="deToggle" text="Deutsch" toggleGroup="$langGroup"/>
        </HBox>
    </VBox>

    <!-- Player names section -->
    <Label text="%settings.players" styleClass="section-heading"
           style="-fx-padding: 16 0 0 0;"/>
    <VBox styleClass="card" spacing="10">
        <padding><Insets top="16" right="16" bottom="16" left="16"/></padding>
        <TextField fx:id="whiteNameField" promptText="%settings.player.white"/>
        <TextField fx:id="blackNameField" promptText="%settings.player.black"/>
    </VBox>

    <!-- Buttons -->
    <HBox spacing="12" alignment="CENTER_RIGHT" style="-fx-padding: 24 0 0 0;">
        <Button text="%settings.cancel" onAction="#onCancel"/>
        <Button text="%settings.save"   onAction="#onSave" styleClass="primary-button"/>
    </HBox>
</VBox>
```

**Step 2: Create SettingsController.java**

```java
package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.navigation.SceneManager;
import io.github.conava.chess.application.settings.SettingsService;
import io.github.conava.chess.application.theme.BoardTheme;
import io.github.conava.chess.application.theme.Theme;
import io.github.conava.chess.application.theme.ThemeManager;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;

public class SettingsController {

    private final SceneManager    sceneManager;
    private final ThemeManager    themeManager;
    private final I18n            i18n;
    private final SettingsService settingsService;

    @FXML private ToggleButton darkToggle;
    @FXML private ToggleButton lightToggle;
    @FXML private ToggleGroup  themeGroup;

    @FXML private ToggleButton classicSwatch;
    @FXML private ToggleButton oceanSwatch;
    @FXML private ToggleButton walnutSwatch;
    @FXML private ToggleGroup  boardGroup;

    @FXML private ToggleButton enToggle;
    @FXML private ToggleButton deToggle;
    @FXML private ToggleGroup  langGroup;

    @FXML private TextField whiteNameField;
    @FXML private TextField blackNameField;

    public SettingsController(SceneManager sceneManager, ThemeManager themeManager,
                               I18n i18n, SettingsService settingsService) {
        this.sceneManager    = sceneManager;
        this.themeManager    = themeManager;
        this.i18n            = i18n;
        this.settingsService = settingsService;
    }

    @FXML
    public void initialize() {
        // Reflect current settings in the UI
        if (themeManager.getTheme() == Theme.DARK) darkToggle.setSelected(true);
        else lightToggle.setSelected(true);

        switch (themeManager.getBoardTheme()) {
            case CLASSIC -> classicSwatch.setSelected(true);
            case OCEAN   -> oceanSwatch.setSelected(true);
            case WALNUT  -> walnutSwatch.setSelected(true);
        }

        if (i18n.getLanguage() == I18n.Language.EN) enToggle.setSelected(true);
        else deToggle.setSelected(true);

        whiteNameField.setText(settingsService.loadPlayerWhite());
        blackNameField.setText(settingsService.loadPlayerBlack());

        // Live preview: theme and language change immediately on toggle
        themeGroup.selectedToggleProperty().addListener((o, old, sel) -> {
            if (sel == darkToggle)  themeManager.setTheme(Theme.DARK);
            else if (sel == lightToggle) themeManager.setTheme(Theme.LIGHT);
        });

        boardGroup.selectedToggleProperty().addListener((o, old, sel) -> {
            if      (sel == classicSwatch) themeManager.setBoardTheme(BoardTheme.CLASSIC);
            else if (sel == oceanSwatch)   themeManager.setBoardTheme(BoardTheme.OCEAN);
            else if (sel == walnutSwatch)  themeManager.setBoardTheme(BoardTheme.WALNUT);
        });

        langGroup.selectedToggleProperty().addListener((o, old, sel) -> {
            if      (sel == enToggle) i18n.setLanguage(I18n.Language.EN);
            else if (sel == deToggle) i18n.setLanguage(I18n.Language.DE);
        });
    }

    @FXML
    private void onSave() {
        settingsService.saveTheme(themeManager.getTheme());
        settingsService.saveBoardTheme(themeManager.getBoardTheme());
        settingsService.saveLanguage(i18n.getLanguage());
        settingsService.savePlayerWhite(whiteNameField.getText().trim());
        settingsService.savePlayerBlack(blackNameField.getText().trim());
        sceneManager.showMainMenu();
    }

    @FXML
    private void onCancel() {
        // Revert live previews to persisted values
        themeManager.setTheme(settingsService.loadTheme());
        themeManager.setBoardTheme(settingsService.loadBoardTheme());
        i18n.setLanguage(settingsService.loadLanguage());
        sceneManager.showMainMenu();
    }
}
```

**Step 3: Compile**

```bash
mvn compile -pl modules/application -am -q
```

**Step 4: Commit**

```bash
git add modules/application/src/main/resources/fxml/settings.fxml \
        modules/application/src/main/java/io/github/conava/chess/application/controllers/SettingsController.java
git commit -m "feat(application): add settings screen with live theme/language switching"
```

---

### Task 10: Offline setup dialog

**Files:**
- Create: `modules/application/src/main/resources/fxml/offline-setup.fxml`
- Create: `modules/application/src/main/java/io/github/conava/chess/application/controllers/OfflineSetupController.java`

**Step 1: Create offline-setup.fxml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.control.*?>
<?import javafx.geometry.*?>

<VBox xmlns:fx="http://javafx.com/fxml/1"
      fx:controller="io.github.conava.chess.application.controllers.OfflineSetupController"
      spacing="14" minWidth="380">
    <padding><Insets top="32" right="40" bottom="32" left="40"/></padding>

    <Label text="%dialog.offline.title" styleClass="dialog-title"/>

    <Label text="%dialog.offline.white"/>
    <TextField fx:id="whiteField" promptText="%dialog.offline.white"/>

    <Label text="%dialog.offline.black"/>
    <TextField fx:id="blackField" promptText="%dialog.offline.black"/>

    <Label text="%dialog.offline.ruleset"/>
    <ComboBox fx:id="rulesetBox" maxWidth="Infinity"/>

    <HBox spacing="12" alignment="CENTER_RIGHT" style="-fx-padding: 8 0 0 0;">
        <Button text="%dialog.offline.cancel" onAction="#onCancel"/>
        <Button text="%dialog.offline.start"  onAction="#onStart"
                styleClass="primary-button"/>
    </HBox>
</VBox>
```

**Step 2: Create OfflineSetupController.java**

```java
package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.settings.SettingsService;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class OfflineSetupController {

    private final I18n            i18n;
    private final SettingsService settingsService;

    @FXML private TextField whiteField;
    @FXML private TextField blackField;
    @FXML private ComboBox<RulesetOptions> rulesetBox;

    private boolean confirmed = false;

    public OfflineSetupController(I18n i18n, SettingsService settingsService) {
        this.i18n            = i18n;
        this.settingsService = settingsService;
    }

    @FXML
    public void initialize() {
        rulesetBox.setItems(FXCollections.observableArrayList(RulesetOptions.values()));
        rulesetBox.getSelectionModel().selectFirst();

        // Pre-fill remembered names
        whiteField.setText(settingsService.loadPlayerWhite());
        blackField.setText(settingsService.loadPlayerBlack());
    }

    @FXML
    private void onStart() {
        if (whiteField.getText().isBlank()) whiteField.setText("Player White");
        if (blackField.getText().isBlank()) blackField.setText("Player Black");
        confirmed = true;
        close();
    }

    @FXML
    private void onCancel() { close(); }

    private void close() {
        ((Stage) whiteField.getScene().getWindow()).close();
    }

    public boolean isConfirmed()       { return confirmed; }
    public String getPlayerWhite()     { return whiteField.getText().trim(); }
    public String getPlayerBlack()     { return blackField.getText().trim(); }
    public RulesetOptions getRuleset() { return rulesetBox.getValue(); }
}
```

**Step 3: Compile and commit**

```bash
mvn compile -pl modules/application -am -q
git add modules/application/src/main/resources/fxml/offline-setup.fxml \
        modules/application/src/main/java/io/github/conava/chess/application/controllers/OfflineSetupController.java
git commit -m "feat(application): add offline game setup dialog"
```

---

### Task 11: Online setup dialog

**Files:**
- Create: `modules/application/src/main/resources/fxml/online-setup.fxml`
- Create: `modules/application/src/main/java/io/github/conava/chess/application/controllers/OnlineSetupController.java`

**Step 1: Create online-setup.fxml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.control.*?>
<?import javafx.geometry.*?>

<VBox xmlns:fx="http://javafx.com/fxml/1"
      fx:controller="io.github.conava.chess.application.controllers.OnlineSetupController"
      spacing="12" minWidth="400">
    <padding><Insets top="32" right="40" bottom="32" left="40"/></padding>

    <Label text="%dialog.online.title" styleClass="dialog-title"/>

    <!-- Create / Join toggle -->
    <HBox spacing="12">
        <ToggleGroup fx:id="modeGroup"/>
        <ToggleButton fx:id="createToggle" text="%dialog.online.create"
                      toggleGroup="$modeGroup" selected="true"/>
        <ToggleButton fx:id="joinToggle"   text="%dialog.online.join"
                      toggleGroup="$modeGroup"/>
    </HBox>

    <Label text="%dialog.online.ip"/>
    <TextField fx:id="ipField" promptText="127.0.0.1"/>

    <Label text="%dialog.online.port"/>
    <TextField fx:id="portField" promptText="8080"/>

    <!-- Join code — only visible when joining -->
    <Label fx:id="joinCodeLabel" text="%dialog.online.joincode"/>
    <TextField fx:id="joinCodeField" promptText="%dialog.online.joincode"
               fx:id="joinCodeField"/>

    <Label text="%dialog.offline.ruleset"/>
    <ComboBox fx:id="rulesetBox" maxWidth="Infinity"/>

    <Label fx:id="errorLabel" style="-fx-text-fill: red;" visible="false"/>

    <HBox spacing="12" alignment="CENTER_RIGHT" style="-fx-padding: 8 0 0 0;">
        <Button text="%dialog.online.cancel" onAction="#onCancel"/>
        <Button text="%dialog.online.start"  onAction="#onConnect"
                styleClass="primary-button"/>
    </HBox>
</VBox>
```

**Step 2: Create OnlineSetupController.java**

```java
package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.settings.SettingsService;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class OnlineSetupController {

    private final I18n            i18n;
    private final SettingsService settingsService;

    @FXML private ToggleButton createToggle;
    @FXML private ToggleButton joinToggle;
    @FXML private ToggleGroup  modeGroup;

    @FXML private TextField ipField;
    @FXML private TextField portField;
    @FXML private TextField joinCodeField;
    @FXML private Label     joinCodeLabel;
    @FXML private ComboBox<RulesetOptions> rulesetBox;
    @FXML private Label     errorLabel;

    private boolean confirmed = false;

    public OnlineSetupController(I18n i18n, SettingsService settingsService) {
        this.i18n            = i18n;
        this.settingsService = settingsService;
    }

    @FXML
    public void initialize() {
        rulesetBox.setItems(FXCollections.observableArrayList(RulesetOptions.values()));
        rulesetBox.getSelectionModel().selectFirst();

        // Show/hide join code based on mode
        boolean isJoining = joinToggle.isSelected();
        joinCodeField.setVisible(isJoining);
        joinCodeLabel.setVisible(isJoining);

        modeGroup.selectedToggleProperty().addListener((o, old, sel) -> {
            boolean joining = sel == joinToggle;
            joinCodeField.setVisible(joining);
            joinCodeLabel.setVisible(joining);
        });
    }

    @FXML
    private void onConnect() {
        errorLabel.setVisible(false);

        String ip   = ipField.getText().trim();
        String port = portField.getText().trim();

        if (!isValidIp(ip)) {
            showError("Invalid IP address.");
            return;
        }
        if (!isValidPort(port)) {
            showError("Port must be 1–65535.");
            return;
        }

        confirmed = true;
        close();
    }

    @FXML
    private void onCancel() { close(); }

    private void close() {
        ((Stage) ipField.getScene().getWindow()).close();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }

    private boolean isValidIp(String ip) {
        if (ip.equals("localhost")) return true;
        // IPv4
        if (ip.matches("(\\d{1,3}\\.){3}\\d{1,3}")) return true;
        // IPv6 (basic)
        if (ip.contains(":")) return true;
        return false;
    }

    private boolean isValidPort(String port) {
        try { int p = Integer.parseInt(port); return p >= 1 && p <= 65535; }
        catch (NumberFormatException e) { return false; }
    }

    public boolean isConfirmed()       { return confirmed; }
    public String  getIp()             { return ipField.getText().trim(); }
    public String  getPort()           { return portField.getText().trim(); }
    public String  getJoinCode()       { return joinCodeField.isVisible() ? joinCodeField.getText().trim() : ""; }
    public RulesetOptions getRuleset() { return rulesetBox.getValue(); }
    public String  getPlayerWhite()    { return joinToggle.isSelected() ? "Opponent" : "You"; }
    public String  getPlayerBlack()    { return joinToggle.isSelected() ? "You" : "Opponent"; }
}
```

**Step 3: Compile and commit**

```bash
mvn compile -pl modules/application -am -q
git add modules/application/src/main/resources/fxml/online-setup.fxml \
        modules/application/src/main/java/io/github/conava/chess/application/controllers/OnlineSetupController.java
git commit -m "feat(application): add online game setup dialog"
```

---

### Task 12: Promotion and Waiting dialogs

**Files:**
- Create: `modules/application/src/main/resources/fxml/promotion.fxml`
- Create: `modules/application/src/main/java/io/github/conava/chess/application/controllers/PromotionController.java`
- Create: `modules/application/src/main/resources/fxml/waiting.fxml`
- Create: `modules/application/src/main/java/io/github/conava/chess/application/controllers/WaitingController.java`

**Step 1: Create promotion.fxml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.control.*?>
<?import javafx.scene.image.*?>
<?import javafx.geometry.*?>

<VBox xmlns:fx="http://javafx.com/fxml/1"
      fx:controller="io.github.conava.chess.application.controllers.PromotionController"
      spacing="20" alignment="CENTER">
    <padding><Insets top="28" right="32" bottom="28" left="32"/></padding>

    <Label text="%promotion.title" styleClass="dialog-title"/>

    <HBox fx:id="pieceRow" spacing="16" alignment="CENTER"/>
</VBox>
```

**Step 2: Create PromotionController.java**

```java
package io.github.conava.chess.application.controllers;

import io.github.conava.chess.core.data.pieces.Pieces;
import io.github.conava.chess.core.data.player.PlayerColor;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class PromotionController {

    @FXML private HBox pieceRow;

    private final PlayerColor playerColor;
    private Pieces selectedPiece = Pieces.QUEEN; // default

    // Pieces available for promotion
    private static final Pieces[] PROMOTION_OPTIONS = {
        Pieces.QUEEN, Pieces.ROOK, Pieces.BISHOP, Pieces.KNIGHT
    };

    public PromotionController(PlayerColor playerColor) {
        this.playerColor = playerColor;
    }

    @FXML
    public void initialize() {
        for (Pieces piece : PROMOTION_OPTIONS) {
            String iconPath = "/icon/" + piece.name().toLowerCase()
                    + "_" + playerColor.name().toLowerCase() + ".png";
            var url = getClass().getResource(iconPath);
            Button btn = new Button();
            if (url != null) {
                ImageView iv = new ImageView(new Image(url.toExternalForm()));
                iv.setFitWidth(64);
                iv.setFitHeight(64);
                iv.setPreserveRatio(true);
                btn.setGraphic(iv);
            } else {
                btn.setText(piece.name());
            }
            btn.getStyleClass().add("nav-button");
            Pieces p = piece;
            btn.setOnAction(e -> { selectedPiece = p; close(btn); });
            pieceRow.getChildren().add(btn);
        }
    }

    private void close(javafx.scene.Node node) {
        ((Stage) node.getScene().getWindow()).close();
    }

    public Pieces getSelectedPiece() { return selectedPiece; }
}
```

**Step 3: Create waiting.fxml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.control.*?>
<?import javafx.geometry.*?>

<VBox xmlns:fx="http://javafx.com/fxml/1"
      fx:controller="io.github.conava.chess.application.controllers.WaitingController"
      spacing="16" alignment="CENTER" minWidth="320">
    <padding><Insets top="36" right="48" bottom="36" left="48"/></padding>

    <Label text="%waiting.title" styleClass="dialog-title"/>
    <Label text="%waiting.code" styleClass="section-heading"/>
    <Label fx:id="codeLabel" style="-fx-font-size:28px; -fx-font-weight:bold;"/>
    <Label text="%waiting.instruction" wrapText="true" alignment="CENTER"/>
    <Button text="%waiting.cancel" onAction="#onCancel" styleClass="danger-button"/>
</VBox>
```

**Step 4: Create WaitingController.java**

```java
package io.github.conava.chess.application.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class WaitingController {

    @FXML private Label codeLabel;

    private final String joinCode;
    private boolean cancelled = false;

    public WaitingController(String joinCode) {
        this.joinCode = joinCode;
    }

    @FXML
    public void initialize() {
        codeLabel.setText(joinCode != null ? joinCode : "—");
    }

    @FXML
    private void onCancel() {
        cancelled = true;
        ((Stage) codeLabel.getScene().getWindow()).close();
    }

    public boolean isCancelled() { return cancelled; }
}
```

**Step 5: Compile and commit**

```bash
mvn compile -pl modules/application -am -q
git add modules/application/src/main/resources/fxml/promotion.fxml \
        modules/application/src/main/resources/fxml/waiting.fxml \
        modules/application/src/main/java/io/github/conava/chess/application/controllers/PromotionController.java \
        modules/application/src/main/java/io/github/conava/chess/application/controllers/WaitingController.java
git commit -m "feat(application): add promotion and waiting dialogs"
```

---

### Task 13: ExecuteMove — port to JavaFX Task

**Files:**
- Overwrite: `modules/application/src/main/java/io/github/conava/chess/application/tasks/ExecuteMove.java`
- Test: `modules/application/src/test/java/io/github/conava/chess/application/tasks/ExecuteMoveTest.java`

**Step 1: Write the failing test**

```java
package io.github.conava.chess.application.tasks;

import io.github.conava.chess.application.Chess;
import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.pieces.Pieces;
import io.github.conava.chess.core.exceptions.IllegalMoveException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExecuteMoveTest {

    @Test
    void callsMoveOnRegularMove() throws Exception {
        Chess chess = mock(Chess.class);
        Square from = new Square(6, 4);
        Square to   = new Square(4, 4);

        ExecuteMove task = new ExecuteMove(chess, from, to, null);
        task.call();

        verify(chess).movePiece(from, to);
        verify(chess, never()).promoteMove(any(), any(), any());
    }

    @Test
    void callsPromoteMoveOnPromotion() throws Exception {
        Chess chess = mock(Chess.class);
        Square from = new Square(1, 4);
        Square to   = new Square(0, 4);

        ExecuteMove task = new ExecuteMove(chess, from, to, Pieces.QUEEN);
        task.call();

        verify(chess).promoteMove(from, to, Pieces.QUEEN);
        verify(chess, never()).movePiece(any(), any());
    }
}
```

Note: this test requires Mockito. Add to pom.xml `<dependencies>` (test scope):

```xml
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <version>5.5.0</version>
    <scope>test</scope>
</dependency>
```

**Step 2: Run test to verify it fails**

```bash
mvn test -pl modules/application -Dtest=ExecuteMoveTest -q
```
Expected: FAIL — class not yet rewritten.

**Step 3: Rewrite ExecuteMove.java**

```java
package io.github.conava.chess.application.tasks;

import io.github.conava.chess.application.Chess;
import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.pieces.Pieces;
import javafx.concurrent.Task;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Background task that executes a chess move off the JavaFX Application Thread.
 *
 * <p>Delegates to {@link Chess#movePiece} or {@link Chess#promoteMove} in {@link #call()}.
 * UI refresh is driven entirely by the observer chain — this task holds no reference
 * to {@code GameController} and does not mutate UI state directly.
 */
public class ExecuteMove extends Task<Void> {

    private static final Logger LOGGER = Logger.getLogger(ExecuteMove.class.getName());

    private final Chess  chess;
    private final Square start;
    private final Square end;
    private final Pieces promotionPiece;

    public ExecuteMove(Chess chess, Square start, Square end, Pieces promotionPiece) {
        this.chess          = chess;
        this.start          = start;
        this.end            = end;
        this.promotionPiece = promotionPiece;
    }

    @Override
    protected Void call() throws Exception {
        if (promotionPiece != null) {
            chess.promoteMove(start, end, promotionPiece);
        } else {
            chess.movePiece(start, end);
        }
        return null;
    }

    @Override
    protected void failed() {
        LOGGER.log(Level.WARNING, "Move execution failed", getException());
    }
}
```

**Step 4: Run tests**

```bash
mvn test -pl modules/application -Dtest=ExecuteMoveTest
```
Expected: PASS.

**Step 5: Commit**

```bash
git add modules/application/pom.xml \
        modules/application/src/main/java/io/github/conava/chess/application/tasks/ExecuteMove.java \
        modules/application/src/test/java/io/github/conava/chess/application/tasks/ExecuteMoveTest.java
git commit -m "feat(application): port ExecuteMove from SwingWorker to JavaFX Task"
```

---

### Task 14: Game screen FXML + GameController

**Files:**
- Create: `modules/application/src/main/resources/fxml/game.fxml`
- Create: `modules/application/src/main/java/io/github/conava/chess/application/controllers/GameController.java`

**Step 1: Create game.fxml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.control.*?>
<?import javafx.geometry.*?>

<BorderPane xmlns:fx="http://javafx.com/fxml/1"
            fx:controller="io.github.conava.chess.application.controllers.GameController"
            styleClass="root">

    <!-- Left: black player card -->
    <left>
        <VBox styleClass="card" alignment="TOP_CENTER" spacing="8"
              minWidth="180" maxWidth="260" BorderPane.alignment="CENTER">
            <padding><Insets top="20" right="16" bottom="20" left="16"/></padding>
            <Label fx:id="blackName"   styleClass="player-name" text="Black"/>
            <Label fx:id="blackActive" styleClass="player-active-indicator"/>
        </VBox>
    </left>

    <!-- Center: board with rank/file labels -->
    <center>
        <VBox alignment="CENTER" BorderPane.alignment="CENTER">
            <!-- Rank labels row placeholder — built programmatically -->
            <HBox fx:id="boardRoot" alignment="CENTER" VBox.vgrow="ALWAYS">
                <!-- Left rank labels -->
                <VBox fx:id="rankLabels" alignment="CENTER" minWidth="20"/>
                <!-- Board container (square aspect ratio) -->
                <StackPane fx:id="boardContainer" VBox.vgrow="ALWAYS" HBox.hgrow="ALWAYS"/>
            </HBox>
            <!-- File labels -->
            <HBox fx:id="fileLabels" alignment="CENTER"/>
        </VBox>
    </center>

    <!-- Right: white player card + move list + leave button -->
    <right>
        <VBox spacing="12" minWidth="180" maxWidth="280" BorderPane.alignment="CENTER">
            <padding><Insets top="20" right="16" bottom="20" left="16"/></padding>

            <VBox styleClass="card" alignment="TOP_CENTER" spacing="8">
                <padding><Insets top="16" right="12" bottom="16" left="12"/></padding>
                <Label fx:id="whiteName"   styleClass="player-name" text="White"/>
                <Label fx:id="whiteActive" styleClass="player-active-indicator"/>
            </VBox>

            <Label text="%game.moves" styleClass="section-heading"/>
            <ListView fx:id="moveList" styleClass="move-list-view" VBox.vgrow="ALWAYS"/>

            <Button text="%game.leave" onAction="#onLeaveGame"
                    styleClass="danger-button" maxWidth="Infinity"/>
        </VBox>
    </right>
</BorderPane>
```

**Step 2: Create GameController.java**

```java
package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.Chess;
import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.navigation.SceneManager;
import io.github.conava.chess.application.tasks.ExecuteMove;
import io.github.conava.chess.application.theme.ThemeManager;
import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.board.Board;
import io.github.conava.chess.core.data.pieces.Piece;
import io.github.conava.chess.core.data.pieces.Pieces;
import io.github.conava.chess.core.data.player.Player;
import io.github.conava.chess.core.logic.game.GameState;
import io.github.conava.chess.core.logic.observer.GameObserver;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class GameController implements GameObserver {

    private static final Logger LOGGER = Logger.getLogger(GameController.class.getName());

    private final SceneManager sceneManager;
    private final Chess        chess;
    private final ThemeManager themeManager;
    private final I18n         i18n;

    @FXML private Label    blackName;
    @FXML private Label    blackActive;
    @FXML private Label    whiteName;
    @FXML private Label    whiteActive;
    @FXML private ListView<String> moveList;
    @FXML private StackPane boardContainer;
    @FXML private VBox     rankLabels;
    @FXML private HBox     fileLabels;
    @FXML private HBox     boardRoot;

    private final StackPane[][] boardSquares = new StackPane[8][8];
    private final List<StackPane> markedSquares = new ArrayList<>();
    private Square selectedSquare = null;
    private List<Square> legalSquares = List.of();
    private Board localBoard;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "chess-move-executor");
                t.setDaemon(true);
                return t;
            });

    public GameController(SceneManager sceneManager, Chess chess,
                          ThemeManager themeManager, I18n i18n) {
        this.sceneManager = sceneManager;
        this.chess        = chess;
        this.themeManager = themeManager;
        this.i18n         = i18n;
    }

    @FXML
    public void initialize() {
        buildBoard();
        buildLabels();
        registerWithGame();
        updateAll();
    }

    // ── Board construction ────────────────────────────────────────────────────

    private void buildBoard() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("chess-board");

        for (int row = 7; row >= 0; row--) {
            for (int col = 0; col < 8; col++) {
                StackPane square = new StackPane();
                square.getStyleClass().addAll("board-square",
                        (row + col) % 2 == 0 ? "light-square" : "dark-square");
                square.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
                square.setPrefSize(72, 72);

                final int r = row, c = col;
                square.setOnMouseClicked(e -> handleSquareClick(r, c));

                boardSquares[row][col] = square;
                grid.add(square, col, 7 - row);
            }
        }

        // Keep board square by binding width to height
        boardContainer.widthProperty().addListener((o, old, w) -> {
            double size = Math.min(w.doubleValue(), boardContainer.getHeight());
            grid.setPrefSize(size, size);
        });
        boardContainer.heightProperty().addListener((o, old, h) -> {
            double size = Math.min(boardContainer.getWidth(), h.doubleValue());
            grid.setPrefSize(size, size);
        });

        boardContainer.getChildren().add(grid);
    }

    private void buildLabels() {
        // Rank labels: 8 down to 1
        for (int row = 8; row >= 1; row--) {
            Label lbl = new Label(String.valueOf(row));
            lbl.getStyleClass().add("board-label");
            lbl.setMinHeight(72);
            rankLabels.getChildren().add(lbl);
        }
        // File labels: a–h
        for (char c = 'a'; c <= 'h'; c++) {
            Label lbl = new Label(String.valueOf(c));
            lbl.getStyleClass().add("board-label");
            lbl.setMinWidth(72);
            fileLabels.getChildren().add(lbl);
        }
    }

    // ── Game wiring ───────────────────────────────────────────────────────────

    private void registerWithGame() {
        chess.addObserver(this);

        Player white = chess.getPlayerWhite();
        Player black = chess.getPlayerBlack();
        if (white != null) whiteName.setText(white.name());
        if (black != null) blackName.setText(black.name());

        localBoard = chess.getBoard();
    }

    @Override
    public void onGameStateChanged() {
        Platform.runLater(this::update);
    }

    private void update() {
        GameState state = chess.getState();
        if (state == null || state == GameState.NO_GAME) return;

        switch (state) {
            case RUNNING -> {
                closeWaitingDialog();
                updateAll();
            }
            case WAITING_FOR_PLAYER -> showWaitingDialog();
            case SERVER_ERROR -> showErrorAndReturnToMenu(
                    i18n.get("error.server.title"), i18n.get("error.server"));
            default -> showGameEndDialog(state);
        }
    }

    // ── Waiting dialog handling ───────────────────────────────────────────────

    private javafx.stage.Stage waitingStage = null;

    private void showWaitingDialog() {
        if (waitingStage != null) return;
        String code = chess.getJoinCode();
        WaitingController ctrl = new WaitingController(code);
        sceneManager.showDialog("/fxml/waiting.fxml", ctrl);
        if (ctrl.isCancelled()) {
            chess.endGame();
            sceneManager.showMainMenu();
        }
    }

    private void closeWaitingDialog() {
        // Dialog closes itself; waitingStage tracking not needed with showAndWait
    }

    // ── UI update helpers ─────────────────────────────────────────────────────

    private void updateAll() {
        updateBoard();
        updateMoveList();
        updateActivePlayerIndicator();
    }

    private void updateBoard() {
        Board board = chess.getBoard();
        if (board == null || board.equals(localBoard)) return;
        localBoard = board;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                setPieceOnSquare(row, col, board.getPieceAt(new Square(row, col)));
            }
        }
    }

    private void setPieceOnSquare(int row, int col, Piece piece) {
        StackPane square = boardSquares[row][col];
        square.getChildren().removeIf(n -> "piece".equals(n.getUserData()));
        if (piece != null) {
            String path = "/icon/" + piece.getType().name().toLowerCase()
                    + "_" + piece.getPlayer().color().name().toLowerCase() + ".png";
            var url = getClass().getResource(path);
            if (url != null) {
                ImageView iv = new ImageView(new Image(url.toExternalForm()));
                iv.setFitWidth(56);
                iv.setFitHeight(56);
                iv.setPreserveRatio(true);
                iv.setUserData("piece");
                square.getChildren().add(iv);
            }
        }
    }

    private void updateMoveList() {
        moveList.setItems(FXCollections.observableArrayList(chess.getMoveList()));
        if (!moveList.getItems().isEmpty()) {
            moveList.scrollTo(moveList.getItems().size() - 1);
        }
    }

    private void updateActivePlayerIndicator() {
        Player current = chess.getCurrentPlayer();
        String activeText  = i18n.get("game.active");
        String waitingText = i18n.get("game.waiting");

        boolean whiteActive = current == chess.getPlayerWhite();
        this.whiteActive.setText(whiteActive ? activeText : waitingText);
        this.blackActive.setText(whiteActive ? waitingText : activeText);
    }

    // ── Board interaction ─────────────────────────────────────────────────────

    private void handleSquareClick(int row, int col) {
        Square clicked = new Square(row, col);
        Piece  piece   = chess.getPieceAt(clicked);

        // Select piece belonging to current player
        if (piece != null && piece.getPlayer() == chess.getCurrentPlayer()) {
            clearLegalMoveMarkers();
            selectedSquare = clicked;
            legalSquares   = chess.getLegalSquares(clicked);
            showLegalMoveMarkers(legalSquares);
            return;
        }

        // Execute move if clicked square is a legal destination
        if (selectedSquare != null && legalSquares.contains(clicked)) {
            clearLegalMoveMarkers();

            Piece movingPiece = chess.getPieceAt(selectedSquare);
            if (movingPiece != null && movingPiece.getType() == Pieces.PAWN
                    && (clicked.getY() == 0 || clicked.getY() == 7)) {
                // Promotion
                PromotionController promoCtrl = new PromotionController(
                        movingPiece.getPlayer().color());
                sceneManager.showDialog("/fxml/promotion.fxml", promoCtrl);
                Pieces chosen = promoCtrl.getSelectedPiece();
                submitMove(selectedSquare, clicked, chosen);
            } else {
                submitMove(selectedSquare, clicked, null);
            }

            selectedSquare = null;
            legalSquares   = List.of();
        } else {
            // Deselect
            clearLegalMoveMarkers();
            selectedSquare = null;
            legalSquares   = List.of();
        }
    }

    private void submitMove(Square from, Square to, Pieces promotion) {
        ExecuteMove task = new ExecuteMove(chess, from, to, promotion);
        executor.submit(task);
    }

    private void showLegalMoveMarkers(List<Square> squares) {
        for (Square sq : squares) {
            StackPane pane = boardSquares[sq.getY()][sq.getX()];
            Circle dot = new Circle(14);
            dot.getStyleClass().add("legal-move-dot");
            dot.setUserData("dot");
            dot.setMouseTransparent(true);
            pane.getChildren().add(dot);
            markedSquares.add(pane);
        }
    }

    private void clearLegalMoveMarkers() {
        for (StackPane pane : markedSquares) {
            pane.getChildren().removeIf(n -> "dot".equals(n.getUserData()));
        }
        markedSquares.clear();
    }

    // ── End-game dialogs ──────────────────────────────────────────────────────

    private void showGameEndDialog(GameState state) {
        String stateKey = "state." + state.name();
        String message;
        try { message = i18n.get(stateKey); }
        catch (java.util.MissingResourceException e) { message = state.name(); }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                message + "\n\n" + i18n.get("game.end.return"),
                ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            chess.endGame();
            sceneManager.showMainMenu();
        }
    }

    private void showErrorAndReturnToMenu(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR,
                message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
        chess.endGame();
        sceneManager.showMainMenu();
    }

    @FXML
    private void onLeaveGame() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                i18n.get("game.end.return"), ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            chess.endGame();
            executor.shutdown();
            sceneManager.showMainMenu();
        }
    }
}
```

**Step 3: Compile**

```bash
mvn compile -pl modules/application -am -q
```
Expected: BUILD SUCCESS.

**Step 4: Commit**

```bash
git add modules/application/src/main/resources/fxml/game.fxml \
        modules/application/src/main/java/io/github/conava/chess/application/controllers/GameController.java
git commit -m "feat(application): add game screen with board, move list, and player panels"
```

---

### Task 15: Delete old Swing files + fill ChessTest

**Files:**
- Delete: all files under `modules/application/src/main/java/io/github/conava/chess/application/components/`
- Delete: all files under `modules/application/src/main/java/io/github/conava/chess/application/window/`
- Update: `modules/application/src/test/java/io/github/conava/chess/application/ChessTest.java`

**Step 1: Delete all old Swing files**

```bash
rm -rf modules/application/src/main/java/io/github/conava/chess/application/components/
rm -rf modules/application/src/main/java/io/github/conava/chess/application/window/
```

**Step 2: Verify project still compiles**

```bash
mvn compile -pl modules/application -am -q
```
Expected: BUILD SUCCESS (no Swing imports remain in any kept file).

**Step 3: Fill ChessTest.java — replace all empty test bodies**

```java
package io.github.conava.chess.application;

import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.pieces.Pieces;
import io.github.conava.chess.core.logic.game.GameState;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChessTest {

    private Chess chess;

    @BeforeEach
    void setUp() {
        chess = new Chess();  // no-arg constructor needed — add one if missing
        chess.startGame(false, RulesetOptions.STANDARD, "White", "Black", null);
    }

    @Test
    void getStateIsRunningAfterStart() {
        assertEquals(GameState.RUNNING, chess.getState());
    }

    @Test
    void getBoardIsNotNullAfterStart() {
        assertNotNull(chess.getBoard());
    }

    @Test
    void getCurrentPlayerIsNotNullAfterStart() {
        assertNotNull(chess.getCurrentPlayer());
    }

    @Test
    void getPlayerWhiteIsNotNull() {
        assertNotNull(chess.getPlayerWhite());
    }

    @Test
    void getPlayerBlackIsNotNull() {
        assertNotNull(chess.getPlayerBlack());
    }

    @Test
    void getLegalSquaresReturnsList() {
        Square e2 = new Square(6, 4);
        assertNotNull(chess.getLegalSquares(e2));
    }

    @Test
    void getMoveListIsEmptyBeforeAnyMove() {
        assertTrue(chess.getMoveList().isEmpty());
    }

    @Test
    void endGameSetsStateToNull() {
        chess.endGame();
        assertNull(chess.getState());
    }

    @Test
    void addObserverThrowsWhenNoGame() {
        chess.endGame();
        assertThrows(IllegalStateException.class, () -> chess.addObserver(() -> {}));
    }

    @Test
    void movePieceThrowsWhenNoGame() {
        chess.endGame();
        assertThrows(IllegalStateException.class,
                () -> chess.movePiece(new Square(6, 4), new Square(4, 4)));
    }
}
```

Note: `Chess` needs a no-arg constructor for tests (without launching the JavaFX `Application`). Add this to `Chess.java`:

```java
/** No-arg constructor for use in tests and non-GUI contexts. */
public Chess() {}
```

**Step 4: Run all application tests**

```bash
mvn test -pl modules/application
```
Expected: All tests PASS.

**Step 5: Commit**

```bash
git add -A modules/application/src/
git commit -m "feat(application): delete Swing code, fill ChessTest with facade assertions"
```

---

### Task 16: Final verification

**Step 1: Full build from root**

```bash
mvn clean install -q
```
Expected: BUILD SUCCESS across all modules.

**Step 2: Run the application**

```bash
mvn javafx:run -pl modules/application -am
```
Expected: main menu appears, dark theme active.

**Step 3: Verify architecture laws**

Check each law manually:
1. `grep -r "import io.github.conava.chess.application" modules/core/` → zero results
2. `grep -r "import io.github.conava.chess.server" modules/application/` → zero results
3. `grep -r "import javax.swing" modules/application/src/main/` → zero results
4. `grep -r "import java.awt" modules/application/src/main/` → zero results (awt.Color gone)

**Step 4: Update CLAUDE.md run commands**

In the root `CLAUDE.md`, update the run section for the application:

```bash
# Development run (JavaFX maven plugin):
mvn javafx:run -pl modules/application -am

# Fat JAR (requires JavaFX SDK on module-path):
java --module-path /path/to/javafx-sdk/lib \
     --add-modules javafx.controls,javafx.fxml \
     -jar modules/application/target/application-0.9.jar
```

**Step 5: Commit**

```bash
git add CLAUDE.md modules/application/CLAUDE.md
git commit -m "docs: update run commands for JavaFX migration"
```

---

## Testing Requirements

| Class | Test File | What to verify |
|-------|-----------|----------------|
| `ThemeManager` | `ThemeManagerTest` | Default values, property updates, cssFile() paths |
| `I18n` | `I18nTest` | EN/DE key lookup, language switching, property updates |
| `SettingsService` | `SettingsServiceTest` | Round-trip save/load for all 5 settings |
| `ExecuteMove` | `ExecuteMoveTest` | Routes to movePiece vs promoteMove correctly |
| `Chess` (facade) | `ChessTest` | State, board, player accessors; ISE on no-game actions |

---

## Documentation Updates & ADR

After implementation is complete:

1. Update `docs/migration/swing-to-javafx.md` — mark migration as complete.
2. Create `docs/decisions/0001-javafx-migration.md` with:
   - **Status**: accepted
   - Decision to rewrite rather than incrementally migrate
   - Decision to use single-stage FXML navigation
   - Decision to use CSS looked-up colors for theming
3. Update `modules/application/CLAUDE.md` — replace Swing descriptions with JavaFX equivalents.
4. Update root `README.md` if it references the Swing UI.
