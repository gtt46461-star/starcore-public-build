package com.ethk3.novaonepro.strategyv2;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** R30-B03-02 import fixture: out-of-order rows, exact and contradicting duplicates, a gap and a day boundary. */
public final class R30HistoryGovernTest {
    static int[] perm(int shift) { int[] p = new int[10]; for (int i = 0; i < 10; i++) p[i] = (i + shift) % 10 + 1; return p; }
    static ReplayDatasetR27.Row row(String issue, int shift) { return new ReplayDatasetR27.Row(issue, ReplayDatasetR27.ordinalOf(issue), perm(shift)); }

    @Test public void governsOrderDuplicatesGapsAndDayBoundary() {
        List<ReplayDatasetR27.Row> in = new ArrayList<>();
        in.add(row("202610310002", 2));
        in.add(row("202610301440", 1));            // previous day, last issue
        in.add(row("202610310001", 3));            // first issue of the next day
        in.add(row("202610310001", 3));            // exact duplicate: kept once
        in.add(row("202610310005", 4));            // gap: 0003, 0004 missing
        in.add(row("202610310006", 5));
        in.add(row("202610310006", 6));            // contradicting duplicate: never imported
        in.add(new ReplayDatasetR27.Row("202610310007", 0, new int[] {1, 1, 2, 3, 4, 5, 6, 7, 8, 9})); // illegal permutation
        HistoryImportR27.Governed g = HistoryImportR27.govern(in);
        List<String> issues = new ArrayList<>(); for (ReplayDatasetR27.Row r : g.rows) issues.add(r.issue);
        assertEquals(java.util.Arrays.asList("202610301440", "202610310001", "202610310002", "202610310005"), issues);
        assertEquals(1, g.invalid);
        assertEquals(2, g.duplicates);
        assertEquals(1, g.duplicateConflicts);
        assertEquals(java.util.Collections.singletonList("202610310006"), g.conflictIssues);
        assertEquals(1, g.gaps);
        assertEquals(2, g.missingIssues);
    }

    @Test public void datasetHashIsANewGenerationWhenRowsChange() {
        ReplayDatasetR27 a = ReplayDatasetR27.synthetic(50, 1, new int[0]);
        ReplayDatasetR27 b = ReplayDatasetR27.synthetic(50, 1, new int[0]);
        assertEquals(a.datasetHash, b.datasetHash);
        assertNotEquals(a.datasetHash, a.tail(49).datasetHash);
    }
}
