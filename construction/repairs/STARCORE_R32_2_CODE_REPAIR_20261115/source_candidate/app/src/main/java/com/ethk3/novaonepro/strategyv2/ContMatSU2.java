package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.util.*;

/**
 * P49 atomic continuation & direction matrix (contmat.py in the Python reference), streaming and phone-feasible.
 *
 * Patterns, all formed from draws already settled (causal):
 *   ATOM i TOP / BOTTOM      score atom i (AtomStreamSU2, z-scored per position, clipped) >= +1.645 / <= -1.645 at (p, n)
 *   BIG/SMALL/ODD/EVEN_RUN_k position p showed a big / small / odd / even number in each of the last k draws
 *                            (k = 3, 5, 7, 10) -> active for those numbers at p ("does the streak continue?")
 *   UNDER_W / OVER_W         number n at position p under / over represented over the last W draws (z <= -2 / >= 2),
 *                            W = 100, 200 ("does it catch up / cool down?")
 * For every (pattern, scope = all positions | one position) the matrix keeps, per 500-period block, how many cells were
 * active and how many came out. At every refit a pattern is VERIFIED only when both halves of the settled blocks deviate
 * from 10% in the same direction with |z| >= 2.5 (all positions) / 3.5 (one position), >= 200 cells each. The table for
 * the next block is softmax_n(sum of log(rate / 0.1) over the verified active patterns); nothing verified -> uniform, and
 * the calibrated table never passes the INFORMATIVE guard. verified() is the human-readable continuation / reversal report.
 * State: (verified patterns / 100, largest added logit, share of positions with an active verified pattern).
 */
public final class ContMatSU2 implements TemporalModelR26 {
    static final int START = 1500, BLOCK = 500, MIN_N = 200, MAX_BLOCKS = 40;   // verification uses the last 40 settled blocks
    static final double Z_ALL = 2.5, Z_POS = 3.5;
    static final float THR = 1.645f;
    static final int[] RUNS = {3, 5, 7, 10};
    static final int[] WINDOWS = {100, 200};
    static final String[] RUN_KINDS = {"BIG", "SMALL", "ODD", "EVEN"};

    final AtomStreamSU2 atoms = new AtomStreamSU2();
    final int nAtoms = AtomStreamSU2.size(), nPat;
    final String[] names;
    final int[][] runLen = new int[4][10];
    final int[][] ring = new int[WINDOWS[WINDOWS.length - 1]][10];
    final int[][][] win = new int[WINDOWS.length][10][10];
    final List<double[]> blkN = new ArrayList<>(), blkK = new ArrayList<>();   // per block: [pattern * 11 + scope]
    int blockBase;                                                               // index of blkN.get(0)
    final List<int[]> verPat = new ArrayList<>();                                // {pattern, scope}
    final List<Double> verLog = new ArrayList<>(), verRate = new ArrayList<>();
    final List<Long> verCells = new ArrayList<>();
    long t,contextStart;
    boolean[][][] active;                                                        // [pattern][p][n] for the current target
    final double[] zraw = new double[100];

    public ContMatSU2() {
        List<String> n = new ArrayList<>();
        for (int i = 0; i < nAtoms; i++) { n.add("ATOM:" + AtomStreamSU2.CATALOG[i].name() + ":TOP"); n.add("ATOM:" + AtomStreamSU2.CATALOG[i].name() + ":BOTTOM"); }
        for (String k : RUN_KINDS) for (int r : RUNS) n.add(k + "_RUN_" + r);
        for (int w : WINDOWS) { n.add("UNDER_" + w); n.add("OVER_" + w); }
        names = n.toArray(new String[0]); nPat = names.length;
    }
    public int stateWidth() { return 3; }

    static boolean sel(int kind, int n) { return kind == 0 ? n >= 5 : kind == 1 ? n < 5 : kind == 2 ? n % 2 == 0 : n % 2 == 1; }

