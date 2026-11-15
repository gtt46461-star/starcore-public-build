package com.ethk3.novaonepro;

import com.ethk3.novaonepro.strategyv2.ReplayDatasetR27;
import com.ethk3.novaonepro.strategyv2.StatsR27;
import java.util.ArrayList;
import java.util.List;

/** R27-014/033/043 point-in-time replay of the 240 registered atoms (GenesisSchemaRegistryR915) through the existing
 * R922 semantic materializer (GenesisAtomicWorldR920). For row t only rows [t−HISTORY, t) are handed to the profile,
 * so no atom can see draw t. R30 materializes all ten positions for each period, with the same prior-history
 * top-three action at each position. The old rotating-position diagnostics remain explicitly versioned. */
public final class AtomReplayR27 {
    private AtomReplayR27() {}
    public static final int HISTORY = 480, MIN_ROWS = 24, ATOMS = 240;

    public static final class Frame {
        public final int from, to; public final double[][] values; public final boolean[][] present; public final int[] position, periodRow; public final int[][] k3; public final int[] hit, actual;
        Frame(int from, int to, int positions) { this.from = from; this.to = to; int n = Math.multiplyExact(to - from, positions); values = new double[n][]; present = new boolean[n][]; position = new int[n]; periodRow = new int[n]; k3 = new int[n][]; hit = new int[n]; actual = new int[n]; }
        public int rows() { return values.length; }
        public int periods() { return to - from; }
    }

    public static int[] eligible() {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < ATOMS; i++) if (GenesisSchemaRegistryR915.atoms().get(i).productionScoreEligible()) out.add(i);
        int[] a = new int[out.size()]; for (int i = 0; i < a.length; i++) a[i] = out.get(i); return a;
    }

    /** Atoms for rows [from,to). Rows with fewer than 24 prior rows are left null (missing). */
    public static Frame build(ReplayDatasetR27 d, int from, int to) {
        return build(d, from, to, true);
    }

    /** Preserves the existing atom-score and rotating-position model ABI; new neural training uses build(). */
    public static Frame buildLegacy(ReplayDatasetR27 d, int from, int to) { return build(d, from, to, false); }

    private static Frame build(ReplayDatasetR27 d, int from, int to, boolean ten) {
        if (from < 0 || to < from || to > d.size()) throw new IllegalArgumentException("R30_ATOM_REPLAY_RANGE");
        int positions = ten ? 10 : 1;
        Frame f = new Frame(from, to, positions);
        for (int t = from; t < to; t++) {
            for (int p = 0; p < positions; p++) { int i = (t-from)*positions+p; f.periodRow[i] = t; f.position[i] = ten ? p+1 : (int)Math.floorMod(d.ordinals[t],10L)+1; }
            int lo = Math.max(0, t - HISTORY);
            if (t - lo < MIN_ROWS) continue;
            List<Models.Draw> h = new ArrayList<>(t - lo);
            for (int s = lo; s < t; s++) h.add(draw(d, s));
            HistoricalStrategyInventionR908.Profile profile = HistoricalStrategyInventionR908.Profile.from(h);
            if (!profile.ready) continue;
            String historyHash = StatsR27.sha256(d.issues[lo] + "|" + d.issues[t - 1] + "|" + (t - lo));
            for (int p = 0; p < positions; p++) {
            int i = (t-from)*positions+p, pos = f.position[i];
            int[] k3 = top3(d, lo, t, pos);
            V51Models.PositionAction action = new V51Models.PositionAction(pos, k3[0], k3[1], k3[2], 0, 0.3, 0, 0, 0, 0, 0, new double[0], "R27_REPLAY_ROTATING_TOP3_120", 1, "");
            AtomCaptureR932.Values v = GenesisAtomicWorldR920.materializeObserved(profile, action, h.size(), historyHash, 0L);
            f.values[i] = v.values.clone(); f.present[i] = v.present.clone(); f.position[i] = pos; f.k3[i] = k3;
            int y = d.draws[t][pos - 1]; f.actual[i] = y; f.hit[i] = y == k3[0] || y == k3[1] || y == k3[2] ? 1 : 0;
            }
        }
        return f;
    }

    static Models.Draw draw(ReplayDatasetR27 d, int s) {
        Models.Draw m = new Models.Draw();
        m.issue = d.issues[s]; m.openTime = d.ordinals[s] * 60_000L; m.fetchTime = m.openTime + 5_000L; m.numbers = d.draws[s].clone(); m.dirty = 0;
        return m;
    }

    /** Three most frequent numbers at the position over rows [max(lo,t−120), t); ties → lower number. */
    static int[] top3(ReplayDatasetR27 d, int lo, int t, int pos) {
        int[] c = new int[11];
        for (int s = Math.max(lo, t - 120); s < t; s++) c[d.draws[s][pos - 1]]++;
        int[] out = new int[3]; boolean[] used = new boolean[11];
        for (int k = 0; k < 3; k++) { int best = -1; for (int n = 1; n <= 10; n++) if (!used[n] && (best < 0 || c[n] > c[best])) best = n; used[best] = true; out[k] = best; }
        java.util.Arrays.sort(out); return out;
    }

    /** The seven masked atom-group means (AtomicToolWorldR943.groupFeatures semantics) plus optionally the 165 raw atoms. */
    public static double[] features(double[] values, boolean[] present, boolean raw) {
        int[] el = eligible(); double[] masked = new double[ATOMS];
        for (int i = 0; i < ATOMS; i++) masked[i] = present != null && present[i] ? values[i] : -1;
        double[] g = AtomicToolWorldR943.groupFeatures(masked);
        if (!raw) return g;
        double[] out = new double[7 + el.length]; System.arraycopy(g, 0, out, 0, 7);
        for (int k = 0; k < el.length; k++) out[7 + k] = masked[el[k]] < 0 ? 0.5 : masked[el[k]];
        return out;
    }
}
