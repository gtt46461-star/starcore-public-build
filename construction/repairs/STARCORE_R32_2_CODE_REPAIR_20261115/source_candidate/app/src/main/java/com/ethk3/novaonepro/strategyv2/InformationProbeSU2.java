package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.util.*;

/**
 * P50 information probes (diagnosis_probe.py): nine representative online models, one per mathematical family, run
 * every period exactly like a live strategy - model -> online temperature calibration (CalibrationSU2.Live) -> best
 * position's top-3 -> causal confidence gate at 15% and 30% (quantile of the last 720 confidences, refreshed every 50
 * periods, INFORMATIVE guard: a uniform table never acts). They answer the diagnosis' first question - is there anything
 * in the draws a family can use, and which family - independently of what evolution happened to choose.
 *
 * Bounded by construction: per probe only the sufficient statistics of the last 40 closed 500-period blocks
 * (n, sum and sum of squares of the log-likelihood gain over uniform, acts / hits / predicted hits at both gates, the
 * REGMEM "no structure" posterior) plus lifetime totals; RAM = the models + one 2,500-period calibration ring each.
 * Java == Python on the real report data (InformationProbeSU2Test, fixture r16_probe).
 */
public final class InformationProbeSU2 {
    public static final int START = 1500, BLOCK = 500, STEP = 50, PAST = 720, MIN_HISTORY = 100, KEEP_BLOCKS = 40;
    static final double INFORMATIVE = ModelReplaySU2.INFORMATIVE;
    /** family -> parameters (every parameter explicit; identical to diagnosis_probe.PROBES). */
    public static final String[][] PROBES = {
        {"MSDIR", "alpha=1.0,scales=(0.9, 0.97, 0.99, 0.997, 1.0),share=0.01"},
        {"BOCPD", "R=160,alpha=0.5,hazard=0.005"},
        {"HMM", "K=3,lr=0.01,seed=1,stay=0.995,temper=0.6"},
        {"LOGLIN", "l2=0.0001,lr=0.05,sources=((0, 1), (1, 1), (5, 1))"},
        {"RELBAYES", "decay=0.995,kappa=100.0,lags=(1,)"},
        {"KALMAN", "horizon=1.0,q_level=2e-05,q_slope=2e-08,temp=12.0"},
        {"REGMEM", "K=3,lr=0.01,seed=1,stay=0.995,temper=0.6"},
        {"RELMEM", "K=1,l2=0.0001,lags=(1,),lr=0.02,seed=1,sources=(),stay=0.995,temper=0.6"},
        {"CONTMAT", ""},
    };

    static Map<String, String> params(String s) {
        Map<String, String> m = new TreeMap<>();
        if (!s.isEmpty()) for (String kv : ModelGenomeSU2.splitTop(s)) { int eq = kv.indexOf('='); m.put(kv.substring(0, eq), kv.substring(eq + 1)); }
        return m;
    }

    /** One probe: model, calibrator, gate state, the open block's buffers and the closed blocks. */
    public static final class Probe {
        public final String family, params; final OnlineModelsSU2.Model model; final CalibrationSU2.Live cal = new CalibrationSU2.Live();
        final float[][] raw = new float[10][10]; final float[] st;
        final double[] ring = new double[PAST]; int ringN; double thr15 = Double.NaN, thr30 = Double.NaN;
        // open block
        int openIndex = -1; final double[] bLlr = new double[BLOCK], bSt0 = new double[BLOCK], bP15 = new double[BLOCK], bP30 = new double[BLOCK];
        int bn, ba15, bh15, ba30, bh30;
        /** closed blocks: {index, n, llr, llr2, a15, h15, p15, a30, h30, p30, st0 (NaN = none)}; lifetime totals likewise. */
        public final ArrayDeque<double[]> blocks = new ArrayDeque<>(); public final double[] lifetime = new double[11];
        Probe(String family, String params) {
            this.family = family; this.params = params;
            this.model = ModelReplaySU2.build(family, InformationProbeSU2.params(params));
            this.st = new float[model.stateWidth()];
        }
    }

    public final List<Probe> probes = new ArrayList<>();
    long t;

    public InformationProbeSU2() { for (String[] p : PROBES) probes.add(new Probe(p[0], p[1])); }

    /** One settled period: every probe decides for period t (draws < t only), is scored on `draw`, then learns it. */
    public void step(int[] draw) {
        OnlineModelsSU2.requireDraw(draw);
        for (Probe p : probes) stepProbe(p, draw);
        t++;
    }

