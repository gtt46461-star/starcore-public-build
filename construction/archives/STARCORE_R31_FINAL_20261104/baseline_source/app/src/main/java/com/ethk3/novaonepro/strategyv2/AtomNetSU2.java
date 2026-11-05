package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.util.*;

/**
 * R14b (P39) ATOMNET member: the atom world as a learned basis (reference/replay/atomnet.py, fixture r14b_atomnet).
 *
 *   z_t[p,n] = b[p,n] + sum_f A_t[p,n,f] (u[f] + v[p,f]) + sum_r [n == map_r(D[t-l_r, p+d_r])] w[p,r]
 *   P_t[p,.] = softmax(z_t[p,.])
 *
 * A = chosen registry atoms, atom x atom crossings and (optionally) the odd/even + big/small group atoms;
 * r = cross-position relations (lag l, source offset d, map SAME/PLUS1/MINUS1/MIRROR). Weights learn online from
 * settled draws (RMS-normalised steps with L2, accumulator decay gd). Parameters (member text):
 *   atoms=3-7-12, pairs=none|3x7-..., lags=none|1-2, group=0|1, lr, l2, gd
 * The learned weights are the per-atom CREDIT that evolution feeds back to the children (credit()).
 */
public final class AtomNetSU2 implements TemporalModelR26 {
    final int[] atoms; final int[][] pairs, mols; final int[] lags; final boolean group; final double lr, l2, gd;
    final int Fd, Fr, maxlag;
    final double[] b = new double[100], Gb = new double[100];
    final double[] u, Gu, v, Gv, w, Gw;
    final AtomStreamSU2 stream = new AtomStreamSU2();
    // features of the current target (cached so update() after predict() reuses them)
    long featT = -1; double[] X; int[] ridx; final double[] pr = new double[100];

    public AtomNetSU2(int[] atoms, int[][] pairs, int[] lags, boolean group, double lr, double l2, double gd) { this(atoms, pairs, new int[0][], lags, group, lr, l2, gd); }

    /** R15: mols = learned molecules (atom i x registry state atom j), see atomnet.AtomSource.mol. */
    public AtomNetSU2(int[] atoms, int[][] pairs, int[][] mols, int[] lags, boolean group, double lr, double l2, double gd) {
        for (int[] m : mols) if (m[0] < 0 || m[0] >= AtomStreamSU2.size() || m[1] < 0 || m[1] >= AtomStreamSU2.states()) throw new IllegalArgumentException("SU2_ATOMNET_MOLECULE");
        this.mols = mols;
        for (int a : atoms) if (a < 0 || a >= AtomStreamSU2.size()) throw new IllegalArgumentException("SU2_ATOMNET_ATOM");
        for (int[] q : pairs) if (q[0] < 0 || q[1] < 0 || q[0] >= AtomStreamSU2.size() || q[1] >= AtomStreamSU2.size()) throw new IllegalArgumentException("SU2_ATOMNET_PAIR");
        int ml = 0; for (int l : lags) { if (l < 1 || l > 5) throw new IllegalArgumentException("SU2_ATOMNET_LAG"); ml = Math.max(ml, l); }
        if (!(lr > 0 && lr <= 1 && l2 >= 0 && l2 <= 0.1 && gd > 0.9 && gd <= 1.0)) throw new IllegalArgumentException("SU2_ATOMNET_RATE");
        if (atoms.length + pairs.length + mols.length > 40) throw new IllegalArgumentException("SU2_ATOMNET_SIZE");
        this.atoms = atoms.clone(); this.pairs = pairs; this.lags = lags.clone(); this.group = group; this.lr = lr; this.l2 = l2; this.gd = gd;
        Fd = atoms.length + pairs.length + mols.length + (group ? AtomStreamSU2.N_GROUP : 0); Fr = lags.length * 40; maxlag = ml;
        u = new double[Fd]; Gu = new double[Fd]; v = new double[10 * Fd]; Gv = new double[10 * Fd]; w = new double[10 * Fr]; Gw = new double[10 * Fr];
        Arrays.fill(Gb, 1e-3); Arrays.fill(Gu, 1e-3); Arrays.fill(Gv, 1e-3); Arrays.fill(Gw, 1e-3);
    }