    private void masks() {
        active = new boolean[nPat][10][10];
        for (int i = 0; i < nAtoms; i++) {
            float[][] a = atoms.atom(i);
            for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) { active[2 * i][p][n] = a[p][n] >= THR; active[2 * i + 1][p][n] = a[p][n] <= -THR; }
        }
        int j = 2 * nAtoms;
        for (int k = 0; k < 4; k++) for (int r : RUNS) { for (int p = 0; p < 10; p++) if (runLen[k][p] >= r) for (int n = 0; n < 10; n++) active[j][p][n] = sel(k, n); j++; }
        for (int wi = 0; wi < WINDOWS.length; wi++) {
            int W = WINDOWS[wi];
            if (t-contextStart >= W) for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) {
                double z = (win[wi][p][n] - W / 10.0) / Math.sqrt(W * 0.09);
                active[j][p][n] = z <= -2; active[j + 1][p][n] = z >= 2;
            }
            j += 2;
        }
    }

    public void predict(float[][] out, float[] st) {
        if (active == null) masks();
        Arrays.fill(zraw, 0.0);
        for (int v = 0; v < verPat.size(); v++) {
            int j = verPat.get(v)[0], sc = verPat.get(v)[1]; double lg = verLog.get(v);
            for (int p = 0; p < 10; p++) { if (sc > 0 && p != sc - 1) continue; for (int n = 0; n < 10; n++) if (active[j][p][n]) zraw[p * 10 + n] += lg; }
        }
        double mx = Double.NEGATIVE_INFINITY; int posAct = 0;
        for (int p = 0; p < 10; p++) { boolean any = false; for (int n = 0; n < 10; n++) { mx = Math.max(mx, zraw[p * 10 + n]); any |= zraw[p * 10 + n] != 0; } if (any) posAct++; }
        double[] e = new double[10];
        for (int p = 0; p < 10; p++) {
            double m = Double.NEGATIVE_INFINITY; for (int n = 0; n < 10; n++) m = Math.max(m, zraw[p * 10 + n]);
            for (int n = 0; n < 10; n++) e[n] = Math.exp(zraw[p * 10 + n] - m);
            double s = ModelOpsSU2.pairwise(e, 0, 10);
            for (int n = 0; n < 10; n++) out[p][n] = (float) (e[n] / s);
        }
        st[0] = (float) (verPat.size() / 100.0); st[1] = (float) mx; st[2] = (float) (posAct / 10.0);
    }

    public void update(int[] d) {
        OnlineModelsSU2.requireDraw(d);
        if (active == null) masks();
        int bi = (int) (t / BLOCK);
        while (blockBase + blkN.size() <= bi) { blkN.add(new double[nPat * 11]); blkK.add(new double[nPat * 11]); }
        double[] bn = blkN.get(bi - blockBase), bk = blkK.get(bi - blockBase);
        for (int j = 0; j < nPat; j++) for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) if (active[j][p][n]) {
            boolean h = d[p] - 1 == n;
            bn[j * 11] += 1; bn[j * 11 + p + 1] += 1;
            if (h) { bk[j * 11] += 1; bk[j * 11 + p + 1] += 1; }
        }
        // direction state
        for (int p = 0; p < 10; p++) {
            int x = d[p] - 1;
            boolean[] ind = {x >= 5, x < 5, x % 2 == 0, x % 2 == 1};
            for (int k = 0; k < 4; k++) runLen[k][p] = ind[k] ? runLen[k][p] + 1 : 0;
        }
        int R = ring.length;
        for (int wi = 0; wi < WINDOWS.length; wi++) {
            int W = WINDOWS[wi];
            if (t-contextStart >= W) { int[] old = ring[(int) ((t - W) % R)]; for (int p = 0; p < 10; p++) win[wi][p][old[p] - 1]--; }
            for (int p = 0; p < 10; p++) win[wi][p][d[p] - 1]++;
        }
        System.arraycopy(d, 0, ring[(int) (t % R)], 0, 10);
        atoms.update(d);
        t++;
        while (blockBase < (int) (t / BLOCK) - MAX_BLOCKS - 1) { blkN.remove(0); blkK.remove(0); blockBase++; }   // keep the window + the open block
        if (t >= START + BLOCK && t % BLOCK == 0) refit();
        active = null;
    }

    private void refit() {
        int end = (int) (t / BLOCK), first = Math.max(START / BLOCK, end - MAX_BLOCKS), nbk = end - first, h1 = first + nbk / 2;
        verPat.clear(); verLog.clear(); verRate.clear(); verCells.clear();
        for (int j = 0; j < nPat; j++) for (int sc = 0; sc < 11; sc++) {
            double n1 = 0, k1 = 0, n2 = 0, k2 = 0;
            for (int b = first; b < h1; b++) { n1 += blkN.get(b - blockBase)[j * 11 + sc]; k1 += blkK.get(b - blockBase)[j * 11 + sc]; }
            for (int b = h1; b < end; b++) { n2 += blkN.get(b - blockBase)[j * 11 + sc]; k2 += blkK.get(b - blockBase)[j * 11 + sc]; }
            if (n1 < MIN_N || n2 < MIN_N) continue;
            double z1 = (k1 / n1 - 0.1) / Math.sqrt(0.09 / n1), z2 = (k2 / n2 - 0.1) / Math.sqrt(0.09 / n2), zt = sc == 0 ? Z_ALL : Z_POS;
            if (z1 * z2 > 0 && Math.abs(z1) >= zt && Math.abs(z2) >= zt) {
                double r = (k1 + k2) / (n1 + n2);
                verPat.add(new int[]{j, sc}); verLog.add(Math.log(Math.max(r, 1e-3) / 0.1)); verRate.add(r); verCells.add((long) (n1 + n2));
            }
        }
    }

    /** The current continuation / reversal report: pattern, scope (0 = all positions, p = position p), rate, cells. */
    public List<String> verified() {
        List<String> out = new ArrayList<>();
        for (int v = 0; v < verPat.size(); v++)
            out.add(String.format(Locale.ROOT, "%s|%s|%.4f|%d", names[verPat.get(v)[0]], verPat.get(v)[1] == 0 ? "all" : "pos" + verPat.get(v)[1], verRate.get(v), verCells.get(v)));
        return out;
    }

    public void resetTemporalContext(){
        try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();new AtomStreamSU2().write(new DataOutputStream(bytes));atoms.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));}catch(IOException impossible){throw new IllegalStateException("R26_CONTMAT_CONTEXT_RESET",impossible);}
        contextStart=t;for(int[] row:runLen)Arrays.fill(row,0);for(int[] row:ring)Arrays.fill(row,0);for(int[][] window:win)for(int[] row:window)Arrays.fill(row,0);active=null;Arrays.fill(zraw,0);
    }
    public void write(DataOutput o) throws IOException {
        o.writeLong(Long.MIN_VALUE);o.writeInt(2);o.writeLong(t);o.writeLong(contextStart);atoms.write(o);
        for (int[] r : runLen) for (int v : r) o.writeInt(v);
        for (int[] r : ring) for (int v : r) o.writeByte(v);
        for (int[][] w : win) for (int[] r : w) for (int v : r) o.writeInt(v);
        o.writeInt(blockBase); o.writeInt(blkN.size());
        for (int b = 0; b < blkN.size(); b++) { for (double v : blkN.get(b)) o.writeDouble(v); for (double v : blkK.get(b)) o.writeDouble(v); }
        o.writeInt(verPat.size());
        for (int v = 0; v < verPat.size(); v++) { o.writeInt(verPat.get(v)[0]); o.writeInt(verPat.get(v)[1]); o.writeDouble(verLog.get(v)); o.writeDouble(verRate.get(v)); o.writeLong(verCells.get(v)); }
    }
    public void read(DataInput in) throws IOException {
        long header=in.readLong();if(header==Long.MIN_VALUE){if(in.readInt()!=2)throw new IOException("R26_CONTMAT_STATE_ABI");t=in.readLong();contextStart=in.readLong();}else{t=header;contextStart=0;}if(t<0||contextStart<0||contextStart>t)throw new IOException("R26_CONTMAT_STATE_CURSOR");atoms.read(in);
        for (int[] r : runLen) for (int i = 0; i < r.length; i++) r[i] = in.readInt();
        for (int[] r : ring) for (int i = 0; i < r.length; i++) r[i] = in.readByte();
        for (int[][] w : win) for (int[] r : w) for (int i = 0; i < r.length; i++) r[i] = in.readInt();
        blockBase = in.readInt(); int nb = in.readInt();if(nb<0||nb>MAX_BLOCKS+1)throw new IOException("R26_CONTMAT_BLOCK_BOUND"); blkN.clear(); blkK.clear();
        for (int b = 0; b < nb; b++) { double[] a = new double[nPat * 11], c = new double[nPat * 11]; for (int i = 0; i < a.length; i++) a[i] = in.readDouble(); for (int i = 0; i < c.length; i++) c[i] = in.readDouble(); blkN.add(a); blkK.add(c); }
        int nv = in.readInt();if(nv<0||nv>nPat*11)throw new IOException("R26_CONTMAT_VERIFICATION_BOUND"); verPat.clear(); verLog.clear(); verRate.clear(); verCells.clear();
        for (int v = 0; v < nv; v++) { verPat.add(new int[]{in.readInt(), in.readInt()}); verLog.add(in.readDouble()); verRate.add(in.readDouble()); verCells.add(in.readLong()); }
        active = null;
    }
}
