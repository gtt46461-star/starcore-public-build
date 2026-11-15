package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import org.junit.Test;

/** R30-B03-01 宿主测试。复制到 app/src/test/java/com/ethk3/novaonepro/strategyv2/。 */
public class R30PitGuardTest {
    static PitGuardR30.Freeze legal() {
        PitGuardR30.Freeze f = new PitGuardR30.Freeze();
        f.targetOrdinal = 100; f.trainedThroughOrdinal = 99; f.featureThroughOrdinal = 99; f.position = 4; f.k3 = new int[] {1, 5, 9};
        f.probabilities = new double[] {.1, .1, .1, .1, .1, .1, .1, .1, .1, .1};
        f.modelAvailableAt = 1000; f.featureAvailableAt = 1000; f.preparedAt = 2000; f.frozenAt = 3000; f.deadlineAt = 9000;
        return f;
    }
    @Test public void pitGuardAcceptsLegalFreeze() { assertTrue(PitGuardR30.check(legal()).isEmpty()); }
    @Test public void pitGuardRejectsLeakageAndShape() {
        PitGuardR30.Freeze f = legal(); f.trainedThroughOrdinal = 100; assertTrue(PitGuardR30.check(f).get(0).startsWith("MODEL_SEES_TARGET"));
        f = legal(); f.k3 = new int[] {1, 1, 9}; assertTrue(PitGuardR30.check(f).contains("K3_DUPLICATE:1"));
        f = legal(); f.frozenAt = 4500; assertTrue(PitGuardR30.check(f).get(0).startsWith("FREEZE_MARGIN_LT_5000"));
        f = legal(); f.probabilities[0] = .2; assertTrue(PitGuardR30.check(f).get(0).startsWith("PROB_SUM"));
        f = legal(); f.modelAvailableAt = 3001; assertTrue(PitGuardR30.check(f).contains("MODEL_AFTER_FREEZE"));
        f = legal(); f.probabilities = null; assertTrue("未校准允许null，不得伪造", PitGuardR30.check(f).isEmpty());
    }
}