    private void stepProbe(Probe p, int[] draw) {
        p.model.predict(p.raw, p.st);
        float[][] pc = p.cal.predict(p.raw);
        // picks: top-3 per position by (value desc, number asc), best position by mass (first maximum)
        int p1 = 0; double best = -1; int[] top = new int[3]; int[] bestTop = new int[3];
        for (int q = 0; q < 10; q++) {
            top3(pc[q], top);
            double m = 0; m += pc[q][top[0]]; m += pc[q][top[1]]; m += pc[q][top[2]];
            if (m > best) { best = m; p1 = q; System.arraycopy(top, 0, bestTop, 0, 3); }
        }
        double conf = best;
        if (t >= START && (t - START) % STEP == 0) {           // refresh the causal gate from the last 720 confidences
            if (p.ringN >= MIN_HISTORY) { p.thr15 = quantile(p, 1 - 0.15); p.thr30 = quantile(p, 1 - 0.30); }
            else { p.thr15 = Double.NaN; p.thr30 = Double.NaN; }
        }
        if (t >= START) {
            int bi = (int) (t / BLOCK);
            if (bi != p.openIndex) { closeBlock(p); p.openIndex = bi; }
            int x = draw[p1] - 1; boolean hit = bestTop[0] == x || bestTop[1] == x || bestTop[2] == x;
            double[] l = new double[10];
            for (int q = 0; q < 10; q++) l[q] = Math.log(Math.min(1.0, Math.max(1e-9, (double) pc[q][draw[q] - 1])) / 0.1);
            p.bLlr[p.bn] = ModelOpsSU2.pairwise(l, 0, 10);
            p.bSt0[p.bn] = p.family.equals("REGMEM") ? (double) p.st[0] : Double.NaN;
            boolean inf = conf > INFORMATIVE;
            if (inf && !Double.isNaN(p.thr15) && conf >= p.thr15) { p.bP15[p.ba15++] = conf; if (hit) p.bh15++; }
            if (inf && !Double.isNaN(p.thr30) && conf >= p.thr30) { p.bP30[p.ba30++] = conf; if (hit) p.bh30++; }
            p.bn++;
        }
        p.ring[p.ringN < PAST ? p.ringN : (int) (t % PAST)] = conf;          // ring holds the last 720 confidences
        if (p.ringN < PAST) p.ringN++;
        p.cal.settle(p.raw, draw);
        p.model.update(draw);
    }

    static void top3(float[] row, int[] out) {
        out[0] = out[1] = out[2] = -1;
        for (int k = 0; k < 3; k++) {
            int bi = -1;
            for (int n = 0; n < 10; n++) {
                if (n == out[0] || n == out[1]) continue;
                if (bi < 0 || row[n] > row[bi]) bi = n;
            }
            out[k] = bi;
        }
    }

    /** numpy.quantile(h, q) (linear) over the ring in time order. */
    static double quantile(Probe p, double q) {
        int n = p.ringN; double[] a = new double[n];
        if (n < PAST) System.arraycopy(p.ring, 0, a, 0, n); else System.arraycopy(p.ring, 0, a, 0, PAST);
        Arrays.sort(a);
        double vi = (n - 1) * q; if (vi >= n - 1) return a[n - 1];
        int g = (int) Math.floor(vi); double fr = vi - g, A = a[g], B = a[g + 1], d = B - A;
        return fr >= 0.5 ? B - d * (1 - fr) : A + d * fr;
    }

    static void closeBlock(Probe p) {
        if (p.openIndex < 0 || p.bn == 0) return;
        double[] b = blockRow(p);
        p.blocks.addLast(b);
        for (int i = 1; i < 10; i++) p.lifetime[i] += b[i];
        while (p.blocks.size() > KEEP_BLOCKS) p.blocks.removeFirst();
        p.bn = p.ba15 = p.bh15 = p.ba30 = p.bh30 = 0;
    }

    static double[] blockRow(Probe p) {
        double[] sq = new double[p.bn]; for (int i = 0; i < p.bn; i++) sq[i] = p.bLlr[i] * p.bLlr[i];
        boolean st = p.family.equals("REGMEM");
        return new double[]{p.openIndex, p.bn, ModelOpsSU2.pairwise(p.bLlr, 0, p.bn), ModelOpsSU2.pairwise(sq, 0, p.bn), p.ba15, p.bh15,
            ModelOpsSU2.pairwise(p.bP15, 0, p.ba15), p.ba30, p.bh30, ModelOpsSU2.pairwise(p.bP30, 0, p.ba30), st ? ModelOpsSU2.pairwise(p.bSt0, 0, p.bn) : Double.NaN};
    }

