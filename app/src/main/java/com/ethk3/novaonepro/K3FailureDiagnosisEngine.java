package com.ethk3.novaonepro;

import android.database.Cursor;
import java.util.ArrayList;

/**
 * Fixed single-position K3 failure decomposition. It diagnoses settled evidence; it does not
 * predict or authorize BET.
 */
public final class K3FailureDiagnosisEngine {
    public static final String OWNER = "B14_K3_FAILURE_DECOMPOSITION";
    public static final String VERSION = "V57_K3_FAILURE_DIAGNOSIS_FULL_20260808";

    public enum Layer {
        NUMBER,
        RANK,
        THIRD,
        WINDOW,
        REGIME,
        MOTIF,
        RELATION,
        CONSENSUS,
        CALIBRATION
    }

    public enum State {
        HEALTHY,
        WATCH,
        SUSPECT,
        BLOCKED,
        UNAVAILABLE
    }

    public static final class LayerFinding {
        public final Layer layer;
        public final State state;
        public final String positive, negative, reason, nextTest;
        public final double confidence;
        public final String checksum;

        LayerFinding(
                Layer layer,
                State state,
                String positive,
                String negative,
                String reason,
                String nextTest,
                double confidence) {
            this.layer = layer;
            this.state = state;
            this.positive = s(positive);
            this.negative = s(negative);
            this.reason = s(reason);
            this.nextTest = s(nextTest);
            this.confidence = clamp(confidence);
            checksum =
                    V51ChecksumEngine.sha256(
                            layer.name()
                                    + '|'
                                    + state.name()
                                    + '|'
                                    + this.positive
                                    + '|'
                                    + this.negative
                                    + '|'
                                    + this.reason
                                    + '|'
                                    + this.nextTest
                                    + '|'
                                    + this.confidence);
        }
    }

    public static final class Report {
        public final LayerFinding[] layers;
        public final String primaryLayer, verdict, checksum;

        Report(LayerFinding[] x) {
            this(x, "", "");
        }

        Report(LayerFinding[] x, String primaryOverride, String verdictOverride) {
            layers = x;
            LayerFinding best = null;
            StringBuilder h = new StringBuilder();
            for (int i = 0; i < x.length; i++) {
                h.append('|').append(x[i].checksum);
                if (best == null
                        || risk(x[i].state) > risk(best.state)
                        || (risk(x[i].state) == risk(best.state)
                                && x[i].confidence > best.confidence)) best = x[i];
            }
            primaryLayer =
                    s(primaryOverride).length() > 0
                            ? s(primaryOverride)
                            : (best == null ? "NONE" : best.layer.name());
            verdict =
                    s(verdictOverride).length() > 0
                            ? s(verdictOverride)
                            : (best == null ? "NO_DATA" : best.state.name());
            checksum = V51ChecksumEngine.sha256(primaryLayer + '|' + verdict + h.toString());
        }
    }

    public Report diagnose(DbStore store) {
        ArrayList<LayerFinding> a = new ArrayList<LayerFinding>();
        a.add(
                frozenArtifactR342(null,store==null?null:store.readable(),Layer.NUMBER,
                        "NumberQ artifact",
                        "Untrained NumberQ cannot establish top-number quality",
                        "Train/freeze NumberQ; compare top-k recall and Sinkhorn parity."));
        a.add(
                frozenArtifactR342(null,store==null?null:store.readable(),Layer.RANK,
                        "1200-action rank artifact",
                        "Untrained ranker cannot establish Champion ordering",
                        "Run same-issue groupwise OOS Rank1/MRR/NDCG/pairwise stability."));
        a.add(
                stateLayer(
                        store,
                        Layer.THIRD,
                        "v51_third_boundary_summary",
                        "Third boundary decides whether final K3 loses its marginal candidate",
                        "Audit rank3 vs rank4 margin, miss migration and after-draw"
                            + " counterfactual."));
        a.add(
                stateLayer(
                        store,
                        Layer.WINDOW,
                        "b05_window_summary",
                        "Window choice can overfit local repetition or react too slowly",
                        "Compare 12/24/48/96/360 survival and PIT-stable window selection."));
        a.add(
                stateLayer(
                        store,
                        Layer.REGIME,
                        "b05_state_summary",
                        "Regime mismatch can invalidate an otherwise strong motif/expert",
                        "Blockwise regime transition replay; inspect CUSUM/BOCPD/drift flags."));
        a.add(
                stateLayer(
                        store,
                        Layer.MOTIF,
                        "b06_motif_summary",
                        "Motif may be sparse, stale or duplicated across templates",
                        "Require support, recent validity and negative evidence before"
                            + " promotion."));
        a.add(
                stateLayer(
                        store,
                        Layer.RELATION,
                        "b06_relation_summary",
                        "Cross-position relation can be spurious or leak contemporaneous"
                            + " information",
                        "Verify relation uses only pre-draw history and survives block ablation."));
        a.add(
                stateLayer(
                        store,
                        Layer.CONSENSUS,
                        "v51_consensus_summary",
                        "Consensus can become correlated-vote double counting",
                        "Measure expert diversity, regret and leave-one-expert-out delta."));
        a.add(
                frozenArtifactR342(null,store==null?null:store.readable(),Layer.CALIBRATION,
                        "SelectedHitP calibration artifact",
                        "Untrained calibration makes threshold/coverage decisions unreliable",
                        "Use selection-conditioned pre-draw OOS ECE/Brier/Wilson/subgroup"
                            + " certificates."));
        LayerFinding[] layers = a.toArray(new LayerFinding[a.size()]);
        RuntimeHealthLedgerR36.BusinessTruth truth =
                RuntimeHealthLedgerR36.businessTruth(store);
        if ("READ_ERROR".equals(truth.rootCause)
                || "RUNTIME_CHAIN".equals(truth.rootCause)
                || "SETTLEMENT_OOS".equals(truth.rootCause)) {
            return new Report(layers, truth.rootCause, "BLOCKED_UPSTREAM");
        }
        return new Report(layers);
    }

