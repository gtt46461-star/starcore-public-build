package com.ethk3.novaonepro;

import java.util.ArrayList;

/** Read-only mathematical and artifact sanity audit for B14/B15. */
public final class FormulaAlgorithmAuditEngine {
    public static final String OWNER = "B14_FORMULA_ALGORITHM_AUDIT";
    public static final String VERSION = "V57_FORMULA_ALGORITHM_AUDIT_FULL_20260808";

    public enum Severity {
        INFO,
        WATCH,
        HARD_BLOCK,
        UNAVAILABLE
    }

    public static final class Finding {
        public final String code, formula, observed, why, repair;
        public final Severity severity;
        public final String checksum;

        Finding(
                String code,
                Severity severity,
                String formula,
                String observed,
                String why,
                String repair) {
            this.code = s(code);
            this.severity = severity;
            this.formula = s(formula);
            this.observed = s(observed);
            this.why = s(why);
            this.repair = s(repair);
            checksum =
                    V51ChecksumEngine.sha256(
                            this.code
                                    + '|'
                                    + severity.name()
                                    + '|'
                                    + this.formula
                                    + '|'
                                    + this.observed
                                    + '|'
                                    + this.why
                                    + '|'
                                    + this.repair);
        }
    }

    public static final class Report {
        public final Finding[] findings;
        public final int blocks, warnings;
        public final String verdict, checksum;

        Report(Finding[] f) {
            findings = f;
            int b = 0, w = 0;
            StringBuilder h = new StringBuilder();
            for (int i = 0; i < f.length; i++) {
                if (f[i].severity == Severity.HARD_BLOCK) b++;
                if (f[i].severity == Severity.WATCH || f[i].severity == Severity.UNAVAILABLE) w++;
                h.append('|').append(f[i].checksum);
            }
            blocks = b;
            warnings = w;
            verdict = b > 0 ? "HARD_BLOCK" : (w > 0 ? "WATCH" : "PASS");
            checksum = V51ChecksumEngine.sha256(verdict + '|' + b + '|' + w + h.toString());
        }
    }

