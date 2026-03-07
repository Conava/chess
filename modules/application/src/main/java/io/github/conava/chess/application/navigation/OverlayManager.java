package io.github.conava.chess.application.navigation;

import io.github.conava.chess.application.i18n.I18n;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Hosts modal dialogs as in-window overlays on top of the primary scene's
 * root {@link StackPane}.
 *
 * <p>Dialogs are loaded from FXML, wrapped in a semi-transparent dim backdrop,
 * and pushed onto the root stack. The calling thread (FX Application Thread)
 * is "blocked" via {@link Platform#enterNestedEventLoop} — the UI remains
 * responsive during the wait because nested event loops still process events.
 *
 * <p>Dialog controllers dismiss themselves by calling the {@code closeAction}
 * {@link Runnable} passed at construction time, which calls {@link #dismiss()},
 * exiting the nested loop and returning control to the caller.
 */
public class OverlayManager {

    private final StackPane rootStack;
    private final I18n      i18n;
    private final Deque<Object> nestedLoopKeys = new ArrayDeque<>();

    public OverlayManager(StackPane rootStack, I18n i18n) {
        this.rootStack = rootStack;
        this.i18n      = i18n;
    }

    /**
     * Loads {@code fxmlPath}, sets {@code controller}, adds a dimmed overlay
     * to the root stack, then blocks via nested event loop until
     * {@link #dismiss()} is called. Returns the controller after dismissal.
     */
    public <C> C showOverlay(String fxmlPath, C controller) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(fxmlPath), i18n.getBundle());
            loader.setController(controller);
            Parent content = loader.load();

            Region dim = new Region();
            dim.getStyleClass().add("overlay-dim");
            dim.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

            StackPane overlay = new StackPane(dim, content);
            overlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

            rootStack.getChildren().add(overlay);
            Object key = new Object();
            nestedLoopKeys.push(key);
            try {
                Platform.enterNestedEventLoop(key);
            } finally {
                rootStack.getChildren().remove(overlay);
                nestedLoopKeys.remove(key);
            }
            return controller;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load overlay: " + fxmlPath, e);
        }
    }

    /**
     * Shows an inline confirmation card (no FXML) and returns {@code true}
     * if the user clicked Yes, {@code false} for No.
     */
    public boolean showConfirm(String message) {
        boolean[] result = {false};

        Label msg = new Label(message);
        msg.setWrapText(true);
        msg.setStyle("-fx-font-size: 15px;");

        Button yes = new Button(i18n.get("dialog.confirm.yes"));
        yes.getStyleClass().add("btn-primary");
        yes.setOnAction(e -> { result[0] = true; dismiss(); });

        Button no = new Button(i18n.get("dialog.confirm.no"));
        no.getStyleClass().add("btn-ghost");
        no.setOnAction(e -> dismiss());

        HBox buttons = new HBox(12, no, yes);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox card = new VBox(16, msg, buttons);
        card.getStyleClass().add("overlay-card");
        card.setPadding(new Insets(28));
        card.setMaxWidth(400);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        Region dim = new Region();
        dim.getStyleClass().add("overlay-dim");
        dim.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        StackPane overlay = new StackPane(dim, card);
        overlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        rootStack.getChildren().add(overlay);
        Object key = new Object();
        nestedLoopKeys.push(key);
        try {
            Platform.enterNestedEventLoop(key);
        } finally {
            rootStack.getChildren().remove(overlay);
            nestedLoopKeys.remove(key);
        }
        return result[0];
    }

    /**
     * Dismisses the topmost overlay and unblocks the corresponding
     * {@link #showOverlay} or {@link #showConfirm} call.
     */
    public void dismiss() {
        if (!nestedLoopKeys.isEmpty()) {
            Platform.exitNestedEventLoop(nestedLoopKeys.pop(), null);
        }
    }
}
