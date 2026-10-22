package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Durable post-settlement FTRL Research learner; artifacts can only be consumed by SHADOW.
 *
 * <p>B09: every new live strict-OOS artifact and its immutable LEARNED_EXACTLY_ONCE event are
 * committed in the same SQLite transaction. The transaction also revalidates the current FTRL tail
 * against the pre-state hash used for computation, so concurrent target issues cannot fork or skip
 * the linear model history. Existing pre-B09 artifacts remain idempotent history.</p>
 */
public final class ResearchAdaptiveLearnerStoreR811 {
    public static final String FEATURE_SCHEMA =
            "R811_FTRL_FEATURES_V1|raw_action_p|rank_score|third_margin|entropy|stability|"
                    + "effective_consensus|selected_hit_p";
    public static final String FEATURE_SCHEMA_HASH = V51ChecksumEngine.sha256(FEATURE_SCHEMA);
    private static final String UPDATE_CONTRACT = AdaptiveShadowLearnerEngine.UPDATE_RULE;
    private static final String LABEL_VERSION = "STRICT_OOS_SETTLED_PREDRAW_V1";
    private static final String GENESIS_HASH =
            V51ChecksumEngine.sha256("R811_FTRL_GENESIS|" + FEATURE_SCHEMA_HASH);

    private ResearchAdaptiveLearnerStoreR811() {}