    private LayerFinding frozenArtifactR342(ReportReadContextCP4 report, android.database.sqlite.SQLiteDatabase database,
            Layer layer, String label, String negative, String next) {
        if (database==null) return new LayerFinding(layer,State.UNAVAILABLE,label,negative,"INPUT_DATABASE_UNAVAILABLE",next,0d);
        String sourceSql="SELECT target_issue,bundle_hash,contract_hash FROM "+V51Repository.ISSUE
                +" ORDER BY target_issue DESC LIMIT 1";
        try (Cursor current=layerQueryR342(report,database,sourceSql,null)) {
            if(!current.moveToFirst())return new LayerFinding(layer,State.UNAVAILABLE,label,negative,
                    "NO_COMMITTED_DYNAMIC_ARTIFACT",next,0d);
            String issue=current.getString(0),bundle=current.getString(1),contract=current.getString(2);
            String evidence=label+"|target="+issue+"|bundle="+bundle+"|contract="+contract;
            if(layer==Layer.CALIBRATION){
                try(Cursor row=layerQueryR342(report,database,"SELECT status,reason,calibration_hash,proof_json,proof_hash FROM "
                        +ChampionProbabilityEvidenceR82.TABLE+" WHERE target_issue=? AND bundle_hash=?",new String[]{issue,bundle})){
                    if(row.moveToFirst()&&"PROVEN".equals(row.getString(0))&&row.getString(2).matches("[a-f0-9]{64}")
                            &&V51ChecksumEngine.sha256(row.getString(3)).equals(row.getString(4))){
                        return new LayerFinding(layer,State.WATCH,evidence+"|calibration="+row.getString(2),negative,
                                "EXACT_FROZEN_CALIBRATION_ORIGINAL_REOPENED;INDEPENDENT_QUALITY_PENDING",next,.5d);
                    }
                    return new LayerFinding(layer,State.BLOCKED,evidence,negative,
                            row.isAfterLast()?"FROZEN_CALIBRATION_ORIGINAL_MISSING":"FROZEN_CALIBRATION_NOT_QUALIFIED",next,.8d);
                }
            }
            String table=layer==Layer.NUMBER?V51Repository.NUMBER:V51Repository.ACTION_SPACE;
            int expected=layer==Layer.NUMBER?100:K3ActionSpaceEngine.TOTAL_ACTIONS;
            try(Cursor rows=layerQueryR342(report,database,"SELECT count(*) FROM (SELECT 1 FROM "+table
                    +" WHERE target_issue=? AND bundle_hash=? AND contract_hash=? LIMIT "+(expected+1)+')',
                    new String[]{issue,bundle,contract})){
                int count=rows.moveToFirst()?rows.getInt(0):0;
                return new LayerFinding(layer,count==expected?State.WATCH:State.BLOCKED,
                        evidence+"|sourceRows="+count+'/'+expected,negative,
                        count==expected?"EXACT_DYNAMIC_SURFACE_PRESENT;INDEPENDENT_QUALITY_PENDING"
                                :"EXACT_DYNAMIC_SURFACE_INCOMPLETE",next,count==expected?.5d:.85d);
            }
        }catch(android.os.OperationCanceledException cancelled){
            throw cancelled;
        }catch(android.database.sqlite.SQLiteException unavailable){
            return new LayerFinding(layer,State.UNAVAILABLE,label,negative,
                    "ARTIFACT_SOURCE_QUERY_"+unavailable.getClass().getSimpleName(),next,0d);
        }
    }

