package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * V43 immutable pre-draw OOS action log.
 *
 * <p>Up to Top-K qualified candidates may be stored for one target issue, each keyed by its
 * immutable contractDecisionId/candidateHash. Settlement only updates outcome columns. Real BET
 * accounting remains completely separate.
 */
public final class OosActionLogEngine {
    public static final String MODULE_VERSION = AppConfig.OOS_ACTION_LOG_VERSION;
    public static final String SOURCE_LIVE_REALTIME = "LIVE_REALTIME_TOPK";
    public static final String SOURCE_HISTORICAL_WALK_FORWARD = "HISTORICAL_WALK_FORWARD_TOPK";

    private OosActionLogEngine() {}

    public static long recordCandidate(
            DbStore store, Models.ComboCandidate c, String selectedMode) {
        if (!valid(c) || store == null) return -1L;
        if (c.oosRankSlot <= 0) c.oosRankSlot = 1;
        if (c.oosCandidateCount <= 0) c.oosCandidateCount = 1;
        ContentValues v = candidateValues(c, selectedMode, SOURCE_LIVE_REALTIME);
        return insertCandidate(store, v, c.contractDecisionId, c.targetIssue, c.candidateHash);
    }

    public static final class BatchWriteResult {
        public int requested, valid, inserted, duplicateExisting, failed;
        public String error = "";

        public boolean exactSuccess() {
            return requested == AppConfig.V48_FORMAL_CANDIDATE_COUNT
                    && valid == requested
                    && failed == 0
                    && inserted + duplicateExisting == requested;
        }

        public String summary() {
            return "requested="
                    + requested
                    + "|valid="
                    + valid
                    + "|inserted="
                    + inserted
                    + "|duplicate="
                    + duplicateExisting
                    + "|failed="
                    + failed
                    + "|error="
                    + error;
        }
    }

    public static BatchWriteResult recordCandidatesDetailed(
            DbStore store,
            java.util.List<Models.ComboCandidate> list,
            int maxCount,
            String selectedMode) {
        BatchWriteResult r = new BatchWriteResult();
        if (store == null || list == null || maxCount <= 0) {
            r.failed = Math.max(1, maxCount);
            r.error = "INVALID_INPUT";
            return r;
        }
        int cap = Math.min(AppConfig.V48_FORMAL_CANDIDATE_COUNT, Math.min(maxCount, list.size()));
        r.requested = cap;
        try (DatabaseCoordinator.WriterGuardCP3 cp3WriterGuard1 = DatabaseCoordinator.acquireWriterGuardCP3()) {
            SQLiteDatabase db = store.writable();
            db.beginTransaction();
            try {
                for (int i = 0; i < cap; i++) {
                    Models.ComboCandidate c = list.get(i);
                    if (!valid(c)) {
                        r.failed++;
                        continue;
                    }
                    r.valid++;
                    c.oosRankSlot = i + 1;
                    c.oosCandidateCount = cap;
                    ContentValues v = candidateValues(c, selectedMode, SOURCE_LIVE_REALTIME);
                    long existing =
                            findExistingCandidateId(
                                    db, c.contractDecisionId, c.targetIssue, c.candidateHash);
                    if (existing >= 0L) {
                        r.duplicateExisting++;
                        continue;
                    }
                    long id =
                            db.insertWithOnConflict(
                                    DbHelper.TABLE_OOS_ACTION_LOG,
                                    null,
                                    v,
                                    SQLiteDatabase.CONFLICT_IGNORE);
                    if (id >= 0L) r.inserted++;
                    else r.failed++;
                }
                db.setTransactionSuccessful();
            } catch (Throwable error) {
                r.failed = Math.max(r.failed, cap - r.inserted - r.duplicateExisting);
                r.error = error.getClass().getSimpleName() + ":" + safe(error.getMessage());
            } finally {
                try {
                    db.endTransaction();
                } catch (Throwable error) {
                    if (r.error.length() == 0)
                        r.error = "END_TX:" + error.getClass().getSimpleName();
                }
            }
        }
        try {
            store.setState("v48_oos_batch_write", r.summary());
            store.setState("v48_oos_batch_write_version", V48BuildManifest.OOS_VERSION);
        } catch (Throwable ignore) {
        }
        return r;
    }