    public Report audit(DbStore store) {
        ArrayList<Finding> f = new ArrayList<Finding>();
        f.add(
                new Finding(
                        "K3_ACTION_COMBINATORICS",
                        V51PositionActionBuilder.TOTAL_ACTIONS == 1200
                                ? Severity.INFO
                                : Severity.HARD_BLOCK,
                        "10*C(10,3)=1200",
                        "TOTAL_ACTIONS=" + V51PositionActionBuilder.TOTAL_ACTIONS,
                        "Reduced action space makes rank and selection statistics incomparable",
                        "Require exactly 1200 unique position+K3 actions every target issue."));
        f.add(
                new Finding(
                        "FIXED_K",
                        Severity.INFO,
                        "K=3",
                        "single-position fixed K3 contract",
                        "Dynamic K changes the estimand and violates settlement semantics",
                        "Keep fixed K=3 for REAL and SHADOW comparison."));
        f.add(
                new Finding(
                        "DEFERRED_SLICE_UNIVERSE",
                        V51PositionActionBuilder.TOTAL_ACTIONS == 1200
                                        && AppConfig.R36_DEFERRED_ACTION_CHUNK > 0
                                        && AppConfig.R36_DEFERRED_ACTION_CHUNK < 1200
                                ? Severity.INFO
                                : Severity.HARD_BLOCK,
                        "slice<1200 AND final action universe=1200",
                        "slice="
                                + AppConfig.R36_DEFERRED_ACTION_CHUNK
                                + "|TOTAL_ACTIONS="
                                + V51PositionActionBuilder.TOTAL_ACTIONS,
                        "Persistence slicing must never shrink the formal action universe",
                        "Restore exact 1200 verification and cursor-only slicing."));
        String parity = get(store, "r811_strategy_parity", "NOT_RUN");
        f.add(
                new Finding(
                        "STRATEGY_PARITY_CERTIFICATE",
                        "PASS".equals(parity) ? Severity.INFO : Severity.UNAVAILABLE,
                        "baseline output fingerprint == R811 output fingerprint",
                        parity,
                        "Runtime/diagnostic changes cannot silently drift B04-B12 decisions",
                        "Run the frozen parity fixtures before release."));
        f.add(
                new Finding(
                        "NUMBER_ARTIFACT",
                        V51NumberModelArtifact.PROVEN ? Severity.INFO : Severity.WATCH,
                        "Q(position,number|history<t)",
                        "PROVEN="
                                + V51NumberModelArtifact.PROVEN
                                + "|DATA="
                                + V51NumberModelArtifact.DATA_SHA256,
                        "An untrained Q can be computed for research but cannot carry a calibrated"
                            + " probability claim",
                        "Train/freeze on time-ordered data and record immutable data/artifact"
                            + " hash."));
        f.add(
                new Finding(
                        "RANK_ARTIFACT",
                        V51ActionRankerArtifact.PROVEN && V51ActionRankerArtifact.RANK_ORDER_PASSED
                                ? Severity.INFO
                                : Severity.WATCH,
                        "rank(a within same target issue)",
                        "PROVEN="
                                + V51ActionRankerArtifact.PROVEN
                                + "|ORDER="
                                + V51ActionRankerArtifact.RANK_ORDER_PASSED,
                        "Ranking must be groupwise across all 1200 actions and stable after freeze",
                        "Verify Rank1/MRR/NDCG/pairwise and champion immutability."));
        f.add(
                new Finding(
                        "SELECTED_HIT_P",
                        V51SelectedCalibrationArtifact.PROVEN ? Severity.INFO : Severity.WATCH,
                        "P(hit | selected, pre-draw features)",
                        "PROVEN="
                                + V51SelectedCalibrationArtifact.PROVEN
                                + "|DATA="
                                + V51SelectedCalibrationArtifact.DATA_SHA256,
                        "Raw rank score or hand-crafted formula is not a calibrated SelectedHitP",
                        "Fit only on pre-draw OOS selected-action ledger; report"
                            + " ECE/Brier/Wilson/subgroups."));
        f.add(
                new Finding(
                        "POLICY_GATE",
                        V51PolicySurfaceArtifact.MODEL_GATE_PASSED ? Severity.INFO : Severity.WATCH,
                        "BET=1 only if frozen model+certificate+policy pass",
                        "MODEL_GATE_PASSED=" + V51PolicySurfaceArtifact.MODEL_GATE_PASSED,
                        "Frequency must emerge from evidence; it cannot be debt-filled",
                        "Keep REAL locked until strict model/certificate chain passes."));

        String brier = get(store, "strict_oos_brier", "");
        String ece = get(store, "strict_oos_ece", "");
        String coverage = get(store, "strict_oos_coverage", "");
        String hit = get(store, "strict_oos_hit_rate", "");
        if (brier.length() == 0
                && ece.length() == 0
                && coverage.length() == 0
                && hit.length() == 0) {
            f.add(
                    new Finding(
                            "STRICT_OOS_METRICS",
                            Severity.UNAVAILABLE,
                            "Brier=mean((p-y)^2); ECE=sum(w_b*|acc_b-conf_b|)",
                            "NO_FROZEN_OOS_METRICS",
                            "Without frozen OOS metrics the software must not claim 40/50/60%",
                            "Generate immutable time-ordered OOS evidence with sample count and"
                                + " confidence interval."));
        } else {
            f.add(
                    new Finding(
                            "STRICT_OOS_METRICS",
                            Severity.INFO,
                            "Brier/ECE/Coverage/Hit",
                            "Brier="
                                    + brier
                                    + "|ECE="
                                    + ece
                                    + "|Coverage="
                                    + coverage
                                    + "|Hit="
                                    + hit,
                            "Metrics are meaningful only with frozen pre-draw sample identity",
                            "Cross-check data hash, issue range, sample count, lane filter and"
                                + " settlement identity."));
        }

        String sinkhorn = get(store, "v51_sinkhorn_summary", "");
        f.add(
                new Finding(
                        "SINKHORN_10X10",
                        sinkhorn.length() == 0 ? Severity.UNAVAILABLE : Severity.INFO,
                        "row sums≈1 and column sums≈1",
                        "state=" + (sinkhorn.length() == 0 ? "N/A" : sinkhorn),
                        "Doubly-stochastic normalization must remain finite and reproducible",
                        "Persist max row/column error and fallback reason for each target issue."));

        String leakage = get(store, "pit_leakage_count", "");
        if (leakage.length() == 0)
            f.add(
                    new Finding(
                            "PIT_LEAKAGE",
                            Severity.UNAVAILABLE,
                            "max(history.issue)<targetIssue",
                            "N/A",
                            "Future leakage invalidates every offline hit-rate claim",
                            "Persist per-target PIT certificate and make any leakage a hard"
                                + " block."));
        else
            f.add(
                    new Finding(
                            "PIT_LEAKAGE",
                            "0".equals(leakage) ? Severity.INFO : Severity.HARD_BLOCK,
                            "max(history.issue)<targetIssue",
                            "leakage_count=" + leakage,
                            "Any positive leakage invalidates affected samples",
                            "Purge affected features/caches, replay and regenerate OOS evidence."));

        return new Report(f.toArray(new Finding[f.size()]));
    }

