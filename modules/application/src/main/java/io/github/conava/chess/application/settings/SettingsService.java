package io.github.conava.chess.application.settings;

import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.theme.Theme;

import java.util.prefs.Preferences;

/**
 * Persists and retrieves user preferences using {@link Preferences}.
 * The no-arg constructor uses the package node; the single-arg constructor
 * accepts an injected {@link Preferences} node (used in tests).
 */
public class SettingsService {

    private static final String KEY_THEME = "theme";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_PLAYER_WHITE = "playerWhite";
    private static final String KEY_PLAYER_BLACK = "playerBlack";
    private static final String KEY_AUTH_TOKEN = "authToken";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_SERVER_HOST = "serverHost";
    private static final String KEY_SERVER_PORT = "serverPort";

    private final Preferences prefs;

    public SettingsService() {
        this(Preferences.userNodeForPackage(SettingsService.class));
    }

    public SettingsService(Preferences prefs) {
        this.prefs = prefs;
    }

    public Theme loadTheme() {
        try {
            return Theme.valueOf(prefs.get(KEY_THEME, Theme.DARK_PURPLE.name()));
        } catch (IllegalArgumentException e) {
            return Theme.DARK_PURPLE;
        }
    }

    public void saveTheme(Theme theme) {
        prefs.put(KEY_THEME, theme.name());
    }

    public I18n.Language loadLanguage() {
        try {
            return I18n.Language.valueOf(prefs.get(KEY_LANGUAGE, I18n.Language.EN.name()));
        } catch (IllegalArgumentException e) {
            return I18n.Language.EN;
        }
    }

    public void saveLanguage(I18n.Language lang) {
        prefs.put(KEY_LANGUAGE, lang.name());
    }

    public String loadPlayerWhite() {
        return prefs.get(KEY_PLAYER_WHITE, "");
    }

    public void savePlayerWhite(String name) {
        prefs.put(KEY_PLAYER_WHITE, name);
    }

    public String loadPlayerBlack() {
        return prefs.get(KEY_PLAYER_BLACK, "");
    }

    public void savePlayerBlack(String name) {
        prefs.put(KEY_PLAYER_BLACK, name);
    }

    // ── Auth token persistence ─────────────────────────────────────────────────

    public String loadAuthToken() {
        return prefs.get(KEY_AUTH_TOKEN, null);
    }

    public void saveAuthToken(String token) {
        if (token == null) {
            prefs.remove(KEY_AUTH_TOKEN);
        } else {
            prefs.put(KEY_AUTH_TOKEN, token);
        }
    }

    public void clearAuthToken() {
        prefs.remove(KEY_AUTH_TOKEN);
        prefs.remove(KEY_USER_ID);
        prefs.remove(KEY_USERNAME);
    }

    public int loadUserId() {
        return prefs.getInt(KEY_USER_ID, 0);
    }

    public void saveUserId(int userId) {
        prefs.putInt(KEY_USER_ID, userId);
    }

    public String loadUsername() {
        return prefs.get(KEY_USERNAME, "");
    }

    public void saveUsername(String username) {
        prefs.put(KEY_USERNAME, username == null ? "" : username);
    }

    public String loadServerHost() {
        return prefs.get(KEY_SERVER_HOST, "");
    }

    public void saveServerHost(String host) {
        prefs.put(KEY_SERVER_HOST, host == null ? "" : host);
    }

    public int loadServerPort() {
        return prefs.getInt(KEY_SERVER_PORT, 54321);
    }

    public void saveServerPort(int port) {
        prefs.putInt(KEY_SERVER_PORT, port);
    }
}