    /** V47 records the exact final ranked 40-candidate surface. It never creates a real action. */
    public static int recordCandidates(
            DbStore store,
            java.util.List<Models.ComboCandidate> list,
            int maxCount,
            String selectedMode) {
        if (store == null || list == null || list.size() == 0 || maxCount <= 0) return 0;
        int cap = Math.min(AppConfig.V48_FORMAL_CANDIDATE_COUNT, Math.min(maxCount, list.size()));
        java.util.ArrayList<Models.ComboCandidate> chosen =
                new java.util.ArrayList<Models.ComboCandidate>();
        for (int i = 0; i < cap; i++) {
            Models.ComboCandidate c = list.get(i);
            if (valid(c)) chosen.add(c);
        }
        int inserted = 0;
        try (DatabaseCoordinator.WriterGuardCP3 cp3WriterGuard2 = DatabaseCoordinator.acquireWriterGuardCP3()) {
            SQLiteDatabase db = store.writable();
            db.beginTransaction();
            try {
                for (int i = 0; i < chosen.size(); i++) {
                    Models.ComboCandidate c = chosen.get(i);
                    if (!valid(c)) continue;
                    c.oosRankSlot = i + 1;
                    c.oosCandidateCount = chosen.size();
                    ContentValues v = candidateValues(c, selectedMode, SOURCE_LIVE_REALTIME);
                    long existing =
                            findExistingCandidateId(
                                    db, c.contractDecisionId, c.targetIssue, c.candidateHash);
                    if (existing < 0L) {
                        long id =
                                db.insertWithOnConflict(
                                        DbHelper.TABLE_OOS_ACTION_LOG,
                                        null,
                                        v,
                                        SQLiteDatabase.CONFLICT_IGNORE);
                        if (id >= 0L) inserted++;
                    }
                }
                db.setTransactionSuccessful();
            } catch (Throwable ignore) {
            } finally {
                try {
                    db.endTransaction();
                } catch (Throwable ignore) {
                }
            }
        }
        try {
            store.setState("v28_oos_last_topk_count", String.valueOf(chosen.size()));
            store.setState("v28_oos_last_inserted", String.valueOf(inserted));
            store.setState("v28_oos_policy", "V47_FINAL40_EXACT_RANK_IMMUTABLE");
            store.setState("v46_oos_policy", "V47_FINAL_EVIDENCE_AFTER_FINAL_RANK40_ONLY");
        } catch (Throwable ignore) {
        }
        return inserted;
    }

    /**
     * Updates V42 policy evidence only for the already-persisted live Top-K row. It never inserts a
     * new candidate and therefore adds only the already persisted final-contract rows. It never
     * inserts candidates outside the final ranked 40 surface.
     */
    public static void updateV42Policy(
            DbStore store, Models.ComboCandidate c, OosEligibilityEngine.Evaluation e) {
        if (store == null
                || c == null
                || e == null
                || c.oosRankSlot <= 0
                || c.oosRankSlot > AppConfig.V48_FORMAL_CANDIDATE_COUNT) return;
        try (DatabaseCoordinator.WriterGuardCP3 cp3WriterGuard3 = DatabaseCoordinator.acquireWriterGuardCP3()) {
            SQLiteDatabase db = store.writable();
            try {
                long id =
                        findExistingCandidateId(
                                db, c.contractDecisionId, c.targetIssue, c.candidateHash);
                if (id < 0L) return;
                ContentValues update = new ContentValues();
                update.put("selected_mode", e.eligible ? safe(e.lane) : "SHADOW");
                update.put("lane_candidate", safe(c.laneCandidate));
                putV42Fields(update, c);
                putV43Fields(update, c);
                putV46Fields(update, c);
                db.update(
                        DbHelper.TABLE_OOS_ACTION_LOG,
                        update,
                        "_id=?",
                        new String[] {String.valueOf(id)});
            } catch (Throwable ignore) {
            }
        }
    }

