package io.github.conava.chess.application.navigation;

import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@link PanelId} enum and the {@link PanelHost} interface.
 *
 * <p>PanelId defines the set of panel identifiers used for in-place panel
 * navigation within the main menu shell. PanelHost is the interface that
 * MainMenuController implements to host and manage those panels.</p>
 */
class PanelIdTest {

    // ── PanelId enum value tests ─────────────────────────────────────────────

    /**
     * Verifies that PanelId contains all six expected panel identifiers.
     */
    @Test
    void PanelId_enumContainsAllExpectedValues() {
        Set<PanelId> values = EnumSet.allOf(PanelId.class);

        assertTrue(values.contains(PanelId.OFFLINE_SETUP), "missing OFFLINE_SETUP");
        assertTrue(values.contains(PanelId.ONLINE_SETUP), "missing ONLINE_SETUP");
        assertTrue(values.contains(PanelId.SETTINGS), "missing SETTINGS");
        assertTrue(values.contains(PanelId.LOGIN), "missing LOGIN");
        assertTrue(values.contains(PanelId.REGISTER), "missing REGISTER");
        assertTrue(values.contains(PanelId.WAITING_FOR_MATCH), "missing WAITING_FOR_MATCH");
    }

    /**
     * Verifies that PanelId.values() returns exactly 6 elements — no hidden extras.
     */
    @Test
    void PanelId_values_returnsCorrectCount() {
        assertEquals(6, PanelId.values().length,
                "PanelId should have exactly 6 values but found: "
                        + Arrays.toString(PanelId.values()));
    }

    // ── PanelHost interface compilation test ─────────────────────────────────

    /**
     * Verifies that PanelHost defines showPanel, closePanel, switchPanel,
     * getActivePanel, and showWaitingForMatch by compiling a concrete stub that
     * implements PanelHost.
     *
     * <p>If PanelHost is missing any of these method signatures this class will
     * not compile, causing the test to fail at build time.</p>
     */
    @Test
    void PanelHost_interfaceDefinesShowAndCloseMethods() {
        // Arrange: create a concrete stub implementing PanelHost
        PanelHost stub = new PanelHostStub();

        // Act & Assert: call each method to confirm the signatures exist
        stub.showPanel(PanelId.OFFLINE_SETUP);
        stub.closePanel();
        stub.switchPanel(PanelId.SETTINGS);
        PanelId active = stub.getActivePanel();

        // The stub tracks the last show/switch call and returns null after close
        assertEquals(PanelId.SETTINGS, active,
                "getActivePanel should return the last panel passed to switchPanel");
    }

    /**
     * Verifies that PanelHost defines showWaitingForMatch(RulesetOptions, String, int)
     * and that calling it transitions the active panel to WAITING_FOR_MATCH.
     */
    @Test
    void PanelHost_showWaitingForMatch_setsActivePanelToWaitingForMatch() {
        PanelHost stub = new PanelHostStub();

        stub.showPanel(PanelId.ONLINE_SETUP);
        stub.showWaitingForMatch(RulesetOptions.STANDARD, "localhost", 54321);

        assertEquals(PanelId.WAITING_FOR_MATCH, stub.getActivePanel(),
                "showWaitingForMatch should set the active panel to WAITING_FOR_MATCH");
    }

    /**
     * Verifies that getActivePanel returns null initially (before any panel is shown).
     */
    @Test
    void PanelHost_getActivePanel_returnsNullBeforeAnyPanelIsShown() {
        PanelHost stub = new PanelHostStub();

        assertNull(stub.getActivePanel(),
                "getActivePanel should be null when no panel has been shown yet");
    }

    // ── Private test stub ────────────────────────────────────────────────────

    /**
     * Minimal concrete implementation of {@link PanelHost} used for compilation
     * and behaviour verification in unit tests. Not intended for production use.
     */
    private static class PanelHostStub implements PanelHost {

        private PanelId activePanel;

        @Override
        public void showPanel(PanelId panelId) {
            this.activePanel = panelId;
        }

        @Override
        public void closePanel() {
            this.activePanel = null;
        }

        @Override
        public void switchPanel(PanelId panelId) {
            this.activePanel = panelId;
        }

        @Override
        public PanelId getActivePanel() {
            return activePanel;
        }

        @Override
        public void showWaitingForMatch(RulesetOptions ruleset, String serverIp, int serverPort) {
            this.activePanel = PanelId.WAITING_FOR_MATCH;
        }
    }
}