    public String auditProbability(String owner, double p) {
        if (Double.isNaN(p) || Double.isInfinite(p)) return "HARD_BLOCK|" + owner + "|NON_FINITE";
        if (p < 0.0d || p > 1.0d) return "HARD_BLOCK|" + owner + "|OUT_OF_RANGE=" + p;
        return "PASS|" + owner + "|p=" + p;
    }

    public String auditMatrix(double[][] q, double tolerance) {
        if (q == null || q.length != 10) return "HARD_BLOCK|MATRIX_ROWS";
        double maxRow = 0.0d, maxCol = 0.0d;
        int bad = 0;
        for (int i = 0; i < 10; i++) {
            if (q[i] == null || q[i].length != 10) {
                bad++;
                continue;
            }
            double sum = 0.0d;
            for (int j = 0; j < 10; j++) {
                double x = q[i][j];
                if (Double.isNaN(x) || Double.isInfinite(x) || x < 0.0d) bad++;
                else sum += x;
            }
            maxRow = Math.max(maxRow, Math.abs(sum - 1.0d));
        }
        for (int j = 0; j < 10; j++) {
            double sum = 0.0d;
            for (int i = 0; i < 10; i++) if (q[i] != null && q[i].length == 10) sum += q[i][j];
            maxCol = Math.max(maxCol, Math.abs(sum - 1.0d));
        }
        return (bad == 0 && maxRow <= tolerance && maxCol <= tolerance ? "PASS" : "HARD_BLOCK")
                + "|bad="
                + bad
                + "|maxRowError="
                + maxRow
                + "|maxColError="
                + maxCol;
    }

    public static double brier(double[] p, int[] y) {
        if (p == null || y == null || p.length == 0 || p.length != y.length) return Double.NaN;
        double s = 0.0d;
        for (int i = 0; i < p.length; i++) {
            double d = p[i] - y[i];
            s += d * d;
        }
        return s / p.length;
    }

    public static double wilsonLower(int hit, int n, double z) {
        if (n <= 0) return Double.NaN;
        double ph = (double) hit / (double) n;
        double z2 = z * z;
        double den = 1.0d + z2 / n;
        double center = ph + z2 / (2.0d * n);
        double rad = z * Math.sqrt((ph * (1.0d - ph) + z2 / (4.0d * n)) / n);
        return Math.max(0.0d, (center - rad) / den);
    }

    public String render(DbStore store) {
        Report r = audit(store);
        StringBuilder b = new StringBuilder(8192);
        b.append("【公式/算法/概率审计】\nVersion=")
                .append(VERSION)
                .append("\nVerdict=")
                .append(r.verdict)
                .append("|Blocks=")
                .append(r.blocks)
                .append("|Warnings=")
                .append(r.warnings)
                .append('\n');
        for (int i = 0; i < r.findings.length; i++) {
            Finding x = r.findings[i];
            b.append(i + 1)
                    .append('.')
                    .append(x.code)
                    .append('|')
                    .append(x.severity.name())
                    .append("\nFormula=")
                    .append(x.formula)
                    .append("\nObserved=")
                    .append(x.observed)
                    .append("\nWhy=")
                    .append(x.why)
                    .append("\nRepair=")
                    .append(x.repair)
                    .append("\nHash=")
                    .append(x.checksum)
                    .append("\n\n");
        }
        b.append("ReportHash=").append(r.checksum).append('\n');
        return b.toString();
    }

    private static String get(DbStore s, String k, String d) {
        try {
            return s == null ? d : s.getState(k, d);
        } catch (Throwable t) {
            return d;
        }
    }

