package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import org.junit.Test;

/** R30-B04-02 宿主测试。复制到 app/src/test/java/com/ethk3/novaonepro/strategyv2/。 */
public class R30BehaviorSignatureTest {
    @Test public void behaviorSignatureIgnoresNonActDetails() {
        boolean[] act = {true, false, true}; int[] pos = {1, 7, 2}, pos2 = {1, 3, 2};
        int[] m = {BehaviorSignatureR30.mask(1, 2, 3), 0, BehaviorSignatureR30.mask(4, 5, 10)};
        int[] m2 = {m[0], BehaviorSignatureR30.mask(6, 7, 8), m[2]};
        assertEquals(BehaviorSignatureR30.signature("ds1", act, pos, m), BehaviorSignatureR30.signature("ds1", act, pos2, m2));
        assertNotEquals(BehaviorSignatureR30.signature("ds1", act, pos, m), BehaviorSignatureR30.signature("ds2", act, pos, m));
        int[] m3 = {m[0], 0, BehaviorSignatureR30.mask(4, 5, 9)};
        assertNotEquals(BehaviorSignatureR30.signature("ds1", act, pos, m), BehaviorSignatureR30.signature("ds1", act, pos, m3));
    }
}