    private static Cursor layerQueryR342(ReportReadContextCP4 report,android.database.sqlite.SQLiteDatabase database,
            String sql,String[] parameters){
        return report==null?database.rawQuery(sql,parameters):report.select(sql,parameters);
    }

    private LayerFinding stateLayer(
            DbStore store, Layer layer, String key, String why, String test) {
        String v = get(store, key, "");
        if (v.length() == 0)
            return new LayerFinding(
                    layer,
                    State.UNAVAILABLE,
                    "N/A",
                    "No persisted runtime evidence",
                    why,
                    test,
                    0.0d);
        String u = v.toUpperCase(java.util.Locale.ROOT);
        State st =
                (u.indexOf("FAIL") >= 0 || u.indexOf("FREEZE") >= 0 || u.indexOf("CHAOS") >= 0)
                        ? State.SUSPECT
                        : (u.indexOf("RISK") >= 0 || u.indexOf("DRIFT") >= 0
                                ? State.WATCH
                                : State.HEALTHY);
        return new LayerFinding(
                layer,
                st,
                v,
                "Runtime summary is observational, not causal proof",
                why,
                test,
                st == State.HEALTHY ? 0.6d : 0.75d);
    }

    public String render(DbStore store) {
        Report r = diagnose(store);
        StringBuilder b = new StringBuilder(8192);
        b.append("【K3失败九类拆解】\nVersion=")
                .append(VERSION)
                .append("\nPrimaryLayer=")
                .append(r.primaryLayer)
                .append("|Verdict=")
                .append(r.verdict)
                .append('\n');
        for (int i = 0; i < r.layers.length; i++) {
            LayerFinding x = r.layers[i];
            b.append(i + 1)
                    .append('.')
                    .append(x.layer.name())
                    .append('|')
                    .append(x.state.name())
                    .append("|confidence=")
                    .append(x.confidence)
                    .append("\nPositive=")
                    .append(x.positive)
                    .append("\nNegative=")
                    .append(x.negative)
                    .append("\nReason=")
                    .append(x.reason)
                    .append("\nNextTest=")
                    .append(x.nextTest)
                    .append("\nHash=")
                    .append(x.checksum)
                    .append("\n\n");
        }
        b.append("ReportHash=").append(r.checksum).append('\n');
        return b.toString();
    }

    private static int risk(State s) {
        if (s == State.BLOCKED) return 5;
        if (s == State.SUSPECT) return 4;
        if (s == State.WATCH) return 3;
        if (s == State.UNAVAILABLE) return 2;
        return 1;
    }

