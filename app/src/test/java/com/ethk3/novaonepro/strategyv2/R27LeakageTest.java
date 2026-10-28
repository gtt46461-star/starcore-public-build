package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;
import com.ethk3.novaonepro.AtomReplayR27;
import org.junit.Test;

/** H01 / R27-014: point-in-time leakage test. For every leaf kind (including every family leaf) and for all 240
 * registered atoms, replacing draw t and everything after it by random permutations must not change any value at
 * rows ≤ t. */
public class R27LeakageTest {
    static ReplayDatasetR27 perturbFrom(ReplayDatasetR27 d, int t, long seed) {
        StatsR27.SplitMix r = new StatsR27.SplitMix(seed); int[][] x = new int[d.size()][];
        for (int i = 0; i < d.size(); i++) x[i] = i < t ? d.draws[i] : ReplayDatasetR27.randomPermutation(r);
        return d.withDraws(x);
    }

    @Test public void everyLeafKindIsPointInTime() throws Exception {
        ReplayDatasetR27 d = R27TestSupport.csv(900);
        java.util.List<PolicyAstR27.Node> leaves = new java.util.ArrayList<>();
        for (String k : PolicyAstR27.LEAVES.keySet()) {
            if (k.equals("family")) { for (int f : PolicyAstR27.FAMILY_LEAVES) leaves.add(PolicyAstR27.leaf("family", f)); continue; }
            leaves.add(OperatorGoldenR27.sample(k));
        }
        int checks = 0;
        for (PolicyAstR27.Node leaf : leaves) for (int tp : new int[] {1, 7}) {
            double[] base = PolicyAstR27.eval(leaf, new FeatureCacheR27(d, 8), tp);
            for (int t : new int[] {300, 650, 899}) {
                double[] v = PolicyAstR27.eval(leaf, new FeatureCacheR27(perturbFrom(d, t, t * 31L + tp), 8), tp);
                for (int i = 0; i <= t * 10 + 9; i++) assertEquals(leaf + "@tp" + tp + " row " + i / 10 + " (perturbed from " + t + ")", Double.doubleToLongBits(base[i]), Double.doubleToLongBits(v[i]));
                checks++;
            }
        }
        R27TestSupport.log("H01_LEAVES_PASS leaves=" + leaves.size() + " checks=" + checks);
    }

    @Test public void all240AtomsArePointInTime() throws Exception {
        ReplayDatasetR27 d = R27TestSupport.csv(700);
        int atoms = 0;
        for (int t : new int[] {120, 480, 699}) {
            AtomReplayR27.Frame base = AtomReplayR27.build(d, t, t + 1), pert = AtomReplayR27.build(perturbFrom(d, t, 99 + t), t, t + 1);
            assertNotNull("atoms materialized at row " + t, base.values[0]);
            assertEquals(240, base.values[0].length);
            for (int a = 0; a < 240; a++) {
                assertEquals("atom " + a + " row " + t, Double.doubleToLongBits(base.values[0][a]), Double.doubleToLongBits(pert.values[0][a]));
                assertEquals("atom presence " + a + " row " + t, base.present[0][a], pert.present[0][a]);
            }
            atoms = 240;
        }
        R27TestSupport.log("H01_ATOMS_PASS atoms=" + atoms + " rows=3");
    }
}