    /**
     * Final selected Decision updates the matching immutable candidate row; it does not erase
     * siblings.
     */
    public static long recordDecision(DbStore store, Models.Decision d) {
        if (store == null
                || d == null
                || d.contractValid != 1
                || safe(d.contractDecisionId).length() == 0
                || safe(d.targetIssue).length() == 0) return -1L;
        ContentValues v = decisionValues(d);
        try (DatabaseCoordinator.WriterGuardCP3 cp3WriterGuard4 = DatabaseCoordinator.acquireWriterGuardCP3()) {
            SQLiteDatabase db = store.writable();
            try {
                long id =
                        findExistingCandidateId(
                                db, d.contractDecisionId, d.targetIssue, d.candidateHash);
                if (id >= 0L) {
                    ContentValues update = new ContentValues();
                    update.put("selected_mode", safe(d.decision));
                    update.put("authorization_id", safe(d.authorizationId));
                    update.put("authorization_valid", d.authorizationValid);
                    update.put("authorization_reject_code", safe(d.authorizationRejectCode));
                    update.put("lane_candidate", safe(d.laneCandidate));
                    update.put("action_utility", d.actionUtility);
                    update.put("uncertainty_penalty", d.actionUncertaintyPenalty);
                    update.put("structure_score", d.actionStructureScore);
                    putV42Fields(update, d);
                    putV43Fields(update, d);
                    putV46Fields(update, d);
                    db.update(
                            DbHelper.TABLE_OOS_ACTION_LOG,
                            update,
                            "_id=?",
                            new String[] {String.valueOf(id)});
                    return id;
                }
                return db.insertWithOnConflict(
                        DbHelper.TABLE_OOS_ACTION_LOG, null, v, SQLiteDatabase.CONFLICT_IGNORE);
            } catch (Throwable e) {
                return -1L;
            }
        }
    }

    /** Historical origin prediction is inserted before the target draw is used. */
    public static long recordHistoricalCandidate(
            DbStore store, Models.ComboCandidate c, Models.Draw actual) {
        return recordHistoricalCandidate(
                store,
                c,
                actual,
                c == null ? 1 : Math.max(1, c.oosRankSlot),
                c == null ? 1 : Math.max(1, c.oosCandidateCount));
    }

    public static long recordHistoricalCandidate(
            DbStore store, Models.ComboCandidate c, Models.Draw actual, int slot, int count) {
        if (store == null
                || !valid(c)
                || actual == null
                || !actual.isValid()
                || !safe(c.targetIssue).equals(safe(actual.issue))) return -1L;
        c.oosRankSlot = Math.max(1, slot);
        c.oosCandidateCount = Math.max(c.oosRankSlot, count);
        ContentValues v = candidateValues(c, "HISTORICAL_OOS", SOURCE_HISTORICAL_WALK_FORWARD);
        v.put("authorization_valid", 0);
        v.put("authorization_reject_code", "HISTORICAL_REPLAY_NO_BET");

        try (DatabaseCoordinator.WriterGuardCP3 cp3WriterGuard5 = DatabaseCoordinator.acquireWriterGuardCP3()) {
            SQLiteDatabase db = store.writable();
            try {
                long id =
                        findExistingCandidateId(
                                db, c.contractDecisionId, c.targetIssue, c.candidateHash);
                if (id < 0L) {
                    id =
                            db.insertWithOnConflict(
                                    DbHelper.TABLE_OOS_ACTION_LOG,
                                    null,
                                    v,
                                    SQLiteDatabase.CONFLICT_IGNORE);
                }
                if (id < 0L) return -1L;
                int actualNumber = actual.getNumberAtPosition(c.position);
                int hit =
                        (actualNumber == c.n1 || actualNumber == c.n2 || actualNumber == c.n3)
                                ? 1
                                : 0;
                ContentValues out = new ContentValues();
                out.put("settled", 1);
                out.put("actual_number", actualNumber);
                out.put("hit", hit);
                out.put("result", hit == 1 ? AppConfig.RESULT_WIN : AppConfig.RESULT_MISS);
                out.put("settled_at", System.currentTimeMillis());
                db.update(
                        DbHelper.TABLE_OOS_ACTION_LOG,
                        out,
                        "_id=? AND settled=0",
                        new String[] {String.valueOf(id)});
                return id;
            } catch (Throwable e) {
                return -1L;
            }
        }
    }

    private static ContentValues candidateValues(
            Models.ComboCandidate c, String selectedMode, String source) {
        ContentValues v =
                baseValues(
                        c.contractDecisionId,
                        c.candidateHash,
                        c.baseIssue,
                        c.targetIssue,
                        c.position,
                        c.n1,
                        c.n2,
                        c.n3,
                        c.rawRankScore,
                        c.calibratedHitProbability,
                        c.dynamicGate,
                        c.coarseFamilyKey,
                        c.midFamilyKey,
                        c.fineFamilyKey,
                        c.positionFamilyKey,
                        c.snapshotMaxIssue,
                        c.trainedUntilIssue,
                        c.buildId,
                        c.cohortId,
                        c.schemaVersion,
                        c.pipelineVersion,
                        c.calibrationVersion,
                        c.gatePolicyVersion,
                        selectedMode,
                        c.authorizationId,
                        c.authorizationValid,
                        c.authorizationRejectCode,
                        source);
        putLearningFields(v, c);
        putV43Fields(v, c);
        putV46Fields(v, c);
        v.put("model_probability", c.modelP);
        OnlineActionModelEngine.putFeatureValues(v, c);
        return v;
    }