    private static double clamp(double x) {
        if (Double.isNaN(x) || Double.isInfinite(x)) return 0;
        return Math.max(0, Math.min(1, x));
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
        Report r = diagnoseSnapshotCP4(report, store);
        com.ethk3.novaonepro.ReportTextCP12 b = new com.ethk3.novaonepro.ReportTextCP12(report, 8192);
        b.append("【K3失败九类拆解】\nVersion=")
                .append(VERSION)
                .append("\nPrimaryLayer=")
                .append(r.primaryLayer)
                .append("|Verdict=")
                .append(r.verdict)
                .append('\n');
        for (int i = 0; i < r.layers.length; i++) {
            LayerFinding x = r.layers[i];
            b.append(i + 1)
                    .append('.')
                    .append(x.layer.name())
                    .append('|')
                    .append(x.state.name())
                    .append("|confidence=")
                    .append(x.confidence)
                    .append("\nPositive=")
                    .append(x.positive)
                    .append("\nNegative=")
                    .append(x.negative)
                    .append("\nReason=")
                    .append(x.reason)
                    .append("\nNextTest=")
                    .append(x.nextTest)
                    .append("\nHash=")
                    .append(x.checksum)
                    .append("\n\n");
        }
        b.append("ReportHash=").append(r.checksum).append('\n');
        return b.toString();
    }

public Report diagnoseSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        ArrayList<LayerFinding> a = new ArrayList<LayerFinding>();
        a.add(
                frozenArtifactR342(report,store==null?null:store.readable(),Layer.NUMBER,
                        "NumberQ artifact",
                        "Untrained NumberQ cannot establish top-number quality",
                        "Train/freeze NumberQ; compare top-k recall and Sinkhorn parity."));
        a.add(
                frozenArtifactR342(report,store==null?null:store.readable(),Layer.RANK,
                        "1200-action rank artifact",
                        "Untrained ranker cannot establish Champion ordering",
                        "Run same-issue groupwise OOS Rank1/MRR/NDCG/pairwise stability."));
        a.add(
                stateLayerSnapshotCP4(report, 
                        store,
                        Layer.THIRD,
                        "v51_third_boundary_summary",
                        "Third boundary decides whether final K3 loses its marginal candidate",
                        "Audit rank3 vs rank4 margin, miss migration and after-draw"
                            + " counterfactual."));
        a.add(
                stateLayerSnapshotCP4(report, 
                        store,
                        Layer.WINDOW,
                        "b05_window_summary",
                        "Window choice can overfit local repetition or react too slowly",
                        "Compare 12/24/48/96/360 survival and PIT-stable window selection."));
        a.add(
                stateLayerSnapshotCP4(report, 
                        store,
                        Layer.REGIME,
                        "b05_state_summary",
                        "Regime mismatch can invalidate an otherwise strong motif/expert",
                        "Blockwise regime transition replay; inspect CUSUM/BOCPD/drift flags."));
        a.add(
                stateLayerSnapshotCP4(report, 
                        store,
                        Layer.MOTIF,
                        "b06_motif_summary",
                        "Motif may be sparse, stale or duplicated across templates",
                        "Require support, recent validity and negative evidence before"
                            + " promotion."));
        a.add(
                stateLayerSnapshotCP4(report, 
                        store,
                        Layer.RELATION,
                        "b06_relation_summary",
                        "Cross-position relation can be spurious or leak contemporaneous"
                            + " information",
                        "Verify relation uses only pre-draw history and survives block ablation."));
        a.add(
                stateLayerSnapshotCP4(report, 
                        store,
                        Layer.CONSENSUS,
                        "v51_consensus_summary",
                        "Consensus can become correlated-vote double counting",
                        "Measure expert diversity, regret and leave-one-expert-out delta."));
        a.add(
                frozenArtifactR342(report,store==null?null:store.readable(),Layer.CALIBRATION,
                        "SelectedHitP calibration artifact",
                        "Untrained calibration makes threshold/coverage decisions unreliable",
                        "Use selection-conditioned pre-draw OOS ECE/Brier/Wilson/subgroup"
                            + " certificates."));
        LayerFinding[] layers = a.toArray(new LayerFinding[a.size()]);
        RuntimeHealthLedgerR36.BusinessTruth truth =
                RuntimeHealthLedgerR36.businessTruthSnapshotCP4(report, store);
        if ("READ_ERROR".equals(truth.rootCause)
                || "RUNTIME_CHAIN".equals(truth.rootCause)
                || "SETTLEMENT_OOS".equals(truth.rootCause)) {
            return new Report(layers, truth.rootCause, "BLOCKED_UPSTREAM");
        }
        return new Report(layers);
    }

public LayerFinding artifactLayerSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, 
            Layer layer, boolean proven, String positive, String negative, String test) {
        return new LayerFinding(
                layer,
                proven ? State.HEALTHY : State.BLOCKED,
                positive + "|PROVEN=" + proven,
                negative,
                proven ? "Frozen artifact evidence exists" : "Strict OOS artifact proof is missing",
                test,
                proven ? 0.9d : 1.0d);
    }

public LayerFinding stateLayerSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, 
            com.ethk3.novaonepro.ReportReadContextCP4 store, Layer layer, String key, String why, String test) {
        String v = getSnapshotCP4(report, store, key, "");
        if (v.length() == 0)
            return new LayerFinding(
                    layer,
                    State.UNAVAILABLE,
                    "N/A",
                    "No persisted runtime evidence",
                    why,
                    test,
                    0.0d);
        String u = v.toUpperCase(java.util.Locale.ROOT);
        State st =
                (u.indexOf("FAIL") >= 0 || u.indexOf("FREEZE") >= 0 || u.indexOf("CHAOS") >= 0)
                        ? State.SUSPECT
                        : (u.indexOf("RISK") >= 0 || u.indexOf("DRIFT") >= 0
                                ? State.WATCH
                                : State.HEALTHY);
        return new LayerFinding(
                layer,
                st,
                v,
                "Runtime summary is observational, not causal proof",
                why,
                test,
                st == State.HEALTHY ? 0.6d : 0.75d);
    }

public static String getSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 s, String k, String d) {
        try {
            return s == null ? d : s.getState(k, d);
        } catch (Throwable t) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",t);}
    }
}
