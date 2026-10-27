package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/**
 * R14 committee strategy (P37): the champion of a frequency tier is the fusion of the top programs
 * chosen by the consensus judge (ModelEvolutionSU2), not one lucky winner.
 *
 *   P_committee(t)[p] = normalise( exp( (1/K) * sum_k log P_k(t)[p] ) )      (geometric mean of K tables)
 *   decision          = best position by top-3 mass, its top-3 numbers, confidence gate at the tier frequency
 *
 * On known-truth worlds the committee of the top-5 calibrated programs reached 37.3-37.7% (regime world),
 * 33.2-33.3% (cross), 33.3-33.4% (drift), 33.0-33.4% (mixed) at 15% frequency, against 30.7-33.3% for the
 * single program R13 selection picked (select_study.py). Canonical text is the identity:
 *
 *   SU2C|COV=0.15|G=<member genome text>##<member genome text>##...
 *
 * Mirrors foundry14.Committee / foundry14.table / foundry14.evaluate (fixture r14_parity).
 */
public final class CommitteeSU2 {
    public static final int MAX_MEMBERS = 5;
    public final List<ModelGenomeSU2> subs; public final double coverage;

    public CommitteeSU2(List<ModelGenomeSU2> subs, double coverage) {
        if (subs == null || subs.isEmpty() || subs.size() > MAX_MEMBERS) throw new IllegalArgumentException("SU2_COMMITTEE_MEMBERS");
        if (!(coverage == 0.15 || coverage == 0.30 || coverage == 1.0)) throw new IllegalArgumentException("SU2_COMMITTEE_FREQUENCY");
        Set<String> seen = new HashSet<>();
        for (ModelGenomeSU2 g : subs) if (!seen.add(g.text())) throw new IllegalArgumentException("SU2_COMMITTEE_DUPLICATE");
        this.subs = Collections.unmodifiableList(new ArrayList<>(subs)); this.coverage = coverage;
    }

    public String text() {
        StringBuilder b = new StringBuilder("SU2C|COV=").append(String.format(Locale.ROOT, "%.2f", coverage)).append("|G=");
        for (int i = 0; i < subs.size(); i++) { if (i > 0) b.append("##"); b.append(subs.get(i).text()); }
        return b.toString();
    }

    public boolean legalReadoutR26(){for(ModelGenomeSU2 g:subs)if(!g.legalReadoutR26())return false;return true;}
    public String key(){return legalReadoutR26()?StrategyPolicyIdentityR26.current(text(),"DETERMINISTIC_BOOTSTRAP_OF_BOUND_MEMBER_DATASETS"):HashingSU1.text("SU2_COMMITTEE_V1|"+text());}

    public String formula() {
        StringBuilder b = new StringBuilder("P = geometric-mean fusion[");
        for (int i = 0; i < subs.size(); i++) { if (i > 0) b.append(" ; "); String f = subs.get(i).formula(); b.append(f, 0, f.indexOf(" -> ")); }
        return b.append("] -> best position Top3 | confidence gate MASS | target frequency ").append(Math.round(coverage * 100)).append('%').toString();
    }

    public static boolean isCommittee(String text) { return text != null && text.startsWith("SU2C|"); }

    public static CommitteeSU2 parse(String text) {
        if (!isCommittee(text) || text.length() > 8192) throw new IllegalArgumentException("SU2_COMMITTEE_TEXT");
        int g = text.indexOf("|G=");
        if (g < 0 || !text.startsWith("SU2C|COV=")) throw new IllegalArgumentException("SU2_COMMITTEE_FIELDS");
        double cov = Double.parseDouble(text.substring(9, g));
        List<ModelGenomeSU2> subs = new ArrayList<>();
        for (String s : text.substring(g + 3).split("##")) subs.add(ModelGenomeSU2.parse(s));
        CommitteeSU2 c = new CommitteeSU2(subs, cov);
        if (!c.text().equals(text)) throw new IllegalArgumentException("SU2_COMMITTEE_NOT_CANONICAL");
        return c;
    }

    /** Geometric-mean fusion of tables (numpy-identical reductions). */
    public static float[][] fuse(float[][][] tables) {
        int K = tables.length; float[][] out = new float[10][10]; double[] L = new double[10], E = new double[10];
        for (int p = 0; p < 10; p++) {
            double mx = Double.NEGATIVE_INFINITY;
            for (int n = 0; n < 10; n++) { double s = 0; for (int k = 0; k < K; k++) s += Math.log(Math.min(1.0, Math.max(1e-9, (double) tables[k][p][n]))); L[n] = s / K; mx = Math.max(mx, L[n]); }
            for (int n = 0; n < 10; n++) E[n] = Math.exp(L[n] - mx);
            double z = ModelOpsSU2.pairwise(E, 0, 10);
            for (int n = 0; n < 10; n++) out[p][n] = (float) (E[n] / z);
        }
        return out;
    }

    public static float[][][] table(CommitteeSU2 c, ModelReplaySU2.Library lib) {
        float[][][][] tabs = new float[c.subs.size()][][][];
        for (int k = 0; k < tabs.length; k++) tabs[k] = ModelReplaySU2.pooledTable(c.subs.get(k), lib);
        int T = tabs[0].length; float[][][] out = new float[T][][]; float[][][] at = new float[tabs.length][][];
        for (int t = 0; t < T; t++) { for (int k = 0; k < tabs.length; k++) at[k] = tabs[k][t]; out[t] = c.legalReadoutR26()||lib.snapshotBinding!=null?LegalReadoutR26.project(fuse(at)):fuse(at); }
        return out;
    }

    public static ModelReplaySU2.Result evaluate(CommitteeSU2 c, ModelReplaySU2.Library lib, int startMeta) {
        return ModelReplaySU2.evaluateTable(table(c, lib), lib.draws(), c.coverage, startMeta);
    }
}
