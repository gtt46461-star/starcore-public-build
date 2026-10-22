package com.ethk3.novaonepro;

import org.junit.Test;
import static org.junit.Assert.*;

/** Regression for existing generated predicates; host SQLite executes all 405 separately. */
public final class RetentionRegistryArgumentsTest {
    @Test public void disabledPredicateWithForeignKeyGuardsHasNoArguments() {
        RetentionTableRegistryCP12.Rule rule=RetentionTableRegistryCP12.require("research_atom_spec_v2");
        assertTrue(rule.eligibleSql.startsWith("0 AND "));
        assertArrayEquals(new String[0],rule.args("202601010001",12345L));
    }
    @Test public void issueTailBindsTheTargetCutoff() {
        assertArrayEquals(new String[]{"202601010001"},
            RetentionTableRegistryCP12.require("draws").args("202601010001",12345L));
    }
    @Test public void timeTailBindsTheProjectedOpenTime() {
        assertArrayEquals(new String[]{"12345"},
            RetentionTableRegistryCP12.require("diagnostic_log").args("202601010001",12345L));
    }
    @Test public void aggregateOverflowHasNoTailArgument() {
        assertArrayEquals(new String[0],
            RetentionTableRegistryCP12.require("break_memory").args("202601010001",12345L));
    }
}
