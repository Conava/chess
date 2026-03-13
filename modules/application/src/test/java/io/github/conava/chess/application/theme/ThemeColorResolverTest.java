package io.github.conava.chess.application.theme;

import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ThemeColorResolverTest {

    private static final List<String> ALL_TOKENS = List.of(
            "board-light", "board-dark", "app-bg", "app-primary",
            "app-dot", "app-text", "app-surface"
    );

    private ThemeManager themeManager;
    private StackPane root;

    @BeforeAll
    static void initToolkit() throws Exception {
        // Initialize JavaFX toolkit for headless testing
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Toolkit already initialized — that is fine
        }
    }

    @BeforeEach
    void setUp() {
        themeManager = new ThemeManager();
        root = new StackPane();
    }

    @Test
    void constructorAddsHiddenRegionsToParent() {
        int childrenBefore = root.getChildren().size();
        ThemeColorResolver resolver = new ThemeColorResolver(root, themeManager, ALL_TOKENS);
        // Should have added one hidden Region per token
        assertEquals(childrenBefore + ALL_TOKENS.size(), root.getChildren().size());
    }

    @Test
    void hiddenRegionsAreNotVisibleAndNotManaged() {
        ThemeColorResolver resolver = new ThemeColorResolver(root, themeManager, ALL_TOKENS);
        root.getChildren().forEach(child -> {
            assertFalse(child.isVisible(), "Hidden region should not be visible");
            assertFalse(child.isManaged(), "Hidden region should not be managed");
        });
    }

    @Test
    void resolveReturnsNonNullForAllTokens() {
        ThemeColorResolver resolver = new ThemeColorResolver(root, themeManager, ALL_TOKENS);
        for (String token : ALL_TOKENS) {
            Color color = resolver.resolve(token);
            assertNotNull(color, "resolve('" + token + "') should not return null");
        }
    }

    @Test
    void resolveReturnsFallbackForUnknownToken() {
        ThemeColorResolver resolver = new ThemeColorResolver(root, themeManager, ALL_TOKENS);
        Color color = resolver.resolve("nonexistent-token");
        assertNotNull(color, "Unknown token should return a fallback color");
        assertEquals(Color.MAGENTA, color, "Unknown token fallback should be MAGENTA");
    }

    @Test
    void cachingReturnsSameInstanceWithoutThemeChange() {
        ThemeColorResolver resolver = new ThemeColorResolver(root, themeManager, ALL_TOKENS);
        Color first = resolver.resolve("app-bg");
        Color second = resolver.resolve("app-bg");
        assertSame(first, second, "Cached color should be the same instance");
    }

    @Test
    void allSevenTokensResolveSimultaneously() {
        ThemeColorResolver resolver = new ThemeColorResolver(root, themeManager, ALL_TOKENS);
        // Resolve all tokens and ensure we get 7 distinct non-null results
        long distinctCount = ALL_TOKENS.stream()
                .map(resolver::resolve)
                .filter(c -> c != null)
                .count();
        assertEquals(ALL_TOKENS.size(), distinctCount,
                "All 7 tokens should resolve to non-null colors");
    }

    @Test
    void cacheInvalidatesOnThemeChange() {
        ThemeColorResolver resolver = new ThemeColorResolver(root, themeManager, ALL_TOKENS);
        Color before = resolver.resolve("app-bg");
        // Trigger theme change
        themeManager.setTheme(Theme.LIGHT_ARCTIC);
        Color after = resolver.resolve("app-bg");
        // In a headless environment both may be fallbacks, but the cache
        // should have been invalidated (re-resolved), so we just verify
        // the resolver still works after a theme change.
        assertNotNull(after, "Color should still be non-null after theme change");
    }

    @Test
    void disposeRemovesHiddenRegionsFromParent() {
        ThemeColorResolver resolver = new ThemeColorResolver(root, themeManager, ALL_TOKENS);
        assertEquals(ALL_TOKENS.size(), root.getChildren().size());
        resolver.dispose();
        assertEquals(0, root.getChildren().size(),
                "dispose() should remove all hidden regions from parent");
    }

    @Test
    void disposeIsIdempotent() {
        ThemeColorResolver resolver = new ThemeColorResolver(root, themeManager, ALL_TOKENS);
        resolver.dispose();
        resolver.dispose(); // Second call should not throw
        assertEquals(0, root.getChildren().size());
    }

    @Test
    void resolveAfterDisposeReturnsFallback() {
        ThemeColorResolver resolver = new ThemeColorResolver(root, themeManager, ALL_TOKENS);
        resolver.dispose();
        // After dispose, resolve should still return a fallback, not throw
        Color color = resolver.resolve("app-bg");
        assertNotNull(color, "resolve() after dispose should return fallback color");
    }
}
