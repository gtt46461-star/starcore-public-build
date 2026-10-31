package com.ethk3.novaonepro.strategyv2;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** R30-B03-04: features extended one row at a time equal a full recomputation bit for bit; a changed prefix is
 * detected by the 100-row hash check and the stream is rebuilt to the full-recomputation values. */
public final class R30IncrementalFeatureTest {
    static ReplayDatasetR27 prefix(ReplayDatasetR27 d, int n) {
        List<ReplayDatasetR27.Row> rows = new ArrayList<>(); for (int i = 0; i < n; i++) rows.add(new ReplayDatasetR27.Row(d.issues[i], d.ordinals[i], d.draws[i])); return ReplayDatasetR27.of(rows);
    }

    @Test public void incrementalEqualsFull() {
        ReplayDatasetR27 d = ReplayDatasetR27.synthetic(420, 11, new int[] {200});
        double[] full = new FeatureCacheR27(d).family(3, 12);
        FeatureCacheR27 prev = null; double[] inc = null;
        for (int n = 300; n <= d.size(); n++) {
            FeatureCacheR27 c = new FeatureCacheR27(prefix(d, n)); if (prev != null) c.extendFrom(prev); prev = c;
            inc = c.family(3, 12);
        }
        assertEquals(full.length, inc.length);
        for (int i = 0; i < full.length; i++) assertEquals("row " + i / 10, Double.doubleToLongBits(full[i]), Double.doubleToLongBits(inc[i]));
    }

    @Test public void changedPrefixIsRebuiltToFullValues() {
        ReplayDatasetR27 d = ReplayDatasetR27.synthetic(400, 12, new int[0]);
        FeatureCacheR27 a = new FeatureCacheR27(prefix(d, 250)); a.family(2, 12);
        int[][] changed = new int[d.size()][]; for (int i = 0; i < d.size(); i++) changed[i] = d.draws[i].clone();
        int[] tmp = changed[10]; changed[10] = changed[11]; changed[11] = tmp;  // rewrite an early row
        ReplayDatasetR27 e = d.withDraws(changed);
        long before = FeatureCacheR27.REBUILDS_R30.get();
        FeatureCacheR27 b = new FeatureCacheR27(e); b.extendFrom(a);
        double[] got = b.family(2, 12), want = new FeatureCacheR27(e).family(2, 12);
        assertTrue("rebuild recorded", FeatureCacheR27.REBUILDS_R30.get() > before);
        for (int i = 0; i < want.length; i++) assertEquals(Double.doubleToLongBits(want[i]), Double.doubleToLongBits(got[i]));
    }
}