    private static ContentValues decisionValues(Models.Decision d) {
        ContentValues v =
                baseValues(
                        d.contractDecisionId,
                        d.candidateHash,
                        d.baseIssue,
                        d.targetIssue,
                        d.position,
                        d.n1,
                        d.n2,
                        d.n3,
                        d.rawRankScore,
                        d.calibratedHitProbability,
                        d.dynamicGate,
                        d.coarseFamilyKey,
                        d.midFamilyKey,
                        d.fineFamilyKey,
                        d.positionFamilyKey,
                        d.snapshotMaxIssue,
                        d.trainedUntilIssue,
                        d.buildId,
                        d.cohortId,
                        d.schemaVersion,
                        d.pipelineVersion,
                        d.calibrationVersion,
                        d.gatePolicyVersion,
                        d.decision,
                        d.authorizationId,
                        d.authorizationValid,
                        d.authorizationRejectCode,
                        SOURCE_LIVE_REALTIME);
        v.put("model_probability", d.modelP);
        v.put("feature_0", d.basePBeforeFusion > 0.0d ? d.basePBeforeFusion : d.modelP);
        for (int i = 1; i < AppConfig.ACTION_MODEL_FEATURE_COUNT; i++) v.put("feature_" + i, 0.5d);
        v.put("model_version", OnlineActionModelEngine.MODULE_VERSION);
        v.put(
                "honest_probability",
                d.honestProbability > 0.0d ? d.honestProbability : d.calibratedHitProbability);
        v.put("action_utility", d.actionUtility);
        v.put("structure_score", d.actionStructureScore);
        v.put("discrimination_score", d.actionDiscriminationScore);
        v.put("maturity_score", d.actionMaturityScore);
        v.put("uncertainty_penalty", d.actionUncertaintyPenalty);
        v.put("frequency_support", d.actionFrequencySupport);
        v.put("action_utility_version", safe(d.actionUtilityVersion));
        v.put("lane_candidate", safe(d.laneCandidate));
        v.put("rank_slot", Math.max(1, d.oosRankSlot));
        v.put("candidate_count", Math.max(1, d.oosCandidateCount));
        v.put("teacher_policy", safe(d.teacherPolicy));
        v.put("posterior_policy", safe(d.posteriorPolicy));
        v.put("window_state_key", safe(d.windowStateKey));
        v.put("dynamic_cluster_key", safe(d.clusterKey));
        v.put("coverage_quality", Math.max(d.coverageQuality, d.coverageQualityScore));
        v.put("third_quality", Math.max(d.thirdQuality, d.thirdNumberQualityScore));
        v.put(
                "counter_risk",
                Math.max(d.counterRisk, Math.max(d.counterEvidenceScore, d.evidenceRisk)));
        putV42Fields(v, d);
        putV43Fields(v, d);
        putV46Fields(v, d);
        return v;
    }

    private static void putLearningFields(ContentValues v, Models.ComboCandidate c) {
        v.put(
                "honest_probability",
                c.honestProbability > 0.0d ? c.honestProbability : c.calibratedHitProbability);
        v.put("action_utility", c.actionUtility);
        v.put("structure_score", c.actionStructureScore);
        v.put("discrimination_score", c.actionDiscriminationScore);
        v.put("maturity_score", c.actionMaturityScore);
        v.put("uncertainty_penalty", c.actionUncertaintyPenalty);
        v.put("frequency_support", c.actionFrequencySupport);
        v.put("action_utility_version", safe(c.actionUtilityVersion));
        v.put("lane_candidate", safe(c.laneCandidate));
        v.put("rank_slot", Math.max(1, c.oosRankSlot));
        v.put("candidate_count", Math.max(1, c.oosCandidateCount));
        v.put("teacher_policy", safe(c.teacherPolicy));
        v.put("posterior_policy", safe(c.posteriorPolicy));
        v.put("window_state_key", safe(c.windowStateKey));
        v.put("dynamic_cluster_key", safe(c.clusterKey));
        v.put("coverage_quality", Math.max(c.coverageQuality, c.coverageQualityScore));
        v.put("third_quality", Math.max(c.thirdQuality, c.thirdNumberQualityScore));
        v.put(
                "counter_risk",
                Math.max(c.counterRisk, Math.max(c.counterEvidenceScore, c.evidenceRisk)));
        putV42Fields(v, c);
    }

