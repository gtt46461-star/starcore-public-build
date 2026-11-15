package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import java.util.List;

/** Always records one immutable Rank1 diagnostic action. Never creates a Trade. */
public final class DiagnosticObservationEngine {
    public static final String MODULE_VERSION = "V48_DIAGNOSTIC_OBSERVATION_20260721_48072148";

    private DiagnosticObservationEngine() {}

    public static long persist(
            DbStore store, List<Models.ComboCandidate> formal, String identityStatus) {
        if (store == null || formal == null || formal.isEmpty()) return -1L;
        String championStatus =
                store.getState(WalkForwardChampionEngine.STATE_STATUS, "MODEL_NOT_PROVEN");
        Models.ComboCandidate c = first(formal, "CHAMPION".equals(championStatus));
        if (c == null || !c.isValidK3()) return -1L;
        ContentValues v = new ContentValues();
        v.put("target_issue", safe(c.targetIssue));
        v.put("base_issue", safe(c.baseIssue));
        v.put("position", c.position);
        v.put("n1", c.n1);
        v.put("n2", c.n2);
        v.put("n3", c.n3);
        double diagnosticP = "CHAMPION".equals(championStatus) ? c.v46EventProbability : c.modelP;
        v.put("event_p", diagnosticP);
        v.put("action_lcb", Math.max(0.001d, diagnosticP - 0.04d));
        v.put("model_probability", c.modelP);
        v.put("final_rank", Math.max(1, c.v46FinalRank));
        v.put("initial_rank", Math.max(1, c.rank));
        v.put("teacher_rank", Math.max(1, c.rank));
        v.put("dynamic_rank", Math.max(1, c.rank));
        v.put("calibrated_rank", Math.max(1, c.rank));
        v.put("build_id", V48BuildManifest.BUILD_ID);
        v.put("cohort_id", V48BuildManifest.COHORT_ID);
        v.put("schema_version", V48BuildManifest.SCHEMA_VERSION);
        v.put("pipeline_version", V48BuildManifest.STRATEGY_VERSION);
        v.put("model_version", V48BuildManifest.MODEL_VERSION);
        v.put("calibration_version", V48BuildManifest.CALIBRATION_VERSION);
        v.put("ranking_version", V48BuildManifest.RANKING_VERSION);
        v.put("identity_status", safe(identityStatus));
        v.put("champion_status", championStatus);
        v.put("root_code", safe(c.v46RootCauseCode));
        v.put("root_detail", safe(c.v46ProbabilityTrace));
        v.put("created_at", System.currentTimeMillis());
        v.put("settled", 0);
        long id = -1;
        try {
            SQLiteDatabase db = store.writable();
            id =
                    db.insertWithOnConflict(
                            DbHelper.TABLE_V48_DIAGNOSTIC_OBSERVATION,
                            null,
                            v,
                            SQLiteDatabase.CONFLICT_REPLACE);
            store.setState(
                    "v48_diagnostic_observation_last",
                    safe(c.targetIssue)
                            + "|P"
                            + c.position
                            + "|"
                            + c.n1
                            + ','
                            + c.n2
                            + ','
                            + c.n3
                            + "|P="
                            + c.v46EventProbability);
            store.setState("v48_diagnostic_observation_version", MODULE_VERSION);
            V48PipelineLedger.diagnostic(store, id >= 0 ? 1 : 0);
        } catch (Throwable e) {
            try {
                store.setState(
                        "v48_diagnostic_observation_error",
                        e.getClass().getSimpleName() + ":" + safe(e.getMessage()));
            } catch (Throwable ignore) {
            }
        }
        return id;
    }

    public static int settleByDraw(DbStore store, Models.Draw draw) {
        // Diagnostic code has no settlement authority. It can only request the coordinator to
        // reconcile the immutable draw; its own observation rows are read by the R815 sidecar.
        SettlementCoordinatorR815.enqueueDraw(
                store, draw, SettlementCoordinatorR815.Reason.LEGACY_API);
        return 0;
    }

    private static Models.ComboCandidate first(List<Models.ComboCandidate> list, boolean champion) {
        Models.ComboCandidate best = null;
        for (Models.ComboCandidate c : list) {
            if (c == null || !c.isValidK3()) continue;
            if (best == null) best = c;
            else if (champion && c.v46EventProbability > best.v46EventProbability) best = c;
            else if (!champion && c.modelP > best.modelP) best = c;
        }
        return best;
    }

    private static String safe(String v) {
        return v == null ? "" : v;
    }
}
