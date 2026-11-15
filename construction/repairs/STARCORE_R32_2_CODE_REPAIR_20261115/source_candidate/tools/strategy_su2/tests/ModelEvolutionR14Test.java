package com.ethk3.novaonepro.strategyv2;

import java.nio.file.*;
import java.util.*;

/** R14 Java foundry (calibrated programs, consensus judge, committee champions): on the REGIME world the committees
 *  must show a forward edge (Wilson lower > 30%) on blocks never seen during creation; on the NULL world they must not
 *  claim one. Forward blocks [6000,8000) and [8000,10000), each after a campaign on draws before it. */
public final class ModelEvolutionR14Test {
    static int[][] load(Path p) throws Exception {
        List<String> lines = Files.readAllLines(p); int[][] d = new int[lines.size()][10];
        for (int t = 0; t < d.length; t++) { String[] f = lines.get(t).trim().split("\\s+"); for (int q = 0; q < 10; q++) d[t][q] = Integer.parseInt(f[q]); }
        return d;
    }
    static double wilson(int k, int n) { if (n == 0) return 0; double p = k / (double) n, z = 1.6449, den = 1 + z * z / n; return (p + z * z / (2 * n) - z * Math.sqrt(p * (1 - p) / n + z * z / (4.0 * n * n))) / den; }

    static int[] rolling(String name, int[][] d, long seed) {
        ModelReplaySU2.Library lib = new ModelReplaySU2.Library(d, 640L << 20);
        ModelEvolutionSU2 evo = new ModelEvolutionSU2(lib, seed, 1500, 2500, 250, true);
        ModelEvolutionSU2.State s = new ModelEvolutionSU2.State();
        int[] tot = new int[6];                                            // F15 n,k  F30 n,k  F100 n,k
        for (int c : new int[]{6000, 8000}) {
            long t0 = System.currentTimeMillis();
            s = evo.campaign(s, new ModelEvolutionSU2.Eras(1500, c - 1500, c - 1500, c), 8, 24, 12);
            int i = 0;
            for (String t : ModelEvolutionSU2.TIERS) {
                CommitteeSU2 cm = s.committees.get(t);
                ModelReplaySU2.Result r = CommitteeSU2.evaluate(cm, lib, 1500);
                int n = r.actions(c, c + 2000), k = r.hits(c, c + 2000); tot[2 * i] += n; tot[2 * i + 1] += k; i++;
                System.out.printf(Locale.ROOT, "  %s checkpoint %d %s committee(%d) forward %d/%d = %.4f%n", name, c, t, cm.subs.size(), k, n, n == 0 ? 0 : k / (double) n);
            }
            System.out.println("  " + name + " checkpoint " + c + " campaign " + (System.currentTimeMillis() - t0) / 1000 + " s, effectiveGenerations=" + s.effectiveGenerations
                + " F15 committee: " + s.committees.get("F15").formula().substring(0, Math.min(220, s.committees.get("F15").formula().length())));
        }
        return tot;
    }

    public static void main(String[] a) throws Exception {
        Path dir = Paths.get(a[0]);
        int[] reg = rolling("REGIME", load(dir.resolve("REGIME_seed1.txt")), 3), nul = rolling("NULL", load(dir.resolve("NULL_seed1.txt")), 3);
        boolean ok = true; String[] T = ModelEvolutionSU2.TIERS;
        double bestReg = 0;
        for (int i = 0; i < 3; i++) {
            double wr = wilson(reg[2 * i + 1], reg[2 * i]), rn = nul[2 * i] == 0 ? 0 : nul[2 * i + 1] / (double) nul[2 * i];
            double nullUpper = 0.30 + 2.326 * Math.sqrt(0.21 / Math.max(1, nul[2 * i]));
            bestReg = Math.max(bestReg, wr);
            boolean nullOk = rn < nullUpper;
            ok &= nullOk;
            System.out.printf(Locale.ROOT, "%s REGIME %.4f (n=%d, Wilson95 %.4f) | NULL %.4f (n=%d, %s)%n", T[i], reg[2 * i] == 0 ? 0 : reg[2 * i + 1] / (double) reg[2 * i], reg[2 * i], wr, rn, nul[2 * i], nullOk ? "no false edge" : "FALSE EDGE");
        }
        ok &= bestReg > 0.30;
        System.out.println(ok ? "R14_JAVA_FOUNDRY_PASS" : "R14_JAVA_FOUNDRY_FAIL");
        if (!ok) System.exit(1);
    }
}
