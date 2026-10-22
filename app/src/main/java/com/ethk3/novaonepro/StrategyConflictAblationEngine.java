package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Read-only conflict/ablation evidence interpreter. Missing measurements stay UNAVAILABLE; no
 * effect is fabricated.
 */
public final class StrategyConflictAblationEngine {
    public static final String OWNER = "B14_STRATEGY_CONFLICT_ABLATION";
    public static final String VERSION = "V57_STRATEGY_CONFLICT_ABLATION_FULL_20260808";

    public enum Evidence {
        MEASURED_OOS,
        MEASURED_SHADOW,
        RUNTIME_SIGNAL,
        UNAVAILABLE
    }

    public static final class ModuleEffect {
        public final String module,
                evidence,
                deltaHit,
                deltaBrier,
                deltaCoverage,
                deltaEce,
                verdict,
                reason;
        public final double confidence;
        public final String checksum;

        ModuleEffect(
                String module,
                String evidence,
                String dh,
                String db,
                String dc,
                String de,
                String verdict,
                String reason,
                double confidence) {
            this.module = s(module);
            this.evidence = s(evidence);
            deltaHit = s(dh);
            deltaBrier = s(db);
            deltaCoverage = s(dc);
            deltaEce = s(de);
            this.verdict = s(verdict);
            this.reason = s(reason);
            this.confidence = clamp(confidence);
            checksum =
                    V51ChecksumEngine.sha256(
                            this.module
                                    + '|'
                                    + this.evidence
                                    + '|'
                                    + deltaHit
                                    + '|'
                                    + deltaBrier
                                    + '|'
                                    + deltaCoverage
                                    + '|'
                                    + deltaEce
                                    + '|'
                                    + this.verdict
                                    + '|'
                                    + this.reason
                                    + '|'
                                    + this.confidence);
        }
    }

    public static final class Conflict {
        public final String left, right, type, evidence, action;
        public final double score;
        public final String checksum;

        Conflict(
                String left,
                String right,
                String type,
                String evidence,
                String action,
                double score) {
            this.left = s(left);
            this.right = s(right);
            this.type = s(type);
            this.evidence = s(evidence);
            this.action = s(action);
            this.score = clamp(score);
            checksum =
                    V51ChecksumEngine.sha256(
                            this.left
                                    + '|'
                                    + this.right
                                    + '|'
                                    + this.type
                                    + '|'
                                    + this.evidence
                                    + '|'
                                    + this.action
                                    + '|'
                                    + this.score);
        }
    }

    public static final class Report {
        public final ModuleEffect[] productionEffects;
        public final ModuleEffect[] researchEffects;
        /** Backward-compatible alias: Production evidence only. */
        public final ModuleEffect[] effects;
        public final Conflict[] conflicts;
        public final String verdict, checksum;

        Report(ModuleEffect[] production, ModuleEffect[] research, Conflict[] c) {
            productionEffects = production;
            researchEffects = research;
            effects = production;
            conflicts = c;
            int measured = 0;
            double max = 0;
            StringBuilder h = new StringBuilder();
            for (int i = 0; i < production.length; i++) {
                if (!"UNAVAILABLE".equals(production[i].evidence)) measured++;
                h.append("|P:").append(production[i].checksum);
            }
            for (int i = 0; i < research.length; i++) {
                h.append("|R:").append(research[i].checksum);
            }
            for (int i = 0; i < c.length; i++) {
                max = Math.max(max, c[i].score);
                h.append('|').append(c[i].checksum);
            }
            verdict =
                    max >= 0.8
                            ? "HIGH_CONFLICT"
                            : (measured == 0 ? "NO_MEASURED_ABLATION" : "MEASURED_REVIEW");
            checksum = V51ChecksumEngine.sha256(verdict + h.toString());
        }
    }

    private static final String[] MODULES = {
        "PIT_FEATURE_GRAPH",
        "STATE_REGIME",
        "MOTIF_RELATION",
        "TEACHER_CAUSAL",
        "EXPERT_GATE",
        "SINKHORN_NUMBER_Q",
        "K3_FUNNEL",
        "ACTION_RANKER",
        "SELECTED_CALIBRATION",
        "DOMAIN_CERTIFICATE",
        "CONFORMAL_RISK",
        "POLICY_COMPILER"
    };

