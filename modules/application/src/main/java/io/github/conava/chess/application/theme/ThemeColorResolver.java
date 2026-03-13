package io.github.conava.chess.application.theme;

import javafx.beans.value.ChangeListener;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bridges CSS looked-up colors into Java code for Canvas rendering.
 *
 * <p>JavaFX does not expose CSS looked-up colors (e.g. {@code board-light},
 * {@code app-primary}) directly to Java code. This class creates hidden
 * {@link Region} nodes styled with {@code -fx-background-color} set to the
 * desired looked-up color. After CSS is applied, the resolved {@link Color}
 * can be read from the Region's {@link Background}.</p>
 *
 * <p>Resolved colors are cached and automatically invalidated when the
 * active theme changes via {@link ThemeManager}.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * ThemeColorResolver resolver = new ThemeColorResolver(rootPane, themeManager,
 *         List.of("board-light", "board-dark", "app-bg"));
 * Color bg = resolver.resolve("app-bg");
 * // ... use bg in Canvas rendering ...
 * resolver.dispose(); // when no longer needed
 * }</pre>
 */
public class ThemeColorResolver {

    /** Fallback color returned when a token cannot be resolved. */
    private static final Color FALLBACK = Color.MAGENTA;

    private final Pane parent;
    private final ThemeManager themeManager;
    private final Map<String, Region> probeRegions;
    private final Map<String, Color> cache;
    private final List<String> tokens;
    private final ChangeListener<Theme> themeListener;
    private boolean disposed;

    /**
     * Creates a new resolver that can look up the given CSS color tokens.
     *
     * <p>For each token, a hidden {@link Region} is created with an inline
     * style of {@code -fx-background-color: <token>}. The regions are added
     * to {@code parent} but are invisible and unmanaged (size 0x0), so they
     * do not affect layout.</p>
     *
     * @param parent       the parent pane (typically the root StackPane) to
     *                     attach hidden probe regions to
     * @param themeManager the theme manager whose changes trigger cache
     *                     invalidation
     * @param tokens       the CSS looked-up color token names to resolve
     *                     (e.g. "board-light", "app-bg")
     */
    public ThemeColorResolver(Pane parent, ThemeManager themeManager, List<String> tokens) {
        this.parent = parent;
        this.themeManager = themeManager;
        this.tokens = List.copyOf(tokens);
        this.probeRegions = new HashMap<>();
        this.cache = new HashMap<>();
        this.disposed = false;

        for (String token : this.tokens) {
            Region probe = createProbeRegion(token);
            probeRegions.put(token, probe);
            parent.getChildren().add(probe);
        }

        // Invalidate cache when the theme changes
        themeListener = (obs, oldTheme, newTheme) -> cache.clear();
        themeManager.currentThemeProperty().addListener(themeListener);
    }

    /**
     * Resolves the CSS looked-up color for the given token name.
     *
     * <p>If the color has already been resolved and the theme has not
     * changed since, the cached value is returned. Otherwise the color
     * is read from the probe region's background fill.</p>
     *
     * <p>If resolution fails (e.g. the region has no background, or CSS
     * has not been applied), a sensible fallback color ({@link Color#MAGENTA})
     * is returned. An unknown token also returns the fallback.</p>
     *
     * @param token the CSS color token name (e.g. "app-primary")
     * @return the resolved {@link Color}, never {@code null}
     */
    public Color resolve(String token) {
        Color cached = cache.get(token);
        if (cached != null) {
            return cached;
        }

        Region probe = probeRegions.get(token);
        if (probe == null) {
            return FALLBACK;
        }

        Color resolved = extractColor(probe);
        if (resolved == null) {
            resolved = getFallbackForToken(token);
        }
        cache.put(token, resolved);
        return resolved;
    }

    /**
     * Removes all hidden probe regions from the parent pane and clears
     * internal state. This method is idempotent.
     */
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        themeManager.currentThemeProperty().removeListener(themeListener);
        parent.getChildren().removeAll(probeRegions.values());
        probeRegions.clear();
        cache.clear();
    }

    /**
     * Creates a hidden, unmanaged Region with an inline style that sets
     * its background color to the given CSS looked-up color token.
     */
    private Region createProbeRegion(String token) {
        Region probe = new Region();
        probe.setManaged(false);
        probe.setVisible(false);
        probe.setPrefSize(0, 0);
        probe.setMaxSize(0, 0);
        probe.setMinSize(0, 0);
        probe.setStyle("-fx-background-color: " + token + ";");
        return probe;
    }

    /**
     * Extracts the color from a Region's background fill, if present.
     *
     * @param probe the probe region to read
     * @return the extracted {@link Color}, or {@code null} if unavailable
     */
    private Color extractColor(Region probe) {
        Background bg = probe.getBackground();
        if (bg == null || bg.getFills().isEmpty()) {
            return null;
        }
        BackgroundFill fill = bg.getFills().get(0);
        Paint paint = fill.getFill();
        if (paint instanceof Color color) {
            return color;
        }
        return null;
    }

    /**
     * Returns a sensible fallback color for a known token.
     * Unknown tokens return {@link #FALLBACK} (magenta).
     */
    private Color getFallbackForToken(String token) {
        return switch (token) {
            case "board-light" -> Color.web("#c4b5d6");
            case "board-dark" -> Color.web("#5c4478");
            case "app-bg" -> Color.web("#080812");
            case "app-primary" -> Color.web("#8b5cf6");
            case "app-dot" -> Color.web("#a78bfa");
            case "app-text" -> Color.web("#ededf8");
            case "app-surface" -> Color.web("#0f1020");
            default -> FALLBACK;
        };
    }
}
