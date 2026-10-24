package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.util.*;

/**
 * Live, incremental execution of R13 strategies (P06/P07): one call before each
 * draw (decide) and one after settlement (settle). Every strategy is computed and
 * settled every period whether it acts or not, its applicability learner keeps
 * training, and the shared models are updated once per draw.
 *
 * Contract: feeding draws 0..T-1 through Hub/ModelLiveSU2 produces exactly the
 * same act/hit/position/score sequence as ModelReplaySU2.evaluate on the same
 * draws (ModelLiveSU2Test, including a save/restore in the middle). History
 * training and live training are one code path.
 */
public final class ModelLiveSU2 {

    /** Shared online models, one instance per (kind, parameters), predicted once per period. */
    public static final class Hub {
        final LinkedHashMap<String, OnlineModelsSU2.Model> models = new LinkedHashMap<>();
        final Map<String, float[][]> p = new HashMap<>(); final Map<String, float[]> st = new HashMap<>();
        long t; boolean predicted;
        public OnlineModelsSU2.Model model(ModelGenomeSU2.Member m) {
            String k = m.modelKey();
            OnlineModelsSU2.Model x = models.get(k);
            if (x == null) {
                if (t != 0) throw new IllegalStateException("SU2_HUB_MODEL_BORN_LATE:" + k); // late members bootstrap by replay (ModelReplaySU2)
                x = ModelReplaySU2.build(m.kind, m.params); models.put(k, x);
            }
            return x;
        }
        /** A member first used after genesis is rebuilt by replaying the stored history (same code path). */
        public void adopt(ModelGenomeSU2.Member m, int[][] history) {
            String k = m.modelKey();
            if (models.containsKey(k)) return;
            if (history.length != t) throw new IllegalStateException("SU2_HUB_ADOPT_HISTORY_LENGTH");
            OnlineModelsSU2.Model x = ModelReplaySU2.build(m.kind, m.params);
            float[][] q = new float[10][10]; float[] s = new float[x.stateWidth()];
            for (int[] d : history) { x.predict(q, s); x.update(d); }
            models.put(k, x); predicted = false; p.clear(); st.clear();
        }
        public void predict() {
            if (predicted) return;
            for (Map.Entry<String, OnlineModelsSU2.Model> e : models.entrySet()) {
                float[][] q = new float[10][10]; float[] s = new float[e.getValue().stateWidth()];
                e.getValue().predict(q, s); p.put(e.getKey(), q); st.put(e.getKey(), s);
            }
            predicted = true;
        }
        public void update(int[] draw) { if (!predicted) predict(); for (OnlineModelsSU2.Model m : models.values()) m.update(draw); predicted = false; t++; }
        public void write(DataOutput o) throws IOException { o.writeLong(t); o.writeInt(models.size()); for (Map.Entry<String, OnlineModelsSU2.Model> e : models.entrySet()) { o.writeUTF(e.getKey()); e.getValue().write(o); } }
        public void readInto(DataInput in) throws IOException { long tt = in.readLong(); int n = in.readInt(); if (n != models.size()) throw new IOException("SU2_HUB_SHAPE"); for (int i = 0; i < n; i++) { String k = in.readUTF(); OnlineModelsSU2.Model m = models.get(k); if (m == null) throw new IOException("SU2_HUB_KEY"); m.read(in); } t = tt; predicted = false; }
    }

    public static final class Decision {
        public final long t; public final int position, mask; public final boolean act; public final double gscore, phat;
        public Decision(long t, int position, int mask, boolean act, double gscore, double phat) { this.t = t; this.position = position; this.mask = mask; this.act = act; this.gscore = gscore; this.phat = phat; }
    }

