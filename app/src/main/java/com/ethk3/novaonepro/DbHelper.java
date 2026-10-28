package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

public final class DbHelper extends SQLiteOpenHelper {
    public static final String MODULE_BUILD_VERSION = AppConfig.ACTIVE_QUALITY_BUILD_VERSION;
    public static final String OUMAN_GALAXY_REBUILD_VERSION =
            AppConfig.OUMAN_GALAXY_REBUILD_VERSION;

    public static final String DBHELPER_DEADLOCK_RECOVERY_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";

    public static final String TABLE_OOS_ACTION_LOG = "oos_action_log_v28";
    public static final String TABLE_HIERARCHICAL_POSTERIOR = "hierarchical_posterior_v28";
    public static final String TABLE_MONOTONIC_CALIBRATION = "monotonic_calibration_v28";
    public static final String TABLE_DECISION_AUTHORIZATION = "decision_authorization_v28";
    public static final String TABLE_V48_DIAGNOSTIC_OBSERVATION = "v48_diagnostic_observation";
    public static final String TABLE_V48_MODEL_ARTIFACT = "v48_model_artifact";

    public static final String TABLE_DRAWS = "draws";
    public static final String TABLE_DECISIONS = "decisions";
    public static final String TABLE_TRADES = "trades";
    public static final String TABLE_R35_SHADOW_OBSERVATION = "v57_shadow_observation_r35";
    public static final String TABLE_R35_OOS_LABEL = "v57_oos_label_r35";
    public static final String TABLE_R35_RUNTIME_HEALTH = "v57_runtime_health_r35";
    public static final String TABLE_R35_DEFERRED_EVIDENCE_JOB = "v57_deferred_evidence_job";
    public static final String TABLE_R36_ANALYSIS_REQUEST = "v57_analysis_request_r36";
    public static final String TABLE_R36_SETTLEMENT_TASK = "v57_settlement_task_r36";
    public static final String TABLE_R811_DRAW_PROVENANCE = "v57_draw_provenance_r811";
    public static final String TABLE_R811_DRAW_CANONICAL = "v57_draw_canonical_r811";
    public static final String TABLE_R815_CRITICAL_ERROR = "v57_critical_error_r815";
    public static final String TABLE_R815_LONG_TEXT = "v57_long_text_r815";
    public static final String TABLE_R815_RECOVERY_GENERATION = "v57_recovery_generation_r815";
    public static final String TABLE_R815_SHADOW_SIDECAR = "v57_shadow_sidecar_r815";
    public static final String TABLE_R815_RESEARCH_CANDIDATE = "v57_research_candidate_r815";
    public static final String TABLE_R815_RESEARCH_EVAL = "v57_research_eval_r815";
    public static final String TABLE_R815_RESEARCH_ARTIFACT = "v57_research_artifact_r815";
    public static final String TABLE_R815_RESEARCH_QUEUE = "v57_research_queue_r815";
    public static final String TABLE_R906_ISSUE_CLOSURE = "v57_issue_closure_r906";
    public static final String TABLE_R906_SETTLEMENT_OUTBOX = "v57_settlement_outbox_r906";
    public static final String TABLE_R906_SELECTED_EVIDENCE = "v57_selected_evidence_r906";
    public static final String TABLE_R906_RESEARCH_ASSET = "v57_research_asset_r906";
    public static final String TABLE_R906_RESEARCH_EVAL_COVERAGE = "v57_research_eval_coverage_r906";
    public static final String TABLE_R906_RESEARCH_COMPACT_LABEL = "v57_research_compact_label_r906";
    public static final String TABLE_R906_RESEARCH_LEARNER_UPDATE = "v57_research_learner_update_r906";
    public static final String TABLE_R906_RESEARCH_ARTIFACT_JOB = "v57_research_artifact_job_r906";
    public static final String TABLE_R906_RESEARCH_CERTIFICATE = "v57_research_certificate_r906";
    public static final String TABLE_R906_RUNTIME_TRUTH = "v57_runtime_truth_snapshot_r906";
    public static final String TABLE_R908_ALGORITHM_GENOME = "v57_algorithm_genome_r908";
    public static final String TABLE_R908_ALGORITHM_PREDRAW = "v57_algorithm_predraw_r908";
    public static final String TABLE_R817_RESEARCH_PREDRAW = "v57_research_predraw_r817";
    public static final String TABLE_SHADOW = "shadow_candidates";
    public static final String TABLE_SHAPE_VECTOR = "shape_vector";
    public static final String TABLE_SHAPE_MEMORY = "shape_memory";
    public static final String TABLE_COMBO_MEMORY = "combo_memory";
    public static final String TABLE_BREAK_MEMORY = "break_memory";
    public static final String TABLE_CALIBRATION = "calibration_bucket";
    public static final String TABLE_MODEL_WEIGHT = "model_weight";
    public static final String TABLE_BACKTEST = "backtest_report";
    public static final String TABLE_APP_STATE = "app_state";

    public static final String TABLE_DEEP_PROFILE = "deep_profile";
    public static final String TABLE_DEEP_ATTRIBUTION = "deep_attribution";
    public static final String TABLE_DEEP_TRANSITION = "deep_transition";
    public static final String TABLE_DEEP_CYCLE = "deep_cycle";
    public static final String TABLE_MANUAL_BACKTEST_TASK = "manual_backtest_task";
    public static final String TABLE_POINT_IN_TIME_SNAPSHOT = "point_in_time_snapshot";
    public static final String TABLE_PROMOTION_CANDIDATE = "promotion_candidate";
    public static final String TABLE_PROBABILITY_AUDIT_BUCKET = "probability_audit_bucket";
    public static final String TABLE_BIAS_EVIDENCE = "bias_evidence";
    public static final String TABLE_LEARNING_SNAPSHOT = "learning_snapshot";
    public static final String TABLE_CANDIDATE_EVIDENCE = "candidate_evidence";
    public static final String TABLE_AI_JUDGE_LOG = "ai_judge_log";
    public static final String TABLE_SELF_DIAGNOSIS_LOG = "self_diagnosis_log";

    public static final String TABLE_TEACHER_SNAPSHOT = "teacher_snapshot";
    public static final String TABLE_REALTIME_VALIDATE = "realtime_validate";
    public static final String TABLE_RUNTIME_PROFILE_CACHE = "runtime_profile_cache";
    public static final String TABLE_STATE_NUMBER_PROFILE = "state_number_profile";
    public static final String TABLE_LEARNING_CASE_LOG = "learning_case_log";
    public static final String TABLE_AUTO_LEARN_CURSOR = "auto_learn_cursor";

    public static final String TABLE_TEACHER_MATRIX_SAMPLE = "teacher_matrix_sample";
    public static final String TABLE_TEACHER_MATRIX_PROFILE = "teacher_matrix_profile";
    public static final String TABLE_TEACHER_MATRIX_STATE_PROFILE = "teacher_matrix_state_profile";
    public static final String TABLE_TEACHER_MATRIX_COMBO_PROFILE = "teacher_matrix_combo_profile";
    public static final String TABLE_TEACHER_MATRIX_REPORT = "teacher_matrix_report";

    public static final String TABLE_PRE_DRAW_REASON_SAMPLE = "pre_draw_reason_sample";
    public static final String TABLE_REASON_CONTRAST_PROFILE = "reason_contrast_profile";
    public static final String TABLE_REASON_CAUSAL_PROFILE = "reason_causal_profile";
    public static final String TABLE_FALSE_STRONG_PROFILE = "false_strong_profile";
    public static final String TABLE_COMBO_ROLE_PROFILE = "combo_role_profile";
    public static final String TABLE_ELITE45_AUDIT_LOG = "elite45_audit_log";

    public static final String TABLE_TEACHER_REPLAY_CASE = "teacher_replay_case";
    public static final String TABLE_TEACHER_REPLAY_CURSOR = "teacher_replay_cursor";
    public static final String TABLE_TEACHER_REPLAY_REPORT = "teacher_replay_report";
    public static final String TABLE_TEACHER_REPLAY_COUNTERFACTUAL =
            "teacher_replay_counterfactual";
    public static final String TABLE_TEACHER_REPLAY_STABILITY = "teacher_replay_stability";
    public static final String TABLE_TEACHER_ASSIST_PROFILE = "teacher_assist_profile";
    public static final String TABLE_UI_TEXT_STATE = "ui_text_state";
    public static final String TABLE_DIAG_LOG = "diagnostic_log";

    public static final String TABLE_PRE_DRAW_FEATURE = "pre_draw_feature";
    public static final String TABLE_PRE_DRAW_NUMBER_SAMPLE = "pre_draw_number_sample";
    public static final String TABLE_FULL_CANDIDATE_SCAN = "full_candidate_scan";
    public static final String TABLE_CANDIDATE_AUDIT = "candidate_audit";
    public static final String TABLE_CAUSAL_FACTOR_MATRIX = "causal_factor_matrix";
    public static final String TABLE_POSITION_CAUSE_MATRIX = "position_cause_matrix";
    public static final String TABLE_TEACHER_NUMBER_MATRIX = "teacher_number_matrix";
    public static final String TABLE_COUNTERFACTUAL_RANK = "counterfactual_rank";
    public static final String TABLE_THIRD_GUARD_MATRIX = "third_guard_matrix";
    public static final String TABLE_RULE_HEALTH_MATRIX = "rule_health_matrix";
    public static final String TABLE_SOURCE_HEALTH_MATRIX = "source_health_matrix";
    public static final String TABLE_STATE_HEALTH_MATRIX = "state_health_matrix";
    public static final String TABLE_SHADOW_HEALTH_GATE = "shadow_health_gate";
    public static final String TABLE_FALSE_STRONG_FREEZE = "false_strong_freeze";
    public static final String TABLE_SEED_CAUSE_EXPANSION = "seed_cause_expansion";
    public static final String TABLE_DYNAMIC_CAUSE_CHAIN = "dynamic_cause_chain";
    public static final String TABLE_DECISION_AUDIT = "decision_audit";
    public static final String TABLE_REAL_TRADE_LEDGER = "real_trade_ledger";
    public static final String TABLE_V20_WINDOW_STATE = "v20_window_state";
    public static final String TABLE_V20_CAUSE_MATRIX = "v20_cause_matrix";
    public static final String TABLE_V20_TEACHER_NUMBER = "v20_teacher_number";
    public static final String TABLE_V20_SIMILAR_WINDOW = "v20_similar_window";
    public static final String TABLE_V20_REVERSE_REASON_SAMPLE = "v20_reverse_reason_sample";
    public static final String TABLE_V20_RULE_HEALTH = "v20_rule_health";
    public static final String TABLE_V20_SHADOW_HEALTH = "v20_shadow_health";
    public static final String TABLE_V20_FALSE_STRONG = "v20_false_strong";
    public static final String TABLE_V20_ACTION_MATRIX = "v20_action_matrix";
    public static final String TABLE_V20_CLOSED_LOOP = "v20_closed_loop_snapshot";

    public static final String TABLE_DYNAMIC_WINDOW_CLUSTER =
            AppConfig.TABLE_DYNAMIC_WINDOW_CLUSTER;
    public static final String TABLE_WINDOW_FEATURE_SAMPLE = AppConfig.TABLE_WINDOW_FEATURE_SAMPLE;
    public static final String TABLE_POSITION_NEED_SAMPLE = AppConfig.TABLE_POSITION_NEED_SAMPLE;
    public static final String TABLE_NUMBER_SUPPLY_SAMPLE = AppConfig.TABLE_NUMBER_SUPPLY_SAMPLE;
    public static final String TABLE_NEED_SUPPLY_MATCH_SAMPLE =
            AppConfig.TABLE_NEED_SUPPLY_MATCH_SAMPLE;
    public static final String TABLE_COUNTERFACTUAL_REASON_SAMPLE =
            AppConfig.TABLE_COUNTERFACTUAL_REASON_SAMPLE;
    public static final String TABLE_CAUSE_ABLATION_SAMPLE = AppConfig.TABLE_CAUSE_ABLATION_SAMPLE;
    public static final String TABLE_MISS_REASON_SAMPLE = AppConfig.TABLE_MISS_REASON_SAMPLE;
    public static final String TABLE_ADAPTIVE_JUDGE_SAMPLE = AppConfig.TABLE_ADAPTIVE_JUDGE_SAMPLE;
    public static final String TABLE_PROMOTION_DOMAIN_SNAPSHOT =
            AppConfig.TABLE_PROMOTION_DOMAIN_SNAPSHOT;
    public static final String TABLE_DYNAMIC_CAUSAL_STATE = AppConfig.TABLE_DYNAMIC_CAUSAL_STATE;
    public static final String DBHELPER_DYNAMIC_CAUSAL_WINDOW_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";
    public static final String DBHELPER_MAIN_CHAIN_CLOSED_LOOP_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";
    public static final String TABLE_MAIN_CHAIN_CANDIDATE_SNAPSHOT =
            "main_chain_candidate_snapshot";
    public static final String TABLE_MAIN_CHAIN_REJECT_BUCKET = "main_chain_reject_bucket";
    public static final String TABLE_V20_FREQUENCY_STATE = AppConfig.TABLE_V20_FREQUENCY_STATE;
    public static final String TABLE_V20_MODULE_REGISTRY = AppConfig.TABLE_V20_MODULE_REGISTRY;
    public static final String TABLE_V20_LAYER_SNAPSHOT = AppConfig.TABLE_V20_LAYER_SNAPSHOT;
    public static final String TABLE_V20_BACKGROUND_CURSOR = AppConfig.TABLE_V20_BACKGROUND_CURSOR;
    public static final String TABLE_V20_SHADOW_DOMAIN_STAT =
            AppConfig.TABLE_V20_SHADOW_DOMAIN_STAT;
    public static final String TABLE_V20_LINK_CANDIDATE_SNAPSHOT =
            AppConfig.TABLE_V20_LINK_CANDIDATE_SNAPSHOT;
    public static final String TABLE_TEACHER_DOMAIN_STAT_V20 = "teacher_domain_stat_v20";
    public static final String TABLE_DYNAMIC_DOMAIN_STAT_V20 = "dynamic_domain_stat_v20";
    public static final String TABLE_PROMOTION_DOMAIN_STAT_V20 = "promotion_domain_stat_v20";
    public static final String TABLE_FORWARD_VALIDATION_STAT_V20 = "forward_validation_stat_v20";
    public static final String TABLE_SERVICE_RECOVERY_EVENT_V20 = "service_recovery_event_v20";
    public static final String DBHELPER_LINK_MODIFICATION_V2_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";

    private boolean openedChecked;

    /** Valid only inside this helper's current legacy-schema transaction. Never retained
     * across commit, rollback, process restart, or another database/DDL owner. */
    private SchemaColumnsR94 schemaColumnsR94;
    private static final class SchemaColumnsR94 {
        final SQLiteDatabase database;
        final Thread owner = Thread.currentThread();
        final java.util.HashMap<String, java.util.HashSet<String>> tables =
                new java.util.HashMap<String, java.util.HashSet<String>>();
        SchemaColumnsR94(SQLiteDatabase database) { this.database = database; }
        boolean owns(SQLiteDatabase db) {
            return database == db && owner == Thread.currentThread() && db.inTransaction();
        }
    }

    public DbHelper(Context context) {
        super(context, AppConfig.DB_NAME, null, AppConfig.DB_VERSION);
        openedChecked = false;
        try {
            if (android.os.Build.VERSION.SDK_INT >= 16) setWriteAheadLoggingEnabled(true);
        } catch (Throwable ignore) {
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        ensureSchema(db);
        V49Repository.ensure(db);
        requireV50Migration(db);
        V51Repository.ensure(db);
        ensureTianshuB03(db);
        ensureR35Schema(db);
        ensureR36Schema(db);
        ensureR815CriticalSchema(db);
        ensureR906CriticalSchema(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        DatabaseUpgradePlanR363.Plan plan =
                DatabaseUpgradePlanR363.select(oldVersion, newVersion);
        if (plan.path == DatabaseUpgradePlanR363.Path.R35_TO_R36_ADDITIVE_ONLY) {
            /* Exact DB35 -> DB36 closure: no historical V50 migration/full integrity scan, no
             * table drop, no row rewrite.  Only the idempotent owners required by R36 run. */
            ensureR35Schema(db);
            ensureR36Schema(db);
            ensureR815CriticalSchema(db);
        ensureR906CriticalSchema(db);
            V51Repository.ensure(db);
            ensureTianshuB03(db);
            return;
        }
        /* DB34 is the locked V57.15.1 baseline.  Its historical ensureSchema()
         * path contains legacy DROP/replace migrations, so the R3.5 upgrade is
         * deliberately additive and must never route through that path. */
        if (oldVersion >= 34 && newVersion >= 35) {
            V49Repository.ensure(db);
            requireV50Migration(db);
            V51Repository.ensure(db);
            ensureTianshuB03(db);
            ensureR35Schema(db);
            if (newVersion >= 36) {
                ensureR36Schema(db);
                ensureR815CriticalSchema(db);
        ensureR906CriticalSchema(db);
            }
            return;
        }
        ensureSchema(db);
        V49Repository.ensure(db);
        requireV50Migration(db);
        V51Repository.ensure(db);
        ensureTianshuB03(db);
        if (oldVersion < 34) migrateB03SelectedEventForR31(db);
        ensureTianshuB03(db);
        if (oldVersion < 26) resetV40DistributionLearningTables(db);
        ensureV28OosCandidateIndexes(db);
        ensureR35Schema(db);
        if (newVersion >= 36) {
            ensureR36Schema(db);
            ensureR815CriticalSchema(db);
        ensureR906CriticalSchema(db);
        }
    }

    private static void requireV50Migration(SQLiteDatabase db) {
        DatabaseMigrationEngine.MigrationReport report = DatabaseMigrationEngine.migrate(db);
        if (report == null || !report.success) {
            throw new IllegalStateException(
                    report == null
                            ? "V50_MIGRATION_REPORT_NULL"
                            : "V50_MIGRATION_FAILED:" + report.detail);
        }
        DatabaseIntegrityVerifier.IntegrityReport integrity = DatabaseIntegrityVerifier.verify(db);
        if (integrity == null || !integrity.valid) {
            throw new IllegalStateException(
                    integrity == null
                            ? "V50_INTEGRITY_REPORT_NULL"
                            : "V50_INTEGRITY_FAILED:" + integrity.detail);
        }
    }

    /**
     * V40 changes the probability contract from rule/rank fusion to a pre-draw ten-number
     * distribution followed by residual learning and OOS calibration. Old
     * posterior/calibration/authorization rows are therefore incompatible and must never be reused
     * after an upgrade. Ledger settlement rows are preserved.
     */
    private void resetV40DistributionLearningTables(SQLiteDatabase db) {
        if (db == null || !db.inTransaction())
            throw new SQLiteException("CP5_SCHEMA_MIGRATION_TRANSACTION_REQUIRED");
        ContentValues state = new ContentValues();
        state.put("state_key", "v40_distribution_schema_reset_version");
        state.put("state_value", "CP5_LEGACY_EVIDENCE_PRESERVED_READ_ONLY");
        db.insertWithOnConflict(TABLE_APP_STATE, null, state, SQLiteDatabase.CONFLICT_REPLACE);
        // Old authorization/settlement facts are not rewritten. Current consumers require
        // the current build/policy/frozen identity and independently reject old evidence.
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new SQLiteException(
                "DOWNGRADE_REQUIRES_VERIFIED_BACKUP_RESTORE:" + oldVersion + "->" + newVersion);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        if (db == null) return;
        // R13-P32: auto_vacuum set here never converts an existing NONE database (needs a VACUUM);
        // DatabaseCompactionSU2 performs that one-time conversion. Keep WAL and journal bounded.
        try { db.execSQL("PRAGMA journal_size_limit=8388608"); try(android.database.Cursor c=db.rawQuery("PRAGMA wal_autocheckpoint=1000",null)){c.moveToFirst();} }
        catch (Exception ignored) { /* Reclaim reports actual page/free-list state. */ }
        try {
            db.setForeignKeyConstraintsEnabled(true);
        } catch (Throwable ignore) {
        }
        try {
            db.execSQL("PRAGMA synchronous=NORMAL");
        } catch (Throwable ignore) {
        }
        try {
            db.execSQL("PRAGMA temp_store=MEMORY");
        } catch (Throwable ignore) {
        }
        configureBusyTimeoutR77(db);
    }

    /** Configure the pool, not whichever read connection happened to answer a PRAGMA.
     * Android's native default is longer than our cooperative SQL slice. In WAL mode a
     * rawQuery alone neither configures the primary writer nor future pool connections.
     * API 33+ retains this statement for every connection opened by that pool;
     * opening a new database pool reruns onConfigure and installs it again.
     * API 30-32 exposes the hook but its native execute rejects row-returning PRAGMAs.
     * On these and older APIs, explicitly pin/configure the primary writer and
     * retain the reader configuration without claiming coverage of future read connections.
     */
    static void configureBusyTimeoutR77(SQLiteDatabase db) {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            db.execPerConnectionSQL("PRAGMA busy_timeout=" + AppConfig.DB_BUSY_TIMEOUT_MS, null);
            verifyBusyTimeoutR77(db);
            return;
        }
        configurePrimaryBusyTimeoutR77(db);
    }

    static void configurePrimaryBusyTimeoutR77(SQLiteDatabase db) {
        // beginTransaction pins this thread to the primary connection. This runs only
        // during onConfigure, before application transactions or workers are admitted.
        db.beginTransactionNonExclusive();
        try {
            setAndVerifyBusyTimeoutR77(db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        setAndVerifyBusyTimeoutR77(db);
    }

    private static void setAndVerifyBusyTimeoutR77(SQLiteDatabase db) {
        try (Cursor busy = db.rawQuery(
                "PRAGMA busy_timeout=" + AppConfig.DB_BUSY_TIMEOUT_MS, null)) {
            if (busy == null || !busy.moveToFirst()
                    || busy.getLong(0) != AppConfig.DB_BUSY_TIMEOUT_MS)
                throw new SQLiteException("R77_SQLITE_BUSY_TIMEOUT_CONFIG_FAILED");
        }
        verifyBusyTimeoutR77(db);
    }

    private static void verifyBusyTimeoutR77(SQLiteDatabase db) {
        try (Cursor busy = db.rawQuery("PRAGMA busy_timeout", null)) {
            if (busy == null || !busy.moveToFirst()
                    || busy.getLong(0) != AppConfig.DB_BUSY_TIMEOUT_MS)
                throw new SQLiteException("R77_SQLITE_BUSY_TIMEOUT_VERIFY_FAILED");
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        /* Schema只允许在onCreate/onUpgrade/onDowngrade执行；禁止每次打开数据库全表扫描和账本修复。 */
        openedChecked = true;
    }

    private void ensureSchema(SQLiteDatabase db) {
        if (db == null) {
            return;
        }

        db.beginTransaction();
        final SchemaColumnsR94 previousColumnsR94 = schemaColumnsR94;
        try {
            schemaColumnsR94 = new SchemaColumnsR94(db);
            createBaseTables(db);
            migrateColumns(db);
            ensureLinkModificationV2Schema(db);
            repairLedgerRows(db);
            createIndexes(db);
            insertDefaultRows(db);
            ensureTianshuB03(db);
            db.setTransactionSuccessful();
        } catch (Throwable e) {
            // A failed DDL/transaction must never leave an optimistic column snapshot for
            // the existing recovery pass. Re-read the actual database on that path.
            schemaColumnsR94 = null;
            try {
                createBaseTables(db);
                migrateColumns(db);
                ensureLinkModificationV2Schema(db);
                createIndexes(db);
                insertDefaultRows(db);
            } catch (Throwable ignore) {
            }
        } finally {
            if (previousColumnsR94 != null) previousColumnsR94.tables.clear();
            schemaColumnsR94 = previousColumnsR94;
            try {
                db.endTransaction();
            } catch (Throwable ignore) {
            }
        }
    }

    private void createBaseTables(SQLiteDatabase db) {

        createBase(
                db,
                TABLE_OOS_ACTION_LOG,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, contract_decision_id TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_HIERARCHICAL_POSTERIOR,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, family_level TEXT NOT NULL, family_key TEXT"
                    + " NOT NULL, UNIQUE(family_level,family_key)");
        createBase(
                db,
                TABLE_MONOTONIC_CALIBRATION,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, calibration_version TEXT NOT NULL,"
                    + " bucket_no INTEGER NOT NULL, UNIQUE(calibration_version,bucket_no)");
        createBase(
                db,
                TABLE_DECISION_AUTHORIZATION,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, contract_decision_id TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_V48_DIAGNOSTIC_OBSERVATION,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, target_issue TEXT NOT NULL UNIQUE");
        createBase(db, TABLE_V48_MODEL_ARTIFACT, "model_id TEXT PRIMARY KEY");

        createBase(
                db,
                TABLE_DRAWS,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, issue TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_DECISIONS,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, target_issue TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_TRADES,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, target_issue TEXT NOT NULL UNIQUE");
        createBase(db, TABLE_SHADOW, "_id INTEGER PRIMARY KEY AUTOINCREMENT");

        createBase(db, TABLE_SHAPE_VECTOR, "_id INTEGER PRIMARY KEY AUTOINCREMENT");
        createBase(
                db,
                TABLE_SHAPE_MEMORY,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, shape_key TEXT NOT NULL, position INTEGER"
                    + " DEFAULT 0, UNIQUE(shape_key, position)");
        createBase(
                db,
                TABLE_COMBO_MEMORY,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, shape_key TEXT NOT NULL, position INTEGER"
                    + " DEFAULT 0, k3_mask INTEGER DEFAULT 0, UNIQUE(shape_key, position,"
                    + " k3_mask)");
        createBase(
                db,
                TABLE_BREAK_MEMORY,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, break_key TEXT NOT NULL, position INTEGER"
                    + " DEFAULT 0, break_depth_bucket INTEGER DEFAULT 0, drift_bucket INTEGER"
                    + " DEFAULT 0, markov_bucket INTEGER DEFAULT 0, UNIQUE(break_key, position,"
                    + " break_depth_bucket, drift_bucket, markov_bucket)");
        createBase(
                db,
                TABLE_CALIBRATION,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, bucket_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_MODEL_WEIGHT,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, weight_key TEXT NOT NULL UNIQUE");
        createBase(db, TABLE_BACKTEST, "_id INTEGER PRIMARY KEY AUTOINCREMENT");
        createBase(db, TABLE_APP_STATE, "state_key TEXT PRIMARY KEY");

        createBase(
                db,
                TABLE_DEEP_PROFILE,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, profile_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_DEEP_ATTRIBUTION,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, attribution_id TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_DEEP_TRANSITION,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, transition_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_DEEP_CYCLE,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, cycle_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_MANUAL_BACKTEST_TASK,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, task_id TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_POINT_IN_TIME_SNAPSHOT,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, snapshot_id TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_PROMOTION_CANDIDATE,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, promotion_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_PROBABILITY_AUDIT_BUCKET,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, bucket_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_BIAS_EVIDENCE,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, evidence_id TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_LEARNING_SNAPSHOT,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, snapshot_id TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_CANDIDATE_EVIDENCE,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, evidence_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_AI_JUDGE_LOG,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, judge_id TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_SELF_DIAGNOSIS_LOG,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, diagnosis_id TEXT NOT NULL UNIQUE");

        createBase(
                db,
                TABLE_TEACHER_SNAPSHOT,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, snapshot_id TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_REALTIME_VALIDATE,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, report_id TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_RUNTIME_PROFILE_CACHE,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, profile_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_STATE_NUMBER_PROFILE,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, profile_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_LEARNING_CASE_LOG,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, case_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_AUTO_LEARN_CURSOR,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, task_key TEXT NOT NULL UNIQUE");

        createBase(
                db,
                TABLE_TEACHER_MATRIX_SAMPLE,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, sample_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_TEACHER_MATRIX_PROFILE,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, profile_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_TEACHER_MATRIX_STATE_PROFILE,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, state_profile_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_TEACHER_MATRIX_COMBO_PROFILE,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, combo_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_TEACHER_MATRIX_REPORT,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, report_id TEXT NOT NULL UNIQUE");

        createBase(
                db,
                TABLE_PRE_DRAW_REASON_SAMPLE,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, sample_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_REASON_CONTRAST_PROFILE,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, profile_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_REASON_CAUSAL_PROFILE,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, causal_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_FALSE_STRONG_PROFILE,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, false_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_COMBO_ROLE_PROFILE,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, combo_role_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_ELITE45_AUDIT_LOG,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, audit_key TEXT NOT NULL UNIQUE");

        createBase(
                db,
                TABLE_TEACHER_REPLAY_CASE,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, case_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_TEACHER_REPLAY_CURSOR,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, cursor_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_TEACHER_REPLAY_REPORT,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, report_id TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_TEACHER_REPLAY_COUNTERFACTUAL,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, cf_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_TEACHER_REPLAY_STABILITY,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, stability_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_TEACHER_ASSIST_PROFILE,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, assist_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_UI_TEXT_STATE,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, text_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_DIAG_LOG,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, log_key TEXT NOT NULL UNIQUE");

        createBase(
                db,
                TABLE_PRE_DRAW_FEATURE,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, feature_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_PRE_DRAW_NUMBER_SAMPLE,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, sample_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_FULL_CANDIDATE_SCAN,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, candidate_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_CANDIDATE_AUDIT,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, audit_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_CAUSAL_FACTOR_MATRIX,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, causal_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_POSITION_CAUSE_MATRIX,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, position_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_TEACHER_NUMBER_MATRIX,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, teacher_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_COUNTERFACTUAL_RANK,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, rank_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_THIRD_GUARD_MATRIX,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, guard_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_RULE_HEALTH_MATRIX,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, rule_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_SOURCE_HEALTH_MATRIX,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, source_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_STATE_HEALTH_MATRIX,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, state_health_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_SHADOW_HEALTH_GATE,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, shadow_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_FALSE_STRONG_FREEZE,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, freeze_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_SEED_CAUSE_EXPANSION,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, seed_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_DYNAMIC_CAUSE_CHAIN,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, chain_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_DECISION_AUDIT,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, decision_audit_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_REAL_TRADE_LEDGER,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, ledger_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_V20_WINDOW_STATE,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, state_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_V20_CAUSE_MATRIX,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, cause_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_V20_TEACHER_NUMBER,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, teacher_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_V20_SIMILAR_WINDOW,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, similar_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_V20_REVERSE_REASON_SAMPLE,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, reverse_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_V20_RULE_HEALTH,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, rule_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_V20_SHADOW_HEALTH,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, shadow_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_V20_FALSE_STRONG,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, false_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_V20_ACTION_MATRIX,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, action_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_V20_CLOSED_LOOP,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, snapshot_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_MAIN_CHAIN_CANDIDATE_SNAPSHOT,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, snapshot_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_MAIN_CHAIN_REJECT_BUCKET,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, bucket_key TEXT NOT NULL UNIQUE");

        createBase(
                db,
                TABLE_DYNAMIC_WINDOW_CLUSTER,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, cluster_id TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_WINDOW_FEATURE_SAMPLE,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, sample_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_POSITION_NEED_SAMPLE,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, sample_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_NUMBER_SUPPLY_SAMPLE,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, sample_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_NEED_SUPPLY_MATCH_SAMPLE,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, match_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_COUNTERFACTUAL_REASON_SAMPLE,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, reason_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_CAUSE_ABLATION_SAMPLE,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, ablation_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_MISS_REASON_SAMPLE,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, miss_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_ADAPTIVE_JUDGE_SAMPLE,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, judge_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_PROMOTION_DOMAIN_SNAPSHOT,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, domain_key TEXT NOT NULL UNIQUE");
        createBase(db, TABLE_DYNAMIC_CAUSAL_STATE, "state_key TEXT PRIMARY KEY");
        createBase(db, TABLE_V20_FREQUENCY_STATE, "state_key TEXT PRIMARY KEY");
        createBase(db, TABLE_V20_MODULE_REGISTRY, "module_name TEXT PRIMARY KEY");
        createBase(
                db,
                TABLE_V20_LAYER_SNAPSHOT,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, snapshot_key TEXT NOT NULL UNIQUE");
        createBase(db, TABLE_V20_BACKGROUND_CURSOR, "task_key TEXT PRIMARY KEY");
        createBase(
                db,
                TABLE_V20_SHADOW_DOMAIN_STAT,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, domain_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_V20_LINK_CANDIDATE_SNAPSHOT,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, snapshot_key TEXT NOT NULL UNIQUE");
        createBase(db, TABLE_TEACHER_DOMAIN_STAT_V20, "domain_key TEXT PRIMARY KEY");
        createBase(db, TABLE_DYNAMIC_DOMAIN_STAT_V20, "domain_key TEXT PRIMARY KEY");
        createBase(db, TABLE_PROMOTION_DOMAIN_STAT_V20, "domain_key TEXT PRIMARY KEY");
        createBase(db, TABLE_FORWARD_VALIDATION_STAT_V20, "domain_key TEXT PRIMARY KEY");
        createBase(db, TABLE_SERVICE_RECOVERY_EVENT_V20, "_id INTEGER PRIMARY KEY AUTOINCREMENT");
        ensureDomainEvidenceColumns(db, TABLE_TEACHER_DOMAIN_STAT_V20);
        ensureDomainEvidenceColumns(db, TABLE_DYNAMIC_DOMAIN_STAT_V20);
        ensureDomainEvidenceColumns(db, TABLE_PROMOTION_DOMAIN_STAT_V20);
        ensureDomainEvidenceColumns(db, TABLE_FORWARD_VALIDATION_STAT_V20);
        addColumn(db, TABLE_SERVICE_RECOVERY_EVENT_V20, "event_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_SERVICE_RECOVERY_EVENT_V20, "heartbeat_age_ms INTEGER DEFAULT 0");
        addColumn(db, TABLE_SERVICE_RECOVERY_EVENT_V20, "source TEXT");
        addColumn(db, TABLE_SERVICE_RECOVERY_EVENT_V20, "stage TEXT");
        addColumn(db, TABLE_SERVICE_RECOVERY_EVENT_V20, "result TEXT");
        addColumn(db, TABLE_SERVICE_RECOVERY_EVENT_V20, "detail TEXT");
        addColumn(db, TABLE_SERVICE_RECOVERY_EVENT_V20, "version TEXT");
    }

    private void createBase(SQLiteDatabase db, String table, String body) {
        execQuiet(db, "CREATE TABLE IF NOT EXISTS " + table + " (" + body + ")");
    }

    private void migrateColumns(SQLiteDatabase db) {
        migrateDrawColumns(db);
        migrateDecisionColumns(db);
        migrateTradeColumns(db);
        migrateShadowColumns(db);
        migrateShapeVectorColumns(db);
        migrateMemoryColumns(db);
        migrateCalibrationColumns(db);
        migrateModelWeightColumns(db);
        migrateBacktestColumns(db);
        migrateAppStateColumns(db);
        migrateDeepColumns(db);
        migrateRuntimeLearningColumns(db);
        migrateTeacherMatrixColumns(db);
        migrateReasonCausalColumns(db);
        migrateTeacherReplayAssistColumns(db);
        migrateV20Columns(db);
        migrateDynamicCausalWindowColumns(db);
        migrateMainChainClosedLoopColumns(db);
        migrateV24OosContractColumns(db);
        migrateV48Columns(db);
    }

    private void migrateV24OosContractColumns(SQLiteDatabase db) {
        String[] common =
                new String[] {
                    "raw_rank_score REAL DEFAULT 0",
                    "calibrated_hit_probability REAL DEFAULT 0",
                    "contract_decision_id TEXT",
                    "candidate_hash TEXT",
                    "snapshot_max_issue TEXT",
                    "trained_until_issue TEXT",
                    "build_id TEXT",
                    "cohort_id TEXT",
                    "schema_version TEXT",
                    "feature_schema_version TEXT",
                    "ranking_version TEXT",
                    "calibration_version TEXT",
                    "gate_policy_version TEXT",
                    "pipeline_version TEXT",
                    "contract_valid INTEGER DEFAULT 0",
                    "contract_status TEXT",
                    "coarse_family_key TEXT",
                    "mid_family_key TEXT",
                    "fine_family_key TEXT",
                    "position_family_key TEXT",
                    "posterior_matched_level TEXT",
                    "posterior_family_key TEXT",
                    "posterior_policy TEXT",
                    "posterior_version TEXT",
                    "posterior_sample INTEGER DEFAULT 0",
                    "posterior_hit INTEGER DEFAULT 0",
                    "posterior_mean REAL DEFAULT 0",
                    "posterior_wilson_lower REAL DEFAULT 0",
                    "posterior_recent_rate REAL DEFAULT 0",
                    "posterior_drift REAL DEFAULT 0",
                    "family_wilson_lower REAL DEFAULT 0",
                    "calibration_bucket INTEGER DEFAULT 0",
                    "calibration_sample INTEGER DEFAULT 0",
                    "calibration_total_sample INTEGER DEFAULT 0",
                    "calibration_ready INTEGER DEFAULT 0",
                    "calibration_status TEXT",
                    "calibration_brier REAL DEFAULT 0",
                    "authorization_id TEXT",
                    "authorization_valid INTEGER DEFAULT 0",
                    "authorized_mode TEXT",
                    "authorization_reject_code TEXT",
                    "authorized_at INTEGER DEFAULT 0",
                    "authorization_probability REAL DEFAULT 0",
                    "authorization_gate REAL DEFAULT 0",
                    "authorization_edge REAL DEFAULT 0",
                    "teacher_policy TEXT",
                    "dynamic_policy TEXT",
                    "adaptive_policy TEXT",
                    "micro_walk_forward_status TEXT",
                    "micro_walk_forward_version TEXT",
                    "micro_walk_forward_as_of_issue TEXT",
                    "micro_walk_forward_sample INTEGER DEFAULT 0",
                    "honest_probability REAL DEFAULT 0",
                    "action_utility REAL DEFAULT 0",
                    "action_structure_score REAL DEFAULT 0",
                    "action_discrimination_score REAL DEFAULT 0",
                    "action_maturity_score REAL DEFAULT 0",
                    "action_uncertainty_penalty REAL DEFAULT 0",
                    "action_frequency_support REAL DEFAULT 0",
                    "action_utility_version TEXT",
                    "lane_candidate TEXT",
                    "oos_rank_slot INTEGER DEFAULT 0",
                    "oos_candidate_count INTEGER DEFAULT 0",
                    "v42_accept_score REAL DEFAULT 0",
                    "v42_family_lcb REAL DEFAULT 0",
                    "v42_regime_quality REAL DEFAULT 0",
                    "v42_association_score REAL DEFAULT 0",
                    "v42_distillation_risk REAL DEFAULT 0",
                    "v42_support_votes INTEGER DEFAULT 0",
                    "v42_regime_family TEXT",
                    "v42_risk_coverage_mode TEXT",
                    "v42_primary_reject TEXT",
                    "v42_secondary_reject TEXT",
                    "v42_tertiary_reject TEXT",
                    "v42_lane_policy_version TEXT",
                    "v43_strategy_support REAL DEFAULT 0",
                    "v43_strategy_risk REAL DEFAULT 0",
                    "v43_strategy_confidence REAL DEFAULT 0",
                    "v43_strategy_diversity REAL DEFAULT 0",
                    "v43_strategy_maturity REAL DEFAULT 0",
                    "v43_safe_probability REAL DEFAULT 0",
                    "v43_probability_residual REAL DEFAULT 0",
                    "v43_ablation_fragility REAL DEFAULT 0",
                    "v43_probability_lock REAL DEFAULT 0",
                    "v43_evidence_group_count INTEGER DEFAULT 0",
                    "v43_hard_risk INTEGER DEFAULT 0",
                    "v43_dominant_evidence TEXT",
                    "v43_missing_evidence TEXT",
                    "v43_evidence_fingerprint TEXT",
                    "v43_probability_lock_hash TEXT",
                    "v43_pipeline_stage TEXT",
                    "v43_invariant_status TEXT",
                    "v43_strategy_version TEXT",
                    "v46_event_probability REAL DEFAULT 0",
                    "v46_action_lcb REAL DEFAULT 0",
                    "v46_probability_uncertainty REAL DEFAULT 0",
                    "v46_expected_value REAL DEFAULT 0",
                    "v46_global_calibrated_probability REAL DEFAULT 0",
                    "v46_position_adjustment REAL DEFAULT 0",
                    "v46_rank_adjustment REAL DEFAULT 0",
                    "v46_regime_adjustment REAL DEFAULT 0",
                    "v46_calibration_confidence REAL DEFAULT 0",
                    "v46_final_rank INTEGER DEFAULT 0",
                    "v46_formal_candidate_count INTEGER DEFAULT 0",
                    "v46_version TEXT",
                    "v46_number_distribution_summary TEXT",
                    "v46_probability_trace TEXT",
                    "v46_root_cause_code TEXT",
                    "v46_root_cause_owner TEXT",
                    "v46_final_contract_hash TEXT",
                    "v46_lane TEXT"
                };
        for (int i = 0; i < common.length; i++) {
            addColumn(db, TABLE_DECISIONS, common[i]);
            addColumn(db, TABLE_TRADES, common[i]);
        }

        String[] oos =
                new String[] {
                    "candidate_hash TEXT",
                    "base_issue TEXT",
                    "target_issue TEXT",
                    "position INTEGER DEFAULT 0",
                    "n1 INTEGER DEFAULT 0",
                    "n2 INTEGER DEFAULT 0",
                    "n3 INTEGER DEFAULT 0",
                    "k3_mask INTEGER DEFAULT 0",
                    "raw_rank_score REAL DEFAULT 0",
                    "calibrated_probability REAL DEFAULT 0",
                    "gate REAL DEFAULT 0",
                    "edge REAL DEFAULT 0",
                    "coarse_family_key TEXT",
                    "mid_family_key TEXT",
                    "fine_family_key TEXT",
                    "position_family_key TEXT",
                    "snapshot_max_issue TEXT",
                    "trained_until_issue TEXT",
                    "build_id TEXT",
                    "cohort_id TEXT",
                    "schema_version TEXT",
                    "pipeline_version TEXT",
                    "calibration_version TEXT",
                    "gate_policy_version TEXT",
                    "selected_mode TEXT",
                    "authorization_id TEXT",
                    "authorization_valid INTEGER DEFAULT 0",
                    "authorization_reject_code TEXT",
                    "sample_source TEXT",
                    "created_at INTEGER DEFAULT 0",
                    "settled INTEGER DEFAULT 0",
                    "actual_number INTEGER DEFAULT 0",
                    "hit INTEGER DEFAULT 0",
                    "result TEXT",
                    "settled_at INTEGER DEFAULT 0",
                    "processed INTEGER DEFAULT 0",
                    "processed_at INTEGER DEFAULT 0",
                    "published_version TEXT",
                    "origin_group_key TEXT",
                    "rank_slot INTEGER DEFAULT 0",
                    "candidate_count INTEGER DEFAULT 0",
                    "honest_probability REAL DEFAULT 0",
                    "action_utility REAL DEFAULT 0",
                    "structure_score REAL DEFAULT 0",
                    "discrimination_score REAL DEFAULT 0",
                    "maturity_score REAL DEFAULT 0",
                    "uncertainty_penalty REAL DEFAULT 0",
                    "frequency_support REAL DEFAULT 0",
                    "action_utility_version TEXT",
                    "lane_candidate TEXT",
                    "coverage_quality REAL DEFAULT 0",
                    "third_quality REAL DEFAULT 0",
                    "counter_risk REAL DEFAULT 0",
                    "v42_accept_score REAL DEFAULT 0",
                    "v42_family_lcb REAL DEFAULT 0",
                    "v42_regime_quality REAL DEFAULT 0",
                    "v42_association_score REAL DEFAULT 0",
                    "v42_distillation_risk REAL DEFAULT 0",
                    "v42_support_votes INTEGER DEFAULT 0",
                    "v42_regime_family TEXT",
                    "v42_risk_coverage_mode TEXT",
                    "v42_primary_reject TEXT",
                    "v42_secondary_reject TEXT",
                    "v42_tertiary_reject TEXT",
                    "v42_lane_policy_version TEXT",
                    "v43_strategy_support REAL DEFAULT 0",
                    "v43_strategy_risk REAL DEFAULT 0",
                    "v43_strategy_confidence REAL DEFAULT 0",
                    "v43_strategy_diversity REAL DEFAULT 0",
                    "v43_strategy_maturity REAL DEFAULT 0",
                    "v43_safe_probability REAL DEFAULT 0",
                    "v43_probability_residual REAL DEFAULT 0",
                    "v43_ablation_fragility REAL DEFAULT 0",
                    "v43_probability_lock REAL DEFAULT 0",
                    "v43_evidence_group_count INTEGER DEFAULT 0",
                    "v43_hard_risk INTEGER DEFAULT 0",
                    "v43_dominant_evidence TEXT",
                    "v43_missing_evidence TEXT",
                    "v43_evidence_fingerprint TEXT",
                    "v43_probability_lock_hash TEXT",
                    "v43_pipeline_stage TEXT",
                    "v43_invariant_status TEXT",
                    "v43_strategy_version TEXT",
                    "window_state_key TEXT",
                    "dynamic_cluster_key TEXT",
                    "teacher_policy TEXT",
                    "posterior_policy TEXT",
                    "model_probability REAL DEFAULT 0",
                    "feature_0 REAL DEFAULT 0",
                    "feature_1 REAL DEFAULT 0",
                    "feature_2 REAL DEFAULT 0",
                    "feature_3 REAL DEFAULT 0",
                    "feature_4 REAL DEFAULT 0",
                    "feature_5 REAL DEFAULT 0",
                    "feature_6 REAL DEFAULT 0",
                    "feature_7 REAL DEFAULT 0",
                    "model_version TEXT",
                    "version TEXT",
                    "v46_event_probability REAL DEFAULT 0",
                    "v46_action_lcb REAL DEFAULT 0",
                    "v46_probability_uncertainty REAL DEFAULT 0",
                    "v46_expected_value REAL DEFAULT 0",
                    "v46_global_calibrated_probability REAL DEFAULT 0",
                    "v46_position_adjustment REAL DEFAULT 0",
                    "v46_rank_adjustment REAL DEFAULT 0",
                    "v46_regime_adjustment REAL DEFAULT 0",
                    "v46_calibration_confidence REAL DEFAULT 0",
                    "v46_final_rank INTEGER DEFAULT 0",
                    "v46_formal_candidate_count INTEGER DEFAULT 0",
                    "v46_version TEXT",
                    "v46_number_distribution_summary TEXT",
                    "v46_probability_trace TEXT",
                    "v46_root_cause_code TEXT",
                    "v46_root_cause_owner TEXT",
                    "v46_final_contract_hash TEXT",
                    "v46_lane TEXT"
                };
        for (int i = 0; i < oos.length; i++) addColumn(db, TABLE_OOS_ACTION_LOG, oos[i]);
        addIndex(
                db,
                "idx_oos_target_settled",
                "CREATE INDEX IF NOT EXISTS idx_oos_target_settled ON "
                        + TABLE_OOS_ACTION_LOG
                        + "(target_issue,settled,processed)");

        String[] posterior =
                new String[] {
                    "sample_count INTEGER DEFAULT 0",
                    "hit_count INTEGER DEFAULT 0",
                    "recent_sample INTEGER DEFAULT 0",
                    "recent_hit INTEGER DEFAULT 0",
                    "posterior_mean REAL DEFAULT 0",
                    "wilson_lower REAL DEFAULT 0",
                    "recent_rate REAL DEFAULT 0",
                    "drift_score REAL DEFAULT 0",
                    "last_target_issue TEXT",
                    "updated_at INTEGER DEFAULT 0",
                    "version TEXT"
                };
        for (int i = 0; i < posterior.length; i++)
            addColumn(db, TABLE_HIERARCHICAL_POSTERIOR, posterior[i]);
        String[] cal =
                new String[] {
                    "sample_count INTEGER DEFAULT 0",
                    "hit_count INTEGER DEFAULT 0",
                    "brier_sum REAL DEFAULT 0",
                    "updated_at INTEGER DEFAULT 0",
                    "version TEXT"
                };
        for (int i = 0; i < cal.length; i++) addColumn(db, TABLE_MONOTONIC_CALIBRATION, cal[i]);
        String[] auth =
                new String[] {
                    "authorization_id TEXT",
                    "build_id TEXT",
                    "cohort_id TEXT",
                    "schema_version TEXT",
                    "pipeline_version TEXT",
                    "calibration_version TEXT",
                    "authorized_mode TEXT",
                    "authorized_at INTEGER DEFAULT 0",
                    "valid INTEGER DEFAULT 0",
                    "calibrated_probability REAL DEFAULT 0",
                    "gate REAL DEFAULT 0",
                    "edge REAL DEFAULT 0",
                    "family_lcb REAL DEFAULT 0",
                    "coverage REAL DEFAULT 0",
                    "third_quality REAL DEFAULT 0",
                    "action_utility REAL DEFAULT 0",
                    "uncertainty_penalty REAL DEFAULT 0",
                    "structure_score REAL DEFAULT 0",
                    "lane_candidate TEXT",
                    "teacher_policy TEXT",
                    "dynamic_policy TEXT",
                    "adaptive_policy TEXT",
                    "reject_code TEXT",
                    "version TEXT"
                };
        for (int i = 0; i < auth.length; i++) addColumn(db, TABLE_DECISION_AUTHORIZATION, auth[i]);
        quarantineLegacyUnauthorizedV24Rows(db);
    }

    /**
     * V24 never inherits a V21/V22/V23 BET as a real trade. A row remains real only when its
     * immutable contract identity and the independently persisted authorization record both belong
     * to this exact build. Everything else is retained for audit but quarantined from real
     * win-rate/PnL statistics.
     */
    private void quarantineLegacyUnauthorizedV24Rows(SQLiteDatabase db) {
        if (db == null) return;
        String decisionValid =
                "authorization_valid=1 AND authorized_mode='BET' AND contract_valid=1 "
                        + "AND build_id='"
                        + AppConfig.BUILD_ID
                        + "' AND cohort_id='"
                        + AppConfig.COHORT_ID
                        + "' "
                        + "AND schema_version='"
                        + AppConfig.SCHEMA_VERSION
                        + "' AND pipeline_version='"
                        + AppConfig.PIPELINE_VERSION
                        + "' "
                        + "AND calibration_version='"
                        + AppConfig.CALIBRATION_VERSION
                        + "' AND authorization_id IS NOT NULL AND authorization_id<>'' "
                        + "AND EXISTS (SELECT 1 FROM "
                        + TABLE_DECISION_AUTHORIZATION
                        + " a WHERE a.contract_decision_id="
                        + TABLE_DECISIONS
                        + ".contract_decision_id "
                        + "AND a.authorization_id="
                        + TABLE_DECISIONS
                        + ".authorization_id AND a.valid=1 AND a.authorized_mode='BET' "
                        + "AND a.build_id='"
                        + AppConfig.BUILD_ID
                        + "' AND a.cohort_id='"
                        + AppConfig.COHORT_ID
                        + "' "
                        + "AND a.schema_version='"
                        + AppConfig.SCHEMA_VERSION
                        + "' AND a.pipeline_version='"
                        + AppConfig.PIPELINE_VERSION
                        + "' "
                        + "AND a.calibration_version='"
                        + AppConfig.CALIBRATION_VERSION
                        + "')";
        String tradeValid =
                "authorization_valid=1 AND authorized_mode='BET' AND contract_valid=1 "
                        + "AND build_id='"
                        + AppConfig.BUILD_ID
                        + "' AND cohort_id='"
                        + AppConfig.COHORT_ID
                        + "' "
                        + "AND schema_version='"
                        + AppConfig.SCHEMA_VERSION
                        + "' AND pipeline_version='"
                        + AppConfig.PIPELINE_VERSION
                        + "' "
                        + "AND calibration_version='"
                        + AppConfig.CALIBRATION_VERSION
                        + "' AND authorization_id IS NOT NULL AND authorization_id<>'' "
                        + "AND EXISTS (SELECT 1 FROM "
                        + TABLE_DECISION_AUTHORIZATION
                        + " a WHERE a.contract_decision_id="
                        + TABLE_TRADES
                        + ".contract_decision_id "
                        + "AND a.authorization_id="
                        + TABLE_TRADES
                        + ".authorization_id AND a.valid=1 AND a.authorized_mode='BET' "
                        + "AND a.build_id='"
                        + AppConfig.BUILD_ID
                        + "' AND a.cohort_id='"
                        + AppConfig.COHORT_ID
                        + "' "
                        + "AND a.schema_version='"
                        + AppConfig.SCHEMA_VERSION
                        + "' AND a.pipeline_version='"
                        + AppConfig.PIPELINE_VERSION
                        + "' "
                        + "AND a.calibration_version='"
                        + AppConfig.CALIBRATION_VERSION
                        + "')";
        execQuiet(
                db,
                "UPDATE "
                        + TABLE_DECISIONS
                        + " SET decision='SHADOW',action='SHADOW',stat_scope='SHADOW',is_realtime_real=0,authorization_valid=0,authorized_mode='SHADOW',authorization_reject_code='V24_LEGACY_UNAUTHORIZED_QUARANTINE'"
                        + " WHERE (decision='BET' OR action='BET' OR stat_scope='REAL_TRADE') AND"
                        + " NOT ("
                        + decisionValid
                        + ")");
        execQuiet(
                db,
                "UPDATE "
                        + TABLE_TRADES
                        + " SET action='SHADOW',stat_scope='SHADOW',is_realtime_real=0,pnl=0,authorization_valid=0,authorized_mode='SHADOW',authorization_reject_code='V24_LEGACY_UNAUTHORIZED_QUARANTINE'"
                        + " WHERE (action='BET' OR stat_scope='REAL_TRADE') AND NOT ("
                        + tradeValid
                        + ")");
    }

    private void addIndex(SQLiteDatabase db, String name, String sql) {
        execQuiet(db, sql);
    }

    private void migrateV48Columns(SQLiteDatabase db) {
        String[] observation =
                new String[] {
                    "base_issue TEXT",
                    "position INTEGER DEFAULT 0",
                    "n1 INTEGER DEFAULT 0",
                    "n2 INTEGER DEFAULT 0",
                    "n3 INTEGER DEFAULT 0",
                    "event_p REAL DEFAULT 0",
                    "action_lcb REAL DEFAULT 0",
                    "model_probability REAL DEFAULT 0",
                    "initial_rank INTEGER DEFAULT 0",
                    "teacher_rank INTEGER DEFAULT 0",
                    "dynamic_rank INTEGER DEFAULT 0",
                    "calibrated_rank INTEGER DEFAULT 0",
                    "final_rank INTEGER DEFAULT 0",
                    "build_id TEXT",
                    "cohort_id TEXT",
                    "schema_version TEXT",
                    "pipeline_version TEXT",
                    "model_version TEXT",
                    "calibration_version TEXT",
                    "ranking_version TEXT",
                    "identity_status TEXT",
                    "champion_status TEXT",
                    "action_status TEXT",
                    "action_block_code TEXT",
                    "root_code TEXT",
                    "root_detail TEXT",
                    "created_at INTEGER DEFAULT 0",
                    "settled INTEGER DEFAULT 0",
                    "actual_number INTEGER DEFAULT 0",
                    "hit INTEGER DEFAULT 0",
                    "result TEXT",
                    "settled_at INTEGER DEFAULT 0"
                };
        for (String column : observation) addColumn(db, TABLE_V48_DIAGNOSTIC_OBSERVATION, column);
        addIndex(
                db,
                "idx_v48_diag_settled",
                "CREATE INDEX IF NOT EXISTS idx_v48_diag_settled ON "
                        + TABLE_V48_DIAGNOSTIC_OBSERVATION
                        + "(settled,target_issue)");
        String[] artifact =
                new String[] {
                    "artifact_version TEXT",
                    "feature_version TEXT",
                    "payload TEXT",
                    "checksum TEXT",
                    "promoted INTEGER DEFAULT 0",
                    "updated_at INTEGER DEFAULT 0"
                };
        for (String column : artifact) addColumn(db, TABLE_V48_MODEL_ARTIFACT, column);
    }

    private void migrateDrawColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_DRAWS, "open_time INTEGER DEFAULT 0");
        addColumn(db, TABLE_DRAWS, "fetch_time INTEGER DEFAULT 0");
        addColumn(db, TABLE_DRAWS, "source_line TEXT");
        addColumn(db, TABLE_DRAWS, "n1 INTEGER DEFAULT 0");
        addColumn(db, TABLE_DRAWS, "n2 INTEGER DEFAULT 0");
        addColumn(db, TABLE_DRAWS, "n3 INTEGER DEFAULT 0");
        addColumn(db, TABLE_DRAWS, "n4 INTEGER DEFAULT 0");
        addColumn(db, TABLE_DRAWS, "n5 INTEGER DEFAULT 0");
        addColumn(db, TABLE_DRAWS, "n6 INTEGER DEFAULT 0");
        addColumn(db, TABLE_DRAWS, "n7 INTEGER DEFAULT 0");
        addColumn(db, TABLE_DRAWS, "n8 INTEGER DEFAULT 0");
        addColumn(db, TABLE_DRAWS, "n9 INTEGER DEFAULT 0");
        addColumn(db, TABLE_DRAWS, "n10 INTEGER DEFAULT 0");
        addColumn(db, TABLE_DRAWS, "raw_json TEXT");
        addColumn(db, TABLE_DRAWS, "dirty INTEGER DEFAULT 0");
        addColumn(db, TABLE_DRAWS, "dirty_reason TEXT");
    }

    private void migrateDecisionColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_DECISIONS, "base_issue TEXT");
        addColumn(db, TABLE_DECISIONS, "created_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "decision TEXT DEFAULT 'HOLD'");
        addColumn(db, TABLE_DECISIONS, "action TEXT");

        addColumn(db, TABLE_DECISIONS, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "n1 INTEGER DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "n2 INTEGER DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "n3 INTEGER DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "k3_mask INTEGER DEFAULT 0");

        addColumn(db, TABLE_DECISIONS, "raw_p REAL DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "calibrated_p REAL DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "final_p REAL DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "dynamic_gate REAL DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "edge REAL DEFAULT 0");

        addColumn(db, TABLE_DECISIONS, "model_p REAL DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "shape_p REAL DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "combo_p REAL DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "recovery_p REAL DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "similar_p REAL DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "memory_p REAL DEFAULT 0");

        addColumn(db, TABLE_DECISIONS, "similar_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "combo_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "effective_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "combo_sample INTEGER DEFAULT 0");

        addColumn(db, TABLE_DECISIONS, "trend_life REAL DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "drift_risk REAL DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "break_depth REAL DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "recovery_score REAL DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "champion_gap REAL DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "combo_gap REAL DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "hot_reverse_risk REAL DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "false_strong_risk REAL DEFAULT 0");

        addColumn(db, TABLE_DECISIONS, "freq100 REAL DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "freq500 REAL DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "freq1000 REAL DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "calibration_error REAL DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "probability_mode TEXT");
        addColumn(db, TABLE_DECISIONS, "backtest_mode TEXT");
        addColumn(db, TABLE_DECISIONS, "gate_mode TEXT");

        addColumn(db, TABLE_DECISIONS, "state_mode TEXT");
        addColumn(db, TABLE_DECISIONS, "shape_key TEXT");
        addColumn(db, TABLE_DECISIONS, "state_key TEXT");
        addColumn(db, TABLE_DECISIONS, "trend_key TEXT");
        addColumn(db, TABLE_DECISIONS, "drift_key TEXT");
        addColumn(db, TABLE_DECISIONS, "break_key TEXT");
        addColumn(db, TABLE_DECISIONS, "recovery_key TEXT");
        addColumn(db, TABLE_DECISIONS, "block_code TEXT");
        addColumn(db, TABLE_DECISIONS, "reason_summary TEXT");
        addColumn(db, TABLE_DECISIONS, "reason_text TEXT");

        addColumn(db, TABLE_DECISIONS, "data_ok INTEGER DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "settled INTEGER DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "result TEXT DEFAULT 'WAIT'");
        addColumn(db, TABLE_DECISIONS, "actual_number INTEGER DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "pnl REAL DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "settle_time INTEGER DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "settled_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "miss_reason TEXT");

        addDeepDecisionTradeColumns(db, TABLE_DECISIONS);

        addColumn(db, TABLE_DECISIONS, "state_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "cache_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "recent_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "recent_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "runtime_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "number_profile_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "realtime_cost_ms INTEGER DEFAULT 0");
        addColumn(db, TABLE_DECISIONS, "realtime_timeout INTEGER DEFAULT 0");

        addColumn(db, TABLE_DECISIONS, "stat_scope TEXT");

        addTeacherMatrixDecisionTradeColumns(db, TABLE_DECISIONS);
        addReasonDecisionTradeColumns(db, TABLE_DECISIONS);
        addV20DecisionTradeColumns(db, TABLE_DECISIONS);
        addDynamicCausalDecisionTradeColumns(db, TABLE_DECISIONS);
    }

    private void migrateTradeColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_TRADES, "base_issue TEXT");
        addColumn(db, TABLE_TRADES, "bet_time INTEGER DEFAULT 0");
        addColumn(db, TABLE_TRADES, "created_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_TRADES, "settle_time INTEGER DEFAULT 0");
        addColumn(db, TABLE_TRADES, "settled_at INTEGER DEFAULT 0");

        addColumn(db, TABLE_TRADES, "action TEXT");

        addColumn(db, TABLE_TRADES, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_TRADES, "n1 INTEGER DEFAULT 0");
        addColumn(db, TABLE_TRADES, "n2 INTEGER DEFAULT 0");
        addColumn(db, TABLE_TRADES, "n3 INTEGER DEFAULT 0");
        addColumn(db, TABLE_TRADES, "k3_mask INTEGER DEFAULT 0");

        addColumn(db, TABLE_TRADES, "raw_p REAL DEFAULT 0");
        addColumn(db, TABLE_TRADES, "calibrated_p REAL DEFAULT 0");
        addColumn(db, TABLE_TRADES, "final_p REAL DEFAULT 0");
        addColumn(db, TABLE_TRADES, "dynamic_gate REAL DEFAULT 0");
        addColumn(db, TABLE_TRADES, "edge REAL DEFAULT 0");

        addColumn(db, TABLE_TRADES, "model_p REAL DEFAULT 0");
        addColumn(db, TABLE_TRADES, "shape_p REAL DEFAULT 0");
        addColumn(db, TABLE_TRADES, "combo_p REAL DEFAULT 0");
        addColumn(db, TABLE_TRADES, "recovery_p REAL DEFAULT 0");
        addColumn(db, TABLE_TRADES, "similar_p REAL DEFAULT 0");
        addColumn(db, TABLE_TRADES, "memory_p REAL DEFAULT 0");
        addColumn(db, TABLE_TRADES, "calibration_error REAL DEFAULT 0");

        addColumn(db, TABLE_TRADES, "similar_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_TRADES, "combo_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_TRADES, "effective_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_TRADES, "combo_sample INTEGER DEFAULT 0");

        addColumn(db, TABLE_TRADES, "trend_life REAL DEFAULT 0");
        addColumn(db, TABLE_TRADES, "drift_risk REAL DEFAULT 0");
        addColumn(db, TABLE_TRADES, "break_depth REAL DEFAULT 0");
        addColumn(db, TABLE_TRADES, "recovery_score REAL DEFAULT 0");
        addColumn(db, TABLE_TRADES, "champion_gap REAL DEFAULT 0");
        addColumn(db, TABLE_TRADES, "combo_gap REAL DEFAULT 0");
        addColumn(db, TABLE_TRADES, "hot_reverse_risk REAL DEFAULT 0");
        addColumn(db, TABLE_TRADES, "false_strong_risk REAL DEFAULT 0");

        addColumn(db, TABLE_TRADES, "state_mode TEXT");
        addColumn(db, TABLE_TRADES, "shape_key TEXT");
        addColumn(db, TABLE_TRADES, "state_key TEXT");
        addColumn(db, TABLE_TRADES, "reason_summary TEXT");

        addColumn(db, TABLE_TRADES, "result TEXT DEFAULT 'WAIT'");
        addColumn(db, TABLE_TRADES, "actual_number INTEGER DEFAULT 0");
        addColumn(db, TABLE_TRADES, "hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_TRADES, "pnl REAL DEFAULT 0");
        addColumn(db, TABLE_TRADES, "miss_reason TEXT");
        addColumn(db, TABLE_TRADES, "settlement_reason TEXT");
        addColumn(db, TABLE_TRADES, "settled INTEGER DEFAULT 0");
        addColumn(db, TABLE_TRADES, "stat_scope TEXT");

        addDeepDecisionTradeColumns(db, TABLE_TRADES);
        addTeacherMatrixDecisionTradeColumns(db, TABLE_TRADES);
        addReasonDecisionTradeColumns(db, TABLE_TRADES);
        addV20DecisionTradeColumns(db, TABLE_TRADES);
        addDynamicCausalDecisionTradeColumns(db, TABLE_TRADES);
    }

    private void addDeepDecisionTradeColumns(SQLiteDatabase db, String table) {
        addColumn(db, table, "deep_score REAL DEFAULT 0");
        addColumn(db, table, "deep_confidence REAL DEFAULT 0");
        addColumn(db, table, "deep_feature_p REAL DEFAULT 0");
        addColumn(db, table, "deep_position_score REAL DEFAULT 0");
        addColumn(db, table, "deep_number_score REAL DEFAULT 0");
        addColumn(db, table, "deep_cycle_score REAL DEFAULT 0");
        addColumn(db, table, "deep_transition_score REAL DEFAULT 0");
        addColumn(db, table, "deep_state_score REAL DEFAULT 0");

        addColumn(db, table, "bias_score REAL DEFAULT 0");
        addColumn(db, table, "bias_confidence REAL DEFAULT 0");
        addColumn(db, table, "profile_score REAL DEFAULT 0");
        addColumn(db, table, "fallback_profile_score REAL DEFAULT 0");
        addColumn(db, table, "runtime_profile_score REAL DEFAULT 0");
        addColumn(db, table, "state_number_profile_score REAL DEFAULT 0");
        addColumn(db, table, "teacher_weight REAL DEFAULT 1");
        addColumn(db, table, "state_confidence REAL DEFAULT 0");
        addColumn(db, table, "audit_penalty REAL DEFAULT 0");
        addColumn(db, table, "audit_boost REAL DEFAULT 0");
        addColumn(db, table, "ai_judge_score REAL DEFAULT 0");
        addColumn(db, table, "ai_judge_confidence REAL DEFAULT 0");
        addColumn(db, table, "evidence_score REAL DEFAULT 0");
        addColumn(db, table, "evidence_risk REAL DEFAULT 0");
        addColumn(db, table, "conflict_level REAL DEFAULT 0");
        addColumn(db, table, "a_plus_score REAL DEFAULT 0");
        addColumn(db, table, "promotion_score REAL DEFAULT 0");
        addColumn(db, table, "weakest_number_score REAL DEFAULT 0");
        addColumn(db, table, "balance_score REAL DEFAULT 0");

        addColumn(db, table, "deep_profile_key TEXT");
        addColumn(db, table, "deep_profile_hit_rate REAL DEFAULT 0");
        addColumn(db, table, "deep_profile_sample INTEGER DEFAULT 0");
        addColumn(db, table, "deep_fallback_level INTEGER DEFAULT 0");
        addColumn(db, table, "fallback_profile_key TEXT");
        addColumn(db, table, "promotion_status TEXT");
        addColumn(db, table, "a_plus_status TEXT");
        addColumn(db, table, "candidate_evidence_key TEXT");
        addColumn(db, table, "snapshot_id TEXT");
        addColumn(db, table, "deep_attribution TEXT");
        addColumn(db, table, "deep_reason TEXT");
        addColumn(db, table, "ai_judge_summary TEXT");
        addColumn(db, table, "learning_summary TEXT");
        addColumn(db, table, "self_diagnosis_text TEXT");
        addColumn(db, table, "manual_backtest_support INTEGER DEFAULT 0");

        if (TABLE_TRADES.equals(table)) {
            addColumn(db, table, "attribution_reason_code TEXT");
            addColumn(db, table, "attribution_text TEXT");
            addColumn(db, table, "affected_module TEXT");
        }
    }

    private void addTeacherMatrixDecisionTradeColumns(SQLiteDatabase db, String table) {
        addColumn(db, table, "teacher_matrix_sample INTEGER DEFAULT 0");
        addColumn(db, table, "teacher_matrix_positive INTEGER DEFAULT 0");
        addColumn(db, table, "teacher_matrix_negative INTEGER DEFAULT 0");
        addColumn(db, table, "teacher_matrix_state_sample INTEGER DEFAULT 0");
        addColumn(db, table, "teacher_matrix_combo_sample INTEGER DEFAULT 0");
        addColumn(db, table, "teacher_matrix_hit_rate REAL DEFAULT 0");
        addColumn(db, table, "teacher_matrix_recent_hit_rate REAL DEFAULT 0");
        addColumn(db, table, "teacher_matrix_confidence REAL DEFAULT 0");
        addColumn(db, table, "teacher_matrix_score REAL DEFAULT 0");
        addColumn(db, table, "teacher_matrix_prob_adjust REAL DEFAULT 0");
        addColumn(db, table, "teacher_matrix_gate_adjust REAL DEFAULT 0");
        addColumn(db, table, "teacher_matrix_risk_penalty REAL DEFAULT 0");
        addColumn(db, table, "teacher_matrix_position_score REAL DEFAULT 0");
        addColumn(db, table, "teacher_matrix_number_score REAL DEFAULT 0");
        addColumn(db, table, "teacher_matrix_combo_score REAL DEFAULT 0");
        addColumn(db, table, "teacher_matrix_state_score REAL DEFAULT 0");
        addColumn(db, table, "teacher_matrix_applied INTEGER DEFAULT 0");
        addColumn(db, table, "teacher_matrix_status TEXT");
        addColumn(db, table, "teacher_matrix_profile_key TEXT");
        addColumn(db, table, "teacher_matrix_combo_key TEXT");
        addColumn(db, table, "teacher_matrix_summary TEXT");
    }

    private void addReasonDecisionTradeColumns(SQLiteDatabase db, String table) {
        addColumn(db, table, "source_type TEXT");
        addColumn(db, table, "is_realtime_real INTEGER DEFAULT 0");

        addColumn(db, table, "pre_draw_reason_fingerprint TEXT");
        addColumn(db, table, "pre_draw_reason_text TEXT");
        addColumn(db, table, "reason_valid_level TEXT");
        addColumn(db, table, "reason_score REAL DEFAULT 0");
        addColumn(db, table, "reason_edge REAL DEFAULT 0");
        addColumn(db, table, "hit_similarity REAL DEFAULT 0");
        addColumn(db, table, "miss_similarity REAL DEFAULT 0");
        addColumn(db, table, "contradiction_score REAL DEFAULT 0");
        addColumn(db, table, "causal_lift REAL DEFAULT 0");
        addColumn(db, table, "causal_confidence REAL DEFAULT 0");
        addColumn(db, table, "valid_reason_count INTEGER DEFAULT 0");
        addColumn(db, table, "false_reason_count INTEGER DEFAULT 0");
        addColumn(db, table, "forward_reason_sample INTEGER DEFAULT 0");
        addColumn(db, table, "forward_reason_hit_rate REAL DEFAULT 0");
        addColumn(db, table, "reason_wilson_score REAL DEFAULT 0");

        addColumn(db, table, "k3_role_summary TEXT");
        addColumn(db, table, "k3_main_number INTEGER DEFAULT 0");
        addColumn(db, table, "k3_support_number INTEGER DEFAULT 0");
        addColumn(db, table, "k3_defense_number INTEGER DEFAULT 0");
        addColumn(db, table, "combo_role_diversity REAL DEFAULT 0");
        addColumn(db, table, "combo_conflict REAL DEFAULT 0");

        addColumn(db, table, "fingerprint_cooling INTEGER DEFAULT 0");
        addColumn(db, table, "elite45_passed INTEGER DEFAULT 0");
        addColumn(db, table, "elite45_status TEXT");
        addColumn(db, table, "elite45_block_code TEXT");
        addColumn(db, table, "elite45_gate REAL DEFAULT 0");
        addColumn(db, table, "elite45_summary TEXT");
    }

    private void addV20DecisionTradeColumns(SQLiteDatabase db, String table) {
        addColumn(db, table, "v20_grade TEXT");
        addColumn(db, table, "v20_action TEXT");
        addColumn(db, table, "v20_candidate_key TEXT");
        addColumn(db, table, "v20_chain_key TEXT");
        addColumn(db, table, "v20_audit_key TEXT");
        addColumn(db, table, "v20_source_key TEXT");
        addColumn(db, table, "v20_rule_key TEXT");
        addColumn(db, table, "v20_same_class_key TEXT");
        addColumn(db, table, "v20_state_key TEXT");
        addColumn(db, table, "v20_position_key TEXT");
        addColumn(db, table, "v20_teacher_key TEXT");
        addColumn(db, table, "v20_rank_key TEXT");
        addColumn(db, table, "v20_guard_key TEXT");
        addColumn(db, table, "v20_shadow_key TEXT");
        addColumn(db, table, "v20_freeze_key TEXT");
        addColumn(db, table, "v20_seed_key TEXT");

        addColumn(db, table, "v20_final_cause_score REAL DEFAULT 0");
        addColumn(db, table, "v20_support_score REAL DEFAULT 0");
        addColumn(db, table, "v20_counter_score REAL DEFAULT 0");
        addColumn(db, table, "v20_confidence REAL DEFAULT 0");
        addColumn(db, table, "v20_sample_trust REAL DEFAULT 0");
        addColumn(db, table, "v20_recent_effectiveness REAL DEFAULT 0");
        addColumn(db, table, "v20_action_confidence REAL DEFAULT 0");
        addColumn(db, table, "v20_teacher_matrix_score REAL DEFAULT 0");
        addColumn(db, table, "v20_position_edge REAL DEFAULT 0");
        addColumn(db, table, "v20_number_teacher_edge REAL DEFAULT 0");
        addColumn(db, table, "v20_combo_role_edge REAL DEFAULT 0");
        addColumn(db, table, "v20_third_guard_edge REAL DEFAULT 0");
        addColumn(db, table, "v20_rank_counterfactual_edge REAL DEFAULT 0");
        addColumn(db, table, "v20_source_health_edge REAL DEFAULT 0");
        addColumn(db, table, "v20_state_health_edge REAL DEFAULT 0");
        addColumn(db, table, "v20_cycle_health_edge REAL DEFAULT 0");
        addColumn(db, table, "v20_seed_similarity REAL DEFAULT 0");
        addColumn(db, table, "v20_feature_distance REAL DEFAULT 0");
        addColumn(db, table, "v20_exact_state_match INTEGER DEFAULT 0");
        addColumn(db, table, "v20_relaxed_state_match INTEGER DEFAULT 0");
        addColumn(db, table, "v20_timing_offset INTEGER DEFAULT 0");
        addColumn(db, table, "v20_timing_bucket TEXT");
        addColumn(db, table, "v20_shadow_health REAL DEFAULT 0");

        addColumn(db, table, "v20_false_strong_risk REAL DEFAULT 0");
        addColumn(db, table, "v20_rule_decay_risk REAL DEFAULT 0");
        addColumn(db, table, "v20_same_class_lose_risk REAL DEFAULT 0");
        addColumn(db, table, "v20_guard_drag_risk REAL DEFAULT 0");
        addColumn(db, table, "v20_position_wrong_risk REAL DEFAULT 0");

        addColumn(db, table, "v20_position_healthy INTEGER DEFAULT 0");
        addColumn(db, table, "v20_number_teacher_support INTEGER DEFAULT 0");
        addColumn(db, table, "v20_rank_counterfactual_ok INTEGER DEFAULT 0");
        addColumn(db, table, "v20_third_guard_ok INTEGER DEFAULT 0");
        addColumn(db, table, "v20_shadow_health_pass INTEGER DEFAULT 0");
        addColumn(db, table, "v20_false_strong_frozen INTEGER DEFAULT 0");
        addColumn(db, table, "v20_same_class_frozen INTEGER DEFAULT 0");
        addColumn(db, table, "v20_rule_active INTEGER DEFAULT 0");
        addColumn(db, table, "v20_rule_frozen INTEGER DEFAULT 0");
        addColumn(db, table, "v20_source_healthy INTEGER DEFAULT 0");
        addColumn(db, table, "v20_state_healthy INTEGER DEFAULT 0");

        addColumn(db, table, "v20_reverse_reason_code TEXT");
        addColumn(db, table, "v20_action_suggestion TEXT");
        addColumn(db, table, "v20_evidence_json TEXT");
        addColumn(db, table, "v20_counter_evidence_json TEXT");
        addColumn(db, table, "v20_reason_atoms TEXT");
        addColumn(db, table, "v20_source_votes TEXT");
        addColumn(db, table, "v20_risk_tags TEXT");
        addColumn(db, table, "v20_decision_reason TEXT");
        addColumn(db, table, "v20_hold_reason TEXT");
        addColumn(db, table, "v20_miss_reason TEXT");
    }

    private void addDynamicCausalDecisionTradeColumns(SQLiteDatabase db, String table) {
        addColumn(db, table, "dynamic_cluster_id TEXT");
        addColumn(db, table, "dynamic_cluster_status TEXT");
        addColumn(db, table, "dynamic_cluster_prototype_key TEXT");
        addColumn(db, table, "dynamic_cluster_feature_key TEXT");
        addColumn(db, table, "dynamic_cluster_sample INTEGER DEFAULT 0");
        addColumn(db, table, "dynamic_cluster_hit_count INTEGER DEFAULT 0");
        addColumn(db, table, "dynamic_cluster_miss_count INTEGER DEFAULT 0");
        addColumn(db, table, "dynamic_cluster_recent_sample INTEGER DEFAULT 0");
        addColumn(db, table, "dynamic_cluster_recent_hit_count INTEGER DEFAULT 0");
        addColumn(db, table, "dynamic_cluster_hit_rate REAL DEFAULT 0");
        addColumn(db, table, "dynamic_cluster_recent_hit_rate REAL DEFAULT 0");
        addColumn(db, table, "dynamic_cluster_confidence REAL DEFAULT 0");
        addColumn(db, table, "dynamic_cluster_distance REAL DEFAULT 0");
        addColumn(db, table, "dynamic_cluster_volatility REAL DEFAULT 0");
        addColumn(db, table, "dynamic_cluster_max_lose INTEGER DEFAULT 0");
        addColumn(db, table, "dynamic_cluster_counter_score REAL DEFAULT 0");
        addColumn(db, table, "dynamic_cluster_version TEXT");
        addColumn(db, table, "dynamic_cluster_reason TEXT");
        addColumn(db, table, "position_need_key TEXT");
        addColumn(db, table, "position_need_summary TEXT");
        addColumn(db, table, "position_need_strength REAL DEFAULT 0");
        addColumn(db, table, "position_need_chaos_risk REAL DEFAULT 0");
        addColumn(db, table, "position_need_repair_score REAL DEFAULT 0");
        addColumn(db, table, "position_need_relay_score REAL DEFAULT 0");
        addColumn(db, table, "number_supply_key TEXT");
        addColumn(db, table, "number_supply_summary TEXT");
        addColumn(db, table, "number_supply_strength REAL DEFAULT 0");
        addColumn(db, table, "number_supply_risk REAL DEFAULT 0");
        addColumn(db, table, "need_supply_match_key TEXT");
        addColumn(db, table, "need_supply_match_score REAL DEFAULT 0");
        addColumn(db, table, "need_supply_third_quality REAL DEFAULT 0");
        addColumn(db, table, "need_supply_role_diversity REAL DEFAULT 0");
        addColumn(db, table, "need_supply_risk REAL DEFAULT 0");
        addColumn(db, table, "need_supply_summary TEXT");
        addColumn(db, table, "k3_role_main TEXT");
        addColumn(db, table, "k3_role_assist TEXT");
        addColumn(db, table, "k3_role_defense TEXT");
        addColumn(db, table, "k3_role_main_number INTEGER DEFAULT 0");
        addColumn(db, table, "k3_role_assist_number INTEGER DEFAULT 0");
        addColumn(db, table, "k3_role_defense_number INTEGER DEFAULT 0");
        addColumn(db, table, "k3_role_structure TEXT");
        addColumn(db, table, "counterfactual_reason_key TEXT");
        addColumn(db, table, "counterfactual_rank INTEGER DEFAULT 0");
        addColumn(db, table, "counterfactual_rank_score REAL DEFAULT 0");
        addColumn(db, table, "counterfactual_why_not_risk REAL DEFAULT 0");
        addColumn(db, table, "counterfactual_why_not TEXT");
        addColumn(db, table, "counterfactual_summary TEXT");
        addColumn(db, table, "cause_ablation_key TEXT");
        addColumn(db, table, "cause_ablation_score REAL DEFAULT 0");
        addColumn(db, table, "cause_ablation_false_cause_count INTEGER DEFAULT 0");
        addColumn(db, table, "cause_ablation_summary TEXT");
        addColumn(db, table, "miss_reason_type TEXT");
        addColumn(db, table, "miss_reason_matrix_key TEXT");
        addColumn(db, table, "miss_reason_detail TEXT");
        addColumn(db, table, "adaptive_judge_candidate_count INTEGER DEFAULT 0");
        addColumn(db, table, "adaptive_judge_status TEXT");
        addColumn(db, table, "adaptive_judge_action TEXT");
        addColumn(db, table, "adaptive_judge_domain TEXT");
        addColumn(db, table, "adaptive_cluster_hit_rate REAL DEFAULT 0");
        addColumn(db, table, "adaptive_cluster_recent_hit_rate REAL DEFAULT 0");
        addColumn(db, table, "adaptive_cluster_sample INTEGER DEFAULT 0");
        addColumn(db, table, "adaptive_cluster_confidence REAL DEFAULT 0");
        addColumn(db, table, "adaptive_cause_score REAL DEFAULT 0");
        addColumn(db, table, "adaptive_counter_score REAL DEFAULT 0");
        addColumn(db, table, "adaptive_promote_score REAL DEFAULT 0");
        addColumn(db, table, "adaptive_freeze_score REAL DEFAULT 0");
        addColumn(db, table, "adaptive_judge_reason TEXT");
        addColumn(db, table, "adaptive_judge_version TEXT");
        addColumn(db, table, "promotion_cluster_status TEXT");
        addColumn(db, table, "promotion_cluster_hit_rate REAL DEFAULT 0");
        addColumn(db, table, "promotion_cluster_recent_hit_rate REAL DEFAULT 0");
        addColumn(db, table, "promotion_cluster_sample INTEGER DEFAULT 0");
        addColumn(db, table, "promotion_cluster_reason TEXT");
        addColumn(db, table, "why_bet TEXT");
        addColumn(db, table, "why_no_bet TEXT");
        addColumn(db, table, "why_hit TEXT");
        addColumn(db, table, "why_miss TEXT");
        addColumn(db, table, "dynamic_causal_summary TEXT");
    }

    private void migrateDynamicCausalWindowColumns(SQLiteDatabase db) {
        addDynamicCausalDecisionTradeColumns(db, TABLE_FULL_CANDIDATE_SCAN);
        addDynamicCausalDecisionTradeColumns(db, TABLE_CANDIDATE_AUDIT);
        addDynamicCausalDecisionTradeColumns(db, TABLE_DYNAMIC_CAUSE_CHAIN);
        addDynamicCausalDecisionTradeColumns(db, TABLE_DECISION_AUDIT);
        addDynamicCausalDecisionTradeColumns(db, TABLE_V20_CLOSED_LOOP);
        addDynamicClusterColumns(db);
        addWindowFeatureSampleColumns(db);
        addPositionNeedSampleColumns(db);
        addNumberSupplySampleColumns(db);
        addNeedSupplyMatchSampleColumns(db);
        addCounterfactualReasonSampleColumns(db);
        addCauseAblationSampleColumns(db);
        addMissReasonSampleColumns(db);
        addAdaptiveJudgeSampleColumns(db);
        addPromotionDomainSnapshotColumns(db);
        addDynamicCausalStateColumns(db);
    }

    private void addDynamicClusterColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_DYNAMIC_WINDOW_CLUSTER, "prototype_key TEXT");
        addColumn(db, TABLE_DYNAMIC_WINDOW_CLUSTER, "prototype_vector TEXT");
        addColumn(db, TABLE_DYNAMIC_WINDOW_CLUSTER, "status TEXT");
        addColumn(db, TABLE_DYNAMIC_WINDOW_CLUSTER, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_WINDOW_CLUSTER, "hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_WINDOW_CLUSTER, "miss_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_WINDOW_CLUSTER, "recent_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_WINDOW_CLUSTER, "recent_hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_WINDOW_CLUSTER, "hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_WINDOW_CLUSTER, "recent_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_WINDOW_CLUSTER, "max_lose_streak INTEGER DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_WINDOW_CLUSTER, "confidence REAL DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_WINDOW_CLUSTER, "volatility REAL DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_WINDOW_CLUSTER, "counter_score REAL DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_WINDOW_CLUSTER, "prototype_distance REAL DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_WINDOW_CLUSTER, "last_target_issue TEXT");
        addColumn(db, TABLE_DYNAMIC_WINDOW_CLUSTER, "version TEXT");
        addColumn(db, TABLE_DYNAMIC_WINDOW_CLUSTER, "reason TEXT");
        addColumn(db, TABLE_DYNAMIC_WINDOW_CLUSTER, "created_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_WINDOW_CLUSTER, "updated_at INTEGER DEFAULT 0");
    }

    private void addWindowFeatureSampleColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_WINDOW_FEATURE_SAMPLE, "base_issue TEXT");
        addColumn(db, TABLE_WINDOW_FEATURE_SAMPLE, "target_issue TEXT");
        addColumn(db, TABLE_WINDOW_FEATURE_SAMPLE, "created_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_WINDOW_FEATURE_SAMPLE, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_WINDOW_FEATURE_SAMPLE, "n1 INTEGER DEFAULT 0");
        addColumn(db, TABLE_WINDOW_FEATURE_SAMPLE, "n2 INTEGER DEFAULT 0");
        addColumn(db, TABLE_WINDOW_FEATURE_SAMPLE, "n3 INTEGER DEFAULT 0");
        addColumn(db, TABLE_WINDOW_FEATURE_SAMPLE, "k3_mask INTEGER DEFAULT 0");
        addColumn(db, TABLE_WINDOW_FEATURE_SAMPLE, "vector_key TEXT");
        addColumn(db, TABLE_WINDOW_FEATURE_SAMPLE, "raw_key TEXT");
        addColumn(db, TABLE_WINDOW_FEATURE_SAMPLE, "cluster_id TEXT");
        addColumn(db, TABLE_WINDOW_FEATURE_SAMPLE, "cluster_status TEXT");
        addColumn(db, TABLE_WINDOW_FEATURE_SAMPLE, "distance REAL DEFAULT 0");
        addColumn(db, TABLE_WINDOW_FEATURE_SAMPLE, "would_hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_WINDOW_FEATURE_SAMPLE, "decision_type TEXT");
        addColumn(db, TABLE_WINDOW_FEATURE_SAMPLE, "trend_slope12 REAL DEFAULT 0");
        addColumn(db, TABLE_WINDOW_FEATURE_SAMPLE, "trend_slope24 REAL DEFAULT 0");
        addColumn(db, TABLE_WINDOW_FEATURE_SAMPLE, "trend_slope48 REAL DEFAULT 0");
        addColumn(db, TABLE_WINDOW_FEATURE_SAMPLE, "trend_slope96 REAL DEFAULT 0");
        addColumn(db, TABLE_WINDOW_FEATURE_SAMPLE, "trend_slope360 REAL DEFAULT 0");
        addColumn(db, TABLE_WINDOW_FEATURE_SAMPLE, "trend_life REAL DEFAULT 0");
        addColumn(db, TABLE_WINDOW_FEATURE_SAMPLE, "trend_fatigue REAL DEFAULT 0");
        addColumn(db, TABLE_WINDOW_FEATURE_SAMPLE, "omission_pressure REAL DEFAULT 0");
        addColumn(db, TABLE_WINDOW_FEATURE_SAMPLE, "hot_cold_transition REAL DEFAULT 0");
        addColumn(db, TABLE_WINDOW_FEATURE_SAMPLE, "density_score REAL DEFAULT 0");
        addColumn(db, TABLE_WINDOW_FEATURE_SAMPLE, "repeat_rhythm REAL DEFAULT 0");
        addColumn(db, TABLE_WINDOW_FEATURE_SAMPLE, "odd_even_bias REAL DEFAULT 0");
        addColumn(db, TABLE_WINDOW_FEATURE_SAMPLE, "high_low_bias REAL DEFAULT 0");
        addColumn(db, TABLE_WINDOW_FEATURE_SAMPLE, "tail_pressure REAL DEFAULT 0");
        addColumn(db, TABLE_WINDOW_FEATURE_SAMPLE, "neighbor_pressure REAL DEFAULT 0");
        addColumn(db, TABLE_WINDOW_FEATURE_SAMPLE, "cross_position_flow REAL DEFAULT 0");
        addColumn(db, TABLE_WINDOW_FEATURE_SAMPLE, "cross_position_crowd REAL DEFAULT 0");
        addColumn(db, TABLE_WINDOW_FEATURE_SAMPLE, "same_number_crowd REAL DEFAULT 0");
        addColumn(db, TABLE_WINDOW_FEATURE_SAMPLE, "timing_offset REAL DEFAULT 0");
        addColumn(db, TABLE_WINDOW_FEATURE_SAMPLE, "counter_evidence REAL DEFAULT 0");
        addColumn(db, TABLE_WINDOW_FEATURE_SAMPLE, "third_quality REAL DEFAULT 0");
        addColumn(db, TABLE_WINDOW_FEATURE_SAMPLE, "summary TEXT");
    }

    private void addPositionNeedSampleColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_POSITION_NEED_SAMPLE, "target_issue TEXT");
        addColumn(db, TABLE_POSITION_NEED_SAMPLE, "created_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_POSITION_NEED_SAMPLE, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_POSITION_NEED_SAMPLE, "actual_number INTEGER DEFAULT 0");
        addColumn(db, TABLE_POSITION_NEED_SAMPLE, "need_key TEXT");
        addColumn(db, TABLE_POSITION_NEED_SAMPLE, "need_hot_continue REAL DEFAULT 0");
        addColumn(db, TABLE_POSITION_NEED_SAMPLE, "need_cold_repair REAL DEFAULT 0");
        addColumn(db, TABLE_POSITION_NEED_SAMPLE, "need_omission_return REAL DEFAULT 0");
        addColumn(db, TABLE_POSITION_NEED_SAMPLE, "need_high_low_balance REAL DEFAULT 0");
        addColumn(db, TABLE_POSITION_NEED_SAMPLE, "need_odd_even_balance REAL DEFAULT 0");
        addColumn(db, TABLE_POSITION_NEED_SAMPLE, "need_tail_repair REAL DEFAULT 0");
        addColumn(db, TABLE_POSITION_NEED_SAMPLE, "need_neighbor_relay REAL DEFAULT 0");
        addColumn(db, TABLE_POSITION_NEED_SAMPLE, "need_cross_position_flow REAL DEFAULT 0");
        addColumn(db, TABLE_POSITION_NEED_SAMPLE, "need_crowd_release REAL DEFAULT 0");
        addColumn(db, TABLE_POSITION_NEED_SAMPLE, "need_reverse_break REAL DEFAULT 0");
        addColumn(db, TABLE_POSITION_NEED_SAMPLE, "need_stable_repeat REAL DEFAULT 0");
        addColumn(db, TABLE_POSITION_NEED_SAMPLE, "need_chaos_avoid REAL DEFAULT 0");
        addColumn(db, TABLE_POSITION_NEED_SAMPLE, "total_need_strength REAL DEFAULT 0");
        addColumn(db, TABLE_POSITION_NEED_SAMPLE, "chaos_risk REAL DEFAULT 0");
        addColumn(db, TABLE_POSITION_NEED_SAMPLE, "main_need_type TEXT");
        addColumn(db, TABLE_POSITION_NEED_SAMPLE, "summary TEXT");
    }

    private void addNumberSupplySampleColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_NUMBER_SUPPLY_SAMPLE, "target_issue TEXT");
        addColumn(db, TABLE_NUMBER_SUPPLY_SAMPLE, "created_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_NUMBER_SUPPLY_SAMPLE, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_NUMBER_SUPPLY_SAMPLE, "number INTEGER DEFAULT 0");
        addColumn(db, TABLE_NUMBER_SUPPLY_SAMPLE, "actual_number INTEGER DEFAULT 0");
        addColumn(db, TABLE_NUMBER_SUPPLY_SAMPLE, "label INTEGER DEFAULT 0");
        addColumn(db, TABLE_NUMBER_SUPPLY_SAMPLE, "supply_key TEXT");
        addColumn(db, TABLE_NUMBER_SUPPLY_SAMPLE, "supply_hot_continue REAL DEFAULT 0");
        addColumn(db, TABLE_NUMBER_SUPPLY_SAMPLE, "supply_cold_repair REAL DEFAULT 0");
        addColumn(db, TABLE_NUMBER_SUPPLY_SAMPLE, "supply_omission_return REAL DEFAULT 0");
        addColumn(db, TABLE_NUMBER_SUPPLY_SAMPLE, "supply_high_low_balance REAL DEFAULT 0");
        addColumn(db, TABLE_NUMBER_SUPPLY_SAMPLE, "supply_odd_even_balance REAL DEFAULT 0");
        addColumn(db, TABLE_NUMBER_SUPPLY_SAMPLE, "supply_tail_repair REAL DEFAULT 0");
        addColumn(db, TABLE_NUMBER_SUPPLY_SAMPLE, "supply_neighbor_relay REAL DEFAULT 0");
        addColumn(db, TABLE_NUMBER_SUPPLY_SAMPLE, "supply_cross_position_flow REAL DEFAULT 0");
        addColumn(db, TABLE_NUMBER_SUPPLY_SAMPLE, "supply_crowd_release REAL DEFAULT 0");
        addColumn(db, TABLE_NUMBER_SUPPLY_SAMPLE, "supply_reverse_break REAL DEFAULT 0");
        addColumn(db, TABLE_NUMBER_SUPPLY_SAMPLE, "supply_stable_repeat REAL DEFAULT 0");
        addColumn(db, TABLE_NUMBER_SUPPLY_SAMPLE, "supply_risk REAL DEFAULT 0");
        addColumn(db, TABLE_NUMBER_SUPPLY_SAMPLE, "total_supply_strength REAL DEFAULT 0");
        addColumn(db, TABLE_NUMBER_SUPPLY_SAMPLE, "main_supply_type TEXT");
        addColumn(db, TABLE_NUMBER_SUPPLY_SAMPLE, "summary TEXT");
    }

    private void addNeedSupplyMatchSampleColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_NEED_SUPPLY_MATCH_SAMPLE, "target_issue TEXT");
        addColumn(db, TABLE_NEED_SUPPLY_MATCH_SAMPLE, "created_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_NEED_SUPPLY_MATCH_SAMPLE, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_NEED_SUPPLY_MATCH_SAMPLE, "n1 INTEGER DEFAULT 0");
        addColumn(db, TABLE_NEED_SUPPLY_MATCH_SAMPLE, "n2 INTEGER DEFAULT 0");
        addColumn(db, TABLE_NEED_SUPPLY_MATCH_SAMPLE, "n3 INTEGER DEFAULT 0");
        addColumn(db, TABLE_NEED_SUPPLY_MATCH_SAMPLE, "k3_mask INTEGER DEFAULT 0");
        addColumn(db, TABLE_NEED_SUPPLY_MATCH_SAMPLE, "main_role_number INTEGER DEFAULT 0");
        addColumn(db, TABLE_NEED_SUPPLY_MATCH_SAMPLE, "assist_role_number INTEGER DEFAULT 0");
        addColumn(db, TABLE_NEED_SUPPLY_MATCH_SAMPLE, "defense_role_number INTEGER DEFAULT 0");
        addColumn(db, TABLE_NEED_SUPPLY_MATCH_SAMPLE, "main_role TEXT");
        addColumn(db, TABLE_NEED_SUPPLY_MATCH_SAMPLE, "assist_role TEXT");
        addColumn(db, TABLE_NEED_SUPPLY_MATCH_SAMPLE, "defense_role TEXT");
        addColumn(db, TABLE_NEED_SUPPLY_MATCH_SAMPLE, "match_score REAL DEFAULT 0");
        addColumn(db, TABLE_NEED_SUPPLY_MATCH_SAMPLE, "main_role_score REAL DEFAULT 0");
        addColumn(db, TABLE_NEED_SUPPLY_MATCH_SAMPLE, "assist_role_score REAL DEFAULT 0");
        addColumn(db, TABLE_NEED_SUPPLY_MATCH_SAMPLE, "defense_role_score REAL DEFAULT 0");
        addColumn(db, TABLE_NEED_SUPPLY_MATCH_SAMPLE, "third_quality REAL DEFAULT 0");
        addColumn(db, TABLE_NEED_SUPPLY_MATCH_SAMPLE, "role_diversity REAL DEFAULT 0");
        addColumn(db, TABLE_NEED_SUPPLY_MATCH_SAMPLE, "supply_risk REAL DEFAULT 0");
        addColumn(db, TABLE_NEED_SUPPLY_MATCH_SAMPLE, "why_match TEXT");
        addColumn(db, TABLE_NEED_SUPPLY_MATCH_SAMPLE, "why_not_match TEXT");
        addColumn(db, TABLE_NEED_SUPPLY_MATCH_SAMPLE, "summary TEXT");
    }

    private void addCounterfactualReasonSampleColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_COUNTERFACTUAL_REASON_SAMPLE, "target_issue TEXT");
        addColumn(db, TABLE_COUNTERFACTUAL_REASON_SAMPLE, "created_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_COUNTERFACTUAL_REASON_SAMPLE, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_COUNTERFACTUAL_REASON_SAMPLE, "n1 INTEGER DEFAULT 0");
        addColumn(db, TABLE_COUNTERFACTUAL_REASON_SAMPLE, "n2 INTEGER DEFAULT 0");
        addColumn(db, TABLE_COUNTERFACTUAL_REASON_SAMPLE, "n3 INTEGER DEFAULT 0");
        addColumn(db, TABLE_COUNTERFACTUAL_REASON_SAMPLE, "k3_mask INTEGER DEFAULT 0");
        addColumn(db, TABLE_COUNTERFACTUAL_REASON_SAMPLE, "cluster_id TEXT");
        addColumn(db, TABLE_COUNTERFACTUAL_REASON_SAMPLE, "position_rank INTEGER DEFAULT 0");
        addColumn(db, TABLE_COUNTERFACTUAL_REASON_SAMPLE, "k3_rank INTEGER DEFAULT 0");
        addColumn(db, TABLE_COUNTERFACTUAL_REASON_SAMPLE, "rank_score REAL DEFAULT 0");
        addColumn(db, TABLE_COUNTERFACTUAL_REASON_SAMPLE, "why_not_risk REAL DEFAULT 0");
        addColumn(db, TABLE_COUNTERFACTUAL_REASON_SAMPLE, "mirror_miss_similarity REAL DEFAULT 0");
        addColumn(db, TABLE_COUNTERFACTUAL_REASON_SAMPLE, "cause_score REAL DEFAULT 0");
        addColumn(db, TABLE_COUNTERFACTUAL_REASON_SAMPLE, "counter_score REAL DEFAULT 0");
        addColumn(db, TABLE_COUNTERFACTUAL_REASON_SAMPLE, "would_hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_COUNTERFACTUAL_REASON_SAMPLE, "why_this_position TEXT");
        addColumn(db, TABLE_COUNTERFACTUAL_REASON_SAMPLE, "why_these_numbers TEXT");
        addColumn(db, TABLE_COUNTERFACTUAL_REASON_SAMPLE, "why_not_other_position TEXT");
        addColumn(db, TABLE_COUNTERFACTUAL_REASON_SAMPLE, "why_not_other_numbers TEXT");
        addColumn(db, TABLE_COUNTERFACTUAL_REASON_SAMPLE, "summary TEXT");
    }

    private void addCauseAblationSampleColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_CAUSE_ABLATION_SAMPLE, "cluster_id TEXT");
        addColumn(db, TABLE_CAUSE_ABLATION_SAMPLE, "created_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_CAUSE_ABLATION_SAMPLE, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_CAUSE_ABLATION_SAMPLE, "omission_drop REAL DEFAULT 0");
        addColumn(db, TABLE_CAUSE_ABLATION_SAMPLE, "tail_drop REAL DEFAULT 0");
        addColumn(db, TABLE_CAUSE_ABLATION_SAMPLE, "neighbor_drop REAL DEFAULT 0");
        addColumn(db, TABLE_CAUSE_ABLATION_SAMPLE, "cross_position_drop REAL DEFAULT 0");
        addColumn(db, TABLE_CAUSE_ABLATION_SAMPLE, "need_supply_drop REAL DEFAULT 0");
        addColumn(db, TABLE_CAUSE_ABLATION_SAMPLE, "third_quality_drop REAL DEFAULT 0");
        addColumn(db, TABLE_CAUSE_ABLATION_SAMPLE, "total_ablation_score REAL DEFAULT 0");
        addColumn(db, TABLE_CAUSE_ABLATION_SAMPLE, "false_cause_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_CAUSE_ABLATION_SAMPLE, "false_cause_summary TEXT");
        addColumn(db, TABLE_CAUSE_ABLATION_SAMPLE, "summary TEXT");
    }

    private void addMissReasonSampleColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_MISS_REASON_SAMPLE, "target_issue TEXT");
        addColumn(db, TABLE_MISS_REASON_SAMPLE, "created_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_MISS_REASON_SAMPLE, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_MISS_REASON_SAMPLE, "n1 INTEGER DEFAULT 0");
        addColumn(db, TABLE_MISS_REASON_SAMPLE, "n2 INTEGER DEFAULT 0");
        addColumn(db, TABLE_MISS_REASON_SAMPLE, "n3 INTEGER DEFAULT 0");
        addColumn(db, TABLE_MISS_REASON_SAMPLE, "k3_mask INTEGER DEFAULT 0");
        addColumn(db, TABLE_MISS_REASON_SAMPLE, "actual_number INTEGER DEFAULT 0");
        addColumn(db, TABLE_MISS_REASON_SAMPLE, "miss_type TEXT");
        addColumn(db, TABLE_MISS_REASON_SAMPLE, "cluster_id TEXT");
        addColumn(db, TABLE_MISS_REASON_SAMPLE, "window_wrong_score REAL DEFAULT 0");
        addColumn(db, TABLE_MISS_REASON_SAMPLE, "position_wrong_score REAL DEFAULT 0");
        addColumn(db, TABLE_MISS_REASON_SAMPLE, "main_number_wrong_score REAL DEFAULT 0");
        addColumn(db, TABLE_MISS_REASON_SAMPLE, "third_drag_score REAL DEFAULT 0");
        addColumn(db, TABLE_MISS_REASON_SAMPLE, "counter_ignored_score REAL DEFAULT 0");
        addColumn(db, TABLE_MISS_REASON_SAMPLE, "detail TEXT");
        addColumn(db, TABLE_MISS_REASON_SAMPLE, "next_advice TEXT");
    }

    private void addAdaptiveJudgeSampleColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_ADAPTIVE_JUDGE_SAMPLE, "target_issue TEXT");
        addColumn(db, TABLE_ADAPTIVE_JUDGE_SAMPLE, "created_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_ADAPTIVE_JUDGE_SAMPLE, "candidate_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_ADAPTIVE_JUDGE_SAMPLE, "cluster_id TEXT");
        addColumn(db, TABLE_ADAPTIVE_JUDGE_SAMPLE, "cluster_status TEXT");
        addColumn(db, TABLE_ADAPTIVE_JUDGE_SAMPLE, "status TEXT");
        addColumn(db, TABLE_ADAPTIVE_JUDGE_SAMPLE, "action TEXT");
        addColumn(db, TABLE_ADAPTIVE_JUDGE_SAMPLE, "domain TEXT");
        addColumn(db, TABLE_ADAPTIVE_JUDGE_SAMPLE, "cluster_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_ADAPTIVE_JUDGE_SAMPLE, "cluster_recent_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_ADAPTIVE_JUDGE_SAMPLE, "cluster_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_ADAPTIVE_JUDGE_SAMPLE, "cluster_confidence REAL DEFAULT 0");
        addColumn(db, TABLE_ADAPTIVE_JUDGE_SAMPLE, "cause_score REAL DEFAULT 0");
        addColumn(db, TABLE_ADAPTIVE_JUDGE_SAMPLE, "counter_score REAL DEFAULT 0");
        addColumn(db, TABLE_ADAPTIVE_JUDGE_SAMPLE, "promote_score REAL DEFAULT 0");
        addColumn(db, TABLE_ADAPTIVE_JUDGE_SAMPLE, "freeze_score REAL DEFAULT 0");
        addColumn(db, TABLE_ADAPTIVE_JUDGE_SAMPLE, "reason TEXT");
        addColumn(db, TABLE_ADAPTIVE_JUDGE_SAMPLE, "version TEXT");
    }

    private void addPromotionDomainSnapshotColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_PROMOTION_DOMAIN_SNAPSHOT, "cluster_id TEXT");
        addColumn(db, TABLE_PROMOTION_DOMAIN_SNAPSHOT, "status TEXT");
        addColumn(db, TABLE_PROMOTION_DOMAIN_SNAPSHOT, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_PROMOTION_DOMAIN_SNAPSHOT, "hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_PROMOTION_DOMAIN_SNAPSHOT, "recent_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_PROMOTION_DOMAIN_SNAPSHOT, "recent_hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_PROMOTION_DOMAIN_SNAPSHOT, "hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_PROMOTION_DOMAIN_SNAPSHOT, "recent_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_PROMOTION_DOMAIN_SNAPSHOT, "max_lose_streak INTEGER DEFAULT 0");
        addColumn(db, TABLE_PROMOTION_DOMAIN_SNAPSHOT, "confidence REAL DEFAULT 0");
        addColumn(db, TABLE_PROMOTION_DOMAIN_SNAPSHOT, "action TEXT");
        addColumn(db, TABLE_PROMOTION_DOMAIN_SNAPSHOT, "reason TEXT");
        addColumn(db, TABLE_PROMOTION_DOMAIN_SNAPSHOT, "updated_at INTEGER DEFAULT 0");
    }

    private void addDynamicCausalStateColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_DYNAMIC_CAUSAL_STATE, "state_value TEXT");
        addColumn(db, TABLE_DYNAMIC_CAUSAL_STATE, "updated_at INTEGER DEFAULT 0");
    }

    /*
     * V20 主链闭环修复 03/22：DbHelper 只负责建表、补列、索引和账本保护。
     * 不计算概率，不生成 BET，不救援 BET，不改门槛，不结算。
     */
    private void migrateMainChainClosedLoopColumns(SQLiteDatabase db) {
        addMainChainClosedLoopColumns(db, TABLE_DECISIONS);
        addMainChainClosedLoopColumns(db, TABLE_TRADES);
        addMainChainClosedLoopColumns(db, TABLE_SHADOW);
        addMainChainClosedLoopColumns(db, TABLE_FULL_CANDIDATE_SCAN);
        addMainChainClosedLoopColumns(db, TABLE_CANDIDATE_AUDIT);
        addMainChainClosedLoopColumns(db, TABLE_DYNAMIC_CAUSE_CHAIN);
        addMainChainClosedLoopColumns(db, TABLE_DECISION_AUDIT);
        addMainChainClosedLoopColumns(db, TABLE_V20_CLOSED_LOOP);
        addMainChainClosedLoopColumns(db, TABLE_MAIN_CHAIN_CANDIDATE_SNAPSHOT);
        addMainChainClosedLoopColumns(db, TABLE_REAL_TRADE_LEDGER);
        addMainChainClosedLoopColumns(db, TABLE_ADAPTIVE_JUDGE_SAMPLE);
        addMainChainClosedLoopColumns(db, TABLE_PROMOTION_DOMAIN_SNAPSHOT);
        addMainChainFactorColumns(db);
        addMainChainRejectBucketColumns(db);
    }

    private void addMainChainClosedLoopColumns(SQLiteDatabase db, String table) {
        addColumn(db, table, "snapshot_key TEXT");
        addColumn(db, table, "base_issue TEXT");
        addColumn(db, table, "target_issue TEXT");
        addColumn(db, table, "created_at INTEGER DEFAULT 0");
        addColumn(db, table, "candidate_key TEXT");
        addColumn(db, table, "candidate_rank INTEGER DEFAULT 0");
        addColumn(db, table, "realtime_top40_rank INTEGER DEFAULT 0");
        addColumn(db, table, "adaptive_input_flag INTEGER DEFAULT 0");
        addColumn(db, table, "position INTEGER DEFAULT 0");
        addColumn(db, table, "n1 INTEGER DEFAULT 0");
        addColumn(db, table, "n2 INTEGER DEFAULT 0");
        addColumn(db, table, "n3 INTEGER DEFAULT 0");
        addColumn(db, table, "k3_mask INTEGER DEFAULT 0");
        addColumn(db, table, "window_state_key TEXT");
        addColumn(db, table, "dynamic_window_state_key TEXT");
        addColumn(db, table, "final_decision_window_state_key TEXT");
        addColumn(db, table, "probability_window_state_key TEXT");
        addColumn(db, table, "cluster_key TEXT");
        addColumn(db, table, "factor_key TEXT");
        addColumn(db, table, "position_need_score REAL DEFAULT 0");
        addColumn(db, table, "number_supply_score REAL DEFAULT 0");
        addColumn(db, table, "coverage_quality REAL DEFAULT 0");
        addColumn(db, table, "third_quality REAL DEFAULT 0");
        addColumn(db, table, "counter_evidence_score REAL DEFAULT 0");
        addColumn(db, table, "similar_sample_count INTEGER DEFAULT 0");
        addColumn(db, table, "similar_hit_rate REAL DEFAULT 0");
        addColumn(db, table, "teacher_status TEXT");
        addColumn(db, table, "teacher_status_class TEXT");
        addColumn(db, table, "teacher_number_status TEXT");
        addColumn(db, table, "teacher_sample_reliability REAL DEFAULT 0");
        addColumn(db, table, "teacher_recent_validity REAL DEFAULT 0");
        addColumn(db, table, "teacher_evidence_contribution REAL DEFAULT 0");
        addColumn(db, table, "teacher_counter_contribution REAL DEFAULT 0");
        addColumn(db, table, "v20_status TEXT");
        addColumn(db, table, "v20_action_advice TEXT");
        addColumn(db, table, "v20_sample_reliability REAL DEFAULT 0");
        addColumn(db, table, "v20_recent_validity REAL DEFAULT 0");
        addColumn(db, table, "v20_evidence_contribution REAL DEFAULT 0");
        addColumn(db, table, "v20_counter_contribution REAL DEFAULT 0");
        addColumn(db, table, "adaptive_status TEXT");
        addColumn(db, table, "shadow_promotion_status TEXT");
        addColumn(db, table, "final_p_before_diagnosis REAL DEFAULT 0");
        addColumn(db, table, "final_p_after_diagnosis REAL DEFAULT 0");
        addColumn(db, table, "gate REAL DEFAULT 0");
        addColumn(db, table, "edge REAL DEFAULT 0");
        addColumn(db, table, "reject_code TEXT");
        addColumn(db, table, "real_probe_allowed INTEGER DEFAULT 0");
        addColumn(db, table, "real_probe_reason TEXT");
        addColumn(db, table, "teacher_as_of_issue TEXT");
        addColumn(db, table, "teacher_issue_lag INTEGER DEFAULT 0");
        addColumn(db, table, "teacher_fresh_by_issue INTEGER DEFAULT 0");
        addColumn(db, table, "teacher_snapshot_created_at INTEGER DEFAULT 0");
        addColumn(db, table, "teacher_number_history_sample INTEGER DEFAULT 0");
        addColumn(db, table, "teacher_domain_shadow_sample INTEGER DEFAULT 0");
        addColumn(db, table, "teacher_domain_real_sample INTEGER DEFAULT 0");
        addColumn(db, table, "teacher_apply_error TEXT");
        addColumn(db, table, "predecision_domain_version TEXT");
        addColumn(db, table, "teacher_runtime_apply_version TEXT");
        addColumn(db, table, "block_tier TEXT");
        addColumn(db, table, "hard_risk_code TEXT");
        addColumn(db, table, "observe_reason TEXT");
        addColumn(db, table, "probe_eligible INTEGER DEFAULT 0");
        addColumn(db, table, "shadow_probe_eligible INTEGER DEFAULT 0");
        addColumn(db, table, "probe_level TEXT");
        addColumn(db, table, "shadow_settled_count INTEGER DEFAULT 0");
        addColumn(db, table, "shadow_domain_sample INTEGER DEFAULT 0");
        addColumn(db, table, "promotion_wilson_lower_bound REAL DEFAULT 0");
        addColumn(db, table, "promotion_eligibility_reason TEXT");
        addColumn(db, table, "predecision_opportunity INTEGER DEFAULT 0");
        addColumn(db, table, "risk_severity TEXT");
        addColumn(db, table, "hard_block_flag INTEGER DEFAULT 0");
        addColumn(db, table, "hard_block_source TEXT");
        addColumn(db, table, "evidence_source TEXT");
        addColumn(db, table, "evidence_settled_only INTEGER DEFAULT 0");
        addColumn(db, table, "evidence_sample_count INTEGER DEFAULT 0");
        addColumn(db, table, "evidence_hit_count INTEGER DEFAULT 0");
        addColumn(db, table, "evidence_miss_count INTEGER DEFAULT 0");
        addColumn(db, table, "promotion_coarse_domain TEXT");
        addColumn(db, table, "promotion_mid_domain TEXT");
        addColumn(db, table, "promotion_fine_domain TEXT");
        addColumn(db, table, "bet_source_engine TEXT");
        addColumn(db, table, "decision_gate_version TEXT");
        addColumn(db, table, "field_chain_version TEXT");
        addColumn(db, table, "schema_fix_version TEXT");
    }

    private void addMainChainFactorColumns(SQLiteDatabase db) {
        addFactorCoreColumns(db, TABLE_CAUSAL_FACTOR_MATRIX);
        addFactorCoreColumns(db, TABLE_POSITION_CAUSE_MATRIX);
        addFactorCoreColumns(db, TABLE_TEACHER_NUMBER_MATRIX);
        addFactorCoreColumns(db, TABLE_COUNTERFACTUAL_RANK);
        addFactorCoreColumns(db, TABLE_THIRD_GUARD_MATRIX);
        addFactorCoreColumns(db, TABLE_RULE_HEALTH_MATRIX);
        addFactorCoreColumns(db, TABLE_SOURCE_HEALTH_MATRIX);
        addFactorCoreColumns(db, TABLE_STATE_HEALTH_MATRIX);
        addFactorCoreColumns(db, TABLE_SHADOW_HEALTH_GATE);
        addFactorCoreColumns(db, TABLE_DYNAMIC_WINDOW_CLUSTER);
        addFactorCoreColumns(db, TABLE_WINDOW_FEATURE_SAMPLE);
        addFactorCoreColumns(db, TABLE_POSITION_NEED_SAMPLE);
        addFactorCoreColumns(db, TABLE_NUMBER_SUPPLY_SAMPLE);
        addFactorCoreColumns(db, TABLE_NEED_SUPPLY_MATCH_SAMPLE);
        addFactorCoreColumns(db, TABLE_COUNTERFACTUAL_REASON_SAMPLE);
    }

    private void addFactorCoreColumns(SQLiteDatabase db, String table) {
        addColumn(db, table, "state_key TEXT");
        addColumn(db, table, "window_state_key TEXT");
        addColumn(db, table, "cluster_key TEXT");
        addColumn(db, table, "factor_key TEXT");
        addColumn(db, table, "factor_id TEXT");
        addColumn(db, table, "factor_name TEXT");
        addColumn(db, table, "sample_count INTEGER DEFAULT 0");
        addColumn(db, table, "recent_sample_count INTEGER DEFAULT 0");
        addColumn(db, table, "hit_count INTEGER DEFAULT 0");
        addColumn(db, table, "miss_count INTEGER DEFAULT 0");
        addColumn(db, table, "hit_rate REAL DEFAULT 0");
        addColumn(db, table, "recent_hit_rate REAL DEFAULT 0");
        addColumn(db, table, "evidence_score REAL DEFAULT 0");
        addColumn(db, table, "anti_evidence_score REAL DEFAULT 0");
        addColumn(db, table, "net_evidence_score REAL DEFAULT 0");
        addColumn(db, table, "sample_reliability REAL DEFAULT 0");
        addColumn(db, table, "recent_validity REAL DEFAULT 0");
        addColumn(db, table, "action_advice TEXT");
        addColumn(db, table, "reject_code TEXT");
        addColumn(db, table, "updated_at INTEGER DEFAULT 0");
    }

    private void addMainChainRejectBucketColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_MAIN_CHAIN_REJECT_BUCKET, "reject_code TEXT");
        addColumn(db, TABLE_MAIN_CHAIN_REJECT_BUCKET, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_MAIN_CHAIN_REJECT_BUCKET, "candidate_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_MAIN_CHAIN_REJECT_BUCKET, "bet_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_MAIN_CHAIN_REJECT_BUCKET, "shadow_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_MAIN_CHAIN_REJECT_BUCKET, "hold_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_MAIN_CHAIN_REJECT_BUCKET, "hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_MAIN_CHAIN_REJECT_BUCKET, "miss_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_MAIN_CHAIN_REJECT_BUCKET, "avg_final_p REAL DEFAULT 0");
        addColumn(db, TABLE_MAIN_CHAIN_REJECT_BUCKET, "avg_gate REAL DEFAULT 0");
        addColumn(db, TABLE_MAIN_CHAIN_REJECT_BUCKET, "avg_edge REAL DEFAULT 0");
        addColumn(db, TABLE_MAIN_CHAIN_REJECT_BUCKET, "updated_at INTEGER DEFAULT 0");
    }

    private void migrateShadowColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_SHADOW, "base_issue TEXT");
        addColumn(db, TABLE_SHADOW, "target_issue TEXT");
        addColumn(db, TABLE_SHADOW, "created_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "rank_no INTEGER DEFAULT 0");

        addColumn(db, TABLE_SHADOW, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "n1 INTEGER DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "n2 INTEGER DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "n3 INTEGER DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "k3_mask INTEGER DEFAULT 0");

        addColumn(db, TABLE_SHADOW, "shape_key TEXT");
        addColumn(db, TABLE_SHADOW, "state_key TEXT");
        addColumn(db, TABLE_SHADOW, "state_mode TEXT");

        addColumn(db, TABLE_SHADOW, "model_p REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "shape_p REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "combo_p REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "recovery_p REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "similar_p REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "memory_p REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "raw_p REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "calibrated_p REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "final_p REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "dynamic_gate REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "edge REAL DEFAULT 0");

        addColumn(db, TABLE_SHADOW, "similar_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "combo_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "effective_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "combo_sample INTEGER DEFAULT 0");

        addColumn(db, TABLE_SHADOW, "trend_life REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "drift_risk REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "break_depth REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "recovery_score REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "champion_gap REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "combo_gap REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "hot_reverse_risk REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "false_strong_risk REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "calibration_error REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "probability_mode TEXT");
        addColumn(db, TABLE_SHADOW, "backtest_mode TEXT");

        addColumn(db, TABLE_SHADOW, "deep_score REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "deep_confidence REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "deep_feature_p REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "deep_position_score REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "deep_number_score REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "deep_cycle_score REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "deep_transition_score REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "deep_state_score REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "bias_score REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "bias_confidence REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "profile_score REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "fallback_profile_score REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "runtime_profile_score REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "state_number_profile_score REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "number_bias REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "combo_bias REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "teacher_weight REAL DEFAULT 1");
        addColumn(db, TABLE_SHADOW, "teacher_matrix_weight REAL DEFAULT 1");
        addColumn(db, TABLE_SHADOW, "teacher_matrix_score REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "state_confidence REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "audit_penalty REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "audit_boost REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "promotion_score REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "a_plus_score REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "evidence_score REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "evidence_risk REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "ai_judge_score REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "ai_judge_confidence REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "conflict_level REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "weakest_number_score REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "balance_score REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "rank_score REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "final_rank_score REAL DEFAULT 0");

        addColumn(db, TABLE_SHADOW, "state_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "cache_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "recent_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "recent_hit_rate REAL DEFAULT 0");

        addColumn(db, TABLE_SHADOW, "deep_profile_key TEXT");
        addColumn(db, TABLE_SHADOW, "deep_profile_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "deep_profile_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "deep_fallback_level INTEGER DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "fallback_profile_key TEXT");
        addColumn(db, TABLE_SHADOW, "candidate_evidence_key TEXT");
        addColumn(db, TABLE_SHADOW, "promotion_status TEXT");
        addColumn(db, TABLE_SHADOW, "a_plus_status TEXT");
        addColumn(db, TABLE_SHADOW, "deep_reason TEXT");
        addColumn(db, TABLE_SHADOW, "ai_judge_summary TEXT");

        addColumn(db, TABLE_SHADOW, "teacher_matrix_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "teacher_matrix_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "teacher_matrix_recent_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "teacher_matrix_confidence REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "teacher_matrix_prob_adjust REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "teacher_matrix_gate_adjust REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "teacher_matrix_risk_penalty REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "teacher_matrix_applied INTEGER DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "teacher_matrix_profile_key TEXT");
        addColumn(db, TABLE_SHADOW, "teacher_matrix_combo_key TEXT");
        addColumn(db, TABLE_SHADOW, "teacher_matrix_summary TEXT");

        addReasonDecisionTradeColumns(db, TABLE_SHADOW);
        addV20DecisionTradeColumns(db, TABLE_SHADOW);
        addDynamicCausalDecisionTradeColumns(db, TABLE_SHADOW);

        addColumn(db, TABLE_SHADOW, "hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "actual_number INTEGER DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "actual_rank INTEGER DEFAULT 0");
        addColumn(db, TABLE_SHADOW, "learned INTEGER DEFAULT 0");
    }

    private void migrateShapeVectorColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_SHAPE_VECTOR, "base_issue TEXT");
        addColumn(db, TABLE_SHAPE_VECTOR, "target_issue TEXT");
        addColumn(db, TABLE_SHAPE_VECTOR, "created_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "position INTEGER DEFAULT 0");

        addColumn(db, TABLE_SHAPE_VECTOR, "entropy16 REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "entropy32 REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "entropy64 REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "entropy128 REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "entropy240 REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "entropy480 REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "entropy960 REAL DEFAULT 0");

        addColumn(db, TABLE_SHAPE_VECTOR, "top3_mass16 REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "top3_mass32 REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "top3_mass64 REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "top3_mass128 REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "top3_mass240 REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "top4_mass128 REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "top6_mass240 REAL DEFAULT 0");

        addColumn(db, TABLE_SHAPE_VECTOR, "slope_short REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "slope_mid REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "slope_long REAL DEFAULT 0");

        addColumn(db, TABLE_SHAPE_VECTOR, "drift_short REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "drift_mid REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "drift_long REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "drift_risk REAL DEFAULT 0");

        addColumn(db, TABLE_SHAPE_VECTOR, "markov1_support REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "markov2_support REAL DEFAULT 0");

        addColumn(db, TABLE_SHAPE_VECTOR, "trend_life REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "break_depth REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "recovery_score REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "champion_gap REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "combo_gap REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "hot_reverse_risk REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "false_strong_risk REAL DEFAULT 0");

        addColumn(db, TABLE_SHAPE_VECTOR, "state_mode TEXT");
        addColumn(db, TABLE_SHAPE_VECTOR, "shape_key TEXT");
        addColumn(db, TABLE_SHAPE_VECTOR, "state_key TEXT");

        addColumn(db, TABLE_SHAPE_VECTOR, "p01 REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "p02 REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "p03 REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "p04 REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "p05 REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "p06 REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "p07 REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "p08 REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "p09 REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "p10 REAL DEFAULT 0");

        addColumn(db, TABLE_SHAPE_VECTOR, "m01 REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "m02 REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "m03 REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "m04 REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "m05 REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "m06 REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "m07 REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "m08 REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "m09 REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "m10 REAL DEFAULT 0");

        addColumn(db, TABLE_SHAPE_VECTOR, "deep_score REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "state_score REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "cycle_score REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "transition_score REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "bias_score REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "profile_score REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "runtime_profile_score REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "teacher_matrix_score REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "reason_score REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "reason_edge REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_VECTOR, "reason_fingerprint TEXT");
        addColumn(db, TABLE_SHAPE_VECTOR, "deep_profile_key TEXT");
    }

    private void migrateMemoryColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_SHAPE_MEMORY, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_SHAPE_MEMORY, "hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_SHAPE_MEMORY, "hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_MEMORY, "last240_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_SHAPE_MEMORY, "last240_hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_SHAPE_MEMORY, "last240_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_MEMORY, "last1000_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_SHAPE_MEMORY, "last1000_hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_SHAPE_MEMORY, "last1000_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_SHAPE_MEMORY, "fail_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_SHAPE_MEMORY, "reverse_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_SHAPE_MEMORY, "active_flag INTEGER DEFAULT 1");
        addColumn(db, TABLE_SHAPE_MEMORY, "updated_at INTEGER DEFAULT 0");

        addColumn(db, TABLE_COMBO_MEMORY, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_COMBO_MEMORY, "hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_COMBO_MEMORY, "hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_COMBO_MEMORY, "last240_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_COMBO_MEMORY, "last240_hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_COMBO_MEMORY, "last240_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_COMBO_MEMORY, "last1000_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_COMBO_MEMORY, "last1000_hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_COMBO_MEMORY, "last1000_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_COMBO_MEMORY, "avg_combo_gap REAL DEFAULT 0");
        addColumn(db, TABLE_COMBO_MEMORY, "avg_trend_life REAL DEFAULT 0");
        addColumn(db, TABLE_COMBO_MEMORY, "active_flag INTEGER DEFAULT 1");
        addColumn(db, TABLE_COMBO_MEMORY, "updated_at INTEGER DEFAULT 0");

        addColumn(db, TABLE_BREAK_MEMORY, "state_mode_before TEXT");
        addColumn(db, TABLE_BREAK_MEMORY, "break_type TEXT");
        addColumn(db, TABLE_BREAK_MEMORY, "trend_life_bucket INTEGER DEFAULT 0");
        addColumn(db, TABLE_BREAK_MEMORY, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_BREAK_MEMORY, "recover1_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_BREAK_MEMORY, "recover2_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_BREAK_MEMORY, "recover3_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_BREAK_MEMORY, "fail_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_BREAK_MEMORY, "reverse_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_BREAK_MEMORY, "recover_rate REAL DEFAULT 0");
        addColumn(db, TABLE_BREAK_MEMORY, "avg_recover_steps REAL DEFAULT 0");
        addColumn(db, TABLE_BREAK_MEMORY, "last240_recover_rate REAL DEFAULT 0");
        addColumn(db, TABLE_BREAK_MEMORY, "last1000_recover_rate REAL DEFAULT 0");
        addColumn(db, TABLE_BREAK_MEMORY, "updated_at INTEGER DEFAULT 0");
    }

    private void migrateCalibrationColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_CALIBRATION, "raw_min REAL DEFAULT 0");
        addColumn(db, TABLE_CALIBRATION, "raw_max REAL DEFAULT 0");
        addColumn(db, TABLE_CALIBRATION, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_CALIBRATION, "hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_CALIBRATION, "real_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_CALIBRATION, "last240_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_CALIBRATION, "last240_hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_CALIBRATION, "last240_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_CALIBRATION, "last1000_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_CALIBRATION, "last1000_hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_CALIBRATION, "last1000_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_CALIBRATION, "updated_at INTEGER DEFAULT 0");
    }

    private void migrateModelWeightColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_MODEL_WEIGHT, "window_weight REAL DEFAULT 0.24");
        addColumn(db, TABLE_MODEL_WEIGHT, "markov_weight REAL DEFAULT 0.14");
        addColumn(db, TABLE_MODEL_WEIGHT, "shape_weight REAL DEFAULT 0.18");
        addColumn(db, TABLE_MODEL_WEIGHT, "combo_weight REAL DEFAULT 0.14");
        addColumn(db, TABLE_MODEL_WEIGHT, "recovery_weight REAL DEFAULT 0.08");
        addColumn(db, TABLE_MODEL_WEIGHT, "similar_weight REAL DEFAULT 0.06");
        addColumn(db, TABLE_MODEL_WEIGHT, "memory_weight REAL DEFAULT 0.04");
        addColumn(db, TABLE_MODEL_WEIGHT, "position_weight REAL DEFAULT 0.08");
        addColumn(db, TABLE_MODEL_WEIGHT, "frequency_weight REAL DEFAULT 0.00");
        addColumn(db, TABLE_MODEL_WEIGHT, "drift_penalty_weight REAL DEFAULT 1.0");
        addColumn(db, TABLE_MODEL_WEIGHT, "hot_reverse_penalty_weight REAL DEFAULT 1.0");
        addColumn(db, TABLE_MODEL_WEIGHT, "false_strong_penalty_weight REAL DEFAULT 1.0");
        addColumn(db, TABLE_MODEL_WEIGHT, "break_penalty_weight REAL DEFAULT 1.0");
        addColumn(db, TABLE_MODEL_WEIGHT, "deep_feature_weight REAL DEFAULT 0.08");
        addColumn(db, TABLE_MODEL_WEIGHT, "bias_weight REAL DEFAULT 0.05");
        addColumn(db, TABLE_MODEL_WEIGHT, "profile_weight REAL DEFAULT 0.08");
        addColumn(db, TABLE_MODEL_WEIGHT, "runtime_profile_weight REAL DEFAULT 0.10");
        addColumn(db, TABLE_MODEL_WEIGHT, "state_number_profile_weight REAL DEFAULT 0.10");
        addColumn(db, TABLE_MODEL_WEIGHT, "teacher_weight REAL DEFAULT 1.0");
        addColumn(db, TABLE_MODEL_WEIGHT, "teacher_matrix_weight REAL DEFAULT 0.18");
        addColumn(db, TABLE_MODEL_WEIGHT, "reason_weight REAL DEFAULT 0.08");
        addColumn(db, TABLE_MODEL_WEIGHT, "elite45_weight REAL DEFAULT 0.06");
        addColumn(db, TABLE_MODEL_WEIGHT, "cycle_weight REAL DEFAULT 0.05");
        addColumn(db, TABLE_MODEL_WEIGHT, "transition_weight REAL DEFAULT 0.05");
        addColumn(db, TABLE_MODEL_WEIGHT, "ai_judge_weight REAL DEFAULT 0.10");
        addColumn(db, TABLE_MODEL_WEIGHT, "audit_weight REAL DEFAULT 0.06");
        addColumn(db, TABLE_MODEL_WEIGHT, "recent_error REAL DEFAULT 0");
        addColumn(db, TABLE_MODEL_WEIGHT, "recent_win_rate REAL DEFAULT 0");
        addColumn(db, TABLE_MODEL_WEIGHT, "recent_frequency REAL DEFAULT 0");
        addColumn(db, TABLE_MODEL_WEIGHT, "recent_pnl REAL DEFAULT 0");
        addColumn(db, TABLE_MODEL_WEIGHT, "updated_at INTEGER DEFAULT 0");
    }

    private void migrateBacktestColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_BACKTEST, "ok INTEGER DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "mode TEXT");
        addColumn(db, TABLE_BACKTEST, "type TEXT");
        addColumn(db, TABLE_BACKTEST, "window_no INTEGER DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "window INTEGER DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "range_start TEXT");
        addColumn(db, TABLE_BACKTEST, "range_end TEXT");
        addColumn(db, TABLE_BACKTEST, "start_issue TEXT");
        addColumn(db, TABLE_BACKTEST, "end_issue TEXT");
        addColumn(db, TABLE_BACKTEST, "train_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "test_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "total_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "bet_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "miss_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "hold_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "shadow_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "candidate_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "enrich_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "deep_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "learning_case_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "teacher_matrix_sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "teacher_matrix_profile_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "teacher_matrix_positive_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "teacher_matrix_negative_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "reason_sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "reason_profile_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "elite45_pass_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "win_rate REAL DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "frequency REAL DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "pnl REAL DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "max_drawdown REAL DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "longest_lose INTEGER DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "avg_raw_p REAL DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "avg_final_p REAL DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "avg_gate REAL DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "avg_edge REAL DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "calibration_error REAL DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "manual_triggered INTEGER DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "profile_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "attribution_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "promoted_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "rejected_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "a_plus_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "progress INTEGER DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "a_plus_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "bias_score_avg REAL DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "brier_score REAL DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "ai_judge_avg_score REAL DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "deep_score_avg REAL DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "teacher_matrix_avg_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "teacher_matrix_avg_confidence REAL DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "reason_avg_edge REAL DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "reason_avg_hit_similarity REAL DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "reason_avg_miss_similarity REAL DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "bias_summary TEXT");
        addColumn(db, TABLE_BACKTEST, "audit_summary TEXT");
        addColumn(db, TABLE_BACKTEST, "promotion_summary TEXT");
        addColumn(db, TABLE_BACKTEST, "a_plus_summary TEXT");
        addColumn(db, TABLE_BACKTEST, "learning_impact_summary TEXT");
        addColumn(db, TABLE_BACKTEST, "teacher_matrix_summary TEXT");
        addColumn(db, TABLE_BACKTEST, "reason_summary TEXT");
        addColumn(db, TABLE_BACKTEST, "elite45_summary TEXT");
        addColumn(db, TABLE_BACKTEST, "top_profiles TEXT");
        addColumn(db, TABLE_BACKTEST, "top_miss_reasons TEXT");
        addColumn(db, TABLE_BACKTEST, "top_hit_reasons TEXT");
        addColumn(db, TABLE_BACKTEST, "status TEXT");
        addColumn(db, TABLE_BACKTEST, "summary TEXT");
        addColumn(db, TABLE_BACKTEST, "started_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "finished_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "cost_ms INTEGER DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "created_at INTEGER DEFAULT 0");

        addColumn(db, TABLE_BACKTEST, "v20_s_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "v20_a_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "v20_b_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "v20_c_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "v20_b_promoted_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "v20_false_strong_block_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "v20_shadow_health_pass_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "v20_max_same_class_lose INTEGER DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "v20_default_mode_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "v20_expansion_mode_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_BACKTEST, "v20_reason_summary TEXT");
    }

    private void migrateAppStateColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_APP_STATE, "state_value TEXT");
        addColumn(db, TABLE_APP_STATE, "updated_at INTEGER DEFAULT 0");
    }

    private void migrateDeepColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_DEEP_PROFILE, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_DEEP_PROFILE, "state_mode TEXT");
        addColumn(db, TABLE_DEEP_PROFILE, "trend_bucket INTEGER DEFAULT 0");
        addColumn(db, TABLE_DEEP_PROFILE, "drift_bucket INTEGER DEFAULT 0");
        addColumn(db, TABLE_DEEP_PROFILE, "break_bucket INTEGER DEFAULT 0");
        addColumn(db, TABLE_DEEP_PROFILE, "recovery_bucket INTEGER DEFAULT 0");
        addColumn(db, TABLE_DEEP_PROFILE, "cycle_bucket INTEGER DEFAULT 0");
        addColumn(db, TABLE_DEEP_PROFILE, "relation_bucket INTEGER DEFAULT 0");
        addColumn(db, TABLE_DEEP_PROFILE, "total_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_DEEP_PROFILE, "total_hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_DEEP_PROFILE, "total_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_DEEP_PROFILE, "recent_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_DEEP_PROFILE, "recent_hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_DEEP_PROFILE, "recent_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_DEEP_PROFILE, "decay_weight REAL DEFAULT 1");
        addColumn(db, TABLE_DEEP_PROFILE, "active_flag INTEGER DEFAULT 1");
        addColumn(db, TABLE_DEEP_PROFILE, "stale_flag INTEGER DEFAULT 0");
        addColumn(db, TABLE_DEEP_PROFILE, "recover_flag INTEGER DEFAULT 0");
        addColumn(db, TABLE_DEEP_PROFILE, "last_hit_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_DEEP_PROFILE, "last_miss_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_DEEP_PROFILE, "fail_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_DEEP_PROFILE, "recover_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_DEEP_PROFILE, "avg_final_p REAL DEFAULT 0");
        addColumn(db, TABLE_DEEP_PROFILE, "avg_edge REAL DEFAULT 0");
        addColumn(db, TABLE_DEEP_PROFILE, "avg_deep_score REAL DEFAULT 0");
        addColumn(db, TABLE_DEEP_PROFILE, "teacher_matrix_score REAL DEFAULT 0");
        addColumn(db, TABLE_DEEP_PROFILE, "reason_score REAL DEFAULT 0");
        addColumn(db, TABLE_DEEP_PROFILE, "updated_at INTEGER DEFAULT 0");

        addColumn(db, TABLE_DEEP_ATTRIBUTION, "target_issue TEXT");
        addColumn(db, TABLE_DEEP_ATTRIBUTION, "base_issue TEXT");
        addColumn(db, TABLE_DEEP_ATTRIBUTION, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_DEEP_ATTRIBUTION, "k3_mask INTEGER DEFAULT 0");
        addColumn(db, TABLE_DEEP_ATTRIBUTION, "actual_number INTEGER DEFAULT 0");
        addColumn(db, TABLE_DEEP_ATTRIBUTION, "hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_DEEP_ATTRIBUTION, "main_reason_code TEXT");
        addColumn(db, TABLE_DEEP_ATTRIBUTION, "main_reason_text TEXT");
        addColumn(db, TABLE_DEEP_ATTRIBUTION, "confidence REAL DEFAULT 0");
        addColumn(db, TABLE_DEEP_ATTRIBUTION, "should_reward INTEGER DEFAULT 0");
        addColumn(db, TABLE_DEEP_ATTRIBUTION, "should_punish INTEGER DEFAULT 0");
        addColumn(db, TABLE_DEEP_ATTRIBUTION, "affected_module TEXT");
        addColumn(db, TABLE_DEEP_ATTRIBUTION, "profile_key TEXT");
        addColumn(db, TABLE_DEEP_ATTRIBUTION, "position_impact REAL DEFAULT 0");
        addColumn(db, TABLE_DEEP_ATTRIBUTION, "number_impact REAL DEFAULT 0");
        addColumn(db, TABLE_DEEP_ATTRIBUTION, "cycle_impact REAL DEFAULT 0");
        addColumn(db, TABLE_DEEP_ATTRIBUTION, "transition_impact REAL DEFAULT 0");
        addColumn(db, TABLE_DEEP_ATTRIBUTION, "calibration_impact REAL DEFAULT 0");
        addColumn(db, TABLE_DEEP_ATTRIBUTION, "teacher_matrix_impact REAL DEFAULT 0");
        addColumn(db, TABLE_DEEP_ATTRIBUTION, "reason_impact REAL DEFAULT 0");
        addColumn(db, TABLE_DEEP_ATTRIBUTION, "learning_text TEXT");
        addColumn(db, TABLE_DEEP_ATTRIBUTION, "created_at INTEGER DEFAULT 0");

        addColumn(db, TABLE_DEEP_TRANSITION, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_DEEP_TRANSITION, "from_number INTEGER DEFAULT 0");
        addColumn(db, TABLE_DEEP_TRANSITION, "mid_number INTEGER DEFAULT 0");
        addColumn(db, TABLE_DEEP_TRANSITION, "to_number INTEGER DEFAULT 0");
        addColumn(db, TABLE_DEEP_TRANSITION, "chain_type TEXT");
        addColumn(db, TABLE_DEEP_TRANSITION, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_DEEP_TRANSITION, "hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_DEEP_TRANSITION, "hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_DEEP_TRANSITION, "lift REAL DEFAULT 0");
        addColumn(db, TABLE_DEEP_TRANSITION, "recent_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_DEEP_TRANSITION, "recent_hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_DEEP_TRANSITION, "recent_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_DEEP_TRANSITION, "support_score REAL DEFAULT 0");
        addColumn(db, TABLE_DEEP_TRANSITION, "confidence REAL DEFAULT 0");
        addColumn(db, TABLE_DEEP_TRANSITION, "updated_at INTEGER DEFAULT 0");

        addColumn(db, TABLE_DEEP_CYCLE, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_DEEP_CYCLE, "number INTEGER DEFAULT 0");
        addColumn(db, TABLE_DEEP_CYCLE, "avg_gap REAL DEFAULT 0");
        addColumn(db, TABLE_DEEP_CYCLE, "last_seen_gap INTEGER DEFAULT 0");
        addColumn(db, TABLE_DEEP_CYCLE, "max_gap INTEGER DEFAULT 0");
        addColumn(db, TABLE_DEEP_CYCLE, "gap_variance REAL DEFAULT 0");
        addColumn(db, TABLE_DEEP_CYCLE, "cycle_score REAL DEFAULT 0");
        addColumn(db, TABLE_DEEP_CYCLE, "cycle_bucket INTEGER DEFAULT 0");
        addColumn(db, TABLE_DEEP_CYCLE, "near_cycle_point INTEGER DEFAULT 0");
        addColumn(db, TABLE_DEEP_CYCLE, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_DEEP_CYCLE, "updated_at INTEGER DEFAULT 0");

        migrateManualAndAuditColumns(db);
    }

    private void migrateManualAndAuditColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_MANUAL_BACKTEST_TASK, "task_name TEXT");
        addColumn(db, TABLE_MANUAL_BACKTEST_TASK, "mode TEXT");
        addColumn(db, TABLE_MANUAL_BACKTEST_TASK, "status TEXT");
        addColumn(db, TABLE_MANUAL_BACKTEST_TASK, "progress INTEGER DEFAULT 0");
        addColumn(db, TABLE_MANUAL_BACKTEST_TASK, "window_size INTEGER DEFAULT 0");
        addColumn(db, TABLE_MANUAL_BACKTEST_TASK, "start_index INTEGER DEFAULT 0");
        addColumn(db, TABLE_MANUAL_BACKTEST_TASK, "end_index INTEGER DEFAULT 0");
        addColumn(db, TABLE_MANUAL_BACKTEST_TASK, "min_train INTEGER DEFAULT 0");
        addColumn(db, TABLE_MANUAL_BACKTEST_TASK, "max_test INTEGER DEFAULT 0");
        addColumn(db, TABLE_MANUAL_BACKTEST_TASK, "top_keep INTEGER DEFAULT 0");
        addColumn(db, TABLE_MANUAL_BACKTEST_TASK, "total_steps INTEGER DEFAULT 0");
        addColumn(db, TABLE_MANUAL_BACKTEST_TASK, "done_steps INTEGER DEFAULT 0");
        addColumn(db, TABLE_MANUAL_BACKTEST_TASK, "started_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_MANUAL_BACKTEST_TASK, "updated_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_MANUAL_BACKTEST_TASK, "finished_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_MANUAL_BACKTEST_TASK, "summary TEXT");
        addColumn(db, TABLE_MANUAL_BACKTEST_TASK, "error TEXT");

        addColumn(db, TABLE_POINT_IN_TIME_SNAPSHOT, "base_issue TEXT");
        addColumn(db, TABLE_POINT_IN_TIME_SNAPSHOT, "target_issue TEXT");
        addColumn(db, TABLE_POINT_IN_TIME_SNAPSHOT, "train_start_issue TEXT");
        addColumn(db, TABLE_POINT_IN_TIME_SNAPSHOT, "train_end_issue TEXT");
        addColumn(db, TABLE_POINT_IN_TIME_SNAPSHOT, "visible_draw_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_POINT_IN_TIME_SNAPSHOT, "visible_trade_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_POINT_IN_TIME_SNAPSHOT, "visible_profile_version INTEGER DEFAULT 0");
        addColumn(
                db, TABLE_POINT_IN_TIME_SNAPSHOT, "visible_calibration_version INTEGER DEFAULT 0");
        addColumn(
                db,
                TABLE_POINT_IN_TIME_SNAPSHOT,
                "visible_teacher_matrix_version INTEGER DEFAULT 0");
        addColumn(db, TABLE_POINT_IN_TIME_SNAPSHOT, "visible_reason_version INTEGER DEFAULT 0");
        addColumn(db, TABLE_POINT_IN_TIME_SNAPSHOT, "feature_summary TEXT");
        addColumn(db, TABLE_POINT_IN_TIME_SNAPSHOT, "candidate_summary TEXT");
        addColumn(db, TABLE_POINT_IN_TIME_SNAPSHOT, "gate_summary TEXT");
        addColumn(db, TABLE_POINT_IN_TIME_SNAPSHOT, "decision_summary TEXT");
        addColumn(db, TABLE_POINT_IN_TIME_SNAPSHOT, "reason_summary TEXT");
        addColumn(db, TABLE_POINT_IN_TIME_SNAPSHOT, "data_hash TEXT");
        addColumn(db, TABLE_POINT_IN_TIME_SNAPSHOT, "no_future_leak_ok INTEGER DEFAULT 0");
        addColumn(db, TABLE_POINT_IN_TIME_SNAPSHOT, "status TEXT");
        addColumn(db, TABLE_POINT_IN_TIME_SNAPSHOT, "warning_text TEXT");
        addColumn(db, TABLE_POINT_IN_TIME_SNAPSHOT, "created_at INTEGER DEFAULT 0");

        addColumn(db, TABLE_PROMOTION_CANDIDATE, "profile_key TEXT");
        addColumn(db, TABLE_PROMOTION_CANDIDATE, "status TEXT");
        addColumn(db, TABLE_PROMOTION_CANDIDATE, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_PROMOTION_CANDIDATE, "hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_PROMOTION_CANDIDATE, "hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_PROMOTION_CANDIDATE, "recent_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_PROMOTION_CANDIDATE, "recent_hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_PROMOTION_CANDIDATE, "recent_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_PROMOTION_CANDIDATE, "calibration_error REAL DEFAULT 0");
        addColumn(db, TABLE_PROMOTION_CANDIDATE, "bias_score REAL DEFAULT 0");
        addColumn(db, TABLE_PROMOTION_CANDIDATE, "a_plus_score REAL DEFAULT 0");
        addColumn(db, TABLE_PROMOTION_CANDIDATE, "teacher_matrix_score REAL DEFAULT 0");
        addColumn(db, TABLE_PROMOTION_CANDIDATE, "reason_score REAL DEFAULT 0");
        addColumn(db, TABLE_PROMOTION_CANDIDATE, "promote_reason TEXT");
        addColumn(db, TABLE_PROMOTION_CANDIDATE, "reject_reason TEXT");
        addColumn(db, TABLE_PROMOTION_CANDIDATE, "updated_at INTEGER DEFAULT 0");

        addColumn(db, TABLE_PROBABILITY_AUDIT_BUCKET, "bucket_low REAL DEFAULT 0");
        addColumn(db, TABLE_PROBABILITY_AUDIT_BUCKET, "bucket_high REAL DEFAULT 0");
        addColumn(db, TABLE_PROBABILITY_AUDIT_BUCKET, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_PROBABILITY_AUDIT_BUCKET, "hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_PROBABILITY_AUDIT_BUCKET, "hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_PROBABILITY_AUDIT_BUCKET, "avg_predicted_p REAL DEFAULT 0");
        addColumn(db, TABLE_PROBABILITY_AUDIT_BUCKET, "calibration_error REAL DEFAULT 0");
        addColumn(db, TABLE_PROBABILITY_AUDIT_BUCKET, "brier_score REAL DEFAULT 0");
        addColumn(db, TABLE_PROBABILITY_AUDIT_BUCKET, "recent_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_PROBABILITY_AUDIT_BUCKET, "recent_hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_PROBABILITY_AUDIT_BUCKET, "recent_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_PROBABILITY_AUDIT_BUCKET, "recent_error REAL DEFAULT 0");
        addColumn(db, TABLE_PROBABILITY_AUDIT_BUCKET, "status TEXT");
        addColumn(db, TABLE_PROBABILITY_AUDIT_BUCKET, "updated_at INTEGER DEFAULT 0");
    }

    private void migrateRuntimeLearningColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_BIAS_EVIDENCE, "issue TEXT");
        addColumn(db, TABLE_BIAS_EVIDENCE, "base_issue TEXT");
        addColumn(db, TABLE_BIAS_EVIDENCE, "target_issue TEXT");
        addColumn(db, TABLE_BIAS_EVIDENCE, "bias_score REAL DEFAULT 0");
        addColumn(db, TABLE_BIAS_EVIDENCE, "position_bias_score REAL DEFAULT 0");
        addColumn(db, TABLE_BIAS_EVIDENCE, "number_bias_score REAL DEFAULT 0");
        addColumn(db, TABLE_BIAS_EVIDENCE, "transition_bias_score REAL DEFAULT 0");
        addColumn(db, TABLE_BIAS_EVIDENCE, "cycle_bias_score REAL DEFAULT 0");
        addColumn(db, TABLE_BIAS_EVIDENCE, "combo_bias_score REAL DEFAULT 0");
        addColumn(db, TABLE_BIAS_EVIDENCE, "state_bias_score REAL DEFAULT 0");
        addColumn(db, TABLE_BIAS_EVIDENCE, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_BIAS_EVIDENCE, "recent_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_BIAS_EVIDENCE, "confidence REAL DEFAULT 0");
        addColumn(db, TABLE_BIAS_EVIDENCE, "status TEXT");
        addColumn(db, TABLE_BIAS_EVIDENCE, "summary_text TEXT");
        addColumn(db, TABLE_BIAS_EVIDENCE, "created_at INTEGER DEFAULT 0");

        addCommonLogColumns(db, TABLE_LEARNING_SNAPSHOT);
        addCandidateEvidenceColumns(db);
        addAiJudgeColumns(db);
        addSelfDiagnosisColumns(db);
        addTeacherSnapshotColumns(db);
        addRealtimeValidateColumns(db);
        addRuntimeProfileColumns(db);
        addLearningCaseColumns(db);
        addAutoLearnCursorColumns(db);
    }

    private void addCommonLogColumns(SQLiteDatabase db, String table) {
        addColumn(db, table, "created_at INTEGER DEFAULT 0");
        addColumn(db, table, "source_type TEXT");
        addColumn(db, table, "target_issue TEXT");
        addColumn(db, table, "profile_key TEXT");
        addColumn(db, table, "hit INTEGER DEFAULT 0");
        addColumn(db, table, "affected_module TEXT");
        addColumn(db, table, "module_action TEXT");
        addColumn(db, table, "module_weight_delta REAL DEFAULT 0");
        addColumn(db, table, "confidence REAL DEFAULT 0");
        addColumn(db, table, "main_reason_code TEXT");
        addColumn(db, table, "summary_text TEXT");
        addColumn(db, table, "total_sample INTEGER DEFAULT 0");
        addColumn(db, table, "total_hit_rate REAL DEFAULT 0");
        addColumn(db, table, "recent_sample INTEGER DEFAULT 0");
        addColumn(db, table, "recent_hit_rate REAL DEFAULT 0");
        addColumn(db, table, "decay_weight REAL DEFAULT 1");
        addColumn(db, table, "fail_count INTEGER DEFAULT 0");
        addColumn(db, table, "stale_flag INTEGER DEFAULT 0");
        addColumn(db, table, "recover_flag INTEGER DEFAULT 0");
        addColumn(db, table, "teacher_matrix_sample_count INTEGER DEFAULT 0");
        addColumn(db, table, "teacher_matrix_hit_rate REAL DEFAULT 0");
        addColumn(db, table, "teacher_matrix_confidence REAL DEFAULT 0");
        addColumn(db, table, "reason_sample_count INTEGER DEFAULT 0");
        addColumn(db, table, "reason_edge REAL DEFAULT 0");
        addColumn(db, table, "reason_confidence REAL DEFAULT 0");
        addColumn(db, table, "profile_summary TEXT");
        addColumn(db, table, "gate_adjust_summary TEXT");
        addColumn(db, table, "probability_adjust_summary TEXT");
        addColumn(db, table, "audit_summary TEXT");
        addColumn(db, table, "bias_summary TEXT");
        addColumn(db, table, "promotion_summary TEXT");
        addColumn(db, table, "a_plus_summary TEXT");
        addColumn(db, table, "teacher_matrix_summary TEXT");
        addColumn(db, table, "reason_summary TEXT");
        addColumn(db, table, "active_json TEXT");
        addColumn(db, table, "learning_summary_text TEXT");
    }

    private void addCandidateEvidenceColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_CANDIDATE_EVIDENCE, "base_issue TEXT");
        addColumn(db, TABLE_CANDIDATE_EVIDENCE, "target_issue TEXT");
        addColumn(db, TABLE_CANDIDATE_EVIDENCE, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_CANDIDATE_EVIDENCE, "n1 INTEGER DEFAULT 0");
        addColumn(db, TABLE_CANDIDATE_EVIDENCE, "n2 INTEGER DEFAULT 0");
        addColumn(db, TABLE_CANDIDATE_EVIDENCE, "n3 INTEGER DEFAULT 0");
        addColumn(db, TABLE_CANDIDATE_EVIDENCE, "k3_mask INTEGER DEFAULT 0");
        addColumn(db, TABLE_CANDIDATE_EVIDENCE, "position_evidence REAL DEFAULT 0");
        addColumn(db, TABLE_CANDIDATE_EVIDENCE, "number_evidence REAL DEFAULT 0");
        addColumn(db, TABLE_CANDIDATE_EVIDENCE, "cycle_evidence REAL DEFAULT 0");
        addColumn(db, TABLE_CANDIDATE_EVIDENCE, "transition_evidence REAL DEFAULT 0");
        addColumn(db, TABLE_CANDIDATE_EVIDENCE, "pattern_evidence REAL DEFAULT 0");
        addColumn(db, TABLE_CANDIDATE_EVIDENCE, "combo_evidence REAL DEFAULT 0");
        addColumn(db, TABLE_CANDIDATE_EVIDENCE, "profile_evidence REAL DEFAULT 0");
        addColumn(db, TABLE_CANDIDATE_EVIDENCE, "runtime_profile_evidence REAL DEFAULT 0");
        addColumn(db, TABLE_CANDIDATE_EVIDENCE, "state_number_evidence REAL DEFAULT 0");
        addColumn(db, TABLE_CANDIDATE_EVIDENCE, "teacher_matrix_evidence REAL DEFAULT 0");
        addColumn(db, TABLE_CANDIDATE_EVIDENCE, "reason_evidence REAL DEFAULT 0");
        addColumn(db, TABLE_CANDIDATE_EVIDENCE, "audit_evidence REAL DEFAULT 0");
        addColumn(db, TABLE_CANDIDATE_EVIDENCE, "risk_score REAL DEFAULT 0");
        addColumn(db, TABLE_CANDIDATE_EVIDENCE, "evidence_score REAL DEFAULT 0");
        addColumn(db, TABLE_CANDIDATE_EVIDENCE, "confidence REAL DEFAULT 0");
        addColumn(db, TABLE_CANDIDATE_EVIDENCE, "support_text TEXT");
        addColumn(db, TABLE_CANDIDATE_EVIDENCE, "risk_text TEXT");
        addColumn(db, TABLE_CANDIDATE_EVIDENCE, "summary_text TEXT");
        addColumn(db, TABLE_CANDIDATE_EVIDENCE, "created_at INTEGER DEFAULT 0");
    }

    private void addAiJudgeColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_AI_JUDGE_LOG, "base_issue TEXT");
        addColumn(db, TABLE_AI_JUDGE_LOG, "target_issue TEXT");
        addColumn(db, TABLE_AI_JUDGE_LOG, "best_evidence_key TEXT");
        addColumn(db, TABLE_AI_JUDGE_LOG, "second_evidence_key TEXT");
        addColumn(db, TABLE_AI_JUDGE_LOG, "champion_gap REAL DEFAULT 0");
        addColumn(db, TABLE_AI_JUDGE_LOG, "judge_score REAL DEFAULT 0");
        addColumn(db, TABLE_AI_JUDGE_LOG, "judge_confidence REAL DEFAULT 0");
        addColumn(db, TABLE_AI_JUDGE_LOG, "conflict_level REAL DEFAULT 0");
        addColumn(db, TABLE_AI_JUDGE_LOG, "bet_advice TEXT");
        addColumn(db, TABLE_AI_JUDGE_LOG, "hold_reason TEXT");
        addColumn(db, TABLE_AI_JUDGE_LOG, "summary_text TEXT");
        addColumn(db, TABLE_AI_JUDGE_LOG, "created_at INTEGER DEFAULT 0");
    }

    private void addSelfDiagnosisColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "level TEXT");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "main_code TEXT");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "main_reason_code TEXT");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "main_problem TEXT");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "affected_module TEXT");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "suggested_action TEXT");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "short_text TEXT");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "summary_text TEXT");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "diagnosis_text TEXT");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "repair_advice TEXT");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "history_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "valid_draw_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "invalid_draw_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "duplicate_issue_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "latest_issue TEXT");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "candidate_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "valid_candidate_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "invalid_candidate_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "over_gate_candidate_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "blocked_candidate_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "real_trade_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "shadow_trade_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "hold_trade_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "trade_hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "trade_miss_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "mixed_stat_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "profile_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "active_profile_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "stale_profile_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "weak_profile_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "teacher_matrix_sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "teacher_matrix_profile_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "teacher_matrix_strong_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "teacher_matrix_risk_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "reason_sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "reason_profile_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "false_reason_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "elite45_pass_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "elite45_block_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "fatal_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "bad_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "warn_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "data_text TEXT");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "candidate_text TEXT");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "trade_text TEXT");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "profile_text TEXT");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "teacher_matrix_text TEXT");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "reason_text TEXT");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "elite45_text TEXT");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "decision_text TEXT");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "runtime_text TEXT");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "best_candidate_text TEXT");
        addColumn(db, TABLE_SELF_DIAGNOSIS_LOG, "created_at INTEGER DEFAULT 0");
    }

    private void addTeacherSnapshotColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "source_type TEXT");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "status TEXT");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "created_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "started_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "finished_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "cost_ms INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "window_size INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "total_history INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "total_steps INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "test_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "start_issue TEXT");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "end_issue TEXT");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "bet_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "miss_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "hold_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "shadow_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "win_rate REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "frequency REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "pnl REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "total_profit REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "max_drawdown REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "avg_profit_per_bet REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "calibration_error REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "brier_score REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "a_plus_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "a_plus_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "bias_score_avg REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "gate_adjust REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "probability_adjust REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "risk_adjust REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "teacher_confidence REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "stability_score REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "overfit_risk REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "profile_freshness REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "position_weights TEXT");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "number_weights TEXT");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "mode_weights TEXT");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "teacher_matrix_sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "teacher_matrix_profile_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "teacher_matrix_avg_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "teacher_matrix_avg_confidence REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "teacher_matrix_gate_adjust REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "teacher_matrix_prob_adjust REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "reason_sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "reason_profile_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "reason_avg_edge REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "reason_avg_confidence REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "elite45_pass_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "top_profiles TEXT");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "top_miss_reasons TEXT");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "top_hit_reasons TEXT");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "promoted_profiles TEXT");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "rejected_profiles TEXT");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "high_win_shape_text TEXT");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "high_risk_shape_text TEXT");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "learning_summary TEXT");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "gate_summary TEXT");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "probability_summary TEXT");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "audit_summary TEXT");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "bias_summary TEXT");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "promotion_summary TEXT");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "a_plus_summary TEXT");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "teacher_matrix_summary TEXT");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "reason_summary TEXT");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "elite45_summary TEXT");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "summary_text TEXT");
        addColumn(db, TABLE_TEACHER_SNAPSHOT, "error_text TEXT");
    }

    private void addRealtimeValidateColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_REALTIME_VALIDATE, "base_issue TEXT");
        addColumn(db, TABLE_REALTIME_VALIDATE, "target_issue TEXT");
        addColumn(db, TABLE_REALTIME_VALIDATE, "started_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "finished_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "created_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "cost_ms INTEGER DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "window_size INTEGER DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "total_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "bet_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "miss_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "hold_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "shadow_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "win_rate REAL DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "frequency REAL DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "pnl REAL DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "max_drawdown REAL DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "longest_lose INTEGER DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "recent_lose_streak INTEGER DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "avg_final_p REAL DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "avg_gate REAL DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "avg_edge REAL DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "avg_drift_risk REAL DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "avg_break_depth REAL DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "avg_trend_life REAL DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "avg_recovery_score REAL DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "sample_weak INTEGER DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "freq_high INTEGER DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "freq_hard INTEGER DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "hit_weak INTEGER DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "recent_miss_warn INTEGER DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "drawdown_warn INTEGER DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "stable_good INTEGER DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "gate_adjust REAL DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "recommend_gate_adjust REAL DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "ok INTEGER DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "usable INTEGER DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "status TEXT");
        addColumn(db, TABLE_REALTIME_VALIDATE, "summary TEXT");
        addColumn(db, TABLE_REALTIME_VALIDATE, "detail TEXT");
        addColumn(db, TABLE_REALTIME_VALIDATE, "warning_text TEXT");
        addColumn(db, TABLE_REALTIME_VALIDATE, "source TEXT");
        addColumn(db, TABLE_REALTIME_VALIDATE, "teacher_matrix_sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "teacher_matrix_used_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "teacher_matrix_avg_confidence REAL DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "teacher_matrix_summary TEXT");
        addColumn(db, TABLE_REALTIME_VALIDATE, "reason_sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "elite45_pass_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "reason_avg_edge REAL DEFAULT 0");
        addColumn(db, TABLE_REALTIME_VALIDATE, "reason_summary TEXT");
    }

    private void addRuntimeProfileColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_RUNTIME_PROFILE_CACHE, "state_key TEXT");
        addColumn(db, TABLE_RUNTIME_PROFILE_CACHE, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_RUNTIME_PROFILE_CACHE, "k3_mask INTEGER DEFAULT 0");
        addColumn(db, TABLE_RUNTIME_PROFILE_CACHE, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_RUNTIME_PROFILE_CACHE, "hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_RUNTIME_PROFILE_CACHE, "hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_RUNTIME_PROFILE_CACHE, "recent_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_RUNTIME_PROFILE_CACHE, "recent_hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_RUNTIME_PROFILE_CACHE, "recent_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_RUNTIME_PROFILE_CACHE, "avg_final_p REAL DEFAULT 0");
        addColumn(db, TABLE_RUNTIME_PROFILE_CACHE, "avg_gate REAL DEFAULT 0");
        addColumn(db, TABLE_RUNTIME_PROFILE_CACHE, "risk_score REAL DEFAULT 0");
        addColumn(db, TABLE_RUNTIME_PROFILE_CACHE, "teacher_weight REAL DEFAULT 1");
        addColumn(db, TABLE_RUNTIME_PROFILE_CACHE, "profile_score REAL DEFAULT 0");
        addColumn(db, TABLE_RUNTIME_PROFILE_CACHE, "state_confidence REAL DEFAULT 0");
        addColumn(db, TABLE_RUNTIME_PROFILE_CACHE, "updated_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_RUNTIME_PROFILE_CACHE, "summary_text TEXT");

        addColumn(db, TABLE_STATE_NUMBER_PROFILE, "state_key TEXT");
        addColumn(db, TABLE_STATE_NUMBER_PROFILE, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_STATE_NUMBER_PROFILE, "number INTEGER DEFAULT 0");
        addColumn(db, TABLE_STATE_NUMBER_PROFILE, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_STATE_NUMBER_PROFILE, "hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_STATE_NUMBER_PROFILE, "hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_STATE_NUMBER_PROFILE, "recent_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_STATE_NUMBER_PROFILE, "recent_hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_STATE_NUMBER_PROFILE, "recent_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_STATE_NUMBER_PROFILE, "trend_score REAL DEFAULT 0");
        addColumn(db, TABLE_STATE_NUMBER_PROFILE, "shape_score REAL DEFAULT 0");
        addColumn(db, TABLE_STATE_NUMBER_PROFILE, "risk_score REAL DEFAULT 0");
        addColumn(db, TABLE_STATE_NUMBER_PROFILE, "number_bias REAL DEFAULT 0");
        addColumn(db, TABLE_STATE_NUMBER_PROFILE, "confidence REAL DEFAULT 0");
        addColumn(db, TABLE_STATE_NUMBER_PROFILE, "why_text TEXT");
        addColumn(db, TABLE_STATE_NUMBER_PROFILE, "updated_at INTEGER DEFAULT 0");
    }

    private void addLearningCaseColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_LEARNING_CASE_LOG, "task_key TEXT");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "segment_key TEXT");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "base_issue TEXT");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "target_issue TEXT");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "created_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "target_index INTEGER DEFAULT 0");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "actual_number INTEGER DEFAULT 0");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "predicted_k3 INTEGER DEFAULT 0");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "k3_mask INTEGER DEFAULT 0");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "state_key TEXT");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "state_mode TEXT");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "trend_key TEXT");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "shape_key TEXT");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "drift_key TEXT");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "break_key TEXT");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "recovery_key TEXT");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "transition_key TEXT");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "expected_shape TEXT");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "actual_shape TEXT");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "why_expected_hit TEXT");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "why_actual_hit TEXT");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "why_miss TEXT");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "next_advice TEXT");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "main_reason_code TEXT");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "affected_module TEXT");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "final_p REAL DEFAULT 0");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "dynamic_gate REAL DEFAULT 0");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "edge REAL DEFAULT 0");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "trend_score REAL DEFAULT 0");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "shape_score REAL DEFAULT 0");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "drift_risk REAL DEFAULT 0");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "break_risk REAL DEFAULT 0");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "recovery_score REAL DEFAULT 0");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "profile_score REAL DEFAULT 0");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "number_bias REAL DEFAULT 0");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "combo_bias REAL DEFAULT 0");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "risk_score REAL DEFAULT 0");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "teacher_weight REAL DEFAULT 1");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "state_confidence REAL DEFAULT 0");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "state_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "cache_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "recent_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "recent_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "pre_draw_reason_fingerprint TEXT");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "pre_draw_reason_text TEXT");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "reason_valid_level TEXT");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "reason_score REAL DEFAULT 0");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "reason_edge REAL DEFAULT 0");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "hit_similarity REAL DEFAULT 0");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "miss_similarity REAL DEFAULT 0");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "contradiction_score REAL DEFAULT 0");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "causal_lift REAL DEFAULT 0");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "causal_confidence REAL DEFAULT 0");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "valid_reason_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "false_reason_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "k3_role_summary TEXT");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "elite45_passed INTEGER DEFAULT 0");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "elite45_summary TEXT");
        addColumn(db, TABLE_LEARNING_CASE_LOG, "saved INTEGER DEFAULT 0");
    }

    private void addAutoLearnCursorColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_AUTO_LEARN_CURSOR, "task_type TEXT");
        addColumn(db, TABLE_AUTO_LEARN_CURSOR, "window_size INTEGER DEFAULT 0");
        addColumn(db, TABLE_AUTO_LEARN_CURSOR, "segment_start_index INTEGER DEFAULT 0");
        addColumn(db, TABLE_AUTO_LEARN_CURSOR, "segment_end_index INTEGER DEFAULT 0");
        addColumn(db, TABLE_AUTO_LEARN_CURSOR, "next_target_index INTEGER DEFAULT 0");
        addColumn(db, TABLE_AUTO_LEARN_CURSOR, "last_start_index INTEGER DEFAULT 0");
        addColumn(db, TABLE_AUTO_LEARN_CURSOR, "random_seed INTEGER DEFAULT 0");
        addColumn(db, TABLE_AUTO_LEARN_CURSOR, "last_issue TEXT");
        addColumn(db, TABLE_AUTO_LEARN_CURSOR, "progress TEXT");
        addColumn(db, TABLE_AUTO_LEARN_CURSOR, "status TEXT");
        addColumn(db, TABLE_AUTO_LEARN_CURSOR, "used_segments TEXT");
        addColumn(db, TABLE_AUTO_LEARN_CURSOR, "updated_at INTEGER DEFAULT 0");
    }

    private void migrateTeacherMatrixColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "source_type TEXT");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "base_issue TEXT");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "target_issue TEXT");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "created_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "target_index INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "number INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "actual_number INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "label INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "k3_mask INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "state_key TEXT");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "state_mode TEXT");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "trend_key TEXT");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "shape_key TEXT");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "drift_key TEXT");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "break_key TEXT");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "recovery_key TEXT");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "transition_key TEXT");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "hot_cold_key TEXT");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "odd_even_key TEXT");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "high_low_key TEXT");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "relation_key TEXT");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "profile_key TEXT");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "combo_key TEXT");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "trend_score REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "shape_score REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "drift_risk REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "break_risk REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "recovery_score REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "number_score REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "position_score REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "risk_score REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "confidence REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "final_p REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "dynamic_gate REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "edge REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "reason_fingerprint TEXT");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "reason_edge REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "hit_similarity REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "miss_similarity REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "reason_level TEXT");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "why_positive TEXT");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "why_negative TEXT");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "why_miss TEXT");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "next_advice TEXT");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "main_reason_code TEXT");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "affected_module TEXT");
        addColumn(db, TABLE_TEACHER_MATRIX_SAMPLE, "saved INTEGER DEFAULT 0");

        addColumn(db, TABLE_TEACHER_MATRIX_PROFILE, "state_key TEXT");
        addColumn(db, TABLE_TEACHER_MATRIX_PROFILE, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_PROFILE, "number INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_PROFILE, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_PROFILE, "positive_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_PROFILE, "negative_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_PROFILE, "recent_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_PROFILE, "recent_positive INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_PROFILE, "recent_negative INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_PROFILE, "hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_PROFILE, "recent_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_PROFILE, "confidence REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_PROFILE, "position_score REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_PROFILE, "number_score REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_PROFILE, "state_score REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_PROFILE, "trend_score REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_PROFILE, "shape_score REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_PROFILE, "risk_score REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_PROFILE, "prob_adjust REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_PROFILE, "gate_adjust REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_PROFILE, "risk_penalty REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_PROFILE, "teacher_weight REAL DEFAULT 1");
        addColumn(db, TABLE_TEACHER_MATRIX_PROFILE, "updated_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_PROFILE, "last_hit_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_PROFILE, "last_miss_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_PROFILE, "status TEXT");
        addColumn(db, TABLE_TEACHER_MATRIX_PROFILE, "why_text TEXT");
        addColumn(db, TABLE_TEACHER_MATRIX_PROFILE, "summary_text TEXT");

        addColumn(db, TABLE_TEACHER_MATRIX_STATE_PROFILE, "state_key TEXT");
        addColumn(db, TABLE_TEACHER_MATRIX_STATE_PROFILE, "state_mode TEXT");
        addColumn(db, TABLE_TEACHER_MATRIX_STATE_PROFILE, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_STATE_PROFILE, "positive_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_STATE_PROFILE, "negative_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_STATE_PROFILE, "hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_STATE_PROFILE, "recent_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_STATE_PROFILE, "recent_positive INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_STATE_PROFILE, "recent_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_STATE_PROFILE, "confidence REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_STATE_PROFILE, "state_score REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_STATE_PROFILE, "risk_score REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_STATE_PROFILE, "prob_adjust REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_STATE_PROFILE, "gate_adjust REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_STATE_PROFILE, "updated_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_STATE_PROFILE, "status TEXT");
        addColumn(db, TABLE_TEACHER_MATRIX_STATE_PROFILE, "summary_text TEXT");

        addColumn(db, TABLE_TEACHER_MATRIX_COMBO_PROFILE, "state_key TEXT");
        addColumn(db, TABLE_TEACHER_MATRIX_COMBO_PROFILE, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_COMBO_PROFILE, "k3_mask INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_COMBO_PROFILE, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_COMBO_PROFILE, "hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_COMBO_PROFILE, "miss_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_COMBO_PROFILE, "recent_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_COMBO_PROFILE, "recent_hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_COMBO_PROFILE, "hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_COMBO_PROFILE, "recent_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_COMBO_PROFILE, "confidence REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_COMBO_PROFILE, "combo_score REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_COMBO_PROFILE, "risk_score REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_COMBO_PROFILE, "prob_adjust REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_COMBO_PROFILE, "gate_adjust REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_COMBO_PROFILE, "risk_penalty REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_COMBO_PROFILE, "updated_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_COMBO_PROFILE, "status TEXT");
        addColumn(db, TABLE_TEACHER_MATRIX_COMBO_PROFILE, "summary_text TEXT");

        addColumn(db, TABLE_TEACHER_MATRIX_REPORT, "task_key TEXT");
        addColumn(db, TABLE_TEACHER_MATRIX_REPORT, "segment_key TEXT");
        addColumn(db, TABLE_TEACHER_MATRIX_REPORT, "source_type TEXT");
        addColumn(db, TABLE_TEACHER_MATRIX_REPORT, "created_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_REPORT, "started_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_REPORT, "finished_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_REPORT, "cost_ms INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_REPORT, "window_size INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_REPORT, "target_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_REPORT, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_REPORT, "positive_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_REPORT, "negative_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_REPORT, "profile_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_REPORT, "combo_profile_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_REPORT, "strong_profile_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_REPORT, "risk_profile_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_REPORT, "applied_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_REPORT, "reason_sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_REPORT, "reason_profile_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_REPORT, "avg_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_REPORT, "avg_confidence REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_REPORT, "avg_prob_adjust REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_REPORT, "avg_gate_adjust REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_REPORT, "avg_reason_edge REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_MATRIX_REPORT, "status TEXT");
        addColumn(db, TABLE_TEACHER_MATRIX_REPORT, "summary_text TEXT");
        addColumn(db, TABLE_TEACHER_MATRIX_REPORT, "error_text TEXT");
    }

    private void migrateReasonCausalColumns(SQLiteDatabase db) {
        addPreDrawReasonColumns(db);
        addReasonContrastColumns(db);
        addReasonCausalProfileColumns(db);
        addFalseStrongProfileColumns(db);
        addComboRoleProfileColumns(db);
        addElite45AuditColumns(db);
    }

    private void addPreDrawReasonColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "source_type TEXT");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "base_issue TEXT");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "target_issue TEXT");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "created_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "target_index INTEGER DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "number INTEGER DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "actual_number INTEGER DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "k3_mask INTEGER DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "label INTEGER DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "hit INTEGER DEFAULT 0");

        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "state_key TEXT");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "state_mode TEXT");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "trend_key TEXT");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "shape_key TEXT");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "drift_key TEXT");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "break_key TEXT");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "recovery_key TEXT");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "transition_key TEXT");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "relation_key TEXT");

        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "reason_fingerprint TEXT");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "reason_atom_text TEXT");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "reason_combo_key TEXT");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "valid_level TEXT");

        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "final_p REAL DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "dynamic_gate REAL DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "edge REAL DEFAULT 0");

        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "reason_score REAL DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "reason_edge REAL DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "hit_similarity REAL DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "miss_similarity REAL DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "contradiction_score REAL DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "causal_lift REAL DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "causal_confidence REAL DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "combo_conflict REAL DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "combo_role_diversity REAL DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "drift_risk REAL DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "break_risk REAL DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "recovery_score REAL DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "risk_score REAL DEFAULT 0");

        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "valid_reason_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "false_reason_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "forward_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "forward_hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "forward_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "wilson_score REAL DEFAULT 0");

        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "k3_role TEXT");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "main_reason_code TEXT");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "why_hit_text TEXT");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "why_miss_text TEXT");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "next_advice TEXT");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "summary_text TEXT");
        addColumn(db, TABLE_PRE_DRAW_REASON_SAMPLE, "saved INTEGER DEFAULT 0");
    }

    private void addReasonContrastColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_REASON_CONTRAST_PROFILE, "reason_fingerprint TEXT");
        addColumn(db, TABLE_REASON_CONTRAST_PROFILE, "state_key TEXT");
        addColumn(db, TABLE_REASON_CONTRAST_PROFILE, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_REASON_CONTRAST_PROFILE, "number INTEGER DEFAULT 0");
        addColumn(db, TABLE_REASON_CONTRAST_PROFILE, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_REASON_CONTRAST_PROFILE, "hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_REASON_CONTRAST_PROFILE, "miss_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_REASON_CONTRAST_PROFILE, "recent_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_REASON_CONTRAST_PROFILE, "recent_hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_REASON_CONTRAST_PROFILE, "recent_miss INTEGER DEFAULT 0");
        addColumn(db, TABLE_REASON_CONTRAST_PROFILE, "hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_REASON_CONTRAST_PROFILE, "miss_rate REAL DEFAULT 0");
        addColumn(db, TABLE_REASON_CONTRAST_PROFILE, "recent_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_REASON_CONTRAST_PROFILE, "reason_edge REAL DEFAULT 0");
        addColumn(db, TABLE_REASON_CONTRAST_PROFILE, "hit_similarity REAL DEFAULT 0");
        addColumn(db, TABLE_REASON_CONTRAST_PROFILE, "miss_similarity REAL DEFAULT 0");
        addColumn(db, TABLE_REASON_CONTRAST_PROFILE, "contradiction_score REAL DEFAULT 0");
        addColumn(db, TABLE_REASON_CONTRAST_PROFILE, "wilson_score REAL DEFAULT 0");
        addColumn(db, TABLE_REASON_CONTRAST_PROFILE, "confidence REAL DEFAULT 0");
        addColumn(db, TABLE_REASON_CONTRAST_PROFILE, "valid_level TEXT");
        addColumn(db, TABLE_REASON_CONTRAST_PROFILE, "why_text TEXT");
        addColumn(db, TABLE_REASON_CONTRAST_PROFILE, "summary_text TEXT");
        addColumn(db, TABLE_REASON_CONTRAST_PROFILE, "updated_at INTEGER DEFAULT 0");
    }

    private void addReasonCausalProfileColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_REASON_CAUSAL_PROFILE, "reason_atom TEXT");
        addColumn(db, TABLE_REASON_CAUSAL_PROFILE, "reason_combo_key TEXT");
        addColumn(db, TABLE_REASON_CAUSAL_PROFILE, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_REASON_CAUSAL_PROFILE, "hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_REASON_CAUSAL_PROFILE, "miss_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_REASON_CAUSAL_PROFILE, "forward_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_REASON_CAUSAL_PROFILE, "forward_hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_REASON_CAUSAL_PROFILE, "forward_miss INTEGER DEFAULT 0");
        addColumn(db, TABLE_REASON_CAUSAL_PROFILE, "hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_REASON_CAUSAL_PROFILE, "forward_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_REASON_CAUSAL_PROFILE, "baseline_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_REASON_CAUSAL_PROFILE, "causal_lift REAL DEFAULT 0");
        addColumn(db, TABLE_REASON_CAUSAL_PROFILE, "causal_confidence REAL DEFAULT 0");
        addColumn(db, TABLE_REASON_CAUSAL_PROFILE, "decay_score REAL DEFAULT 0");
        addColumn(db, TABLE_REASON_CAUSAL_PROFILE, "risk_score REAL DEFAULT 0");
        addColumn(db, TABLE_REASON_CAUSAL_PROFILE, "prob_adjust REAL DEFAULT 0");
        addColumn(db, TABLE_REASON_CAUSAL_PROFILE, "gate_adjust REAL DEFAULT 0");
        addColumn(db, TABLE_REASON_CAUSAL_PROFILE, "valid_level TEXT");
        addColumn(db, TABLE_REASON_CAUSAL_PROFILE, "summary_text TEXT");
        addColumn(db, TABLE_REASON_CAUSAL_PROFILE, "updated_at INTEGER DEFAULT 0");
    }

    private void addFalseStrongProfileColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_FALSE_STRONG_PROFILE, "state_key TEXT");
        addColumn(db, TABLE_FALSE_STRONG_PROFILE, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_FALSE_STRONG_PROFILE, "number INTEGER DEFAULT 0");
        addColumn(db, TABLE_FALSE_STRONG_PROFILE, "k3_mask INTEGER DEFAULT 0");
        addColumn(db, TABLE_FALSE_STRONG_PROFILE, "reason_fingerprint TEXT");
        addColumn(db, TABLE_FALSE_STRONG_PROFILE, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_FALSE_STRONG_PROFILE, "miss_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_FALSE_STRONG_PROFILE, "hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_FALSE_STRONG_PROFILE, "consecutive_miss INTEGER DEFAULT 0");
        addColumn(db, TABLE_FALSE_STRONG_PROFILE, "cooldown_until_index INTEGER DEFAULT 0");
        addColumn(db, TABLE_FALSE_STRONG_PROFILE, "cooldown_until_time INTEGER DEFAULT 0");
        addColumn(db, TABLE_FALSE_STRONG_PROFILE, "miss_rate REAL DEFAULT 0");
        addColumn(db, TABLE_FALSE_STRONG_PROFILE, "hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_FALSE_STRONG_PROFILE, "false_strong_score REAL DEFAULT 0");
        addColumn(db, TABLE_FALSE_STRONG_PROFILE, "risk_penalty REAL DEFAULT 0");
        addColumn(db, TABLE_FALSE_STRONG_PROFILE, "gate_boost REAL DEFAULT 0");
        addColumn(db, TABLE_FALSE_STRONG_PROFILE, "status TEXT");
        addColumn(db, TABLE_FALSE_STRONG_PROFILE, "summary_text TEXT");
        addColumn(db, TABLE_FALSE_STRONG_PROFILE, "updated_at INTEGER DEFAULT 0");
    }

    private void addComboRoleProfileColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_COMBO_ROLE_PROFILE, "state_key TEXT");
        addColumn(db, TABLE_COMBO_ROLE_PROFILE, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_COMBO_ROLE_PROFILE, "k3_mask INTEGER DEFAULT 0");
        addColumn(db, TABLE_COMBO_ROLE_PROFILE, "main_number INTEGER DEFAULT 0");
        addColumn(db, TABLE_COMBO_ROLE_PROFILE, "support_number INTEGER DEFAULT 0");
        addColumn(db, TABLE_COMBO_ROLE_PROFILE, "defense_number INTEGER DEFAULT 0");
        addColumn(db, TABLE_COMBO_ROLE_PROFILE, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_COMBO_ROLE_PROFILE, "hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_COMBO_ROLE_PROFILE, "main_hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_COMBO_ROLE_PROFILE, "support_hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_COMBO_ROLE_PROFILE, "defense_hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_COMBO_ROLE_PROFILE, "miss_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_COMBO_ROLE_PROFILE, "hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_COMBO_ROLE_PROFILE, "main_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_COMBO_ROLE_PROFILE, "support_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_COMBO_ROLE_PROFILE, "defense_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_COMBO_ROLE_PROFILE, "role_diversity REAL DEFAULT 0");
        addColumn(db, TABLE_COMBO_ROLE_PROFILE, "combo_conflict REAL DEFAULT 0");
        addColumn(db, TABLE_COMBO_ROLE_PROFILE, "role_score REAL DEFAULT 0");
        addColumn(db, TABLE_COMBO_ROLE_PROFILE, "confidence REAL DEFAULT 0");
        addColumn(db, TABLE_COMBO_ROLE_PROFILE, "status TEXT");
        addColumn(db, TABLE_COMBO_ROLE_PROFILE, "summary_text TEXT");
        addColumn(db, TABLE_COMBO_ROLE_PROFILE, "updated_at INTEGER DEFAULT 0");
    }

    private void addElite45AuditColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_ELITE45_AUDIT_LOG, "target_issue TEXT");
        addColumn(db, TABLE_ELITE45_AUDIT_LOG, "created_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_ELITE45_AUDIT_LOG, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_ELITE45_AUDIT_LOG, "n1 INTEGER DEFAULT 0");
        addColumn(db, TABLE_ELITE45_AUDIT_LOG, "n2 INTEGER DEFAULT 0");
        addColumn(db, TABLE_ELITE45_AUDIT_LOG, "n3 INTEGER DEFAULT 0");
        addColumn(db, TABLE_ELITE45_AUDIT_LOG, "k3_mask INTEGER DEFAULT 0");
        addColumn(db, TABLE_ELITE45_AUDIT_LOG, "passed INTEGER DEFAULT 0");
        addColumn(db, TABLE_ELITE45_AUDIT_LOG, "status TEXT");
        addColumn(db, TABLE_ELITE45_AUDIT_LOG, "block_code TEXT");
        addColumn(db, TABLE_ELITE45_AUDIT_LOG, "final_p REAL DEFAULT 0");
        addColumn(db, TABLE_ELITE45_AUDIT_LOG, "dynamic_gate REAL DEFAULT 0");
        addColumn(db, TABLE_ELITE45_AUDIT_LOG, "elite_gate REAL DEFAULT 0");
        addColumn(db, TABLE_ELITE45_AUDIT_LOG, "reason_edge REAL DEFAULT 0");
        addColumn(db, TABLE_ELITE45_AUDIT_LOG, "hit_similarity REAL DEFAULT 0");
        addColumn(db, TABLE_ELITE45_AUDIT_LOG, "miss_similarity REAL DEFAULT 0");
        addColumn(db, TABLE_ELITE45_AUDIT_LOG, "contradiction_score REAL DEFAULT 0");
        addColumn(db, TABLE_ELITE45_AUDIT_LOG, "combo_conflict REAL DEFAULT 0");
        addColumn(db, TABLE_ELITE45_AUDIT_LOG, "combo_role_diversity REAL DEFAULT 0");
        addColumn(db, TABLE_ELITE45_AUDIT_LOG, "drift_risk REAL DEFAULT 0");
        addColumn(db, TABLE_ELITE45_AUDIT_LOG, "break_risk REAL DEFAULT 0");
        addColumn(db, TABLE_ELITE45_AUDIT_LOG, "risk_score REAL DEFAULT 0");
        addColumn(db, TABLE_ELITE45_AUDIT_LOG, "valid_reason_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_ELITE45_AUDIT_LOG, "false_reason_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_ELITE45_AUDIT_LOG, "fingerprint_cooling INTEGER DEFAULT 0");
        addColumn(db, TABLE_ELITE45_AUDIT_LOG, "reason_level TEXT");
        addColumn(db, TABLE_ELITE45_AUDIT_LOG, "reason_fingerprint TEXT");
        addColumn(db, TABLE_ELITE45_AUDIT_LOG, "k3_role_summary TEXT");
        addColumn(db, TABLE_ELITE45_AUDIT_LOG, "summary_text TEXT");
    }

    private void migrateTeacherReplayAssistColumns(SQLiteDatabase db) {
        addReplayCaseColumns(db);
        addReplayCursorColumns(db);
        addReplayReportColumns(db);
        addReplayCounterfactualColumns(db);
        addReplayStabilityColumns(db);
        addAssistProfileColumns(db);
        addUiTextAndDiagColumns(db);
    }

    private void addReplayCaseColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_TEACHER_REPLAY_CASE, "source_type TEXT");
        addColumn(db, TABLE_TEACHER_REPLAY_CASE, "base_issue TEXT");
        addColumn(db, TABLE_TEACHER_REPLAY_CASE, "target_issue TEXT");
        addColumn(db, TABLE_TEACHER_REPLAY_CASE, "created_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_CASE, "target_index INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_CASE, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_CASE, "n1 INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_CASE, "n2 INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_CASE, "n3 INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_CASE, "k3_mask INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_CASE, "actual_number INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_CASE, "hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_CASE, "state_key TEXT");
        addColumn(db, TABLE_TEACHER_REPLAY_CASE, "state_mode TEXT");
        addColumn(db, TABLE_TEACHER_REPLAY_CASE, "shape_key TEXT");
        addColumn(db, TABLE_TEACHER_REPLAY_CASE, "trend_key TEXT");
        addColumn(db, TABLE_TEACHER_REPLAY_CASE, "final_p REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_CASE, "dynamic_gate REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_CASE, "edge REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_CASE, "lift REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_CASE, "confidence REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_CASE, "risk_score REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_CASE, "reason_type TEXT");
        addColumn(db, TABLE_TEACHER_REPLAY_CASE, "why_hit TEXT");
        addColumn(db, TABLE_TEACHER_REPLAY_CASE, "why_miss TEXT");
        addColumn(db, TABLE_TEACHER_REPLAY_CASE, "counterfactual_text TEXT");
        addColumn(db, TABLE_TEACHER_REPLAY_CASE, "stability_text TEXT");
        addColumn(db, TABLE_TEACHER_REPLAY_CASE, "summary_text TEXT");
        addColumn(db, TABLE_TEACHER_REPLAY_CASE, "saved INTEGER DEFAULT 0");
    }

    private void addReplayCursorColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_TEACHER_REPLAY_CURSOR, "task_type TEXT");
        addColumn(db, TABLE_TEACHER_REPLAY_CURSOR, "window_size INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_CURSOR, "segment_start_index INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_CURSOR, "segment_end_index INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_CURSOR, "next_target_index INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_CURSOR, "last_target_issue TEXT");
        addColumn(db, TABLE_TEACHER_REPLAY_CURSOR, "used_targets TEXT");
        addColumn(db, TABLE_TEACHER_REPLAY_CURSOR, "status TEXT");
        addColumn(db, TABLE_TEACHER_REPLAY_CURSOR, "progress TEXT");
        addColumn(db, TABLE_TEACHER_REPLAY_CURSOR, "updated_at INTEGER DEFAULT 0");
    }

    private void addReplayReportColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_TEACHER_REPLAY_REPORT, "task_key TEXT");
        addColumn(db, TABLE_TEACHER_REPLAY_REPORT, "segment_key TEXT");
        addColumn(db, TABLE_TEACHER_REPLAY_REPORT, "status TEXT");
        addColumn(db, TABLE_TEACHER_REPLAY_REPORT, "created_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_REPORT, "started_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_REPORT, "finished_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_REPORT, "cost_ms INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_REPORT, "target_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_REPORT, "case_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_REPORT, "hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_REPORT, "miss_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_REPORT, "counterfactual_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_REPORT, "stability_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_REPORT, "teacher_matrix_feed_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_REPORT, "reason_feed_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_REPORT, "summary_text TEXT");
        addColumn(db, TABLE_TEACHER_REPLAY_REPORT, "error_text TEXT");
    }

    private void addReplayCounterfactualColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_TEACHER_REPLAY_COUNTERFACTUAL, "target_issue TEXT");
        addColumn(db, TABLE_TEACHER_REPLAY_COUNTERFACTUAL, "created_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_COUNTERFACTUAL, "chosen_position INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_COUNTERFACTUAL, "chosen_k3_mask INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_COUNTERFACTUAL, "better_position INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_COUNTERFACTUAL, "better_k3_mask INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_COUNTERFACTUAL, "actual_number INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_COUNTERFACTUAL, "miss_type TEXT");
        addColumn(db, TABLE_TEACHER_REPLAY_COUNTERFACTUAL, "summary_text TEXT");
    }

    private void addReplayStabilityColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_TEACHER_REPLAY_STABILITY, "state_key TEXT");
        addColumn(db, TABLE_TEACHER_REPLAY_STABILITY, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_STABILITY, "k3_mask INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_STABILITY, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_STABILITY, "hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_STABILITY, "hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_STABILITY, "stability_score REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_STABILITY, "bad_rate REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_STABILITY, "updated_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_REPLAY_STABILITY, "summary_text TEXT");
    }

    private void addAssistProfileColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_TEACHER_ASSIST_PROFILE, "state_key TEXT");
        addColumn(db, TABLE_TEACHER_ASSIST_PROFILE, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_ASSIST_PROFILE, "k3_mask INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_ASSIST_PROFILE, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_ASSIST_PROFILE, "hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_ASSIST_PROFILE, "hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_ASSIST_PROFILE, "recent_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_ASSIST_PROFILE, "recent_hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_ASSIST_PROFILE, "recent_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_ASSIST_PROFILE, "lift REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_ASSIST_PROFILE, "confidence REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_ASSIST_PROFILE, "conflict_rate REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_ASSIST_PROFILE, "prob_adjust REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_ASSIST_PROFILE, "gate_adjust REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_ASSIST_PROFILE, "sort_adjust REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_ASSIST_PROFILE, "status TEXT");
        addColumn(db, TABLE_TEACHER_ASSIST_PROFILE, "summary_text TEXT");
        addColumn(db, TABLE_TEACHER_ASSIST_PROFILE, "updated_at INTEGER DEFAULT 0");
    }

    private void addUiTextAndDiagColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_UI_TEXT_STATE, "expanded INTEGER DEFAULT 0");
        addColumn(db, TABLE_UI_TEXT_STATE, "scroll_y INTEGER DEFAULT 0");
        addColumn(db, TABLE_UI_TEXT_STATE, "updated_at INTEGER DEFAULT 0");

        addColumn(db, TABLE_DIAG_LOG, "created_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_DIAG_LOG, "level TEXT");
        addColumn(db, TABLE_DIAG_LOG, "module TEXT");
        addColumn(db, TABLE_DIAG_LOG, "code TEXT");
        addColumn(db, TABLE_DIAG_LOG, "message TEXT");
        addColumn(db, TABLE_DIAG_LOG, "detail TEXT");
    }

    private void migrateV20Columns(SQLiteDatabase db) {
        addV20PreDrawFeatureColumns(db);
        addV20PreDrawNumberSampleColumns(db);
        addV20CandidateScanColumns(db);
        addV20CandidateAuditColumns(db);
        addV20CausalFactorColumns(db);
        addV20PositionCauseColumns(db);
        addV20TeacherNumberColumns(db);
        addV20CounterfactualRankColumns(db);
        addV20ThirdGuardColumns(db);
        addV20HealthColumns(db);
        addV20ShadowHealthColumns(db);
        addV20FalseStrongFreezeColumns(db);
        addV20SeedCauseColumns(db);
        addV20DynamicCauseChainColumns(db);
        addV20DecisionAuditColumns(db);
        addV20RealTradeLedgerColumns(db);
        addV20ClosedLoopSchemaColumns(db);
    }

    private void addV20PreDrawFeatureColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_PRE_DRAW_FEATURE, "base_issue TEXT");
        addColumn(db, TABLE_PRE_DRAW_FEATURE, "target_issue TEXT");
        addColumn(db, TABLE_PRE_DRAW_FEATURE, "created_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_FEATURE, "history_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_FEATURE, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_FEATURE, "window_12_ok INTEGER DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_FEATURE, "window_24_ok INTEGER DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_FEATURE, "window_48_ok INTEGER DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_FEATURE, "window_96_ok INTEGER DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_FEATURE, "window_180_ok INTEGER DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_FEATURE, "window_360_ok INTEGER DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_FEATURE, "window_720_ok INTEGER DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_FEATURE, "exact_state_key TEXT");
        addColumn(db, TABLE_PRE_DRAW_FEATURE, "relaxed_state_key TEXT");
        addColumn(db, TABLE_PRE_DRAW_FEATURE, "hot_cold_slope REAL DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_FEATURE, "miss_bucket INTEGER DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_FEATURE, "density_score REAL DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_FEATURE, "repeat_score REAL DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_FEATURE, "odd_even_bias REAL DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_FEATURE, "big_small_bias REAL DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_FEATURE, "neighbor_score REAL DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_FEATURE, "same_tail_score REAL DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_FEATURE, "position_crowd_score REAL DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_FEATURE, "cross_position_sync REAL DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_FEATURE, "recent_lose_chain INTEGER DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_FEATURE, "timing_offset INTEGER DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_FEATURE, "timing_bucket TEXT");
        addColumn(db, TABLE_PRE_DRAW_FEATURE, "trend_state TEXT");
        addColumn(db, TABLE_PRE_DRAW_FEATURE, "shape_state TEXT");
        addColumn(db, TABLE_PRE_DRAW_FEATURE, "entropy_state TEXT");
        addColumn(db, TABLE_PRE_DRAW_FEATURE, "drift_state TEXT");
        addColumn(db, TABLE_PRE_DRAW_FEATURE, "flow_state TEXT");
        addColumn(db, TABLE_PRE_DRAW_FEATURE, "cycle_state TEXT");
        addColumn(db, TABLE_PRE_DRAW_FEATURE, "rank_mode TEXT");
        addColumn(db, TABLE_PRE_DRAW_FEATURE, "board_risk REAL DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_FEATURE, "state_key TEXT");
        addColumn(db, TABLE_PRE_DRAW_FEATURE, "source_key TEXT");
        addColumn(db, TABLE_PRE_DRAW_FEATURE, "reason_atoms TEXT");
        addColumn(db, TABLE_PRE_DRAW_FEATURE, "feature_json TEXT");
        addColumn(db, TABLE_PRE_DRAW_FEATURE, "summary_text TEXT");
    }

    private void addV20PreDrawNumberSampleColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_PRE_DRAW_NUMBER_SAMPLE, "base_issue TEXT");
        addColumn(db, TABLE_PRE_DRAW_NUMBER_SAMPLE, "target_issue TEXT");
        addColumn(db, TABLE_PRE_DRAW_NUMBER_SAMPLE, "created_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_NUMBER_SAMPLE, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_NUMBER_SAMPLE, "number INTEGER DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_NUMBER_SAMPLE, "actual_number INTEGER DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_NUMBER_SAMPLE, "label INTEGER DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_NUMBER_SAMPLE, "state_key TEXT");
        addColumn(db, TABLE_PRE_DRAW_NUMBER_SAMPLE, "trend_state TEXT");
        addColumn(db, TABLE_PRE_DRAW_NUMBER_SAMPLE, "shape_state TEXT");
        addColumn(db, TABLE_PRE_DRAW_NUMBER_SAMPLE, "life_state TEXT");
        addColumn(db, TABLE_PRE_DRAW_NUMBER_SAMPLE, "miss_gap INTEGER DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_NUMBER_SAMPLE, "hot_score REAL DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_NUMBER_SAMPLE, "cold_score REAL DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_NUMBER_SAMPLE, "cycle_score REAL DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_NUMBER_SAMPLE, "migration_score REAL DEFAULT 0");
        addColumn(db, TABLE_PRE_DRAW_NUMBER_SAMPLE, "teacher_key TEXT");
        addColumn(db, TABLE_PRE_DRAW_NUMBER_SAMPLE, "why_text TEXT");
    }

    private void addV20CandidateScanColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_FULL_CANDIDATE_SCAN, "base_issue TEXT");
        addColumn(db, TABLE_FULL_CANDIDATE_SCAN, "target_issue TEXT");
        addColumn(db, TABLE_FULL_CANDIDATE_SCAN, "created_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_FULL_CANDIDATE_SCAN, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_FULL_CANDIDATE_SCAN, "n1 INTEGER DEFAULT 0");
        addColumn(db, TABLE_FULL_CANDIDATE_SCAN, "n2 INTEGER DEFAULT 0");
        addColumn(db, TABLE_FULL_CANDIDATE_SCAN, "n3 INTEGER DEFAULT 0");
        addColumn(db, TABLE_FULL_CANDIDATE_SCAN, "k3_mask INTEGER DEFAULT 0");
        addColumn(db, TABLE_FULL_CANDIDATE_SCAN, "raw_rank INTEGER DEFAULT 0");
        addColumn(db, TABLE_FULL_CANDIDATE_SCAN, "rank_no INTEGER DEFAULT 0");
        addColumn(db, TABLE_FULL_CANDIDATE_SCAN, "rank_group TEXT");
        addColumn(db, TABLE_FULL_CANDIDATE_SCAN, "raw_score REAL DEFAULT 0");
        addColumn(db, TABLE_FULL_CANDIDATE_SCAN, "base_k3_p REAL DEFAULT 0");
        addColumn(db, TABLE_FULL_CANDIDATE_SCAN, "final_p REAL DEFAULT 0");
        addColumn(db, TABLE_FULL_CANDIDATE_SCAN, "dynamic_gate REAL DEFAULT 0");
        addColumn(db, TABLE_FULL_CANDIDATE_SCAN, "edge REAL DEFAULT 0");
        addColumn(db, TABLE_FULL_CANDIDATE_SCAN, "margin REAL DEFAULT 0");
        addColumn(db, TABLE_FULL_CANDIDATE_SCAN, "state_key TEXT");
        addColumn(db, TABLE_FULL_CANDIDATE_SCAN, "source_key TEXT");
        addColumn(db, TABLE_FULL_CANDIDATE_SCAN, "rule_key TEXT");
        addColumn(db, TABLE_FULL_CANDIDATE_SCAN, "same_class_key TEXT");
        addColumn(db, TABLE_FULL_CANDIDATE_SCAN, "reason_atoms TEXT");
        addColumn(db, TABLE_FULL_CANDIDATE_SCAN, "source_votes TEXT");
        addColumn(db, TABLE_FULL_CANDIDATE_SCAN, "risk_tags TEXT");
        addColumn(db, TABLE_FULL_CANDIDATE_SCAN, "grade TEXT");
        addColumn(db, TABLE_FULL_CANDIDATE_SCAN, "action TEXT");
        addColumn(db, TABLE_FULL_CANDIDATE_SCAN, "hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_FULL_CANDIDATE_SCAN, "actual_number INTEGER DEFAULT 0");
        addColumn(db, TABLE_FULL_CANDIDATE_SCAN, "settled INTEGER DEFAULT 0");
        addColumn(db, TABLE_FULL_CANDIDATE_SCAN, "learned INTEGER DEFAULT 0");
    }

    private void addV20CandidateAuditColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_CANDIDATE_AUDIT, "base_issue TEXT");
        addColumn(db, TABLE_CANDIDATE_AUDIT, "target_issue TEXT");
        addColumn(db, TABLE_CANDIDATE_AUDIT, "created_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_CANDIDATE_AUDIT, "candidate_key TEXT");
        addColumn(db, TABLE_CANDIDATE_AUDIT, "rank_no INTEGER DEFAULT 0");
        addColumn(db, TABLE_CANDIDATE_AUDIT, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_CANDIDATE_AUDIT, "k3_mask INTEGER DEFAULT 0");
        addColumn(db, TABLE_CANDIDATE_AUDIT, "grade TEXT");
        addColumn(db, TABLE_CANDIDATE_AUDIT, "action TEXT");
        addColumn(db, TABLE_CANDIDATE_AUDIT, "hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_CANDIDATE_AUDIT, "actual_number INTEGER DEFAULT 0");
        addColumn(db, TABLE_CANDIDATE_AUDIT, "actual_rank INTEGER DEFAULT 0");
        addColumn(db, TABLE_CANDIDATE_AUDIT, "final_p REAL DEFAULT 0");
        addColumn(db, TABLE_CANDIDATE_AUDIT, "dynamic_gate REAL DEFAULT 0");
        addColumn(db, TABLE_CANDIDATE_AUDIT, "final_cause_score REAL DEFAULT 0");
        addColumn(db, TABLE_CANDIDATE_AUDIT, "why_hit TEXT");
        addColumn(db, TABLE_CANDIDATE_AUDIT, "why_miss TEXT");
        addColumn(db, TABLE_CANDIDATE_AUDIT, "why_hold TEXT");
    }

    private void addV20CausalFactorColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_CAUSAL_FACTOR_MATRIX, "state_key TEXT");
        addColumn(db, TABLE_CAUSAL_FACTOR_MATRIX, "reason_atom TEXT");
        addColumn(db, TABLE_CAUSAL_FACTOR_MATRIX, "reason_combo_key TEXT");
        addColumn(db, TABLE_CAUSAL_FACTOR_MATRIX, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_CAUSAL_FACTOR_MATRIX, "with_cause_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_CAUSAL_FACTOR_MATRIX, "with_cause_hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_CAUSAL_FACTOR_MATRIX, "without_cause_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_CAUSAL_FACTOR_MATRIX, "without_cause_hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_CAUSAL_FACTOR_MATRIX, "hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_CAUSAL_FACTOR_MATRIX, "contrast_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_CAUSAL_FACTOR_MATRIX, "causal_edge REAL DEFAULT 0");
        addColumn(db, TABLE_CAUSAL_FACTOR_MATRIX, "lcb REAL DEFAULT 0");
        addColumn(db, TABLE_CAUSAL_FACTOR_MATRIX, "recent30_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_CAUSAL_FACTOR_MATRIX, "recent60_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_CAUSAL_FACTOR_MATRIX, "decay_risk REAL DEFAULT 0");
        addColumn(db, TABLE_CAUSAL_FACTOR_MATRIX, "valid_level TEXT");
        addColumn(db, TABLE_CAUSAL_FACTOR_MATRIX, "updated_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_CAUSAL_FACTOR_MATRIX, "summary_text TEXT");
    }

    private void addV20PositionCauseColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_POSITION_CAUSE_MATRIX, "state_key TEXT");
        addColumn(db, TABLE_POSITION_CAUSE_MATRIX, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_POSITION_CAUSE_MATRIX, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_POSITION_CAUSE_MATRIX, "hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_POSITION_CAUSE_MATRIX, "miss_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_POSITION_CAUSE_MATRIX, "recent30_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_POSITION_CAUSE_MATRIX, "recent30_hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_POSITION_CAUSE_MATRIX, "recent60_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_POSITION_CAUSE_MATRIX, "recent60_hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_POSITION_CAUSE_MATRIX, "recent120_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_POSITION_CAUSE_MATRIX, "recent120_hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_POSITION_CAUSE_MATRIX, "hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_POSITION_CAUSE_MATRIX, "recent_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_POSITION_CAUSE_MATRIX, "shadow_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_POSITION_CAUSE_MATRIX, "max_lose INTEGER DEFAULT 0");
        addColumn(db, TABLE_POSITION_CAUSE_MATRIX, "current_lose INTEGER DEFAULT 0");
        addColumn(db, TABLE_POSITION_CAUSE_MATRIX, "false_strong_rate REAL DEFAULT 0");
        addColumn(db, TABLE_POSITION_CAUSE_MATRIX, "recovery_score REAL DEFAULT 0");
        addColumn(db, TABLE_POSITION_CAUSE_MATRIX, "cycle_health REAL DEFAULT 0");
        addColumn(db, TABLE_POSITION_CAUSE_MATRIX, "position_edge REAL DEFAULT 0");
        addColumn(db, TABLE_POSITION_CAUSE_MATRIX, "position_risk REAL DEFAULT 0");
        addColumn(db, TABLE_POSITION_CAUSE_MATRIX, "position_healthy INTEGER DEFAULT 0");
        addColumn(db, TABLE_POSITION_CAUSE_MATRIX, "freeze_until_issue TEXT");
        addColumn(db, TABLE_POSITION_CAUSE_MATRIX, "status TEXT");
        addColumn(db, TABLE_POSITION_CAUSE_MATRIX, "updated_at INTEGER DEFAULT 0");
    }

    private void addV20TeacherNumberColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_TEACHER_NUMBER_MATRIX, "state_key TEXT");
        addColumn(db, TABLE_TEACHER_NUMBER_MATRIX, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_NUMBER_MATRIX, "number INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_NUMBER_MATRIX, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_NUMBER_MATRIX, "positive_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_NUMBER_MATRIX, "negative_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_NUMBER_MATRIX, "recent30_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_NUMBER_MATRIX, "recent30_positive INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_NUMBER_MATRIX, "recent60_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_NUMBER_MATRIX, "recent60_positive INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_NUMBER_MATRIX, "hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_NUMBER_MATRIX, "recent_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_NUMBER_MATRIX, "teacher_edge REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_NUMBER_MATRIX, "confidence REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_NUMBER_MATRIX, "support_pass INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_NUMBER_MATRIX, "risk_score REAL DEFAULT 0");
        addColumn(db, TABLE_TEACHER_NUMBER_MATRIX, "life_state TEXT");
        addColumn(db, TABLE_TEACHER_NUMBER_MATRIX, "updated_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_TEACHER_NUMBER_MATRIX, "why_text TEXT");
    }

    private void addV20CounterfactualRankColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_COUNTERFACTUAL_RANK, "target_issue TEXT");
        addColumn(db, TABLE_COUNTERFACTUAL_RANK, "created_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_COUNTERFACTUAL_RANK, "state_key TEXT");
        addColumn(db, TABLE_COUNTERFACTUAL_RANK, "rank_no INTEGER DEFAULT 0");
        addColumn(db, TABLE_COUNTERFACTUAL_RANK, "rank_group TEXT");
        addColumn(db, TABLE_COUNTERFACTUAL_RANK, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_COUNTERFACTUAL_RANK, "k3_mask INTEGER DEFAULT 0");
        addColumn(db, TABLE_COUNTERFACTUAL_RANK, "hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_COUNTERFACTUAL_RANK, "actual_number INTEGER DEFAULT 0");
        addColumn(db, TABLE_COUNTERFACTUAL_RANK, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_COUNTERFACTUAL_RANK, "hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_COUNTERFACTUAL_RANK, "hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_COUNTERFACTUAL_RANK, "rank_counter_edge REAL DEFAULT 0");
        addColumn(db, TABLE_COUNTERFACTUAL_RANK, "false_strong_risk REAL DEFAULT 0");
        addColumn(db, TABLE_COUNTERFACTUAL_RANK, "rank4to6_overtake_risk REAL DEFAULT 0");
        addColumn(db, TABLE_COUNTERFACTUAL_RANK, "rank_pass INTEGER DEFAULT 0");
        addColumn(db, TABLE_COUNTERFACTUAL_RANK, "truth_mode TEXT");
        addColumn(db, TABLE_COUNTERFACTUAL_RANK, "summary_text TEXT");
    }

    private void addV20ThirdGuardColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_THIRD_GUARD_MATRIX, "state_key TEXT");
        addColumn(db, TABLE_THIRD_GUARD_MATRIX, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_THIRD_GUARD_MATRIX, "main_mask INTEGER DEFAULT 0");
        addColumn(db, TABLE_THIRD_GUARD_MATRIX, "guard_number INTEGER DEFAULT 0");
        addColumn(db, TABLE_THIRD_GUARD_MATRIX, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_THIRD_GUARD_MATRIX, "save_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_THIRD_GUARD_MATRIX, "drag_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_THIRD_GUARD_MATRIX, "hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_THIRD_GUARD_MATRIX, "save_rate REAL DEFAULT 0");
        addColumn(db, TABLE_THIRD_GUARD_MATRIX, "drag_rate REAL DEFAULT 0");
        addColumn(db, TABLE_THIRD_GUARD_MATRIX, "third_guard_net REAL DEFAULT 0");
        addColumn(db, TABLE_THIRD_GUARD_MATRIX, "third_guard_pass INTEGER DEFAULT 0");
        addColumn(db, TABLE_THIRD_GUARD_MATRIX, "guard_role TEXT");
        addColumn(db, TABLE_THIRD_GUARD_MATRIX, "risk_score REAL DEFAULT 0");
        addColumn(db, TABLE_THIRD_GUARD_MATRIX, "updated_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_THIRD_GUARD_MATRIX, "summary_text TEXT");
    }

    private void addV20HealthColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_RULE_HEALTH_MATRIX, "state_key TEXT");
        addColumn(db, TABLE_RULE_HEALTH_MATRIX, "rule_name TEXT");
        addColumn(db, TABLE_RULE_HEALTH_MATRIX, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_RULE_HEALTH_MATRIX, "hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_RULE_HEALTH_MATRIX, "miss_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_RULE_HEALTH_MATRIX, "hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_RULE_HEALTH_MATRIX, "recent_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_RULE_HEALTH_MATRIX, "max_lose INTEGER DEFAULT 0");
        addColumn(db, TABLE_RULE_HEALTH_MATRIX, "current_lose INTEGER DEFAULT 0");
        addColumn(db, TABLE_RULE_HEALTH_MATRIX, "edge_decay_risk REAL DEFAULT 0");
        addColumn(db, TABLE_RULE_HEALTH_MATRIX, "is_active INTEGER DEFAULT 0");
        addColumn(db, TABLE_RULE_HEALTH_MATRIX, "is_frozen INTEGER DEFAULT 0");
        addColumn(db, TABLE_RULE_HEALTH_MATRIX, "freeze_until_issue TEXT");
        addColumn(db, TABLE_RULE_HEALTH_MATRIX, "updated_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_RULE_HEALTH_MATRIX, "summary_text TEXT");

        addColumn(db, TABLE_SOURCE_HEALTH_MATRIX, "state_key TEXT");
        addColumn(db, TABLE_SOURCE_HEALTH_MATRIX, "source_name TEXT");
        addColumn(db, TABLE_SOURCE_HEALTH_MATRIX, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_SOURCE_HEALTH_MATRIX, "hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_SOURCE_HEALTH_MATRIX, "miss_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_SOURCE_HEALTH_MATRIX, "hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_SOURCE_HEALTH_MATRIX, "recent_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_SOURCE_HEALTH_MATRIX, "source_edge REAL DEFAULT 0");
        addColumn(db, TABLE_SOURCE_HEALTH_MATRIX, "source_healthy INTEGER DEFAULT 0");
        addColumn(db, TABLE_SOURCE_HEALTH_MATRIX, "is_frozen INTEGER DEFAULT 0");
        addColumn(db, TABLE_SOURCE_HEALTH_MATRIX, "updated_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_SOURCE_HEALTH_MATRIX, "summary_text TEXT");

        addColumn(db, TABLE_STATE_HEALTH_MATRIX, "state_key TEXT");
        addColumn(db, TABLE_STATE_HEALTH_MATRIX, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_STATE_HEALTH_MATRIX, "hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_STATE_HEALTH_MATRIX, "miss_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_STATE_HEALTH_MATRIX, "hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_STATE_HEALTH_MATRIX, "recent_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_STATE_HEALTH_MATRIX, "state_edge REAL DEFAULT 0");
        addColumn(db, TABLE_STATE_HEALTH_MATRIX, "state_healthy INTEGER DEFAULT 0");
        addColumn(db, TABLE_STATE_HEALTH_MATRIX, "max_lose INTEGER DEFAULT 0");
        addColumn(db, TABLE_STATE_HEALTH_MATRIX, "current_lose INTEGER DEFAULT 0");
        addColumn(db, TABLE_STATE_HEALTH_MATRIX, "is_frozen INTEGER DEFAULT 0");
        addColumn(db, TABLE_STATE_HEALTH_MATRIX, "updated_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_STATE_HEALTH_MATRIX, "summary_text TEXT");
    }

    private void addV20ShadowHealthColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_SHADOW_HEALTH_GATE, "shadow_key_raw TEXT");
        addColumn(db, TABLE_SHADOW_HEALTH_GATE, "grade TEXT");
        addColumn(db, TABLE_SHADOW_HEALTH_GATE, "state_key TEXT");
        addColumn(db, TABLE_SHADOW_HEALTH_GATE, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_SHADOW_HEALTH_GATE, "sample30 INTEGER DEFAULT 0");
        addColumn(db, TABLE_SHADOW_HEALTH_GATE, "hit30 INTEGER DEFAULT 0");
        addColumn(db, TABLE_SHADOW_HEALTH_GATE, "sample60 INTEGER DEFAULT 0");
        addColumn(db, TABLE_SHADOW_HEALTH_GATE, "hit60 INTEGER DEFAULT 0");
        addColumn(db, TABLE_SHADOW_HEALTH_GATE, "sample120 INTEGER DEFAULT 0");
        addColumn(db, TABLE_SHADOW_HEALTH_GATE, "hit120 INTEGER DEFAULT 0");
        addColumn(db, TABLE_SHADOW_HEALTH_GATE, "hit_rate30 REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW_HEALTH_GATE, "hit_rate60 REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW_HEALTH_GATE, "hit_rate120 REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW_HEALTH_GATE, "max_shadow_lose INTEGER DEFAULT 0");
        addColumn(db, TABLE_SHADOW_HEALTH_GATE, "current_shadow_lose INTEGER DEFAULT 0");
        addColumn(db, TABLE_SHADOW_HEALTH_GATE, "decay_risk REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW_HEALTH_GATE, "shadow_health_score REAL DEFAULT 0");
        addColumn(db, TABLE_SHADOW_HEALTH_GATE, "promotion_pass INTEGER DEFAULT 0");
        addColumn(db, TABLE_SHADOW_HEALTH_GATE, "updated_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_SHADOW_HEALTH_GATE, "summary_text TEXT");
    }

    private void addV20FalseStrongFreezeColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_FALSE_STRONG_FREEZE, "state_key TEXT");
        addColumn(db, TABLE_FALSE_STRONG_FREEZE, "source_key TEXT");
        addColumn(db, TABLE_FALSE_STRONG_FREEZE, "rule_key TEXT");
        addColumn(db, TABLE_FALSE_STRONG_FREEZE, "same_class_key TEXT");
        addColumn(db, TABLE_FALSE_STRONG_FREEZE, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_FALSE_STRONG_FREEZE, "rank_mode TEXT");
        addColumn(db, TABLE_FALSE_STRONG_FREEZE, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_FALSE_STRONG_FREEZE, "hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_FALSE_STRONG_FREEZE, "miss_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_FALSE_STRONG_FREEZE, "consecutive_miss INTEGER DEFAULT 0");
        addColumn(db, TABLE_FALSE_STRONG_FREEZE, "recent_false_strong_rate REAL DEFAULT 0");
        addColumn(db, TABLE_FALSE_STRONG_FREEZE, "false_strong_risk REAL DEFAULT 0");
        addColumn(db, TABLE_FALSE_STRONG_FREEZE, "freeze_level INTEGER DEFAULT 0");
        addColumn(db, TABLE_FALSE_STRONG_FREEZE, "freeze_until_issue TEXT");
        addColumn(db, TABLE_FALSE_STRONG_FREEZE, "is_frozen INTEGER DEFAULT 0");
        addColumn(db, TABLE_FALSE_STRONG_FREEZE, "updated_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_FALSE_STRONG_FREEZE, "summary_text TEXT");
    }

    private void addV20SeedCauseColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_SEED_CAUSE_EXPANSION, "seed_source_key TEXT");
        addColumn(db, TABLE_SEED_CAUSE_EXPANSION, "state_key TEXT");
        addColumn(db, TABLE_SEED_CAUSE_EXPANSION, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_SEED_CAUSE_EXPANSION, "k3_mask INTEGER DEFAULT 0");
        addColumn(db, TABLE_SEED_CAUSE_EXPANSION, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_SEED_CAUSE_EXPANSION, "hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_SEED_CAUSE_EXPANSION, "miss_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_SEED_CAUSE_EXPANSION, "hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_SEED_CAUSE_EXPANSION, "purity_score REAL DEFAULT 0");
        addColumn(db, TABLE_SEED_CAUSE_EXPANSION, "seed_similarity REAL DEFAULT 0");
        addColumn(db, TABLE_SEED_CAUSE_EXPANSION, "false_seed_similarity REAL DEFAULT 0");
        addColumn(db, TABLE_SEED_CAUSE_EXPANSION, "expansion_pass INTEGER DEFAULT 0");
        addColumn(db, TABLE_SEED_CAUSE_EXPANSION, "updated_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_SEED_CAUSE_EXPANSION, "summary_text TEXT");
    }

    private void addV20DynamicCauseChainColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "base_issue TEXT");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "target_issue TEXT");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "created_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "candidate_key TEXT");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "n1 INTEGER DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "n2 INTEGER DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "n3 INTEGER DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "k3_mask INTEGER DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "state_key TEXT");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "source_key TEXT");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "rule_key TEXT");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "same_class_key TEXT");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "grade TEXT");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "final_cause_score REAL DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "support_score REAL DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "counter_score REAL DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "confidence REAL DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "sample_trust REAL DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "recent_effectiveness REAL DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "action_suggestion TEXT");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "position_edge REAL DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "number_teacher_edge REAL DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "combo_role_edge REAL DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "third_guard_edge REAL DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "rank_counterfactual_edge REAL DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "source_health_edge REAL DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "state_health_edge REAL DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "cycle_health_edge REAL DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "seed_similarity REAL DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "shadow_health REAL DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "false_strong_risk REAL DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "rule_decay_risk REAL DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "same_class_lose_risk REAL DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "guard_drag_risk REAL DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "position_wrong_risk REAL DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "position_healthy INTEGER DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "number_teacher_support INTEGER DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "rank_pass INTEGER DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "third_guard_pass INTEGER DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "shadow_pass INTEGER DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "false_strong_frozen INTEGER DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "same_class_frozen INTEGER DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "rule_active INTEGER DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "source_healthy INTEGER DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "state_healthy INTEGER DEFAULT 0");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "evidence_json TEXT");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "counter_evidence_json TEXT");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "risk_tags TEXT");
        addColumn(db, TABLE_DYNAMIC_CAUSE_CHAIN, "reason_text TEXT");
    }

    private void addV20DecisionAuditColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_DECISION_AUDIT, "base_issue TEXT");
        addColumn(db, TABLE_DECISION_AUDIT, "target_issue TEXT");
        addColumn(db, TABLE_DECISION_AUDIT, "created_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_DECISION_AUDIT, "candidate_key TEXT");
        addColumn(db, TABLE_DECISION_AUDIT, "chain_key TEXT");
        addColumn(db, TABLE_DECISION_AUDIT, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_DECISION_AUDIT, "n1 INTEGER DEFAULT 0");
        addColumn(db, TABLE_DECISION_AUDIT, "n2 INTEGER DEFAULT 0");
        addColumn(db, TABLE_DECISION_AUDIT, "n3 INTEGER DEFAULT 0");
        addColumn(db, TABLE_DECISION_AUDIT, "k3_mask INTEGER DEFAULT 0");
        addColumn(db, TABLE_DECISION_AUDIT, "grade TEXT");
        addColumn(db, TABLE_DECISION_AUDIT, "action TEXT");
        addColumn(db, TABLE_DECISION_AUDIT, "final_p REAL DEFAULT 0");
        addColumn(db, TABLE_DECISION_AUDIT, "dynamic_gate REAL DEFAULT 0");
        addColumn(db, TABLE_DECISION_AUDIT, "final_cause_score REAL DEFAULT 0");
        addColumn(db, TABLE_DECISION_AUDIT, "decision_pass INTEGER DEFAULT 0");
        addColumn(db, TABLE_DECISION_AUDIT, "block_code TEXT");
        addColumn(db, TABLE_DECISION_AUDIT, "block_reason TEXT");
        addColumn(db, TABLE_DECISION_AUDIT, "decision_reason TEXT");
        addColumn(db, TABLE_DECISION_AUDIT, "settled INTEGER DEFAULT 0");
        addColumn(db, TABLE_DECISION_AUDIT, "result TEXT DEFAULT 'WAIT'");
        addColumn(db, TABLE_DECISION_AUDIT, "hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_DECISION_AUDIT, "actual_number INTEGER DEFAULT 0");
        addColumn(db, TABLE_DECISION_AUDIT, "pnl REAL DEFAULT 0");
    }

    private void addV20RealTradeLedgerColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_REAL_TRADE_LEDGER, "base_issue TEXT");
        addColumn(db, TABLE_REAL_TRADE_LEDGER, "target_issue TEXT");
        addColumn(db, TABLE_REAL_TRADE_LEDGER, "created_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_REAL_TRADE_LEDGER, "settled_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_REAL_TRADE_LEDGER, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_REAL_TRADE_LEDGER, "n1 INTEGER DEFAULT 0");
        addColumn(db, TABLE_REAL_TRADE_LEDGER, "n2 INTEGER DEFAULT 0");
        addColumn(db, TABLE_REAL_TRADE_LEDGER, "n3 INTEGER DEFAULT 0");
        addColumn(db, TABLE_REAL_TRADE_LEDGER, "k3_mask INTEGER DEFAULT 0");
        addColumn(db, TABLE_REAL_TRADE_LEDGER, "grade TEXT");
        addColumn(db, TABLE_REAL_TRADE_LEDGER, "action TEXT");
        addColumn(db, TABLE_REAL_TRADE_LEDGER, "stat_scope TEXT");
        addColumn(db, TABLE_REAL_TRADE_LEDGER, "source_type TEXT");
        addColumn(db, TABLE_REAL_TRADE_LEDGER, "is_realtime_real INTEGER DEFAULT 0");
        addColumn(db, TABLE_REAL_TRADE_LEDGER, "result TEXT DEFAULT 'WAIT'");
        addColumn(db, TABLE_REAL_TRADE_LEDGER, "actual_number INTEGER DEFAULT 0");
        addColumn(db, TABLE_REAL_TRADE_LEDGER, "hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_REAL_TRADE_LEDGER, "pnl REAL DEFAULT 0");
        addColumn(db, TABLE_REAL_TRADE_LEDGER, "same_class_key TEXT");
        addColumn(db, TABLE_REAL_TRADE_LEDGER, "chain_key TEXT");
        addColumn(db, TABLE_REAL_TRADE_LEDGER, "decision_audit_key TEXT");
        addColumn(db, TABLE_REAL_TRADE_LEDGER, "settlement_reason TEXT");
    }

    private void repairLedgerRows(SQLiteDatabase db) {
        long now = System.currentTimeMillis();

        execQuiet(
                db,
                "UPDATE "
                        + TABLE_DECISIONS
                        + " SET decision='HOLD' WHERE decision IS NULL OR decision=''");
        execQuiet(
                db,
                "UPDATE "
                        + TABLE_DECISIONS
                        + " SET action=decision WHERE action IS NULL OR action=''");
        execQuiet(
                db,
                "UPDATE "
                        + TABLE_DECISIONS
                        + " SET result='WAIT' WHERE result IS NULL OR result=''");
        execQuiet(
                db,
                "UPDATE "
                        + TABLE_DECISIONS
                        + " SET result='WIN' WHERE result='HIT' OR result='命中'");
        execQuiet(
                db,
                "UPDATE "
                        + TABLE_DECISIONS
                        + " SET result='MISS' WHERE result='LOSE' OR result='LOSS' OR result='未中'");
        execQuiet(
                db,
                "UPDATE "
                        + TABLE_DECISIONS
                        + " SET source_type='UNKNOWN' WHERE source_type IS NULL OR source_type=''");
        execQuiet(
                db,
                "UPDATE "
                        + TABLE_DECISIONS
                        + " SET is_realtime_real=0 WHERE is_realtime_real IS NULL");
        execQuiet(
                db,
                "UPDATE "
                        + TABLE_DECISIONS
                        + " SET schema_fix_version='"
                        + DBHELPER_MAIN_CHAIN_CLOSED_LOOP_VERSION
                        + "' WHERE schema_fix_version IS NULL OR schema_fix_version=''");
        execQuiet(
                db,
                "UPDATE "
                        + TABLE_DECISIONS
                        + " SET gate=dynamic_gate WHERE (gate IS NULL OR gate=0) AND"
                        + " dynamic_gate>0");
        execQuiet(
                db,
                "UPDATE "
                        + TABLE_DECISIONS
                        + " SET final_p_before_diagnosis=final_p WHERE (final_p_before_diagnosis IS"
                        + " NULL OR final_p_before_diagnosis=0) AND final_p>0");
        execQuiet(
                db,
                "UPDATE "
                        + TABLE_DECISIONS
                        + " SET final_p_after_diagnosis=final_p WHERE (final_p_after_diagnosis IS"
                        + " NULL OR final_p_after_diagnosis=0) AND final_p>0");
        execQuiet(
                db,
                "UPDATE "
                        + TABLE_DECISIONS
                        + " SET reject_code=block_code WHERE (reject_code IS NULL OR"
                        + " reject_code='') AND block_code IS NOT NULL AND block_code<>''");

        execQuiet(
                db,
                "UPDATE "
                        + TABLE_DECISIONS
                        + " SET stat_scope='REAL_TRADE' WHERE decision='BET' AND"
                        + " source_type='REALTIME' AND is_realtime_real=1 AND (stat_scope IS NULL"
                        + " OR stat_scope='' OR stat_scope='REAL')");
        execQuiet(
                db,
                "UPDATE "
                        + TABLE_DECISIONS
                        + " SET stat_scope='SHADOW' WHERE decision='BET' AND NOT"
                        + " (source_type='REALTIME' AND is_realtime_real=1) AND (stat_scope IS NULL"
                        + " OR stat_scope='' OR stat_scope='REAL' OR stat_scope='REAL_TRADE')");
        execQuiet(
                db,
                "UPDATE "
                        + TABLE_DECISIONS
                        + " SET stat_scope='SHADOW' WHERE decision='SHADOW' AND (stat_scope IS NULL"
                        + " OR stat_scope='')");
        execQuiet(
                db,
                "UPDATE "
                        + TABLE_DECISIONS
                        + " SET stat_scope='HOLD' WHERE decision='HOLD' AND (stat_scope IS NULL OR"
                        + " stat_scope='')");
        execQuiet(
                db,
                "UPDATE "
                        + TABLE_DECISIONS
                        + " SET settled=1 WHERE result IN"
                        + " ('WIN','MISS','DATA_ERROR','DATA_OFFSET','VOID') AND settled=0");

        execQuiet(
                db,
                "UPDATE " + TABLE_TRADES + " SET action='BET' WHERE action IS NULL OR action=''");
        execQuiet(
                db,
                "UPDATE " + TABLE_TRADES + " SET result='WAIT' WHERE result IS NULL OR result=''");
        execQuiet(
                db,
                "UPDATE " + TABLE_TRADES + " SET result='WIN' WHERE result='HIT' OR result='命中'");
        execQuiet(
                db,
                "UPDATE "
                        + TABLE_TRADES
                        + " SET result='MISS' WHERE result='LOSE' OR result='LOSS' OR result='未中'");
        execQuiet(
                db,
                "UPDATE "
                        + TABLE_TRADES
                        + " SET source_type='UNKNOWN' WHERE source_type IS NULL OR source_type=''");
        execQuiet(
                db,
                "UPDATE "
                        + TABLE_TRADES
                        + " SET is_realtime_real=0 WHERE is_realtime_real IS NULL");
        execQuiet(
                db,
                "UPDATE "
                        + TABLE_TRADES
                        + " SET schema_fix_version='"
                        + DBHELPER_MAIN_CHAIN_CLOSED_LOOP_VERSION
                        + "' WHERE schema_fix_version IS NULL OR schema_fix_version=''");
        execQuiet(
                db,
                "UPDATE "
                        + TABLE_TRADES
                        + " SET gate=dynamic_gate WHERE (gate IS NULL OR gate=0) AND"
                        + " dynamic_gate>0");
        execQuiet(
                db,
                "UPDATE "
                        + TABLE_TRADES
                        + " SET final_p_before_diagnosis=final_p WHERE (final_p_before_diagnosis IS"
                        + " NULL OR final_p_before_diagnosis=0) AND final_p>0");
        execQuiet(
                db,
                "UPDATE "
                        + TABLE_TRADES
                        + " SET final_p_after_diagnosis=final_p WHERE (final_p_after_diagnosis IS"
                        + " NULL OR final_p_after_diagnosis=0) AND final_p>0");

        execQuiet(
                db,
                "UPDATE "
                        + TABLE_TRADES
                        + " SET stat_scope='REAL_TRADE' WHERE action='BET' AND"
                        + " source_type='REALTIME' AND is_realtime_real=1 AND (stat_scope IS NULL"
                        + " OR stat_scope='' OR stat_scope='REAL')");
        execQuiet(
                db,
                "UPDATE "
                        + TABLE_TRADES
                        + " SET stat_scope='SHADOW' WHERE NOT (action='BET' AND"
                        + " source_type='REALTIME' AND is_realtime_real=1) AND (stat_scope IS NULL"
                        + " OR stat_scope='' OR stat_scope='REAL' OR stat_scope='REAL_TRADE')");

        execQuiet(
                db,
                "UPDATE "
                        + TABLE_TRADES
                        + " SET settled=1 WHERE result IN"
                        + " ('WIN','MISS','DATA_ERROR','DATA_OFFSET','VOID') AND settled=0");
        execQuiet(db, "UPDATE " + TABLE_TRADES + " SET hit=1 WHERE result='WIN'");
        execQuiet(db, "UPDATE " + TABLE_TRADES + " SET hit=0 WHERE result='MISS'");
        execQuiet(
                db,
                "UPDATE "
                        + TABLE_TRADES
                        + " SET pnl="
                        + AppConfig.WIN_NET_PROFIT
                        + " WHERE result='WIN' AND stat_scope='REAL_TRADE' AND"
                        + " source_type='REALTIME' AND is_realtime_real=1 AND (pnl IS NULL OR"
                        + " pnl=0)");
        execQuiet(
                db,
                "UPDATE "
                        + TABLE_TRADES
                        + " SET pnl="
                        + AppConfig.LOSE_NET_PROFIT
                        + " WHERE result='MISS' AND stat_scope='REAL_TRADE' AND"
                        + " source_type='REALTIME' AND is_realtime_real=1 AND (pnl IS NULL OR"
                        + " pnl=0)");
        execQuiet(
                db,
                "UPDATE "
                        + TABLE_TRADES
                        + " SET pnl=0 WHERE NOT (stat_scope='REAL_TRADE' AND source_type='REALTIME'"
                        + " AND is_realtime_real=1)");
        execQuiet(
                db,
                "UPDATE "
                        + TABLE_TRADES
                        + " SET settle_time="
                        + now
                        + " WHERE settled=1 AND settle_time=0");
        execQuiet(
                db,
                "UPDATE "
                        + TABLE_TRADES
                        + " SET settled_at=settle_time WHERE settled=1 AND settled_at=0 AND"
                        + " settle_time>0");

        execQuiet(
                db,
                "UPDATE "
                        + TABLE_DECISIONS
                        + " SET "
                        + "result=(SELECT result FROM "
                        + TABLE_TRADES
                        + " WHERE "
                        + TABLE_TRADES
                        + ".target_issue="
                        + TABLE_DECISIONS
                        + ".target_issue AND "
                        + TABLE_TRADES
                        + ".stat_scope='REAL_TRADE' AND "
                        + TABLE_TRADES
                        + ".source_type='REALTIME' AND "
                        + TABLE_TRADES
                        + ".is_realtime_real=1), "
                        + "settled=(SELECT settled FROM "
                        + TABLE_TRADES
                        + " WHERE "
                        + TABLE_TRADES
                        + ".target_issue="
                        + TABLE_DECISIONS
                        + ".target_issue AND "
                        + TABLE_TRADES
                        + ".stat_scope='REAL_TRADE' AND "
                        + TABLE_TRADES
                        + ".source_type='REALTIME' AND "
                        + TABLE_TRADES
                        + ".is_realtime_real=1), "
                        + "actual_number=(SELECT actual_number FROM "
                        + TABLE_TRADES
                        + " WHERE "
                        + TABLE_TRADES
                        + ".target_issue="
                        + TABLE_DECISIONS
                        + ".target_issue AND "
                        + TABLE_TRADES
                        + ".stat_scope='REAL_TRADE' AND "
                        + TABLE_TRADES
                        + ".source_type='REALTIME' AND "
                        + TABLE_TRADES
                        + ".is_realtime_real=1), "
                        + "hit=(SELECT hit FROM "
                        + TABLE_TRADES
                        + " WHERE "
                        + TABLE_TRADES
                        + ".target_issue="
                        + TABLE_DECISIONS
                        + ".target_issue AND "
                        + TABLE_TRADES
                        + ".stat_scope='REAL_TRADE' AND "
                        + TABLE_TRADES
                        + ".source_type='REALTIME' AND "
                        + TABLE_TRADES
                        + ".is_realtime_real=1), "
                        + "pnl=(SELECT pnl FROM "
                        + TABLE_TRADES
                        + " WHERE "
                        + TABLE_TRADES
                        + ".target_issue="
                        + TABLE_DECISIONS
                        + ".target_issue AND "
                        + TABLE_TRADES
                        + ".stat_scope='REAL_TRADE' AND "
                        + TABLE_TRADES
                        + ".source_type='REALTIME' AND "
                        + TABLE_TRADES
                        + ".is_realtime_real=1), "
                        + "settle_time=(SELECT settle_time FROM "
                        + TABLE_TRADES
                        + " WHERE "
                        + TABLE_TRADES
                        + ".target_issue="
                        + TABLE_DECISIONS
                        + ".target_issue AND "
                        + TABLE_TRADES
                        + ".stat_scope='REAL_TRADE' AND "
                        + TABLE_TRADES
                        + ".source_type='REALTIME' AND "
                        + TABLE_TRADES
                        + ".is_realtime_real=1), "
                        + "settled_at=(SELECT settled_at FROM "
                        + TABLE_TRADES
                        + " WHERE "
                        + TABLE_TRADES
                        + ".target_issue="
                        + TABLE_DECISIONS
                        + ".target_issue AND "
                        + TABLE_TRADES
                        + ".stat_scope='REAL_TRADE' AND "
                        + TABLE_TRADES
                        + ".source_type='REALTIME' AND "
                        + TABLE_TRADES
                        + ".is_realtime_real=1), "
                        + "miss_reason=(SELECT miss_reason FROM "
                        + TABLE_TRADES
                        + " WHERE "
                        + TABLE_TRADES
                        + ".target_issue="
                        + TABLE_DECISIONS
                        + ".target_issue AND "
                        + TABLE_TRADES
                        + ".stat_scope='REAL_TRADE' AND "
                        + TABLE_TRADES
                        + ".source_type='REALTIME' AND "
                        + TABLE_TRADES
                        + ".is_realtime_real=1) WHERE decision='BET' AND stat_scope='REAL_TRADE'"
                        + " AND source_type='REALTIME' AND is_realtime_real=1 AND EXISTS (SELECT 1"
                        + " FROM "
                        + TABLE_TRADES
                        + " WHERE "
                        + TABLE_TRADES
                        + ".target_issue="
                        + TABLE_DECISIONS
                        + ".target_issue AND "
                        + TABLE_TRADES
                        + ".settled=1 AND "
                        + TABLE_TRADES
                        + ".stat_scope='REAL_TRADE' AND "
                        + TABLE_TRADES
                        + ".source_type='REALTIME' AND "
                        + TABLE_TRADES
                        + ".is_realtime_real=1)");
    }

    /**
     * V40 allows a bounded Top-K learning surface per target. Contract ID remains unique, and
     * target+candidate hash prevents duplicate candidate rows.
     */
    private void ensureV28OosCandidateIndexes(SQLiteDatabase db) {
        if (db == null) return;
        execQuiet(db, "DROP INDEX IF EXISTS idx_v26_oos_one_action_target");
        execQuiet(
                db,
                "CREATE UNIQUE INDEX IF NOT EXISTS idx_v28_oos_target_candidate ON "
                        + TABLE_OOS_ACTION_LOG
                        + "(target_issue,candidate_hash,build_id,cohort_id,schema_version,pipeline_version,calibration_version,version)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v28_oos_publish_queue ON "
                        + TABLE_OOS_ACTION_LOG
                        + "(settled,processed,build_id,cohort_id,pipeline_version,calibration_version,target_issue,rank_slot)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v28_oos_reliability ON "
                        + TABLE_OOS_ACTION_LOG
                        + "(settled,calibration_version,raw_rank_score,calibrated_probability,hit)");
    }

    private void createIndexes(SQLiteDatabase db) {
        ensureV28OosCandidateIndexes(db);
        execQuiet(db, "CREATE INDEX IF NOT EXISTS idx_draws_issue ON " + TABLE_DRAWS + "(issue)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_draws_open_time ON " + TABLE_DRAWS + "(open_time)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_draws_fetch_time ON "
                        + TABLE_DRAWS
                        + "(fetch_time)");

        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_decisions_target ON "
                        + TABLE_DECISIONS
                        + "(target_issue)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_decisions_created ON "
                        + TABLE_DECISIONS
                        + "(created_at)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_decisions_decision ON "
                        + TABLE_DECISIONS
                        + "(decision)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_decisions_action ON "
                        + TABLE_DECISIONS
                        + "(action)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_decisions_result ON "
                        + TABLE_DECISIONS
                        + "(result)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_decisions_settled ON "
                        + TABLE_DECISIONS
                        + "(settled)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_decisions_scope ON "
                        + TABLE_DECISIONS
                        + "(stat_scope)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_decisions_real_scope ON "
                        + TABLE_DECISIONS
                        + "(decision, stat_scope, source_type, is_realtime_real, settled)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_decisions_state ON "
                        + TABLE_DECISIONS
                        + "(state_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_decisions_profile ON "
                        + TABLE_DECISIONS
                        + "(deep_profile_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_decisions_tm_profile ON "
                        + TABLE_DECISIONS
                        + "(teacher_matrix_profile_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_decisions_reason_fp ON "
                        + TABLE_DECISIONS
                        + "(pre_draw_reason_fingerprint)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_decisions_elite45 ON "
                        + TABLE_DECISIONS
                        + "(elite45_passed, elite45_status)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_decisions_v20_grade ON "
                        + TABLE_DECISIONS
                        + "(v20_grade)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_decisions_v20_chain ON "
                        + TABLE_DECISIONS
                        + "(v20_chain_key)");

        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_trades_target ON "
                        + TABLE_TRADES
                        + "(target_issue)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_trades_settled ON " + TABLE_TRADES + "(settled)");
        execQuiet(
                db, "CREATE INDEX IF NOT EXISTS idx_trades_result ON " + TABLE_TRADES + "(result)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_trades_bet_time ON " + TABLE_TRADES + "(bet_time)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_trades_settle_time ON "
                        + TABLE_TRADES
                        + "(settle_time)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_trades_scope ON " + TABLE_TRADES + "(stat_scope)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_trades_real_scope ON "
                        + TABLE_TRADES
                        + "(action, stat_scope, source_type, is_realtime_real, settled)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_trades_position ON " + TABLE_TRADES + "(position)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_trades_profile ON "
                        + TABLE_TRADES
                        + "(deep_profile_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_trades_tm_profile ON "
                        + TABLE_TRADES
                        + "(teacher_matrix_profile_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_trades_reason_fp ON "
                        + TABLE_TRADES
                        + "(pre_draw_reason_fingerprint)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_trades_elite45 ON "
                        + TABLE_TRADES
                        + "(elite45_passed, elite45_status)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_trades_v20_grade ON "
                        + TABLE_TRADES
                        + "(v20_grade)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_trades_v20_same_class ON "
                        + TABLE_TRADES
                        + "(v20_same_class_key)");

        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_shadow_target ON "
                        + TABLE_SHADOW
                        + "(target_issue)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_shadow_rank ON "
                        + TABLE_SHADOW
                        + "(target_issue, rank_no)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_shadow_learned ON " + TABLE_SHADOW + "(learned)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_shadow_state ON " + TABLE_SHADOW + "(state_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_shadow_profile ON "
                        + TABLE_SHADOW
                        + "(deep_profile_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_shadow_tm_profile ON "
                        + TABLE_SHADOW
                        + "(teacher_matrix_profile_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_shadow_reason_fp ON "
                        + TABLE_SHADOW
                        + "(pre_draw_reason_fingerprint)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_shadow_elite45 ON "
                        + TABLE_SHADOW
                        + "(elite45_passed, elite45_status)");

        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_shape_vector_issue_pos ON "
                        + TABLE_SHAPE_VECTOR
                        + "(base_issue, position)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_shape_vector_target ON "
                        + TABLE_SHAPE_VECTOR
                        + "(target_issue)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_shape_vector_shape_key ON "
                        + TABLE_SHAPE_VECTOR
                        + "(shape_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_shape_vector_state_key ON "
                        + TABLE_SHAPE_VECTOR
                        + "(state_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_shape_vector_reason_fp ON "
                        + TABLE_SHAPE_VECTOR
                        + "(reason_fingerprint)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_shape_vector_created ON "
                        + TABLE_SHAPE_VECTOR
                        + "(created_at)");

        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_shape_memory_key ON "
                        + TABLE_SHAPE_MEMORY
                        + "(shape_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_shape_memory_active ON "
                        + TABLE_SHAPE_MEMORY
                        + "(active_flag)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_combo_memory_key ON "
                        + TABLE_COMBO_MEMORY
                        + "(shape_key, position, k3_mask)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_combo_memory_active ON "
                        + TABLE_COMBO_MEMORY
                        + "(active_flag)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_break_memory_key ON "
                        + TABLE_BREAK_MEMORY
                        + "(break_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_calibration_bucket ON "
                        + TABLE_CALIBRATION
                        + "(bucket_key)");

        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_backtest_created ON "
                        + TABLE_BACKTEST
                        + "(created_at)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_backtest_mode_window ON "
                        + TABLE_BACKTEST
                        + "(mode, window_no, created_at)");

        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_deep_profile_key ON "
                        + TABLE_DEEP_PROFILE
                        + "(profile_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_deep_profile_pos_state ON "
                        + TABLE_DEEP_PROFILE
                        + "(position, state_mode)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_deep_profile_active ON "
                        + TABLE_DEEP_PROFILE
                        + "(active_flag, stale_flag)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_deep_profile_updated ON "
                        + TABLE_DEEP_PROFILE
                        + "(updated_at)");

        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_attr_target ON "
                        + TABLE_DEEP_ATTRIBUTION
                        + "(target_issue)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_attr_reason ON "
                        + TABLE_DEEP_ATTRIBUTION
                        + "(main_reason_code)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_attr_module ON "
                        + TABLE_DEEP_ATTRIBUTION
                        + "(affected_module)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_attr_profile ON "
                        + TABLE_DEEP_ATTRIBUTION
                        + "(profile_key)");

        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_transition_key ON "
                        + TABLE_DEEP_TRANSITION
                        + "(transition_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_transition_pos_from_to ON "
                        + TABLE_DEEP_TRANSITION
                        + "(position, from_number, to_number)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_transition_chain ON "
                        + TABLE_DEEP_TRANSITION
                        + "(chain_type)");

        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_cycle_key ON " + TABLE_DEEP_CYCLE + "(cycle_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_cycle_pos_num ON "
                        + TABLE_DEEP_CYCLE
                        + "(position, number)");

        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_manual_task_status ON "
                        + TABLE_MANUAL_BACKTEST_TASK
                        + "(status)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_manual_task_started ON "
                        + TABLE_MANUAL_BACKTEST_TASK
                        + "(started_at)");

        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_pit_snapshot_id ON "
                        + TABLE_POINT_IN_TIME_SNAPSHOT
                        + "(snapshot_id)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_pit_target ON "
                        + TABLE_POINT_IN_TIME_SNAPSHOT
                        + "(target_issue)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_pit_base ON "
                        + TABLE_POINT_IN_TIME_SNAPSHOT
                        + "(base_issue)");

        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_promotion_key ON "
                        + TABLE_PROMOTION_CANDIDATE
                        + "(promotion_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_promotion_profile ON "
                        + TABLE_PROMOTION_CANDIDATE
                        + "(profile_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_promotion_status ON "
                        + TABLE_PROMOTION_CANDIDATE
                        + "(status)");

        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_prob_audit_bucket ON "
                        + TABLE_PROBABILITY_AUDIT_BUCKET
                        + "(bucket_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_prob_audit_status ON "
                        + TABLE_PROBABILITY_AUDIT_BUCKET
                        + "(status)");

        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_bias_issue ON " + TABLE_BIAS_EVIDENCE + "(issue)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_bias_status ON "
                        + TABLE_BIAS_EVIDENCE
                        + "(status)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_bias_created ON "
                        + TABLE_BIAS_EVIDENCE
                        + "(created_at)");

        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_learning_snapshot_created ON "
                        + TABLE_LEARNING_SNAPSHOT
                        + "(created_at)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_learning_snapshot_profile ON "
                        + TABLE_LEARNING_SNAPSHOT
                        + "(profile_key)");

        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_candidate_evidence_key ON "
                        + TABLE_CANDIDATE_EVIDENCE
                        + "(evidence_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_candidate_evidence_target ON "
                        + TABLE_CANDIDATE_EVIDENCE
                        + "(target_issue)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_candidate_evidence_score ON "
                        + TABLE_CANDIDATE_EVIDENCE
                        + "(evidence_score)");

        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_ai_judge_target ON "
                        + TABLE_AI_JUDGE_LOG
                        + "(target_issue)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_ai_judge_created ON "
                        + TABLE_AI_JUDGE_LOG
                        + "(created_at)");

        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_diagnosis_created ON "
                        + TABLE_SELF_DIAGNOSIS_LOG
                        + "(created_at)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_diagnosis_level ON "
                        + TABLE_SELF_DIAGNOSIS_LOG
                        + "(level)");

        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_teacher_snapshot_created ON "
                        + TABLE_TEACHER_SNAPSHOT
                        + "(created_at)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_teacher_snapshot_status ON "
                        + TABLE_TEACHER_SNAPSHOT
                        + "(status)");

        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_realtime_validate_target ON "
                        + TABLE_REALTIME_VALIDATE
                        + "(target_issue)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_realtime_validate_created ON "
                        + TABLE_REALTIME_VALIDATE
                        + "(created_at)");

        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_runtime_profile_key ON "
                        + TABLE_RUNTIME_PROFILE_CACHE
                        + "(profile_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_runtime_profile_state ON "
                        + TABLE_RUNTIME_PROFILE_CACHE
                        + "(state_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_runtime_profile_pos_mask ON "
                        + TABLE_RUNTIME_PROFILE_CACHE
                        + "(position, k3_mask)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_runtime_profile_updated ON "
                        + TABLE_RUNTIME_PROFILE_CACHE
                        + "(updated_at)");

        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_state_number_key ON "
                        + TABLE_STATE_NUMBER_PROFILE
                        + "(profile_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_state_number_state ON "
                        + TABLE_STATE_NUMBER_PROFILE
                        + "(state_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_state_number_pos_num ON "
                        + TABLE_STATE_NUMBER_PROFILE
                        + "(position, number)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_state_number_updated ON "
                        + TABLE_STATE_NUMBER_PROFILE
                        + "(updated_at)");

        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_learning_case_key ON "
                        + TABLE_LEARNING_CASE_LOG
                        + "(case_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_learning_case_target ON "
                        + TABLE_LEARNING_CASE_LOG
                        + "(target_issue)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_learning_case_task ON "
                        + TABLE_LEARNING_CASE_LOG
                        + "(task_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_learning_case_segment ON "
                        + TABLE_LEARNING_CASE_LOG
                        + "(segment_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_learning_case_state ON "
                        + TABLE_LEARNING_CASE_LOG
                        + "(state_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_learning_case_reason_fp ON "
                        + TABLE_LEARNING_CASE_LOG
                        + "(pre_draw_reason_fingerprint)");

        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_auto_learn_cursor_task ON "
                        + TABLE_AUTO_LEARN_CURSOR
                        + "(task_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_auto_learn_cursor_status ON "
                        + TABLE_AUTO_LEARN_CURSOR
                        + "(status)");

        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_tm_sample_key ON "
                        + TABLE_TEACHER_MATRIX_SAMPLE
                        + "(sample_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_tm_sample_target ON "
                        + TABLE_TEACHER_MATRIX_SAMPLE
                        + "(target_issue)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_tm_sample_base ON "
                        + TABLE_TEACHER_MATRIX_SAMPLE
                        + "(base_issue)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_tm_sample_pos_num ON "
                        + TABLE_TEACHER_MATRIX_SAMPLE
                        + "(position, number)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_tm_sample_state ON "
                        + TABLE_TEACHER_MATRIX_SAMPLE
                        + "(state_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_tm_sample_profile ON "
                        + TABLE_TEACHER_MATRIX_SAMPLE
                        + "(profile_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_tm_sample_combo ON "
                        + TABLE_TEACHER_MATRIX_SAMPLE
                        + "(combo_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_tm_sample_reason_fp ON "
                        + TABLE_TEACHER_MATRIX_SAMPLE
                        + "(reason_fingerprint)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_tm_sample_label ON "
                        + TABLE_TEACHER_MATRIX_SAMPLE
                        + "(label)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_tm_sample_created ON "
                        + TABLE_TEACHER_MATRIX_SAMPLE
                        + "(created_at)");

        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_tm_profile_key ON "
                        + TABLE_TEACHER_MATRIX_PROFILE
                        + "(profile_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_tm_profile_state ON "
                        + TABLE_TEACHER_MATRIX_PROFILE
                        + "(state_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_tm_profile_pos_num ON "
                        + TABLE_TEACHER_MATRIX_PROFILE
                        + "(position, number)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_tm_profile_status ON "
                        + TABLE_TEACHER_MATRIX_PROFILE
                        + "(status)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_tm_profile_updated ON "
                        + TABLE_TEACHER_MATRIX_PROFILE
                        + "(updated_at)");

        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_tm_state_key ON "
                        + TABLE_TEACHER_MATRIX_STATE_PROFILE
                        + "(state_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_tm_state_status ON "
                        + TABLE_TEACHER_MATRIX_STATE_PROFILE
                        + "(status)");

        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_tm_combo_key ON "
                        + TABLE_TEACHER_MATRIX_COMBO_PROFILE
                        + "(combo_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_tm_combo_state ON "
                        + TABLE_TEACHER_MATRIX_COMBO_PROFILE
                        + "(state_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_tm_combo_pos_mask ON "
                        + TABLE_TEACHER_MATRIX_COMBO_PROFILE
                        + "(position, k3_mask)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_tm_combo_status ON "
                        + TABLE_TEACHER_MATRIX_COMBO_PROFILE
                        + "(status)");

        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_tm_report_created ON "
                        + TABLE_TEACHER_MATRIX_REPORT
                        + "(created_at)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_tm_report_task ON "
                        + TABLE_TEACHER_MATRIX_REPORT
                        + "(task_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_tm_report_segment ON "
                        + TABLE_TEACHER_MATRIX_REPORT
                        + "(segment_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_tm_report_status ON "
                        + TABLE_TEACHER_MATRIX_REPORT
                        + "(status)");

        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_reason_sample_key ON "
                        + TABLE_PRE_DRAW_REASON_SAMPLE
                        + "(sample_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_reason_sample_target ON "
                        + TABLE_PRE_DRAW_REASON_SAMPLE
                        + "(target_issue)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_reason_sample_state ON "
                        + TABLE_PRE_DRAW_REASON_SAMPLE
                        + "(state_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_reason_sample_fp ON "
                        + TABLE_PRE_DRAW_REASON_SAMPLE
                        + "(reason_fingerprint)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_reason_sample_pos_num ON "
                        + TABLE_PRE_DRAW_REASON_SAMPLE
                        + "(position, number)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_reason_sample_label ON "
                        + TABLE_PRE_DRAW_REASON_SAMPLE
                        + "(label)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_reason_sample_created ON "
                        + TABLE_PRE_DRAW_REASON_SAMPLE
                        + "(created_at)");

        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_reason_contrast_key ON "
                        + TABLE_REASON_CONTRAST_PROFILE
                        + "(profile_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_reason_contrast_fp ON "
                        + TABLE_REASON_CONTRAST_PROFILE
                        + "(reason_fingerprint)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_reason_contrast_state ON "
                        + TABLE_REASON_CONTRAST_PROFILE
                        + "(state_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_reason_contrast_pos_num ON "
                        + TABLE_REASON_CONTRAST_PROFILE
                        + "(position, number)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_reason_contrast_level ON "
                        + TABLE_REASON_CONTRAST_PROFILE
                        + "(valid_level)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_reason_contrast_updated ON "
                        + TABLE_REASON_CONTRAST_PROFILE
                        + "(updated_at)");

        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_reason_causal_key ON "
                        + TABLE_REASON_CAUSAL_PROFILE
                        + "(causal_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_reason_causal_combo ON "
                        + TABLE_REASON_CAUSAL_PROFILE
                        + "(reason_combo_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_reason_causal_atom ON "
                        + TABLE_REASON_CAUSAL_PROFILE
                        + "(reason_atom)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_reason_causal_level ON "
                        + TABLE_REASON_CAUSAL_PROFILE
                        + "(valid_level)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_reason_causal_updated ON "
                        + TABLE_REASON_CAUSAL_PROFILE
                        + "(updated_at)");

        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_false_strong_key ON "
                        + TABLE_FALSE_STRONG_PROFILE
                        + "(false_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_false_strong_state ON "
                        + TABLE_FALSE_STRONG_PROFILE
                        + "(state_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_false_strong_fp ON "
                        + TABLE_FALSE_STRONG_PROFILE
                        + "(reason_fingerprint)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_false_strong_pos_num ON "
                        + TABLE_FALSE_STRONG_PROFILE
                        + "(position, number)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_false_strong_status ON "
                        + TABLE_FALSE_STRONG_PROFILE
                        + "(status)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_false_strong_updated ON "
                        + TABLE_FALSE_STRONG_PROFILE
                        + "(updated_at)");

        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_combo_role_key ON "
                        + TABLE_COMBO_ROLE_PROFILE
                        + "(combo_role_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_combo_role_state ON "
                        + TABLE_COMBO_ROLE_PROFILE
                        + "(state_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_combo_role_pos_mask ON "
                        + TABLE_COMBO_ROLE_PROFILE
                        + "(position, k3_mask)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_combo_role_status ON "
                        + TABLE_COMBO_ROLE_PROFILE
                        + "(status)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_combo_role_updated ON "
                        + TABLE_COMBO_ROLE_PROFILE
                        + "(updated_at)");

        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_elite45_audit_key ON "
                        + TABLE_ELITE45_AUDIT_LOG
                        + "(audit_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_elite45_audit_target ON "
                        + TABLE_ELITE45_AUDIT_LOG
                        + "(target_issue)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_elite45_audit_passed ON "
                        + TABLE_ELITE45_AUDIT_LOG
                        + "(passed)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_elite45_audit_block ON "
                        + TABLE_ELITE45_AUDIT_LOG
                        + "(block_code)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_elite45_audit_created ON "
                        + TABLE_ELITE45_AUDIT_LOG
                        + "(created_at)");

        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_tr_case_target ON "
                        + TABLE_TEACHER_REPLAY_CASE
                        + "(target_issue)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_tr_case_state ON "
                        + TABLE_TEACHER_REPLAY_CASE
                        + "(state_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_tr_case_created ON "
                        + TABLE_TEACHER_REPLAY_CASE
                        + "(created_at)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_tr_cursor_status ON "
                        + TABLE_TEACHER_REPLAY_CURSOR
                        + "(status)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_tr_report_created ON "
                        + TABLE_TEACHER_REPLAY_REPORT
                        + "(created_at)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_tr_cf_target ON "
                        + TABLE_TEACHER_REPLAY_COUNTERFACTUAL
                        + "(target_issue)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_tr_stability_state ON "
                        + TABLE_TEACHER_REPLAY_STABILITY
                        + "(state_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_ta_profile_state ON "
                        + TABLE_TEACHER_ASSIST_PROFILE
                        + "(state_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_ta_profile_pos_mask ON "
                        + TABLE_TEACHER_ASSIST_PROFILE
                        + "(position, k3_mask)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_ui_text_key ON "
                        + TABLE_UI_TEXT_STATE
                        + "(text_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_diag_created ON "
                        + TABLE_DIAG_LOG
                        + "(created_at)");

        createV20Indexes(db);
        createDynamicCausalWindowIndexes(db);
    }

    private void addV20ClosedLoopSchemaColumns(SQLiteDatabase db) {
        addColumn(db, TABLE_V20_WINDOW_STATE, "base_issue TEXT");
        addColumn(db, TABLE_V20_WINDOW_STATE, "target_issue TEXT");
        addColumn(db, TABLE_V20_WINDOW_STATE, "created_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_WINDOW_STATE, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_WINDOW_STATE, "window_len INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_WINDOW_STATE, "window_start_issue TEXT");
        addColumn(db, TABLE_V20_WINDOW_STATE, "window_end_issue TEXT");
        addColumn(db, TABLE_V20_WINDOW_STATE, "exact_state_key TEXT");
        addColumn(db, TABLE_V20_WINDOW_STATE, "relaxed_state_key TEXT");
        addColumn(db, TABLE_V20_WINDOW_STATE, "hot_slope REAL DEFAULT 0");
        addColumn(db, TABLE_V20_WINDOW_STATE, "cold_slope REAL DEFAULT 0");
        addColumn(db, TABLE_V20_WINDOW_STATE, "miss_bucket INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_WINDOW_STATE, "density_score REAL DEFAULT 0");
        addColumn(db, TABLE_V20_WINDOW_STATE, "repeat_score REAL DEFAULT 0");
        addColumn(db, TABLE_V20_WINDOW_STATE, "odd_even_bias REAL DEFAULT 0");
        addColumn(db, TABLE_V20_WINDOW_STATE, "big_small_bias REAL DEFAULT 0");
        addColumn(db, TABLE_V20_WINDOW_STATE, "neighbor_score REAL DEFAULT 0");
        addColumn(db, TABLE_V20_WINDOW_STATE, "same_tail_score REAL DEFAULT 0");
        addColumn(db, TABLE_V20_WINDOW_STATE, "position_crowd_score REAL DEFAULT 0");
        addColumn(db, TABLE_V20_WINDOW_STATE, "cross_position_sync_score REAL DEFAULT 0");
        addColumn(db, TABLE_V20_WINDOW_STATE, "squeeze_score REAL DEFAULT 0");
        addColumn(db, TABLE_V20_WINDOW_STATE, "recent_lose_chain INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_WINDOW_STATE, "timing_offset INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_WINDOW_STATE, "timing_bucket TEXT");
        addColumn(db, TABLE_V20_WINDOW_STATE, "feature_distance REAL DEFAULT 0");
        addColumn(db, TABLE_V20_WINDOW_STATE, "sample_trust REAL DEFAULT 0");
        addColumn(db, TABLE_V20_WINDOW_STATE, "feature_json TEXT");
        addColumn(db, TABLE_V20_WINDOW_STATE, "summary_text TEXT");

        addColumn(db, TABLE_V20_CAUSE_MATRIX, "state_key TEXT");
        addColumn(db, TABLE_V20_CAUSE_MATRIX, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_CAUSE_MATRIX, "number INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_CAUSE_MATRIX, "k3_mask INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_CAUSE_MATRIX, "reason_atom TEXT");
        addColumn(db, TABLE_V20_CAUSE_MATRIX, "support_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_CAUSE_MATRIX, "support_hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_CAUSE_MATRIX, "counter_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_CAUSE_MATRIX, "counter_hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_CAUSE_MATRIX, "support_score REAL DEFAULT 0");
        addColumn(db, TABLE_V20_CAUSE_MATRIX, "counter_score REAL DEFAULT 0");
        addColumn(db, TABLE_V20_CAUSE_MATRIX, "lift REAL DEFAULT 0");
        addColumn(db, TABLE_V20_CAUSE_MATRIX, "confidence REAL DEFAULT 0");
        addColumn(db, TABLE_V20_CAUSE_MATRIX, "sample_trust REAL DEFAULT 0");
        addColumn(db, TABLE_V20_CAUSE_MATRIX, "recent_effectiveness REAL DEFAULT 0");
        addColumn(db, TABLE_V20_CAUSE_MATRIX, "decay_risk REAL DEFAULT 0");
        addColumn(db, TABLE_V20_CAUSE_MATRIX, "action_suggestion TEXT");
        addColumn(db, TABLE_V20_CAUSE_MATRIX, "updated_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_CAUSE_MATRIX, "summary_text TEXT");

        addColumn(db, TABLE_V20_TEACHER_NUMBER, "state_key TEXT");
        addColumn(db, TABLE_V20_TEACHER_NUMBER, "profile_key TEXT");
        addColumn(db, TABLE_V20_TEACHER_NUMBER, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_TEACHER_NUMBER, "number INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_TEACHER_NUMBER, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_TEACHER_NUMBER, "hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_TEACHER_NUMBER, "miss_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_TEACHER_NUMBER, "recent30_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_TEACHER_NUMBER, "recent30_hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_TEACHER_NUMBER, "recent60_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_TEACHER_NUMBER, "recent60_hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_TEACHER_NUMBER, "recent120_sample INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_TEACHER_NUMBER, "recent120_hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_TEACHER_NUMBER, "hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_V20_TEACHER_NUMBER, "recent_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_V20_TEACHER_NUMBER, "teacher_edge REAL DEFAULT 0");
        addColumn(db, TABLE_V20_TEACHER_NUMBER, "confidence REAL DEFAULT 0");
        addColumn(db, TABLE_V20_TEACHER_NUMBER, "support_score REAL DEFAULT 0");
        addColumn(db, TABLE_V20_TEACHER_NUMBER, "risk_score REAL DEFAULT 0");
        addColumn(db, TABLE_V20_TEACHER_NUMBER, "teacher_status TEXT DEFAULT 'EMPTY'");
        addColumn(db, TABLE_V20_TEACHER_NUMBER, "why_hit_text TEXT");
        addColumn(db, TABLE_V20_TEACHER_NUMBER, "why_miss_text TEXT");
        addColumn(db, TABLE_V20_TEACHER_NUMBER, "updated_at INTEGER DEFAULT 0");

        addColumn(db, TABLE_V20_SIMILAR_WINDOW, "target_issue TEXT");
        addColumn(db, TABLE_V20_SIMILAR_WINDOW, "matched_issue TEXT");
        addColumn(db, TABLE_V20_SIMILAR_WINDOW, "created_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_SIMILAR_WINDOW, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_SIMILAR_WINDOW, "k3_mask INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_SIMILAR_WINDOW, "state_key TEXT");
        addColumn(db, TABLE_V20_SIMILAR_WINDOW, "exact_state_key TEXT");
        addColumn(db, TABLE_V20_SIMILAR_WINDOW, "relaxed_state_key TEXT");
        addColumn(db, TABLE_V20_SIMILAR_WINDOW, "feature_distance REAL DEFAULT 0");
        addColumn(db, TABLE_V20_SIMILAR_WINDOW, "exact_match INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_SIMILAR_WINDOW, "relaxed_match INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_SIMILAR_WINDOW, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_SIMILAR_WINDOW, "hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_SIMILAR_WINDOW, "hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_V20_SIMILAR_WINDOW, "confidence REAL DEFAULT 0");
        addColumn(db, TABLE_V20_SIMILAR_WINDOW, "summary_text TEXT");

        addColumn(db, TABLE_V20_REVERSE_REASON_SAMPLE, "target_issue TEXT");
        addColumn(db, TABLE_V20_REVERSE_REASON_SAMPLE, "created_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_REVERSE_REASON_SAMPLE, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_REVERSE_REASON_SAMPLE, "n1 INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_REVERSE_REASON_SAMPLE, "n2 INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_REVERSE_REASON_SAMPLE, "n3 INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_REVERSE_REASON_SAMPLE, "k3_mask INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_REVERSE_REASON_SAMPLE, "actual_number INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_REVERSE_REASON_SAMPLE, "hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_REVERSE_REASON_SAMPLE, "state_key TEXT");
        addColumn(db, TABLE_V20_REVERSE_REASON_SAMPLE, "reason_code TEXT");
        addColumn(db, TABLE_V20_REVERSE_REASON_SAMPLE, "early1_risk REAL DEFAULT 0");
        addColumn(db, TABLE_V20_REVERSE_REASON_SAMPLE, "early2_risk REAL DEFAULT 0");
        addColumn(db, TABLE_V20_REVERSE_REASON_SAMPLE, "false_strong_risk REAL DEFAULT 0");
        addColumn(db, TABLE_V20_REVERSE_REASON_SAMPLE, "rank_wrong_risk REAL DEFAULT 0");
        addColumn(db, TABLE_V20_REVERSE_REASON_SAMPLE, "teacher_decay_risk REAL DEFAULT 0");
        addColumn(db, TABLE_V20_REVERSE_REASON_SAMPLE, "rule_decay_risk REAL DEFAULT 0");
        addColumn(db, TABLE_V20_REVERSE_REASON_SAMPLE, "sample_weak_risk REAL DEFAULT 0");
        addColumn(db, TABLE_V20_REVERSE_REASON_SAMPLE, "noise_score REAL DEFAULT 0");
        addColumn(db, TABLE_V20_REVERSE_REASON_SAMPLE, "position_wrong_risk REAL DEFAULT 0");
        addColumn(db, TABLE_V20_REVERSE_REASON_SAMPLE, "number_cover_wrong_risk REAL DEFAULT 0");
        addColumn(db, TABLE_V20_REVERSE_REASON_SAMPLE, "reverse_text TEXT");

        addColumn(db, TABLE_V20_RULE_HEALTH, "state_key TEXT");
        addColumn(db, TABLE_V20_RULE_HEALTH, "rule_name TEXT");
        addColumn(db, TABLE_V20_RULE_HEALTH, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_RULE_HEALTH, "hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_RULE_HEALTH, "miss_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_RULE_HEALTH, "hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_V20_RULE_HEALTH, "recent_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_V20_RULE_HEALTH, "current_lose INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_RULE_HEALTH, "max_lose INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_RULE_HEALTH, "edge_decay_risk REAL DEFAULT 0");
        addColumn(db, TABLE_V20_RULE_HEALTH, "is_active INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_RULE_HEALTH, "is_frozen INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_RULE_HEALTH, "freeze_until_issue TEXT");
        addColumn(db, TABLE_V20_RULE_HEALTH, "updated_at INTEGER DEFAULT 0");

        addColumn(db, TABLE_V20_SHADOW_HEALTH, "state_key TEXT");
        addColumn(db, TABLE_V20_SHADOW_HEALTH, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_SHADOW_HEALTH, "grade TEXT");
        addColumn(db, TABLE_V20_SHADOW_HEALTH, "sample30 INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_SHADOW_HEALTH, "hit30 INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_SHADOW_HEALTH, "sample60 INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_SHADOW_HEALTH, "hit60 INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_SHADOW_HEALTH, "sample120 INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_SHADOW_HEALTH, "hit120 INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_SHADOW_HEALTH, "hit_rate30 REAL DEFAULT 0");
        addColumn(db, TABLE_V20_SHADOW_HEALTH, "hit_rate60 REAL DEFAULT 0");
        addColumn(db, TABLE_V20_SHADOW_HEALTH, "hit_rate120 REAL DEFAULT 0");
        addColumn(db, TABLE_V20_SHADOW_HEALTH, "current_shadow_lose INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_SHADOW_HEALTH, "shadow_health_score REAL DEFAULT 0");
        addColumn(db, TABLE_V20_SHADOW_HEALTH, "promotion_pass INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_SHADOW_HEALTH, "updated_at INTEGER DEFAULT 0");

        addColumn(db, TABLE_V20_FALSE_STRONG, "state_key TEXT");
        addColumn(db, TABLE_V20_FALSE_STRONG, "same_class_key TEXT");
        addColumn(db, TABLE_V20_FALSE_STRONG, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_FALSE_STRONG, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_FALSE_STRONG, "hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_FALSE_STRONG, "miss_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_FALSE_STRONG, "consecutive_miss INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_FALSE_STRONG, "false_strong_risk REAL DEFAULT 0");
        addColumn(db, TABLE_V20_FALSE_STRONG, "freeze_level INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_FALSE_STRONG, "freeze_until_issue TEXT");
        addColumn(db, TABLE_V20_FALSE_STRONG, "is_frozen INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_FALSE_STRONG, "updated_at INTEGER DEFAULT 0");

        addColumn(db, TABLE_V20_ACTION_MATRIX, "target_issue TEXT");
        addColumn(db, TABLE_V20_ACTION_MATRIX, "created_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_ACTION_MATRIX, "candidate_key TEXT");
        addColumn(db, TABLE_V20_ACTION_MATRIX, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_ACTION_MATRIX, "n1 INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_ACTION_MATRIX, "n2 INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_ACTION_MATRIX, "n3 INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_ACTION_MATRIX, "k3_mask INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_ACTION_MATRIX, "k3_legal INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_ACTION_MATRIX, "grade TEXT");
        addColumn(db, TABLE_V20_ACTION_MATRIX, "action TEXT");
        addColumn(db, TABLE_V20_ACTION_MATRIX, "action_confidence REAL DEFAULT 0");
        addColumn(db, TABLE_V20_ACTION_MATRIX, "bet_now_pass INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_ACTION_MATRIX, "hold_reason TEXT");
        addColumn(db, TABLE_V20_ACTION_MATRIX, "shadow_reason TEXT");
        addColumn(db, TABLE_V20_ACTION_MATRIX, "freeze_reason TEXT");

        addColumn(db, TABLE_V20_CLOSED_LOOP, "base_issue TEXT");
        addColumn(db, TABLE_V20_CLOSED_LOOP, "target_issue TEXT");
        addColumn(db, TABLE_V20_CLOSED_LOOP, "created_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_CLOSED_LOOP, "settled_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_CLOSED_LOOP, "position INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_CLOSED_LOOP, "k3_mask INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_CLOSED_LOOP, "action TEXT");
        addColumn(db, TABLE_V20_CLOSED_LOOP, "stat_scope TEXT");
        addColumn(db, TABLE_V20_CLOSED_LOOP, "source_type TEXT");
        addColumn(db, TABLE_V20_CLOSED_LOOP, "is_realtime_real INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_CLOSED_LOOP, "result TEXT DEFAULT 'WAIT'");
        addColumn(db, TABLE_V20_CLOSED_LOOP, "actual_number INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_CLOSED_LOOP, "hit INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_CLOSED_LOOP, "pnl REAL DEFAULT 0");
        addColumn(db, TABLE_V20_CLOSED_LOOP, "window_state_key TEXT");
        addColumn(db, TABLE_V20_CLOSED_LOOP, "cause_key TEXT");
        addColumn(db, TABLE_V20_CLOSED_LOOP, "teacher_key TEXT");
        addColumn(db, TABLE_V20_CLOSED_LOOP, "similar_key TEXT");
        addColumn(db, TABLE_V20_CLOSED_LOOP, "reverse_key TEXT");
        addColumn(db, TABLE_V20_CLOSED_LOOP, "action_key TEXT");
        addColumn(db, TABLE_V20_CLOSED_LOOP, "decision_audit_key TEXT");
        addColumn(db, TABLE_V20_CLOSED_LOOP, "closed_loop_text TEXT");
    }

    private void createV20Indexes(SQLiteDatabase db) {
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_feature_target ON "
                        + TABLE_PRE_DRAW_FEATURE
                        + "(target_issue)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_feature_state ON "
                        + TABLE_PRE_DRAW_FEATURE
                        + "(state_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_num_sample_target ON "
                        + TABLE_PRE_DRAW_NUMBER_SAMPLE
                        + "(target_issue)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_num_sample_pos_num ON "
                        + TABLE_PRE_DRAW_NUMBER_SAMPLE
                        + "(position, number)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_scan_target_rank ON "
                        + TABLE_FULL_CANDIDATE_SCAN
                        + "(target_issue, rank_no)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_scan_state ON "
                        + TABLE_FULL_CANDIDATE_SCAN
                        + "(state_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_scan_same_class ON "
                        + TABLE_FULL_CANDIDATE_SCAN
                        + "(same_class_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_audit_target ON "
                        + TABLE_CANDIDATE_AUDIT
                        + "(target_issue)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_audit_candidate ON "
                        + TABLE_CANDIDATE_AUDIT
                        + "(candidate_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_causal_state ON "
                        + TABLE_CAUSAL_FACTOR_MATRIX
                        + "(state_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_causal_atom ON "
                        + TABLE_CAUSAL_FACTOR_MATRIX
                        + "(reason_atom)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_position_state_pos ON "
                        + TABLE_POSITION_CAUSE_MATRIX
                        + "(state_key, position)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_teacher_state_pos_num ON "
                        + TABLE_TEACHER_NUMBER_MATRIX
                        + "(state_key, position, number)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_rank_target_rank ON "
                        + TABLE_COUNTERFACTUAL_RANK
                        + "(target_issue, rank_no)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_rank_state_group ON "
                        + TABLE_COUNTERFACTUAL_RANK
                        + "(state_key, rank_group)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_guard_state_pos ON "
                        + TABLE_THIRD_GUARD_MATRIX
                        + "(state_key, position)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_rule_state ON "
                        + TABLE_RULE_HEALTH_MATRIX
                        + "(state_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_rule_active ON "
                        + TABLE_RULE_HEALTH_MATRIX
                        + "(is_active, is_frozen)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_source_state ON "
                        + TABLE_SOURCE_HEALTH_MATRIX
                        + "(state_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_state_health ON "
                        + TABLE_STATE_HEALTH_MATRIX
                        + "(state_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_shadow_grade ON "
                        + TABLE_SHADOW_HEALTH_GATE
                        + "(grade)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_shadow_state_pos ON "
                        + TABLE_SHADOW_HEALTH_GATE
                        + "(state_key, position)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_freeze_same_class ON "
                        + TABLE_FALSE_STRONG_FREEZE
                        + "(same_class_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_freeze_state ON "
                        + TABLE_FALSE_STRONG_FREEZE
                        + "(state_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_freeze_active ON "
                        + TABLE_FALSE_STRONG_FREEZE
                        + "(is_frozen)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_seed_state_pos ON "
                        + TABLE_SEED_CAUSE_EXPANSION
                        + "(state_key, position)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_chain_target ON "
                        + TABLE_DYNAMIC_CAUSE_CHAIN
                        + "(target_issue)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_chain_candidate ON "
                        + TABLE_DYNAMIC_CAUSE_CHAIN
                        + "(candidate_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_chain_grade ON "
                        + TABLE_DYNAMIC_CAUSE_CHAIN
                        + "(grade)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_decision_audit_target ON "
                        + TABLE_DECISION_AUDIT
                        + "(target_issue)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_decision_audit_action ON "
                        + TABLE_DECISION_AUDIT
                        + "(action)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_ledger_target ON "
                        + TABLE_REAL_TRADE_LEDGER
                        + "(target_issue)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_ledger_real ON "
                        + TABLE_REAL_TRADE_LEDGER
                        + "(action, stat_scope, source_type, is_realtime_real, result)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_ws_target_pos_len ON "
                        + TABLE_V20_WINDOW_STATE
                        + "(target_issue, position, window_len)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_ws_exact ON "
                        + TABLE_V20_WINDOW_STATE
                        + "(exact_state_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_ws_relaxed ON "
                        + TABLE_V20_WINDOW_STATE
                        + "(relaxed_state_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_cm_state_pos ON "
                        + TABLE_V20_CAUSE_MATRIX
                        + "(state_key, position)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_cm_atom ON "
                        + TABLE_V20_CAUSE_MATRIX
                        + "(reason_atom)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_tn_state_pos_num ON "
                        + TABLE_V20_TEACHER_NUMBER
                        + "(state_key, position, number)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_tn_status ON "
                        + TABLE_V20_TEACHER_NUMBER
                        + "(teacher_status)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_sim_target ON "
                        + TABLE_V20_SIMILAR_WINDOW
                        + "(target_issue)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_sim_distance ON "
                        + TABLE_V20_SIMILAR_WINDOW
                        + "(state_key, feature_distance)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_rev_target ON "
                        + TABLE_V20_REVERSE_REASON_SAMPLE
                        + "(target_issue)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_rev_reason ON "
                        + TABLE_V20_REVERSE_REASON_SAMPLE
                        + "(reason_code)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_rule2_state ON "
                        + TABLE_V20_RULE_HEALTH
                        + "(state_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_rule2_active ON "
                        + TABLE_V20_RULE_HEALTH
                        + "(is_active, is_frozen)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_shadow2_state_grade ON "
                        + TABLE_V20_SHADOW_HEALTH
                        + "(state_key, grade)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_false2_same_class ON "
                        + TABLE_V20_FALSE_STRONG
                        + "(same_class_key, is_frozen)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_action_target ON "
                        + TABLE_V20_ACTION_MATRIX
                        + "(target_issue)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_action_candidate ON "
                        + TABLE_V20_ACTION_MATRIX
                        + "(candidate_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_loop_target ON "
                        + TABLE_V20_CLOSED_LOOP
                        + "(target_issue)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_loop_real ON "
                        + TABLE_V20_CLOSED_LOOP
                        + "(action, stat_scope, source_type, is_realtime_real, result)");
    }

    private void createDynamicCausalWindowIndexes(SQLiteDatabase db) {
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_decisions_dynamic_cluster ON "
                        + TABLE_DECISIONS
                        + "(dynamic_cluster_id)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_decisions_dynamic_status ON "
                        + TABLE_DECISIONS
                        + "(dynamic_cluster_status)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_decisions_adaptive_action ON "
                        + TABLE_DECISIONS
                        + "(adaptive_judge_action)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_decisions_need_supply ON "
                        + TABLE_DECISIONS
                        + "(need_supply_match_score)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_trades_dynamic_cluster ON "
                        + TABLE_TRADES
                        + "(dynamic_cluster_id)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_trades_dynamic_status ON "
                        + TABLE_TRADES
                        + "(dynamic_cluster_status)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_trades_adaptive_action ON "
                        + TABLE_TRADES
                        + "(adaptive_judge_action)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_shadow_dynamic_cluster ON "
                        + TABLE_SHADOW
                        + "(dynamic_cluster_id)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_shadow_dynamic_status ON "
                        + TABLE_SHADOW
                        + "(dynamic_cluster_status)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_shadow_adaptive_action ON "
                        + TABLE_SHADOW
                        + "(adaptive_judge_action)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_shadow_need_supply ON "
                        + TABLE_SHADOW
                        + "(need_supply_match_score)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_dcw_status ON "
                        + TABLE_DYNAMIC_WINDOW_CLUSTER
                        + "(status)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_dcw_hit_rate ON "
                        + TABLE_DYNAMIC_WINDOW_CLUSTER
                        + "(hit_rate)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_dcw_recent_hit_rate ON "
                        + TABLE_DYNAMIC_WINDOW_CLUSTER
                        + "(recent_hit_rate)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_dcw_updated ON "
                        + TABLE_DYNAMIC_WINDOW_CLUSTER
                        + "(updated_at)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_dcw_prototype ON "
                        + TABLE_DYNAMIC_WINDOW_CLUSTER
                        + "(prototype_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_wfs_target ON "
                        + TABLE_WINDOW_FEATURE_SAMPLE
                        + "(target_issue)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_wfs_cluster ON "
                        + TABLE_WINDOW_FEATURE_SAMPLE
                        + "(cluster_id)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_wfs_pos_k ON "
                        + TABLE_WINDOW_FEATURE_SAMPLE
                        + "(position, k3_mask)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_wfs_decision ON "
                        + TABLE_WINDOW_FEATURE_SAMPLE
                        + "(decision_type)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_pns_target_pos ON "
                        + TABLE_POSITION_NEED_SAMPLE
                        + "(target_issue, position)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_pns_need_type ON "
                        + TABLE_POSITION_NEED_SAMPLE
                        + "(main_need_type)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_nss_target_pos_num ON "
                        + TABLE_NUMBER_SUPPLY_SAMPLE
                        + "(target_issue, position, number)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_nss_label ON "
                        + TABLE_NUMBER_SUPPLY_SAMPLE
                        + "(label)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_nss_supply_type ON "
                        + TABLE_NUMBER_SUPPLY_SAMPLE
                        + "(main_supply_type)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_nsms_target ON "
                        + TABLE_NEED_SUPPLY_MATCH_SAMPLE
                        + "(target_issue)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_nsms_pos_k ON "
                        + TABLE_NEED_SUPPLY_MATCH_SAMPLE
                        + "(position, k3_mask)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_nsms_score ON "
                        + TABLE_NEED_SUPPLY_MATCH_SAMPLE
                        + "(match_score)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_cfr_target ON "
                        + TABLE_COUNTERFACTUAL_REASON_SAMPLE
                        + "(target_issue)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_cfr_cluster ON "
                        + TABLE_COUNTERFACTUAL_REASON_SAMPLE
                        + "(cluster_id)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_cfr_rank ON "
                        + TABLE_COUNTERFACTUAL_REASON_SAMPLE
                        + "(position_rank, k3_rank)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_ablation_cluster ON "
                        + TABLE_CAUSE_ABLATION_SAMPLE
                        + "(cluster_id)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_ablation_score ON "
                        + TABLE_CAUSE_ABLATION_SAMPLE
                        + "(total_ablation_score)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_miss_target ON "
                        + TABLE_MISS_REASON_SAMPLE
                        + "(target_issue)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_miss_cluster ON "
                        + TABLE_MISS_REASON_SAMPLE
                        + "(cluster_id)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_miss_type ON "
                        + TABLE_MISS_REASON_SAMPLE
                        + "(miss_type)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_adaptive_target ON "
                        + TABLE_ADAPTIVE_JUDGE_SAMPLE
                        + "(target_issue)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_adaptive_cluster ON "
                        + TABLE_ADAPTIVE_JUDGE_SAMPLE
                        + "(cluster_id)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_adaptive_action ON "
                        + TABLE_ADAPTIVE_JUDGE_SAMPLE
                        + "(action)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_promo_cluster ON "
                        + TABLE_PROMOTION_DOMAIN_SNAPSHOT
                        + "(cluster_id)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_promo_status ON "
                        + TABLE_PROMOTION_DOMAIN_SNAPSHOT
                        + "(status)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_dynamic_causal_state_updated ON "
                        + TABLE_DYNAMIC_CAUSAL_STATE
                        + "(updated_at)");
        createMainChainClosedLoopIndexes(db);
    }

    private void createMainChainClosedLoopIndexes(SQLiteDatabase db) {
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_mccs_target_rank ON "
                        + TABLE_MAIN_CHAIN_CANDIDATE_SNAPSHOT
                        + "(target_issue, realtime_top40_rank)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_mccs_candidate ON "
                        + TABLE_MAIN_CHAIN_CANDIDATE_SNAPSHOT
                        + "(candidate_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_mccs_adaptive ON "
                        + TABLE_MAIN_CHAIN_CANDIDATE_SNAPSHOT
                        + "(adaptive_input_flag)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_mccs_reject ON "
                        + TABLE_MAIN_CHAIN_CANDIDATE_SNAPSHOT
                        + "(reject_code)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_mcrb_code ON "
                        + TABLE_MAIN_CHAIN_REJECT_BUCKET
                        + "(reject_code)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_decisions_main_chain ON "
                        + TABLE_DECISIONS
                        + "(target_issue, decision, reject_code, real_probe_allowed)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_decisions_adaptive_status ON "
                        + TABLE_DECISIONS
                        + "(adaptive_status, shadow_promotion_status)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_decisions_factor_cluster ON "
                        + TABLE_DECISIONS
                        + "(cluster_key, factor_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_trades_main_chain ON "
                        + TABLE_TRADES
                        + "(target_issue, action, reject_code, real_probe_allowed)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_trades_factor_cluster ON "
                        + TABLE_TRADES
                        + "(cluster_key, factor_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_shadow_main_chain ON "
                        + TABLE_SHADOW
                        + "(target_issue, rank_no, reject_code)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_shadow_promotion_status ON "
                        + TABLE_SHADOW
                        + "(shadow_promotion_status)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_candidate_scan_top40 ON "
                        + TABLE_FULL_CANDIDATE_SCAN
                        + "(target_issue, realtime_top40_rank, adaptive_input_flag)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_candidate_scan_main_chain ON "
                        + TABLE_FULL_CANDIDATE_SCAN
                        + "(target_issue, reject_code, real_probe_allowed)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_candidate_scan_factor_cluster ON "
                        + TABLE_FULL_CANDIDATE_SCAN
                        + "(cluster_key, factor_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_adaptive_candidate_count ON "
                        + TABLE_ADAPTIVE_JUDGE_SAMPLE
                        + "(target_issue, candidate_count)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_adaptive_status_action ON "
                        + TABLE_ADAPTIVE_JUDGE_SAMPLE
                        + "(adaptive_status, action)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_promo_main_chain ON "
                        + TABLE_PROMOTION_DOMAIN_SNAPSHOT
                        + "(shadow_promotion_status, sample_count)");
    }

    private void insertDefaultRows(SQLiteDatabase db) {
        long now = System.currentTimeMillis();

        insertState(db, "service_status", AppConfig.SERVICE_STATUS_STOPPED, now);
        insertState(db, "running_flag", "0", now);
        insertState(db, "current_issue", "", now);
        insertState(db, "target_issue", "", now);
        insertState(db, "api_line", AppConfig.API_LINE_PRIMARY, now);
        insertState(db, "countdown_ms", "0", now);
        insertState(db, "next_open_time", "0", now);
        insertState(db, "last_decision", AppConfig.DECISION_HOLD, now);
        insertState(db, "last_error", "", now);

        insertState(db, "ledger_schema_version", String.valueOf(AppConfig.DB_VERSION), now);
        insertState(db, "ledger_rule", "STRICT_SINGLE_POSITION_K3_TARGET_ISSUE_REALTIME_ONLY", now);
        insertState(db, "ledger_real_stat_table", TABLE_TRADES, now);
        insertState(db, "ledger_real_stat_scope", AppConfig.STAT_SCOPE_REAL_TRADE, now);
        insertState(db, "ledger_real_stat_source_type", "REALTIME", now);
        insertState(db, "ledger_real_stat_flag", "is_realtime_real=1", now);
        insertState(db, "ledger_repair_policy", "NO_DECISION_TO_TRADE_AUTO_INSERT", now);

        insertState(db, "backtest_status", AppConfig.BACKTEST_STATUS_IDLE, now);
        insertState(db, "backtest_mode", "", now);
        insertState(db, "backtest_reason", "", now);
        insertState(db, "backtest_cost_ms", "0", now);
        insertState(db, "backtest_finished_at", "0", now);
        insertState(db, "backtest_error", "", now);

        insertState(db, "manual_backtest_status", AppConfig.BACKTEST_STATUS_IDLE, now);
        insertState(db, "manual_backtest_progress", "0", now);
        insertState(db, "manual_backtest_summary", "", now);
        insertState(db, "learning_summary", "", now);
        insertState(db, "bias_summary", "", now);
        insertState(db, "audit_summary", "", now);
        insertState(db, "promotion_summary", "", now);
        insertState(db, "a_plus_summary", "", now);
        insertState(db, "ai_judge_summary", "", now);
        insertState(db, "self_diagnosis_summary", "", now);
        insertState(db, "scheduler_summary", "", now);
        insertState(db, "last_snapshot_id", "", now);

        insertState(db, "teacher_snapshot_status", AppConfig.BACKTEST_STATUS_IDLE, now);
        insertState(db, "teacher_snapshot_summary", "", now);
        insertState(db, "teacher_matrix_status", AppConfig.TEACHER_MATRIX_STATUS_NONE, now);
        insertState(db, "teacher_matrix_sample_count", "0", now);
        insertState(db, "teacher_matrix_profile_count", "0", now);
        insertState(db, "teacher_matrix_strong_count", "0", now);
        insertState(db, "teacher_matrix_risk_count", "0", now);
        insertState(db, "teacher_matrix_summary", "", now);

        insertState(db, "reason_matrix_status", "NONE", now);
        insertState(db, "reason_sample_count", "0", now);
        insertState(db, "reason_profile_count", "0", now);
        insertState(db, "reason_false_strong_count", "0", now);
        insertState(db, "reason_summary", "", now);
        insertState(db, "elite45_status", "NONE", now);
        insertState(db, "elite45_pass_count", "0", now);
        insertState(db, "elite45_block_count", "0", now);
        insertState(db, "elite45_summary", "", now);

        insertState(db, "teacher_replay_status", AppConfig.TEACHER_REPLAY_STATUS_IDLE, now);
        insertState(db, "teacher_replay_summary", "", now);
        insertState(db, "teacher_assist_status", AppConfig.TEACHER_ASSIST_STATUS_NONE, now);
        insertState(db, "teacher_assist_summary", "", now);
        insertState(db, "realtime_validate_status", AppConfig.BACKTEST_STATUS_IDLE, now);
        insertState(db, "realtime_validate_summary", "", now);
        insertState(db, "auto_learn_status", AppConfig.AUTO_LEARN_STATUS_IDLE, now);
        insertState(db, "auto_learn_progress", "0", now);
        insertState(db, "auto_learn_task_key", "", now);
        insertState(db, "auto_learn_summary", "", now);
        insertState(db, "runtime_profile_summary", "", now);

        insertState(db, "v20_schema_status", "READY", now);
        insertState(db, "v20_strategy_name", "DYNAMIC_CAUSE_CHAIN_V20", now);
        insertState(db, "v20_real_bet_entry", "DecisionGateEngine_ONLY", now);
        insertState(db, "v20_settlement_rule", "STRICT_SINGLE_POSITION_K3_ONLY", now);
        insertState(db, "v20_full_candidate_scan", "REALTIME_TOP40_BACKGROUND_1200", now);
        insertState(db, "v20_shadow_warmup", "240", now);
        insertState(db, "v20_grade_rule", "S_A_B_C_WITH_B_SHADOW_PROMOTION", now);
        insertState(db, "v20_c_never_bet", "1", now);
        insertState(db, "v20_teacher_not_authority", "1", now);
        insertState(db, "v20_no_future_leak", "TARGET_ISSUE_BEFORE_ONLY", now);
        insertState(db, "v20_reason_summary", "", now);
        insertState(db, "v20_health_summary", "", now);
        insertState(db, "v20_false_strong_summary", "", now);
        insertState(db, "v20_shadow_health_summary", "", now);
        insertState(db, "v20_window_state_schema", TABLE_V20_WINDOW_STATE, now);
        insertState(db, "v20_cause_matrix_schema", TABLE_V20_CAUSE_MATRIX, now);
        insertState(db, "v20_teacher_number_schema", TABLE_V20_TEACHER_NUMBER, now);
        insertState(db, "v20_similar_window_schema", TABLE_V20_SIMILAR_WINDOW, now);
        insertState(db, "v20_reverse_reason_schema", TABLE_V20_REVERSE_REASON_SAMPLE, now);
        insertState(db, "v20_action_matrix_schema", TABLE_V20_ACTION_MATRIX, now);
        insertState(db, "v20_closed_loop_schema", TABLE_V20_CLOSED_LOOP, now);
        insertState(db, "v20_stat_isolation", "REAL_TRADE_ONLY_FOR_TRUE_WINRATE_PNL", now);
        insertState(db, "v20_shadow_hold_no_real_stat", "1", now);

        insertState(
                db,
                AppConfig.STATE_DYNAMIC_CLUSTER_SNAPSHOT_VERSION,
                DBHELPER_DYNAMIC_CAUSAL_WINDOW_VERSION,
                now);
        insertState(db, AppConfig.STATE_DYNAMIC_CLUSTER_LAST_RUN, "0", now);
        insertState(db, AppConfig.STATE_DYNAMIC_CLUSTER_SUMMARY, "", now);
        insertState(db, AppConfig.STATE_NEED_SUPPLY_LAST_RUN, "0", now);
        insertState(db, AppConfig.STATE_COUNTERFACTUAL_LAST_RUN, "0", now);
        insertState(db, AppConfig.STATE_ADAPTIVE_JUDGE_CANDIDATE_COUNT, "0", now);
        insertState(db, AppConfig.STATE_ADAPTIVE_JUDGE_REALTIME_SUMMARY, "候选=0｜等待实时接入", now);
        insertState(db, AppConfig.STATE_DYNAMIC_CAUSAL_DIAGNOSIS, "READY｜动态因果窗口Schema已就绪", now);
        insertState(db, "dynamic_causal_schema_status", "READY", now);
        insertState(
                db, "dynamic_causal_schema_version", DBHELPER_DYNAMIC_CAUSAL_WINDOW_VERSION, now);
        insertState(db, "main_chain_schema_version", DBHELPER_MAIN_CHAIN_CLOSED_LOOP_VERSION, now);
        insertState(
                db,
                "main_chain_candidate_snapshot_table",
                TABLE_MAIN_CHAIN_CANDIDATE_SNAPSHOT,
                now);
        insertState(db, "main_chain_reject_bucket_table", TABLE_MAIN_CHAIN_REJECT_BUCKET, now);
        insertState(
                db,
                "main_chain_probe_rule",
                "DecisionGateEngine_ONLY_FINALP_0345_EDGE_NON_NEGATIVE",
                now);
        insertState(db, "main_chain_no_autofetch_rescue", "1", now);
        insertState(db, "main_chain_no_dbstore_rescue", "1", now);
        insertState(db, "dynamic_causal_real_bet_entry", "DecisionGateEngine_ONLY", now);
        insertState(db, "dynamic_causal_learning_mode", "BACKGROUND_CLUSTER_REALTIME_MATCH", now);
        insertState(db, "dynamic_causal_baseline_guard", "RANDOM_BASELINE_REQUIRED", now);
        insertState(db, "dynamic_causal_no_fake_strong", "1", now);
        insertState(db, "dynamic_causal_no_future_leak", "TARGET_ISSUE_BEFORE_ONLY", now);
        insertState(db, "dynamic_causal_shadow_full_fields", "1", now);
        insertState(
                db,
                "dynamic_causal_tables",
                TABLE_DYNAMIC_WINDOW_CLUSTER
                        + ","
                        + TABLE_WINDOW_FEATURE_SAMPLE
                        + ","
                        + TABLE_POSITION_NEED_SAMPLE
                        + ","
                        + TABLE_NUMBER_SUPPLY_SAMPLE,
                now);

        insertCalibration(db, "P00_32", 0.000d, 0.320d, now);
        insertCalibration(db, "P32_34", 0.320d, 0.340d, now);
        insertCalibration(db, "P34_36", 0.340d, 0.360d, now);
        insertCalibration(db, "P36_38", 0.360d, 0.380d, now);
        insertCalibration(db, "P38_40", 0.380d, 0.400d, now);
        insertCalibration(db, "P40_42", 0.400d, 0.420d, now);
        insertCalibration(db, "P42_44", 0.420d, 0.440d, now);
        insertCalibration(db, "P44_46", 0.440d, 0.460d, now);
        insertCalibration(db, "P46_48", 0.460d, 0.480d, now);
        insertCalibration(db, "P48_PLUS", 0.480d, 1.000d, now);

        insertAuditBucket(db, "A00_05", 0.000d, 0.050d, now);
        insertAuditBucket(db, "A05_10", 0.050d, 0.100d, now);
        insertAuditBucket(db, "A10_15", 0.100d, 0.150d, now);
        insertAuditBucket(db, "A15_20", 0.150d, 0.200d, now);
        insertAuditBucket(db, "A20_25", 0.200d, 0.250d, now);
        insertAuditBucket(db, "A25_30", 0.250d, 0.300d, now);
        insertAuditBucket(db, "A30_32", 0.300d, 0.320d, now);
        insertAuditBucket(db, "A32_34", 0.320d, 0.340d, now);
        insertAuditBucket(db, "A34_36", 0.340d, 0.360d, now);
        insertAuditBucket(db, "A36_38", 0.360d, 0.380d, now);
        insertAuditBucket(db, "A38_40", 0.380d, 0.400d, now);
        insertAuditBucket(db, "A40_42", 0.400d, 0.420d, now);
        insertAuditBucket(db, "A42_44", 0.420d, 0.440d, now);
        insertAuditBucket(db, "A44_46", 0.440d, 0.460d, now);
        insertAuditBucket(db, "A46_48", 0.460d, 0.480d, now);
        insertAuditBucket(db, "A48_50", 0.480d, 0.500d, now);
        insertAuditBucket(db, "A50_55", 0.500d, 0.550d, now);
        insertAuditBucket(db, "A55_60", 0.550d, 0.600d, now);
        insertAuditBucket(db, "A60_70", 0.600d, 0.700d, now);
        insertAuditBucket(db, "A70_PLUS", 0.700d, 1.000d, now);

        insertDefaultModelWeight(db, now);
    }

    private void insertDefaultModelWeight(SQLiteDatabase db, long now) {
        execQuiet(
                db,
                "INSERT OR IGNORE INTO "
                        + TABLE_MODEL_WEIGHT
                        + " (weight_key, window_weight, markov_weight, shape_weight, combo_weight,"
                        + " recovery_weight, similar_weight, memory_weight, position_weight,"
                        + " frequency_weight, drift_penalty_weight, hot_reverse_penalty_weight,"
                        + " false_strong_penalty_weight, break_penalty_weight, deep_feature_weight,"
                        + " bias_weight, profile_weight, runtime_profile_weight,"
                        + " state_number_profile_weight, teacher_weight, teacher_matrix_weight,"
                        + " reason_weight, elite45_weight, cycle_weight, transition_weight,"
                        + " ai_judge_weight, audit_weight, recent_error, recent_win_rate,"
                        + " recent_frequency, recent_pnl, updated_at) VALUES ('GLOBAL', 0.24, 0.14,"
                        + " 0.18, 0.14, 0.08, 0.06, 0.04, 0.08, 0.00, 1.0, 1.0, 1.0, 1.0, 0.08,"
                        + " 0.05, 0.08, 0.10, 0.10, 1.0, 0.18, 0.08, 0.06, 0.05, 0.05, 0.10, 0.06,"
                        + " 0.0, 0.0, 0.0, 0.0, "
                        + now
                        + ")");
    }

    private void insertState(SQLiteDatabase db, String key, String value, long now) {
        execQuiet(
                db,
                "INSERT OR IGNORE INTO "
                        + TABLE_APP_STATE
                        + " (state_key, state_value, updated_at) VALUES ("
                        + quote(key)
                        + ","
                        + quote(value)
                        + ","
                        + now
                        + ")");
    }

    private void insertCalibration(
            SQLiteDatabase db, String key, double min, double max, long now) {
        execQuiet(
                db,
                "INSERT OR IGNORE INTO "
                        + TABLE_CALIBRATION
                        + " (bucket_key, raw_min, raw_max, sample_count, hit_count, real_hit_rate,"
                        + " updated_at) VALUES ("
                        + quote(key)
                        + ","
                        + min
                        + ","
                        + max
                        + ",0,0,0,"
                        + now
                        + ")");
    }

    private void insertAuditBucket(
            SQLiteDatabase db, String key, double min, double max, long now) {
        execQuiet(
                db,
                "INSERT OR IGNORE INTO "
                        + TABLE_PROBABILITY_AUDIT_BUCKET
                        + " (bucket_key, bucket_low, bucket_high, sample_count, hit_count,"
                        + " hit_rate, avg_predicted_p, calibration_error, brier_score,"
                        + " recent_sample, recent_hit, recent_hit_rate, recent_error, status,"
                        + " updated_at) VALUES ("
                        + quote(key)
                        + ","
                        + min
                        + ","
                        + max
                        + ",0,0,0,0,0,0,0,0,0,0,"
                        + quote(String.valueOf(AppConfig.PROB_BUCKET_SAMPLE_WEAK))
                        + ","
                        + now
                        + ")");
    }

    /* 链路修改升级V2数据库结构：幂等建表、幂等加列，不清库、不删旧列。 */
    private void ensureLinkModificationV2Schema(SQLiteDatabase db) {
        if (db == null) return;
        createBase(db, TABLE_V20_FREQUENCY_STATE, "state_key TEXT PRIMARY KEY");
        createBase(db, TABLE_V20_MODULE_REGISTRY, "module_name TEXT PRIMARY KEY");
        createBase(
                db,
                TABLE_V20_LAYER_SNAPSHOT,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, snapshot_key TEXT NOT NULL UNIQUE");
        createBase(db, TABLE_V20_BACKGROUND_CURSOR, "task_key TEXT PRIMARY KEY");
        createBase(
                db,
                TABLE_V20_SHADOW_DOMAIN_STAT,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, domain_key TEXT NOT NULL UNIQUE");
        createBase(
                db,
                TABLE_V20_LINK_CANDIDATE_SNAPSHOT,
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, snapshot_key TEXT NOT NULL UNIQUE");

        String[] decisionLike =
                new String[] {
                    TABLE_DECISIONS,
                    TABLE_TRADES,
                    TABLE_SHADOW,
                    TABLE_FULL_CANDIDATE_SCAN,
                    TABLE_CANDIDATE_AUDIT,
                    TABLE_DYNAMIC_CAUSE_CHAIN,
                    TABLE_DECISION_AUDIT,
                    TABLE_V20_CLOSED_LOOP,
                    TABLE_MAIN_CHAIN_CANDIDATE_SNAPSHOT,
                    TABLE_V20_LINK_CANDIDATE_SNAPSHOT,
                    TABLE_REAL_TRADE_LEDGER
                };
        for (int i = 0; i < decisionLike.length; i++) {
            addLinkModificationDecisionColumns(db, decisionLike[i]);
        }

        addColumn(db, TABLE_V20_FREQUENCY_STATE, "as_of_issue TEXT");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "status TEXT");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "version TEXT");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "frequency_20 REAL DEFAULT 0");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "frequency_50 REAL DEFAULT 0");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "frequency_100 REAL DEFAULT 0");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "frequency_300 REAL DEFAULT 0");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "frequency_debt REAL DEFAULT 0");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "valid_issue_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "real_bet_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "qualified_opportunity_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "recent_probe_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "recent_lose_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "debt_frozen INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "required_floor_bet_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "target_bet_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "floor_debt_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "target_debt_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "no_upper_cap INTEGER DEFAULT 1");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "observed_20 INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "observed_50 INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "observed_100 INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "observed_300 INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "bet_count_20 INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "bet_count_50 INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "bet_count_100 INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "bet_count_300 INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "floor_debt_20 INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "floor_debt_50 INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "floor_debt_100 INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "floor_debt_300 INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "target_debt_20 INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "target_debt_50 INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "target_debt_100 INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "target_debt_300 INTEGER DEFAULT 0");
        addColumn(
                db, TABLE_V20_FREQUENCY_STATE, "qualified_floor_candidate_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "urgent_window TEXT");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "issues_since_last_real_bet INTEGER DEFAULT 999");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "issues_since_last_floor_bet INTEGER DEFAULT 999");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "consecutive_floor_bets INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "floor_pacing_min_gap INTEGER DEFAULT 2147483647");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "floor_pacing_ready INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "floor_pacing_reason TEXT");
        addColumn(db, TABLE_V20_FREQUENCY_STATE, "updated_at INTEGER DEFAULT 0");

        addColumn(db, TABLE_V20_MODULE_REGISTRY, "version TEXT");
        addColumn(db, TABLE_V20_MODULE_REGISTRY, "cohort TEXT");
        addColumn(db, TABLE_V20_MODULE_REGISTRY, "status TEXT");
        addColumn(db, TABLE_V20_MODULE_REGISTRY, "detail TEXT");
        addColumn(db, TABLE_V20_MODULE_REGISTRY, "registered_at INTEGER DEFAULT 0");

        addColumn(db, TABLE_V20_LAYER_SNAPSHOT, "layer TEXT");
        addColumn(db, TABLE_V20_LAYER_SNAPSHOT, "as_of_issue TEXT");
        addColumn(db, TABLE_V20_LAYER_SNAPSHOT, "max_included_issue TEXT");
        addColumn(db, TABLE_V20_LAYER_SNAPSHOT, "schema_version TEXT");
        addColumn(db, TABLE_V20_LAYER_SNAPSHOT, "strategy_version TEXT");
        addColumn(db, TABLE_V20_LAYER_SNAPSHOT, "freshness_status TEXT");
        addColumn(db, TABLE_V20_LAYER_SNAPSHOT, "payload TEXT");
        addColumn(db, TABLE_V20_LAYER_SNAPSHOT, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_LAYER_SNAPSHOT, "created_at INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_LAYER_SNAPSHOT, "expires_at INTEGER DEFAULT 0");

        addColumn(db, TABLE_V20_BACKGROUND_CURSOR, "cursor_issue TEXT");
        addColumn(db, TABLE_V20_BACKGROUND_CURSOR, "target_issue TEXT");
        addColumn(db, TABLE_V20_BACKGROUND_CURSOR, "status TEXT");
        addColumn(db, TABLE_V20_BACKGROUND_CURSOR, "version TEXT");
        addColumn(db, TABLE_V20_BACKGROUND_CURSOR, "processed_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_BACKGROUND_CURSOR, "pending_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_BACKGROUND_CURSOR, "updated_at INTEGER DEFAULT 0");

        addColumn(db, TABLE_V20_SHADOW_DOMAIN_STAT, "parent_domain_key TEXT");
        addColumn(db, TABLE_V20_SHADOW_DOMAIN_STAT, "promotion_status TEXT");
        addColumn(db, TABLE_V20_SHADOW_DOMAIN_STAT, "version TEXT");
        addColumn(db, TABLE_V20_SHADOW_DOMAIN_STAT, "sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_SHADOW_DOMAIN_STAT, "hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_SHADOW_DOMAIN_STAT, "miss_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_SHADOW_DOMAIN_STAT, "recent_sample_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_SHADOW_DOMAIN_STAT, "recent_hit_count INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_SHADOW_DOMAIN_STAT, "misleading_streak INTEGER DEFAULT 0");
        addColumn(db, TABLE_V20_SHADOW_DOMAIN_STAT, "hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_V20_SHADOW_DOMAIN_STAT, "recent_hit_rate REAL DEFAULT 0");
        addColumn(db, TABLE_V20_SHADOW_DOMAIN_STAT, "posterior_mean REAL DEFAULT 0");
        addColumn(db, TABLE_V20_SHADOW_DOMAIN_STAT, "wilson_lower_bound REAL DEFAULT 0");
        addColumn(db, TABLE_V20_SHADOW_DOMAIN_STAT, "drift_score REAL DEFAULT 0");
        addColumn(db, TABLE_V20_SHADOW_DOMAIN_STAT, "last_settled_issue TEXT");
        addColumn(db, TABLE_V20_SHADOW_DOMAIN_STAT, "updated_at INTEGER DEFAULT 0");

        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_layer_snapshot_layer_issue ON "
                        + TABLE_V20_LAYER_SNAPSHOT
                        + "(layer,as_of_issue)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_background_cursor_status ON "
                        + TABLE_V20_BACKGROUND_CURSOR
                        + "(status,updated_at)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_shadow_domain_status ON "
                        + TABLE_V20_SHADOW_DOMAIN_STAT
                        + "(promotion_status,sample_count)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_v20_link_candidate_target_rank ON "
                        + TABLE_V20_LINK_CANDIDATE_SNAPSHOT
                        + "(target_issue,candidate_rank)");
        execQuiet(
                db,
                "CREATE UNIQUE INDEX IF NOT EXISTS idx_v20_link_candidate_unique ON "
                        + TABLE_V20_LINK_CANDIDATE_SNAPSHOT
                        + "(target_issue,candidate_key)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_teacher_domain_status_v20 ON "
                        + TABLE_TEACHER_DOMAIN_STAT_V20
                        + "(status,sample_count)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_dynamic_domain_status_v20 ON "
                        + TABLE_DYNAMIC_DOMAIN_STAT_V20
                        + "(status,sample_count)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_promotion_domain_status_v20 ON "
                        + TABLE_PROMOTION_DOMAIN_STAT_V20
                        + "(status,sample_count)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_service_recovery_event_at_v20 ON "
                        + TABLE_SERVICE_RECOVERY_EVENT_V20
                        + "(event_at)");
    }

    private void ensureDomainEvidenceColumns(SQLiteDatabase db, String table) {
        addColumn(db, table, "parent_domain_key TEXT");
        addColumn(db, table, "coarse_domain_key TEXT");
        addColumn(db, table, "status TEXT");
        addColumn(db, table, "version TEXT");
        addColumn(db, table, "sample_count INTEGER DEFAULT 0");
        addColumn(db, table, "hit_count INTEGER DEFAULT 0");
        addColumn(db, table, "miss_count INTEGER DEFAULT 0");
        addColumn(db, table, "recent_sample_count INTEGER DEFAULT 0");
        addColumn(db, table, "recent_hit_count INTEGER DEFAULT 0");
        addColumn(db, table, "misleading_streak INTEGER DEFAULT 0");
        addColumn(db, table, "hit_rate REAL DEFAULT 0");
        addColumn(db, table, "recent_hit_rate REAL DEFAULT 0");
        addColumn(db, table, "posterior_mean REAL DEFAULT 0");
        addColumn(db, table, "lower_bound REAL DEFAULT 0");
        addColumn(db, table, "upper_bound REAL DEFAULT 1");
        addColumn(db, table, "drift_score REAL DEFAULT 0");
        addColumn(db, table, "updated_at INTEGER DEFAULT 0");
    }

    private void addLinkModificationDecisionColumns(SQLiteDatabase db, String table) {
        if (table == null || table.length() == 0) return;
        addColumn(db, table, "base_gate REAL DEFAULT 0");
        addColumn(db, table, "risk_adjusted_gate REAL DEFAULT 0");
        addColumn(db, table, "frequency_gate_adjust REAL DEFAULT 0");
        addColumn(db, table, "effective_probe_gate REAL DEFAULT 0");
        addColumn(db, table, "raw_edge REAL DEFAULT 0");
        addColumn(db, table, "effective_probe_edge REAL DEFAULT 0");
        addColumn(db, table, "frequency_50 REAL DEFAULT 0");
        addColumn(db, table, "frequency_100 REAL DEFAULT 0");
        addColumn(db, table, "frequency_300 REAL DEFAULT 0");
        addColumn(db, table, "frequency_debt_score REAL DEFAULT 0");
        addColumn(db, table, "frequency_support_score REAL DEFAULT 0");
        addColumn(db, table, "qualified_probe_score REAL DEFAULT 0");
        addColumn(db, table, "shadow_promotion_score REAL DEFAULT 0");
        addColumn(db, table, "need_supply_score REAL DEFAULT 0");
        addColumn(db, table, "counter_risk REAL DEFAULT 0");
        addColumn(db, table, "qualified_probe INTEGER DEFAULT 0");
        addColumn(db, table, "real_trade INTEGER DEFAULT 0");
        addColumn(db, table, "coverage_available INTEGER DEFAULT 0");
        addColumn(db, table, "third_quality_available INTEGER DEFAULT 0");
        addColumn(db, table, "promotion_domain_available INTEGER DEFAULT 0");
        addColumn(db, table, "frequency_state TEXT");
        addColumn(db, table, "frequency_state_version TEXT");
        addColumn(db, table, "shadow_promotion_domain TEXT");
        addColumn(db, table, "decision_path TEXT");
        addColumn(db, table, "bet_domain TEXT");
        addColumn(db, table, "probe_reject_reason TEXT");
        addColumn(db, table, "probe_eligibility_version TEXT");
        addColumn(db, table, "teacher_as_of_issue TEXT");
        addColumn(db, table, "teacher_issue_lag INTEGER DEFAULT 0");
        addColumn(db, table, "teacher_fresh_by_issue INTEGER DEFAULT 0");
        addColumn(db, table, "teacher_snapshot_created_at INTEGER DEFAULT 0");
        addColumn(db, table, "teacher_number_history_sample INTEGER DEFAULT 0");
        addColumn(db, table, "teacher_domain_shadow_sample INTEGER DEFAULT 0");
        addColumn(db, table, "teacher_domain_real_sample INTEGER DEFAULT 0");
        addColumn(db, table, "teacher_apply_error TEXT");
        addColumn(db, table, "predecision_domain_version TEXT");
        addColumn(db, table, "teacher_runtime_apply_version TEXT");
        addColumn(db, table, "block_tier TEXT");
        addColumn(db, table, "hard_risk_code TEXT");
        addColumn(db, table, "observe_reason TEXT");
        addColumn(db, table, "probe_eligible INTEGER DEFAULT 0");
        addColumn(db, table, "shadow_probe_eligible INTEGER DEFAULT 0");
        addColumn(db, table, "probe_level TEXT");
        addColumn(db, table, "shadow_settled_count INTEGER DEFAULT 0");
        addColumn(db, table, "shadow_domain_sample INTEGER DEFAULT 0");
        addColumn(db, table, "promotion_wilson_lower_bound REAL DEFAULT 0");
        addColumn(db, table, "promotion_eligibility_reason TEXT");
        addColumn(db, table, "rule_health_status TEXT");
        addColumn(db, table, "snapshot_freshness TEXT");
        addColumn(db, table, "warm_profile_freshness TEXT");
        addColumn(db, table, "cold_profile_freshness TEXT");
        addColumn(db, table, "deep_profile_freshness TEXT");
        addColumn(db, table, "decision_version_v2 TEXT");
        addColumn(db, table, "evidence_as_of_issue TEXT");
        addColumn(db, table, "snapshot_as_of_issue TEXT");
        addColumn(db, table, "snapshot_created_at INTEGER DEFAULT 0");
        addColumn(db, table, "frequency_state_updated_at INTEGER DEFAULT 0");
        addColumn(db, table, "frequency_floor_required_count INTEGER DEFAULT 0");
        addColumn(db, table, "frequency_floor_target_count INTEGER DEFAULT 0");
        addColumn(db, table, "frequency_floor_actual_count INTEGER DEFAULT 0");
        addColumn(db, table, "frequency_floor_debt_count INTEGER DEFAULT 0");
        addColumn(db, table, "frequency_target_debt_count INTEGER DEFAULT 0");
        addColumn(db, table, "frequency_floor_mandatory INTEGER DEFAULT 0");
        addColumn(db, table, "frequency_floor_no_upper_cap INTEGER DEFAULT 1");
        addColumn(db, table, "frequency_floor_rank_score REAL DEFAULT 0");
        addColumn(db, table, "frequency_floor_version TEXT");
        addColumn(db, table, "real_bet_frequency_20 REAL DEFAULT 0");
        addColumn(db, table, "frequency_observed_20 INTEGER DEFAULT 0");
        addColumn(db, table, "frequency_observed_50 INTEGER DEFAULT 0");
        addColumn(db, table, "frequency_observed_100 INTEGER DEFAULT 0");
        addColumn(db, table, "frequency_observed_300 INTEGER DEFAULT 0");
        addColumn(db, table, "frequency_bet_count_20 INTEGER DEFAULT 0");
        addColumn(db, table, "frequency_bet_count_50 INTEGER DEFAULT 0");
        addColumn(db, table, "frequency_bet_count_100 INTEGER DEFAULT 0");
        addColumn(db, table, "frequency_bet_count_300 INTEGER DEFAULT 0");
        addColumn(db, table, "frequency_debt_20 INTEGER DEFAULT 0");
        addColumn(db, table, "frequency_debt_50 INTEGER DEFAULT 0");
        addColumn(db, table, "frequency_debt_100 INTEGER DEFAULT 0");
        addColumn(db, table, "frequency_debt_300 INTEGER DEFAULT 0");
        addColumn(db, table, "frequency_target_debt_20 INTEGER DEFAULT 0");
        addColumn(db, table, "frequency_target_debt_50 INTEGER DEFAULT 0");
        addColumn(db, table, "frequency_target_debt_100 INTEGER DEFAULT 0");
        addColumn(db, table, "frequency_target_debt_300 INTEGER DEFAULT 0");
        addColumn(db, table, "frequency_urgent_window TEXT");
        addColumn(db, table, "frequency_issues_since_last_real_bet INTEGER DEFAULT 999");
        addColumn(db, table, "frequency_issues_since_last_floor_bet INTEGER DEFAULT 999");
        addColumn(db, table, "frequency_consecutive_floor_bets INTEGER DEFAULT 0");
        addColumn(db, table, "frequency_floor_min_gap INTEGER DEFAULT 2147483647");
        addColumn(db, table, "frequency_floor_pacing_ready INTEGER DEFAULT 0");
        addColumn(db, table, "frequency_floor_pacing_reason TEXT");
        addColumn(db, table, "decision_quality_path TEXT");
        addColumn(db, table, "evidence_domain_frozen INTEGER DEFAULT 0");
        addColumn(db, table, "evidence_domain_frozen_issue TEXT");
        addColumn(db, table, "evidence_domain_frozen_at INTEGER DEFAULT 0");
        addColumn(db, table, "dynamic_matched_level TEXT");
        addColumn(db, table, "dynamic_real_sample INTEGER DEFAULT 0");
        addColumn(db, table, "dynamic_shadow_sample INTEGER DEFAULT 0");
        addColumn(db, table, "module_build_version TEXT");
    }

    private void addColumn(SQLiteDatabase db, String table, String columnSql) {
        if (db == null
                || table == null
                || table.length() == 0
                || columnSql == null
                || columnSql.length() == 0) {
            return;
        }

        String columnName = firstToken(columnSql);
        if (columnName.length() == 0) {
            return;
        }

        if (columnExists(db, table, columnName)) {
            return;
        }

        try {
            db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + columnSql);
            SchemaColumnsR94 scope = schemaColumnsR94;
            if (scope != null && scope.owns(db)) {
                java.util.HashSet<String> columns = scope.tables.get(table);
                if (columns != null) columns.add(columnName.toLowerCase(java.util.Locale.ROOT));
            }
        } catch (Throwable e) {
            // SQLite can roll back a transaction after storage/I/O failures. An ALTER
            // failure invalidates every optimistic observation from this scope.
            if (schemaColumnsR94 != null) schemaColumnsR94.tables.clear();
        }
    }

    private boolean columnExists(SQLiteDatabase db, String table, String columnName) {
        SchemaColumnsR94 scope = schemaColumnsR94;
        if (scope != null && !scope.owns(db)) scope = null;
        if (scope != null) {
            java.util.HashSet<String> columns = scope.tables.get(table);
            if (columns != null)
                return columns.contains(columnName.toLowerCase(java.util.Locale.ROOT));
        }
        Cursor c = null;
        try {
            c = db.rawQuery("PRAGMA table_info(" + table + ")", null);
            if (c == null) {
                return false;
            }
            int nameIndex = c.getColumnIndex("name");
            java.util.HashSet<String> observed = scope == null ? null
                    : new java.util.HashSet<String>();
            boolean found = false;
            while (c.moveToNext()) {
                String name = c.getString(nameIndex);
                if (observed != null) observed.add(name.toLowerCase(java.util.Locale.ROOT));
                if (columnName.equalsIgnoreCase(name)) {
                    if (observed == null) return true;
                    found = true;
                }
            }
            // An absent table is not a reusable schema snapshot: a later CREATE may
            // establish it inside this transaction.
            if (scope != null && !observed.isEmpty()) scope.tables.put(table, observed);
            return found;
        } catch (Throwable e) {
            if (scope != null) scope.tables.clear();
            return false;
        } finally {
            if (c != null) {
                try {
                    c.close();
                } catch (Throwable ignore) {
                }
            }
        }
    }

    private String firstToken(String text) {
        if (text == null) {
            return "";
        }
        String t = text.trim();
        if (t.length() == 0) {
            return "";
        }
        int p = t.indexOf(' ');
        if (p <= 0) {
            return t;
        }
        return t.substring(0, p).trim();
    }

    private void execQuiet(SQLiteDatabase db, String sql) {
        try {
            db.execSQL(sql);
        } catch (Throwable e) {
        }
    }

    private String quote(String value) {
        if (value == null) {
            return "''";
        }
        return "'" + value.replace("'", "''") + "'";
    }

    @Deprecated
    public void dropAll(SQLiteDatabase db) {
        throw new SQLiteException("CP5_DESTRUCTIVE_RESET_RETIRED_USE_VERIFIED_RESTORE");
    }

    /**
     * Additive R3.5 runtime schema. No table is dropped and no historical row is rewritten.
     * target_issue is the immutable idempotency key.
     */
    public static void ensureR35Schema(SQLiteDatabase db) {
        if (db == null) throw new IllegalArgumentException("R35_DB_NULL");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + TABLE_R35_SHADOW_OBSERVATION
                        + "(target_issue TEXT PRIMARY KEY,game_code TEXT NOT NULL,base_issue TEXT"
                        + " NOT NULL,position INTEGER NOT NULL,n1 INTEGER NOT NULL,n2 INTEGER NOT"
                        + " NULL,n3 INTEGER NOT NULL,selected_hit_p REAL NOT NULL DEFAULT"
                        + " -1,predictability REAL NOT NULL DEFAULT 0,lane TEXT NOT"
                        + " NULL,decision_action TEXT NOT NULL,reject_code TEXT NOT"
                        + " NULL,bundle_hash TEXT NOT NULL,contract_hash TEXT NOT"
                        + " NULL,authorization_hash TEXT NOT NULL,record_hash TEXT NOT NULL"
                        + " UNIQUE,pre_draw_frozen INTEGER NOT NULL DEFAULT 1,model_proven INTEGER"
                        + " NOT NULL DEFAULT 0,created_at INTEGER NOT NULL,settled INTEGER NOT NULL"
                        + " DEFAULT 0,actual_number INTEGER NOT NULL DEFAULT 0,hit_label INTEGER"
                        + " NOT NULL DEFAULT -1,settled_at INTEGER NOT NULL DEFAULT"
                        + " 0,settlement_hash TEXT NOT NULL DEFAULT '',error_code TEXT NOT NULL"
                        + " DEFAULT '',CHECK(game_code='Binance1-PK10'),CHECK(position BETWEEN 1"
                        + " AND 10),CHECK(n1 BETWEEN 1 AND 10),CHECK(n2 BETWEEN 1 AND 10),CHECK(n3"
                        + " BETWEEN 1 AND 10),CHECK(pre_draw_frozen=1),CHECK(model_proven IN"
                        + " (0,1)),CHECK(settled IN (0,1)))");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + TABLE_R35_OOS_LABEL
                        + "(target_issue TEXT PRIMARY KEY,game_code TEXT NOT NULL,base_issue TEXT"
                        + " NOT NULL,position INTEGER NOT NULL,n1 INTEGER NOT NULL,n2 INTEGER NOT"
                        + " NULL,n3 INTEGER NOT NULL,selected_hit_p REAL NOT NULL DEFAULT"
                        + " -1,actual_number INTEGER NOT NULL,hit_label INTEGER NOT NULL,strict_oos"
                        + " INTEGER NOT NULL,pre_draw_frozen INTEGER NOT NULL,prediction_created_at"
                        + " INTEGER NOT NULL,draw_open_time INTEGER NOT NULL DEFAULT 0,settled_at"
                        + " INTEGER NOT NULL,brier REAL NOT NULL DEFAULT -1,provenance_hash TEXT"
                        + " NOT NULL UNIQUE,record_hash TEXT NOT NULL UNIQUE,error_code TEXT NOT"
                        + " NULL DEFAULT '',CHECK(game_code='Binance1-PK10'),CHECK(position BETWEEN"
                        + " 1 AND 10),CHECK(hit_label IN (0,1)),CHECK(strict_oos IN"
                        + " (0,1)),CHECK(pre_draw_frozen=1))");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + TABLE_R35_RUNTIME_HEALTH
                        + "(_id INTEGER PRIMARY KEY AUTOINCREMENT,event_at INTEGER NOT NULL,stage"
                        + " TEXT NOT NULL,target_issue TEXT NOT NULL DEFAULT '',status TEXT NOT"
                        + " NULL,elapsed_ms INTEGER NOT NULL DEFAULT 0,active_concurrency INTEGER"
                        + " NOT NULL DEFAULT 0,max_concurrency INTEGER NOT NULL DEFAULT 0,oom_count"
                        + " INTEGER NOT NULL DEFAULT 0,detail TEXT NOT NULL DEFAULT '',record_hash"
                        + " TEXT NOT NULL UNIQUE)");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + TABLE_R35_DEFERRED_EVIDENCE_JOB
                        + "(target_issue TEXT PRIMARY KEY,job_type TEXT NOT NULL,status TEXT NOT"
                        + " NULL,attempts INTEGER NOT NULL DEFAULT 0,bundle_hash TEXT NOT NULL"
                        + " DEFAULT '',created_at INTEGER NOT NULL,updated_at INTEGER NOT"
                        + " NULL,last_error TEXT NOT NULL DEFAULT '',record_hash TEXT NOT NULL"
                        + " UNIQUE)");
        db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_r35_shadow_settled ON "
                        + TABLE_R35_SHADOW_OBSERVATION
                        + "(settled,target_issue)");
        db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_r35_shadow_created ON "
                        + TABLE_R35_SHADOW_OBSERVATION
                        + "(created_at DESC)");
        db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_r35_oos_settled ON "
                        + TABLE_R35_OOS_LABEL
                        + "(strict_oos,settled_at DESC)");
        db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_r35_health_stage ON "
                        + TABLE_R35_RUNTIME_HEALTH
                        + "(stage,event_at DESC)");
        db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_r35_deferred_status ON "
                        + TABLE_R35_DEFERRED_EVIDENCE_JOB
                        + "(status,updated_at)");
        // Every canonical writer needs reason receipts, even before Research is initialized.
        // Fresh SQLiteOpenHelper creation and additive overwrite repair share this owner.
        boolean ownReasonTx = !db.inTransaction();
        if (ownReasonTx) db.beginTransaction();
        try {
            com.ethk3.novaonepro.r952.CanonicalSchemaCP12.installReasons(db);
            if (ownReasonTx) db.setTransactionSuccessful();
        } finally { if (ownReasonTx) db.endTransaction(); }
    }

    /**
     * DB36 is additive-only. This method is called by the single database-startup schema owner
     * (including same-version overwrite installs); active R3.6 reads and business writes never
     * repair schema.
     */
    public static void ensureR36Schema(SQLiteDatabase db) {
        if (db == null) throw new IllegalArgumentException("R36_DB_NULL");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + TABLE_R36_ANALYSIS_REQUEST
                        + "(target_issue TEXT PRIMARY KEY,base_issue TEXT NOT NULL,status TEXT NOT"
                        + " NULL,generation INTEGER NOT NULL,request_at INTEGER NOT"
                        + " NULL,deadline_at INTEGER NOT NULL DEFAULT 0,started_at INTEGER NOT NULL"
                        + " DEFAULT 0,finished_at INTEGER NOT NULL DEFAULT 0,lease_owner TEXT NOT"
                        + " NULL DEFAULT '',lease_until INTEGER NOT NULL DEFAULT 0,attempts INTEGER"
                        + " NOT NULL DEFAULT 0,last_stage TEXT NOT NULL DEFAULT '',error_code TEXT"
                        + " NOT NULL DEFAULT '',history_hash TEXT NOT NULL DEFAULT"
                        + " '',canonical_hash TEXT NOT NULL DEFAULT '',request_hash TEXT NOT NULL"
                        + " UNIQUE,next_attempt_at INTEGER NOT NULL DEFAULT 0,repair_version TEXT NOT"
                        + " NULL DEFAULT '',first_error_code TEXT NOT NULL DEFAULT '',last_transition_at"
                        + " INTEGER NOT NULL DEFAULT 0)");
        db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_r36_analysis_status ON "
                        + TABLE_R36_ANALYSIS_REQUEST
                        + "(status,lease_until,request_at)");
        addColumnR36(
                db,
                TABLE_R36_ANALYSIS_REQUEST,
                "next_attempt_at INTEGER NOT NULL DEFAULT 0");
        addColumnR36(
                db,
                TABLE_R36_ANALYSIS_REQUEST,
                "repair_version TEXT NOT NULL DEFAULT ''");
        addColumnR36(
                db,
                TABLE_R36_ANALYSIS_REQUEST,
                "first_error_code TEXT NOT NULL DEFAULT ''");
        addColumnR36(
                db,
                TABLE_R36_ANALYSIS_REQUEST,
                "last_transition_at INTEGER NOT NULL DEFAULT 0");
        db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_r36_analysis_runnable ON "
                        + TABLE_R36_ANALYSIS_REQUEST
                        + "(status,next_attempt_at,lease_until,target_issue)");

        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + TABLE_R36_SETTLEMENT_TASK
                        + "(target_issue TEXT PRIMARY KEY,draw_issue TEXT NOT NULL,status TEXT NOT"
                        + " NULL,created_at INTEGER NOT NULL,updated_at INTEGER NOT"
                        + " NULL,completed_at INTEGER NOT NULL DEFAULT 0,lease_owner TEXT NOT NULL"
                        + " DEFAULT '',lease_until INTEGER NOT NULL DEFAULT 0,attempts INTEGER NOT"
                        + " NULL DEFAULT 0,next_attempt_at INTEGER NOT NULL DEFAULT 0,draw_hash"
                        + " TEXT NOT NULL,settlement_hash TEXT NOT NULL DEFAULT '',last_error TEXT"
                        + " NOT NULL DEFAULT '')");
        db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_r36_settlement_status ON "
                        + TABLE_R36_SETTLEMENT_TASK
                        + "(status,next_attempt_at,lease_until)");

        addColumnR36(
                db, TABLE_R35_SHADOW_OBSERVATION, "analysis_attempt_id TEXT NOT NULL DEFAULT ''");
        addColumnR36(db, TABLE_R35_SHADOW_OBSERVATION, "history_hash TEXT NOT NULL DEFAULT ''");
        addColumnR36(
                db, TABLE_R35_SHADOW_OBSERVATION, "history_max_issue TEXT NOT NULL DEFAULT ''");
        addColumnR36(
                db, TABLE_R35_SHADOW_OBSERVATION, "action_space_hash TEXT NOT NULL DEFAULT ''");
        addColumnR36(db, TABLE_R35_SHADOW_OBSERVATION, "snapshot_hash TEXT NOT NULL DEFAULT ''");
        addColumnR36(db, TABLE_R35_SHADOW_OBSERVATION, "request_at INTEGER NOT NULL DEFAULT 0");
        addColumnR36(db, TABLE_R35_SHADOW_OBSERVATION, "commit_at INTEGER NOT NULL DEFAULT 0");
        addColumnR36(db, TABLE_R35_SHADOW_OBSERVATION, "deadline_at INTEGER NOT NULL DEFAULT 0");
        addColumnR36(db, TABLE_R35_SHADOW_OBSERVATION, "runtime_version TEXT NOT NULL DEFAULT ''");
        SettlementTaskStoreR36.ensureRepairAuditSchema(db);

        addColumnR36(db, TABLE_R35_DEFERRED_EVIDENCE_JOB, "snapshot_blob BLOB");
        addColumnR36(db, TABLE_R35_DEFERRED_EVIDENCE_JOB, "snapshot_hash TEXT NOT NULL DEFAULT ''");
        addColumnR36(
                db, TABLE_R35_DEFERRED_EVIDENCE_JOB, "snapshot_schema INTEGER NOT NULL DEFAULT 0");
        addColumnR36(db, TABLE_R35_DEFERRED_EVIDENCE_JOB, "lease_owner TEXT NOT NULL DEFAULT ''");
        addColumnR36(db, TABLE_R35_DEFERRED_EVIDENCE_JOB, "lease_until INTEGER NOT NULL DEFAULT 0");
        addColumnR36(
                db, TABLE_R35_DEFERRED_EVIDENCE_JOB, "next_attempt_at INTEGER NOT NULL DEFAULT 0");
        addColumnR36(
                db, TABLE_R35_DEFERRED_EVIDENCE_JOB, "completed_at INTEGER NOT NULL DEFAULT 0");
        addColumnR36(db, TABLE_R35_DEFERRED_EVIDENCE_JOB, "number_rows INTEGER NOT NULL DEFAULT 0");
        addColumnR36(db, TABLE_R35_DEFERRED_EVIDENCE_JOB, "action_rows INTEGER NOT NULL DEFAULT 0");
        addColumnR36(db, TABLE_R35_DEFERRED_EVIDENCE_JOB, "last_stage TEXT NOT NULL DEFAULT ''");

        addColumnR36(db, TABLE_R35_RUNTIME_HEALTH, "attempt_id TEXT NOT NULL DEFAULT ''");
        addColumnR36(db, TABLE_R35_RUNTIME_HEALTH, "event_type TEXT NOT NULL DEFAULT ''");
        addColumnR36(db, TABLE_R35_RUNTIME_HEALTH, "request_at INTEGER NOT NULL DEFAULT 0");
        addColumnR36(db, TABLE_R35_RUNTIME_HEALTH, "commit_at INTEGER NOT NULL DEFAULT 0");
        addColumnR36(db, TABLE_R35_RUNTIME_HEALTH, "terminal INTEGER NOT NULL DEFAULT 0");
    }

    /**
     * R811 critical extension for both fresh and same-version DB36 installs. The single startup
     * coordinator owns this DDL; no live business path is allowed to invoke it.
     */
    public static void ensureR811CriticalSchema(SQLiteDatabase db) {
        if (db == null) throw new IllegalArgumentException("R811_DB_NULL");
        addColumnR36(
                db,
                TABLE_R36_SETTLEMENT_TASK,
                "task_class TEXT NOT NULL DEFAULT 'BACKLOG'");
        addColumnR36(db, TABLE_R36_SETTLEMENT_TASK, "priority INTEGER NOT NULL DEFAULT 0");
        addColumnR36(
                db,
                TABLE_R36_SETTLEMENT_TASK,
                "terminal_code TEXT NOT NULL DEFAULT ''");
        addColumnR36(db, TABLE_R36_SETTLEMENT_TASK, "source TEXT NOT NULL DEFAULT ''");
        addColumnR36(
                db, TABLE_OOS_ACTION_LOG, "settlement_task_id TEXT NOT NULL DEFAULT ''");
        addColumnR36(db, TABLE_OOS_ACTION_LOG, "error_code TEXT NOT NULL DEFAULT ''");
        addColumnR36(
                db,
                TABLE_R35_DEFERRED_EVIDENCE_JOB,
                "core_materialized INTEGER NOT NULL DEFAULT 0");
        addColumnR36(db,TABLE_R35_DEFERRED_EVIDENCE_JOB,"core_next_ordinal INTEGER NOT NULL DEFAULT 0");
        addColumnR36(
                db,
                TABLE_R35_DEFERRED_EVIDENCE_JOB,
                "next_action_ordinal INTEGER NOT NULL DEFAULT 0");
        addColumnR36(
                db,
                TABLE_R35_DEFERRED_EVIDENCE_JOB,
                "cursor_version INTEGER NOT NULL DEFAULT 1");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + TABLE_R811_DRAW_PROVENANCE
                        + "(provenance_id TEXT PRIMARY KEY,endpoint_id TEXT NOT NULL,"
                        + "request_at INTEGER NOT NULL,response_at INTEGER NOT NULL,result_code TEXT NOT NULL,"
                        + "raw_payload_sha256 TEXT NOT NULL,parsed_issue TEXT NOT NULL,permutation TEXT NOT NULL,"
                        + "server_open_time INTEGER NOT NULL,parser_version TEXT NOT NULL,source_line TEXT NOT NULL,"
                        + "draw_hash TEXT NOT NULL,observed_at INTEGER NOT NULL)" );
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + TABLE_R811_DRAW_CANONICAL
                        + "(target_issue TEXT PRIMARY KEY,status TEXT NOT NULL,canonical_draw_hash TEXT NOT NULL DEFAULT '',"
                        + "evidence_hash TEXT NOT NULL,source_count INTEGER NOT NULL DEFAULT 0,"
                        + "conflict_count INTEGER NOT NULL DEFAULT 0,updated_at INTEGER NOT NULL)" );
        db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_r811_settlement_runnable ON "
                        + TABLE_R36_SETTLEMENT_TASK
                        + "(priority DESC,next_attempt_at,created_at)");
        db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_r811_deferred_runnable ON "
                        + TABLE_R35_DEFERRED_EVIDENCE_JOB
                        + "(status,next_attempt_at,lease_until,updated_at)");
        db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_r811_draw_provenance_issue ON "
                        + TABLE_R811_DRAW_PROVENANCE
                        + "(parsed_issue,observed_at,source_line)");
        db.execSQL(
                "INSERT OR REPLACE INTO "
                        + TABLE_APP_STATE
                        + "(state_key,state_value,updated_at) VALUES(?,?,?)",
                new Object[] {
                    "r811_schema_revision",
                    String.valueOf(AppConfig.R811_SCHEMA_REVISION),
                    System.currentTimeMillis()
                });
    }

    /** Lazy Research-only extension. Call only after realtime pressure has cleared. */
    public static void ensureR811ResearchSchema(SQLiteDatabase db) {
        if (db == null) throw new IllegalArgumentException("R811_RESEARCH_DB_NULL");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS v57_research_run_r811("
                        + "run_id TEXT PRIMARY KEY,state TEXT NOT NULL,target_issue TEXT NOT NULL DEFAULT '',"
                        + "data_until_issue TEXT NOT NULL DEFAULT '',input_hash TEXT NOT NULL,dataset_hash TEXT NOT NULL DEFAULT '',"
                        + "champion_hash TEXT NOT NULL DEFAULT '',generated_count INTEGER NOT NULL DEFAULT 0,"
                        + "evaluated_count INTEGER NOT NULL DEFAULT 0,shadow_eligible_count INTEGER NOT NULL DEFAULT 0,"
                        + "error_code TEXT NOT NULL DEFAULT '',error_detail TEXT NOT NULL DEFAULT '',"
                        + "created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL)");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS v57_research_hypothesis_r811("
                        + "hypothesis_id TEXT PRIMARY KEY,run_id TEXT NOT NULL,failure_layer TEXT NOT NULL,"
                        + "mechanism TEXT NOT NULL,positive_evidence TEXT NOT NULL,counter_evidence TEXT NOT NULL,"
                        + "affected_regime TEXT NOT NULL DEFAULT '',candidate_family TEXT NOT NULL,"
                        + "acceptance_json TEXT NOT NULL,status TEXT NOT NULL,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL)");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS v57_research_candidate_r811("
                        + "candidate_id TEXT PRIMARY KEY,run_id TEXT NOT NULL,hypothesis_id TEXT NOT NULL DEFAULT '',"
                        + "candidate_type TEXT NOT NULL,parent_champion_hash TEXT NOT NULL,canonical_spec TEXT NOT NULL,"
                        + "dataset_hash TEXT NOT NULL DEFAULT '',"
                        + "formula_ast TEXT NOT NULL DEFAULT '',changed_symbols TEXT NOT NULL,forbidden_symbols TEXT NOT NULL,"
                        + "seed INTEGER NOT NULL,complexity REAL NOT NULL DEFAULT 0,code_hash TEXT NOT NULL DEFAULT '',"
                        + "generation INTEGER NOT NULL DEFAULT 0,genetic_operator TEXT NOT NULL DEFAULT 'DIRECT_SPEC',"
                        + "parent_formula_ids TEXT NOT NULL DEFAULT '',feature_dependencies TEXT NOT NULL DEFAULT '',"
                        + "artifact_hash TEXT NOT NULL DEFAULT '',runtime_executable INTEGER NOT NULL DEFAULT 0,"
                        + "status TEXT NOT NULL,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL)");
        addColumnR36(
                db, "v57_research_candidate_r811", "dataset_hash TEXT NOT NULL DEFAULT ''");
        addColumnR36(
                db, "v57_research_candidate_r811", "generation INTEGER NOT NULL DEFAULT 0");
        addColumnR36(
                db,
                "v57_research_candidate_r811",
                "genetic_operator TEXT NOT NULL DEFAULT 'DIRECT_SPEC'");
        addColumnR36(
                db,
                "v57_research_candidate_r811",
                "parent_formula_ids TEXT NOT NULL DEFAULT ''");
        addColumnR36(
                db,
                "v57_research_candidate_r811",
                "feature_dependencies TEXT NOT NULL DEFAULT ''");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS v57_research_experiment_r811("
                        + "experiment_id TEXT PRIMARY KEY,candidate_id TEXT NOT NULL,fold_id TEXT NOT NULL,"
                        + "split_role TEXT NOT NULL,start_issue TEXT NOT NULL,end_issue TEXT NOT NULL,n INTEGER NOT NULL,"
                        + "eligible_n INTEGER NOT NULL DEFAULT 0,selected_n INTEGER NOT NULL DEFAULT 0,"
                        + "hit_n INTEGER NOT NULL DEFAULT 0,"
                        + "hit_rate REAL,coverage REAL,wilson_lower REAL,beta_lower REAL,ece REAL,brier REAL,coverage_cv REAL,"
                        + "longest_lose INTEGER,latency_p95_ms REAL,ram_peak_bytes INTEGER,p_value REAL,"
                        + "fdr_pass INTEGER NOT NULL DEFAULT 0,result_hash TEXT NOT NULL,created_at INTEGER NOT NULL)");
        addColumnR36(
                db, "v57_research_experiment_r811", "eligible_n INTEGER NOT NULL DEFAULT 0");
        addColumnR36(
                db, "v57_research_experiment_r811", "selected_n INTEGER NOT NULL DEFAULT 0");
        addColumnR36(
                db, "v57_research_experiment_r811", "hit_n INTEGER NOT NULL DEFAULT 0");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS v57_research_codegen_r811("
                        + "candidate_id TEXT PRIMARY KEY,bundle_path TEXT NOT NULL DEFAULT '',java_files TEXT NOT NULL DEFAULT '',"
                        + "patch_sha256 TEXT NOT NULL DEFAULT '',manifest_sha256 TEXT NOT NULL DEFAULT '',"
                        + "compile_status TEXT NOT NULL DEFAULT 'NOT_RUN',owner_audit_status TEXT NOT NULL DEFAULT 'NOT_RUN',"
                        + "java_source TEXT NOT NULL DEFAULT '',patch_text TEXT NOT NULL DEFAULT '',"
                        + "manifest_text TEXT NOT NULL DEFAULT '',fixtures_text TEXT NOT NULL DEFAULT '',"
                        + "bundle_hash TEXT NOT NULL DEFAULT '',generated_at INTEGER NOT NULL)");
        /* Same-version DB36 installs may already own the initial R811 table shape. */
        addColumnR36(
                db, "v57_research_codegen_r811", "java_source TEXT NOT NULL DEFAULT ''");
        addColumnR36(
                db, "v57_research_codegen_r811", "patch_text TEXT NOT NULL DEFAULT ''");
        addColumnR36(
                db, "v57_research_codegen_r811", "manifest_text TEXT NOT NULL DEFAULT ''");
        addColumnR36(
                db, "v57_research_codegen_r811", "fixtures_text TEXT NOT NULL DEFAULT ''");
        addColumnR36(
                db, "v57_research_codegen_r811", "bundle_hash TEXT NOT NULL DEFAULT ''");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS v57_research_expert_predraw_r811("
                        + "target_issue TEXT PRIMARY KEY,base_issue TEXT NOT NULL,history_hash TEXT NOT NULL,"
                        + "canonical_hash TEXT NOT NULL,schema_version INTEGER NOT NULL,expert_count INTEGER NOT NULL,"
                        + "blob BLOB NOT NULL,blob_sha256 TEXT NOT NULL,frozen_at INTEGER NOT NULL,created_at INTEGER NOT NULL)");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS v57_research_expert_outcome_r811("
                        + "target_issue TEXT NOT NULL,expert_id TEXT NOT NULL,position INTEGER NOT NULL,regime_id TEXT NOT NULL DEFAULT '',"
                        + "actual_number INTEGER NOT NULL,p_actual REAL NOT NULL,log_loss REAL NOT NULL,multiclass_brier REAL NOT NULL,"
                        + "actual_rank INTEGER NOT NULL,k3_probability REAL NOT NULL DEFAULT -1,k3_hit INTEGER NOT NULL DEFAULT -1,"
                        + "record_hash TEXT NOT NULL UNIQUE,created_at INTEGER NOT NULL,PRIMARY KEY(target_issue,expert_id,position))");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS v57_research_action_label_r811("
                        + "target_issue TEXT NOT NULL,global_ordinal INTEGER NOT NULL,action_hash TEXT NOT NULL,position INTEGER NOT NULL,"
                        + "n1 INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,actual_number INTEGER NOT NULL,"
                        + "hit_label INTEGER NOT NULL,label_hash TEXT NOT NULL UNIQUE,created_at INTEGER NOT NULL,"
                        + "PRIMARY KEY(target_issue,global_ordinal))");
        // R900 dynamic Research lifecycle: compact live score + bounded prune audit.
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS v57_research_candidate_score_r900("
                        + "candidate_id TEXT PRIMARY KEY,future_n INTEGER NOT NULL DEFAULT 0,"
                        + "hit_n INTEGER NOT NULL DEFAULT 0,hit_rate REAL NOT NULL DEFAULT 0,"
                        + "wilson_lower REAL NOT NULL DEFAULT 0,wilson_upper REAL NOT NULL DEFAULT 1,"
                        + "brier REAL NOT NULL DEFAULT -1,state TEXT NOT NULL DEFAULT 'INCUBATING',"
                        + "last_issue TEXT NOT NULL DEFAULT '',updated_at INTEGER NOT NULL DEFAULT 0)");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS v57_research_prune_log_r900("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT,candidate_id TEXT NOT NULL,reason TEXT NOT NULL,"
                        + "future_n INTEGER NOT NULL,hit_n INTEGER NOT NULL,hit_rate REAL NOT NULL,"
                        + "wilson_upper REAL NOT NULL,snapshot_hash TEXT NOT NULL,created_at INTEGER NOT NULL)");
        db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_r900_candidate_score_state ON "
                        + "v57_research_candidate_score_r900(state,updated_at)");
        db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_r900_prune_created ON "
                        + "v57_research_prune_log_r900(created_at)");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS v57_research_candidate_outcome_r811("
                        + "candidate_id TEXT NOT NULL,target_issue TEXT NOT NULL,selected_ordinal INTEGER NOT NULL,"
                        + "score REAL NOT NULL,probability REAL NOT NULL,hit_label INTEGER NOT NULL,regime_id TEXT NOT NULL,"
                        + "ablated_feature TEXT NOT NULL,ablated_ordinal INTEGER NOT NULL,ablated_score REAL NOT NULL,"
                        + "ablated_probability REAL NOT NULL,ablated_hit_label INTEGER NOT NULL,ablated_regime_id TEXT NOT NULL,"
                        + "latency_ms REAL NOT NULL,ram_peak_bytes INTEGER NOT NULL,prediction_hash TEXT NOT NULL UNIQUE,"
                        + "created_at INTEGER NOT NULL,PRIMARY KEY(candidate_id,target_issue))");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + TABLE_R817_RESEARCH_PREDRAW
                        + "(candidate_id TEXT NOT NULL,target_issue TEXT NOT NULL,phase TEXT NOT NULL,"
                        + "position INTEGER NOT NULL,n1 INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,"
                        + "selected_ordinal INTEGER NOT NULL,score REAL NOT NULL,probability REAL NOT NULL,"
                        + "formula_probability REAL NOT NULL DEFAULT 0,adaptive_probability REAL NOT NULL DEFAULT -1,"
                        + "adaptive_artifact_hash TEXT NOT NULL DEFAULT '',adaptive_trained_until TEXT NOT NULL DEFAULT '',"
                        + "adaptive_updates INTEGER NOT NULL DEFAULT 0,adaptive_weight REAL NOT NULL DEFAULT 0,"
                        + "formula_hash TEXT NOT NULL,input_hash TEXT NOT NULL,frozen_at INTEGER NOT NULL,"
                        + "actual_number INTEGER NOT NULL DEFAULT 0,hit_label INTEGER NOT NULL DEFAULT -1,"
                        + "settled_at INTEGER NOT NULL DEFAULT 0,outcome_hash TEXT NOT NULL DEFAULT '',"
                        + "record_hash TEXT NOT NULL UNIQUE,PRIMARY KEY(candidate_id,target_issue))");
        /* Same-version interrupted installs are repaired additively; no user data is reset. */
        addColumnR36(
                db,
                TABLE_R817_RESEARCH_PREDRAW,
                "formula_probability REAL NOT NULL DEFAULT 0");
        addColumnR36(
                db,
                TABLE_R817_RESEARCH_PREDRAW,
                "adaptive_probability REAL NOT NULL DEFAULT -1");
        addColumnR36(
                db,
                TABLE_R817_RESEARCH_PREDRAW,
                "adaptive_artifact_hash TEXT NOT NULL DEFAULT ''");
        addColumnR36(
                db,
                TABLE_R817_RESEARCH_PREDRAW,
                "adaptive_trained_until TEXT NOT NULL DEFAULT ''");
        addColumnR36(
                db,
                TABLE_R817_RESEARCH_PREDRAW,
                "adaptive_updates INTEGER NOT NULL DEFAULT 0");
        addColumnR36(
                db,
                TABLE_R817_RESEARCH_PREDRAW,
                "adaptive_weight REAL NOT NULL DEFAULT 0");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS v57_research_artifact_r811("
                        + "artifact_id TEXT PRIMARY KEY,artifact_type TEXT NOT NULL,trained_until_issue TEXT NOT NULL,"
                        + "feature_schema_hash TEXT NOT NULL,artifact_blob BLOB NOT NULL,artifact_hash TEXT NOT NULL UNIQUE,"
                        + "status TEXT NOT NULL,created_at INTEGER NOT NULL)");
        db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_r811_research_candidate_status ON "
                        + "v57_research_candidate_r811(status,updated_at)");
        db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_r811_research_experiment_candidate ON "
                        + "v57_research_experiment_r811(candidate_id,split_role,start_issue)");
        db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_r811_action_label_target ON "
                        + "v57_research_action_label_r811(target_issue,global_ordinal)");
        db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_r811_candidate_outcome_issue ON "
                        + "v57_research_candidate_outcome_r811(candidate_id,target_issue)");
        db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_r817_research_predraw_target ON "
                        + TABLE_R817_RESEARCH_PREDRAW
                        + "(target_issue,hit_label,frozen_at)");
        db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_r811_research_artifact_trained ON "
                        + "v57_research_artifact_r811(artifact_type,trained_until_issue)");
    }

    /**
     * R815 is additive while DB_VERSION remains 36.  Every column addition is guarded through
     * PRAGMA table_info, which makes same-version coverage installs and interrupted retries safe.
     */
    public static void ensureR815CriticalSchema(SQLiteDatabase db) {
        if (db == null) throw new IllegalArgumentException("R815_CRITICAL_DB_NULL");
        addColumnR36(db, TABLE_DRAWS, "first_seen_at INTEGER NOT NULL DEFAULT 0");
        addColumnR36(db, TABLE_DRAWS, "draw_hash_version INTEGER NOT NULL DEFAULT 1");
        addColumnR36(db, TABLE_DRAWS, "draw_hash_v2 TEXT NOT NULL DEFAULT ''");
        DrawDependencyInputCP12.schemaWithinMigration(db);
        db.execSQL("UPDATE " + TABLE_DRAWS + " SET first_seen_at=CASE WHEN fetch_time>0 THEN fetch_time WHEN open_time>0 THEN open_time ELSE ? END WHERE first_seen_at<=0",
                new Object[] {Long.valueOf(System.currentTimeMillis())});
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_draws_issue_id_r815 ON " + TABLE_DRAWS + "(issue DESC,_id DESC)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_draws_first_seen_r815 ON " + TABLE_DRAWS + "(first_seen_at)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_R815_CRITICAL_ERROR + "(id INTEGER PRIMARY KEY AUTOINCREMENT,event_key TEXT NOT NULL UNIQUE,domain TEXT NOT NULL,target_issue TEXT NOT NULL DEFAULT '',generation INTEGER NOT NULL DEFAULT 0,error_code TEXT NOT NULL,throwable_class TEXT NOT NULL DEFAULT '',detail TEXT NOT NULL DEFAULT '',retryable INTEGER NOT NULL DEFAULT 0,created_at INTEGER NOT NULL,resolved_at INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_critical_error_open_r815 ON " + TABLE_R815_CRITICAL_ERROR + "(resolved_at,domain,created_at)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_R815_LONG_TEXT + "(token TEXT NOT NULL,chunk_index INTEGER NOT NULL,chunk_text TEXT NOT NULL,chunk_sha256 TEXT NOT NULL,report_sha256 TEXT NOT NULL,created_at INTEGER NOT NULL,expires_at INTEGER NOT NULL,PRIMARY KEY(token,chunk_index))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_long_text_expiry_r815 ON " + TABLE_R815_LONG_TEXT + "(expires_at)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_R815_RECOVERY_GENERATION + "(generation INTEGER PRIMARY KEY,state TEXT NOT NULL,trigger_mask INTEGER NOT NULL DEFAULT 0,attempt_count INTEGER NOT NULL DEFAULT 0,window_started_at INTEGER NOT NULL,last_attempt_at INTEGER NOT NULL DEFAULT 0,completed_at INTEGER NOT NULL DEFAULT 0,detail TEXT NOT NULL DEFAULT '')");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_R815_SHADOW_SIDECAR + "(target_issue TEXT PRIMARY KEY,settlement_hash TEXT NOT NULL,canonical_hash TEXT NOT NULL,actual_number INTEGER NOT NULL,hit_label INTEGER NOT NULL,created_at INTEGER NOT NULL,retry_count INTEGER NOT NULL DEFAULT 0,last_error TEXT NOT NULL DEFAULT '',CHECK(hit_label IN (0,1)))");
    }

    public static void ensureR815ResearchSchema(SQLiteDatabase db) {
        if (db == null) throw new IllegalArgumentException("R815_RESEARCH_DB_NULL");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_R815_RESEARCH_CANDIDATE + "(candidate_id TEXT PRIMARY KEY,candidate_type TEXT NOT NULL,genome_sha256 TEXT NOT NULL,schema_sha256 TEXT NOT NULL,dataset_sha256 TEXT NOT NULL,game_code TEXT NOT NULL,state TEXT NOT NULL,created_target_issue TEXT NOT NULL,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL,detail TEXT NOT NULL DEFAULT '')");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_R815_RESEARCH_EVAL + "(candidate_id TEXT NOT NULL,target_issue TEXT NOT NULL,artifact_sha256 TEXT NOT NULL,selected INTEGER NOT NULL,hit INTEGER NOT NULL,selected_p REAL,brier REAL,log_loss REAL,block_id TEXT NOT NULL,regime_id TEXT NOT NULL,settled_at INTEGER NOT NULL,record_hash TEXT NOT NULL,PRIMARY KEY(candidate_id,target_issue))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_research_eval_target_r815 ON " + TABLE_R815_RESEARCH_EVAL + "(target_issue,candidate_id)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_R815_RESEARCH_ARTIFACT + "(artifact_id TEXT PRIMARY KEY,candidate_id TEXT NOT NULL,trained_until_issue TEXT NOT NULL,schema_sha256 TEXT NOT NULL,dataset_sha256 TEXT NOT NULL,artifact_sha256 TEXT NOT NULL UNIQUE,status TEXT NOT NULL,created_at INTEGER NOT NULL,detail TEXT NOT NULL DEFAULT '')");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_R815_RESEARCH_QUEUE + "(target_issue TEXT PRIMARY KEY,settlement_hash TEXT NOT NULL,state TEXT NOT NULL,attempts INTEGER NOT NULL DEFAULT 0,next_attempt_at INTEGER NOT NULL DEFAULT 0,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL,last_error TEXT NOT NULL DEFAULT '')");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_research_queue_runnable_r815 ON " + TABLE_R815_RESEARCH_QUEUE + "(state,next_attempt_at,created_at)");
    }

    /** Compatibility entry point for explicit full-schema consumers. */
    public static void ensureR815Schema(SQLiteDatabase db) {
        ensureR815CriticalSchema(db);
        ensureR815ResearchSchema(db);
        db.execSQL("INSERT OR REPLACE INTO " + TABLE_APP_STATE + "(state_key,state_value,updated_at) VALUES(?,?,?)",
                new Object[] {"r815_schema_revision", String.valueOf(AppConfig.R815_SCHEMA_REVISION), Long.valueOf(System.currentTimeMillis())});
    }

    /** R906 same-DB-version additive schema. Critical closure tables fail closed; Research remains fail-open to SHADOW-only runtime. */
    /** R926 critical-only R906 schema. No Research DDL is permitted on this path. */
    public static void ensureR906CriticalSchema(SQLiteDatabase db) {
        if (db == null) throw new IllegalArgumentException("R906_CRITICAL_DB_NULL");
        final long now = System.currentTimeMillis();
        addColumnR36(db, TABLE_R36_SETTLEMENT_TASK, "lease_generation INTEGER NOT NULL DEFAULT 0");
        addColumnR36(db, TABLE_R36_SETTLEMENT_TASK, "process_boot_id TEXT NOT NULL DEFAULT ''");
        addColumnR36(db, TABLE_R36_SETTLEMENT_TASK, "last_progress_at INTEGER NOT NULL DEFAULT 0");
        addColumnR36(db, TABLE_R36_SETTLEMENT_TASK, "eligibility TEXT NOT NULL DEFAULT 'UNKNOWN'");
        addColumnR36(db, TABLE_R36_SETTLEMENT_TASK, "failure_class TEXT NOT NULL DEFAULT ''");
        addColumnR36(db, TABLE_R36_SETTLEMENT_TASK, "recovery_count INTEGER NOT NULL DEFAULT 0");
        String[] critical = new String[] {            "CREATE TABLE IF NOT EXISTS v57_issue_closure_r906 ( target_issue TEXT PRIMARY KEY, draw_hash TEXT NOT NULL, predraw_record_hash TEXT NOT NULL DEFAULT '', eligibility TEXT NOT NULL, core_status TEXT NOT NULL, actual_number INTEGER NOT NULL DEFAULT 0, hit_label INTEGER NOT NULL DEFAULT -1, oos_record_hash TEXT NOT NULL DEFAULT '', real_record_hash TEXT NOT NULL DEFAULT '', settlement_hash TEXT NOT NULL DEFAULT '', previous_task_status TEXT NOT NULL DEFAULT '', exclusion_or_error_code TEXT NOT NULL DEFAULT '', classified_at INTEGER NOT NULL, completed_at INTEGER NOT NULL DEFAULT 0, record_hash TEXT NOT NULL UNIQUE )",
            "CREATE INDEX IF NOT EXISTS idx_r906_closure_status ON v57_issue_closure_r906(core_status,target_issue)",
            "CREATE TABLE IF NOT EXISTS v57_settlement_outbox_r906 ( event_id TEXT PRIMARY KEY, event_type TEXT NOT NULL, target_issue TEXT NOT NULL, subject_id TEXT NOT NULL DEFAULT '', payload_hash TEXT NOT NULL, status TEXT NOT NULL, priority INTEGER NOT NULL, attempts INTEGER NOT NULL DEFAULT 0, next_attempt_at INTEGER NOT NULL DEFAULT 0, lease_owner TEXT NOT NULL DEFAULT '', lease_generation INTEGER NOT NULL DEFAULT 0, process_boot_id TEXT NOT NULL DEFAULT '', lease_until INTEGER NOT NULL DEFAULT 0, last_progress_at INTEGER NOT NULL DEFAULT 0, last_error_code TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, completed_at INTEGER NOT NULL DEFAULT 0, UNIQUE(event_type,target_issue,subject_id) )",
            "CREATE INDEX IF NOT EXISTS idx_r906_outbox_runnable ON v57_settlement_outbox_r906(status,priority DESC,next_attempt_at,created_at)",
            "CREATE TABLE IF NOT EXISTS v57_selected_evidence_r906 ( target_issue TEXT PRIMARY KEY, action_ordinal INTEGER NOT NULL, action_hash TEXT NOT NULL, raw_action_p_value REAL, raw_action_p_status TEXT NOT NULL, raw_action_p_source TEXT NOT NULL, raw_action_p_reason TEXT NOT NULL DEFAULT '', rank_no INTEGER, rank_score_value REAL, rank_status TEXT NOT NULL, third_margin_value REAL, third_margin_status TEXT NOT NULL, entropy_risk_value REAL, entropy_risk_status TEXT NOT NULL, entropy_risk_source TEXT NOT NULL, stability_value REAL, stability_status TEXT NOT NULL, stability_source TEXT NOT NULL, stability_reason TEXT NOT NULL DEFAULT '', window_consensus_value REAL, window_consensus_status TEXT NOT NULL, assignment_support_value REAL, assignment_support_status TEXT NOT NULL, metric_schema_hash TEXT NOT NULL, frozen_at INTEGER NOT NULL, record_hash TEXT NOT NULL UNIQUE )",
            "CREATE TABLE IF NOT EXISTS v57_runtime_truth_snapshot_r906 ( snapshot_id TEXT PRIMARY KEY, generated_at INTEGER NOT NULL, db_data_version INTEGER NOT NULL, latest_draw_issue TEXT NOT NULL, latest_canonical_issue TEXT NOT NULL, latest_closure_issue TEXT NOT NULL, latest_oos_issue TEXT NOT NULL, payload_json TEXT NOT NULL, payload_hash TEXT NOT NULL UNIQUE )"
        };
        for (String statement : critical) db.execSQL(statement);
        // R922 same-version additive audit tables are startup-owned.  The canonical transaction
        // only inserts immutable rows; reports never create or repair schema on a read path.
        ControlledCandidateOpportunityAuditR922.ensureSchema(db);
        ResearchMainlineContributionAuditR922.ensureSchema(db);
        migrateSelectedEvidenceSchemaR911(db);
        requireSchemaObjectR906(db, "table", TABLE_R906_ISSUE_CLOSURE);
        requireSchemaObjectR906(db, "table", TABLE_R906_SETTLEMENT_OUTBOX);
        requireSchemaObjectR906(db, "table", TABLE_R906_SELECTED_EVIDENCE);
        requireSchemaObjectR906(db, "table", TABLE_R906_RUNTIME_TRUTH);
        requireSchemaObjectR906(db, "table", ControlledCandidateOpportunityAuditR922.TABLE);
        requireSchemaObjectR906(db, "table", ResearchMainlineContributionAuditR922.TABLE);
        requireSchemaObjectR906(db, "index", "idx_r906_closure_status");
        requireSchemaObjectR906(db, "index", "idx_r906_outbox_runnable");
        writeSchemaStateR906(db, "r906_schema_critical_ready", "1", now);
    }

    /** R926 research-only R906/R908 schema. Failure is observable and never disguised as critical success. */
    public static void ensureR906ResearchSchema(SQLiteDatabase db) {
        if (db == null) throw new IllegalArgumentException("R906_RESEARCH_DB_NULL");
        final long now = System.currentTimeMillis();
        boolean researchReady = false;
        String researchError = "";
        try {
            String[] research = new String[] {                "CREATE TABLE IF NOT EXISTS v57_research_asset_r906 ( asset_id TEXT PRIMARY KEY, display_name_zh TEXT NOT NULL, normalized_name_zh TEXT NOT NULL UNIQUE, candidate_type TEXT NOT NULL, novelty_class TEXT NOT NULL, tier TEXT NOT NULL, method_signature TEXT NOT NULL, canonical_spec TEXT NOT NULL, representation_family TEXT NOT NULL, learner_family TEXT NOT NULL, update_rule TEXT NOT NULL, objective_family TEXT NOT NULL, calibration_family TEXT NOT NULL, risk_family TEXT NOT NULL, falsifier_family TEXT NOT NULL, parent_asset_ids TEXT NOT NULL DEFAULT '', generation INTEGER NOT NULL, genetic_operator TEXT NOT NULL, feature_schema_hash TEXT NOT NULL DEFAULT '', artifact_status TEXT NOT NULL, artifact_hash TEXT NOT NULL DEFAULT '', trained_until_issue TEXT NOT NULL DEFAULT '', earliest_use_issue TEXT NOT NULL DEFAULT '', runtime_executable INTEGER NOT NULL DEFAULT 0, may_authorize_real INTEGER NOT NULL DEFAULT 0 CHECK(may_authorize_real=0), last_transition_code TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, registry_hash TEXT NOT NULL UNIQUE )",
                "CREATE INDEX IF NOT EXISTS idx_r906_asset_tier ON v57_research_asset_r906(tier,runtime_executable,updated_at)",
                "CREATE INDEX IF NOT EXISTS idx_r906_asset_signature ON v57_research_asset_r906(method_signature,created_at)",
                "CREATE TABLE IF NOT EXISTS v57_research_eval_coverage_r906 ( asset_id TEXT PRIMARY KEY, eligible_n INTEGER NOT NULL DEFAULT 0, scheduled_n INTEGER NOT NULL DEFAULT 0, frozen_n INTEGER NOT NULL DEFAULT 0, settled_n INTEGER NOT NULL DEFAULT 0, hit_n INTEGER NOT NULL DEFAULT 0, first_eligible_issue TEXT NOT NULL DEFAULT '', last_eligible_issue TEXT NOT NULL DEFAULT '', last_frozen_issue TEXT NOT NULL DEFAULT '', last_settled_issue TEXT NOT NULL DEFAULT '', max_gap_issues INTEGER NOT NULL DEFAULT 0, hit_rate REAL, interval_lower REAL, interval_upper REAL, interval_method TEXT NOT NULL DEFAULT '', brier REAL, ece REAL, block_stability REAL, drift_score REAL, latency_p95_ms REAL, ram_p95_bytes INTEGER, evidence_hash TEXT NOT NULL, updated_at INTEGER NOT NULL )",
                "CREATE TABLE IF NOT EXISTS v57_research_compact_label_r906 ( asset_id TEXT NOT NULL, target_issue TEXT NOT NULL, selected_ordinal INTEGER NOT NULL, feature_schema_hash TEXT NOT NULL, feature_blob BLOB NOT NULL, feature_hash TEXT NOT NULL, predicted_probability REAL NOT NULL, actual_number INTEGER NOT NULL, hit_label INTEGER NOT NULL CHECK(hit_label IN (0,1)), predraw_hash TEXT NOT NULL, draw_hash TEXT NOT NULL, outcome_hash TEXT NOT NULL UNIQUE, frozen_at INTEGER NOT NULL, settled_at INTEGER NOT NULL, PRIMARY KEY(asset_id,target_issue) )",
                "CREATE INDEX IF NOT EXISTS idx_r906_compact_label_issue ON v57_research_compact_label_r906(target_issue,asset_id)",
                "CREATE TABLE IF NOT EXISTS v57_research_learner_update_r906 ( learner_id TEXT NOT NULL, target_issue TEXT NOT NULL, prior_artifact_hash TEXT NOT NULL, feature_hash TEXT NOT NULL, outcome_hash TEXT NOT NULL, new_artifact_hash TEXT NOT NULL, trained_until_issue TEXT NOT NULL, earliest_use_issue TEXT NOT NULL, update_status TEXT NOT NULL, updated_at INTEGER NOT NULL, record_hash TEXT NOT NULL UNIQUE, PRIMARY KEY(learner_id,target_issue) )",
                "CREATE TABLE IF NOT EXISTS v57_research_artifact_job_r906 ( job_id TEXT PRIMARY KEY, asset_id TEXT NOT NULL, job_status TEXT NOT NULL, dataset_hash TEXT NOT NULL, data_until_issue TEXT NOT NULL, feature_schema_hash TEXT NOT NULL, canonical_method_spec TEXT NOT NULL, seed INTEGER NOT NULL, protocol_hash TEXT NOT NULL, artifact_hash TEXT NOT NULL DEFAULT '', manifest_hash TEXT NOT NULL DEFAULT '', parity_status TEXT NOT NULL DEFAULT 'NOT_RUN', last_error_code TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, UNIQUE(asset_id,dataset_hash,protocol_hash) )",
                "CREATE TABLE IF NOT EXISTS v57_research_certificate_r906 ( certificate_id TEXT PRIMARY KEY, asset_id TEXT NOT NULL, certificate_type TEXT NOT NULL, evidence_start_issue TEXT NOT NULL, evidence_end_issue TEXT NOT NULL, future_n INTEGER NOT NULL, coverage REAL NOT NULL, paired_delta REAL NOT NULL, adjusted_lower REAL NOT NULL, brier REAL NOT NULL, ece REAL NOT NULL, block_stability REAL NOT NULL, drift_score REAL NOT NULL, latency_p95_ms REAL NOT NULL, ram_p95_bytes INTEGER NOT NULL, valid_from_issue TEXT NOT NULL, valid_until_issue TEXT NOT NULL, status TEXT NOT NULL, revocation_code TEXT NOT NULL DEFAULT '', evidence_hash TEXT NOT NULL, record_hash TEXT NOT NULL UNIQUE, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL )",
                "CREATE INDEX IF NOT EXISTS idx_r906_certificate_active ON v57_research_certificate_r906(status,valid_from_issue,valid_until_issue)",
                "CREATE TABLE IF NOT EXISTS v57_algorithm_genome_r908 ( genome_id TEXT PRIMARY KEY, display_name_zh TEXT NOT NULL, genome_spec TEXT NOT NULL, structural_hash TEXT NOT NULL UNIQUE, behavior_hash TEXT NOT NULL DEFAULT '', generation INTEGER NOT NULL, parent_ids TEXT NOT NULL DEFAULT '', trained_until_issue TEXT NOT NULL DEFAULT '', earliest_use_issue TEXT NOT NULL DEFAULT '', dev_n INTEGER NOT NULL DEFAULT 0, dev_hit INTEGER NOT NULL DEFAULT 0, dev_hit_rate REAL, dev_wilson_lower REAL, dev_block_min REAL, dev_max_miss INTEGER NOT NULL DEFAULT 0, novelty REAL NOT NULL DEFAULT 0, complexity REAL NOT NULL DEFAULT 0, state_blob TEXT NOT NULL, state_updates INTEGER NOT NULL DEFAULT 0, status TEXT NOT NULL, may_authorize_real INTEGER NOT NULL DEFAULT 0 CHECK(may_authorize_real=0), created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL )",
                "CREATE INDEX IF NOT EXISTS idx_r908_algorithm_status ON v57_algorithm_genome_r908(status,generation,updated_at)",
                "CREATE TABLE IF NOT EXISTS v57_algorithm_predraw_r908 ( genome_id TEXT NOT NULL, target_issue TEXT NOT NULL, position INTEGER NOT NULL, n1 INTEGER NOT NULL, n2 INTEGER NOT NULL, n3 INTEGER NOT NULL, selected_ordinal INTEGER NOT NULL, score REAL NOT NULL, feature_blob TEXT NOT NULL, feature_hash TEXT NOT NULL, frozen_at INTEGER NOT NULL, record_hash TEXT NOT NULL UNIQUE, actual_number INTEGER NOT NULL DEFAULT 0, hit_label INTEGER NOT NULL DEFAULT -1 CHECK(hit_label IN (-1,0,1)), settled_at INTEGER NOT NULL DEFAULT 0, outcome_hash TEXT NOT NULL DEFAULT '', PRIMARY KEY(genome_id,target_issue) )",
                "CREATE INDEX IF NOT EXISTS idx_r908_algorithm_predraw_issue ON v57_algorithm_predraw_r908(target_issue,hit_label,genome_id)"
            };
            for (String statement : research) db.execSQL(statement);
            // R908 additive evolution evidence. Existing DB_VERSION=36 installs receive these
            // columns idempotently; no destructive migration and no uninstall is required.
            addColumnR36(db, TABLE_R908_ALGORITHM_GENOME, "future_n INTEGER NOT NULL DEFAULT 0");
            addColumnR36(db, TABLE_R908_ALGORITHM_GENOME, "future_hit INTEGER NOT NULL DEFAULT 0");
            addColumnR36(db, TABLE_R908_ALGORITHM_GENOME, "future_hit_rate REAL");
            addColumnR36(db, TABLE_R908_ALGORITHM_GENOME, "future_wilson_lower REAL");
            addColumnR36(db, TABLE_R908_ALGORITHM_GENOME, "future_last_issue TEXT NOT NULL DEFAULT ''");
            addColumnR36(db, TABLE_R908_ALGORITHM_GENOME, "future_evidence_hash TEXT NOT NULL DEFAULT ''");
            // R914 algorithm-family evidence is additive on DB v36 so cover-install keeps every
            // existing draw, prediction, settlement and invented genome intact.
            addColumnR36(db, TABLE_R908_ALGORITHM_GENOME, "mechanism_spec_zh TEXT NOT NULL DEFAULT ''");
            addColumnR36(db, TABLE_R908_ALGORITHM_GENOME, "future_brier REAL NOT NULL DEFAULT 1");
            addColumnR36(db, TABLE_R908_ALGORITHM_GENOME, "future_ece REAL NOT NULL DEFAULT 1");
            addColumnR36(db, TABLE_R908_ALGORITHM_GENOME, "future_coverage REAL NOT NULL DEFAULT 0");
            addColumnR36(db, TABLE_R908_ALGORITHM_GENOME, "future_block_min REAL NOT NULL DEFAULT 0");
            addColumnR36(db, TABLE_R908_ALGORITHM_GENOME, "future_regime_min REAL NOT NULL DEFAULT 0");
            addColumnR36(db, TABLE_R908_ALGORITHM_GENOME, "future_max_miss INTEGER NOT NULL DEFAULT 0");
            addColumnR36(db, TABLE_R908_ALGORITHM_GENOME, "paired_mainline_n INTEGER NOT NULL DEFAULT 0");
            addColumnR36(db, TABLE_R908_ALGORITHM_GENOME, "paired_mainline_delta REAL NOT NULL DEFAULT 0");
            addColumnR36(db, TABLE_R908_ALGORITHM_GENOME, "lifecycle_reason TEXT NOT NULL DEFAULT ''");
            addColumnR36(db, TABLE_R908_ALGORITHM_GENOME, "research_genome_contract TEXT NOT NULL DEFAULT ''");
            addColumnR36(db, TABLE_R908_ALGORITHM_GENOME, "future_fdr_q REAL NOT NULL DEFAULT 1");
            // R919 makes invention auditable: every generated program records which settled
            // failure vector requested the structural repair and which topology intent was used.
            addColumnR36(db, TABLE_R908_ALGORITHM_GENOME, "mutation_intent TEXT NOT NULL DEFAULT ''");
            addColumnR36(db, TABLE_R908_ALGORITHM_GENOME, "creation_reason TEXT NOT NULL DEFAULT ''");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_r914_algorithm_future ON "
                    + TABLE_R908_ALGORITHM_GENOME
                    + "(status,future_wilson_lower DESC,future_n DESC,updated_at)");
            AssetGenesisEngineR915.ensureSchema(db);
            LegacyPredrawContextBridgeR915.ensureSchema(db);
            requireSchemaObjectR906(db, "table", TABLE_R906_RESEARCH_ASSET);
            requireSchemaObjectR906(db, "table", TABLE_R906_RESEARCH_EVAL_COVERAGE);
            requireSchemaObjectR906(db, "table", TABLE_R906_RESEARCH_COMPACT_LABEL);
            requireSchemaObjectR906(db, "table", TABLE_R906_RESEARCH_LEARNER_UPDATE);
            requireSchemaObjectR906(db, "table", TABLE_R906_RESEARCH_ARTIFACT_JOB);
            requireSchemaObjectR906(db, "table", TABLE_R906_RESEARCH_CERTIFICATE);
            requireSchemaObjectR906(db, "table", TABLE_R908_ALGORITHM_GENOME);
            requireSchemaObjectR906(db, "table", TABLE_R908_ALGORITHM_PREDRAW);
            requireSchemaObjectR906(db, "table", AssetGenesisEngineR915.TABLE_LEGACY_REGISTRY);
            requireSchemaObjectR906(db, "table", AssetGenesisEngineR915.TABLE_ATOM_SCHEMA);
            requireSchemaObjectR906(db, "table", AssetGenesisEngineR915.TABLE_ASSET_GENOME);
            requireSchemaObjectR906(db, "table", AssetGenesisEngineR915.TABLE_ASSET_FREEZE);
            requireSchemaObjectR906(db, "table", AssetGenesisEngineR915.TABLE_ASSET_CERTIFICATE);
            requireSchemaObjectR906(db, "table", AssetGenesisEngineR915.TABLE_EVOLUTION_EVENT);
            requireSchemaObjectR906(db, "table", CertifiedResearchMixerR915.TABLE_CONTRIBUTION);
            requireSchemaObjectR906(db, "table", LegacyPredrawContextBridgeR915.TABLE);
            requireSchemaObjectR906(db, "table", LegacyPredrawContextBridgeR915.POST_TABLE);
            requireSchemaObjectR906(db, "index", "idx_r919_legacy_post_status");
            requireSchemaObjectR906(db, "index", "idx_r906_asset_tier");
            requireSchemaObjectR906(db, "index", "idx_r906_asset_signature");
            requireSchemaObjectR906(db, "index", "idx_r906_compact_label_issue");
            requireSchemaObjectR906(db, "index", "idx_r906_certificate_active");
            researchReady = true;
        } catch (ResearchWorkBudgetR927.Yield | RuntimeResourceAuditR927.BudgetExceeded yielded) {
            throw yielded; // A yielded DDL transaction must never receive a COMPLETED journal row.
        } catch (Throwable failure) {
            researchError = failure.getClass().getSimpleName() + ':' + String.valueOf(failure.getMessage());
        }
        writeSchemaStateR906(db, "r906_schema_research_ready", researchReady ? "1" : "0", now);
        writeSchemaStateR906(db, "r906_schema_research_error", researchError, now);
        if (!researchReady) throw new IllegalStateException("R64_RESEARCH_SCHEMA_INCOMPLETE|" + researchError);
        if (researchReady) {
            SchemaRevisionR925.ensureTables(db);
            writeSchemaStateR906(db, "r906_schema_revision", String.valueOf(AppConfig.R906_SCHEMA_REVISION), now);
        }
    }

    /** Compatibility entry point for legacy callers that explicitly require both domains. */
    public static void ensureR906Schema(SQLiteDatabase db) {
        ensureR906CriticalSchema(db);
        ensureR906ResearchSchema(db);
    }

    private static void writeSchemaStateR906(SQLiteDatabase db, String key, String value, long now) {
        db.execSQL(
                "INSERT OR REPLACE INTO " + TABLE_APP_STATE + "(state_key,state_value,updated_at) VALUES(?,?,?)",
                new Object[] {key, value == null ? "" : value, Long.valueOf(now)});
    }

    private static void requireSchemaObjectR906(SQLiteDatabase db, String type, String name) {
        Cursor c = null;
        try {
            c = db.rawQuery(
                    "SELECT 1 FROM sqlite_master WHERE type=? AND name=? LIMIT 1",
                    new String[] {type, name});
            if (!c.moveToFirst()) throw new SQLiteException("R906_SCHEMA_OBJECT_MISSING:" + type + ':' + name);
        } finally {
            if (c != null) c.close();
        }
    }

    /**
     * R911 same-DB-version compatibility repair.
     *
     * <p>R906 originally shipped on some devices with the selected-evidence rank availability
     * column named {@code rank_score_status}; the canonical schema uses {@code rank_status}.
     * CREATE TABLE IF NOT EXISTS cannot repair an already-existing table, so every startup must
     * converge the additive column and migrate the legacy value before any Research SQL is armed.
     */
    private static void migrateSelectedEvidenceSchemaR911(SQLiteDatabase db) {
        if (db == null) return;
        boolean hasCanonical = columnExistsR36(db, TABLE_R906_SELECTED_EVIDENCE, "rank_status");
        boolean hasLegacy = columnExistsR36(db, TABLE_R906_SELECTED_EVIDENCE, "rank_score_status");
        if (!hasCanonical) {
            addColumnR36(
                    db,
                    TABLE_R906_SELECTED_EVIDENCE,
                    "rank_status TEXT NOT NULL DEFAULT 'UNAVAILABLE'");
            hasCanonical = true;
        }
        if (hasCanonical && hasLegacy) {
            db.execSQL(
                    "UPDATE " + TABLE_R906_SELECTED_EVIDENCE
                            + " SET rank_status=rank_score_status"
                            + " WHERE rank_score_status IS NOT NULL AND rank_score_status<>''"
                            + " AND (rank_status IS NULL OR rank_status='' OR rank_status='UNAVAILABLE')");
        }
        if (!columnExistsR36(db, TABLE_R906_SELECTED_EVIDENCE, "rank_status")) {
            throw new SQLiteException("R911_SELECTED_EVIDENCE_RANK_STATUS_MISSING");
        }
    }

    private static void addColumnR36(SQLiteDatabase db, String table, String definition) {
        String name = definition.substring(0, definition.indexOf(' '));
        if (columnExistsR36(db, table, name)) return;
        db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + definition);
        if (!columnExistsR36(db, table, name)) {
            throw new SQLiteException("R36_ADD_COLUMN_NOT_VISIBLE:" + table + "." + name);
        }
    }

    private static boolean columnExistsR36(SQLiteDatabase db, String table, String name) {
        Cursor c = null;
        try {
            c = db.rawQuery("PRAGMA table_info(" + table + ")", null);
            int index = c.getColumnIndex("name");
            while (c.moveToNext()) if (index >= 0 && name.equals(c.getString(index))) return true;
            return false;
        } finally {
            if (c != null) c.close();
        }
    }

    /* STARCORE_TIANSHU_B03_STORAGE_BEGIN */
    public static final String TABLE_B03_SCHEMA_META = "b03_schema_meta";
    public static final String TABLE_B03_TRANSACTION_AUDIT = "b03_transaction_audit";
    public static final String TABLE_B03_MIGRATION_JOURNAL = "b03_migration_journal";
    public static final String TABLE_B03_DRAW_QUARANTINE = "b03_draw_quarantine";
    public static final String TABLE_B03_PREDICTION_BUNDLE = "b03_prediction_bundle";
    public static final String TABLE_B03_NUMBER_EVENT = "b03_number_event";
    public static final String TABLE_B03_ACTION_EVENT = "b03_action_event";
    public static final String TABLE_B03_PERMUTATION_EVENT = "b03_permutation_event";
    public static final String TABLE_B03_SELECTED_EVENT = "b03_selected_event";
    public static final String TABLE_B03_AUTHORIZATION_EVENT = "b03_authorization_event";
    public static final String TABLE_B03_SETTLEMENT_EVENT = "b03_settlement_event";
    public static final String TABLE_B03_INTEGRITY_SNAPSHOT = "b03_integrity_snapshot";
    public static final String TABLE_B03_REPLAY_AUDIT = "b03_replay_audit";

    public static final class StorageRule {
        public final String id, owner, invariant;
        public final boolean mandatory;
        public final int severity;

        StorageRule(String id, String owner, String invariant, boolean mandatory, int severity) {
            this.id = id;
            this.owner = owner;
            this.invariant = invariant;
            this.mandatory = mandatory;
            this.severity = severity;
        }

        public String canonical() {
            return id + '|' + owner + '|' + invariant + '|' + (mandatory ? 1 : 0) + '|' + severity;
        }
    }

    public static void ensureTianshuB03(SQLiteDatabase db) {
        if (db == null) throw new IllegalArgumentException("B03_DB_NULL");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + TABLE_B03_SCHEMA_META
                        + "(meta_key TEXT PRIMARY KEY,meta_value TEXT NOT NULL,updated_at INTEGER"
                        + " NOT NULL)");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + TABLE_B03_TRANSACTION_AUDIT
                        + "(_id INTEGER PRIMARY KEY AUTOINCREMENT,idempotency_key TEXT NOT NULL"
                        + " UNIQUE,operation TEXT NOT NULL,game_code TEXT NOT NULL,target_issue"
                        + " TEXT NOT NULL DEFAULT '',transaction_hash TEXT NOT NULL,status TEXT NOT"
                        + " NULL,error_detail TEXT NOT NULL DEFAULT '',created_at INTEGER NOT"
                        + " NULL,CHECK(game_code='Binance1-PK10'))");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + TABLE_B03_MIGRATION_JOURNAL
                        + "(migration_id TEXT PRIMARY KEY,source_database TEXT NOT"
                        + " NULL,source_game_code TEXT NOT NULL,policy TEXT NOT NULL,cursor_issue"
                        + " TEXT NOT NULL DEFAULT '',scanned_count INTEGER NOT NULL DEFAULT"
                        + " 0,imported_count INTEGER NOT NULL DEFAULT 0,replay_hash TEXT NOT"
                        + " NULL,status TEXT NOT NULL,updated_at INTEGER NOT NULL)");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + TABLE_B03_DRAW_QUARANTINE
                        + "(_id INTEGER PRIMARY KEY AUTOINCREMENT,source_database TEXT NOT"
                        + " NULL,source_game_code TEXT NOT NULL,source_table TEXT NOT"
                        + " NULL,source_issue TEXT NOT NULL,source_numbers TEXT NOT"
                        + " NULL,normalized_numbers TEXT NOT NULL DEFAULT '',draw_time INTEGER NOT"
                        + " NULL DEFAULT 0,quarantine_reason TEXT NOT NULL,review_status TEXT NOT"
                        + " NULL,row_hash TEXT NOT NULL UNIQUE,created_at INTEGER NOT NULL)");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + TABLE_B03_PREDICTION_BUNDLE
                        + "(bundle_id TEXT PRIMARY KEY,game_code TEXT NOT NULL,target_issue TEXT"
                        + " NOT NULL,base_issue TEXT NOT NULL,history_hash TEXT NOT"
                        + " NULL,feature_hash TEXT NOT NULL,state_hash TEXT NOT NULL,expert_hash"
                        + " TEXT NOT NULL,bundle_hash TEXT NOT NULL,contract_hash TEXT NOT"
                        + " NULL,artifact_bundle_hash TEXT NOT NULL,cohort_id TEXT NOT NULL,status"
                        + " TEXT NOT NULL,record_hash TEXT NOT NULL UNIQUE,created_at INTEGER NOT"
                        + " NULL,UNIQUE(game_code,target_issue),CHECK(game_code='Binance1-PK10'))");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + TABLE_B03_NUMBER_EVENT
                        + "(_id INTEGER PRIMARY KEY AUTOINCREMENT,bundle_id TEXT NOT NULL,game_code"
                        + " TEXT NOT NULL,target_issue TEXT NOT NULL,position INTEGER NOT"
                        + " NULL,number INTEGER NOT NULL,raw_score REAL NOT NULL,number_p REAL NOT"
                        + " NULL,number_rank INTEGER NOT NULL,state_id TEXT NOT NULL,domain_id TEXT"
                        + " NOT NULL,contract_hash TEXT NOT NULL,bundle_hash TEXT NOT"
                        + " NULL,record_hash TEXT NOT NULL UNIQUE,created_at INTEGER NOT"
                        + " NULL,UNIQUE(bundle_id,position,number),FOREIGN KEY(bundle_id)"
                        + " REFERENCES "
                        + TABLE_B03_PREDICTION_BUNDLE
                        + "(bundle_id) ON DELETE"
                        + " RESTRICT,CHECK(game_code='Binance1-PK10'),CHECK(position BETWEEN 1 AND"
                        + " 10),CHECK(number BETWEEN 1 AND 10))");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + TABLE_B03_ACTION_EVENT
                        + "(_id INTEGER PRIMARY KEY AUTOINCREMENT,bundle_id TEXT NOT NULL,game_code"
                        + " TEXT NOT NULL,target_issue TEXT NOT NULL,position INTEGER NOT NULL,n1"
                        + " INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,n4 INTEGER NOT"
                        + " NULL DEFAULT 0,raw_action_p REAL NOT NULL,rank_score REAL NOT"
                        + " NULL,rank_no INTEGER NOT NULL,action_hash TEXT NOT NULL,contract_hash"
                        + " TEXT NOT NULL,bundle_hash TEXT NOT NULL,record_hash TEXT NOT NULL"
                        + " UNIQUE,created_at INTEGER NOT NULL,UNIQUE(bundle_id,position),FOREIGN"
                        + " KEY(bundle_id) REFERENCES "
                        + TABLE_B03_PREDICTION_BUNDLE
                        + "(bundle_id) ON DELETE"
                        + " RESTRICT,CHECK(game_code='Binance1-PK10'),CHECK(position BETWEEN 1 AND"
                        + " 10))");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + TABLE_B03_PERMUTATION_EVENT
                        + "(bundle_id TEXT PRIMARY KEY,game_code TEXT NOT NULL,target_issue TEXT"
                        + " NOT NULL,matrix_blob TEXT NOT NULL,row_error REAL NOT NULL,column_error"
                        + " REAL NOT NULL,iterations INTEGER NOT NULL,converged INTEGER NOT"
                        + " NULL,used_fallback INTEGER NOT NULL,map_blob TEXT NOT NULL,matrix_hash"
                        + " TEXT NOT NULL,contract_hash TEXT NOT NULL,bundle_hash TEXT NOT"
                        + " NULL,record_hash TEXT NOT NULL UNIQUE,created_at INTEGER NOT"
                        + " NULL,FOREIGN KEY(bundle_id) REFERENCES "
                        + TABLE_B03_PREDICTION_BUNDLE
                        + "(bundle_id) ON DELETE RESTRICT,CHECK(game_code='Binance1-PK10'))");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + TABLE_B03_SELECTED_EVENT
                        + "(bundle_id TEXT PRIMARY KEY,game_code TEXT NOT NULL,target_issue TEXT"
                        + " NOT NULL,decision_id TEXT NOT NULL UNIQUE,position INTEGER NOT NULL,n1"
                        + " INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,selected_hit_p"
                        + " REAL NOT NULL,predictability REAL NOT NULL,lane TEXT NOT"
                        + " NULL,decision_action TEXT NOT NULL,reject_code TEXT NOT"
                        + " NULL,contract_hash TEXT NOT NULL,bundle_hash TEXT NOT"
                        + " NULL,authorization_hash TEXT NOT NULL,record_hash TEXT NOT NULL"
                        + " UNIQUE,created_at INTEGER NOT NULL,FOREIGN KEY(bundle_id) REFERENCES "
                        + TABLE_B03_PREDICTION_BUNDLE
                        + "(bundle_id) ON DELETE"
                        + " RESTRICT,CHECK(game_code='Binance1-PK10'),CHECK(decision_action IN"
                        + " ('BET','SHADOW','HOLD')))");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + TABLE_B03_AUTHORIZATION_EVENT
                        + "(decision_id TEXT PRIMARY KEY,bundle_id TEXT NOT NULL,game_code TEXT NOT"
                        + " NULL,target_issue TEXT NOT NULL,contract_hash TEXT NOT NULL,bundle_hash"
                        + " TEXT NOT NULL,artifact_bundle_hash TEXT NOT NULL,cohort_id TEXT NOT"
                        + " NULL,authorization_hash TEXT NOT NULL,record_hash TEXT NOT NULL"
                        + " UNIQUE,created_at INTEGER NOT NULL,FOREIGN KEY(bundle_id) REFERENCES "
                        + TABLE_B03_PREDICTION_BUNDLE
                        + "(bundle_id) ON DELETE RESTRICT,CHECK(game_code='Binance1-PK10'))");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + TABLE_B03_SETTLEMENT_EVENT
                        + "(_id INTEGER PRIMARY KEY AUTOINCREMENT,bundle_id TEXT NOT NULL,game_code"
                        + " TEXT NOT NULL,target_issue TEXT NOT NULL,actual_permutation TEXT NOT"
                        + " NULL,result TEXT NOT NULL,settlement_hash TEXT NOT NULL"
                        + " UNIQUE,record_hash TEXT NOT NULL UNIQUE,created_at INTEGER NOT"
                        + " NULL,UNIQUE(game_code,target_issue),FOREIGN KEY(bundle_id) REFERENCES "
                        + TABLE_B03_PREDICTION_BUNDLE
                        + "(bundle_id) ON DELETE RESTRICT,CHECK(game_code='Binance1-PK10'))");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + TABLE_B03_INTEGRITY_SNAPSHOT
                        + "(_id INTEGER PRIMARY KEY AUTOINCREMENT,snapshot_hash TEXT NOT NULL"
                        + " UNIQUE,schema_hash TEXT NOT NULL,replay_hash TEXT NOT NULL,valid"
                        + " INTEGER NOT NULL,detail TEXT NOT NULL,created_at INTEGER NOT NULL)");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + TABLE_B03_REPLAY_AUDIT
                        + "(_id INTEGER PRIMARY KEY AUTOINCREMENT,game_code TEXT NOT"
                        + " NULL,target_issue TEXT NOT NULL,replay_hash TEXT NOT NULL,bundle_hash"
                        + " TEXT NOT NULL,contract_hash TEXT NOT NULL,status TEXT NOT NULL,detail"
                        + " TEXT NOT NULL,created_at INTEGER NOT"
                        + " NULL,UNIQUE(game_code,target_issue,replay_hash),CHECK(game_code='Binance1-PK10'))");
        createB03Indexes(db);
        createB03ImmutableTriggers(db);
        putB03Meta(db, "schema_id", "STARCORE_TIANSHU_B03");
        putB03Meta(db, "schema_hash", b03SchemaHash());
        putB03Meta(db, "database_name", "binance_k3_nova_one_pro_v57.db");
        putB03Meta(db, "game_code", "Binance1-PK10");
        putB03Meta(db, "model_proven", String.valueOf(V51BuildManifest.MODEL_PROVEN));
        putB03Meta(db, "shadow_only", String.valueOf(V51BuildManifest.SHADOW_ONLY));
        putB03Meta(
                db,
                "real_bet_hard_disabled",
                String.valueOf(V51BuildManifest.REAL_BET_HARD_DISABLED));
        putB03Meta(db, "real_action_policy", V51BuildManifest.releaseState());
    }

    private static void migrateB03SelectedEventForR31(SQLiteDatabase db) {
        if (db == null) return;
        android.database.Cursor c = null;
        String sql = "";
        try {
            c =
                    db.rawQuery(
                            "SELECT sql FROM sqlite_master WHERE type='table' AND name=?",
                            new String[] {TABLE_B03_SELECTED_EVENT});
            if (c.moveToFirst() && !c.isNull(0)) sql = c.getString(0);
        } finally {
            if (c != null) c.close();
        }
        if (sql == null || sql.length() == 0 || sql.indexOf("decision_action<>'BET'") < 0) return;
        String legacy = TABLE_B03_SELECTED_EVENT + "_r30_shadow";
        db.execSQL("DROP TRIGGER IF EXISTS trg_" + TABLE_B03_SELECTED_EVENT + "_no_delete");
        db.execSQL("DROP TRIGGER IF EXISTS trg_" + TABLE_B03_SELECTED_EVENT + "_no_update");
        db.execSQL("DROP INDEX IF EXISTS idx_b03_selected_lane");
        db.execSQL("DROP TABLE IF EXISTS " + legacy);
        db.execSQL("ALTER TABLE " + TABLE_B03_SELECTED_EVENT + " RENAME TO " + legacy);
        db.execSQL(
                "CREATE TABLE "
                        + TABLE_B03_SELECTED_EVENT
                        + "(bundle_id TEXT PRIMARY KEY,game_code TEXT NOT NULL,target_issue TEXT"
                        + " NOT NULL,decision_id TEXT NOT NULL UNIQUE,position INTEGER NOT NULL,n1"
                        + " INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,selected_hit_p"
                        + " REAL NOT NULL,predictability REAL NOT NULL,lane TEXT NOT"
                        + " NULL,decision_action TEXT NOT NULL,reject_code TEXT NOT"
                        + " NULL,contract_hash TEXT NOT NULL,bundle_hash TEXT NOT"
                        + " NULL,authorization_hash TEXT NOT NULL,record_hash TEXT NOT NULL"
                        + " UNIQUE,created_at INTEGER NOT NULL,FOREIGN KEY(bundle_id) REFERENCES "
                        + TABLE_B03_PREDICTION_BUNDLE
                        + "(bundle_id) ON DELETE"
                        + " RESTRICT,CHECK(game_code='Binance1-PK10'),CHECK(decision_action IN"
                        + " ('BET','SHADOW','HOLD')))");
        db.execSQL(
                "INSERT INTO "
                        + TABLE_B03_SELECTED_EVENT
                        + "(bundle_id,game_code,target_issue,decision_id,position,n1,n2,n3,selected_hit_p,predictability,lane,decision_action,reject_code,contract_hash,bundle_hash,authorization_hash,record_hash,created_at)"
                        + " SELECT"
                        + " bundle_id,game_code,target_issue,decision_id,position,n1,n2,n3,selected_hit_p,predictability,lane,decision_action,reject_code,contract_hash,bundle_hash,authorization_hash,record_hash,created_at"
                        + " FROM "
                        + legacy);
        db.execSQL("DROP TABLE " + legacy);
        createB03Indexes(db);
        createB03ImmutableTriggers(db);
        putB03Meta(db, "r31_selected_event_migration", "DONE");
    }

    private static void createB03Indexes(SQLiteDatabase db) {
        String[] sql = {
            "CREATE INDEX IF NOT EXISTS idx_b03_tx_target ON "
                    + TABLE_B03_TRANSACTION_AUDIT
                    + "(game_code,target_issue,created_at)",
            "CREATE INDEX IF NOT EXISTS idx_b03_migration_status ON "
                    + TABLE_B03_MIGRATION_JOURNAL
                    + "(status,updated_at)",
            "CREATE INDEX IF NOT EXISTS idx_b03_quarantine_issue ON "
                    + TABLE_B03_DRAW_QUARANTINE
                    + "(source_game_code,source_issue)",
            "CREATE INDEX IF NOT EXISTS idx_b03_bundle_target ON "
                    + TABLE_B03_PREDICTION_BUNDLE
                    + "(game_code,target_issue)",
            "CREATE INDEX IF NOT EXISTS idx_b03_number_target ON "
                    + TABLE_B03_NUMBER_EVENT
                    + "(game_code,target_issue,position,number)",
            "CREATE INDEX IF NOT EXISTS idx_b03_action_rank ON "
                    + TABLE_B03_ACTION_EVENT
                    + "(game_code,target_issue,rank_no)",
            "CREATE INDEX IF NOT EXISTS idx_b03_selected_lane ON "
                    + TABLE_B03_SELECTED_EVENT
                    + "(game_code,lane,decision_action)",
            "CREATE INDEX IF NOT EXISTS idx_b03_settlement_target ON "
                    + TABLE_B03_SETTLEMENT_EVENT
                    + "(game_code,target_issue,created_at)",
            "CREATE INDEX IF NOT EXISTS idx_b03_integrity_created ON "
                    + TABLE_B03_INTEGRITY_SNAPSHOT
                    + "(created_at)",
            "CREATE INDEX IF NOT EXISTS idx_b03_replay_target ON "
                    + TABLE_B03_REPLAY_AUDIT
                    + "(game_code,target_issue,created_at)"
        };
        for (int i = 0; i < sql.length; i++) db.execSQL(sql[i]);
    }

    private static void createB03ImmutableTriggers(SQLiteDatabase db) {
        String[] tables = {
            TABLE_B03_PREDICTION_BUNDLE,
            TABLE_B03_NUMBER_EVENT,
            TABLE_B03_ACTION_EVENT,
            TABLE_B03_PERMUTATION_EVENT,
            TABLE_B03_SELECTED_EVENT,
            TABLE_B03_AUTHORIZATION_EVENT,
            TABLE_B03_SETTLEMENT_EVENT
        };
        for (int i = 0; i < tables.length; i++) {
            String t = tables[i];
            db.execSQL(
                    "CREATE TRIGGER IF NOT EXISTS trg_"
                            + t
                            + "_no_delete BEFORE DELETE ON "
                            + t
                            + " BEGIN SELECT RAISE(ABORT,'B03_IMMUTABLE_DELETE'); END");
            db.execSQL(
                    "CREATE TRIGGER IF NOT EXISTS trg_"
                            + t
                            + "_no_update BEFORE UPDATE ON "
                            + t
                            + " BEGIN SELECT RAISE(ABORT,'B03_IMMUTABLE_UPDATE'); END");
        }
        db.execSQL(
                "CREATE TRIGGER IF NOT EXISTS trg_"
                        + TABLE_B03_DRAW_QUARANTINE
                        + "_no_delete BEFORE DELETE ON "
                        + TABLE_B03_DRAW_QUARANTINE
                        + " BEGIN SELECT RAISE(ABORT,'B03_IMMUTABLE_DELETE'); END");
        db.execSQL(
                "CREATE TRIGGER IF NOT EXISTS trg_"
                        + TABLE_B03_DRAW_QUARANTINE
                        + "_no_update BEFORE UPDATE ON "
                        + TABLE_B03_DRAW_QUARANTINE
                        + " WHEN OLD.source_database<>NEW.source_database OR"
                        + " OLD.source_game_code<>NEW.source_game_code OR"
                        + " OLD.source_table<>NEW.source_table OR"
                        + " OLD.source_issue<>NEW.source_issue OR"
                        + " OLD.source_numbers<>NEW.source_numbers OR OLD.row_hash<>NEW.row_hash"
                        + " BEGIN SELECT RAISE(ABORT,'B03_IMMUTABLE_SOURCE'); END");
    }

    /** R943 user retention policy: recent immutable evidence is still protected. Only rows
     * strictly outside the independently computed retention boundary can be removed. */
    static void installRetentionTriggersR943(SQLiteDatabase db) {
        String[] tables={TABLE_B03_PREDICTION_BUNDLE,TABLE_B03_NUMBER_EVENT,TABLE_B03_ACTION_EVENT,
            TABLE_B03_PERMUTATION_EVENT,TABLE_B03_SELECTED_EVENT,TABLE_B03_AUTHORIZATION_EVENT,
            TABLE_B03_SETTLEMENT_EVENT,TABLE_B03_DRAW_QUARANTINE};
        for(String table:tables){
            try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{table})){
                if(!c.moveToFirst())continue;
            }
            String column=table.equals(TABLE_B03_DRAW_QUARANTINE)?"source_issue":"target_issue";
            int offset=DataRetentionGovernorR900.isHeavy(table)?119:AppConfig.MAX_LOCAL_HISTORY_SAVE-1;
            String trigger="trg_"+table+"_no_delete";
            db.execSQL("DROP TRIGGER IF EXISTS "+trigger);
            db.execSQL("CREATE TRIGGER "+trigger+" BEFORE DELETE ON "+table
                +" WHEN length(OLD."+column+")<>12 OR OLD."+column+">=COALESCE((SELECT issue FROM "+TABLE_DRAWS
                +" WHERE dirty=0 ORDER BY issue DESC LIMIT 1 OFFSET "+offset+"),'')"
                +" BEGIN SELECT RAISE(ABORT,'B03_IMMUTABLE_DELETE_WITHIN_RETENTION'); END");
        }
    }

    private static void putB03Meta(SQLiteDatabase db, String key, String value) {
        android.content.ContentValues cv = new android.content.ContentValues();
        cv.put("meta_key", key);
        cv.put("meta_value", value);
        cv.put("updated_at", System.currentTimeMillis());
        db.insertWithOnConflict(TABLE_B03_SCHEMA_META, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static java.util.List<String> b03TableNames() {
        java.util.ArrayList<String> x = new java.util.ArrayList<String>();
        String[] a = {
            TABLE_B03_SCHEMA_META,
            TABLE_B03_TRANSACTION_AUDIT,
            TABLE_B03_MIGRATION_JOURNAL,
            TABLE_B03_DRAW_QUARANTINE,
            TABLE_B03_PREDICTION_BUNDLE,
            TABLE_B03_NUMBER_EVENT,
            TABLE_B03_ACTION_EVENT,
            TABLE_B03_PERMUTATION_EVENT,
            TABLE_B03_SELECTED_EVENT,
            TABLE_B03_AUTHORIZATION_EVENT,
            TABLE_B03_SETTLEMENT_EVENT,
            TABLE_B03_INTEGRITY_SNAPSHOT,
            TABLE_B03_REPLAY_AUDIT
        };
        for (int i = 0; i < a.length; i++) x.add(a[i]);
        return java.util.Collections.unmodifiableList(x);
    }

    public static java.util.List<String> b03IndexNames() {
        java.util.ArrayList<String> x = new java.util.ArrayList<String>();
        String[] a = {
            "idx_b03_tx_target",
            "idx_b03_migration_status",
            "idx_b03_quarantine_issue",
            "idx_b03_bundle_target",
            "idx_b03_number_target",
            "idx_b03_action_rank",
            "idx_b03_selected_lane",
            "idx_b03_settlement_target",
            "idx_b03_integrity_created",
            "idx_b03_replay_target"
        };
        for (int i = 0; i < a.length; i++) x.add(a[i]);
        return java.util.Collections.unmodifiableList(x);
    }

    public static java.util.List<String> b03TriggerNames() {
        java.util.ArrayList<String> x = new java.util.ArrayList<String>();
        String[] tables = {
            TABLE_B03_PREDICTION_BUNDLE,
            TABLE_B03_NUMBER_EVENT,
            TABLE_B03_ACTION_EVENT,
            TABLE_B03_PERMUTATION_EVENT,
            TABLE_B03_SELECTED_EVENT,
            TABLE_B03_AUTHORIZATION_EVENT,
            TABLE_B03_SETTLEMENT_EVENT,
            TABLE_B03_DRAW_QUARANTINE
        };
        for (int i = 0; i < tables.length; i++) {
            x.add("trg_" + tables[i] + "_no_delete");
            x.add("trg_" + tables[i] + "_no_update");
        }
        return java.util.Collections.unmodifiableList(x);
    }

    public static String b03SchemaHash() {
        StringBuilder b = new StringBuilder();
        java.util.List<StorageRule> x = b03StorageRules();
        for (int i = 0; i < x.size(); i++) {
            if (i > 0) b.append('|');
            b.append(x.get(i).canonical());
        }
        for (String t : b03TableNames()) b.append('|').append(t);
        return V51ChecksumEngine.sha256(b.toString());
    }

    public static java.util.List<StorageRule> b03StorageRules() {
        java.util.ArrayList<StorageRule> rules = new java.util.ArrayList<StorageRule>();
        rules.add(new StorageRule("B03-SCHEMA-001", "DbHelper", "TABLE_OWNER", true, 1));
        rules.add(new StorageRule("B03-SCHEMA-002", "V51Repository", "COLUMN_OWNER", true, 2));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-003", "DatabaseMigrationEngine", "UNIQUE_OWNER", true, 3));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-004",
                        "DatabaseIntegrityVerifier",
                        "FOREIGN_KEY_OWNER",
                        true,
                        4));
        rules.add(new StorageRule("B03-SCHEMA-005", "DbHelper", "TRIGGER_OWNER", true, 5));
        rules.add(new StorageRule("B03-SCHEMA-006", "V51Repository", "HASH_OWNER", true, 6));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-007", "DatabaseMigrationEngine", "GAME_OWNER", true, 7));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-008", "DatabaseIntegrityVerifier", "IMMUTABLE_OWNER", true, 1));
        rules.add(new StorageRule("B03-SCHEMA-009", "DbHelper", "MIGRATION_OWNER", true, 2));
        rules.add(new StorageRule("B03-SCHEMA-010", "V51Repository", "REPLAY_OWNER", true, 3));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-011", "DatabaseMigrationEngine", "TABLE_OWNER", true, 4));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-012", "DatabaseIntegrityVerifier", "COLUMN_OWNER", true, 5));
        rules.add(new StorageRule("B03-SCHEMA-013", "DbHelper", "UNIQUE_OWNER", true, 6));
        rules.add(new StorageRule("B03-SCHEMA-014", "V51Repository", "FOREIGN_KEY_OWNER", true, 7));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-015", "DatabaseMigrationEngine", "TRIGGER_OWNER", true, 1));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-016", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 2));
        rules.add(new StorageRule("B03-SCHEMA-017", "DbHelper", "GAME_OWNER", true, 3));
        rules.add(new StorageRule("B03-SCHEMA-018", "V51Repository", "IMMUTABLE_OWNER", true, 4));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-019", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 5));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-020", "DatabaseIntegrityVerifier", "REPLAY_OWNER", true, 6));
        rules.add(new StorageRule("B03-SCHEMA-021", "DbHelper", "TABLE_OWNER", true, 7));
        rules.add(new StorageRule("B03-SCHEMA-022", "V51Repository", "COLUMN_OWNER", true, 1));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-023", "DatabaseMigrationEngine", "UNIQUE_OWNER", true, 2));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-024",
                        "DatabaseIntegrityVerifier",
                        "FOREIGN_KEY_OWNER",
                        true,
                        3));
        rules.add(new StorageRule("B03-SCHEMA-025", "DbHelper", "TRIGGER_OWNER", true, 4));
        rules.add(new StorageRule("B03-SCHEMA-026", "V51Repository", "HASH_OWNER", true, 5));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-027", "DatabaseMigrationEngine", "GAME_OWNER", true, 6));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-028", "DatabaseIntegrityVerifier", "IMMUTABLE_OWNER", true, 7));
        rules.add(new StorageRule("B03-SCHEMA-029", "DbHelper", "MIGRATION_OWNER", true, 1));
        rules.add(new StorageRule("B03-SCHEMA-030", "V51Repository", "REPLAY_OWNER", true, 2));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-031", "DatabaseMigrationEngine", "TABLE_OWNER", true, 3));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-032", "DatabaseIntegrityVerifier", "COLUMN_OWNER", true, 4));
        rules.add(new StorageRule("B03-SCHEMA-033", "DbHelper", "UNIQUE_OWNER", true, 5));
        rules.add(new StorageRule("B03-SCHEMA-034", "V51Repository", "FOREIGN_KEY_OWNER", true, 6));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-035", "DatabaseMigrationEngine", "TRIGGER_OWNER", true, 7));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-036", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 1));
        rules.add(new StorageRule("B03-SCHEMA-037", "DbHelper", "GAME_OWNER", true, 2));
        rules.add(new StorageRule("B03-SCHEMA-038", "V51Repository", "IMMUTABLE_OWNER", true, 3));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-039", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 4));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-040", "DatabaseIntegrityVerifier", "REPLAY_OWNER", true, 5));
        rules.add(new StorageRule("B03-SCHEMA-041", "DbHelper", "TABLE_OWNER", true, 6));
        rules.add(new StorageRule("B03-SCHEMA-042", "V51Repository", "COLUMN_OWNER", true, 7));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-043", "DatabaseMigrationEngine", "UNIQUE_OWNER", true, 1));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-044",
                        "DatabaseIntegrityVerifier",
                        "FOREIGN_KEY_OWNER",
                        true,
                        2));
        rules.add(new StorageRule("B03-SCHEMA-045", "DbHelper", "TRIGGER_OWNER", true, 3));
        rules.add(new StorageRule("B03-SCHEMA-046", "V51Repository", "HASH_OWNER", true, 4));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-047", "DatabaseMigrationEngine", "GAME_OWNER", true, 5));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-048", "DatabaseIntegrityVerifier", "IMMUTABLE_OWNER", true, 6));
        rules.add(new StorageRule("B03-SCHEMA-049", "DbHelper", "MIGRATION_OWNER", true, 7));
        rules.add(new StorageRule("B03-SCHEMA-050", "V51Repository", "REPLAY_OWNER", true, 1));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-051", "DatabaseMigrationEngine", "TABLE_OWNER", true, 2));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-052", "DatabaseIntegrityVerifier", "COLUMN_OWNER", true, 3));
        rules.add(new StorageRule("B03-SCHEMA-053", "DbHelper", "UNIQUE_OWNER", true, 4));
        rules.add(new StorageRule("B03-SCHEMA-054", "V51Repository", "FOREIGN_KEY_OWNER", true, 5));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-055", "DatabaseMigrationEngine", "TRIGGER_OWNER", true, 6));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-056", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 7));
        rules.add(new StorageRule("B03-SCHEMA-057", "DbHelper", "GAME_OWNER", true, 1));
        rules.add(new StorageRule("B03-SCHEMA-058", "V51Repository", "IMMUTABLE_OWNER", true, 2));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-059", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 3));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-060", "DatabaseIntegrityVerifier", "REPLAY_OWNER", true, 4));
        rules.add(new StorageRule("B03-SCHEMA-061", "DbHelper", "TABLE_OWNER", true, 5));
        rules.add(new StorageRule("B03-SCHEMA-062", "V51Repository", "COLUMN_OWNER", true, 6));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-063", "DatabaseMigrationEngine", "UNIQUE_OWNER", true, 7));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-064",
                        "DatabaseIntegrityVerifier",
                        "FOREIGN_KEY_OWNER",
                        true,
                        1));
        rules.add(new StorageRule("B03-SCHEMA-065", "DbHelper", "TRIGGER_OWNER", true, 2));
        rules.add(new StorageRule("B03-SCHEMA-066", "V51Repository", "HASH_OWNER", true, 3));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-067", "DatabaseMigrationEngine", "GAME_OWNER", true, 4));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-068", "DatabaseIntegrityVerifier", "IMMUTABLE_OWNER", true, 5));
        rules.add(new StorageRule("B03-SCHEMA-069", "DbHelper", "MIGRATION_OWNER", true, 6));
        rules.add(new StorageRule("B03-SCHEMA-070", "V51Repository", "REPLAY_OWNER", true, 7));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-071", "DatabaseMigrationEngine", "TABLE_OWNER", true, 1));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-072", "DatabaseIntegrityVerifier", "COLUMN_OWNER", true, 2));
        rules.add(new StorageRule("B03-SCHEMA-073", "DbHelper", "UNIQUE_OWNER", true, 3));
        rules.add(new StorageRule("B03-SCHEMA-074", "V51Repository", "FOREIGN_KEY_OWNER", true, 4));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-075", "DatabaseMigrationEngine", "TRIGGER_OWNER", true, 5));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-076", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 6));
        rules.add(new StorageRule("B03-SCHEMA-077", "DbHelper", "GAME_OWNER", true, 7));
        rules.add(new StorageRule("B03-SCHEMA-078", "V51Repository", "IMMUTABLE_OWNER", true, 1));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-079", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 2));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-080", "DatabaseIntegrityVerifier", "REPLAY_OWNER", true, 3));
        rules.add(new StorageRule("B03-SCHEMA-081", "DbHelper", "TABLE_OWNER", true, 4));
        rules.add(new StorageRule("B03-SCHEMA-082", "V51Repository", "COLUMN_OWNER", true, 5));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-083", "DatabaseMigrationEngine", "UNIQUE_OWNER", true, 6));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-084",
                        "DatabaseIntegrityVerifier",
                        "FOREIGN_KEY_OWNER",
                        true,
                        7));
        rules.add(new StorageRule("B03-SCHEMA-085", "DbHelper", "TRIGGER_OWNER", true, 1));
        rules.add(new StorageRule("B03-SCHEMA-086", "V51Repository", "HASH_OWNER", true, 2));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-087", "DatabaseMigrationEngine", "GAME_OWNER", true, 3));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-088", "DatabaseIntegrityVerifier", "IMMUTABLE_OWNER", true, 4));
        rules.add(new StorageRule("B03-SCHEMA-089", "DbHelper", "MIGRATION_OWNER", true, 5));
        rules.add(new StorageRule("B03-SCHEMA-090", "V51Repository", "REPLAY_OWNER", true, 6));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-091", "DatabaseMigrationEngine", "TABLE_OWNER", true, 7));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-092", "DatabaseIntegrityVerifier", "COLUMN_OWNER", true, 1));
        rules.add(new StorageRule("B03-SCHEMA-093", "DbHelper", "UNIQUE_OWNER", true, 2));
        rules.add(new StorageRule("B03-SCHEMA-094", "V51Repository", "FOREIGN_KEY_OWNER", true, 3));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-095", "DatabaseMigrationEngine", "TRIGGER_OWNER", true, 4));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-096", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 5));
        rules.add(new StorageRule("B03-SCHEMA-097", "DbHelper", "GAME_OWNER", true, 6));
        rules.add(new StorageRule("B03-SCHEMA-098", "V51Repository", "IMMUTABLE_OWNER", true, 7));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-099", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 1));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-100", "DatabaseIntegrityVerifier", "REPLAY_OWNER", true, 2));
        rules.add(new StorageRule("B03-SCHEMA-101", "DbHelper", "TABLE_OWNER", true, 3));
        rules.add(new StorageRule("B03-SCHEMA-102", "V51Repository", "COLUMN_OWNER", true, 4));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-103", "DatabaseMigrationEngine", "UNIQUE_OWNER", true, 5));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-104",
                        "DatabaseIntegrityVerifier",
                        "FOREIGN_KEY_OWNER",
                        true,
                        6));
        rules.add(new StorageRule("B03-SCHEMA-105", "DbHelper", "TRIGGER_OWNER", true, 7));
        rules.add(new StorageRule("B03-SCHEMA-106", "V51Repository", "HASH_OWNER", true, 1));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-107", "DatabaseMigrationEngine", "GAME_OWNER", true, 2));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-108", "DatabaseIntegrityVerifier", "IMMUTABLE_OWNER", true, 3));
        rules.add(new StorageRule("B03-SCHEMA-109", "DbHelper", "MIGRATION_OWNER", true, 4));
        rules.add(new StorageRule("B03-SCHEMA-110", "V51Repository", "REPLAY_OWNER", true, 5));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-111", "DatabaseMigrationEngine", "TABLE_OWNER", true, 6));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-112", "DatabaseIntegrityVerifier", "COLUMN_OWNER", true, 7));
        rules.add(new StorageRule("B03-SCHEMA-113", "DbHelper", "UNIQUE_OWNER", true, 1));
        rules.add(new StorageRule("B03-SCHEMA-114", "V51Repository", "FOREIGN_KEY_OWNER", true, 2));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-115", "DatabaseMigrationEngine", "TRIGGER_OWNER", true, 3));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-116", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 4));
        rules.add(new StorageRule("B03-SCHEMA-117", "DbHelper", "GAME_OWNER", true, 5));
        rules.add(new StorageRule("B03-SCHEMA-118", "V51Repository", "IMMUTABLE_OWNER", true, 6));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-119", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 7));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-120", "DatabaseIntegrityVerifier", "REPLAY_OWNER", true, 1));
        rules.add(new StorageRule("B03-SCHEMA-121", "DbHelper", "TABLE_OWNER", true, 2));
        rules.add(new StorageRule("B03-SCHEMA-122", "V51Repository", "COLUMN_OWNER", true, 3));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-123", "DatabaseMigrationEngine", "UNIQUE_OWNER", true, 4));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-124",
                        "DatabaseIntegrityVerifier",
                        "FOREIGN_KEY_OWNER",
                        true,
                        5));
        rules.add(new StorageRule("B03-SCHEMA-125", "DbHelper", "TRIGGER_OWNER", true, 6));
        rules.add(new StorageRule("B03-SCHEMA-126", "V51Repository", "HASH_OWNER", true, 7));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-127", "DatabaseMigrationEngine", "GAME_OWNER", true, 1));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-128", "DatabaseIntegrityVerifier", "IMMUTABLE_OWNER", true, 2));
        rules.add(new StorageRule("B03-SCHEMA-129", "DbHelper", "MIGRATION_OWNER", true, 3));
        rules.add(new StorageRule("B03-SCHEMA-130", "V51Repository", "REPLAY_OWNER", true, 4));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-131", "DatabaseMigrationEngine", "TABLE_OWNER", true, 5));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-132", "DatabaseIntegrityVerifier", "COLUMN_OWNER", true, 6));
        rules.add(new StorageRule("B03-SCHEMA-133", "DbHelper", "UNIQUE_OWNER", true, 7));
        rules.add(new StorageRule("B03-SCHEMA-134", "V51Repository", "FOREIGN_KEY_OWNER", true, 1));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-135", "DatabaseMigrationEngine", "TRIGGER_OWNER", true, 2));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-136", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 3));
        rules.add(new StorageRule("B03-SCHEMA-137", "DbHelper", "GAME_OWNER", true, 4));
        rules.add(new StorageRule("B03-SCHEMA-138", "V51Repository", "IMMUTABLE_OWNER", true, 5));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-139", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 6));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-140", "DatabaseIntegrityVerifier", "REPLAY_OWNER", true, 7));
        rules.add(new StorageRule("B03-SCHEMA-141", "DbHelper", "TABLE_OWNER", true, 1));
        rules.add(new StorageRule("B03-SCHEMA-142", "V51Repository", "COLUMN_OWNER", true, 2));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-143", "DatabaseMigrationEngine", "UNIQUE_OWNER", true, 3));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-144",
                        "DatabaseIntegrityVerifier",
                        "FOREIGN_KEY_OWNER",
                        true,
                        4));
        rules.add(new StorageRule("B03-SCHEMA-145", "DbHelper", "TRIGGER_OWNER", true, 5));
        rules.add(new StorageRule("B03-SCHEMA-146", "V51Repository", "HASH_OWNER", true, 6));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-147", "DatabaseMigrationEngine", "GAME_OWNER", true, 7));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-148", "DatabaseIntegrityVerifier", "IMMUTABLE_OWNER", true, 1));
        rules.add(new StorageRule("B03-SCHEMA-149", "DbHelper", "MIGRATION_OWNER", true, 2));
        rules.add(new StorageRule("B03-SCHEMA-150", "V51Repository", "REPLAY_OWNER", true, 3));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-151", "DatabaseMigrationEngine", "TABLE_OWNER", true, 4));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-152", "DatabaseIntegrityVerifier", "COLUMN_OWNER", true, 5));
        rules.add(new StorageRule("B03-SCHEMA-153", "DbHelper", "UNIQUE_OWNER", true, 6));
        rules.add(new StorageRule("B03-SCHEMA-154", "V51Repository", "FOREIGN_KEY_OWNER", true, 7));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-155", "DatabaseMigrationEngine", "TRIGGER_OWNER", true, 1));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-156", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 2));
        rules.add(new StorageRule("B03-SCHEMA-157", "DbHelper", "GAME_OWNER", true, 3));
        rules.add(new StorageRule("B03-SCHEMA-158", "V51Repository", "IMMUTABLE_OWNER", true, 4));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-159", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 5));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-160", "DatabaseIntegrityVerifier", "REPLAY_OWNER", true, 6));
        rules.add(new StorageRule("B03-SCHEMA-161", "DbHelper", "TABLE_OWNER", true, 7));
        rules.add(new StorageRule("B03-SCHEMA-162", "V51Repository", "COLUMN_OWNER", true, 1));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-163", "DatabaseMigrationEngine", "UNIQUE_OWNER", true, 2));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-164",
                        "DatabaseIntegrityVerifier",
                        "FOREIGN_KEY_OWNER",
                        true,
                        3));
        rules.add(new StorageRule("B03-SCHEMA-165", "DbHelper", "TRIGGER_OWNER", true, 4));
        rules.add(new StorageRule("B03-SCHEMA-166", "V51Repository", "HASH_OWNER", true, 5));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-167", "DatabaseMigrationEngine", "GAME_OWNER", true, 6));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-168", "DatabaseIntegrityVerifier", "IMMUTABLE_OWNER", true, 7));
        rules.add(new StorageRule("B03-SCHEMA-169", "DbHelper", "MIGRATION_OWNER", true, 1));
        rules.add(new StorageRule("B03-SCHEMA-170", "V51Repository", "REPLAY_OWNER", true, 2));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-171", "DatabaseMigrationEngine", "TABLE_OWNER", true, 3));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-172", "DatabaseIntegrityVerifier", "COLUMN_OWNER", true, 4));
        rules.add(new StorageRule("B03-SCHEMA-173", "DbHelper", "UNIQUE_OWNER", true, 5));
        rules.add(new StorageRule("B03-SCHEMA-174", "V51Repository", "FOREIGN_KEY_OWNER", true, 6));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-175", "DatabaseMigrationEngine", "TRIGGER_OWNER", true, 7));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-176", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 1));
        rules.add(new StorageRule("B03-SCHEMA-177", "DbHelper", "GAME_OWNER", true, 2));
        rules.add(new StorageRule("B03-SCHEMA-178", "V51Repository", "IMMUTABLE_OWNER", true, 3));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-179", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 4));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-180", "DatabaseIntegrityVerifier", "REPLAY_OWNER", true, 5));
        rules.add(new StorageRule("B03-SCHEMA-181", "DbHelper", "TABLE_OWNER", true, 6));
        rules.add(new StorageRule("B03-SCHEMA-182", "V51Repository", "COLUMN_OWNER", true, 7));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-183", "DatabaseMigrationEngine", "UNIQUE_OWNER", true, 1));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-184",
                        "DatabaseIntegrityVerifier",
                        "FOREIGN_KEY_OWNER",
                        true,
                        2));
        rules.add(new StorageRule("B03-SCHEMA-185", "DbHelper", "TRIGGER_OWNER", true, 3));
        rules.add(new StorageRule("B03-SCHEMA-186", "V51Repository", "HASH_OWNER", true, 4));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-187", "DatabaseMigrationEngine", "GAME_OWNER", true, 5));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-188", "DatabaseIntegrityVerifier", "IMMUTABLE_OWNER", true, 6));
        rules.add(new StorageRule("B03-SCHEMA-189", "DbHelper", "MIGRATION_OWNER", true, 7));
        rules.add(new StorageRule("B03-SCHEMA-190", "V51Repository", "REPLAY_OWNER", true, 1));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-191", "DatabaseMigrationEngine", "TABLE_OWNER", true, 2));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-192", "DatabaseIntegrityVerifier", "COLUMN_OWNER", true, 3));
        rules.add(new StorageRule("B03-SCHEMA-193", "DbHelper", "UNIQUE_OWNER", true, 4));
        rules.add(new StorageRule("B03-SCHEMA-194", "V51Repository", "FOREIGN_KEY_OWNER", true, 5));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-195", "DatabaseMigrationEngine", "TRIGGER_OWNER", true, 6));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-196", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 7));
        rules.add(new StorageRule("B03-SCHEMA-197", "DbHelper", "GAME_OWNER", true, 1));
        rules.add(new StorageRule("B03-SCHEMA-198", "V51Repository", "IMMUTABLE_OWNER", true, 2));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-199", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 3));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-200", "DatabaseIntegrityVerifier", "REPLAY_OWNER", true, 4));
        rules.add(new StorageRule("B03-SCHEMA-201", "DbHelper", "TABLE_OWNER", true, 5));
        rules.add(new StorageRule("B03-SCHEMA-202", "V51Repository", "COLUMN_OWNER", true, 6));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-203", "DatabaseMigrationEngine", "UNIQUE_OWNER", true, 7));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-204",
                        "DatabaseIntegrityVerifier",
                        "FOREIGN_KEY_OWNER",
                        true,
                        1));
        rules.add(new StorageRule("B03-SCHEMA-205", "DbHelper", "TRIGGER_OWNER", true, 2));
        rules.add(new StorageRule("B03-SCHEMA-206", "V51Repository", "HASH_OWNER", true, 3));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-207", "DatabaseMigrationEngine", "GAME_OWNER", true, 4));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-208", "DatabaseIntegrityVerifier", "IMMUTABLE_OWNER", true, 5));
        rules.add(new StorageRule("B03-SCHEMA-209", "DbHelper", "MIGRATION_OWNER", true, 6));
        rules.add(new StorageRule("B03-SCHEMA-210", "V51Repository", "REPLAY_OWNER", true, 7));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-211", "DatabaseMigrationEngine", "TABLE_OWNER", true, 1));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-212", "DatabaseIntegrityVerifier", "COLUMN_OWNER", true, 2));
        rules.add(new StorageRule("B03-SCHEMA-213", "DbHelper", "UNIQUE_OWNER", true, 3));
        rules.add(new StorageRule("B03-SCHEMA-214", "V51Repository", "FOREIGN_KEY_OWNER", true, 4));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-215", "DatabaseMigrationEngine", "TRIGGER_OWNER", true, 5));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-216", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 6));
        rules.add(new StorageRule("B03-SCHEMA-217", "DbHelper", "GAME_OWNER", true, 7));
        rules.add(new StorageRule("B03-SCHEMA-218", "V51Repository", "IMMUTABLE_OWNER", true, 1));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-219", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 2));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-220", "DatabaseIntegrityVerifier", "REPLAY_OWNER", true, 3));
        rules.add(new StorageRule("B03-SCHEMA-221", "DbHelper", "TABLE_OWNER", true, 4));
        rules.add(new StorageRule("B03-SCHEMA-222", "V51Repository", "COLUMN_OWNER", true, 5));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-223", "DatabaseMigrationEngine", "UNIQUE_OWNER", true, 6));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-224",
                        "DatabaseIntegrityVerifier",
                        "FOREIGN_KEY_OWNER",
                        true,
                        7));
        rules.add(new StorageRule("B03-SCHEMA-225", "DbHelper", "TRIGGER_OWNER", true, 1));
        rules.add(new StorageRule("B03-SCHEMA-226", "V51Repository", "HASH_OWNER", true, 2));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-227", "DatabaseMigrationEngine", "GAME_OWNER", true, 3));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-228", "DatabaseIntegrityVerifier", "IMMUTABLE_OWNER", true, 4));
        rules.add(new StorageRule("B03-SCHEMA-229", "DbHelper", "MIGRATION_OWNER", true, 5));
        rules.add(new StorageRule("B03-SCHEMA-230", "V51Repository", "REPLAY_OWNER", true, 6));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-231", "DatabaseMigrationEngine", "TABLE_OWNER", true, 7));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-232", "DatabaseIntegrityVerifier", "COLUMN_OWNER", true, 1));
        rules.add(new StorageRule("B03-SCHEMA-233", "DbHelper", "UNIQUE_OWNER", true, 2));
        rules.add(new StorageRule("B03-SCHEMA-234", "V51Repository", "FOREIGN_KEY_OWNER", true, 3));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-235", "DatabaseMigrationEngine", "TRIGGER_OWNER", true, 4));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-236", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 5));
        rules.add(new StorageRule("B03-SCHEMA-237", "DbHelper", "GAME_OWNER", true, 6));
        rules.add(new StorageRule("B03-SCHEMA-238", "V51Repository", "IMMUTABLE_OWNER", true, 7));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-239", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 1));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-240", "DatabaseIntegrityVerifier", "REPLAY_OWNER", true, 2));
        rules.add(new StorageRule("B03-SCHEMA-241", "DbHelper", "TABLE_OWNER", true, 3));
        rules.add(new StorageRule("B03-SCHEMA-242", "V51Repository", "COLUMN_OWNER", true, 4));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-243", "DatabaseMigrationEngine", "UNIQUE_OWNER", true, 5));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-244",
                        "DatabaseIntegrityVerifier",
                        "FOREIGN_KEY_OWNER",
                        true,
                        6));
        rules.add(new StorageRule("B03-SCHEMA-245", "DbHelper", "TRIGGER_OWNER", true, 7));
        rules.add(new StorageRule("B03-SCHEMA-246", "V51Repository", "HASH_OWNER", true, 1));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-247", "DatabaseMigrationEngine", "GAME_OWNER", true, 2));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-248", "DatabaseIntegrityVerifier", "IMMUTABLE_OWNER", true, 3));
        rules.add(new StorageRule("B03-SCHEMA-249", "DbHelper", "MIGRATION_OWNER", true, 4));
        rules.add(new StorageRule("B03-SCHEMA-250", "V51Repository", "REPLAY_OWNER", true, 5));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-251", "DatabaseMigrationEngine", "TABLE_OWNER", true, 6));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-252", "DatabaseIntegrityVerifier", "COLUMN_OWNER", true, 7));
        rules.add(new StorageRule("B03-SCHEMA-253", "DbHelper", "UNIQUE_OWNER", true, 1));
        rules.add(new StorageRule("B03-SCHEMA-254", "V51Repository", "FOREIGN_KEY_OWNER", true, 2));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-255", "DatabaseMigrationEngine", "TRIGGER_OWNER", true, 3));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-256", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 4));
        rules.add(new StorageRule("B03-SCHEMA-257", "DbHelper", "GAME_OWNER", true, 5));
        rules.add(new StorageRule("B03-SCHEMA-258", "V51Repository", "IMMUTABLE_OWNER", true, 6));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-259", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 7));
        rules.add(
                new StorageRule(
                        "B03-SCHEMA-260", "DatabaseIntegrityVerifier", "REPLAY_OWNER", true, 1));
        return java.util.Collections.unmodifiableList(rules);
    }

    public static boolean b03SelfTest() {
        return b03TableNames().size() == 13
                && b03IndexNames().size() == 10
                && b03TriggerNames().size() == 16
                && b03StorageRules().size() == 260
                && b03SchemaHash().length() == 64
                && "binance_k3_nova_one_pro_v57.db".equals(AppConfig.DB_NAME);
    }
    /* STARCORE_TIANSHU_B03_STORAGE_END */

    // Exact legacy SQL; the incremental Owner installs and protects one table per transaction.
    private static final String[] DIAGNOSIS_TABLES_CP12 = {
        "diagnosis_case_log",
        "module_effect_cache",
        "shadow_promotion_cache",
        "dynamic_freeze_matrix",
        "adaptive_judge_log",
        "diagnosis_fix_plan",
    };
    private static final String[] DIAGNOSIS_CREATE_CP12 = {
        "CREATE TABLE IF NOT EXISTS diagnosis_case_log (_id INTEGER PRIMARY KEY AUTOINCREMENT,case_key TEXT UNIQUE,source_type TEXT,created_at INTEGER DEFAULT 0,updated_at INTEGER DEFAULT 0,base_issue TEXT,target_issue TEXT,decision_type TEXT,stat_scope TEXT,position INTEGER DEFAULT 0,n1 INTEGER DEFAULT 0,n2 INTEGER DEFAULT 0,n3 INTEGER DEFAULT 0,k3_mask INTEGER DEFAULT 0,actual_number INTEGER DEFAULT 0,result TEXT,hit INTEGER DEFAULT 0,pnl REAL DEFAULT 0,final_p REAL DEFAULT 0,dynamic_gate REAL DEFAULT 0,edge REAL DEFAULT 0,raw_p REAL DEFAULT 0,calibrated_p REAL DEFAULT 0,teacher_status TEXT,teacher_sample INTEGER DEFAULT 0,teacher_hit_rate REAL DEFAULT 0,teacher_confidence REAL DEFAULT 0,teacher_recent_validity REAL DEFAULT 0,v20_status TEXT,v20_support REAL DEFAULT 0,v20_risk REAL DEFAULT 0,evidence_contribution REAL DEFAULT 0,counter_contribution REAL DEFAULT 0,sample_reliability REAL DEFAULT 0,recent_validity REAL DEFAULT 0,window_state_key TEXT,similar_window_sample INTEGER DEFAULT 0,similar_window_hit_rate REAL DEFAULT 0,miss_reason TEXT,failure_type TEXT,responsible_module TEXT,fix_hint TEXT,module_name TEXT,module_status TEXT,freeze_key TEXT,freeze_level TEXT,judge_status TEXT,judge_score REAL DEFAULT 0,judge_risk REAL DEFAULT 0,shadow_would_hit INTEGER DEFAULT 0,promotion_hint TEXT,summary_text TEXT,detail_text TEXT,ext_json TEXT)",
        "CREATE TABLE IF NOT EXISTS module_effect_cache (_id INTEGER PRIMARY KEY AUTOINCREMENT,cache_key TEXT UNIQUE,module_name TEXT,created_at INTEGER DEFAULT 0,updated_at INTEGER DEFAULT 0,support_count INTEGER DEFAULT 0,bet_count INTEGER DEFAULT 0,hit_count INTEGER DEFAULT 0,miss_count INTEGER DEFAULT 0,hit_rate REAL DEFAULT 0,avg_final_p REAL DEFAULT 0,avg_gate REAL DEFAULT 0,avg_edge REAL DEFAULT 0,profit REAL DEFAULT 0,shadow_would_hit_rate REAL DEFAULT 0,reliability REAL DEFAULT 0,recent_effectiveness REAL DEFAULT 0,effect_status TEXT,problem TEXT,fix_advice TEXT,summary_text TEXT,detail_text TEXT,ext_json TEXT)",
        "CREATE TABLE IF NOT EXISTS shadow_promotion_cache (_id INTEGER PRIMARY KEY AUTOINCREMENT,promotion_key TEXT UNIQUE,domain_key TEXT,domain_type TEXT,created_at INTEGER DEFAULT 0,updated_at INTEGER DEFAULT 0,total_shadow INTEGER DEFAULT 0,would_hit INTEGER DEFAULT 0,would_miss INTEGER DEFAULT 0,would_hit_rate REAL DEFAULT 0,real_bet_rate REAL DEFAULT 0,promotion_status TEXT,promotion_score REAL DEFAULT 0,reliability REAL DEFAULT 0,watch_count INTEGER DEFAULT 0,blocked_count INTEGER DEFAULT 0,summary_text TEXT,detail_text TEXT,ext_json TEXT)",
        "CREATE TABLE IF NOT EXISTS dynamic_freeze_matrix (_id INTEGER PRIMARY KEY AUTOINCREMENT,freeze_key TEXT UNIQUE,freeze_type TEXT,freeze_value TEXT,freeze_level TEXT,created_at INTEGER DEFAULT 0,updated_at INTEGER DEFAULT 0,frozen INTEGER DEFAULT 0,freeze_until_issue TEXT,freeze_until_time INTEGER DEFAULT 0,real_count INTEGER DEFAULT 0,real_hit INTEGER DEFAULT 0,real_miss INTEGER DEFAULT 0,real_hit_rate REAL DEFAULT 0,shadow_review_count INTEGER DEFAULT 0,shadow_hit INTEGER DEFAULT 0,shadow_miss INTEGER DEFAULT 0,shadow_hit_rate REAL DEFAULT 0,recover_need_hits INTEGER DEFAULT 0,recover_need_rate REAL DEFAULT 0,freeze_reason TEXT,recover_hint TEXT,summary_text TEXT,detail_text TEXT,ext_json TEXT)",
        "CREATE TABLE IF NOT EXISTS adaptive_judge_log (_id INTEGER PRIMARY KEY AUTOINCREMENT,judge_key TEXT UNIQUE,created_at INTEGER DEFAULT 0,updated_at INTEGER DEFAULT 0,base_issue TEXT,target_issue TEXT,position INTEGER DEFAULT 0,n1 INTEGER DEFAULT 0,n2 INTEGER DEFAULT 0,n3 INTEGER DEFAULT 0,k3_mask INTEGER DEFAULT 0,final_p REAL DEFAULT 0,dynamic_gate REAL DEFAULT 0,edge REAL DEFAULT 0,teacher_status TEXT,v20_status TEXT,window_state_key TEXT,judge_status TEXT,judge_score REAL DEFAULT 0,judge_risk REAL DEFAULT 0,pass INTEGER DEFAULT 0,downgrade_to_shadow INTEGER DEFAULT 0,freeze INTEGER DEFAULT 0,promote_shadow_candidate INTEGER DEFAULT 0,freeze_key TEXT,freeze_level TEXT,reason_text TEXT,module_advice TEXT,summary_text TEXT,detail_text TEXT,ext_json TEXT)",
        "CREATE TABLE IF NOT EXISTS diagnosis_fix_plan (_id INTEGER PRIMARY KEY AUTOINCREMENT,plan_key TEXT UNIQUE,created_at INTEGER DEFAULT 0,updated_at INTEGER DEFAULT 0,file_name TEXT,priority INTEGER DEFAULT 0,overall_problem TEXT,problem_evidence TEXT,why_this_file TEXT,what_to_change TEXT,expected_effect TEXT,risk_if_not_change TEXT,dependency TEXT,new_engine_needed INTEGER DEFAULT 0,strategy_gap TEXT,risk_warning TEXT,summary_text TEXT,detail_text TEXT,ext_json TEXT)",
    };
    private static final String[] DIAGNOSIS_COLUMNS_CP12 = {
        "record_key TEXT",
        "source_type TEXT",
        "created_at INTEGER DEFAULT 0",
        "updated_at INTEGER DEFAULT 0",
        "base_issue TEXT",
        "target_issue TEXT",
        "position INTEGER DEFAULT 0",
        "final_p REAL DEFAULT 0",
        "dynamic_gate REAL DEFAULT 0",
        "edge REAL DEFAULT 0",
        "teacher_status TEXT",
        "v20_status TEXT",
        "window_state_key TEXT",
        "module_name TEXT",
        "status TEXT",
        "summary_text TEXT",
        "detail_text TEXT",
        "ext_json TEXT",
    };
    private static final String[][] DIAGNOSIS_INDEXES_CP12 = {
        {
            "CREATE INDEX IF NOT EXISTS idx_diag_case_target ON diagnosis_case_log(target_issue,position)",
            "CREATE INDEX IF NOT EXISTS idx_diag_case_created ON diagnosis_case_log(created_at)",
            "CREATE INDEX IF NOT EXISTS idx_diag_case_failure ON diagnosis_case_log(failure_type,responsible_module)",
            "CREATE INDEX IF NOT EXISTS idx_diag_case_shadow ON diagnosis_case_log(decision_type,shadow_would_hit)",
        },
        {
            "CREATE INDEX IF NOT EXISTS idx_module_effect_name ON module_effect_cache(module_name,effect_status)",
            "CREATE INDEX IF NOT EXISTS idx_module_effect_update ON module_effect_cache(updated_at)",
        },
        {
            "CREATE INDEX IF NOT EXISTS idx_shadow_promotion_domain ON shadow_promotion_cache(domain_type,domain_key)",
            "CREATE INDEX IF NOT EXISTS idx_shadow_promotion_status ON shadow_promotion_cache(promotion_status,promotion_score)",
        },
        {
            "CREATE INDEX IF NOT EXISTS idx_freeze_matrix_level ON dynamic_freeze_matrix(freeze_level,frozen)",
            "CREATE INDEX IF NOT EXISTS idx_freeze_matrix_type ON dynamic_freeze_matrix(freeze_type,freeze_value)",
            "CREATE INDEX IF NOT EXISTS idx_freeze_matrix_until ON dynamic_freeze_matrix(freeze_until_time)",
        },
        {
            "CREATE INDEX IF NOT EXISTS idx_adaptive_judge_target ON adaptive_judge_log(target_issue,position)",
            "CREATE INDEX IF NOT EXISTS idx_adaptive_judge_status ON adaptive_judge_log(judge_status,judge_score,judge_risk)",
            "CREATE INDEX IF NOT EXISTS idx_adaptive_judge_created ON adaptive_judge_log(created_at)",
        },
        {
            "CREATE INDEX IF NOT EXISTS idx_fix_plan_priority ON diagnosis_fix_plan(priority,file_name)",
            "CREATE INDEX IF NOT EXISTS idx_fix_plan_update ON diagnosis_fix_plan(updated_at)",
        },
    };
    /** Synchronous compatibility path; incremental research uses one table at a time. */
    public static void installDiagnosisLoopSchemaCP12(SQLiteDatabase db) {
        for (String table : DIAGNOSIS_TABLES_CP12) installDiagnosisLoopSchemaUnitCP12(db, table);
        requireDiagnosisLoopSchemaCP12(db);
    }

    /** Called with the matching retention unit in the same Owner transaction. No readiness cache. */
    public static void installDiagnosisLoopSchemaUnitCP12(SQLiteDatabase db, String table) {
        if (db == null || !db.inTransaction()) throw new IllegalStateException("DIAGNOSIS_SCHEMA_OWNER");
        int unit = -1;
        for (int i = 0; i < DIAGNOSIS_TABLES_CP12.length; i++) if (DIAGNOSIS_TABLES_CP12[i].equals(table)) unit = i;
        if (unit < 0) throw new IllegalArgumentException("DIAGNOSIS_SCHEMA_TABLE");
        db.execSQL(DIAGNOSIS_CREATE_CP12[unit]);
        java.util.HashSet<String> columns = new java.util.HashSet<>();
        try (Cursor cursor = db.rawQuery("PRAGMA table_info(\"" + table + "\")", null)) {
            while (cursor.moveToNext()) columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")));
        }
        for (String definition : DIAGNOSIS_COLUMNS_CP12) {
            String name = definition.substring(0, definition.indexOf(' '));
            if (!columns.contains(name)) db.execSQL("ALTER TABLE \"" + table + "\" ADD COLUMN " + definition);
        }
        for (String sql : DIAGNOSIS_INDEXES_CP12[unit]) db.execSQL(sql);
    }

    /** Runtime readers never create schema or write a migration receipt. */
    public static void requireDiagnosisLoopSchemaCP12(SQLiteDatabase db) {
        if (db == null) throw new IllegalStateException("DIAGNOSIS_SCHEMA_NOT_READY");
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name IN ('diagnosis_case_log','module_effect_cache','shadow_promotion_cache','dynamic_freeze_matrix','adaptive_judge_log','diagnosis_fix_plan')", null)) {
            if (!cursor.moveToFirst() || cursor.getInt(0) != 6) throw new IllegalStateException("DIAGNOSIS_SCHEMA_NOT_READY");
        }
    }
}
