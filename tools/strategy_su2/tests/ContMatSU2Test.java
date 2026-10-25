package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * P49 continuation & direction matrix against the Python reference (fixture r16_contmat: 8,000 HOTCOLD draws):
 * the verified-pattern report at every refit, the probability table and the state; checkpoint continuity (write at
 * t=4100, read into a fresh instance, continue). Prints R16_CONTMAT_PARITY_PASS.
 */
public final class ContMatSU2Test {
    public static void main(String[] args) throws Exception {
        Path dir = Paths.get(args.length > 0 ? args[0] : "reference/fixtures/r16_contmat");
        int[][] D = AtomNetSU2Test.draws(dir.resolve("draws.txt")); int T = D.length;
        float[] PP = AtomNetSU2Test.f32(dir.resolve("P.f32")), SS = AtomNetSU2Test.f32(dir.resolve("ST.f32"));
        Map<Integer, List<String>> py = new TreeMap<>();
        for (String l : Files.readAllLines(dir.resolve("verified.txt"))) { if (l.trim().isEmpty()) continue; String[] f = l.split("\\|"); py.computeIfAbsent(Integer.parseInt(f[0]), k -> new ArrayList<>()).add(f[1] + "|" + f[2] + "|" + f[3] + "|" + f[4]); }
        ContMatSU2 m = new ContMatSU2();
        float[][] p = new float[10][10]; float[] s = new float[3];
        double wp = 0, ws = 0; int refits = 0, reportMismatch = 0; long t0 = System.nanoTime();
        for (int t = 0; t < T; t++) {
            if (t >= ContMatSU2.START + ContMatSU2.BLOCK && t % ContMatSU2.BLOCK == 0) {
                List<String> j = m.verified(), q = py.getOrDefault(t, new ArrayList<>());
                refits++;
                if (!j.equals(q)) { reportMismatch++; System.out.println("refit " + t + ": java " + j.size() + " python " + q.size() + (j.isEmpty() || q.isEmpty() ? "" : " first java " + j.get(0) + " python " + q.get(0))); }
            }
            m.predict(p, s);
            for (int a = 0; a < 10; a++) for (int n = 0; n < 10; n++) wp = Math.max(wp, Math.abs(p[a][n] - PP[(t * 10 + a) * 10 + n]));
            for (int k = 0; k < 3; k++) ws = Math.max(ws, Math.abs(s[k] - SS[t * 3 + k]));
            m.update(D[t]);
            if (t == 4099) {
                ByteArrayOutputStream bo = new ByteArrayOutputStream(); m.write(new DataOutputStream(bo));
                ContMatSU2 r = new ContMatSU2(); r.read(new DataInputStream(new ByteArrayInputStream(bo.toByteArray()))); m = r;
                System.out.println("checkpoint at t=4100: " + bo.size() + " bytes");
            }
        }
        System.out.printf(Locale.ROOT, "continuation matrix: %d refits, report mismatches %d; max|dP| %.2e max|dST| %.2e; %.2f ms/period%n", refits, reportMismatch, wp, ws, (System.nanoTime() - t0) / 1e6 / T);
        System.out.println("last report (" + m.verified().size() + " verified): " + m.verified().subList(0, Math.min(3, m.verified().size())));
        boolean ok = reportMismatch == 0 && wp < 1e-5 && ws < 1e-4;
        System.out.println(ok ? "R16_CONTMAT_PARITY_PASS" : "R16_CONTMAT_PARITY_FAIL");
        if (!ok) System.exit(1);
    }
}