    final ModelGenomeSU2 g; Hub hub; final int startMeta, window, refit, M, F;
    final String[] keys; final double[] w; final double[] bmaLogw; final ModelOpsSU2.GatedState gated;
    long t;
    // rolling buffers (ring) for the applicability learner and the frequency gate
    final double[][] feat; final boolean[] lab; final double[] gsRing; final double[] c1Ring, c2Ring; double c1, c2;
    double ema12 = 0.3, ema48 = 0.3, ema200 = 0.3; double thr = Double.NaN; boolean thrValid;
    double[] wt, mu, sd; long last = Long.MIN_VALUE / 2;
    // pending decision for the current target
    int curP1 = -1; int[] curK3 = new int[3]; double[] curRow; double curGs;
    /** Pooled table of the current pending decision (read by MainlineAdapterSU2 to rank the 1200 K3 actions). */
    float[][] lastPooled; long lastPooledT = -1;

    public ModelLiveSU2(ModelGenomeSU2 g, Hub hub, int startMeta, int window, int refit) {
        this.g = g; this.hub = hub; this.startMeta = startMeta; this.window = window; this.refit = refit;
        M = g.members.size(); keys = new String[M]; w = new double[M];
        int stw = 0;
        for (int m = 0; m < M; m++) { keys[m] = g.members.get(m).modelKey(); w[m] = g.members.get(m).weight; stw += hub.model(g.members.get(m)).stateWidth(); }
        bmaLogw = new double[M * 10];
        gated = M > 1 && g.pool.equals("GATED") ? new ModelOpsSU2.GatedState(M, stw) : null;
        F = 4 + 4 + stw + 3;
        feat = new double[window][F]; lab = new boolean[window]; gsRing = new double[720];
        c1Ring = new double[721]; c2Ring = new double[721];
        t = hub.t;
        if (t != 0) throw new IllegalStateException("SU2_LIVE_START_AT_GENESIS_OR_RESTORE");
    }

    private float[][] pooled() {
        float[][] P = new float[10][10];
        if (M == 1) { float[][] q = hub.p.get(keys[0]); for (int p = 0; p < 10; p++) P[p] = q[p].clone(); }
        else if (gated != null) { P = gated.predict(memberTables(), memberStates()); }
        else if (g.pool.equals("BMA")) {
            for (int p = 0; p < 10; p++) {
                double mx = Double.NEGATIVE_INFINITY; for (int m = 0; m < M; m++) mx = Math.max(mx, bmaLogw[m * 10 + p]);
                double z = 0; double[] ww = new double[M]; for (int m = 0; m < M; m++) { ww[m] = Math.exp(bmaLogw[m * 10 + p] - mx); z += ww[m]; }
                for (int m = 0; m < M; m++) ww[m] /= z;
                for (int n = 0; n < 10; n++) { double v = 0; for (int m = 0; m < M; m++) v += ww[m] * (double) hub.p.get(keys[m])[p][n]; P[p][n] = (float) v; }
            }
        } else {
            for (int p = 0; p < 10; p++) {
                double[] L = new double[10]; double mx = Double.NEGATIVE_INFINITY;
                for (int n = 0; n < 10; n++) { double v = 0; for (int m = 0; m < M; m++) v += w[m] * Math.log(Math.max((double) hub.p.get(keys[m])[p][n], ModelOpsSU2.EPS)); L[n] = v; mx = Math.max(mx, v); }
                double s = 0; for (int n = 0; n < 10; n++) { L[n] = Math.exp(L[n] - mx); s += L[n]; }
                for (int n = 0; n < 10; n++) P[p][n] = (float) (L[n] / s);
            }
        }
        if (g.sink) P = ModelOpsSU2.sink(new float[][][]{P}, 20)[0];
        return P;
    }

    float[][][] memberTables() { float[][][] x = new float[M][][]; for (int m = 0; m < M; m++) x[m] = hub.p.get(keys[m]); return x; }
    float[][] memberStates() { float[][] x = new float[M][]; for (int m = 0; m < M; m++) x[m] = hub.st.get(keys[m]); return x; }