    public static UpdateResult updateFromStrictOos(DbStore store, String targetIssue) {
        if (store == null || !TargetIssueResolver.isValid(targetIssue))
            return UpdateResult.fail("INVALID_INPUT");
        if (RealtimePressureGateR811.hasActiveCriticalWork()
                && AnalysisSingleFlightR36.shared().activeCount() > 0)
            return UpdateResult.fail("YIELD_REALTIME");
        if (!store.coordinatorR36().ensureR811ResearchSchemaOnce())
            return UpdateResult.fail("SCHEMA_NOT_READY");

        Existing existing = existingForIssue(store, targetIssue);
        if (existing != null)
            return new UpdateResult(
                    true,
                    "REPLAY_IDEMPOTENT_ARTIFACT_PRESENT",
                    targetIssue,
                    existing.artifactHash,
                    existing.updates);

        String latestTrained = latestTrainedIssue(store);
        if (latestTrained.length() > 0
                && TargetIssueResolver.compare(latestTrained, targetIssue) >= 0)
            return UpdateResult.fail(
                    "BACKFILL_LEARNING_FORBIDDEN|latest="
                            + latestTrained
                            + "|target="
                            + targetIssue);

        final Input input = readInput(store, targetIssue);
        if (input == null) return UpdateResult.fail("STRICT_OOS_OR_PREDRAW_FEATURE_MISSING");
        ResearchArtifactCodecR811.Artifact previous = latestBefore(store, targetIssue);
        final String preStateHash =
                previous == null ? GENESIS_HASH : ResearchArtifactCodecR811.hash(previous);
        AdaptiveShadowLearnerEngine learner = AdaptiveShadowLearnerEngine.restore(previous);
        AdaptiveShadowLearnerEngine.Update update =
                learner.updateAfterSettlement(
                        new AdaptiveShadowLearnerEngine.OosRow(
                                targetIssue,
                                input.frozenAt,
                                input.drawOpenTime,
                                true,
                                input.label,
                                input.features));
        if (!update.updated) return UpdateResult.fail(update.status);

        final ResearchArtifactCodecR811.Artifact artifact = learner.snapshotResearchArtifact();
        if (!targetIssue.equals(artifact.trainedUntilIssue))
            return UpdateResult.fail("TRAINED_UNTIL_MISMATCH");
        final byte[] encoded = ResearchArtifactCodecR811.encode(artifact);
        final String hash = ResearchArtifactCodecR811.hash(artifact);
        if (!ResearchArtifactCodecR811.verify(encoded, hash))
            return UpdateResult.fail("ARTIFACT_SELF_VERIFY_FAILED");

        final String inputHash = inputHash(targetIssue, input);
        final String learningIdentity =
                Batch2ContractCoreR926.learningKey(
                        V51Repository.GAME_CODE,
                        targetIssue,
                        UPDATE_CONTRACT,
                        LABEL_VERSION,
                        inputHash);
        if (!Batch2ContractCoreR926.isSha256(preStateHash)
                || !Batch2ContractCoreR926.isSha256(hash)
                || !Batch2ContractCoreR926.isSha256(learningIdentity))
            return UpdateResult.fail("B09_LEARNING_HASH_INVALID");

        final long updates = update.updates;
        DatabaseCoordinator.TransactionResult<Boolean> tx =
                store.coordinatorR36()
                        .runTransaction(
                                "R811_FTRL_ARTIFACT_B09_ATOMIC",
                                targetIssue,
                                learningIdentity,
                                new DatabaseCoordinator.TransactionCallable<Boolean>() {
                                    public Boolean call(
                                            SQLiteDatabase db,
                                            DatabaseCoordinator.TransactionContext context) {
                                        if (!db.inTransaction())
                                            throw new IllegalStateException(
                                                    "B09_FTRL_TRANSACTION_NOT_ACTIVE");
                                        if (!tailMatchesPreState(db, targetIssue, preStateHash))
                                            throw new IllegalStateException(
                                                    "B09_FTRL_STALE_PRE_STATE");
                                        if (!targetCompatible(db, artifact.type, targetIssue, hash))
                                            throw new IllegalStateException(
                                                    "B09_FTRL_TARGET_ARTIFACT_CONFLICT");

                                        ContentValues values = new ContentValues();
                                        values.put(
                                                "artifact_id",
                                                "R811-FTRL-" + hash.substring(0, 32));
                                        values.put("artifact_type", artifact.type);
                                        values.put("trained_until_issue", artifact.trainedUntilIssue);
                                        values.put("feature_schema_hash", FEATURE_SCHEMA_HASH);
                                        values.put("artifact_blob", encoded);
                                        values.put("artifact_hash", hash);
                                        values.put(
                                                "status",
                                                "RESEARCH_ONLY_SHADOW_LOADABLE_B09_ATOMIC");
                                        values.put("created_at", TimeSourceR811.wallNowMs());
                                        long row =
                                                db.insertWithOnConflict(
                                                        "v57_research_artifact_r811",
                                                        null,
                                                        values,
                                                        SQLiteDatabase.CONFLICT_IGNORE);
                                        boolean artifactDurable =
                                                row >= 0L || matches(db, artifact, hash);
                                        if (!artifactDurable)
                                            throw new IllegalStateException(
                                                    "B09_FTRL_ARTIFACT_CONFLICT");

                                        RuntimeEventHooksR926.learnedWithinTransaction(
                                                db,
                                                targetIssue,
                                                "ResearchAdaptiveLearnerStoreR811",
                                                learningIdentity,
                                                preStateHash,
                                                hash,
                                                artifact.trainedUntilIssue,
                                                "contract="
                                                        + UPDATE_CONTRACT
                                                        + "|label_version="
                                                        + LABEL_VERSION
                                                        + "|input_hash="
                                                        + inputHash
                                                        + "|updates="
                                                        + updates
                                                        + "|tx_hash="
                                                        + safe(context.transactionHash));
                                        return Boolean.TRUE;
                                    }
                                },
                                Boolean.FALSE);
        Existing durable = existingForIssue(store, targetIssue);
        if (!tx.success
                || durable == null
                || !hash.equals(durable.artifactHash))
            return UpdateResult.fail(
                    "ARTIFACT_OR_LEARNED_TX_FAILED|"
                            + safe(tx.code)
                            + "|replay="
                            + (tx.replay ? 1 : 0));

        Map<String, String> state = new LinkedHashMap<String, String>();
        state.put("r811_ftrl_trained_until", targetIssue);
        state.put("r811_ftrl_artifact_hash", hash);
        state.put("r811_ftrl_updates", String.valueOf(durable.updates));
        state.put("r811_ftrl_learning_identity", learningIdentity);
        state.put("r811_ftrl_b09_status", "ARTIFACT_AND_LEARNED_ATOMIC_LINEAR");
        state.put("r811_ftrl_b09_tx_hash", safe(tx.transactionHash));
        // Artifact + learned receipt are already committed above. A dashboard
        // write cannot change that authoritative result or skip the next learner.
        store.trySetTelemetryStatesR77(state);
        return new UpdateResult(
                true,
                tx.replay
                        ? "RESEARCH_REPLAY_B09_EXACTLY_ONCE"
                        : "RESEARCH_UPDATED_B09_EXACTLY_ONCE",
                targetIssue,
                hash,
                durable.updates);
    }

    public static ResearchArtifactCodecR811.Artifact loadForTarget(
            DbStore store, String targetIssue) {
        if (store == null || !TargetIssueResolver.isValid(targetIssue)) return null;
        ResearchArtifactCodecR811.Artifact artifact = latestBefore(store, targetIssue);
        return artifact != null
                        && TargetIssueResolver.compare(artifact.trainedUntilIssue, targetIssue) < 0
                ? artifact
                : null;
    }

