package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;

/** B04 宿主测试：I14 产物全部通过完整策略合同；窗口只读过去。 */
public class R30B04StructureTest {
    @Test public void i14ProposalsAreCompleteContracts() {
        SetIntersectionPolicyR30 fam = new SetIntersectionPolicyR30();
        StatsR27.SplitMix r = new StatsR27.SplitMix(20261031L);
        IdeaFamilyR27.Context ctx = new IdeaFamilyR27.Context();
        int ok = 0;
        for (int k = 0; k < 500; k++) {
            PolicySpecR27 s = fam.generate(r, ctx);
            List<String> v = PolicyContractR30.violations(s);
            if (v.isEmpty()) ok++; else fail("I14 产生空壳/非法策略：" + v + " " + s.canonical());
        }
        assertEquals(500, ok);
    }

    @Test public void i14MutationsStayComplete() {
        SetIntersectionPolicyR30 fam = new SetIntersectionPolicyR30();
        StatsR27.SplitMix r = new StatsR27.SplitMix(7L);
        IdeaFamilyR27.Context ctx = new IdeaFamilyR27.Context();
        PolicySpecR27 s = fam.generate(r, ctx);
        for (int k = 0; k < 200; k++) {
            PolicySpecR27 m = fam.mutate(s, r, ctx);
            if (m == null) continue;
            assertTrue(PolicyContractR30.violations(m).toString(), PolicyContractR30.violations(m).isEmpty());
            s = m;
        }
    }

    @Test public void windowReadsOnlyThePast() {
        int[][] d = new int[200][10];
        for (int t = 0; t < 200; t++) for (int p = 0; p < 10; p++) d[t][p] = (t + p) % 10 + 1;
        int[][] a = StructureWindowR30.counts(d, 150, 32);
        for (int p = 0; p < 10; p++) d[150][p] = 1; // 修改目标期本身
        int[][] b = StructureWindowR30.counts(d, 150, 32);
        for (int p = 0; p < 10; p++) for (int n = 1; n <= 10; n++) assertEquals(a[p][n], b[p][n]);
        assertEquals(3, StructureWindowR30.top3(a, 1).length);
    }

    @Test(expected = IllegalArgumentException.class) public void windowUnderflowRejected() {
        StructureWindowR30.counts(new int[10][10], 5, 8);
    }
}