    private static void putV42Fields(ContentValues v, Object source) {
        if (v == null || source == null) return;
        v.put("v42_accept_score", optionalDouble(source, "v42AcceptScore"));
        v.put("v42_family_lcb", optionalDouble(source, "v42FamilyLcb"));
        v.put("v42_regime_quality", optionalDouble(source, "v42RegimeQuality"));
        v.put("v42_association_score", optionalDouble(source, "v42AssociationScore"));
        v.put("v42_distillation_risk", optionalDouble(source, "v42DistillationRisk"));
        v.put("v42_support_votes", optionalInt(source, "v42SupportVotes"));
        v.put("v42_regime_family", optionalString(source, "v42RegimeFamily"));
        v.put("v42_risk_coverage_mode", optionalString(source, "v42RiskCoverageMode"));
        v.put("v42_primary_reject", optionalString(source, "v42PrimaryReject"));
        v.put("v42_secondary_reject", optionalString(source, "v42SecondaryReject"));
        v.put("v42_tertiary_reject", optionalString(source, "v42TertiaryReject"));
        v.put("v42_lane_policy_version", optionalString(source, "v42LanePolicyVersion"));
    }

    private static void putV43Fields(ContentValues v, Object source) {
        if (v == null || source == null) return;
        v.put("v43_strategy_support", optionalDouble(source, "v43StrategySupport"));
        v.put("v43_strategy_risk", optionalDouble(source, "v43StrategyRisk"));
        v.put("v43_strategy_confidence", optionalDouble(source, "v43StrategyConfidence"));
        v.put("v43_strategy_diversity", optionalDouble(source, "v43StrategyDiversity"));
        v.put("v43_strategy_maturity", optionalDouble(source, "v43StrategyMaturity"));
        v.put("v43_safe_probability", optionalDouble(source, "v43SafeProbability"));
        v.put("v43_probability_residual", optionalDouble(source, "v43ProbabilityResidual"));
        v.put("v43_ablation_fragility", optionalDouble(source, "v43AblationFragility"));
        v.put("v43_probability_lock", optionalDouble(source, "v43ProbabilityLock"));
        v.put("v43_evidence_group_count", optionalInt(source, "v43EvidenceGroupCount"));
        v.put("v43_hard_risk", optionalInt(source, "v43HardRisk"));
        v.put("v43_dominant_evidence", optionalString(source, "v43DominantEvidence"));
        v.put("v43_missing_evidence", optionalString(source, "v43MissingEvidence"));
        v.put("v43_evidence_fingerprint", optionalString(source, "v43EvidenceFingerprint"));
        v.put("v43_probability_lock_hash", optionalString(source, "v43ProbabilityLockHash"));
        v.put("v43_pipeline_stage", optionalString(source, "v43PipelineStage"));
        v.put("v43_invariant_status", optionalString(source, "v43InvariantStatus"));
        v.put("v43_strategy_version", optionalString(source, "v43StrategyVersion"));
    }

    private static void putV46Fields(ContentValues v, Object source) {
        if (v == null || source == null) return;
        v.put("v46_event_probability", optionalDouble(source, "v46EventProbability"));
        v.put("v46_action_lcb", optionalDouble(source, "v46ActionLcb"));
        v.put("v46_probability_uncertainty", optionalDouble(source, "v46ProbabilityUncertainty"));
        v.put("v46_expected_value", optionalDouble(source, "v46ExpectedValue"));
        v.put(
                "v46_global_calibrated_probability",
                optionalDouble(source, "v46GlobalCalibratedProbability"));
        v.put("v46_position_adjustment", optionalDouble(source, "v46PositionAdjustment"));
        v.put("v46_rank_adjustment", optionalDouble(source, "v46RankAdjustment"));
        v.put("v46_regime_adjustment", optionalDouble(source, "v46RegimeAdjustment"));
        v.put("v46_calibration_confidence", optionalDouble(source, "v46CalibrationConfidence"));
        v.put("v46_final_rank", optionalInt(source, "v46FinalRank"));
        v.put("v46_formal_candidate_count", optionalInt(source, "v46FormalCandidateCount"));
        v.put("v46_version", optionalString(source, "v46Version"));
        v.put(
                "v46_number_distribution_summary",
                optionalString(source, "v46NumberDistributionSummary"));
        v.put("v46_probability_trace", optionalString(source, "v46ProbabilityTrace"));
        v.put("v46_root_cause_code", optionalString(source, "v46RootCauseCode"));
        v.put("v46_root_cause_owner", optionalString(source, "v46RootCauseOwner"));
        v.put("v46_final_contract_hash", optionalString(source, "v46FinalContractHash"));
        v.put("v46_lane", optionalString(source, "v46Lane"));
    }

