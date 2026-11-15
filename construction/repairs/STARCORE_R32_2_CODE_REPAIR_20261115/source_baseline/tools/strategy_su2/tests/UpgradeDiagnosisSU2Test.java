package com.ethk3.novaonepro.strategyv2;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * P50 upgrade diagnosis against the Python reference (fixtures r16_diag: real report data, its time-shuffled control,
 * the regime and cross-position worlds; 3-5 successive diagnoses each, the ledger persisted and reloaded in between):
 * every UPGRADE_DIAGNOSIS.md and .json byte-identical, the final ledger text identical. Then the bounds that keep the
 * report and its memory finite however long the phone runs: 5,000 diagnoses with up to 30 findings from 600 issue keys,
 * 10,000 campaigns on the generation curve, 8 snapshots, an oversized snapshot -> every limit holds and what must be
 * kept is kept (open P0 issues, first / latest 64 / best campaign, exact campaign count and sums). Shadow challengers:
 * ADD_MEMBER and TIER_COVERAGE produce canonical committees. Prints R16_DIAG_PARITY_PASS.
 */
public final class UpgradeDiagnosisSU2Test {
    static String read(Path p) throws Exception { return new String(Files.readAllBytes(p), StandardCharsets.UTF_8); }

    public static void main(String[] args) throws Exception {
        Path dir = Paths.get(args.length > 0 ? args[0] : "reference/fixtures/r16_diag");
        TreeSet<String> tags = new TreeSet<>();
        for (File f : Objects.requireNonNull(dir.toFile().listFiles())) if (f.getName().endsWith("_0.snapshot.txt")) tags.add(f.getName().replace("_0.snapshot.txt", ""));
        int reports = 0, mismatches = 0; long t0 = System.nanoTime();
        for (String tag : tags) {
            UpgradeDiagnosisSU2.Ledger L = new UpgradeDiagnosisSU2.Ledger();
            for (int i = 0; Files.exists(dir.resolve(tag + "_" + i + ".snapshot.txt")); i++) {
                String snap = read(dir.resolve(tag + "_" + i + ".snapshot.txt"));
                UpgradeDiagnosisSU2.Snapshot S = UpgradeDiagnosisSU2.parse(snap);
                UpgradeDiagnosisSU2.Diagnosis D = UpgradeDiagnosisSU2.diagnose(S);
                L = UpgradeDiagnosisSU2.Ledger.fromText(L.text(), L.snaps);              // persisted between diagnoses
                L.update(D.findings, S.seq); L.addSnapshot(S.seq, snap);
                String md = UpgradeDiagnosisSU2.render(D, L), js = UpgradeDiagnosisSU2.toJson(D, L);
                String pmd = read(dir.resolve(tag + "_" + i + ".md")), pjs = read(dir.resolve(tag + "_" + i + ".json"));
                reports++;
                if (!md.equals(pmd)) { mismatches++; firstDiff(tag + "_" + i + ".md", md, pmd); }
                if (!js.equals(pjs)) { mismatches++; firstDiff(tag + "_" + i + ".json", js, pjs); }
            }
            String pl = read(dir.resolve(tag + ".ledger.txt"));
            if (!L.text().equals(pl)) { mismatches++; firstDiff(tag + ".ledger.txt", L.text(), pl); }
            System.out.println(tag + ": " + L.entries.size() + " ledger entries, verdict and findings identical");
        }
        System.out.printf(Locale.ROOT, "parity: %d reports over %d datasets, %d mismatches, %.1f ms/report%n", reports, tags.size(), mismatches, (System.nanoTime() - t0) / 1e6 / Math.max(1, reports));

        // ---------------------------------------------------------------- bounds
        boolean ok = mismatches == 0 && reports >= 12;
        Random rng = new Random(50);
        UpgradeDiagnosisSU2.Ledger L = new UpgradeDiagnosisSU2.Ledger();
        String[] codes = {"G1", "G2", "G3", "S1", "S2", "S3", "Q1", "Q2", "E1", "E2", "M1", "S7"};
        String[] sevs = {"P0", "P1", "P2", "INFO"};
        int maxEntries = 0, maxLedgerBytes = 0; boolean p0Kept = true;
        for (int r = 1; r <= 5000; r++) {
            List<UpgradeDiagnosisSU2.Finding> fs = new ArrayList<>(); Set<String> seen = new HashSet<>();
            int n = 1 + rng.nextInt(30);
            for (int j = 0; j < n; j++) {
                String code = codes[rng.nextInt(codes.length)], scope = "s" + rng.nextInt(50), sev = sevs[rng.nextInt(4)];
                if (!seen.add(code + "|" + scope)) continue;
                fs.add(new UpgradeDiagnosisSU2.Finding(code, sev, scope, rng.nextDouble(), rng.nextBoolean() ? "ADD_MEMBER X{}" : "", new HashMap<>()));
            }
            L = UpgradeDiagnosisSU2.Ledger.fromText(L.text(), L.snaps);
            L.update(fs, r * 500L);
            for (UpgradeDiagnosisSU2.Finding f : fs) if (f.sev.equals("P0") && !L.entries.containsKey(f.code + "|" + f.scope)) p0Kept = false;   // current P0 never evicted
            maxEntries = Math.max(maxEntries, L.entries.size());
            maxLedgerBytes = Math.max(maxLedgerBytes, L.text().getBytes(StandardCharsets.UTF_8).length);
        }
        System.out.printf(Locale.ROOT, "ledger: 5,000 diagnoses x up to 30 findings from 600 keys -> max %d entries (cap %d), max %,d bytes, current P0 always kept: %s%n",
            maxEntries, UpgradeDiagnosisSU2.LEDGER_CAP, maxLedgerBytes, p0Kept);
        ok &= maxEntries <= UpgradeDiagnosisSU2.LEDGER_CAP && maxLedgerBytes <= 262144 && p0Kept;

        UpgradeDiagnosisSU2.Ledger C = new UpgradeDiagnosisSU2.Ledger();
        double sum1 = 0, bestR = -1; long bestSeq = -1;
        for (int c = 0; c < 10000; c++) {
            double r0 = 0.25 + 0.1 * rng.nextDouble(), r1 = 0.25 + 0.1 * rng.nextDouble() + (c == 7777 ? 0.2 : 0);
            C.addCampaign(c, r0, r1); sum1 += UpgradeDiagnosisSU2.r9(r1);
            if (UpgradeDiagnosisSU2.r9(r1) > bestR) { bestR = UpgradeDiagnosisSU2.r9(r1); bestSeq = c; }
        }
        long w = 0; double s1 = 0; boolean bestKept = false, recent = true;
        for (double[] p : C.curve) { w += (long) p[1]; s1 += p[3]; if ((long) p[0] == bestSeq && p[1] == 1 && p[3] == bestR) bestKept = true; }
        for (int i = 0; i < 64; i++) { double[] p = C.curve.get(C.curve.size() - 1 - i); recent &= (long) p[0] == 9999 - i && p[1] == 1; }
        boolean first = C.curve.get(0)[0] == 0 && C.curve.get(0)[1] == 1;
        System.out.printf(Locale.ROOT, "generation curve: 10,000 campaigns -> %d points (cap %d), weights sum %d, r1 sum diff %.2e, first kept %s, latest 64 kept %s, best campaign (#%d) kept %s%n",
            C.curve.size(), UpgradeDiagnosisSU2.CURVE_CAP, w, Math.abs(s1 - sum1), first, recent, bestSeq, bestKept);
        ok &= C.curve.size() <= UpgradeDiagnosisSU2.CURVE_CAP && w == 10000 && Math.abs(s1 - sum1) < 1e-4 && first && recent && bestKept;

        StringBuilder big = new StringBuilder("SU2DIAG\t1\ndataset\tBIG\nseq\t1\n");
        for (int i = 0; i < 2000; i++) big.append("teacher\tmember").append(i % 40).append("\tSTATE_").append(i).append("_WITH_A_LONG_NAME\t-0.1\t0.2\n");
        UpgradeDiagnosisSU2.Ledger B = new UpgradeDiagnosisSU2.Ledger();
        for (int i = 0; i < 20; i++) B.addSnapshot(i, big.toString());
        boolean snapsOk = B.snaps.size() == UpgradeDiagnosisSU2.SNAP_KEEP;
        for (Object[] s : B.snaps) snapsOk &= ((String) s[1]).getBytes(StandardCharsets.UTF_8).length <= UpgradeDiagnosisSU2.SNAP_MAX_BYTES;
        StringBuilder bigReport = new StringBuilder("SU2DIAG\t1\ndataset\tBIG\nseq\t1\ntrack\tF15\t1000\t300:100\n");
        for (int i = 0; i < 4000; i++) bigReport.append("teacher\tmember").append(i % 40).append("\tSTATE_").append(i).append("\t-0.1\t0.2\n");
        UpgradeDiagnosisSU2.Diagnosis D = UpgradeDiagnosisSU2.diagnose(UpgradeDiagnosisSU2.parse(bigReport.toString()));
        String md = UpgradeDiagnosisSU2.render(D, B);
        int mdBytes = md.getBytes(StandardCharsets.UTF_8).length;
        System.out.printf(Locale.ROOT, "snapshots: 20 added -> %d kept, each <= 16 KB: %s; oversized report -> %,d bytes (cap %,d), truncation note: %s%n",
            B.snaps.size(), snapsOk, mdBytes, UpgradeDiagnosisSU2.MD_MAX_BYTES, md.contains("64 KB"));
        ok &= snapsOk && mdBytes <= UpgradeDiagnosisSU2.MD_MAX_BYTES && md.contains("64 KB");

        String champ = "SU2C|COV=0.15|G=SU2|CAL=1|POOL=STATIC|SINK=0|GATE=MASS|COV=0.15|M=HMM{K=3,lr=0.01,seed=1,stay=0.995,temper=0.6}*1";
        String s1t = UpgradeDiagnosisSU2.shadowText("ADD_MEMBER REGMEM{K=3,lr=0.01,seed=1,stay=0.995,temper=0.6}", champ);
        String s2t = UpgradeDiagnosisSU2.shadowText("TIER_COVERAGE F15 0.30", champ);
        boolean shadowOk = s1t != null && CommitteeSU2.parse(s1t).subs.size() == 2 && s2t != null && CommitteeSU2.parse(s2t).coverage == 0.30
            && UpgradeDiagnosisSU2.shadowText("JUDGE_KAPPA x2", champ) == null
            && UpgradeDiagnosisSU2.shadowOutcome(600, 230, 600, 180).equals("EFFECTIVE") && UpgradeDiagnosisSU2.shadowOutcome(599, 230, 600, 180).equals("PENDING");
        System.out.println("shadow challengers: " + s1t + " | " + s2t + " -> " + shadowOk);
        ok &= shadowOk;
        System.out.println(ok ? "R16_DIAG_PARITY_PASS" : "R16_DIAG_PARITY_FAIL");
        if (!ok) System.exit(1);
    }

    static void firstDiff(String name, String a, String b) {
        int i = 0; while (i < Math.min(a.length(), b.length()) && a.charAt(i) == b.charAt(i)) i++;
        System.out.println("MISMATCH " + name + " at " + i + ": java [" + a.substring(Math.max(0, i - 60), Math.min(a.length(), i + 60)).replace("\n", "\\n")
            + "] python [" + b.substring(Math.max(0, i - 60), Math.min(b.length(), i + 60)).replace("\n", "\\n") + "]");
    }
}