    public static String nextTrainableIssue(DbStore store) {
        String trained = latestTrainedIssue(store);
        Cursor c = null;
        try {
            c =
                    store.readable()
                            .rawQuery(
                                    "SELECT o.target_issue FROM "
                                            + DbHelper.TABLE_R35_OOS_LABEL
                                            + " o JOIN "
                                            + DbHelper.TABLE_R906_SELECTED_EVIDENCE
                                            + " e ON e.target_issue=o.target_issue JOIN "
                                            + DbHelper.TABLE_R36_SETTLEMENT_TASK
                                            + " t ON t.target_issue=o.target_issue JOIN "
                                            + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                                            + " s ON s.target_issue=o.target_issue"
                                            + " WHERE o.target_issue>? AND o.strict_oos=1 AND o.hit_label IN (0,1)"
                                            + " AND t.status='COMPLETED' AND s.pre_draw_frozen=1 AND s.settled=1"
                                            + " AND o.prediction_created_at>0 AND o.draw_open_time>o.prediction_created_at"
                                            + " AND e.raw_action_p_status='AVAILABLE' AND e.rank_status='AVAILABLE'"
                                            + " AND e.third_margin_status='AVAILABLE' AND e.entropy_risk_status='AVAILABLE'"
                                            + " AND e.stability_status='AVAILABLE' AND e.window_consensus_status='AVAILABLE'"
                                            + " ORDER BY o.target_issue ASC LIMIT 1",
                                    new String[] {trained});
            return c.moveToFirst() ? safe(c.getString(0)) : "";
        } finally {
            if (c != null) c.close();
        }
    }