    private static double optionalDouble(Object source, String field) {
        try {
            return source.getClass().getField(field).getDouble(source);
        } catch (Throwable ignore) {
            return 0.0d;
        }
    }

    private static int optionalInt(Object source, String field) {
        try {
            return source.getClass().getField(field).getInt(source);
        } catch (Throwable ignore) {
            return 0;
        }
    }

    private static String optionalString(Object source, String field) {
        try {
            Object v = source.getClass().getField(field).get(source);
            return safe(v == null ? "" : String.valueOf(v));
        } catch (Throwable ignore) {
            return "";
        }
    }

    private static ContentValues baseValues(
            String id,
            String hash,
            String base,
            String target,
            int position,
            int n1,
            int n2,
            int n3,
            double rank,
            double p,
            double gate,
            String coarse,
            String mid,
            String fine,
            String pos,
            String snapshot,
            String trained,
            String buildId,
            String cohortId,
            String schemaVersion,
            String pipeline,
            String calibration,
            String gateVersion,
            String selectedMode,
            String authId,
            int authValid,
            String reject,
            String source) {
        ContentValues v = new ContentValues();
        v.put("contract_decision_id", safe(id));
        v.put("candidate_hash", safe(hash));
        v.put("base_issue", safe(base));
        v.put("target_issue", safe(target));
        v.put("position", position);
        v.put("n1", n1);
        v.put("n2", n2);
        v.put("n3", n3);
        v.put("k3_mask", Models.buildK3Mask(n1, n2, n3));
        v.put("raw_rank_score", rank);
        v.put("calibrated_probability", p);
        v.put("gate", gate);
        v.put("edge", p - gate);
        v.put("coarse_family_key", safe(coarse));
        v.put("mid_family_key", safe(mid));
        v.put("fine_family_key", safe(fine));
        v.put("position_family_key", safe(pos));
        v.put("snapshot_max_issue", safe(snapshot));
        v.put("trained_until_issue", safe(trained));
        v.put("build_id", safe(buildId));
        v.put("cohort_id", safe(cohortId));
        v.put("schema_version", safe(schemaVersion));
        v.put("pipeline_version", safe(pipeline));
        v.put("calibration_version", safe(calibration));
        v.put("gate_policy_version", safe(gateVersion));
        v.put("selected_mode", safe(selectedMode));
        v.put("authorization_id", safe(authId));
        v.put("authorization_valid", authValid);
        v.put("authorization_reject_code", safe(reject));
        v.put("sample_source", safe(source));
        v.put("origin_group_key", safe(target) + "|" + safe(source));
        v.put("created_at", System.currentTimeMillis());
        v.put("settled", 0);
        v.put("processed", 0);
        v.put("version", MODULE_VERSION);
        return v;
    }

    private static long insertCandidate(
            DbStore store, ContentValues values, String contractId, String target, String hash) {
        try (DatabaseCoordinator.WriterGuardCP3 cp3WriterGuard6 = DatabaseCoordinator.acquireWriterGuardCP3()) {
            SQLiteDatabase db = store.writable();
            try {
                long existing = findExistingCandidateId(db, contractId, target, hash);
                if (existing >= 0L) return existing;
                return db.insertWithOnConflict(
                        DbHelper.TABLE_OOS_ACTION_LOG,
                        null,
                        values,
                        SQLiteDatabase.CONFLICT_IGNORE);
            } catch (Throwable e) {
                return -1L;
            }
        }
    }

    private static long findExistingCandidateId(
            SQLiteDatabase db, String contractId, String targetIssue, String candidateHash) {
        if (db == null) return -1L;
        Cursor c = null;
        try {
            if (safe(contractId).length() > 0) {
                c =
                        db.query(
                                DbHelper.TABLE_OOS_ACTION_LOG,
                                new String[] {"_id"},
                                "contract_decision_id=?",
                                new String[] {contractId},
                                null,
                                null,
                                "_id ASC",
                                "1");
                if (c != null && c.moveToFirst()) return c.getLong(0);
                if (c != null) {
                    try {
                        c.close();
                    } catch (Throwable ignore) {
                    }
                    c = null;
                }
            }
            c =
                    db.query(
                            DbHelper.TABLE_OOS_ACTION_LOG,
                            new String[] {"_id"},
                            "target_issue=? AND candidate_hash=? AND build_id=? AND cohort_id=? AND"
                                + " schema_version=? AND pipeline_version=? AND"
                                + " calibration_version=? AND version=?",
                            new String[] {
                                safe(targetIssue),
                                safe(candidateHash),
                                AppConfig.BUILD_ID,
                                AppConfig.COHORT_ID,
                                AppConfig.SCHEMA_VERSION,
                                AppConfig.PIPELINE_VERSION,
                                AppConfig.CALIBRATION_VERSION,
                                MODULE_VERSION
                            },
                            null,
                            null,
                            "_id ASC",
                            "1");
            return c != null && c.moveToFirst() ? c.getLong(0) : -1L;
        } catch (Throwable e) {
            return -1L;
        } finally {
            if (c != null)
                try {
                    c.close();
                } catch (Throwable ignore) {
                }
        }
    }

