package io.github.conava.chess.application.menu;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MenuSilhouetteLayerTest {

    /** Chess piece Unicode characters U+2654 through U+265F. */
    private static final Set<String> CHESS_PIECES = Set.of(
            "\u2654", "\u2655", "\u2656", "\u2657", "\u2658", "\u2659",
            "\u265A", "\u265B", "\u265C", "\u265D", "\u265E", "\u265F"
    );

    private MenuSilhouetteLayer layer;

    @BeforeAll
    static void initToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Toolkit already initialized
        }
    }

    @BeforeEach
    void setUp() {
        layer = new MenuSilhouetteLayer(Color.WHITE);
    }

    @Test
    void createsCorrectNumberOfSilhouettes() {
        long textNodeCount = layer.getChildren().stream()
                .filter(n -> n instanceof Text)
                .count();
        assertTrue(textNodeCount >= 3 && textNodeCount <= 5,
                "Expected 3-5 Text nodes, got " + textNodeCount);
    }

    @Test
    void silhouettesAreMouseTransparent() {
        layer.getChildren().stream()
                .filter(n -> n instanceof Text)
                .forEach(n -> assertTrue(n.isMouseTransparent(),
                        "Each silhouette Text node must be mouseTransparent"));
    }

    @Test
    void silhouetteOpacityWithinSpec() {
        layer.getChildren().stream()
                .filter(n -> n instanceof Text)
                .map(n -> (Text) n)
                .forEach(t -> {
                    double opacity = t.getOpacity();
                    assertTrue(opacity >= 0.04 && opacity <= 0.12,
                            "Opacity " + opacity + " outside range [0.04, 0.12]");
                });
    }

    @Test
    void silhouetteFontSizeWithinSpec() {
        layer.getChildren().stream()
                .filter(n -> n instanceof Text)
                .map(n -> (Text) n)
                .forEach(t -> {
                    double size = t.getFont().getSize();
                    assertTrue(size >= 80 && size <= 160,
                            "Font size " + size + " outside range [80, 160]");
                });
    }

    @Test
    void silhouettesUseChessPieceCharacters() {
        layer.getChildren().stream()
                .filter(n -> n instanceof Text)
                .map(n -> (Text) n)
                .forEach(t -> assertTrue(CHESS_PIECES.contains(t.getText()),
                        "Text '" + t.getText() + "' is not a chess piece Unicode character"));
    }

    @Test
    void stopAllCleansUpTransitions() {
        layer.startAnimations();
        layer.stopAll();
        // After stopAll, no exception should be thrown and the layer should still
        // contain its children (cleanup means transitions stopped, not nodes removed)
        assertFalse(layer.getChildren().isEmpty(),
                "Children should still exist after stopAll");
    }

    @Test
    void layerIsMouseTransparent() {
        assertTrue(layer.isMouseTransparent(),
                "The layer Pane itself must be mouseTransparent");
        assertFalse(layer.isPickOnBounds(),
                "The layer Pane must have pickOnBounds=false");
    }
}
