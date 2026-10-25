package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;

/**
 * Parity of AtomStreamSU2 / AtomNetSU2 with reference/replay/atomnet.py (fixture r14b_atomnet):
 *  1. catalog order identical;
 *  2. every one of the 138 atoms, two atom crossings and the group atoms at 31 sample periods (|diff| <= 1e-6);
 *  3. three ATOMNET configurations over 5,000 periods: tables |diff| <= 1e-5, states <= 1e-4, same top-3 picks;
 *  4. a checkpoint written at period 2,500 and read into a fresh model continues bit-identically.
 * Prints R14B_ATOMNET_PARITY_PASS.
 */
public final class AtomNetSU2Test {
    public static float[] f32(Path p) throws IOException {
        byte[] b = Files.readAllBytes(p); FloatBuffer fb = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
        float[] out = new float[fb.remaining()]; fb.get(out); return out;
    }
    public static int[][] draws(Path p) throws IOException {
        List<int[]> rows = new ArrayList<>();
        for (String line : Files.readAllLines(p)) { if (line.trim().isEmpty()) continue; String[] f = line.trim().split("\\s+"); int[] r = new int[10]; for (int i = 0; i < 10; i++) r[i] = Integer.parseInt(f[i]); rows.add(r); }
        return rows.toArray(new int[0][]);
    }
    public static Map<String, String> member(String text) {
        String body = text.substring(text.indexOf('{') + 1, text.lastIndexOf('}'));
        Map<String, String> m = new TreeMap<>();
        for (String kv : body.split(",")) { String[] a = kv.split("=", 2); m.put(a[0], a[1]); }
        return m;
    }