    /** Before draw t: compute the strategy, its gate score and whether it acts. Pure w.r.t. persisted state. */
    public Decision decide() {
        if (hub.t != t) throw new IllegalStateException("SU2_LIVE_HUB_DESYNC");
        hub.predict();
        float[][] P = pooled();
        lastPooled = P; lastPooledT = t;
        double[] mass = new double[10]; int[][] top = new int[10][3]; Integer[] ord = new Integer[10];
        for (int p = 0; p < 10; p++) {
            final float[] row = P[p]; for (int i = 0; i < 10; i++) ord[i] = i;
            Arrays.sort(ord, (a, b) -> Double.compare((double) row[b] - b * 1e-12, (double) row[a] - a * 1e-12));
            double s = 0; for (int i = 0; i < 3; i++) { top[p][i] = ord[i]; s += (double) row[ord[i]]; }
            mass[p] = Math.rint(s * 1e9) / 1e9;
        }
        int p1 = 0; for (int p = 1; p < 10; p++) if (mass[p] > mass[p1]) p1 = p;
        int p2 = p1 == 0 ? 1 : 0; for (int p = 0; p < 10; p++) if (p != p1 && mass[p] > mass[p2]) p2 = p;
        curP1 = p1; curK3 = top[p1].clone();
        double conf = mass[p1];
        double gs, phat;
        if (g.gate.equals("META")) {
            double[] row = new double[F]; int j = 0;
            double e = 0; for (int n = 0; n < 10; n++) { double v = P[p1][n]; e -= v * Math.log(v + 1e-12); }
            row[j++] = conf; row[j++] = mass[p1] - mass[p2]; row[j++] = e / Math.log(10); row[j++] = rollingZ(conf);
            for (int k : new int[]{5, 7}) {
                double s = 0, mn = 1e9;
                for (int m = 0; m < M; m++) {
                    final float[] r = hub.p.get(keys[m])[p1]; for (int i = 0; i < 10; i++) ord[i] = i;
                    Arrays.sort(ord, (a, b) -> Float.compare(r[b], r[a]));
                    int in = 0; for (int q = 0; q < 3; q++) for (int i = 0; i < k; i++) if (ord[i] == curK3[q]) { in++; break; }
                    double sup = in / 3.0; s += sup; mn = Math.min(mn, sup);
                }
                row[j++] = s / M; row[j++] = mn;
            }
            for (int m = 0; m < M; m++) for (float v : hub.st.get(keys[m])) row[j++] = v;
            row[j++] = ema12; row[j++] = ema48; row[j++] = ema200;
            for (int q = 0; q < F; q++) if (!Double.isFinite(row[q])) row[q] = 0.0;
            curRow = row;
            if (t >= startMeta && (wt == null || t - last >= refit)) refitLogistic();
            if (t >= startMeta && wt != null) {
                double z = wt[0]; for (int q = 0; q < F; q++) z += (row[q] - mu[q]) / sd[q] * wt[q + 1];
                z = Math.max(-30, Math.min(30, z)); phat = 1.0 / (1.0 + Math.exp(-z)); gs = phat;
            } else { phat = Double.NaN; gs = -1.0; }
        } else { curRow = null; gs = conf; phat = conf; }
        curGs = gs;
        boolean act = false;
        if (t >= startMeta) {
            if (g.coverage >= .999) act = true;
            else {
                if ((t - startMeta) % 50 == 0) blockThreshold();
                act = thrValid && gs >= thr && gs > -1;
            }
        }
        int mask = (1 << curK3[0]) | (1 << curK3[1]) | (1 << curK3[2]);
        return new Decision(t, p1 + 1, mask, act, gs, phat);
    }

