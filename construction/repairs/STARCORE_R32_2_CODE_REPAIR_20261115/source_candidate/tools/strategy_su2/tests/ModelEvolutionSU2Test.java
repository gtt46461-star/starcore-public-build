package com.ethk3.novaonepro.strategyv2;

import java.nio.file.*;
import java.util.*;

/** Java foundry on a structured world must find forward edge; on a no-structure world it must not claim one. */
public final class ModelEvolutionSU2Test {
    static int[][] load(Path p) throws Exception {
        List<String> lines = Files.readAllLines(p); int[][] d = new int[lines.size()][10];
        for (int t = 0; t < d.length; t++) { String[] f = lines.get(t).trim().split("\\s+"); for (int q = 0; q < 10; q++) d[t][q] = Integer.parseInt(f[q]); }
        return d;
    }
    static double[] rolling(int[][] d, long seed) {
        ModelReplaySU2.Library lib = new ModelReplaySU2.Library(d, 768L << 20);
        ModelEvolutionSU2 evo = new ModelEvolutionSU2(lib, seed, 1500, 2500, 250, false);   // the R13 engine
        ModelEvolutionSU2.State s = new ModelEvolutionSU2.State();
        int n = 0, k = 0;
        for (int c : new int[]{6000, 8000}) {
            s = evo.campaign(s, new ModelEvolutionSU2.Eras(1500, c - 1500, c - 1500, c), 8, 24, 12);
            ModelReplaySU2.Result r = evo.eval(s.champion.g);
            int a = r.actions(c, c + 2000), h = r.hits(c, c + 2000); n += a; k += h;
            System.out.println("  checkpoint " + c + " champion=" + s.champion.g.formula() + " forward=" + h + "/" + a);
        }
        System.out.println("  effectiveGenerations=" + s.effectiveGenerations + " repairNotes=" + s.repairNotes.size() + (s.repairNotes.isEmpty() ? "" : " e.g. " + s.repairNotes.get(0)));
        return new double[]{n, k};
    }
    public static void main(String[] a) throws Exception {
        Path dir = Paths.get(a[0]);
        long t0 = System.currentTimeMillis();
        System.out.println("REGIME world:"); double[] reg = rolling(load(dir.resolve("REGIME_seed1.txt")), 3);
        System.out.println("NULL world:"); double[] nul = rolling(load(dir.resolve("NULL_seed1.txt")), 3);
        double rr = reg[1] / reg[0], nr = nul[1] / nul[0];
        double lbReg = FoldStabilityLower.wilson((int) reg[1], (int) reg[0]);
        double ubNull = nr + 2.326 * Math.sqrt(0.3 * 0.7 / nul[0]);
        boolean ok = lbReg > 0.30 && nr < 0.30 + 2.326 * Math.sqrt(0.21 / nul[0]);
        System.out.printf(Locale.ROOT, "REGIME forward %.4f (Wilson95 %.4f)  NULL forward %.4f  time %ds%n", rr, lbReg, nr, (System.currentTimeMillis() - t0) / 1000);
        System.out.println(ok ? "R13_JAVA_FOUNDRY_PASS" : "R13_JAVA_FOUNDRY_FAIL");
        if (!ok) System.exit(1);
    }
    static final class FoldStabilityLower { static double wilson(int k, int n) { if (n == 0) return 0; double p = k / (double) n, z = 1.6449, den = 1 + z * z / n; return (p + z * z / (2 * n) - z * Math.sqrt(p * (1 - p) / n + z * z / (4.0 * n * n))) / den; } }
}
