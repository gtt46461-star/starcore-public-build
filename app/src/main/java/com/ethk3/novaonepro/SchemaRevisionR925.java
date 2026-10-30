package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** R925 additive objects, split by R926 ownership. Compatibility stamp never fingerprints the full database. */
public final class SchemaRevisionR925 {
    public static final String REVISION = ReleaseManifestR926.R925_REVISION;
    public static final String TABLE_LIFECYCLE_AUDIT="v57_lifecycle_audit_r925";
    public static final String TABLE_CAPTURE_OPPORTUNITY="v57_capture_opportunity_r925";
    public static final String TABLE_PROMOTION_WINDOW="v57_promotion_window_r925";
    public static final String TABLE_PROMOTION_HISTORY="v57_promotion_history_r925";
    public static final String TABLE_HYPOTHESIS="v57_hypothesis_r925";
    public static final String TABLE_CANDIDATE_COVERAGE="v57_candidate_coverage_r925";
    public static final String TABLE_ATOM_QUALITY="v57_atom_quality_r925";
    public static final String TABLE_ATOM_EDGE="v57_atom_edge_r925";
    public static final String TABLE_FETCH_LEDGER="v57_fetch_ledger_r925";
    public static final String TABLE_PREDICTION_TIMING="v57_prediction_timing_r925";
    public static final String TABLE_RUNTIME_EPOCH="v57_runtime_epoch_r925";

    private static final String E02C_FTRL_SOURCE = "ResearchAdaptiveLearnerStoreR811";
    private static final String E02C_TINY_SOURCE = "ResearchTinyDeepLearnerR909";

    private SchemaRevisionR925() {}