    public Report audit(DbStore store) {
        ArrayList<ModuleEffect> production = new ArrayList<ModuleEffect>();
        ArrayList<ModuleEffect> research = new ArrayList<ModuleEffect>();
        for (int i = 0; i < MODULES.length; i++) {
            production.add(readEffect(store, MODULES[i], "b14_ablation_", true));
            research.add(readEffect(store, MODULES[i], "r811_research_ablation_", false));
        }
        ArrayList<Conflict> conflicts = new ArrayList<Conflict>();
        addRuntimeConflict(
                store, conflicts, "STATE_REGIME", "MOTIF_RELATION", "b14_conflict_state_motif");
        addRuntimeConflict(
                store, conflicts, "TEACHER_CAUSAL", "EXPERT_GATE", "b14_conflict_teacher_gate");
        addRuntimeConflict(
                store, conflicts, "SINKHORN_NUMBER_Q", "ACTION_RANKER", "b14_conflict_number_rank");
        addRuntimeConflict(
                store,
                conflicts,
                "SELECTED_CALIBRATION",
                "POLICY_COMPILER",
                "b14_conflict_calibration_policy");
        addRuntimeConflict(
                store,
                conflicts,
                "DOMAIN_CERTIFICATE",
                "CONFORMAL_RISK",
                "b14_conflict_domain_conformal");
        Collections.sort(
                conflicts,
                new Comparator<Conflict>() {
                    public int compare(Conflict a, Conflict b) {
                        return a.score == b.score ? 0 : (a.score > b.score ? -1 : 1);
                    }
                });
        return new Report(
                production.toArray(new ModuleEffect[production.size()]),
                research.toArray(new ModuleEffect[research.size()]),
                conflicts.toArray(new Conflict[conflicts.size()]));
    }

    private ModuleEffect readEffect(
            DbStore store, String module, String prefixRoot, boolean production) {
        String prefix = prefixRoot + module.toLowerCase(java.util.Locale.ROOT);
        String dh = get(store, prefix + "_delta_hit", "");
        String db = get(store, prefix + "_delta_brier", "");
        String dc = get(store, prefix + "_delta_coverage", "");
        String de = get(store, prefix + "_delta_ece", "");
        String scope = get(store, prefix + "_scope", "");
        if (dh.length() == 0 && db.length() == 0 && dc.length() == 0 && de.length() == 0)
            return new ModuleEffect(
                    module,
                    "UNAVAILABLE",
                    "N/A",
                    "N/A",
                    "N/A",
                    "N/A",
                    "NO_CLAIM",
                    "No frozen OOS/shadow ablation measurement is persisted; do not invent"
                        + " contribution.",
                    0.0d);
        if (production && (!"OOS".equalsIgnoreCase(scope) || !hasStrictOos(store))) {
            return new ModuleEffect(
                    module,
                    "UNAVAILABLE",
                    "N/A",
                    "N/A",
                    "N/A",
                    "N/A",
                    "NO_CLAIM",
                    "Production contribution requires an existing strict OOS label and OOS scope.",
                    0.0d);
        }
        String ev =
                production
                        ? "MEASURED_OOS"
                        : ("OOS".equalsIgnoreCase(scope)
                                ? "RESEARCH_MEASURED_OOS"
                                : "RESEARCH_MEASURED_SHADOW");
        return new ModuleEffect(
                module,
                ev,
                val(dh),
                val(db),
                val(dc),
                val(de),
                "REVIEW_MEASURED_DELTA",
                "Interpret hit together with Brier/ECE/coverage; one metric alone cannot prove"
                    + " usefulness.",
                "MEASURED_OOS".equals(ev) || "RESEARCH_MEASURED_OOS".equals(ev)
                        ? 0.9d
                        : 0.65d);
    }

