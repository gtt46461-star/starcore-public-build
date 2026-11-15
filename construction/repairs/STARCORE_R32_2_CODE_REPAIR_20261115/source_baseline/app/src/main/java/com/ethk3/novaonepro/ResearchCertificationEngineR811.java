package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Research-only certificate with eligible-opportunity denominators; never replaces Production B11. */
public final class ResearchCertificationEngineR811 {
    private ResearchCertificationEngineR811() {}

    public static double overallCoverage(List<Observation> rows) {
        return coverage(rows, "");
    }

    public static double domainCoverage(List<Observation> rows, String domain) {
        return coverage(rows, domain == null ? "" : domain);
    }

    public static double actionFrequencyCoverage(List<Observation> rows) {
        Set<String> eligible = new HashSet<String>();
        int actions = 0;
        if (rows != null)
            for (int i = 0; i < rows.size(); i++) {
                Observation row = rows.get(i);
                if (row == null || !row.valid()) continue;
                if (row.eligible) eligible.add(row.targetIssue);
                if (row.eligible && row.selected) actions++;
            }
        return eligible.isEmpty() ? 0.0d : ((double) actions) / eligible.size();
    }

    public static int eligibleIssuesForSelectedProof(int selectedProofN, double coverage) {
        if (selectedProofN <= 0
                || !(coverage > 0.0d && coverage <= 1.0d)
                || Double.isNaN(coverage)
                || Double.isInfinite(coverage)) return 0;
        return (int) Math.ceil(selectedProofN / coverage);
    }

    public static Certificate certifyCandidate(String candidateId, List<Observation> rows) {
        return certifyCandidate(candidateId, rows, ProofGuards.unverified());
    }

    public static Certificate certifyCandidate(
            String candidateId, List<Observation> rows, ProofGuards guards) {
        Map<String, Observation> selectedByIssue = new HashMap<String, Observation>();
        Set<String> conflictingIssues = new HashSet<String>();
        Set<String> eligibleIssues = new HashSet<String>();
        if (rows != null)
            for (int i = 0; i < rows.size(); i++) {
                Observation row = rows.get(i);
                if (row == null || !row.valid()) continue;
                if (row.eligible) eligibleIssues.add(row.targetIssue);
                if (row.eligible && row.selected) {
                    Observation existing = selectedByIssue.get(row.targetIssue);
                    if (existing == null && !conflictingIssues.contains(row.targetIssue)) {
                        selectedByIssue.put(row.targetIssue, row);
                    } else if (existing != null && !sameOutcome(existing, row)) {
                        selectedByIssue.remove(row.targetIssue);
                        conflictingIssues.add(row.targetIssue);
                    }
                }
            }
        ArrayList<Observation> selected =
                new ArrayList<Observation>(selectedByIssue.values());
        Collections.sort(
                selected,
                new Comparator<Observation>() {
                    public int compare(Observation left, Observation right) {
                        return TargetIssueResolver.compare(left.targetIssue, right.targetIssue);
                    }
                });
        int n = selected.size(), hit = 0;
        double brier = 0.0d;
        for (int i = 0; i < n; i++) {
            Observation row = selected.get(i);
            hit += row.label;
            brier += (row.probability - row.label) * (row.probability - row.label);
        }
        double hitRate = n == 0 ? 0.0d : ((double) hit) / n;
        double coverage = eligibleIssues.isEmpty() ? 0.0d : ((double) distinctSelected(selected)) / eligibleIssues.size();
        double ece = ece(selected);
        double wilson = wilsonLower(hit, n);
        int stableBlocks = stableBlocks(selected);
        ProofGuards proof = guards == null ? ProofGuards.unverified() : guards;
        boolean proven =
                n >= 480
                        && hitRate >= 0.45d
                        && wilson >= 0.40d
                        && proof.betaLower >= 0.40d
                        && coverage >= 0.15d
                        && ece <= 0.04d
                        && proof.championBrier >= 0.0d
                        && (n == 0 ? -1.0d : brier / n) <= proof.championBrier
                        && proof.coverageCv >= 0.0d
                        && proof.coverageCv <= 0.35d
                        && stableBlocks >= 3
                        && proof.blockMinimum >= 0.38d
                        && proof.regimePass
                        && proof.longestLosePass
                        && proof.resourcePass
                        && proof.frozenHoldoutPass
                        && proof.fdrPass
                        && proof.finalHoldoutFresh
                        && conflictingIssues.isEmpty();
        boolean shadowReady =
                n >= 240
                        && coverage >= 0.15d
                        && stableBlocks >= 3
                        && conflictingIssues.isEmpty();
        String phase =
                proven
                        ? "RESEARCH_PROVEN_45"
                        : (shadowReady
                                ? "SHADOW_OBSERVE"
                                : (n >= 120 ? "SCREENING" : "INSUFFICIENT_N"));
        return new Certificate(
                candidateId,
                n,
                hit,
                eligibleIssues.size(),
                hitRate,
                coverage,
                ece,
                n == 0 ? -1.0d : brier / n,
                wilson,
                stableBlocks,
                proof.betaLower,
                proof.coverageCv,
                proof.blockMinimum,
                conflictingIssues.size(),
                phase,
                proven ? "PROVEN" : "NOT_PROVEN");
    }

    private static boolean sameOutcome(Observation left, Observation right) {
        return left.label == right.label
                && Double.doubleToLongBits(left.probability)
                        == Double.doubleToLongBits(right.probability)
                && left.domain.equals(right.domain);
    }

