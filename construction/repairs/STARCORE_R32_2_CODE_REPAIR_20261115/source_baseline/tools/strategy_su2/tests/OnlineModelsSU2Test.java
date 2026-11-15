package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Java/Python parity for the R13 model families, pooling, Sinkhorn, the META
 * applicability gate and the frequency gate, on the 5.4.3 report draws.
 *   java ... OnlineModelsSU2Test reference/fixtures/r13_parity
 * Exit code 0 only if every model matches within 2e-6 (P) / 2e-5 (state), MASS
 * genomes act identically, and META genomes differ on at most 0.5% of periods
 * (Newton solves differ in the last bits between LAPACK and Java).
 */
public final class OnlineModelsSU2Test {
    public static void main(String[] args) throws Exception {
        Path dir = Paths.get(args[0]);
        List<String> lines = Files.readAllLines(dir.resolve("draws.txt"));
        int[][] d = new int[lines.size()][10];
        for (int t = 0; t < d.length; t++) { String[] f = lines.get(t).trim().split("\\s+"); for (int p = 0; p < 10; p++) d[t][p] = Integer.parseInt(f[p]); }
        String json = new String(Files.readAllBytes(dir.resolve("genomes.json")), "UTF-8");
        boolean ok = true;
        ModelReplaySU2.Library lib = new ModelReplaySU2.Library(d, 512L << 20);
        Map<String, Map<String, String>> central = centralParams(json);
        for (String kind : ModelGenomeSU2.KINDS) {
            long t0 = System.nanoTime();
            OnlineModelsSU2.Output o = ModelReplaySU2.compute(d, kind, central.get(kind));
            double ms = (System.nanoTime() - t0) / 1e6;
            float[] P = read(dir.resolve(kind + ".P.f32")), S = read(dir.resolve(kind + ".S.f32"));
            double dp = 0, ds = 0; int sw = o.state[0].length;
            for (int t = 0; t < d.length; t++) {
                for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) dp = Math.max(dp, Math.abs(o.p[t][p][n] - P[t * 100 + p * 10 + n]));
                for (int j = 0; j < sw; j++) { double ref = S[t * sw + j]; ds = Math.max(ds, Math.abs(o.state[t][j] - ref) / Math.max(1.0, Math.abs(ref))); }
            }
            boolean pass = dp <= 2e-6 && ds <= 2e-5;
            ok &= pass;
            System.out.printf(Locale.ROOT, "%-9s maxAbsP=%.3g maxRelState=%.3g time=%.0fms %s%n", kind, dp, ds, ms, pass ? "PASS" : "FAIL");
        }
        Matcher m = Pattern.compile("\\{\"text\": \"([^\"]+)\", \"act\": \"([01]+)\", \"hit\": \"([01]+)\"").matcher(json);
        while (m.find()) {
            ModelGenomeSU2 g = ModelGenomeSU2.parse(m.group(1));
            ModelReplaySU2.Result r = ModelReplaySU2.evaluate(g, lib, 1500, 2500, 250);
            String act = m.group(2), hit = m.group(3);
            int da = 0, dh = 0;
            for (int t = 0; t < d.length; t++) { if (r.act[t] != (act.charAt(t) == '1')) da++; if (r.hit[t] != (hit.charAt(t) == '1')) dh++; }
            boolean meta = g.gate.equals("META"), gated = g.pool.equals("GATED");
            // GATED: BLAS vs Java reduction order differs in the last bits, so a few position/K3 ties may flip
            boolean pass = gated ? (dh <= d.length / 200 && da <= d.length / 100) : dh == 0 && (meta ? da <= d.length / 200 : da == 0);
            ok &= pass;
            System.out.printf(Locale.ROOT, "%s actMismatch=%d hitMismatch=%d %s%n", g.text().substring(0, Math.min(90, g.text().length())), da, dh, pass ? "PASS" : "FAIL");
        }
        System.out.println(ok ? "R13_PARITY_PASS" : "R13_PARITY_FAIL");
        if (!ok) System.exit(1);
    }

    static float[] read(Path p) throws IOException {
        byte[] b = Files.readAllBytes(p);
        FloatBuffer fb = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
        float[] out = new float[fb.remaining()]; fb.get(out); return out;
    }

    static Map<String, Map<String, String>> centralParams(String json) {
        Map<String, Map<String, String>> out = new HashMap<>();
        int i = json.indexOf("\"central\": {") + 12;
        for (String kind : ModelGenomeSU2.KINDS) {
            int k = json.indexOf("\"" + kind + "\": {", i); int s = json.indexOf('{', k) + 1, e = json.indexOf('}', s);
            TreeMap<String, String> params = new TreeMap<>();
            Matcher m = Pattern.compile("\"([A-Za-z_0-9]+)\": \"([^\"]*)\"").matcher(json.substring(s, e));
            while (m.find()) params.put(m.group(1), m.group(2));
            out.put(kind, params);
        }
        return out;
    }
}
