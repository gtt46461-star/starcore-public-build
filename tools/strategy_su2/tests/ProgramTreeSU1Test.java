package com.ethk3.novaonepro.strategyv2;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Python-to-Java parity of GRAMMAR programs on real draws (reference/fixtures/grammar_parity.txt). */
public final class ProgramTreeSU1Test {
    private static int checks;
    private static void check(boolean ok, String message) { checks++; if (!ok) throw new AssertionError(message); }
    private static void rejects(Runnable r, String message) {
        checks++;
        try { r.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("not rejected: " + message);
    }

    public static void main(String[] args) throws Exception {
        List<String> lines = Files.readAllLines(Paths.get("reference/fixtures/grammar_parity.txt"), StandardCharsets.UTF_8);
        int i = 0;
        while (lines.get(i).startsWith("#")) i++;
        int n = Integer.parseInt(lines.get(i++).split(" ")[1]);
        int[][] draws = new int[n][10];
        for (int r = 0; r < n; r++) {
            String[] f = lines.get(i++).trim().split(" ");
            for (int p = 0; p < 10; p++) draws[r][p] = Integer.parseInt(f[p]);
        }
        Map<Integer, ProgramTreeSU1.Node> trees = new HashMap<>();
        Map<Integer, String> text = new HashMap<>();
        double worst = 0; int actionChecked = 0, actionSkippedNearTie = 0, values = 0;
        for (; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isEmpty()) continue;
            if (line.startsWith("TREE ")) {
                String[] f = line.split(" ", 3);
                trees.put(Integer.parseInt(f[1]), ProgramTreeSU1.parse(f[2]));
                text.put(Integer.parseInt(f[1]), f[2]);
                continue;
            }
            String[] f = line.split(" ");
            int id = Integer.parseInt(f[1]), t = Integer.parseInt(f[2]);
            int[] want = {Integer.parseInt(f[3]), Integer.parseInt(f[4]), Integer.parseInt(f[5]), Integer.parseInt(f[6])};
            double[][] got = trees.get(id).score(new ProgramTreeSU1.Context(draws, t));
            for (int k = 0; k < 100; k++) {
                double e = Double.parseDouble(f[7 + k]), g = got[k / 10][k % 10];
                double err = Math.abs(e - g) / (1.0 + Math.abs(e));
                worst = Math.max(worst, err);
                values++;
                if (err > 1e-4) throw new AssertionError("tree " + id + " t=" + t + " cell " + k + " python=" + e + " java=" + g + " :: " + text.get(id));
            }
            // Action parity, except when the top-2 position masses are within float32 noise.
            int[] act = ProgramTreeSU1.action(got);
            if (nearTie(got)) { actionSkippedNearTie++; continue; }
            check(act[0] == want[0], "position tree " + id + " t=" + t);
            java.util.Set<Integer> a = new java.util.HashSet<>(java.util.Arrays.asList(act[1], act[2], act[3]));
            java.util.Set<Integer> b = new java.util.HashSet<>(java.util.Arrays.asList(want[1], want[2], want[3]));
            check(a.equals(b), "K3 tree " + id + " t=" + t);
            actionChecked++;
        }
        checks += values;
        check(trees.size() >= 30, "tree count");
        check(actionChecked >= 100, "actions checked " + actionChecked);

        // Causality: changing rows >= t never changes the score at t.
        int t = 1200;
        int[][] altered = new int[n][];
        for (int r = 0; r < n; r++) altered[r] = draws[r].clone();
        for (int r = t; r < n; r++) { int[] row = altered[r]; int tmp = row[0]; row[0] = row[9]; row[9] = tmp; }
        for (Map.Entry<Integer, ProgramTreeSU1.Node> e : trees.entrySet()) {
            double[][] a = e.getValue().score(new ProgramTreeSU1.Context(draws, t));
            double[][] b = e.getValue().score(new ProgramTreeSU1.Context(altered, t));
            boolean same = true;
            for (int p = 0; p < 10; p++) for (int k = 0; k < 10; k++) same &= a[p][k] == b[p][k];
            check(same, "future rows changed score of tree " + e.getKey());
        }
        rejects(() -> ProgramTreeSU1.parse("ATOM(FREQUENCY,window=10)").score(new ProgramTreeSU1.Context(draws, 100)), "ATOM needs production lift");
        rejects(() -> ProgramTreeSU1.parse("ADD(EXPERT(1)"), "unbalanced");
        rejects(() -> ProgramTreeSU1.parse("FOO(1)"), "unknown op");
        rejects(() -> ProgramTreeSU1.parse("EXPERT(9)"), "expert range");
        System.out.println("ProgramTreeSU1Test PASS checks=" + checks + " values=" + values + " worst_rel_err=" + worst
                + " actions=" + actionChecked + " near_tie_skipped=" + actionSkippedNearTie
                + " scope=PURE_JAVA_REFERENCE_NOT_ANDROID");
    }

    private static boolean nearTie(double[][] s) {
        double[] mass = new double[10];
        for (int p = 0; p < 10; p++) {
            double mx = Double.NEGATIVE_INFINITY;
            for (int k = 0; k < 10; k++) mx = Math.max(mx, s[p][k]);
            double[] e = new double[10]; double sum = 0;
            for (int k = 0; k < 10; k++) { e[k] = Math.exp(s[p][k] - mx); sum += e[k]; }
            double[] sorted = e.clone(); java.util.Arrays.sort(sorted);
            mass[p] = (sorted[9] + sorted[8] + sorted[7]) / sum;
            // also a near tie between the 3rd and 4th number inside a position
            double g34 = Math.abs(s[p][idxOf(s[p], 2)] - s[p][idxOf(s[p], 3)]);
            if (g34 > 0 && g34 < 1e-5) return true;      // exact ties resolve identically on both sides
        }
        double[] m = mass.clone(); java.util.Arrays.sort(m);
        double gap = m[9] - m[8];
        return gap > 0 && gap < 1e-5;
    }

    private static int idxOf(double[] row, int rank) {
        Integer[] idx = new Integer[10];
        for (int k = 0; k < 10; k++) idx[k] = k;
        java.util.Arrays.sort(idx, (a, b) -> Double.compare(row[b] - b * 1e-6, row[a] - a * 1e-6));
        return idx[rank];
    }
}
