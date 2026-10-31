package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/** R30-B04-01 宿主测试。复制到 app/src/test/java/com/ethk3/novaonepro/strategyv2/。 */
public class R30PolicyContractTest {
    static PolicySpecR27 policy(PolicyAstR27.Node ast, PolicySpecR27.Rule rule, PolicySpecR27.Meta meta) {
        return new PolicySpecR27(ast, 3, rule, 0.02, 0.15, 1.0, true, 0.01, 0.001, meta);
    }
    static final PolicySpecR27.Meta META = new PolicySpecR27.Meta("I01", "BIRTH", Arrays.asList("p0"), 1, 7L, "频率×遗漏组合");

    @Test public void completePolicyPasses() {
        PolicyAstR27.Node ast = PolicyAstR27.op("add", PolicyAstR27.leaf("freq", 3, 32), PolicyAstR27.leaf("freqnb", 3, 128));
        List<String> v = PolicyContractR30.violations(policy(ast, PolicySpecR27.Rule.MARGIN, META));
        assertTrue(v.toString(), v.isEmpty());
    }
    @Test public void emptyShellsRejected() {
        assertTrue(PolicyContractR30.violations(policy(PolicyAstR27.constant(1), PolicySpecR27.Rule.MARGIN, META)).contains("PROGRAM_CONSTANT_ONLY"));
        assertTrue(PolicyContractR30.violations(policy(PolicyAstR27.leaf("freq", 3, 32), PolicySpecR27.Rule.MARGIN, null)).contains("PROVENANCE_MISSING"));
        assertTrue(PolicyContractR30.violations(policy(PolicyAstR27.leaf("freq", 3, 33), PolicySpecR27.Rule.MARGIN, META)).get(0).startsWith("PROGRAM_RANGE"));
        assertTrue(PolicyContractR30.violations(new PolicySpecR27(PolicyAstR27.leaf("freq", 3, 32), 11, PolicySpecR27.Rule.ALWAYS, 0, 0, 1, true, 0.01, 0, META)).contains("SELECTOR_POSITION:11"));
        assertTrue(PolicyContractR30.controlOnly(policy(PolicyAstR27.leaf("freq", 3, 32), PolicySpecR27.Rule.RANDOM, META)));
    }
}