    private static java.util.ArrayList<Models.ComboCandidate> diversified(
            java.util.List<Models.ComboCandidate> list, int cap) {
        java.util.ArrayList<Models.ComboCandidate> out =
                new java.util.ArrayList<Models.ComboCandidate>();
        boolean[] usedPosition = new boolean[11];
        for (int pass = 0; pass < 2 && out.size() < cap; pass++) {
            for (int i = 0; i < list.size() && out.size() < cap; i++) {
                Models.ComboCandidate c = list.get(i);
                if (!valid(c) || out.contains(c)) continue;
                if (pass == 0 && c.position >= 1 && c.position <= 10 && usedPosition[c.position])
                    continue;
                out.add(c);
                if (c.position >= 1 && c.position <= 10) usedPosition[c.position] = true;
            }
        }
        return out;
    }

    public static int settleByDraw(DbStore store, Models.Draw draw) {
        if (store == null || draw == null || !draw.isValid()) return 0;
        int changed = 0;
        Cursor c = null;
        try (DatabaseCoordinator.WriterGuardCP3 cp3WriterGuard7 = DatabaseCoordinator.acquireWriterGuardCP3()) {
            SQLiteDatabase db = store.writable();
            try {
                c =
                        db.query(
                                DbHelper.TABLE_OOS_ACTION_LOG,
                                new String[] {"_id", "position", "n1", "n2", "n3"},
                                "target_issue=? AND settled=0",
                                new String[] {draw.issue},
                                null,
                                null,
                                "_id ASC");
                while (c != null && c.moveToNext()) {
                    long id = c.getLong(0);
                    int pos = c.getInt(1), a = c.getInt(2), b = c.getInt(3), d = c.getInt(4);
                    int actual = draw.getNumberAtPosition(pos);
                    int hit = (actual == a || actual == b || actual == d) ? 1 : 0;
                    ContentValues v = new ContentValues();
                    v.put("settled", 1);
                    v.put("actual_number", actual);
                    v.put("hit", hit);
                    v.put("result", hit == 1 ? AppConfig.RESULT_WIN : AppConfig.RESULT_MISS);
                    v.put("settled_at", System.currentTimeMillis());
                    v.put("settlement_task_id", draw.issue);
                    v.put("error_code", "");
                    changed +=
                            db.update(
                                    DbHelper.TABLE_OOS_ACTION_LOG,
                                    v,
                                    "_id=? AND settled=0",
                                    new String[] {String.valueOf(id)});
                }
            } catch (Throwable ignore) {
            } finally {
                if (c != null)
                    try {
                        c.close();
                    } catch (Throwable ignore) {
                    }
            }
        }
        try {
            store.setState("oos_last_settled_issue", draw.issue);
            store.setState("oos_last_settled_count", String.valueOf(changed));
        } catch (Throwable ignore) {
        }
        return changed;
    }

    /** Adds the settlement-chain reference without changing any frozen action or outcome field. */
    public static int linkSettlement(
            final DbStore store,
            final String targetIssue,
            final String settlementTaskId,
            final String errorCode) {
        if (store == null || safe(targetIssue).length() == 0) return 0;
        return store.coordinatorR36()
                .runWrite(
                        "R811_OOS_TASK_LINK",
                        new DatabaseCoordinator.WriteCallable<Integer>() {
                            public Integer call(SQLiteDatabase db) {
                                ContentValues values = new ContentValues();
                                values.put("settlement_task_id", safe(settlementTaskId));
                                values.put("error_code", safe(errorCode));
                                return Integer.valueOf(
                                        db.update(
                                                DbHelper.TABLE_OOS_ACTION_LOG,
                                                values,
                                                "target_issue=?",
                                                new String[] {safe(targetIssue)}));
                            }
                        },
                        Integer.valueOf(0))
                .intValue();
    }

