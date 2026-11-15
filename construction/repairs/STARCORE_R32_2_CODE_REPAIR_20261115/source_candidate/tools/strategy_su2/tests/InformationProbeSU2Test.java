package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * P50 information probes against the Python reference (fixture r16_probe: the 4,995 real report draws; nine family
 * probes, 7 blocks each): acts and hits at both gates identical, log-likelihood and predicted-hit sums within 1e-9
 * relative; a checkpoint written at period 3,210 and read into a fresh bank continues identically; the snapshot lines
 * parse and diagnose. Prints R16_PROBE_PARITY_PASS.
 */
public final class InformationProbeSU2Test {
    public static void main(String[] args) throws Exception {
        Path dir = Paths.get(args.length > 0 ? args[0] : "reference/fixtures/r16_probe");
        int[][] D = AtomNetSU2Test.draws(dir.resolve("draws.txt"));
        Map<String, List<String[]>> py = new LinkedHashMap<>();
        for (String l : Files.readAllLines(dir.resolve("blocks.tsv"))) { if (l.isEmpty()) continue; String[] f = l.split("\t"); py.computeIfAbsent(f[0], k -> new ArrayList<>()).add(f); }
        long t0 = System.nanoTime();
        InformationProbeSU2 bank = new InformationProbeSU2();
        long ckBytes = 0;
        for (int t = 0; t < D.length; t++) {
            bank.step(D[t]);
            if (t == 3209) {
                ByteArrayOutputStream bo = new ByteArrayOutputStream(); bank.write(new DataOutputStream(bo)); ckBytes = bo.size();
                InformationProbeSU2 r = new InformationProbeSU2(); r.read(new DataInputStream(new ByteArrayInputStream(bo.toByteArray()))); bank = r;
            }
        }
        double secs = (System.nanoTime() - t0) / 1e9;
        int blocks = 0, intMismatch = 0; double worstRel = 0;
        for (InformationProbeSU2.Probe p : bank.probes) {
            List<double[]> rs = bank.rows(p); List<String[]> e = py.get(p.family);
            if (e == null || e.size() != rs.size()) { System.out.println(p.family + ": block count java " + rs.size() + " python " + (e == null ? 0 : e.size())); intMismatch++; continue; }
            for (int i = 0; i < rs.size(); i++) {
                double[] b = rs.get(i); String[] f = e.get(i); blocks++;
                int[] ints = {1, 4, 5, 7, 8};      // n, a15, h15, a30, h30 (java index) <-> python columns 2,5,6,8,9
                int[] pcol = {2, 5, 6, 8, 9};
                if ((long) b[0] != Long.parseLong(f[1])) intMismatch++;
                for (int k = 0; k < ints.length; k++) if ((long) b[ints[k]] != Long.parseLong(f[pcol[k]])) { intMismatch++; System.out.println(p.family + " block " + f[1] + " col " + pcol[k] + ": java " + (long) b[ints[k]] + " python " + f[pcol[k]]); }
                int[] dj = {2, 3, 6, 9}, dp = {3, 4, 7, 10};
                for (int k = 0; k < dj.length; k++) worstRel = Math.max(worstRel, rel(b[dj[k]], Double.parseDouble(f[dp[k]])));
                if (!f[11].equals("-")) worstRel = Math.max(worstRel, rel(b[10], Double.parseDouble(f[11])));
                else if (!Double.isNaN(b[10])) intMismatch++;
            }
        }
        String snap = "SU2DIAG\t1\ndataset\tPROBE\nseq\t" + D.length + "\n" + bank.snapshotLines();
        UpgradeDiagnosisSU2.Diagnosis d = UpgradeDiagnosisSU2.diagnose(UpgradeDiagnosisSU2.parse(snap));
        boolean diagOk = d.probes.size() == 9 && d.probes.get("HMM").n == D.length - InformationProbeSU2.START;
        System.out.printf(Locale.ROOT, "information probes: 9 families x %d periods in %.1f s (%.2f ms/period); %d blocks, integer mismatches %d, worst relative diff %.2e; checkpoint %,d bytes; snapshot diagnosed: %s (S1 fired: %s)%n",
            D.length, secs, secs * 1000 / D.length, blocks, intMismatch, worstRel, ckBytes, diagOk, d.findings.stream().anyMatch(x -> x.code.equals("S1")));
        boolean ok = blocks == 63 && intMismatch == 0 && worstRel < 1e-9 && diagOk;
        System.out.println(ok ? "R16_PROBE_PARITY_PASS" : "R16_PROBE_PARITY_FAIL");
        if (!ok) System.exit(1);
    }
    static double rel(double a, double b) { return Math.abs(a - b) / Math.max(1e-12, Math.max(Math.abs(a), Math.abs(b))); }
}
