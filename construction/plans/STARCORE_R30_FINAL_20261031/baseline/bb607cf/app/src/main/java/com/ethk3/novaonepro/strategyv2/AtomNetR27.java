package com.ethk3.novaonepro.strategyv2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/** R27-033 AtomNet (docs/06 6.2): in → 32 → 16 → {1 sigmoid (K3 hit), 10 softmax (number at the position)}, ReLU,
 * dropout 0.1 while training, loss = BCE + 0.5·CE + 1e-4·L2, Adam. ≤ 7k parameters at in = 172. Pure Java. */
public final class AtomNetR27 {
    public static final int H1 = 32, H2 = 16; public static final double DROPOUT = 0.1, L2 = 1e-4, CE_WEIGHT = 0.5;
    public final int in;
    final double[] w1, b1, w2, b2, wh, bh, wc, bc; // hidden1, hidden2, hit head, class head
    // Adam moments
    private final double[][] m, v; private long adamT;

    public AtomNetR27(int in, long seed) {
        this.in = in;
        w1 = new double[in * H1]; b1 = new double[H1]; w2 = new double[H1 * H2]; b2 = new double[H2]; wh = new double[H2]; bh = new double[1]; wc = new double[H2 * 10]; bc = new double[10];
        StatsR27.SplitMix r = new StatsR27.SplitMix(seed);
        init(w1, in, r); init(w2, H1, r); init(wh, H2, r); init(wc, H2, r);
        bh[0] = Math.log(0.3 / 0.7);
        double[][] p = params(); m = new double[p.length][]; v = new double[p.length][];
        for (int i = 0; i < p.length; i++) { m[i] = new double[p[i].length]; v[i] = new double[p[i].length]; }
    }
    static void init(double[] w, int fanIn, StatsR27.SplitMix r) { double s = Math.sqrt(2.0 / fanIn); for (int i = 0; i < w.length; i++) w[i] = r.gaussian() * s; }
    double[][] params() { return new double[][] {w1, b1, w2, b2, wh, bh, wc, bc}; }
    public int parameterCount() { int n = 0; for (double[] p : params()) n += p.length; return n; }

    public static final class Out { public double pHit; public final double[] pClass = new double[10]; final double[] h1 = new double[H1], h2 = new double[H2]; final boolean[] d1 = new boolean[H1], d2 = new boolean[H2]; }

    public Out forward(double[] x, StatsR27.SplitMix dropout) {
        Out o = new Out();
        for (int j = 0; j < H1; j++) { double s = b1[j]; for (int i = 0; i < in; i++) s += w1[j * in + i] * x[i]; o.h1[j] = Math.max(0, s); if (dropout != null) { o.d1[j] = dropout.chance(DROPOUT); o.h1[j] = o.d1[j] ? 0 : o.h1[j] / (1 - DROPOUT); } }
        for (int k = 0; k < H2; k++) { double s = b2[k]; for (int j = 0; j < H1; j++) s += w2[k * H1 + j] * o.h1[j]; o.h2[k] = Math.max(0, s); if (dropout != null) { o.d2[k] = dropout.chance(DROPOUT); o.h2[k] = o.d2[k] ? 0 : o.h2[k] / (1 - DROPOUT); } }
        double z = bh[0]; for (int k = 0; k < H2; k++) z += wh[k] * o.h2[k];
        o.pHit = 1 / (1 + Math.exp(-StatsR27.clamp(z, -30, 30)));
        double mx = Double.NEGATIVE_INFINITY; double[] zc = new double[10];
        for (int c = 0; c < 10; c++) { double s = bc[c]; for (int k = 0; k < H2; k++) s += wc[c * H2 + k] * o.h2[k]; zc[c] = s; mx = Math.max(mx, s); }
        double sum = 0; for (int c = 0; c < 10; c++) { o.pClass[c] = Math.exp(zc[c] - mx); sum += o.pClass[c]; }
        for (int c = 0; c < 10; c++) o.pClass[c] /= sum;
        return o;
    }
    public double predict(double[] x) { return forward(x, null).pHit; }