    private boolean hasStrictOos(DbStore store) {
        android.database.Cursor cursor = null;
        try {
            if (store == null) return false;
            cursor =
                    store.readable()
                            .rawQuery(
                                    "SELECT 1 FROM "
                                            + DbHelper.TABLE_R35_OOS_LABEL
                                            + " WHERE strict_oos=1 AND settled_at>0 LIMIT 1",
                                    null);
            return cursor.moveToFirst();
        } catch (Throwable error) {
            return false;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private void addRuntimeConflict(
            DbStore store, ArrayList<Conflict> out, String left, String right, String key) {
        String raw = get(store, key, "");
        if (raw.length() == 0) {
            out.add(
                    new Conflict(
                            left,
                            right,
                            "UNAVAILABLE",
                            "No persisted pairwise conflict measurement",
                            "Run time-ordered ablation/correlation/replay before changing weights.",
                            0.0d));
            return;
        }
        double score = parse01(raw);
        String type =
                score >= 0.8
                        ? "STRONG_CONFLICT"
                        : (score >= 0.5 ? "WATCH_CONFLICT" : "LOW_CONFLICT");
        out.add(
                new Conflict(
                        left,
                        right,
                        type,
                        key + '=' + raw,
                        score >= 0.8
                                ? "Freeze automatic fusion for this pair and run isolated replay."
                                : "Retain but monitor blockwise stability.",
                        score));
    }

    public String render(DbStore store) {
        Report r = audit(store);
        StringBuilder b = new StringBuilder(12288);
        b.append("【策略贡献/冲突/冗余/消融】\nVersion=")
                .append(VERSION)
                .append("\nVerdict=")
                .append(r.verdict)
                .append('\n');
        b.append("\n-- Production模块消融（strict OOS only） --\n");
        appendEffects(b, r.productionEffects);
        b.append("\n-- Research模块消融（不并入Production） --\n");
        appendEffects(b, r.researchEffects);
        b.append("\n-- Pairwise冲突 --\n");
        for (int i = 0; i < r.conflicts.length; i++) {
            Conflict c = r.conflicts[i];
            b.append(c.left)
                    .append(" <-> ")
                    .append(c.right)
                    .append('|')
                    .append(c.type)
                    .append("|score=")
                    .append(c.score)
                    .append("\nEvidence=")
                    .append(c.evidence)
                    .append("\nAction=")
                    .append(c.action)
                    .append("\nHash=")
                    .append(c.checksum)
                    .append('\n');
        }
        b.append("ReportHash=").append(r.checksum).append('\n');
        return b.toString();
    }

    private void appendEffects(StringBuilder b, ModuleEffect[] effects) {
        for (int i = 0; i < effects.length; i++) {
            ModuleEffect e = effects[i];
            b.append(e.module)
                    .append('|')
                    .append(e.evidence)
                    .append("|dHit=")
                    .append(e.deltaHit)
                    .append("|dBrier=")
                    .append(e.deltaBrier)
                    .append("|dCoverage=")
                    .append(e.deltaCoverage)
                    .append("|dECE=")
                    .append(e.deltaEce)
                    .append("|Verdict=")
                    .append(e.verdict)
                    .append("|Confidence=")
                    .append(e.confidence)
                    .append("\nReason=")
                    .append(e.reason)
                    .append("\nHash=")
                    .append(e.checksum)
                    .append('\n');
        }
    }

    private static String val(String x) {
        return x == null || x.length() == 0 ? "N/A" : x;
    }

    private static double parse01(String x) {
        try {
            return clamp(Double.parseDouble(x));
        } catch (Throwable t) {
            return 0.0d;
        }
    }

    private static double clamp(double x) {
        if (Double.isNaN(x) || Double.isInfinite(x)) return 0.0d;
        return Math.max(0.0d, Math.min(1.0d, x));
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
        com.ethk3.novaonepro.ReportTextCP12 b = new com.ethk3.novaonepro.ReportTextCP12(report, 12288);
        b.append("【策略贡献/冲突/冗余/消融】\nVersion=")
                .append(VERSION)
                .append("\nVerdict=")
                .append(r.verdict)
                .append('\n');
        b.append("\n-- Production模块消融（strict OOS only） --\n");
        appendEffectsSnapshotCP4(report, b, r.productionEffects);
        b.append("\n-- Research模块消融（不并入Production） --\n");
        appendEffectsSnapshotCP4(report, b, r.researchEffects);
        b.append("\n-- Pairwise冲突 --\n");
        for (int i = 0; i < r.conflicts.length; i++) {
            Conflict c = r.conflicts[i];
            b.append(c.left)
                    .append(" <-> ")
                    .append(c.right)
                    .append('|')
                    .append(c.type)
                    .append("|score=")
                    .append(c.score)
                    .append("\nEvidence=")
                    .append(c.evidence)
                    .append("\nAction=")
                    .append(c.action)
                    .append("\nHash=")
                    .append(c.checksum)
                    .append('\n');
        }
        b.append("ReportHash=").append(r.checksum).append('\n');
        return b.toString();
    }

public void appendEffectsSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportTextCP12 b, ModuleEffect[] effects) {
        for (int i = 0; i < effects.length; i++) {
            ModuleEffect e = effects[i];
            b.append(e.module)
                    .append('|')
                    .append(e.evidence)
                    .append("|dHit=")
                    .append(e.deltaHit)
                    .append("|dBrier=")
                    .append(e.deltaBrier)
                    .append("|dCoverage=")
                    .append(e.deltaCoverage)
                    .append("|dECE=")
                    .append(e.deltaEce)
                    .append("|Verdict=")
                    .append(e.verdict)
                    .append("|Confidence=")
                    .append(e.confidence)
                    .append("\nReason=")
                    .append(e.reason)
                    .append("\nHash=")
                    .append(e.checksum)
                    .append('\n');
        }
    }

public Report auditSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        ArrayList<ModuleEffect> production = new ArrayList<ModuleEffect>();
        ArrayList<ModuleEffect> research = new ArrayList<ModuleEffect>();
        for (int i = 0; i < MODULES.length; i++) {
            production.add(readEffectSnapshotCP4(report, store, MODULES[i], "b14_ablation_", true));
            research.add(readEffectSnapshotCP4(report, store, MODULES[i], "r811_research_ablation_", false));
        }
        ArrayList<Conflict> conflicts = new ArrayList<Conflict>();
        addRuntimeConflictSnapshotCP4(report, 
                store, conflicts, "STATE_REGIME", "MOTIF_RELATION", "b14_conflict_state_motif");
        addRuntimeConflictSnapshotCP4(report, 
                store, conflicts, "TEACHER_CAUSAL", "EXPERT_GATE", "b14_conflict_teacher_gate");
        addRuntimeConflictSnapshotCP4(report, 
                store, conflicts, "SINKHORN_NUMBER_Q", "ACTION_RANKER", "b14_conflict_number_rank");
        addRuntimeConflictSnapshotCP4(report, 
                store,
                conflicts,
                "SELECTED_CALIBRATION",
                "POLICY_COMPILER",
                "b14_conflict_calibration_policy");
        addRuntimeConflictSnapshotCP4(report, 
                store,
                conflicts,
                "DOMAIN_CERTIFICATE",
                "CONFORMAL_RISK",
                "b14_conflict_domain_conformal");
        Collections.sort(
                conflicts,
                new Comparator<Conflict>() {
                    public int compare(Conflict a, Conflict b) {
                        return a.score == b.score ? 0 : (a.score > b.score ? -1 : 1);
                    }
                });
        return new Report(
                production.toArray(new ModuleEffect[production.size()]),
                research.toArray(new ModuleEffect[research.size()]),
                conflicts.toArray(new Conflict[conflicts.size()]));
    }

public void addRuntimeConflictSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, 
            com.ethk3.novaonepro.ReportReadContextCP4 store, ArrayList<Conflict> out, String left, String right, String key) {
        String raw = getSnapshotCP4(report, store, key, "");
        if (raw.length() == 0) {
            out.add(
                    new Conflict(
                            left,
                            right,
                            "UNAVAILABLE",
                            "No persisted pairwise conflict measurement",
                            "Run time-ordered ablation/correlation/replay before changing weights.",
                            0.0d));
            return;
        }
        double score = parse01SnapshotCP4(report, raw);
        String type =
                score >= 0.8
                        ? "STRONG_CONFLICT"
                        : (score >= 0.5 ? "WATCH_CONFLICT" : "LOW_CONFLICT");
        out.add(
                new Conflict(
                        left,
                        right,
                        type,
                        key + '=' + raw,
                        score >= 0.8
                                ? "Freeze automatic fusion for this pair and run isolated replay."
                                : "Retain but monitor blockwise stability.",
                        score));
    }