    private static String s(String x) {
        return x == null ? "" : x;
    }

public String renderSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        Report r = auditSnapshotCP4(report, store);
        com.ethk3.novaonepro.ReportTextCP12 b = new com.ethk3.novaonepro.ReportTextCP12(report, 8192);
        b.append("【公式/算法/概率审计】\nVersion=")
                .append(VERSION)
                .append("\nVerdict=")
                .append(r.verdict)
                .append("|Blocks=")
                .append(r.blocks)
                .append("|Warnings=")
                .append(r.warnings)
                .append('\n');
        for (int i = 0; i < r.findings.length; i++) {
            Finding x = r.findings[i];
            b.append(i + 1)
                    .append('.')
                    .append(x.code)
                    .append('|')
                    .append(x.severity.name())
                    .append("\nFormula=")
                    .append(x.formula)
                    .append("\nObserved=")
                    .append(x.observed)
                    .append("\nWhy=")
                    .append(x.why)
                    .append("\nRepair=")
                    .append(x.repair)
                    .append("\nHash=")
                    .append(x.checksum)
                    .append("\n\n");
        }
        b.append("ReportHash=").append(r.checksum).append('\n');
        return b.toString();
    }

public Report auditSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        ArrayList<Finding> f = new ArrayList<Finding>();
        f.add(
                new Finding(
                        "K3_ACTION_COMBINATORICS",
                        V51PositionActionBuilder.TOTAL_ACTIONS == 1200
                                ? Severity.INFO
                                : Severity.HARD_BLOCK,
                        "10*C(10,3)=1200",
                        "TOTAL_ACTIONS=" + V51PositionActionBuilder.TOTAL_ACTIONS,
                        "Reduced action space makes rank and selection statistics incomparable",
                        "Require exactly 1200 unique position+K3 actions every target issue."));
        f.add(
                new Finding(
                        "FIXED_K",
                        Severity.INFO,
                        "K=3",
                        "single-position fixed K3 contract",
                        "Dynamic K changes the estimand and violates settlement semantics",
                        "Keep fixed K=3 for REAL and SHADOW comparison."));
        f.add(
                new Finding(
                        "DEFERRED_SLICE_UNIVERSE",
                        V51PositionActionBuilder.TOTAL_ACTIONS == 1200
                                        && AppConfig.R36_DEFERRED_ACTION_CHUNK > 0
                                        && AppConfig.R36_DEFERRED_ACTION_CHUNK < 1200
                                ? Severity.INFO
                                : Severity.HARD_BLOCK,
                        "slice<1200 AND final action universe=1200",
                        "slice="
                                + AppConfig.R36_DEFERRED_ACTION_CHUNK
                                + "|TOTAL_ACTIONS="
                                + V51PositionActionBuilder.TOTAL_ACTIONS,
                        "Persistence slicing must never shrink the formal action universe",
                        "Restore exact 1200 verification and cursor-only slicing."));
        String parity = getSnapshotCP4(report, store, "r811_strategy_parity", "NOT_RUN");
        f.add(
                new Finding(
                        "STRATEGY_PARITY_CERTIFICATE",
                        "PASS".equals(parity) ? Severity.INFO : Severity.UNAVAILABLE,
                        "baseline output fingerprint == R811 output fingerprint",
                        parity,
                        "Runtime/diagnostic changes cannot silently drift B04-B12 decisions",
                        "Run the frozen parity fixtures before release."));
        f.add(
                new Finding(
                        "NUMBER_ARTIFACT",
                        V51NumberModelArtifact.PROVEN ? Severity.INFO : Severity.WATCH,
                        "Q(position,number|history<t)",
                        "PROVEN="
                                + V51NumberModelArtifact.PROVEN
                                + "|DATA="
                                + V51NumberModelArtifact.DATA_SHA256,
                        "An untrained Q can be computed for research but cannot carry a calibrated"
                            + " probability claim",
                        "Train/freeze on time-ordered data and record immutable data/artifact"
                            + " hash."));
        f.add(
                new Finding(
                        "RANK_ARTIFACT",
                        V51ActionRankerArtifact.PROVEN && V51ActionRankerArtifact.RANK_ORDER_PASSED
                                ? Severity.INFO
                                : Severity.WATCH,
                        "rank(a within same target issue)",
                        "PROVEN="
                                + V51ActionRankerArtifact.PROVEN
                                + "|ORDER="
                                + V51ActionRankerArtifact.RANK_ORDER_PASSED,
                        "Ranking must be groupwise across all 1200 actions and stable after freeze",
                        "Verify Rank1/MRR/NDCG/pairwise and champion immutability."));
        f.add(
                new Finding(
                        "SELECTED_HIT_P",
                        V51SelectedCalibrationArtifact.PROVEN ? Severity.INFO : Severity.WATCH,
                        "P(hit | selected, pre-draw features)",
                        "PROVEN="
                                + V51SelectedCalibrationArtifact.PROVEN
                                + "|DATA="
                                + V51SelectedCalibrationArtifact.DATA_SHA256,
                        "Raw rank score or hand-crafted formula is not a calibrated SelectedHitP",
                        "Fit only on pre-draw OOS selected-action ledger; report"
                            + " ECE/Brier/Wilson/subgroups."));
        f.add(
                new Finding(
                        "POLICY_GATE",
                        V51PolicySurfaceArtifact.MODEL_GATE_PASSED ? Severity.INFO : Severity.WATCH,
                        "BET=1 only if frozen model+certificate+policy pass",
                        "MODEL_GATE_PASSED=" + V51PolicySurfaceArtifact.MODEL_GATE_PASSED,
                        "Frequency must emerge from evidence; it cannot be debt-filled",
                        "Keep REAL locked until strict model/certificate chain passes."));

        String brier = getSnapshotCP4(report, store, "strict_oos_brier", "");
        String ece = getSnapshotCP4(report, store, "strict_oos_ece", "");
        String coverage = getSnapshotCP4(report, store, "strict_oos_coverage", "");
        String hit = getSnapshotCP4(report, store, "strict_oos_hit_rate", "");
        if (brier.length() == 0
                && ece.length() == 0
                && coverage.length() == 0
                && hit.length() == 0) {
            f.add(
                    new Finding(
                            "STRICT_OOS_METRICS",
                            Severity.UNAVAILABLE,
                            "Brier=mean((p-y)^2); ECE=sum(w_b*|acc_b-conf_b|)",
                            "NO_FROZEN_OOS_METRICS",
                            "Without frozen OOS metrics the software must not claim 40/50/60%",
                            "Generate immutable time-ordered OOS evidence with sample count and"
                                + " confidence interval."));
        } else {
            f.add(
                    new Finding(
                            "STRICT_OOS_METRICS",
                            Severity.INFO,
                            "Brier/ECE/Coverage/Hit",
                            "Brier="
                                    + brier
                                    + "|ECE="
                                    + ece
                                    + "|Coverage="
                                    + coverage
                                    + "|Hit="
                                    + hit,
                            "Metrics are meaningful only with frozen pre-draw sample identity",
                            "Cross-check data hash, issue range, sample count, lane filter and"
                                + " settlement identity."));
        }

        String sinkhorn = getSnapshotCP4(report, store, "v51_sinkhorn_summary", "");
        f.add(
                new Finding(
                        "SINKHORN_10X10",
                        sinkhorn.length() == 0 ? Severity.UNAVAILABLE : Severity.INFO,
                        "row sums≈1 and column sums≈1",
                        "state=" + (sinkhorn.length() == 0 ? "N/A" : sinkhorn),
                        "Doubly-stochastic normalization must remain finite and reproducible",
                        "Persist max row/column error and fallback reason for each target issue."));

        String leakage = getSnapshotCP4(report, store, "pit_leakage_count", "");
        if (leakage.length() == 0)
            f.add(
                    new Finding(
                            "PIT_LEAKAGE",
                            Severity.UNAVAILABLE,
                            "max(history.issue)<targetIssue",
                            "N/A",
                            "Future leakage invalidates every offline hit-rate claim",
                            "Persist per-target PIT certificate and make any leakage a hard"
                                + " block."));
        else
            f.add(
                    new Finding(
                            "PIT_LEAKAGE",
                            "0".equals(leakage) ? Severity.INFO : Severity.HARD_BLOCK,
                            "max(history.issue)<targetIssue",
                            "leakage_count=" + leakage,
                            "Any positive leakage invalidates affected samples",
                            "Purge affected features/caches, replay and regenerate OOS evidence."));

        return new Report(f.toArray(new Finding[f.size()]));
    }

public static String getSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 s, String k, String d) {
        try {
            return s == null ? d : s.getState(k, d);
        } catch (Throwable t) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",t);}
    }
}
