package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import java.util.List;

/** Freezes the complete prediction-time hypothesis before the target draw exists. */
public final class PreDrawHypothesisLedgerEngine {
    public static final String MODULE_VERSION =
            V49BuildManifest.STRATEGY_VERSION + ":PREDRAW_LEDGER";

    public void persist(
            DbStore store,
            V49FeatureSnapshot f,
            MixtureOfExpertsGateEngine.FusionResult fusion,
            List<Models.ComboCandidate> candidates,
            Models.ComboCandidate selected,
            String primaryCause,
            String secondaryCause,
            String counterEvidence) {
        if (store == null || f == null || fusion == null || candidates == null) return;
        V49Repository.ensure(store);
        SQLiteDatabase db = store.writable();
        db.beginTransaction();
        try {
            ContentValues snap = new ContentValues();
            snap.put("target_issue", f.targetIssue);
            snap.put("base_issue", f.baseIssue);
            snap.put("created_at", System.currentTimeMillis());
            snap.put("feature_checksum", f.checksum);
            snap.put("state_checksum", stateChecksum(f));
            snap.put("raw_probability_blob", matrix(fusion.rawProbability));
            snap.put("calibrated_probability_blob", matrix(fusion.probability));
            snap.put("expert_blob", experts(fusion));
            snap.put("top3_blob", top3(candidates));
            snap.put("opportunity_blob", opportunity(candidates));
            snap.put(
                    "lane", selected == null ? V49LanePolicyEngine.SHADOW : safe(selected.v46Lane));
            snap.put("selected_position", selected == null ? 0 : selected.position);
            snap.put("selected_action_p", selected == null ? 0.0d : selected.finalP);
            snap.put("selected_score", selected == null ? 0.0d : selected.v43StrategySupport);
            snap.put("primary_cause", safe(primaryCause));
            snap.put("secondary_cause", safe(secondaryCause));
            snap.put("counter_evidence", safe(counterEvidence));
            snap.put(
                    "contract_hash", selected == null ? "" : safe(selected.v43EvidenceFingerprint));
            snap.put("settled", 0);
            snap.put("version", MODULE_VERSION);
            db.insertWithOnConflict(
                    V49Repository.TABLE_SNAPSHOT, null, snap, SQLiteDatabase.CONFLICT_REPLACE);

            for (int i = 0; i < candidates.size(); i++) {
                Models.ComboCandidate c = candidates.get(i);
                if (c == null) continue;
                ContentValues a = new ContentValues();
                a.put("target_issue", f.targetIssue);
                a.put("position", c.position);
                a.put("n1", c.n1);
                a.put("n2", c.n2);
                a.put("n3", c.n3);
                a.put("action_p", c.finalP);
                a.put("opportunity", c.v43StrategySupport);
                a.put("agreement", c.coverageQualityScore);
                a.put("uncertainty", c.k3CoverageRisk);
                a.put("family", safe(c.stateKey).split("\\|")[0]);
                a.put("lifecycle", lifecycle(c.stateKey));
                a.put("switch_risk", c.driftRisk);
                a.put("family_strength", c.v43StrategyMaturity);
                a.put("lane", safe(c.v46Lane));
                a.put("contract_hash", safe(c.v43EvidenceFingerprint));
                a.put("settled", 0);
                a.put("hit", 0);
                db.insertWithOnConflict(
                        V49Repository.TABLE_ACTION, null, a, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private static String matrix(double[][] m) {
        StringBuilder b = new StringBuilder();
        for (int p = 0; p < 10; p++) {
            if (p > 0) b.append(';');
            for (int n = 0; n < 10; n++) {
                if (n > 0) b.append(',');
                b.append(Double.toString(m[p][n]));
            }
        }
        return b.toString();
    }

    private static String experts(MixtureOfExpertsGateEngine.FusionResult f) {
        StringBuilder b = new StringBuilder();
        for (int e = 0; e < f.experts.size(); e++) {
            NumberExpert.Result r = f.experts.get(e);
            if (e > 0) b.append('~');
            b.append(r.expertName).append('#');
            b.append(matrix(r.probability)).append('#');
            for (int p = 0; p < 10; p++) {
                if (p > 0) b.append(',');
                b.append(Double.toString(r.trust[p]));
            }
        }
        return b.toString();
    }

    private static String top3(List<Models.ComboCandidate> c) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < c.size(); i++) {
            Models.ComboCandidate x = c.get(i);
            if (x == null) continue;
            if (b.length() > 0) b.append(';');
            b.append(x.position)
                    .append(':')
                    .append(x.n1)
                    .append(',')
                    .append(x.n2)
                    .append(',')
                    .append(x.n3);
        }
        return b.toString();
    }

    private static String opportunity(List<Models.ComboCandidate> c) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < c.size(); i++) {
            Models.ComboCandidate x = c.get(i);
            if (x == null) continue;
            if (b.length() > 0) b.append(';');
            b.append(x.position)
                    .append(':')
                    .append(x.v43StrategySupport)
                    .append(':')
                    .append(x.finalP)
                    .append(':')
                    .append(safe(x.v46Lane));
        }
        return b.toString();
    }

    private static String stateChecksum(V49FeatureSnapshot f) {
        StringBuilder b = new StringBuilder();
        for (int p = 0; p < 10; p++) b.append('|').append(f.positionSummary(p));
        return FeatureContractRegistry.sha256(b.toString());
    }

    private static String lifecycle(String state) {
        if (state == null) return "";
        String[] a = state.split("\\|");
        return a.length > 1 ? a[1] : "";
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