    private static Input readInput(DbStore store, String target) {
        Cursor cursor = null;
        try {
            cursor =
                    store.readable()
                            .rawQuery(
                                    "SELECT o.prediction_created_at,o.draw_open_time,o.hit_label,"
                                            + "o.selected_hit_p,e.raw_action_p_value,e.rank_score_value,"
                                            + "e.third_margin_value,e.entropy_risk_value,e.stability_value,"
                                            + "e.window_consensus_value,e.raw_action_p_status,e.rank_status,"
                                            + "e.third_margin_status,e.entropy_risk_status,e.stability_status,e.window_consensus_status"
                                            + " FROM "
                                            + DbHelper.TABLE_R35_OOS_LABEL
                                            + " o JOIN "
                                            + DbHelper.TABLE_R906_SELECTED_EVIDENCE
                                            + " e ON e.target_issue=o.target_issue"
                                            + " JOIN "
                                            + DbHelper.TABLE_R36_SETTLEMENT_TASK
                                            + " t ON t.target_issue=o.target_issue"
                                            + " JOIN "
                                            + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                                            + " s ON s.target_issue=o.target_issue"
                                            + " WHERE o.target_issue=? AND o.strict_oos=1 AND t.status='COMPLETED'"
                                            + " AND s.pre_draw_frozen=1 AND s.settled=1 LIMIT 1",
                                    new String[] {target});
            if (!cursor.moveToFirst()) return null;
            for (int i = 10; i <= 15; i++)
                if (!"AVAILABLE".equals(safe(cursor.getString(i)))) return null;
            double[] features =
                    new double[] {
                        finite(cursor.getDouble(4)),
                        finite(cursor.getDouble(5)),
                        finite(cursor.getDouble(6)),
                        finite(cursor.getDouble(7)),
                        finite(cursor.getDouble(8)),
                        finite(cursor.getDouble(9)),
                        finite(cursor.getDouble(3))
                    };
            return new Input(cursor.getLong(0), cursor.getLong(1), cursor.getInt(2), features);
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static String inputHash(String target, Input input) {
        StringBuilder b =
                new StringBuilder("R811_FTRL_INPUT|")
                        .append(safe(target))
                        .append('|')
                        .append(input.frozenAt)
                        .append('|')
                        .append(input.drawOpenTime)
                        .append('|')
                        .append(input.label);
        for (double v : input.features) b.append('|').append(Double.toString(finite(v)));
        return V51ChecksumEngine.sha256(b.toString());
    }

    private static ResearchArtifactCodecR811.Artifact latestBefore(
            DbStore store, String targetIssue) {
        Cursor cursor = null;
        try {
            cursor =
                    store.readable()
                            .rawQuery(
                                    "SELECT artifact_blob,artifact_hash,feature_schema_hash FROM"
                                            + " v57_research_artifact_r811 WHERE artifact_type='FTRL_SHADOW'"
                                            + " AND trained_until_issue<? ORDER BY trained_until_issue DESC LIMIT 1",
                                    new String[] {targetIssue});
            if (!cursor.moveToFirst()) return null;
            byte[] bytes = cursor.getBlob(0);
            String hash = safe(cursor.getString(1));
            if (!FEATURE_SCHEMA_HASH.equals(safe(cursor.getString(2)))
                    || !ResearchArtifactCodecR811.verify(bytes, hash)) return null;
            return ResearchArtifactCodecR811.decode(bytes);
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static Existing existingForIssue(DbStore store, String issue) {
        Cursor cursor = null;
        try {
            cursor =
                    store.readable()
                            .rawQuery(
                                    "SELECT artifact_blob,artifact_hash FROM v57_research_artifact_r811"
                                            + " WHERE artifact_type='FTRL_SHADOW' AND trained_until_issue=? LIMIT 1",
                                    new String[] {issue});
            if (!cursor.moveToFirst()) return null;
            byte[] bytes = cursor.getBlob(0);
            String hash = safe(cursor.getString(1));
            if (!ResearchArtifactCodecR811.verify(bytes, hash)) return null;
            ResearchArtifactCodecR811.Artifact artifact = ResearchArtifactCodecR811.decode(bytes);
            double[] meta = artifact.fields.get("meta");
            long updates = meta == null || meta.length == 0 ? 0L : (long) meta[0];
            return new Existing(hash, updates);
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static String latestTrainedIssue(DbStore store) {
        Cursor cursor = null;
        try {
            cursor =
                    store.readable()
                            .rawQuery(
                                    "SELECT trained_until_issue FROM v57_research_artifact_r811"
                                            + " WHERE artifact_type='FTRL_SHADOW'"
                                            + " ORDER BY trained_until_issue DESC LIMIT 1",
                                    null);
            return cursor.moveToFirst() ? safe(cursor.getString(0)) : "";
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    /** Commit-time compare-and-swap for the linear FTRL chain. */
    private static boolean tailMatchesPreState(
            SQLiteDatabase db, String targetIssue, String expectedPreStateHash) {
        Cursor cursor = null;
        try {
            cursor =
                    db.rawQuery(
                            "SELECT trained_until_issue,artifact_hash FROM v57_research_artifact_r811"
                                    + " WHERE artifact_type='FTRL_SHADOW'"
                                    + " ORDER BY trained_until_issue DESC,created_at DESC LIMIT 1",
                            null);
            if (!cursor.moveToFirst()) return GENESIS_HASH.equals(expectedPreStateHash);
            String tailIssue = safe(cursor.getString(0));
            String tailHash = safe(cursor.getString(1));
            if (!TargetIssueResolver.isValid(tailIssue)
                    || TargetIssueResolver.compare(tailIssue, targetIssue) >= 0) return false;
            return tailHash.equals(expectedPreStateHash);
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    /** No issue may hold two different FTRL artifacts even though legacy schema lacks that UNIQUE. */
    private static boolean targetCompatible(
            SQLiteDatabase db, String artifactType, String issue, String expectedHash) {
        Cursor cursor = null;
        try {
            cursor =
                    db.rawQuery(
                            "SELECT artifact_hash FROM v57_research_artifact_r811"
                                    + " WHERE artifact_type=? AND trained_until_issue=?",
                            new String[] {safe(artifactType), safe(issue)});
            while (cursor.moveToNext())
                if (!expectedHash.equals(safe(cursor.getString(0)))) return false;
            return true;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static boolean matches(
            SQLiteDatabase db, ResearchArtifactCodecR811.Artifact artifact, String hash) {
        Cursor cursor = null;
        try {
            cursor =
                    db.rawQuery(
                            "SELECT artifact_hash,feature_schema_hash FROM"
                                    + " v57_research_artifact_r811 WHERE artifact_type=?"
                                    + " AND trained_until_issue=?",
                            new String[] {artifact.type, artifact.trainedUntilIssue});
            boolean found = false;
            while (cursor.moveToNext()) {
            ResearchWorkBudgetR927.checkpoint();
                found = true;
                if (!hash.equals(safe(cursor.getString(0)))
                        || !FEATURE_SCHEMA_HASH.equals(safe(cursor.getString(1)))) return false;
            }
            return found;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static double finite(double value) {
        return Double.isNaN(value) || Double.isInfinite(value) ? 0.0d : value;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class Input {
        final long frozenAt, drawOpenTime;
        final int label;
        final double[] features;

        Input(long frozen, long open, int label, double[] features) {
            frozenAt = frozen;
            drawOpenTime = open;
            this.label = label;
            this.features = features;
        }
    }

    private static final class Existing {
        final String artifactHash;
        final long updates;

        Existing(String hash, long updates) {
            artifactHash = hash;
            this.updates = updates;
        }
    }

    public static final class UpdateResult {
        public final boolean updated;
        public final String status, trainedUntilIssue, artifactHash;
        public final long updates;

        UpdateResult(boolean updated, String status, String issue, String hash, long updates) {
            this.updated = updated;
            this.status = safe(status);
            trainedUntilIssue = safe(issue);
            artifactHash = safe(hash);
            this.updates = updates;
        }

        static UpdateResult fail(String status) {
            return new UpdateResult(false, status, "", "", 0L);
        }
    }
}
