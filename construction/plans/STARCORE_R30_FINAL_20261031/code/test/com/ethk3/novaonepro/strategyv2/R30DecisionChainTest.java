package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import java.util.Arrays;
import org.junit.Test;

/** R30-B07-03 宿主测试。复制到 app/src/test/java/com/ethk3/novaonepro/strategyv2/。 */
public class R30DecisionChainTest {
    static DecisionChainR30.Facts allPass() {
        DecisionChainR30.Facts f = new DecisionChainR30.Facts();
        f.dataHealthy = f.freezeComplete = f.modelAvailable = f.certificateActive = f.selectorConfirmed = f.onePerIssue = f.stakeFixed = f.actRuleFired = f.userRealEnabled = Boolean.TRUE;
        return f;
    }
    @Test public void realRequiresEveryItemAndDefaultsOff() {
        assertTrue(DecisionChainR30.explain(allPass()).allowReal);
        DecisionChainR30.Facts f = allPass(); f.userRealEnabled = null;
        DecisionChainR30.Verdict v = DecisionChainR30.explain(f);
        assertFalse(v.allowReal); assertEquals("userRealEnabled:UNKNOWN", v.firstBlocker);
        f = allPass(); f.stakeFixed = false; f.certificateActive = false;
        v = DecisionChainR30.explain(f);
        assertEquals("certificateActive:FAIL", v.firstBlocker);
        assertEquals("FAIL", v.items.get("stakeFixed"));
        assertEquals(1, DecisionChainR30.blockerHistogram(Arrays.asList(v, DecisionChainR30.explain(allPass()))).size());
    }
}