    public static void ensureCriticalTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_FETCH_LEDGER
                + " (request_id TEXT PRIMARY KEY, method TEXT NOT NULL, endpoint TEXT NOT NULL,"
                + " request_at INTEGER NOT NULL, response_at INTEGER, latency_ms INTEGER, http_code INTEGER,"
                + " response_hash TEXT, source_line TEXT, latest_issue TEXT, status TEXT NOT NULL, error_code TEXT,"
                + " provider TEXT NOT NULL DEFAULT '', payload_hash TEXT NOT NULL DEFAULT '',"
                + " first_available_at INTEGER NOT NULL DEFAULT 0, received_at INTEGER NOT NULL DEFAULT 0,"
                + " draw_at INTEGER NOT NULL DEFAULT 0, clock_epoch TEXT NOT NULL DEFAULT '',"
                + " label_version TEXT NOT NULL DEFAULT '')");
        // DB user_version stays 36. These ALTERs are the same-version, additive coverage-upgrade path.
        addColumn(db, TABLE_FETCH_LEDGER, "provider TEXT NOT NULL DEFAULT ''");
        addColumn(db, TABLE_FETCH_LEDGER, "payload_hash TEXT NOT NULL DEFAULT ''");
        addColumn(db, TABLE_FETCH_LEDGER, "first_available_at INTEGER NOT NULL DEFAULT 0");
        addColumn(db, TABLE_FETCH_LEDGER, "received_at INTEGER NOT NULL DEFAULT 0");
        addColumn(db, TABLE_FETCH_LEDGER, "draw_at INTEGER NOT NULL DEFAULT 0");
        addColumn(db, TABLE_FETCH_LEDGER, "clock_epoch TEXT NOT NULL DEFAULT ''");
        addColumn(db, TABLE_FETCH_LEDGER, "label_version TEXT NOT NULL DEFAULT ''");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r925_fetch_time ON " + TABLE_FETCH_LEDGER + "(request_at)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r925_fetch_issue_available ON "
                + TABLE_FETCH_LEDGER + "(latest_issue,first_available_at)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PREDICTION_TIMING + " (target_issue TEXT PRIMARY KEY, process_id TEXT NOT NULL, release_code INTEGER NOT NULL, request_at INTEGER NOT NULL, runtime_at INTEGER NOT NULL, durable_at INTEGER NOT NULL, queue_ms INTEGER NOT NULL, compute_ms INTEGER NOT NULL, commit_ms INTEGER NOT NULL, total_ms INTEGER NOT NULL, sample_class TEXT NOT NULL)");
        addColumn(db, TABLE_PREDICTION_TIMING, "attempt_id TEXT NOT NULL DEFAULT ''");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_RUNTIME_EPOCH + " (epoch_id TEXT PRIMARY KEY, kind TEXT NOT NULL, started_at INTEGER NOT NULL, start_issue TEXT NOT NULL, release_code INTEGER NOT NULL)");

        // E02C-A03: after a FTRL LEARNED_EXACTLY_ONCE event exists, the exact label/evidence
        // surface that forms its learning identity is immutable. A correction must use an explicit
        // rebuild/version path; Batch2 deliberately fails closed instead of silently reusing an old
        // artifact. SQLite owns the invariant so restart and dual-worker races cannot bypass it.
        RuntimeResourceAuditR927.ensure(db);
        ensureE02cFtrlInputFreeze(db);
    }

    public static void ensureResearchTables(SQLiteDatabase db) {
        if (com.ethk3.novaonepro.strategyv2.ResearchRouteR29.isResearchDatabase(db))
            LegacyLearningCommitR29.installResearch(db);
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_LIFECYCLE_AUDIT + " (genome_id TEXT PRIMARY KEY, first_prune_reason TEXT NOT NULL, first_prune_metrics TEXT NOT NULL, first_prune_issue TEXT NOT NULL, first_prune_at INTEGER NOT NULL, prune_policy_version TEXT NOT NULL, original_available INTEGER NOT NULL, latest_status TEXT NOT NULL, latest_reason TEXT NOT NULL, updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CAPTURE_OPPORTUNITY + " (target_issue TEXT NOT NULL, genome_id TEXT NOT NULL, opportunity_at INTEGER NOT NULL, status TEXT NOT NULL, cost_ms INTEGER NOT NULL DEFAULT 0, reason TEXT NOT NULL DEFAULT '', PRIMARY KEY(target_issue,genome_id))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r925_capture_genome ON " + TABLE_CAPTURE_OPPORTUNITY + "(genome_id,target_issue)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PROMOTION_WINDOW + " (genome_id TEXT PRIMARY KEY, start_issue TEXT NOT NULL, initial_n INTEGER NOT NULL, initial_evidence_hash TEXT NOT NULL, registered_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PROMOTION_HISTORY + " (genome_id TEXT NOT NULL, start_issue TEXT NOT NULL, initial_n INTEGER NOT NULL, initial_evidence_hash TEXT NOT NULL, registered_at INTEGER NOT NULL, PRIMARY KEY(genome_id,start_issue))");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_HYPOTHESIS + " (genome_id TEXT PRIMARY KEY, family_id TEXT NOT NULL, structural_hash TEXT NOT NULL, registration_origin TEXT NOT NULL, registered_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CANDIDATE_COVERAGE + " (genome_id TEXT PRIMARY KEY, system_capture_coverage REAL NOT NULL, candidate_execution_coverage REAL, execution_opportunities INTEGER NOT NULL, coverage_contract TEXT NOT NULL, updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ATOM_QUALITY + " (atom_id TEXT PRIMARY KEY, support_issues INTEGER NOT NULL, mean REAL NOT NULL, variance REAL NOT NULL, drift_score REAL NOT NULL, observation_state TEXT NOT NULL, semantic_tier TEXT NOT NULL, last_issue TEXT NOT NULL, consumer_scope TEXT NOT NULL, updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ATOM_EDGE + " (left_atom TEXT NOT NULL, right_atom TEXT NOT NULL, correlation REAL NOT NULL, support_issues INTEGER NOT NULL, last_issue TEXT NOT NULL, PRIMARY KEY(left_atom,right_atom))");

        // R930 Batch-3/4: additive, restart-safe evidence stores and immutable ledgers. This DDL is
        // deliberately executed by the Research schema owner, never by the per-issue hot path.
        Batch34SchemaR930.ensure(db);

        // E02C-A03 TinyDeep side of the same contract. Only the PRODUCTION_SELECTED compact label
        // is a training source, so unrelated research assets remain mutable.
        ensureE02cTinyDeepInputFreeze(db);
    }

    private static void ensureE02cFtrlInputFreeze(SQLiteDatabase db) {
        String learnedOld = learnedExistsSql("OLD.target_issue", E02C_FTRL_SOURCE);
        String learnedNew = learnedExistsSql("NEW.target_issue", E02C_FTRL_SOURCE);

        db.execSQL("CREATE TRIGGER IF NOT EXISTS trg_r927_e02c_ftrl_oos_update_freeze"
                + " BEFORE UPDATE OF target_issue,strict_oos,prediction_created_at,draw_open_time,hit_label,selected_position,selected_hit_p"
                + " ON " + DbHelper.TABLE_R35_OOS_LABEL
                + " WHEN " + learnedOld
                + " BEGIN SELECT RAISE(ABORT,'E02C_FTRL_LABEL_FROZEN_AFTER_LEARN'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS trg_r927_e02c_ftrl_oos_delete_freeze"
                + " BEFORE DELETE ON " + DbHelper.TABLE_R35_OOS_LABEL
                + " WHEN " + learnedOld
                + " BEGIN SELECT RAISE(ABORT,'E02C_FTRL_LABEL_FROZEN_AFTER_LEARN'); END");

        db.execSQL("CREATE TRIGGER IF NOT EXISTS trg_r927_e02c_ftrl_evidence_update_freeze"
                + " BEFORE UPDATE OF target_issue,selected_position,raw_action_p_value,rank_score_value,third_margin_value,entropy_risk_value,stability_value,window_consensus_value,raw_action_p_status,rank_status,third_margin_status,entropy_risk_status,stability_status,window_consensus_status"
                + " ON " + DbHelper.TABLE_R906_SELECTED_EVIDENCE
                + " WHEN " + learnedOld
                + " BEGIN SELECT RAISE(ABORT,'E02C_FTRL_EVIDENCE_FROZEN_AFTER_LEARN'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS trg_r927_e02c_ftrl_evidence_delete_freeze"
                + " BEFORE DELETE ON " + DbHelper.TABLE_R906_SELECTED_EVIDENCE
                + " WHEN " + learnedOld
                + " BEGIN SELECT RAISE(ABORT,'E02C_FTRL_EVIDENCE_FROZEN_AFTER_LEARN'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS trg_r927_e02c_ftrl_evidence_insert_freeze"
                + " BEFORE INSERT ON " + DbHelper.TABLE_R906_SELECTED_EVIDENCE
                + " WHEN " + learnedNew
                + " BEGIN SELECT RAISE(ABORT,'E02C_FTRL_EVIDENCE_FROZEN_AFTER_LEARN'); END");
    }

    private static void ensureE02cTinyDeepInputFreeze(SQLiteDatabase db) {
        if (com.ethk3.novaonepro.strategyv2.ResearchRouteR29.isResearchDatabase(db)) {
            // The compact label belongs to RP after cutover. The Root event ledger remains
            // authoritative in Root: use a distinct, immutable projection of real learning
            // facts and fail closed until its bootstrap is complete. Never invent that ledger
            // in the research database merely to make a cross-domain trigger compile.
            com.ethk3.novaonepro.strategyv2.LegacyLearnedInputR29.installResearch(db);
            return;
        }
        String learnedOld = learnedExistsSql("OLD.target_issue", E02C_TINY_SOURCE);
        db.execSQL("CREATE TRIGGER IF NOT EXISTS trg_r927_e02c_tiny_label_update_freeze"
                + " BEFORE UPDATE OF asset_id,target_issue,feature_blob,hit_label"
                + " ON " + DbHelper.TABLE_R906_RESEARCH_COMPACT_LABEL
                + " WHEN OLD.asset_id='PRODUCTION_SELECTED' AND " + learnedOld
                + " BEGIN SELECT RAISE(ABORT,'E02C_TINY_LABEL_FROZEN_AFTER_LEARN'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS trg_r927_e02c_tiny_label_delete_freeze"
                + " BEFORE DELETE ON " + DbHelper.TABLE_R906_RESEARCH_COMPACT_LABEL
                + " WHEN OLD.asset_id='PRODUCTION_SELECTED' AND " + learnedOld
                + " BEGIN SELECT RAISE(ABORT,'E02C_TINY_LABEL_FROZEN_AFTER_LEARN'); END");
    }

    private static String learnedExistsSql(String issueExpression, String source) {
        return "EXISTS(SELECT 1 FROM " + RuntimeEventLedgerR926.TABLE_EVENT
                + " WHERE game_code='" + ReleaseManifestR926.GAME_CODE + "'"
                + " AND target_issue=" + issueExpression
                + " AND event_type='LEARNED' AND stage='LEARNED_EXACTLY_ONCE'"
                + " AND source='" + source + "')";
    }

    private static void addColumn(SQLiteDatabase db, String table, String definition) {
        if (db == null || table == null || definition == null) return;
        int split = definition.indexOf(' ');
        if (split <= 0) throw new IllegalArgumentException("R925_COLUMN_DEFINITION_INVALID");
        String column = definition.substring(0, split);
        if (columnExists(db, table, column)) return;
        db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + definition);
    }

    private static boolean columnExists(SQLiteDatabase db, String table, String column) {
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("PRAGMA table_info(" + table + ")", null);
            while (cursor.moveToNext()) if (column.equals(cursor.getString(1))) return true;
            return false;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    /** Legacy compatibility entry point. Explicit callers asking for all R925 objects still get both domains. */
    public static void ensureTables(SQLiteDatabase db) { ensureCriticalTables(db); ensureResearchTables(db); }
    public static boolean matches(SQLiteDatabase db) { return SchemaDomainCoordinatorR926.matches(db, SchemaDomainCoordinatorR926.criticalContract()); }
    public static void stamp(SQLiteDatabase db) { SchemaDomainCoordinatorR926.verifyAndStamp(db, SchemaDomainCoordinatorR926.criticalContract()); }
    static String fingerprint(SQLiteDatabase db) { return SchemaDomainCoordinatorR926.fingerprint(db, SchemaDomainCoordinatorR926.criticalContract()); }
}