    public static int countSettledUnprocessedForCurrentBuild(DbStore store) {
        if (store == null) return 0;
        Cursor c = null;
        try {
            c =
                    store.readable()
                            .rawQuery(
                                    "SELECT COUNT(*) FROM "
                                            + DbHelper.TABLE_OOS_ACTION_LOG
                                            + " WHERE settled=1 AND processed=0 AND build_id=? AND"
                                            + " cohort_id=? AND schema_version=? AND"
                                            + " pipeline_version=? AND calibration_version=? AND"
                                            + " version=?",
                                    new String[] {
                                        AppConfig.BUILD_ID,
                                        AppConfig.COHORT_ID,
                                        AppConfig.SCHEMA_VERSION,
                                        AppConfig.PIPELINE_VERSION,
                                        AppConfig.CALIBRATION_VERSION,
                                        MODULE_VERSION
                                    });
            return c != null && c.moveToFirst() ? c.getInt(0) : 0;
        } catch (Throwable e) {
            return 0;
        } finally {
            if (c != null)
                try {
                    c.close();
                } catch (Throwable ignore) {
                }
        }
    }

    public static int countSettledUnprocessed(DbStore store) {
        if (store == null) return 0;
        Cursor c = null;
        try {
            c =
                    store.readable()
                            .rawQuery(
                                    "SELECT COUNT(*) FROM "
                                            + DbHelper.TABLE_OOS_ACTION_LOG
                                            + " WHERE settled=1 AND processed=0",
                                    null);
            return c != null && c.moveToFirst() ? c.getInt(0) : 0;
        } catch (Throwable e) {
            return 0;
        } finally {
            if (c != null)
                try {
                    c.close();
                } catch (Throwable ignore) {
                }
        }
    }

    private static boolean valid(Models.ComboCandidate c) {
        return c != null
                && c.contractValid == 1
                && c.isValidK3()
                && safe(c.contractDecisionId).length() > 0
                && safe(c.targetIssue).length() > 0;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    public static final class V57AuthorizationAudit {
        public final String targetIssue;
        public final String decisionId;
        public final String action;
        public final String rejectCode;
        public final String contractHash;
        public final String authorizationHash;
        public final String championHash;
        public final boolean real;
        public final String checksum;

        V57AuthorizationAudit(
                String targetIssue,
                String decisionId,
                String action,
                String rejectCode,
                String contractHash,
                String authorizationHash,
                String championHash,
                boolean real) {
            this.targetIssue = safe(targetIssue);
            this.decisionId = safe(decisionId);
            this.action = safe(action);
            this.rejectCode = safe(rejectCode);
            this.contractHash = safe(contractHash);
            this.authorizationHash = safe(authorizationHash);
            this.championHash = safe(championHash);
            this.real = real;
            this.checksum = V51ChecksumEngine.sha256(canonical());
        }

        public String canonical() {
            return targetIssue
                    + '|'
                    + decisionId
                    + '|'
                    + action
                    + '|'
                    + rejectCode
                    + '|'
                    + contractHash
                    + '|'
                    + authorizationHash
                    + '|'
                    + championHash
                    + '|'
                    + (real ? 1 : 0);
        }
    }

    public static V57AuthorizationAudit recordV51AuthorizationAudit(
            DbStore store,
            V51Models.AuthorizedDecision decision,
            V51CompiledActionContract compiled) {
        if (decision == null || decision.selected == null || decision.selected.context == null) {
            return new V57AuthorizationAudit("", "", "HOLD", "DATA_INCOMPLETE", "", "", "", false);
        }
        String champion = store == null ? "" : store.getState("v57_b10_champion_hash", "");
        V57AuthorizationAudit audit =
                new V57AuthorizationAudit(
                        decision.selected.context.targetIssue,
                        decision.decisionId,
                        decision.action.name(),
                        decision.rejectCode.name(),
                        compiled == null ? "" : compiled.checksum,
                        decision.authorizationHash,
                        champion,
                        decision.action == V51Enums.DecisionAction.BET);
        if (store != null) {
            try {
                store.setState("v57_b12_last_authorization_audit", audit.canonical());
                store.setState("v57_b12_last_authorization_hash", audit.checksum);
            } catch (Throwable ignore) {
            }
        }
        return audit;
    }

    public static boolean v57CanCreateBet() {
        return false;
    }

    public static boolean v57CanSettle() {
        return false;
    }
}