    static int[] ints(String s) {
        if (s == null || s.equals("none") || s.isEmpty()) return new int[0];
        String[] f = s.split("-"); int[] out = new int[f.length]; for (int i = 0; i < f.length; i++) out[i] = Integer.parseInt(f[i]); return out;
    }
    static int[][] molList(String s) {
        if (s == null || s.equals("none") || s.isEmpty()) return new int[0][];
        String[] f = s.split("-"); int[][] out = new int[f.length][2];
        for (int i = 0; i < f.length; i++) { String[] a = f[i].split("@"); out[i][0] = Integer.parseInt(a[0]); out[i][1] = Integer.parseInt(a[1]); }
        return out;
    }
    static int[][] pairList(String s) {
        if (s == null || s.equals("none") || s.isEmpty()) return new int[0][];
        String[] f = s.split("-"); int[][] out = new int[f.length][2];
        for (int i = 0; i < f.length; i++) { String[] a = f[i].split("x"); out[i][0] = Integer.parseInt(a[0]); out[i][1] = Integer.parseInt(a[1]); }
        return out;
    }

    /** Builds from member parameters (ModelReplaySU2.build). */
    public static AtomNetSU2 of(Map<String, String> p) {
        return new AtomNetSU2(ints(p.get("atoms")), pairList(p.get("pairs")), molList(p.get("mols")), ints(p.get("lags")), "1".equals(p.get("group")),
            Double.parseDouble(p.get("lr")), Double.parseDouble(p.get("l2")), Double.parseDouble(p.get("gd")));
    }

