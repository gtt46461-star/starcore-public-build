package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.util.*;

/**
 * Live, incremental committee (P37): every period each member program computes its (calibrated) pooled table,
 * the tables are fused by geometric mean, the best position's top-3 is the decision and the confidence gate at
 * the tier frequency decides whether to act (never on a chance-level table). Identical to
 * CommitteeSU2.evaluate on the same draws (CommitteeLiveSU2Test, including save/restore and late birth).
 */
public final class CommitteeLiveSU2 implements LiveStrategySU2 {
    final CommitteeSU2 c; final ModelLiveSU2[] subs; final int startMeta; long t;
    final double[] gsRing = new double[720]; double thr = Double.NaN; boolean thrValid;
    int curP1 = -1; int[] curK3 = new int[3]; double curGs; float[][] lastPooled; long lastPooledT = -1;

    public CommitteeLiveSU2(CommitteeSU2 c, ModelLiveSU2.Hub hub, int startMeta, int window, int refit) {
        this.c = c; this.startMeta = startMeta; subs = new ModelLiveSU2[c.subs.size()];
        for (int k = 0; k < subs.length; k++) subs[k] = new ModelLiveSU2(c.subs.get(k), hub, startMeta, window, refit);
        t = hub.t;
    }

    public String key() { return c.key(); }
    public boolean hasPending() { return curP1 >= 0; }
    public float[][] pendingTable(long tt) { return lastPooledT == tt ? lastPooled : null; }

    public ModelLiveSU2.Decision decide() {
        float[][][] tabs = new float[subs.length][][];
        for (int k = 0; k < subs.length; k++) { subs[k].decide(); tabs[k] = subs[k].lastPooled; }
        float[][] P = CommitteeSU2.fuse(tabs);
        lastPooled = P; lastPooledT = t;
        double[] mass = new double[10]; int[][] top = new int[10][3]; Integer[] ord = new Integer[10];
        for (int p = 0; p < 10; p++) {
            final float[] row = P[p]; for (int i = 0; i < 10; i++) ord[i] = i;
            Arrays.sort(ord, (a, b) -> Double.compare((double) row[b] - b * 1e-12, (double) row[a] - a * 1e-12));
            double s = 0; for (int i = 0; i < 3; i++) { top[p][i] = ord[i]; s += (double) row[ord[i]]; }
            mass[p] = Math.rint(s * 1e9) / 1e9;
        }
        int p1 = 0; for (int p = 1; p < 10; p++) if (mass[p] > mass[p1]) p1 = p;
        curP1 = p1; curK3 = top[p1].clone(); curGs = mass[p1];
        boolean act = false;
        if (t >= startMeta) {
            if (c.coverage >= .999) act = curGs > ModelReplaySU2.INFORMATIVE;
            else {
                if ((t - startMeta) % 50 == 0) blockThreshold();
                act = thrValid && curGs >= thr && curGs > ModelReplaySU2.INFORMATIVE;
            }
        }
        return new ModelLiveSU2.Decision(t, p1 + 1, (1 << curK3[0]) | (1 << curK3[1]) | (1 << curK3[2]), act, curGs, curGs);
    }

    private void blockThreshold() {
        long lo = Math.max(startMeta - 720, t - 720); int n = (int) (t - lo);
        if (n < 100) { thrValid = false; return; }
        double[] hist = new double[n]; for (int i = 0; i < n; i++) hist[i] = gsRing[(int) ((lo + i) % 720)];
        thr = ModelReplaySU2.quantileLinear(hist, 1 - c.coverage); thrValid = true;
    }

    public boolean settle(int[] draw) {
        if (curP1 < 0) throw new IllegalStateException("SU2_COMMITTEE_SETTLE_WITHOUT_DECISION");
        for (ModelLiveSU2 s : subs) s.settle(draw);
        int x = draw[curP1] - 1; boolean hit = curK3[0] == x || curK3[1] == x || curK3[2] == x;
        gsRing[(int) (t % 720)] = curGs; curP1 = -1; t++;
        return hit;
    }

    /** A committee born after genesis: replay the stored history through a private hub, then join the shared hub. */
    public static CommitteeLiveSU2 bootstrap(CommitteeSU2 c, ModelLiveSU2.Hub shared, int[][] history, int startMeta, int window, int refit) {
        for (ModelGenomeSU2 g : c.subs) for (ModelGenomeSU2.Member m : g.members) { shared.adopt(m, history); if (g.calibrated) shared.adoptCalibrated(m, history); }
        ModelLiveSU2.Hub own = new ModelLiveSU2.Hub();
        CommitteeLiveSU2 live = new CommitteeLiveSU2(c, own, startMeta, window, refit);
        for (int[] d : history) { live.decide(); live.settle(d); own.update(d); }
        if (own.t != shared.t) throw new IllegalStateException("SU2_BOOTSTRAP_DESYNC");
        for (ModelLiveSU2 s : live.subs) s.hub = shared;
        return live;
    }

    public void write(DataOutput o) throws IOException {
        o.writeUTF(c.key()); o.writeLong(t); for (double v : gsRing) o.writeDouble(v); o.writeDouble(thr); o.writeBoolean(thrValid);
        for (ModelLiveSU2 s : subs) s.write(o);
    }

    public void readInto(DataInput in) throws IOException {
        if (!c.key().equals(in.readUTF())) throw new IOException("SU2_COMMITTEE_STATE_KEY");
        t = in.readLong(); for (int i = 0; i < gsRing.length; i++) gsRing[i] = in.readDouble(); thr = in.readDouble(); thrValid = in.readBoolean();
        for (ModelLiveSU2 s : subs) s.readInto(in); curP1 = -1;
    }
}
