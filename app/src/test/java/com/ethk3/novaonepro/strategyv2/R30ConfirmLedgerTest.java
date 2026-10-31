package com.ethk3.novaonepro.strategyv2;

import org.junit.Test;
import static org.junit.Assert.*;

/** R30-B03-03: H_CONFIRM is read once per candidate and dataset hash; atom scoring never reads it. */
public final class R30ConfirmLedgerTest {
    @Test public void secondConfirmReadOfSameCandidateIsRejected() {
        HoldoutLedgerR27 l = new HoldoutLedgerR27();
        HoldoutLedgerR27.ConfirmRead r = l.confirm("P", "D1", 7, 100, 820);
        assertEquals("P", r.policyHash); assertTrue(l.confirmed("P", "D1", 7));
        try { l.confirm("P", "D1", 7, 100, 820); fail("reuse accepted"); }
        catch (IllegalStateException e) { assertTrue(e.getMessage().startsWith(HoldoutLedgerR27.CONFIRM_REUSED)); }
        assertEquals(1, l.confirmReuseRejected());
        l.confirm("P", "D2", 7, 100, 820);   // new dataset generation
        l.confirm("Q", "D1", 7, 100, 820);   // another candidate
        assertEquals(3, l.drainUnsavedConfirmReads().size());
        l.restoreConfirm("R", "D1", 7, 100, 820, 1L);
        try { l.confirm("R", "D1", 7, 100, 820); fail("restored read reopened"); } catch (IllegalStateException expected) { }
    }

    @Test public void atomScoreIgnoresConfirmRows() {
        int n = 1000, selectEnd = 700; double[][] v = new double[n][1]; boolean[][] present = new boolean[n][1]; int[] hit = new int[n];
        java.util.Random r = new java.util.Random(3);
        for (int t = 0; t < n; t++) { v[t][0] = r.nextDouble(); present[t][0] = true; hit[t] = r.nextInt(10) < 3 ? 1 : 0; }
        AtomScoreR27.Score a = AtomScoreR27.score(0, v, present, hit, selectEnd);
        for (int t = selectEnd; t < n; t++) { v[t][0] = 1 - v[t][0]; hit[t] = 1 - hit[t]; present[t][0] = t % 2 == 0; }
        AtomScoreR27.Score b = AtomScoreR27.score(0, v, present, hit, selectEnd);
        assertEquals(a.select.mean, b.select.mean, 0); assertEquals(a.nSelect, b.nSelect); assertEquals(a.state, b.state);
        assertEquals(0, a.nHoldout); assertEquals(selectEnd, a.nSelect);
    }

    @Test public void splitRolesAreFixedByTheDataset() {
        ReplayDatasetR27 d = ReplayDatasetR27.synthetic(3000, 2, new int[0]);
        SplitR27 a = SplitR27.of(d, null), b = SplitR27.of(ReplayDatasetR27.synthetic(3000, 2, new int[0]), null);
        assertEquals(a.describe(), b.describe());
        assertEquals(SplitR27.TRAIN, a.role(0)); assertEquals(SplitR27.SELECT, a.role(a.selectFrom));
        if (a.holdoutAvailable) assertEquals(SplitR27.CONFIRM, a.role(a.holdoutFrom));
    }
}