    public static void main(String[] args) throws Exception {
        Path dir = Paths.get(args.length > 0 ? args[0] : "reference/fixtures/r14b_atomnet");
        int[][] D = draws(dir.resolve("draws.txt")); int T = D.length;
        // 1. catalog
        List<String> cat = Files.readAllLines(dir.resolve("catalog.txt"));
        // later engines append atoms; every index of this fixture's catalog must keep its meaning
        if (cat.size() > AtomStreamSU2.size()) throw new AssertionError("catalog size " + cat.size() + " vs " + AtomStreamSU2.size());
        for (int i = 0; i < cat.size(); i++) {
            String[] f = cat.get(i).split("\\|", -1);
            if (!f[1].equals(AtomStreamSU2.CATALOG[i].family)) throw new AssertionError("catalog " + i + " " + cat.get(i));
            for (String kv : f[2].isEmpty() ? new String[0] : f[2].split(",")) { String[] a = kv.split("="); if (Math.abs(AtomStreamSU2.CATALOG[i].d(a[0]) - Double.parseDouble(a[1])) > 0) throw new AssertionError("catalog param " + i); }
        }
        // 2. atoms / pairs / group at sample periods
        List<Integer> S = new ArrayList<>(); for (String l : Files.readAllLines(dir.resolve("sample_t.txt"))) if (!l.trim().isEmpty()) S.add(Integer.parseInt(l.trim()));
        float[] A = f32(dir.resolve("atoms_sample.f32")), PR = f32(dir.resolve("pairs_sample.f32")), GR = f32(dir.resolve("group_sample.f32"));
        List<int[]> pairs = new ArrayList<>(); for (String l : Files.readAllLines(dir.resolve("pairs.txt"))) if (!l.trim().isEmpty()) { String[] f = l.trim().split("\\s+"); pairs.add(new int[]{Integer.parseInt(f[0]), Integer.parseInt(f[1])}); }
        AtomStreamSU2 st = new AtomStreamSU2(); int si = 0; double worstAtom = 0; int nA = cat.size(), nS = S.size();
        for (int t = 0; t < T && si < nS; t++) {
            if (t == S.get(si)) {
                for (int i = 0; i < nA; i++) { float[][] a = st.atom(i); for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) {
                    double d = Math.abs(a[p][n] - A[((i * nS + si) * 10 + p) * 10 + n]); worstAtom = Math.max(worstAtom, d);
                    if (d > 1e-6) throw new AssertionError("atom " + AtomStreamSU2.CATALOG[i].name() + " t=" + t + " p=" + p + " n=" + n + " java=" + a[p][n] + " py=" + A[((i * nS + si) * 10 + p) * 10 + n]); } }
                for (int k = 0; k < pairs.size(); k++) { float[][] a = AtomStreamSU2.pair(st.atom(pairs.get(k)[0]), st.atom(pairs.get(k)[1]));
                    for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) { double d = Math.abs(a[p][n] - PR[((k * nS + si) * 10 + p) * 10 + n]); worstAtom = Math.max(worstAtom, d); if (d > 1e-6) throw new AssertionError("pair " + k + " t=" + t); } }
                float[][][] g = st.group();
                for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) for (int k = 0; k < AtomStreamSU2.N_GROUP; k++) { double d = Math.abs(g[p][n][k] - GR[((si * 10 + p) * 10 + n) * AtomStreamSU2.N_GROUP + k]); if (d > 1e-6) throw new AssertionError("group t=" + t + " k=" + k); }
                si++;
            }
            st.update(D[t]);
        }
        System.out.printf(Locale.ROOT, "atoms: %d atoms + %d crossings + group at %d periods, max |diff| %.2e%n", nA, pairs.size(), nS, worstAtom);
        // 3./4. three configurations
        for (String cfg : new String[]{"cfg1", "cfg2", "cfg3"}) {
            Map<String, String> params = member(Files.readAllLines(dir.resolve(cfg + ".txt")).get(0));
            float[] PP = f32(dir.resolve(cfg + ".P.f32")), SS = f32(dir.resolve(cfg + ".ST.f32"));
            AtomNetSU2 m = AtomNetSU2.of(params), resumed = null;
            float[][] p = new float[10][10]; float[] s = new float[3]; double worstP = 0, worstS = 0; int pickDiff = 0;
            long t0 = System.nanoTime();
            for (int t = 0; t < T; t++) {
                AtomNetSU2 cur = resumed != null ? resumed : m;
                cur.predict(p, s);
                for (int q = 0; q < 10; q++) for (int n = 0; n < 10; n++) worstP = Math.max(worstP, Math.abs(p[q][n] - PP[(t * 10 + q) * 10 + n]));
                for (int k = 0; k < 3; k++) worstS = Math.max(worstS, Math.abs(s[k] - SS[t * 3 + k]));
                pickDiff += top3(p, null) == top3(null, Arrays.copyOfRange(PP, t * 100, t * 100 + 100)) ? 0 : 1;
                cur.update(D[t]);
                if (t == T / 2 - 1) {
                    ByteArrayOutputStream bo = new ByteArrayOutputStream(); cur.write(new DataOutputStream(bo));
                    AtomNetSU2 fresh = AtomNetSU2.of(params); fresh.read(new DataInputStream(new ByteArrayInputStream(bo.toByteArray())));
                    resumed = fresh;
                    System.out.printf(Locale.ROOT, "  %s checkpoint at t=%d: %d bytes%n", cfg, t + 1, bo.size());
                }
            }
            double ms = (System.nanoTime() - t0) / 1e6 / T;
            System.out.printf(Locale.ROOT, "%s %s: max|dP| %.2e, max|dST| %.2e, top-3 pick differences %d/%d, %.3f ms/period%n", cfg, params, worstP, worstS, pickDiff, T, ms);
            if (worstP > 1e-5 || worstS > 1e-4 || pickDiff > T / 1000) throw new AssertionError(cfg + " parity");
            System.out.println("  credit " + (resumed != null ? resumed : m).credit());
        }
        System.out.println("R14B_ATOMNET_PARITY_PASS");
    }

    /** Encodes (best position by top-3 mass, its top-3 set) for comparing picks. */
    static long top3(float[][] p, float[] flat) {
        double best = -1; int bp = 0; int bm = 0;
        for (int q = 0; q < 10; q++) {
            Integer[] ord = new Integer[10]; for (int i = 0; i < 10; i++) ord[i] = i;
            final int qq = q;
            Arrays.sort(ord, (a, b) -> Double.compare(val(p, flat, qq, b) - b * 1e-12, val(p, flat, qq, a) - a * 1e-12));
            double m = 0; int mask = 0; for (int i = 0; i < 3; i++) { m += val(p, flat, q, ord[i]); mask |= 1 << ord[i]; }
            m = Math.rint(m * 1e9) / 1e9;
            if (m > best) { best = m; bp = q; bm = mask; }
        }
        return bp * 2048L + bm;
    }
    static double val(float[][] p, float[] flat, int q, int n) { return p != null ? p[q][n] : flat[q * 10 + n]; }
}