    private double rollingZ(double v) {
        // window [t-720, t) through cumulative sums, exactly like the replay (cumsum differences)
        long lo = Math.max(t - 720, 0); int n = (int) Math.max(t - lo, 1);
        double clo = c1Ring[(int) (lo % 721)], c2lo = c2Ring[(int) (lo % 721)];
        double m = (c1 - clo) / n, var = Math.max((c2 - c2lo) / n - m * m, 1e-12);
        double s = (v - m) / Math.sqrt(var);
        return t < 30 ? 0 : Math.max(-6, Math.min(6, s));
    }

    private void refitLogistic() {
        long lo = Math.max(0, t - window); int n = (int) (t - lo);
        if (n < 400) return;
        double[] m = new double[F], s = new double[F];
        for (int j = 0; j < F; j++) { double a = 0; for (long i = lo; i < t; i++) a += feat[(int) (i % window)][j]; m[j] = a / n; double v = 0; for (long i = lo; i < t; i++) { double q = feat[(int) (i % window)][j] - m[j]; v += q * q; } s[j] = Math.sqrt(v / n) + 1e-9; }
        double[][] Z = new double[n][F]; double[] y = new double[n];
        for (int i = 0; i < n; i++) { double[] r = feat[(int) ((lo + i) % window)]; for (int j = 0; j < F; j++) Z[i][j] = (r[j] - m[j]) / s[j]; y[i] = lab[(int) ((lo + i) % window)] ? 1 : 0; }
        wt = LogisticRidgeSU2.fit(Z, y, n, g.metaC, 12); mu = m; sd = s; last = t;
    }

    private void blockThreshold() {
        long lo = Math.max(g.gate.equals("META") ? startMeta : startMeta - 720, t - 720);
        double[] hist = new double[(int) (t - lo)]; int n = 0;
        for (long u = lo; u < t; u++) { double v = gsRing[(int) (u % 720)]; if (v > -1) hist[n++] = v; }
        if (n < 100) { thrValid = false; return; }
        thr = ModelReplaySU2.quantileLinear(Arrays.copyOf(hist, n), 1 - g.coverage); thrValid = true;
    }

    public boolean hasPending() { return curP1 >= 0; }

    /** After draw t is settled: label, feedback, gate history; then the hub updates the shared models. */
    public boolean settle(int[] draw) {
        if (curP1 < 0 || hub.t != t) throw new IllegalStateException("SU2_LIVE_SETTLE_WITHOUT_DECISION");
        OnlineModelsSU2.requireDraw(draw);
        int x = draw[curP1] - 1;
        boolean hit = curK3[0] == x || curK3[1] == x || curK3[2] == x;
        if (g.gate.equals("META")) { feat[(int) (t % window)] = curRow; lab[(int) (t % window)] = hit; }
        else lab[(int) (t % window)] = hit;
        gsRing[(int) (t % 720)] = curGs;
        // cumulative sums for the confidence rolling z (conf = mass at the chosen position)
        double conf = g.gate.equals("META") ? curRow[0] : curGs;
        c1Ring[(int) (t % 721)] = c1; c2Ring[(int) (t % 721)] = c2;
        c1 += conf; c2 += conf * conf;
        c1Ring[(int) ((t + 1) % 721)] = c1; c2Ring[(int) ((t + 1) % 721)] = c2;
        double a12 = 1.0 - Math.pow(0.5, 1.0 / 12), a48 = 1.0 - Math.pow(0.5, 1.0 / 48), a200 = 1.0 - Math.pow(0.5, 1.0 / 200), h = hit ? 1.0 : 0.0;
        ema12 = (1 - a12) * ema12 + a12 * h; ema48 = (1 - a48) * ema48 + a48 * h; ema200 = (1 - a200) * ema200 + a200 * h;
        if (gated != null) gated.update(memberTables(), memberStates(), draw, 0.03, 1e-3);
        if (M > 1 && g.pool.equals("BMA")) {
            for (int p = 0; p < 10; p++) {
                int xx = draw[p] - 1;
                for (int m = 0; m < M; m++) bmaLogw[m * 10 + p] = 0.995 * bmaLogw[m * 10 + p] + Math.log(Math.max((double) hub.p.get(keys[m])[p][xx], ModelOpsSU2.EPS));
                double mx = Double.NEGATIVE_INFINITY; for (int m = 0; m < M; m++) mx = Math.max(mx, bmaLogw[m * 10 + p]);
                double z = 0; double[] ww = new double[M]; for (int m = 0; m < M; m++) { ww[m] = Math.exp(bmaLogw[m * 10 + p] - mx); z += ww[m]; }
                for (int m = 0; m < M; m++) { ww[m] = (1 - 0.02) * (ww[m] / z) + 0.02 / M; bmaLogw[m * 10 + p] = Math.log(ww[m]); }
            }
        }
        curP1 = -1; t++;
        return hit;
    }