    private static double coverage(List<Observation> rows, String domain) {
        Set<String> eligible = new HashSet<String>(), selected = new HashSet<String>();
        if (rows != null)
            for (int i = 0; i < rows.size(); i++) {
                Observation row = rows.get(i);
                if (row == null || !row.valid() || (!domain.isEmpty() && !domain.equals(row.domain))) continue;
                if (row.eligible) eligible.add(row.targetIssue);
                if (row.eligible && row.selected) selected.add(row.targetIssue);
            }
        return eligible.isEmpty() ? 0.0d : ((double) selected.size()) / eligible.size();
    }

    private static int distinctSelected(List<Observation> rows) {
        Set<String> out = new HashSet<String>(); for (int i = 0; i < rows.size(); i++) out.add(rows.get(i).targetIssue); return out.size();
    }

    private static double wilsonLower(int hit, int n) {
        if (n <= 0) return 0.0d;
        double z = 1.959963984540054d, p = ((double) hit) / n, z2 = z * z;
        return Math.max(0.0d, (p + z2 / (2.0d * n) - z * Math.sqrt((p * (1.0d - p) + z2 / (4.0d * n)) / n)) / (1.0d + z2 / n));
    }

    private static double ece(List<Observation> rows) {
        if (rows.isEmpty()) return 1.0d;
        int[] n = new int[10], hit = new int[10]; double[] p = new double[10];
        for (int i = 0; i < rows.size(); i++) {
            Observation row = rows.get(i); int b = Math.min(9, (int) Math.floor(row.probability * 10.0d));
            n[b]++; hit[b] += row.label; p[b] += row.probability;
        }
        double out = 0.0d;
        for (int i = 0; i < 10; i++) if (n[i] > 0) out += ((double) n[i]) / rows.size() * Math.abs(p[i] / n[i] - ((double) hit[i]) / n[i]);
        return out;
    }

    private static int stableBlocks(List<Observation> rows) {
        if (rows.size() < 4) return 0;
        int blocks = 4, stable = 0;
        for (int b = 0; b < blocks; b++) {
            int from = b * rows.size() / blocks, to = (b + 1) * rows.size() / blocks, hit = 0;
            for (int i = from; i < to; i++) hit += rows.get(i).label;
            if (to > from && ((double) hit) / (to - from) >= 0.40d) stable++;
        }
        return stable;
    }

    public static final class Observation {
        public final String targetIssue, domain;
        public final boolean eligible, selected;
        public final double probability;
        public final int label;
        public Observation(String issue, String domain, boolean eligible, boolean selected, double p, int y) {
            targetIssue = issue == null ? "" : issue; this.domain = domain == null ? "" : domain;
            this.eligible = eligible; this.selected = selected; probability = p; label = y;
        }
        boolean valid() { return TargetIssueResolver.isValid(targetIssue) && label >= 0 && label <= 1 && probability >= 0.0d && probability <= 1.0d; }
    }

    public static final class ProofGuards {
        public final double betaLower, championBrier, coverageCv, blockMinimum;
        public final boolean regimePass,
                longestLosePass,
                resourcePass,
                frozenHoldoutPass,
                fdrPass,
                finalHoldoutFresh;

        public ProofGuards(
                double betaLower,
                double championBrier,
                double coverageCv,
                double blockMinimum,
                boolean regimePass,
                boolean longestLosePass,
                boolean resourcePass,
                boolean frozenHoldoutPass,
                boolean fdrPass,
                boolean finalHoldoutFresh) {
            this.betaLower = finiteOrMissing(betaLower);
            this.championBrier = finiteOrMissing(championBrier);
            this.coverageCv = finiteOrMissing(coverageCv);
            this.blockMinimum = finiteOrMissing(blockMinimum);
            this.regimePass = regimePass;
            this.longestLosePass = longestLosePass;
            this.resourcePass = resourcePass;
            this.frozenHoldoutPass = frozenHoldoutPass;
            this.fdrPass = fdrPass;
            this.finalHoldoutFresh = finalHoldoutFresh;
        }

        public static ProofGuards unverified() {
            return new ProofGuards(-1.0d, -1.0d, -1.0d, -1.0d, false, false, false, false, false, false);
        }

        private static double finiteOrMissing(double value) {
            return Double.isNaN(value) || Double.isInfinite(value) ? -1.0d : value;
        }
    }

    public static final class Certificate {
        public final String candidateId, phase, proofStatus, checksum;
        public final int sampleN, hit, eligibleN, stableBlocks, conflictingIssueN;
        public final double hitRate,
                coverage,
                ece,
                brier,
                wilsonLower,
                betaLower,
                coverageCv,
                blockMinimum;
        Certificate(
                String id,
                int n,
                int hit,
                int eligible,
                double hr,
                double cov,
                double ece,
                double brier,
                double wilson,
                int stable,
                double beta,
                double coverageCv,
                double blockMinimum,
                int conflicts,
                String phase,
                String proofStatus) {
            candidateId = id == null ? "" : id; sampleN = n; this.hit = hit; eligibleN = eligible;
            hitRate = hr; coverage = cov; this.ece = ece; this.brier = brier; wilsonLower = wilson; stableBlocks = stable;
            betaLower = beta; this.coverageCv = coverageCv; this.blockMinimum = blockMinimum; conflictingIssueN = conflicts;
            this.phase = phase; this.proofStatus = proofStatus;
            checksum = V51ChecksumEngine.sha256(candidateId + '|' + n + '|' + hit + '|' + eligible + '|' + hr + '|' + cov + '|' + ece + '|' + brier + '|' + wilson + '|' + stable + '|' + beta + '|' + coverageCv + '|' + blockMinimum + '|' + conflicts + '|' + phase + '|' + proofStatus);
        }
    }
}
