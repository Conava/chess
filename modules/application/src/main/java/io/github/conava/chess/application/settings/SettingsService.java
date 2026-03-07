package io.github.conava.chess.application.settings;

import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.theme.BoardTheme;
import io.github.conava.chess.application.theme.Theme;
import java.util.prefs.Preferences;

/**
 * Persists and retrieves user preferences using {@link Preferences}.
 * The no-arg constructor uses the package node; the single-arg constructor
 * accepts an injected {@link Preferences} node (used in tests).
 */
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
        try { return Theme.valueOf(prefs.get(KEY_THEME, Theme.DARK_PURPLE.name())); }
        catch (IllegalArgumentException e) { return Theme.DARK_PURPLE; }
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
