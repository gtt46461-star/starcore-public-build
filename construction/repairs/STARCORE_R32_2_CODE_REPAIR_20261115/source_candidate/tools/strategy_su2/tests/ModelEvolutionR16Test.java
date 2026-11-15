package com.ethk3.novaonepro.strategyv2;

import java.nio.file.*;
import java.util.*;

/**
 * R16 Java foundry (R15 + regime-memory families REGMEM / RELMEM, P48) against R15 on the same data and seeds:
 *   REGIME (sticky recurring regimes: the case the R15 analysis found), NULL (no structure: no false edge).
 * Forward blocks [6000,8000) and [8000,10000), each after a campaign on draws before it; committees are scored only on
 * blocks they never saw. Pass: R16 REGIME F15 Wilson95 > 30% and >= R15 REGIME F15 - 1pt; NULL inside the 99% band of
 * chance in every tier; the regime-memory families were created and evaluated (some program in the population, hall of
 * fame or committees has a REGMEM / RELMEM member). Prints R16_JAVA_FOUNDRY_PASS.
 */
public final class ModelEvolutionR16Test {
    static int[] rolling(String name, int[][] d, long seed, boolean r16, List<String> used) {
        ModelReplaySU2.Library lib = new ModelReplaySU2.Library(d, Long.getLong("su2.libmb", 640L) << 20);
        ModelEvolutionSU2 evo = new ModelEvolutionSU2(lib, seed, 1500, 2500, 250, true, true, true, r16);
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
                System.out.printf(Locale.ROOT, "  %s %s checkpoint %d %s committee(%d) forward %d/%d = %.4f%n", r16 ? "R16" : "R15", name, c, t, cm.subs.size(), k, n, n == 0 ? 0 : k / (double) n);
            }
            String f = s.committees.get("F15").formula();
            System.out.println("  " + name + " checkpoint " + c + " campaign " + (System.currentTimeMillis() - t0) / 1000 + " s; F15 committee: " + f.substring(0, Math.min(260, f.length())));
        }
        Set<String> kinds = new TreeSet<>();
        List<ModelGenomeSU2> all = new ArrayList<>();
        for (ModelEvolutionSU2.Ranked r : s.population) all.add(r.g);
        for (ModelEvolutionSU2.Ranked r : s.hallOfFame) all.add(r.g);
        for (CommitteeSU2 c : s.committees.values()) all.addAll(c.subs);
        for (ModelGenomeSU2 g : all) for (ModelGenomeSU2.Member m : g.members) if (Arrays.asList(ModelGenomeSU2.R16_KINDS).contains(m.kind)) kinds.add(m.modelKey());
        int inCommittee = 0;
        for (CommitteeSU2 c : s.committees.values()) for (ModelGenomeSU2 g : c.subs) for (ModelGenomeSU2.Member m : g.members) if (Arrays.asList(ModelGenomeSU2.R16_KINDS).contains(m.kind)) inCommittee++;
        if (r16) { used.addAll(kinds); System.out.println("  " + name + " regime-memory members alive: " + kinds.size() + ", inside committees: " + inCommittee); }
        return tot;
    }

    public static void main(String[] a) throws Exception {
        Path w13 = Paths.get(a[0]);
        List<String> used = new ArrayList<>();
        int[] reg16 = rolling("REGIME", ModelEvolutionR14Test.load(w13.resolve("REGIME_seed1.txt")), 3, true, used);
        int[] reg15 = rolling("REGIME", ModelEvolutionR14Test.load(w13.resolve("REGIME_seed1.txt")), 3, false, new ArrayList<>());
        int[] nul = rolling("NULL", ModelEvolutionR14Test.load(w13.resolve("NULL_seed1.txt")), 3, true, used);
        boolean ok = true; String[] T = ModelEvolutionSU2.TIERS;
        for (int i = 0; i < 3; i++) {
            double r16 = rate(reg16, i), r15 = rate(reg15, i), rn = rate(nul, i);
            double nullUpper = 0.30 + 2.326 * Math.sqrt(0.21 / Math.max(1, nul[2 * i]));
            boolean nullOk = rn < nullUpper; ok &= nullOk;
            System.out.printf(Locale.ROOT, "%s REGIME R16 %.4f (n=%d, Wilson95 %.4f) vs R15 %.4f (n=%d) | NULL %.4f (n=%d, %s)%n",
                T[i], r16, reg16[2 * i], ModelEvolutionR14Test.wilson(reg16[2 * i + 1], reg16[2 * i]), r15, reg15[2 * i], rn, nul[2 * i], nullOk ? "no false edge" : "FALSE EDGE");
        }
        ok &= ModelEvolutionR14Test.wilson(reg16[1], reg16[0]) > 0.30;
        ok &= rate(reg16, 0) >= rate(reg15, 0) - 0.01;
        System.out.println("regime-memory members created and alive: " + used.size());
        for (int i = 0; i < Math.min(6, used.size()); i++) System.out.println("  " + used.get(i));
        ok &= !used.isEmpty();
        System.out.println(ok ? "R16_JAVA_FOUNDRY_PASS" : "R16_JAVA_FOUNDRY_FAIL");
        if (!ok) System.exit(1);
    }
    static double rate(int[] x, int i) { return x[2 * i] == 0 ? 0 : x[2 * i + 1] / (double) x[2 * i]; }
}