public static double parse01SnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, String x) {
        try {
            return clampSnapshotCP4(report, Double.parseDouble(x));
        } catch (Throwable t) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",t);}
    }

public static double clampSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, double x) {
        if (Double.isNaN(x) || Double.isInfinite(x)) return 0.0d;
        return Math.max(0.0d, Math.min(1.0d, x));
    }

public static String getSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 s, String k, String d) {
        try {
            return s == null ? d : s.getState(k, d);
        } catch (Throwable t) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",t);}
    }

public ModuleEffect readEffectSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, 
            com.ethk3.novaonepro.ReportReadContextCP4 store, String module, String prefixRoot, boolean production) {
        String prefix = prefixRoot + module.toLowerCase(java.util.Locale.ROOT);
        String dh = getSnapshotCP4(report, store, prefix + "_delta_hit", "");
        String db = getSnapshotCP4(report, store, prefix + "_delta_brier", "");
        String dc = getSnapshotCP4(report, store, prefix + "_delta_coverage", "");
        String de = getSnapshotCP4(report, store, prefix + "_delta_ece", "");
        String scope = getSnapshotCP4(report, store, prefix + "_scope", "");
        if (dh.length() == 0 && db.length() == 0 && dc.length() == 0 && de.length() == 0)
            return new ModuleEffect(
                    module,
                    "UNAVAILABLE",
                    "N/A",
                    "N/A",
                    "N/A",
                    "N/A",
                    "NO_CLAIM",
                    "No frozen OOS/shadow ablation measurement is persisted; do not invent"
                        + " contribution.",
                    0.0d);
        if (production && (!"OOS".equalsIgnoreCase(scope) || !hasStrictOosSnapshotCP4(report, store))) {
            return new ModuleEffect(
                    module,
                    "UNAVAILABLE",
                    "N/A",
                    "N/A",
                    "N/A",
                    "N/A",
                    "NO_CLAIM",
                    "Production contribution requires an existing strict OOS label and OOS scope.",
                    0.0d);
        }
        String ev =
                production
                        ? "MEASURED_OOS"
                        : ("OOS".equalsIgnoreCase(scope)
                                ? "RESEARCH_MEASURED_OOS"
                                : "RESEARCH_MEASURED_SHADOW");
        return new ModuleEffect(
                module,
                ev,
                valSnapshotCP4(report, dh),
                valSnapshotCP4(report, db),
                valSnapshotCP4(report, dc),
                valSnapshotCP4(report, de),
                "REVIEW_MEASURED_DELTA",
                "Interpret hit together with Brier/ECE/coverage; one metric alone cannot prove"
                    + " usefulness.",
                "MEASURED_OOS".equals(ev) || "RESEARCH_MEASURED_OOS".equals(ev)
                        ? 0.9d
                        : 0.65d);
    }

public static String valSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, String x) {
        return x == null || x.length() == 0 ? "N/A" : x;
    }

public boolean hasStrictOosSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        android.database.Cursor cursor = null;
        try {
            if (store == null) return false;
            cursor =
                    report.select(
                                    "SELECT 1 FROM "
                                            + DbHelper.TABLE_R35_OOS_LABEL
                                            + " WHERE strict_oos=1 AND settled_at>0 LIMIT 1",
                                    null);
            return cursor.moveToFirst();
        } catch (Throwable error) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",error);} finally {
            if (cursor != null) cursor.close();
        }
    }
}
