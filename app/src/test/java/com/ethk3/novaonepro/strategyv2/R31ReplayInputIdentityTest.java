package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import java.util.*;
import org.junit.Test;

/** R31-B06-C01#A1/#A2: a fixed 5000-row input keeps one dataset identity across restart (re-read of the same rows),
 * a rolling tail of the same window and incremental append; any revision of a row yields a new identity. */
public final class R31ReplayInputIdentityTest {
    private static List<ReplayDatasetR27.Row> rows(ReplayDatasetR27 d, int from, int to) {
        List<ReplayDatasetR27.Row> out = new ArrayList<>();
        for (int t = from; t < to; t++) out.add(new ReplayDatasetR27.Row(d.issues[t], d.ordinals[t], d.draws[t].clone()));
        return out;
    }
    @Test public void fixedFiveThousandRowIdentityIsStableAcrossRestartTailAndAppend() {
        ReplayDatasetR27 source = ReplayDatasetR27.synthetic(5100, 7311, null);
        ReplayDatasetR27 window = ReplayDatasetR27.of(rows(source, 100, 5100));
        ReplayDatasetR27 restarted = ReplayDatasetR27.of(rows(source, 100, 5100));
        assertEquals("re-read after restart of the same ordered rows", window.datasetHash, restarted.datasetHash);
        assertEquals(5000, window.size());
        ReplayDatasetR27 rolled = ReplayDatasetR27.of(rows(source, 0, 5100)).tail(5000);
        assertEquals("a rolling tail over the same 5000 rows has the same identity", window.datasetHash, rolled.datasetHash);
        ReplayDatasetR27 appended = ReplayDatasetR27.of(rows(source, 100, 5099)).append(new ReplayDatasetR27.Row(source.issues[5099], source.ordinals[5099], source.draws[5099].clone()));
        assertEquals("incremental append equals a full read", window.datasetHash, appended.datasetHash);
    }
    @Test public void anyRevisionOfOneRowIsANewIdentity() {
        ReplayDatasetR27 source = ReplayDatasetR27.synthetic(5000, 7312, null);
        List<ReplayDatasetR27.Row> revised = rows(source, 0, 5000);
        int[] corrected = revised.get(2500).numbers.clone(); int x = corrected[0]; corrected[0] = corrected[1]; corrected[1] = x;
        revised.set(2500, new ReplayDatasetR27.Row(revised.get(2500).issue, revised.get(2500).ordinal, corrected));
        assertNotEquals("a corrected historical row is a new input identity", source.datasetHash, ReplayDatasetR27.of(revised).datasetHash);
    }
}