    /** One Adam step on a mini-batch. y = K3 hit (0/1), cls = number at the position (0..9, or −1 when unknown).
     * Returns the batch loss before the step. */
    public double trainBatch(double[][] xs, int[] y, int[] cls, double lr, StatsR27.SplitMix rng) {
        double[][] p = params(); double[][] g = new double[p.length][];
        for (int i = 0; i < p.length; i++) g[i] = new double[p[i].length];
        double loss = 0; int n = xs.length;
        for (int b = 0; b < n; b++) {
            double[] x = xs[b]; Out o = forward(x, rng);
            loss -= y[b] == 1 ? Math.log(Math.max(1e-12, o.pHit)) : Math.log(Math.max(1e-12, 1 - o.pHit));
            if (cls[b] >= 0) loss -= CE_WEIGHT * Math.log(Math.max(1e-12, o.pClass[cls[b]]));
            double dz = o.pHit - y[b]; double[] dh2 = new double[H2];
            for (int k = 0; k < H2; k++) { g[4][k] += dz * o.h2[k]; dh2[k] += dz * wh[k]; }
            g[5][0] += dz;
            if (cls[b] >= 0) for (int c = 0; c < 10; c++) {
                double dc = CE_WEIGHT * (o.pClass[c] - (c == cls[b] ? 1 : 0));
                for (int k = 0; k < H2; k++) { g[6][c * H2 + k] += dc * o.h2[k]; dh2[k] += dc * wc[c * H2 + k]; }
                g[7][c] += dc;
            }
            double[] dh1 = new double[H1];
            for (int k = 0; k < H2; k++) {
                if (o.h2[k] <= 0) continue; double d = dh2[k] / (1 - (rng == null ? 0 : DROPOUT));
                for (int j = 0; j < H1; j++) { g[2][k * H1 + j] += d * o.h1[j]; dh1[j] += d * w2[k * H1 + j]; }
                g[3][k] += d;
            }
            for (int j = 0; j < H1; j++) {
                if (o.h1[j] <= 0) continue; double d = dh1[j] / (1 - (rng == null ? 0 : DROPOUT));
                for (int i = 0; i < in; i++) g[0][j * in + i] += d * x[i];
                g[1][j] += d;
            }
        }
        adamT++; double b1c = 1 - Math.pow(0.9, adamT), b2c = 1 - Math.pow(0.999, adamT);
        for (int i = 0; i < p.length; i++) for (int k = 0; k < p[i].length; k++) {
            double gr = g[i][k] / n + ((i % 2 == 0) ? L2 * p[i][k] : 0);
            m[i][k] = 0.9 * m[i][k] + 0.1 * gr; v[i][k] = 0.999 * v[i][k] + 0.001 * gr * gr;
            p[i][k] -= lr * (m[i][k] / b1c) / (Math.sqrt(v[i][k] / b2c) + 1e-8);
        }
        return loss / n;
    }

    public byte[] encode() {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream(); DataOutputStream o = new DataOutputStream(b);
            o.writeInt(0x27A70001); o.writeInt(in); o.writeLong(adamT);
            double[][] p = params();
            for (int i = 0; i < p.length; i++) for (double[] a : new double[][] {p[i], m[i], v[i]}) for (double x : a) o.writeDouble(x);
            o.close(); return b.toByteArray();
        } catch (IOException e) { throw new IllegalStateException(e); }
    }
    public static AtomNetR27 decode(byte[] bytes) {
        try {
            DataInputStream d = new DataInputStream(new ByteArrayInputStream(bytes));
            if (d.readInt() != 0x27A70001) throw new IOException("R27_ATOMNET_ABI");
            AtomNetR27 n = new AtomNetR27(d.readInt(), 0); n.adamT = d.readLong();
            double[][] p = n.params();
            for (int i = 0; i < p.length; i++) for (double[] a : new double[][] {p[i], n.m[i], n.v[i]}) for (int k = 0; k < a.length; k++) a[k] = d.readDouble();
            return n;
        } catch (IOException e) { throw new IllegalStateException(e); }
    }
    public AtomNetR27 copy() { return decode(encode()); }
}
