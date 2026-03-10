package io.github.conava.chess.core.logic.ruleset;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for RulesetOptions enum display names and valueOf behaviour.
 * The valueOf contract must remain unaffected by the toString override so that
 * server-side parsing (which uses valueOf) continues to work correctly.
 */
class RulesetOptionsTest {

    @Test
    void standard_toString_returnsDisplayName() {
        assertEquals("Standard Chess", RulesetOptions.STANDARD.toString());
    }

    @Test
    void chess960_toString_returnsDisplayName() {
        assertEquals("Chess 960", RulesetOptions.CHESS960.toString());
    }

    @Test
    void valueOf_standard_resolvesByConstantName() {
        assertEquals(RulesetOptions.STANDARD, RulesetOptions.valueOf("STANDARD"));
    }

    @Test
    void valueOf_chess960_resolvesByConstantName() {
        assertEquals(RulesetOptions.CHESS960, RulesetOptions.valueOf("CHESS960"));
    }

    @Test
    void values_hasTwoConstants() {
        assertEquals(2, RulesetOptions.values().length);
    }

    @Test
    void values_containsExactlyTwoOptions() {
        assertEquals(2, RulesetOptions.values().length, "Exactly STANDARD and CHESS960 must exist");
    }
}
