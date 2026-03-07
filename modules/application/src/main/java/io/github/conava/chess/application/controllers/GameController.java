package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.Chess;
import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.navigation.SceneManager;
import io.github.conava.chess.application.theme.ThemeManager;

/**
 * Stub for the game screen controller. Full implementation in Task 14.
 */
public class GameController {

    private final SceneManager sceneManager;
    private final Chess        chess;
    private final ThemeManager themeManager;
    private final I18n         i18n;

    public GameController(SceneManager sceneManager, Chess chess,
                          ThemeManager themeManager, I18n i18n) {
        this.sceneManager = sceneManager;
        this.chess        = chess;
        this.themeManager = themeManager;
        this.i18n         = i18n;
    }
}
