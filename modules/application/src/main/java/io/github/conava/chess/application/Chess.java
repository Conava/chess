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
 * as the single approved API surface between the UI layer and the {@code core} module
 * (Architecture Law 2). All game interaction must go through this class.
 */
public class Chess extends Application {

    private static final Logger LOGGER = Logger.getLogger(Chess.class.getName());
    private Game game;

    /** No-arg constructor required by JavaFX Application and for unit tests. */
    public Chess() {}

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

        SettingsService settings   = new SettingsService();
        ThemeManager themeManager  = new ThemeManager();
        I18n i18n                  = new I18n(settings.loadLanguage());

        themeManager.setTheme(settings.loadTheme());
        themeManager.setBoardTheme(settings.loadBoardTheme());

        SceneManager sceneManager = new SceneManager(
                primaryStage, this, themeManager, i18n, settings);

        primaryStage.setTitle("Chess");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(650);
        sceneManager.showMainMenu();
    }

    // ── Game facade ───────────────────────────────────────────────────────────

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

    public GameState getState()           { return game == null ? null : game.getState(); }
    public Board     getBoard()           { return game == null ? null : game.getBoard(); }
    public Player    getCurrentPlayer()   { return game == null ? null : game.getCurrentPlayer(); }
    public Player    getPlayerWhite()     { return game == null ? null : game.getPlayerWhite(); }
    public Player    getPlayerBlack()     { return game == null ? null : game.getPlayerBlack(); }
    public String    getJoinCode()        { return game != null ? game.getJoinCode() : null; }
    public String    getGameLabel()       { return game != null ? game.getRuleset() != null ? game.getRuleset().getGameLabel() : "" : ""; }

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