    public void resetTemporalContext(){try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();new AtomStreamSU2().write(new DataOutputStream(bytes));stream.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));}catch(IOException impossible){throw new IllegalStateException("R26_ATOM_CONTEXT_RESET",impossible);}featT=-1;X=null;ridx=null;Arrays.fill(pr,0);}
    public int stateWidth() { return 3; }

    private void features() {
        long t = stream.time();
        if (featT == t) return;
        X = new double[100 * Fd];
        float[][][] cols = new float[atoms.length][][];
        int f = 0;
        Map<Integer, float[][]> got = new HashMap<>();
        for (int a : atoms) { float[][] A = got.computeIfAbsent(a, stream::atom); for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) X[(p * 10 + n) * Fd + f] = A[p][n]; f++; }
        for (int[] q : pairs) {
            float[][] A = AtomStreamSU2.pair(got.computeIfAbsent(q[0], stream::atom), got.computeIfAbsent(q[1], stream::atom));
            for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) X[(p * 10 + n) * Fd + f] = A[p][n];
            f++;
        }
        for (int[] m : mols) {
            float[][] A = got.computeIfAbsent(m[0], stream::atom); float[] st = stream.state(m[1]);
            for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) X[(p * 10 + n) * Fd + f] = A[p][n] * st[p];
            f++;
        }
        if (group) {
            float[][][] G = stream.group();
            for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) for (int k = 0; k < AtomStreamSU2.N_GROUP; k++) X[(p * 10 + n) * Fd + f + k] = G[p][n][k];
        }
        ridx = new int[10 * Fr];
        int r = 0;
        for (int l : lags) for (int d = 0; d < 10; d++) for (int m = 0; m < 4; m++) {ExecutionBudgetR26.poll();
            for (int p = 0; p < 10; p++) {
                int src = stream.lastDraw((p + d) % 10, l);
                ridx[p * Fr + r] = src < 0 ? -1 : (m == 0 ? src : m == 1 ? (src + 1) % 10 : m == 2 ? (src + 9) % 10 : 9 - src);
            }
            r++;
        }
        featT = t;
        probs();
    }

    private boolean anyRel() { for (int i = 0; i < ridx.length; i++) if (ridx[i] >= 0) return true; return false; }

    private void probs() {
        double[] tmp = new double[Fd];
        double[] z = new double[100];
        for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) {
            double s = 0.0;
            if (Fd > 0) { int base = (p * 10 + n) * Fd; for (int f = 0; f < Fd; f++) tmp[f] = X[base + f] * (u[f] + v[p * Fd + f]); s = ModelOpsSU2.pairwise(tmp, 0, Fd); }
            z[p * 10 + n] = b[p * 10 + n] + s;
        }
        if (Fr > 0 && anyRel()) {
            double[] acc = new double[100];
            for (int p = 0; p < 10; p++) for (int r = 0; r < Fr; r++) { int i = ridx[p * Fr + r]; boolean ok = i >= 0; acc[p * 10 + (ok ? i : 0)] += w[p * Fr + r] * (ok ? 1.0 : 0.0); }
            for (int i = 0; i < 100; i++) z[i] += acc[i];
        }
        double[] e = new double[10];
        for (int p = 0; p < 10; p++) {
            double m = Double.NEGATIVE_INFINITY; for (int n = 0; n < 10; n++) m = Math.max(m, z[p * 10 + n]);
            for (int n = 0; n < 10; n++) e[n] = Math.exp(z[p * 10 + n] - m);
            double sum = ModelOpsSU2.pairwise(e, 0, 10);
            for (int n = 0; n < 10; n++) pr[p * 10 + n] = e[n] / sum;
        }
    }

    public void predict(float[][] out, float[] st) {
        features();
        double[] best = new double[10], ent = new double[10], row = new double[10];
        for (int p = 0; p < 10; p++) {
            double bm = 0; for (int n = 0; n < 10; n++) { double q = pr[p * 10 + n]; out[p][n] = (float) q; bm = Math.max(bm, q); row[n] = q * Math.log(q); }
            best[p] = bm; ent[p] = -ModelOpsSU2.pairwise(row, 0, 10);
        }
        st[0] = (float) (ModelOpsSU2.pairwise(best, 0, 10) / 10);
        if (Fd > 0) {
            double[] au = new double[Fd], av = new double[Fd];
            for (int f = 0; f < Fd; f++) { au[f] = Math.abs(u[f]); double s = Math.abs(v[f]); for (int p = 1; p < 10; p++) s = s + Math.abs(v[p * Fd + f]); av[f] = s / 10; }
            st[1] = (float) (ModelOpsSU2.pairwise(au, 0, Fd) + ModelOpsSU2.pairwise(av, 0, Fd));
        } else st[1] = 0f;
        st[2] = (float) (ModelOpsSU2.pairwise(ent, 0, 10) / 10 / Math.log(10));
    }

    public void update(int[] draw) {
        OnlineModelsSU2.requireDraw(draw);
        features();
        double[] g = new double[100];
        for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) g[p * 10 + n] = pr[p * 10 + n] - (n == draw[p] - 1 ? 1.0 : 0.0);
        for (int i = 0; i < 100; i++) { double gb = g[i] + l2 * b[i]; Gb[i] = gd * Gb[i] + gb * gb; b[i] -= lr * gb / Math.sqrt(Gb[i]); }
        if (Fd > 0) {
            double[] gv = new double[10 * Fd];
            for (int p = 0; p < 10; p++) for (int f = 0; f < Fd; f++) {
                double s = g[p * 10] * X[(p * 10) * Fd + f];
                for (int n = 1; n < 10; n++) s = s + g[p * 10 + n] * X[(p * 10 + n) * Fd + f];
                gv[p * Fd + f] = s;
            }
            double[] gu = new double[Fd];
            for (int f = 0; f < Fd; f++) { double s = gv[f]; for (int p = 1; p < 10; p++) s = s + gv[p * Fd + f]; gu[f] = s + l2 * u[f]; }
            double l2v = 10 * l2;
            for (int i = 0; i < 10 * Fd; i++) gv[i] = gv[i] + l2v * v[i];
            for (int f = 0; f < Fd; f++) { Gu[f] = gd * Gu[f] + gu[f] * gu[f]; u[f] -= lr * gu[f] / Math.sqrt(Gu[f]); }
            for (int i = 0; i < 10 * Fd; i++) { Gv[i] = gd * Gv[i] + gv[i] * gv[i]; v[i] -= lr * gv[i] / Math.sqrt(Gv[i]); }
        }
        if (Fr > 0 && anyRel()) {
            for (int p = 0; p < 10; p++) for (int r = 0; r < Fr; r++) {
                int k = p * Fr + r, i = ridx[k]; boolean ok = i >= 0;
                double gw = (ok ? g[p * 10 + i] : 0.0) + l2 * w[k];
                if (ok) { Gw[k] = gd * Gw[k] + gw * gw; w[k] -= lr * gw / Math.sqrt(Gw[k]); }
            }
        }
        stream.update(draw);
    }

    /** Learned credit per atom (|u| + mean_p |v|), group (mean over group atoms), relations (max |w|). */
    public Map<String, Double> credit() {
        Map<String, Double> out = new LinkedHashMap<>();
        for (int i = 0; i < atoms.length; i++) { double s = Math.abs(v[i]); for (int p = 1; p < 10; p++) s = s + Math.abs(v[p * Fd + i]); out.put("A" + atoms[i], Math.abs(u[i]) + s / 10); }
        for (int i = 0; i < pairs.length; i++) { int f = atoms.length + i; double s = Math.abs(v[f]); for (int p = 1; p < 10; p++) s = s + Math.abs(v[p * Fd + f]); out.put("P" + pairs[i][0] + "x" + pairs[i][1], Math.abs(u[f]) + s / 10); }
        for (int i = 0; i < mols.length; i++) { int f = atoms.length + pairs.length + i; double s = Math.abs(v[f]); for (int p = 1; p < 10; p++) s = s + Math.abs(v[p * Fd + f]); out.put("M" + mols[i][0] + "@" + mols[i][1], Math.abs(u[f]) + s / 10); }
        if (group) { double tot = 0; int g0 = atoms.length + pairs.length + mols.length; for (int k = 0; k < AtomStreamSU2.N_GROUP; k++) { int f = g0 + k; double s = Math.abs(v[f]); for (int p = 1; p < 10; p++) s = s + Math.abs(v[p * Fd + f]); tot += Math.abs(u[f]) + s / 10; } out.put("GROUP", tot / AtomStreamSU2.N_GROUP); }
        if (Fr > 0) { double m = 0; for (double x : w) m = Math.max(m, Math.abs(x)); out.put("REL", m); }
        return out;
    }

    public void write(DataOutput o) throws IOException {
        OnlineModelsSU2.writeArr(o, b); OnlineModelsSU2.writeArr(o, Gb); OnlineModelsSU2.writeArr(o, u); OnlineModelsSU2.writeArr(o, Gu);
        OnlineModelsSU2.writeArr(o, v); OnlineModelsSU2.writeArr(o, Gv); OnlineModelsSU2.writeArr(o, w); OnlineModelsSU2.writeArr(o, Gw);
        stream.write(o);
    }
    public void read(DataInput in) throws IOException {
        OnlineModelsSU2.readArr(in, b); OnlineModelsSU2.readArr(in, Gb); OnlineModelsSU2.readArr(in, u); OnlineModelsSU2.readArr(in, Gu);
        OnlineModelsSU2.readArr(in, v); OnlineModelsSU2.readArr(in, Gv); OnlineModelsSU2.readArr(in, w); OnlineModelsSU2.readArr(in, Gw);
        stream.read(in); featT = -1;
    }
}
