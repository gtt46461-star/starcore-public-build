package com.ethk3.novaonepro.strategyv2;

import java.nio.file.*;
import java.util.*;

/**
 * R14b Java foundry (ATOMNET members with atom-credit-guided children, open LOGLIN relations, empirical-Bayes judge,
 * family-capped consensus) against the R14 engine on the same data and seeds:
 *   HOTCOLD (held-out world: momentum / overdue regimes the six R13 families were not built for),
 *   REGIME (an R13 world: R14b must not lose it), NULL (no structure: no false edge).
 * Forward blocks [6000,8000) and [8000,10000), each after a campaign on draws before it; committees are scored only
 * on blocks they never saw. Pass: R14b HOTCOLD F15 Wilson95 > 30% and >= R14 HOTCOLD F15 - 1pt; REGIME F15 Wilson95
 * > 30%; NULL inside the 99% band of chance in every tier; at least one atom-credit-guided child note recorded.
 */
public final class ModelEvolutionR14bTest {
    static int[] rolling(String name, int[][] d, long seed, boolean r14b, List<String> notes) {
        ModelReplaySU2.Library lib = new ModelReplaySU2.Library(d, Long.getLong("su2.libmb", 640L) << 20);
        ModelEvolutionSU2 evo = new ModelEvolutionSU2(lib, seed, 1500, 2500, 250, true, r14b);
        ModelEvolutionSU2.State s = new ModelEvolutionSU2.State();
        int[] tot = new int[6];
        for (int c : new int[]{6000, 8000}) {
            long t0 = System.currentTimeMillis();
            s = evo.campaign(s, new ModelEvolutionSU2.Eras(1500, c - 1500, c - 1500, c), 8, 24, 12);
            int i = 0;
            for (String t : ModelEvolutionSU2.TIERS) {
                CommitteeSU2 cm = s.committees.get(t);
                ModelReplaySU2.Result r = CommitteeSU2.evaluate(cm, lib, 1500);
                int n = r.actions(c, c + 2000), k = r.hits(c, c + 2000); tot[2 * i] += n; tot[2 * i + 1] += k; i++;
                System.out.printf(Locale.ROOT, "  %s %s checkpoint %d %s committee(%d) forward %d/%d = %.4f%n", r14b ? "R14b" : "R14 ", name, c, t, cm.subs.size(), k, n, n == 0 ? 0 : k / (double) n);
            }
            String f = s.committees.get("F15").formula();
            System.out.println("  " + name + " checkpoint " + c + " campaign " + (System.currentTimeMillis() - t0) / 1000 + " s; F15 committee: " + f.substring(0, Math.min(260, f.length())));
        }
        for (String n : s.repairNotes) if (n.contains("CREDIT") || n.contains("ATOM") || n.contains("CROSSED")) notes.add(name + ": " + n);
        return tot;
    }

    public static void main(String[] a) throws Exception {
        Path w13 = Paths.get(a[0]), w14b = Paths.get(a[1]);
        List<String> notes = new ArrayList<>();
        int[] hcB = rolling("HOTCOLD", ModelEvolutionR14Test.load(w14b.resolve("HOTCOLD_seed1.txt")), 3, true, notes);
        int[] hcA = rolling("HOTCOLD", ModelEvolutionR14Test.load(w14b.resolve("HOTCOLD_seed1.txt")), 3, false, new ArrayList<>());
        int[] reg = rolling("REGIME", ModelEvolutionR14Test.load(w13.resolve("REGIME_seed1.txt")), 3, true, notes);
        int[] nul = rolling("NULL", ModelEvolutionR14Test.load(w13.resolve("NULL_seed1.txt")), 3, true, notes);
        boolean ok = true; String[] T = ModelEvolutionSU2.TIERS;
        for (int i = 0; i < 3; i++) {
            double rb = rate(hcB, i), ra = rate(hcA, i), rr = rate(reg, i), rn = rate(nul, i);
            double nullUpper = 0.30 + 2.326 * Math.sqrt(0.21 / Math.max(1, nul[2 * i]));
            boolean nullOk = rn < nullUpper; ok &= nullOk;
            System.out.printf(Locale.ROOT, "%s HOTCOLD R14b %.4f (n=%d, Wilson95 %.4f) vs R14 %.4f (n=%d) | REGIME R14b %.4f (n=%d, Wilson95 %.4f) | NULL %.4f (n=%d, %s)%n",
                T[i], rb, hcB[2 * i], ModelEvolutionR14Test.wilson(hcB[2 * i + 1], hcB[2 * i]), ra, hcA[2 * i], rr, reg[2 * i], ModelEvolutionR14Test.wilson(reg[2 * i + 1], reg[2 * i]), rn, nul[2 * i], nullOk ? "no false edge" : "FALSE EDGE");
        }
        ok &= ModelEvolutionR14Test.wilson(hcB[1], hcB[0]) > 0.30;
        ok &= rate(hcB, 0) >= rate(hcA, 0) - 0.01;
        ok &= ModelEvolutionR14Test.wilson(reg[1], reg[0]) > 0.30;
        System.out.println("atom-credit feedback notes: " + notes.size());
        for (int i = 0; i < Math.min(8, notes.size()); i++) System.out.println("  " + notes.get(i));
        ok &= !notes.isEmpty();
        System.out.println(ok ? "R14B_JAVA_FOUNDRY_PASS" : "R14B_JAVA_FOUNDRY_FAIL");
        if (!ok) System.exit(1);
    }
    static double rate(int[] x, int i) { return x[2 * i] == 0 ? 0 : x[2 * i + 1] / (double) x[2 * i]; }
}
