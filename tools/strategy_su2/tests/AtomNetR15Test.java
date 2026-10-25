package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;

/**
 * R15 parity with the Python reference (fixture r15_atomnet):
 *  1. the 165-atom catalog and the 112 registry state atoms in the Python order;
 *  2. the 27 R15 atoms (GAP statistics, decomposed XI experts and supports), the 112 state series and their
 *     indicators at 37 sample periods;
 *  3. two ATOMNET configurations over 5,000 periods (decomposed XI; default + GAP + crossing + 3 learned molecules +
 *     group + relations);
 *  4. the TEACHER:<j> pool of three calibrated atom networks: replay == Python, live == replay (with a checkpoint).
 * Prints R15_ATOMNET_PARITY_PASS.
 */
public final class AtomNetR15Test {
    static double[] f64(Path p) throws IOException { byte[] b = Files.readAllBytes(p); DoubleBuffer d = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).asDoubleBuffer(); double[] o = new double[d.remaining()]; d.get(o); return o; }

    public static void main(String[] args) throws Exception {
        Path dir = Paths.get(args.length > 0 ? args[0] : "reference/fixtures/r15_atomnet");
        Path d14 = Paths.get(args.length > 1 ? args[1] : "reference/fixtures/r14b_atomnet");
        int[][] D = AtomNetSU2Test.draws(d14.resolve("draws.txt")); int T = D.length;
        // 1. catalogs
        int na = 0, ns = 0;
        for (String line : Files.readAllLines(dir.resolve("catalog15.txt"))) {
            String[] f = line.split("\\|", -1);
            AtomStreamSU2.Spec s = f[0].equals("A") ? AtomStreamSU2.CATALOG[Integer.parseInt(f[1])] : AtomStreamSU2.STATES[Integer.parseInt(f[1])];
            if (!s.family.equals(f[2])) throw new AssertionError("catalog " + line);
            for (String kv : f[3].isEmpty() ? new String[0] : f[3].split(",")) {
                String[] a = kv.split("=");
                boolean ok = s.p.containsKey(a[0]) ? s.p.get(a[0]) == Double.parseDouble(a[1]) : a[1].equals(s.str.get(a[0]));
                if (!ok) throw new AssertionError("catalog param " + line);
            }
            if (f[0].equals("A")) na++; else ns++;
        }
        if (na != AtomStreamSU2.size() || ns != AtomStreamSU2.states()) throw new AssertionError("catalog sizes " + na + "/" + ns);
        int base = AtomStreamSU2.r14bSize(), n15 = AtomStreamSU2.size() - base;
        // 2. atoms, state series and indicators at the sample periods
        List<Integer> S = new ArrayList<>(); for (String l : Files.readAllLines(dir.resolve("sample_t.txt"))) if (!l.trim().isEmpty()) S.add(Integer.parseInt(l.trim()));
        int nS = S.size(); float[] A = AtomNetSU2Test.f32(dir.resolve("atoms15_sample.f32")), IND = AtomNetSU2Test.f32(dir.resolve("states_ind_sample.f32"));
        double[] RAW = f64(dir.resolve("states_raw_sample.f64"));
        AtomStreamSU2 st = new AtomStreamSU2(); int si = 0; double worstA = 0, worstRaw = 0; int indDiff = 0;
        for (int t = 0; t < T && si < nS; t++) {
            if (t == S.get(si)) {
                for (int i = 0; i < n15; i++) { float[][] a = st.atom(base + i); for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) {
                    double dd = Math.abs(a[p][n] - A[((i * nS + si) * 10 + p) * 10 + n]); worstA = Math.max(worstA, dd);
                    if (dd > 1e-6) throw new AssertionError("R15 atom " + AtomStreamSU2.CATALOG[base + i].name() + " t=" + t + " p=" + p + " n=" + n + " java=" + a[p][n] + " py=" + A[((i * nS + si) * 10 + p) * 10 + n]); } }
                for (int j = 0; j < AtomStreamSU2.states(); j++) {
                    double[] v = st.stateAt(AtomStreamSU2.STATES[j], t); float[] ind = st.state(j);
                    for (int p = 0; p < 10; p++) {
                        double py = RAW[(j * nS + si) * 10 + p];
                        if (Double.isNaN(py) != Double.isNaN(v[p])) throw new AssertionError("state missing " + AtomStreamSU2.STATES[j].name() + " t=" + t + " p=" + p);
                        if (!Double.isNaN(py)) { double dd = Math.abs(py - v[p]) / Math.max(1.0, Math.abs(py)); worstRaw = Math.max(worstRaw, dd); if (dd > 1e-12) throw new AssertionError("state value " + AtomStreamSU2.STATES[j].name() + " t=" + t + " java=" + v[p] + " py=" + py); }
                        if (ind[p] != IND[(j * nS + si) * 10 + p]) indDiff++;
                    }
                }
                si++;
            }
            st.update(D[t]);
        }
        System.out.printf(Locale.ROOT, "R15 atoms: %d at %d periods max|diff| %.2e; 112 state series max rel diff %.2e; indicator differences %d/%d%n", n15, nS, worstA, worstRaw, indDiff, 112 * nS * 10);
        if (indDiff > 0) throw new AssertionError("state indicators");
        // 3. ATOMNET configurations
        Map<String, float[][][]> tables = new LinkedHashMap<>();
        for (String cfg : new String[]{"cfg4", "cfg5"}) {
            Map<String, String> params = AtomNetSU2Test.member(Files.readAllLines(dir.resolve(cfg + ".txt")).get(0));
            float[] PP = AtomNetSU2Test.f32(dir.resolve(cfg + ".P.f32")), SS = AtomNetSU2Test.f32(dir.resolve(cfg + ".ST.f32"));
            AtomNetSU2 m = AtomNetSU2.of(params); float[][][] tab = new float[T][][];
            float[][] p = new float[10][10]; float[] s = new float[3]; double wp = 0, ws = 0; long t0 = System.nanoTime();
            for (int t = 0; t < T; t++) {
                m.predict(p, s); tab[t] = new float[10][]; for (int q = 0; q < 10; q++) tab[t][q] = p[q].clone();
                for (int q = 0; q < 10; q++) for (int n = 0; n < 10; n++) wp = Math.max(wp, Math.abs(p[q][n] - PP[(t * 10 + q) * 10 + n]));
                for (int k = 0; k < 3; k++) ws = Math.max(ws, Math.abs(s[k] - SS[t * 3 + k]));
                m.update(D[t]);
            }
            System.out.printf(Locale.ROOT, "%s %s: max|dP| %.2e max|dST| %.2e, %.3f ms/period%n", cfg, params, wp, ws, (System.nanoTime() - t0) / 1e6 / T);
            if (wp > 1e-5 || ws > 1e-4) throw new AssertionError(cfg + " parity");
            System.out.println("  credit " + m.credit());
            tables.put(cfg, tab);
        }
        // 4. teacher matrix pool
        List<String> tl = Files.readAllLines(dir.resolve("teacher.txt")); int j = Integer.parseInt(tl.get(0).trim());
        float[][][][] cal = new float[3][][][];
        for (int k = 0; k < 3; k++) {
            OnlineModelsSU2.Output o = OnlineModelsSU2.run(AtomNetSU2.of(AtomNetSU2Test.member(tl.get(1 + k))), D);
            cal[k] = CalibrationSU2.calibrate(o.p, D);
        }
        float[][][] PT = TeacherPoolSU2.pool(cal, D, TeacherPoolSU2.states(D, j));
        float[] FT = AtomNetSU2Test.f32(dir.resolve("teacher.P.f32")); double wt = 0;
        for (int t = 0; t < T; t++) for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) wt = Math.max(wt, Math.abs(PT[t][p][n] - FT[(t * 10 + p) * 10 + n]));
        TeacherPoolSU2.Live live = new TeacherPoolSU2.Live(3, j); double wl = 0; float[][][] at = new float[3][][];
        for (int t = 0; t < T; t++) {
            for (int k = 0; k < 3; k++) at[k] = cal[k][t];
            float[][] q = live.predict(at);
            for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) wl = Math.max(wl, Math.abs(q[p][n] - PT[t][p][n]));
            live.settle(at, D[t]);
            if (t == 3001) { ByteArrayOutputStream bo = new ByteArrayOutputStream(); live.write(new DataOutputStream(bo)); TeacherPoolSU2.Live r = new TeacherPoolSU2.Live(3, j); r.read(new DataInputStream(new ByteArrayInputStream(bo.toByteArray()))); live = r; System.out.println("  teacher checkpoint at t=3002: " + bo.size() + " bytes"); }
        }
        System.out.printf(Locale.ROOT, "teacher pool (state %s): replay vs Python max|dP| %.2e; live vs replay max|dP| %.2e%n", AtomStreamSU2.STATES[j].name(), wt, wl);
        if (wt > 1e-6 || wl > 0) throw new AssertionError("teacher pool parity");
        // 5. atomic distillation of the calibrated decomposed-XI table (cfg4) == Python distill.fit / atomic_genome
        String js = new String(Files.readAllBytes(dir.resolve("distill.json")), "UTF-8").replaceAll("\\s", "");
        List<Integer> cand = new ArrayList<>(); for (String x : js.replaceAll(".*\"candidates\":\\[([^\\]]*)\\].*", "$1").split(",")) cand.add(Integer.parseInt(x));
        String[] wS = js.replaceAll(".*\"w\":\\[([^\\]]*)\\].*", "$1").split(",");
        double r2py = Double.parseDouble(js.replaceAll(".*\"r2\":([-0-9.eE]+).*", "$1"));
        TreeSet<Integer> topPy = new TreeSet<>(); for (String x : js.replaceAll(".*\"top\":\\[([^\\]]*)\\].*", "$1").split(",")) topPy.add(Integer.parseInt(x));
        ModelEvolutionSU2.Distilled all = ModelEvolutionSU2.distill(cal[1], D, cand, T, cand.size());
        double wDiff = 0; for (int i = 0; i < all.atoms.length; i++) { double py = Double.parseDouble(wS[cand.indexOf(all.atoms[i])]); wDiff = Math.max(wDiff, Math.abs(all.weights[i] - py) / Math.max(1e-3, Math.abs(py))); }
        ModelEvolutionSU2.Distilled top = ModelEvolutionSU2.distill(cal[1], D, cand, T, 8);
        TreeSet<Integer> topJ = new TreeSet<>(); for (int a : top.atoms) topJ.add(a);
        System.out.printf(Locale.ROOT, "distillation: R2 java %.6f python %.6f; max rel weight diff %.2e; top-8 atoms equal: %s%n", all.r2, r2py, wDiff, topJ.equals(topPy));
        if (Math.abs(all.r2 - r2py) > 1e-6 || wDiff > 1e-4 || !topJ.equals(topPy)) throw new AssertionError("distillation parity");
        System.out.println("R15_ATOMNET_PARITY_PASS");
    }
}