    /** Closed blocks plus the open one (as the diagnosis sees it at this moment). */
    public List<double[]> rows(Probe p) {
        List<double[]> out = new ArrayList<>(p.blocks);
        if (p.bn > 0) out.add(blockRow(p));
        return out.subList(Math.max(0, out.size() - KEEP_BLOCKS), out.size());
    }

    /** The snapshot lines (UpgradeDiagnosisSU2.parse format). */
    public String snapshotLines() {
        StringBuilder o = new StringBuilder();
        for (Probe p : probes) {
            o.append("probe\t").append(p.family).append('\t').append(p.params.isEmpty() ? "-" : p.params).append('\t');
            List<double[]> rs = rows(p);
            for (int i = 0; i < rs.size(); i++) { double[] b = rs.get(i); if (i > 0) o.append(',');
                o.append((long) b[1]).append(':').append(b[2]).append(':').append(b[3]).append(':').append((long) b[4]).append(':').append((long) b[5]).append(':').append(b[6])
                 .append(':').append((long) b[7]).append(':').append((long) b[8]).append(':').append(b[9]).append(':').append(Double.isNaN(b[10]) ? "-" : String.valueOf(b[10])); }
            o.append('\n');
        }
        return o.toString();
    }

    /** Continuation-matrix line for the snapshot (the CONTMAT probe is the live continuation / reversal matrix). */
    public String contmatLine(int refits, int maxVerified) {
        for (Probe p : probes) if (p.model instanceof ContMatSU2) {
            List<String> v = ((ContMatSU2) p.model).verified();
            return "contmat\t" + refits + "\t" + v.size() + "\t" + Math.max(maxVerified, v.size()) + "\t" + String.join(" ; ", v.subList(0, Math.min(6, v.size()))) + "\n";
        }
        return "";
    }

    public void write(DataOutput o) throws IOException {
        o.writeLong(t);
        for (Probe p : probes) {
            p.model.write(o); p.cal.write(o);
            o.writeInt(p.ringN); for (int i = 0; i < p.ringN; i++) o.writeDouble(p.ring[i]); o.writeDouble(p.thr15); o.writeDouble(p.thr30);
            o.writeInt(p.openIndex); o.writeInt(p.bn); o.writeInt(p.ba15); o.writeInt(p.bh15); o.writeInt(p.ba30); o.writeInt(p.bh30);
            for (int i = 0; i < p.bn; i++) { o.writeDouble(p.bLlr[i]); o.writeDouble(p.bSt0[i]); }
            for (int i = 0; i < p.ba15; i++) o.writeDouble(p.bP15[i]);
            for (int i = 0; i < p.ba30; i++) o.writeDouble(p.bP30[i]);
            o.writeInt(p.blocks.size()); for (double[] b : p.blocks) for (double v : b) o.writeDouble(v);
            for (double v : p.lifetime) o.writeDouble(v);
        }
    }

    public void read(DataInput in) throws IOException {
        t = in.readLong();
        for (Probe p : probes) {
            p.model.read(in); p.cal.read(in);
            p.ringN = in.readInt(); for (int i = 0; i < p.ringN; i++) p.ring[i] = in.readDouble(); p.thr15 = in.readDouble(); p.thr30 = in.readDouble();
            p.openIndex = in.readInt(); p.bn = in.readInt(); p.ba15 = in.readInt(); p.bh15 = in.readInt(); p.ba30 = in.readInt(); p.bh30 = in.readInt();
            for (int i = 0; i < p.bn; i++) { p.bLlr[i] = in.readDouble(); p.bSt0[i] = in.readDouble(); }
            for (int i = 0; i < p.ba15; i++) p.bP15[i] = in.readDouble();
            for (int i = 0; i < p.ba30; i++) p.bP30[i] = in.readDouble();
            int nb = in.readInt(); p.blocks.clear(); for (int b = 0; b < nb; b++) { double[] r = new double[11]; for (int i = 0; i < 11; i++) r[i] = in.readDouble(); p.blocks.addLast(r); }
            for (int i = 0; i < 11; i++) p.lifetime[i] = in.readDouble();
        }
    }
}
