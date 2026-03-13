package io.github.conava.chess.application.fxml;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * String-content tests that verify each converted FXML has the expected
 * cinematic panel structure. No JavaFX toolkit is needed because the checks
 * are done against the raw XML text.
 *
 * <p>Tests verify:
 * <ul>
 *   <li>Root element is a {@code VBox} with {@code styleClass="cinematic-form-panel"}</li>
 *   <li>No legacy {@code menu-brand-panel} StackPane remains</li>
 *   <li>All critical {@code fx:id} attributes are preserved</li>
 * </ul>
 */
class FxmlPanelStructureTest {

    // ── helpers ───────────────────────────────────────────────────────────────

    /**
     * Reads an FXML file from the classpath and returns its full text content.
     *
     * @param resourcePath classpath-relative path, e.g. {@code /fxml/login.fxml}
     * @return file contents as a String
     * @throws IOException if the resource cannot be found or read
     */
    private String readFxml(String resourcePath) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            assertNotNull(is, "FXML resource not found on classpath: " + resourcePath);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // ── login.fxml ────────────────────────────────────────────────────────────

    /**
     * Verifies that login.fxml root element is a VBox containing the
     * "cinematic-form-panel" style class (not the old HBox root).
     */
    @Test
    void loginFxml_rootIsVBoxWithCinematicFormPanel() throws IOException {
        String content = readFxml("/fxml/login.fxml");
        // Root must be a VBox — old root was HBox
        assertTrue(content.contains("<VBox"), "login.fxml should have a VBox root");
        // Must carry the cinematic-form-panel style class
        assertTrue(content.contains("cinematic-form-panel"),
                "login.fxml VBox should have styleClass=\"cinematic-form-panel\"");
    }

    /**
     * Verifies that the legacy left-side brand panel has been removed from login.fxml.
     */
    @Test
    void loginFxml_doesNotContainBrandPanel() throws IOException {
        String content = readFxml("/fxml/login.fxml");
        assertFalse(content.contains("menu-brand-panel"),
                "login.fxml must not contain the legacy menu-brand-panel");
    }

    /**
     * Verifies that all fx:id attributes required by LoginController are still present.
     */
    @Test
    void loginFxml_preservesAllFxIds() throws IOException {
        String content = readFxml("/fxml/login.fxml");
        assertTrue(content.contains("fx:id=\"ipField\""), "ipField fx:id missing");
        assertTrue(content.contains("fx:id=\"portField\""), "portField fx:id missing");
        assertTrue(content.contains("fx:id=\"usernameField\""), "usernameField fx:id missing");
        assertTrue(content.contains("fx:id=\"passwordField\""), "passwordField fx:id missing");
        assertTrue(content.contains("fx:id=\"loginButton\""), "loginButton fx:id missing");
        assertTrue(content.contains("fx:id=\"registerLink\""), "registerLink fx:id missing");
        assertTrue(content.contains("fx:id=\"errorLabel\""), "errorLabel fx:id missing");
    }

    // ── register.fxml ─────────────────────────────────────────────────────────

    /**
     * Verifies that register.fxml root element is a VBox containing the
     * "cinematic-form-panel" style class (not the old HBox root).
     */
    @Test
    void registerFxml_rootIsVBoxWithCinematicFormPanel() throws IOException {
        String content = readFxml("/fxml/register.fxml");
        assertTrue(content.contains("<VBox"), "register.fxml should have a VBox root");
        assertTrue(content.contains("cinematic-form-panel"),
                "register.fxml VBox should have styleClass=\"cinematic-form-panel\"");
    }

    /**
     * Verifies that the legacy left-side brand panel has been removed from register.fxml.
     */
    @Test
    void registerFxml_doesNotContainBrandPanel() throws IOException {
        String content = readFxml("/fxml/register.fxml");
        assertFalse(content.contains("menu-brand-panel"),
                "register.fxml must not contain the legacy menu-brand-panel");
    }

    /**
     * Verifies that all fx:id attributes required by RegisterController are still present.
     */
    @Test
    void registerFxml_preservesAllFxIds() throws IOException {
        String content = readFxml("/fxml/register.fxml");
        assertTrue(content.contains("fx:id=\"ipField\""), "ipField fx:id missing");
        assertTrue(content.contains("fx:id=\"portField\""), "portField fx:id missing");
        assertTrue(content.contains("fx:id=\"usernameField\""), "usernameField fx:id missing");
        assertTrue(content.contains("fx:id=\"passwordField\""), "passwordField fx:id missing");
        assertTrue(content.contains("fx:id=\"confirmPasswordField\""), "confirmPasswordField fx:id missing");
        assertTrue(content.contains("fx:id=\"registerButton\""), "registerButton fx:id missing");
        assertTrue(content.contains("fx:id=\"loginLink\""), "loginLink fx:id missing");
        assertTrue(content.contains("fx:id=\"errorLabel\""), "errorLabel fx:id missing");
    }

    // ── waiting-for-match.fxml ────────────────────────────────────────────────

    /**
     * Verifies that waiting-for-match.fxml uses the "cinematic-form-panel" style class.
     */
    @Test
    void waitingForMatchFxml_hasCinematicFormPanelStyle() throws IOException {
        String content = readFxml("/fxml/waiting-for-match.fxml");
        assertTrue(content.contains("cinematic-form-panel"),
                "waiting-for-match.fxml should use cinematic-form-panel style class");
    }

    /**
     * Verifies that waiting-for-match.fxml preserves all fx:id elements required by
     * WaitingForMatchController.
     */
    @Test
    void waitingForMatchFxml_preservesAllFxIds() throws IOException {
        String content = readFxml("/fxml/waiting-for-match.fxml");
        assertTrue(content.contains("fx:id=\"searchingLabel\""), "searchingLabel fx:id missing");
        assertTrue(content.contains("fx:id=\"spinner\""), "spinner fx:id missing");
        assertTrue(content.contains("fx:id=\"rulesetLabel\""), "rulesetLabel fx:id missing");
        assertTrue(content.contains("fx:id=\"cancelBtn\""), "cancelBtn fx:id missing");
    }

    /**
     * Verifies that waiting-for-match.fxml aligns its panel to the right, matching
     * the offline-setup.fxml cinematic pattern.
     */
    @Test
    void waitingForMatchFxml_hasCenterRightAlignment() throws IOException {
        String content = readFxml("/fxml/waiting-for-match.fxml");
        assertTrue(content.contains("CENTER_RIGHT"),
                "waiting-for-match.fxml should have StackPane.alignment=\"CENTER_RIGHT\"");
    }
}