    /** A strategy born after genesis: replay the stored history through a private hub, then join the shared hub. */
    public static ModelLiveSU2 bootstrap(ModelGenomeSU2 g, Hub shared, int[][] history, int startMeta, int window, int refit) {
        for (ModelGenomeSU2.Member m : g.members) shared.adopt(m, history);
        Hub own = new Hub();
        ModelLiveSU2 live = new ModelLiveSU2(g, own, startMeta, window, refit);
        for (int[] d : history) { live.decide(); live.settle(d); own.update(d); }
        if (own.t != shared.t) throw new IllegalStateException("SU2_BOOTSTRAP_DESYNC");
        live.hub = shared;
        return live;
    }

    public void write(DataOutput o) throws IOException {
        o.writeUTF(g.key()); o.writeLong(t);
        for (double v : bmaLogw) o.writeDouble(v);
        for (double[] r : feat) for (double v : r) o.writeDouble(v);
        for (boolean b : lab) o.writeBoolean(b);
        for (double v : gsRing) o.writeDouble(v);
        for (double v : c1Ring) o.writeDouble(v); for (double v : c2Ring) o.writeDouble(v); o.writeDouble(c1); o.writeDouble(c2);
        o.writeDouble(ema12); o.writeDouble(ema48); o.writeDouble(ema200); o.writeDouble(thr); o.writeBoolean(thrValid);
        o.writeBoolean(wt != null); if (wt != null) { for (double v : wt) o.writeDouble(v); for (double v : mu) o.writeDouble(v); for (double v : sd) o.writeDouble(v); }
        o.writeLong(last);
        if (gated != null) gated.write(o);
    }

    public void readInto(DataInput in) throws IOException {
        if (!g.key().equals(in.readUTF())) throw new IOException("SU2_LIVE_STATE_GENOME");
        t = in.readLong();
        for (int i = 0; i < bmaLogw.length; i++) bmaLogw[i] = in.readDouble();
        for (double[] r : feat) for (int j = 0; j < F; j++) r[j] = in.readDouble();
        for (int i = 0; i < lab.length; i++) lab[i] = in.readBoolean();
        for (int i = 0; i < gsRing.length; i++) gsRing[i] = in.readDouble();
        for (int i = 0; i < c1Ring.length; i++) c1Ring[i] = in.readDouble(); for (int i = 0; i < c2Ring.length; i++) c2Ring[i] = in.readDouble(); c1 = in.readDouble(); c2 = in.readDouble();
        ema12 = in.readDouble(); ema48 = in.readDouble(); ema200 = in.readDouble(); thr = in.readDouble(); thrValid = in.readBoolean();
        if (in.readBoolean()) { wt = new double[F + 1]; mu = new double[F]; sd = new double[F]; for (int i = 0; i <= F; i++) wt[i] = in.readDouble(); for (int i = 0; i < F; i++) mu[i] = in.readDouble(); for (int i = 0; i < F; i++) sd[i] = in.readDouble(); } else wt = null;
        last = in.readLong(); curP1 = -1;
        if (gated != null) gated.read(in);
    }
}
