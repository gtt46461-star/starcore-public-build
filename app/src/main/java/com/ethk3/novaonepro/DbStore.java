package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * DbStore.java
 *
 * <p>第4/22 主链闭环存储中心升级文件。
 *
 * <p>V20 真实账本 + 因链矩阵 + 满级诊断闭环存储中心满血升级版 / Java7 AIDE版
 *
 * <p>本文件只负责： 1. 保存、读取、结算、统计。 2. 不改变 finalP / dynamicGate / edge。 3. 不用 TeacherMatrix 在数据库层救援 BET。
 * 4. BET 决策保存后必须创建真实 Trade。 5. 真实统计只认 trades + action=BET + stat_scope=REAL_TRADE + settled=1 +
 * WIN/MISS。 6. SHADOW / HOLD / QUICK_OBSERVE / 占位 HOLD 不进入真实盈亏。 7. 结算严格单位置K3：只看预测位置 actual 是否等于
 * n1/n2/n3。 8. 修复“结算了但真实盈亏不变化”的账本链路。 9. 修复正式 BET 被后续 HOLD / SHADOW / 临时占位覆盖的问题。 10. 兼容旧库
 * action/stat_scope 为空、REAL、REAL_TRADE 的旧数据。 11. 新增满级诊断闭环存储：逐单诊断、模块评分、SHADOW晋级、动态冻结、智能审判、修复计划。 12.
 * DbStore 仍然只做存取，不决定 BET，不救援 Trade，不污染真实盈亏。 13. 第2/4联调补丁：补齐 Decision/Trade/SHADOW
 * 闭环字段建列、保存、读取、读回修复。 14. 第2/4联调补丁：真实Trade创建前做存储层安全保护，字段链危险BET不自动建Trade。 15. 第4/22
 * 主链闭环修复：保存/读取前40候选、Adaptive输入、SHADOW反事实、Decision最终字段、Trade真实字段。 16. DbStore
 * 不计算概率、不救援BET、不新增BET；只修复已由DecisionGateEngine输出的BET对应Trade链。
 */
public final class DbStore {
    private static final long R811_DRAW_CONFLICT_BLOCKED = -2L;
    public static final String V48_DBSTORE_VERSION = V48BuildManifest.STRATEGY_VERSION;
    public static final String MODULE_BUILD_VERSION = AppConfig.ACTIVE_QUALITY_BUILD_VERSION;
    public static final String OUMAN_GALAXY_REBUILD_VERSION =
            AppConfig.OUMAN_GALAXY_REBUILD_VERSION;

    public static final String DBSTORE_DEADLOCK_RECOVERY_VERSION =
            "V48_POINT_IN_TIME_MODEL_LAB_SINGLE_ACTION_CHAMPION_FULL_DIAGNOSIS_20260721_48072148";

    private static final String TABLE_REALTIME_VALIDATION_REPORT = DbHelper.TABLE_REALTIME_VALIDATE;
    private static final String TABLE_TEACHER_SNAPSHOT = DbHelper.TABLE_TEACHER_SNAPSHOT;

    private static final String TABLE_RUNTIME_PROFILE_CACHE = DbHelper.TABLE_RUNTIME_PROFILE_CACHE;
    private static final String TABLE_STATE_NUMBER_PROFILE = DbHelper.TABLE_STATE_NUMBER_PROFILE;
    private static final String TABLE_LEARNING_CASE_LOG = DbHelper.TABLE_LEARNING_CASE_LOG;
    private static final String TABLE_AUTO_LEARN_CURSOR = DbHelper.TABLE_AUTO_LEARN_CURSOR;

    private static final String TABLE_DYNAMIC_WINDOW_CLUSTER =
            DbHelper.TABLE_DYNAMIC_WINDOW_CLUSTER;
    private static final String TABLE_WINDOW_FEATURE_SAMPLE = DbHelper.TABLE_WINDOW_FEATURE_SAMPLE;
    private static final String TABLE_POSITION_NEED_SAMPLE = DbHelper.TABLE_POSITION_NEED_SAMPLE;
    private static final String TABLE_NUMBER_SUPPLY_SAMPLE = DbHelper.TABLE_NUMBER_SUPPLY_SAMPLE;
    private static final String TABLE_NEED_SUPPLY_MATCH_SAMPLE =
            DbHelper.TABLE_NEED_SUPPLY_MATCH_SAMPLE;
    private static final String TABLE_COUNTERFACTUAL_REASON_SAMPLE =
            DbHelper.TABLE_COUNTERFACTUAL_REASON_SAMPLE;
    private static final String TABLE_CAUSE_ABLATION_SAMPLE = DbHelper.TABLE_CAUSE_ABLATION_SAMPLE;
    private static final String TABLE_MISS_REASON_SAMPLE = DbHelper.TABLE_MISS_REASON_SAMPLE;
    private static final String TABLE_ADAPTIVE_JUDGE_SAMPLE = DbHelper.TABLE_ADAPTIVE_JUDGE_SAMPLE;
    private static final String TABLE_PROMOTION_DOMAIN_SNAPSHOT =
            DbHelper.TABLE_PROMOTION_DOMAIN_SNAPSHOT;
    private static final String TABLE_DYNAMIC_CAUSAL_STATE = DbHelper.TABLE_DYNAMIC_CAUSAL_STATE;

    private static final String TABLE_TEACHER_MATRIX_SAMPLE = DbHelper.TABLE_TEACHER_MATRIX_SAMPLE;
    private static final String TABLE_TEACHER_MATRIX_PROFILE =
            DbHelper.TABLE_TEACHER_MATRIX_PROFILE;
    private static final String TABLE_TEACHER_MATRIX_COMBO_PROFILE =
            DbHelper.TABLE_TEACHER_MATRIX_COMBO_PROFILE;
    private static final String TABLE_TEACHER_MATRIX_REPORT = DbHelper.TABLE_TEACHER_MATRIX_REPORT;

    private static final String AUTO_LEARN_STATUS_IDLE = "IDLE";
    private static final String AUTO_LEARN_STATUS_RUNNING = "RUNNING";

    private static final int LEDGER_REPAIR_LIMIT = 600;
    private static final String V20_FIELD_CHAIN_DBSTORE_VERSION =
            AppConfig.ACTIVE_QUALITY_BUILD_VERSION;
    private static final String V20_DYNAMIC_CAUSAL_DBSTORE_VERSION =
            "V48_POINT_IN_TIME_MODEL_LAB_SINGLE_ACTION_CHAMPION_FULL_DIAGNOSIS_20260721_48072148";
    private static final String DBSTORE_MAIN_CHAIN_CLOSED_LOOP_VERSION =
            "V48_POINT_IN_TIME_MODEL_LAB_SINGLE_ACTION_CHAMPION_FULL_DIAGNOSIS_20260721_48072148";
    private static final String TABLE_MAIN_CHAIN_CANDIDATE_SNAPSHOT =
            DbHelper.TABLE_MAIN_CHAIN_CANDIDATE_SNAPSHOT;
    private static final String TABLE_MAIN_CHAIN_REJECT_BUCKET =
            DbHelper.TABLE_MAIN_CHAIN_REJECT_BUCKET;
    private static final String TABLE_V20_FREQUENCY_STATE = DbHelper.TABLE_V20_FREQUENCY_STATE;
    private static final String TABLE_V20_LAYER_SNAPSHOT = DbHelper.TABLE_V20_LAYER_SNAPSHOT;
    private static final String TABLE_V20_BACKGROUND_CURSOR = DbHelper.TABLE_V20_BACKGROUND_CURSOR;
    private static final String TABLE_V20_SHADOW_DOMAIN_STAT =
            DbHelper.TABLE_V20_SHADOW_DOMAIN_STAT;
    private static final String TABLE_V20_LINK_CANDIDATE_SNAPSHOT =
            DbHelper.TABLE_V20_LINK_CANDIDATE_SNAPSHOT;
    private static final String DBSTORE_LINK_MODIFICATION_V2_VERSION =
            "V48_POINT_IN_TIME_MODEL_LAB_SINGLE_ACTION_CHAMPION_FULL_DIAGNOSIS_20260721_48072148";
    private static final int MAIN_CHAIN_REALTIME_TOP40_LIMIT = 40;
    private static final int MAIN_CHAIN_CANDIDATE_SNAPSHOT_KEEP = 160;
    private static final String REJECT_CODE_NONE = "";
    private static final double FIELD_CHAIN_EDGE_FALSE_STRONG_LINE = 0.020d;
    private static final double FIELD_CHAIN_P_NO_STRONG_CAUSE_LINE = 0.415d;
    private static final double FIELD_CHAIN_K3_COVERAGE_RISK_LINE = 0.620d;
    private static final double FIELD_CHAIN_K3_COVERAGE_HARD_RISK = 0.700d;
    private static final double FIELD_CHAIN_THIRD_NUMBER_MIN = 0.088d;

    private final Context researchAppCP12;
    private final java.io.File researchInputRootCP11;
    private final DatabaseCoordinator coordinator;
    private final DbHelper helper;
    private final HashMap<String, HashSet<String>> columnCache;

    public java.io.InputStream openResearchAssetCP12(String path) throws java.io.IOException {
        if (!"research/cp4_molecule_seeds.jsonl".equals(path)) throw new IllegalArgumentException("TRAIN_ASSET_PATH");
        return researchAppCP12.getAssets().open(path);
    }
    /** API21 asset seek: openFd requires noCompress; pin identity is generated once.
     * Only the importer owns bytes/cursor/SHA. No repeated prefix skip or file array. */
    public java.io.InputStream openResearchAtomCatalogAtCP12(long byteOffset) throws java.io.IOException {
        final long pinnedBytes=com.ethk3.novaonepro.r952.AtomCatalogPinnedCP12.BYTES;
        if(byteOffset<0||byteOffset>pinnedBytes)throw new IllegalArgumentException("ATOM_ASSET_OFFSET");
        final android.content.res.AssetFileDescriptor asset=researchAppCP12.getAssets().openFd(com.ethk3.novaonepro.r952.AtomCatalogPinnedCP12.ASSET);
        java.io.FileInputStream opened=null;
        try {
            if(asset.getLength()!=pinnedBytes||asset.getStartOffset()<0||asset.getStartOffset()>Long.MAX_VALUE-byteOffset)throw new java.io.IOException("ATOM_ASSET_PINNED_SIZE");
            opened=new java.io.FileInputStream(asset.getFileDescriptor());opened.getChannel().position(asset.getStartOffset()+byteOffset);
            final java.io.FileInputStream file=opened;final long available=pinnedBytes-byteOffset;
            return new java.io.InputStream(){private long left=available;private boolean closed;
                private void check()throws java.io.IOException{if(closed)throw new java.io.IOException("ATOM_ASSET_CLOSED");}
                public int read()throws java.io.IOException{check();if(left==0)return -1;int value=file.read();if(value<0)throw new java.io.EOFException("ATOM_ASSET_TRUNCATED");left--;return value;}
                public int read(byte[] bytes,int offset,int count)throws java.io.IOException{check();if(bytes==null)throw new NullPointerException();if(offset<0||count<0||offset>bytes.length-count)throw new IndexOutOfBoundsException();if(count==0)return 0;if(left==0)return -1;int n=file.read(bytes,offset,(int)Math.min(left,count));if(n<0)throw new java.io.EOFException("ATOM_ASSET_TRUNCATED");left-=n;return n;}
                public void close()throws java.io.IOException{if(closed)return;closed=true;try{file.close();}finally{asset.close();}}
            };
        }catch(java.io.IOException|RuntimeException|Error failed){if(opened!=null)try{opened.close();}catch(java.io.IOException ignored){}try{asset.close();}catch(java.io.IOException ignored){}throw failed;}
    }
    public java.io.InputStream openResearchMoleculeCatalogAtCP12(long byteOffset) throws java.io.IOException {
        final long pinnedBytes=com.ethk3.novaonepro.r952.MoleculeSeedPageCP12.BYTES;
        if(byteOffset<0||byteOffset>pinnedBytes)throw new IllegalArgumentException("MOLECULE_ASSET_OFFSET");
        final android.content.res.AssetFileDescriptor asset=researchAppCP12.getAssets().openFd(com.ethk3.novaonepro.r952.ModernSeedFactoryCP12.ASSET);
        java.io.FileInputStream opened=null;
        try {
            if(asset.getLength()!=pinnedBytes||asset.getStartOffset()<0||asset.getStartOffset()>Long.MAX_VALUE-byteOffset)throw new java.io.IOException("MOLECULE_ASSET_PINNED_SIZE");
            opened=new java.io.FileInputStream(asset.getFileDescriptor());opened.getChannel().position(asset.getStartOffset()+byteOffset);
            final java.io.FileInputStream file=opened;final long available=pinnedBytes-byteOffset;
            return new java.io.InputStream(){private long left=available;private boolean closed;
                private void check()throws java.io.IOException{if(closed)throw new java.io.IOException("MOLECULE_ASSET_CLOSED");}
                public int read()throws java.io.IOException{check();if(left==0)return -1;int value=file.read();if(value<0)throw new java.io.EOFException("MOLECULE_ASSET_TRUNCATED");left--;return value;}
                public int read(byte[] bytes,int offset,int count)throws java.io.IOException{check();if(bytes==null)throw new NullPointerException();if(offset<0||count<0||offset>bytes.length-count)throw new IndexOutOfBoundsException();if(count==0)return 0;if(left==0)return -1;int n=file.read(bytes,offset,(int)Math.min(left,count));if(n<0)throw new java.io.EOFException("MOLECULE_ASSET_TRUNCATED");left-=n;return n;}
                public void close()throws java.io.IOException{if(closed)return;closed=true;try{file.close();}finally{asset.close();}}
            };
        }catch(java.io.IOException|RuntimeException|Error failed){if(opened!=null)try{opened.close();}catch(java.io.IOException ignored){}try{asset.close();}catch(java.io.IOException ignored){}throw failed;}
    }
    public java.io.File researchInputRootCP11() {
        if (!researchInputRootCP11.isDirectory() && !researchInputRootCP11.mkdirs())
            throw new IllegalStateException("TRAIN_INPUT_DIRECTORY");
        return researchInputRootCP11;
    }

    public DbStore(Context context) {
        Context app = context == null ? null : context.getApplicationContext();
        ModelBundleRuntimeR932.configure(app == null ? context : app);
        Context retainedApp = app == null ? context : app;
        if (retainedApp == null) throw new IllegalArgumentException("TRAIN_CONTEXT_REQUIRED");
        researchAppCP12 = retainedApp;
        researchInputRootCP11 = new java.io.File(retainedApp.getFilesDir(), "research-input-v2");
        coordinator = DatabaseCoordinator.get(retainedApp);
        helper = coordinator.getHelper();
        columnCache = new HashMap<String, HashSet<String>>();
        coordinator.ensureCriticalSchemaOrThrow(
                new DatabaseCoordinator.SchemaInitializer() {
                    @Override
                    public void run(SQLiteDatabase db) {
                        clearColumnCache();
                    }
                });
        // Establish the R816 diagnostic boundary before any UI/service reader can mistake old
        // OOM or latency events for failures produced by the upgraded runtime. Historical rows
        // remain immutable and are still reported by lifetime counters.
        RuntimeReleaseEpochR817.ensure(this);
        RuntimeObservabilityR925.ensureEpoch(this);
        if (!RuntimeEventLedgerR926.startupReconciledR971()) coordinator.runWrite(
                "R926_EVENT_STARTUP_RECONCILE",
                new DatabaseCoordinator.WriteRunnable() {
                    @Override
                    public void run(SQLiteDatabase db) {
                        RuntimeEventLedgerR926.reconcileStartupOnce(db, 500);
                    }
                });
    }

    public void close() {
        /* DbHelper由DatabaseCoordinator进程单例持有，单个DbStore关闭不得关闭共享连接。 */
    }

    public SQLiteDatabase readable() {
        return coordinator.readable();
    }

    public SQLiteDatabase writable() {
        return coordinator.writable();
    }

    DatabaseCoordinator coordinatorR36() {
        return coordinator;
    }

    /**
     * R952 extension boundary.  Research modules must use the process-wide coordinator instead of
     * opening an independent SQLite writer or owning a second transaction queue.
     */
    public DatabaseCoordinator coordinatorForResearchR952() {
        return coordinator;
    }

    public String startupSchemaSummaryR363() {
        return coordinator.startupSchemaSummary();
    }

    static void putStatesR36(SQLiteDatabase db, Map<String, String> values) {
        if (db == null || values == null || values.size() == 0) return;
        long now = System.currentTimeMillis();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.length() == 0) continue;
            ContentValues cv = new ContentValues();
            cv.put("state_key", key);
            cv.put("state_value", entry.getValue() == null ? "" : entry.getValue());
            cv.put("updated_at", now);
            db.insertWithOnConflict(
                    DbHelper.TABLE_APP_STATE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    public void clearColumnCache() {
        synchronized (columnCache) {
            columnCache.clear();
        }
    }

    public int repairLedgerIntegrityNow() {
        SQLiteDatabase db = writable();
        ensureRuntimeExtraTablesOnce(db);

        int repaired = 0;

        try {
            normalizeLegacyLedgerScopes(db, LEDGER_REPAIR_LIMIT * 5);
            repaired += repairMissingRealTradesFromBetDecisions(db, LEDGER_REPAIR_LIMIT);
            repaired += repairTradeDecisionLinks(db, LEDGER_REPAIR_LIMIT);
            repaired += repairSettledTradePnl(db, LEDGER_REPAIR_LIMIT);
            repaired += cleanupDuplicateRealTradesAll(db, LEDGER_REPAIR_LIMIT);
        } catch (Throwable e) {
        }

        return repaired;
    }

    private void ensureRuntimeExtraTablesOnce(final SQLiteDatabase db) {
        boolean ready =
                coordinator.ensureRuntimeSchemaOnce(
                        new DatabaseCoordinator.SchemaInitializer() {
                            @Override
                            public void run(SQLiteDatabase ignored) {
                                SQLiteDatabase target = db == null ? ignored : db;
                                ensureRuntimeExtraTables(target);
                                verifyLegacyRuntimeTablesR363(target);
                                clearColumnCache();
                            }
                        });
        if (!ready)
            throw new IllegalStateException(
                    "LEGACY_RUNTIME_SCHEMA_NOT_READY|" + coordinator.startupSchemaSummary());
    }

    private void verifyLegacyRuntimeTablesR363(SQLiteDatabase db) {
        String[] required =
                new String[] {
                    TABLE_REALTIME_VALIDATION_REPORT,
                    TABLE_TEACHER_SNAPSHOT,
                    TABLE_LEARNING_CASE_LOG,
                    TABLE_RUNTIME_PROFILE_CACHE,
                    TABLE_STATE_NUMBER_PROFILE,
                    TABLE_AUTO_LEARN_CURSOR,
                    TABLE_TEACHER_MATRIX_SAMPLE,
                    TABLE_TEACHER_MATRIX_PROFILE,
                    TABLE_TEACHER_MATRIX_COMBO_PROFILE,
                    TABLE_TEACHER_MATRIX_REPORT
                };
        for (int i = 0; i < required.length; i++) {
            if (getTableColumns(db, required[i]).isEmpty())
                throw new IllegalStateException("LEGACY_REQUIRED_TABLE_MISSING:" + required[i]);
        }
    }

    private void ensureRuntimeExtraTables(SQLiteDatabase db) {
        if (db == null) {
            return;
        }

        ensureRealtimeValidationReportTable(db);
        ensureTeacherSnapshotTable(db);
        ensureLearningCaseLogTable(db);
        ensureRuntimeProfileCacheTable(db);
        ensureStateNumberProfileTable(db);
        ensureAutoLearnCursorTable(db);
        ensureTeacherMatrixSampleTable(db);
        ensureTeacherMatrixProfileTable(db);
        ensureTeacherMatrixComboProfileTable(db);
        ensureTeacherMatrixReportTable(db);
        ensureRuntimeExtraColumns(db);
        ensureTeacherMatrixColumns(db);
        ensureLedgerColumns(db);
        ensureLedgerIndexes(db);
        ensureV20CauseChainReady(db);
        ensureFieldChainColumns(db);
        ensureDynamicCausalWindowReady(db);
    }

    public void ensureLearningTables() {
        try {
            SQLiteDatabase db = writable();
            ensureLearningCaseLogTable(db);
            ensureRuntimeProfileCacheTable(db);
            ensureStateNumberProfileTable(db);
            ensureAutoLearnCursorTable(db);
            ensureTeacherMatrixSampleTable(db);
            ensureTeacherMatrixProfileTable(db);
            ensureTeacherMatrixComboProfileTable(db);
            ensureTeacherMatrixReportTable(db);
            ensureRuntimeExtraColumns(db);
            ensureTeacherMatrixColumns(db);
            ensureLedgerColumns(db);
            ensureLedgerIndexes(db);
            ensureV20CauseChainReady(db);
            ensureFieldChainColumns(db);
            ensureDynamicCausalWindowReady(db);
            ensureLinkModificationV2Ready(db);
            clearColumnCache();
        } catch (Throwable e) {
        }
    }

    private void ensureDynamicCausalWindowReady(SQLiteDatabase db) {
        if (db == null) return;
        try {
            ensureDynamicCausalTables(db);
            ensureDynamicCausalDecisionLikeColumns(db, DbHelper.TABLE_DECISIONS);
            ensureDynamicCausalDecisionLikeColumns(db, DbHelper.TABLE_TRADES);
            ensureDynamicCausalDecisionLikeColumns(db, DbHelper.TABLE_SHADOW);
            ensureDynamicCausalDecisionLikeColumns(db, DbHelper.TABLE_FULL_CANDIDATE_SCAN);
            ensureDynamicCausalDecisionLikeColumns(db, DbHelper.TABLE_CANDIDATE_AUDIT);
            ensureDynamicCausalDecisionLikeColumns(db, DbHelper.TABLE_DYNAMIC_CAUSE_CHAIN);
            ensureDynamicCausalDecisionLikeColumns(db, DbHelper.TABLE_DECISION_AUDIT);
            ensureDynamicCausalDecisionLikeColumns(db, DbHelper.TABLE_V20_CLOSED_LOOP);
            ensureDynamicCausalIndexes(db);
            ensureDynamicCausalDefaultState(db);
            clearColumnCache();
        } catch (Throwable e) {
        }
    }

    private void ensureMainChainClosedLoopReady(SQLiteDatabase db) {
        if (db == null) {
            return;
        }
        try {
            execQuiet(
                    db,
                    "CREATE TABLE IF NOT EXISTS "
                            + TABLE_MAIN_CHAIN_CANDIDATE_SNAPSHOT
                            + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, snapshot_key TEXT NOT NULL"
                            + " UNIQUE)");
            execQuiet(
                    db,
                    "CREATE TABLE IF NOT EXISTS "
                            + TABLE_MAIN_CHAIN_REJECT_BUCKET
                            + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, bucket_key TEXT NOT NULL"
                            + " UNIQUE)");
            ensureMainChainColumns(db, DbHelper.TABLE_DECISIONS);
            ensureMainChainColumns(db, DbHelper.TABLE_TRADES);
            ensureMainChainColumns(db, DbHelper.TABLE_SHADOW);
            ensureMainChainColumns(db, DbHelper.TABLE_FULL_CANDIDATE_SCAN);
            ensureMainChainColumns(db, DbHelper.TABLE_CANDIDATE_AUDIT);
            ensureMainChainColumns(db, DbHelper.TABLE_DYNAMIC_CAUSE_CHAIN);
            ensureMainChainColumns(db, DbHelper.TABLE_DECISION_AUDIT);
            ensureMainChainColumns(db, DbHelper.TABLE_V20_CLOSED_LOOP);
            ensureMainChainColumns(db, TABLE_MAIN_CHAIN_CANDIDATE_SNAPSHOT);
            ensureMainChainColumns(db, DbHelper.TABLE_REAL_TRADE_LEDGER);
            ensureMainChainColumns(db, TABLE_ADAPTIVE_JUDGE_SAMPLE);
            ensureMainChainColumns(db, TABLE_PROMOTION_DOMAIN_SNAPSHOT);
            ensureMainChainRejectBucketColumns(db);
            ensureMainChainFactorColumns(db);
            ensureMainChainIndexes(db);
            ensureMainChainDefaultState(db);
            clearColumnCache();
        } catch (Throwable e) {
        }
    }

    private void ensureMainChainColumns(SQLiteDatabase db, String table) {
        if (db == null || table == null || table.length() == 0) return;
        String[] defs = mainChainClosedLoopColumnDefs();
        addColumnArray(db, table, defs);
    }

    private String[] mainChainClosedLoopColumnDefs() {
        return new String[] {
            "snapshot_key TEXT",
            "candidate_key TEXT",
            "candidate_rank INTEGER DEFAULT 0",
            "realtime_top40_rank INTEGER DEFAULT 0",
            "adaptive_input_flag INTEGER DEFAULT 0",
            "window_state_key TEXT",
            "dynamic_window_state_key TEXT",
            "final_decision_window_state_key TEXT",
            "probability_window_state_key TEXT",
            "cluster_key TEXT",
            "factor_key TEXT",
            "position_need_score REAL DEFAULT 0",
            "number_supply_score REAL DEFAULT 0",
            "coverage_quality REAL DEFAULT 0",
            "third_quality REAL DEFAULT 0",
            "counter_evidence_score REAL DEFAULT 0",
            "similar_sample_count INTEGER DEFAULT 0",
            "similar_hit_rate REAL DEFAULT 0",
            "teacher_status TEXT",
            "teacher_status_class TEXT",
            "teacher_number_status TEXT",
            "teacher_sample_reliability REAL DEFAULT 0",
            "teacher_recent_validity REAL DEFAULT 0",
            "teacher_evidence_contribution REAL DEFAULT 0",
            "teacher_counter_contribution REAL DEFAULT 0",
            "v20_status TEXT",
            "v20_action_advice TEXT",
            "v20_sample_reliability REAL DEFAULT 0",
            "v20_recent_validity REAL DEFAULT 0",
            "v20_evidence_contribution REAL DEFAULT 0",
            "v20_counter_contribution REAL DEFAULT 0",
            "adaptive_status TEXT",
            "shadow_promotion_status TEXT",
            "final_p_before_diagnosis REAL DEFAULT 0",
            "final_p_after_diagnosis REAL DEFAULT 0",
            "gate REAL DEFAULT 0",
            "edge REAL DEFAULT 0",
            "reject_code TEXT",
            "real_probe_allowed INTEGER DEFAULT 0",
            "real_probe_reason TEXT",
            "teacher_as_of_issue TEXT",
            "teacher_issue_lag INTEGER DEFAULT 0",
            "teacher_fresh_by_issue INTEGER DEFAULT 0",
            "teacher_snapshot_created_at INTEGER DEFAULT 0",
            "block_tier TEXT",
            "hard_risk_code TEXT",
            "observe_reason TEXT",
            "probe_eligible INTEGER DEFAULT 0",
            "shadow_probe_eligible INTEGER DEFAULT 0",
            "probe_level TEXT",
            "shadow_settled_count INTEGER DEFAULT 0",
            "shadow_domain_sample INTEGER DEFAULT 0",
            "promotion_wilson_lower_bound REAL DEFAULT 0",
            "promotion_eligibility_reason TEXT",
            "predecision_opportunity INTEGER DEFAULT 0",
            "risk_severity TEXT",
            "hard_block_flag INTEGER DEFAULT 0",
            "hard_block_source TEXT",
            "evidence_source TEXT",
            "evidence_settled_only INTEGER DEFAULT 1",
            "evidence_sample_count INTEGER DEFAULT 0",
            "evidence_hit_count INTEGER DEFAULT 0",
            "evidence_miss_count INTEGER DEFAULT 0",
            "promotion_coarse_domain TEXT",
            "promotion_mid_domain TEXT",
            "promotion_fine_domain TEXT",
            "frequency_floor_required_count INTEGER DEFAULT 0",
            "frequency_floor_target_count INTEGER DEFAULT 0",
            "frequency_floor_actual_count INTEGER DEFAULT 0",
            "frequency_floor_debt_count INTEGER DEFAULT 0",
            "frequency_target_debt_count INTEGER DEFAULT 0",
            "frequency_floor_mandatory INTEGER DEFAULT 0",
            "frequency_floor_no_upper_cap INTEGER DEFAULT 1",
            "frequency_floor_rank_score REAL DEFAULT 0",
            "frequency_floor_version TEXT",
            "bet_source_engine TEXT",
            "decision_gate_version TEXT",
            "field_chain_version TEXT",
            "schema_fix_version TEXT"
        };
    }

    private void ensureMainChainFactorColumns(SQLiteDatabase db) {
        String[] tables =
                new String[] {
                    DbHelper.TABLE_CAUSAL_FACTOR_MATRIX, DbHelper.TABLE_POSITION_CAUSE_MATRIX,
                            DbHelper.TABLE_TEACHER_NUMBER_MATRIX,
                    DbHelper.TABLE_COUNTERFACTUAL_RANK, DbHelper.TABLE_THIRD_GUARD_MATRIX,
                            DbHelper.TABLE_RULE_HEALTH_MATRIX,
                    DbHelper.TABLE_SOURCE_HEALTH_MATRIX, DbHelper.TABLE_STATE_HEALTH_MATRIX,
                            DbHelper.TABLE_SHADOW_HEALTH_GATE,
                    DbHelper.TABLE_DYNAMIC_WINDOW_CLUSTER, DbHelper.TABLE_WINDOW_FEATURE_SAMPLE,
                            DbHelper.TABLE_POSITION_NEED_SAMPLE,
                    DbHelper.TABLE_NUMBER_SUPPLY_SAMPLE, DbHelper.TABLE_NEED_SUPPLY_MATCH_SAMPLE,
                            DbHelper.TABLE_COUNTERFACTUAL_REASON_SAMPLE,
                    DbHelper.TABLE_PROMOTION_DOMAIN_SNAPSHOT
                };
        for (int i = 0; i < tables.length; i++) {
            addColumnArray(db, tables[i], mainChainFactorColumnDefs());
        }
    }

    private String[] mainChainFactorColumnDefs() {
        return new String[] {
            "state_key TEXT",
            "window_state_key TEXT",
            "cluster_key TEXT",
            "factor_key TEXT",
            "factor_id TEXT",
            "factor_name TEXT",
            "sample_count INTEGER DEFAULT 0",
            "recent_sample_count INTEGER DEFAULT 0",
            "hit_count INTEGER DEFAULT 0",
            "miss_count INTEGER DEFAULT 0",
            "hit_rate REAL DEFAULT 0",
            "recent_hit_rate REAL DEFAULT 0",
            "evidence_score REAL DEFAULT 0",
            "anti_evidence_score REAL DEFAULT 0",
            "net_evidence_score REAL DEFAULT 0",
            "sample_reliability REAL DEFAULT 0",
            "recent_validity REAL DEFAULT 0",
            "action_advice TEXT",
            "reject_code TEXT",
            "schema_fix_version TEXT"
        };
    }

    private void ensureMainChainRejectBucketColumns(SQLiteDatabase db) {
        addColumnIfMissing(db, TABLE_MAIN_CHAIN_REJECT_BUCKET, "reject_code", "TEXT");
        addColumnIfMissing(db, TABLE_MAIN_CHAIN_REJECT_BUCKET, "sample_count", "INTEGER DEFAULT 0");
        addColumnIfMissing(
                db, TABLE_MAIN_CHAIN_REJECT_BUCKET, "candidate_count", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, TABLE_MAIN_CHAIN_REJECT_BUCKET, "bet_count", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, TABLE_MAIN_CHAIN_REJECT_BUCKET, "shadow_count", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, TABLE_MAIN_CHAIN_REJECT_BUCKET, "hold_count", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, TABLE_MAIN_CHAIN_REJECT_BUCKET, "hit_count", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, TABLE_MAIN_CHAIN_REJECT_BUCKET, "miss_count", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, TABLE_MAIN_CHAIN_REJECT_BUCKET, "avg_final_p", "REAL DEFAULT 0");
        addColumnIfMissing(db, TABLE_MAIN_CHAIN_REJECT_BUCKET, "avg_gate", "REAL DEFAULT 0");
        addColumnIfMissing(db, TABLE_MAIN_CHAIN_REJECT_BUCKET, "avg_edge", "REAL DEFAULT 0");
        addColumnIfMissing(db, TABLE_MAIN_CHAIN_REJECT_BUCKET, "updated_at", "INTEGER DEFAULT 0");
    }

    private void ensureMainChainIndexes(SQLiteDatabase db) {
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_dbstore_mccs_target_rank ON "
                        + TABLE_MAIN_CHAIN_CANDIDATE_SNAPSHOT
                        + "(target_issue,realtime_top40_rank)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_dbstore_mccs_adaptive ON "
                        + TABLE_MAIN_CHAIN_CANDIDATE_SNAPSHOT
                        + "(target_issue,adaptive_input_flag)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_dbstore_mccs_reject ON "
                        + TABLE_MAIN_CHAIN_CANDIDATE_SNAPSHOT
                        + "(target_issue,reject_code)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_dbstore_mcrb_code ON "
                        + TABLE_MAIN_CHAIN_REJECT_BUCKET
                        + "(reject_code)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_dbstore_decisions_main_chain ON "
                        + DbHelper.TABLE_DECISIONS
                        + "(target_issue,decision,reject_code,real_probe_allowed)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_dbstore_trades_main_chain ON "
                        + DbHelper.TABLE_TRADES
                        + "(target_issue,action,reject_code,real_probe_allowed)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_dbstore_shadow_main_chain ON "
                        + DbHelper.TABLE_SHADOW
                        + "(target_issue,rank_no,reject_code)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_dbstore_scan_main_chain ON "
                        + DbHelper.TABLE_FULL_CANDIDATE_SCAN
                        + "(target_issue,realtime_top40_rank,adaptive_input_flag)");
    }

    private void ensureMainChainDefaultState(SQLiteDatabase db) {
        long now = System.currentTimeMillis();
        putDynamicCausalState(
                db, "dbstore_main_chain_version", DBSTORE_MAIN_CHAIN_CLOSED_LOOP_VERSION, now);
        putDynamicCausalState(
                db,
                "dbstore_main_chain_candidate_snapshot",
                TABLE_MAIN_CHAIN_CANDIDATE_SNAPSHOT,
                now);
        putDynamicCausalState(
                db, "dbstore_main_chain_reject_bucket", TABLE_MAIN_CHAIN_REJECT_BUCKET, now);
        putDynamicCausalState(db, "dbstore_main_chain_no_bet_rescue", "1", now);
        putDynamicCausalState(db, "dbstore_main_chain_scope", "SAVE_READ_REPAIR_ONLY", now);
    }

    private void execQuiet(SQLiteDatabase db, String sql) {
        if (db == null || sql == null || sql.length() == 0) {
            return;
        }
        try {
            db.execSQL(sql);
        } catch (Throwable e) {
        }
    }

    private void ensureDynamicCausalTables(SQLiteDatabase db) {
        createDynamicTable(db, TABLE_DYNAMIC_WINDOW_CLUSTER, "cluster_id");
        createDynamicTable(db, TABLE_WINDOW_FEATURE_SAMPLE, "sample_key");
        createDynamicTable(db, TABLE_POSITION_NEED_SAMPLE, "sample_key");
        createDynamicTable(db, TABLE_NUMBER_SUPPLY_SAMPLE, "sample_key");
        createDynamicTable(db, TABLE_NEED_SUPPLY_MATCH_SAMPLE, "match_key");
        createDynamicTable(db, TABLE_COUNTERFACTUAL_REASON_SAMPLE, "reason_key");
        createDynamicTable(db, TABLE_CAUSE_ABLATION_SAMPLE, "ablation_key");
        createDynamicTable(db, TABLE_MISS_REASON_SAMPLE, "miss_key");
        createDynamicTable(db, TABLE_ADAPTIVE_JUDGE_SAMPLE, "judge_key");
        createDynamicTable(db, TABLE_PROMOTION_DOMAIN_SNAPSHOT, "domain_key");
        execQuiet(
                db,
                "CREATE TABLE IF NOT EXISTS "
                        + TABLE_DYNAMIC_CAUSAL_STATE
                        + " (state_key TEXT PRIMARY KEY)");
        ensureDynamicClusterColumns(db);
        ensureDynamicSampleColumns(db);
    }

    private void createDynamicTable(SQLiteDatabase db, String table, String key) {
        execQuiet(
                db,
                "CREATE TABLE IF NOT EXISTS "
                        + table
                        + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + key
                        + " TEXT NOT NULL UNIQUE)");
    }

    private void ensureDynamicClusterColumns(SQLiteDatabase db) {
        String t = TABLE_DYNAMIC_WINDOW_CLUSTER;
        addColumnIfMissing(db, t, "prototype_key", "TEXT");
        addColumnIfMissing(db, t, "prototype_vector", "TEXT");
        addColumnIfMissing(db, t, "status", "TEXT");
        addColumnIfMissing(db, t, "sample_count", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, t, "hit_count", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, t, "miss_count", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, t, "recent_sample", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, t, "recent_hit_count", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, t, "hit_rate", "REAL DEFAULT 0");
        addColumnIfMissing(db, t, "recent_hit_rate", "REAL DEFAULT 0");
        addColumnIfMissing(db, t, "max_lose_streak", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, t, "confidence", "REAL DEFAULT 0");
        addColumnIfMissing(db, t, "volatility", "REAL DEFAULT 0");
        addColumnIfMissing(db, t, "counter_score", "REAL DEFAULT 0");
        addColumnIfMissing(db, t, "prototype_distance", "REAL DEFAULT 0");
        addColumnIfMissing(db, t, "last_target_issue", "TEXT");
        addColumnIfMissing(db, t, "version", "TEXT");
        addColumnIfMissing(db, t, "reason", "TEXT");
        addColumnIfMissing(db, t, "created_at", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, t, "updated_at", "INTEGER DEFAULT 0");
        t = TABLE_PROMOTION_DOMAIN_SNAPSHOT;
        addColumnIfMissing(db, t, "cluster_id", "TEXT");
        addColumnIfMissing(db, t, "status", "TEXT");
        addColumnIfMissing(db, t, "sample_count", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, t, "hit_count", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, t, "recent_sample", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, t, "recent_hit_count", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, t, "hit_rate", "REAL DEFAULT 0");
        addColumnIfMissing(db, t, "recent_hit_rate", "REAL DEFAULT 0");
        addColumnIfMissing(db, t, "max_lose_streak", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, t, "confidence", "REAL DEFAULT 0");
        addColumnIfMissing(db, t, "action", "TEXT");
        addColumnIfMissing(db, t, "reason", "TEXT");
        addColumnIfMissing(db, t, "updated_at", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, TABLE_DYNAMIC_CAUSAL_STATE, "state_value", "TEXT");
        addColumnIfMissing(db, TABLE_DYNAMIC_CAUSAL_STATE, "updated_at", "INTEGER DEFAULT 0");
    }

    private void ensureDynamicSampleColumns(SQLiteDatabase db) {
        String t = TABLE_WINDOW_FEATURE_SAMPLE;
        String[] cols1 =
                new String[] {
                    "base_issue TEXT",
                    "target_issue TEXT",
                    "created_at INTEGER DEFAULT 0",
                    "position INTEGER DEFAULT 0",
                    "n1 INTEGER DEFAULT 0",
                    "n2 INTEGER DEFAULT 0",
                    "n3 INTEGER DEFAULT 0",
                    "k3_mask INTEGER DEFAULT 0",
                    "vector_key TEXT",
                    "raw_key TEXT",
                    "cluster_id TEXT",
                    "cluster_status TEXT",
                    "distance REAL DEFAULT 0",
                    "would_hit INTEGER DEFAULT 0",
                    "decision_type TEXT",
                    "trend_slope12 REAL DEFAULT 0",
                    "trend_slope24 REAL DEFAULT 0",
                    "trend_slope48 REAL DEFAULT 0",
                    "trend_slope96 REAL DEFAULT 0",
                    "trend_slope360 REAL DEFAULT 0",
                    "trend_life REAL DEFAULT 0",
                    "trend_fatigue REAL DEFAULT 0",
                    "omission_pressure REAL DEFAULT 0",
                    "hot_cold_transition REAL DEFAULT 0",
                    "density_score REAL DEFAULT 0",
                    "repeat_rhythm REAL DEFAULT 0",
                    "odd_even_bias REAL DEFAULT 0",
                    "high_low_bias REAL DEFAULT 0",
                    "tail_pressure REAL DEFAULT 0",
                    "neighbor_pressure REAL DEFAULT 0",
                    "cross_position_flow REAL DEFAULT 0",
                    "cross_position_crowd REAL DEFAULT 0",
                    "same_number_crowd REAL DEFAULT 0",
                    "timing_offset REAL DEFAULT 0",
                    "counter_evidence REAL DEFAULT 0",
                    "third_quality REAL DEFAULT 0",
                    "summary TEXT"
                };
        addColumnArray(db, t, cols1);

        t = TABLE_POSITION_NEED_SAMPLE;
        String[] cols2 =
                new String[] {
                    "target_issue TEXT",
                    "created_at INTEGER DEFAULT 0",
                    "position INTEGER DEFAULT 0",
                    "actual_number INTEGER DEFAULT 0",
                    "need_key TEXT",
                    "need_hot_continue REAL DEFAULT 0",
                    "need_cold_repair REAL DEFAULT 0",
                    "need_omission_return REAL DEFAULT 0",
                    "need_high_low_balance REAL DEFAULT 0",
                    "need_odd_even_balance REAL DEFAULT 0",
                    "need_tail_repair REAL DEFAULT 0",
                    "need_neighbor_relay REAL DEFAULT 0",
                    "need_cross_position_flow REAL DEFAULT 0",
                    "need_crowd_release REAL DEFAULT 0",
                    "need_reverse_break REAL DEFAULT 0",
                    "need_stable_repeat REAL DEFAULT 0",
                    "need_chaos_avoid REAL DEFAULT 0",
                    "total_need_strength REAL DEFAULT 0",
                    "chaos_risk REAL DEFAULT 0",
                    "main_need_type TEXT",
                    "summary TEXT"
                };
        addColumnArray(db, t, cols2);

        t = TABLE_NUMBER_SUPPLY_SAMPLE;
        String[] cols3 =
                new String[] {
                    "target_issue TEXT",
                    "created_at INTEGER DEFAULT 0",
                    "position INTEGER DEFAULT 0",
                    "number INTEGER DEFAULT 0",
                    "actual_number INTEGER DEFAULT 0",
                    "label INTEGER DEFAULT 0",
                    "supply_key TEXT",
                    "supply_hot_continue REAL DEFAULT 0",
                    "supply_cold_repair REAL DEFAULT 0",
                    "supply_omission_return REAL DEFAULT 0",
                    "supply_high_low_balance REAL DEFAULT 0",
                    "supply_odd_even_balance REAL DEFAULT 0",
                    "supply_tail_repair REAL DEFAULT 0",
                    "supply_neighbor_relay REAL DEFAULT 0",
                    "supply_cross_position_flow REAL DEFAULT 0",
                    "supply_crowd_release REAL DEFAULT 0",
                    "supply_reverse_break REAL DEFAULT 0",
                    "supply_stable_repeat REAL DEFAULT 0",
                    "supply_risk REAL DEFAULT 0",
                    "total_supply_strength REAL DEFAULT 0",
                    "main_supply_type TEXT",
                    "summary TEXT"
                };
        addColumnArray(db, t, cols3);

        t = TABLE_NEED_SUPPLY_MATCH_SAMPLE;
        String[] cols4 =
                new String[] {
                    "target_issue TEXT",
                    "created_at INTEGER DEFAULT 0",
                    "position INTEGER DEFAULT 0",
                    "n1 INTEGER DEFAULT 0",
                    "n2 INTEGER DEFAULT 0",
                    "n3 INTEGER DEFAULT 0",
                    "k3_mask INTEGER DEFAULT 0",
                    "main_role_number INTEGER DEFAULT 0",
                    "assist_role_number INTEGER DEFAULT 0",
                    "defense_role_number INTEGER DEFAULT 0",
                    "main_role TEXT",
                    "assist_role TEXT",
                    "defense_role TEXT",
                    "match_score REAL DEFAULT 0",
                    "main_role_score REAL DEFAULT 0",
                    "assist_role_score REAL DEFAULT 0",
                    "defense_role_score REAL DEFAULT 0",
                    "third_quality REAL DEFAULT 0",
                    "role_diversity REAL DEFAULT 0",
                    "supply_risk REAL DEFAULT 0",
                    "why_match TEXT",
                    "why_not_match TEXT",
                    "summary TEXT"
                };
        addColumnArray(db, t, cols4);

        t = TABLE_COUNTERFACTUAL_REASON_SAMPLE;
        String[] cols5 =
                new String[] {
                    "target_issue TEXT",
                    "created_at INTEGER DEFAULT 0",
                    "position INTEGER DEFAULT 0",
                    "n1 INTEGER DEFAULT 0",
                    "n2 INTEGER DEFAULT 0",
                    "n3 INTEGER DEFAULT 0",
                    "k3_mask INTEGER DEFAULT 0",
                    "cluster_id TEXT",
                    "position_rank INTEGER DEFAULT 0",
                    "k3_rank INTEGER DEFAULT 0",
                    "rank_score REAL DEFAULT 0",
                    "why_not_risk REAL DEFAULT 0",
                    "mirror_miss_similarity REAL DEFAULT 0",
                    "cause_score REAL DEFAULT 0",
                    "counter_score REAL DEFAULT 0",
                    "would_hit INTEGER DEFAULT 0",
                    "why_this_position TEXT",
                    "why_these_numbers TEXT",
                    "why_not_other_position TEXT",
                    "why_not_other_numbers TEXT",
                    "summary TEXT"
                };
        addColumnArray(db, t, cols5);

        t = TABLE_CAUSE_ABLATION_SAMPLE;
        String[] cols6 =
                new String[] {
                    "cluster_id TEXT",
                    "created_at INTEGER DEFAULT 0",
                    "sample_count INTEGER DEFAULT 0",
                    "omission_drop REAL DEFAULT 0",
                    "tail_drop REAL DEFAULT 0",
                    "neighbor_drop REAL DEFAULT 0",
                    "cross_position_drop REAL DEFAULT 0",
                    "need_supply_drop REAL DEFAULT 0",
                    "third_quality_drop REAL DEFAULT 0",
                    "total_ablation_score REAL DEFAULT 0",
                    "false_cause_count INTEGER DEFAULT 0",
                    "false_cause_summary TEXT",
                    "summary TEXT"
                };
        addColumnArray(db, t, cols6);

        t = TABLE_MISS_REASON_SAMPLE;
        String[] cols7 =
                new String[] {
                    "target_issue TEXT",
                    "created_at INTEGER DEFAULT 0",
                    "position INTEGER DEFAULT 0",
                    "n1 INTEGER DEFAULT 0",
                    "n2 INTEGER DEFAULT 0",
                    "n3 INTEGER DEFAULT 0",
                    "k3_mask INTEGER DEFAULT 0",
                    "actual_number INTEGER DEFAULT 0",
                    "miss_type TEXT",
                    "cluster_id TEXT",
                    "window_wrong_score REAL DEFAULT 0",
                    "position_wrong_score REAL DEFAULT 0",
                    "main_number_wrong_score REAL DEFAULT 0",
                    "third_drag_score REAL DEFAULT 0",
                    "counter_ignored_score REAL DEFAULT 0",
                    "detail TEXT",
                    "next_advice TEXT"
                };
        addColumnArray(db, t, cols7);

        t = TABLE_ADAPTIVE_JUDGE_SAMPLE;
        String[] cols8 =
                new String[] {
                    "target_issue TEXT",
                    "created_at INTEGER DEFAULT 0",
                    "candidate_count INTEGER DEFAULT 0",
                    "cluster_id TEXT",
                    "cluster_status TEXT",
                    "status TEXT",
                    "action TEXT",
                    "domain TEXT",
                    "cluster_hit_rate REAL DEFAULT 0",
                    "cluster_recent_hit_rate REAL DEFAULT 0",
                    "cluster_sample INTEGER DEFAULT 0",
                    "cluster_confidence REAL DEFAULT 0",
                    "cause_score REAL DEFAULT 0",
                    "counter_score REAL DEFAULT 0",
                    "promote_score REAL DEFAULT 0",
                    "freeze_score REAL DEFAULT 0",
                    "reason TEXT",
                    "version TEXT"
                };
        addColumnArray(db, t, cols8);
    }

    private void addColumnArray(SQLiteDatabase db, String table, String[] defs) {
        if (defs == null) return;
        for (int i = 0; i < defs.length; i++) {
            String d = defs[i];
            int p = d.indexOf(' ');
            if (p <= 0) continue;
            addColumnIfMissing(db, table, d.substring(0, p), d.substring(p + 1));
        }
    }

    private void ensureDynamicCausalDecisionLikeColumns(SQLiteDatabase db, String table) {
        if (db == null || table == null || table.length() == 0) return;
        String[] defs = dynamicCausalDecisionColumnDefs();
        addColumnArray(db, table, defs);
    }

    private String[] dynamicCausalDecisionColumnDefs() {
        return new String[] {
            "dynamic_cluster_id TEXT",
            "dynamic_cluster_status TEXT",
            "dynamic_cluster_prototype_key TEXT",
            "dynamic_cluster_feature_key TEXT",
            "dynamic_cluster_sample INTEGER DEFAULT 0",
            "dynamic_cluster_hit_count INTEGER DEFAULT 0",
            "dynamic_cluster_miss_count INTEGER DEFAULT 0",
            "dynamic_cluster_recent_sample INTEGER DEFAULT 0",
            "dynamic_cluster_recent_hit_count INTEGER DEFAULT 0",
            "dynamic_cluster_hit_rate REAL DEFAULT 0",
            "dynamic_cluster_recent_hit_rate REAL DEFAULT 0",
            "dynamic_cluster_confidence REAL DEFAULT 0",
            "dynamic_cluster_distance REAL DEFAULT 0",
            "dynamic_cluster_volatility REAL DEFAULT 0",
            "dynamic_cluster_max_lose INTEGER DEFAULT 0",
            "dynamic_cluster_counter_score REAL DEFAULT 0",
            "dynamic_cluster_version TEXT",
            "dynamic_cluster_reason TEXT",
            "position_need_key TEXT",
            "position_need_summary TEXT",
            "position_need_strength REAL DEFAULT 0",
            "position_need_chaos_risk REAL DEFAULT 0",
            "position_need_repair_score REAL DEFAULT 0",
            "position_need_relay_score REAL DEFAULT 0",
            "number_supply_key TEXT",
            "number_supply_summary TEXT",
            "number_supply_strength REAL DEFAULT 0",
            "number_supply_risk REAL DEFAULT 0",
            "need_supply_match_key TEXT",
            "need_supply_match_score REAL DEFAULT 0",
            "need_supply_third_quality REAL DEFAULT 0",
            "need_supply_role_diversity REAL DEFAULT 0",
            "need_supply_risk REAL DEFAULT 0",
            "need_supply_summary TEXT",
            "k3_role_main TEXT",
            "k3_role_assist TEXT",
            "k3_role_defense TEXT",
            "k3_role_main_number INTEGER DEFAULT 0",
            "k3_role_assist_number INTEGER DEFAULT 0",
            "k3_role_defense_number INTEGER DEFAULT 0",
            "k3_role_structure TEXT",
            "counterfactual_reason_key TEXT",
            "counterfactual_rank INTEGER DEFAULT 0",
            "counterfactual_rank_score REAL DEFAULT 0",
            "counterfactual_why_not_risk REAL DEFAULT 0",
            "counterfactual_why_not TEXT",
            "counterfactual_summary TEXT",
            "cause_ablation_key TEXT",
            "cause_ablation_score REAL DEFAULT 0",
            "cause_ablation_false_cause_count INTEGER DEFAULT 0",
            "cause_ablation_summary TEXT",
            "miss_reason_type TEXT",
            "miss_reason_matrix_key TEXT",
            "miss_reason_detail TEXT",
            "adaptive_judge_candidate_count INTEGER DEFAULT 0",
            "adaptive_judge_status TEXT",
            "adaptive_judge_action TEXT",
            "adaptive_judge_domain TEXT",
            "adaptive_cluster_hit_rate REAL DEFAULT 0",
            "adaptive_cluster_recent_hit_rate REAL DEFAULT 0",
            "adaptive_cluster_sample INTEGER DEFAULT 0",
            "adaptive_cluster_confidence REAL DEFAULT 0",
            "adaptive_cause_score REAL DEFAULT 0",
            "adaptive_counter_score REAL DEFAULT 0",
            "adaptive_promote_score REAL DEFAULT 0",
            "adaptive_freeze_score REAL DEFAULT 0",
            "adaptive_judge_reason TEXT",
            "adaptive_judge_version TEXT",
            "promotion_cluster_status TEXT",
            "promotion_cluster_hit_rate REAL DEFAULT 0",
            "promotion_cluster_recent_hit_rate REAL DEFAULT 0",
            "promotion_cluster_sample INTEGER DEFAULT 0",
            "promotion_cluster_reason TEXT",
            "why_bet TEXT",
            "why_no_bet TEXT",
            "why_hit TEXT",
            "why_miss TEXT",
            "dynamic_causal_summary TEXT"
        };
    }

    private void ensureDynamicCausalIndexes(SQLiteDatabase db) {
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_dbstore_dcw_status ON "
                        + TABLE_DYNAMIC_WINDOW_CLUSTER
                        + "(status)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_dbstore_dcw_rate ON "
                        + TABLE_DYNAMIC_WINDOW_CLUSTER
                        + "(hit_rate,recent_hit_rate)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_dbstore_dcw_updated ON "
                        + TABLE_DYNAMIC_WINDOW_CLUSTER
                        + "(updated_at)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_dbstore_wfs_target ON "
                        + TABLE_WINDOW_FEATURE_SAMPLE
                        + "(target_issue)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_dbstore_wfs_cluster ON "
                        + TABLE_WINDOW_FEATURE_SAMPLE
                        + "(cluster_id)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_dbstore_wfs_pos_k ON "
                        + TABLE_WINDOW_FEATURE_SAMPLE
                        + "(position,k3_mask)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_dbstore_nss_target ON "
                        + TABLE_NUMBER_SUPPLY_SAMPLE
                        + "(target_issue,position,number)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_dbstore_nsms_target ON "
                        + TABLE_NEED_SUPPLY_MATCH_SAMPLE
                        + "(target_issue,position,k3_mask)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_dbstore_cfr_target ON "
                        + TABLE_COUNTERFACTUAL_REASON_SAMPLE
                        + "(target_issue,cluster_id)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_dbstore_miss_cluster ON "
                        + TABLE_MISS_REASON_SAMPLE
                        + "(cluster_id,miss_type)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_dbstore_adaptive_target ON "
                        + TABLE_ADAPTIVE_JUDGE_SAMPLE
                        + "(target_issue,action)");
        execQuiet(
                db,
                "CREATE INDEX IF NOT EXISTS idx_dbstore_promo_cluster ON "
                        + TABLE_PROMOTION_DOMAIN_SNAPSHOT
                        + "(cluster_id,status)");
    }

    private void ensureDynamicCausalDefaultState(SQLiteDatabase db) {
        long now = System.currentTimeMillis();
        putDynamicCausalState(
                db,
                AppConfig.STATE_DYNAMIC_CLUSTER_SNAPSHOT_VERSION,
                V20_DYNAMIC_CAUSAL_DBSTORE_VERSION,
                now);
        putDynamicCausalState(db, AppConfig.STATE_DYNAMIC_CLUSTER_LAST_RUN, "0", now);
        putDynamicCausalState(db, AppConfig.STATE_DYNAMIC_CLUSTER_SUMMARY, "", now);
        putDynamicCausalState(db, AppConfig.STATE_NEED_SUPPLY_LAST_RUN, "0", now);
        putDynamicCausalState(db, AppConfig.STATE_COUNTERFACTUAL_LAST_RUN, "0", now);
        putDynamicCausalState(db, AppConfig.STATE_ADAPTIVE_JUDGE_CANDIDATE_COUNT, "0", now);
        putDynamicCausalState(
                db, AppConfig.STATE_ADAPTIVE_JUDGE_REALTIME_SUMMARY, "候选=0｜等待AutoFetch接入", now);
        putDynamicCausalState(
                db, AppConfig.STATE_DYNAMIC_CAUSAL_DIAGNOSIS, "READY｜DbStore动态因果存储已就绪", now);
        putDynamicCausalState(db, "dynamic_causal_real_bet_entry", "DecisionGateEngine_ONLY", now);
        putDynamicCausalState(db, "dynamic_causal_storage_mode", "FIELD_BRIDGE_FULL_SAVE", now);
        putDynamicCausalState(db, "dynamic_causal_shadow_full_fields", "1", now);
        putDynamicCausalState(
                db, "dbstore_main_chain_version", DBSTORE_MAIN_CHAIN_CLOSED_LOOP_VERSION, now);
    }

    private void putDynamicCausalState(SQLiteDatabase db, String key, String value, long now) {
        if (db == null || key == null || key.length() == 0) return;
        try {
            ContentValues cv = new ContentValues();
            cv.put("state_key", key);
            cv.put("state_value", value == null ? "" : value);
            cv.put("updated_at", now);
            insertFiltered(db, TABLE_DYNAMIC_CAUSAL_STATE, cv, SQLiteDatabase.CONFLICT_REPLACE);
            ContentValues app = new ContentValues();
            app.put("state_key", key);
            app.put("state_value", value == null ? "" : value);
            app.put("updated_at", now);
            insertFiltered(db, DbHelper.TABLE_APP_STATE, app, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Throwable e) {
        }
    }

    private void ensureLearningCaseLogTable(SQLiteDatabase db) {
        if (db == null) {
            return;
        }

        try {
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS "
                            + TABLE_LEARNING_CASE_LOG
                            + " ("
                            + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "case_key TEXT UNIQUE,"
                            + "task_key TEXT,"
                            + "segment_key TEXT,"
                            + "base_issue TEXT,"
                            + "target_issue TEXT,"
                            + "created_at INTEGER DEFAULT 0,"
                            + "target_index INTEGER DEFAULT -1,"
                            + "position INTEGER DEFAULT 0,"
                            + "actual_number INTEGER DEFAULT 0,"
                            + "predicted_k3 INTEGER DEFAULT 0,"
                            + "k3_mask INTEGER DEFAULT 0,"
                            + "hit INTEGER DEFAULT 0,"
                            + "state_key TEXT,"
                            + "state_mode TEXT,"
                            + "trend_key TEXT,"
                            + "shape_key TEXT,"
                            + "drift_key TEXT,"
                            + "break_key TEXT,"
                            + "recovery_key TEXT,"
                            + "transition_key TEXT,"
                            + "expected_shape TEXT,"
                            + "actual_shape TEXT,"
                            + "why_expected_hit TEXT,"
                            + "why_actual_hit TEXT,"
                            + "why_miss TEXT,"
                            + "next_advice TEXT,"
                            + "main_reason_code TEXT,"
                            + "affected_module TEXT,"
                            + "final_p REAL DEFAULT 0,"
                            + "dynamic_gate REAL DEFAULT 0,"
                            + "edge REAL DEFAULT 0,"
                            + "trend_score REAL DEFAULT 0,"
                            + "shape_score REAL DEFAULT 0,"
                            + "drift_risk REAL DEFAULT 0,"
                            + "break_risk REAL DEFAULT 0,"
                            + "recovery_score REAL DEFAULT 0,"
                            + "profile_score REAL DEFAULT 0,"
                            + "number_bias REAL DEFAULT 0,"
                            + "combo_bias REAL DEFAULT 0,"
                            + "risk_score REAL DEFAULT 0,"
                            + "teacher_weight REAL DEFAULT 1,"
                            + "state_confidence REAL DEFAULT 0,"
                            + "state_sample INTEGER DEFAULT 0,"
                            + "cache_sample INTEGER DEFAULT 0,"
                            + "recent_sample INTEGER DEFAULT 0,"
                            + "recent_hit_rate REAL DEFAULT 0,"
                            + "saved INTEGER DEFAULT 0"
                            + ")");

            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_learning_case_target ON "
                            + TABLE_LEARNING_CASE_LOG
                            + "(target_issue,position)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_learning_case_state ON "
                            + TABLE_LEARNING_CASE_LOG
                            + "(state_key,position)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_learning_case_task ON "
                            + TABLE_LEARNING_CASE_LOG
                            + "(task_key,segment_key)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_learning_case_created ON "
                            + TABLE_LEARNING_CASE_LOG
                            + "(created_at)");
        } catch (Throwable e) {
        }
    }

    private void ensureRuntimeProfileCacheTable(SQLiteDatabase db) {
        if (db == null) {
            return;
        }

        try {
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS "
                            + TABLE_RUNTIME_PROFILE_CACHE
                            + " ("
                            + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "profile_key TEXT UNIQUE,"
                            + "state_key TEXT,"
                            + "position INTEGER DEFAULT 0,"
                            + "k3_mask INTEGER DEFAULT 0,"
                            + "sample_count INTEGER DEFAULT 0,"
                            + "hit_count INTEGER DEFAULT 0,"
                            + "hit_rate REAL DEFAULT 0,"
                            + "recent_sample INTEGER DEFAULT 0,"
                            + "recent_hit INTEGER DEFAULT 0,"
                            + "recent_hit_rate REAL DEFAULT 0,"
                            + "avg_final_p REAL DEFAULT 0,"
                            + "avg_gate REAL DEFAULT 0,"
                            + "risk_score REAL DEFAULT 0,"
                            + "teacher_weight REAL DEFAULT 1,"
                            + "profile_score REAL DEFAULT 0,"
                            + "state_confidence REAL DEFAULT 0,"
                            + "updated_at INTEGER DEFAULT 0,"
                            + "summary_text TEXT"
                            + ")");

            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_runtime_profile_state ON "
                            + TABLE_RUNTIME_PROFILE_CACHE
                            + "(state_key,position)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_runtime_profile_score ON "
                            + TABLE_RUNTIME_PROFILE_CACHE
                            + "(profile_score DESC,state_confidence DESC)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_runtime_profile_updated ON "
                            + TABLE_RUNTIME_PROFILE_CACHE
                            + "(updated_at)");
        } catch (Throwable e) {
        }
    }

    private void ensureStateNumberProfileTable(SQLiteDatabase db) {
        if (db == null) {
            return;
        }

        try {
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS "
                            + TABLE_STATE_NUMBER_PROFILE
                            + " ("
                            + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "profile_key TEXT UNIQUE,"
                            + "state_key TEXT,"
                            + "position INTEGER DEFAULT 0,"
                            + "number INTEGER DEFAULT 0,"
                            + "sample_count INTEGER DEFAULT 0,"
                            + "hit_count INTEGER DEFAULT 0,"
                            + "hit_rate REAL DEFAULT 0,"
                            + "recent_sample INTEGER DEFAULT 0,"
                            + "recent_hit INTEGER DEFAULT 0,"
                            + "recent_hit_rate REAL DEFAULT 0,"
                            + "trend_score REAL DEFAULT 0,"
                            + "shape_score REAL DEFAULT 0,"
                            + "risk_score REAL DEFAULT 0,"
                            + "number_bias REAL DEFAULT 0,"
                            + "confidence REAL DEFAULT 0,"
                            + "why_text TEXT,"
                            + "updated_at INTEGER DEFAULT 0"
                            + ")");

            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_state_number_state ON "
                            + TABLE_STATE_NUMBER_PROFILE
                            + "(state_key,position)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_state_number_number ON "
                            + TABLE_STATE_NUMBER_PROFILE
                            + "(position,number)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_state_number_bias ON "
                            + TABLE_STATE_NUMBER_PROFILE
                            + "(number_bias DESC,confidence DESC)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_state_number_updated ON "
                            + TABLE_STATE_NUMBER_PROFILE
                            + "(updated_at)");
        } catch (Throwable e) {
        }
    }

    private void ensureAutoLearnCursorTable(SQLiteDatabase db) {
        if (db == null) {
            return;
        }

        try {
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS "
                            + TABLE_AUTO_LEARN_CURSOR
                            + " ("
                            + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "task_key TEXT UNIQUE,"
                            + "task_type TEXT,"
                            + "window_size INTEGER DEFAULT 0,"
                            + "segment_start_index INTEGER DEFAULT 0,"
                            + "segment_end_index INTEGER DEFAULT 0,"
                            + "next_target_index INTEGER DEFAULT 0,"
                            + "last_start_index INTEGER DEFAULT 0,"
                            + "random_seed INTEGER DEFAULT 0,"
                            + "last_issue TEXT,"
                            + "progress TEXT,"
                            + "status TEXT,"
                            + "used_segments TEXT,"
                            + "updated_at INTEGER DEFAULT 0"
                            + ")");

            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_auto_learn_status ON "
                            + TABLE_AUTO_LEARN_CURSOR
                            + "(status,updated_at)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_auto_learn_task ON "
                            + TABLE_AUTO_LEARN_CURSOR
                            + "(task_type,window_size)");
        } catch (Throwable e) {
        }
    }

    private void ensureTeacherMatrixSampleTable(SQLiteDatabase db) {
        if (db == null) {
            return;
        }

        try {
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS "
                            + TABLE_TEACHER_MATRIX_SAMPLE
                            + " ("
                            + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "sample_key TEXT NOT NULL UNIQUE,"
                            + "source_type TEXT,"
                            + "base_issue TEXT,"
                            + "target_issue TEXT NOT NULL,"
                            + "created_at INTEGER DEFAULT 0,"
                            + "target_index INTEGER DEFAULT 0,"
                            + "position INTEGER DEFAULT 0,"
                            + "number INTEGER DEFAULT 0,"
                            + "actual_number INTEGER DEFAULT 0,"
                            + "label INTEGER DEFAULT 0,"
                            + "hit INTEGER DEFAULT 0,"
                            + "k3_mask INTEGER DEFAULT 0,"
                            + "state_key TEXT,"
                            + "state_mode TEXT,"
                            + "trend_key TEXT,"
                            + "shape_key TEXT,"
                            + "drift_key TEXT,"
                            + "break_key TEXT,"
                            + "recovery_key TEXT,"
                            + "transition_key TEXT,"
                            + "hot_cold_key TEXT,"
                            + "odd_even_key TEXT,"
                            + "high_low_key TEXT,"
                            + "relation_key TEXT,"
                            + "profile_key TEXT,"
                            + "combo_key TEXT,"
                            + "trend_score REAL DEFAULT 0,"
                            + "shape_score REAL DEFAULT 0,"
                            + "drift_risk REAL DEFAULT 0,"
                            + "break_risk REAL DEFAULT 0,"
                            + "recovery_score REAL DEFAULT 0,"
                            + "number_score REAL DEFAULT 0,"
                            + "position_score REAL DEFAULT 0,"
                            + "risk_score REAL DEFAULT 0,"
                            + "confidence REAL DEFAULT 0,"
                            + "final_p REAL DEFAULT 0,"
                            + "dynamic_gate REAL DEFAULT 0,"
                            + "edge REAL DEFAULT 0,"
                            + "why_positive TEXT,"
                            + "why_negative TEXT,"
                            + "why_miss TEXT,"
                            + "next_advice TEXT,"
                            + "main_reason_code TEXT,"
                            + "affected_module TEXT,"
                            + "saved INTEGER DEFAULT 0"
                            + ")");

            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_tm_sample_target ON "
                            + TABLE_TEACHER_MATRIX_SAMPLE
                            + "(target_issue)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_tm_sample_base ON "
                            + TABLE_TEACHER_MATRIX_SAMPLE
                            + "(base_issue)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_tm_sample_pos_num ON "
                            + TABLE_TEACHER_MATRIX_SAMPLE
                            + "(position,number)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_tm_sample_state ON "
                            + TABLE_TEACHER_MATRIX_SAMPLE
                            + "(state_key)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_tm_sample_profile ON "
                            + TABLE_TEACHER_MATRIX_SAMPLE
                            + "(profile_key)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_tm_sample_combo ON "
                            + TABLE_TEACHER_MATRIX_SAMPLE
                            + "(combo_key)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_tm_sample_label ON "
                            + TABLE_TEACHER_MATRIX_SAMPLE
                            + "(label)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_tm_sample_created ON "
                            + TABLE_TEACHER_MATRIX_SAMPLE
                            + "(created_at)");
        } catch (Throwable e) {
        }
    }

    private void ensureTeacherMatrixProfileTable(SQLiteDatabase db) {
        if (db == null) {
            return;
        }

        try {
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS "
                            + TABLE_TEACHER_MATRIX_PROFILE
                            + " ("
                            + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "profile_key TEXT NOT NULL UNIQUE,"
                            + "state_key TEXT,"
                            + "position INTEGER DEFAULT 0,"
                            + "number INTEGER DEFAULT 0,"
                            + "sample_count INTEGER DEFAULT 0,"
                            + "positive_count INTEGER DEFAULT 0,"
                            + "negative_count INTEGER DEFAULT 0,"
                            + "recent_sample INTEGER DEFAULT 0,"
                            + "recent_positive INTEGER DEFAULT 0,"
                            + "recent_negative INTEGER DEFAULT 0,"
                            + "hit_rate REAL DEFAULT 0,"
                            + "recent_hit_rate REAL DEFAULT 0,"
                            + "confidence REAL DEFAULT 0,"
                            + "position_score REAL DEFAULT 0,"
                            + "number_score REAL DEFAULT 0,"
                            + "state_score REAL DEFAULT 0,"
                            + "trend_score REAL DEFAULT 0,"
                            + "shape_score REAL DEFAULT 0,"
                            + "risk_score REAL DEFAULT 0,"
                            + "prob_adjust REAL DEFAULT 0,"
                            + "gate_adjust REAL DEFAULT 0,"
                            + "risk_penalty REAL DEFAULT 0,"
                            + "teacher_weight REAL DEFAULT 1,"
                            + "updated_at INTEGER DEFAULT 0,"
                            + "last_hit_at INTEGER DEFAULT 0,"
                            + "last_miss_at INTEGER DEFAULT 0,"
                            + "status TEXT,"
                            + "why_text TEXT,"
                            + "summary_text TEXT"
                            + ")");

            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_tm_profile_state ON "
                            + TABLE_TEACHER_MATRIX_PROFILE
                            + "(state_key)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_tm_profile_pos_num ON "
                            + TABLE_TEACHER_MATRIX_PROFILE
                            + "(position,number)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_tm_profile_status ON "
                            + TABLE_TEACHER_MATRIX_PROFILE
                            + "(status)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_tm_profile_score ON "
                            + TABLE_TEACHER_MATRIX_PROFILE
                            + "(confidence DESC, hit_rate DESC, sample_count DESC)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_tm_profile_updated ON "
                            + TABLE_TEACHER_MATRIX_PROFILE
                            + "(updated_at)");
        } catch (Throwable e) {
        }
    }

    private void ensureTeacherMatrixComboProfileTable(SQLiteDatabase db) {
        if (db == null) {
            return;
        }

        try {
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS "
                            + TABLE_TEACHER_MATRIX_COMBO_PROFILE
                            + " ("
                            + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "combo_key TEXT NOT NULL UNIQUE,"
                            + "state_key TEXT,"
                            + "position INTEGER DEFAULT 0,"
                            + "k3_mask INTEGER DEFAULT 0,"
                            + "sample_count INTEGER DEFAULT 0,"
                            + "hit_count INTEGER DEFAULT 0,"
                            + "miss_count INTEGER DEFAULT 0,"
                            + "recent_sample INTEGER DEFAULT 0,"
                            + "recent_hit INTEGER DEFAULT 0,"
                            + "hit_rate REAL DEFAULT 0,"
                            + "recent_hit_rate REAL DEFAULT 0,"
                            + "confidence REAL DEFAULT 0,"
                            + "combo_score REAL DEFAULT 0,"
                            + "risk_score REAL DEFAULT 0,"
                            + "prob_adjust REAL DEFAULT 0,"
                            + "gate_adjust REAL DEFAULT 0,"
                            + "updated_at INTEGER DEFAULT 0,"
                            + "status TEXT,"
                            + "summary_text TEXT"
                            + ")");

            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_tm_combo_state ON "
                            + TABLE_TEACHER_MATRIX_COMBO_PROFILE
                            + "(state_key)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_tm_combo_pos_mask ON "
                            + TABLE_TEACHER_MATRIX_COMBO_PROFILE
                            + "(position,k3_mask)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_tm_combo_status ON "
                            + TABLE_TEACHER_MATRIX_COMBO_PROFILE
                            + "(status)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_tm_combo_score ON "
                            + TABLE_TEACHER_MATRIX_COMBO_PROFILE
                            + "(confidence DESC, hit_rate DESC, sample_count DESC)");
        } catch (Throwable e) {
        }
    }

    private void ensureTeacherMatrixReportTable(SQLiteDatabase db) {
        if (db == null) {
            return;
        }

        try {
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS "
                            + TABLE_TEACHER_MATRIX_REPORT
                            + " ("
                            + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "report_id TEXT NOT NULL UNIQUE,"
                            + "task_key TEXT,"
                            + "segment_key TEXT,"
                            + "source_type TEXT,"
                            + "created_at INTEGER DEFAULT 0,"
                            + "started_at INTEGER DEFAULT 0,"
                            + "finished_at INTEGER DEFAULT 0,"
                            + "cost_ms INTEGER DEFAULT 0,"
                            + "window_size INTEGER DEFAULT 0,"
                            + "target_count INTEGER DEFAULT 0,"
                            + "sample_count INTEGER DEFAULT 0,"
                            + "positive_count INTEGER DEFAULT 0,"
                            + "negative_count INTEGER DEFAULT 0,"
                            + "profile_count INTEGER DEFAULT 0,"
                            + "combo_profile_count INTEGER DEFAULT 0,"
                            + "strong_profile_count INTEGER DEFAULT 0,"
                            + "risk_profile_count INTEGER DEFAULT 0,"
                            + "applied_count INTEGER DEFAULT 0,"
                            + "avg_hit_rate REAL DEFAULT 0,"
                            + "avg_confidence REAL DEFAULT 0,"
                            + "avg_prob_adjust REAL DEFAULT 0,"
                            + "avg_gate_adjust REAL DEFAULT 0,"
                            + "status TEXT,"
                            + "summary_text TEXT,"
                            + "error_text TEXT"
                            + ")");

            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_tm_report_created ON "
                            + TABLE_TEACHER_MATRIX_REPORT
                            + "(created_at)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_tm_report_task ON "
                            + TABLE_TEACHER_MATRIX_REPORT
                            + "(task_key)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_tm_report_segment ON "
                            + TABLE_TEACHER_MATRIX_REPORT
                            + "(segment_key)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_tm_report_status ON "
                            + TABLE_TEACHER_MATRIX_REPORT
                            + "(status)");
        } catch (Throwable e) {
        }
    }

    private void ensureRuntimeExtraColumns(SQLiteDatabase db) {
        if (db == null) {
            return;
        }

        try {
            addColumnIfMissing(db, DbHelper.TABLE_DECISIONS, "action", "TEXT");
            addColumnIfMissing(db, DbHelper.TABLE_DECISIONS, "state_key", "TEXT");
            addColumnIfMissing(db, DbHelper.TABLE_DECISIONS, "trend_key", "TEXT");
            addColumnIfMissing(db, DbHelper.TABLE_DECISIONS, "drift_key", "TEXT");
            addColumnIfMissing(db, DbHelper.TABLE_DECISIONS, "break_key", "TEXT");
            addColumnIfMissing(db, DbHelper.TABLE_DECISIONS, "recovery_key", "TEXT");
            addColumnIfMissing(db, DbHelper.TABLE_DECISIONS, "reason_text", "TEXT");
            addColumnIfMissing(db, DbHelper.TABLE_DECISIONS, "gate_mode", "TEXT");
            addColumnIfMissing(
                    db, DbHelper.TABLE_DECISIONS, "runtime_profile_score", "REAL DEFAULT 0");
            addColumnIfMissing(
                    db, DbHelper.TABLE_DECISIONS, "state_number_profile_score", "REAL DEFAULT 0");
            addColumnIfMissing(db, DbHelper.TABLE_DECISIONS, "teacher_weight", "REAL DEFAULT 1");
            addColumnIfMissing(db, DbHelper.TABLE_DECISIONS, "state_confidence", "REAL DEFAULT 0");
            addColumnIfMissing(db, DbHelper.TABLE_DECISIONS, "state_sample", "INTEGER DEFAULT 0");
            addColumnIfMissing(db, DbHelper.TABLE_DECISIONS, "cache_sample", "INTEGER DEFAULT 0");
            addColumnIfMissing(db, DbHelper.TABLE_DECISIONS, "recent_sample", "INTEGER DEFAULT 0");
            addColumnIfMissing(db, DbHelper.TABLE_DECISIONS, "recent_hit_rate", "REAL DEFAULT 0");
            addColumnIfMissing(db, DbHelper.TABLE_DECISIONS, "runtime_hit_rate", "REAL DEFAULT 0");
            addColumnIfMissing(
                    db, DbHelper.TABLE_DECISIONS, "number_profile_hit_rate", "REAL DEFAULT 0");
            addColumnIfMissing(
                    db, DbHelper.TABLE_DECISIONS, "realtime_cost_ms", "INTEGER DEFAULT 0");
            addColumnIfMissing(
                    db, DbHelper.TABLE_DECISIONS, "realtime_timeout", "INTEGER DEFAULT 0");

            addColumnIfMissing(db, DbHelper.TABLE_TRADES, "state_key", "TEXT");
            addColumnIfMissing(
                    db, DbHelper.TABLE_TRADES, "runtime_profile_score", "REAL DEFAULT 0");
            addColumnIfMissing(
                    db, DbHelper.TABLE_TRADES, "state_number_profile_score", "REAL DEFAULT 0");
            addColumnIfMissing(db, DbHelper.TABLE_TRADES, "teacher_weight", "REAL DEFAULT 1");

            addColumnIfMissing(db, DbHelper.TABLE_SHADOW, "state_key", "TEXT");
            addColumnIfMissing(
                    db, DbHelper.TABLE_SHADOW, "runtime_profile_score", "REAL DEFAULT 0");
            addColumnIfMissing(
                    db, DbHelper.TABLE_SHADOW, "state_number_profile_score", "REAL DEFAULT 0");
            addColumnIfMissing(db, DbHelper.TABLE_SHADOW, "number_bias", "REAL DEFAULT 0");
            addColumnIfMissing(db, DbHelper.TABLE_SHADOW, "combo_bias", "REAL DEFAULT 0");
            addColumnIfMissing(db, DbHelper.TABLE_SHADOW, "teacher_weight", "REAL DEFAULT 1");
            addColumnIfMissing(db, DbHelper.TABLE_SHADOW, "state_confidence", "REAL DEFAULT 0");
            addColumnIfMissing(db, DbHelper.TABLE_SHADOW, "state_sample", "INTEGER DEFAULT 0");
            addColumnIfMissing(db, DbHelper.TABLE_SHADOW, "cache_sample", "INTEGER DEFAULT 0");
            addColumnIfMissing(db, DbHelper.TABLE_SHADOW, "recent_sample", "INTEGER DEFAULT 0");
            addColumnIfMissing(db, DbHelper.TABLE_SHADOW, "recent_hit_rate", "REAL DEFAULT 0");

            addColumnIfMissing(db, DbHelper.TABLE_SHAPE_VECTOR, "state_key", "TEXT");
            addColumnIfMissing(
                    db, DbHelper.TABLE_SHAPE_VECTOR, "runtime_profile_score", "REAL DEFAULT 0");
            addColumnIfMissing(
                    db, DbHelper.TABLE_SHAPE_VECTOR, "teacher_matrix_score", "REAL DEFAULT 0");

            addColumnIfMissing(db, DbHelper.TABLE_CANDIDATE_EVIDENCE, "state_key", "TEXT");
            addColumnIfMissing(
                    db,
                    DbHelper.TABLE_CANDIDATE_EVIDENCE,
                    "runtime_profile_evidence",
                    "REAL DEFAULT 0");
            addColumnIfMissing(
                    db,
                    DbHelper.TABLE_CANDIDATE_EVIDENCE,
                    "state_number_evidence",
                    "REAL DEFAULT 0");

            addColumnIfMissing(
                    db, DbHelper.TABLE_MODEL_WEIGHT, "runtime_profile_weight", "REAL DEFAULT 0.12");
            addColumnIfMissing(
                    db,
                    DbHelper.TABLE_MODEL_WEIGHT,
                    "state_number_profile_weight",
                    "REAL DEFAULT 0.12");
            addColumnIfMissing(db, DbHelper.TABLE_MODEL_WEIGHT, "teacher_weight", "REAL DEFAULT 1");

            addColumnIfMissing(
                    db, DbHelper.TABLE_BACKTEST, "runtime_profile_count", "INTEGER DEFAULT 0");
            addColumnIfMissing(
                    db, DbHelper.TABLE_BACKTEST, "state_number_profile_count", "INTEGER DEFAULT 0");
            addColumnIfMissing(
                    db, DbHelper.TABLE_BACKTEST, "learning_case_count", "INTEGER DEFAULT 0");

            addColumnIfMissing(
                    db,
                    DbHelper.TABLE_SELF_DIAGNOSIS_LOG,
                    "runtime_profile_count",
                    "INTEGER DEFAULT 0");
            addColumnIfMissing(
                    db,
                    DbHelper.TABLE_SELF_DIAGNOSIS_LOG,
                    "state_number_profile_count",
                    "INTEGER DEFAULT 0");
            addColumnIfMissing(
                    db,
                    DbHelper.TABLE_SELF_DIAGNOSIS_LOG,
                    "learning_case_count",
                    "INTEGER DEFAULT 0");

            ensureFieldChainColumns(db);
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_decisions_state_key ON "
                            + DbHelper.TABLE_DECISIONS
                            + "(state_key,position)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_decisions_target_created ON "
                            + DbHelper.TABLE_DECISIONS
                            + "(target_issue,created_at)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_trades_state_key ON "
                            + DbHelper.TABLE_TRADES
                            + "(state_key,position)");
        } catch (Throwable e) {
        }
    }

    private void ensureTeacherMatrixColumns(SQLiteDatabase db) {
        if (db == null) {
            return;
        }

        try {
            addTeacherMatrixColumnsToDecisionLikeTable(db, DbHelper.TABLE_DECISIONS);
            addTeacherMatrixColumnsToDecisionLikeTable(db, DbHelper.TABLE_TRADES);
            addTeacherMatrixColumnsToDecisionLikeTable(db, DbHelper.TABLE_SHADOW);

            addColumnIfMissing(
                    db, DbHelper.TABLE_MODEL_WEIGHT, "teacher_matrix_weight", "REAL DEFAULT 0.18");

            addColumnIfMissing(
                    db,
                    DbHelper.TABLE_BACKTEST,
                    "teacher_matrix_sample_count",
                    "INTEGER DEFAULT 0");
            addColumnIfMissing(
                    db,
                    DbHelper.TABLE_BACKTEST,
                    "teacher_matrix_profile_count",
                    "INTEGER DEFAULT 0");
            addColumnIfMissing(
                    db, DbHelper.TABLE_BACKTEST, "teacher_matrix_avg_hit_rate", "REAL DEFAULT 0");
            addColumnIfMissing(
                    db, DbHelper.TABLE_BACKTEST, "teacher_matrix_avg_confidence", "REAL DEFAULT 0");
            addColumnIfMissing(db, DbHelper.TABLE_BACKTEST, "teacher_matrix_summary", "TEXT");

            addColumnIfMissing(
                    db,
                    DbHelper.TABLE_LEARNING_SNAPSHOT,
                    "teacher_matrix_sample_count",
                    "INTEGER DEFAULT 0");
            addColumnIfMissing(
                    db,
                    DbHelper.TABLE_LEARNING_SNAPSHOT,
                    "teacher_matrix_hit_rate",
                    "REAL DEFAULT 0");
            addColumnIfMissing(
                    db,
                    DbHelper.TABLE_LEARNING_SNAPSHOT,
                    "teacher_matrix_confidence",
                    "REAL DEFAULT 0");
            addColumnIfMissing(
                    db, DbHelper.TABLE_LEARNING_SNAPSHOT, "teacher_matrix_summary", "TEXT");

            addColumnIfMissing(
                    db,
                    DbHelper.TABLE_CANDIDATE_EVIDENCE,
                    "teacher_matrix_evidence",
                    "REAL DEFAULT 0");
            addColumnIfMissing(
                    db,
                    DbHelper.TABLE_POINT_IN_TIME_SNAPSHOT,
                    "visible_teacher_matrix_version",
                    "INTEGER DEFAULT 0");

            addColumnIfMissing(
                    db, DbHelper.TABLE_DEEP_PROFILE, "teacher_matrix_score", "REAL DEFAULT 0");
            addColumnIfMissing(
                    db, DbHelper.TABLE_DEEP_ATTRIBUTION, "teacher_matrix_impact", "REAL DEFAULT 0");
            addColumnIfMissing(
                    db,
                    DbHelper.TABLE_PROMOTION_CANDIDATE,
                    "teacher_matrix_score",
                    "REAL DEFAULT 0");

            addColumnIfMissing(
                    db,
                    DbHelper.TABLE_SELF_DIAGNOSIS_LOG,
                    "teacher_matrix_sample_count",
                    "INTEGER DEFAULT 0");
            addColumnIfMissing(
                    db,
                    DbHelper.TABLE_SELF_DIAGNOSIS_LOG,
                    "teacher_matrix_profile_count",
                    "INTEGER DEFAULT 0");
            addColumnIfMissing(
                    db,
                    DbHelper.TABLE_SELF_DIAGNOSIS_LOG,
                    "teacher_matrix_strong_count",
                    "INTEGER DEFAULT 0");
            addColumnIfMissing(
                    db,
                    DbHelper.TABLE_SELF_DIAGNOSIS_LOG,
                    "teacher_matrix_risk_count",
                    "INTEGER DEFAULT 0");
            addColumnIfMissing(
                    db, DbHelper.TABLE_SELF_DIAGNOSIS_LOG, "teacher_matrix_text", "TEXT");

            addColumnIfMissing(
                    db,
                    TABLE_REALTIME_VALIDATION_REPORT,
                    "teacher_matrix_sample_count",
                    "INTEGER DEFAULT 0");
            addColumnIfMissing(
                    db,
                    TABLE_REALTIME_VALIDATION_REPORT,
                    "teacher_matrix_used_count",
                    "INTEGER DEFAULT 0");
            addColumnIfMissing(
                    db,
                    TABLE_REALTIME_VALIDATION_REPORT,
                    "teacher_matrix_avg_confidence",
                    "REAL DEFAULT 0");
            addColumnIfMissing(
                    db, TABLE_REALTIME_VALIDATION_REPORT, "teacher_matrix_summary", "TEXT");
        } catch (Throwable e) {
        }
    }

    private void addTeacherMatrixColumnsToDecisionLikeTable(SQLiteDatabase db, String table) {
        addColumnIfMissing(db, table, "teacher_matrix_sample", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "teacher_matrix_hit_rate", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "teacher_matrix_confidence", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "teacher_matrix_prob_adjust", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "teacher_matrix_gate_adjust", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "teacher_matrix_risk_penalty", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "teacher_matrix_applied", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "teacher_matrix_status", "TEXT");
        addColumnIfMissing(db, table, "teacher_matrix_profile_key", "TEXT");
        addColumnIfMissing(db, table, "teacher_matrix_combo_key", "TEXT");
        addColumnIfMissing(db, table, "teacher_matrix_summary", "TEXT");
    }

    private void ensureLedgerColumns(SQLiteDatabase db) {
        if (db == null) {
            return;
        }

        try {
            addColumnIfMissing(db, DbHelper.TABLE_DECISIONS, "trade_id", "INTEGER DEFAULT 0");
            addColumnIfMissing(db, DbHelper.TABLE_DECISIONS, "settle_time", "INTEGER DEFAULT 0");
            addColumnIfMissing(db, DbHelper.TABLE_DECISIONS, "settled_at", "INTEGER DEFAULT 0");
            addColumnIfMissing(db, DbHelper.TABLE_DECISIONS, "actual_number", "INTEGER DEFAULT 0");
            addColumnIfMissing(db, DbHelper.TABLE_DECISIONS, "hit", "INTEGER DEFAULT 0");
            addColumnIfMissing(db, DbHelper.TABLE_DECISIONS, "pnl", "REAL DEFAULT 0");
            addColumnIfMissing(db, DbHelper.TABLE_DECISIONS, "miss_reason", "TEXT");
            addColumnIfMissing(db, DbHelper.TABLE_DECISIONS, "settlement_reason", "TEXT");
            addColumnIfMissing(db, DbHelper.TABLE_DECISIONS, "stat_scope", "TEXT");
            addColumnIfMissing(db, DbHelper.TABLE_DECISIONS, "settled", "INTEGER DEFAULT 0");
            addColumnIfMissing(db, DbHelper.TABLE_DECISIONS, "result", "TEXT");

            addColumnIfMissing(db, DbHelper.TABLE_TRADES, "decision_id", "INTEGER DEFAULT 0");
            addColumnIfMissing(db, DbHelper.TABLE_TRADES, "action", "TEXT");
            addColumnIfMissing(db, DbHelper.TABLE_TRADES, "settlement_reason", "TEXT");
            addColumnIfMissing(db, DbHelper.TABLE_TRADES, "settled_at", "INTEGER DEFAULT 0");
            addColumnIfMissing(db, DbHelper.TABLE_TRADES, "created_at", "INTEGER DEFAULT 0");
            addColumnIfMissing(db, DbHelper.TABLE_TRADES, "stat_scope", "TEXT");
            addColumnIfMissing(db, DbHelper.TABLE_TRADES, "actual_number", "INTEGER DEFAULT 0");
            addColumnIfMissing(db, DbHelper.TABLE_TRADES, "hit", "INTEGER DEFAULT 0");
            addColumnIfMissing(db, DbHelper.TABLE_TRADES, "pnl", "REAL DEFAULT 0");
            addColumnIfMissing(db, DbHelper.TABLE_TRADES, "miss_reason", "TEXT");
            addColumnIfMissing(db, DbHelper.TABLE_TRADES, "settled", "INTEGER DEFAULT 0");
            addColumnIfMissing(db, DbHelper.TABLE_TRADES, "result", "TEXT");
        } catch (Throwable e) {
        }
    }

    private void ensureLedgerIndexes(SQLiteDatabase db) {
        if (db == null) {
            return;
        }

        try {
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_decisions_trade_id ON "
                            + DbHelper.TABLE_DECISIONS
                            + "(trade_id)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_decisions_scope_settled ON "
                            + DbHelper.TABLE_DECISIONS
                            + "(stat_scope,settled)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_decisions_target_decision_scope ON "
                            + DbHelper.TABLE_DECISIONS
                            + "(target_issue,decision,stat_scope)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_trades_target_scope ON "
                            + DbHelper.TABLE_TRADES
                            + "(target_issue,stat_scope)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_trades_settled_scope ON "
                            + DbHelper.TABLE_TRADES
                            + "(settled,stat_scope)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_trades_decision_id ON "
                            + DbHelper.TABLE_TRADES
                            + "(decision_id)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_trades_action_scope_settled ON "
                            + DbHelper.TABLE_TRADES
                            + "(action,stat_scope,settled)");
        } catch (Throwable e) {
        }
    }

    private void ensureFieldChainColumns(SQLiteDatabase db) {
        if (db == null) {
            return;
        }
        try {
            addFieldChainColumnsToDecisionLikeTable(db, DbHelper.TABLE_DECISIONS);
            addFieldChainColumnsToDecisionLikeTable(db, DbHelper.TABLE_TRADES);
            addFieldChainColumnsToDecisionLikeTable(db, DbHelper.TABLE_SHADOW);
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_decisions_field_chain_reject ON "
                            + DbHelper.TABLE_DECISIONS
                            + "(final_decision_reject_code,target_issue)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_trades_field_chain_reject ON "
                            + DbHelper.TABLE_TRADES
                            + "(final_decision_reject_code,target_issue)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_trades_field_chain_teacher ON "
                            + DbHelper.TABLE_TRADES
                            + "(teacher_matrix_status,v20_status,target_issue)");
            clearColumnCache();
        } catch (Throwable e) {
        }
    }

    private void addFieldChainColumnsToDecisionLikeTable(SQLiteDatabase db, String table) {
        if (db == null || table == null || table.length() == 0) {
            return;
        }

        addColumnIfMissing(db, table, "teacher_weak_no_bet_flag", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "teacher_matrix_weak_no_bet_flag", "INTEGER DEFAULT 0");
        addColumnIfMissing(
                db, table, "teacher_matrix_ordinary_bet_block_flag", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "teacher_matrix_supports_final_gate", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "teacher_matrix_supports_bet", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "teacher_ordinary_bet_policy", "TEXT");
        addColumnIfMissing(db, table, "teacher_matrix_ordinary_bet_policy", "TEXT");
        addColumnIfMissing(db, table, "teacher_field_source", "TEXT");
        addColumnIfMissing(db, table, "teacher_field_version", "TEXT");
        addColumnIfMissing(db, table, "teacher_number_status", "TEXT");
        addColumnIfMissing(db, table, "teacher_status", "TEXT");
        addColumnIfMissing(db, table, "teacher_evidence_contribution", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "teacher_matrix_evidence_contribution", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "teacher_counter_contribution", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "teacher_matrix_counter_contribution", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "teacher_sample_reliability", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "teacher_recent_validity", "REAL DEFAULT 0");

        addColumnIfMissing(db, table, "v20_status", "TEXT");
        addColumnIfMissing(db, table, "dynamic_status", "TEXT");
        addColumnIfMissing(db, table, "dynamic_cause_status", "TEXT");
        addColumnIfMissing(db, table, "v20_support_score", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "v20_risk_score", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "v20_evidence_contribution", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "v20_counter_contribution", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "v20_sample_reliability", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "v20_recent_validity", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "v20_action_advice", "TEXT");
        addColumnIfMissing(db, table, "v20_window_state_key", "TEXT");
        addColumnIfMissing(db, table, "window_state_key", "TEXT");
        addColumnIfMissing(db, table, "final_decision_window_state_key", "TEXT");
        addColumnIfMissing(db, table, "dynamic_window_state_key", "TEXT");
        addColumnIfMissing(db, table, "probability_window_state_key", "TEXT");

        addColumnIfMissing(db, table, "coverage_quality_score", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "third_number_quality_score", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "k3_coverage_risk", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "false_high_p_flag", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "probability_false_high_p_flag", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "probability_false_high_risk", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "probability_preferred_edge_band", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "probability_teacher_weak_or_empty", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "probability_v20_unknown_or_empty", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "probability_strong_cause_flag", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "probability_coverage_summary", "TEXT");
        addColumnIfMissing(db, table, "probability_action_advice", "TEXT");

        addColumnIfMissing(db, table, "dynamic_gate_false_high_p_flag", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "dynamic_gate_preferred_edge_band", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "dynamic_gate_edge_policy", "TEXT");
        addColumnIfMissing(db, table, "dynamic_gate_coverage_risk", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "dynamic_gate_coverage_quality_score", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "dynamic_gate_third_number_quality", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "dynamic_gate_virtual_edge_risk", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "dynamic_gate_version", "TEXT");

        addColumnIfMissing(db, table, "final_decision_version", "TEXT");
        addColumnIfMissing(db, table, "final_decision_edge_policy", "TEXT");
        addColumnIfMissing(db, table, "final_decision_teacher_policy", "TEXT");
        addColumnIfMissing(db, table, "final_decision_v20_policy", "TEXT");
        addColumnIfMissing(db, table, "final_decision_bet_domain", "TEXT");
        addColumnIfMissing(db, table, "final_decision_reject_code", "TEXT");
        addColumnIfMissing(db, table, "final_decision_shadow_reason", "TEXT");
        addColumnIfMissing(db, table, "final_decision_coverage_risk", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "final_decision_third_number_quality", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "final_decision_false_high_p_flag", "INTEGER DEFAULT 0");
    }

    private void addColumnIfMissing(
            SQLiteDatabase db, String table, String column, String typeSql) {
        if (db == null
                || table == null
                || table.length() == 0
                || column == null
                || column.length() == 0) {
            return;
        }

        try {
            HashSet<String> columns = getTableColumns(db, table);

            if (columns != null && columns.contains(column)) {
                return;
            }

            db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " " + typeSql);
            /* Keep the just-read table cache and advance it in place.  Clearing the complete
             * cache after each ALTER made a first upgrade re-run PRAGMA table_info for every
             * remaining column (quadratic startup work across the historical schema). */
            synchronized (columnCache) {
                HashSet<String> cached = columnCache.get(table);
                if (cached != null) cached.add(column);
            }
        } catch (Throwable e) {
        }
    }

    private void repairLedgerIntegrityQuiet(SQLiteDatabase db) {
        if (db == null) {
            return;
        }

        try {
            ensureLedgerColumns(db);
            ensureLedgerIndexes(db);

            normalizeLegacyLedgerScopes(db, LEDGER_REPAIR_LIMIT * 3);
            repairMissingRealTradesFromBetDecisions(db, LEDGER_REPAIR_LIMIT);
            repairTradeDecisionLinks(db, LEDGER_REPAIR_LIMIT);
            repairSettledTradePnl(db, LEDGER_REPAIR_LIMIT);
            cleanupDuplicateRealTradesAll(db, LEDGER_REPAIR_LIMIT);
        } catch (Throwable e) {
        }
    }

    private void normalizeLegacyLedgerScopes(SQLiteDatabase db, int limit) {
        // CP4: old labels are interpreted by read-only legacy views; never promote blanks to REAL.
        // Existing audit/backup retains every original field. No business write here.
    }

    /** Keep legacy rows for audit, but never let them re-enter V24 real statistics. */
    private void quarantineUnauthorizedV24LedgerRows(SQLiteDatabase db) {
        // CP4: diagnostic consumers reject unproven rows by joins; do not rewrite historical REAL.
        // Release preflight lists conflicting rows and requires explicit verified migration handling.
    }

    private int repairMissingRealTradesFromBetDecisions(SQLiteDatabase db, int limit) {
        return 0; // CP4 audit/quarantine only; immutable REAL rows are never silently repaired or deleted.
    }

    private int repairTradeDecisionLinks(SQLiteDatabase db, int limit) {
        return 0; // CP4 audit/quarantine only; immutable REAL rows are never silently repaired or deleted.
    }

    private int repairSettledTradePnl(SQLiteDatabase db, int limit) {
        return 0; // CP4 audit/quarantine only; immutable REAL rows are never silently repaired or deleted.
    }

    private int cleanupDuplicateRealTradesAll(SQLiteDatabase db, int limit) {
        return 0; // CP4 audit/quarantine only; immutable REAL rows are never silently repaired or deleted.
    }

    private int cleanupDuplicateTradesForTarget(SQLiteDatabase db, String targetIssue) {
        return 0; // CP4 audit/quarantine only; immutable REAL rows are never silently repaired or deleted.
    }

    private Models.Trade chooseBestTradeToKeep(ArrayList<Models.Trade> list) {
        if (list == null || list.size() == 0) {
            return null;
        }

        Models.Trade best = null;

        for (int i = 0; i < list.size(); i++) {
            Models.Trade t = list.get(i);

            if (t == null || t.id <= 0L) {
                continue;
            }

            if (best == null) {
                best = t;
                continue;
            }

            int p1 = tradeKeepPriority(t);
            int p2 = tradeKeepPriority(best);

            if (p1 > p2) {
                best = t;
            } else if (p1 == p2) {
                long a =
                        t.settledAt > 0L
                                ? t.settledAt
                                : (t.settleTime > 0L ? t.settleTime : t.betTime);
                long b =
                        best.settledAt > 0L
                                ? best.settledAt
                                : (best.settleTime > 0L ? best.settleTime : best.betTime);

                if (a > b) {
                    best = t;
                } else if (a == b && t.id > best.id) {
                    best = t;
                }
            }
        }

        return best;
    }

    private int tradeKeepPriority(Models.Trade t) {
        if (t == null) {
            return 0;
        }

        int p = 0;

        if (t.settled == 1 && AppConfig.isTerminalResult(t.result)) {
            p += 1000;
        }

        if (AppConfig.RESULT_WIN.equals(AppConfig.normalizeResult(t.result))) {
            p += 100;
        }

        if (AppConfig.RESULT_MISS.equals(AppConfig.normalizeResult(t.result))) {
            p += 80;
        }

        if (t.decisionId > 0L) {
            p += 40;
        }

        if (AppConfig.isValidPosition(t.position)) {
            p += 20;
        }

        if (Models.isValidK3Mask(t.k3Mask)) {
            p += 20;
        }

        if (t.betTime > 0L || t.createdAt > 0L) {
            p += 10;
        }

        return p;
    }

    public long saveDraw(final Models.Draw draw) {
        if (draw == null) return -1L;
        try {
            draw.normalizeDirty();
        } catch (Throwable normalizeFailure) {
            RuntimeRecoveryCoordinatorR926.record(
                    this, "DRAW_NORMALIZE_BEFORE_SAVE", draw.issue, normalizeFailure);
            return -1L;
        }
        if (!draw.isValid()) return -1L;
        Long out = coordinator.runWriteOrThrowF01(
                "R914_DRAW_SAVE_ATOMIC",
                new DatabaseCoordinator.WriteCallable<Long>() {
                    @Override public Long call(SQLiteDatabase db) {
                        boolean owns = !db.inTransaction();
                        if (owns) db.beginTransaction();
                        try {
                            FetchLedgerR925.observeDrawWithinR82(db,draw);
                            long rowId = insertDrawInternal(db, draw);
                            if (rowId == R811_DRAW_CONFLICT_BLOCKED || rowId == CP12_DRAW_RETENTION_DEFERRED || rowId == CP12_DRAW_OUTSIDE_HISTORY_TAIL) {
                                if (owns) db.setTransactionSuccessful();
                                return Long.valueOf(rowId);
                            }
                            FetchLedgerR925.canonicalDrawWithinR82(db,draw);
                            SettlementTaskStoreR36.enqueueConfirmedWithinWrite(
                                    db, draw, System.currentTimeMillis(),
                                    SettlementTaskStoreR36.BACKLOG,
                                    SettlementTaskStoreR36.PRIORITY_BACKLOG,
                                    "R914_DRAW_SAVE_ATOMIC");
                            RuntimeEventLedgerR926.appendWithinTransaction(db,
                                    RuntimeEventLedgerR926.Event.of(draw.issue, "", "", "FETCH", "CANONICAL_ACCEPTED",
                                            RuntimeEventLedgerR926.RANK_ACCEPTED,
                                            "numbers=" + draw.numbersText() + "|source=" + draw.sourceLine + "|draw_at=" + draw.openTime+"|"+FetchLedgerR925.canonicalBindingSummaryR82(draw),
                                            "DbStore.saveDraw", "DRAW_ACCEPTED|" + draw.issue + "|" + V51ChecksumEngine.sha256(draw.numbersText())));
                            if (owns) db.setTransactionSuccessful();
                            return Long.valueOf(rowId);
                        } finally {
                            if (owns && db.inTransaction()) db.endTransaction();
                        }
                    }
                });
        return out == null ? -1L : out.longValue();
    }

    private long insertDrawInternal(SQLiteDatabase db, Models.Draw draw) {
        if (db == null || draw == null) {
            return -1L;
        }

        try {
            draw.normalizeDirty();
        } catch (Throwable e) {
            throw new IllegalArgumentException("R926_DRAW_NORMALIZE_IN_TX_FAILED", e);
        }

        if (!draw.isValid()) {
            return -1L;
        }

        DrawCanonicalizerR811.PersistenceDecision canonical =
                DrawCanonicalizerR811.observeWithinWrite(db, draw);
        if (!canonical.confirmed) return R811_DRAW_CONFLICT_BLOCKED;

        // Resolve an existing issue before INSERT OR IGNORE. On AUTOINCREMENT
        // tables even an ignored duplicate advances sqlite_sequence and executes
        // BEFORE INSERT guards; polling must not mutate an unchanged draw.
        try(Cursor existing=db.rawQuery(
                "SELECT draw_hash_v2,draw_hash_version,open_time,fetch_time,first_seen_at,source_line,raw_json FROM "
                        +DbHelper.TABLE_DRAWS+" WHERE issue=? LIMIT 1",new String[]{draw.issue})){
            if(existing.moveToFirst()){
                if(!DrawHashR36.matchesPersisted(existing.getString(0),existing.getInt(1),draw))
                    return R811_DRAW_CONFLICT_BLOCKED;
                repairDrawMetadataR816(db,draw,existing.getLong(2),existing.getLong(3),existing.getLong(4),
                        safeString(existing.getString(5)),safeString(existing.getString(6)),
                        safeString(existing.getString(0)),existing.getInt(1));
                return -1L;
            }
        }
        if(!DataRetentionGovernorR900.reserveTailSlotWithinWriteCP12(db,draw.issue)){
            String oldest="";long count=0;
            try(Cursor tail=db.rawQuery("SELECT MIN(issue),COUNT(*) FROM draws WHERE dirty=0",null)){if(tail.moveToFirst()){oldest=safeString(tail.getString(0));count=tail.getLong(1);}}
            boolean outside=count>=5000&&!oldest.isEmpty()&&draw.issue.compareTo(oldest)<=0;
            db.execSQL("INSERT OR REPLACE INTO app_state(state_key,state_value,updated_at) VALUES('cp4_retention_requested','1',?)",new Object[]{System.currentTimeMillis()});
            db.execSQL("INSERT OR REPLACE INTO app_state(state_key,state_value,updated_at) VALUES('r979_retention_due_at','0',?)",new Object[]{System.currentTimeMillis()});
            db.execSQL("INSERT OR REPLACE INTO app_state(state_key,state_value,updated_at) VALUES('cp12_draw_retention_reason',?,?)",new Object[]{outside?"DRAW_OUTSIDE_HISTORY_TAIL":"DRAW_HISTORY_TAIL_DEPENDENCY_PENDING",System.currentTimeMillis()});
            // observeWithinWrite already durably recorded immutable provider provenance/canonicalhash.
            // No canonical acceptance event, settlementenqueue or targetadvance is emitted yet.
            return outside?CP12_DRAW_OUTSIDE_HISTORY_TAIL:CP12_DRAW_RETENTION_DEFERRED;
        }
        ContentValues cv = new ContentValues();

        cv.put("issue", draw.issue);
        cv.put("open_time", draw.openTime);
        cv.put("fetch_time", draw.fetchTime);
        cv.put("source_line", draw.sourceLine);
        // First receipt is immutable business provenance; later provider metadata corrections must
        // not reset the OOS boundary for this issue.
        cv.put(
                "first_seen_at",
                draw.fetchTime > 0L ? draw.fetchTime : TimeSourceR811.wallNowMs());
        cv.put("draw_hash_version", DrawHashR36.CURRENT_VERSION);
        cv.put("draw_hash_v2", DrawHashR36.sha256V2(draw));

        cv.put("n1", draw.numbers[0]);
        cv.put("n2", draw.numbers[1]);
        cv.put("n3", draw.numbers[2]);
        cv.put("n4", draw.numbers[3]);
        cv.put("n5", draw.numbers[4]);
        cv.put("n6", draw.numbers[5]);
        cv.put("n7", draw.numbers[6]);
        cv.put("n8", draw.numbers[7]);
        cv.put("n9", draw.numbers[8]);
        cv.put("n10", draw.numbers[9]);

        cv.put("raw_json", draw.rawJson);
        cv.put("dirty", draw.dirty);
        cv.put("dirty_reason", draw.dirtyReason);

        long inserted = insertFiltered(db, DbHelper.TABLE_DRAWS, cv, SQLiteDatabase.CONFLICT_IGNORE);
        if (inserted >= 0L) return inserted;
        return R811_DRAW_CONFLICT_BLOCKED;
    }

    /**
     * Repairs provider metadata for an already confirmed issue/permutation without changing its
     * business identity or immutable first receipt. This lets the UTC parser correction heal an
     * installed R815 database instead of requiring data deletion/reinstallation.
     */
    private void repairDrawMetadataR816(
            SQLiteDatabase db,
            Models.Draw candidate,
            long existingOpenTime,
            long existingFetchTime,
            long firstSeenAt,
            String existingSource,
            String existingRaw,
            String existingDrawHashR48,
            int existingDrawHashVersionR48) {
        if (db == null || candidate == null || !candidate.isValid()) return;
        ContentValues update = new ContentValues();
        long corrected =
                ProviderTimeReconcilerR817.prefer(
                        existingOpenTime,
                        candidate.openTime,
                        firstSeenAt,
                        candidate.fetchTime > 0L
                                ? candidate.fetchTime
                                : TimeSourceR811.wallNowMs());
        if (corrected > 0L && corrected != existingOpenTime)
            update.put("open_time", corrected);
        if (existingFetchTime <= 0L && candidate.fetchTime > 0L)
            update.put("fetch_time", candidate.fetchTime);
        if (firstSeenAt <= 0L && existingFetchTime > 0L)
            update.put("first_seen_at", existingFetchTime);
        if (safeString(existingSource).length() == 0
                && safeString(candidate.sourceLine).length() > 0)
            update.put("source_line", candidate.sourceLine);
        if (safeString(existingRaw).length() == 0 && safeString(candidate.rawJson).length() > 0)
            update.put("raw_json", candidate.rawJson);
        final String candidateHashR48 = DrawHashR36.sha256V2(candidate);
        if (existingDrawHashVersionR48 != DrawHashR36.CURRENT_VERSION
                || !candidateHashR48.equals(existingDrawHashR48)) {
            update.put("draw_hash_version", DrawHashR36.CURRENT_VERSION);
            update.put("draw_hash_v2", candidateHashR48);
        }
        // Identical provider polling must preserve archive CAS and training input generations.
        if (update.size() == 0) return;
        int changed =
                db.update(
                        DbHelper.TABLE_DRAWS,
                        update,
                        "issue=? AND n1=? AND n2=? AND n3=? AND n4=? AND n5=? AND n6=? AND n7=? AND n8=? AND n9=? AND n10=?",
                        new String[] {
                            candidate.issue,
                            String.valueOf(candidate.numbers[0]),
                            String.valueOf(candidate.numbers[1]),
                            String.valueOf(candidate.numbers[2]),
                            String.valueOf(candidate.numbers[3]),
                            String.valueOf(candidate.numbers[4]),
                            String.valueOf(candidate.numbers[5]),
                            String.valueOf(candidate.numbers[6]),
                            String.valueOf(candidate.numbers[7]),
                            String.valueOf(candidate.numbers[8]),
                            String.valueOf(candidate.numbers[9])
                        });
        if (changed != 1)
            throw new IllegalStateException("R816_DRAW_METADATA_REPAIR_CARDINALITY_" + changed);
    }

    public int saveDraws(List<Models.Draw> draws) {
        if (draws == null || draws.size() == 0) return 0;
        final ArrayList<Models.Draw> valid = new ArrayList<Models.Draw>();
        for (int i = 0; i < draws.size(); i++) {
            Models.Draw draw = draws.get(i);
            if (draw == null) continue;
            try { draw.normalizeDirty(); } catch (Throwable ignored) {}
            if (draw.isValid()) valid.add(draw);
        }
        int inserted = 0;
        // Yield the sole fair writer between small history pages. Current draw,
        // predraw freeze and settlement must not wait behind a 48-row archive/CAS scan.
        final int chunk = 8;
        for (int from = 0; from < valid.size(); from += chunk) {
            final ArrayList<Models.Draw> batch = new ArrayList<Models.Draw>(
                    valid.subList(from, Math.min(valid.size(), from + chunk)));
            final Integer saved;
            try {
                saved = coordinator.runWriteOrThrowF01(
                    "R914_DRAW_HISTORY_BATCH",
                    new DatabaseCoordinator.WriteCallable<Integer>() {
                        @Override public Integer call(SQLiteDatabase db) {
                            boolean owns = !db.inTransaction();
                            if (owns) db.beginTransaction();
                            int count = 0;
                            try {
                                for (int i = 0; i < batch.size(); i++) {
                                    Models.Draw draw = batch.get(i);
                                    FetchLedgerR925.observeDrawWithinR82(db,draw);
                                    long rowId = insertDrawInternal(db, draw);
                                    if (rowId == R811_DRAW_CONFLICT_BLOCKED || rowId == CP12_DRAW_RETENTION_DEFERRED || rowId == CP12_DRAW_OUTSIDE_HISTORY_TAIL) continue;
                                    FetchLedgerR925.canonicalDrawWithinR82(db,draw);
                                    if (rowId >= 0L) count++;
                                    SettlementTaskStoreR36.enqueueConfirmedWithinWrite(
                                            db, draw, System.currentTimeMillis(),
                                            SettlementTaskStoreR36.BACKLOG,
                                            SettlementTaskStoreR36.PRIORITY_BACKLOG,
                                            "R914_DRAW_HISTORY_BATCH");
                                }
                                if (owns) db.setTransactionSuccessful();
                                return Integer.valueOf(count);
                            } finally {
                                if (owns && db.inTransaction()) db.endTransaction();
                            }
                        }
                    });
            } catch(RuntimeException writeFailure) {
                throw new IllegalStateException("R914_DRAW_HISTORY_BATCH_WRITE_FAILED|committedRows="
                        +inserted+"|fromOrdinal="+from+"|chunkCount="+batch.size(),writeFailure);
            }
            if (saved == null || saved.intValue() < 0)
                throw new IllegalStateException("F01_HISTORY_INVALID_RESULT");
            inserted += saved.intValue();
            if (RealtimePressureGateR811.hasActiveCriticalWork()) Thread.yield();
        }
        coordinator.runWrite(
                "R914_DRAW_HISTORY_TRIM",
                new DatabaseCoordinator.WriteRunnable() {
                    @Override public void run(SQLiteDatabase db) { trimDrawHistoryIfNeeded(db); }
                });
        return inserted;
    }

    public int saveDraws(ArrayList<Models.Draw> draws) {
        return saveDraws((List<Models.Draw>) draws);
    }

    private void trimDrawHistoryIfNeeded(SQLiteDatabase db) {
        if(db==null) throw new IllegalArgumentException("CP4_RETENTION_DB_NULL");
        // This import path may request retention; only DataRetentionGovernor may prune,
        // after archive verification, terminal/pin checks, and exact row-hash CAS.
        db.execSQL("INSERT OR REPLACE INTO app_state(state_key,state_value,updated_at) VALUES(?,?,?)",
            new Object[]{"cp4_retention_requested","1",Long.valueOf(System.currentTimeMillis())});
    }

    public boolean hasDrawIssue(String issue) {
        if (issue == null || issue.length() == 0) {
            return false;
        }

        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    DbHelper.TABLE_DRAWS,
                                    new String[] {"_id"},
                                    "issue=?",
                                    new String[] {issue},
                                    null,
                                    null,
                                    null,
                                    "1");

            return c.moveToFirst();
        } finally {
            closeCursor(c);
        }
    }

    public Models.Draw getLatestDraw() {
        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    DbHelper.TABLE_DRAWS,
                                    DrawHistoryProjectionR817.COLUMNS,
                                    "dirty=0",
                                    null,
                                    null,
                                    null,
                                    "issue DESC, open_time DESC, _id DESC",
                                    "1");

            if (c.moveToFirst()) {
                return readDraw(c);
            }
        } finally {
            closeCursor(c);
        }

        return null;
    }

    public Models.Draw getFirstDraw() {
        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    DbHelper.TABLE_DRAWS,
                                    DrawHistoryProjectionR817.COLUMNS,
                                    "dirty=0",
                                    null,
                                    null,
                                    null,
                                    "issue ASC, open_time ASC, _id ASC",
                                    "1");

            if (c.moveToFirst()) {
                return readDraw(c);
            }
        } finally {
            closeCursor(c);
        }

        return null;
    }

    public Models.Draw getDrawByIssue(String issue) {
        if (issue == null || issue.length() == 0) {
            return null;
        }

        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    DrawDependencyInputCP12.SOURCE,
                                    null,
                                    "issue=?",
                                    new String[] {issue},
                                    null,
                                    null,
                                    null,
                                    "1");

            if (c.moveToFirst()) {
                return readDraw(c);
            }
        } finally {
            closeCursor(c);
        }

        return null;
    }

    public Models.Draw loadDrawByIssue(String issue) {
        return getDrawByIssue(issue);
    }

    public List<Models.Draw> getRecentDraws(int limit) {
        ArrayList<Models.Draw> list = new ArrayList<Models.Draw>();

        if (limit <= 0) {
            return list;
        }

        Cursor c = null;

        try {
            StringBuilder columnsR927 = new StringBuilder();
            for (String columnR927 : DrawHistoryProjectionR817.COLUMNS) {
                if (columnsR927.length() > 0) columnsR927.append(',');
                columnsR927.append(columnR927);
            }
            c = BudgetedQueryR927.query(readable(), "SELECT " + columnsR927 + " FROM "
                    + DbHelper.TABLE_DRAWS + " WHERE dirty=0 ORDER BY issue DESC, open_time DESC, _id DESC LIMIT " + limit, null);

            while (c.moveToNext()) {
                list.add(readDraw(c));
            }
        } finally {
            closeCursor(c);
        }

        ArrayList<Models.Draw> ordered = new ArrayList<Models.Draw>();

        for (int i = list.size() - 1; i >= 0; i--) {
            ordered.add(list.get(i));
        }

        return ordered;
    }

    public List<Models.Draw> loadRecentDraws(int limit) {
        return getRecentDraws(limit);
    }

    public List<Models.Draw> getDrawsAsc(int limit) {
        ArrayList<Models.Draw> list = new ArrayList<Models.Draw>();

        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    DbHelper.TABLE_DRAWS,
                                    DrawHistoryProjectionR817.COLUMNS,
                                    "dirty=0",
                                    null,
                                    null,
                                    null,
                                    "issue ASC, open_time ASC, _id ASC",
                                    limit > 0 ? String.valueOf(limit) : null);

            while (c.moveToNext()) {
                list.add(readDraw(c));
            }
        } finally {
            closeCursor(c);
        }

        return list;
    }

    public List<Models.Draw> loadDrawsAsc(int limit) {
        return getDrawsAsc(limit);
    }

    public List<Models.Draw> getDrawsBeforeIssue(String issue, int limit) {
        ArrayList<Models.Draw> list = new ArrayList<Models.Draw>();

        if (issue == null || issue.length() == 0 || limit <= 0) {
            return list;
        }

        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    DbHelper.TABLE_DRAWS,
                                    DrawHistoryProjectionR817.COLUMNS,
                                    "dirty=0 AND issue<?",
                                    new String[] {issue},
                                    null,
                                    null,
                                    "issue DESC, open_time DESC, _id DESC",
                                    String.valueOf(limit));

            while (c.moveToNext()) {
                list.add(readDraw(c));
            }
        } finally {
            closeCursor(c);
        }

        ArrayList<Models.Draw> ordered = new ArrayList<Models.Draw>();

        for (int i = list.size() - 1; i >= 0; i--) {
            ordered.add(list.get(i));
        }

        return ordered;
    }

    public List<Models.Draw> getDrawsBetweenIssues(String startIssue, String endIssue, int limit) {
        ArrayList<Models.Draw> list = new ArrayList<Models.Draw>();

        if (startIssue == null
                || endIssue == null
                || startIssue.length() == 0
                || endIssue.length() == 0) {
            return list;
        }

        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    DbHelper.TABLE_DRAWS,
                                    DrawHistoryProjectionR817.COLUMNS,
                                    "dirty=0 AND issue>=? AND issue<=?",
                                    new String[] {startIssue, endIssue},
                                    null,
                                    null,
                                    "issue ASC, open_time ASC, _id ASC",
                                    limit > 0 ? String.valueOf(limit) : null);

            while (c.moveToNext()) {
                list.add(readDraw(c));
            }
        } finally {
            closeCursor(c);
        }

        return list;
    }

    public int countDraws() {
        return countTable(DbHelper.TABLE_DRAWS, "dirty=0", null);
    }

    public int getDrawCount() {
        return countDraws();
    }

    public int getValidDrawCount() {
        return countDraws();
    }

    public int getTodayDrawCount() {
        long start = todayStartMs();
        long end = start + 24L * 60L * 60L * 1000L;
        String todayPrefix = todayIssuePrefix();

        String where = "dirty=0 AND ((open_time>=? AND open_time<?) OR issue LIKE ?)";
        String[] args =
                new String[] {String.valueOf(start), String.valueOf(end), todayPrefix + "%"};

        return countTable(DbHelper.TABLE_DRAWS, where, args);
    }

    public int getDrawRemainTo240() {
        int count = getValidDrawCount();
        int need = AppConfig.WINDOW_240 - count;

        if (need < 0) {
            need = 0;
        }

        return need;
    }

    public boolean isDrawHistoryReady240() {
        return getValidDrawCount() >= AppConfig.WINDOW_240;
    }

    public String getLatestDrawIssue() {
        Models.Draw d = getLatestDraw();
        return d == null ? "" : d.issue;
    }

    public String getFirstDrawIssue() {
        Models.Draw d = getFirstDraw();
        return d == null ? "" : d.issue;
    }

    public long getLatestDrawTime() {
        Models.Draw d = getLatestDraw();
        return d == null ? 0L : d.openTime;
    }

    public long getFirstDrawTime() {
        Models.Draw d = getFirstDraw();
        return d == null ? 0L : d.openTime;
    }

    public int getRecentIssueGapCount(int limit) {
        List<Models.Draw> draws = getRecentDraws(limit);

        if (draws == null || draws.size() < 2) {
            return 0;
        }

        int gaps = 0;

        for (int i = 1; i < draws.size(); i++) {
            Models.Draw prev = draws.get(i - 1);
            Models.Draw cur = draws.get(i);

            long p = parseIssueLong(prev == null ? "" : prev.issue);
            long c = parseIssueLong(cur == null ? "" : cur.issue);

            if (p <= 0L || c <= 0L) {
                continue;
            }

            long diff = Math.abs((long) TargetIssueResolver.distance(prev.issue, cur.issue));

            if (diff > 1L && diff < 10000L) {
                gaps += (int) (diff - 1L);
            }
        }

        return gaps;
    }

    public String getDrawDataProgressText() {
        int total = getValidDrawCount();
        int today = getTodayDrawCount();
        int remain = getDrawRemainTo240();
        int gap = getRecentIssueGapCount(300);

        String latest = getLatestDrawIssue();
        String first = getFirstDrawIssue();

        StringBuilder sb = new StringBuilder();

        sb.append("历史数据：已抓取 ");
        sb.append(total);
        sb.append(" 期");

        if (remain > 0) {
            sb.append("｜距30期受控启动还差 ");
            sb.append(remain);
            sb.append(" 期");
        } else {
            sb.append("｜已满足30期受控启动门槛");
        }

        sb.append("\n今日已存：");
        sb.append(today);
        sb.append(" 期");

        if (latest != null && latest.length() > 0) {
            sb.append("｜最新开奖：");
            sb.append(latest);
        }

        if (first != null && first.length() > 0) {
            sb.append("\n最早开奖：");
            sb.append(first);
        }

        if (gap > 0) {
            sb.append("｜近300期疑似缺口：");
            sb.append(gap);
            sb.append(" 期");
        } else {
            sb.append("｜近300期未发现明显缺口");
        }

        return sb.toString();
    }

    public void updateBackfillStatus(String status, int inserted, String message) {
        long now = System.currentTimeMillis();

        setState("backfill_status", status == null ? "" : status);
        setState("backfill_inserted", String.valueOf(inserted));
        setState("backfill_message", message == null ? "" : message);
        setState("backfill_time", String.valueOf(now));
    }

    public String getBackfillStatusText() {
        String status = getState("backfill_status", "");
        String inserted = getState("backfill_inserted", "0");
        String message = getState("backfill_message", "");
        long time = parseLong(getState("backfill_time", "0"), 0L);

        String text;

        if ("RUNNING".equals(status)) {
            text = "回补状态：回补中";
        } else if ("OK".equals(status)) {
            text = "回补状态：成功，新增 " + inserted + " 期";
        } else if ("SKIP".equals(status)) {
            text = "回补状态：等待下一次检查";
        } else if ("ERROR".equals(status)) {
            text = "回补状态：异常";
        } else {
            text = "回补状态：未开始";
        }

        if (message != null && message.length() > 0) {
            text += "｜" + message;
        }

        if (time > 0L) {
            text += "｜" + formatTime(time);
        }

        return text;
    }

    public long saveDynamicWindowCluster(Models.DynamicWindowCluster c) {
        if (c == null) return -1L;
        try {
            c.recalc();
        } catch (Throwable e) {
        }
        if (c.clusterId == null || c.clusterId.length() == 0)
            c.clusterId = "DCW_AUTO_" + System.currentTimeMillis();
        SQLiteDatabase db = writable();
        ensureDynamicCausalWindowReady(db);
        ContentValues cv = new ContentValues();
        cv.put("cluster_id", c.clusterId);
        cv.put("prototype_key", c.prototypeKey);
        cv.put("prototype_vector", c.prototypeVector);
        cv.put("status", AppConfig.normalizeDynamicClusterStatus(c.status));
        cv.put("sample_count", c.sampleCount);
        cv.put("hit_count", c.hitCount);
        cv.put("miss_count", c.missCount);
        cv.put("recent_sample", c.recentSample);
        cv.put("recent_hit_count", c.recentHitCount);
        cv.put("hit_rate", c.hitRate);
        cv.put("recent_hit_rate", c.recentHitRate);
        cv.put("max_lose_streak", c.maxLoseStreak);
        cv.put("confidence", c.confidence);
        cv.put("volatility", c.volatility);
        cv.put("counter_score", c.counterScore);
        cv.put("prototype_distance", c.prototypeDistance);
        cv.put("version", V20_DYNAMIC_CAUSAL_DBSTORE_VERSION);
        cv.put("reason", c.reason);
        long now = System.currentTimeMillis();
        if (c.createdAt <= 0L) c.createdAt = now;
        c.updatedAt = now;
        cv.put("created_at", c.createdAt);
        cv.put("updated_at", c.updatedAt);
        return insertFiltered(
                db, TABLE_DYNAMIC_WINDOW_CLUSTER, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public ArrayList<Models.DynamicWindowCluster> loadActiveDynamicWindowClusters(int limit) {
        ArrayList<Models.DynamicWindowCluster> list = new ArrayList<Models.DynamicWindowCluster>();
        SQLiteDatabase db = readable();
        ensureDynamicCausalWindowReady(db);
        Cursor c = null;
        try {
            c =
                    db.query(
                            TABLE_DYNAMIC_WINDOW_CLUSTER,
                            null,
                            "status<>? AND status<>?",
                            new String[] {
                                AppConfig.CLUSTER_STATUS_DEAD, AppConfig.CLUSTER_STATUS_FREEZE
                            },
                            null,
                            null,
                            "updated_at DESC, confidence DESC, hit_rate DESC",
                            limit > 0
                                    ? String.valueOf(limit)
                                    : String.valueOf(AppConfig.DYNAMIC_CLUSTER_MAX_ACTIVE));
            while (c != null && c.moveToNext()) {
                Models.DynamicWindowCluster dc = readDynamicWindowCluster(c);
                if (dc != null) list.add(dc);
            }
        } catch (Throwable e) {
        } finally {
            closeCursor(c);
        }
        return list;
    }

    public Models.DynamicWindowCluster getDynamicWindowCluster(String clusterId) {
        if (clusterId == null || clusterId.length() == 0) return null;
        SQLiteDatabase db = readable();
        ensureDynamicCausalWindowReady(db);
        Cursor c = null;
        try {
            c =
                    db.query(
                            TABLE_DYNAMIC_WINDOW_CLUSTER,
                            null,
                            "cluster_id=?",
                            new String[] {clusterId},
                            null,
                            null,
                            null,
                            "1");
            if (c != null && c.moveToFirst()) return readDynamicWindowCluster(c);
        } catch (Throwable e) {
        } finally {
            closeCursor(c);
        }
        return null;
    }

    private Models.DynamicWindowCluster readDynamicWindowCluster(Cursor c) {
        if (c == null) return null;
        Models.DynamicWindowCluster dc = new Models.DynamicWindowCluster();
        dc.clusterId = getString(c, "cluster_id");
        dc.prototypeKey = getString(c, "prototype_key");
        dc.prototypeVector = getString(c, "prototype_vector");
        dc.status = AppConfig.normalizeDynamicClusterStatus(getString(c, "status"));
        dc.sampleCount = getInt(c, "sample_count");
        dc.hitCount = getInt(c, "hit_count");
        dc.missCount = getInt(c, "miss_count");
        dc.recentSample = getInt(c, "recent_sample");
        dc.recentHitCount = getInt(c, "recent_hit_count");
        dc.hitRate = getDouble(c, "hit_rate");
        dc.recentHitRate = getDouble(c, "recent_hit_rate");
        dc.maxLoseStreak = getInt(c, "max_lose_streak");
        dc.confidence = getDouble(c, "confidence");
        dc.volatility = getDouble(c, "volatility");
        dc.counterScore = getDouble(c, "counter_score");
        dc.prototypeDistance = getDouble(c, "prototype_distance");
        dc.createdAt = getLong(c, "created_at");
        dc.updatedAt = getLong(c, "updated_at");
        dc.reason = getString(c, "reason");
        return dc;
    }

    public long saveWindowFeatureSample(
            Models.WindowFeatureVector w,
            String baseIssue,
            String decisionType,
            boolean wouldHit,
            String clusterId,
            String clusterStatus,
            double distance) {
        if (w == null) return -1L;
        try {
            w.rebuildKey();
        } catch (Throwable e) {
        }
        String key =
                "WFS|"
                        + safeDbKey(w.targetIssue)
                        + "|P"
                        + w.position
                        + "|K"
                        + AppConfig.normalizeK3Part(w.n1, w.n2, w.n3)
                        + "|"
                        + safeDbKey(w.vectorKey);
        SQLiteDatabase db = writable();
        ensureDynamicCausalWindowReady(db);
        ContentValues cv = new ContentValues();
        cv.put("sample_key", key);
        cv.put("base_issue", baseIssue);
        cv.put("target_issue", w.targetIssue);
        cv.put("created_at", System.currentTimeMillis());
        cv.put("position", w.position);
        cv.put("n1", w.n1);
        cv.put("n2", w.n2);
        cv.put("n3", w.n3);
        cv.put("k3_mask", w.k3Mask);
        cv.put("vector_key", w.vectorKey);
        cv.put("raw_key", w.rawKey);
        cv.put("cluster_id", clusterId);
        cv.put("cluster_status", AppConfig.normalizeDynamicClusterStatus(clusterStatus));
        cv.put("distance", distance);
        cv.put("would_hit", wouldHit ? 1 : 0);
        cv.put("decision_type", decisionType);
        cv.put("trend_slope12", w.trendSlope12);
        cv.put("trend_slope24", w.trendSlope24);
        cv.put("trend_slope48", w.trendSlope48);
        cv.put("trend_slope96", w.trendSlope96);
        cv.put("trend_slope360", w.trendSlope360);
        cv.put("trend_life", w.trendLife);
        cv.put("trend_fatigue", w.trendFatigue);
        cv.put("omission_pressure", w.omissionPressure);
        cv.put("hot_cold_transition", w.hotColdTransition);
        cv.put("density_score", w.densityScore);
        cv.put("repeat_rhythm", w.repeatRhythm);
        cv.put("odd_even_bias", w.oddEvenBias);
        cv.put("high_low_bias", w.highLowBias);
        cv.put("tail_pressure", w.tailPressure);
        cv.put("neighbor_pressure", w.neighborPressure);
        cv.put("cross_position_flow", w.crossPositionFlow);
        cv.put("cross_position_crowd", w.crossPositionCrowd);
        cv.put("same_number_crowd", w.sameNumberCrowd);
        cv.put("timing_offset", w.timingOffset);
        cv.put("counter_evidence", w.counterEvidence);
        cv.put("third_quality", w.thirdQuality);
        cv.put("summary", w.summary);
        return insertFiltered(db, TABLE_WINDOW_FEATURE_SAMPLE, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public long savePositionNeedSample(Models.PositionNeedState p, int actualNumber) {
        if (p == null) return -1L;
        try {
            p.recalc();
        } catch (Throwable e) {
        }
        String key =
                "PNS|" + safeDbKey(p.targetIssue) + "|P" + p.position + "|" + safeDbKey(p.needKey);
        SQLiteDatabase db = writable();
        ensureDynamicCausalWindowReady(db);
        ContentValues cv = new ContentValues();
        cv.put("sample_key", key);
        cv.put("target_issue", p.targetIssue);
        cv.put("created_at", System.currentTimeMillis());
        cv.put("position", p.position);
        cv.put("actual_number", actualNumber);
        cv.put("need_key", p.needKey);
        cv.put("need_hot_continue", p.needHotContinue);
        cv.put("need_cold_repair", p.needColdRepair);
        cv.put("need_omission_return", p.needOmissionReturn);
        cv.put("need_high_low_balance", p.needHighLowBalance);
        cv.put("need_odd_even_balance", p.needOddEvenBalance);
        cv.put("need_tail_repair", p.needTailRepair);
        cv.put("need_neighbor_relay", p.needNeighborRelay);
        cv.put("need_cross_position_flow", p.needCrossPositionFlow);
        cv.put("need_crowd_release", p.needCrowdRelease);
        cv.put("need_reverse_break", p.needReverseBreak);
        cv.put("need_stable_repeat", p.needStableRepeat);
        cv.put("need_chaos_avoid", p.needChaosAvoid);
        cv.put("total_need_strength", p.totalNeedStrength);
        cv.put("chaos_risk", p.chaosRisk);
        cv.put("main_need_type", p.mainNeedType);
        cv.put("summary", p.summary);
        return insertFiltered(db, TABLE_POSITION_NEED_SAMPLE, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public long saveNumberSupplySample(
            Models.NumberSupplyState n, int actualNumber, boolean label) {
        if (n == null) return -1L;
        try {
            n.recalc();
        } catch (Throwable e) {
        }
        String key =
                "NSS|"
                        + safeDbKey(n.targetIssue)
                        + "|P"
                        + n.position
                        + "|N"
                        + n.number
                        + "|"
                        + safeDbKey(n.supplyKey);
        SQLiteDatabase db = writable();
        ensureDynamicCausalWindowReady(db);
        ContentValues cv = new ContentValues();
        cv.put("sample_key", key);
        cv.put("target_issue", n.targetIssue);
        cv.put("created_at", System.currentTimeMillis());
        cv.put("position", n.position);
        cv.put("number", n.number);
        cv.put("actual_number", actualNumber);
        cv.put("label", label ? 1 : 0);
        cv.put("supply_key", n.supplyKey);
        cv.put("supply_hot_continue", n.supplyHotContinue);
        cv.put("supply_cold_repair", n.supplyColdRepair);
        cv.put("supply_omission_return", n.supplyOmissionReturn);
        cv.put("supply_high_low_balance", n.supplyHighLowBalance);
        cv.put("supply_odd_even_balance", n.supplyOddEvenBalance);
        cv.put("supply_tail_repair", n.supplyTailRepair);
        cv.put("supply_neighbor_relay", n.supplyNeighborRelay);
        cv.put("supply_cross_position_flow", n.supplyCrossPositionFlow);
        cv.put("supply_crowd_release", n.supplyCrowdRelease);
        cv.put("supply_reverse_break", n.supplyReverseBreak);
        cv.put("supply_stable_repeat", n.supplyStableRepeat);
        cv.put("supply_risk", n.supplyRisk);
        cv.put("total_supply_strength", n.totalSupplyStrength);
        cv.put("main_supply_type", n.mainSupplyType);
        cv.put("summary", n.summary);
        return insertFiltered(db, TABLE_NUMBER_SUPPLY_SAMPLE, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public long saveNeedSupplyMatchSample(Models.NeedSupplyMatchResult r) {
        if (r == null) return -1L;
        try {
            r.recalc();
        } catch (Throwable e) {
        }
        SQLiteDatabase db = writable();
        ensureDynamicCausalWindowReady(db);
        ContentValues cv = new ContentValues();
        cv.put("match_key", r.matchKey);
        cv.put("target_issue", r.targetIssue);
        cv.put("created_at", System.currentTimeMillis());
        cv.put("position", r.position);
        cv.put("n1", r.n1);
        cv.put("n2", r.n2);
        cv.put("n3", r.n3);
        cv.put("k3_mask", r.k3Mask);
        cv.put("main_role_number", r.mainRoleNumber);
        cv.put("assist_role_number", r.assistRoleNumber);
        cv.put("defense_role_number", r.defenseRoleNumber);
        cv.put("main_role", r.mainRole);
        cv.put("assist_role", r.assistRole);
        cv.put("defense_role", r.defenseRole);
        cv.put("match_score", r.matchScore);
        cv.put("main_role_score", r.mainRoleScore);
        cv.put("assist_role_score", r.assistRoleScore);
        cv.put("defense_role_score", r.defenseRoleScore);
        cv.put("third_quality", r.thirdQuality);
        cv.put("role_diversity", r.roleDiversity);
        cv.put("supply_risk", r.supplyRisk);
        cv.put("why_match", r.whyMatch);
        cv.put("why_not_match", r.whyNotMatch);
        cv.put("summary", r.summary);
        return insertFiltered(
                db, TABLE_NEED_SUPPLY_MATCH_SAMPLE, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public long saveCounterfactualReasonSample(
            Models.CounterfactualReasonResult r, String clusterId, boolean wouldHit) {
        if (r == null) return -1L;
        try {
            r.recalc();
        } catch (Throwable e) {
        }
        if (r.reasonKey == null || r.reasonKey.length() == 0)
            r.reasonKey =
                    AppConfig.buildCounterfactualReasonKey(
                            r.targetIssue, r.position, r.n1, r.n2, r.n3, clusterId);
        SQLiteDatabase db = writable();
        ensureDynamicCausalWindowReady(db);
        ContentValues cv = new ContentValues();
        cv.put("reason_key", r.reasonKey);
        cv.put("target_issue", r.targetIssue);
        cv.put("created_at", System.currentTimeMillis());
        cv.put("position", r.position);
        cv.put("n1", r.n1);
        cv.put("n2", r.n2);
        cv.put("n3", r.n3);
        cv.put("k3_mask", r.k3Mask);
        cv.put("cluster_id", clusterId);
        cv.put("position_rank", r.positionRank);
        cv.put("k3_rank", r.k3Rank);
        cv.put("rank_score", r.rankScore);
        cv.put("why_not_risk", r.whyNotRisk);
        cv.put("mirror_miss_similarity", r.mirrorMissSimilarity);
        cv.put("cause_score", r.causeScore);
        cv.put("counter_score", r.counterScore);
        cv.put("would_hit", wouldHit ? 1 : 0);
        cv.put("why_this_position", r.whyThisPosition);
        cv.put("why_these_numbers", r.whyTheseNumbers);
        cv.put("why_not_other_position", r.whyNotOtherPosition);
        cv.put("why_not_other_numbers", r.whyNotOtherNumbers);
        cv.put("summary", r.summary);
        return insertFiltered(
                db, TABLE_COUNTERFACTUAL_REASON_SAMPLE, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public long saveCauseAblationSample(Models.CauseAblationResult r) {
        if (r == null) return -1L;
        try {
            r.recalc();
        } catch (Throwable e) {
        }
        if (r.ablationKey == null || r.ablationKey.length() == 0)
            r.ablationKey = "ABL|" + safeDbKey(r.clusterId) + "|" + System.currentTimeMillis();
        SQLiteDatabase db = writable();
        ensureDynamicCausalWindowReady(db);
        ContentValues cv = new ContentValues();
        cv.put("ablation_key", r.ablationKey);
        cv.put("cluster_id", r.clusterId);
        cv.put("created_at", System.currentTimeMillis());
        cv.put("sample_count", r.sampleCount);
        cv.put("omission_drop", r.omissionDrop);
        cv.put("tail_drop", r.tailDrop);
        cv.put("neighbor_drop", r.neighborDrop);
        cv.put("cross_position_drop", r.crossPositionDrop);
        cv.put("need_supply_drop", r.needSupplyDrop);
        cv.put("third_quality_drop", r.thirdQualityDrop);
        cv.put("total_ablation_score", r.totalAblationScore);
        cv.put("false_cause_count", r.falseCauseCount);
        cv.put("false_cause_summary", r.falseCauseSummary);
        cv.put("summary", r.summary);
        return insertFiltered(db, TABLE_CAUSE_ABLATION_SAMPLE, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public long saveMissReasonSample(Models.MissReasonResult r) {
        if (r == null) return -1L;
        try {
            r.recalc();
        } catch (Throwable e) {
        }
        SQLiteDatabase db = writable();
        ensureDynamicCausalWindowReady(db);
        ContentValues cv = new ContentValues();
        cv.put("miss_key", r.missKey);
        cv.put("target_issue", r.targetIssue);
        cv.put("created_at", System.currentTimeMillis());
        cv.put("position", r.position);
        cv.put("n1", r.n1);
        cv.put("n2", r.n2);
        cv.put("n3", r.n3);
        cv.put("k3_mask", r.k3Mask);
        cv.put("actual_number", r.actualNumber);
        cv.put("miss_type", r.missType);
        cv.put("cluster_id", r.clusterId);
        cv.put("window_wrong_score", r.windowWrongScore);
        cv.put("position_wrong_score", r.positionWrongScore);
        cv.put("main_number_wrong_score", r.mainNumberWrongScore);
        cv.put("third_drag_score", r.thirdDragScore);
        cv.put("counter_ignored_score", r.counterIgnoredScore);
        cv.put("detail", r.detail);
        cv.put("next_advice", r.nextAdvice);
        return insertFiltered(db, TABLE_MISS_REASON_SAMPLE, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public long saveAdaptiveJudgeSample(Models.AdaptiveJudgeResult r) {
        if (r == null) return -1L;
        try {
            r.recalc();
        } catch (Throwable e) {
        }
        String key =
                "ADJ|"
                        + safeDbKey(r.targetIssue)
                        + "|"
                        + safeDbKey(r.clusterId)
                        + "|"
                        + safeDbKey(r.action);
        SQLiteDatabase db = writable();
        ensureDynamicCausalWindowReady(db);
        ContentValues cv = new ContentValues();
        cv.put("judge_key", key);
        cv.put("target_issue", r.targetIssue);
        cv.put("created_at", System.currentTimeMillis());
        cv.put("candidate_count", r.candidateCount);
        cv.put("cluster_id", r.clusterId);
        cv.put("cluster_status", AppConfig.normalizeDynamicClusterStatus(r.clusterStatus));
        cv.put("status", AppConfig.normalizeDynamicClusterStatus(r.status));
        cv.put("action", r.action);
        cv.put("domain", r.domain);
        cv.put("cluster_hit_rate", r.clusterHitRate);
        cv.put("cluster_recent_hit_rate", r.clusterRecentHitRate);
        cv.put("cluster_sample", r.clusterSample);
        cv.put("cluster_confidence", r.clusterConfidence);
        cv.put("cause_score", r.causeScore);
        cv.put("counter_score", r.counterScore);
        cv.put("promote_score", r.promoteScore);
        cv.put("freeze_score", r.freezeScore);
        cv.put("reason", r.reason);
        cv.put("version", r.version);
        putDynamicCausalState(
                db,
                AppConfig.STATE_ADAPTIVE_JUDGE_CANDIDATE_COUNT,
                String.valueOf(r.candidateCount),
                System.currentTimeMillis());
        putDynamicCausalState(
                db,
                AppConfig.STATE_ADAPTIVE_JUDGE_REALTIME_SUMMARY,
                r.reason == null ? "" : r.reason,
                System.currentTimeMillis());
        return insertFiltered(db, TABLE_ADAPTIVE_JUDGE_SAMPLE, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public long savePromotionDomainSnapshot(Models.PromotionDomainState p) {
        if (p == null) return -1L;
        try {
            p.recalc();
        } catch (Throwable e) {
        }
        if (p.domainKey == null || p.domainKey.length() == 0)
            p.domainKey = "PROMO|" + safeDbKey(p.clusterId);
        SQLiteDatabase db = writable();
        ensureDynamicCausalWindowReady(db);
        ContentValues cv = new ContentValues();
        cv.put("domain_key", p.domainKey);
        cv.put("cluster_id", p.clusterId);
        cv.put("status", AppConfig.normalizeDynamicClusterStatus(p.status));
        cv.put("sample_count", p.sampleCount);
        cv.put("hit_count", p.hitCount);
        cv.put("recent_sample", p.recentSample);
        cv.put("recent_hit_count", p.recentHitCount);
        cv.put("hit_rate", p.hitRate);
        cv.put("recent_hit_rate", p.recentHitRate);
        cv.put("max_lose_streak", p.maxLoseStreak);
        cv.put("confidence", p.confidence);
        cv.put("action", p.action);
        cv.put("reason", p.reason);
        cv.put("updated_at", System.currentTimeMillis());
        return insertFiltered(
                db, TABLE_PROMOTION_DOMAIN_SNAPSHOT, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void updateDynamicCausalState(String key, String value) {
        SQLiteDatabase db = writable();
        ensureDynamicCausalWindowReady(db);
        putDynamicCausalState(db, key, value, System.currentTimeMillis());
    }

    public String getDynamicCausalState(String key, String def) {
        if (key == null || key.length() == 0) return def;
        SQLiteDatabase db = readable();
        ensureDynamicCausalWindowReady(db);
        Cursor c = null;
        try {
            c =
                    db.query(
                            TABLE_DYNAMIC_CAUSAL_STATE,
                            new String[] {"state_value"},
                            "state_key=?",
                            new String[] {key},
                            null,
                            null,
                            null,
                            "1");
            if (c != null && c.moveToFirst()) {
                String v = getString(c, "state_value");
                return v == null ? def : v;
            }
        } catch (Throwable e) {
        } finally {
            closeCursor(c);
        }
        return def;
    }

    private String safeDbKey(String s) {
        if (s == null || s.length() == 0) return "NA";
        return AppConfig.safeKeyPart(s);
    }

    public long saveDecision(Models.Decision d) {
        return saveDecisionInternal(d, false);
    }

    public long replaceDecisionFull(Models.Decision d) {
        return saveDecisionInternal(d, true);
    }

    /**
     * Package-private typed bridge used only inside V50AtomicDecisionLedgerEngine's already-open
     * transaction. It persists the compatibility Decision and, for an authorized BET, its Trade
     * without reflection, nested transactions, rescue logic, or post-commit repair.
     */
    static final class V50AtomicLegacyWrite {
        final long decisionRowId;
        final long tradeRowId;
        final boolean idempotent;

        V50AtomicLegacyWrite(long decisionRowId, long tradeRowId, boolean idempotent) {
            this.decisionRowId = decisionRowId;
            this.tradeRowId = tradeRowId;
            this.idempotent = idempotent;
        }
    }

    V50AtomicLegacyWrite persistV50DecisionAndTrade(SQLiteDatabase db, Models.Decision decision) {
        throw new IllegalStateException("CP4_LEGACY_TRADE_WRITER_RETIRED:persistV50DecisionAndTrade");
    }

    private static final class ExistingV50Legacy {
        boolean exists;
        long rowId;
        String candidateHash = "";
        String contractHash = "";
        String action = "";
        String authorizationId = "";
    }

    private ExistingV50Legacy readExistingV50Legacy(
            SQLiteDatabase db, String table, String targetIssue) {
        ExistingV50Legacy out = new ExistingV50Legacy();
        Cursor cursor = null;
        try {
            cursor =
                    db.query(
                            table,
                            new String[] {
                                "_id",
                                "candidate_hash",
                                "v46_final_contract_hash",
                                DbHelper.TABLE_DECISIONS.equals(table) ? "decision" : "action",
                                "authorization_id"
                            },
                            "target_issue=?",
                            new String[] {targetIssue},
                            null,
                            null,
                            "_id ASC",
                            "2");
            if (!cursor.moveToFirst()) return out;
            out.exists = true;
            out.rowId = cursor.getLong(0);
            out.candidateHash = safeV50Text(cursor.getString(1));
            out.contractHash = safeV50Text(cursor.getString(2));
            out.action = AppConfig.normalizeDecision(cursor.getString(3));
            out.authorizationId = safeV50Text(cursor.getString(4));
            if (cursor.moveToNext()) {
                throw new IllegalStateException("V50_ATOMIC_DUPLICATE_LEGACY_ROWS");
            }
            return out;
        } finally {
            closeCursor(cursor);
        }
    }

    private boolean sameV50Legacy(ExistingV50Legacy existing, Models.Decision incoming) {
        return existing != null
                && incoming != null
                && safeV50Text(incoming.candidateHash).equals(existing.candidateHash)
                && safeV50Text(incoming.v46ContractHash).equals(existing.contractHash)
                && AppConfig.normalizeDecision(incoming.decision).equals(existing.action)
                && safeV50Text(incoming.authorizationId).equals(existing.authorizationId);
    }

    private int countOtherOpenV50Trades(SQLiteDatabase db, String targetIssue) {
        Cursor cursor = null;
        try {
            cursor =
                    db.rawQuery(
                            "SELECT COUNT(*) FROM "
                                    + DbHelper.TABLE_TRADES
                                    + " WHERE settled=0 AND target_issue<>?"
                                    + " AND action=? AND stat_scope=?",
                            new String[] {
                                targetIssue, AppConfig.ACTION_BET, AppConfig.STAT_SCOPE_REAL_TRADE
                            });
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            closeCursor(cursor);
        }
    }

    private ContentValues v50DecisionValues(Models.Decision d) {
        ContentValues cv = v50CommonValues(d);
        cv.put("decision", d.decision);
        cv.put("action", d.action);
        cv.put("trade_id", d.tradeId);
        cv.put("block_code", safeV50Text(d.blockCode));
        cv.put("reason_text", safeV50Text(d.reasonText));
        cv.put("data_ok", d.dataOk);
        cv.put("realtime_cost_ms", d.realtimeCostMs);
        return cv;
    }

    private ContentValues v50TradeValues(Models.Decision d, long decisionRowId) {
        ContentValues cv = v50CommonValues(d);
        cv.put("decision_id", decisionRowId);
        cv.put("action", AppConfig.ACTION_BET);
        cv.put("bet_time", d.createdAt);
        cv.put("settle_time", 0L);
        return cv;
    }

    private ContentValues v50CommonValues(Models.Decision d) {
        ContentValues cv = new ContentValues();
        cv.put("base_issue", safeV50Text(d.baseIssue));
        cv.put("target_issue", safeV50Text(d.targetIssue));
        cv.put("created_at", d.createdAt);
        cv.put("settled_at", 0L);
        cv.put("position", d.position);
        cv.put("n1", d.n1);
        cv.put("n2", d.n2);
        cv.put("n3", d.n3);
        cv.put("k3_mask", d.k3Mask);
        cv.put("raw_p", d.rawP);
        cv.put("model_p", d.modelP);
        cv.put("calibrated_p", d.calibratedP);
        cv.put("final_p", d.finalP);
        cv.put("dynamic_gate", d.dynamicGate);
        cv.put("edge", d.edge);
        cv.put("raw_rank_score", d.rawRankScore);
        cv.put("calibrated_hit_probability", d.calibratedHitProbability);
        cv.put("honest_probability", d.honestProbability);
        cv.put("state_key", safeV50Text(d.stateKey));
        cv.put("reason_summary", safeV50Text(d.reasonSummary));
        cv.put("result", AppConfig.RESULT_WAIT);
        cv.put("settled", 0);
        cv.put("actual_number", 0);
        cv.put("hit", 0);
        cv.put("pnl", 0.0d);
        cv.put("source_type", Models.SOURCE_TYPE_REALTIME);
        cv.put("is_realtime_real", AppConfig.DECISION_BET.equals(d.decision) ? 1 : 0);
        cv.put("stat_scope", d.statScope);
        cv.put("probability_mode", safeV50Text(d.probabilityMode));
        cv.put("contract_decision_id", safeV50Text(d.contractDecisionId));
        cv.put("candidate_hash", safeV50Text(d.candidateHash));
        cv.put("snapshot_max_issue", safeV50Text(d.snapshotMaxIssue));
        cv.put("trained_until_issue", safeV50Text(d.trainedUntilIssue));
        cv.put("build_id", safeV50Text(d.buildId));
        cv.put("cohort_id", safeV50Text(d.cohortId));
        cv.put("schema_version", safeV50Text(d.schemaVersion));
        cv.put("feature_schema_version", safeV50Text(d.featureSchemaVersion));
        cv.put("ranking_version", safeV50Text(d.rankingVersion));
        cv.put("calibration_version", safeV50Text(d.calibrationVersion));
        cv.put("gate_policy_version", safeV50Text(d.gatePolicyVersion));
        cv.put("pipeline_version", safeV50Text(d.pipelineVersion));
        cv.put("contract_valid", 1);
        cv.put("contract_status", PredictionContractEngine.STATUS_VALID);
        cv.put("authorization_id", safeV50Text(d.authorizationId));
        cv.put("authorization_valid", d.authorizationValid);
        cv.put("authorized_mode", safeV50Text(d.authorizedMode));
        cv.put("authorization_reject_code", safeV50Text(d.authorizationRejectCode));
        cv.put("authorized_at", d.authorizedAt);
        cv.put("authorization_probability", d.authorizationProbability);
        cv.put("authorization_gate", d.authorizationGate);
        cv.put("authorization_edge", d.authorizationEdge);
        cv.put("lane_candidate", safeV50Text(d.laneCandidate));
        cv.put("v46_event_probability", d.v46EventProbability);
        cv.put("v46_action_lcb", d.v46ActionLcb);
        cv.put("v46_probability_uncertainty", d.v46ProbabilityUncertainty);
        cv.put("v46_expected_value", d.v46ExpectedValue);
        cv.put("v46_final_rank", d.v46FinalRank);
        cv.put("v46_formal_candidate_count", d.v46FormalCandidateCount);
        cv.put("v46_final_contract_hash", safeV50Text(d.v46ContractHash));
        cv.put("v46_lane", safeV50Text(d.v46Lane));
        cv.put("final_decision_bet_domain", safeV50Text(d.finalDecisionBetDomain));
        return cv;
    }

    private static String safeV50Text(String value) {
        return value == null ? "" : value;
    }

    private long saveDecisionInternal(Models.Decision d, boolean forceReplace) {
        try (DatabaseCoordinator.WriterGuardCP3 cp3WriterGuard1 = DatabaseCoordinator.acquireWriterGuardCP3()) {
            if (d == null || d.targetIssue == null || d.targetIssue.length() == 0) {
                return -1L;
            }

            normalizeDecisionBeforeSave(d);
            if (AppConfig.DECISION_BET.equals(d.decision)
                    && !DecisionAuthorizationEngine.isDecisionAuthorized(d)) {
                DecisionAuthorizationEngine.downgradeUnauthorized(
                        d, "DBSTORE_AUTHORIZATION_REQUIRED");
                normalizeDecisionBeforeSave(d);
            }

            SQLiteDatabase db = writable();
            ensureRuntimeExtraTablesOnce(db);

            long resultId = -1L;
            Models.Decision stateDecision = d;

            db.beginTransaction();

            try {
                Models.Decision old = getBestDecisionByTargetIssue(db, d.targetIssue);

                if (old != null && old.id > 0L) {
                    boolean keepOld = false;

                    if (isValidBetDecision(old) && !AppConfig.DECISION_BET.equals(d.decision)) {
                        keepOld = true;
                    }

                    if (!keepOld && !forceReplace && shouldKeepOldDecision(old, d)) {
                        keepOld = true;
                    }

                    if (!keepOld
                            && forceReplace
                            && isQuickObservationDecision(d)
                            && !isQuickObservationDecision(old)) {
                        keepOld = true;
                    }

                    if (keepOld) {
                        cleanupDuplicateDecisionsForTarget(db, old.targetIssue, old.id);
                        resultId = old.id;
                        stateDecision = old;
                    } else {
                        d.id = old.id;
                        ContentValues cv = decisionToValues(d);

                        int updated =
                                updateFiltered(
                                        db,
                                        DbHelper.TABLE_DECISIONS,
                                        cv,
                                        "_id=?",
                                        new String[] {String.valueOf(old.id)});

                        if (updated > 0) {
                            resultId = old.id;
                        } else {
                            resultId =
                                    insertFiltered(
                                            db,
                                            DbHelper.TABLE_DECISIONS,
                                            cv,
                                            SQLiteDatabase.CONFLICT_NONE);
                        }

                        stateDecision = d;
                    }
                } else {
                    ContentValues cv = decisionToValues(d);
                    resultId =
                            insertFiltered(
                                    db, DbHelper.TABLE_DECISIONS, cv, SQLiteDatabase.CONFLICT_NONE);
                    stateDecision = d;
                }

                if (resultId > 0L) {
                    stateDecision.id = resultId;
                    cleanupDuplicateDecisionsForTarget(db, d.targetIssue, resultId);
                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

            if (resultId > 0L) {
                updateDecisionStateAfterSave(stateDecision);
                ensureTradeForRealBetDecision(stateDecision, resultId);
            }

            return resultId;
        }
    }

    private String buildFieldChainSummary(Object obj) {
        if (obj == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Teacher=");
        sb.append(
                getOptionalStringField(
                        obj,
                        "teacherMatrixStatus",
                        getOptionalStringField(obj, "teacherStatus", "")));
        sb.append(" Policy=");
        sb.append(getOptionalStringField(obj, "teacherOrdinaryBetPolicy", ""));
        sb.append(" V20=");
        sb.append(
                getOptionalStringField(
                        obj, "v20Status", getOptionalStringField(obj, "dynamicStatus", "")));
        sb.append(" Cover=");
        sb.append(getOptionalDoubleField(obj, "coverageQualityScore", 0.0d));
        sb.append("/");
        sb.append(getOptionalDoubleField(obj, "k3CoverageRisk", 0.0d));
        sb.append(" Third=");
        sb.append(getOptionalDoubleField(obj, "thirdNumberQualityScore", 0.0d));
        sb.append(" Reject=");
        sb.append(getOptionalStringField(obj, "finalDecisionRejectCode", ""));
        return sb.toString();
    }

    private void updateDecisionStateAfterSave(Models.Decision d) {
        if (d == null || d.targetIssue == null || d.targetIssue.length() == 0) {
            return;
        }

        setState("last_decision", d.decision == null ? "" : d.decision);
        setState("last_decision_target_issue", d.targetIssue);
        setState("last_decision_summary", d.reasonSummary == null ? "" : d.reasonSummary);
        setState("decision_updated_at", String.valueOf(System.currentTimeMillis()));
        setState("field_chain_dbstore_version", V20_FIELD_CHAIN_DBSTORE_VERSION);
        setState("field_chain_last_decision_summary", buildFieldChainSummary(d));
        setState("target_issue", d.targetIssue);

        if (d.baseIssue != null && d.baseIssue.length() > 0) {
            setState("current_issue", d.baseIssue);
        }
    }

    private void normalizeDecisionBeforeSave(Models.Decision d) {
        if (d == null) {
            return;
        }
        if (V50BuildManifest.COHORT_ID.equals(d.cohortId == null ? "" : d.cohortId)) {
            normalizeV50DecisionBeforeSave(d);
            return;
        }

        try {
            d.normalize();
        } catch (Throwable e) {
        }

        if (d.createdAt <= 0L) {
            d.createdAt = System.currentTimeMillis();
        }

        d.decision = AppConfig.normalizeDecision(d.decision);
        d.action = d.decision;

        if (d.k3Mask <= 0 && AppConfig.isValidK3Numbers(d.n1, d.n2, d.n3)) {
            d.k3Mask = Models.buildK3Mask(d.n1, d.n2, d.n3);
        }

        d.result = AppConfig.normalizeResult(d.result);

        if (d.result == null || d.result.length() == 0) {
            d.result = AppConfig.RESULT_WAIT;
        }

        syncFieldChainAliases(d);
        normalizeClosedLoopEvidenceAliases(d);
        ensureSafePreDrawPromotionDomain(d);
        d.statScope = AppConfig.normalizeStatScopeForDecision(d.decision, d.statScope);

        if (!AppConfig.DECISION_BET.equals(d.decision)) {
            if (AppConfig.DECISION_SHADOW.equals(d.decision)) {
                d.statScope = AppConfig.STAT_SCOPE_SHADOW;
            } else {
                d.statScope = AppConfig.STAT_SCOPE_HOLD;
            }
        }

        if (d.edge == 0.0d && d.finalP > 0.0d && d.dynamicGate > 0.0d) {
            d.edge = d.finalP - d.dynamicGate;
        }

        if (d.reasonText == null || d.reasonText.length() == 0) {
            d.reasonText = d.reasonSummary == null ? "" : d.reasonSummary;
        }
    }

    private void normalizeV50DecisionBeforeSave(Models.Decision d) {
        if (d.createdAt <= 0L) d.createdAt = System.currentTimeMillis();
        d.decision = AppConfig.normalizeDecision(d.decision);
        d.action = d.decision;
        d.k3Mask = Models.buildK3Mask(d.n1, d.n2, d.n3);
        d.rawP = AppConfig.clampProbability(d.rawP);
        d.modelP = AppConfig.clampProbability(d.modelP);
        d.calibratedP = AppConfig.clampProbability(d.calibratedP);
        d.finalP = AppConfig.clampProbability(d.finalP);
        d.dynamicGate = AppConfig.clampGate(d.dynamicGate);
        d.edge = d.finalP - d.dynamicGate;
        d.result = AppConfig.normalizeResult(d.result);
        if (d.result == null || d.result.length() == 0) {
            d.result = AppConfig.RESULT_WAIT;
        }
        d.sourceType = Models.SOURCE_TYPE_REALTIME;
        d.isRealtimeReal = AppConfig.DECISION_BET.equals(d.decision) ? 1 : 0;
        d.statScope =
                AppConfig.DECISION_BET.equals(d.decision)
                        ? AppConfig.STAT_SCOPE_REAL_TRADE
                        : (AppConfig.DECISION_SHADOW.equals(d.decision)
                                ? AppConfig.STAT_SCOPE_SHADOW
                                : AppConfig.STAT_SCOPE_HOLD);
        if (d.reasonText == null || d.reasonText.length() == 0) {
            d.reasonText = d.reasonSummary == null ? "" : d.reasonSummary;
        }
    }

    private boolean shouldKeepOldDecision(Models.Decision oldDecision, Models.Decision incoming) {
        if (oldDecision == null || incoming == null) {
            return false;
        }

        if (isValidBetDecision(oldDecision) && !AppConfig.DECISION_BET.equals(incoming.decision)) {
            return true;
        }

        if (isQuickObservationDecision(oldDecision) && !isQuickObservationDecision(incoming)) {
            return false;
        }

        if (!isQuickObservationDecision(oldDecision) && isQuickObservationDecision(incoming)) {
            return true;
        }

        boolean oldWeak = isWeakPlaceholderDecision(oldDecision);
        boolean newWeak = isWeakPlaceholderDecision(incoming);

        if (oldWeak && !newWeak) {
            return false;
        }

        if (!oldWeak && newWeak) {
            return true;
        }

        if (!AppConfig.DECISION_BET.equals(oldDecision.decision)
                && AppConfig.DECISION_BET.equals(incoming.decision)) {
            return false;
        }

        int oldPriority = decisionPriority(oldDecision);
        int newPriority = decisionPriority(incoming);

        if (oldPriority > newPriority) {
            return true;
        }

        if (oldPriority < newPriority) {
            return false;
        }

        return oldDecision.createdAt > incoming.createdAt && oldDecision.finalP >= incoming.finalP;
    }

    private int decisionPriority(Models.Decision d) {
        if (d == null) {
            return 0;
        }

        d.decision = AppConfig.normalizeDecision(d.decision);

        if (isQuickObservationDecision(d)) {
            return 2;
        }

        if (isWeakPlaceholderDecision(d)) {
            return 1;
        }

        if (AppConfig.DECISION_BET.equals(d.decision)) {
            if (isValidBetDecision(d)) {
                return 100;
            }
            return 80;
        }

        if (AppConfig.DECISION_SHADOW.equals(d.decision)) {
            if (Models.isValidK3Mask(d.k3Mask) && d.finalP > 0.0d) {
                return 60;
            }
            return 30;
        }

        if (AppConfig.DECISION_HOLD.equals(d.decision)) {
            if (d.finalP > 0.0d || d.dynamicGate > 0.0d || AppConfig.isValidPosition(d.position)) {
                return 40;
            }
            return 20;
        }

        return 10;
    }

    private boolean isValidBetDecision(Models.Decision d) {
        if (d == null) {
            return false;
        }

        d.decision = AppConfig.normalizeDecision(d.decision);

        if (!AppConfig.DECISION_BET.equals(d.decision)) {
            return false;
        }

        if (!AppConfig.isValidPosition(d.position)) {
            return false;
        }

        if (d.k3Mask <= 0 && AppConfig.isValidK3Numbers(d.n1, d.n2, d.n3)) {
            d.k3Mask = Models.buildK3Mask(d.n1, d.n2, d.n3);
        }

        if (!AppConfig.isValidNumber(d.n1)
                || !AppConfig.isValidNumber(d.n2)
                || !AppConfig.isValidNumber(d.n3)) {
            return false;
        }

        if (!Models.isValidK3Mask(d.k3Mask)) {
            return false;
        }

        try {
            if (!d.hasLegalPlayRule()) {
                return false;
            }
        } catch (Throwable e) {
        }

        /* DbStore不创造BET，只验证DecisionGate签发的不可变授权。 */
        return DecisionAuthorizationEngine.isDecisionAuthorized(d);
    }

    private boolean isWeakPlaceholderDecision(Models.Decision d) {
        if (d == null) {
            return true;
        }

        if (isQuickObservationDecision(d)) {
            return true;
        }

        String decision = AppConfig.normalizeDecision(d.decision);
        if (AppConfig.DECISION_BET.equals(decision)
                && AppConfig.isValidPosition(d.position)
                && Models.isValidK3Mask(d.k3Mask)) {
            return false;
        }
        String text = "";

        if (d.reasonSummary != null) text += d.reasonSummary;
        if (d.reasonText != null) text += " " + d.reasonText;
        if (d.deepReason != null) text += " " + d.deepReason;
        if (d.learningSummary != null) text += " " + d.learningSummary;
        if (d.selfDiagnosisText != null) text += " " + d.selfDiagnosisText;
        if (d.blockCode != null) text += " " + d.blockCode;

        if (text.indexOf("临时") >= 0
                || text.indexOf("占位") >= 0
                || text.indexOf("正在生成") >= 0
                || text.indexOf("保护性HOLD") >= 0
                || text.indexOf("未生成有效决策") >= 0
                || text.indexOf("分析线程未生成") >= 0
                || text.indexOf("等待实时分析") >= 0
                || text.indexOf("看门狗") >= 0
                || text.indexOf("快速观察") >= 0
                || text.indexOf("只做观察") >= 0
                || text.indexOf("兜底") >= 0) {
            return true;
        }

        if (AppConfig.DECISION_HOLD.equals(decision)
                && d.finalP <= 0.0001d
                && d.position <= 0
                && !Models.isValidK3Mask(d.k3Mask)) {
            return true;
        }

        return false;
    }

    private boolean isQuickObservationDecision(Models.Decision d) {
        if (d == null) {
            return false;
        }

        String text = "";

        if (d.shapeKey != null) text += d.shapeKey;
        if (d.probabilityMode != null) text += " " + d.probabilityMode;
        if (d.backtestMode != null) text += " " + d.backtestMode;
        if (d.reasonSummary != null) text += " " + d.reasonSummary;
        if (d.reasonText != null) text += " " + d.reasonText;
        if (d.deepReason != null) text += " " + d.deepReason;
        if (d.learningSummary != null) text += " " + d.learningSummary;
        if (d.selfDiagnosisText != null) text += " " + d.selfDiagnosisText;

        if (text.indexOf("QUICK_OBSERVE") >= 0
                || text.indexOf("QUICK_") >= 0
                || text.indexOf("快速观察") >= 0
                || text.indexOf("只做观察") >= 0
                || text.indexOf("不计真实BET") >= 0
                || text.indexOf("不作为真实BET") >= 0
                || text.indexOf("兜底") >= 0) {
            return true;
        }

        return false;
    }

    private Models.Decision getBestDecisionByTargetIssue(SQLiteDatabase db, String targetIssue) {
        if (db == null || targetIssue == null || targetIssue.length() == 0) {
            return null;
        }

        Cursor c = null;
        Models.Decision best = null;
        Models.Decision fallback = null;

        try {
            c =
                    db.query(
                            DbHelper.TABLE_DECISIONS,
                            null,
                            "target_issue=?",
                            new String[] {targetIssue},
                            null,
                            null,
                            "created_at DESC, _id DESC",
                            "30");

            while (c.moveToNext()) {
                Models.Decision d = readDecision(c);

                if (fallback == null) {
                    fallback = d;
                }

                if (best == null) {
                    best = d;
                    continue;
                }

                int p1 = decisionPriority(d);
                int p2 = decisionPriority(best);

                if (p1 > p2) {
                    best = d;
                } else if (p1 == p2 && d.createdAt > best.createdAt) {
                    best = d;
                }
            }
        } finally {
            closeCursor(c);
        }

        if (best != null) {
            return best;
        }

        return fallback;
    }

    private void cleanupDuplicateDecisionsForTarget(
            SQLiteDatabase db, String targetIssue, long keepId) {
        if (db == null || targetIssue == null || targetIssue.length() == 0 || keepId <= 0L) {
            return;
        }

        try {
            db.delete(
                    DbHelper.TABLE_DECISIONS,
                    "target_issue=? AND _id<>?",
                    new String[] {targetIssue, String.valueOf(keepId)});
        } catch (Throwable e) {
        }
    }

    public int deleteDecisionByTargetIssue(String targetIssue) {
        if (targetIssue == null || targetIssue.length() == 0) {
            return 0;
        }

        try {
            Models.Trade t = getTradeByTargetIssue(targetIssue);
            if (t != null && t.id > 0L) {
                return deleteHoldOrShadowDecisionByTargetIssue(targetIssue);
            }
        } catch (Throwable e) {
        }

        return writable()
                .delete(
                        DbHelper.TABLE_DECISIONS,
                        "target_issue=? AND decision<>?",
                        new String[] {targetIssue, AppConfig.DECISION_BET});
    }

    public int deleteHoldOrShadowDecisionByTargetIssue(String targetIssue) {
        if (targetIssue == null || targetIssue.length() == 0) {
            return 0;
        }

        return writable()
                .delete(
                        DbHelper.TABLE_DECISIONS,
                        "target_issue=? AND decision<>?",
                        new String[] {targetIssue, AppConfig.DECISION_BET});
    }

    private void putDynamicCausalValues(ContentValues cv, Object o) {
        if (cv == null || o == null) return;
        String[][] textMap = dynamicCausalTextMap();
        for (int i = 0; i < textMap.length; i++)
            putStringField(cv, o, textMap[i][0], textMap[i][1]);
        String[][] intMap = dynamicCausalIntMap();
        for (int i = 0; i < intMap.length; i++) putIntField(cv, o, intMap[i][0], intMap[i][1]);
        String[][] doubleMap = dynamicCausalDoubleMap();
        for (int i = 0; i < doubleMap.length; i++)
            putDoubleField(cv, o, doubleMap[i][0], doubleMap[i][1]);
    }

    private void readDynamicCausalFields(Cursor c, Object o) {
        if (c == null || o == null) return;
        String[][] textMap = dynamicCausalTextMap();
        for (int i = 0; i < textMap.length; i++)
            setStringField(o, textMap[i][1], getString(c, textMap[i][0]));
        String[][] intMap = dynamicCausalIntMap();
        for (int i = 0; i < intMap.length; i++)
            setIntField(o, intMap[i][1], getInt(c, intMap[i][0]));
        String[][] doubleMap = dynamicCausalDoubleMap();
        for (int i = 0; i < doubleMap.length; i++)
            setDoubleField(o, doubleMap[i][1], getDouble(c, doubleMap[i][0]));
        try {
            Models.normalizeDynamicCausalBridgeFields(o);
        } catch (Throwable e) {
        }
    }

    private String[][] dynamicCausalTextMap() {
        return new String[][] {
            {"dynamic_cluster_id", "dynamicClusterId"},
            {"dynamic_cluster_status", "dynamicClusterStatus"},
            {"dynamic_cluster_prototype_key", "dynamicClusterPrototypeKey"},
            {"dynamic_cluster_feature_key", "dynamicClusterFeatureKey"},
            {"dynamic_cluster_version", "dynamicClusterVersion"},
            {"dynamic_cluster_reason", "dynamicClusterReason"},
            {"position_need_key", "positionNeedKey"},
            {"position_need_summary", "positionNeedSummary"},
            {"number_supply_key", "numberSupplyKey"},
            {"number_supply_summary", "numberSupplySummary"},
            {"need_supply_match_key", "needSupplyMatchKey"},
            {"need_supply_summary", "needSupplySummary"},
            {"k3_role_main", "k3RoleMain"},
            {"k3_role_assist", "k3RoleAssist"},
            {"k3_role_defense", "k3RoleDefense"},
            {"k3_role_structure", "k3RoleStructure"},
            {"counterfactual_reason_key", "counterfactualReasonKey"},
            {"counterfactual_why_not", "counterfactualWhyNot"},
            {"counterfactual_summary", "counterfactualSummary"},
            {"cause_ablation_key", "causeAblationKey"},
            {"cause_ablation_summary", "causeAblationSummary"},
            {"miss_reason_type", "missReasonType"},
            {"miss_reason_matrix_key", "missReasonMatrixKey"},
            {"miss_reason_detail", "missReasonDetail"},
            {"adaptive_judge_status", "adaptiveJudgeStatus"},
            {"adaptive_judge_action", "adaptiveJudgeAction"},
            {"adaptive_judge_domain", "adaptiveJudgeDomain"},
            {"adaptive_judge_reason", "adaptiveJudgeReason"},
            {"adaptive_judge_version", "adaptiveJudgeVersion"},
            {"promotion_cluster_status", "promotionClusterStatus"},
            {"promotion_cluster_reason", "promotionClusterReason"},
            {"why_bet", "whyBet"},
            {"why_no_bet", "whyNoBet"},
            {"why_hit", "whyHit"},
            {"why_miss", "whyMiss"},
            {"dynamic_causal_summary", "dynamicCausalSummary"}
        };
    }

    private String[][] dynamicCausalIntMap() {
        return new String[][] {
            {"dynamic_cluster_sample", "dynamicClusterSample"},
            {"dynamic_cluster_hit_count", "dynamicClusterHitCount"},
            {"dynamic_cluster_miss_count", "dynamicClusterMissCount"},
            {"dynamic_cluster_recent_sample", "dynamicClusterRecentSample"},
            {"dynamic_cluster_recent_hit_count", "dynamicClusterRecentHitCount"},
            {"dynamic_cluster_max_lose", "dynamicClusterMaxLose"},
            {"k3_role_main_number", "k3RoleMainNumber"},
            {"k3_role_assist_number", "k3RoleAssistNumber"},
            {"k3_role_defense_number", "k3RoleDefenseNumber"},
            {"counterfactual_rank", "counterfactualRank"},
            {"cause_ablation_false_cause_count", "causeAblationFalseCauseCount"},
            {"adaptive_judge_candidate_count", "adaptiveJudgeCandidateCount"},
            {"adaptive_cluster_sample", "adaptiveClusterSample"},
            {"promotion_cluster_sample", "promotionClusterSample"}
        };
    }

    private String[][] dynamicCausalDoubleMap() {
        return new String[][] {
            {"dynamic_cluster_hit_rate", "dynamicClusterHitRate"},
            {"dynamic_cluster_recent_hit_rate", "dynamicClusterRecentHitRate"},
            {"dynamic_cluster_confidence", "dynamicClusterConfidence"},
            {"dynamic_cluster_distance", "dynamicClusterDistance"},
            {"dynamic_cluster_volatility", "dynamicClusterVolatility"},
            {"dynamic_cluster_counter_score", "dynamicClusterCounterScore"},
            {"position_need_strength", "positionNeedStrength"},
            {"position_need_chaos_risk", "positionNeedChaosRisk"},
            {"position_need_repair_score", "positionNeedRepairScore"},
            {"position_need_relay_score", "positionNeedRelayScore"},
            {"number_supply_strength", "numberSupplyStrength"},
            {"number_supply_risk", "numberSupplyRisk"},
            {"need_supply_match_score", "needSupplyMatchScore"},
            {"need_supply_third_quality", "needSupplyThirdQuality"},
            {"need_supply_role_diversity", "needSupplyRoleDiversity"},
            {"need_supply_risk", "needSupplyRisk"},
            {"counterfactual_rank_score", "counterfactualRankScore"},
            {"counterfactual_why_not_risk", "counterfactualWhyNotRisk"},
            {"cause_ablation_score", "causeAblationScore"},
            {"adaptive_cluster_hit_rate", "adaptiveClusterHitRate"},
            {"adaptive_cluster_recent_hit_rate", "adaptiveClusterRecentHitRate"},
            {"adaptive_cluster_confidence", "adaptiveClusterConfidence"},
            {"adaptive_cause_score", "adaptiveCauseScore"},
            {"adaptive_counter_score", "adaptiveCounterScore"},
            {"adaptive_promote_score", "adaptivePromoteScore"},
            {"adaptive_freeze_score", "adaptiveFreezeScore"},
            {"promotion_cluster_hit_rate", "promotionClusterHitRate"},
            {"promotion_cluster_recent_hit_rate", "promotionClusterRecentHitRate"}
        };
    }

    private void putStringField(ContentValues cv, Object o, String col, String field) {
        try {
            Field f = o.getClass().getField(field);
            Object v = f.get(o);
            if (v != null) cv.put(col, String.valueOf(v));
        } catch (Throwable e) {
        }
    }

    private void putIntField(ContentValues cv, Object o, String col, String field) {
        try {
            Field f = o.getClass().getField(field);
            cv.put(col, f.getInt(o));
        } catch (Throwable e) {
        }
    }

    private void putDoubleField(ContentValues cv, Object o, String col, String field) {
        try {
            Field f = o.getClass().getField(field);
            cv.put(col, AppConfig.clampDynamicCausalScore(f.getDouble(o)));
        } catch (Throwable e) {
        }
    }

    private void setStringField(Object o, String field, String v) {
        try {
            Field f = o.getClass().getField(field);
            f.set(o, v == null ? "" : v);
        } catch (Throwable e) {
        }
    }

    private void setIntField(Object o, String field, int v) {
        try {
            Field f = o.getClass().getField(field);
            f.setInt(o, v);
        } catch (Throwable e) {
        }
    }

    private void setDoubleField(Object o, String field, double v) {
        try {
            Field f = o.getClass().getField(field);
            f.setDouble(o, AppConfig.clampDynamicCausalScore(v));
        } catch (Throwable e) {
        }
    }

    private String[][] v24StringMap() {
        return new String[][] {
            // These two provenance fields are part of the same persisted decision/trade
            // contract.  Omitting them here meant that every settlement read/write round-trip
            // silently reset a legitimate REALTIME trade to UNKNOWN/0.
            {"source_type", "sourceType"},
            {"contract_decision_id", "contractDecisionId"},
            {"candidate_hash", "candidateHash"},
            {"snapshot_max_issue", "snapshotMaxIssue"},
            {"trained_until_issue", "trainedUntilIssue"},
            {"build_id", "buildId"},
            {"cohort_id", "cohortId"},
            {"schema_version", "schemaVersion"},
            {"feature_schema_version", "featureSchemaVersion"},
            {"ranking_version", "rankingVersion"},
            {"calibration_version", "calibrationVersion"},
            {"gate_policy_version", "gatePolicyVersion"},
            {"pipeline_version", "pipelineVersion"},
            {"contract_status", "contractStatus"},
            {"coarse_family_key", "coarseFamilyKey"},
            {"mid_family_key", "midFamilyKey"},
            {"fine_family_key", "fineFamilyKey"},
            {"position_family_key", "positionFamilyKey"},
            {"posterior_matched_level", "posteriorMatchedLevel"},
            {"posterior_family_key", "posteriorFamilyKey"},
            {"posterior_policy", "posteriorPolicy"},
            {"posterior_version", "posteriorVersion"},
            {"calibration_status", "calibrationStatus"},
            {"authorization_id", "authorizationId"},
            {"authorized_mode", "authorizedMode"},
            {"authorization_reject_code", "authorizationRejectCode"},
            {"teacher_policy", "teacherPolicy"},
            {"dynamic_policy", "dynamicPolicy"},
            {"adaptive_policy", "adaptivePolicy"},
            {"micro_walk_forward_status", "microWalkForwardStatus"},
            {"micro_walk_forward_version", "microWalkForwardVersion"},
            {"micro_walk_forward_as_of_issue", "microWalkForwardAsOfIssue"},
            {"action_utility_version", "actionUtilityVersion"},
            {"lane_candidate", "laneCandidate"},
            {"v42_regime_family", "v42RegimeFamily"},
            {"v42_risk_coverage_mode", "v42RiskCoverageMode"},
            {"v42_primary_reject", "v42PrimaryReject"},
            {"v42_secondary_reject", "v42SecondaryReject"},
            {"v42_tertiary_reject", "v42TertiaryReject"},
            {"v42_lane_policy_version", "v42LanePolicyVersion"},
            {"v43_dominant_evidence", "v43DominantEvidence"},
            {"v43_missing_evidence", "v43MissingEvidence"},
            {"v43_evidence_fingerprint", "v43EvidenceFingerprint"},
            {"v43_probability_lock_hash", "v43ProbabilityLockHash"},
            {"v43_pipeline_stage", "v43PipelineStage"},
            {"v43_invariant_status", "v43InvariantStatus"},
            {"v43_strategy_version", "v43StrategyVersion"}
        };
    }

    private String[][] v24IntMap() {
        return new String[][] {
            {"is_realtime_real", "isRealtimeReal"},
            {"contract_valid", "contractValid"},
            {"posterior_sample", "posteriorSample"},
            {"posterior_hit", "posteriorHit"},
            {"calibration_bucket", "calibrationBucket"},
            {"calibration_sample", "calibrationSample"},
            {"calibration_total_sample", "calibrationTotalSample"},
            {"calibration_ready", "calibrationReady"},
            {"authorization_valid", "authorizationValid"},
            {"micro_walk_forward_sample", "microWalkForwardSample"},
            {"oos_rank_slot", "oosRankSlot"},
            {"oos_candidate_count", "oosCandidateCount"},
            {"v42_support_votes", "v42SupportVotes"},
            {"v43_evidence_group_count", "v43EvidenceGroupCount"},
            {"v43_hard_risk", "v43HardRisk"}
        };
    }

    private String[][] v24LongMap() {
        return new String[][] {{"authorized_at", "authorizedAt"}};
    }

    private String[][] v24DoubleMap() {
        return new String[][] {
            {"raw_rank_score", "rawRankScore"},
            {"calibrated_hit_probability", "calibratedHitProbability"},
            {"posterior_mean", "posteriorMean"},
            {"posterior_wilson_lower", "posteriorWilsonLower"},
            {"posterior_recent_rate", "posteriorRecentRate"},
            {"posterior_drift", "posteriorDrift"},
            {"family_wilson_lower", "familyWilsonLower"},
            {"calibration_brier", "calibrationBrier"},
            {"authorization_probability", "authorizationProbability"},
            {"authorization_gate", "authorizationGate"},
            {"authorization_edge", "authorizationEdge"},
            {"honest_probability", "honestProbability"},
            {"action_utility", "actionUtility"},
            {"action_structure_score", "actionStructureScore"},
            {"action_discrimination_score", "actionDiscriminationScore"},
            {"action_maturity_score", "actionMaturityScore"},
            {"action_uncertainty_penalty", "actionUncertaintyPenalty"},
            {"action_frequency_support", "actionFrequencySupport"},
            {"v42_accept_score", "v42AcceptScore"},
            {"v42_family_lcb", "v42FamilyLcb"},
            {"v42_regime_quality", "v42RegimeQuality"},
            {"v42_association_score", "v42AssociationScore"},
            {"v42_distillation_risk", "v42DistillationRisk"},
            {"v43_strategy_support", "v43StrategySupport"},
            {"v43_strategy_risk", "v43StrategyRisk"},
            {"v43_strategy_confidence", "v43StrategyConfidence"},
            {"v43_strategy_diversity", "v43StrategyDiversity"},
            {"v43_strategy_maturity", "v43StrategyMaturity"},
            {"v43_safe_probability", "v43SafeProbability"},
            {"v43_probability_residual", "v43ProbabilityResidual"},
            {"v43_ablation_fragility", "v43AblationFragility"},
            {"v43_probability_lock", "v43ProbabilityLock"}
        };
    }

    private void putV24ContractValues(ContentValues cv, Object obj) {
        if (cv == null || obj == null) return;
        String[][] m = v24StringMap();
        for (int i = 0; i < m.length; i++) putFieldChainString(cv, obj, m[i][0], m[i][1]);
        m = v24IntMap();
        for (int i = 0; i < m.length; i++) putFieldChainInt(cv, obj, m[i][0], m[i][1]);
        m = v24LongMap();
        for (int i = 0; i < m.length; i++) cv.put(m[i][0], getOptionalLongField(obj, m[i][1], 0L));
        m = v24DoubleMap();
        for (int i = 0; i < m.length; i++)
            cv.put(m[i][0], getOptionalDoubleField(obj, m[i][1], 0.0d));
    }

    private void readV24ContractValues(Cursor c, Object obj) {
        if (c == null || obj == null) return;
        String[][] m = v24StringMap();
        for (int i = 0; i < m.length; i++) readFieldChainString(c, obj, m[i][0], m[i][1]);
        m = v24IntMap();
        for (int i = 0; i < m.length; i++) readFieldChainInt(c, obj, m[i][0], m[i][1]);
        m = v24LongMap();
        for (int i = 0; i < m.length; i++) setOptionalLongField(obj, m[i][1], getLong(c, m[i][0]));
        m = v24DoubleMap();
        for (int i = 0; i < m.length; i++)
            setOptionalDoubleField(obj, m[i][1], getDouble(c, m[i][0]));
    }

    private void putFieldChainValues(ContentValues cv, Object obj) {
        if (cv == null || obj == null) {
            return;
        }
        putFieldChainInt(cv, obj, "teacher_weak_no_bet_flag", "teacherWeakNoBetFlag");
        putFieldChainInt(cv, obj, "teacher_matrix_weak_no_bet_flag", "teacherMatrixWeakNoBetFlag");
        putFieldChainInt(
                cv,
                obj,
                "teacher_matrix_ordinary_bet_block_flag",
                "teacherMatrixOrdinaryBetBlockFlag");
        putFieldChainInt(
                cv, obj, "teacher_matrix_supports_final_gate", "teacherMatrixSupportsFinalGate");
        putFieldChainInt(cv, obj, "teacher_matrix_supports_bet", "teacherMatrixSupportsBet");
        putFieldChainString(cv, obj, "teacher_ordinary_bet_policy", "teacherOrdinaryBetPolicy");
        putFieldChainString(
                cv, obj, "teacher_matrix_ordinary_bet_policy", "teacherMatrixOrdinaryBetPolicy");
        putFieldChainString(cv, obj, "teacher_field_source", "teacherFieldSource");
        putFieldChainString(cv, obj, "teacher_field_version", "teacherFieldVersion");
        putFieldChainString(cv, obj, "teacher_number_status", "teacherNumberStatus");
        putFieldChainString(cv, obj, "teacher_status", "teacherStatus");
        putFieldChainDouble(
                cv, obj, "teacher_evidence_contribution", "teacherEvidenceContribution");
        putFieldChainDouble(
                cv,
                obj,
                "teacher_matrix_evidence_contribution",
                "teacherMatrixEvidenceContribution");
        putFieldChainDouble(cv, obj, "teacher_counter_contribution", "teacherCounterContribution");
        putFieldChainDouble(
                cv, obj, "teacher_matrix_counter_contribution", "teacherMatrixCounterContribution");
        putFieldChainDouble(cv, obj, "teacher_sample_reliability", "teacherSampleReliability");
        putFieldChainDouble(cv, obj, "teacher_recent_validity", "teacherRecentValidity");

        putFieldChainString(cv, obj, "v20_status", "v20Status");
        putFieldChainString(cv, obj, "dynamic_status", "dynamicStatus");
        putFieldChainString(cv, obj, "dynamic_cause_status", "dynamicCauseStatus");
        putFieldChainDouble(cv, obj, "v20_support_score", "v20SupportScore");
        putFieldChainDouble(cv, obj, "v20_risk_score", "v20RiskScore");
        putFieldChainDouble(cv, obj, "v20_evidence_contribution", "v20EvidenceContribution");
        putFieldChainDouble(cv, obj, "v20_counter_contribution", "v20CounterContribution");
        putFieldChainDouble(cv, obj, "v20_sample_reliability", "v20SampleReliability");
        putFieldChainDouble(cv, obj, "v20_recent_validity", "v20RecentValidity");
        putFieldChainString(cv, obj, "v20_action_advice", "v20ActionAdvice");
        putFieldChainString(cv, obj, "v20_window_state_key", "v20WindowStateKey");
        putFieldChainString(cv, obj, "window_state_key", "windowStateKey");
        putFieldChainString(
                cv, obj, "final_decision_window_state_key", "finalDecisionWindowStateKey");
        putFieldChainString(cv, obj, "dynamic_window_state_key", "dynamicWindowStateKey");
        putFieldChainString(cv, obj, "probability_window_state_key", "probabilityWindowStateKey");

        putFieldChainDouble(cv, obj, "coverage_quality_score", "coverageQualityScore");
        putFieldChainDouble(cv, obj, "third_number_quality_score", "thirdNumberQualityScore");
        putFieldChainDouble(cv, obj, "k3_coverage_risk", "k3CoverageRisk");
        putFieldChainInt(cv, obj, "false_high_p_flag", "falseHighPFlag");
        putFieldChainInt(cv, obj, "probability_false_high_p_flag", "probabilityFalseHighPFlag");
        putFieldChainDouble(cv, obj, "probability_false_high_risk", "probabilityFalseHighRisk");
        putFieldChainInt(
                cv, obj, "probability_preferred_edge_band", "probabilityPreferredEdgeBand");
        putFieldChainInt(
                cv, obj, "probability_teacher_weak_or_empty", "probabilityTeacherWeakOrEmpty");
        putFieldChainInt(
                cv, obj, "probability_v20_unknown_or_empty", "probabilityV20UnknownOrEmpty");
        putFieldChainInt(cv, obj, "probability_strong_cause_flag", "probabilityStrongCauseFlag");
        putFieldChainString(cv, obj, "probability_coverage_summary", "probabilityCoverageSummary");
        putFieldChainString(cv, obj, "probability_action_advice", "probabilityActionAdvice");

        putFieldChainInt(cv, obj, "dynamic_gate_false_high_p_flag", "dynamicGateFalseHighPFlag");
        putFieldChainInt(
                cv, obj, "dynamic_gate_preferred_edge_band", "dynamicGatePreferredEdgeBand");
        putFieldChainString(cv, obj, "dynamic_gate_edge_policy", "dynamicGateEdgePolicy");
        putFieldChainDouble(cv, obj, "dynamic_gate_coverage_risk", "dynamicGateCoverageRisk");
        putFieldChainDouble(
                cv, obj, "dynamic_gate_coverage_quality_score", "dynamicGateCoverageQualityScore");
        putFieldChainDouble(
                cv, obj, "dynamic_gate_third_number_quality", "dynamicGateThirdNumberQuality");
        putFieldChainDouble(
                cv, obj, "dynamic_gate_virtual_edge_risk", "dynamicGateVirtualEdgeRisk");
        putFieldChainString(cv, obj, "dynamic_gate_version", "dynamicGateVersion");

        putFieldChainString(cv, obj, "final_decision_version", "finalDecisionVersion");
        putFieldChainString(cv, obj, "final_decision_edge_policy", "finalDecisionEdgePolicy");
        putFieldChainString(cv, obj, "final_decision_teacher_policy", "finalDecisionTeacherPolicy");
        putFieldChainString(cv, obj, "final_decision_v20_policy", "finalDecisionV20Policy");
        putFieldChainString(cv, obj, "final_decision_bet_domain", "finalDecisionBetDomain");
        putFieldChainString(cv, obj, "final_decision_reject_code", "finalDecisionRejectCode");
        putFieldChainString(cv, obj, "final_decision_shadow_reason", "finalDecisionShadowReason");
        putFieldChainDouble(cv, obj, "final_decision_coverage_risk", "finalDecisionCoverageRisk");
        putFieldChainDouble(
                cv, obj, "final_decision_third_number_quality", "finalDecisionThirdNumberQuality");
        putFieldChainInt(
                cv, obj, "final_decision_false_high_p_flag", "finalDecisionFalseHighPFlag");
    }

    private void readFieldChainValues(Cursor c, Object obj) {
        if (c == null || obj == null) {
            return;
        }
        readFieldChainInt(c, obj, "teacher_weak_no_bet_flag", "teacherWeakNoBetFlag");
        readFieldChainInt(c, obj, "teacher_matrix_weak_no_bet_flag", "teacherMatrixWeakNoBetFlag");
        readFieldChainInt(
                c,
                obj,
                "teacher_matrix_ordinary_bet_block_flag",
                "teacherMatrixOrdinaryBetBlockFlag");
        readFieldChainInt(
                c, obj, "teacher_matrix_supports_final_gate", "teacherMatrixSupportsFinalGate");
        readFieldChainInt(c, obj, "teacher_matrix_supports_bet", "teacherMatrixSupportsBet");
        readFieldChainString(c, obj, "teacher_ordinary_bet_policy", "teacherOrdinaryBetPolicy");
        readFieldChainString(
                c, obj, "teacher_matrix_ordinary_bet_policy", "teacherMatrixOrdinaryBetPolicy");
        readFieldChainString(c, obj, "teacher_field_source", "teacherFieldSource");
        readFieldChainString(c, obj, "teacher_field_version", "teacherFieldVersion");
        readFieldChainString(c, obj, "teacher_number_status", "teacherNumberStatus");
        readFieldChainString(c, obj, "teacher_status", "teacherStatus");
        readFieldChainDouble(
                c, obj, "teacher_evidence_contribution", "teacherEvidenceContribution");
        readFieldChainDouble(
                c,
                obj,
                "teacher_matrix_evidence_contribution",
                "teacherMatrixEvidenceContribution");
        readFieldChainDouble(c, obj, "teacher_counter_contribution", "teacherCounterContribution");
        readFieldChainDouble(
                c, obj, "teacher_matrix_counter_contribution", "teacherMatrixCounterContribution");
        readFieldChainDouble(c, obj, "teacher_sample_reliability", "teacherSampleReliability");
        readFieldChainDouble(c, obj, "teacher_recent_validity", "teacherRecentValidity");

        readFieldChainString(c, obj, "v20_status", "v20Status");
        readFieldChainString(c, obj, "dynamic_status", "dynamicStatus");
        readFieldChainString(c, obj, "dynamic_cause_status", "dynamicCauseStatus");
        readFieldChainDouble(c, obj, "v20_support_score", "v20SupportScore");
        readFieldChainDouble(c, obj, "v20_risk_score", "v20RiskScore");
        readFieldChainDouble(c, obj, "v20_evidence_contribution", "v20EvidenceContribution");
        readFieldChainDouble(c, obj, "v20_counter_contribution", "v20CounterContribution");
        readFieldChainDouble(c, obj, "v20_sample_reliability", "v20SampleReliability");
        readFieldChainDouble(c, obj, "v20_recent_validity", "v20RecentValidity");
        readFieldChainString(c, obj, "v20_action_advice", "v20ActionAdvice");
        readFieldChainString(c, obj, "v20_window_state_key", "v20WindowStateKey");
        readFieldChainString(c, obj, "window_state_key", "windowStateKey");
        readFieldChainString(
                c, obj, "final_decision_window_state_key", "finalDecisionWindowStateKey");
        readFieldChainString(c, obj, "dynamic_window_state_key", "dynamicWindowStateKey");
        readFieldChainString(c, obj, "probability_window_state_key", "probabilityWindowStateKey");

        readFieldChainDouble(c, obj, "coverage_quality_score", "coverageQualityScore");
        readFieldChainDouble(c, obj, "third_number_quality_score", "thirdNumberQualityScore");
        readFieldChainDouble(c, obj, "k3_coverage_risk", "k3CoverageRisk");
        readFieldChainInt(c, obj, "false_high_p_flag", "falseHighPFlag");
        readFieldChainInt(c, obj, "probability_false_high_p_flag", "probabilityFalseHighPFlag");
        readFieldChainDouble(c, obj, "probability_false_high_risk", "probabilityFalseHighRisk");
        readFieldChainInt(
                c, obj, "probability_preferred_edge_band", "probabilityPreferredEdgeBand");
        readFieldChainInt(
                c, obj, "probability_teacher_weak_or_empty", "probabilityTeacherWeakOrEmpty");
        readFieldChainInt(
                c, obj, "probability_v20_unknown_or_empty", "probabilityV20UnknownOrEmpty");
        readFieldChainInt(c, obj, "probability_strong_cause_flag", "probabilityStrongCauseFlag");
        readFieldChainString(c, obj, "probability_coverage_summary", "probabilityCoverageSummary");
        readFieldChainString(c, obj, "probability_action_advice", "probabilityActionAdvice");

        readFieldChainInt(c, obj, "dynamic_gate_false_high_p_flag", "dynamicGateFalseHighPFlag");
        readFieldChainInt(
                c, obj, "dynamic_gate_preferred_edge_band", "dynamicGatePreferredEdgeBand");
        readFieldChainString(c, obj, "dynamic_gate_edge_policy", "dynamicGateEdgePolicy");
        readFieldChainDouble(c, obj, "dynamic_gate_coverage_risk", "dynamicGateCoverageRisk");
        readFieldChainDouble(
                c, obj, "dynamic_gate_coverage_quality_score", "dynamicGateCoverageQualityScore");
        readFieldChainDouble(
                c, obj, "dynamic_gate_third_number_quality", "dynamicGateThirdNumberQuality");
        readFieldChainDouble(
                c, obj, "dynamic_gate_virtual_edge_risk", "dynamicGateVirtualEdgeRisk");
        readFieldChainString(c, obj, "dynamic_gate_version", "dynamicGateVersion");

        readFieldChainString(c, obj, "final_decision_version", "finalDecisionVersion");
        readFieldChainString(c, obj, "final_decision_edge_policy", "finalDecisionEdgePolicy");
        readFieldChainString(c, obj, "final_decision_teacher_policy", "finalDecisionTeacherPolicy");
        readFieldChainString(c, obj, "final_decision_v20_policy", "finalDecisionV20Policy");
        readFieldChainString(c, obj, "final_decision_bet_domain", "finalDecisionBetDomain");
        readFieldChainString(c, obj, "final_decision_reject_code", "finalDecisionRejectCode");
        readFieldChainString(c, obj, "final_decision_shadow_reason", "finalDecisionShadowReason");
        readFieldChainDouble(c, obj, "final_decision_coverage_risk", "finalDecisionCoverageRisk");
        readFieldChainDouble(
                c, obj, "final_decision_third_number_quality", "finalDecisionThirdNumberQuality");
        readFieldChainInt(
                c, obj, "final_decision_false_high_p_flag", "finalDecisionFalseHighPFlag");

        syncFieldChainAliases(obj);
    }

    private void putFieldChainInt(ContentValues cv, Object obj, String column, String field) {
        cv.put(column, getOptionalIntField(obj, field, 0));
    }

    private void putFieldChainDouble(ContentValues cv, Object obj, String column, String field) {
        cv.put(column, getOptionalDoubleField(obj, field, 0.0d));
    }

    private void putFieldChainString(ContentValues cv, Object obj, String column, String field) {
        cv.put(column, getOptionalStringField(obj, field, ""));
    }

    private void readFieldChainInt(Cursor c, Object obj, String column, String field) {
        setOptionalIntField(obj, field, getInt(c, column));
    }

    private void readFieldChainDouble(Cursor c, Object obj, String column, String field) {
        setOptionalDoubleField(obj, field, getDouble(c, column));
    }

    private void readFieldChainString(Cursor c, Object obj, String column, String field) {
        setOptionalStringField(obj, field, getString(c, column));
    }

    private void syncFieldChainAliases(Object obj) {
        if (obj == null) {
            return;
        }
        if (getOptionalStringField(obj, "teacherMatrixStatus", "").length() == 0) {
            setOptionalStringField(
                    obj,
                    "teacherMatrixStatus",
                    firstNonEmpty(
                            getOptionalStringField(obj, "teacherNumberStatus", ""),
                            getOptionalStringField(obj, "teacherStatus", "")));
        }
        if (getOptionalStringField(obj, "teacherStatus", "").length() == 0) {
            setOptionalStringField(
                    obj,
                    "teacherStatus",
                    firstNonEmpty(
                            getOptionalStringField(obj, "teacherMatrixStatus", ""),
                            getOptionalStringField(obj, "teacherNumberStatus", "")));
        }
        if (getOptionalStringField(obj, "v20Status", "").length() == 0) {
            setOptionalStringField(
                    obj,
                    "v20Status",
                    firstNonEmpty(
                            getOptionalStringField(obj, "dynamicStatus", ""),
                            getOptionalStringField(obj, "dynamicCauseStatus", "")));
        }
        if (getOptionalStringField(obj, "dynamicStatus", "").length() == 0) {
            setOptionalStringField(
                    obj,
                    "dynamicStatus",
                    firstNonEmpty(
                            getOptionalStringField(obj, "v20Status", ""),
                            getOptionalStringField(obj, "dynamicCauseStatus", "")));
        }
        if (getOptionalStringField(obj, "windowStateKey", "").length() == 0) {
            setOptionalStringField(
                    obj,
                    "windowStateKey",
                    firstNonEmpty(
                            getOptionalStringField(obj, "finalDecisionWindowStateKey", ""),
                            firstNonEmpty(
                                    getOptionalStringField(obj, "dynamicWindowStateKey", ""),
                                    getOptionalStringField(obj, "probabilityWindowStateKey", ""))));
        }
        double cr =
                maxOptionalDouble(
                        obj,
                        new String[] {
                            "k3CoverageRisk", "dynamicGateCoverageRisk", "finalDecisionCoverageRisk"
                        });
        if (cr > 0.0d) {
            setOptionalDoubleField(obj, "k3CoverageRisk", cr);
            setOptionalDoubleField(obj, "dynamicGateCoverageRisk", cr);
        }
        double third =
                maxOptionalDouble(
                        obj,
                        new String[] {
                            "thirdNumberQualityScore",
                            "dynamicGateThirdNumberQuality",
                            "finalDecisionThirdNumberQuality"
                        });
        if (third > 0.0d) {
            setOptionalDoubleField(obj, "thirdNumberQualityScore", third);
            setOptionalDoubleField(obj, "dynamicGateThirdNumberQuality", third);
        }
        int fh =
                maxOptionalInt(
                        obj,
                        new String[] {
                            "falseHighPFlag",
                            "probabilityFalseHighPFlag",
                            "dynamicGateFalseHighPFlag",
                            "finalDecisionFalseHighPFlag"
                        });
        if (fh > 0) {
            setOptionalIntField(obj, "falseHighPFlag", fh);
            setOptionalIntField(obj, "dynamicGateFalseHighPFlag", fh);
        }
    }

    private int maxOptionalInt(Object obj, String[] fields) {
        int best = 0;
        if (fields == null) {
            return best;
        }
        for (int i = 0; i < fields.length; i++) {
            int v = getOptionalIntField(obj, fields[i], 0);
            if (v > best) {
                best = v;
            }
        }
        return best;
    }

    private double maxOptionalDouble(Object obj, String[] fields) {
        double best = 0.0d;
        if (fields == null) {
            return best;
        }
        for (int i = 0; i < fields.length; i++) {
            double v = getOptionalDoubleField(obj, fields[i], 0.0d);
            if (v > best) {
                best = v;
            }
        }
        return best;
    }

    private ContentValues decisionToValues(Models.Decision d) {
        ContentValues cv = new ContentValues();

        cv.put("base_issue", d.baseIssue);
        cv.put("target_issue", d.targetIssue);
        cv.put("created_at", d.createdAt);
        cv.put("decision", d.decision);
        cv.put("action", d.action);
        cv.put("trade_id", d.tradeId);

        cv.put("position", d.position);
        cv.put("n1", d.n1);
        cv.put("n2", d.n2);
        cv.put("n3", d.n3);
        cv.put("k3_mask", d.k3Mask);

        cv.put("raw_p", d.rawP);
        cv.put("calibrated_p", d.calibratedP);
        cv.put("final_p", d.finalP);
        cv.put("dynamic_gate", d.dynamicGate);
        cv.put("edge", d.edge);

        cv.put("model_p", d.modelP);
        cv.put("shape_p", d.shapeP);
        cv.put("combo_p", d.comboP);
        cv.put("recovery_p", d.recoveryP);
        cv.put("similar_p", d.similarP);
        cv.put("memory_p", d.memoryP);

        cv.put("similar_hit_rate", d.similarHitRate);
        cv.put("combo_hit_rate", d.comboHitRate);
        cv.put("effective_sample", d.effectiveSample);
        cv.put("combo_sample", d.comboSample);

        cv.put("trend_life", d.trendLife);
        cv.put("drift_risk", d.driftRisk);
        cv.put("break_depth", d.breakDepth);
        cv.put("recovery_score", d.recoveryScore);
        cv.put("champion_gap", d.championGap);
        cv.put("combo_gap", d.comboGap);
        cv.put("hot_reverse_risk", d.hotReverseRisk);
        cv.put("false_strong_risk", d.falseStrongRisk);

        cv.put("freq100", d.freq100);
        cv.put("freq500", d.freq500);
        cv.put("freq1000", d.freq1000);
        cv.put("calibration_error", d.calibrationError);
        cv.put("probability_mode", d.probabilityMode);
        cv.put("backtest_mode", d.backtestMode);
        cv.put("gate_mode", getOptionalStringField(d, "gateMode", ""));

        cv.put("state_mode", d.stateMode);
        cv.put("shape_key", d.shapeKey);
        cv.put("block_code", d.blockCode);
        cv.put("reason_summary", d.reasonSummary);
        cv.put("reason_text", d.reasonText);

        cv.put("data_ok", d.dataOk);
        cv.put("settled", d.settled);
        cv.put("result", d.result);
        cv.put("settle_time", d.settleTime);
        cv.put("settled_at", d.settledAt);
        cv.put("actual_number", d.actualNumber);
        cv.put("hit", d.hit);
        cv.put("pnl", d.pnl);
        cv.put("miss_reason", d.missReason);
        cv.put("settlement_reason", d.settlementReason);

        cv.put("deep_score", d.deepScore);
        cv.put("deep_confidence", d.deepConfidence);
        cv.put("deep_feature_p", d.deepFeatureP);
        cv.put("deep_position_score", d.deepPositionScore);
        cv.put("deep_number_score", d.deepNumberScore);
        cv.put("deep_cycle_score", d.deepCycleScore);
        cv.put("deep_transition_score", d.deepTransitionScore);
        cv.put("deep_state_score", d.deepStateScore);
        cv.put("bias_score", d.biasScore);
        cv.put("bias_confidence", d.biasConfidence);
        cv.put("profile_score", d.profileScore);
        cv.put("fallback_profile_score", d.fallbackProfileScore);
        cv.put("audit_penalty", d.auditPenalty);
        cv.put("audit_boost", d.auditBoost);
        cv.put("ai_judge_score", d.aiJudgeScore);
        cv.put("ai_judge_confidence", d.aiJudgeConfidence);
        cv.put("evidence_score", d.evidenceScore);
        cv.put("evidence_risk", d.evidenceRisk);
        cv.put("conflict_level", d.conflictLevel);
        cv.put("a_plus_score", d.aPlusScore);
        cv.put("promotion_score", d.promotionScore);
        cv.put("weakest_number_score", d.weakestNumberScore);
        cv.put("balance_score", d.balanceScore);

        cv.put("deep_profile_key", d.deepProfileKey);
        cv.put("deep_profile_hit_rate", d.deepProfileHitRate);
        cv.put("deep_profile_sample", d.deepProfileSample);
        cv.put("deep_fallback_level", d.deepFallbackLevel);
        cv.put("fallback_profile_key", d.fallbackProfileKey);
        cv.put("promotion_status", d.promotionStatus);
        cv.put("a_plus_status", d.aPlusStatus);
        cv.put("candidate_evidence_key", d.candidateEvidenceKey);
        cv.put("snapshot_id", d.snapshotId);

        cv.put("deep_attribution", d.deepAttribution);
        cv.put("deep_reason", d.deepReason);
        cv.put("ai_judge_summary", d.aiJudgeSummary);
        cv.put("learning_summary", d.learningSummary);
        cv.put("self_diagnosis_text", d.selfDiagnosisText);

        cv.put("manual_backtest_support", d.manualBacktestSupport);
        cv.put("stat_scope", d.statScope);

        cv.put("state_key", firstNonEmpty(d.stateKey, safeStateKeyFromDecision(d)));
        cv.put("trend_key", firstNonEmpty(d.trendKey, safeString(d.shapeKey)));
        cv.put("drift_key", firstNonEmpty(d.driftKey, bucketText("DRIFT", d.driftRisk)));
        cv.put("break_key", firstNonEmpty(d.breakKey, bucketText("BREAK", d.breakDepth)));
        cv.put(
                "recovery_key",
                firstNonEmpty(d.recoveryKey, bucketText("RECOVERY", d.recoveryScore)));

        cv.put(
                "runtime_profile_score",
                d.runtimeProfileScore != 0.0d ? d.runtimeProfileScore : d.profileScore);
        cv.put(
                "state_number_profile_score",
                d.stateNumberProfileScore != 0.0d ? d.stateNumberProfileScore : d.biasScore);
        cv.put(
                "teacher_weight",
                d.teacherWeight > 0.0d
                        ? d.teacherWeight
                        : (d.aiJudgeConfidence > 0.0d ? d.aiJudgeConfidence : 1.0d));
        cv.put(
                "state_confidence",
                d.stateConfidence != 0.0d ? d.stateConfidence : d.deepConfidence);

        cv.put("state_sample", d.stateSample != 0 ? d.stateSample : d.effectiveSample);
        cv.put("cache_sample", d.cacheSample != 0 ? d.cacheSample : d.comboSample);
        cv.put("recent_sample", d.recentSample != 0 ? d.recentSample : d.deepProfileSample);

        cv.put("recent_hit_rate", d.recentHitRate != 0.0d ? d.recentHitRate : d.deepProfileHitRate);
        cv.put("runtime_hit_rate", d.runtimeHitRate != 0.0d ? d.runtimeHitRate : d.comboHitRate);
        cv.put(
                "number_profile_hit_rate",
                d.numberProfileHitRate != 0.0d ? d.numberProfileHitRate : d.similarHitRate);

        cv.put("realtime_cost_ms", d.realtimeCostMs);
        cv.put("realtime_timeout", d.realtimeTimeout);

        putTeacherMatrixFieldsFromObject(cv, d, safeStateKeyFromDecision(d), d.position, d.k3Mask);

        putFieldChainValues(cv, d);
        putDynamicCausalValues(cv, d);
        putMainChainClosedLoopValues(cv, d);
        putFinalVersionFallback(cv, d);
        putV24ContractValues(cv, d);
        cv.put("predecision_opportunity", d.preDecisionOpportunity);
        cv.put(
                "risk_severity",
                d.riskSeverity == null ? AppConfig.EVIDENCE_SEVERITY_NEUTRAL : d.riskSeverity);
        cv.put("hard_block_flag", d.hardBlockFlag);
        cv.put("hard_block_source", d.hardBlockSource == null ? "" : d.hardBlockSource);
        cv.put("evidence_source", d.evidenceSource == null ? "NONE" : d.evidenceSource);
        cv.put("evidence_settled_only", d.evidenceSettledOnly == 0 ? 1 : d.evidenceSettledOnly);
        cv.put("evidence_sample_count", d.evidenceSampleCount);
        cv.put("evidence_hit_count", d.evidenceHitCount);
        cv.put("evidence_miss_count", d.evidenceMissCount);
        cv.put(
                "promotion_coarse_domain",
                d.promotionCoarseDomain == null ? "" : d.promotionCoarseDomain);
        cv.put("promotion_mid_domain", d.promotionMidDomain == null ? "" : d.promotionMidDomain);
        cv.put("promotion_fine_domain", d.promotionFineDomain == null ? "" : d.promotionFineDomain);

        return cv;
    }

    public int updateDecisionDeepFields(Models.Decision d) {
        if (d == null || d.targetIssue == null || d.targetIssue.length() == 0) {
            return 0;
        }

        long id = replaceDecisionFull(d);
        return id > 0L ? 1 : 0;
    }

    public Models.Decision getLatestDecision() {
        String targetIssue = getState("target_issue", "");

        if (targetIssue != null && targetIssue.length() > 0) {
            Models.Decision current = getDecisionByTargetIssue(targetIssue);

            if (current != null) {
                return current;
            }

            return buildPendingDecisionForTargetIssue(targetIssue);
        }

        Cursor c = null;
        Models.Decision fallback = null;

        try {
            c =
                    readable()
                            .query(
                                    DbHelper.TABLE_DECISIONS,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    "created_at DESC, _id DESC",
                                    "30");

            while (c.moveToNext()) {
                Models.Decision d = readDecision(c);

                if (fallback == null) {
                    fallback = d;
                }

                if (!isWeakPlaceholderDecision(d)) {
                    return d;
                }
            }
        } finally {
            closeCursor(c);
        }

        return fallback;
    }

    public Models.Decision loadLastDecision() {
        return getLatestDecision();
    }

    public Models.Decision getLatestDecisionForCurrentTargetStrict() {
        String targetIssue = getState("target_issue", "");

        if (targetIssue == null || targetIssue.length() == 0) {
            return null;
        }

        return getDecisionByTargetIssue(targetIssue);
    }

    private Models.Decision buildPendingDecisionForTargetIssue(String targetIssue) {
        Models.Decision d = new Models.Decision();

        d.baseIssue = getState("current_issue", "");
        d.targetIssue = targetIssue == null ? "" : targetIssue;
        d.createdAt = System.currentTimeMillis();
        d.decision = AppConfig.DECISION_HOLD;
        d.action = AppConfig.DECISION_HOLD;
        d.statScope = AppConfig.STAT_SCOPE_HOLD;

        d.position = 0;
        d.n1 = 0;
        d.n2 = 0;
        d.n3 = 0;
        d.k3Mask = 0;

        d.rawP = 0.0d;
        d.calibratedP = 0.0d;
        d.finalP = 0.0d;
        d.dynamicGate = AppConfig.DEFAULT_DYNAMIC_GATE;
        d.edge = 0.0d;

        d.blockCode = AppConfig.BLOCK_SCHEDULER_BUSY;
        d.reasonSummary = "当前目标期尚未生成正式决策，等待实时分析线程完成；不会用旧目标期替代当前期。";
        d.reasonText = d.reasonSummary;
        d.selfDiagnosisText = d.reasonSummary;

        d.dataOk = 0;
        d.settled = 0;
        d.result = AppConfig.RESULT_WAIT;

        return d;
    }

    public Models.Decision getDecisionByTargetIssue(String targetIssue) {
        if (targetIssue == null || targetIssue.length() == 0) {
            return null;
        }

        Models.Decision canonical = CanonicalDecisionViewAdapterR36.read(this, targetIssue);
        if (canonical != null) return canonical;
        /* The live card is canonical-only. A legacy row for the same target must
         * never leak old numbers while the R3.6 request is still running. */
        String currentTarget = getState("target_issue", "");
        if (AppConfig.DB_VERSION >= 36 && targetIssue.equals(currentTarget)) return null;
        return getBestDecisionByTargetIssue(readable(), targetIssue);
    }

    public List<Models.Decision> getRecentDecisions(int limit) {
        ArrayList<Models.Decision> list = new ArrayList<Models.Decision>();

        if (limit <= 0) {
            return list;
        }

        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    DbHelper.TABLE_DECISIONS,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    "created_at DESC, _id DESC",
                                    String.valueOf(limit));

            while (c.moveToNext()) {
                list.add(readDecision(c));
            }
        } finally {
            closeCursor(c);
        }

        return list;
    }

    private void ensureTradeForRealBetDecision(Models.Decision d, long decisionId) {
        if (d == null || decisionId <= 0L) {
            return;
        }

        if (!isValidBetDecision(d)) {
            return;
        }

        try {
            d.id = decisionId;
            long tradeId = saveTradeFromDecision(d);

            if (tradeId > 0L && d.tradeId != tradeId) {
                d.tradeId = tradeId;

                ContentValues cv = new ContentValues();
                cv.put("trade_id", tradeId);
                cv.put("stat_scope", AppConfig.STAT_SCOPE_REAL_TRADE);
                cv.put("action", AppConfig.DECISION_BET);

                updateFiltered(
                        writable(),
                        DbHelper.TABLE_DECISIONS,
                        cv,
                        "_id=?",
                        new String[] {String.valueOf(decisionId)});
            }
        } catch (Throwable e) {
        }
    }

    public long saveTradeFromDecision(Models.Decision d) {
        if (d==null || d.targetIssue==null) return -1L;
        Models.Trade existing=getTradeByTargetIssue(d.targetIssue);
        if(existing==null || existing.position!=d.position || existing.n1!=d.n1
                || existing.n2!=d.n2 || existing.n3!=d.n3) return -1L;
        return existing.id; // Read-only compatibility. Missing REAL is never backfilled.
    }

    /**
     * R915 atomic V51 REAL ledger writer.  It is called only from V51Repository's canonical
     * pre-draw transaction, so the BET authorization, frozen observation and settlement target can
     * never diverge across a process death.
     */
    long insertAuthorizedTradeWithinCanonicalR915(
            SQLiteDatabase db, V51RuntimeResult result, long commitAt) {
        if (db == null || !db.inTransaction())
            throw new IllegalStateException("R915_REAL_CANONICAL_TRANSACTION_REQUIRED");
        if (result == null || result.authorized == null
                || result.authorized.action != V51Enums.DecisionAction.BET) return 0L;
        if (result.contract == null || result.contract.certifiedPrediction == null)
            throw new IllegalStateException("CP3_NO_VALID_CERTIFICATE");
        if (!V51CandidateRealGateR915.verifyAuthorized(null, result.contract, result.authorized))
            throw new IllegalStateException("R915_REAL_GATE_OR_SIGNATURE_INVALID");
        V51Models.SelectedActionEnvelope selected = result.selected;
        V51Models.PositionAction action = selected.action;
        String target = selected.context.targetIssue;
        if (android.database.DatabaseUtils.longForQuery(
                        db,
                        "SELECT COUNT(1) FROM " + DbHelper.TABLE_DRAWS
                                + " WHERE issue=? AND dirty=0",
                        new String[] {target}) != 0L)
            throw new IllegalStateException("R915_REAL_TARGET_ALREADY_DRAWN");

        Cursor existing = null;
        try {
            existing = db.rawQuery(
                    "SELECT _id,position,n1,n2,n3,authorization_id FROM "
                            + DbHelper.TABLE_TRADES
                            + " WHERE target_issue=? AND (action='BET' OR stat_scope='REAL_TRADE')"
                            + " ORDER BY _id ASC LIMIT 1",
                    new String[] {target});
            if (existing.moveToFirst()) {
                boolean exact = existing.getInt(1) == action.position
                        && existing.getInt(2) == action.n1
                        && existing.getInt(3) == action.n2
                        && existing.getInt(4) == action.n3
                        && result.authorized.authorizationHash.equals(existing.getString(5));
                if (!exact) throw new IllegalStateException("R915_REAL_FIRST_WRITE_CONFLICT");
                return existing.getLong(0);
            }
        } finally {
            if (existing != null) existing.close();
        }
        CertifiedPredictionR974.beforeInsert(this,db,result.contract);
        // Exact branch already requires its Proof; only same target is unique. Legacy writers retain their separate policy.

        Models.Decision decision = new DecisionGateEngine().toLegacyDecisionV51(result);
        // REAL persists the exact certificate-bound candidate probability without re-estimation.
        double exactP=result.contract.certifiedPrediction.selectedHitP;
        if(!CertifiedPredictionR974.validSelectedProbability(exactP)
                ||Double.compare(exactP,selected.selectedHitP)!=0)
            throw new IllegalStateException("R974_REAL_CALIBRATED_P_REQUIRED");
        decision.calibratedP = exactP;
        decision.finalP = exactP;
        decision.honestProbability = exactP;
        decision.calibratedHitProbability = exactP;
        decision.dynamicGate = 0.0d;
        decision.edge = 0.0d;
        decision.authorizationProbability = exactP;
        decision.authorizationGate = 0.0d;
        decision.authorizationEdge = 0.0d;
        decision.reasonSummary =
                "R915受控工程候选｜MODEL_NOT_PROVEN｜原始动作概率=" + action.rawActionP
                        + "｜第三边界=" + action.thirdMargin
                        + "｜稳定性=" + action.stability
                        + "｜非校准命中率声明";
        if(result.contract.certifiedPrediction!=null)decision.reasonSummary=
                "R974独立证据合格模型｜"+result.contract.certifiedPrediction.candidate
                +"｜证书="+result.contract.certifiedPrediction.certificate+"｜预测收据="+result.contract.certifiedPrediction.hash
                +"｜每期最多一次｜不限制合格期频率｜不声明未来胜率";
        decision.reasonText = decision.reasonSummary;
        Models.Trade trade = new Models.Trade();
        trade.applyDecision(decision);
        trade.baseIssue = selected.context.baseIssue;
        trade.targetIssue = target;
        trade.position = action.position;
        trade.setNumbers(action.n1, action.n2, action.n3);
        trade.rawP = action.rawActionP;
        trade.modelP = action.rankScore;
        trade.calibratedP = exactP;
        trade.finalP = exactP;
        trade.dynamicGate = 0.0d;
        trade.edge = 0.0d;
        trade.buildId = V51BuildManifest.BUILD_ID;
        trade.cohortId = V51BuildManifest.COHORT_ID;
        trade.schemaVersion = V51BuildManifest.DATABASE_SCHEMA;
        trade.featureSchemaVersion = V51BuildManifest.FEATURE_SCHEMA;
        trade.rankingVersion = V51ActionRankerArtifact.ARTIFACT_ID;
        trade.calibrationVersion = V51SelectedCalibrationArtifact.ARTIFACT_ID;
        trade.gatePolicyVersion = V51PolicySurfaceArtifact.ARTIFACT_ID;
        trade.pipelineVersion = V51BuildManifest.VERSION_NAME;
        trade.contractDecisionId = result.authorized.decisionId;
        trade.candidateHash = result.contract.checksum;
        trade.snapshotMaxIssue = selected.context.baseIssue;
        trade.trainedUntilIssue = selected.context.baseIssue;
        trade.v46ContractHash = selected.contractHash;
        trade.preDrawReasonFingerprint = selected.contractHash;
        trade.authorizationId = result.authorized.authorizationHash;
        trade.authorizationValid = 1;
        trade.authorizedMode = "BET";
        trade.authorizationRejectCode = "";
        trade.authorizedAt = V51AuthorizationCore.authorizationTime(trade.authorizationId);
        trade.authorizationProbability = exactP;
        trade.authorizationGate = 0.0d;
        trade.authorizationEdge = 0.0d;
        trade.contractValid = 1;
        trade.contractStatus = ControlledRealContractAuditR922.FORMAL_EXACT_STATUS;
        trade.laneCandidate = V51Enums.Lane.CONTROLLED_PROBE.name();
        trade.action = AppConfig.DECISION_BET;
        trade.statScope = AppConfig.STAT_SCOPE_REAL_TRADE;
        trade.sourceType = Models.SOURCE_TYPE_REALTIME;
        trade.isRealtimeReal = 1;
        trade.createdAt = Math.max(1L, commitAt);
        trade.betTime = trade.createdAt;
        trade.settled = 0;
        trade.result = AppConfig.RESULT_WAIT;
        trade.hit = 0;
        trade.pnl = 0.0d;
        trade.reasonSummary = decision.reasonSummary;
        if (!DecisionAuthorizationEngine.isTradeAuthorized(trade))
            throw new IllegalStateException("R915_REAL_PERSISTED_CONTRACT_INVALID");
        long id = db.insertWithOnConflict(DbHelper.TABLE_TRADES, null,
                filterValuesForTable(db,DbHelper.TABLE_TRADES,tradeToValues(trade)),SQLiteDatabase.CONFLICT_ABORT);
        if (id <= 0L) throw new IllegalStateException("R915_REAL_INSERT_FAILED");
        if(result.contract.certifiedPrediction!=null){
            CertifiedPredictionR974.Proof p=result.contract.certifiedPrediction;
            db.execSQL("INSERT INTO r974_exact_real_receipt VALUES(?,?,?,?,?,?,?,?,?,?)",new Object[]{target,id,p.candidate,p.modelHash,p.certificate,p.hash,V51ChecksumEngine.sha256(p.recipe),p.actionHash,result.authorized.authorizationHash,System.currentTimeMillis()});
            com.ethk3.novaonepro.strategyv2.MainlineAdapterSU1.bind(db,p,result.authorized.authorizationHash);
            db.execSQL("INSERT INTO research_exact_probability_f05(game_code,target_issue,trade_id,candidate_id,model_hash,certificate_hash,calibration_hash,selection_hash,scope_policy_hash,selected_hit_p,proof_hash,action_hash,authorization_hash,captured_at,committed_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    new Object[]{V51Repository.GAME_CODE,target,id,p.candidate,p.modelHash,p.certificate,p.calibrationHash,p.selectionHash,p.scopePolicyHash,p.selectedHitP,p.hash,p.actionHash,result.authorized.authorizationHash,p.capturedAt,commitAt});
        }
        Cursor provenance = null;
        try {
            provenance = db.rawQuery("SELECT source_type,is_realtime_real FROM " + DbHelper.TABLE_TRADES
                    + " WHERE _id=?", new String[] {String.valueOf(id)});
            if (!provenance.moveToFirst() || !Models.SOURCE_TYPE_REALTIME.equals(provenance.getString(0))
                    || provenance.getInt(1) != 1)
                throw new IllegalStateException("R924_NEW_REAL_PROVENANCE_WRITE_FAILED");
        } finally { if (provenance != null) provenance.close(); }
        trade.id = id;
        return id;
    }

    public long saveTrade(Models.Trade t) {
        throw new IllegalStateException("CP4_LEGACY_TRADE_WRITER_RETIRED:saveTrade");
    }

    private void normalizeTradeBeforeSave(Models.Trade t) {
        if (t == null) {
            return;
        }
        if (V50BuildManifest.COHORT_ID.equals(t.cohortId == null ? "" : t.cohortId)) {
            normalizeV50TradeBeforeSave(t);
            return;
        }

        try {
            t.normalize();
        } catch (Throwable e) {
        }

        if (t.k3Mask <= 0 && AppConfig.isValidK3Numbers(t.n1, t.n2, t.n3)) {
            t.k3Mask = Models.buildK3Mask(t.n1, t.n2, t.n3);
        }

        if (t.createdAt <= 0L && t.betTime > 0L) {
            t.createdAt = t.betTime;
        }

        if (t.betTime <= 0L && t.createdAt > 0L) {
            t.betTime = t.createdAt;
        }

        if (t.betTime <= 0L) {
            t.betTime = System.currentTimeMillis();
            t.createdAt = t.betTime;
        }

        if (t.settledAt <= 0L && t.settleTime > 0L) {
            t.settledAt = t.settleTime;
        }

        if (t.settleTime <= 0L && t.settledAt > 0L) {
            t.settleTime = t.settledAt;
        }

        t.result = AppConfig.normalizeResult(t.result);

        if (t.result == null || t.result.length() == 0) {
            t.result = AppConfig.RESULT_WAIT;
        }

        t.action = AppConfig.DECISION_BET;
        t.statScope = AppConfig.STAT_SCOPE_REAL_TRADE;

        if (t.settled == 1 && AppConfig.isTerminalResult(t.result)) {
            t.hit = AppConfig.isWinResult(t.result) ? 1 : 0;
            t.pnl = pnlForHitLocal(t.hit == 1);

            if (t.settleTime <= 0L) {
                t.settleTime = System.currentTimeMillis();
            }

            if (t.settledAt <= 0L) {
                t.settledAt = t.settleTime;
            }
        }

        if (t.edge == 0.0d && t.finalP > 0.0d && t.dynamicGate > 0.0d) {
            t.edge = t.finalP - t.dynamicGate;
        }
    }

    private void normalizeV50TradeBeforeSave(Models.Trade t) {
        t.k3Mask = Models.buildK3Mask(t.n1, t.n2, t.n3);
        if (t.createdAt <= 0L && t.betTime > 0L) t.createdAt = t.betTime;
        if (t.betTime <= 0L && t.createdAt > 0L) t.betTime = t.createdAt;
        if (t.betTime <= 0L) {
            t.betTime = System.currentTimeMillis();
            t.createdAt = t.betTime;
        }
        if (t.settledAt <= 0L && t.settleTime > 0L) {
            t.settledAt = t.settleTime;
        }
        if (t.settleTime <= 0L && t.settledAt > 0L) {
            t.settleTime = t.settledAt;
        }
        t.rawP = AppConfig.clampProbability(t.rawP);
        t.modelP = AppConfig.clampProbability(t.modelP);
        t.calibratedP = AppConfig.clampProbability(t.calibratedP);
        t.finalP = AppConfig.clampProbability(t.finalP);
        t.dynamicGate = AppConfig.clampGate(t.dynamicGate);
        t.edge = t.finalP - t.dynamicGate;
        t.result = AppConfig.normalizeResult(t.result);
        if (t.result == null || t.result.length() == 0) {
            t.result = AppConfig.RESULT_WAIT;
        }
        t.action = AppConfig.ACTION_BET;
        t.statScope = AppConfig.STAT_SCOPE_REAL_TRADE;
        t.sourceType = Models.SOURCE_TYPE_REALTIME;
        t.isRealtimeReal = 1;
        if (t.settled == 1 && AppConfig.isTerminalResult(t.result)) {
            t.hit = AppConfig.isWinResult(t.result) ? 1 : 0;
            t.pnl = pnlForHitLocal(t.hit == 1);
            if (t.settleTime <= 0L) t.settleTime = System.currentTimeMillis();
            if (t.settledAt <= 0L) t.settledAt = t.settleTime;
        }
    }

    private void copySettlementFields(Models.Trade from, Models.Trade to) {
        if (from == null || to == null) {
            return;
        }

        to.settled = from.settled;
        to.result = from.result;
        to.actualNumber = from.actualNumber;
        to.hit = from.hit;
        to.pnl = from.pnl;
        to.missReason = from.missReason;
        to.settleTime = from.settleTime;
        to.settledAt = from.settledAt;
        to.settlementReason = from.settlementReason;
    }

    private void linkDecisionTradeAfterSave(SQLiteDatabase db, Models.Trade t) {
        if (db == null || t == null || t.id <= 0L) {
            return;
        }

        try {
            ContentValues dcv = new ContentValues();
            dcv.put("trade_id", t.id);
            dcv.put("stat_scope", AppConfig.STAT_SCOPE_REAL_TRADE);
            dcv.put("action", AppConfig.DECISION_BET);

            if (t.decisionId > 0L) {
                updateFiltered(
                        db,
                        DbHelper.TABLE_DECISIONS,
                        dcv,
                        "_id=?",
                        new String[] {String.valueOf(t.decisionId)});
                return;
            }

            updateFiltered(
                    db,
                    DbHelper.TABLE_DECISIONS,
                    dcv,
                    "target_issue=? AND decision=?",
                    new String[] {t.targetIssue, AppConfig.DECISION_BET});
        } catch (Throwable e) {
        }
    }

    public int updateTradeFull(Models.Trade t) {
        throw new IllegalStateException("CP4_LEGACY_TRADE_WRITER_RETIRED:updateTradeFull");
    }

    public int updateTrade(Models.Trade t) {
        throw new IllegalStateException("CP4_LEGACY_TRADE_WRITER_RETIRED:updateTrade");
    }

    public long replaceTradeFull(Models.Trade t) {
        throw new IllegalStateException("CP4_LEGACY_TRADE_WRITER_RETIRED:replaceTradeFull");
    }

    private void copyFieldChainFields(Object from, Object to) {
        if (from == null || to == null) {
            return;
        }
        copyFieldChainInt(from, to, "teacherWeakNoBetFlag");
        copyFieldChainInt(from, to, "teacherMatrixWeakNoBetFlag");
        copyFieldChainInt(from, to, "teacherMatrixOrdinaryBetBlockFlag");
        copyFieldChainInt(from, to, "teacherMatrixSupportsFinalGate");
        copyFieldChainInt(from, to, "teacherMatrixSupportsBet");
        copyFieldChainString(from, to, "teacherOrdinaryBetPolicy");
        copyFieldChainString(from, to, "teacherMatrixOrdinaryBetPolicy");
        copyFieldChainString(from, to, "teacherFieldSource");
        copyFieldChainString(from, to, "teacherFieldVersion");
        copyFieldChainString(from, to, "teacherMatrixStatus");
        copyFieldChainString(from, to, "teacherNumberStatus");
        copyFieldChainString(from, to, "teacherStatus");
        copyFieldChainDouble(from, to, "teacherEvidenceContribution");
        copyFieldChainDouble(from, to, "teacherMatrixEvidenceContribution");
        copyFieldChainDouble(from, to, "teacherCounterContribution");
        copyFieldChainDouble(from, to, "teacherMatrixCounterContribution");
        copyFieldChainDouble(from, to, "teacherSampleReliability");
        copyFieldChainDouble(from, to, "teacherRecentValidity");

        copyFieldChainString(from, to, "v20Status");
        copyFieldChainString(from, to, "dynamicStatus");
        copyFieldChainString(from, to, "dynamicCauseStatus");
        copyFieldChainDouble(from, to, "v20SupportScore");
        copyFieldChainDouble(from, to, "v20RiskScore");
        copyFieldChainDouble(from, to, "v20EvidenceContribution");
        copyFieldChainDouble(from, to, "v20CounterContribution");
        copyFieldChainDouble(from, to, "v20SampleReliability");
        copyFieldChainDouble(from, to, "v20RecentValidity");
        copyFieldChainString(from, to, "v20ActionAdvice");
        copyFieldChainString(from, to, "v20WindowStateKey");
        copyFieldChainString(from, to, "windowStateKey");
        copyFieldChainString(from, to, "finalDecisionWindowStateKey");
        copyFieldChainString(from, to, "dynamicWindowStateKey");
        copyFieldChainString(from, to, "probabilityWindowStateKey");

        copyFieldChainDouble(from, to, "coverageQualityScore");
        copyFieldChainDouble(from, to, "thirdNumberQualityScore");
        copyFieldChainDouble(from, to, "k3CoverageRisk");
        copyFieldChainInt(from, to, "falseHighPFlag");
        copyFieldChainInt(from, to, "probabilityFalseHighPFlag");
        copyFieldChainDouble(from, to, "probabilityFalseHighRisk");
        copyFieldChainInt(from, to, "probabilityPreferredEdgeBand");
        copyFieldChainInt(from, to, "probabilityTeacherWeakOrEmpty");
        copyFieldChainInt(from, to, "probabilityV20UnknownOrEmpty");
        copyFieldChainInt(from, to, "probabilityStrongCauseFlag");
        copyFieldChainString(from, to, "probabilityCoverageSummary");
        copyFieldChainString(from, to, "probabilityActionAdvice");

        copyFieldChainInt(from, to, "dynamicGateFalseHighPFlag");
        copyFieldChainInt(from, to, "dynamicGatePreferredEdgeBand");
        copyFieldChainString(from, to, "dynamicGateEdgePolicy");
        copyFieldChainDouble(from, to, "dynamicGateCoverageRisk");
        copyFieldChainDouble(from, to, "dynamicGateCoverageQualityScore");
        copyFieldChainDouble(from, to, "dynamicGateThirdNumberQuality");
        copyFieldChainDouble(from, to, "dynamicGateVirtualEdgeRisk");
        copyFieldChainString(from, to, "dynamicGateVersion");

        copyFieldChainString(from, to, "finalDecisionVersion");
        copyFieldChainString(from, to, "finalDecisionEdgePolicy");
        copyFieldChainString(from, to, "finalDecisionTeacherPolicy");
        copyFieldChainString(from, to, "finalDecisionV20Policy");
        copyFieldChainString(from, to, "finalDecisionBetDomain");
        copyFieldChainString(from, to, "finalDecisionRejectCode");
        copyFieldChainString(from, to, "finalDecisionShadowReason");
        copyFieldChainDouble(from, to, "finalDecisionCoverageRisk");
        copyFieldChainDouble(from, to, "finalDecisionThirdNumberQuality");
        copyFieldChainInt(from, to, "finalDecisionFalseHighPFlag");
        syncFieldChainAliases(to);
    }

    private void copyFieldChainInt(Object from, Object to, String field) {
        setOptionalIntField(to, field, getOptionalIntField(from, field, 0));
    }

    private void copyFieldChainDouble(Object from, Object to, String field) {
        setOptionalDoubleField(to, field, getOptionalDoubleField(from, field, 0.0d));
    }

    private void copyFieldChainString(Object from, Object to, String field) {
        setOptionalStringField(to, field, getOptionalStringField(from, field, ""));
    }

    private boolean isFieldChainUnsafeRealBetDecision(Models.Decision d) {
        if (d == null || !AppConfig.DECISION_BET.equals(AppConfig.normalizeDecision(d.decision))) {
            return false;
        }
        syncFieldChainAliases(d);
        String reject = getOptionalStringField(d, "finalDecisionRejectCode", "");
        if (reject != null
                && reject.length() > 0
                && !containsFieldChainText(reject, "PASS")
                && !containsFieldChainText(reject, "READY")) {
            return true;
        }
        int teacherBlock =
                maxOptionalInt(
                        d,
                        new String[] {
                            "teacherWeakNoBetFlag",
                            "teacherMatrixWeakNoBetFlag",
                            "teacherMatrixOrdinaryBetBlockFlag"
                        });
        String teacherPolicy =
                getOptionalStringField(
                        d,
                        "teacherOrdinaryBetPolicy",
                        getOptionalStringField(d, "teacherMatrixOrdinaryBetPolicy", ""));
        String teacherStatus =
                getOptionalStringField(
                        d, "teacherMatrixStatus", getOptionalStringField(d, "teacherStatus", ""));
        String v20Status =
                getOptionalStringField(
                        d, "v20Status", getOptionalStringField(d, "dynamicStatus", ""));
        double edge = d.edge;
        if ((Double.isNaN(edge) || Double.isInfinite(edge) || Math.abs(edge) > 1.0d)
                && d.finalP > 0.0d
                && d.dynamicGate > 0.0d) {
            edge = d.finalP - d.dynamicGate;
        }
        double coverRisk =
                maxOptionalDouble(
                        d,
                        new String[] {
                            "k3CoverageRisk", "dynamicGateCoverageRisk", "finalDecisionCoverageRisk"
                        });
        double coverQuality =
                maxOptionalDouble(
                        d,
                        new String[] {"coverageQualityScore", "dynamicGateCoverageQualityScore"});
        double third =
                maxOptionalDouble(
                        d,
                        new String[] {
                            "thirdNumberQualityScore",
                            "dynamicGateThirdNumberQuality",
                            "finalDecisionThirdNumberQuality"
                        });
        int falseHigh =
                maxOptionalInt(
                        d,
                        new String[] {
                            "falseHighPFlag",
                            "probabilityFalseHighPFlag",
                            "dynamicGateFalseHighPFlag",
                            "finalDecisionFalseHighPFlag"
                        });
        boolean teacherWeak =
                teacherBlock > 0
                        || containsFieldChainText(teacherPolicy, "WEAK_NO_BET")
                        || weakTeacherText(teacherStatus);
        boolean v20Weak = weakV20Text(v20Status);
        boolean coverEmpty = coverQuality <= 0.0001d && coverRisk <= 0.0001d && third <= 0.0001d;
        if (teacherBlock > 0 || containsFieldChainText(teacherPolicy, "WEAK_NO_BET")) {
            return true;
        }
        if (falseHigh > 0 && d.finalP < 0.425d) {
            return true;
        }
        if (coverRisk >= FIELD_CHAIN_K3_COVERAGE_HARD_RISK && d.finalP < 0.430d) {
            return true;
        }
        if (coverRisk >= FIELD_CHAIN_K3_COVERAGE_RISK_LINE && d.finalP < 0.420d) {
            return true;
        }
        if (third > 0.0d && third < FIELD_CHAIN_THIRD_NUMBER_MIN && d.finalP < 0.420d) {
            return true;
        }
        if (teacherWeak && v20Weak && d.finalP < FIELD_CHAIN_P_NO_STRONG_CAUSE_LINE) {
            return true;
        }
        if (teacherWeak && v20Weak && edge > FIELD_CHAIN_EDGE_FALSE_STRONG_LINE) {
            return true;
        }
        if (coverEmpty && teacherWeak && v20Weak && d.finalP < 0.425d) {
            return true;
        }
        return false;
    }

    private boolean weakTeacherText(String text) {
        if (text == null || text.length() == 0 || "空".equals(text)) {
            return true;
        }
        String s = text.toUpperCase(java.util.Locale.ROOT);
        return s.indexOf("STRONG") < 0
                && s.indexOf("READY") < 0
                && s.indexOf("PASS") < 0
                && s.indexOf("RISK") < 0
                && s.indexOf("FREEZE") < 0;
    }

    private boolean weakV20Text(String text) {
        if (text == null || text.length() == 0 || "空".equals(text)) {
            return true;
        }
        String s = text.toUpperCase(java.util.Locale.ROOT);
        return s.indexOf("STRONG") < 0
                && s.indexOf("READY") < 0
                && s.indexOf("PASS") < 0
                && !"S".equals(s)
                && !"A".equals(s);
    }

    private boolean containsFieldChainText(String text, String key) {
        if (text == null || key == null || key.length() == 0) {
            return false;
        }
        return text.toUpperCase(java.util.Locale.ROOT).indexOf(key.toUpperCase(java.util.Locale.ROOT)) >= 0;
    }

    private ContentValues tradeToValues(Models.Trade t) {
        ContentValues cv = new ContentValues();

        cv.put("base_issue", t.baseIssue);
        cv.put("target_issue", t.targetIssue);
        cv.put("bet_time", t.betTime);
        cv.put("created_at", t.createdAt);
        cv.put("settle_time", t.settleTime);
        cv.put("settled_at", t.settledAt);
        cv.put("decision_id", t.decisionId);
        cv.put("action", t.action);

        cv.put("position", t.position);
        cv.put("n1", t.n1);
        cv.put("n2", t.n2);
        cv.put("n3", t.n3);
        cv.put("k3_mask", t.k3Mask);

        cv.put("raw_p", t.rawP);
        cv.put("calibrated_p", t.calibratedP);
        cv.put("final_p", t.finalP);
        cv.put("dynamic_gate", t.dynamicGate);
        cv.put("edge", t.edge);

        cv.put("model_p", t.modelP);
        cv.put("shape_p", t.shapeP);
        cv.put("combo_p", t.comboP);
        cv.put("recovery_p", t.recoveryP);
        cv.put("similar_p", t.similarP);
        cv.put("memory_p", t.memoryP);
        cv.put("calibration_error", t.calibrationError);

        cv.put("similar_hit_rate", t.similarHitRate);
        cv.put("combo_hit_rate", t.comboHitRate);
        cv.put("effective_sample", t.effectiveSample);
        cv.put("combo_sample", t.comboSample);

        cv.put("trend_life", t.trendLife);
        cv.put("drift_risk", t.driftRisk);
        cv.put("break_depth", t.breakDepth);
        cv.put("recovery_score", t.recoveryScore);
        cv.put("champion_gap", t.championGap);
        cv.put("combo_gap", t.comboGap);
        cv.put("hot_reverse_risk", t.hotReverseRisk);
        cv.put("false_strong_risk", t.falseStrongRisk);

        cv.put("state_mode", t.stateMode);
        cv.put("shape_key", t.shapeKey);
        cv.put("reason_summary", t.reasonSummary);

        cv.put("result", t.result);
        cv.put("actual_number", t.actualNumber);
        cv.put("hit", t.hit);
        cv.put("pnl", t.pnl);
        cv.put("miss_reason", t.missReason);
        cv.put("settlement_reason", t.settlementReason);
        cv.put("settled", t.settled);

        cv.put("deep_score", t.deepScore);
        cv.put("deep_confidence", t.deepConfidence);
        cv.put("deep_feature_p", t.deepFeatureP);
        cv.put("deep_position_score", t.deepPositionScore);
        cv.put("deep_number_score", t.deepNumberScore);
        cv.put("deep_cycle_score", t.deepCycleScore);
        cv.put("deep_transition_score", t.deepTransitionScore);
        cv.put("deep_state_score", t.deepStateScore);
        cv.put("ai_judge_score", t.aiJudgeScore);
        cv.put("ai_judge_confidence", t.aiJudgeConfidence);
        cv.put("evidence_score", t.evidenceScore);
        cv.put("evidence_risk", t.evidenceRisk);
        cv.put("bias_score", t.biasScore);
        cv.put("bias_confidence", t.biasConfidence);
        cv.put("audit_penalty", t.auditPenalty);
        cv.put("audit_boost", t.auditBoost);
        cv.put("profile_score", t.profileScore);
        cv.put("fallback_profile_score", t.fallbackProfileScore);
        cv.put("a_plus_score", t.aPlusScore);
        cv.put("promotion_score", t.promotionScore);
        cv.put("conflict_level", t.conflictLevel);
        cv.put("weakest_number_score", t.weakestNumberScore);
        cv.put("balance_score", t.balanceScore);

        cv.put("deep_profile_key", t.deepProfileKey);
        cv.put("deep_profile_hit_rate", t.deepProfileHitRate);
        cv.put("deep_profile_sample", t.deepProfileSample);
        cv.put("deep_fallback_level", t.deepFallbackLevel);
        cv.put("fallback_profile_key", t.fallbackProfileKey);
        cv.put("promotion_status", t.promotionStatus);
        cv.put("a_plus_status", t.aPlusStatus);
        cv.put("candidate_evidence_key", t.candidateEvidenceKey);
        cv.put("snapshot_id", t.snapshotId);

        cv.put("attribution_reason_code", t.attributionReasonCode);
        cv.put("attribution_text", t.attributionText);
        cv.put("affected_module", t.affectedModule);
        cv.put("stat_scope", AppConfig.STAT_SCOPE_REAL_TRADE);

        cv.put("state_key", firstNonEmpty(t.stateKey, safeTradeStateKey(t)));
        cv.put(
                "runtime_profile_score",
                t.runtimeProfileScore != 0.0d ? t.runtimeProfileScore : t.profileScore);
        cv.put(
                "state_number_profile_score",
                t.stateNumberProfileScore != 0.0d ? t.stateNumberProfileScore : t.biasScore);
        cv.put(
                "teacher_weight",
                t.teacherWeight > 0.0d
                        ? t.teacherWeight
                        : (t.aiJudgeConfidence > 0.0d ? t.aiJudgeConfidence : 1.0d));

        putTeacherMatrixFieldsFromObject(cv, t, safeTradeStateKey(t), t.position, t.k3Mask);

        putFieldChainValues(cv, t);
        putDynamicCausalValues(cv, t);
        putMainChainClosedLoopValues(cv, t);
        putFinalVersionFallback(cv, t);
        putV24ContractValues(cv, t);

        // R924 typed provenance survives the legacy reflection field bridge.
        cv.put("source_type", t.sourceType);
        cv.put("is_realtime_real", t.isRealtimeReal);

        return cv;
    }

    public Models.Trade getOpenTrade() {
        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    DbHelper.TABLE_TRADES,
                                    null,
                                    "settled=0 AND (action=? OR action IS NULL OR action='' OR"
                                        + " action='REAL' OR action='REAL_TRADE') AND (stat_scope=?"
                                        + " OR stat_scope IS NULL OR stat_scope='' OR"
                                        + " stat_scope='REAL')",
                                    new String[] {
                                        AppConfig.DECISION_BET, AppConfig.STAT_SCOPE_REAL_TRADE
                                    },
                                    null,
                                    null,
                                    "bet_time ASC, created_at ASC, _id ASC",
                                    "1");

            if (c.moveToFirst()) {
                Models.Trade t = readTrade(c);

                if (t != null && isValidRealTradeLoose(t)) {
                    return t;
                }
            }
        } finally {
            closeCursor(c);
        }

        return null;
    }

    public boolean hasOpenRealTrade() {
        return getOpenTrade() != null;
    }

    public List<Models.Trade> getOpenTrades(int limit) {
        ArrayList<Models.Trade> list = new ArrayList<Models.Trade>();

        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    DbHelper.TABLE_TRADES,
                                    null,
                                    "settled=0 AND (action=? OR action IS NULL OR action='' OR"
                                        + " action='REAL' OR action='REAL_TRADE') AND (stat_scope=?"
                                        + " OR stat_scope IS NULL OR stat_scope='' OR"
                                        + " stat_scope='REAL')",
                                    new String[] {
                                        AppConfig.DECISION_BET, AppConfig.STAT_SCOPE_REAL_TRADE
                                    },
                                    null,
                                    null,
                                    "bet_time ASC, created_at ASC, _id ASC",
                                    limit > 0 ? String.valueOf(limit) : null);

            while (c.moveToNext()) {
                Models.Trade t = readTrade(c);

                if (t == null) {
                    continue;
                }

                if (!isValidRealTradeLoose(t)) {
                    continue;
                }

                list.add(t);
            }
        } finally {
            closeCursor(c);
        }

        return list;
    }

    public Models.Trade getTradeByTargetIssue(String targetIssue) {
        if (targetIssue == null || targetIssue.length() == 0) {
            return null;
        }

        return getTradeByTargetIssueLoose(readable(), targetIssue);
    }

    private Models.Trade getTradeByTargetIssueLoose(SQLiteDatabase db, String targetIssue) {
        if (db == null || targetIssue == null || targetIssue.length() == 0) {
            return null;
        }

        Cursor c = null;

        try {
            c =
                    db.query(
                            DbHelper.TABLE_TRADES,
                            null,
                            "target_issue=? AND (action=? OR action IS NULL OR action='' OR"
                                + " action='REAL' OR action='REAL_TRADE') AND (stat_scope=? OR"
                                + " stat_scope IS NULL OR stat_scope='' OR stat_scope='REAL')",
                            new String[] {
                                targetIssue, AppConfig.DECISION_BET, AppConfig.STAT_SCOPE_REAL_TRADE
                            },
                            null,
                            null,
                            "settled DESC, bet_time DESC, created_at DESC, _id DESC",
                            "1");

            if (c.moveToFirst()) {
                Models.Trade t = readTrade(c);

                if (t != null && isValidRealTradeLoose(t)) {
                    return t;
                }
            }
        } finally {
            closeCursor(c);
        }

        return null;
    }

    public Models.Trade loadLastTrade() {
        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    DbHelper.TABLE_TRADES,
                                    null,
                                    "(action=? OR action IS NULL OR action='' OR action='REAL' OR"
                                        + " action='REAL_TRADE') AND (stat_scope=? OR stat_scope IS"
                                        + " NULL OR stat_scope='' OR stat_scope='REAL')",
                                    new String[] {
                                        AppConfig.DECISION_BET, AppConfig.STAT_SCOPE_REAL_TRADE
                                    },
                                    null,
                                    null,
                                    "bet_time DESC, created_at DESC, _id DESC",
                                    "1");

            if (c.moveToFirst()) {
                return readTrade(c);
            }
        } finally {
            closeCursor(c);
        }

        return null;
    }

    private boolean isValidRealTrade(Models.Trade t) {
        if (t == null) {
            return false;
        }

        if (t.targetIssue == null || t.targetIssue.length() == 0) {
            return false;
        }

        if (!AppConfig.DECISION_BET.equals(t.action)) {
            return false;
        }

        if (!AppConfig.STAT_SCOPE_REAL_TRADE.equals(t.statScope)) {
            return false;
        }

        if (!AppConfig.isValidPosition(t.position)) {
            return false;
        }

        if (!Models.isValidK3Mask(t.k3Mask)) {
            return false;
        }

        return true;
    }

    private boolean isValidRealTradeLoose(Models.Trade t) {
        if (t == null) {
            return false;
        }

        if (t.targetIssue == null || t.targetIssue.length() == 0) {
            return false;
        }

        if (!AppConfig.isValidPosition(t.position)) {
            return false;
        }

        if (t.k3Mask <= 0 && AppConfig.isValidK3Numbers(t.n1, t.n2, t.n3)) {
            t.k3Mask = Models.buildK3Mask(t.n1, t.n2, t.n3);
        }

        if (!Models.isValidK3Mask(t.k3Mask)) {
            return false;
        }

        String action = AppConfig.normalizeAction(t.action);

        if (action == null
                || action.length() == 0
                || "REAL".equals(action)
                || "REAL_TRADE".equals(action)) {
            action = AppConfig.DECISION_BET;
        }

        String scope = t.statScope;

        if (scope == null || scope.length() == 0 || "REAL".equals(scope)) {
            scope = AppConfig.STAT_SCOPE_REAL_TRADE;
        }

        return AppConfig.DECISION_BET.equals(action)
                && AppConfig.STAT_SCOPE_REAL_TRADE.equals(scope);
    }

    public int settleTrade(Models.Trade t) {
        if(t==null || t.targetIssue==null)return 0;
        Models.Draw draw=getDrawByIssue(t.targetIssue);
        if(draw==null || !draw.isValid())return 0;
        SettlementEngine.SettlementReport report=SettlementCoordinatorR815.settleNow(
                this,draw,SettlementCoordinatorR815.Reason.LEGACY_API);
        return report==null?0:report.settledCount;
    }

    public int settleTradeByDraw(Models.Trade t, Models.Draw draw) {
        if(t==null || draw==null || !draw.isValid() || !draw.issue.equals(t.targetIssue))return 0;
        SettlementEngine.SettlementReport report=SettlementCoordinatorR815.settleNow(
                this,draw,SettlementCoordinatorR815.Reason.LEGACY_API);
        return report==null?0:report.settledCount;
    }

    private boolean isTradeK3HitStrict(Models.Trade t, int actual) {
        if (t == null || !AppConfig.isValidNumber(actual)) {
            return false;
        }

        if (!AppConfig.isValidNumber(t.n1)
                || !AppConfig.isValidNumber(t.n2)
                || !AppConfig.isValidNumber(t.n3)) {
            return false;
        }

        return actual == t.n1 || actual == t.n2 || actual == t.n3;
    }

    private int getActualNumberByPosition(Models.Draw draw, int position) {
        if (draw == null || !AppConfig.isValidPosition(position)) {
            return 0;
        }

        try {
            if (draw.numbers == null || draw.numbers.length < 10) {
                return 0;
            }

            return draw.numbers[position - 1];
        } catch (Throwable e) {
            return 0;
        }
    }

    public int settleTradeByTargetIssue(String targetIssue, Models.Draw draw) {
        if (targetIssue == null || targetIssue.length() == 0 || draw == null) {
            return 0;
        }

        Models.Trade t = getTradeByTargetIssue(targetIssue);

        if (t == null) {
            return 0;
        }

        return settleTradeByDraw(t, draw);
    }

    public int settleOpenTradesByDraw(Models.Draw draw) {
        if (draw == null || draw.issue == null || draw.issue.length() == 0 || !draw.isValid()) {
            return 0;
        }

        List<Models.Trade> list = getOpenTrades(20);

        if (list == null || list.size() == 0) {
            return 0;
        }

        int count = 0;

        for (int i = 0; i < list.size(); i++) {
            Models.Trade t = list.get(i);

            if (t == null) {
                continue;
            }

            if (!draw.issue.equals(t.targetIssue)) {
                continue;
            }

            int r = settleTradeByDraw(t, draw);

            if (r > 0) {
                count += r;
            }
        }

        return count;
    }

    private void updateDecisionSettlementFromTrade(SQLiteDatabase db, Models.Trade t) {
        if (db == null || t == null || t.targetIssue == null || t.targetIssue.length() == 0) {
            return;
        }

        ContentValues dcv = new ContentValues();

        dcv.put("trade_id", t.id);
        dcv.put("settled", 1);
        dcv.put("result", AppConfig.normalizeResult(t.result));
        dcv.put("settle_time", t.settleTime);
        dcv.put("settled_at", t.settledAt);
        dcv.put("actual_number", t.actualNumber);
        dcv.put("hit", t.hit);
        dcv.put("pnl", t.pnl);
        dcv.put("miss_reason", t.missReason);
        dcv.put("settlement_reason", t.settlementReason);
        dcv.put("deep_attribution", t.attributionReasonCode);
        dcv.put("self_diagnosis_text", t.attributionText);
        dcv.put("stat_scope", AppConfig.STAT_SCOPE_REAL_TRADE);
        dcv.put("action", AppConfig.DECISION_BET);

        putTeacherMatrixFieldsFromObject(dcv, t, safeTradeStateKey(t), t.position, t.k3Mask);
        putFieldChainValues(dcv, t);
        putDynamicCausalValues(dcv, t);
        putFinalVersionFallback(dcv, t);

        if (t.decisionId > 0L) {
            updateFiltered(
                    db,
                    DbHelper.TABLE_DECISIONS,
                    dcv,
                    "_id=?",
                    new String[] {String.valueOf(t.decisionId)});
            return;
        }

        updateFiltered(
                db,
                DbHelper.TABLE_DECISIONS,
                dcv,
                "target_issue=? AND decision=?",
                new String[] {t.targetIssue, AppConfig.DECISION_BET});
    }

    public void saveShadowCandidates(
            String targetIssue, ArrayList<Models.ComboCandidate> candidates) {
        saveShadowCandidates("", targetIssue, candidates, AppConfig.SHADOW_SAVE_TOP_N);
    }

    public void saveShadowCandidates(String targetIssue, List<Models.ComboCandidate> candidates) {
        saveShadowCandidates("", targetIssue, candidates, AppConfig.SHADOW_SAVE_TOP_N);
    }

    public void saveShadowCandidates(
            String baseIssue,
            String targetIssue,
            List<Models.ComboCandidate> candidates,
            int limit) {
        try (DatabaseCoordinator.WriterGuardCP3 cp3WriterGuard3 = DatabaseCoordinator.acquireWriterGuardCP3()) {
            saveMainChainCandidateSnapshot(
                    baseIssue, targetIssue, candidates, MAIN_CHAIN_REALTIME_TOP40_LIMIT);
            if (targetIssue == null || targetIssue.length() == 0 || candidates == null) {
                return;
            }

            SQLiteDatabase db = writable();
            ensureRuntimeExtraTablesOnce(db);

            db.beginTransaction();

            try {
                db.delete(DbHelper.TABLE_SHADOW, "target_issue=?", new String[] {targetIssue});

                int max = limit <= 0 ? candidates.size() : Math.min(limit, candidates.size());
                long now = System.currentTimeMillis();

                for (int i = 0; i < max; i++) {
                    Models.ComboCandidate c = candidates.get(i);

                    if (c == null || !c.isValidK3()) {
                        continue;
                    }

                    attachTeacherMatrixFieldsToCandidate(c);
                    ensureSafePreDrawPromotionDomain(c);

                    ContentValues cv = comboCandidateToValues(c);

                    cv.put(
                            "base_issue",
                            baseIssue == null || baseIssue.length() == 0 ? c.baseIssue : baseIssue);
                    cv.put("target_issue", targetIssue);
                    cv.put("created_at", now);
                    cv.put("rank_no", i + 1);
                    cv.put("learned", 0);

                    insertFiltered(db, DbHelper.TABLE_SHADOW, cv, SQLiteDatabase.CONFLICT_NONE);
                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    private ContentValues comboCandidateToValues(Models.ComboCandidate c) {
        normalizeClosedLoopEvidenceAliases(c);
        ensureSafePreDrawPromotionDomain(c);
        ContentValues cv = new ContentValues();

        cv.put("base_issue", c.baseIssue);
        cv.put("target_issue", c.targetIssue);
        cv.put("created_at", c.createdAt);
        cv.put("rank_no", c.rank);

        cv.put("position", c.position);
        cv.put("n1", c.n1);
        cv.put("n2", c.n2);
        cv.put("n3", c.n3);
        cv.put("k3_mask", c.k3Mask);

        cv.put("shape_key", c.shapeKey);
        cv.put("state_mode", c.stateMode);

        cv.put("model_p", c.modelP);
        cv.put("shape_p", c.shapeP);
        cv.put("combo_p", c.comboP);
        cv.put("recovery_p", c.recoveryP);
        cv.put("similar_p", c.similarP);
        cv.put("memory_p", c.memoryP);

        cv.put("raw_p", c.rawP);
        cv.put("calibrated_p", c.calibratedP);
        cv.put("final_p", c.finalP);
        cv.put("dynamic_gate", c.dynamicGate);
        cv.put("edge", c.edge);

        cv.put("similar_hit_rate", c.similarHitRate);
        cv.put("combo_hit_rate", c.comboHitRate);
        cv.put("effective_sample", c.effectiveSample);
        cv.put("combo_sample", c.comboSample);

        cv.put("trend_life", c.trendLife);
        cv.put("drift_risk", c.driftRisk);
        cv.put("break_depth", c.breakDepth);
        cv.put("recovery_score", c.recoveryScore);
        cv.put("champion_gap", c.championGap);
        cv.put("combo_gap", c.comboGap);
        cv.put("hot_reverse_risk", c.hotReverseRisk);
        cv.put("false_strong_risk", c.falseStrongRisk);

        cv.put("calibration_error", c.calibrationError);
        cv.put("probability_mode", c.probabilityMode);
        cv.put("backtest_mode", c.backtestMode);

        cv.put("deep_score", c.deepScore);
        cv.put("deep_confidence", c.deepConfidence);
        cv.put("deep_position_score", c.deepPositionScore);
        cv.put("deep_number_score", c.deepNumberScore);
        cv.put("deep_cycle_score", c.deepCycleScore);
        cv.put("deep_transition_score", c.deepTransitionScore);
        cv.put("deep_state_score", c.deepStateScore);
        cv.put("bias_score", c.biasScore);
        cv.put("bias_confidence", c.biasConfidence);
        cv.put("profile_score", c.profileScore);
        cv.put("fallback_profile_score", c.fallbackProfileScore);
        cv.put("audit_penalty", c.auditPenalty);
        cv.put("audit_boost", c.auditBoost);
        cv.put("promotion_score", c.promotionScore);
        cv.put("a_plus_score", c.aPlusScore);
        cv.put("evidence_score", c.evidenceScore);
        cv.put("evidence_risk", c.evidenceRisk);
        cv.put("ai_judge_score", c.aiJudgeScore);
        cv.put("ai_judge_confidence", c.aiJudgeConfidence);
        cv.put("weakest_number_score", c.weakestNumberScore);
        cv.put("balance_score", c.balanceScore);

        cv.put("deep_profile_key", c.deepProfileKey);
        cv.put("fallback_profile_key", c.fallbackProfileKey);
        cv.put("candidate_evidence_key", c.candidateEvidenceKey);
        cv.put("promotion_status", c.promotionStatus);
        cv.put("a_plus_status", c.aPlusStatus);
        cv.put("deep_reason", c.deepReason);

        cv.put("state_key", firstNonEmpty(c.stateKey, safeStateKeyFromCandidate(c)));
        cv.put(
                "runtime_profile_score",
                c.runtimeProfileScore != 0.0d ? c.runtimeProfileScore : c.profileScore);
        cv.put(
                "state_number_profile_score",
                c.stateNumberProfileScore != 0.0d ? c.stateNumberProfileScore : c.biasScore);
        cv.put("number_bias", c.numberBias != 0.0d ? c.numberBias : c.biasScore);
        cv.put("combo_bias", c.comboBias != 0.0d ? c.comboBias : c.profileScore);
        cv.put(
                "teacher_weight",
                c.teacherWeight > 0.0d
                        ? c.teacherWeight
                        : (c.aiJudgeConfidence > 0.0d ? c.aiJudgeConfidence : 1.0d));
        cv.put(
                "state_confidence",
                c.stateConfidence != 0.0d ? c.stateConfidence : c.deepConfidence);
        cv.put("state_sample", c.stateSample != 0 ? c.stateSample : c.effectiveSample);
        cv.put("cache_sample", c.cacheSample != 0 ? c.cacheSample : c.comboSample);
        cv.put("recent_sample", c.recentSample);
        cv.put("recent_hit_rate", c.recentHitRate);

        putTeacherMatrixFieldsFromObject(cv, c, safeStateKeyFromCandidate(c), c.position, c.k3Mask);

        putFieldChainValues(cv, c);
        putDynamicCausalValues(cv, c);
        putMainChainClosedLoopValues(cv, c);
        putShadowDynamicBridgeAliases(cv, c);

        return cv;
    }

    private void putMainChainClosedLoopValues(ContentValues cv, Object obj) {
        if (cv == null || obj == null) return;
        try {
            Models.normalizeMainChainBridgeFields(obj);
        } catch (Throwable e) {
        }
        String[][] textMap = mainChainTextMap();
        for (int i = 0; i < textMap.length; i++)
            putStringField(cv, obj, textMap[i][0], textMap[i][1]);
        String[][] intMap = mainChainIntMap();
        for (int i = 0; i < intMap.length; i++) putIntField(cv, obj, intMap[i][0], intMap[i][1]);
        String[][] doubleMap = mainChainDoubleMap();
        for (int i = 0; i < doubleMap.length; i++)
            putDoubleRawField(cv, obj, doubleMap[i][0], doubleMap[i][1]);
        cv.put(
                "teacher_snapshot_created_at",
                getOptionalLongField(obj, "teacherSnapshotCreatedAt", 0L));
        putMainChainAliases(cv, obj);
        putLinkModificationV2Values(cv, obj);
        cv.put("field_chain_version", DBSTORE_MAIN_CHAIN_CLOSED_LOOP_VERSION);
        cv.put("schema_fix_version", DBSTORE_MAIN_CHAIN_CLOSED_LOOP_VERSION);
    }

    private void readMainChainClosedLoopFields(Cursor c, Object obj) {
        if (c == null || obj == null) return;
        String[][] textMap = mainChainTextMap();
        for (int i = 0; i < textMap.length; i++)
            setStringField(obj, textMap[i][1], getString(c, textMap[i][0]));
        String[][] intMap = mainChainIntMap();
        for (int i = 0; i < intMap.length; i++)
            setIntField(obj, intMap[i][1], getInt(c, intMap[i][0]));
        String[][] doubleMap = mainChainDoubleMap();
        for (int i = 0; i < doubleMap.length; i++)
            setDoubleRawField(obj, doubleMap[i][1], getDouble(c, doubleMap[i][0]));
        setOptionalLongField(
                obj, "teacherSnapshotCreatedAt", getLong(c, "teacher_snapshot_created_at"));
        readLinkModificationV2Values(c, obj);
        try {
            Models.normalizeMainChainBridgeFields(obj);
        } catch (Throwable e) {
        }
    }

    private String[][] mainChainTextMap() {
        return new String[][] {
            {"snapshot_key", "snapshotId"},
            {"candidate_key", "candidateEvidenceKey"},
            {"window_state_key", "windowStateKey"},
            {"dynamic_window_state_key", "dynamicWindowStateKey"},
            {"final_decision_window_state_key", "finalDecisionWindowStateKey"},
            {"probability_window_state_key", "probabilityWindowStateKey"},
            {"cluster_key", "clusterKey"},
            {"factor_key", "factorKey"},
            {"teacher_status", "teacherStatus"},
            {"teacher_status_class", "teacherStatusClass"},
            {"teacher_number_status", "teacherNumberStatus"},
            {"v20_status", "v20Status"},
            {"v20_action_advice", "v20ActionAdvice"},
            {"adaptive_status", "adaptiveStatus"},
            {"shadow_promotion_status", "shadowPromotionStatus"},
            {"reject_code", "rejectCode"},
            {"real_probe_reason", "realProbeReason"},
            {"teacher_number_history_sample", "teacherNumberHistorySample"},
            {"teacher_domain_shadow_sample", "teacherDomainShadowSample"},
            {"teacher_domain_real_sample", "teacherDomainRealSample"},
            {"teacher_apply_error", "teacherApplyError"},
            {"predecision_domain_version", "preDecisionDomainVersion"},
            {"teacher_runtime_apply_version", "teacherRuntimeApplyVersion"},
            {"teacher_as_of_issue", "teacherAsOfIssue"},
            {"block_tier", "blockTier"},
            {"hard_risk_code", "hardRiskCode"},
            {"observe_reason", "observeReason"},
            {"risk_severity", "riskSeverity"},
            {"hard_block_source", "hardBlockSource"},
            {"evidence_source", "evidenceSource"},
            {"promotion_coarse_domain", "promotionCoarseDomain"},
            {"promotion_mid_domain", "promotionMidDomain"},
            {"promotion_fine_domain", "promotionFineDomain"},
            {"probe_level", "probeLevel"},
            {"promotion_eligibility_reason", "promotionEligibilityReason"},
            {"bet_source_engine", "betSourceEngine"},
            {"decision_gate_version", "finalDecisionVersion"},
            {"field_chain_version", "fieldChainVersion"},
            {"schema_fix_version", "schemaFixVersion"}
        };
    }

    private String[][] mainChainIntMap() {
        return new String[][] {
            {"candidate_rank", "rank"},
            {"realtime_top40_rank", "realtimeTop40Rank"},
            {"adaptive_input_flag", "adaptiveInputFlag"},
            {"similar_sample_count", "similarSampleCount"},
            {"real_probe_allowed", "realProbeAllowed"},
            {"teacher_issue_lag", "teacherIssueLag"},
            {"teacher_fresh_by_issue", "teacherFreshByIssue"},
            {"probe_eligible", "probeEligible"},
            {"shadow_probe_eligible", "shadowProbeEligible"},
            {"shadow_settled_count", "shadowSettledCount"},
            {"shadow_domain_sample", "shadowDomainSample"},
            {"predecision_opportunity", "preDecisionOpportunity"},
            {"hard_block_flag", "hardBlockFlag"},
            {"evidence_settled_only", "evidenceSettledOnly"},
            {"evidence_sample_count", "evidenceSampleCount"},
            {"evidence_hit_count", "evidenceHitCount"},
            {"evidence_miss_count", "evidenceMissCount"}
        };
    }

    private String[][] mainChainDoubleMap() {
        return new String[][] {
            {"position_need_score", "positionNeedScore"},
            {"number_supply_score", "numberSupplyScore"},
            {"coverage_quality", "coverageQuality"},
            {"third_quality", "thirdQuality"},
            {"counter_evidence_score", "counterEvidenceScore"},
            {"similar_hit_rate", "similarHitRate"},
            {"teacher_sample_reliability", "teacherSampleReliability"},
            {"teacher_recent_validity", "teacherRecentValidity"},
            {"teacher_evidence_contribution", "teacherEvidenceContribution"},
            {"teacher_counter_contribution", "teacherCounterContribution"},
            {"v20_sample_reliability", "v20SampleReliability"},
            {"v20_recent_validity", "v20RecentValidity"},
            {"v20_evidence_contribution", "v20EvidenceContribution"},
            {"v20_counter_contribution", "v20CounterContribution"},
            {"final_p_before_diagnosis", "finalPBeforeDiagnosis"},
            {"final_p_after_diagnosis", "finalPAfterDiagnosis"},
            {"gate", "gate"},
            {"edge", "edge"},
            {"promotion_wilson_lower_bound", "promotionWilsonLowerBound"}
        };
    }

    private void putMainChainAliases(ContentValues cv, Object obj) {
        try {
            String targetIssue =
                    firstNonEmpty(
                            getStringField(obj, "targetIssue"), getStringField(obj, "target"), "");
            int position = getIntField(obj, "position");
            int n1 = getIntField(obj, "n1");
            int n2 = getIntField(obj, "n2");
            int n3 = getIntField(obj, "n3");
            int mask = getIntField(obj, "k3Mask");
            int rank = getIntField(obj, "rank");
            if (rank <= 0) rank = getIntField(obj, "candidateRank");
            if (rank > 0) cv.put("candidate_rank", rank);
            if (rank > 0 && rank <= MAIN_CHAIN_REALTIME_TOP40_LIMIT) {
                cv.put("realtime_top40_rank", rank);
                cv.put("adaptive_input_flag", 1);
            }
            String candidateKey =
                    firstNonEmpty(
                            getStringField(obj, "candidateEvidenceKey"),
                            getStringField(obj, "candidateKey"),
                            "");
            if (candidateKey.length() == 0) {
                candidateKey =
                        buildMainChainCandidateKey(targetIssue, position, n1, n2, n3, mask, rank);
            }
            cv.put("candidate_key", candidateKey);
            String snapshotKey =
                    firstNonEmpty(
                            getStringField(obj, "snapshotId"),
                            getStringField(obj, "snapshotKey"),
                            "");
            if (snapshotKey.length() == 0) snapshotKey = candidateKey;
            cv.put("snapshot_key", snapshotKey);
            String window =
                    firstNonEmpty(
                            getStringField(obj, "windowStateKey"),
                            firstNonEmpty(
                                    getStringField(obj, "finalDecisionWindowStateKey"),
                                    firstNonEmpty(
                                            getStringField(obj, "dynamicWindowStateKey"),
                                            getStringField(obj, "probabilityWindowStateKey"))));
            if (window.length() > 0) cv.put("window_state_key", window);
            String cluster =
                    firstNonEmpty(
                            getStringField(obj, "clusterKey"),
                            firstNonEmpty(
                                    getStringField(obj, "dynamicClusterId"),
                                    getStringField(obj, "dynamicClusterFeatureKey")));
            if (cluster.length() > 0) cv.put("cluster_key", cluster);
            String factor =
                    firstNonEmpty(
                            getStringField(obj, "factorKey"),
                            firstNonEmpty(
                                    getStringField(obj, "candidateEvidenceKey"),
                                    getStringField(obj, "needSupplyMatchKey")));
            if (factor.length() > 0) cv.put("factor_key", factor);
            double cover =
                    maxDouble(
                            getDoubleField(obj, "coverageQuality"),
                            getDoubleField(obj, "coverageQualityScore"),
                            getDoubleField(obj, "finalDecisionCoverageQuality"),
                            getDoubleField(obj, "needSupplyMatchScore"));
            if (cover > 0.0d) cv.put("coverage_quality", cover);
            double third =
                    maxDouble(
                            getDoubleField(obj, "thirdQuality"),
                            getDoubleField(obj, "thirdNumberQualityScore"),
                            getDoubleField(obj, "finalDecisionThirdNumberQuality"),
                            getDoubleField(obj, "needSupplyThirdQuality"));
            if (third > 0.0d) cv.put("third_quality", third);
            double need =
                    maxDouble(
                            getDoubleField(obj, "positionNeedScore"),
                            getDoubleField(obj, "positionNeedStrength"),
                            getDoubleField(obj, "deepPositionScore"),
                            0.0d);
            if (need > 0.0d) cv.put("position_need_score", need);
            double supply =
                    maxDouble(
                            getDoubleField(obj, "numberSupplyScore"),
                            getDoubleField(obj, "numberSupplyStrength"),
                            getDoubleField(obj, "deepNumberScore"),
                            0.0d);
            if (supply > 0.0d) cv.put("number_supply_score", supply);
            double counter =
                    maxDouble(
                            getDoubleField(obj, "counterEvidenceScore"),
                            getDoubleField(obj, "evidenceRisk"),
                            getDoubleField(obj, "k3CoverageRisk"),
                            getDoubleField(obj, "counterfactualWhyNotRisk"));
            if (counter > 0.0d) cv.put("counter_evidence_score", counter);
            double before =
                    maxDouble(
                            getDoubleField(obj, "finalPBeforeDiagnosis"),
                            getDoubleField(obj, "finalPBeforeCoverage"),
                            getDoubleField(obj, "rawP"),
                            getDoubleField(obj, "finalP"));
            double after =
                    maxDouble(
                            getDoubleField(obj, "finalPAfterDiagnosis"),
                            getDoubleField(obj, "coverageAdjustedP"),
                            getDoubleField(obj, "finalPAfterCoverage"),
                            getDoubleField(obj, "finalP"));
            if (before > 0.0d) cv.put("final_p_before_diagnosis", before);
            if (after > 0.0d) cv.put("final_p_after_diagnosis", after);
            double gate =
                    maxDouble(
                            getDoubleField(obj, "gate"),
                            getDoubleField(obj, "dynamicGate"),
                            0.0d,
                            0.0d);
            if (gate > 0.0d) cv.put("gate", gate);
            double edge = getDoubleField(obj, "edge");
            if (edge == 0.0d && after > 0.0d && gate > 0.0d) edge = after - gate;
            cv.put("edge", edge);
            String reject =
                    firstNonEmpty(
                            getStringField(obj, "rejectCode"),
                            firstNonEmpty(
                                    getStringField(obj, "finalDecisionRejectCode"),
                                    getStringField(obj, "blockCode")));
            cv.put("reject_code", reject);
            String adaptive =
                    firstNonEmpty(
                            getStringField(obj, "adaptiveStatus"),
                            firstNonEmpty(
                                    getStringField(obj, "adaptiveJudgeAction"),
                                    getStringField(obj, "adaptiveJudgeStatus")));
            cv.put("adaptive_status", adaptive);
            String promo =
                    firstNonEmpty(
                            getStringField(obj, "shadowPromotionStatus"),
                            firstNonEmpty(
                                    getStringField(obj, "promotionClusterStatus"),
                                    getStringField(obj, "promotionStatus")));
            cv.put("shadow_promotion_status", promo);
            cv.put("bet_source_engine", "DecisionGateEngine_ONLY");
        } catch (Throwable e) {
        }
    }

    private void copyMainChainClosedLoopFields(Object from, Object to) {
        if (from == null || to == null) return;
        try {
            Models.copyMainChainBridgeFields(from, to);
            return;
        } catch (Throwable e) {
        }
        String[][] textMap = mainChainTextMap();
        for (int i = 0; i < textMap.length; i++)
            setOptionalStringField(
                    to, textMap[i][1], getOptionalStringField(from, textMap[i][1], ""));
        String[][] intMap = mainChainIntMap();
        for (int i = 0; i < intMap.length; i++)
            setOptionalIntField(to, intMap[i][1], getOptionalIntField(from, intMap[i][1], 0));
        String[][] doubleMap = mainChainDoubleMap();
        for (int i = 0; i < doubleMap.length; i++)
            setOptionalDoubleField(
                    to, doubleMap[i][1], getOptionalDoubleField(from, doubleMap[i][1], 0.0d));
        copyLinkModificationV2Values(from, to);
    }

    private void putLinkModificationV2Values(ContentValues cv, Object obj) {
        if (cv == null || obj == null) return;
        String[][] textMap = linkModificationV2TextMap();
        for (int i = 0; i < textMap.length; i++)
            putStringField(cv, obj, textMap[i][0], textMap[i][1]);
        String[][] intMap = linkModificationV2IntMap();
        for (int i = 0; i < intMap.length; i++) putIntField(cv, obj, intMap[i][0], intMap[i][1]);
        String[][] doubleMap = linkModificationV2DoubleMap();
        for (int i = 0; i < doubleMap.length; i++)
            putDoubleRawField(cv, obj, doubleMap[i][0], doubleMap[i][1]);
        cv.put("snapshot_created_at", getOptionalLongField(obj, "snapshotCreatedAt", 0L));
        cv.put(
                "frequency_state_updated_at",
                getOptionalLongField(obj, "frequencyStateUpdatedAt", 0L));
        cv.put(
                "frequency_floor_required_count",
                getOptionalIntField(obj, "frequencyFloorRequiredCount", 0));
        cv.put(
                "frequency_floor_target_count",
                getOptionalIntField(obj, "frequencyFloorTargetCount", 0));
        cv.put(
                "frequency_floor_actual_count",
                getOptionalIntField(obj, "frequencyFloorActualCount", 0));
        cv.put(
                "frequency_floor_debt_count",
                getOptionalIntField(obj, "frequencyFloorDebtCount", 0));
        cv.put(
                "frequency_target_debt_count",
                getOptionalIntField(obj, "frequencyTargetDebtCount", 0));
        cv.put("frequency_floor_mandatory", getOptionalIntField(obj, "frequencyFloorMandatory", 0));
        cv.put(
                "frequency_floor_no_upper_cap",
                getOptionalIntField(obj, "frequencyFloorNoUpperCap", 1));
        cv.put(
                "frequency_floor_rank_score",
                getOptionalDoubleField(obj, "frequencyFloorRankScore", 0.0d));
        cv.put(
                "frequency_floor_version",
                getOptionalStringField(
                        obj, "frequencyFloorVersion", AppConfig.ACTIVE_QUALITY_BUILD_VERSION));
        cv.put("real_bet_frequency_20", getOptionalDoubleField(obj, "realBetFrequency20", 0.0d));
        cv.put("frequency_observed_20", getOptionalIntField(obj, "frequencyObserved20", 0));
        cv.put("frequency_observed_50", getOptionalIntField(obj, "frequencyObserved50", 0));
        cv.put("frequency_observed_100", getOptionalIntField(obj, "frequencyObserved100", 0));
        cv.put("frequency_observed_300", getOptionalIntField(obj, "frequencyObserved300", 0));
        cv.put("frequency_bet_count_20", getOptionalIntField(obj, "frequencyBetCount20", 0));
        cv.put("frequency_bet_count_50", getOptionalIntField(obj, "frequencyBetCount50", 0));
        cv.put("frequency_bet_count_100", getOptionalIntField(obj, "frequencyBetCount100", 0));
        cv.put("frequency_bet_count_300", getOptionalIntField(obj, "frequencyBetCount300", 0));
        cv.put("frequency_debt_20", getOptionalIntField(obj, "frequencyDebt20", 0));
        cv.put("frequency_debt_50", getOptionalIntField(obj, "frequencyDebt50", 0));
        cv.put("frequency_debt_100", getOptionalIntField(obj, "frequencyDebt100", 0));
        cv.put("frequency_debt_300", getOptionalIntField(obj, "frequencyDebt300", 0));
        cv.put("frequency_target_debt_20", getOptionalIntField(obj, "frequencyTargetDebt20", 0));
        cv.put("frequency_target_debt_50", getOptionalIntField(obj, "frequencyTargetDebt50", 0));
        cv.put("frequency_target_debt_100", getOptionalIntField(obj, "frequencyTargetDebt100", 0));
        cv.put("frequency_target_debt_300", getOptionalIntField(obj, "frequencyTargetDebt300", 0));
        cv.put("frequency_urgent_window", getOptionalStringField(obj, "frequencyUrgentWindow", ""));
        cv.put(
                "frequency_issues_since_last_real_bet",
                getOptionalIntField(obj, "frequencyIssuesSinceLastRealBet", 999));
        cv.put(
                "frequency_issues_since_last_floor_bet",
                getOptionalIntField(obj, "frequencyIssuesSinceLastFloorBet", 999));
        cv.put(
                "frequency_consecutive_floor_bets",
                getOptionalIntField(obj, "frequencyConsecutiveFloorBets", 0));
        cv.put(
                "frequency_floor_min_gap",
                getOptionalIntField(obj, "frequencyFloorMinGap", Integer.MAX_VALUE));
        cv.put(
                "frequency_floor_pacing_ready",
                getOptionalIntField(obj, "frequencyFloorPacingReady", 0));
        cv.put(
                "frequency_floor_pacing_reason",
                getOptionalStringField(obj, "frequencyFloorPacingReason", ""));
        cv.put("decision_quality_path", getOptionalStringField(obj, "decisionQualityPath", ""));
        cv.put("evidence_domain_frozen", getOptionalIntField(obj, "evidenceDomainFrozen", 0));
        cv.put(
                "evidence_domain_frozen_issue",
                getOptionalStringField(obj, "evidenceDomainFrozenIssue", ""));
        cv.put(
                "evidence_domain_frozen_at",
                getOptionalLongField(obj, "evidenceDomainFrozenAt", 0L));
        cv.put("dynamic_matched_level", getOptionalStringField(obj, "dynamicMatchedLevel", ""));
        cv.put("dynamic_real_sample", getOptionalIntField(obj, "dynamicRealSample", 0));
        cv.put("dynamic_shadow_sample", getOptionalIntField(obj, "dynamicShadowSample", 0));
        cv.put(
                "module_build_version",
                getOptionalStringField(
                        obj, "moduleBuildVersion", AppConfig.ACTIVE_QUALITY_BUILD_VERSION));
        cv.put(
                "decision_version_v2",
                firstNonEmpty(
                        getOptionalStringField(obj, "decisionVersionV2", ""),
                        DBSTORE_LINK_MODIFICATION_V2_VERSION));
    }

    private void readLinkModificationV2Values(Cursor c, Object obj) {
        if (c == null || obj == null) return;
        String[][] textMap = linkModificationV2TextMap();
        for (int i = 0; i < textMap.length; i++)
            setOptionalStringField(obj, textMap[i][1], getString(c, textMap[i][0]));
        String[][] intMap = linkModificationV2IntMap();
        for (int i = 0; i < intMap.length; i++)
            setOptionalIntField(obj, intMap[i][1], getInt(c, intMap[i][0]));
        String[][] doubleMap = linkModificationV2DoubleMap();
        for (int i = 0; i < doubleMap.length; i++)
            setOptionalDoubleField(obj, doubleMap[i][1], getDouble(c, doubleMap[i][0]));
        setOptionalLongField(obj, "snapshotCreatedAt", getLong(c, "snapshot_created_at"));
        setOptionalLongField(
                obj, "frequencyStateUpdatedAt", getLong(c, "frequency_state_updated_at"));
        setOptionalIntField(
                obj, "frequencyFloorRequiredCount", getInt(c, "frequency_floor_required_count"));
        setOptionalIntField(
                obj, "frequencyFloorTargetCount", getInt(c, "frequency_floor_target_count"));
        setOptionalIntField(
                obj, "frequencyFloorActualCount", getInt(c, "frequency_floor_actual_count"));
        setOptionalIntField(
                obj, "frequencyFloorDebtCount", getInt(c, "frequency_floor_debt_count"));
        setOptionalIntField(
                obj, "frequencyTargetDebtCount", getInt(c, "frequency_target_debt_count"));
        setOptionalIntField(obj, "frequencyFloorMandatory", getInt(c, "frequency_floor_mandatory"));
        setOptionalIntField(
                obj, "frequencyFloorNoUpperCap", getInt(c, "frequency_floor_no_upper_cap"));
        setOptionalDoubleField(
                obj, "frequencyFloorRankScore", getDouble(c, "frequency_floor_rank_score"));
        setOptionalStringField(
                obj, "frequencyFloorVersion", getString(c, "frequency_floor_version"));
        setOptionalDoubleField(obj, "realBetFrequency20", getDouble(c, "real_bet_frequency_20"));
        setOptionalIntField(obj, "frequencyObserved20", getInt(c, "frequency_observed_20"));
        setOptionalIntField(obj, "frequencyObserved50", getInt(c, "frequency_observed_50"));
        setOptionalIntField(obj, "frequencyObserved100", getInt(c, "frequency_observed_100"));
        setOptionalIntField(obj, "frequencyObserved300", getInt(c, "frequency_observed_300"));
        setOptionalIntField(obj, "frequencyBetCount20", getInt(c, "frequency_bet_count_20"));
        setOptionalIntField(obj, "frequencyBetCount50", getInt(c, "frequency_bet_count_50"));
        setOptionalIntField(obj, "frequencyBetCount100", getInt(c, "frequency_bet_count_100"));
        setOptionalIntField(obj, "frequencyBetCount300", getInt(c, "frequency_bet_count_300"));
        setOptionalIntField(obj, "frequencyDebt20", getInt(c, "frequency_debt_20"));
        setOptionalIntField(obj, "frequencyDebt50", getInt(c, "frequency_debt_50"));
        setOptionalIntField(obj, "frequencyDebt100", getInt(c, "frequency_debt_100"));
        setOptionalIntField(obj, "frequencyDebt300", getInt(c, "frequency_debt_300"));
        setOptionalIntField(obj, "frequencyTargetDebt20", getInt(c, "frequency_target_debt_20"));
        setOptionalIntField(obj, "frequencyTargetDebt50", getInt(c, "frequency_target_debt_50"));
        setOptionalIntField(obj, "frequencyTargetDebt100", getInt(c, "frequency_target_debt_100"));
        setOptionalIntField(obj, "frequencyTargetDebt300", getInt(c, "frequency_target_debt_300"));
        setOptionalStringField(
                obj, "frequencyUrgentWindow", getString(c, "frequency_urgent_window"));
        setOptionalIntField(
                obj,
                "frequencyIssuesSinceLastRealBet",
                getInt(c, "frequency_issues_since_last_real_bet"));
        setOptionalIntField(
                obj,
                "frequencyIssuesSinceLastFloorBet",
                getInt(c, "frequency_issues_since_last_floor_bet"));
        setOptionalIntField(
                obj,
                "frequencyConsecutiveFloorBets",
                getInt(c, "frequency_consecutive_floor_bets"));
        setOptionalIntField(obj, "frequencyFloorMinGap", getInt(c, "frequency_floor_min_gap"));
        setOptionalIntField(
                obj, "frequencyFloorPacingReady", getInt(c, "frequency_floor_pacing_ready"));
        setOptionalStringField(
                obj, "frequencyFloorPacingReason", getString(c, "frequency_floor_pacing_reason"));
        setOptionalStringField(obj, "decisionQualityPath", getString(c, "decision_quality_path"));
        setOptionalIntField(obj, "evidenceDomainFrozen", getInt(c, "evidence_domain_frozen"));
        setOptionalStringField(
                obj, "evidenceDomainFrozenIssue", getString(c, "evidence_domain_frozen_issue"));
        setOptionalLongField(
                obj, "evidenceDomainFrozenAt", getLong(c, "evidence_domain_frozen_at"));
        setOptionalStringField(obj, "dynamicMatchedLevel", getString(c, "dynamic_matched_level"));
        setOptionalIntField(obj, "dynamicRealSample", getInt(c, "dynamic_real_sample"));
        setOptionalIntField(obj, "dynamicShadowSample", getInt(c, "dynamic_shadow_sample"));
        setOptionalStringField(obj, "moduleBuildVersion", getString(c, "module_build_version"));
    }

    private void copyLinkModificationV2Values(Object from, Object to) {
        if (from == null || to == null) return;
        String[][] textMap = linkModificationV2TextMap();
        for (int i = 0; i < textMap.length; i++)
            setOptionalStringField(
                    to, textMap[i][1], getOptionalStringField(from, textMap[i][1], ""));
        String[][] intMap = linkModificationV2IntMap();
        for (int i = 0; i < intMap.length; i++)
            setOptionalIntField(to, intMap[i][1], getOptionalIntField(from, intMap[i][1], 0));
        String[][] doubleMap = linkModificationV2DoubleMap();
        for (int i = 0; i < doubleMap.length; i++)
            setOptionalDoubleField(
                    to, doubleMap[i][1], getOptionalDoubleField(from, doubleMap[i][1], 0.0d));
        setOptionalLongField(
                to, "snapshotCreatedAt", getOptionalLongField(from, "snapshotCreatedAt", 0L));
        setOptionalLongField(
                to,
                "frequencyStateUpdatedAt",
                getOptionalLongField(from, "frequencyStateUpdatedAt", 0L));
    }

    private String[][] linkModificationV2TextMap() {
        return new String[][] {
            {"frequency_state", "frequencyState"},
            {"frequency_state_version", "frequencyStateVersion"},
            {"shadow_promotion_domain", "shadowPromotionDomain"},
            {"decision_path", "decisionPath"},
            {"bet_domain", "betDomain"},
            {"probe_reject_reason", "probeRejectReason"},
            {"probe_eligibility_version", "probeEligibilityVersion"},
            {"rule_health_status", "ruleHealthStatus"},
            {"snapshot_freshness", "snapshotFreshness"},
            {"warm_profile_freshness", "warmProfileFreshness"},
            {"cold_profile_freshness", "coldProfileFreshness"},
            {"deep_profile_freshness", "deepProfileFreshness"},
            {"decision_version_v2", "decisionVersionV2"},
            {"evidence_as_of_issue", "evidenceAsOfIssue"},
            {"snapshot_as_of_issue", "snapshotAsOfIssue"}
        };
    }

    private String[][] linkModificationV2IntMap() {
        return new String[][] {
            {"qualified_probe", "qualifiedProbe"},
            {"real_trade", "realTrade"},
            {"coverage_available", "coverageAvailable"},
            {"third_quality_available", "thirdQualityAvailable"},
            {"promotion_domain_available", "promotionDomainAvailable"}
        };
    }

    private String[][] linkModificationV2DoubleMap() {
        return new String[][] {
            {"base_gate", "baseGate"}, {"risk_adjusted_gate", "riskAdjustedGate"},
            {"frequency_gate_adjust", "frequencyGateAdjust"},
            {"effective_probe_gate", "effectiveProbeGate"}, {"raw_edge", "rawEdge"},
            {"effective_probe_edge", "effectiveProbeEdge"},
            {"frequency_50", "realBetFrequency50"}, {"frequency_100", "realBetFrequency100"},
            {"frequency_300", "realBetFrequency300"},
            {"frequency_debt_score", "frequencyDebtScore"},
            {"frequency_support_score", "frequencySupportScore"},
            {"qualified_probe_score", "qualifiedProbeScore"},
            {"shadow_promotion_score", "shadowPromotionScore"},
            {"need_supply_score", "needSupplyScore"}, {"counter_risk", "counterRisk"}
        };
    }

    private void putDoubleRawField(ContentValues cv, Object o, String col, String field) {
        try {
            Field f = o.getClass().getField(field);
            cv.put(col, f.getDouble(o));
        } catch (Throwable e) {
        }
    }

    private void setDoubleRawField(Object o, String field, double v) {
        try {
            Field f = o.getClass().getField(field);
            f.setDouble(o, v);
        } catch (Throwable e) {
        }
    }

    private int getIntField(Object o, String field) {
        try {
            Field f = o.getClass().getField(field);
            Object v = f.get(o);
            if (v instanceof Number) return ((Number) v).intValue();
            if (v != null) return Integer.parseInt(String.valueOf(v));
        } catch (Throwable e) {
        }
        return 0;
    }

    private double maxDouble(double a, double b, double c, double d) {
        double m = a;
        if (b > m) m = b;
        if (c > m) m = c;
        if (d > m) m = d;
        return m;
    }

    private String buildMainChainCandidateKey(
            String targetIssue, int position, int n1, int n2, int n3, int mask, int rank) {
        String issue = targetIssue == null ? "" : targetIssue;
        int k = mask > 0 ? mask : AppConfig.buildK3Mask(n1, n2, n3);
        return "MCCS_" + AppConfig.safeIssue(issue) + "_P" + position + "_K" + k + "_R" + rank;
    }

    public int saveMainChainCandidateSnapshot(
            String baseIssue,
            String targetIssue,
            List<Models.ComboCandidate> candidates,
            int limit) {
        if (targetIssue == null
                || targetIssue.length() == 0
                || candidates == null
                || candidates.size() == 0) {
            return 0;
        }
        SQLiteDatabase db = writable();
        ensureMainChainClosedLoopReady(db);
        int max = limit > 0 ? limit : MAIN_CHAIN_REALTIME_TOP40_LIMIT;
        if (max > MAIN_CHAIN_CANDIDATE_SNAPSHOT_KEEP) max = MAIN_CHAIN_CANDIDATE_SNAPSHOT_KEEP;
        int saved = 0;
        db.beginTransaction();
        try {
            for (int i = 0; i < candidates.size() && i < max; i++) {
                Models.ComboCandidate c = candidates.get(i);
                if (c == null) continue;
                if (c.rank <= 0) c.rank = i + 1;
                c.baseIssue = firstNonEmpty(c.baseIssue, baseIssue, "");
                c.targetIssue = firstNonEmpty(c.targetIssue, targetIssue, "");
                setOptionalIntField(
                        c,
                        "realtimeTop40Rank",
                        c.rank <= MAIN_CHAIN_REALTIME_TOP40_LIMIT ? c.rank : 0);
                setOptionalIntField(
                        c, "adaptiveInputFlag", c.rank <= MAIN_CHAIN_REALTIME_TOP40_LIMIT ? 1 : 0);
                ContentValues cv = comboCandidateToValues(c);
                putMainChainClosedLoopValues(cv, c);
                cv.put(
                        "snapshot_key",
                        buildMainChainCandidateKey(
                                targetIssue, c.position, c.n1, c.n2, c.n3, c.k3Mask, c.rank));
                insertFiltered(
                        db,
                        TABLE_MAIN_CHAIN_CANDIDATE_SNAPSHOT,
                        cv,
                        SQLiteDatabase.CONFLICT_REPLACE);
                insertFiltered(
                        db,
                        DbHelper.TABLE_FULL_CANDIDATE_SCAN,
                        cv,
                        SQLiteDatabase.CONFLICT_REPLACE);
                if (c.rank <= MAIN_CHAIN_REALTIME_TOP40_LIMIT) {
                    insertFiltered(
                            db,
                            DbHelper.TABLE_CANDIDATE_AUDIT,
                            cv,
                            SQLiteDatabase.CONFLICT_REPLACE);
                }
                recordMainChainRejectBucket(
                        db, c, getStringField(c, "rejectCode"), AppConfig.DECISION_SHADOW);
                saved++;
            }
            db.setTransactionSuccessful();
        } catch (Throwable e) {
        } finally {
            try {
                db.endTransaction();
            } catch (Throwable ignore) {
            }
        }
        return saved;
    }

    public int saveRealtimeTop40Candidates(
            String baseIssue, String targetIssue, List<Models.ComboCandidate> candidates) {
        return saveMainChainCandidateSnapshot(
                baseIssue, targetIssue, candidates, MAIN_CHAIN_REALTIME_TOP40_LIMIT);
    }

    public int saveAdaptiveJudgeInputCandidates(
            String targetIssue, List<Models.ComboCandidate> candidates) {
        return saveMainChainCandidateSnapshot(
                "", targetIssue, candidates, MAIN_CHAIN_REALTIME_TOP40_LIMIT);
    }

    private void recordMainChainRejectBucket(
            SQLiteDatabase db, Object obj, String rejectCode, String decision) {
        if (db == null) return;
        String code =
                rejectCode == null || rejectCode.length() == 0 ? REJECT_CODE_NONE : rejectCode;
        if (code.length() == 0) code = "PASS_OR_UNKNOWN";
        try {
            String bucketKey = "MCRB_" + AppConfig.safeKeyPart(code);
            ContentValues cv = new ContentValues();
            cv.put("bucket_key", bucketKey);
            cv.put("reject_code", code);
            cv.put("sample_count", 1);
            cv.put("candidate_count", 1);
            cv.put(
                    "bet_count",
                    AppConfig.DECISION_BET.equals(AppConfig.normalizeDecision(decision)) ? 1 : 0);
            cv.put(
                    "shadow_count",
                    AppConfig.DECISION_SHADOW.equals(AppConfig.normalizeDecision(decision))
                            ? 1
                            : 0);
            cv.put(
                    "hold_count",
                    AppConfig.DECISION_HOLD.equals(AppConfig.normalizeDecision(decision)) ? 1 : 0);
            cv.put("avg_final_p", getDoubleField(obj, "finalP"));
            cv.put(
                    "avg_gate",
                    maxDouble(
                            getDoubleField(obj, "gate"),
                            getDoubleField(obj, "dynamicGate"),
                            0.0d,
                            0.0d));
            cv.put("avg_edge", getDoubleField(obj, "edge"));
            cv.put("updated_at", System.currentTimeMillis());
            insertFiltered(db, TABLE_MAIN_CHAIN_REJECT_BUCKET, cv, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Throwable e) {
        }
    }

    private void putFinalVersionFallback(ContentValues cv, Object obj) {
        if (cv == null || obj == null) {
            return;
        }
        try {
            String v = getStringField(obj, "finalDecisionVersion");
            if ((v == null || v.length() == 0) && !cv.containsKey("final_decision_version")) {
                cv.put("final_decision_version", V20_DYNAMIC_CAUSAL_DBSTORE_VERSION);
            }
            double cover =
                    Math.max(
                            getDoubleField(obj, "coverageQualityScore"),
                            Math.max(
                                    getDoubleField(obj, "dynamicGateCoverageQualityScore"),
                                    getDoubleField(obj, "v20CoverageScore")));
            double third =
                    Math.max(
                            getDoubleField(obj, "thirdNumberQualityScore"),
                            Math.max(
                                    getDoubleField(obj, "dynamicGateThirdNumberQuality"),
                                    Math.max(
                                            getDoubleField(obj, "needSupplyThirdQuality"),
                                            getDoubleField(
                                                    obj, "finalDecisionThirdNumberQuality"))));
            double risk =
                    Math.max(
                            getDoubleField(obj, "k3CoverageRisk"),
                            Math.max(
                                    getDoubleField(obj, "dynamicGateCoverageRisk"),
                                    getDoubleField(obj, "finalDecisionCoverageRisk")));
            if (cover > 0.0d) cv.put("coverage_quality_score", cover);
            if (third > 0.0d) cv.put("third_number_quality_score", third);
            if (risk > 0.0d) cv.put("k3_coverage_risk", risk);
            String domain =
                    firstNonEmpty(
                            getStringField(obj, "finalDecisionBetDomain"),
                            getStringField(obj, "adaptiveJudgeDomain"),
                            getStringField(obj, "promotionClusterStatus"),
                            getStringField(obj, "dynamicClusterStatus"),
                            "");
            if (domain != null && domain.length() > 0) {
                cv.put("final_decision_bet_domain", domain);
            }
        } catch (Throwable e) {
        }
    }

    private void putShadowDynamicBridgeAliases(ContentValues cv, Models.ComboCandidate c) {
        if (cv == null || c == null) {
            return;
        }
        try {
            String domain =
                    firstNonEmpty(
                            getStringField(c, "adaptiveJudgeDomain"),
                            getStringField(c, "promotionDomain"),
                            getStringField(c, "promotionClusterStatus"),
                            getStringField(c, "dynamicClusterStatus"),
                            getStringField(c, "finalDecisionBetDomain"));
            if (domain != null && domain.length() > 0) {
                cv.put("final_decision_bet_domain", domain);
                cv.put("adaptive_judge_domain", domain);
            }
            double cover =
                    Math.max(
                            getDoubleField(c, "coverageQualityScore"),
                            Math.max(
                                    getDoubleField(c, "dynamicGateCoverageQualityScore"),
                                    getDoubleField(c, "v20CoverageScore")));
            double third =
                    Math.max(
                            getDoubleField(c, "thirdNumberQualityScore"),
                            Math.max(
                                    getDoubleField(c, "dynamicGateThirdNumberQuality"),
                                    getDoubleField(c, "needSupplyThirdQuality")));
            double risk =
                    Math.max(
                            getDoubleField(c, "k3CoverageRisk"),
                            Math.max(
                                    getDoubleField(c, "dynamicGateCoverageRisk"),
                                    getDoubleField(c, "finalDecisionCoverageRisk")));
            if (cover > 0.0d) cv.put("coverage_quality_score", cover);
            if (third > 0.0d) cv.put("third_number_quality_score", third);
            if (risk > 0.0d) cv.put("k3_coverage_risk", risk);
            String summary =
                    firstNonEmpty(
                            getStringField(c, "dynamicCausalSummary"),
                            getStringField(c, "dynamicCausalDecisionSummary"),
                            getStringField(c, "needSupplySummary"),
                            getStringField(c, "counterfactualSummary"));
            if (summary != null && summary.length() > 0) {
                cv.put("dynamic_causal_summary", summary);
                cv.put("why_no_bet", summary);
            }
            cv.put("dynamic_causal_bridge_version", V20_DYNAMIC_CAUSAL_DBSTORE_VERSION);
        } catch (Throwable e) {
        }
    }

    private String getStringField(Object o, String field) {
        try {
            java.lang.reflect.Field f = o.getClass().getField(field);
            Object v = f.get(o);
            return v == null ? "" : String.valueOf(v);
        } catch (Throwable e) {
            return "";
        }
    }

    private double getDoubleField(Object o, String field) {
        try {
            java.lang.reflect.Field f = o.getClass().getField(field);
            return f.getDouble(o);
        } catch (Throwable e) {
            return 0.0d;
        }
    }

    public List<Models.ComboCandidate> getShadowCandidates(String targetIssue, int limit) {
        ArrayList<Models.ComboCandidate> list = new ArrayList<Models.ComboCandidate>();

        if (targetIssue == null || targetIssue.length() == 0) {
            return list;
        }

        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    DbHelper.TABLE_SHADOW,
                                    null,
                                    "target_issue=?",
                                    new String[] {targetIssue},
                                    null,
                                    null,
                                    "rank_no ASC",
                                    limit > 0 ? String.valueOf(limit) : null);

            while (c.moveToNext()) {
                list.add(readShadowCandidate(c));
            }
        } finally {
            closeCursor(c);
        }

        return list;
    }

    public void markShadowLearned(String targetIssue) {
        if (targetIssue == null || targetIssue.length() == 0) {
            return;
        }

        ContentValues cv = new ContentValues();
        cv.put("learned", 1);

        updateFiltered(
                writable(),
                DbHelper.TABLE_SHADOW,
                cv,
                "target_issue=?",
                new String[] {targetIssue});
    }

    public long saveTeacherMatrixSample(ContentValues source) {
        if (source == null) {
            return -1L;
        }

        SQLiteDatabase db = writable();
        ensureRuntimeExtraTablesOnce(db);

        ContentValues cv = new ContentValues(source);
        normalizeV20EvidenceCv(cv, TABLE_TEACHER_MATRIX_SAMPLE);
        long now = System.currentTimeMillis();

        String targetIssue = getCvString(cv, "target_issue", "");
        int position = getCvInt(cv, "position", 0);
        int number = getCvInt(cv, "number", 0);

        if (targetIssue.length() == 0
                || !AppConfig.isValidPosition(position)
                || !AppConfig.isValidNumber(number)) {
            return -1L;
        }

        if (getCvLong(cv, "created_at", 0L) <= 0L) {
            cv.put("created_at", now);
        }

        if (getCvString(cv, "sample_key", "").length() == 0) {
            cv.put(
                    "sample_key",
                    buildTeacherMatrixSampleKey(
                            getCvString(cv, "source_type", "AUTO"),
                            getCvString(cv, "base_issue", ""),
                            targetIssue,
                            position,
                            number));
        }

        if (getCvString(cv, "state_key", "").length() == 0) {
            cv.put(
                    "state_key",
                    buildStateKey(
                            getCvString(cv, "state_mode", ""),
                            getCvString(cv, "shape_key", ""),
                            position,
                            getCvDouble(cv, "drift_risk", 0.0d),
                            getCvDouble(cv, "break_risk", 0.0d),
                            getCvDouble(cv, "recovery_score", 0.0d)));
        }

        if (getCvString(cv, "profile_key", "").length() == 0) {
            cv.put(
                    "profile_key",
                    buildTeacherMatrixProfileKey(
                            getCvString(cv, "state_key", ""), position, number));
        }

        if (getCvString(cv, "combo_key", "").length() == 0) {
            cv.put(
                    "combo_key",
                    buildTeacherMatrixComboKey(
                            getCvString(cv, "state_key", ""),
                            position,
                            getCvInt(cv, "k3_mask", 0)));
        }

        long id =
                insertFiltered(db, TABLE_TEACHER_MATRIX_SAMPLE, cv, SQLiteDatabase.CONFLICT_IGNORE);

        if (id >= 0L) {
            updateTeacherMatrixProfileFromSample(db, cv);
            updateTeacherMatrixComboProfileFromSample(db, cv);
            updateTeacherMatrixSummaryState(db);
        }

        return id;
    }

    public int saveTeacherMatrixSamples(List<ContentValues> samples) {
        if (samples == null || samples.size() == 0) {
            return 0;
        }

        int saved = 0;
        SQLiteDatabase db = writable();
        ensureRuntimeExtraTablesOnce(db);

        db.beginTransaction();

        try {
            for (int i = 0; i < samples.size(); i++) {
                ContentValues source = samples.get(i);

                if (source == null) {
                    continue;
                }

                ContentValues cv = new ContentValues(source);
                normalizeV20EvidenceCv(cv, TABLE_TEACHER_MATRIX_SAMPLE);
                long now = System.currentTimeMillis();

                String targetIssue = getCvString(cv, "target_issue", "");
                int position = getCvInt(cv, "position", 0);
                int number = getCvInt(cv, "number", 0);

                if (targetIssue.length() == 0
                        || !AppConfig.isValidPosition(position)
                        || !AppConfig.isValidNumber(number)) {
                    continue;
                }

                if (getCvLong(cv, "created_at", 0L) <= 0L) {
                    cv.put("created_at", now);
                }

                if (getCvString(cv, "sample_key", "").length() == 0) {
                    cv.put(
                            "sample_key",
                            buildTeacherMatrixSampleKey(
                                    getCvString(cv, "source_type", "AUTO"),
                                    getCvString(cv, "base_issue", ""),
                                    targetIssue,
                                    position,
                                    number));
                }

                if (getCvString(cv, "state_key", "").length() == 0) {
                    cv.put(
                            "state_key",
                            buildStateKey(
                                    getCvString(cv, "state_mode", ""),
                                    getCvString(cv, "shape_key", ""),
                                    position,
                                    getCvDouble(cv, "drift_risk", 0.0d),
                                    getCvDouble(cv, "break_risk", 0.0d),
                                    getCvDouble(cv, "recovery_score", 0.0d)));
                }

                if (getCvString(cv, "profile_key", "").length() == 0) {
                    cv.put(
                            "profile_key",
                            buildTeacherMatrixProfileKey(
                                    getCvString(cv, "state_key", ""), position, number));
                }

                if (getCvString(cv, "combo_key", "").length() == 0) {
                    cv.put(
                            "combo_key",
                            buildTeacherMatrixComboKey(
                                    getCvString(cv, "state_key", ""),
                                    position,
                                    getCvInt(cv, "k3_mask", 0)));
                }

                long id =
                        insertFiltered(
                                db,
                                TABLE_TEACHER_MATRIX_SAMPLE,
                                cv,
                                SQLiteDatabase.CONFLICT_IGNORE);

                if (id >= 0L) {
                    updateTeacherMatrixProfileFromSample(db, cv);
                    updateTeacherMatrixComboProfileFromSample(db, cv);
                    saved++;
                }
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        if (saved > 0) {
            updateTeacherMatrixSummaryState(db);
        }

        return saved;
    }

    public int saveTeacherMatrixFromLearningCases(List<Models.LearningCase> cases) {
        if (cases == null || cases.size() == 0) {
            return 0;
        }

        ArrayList<ContentValues> samples = new ArrayList<ContentValues>();

        for (int i = 0; i < cases.size(); i++) {
            Models.LearningCase x = cases.get(i);

            if (x == null
                    || x.targetIssue == null
                    || x.targetIssue.length() == 0
                    || !AppConfig.isValidPosition(x.position)) {
                continue;
            }

            int actual = x.actualNumber;
            int k3Mask = getLearningCaseK3Mask(x);

            for (int n = 1; n <= 10; n++) {
                ContentValues cv = new ContentValues();

                cv.put("source_type", "LEARNING_CASE");
                cv.put("base_issue", x.baseIssue);
                cv.put("target_issue", x.targetIssue);
                cv.put("created_at", x.createdAt > 0L ? x.createdAt : System.currentTimeMillis());
                cv.put("target_index", x.targetIndex);
                cv.put("position", x.position);
                cv.put("number", n);
                cv.put("actual_number", actual);
                cv.put("label", actual == n ? 1 : 0);
                cv.put("hit", actual == n ? 1 : 0);
                cv.put("k3_mask", k3Mask);
                cv.put("state_key", x.stateKey);
                cv.put("state_mode", x.stateMode);
                cv.put("trend_key", x.trendKey);
                cv.put("shape_key", x.shapeKey);
                cv.put("drift_key", x.driftKey);
                cv.put("break_key", x.breakKey);
                cv.put("recovery_key", x.recoveryKey);
                cv.put("transition_key", x.transitionKey);
                cv.put("profile_key", buildTeacherMatrixProfileKey(x.stateKey, x.position, n));
                cv.put("combo_key", buildTeacherMatrixComboKey(x.stateKey, x.position, k3Mask));
                cv.put("trend_score", x.trendScore);
                cv.put("shape_score", x.shapeScore);
                cv.put("drift_risk", x.driftRisk);
                cv.put("break_risk", x.breakRisk);
                cv.put("recovery_score", x.recoveryScore);
                cv.put("number_score", x.numberBias);
                cv.put("position_score", x.profileScore);
                cv.put("risk_score", x.riskScore);
                cv.put("confidence", x.stateConfidence);
                cv.put("final_p", x.finalP);
                cv.put("dynamic_gate", x.dynamicGate);
                cv.put("edge", x.edge);
                cv.put("why_positive", x.whyActualHit);
                cv.put("why_negative", x.whyExpectedHit);
                cv.put("why_miss", x.whyMiss);
                cv.put("next_advice", x.nextAdvice);
                cv.put("main_reason_code", x.mainReasonCode);
                cv.put("affected_module", x.affectedModule);
                cv.put("saved", 1);

                samples.add(cv);
            }
        }

        return saveTeacherMatrixSamples(samples);
    }

    private void updateTeacherMatrixProfileFromSample(SQLiteDatabase db, ContentValues sample) {
        if (db == null || sample == null) {
            return;
        }

        String profileKey = getCvString(sample, "profile_key", "");
        String stateKey = getCvString(sample, "state_key", "");
        int position = getCvInt(sample, "position", 0);
        int number = getCvInt(sample, "number", 0);

        if (profileKey.length() == 0
                || stateKey.length() == 0
                || !AppConfig.isValidPosition(position)
                || !AppConfig.isValidNumber(number)) {
            return;
        }

        long now = System.currentTimeMillis();
        int label = getCvInt(sample, "label", 0) == 1 ? 1 : 0;

        ContentValues init = new ContentValues();
        init.put("profile_key", profileKey);
        init.put("state_key", stateKey);
        init.put("position", position);
        init.put("number", number);
        init.put("sample_count", 0);
        init.put("positive_count", 0);
        init.put("negative_count", 0);
        init.put("recent_sample", 0);
        init.put("recent_positive", 0);
        init.put("recent_negative", 0);
        init.put("hit_rate", 0.0d);
        init.put("recent_hit_rate", 0.0d);
        init.put("confidence", 0.0d);
        init.put("position_score", 0.0d);
        init.put("number_score", 0.0d);
        init.put("state_score", 0.0d);
        init.put("trend_score", 0.0d);
        init.put("shape_score", 0.0d);
        init.put("risk_score", 0.0d);
        init.put("prob_adjust", 0.0d);
        init.put("gate_adjust", 0.0d);
        init.put("risk_penalty", 0.0d);
        init.put("teacher_weight", 1.0d);
        init.put("updated_at", now);
        init.put("status", "WEAK");
        init.put("why_text", "");
        init.put("summary_text", "");

        insertFiltered(db, TABLE_TEACHER_MATRIX_PROFILE, init, SQLiteDatabase.CONFLICT_IGNORE);

        double trendScore =
                AppConfig.clampProbability(Math.abs(getCvDouble(sample, "trend_score", 0.0d)));
        double shapeScore =
                AppConfig.clampProbability(Math.abs(getCvDouble(sample, "shape_score", 0.0d)));
        double positionScore =
                AppConfig.clampProbability(Math.abs(getCvDouble(sample, "position_score", 0.0d)));
        double numberScore =
                AppConfig.clampProbability(Math.abs(getCvDouble(sample, "number_score", 0.0d)));
        double stateScore =
                AppConfig.clampProbability(Math.abs(getCvDouble(sample, "confidence", 0.0d)));
        double riskScore =
                normalizeRisk(
                        getCvDouble(
                                sample,
                                "risk_score",
                                Math.max(
                                        getCvDouble(sample, "drift_risk", 0.0d),
                                        getCvDouble(sample, "break_risk", 0.0d))));

        db.execSQL(
                "UPDATE "
                        + TABLE_TEACHER_MATRIX_PROFILE
                        + " SET sample_count=sample_count+1,positive_count=positive_count+?,"
                        + "negative_count=negative_count+?,recent_sample=CASE WHEN"
                        + " recent_sample>=240 THEN 240 ELSE recent_sample+1"
                        + " END,recent_positive=CASE WHEN recent_sample>=240 THEN recent_positive"
                        + " ELSE recent_positive+? END,recent_negative=CASE WHEN recent_sample>=240"
                        + " THEN recent_negative ELSE recent_negative+? END,position_score=CASE"
                        + " WHEN sample_count<=0 THEN ? ELSE"
                        + " ((position_score*sample_count)+?)/(sample_count+1)"
                        + " END,number_score=CASE WHEN sample_count<=0 THEN ? ELSE"
                        + " ((number_score*sample_count)+?)/(sample_count+1) END,state_score=CASE"
                        + " WHEN sample_count<=0 THEN ? ELSE"
                        + " ((state_score*sample_count)+?)/(sample_count+1) END,trend_score=CASE"
                        + " WHEN sample_count<=0 THEN ? ELSE"
                        + " ((trend_score*sample_count)+?)/(sample_count+1) END,shape_score=CASE"
                        + " WHEN sample_count<=0 THEN ? ELSE"
                        + " ((shape_score*sample_count)+?)/(sample_count+1) END,risk_score=CASE"
                        + " WHEN sample_count<=0 THEN ? ELSE"
                        + " ((risk_score*sample_count)+?)/(sample_count+1) END,"
                        + "updated_at=?,last_hit_at=CASE WHEN ?=1 THEN ? ELSE last_hit_at"
                        + " END,last_miss_at=CASE WHEN ?=0 THEN ? ELSE last_miss_at END WHERE"
                        + " profile_key=?",
                new Object[] {
                    Integer.valueOf(label),
                    Integer.valueOf(label == 1 ? 0 : 1),
                    Integer.valueOf(label),
                    Integer.valueOf(label == 1 ? 0 : 1),
                    Double.valueOf(positionScore),
                    Double.valueOf(positionScore),
                    Double.valueOf(numberScore),
                    Double.valueOf(numberScore),
                    Double.valueOf(stateScore),
                    Double.valueOf(stateScore),
                    Double.valueOf(trendScore),
                    Double.valueOf(trendScore),
                    Double.valueOf(shapeScore),
                    Double.valueOf(shapeScore),
                    Double.valueOf(riskScore),
                    Double.valueOf(riskScore),
                    Long.valueOf(now),
                    Integer.valueOf(label),
                    Long.valueOf(now),
                    Integer.valueOf(label),
                    Long.valueOf(now),
                    profileKey
                });

        recalcTeacherMatrixProfile(db, profileKey);
    }

    private void updateTeacherMatrixComboProfileFromSample(
            SQLiteDatabase db, ContentValues sample) {
        if (db == null || sample == null) {
            return;
        }

        String comboKey = getCvString(sample, "combo_key", "");
        String stateKey = getCvString(sample, "state_key", "");
        int position = getCvInt(sample, "position", 0);
        int k3Mask = getCvInt(sample, "k3_mask", 0);

        if (comboKey.length() == 0
                || stateKey.length() == 0
                || !AppConfig.isValidPosition(position)
                || !Models.isValidK3Mask(k3Mask)) {
            return;
        }

        long now = System.currentTimeMillis();
        int actual = getCvInt(sample, "actual_number", 0);
        boolean hit = maskContainsNumber(k3Mask, actual);
        int hitAdd = hit ? 1 : 0;

        ContentValues init = new ContentValues();
        init.put("combo_key", comboKey);
        init.put("state_key", stateKey);
        init.put("position", position);
        init.put("k3_mask", k3Mask);
        init.put("sample_count", 0);
        init.put("hit_count", 0);
        init.put("miss_count", 0);
        init.put("recent_sample", 0);
        init.put("recent_hit", 0);
        init.put("hit_rate", 0.0d);
        init.put("recent_hit_rate", 0.0d);
        init.put("confidence", 0.0d);
        init.put("combo_score", 0.0d);
        init.put("risk_score", 0.0d);
        init.put("prob_adjust", 0.0d);
        init.put("gate_adjust", 0.0d);
        init.put("updated_at", now);
        init.put("status", "WEAK");
        init.put("summary_text", "");

        insertFiltered(
                db, TABLE_TEACHER_MATRIX_COMBO_PROFILE, init, SQLiteDatabase.CONFLICT_IGNORE);

        double risk =
                normalizeRisk(
                        getCvDouble(
                                sample,
                                "risk_score",
                                Math.max(
                                        getCvDouble(sample, "drift_risk", 0.0d),
                                        getCvDouble(sample, "break_risk", 0.0d))));

        db.execSQL(
                "UPDATE "
                        + TABLE_TEACHER_MATRIX_COMBO_PROFILE
                        + " SET sample_count=sample_count+1,hit_count=hit_count+?,"
                        + "miss_count=miss_count+?,recent_sample=CASE WHEN recent_sample>=240 THEN"
                        + " 240 ELSE recent_sample+1 END,recent_hit=CASE WHEN recent_sample>=240"
                        + " THEN recent_hit ELSE recent_hit+? END,risk_score=CASE WHEN"
                        + " sample_count<=0 THEN ? ELSE"
                        + " ((risk_score*sample_count)+?)/(sample_count+1) END,updated_at=? WHERE"
                        + " combo_key=?",
                new Object[] {
                    Integer.valueOf(hitAdd),
                    Integer.valueOf(hit ? 0 : 1),
                    Integer.valueOf(hitAdd),
                    Double.valueOf(risk),
                    Double.valueOf(risk),
                    Long.valueOf(now),
                    comboKey
                });

        recalcTeacherMatrixComboProfile(db, comboKey);
    }

    private void recalcTeacherMatrixProfile(SQLiteDatabase db, String profileKey) {
        if (db == null || profileKey == null || profileKey.length() == 0) {
            return;
        }

        ContentValues p = getTeacherMatrixProfileValues(profileKey);

        if (p == null) {
            return;
        }

        int sample = getCvInt(p, "sample_count", 0);
        int positive = getCvInt(p, "positive_count", 0);
        int recentSample = getCvInt(p, "recent_sample", 0);
        int recentPositive = getCvInt(p, "recent_positive", 0);
        double risk = normalizeRisk(getCvDouble(p, "risk_score", 0.0d));

        double hitRate = sample > 0 ? ((double) positive) / ((double) sample) : 0.0d;
        double recentHitRate =
                recentSample > 0 ? ((double) recentPositive) / ((double) recentSample) : 0.0d;
        double confidence = confidenceFromSample(sample);

        double base = hitRate * 0.46d + recentHitRate * 0.30d + confidence * 0.12d;
        double probAdjust = limitAdjustLocal((base - 0.10d) * 0.12d, 0.020d);
        double gateAdjust = 0.0d;
        double riskPenalty = 0.0d;

        if (sample < 30) {
            gateAdjust += 0.006d;
        }

        if (hitRate < 0.075d && sample >= 30) {
            gateAdjust += 0.008d;
            riskPenalty += 0.006d;
        }

        if (risk > 0.55d) {
            gateAdjust += 0.006d;
            riskPenalty += 0.006d;
        }

        if (gateAdjust < 0.0d) {
            gateAdjust = 0.0d;
        }

        gateAdjust = limitAdjustLocal(gateAdjust, 0.025d);
        riskPenalty = limitAdjustLocal(riskPenalty, 0.025d);

        String status;

        if (sample < 20) {
            status = "WEAK";
        } else if (hitRate >= 0.135d && recentHitRate >= 0.12d) {
            status = "STRONG";
        } else if (hitRate < 0.075d && sample >= 30) {
            status = "RISK";
        } else {
            status = "NORMAL";
        }

        String summary =
                "教师矩阵画像："
                        + profileKey
                        + "｜样本="
                        + sample
                        + "｜命中率="
                        + percent(hitRate)
                        + "｜近期="
                        + percent(recentHitRate)
                        + "｜状态="
                        + status
                        + "｜概率字段="
                        + signedPercent(probAdjust)
                        + "｜风险闸门="
                        + signedPercent(gateAdjust)
                        + "｜说明=DbStore只写字段，不直接救援BET";

        ContentValues cv = new ContentValues();
        cv.put("hit_rate", hitRate);
        cv.put("recent_hit_rate", recentHitRate);
        cv.put("confidence", confidence);
        cv.put("prob_adjust", probAdjust);
        cv.put("gate_adjust", gateAdjust);
        cv.put("risk_penalty", riskPenalty);
        cv.put("teacher_weight", confidence);
        cv.put("status", status);
        cv.put("why_text", summary);
        cv.put("summary_text", summary);
        cv.put("updated_at", System.currentTimeMillis());

        updateFiltered(
                db, TABLE_TEACHER_MATRIX_PROFILE, cv, "profile_key=?", new String[] {profileKey});
    }

    private void recalcTeacherMatrixComboProfile(SQLiteDatabase db, String comboKey) {
        if (db == null || comboKey == null || comboKey.length() == 0) {
            return;
        }

        ContentValues p = getTeacherMatrixComboProfileValues(comboKey);

        if (p == null) {
            return;
        }

        int sample = getCvInt(p, "sample_count", 0);
        int hit = getCvInt(p, "hit_count", 0);
        int recentSample = getCvInt(p, "recent_sample", 0);
        int recentHit = getCvInt(p, "recent_hit", 0);
        double risk = normalizeRisk(getCvDouble(p, "risk_score", 0.0d));

        double hitRate = sample > 0 ? ((double) hit) / ((double) sample) : 0.0d;
        double recentHitRate =
                recentSample > 0 ? ((double) recentHit) / ((double) recentSample) : 0.0d;
        double confidence = confidenceFromSample(sample);

        double comboScore =
                hitRate * 0.50d + recentHitRate * 0.30d + confidence * 0.12d - risk * 0.10d;
        double probAdjust =
                limitAdjustLocal((hitRate - AppConfig.K3_BASE_PROBABILITY) * 0.16d, 0.020d);
        double gateAdjust = 0.0d;

        if (sample < 25) {
            gateAdjust += 0.004d;
        }

        if (hitRate < AppConfig.TARGET_HIT_RATE_MIN && sample >= 35) {
            gateAdjust += 0.008d;
        }

        if (gateAdjust < 0.0d) {
            gateAdjust = 0.0d;
        }

        gateAdjust = limitAdjustLocal(gateAdjust, 0.025d);

        String status;

        if (sample < 20) {
            status = "WEAK";
        } else if (hitRate >= AppConfig.TARGET_HIT_RATE_IDEAL
                && recentHitRate >= AppConfig.TARGET_HIT_RATE_MIN) {
            status = "STRONG";
        } else if (hitRate < AppConfig.TARGET_HIT_RATE_MIN && sample >= 35) {
            status = "RISK";
        } else {
            status = "NORMAL";
        }

        String summary =
                "教师矩阵组合："
                        + comboKey
                        + "｜样本="
                        + sample
                        + "｜K3命中率="
                        + percent(hitRate)
                        + "｜近期="
                        + percent(recentHitRate)
                        + "｜状态="
                        + status
                        + "｜概率字段="
                        + signedPercent(probAdjust)
                        + "｜风险闸门="
                        + signedPercent(gateAdjust)
                        + "｜说明=DbStore只写字段，不直接救援BET";

        ContentValues cv = new ContentValues();
        cv.put("hit_rate", hitRate);
        cv.put("recent_hit_rate", recentHitRate);
        cv.put("confidence", confidence);
        cv.put("combo_score", comboScore);
        cv.put("prob_adjust", probAdjust);
        cv.put("gate_adjust", gateAdjust);
        cv.put("status", status);
        cv.put("summary_text", summary);
        cv.put("updated_at", System.currentTimeMillis());

        updateFiltered(
                db, TABLE_TEACHER_MATRIX_COMBO_PROFILE, cv, "combo_key=?", new String[] {comboKey});
    }

    public ContentValues getTeacherMatrixProfileValues(String profileKey) {
        if (profileKey == null || profileKey.length() == 0) {
            return null;
        }

        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    TABLE_TEACHER_MATRIX_PROFILE,
                                    null,
                                    "profile_key=?",
                                    new String[] {profileKey},
                                    null,
                                    null,
                                    null,
                                    "1");

            if (c.moveToFirst()) {
                return cursorRowToContentValues(c);
            }
        } catch (Throwable e) {
        } finally {
            closeCursor(c);
        }

        return null;
    }

    public ContentValues getTeacherMatrixProfileValues(String stateKey, int position, int number) {
        return getTeacherMatrixProfileValues(
                buildTeacherMatrixProfileKey(stateKey, position, number));
    }

    public ContentValues getTeacherMatrixComboProfileValues(String comboKey) {
        if (comboKey == null || comboKey.length() == 0) {
            return null;
        }

        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    TABLE_TEACHER_MATRIX_COMBO_PROFILE,
                                    null,
                                    "combo_key=?",
                                    new String[] {comboKey},
                                    null,
                                    null,
                                    null,
                                    "1");

            if (c.moveToFirst()) {
                return cursorRowToContentValues(c);
            }
        } catch (Throwable e) {
        } finally {
            closeCursor(c);
        }

        return null;
    }

    public ContentValues getTeacherMatrixComboProfileValues(
            String stateKey, int position, int k3Mask) {
        return getTeacherMatrixComboProfileValues(
                buildTeacherMatrixComboKey(stateKey, position, k3Mask));
    }

    public double getTeacherMatrixNumberProbAdjust(String stateKey, int position, int number) {
        ContentValues p = getTeacherMatrixProfileValues(stateKey, position, number);

        if (p == null) {
            return 0.0d;
        }

        return limitAdjustLocal(getCvDouble(p, "prob_adjust", 0.0d), 0.020d);
    }

    public double getTeacherMatrixNumberGateAdjust(String stateKey, int position, int number) {
        ContentValues p = getTeacherMatrixProfileValues(stateKey, position, number);

        if (p == null) {
            return 0.0d;
        }

        double v = getCvDouble(p, "gate_adjust", 0.0d);

        if (v < 0.0d) {
            v = 0.0d;
        }

        return limitAdjustLocal(v, 0.030d);
    }

    public double getTeacherMatrixComboProbAdjust(String stateKey, int position, int k3Mask) {
        ContentValues p = getTeacherMatrixComboProfileValues(stateKey, position, k3Mask);

        if (p == null) {
            return 0.0d;
        }

        return limitAdjustLocal(getCvDouble(p, "prob_adjust", 0.0d), 0.020d);
    }

    public double getTeacherMatrixComboGateAdjust(String stateKey, int position, int k3Mask) {
        ContentValues p = getTeacherMatrixComboProfileValues(stateKey, position, k3Mask);

        if (p == null) {
            return 0.0d;
        }

        double v = getCvDouble(p, "gate_adjust", 0.0d);

        if (v < 0.0d) {
            v = 0.0d;
        }

        return limitAdjustLocal(v, 0.030d);
    }

    public int countTeacherMatrixSamples() {
        return countTable(TABLE_TEACHER_MATRIX_SAMPLE, null, null);
    }

    public int countTeacherMatrixProfiles() {
        return countTable(TABLE_TEACHER_MATRIX_PROFILE, null, null);
    }

    public int countTeacherMatrixStrongProfiles() {
        return countTable(TABLE_TEACHER_MATRIX_PROFILE, "status=?", new String[] {"STRONG"});
    }

    public int countTeacherMatrixRiskProfiles() {
        return countTable(TABLE_TEACHER_MATRIX_PROFILE, "status=?", new String[] {"RISK"});
    }

    private void updateTeacherMatrixSummaryState(SQLiteDatabase db) {
        int sample = countTable(db, TABLE_TEACHER_MATRIX_SAMPLE, null, null);
        int profile = countTable(db, TABLE_TEACHER_MATRIX_PROFILE, null, null);
        int strong =
                countTable(db, TABLE_TEACHER_MATRIX_PROFILE, "status=?", new String[] {"STRONG"});
        int risk = countTable(db, TABLE_TEACHER_MATRIX_PROFILE, "status=?", new String[] {"RISK"});

        String summary =
                "教师矩阵：样本="
                        + sample
                        + "｜画像="
                        + profile
                        + "｜强="
                        + strong
                        + "｜风险="
                        + risk
                        + "｜说明=开奖后反推学习；实时主链可读取字段，但不能越过最终门直接BET";

        setState(
                "teacher_matrix_status",
                sample > 0 ? "TEACHER_MATRIX_READY" : "TEACHER_MATRIX_NONE");
        setState("teacher_matrix_sample_count", String.valueOf(sample));
        setState("teacher_matrix_profile_count", String.valueOf(profile));
        setState("teacher_matrix_strong_count", String.valueOf(strong));
        setState("teacher_matrix_risk_count", String.valueOf(risk));
        setState("teacher_matrix_summary", summary);
    }

    private void applyTeacherMatrixToCandidate(Models.ComboCandidate c) {
        attachTeacherMatrixFieldsToCandidate(c);
    }

    private void attachTeacherMatrixFieldsToCandidate(Models.ComboCandidate c) {
        if (c == null) {
            return;
        }

        String stateKey = firstNonEmpty(c.stateKey, safeStateKeyFromCandidate(c));

        if (stateKey.length() == 0
                || !AppConfig.isValidPosition(c.position)
                || !Models.isValidK3Mask(c.k3Mask)) {
            return;
        }

        TeacherMatrixApplyResult r =
                buildTeacherMatrixApplyResult(stateKey, c.position, c.k3Mask, c.n1, c.n2, c.n3);

        if (r == null || r.applied <= 0) {
            return;
        }

        setOptionalIntField(c, "teacherMatrixSample", r.sample);
        setOptionalDoubleField(c, "teacherMatrixHitRate", r.hitRate);
        setOptionalDoubleField(c, "teacherMatrixConfidence", r.confidence);
        setOptionalDoubleField(c, "teacherMatrixProbAdjust", r.probAdjust);
        setOptionalDoubleField(c, "teacherMatrixGateAdjust", r.gateAdjust);
        setOptionalDoubleField(c, "teacherMatrixRiskPenalty", r.riskPenalty);
        setOptionalIntField(c, "teacherMatrixApplied", r.applied);
        setOptionalStringField(c, "teacherMatrixStatus", r.status);
        setOptionalStringField(c, "teacherMatrixProfileKey", r.profileKey);
        setOptionalStringField(c, "teacherMatrixComboKey", r.comboKey);
        setOptionalStringField(c, "teacherMatrixSummary", r.summary);
    }

    private void applyTeacherMatrixToDecision(Models.Decision d) {
        attachTeacherMatrixFieldsToDecision(d);
    }

    private void attachTeacherMatrixFieldsToDecision(Models.Decision d) {
        if (d == null) {
            return;
        }

        if (!Models.isValidK3Mask(d.k3Mask) || !AppConfig.isValidPosition(d.position)) {
            return;
        }

        String stateKey = firstNonEmpty(d.stateKey, safeStateKeyFromDecision(d));
        TeacherMatrixApplyResult r =
                buildTeacherMatrixApplyResult(stateKey, d.position, d.k3Mask, d.n1, d.n2, d.n3);

        if (r == null || r.applied <= 0) {
            return;
        }

        setOptionalIntField(d, "teacherMatrixSample", r.sample);
        setOptionalDoubleField(d, "teacherMatrixHitRate", r.hitRate);
        setOptionalDoubleField(d, "teacherMatrixConfidence", r.confidence);
        setOptionalDoubleField(d, "teacherMatrixProbAdjust", r.probAdjust);
        setOptionalDoubleField(d, "teacherMatrixGateAdjust", r.gateAdjust);
        setOptionalDoubleField(d, "teacherMatrixRiskPenalty", r.riskPenalty);
        setOptionalIntField(d, "teacherMatrixApplied", r.applied);
        setOptionalStringField(d, "teacherMatrixStatus", r.status);
        setOptionalStringField(d, "teacherMatrixProfileKey", r.profileKey);
        setOptionalStringField(d, "teacherMatrixComboKey", r.comboKey);
        setOptionalStringField(d, "teacherMatrixSummary", r.summary);
    }

    private void applyTeacherMatrixToTrade(Models.Trade t) {
        attachTeacherMatrixFieldsToTrade(t);
    }

    private void attachTeacherMatrixFieldsToTrade(Models.Trade t) {
        if (t == null) {
            return;
        }

        if (!Models.isValidK3Mask(t.k3Mask) || !AppConfig.isValidPosition(t.position)) {
            return;
        }

        String stateKey = firstNonEmpty(t.stateKey, safeTradeStateKey(t));
        TeacherMatrixApplyResult r =
                buildTeacherMatrixApplyResult(stateKey, t.position, t.k3Mask, t.n1, t.n2, t.n3);

        if (r == null || r.applied <= 0) {
            return;
        }

        setOptionalIntField(t, "teacherMatrixSample", r.sample);
        setOptionalDoubleField(t, "teacherMatrixHitRate", r.hitRate);
        setOptionalDoubleField(t, "teacherMatrixConfidence", r.confidence);
        setOptionalDoubleField(t, "teacherMatrixProbAdjust", r.probAdjust);
        setOptionalDoubleField(t, "teacherMatrixGateAdjust", r.gateAdjust);
        setOptionalDoubleField(t, "teacherMatrixRiskPenalty", r.riskPenalty);
        setOptionalIntField(t, "teacherMatrixApplied", r.applied);
        setOptionalStringField(t, "teacherMatrixStatus", r.status);
        setOptionalStringField(t, "teacherMatrixProfileKey", r.profileKey);
        setOptionalStringField(t, "teacherMatrixComboKey", r.comboKey);
        setOptionalStringField(t, "teacherMatrixSummary", r.summary);
    }

    private TeacherMatrixApplyResult buildTeacherMatrixApplyResult(
            String stateKey, int position, int k3Mask, int n1, int n2, int n3) {
        if (stateKey == null
                || stateKey.length() == 0
                || !AppConfig.isValidPosition(position)
                || !Models.isValidK3Mask(k3Mask)) {
            return null;
        }

        TeacherMatrixApplyResult r = new TeacherMatrixApplyResult();
        r.profileKey = "";
        r.comboKey = buildTeacherMatrixComboKey(stateKey, position, k3Mask);

        int[] nums = new int[] {n1, n2, n3};
        double pSum = 0.0d;
        double gSum = 0.0d;
        double riskSum = 0.0d;
        double hitRateSum = 0.0d;
        double confSum = 0.0d;
        int sampleSum = 0;
        int used = 0;
        String status = "";

        for (int i = 0; i < nums.length; i++) {
            int n = nums[i];

            if (!AppConfig.isValidNumber(n)) {
                continue;
            }

            String pk = buildTeacherMatrixProfileKey(stateKey, position, n);
            ContentValues p = getTeacherMatrixProfileValues(pk);

            if (p == null) {
                continue;
            }

            int sample = getCvInt(p, "sample_count", 0);

            if (sample <= 0) {
                continue;
            }

            if (r.profileKey.length() == 0) {
                r.profileKey = pk;
            }

            pSum += getCvDouble(p, "prob_adjust", 0.0d);
            gSum += Math.max(0.0d, getCvDouble(p, "gate_adjust", 0.0d));
            riskSum += getCvDouble(p, "risk_penalty", 0.0d);
            hitRateSum += getCvDouble(p, "hit_rate", 0.0d);
            confSum += getCvDouble(p, "confidence", 0.0d);
            sampleSum += sample;
            used++;

            String st = getCvString(p, "status", "");
            if ("RISK".equals(st)) {
                status = st;
            } else if (status.length() == 0 && st.length() > 0) {
                status = st;
            }
        }

        ContentValues combo = getTeacherMatrixComboProfileValues(r.comboKey);

        if (combo != null) {
            int comboSample = getCvInt(combo, "sample_count", 0);

            if (comboSample > 0) {
                pSum += getCvDouble(combo, "prob_adjust", 0.0d);
                gSum += Math.max(0.0d, getCvDouble(combo, "gate_adjust", 0.0d));
                hitRateSum += getCvDouble(combo, "hit_rate", 0.0d);
                confSum += getCvDouble(combo, "confidence", 0.0d);
                sampleSum += comboSample;
                used++;

                String st = getCvString(combo, "status", "");
                if ("RISK".equals(st)) {
                    status = st;
                } else if (status.length() == 0 && st.length() > 0) {
                    status = st;
                }
            }
        }

        if (used <= 0) {
            return null;
        }

        r.sample = sampleSum;
        r.hitRate = hitRateSum / used;
        r.confidence = confSum / used;
        r.probAdjust = limitAdjustLocal(pSum / used, 0.020d);
        r.gateAdjust = limitAdjustLocal(Math.max(0.0d, gSum / used), 0.030d);
        r.riskPenalty = limitAdjustLocal(Math.max(0.0d, riskSum / used), 0.030d);
        r.applied = 1;
        r.status = status.length() == 0 ? "NORMAL" : status;
        r.summary =
                "教师矩阵字段｜样本="
                        + r.sample
                        + "｜命中率="
                        + percent(r.hitRate)
                        + "｜置信="
                        + percent(r.confidence)
                        + "｜概率字段="
                        + signedPercent(r.probAdjust)
                        + "｜风险闸门="
                        + signedPercent(r.gateAdjust)
                        + "｜风险扣减="
                        + signedPercent(r.riskPenalty)
                        + "｜状态="
                        + r.status
                        + "｜说明=DbStore只挂字段，不改概率，不越过最终门";

        return r;
    }

    private void putTeacherMatrixFieldsFromObject(
            ContentValues cv, Object obj, String stateKey, int position, int k3Mask) {
        if (cv == null) {
            return;
        }

        int sample = getOptionalIntField(obj, "teacherMatrixSample", 0);
        double hitRate = getOptionalDoubleField(obj, "teacherMatrixHitRate", 0.0d);
        double confidence = getOptionalDoubleField(obj, "teacherMatrixConfidence", 0.0d);
        double probAdjust = getOptionalDoubleField(obj, "teacherMatrixProbAdjust", 0.0d);
        double gateAdjust = getOptionalDoubleField(obj, "teacherMatrixGateAdjust", 0.0d);
        double riskPenalty = getOptionalDoubleField(obj, "teacherMatrixRiskPenalty", 0.0d);
        int applied = getOptionalIntField(obj, "teacherMatrixApplied", 0);
        String status = getOptionalStringField(obj, "teacherMatrixStatus", "");
        String profileKey = getOptionalStringField(obj, "teacherMatrixProfileKey", "");
        String comboKey = getOptionalStringField(obj, "teacherMatrixComboKey", "");
        String summary = getOptionalStringField(obj, "teacherMatrixSummary", "");

        if (applied <= 0
                && stateKey != null
                && stateKey.length() > 0
                && AppConfig.isValidPosition(position)
                && Models.isValidK3Mask(k3Mask)) {
            TeacherMatrixApplyResult r =
                    buildTeacherMatrixApplyResult(stateKey, position, k3Mask, 0, 0, 0);

            if (r != null) {
                sample = r.sample;
                hitRate = r.hitRate;
                confidence = r.confidence;
                probAdjust = r.probAdjust;
                gateAdjust = r.gateAdjust;
                riskPenalty = r.riskPenalty;
                applied = r.applied;
                status = r.status;
                profileKey = r.profileKey;
                comboKey = r.comboKey;
                summary = r.summary;
            }
        }

        if (gateAdjust < 0.0d) {
            gateAdjust = 0.0d;
        }

        if (riskPenalty < 0.0d) {
            riskPenalty = 0.0d;
        }

        cv.put("teacher_matrix_sample", sample);
        cv.put("teacher_matrix_hit_rate", hitRate);
        cv.put("teacher_matrix_confidence", confidence);
        cv.put("teacher_matrix_prob_adjust", probAdjust);
        cv.put("teacher_matrix_gate_adjust", gateAdjust);
        cv.put("teacher_matrix_risk_penalty", riskPenalty);
        cv.put("teacher_matrix_applied", applied);
        cv.put("teacher_matrix_status", status);
        cv.put("teacher_matrix_profile_key", profileKey);
        cv.put("teacher_matrix_combo_key", comboKey);
        cv.put("teacher_matrix_summary", summary);
    }

    private static final class TeacherMatrixApplyResult {
        int sample;
        double hitRate;
        double confidence;
        double probAdjust;
        double gateAdjust;
        double riskPenalty;
        int applied;
        String status;
        String profileKey;
        String comboKey;
        String summary;
    }

    public long saveShapeVector(Models.ShapeVector v) {
        if (v == null || !AppConfig.isValidPosition(v.position)) {
            return -1L;
        }

        ContentValues cv = new ContentValues();

        cv.put("base_issue", v.baseIssue);
        cv.put("target_issue", v.targetIssue);
        cv.put("created_at", v.createdAt);
        cv.put("position", v.position);

        cv.put("entropy16", v.entropy16);
        cv.put("entropy32", v.entropy32);
        cv.put("entropy64", v.entropy64);
        cv.put("entropy128", v.entropy128);
        cv.put("entropy240", v.entropy240);
        cv.put("entropy480", v.entropy480);
        cv.put("entropy960", v.entropy960);

        cv.put("top3_mass16", v.top3Mass16);
        cv.put("top3_mass32", v.top3Mass32);
        cv.put("top3_mass64", v.top3Mass64);
        cv.put("top3_mass128", v.top3Mass128);
        cv.put("top3_mass240", v.top3Mass240);
        cv.put("top4_mass128", v.top4Mass128);
        cv.put("top6_mass240", v.top6Mass240);

        cv.put("slope_short", v.slopeShort);
        cv.put("slope_mid", v.slopeMid);
        cv.put("slope_long", v.slopeLong);

        cv.put("drift_short", v.driftShort);
        cv.put("drift_mid", v.driftMid);
        cv.put("drift_long", v.driftLong);
        cv.put("drift_risk", v.driftRisk);

        cv.put("markov1_support", v.markov1Support);
        cv.put("markov2_support", v.markov2Support);

        cv.put("trend_life", v.trendLife);
        cv.put("break_depth", v.breakDepth);
        cv.put("recovery_score", v.recoveryScore);
        cv.put("champion_gap", v.championGap);
        cv.put("combo_gap", v.comboGap);
        cv.put("hot_reverse_risk", v.hotReverseRisk);
        cv.put("false_strong_risk", v.falseStrongRisk);

        cv.put("state_mode", v.stateMode);
        cv.put("shape_key", v.shapeKey);

        for (int n = 1; n <= 10; n++) {
            cv.put(probColumn(n), v.getNumberProbability(n));
            cv.put(markovColumn(n), v.getMarkovProbability(n));
        }

        cv.put("deep_score", v.deepScore);
        cv.put("state_score", v.stateScore);
        cv.put("cycle_score", v.cycleScore);
        cv.put("transition_score", v.transitionScore);
        cv.put("bias_score", v.biasScore);
        cv.put("profile_score", v.profileScore);
        cv.put("deep_profile_key", v.deepProfileKey);

        String stateKey =
                firstNonEmpty(
                        v.stateKey,
                        buildStateKey(
                                v.stateMode,
                                v.shapeKey,
                                v.position,
                                v.driftRisk,
                                v.breakDepth,
                                v.recoveryScore));

        cv.put("state_key", stateKey);
        cv.put(
                "runtime_profile_score",
                v.runtimeProfileScore != 0.0d ? v.runtimeProfileScore : v.profileScore);

        double teacherScore = 0.0d;
        int teacherUsed = 0;

        for (int n = 1; n <= 10; n++) {
            ContentValues p = getTeacherMatrixProfileValues(stateKey, v.position, n);

            if (p == null) {
                continue;
            }

            int sample = getCvInt(p, "sample_count", 0);

            if (sample <= 0) {
                continue;
            }

            teacherScore += getCvDouble(p, "prob_adjust", 0.0d);
            teacherUsed++;
        }

        if (teacherUsed > 0) {
            cv.put("teacher_matrix_score", limitAdjustLocal(teacherScore / teacherUsed, 0.020d));
        }

        return insertFiltered(
                writable(), DbHelper.TABLE_SHAPE_VECTOR, cv, SQLiteDatabase.CONFLICT_NONE);
    }

    public List<Models.ShapeVector> getRecentShapeVectors(int limit) {
        return getRecentShapeVectors(0, limit);
    }

    public List<Models.ShapeVector> getRecentShapeVectors(int position, int limit) {
        ArrayList<Models.ShapeVector> list = new ArrayList<Models.ShapeVector>();

        Cursor c = null;
        String where = position > 0 ? "position=?" : null;
        String[] args = position > 0 ? new String[] {String.valueOf(position)} : null;

        try {
            c =
                    readable()
                            .query(
                                    DbHelper.TABLE_SHAPE_VECTOR,
                                    null,
                                    where,
                                    args,
                                    null,
                                    null,
                                    "created_at DESC, _id DESC",
                                    limit > 0 ? String.valueOf(limit) : null);

            while (c.moveToNext()) {
                list.add(readShapeVector(c));
            }
        } finally {
            closeCursor(c);
        }

        ArrayList<Models.ShapeVector> ordered = new ArrayList<Models.ShapeVector>();

        for (int i = list.size() - 1; i >= 0; i--) {
            ordered.add(list.get(i));
        }

        return ordered;
    }

    public List<Models.ShapeVector> getShapeVectorsForIssue(String baseIssue) {
        ArrayList<Models.ShapeVector> list = new ArrayList<Models.ShapeVector>();

        if (baseIssue == null || baseIssue.length() == 0) {
            return list;
        }

        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    DbHelper.TABLE_SHAPE_VECTOR,
                                    null,
                                    "base_issue=?",
                                    new String[] {baseIssue},
                                    null,
                                    null,
                                    "position ASC");

            while (c.moveToNext()) {
                list.add(readShapeVector(c));
            }
        } finally {
            closeCursor(c);
        }

        return list;
    }

    public void updateShapeMemory(
            String shapeKey, int position, boolean hit, boolean fail, boolean reverse) {
        if (shapeKey == null || shapeKey.length() == 0 || !AppConfig.isValidPosition(position)) {
            return;
        }

        SQLiteDatabase db = writable();
        long now = System.currentTimeMillis();

        db.execSQL(
                "INSERT OR IGNORE INTO "
                        + DbHelper.TABLE_SHAPE_MEMORY
                        + " (shape_key, position, sample_count, hit_count, hit_rate, active_flag,"
                        + " updated_at) VALUES (?,?,?,?,?,?,?)",
                new Object[] {
                    shapeKey,
                    Integer.valueOf(position),
                    Integer.valueOf(0),
                    Integer.valueOf(0),
                    Double.valueOf(0.0d),
                    Integer.valueOf(1),
                    Long.valueOf(now)
                });

        db.execSQL(
                "UPDATE "
                        + DbHelper.TABLE_SHAPE_MEMORY
                        + " SET "
                        + "sample_count=sample_count+1,"
                        + "hit_count=hit_count+?,"
                        + "fail_count=fail_count+?,"
                        + "reverse_count=reverse_count+?,"
                        + "updated_at=? "
                        + "WHERE shape_key=? AND position=?",
                new Object[] {
                    Integer.valueOf(hit ? 1 : 0),
                    Integer.valueOf(fail ? 1 : 0),
                    Integer.valueOf(reverse ? 1 : 0),
                    Long.valueOf(now),
                    shapeKey,
                    Integer.valueOf(position)
                });

        db.execSQL(
                "UPDATE "
                        + DbHelper.TABLE_SHAPE_MEMORY
                        + " SET hit_rate=CASE WHEN sample_count>0 THEN CAST(hit_count AS"
                        + " REAL)/sample_count ELSE 0 END WHERE shape_key=? AND position=?",
                new Object[] {shapeKey, Integer.valueOf(position)});
    }

    public void updateComboMemory(
            String shapeKey,
            int position,
            int k3Mask,
            boolean hit,
            double comboGap,
            double trendLife) {
        if (shapeKey == null
                || shapeKey.length() == 0
                || !AppConfig.isValidPosition(position)
                || !Models.isValidK3Mask(k3Mask)) {
            return;
        }

        SQLiteDatabase db = writable();
        long now = System.currentTimeMillis();

        db.execSQL(
                "INSERT OR IGNORE INTO "
                        + DbHelper.TABLE_COMBO_MEMORY
                        + " (shape_key, position, k3_mask, sample_count, hit_count, hit_rate,"
                        + " active_flag, updated_at) VALUES (?,?,?,?,?,?,?,?)",
                new Object[] {
                    shapeKey,
                    Integer.valueOf(position),
                    Integer.valueOf(k3Mask),
                    Integer.valueOf(0),
                    Integer.valueOf(0),
                    Double.valueOf(0.0d),
                    Integer.valueOf(1),
                    Long.valueOf(now)
                });

        db.execSQL(
                "UPDATE "
                        + DbHelper.TABLE_COMBO_MEMORY
                        + " SET "
                        + "sample_count=sample_count+1,"
                        + "hit_count=hit_count+?,"
                        + "avg_combo_gap=((avg_combo_gap*sample_count)+?)/(sample_count+1),"
                        + "avg_trend_life=((avg_trend_life*sample_count)+?)/(sample_count+1),"
                        + "updated_at=? "
                        + "WHERE shape_key=? AND position=? AND k3_mask=?",
                new Object[] {
                    Integer.valueOf(hit ? 1 : 0),
                    Double.valueOf(comboGap),
                    Double.valueOf(trendLife),
                    Long.valueOf(now),
                    shapeKey,
                    Integer.valueOf(position),
                    Integer.valueOf(k3Mask)
                });

        db.execSQL(
                "UPDATE "
                        + DbHelper.TABLE_COMBO_MEMORY
                        + " SET hit_rate=CASE WHEN sample_count>0 THEN CAST(hit_count AS"
                        + " REAL)/sample_count ELSE 0 END WHERE shape_key=? AND position=? AND"
                        + " k3_mask=?",
                new Object[] {shapeKey, Integer.valueOf(position), Integer.valueOf(k3Mask)});
    }

    public void updateBreakMemory(
            String breakKey,
            int position,
            String stateModeBefore,
            String breakType,
            int breakDepthBucket,
            int trendLifeBucket,
            int driftBucket,
            int markovBucket,
            int recoverStep,
            boolean fail,
            boolean reverse) {

        if (breakKey == null || breakKey.length() == 0 || !AppConfig.isValidPosition(position)) {
            return;
        }

        SQLiteDatabase db = writable();
        long now = System.currentTimeMillis();

        db.execSQL(
                "INSERT OR IGNORE INTO "
                        + DbHelper.TABLE_BREAK_MEMORY
                        + " (break_key, position, state_mode_before, break_type,"
                        + " break_depth_bucket, trend_life_bucket, drift_bucket, markov_bucket,"
                        + " updated_at) VALUES (?,?,?,?,?,?,?,?,?)",
                new Object[] {
                    breakKey,
                    Integer.valueOf(position),
                    stateModeBefore,
                    breakType,
                    Integer.valueOf(breakDepthBucket),
                    Integer.valueOf(trendLifeBucket),
                    Integer.valueOf(driftBucket),
                    Integer.valueOf(markovBucket),
                    Long.valueOf(now)
                });

        int r1 = recoverStep == 1 ? 1 : 0;
        int r2 = recoverStep == 2 ? 1 : 0;
        int r3 = recoverStep == 3 ? 1 : 0;

        db.execSQL(
                "UPDATE "
                        + DbHelper.TABLE_BREAK_MEMORY
                        + " SET sample_count=sample_count+1,recover1_count=recover1_count+?,"
                        + "recover2_count=recover2_count+?,recover3_count=recover3_count+?,"
                        + "fail_count=fail_count+?,reverse_count=reverse_count+?,updated_at=? WHERE"
                        + " break_key=? AND position=? AND break_depth_bucket=? AND drift_bucket=?"
                        + " AND markov_bucket=?",
                new Object[] {
                    Integer.valueOf(r1),
                    Integer.valueOf(r2),
                    Integer.valueOf(r3),
                    Integer.valueOf(fail ? 1 : 0),
                    Integer.valueOf(reverse ? 1 : 0),
                    Long.valueOf(now),
                    breakKey,
                    Integer.valueOf(position),
                    Integer.valueOf(breakDepthBucket),
                    Integer.valueOf(driftBucket),
                    Integer.valueOf(markovBucket)
                });

        db.execSQL(
                "UPDATE "
                        + DbHelper.TABLE_BREAK_MEMORY
                        + " SET recover_rate=CASE WHEN sample_count>0 THEN"
                        + " CAST((recover1_count+recover2_count+recover3_count) AS"
                        + " REAL)/sample_count ELSE 0 END WHERE break_key=? AND position=? AND"
                        + " break_depth_bucket=? AND drift_bucket=? AND markov_bucket=?",
                new Object[] {
                    breakKey,
                    Integer.valueOf(position),
                    Integer.valueOf(breakDepthBucket),
                    Integer.valueOf(driftBucket),
                    Integer.valueOf(markovBucket)
                });
    }

    public void updateCalibration(double rawP, boolean hit) {
        rawP = AppConfig.clampProbability(rawP);

        String bucket = bucketKey(rawP);
        long now = System.currentTimeMillis();
        SQLiteDatabase db = writable();

        ensureCalibrationBucket(db, bucket, rawP);

        db.execSQL(
                "UPDATE "
                        + DbHelper.TABLE_CALIBRATION
                        + " SET "
                        + "sample_count=sample_count+1,"
                        + "hit_count=hit_count+?,"
                        + "updated_at=? "
                        + "WHERE bucket_key=?",
                new Object[] {Integer.valueOf(hit ? 1 : 0), Long.valueOf(now), bucket});

        db.execSQL(
                "UPDATE "
                        + DbHelper.TABLE_CALIBRATION
                        + " SET real_hit_rate=CASE WHEN sample_count>0 THEN CAST(hit_count AS"
                        + " REAL)/sample_count ELSE 0 END WHERE bucket_key=?",
                new Object[] {bucket});
    }

    private void ensureCalibrationBucket(SQLiteDatabase db, String bucket, double rawP) {
        if (db == null || bucket == null || bucket.length() == 0) {
            return;
        }

        ContentValues cv = new ContentValues();

        cv.put("bucket_key", bucket);
        cv.put("raw_min", bucketRawMin(bucket, rawP));
        cv.put("raw_max", bucketRawMax(bucket, rawP));
        cv.put("sample_count", 0);
        cv.put("hit_count", 0);
        cv.put("real_hit_rate", 0.0d);
        cv.put("last240_sample", 0);
        cv.put("last240_hit", 0);
        cv.put("last240_hit_rate", 0.0d);
        cv.put("last1000_sample", 0);
        cv.put("last1000_hit", 0);
        cv.put("last1000_hit_rate", 0.0d);
        cv.put("updated_at", System.currentTimeMillis());

        insertFiltered(db, DbHelper.TABLE_CALIBRATION, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public double getCalibratedProbability(double rawP) {
        rawP = AppConfig.clampProbability(rawP);

        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    DbHelper.TABLE_CALIBRATION,
                                    null,
                                    "bucket_key=?",
                                    new String[] {bucketKey(rawP)},
                                    null,
                                    null,
                                    null,
                                    "1");

            if (c.moveToFirst()) {
                int sample = getInt(c, "sample_count");
                double real = getDouble(c, "real_hit_rate");

                if (sample >= 80 && real > 0.0d) {
                    return AppConfig.clampProbability(real);
                }
            }
        } finally {
            closeCursor(c);
        }

        return fallbackCalibrated(rawP);
    }

    private double fallbackCalibrated(double rawP) {
        rawP = AppConfig.clampProbability(rawP);

        if (rawP < 0.360d) {
            return rawP;
        }

        if (rawP < 0.380d) {
            return AppConfig.clampProbability(rawP - 0.002d);
        }

        if (rawP < 0.420d) {
            return AppConfig.clampProbability(rawP - 0.004d);
        }

        if (rawP < 0.460d) {
            return AppConfig.clampProbability(rawP - 0.006d);
        }

        return AppConfig.clampProbability(rawP - 0.008d);
    }

    public Models.ModelWeight getModelWeight() {
        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    DbHelper.TABLE_MODEL_WEIGHT,
                                    null,
                                    "weight_key='GLOBAL'",
                                    null,
                                    null,
                                    null,
                                    null,
                                    "1");

            if (c.moveToFirst()) {
                Models.ModelWeight w = new Models.ModelWeight();

                w.windowWeight = getDouble(c, "window_weight");
                w.markovWeight = getDouble(c, "markov_weight");
                w.shapeWeight = getDouble(c, "shape_weight");
                w.comboWeight = getDouble(c, "combo_weight");
                w.recoveryWeight = getDouble(c, "recovery_weight");

                w.similarWeight = getDouble(c, "similar_weight");
                w.memoryWeight = getDouble(c, "memory_weight");
                w.positionWeight = getDouble(c, "position_weight");
                w.frequencyWeight = getDouble(c, "frequency_weight");

                w.driftPenaltyWeight = getDouble(c, "drift_penalty_weight");
                w.hotReversePenaltyWeight = getDouble(c, "hot_reverse_penalty_weight");
                w.falseStrongPenaltyWeight = getDouble(c, "false_strong_penalty_weight");
                w.breakPenaltyWeight = getDouble(c, "break_penalty_weight");

                w.deepFeatureWeight = getDouble(c, "deep_feature_weight");
                w.biasWeight = getDouble(c, "bias_weight");
                w.profileWeight = getDouble(c, "profile_weight");
                w.cycleWeight = getDouble(c, "cycle_weight");
                w.transitionWeight = getDouble(c, "transition_weight");
                w.aiJudgeWeight = getDouble(c, "ai_judge_weight");
                w.auditWeight = getDouble(c, "audit_weight");

                w.runtimeProfileWeight = getDouble(c, "runtime_profile_weight");
                w.stateNumberProfileWeight = getDouble(c, "state_number_profile_weight");
                w.teacherWeight = getDouble(c, "teacher_weight");

                setOptionalDoubleField(
                        w, "teacherMatrixWeight", getDouble(c, "teacher_matrix_weight"));

                w.recentError = getDouble(c, "recent_error");
                w.recentWinRate = getDouble(c, "recent_win_rate");
                w.recentFrequency = getDouble(c, "recent_frequency");
                w.recentPnl = getDouble(c, "recent_pnl");
                w.updatedAt = getLong(c, "updated_at");

                w.normalizePositive();
                return w;
            }
        } finally {
            closeCursor(c);
        }

        return new Models.ModelWeight();
    }

    public void saveModelWeight(Models.ModelWeight w) {
        if (w == null) {
            return;
        }

        w.normalizePositive();

        SQLiteDatabase db = writable();
        ContentValues cv = new ContentValues();

        cv.put("weight_key", "GLOBAL");
        cv.put("window_weight", w.windowWeight);
        cv.put("markov_weight", w.markovWeight);
        cv.put("shape_weight", w.shapeWeight);
        cv.put("combo_weight", w.comboWeight);
        cv.put("recovery_weight", w.recoveryWeight);

        cv.put("similar_weight", w.similarWeight);
        cv.put("memory_weight", w.memoryWeight);
        cv.put("position_weight", w.positionWeight);
        cv.put("frequency_weight", w.frequencyWeight);

        cv.put("drift_penalty_weight", w.driftPenaltyWeight);
        cv.put("hot_reverse_penalty_weight", w.hotReversePenaltyWeight);
        cv.put("false_strong_penalty_weight", w.falseStrongPenaltyWeight);
        cv.put("break_penalty_weight", w.breakPenaltyWeight);

        cv.put("deep_feature_weight", w.deepFeatureWeight);
        cv.put("bias_weight", w.biasWeight);
        cv.put("profile_weight", w.profileWeight);
        cv.put("cycle_weight", w.cycleWeight);
        cv.put("transition_weight", w.transitionWeight);
        cv.put("ai_judge_weight", w.aiJudgeWeight);
        cv.put("audit_weight", w.auditWeight);

        cv.put("runtime_profile_weight", w.runtimeProfileWeight);
        cv.put("state_number_profile_weight", w.stateNumberProfileWeight);
        cv.put("teacher_weight", w.teacherWeight > 0.0d ? w.teacherWeight : 1.0d);
        cv.put("teacher_matrix_weight", getOptionalDoubleField(w, "teacherMatrixWeight", 0.18d));

        cv.put("recent_error", w.recentError);
        cv.put("recent_win_rate", w.recentWinRate);
        cv.put("recent_frequency", w.recentFrequency);
        cv.put("recent_pnl", w.recentPnl);
        cv.put("updated_at", System.currentTimeMillis());

        int updated =
                updateFiltered(db, DbHelper.TABLE_MODEL_WEIGHT, cv, "weight_key='GLOBAL'", null);

        if (updated <= 0) {
            insertFiltered(db, DbHelper.TABLE_MODEL_WEIGHT, cv, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    public long saveLearningCase(Models.LearningCase x) {
        if (x == null) {
            return -1L;
        }

        SQLiteDatabase db = writable();
        ensureRuntimeExtraTablesOnce(db);

        ContentValues cv = learningCaseToValues(x);
        long id = insertFiltered(db, TABLE_LEARNING_CASE_LOG, cv, SQLiteDatabase.CONFLICT_REPLACE);

        if (id >= 0L) {
            updateRuntimeProfileFromLearningCase(x);
            updateStateNumberProfileFromLearningCase(x);

            ArrayList<Models.LearningCase> one = new ArrayList<Models.LearningCase>();
            one.add(x);
            saveTeacherMatrixFromLearningCases(one);
        }

        return id;
    }

    public long insertOrIgnoreLearningCase(Models.LearningCase x) {
        if (x == null) {
            return -1L;
        }

        SQLiteDatabase db = writable();
        ensureRuntimeExtraTablesOnce(db);

        ContentValues cv = learningCaseToValues(x);
        long id = insertFiltered(db, TABLE_LEARNING_CASE_LOG, cv, SQLiteDatabase.CONFLICT_IGNORE);

        if (id >= 0L) {
            updateRuntimeProfileFromLearningCase(x);
            updateStateNumberProfileFromLearningCase(x);

            ArrayList<Models.LearningCase> one = new ArrayList<Models.LearningCase>();
            one.add(x);
            saveTeacherMatrixFromLearningCases(one);
        }

        return id;
    }

    public int saveLearningCases(List<Models.LearningCase> list) {
        if (list == null || list.size() == 0) {
            return 0;
        }

        int saved = 0;
        SQLiteDatabase db = writable();
        ensureRuntimeExtraTablesOnce(db);

        db.beginTransaction();

        try {
            for (int i = 0; i < list.size(); i++) {
                Models.LearningCase x = list.get(i);

                if (x == null) {
                    continue;
                }

                ContentValues cv = learningCaseToValues(x);
                long id =
                        insertFiltered(
                                db, TABLE_LEARNING_CASE_LOG, cv, SQLiteDatabase.CONFLICT_REPLACE);

                if (id >= 0L) {
                    updateRuntimeProfileFromLearningCaseInternal(db, x);
                    updateStateNumberProfileFromLearningCaseInternal(db, x);
                    saved++;
                }
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        int tmSaved = 0;

        if (saved > 0) {
            tmSaved = saveTeacherMatrixFromLearningCases(list);
            setState(
                    "learning_summary",
                    "后台学习已写入 "
                            + saved
                            + " 条位置样本，教师矩阵同步写入 "
                            + tmSaved
                            + " 条号码样本；实时主链轻量读取字段，由 Probability/DynamicGate/DecisionGate 决定是否采用。");
            setState("runtime_profile_summary", buildProfileCacheSummary());
        }

        return saved;
    }

    private ContentValues learningCaseToValues(Models.LearningCase x) {
        ContentValues cv = new ContentValues();

        long now = System.currentTimeMillis();

        String targetIssue = safeString(x.targetIssue);
        int position = x.position;
        String stateKey = safeString(x.stateKey);
        int k3Mask = getLearningCaseK3Mask(x);
        String caseKey = x.caseKey;

        if (caseKey == null || caseKey.length() == 0) {
            caseKey = buildLearningCaseKey(x.taskKey, x.segmentKey, targetIssue, position);
        }

        cv.put("case_key", caseKey);
        cv.put("task_key", x.taskKey);
        cv.put("segment_key", x.segmentKey);
        cv.put("base_issue", x.baseIssue);
        cv.put("target_issue", targetIssue);
        cv.put("created_at", x.createdAt > 0L ? x.createdAt : now);
        cv.put("target_index", x.targetIndex);
        cv.put("position", position);
        cv.put("actual_number", x.actualNumber);
        cv.put("predicted_k3", k3Mask);
        cv.put("k3_mask", k3Mask);
        cv.put("hit", x.hit);
        cv.put("state_key", stateKey);
        cv.put("state_mode", x.stateMode);
        cv.put("trend_key", x.trendKey);
        cv.put("shape_key", x.shapeKey);
        cv.put("drift_key", x.driftKey);
        cv.put("break_key", x.breakKey);
        cv.put("recovery_key", x.recoveryKey);
        cv.put("transition_key", x.transitionKey);
        cv.put("expected_shape", x.expectedShape);
        cv.put("actual_shape", x.actualShape);
        cv.put("why_expected_hit", x.whyExpectedHit);
        cv.put("why_actual_hit", x.whyActualHit);
        cv.put("why_miss", x.whyMiss);
        cv.put("next_advice", x.nextAdvice);
        cv.put("main_reason_code", x.mainReasonCode);
        cv.put("affected_module", x.affectedModule);
        cv.put("final_p", x.finalP);
        cv.put("dynamic_gate", x.dynamicGate);
        cv.put("edge", x.edge);
        cv.put("trend_score", x.trendScore);
        cv.put("shape_score", x.shapeScore);
        cv.put("drift_risk", x.driftRisk);
        cv.put("break_risk", x.breakRisk);
        cv.put("recovery_score", x.recoveryScore);
        cv.put("profile_score", x.profileScore);
        cv.put("number_bias", x.numberBias);
        cv.put("combo_bias", x.comboBias);
        cv.put("risk_score", x.riskScore);
        cv.put("teacher_weight", x.teacherWeight);
        cv.put("state_confidence", x.stateConfidence);
        cv.put("state_sample", x.stateSample);
        cv.put("cache_sample", x.cacheSample);
        cv.put("recent_sample", x.recentSample);
        cv.put("recent_hit_rate", x.recentHitRate);
        cv.put("saved", 1);

        return cv;
    }

    public boolean isLearningCaseExists(String targetIssue, int position) {
        if (targetIssue == null
                || targetIssue.length() == 0
                || !AppConfig.isValidPosition(position)) {
            return false;
        }

        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    TABLE_LEARNING_CASE_LOG,
                                    new String[] {"_id"},
                                    "target_issue=? AND position=?",
                                    new String[] {targetIssue, String.valueOf(position)},
                                    null,
                                    null,
                                    null,
                                    "1");

            return c.moveToFirst();
        } finally {
            closeCursor(c);
        }
    }

    public Models.LearningCase getLearningCase(String targetIssue, int position) {
        if (targetIssue == null
                || targetIssue.length() == 0
                || !AppConfig.isValidPosition(position)) {
            return null;
        }

        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    TABLE_LEARNING_CASE_LOG,
                                    null,
                                    "target_issue=? AND position=?",
                                    new String[] {targetIssue, String.valueOf(position)},
                                    null,
                                    null,
                                    "created_at DESC, _id DESC",
                                    "1");

            if (c.moveToFirst()) {
                return readLearningCase(c);
            }
        } finally {
            closeCursor(c);
        }

        return null;
    }

    public List<Models.LearningCase> getRecentLearningCases(int limit) {
        ArrayList<Models.LearningCase> list = new ArrayList<Models.LearningCase>();

        if (limit <= 0) {
            limit = 50;
        }

        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    TABLE_LEARNING_CASE_LOG,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    "created_at DESC, _id DESC",
                                    String.valueOf(limit));

            while (c.moveToNext()) {
                list.add(readLearningCase(c));
            }
        } finally {
            closeCursor(c);
        }

        return list;
    }

    public List<Models.LearningCase> loadRecentLearningCases(int limit) {
        return getRecentLearningCases(limit);
    }

    public int countLearningCases() {
        return countTable(TABLE_LEARNING_CASE_LOG, null, null);
    }

    public int countLearningCasesByTask(String taskKey) {
        if (taskKey == null || taskKey.length() == 0) {
            return 0;
        }

        return countTable(TABLE_LEARNING_CASE_LOG, "task_key=?", new String[] {taskKey});
    }

    private Models.LearningCase readLearningCase(Cursor c) {
        Models.LearningCase x = new Models.LearningCase();

        x.caseKey = getString(c, "case_key");
        x.taskKey = getString(c, "task_key");
        x.segmentKey = getString(c, "segment_key");
        x.baseIssue = getString(c, "base_issue");
        x.targetIssue = getString(c, "target_issue");
        x.createdAt = getLong(c, "created_at");
        x.targetIndex = getInt(c, "target_index");
        x.position = getInt(c, "position");
        x.actualNumber = getInt(c, "actual_number");
        x.predictedK3 = getInt(c, "predicted_k3");
        setLearningCaseK3Mask(x, getIntAny(c, "k3_mask", "predicted_k3"));
        x.hit = getInt(c, "hit");
        x.stateKey = getString(c, "state_key");
        x.stateMode = getString(c, "state_mode");
        x.trendKey = getString(c, "trend_key");
        x.shapeKey = getString(c, "shape_key");
        x.driftKey = getString(c, "drift_key");
        x.breakKey = getString(c, "break_key");
        x.recoveryKey = getString(c, "recovery_key");
        x.transitionKey = getString(c, "transition_key");
        x.expectedShape = getString(c, "expected_shape");
        x.actualShape = getString(c, "actual_shape");
        x.whyExpectedHit = getString(c, "why_expected_hit");
        x.whyActualHit = getString(c, "why_actual_hit");
        x.whyMiss = getString(c, "why_miss");
        x.nextAdvice = getString(c, "next_advice");
        x.mainReasonCode = getString(c, "main_reason_code");
        x.affectedModule = getString(c, "affected_module");
        x.finalP = getDouble(c, "final_p");
        x.dynamicGate = getDouble(c, "dynamic_gate");
        x.edge = getDouble(c, "edge");
        x.trendScore = getDouble(c, "trend_score");
        x.shapeScore = getDouble(c, "shape_score");
        x.driftRisk = getDouble(c, "drift_risk");
        x.breakRisk = getDouble(c, "break_risk");
        x.recoveryScore = getDouble(c, "recovery_score");
        x.profileScore = getDouble(c, "profile_score");
        x.numberBias = getDouble(c, "number_bias");
        x.comboBias = getDouble(c, "combo_bias");
        x.riskScore = getDouble(c, "risk_score");
        x.teacherWeight = getDouble(c, "teacher_weight");
        x.stateConfidence = getDouble(c, "state_confidence");
        x.stateSample = getInt(c, "state_sample");
        x.cacheSample = getInt(c, "cache_sample");
        x.recentSample = getInt(c, "recent_sample");
        x.recentHitRate = getDouble(c, "recent_hit_rate");

        return x;
    }

    public long upsertRuntimeProfile(Models.RuntimeProfileCache p) {
        if (p == null) {
            return -1L;
        }

        SQLiteDatabase db = writable();
        ensureRuntimeExtraTablesOnce(db);

        if (p.profileKey == null || p.profileKey.length() == 0) {
            p.profileKey = buildRuntimeProfileKey(p.stateKey, p.position, p.k3Mask);
        }

        ContentValues cv = runtimeProfileToValues(p);
        long id =
                insertFiltered(
                        db, TABLE_RUNTIME_PROFILE_CACHE, cv, SQLiteDatabase.CONFLICT_REPLACE);

        if (id >= 0L) {
            setState("runtime_profile_summary", buildProfileCacheSummary());
        }

        return id;
    }

    private ContentValues runtimeProfileToValues(Models.RuntimeProfileCache p) {
        ContentValues cv = new ContentValues();

        cv.put("profile_key", p.profileKey);
        cv.put("state_key", p.stateKey);
        cv.put("position", p.position);
        cv.put("k3_mask", p.k3Mask);
        cv.put("sample_count", p.sampleCount);
        cv.put("hit_count", p.hitCount);
        cv.put("hit_rate", p.hitRate);
        cv.put("recent_sample", p.recentSample);
        cv.put("recent_hit", p.recentHit);
        cv.put("recent_hit_rate", p.recentHitRate);
        cv.put("avg_final_p", p.avgFinalP);
        cv.put("avg_gate", p.avgGate);
        cv.put("risk_score", p.riskScore);
        cv.put("teacher_weight", p.teacherWeight);
        cv.put("profile_score", p.profileScore);
        cv.put("state_confidence", p.stateConfidence);
        cv.put("updated_at", p.updatedAt > 0L ? p.updatedAt : System.currentTimeMillis());
        cv.put("summary_text", p.summaryText);

        return cv;
    }

    public void updateRuntimeProfileFromLearningCase(Models.LearningCase x) {
        if (x == null) {
            return;
        }

        SQLiteDatabase db = writable();
        ensureRuntimeExtraTablesOnce(db);
        updateRuntimeProfileFromLearningCaseInternal(db, x);
        setState("runtime_profile_summary", buildProfileCacheSummary());
    }

    private void updateRuntimeProfileFromLearningCaseInternal(
            SQLiteDatabase db, Models.LearningCase x) {
        if (db == null || x == null) {
            return;
        }

        String stateKey = safeString(x.stateKey);

        if (stateKey.length() == 0 || !AppConfig.isValidPosition(x.position)) {
            return;
        }

        int k3Mask = getLearningCaseK3Mask(x);

        if (!Models.isValidK3Mask(k3Mask)) {
            return;
        }

        double p = x.finalP > 0.0d ? x.finalP : 0.0d;
        double gate = x.dynamicGate > 0.0d ? x.dynamicGate : 0.0d;
        double risk =
                normalizeRisk(
                        x.riskScore > 0.0d ? x.riskScore : Math.max(x.driftRisk, x.breakRisk));
        double teacherWeight = x.teacherWeight > 0.0d ? x.teacherWeight : 1.0d;
        double stateConfidence = x.stateConfidence > 0.0d ? x.stateConfidence : 0.0d;

        updateRuntimeProfileDirect(
                db,
                stateKey,
                x.position,
                k3Mask,
                x.hit == 1,
                p,
                gate,
                risk,
                teacherWeight,
                stateConfidence,
                1);
    }

    public int updateRuntimeProfileCache(
            String stateKey, boolean hit, double score, double risk, int sample) {
        return updateRuntimeProfileDirect(
                writable(),
                stateKey,
                0,
                0,
                hit,
                score,
                0.0d,
                risk,
                1.0d,
                confidenceFromSample(sample),
                sample);
    }

    public int updateRuntimeProfile(
            String stateKey, boolean hit, double score, double risk, int sample) {
        return updateRuntimeProfileCache(stateKey, hit, score, risk, sample);
    }

    public int learnRuntimeProfile(
            String stateKey, boolean hit, double score, double risk, int sample) {
        return updateRuntimeProfileCache(stateKey, hit, score, risk, sample);
    }

    public int upsertRuntimeProfile(
            String stateKey, boolean hit, double score, double risk, int sample) {
        return updateRuntimeProfileCache(stateKey, hit, score, risk, sample);
    }

    public int updateRuntimeProfileCache(
            String stateKey,
            int position,
            int k3Mask,
            boolean hit,
            double finalP,
            double gate,
            double risk,
            double teacherWeight,
            double stateConfidence) {

        return updateRuntimeProfileDirect(
                writable(),
                stateKey,
                position,
                k3Mask,
                hit,
                finalP,
                gate,
                risk,
                teacherWeight,
                stateConfidence,
                1);
    }

    public int updateRuntimeProfile(
            String stateKey,
            int position,
            int k3Mask,
            boolean hit,
            double finalP,
            double gate,
            double risk,
            double teacherWeight,
            double stateConfidence) {

        return updateRuntimeProfileCache(
                stateKey,
                position,
                k3Mask,
                hit,
                finalP,
                gate,
                risk,
                teacherWeight,
                stateConfidence);
    }

    public int learnRuntimeProfile(
            String stateKey,
            int position,
            int k3Mask,
            boolean hit,
            double finalP,
            double gate,
            double risk,
            double teacherWeight,
            double stateConfidence) {

        return updateRuntimeProfileCache(
                stateKey,
                position,
                k3Mask,
                hit,
                finalP,
                gate,
                risk,
                teacherWeight,
                stateConfidence);
    }

    public int upsertRuntimeProfile(
            String stateKey,
            int position,
            int k3Mask,
            boolean hit,
            double finalP,
            double gate,
            double risk,
            double teacherWeight,
            double stateConfidence) {

        return updateRuntimeProfileCache(
                stateKey,
                position,
                k3Mask,
                hit,
                finalP,
                gate,
                risk,
                teacherWeight,
                stateConfidence);
    }

    private int updateRuntimeProfileDirect(
            SQLiteDatabase db,
            String stateKey,
            int position,
            int k3Mask,
            boolean hit,
            double finalP,
            double gate,
            double risk,
            double teacherWeight,
            double stateConfidence,
            int sample) {

        if (db == null) {
            return 0;
        }

        stateKey = safeString(stateKey);

        if (stateKey.length() == 0) {
            return 0;
        }

        ensureRuntimeExtraTablesOnce(db);

        if (sample <= 0) {
            sample = 1;
        }

        if (sample > 20) {
            sample = 20;
        }

        finalP = AppConfig.clampProbability(finalP);
        gate = gate < 0.0d ? 0.0d : gate;
        risk = normalizeRisk(risk);

        if (teacherWeight <= 0.0d) {
            teacherWeight = 1.0d;
        }

        stateConfidence = AppConfig.clampProbability(stateConfidence);

        long now = System.currentTimeMillis();
        String profileKey = buildRuntimeProfileKey(stateKey, position, k3Mask);

        ContentValues init = new ContentValues();
        init.put("profile_key", profileKey);
        init.put("state_key", stateKey);
        init.put("position", position);
        init.put("k3_mask", k3Mask);
        init.put("sample_count", 0);
        init.put("hit_count", 0);
        init.put("hit_rate", 0.0d);
        init.put("recent_sample", 0);
        init.put("recent_hit", 0);
        init.put("recent_hit_rate", 0.0d);
        init.put("avg_final_p", 0.0d);
        init.put("avg_gate", 0.0d);
        init.put("risk_score", 0.0d);
        init.put("teacher_weight", 1.0d);
        init.put("profile_score", 0.0d);
        init.put("state_confidence", 0.0d);
        init.put("updated_at", now);
        init.put("summary_text", "");

        insertFiltered(db, TABLE_RUNTIME_PROFILE_CACHE, init, SQLiteDatabase.CONFLICT_IGNORE);

        int hitAdd = hit ? 1 : 0;

        db.execSQL(
                "UPDATE "
                        + TABLE_RUNTIME_PROFILE_CACHE
                        + " SET sample_count=sample_count+?,"
                        + "hit_count=hit_count+?,recent_sample=CASE WHEN recent_sample>=240 THEN"
                        + " 240 ELSE recent_sample+? END,recent_hit=CASE WHEN recent_sample>=240"
                        + " THEN recent_hit ELSE recent_hit+? END,avg_final_p=CASE WHEN"
                        + " sample_count<=0 THEN ? ELSE"
                        + " ((avg_final_p*sample_count)+?)/(sample_count+?) END,avg_gate=CASE WHEN"
                        + " sample_count<=0 THEN ? ELSE"
                        + " ((avg_gate*sample_count)+?)/(sample_count+?) END,risk_score=CASE WHEN"
                        + " sample_count<=0 THEN ? ELSE"
                        + " ((risk_score*sample_count)+?)/(sample_count+?) END,teacher_weight=CASE"
                        + " WHEN sample_count<=0 THEN ? ELSE"
                        + " ((teacher_weight*sample_count)+?)/(sample_count+?)"
                        + " END,state_confidence=CASE WHEN sample_count<=0 THEN ? ELSE"
                        + " ((state_confidence*sample_count)+?)/(sample_count+?) END,updated_at=?"
                        + " WHERE profile_key=?",
                new Object[] {
                    Integer.valueOf(sample),
                    Integer.valueOf(hitAdd),
                    Integer.valueOf(sample),
                    Integer.valueOf(hitAdd),
                    Double.valueOf(finalP),
                    Double.valueOf(finalP * sample),
                    Integer.valueOf(sample),
                    Double.valueOf(gate),
                    Double.valueOf(gate * sample),
                    Integer.valueOf(sample),
                    Double.valueOf(risk),
                    Double.valueOf(risk * sample),
                    Integer.valueOf(sample),
                    Double.valueOf(teacherWeight),
                    Double.valueOf(teacherWeight * sample),
                    Integer.valueOf(sample),
                    Double.valueOf(stateConfidence),
                    Double.valueOf(stateConfidence * sample),
                    Integer.valueOf(sample),
                    Long.valueOf(now),
                    profileKey
                });

        db.execSQL(
                "UPDATE "
                        + TABLE_RUNTIME_PROFILE_CACHE
                        + " SET hit_rate=CASE WHEN sample_count>0 THEN CAST(hit_count AS"
                        + " REAL)/sample_count ELSE 0 END,recent_hit_rate=CASE WHEN recent_sample>0"
                        + " THEN CAST(recent_hit AS REAL)/recent_sample ELSE 0"
                        + " END,profile_score=CASE WHEN sample_count>0 THEN ((CAST(hit_count AS"
                        + " REAL)/sample_count)*0.52 + (CASE WHEN recent_sample>0 THEN"
                        + " CAST(recent_hit AS REAL)/recent_sample ELSE 0 END)*0.28 +"
                        + " avg_final_p*0.15 + state_confidence*0.10 - risk_score*0.15) ELSE 0"
                        + " END,summary_text=? WHERE profile_key=?",
                new Object[] {
                    buildRuntimeProfileSummaryText(stateKey, position, k3Mask), profileKey
                });

        setState("runtime_profile_summary", buildProfileCacheSummary());
        return 1;
    }

    public Models.RuntimeProfileCache getRuntimeProfileCache(
            String stateKey, int position, int k3Mask) {
        if (stateKey == null
                || stateKey.length() == 0
                || !AppConfig.isValidPosition(position)
                || !Models.isValidK3Mask(k3Mask)) {
            return null;
        }

        String profileKey = buildRuntimeProfileKey(stateKey, position, k3Mask);
        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    TABLE_RUNTIME_PROFILE_CACHE,
                                    null,
                                    "profile_key=?",
                                    new String[] {profileKey},
                                    null,
                                    null,
                                    null,
                                    "1");

            if (c.moveToFirst()) {
                return readRuntimeProfileCache(c);
            }
        } finally {
            closeCursor(c);
        }

        return null;
    }

    public Models.RuntimeProfileCache loadRuntimeProfile(
            String stateKey, int position, int k3Mask) {
        return getRuntimeProfileCache(stateKey, position, k3Mask);
    }

    public List<Models.RuntimeProfileCache> getRuntimeProfileCaches(
            String stateKey, int position, int limit) {
        ArrayList<Models.RuntimeProfileCache> list = new ArrayList<Models.RuntimeProfileCache>();

        if (stateKey == null || stateKey.length() == 0 || !AppConfig.isValidPosition(position)) {
            return list;
        }

        if (limit <= 0) {
            limit = 20;
        }

        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    TABLE_RUNTIME_PROFILE_CACHE,
                                    null,
                                    "state_key=? AND position=?",
                                    new String[] {stateKey, String.valueOf(position)},
                                    null,
                                    null,
                                    "profile_score DESC, recent_hit_rate DESC, sample_count DESC",
                                    String.valueOf(limit));

            while (c.moveToNext()) {
                list.add(readRuntimeProfileCache(c));
            }
        } finally {
            closeCursor(c);
        }

        return list;
    }

    public List<Models.RuntimeProfileCache> getTopRuntimeProfileCaches(int limit) {
        ArrayList<Models.RuntimeProfileCache> list = new ArrayList<Models.RuntimeProfileCache>();

        if (limit <= 0) {
            limit = 20;
        }

        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    TABLE_RUNTIME_PROFILE_CACHE,
                                    null,
                                    "sample_count>=?",
                                    new String[] {"5"},
                                    null,
                                    null,
                                    "profile_score DESC, recent_hit_rate DESC, sample_count DESC",
                                    String.valueOf(limit));

            while (c.moveToNext()) {
                list.add(readRuntimeProfileCache(c));
            }
        } finally {
            closeCursor(c);
        }

        return list;
    }

    private Models.RuntimeProfileCache readRuntimeProfileCache(Cursor c) {
        Models.RuntimeProfileCache p = new Models.RuntimeProfileCache();

        p.profileKey = getString(c, "profile_key");
        p.stateKey = getString(c, "state_key");
        p.position = getInt(c, "position");
        p.k3Mask = getInt(c, "k3_mask");
        p.sampleCount = getInt(c, "sample_count");
        p.hitCount = getInt(c, "hit_count");
        p.hitRate = getDouble(c, "hit_rate");
        p.recentSample = getInt(c, "recent_sample");
        p.recentHit = getInt(c, "recent_hit");
        p.recentHitRate = getDouble(c, "recent_hit_rate");
        p.avgFinalP = getDouble(c, "avg_final_p");
        p.avgGate = getDouble(c, "avg_gate");
        p.riskScore = getDouble(c, "risk_score");
        p.teacherWeight = getDouble(c, "teacher_weight");
        p.profileScore = getDouble(c, "profile_score");
        p.stateConfidence = getDouble(c, "state_confidence");
        p.updatedAt = getLong(c, "updated_at");
        p.summaryText = getString(c, "summary_text");

        return p;
    }

    public long upsertStateNumberProfile(Models.StateNumberProfile p) {
        if (p == null) {
            return -1L;
        }

        SQLiteDatabase db = writable();
        ensureRuntimeExtraTablesOnce(db);

        if (p.profileKey == null || p.profileKey.length() == 0) {
            p.profileKey = buildStateNumberProfileKey(p.stateKey, p.position, p.number);
        }

        ContentValues cv = stateNumberProfileToValues(p);
        long id =
                insertFiltered(db, TABLE_STATE_NUMBER_PROFILE, cv, SQLiteDatabase.CONFLICT_REPLACE);

        if (id >= 0L) {
            setState("runtime_profile_summary", buildProfileCacheSummary());
        }

        return id;
    }

    private ContentValues stateNumberProfileToValues(Models.StateNumberProfile p) {
        ContentValues cv = new ContentValues();

        cv.put("profile_key", p.profileKey);
        cv.put("state_key", p.stateKey);
        cv.put("position", p.position);
        cv.put("number", p.number);
        cv.put("sample_count", p.sampleCount);
        cv.put("hit_count", p.hitCount);
        cv.put("hit_rate", p.hitRate);
        cv.put("recent_sample", p.recentSample);
        cv.put("recent_hit", p.recentHit);
        cv.put("recent_hit_rate", p.recentHitRate);
        cv.put("trend_score", p.trendScore);
        cv.put("shape_score", p.shapeScore);
        cv.put("risk_score", p.riskScore);
        cv.put("number_bias", p.numberBias);
        cv.put("confidence", p.confidence);
        cv.put("why_text", p.whyText);
        cv.put("updated_at", p.updatedAt > 0L ? p.updatedAt : System.currentTimeMillis());

        return cv;
    }

    public void updateStateNumberProfileFromLearningCase(Models.LearningCase x) {
        if (x == null) {
            return;
        }

        SQLiteDatabase db = writable();
        ensureRuntimeExtraTablesOnce(db);
        updateStateNumberProfileFromLearningCaseInternal(db, x);
        setState("runtime_profile_summary", buildProfileCacheSummary());
    }

    private void updateStateNumberProfileFromLearningCaseInternal(
            SQLiteDatabase db, Models.LearningCase x) {
        if (db == null || x == null) {
            return;
        }

        String stateKey = safeString(x.stateKey);

        if (stateKey.length() == 0
                || !AppConfig.isValidPosition(x.position)
                || !AppConfig.isValidNumber(x.actualNumber)) {
            return;
        }

        int predictedK3 = getLearningCaseK3Mask(x);

        for (int n = 1; n <= 10; n++) {
            boolean isActual = n == x.actualNumber;
            boolean inK3 = maskContainsNumber(predictedK3, n);
            double trendScore = isActual ? Math.max(0.01d, x.trendScore) : 0.0d;
            double shapeScore = isActual ? Math.max(0.01d, x.shapeScore) : 0.0d;
            double risk =
                    normalizeRisk(
                            x.riskScore > 0.0d ? x.riskScore : Math.max(x.driftRisk, x.breakRisk));
            double support = 0.0d;

            if (isActual) {
                support += 0.18d;
            }

            if (inK3 && isActual) {
                support += 0.12d;
            }

            if (inK3 && !isActual) {
                support -= 0.04d;
            }

            support += (trendScore * 0.08d);
            support += (shapeScore * 0.06d);
            support -= (risk * 0.08d);
            support = limitAdjustLocal(support, 0.30d);

            updateStateNumberProfileDirect(
                    db,
                    stateKey,
                    x.position,
                    n,
                    isActual,
                    support,
                    risk,
                    1,
                    trendScore,
                    shapeScore,
                    inK3);
        }
    }

    public int updateStateNumberProfile(
            String stateKey,
            int position,
            int number,
            boolean hit,
            double support,
            double risk,
            int sample) {
        return updateStateNumberProfileDirect(
                writable(),
                stateKey,
                position,
                number,
                hit,
                support,
                risk,
                sample,
                support,
                support,
                false);
    }

    public int updateStateNumberProfileCache(
            String stateKey,
            int position,
            int number,
            boolean hit,
            double support,
            double risk,
            int sample) {
        return updateStateNumberProfile(stateKey, position, number, hit, support, risk, sample);
    }

    public int learnStateNumberProfile(
            String stateKey,
            int position,
            int number,
            boolean hit,
            double support,
            double risk,
            int sample) {
        return updateStateNumberProfile(stateKey, position, number, hit, support, risk, sample);
    }

    public int upsertStateNumberProfile(
            String stateKey,
            int position,
            int number,
            boolean hit,
            double support,
            double risk,
            int sample) {
        return updateStateNumberProfile(stateKey, position, number, hit, support, risk, sample);
    }

    private int updateStateNumberProfileDirect(
            SQLiteDatabase db,
            String stateKey,
            int position,
            int number,
            boolean hit,
            double support,
            double risk,
            int sample,
            double trendScore,
            double shapeScore,
            boolean inK3) {

        if (db == null) {
            return 0;
        }

        stateKey = safeString(stateKey);

        if (stateKey.length() == 0
                || !AppConfig.isValidPosition(position)
                || !AppConfig.isValidNumber(number)) {
            return 0;
        }

        ensureRuntimeExtraTablesOnce(db);

        if (sample <= 0) {
            sample = 1;
        }

        if (sample > 20) {
            sample = 20;
        }

        risk = normalizeRisk(risk);
        support = limitAdjustLocal(support, 0.35d);
        trendScore = AppConfig.clampProbability(Math.abs(trendScore));
        shapeScore = AppConfig.clampProbability(Math.abs(shapeScore));

        long now = System.currentTimeMillis();
        String profileKey = buildStateNumberProfileKey(stateKey, position, number);

        ContentValues init = new ContentValues();
        init.put("profile_key", profileKey);
        init.put("state_key", stateKey);
        init.put("position", position);
        init.put("number", number);
        init.put("sample_count", 0);
        init.put("hit_count", 0);
        init.put("hit_rate", 0.0d);
        init.put("recent_sample", 0);
        init.put("recent_hit", 0);
        init.put("recent_hit_rate", 0.0d);
        init.put("trend_score", 0.0d);
        init.put("shape_score", 0.0d);
        init.put("risk_score", 0.0d);
        init.put("number_bias", 0.0d);
        init.put("confidence", 0.0d);
        init.put("why_text", "");
        init.put("updated_at", now);

        insertFiltered(db, TABLE_STATE_NUMBER_PROFILE, init, SQLiteDatabase.CONFLICT_IGNORE);

        int hitAdd = hit ? 1 : 0;

        db.execSQL(
                "UPDATE "
                        + TABLE_STATE_NUMBER_PROFILE
                        + " SET sample_count=sample_count+?,"
                        + "hit_count=hit_count+?,recent_sample=CASE WHEN recent_sample>=240 THEN"
                        + " 240 ELSE recent_sample+? END,recent_hit=CASE WHEN recent_sample>=240"
                        + " THEN recent_hit ELSE recent_hit+? END,trend_score=CASE WHEN"
                        + " sample_count<=0 THEN ? ELSE"
                        + " ((trend_score*sample_count)+?)/(sample_count+?) END,shape_score=CASE"
                        + " WHEN sample_count<=0 THEN ? ELSE"
                        + " ((shape_score*sample_count)+?)/(sample_count+?) END,risk_score=CASE"
                        + " WHEN sample_count<=0 THEN ? ELSE"
                        + " ((risk_score*sample_count)+?)/(sample_count+?) END,number_bias=CASE"
                        + " WHEN sample_count<=0 THEN ? ELSE"
                        + " ((number_bias*sample_count)+?)/(sample_count+?) END,updated_at=? WHERE"
                        + " profile_key=?",
                new Object[] {
                    Integer.valueOf(sample),
                    Integer.valueOf(hitAdd),
                    Integer.valueOf(sample),
                    Integer.valueOf(hitAdd),
                    Double.valueOf(trendScore),
                    Double.valueOf(trendScore * sample),
                    Integer.valueOf(sample),
                    Double.valueOf(shapeScore),
                    Double.valueOf(shapeScore * sample),
                    Integer.valueOf(sample),
                    Double.valueOf(risk),
                    Double.valueOf(risk * sample),
                    Integer.valueOf(sample),
                    Double.valueOf(support),
                    Double.valueOf(support * sample),
                    Integer.valueOf(sample),
                    Long.valueOf(now),
                    profileKey
                });

        db.execSQL(
                "UPDATE "
                        + TABLE_STATE_NUMBER_PROFILE
                        + " SET hit_rate=CASE WHEN sample_count>0 THEN CAST(hit_count AS"
                        + " REAL)/sample_count ELSE 0 END,recent_hit_rate=CASE WHEN recent_sample>0"
                        + " THEN CAST(recent_hit AS REAL)/recent_sample ELSE 0 END,confidence=CASE"
                        + " WHEN sample_count>=80 THEN 1.0 WHEN sample_count>0 THEN"
                        + " CAST(sample_count AS REAL)/80.0 ELSE 0 END,why_text=? WHERE"
                        + " profile_key=?",
                new Object[] {
                    buildNumberProfileWhyText(stateKey, position, number, hit, inK3), profileKey
                });

        setState("runtime_profile_summary", buildProfileCacheSummary());
        return 1;
    }

    public Models.StateNumberProfile getStateNumberProfile(
            String stateKey, int position, int number) {
        if (stateKey == null
                || stateKey.length() == 0
                || !AppConfig.isValidPosition(position)
                || !AppConfig.isValidNumber(number)) {
            return null;
        }

        String profileKey = buildStateNumberProfileKey(stateKey, position, number);
        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    TABLE_STATE_NUMBER_PROFILE,
                                    null,
                                    "profile_key=?",
                                    new String[] {profileKey},
                                    null,
                                    null,
                                    null,
                                    "1");

            if (c.moveToFirst()) {
                return readStateNumberProfile(c);
            }
        } finally {
            closeCursor(c);
        }

        return null;
    }

    public List<Models.StateNumberProfile> getStateNumberProfiles(String stateKey, int position) {
        ArrayList<Models.StateNumberProfile> list = new ArrayList<Models.StateNumberProfile>();

        if (stateKey == null || stateKey.length() == 0 || !AppConfig.isValidPosition(position)) {
            return list;
        }

        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    TABLE_STATE_NUMBER_PROFILE,
                                    null,
                                    "state_key=? AND position=?",
                                    new String[] {stateKey, String.valueOf(position)},
                                    null,
                                    null,
                                    "number_bias DESC, recent_hit_rate DESC, hit_rate DESC,"
                                        + " sample_count DESC");

            while (c.moveToNext()) {
                list.add(readStateNumberProfile(c));
            }
        } finally {
            closeCursor(c);
        }

        return list;
    }

    public List<Models.StateNumberProfile> loadNumberProfiles(String stateKey, int position) {
        return getStateNumberProfiles(stateKey, position);
    }

    public List<Models.StateNumberProfile> getTopStateNumberProfiles(
            String stateKey, int position, int limit) {
        ArrayList<Models.StateNumberProfile> list = new ArrayList<Models.StateNumberProfile>();

        if (stateKey == null || stateKey.length() == 0 || !AppConfig.isValidPosition(position)) {
            return list;
        }

        if (limit <= 0) {
            limit = 10;
        }

        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    TABLE_STATE_NUMBER_PROFILE,
                                    null,
                                    "state_key=? AND position=?",
                                    new String[] {stateKey, String.valueOf(position)},
                                    null,
                                    null,
                                    "number_bias DESC, confidence DESC, recent_hit_rate DESC",
                                    String.valueOf(limit));

            while (c.moveToNext()) {
                list.add(readStateNumberProfile(c));
            }
        } finally {
            closeCursor(c);
        }

        return list;
    }

    private Models.StateNumberProfile readStateNumberProfile(Cursor c) {
        Models.StateNumberProfile p = new Models.StateNumberProfile();

        p.profileKey = getString(c, "profile_key");
        p.stateKey = getString(c, "state_key");
        p.position = getInt(c, "position");
        p.number = getInt(c, "number");
        p.sampleCount = getInt(c, "sample_count");
        p.hitCount = getInt(c, "hit_count");
        p.hitRate = getDouble(c, "hit_rate");
        p.recentSample = getInt(c, "recent_sample");
        p.recentHit = getInt(c, "recent_hit");
        p.recentHitRate = getDouble(c, "recent_hit_rate");
        p.trendScore = getDouble(c, "trend_score");
        p.shapeScore = getDouble(c, "shape_score");
        p.riskScore = getDouble(c, "risk_score");
        p.numberBias = getDouble(c, "number_bias");
        p.confidence = getDouble(c, "confidence");
        p.whyText = getString(c, "why_text");
        p.updatedAt = getLong(c, "updated_at");

        return p;
    }

    public long saveAutoLearnCursor(Models.AutoLearnTask t) {
        if (t == null || t.taskKey == null || t.taskKey.length() == 0) {
            return -1L;
        }

        SQLiteDatabase db = writable();
        ensureRuntimeExtraTablesOnce(db);

        long now = System.currentTimeMillis();

        if (t.updatedAt <= 0L) {
            t.updatedAt = now;
        }

        if (t.status == null || t.status.length() == 0) {
            t.status = AUTO_LEARN_STATUS_IDLE;
        }

        String oldUsedSegments = getAutoLearnUsedSegments(t.taskKey);
        String incomingUsedSegments = getOptionalStringField(t, "usedSegments", "");
        String mergedUsedSegments = mergeUsedSegments(oldUsedSegments, incomingUsedSegments);

        int lastStartIndex = getOptionalIntField(t, "lastStartIndex", 0);

        if (lastStartIndex <= 0) {
            lastStartIndex = getAutoLearnLastStartIndex(t.taskKey);
        }

        if (lastStartIndex <= 0) {
            lastStartIndex = t.segmentStartIndex;
        }

        ContentValues cv = new ContentValues();

        cv.put("task_key", t.taskKey);
        cv.put("task_type", t.taskType);
        cv.put("window_size", t.windowSize);
        cv.put("segment_start_index", t.segmentStartIndex);
        cv.put("segment_end_index", t.segmentEndIndex);
        cv.put("next_target_index", t.nextTargetIndex);
        cv.put("last_start_index", lastStartIndex);
        cv.put("random_seed", t.randomSeed);
        cv.put("last_issue", t.lastIssue);
        cv.put("progress", t.progress);
        cv.put("status", t.status);
        cv.put("used_segments", mergedUsedSegments);
        cv.put("updated_at", t.updatedAt);

        setOptionalIntField(t, "lastStartIndex", lastStartIndex);
        setOptionalStringField(t, "usedSegments", mergedUsedSegments);

        long id = insertFiltered(db, TABLE_AUTO_LEARN_CURSOR, cv, SQLiteDatabase.CONFLICT_REPLACE);

        setState("auto_learn_status", t.status);
        setState("auto_learn_task", t.taskKey);
        setState("auto_learn_progress", t.progress == null ? "" : t.progress);
        setState("auto_learn_summary", buildAutoLearnSummary(t));

        return id;
    }

    public long saveAutoLearnCursor(Models.AutoLearnCursor cursor) {
        if (cursor == null || cursor.taskKey == null || cursor.taskKey.length() == 0) {
            return -1L;
        }

        SQLiteDatabase db = writable();
        ensureRuntimeExtraTablesOnce(db);

        long now = System.currentTimeMillis();

        if (cursor.updatedAt <= 0L) {
            cursor.updatedAt = now;
        }

        if (cursor.status == null || cursor.status.length() == 0) {
            cursor.status = AUTO_LEARN_STATUS_IDLE;
        }

        cursor.usedSegments =
                mergeUsedSegments(getAutoLearnUsedSegments(cursor.taskKey), cursor.usedSegments);

        if (cursor.lastStartIndex <= 0) {
            cursor.lastStartIndex = getAutoLearnLastStartIndex(cursor.taskKey);
        }

        if (cursor.lastStartIndex <= 0) {
            cursor.lastStartIndex = cursor.segmentStartIndex;
        }

        ContentValues cv = new ContentValues();

        cv.put("task_key", cursor.taskKey);
        cv.put("task_type", cursor.taskType);
        cv.put("window_size", cursor.windowSize);
        cv.put("segment_start_index", cursor.segmentStartIndex);
        cv.put("segment_end_index", cursor.segmentEndIndex);
        cv.put("next_target_index", cursor.nextTargetIndex);
        cv.put("last_start_index", cursor.lastStartIndex);
        cv.put("random_seed", cursor.randomSeed);
        cv.put("last_issue", cursor.lastIssue);
        cv.put("progress", cursor.progress);
        cv.put("status", cursor.status);
        cv.put("used_segments", cursor.usedSegments);
        cv.put("updated_at", cursor.updatedAt);

        long id = insertFiltered(db, TABLE_AUTO_LEARN_CURSOR, cv, SQLiteDatabase.CONFLICT_REPLACE);

        setState("auto_learn_status", cursor.status);
        setState("auto_learn_task", cursor.taskKey);
        setState("auto_learn_progress", cursor.progress == null ? "" : cursor.progress);
        setState("auto_learn_summary", buildAutoLearnCursorSummary(cursor));

        return id;
    }

    public Models.AutoLearnTask getAutoLearnCursor(String taskKey) {
        if (taskKey == null || taskKey.length() == 0) {
            return null;
        }

        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    TABLE_AUTO_LEARN_CURSOR,
                                    null,
                                    "task_key=?",
                                    new String[] {taskKey},
                                    null,
                                    null,
                                    "updated_at DESC, _id DESC",
                                    "1");

            if (c.moveToFirst()) {
                return readAutoLearnTask(c);
            }
        } finally {
            closeCursor(c);
        }

        return null;
    }

    public Models.AutoLearnCursor loadAutoLearnCursor(String taskKey) {
        if (taskKey == null || taskKey.length() == 0) {
            return null;
        }

        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    TABLE_AUTO_LEARN_CURSOR,
                                    null,
                                    "task_key=?",
                                    new String[] {taskKey},
                                    null,
                                    null,
                                    "updated_at DESC, _id DESC",
                                    "1");

            if (c.moveToFirst()) {
                return readAutoLearnCursor(c);
            }
        } finally {
            closeCursor(c);
        }

        return null;
    }

    public Models.AutoLearnTask getLatestAutoLearnCursor() {
        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    TABLE_AUTO_LEARN_CURSOR,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    "CASE WHEN status='"
                                            + AUTO_LEARN_STATUS_RUNNING
                                            + "' THEN 0 ELSE 1 END ASC, updated_at DESC, _id DESC",
                                    "1");

            if (c.moveToFirst()) {
                return readAutoLearnTask(c);
            }
        } finally {
            closeCursor(c);
        }

        return null;
    }

    public Models.AutoLearnTask getRunningAutoLearnCursor() {
        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    TABLE_AUTO_LEARN_CURSOR,
                                    null,
                                    "status=?",
                                    new String[] {AUTO_LEARN_STATUS_RUNNING},
                                    null,
                                    null,
                                    "updated_at DESC, _id DESC",
                                    "1");

            if (c.moveToFirst()) {
                return readAutoLearnTask(c);
            }
        } finally {
            closeCursor(c);
        }

        return null;
    }

    public void markAutoLearnSegmentDone(String taskKey, String segmentText, String lastIssue) {
        if (taskKey == null || taskKey.length() == 0) {
            return;
        }

        SQLiteDatabase db = writable();
        ensureRuntimeExtraTablesOnce(db);

        String used = getAutoLearnUsedSegments(taskKey);

        if (segmentText != null
                && segmentText.length() > 0
                && !containsSegment(used, segmentText)) {
            if (used.length() > 0) {
                used += ",";
            }

            used += segmentText;
        }

        used = trimUsedSegments(used);

        ContentValues cv = new ContentValues();
        cv.put("used_segments", used);
        cv.put("last_issue", lastIssue == null ? "" : lastIssue);
        cv.put("status", AUTO_LEARN_STATUS_IDLE);
        cv.put("progress", "段完成：" + safeString(segmentText));
        cv.put("updated_at", System.currentTimeMillis());

        updateFiltered(db, TABLE_AUTO_LEARN_CURSOR, cv, "task_key=?", new String[] {taskKey});

        setState("auto_learn_status", AUTO_LEARN_STATUS_IDLE);
        setState("auto_learn_progress", "段完成：" + safeString(segmentText));
        setState("auto_learn_summary", getAutoLearnStatusText());
    }

    public void markSegmentDone(String taskKey, int startIndex, int endIndex, String lastIssue) {
        markAutoLearnSegmentDone(
                taskKey, String.valueOf(startIndex) + "-" + String.valueOf(endIndex), lastIssue);
    }

    public boolean isAutoLearnSegmentUsed(String taskKey, int startIndex, int endIndex) {
        String used = getAutoLearnUsedSegments(taskKey);
        String seg = String.valueOf(startIndex) + "-" + String.valueOf(endIndex);

        return containsSegment(used, seg);
    }

    private String getAutoLearnUsedSegments(String taskKey) {
        if (taskKey == null || taskKey.length() == 0) {
            return "";
        }

        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    TABLE_AUTO_LEARN_CURSOR,
                                    new String[] {"used_segments"},
                                    "task_key=?",
                                    new String[] {taskKey},
                                    null,
                                    null,
                                    "updated_at DESC, _id DESC",
                                    "1");

            if (c.moveToFirst()) {
                return getString(c, "used_segments");
            }
        } finally {
            closeCursor(c);
        }

        return "";
    }

    private int getAutoLearnLastStartIndex(String taskKey) {
        if (taskKey == null || taskKey.length() == 0) {
            return 0;
        }

        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    TABLE_AUTO_LEARN_CURSOR,
                                    new String[] {"last_start_index"},
                                    "task_key=?",
                                    new String[] {taskKey},
                                    null,
                                    null,
                                    "updated_at DESC, _id DESC",
                                    "1");

            if (c.moveToFirst()) {
                return getInt(c, "last_start_index");
            }
        } finally {
            closeCursor(c);
        }

        return 0;
    }

    private boolean containsSegment(String used, String seg) {
        if (used == null || used.length() == 0 || seg == null || seg.length() == 0) {
            return false;
        }

        String[] parts = used.split(",");

        for (int i = 0; i < parts.length; i++) {
            String item = parts[i] == null ? "" : parts[i].trim();

            if (seg.equals(item)) {
                return true;
            }
        }

        return false;
    }

    private String mergeUsedSegments(String oldText, String newText) {
        String oldValue = oldText == null ? "" : oldText;
        String newValue = newText == null ? "" : newText;

        if (oldValue.length() == 0) {
            return trimUsedSegments(newValue);
        }

        if (newValue.length() == 0) {
            return trimUsedSegments(oldValue);
        }

        StringBuilder sb = new StringBuilder();
        ArrayList<String> parts = new ArrayList<String>();

        addSegmentsToList(parts, oldValue);
        addSegmentsToList(parts, newValue);

        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }

            sb.append(parts.get(i));
        }

        return trimUsedSegments(sb.toString());
    }

    private void addSegmentsToList(ArrayList<String> list, String text) {
        if (list == null || text == null || text.length() == 0) {
            return;
        }

        String[] arr = text.split(",");

        for (int i = 0; i < arr.length; i++) {
            String s = arr[i];

            if (s == null) {
                continue;
            }

            s = s.trim();

            if (s.length() == 0) {
                continue;
            }

            if (!list.contains(s)) {
                list.add(s);
            }
        }
    }

    private String trimUsedSegments(String text) {
        if (text == null || text.length() == 0) {
            return "";
        }

        String[] arr = text.split(",");
        ArrayList<String> list = new ArrayList<String>();

        for (int i = 0; i < arr.length; i++) {
            String s = arr[i];

            if (s == null) {
                continue;
            }

            s = s.trim();

            if (s.length() == 0) {
                continue;
            }

            if (!list.contains(s)) {
                list.add(s);
            }
        }

        int keep = 160;
        int start = list.size() - keep;

        if (start < 0) {
            start = 0;
        }

        StringBuilder sb = new StringBuilder();

        for (int i = start; i < list.size(); i++) {
            if (sb.length() > 0) {
                sb.append(",");
            }

            sb.append(list.get(i));
        }

        return sb.toString();
    }

    private Models.AutoLearnTask readAutoLearnTask(Cursor c) {
        Models.AutoLearnTask t = new Models.AutoLearnTask();

        t.taskKey = getString(c, "task_key");
        t.taskType = getString(c, "task_type");
        t.windowSize = getInt(c, "window_size");
        t.segmentStartIndex = getInt(c, "segment_start_index");
        t.segmentEndIndex = getInt(c, "segment_end_index");
        t.nextTargetIndex = getInt(c, "next_target_index");
        t.randomSeed = getLong(c, "random_seed");
        t.lastIssue = getString(c, "last_issue");
        t.progress = getString(c, "progress");
        t.status = getString(c, "status");
        t.updatedAt = getLong(c, "updated_at");

        setOptionalIntField(t, "lastStartIndex", getInt(c, "last_start_index"));
        setOptionalStringField(t, "usedSegments", getString(c, "used_segments"));

        return t;
    }

    private Models.AutoLearnCursor readAutoLearnCursor(Cursor c) {
        Models.AutoLearnCursor cursor = new Models.AutoLearnCursor();

        cursor.taskKey = getString(c, "task_key");
        cursor.taskType = getString(c, "task_type");
        cursor.windowSize = getInt(c, "window_size");
        cursor.segmentStartIndex = getInt(c, "segment_start_index");
        cursor.segmentEndIndex = getInt(c, "segment_end_index");
        cursor.nextTargetIndex = getInt(c, "next_target_index");
        cursor.lastStartIndex = getInt(c, "last_start_index");
        cursor.randomSeed = getLong(c, "random_seed");
        cursor.lastIssue = getString(c, "last_issue");
        cursor.progress = getString(c, "progress");
        cursor.status = getString(c, "status");
        cursor.usedSegments = getString(c, "used_segments");
        cursor.updatedAt = getLong(c, "updated_at");

        return cursor;
    }

    public String getAutoLearnStatusText() {
        Models.AutoLearnTask t = getLatestAutoLearnCursor();

        if (t == null) {
            return "后台学习：未开始｜等待历史数据满足30期后进入受控启动";
        }

        return buildAutoLearnSummary(t);
    }

    private String buildAutoLearnSummary(Models.AutoLearnTask t) {
        if (t == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("后台学习：");
        sb.append(t.status == null || t.status.length() == 0 ? AUTO_LEARN_STATUS_IDLE : t.status);

        if (t.taskKey != null && t.taskKey.length() > 0) {
            sb.append("｜任务=");
            sb.append(t.taskKey);
        }

        if (t.windowSize > 0) {
            sb.append("｜窗口=");
            sb.append(t.windowSize);
        }

        sb.append("｜段=");
        sb.append(t.segmentStartIndex);
        sb.append("-");
        sb.append(t.segmentEndIndex);
        sb.append("｜下标=");
        sb.append(t.nextTargetIndex);

        if (t.lastIssue != null && t.lastIssue.length() > 0) {
            sb.append("｜最近学习=");
            sb.append(t.lastIssue);
        }

        if (t.progress != null && t.progress.length() > 0) {
            sb.append("｜");
            sb.append(t.progress);
        }

        sb.append("｜说明=后台学习不抢实时主链，不直接创建BET");

        return sb.toString();
    }

    private String buildAutoLearnCursorSummary(Models.AutoLearnCursor t) {
        if (t == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("后台学习：");
        sb.append(t.status == null || t.status.length() == 0 ? AUTO_LEARN_STATUS_IDLE : t.status);

        if (t.taskKey != null && t.taskKey.length() > 0) {
            sb.append("｜任务=");
            sb.append(t.taskKey);
        }

        if (t.windowSize > 0) {
            sb.append("｜窗口=");
            sb.append(t.windowSize);
        }

        sb.append("｜段=");
        sb.append(t.segmentStartIndex);
        sb.append("-");
        sb.append(t.segmentEndIndex);
        sb.append("｜下标=");
        sb.append(t.nextTargetIndex);

        if (t.lastIssue != null && t.lastIssue.length() > 0) {
            sb.append("｜最近学习=");
            sb.append(t.lastIssue);
        }

        if (t.progress != null && t.progress.length() > 0) {
            sb.append("｜");
            sb.append(t.progress);
        }

        sb.append("｜说明=后台学习不抢实时主链，不直接创建BET");

        return sb.toString();
    }

    public long saveBacktestReport(Models.BacktestReport r) {
        if (r == null) {
            return -1L;
        }

        r.recalc();

        SQLiteDatabase db = writable();
        ensureRuntimeExtraTablesOnce(db);

        ContentValues cv = backtestReportToValues(r);
        long id = insertFiltered(db, DbHelper.TABLE_BACKTEST, cv, SQLiteDatabase.CONFLICT_NONE);

        String summary =
                r.summary == null || r.summary.length() == 0
                        ? buildBacktestSummaryText(r)
                        : r.summary;

        setState("backtest_last_summary", summary);
        setState("backtest_last_window", String.valueOf(r.window));
        setState("backtest_progress", String.valueOf(r.progress));
        setState("backtest_mode", r.mode == null ? "" : r.mode);

        if (id >= 0L) {
            saveTeacherSnapshotFromBacktest(r);
        }

        return id;
    }

    private ContentValues backtestReportToValues(Models.BacktestReport r) {
        ContentValues cv = new ContentValues();

        cv.put("ok", r.ok);
        cv.put("mode", r.mode);
        cv.put("type", r.type);
        cv.put("window_no", r.window);
        cv.put("window", r.window);

        cv.put(
                "range_start",
                r.rangeStart == null || r.rangeStart.length() == 0 ? r.startIssue : r.rangeStart);
        cv.put(
                "range_end",
                r.rangeEnd == null || r.rangeEnd.length() == 0 ? r.endIssue : r.rangeEnd);
        cv.put("start_issue", r.startIssue);
        cv.put("end_issue", r.endIssue);

        cv.put("train_count", r.trainCount);
        cv.put("test_count", r.testCount > 0 ? r.testCount : r.totalCount);
        cv.put("total_count", r.totalCount);

        cv.put("bet_count", r.betCount);
        cv.put("hit_count", r.hitCount);
        cv.put("miss_count", r.missCount);
        cv.put("hold_count", r.holdCount);
        cv.put("shadow_count", r.shadowCount);

        cv.put("candidate_count", r.candidateCount);
        cv.put("enrich_count", r.enrichCount);
        cv.put("deep_count", r.deepCount);

        cv.put("hit_rate", r.hitRate > 0.0d ? r.hitRate : r.winRate);
        cv.put("win_rate", r.winRate);
        cv.put("frequency", r.frequency);
        cv.put("pnl", r.pnl);
        cv.put("max_drawdown", r.maxDrawdown);
        cv.put("longest_lose", r.longestLose);

        cv.put("avg_raw_p", r.avgRawP);
        cv.put("avg_final_p", r.avgFinalP);
        cv.put("avg_gate", r.avgGate);
        cv.put("avg_edge", r.avgEdge);
        cv.put("calibration_error", r.calibrationError);

        cv.put("status", r.status == null || r.status.length() == 0 ? r.summary : r.status);
        cv.put("summary", r.summary);
        cv.put("started_at", r.startedAt);
        cv.put("finished_at", r.finishedAt);
        cv.put("cost_ms", r.costMs);
        cv.put("created_at", r.createdAt > 0L ? r.createdAt : System.currentTimeMillis());

        cv.put("manual_triggered", r.manualTriggered);
        cv.put("profile_count", r.profileCount);
        cv.put("attribution_count", r.attributionCount);
        cv.put("promoted_count", r.promotedCount);
        cv.put("rejected_count", r.rejectedCount);
        cv.put("a_plus_count", r.aPlusCount);
        cv.put("progress", r.progress);
        cv.put("a_plus_hit_rate", r.aPlusHitRate);
        cv.put("bias_score_avg", r.biasScoreAvg);
        cv.put("brier_score", r.brierScore);
        cv.put("ai_judge_avg_score", r.aiJudgeAvgScore);
        cv.put("deep_score_avg", r.deepScoreAvg);
        cv.put("bias_summary", r.biasSummary);
        cv.put("audit_summary", r.auditSummary);
        cv.put("promotion_summary", r.promotionSummary);
        cv.put("a_plus_summary", r.aPlusSummary);
        cv.put("learning_impact_summary", r.learningImpactSummary);
        cv.put("top_profiles", r.topProfiles);
        cv.put("top_miss_reasons", r.topMissReasons);
        cv.put("top_hit_reasons", r.topHitReasons);

        cv.put("runtime_profile_count", countTable(TABLE_RUNTIME_PROFILE_CACHE, null, null));
        cv.put("state_number_profile_count", countTable(TABLE_STATE_NUMBER_PROFILE, null, null));
        cv.put("learning_case_count", countTable(TABLE_LEARNING_CASE_LOG, null, null));

        cv.put("teacher_matrix_sample_count", countTable(TABLE_TEACHER_MATRIX_SAMPLE, null, null));
        cv.put(
                "teacher_matrix_profile_count",
                countTable(TABLE_TEACHER_MATRIX_PROFILE, null, null));
        cv.put("teacher_matrix_avg_hit_rate", calcTeacherMatrixAvg("hit_rate"));
        cv.put("teacher_matrix_avg_confidence", calcTeacherMatrixAvg("confidence"));
        cv.put("teacher_matrix_summary", getState("teacher_matrix_summary", ""));

        return cv;
    }

    public Models.BacktestReport getLatestBacktestReport() {
        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    DbHelper.TABLE_BACKTEST,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    "created_at DESC, _id DESC",
                                    "1");

            if (c.moveToFirst()) {
                return readBacktestReport(c);
            }
        } finally {
            closeCursor(c);
        }

        return null;
    }

    public List<Models.BacktestReport> getRecentBacktestReports(int limit) {
        ArrayList<Models.BacktestReport> list = new ArrayList<Models.BacktestReport>();

        if (limit <= 0) {
            limit = 10;
        }

        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    DbHelper.TABLE_BACKTEST,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    "created_at DESC, _id DESC",
                                    String.valueOf(limit));

            while (c.moveToNext()) {
                list.add(readBacktestReport(c));
            }
        } finally {
            closeCursor(c);
        }

        return list;
    }

    public Models.RuntimeStats getRuntimeStats() {
        Models.RuntimeStats stats = new Models.RuntimeStats();

        Cursor c = null;

        try {
            c =
                    readable()
                            .rawQuery(
                                    "SELECT COUNT(*) AS bet_count, SUM(hit) AS hit_count, SUM(pnl)"
                                        + " AS pnl FROM "
                                            + DbHelper.TABLE_TRADES
                                            + " WHERE settled=1 AND action=? AND stat_scope=? AND"
                                            + " result IN (?,?)",
                                    new String[] {
                                        AppConfig.DECISION_BET,
                                        AppConfig.STAT_SCOPE_REAL_TRADE,
                                        AppConfig.RESULT_WIN,
                                        AppConfig.RESULT_MISS
                                    });

            if (c.moveToFirst()) {
                stats.betCount = getInt(c, "bet_count");
                stats.hitCount = getInt(c, "hit_count");
                stats.pnl = getDouble(c, "pnl");
            }
        } finally {
            closeCursor(c);
        }

        try {
            c =
                    readable()
                            .rawQuery(
                                    "SELECT COUNT(1),"
                                            + "COALESCE(SUM(CASE WHEN settled=1 THEN 1 ELSE 0 END),0),"
                                            + "COALESCE(SUM(CASE WHEN settled=1 AND hit_label=1 THEN 1 ELSE 0 END),0)"
                                            + " FROM "
                                            + DbHelper.TABLE_R35_SHADOW_OBSERVATION,
                                    null);
            if (c.moveToFirst()) {
                stats.shadowObservationCount = c.getInt(0);
                stats.shadowSettledCount = c.getInt(1);
                stats.shadowHitCount = c.getInt(2);
            }
        } finally {
            closeCursor(c);
            c = null;
        }

        stats.holdCount = countDecisionByType(AppConfig.DECISION_HOLD);
        stats.shadowCount = countDecisionByType(AppConfig.DECISION_SHADOW);

        stats.freq100 = calcFrequency(AppConfig.FREQ_WINDOW_SHORT);
        stats.freq500 = calcFrequency(AppConfig.FREQ_WINDOW_MID);
        stats.freq1000 = calcFrequency(AppConfig.FREQ_WINDOW_LONG);
        stats.frequency = stats.freq100;
        stats.maxDrawdown = calcMaxDrawdown();
        stats.loseStreak = calcLoseStreak();

        stats.openTrades =
                countTable(
                        DbHelper.TABLE_TRADES,
                        "settled=0 AND action=? AND stat_scope=?",
                        new String[] {AppConfig.DECISION_BET, AppConfig.STAT_SCOPE_REAL_TRADE});

        stats.lastBacktestSummary =
                firstNonEmpty(
                        getState("backtest_last_summary", ""), getState("backtest_reason", ""));
        stats.lastBacktestMode = getState("backtest_mode", "");
        stats.learningSummary =
                firstNonEmpty(getState("auto_learn_summary", ""), getState("learning_summary", ""));
        stats.autoLearnSummary = getState("auto_learn_summary", "");
        stats.runtimeProfileSummary = uiStateReadR945.get()==null ? buildProfileCacheSummary() : getState("runtime_profile_summary", "");
        stats.biasSummary = getState("bias_summary", "");
        stats.auditSummary = getState("audit_summary", "");
        stats.promotionSummary = getState("promotion_summary", "");
        stats.aPlusSummary = getState("a_plus_summary", "");
        stats.aiJudgeSummary = getState("ai_judge_summary", "");
        stats.selfDiagnosisSummary = getState("self_diagnosis_summary", "");
        stats.lastSnapshotId = getState("last_snapshot_id", "");

        setOptionalStringField(
                stats, "teacherMatrixSummary", getState("teacher_matrix_summary", ""));
        setOptionalIntField(stats, "teacherMatrixSampleCount", countTeacherMatrixSamples());
        setOptionalIntField(stats, "teacherMatrixProfileCount", countTeacherMatrixProfiles());
        setOptionalIntField(stats, "teacherMatrixStrongCount", countTeacherMatrixStrongProfiles());
        setOptionalIntField(stats, "teacherMatrixRiskCount", countTeacherMatrixRiskProfiles());

        Models.BacktestReport latest = getLatestBacktestReport();

        if (latest != null) {
            stats.lastBacktestMode = latest.mode;
            stats.lastBacktestSummary = latest.summary;
            stats.lastBacktestWinRate = latest.winRate;
            stats.lastBacktestFrequency = latest.frequency;
            stats.lastBacktestPnl = latest.pnl;
            stats.lastBacktestMaxDrawdown = latest.maxDrawdown;
        }

        stats.updatedAt = System.currentTimeMillis();
        stats.recalc();

        return stats;
    }

    public Models.RuntimeStats loadLatestRuntimeStats() {
        return getRuntimeStats();
    }

    private String buildProfileCacheSummary() {
        int a = countTable(TABLE_RUNTIME_PROFILE_CACHE, null, null);
        int b = countTable(TABLE_STATE_NUMBER_PROFILE, null, null);
        int c = countTable(TABLE_LEARNING_CASE_LOG, null, null);
        int d = countTable(TABLE_TEACHER_MATRIX_SAMPLE, null, null);
        int e = countTable(TABLE_TEACHER_MATRIX_PROFILE, null, null);

        return "画像缓存：组合="
                + a
                + "｜号码="
                + b
                + "｜学习样本="
                + c
                + "｜教师样本="
                + d
                + "｜教师画像="
                + e
                + "｜说明=缓存只辅助解释、反推和主链读取字段，不在DbStore直接救援BET";
    }

    public void setState(final String key, final String value) {
        if (key == null || key.length() == 0) return;
        coordinator.runWrite(
                "R914_TELEMETRY_STATE",
                new DatabaseCoordinator.WriteRunnable() {
                    @Override public void run(SQLiteDatabase db) {
                        ContentValues cv = new ContentValues();
                        cv.put("state_key", key);
                        cv.put("state_value", value == null ? "" : value);
                        cv.put("updated_at", System.currentTimeMillis());
                        db.insertWithOnConflict(
                                DbHelper.TABLE_APP_STATE, null, cv,
                                SQLiteDatabase.CONFLICT_REPLACE);
                    }
                });
    }

    private final ThreadLocal<Map<String,String>> uiStateReadR945 = new ThreadLocal<Map<String,String>>();

    /** UI-only read scope: reuse the already bounded snapshot instead of dozens of round trips. */
    public Models.AppRuntimeState getUiRuntimeStateR945(Map<String,String> snapshot) {
        Map<String,String> previous=uiStateReadR945.get();
        uiStateReadR945.set(snapshot);
        try { return getRuntimeState(); }
        finally { if(previous==null)uiStateReadR945.remove();else uiStateReadR945.set(previous); }
    }

    public String getState(String key, String fallback) {
        Map<String,String> snapshotR945=uiStateReadR945.get();
        if(snapshotR945!=null&&snapshotR945.containsKey(key))return snapshotR945.get(key);
        if (key == null || key.length() == 0) {
            return fallback;
        }

        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    DbHelper.TABLE_APP_STATE,
                                    null,
                                    "state_key=?",
                                    new String[] {key},
                                    null,
                                    null,
                                    null,
                                    "1");

            if (c.moveToFirst()) {
                return getString(c, "state_value");
            }
        } finally {
            closeCursor(c);
        }

        return fallback;
    }

    /**
     * Read a bounded state namespace with one indexed range query.
     *
     * <p>The returned map is detached from the cursor and safe to use as a read-only snapshot on
     * a prediction hot path.  Prefix plus U+FFFF forms an exclusive practical upper bound for the
     * app's ASCII state keys, while the primary-key index avoids a full table scan.</p>
     */
    public Map<String, String> readStatePrefixR923(String requestedPrefix, int requestedLimit) {
        LinkedHashMap<String, String> out = new LinkedHashMap<String, String>();
        String prefix = requestedPrefix == null ? "" : requestedPrefix.trim();
        if (prefix.length() == 0) return out;
        int limit = Math.max(1, Math.min(2048, requestedLimit));
        Cursor c = null;
        try {
            c = readable().rawQuery(
                    "SELECT state_key,COALESCE(state_value,'') FROM "
                            + DbHelper.TABLE_APP_STATE
                            + " WHERE state_key>=? AND state_key<? ORDER BY state_key ASC LIMIT ?",
                    new String[] {prefix, prefix + '\uffff', String.valueOf(limit)});
            while (c.moveToNext()) {
                String key = safeString(c.getString(0));
                if (key.startsWith(prefix)) out.put(key, safeString(c.getString(1)));
            }
        } catch (Throwable ignored) {
            out.clear();
        } finally {
            closeCursor(c);
        }
        return out;
    }

    /**
     * One bounded cursor for the UI's read-only state snapshot. R815 issued hundreds of individual
     * SELECTs every light refresh, contending with canonical/settlement writes and allocating a
     * cursor/string graph per key. Values are display summaries and are capped here; complete long
     * reports remain in {@link LongTextStoreR815} and are read only on explicit user action.
     */
    public boolean readUiStateSnapshotR816(
            HashMap<String, String> destination, int requestedEntries, int requestedValueChars) {
        if (destination == null) return false;
        int entries = Math.max(64, Math.min(2048, requestedEntries));
        int valueChars = Math.max(256, Math.min(4096, requestedValueChars));
        Cursor c = null;
        try {
            c =
                    readable()
                            .rawQuery(
                                    "SELECT state_key,substr(COALESCE(state_value,''),1,?) FROM "
                                            + DbHelper.TABLE_APP_STATE
                                            + " ORDER BY updated_at DESC LIMIT ?",
                                    new String[] {
                                        String.valueOf(valueChars), String.valueOf(entries)
                                    });
            while (c.moveToNext()) {
                String key = safeString(c.getString(0));
                if (key.length() > 0) destination.put(key, safeString(c.getString(1)));
            }
            return true;
        } catch (Throwable error) {
            return false;
        } finally {
            closeCursor(c);
        }
    }

    /** Canonical R811 truth accessors; callers must not reimplement health formulas. */
    public RuntimeHealthLedgerR36.BusinessTruth businessTruthR811() {
        return RuntimeHealthLedgerR36.businessTruth(this);
    }

    public DiagnosticSnapshotR811 diagnosticSnapshotR811() {
        return DiagnosticSnapshotR811.capture(this);
    }

    public boolean ensureResearchSidecarR811() {
        return coordinatorR36().ensureR811ResearchSchemaOnce();
    }

    public String getResearchStateR811(String key, String fallback) {
        String safeKey = key == null ? "" : key.trim();
        if (!safeKey.startsWith("r811_research_")) return fallback;
        return getState(safeKey, fallback);
    }

    /** Fixed indexed UI keys: unrelated research state cannot evict the current target. */
    public void readRealtimeUiStateR71(HashMap<String,String> destination) {
        String[] keys=new String[]{"a_plus_summary","ai_judge_summary","analysis_done_target_issue","analysis_execution_stage","analysis_execution_target","analysis_last_decision","analysis_last_summary","analysis_progress","analysis_status","analysis_summary","analysis_target_issue","api_line","audit_summary","auto_learn_progress","auto_learn_status","auto_learn_summary","auto_learn_task","backfill_inserted","backfill_message","backfill_status","backtest_last_summary","backtest_mode","backtest_progress","backtest_reason","backtest_status","backtest_window","bias_summary","countdown_ms","cp12_draw_retention_reason","current_issue","fetch_count","fetch_status","fetch_summary","last_decision","last_error","last_fetch_at","last_snapshot_id","learning_summary","manual_backtest_progress","manual_backtest_status","manual_backtest_summary","next_open_time","open_fetch_try","promotion_summary","r365_last_analysis_failure","r365_last_analysis_failure_target","realtime_validate_status","realtime_validate_summary","running_flag","runtime_profile_summary","scheduler_summary","self_diagnosis_summary","service_core_summary","service_last_action","service_status","target_issue","teacher_matrix_profile_count","teacher_matrix_sample_count","teacher_matrix_status","teacher_matrix_summary","teacher_snapshot_status","teacher_snapshot_summary"};
        StringBuilder sql=new StringBuilder("SELECT state_key,substr(COALESCE(state_value,''),1,1024) FROM "+DbHelper.TABLE_APP_STATE+" WHERE state_key IN (");
        for(int i=0;i<keys.length;i++) { if(i>0)sql.append(',');sql.append('?');destination.put(keys[i],""); }
        sql.append(')');
        try(Cursor c=readable().rawQuery(sql.toString(),keys)) {
            while(c.moveToNext())destination.put(c.getString(0),c.getString(1));
        }
    }

    public Models.AppRuntimeState getRuntimeState() {
        Models.AppRuntimeState s = new Models.AppRuntimeState();

        s.serviceStatus = getState("service_status", AppConfig.SERVICE_STATUS_STOPPED);
        s.runningFlag = parseInt(getState("running_flag", "0"), 0);
        s.currentIssue = getState("current_issue", "");
        s.targetIssue = getState("target_issue", "");
        s.apiLine = getState("api_line", AppConfig.API_LINE_PRIMARY);
        s.countdownMs = parseLong(getState("countdown_ms", "0"), 0L);
        s.nextOpenTime = parseLong(getState("next_open_time", "0"), 0L);
        s.lastDecision = getState("last_decision", AppConfig.DECISION_HOLD);
        s.lastError = getState("last_error", "");

        s.backtestStatus = getState("backtest_status", AppConfig.BACKTEST_STATUS_IDLE);
        s.backtestMode = getState("backtest_mode", "");
        s.backtestProgress = parseInt(getState("backtest_progress", "0"), 0);
        s.backtestRunning =
                "RUNNING".equals(s.backtestStatus)
                                || AppConfig.BACKTEST_STATUS_RUNNING.equals(s.backtestStatus)
                        ? 1
                        : 0;
        s.backtestSummary =
                firstNonEmpty(
                        getState("backtest_last_summary", ""), getState("backtest_reason", ""));

        s.manualBacktestStatus = getState("manual_backtest_status", AppConfig.BACKTEST_STATUS_IDLE);
        s.manualBacktestProgress = parseInt(getState("manual_backtest_progress", "0"), 0);
        s.manualBacktestRunning =
                "RUNNING".equals(s.manualBacktestStatus)
                                || AppConfig.BACKTEST_STATUS_RUNNING.equals(s.manualBacktestStatus)
                        ? 1
                        : 0;
        s.manualBacktestSummary = getState("manual_backtest_summary", "");

        s.learningSummary =
                firstNonEmpty(getState("auto_learn_summary", ""), getState("learning_summary", ""));
        s.biasSummary = getState("bias_summary", "");
        s.auditSummary = getState("audit_summary", "");
        s.aiJudgeSummary = getState("ai_judge_summary", "");
        s.selfDiagnosisSummary = getState("self_diagnosis_summary", "");
        s.promotionSummary = getState("promotion_summary", "");
        s.aPlusSummary = getState("a_plus_summary", "");
        s.schedulerSummary = buildRuntimeSchedulerSummary();
        s.lastSnapshotId = getState("last_snapshot_id", "");

        s.autoLearnStatus = getState("auto_learn_status", AUTO_LEARN_STATUS_IDLE);
        s.autoLearnRunning = AUTO_LEARN_STATUS_RUNNING.equals(s.autoLearnStatus) ? 1 : 0;
        s.autoLearnProgress = parseInt(getState("auto_learn_progress", "0"), 0);
        s.autoLearnTaskKey = getState("auto_learn_task", "");
        s.autoLearnSummary = getState("auto_learn_summary", "");
        s.runtimeProfileSummary = uiStateReadR945.get()==null ? buildProfileCacheSummary() : getState("runtime_profile_summary", "");

        setOptionalStringField(s, "teacherMatrixSummary", getState("teacher_matrix_summary", ""));
        setOptionalIntField(s, "teacherMatrixSampleCount", uiStateReadR945.get()==null ? countTeacherMatrixSamples() : parseInt(getState("teacher_matrix_sample_count","0"),0));
        setOptionalIntField(s, "teacherMatrixProfileCount", uiStateReadR945.get()==null ? countTeacherMatrixProfiles() : parseInt(getState("teacher_matrix_profile_count","0"),0));

        s.updatedAt = System.currentTimeMillis();

        return s;
    }

    private String buildRuntimeSchedulerSummary() {
        String base = getState("scheduler_summary", "");
        String analysisStatus = getState("analysis_status", "");
        String analysisProgress = getState("analysis_progress", "0");
        String analysisSummary = getState("analysis_summary", "");
        String backtestStatus = getState("backtest_status", "");
        String backtestProgress = getState("backtest_progress", "0");
        String backtestWindow = getState("backtest_window", "0");
        String teacherStatus = getState("teacher_snapshot_status", "");
        String teacherSummary = getState("teacher_snapshot_summary", "");
        String teacherMatrixStatus = getState("teacher_matrix_status", "");
        String teacherMatrixSummary = getState("teacher_matrix_summary", "");
        String validateStatus = getState("realtime_validate_status", "");
        String validateSummary = getState("realtime_validate_summary", "");
        String autoLearnStatus = getState("auto_learn_status", "");
        String autoLearnSummary = getState("auto_learn_summary", "");

        StringBuilder sb = new StringBuilder();

        if (base != null && base.length() > 0) {
            sb.append(base);
        }

        if (analysisStatus != null && analysisStatus.length() > 0) {
            appendSep(sb);
            sb.append("实时分析=");
            sb.append(analysisStatus);
            sb.append(" ");
            sb.append(analysisProgress);
            sb.append("%");

            if (analysisSummary != null && analysisSummary.length() > 0) {
                sb.append(" ");
                sb.append(analysisSummary);
            }
        }

        if (autoLearnStatus != null && autoLearnStatus.length() > 0) {
            appendSep(sb);
            sb.append("后台学习=");
            sb.append(autoLearnStatus);

            if (autoLearnSummary != null && autoLearnSummary.length() > 0) {
                sb.append(" ");
                sb.append(autoLearnSummary);
            }
        }

        if (teacherMatrixStatus != null && teacherMatrixStatus.length() > 0) {
            appendSep(sb);
            sb.append("教师矩阵=");
            sb.append(teacherMatrixStatus);

            if (teacherMatrixSummary != null && teacherMatrixSummary.length() > 0) {
                sb.append(" ");
                sb.append(teacherMatrixSummary);
            }
        }

        if (backtestStatus != null && backtestStatus.length() > 0) {
            appendSep(sb);
            sb.append("回测=");
            sb.append(backtestStatus);
            sb.append(" ");
            sb.append(backtestProgress);
            sb.append("%");

            if (backtestWindow != null
                    && backtestWindow.length() > 0
                    && !"0".equals(backtestWindow)) {
                sb.append(" W");
                sb.append(backtestWindow);
            }
        }

        if (teacherStatus != null && teacherStatus.length() > 0) {
            appendSep(sb);
            sb.append("教师快照=");
            sb.append(teacherStatus);

            if (teacherSummary != null && teacherSummary.length() > 0) {
                sb.append(" ");
                sb.append(teacherSummary);
            }
        }

        if (validateStatus != null && validateStatus.length() > 0) {
            appendSep(sb);
            sb.append("轻量验证=");
            sb.append(validateStatus);

            if (validateSummary != null && validateSummary.length() > 0) {
                sb.append(" ");
                sb.append(validateSummary);
            }
        }

        return sb.toString();
    }

    public void updateRuntimeState(Models.AppRuntimeState state) {
        if (state == null) {
            return;
        }

        Map<String,String> values = runtimeStateValuesR77(
                state.serviceStatus,
                state.currentIssue,
                state.targetIssue,
                state.apiLine,
                state.countdownMs,
                state.nextOpenTime,
                state.lastDecision,
                state.lastError);

        values.put("backtest_status", state.backtestStatus);
        values.put("backtest_mode", state.backtestMode);
        values.put("backtest_progress", String.valueOf(state.backtestProgress));

        if (state.backtestSummary != null && state.backtestSummary.length() > 0) {
            values.put("backtest_last_summary", state.backtestSummary);
            values.put("backtest_reason", state.backtestSummary);
        }

        values.put("manual_backtest_status", state.manualBacktestStatus);
        values.put("manual_backtest_progress", String.valueOf(state.manualBacktestProgress));

        if (state.manualBacktestSummary != null && state.manualBacktestSummary.length() > 0) {
            values.put("manual_backtest_summary", state.manualBacktestSummary);
        }

        values.put("learning_summary", state.learningSummary);
        values.put("bias_summary", state.biasSummary);
        values.put("audit_summary", state.auditSummary);
        values.put("ai_judge_summary", state.aiJudgeSummary);
        values.put("self_diagnosis_summary", state.selfDiagnosisSummary);
        values.put("promotion_summary", state.promotionSummary);
        values.put("a_plus_summary", state.aPlusSummary);
        values.put("scheduler_summary", state.schedulerSummary);
        values.put("last_snapshot_id", state.lastSnapshotId);
        values.put("auto_learn_status", state.autoLearnStatus);
        values.put("auto_learn_task", state.autoLearnTaskKey);
        values.put("auto_learn_summary", state.autoLearnSummary);
        values.put("runtime_profile_summary", state.runtimeProfileSummary);

        values.put(
                "teacher_matrix_summary",
                getOptionalStringField(
                        state, "teacherMatrixSummary", getState("teacher_matrix_summary", "")));
        // One writer admission and one SQLite transaction for this observation.
        // Previously every field independently waited for the writer and committed.
        setStates(values);
    }

    public void updateRuntimeState(
            String status,
            String currentIssue,
            String targetIssue,
            String apiLine,
            long countdownMs,
            long nextOpenTime,
            String lastDecision,
            String lastError) {
        setStates(runtimeStateValuesR77(status,currentIssue,targetIssue,apiLine,
                countdownMs,nextOpenTime,lastDecision,lastError));
    }

    private static Map<String,String> runtimeStateValuesR77(
            String status,String currentIssue,String targetIssue,String apiLine,
            long countdownMs,long nextOpenTime,String lastDecision,String lastError) {
        Map<String,String> values = new LinkedHashMap<String,String>();
        values.put("service_status", status == null ? "" : status);
        values.put("running_flag", AppConfig.SERVICE_STATUS_STOPPED.equals(status) ? "0" : "1");
        values.put("current_issue", currentIssue == null ? "" : currentIssue);
        values.put("target_issue", targetIssue == null ? "" : targetIssue);
        values.put("api_line", apiLine == null ? "" : apiLine);
        values.put("countdown_ms", String.valueOf(countdownMs));
        values.put("next_open_time", String.valueOf(nextOpenTime));
        values.put("last_decision", lastDecision == null ? "" : lastDecision);
        values.put("last_error", lastError == null ? "" : lastError);
        return values;
    }

    public void updateDeepRuntimeSummaries(
            String learning,
            String bias,
            String audit,
            String aiJudge,
            String promotion,
            String aPlus,
            String diagnosis) {

        setState("learning_summary", learning == null ? "" : learning);
        setState("bias_summary", bias == null ? "" : bias);
        setState("audit_summary", audit == null ? "" : audit);
        setState("ai_judge_summary", aiJudge == null ? "" : aiJudge);
        setState("promotion_summary", promotion == null ? "" : promotion);
        setState("a_plus_summary", aPlus == null ? "" : aPlus);
        setState("self_diagnosis_summary", diagnosis == null ? "" : diagnosis);
    }

    public long savePointInTimeSnapshot(Models.PointInTimeSnapshot s) {
        if (s == null || s.snapshotId == null || s.snapshotId.length() == 0) {
            return -1L;
        }

        if (s.createdAt <= 0L) {
            s.createdAt = System.currentTimeMillis();
        }

        ContentValues cv = new ContentValues();

        cv.put("snapshot_id", s.snapshotId);
        cv.put("base_issue", s.baseIssue);
        cv.put("target_issue", s.targetIssue);
        cv.put("train_start_issue", s.trainStartIssue);
        cv.put("train_end_issue", s.trainEndIssue);
        cv.put("visible_draw_count", s.visibleDrawCount);
        cv.put("visible_trade_count", s.visibleTradeCount);
        cv.put("visible_profile_version", s.visibleProfileVersion);
        cv.put("visible_calibration_version", s.visibleCalibrationVersion);
        cv.put("feature_summary", s.featureSummary);
        cv.put("candidate_summary", s.candidateSummary);
        cv.put("gate_summary", s.gateSummary);
        cv.put("decision_summary", s.decisionSummary);
        cv.put("data_hash", s.dataHash);
        cv.put("no_future_leak_ok", s.noFutureLeakOk);
        cv.put("status", s.status);
        cv.put("warning_text", s.warningText);
        cv.put("created_at", s.createdAt);
        cv.put("visible_teacher_matrix_version", countTeacherMatrixSamples());

        long id =
                insertFiltered(
                        writable(),
                        DbHelper.TABLE_POINT_IN_TIME_SNAPSHOT,
                        cv,
                        SQLiteDatabase.CONFLICT_REPLACE);
        setState("last_snapshot_id", s.snapshotId);

        return id;
    }

    public Models.PointInTimeSnapshot getLatestPointInTimeSnapshot() {
        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    DbHelper.TABLE_POINT_IN_TIME_SNAPSHOT,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    "created_at DESC, _id DESC",
                                    "1");

            if (c.moveToFirst()) {
                return readPointInTimeSnapshot(c);
            }
        } finally {
            closeCursor(c);
        }

        return null;
    }

    public long saveCandidateEvidence(Models.CandidateEvidence e) {
        if (e == null || e.evidenceKey == null || e.evidenceKey.length() == 0) {
            return -1L;
        }

        if (e.createdAt <= 0L) {
            e.createdAt = System.currentTimeMillis();
        }

        ContentValues cv = new ContentValues();

        cv.put("evidence_key", e.evidenceKey);
        cv.put("base_issue", e.baseIssue);
        cv.put("target_issue", e.targetIssue);
        cv.put("position", e.position);
        cv.put("n1", e.n1);
        cv.put("n2", e.n2);
        cv.put("n3", e.n3);
        cv.put("k3_mask", e.k3Mask);
        cv.put("position_evidence", e.positionEvidence);
        cv.put("number_evidence", e.numberEvidence);
        cv.put("cycle_evidence", e.cycleEvidence);
        cv.put("transition_evidence", e.transitionEvidence);
        cv.put("pattern_evidence", e.patternEvidence);
        cv.put("combo_evidence", e.comboEvidence);
        cv.put("profile_evidence", e.profileEvidence);
        cv.put("audit_evidence", e.auditEvidence);
        cv.put("risk_score", e.riskScore);
        cv.put("evidence_score", e.evidenceScore);
        cv.put("confidence", e.confidence);
        cv.put("support_text", e.supportText);
        cv.put("risk_text", e.riskText);
        cv.put("summary_text", e.summaryText);
        cv.put("created_at", e.createdAt);

        String stateKey = buildStateKey("", "", e.position, e.riskScore, 0.0d, 0.0d);

        cv.put("state_key", stateKey);
        cv.put("runtime_profile_evidence", e.profileEvidence);
        cv.put("state_number_evidence", e.numberEvidence);
        cv.put(
                "teacher_matrix_evidence",
                getTeacherMatrixComboProbAdjust(stateKey, e.position, e.k3Mask));

        return insertFiltered(
                writable(), DbHelper.TABLE_CANDIDATE_EVIDENCE, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public int saveCandidateEvidences(List<Models.CandidateEvidence> list) {
        if (list == null || list.size() == 0) {
            return 0;
        }

        int saved = 0;

        for (int i = 0; i < list.size(); i++) {
            Models.CandidateEvidence e = list.get(i);

            if (e == null) {
                continue;
            }

            if (saveCandidateEvidence(e) >= 0L) {
                saved++;
            }
        }

        return saved;
    }

    public Models.CandidateEvidence getCandidateEvidence(String evidenceKey) {
        if (evidenceKey == null || evidenceKey.length() == 0) {
            return null;
        }

        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    DbHelper.TABLE_CANDIDATE_EVIDENCE,
                                    null,
                                    "evidence_key=?",
                                    new String[] {evidenceKey},
                                    null,
                                    null,
                                    null,
                                    "1");

            if (c.moveToFirst()) {
                return readCandidateEvidence(c);
            }
        } finally {
            closeCursor(c);
        }

        return null;
    }

    public long saveAiJudgeResult(Models.AiJudgeResult r) {
        if (r == null || r.judgeId == null || r.judgeId.length() == 0) {
            return -1L;
        }

        if (r.createdAt <= 0L) {
            r.createdAt = System.currentTimeMillis();
        }

        if (r.bestCandidate != null) {
            saveCandidateEvidence(r.bestCandidate);
        }

        if (r.secondCandidate != null) {
            saveCandidateEvidence(r.secondCandidate);
        }

        ContentValues cv = new ContentValues();

        cv.put("judge_id", r.judgeId);
        cv.put("base_issue", r.baseIssue);
        cv.put("target_issue", r.targetIssue);
        cv.put("best_evidence_key", r.bestCandidate == null ? "" : r.bestCandidate.evidenceKey);
        cv.put(
                "second_evidence_key",
                r.secondCandidate == null ? "" : r.secondCandidate.evidenceKey);
        cv.put("champion_gap", r.championGap);
        cv.put("judge_score", r.judgeScore);
        cv.put("judge_confidence", r.judgeConfidence);
        cv.put("conflict_level", r.conflictLevel);
        cv.put("bet_advice", r.betAdvice);
        cv.put("hold_reason", r.holdReason);
        cv.put("summary_text", r.summaryText);
        cv.put("created_at", r.createdAt);

        long id =
                insertFiltered(
                        writable(),
                        DbHelper.TABLE_AI_JUDGE_LOG,
                        cv,
                        SQLiteDatabase.CONFLICT_REPLACE);
        setState("ai_judge_summary", r.summaryText);

        return id;
    }

    public Models.AiJudgeResult getLatestAiJudgeResult() {
        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    DbHelper.TABLE_AI_JUDGE_LOG,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    "created_at DESC, _id DESC",
                                    "1");

            if (c.moveToFirst()) {
                return readAiJudgeResult(c);
            }
        } finally {
            closeCursor(c);
        }

        return null;
    }

    public long saveBiasEvidence(Models.BiasEvidenceResult r) {
        if (r == null || r.evidenceId == null || r.evidenceId.length() == 0) {
            return -1L;
        }

        if (r.createdAt <= 0L) {
            r.createdAt = System.currentTimeMillis();
        }

        ContentValues cv = new ContentValues();

        cv.put("evidence_id", r.evidenceId);
        cv.put("issue", r.issue);
        cv.put("base_issue", r.baseIssue);
        cv.put("target_issue", r.targetIssue);
        cv.put("bias_score", r.biasScore);
        cv.put("position_bias_score", r.positionBiasScore);
        cv.put("number_bias_score", r.numberBiasScore);
        cv.put("transition_bias_score", r.transitionBiasScore);
        cv.put("cycle_bias_score", r.cycleBiasScore);
        cv.put("combo_bias_score", r.comboBiasScore);
        cv.put("state_bias_score", r.stateBiasScore);
        cv.put("sample_count", r.sampleCount);
        cv.put("recent_sample", r.recentSample);
        cv.put("confidence", r.confidence);
        cv.put("status", r.status);
        cv.put("summary_text", r.summaryText);
        cv.put("created_at", r.createdAt);

        long id =
                insertFiltered(
                        writable(),
                        DbHelper.TABLE_BIAS_EVIDENCE,
                        cv,
                        SQLiteDatabase.CONFLICT_REPLACE);
        setState("bias_summary", r.summaryText);

        return id;
    }

    public Models.BiasEvidenceResult getLatestBiasEvidence() {
        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    DbHelper.TABLE_BIAS_EVIDENCE,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    "created_at DESC, _id DESC",
                                    "1");

            if (c.moveToFirst()) {
                return readBiasEvidence(c);
            }
        } finally {
            closeCursor(c);
        }

        return null;
    }

    public long saveDeepProfile(Models.DeepProfile p) {
        if (p == null || p.profileKey == null || p.profileKey.length() == 0) {
            return -1L;
        }

        p.recalc();
        p.updatedAt = System.currentTimeMillis();

        ContentValues cv = deepProfileToValues(p);
        return insertFiltered(
                writable(), DbHelper.TABLE_DEEP_PROFILE, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private ContentValues deepProfileToValues(Models.DeepProfile p) {
        ContentValues cv = new ContentValues();

        cv.put("profile_key", p.profileKey);
        cv.put("position", p.position);
        cv.put("state_mode", p.stateMode);
        cv.put("trend_bucket", p.trendBucket);
        cv.put("drift_bucket", p.driftBucket);
        cv.put("break_bucket", p.breakBucket);
        cv.put("recovery_bucket", p.recoveryBucket);
        cv.put("cycle_bucket", p.cycleBucket);
        cv.put("relation_bucket", p.relationBucket);
        cv.put("total_sample", p.totalSample);
        cv.put("total_hit", p.totalHit);
        cv.put("total_hit_rate", p.totalHitRate);
        cv.put("recent_sample", p.recentSample);
        cv.put("recent_hit", p.recentHit);
        cv.put("recent_hit_rate", p.recentHitRate);
        cv.put("decay_weight", p.decayWeight);
        cv.put("active_flag", p.activeFlag);
        cv.put("stale_flag", p.staleFlag);
        cv.put("recover_flag", p.recoverFlag);
        cv.put("last_hit_at", p.lastHitAt);
        cv.put("last_miss_at", p.lastMissAt);
        cv.put("fail_count", p.failCount);
        cv.put("recover_count", p.recoverCount);
        cv.put("avg_final_p", p.avgFinalP);
        cv.put("avg_edge", p.avgEdge);
        cv.put("avg_deep_score", p.avgDeepScore);
        cv.put("updated_at", p.updatedAt);

        String stateKey =
                buildStateKey(
                        p.stateMode,
                        "",
                        p.position,
                        p.driftBucket,
                        p.breakBucket,
                        p.recoveryBucket);

        cv.put("state_key", stateKey);
        cv.put("runtime_profile_score", p.totalHitRate);
        cv.put("teacher_weight", p.decayWeight);
        cv.put("teacher_matrix_score", getTeacherMatrixComboProbAdjust(stateKey, p.position, 0));

        return cv;
    }

    public Models.DeepProfile getDeepProfile(String profileKey) {
        if (profileKey == null || profileKey.length() == 0) {
            return null;
        }

        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    DbHelper.TABLE_DEEP_PROFILE,
                                    null,
                                    "profile_key=?",
                                    new String[] {profileKey},
                                    null,
                                    null,
                                    null,
                                    "1");

            if (c.moveToFirst()) {
                return readDeepProfile(c);
            }
        } finally {
            closeCursor(c);
        }

        return null;
    }

    public List<Models.DeepProfile> getTopDeepProfiles(int limit) {
        ArrayList<Models.DeepProfile> list = new ArrayList<Models.DeepProfile>();

        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    DbHelper.TABLE_DEEP_PROFILE,
                                    null,
                                    "active_flag=1 AND stale_flag=0",
                                    null,
                                    null,
                                    null,
                                    "total_hit_rate DESC, recent_hit_rate DESC, total_sample DESC",
                                    limit > 0 ? String.valueOf(limit) : "20");

            while (c.moveToNext()) {
                list.add(readDeepProfile(c));
            }
        } finally {
            closeCursor(c);
        }

        return list;
    }

    public void updateDeepProfileFromSample(
            Models.LearningSample sample, double finalP, double edge, double deepScore) {
        if (sample == null
                || sample.deepProfileKey == null
                || sample.deepProfileKey.length() == 0) {
            return;
        }

        Models.DeepProfile p = getDeepProfile(sample.deepProfileKey);

        if (p == null) {
            p = new Models.DeepProfile();
            p.profileKey = sample.deepProfileKey;
            p.position = sample.position;
            p.stateMode = sample.stateMode;
        }

        long now = System.currentTimeMillis();

        p.totalSample++;
        p.recentSample++;

        if (sample.hit == 1) {
            p.totalHit++;
            p.recentHit++;
            p.lastHitAt = now;
            p.recoverCount++;
            p.failCount = 0;
        } else {
            p.lastMissAt = now;
            p.failCount++;
        }

        p.avgFinalP = rollingAverage(p.avgFinalP, p.totalSample, finalP);
        p.avgEdge = rollingAverage(p.avgEdge, p.totalSample, edge);
        p.avgDeepScore = rollingAverage(p.avgDeepScore, p.totalSample, deepScore);

        if (p.failCount >= 4) {
            p.staleFlag = 1;
            p.activeFlag = 0;
        }

        p.recalc();
        saveDeepProfile(p);
    }

    public long saveAttributionResult(Models.AttributionResult r) {
        if (r == null || r.attributionId == null || r.attributionId.length() == 0) {
            return -1L;
        }

        if (r.createdAt <= 0L) {
            r.createdAt = System.currentTimeMillis();
        }

        ContentValues cv = new ContentValues();

        cv.put("attribution_id", r.attributionId);
        cv.put("target_issue", r.targetIssue);
        cv.put("hit", r.hit);
        cv.put("main_reason_code", r.mainReasonCode);
        cv.put("main_reason_text", r.mainReasonText);
        cv.put("confidence", r.confidence);
        cv.put("should_reward", r.shouldReward);
        cv.put("should_punish", r.shouldPunish);
        cv.put("affected_module", r.affectedModule);
        cv.put("profile_key", r.profileKey);
        cv.put("position_impact", r.positionImpact);
        cv.put("number_impact", r.numberImpact);
        cv.put("cycle_impact", r.cycleImpact);
        cv.put("transition_impact", r.transitionImpact);
        cv.put("calibration_impact", r.calibrationImpact);
        cv.put("teacher_matrix_impact", getOptionalDoubleField(r, "teacherMatrixImpact", 0.0d));
        cv.put("learning_text", r.learningText);
        cv.put("created_at", r.createdAt);

        long id =
                insertFiltered(
                        writable(),
                        DbHelper.TABLE_DEEP_ATTRIBUTION,
                        cv,
                        SQLiteDatabase.CONFLICT_REPLACE);
        setState("last_attribution", r.mainReasonText);

        return id;
    }

    public List<Models.AttributionResult> getRecentAttributions(int limit) {
        ArrayList<Models.AttributionResult> list = new ArrayList<Models.AttributionResult>();

        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    DbHelper.TABLE_DEEP_ATTRIBUTION,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    "created_at DESC, _id DESC",
                                    limit > 0 ? String.valueOf(limit) : "20");

            while (c.moveToNext()) {
                list.add(readAttributionResult(c));
            }
        } finally {
            closeCursor(c);
        }

        return list;
    }

    public long savePromotionCandidate(Models.PromotionCandidate p) {
        if (p == null || p.promotionKey == null || p.promotionKey.length() == 0) {
            return -1L;
        }

        p.recalc();
        p.updatedAt = System.currentTimeMillis();

        ContentValues cv = new ContentValues();

        cv.put("promotion_key", p.promotionKey);
        cv.put("profile_key", p.profileKey);
        cv.put("status", p.status);
        cv.put("sample_count", p.sampleCount);
        cv.put("hit_count", p.hitCount);
        cv.put("hit_rate", p.hitRate);
        cv.put("recent_sample", p.recentSample);
        cv.put("recent_hit", p.recentHit);
        cv.put("recent_hit_rate", p.recentHitRate);
        cv.put("calibration_error", p.calibrationError);
        cv.put("bias_score", p.biasScore);
        cv.put("a_plus_score", p.aPlusScore);
        cv.put("teacher_matrix_score", getOptionalDoubleField(p, "teacherMatrixScore", 0.0d));
        cv.put("promote_reason", p.promoteReason);
        cv.put("reject_reason", p.rejectReason);
        cv.put("updated_at", p.updatedAt);

        long id =
                insertFiltered(
                        writable(),
                        DbHelper.TABLE_PROMOTION_CANDIDATE,
                        cv,
                        SQLiteDatabase.CONFLICT_REPLACE);
        setState("promotion_summary", p.promoteReason);

        return id;
    }

    public Models.PromotionCandidate getPromotionCandidate(String promotionKey) {
        if (promotionKey == null || promotionKey.length() == 0) {
            return null;
        }

        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    DbHelper.TABLE_PROMOTION_CANDIDATE,
                                    null,
                                    "promotion_key=?",
                                    new String[] {promotionKey},
                                    null,
                                    null,
                                    null,
                                    "1");

            if (c.moveToFirst()) {
                return readPromotionCandidate(c);
            }
        } finally {
            closeCursor(c);
        }

        return null;
    }

    public long saveManualBacktestTask(Models.ManualBacktestTask t) {
        if (t == null || t.taskId == null || t.taskId.length() == 0) {
            return -1L;
        }

        long now = System.currentTimeMillis();

        if (t.startedAt <= 0L
                && ("RUNNING".equals(t.status)
                        || AppConfig.BACKTEST_STATUS_RUNNING.equals(t.status))) {
            t.startedAt = now;
        }

        ContentValues cv = new ContentValues();

        cv.put("task_id", t.taskId);
        cv.put("mode", t.mode);
        cv.put("status", t.status);
        cv.put("progress", t.progress);
        cv.put("window_size", t.windowSize);
        cv.put("start_index", t.startIndex);
        cv.put("end_index", t.endIndex);
        cv.put("min_train", t.minTrain);
        cv.put("max_test", t.maxTest);
        cv.put("top_keep", t.topKeep);
        cv.put("total_steps", t.totalSteps);
        cv.put("done_steps", t.doneSteps);
        cv.put("started_at", t.startedAt);
        cv.put("updated_at", now);
        cv.put("finished_at", t.finishedAt);
        cv.put("summary", t.summary);
        cv.put("error", t.error);

        SQLiteDatabase db = writable();
        long id = -1L;

        db.beginTransaction();

        try {
            long oldId = getManualTaskRowId(db, t.taskId);

            if (oldId > 0L) {
                updateFiltered(
                        db,
                        DbHelper.TABLE_MANUAL_BACKTEST_TASK,
                        cv,
                        "_id=?",
                        new String[] {String.valueOf(oldId)});

                id = oldId;
            } else {
                id =
                        insertFiltered(
                                db,
                                DbHelper.TABLE_MANUAL_BACKTEST_TASK,
                                cv,
                                SQLiteDatabase.CONFLICT_NONE);
            }

            cleanupOldManualRunningTasks(db, t, id, now);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        setState("manual_backtest_status", t.status);
        setState("manual_backtest_progress", String.valueOf(t.progress));
        setState("manual_backtest_summary", t.summary == null ? "" : t.summary);

        return id;
    }

    private long getManualTaskRowId(SQLiteDatabase db, String taskId) {
        if (db == null || taskId == null || taskId.length() == 0) {
            return 0L;
        }

        Cursor c = null;

        try {
            c =
                    db.query(
                            DbHelper.TABLE_MANUAL_BACKTEST_TASK,
                            new String[] {"_id"},
                            "task_id=?",
                            new String[] {taskId},
                            null,
                            null,
                            "updated_at DESC, _id DESC",
                            "1");

            if (c.moveToFirst()) {
                return getLong(c, "_id");
            }
        } finally {
            closeCursor(c);
        }

        return 0L;
    }

    private void cleanupOldManualRunningTasks(
            SQLiteDatabase db, Models.ManualBacktestTask t, long keepId, long now) {
        if (db == null || t == null || keepId <= 0L) {
            return;
        }

        try {
            ContentValues cv = new ContentValues();

            cv.put("status", "REPLACED");
            cv.put("finished_at", now);
            cv.put("updated_at", now);

            if (t.summary != null && t.summary.length() > 0) {
                cv.put("summary", "新回测进度已接管：" + t.summary);
            } else {
                cv.put("summary", "新回测进度已接管");
            }

            updateFiltered(
                    db,
                    DbHelper.TABLE_MANUAL_BACKTEST_TASK,
                    cv,
                    "_id<>? AND status=?",
                    new String[] {String.valueOf(keepId), "RUNNING"});

            updateFiltered(
                    db,
                    DbHelper.TABLE_MANUAL_BACKTEST_TASK,
                    cv,
                    "_id<>? AND status=?",
                    new String[] {String.valueOf(keepId), AppConfig.BACKTEST_STATUS_RUNNING});
        } catch (Throwable e) {
        }
    }

    public Models.ManualBacktestTask getLatestManualBacktestTask() {
        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    DbHelper.TABLE_MANUAL_BACKTEST_TASK,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    "CASE WHEN status='RUNNING' THEN 0 WHEN status='"
                                            + AppConfig.BACKTEST_STATUS_RUNNING
                                            + "' THEN 0 ELSE 1 END ASC, updated_at DESC, _id DESC",
                                    "1");

            if (c.moveToFirst()) {
                return readManualBacktestTask(c);
            }
        } finally {
            closeCursor(c);
        }

        return null;
    }

    public void updateProbabilityAudit(double predictedP, boolean hit) {
        predictedP = AppConfig.clampProbability(predictedP);

        String key = auditBucketKey(predictedP);
        long now = System.currentTimeMillis();

        SQLiteDatabase db = writable();
        ensureAuditBucket(db, key, predictedP);

        db.execSQL(
                "UPDATE "
                        + DbHelper.TABLE_PROBABILITY_AUDIT_BUCKET
                        + " SET "
                        + "sample_count=sample_count+1,"
                        + "hit_count=hit_count+?,"
                        + "avg_predicted_p=((avg_predicted_p*sample_count)+?)/(sample_count+1),"
                        + "brier_score=((brier_score*sample_count)+?)/(sample_count+1),"
                        + "recent_sample=recent_sample+1,"
                        + "recent_hit=recent_hit+?,"
                        + "updated_at=? "
                        + "WHERE bucket_key=?",
                new Object[] {
                    Integer.valueOf(hit ? 1 : 0),
                    Double.valueOf(predictedP),
                    Double.valueOf(brier(predictedP, hit)),
                    Integer.valueOf(hit ? 1 : 0),
                    Long.valueOf(now),
                    key
                });

        recalcProbabilityAuditBucket(db, key);
    }

    private void ensureAuditBucket(SQLiteDatabase db, String key, double p) {
        if (db == null || key == null || key.length() == 0) {
            return;
        }

        double low = auditBucketLow(key, p);
        double high = auditBucketHigh(key, p);

        ContentValues cv = new ContentValues();

        cv.put("bucket_key", key);
        cv.put("bucket_low", low);
        cv.put("bucket_high", high);
        cv.put("sample_count", 0);
        cv.put("hit_count", 0);
        cv.put("hit_rate", 0.0d);
        cv.put("avg_predicted_p", 0.0d);
        cv.put("calibration_error", 0.0d);
        cv.put("brier_score", 0.0d);
        cv.put("recent_sample", 0);
        cv.put("recent_hit", 0);
        cv.put("recent_hit_rate", 0.0d);
        cv.put("recent_error", 0.0d);
        cv.put("status", AppConfig.PROB_BUCKET_SAMPLE_WEAK);
        cv.put("updated_at", System.currentTimeMillis());

        insertFiltered(
                db, DbHelper.TABLE_PROBABILITY_AUDIT_BUCKET, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private void recalcProbabilityAuditBucket(SQLiteDatabase db, String key) {
        Models.ProbabilityAuditBucket b = getProbabilityAuditBucket(key);

        if (b == null) {
            return;
        }

        b.recalc();

        ContentValues cv = new ContentValues();

        cv.put("hit_rate", b.hitRate);
        cv.put("calibration_error", b.calibrationError);
        cv.put("recent_hit_rate", b.recentHitRate);
        cv.put("recent_error", b.recentError);
        cv.put("status", b.status);
        cv.put("updated_at", System.currentTimeMillis());

        updateFiltered(
                db,
                DbHelper.TABLE_PROBABILITY_AUDIT_BUCKET,
                cv,
                "bucket_key=?",
                new String[] {key});
        setState(
                "audit_summary",
                "概率审计 " + key + "｜状态=" + b.status + "｜误差=" + percent(b.calibrationError));
    }

    public Models.ProbabilityAuditBucket getProbabilityAuditBucket(String key) {
        if (key == null || key.length() == 0) {
            return null;
        }

        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    DbHelper.TABLE_PROBABILITY_AUDIT_BUCKET,
                                    null,
                                    "bucket_key=?",
                                    new String[] {key},
                                    null,
                                    null,
                                    null,
                                    "1");

            if (c.moveToFirst()) {
                return readProbabilityAuditBucket(c);
            }
        } finally {
            closeCursor(c);
        }

        return null;
    }

    public Models.ProbabilityAuditBucket getProbabilityAuditBucketForP(double p) {
        return getProbabilityAuditBucket(auditBucketKey(p));
    }

    public long saveLearningSnapshot(Models.RuntimeLearningSnapshot s) {
        if (s == null || s.snapshotId == null || s.snapshotId.length() == 0) {
            return -1L;
        }

        if (s.createdAt <= 0L) {
            s.createdAt = System.currentTimeMillis();
        }

        ContentValues cv = new ContentValues();

        cv.put("snapshot_id", s.snapshotId);
        cv.put("created_at", s.createdAt);
        cv.put("source_type", s.sourceType);
        cv.put("target_issue", s.targetIssue);
        cv.put("profile_key", s.profileKey);
        cv.put("hit", s.hit);
        cv.put("affected_module", s.affectedModule);
        cv.put("module_action", s.moduleAction);
        cv.put("module_weight_delta", s.moduleWeightDelta);
        cv.put("confidence", s.confidence);
        cv.put("main_reason_code", s.mainReasonCode);
        cv.put("summary_text", s.summaryText);
        cv.put("total_sample", s.totalSample);
        cv.put("total_hit_rate", s.totalHitRate);
        cv.put("recent_sample", s.recentSample);
        cv.put("recent_hit_rate", s.recentHitRate);
        cv.put("decay_weight", s.decayWeight);
        cv.put("fail_count", s.failCount);
        cv.put("stale_flag", s.staleFlag);
        cv.put("recover_flag", s.recoverFlag);
        cv.put("profile_summary", s.profileSummary);
        cv.put("gate_adjust_summary", s.gateAdjustSummary);
        cv.put("probability_adjust_summary", s.probabilityAdjustSummary);
        cv.put("audit_summary", s.auditSummary);
        cv.put("bias_summary", s.biasSummary);
        cv.put("promotion_summary", s.promotionSummary);
        cv.put("a_plus_summary", s.aPlusSummary);
        cv.put("active_json", s.activeJson);
        cv.put("learning_summary_text", s.learningSummaryText);

        cv.put("teacher_matrix_sample_count", countTeacherMatrixSamples());
        cv.put("teacher_matrix_hit_rate", calcTeacherMatrixAvg("hit_rate"));
        cv.put("teacher_matrix_confidence", calcTeacherMatrixAvg("confidence"));
        cv.put("teacher_matrix_summary", getState("teacher_matrix_summary", ""));

        long id =
                insertFiltered(
                        writable(),
                        DbHelper.TABLE_LEARNING_SNAPSHOT,
                        cv,
                        SQLiteDatabase.CONFLICT_REPLACE);

        setState("learning_summary", s.learningSummaryText);
        setState("audit_summary", s.auditSummary);
        setState("bias_summary", s.biasSummary);
        setState("promotion_summary", s.promotionSummary);
        setState("a_plus_summary", s.aPlusSummary);

        return id;
    }

    public long saveSelfDiagnosisResult(Models.SelfDiagnosisResult r) {
        if (r == null || r.diagnosisId == null || r.diagnosisId.length() == 0) {
            return -1L;
        }

        if (r.createdAt <= 0L) {
            r.createdAt = System.currentTimeMillis();
        }

        ContentValues cv = new ContentValues();

        cv.put("diagnosis_id", r.diagnosisId);
        cv.put("level", r.level);
        cv.put("main_code", r.mainCode);
        cv.put("main_reason_code", r.mainReasonCode);
        cv.put("main_problem", r.mainProblem);
        cv.put("affected_module", r.affectedModule);
        cv.put("suggested_action", r.suggestedAction);
        cv.put("short_text", r.shortText);
        cv.put("summary_text", r.summaryText);
        cv.put("diagnosis_text", r.diagnosisText);
        cv.put("repair_advice", r.repairAdvice);
        cv.put("history_count", r.historyCount);
        cv.put("valid_draw_count", r.validDrawCount);
        cv.put("invalid_draw_count", r.invalidDrawCount);
        cv.put("duplicate_issue_count", r.duplicateIssueCount);
        cv.put("latest_issue", r.latestIssue);
        cv.put("candidate_count", r.candidateCount);
        cv.put("valid_candidate_count", r.validCandidateCount);
        cv.put("invalid_candidate_count", r.invalidCandidateCount);
        cv.put("over_gate_candidate_count", r.overGateCandidateCount);
        cv.put("blocked_candidate_count", r.blockedCandidateCount);
        cv.put("real_trade_count", r.realTradeCount);
        cv.put("shadow_trade_count", r.shadowTradeCount);
        cv.put("hold_trade_count", r.holdTradeCount);
        cv.put("trade_hit_count", r.tradeHitCount);
        cv.put("trade_miss_count", r.tradeMissCount);
        cv.put("mixed_stat_count", r.mixedStatCount);
        cv.put("profile_count", r.profileCount);
        cv.put("active_profile_count", r.activeProfileCount);
        cv.put("stale_profile_count", r.staleProfileCount);
        cv.put("weak_profile_count", r.weakProfileCount);
        cv.put("fatal_count", r.fatalCount);
        cv.put("bad_count", r.badCount);
        cv.put("warn_count", r.warnCount);
        cv.put("data_text", r.dataText);
        cv.put("candidate_text", r.candidateText);
        cv.put("trade_text", r.tradeText);
        cv.put("profile_text", r.profileText);
        cv.put("decision_text", r.decisionText);
        cv.put("runtime_text", r.runtimeText);
        cv.put("best_candidate_text", r.bestCandidateText);
        cv.put("created_at", r.createdAt);

        cv.put("runtime_profile_count", countTable(TABLE_RUNTIME_PROFILE_CACHE, null, null));
        cv.put("state_number_profile_count", countTable(TABLE_STATE_NUMBER_PROFILE, null, null));
        cv.put("learning_case_count", countTable(TABLE_LEARNING_CASE_LOG, null, null));
        cv.put("teacher_matrix_sample_count", countTeacherMatrixSamples());
        cv.put("teacher_matrix_profile_count", countTeacherMatrixProfiles());
        cv.put("teacher_matrix_strong_count", countTeacherMatrixStrongProfiles());
        cv.put("teacher_matrix_risk_count", countTeacherMatrixRiskProfiles());
        cv.put("teacher_matrix_text", getState("teacher_matrix_summary", ""));

        long id =
                insertFiltered(
                        writable(),
                        DbHelper.TABLE_SELF_DIAGNOSIS_LOG,
                        cv,
                        SQLiteDatabase.CONFLICT_REPLACE);
        setState("self_diagnosis_summary", r.diagnosisText);

        return id;
    }

    public long saveRealtimeValidationReport(Models.BacktestReport r) {
        if (r == null) {
            return -1L;
        }

        r.recalc();

        SQLiteDatabase db = writable();
        ensureRealtimeValidationReportTable(db);

        long now = System.currentTimeMillis();

        if (r.createdAt <= 0L) {
            r.createdAt = now;
        }

        ContentValues cv = new ContentValues();

        cv.put("report_id", "RV_" + r.createdAt + "_" + Math.abs(r.window));
        cv.put(
                "mode",
                r.mode == null || r.mode.length() == 0 ? AppConfig.BACKTEST_MODE_FAST : r.mode);
        cv.put("type", "REALTIME_VALIDATE");
        cv.put("base_issue", r.startIssue);
        cv.put("target_issue", r.endIssue);
        cv.put("start_issue", r.startIssue);
        cv.put("end_issue", r.endIssue);
        cv.put("window_no", r.window);
        cv.put("total_count", r.totalCount);
        cv.put("test_count", r.testCount);
        cv.put("bet_count", r.betCount);
        cv.put("hit_count", r.hitCount);
        cv.put("miss_count", r.missCount);
        cv.put("hold_count", r.holdCount);
        cv.put("shadow_count", r.shadowCount);
        cv.put("hit_rate", r.hitRate);
        cv.put("win_rate", r.winRate);
        cv.put("frequency", r.frequency);
        cv.put("pnl", r.pnl);
        cv.put("max_drawdown", r.maxDrawdown);
        cv.put("longest_lose", r.longestLose);
        cv.put("avg_final_p", r.avgFinalP);
        cv.put("avg_gate", r.avgGate);
        cv.put("avg_edge", r.avgEdge);
        cv.put("calibration_error", r.calibrationError);
        cv.put("status", normalizeReportStatusForSnapshot(r.status, r.ok));
        cv.put(
                "summary",
                r.summary == null || r.summary.length() == 0
                        ? buildBacktestSummaryText(r)
                        : r.summary);
        cv.put("created_at", r.createdAt);
        cv.put("cost_ms", r.costMs);
        cv.put("usable", isRealtimeValidationUsable(r) ? 1 : 0);
        cv.put("teacher_matrix_sample_count", countTeacherMatrixSamples());
        cv.put("teacher_matrix_used_count", countTeacherMatrixProfiles());
        cv.put("teacher_matrix_avg_confidence", calcTeacherMatrixAvg("confidence"));
        cv.put("teacher_matrix_summary", getState("teacher_matrix_summary", ""));

        long id =
                insertFiltered(
                        db, TABLE_REALTIME_VALIDATION_REPORT, cv, SQLiteDatabase.CONFLICT_REPLACE);

        setState("realtime_validate_status", getStringFromCv(cv, "status"));
        setState("realtime_validate_summary", getStringFromCv(cv, "summary"));
        setState("realtime_validate_window", String.valueOf(r.window));
        setState("realtime_validate_hit_rate", String.valueOf(r.hitRate));
        setState("realtime_validate_frequency", String.valueOf(r.frequency));

        return id;
    }

    public Models.BacktestReport getLatestRealtimeValidationReport() {
        return getLatestRealtimeValidationReport(null);
    }

    public Models.BacktestReport getLatestRealtimeValidationReport(String targetIssue) {
        SQLiteDatabase db = writable();
        ensureRealtimeValidationReportTable(db);

        Cursor c = null;

        try {
            String where = null;
            String[] args = null;

            if (targetIssue != null && targetIssue.length() > 0) {
                where = "target_issue=?";
                args = new String[] {targetIssue};
            }

            c =
                    db.query(
                            TABLE_REALTIME_VALIDATION_REPORT,
                            null,
                            where,
                            args,
                            null,
                            null,
                            "created_at DESC, _id DESC",
                            "1");

            if (c.moveToFirst()) {
                return readRealtimeValidationReport(c);
            }
        } catch (Throwable e) {
        } finally {
            closeCursor(c);
        }

        return null;
    }

    public long saveTeacherSnapshotFromBacktest(Models.BacktestReport r) {
        if (r == null) {
            return -1L;
        }

        r.recalc();

        SQLiteDatabase db = writable();
        ensureTeacherSnapshotTable(db);

        long now = System.currentTimeMillis();

        if (r.createdAt <= 0L) {
            r.createdAt = now;
        }

        String status = normalizeReportStatusForSnapshot(r.status, r.ok);
        boolean usable = isTeacherSnapshotUsable(r, status);
        double gateAdjust = calcTeacherGateAdjust(r);

        String snapshotId = "TS_" + r.createdAt + "_W" + r.window;

        ContentValues cv = new ContentValues();

        cv.put("snapshot_id", snapshotId);
        cv.put("source_type", "TEACHER");
        cv.put("source_report_id", r.id);
        cv.put(
                "mode",
                r.mode == null || r.mode.length() == 0 ? AppConfig.BACKTEST_MODE_FULL : r.mode);
        cv.put("status", status);
        cv.put("start_issue", r.startIssue);
        cv.put("end_issue", r.endIssue);
        cv.put("window_no", r.window);
        cv.put("train_count", r.trainCount);
        cv.put("test_count", r.testCount);
        cv.put("total_count", r.totalCount);
        cv.put("bet_count", r.betCount);
        cv.put("hit_count", r.hitCount);
        cv.put("miss_count", r.missCount);
        cv.put("hold_count", r.holdCount);
        cv.put("shadow_count", r.shadowCount);
        cv.put("hit_rate", r.hitRate);
        cv.put("win_rate", r.winRate);
        cv.put("frequency", r.frequency);
        cv.put("pnl", r.pnl);
        cv.put("max_drawdown", r.maxDrawdown);
        cv.put("longest_lose", r.longestLose);
        cv.put("avg_raw_p", r.avgRawP);
        cv.put("avg_final_p", r.avgFinalP);
        cv.put("avg_gate", r.avgGate);
        cv.put("avg_edge", r.avgEdge);
        cv.put("calibration_error", r.calibrationError);
        cv.put("a_plus_hit_rate", r.aPlusHitRate);
        cv.put("bias_score_avg", r.biasScoreAvg);
        cv.put("brier_score", r.brierScore);
        cv.put("ai_judge_avg_score", r.aiJudgeAvgScore);
        cv.put("deep_score_avg", r.deepScoreAvg);
        cv.put("gate_adjust", gateAdjust);
        cv.put("position_weight_json", "");
        cv.put("state_weight_json", "");
        cv.put("combo_weight_json", "");
        cv.put(
                "summary",
                r.summary == null || r.summary.length() == 0
                        ? buildBacktestSummaryText(r)
                        : r.summary);
        cv.put("top_profiles", r.topProfiles);
        cv.put("top_miss_reasons", r.topMissReasons);
        cv.put("top_hit_reasons", r.topHitReasons);
        cv.put("created_at", r.createdAt);
        cv.put("usable", usable ? 1 : 0);

        long id = insertFiltered(db, TABLE_TEACHER_SNAPSHOT, cv, SQLiteDatabase.CONFLICT_REPLACE);

        setState("teacher_snapshot_status", status);
        setState("teacher_snapshot_id", snapshotId);
        setState("teacher_snapshot_summary", getStringFromCv(cv, "summary"));
        setState("teacher_snapshot_gate_adjust", String.valueOf(gateAdjust));
        setState("teacher_snapshot_usable", usable ? "1" : "0");
        setState("last_snapshot_id", snapshotId);

        cleanupOldTeacherSnapshots(db);

        return id;
    }

    public Models.BacktestReport getLatestTeacherSnapshotReport() {
        return getLatestTeacherSnapshotReport(false);
    }

    public Models.BacktestReport getLatestUsableTeacherSnapshotReport() {
        return getLatestTeacherSnapshotReport(true);
    }

    private Models.BacktestReport getLatestTeacherSnapshotReport(boolean onlyUsable) {
        SQLiteDatabase db = writable();
        ensureTeacherSnapshotTable(db);

        Cursor c = null;

        try {
            String where = null;
            String[] args = null;

            if (onlyUsable) {
                where = "usable=1 AND (status=? OR status=?)";
                args = new String[] {AppConfig.BACKTEST_STATUS_OK, "FINISHED"};
            }

            c =
                    db.query(
                            TABLE_TEACHER_SNAPSHOT,
                            null,
                            where,
                            args,
                            null,
                            null,
                            "created_at DESC, _id DESC",
                            "1");

            if (c.moveToFirst()) {
                return readTeacherSnapshotReport(c);
            }
        } catch (Throwable e) {
        } finally {
            closeCursor(c);
        }

        return null;
    }

    public double getLatestTeacherGateAdjust() {
        SQLiteDatabase db = writable();
        ensureTeacherSnapshotTable(db);

        Cursor c = null;

        try {
            c =
                    db.query(
                            TABLE_TEACHER_SNAPSHOT,
                            new String[] {"gate_adjust"},
                            "usable=1 AND (status=? OR status=?)",
                            new String[] {AppConfig.BACKTEST_STATUS_OK, "FINISHED"},
                            null,
                            null,
                            "created_at DESC, _id DESC",
                            "1");

            if (c.moveToFirst()) {
                double v = getDouble(c, "gate_adjust");

                if (v < 0.0d) {
                    return 0.0d;
                }

                return limitAdjustLocal(v, 0.020d);
            }
        } catch (Throwable e) {
        } finally {
            closeCursor(c);
        }

        return 0.0d;
    }

    private boolean isRealtimeValidationUsable(Models.BacktestReport r) {
        if (r == null) {
            return false;
        }

        r.recalc();

        if (r.window < AppConfig.AUTO_VALIDATE_MIN_WINDOW
                || r.window > AppConfig.AUTO_VALIDATE_MAX_WINDOW) {
            return false;
        }

        if (r.totalCount < AppConfig.AUTO_VALIDATE_SAMPLE_MIN
                && r.testCount < AppConfig.AUTO_VALIDATE_SAMPLE_MIN) {
            return false;
        }

        return true;
    }

    private boolean isTeacherSnapshotUsable(Models.BacktestReport r, String status) {
        if (r == null) {
            return false;
        }

        r.recalc();

        if (!AppConfig.BACKTEST_STATUS_OK.equals(status) && !"FINISHED".equals(status)) {
            return false;
        }

        if (r.window < AppConfig.MANUAL_BACKTEST_MIN_WINDOW) {
            return false;
        }

        if (r.betCount <= 0) {
            return false;
        }

        return true;
    }

    private String normalizeReportStatusForSnapshot(String status, int ok) {
        if (status == null || status.length() == 0) {
            return ok == 1 ? AppConfig.BACKTEST_STATUS_OK : AppConfig.BACKTEST_STATUS_ERROR;
        }

        if (AppConfig.BACKTEST_STATUS_OK.equals(status)
                || "FINISHED".equals(status)
                || AppConfig.BACKTEST_STATUS_RUNNING.equals(status)
                || AppConfig.BACKTEST_STATUS_ERROR.equals(status)
                || AppConfig.BACKTEST_STATUS_CANCELLED.equals(status)
                || AppConfig.BACKTEST_STATUS_IDLE.equals(status)) {
            return status;
        }

        if (ok == 1) {
            return AppConfig.BACKTEST_STATUS_OK;
        }

        return status;
    }

    private double calcTeacherGateAdjust(Models.BacktestReport r) {
        if (r == null) {
            return 0.0d;
        }

        r.recalc();

        double adjust = 0.0d;

        if (r.hitRate > 0.0d && r.hitRate < AppConfig.TARGET_HIT_RATE_MIN) {
            adjust += 0.010d;
        }

        if (r.winRate > 0.0d && r.winRate < AppConfig.TARGET_HIT_RATE_MIN) {
            adjust += 0.010d;
        }

        if (r.calibrationError > AppConfig.CALIBRATION_ERROR_MAX) {
            adjust += 0.006d;
        }

        if (r.maxDrawdown > Math.abs(AppConfig.LOSE_NET_PROFIT) * 6.0d) {
            adjust += 0.004d;
        }

        if (adjust < 0.0d) {
            adjust = 0.0d;
        }

        return limitAdjustLocal(adjust, 0.020d);
    }

    private void cleanupOldTeacherSnapshots(SQLiteDatabase db) {
        if (db == null) {
            return;
        }

        try {
            db.execSQL(
                    "DELETE FROM "
                            + TABLE_TEACHER_SNAPSHOT
                            + " WHERE _id NOT IN ("
                            + "SELECT _id FROM "
                            + TABLE_TEACHER_SNAPSHOT
                            + " ORDER BY created_at DESC, _id DESC LIMIT 20"
                            + ")");
        } catch (Throwable e) {
        }
    }

    private void ensureRealtimeValidationReportTable(SQLiteDatabase db) {
        if (db == null) {
            return;
        }

        try {
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS "
                            + TABLE_REALTIME_VALIDATION_REPORT
                            + " ("
                            + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "report_id TEXT UNIQUE,"
                            + "mode TEXT,"
                            + "type TEXT,"
                            + "base_issue TEXT,"
                            + "target_issue TEXT,"
                            + "start_issue TEXT,"
                            + "end_issue TEXT,"
                            + "window_no INTEGER DEFAULT 0,"
                            + "total_count INTEGER DEFAULT 0,"
                            + "test_count INTEGER DEFAULT 0,"
                            + "bet_count INTEGER DEFAULT 0,"
                            + "hit_count INTEGER DEFAULT 0,"
                            + "miss_count INTEGER DEFAULT 0,"
                            + "hold_count INTEGER DEFAULT 0,"
                            + "shadow_count INTEGER DEFAULT 0,"
                            + "hit_rate REAL DEFAULT 0,"
                            + "win_rate REAL DEFAULT 0,"
                            + "frequency REAL DEFAULT 0,"
                            + "pnl REAL DEFAULT 0,"
                            + "max_drawdown REAL DEFAULT 0,"
                            + "longest_lose INTEGER DEFAULT 0,"
                            + "avg_final_p REAL DEFAULT 0,"
                            + "avg_gate REAL DEFAULT 0,"
                            + "avg_edge REAL DEFAULT 0,"
                            + "calibration_error REAL DEFAULT 0,"
                            + "status TEXT,"
                            + "summary TEXT,"
                            + "created_at INTEGER DEFAULT 0,"
                            + "cost_ms INTEGER DEFAULT 0,"
                            + "usable INTEGER DEFAULT 0"
                            + ")");

            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_realtime_validation_target ON "
                            + TABLE_REALTIME_VALIDATION_REPORT
                            + "(target_issue)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_realtime_validation_created ON "
                            + TABLE_REALTIME_VALIDATION_REPORT
                            + "(created_at)");
        } catch (Throwable e) {
        }
    }

    private void ensureTeacherSnapshotTable(SQLiteDatabase db) {
        if (db == null) {
            return;
        }

        try {
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS "
                            + TABLE_TEACHER_SNAPSHOT
                            + " ("
                            + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "snapshot_id TEXT UNIQUE,"
                            + "source_type TEXT,"
                            + "source_report_id INTEGER DEFAULT 0,"
                            + "mode TEXT,"
                            + "status TEXT,"
                            + "start_issue TEXT,"
                            + "end_issue TEXT,"
                            + "window_no INTEGER DEFAULT 0,"
                            + "train_count INTEGER DEFAULT 0,"
                            + "test_count INTEGER DEFAULT 0,"
                            + "total_count INTEGER DEFAULT 0,"
                            + "bet_count INTEGER DEFAULT 0,"
                            + "hit_count INTEGER DEFAULT 0,"
                            + "miss_count INTEGER DEFAULT 0,"
                            + "hold_count INTEGER DEFAULT 0,"
                            + "shadow_count INTEGER DEFAULT 0,"
                            + "hit_rate REAL DEFAULT 0,"
                            + "win_rate REAL DEFAULT 0,"
                            + "frequency REAL DEFAULT 0,"
                            + "pnl REAL DEFAULT 0,"
                            + "max_drawdown REAL DEFAULT 0,"
                            + "longest_lose INTEGER DEFAULT 0,"
                            + "avg_raw_p REAL DEFAULT 0,"
                            + "avg_final_p REAL DEFAULT 0,"
                            + "avg_gate REAL DEFAULT 0,"
                            + "avg_edge REAL DEFAULT 0,"
                            + "calibration_error REAL DEFAULT 0,"
                            + "a_plus_hit_rate REAL DEFAULT 0,"
                            + "bias_score_avg REAL DEFAULT 0,"
                            + "brier_score REAL DEFAULT 0,"
                            + "ai_judge_avg_score REAL DEFAULT 0,"
                            + "deep_score_avg REAL DEFAULT 0,"
                            + "gate_adjust REAL DEFAULT 0,"
                            + "position_weight_json TEXT,"
                            + "state_weight_json TEXT,"
                            + "combo_weight_json TEXT,"
                            + "summary TEXT,"
                            + "top_profiles TEXT,"
                            + "top_miss_reasons TEXT,"
                            + "top_hit_reasons TEXT,"
                            + "created_at INTEGER DEFAULT 0,"
                            + "usable INTEGER DEFAULT 0"
                            + ")");

            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_teacher_snapshot_created ON "
                            + TABLE_TEACHER_SNAPSHOT
                            + "(created_at)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_teacher_snapshot_usable ON "
                            + TABLE_TEACHER_SNAPSHOT
                            + "(usable,status)");
        } catch (Throwable e) {
        }
    }

    private double calcFrequency(int window) {
        if (window <= 0) {
            window = AppConfig.FREQ_WINDOW_SHORT;
        }

        List<Models.Draw> draws = null;

        try {
            draws = getRecentDraws(window);
        } catch (Throwable e) {
            draws = null;
        }

        if (draws == null || draws.size() == 0) {
            return 0.0d;
        }

        HashSet<String> issueSet = new HashSet<String>();

        for (int i = 0; i < draws.size(); i++) {
            Models.Draw d = draws.get(i);

            if (d == null || d.issue == null || d.issue.length() == 0) {
                continue;
            }

            issueSet.add(d.issue);
        }

        if (issueSet.size() == 0) {
            return 0.0d;
        }

        int betCount = countRealBetIssueInRecentDrawIssues(issueSet, window);
        int total = issueSet.size();

        if (betCount < 0) {
            betCount = 0;
        }

        if (betCount > total) {
            betCount = total;
        }

        return total > 0
                ? AppConfig.clampProbability(((double) betCount) / ((double) total))
                : 0.0d;
    }

    private int countRealBetIssueInRecentDrawIssues(HashSet<String> issueSet, int window) {
        if (issueSet == null || issueSet.size() == 0) {
            return 0;
        }

        HashSet<String> betIssues = new HashSet<String>();

        countRealBetIssuesFromTrades(issueSet, betIssues, window);
        countRealBetIssuesFromDecisions(issueSet, betIssues, window);

        return betIssues.size();
    }

    private void countRealBetIssuesFromTrades(
            HashSet<String> issueSet, HashSet<String> betIssues, int window) {
        if (issueSet == null || betIssues == null || issueSet.size() == 0) {
            return;
        }

        Cursor c = null;

        try {
            int queryLimit = window * 5;

            if (queryLimit < 80) {
                queryLimit = 80;
            }

            if (queryLimit > 5000) {
                queryLimit = 5000;
            }

            c =
                    readable()
                            .query(
                                    DbHelper.TABLE_TRADES,
                                    null,
                                    "action=? AND stat_scope=?",
                                    new String[] {
                                        AppConfig.DECISION_BET, AppConfig.STAT_SCOPE_REAL_TRADE
                                    },
                                    null,
                                    null,
                                    "bet_time DESC, created_at DESC, _id DESC",
                                    String.valueOf(queryLimit));

            while (c.moveToNext()) {
                String targetIssue = getString(c, "target_issue");

                if (targetIssue == null || targetIssue.length() == 0) {
                    continue;
                }

                if (!issueSet.contains(targetIssue)) {
                    continue;
                }

                int settled = getInt(c, "settled");
                String result = AppConfig.normalizeResult(getString(c, "result"));

                if (settled == 1 && !AppConfig.isTerminalResult(result)) {
                    continue;
                }

                betIssues.add(targetIssue);
            }
        } catch (Throwable e) {
        } finally {
            closeCursor(c);
        }
    }

    private void countRealBetIssuesFromDecisions(
            HashSet<String> issueSet, HashSet<String> betIssues, int window) {
        if (issueSet == null || betIssues == null || issueSet.size() == 0) {
            return;
        }

        Cursor c = null;

        try {
            int queryLimit = window * 8;

            if (queryLimit < 120) {
                queryLimit = 120;
            }

            if (queryLimit > 6000) {
                queryLimit = 6000;
            }

            c =
                    readable()
                            .query(
                                    DbHelper.TABLE_DECISIONS,
                                    null,
                                    "decision=? AND stat_scope=?",
                                    new String[] {
                                        AppConfig.DECISION_BET, AppConfig.STAT_SCOPE_REAL_TRADE
                                    },
                                    null,
                                    null,
                                    "created_at DESC, _id DESC",
                                    String.valueOf(queryLimit));

            while (c.moveToNext()) {
                Models.Decision d = readDecision(c);

                if (d == null) {
                    continue;
                }

                if (d.targetIssue == null || d.targetIssue.length() == 0) {
                    continue;
                }

                if (!issueSet.contains(d.targetIssue)) {
                    continue;
                }

                if (!AppConfig.DECISION_BET.equals(d.decision)) {
                    continue;
                }

                if (!isValidBetDecision(d)) {
                    continue;
                }

                if (isWeakPlaceholderDecision(d) || isQuickObservationDecision(d)) {
                    continue;
                }

                if (!AppConfig.STAT_SCOPE_REAL_TRADE.equals(d.statScope)) {
                    continue;
                }

                betIssues.add(d.targetIssue);
            }
        } catch (Throwable e) {
        } finally {
            closeCursor(c);
        }
    }

    private int countDecisionByType(String decision) {
        if (decision == null || decision.length() == 0) {
            return 0;
        }

        return countTable(DbHelper.TABLE_DECISIONS, "decision=?", new String[] {decision});
    }

    private double calcMaxDrawdown() {
        Cursor c = null;
        double equity = 0.0d;
        double peak = 0.0d;
        double maxDd = 0.0d;

        try {
            c =
                    readable()
                            .query(
                                    DbHelper.TABLE_TRADES,
                                    new String[] {"pnl"},
                                    "settled=1 AND action=? AND stat_scope=? AND result IN (?,?)",
                                    new String[] {
                                        AppConfig.DECISION_BET,
                                        AppConfig.STAT_SCOPE_REAL_TRADE,
                                        AppConfig.RESULT_WIN,
                                        AppConfig.RESULT_MISS
                                    },
                                    null,
                                    null,
                                    "settle_time ASC, settled_at ASC, _id ASC");

            while (c.moveToNext()) {
                equity += getDouble(c, "pnl");

                if (equity > peak) {
                    peak = equity;
                }

                double dd = peak - equity;

                if (dd > maxDd) {
                    maxDd = dd;
                }
            }
        } finally {
            closeCursor(c);
        }

        return maxDd;
    }

    private int calcLoseStreak() {
        Cursor c = null;
        int streak = 0;

        try {
            c =
                    readable()
                            .query(
                                    DbHelper.TABLE_TRADES,
                                    new String[] {"result"},
                                    "settled=1 AND action=? AND stat_scope=? AND result IN (?,?)",
                                    new String[] {
                                        AppConfig.DECISION_BET,
                                        AppConfig.STAT_SCOPE_REAL_TRADE,
                                        AppConfig.RESULT_WIN,
                                        AppConfig.RESULT_MISS
                                    },
                                    null,
                                    null,
                                    "settle_time DESC, settled_at DESC, _id DESC");

            while (c.moveToNext()) {
                String r = AppConfig.normalizeResult(getString(c, "result"));

                if (AppConfig.RESULT_MISS.equals(r)) {
                    streak++;
                } else {
                    break;
                }
            }
        } finally {
            closeCursor(c);
        }

        return streak;
    }

    private Models.Draw readDraw(Cursor c) {
        Models.Draw d = new Models.Draw();

        d.id = getLong(c, "_id");
        d.issue = getString(c, "issue");
        d.openTime = getLong(c, "open_time");
        d.fetchTime = getLong(c, "fetch_time");
        d.sourceLine = getString(c, "source_line");

        d.numbers[0] = getInt(c, "n1");
        d.numbers[1] = getInt(c, "n2");
        d.numbers[2] = getInt(c, "n3");
        d.numbers[3] = getInt(c, "n4");
        d.numbers[4] = getInt(c, "n5");
        d.numbers[5] = getInt(c, "n6");
        d.numbers[6] = getInt(c, "n7");
        d.numbers[7] = getInt(c, "n8");
        d.numbers[8] = getInt(c, "n9");
        d.numbers[9] = getInt(c, "n10");

        d.rawJson = getString(c, "raw_json");
        d.dirty = getInt(c, "dirty");
        d.dirtyReason = getString(c, "dirty_reason");

        try {
            d.normalizeDirty();
        } catch (Throwable e) {
        }

        return d;
    }

    private Models.Decision readDecision(Cursor c) {
        Models.Decision d = new Models.Decision();

        d.id = getLong(c, "_id");
        d.tradeId = getLong(c, "trade_id");
        d.baseIssue = getString(c, "base_issue");
        d.targetIssue = getString(c, "target_issue");
        d.createdAt = getLong(c, "created_at");
        d.decision = AppConfig.normalizeDecision(getString(c, "decision"));
        d.action = firstNonEmpty(AppConfig.normalizeAction(getString(c, "action")), d.decision);

        d.position = getInt(c, "position");
        d.n1 = getInt(c, "n1");
        d.n2 = getInt(c, "n2");
        d.n3 = getInt(c, "n3");
        d.k3Mask = getInt(c, "k3_mask");

        d.rawP = getDouble(c, "raw_p");
        d.calibratedP = getDouble(c, "calibrated_p");
        d.finalP = getDouble(c, "final_p");
        d.dynamicGate = getDouble(c, "dynamic_gate");
        d.edge = getDouble(c, "edge");

        d.modelP = getDouble(c, "model_p");
        d.shapeP = getDouble(c, "shape_p");
        d.comboP = getDouble(c, "combo_p");
        d.recoveryP = getDouble(c, "recovery_p");
        d.similarP = getDouble(c, "similar_p");
        d.memoryP = getDouble(c, "memory_p");

        d.similarHitRate = getDouble(c, "similar_hit_rate");
        d.comboHitRate = getDouble(c, "combo_hit_rate");
        d.effectiveSample = getInt(c, "effective_sample");
        d.comboSample = getInt(c, "combo_sample");

        d.trendLife = getDouble(c, "trend_life");
        d.driftRisk = getDouble(c, "drift_risk");
        d.breakDepth = getDouble(c, "break_depth");
        d.recoveryScore = getDouble(c, "recovery_score");
        d.championGap = getDouble(c, "champion_gap");
        d.comboGap = getDouble(c, "combo_gap");
        d.hotReverseRisk = getDouble(c, "hot_reverse_risk");
        d.falseStrongRisk = getDouble(c, "false_strong_risk");

        d.freq100 = getDouble(c, "freq100");
        d.freq500 = getDouble(c, "freq500");
        d.freq1000 = getDouble(c, "freq1000");

        d.calibrationError = getDouble(c, "calibration_error");
        d.probabilityMode = getString(c, "probability_mode");
        d.backtestMode = getString(c, "backtest_mode");

        d.stateMode = getString(c, "state_mode");
        d.shapeKey = getString(c, "shape_key");
        d.blockCode = getString(c, "block_code");
        d.reasonSummary = getString(c, "reason_summary");
        d.reasonText = firstNonEmpty(getString(c, "reason_text"), d.reasonSummary);

        d.dataOk = getInt(c, "data_ok");
        d.settled = getInt(c, "settled");
        d.result = AppConfig.normalizeResult(getString(c, "result"));
        d.settleTime = getLong(c, "settle_time");
        d.settledAt = getLong(c, "settled_at");
        d.actualNumber = getInt(c, "actual_number");
        d.hit = getInt(c, "hit");
        d.pnl = getDouble(c, "pnl");
        d.missReason = getString(c, "miss_reason");
        d.settlementReason = getString(c, "settlement_reason");

        d.deepScore = getDouble(c, "deep_score");
        d.deepConfidence = getDouble(c, "deep_confidence");
        d.deepFeatureP = getDouble(c, "deep_feature_p");
        d.deepPositionScore = getDouble(c, "deep_position_score");
        d.deepNumberScore = getDouble(c, "deep_number_score");
        d.deepCycleScore = getDouble(c, "deep_cycle_score");
        d.deepTransitionScore = getDouble(c, "deep_transition_score");
        d.deepStateScore = getDouble(c, "deep_state_score");

        d.biasScore = getDouble(c, "bias_score");
        d.biasConfidence = getDouble(c, "bias_confidence");
        d.profileScore = getDouble(c, "profile_score");
        d.fallbackProfileScore = getDouble(c, "fallback_profile_score");
        d.auditPenalty = getDouble(c, "audit_penalty");
        d.auditBoost = getDouble(c, "audit_boost");

        d.aiJudgeScore = getDouble(c, "ai_judge_score");
        d.aiJudgeConfidence = getDouble(c, "ai_judge_confidence");
        d.evidenceScore = getDouble(c, "evidence_score");
        d.evidenceRisk = getDouble(c, "evidence_risk");
        d.conflictLevel = getDouble(c, "conflict_level");

        d.aPlusScore = getDouble(c, "a_plus_score");
        d.promotionScore = getDouble(c, "promotion_score");
        d.weakestNumberScore = getDouble(c, "weakest_number_score");
        d.balanceScore = getDouble(c, "balance_score");

        d.deepProfileKey = getString(c, "deep_profile_key");
        d.deepProfileHitRate = getDouble(c, "deep_profile_hit_rate");
        d.deepProfileSample = getInt(c, "deep_profile_sample");
        d.deepFallbackLevel = getInt(c, "deep_fallback_level");
        d.fallbackProfileKey = getString(c, "fallback_profile_key");
        d.promotionStatus = getString(c, "promotion_status");
        d.aPlusStatus = getString(c, "a_plus_status");
        d.candidateEvidenceKey = getString(c, "candidate_evidence_key");
        d.snapshotId = getString(c, "snapshot_id");

        d.deepAttribution = getString(c, "deep_attribution");
        d.deepReason = getString(c, "deep_reason");
        d.aiJudgeSummary = getString(c, "ai_judge_summary");
        d.learningSummary = getString(c, "learning_summary");
        d.selfDiagnosisText = getString(c, "self_diagnosis_text");

        d.manualBacktestSupport = getInt(c, "manual_backtest_support");
        d.statScope = getString(c, "stat_scope");

        d.stateKey = getString(c, "state_key");
        d.trendKey = getString(c, "trend_key");
        d.driftKey = getString(c, "drift_key");
        d.breakKey = getString(c, "break_key");
        d.recoveryKey = getString(c, "recovery_key");
        d.runtimeProfileScore = getDouble(c, "runtime_profile_score");
        d.stateNumberProfileScore = getDouble(c, "state_number_profile_score");
        d.teacherWeight = getDouble(c, "teacher_weight");
        d.stateConfidence = getDouble(c, "state_confidence");
        d.stateSample = getInt(c, "state_sample");
        d.cacheSample = getInt(c, "cache_sample");
        d.recentSample = getInt(c, "recent_sample");
        d.recentHitRate = getDouble(c, "recent_hit_rate");
        d.runtimeHitRate = getDouble(c, "runtime_hit_rate");
        d.numberProfileHitRate = getDouble(c, "number_profile_hit_rate");
        d.realtimeCostMs = getLong(c, "realtime_cost_ms");
        d.realtimeTimeout = getInt(c, "realtime_timeout");

        readTeacherMatrixFieldsToObject(c, d);

        if (d.edge == 0.0d && d.finalP > 0.0d && d.dynamicGate > 0.0d) {
            d.edge = d.finalP - d.dynamicGate;
        }

        d.statScope = AppConfig.normalizeStatScopeForDecision(d.decision, d.statScope);

        readFieldChainValues(c, d);
        readDynamicCausalFields(c, d);
        readMainChainClosedLoopFields(c, d);
        readV24ContractValues(c, d);
        d.v46EventProbability = getDouble(c, "v46_event_probability");
        d.v46ActionLcb = getDouble(c, "v46_action_lcb");
        d.v46ProbabilityUncertainty = getDouble(c, "v46_probability_uncertainty");
        d.v46ExpectedValue = getDouble(c, "v46_expected_value");
        d.v46FinalRank = getInt(c, "v46_final_rank");
        d.v46FormalCandidateCount = getInt(c, "v46_formal_candidate_count");
        d.v46FinalContractHash = getString(c, "v46_final_contract_hash");
        d.v46ContractHash = d.v46FinalContractHash;
        d.v46Lane = getString(c, "v46_lane");

        return d;
    }

    private Models.Trade readTrade(Cursor c) {
        Models.Trade t = new Models.Trade();

        t.id = getLong(c, "_id");
        t.decisionId = getLong(c, "decision_id");
        t.baseIssue = getString(c, "base_issue");
        t.targetIssue = getString(c, "target_issue");

        t.betTime = getLong(c, "bet_time");
        t.createdAt = getLong(c, "created_at");

        if (t.createdAt <= 0L) {
            t.createdAt = t.betTime;
        }

        t.settleTime = getLong(c, "settle_time");
        t.settledAt = getLong(c, "settled_at");

        if (t.settledAt <= 0L) {
            t.settledAt = t.settleTime;
        }

        t.action =
                firstNonEmpty(
                        AppConfig.normalizeAction(getString(c, "action")), AppConfig.DECISION_BET);

        t.position = getInt(c, "position");
        t.n1 = getInt(c, "n1");
        t.n2 = getInt(c, "n2");
        t.n3 = getInt(c, "n3");
        t.k3Mask = getInt(c, "k3_mask");

        t.rawP = getDouble(c, "raw_p");
        t.calibratedP = getDouble(c, "calibrated_p");
        t.finalP = getDouble(c, "final_p");
        t.dynamicGate = getDouble(c, "dynamic_gate");
        t.edge = getDouble(c, "edge");

        if (t.edge == 0.0d) {
            t.edge = t.finalP - t.dynamicGate;
        }

        t.modelP = getDouble(c, "model_p");
        t.shapeP = getDouble(c, "shape_p");
        t.comboP = getDouble(c, "combo_p");
        t.recoveryP = getDouble(c, "recovery_p");
        t.similarP = getDouble(c, "similar_p");
        t.memoryP = getDouble(c, "memory_p");
        t.calibrationError = getDouble(c, "calibration_error");

        t.similarHitRate = getDouble(c, "similar_hit_rate");
        t.comboHitRate = getDouble(c, "combo_hit_rate");
        t.effectiveSample = getInt(c, "effective_sample");
        t.comboSample = getInt(c, "combo_sample");

        t.trendLife = getDouble(c, "trend_life");
        t.driftRisk = getDouble(c, "drift_risk");
        t.breakDepth = getDouble(c, "break_depth");
        t.recoveryScore = getDouble(c, "recovery_score");
        t.championGap = getDouble(c, "champion_gap");
        t.comboGap = getDouble(c, "combo_gap");
        t.hotReverseRisk = getDouble(c, "hot_reverse_risk");
        t.falseStrongRisk = getDouble(c, "false_strong_risk");

        t.stateMode = getString(c, "state_mode");
        t.shapeKey = getString(c, "shape_key");
        t.reasonSummary = getString(c, "reason_summary");

        t.result = AppConfig.normalizeResult(getString(c, "result"));
        t.actualNumber = getInt(c, "actual_number");
        t.hit = getInt(c, "hit");
        t.pnl = getDouble(c, "pnl");
        t.missReason = getString(c, "miss_reason");
        t.settlementReason = getString(c, "settlement_reason");
        t.settled = getInt(c, "settled");

        t.deepScore = getDouble(c, "deep_score");
        t.deepConfidence = getDouble(c, "deep_confidence");
        t.deepFeatureP = getDouble(c, "deep_feature_p");
        t.deepPositionScore = getDouble(c, "deep_position_score");
        t.deepNumberScore = getDouble(c, "deep_number_score");
        t.deepCycleScore = getDouble(c, "deep_cycle_score");
        t.deepTransitionScore = getDouble(c, "deep_transition_score");
        t.deepStateScore = getDouble(c, "deep_state_score");

        t.aiJudgeScore = getDouble(c, "ai_judge_score");
        t.aiJudgeConfidence = getDouble(c, "ai_judge_confidence");
        t.evidenceScore = getDouble(c, "evidence_score");
        t.evidenceRisk = getDouble(c, "evidence_risk");

        t.biasScore = getDouble(c, "bias_score");
        t.biasConfidence = getDouble(c, "bias_confidence");
        t.auditPenalty = getDouble(c, "audit_penalty");
        t.auditBoost = getDouble(c, "audit_boost");
        t.profileScore = getDouble(c, "profile_score");
        t.fallbackProfileScore = getDouble(c, "fallback_profile_score");

        t.aPlusScore = getDouble(c, "a_plus_score");
        t.promotionScore = getDouble(c, "promotion_score");
        t.conflictLevel = getDouble(c, "conflict_level");
        t.weakestNumberScore = getDouble(c, "weakest_number_score");
        t.balanceScore = getDouble(c, "balance_score");

        t.deepProfileKey = getString(c, "deep_profile_key");
        t.deepProfileHitRate = getDouble(c, "deep_profile_hit_rate");
        t.deepProfileSample = getInt(c, "deep_profile_sample");
        t.deepFallbackLevel = getInt(c, "deep_fallback_level");
        t.fallbackProfileKey = getString(c, "fallback_profile_key");
        t.promotionStatus = getString(c, "promotion_status");
        t.aPlusStatus = getString(c, "a_plus_status");
        t.candidateEvidenceKey = getString(c, "candidate_evidence_key");
        t.snapshotId = getString(c, "snapshot_id");

        t.attributionReasonCode = getString(c, "attribution_reason_code");
        t.attributionText = getString(c, "attribution_text");
        t.affectedModule = getString(c, "affected_module");
        t.statScope = getString(c, "stat_scope");

        t.stateKey = getString(c, "state_key");
        t.runtimeProfileScore = getDouble(c, "runtime_profile_score");
        t.stateNumberProfileScore = getDouble(c, "state_number_profile_score");
        t.teacherWeight = getDouble(c, "teacher_weight");

        readTeacherMatrixFieldsToObject(c, t);

        if (t.statScope == null || t.statScope.length() == 0) {
            t.statScope = AppConfig.STAT_SCOPE_REAL_TRADE;
        }

        readFieldChainValues(c, t);
        readDynamicCausalFields(c, t);
        readMainChainClosedLoopFields(c, t);
        readV24ContractValues(c, t);
        t.v46EventProbability = getDouble(c, "v46_event_probability");
        t.v46ActionLcb = getDouble(c, "v46_action_lcb");
        t.v46ProbabilityUncertainty = getDouble(c, "v46_probability_uncertainty");
        t.v46ExpectedValue = getDouble(c, "v46_expected_value");
        t.v46FinalRank = getInt(c, "v46_final_rank");
        t.v46FormalCandidateCount = getInt(c, "v46_formal_candidate_count");
        t.v46ContractHash = getString(c, "v46_final_contract_hash");
        t.laneCandidate = getString(c, "v46_lane");

        return t;
    }

    private Models.ComboCandidate readShadowCandidate(Cursor c) {
        Models.ComboCandidate cc = new Models.ComboCandidate();

        cc.rank = getInt(c, "rank_no");
        cc.baseIssue = getString(c, "base_issue");
        cc.targetIssue = getString(c, "target_issue");
        cc.createdAt = getLong(c, "created_at");

        cc.position = getInt(c, "position");
        cc.n1 = getInt(c, "n1");
        cc.n2 = getInt(c, "n2");
        cc.n3 = getInt(c, "n3");
        cc.k3Mask = getInt(c, "k3_mask");

        cc.shapeKey = getString(c, "shape_key");
        cc.stateMode = getString(c, "state_mode");

        cc.modelP = getDouble(c, "model_p");
        cc.shapeP = getDouble(c, "shape_p");
        cc.comboP = getDouble(c, "combo_p");
        cc.recoveryP = getDouble(c, "recovery_p");
        cc.similarP = getDouble(c, "similar_p");
        cc.memoryP = getDouble(c, "memory_p");

        cc.rawP = getDouble(c, "raw_p");
        cc.calibratedP = getDouble(c, "calibrated_p");
        cc.finalP = getDouble(c, "final_p");
        cc.dynamicGate = getDouble(c, "dynamic_gate");
        cc.edge = getDouble(c, "edge");

        if (cc.edge == 0.0d) {
            cc.edge = cc.finalP - cc.dynamicGate;
        }

        cc.similarHitRate = getDouble(c, "similar_hit_rate");
        cc.comboHitRate = getDouble(c, "combo_hit_rate");
        cc.effectiveSample = getInt(c, "effective_sample");
        cc.comboSample = getInt(c, "combo_sample");

        cc.trendLife = getDouble(c, "trend_life");
        cc.driftRisk = getDouble(c, "drift_risk");
        cc.breakDepth = getDouble(c, "break_depth");
        cc.recoveryScore = getDouble(c, "recovery_score");
        cc.championGap = getDouble(c, "champion_gap");
        cc.comboGap = getDouble(c, "combo_gap");
        cc.hotReverseRisk = getDouble(c, "hot_reverse_risk");
        cc.falseStrongRisk = getDouble(c, "false_strong_risk");

        cc.calibrationError = getDouble(c, "calibration_error");
        cc.probabilityMode = getString(c, "probability_mode");
        cc.backtestMode = getString(c, "backtest_mode");

        cc.deepScore = getDouble(c, "deep_score");
        cc.deepConfidence = getDouble(c, "deep_confidence");
        cc.deepPositionScore = getDouble(c, "deep_position_score");
        cc.deepNumberScore = getDouble(c, "deep_number_score");
        cc.deepCycleScore = getDouble(c, "deep_cycle_score");
        cc.deepTransitionScore = getDouble(c, "deep_transition_score");
        cc.deepStateScore = getDouble(c, "deep_state_score");

        cc.biasScore = getDouble(c, "bias_score");
        cc.biasConfidence = getDouble(c, "bias_confidence");
        cc.profileScore = getDouble(c, "profile_score");
        cc.fallbackProfileScore = getDouble(c, "fallback_profile_score");
        cc.auditPenalty = getDouble(c, "audit_penalty");
        cc.auditBoost = getDouble(c, "audit_boost");

        cc.promotionScore = getDouble(c, "promotion_score");
        cc.aPlusScore = getDouble(c, "a_plus_score");
        cc.evidenceScore = getDouble(c, "evidence_score");
        cc.evidenceRisk = getDouble(c, "evidence_risk");

        cc.aiJudgeScore = getDouble(c, "ai_judge_score");
        cc.aiJudgeConfidence = getDouble(c, "ai_judge_confidence");
        cc.weakestNumberScore = getDouble(c, "weakest_number_score");
        cc.balanceScore = getDouble(c, "balance_score");

        cc.deepProfileKey = getString(c, "deep_profile_key");
        cc.deepProfileHitRate = getDouble(c, "deep_profile_hit_rate");
        cc.deepProfileSample = getInt(c, "deep_profile_sample");
        cc.deepFallbackLevel = getInt(c, "deep_fallback_level");
        cc.fallbackProfileKey = getString(c, "fallback_profile_key");
        cc.candidateEvidenceKey = getString(c, "candidate_evidence_key");
        cc.promotionStatus = getString(c, "promotion_status");
        cc.aPlusStatus = getString(c, "a_plus_status");
        cc.deepReason = getString(c, "deep_reason");

        cc.stateKey = getString(c, "state_key");
        cc.runtimeProfileScore = getDouble(c, "runtime_profile_score");
        cc.stateNumberProfileScore = getDouble(c, "state_number_profile_score");
        cc.numberBias = getDouble(c, "number_bias");
        cc.comboBias = getDouble(c, "combo_bias");
        cc.teacherWeight = getDouble(c, "teacher_weight");
        cc.stateConfidence = getDouble(c, "state_confidence");
        cc.stateSample = getInt(c, "state_sample");
        cc.cacheSample = getInt(c, "cache_sample");
        cc.recentSample = getInt(c, "recent_sample");
        cc.recentHitRate = getDouble(c, "recent_hit_rate");

        readTeacherMatrixFieldsToObject(c, cc);
        readFieldChainValues(c, cc);
        readDynamicCausalFields(c, cc);
        readMainChainClosedLoopFields(c, cc);

        return cc;
    }

    private Models.ShapeVector readShapeVector(Cursor c) {
        Models.ShapeVector v = new Models.ShapeVector();

        v.id = getLong(c, "_id");
        v.baseIssue = getString(c, "base_issue");
        v.targetIssue = getString(c, "target_issue");
        v.createdAt = getLong(c, "created_at");
        v.position = getInt(c, "position");

        v.entropy16 = getDouble(c, "entropy16");
        v.entropy32 = getDouble(c, "entropy32");
        v.entropy64 = getDouble(c, "entropy64");
        v.entropy128 = getDouble(c, "entropy128");
        v.entropy240 = getDouble(c, "entropy240");
        v.entropy480 = getDouble(c, "entropy480");
        v.entropy960 = getDouble(c, "entropy960");

        v.top3Mass16 = getDouble(c, "top3_mass16");
        v.top3Mass32 = getDouble(c, "top3_mass32");
        v.top3Mass64 = getDouble(c, "top3_mass64");
        v.top3Mass128 = getDouble(c, "top3_mass128");
        v.top3Mass240 = getDouble(c, "top3_mass240");
        v.top4Mass128 = getDouble(c, "top4_mass128");
        v.top6Mass240 = getDouble(c, "top6_mass240");

        v.slopeShort = getDouble(c, "slope_short");
        v.slopeMid = getDouble(c, "slope_mid");
        v.slopeLong = getDouble(c, "slope_long");

        v.driftShort = getDouble(c, "drift_short");
        v.driftMid = getDouble(c, "drift_mid");
        v.driftLong = getDouble(c, "drift_long");
        v.driftRisk = getDouble(c, "drift_risk");

        v.markov1Support = getDouble(c, "markov1_support");
        v.markov2Support = getDouble(c, "markov2_support");

        v.trendLife = getDouble(c, "trend_life");
        v.breakDepth = getDouble(c, "break_depth");
        v.recoveryScore = getDouble(c, "recovery_score");
        v.championGap = getDouble(c, "champion_gap");
        v.comboGap = getDouble(c, "combo_gap");
        v.hotReverseRisk = getDouble(c, "hot_reverse_risk");
        v.falseStrongRisk = getDouble(c, "false_strong_risk");

        v.stateMode = getString(c, "state_mode");
        v.shapeKey = getString(c, "shape_key");

        for (int n = 1; n <= 10; n++) {
            v.numberProbs[n] = getDouble(c, probColumn(n));
            v.markovProbs[n] = getDouble(c, markovColumn(n));
        }

        v.deepScore = getDouble(c, "deep_score");
        v.stateScore = getDouble(c, "state_score");
        v.cycleScore = getDouble(c, "cycle_score");
        v.transitionScore = getDouble(c, "transition_score");
        v.biasScore = getDouble(c, "bias_score");
        v.profileScore = getDouble(c, "profile_score");
        v.deepProfileKey = getString(c, "deep_profile_key");
        v.stateKey = getString(c, "state_key");
        v.runtimeProfileScore = getDouble(c, "runtime_profile_score");

        setOptionalDoubleField(v, "teacherMatrixScore", getDouble(c, "teacher_matrix_score"));

        return v;
    }

    private Models.BacktestReport readBacktestReport(Cursor c) {
        Models.BacktestReport r = new Models.BacktestReport();

        r.id = getLong(c, "_id");

        r.ok = getInt(c, "ok");
        r.mode = getString(c, "mode");

        if (r.mode == null || r.mode.length() == 0) {
            r.mode = AppConfig.BACKTEST_MODE_FAST;
        }

        r.type = getString(c, "type");
        r.window = getIntAny(c, "window_no", "window");

        r.rangeStart = getString(c, "range_start");
        r.rangeEnd = getString(c, "range_end");
        r.startIssue = getString(c, "start_issue");
        r.endIssue = getString(c, "end_issue");

        if (r.startIssue == null || r.startIssue.length() == 0) {
            r.startIssue = r.rangeStart;
        }

        if (r.endIssue == null || r.endIssue.length() == 0) {
            r.endIssue = r.rangeEnd;
        }

        r.trainCount = getInt(c, "train_count");
        r.testCount = getInt(c, "test_count");
        r.totalCount = getInt(c, "total_count");

        if (r.totalCount <= 0) {
            r.totalCount = r.testCount;
        }

        r.betCount = getInt(c, "bet_count");
        r.hitCount = getInt(c, "hit_count");
        r.missCount = getInt(c, "miss_count");

        if (r.missCount <= 0) {
            r.missCount = r.betCount - r.hitCount;

            if (r.missCount < 0) {
                r.missCount = 0;
            }
        }

        r.holdCount = getInt(c, "hold_count");
        r.shadowCount = getInt(c, "shadow_count");

        r.candidateCount = getInt(c, "candidate_count");
        r.enrichCount = getInt(c, "enrich_count");
        r.deepCount = getInt(c, "deep_count");

        r.hitRate = getDouble(c, "hit_rate");
        r.winRate = getDouble(c, "win_rate");

        if (r.winRate <= 0.0d) {
            r.winRate = r.hitRate;
        }

        r.frequency = getDouble(c, "frequency");
        r.pnl = getDouble(c, "pnl");
        r.maxDrawdown = getDouble(c, "max_drawdown");
        r.longestLose = getInt(c, "longest_lose");

        r.avgRawP = getDouble(c, "avg_raw_p");
        r.avgFinalP = getDouble(c, "avg_final_p");
        r.avgGate = getDouble(c, "avg_gate");
        r.avgEdge = getDouble(c, "avg_edge");
        r.calibrationError = getDouble(c, "calibration_error");

        r.status = getString(c, "status");
        r.summary = getString(c, "summary");

        if (r.summary == null || r.summary.length() == 0) {
            r.summary = r.status;
        }

        r.startedAt = getLong(c, "started_at");
        r.finishedAt = getLong(c, "finished_at");
        r.costMs = getLong(c, "cost_ms");
        r.createdAt = getLong(c, "created_at");

        r.manualTriggered = getInt(c, "manual_triggered");
        r.profileCount = getInt(c, "profile_count");
        r.attributionCount = getInt(c, "attribution_count");
        r.promotedCount = getInt(c, "promoted_count");
        r.rejectedCount = getInt(c, "rejected_count");
        r.aPlusCount = getInt(c, "a_plus_count");
        r.progress = getInt(c, "progress");
        r.aPlusHitRate = getDouble(c, "a_plus_hit_rate");
        r.biasScoreAvg = getDouble(c, "bias_score_avg");
        r.brierScore = getDouble(c, "brier_score");
        r.aiJudgeAvgScore = getDouble(c, "ai_judge_avg_score");
        r.deepScoreAvg = getDouble(c, "deep_score_avg");
        r.biasSummary = getString(c, "bias_summary");
        r.auditSummary = getString(c, "audit_summary");
        r.promotionSummary = getString(c, "promotion_summary");
        r.aPlusSummary = getString(c, "a_plus_summary");
        r.learningImpactSummary = getString(c, "learning_impact_summary");
        r.topProfiles = getString(c, "top_profiles");
        r.topMissReasons = getString(c, "top_miss_reasons");
        r.topHitReasons = getString(c, "top_hit_reasons");

        setOptionalIntField(
                r, "teacherMatrixSampleCount", getInt(c, "teacher_matrix_sample_count"));
        setOptionalIntField(
                r, "teacherMatrixProfileCount", getInt(c, "teacher_matrix_profile_count"));
        setOptionalDoubleField(
                r, "teacherMatrixAvgHitRate", getDouble(c, "teacher_matrix_avg_hit_rate"));
        setOptionalDoubleField(
                r, "teacherMatrixAvgConfidence", getDouble(c, "teacher_matrix_avg_confidence"));
        setOptionalStringField(r, "teacherMatrixSummary", getString(c, "teacher_matrix_summary"));

        if (r.window <= 0) {
            r.window = r.testCount;
        }

        return r;
    }

    private Models.BacktestReport readRealtimeValidationReport(Cursor c) {
        Models.BacktestReport r = new Models.BacktestReport();

        r.type = getString(c, "type");
        r.mode = getString(c, "mode");
        r.startIssue = getString(c, "start_issue");
        r.endIssue = getString(c, "end_issue");
        r.rangeStart = r.startIssue;
        r.rangeEnd = r.endIssue;
        r.window = getInt(c, "window_no");
        r.totalCount = getInt(c, "total_count");
        r.testCount = getInt(c, "test_count");
        r.betCount = getInt(c, "bet_count");
        r.hitCount = getInt(c, "hit_count");
        r.missCount = getInt(c, "miss_count");
        r.holdCount = getInt(c, "hold_count");
        r.shadowCount = getInt(c, "shadow_count");
        r.hitRate = getDouble(c, "hit_rate");
        r.winRate = getDouble(c, "win_rate");
        r.frequency = getDouble(c, "frequency");
        r.pnl = getDouble(c, "pnl");
        r.maxDrawdown = getDouble(c, "max_drawdown");
        r.longestLose = getInt(c, "longest_lose");
        r.avgFinalP = getDouble(c, "avg_final_p");
        r.avgGate = getDouble(c, "avg_gate");
        r.avgEdge = getDouble(c, "avg_edge");
        r.calibrationError = getDouble(c, "calibration_error");
        r.status = getString(c, "status");
        r.summary = getString(c, "summary");
        r.createdAt = getLong(c, "created_at");
        r.costMs = getLong(c, "cost_ms");
        r.ok = getInt(c, "usable");

        setOptionalIntField(
                r, "teacherMatrixSampleCount", getInt(c, "teacher_matrix_sample_count"));
        setOptionalIntField(r, "teacherMatrixUsedCount", getInt(c, "teacher_matrix_used_count"));
        setOptionalDoubleField(
                r, "teacherMatrixAvgConfidence", getDouble(c, "teacher_matrix_avg_confidence"));
        setOptionalStringField(r, "teacherMatrixSummary", getString(c, "teacher_matrix_summary"));

        r.recalc();

        return r;
    }

    private Models.BacktestReport readTeacherSnapshotReport(Cursor c) {
        Models.BacktestReport r = new Models.BacktestReport();

        r.type = getString(c, "source_type");
        r.mode = getString(c, "mode");
        r.status = getString(c, "status");
        r.startIssue = getString(c, "start_issue");
        r.endIssue = getString(c, "end_issue");
        r.rangeStart = r.startIssue;
        r.rangeEnd = r.endIssue;
        r.window = getInt(c, "window_no");
        r.trainCount = getInt(c, "train_count");
        r.testCount = getInt(c, "test_count");
        r.totalCount = getInt(c, "total_count");
        r.betCount = getInt(c, "bet_count");
        r.hitCount = getInt(c, "hit_count");
        r.missCount = getInt(c, "miss_count");
        r.holdCount = getInt(c, "hold_count");
        r.shadowCount = getInt(c, "shadow_count");
        r.hitRate = getDouble(c, "hit_rate");
        r.winRate = getDouble(c, "win_rate");
        r.frequency = getDouble(c, "frequency");
        r.pnl = getDouble(c, "pnl");
        r.maxDrawdown = getDouble(c, "max_drawdown");
        r.longestLose = getInt(c, "longest_lose");
        r.avgRawP = getDouble(c, "avg_raw_p");
        r.avgFinalP = getDouble(c, "avg_final_p");
        r.avgGate = getDouble(c, "avg_gate");
        r.avgEdge = getDouble(c, "avg_edge");
        r.calibrationError = getDouble(c, "calibration_error");
        r.aPlusHitRate = getDouble(c, "a_plus_hit_rate");
        r.biasScoreAvg = getDouble(c, "bias_score_avg");
        r.brierScore = getDouble(c, "brier_score");
        r.aiJudgeAvgScore = getDouble(c, "ai_judge_avg_score");
        r.deepScoreAvg = getDouble(c, "deep_score_avg");
        r.summary = getString(c, "summary");
        r.topProfiles = getString(c, "top_profiles");
        r.topMissReasons = getString(c, "top_miss_reasons");
        r.topHitReasons = getString(c, "top_hit_reasons");
        r.createdAt = getLong(c, "created_at");
        r.ok = getInt(c, "usable");

        r.recalc();

        return r;
    }

    private Models.PointInTimeSnapshot readPointInTimeSnapshot(Cursor c) {
        Models.PointInTimeSnapshot s = new Models.PointInTimeSnapshot();

        s.snapshotId = getString(c, "snapshot_id");
        s.baseIssue = getString(c, "base_issue");
        s.targetIssue = getString(c, "target_issue");
        s.trainStartIssue = getString(c, "train_start_issue");
        s.trainEndIssue = getString(c, "train_end_issue");
        s.visibleDrawCount = getInt(c, "visible_draw_count");
        s.visibleTradeCount = getInt(c, "visible_trade_count");
        s.visibleProfileVersion = getLong(c, "visible_profile_version");
        s.visibleCalibrationVersion = getLong(c, "visible_calibration_version");
        s.featureSummary = getString(c, "feature_summary");
        s.candidateSummary = getString(c, "candidate_summary");
        s.gateSummary = getString(c, "gate_summary");
        s.decisionSummary = getString(c, "decision_summary");
        s.dataHash = getString(c, "data_hash");
        s.noFutureLeakOk = getInt(c, "no_future_leak_ok");
        s.status = getString(c, "status");
        s.warningText = getString(c, "warning_text");
        s.createdAt = getLong(c, "created_at");

        setOptionalLongField(
                s, "visibleTeacherMatrixVersion", getLong(c, "visible_teacher_matrix_version"));

        return s;
    }

    private Models.CandidateEvidence readCandidateEvidence(Cursor c) {
        Models.CandidateEvidence e = new Models.CandidateEvidence();

        e.evidenceKey = getString(c, "evidence_key");
        e.baseIssue = getString(c, "base_issue");
        e.targetIssue = getString(c, "target_issue");
        e.position = getInt(c, "position");
        e.n1 = getInt(c, "n1");
        e.n2 = getInt(c, "n2");
        e.n3 = getInt(c, "n3");
        e.k3Mask = getInt(c, "k3_mask");
        e.positionEvidence = getDouble(c, "position_evidence");
        e.numberEvidence = getDouble(c, "number_evidence");
        e.cycleEvidence = getDouble(c, "cycle_evidence");
        e.transitionEvidence = getDouble(c, "transition_evidence");
        e.patternEvidence = getDouble(c, "pattern_evidence");
        e.comboEvidence = getDouble(c, "combo_evidence");
        e.profileEvidence = getDouble(c, "profile_evidence");
        e.auditEvidence = getDouble(c, "audit_evidence");
        e.riskScore = getDouble(c, "risk_score");
        e.evidenceScore = getDouble(c, "evidence_score");
        e.confidence = getDouble(c, "confidence");
        e.supportText = getString(c, "support_text");
        e.riskText = getString(c, "risk_text");
        e.summaryText = getString(c, "summary_text");
        e.createdAt = getLong(c, "created_at");

        setOptionalStringField(e, "stateKey", getString(c, "state_key"));
        setOptionalDoubleField(
                e, "runtimeProfileEvidence", getDouble(c, "runtime_profile_evidence"));
        setOptionalDoubleField(e, "stateNumberEvidence", getDouble(c, "state_number_evidence"));
        setOptionalDoubleField(e, "teacherMatrixEvidence", getDouble(c, "teacher_matrix_evidence"));

        return e;
    }

    private Models.AiJudgeResult readAiJudgeResult(Cursor c) {
        Models.AiJudgeResult r = new Models.AiJudgeResult();

        r.judgeId = getString(c, "judge_id");
        r.baseIssue = getString(c, "base_issue");
        r.targetIssue = getString(c, "target_issue");
        r.bestCandidate = getCandidateEvidence(getString(c, "best_evidence_key"));
        r.secondCandidate = getCandidateEvidence(getString(c, "second_evidence_key"));
        r.championGap = getDouble(c, "champion_gap");
        r.judgeScore = getDouble(c, "judge_score");
        r.judgeConfidence = getDouble(c, "judge_confidence");
        r.conflictLevel = getDouble(c, "conflict_level");
        r.betAdvice = getString(c, "bet_advice");
        r.holdReason = getString(c, "hold_reason");
        r.summaryText = getString(c, "summary_text");
        r.createdAt = getLong(c, "created_at");

        return r;
    }

    private Models.BiasEvidenceResult readBiasEvidence(Cursor c) {
        Models.BiasEvidenceResult r = new Models.BiasEvidenceResult();

        r.evidenceId = getString(c, "evidence_id");
        r.issue = getString(c, "issue");
        r.baseIssue = getString(c, "base_issue");
        r.targetIssue = getString(c, "target_issue");
        r.biasScore = getDouble(c, "bias_score");
        r.positionBiasScore = getDouble(c, "position_bias_score");
        r.numberBiasScore = getDouble(c, "number_bias_score");
        r.transitionBiasScore = getDouble(c, "transition_bias_score");
        r.cycleBiasScore = getDouble(c, "cycle_bias_score");
        r.comboBiasScore = getDouble(c, "combo_bias_score");
        r.stateBiasScore = getDouble(c, "state_bias_score");
        r.sampleCount = getInt(c, "sample_count");
        r.recentSample = getInt(c, "recent_sample");
        r.confidence = getDouble(c, "confidence");
        r.status = getString(c, "status");
        r.summaryText = getString(c, "summary_text");
        r.createdAt = getLong(c, "created_at");

        return r;
    }

    private Models.DeepProfile readDeepProfile(Cursor c) {
        Models.DeepProfile p = new Models.DeepProfile();

        p.profileKey = getString(c, "profile_key");
        p.position = getInt(c, "position");
        p.stateMode = getString(c, "state_mode");
        p.trendBucket = getInt(c, "trend_bucket");
        p.driftBucket = getInt(c, "drift_bucket");
        p.breakBucket = getInt(c, "break_bucket");
        p.recoveryBucket = getInt(c, "recovery_bucket");
        p.cycleBucket = getInt(c, "cycle_bucket");
        p.relationBucket = getInt(c, "relation_bucket");
        p.totalSample = getInt(c, "total_sample");
        p.totalHit = getInt(c, "total_hit");
        p.totalHitRate = getDouble(c, "total_hit_rate");
        p.recentSample = getInt(c, "recent_sample");
        p.recentHit = getInt(c, "recent_hit");
        p.recentHitRate = getDouble(c, "recent_hit_rate");
        p.decayWeight = getDouble(c, "decay_weight");
        p.activeFlag = getInt(c, "active_flag");
        p.staleFlag = getInt(c, "stale_flag");
        p.recoverFlag = getInt(c, "recover_flag");
        p.lastHitAt = getLong(c, "last_hit_at");
        p.lastMissAt = getLong(c, "last_miss_at");
        p.failCount = getInt(c, "fail_count");
        p.recoverCount = getInt(c, "recover_count");
        p.avgFinalP = getDouble(c, "avg_final_p");
        p.avgEdge = getDouble(c, "avg_edge");
        p.avgDeepScore = getDouble(c, "avg_deep_score");
        p.updatedAt = getLong(c, "updated_at");

        setOptionalStringField(p, "stateKey", getString(c, "state_key"));
        setOptionalDoubleField(p, "runtimeProfileScore", getDouble(c, "runtime_profile_score"));
        setOptionalDoubleField(p, "teacherMatrixScore", getDouble(c, "teacher_matrix_score"));

        return p;
    }

    private Models.AttributionResult readAttributionResult(Cursor c) {
        Models.AttributionResult r = new Models.AttributionResult();

        r.attributionId = getString(c, "attribution_id");
        r.targetIssue = getString(c, "target_issue");
        r.hit = getInt(c, "hit");
        r.mainReasonCode = getString(c, "main_reason_code");
        r.mainReasonText = getString(c, "main_reason_text");
        r.confidence = getDouble(c, "confidence");
        r.shouldReward = getInt(c, "should_reward");
        r.shouldPunish = getInt(c, "should_punish");
        r.affectedModule = getString(c, "affected_module");
        r.profileKey = getString(c, "profile_key");
        r.positionImpact = getDouble(c, "position_impact");
        r.numberImpact = getDouble(c, "number_impact");
        r.cycleImpact = getDouble(c, "cycle_impact");
        r.transitionImpact = getDouble(c, "transition_impact");
        r.calibrationImpact = getDouble(c, "calibration_impact");

        setOptionalDoubleField(r, "teacherMatrixImpact", getDouble(c, "teacher_matrix_impact"));

        r.learningText = getString(c, "learning_text");
        r.createdAt = getLong(c, "created_at");

        return r;
    }

    private Models.PromotionCandidate readPromotionCandidate(Cursor c) {
        Models.PromotionCandidate p = new Models.PromotionCandidate();

        p.promotionKey = getString(c, "promotion_key");
        p.profileKey = getString(c, "profile_key");
        p.status = getString(c, "status");
        p.sampleCount = getInt(c, "sample_count");
        p.hitCount = getInt(c, "hit_count");
        p.hitRate = getDouble(c, "hit_rate");
        p.recentSample = getInt(c, "recent_sample");
        p.recentHit = getInt(c, "recent_hit");
        p.recentHitRate = getDouble(c, "recent_hit_rate");
        p.calibrationError = getDouble(c, "calibration_error");
        p.biasScore = getDouble(c, "bias_score");
        p.aPlusScore = getDouble(c, "a_plus_score");

        setOptionalDoubleField(p, "teacherMatrixScore", getDouble(c, "teacher_matrix_score"));

        p.promoteReason = getString(c, "promote_reason");
        p.rejectReason = getString(c, "reject_reason");
        p.updatedAt = getLong(c, "updated_at");

        return p;
    }

    private Models.ManualBacktestTask readManualBacktestTask(Cursor c) {
        Models.ManualBacktestTask t = new Models.ManualBacktestTask();

        t.taskId = getString(c, "task_id");
        t.mode = getString(c, "mode");
        t.status = getString(c, "status");
        t.progress = getInt(c, "progress");
        t.windowSize = getInt(c, "window_size");
        t.startIndex = getInt(c, "start_index");
        t.endIndex = getInt(c, "end_index");
        t.minTrain = getInt(c, "min_train");
        t.maxTest = getInt(c, "max_test");
        t.topKeep = getInt(c, "top_keep");
        t.totalSteps = getInt(c, "total_steps");
        t.doneSteps = getInt(c, "done_steps");
        t.startedAt = getLong(c, "started_at");
        t.finishedAt = getLong(c, "finished_at");
        t.summary = getString(c, "summary");
        t.error = getString(c, "error");

        return t;
    }

    private Models.ProbabilityAuditBucket readProbabilityAuditBucket(Cursor c) {
        Models.ProbabilityAuditBucket b = new Models.ProbabilityAuditBucket();

        b.bucketKey = getString(c, "bucket_key");
        b.bucketLow = getDouble(c, "bucket_low");
        b.bucketHigh = getDouble(c, "bucket_high");
        b.sampleCount = getInt(c, "sample_count");
        b.hitCount = getInt(c, "hit_count");
        b.hitRate = getDouble(c, "hit_rate");
        b.avgPredictedP = getDouble(c, "avg_predicted_p");
        b.calibrationError = getDouble(c, "calibration_error");
        b.brierScore = getDouble(c, "brier_score");
        b.recentSample = getInt(c, "recent_sample");
        b.recentHit = getInt(c, "recent_hit");
        b.recentHitRate = getDouble(c, "recent_hit_rate");
        b.recentError = getDouble(c, "recent_error");
        b.status = getString(c, "status");
        b.updatedAt = getLong(c, "updated_at");

        return b;
    }

    private String buildBacktestSummaryText(Models.BacktestReport r) {
        if (r == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("WalkForward");

        if (r.window > 0) {
            sb.append(" W");
            sb.append(r.window);
        }

        sb.append("：出手=");
        sb.append(r.betCount);
        sb.append("/");
        sb.append(r.totalCount);
        sb.append("，频率=");
        sb.append(percent(r.frequency));
        sb.append("，胜率=");
        sb.append(percent(r.winRate > 0.0d ? r.winRate : r.hitRate));
        sb.append("，盈亏=");
        sb.append(formatMoney(r.pnl));
        sb.append("，最大回撤=");
        sb.append(formatMoney(r.maxDrawdown));

        String tm = getState("teacher_matrix_summary", "");

        if (tm != null && tm.length() > 0) {
            sb.append("｜");
            sb.append(tm);
        }

        sb.append("｜说明=回测/教师/画像只做后台反推和字段沉淀，实时是否出手由主链最终门决定");

        return sb.toString();
    }

    private String bucketKey(double rawP) {
        rawP = AppConfig.clampProbability(rawP);

        if (rawP < 0.320d) return "P00_32";
        if (rawP < 0.340d) return "P32_34";
        if (rawP < 0.360d) return "P34_36";
        if (rawP < 0.380d) return "P36_38";
        if (rawP < 0.400d) return "P38_40";
        if (rawP < 0.420d) return "P40_42";
        if (rawP < 0.440d) return "P42_44";
        if (rawP < 0.460d) return "P44_46";
        if (rawP < 0.480d) return "P46_48";
        return "P48_PLUS";
    }

    private double bucketRawMin(String bucket, double rawP) {
        if ("P00_32".equals(bucket)) return 0.000d;
        if ("P32_34".equals(bucket)) return 0.320d;
        if ("P34_36".equals(bucket)) return 0.340d;
        if ("P36_38".equals(bucket)) return 0.360d;
        if ("P38_40".equals(bucket)) return 0.380d;
        if ("P40_42".equals(bucket)) return 0.400d;
        if ("P42_44".equals(bucket)) return 0.420d;
        if ("P44_46".equals(bucket)) return 0.440d;
        if ("P46_48".equals(bucket)) return 0.460d;
        if ("P48_PLUS".equals(bucket)) return 0.480d;
        return rawP;
    }

    private double bucketRawMax(String bucket, double rawP) {
        if ("P00_32".equals(bucket)) return 0.320d;
        if ("P32_34".equals(bucket)) return 0.340d;
        if ("P34_36".equals(bucket)) return 0.360d;
        if ("P36_38".equals(bucket)) return 0.380d;
        if ("P38_40".equals(bucket)) return 0.400d;
        if ("P40_42".equals(bucket)) return 0.420d;
        if ("P42_44".equals(bucket)) return 0.440d;
        if ("P44_46".equals(bucket)) return 0.460d;
        if ("P46_48".equals(bucket)) return 0.480d;
        if ("P48_PLUS".equals(bucket)) return 1.000d;
        return rawP;
    }

    private String auditBucketKey(double p) {
        p = AppConfig.clampProbability(p);

        if (p < 0.050d) return "A00_05";
        if (p < 0.100d) return "A05_10";
        if (p < 0.150d) return "A10_15";
        if (p < 0.200d) return "A15_20";
        if (p < 0.250d) return "A20_25";
        if (p < 0.300d) return "A25_30";
        if (p < 0.320d) return "A30_32";
        if (p < 0.340d) return "A32_34";
        if (p < 0.360d) return "A34_36";
        if (p < 0.380d) return "A36_38";
        if (p < 0.400d) return "A38_40";
        if (p < 0.420d) return "A40_42";
        if (p < 0.440d) return "A42_44";
        if (p < 0.460d) return "A44_46";
        if (p < 0.480d) return "A46_48";
        if (p < 0.500d) return "A48_50";
        if (p < 0.550d) return "A50_55";
        if (p < 0.600d) return "A55_60";
        if (p < 0.700d) return "A60_70";
        return "A70_PLUS";
    }

    private double auditBucketLow(String key, double p) {
        if ("A00_05".equals(key)) return 0.000d;
        if ("A05_10".equals(key)) return 0.050d;
        if ("A10_15".equals(key)) return 0.100d;
        if ("A15_20".equals(key)) return 0.150d;
        if ("A20_25".equals(key)) return 0.200d;
        if ("A25_30".equals(key)) return 0.250d;
        if ("A30_32".equals(key)) return 0.300d;
        if ("A32_34".equals(key)) return 0.320d;
        if ("A34_36".equals(key)) return 0.340d;
        if ("A36_38".equals(key)) return 0.360d;
        if ("A38_40".equals(key)) return 0.380d;
        if ("A40_42".equals(key)) return 0.400d;
        if ("A42_44".equals(key)) return 0.420d;
        if ("A44_46".equals(key)) return 0.440d;
        if ("A46_48".equals(key)) return 0.460d;
        if ("A48_50".equals(key)) return 0.480d;
        if ("A50_55".equals(key)) return 0.500d;
        if ("A55_60".equals(key)) return 0.550d;
        if ("A60_70".equals(key)) return 0.600d;
        if ("A70_PLUS".equals(key)) return 0.700d;
        return p;
    }

    private double auditBucketHigh(String key, double p) {
        if ("A00_05".equals(key)) return 0.050d;
        if ("A05_10".equals(key)) return 0.100d;
        if ("A10_15".equals(key)) return 0.150d;
        if ("A15_20".equals(key)) return 0.200d;
        if ("A20_25".equals(key)) return 0.250d;
        if ("A25_30".equals(key)) return 0.300d;
        if ("A30_32".equals(key)) return 0.320d;
        if ("A32_34".equals(key)) return 0.340d;
        if ("A34_36".equals(key)) return 0.360d;
        if ("A36_38".equals(key)) return 0.380d;
        if ("A38_40".equals(key)) return 0.400d;
        if ("A40_42".equals(key)) return 0.420d;
        if ("A42_44".equals(key)) return 0.440d;
        if ("A44_46".equals(key)) return 0.460d;
        if ("A46_48".equals(key)) return 0.480d;
        if ("A48_50".equals(key)) return 0.500d;
        if ("A50_55".equals(key)) return 0.550d;
        if ("A55_60".equals(key)) return 0.600d;
        if ("A60_70".equals(key)) return 0.700d;
        if ("A70_PLUS".equals(key)) return 1.000d;
        return p;
    }

    private double brier(double predictedP, boolean hit) {
        double y = hit ? 1.0d : 0.0d;
        double d = predictedP - y;
        return d * d;
    }

    private double rollingAverage(double oldAverage, int newCount, double value) {
        if (newCount <= 1) {
            return value;
        }

        return ((oldAverage * (newCount - 1)) + value) / newCount;
    }

    private double normalizeRisk(double value) {
        if (value < 0.0d) {
            return 0.0d;
        }

        if (value > 1.0d) {
            return 1.0d;
        }

        return value;
    }

    private double limitAdjustLocal(double value, double maxAbs) {
        if (maxAbs < 0.0d) {
            maxAbs = -maxAbs;
        }

        if (value > maxAbs) {
            return maxAbs;
        }

        if (value < -maxAbs) {
            return -maxAbs;
        }

        return value;
    }

    private double confidenceFromSample(int sample) {
        if (sample <= 0) {
            return 0.0d;
        }

        if (sample >= 80) {
            return 1.0d;
        }

        return ((double) sample) / 80.0d;
    }

    private String percent(double value) {
        int v = (int) Math.round(AppConfig.clampProbability(value) * 100.0d);
        return String.valueOf(v) + "%";
    }

    private String signedPercent(double value) {
        String sign = value >= 0.0d ? "+" : "";
        return sign + String.format(Locale.US, "%.2f", Double.valueOf(value * 100.0d)) + "%";
    }

    private String formatMoney(double value) {
        String sign = value >= 0.0d ? "+" : "";
        return sign + String.format(Locale.US, "%.2f", Double.valueOf(value));
    }

    private String probColumn(int n) {
        return n < 10 ? "p0" + n : "p10";
    }

    private String markovColumn(int n) {
        return n < 10 ? "m0" + n : "m10";
    }

    private long todayStartMs() {
        try {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            return c.getTimeInMillis();
        } catch (Throwable e) {
            return 0L;
        }
    }

    private String todayIssuePrefix() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.US);
            return sdf.format(new Date(System.currentTimeMillis()));
        } catch (Throwable e) {
            return "";
        }
    }

    private String formatTime(long time) {
        if (time <= 0L) {
            return "";
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.US);
            return sdf.format(new Date(time));
        } catch (Throwable e) {
            return "";
        }
    }

    private long parseIssueLong(String issue) {
        if (issue == null || issue.length() == 0) {
            return 0L;
        }

        try {
            return Long.parseLong(issue);
        } catch (Throwable e) {
            return 0L;
        }
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private String firstNonEmpty(String a, String b) {
        if (a != null && a.length() > 0) {
            return a;
        }

        if (b != null) {
            return b;
        }

        return "";
    }

    private void appendSep(StringBuilder sb) {
        if (sb != null && sb.length() > 0) {
            sb.append("｜");
        }
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }

    private boolean maskContainsNumber(int mask, int number) {
        return AppConfig.maskContainsNumber(mask, number);
    }

    private String safeStateKeyFromDecision(Models.Decision d) {
        if (d == null) {
            return "";
        }

        if (d.stateKey != null && d.stateKey.length() > 0) {
            return d.stateKey;
        }

        String state = safeString(d.stateMode);
        String shape = safeString(d.shapeKey);

        if (state.length() == 0 && shape.length() == 0) {
            state = "DECISION";
        }

        return buildStateKey(state, shape, d.position, d.driftRisk, d.breakDepth, d.recoveryScore);
    }

    private String safeTradeStateKey(Models.Trade t) {
        if (t == null) {
            return "";
        }

        if (t.stateKey != null && t.stateKey.length() > 0) {
            return t.stateKey;
        }

        String state = safeString(t.stateMode);
        String shape = safeString(t.shapeKey);

        if (state.length() == 0 && shape.length() == 0) {
            state = "TRADE";
        }

        return buildStateKey(state, shape, t.position, t.driftRisk, t.breakDepth, t.recoveryScore);
    }

    private String safeStateKeyFromCandidate(Models.ComboCandidate c) {
        if (c == null) {
            return "";
        }

        if (c.stateKey != null && c.stateKey.length() > 0) {
            return c.stateKey;
        }

        String state = safeString(c.stateMode);
        String shape = safeString(c.shapeKey);

        if (state.length() == 0 && shape.length() == 0) {
            state = "CANDIDATE";
        }

        return buildStateKey(state, shape, c.position, c.driftRisk, c.breakDepth, c.recoveryScore);
    }

    private String buildStateKey(
            String stateMode,
            String shapeKey,
            int position,
            double driftRisk,
            double breakDepth,
            double recoveryScore) {
        StringBuilder sb = new StringBuilder();

        sb.append("P");
        sb.append(position);

        if (stateMode != null && stateMode.length() > 0) {
            sb.append("|S=");
            sb.append(cleanKeyPart(stateMode));
        }

        if (shapeKey != null && shapeKey.length() > 0) {
            sb.append("|K=");
            sb.append(cleanKeyPart(shapeKey));
        }

        sb.append("|");
        sb.append(bucketText("DRIFT", driftRisk));
        sb.append("|");
        sb.append(bucketText("BREAK", breakDepth));
        sb.append("|");
        sb.append(bucketText("RECOVER", recoveryScore));

        return sb.toString();
    }

    private String bucketText(String prefix, double v) {
        if (prefix == null || prefix.length() == 0) {
            prefix = "B";
        }

        if (v <= 0.0d) {
            return prefix + "_0";
        }

        if (v < 0.20d) {
            return prefix + "_LOW";
        }

        if (v < 0.45d) {
            return prefix + "_MID";
        }

        if (v < 0.70d) {
            return prefix + "_HIGH";
        }

        return prefix + "_EXTREME";
    }

    private String cleanKeyPart(String s) {
        if (s == null) {
            return "";
        }

        String out = s.replace('|', '_');
        out = out.replace(' ', '_');
        out = out.replace('\n', '_');
        out = out.replace('\r', '_');
        out = out.replace('\t', '_');

        if (out.length() > 80) {
            out = out.substring(0, 80);
        }

        return out;
    }

    private String buildLearningCaseKey(
            String taskKey, String segmentKey, String targetIssue, int position) {
        StringBuilder sb = new StringBuilder();

        sb.append(safeString(taskKey));
        sb.append("_");
        sb.append(safeString(segmentKey));
        sb.append("_");
        sb.append(safeString(targetIssue));
        sb.append("_P");
        sb.append(position);

        return sb.toString();
    }

    private String buildRuntimeProfileKey(String stateKey, int position, int k3Mask) {
        return cleanKeyPart(stateKey) + "|P" + position + "|K3_" + k3Mask;
    }

    private String buildStateNumberProfileKey(String stateKey, int position, int number) {
        return cleanKeyPart(stateKey) + "|P" + position + "|N" + number;
    }

    private String buildTeacherMatrixSampleKey(
            String sourceType, String baseIssue, String targetIssue, int position, int number) {
        return cleanKeyPart(sourceType)
                + "|"
                + safeString(baseIssue)
                + "|"
                + safeString(targetIssue)
                + "|P"
                + position
                + "|N"
                + number;
    }

    private String buildTeacherMatrixProfileKey(String stateKey, int position, int number) {
        return cleanKeyPart(stateKey) + "|P" + position + "|N" + number;
    }

    private String buildTeacherMatrixComboKey(String stateKey, int position, int k3Mask) {
        return cleanKeyPart(stateKey) + "|P" + position + "|K3_" + k3Mask;
    }

    private String buildRuntimeProfileSummaryText(String stateKey, int position, int k3Mask) {
        StringBuilder sb = new StringBuilder();

        sb.append("状态画像：");
        sb.append(stateKey);
        sb.append("｜位置=");
        sb.append(position);
        sb.append("｜K3=");
        sb.append(k3Mask);
        sb.append("｜后台学习已更新，实时分析可读取该组合表现。");
        sb.append("｜说明=画像字段参与主链计算前由Probability/DynamicGate判断，DbStore不直接救援BET。");

        return sb.toString();
    }

    private String buildNumberProfileWhyText(
            String stateKey, int position, int number, boolean actual, boolean inK3) {
        StringBuilder sb = new StringBuilder();

        sb.append("号码画像：状态=");
        sb.append(stateKey);
        sb.append("｜位置=");
        sb.append(position);
        sb.append("｜号码=");
        sb.append(number);

        if (actual && inK3) {
            sb.append("｜本次在预测K3内且实际开出，增强该状态下号码权重。");
        } else if (actual) {
            sb.append("｜本次实际开出但不在预测K3内，属于漏判样本，下次提高相似号码关注。");
        } else if (inK3) {
            sb.append("｜本次在预测K3内但未开出，降低该状态下追随权重。");
        } else {
            sb.append("｜本次未开出，作为状态背景样本。");
        }

        sb.append("｜说明=号码画像不直接创建BET。");

        return sb.toString();
    }

    private long insertFiltered(
            SQLiteDatabase db, String table, ContentValues source, int conflictAlgorithm) {
        if(DbHelper.TABLE_TRADES.equals(table))
            throw new IllegalStateException("CP4_TRADE_INSERT_REQUIRES_CANONICAL_OWNER");
        if (db == null || table == null || table.length() == 0 || source == null) {
            return -1L;
        }

        ContentValues filtered = filterValuesForTable(db, table, source);

        if (filtered.size() == 0) {
            return -1L;
        }

        RetentionAggregateCapacityCP12.checkAdmission(db,table,filtered);
        if (conflictAlgorithm == SQLiteDatabase.CONFLICT_NONE) {
            return db.insert(table, null, filtered);
        }

        return db.insertWithOnConflict(table, null, filtered, conflictAlgorithm);
    }

    private int updateFiltered(
            SQLiteDatabase db, String table, ContentValues source, String where, String[] args) {
        if(DbHelper.TABLE_TRADES.equals(table))
            throw new IllegalStateException("CP4_TRADE_UPDATE_REQUIRES_SETTLEMENT_OWNER");
        if (db == null || table == null || table.length() == 0 || source == null) {
            return 0;
        }

        ContentValues filtered = filterValuesForTable(db, table, source);

        if (filtered.size() == 0) {
            return 0;
        }

        return db.update(table, filtered, where, args);
    }

    private ContentValues filterValuesForTable(
            SQLiteDatabase db, String table, ContentValues source) {
        ContentValues out = new ContentValues();

        if (db == null || table == null || source == null) {
            return out;
        }

        HashSet<String> columns = getTableColumns(db, table);

        if (columns == null || columns.size() == 0) {
            return source;
        }

        for (Map.Entry<String, Object> entry : source.valueSet()) {
            if (entry == null) {
                continue;
            }

            String key = entry.getKey();

            if (key == null || !columns.contains(key)) {
                continue;
            }

            putObject(out, key, entry.getValue());
        }

        return out;
    }

    private HashSet<String> getTableColumns(SQLiteDatabase db, String table) {
        synchronized (columnCache) {
            HashSet<String> cached = columnCache.get(table);

            if (cached != null) {
                return cached;
            }
        }

        HashSet<String> set = new HashSet<String>();
        Cursor c = null;

        try {
            c = db.rawQuery("PRAGMA table_info(" + table + ")", null);

            while (c.moveToNext()) {
                String name = getString(c, "name");

                if (name != null && name.length() > 0) {
                    set.add(name);
                }
            }
        } catch (Throwable e) {
            set.clear();
        } finally {
            closeCursor(c);
        }

        synchronized (columnCache) {
            columnCache.put(table, set);
        }

        return set;
    }

    private int getLearningCaseK3Mask(Models.LearningCase x) {
        if (x == null) {
            return 0;
        }

        int mask = x.predictedK3;

        if (Models.isValidK3Mask(mask)) {
            return mask;
        }

        int optional = getOptionalIntField(x, "k3Mask", 0);

        if (Models.isValidK3Mask(optional)) {
            return optional;
        }

        return mask;
    }

    private void setLearningCaseK3Mask(Models.LearningCase x, int mask) {
        if (x == null) {
            return;
        }

        x.predictedK3 = mask;
        setOptionalIntField(x, "k3Mask", mask);
    }

    private void readTeacherMatrixFieldsToObject(Cursor c, Object obj) {
        if (c == null || obj == null) {
            return;
        }

        setOptionalIntField(obj, "teacherMatrixSample", getInt(c, "teacher_matrix_sample"));
        setOptionalDoubleField(
                obj, "teacherMatrixHitRate", getDouble(c, "teacher_matrix_hit_rate"));
        setOptionalDoubleField(
                obj, "teacherMatrixConfidence", getDouble(c, "teacher_matrix_confidence"));
        setOptionalDoubleField(
                obj, "teacherMatrixProbAdjust", getDouble(c, "teacher_matrix_prob_adjust"));
        setOptionalDoubleField(
                obj, "teacherMatrixGateAdjust", getDouble(c, "teacher_matrix_gate_adjust"));
        setOptionalDoubleField(
                obj, "teacherMatrixRiskPenalty", getDouble(c, "teacher_matrix_risk_penalty"));
        setOptionalIntField(obj, "teacherMatrixApplied", getInt(c, "teacher_matrix_applied"));
        setOptionalStringField(obj, "teacherMatrixStatus", getString(c, "teacher_matrix_status"));
        setOptionalStringField(
                obj, "teacherMatrixProfileKey", getString(c, "teacher_matrix_profile_key"));
        setOptionalStringField(
                obj, "teacherMatrixComboKey", getString(c, "teacher_matrix_combo_key"));
        setOptionalStringField(obj, "teacherMatrixSummary", getString(c, "teacher_matrix_summary"));
    }

    private ContentValues cursorRowToContentValues(Cursor c) {
        ContentValues cv = new ContentValues();

        if (c == null) {
            return cv;
        }

        String[] names = c.getColumnNames();

        if (names == null) {
            return cv;
        }

        for (int i = 0; i < names.length; i++) {
            String name = names[i];

            if (name == null || name.length() == 0) {
                continue;
            }

            if (c.isNull(i)) {
                cv.putNull(name);
                continue;
            }

            try {
                int type = c.getType(i);

                if (type == Cursor.FIELD_TYPE_INTEGER) {
                    cv.put(name, Long.valueOf(c.getLong(i)));
                } else if (type == Cursor.FIELD_TYPE_FLOAT) {
                    cv.put(name, Double.valueOf(c.getDouble(i)));
                } else if (type == Cursor.FIELD_TYPE_BLOB) {
                    cv.put(name, c.getBlob(i));
                } else {
                    cv.put(name, c.getString(i));
                }
            } catch (Throwable e) {
                try {
                    cv.put(name, c.getString(i));
                } catch (Throwable ignored) {
                }
            }
        }

        return cv;
    }

    private double calcTeacherMatrixAvg(String column) {
        if (column == null || column.length() == 0) {
            return 0.0d;
        }

        if (!"hit_rate".equals(column)
                && !"confidence".equals(column)
                && !"prob_adjust".equals(column)
                && !"gate_adjust".equals(column)) {
            return 0.0d;
        }

        Cursor c = null;

        try {
            c =
                    readable()
                            .rawQuery(
                                    "SELECT AVG("
                                            + column
                                            + ") AS v FROM "
                                            + TABLE_TEACHER_MATRIX_PROFILE
                                            + " WHERE sample_count>0",
                                    null);

            if (c.moveToFirst()) {
                return getDouble(c, "v");
            }
        } catch (Throwable e) {
        } finally {
            closeCursor(c);
        }

        return 0.0d;
    }

    private String getCvString(ContentValues cv, String key, String fallback) {
        if (cv == null || key == null) {
            return fallback;
        }

        Object v = cv.get(key);

        if (v == null) {
            return fallback;
        }

        return String.valueOf(v);
    }

    private int getCvInt(ContentValues cv, String key, int fallback) {
        if (cv == null || key == null) {
            return fallback;
        }

        Object v = cv.get(key);

        if (v == null) {
            return fallback;
        }

        try {
            if (v instanceof Integer) {
                return ((Integer) v).intValue();
            }

            if (v instanceof Long) {
                return (int) ((Long) v).longValue();
            }

            if (v instanceof Number) {
                return ((Number) v).intValue();
            }

            return Integer.parseInt(String.valueOf(v));
        } catch (Throwable e) {
            return fallback;
        }
    }

    private long getCvLong(ContentValues cv, String key, long fallback) {
        if (cv == null || key == null) {
            return fallback;
        }

        Object v = cv.get(key);

        if (v == null) {
            return fallback;
        }

        try {
            if (v instanceof Long) {
                return ((Long) v).longValue();
            }

            if (v instanceof Integer) {
                return ((Integer) v).longValue();
            }

            if (v instanceof Number) {
                return ((Number) v).longValue();
            }

            return Long.parseLong(String.valueOf(v));
        } catch (Throwable e) {
            return fallback;
        }
    }

    private double getCvDouble(ContentValues cv, String key, double fallback) {
        if (cv == null || key == null) {
            return fallback;
        }

        Object v = cv.get(key);

        if (v == null) {
            return fallback;
        }

        try {
            if (v instanceof Double) {
                return ((Double) v).doubleValue();
            }

            if (v instanceof Float) {
                return ((Float) v).doubleValue();
            }

            if (v instanceof Number) {
                return ((Number) v).doubleValue();
            }

            return Double.parseDouble(String.valueOf(v));
        } catch (Throwable e) {
            return fallback;
        }
    }

    private int getOptionalIntField(Object obj, String fieldName, int fallback) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return fallback;
        }

        try {
            java.lang.reflect.Field f = obj.getClass().getField(fieldName);
            Object value = f.get(obj);

            if (value instanceof Integer) {
                return ((Integer) value).intValue();
            }

            if (value instanceof Long) {
                return (int) ((Long) value).longValue();
            }

            if (value instanceof Number) {
                return ((Number) value).intValue();
            }

            if (value != null) {
                return Integer.parseInt(String.valueOf(value));
            }
        } catch (Throwable e) {
        }

        return fallback;
    }

    private long getOptionalLongField(Object obj, String fieldName, long fallback) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return fallback;
        }

        try {
            java.lang.reflect.Field f = obj.getClass().getField(fieldName);
            Object value = f.get(obj);

            if (value instanceof Long) {
                return ((Long) value).longValue();
            }

            if (value instanceof Integer) {
                return ((Integer) value).longValue();
            }

            if (value instanceof Number) {
                return ((Number) value).longValue();
            }

            if (value != null) {
                return Long.parseLong(String.valueOf(value));
            }
        } catch (Throwable e) {
        }

        return fallback;
    }

    private double getOptionalDoubleField(Object obj, String fieldName, double fallback) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return fallback;
        }

        try {
            java.lang.reflect.Field f = obj.getClass().getField(fieldName);
            Object value = f.get(obj);

            if (value instanceof Double) {
                return ((Double) value).doubleValue();
            }

            if (value instanceof Float) {
                return ((Float) value).doubleValue();
            }

            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }

            if (value != null) {
                return Double.parseDouble(String.valueOf(value));
            }
        } catch (Throwable e) {
        }

        return fallback;
    }

    private String getOptionalStringField(Object obj, String fieldName, String fallback) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return fallback;
        }

        try {
            java.lang.reflect.Field f = obj.getClass().getField(fieldName);
            Object value = f.get(obj);

            if (value != null) {
                return String.valueOf(value);
            }
        } catch (Throwable e) {
        }

        return fallback;
    }

    private void setOptionalIntField(Object obj, String fieldName, int value) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return;
        }

        try {
            java.lang.reflect.Field f = obj.getClass().getField(fieldName);
            f.setInt(obj, value);
        } catch (Throwable e) {
            try {
                java.lang.reflect.Field f = obj.getClass().getField(fieldName);
                f.set(obj, Integer.valueOf(value));
            } catch (Throwable ignored) {
            }
        }
    }

    private void setOptionalLongField(Object obj, String fieldName, long value) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return;
        }

        try {
            java.lang.reflect.Field f = obj.getClass().getField(fieldName);
            f.setLong(obj, value);
        } catch (Throwable e) {
            try {
                java.lang.reflect.Field f = obj.getClass().getField(fieldName);
                f.set(obj, Long.valueOf(value));
            } catch (Throwable ignored) {
            }
        }
    }

    private void setOptionalDoubleField(Object obj, String fieldName, double value) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return;
        }

        try {
            java.lang.reflect.Field f = obj.getClass().getField(fieldName);
            f.setDouble(obj, value);
        } catch (Throwable e) {
            try {
                java.lang.reflect.Field f = obj.getClass().getField(fieldName);
                f.set(obj, Double.valueOf(value));
            } catch (Throwable ignored) {
            }
        }
    }

    private void setOptionalStringField(Object obj, String fieldName, String value) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return;
        }

        try {
            java.lang.reflect.Field f = obj.getClass().getField(fieldName);
            f.set(obj, value == null ? "" : value);
        } catch (Throwable e) {
        }
    }

    private void putObject(ContentValues cv, String key, Object value) {
        if (cv == null || key == null) {
            return;
        }

        if (value == null) {
            cv.putNull(key);
        } else if (value instanceof String) {
            cv.put(key, (String) value);
        } else if (value instanceof Integer) {
            cv.put(key, (Integer) value);
        } else if (value instanceof Long) {
            cv.put(key, (Long) value);
        } else if (value instanceof Double) {
            cv.put(key, (Double) value);
        } else if (value instanceof Float) {
            cv.put(key, (Float) value);
        } else if (value instanceof Short) {
            cv.put(key, (Short) value);
        } else if (value instanceof Boolean) {
            cv.put(key, ((Boolean) value).booleanValue() ? 1 : 0);
        } else if (value instanceof byte[]) {
            cv.put(key, (byte[]) value);
        } else {
            cv.put(key, String.valueOf(value));
        }
    }

    private String getStringFromCv(ContentValues cv, String key) {
        if (cv == null || key == null) {
            return "";
        }

        Object value = cv.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private int countTable(String table, String where, String[] args) {
        return countTable(readable(), table, where, args);
    }

    private int countTable(SQLiteDatabase db, String table, String where, String[] args) {
        if (db == null || table == null || table.length() == 0) {
            return 0;
        }

        Cursor c = null;

        try {
            c = db.query(table, new String[] {"COUNT(*) AS c"}, where, args, null, null, null);

            if (c.moveToFirst()) {
                return getInt(c, "c");
            }
        } catch (Throwable e) {
        } finally {
            closeCursor(c);
        }

        return 0;
    }

    private int getInt(Cursor c, String name) {
        if (c == null || name == null) {
            return 0;
        }

        int index = c.getColumnIndex(name);

        if (index < 0 || c.isNull(index)) {
            return 0;
        }

        return c.getInt(index);
    }

    private int getIntAny(Cursor c, String a, String b) {
        int v = getInt(c, a);

        if (v != 0) {
            return v;
        }

        return getInt(c, b);
    }

    private long getLong(Cursor c, String name) {
        if (c == null || name == null) {
            return 0L;
        }

        int index = c.getColumnIndex(name);

        if (index < 0 || c.isNull(index)) {
            return 0L;
        }

        return c.getLong(index);
    }

    private double getDouble(Cursor c, String name) {
        if (c == null || name == null) {
            return 0.0d;
        }

        int index = c.getColumnIndex(name);

        if (index < 0 || c.isNull(index)) {
            return 0.0d;
        }

        return c.getDouble(index);
    }

    private String getString(Cursor c, String name) {
        if (c == null || name == null) {
            return "";
        }

        int index = c.getColumnIndex(name);

        if (index < 0 || c.isNull(index)) {
            return "";
        }

        return c.getString(index);
    }

    private void closeCursor(Cursor c) {
        if (c != null) {
            try {
                c.close();
            } catch (Throwable e) {
            }
        }
    }

    private double pnlForHitLocal(boolean hit) {
        if (hit) {
            return readAppDoubleLocal(
                    "WIN_PNL_K3",
                    readAppDoubleLocal(
                            "WIN_NET_PROFIT", readAppDoubleLocal("BET_WIN_NET_PROFIT", 6.9d)));
        }

        return readAppDoubleLocal(
                "MISS_PNL_K3",
                readAppDoubleLocal(
                        "LOSE_NET_PROFIT", readAppDoubleLocal("BET_LOSE_NET_PROFIT", -3.0d)));
    }

    private double readAppDoubleLocal(String fieldName, double fallback) {
        if (fieldName == null || fieldName.length() == 0) {
            return fallback;
        }

        try {
            java.lang.reflect.Field f = AppConfig.class.getField(fieldName);
            Object v = f.get(null);

            if (v instanceof Double) {
                return ((Double) v).doubleValue();
            }

            if (v instanceof Float) {
                return ((Float) v).doubleValue();
            }

            if (v instanceof Integer) {
                return ((Integer) v).doubleValue();
            }

            if (v instanceof Long) {
                return ((Long) v).doubleValue();
            }

            if (v instanceof Number) {
                return ((Number) v).doubleValue();
            }

            if (v != null) {
                return Double.parseDouble(String.valueOf(v));
            }
        } catch (Throwable e) {
        }

        return fallback;
    }

    // =========================
    // V20 Dynamic Cause Chain DbStore Extension
    // 追加位置：DbStore.java 最后一个 } 之前
    // 作用：只做 V20 表保障、读写、审计、反推落库，不直接救援 BET，不绕过 DecisionGateEngine。
    // =========================

    private static final String V20_TABLE_PRE_DRAW_FEATURE = "pre_draw_feature";
    private static final String V20_TABLE_PRE_DRAW_NUMBER_SAMPLE = "pre_draw_number_sample";
    private static final String V20_TABLE_FULL_CANDIDATE_SCAN = "full_candidate_scan";
    private static final String V20_TABLE_CANDIDATE_AUDIT = "candidate_audit";
    private static final String V20_TABLE_CAUSAL_FACTOR_MATRIX = "causal_factor_matrix";
    private static final String V20_TABLE_POSITION_CAUSE_MATRIX = "position_cause_matrix";
    private static final String V20_TABLE_TEACHER_NUMBER_MATRIX = "teacher_number_matrix";
    private static final String V20_TABLE_COUNTERFACTUAL_RANK = "counterfactual_rank";
    private static final String V20_TABLE_THIRD_GUARD_MATRIX = "third_guard_matrix";
    private static final String V20_TABLE_RULE_HEALTH_MATRIX = "rule_health_matrix";
    private static final String V20_TABLE_SHADOW_HEALTH_GATE = "shadow_health_gate";
    private static final String V20_TABLE_FALSE_STRONG_FREEZE = "false_strong_freeze";
    private static final String V20_TABLE_SEED_CAUSE_MATRIX = "seed_cause_matrix";
    private static final String V20_TABLE_DYNAMIC_CAUSE_CHAIN = "dynamic_cause_chain";
    private static final String V20_TABLE_DECISION_AUDIT = "decision_audit";
    private static final String V20_TABLE_REAL_TRADE_LEDGER = "real_trade_ledger";

    private static final String V20_RECORD_KEY = "record_key";

    public void ensureV20CauseChainTablesNow() {
        try {
            SQLiteDatabase db = writable();
            ensureV20CauseChainReady(db);
            clearColumnCache();
        } catch (Throwable e) {
        }
    }

    private void ensureV20CauseChainReady(SQLiteDatabase db) {
        if (db == null) {
            return;
        }

        try {
            ensureV20BaseTables(db);
            ensureV20CommonColumns(db);
            ensureV20Indexes(db);
        } catch (Throwable e) {
        }
    }

    private void ensureV20BaseTables(SQLiteDatabase db) {
        if (db == null) {
            return;
        }

        String[] tables =
                new String[] {
                    V20_TABLE_PRE_DRAW_FEATURE,
                    V20_TABLE_PRE_DRAW_NUMBER_SAMPLE,
                    V20_TABLE_FULL_CANDIDATE_SCAN,
                    V20_TABLE_CANDIDATE_AUDIT,
                    V20_TABLE_CAUSAL_FACTOR_MATRIX,
                    V20_TABLE_POSITION_CAUSE_MATRIX,
                    V20_TABLE_TEACHER_NUMBER_MATRIX,
                    V20_TABLE_COUNTERFACTUAL_RANK,
                    V20_TABLE_THIRD_GUARD_MATRIX,
                    V20_TABLE_RULE_HEALTH_MATRIX,
                    V20_TABLE_SHADOW_HEALTH_GATE,
                    V20_TABLE_FALSE_STRONG_FREEZE,
                    V20_TABLE_SEED_CAUSE_MATRIX,
                    V20_TABLE_DYNAMIC_CAUSE_CHAIN,
                    V20_TABLE_DECISION_AUDIT,
                    V20_TABLE_REAL_TRADE_LEDGER
                };

        for (int i = 0; i < tables.length; i++) {
            createV20GenericTable(db, tables[i]);
        }
    }

    private void createV20GenericTable(SQLiteDatabase db, String table) {
        if (db == null || table == null || table.length() == 0) {
            return;
        }

        try {
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS "
                            + table
                            + " ("
                            + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "record_key TEXT UNIQUE,"
                            + "base_issue TEXT,"
                            + "target_issue TEXT,"
                            + "created_at INTEGER DEFAULT 0,"
                            + "updated_at INTEGER DEFAULT 0,"
                            + "position INTEGER DEFAULT 0,"
                            + "number INTEGER DEFAULT 0,"
                            + "state_key TEXT,"
                            + "source_key TEXT,"
                            + "rule_key TEXT,"
                            + "same_class_key TEXT,"
                            + "false_strong_key TEXT,"
                            + "status TEXT,"
                            + "summary_text TEXT,"
                            + "reason_text TEXT,"
                            + "ext_json TEXT"
                            + ")");
        } catch (Throwable e) {
        }
    }

    private void ensureV20CommonColumns(SQLiteDatabase db) {
        if (db == null) {
            return;
        }

        String[] tables =
                new String[] {
                    V20_TABLE_PRE_DRAW_FEATURE,
                    V20_TABLE_PRE_DRAW_NUMBER_SAMPLE,
                    V20_TABLE_FULL_CANDIDATE_SCAN,
                    V20_TABLE_CANDIDATE_AUDIT,
                    V20_TABLE_CAUSAL_FACTOR_MATRIX,
                    V20_TABLE_POSITION_CAUSE_MATRIX,
                    V20_TABLE_TEACHER_NUMBER_MATRIX,
                    V20_TABLE_COUNTERFACTUAL_RANK,
                    V20_TABLE_THIRD_GUARD_MATRIX,
                    V20_TABLE_RULE_HEALTH_MATRIX,
                    V20_TABLE_SHADOW_HEALTH_GATE,
                    V20_TABLE_FALSE_STRONG_FREEZE,
                    V20_TABLE_SEED_CAUSE_MATRIX,
                    V20_TABLE_DYNAMIC_CAUSE_CHAIN,
                    V20_TABLE_DECISION_AUDIT,
                    V20_TABLE_REAL_TRADE_LEDGER
                };

        for (int i = 0; i < tables.length; i++) {
            ensureV20ColumnsForTable(db, tables[i]);
        }
    }

    private void ensureV20ColumnsForTable(SQLiteDatabase db, String table) {
        if (db == null || table == null || table.length() == 0) {
            return;
        }

        addColumnIfMissing(db, table, "source_issue", "TEXT");
        addColumnIfMissing(db, table, "decision_id", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "trade_id", "INTEGER DEFAULT 0");

        addColumnIfMissing(db, table, "n1", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "n2", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "n3", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "k3_mask", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "actual_number", "INTEGER DEFAULT 0");

        addColumnIfMissing(db, table, "rank_no", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "rank_group", "TEXT");
        addColumnIfMissing(db, table, "grade", "TEXT");
        addColumnIfMissing(db, table, "action", "TEXT");
        addColumnIfMissing(db, table, "result", "TEXT");
        addColumnIfMissing(db, table, "settled", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "hit", "INTEGER DEFAULT 0");

        addColumnIfMissing(db, table, "final_p", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "dynamic_gate", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "edge", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "confidence", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "cause_score", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "final_cause_score", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "risk_score", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "health_score", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "support_score", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "prob_adjust", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "gate_adjust", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "risk_penalty", "REAL DEFAULT 0");

        addColumnIfMissing(db, table, "trend_state", "TEXT");
        addColumnIfMissing(db, table, "shape_state", "TEXT");
        addColumnIfMissing(db, table, "entropy_state", "TEXT");
        addColumnIfMissing(db, table, "drift_state", "TEXT");
        addColumnIfMissing(db, table, "cycle_state", "TEXT");
        addColumnIfMissing(db, table, "flow_state", "TEXT");
        addColumnIfMissing(db, table, "life_state", "TEXT");
        addColumnIfMissing(db, table, "hot_cold_state", "TEXT");
        addColumnIfMissing(db, table, "rank_truth_mode", "TEXT");
        addColumnIfMissing(db, table, "role_type", "TEXT");

        addColumnIfMissing(db, table, "history_count", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "window_12", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "window_24", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "window_48", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "window_96", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "window_180", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "window_360", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "window_720", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "state_key_12", "TEXT");
        addColumnIfMissing(db, table, "state_key_24", "TEXT");
        addColumnIfMissing(db, table, "state_key_48", "TEXT");
        addColumnIfMissing(db, table, "state_key_96", "TEXT");
        addColumnIfMissing(db, table, "state_key_360", "TEXT");
        addColumnIfMissing(db, table, "window_state_key", "TEXT");
        addColumnIfMissing(db, table, "hot_cold_slope", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "hot_cold_bucket", "TEXT");
        addColumnIfMissing(db, table, "omission_bucket", "TEXT");
        addColumnIfMissing(db, table, "density_bucket", "TEXT");
        addColumnIfMissing(db, table, "repeat_bucket", "TEXT");
        addColumnIfMissing(db, table, "odd_even_bias", "TEXT");
        addColumnIfMissing(db, table, "high_low_bias", "TEXT");
        addColumnIfMissing(db, table, "neighbor_tail_state", "TEXT");
        addColumnIfMissing(db, table, "position_crowd_state", "TEXT");
        addColumnIfMissing(db, table, "cross_position_sync", "TEXT");
        addColumnIfMissing(db, table, "cross_position_squeeze", "TEXT");
        addColumnIfMissing(db, table, "recent_lose_streak", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "timing_shift", "TEXT");
        addColumnIfMissing(db, table, "feature_distance", "REAL DEFAULT 1");
        addColumnIfMissing(db, table, "similar_window_sample", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "similar_window_hit", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "similar_window_hit_rate", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "similar_window_recent_rate", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "sample_trust", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "recent_effectiveness", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "evidence_contribution", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "counter_contribution", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "evidence_support_text", "TEXT");
        addColumnIfMissing(db, table, "counter_evidence_text", "TEXT");
        addColumnIfMissing(db, table, "action_advice", "TEXT");
        addColumnIfMissing(db, table, "replay_key", "TEXT");
        addColumnIfMissing(db, table, "calculation_version", "TEXT");
        addColumnIfMissing(db, table, "no_future_leak_flag", "INTEGER DEFAULT 1");
        addColumnIfMissing(db, table, "board_risk", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "missing_gap", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "recent_count", "INTEGER DEFAULT 0");

        addColumnIfMissing(db, table, "sample_count", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "hit_count", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "miss_count", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "positive_count", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "negative_count", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "with_sample", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "with_hit", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "without_sample", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "without_hit", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "recent_sample", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "recent_hit", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "recent_miss", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "shadow_sample", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "shadow_hit", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "real_sample", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "actual_hit_count", "INTEGER DEFAULT 0");

        addColumnIfMissing(db, table, "hit_rate", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "recent_hit_rate", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "shadow_hit_rate", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "contrast_hit_rate", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "causal_edge", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "lcb", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "decay_risk", "REAL DEFAULT 0");

        addColumnIfMissing(db, table, "position_edge", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "number_teacher_edge", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "teacher_edge", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "rank_counter_edge", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "third_guard_net", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "source_health_edge", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "state_health_edge", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "cycle_health_edge", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "seed_similarity", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "false_seed_similarity", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "shadow_health", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "false_strong_risk", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "same_class_lose_risk", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "guard_drag_risk", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "position_wrong_risk", "REAL DEFAULT 0");

        addColumnIfMissing(db, table, "save_count", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "drag_count", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "save_rate", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "drag_rate", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "net_edge", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "overtake_rate", "REAL DEFAULT 0");
        addColumnIfMissing(db, table, "false_strong_rate", "REAL DEFAULT 0");

        addColumnIfMissing(db, table, "max_lose", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "win_streak", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "lose_streak", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "shadow_lose_streak", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "active_flag", "INTEGER DEFAULT 1");
        addColumnIfMissing(db, table, "frozen", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "freeze_level", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "freeze_until_issue", "TEXT");
        addColumnIfMissing(db, table, "pass_flag", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "promote_pass", "INTEGER DEFAULT 0");

        addColumnIfMissing(db, table, "factor_key", "TEXT");
        addColumnIfMissing(db, table, "factor_name", "TEXT");
        addColumnIfMissing(db, table, "seed_key", "TEXT");
        addColumnIfMissing(db, table, "shadow_key", "TEXT");
        addColumnIfMissing(db, table, "source_votes", "INTEGER DEFAULT 0");
        addColumnIfMissing(db, table, "independent_evidence_count", "INTEGER DEFAULT 0");

        addColumnIfMissing(db, table, "feature_json", "TEXT");
        addColumnIfMissing(db, table, "tags_text", "TEXT");
        addColumnIfMissing(db, table, "risk_text", "TEXT");
        addColumnIfMissing(db, table, "fail_reason", "TEXT");
    }

    private void ensureV20Indexes(SQLiteDatabase db) {
        if (db == null) {
            return;
        }

        String[] tables =
                new String[] {
                    V20_TABLE_PRE_DRAW_FEATURE,
                    V20_TABLE_PRE_DRAW_NUMBER_SAMPLE,
                    V20_TABLE_FULL_CANDIDATE_SCAN,
                    V20_TABLE_CANDIDATE_AUDIT,
                    V20_TABLE_CAUSAL_FACTOR_MATRIX,
                    V20_TABLE_POSITION_CAUSE_MATRIX,
                    V20_TABLE_TEACHER_NUMBER_MATRIX,
                    V20_TABLE_COUNTERFACTUAL_RANK,
                    V20_TABLE_THIRD_GUARD_MATRIX,
                    V20_TABLE_RULE_HEALTH_MATRIX,
                    V20_TABLE_SHADOW_HEALTH_GATE,
                    V20_TABLE_FALSE_STRONG_FREEZE,
                    V20_TABLE_SEED_CAUSE_MATRIX,
                    V20_TABLE_DYNAMIC_CAUSE_CHAIN,
                    V20_TABLE_DECISION_AUDIT,
                    V20_TABLE_REAL_TRADE_LEDGER
                };

        for (int i = 0; i < tables.length; i++) {
            ensureV20IndexesForTable(db, tables[i]);
        }
    }

    private void ensureV20IndexesForTable(SQLiteDatabase db, String table) {
        if (db == null || table == null || table.length() == 0) {
            return;
        }

        try {
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_"
                            + table
                            + "_target ON "
                            + table
                            + "(target_issue)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_"
                            + table
                            + "_state ON "
                            + table
                            + "(state_key,position)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_"
                            + table
                            + "_pos_num ON "
                            + table
                            + "(position,number)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_"
                            + table
                            + "_rank ON "
                            + table
                            + "(target_issue,rank_no)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_"
                            + table
                            + "_rule ON "
                            + table
                            + "(rule_key,source_key,state_key)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_"
                            + table
                            + "_false ON "
                            + table
                            + "(false_strong_key,frozen,freeze_level)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_"
                            + table
                            + "_window_state ON "
                            + table
                            + "(window_state_key,position,feature_distance)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_"
                            + table
                            + "_replay ON "
                            + table
                            + "(replay_key)");
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_"
                            + table
                            + "_updated ON "
                            + table
                            + "(updated_at)");
        } catch (Throwable e) {
        }
    }

    private void normalizeV20EvidenceCv(ContentValues cv, String table) {
        if (cv == null) {
            return;
        }

        String stateKey = getCvString(cv, "state_key", "");
        String windowState = getCvString(cv, "window_state_key", "");
        int position = getCvInt(cv, "position", 0);

        if (windowState.length() == 0) {
            windowState = buildDeterministicWindowStateKey(cv);
            cv.put("window_state_key", windowState);
        }

        if (stateKey.length() == 0) {
            cv.put("state_key", windowState.length() == 0 ? "V20_STATE_UNKNOWN" : windowState);
            stateKey = getCvString(cv, "state_key", "");
        }

        String base = getCvString(cv, "base_issue", "");
        String target = getCvString(cv, "target_issue", "");
        int mask = getCvInt(cv, "k3_mask", 0);
        int number = getCvInt(cv, "number", 0);
        int rank = getCvInt(cv, "rank_no", 0);

        if (getCvString(cv, "replay_key", "").length() == 0) {
            cv.put(
                    "replay_key",
                    "RP|"
                            + cleanKeyPart(table)
                            + "|B="
                            + cleanKeyPart(base)
                            + "|T="
                            + cleanKeyPart(target)
                            + "|S="
                            + cleanKeyPart(stateKey)
                            + "|P"
                            + position
                            + "|N"
                            + number
                            + "|K"
                            + mask
                            + "|R"
                            + rank);
        }

        if (getCvString(cv, "calculation_version", "").length() == 0) {
            cv.put("calculation_version", "V20_DETERMINISTIC_CAUSE_CHAIN_11F_V2");
        }

        if (!cv.containsKey("no_future_leak_flag")) {
            cv.put("no_future_leak_flag", 1);
        }

        double evidence = getCvDouble(cv, "evidence_contribution", 0.0d);
        if (evidence == 0.0d) {
            evidence =
                    getCvDouble(cv, "support_score", 0.0d)
                            + getCvDouble(cv, "causal_edge", 0.0d)
                            + getCvDouble(cv, "teacher_edge", 0.0d)
                            + getCvDouble(cv, "position_edge", 0.0d);
            cv.put("evidence_contribution", clampLocal01(evidence));
        }

        double counter = getCvDouble(cv, "counter_contribution", 0.0d);
        if (counter == 0.0d) {
            counter =
                    getCvDouble(cv, "risk_score", 0.0d)
                            + getCvDouble(cv, "false_strong_risk", 0.0d)
                            + getCvDouble(cv, "same_class_lose_risk", 0.0d)
                            + getCvDouble(cv, "guard_drag_risk", 0.0d);
            cv.put("counter_contribution", clampLocal01(counter));
        }

        if (getCvString(cv, "sample_trust", "").length() == 0) {
            int sample = getCvInt(cv, "sample_count", getCvInt(cv, "similar_window_sample", 0));
            cv.put("sample_trust", sampleTrustFromCount(sample));
        }

        if (getCvString(cv, "recent_effectiveness", "").length() == 0) {
            double recent =
                    getCvDouble(
                            cv,
                            "recent_hit_rate",
                            getCvDouble(cv, "similar_window_recent_rate", 0.0d));
            cv.put("recent_effectiveness", clampLocal01(recent));
        }

        if (getCvString(cv, "action_advice", "").length() == 0) {
            cv.put("action_advice", buildV20ActionAdvice(cv));
        }

        if (getCvString(cv, "evidence_support_text", "").length() == 0) {
            cv.put("evidence_support_text", buildEvidenceSupportText(cv));
        }

        if (getCvString(cv, "counter_evidence_text", "").length() == 0) {
            cv.put("counter_evidence_text", buildCounterEvidenceText(cv));
        }

        if (getCvString(cv, "summary_text", "").length() == 0) {
            cv.put("summary_text", buildV20ReplaySummary(cv));
        }
    }

    private String buildDeterministicWindowStateKey(ContentValues cv) {
        if (cv == null) {
            return "V20_STATE_UNKNOWN";
        }

        int position = getCvInt(cv, "position", 0);
        String s12 =
                firstNonEmpty(
                        getCvString(cv, "state_key_12", ""),
                        bucketWindow(getCvDouble(cv, "window_12", 0.0d), "W12"));
        String s24 =
                firstNonEmpty(
                        getCvString(cv, "state_key_24", ""),
                        bucketWindow(getCvDouble(cv, "window_24", 0.0d), "W24"));
        String s48 =
                firstNonEmpty(
                        getCvString(cv, "state_key_48", ""),
                        bucketWindow(getCvDouble(cv, "window_48", 0.0d), "W48"));
        String s96 =
                firstNonEmpty(
                        getCvString(cv, "state_key_96", ""),
                        bucketWindow(getCvDouble(cv, "window_96", 0.0d), "W96"));
        String s360 =
                firstNonEmpty(
                        getCvString(cv, "state_key_360", ""),
                        bucketWindow(getCvDouble(cv, "window_360", 0.0d), "W360"));
        String hot =
                firstNonEmpty(
                        getCvString(cv, "hot_cold_bucket", ""),
                        bucketSignedText(getCvDouble(cv, "hot_cold_slope", 0.0d), "HC"));
        String miss =
                firstNonEmpty(
                        getCvString(cv, "omission_bucket", ""),
                        "MISS_" + getCvInt(cv, "missing_gap", 0));
        String dense =
                firstNonEmpty(
                        getCvString(cv, "density_bucket", ""),
                        bucketWindow(
                                getCvDouble(
                                        cv,
                                        "density_score",
                                        getCvDouble(cv, "support_score", 0.0d)),
                                "DEN"));
        String rep =
                firstNonEmpty(
                        getCvString(cv, "repeat_bucket", ""),
                        bucketWindow(getCvDouble(cv, "repeat_score", 0.0d), "REP"));
        String oe = firstNonEmpty(getCvString(cv, "odd_even_bias", ""), "OE_N");
        String hl = firstNonEmpty(getCvString(cv, "high_low_bias", ""), "HL_N");
        String nt = firstNonEmpty(getCvString(cv, "neighbor_tail_state", ""), "NT_N");
        String crowd = firstNonEmpty(getCvString(cv, "position_crowd_state", ""), "CROWD_N");
        String sync = firstNonEmpty(getCvString(cv, "cross_position_sync", ""), "SYNC_N");
        String squeeze = firstNonEmpty(getCvString(cv, "cross_position_squeeze", ""), "SQUEEZE_N");
        String shift = firstNonEmpty(getCvString(cv, "timing_shift", ""), "TIME_N");
        int lose = getCvInt(cv, "recent_lose_streak", getCvInt(cv, "lose_streak", 0));

        String key =
                "WS|P" + position + "|" + s12 + "|" + s24 + "|" + s48 + "|" + s96 + "|" + s360 + "|"
                        + hot + "|" + miss + "|" + dense + "|" + rep + "|" + oe + "|" + hl + "|"
                        + nt + "|" + crowd + "|" + sync + "|" + squeeze + "|L" + lose + "|" + shift;
        if (key.length() > 240) {
            key = key.substring(0, 200) + "|" + Math.abs(key.hashCode());
        }
        return key;
    }

    private String buildEvidenceSupportText(ContentValues cv) {
        return "证据贡献="
                + percentLocal(getCvDouble(cv, "evidence_contribution", 0.0d))
                + "｜因果="
                + percentLocal(getCvDouble(cv, "causal_edge", getCvDouble(cv, "cause_score", 0.0d)))
                + "｜教师="
                + percentLocal(
                        getCvDouble(
                                cv, "teacher_edge", getCvDouble(cv, "number_teacher_edge", 0.0d)))
                + "｜位置="
                + percentLocal(
                        getCvDouble(cv, "position_edge", getCvDouble(cv, "health_score", 0.0d)))
                + "｜相似窗="
                + percentLocal(getCvDouble(cv, "similar_window_hit_rate", 0.0d))
                + "｜样本可信="
                + percentLocal(getCvDouble(cv, "sample_trust", 0.0d));
    }

    private String buildCounterEvidenceText(ContentValues cv) {
        return "反证贡献="
                + percentLocal(getCvDouble(cv, "counter_contribution", 0.0d))
                + "｜风险="
                + percentLocal(getCvDouble(cv, "risk_score", 0.0d))
                + "｜伪强="
                + percentLocal(getCvDouble(cv, "false_strong_risk", 0.0d))
                + "｜同类连挂="
                + percentLocal(getCvDouble(cv, "same_class_lose_risk", 0.0d))
                + "｜第三防线="
                + percentLocal(getCvDouble(cv, "guard_drag_risk", 0.0d))
                + "｜近期有效="
                + percentLocal(getCvDouble(cv, "recent_effectiveness", 0.0d));
    }

    private String buildV20ActionAdvice(ContentValues cv) {
        double evidence = getCvDouble(cv, "evidence_contribution", 0.0d);
        double counter = getCvDouble(cv, "counter_contribution", 0.0d);
        double trust = getCvDouble(cv, "sample_trust", 0.0d);
        double recent = getCvDouble(cv, "recent_effectiveness", 0.0d);
        int frozen = getCvInt(cv, "frozen", 0);
        int pass = getCvInt(cv, "pass_flag", getCvInt(cv, "promote_pass", 0));

        if (frozen == 1 || counter >= 0.72d) {
            return "HOLD：反证或冻结偏强，只记录原因，不放行BET";
        }

        if (pass == 1 && evidence >= 0.58d && counter <= 0.48d && trust >= 0.32d) {
            return "ALLOW_TO_GATE：证据可送最终门，仍由DecisionGateEngine裁决";
        }

        if (evidence >= 0.52d && counter <= 0.55d && recent >= 0.30d) {
            return "WATCH_TO_GATE：证据略强，建议进入最终门复核";
        }

        return "SHADOW/HOLD：证据不足或样本可信不足，不计真实盈亏";
    }

    private String buildV20ReplaySummary(ContentValues cv) {
        return "V20可回放原因｜state="
                + getCvString(cv, "state_key", "")
                + "｜windowState="
                + getCvString(cv, "window_state_key", "")
                + "｜"
                + buildEvidenceSupportText(cv)
                + "｜"
                + buildCounterEvidenceText(cv)
                + "｜建议="
                + getCvString(cv, "action_advice", "")
                + "｜说明=同样输入同样输出，可复查、可回放，不表示必中";
    }

    public ContentValues buildV20WindowStateEvidence(
            String baseIssue,
            String targetIssue,
            int position,
            int number,
            int k3Mask,
            String sourceKey,
            String ruleKey,
            double support,
            double risk,
            int sample,
            double recentHitRate) {
        ContentValues cv = new ContentValues();
        cv.put("base_issue", baseIssue == null ? "" : baseIssue);
        cv.put("target_issue", targetIssue == null ? "" : targetIssue);
        cv.put("position", position);
        cv.put("number", number);
        cv.put("k3_mask", k3Mask);
        cv.put("source_key", sourceKey == null ? "" : sourceKey);
        cv.put("rule_key", ruleKey == null ? "" : ruleKey);
        cv.put("support_score", clampLocal01(support));
        cv.put("risk_score", clampLocal01(risk));
        cv.put("sample_count", Math.max(0, sample));
        cv.put("recent_hit_rate", clampLocal01(recentHitRate));
        cv.put("similar_window_sample", Math.max(0, sample));
        cv.put("similar_window_hit_rate", clampLocal01(recentHitRate));
        cv.put("window_state_key", buildDeterministicWindowStateKey(cv));
        normalizeV20EvidenceCv(cv, "V20_WINDOW_STATE");
        return cv;
    }

    public long saveV20WindowStateEvidence(ContentValues cv) {
        if (cv == null) {
            return -1L;
        }
        return saveV20Record(V20_TABLE_CANDIDATE_AUDIT, cv);
    }

    public List<ContentValues> querySimilarWindowRecords(
            String stateKey, int position, double maxFeatureDistance, int limit) {
        if (stateKey == null || stateKey.length() == 0 || !AppConfig.isValidPosition(position)) {
            return new ArrayList<ContentValues>();
        }
        if (maxFeatureDistance <= 0.0d) {
            maxFeatureDistance = 0.28d;
        }
        return queryV20Records(
                V20_TABLE_DYNAMIC_CAUSE_CHAIN,
                "state_key=? AND position=? AND feature_distance<=?",
                new String[] {
                    stateKey, String.valueOf(position), String.valueOf(maxFeatureDistance)
                },
                "target_issue DESC, feature_distance ASC, updated_at DESC",
                limit > 0 ? limit : 120);
    }

    private double sampleTrustFromCount(int sample) {
        if (sample <= 0) {
            return 0.0d;
        }
        if (sample >= 240) {
            return 1.0d;
        }
        return clampLocal01(sample / 240.0d);
    }

    private double clampLocal01(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return 0.0d;
        }
        if (v < 0.0d) {
            return 0.0d;
        }
        if (v > 1.0d) {
            return 1.0d;
        }
        return v;
    }

    private String bucketWindow(double v, String prefix) {
        String p = prefix == null ? "B" : prefix;
        if (v >= 0.72d) {
            return p + "_VH";
        }
        if (v >= 0.58d) {
            return p + "_H";
        }
        if (v >= 0.42d) {
            return p + "_M";
        }
        if (v > 0.0d) {
            return p + "_L";
        }
        return p + "_N";
    }

    private String bucketSignedText(double v, String prefix) {
        String p = prefix == null ? "S" : prefix;
        if (v >= 0.18d) {
            return p + "_UP2";
        }
        if (v >= 0.05d) {
            return p + "_UP1";
        }
        if (v <= -0.18d) {
            return p + "_DOWN2";
        }
        if (v <= -0.05d) {
            return p + "_DOWN1";
        }
        return p + "_FLAT";
    }

    private String percentLocal(double v) {
        return String.format(Locale.US, "%.1f%%", Double.valueOf(clampLocal01(v) * 100.0d));
    }

    public long saveV20Record(String table, ContentValues source) {
        if (!isV20Table(table) || source == null) {
            return -1L;
        }

        SQLiteDatabase db = writable();
        ensureV20CauseChainReady(db);

        ContentValues cv = new ContentValues(source);
        long now = System.currentTimeMillis();

        if (getCvLong(cv, "created_at", 0L) <= 0L) {
            cv.put("created_at", now);
        }

        cv.put("updated_at", now);

        if (getCvString(cv, V20_RECORD_KEY, "").length() == 0) {
            cv.put(V20_RECORD_KEY, buildV20RecordKey(table, cv));
        }

        return insertFiltered(db, table, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public int saveV20Records(String table, List<ContentValues> list) {
        if (!isV20Table(table) || list == null || list.size() == 0) {
            return 0;
        }

        SQLiteDatabase db = writable();
        ensureV20CauseChainReady(db);

        int saved = 0;
        db.beginTransaction();

        try {
            for (int i = 0; i < list.size(); i++) {
                ContentValues source = list.get(i);

                if (source == null) {
                    continue;
                }

                ContentValues cv = new ContentValues(source);
                long now = System.currentTimeMillis();

                if (getCvLong(cv, "created_at", 0L) <= 0L) {
                    cv.put("created_at", now);
                }

                cv.put("updated_at", now);

                if (getCvString(cv, V20_RECORD_KEY, "").length() == 0) {
                    cv.put(V20_RECORD_KEY, buildV20RecordKey(table, cv));
                }

                long id = insertFiltered(db, table, cv, SQLiteDatabase.CONFLICT_REPLACE);

                if (id >= 0L) {
                    saved++;
                }
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        return saved;
    }

    public ContentValues getV20RecordByKey(String table, String recordKey) {
        if (!isV20Table(table) || recordKey == null || recordKey.length() == 0) {
            return null;
        }

        return getV20Record(table, V20_RECORD_KEY, recordKey);
    }

    public ContentValues getV20Record(String table, String keyColumn, String keyValue) {
        if (!isV20Table(table)
                || keyColumn == null
                || keyColumn.length() == 0
                || keyValue == null
                || keyValue.length() == 0) {
            return null;
        }

        SQLiteDatabase db = readable();
        Cursor c = null;

        try {
            c =
                    db.query(
                            table,
                            null,
                            keyColumn + "=?",
                            new String[] {keyValue},
                            null,
                            null,
                            "updated_at DESC, _id DESC",
                            "1");

            if (c.moveToFirst()) {
                return cursorRowToContentValues(c);
            }
        } catch (Throwable e) {
        } finally {
            closeCursor(c);
        }

        return null;
    }

    public List<ContentValues> queryV20Records(
            String table, String where, String[] args, String orderBy, int limit) {
        ArrayList<ContentValues> list = new ArrayList<ContentValues>();

        if (!isV20Table(table)) {
            return list;
        }

        Cursor c = null;

        try {
            c =
                    readable()
                            .query(
                                    table,
                                    null,
                                    where,
                                    args,
                                    null,
                                    null,
                                    orderBy == null || orderBy.length() == 0
                                            ? "updated_at DESC, _id DESC"
                                            : orderBy,
                                    limit > 0 ? String.valueOf(limit) : null);

            while (c.moveToNext()) {
                list.add(cursorRowToContentValues(c));
            }
        } catch (Throwable e) {
        } finally {
            closeCursor(c);
        }

        return list;
    }

    public long savePreDrawFeature(ContentValues cv) {
        if (cv == null) {
            return -1L;
        }

        String target = getCvString(cv, "target_issue", "");

        if (target.length() > 0) {
            cv.put(V20_RECORD_KEY, "PDF|" + target);
        }

        return saveV20Record(V20_TABLE_PRE_DRAW_FEATURE, cv);
    }

    public ContentValues getPreDrawFeature(String targetIssue) {
        if (targetIssue == null || targetIssue.length() == 0) {
            return null;
        }

        return getV20RecordByKey(V20_TABLE_PRE_DRAW_FEATURE, "PDF|" + targetIssue);
    }

    public long savePreDrawNumberSample(ContentValues cv) {
        if (cv == null) {
            return -1L;
        }

        String target = getCvString(cv, "target_issue", "");
        int position = getCvInt(cv, "position", 0);
        int number = getCvInt(cv, "number", 0);

        if (target.length() > 0
                && AppConfig.isValidPosition(position)
                && AppConfig.isValidNumber(number)) {
            cv.put(V20_RECORD_KEY, "PDN|" + target + "|P" + position + "|N" + number);
        }

        return saveV20Record(V20_TABLE_PRE_DRAW_NUMBER_SAMPLE, cv);
    }

    public int savePreDrawNumberSamples(List<ContentValues> list) {
        return saveV20Records(V20_TABLE_PRE_DRAW_NUMBER_SAMPLE, list);
    }

    public List<ContentValues> getPreDrawNumberSamples(String targetIssue, int position) {
        if (targetIssue == null || targetIssue.length() == 0) {
            return new ArrayList<ContentValues>();
        }

        if (AppConfig.isValidPosition(position)) {
            return queryV20Records(
                    V20_TABLE_PRE_DRAW_NUMBER_SAMPLE,
                    "target_issue=? AND position=?",
                    new String[] {targetIssue, String.valueOf(position)},
                    "number ASC",
                    20);
        }

        return queryV20Records(
                V20_TABLE_PRE_DRAW_NUMBER_SAMPLE,
                "target_issue=?",
                new String[] {targetIssue},
                "position ASC, number ASC",
                120);
    }

    public long saveFullCandidateScan(ContentValues cv) {
        if (cv == null) {
            return -1L;
        }

        String target = getCvString(cv, "target_issue", "");
        int rank = getCvInt(cv, "rank_no", 0);
        int position = getCvInt(cv, "position", 0);
        int mask = getCvInt(cv, "k3_mask", 0);

        if (target.length() > 0) {
            cv.put(V20_RECORD_KEY, "SCAN|" + target + "|R" + rank + "|P" + position + "|K" + mask);
        }

        return saveV20Record(V20_TABLE_FULL_CANDIDATE_SCAN, cv);
    }

    public int saveFullCandidateScans(List<ContentValues> list) {
        return saveV20Records(V20_TABLE_FULL_CANDIDATE_SCAN, list);
    }

    public List<ContentValues> getFullCandidateScans(String targetIssue, int limit) {
        if (targetIssue == null || targetIssue.length() == 0) {
            return new ArrayList<ContentValues>();
        }

        return queryV20Records(
                V20_TABLE_FULL_CANDIDATE_SCAN,
                "target_issue=?",
                new String[] {targetIssue},
                "rank_no ASC, final_cause_score DESC, final_p DESC",
                limit > 0 ? limit : 1200);
    }

    public List<ContentValues> getTopCandidateScans(String targetIssue, int limit) {
        return getFullCandidateScans(targetIssue, limit <= 0 ? 18 : limit);
    }

    public long saveCandidateAudit(ContentValues cv) {
        if (cv == null) {
            return -1L;
        }

        String target = getCvString(cv, "target_issue", "");
        int rank = getCvInt(cv, "rank_no", 0);
        int position = getCvInt(cv, "position", 0);
        int mask = getCvInt(cv, "k3_mask", 0);

        if (target.length() > 0) {
            cv.put(V20_RECORD_KEY, "AUDIT|" + target + "|R" + rank + "|P" + position + "|K" + mask);
        }

        return saveV20Record(V20_TABLE_CANDIDATE_AUDIT, cv);
    }

    public int saveCandidateAudits(List<ContentValues> list) {
        return saveV20Records(V20_TABLE_CANDIDATE_AUDIT, list);
    }

    public List<ContentValues> getCandidateAudits(String targetIssue, int limit) {
        if (targetIssue == null || targetIssue.length() == 0) {
            return new ArrayList<ContentValues>();
        }

        return queryV20Records(
                V20_TABLE_CANDIDATE_AUDIT,
                "target_issue=?",
                new String[] {targetIssue},
                "rank_no ASC, final_cause_score DESC, final_p DESC",
                limit > 0 ? limit : 60);
    }

    public long saveCausalFactorMatrix(ContentValues cv) {
        if (cv == null) {
            return -1L;
        }

        String stateKey = getCvString(cv, "state_key", "");
        String factorKey = getCvString(cv, "factor_key", "");
        int position = getCvInt(cv, "position", 0);

        if (stateKey.length() > 0 || factorKey.length() > 0) {
            cv.put(
                    V20_RECORD_KEY,
                    "CAUSE|"
                            + cleanKeyPart(stateKey)
                            + "|F="
                            + cleanKeyPart(factorKey)
                            + "|P"
                            + position);
        }

        return saveV20Record(V20_TABLE_CAUSAL_FACTOR_MATRIX, cv);
    }

    public ContentValues getCausalFactorMatrix(String stateKey, String factorKey, int position) {
        String key =
                "CAUSE|"
                        + cleanKeyPart(stateKey)
                        + "|F="
                        + cleanKeyPart(factorKey)
                        + "|P"
                        + position;
        return getV20RecordByKey(V20_TABLE_CAUSAL_FACTOR_MATRIX, key);
    }

    public double getCausalFactorEdge(String stateKey, String factorKey, int position) {
        ContentValues cv = getCausalFactorMatrix(stateKey, factorKey, position);

        if (cv == null) {
            return 0.0d;
        }

        return getCvDouble(cv, "causal_edge", 0.0d);
    }

    public long savePositionCauseMatrix(ContentValues cv) {
        if (cv == null) {
            return -1L;
        }

        String stateKey = getCvString(cv, "state_key", "GLOBAL");
        int position = getCvInt(cv, "position", 0);

        if (AppConfig.isValidPosition(position)) {
            cv.put(V20_RECORD_KEY, "POS|" + cleanKeyPart(stateKey) + "|P" + position);
        }

        return saveV20Record(V20_TABLE_POSITION_CAUSE_MATRIX, cv);
    }

    public ContentValues getPositionCauseMatrix(String stateKey, int position) {
        if (!AppConfig.isValidPosition(position)) {
            return null;
        }

        ContentValues exact =
                getV20RecordByKey(
                        V20_TABLE_POSITION_CAUSE_MATRIX,
                        "POS|" + cleanKeyPart(stateKey) + "|P" + position);

        if (exact != null) {
            return exact;
        }

        return getV20RecordByKey(V20_TABLE_POSITION_CAUSE_MATRIX, "POS|GLOBAL|P" + position);
    }

    public double getPositionHealthScore(String stateKey, int position) {
        ContentValues cv = getPositionCauseMatrix(stateKey, position);

        if (cv == null) {
            return 0.0d;
        }

        double h = getCvDouble(cv, "health_score", 0.0d);

        if (h == 0.0d) {
            h = getCvDouble(cv, "position_edge", 0.0d);
        }

        return h;
    }

    public long saveTeacherNumberMatrix(ContentValues cv) {
        if (cv == null) {
            return -1L;
        }

        String stateKey = getCvString(cv, "state_key", "");
        int position = getCvInt(cv, "position", 0);
        int number = getCvInt(cv, "number", 0);

        if (stateKey.length() > 0
                && AppConfig.isValidPosition(position)
                && AppConfig.isValidNumber(number)) {
            cv.put(
                    V20_RECORD_KEY,
                    "TNM|" + cleanKeyPart(stateKey) + "|P" + position + "|N" + number);
        }

        return saveV20Record(V20_TABLE_TEACHER_NUMBER_MATRIX, cv);
    }

    public ContentValues getTeacherNumberMatrix(String stateKey, int position, int number) {
        if (stateKey == null
                || stateKey.length() == 0
                || !AppConfig.isValidPosition(position)
                || !AppConfig.isValidNumber(number)) {
            return null;
        }

        return getV20RecordByKey(
                V20_TABLE_TEACHER_NUMBER_MATRIX,
                "TNM|" + cleanKeyPart(stateKey) + "|P" + position + "|N" + number);
    }

    public double getTeacherNumberSupport(String stateKey, int position, int number) {
        ContentValues cv = getTeacherNumberMatrix(stateKey, position, number);

        if (cv == null) {
            return 0.0d;
        }

        double v = getCvDouble(cv, "support_score", 0.0d);

        if (v == 0.0d) {
            v = getCvDouble(cv, "teacher_edge", 0.0d);
        }

        if (v == 0.0d) {
            v = getCvDouble(cv, "number_teacher_edge", 0.0d);
        }

        return v;
    }

    public long saveCounterfactualRank(ContentValues cv) {
        if (cv == null) {
            return -1L;
        }

        String stateKey = getCvString(cv, "state_key", "");
        int rank = getCvInt(cv, "rank_no", 0);

        if (stateKey.length() > 0 && rank > 0) {
            cv.put(V20_RECORD_KEY, "RANK|" + cleanKeyPart(stateKey) + "|R" + rank);
        }

        return saveV20Record(V20_TABLE_COUNTERFACTUAL_RANK, cv);
    }

    public ContentValues getCounterfactualRank(String stateKey, int rankNo) {
        if (stateKey == null || stateKey.length() == 0 || rankNo <= 0) {
            return null;
        }

        return getV20RecordByKey(
                V20_TABLE_COUNTERFACTUAL_RANK, "RANK|" + cleanKeyPart(stateKey) + "|R" + rankNo);
    }

    public double getRankCounterEdge(String stateKey, int rankNo) {
        ContentValues cv = getCounterfactualRank(stateKey, rankNo);

        if (cv == null) {
            return 0.0d;
        }

        return getCvDouble(cv, "rank_counter_edge", 0.0d);
    }

    public long saveThirdGuardMatrix(ContentValues cv) {
        if (cv == null) {
            return -1L;
        }

        String stateKey = getCvString(cv, "state_key", "");
        int position = getCvInt(cv, "position", 0);
        int mask = getCvInt(cv, "k3_mask", 0);
        int number = getCvInt(cv, "number", 0);

        if (stateKey.length() > 0) {
            cv.put(
                    V20_RECORD_KEY,
                    "THIRD|"
                            + cleanKeyPart(stateKey)
                            + "|P"
                            + position
                            + "|K"
                            + mask
                            + "|N"
                            + number);
        }

        return saveV20Record(V20_TABLE_THIRD_GUARD_MATRIX, cv);
    }

    public ContentValues getThirdGuardMatrix(
            String stateKey, int position, int k3Mask, int number) {
        if (stateKey == null || stateKey.length() == 0) {
            return null;
        }

        return getV20RecordByKey(
                V20_TABLE_THIRD_GUARD_MATRIX,
                "THIRD|"
                        + cleanKeyPart(stateKey)
                        + "|P"
                        + position
                        + "|K"
                        + k3Mask
                        + "|N"
                        + number);
    }

    public double getThirdGuardNet(String stateKey, int position, int k3Mask, int number) {
        ContentValues cv = getThirdGuardMatrix(stateKey, position, k3Mask, number);

        if (cv == null) {
            return 0.0d;
        }

        double v = getCvDouble(cv, "third_guard_net", 0.0d);

        if (v == 0.0d) {
            v = getCvDouble(cv, "net_edge", 0.0d);
        }

        return v;
    }

    public long saveRuleHealthMatrix(ContentValues cv) {
        if (cv == null) {
            return -1L;
        }

        String stateKey = getCvString(cv, "state_key", "");
        String sourceKey = getCvString(cv, "source_key", "");
        String ruleKey = getCvString(cv, "rule_key", "");

        cv.put(
                V20_RECORD_KEY,
                "RULE|"
                        + cleanKeyPart(stateKey)
                        + "|S="
                        + cleanKeyPart(sourceKey)
                        + "|R="
                        + cleanKeyPart(ruleKey));
        return saveV20Record(V20_TABLE_RULE_HEALTH_MATRIX, cv);
    }

    public ContentValues getRuleHealthMatrix(String stateKey, String sourceKey, String ruleKey) {
        return getV20RecordByKey(
                V20_TABLE_RULE_HEALTH_MATRIX,
                "RULE|"
                        + cleanKeyPart(stateKey)
                        + "|S="
                        + cleanKeyPart(sourceKey)
                        + "|R="
                        + cleanKeyPart(ruleKey));
    }

    public boolean isV20RuleFrozen(String stateKey, String sourceKey, String ruleKey) {
        ContentValues cv = getRuleHealthMatrix(stateKey, sourceKey, ruleKey);

        if (cv == null) {
            return false;
        }

        if (getCvInt(cv, "frozen", 0) == 1) {
            return true;
        }

        if (getCvInt(cv, "freeze_level", 0) > 0) {
            return true;
        }

        String status = getCvString(cv, "status", "");
        return "FROZEN".equals(status) || "RISK".equals(status) || "DECAY".equals(status);
    }

    public double getRuleHealthEdge(String stateKey, String sourceKey, String ruleKey) {
        ContentValues cv = getRuleHealthMatrix(stateKey, sourceKey, ruleKey);

        if (cv == null) {
            return 0.0d;
        }

        double v = getCvDouble(cv, "health_score", 0.0d);

        if (v == 0.0d) {
            v = getCvDouble(cv, "source_health_edge", 0.0d);
        }

        return v;
    }

    public long saveShadowHealthGate(ContentValues cv) {
        if (cv == null) {
            return -1L;
        }

        String stateKey = getCvString(cv, "state_key", "");
        String grade = getCvString(cv, "grade", "");
        int position = getCvInt(cv, "position", 0);

        cv.put(
                V20_RECORD_KEY,
                "SHADOW|" + cleanKeyPart(grade) + "|" + cleanKeyPart(stateKey) + "|P" + position);
        return saveV20Record(V20_TABLE_SHADOW_HEALTH_GATE, cv);
    }

    public ContentValues getShadowHealthGate(String grade, String stateKey, int position) {
        return getV20RecordByKey(
                V20_TABLE_SHADOW_HEALTH_GATE,
                "SHADOW|" + cleanKeyPart(grade) + "|" + cleanKeyPart(stateKey) + "|P" + position);
    }

    public boolean isShadowHealthPromotePass(String grade, String stateKey, int position) {
        ContentValues cv = getShadowHealthGate(grade, stateKey, position);

        if (cv == null) {
            return false;
        }

        if (getCvInt(cv, "promote_pass", 0) == 1) {
            return true;
        }

        if (getCvInt(cv, "pass_flag", 0) == 1) {
            return true;
        }

        String status = getCvString(cv, "status", "");
        return "PASS".equals(status) || "PROMOTE".equals(status) || "READY".equals(status);
    }

    public long saveFalseStrongFreeze(ContentValues cv) {
        if (cv == null) {
            return -1L;
        }

        String key = getCvString(cv, "false_strong_key", "");

        if (key.length() == 0) {
            key = buildFalseStrongKeyFromCv(cv);
            cv.put("false_strong_key", key);
        }

        cv.put(V20_RECORD_KEY, "FALSE|" + cleanKeyPart(key));
        return saveV20Record(V20_TABLE_FALSE_STRONG_FREEZE, cv);
    }

    public ContentValues getFalseStrongFreeze(String falseStrongKey) {
        if (falseStrongKey == null || falseStrongKey.length() == 0) {
            return null;
        }

        return getV20RecordByKey(
                V20_TABLE_FALSE_STRONG_FREEZE, "FALSE|" + cleanKeyPart(falseStrongKey));
    }

    public boolean isFalseStrongFrozen(String falseStrongKey, String currentIssue) {
        ContentValues cv = getFalseStrongFreeze(falseStrongKey);

        if (cv == null) {
            return false;
        }

        if (getCvInt(cv, "frozen", 0) == 1) {
            return true;
        }

        if (getCvInt(cv, "freeze_level", 0) <= 0) {
            return false;
        }

        String until = getCvString(cv, "freeze_until_issue", "");

        if (until.length() == 0 || currentIssue == null || currentIssue.length() == 0) {
            return true;
        }

        long a = parseIssueLong(currentIssue);
        long b = parseIssueLong(until);

        if (a <= 0L || b <= 0L) {
            return true;
        }

        return a <= b;
    }

    public long saveSeedCauseMatrix(ContentValues cv) {
        if (cv == null) {
            return -1L;
        }

        String stateKey = getCvString(cv, "state_key", "");
        String seedKey = getCvString(cv, "seed_key", "");

        cv.put(V20_RECORD_KEY, "SEED|" + cleanKeyPart(stateKey) + "|S=" + cleanKeyPart(seedKey));
        return saveV20Record(V20_TABLE_SEED_CAUSE_MATRIX, cv);
    }

    public ContentValues getSeedCauseMatrix(String stateKey, String seedKey) {
        return getV20RecordByKey(
                V20_TABLE_SEED_CAUSE_MATRIX,
                "SEED|" + cleanKeyPart(stateKey) + "|S=" + cleanKeyPart(seedKey));
    }

    public double getSeedSimilarityNet(String stateKey, String seedKey) {
        ContentValues cv = getSeedCauseMatrix(stateKey, seedKey);

        if (cv == null) {
            return 0.0d;
        }

        return getCvDouble(cv, "seed_similarity", 0.0d)
                - getCvDouble(cv, "false_seed_similarity", 0.0d);
    }

    public long saveDynamicCauseChain(ContentValues cv) {
        if (cv == null) {
            return -1L;
        }

        String target = getCvString(cv, "target_issue", "");
        int rank = getCvInt(cv, "rank_no", 0);
        int position = getCvInt(cv, "position", 0);
        int mask = getCvInt(cv, "k3_mask", 0);

        if (target.length() > 0) {
            cv.put(V20_RECORD_KEY, "CHAIN|" + target + "|R" + rank + "|P" + position + "|K" + mask);
        }

        return saveV20Record(V20_TABLE_DYNAMIC_CAUSE_CHAIN, cv);
    }

    public int saveDynamicCauseChains(List<ContentValues> list) {
        return saveV20Records(V20_TABLE_DYNAMIC_CAUSE_CHAIN, list);
    }

    public ContentValues getBestDynamicCauseChain(String targetIssue) {
        if (targetIssue == null || targetIssue.length() == 0) {
            return null;
        }

        List<ContentValues> list =
                queryV20Records(
                        V20_TABLE_DYNAMIC_CAUSE_CHAIN,
                        "target_issue=?",
                        new String[] {targetIssue},
                        "final_cause_score DESC, final_p DESC, rank_no ASC",
                        1);

        return list.size() > 0 ? list.get(0) : null;
    }

    public List<ContentValues> getDynamicCauseChains(String targetIssue, int limit) {
        if (targetIssue == null || targetIssue.length() == 0) {
            return new ArrayList<ContentValues>();
        }

        return queryV20Records(
                V20_TABLE_DYNAMIC_CAUSE_CHAIN,
                "target_issue=?",
                new String[] {targetIssue},
                "final_cause_score DESC, final_p DESC, rank_no ASC",
                limit > 0 ? limit : 60);
    }

    public long saveDecisionAudit(ContentValues cv) {
        if (cv == null) {
            return -1L;
        }

        String target = getCvString(cv, "target_issue", "");

        if (target.length() > 0) {
            cv.put(V20_RECORD_KEY, "DECISION_AUDIT|" + target);
        }

        return saveV20Record(V20_TABLE_DECISION_AUDIT, cv);
    }

    public ContentValues getDecisionAudit(String targetIssue) {
        if (targetIssue == null || targetIssue.length() == 0) {
            return null;
        }

        return getV20RecordByKey(V20_TABLE_DECISION_AUDIT, "DECISION_AUDIT|" + targetIssue);
    }

    public long saveRealTradeLedgerFromTrade(Models.Trade t) {
        if (t == null || t.targetIssue == null || t.targetIssue.length() == 0) {
            return -1L;
        }

        ContentValues cv = new ContentValues();

        cv.put(V20_RECORD_KEY, "LEDGER|" + t.targetIssue);
        cv.put("trade_id", t.id);
        cv.put("decision_id", t.decisionId);
        cv.put("base_issue", t.baseIssue);
        cv.put("target_issue", t.targetIssue);
        cv.put("created_at", t.createdAt > 0L ? t.createdAt : t.betTime);
        cv.put("updated_at", System.currentTimeMillis());
        cv.put("position", t.position);
        cv.put("n1", t.n1);
        cv.put("n2", t.n2);
        cv.put("n3", t.n3);
        cv.put("k3_mask", t.k3Mask);
        cv.put("actual_number", t.actualNumber);
        cv.put("action", AppConfig.DECISION_BET);
        cv.put("result", t.result);
        cv.put("settled", t.settled);
        cv.put("hit", t.hit);
        cv.put("final_p", t.finalP);
        cv.put("dynamic_gate", t.dynamicGate);
        cv.put("edge", t.edge);
        cv.put("state_key", t.stateKey);
        cv.put("risk_score", t.evidenceRisk);
        cv.put("summary_text", t.settlementReason);
        cv.put("reason_text", t.reasonSummary);

        return saveV20Record(V20_TABLE_REAL_TRADE_LEDGER, cv);
    }

    public int markV20CandidateSettlement(String targetIssue, int position, int actualNumber) {
        if (targetIssue == null
                || targetIssue.length() == 0
                || !AppConfig.isValidPosition(position)
                || !AppConfig.isValidNumber(actualNumber)) {
            return 0;
        }

        int a =
                markV20TableCandidateSettlement(
                        V20_TABLE_FULL_CANDIDATE_SCAN, targetIssue, position, actualNumber);
        int b =
                markV20TableCandidateSettlement(
                        V20_TABLE_CANDIDATE_AUDIT, targetIssue, position, actualNumber);
        int c =
                markV20TableCandidateSettlement(
                        V20_TABLE_DYNAMIC_CAUSE_CHAIN, targetIssue, position, actualNumber);

        return a + b + c;
    }

    private int markV20TableCandidateSettlement(
            String table, String targetIssue, int position, int actualNumber) {
        if (!isV20Table(table)) {
            return 0;
        }

        SQLiteDatabase db = writable();
        ensureV20CauseChainReady(db);

        int updated = 0;
        Cursor c = null;

        try {
            c =
                    db.query(
                            table,
                            null,
                            "target_issue=? AND position=?",
                            new String[] {targetIssue, String.valueOf(position)},
                            null,
                            null,
                            "rank_no ASC, _id ASC");

            while (c.moveToNext()) {
                long id = getLong(c, "_id");
                int mask = getInt(c, "k3_mask");

                if (id <= 0L || !Models.isValidK3Mask(mask)) {
                    continue;
                }

                boolean hit = maskContainsNumber(mask, actualNumber);

                ContentValues cv = new ContentValues();
                cv.put("actual_number", actualNumber);
                cv.put("hit", hit ? 1 : 0);
                cv.put("result", hit ? AppConfig.RESULT_WIN : AppConfig.RESULT_MISS);
                cv.put("settled", 1);
                cv.put("updated_at", System.currentTimeMillis());

                updated +=
                        updateFiltered(db, table, cv, "_id=?", new String[] {String.valueOf(id)});
            }
        } catch (Throwable e) {
        } finally {
            closeCursor(c);
        }

        return updated;
    }

    public int markV20CandidateSettlementByDraw(Models.Draw draw) {
        if (draw == null
                || draw.issue == null
                || draw.issue.length() == 0
                || draw.numbers == null
                || draw.numbers.length < 10) {
            return 0;
        }

        int updated = 0;

        for (int p = 1; p <= 10; p++) {
            int actual = getActualNumberByPosition(draw, p);

            if (AppConfig.isValidNumber(actual)) {
                updated += markV20CandidateSettlement(draw.issue, p, actual);
            }
        }

        return updated;
    }

    public void saveV20SettlementFromTrade(Models.Trade t) {
        if (t == null) {
            return;
        }

        try {
            saveRealTradeLedgerFromTrade(t);

            if (t.targetIssue != null
                    && t.targetIssue.length() > 0
                    && AppConfig.isValidPosition(t.position)
                    && AppConfig.isValidNumber(t.actualNumber)) {
                markV20CandidateSettlement(t.targetIssue, t.position, t.actualNumber);
            }
        } catch (Throwable e) {
        }
    }

    public int countV20Table(String table) {
        if (!isV20Table(table)) {
            return 0;
        }

        return countTable(table, null, null);
    }

    public String getV20CauseChainSummaryText() {
        int feature = countV20Table(V20_TABLE_PRE_DRAW_FEATURE);
        int scan = countV20Table(V20_TABLE_FULL_CANDIDATE_SCAN);
        int audit = countV20Table(V20_TABLE_CANDIDATE_AUDIT);
        int cause = countV20Table(V20_TABLE_CAUSAL_FACTOR_MATRIX);
        int position = countV20Table(V20_TABLE_POSITION_CAUSE_MATRIX);
        int teacher = countV20Table(V20_TABLE_TEACHER_NUMBER_MATRIX);
        int rank = countV20Table(V20_TABLE_COUNTERFACTUAL_RANK);
        int third = countV20Table(V20_TABLE_THIRD_GUARD_MATRIX);
        int shadow = countV20Table(V20_TABLE_SHADOW_HEALTH_GATE);
        int freeze = countV20Table(V20_TABLE_FALSE_STRONG_FREEZE);
        int chain = countV20Table(V20_TABLE_DYNAMIC_CAUSE_CHAIN);

        return "V20因链库：特征="
                + feature
                + "｜全候选="
                + scan
                + "｜审计="
                + audit
                + "｜因果="
                + cause
                + "｜位置="
                + position
                + "｜教师号码="
                + teacher
                + "｜Rank18="
                + rank
                + "｜第三码="
                + third
                + "｜SHADOW健康="
                + shadow
                + "｜假强冻结="
                + freeze
                + "｜动态因链="
                + chain
                + "｜说明=DbStore只保存/读取/审计/反推，不直接放行BET";
    }

    private boolean isV20Table(String table) {
        if (table == null || table.length() == 0) {
            return false;
        }

        return V20_TABLE_PRE_DRAW_FEATURE.equals(table)
                || V20_TABLE_PRE_DRAW_NUMBER_SAMPLE.equals(table)
                || V20_TABLE_FULL_CANDIDATE_SCAN.equals(table)
                || V20_TABLE_CANDIDATE_AUDIT.equals(table)
                || V20_TABLE_CAUSAL_FACTOR_MATRIX.equals(table)
                || V20_TABLE_POSITION_CAUSE_MATRIX.equals(table)
                || V20_TABLE_TEACHER_NUMBER_MATRIX.equals(table)
                || V20_TABLE_COUNTERFACTUAL_RANK.equals(table)
                || V20_TABLE_THIRD_GUARD_MATRIX.equals(table)
                || V20_TABLE_RULE_HEALTH_MATRIX.equals(table)
                || V20_TABLE_SHADOW_HEALTH_GATE.equals(table)
                || V20_TABLE_FALSE_STRONG_FREEZE.equals(table)
                || V20_TABLE_SEED_CAUSE_MATRIX.equals(table)
                || V20_TABLE_DYNAMIC_CAUSE_CHAIN.equals(table)
                || V20_TABLE_DECISION_AUDIT.equals(table)
                || V20_TABLE_REAL_TRADE_LEDGER.equals(table);
    }

    private String buildV20RecordKey(String table, ContentValues cv) {
        String target = getCvString(cv, "target_issue", "");
        String state = getCvString(cv, "state_key", "");
        String source = getCvString(cv, "source_key", "");
        String rule = getCvString(cv, "rule_key", "");
        String factor = getCvString(cv, "factor_key", "");
        String seed = getCvString(cv, "seed_key", "");
        String falseStrong = getCvString(cv, "false_strong_key", "");

        int position = getCvInt(cv, "position", 0);
        int number = getCvInt(cv, "number", 0);
        int rank = getCvInt(cv, "rank_no", 0);
        int mask = getCvInt(cv, "k3_mask", 0);

        StringBuilder sb = new StringBuilder();

        sb.append(table);
        sb.append("|T=");
        sb.append(target);
        sb.append("|S=");
        sb.append(cleanKeyPart(state));
        sb.append("|SRC=");
        sb.append(cleanKeyPart(source));
        sb.append("|R=");
        sb.append(cleanKeyPart(rule));
        sb.append("|F=");
        sb.append(cleanKeyPart(factor));
        sb.append("|SEED=");
        sb.append(cleanKeyPart(seed));
        sb.append("|FS=");
        sb.append(cleanKeyPart(falseStrong));
        sb.append("|P=");
        sb.append(position);
        sb.append("|N=");
        sb.append(number);
        sb.append("|RK=");
        sb.append(rank);
        sb.append("|K=");
        sb.append(mask);

        String key = sb.toString();

        if (key.length() > 260) {
            key = key.substring(0, 220) + "|" + String.valueOf(Math.abs(key.hashCode()));
        }

        return key;
    }

    private String buildFalseStrongKeyFromCv(ContentValues cv) {
        if (cv == null) {
            return "";
        }

        String state = getCvString(cv, "state_key", "");
        String source = getCvString(cv, "source_key", "");
        String rule = getCvString(cv, "rule_key", "");
        String same = getCvString(cv, "same_class_key", "");
        int position = getCvInt(cv, "position", 0);
        int rank = getCvInt(cv, "rank_no", 0);
        int mask = getCvInt(cv, "k3_mask", 0);

        return "FS|"
                + cleanKeyPart(state)
                + "|SRC="
                + cleanKeyPart(source)
                + "|R="
                + cleanKeyPart(rule)
                + "|SC="
                + cleanKeyPart(same)
                + "|P"
                + position
                + "|RK"
                + rank
                + "|K"
                + mask;
    }

    /* ============================================================
     * V20 真实BET复盘诊断补丁第2/5：DbStore诊断读取/保存接口
     *
     * 只做数据读取和诊断文本保存：
     * - 不改概率
     * - 不改动态门槛
     * - 不创建BET
     * - 不结算
     * - 不写真实盈亏
     * - 不把HOLD/SHADOW写入真实统计
     * ============================================================ */

    public List<Models.Trade> getRecentTrades(int limit) {
        return getRecentRealBetTradesForDiagnosis(limit);
    }

    public List<Models.Trade> getRecentTradeList(int limit) {
        return getRecentRealBetTradesForDiagnosis(limit);
    }

    public List<Models.Trade> listRecentTrades(int limit) {
        return getRecentRealBetTradesForDiagnosis(limit);
    }

    public List<Models.Trade> getAllRecentTrades(int limit) {
        return getRecentRealBetTradesForDiagnosis(limit);
    }

    public List<Models.Trade> getTrades(int limit) {
        return getRecentRealBetTradesForDiagnosis(limit);
    }

    public List<Models.Trade> getRecentRealBetTradesForDiagnosis(int limit) {
        ArrayList<Models.Trade> out = new ArrayList<Models.Trade>();
        if (limit <= 0) {
            limit = 120;
        }
        Cursor c = null;
        try {
            c =
                    readable()
                            .query(
                                    DbHelper.TABLE_TRADES,
                                    null,
                                    "(action=? OR action=? OR action IS NULL OR action='' OR"
                                        + " action='REAL' OR action='REAL_TRADE' OR action='投注' OR"
                                        + " action='下注') AND (stat_scope=? OR stat_scope IS NULL OR"
                                        + " stat_scope='' OR stat_scope='REAL' OR"
                                        + " stat_scope='REALBET' OR stat_scope='REAL_BET') AND"
                                        + " target_issue IS NOT NULL AND target_issue<>''",
                                    new String[] {
                                        AppConfig.DECISION_BET,
                                        AppConfig.ACTION_BET,
                                        AppConfig.STAT_SCOPE_REAL_TRADE
                                    },
                                    null,
                                    null,
                                    "settled DESC, bet_time DESC, created_at DESC, _id DESC",
                                    String.valueOf(limit));
            while (c.moveToNext()) {
                Models.Trade t = readTrade(c);
                if (t == null) {
                    continue;
                }
                if (!isDiagnosisRealBetTrade(t)) {
                    continue;
                }
                normalizeDiagnosisTradeForRead(t);
                out.add(t);
            }
        } catch (Throwable e) {
        } finally {
            closeCursor(c);
        }
        return out;
    }

    public List<Models.Trade> getRecentSettledRealBetTradesForDiagnosis(int limit) {
        ArrayList<Models.Trade> out = new ArrayList<Models.Trade>();
        if (limit <= 0) {
            limit = 120;
        }
        Cursor c = null;
        try {
            c =
                    readable()
                            .query(
                                    DbHelper.TABLE_TRADES,
                                    null,
                                    "settled=1 AND (result=? OR result=? OR result='HIT' OR"
                                        + " result='LOSE' OR result='LOSS' OR result='命中' OR"
                                        + " result='未中' OR result='不中') AND (action=? OR action=?"
                                        + " OR action IS NULL OR action='' OR action='REAL' OR"
                                        + " action='REAL_TRADE' OR action='投注' OR action='下注') AND"
                                        + " (stat_scope=? OR stat_scope IS NULL OR stat_scope='' OR"
                                        + " stat_scope='REAL' OR stat_scope='REALBET' OR"
                                        + " stat_scope='REAL_BET') AND target_issue IS NOT NULL AND"
                                        + " target_issue<>''",
                                    new String[] {
                                        AppConfig.RESULT_WIN,
                                        AppConfig.RESULT_MISS,
                                        AppConfig.DECISION_BET,
                                        AppConfig.ACTION_BET,
                                        AppConfig.STAT_SCOPE_REAL_TRADE
                                    },
                                    null,
                                    null,
                                    "settled_at DESC, settle_time DESC, bet_time DESC, created_at"
                                        + " DESC, _id DESC",
                                    String.valueOf(limit));
            while (c.moveToNext()) {
                Models.Trade t = readTrade(c);
                if (t == null || !isDiagnosisRealBetTrade(t)) {
                    continue;
                }
                normalizeDiagnosisTradeForRead(t);
                out.add(t);
            }
        } catch (Throwable e) {
        } finally {
            closeCursor(c);
        }
        return out;
    }

    public List<Models.Decision> getRecentBetDecisionsForDiagnosis(int limit) {
        return getRecentDecisionsForDiagnosis(limit, AppConfig.DECISION_BET);
    }

    public List<Models.Decision> getRecentShadowDecisionsForDiagnosis(int limit) {
        return getRecentDecisionsForDiagnosis(limit, AppConfig.DECISION_SHADOW);
    }

    public List<Models.Decision> getRecentDiagnosisSourceDecisions(int limit) {
        ArrayList<Models.Decision> out = new ArrayList<Models.Decision>();
        if (limit <= 0) {
            limit = 420;
        }
        Cursor c = null;
        try {
            c =
                    readable()
                            .query(
                                    DbHelper.TABLE_DECISIONS,
                                    null,
                                    "target_issue IS NOT NULL AND target_issue<>''",
                                    null,
                                    null,
                                    null,
                                    "created_at DESC, _id DESC",
                                    String.valueOf(limit));
            while (c.moveToNext()) {
                Models.Decision d = readDecision(c);
                if (d == null) {
                    continue;
                }
                out.add(d);
            }
        } catch (Throwable e) {
        } finally {
            closeCursor(c);
        }
        return out;
    }

    private List<Models.Decision> getRecentDecisionsForDiagnosis(int limit, String decisionType) {
        ArrayList<Models.Decision> out = new ArrayList<Models.Decision>();
        if (limit <= 0) {
            limit = 240;
        }
        if (decisionType == null) {
            decisionType = "";
        }
        Cursor c = null;
        try {
            c =
                    readable()
                            .query(
                                    DbHelper.TABLE_DECISIONS,
                                    null,
                                    "decision=? AND target_issue IS NOT NULL AND target_issue<>''",
                                    new String[] {decisionType},
                                    null,
                                    null,
                                    "created_at DESC, _id DESC",
                                    String.valueOf(limit));
            while (c.moveToNext()) {
                Models.Decision d = readDecision(c);
                if (d == null) {
                    continue;
                }
                out.add(d);
            }
        } catch (Throwable e) {
        } finally {
            closeCursor(c);
        }
        return out;
    }

    public Models.Draw getDrawByIssueForDiagnosis(String issue) {
        return getDrawByIssue(issue);
    }

    public String refreshRealBetDiagnosisState() {
        try {
            RealBetDiagnosisEngine engine = new RealBetDiagnosisEngine();
            RealBetDiagnosisEngine.DiagnosisReport report = engine.saveDiagnosisState(this);
            if (report == null) {
                setState("real_bet_diagnosis_status", "ERROR");
                setState("real_bet_diagnosis_summary", "真实BET诊断失败：report为空");
                return "真实BET诊断失败：report为空";
            }
            setState(
                    "real_bet_diagnosis_last_refresh_at",
                    String.valueOf(System.currentTimeMillis()));
            return report.summary == null ? "" : report.summary;
        } catch (Throwable e) {
            String err = "真实BET诊断异常：" + String.valueOf(e);
            setState("real_bet_diagnosis_status", "ERROR");
            setState("real_bet_diagnosis_summary", err);
            setState("real_bet_diagnosis_detail", err);
            return err;
        }
    }

    public String buildRealBetDiagnosisSummary() {
        try {
            RealBetDiagnosisEngine engine = new RealBetDiagnosisEngine();
            return engine.buildDiagnosisSummary(this);
        } catch (Throwable e) {
            return "真实BET诊断摘要异常：" + String.valueOf(e);
        }
    }

    public String buildRealBetDiagnosisDetail() {
        try {
            RealBetDiagnosisEngine engine = new RealBetDiagnosisEngine();
            return engine.buildDiagnosisDetail(this);
        } catch (Throwable e) {
            return "真实BET诊断明细异常：" + String.valueOf(e);
        }
    }

    public String buildRealBetDiagnosisCopyText() {
        try {
            RealBetDiagnosisEngine engine = new RealBetDiagnosisEngine();
            return engine.buildDiagnosisCopyText(this);
        } catch (Throwable e) {
            return "真实BET诊断复制文本异常：" + String.valueOf(e);
        }
    }

    public void saveRealBetDiagnosisState(String summary, String detail) {
        long now = System.currentTimeMillis();
        setState("real_bet_diagnosis_status", "OK");
        setState("real_bet_diagnosis_version", "V20_DBSTORE_DIAG_51286437");
        setState("real_bet_diagnosis_updated_at", String.valueOf(now));
        setState("real_bet_diagnosis_summary", summary == null ? "" : summary);
        setState("real_bet_diagnosis_detail", detail == null ? "" : detail);
    }

    public void saveRealBetDiagnosisState(RealBetDiagnosisEngine.DiagnosisReport report) {
        if (report == null) {
            setState("real_bet_diagnosis_status", "ERROR");
            setState("real_bet_diagnosis_summary", "真实BET诊断结果为空");
            return;
        }
        setState("real_bet_diagnosis_status", report.ok == 1 ? "OK" : "ERROR");
        setState("real_bet_diagnosis_version", report.version == null ? "" : report.version);
        setState("real_bet_diagnosis_updated_at", String.valueOf(report.createdAt));
        setState("real_bet_diagnosis_summary", report.summary == null ? "" : report.summary);
        setState("real_bet_diagnosis_detail", report.detail == null ? "" : report.detail);
        setState("real_bet_diagnosis_total", String.valueOf(report.totalBet));
        setState("real_bet_diagnosis_hit", String.valueOf(report.hitBet));
        setState("real_bet_diagnosis_miss", String.valueOf(report.missBet));
        setState("real_bet_diagnosis_win_rate", String.valueOf(report.winRate));
        setState("real_bet_diagnosis_pnl", String.valueOf(report.pnl));
        setState("real_bet_diagnosis_shadow_total", String.valueOf(report.shadowTotal));
        setState("real_bet_diagnosis_shadow_hit", String.valueOf(report.shadowHit));
        setState("real_bet_diagnosis_shadow_rate", String.valueOf(report.shadowHitRate));
        setState(
                "real_bet_diagnosis_main_problem",
                report.mainProblem == null ? "" : report.mainProblem);
        setState(
                "real_bet_diagnosis_next_fix",
                report.nextFixAdvice == null ? "" : report.nextFixAdvice);
    }

    public String getRealBetDiagnosisSummaryState() {
        return getState("real_bet_diagnosis_summary", "");
    }

    public String getRealBetDiagnosisDetailState() {
        return getState("real_bet_diagnosis_detail", "");
    }

    private boolean isDiagnosisRealBetTrade(Models.Trade t) {
        if (t == null) {
            return false;
        }
        if (t.targetIssue == null || t.targetIssue.length() == 0) {
            return false;
        }
        if (!AppConfig.isValidPosition(t.position)) {
            return false;
        }
        if (!AppConfig.isValidNumber(t.n1)
                || !AppConfig.isValidNumber(t.n2)
                || !AppConfig.isValidNumber(t.n3)) {
            return false;
        }
        if (!isDiagnosisRealScope(t.statScope)) {
            return false;
        }
        String action = t.action == null ? "" : t.action.trim();
        if (action.length() == 0) {
            return true;
        }
        return isDiagnosisBetText(action);
    }

    private boolean isDiagnosisBetText(String value) {
        if (value == null) {
            return false;
        }
        String s = value.trim();
        if (s.length() == 0) {
            return false;
        }
        if (AppConfig.DECISION_BET.equals(s) || AppConfig.ACTION_BET.equals(s)) {
            return true;
        }
        return "BET".equalsIgnoreCase(s) || "出手".equals(s) || "下注".equals(s) || "投注".equals(s);
    }

    private boolean isDiagnosisRealScope(String scope) {
        if (scope == null) {
            return true;
        }
        String s = scope.trim();
        if (s.length() == 0) {
            return true;
        }
        if (AppConfig.STAT_SCOPE_REAL_TRADE.equals(s)) {
            return true;
        }
        return "REAL".equals(s)
                || "REAL_TRADE".equals(s)
                || "REALBET".equals(s)
                || "REAL_BET".equals(s);
    }

    private void normalizeDiagnosisTradeForRead(Models.Trade t) {
        if (t == null) {
            return;
        }
        if (t.action == null
                || t.action.length() == 0
                || "REAL".equals(t.action)
                || "REAL_TRADE".equals(t.action)) {
            t.action = AppConfig.ACTION_BET;
        }
        if (t.statScope == null || t.statScope.length() == 0 || "REAL".equals(t.statScope)) {
            t.statScope = AppConfig.STAT_SCOPE_REAL_TRADE;
        }
        if (t.edge == 0.0d && t.finalP > 0.0d && t.dynamicGate > 0.0d) {
            t.edge = t.finalP - t.dynamicGate;
        }
        try {
            t.result = AppConfig.normalizeResult(t.result);
        } catch (Throwable e) {
        }
    }

    // =========================
    // V20 Full Diagnosis Closed Loop DbStore Extension
    // 第7/14：DbStore.java
    // 作用：只做诊断闭环表保障、缓存、读取、保存、复制报告。
    // 红线：不决定 BET，不创建 Trade，不结算，不写真实盈亏，不把 SHADOW 转 BET，不绕过 DecisionGateEngine。
    // =========================

    private static final String DIAG_TABLE_CASE_LOG = "diagnosis_case_log";
    private static final String DIAG_TABLE_MODULE_EFFECT_CACHE = "module_effect_cache";
    private static final String DIAG_TABLE_SHADOW_PROMOTION_CACHE = "shadow_promotion_cache";
    private static final String DIAG_TABLE_DYNAMIC_FREEZE_MATRIX = "dynamic_freeze_matrix";
    private static final String DIAG_TABLE_ADAPTIVE_JUDGE_LOG = "adaptive_judge_log";
    private static final String DIAG_TABLE_FIX_PLAN = "diagnosis_fix_plan";

    private static final String DIAG_RECORD_LATEST = "LATEST";
    private static final int DIAG_DEFAULT_LIMIT = 200;

    public void ensureDiagnosisLoopTablesNow() {
        DbHelper.requireDiagnosisLoopSchemaCP12(readable());
    }

    private void ensureDiagnosticLoopTables(SQLiteDatabase db) {
        DbHelper.requireDiagnosisLoopSchemaCP12(db);
    }
























    public List<ContentValues> getRecentDiagnosisCases(int limit) {
        if (limit <= 0) {
            limit = DIAG_DEFAULT_LIMIT;
        }
        SQLiteDatabase db = readable();
        ensureDiagnosticLoopTables(db);
        return queryDiagnosisTable(
                db, DIAG_TABLE_CASE_LOG, null, null, "created_at DESC, _id DESC", limit);
    }

    public List<ContentValues> getRecentShadowCases(int limit) {
        if (limit <= 0) {
            limit = DIAG_DEFAULT_LIMIT;
        }
        ArrayList<ContentValues> out = new ArrayList<ContentValues>();
        SQLiteDatabase db = readable();
        ensureDiagnosticLoopTables(db);
        List<ContentValues> saved =
                queryDiagnosisTable(
                        db,
                        DIAG_TABLE_CASE_LOG,
                        "(decision_type=? OR source_type=? OR shadow_would_hit<>0)",
                        new String[] {AppConfig.DECISION_SHADOW, "SHADOW"},
                        "created_at DESC, _id DESC",
                        limit);
        out.addAll(saved);
        if (out.size() >= limit) {
            return out;
        }
        try {
            List<Models.Decision> shadows =
                    getRecentShadowDecisionsForDiagnosis(limit - out.size());
            for (int i = 0; shadows != null && i < shadows.size(); i++) {
                Models.Decision d = shadows.get(i);
                if (d == null) {
                    continue;
                }
                ContentValues cv = new ContentValues();
                cv.put(
                        "case_key",
                        "SHADOW_DECISION|" + safeText(d.targetIssue) + "|P" + d.position);
                cv.put("source_type", "SHADOW");
                cv.put("decision_type", AppConfig.DECISION_SHADOW);
                cv.put("created_at", d.createdAt);
                cv.put("updated_at", System.currentTimeMillis());
                cv.put("base_issue", d.baseIssue);
                cv.put("target_issue", d.targetIssue);
                cv.put("position", d.position);
                cv.put("n1", d.n1);
                cv.put("n2", d.n2);
                cv.put("n3", d.n3);
                cv.put("k3_mask", d.k3Mask);
                cv.put("final_p", d.finalP);
                cv.put("dynamic_gate", d.dynamicGate);
                cv.put("edge", d.edge);
                cv.put(
                        "teacher_status",
                        firstNonEmpty(
                                getString(d, "teacherMatrixStatus", ""),
                                getString(d, "teacherStatus", ""),
                                getString(d, "teacherNumberStatus", "")));
                cv.put(
                        "v20_status",
                        firstNonEmpty(
                                getString(d, "dynamicStatus", ""),
                                getString(d, "v20Status", ""),
                                getString(d, "dynamicCauseStatus", "")));
                cv.put(
                        "window_state_key",
                        firstNonEmpty(
                                getString(d, "stateKey", ""),
                                getString(d, "windowStateKey", ""),
                                getString(d, "dynamicWindowStateKey", "")));
                cv.put("summary_text", d.reasonSummary);
                out.add(cv);
            }
        } catch (Throwable e) {
        }
        return out;
    }

    public ContentValues getModuleEffectCache() {
        return getLatestDiagnosisRecord(DIAG_TABLE_MODULE_EFFECT_CACHE);
    }

    public ContentValues getShadowPromotionCache() {
        return getLatestDiagnosisRecord(DIAG_TABLE_SHADOW_PROMOTION_CACHE);
    }

    public ContentValues getDynamicFreezeState() {
        return getLatestDiagnosisRecord(DIAG_TABLE_DYNAMIC_FREEZE_MATRIX);
    }

    public List<ContentValues> getAdaptiveJudgeRecentLogs(int limit) {
        if (limit <= 0) {
            limit = DIAG_DEFAULT_LIMIT;
        }
        SQLiteDatabase db = readable();
        ensureDiagnosticLoopTables(db);
        return queryDiagnosisTable(
                db, DIAG_TABLE_ADAPTIVE_JUDGE_LOG, null, null, "created_at DESC, _id DESC", limit);
    }

    public ContentValues getDiagnosisFixPlan() {
        return getLatestDiagnosisRecord(DIAG_TABLE_FIX_PLAN);
    }

    public long saveDiagnosisCaseLog(ContentValues cv) {
        return saveDiagnosisRecord(DIAG_TABLE_CASE_LOG, "case_key", cv);
    }

    public long saveDiagnosisCaseLog(Object obj) {
        ContentValues cv = diagnosisObjectToValues(obj);
        return saveDiagnosisCaseLog(cv);
    }

    public long saveDiagnosisCaseLog(
            String caseKey,
            String targetIssue,
            String decisionType,
            String summary,
            String detail) {
        ContentValues cv = new ContentValues();
        cv.put("case_key", caseKey);
        cv.put("source_type", "MANUAL");
        cv.put("target_issue", targetIssue);
        cv.put("decision_type", decisionType);
        cv.put("summary_text", summary);
        cv.put("detail_text", detail);
        cv.put("created_at", System.currentTimeMillis());
        cv.put("updated_at", System.currentTimeMillis());
        return saveDiagnosisCaseLog(cv);
    }

    public long saveModuleEffectCache(ContentValues cv) {
        return saveDiagnosisRecord(DIAG_TABLE_MODULE_EFFECT_CACHE, "cache_key", cv);
    }

    public long saveModuleEffectCache(Object obj) {
        ContentValues cv = diagnosisObjectToValues(obj);
        if (!cv.containsKey("cache_key")) {
            String key = getCvString(cv, "module_name", "");
            if (key.length() == 0) {
                key = DIAG_RECORD_LATEST;
            }
            cv.put("cache_key", key);
        }
        return saveModuleEffectCache(cv);
    }

    public long saveModuleEffectCache(
            String moduleName,
            String status,
            int supportCount,
            int hitCount,
            int missCount,
            double hitRate,
            String summary) {
        ContentValues cv = new ContentValues();
        cv.put(
                "cache_key",
                moduleName == null || moduleName.length() == 0 ? DIAG_RECORD_LATEST : moduleName);
        cv.put("module_name", moduleName);
        cv.put("effect_status", status);
        cv.put("support_count", supportCount);
        cv.put("hit_count", hitCount);
        cv.put("miss_count", missCount);
        cv.put("hit_rate", hitRate);
        cv.put("summary_text", summary);
        cv.put("created_at", System.currentTimeMillis());
        cv.put("updated_at", System.currentTimeMillis());
        return saveModuleEffectCache(cv);
    }

    public long saveShadowPromotionCache(ContentValues cv) {
        return saveDiagnosisRecord(DIAG_TABLE_SHADOW_PROMOTION_CACHE, "promotion_key", cv);
    }

    public long saveShadowPromotionCache(Object obj) {
        ContentValues cv = diagnosisObjectToValues(obj);
        if (!cv.containsKey("promotion_key")) {
            String key = getCvString(cv, "domain_key", "");
            if (key.length() == 0) {
                key = DIAG_RECORD_LATEST;
            }
            cv.put("promotion_key", key);
        }
        return saveShadowPromotionCache(cv);
    }

    public long saveShadowPromotionCache(
            String domainKey,
            String domainType,
            String status,
            int totalShadow,
            int wouldHit,
            int wouldMiss,
            double rate,
            String summary) {
        ContentValues cv = new ContentValues();
        cv.put("promotion_key", domainType + "|" + domainKey);
        cv.put("domain_key", domainKey);
        cv.put("domain_type", domainType);
        cv.put("promotion_status", status);
        cv.put("total_shadow", totalShadow);
        cv.put("would_hit", wouldHit);
        cv.put("would_miss", wouldMiss);
        cv.put("would_hit_rate", rate);
        cv.put("summary_text", summary);
        cv.put("created_at", System.currentTimeMillis());
        cv.put("updated_at", System.currentTimeMillis());
        return saveShadowPromotionCache(cv);
    }

    public long saveDynamicFreezeState(ContentValues cv) {
        return saveDiagnosisRecord(DIAG_TABLE_DYNAMIC_FREEZE_MATRIX, "freeze_key", cv);
    }

    public long saveDynamicFreezeState(Object obj) {
        ContentValues cv = diagnosisObjectToValues(obj);
        if (!cv.containsKey("freeze_key")) {
            String key =
                    getCvString(cv, "freeze_type", "") + "|" + getCvString(cv, "freeze_value", "");
            if (key.length() <= 1) {
                key = DIAG_RECORD_LATEST;
            }
            cv.put("freeze_key", key);
        }
        return saveDynamicFreezeState(cv);
    }

    public long saveDynamicFreezeState(
            String freezeKey,
            String freezeType,
            String freezeLevel,
            int frozen,
            String reason,
            String recoverHint) {
        ContentValues cv = new ContentValues();
        cv.put("freeze_key", freezeKey);
        cv.put("freeze_type", freezeType);
        cv.put("freeze_level", freezeLevel);
        cv.put("frozen", frozen);
        cv.put("freeze_reason", reason);
        cv.put("recover_hint", recoverHint);
        cv.put("created_at", System.currentTimeMillis());
        cv.put("updated_at", System.currentTimeMillis());
        return saveDynamicFreezeState(cv);
    }

    public long saveAdaptiveJudgeLog(ContentValues cv) {
        return saveDiagnosisRecord(DIAG_TABLE_ADAPTIVE_JUDGE_LOG, "judge_key", cv);
    }

    public long saveAdaptiveJudgeLog(Object obj) {
        ContentValues cv = diagnosisObjectToValues(obj);
        if (!cv.containsKey("judge_key")) {
            String issue = getCvString(cv, "target_issue", "");
            String pos = getCvString(cv, "position", "");
            String status = getCvString(cv, "judge_status", getCvString(cv, "status", ""));
            cv.put(
                    "judge_key",
                    "JUDGE|"
                            + safeText(issue)
                            + "|P"
                            + safeText(pos)
                            + "|"
                            + safeText(status)
                            + "|"
                            + System.currentTimeMillis());
        }
        return saveAdaptiveJudgeLog(cv);
    }

    public long saveAdaptiveJudgeLog(
            String targetIssue,
            int position,
            int n1,
            int n2,
            int n3,
            String status,
            double judgeScore,
            double judgeRisk,
            String reason) {
        ContentValues cv = new ContentValues();
        cv.put(
                "judge_key",
                "JUDGE|"
                        + safeText(targetIssue)
                        + "|P"
                        + position
                        + "|"
                        + System.currentTimeMillis());
        cv.put("target_issue", targetIssue);
        cv.put("position", position);
        cv.put("n1", n1);
        cv.put("n2", n2);
        cv.put("n3", n3);
        cv.put("judge_status", status);
        cv.put("judge_score", judgeScore);
        cv.put("judge_risk", judgeRisk);
        cv.put("reason_text", reason);
        cv.put("created_at", System.currentTimeMillis());
        cv.put("updated_at", System.currentTimeMillis());
        return saveAdaptiveJudgeLog(cv);
    }

    public long saveDiagnosisFixPlan(ContentValues cv) {
        return saveDiagnosisRecord(DIAG_TABLE_FIX_PLAN, "plan_key", cv);
    }

    public long saveDiagnosisFixPlan(Object obj) {
        ContentValues cv = diagnosisObjectToValues(obj);
        if (!cv.containsKey("plan_key")) {
            String file = getCvString(cv, "file_name", "");
            String priority = getCvString(cv, "priority", "");
            if (file.length() == 0) {
                file = DIAG_RECORD_LATEST;
            }
            cv.put("plan_key", "FIX|" + file + "|" + priority);
        }
        return saveDiagnosisFixPlan(cv);
    }

    public long saveDiagnosisFixPlan(
            String fileName,
            int priority,
            String problemEvidence,
            String whatToChange,
            String expectedEffect) {
        ContentValues cv = new ContentValues();
        cv.put("plan_key", "FIX|" + safeText(fileName) + "|" + priority);
        cv.put("file_name", fileName);
        cv.put("priority", priority);
        cv.put("problem_evidence", problemEvidence);
        cv.put("what_to_change", whatToChange);
        cv.put("expected_effect", expectedEffect);
        cv.put("created_at", System.currentTimeMillis());
        cv.put("updated_at", System.currentTimeMillis());
        return saveDiagnosisFixPlan(cv);
    }

    /**
     * B07 writes one explicit NO_SAMPLE readiness row per diagnosis table when a fresh database has
     * no records. These rows are source_type=SYSTEM_BASELINE_NO_SAMPLE, contain no hit/PnL, never
     * create Decision/Trade/BET, and are excluded from settled real-performance counts.
     */
    public String ensureDiagnosisBaselineRecords(String targetIssue) {
        try {
            ensureDiagnosisLoopTablesNow();
            long now = System.currentTimeMillis();
            String source = AppConfig.B07_DIAG_BASELINE_SOURCE;
            if (countTable(DIAG_TABLE_CASE_LOG, null, null) == 0) {
                ContentValues cv = new ContentValues();
                cv.put("case_key", "B07_BASELINE_NO_SAMPLE");
                cv.put("record_key", "B07_BASELINE_NO_SAMPLE");
                cv.put("source_type", source);
                cv.put("target_issue", safeText(targetIssue));
                cv.put("decision_type", "NO_SAMPLE");
                cv.put("stat_scope", "DIAGNOSIS_READINESS_ONLY");
                cv.put("result", AppConfig.RESULT_WAIT);
                cv.put("summary_text", "逐单诊断表已就绪；当前尚无已结算真实BET样本");
                cv.put("detail_text", "BASELINE_ONLY_NOT_REAL_BET_NOT_SHADOW_NOT_PNL");
                cv.put("created_at", now);
                saveDiagnosisCaseLog(cv);
            }
            if (countTable(DIAG_TABLE_MODULE_EFFECT_CACHE, null, null) == 0) {
                ContentValues cv = new ContentValues();
                cv.put("cache_key", "B07_BASELINE_NO_SAMPLE");
                cv.put("record_key", "B07_BASELINE_NO_SAMPLE");
                cv.put("source_type", source);
                cv.put("module_name", "ALL_MODULES");
                cv.put("effect_status", "INSUFFICIENT_NO_SAMPLE");
                cv.put("summary_text", "模块评分表已就绪；无样本时不伪造支持、命中或责任");
                cv.put("created_at", now);
                saveModuleEffectCache(cv);
            }
            if (countTable(DIAG_TABLE_SHADOW_PROMOTION_CACHE, null, null) == 0) {
                ContentValues cv = new ContentValues();
                cv.put("promotion_key", "B07_BASELINE_NO_SAMPLE");
                cv.put("record_key", "B07_BASELINE_NO_SAMPLE");
                cv.put("source_type", source);
                cv.put("domain_key", "NO_SAMPLE");
                cv.put("domain_type", "READINESS");
                cv.put("promotion_status", "NO_SAMPLE");
                cv.put("summary_text", "影子晋级表已就绪；不以基线行晋级");
                cv.put("created_at", now);
                saveShadowPromotionCache(cv);
            }
            if (countTable(DIAG_TABLE_DYNAMIC_FREEZE_MATRIX, null, null) == 0) {
                ContentValues cv = new ContentValues();
                cv.put("freeze_key", "B07_BASELINE_NO_SAMPLE");
                cv.put("record_key", "B07_BASELINE_NO_SAMPLE");
                cv.put("source_type", source);
                cv.put("freeze_type", "READINESS");
                cv.put("freeze_value", "NO_SAMPLE");
                cv.put("freeze_level", "WATCH");
                cv.put("frozen", 0);
                cv.put("freeze_reason", "NO_SAMPLE_NOT_FREEZE");
                cv.put("recover_hint", "等待真实BET/SHADOW结算样本");
                cv.put("created_at", now);
                saveDynamicFreezeState(cv);
            }
            if (countTable(DIAG_TABLE_ADAPTIVE_JUDGE_LOG, null, null) == 0) {
                ContentValues cv = new ContentValues();
                cv.put("judge_key", "B07_BASELINE_NO_SAMPLE");
                cv.put("record_key", "B07_BASELINE_NO_SAMPLE");
                cv.put("source_type", source);
                cv.put("target_issue", safeText(targetIssue));
                cv.put("judge_status", "NO_SAMPLE_WATCH");
                cv.put("pass", 0);
                cv.put("downgrade_to_shadow", 0);
                cv.put("freeze", 0);
                cv.put("promote_shadow_candidate", 0);
                cv.put("reason_text", "智能审判表已就绪；无样本保持中性观察，不授权BET");
                cv.put("created_at", now);
                saveAdaptiveJudgeLog(cv);
            }
            if (countTable(DIAG_TABLE_FIX_PLAN, null, null) == 0) {
                ContentValues cv = new ContentValues();
                cv.put("plan_key", "B07_BASELINE_NO_SAMPLE");
                cv.put("record_key", "B07_BASELINE_NO_SAMPLE");
                cv.put("source_type", source);
                cv.put("file_name", "B07_RUNTIME_AND_OOS_PIPELINE");
                cv.put("priority", 0);
                cv.put("overall_problem", "NO_SAMPLE");
                cv.put("problem_evidence", "当前无足够已结算样本，不能评价胜率");
                cv.put("what_to_change", "等待真实闭环数据；不得放宽DecisionGate或频率补单");
                cv.put("expected_effect", "形成可复查诊断证据");
                cv.put("summary_text", "修复计划表已就绪；基线不修改概率、门槛、结算或BET");
                cv.put("created_at", now);
                saveDiagnosisFixPlan(cv);
            }
            setState("b07_diagnosis_baseline_status", "READY_NO_SAMPLE_TRUTHFUL");
            setState("b07_diagnosis_baseline_at", String.valueOf(now));
            return "READY_NO_SAMPLE_TRUTHFUL";
        } catch (Throwable e) {
            String err = "ERROR:" + e.getClass().getSimpleName();
            try {
                setState("b07_diagnosis_baseline_status", err);
            } catch (Throwable ignore) {
            }
            return err;
        }
    }

    public String refreshDiagnosisClosedLoopState() {
        try {
            ensureDiagnosisLoopTablesNow();
            ensureDiagnosisBaselineRecords("");
            String source = AppConfig.B07_DIAG_BASELINE_SOURCE;
            String where = "(source_type IS NULL OR source_type<>?)";
            String[] args = new String[] {source};
            StringBuilder sb = new StringBuilder();
            sb.append("诊断闭环存储已刷新");
            sb.append("｜逐单诊断=")
                    .append(countTable(DIAG_TABLE_CASE_LOG, null, null))
                    .append("(真实记录=")
                    .append(countTable(DIAG_TABLE_CASE_LOG, where, args))
                    .append(")");
            sb.append("｜模块评分=")
                    .append(countTable(DIAG_TABLE_MODULE_EFFECT_CACHE, null, null))
                    .append("(真实记录=")
                    .append(countTable(DIAG_TABLE_MODULE_EFFECT_CACHE, where, args))
                    .append(")");
            sb.append("｜SHADOW晋级=")
                    .append(countTable(DIAG_TABLE_SHADOW_PROMOTION_CACHE, null, null))
                    .append("(真实记录=")
                    .append(countTable(DIAG_TABLE_SHADOW_PROMOTION_CACHE, where, args))
                    .append(")");
            sb.append("｜冻结矩阵=")
                    .append(countTable(DIAG_TABLE_DYNAMIC_FREEZE_MATRIX, null, null))
                    .append("(真实记录=")
                    .append(countTable(DIAG_TABLE_DYNAMIC_FREEZE_MATRIX, where, args))
                    .append(")");
            sb.append("｜智能审判=")
                    .append(countTable(DIAG_TABLE_ADAPTIVE_JUDGE_LOG, null, null))
                    .append("(真实记录=")
                    .append(countTable(DIAG_TABLE_ADAPTIVE_JUDGE_LOG, where, args))
                    .append(")");
            sb.append("｜修复计划=")
                    .append(countTable(DIAG_TABLE_FIX_PLAN, null, null))
                    .append("(真实记录=")
                    .append(countTable(DIAG_TABLE_FIX_PLAN, where, args))
                    .append(")");
            sb.append("｜基线=NO_SAMPLE只读，不计胜率/盈亏，不授权BET");
            String text = sb.toString();
            setState("diagnosis_closed_loop_summary", text);
            setState(
                    "diagnosis_closed_loop_updated_at", String.valueOf(System.currentTimeMillis()));
            return text;
        } catch (Throwable e) {
            String err = "诊断闭环存储刷新失败：" + e.getClass().getSimpleName();
            setState("diagnosis_closed_loop_summary", err);
            return err;
        }
    }

    public String buildDiagnosisClosedLoopStorageSummary() {
        String text = getState("diagnosis_closed_loop_summary", "");
        if (text != null && text.length() > 0) {
            return text;
        }
        return refreshDiagnosisClosedLoopState();
    }

    private long saveDiagnosisRecord(String table, String keyColumn, ContentValues input) {
        if (table == null || table.length() == 0 || keyColumn == null || keyColumn.length() == 0) {
            return -1L;
        }
        SQLiteDatabase db = writable();
        ensureDiagnosticLoopTables(db);
        ContentValues cv = input == null ? new ContentValues() : new ContentValues(input);
        long now = System.currentTimeMillis();
        if (!cv.containsKey("created_at") || getCvLong(cv, "created_at", 0L) <= 0L) {
            cv.put("created_at", now);
        }
        cv.put("updated_at", now);
        if (!cv.containsKey(keyColumn) || getCvString(cv, keyColumn, "").length() == 0) {
            cv.put(keyColumn, table + "|" + now);
        }
        if (!cv.containsKey("record_key") || getCvString(cv, "record_key", "").length() == 0) {
            cv.put("record_key", getCvString(cv, keyColumn, table + "|" + now));
        }
        Long id = coordinator.runAtomicWrite(
                "DIAGNOSIS_RECORD_CP12",
                new DatabaseCoordinator.WriteCallable<Long>() {
                    @Override public Long call(SQLiteDatabase ownedDb) {
                        return upsertDiagnosisRecord(ownedDb, table, keyColumn, cv);
                    }
                }, Long.valueOf(-1L));
        return id == null ? -1L : id.longValue();
    }

    private long upsertDiagnosisRecord(
            SQLiteDatabase db, String table, String keyColumn, ContentValues cv) {
        if (db == null || table == null || keyColumn == null || cv == null) {
            return -1L;
        }
        ContentValues filtered = filterContentValuesForTable(db, table, cv);
        String key = getCvString(filtered, keyColumn, "");
        if (key.length() == 0) {
            key = table + "|" + System.currentTimeMillis();
            filtered.put(keyColumn, key);
        }
        long oldId = getDiagnosisRecordId(db, table, keyColumn, key);
        if (oldId > 0L) {
            updateFiltered(db, table, filtered, "_id=?", new String[] {String.valueOf(oldId)});
            return oldId;
        }
        return insertFiltered(db, table, filtered, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private long getDiagnosisRecordId(
            SQLiteDatabase db, String table, String keyColumn, String key) {
        if (db == null || table == null || keyColumn == null || key == null || key.length() == 0) {
            return 0L;
        }
        Cursor c = null;
        try {
            c =
                    db.query(
                            table,
                            new String[] {"_id"},
                            keyColumn + "=?",
                            new String[] {key},
                            null,
                            null,
                            "_id DESC",
                            "1");
            if (c.moveToFirst()) {
                return getLong(c, "_id");
            }
        } catch (Throwable e) {
        } finally {
            closeCursor(c);
        }
        return 0L;
    }

    private ContentValues getLatestDiagnosisRecord(String table) {
        SQLiteDatabase db = readable();
        ensureDiagnosticLoopTables(db);
        List<ContentValues> list =
                queryDiagnosisTable(
                        db, table, null, null, "updated_at DESC, created_at DESC, _id DESC", 1);
        if (list.size() > 0) {
            return list.get(0);
        }
        return new ContentValues();
    }

    private List<ContentValues> queryDiagnosisTable(
            SQLiteDatabase db,
            String table,
            String where,
            String[] args,
            String orderBy,
            int limit) {
        ArrayList<ContentValues> out = new ArrayList<ContentValues>();
        if (db == null || table == null || table.length() == 0) {
            return out;
        }
        Cursor c = null;
        try {
            c =
                    db.query(
                            table,
                            null,
                            where,
                            args,
                            null,
                            null,
                            orderBy,
                            limit > 0 ? String.valueOf(limit) : null);
            while (c.moveToNext()) {
                out.add(cursorToContentValues(c));
            }
        } catch (Throwable e) {
        } finally {
            closeCursor(c);
        }
        return out;
    }

    private ContentValues cursorToContentValues(Cursor c) {
        ContentValues cv = new ContentValues();
        if (c == null) {
            return cv;
        }
        try {
            String[] names = c.getColumnNames();
            for (int i = 0; names != null && i < names.length; i++) {
                String name = names[i];
                if (name == null || name.length() == 0) {
                    continue;
                }
                int type = c.getType(i);
                if (type == Cursor.FIELD_TYPE_INTEGER) {
                    cv.put(name, c.getLong(i));
                } else if (type == Cursor.FIELD_TYPE_FLOAT) {
                    cv.put(name, c.getDouble(i));
                } else if (type == Cursor.FIELD_TYPE_STRING) {
                    cv.put(name, c.getString(i));
                } else if (type == Cursor.FIELD_TYPE_BLOB) {
                    cv.put(name, c.getBlob(i));
                } else {
                    cv.put(name, "");
                }
            }
        } catch (Throwable e) {
        }
        return cv;
    }

    private ContentValues filterContentValuesForTable(
            SQLiteDatabase db, String table, ContentValues cv) {
        ContentValues out = new ContentValues();
        if (db == null || table == null || cv == null) {
            return out;
        }
        HashSet<String> cols = getTableColumns(db, table);
        if (cols == null || cols.size() == 0) {
            return cv;
        }
        for (Map.Entry<String, Object> e : cv.valueSet()) {
            if (e == null || e.getKey() == null) {
                continue;
            }
            if (!cols.contains(e.getKey())) {
                continue;
            }
            Object v = e.getValue();
            if (v == null) {
                out.putNull(e.getKey());
            } else if (v instanceof Integer) {
                out.put(e.getKey(), (Integer) v);
            } else if (v instanceof Long) {
                out.put(e.getKey(), (Long) v);
            } else if (v instanceof Float) {
                out.put(e.getKey(), (Float) v);
            } else if (v instanceof Double) {
                out.put(e.getKey(), (Double) v);
            } else if (v instanceof Boolean) {
                out.put(e.getKey(), ((Boolean) v).booleanValue() ? 1 : 0);
            } else if (v instanceof byte[]) {
                out.put(e.getKey(), (byte[]) v);
            } else {
                out.put(e.getKey(), String.valueOf(v));
            }
        }
        return out;
    }

    private ContentValues diagnosisObjectToValues(Object obj) {
        ContentValues cv = new ContentValues();
        if (obj == null) {
            cv.put("created_at", System.currentTimeMillis());
            cv.put("updated_at", System.currentTimeMillis());
            return cv;
        }
        if (obj instanceof ContentValues) {
            return new ContentValues((ContentValues) obj);
        }
        try {
            Field[] fs = obj.getClass().getFields();
            for (int i = 0; fs != null && i < fs.length; i++) {
                Field f = fs[i];
                if (f == null) {
                    continue;
                }
                String name = f.getName();
                Object v = f.get(obj);
                putObjectToContentValues(cv, javaFieldToDbColumn(name), v);
            }
        } catch (Throwable e) {
        }
        if (!cv.containsKey("summary_text")) {
            String summary = readObjectString(obj, "summary", "");
            if (summary.length() > 0) {
                cv.put("summary_text", summary);
            }
        }
        if (!cv.containsKey("detail_text")) {
            String detail = readObjectString(obj, "detail", "");
            if (detail.length() > 0) {
                cv.put("detail_text", detail);
            }
        }
        if (!cv.containsKey("created_at")) {
            long created = readObjectLong(obj, "createdAt", 0L);
            cv.put("created_at", created > 0L ? created : System.currentTimeMillis());
        }
        cv.put("updated_at", System.currentTimeMillis());
        return cv;
    }

    private void putObjectToContentValues(ContentValues cv, String key, Object v) {
        if (cv == null || key == null || key.length() == 0) {
            return;
        }
        if (v == null) {
            cv.putNull(key);
        } else if (v instanceof Integer) {
            cv.put(key, (Integer) v);
        } else if (v instanceof Long) {
            cv.put(key, (Long) v);
        } else if (v instanceof Float) {
            cv.put(key, (Float) v);
        } else if (v instanceof Double) {
            cv.put(key, (Double) v);
        } else if (v instanceof Boolean) {
            cv.put(key, ((Boolean) v).booleanValue() ? 1 : 0);
        } else {
            String text = String.valueOf(v);
            if (text.length() > 16000) {
                text = text.substring(0, 16000);
            }
            cv.put(key, text);
        }
    }

    private String javaFieldToDbColumn(String name) {
        if (name == null || name.length() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (Character.isUpperCase(ch)) {
                if (i > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(ch));
            } else {
                sb.append(ch);
            }
        }
        String s = sb.toString();
        if ("created_at".equals(s) || "updated_at".equals(s)) {
            return s;
        }
        if ("created_at_millis".equals(s)) {
            return "created_at";
        }
        if ("version".equals(s)) {
            return "ext_json";
        }
        if ("summary".equals(s)) {
            return "summary_text";
        }
        if ("detail".equals(s)) {
            return "detail_text";
        }
        if ("status".equals(s)) {
            return "status";
        }
        return s;
    }

    private Object getObject(Object obj, String field, Object def) {
        if (obj == null || field == null || field.length() == 0) {
            return def;
        }
        try {
            Field f = obj.getClass().getField(field);
            Object v = f.get(obj);
            return v == null ? def : v;
        } catch (Throwable e) {
        }
        return def;
    }

    private String getString(Object obj, String field, String def) {
        if (obj == null || field == null || field.length() == 0) {
            return def == null ? "" : def;
        }
        try {
            Field f = obj.getClass().getField(field);
            Object v = f.get(obj);
            if (v != null) {
                return String.valueOf(v);
            }
        } catch (Throwable e) {
        }
        return def == null ? "" : def;
    }

    private int getInt(Object obj, String field, int def) {
        if (obj == null || field == null || field.length() == 0) {
            return def;
        }
        try {
            Field f = obj.getClass().getField(field);
            Object v = f.get(obj);
            if (v instanceof Number) {
                return ((Number) v).intValue();
            }
            if (v != null) {
                return Integer.parseInt(String.valueOf(v));
            }
        } catch (Throwable e) {
        }
        return def;
    }

    private long getLong(Object obj, String field, long def) {
        if (obj == null || field == null || field.length() == 0) {
            return def;
        }
        try {
            Field f = obj.getClass().getField(field);
            Object v = f.get(obj);
            if (v instanceof Number) {
                return ((Number) v).longValue();
            }
            if (v != null) {
                return Long.parseLong(String.valueOf(v));
            }
        } catch (Throwable e) {
        }
        return def;
    }

    private double getDouble(Object obj, String field, double def) {
        if (obj == null || field == null || field.length() == 0) {
            return def;
        }
        try {
            Field f = obj.getClass().getField(field);
            Object v = f.get(obj);
            if (v instanceof Number) {
                return ((Number) v).doubleValue();
            }
            if (v != null) {
                return Double.parseDouble(String.valueOf(v));
            }
        } catch (Throwable e) {
        }
        return def;
    }

    private String readObjectString(Object obj, String field, String def) {
        if (obj == null || field == null) {
            return def == null ? "" : def;
        }
        try {
            Field f = obj.getClass().getField(field);
            Object v = f.get(obj);
            if (v != null) {
                return String.valueOf(v);
            }
        } catch (Throwable e) {
        }
        return def == null ? "" : def;
    }

    private long readObjectLong(Object obj, String field, long def) {
        if (obj == null || field == null) {
            return def;
        }
        try {
            Field f = obj.getClass().getField(field);
            Object v = f.get(obj);
            if (v instanceof Number) {
                return ((Number) v).longValue();
            }
            if (v != null) {
                return Long.parseLong(String.valueOf(v));
            }
        } catch (Throwable e) {
        }
        return def;
    }

    private String firstNonEmpty(String a, String b, String c) {
        if (a != null && a.length() > 0) {
            return a;
        }
        if (b != null && b.length() > 0) {
            return b;
        }
        return c == null ? "" : c;
    }

    private String firstNonEmpty(String a, String b, String c, String d) {
        if (a != null && a.length() > 0) {
            return a;
        }
        if (b != null && b.length() > 0) {
            return b;
        }
        if (c != null && c.length() > 0) {
            return c;
        }
        return d == null ? "" : d;
    }

    private String firstNonEmpty(String a, String b, String c, String d, String e) {
        if (a != null && a.length() > 0) {
            return a;
        }
        if (b != null && b.length() > 0) {
            return b;
        }
        if (c != null && c.length() > 0) {
            return c;
        }
        if (d != null && d.length() > 0) {
            return d;
        }
        return e == null ? "" : e;
    }

    private String safeText(String s) {
        return s == null ? "" : s;
    }

    /*
     * 链路修改升级V2存储接口。
     * 只负责建列、保存、读取、聚合统计与账本保护；不计算概率、不救援BET、不新增BET。
     */
    private void ensureLinkModificationV2Ready(SQLiteDatabase db) {
        if (db == null) return;
        if (!coordinator.beginLinkSchemaEnsure()) return;
        boolean ok = false;
        try {
            execQuiet(
                    db,
                    "CREATE TABLE IF NOT EXISTS "
                            + TABLE_V20_FREQUENCY_STATE
                            + " (state_key TEXT PRIMARY KEY)");
            execQuiet(
                    db,
                    "CREATE TABLE IF NOT EXISTS "
                            + TABLE_V20_LAYER_SNAPSHOT
                            + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, snapshot_key TEXT NOT NULL"
                            + " UNIQUE)");
            execQuiet(
                    db,
                    "CREATE TABLE IF NOT EXISTS "
                            + TABLE_V20_BACKGROUND_CURSOR
                            + " (task_key TEXT PRIMARY KEY)");
            execQuiet(
                    db,
                    "CREATE TABLE IF NOT EXISTS "
                            + TABLE_V20_SHADOW_DOMAIN_STAT
                            + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, domain_key TEXT NOT NULL"
                            + " UNIQUE)");
            execQuiet(
                    db,
                    "CREATE TABLE IF NOT EXISTS "
                            + TABLE_V20_LINK_CANDIDATE_SNAPSHOT
                            + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, snapshot_key TEXT NOT NULL"
                            + " UNIQUE)");
            addColumnArray(
                    db,
                    TABLE_V20_FREQUENCY_STATE,
                    new String[] {
                        "as_of_issue TEXT",
                        "status TEXT",
                        "version TEXT",
                        "frequency_50 REAL DEFAULT 0",
                        "frequency_100 REAL DEFAULT 0",
                        "frequency_300 REAL DEFAULT 0",
                        "frequency_debt REAL DEFAULT 0",
                        "valid_issue_count INTEGER DEFAULT 0",
                        "real_bet_count INTEGER DEFAULT 0",
                        "qualified_opportunity_count INTEGER DEFAULT 0",
                        "recent_probe_count INTEGER DEFAULT 0",
                        "recent_lose_count INTEGER DEFAULT 0",
                        "debt_frozen INTEGER DEFAULT 0",
                        "updated_at INTEGER DEFAULT 0"
                    });
            addColumnArray(
                    db,
                    TABLE_V20_LAYER_SNAPSHOT,
                    new String[] {
                        "layer TEXT",
                        "as_of_issue TEXT",
                        "max_included_issue TEXT",
                        "schema_version TEXT",
                        "strategy_version TEXT",
                        "freshness_status TEXT",
                        "payload TEXT",
                        "sample_count INTEGER DEFAULT 0",
                        "created_at INTEGER DEFAULT 0",
                        "expires_at INTEGER DEFAULT 0"
                    });
            addColumnArray(
                    db,
                    TABLE_V20_BACKGROUND_CURSOR,
                    new String[] {
                        "cursor_issue TEXT",
                        "target_issue TEXT",
                        "status TEXT",
                        "version TEXT",
                        "processed_count INTEGER DEFAULT 0",
                        "pending_count INTEGER DEFAULT 0",
                        "updated_at INTEGER DEFAULT 0"
                    });
            addColumnArray(
                    db,
                    TABLE_V20_SHADOW_DOMAIN_STAT,
                    new String[] {
                        "parent_domain_key TEXT",
                        "promotion_status TEXT",
                        "version TEXT",
                        "sample_count INTEGER DEFAULT 0",
                        "hit_count INTEGER DEFAULT 0",
                        "miss_count INTEGER DEFAULT 0",
                        "recent_sample_count INTEGER DEFAULT 0",
                        "recent_hit_count INTEGER DEFAULT 0",
                        "misleading_streak INTEGER DEFAULT 0",
                        "hit_rate REAL DEFAULT 0",
                        "recent_hit_rate REAL DEFAULT 0",
                        "posterior_mean REAL DEFAULT 0",
                        "wilson_lower_bound REAL DEFAULT 0",
                        "drift_score REAL DEFAULT 0",
                        "last_settled_issue TEXT",
                        "updated_at INTEGER DEFAULT 0"
                    });
            String[] decisionLike =
                    new String[] {
                        DbHelper.TABLE_DECISIONS,
                        DbHelper.TABLE_TRADES,
                        DbHelper.TABLE_SHADOW,
                        DbHelper.TABLE_FULL_CANDIDATE_SCAN,
                        DbHelper.TABLE_CANDIDATE_AUDIT,
                        DbHelper.TABLE_DYNAMIC_CAUSE_CHAIN,
                        DbHelper.TABLE_DECISION_AUDIT,
                        DbHelper.TABLE_V20_CLOSED_LOOP,
                        TABLE_MAIN_CHAIN_CANDIDATE_SNAPSHOT,
                        TABLE_V20_LINK_CANDIDATE_SNAPSHOT,
                        DbHelper.TABLE_REAL_TRADE_LEDGER
                    };
            for (int i = 0; i < decisionLike.length; i++)
                addColumnArray(db, decisionLike[i], linkModificationV2ColumnDefs());
            execQuiet(
                    db,
                    "CREATE INDEX IF NOT EXISTS idx_store_v20_layer_issue ON "
                            + TABLE_V20_LAYER_SNAPSHOT
                            + "(layer,as_of_issue)");
            execQuiet(
                    db,
                    "CREATE INDEX IF NOT EXISTS idx_store_v20_shadow_domain ON "
                            + TABLE_V20_SHADOW_DOMAIN_STAT
                            + "(promotion_status,sample_count)");
            execQuiet(
                    db,
                    "CREATE UNIQUE INDEX IF NOT EXISTS idx_store_v20_candidate_unique ON "
                            + TABLE_V20_LINK_CANDIDATE_SNAPSHOT
                            + "(target_issue,candidate_key)");
            clearColumnCache();
            ok = true;
        } finally {
            if (!ok) coordinator.markLinkSchemaFailed();
        }
    }

    private String[] linkModificationV2ColumnDefs() {
        return new String[] {
            "base_gate REAL DEFAULT 0",
            "risk_adjusted_gate REAL DEFAULT 0",
            "frequency_gate_adjust REAL DEFAULT 0",
            "effective_probe_gate REAL DEFAULT 0",
            "raw_edge REAL DEFAULT 0",
            "effective_probe_edge REAL DEFAULT 0",
            "frequency_50 REAL DEFAULT 0",
            "frequency_100 REAL DEFAULT 0",
            "frequency_300 REAL DEFAULT 0",
            "frequency_debt_score REAL DEFAULT 0",
            "frequency_support_score REAL DEFAULT 0",
            "qualified_probe_score REAL DEFAULT 0",
            "shadow_promotion_score REAL DEFAULT 0",
            "need_supply_score REAL DEFAULT 0",
            "counter_risk REAL DEFAULT 0",
            "qualified_probe INTEGER DEFAULT 0",
            "real_trade INTEGER DEFAULT 0",
            "coverage_available INTEGER DEFAULT 0",
            "third_quality_available INTEGER DEFAULT 0",
            "promotion_domain_available INTEGER DEFAULT 0",
            "frequency_state TEXT",
            "frequency_state_version TEXT",
            "shadow_promotion_domain TEXT",
            "decision_path TEXT",
            "bet_domain TEXT",
            "probe_reject_reason TEXT",
            "probe_eligibility_version TEXT",
            "rule_health_status TEXT",
            "snapshot_freshness TEXT",
            "warm_profile_freshness TEXT",
            "cold_profile_freshness TEXT",
            "deep_profile_freshness TEXT",
            "decision_version_v2 TEXT",
            "evidence_as_of_issue TEXT",
            "snapshot_as_of_issue TEXT",
            "snapshot_created_at INTEGER DEFAULT 0",
            "frequency_state_updated_at INTEGER DEFAULT 0",
            "frequency_floor_required_count INTEGER DEFAULT 0",
            "frequency_floor_target_count INTEGER DEFAULT 0",
            "frequency_floor_actual_count INTEGER DEFAULT 0",
            "frequency_floor_debt_count INTEGER DEFAULT 0",
            "frequency_target_debt_count INTEGER DEFAULT 0",
            "frequency_floor_mandatory INTEGER DEFAULT 0",
            "frequency_floor_no_upper_cap INTEGER DEFAULT 1",
            "frequency_floor_rank_score REAL DEFAULT 0",
            "frequency_floor_version TEXT"
        };
    }

    public void ensureLinkModificationV2TablesNow() {
        try {
            ensureLinkModificationV2Ready(writable());
        } catch (Throwable e) {
        }
    }

    public void saveFrequencyState(Models.FrequencyState x) {
        if (x == null) return;
        SQLiteDatabase db = writable();
        ensureLinkModificationV2Ready(db);
        ContentValues cv = new ContentValues();
        cv.put("state_key", x.stateKey == null || x.stateKey.length() == 0 ? "MAIN" : x.stateKey);
        cv.put("as_of_issue", Models.safeIssue(x.asOfIssue));
        cv.put("status", Models.safeText(x.status));
        cv.put("version", Models.safeText(x.version));
        cv.put("frequency_20", AppConfig.clampProbability(x.frequency20));
        cv.put("frequency_50", AppConfig.clampProbability(x.frequency50));
        cv.put("frequency_100", AppConfig.clampProbability(x.frequency100));
        cv.put("frequency_300", AppConfig.clampProbability(x.frequency300));
        cv.put("frequency_debt", AppConfig.clampFrequencyDebt(x.frequencyDebt));
        cv.put("valid_issue_count", Math.max(0, x.validIssueCount));
        cv.put("real_bet_count", Math.max(0, x.realBetCount));
        cv.put("qualified_opportunity_count", Math.max(0, x.qualifiedOpportunityCount));
        cv.put("recent_probe_count", Math.max(0, x.recentProbeCount));
        cv.put("recent_lose_count", Math.max(0, x.recentLoseCount));
        cv.put("debt_frozen", x.debtFrozen == 0 ? 0 : 1);
        cv.put("required_floor_bet_count", Math.max(0, x.requiredFloorBetCount));
        cv.put("target_bet_count", Math.max(0, x.targetBetCount));
        cv.put("floor_debt_count", Math.max(0, x.floorDebtCount));
        cv.put("target_debt_count", Math.max(0, x.targetDebtCount));
        cv.put("no_upper_cap", x.noUpperCap == 0 ? 0 : 1);
        cv.put("observed_20", Math.max(0, x.observed20));
        cv.put("observed_50", Math.max(0, x.observed50));
        cv.put("observed_100", Math.max(0, x.observed100));
        cv.put("observed_300", Math.max(0, x.observed300));
        cv.put("bet_count_20", Math.max(0, x.betCount20));
        cv.put("bet_count_50", Math.max(0, x.betCount50));
        cv.put("bet_count_100", Math.max(0, x.betCount100));
        cv.put("bet_count_300", Math.max(0, x.betCount300));
        cv.put("floor_debt_20", Math.max(0, x.floorDebt20));
        cv.put("floor_debt_50", Math.max(0, x.floorDebt50));
        cv.put("floor_debt_100", Math.max(0, x.floorDebt100));
        cv.put("floor_debt_300", Math.max(0, x.floorDebt300));
        cv.put("target_debt_20", Math.max(0, x.targetDebt20));
        cv.put("target_debt_50", Math.max(0, x.targetDebt50));
        cv.put("target_debt_100", Math.max(0, x.targetDebt100));
        cv.put("target_debt_300", Math.max(0, x.targetDebt300));
        cv.put("qualified_floor_candidate_count", Math.max(0, x.qualifiedFloorCandidateCount));
        cv.put("urgent_window", Models.safeText(x.urgentWindow));
        cv.put("issues_since_last_real_bet", Math.max(0, x.issuesSinceLastRealBet));
        cv.put("issues_since_last_floor_bet", Math.max(0, x.issuesSinceLastQualifiedFloorBet));
        cv.put("consecutive_floor_bets", Math.max(0, x.consecutiveQualifiedFloorBets));
        cv.put("floor_pacing_min_gap", x.floorPacingMinGap);
        cv.put("floor_pacing_ready", x.floorPacingReady == 0 ? 0 : 1);
        cv.put("floor_pacing_reason", Models.safeText(x.floorPacingReason));
        cv.put("updated_at", x.updatedAt > 0L ? x.updatedAt : System.currentTimeMillis());
        insertFiltered(db, TABLE_V20_FREQUENCY_STATE, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public Models.FrequencyState loadFrequencyState() {
        return loadFrequencyState("MAIN");
    }

    public Models.FrequencyState loadFrequencyState(String stateKey) {
        Models.FrequencyState x = new Models.FrequencyState();
        String key = stateKey == null || stateKey.length() == 0 ? "MAIN" : stateKey;
        x.stateKey = key;
        Cursor c = null;
        try {
            SQLiteDatabase db = readable();
            ensureLinkModificationV2Ready(db);
            c =
                    db.query(
                            TABLE_V20_FREQUENCY_STATE,
                            null,
                            "state_key=?",
                            new String[] {key},
                            null,
                            null,
                            null,
                            "1");
            if (c != null && c.moveToFirst()) {
                x.asOfIssue = getString(c, "as_of_issue");
                x.status = getString(c, "status");
                x.version = getString(c, "version");
                x.frequency20 = getDouble(c, "frequency_20");
                x.frequency50 = getDouble(c, "frequency_50");
                x.frequency100 = getDouble(c, "frequency_100");
                x.frequency300 = getDouble(c, "frequency_300");
                x.frequencyDebt = getDouble(c, "frequency_debt");
                x.validIssueCount = getInt(c, "valid_issue_count");
                x.realBetCount = getInt(c, "real_bet_count");
                x.qualifiedOpportunityCount = getInt(c, "qualified_opportunity_count");
                x.recentProbeCount = getInt(c, "recent_probe_count");
                x.recentLoseCount = getInt(c, "recent_lose_count");
                x.debtFrozen = getInt(c, "debt_frozen");
                x.requiredFloorBetCount = getInt(c, "required_floor_bet_count");
                x.targetBetCount = getInt(c, "target_bet_count");
                x.floorDebtCount = getInt(c, "floor_debt_count");
                x.targetDebtCount = getInt(c, "target_debt_count");
                x.noUpperCap = getInt(c, "no_upper_cap");
                x.observed20 = getInt(c, "observed_20");
                x.observed50 = getInt(c, "observed_50");
                x.observed100 = getInt(c, "observed_100");
                x.observed300 = getInt(c, "observed_300");
                x.betCount20 = getInt(c, "bet_count_20");
                x.betCount50 = getInt(c, "bet_count_50");
                x.betCount100 = getInt(c, "bet_count_100");
                x.betCount300 = getInt(c, "bet_count_300");
                x.floorDebt20 = getInt(c, "floor_debt_20");
                x.floorDebt50 = getInt(c, "floor_debt_50");
                x.floorDebt100 = getInt(c, "floor_debt_100");
                x.floorDebt300 = getInt(c, "floor_debt_300");
                x.targetDebt20 = getInt(c, "target_debt_20");
                x.targetDebt50 = getInt(c, "target_debt_50");
                x.targetDebt100 = getInt(c, "target_debt_100");
                x.targetDebt300 = getInt(c, "target_debt_300");
                x.qualifiedFloorCandidateCount = getInt(c, "qualified_floor_candidate_count");
                x.urgentWindow = getString(c, "urgent_window");
                x.issuesSinceLastRealBet = getInt(c, "issues_since_last_real_bet");
                x.issuesSinceLastQualifiedFloorBet = getInt(c, "issues_since_last_floor_bet");
                x.consecutiveQualifiedFloorBets = getInt(c, "consecutive_floor_bets");
                x.floorPacingMinGap = getInt(c, "floor_pacing_min_gap");
                x.floorPacingReady = getInt(c, "floor_pacing_ready");
                x.floorPacingReason = getString(c, "floor_pacing_reason");
                x.updatedAt = getLong(c, "updated_at");
            }
        } catch (Throwable e) {
        } finally {
            closeCursor(c);
        }
        if (x.status == null || x.status.length() == 0) x.status = AppConfig.FREQ_STATE_NORMAL;
        if (x.version == null || x.version.length() == 0)
            x.version = DBSTORE_LINK_MODIFICATION_V2_VERSION;
        return x;
    }

    public void saveLayerSnapshot(Models.LayerSnapshot x) {
        try (DatabaseCoordinator.WriterGuardCP3 cp3WriterGuard4 = DatabaseCoordinator.acquireWriterGuardCP3()) {
            if (x == null || x.snapshotKey == null || x.snapshotKey.length() == 0) return;
            if (x.maxIncludedIssue != null
                    && x.asOfIssue != null
                    && x.asOfIssue.length() > 0
                    && x.maxIncludedIssue.length() > 0
                    && x.maxIncludedIssue.compareTo(x.asOfIssue) > 0) return;
            SQLiteDatabase db = writable();
            ensureLinkModificationV2Ready(db);
            ContentValues cv = new ContentValues();
            cv.put("snapshot_key", x.snapshotKey);
            cv.put("layer", Models.safeText(x.layer));
            cv.put("as_of_issue", Models.safeIssue(x.asOfIssue));
            cv.put("max_included_issue", Models.safeIssue(x.maxIncludedIssue));
            cv.put("schema_version", Models.safeText(x.schemaVersion));
            cv.put("strategy_version", Models.safeText(x.strategyVersion));
            cv.put("freshness_status", Models.safeText(x.freshnessStatus));
            cv.put("payload", Models.safeText(x.payload));
            cv.put("sample_count", Math.max(0, x.sampleCount));
            cv.put("created_at", x.createdAt > 0L ? x.createdAt : System.currentTimeMillis());
            cv.put("expires_at", Math.max(0L, x.expiresAt));
            insertFiltered(db, TABLE_V20_LAYER_SNAPSHOT, cv, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    public void saveWarmSnapshot(Models.LayerSnapshot x) {
        if (x != null) {
            x.layer = "WARM";
            saveLayerSnapshot(x);
        }
    }

    public void saveColdSnapshot(Models.LayerSnapshot x) {
        if (x != null) {
            x.layer = "COLD";
            saveLayerSnapshot(x);
        }
    }

    public void saveDeepSnapshot(Models.LayerSnapshot x) {
        if (x != null) {
            x.layer = "DEEP";
            saveLayerSnapshot(x);
        }
    }

    public Models.LayerSnapshot loadWarmSnapshot() {
        Models.LayerSnapshot x = loadLayerSnapshot(AppConfig.SNAPSHOT_KEY_TEACHER_WARM);
        return x != null ? x : loadLatestLayerSnapshot("WARM");
    }

    public Models.LayerSnapshot loadColdSnapshot() {
        Models.LayerSnapshot x = loadLayerSnapshot(AppConfig.SNAPSHOT_KEY_TEACHER_COLD);
        return x != null ? x : loadLatestLayerSnapshot("COLD");
    }

    public Models.LayerSnapshot loadDeepSnapshot() {
        Models.LayerSnapshot x = loadLayerSnapshot(AppConfig.SNAPSHOT_KEY_TEACHER_DEEP);
        return x != null ? x : loadLatestLayerSnapshot("DEEP");
    }

    public Models.LayerSnapshot loadTeacherWarmSnapshot() {
        return loadLayerSnapshot(AppConfig.SNAPSHOT_KEY_TEACHER_WARM);
    }

    public Models.LayerSnapshot loadTeacherColdSnapshot() {
        return loadLayerSnapshot(AppConfig.SNAPSHOT_KEY_TEACHER_COLD);
    }

    public Models.LayerSnapshot loadTeacherDeepSnapshot() {
        return loadLayerSnapshot(AppConfig.SNAPSHOT_KEY_TEACHER_DEEP);
    }

    public Models.LayerSnapshot loadLatestLayerSnapshot(String layer) {
        Cursor c = null;
        try {
            SQLiteDatabase db = readable();
            ensureLinkModificationV2Ready(db);
            c =
                    db.query(
                            TABLE_V20_LAYER_SNAPSHOT,
                            null,
                            "layer=?",
                            new String[] {Models.safeText(layer)},
                            null,
                            null,
                            "created_at DESC",
                            "1");
            if (c != null && c.moveToFirst()) {
                Models.LayerSnapshot x = new Models.LayerSnapshot();
                x.snapshotKey = getString(c, "snapshot_key");
                x.layer = getString(c, "layer");
                x.asOfIssue = getString(c, "as_of_issue");
                x.maxIncludedIssue = getString(c, "max_included_issue");
                x.schemaVersion = getString(c, "schema_version");
                x.strategyVersion = getString(c, "strategy_version");
                x.freshnessStatus = getString(c, "freshness_status");
                x.payload = getString(c, "payload");
                x.sampleCount = getInt(c, "sample_count");
                x.createdAt = getLong(c, "created_at");
                x.expiresAt = getLong(c, "expires_at");
                return x;
            }
        } catch (Throwable e) {
        } finally {
            closeCursor(c);
        }
        return null;
    }

    public void saveBackgroundCursor(Models.BackgroundCursor x) {
        try (DatabaseCoordinator.WriterGuardCP3 cp3WriterGuard5 = DatabaseCoordinator.acquireWriterGuardCP3()) {
            if (x == null || x.taskKey == null || x.taskKey.length() == 0) return;
            SQLiteDatabase db = writable();
            ensureLinkModificationV2Ready(db);
            ContentValues cv = new ContentValues();
            cv.put("task_key", x.taskKey);
            cv.put("cursor_issue", Models.safeIssue(x.cursorIssue));
            cv.put("target_issue", Models.safeIssue(x.targetIssue));
            cv.put("status", Models.safeText(x.status));
            cv.put("version", Models.safeText(x.version));
            cv.put("processed_count", Math.max(0, x.processedCount));
            cv.put("pending_count", Math.max(0, x.pendingCount));
            cv.put("updated_at", x.updatedAt > 0L ? x.updatedAt : System.currentTimeMillis());
            insertFiltered(db, TABLE_V20_BACKGROUND_CURSOR, cv, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    public Models.BackgroundCursor loadBackgroundCursor(String taskKey) {
        Models.BackgroundCursor x = new Models.BackgroundCursor();
        x.taskKey = Models.safeText(taskKey);
        Cursor c = null;
        try {
            SQLiteDatabase db = readable();
            ensureLinkModificationV2Ready(db);
            c =
                    db.query(
                            TABLE_V20_BACKGROUND_CURSOR,
                            null,
                            "task_key=?",
                            new String[] {x.taskKey},
                            null,
                            null,
                            null,
                            "1");
            if (c != null && c.moveToFirst()) {
                x.cursorIssue = getString(c, "cursor_issue");
                x.targetIssue = getString(c, "target_issue");
                x.status = getString(c, "status");
                x.version = getString(c, "version");
                x.processedCount = getInt(c, "processed_count");
                x.pendingCount = getInt(c, "pending_count");
                x.updatedAt = getLong(c, "updated_at");
            }
        } catch (Throwable e) {
        } finally {
            closeCursor(c);
        }
        return x;
    }

    public int batchSaveCandidateSnapshot(List<Models.ComboCandidate> candidates) {
        if (candidates == null || candidates.size() == 0) return 0;
        SQLiteDatabase db = writable();
        ensureLinkModificationV2Ready(db);
        int saved = 0;
        db.beginTransaction();
        try {
            int limit = Math.min(candidates.size(), AppConfig.REALTIME_PROBABILITY_AUDIT_LIMIT);
            for (int i = 0; i < limit; i++) {
                Models.ComboCandidate c = candidates.get(i);
                if (c == null || c.targetIssue == null || c.targetIssue.length() == 0) continue;
                ContentValues cv = buildLinkCandidateValues(c);
                cv.put(
                        "snapshot_key",
                        "LINK|"
                                + c.targetIssue
                                + "|"
                                + Math.abs(Models.safeText(c.stateKey).hashCode())
                                + "|"
                                + c.position
                                + "|"
                                + c.k3Mask);
                cv.put("candidate_key", c.targetIssue + "|P" + c.position + "|K" + c.k3Mask);
                cv.put("candidate_rank", c.rank);
                cv.put("target_issue", c.targetIssue);
                cv.put("base_issue", c.baseIssue);
                cv.put("created_at", c.createdAt > 0L ? c.createdAt : System.currentTimeMillis());
                if (insertFiltered(
                                db,
                                TABLE_V20_LINK_CANDIDATE_SNAPSHOT,
                                cv,
                                SQLiteDatabase.CONFLICT_REPLACE)
                        >= 0L) saved++;
            }
            db.setTransactionSuccessful();
        } catch (Throwable e) {
        } finally {
            try {
                db.endTransaction();
            } catch (Throwable ignore) {
            }
        }
        return saved;
    }

    public int batchSaveShadowFields(List<Models.ComboCandidate> candidates) {
        if (candidates == null || candidates.size() == 0) return 0;
        SQLiteDatabase db = writable();
        ensureLinkModificationV2Ready(db);
        int changed = 0;
        db.beginTransaction();
        try {
            for (int i = 0; i < candidates.size(); i++) {
                Models.ComboCandidate c = candidates.get(i);
                if (c == null || c.targetIssue == null || c.targetIssue.length() == 0) continue;
                ContentValues cv = buildLinkCandidateValues(c);
                changed +=
                        updateFiltered(
                                db,
                                DbHelper.TABLE_SHADOW,
                                cv,
                                "target_issue=? AND position=? AND k3_mask=?",
                                new String[] {
                                    c.targetIssue,
                                    String.valueOf(c.position),
                                    String.valueOf(c.k3Mask)
                                });
            }
            db.setTransactionSuccessful();
        } catch (Throwable e) {
        } finally {
            try {
                db.endTransaction();
            } catch (Throwable ignore) {
            }
        }
        return changed;
    }

    private ContentValues buildLinkCandidateValues(Object c) {
        ensureSafePreDrawPromotionDomain(c);
        ContentValues cv = new ContentValues();
        cv.put("base_gate", getDouble(c, "baseGate", 0.0d));
        cv.put(
                "risk_adjusted_gate",
                getDouble(c, "riskAdjustedGate", getDouble(c, "dynamicGate", 0.0d)));
        cv.put("frequency_gate_adjust", getDouble(c, "frequencyGateAdjust", 0.0d));
        cv.put("effective_probe_gate", getDouble(c, "effectiveProbeGate", 0.0d));
        cv.put("raw_edge", getDouble(c, "rawEdge", getDouble(c, "edge", 0.0d)));
        cv.put("effective_probe_edge", getDouble(c, "effectiveProbeEdge", 0.0d));
        cv.put("frequency_50", getDouble(c, "realBetFrequency50", 0.0d));
        cv.put("frequency_100", getDouble(c, "realBetFrequency100", 0.0d));
        cv.put("frequency_300", getDouble(c, "realBetFrequency300", 0.0d));
        cv.put("frequency_debt_score", getDouble(c, "frequencyDebtScore", 0.0d));
        cv.put("frequency_support_score", getDouble(c, "frequencySupportScore", 0.0d));
        cv.put("qualified_probe_score", getDouble(c, "qualifiedProbeScore", 0.0d));
        cv.put("shadow_promotion_score", getDouble(c, "shadowPromotionScore", 0.0d));
        cv.put(
                "need_supply_score",
                getDouble(c, "needSupplyScore", getDouble(c, "needSupplyMatchScore", 0.0d)));
        cv.put(
                "counter_risk",
                getDouble(c, "counterRisk", getDouble(c, "counterfactualWhyNotRisk", 0.0d)));
        cv.put("qualified_probe", getInt(c, "qualifiedProbe", 0));
        cv.put("real_trade", getInt(c, "realTrade", 0));
        cv.put("coverage_available", getInt(c, "coverageAvailable", 0));
        cv.put("third_quality_available", getInt(c, "thirdQualityAvailable", 0));
        cv.put("promotion_domain_available", getInt(c, "promotionDomainAvailable", 0));
        cv.put("frequency_state", getString(c, "frequencyState", ""));
        cv.put(
                "frequency_state_version",
                getString(c, "frequencyStateVersion", DBSTORE_LINK_MODIFICATION_V2_VERSION));
        cv.put("shadow_promotion_domain", getString(c, "shadowPromotionDomain", ""));
        cv.put("decision_path", getString(c, "decisionPath", ""));
        cv.put("bet_domain", getString(c, "betDomain", getString(c, "finalDecisionBetDomain", "")));
        cv.put("probe_reject_reason", getString(c, "probeRejectReason", ""));
        cv.put("probe_eligibility_version", getString(c, "probeEligibilityVersion", ""));
        cv.put("rule_health_status", getString(c, "ruleHealthStatus", ""));
        cv.put("snapshot_freshness", getString(c, "snapshotFreshness", ""));
        cv.put("warm_profile_freshness", getString(c, "warmProfileFreshness", ""));
        cv.put("cold_profile_freshness", getString(c, "coldProfileFreshness", ""));
        cv.put("deep_profile_freshness", getString(c, "deepProfileFreshness", ""));
        cv.put(
                "decision_version_v2",
                getString(c, "decisionVersionV2", DBSTORE_LINK_MODIFICATION_V2_VERSION));
        cv.put("evidence_as_of_issue", getString(c, "evidenceAsOfIssue", ""));
        cv.put("snapshot_as_of_issue", getString(c, "snapshotAsOfIssue", ""));
        cv.put("snapshot_created_at", getLong(c, "snapshotCreatedAt", 0L));
        cv.put("frequency_state_updated_at", getLong(c, "frequencyStateUpdatedAt", 0L));
        cv.put("frequency_floor_required_count", getInt(c, "frequencyFloorRequiredCount", 0));
        cv.put("frequency_floor_target_count", getInt(c, "frequencyFloorTargetCount", 0));
        cv.put("frequency_floor_actual_count", getInt(c, "frequencyFloorActualCount", 0));
        cv.put("frequency_floor_debt_count", getInt(c, "frequencyFloorDebtCount", 0));
        cv.put("frequency_target_debt_count", getInt(c, "frequencyTargetDebtCount", 0));
        cv.put("frequency_floor_mandatory", getInt(c, "frequencyFloorMandatory", 0));
        cv.put("frequency_floor_no_upper_cap", getInt(c, "frequencyFloorNoUpperCap", 1));
        cv.put("frequency_floor_rank_score", getDouble(c, "frequencyFloorRankScore", 0.0d));
        cv.put(
                "frequency_floor_version",
                getString(c, "frequencyFloorVersion", AppConfig.FREQUENCY_FLOOR_VERSION));
        return cv;
    }

    public double getRecentDecisionFrequency() {
        return getRecentDecisionFrequency(AppConfig.FREQ_LOOKBACK_ISSUES);
    }

    public double getRecentDecisionFrequency(int lookback) {
        return getRecentRealBetFrequency(lookback);
    }

    public double getRecentRealBetFrequency() {
        return getRecentRealBetFrequency(AppConfig.FREQ_LOOKBACK_ISSUES);
    }

    public double getRecentRealBetFrequency(int lookback) {
        int n = Math.max(1, lookback);
        Cursor c = null;
        try {
            SQLiteDatabase db = readable();
            String sub =
                    "SELECT target_issue FROM "
                            + DbHelper.TABLE_DECISIONS
                            + " ORDER BY created_at DESC LIMIT "
                            + n;
            c =
                    db.rawQuery(
                            "SELECT COUNT(*) AS total, SUM(CASE WHEN decision='BET' AND"
                                + " stat_scope='REAL_TRADE' AND real_trade=1 THEN 1 ELSE 0 END) AS"
                                + " bets FROM (SELECT * FROM "
                                    + DbHelper.TABLE_DECISIONS
                                    + " WHERE target_issue IN ("
                                    + sub
                                    + "))",
                            null);
            if (c != null && c.moveToFirst()) {
                int total = getInt(c, "total");
                int bets = getInt(c, "bets");
                return total <= 0
                        ? 0.0d
                        : AppConfig.clampProbability(((double) bets) / ((double) total));
            }
        } catch (Throwable e) {
        } finally {
            closeCursor(c);
        }
        return 0.0d;
    }

    public double getRecentProbeFrequency(int lookback) {
        int n = Math.max(1, lookback);
        Cursor c = null;
        try {
            SQLiteDatabase db = readable();
            c =
                    db.rawQuery(
                            "SELECT COUNT(*) AS total, SUM(CASE WHEN bet_domain IN (?,?) AND"
                                + " decision='BET' AND real_trade=1 THEN 1 ELSE 0 END) AS probes"
                                + " FROM (SELECT * FROM "
                                    + DbHelper.TABLE_DECISIONS
                                    + " ORDER BY created_at DESC LIMIT "
                                    + n
                                    + ")",
                            new String[] {
                                AppConfig.BET_DOMAIN_FREQ_RECOVERY,
                                AppConfig.BET_DOMAIN_REAL_BET_PROBE
                            });
            if (c != null && c.moveToFirst()) {
                int total = getInt(c, "total");
                int probes = getInt(c, "probes");
                return total <= 0
                        ? 0.0d
                        : AppConfig.clampProbability(((double) probes) / ((double) total));
            }
        } catch (Throwable e) {
        } finally {
            closeCursor(c);
        }
        return 0.0d;
    }

    public int getRecentDecisionCount(int lookback) {
        int n = Math.max(1, lookback);
        Cursor c = null;
        try {
            SQLiteDatabase db = readable();
            c =
                    db.rawQuery(
                            "SELECT COUNT(*) AS total FROM (SELECT target_issue FROM "
                                    + DbHelper.TABLE_DECISIONS
                                    + " ORDER BY created_at DESC LIMIT "
                                    + n
                                    + ")",
                            null);
            if (c != null && c.moveToFirst()) return Math.max(0, getInt(c, "total"));
        } catch (Throwable e) {
        } finally {
            closeCursor(c);
        }
        return 0;
    }

    public int getRecentQualifiedOpportunityCount(int lookback) {
        int n = Math.max(1, lookback);
        Cursor c = null;
        try {
            SQLiteDatabase db = readable();
            ensureLinkModificationV2Ready(db);
            String sql =
                    "SELECT SUM(CASE WHEN COALESCE(predecision_opportunity,0)=1 OR"
                        + " (COALESCE(hard_block_flag,0)=0 AND position BETWEEN 1 AND 10 AND n1"
                        + " BETWEEN 1 AND 10 AND n2 BETWEEN 1 AND 10 AND n3 BETWEEN 1 AND 10 AND"
                        + " n1<>n2 AND n1<>n3 AND n2<>n3) THEN 1 ELSE 0 END) AS cnt FROM (SELECT *"
                        + " FROM "
                            + DbHelper.TABLE_DECISIONS
                            + " ORDER BY created_at DESC LIMIT "
                            + n
                            + ")";
            c = db.rawQuery(sql, null);
            if (c != null && c.moveToFirst()) return Math.max(0, getInt(c, "cnt"));
        } catch (Throwable e) {
        } finally {
            closeCursor(c);
        }
        return 0;
    }

    public int getRecentRealBetCount(int lookback) {
        int n = Math.max(1, lookback);
        Cursor c = null;
        try {
            SQLiteDatabase db = readable();
            c =
                    db.rawQuery(
                            "SELECT SUM(CASE WHEN decision='BET' AND stat_scope='REAL_TRADE' AND"
                                + " real_trade=1 THEN 1 ELSE 0 END) AS cnt FROM (SELECT * FROM "
                                    + DbHelper.TABLE_DECISIONS
                                    + " ORDER BY created_at DESC LIMIT "
                                    + n
                                    + ")",
                            null);
            if (c != null && c.moveToFirst()) return Math.max(0, getInt(c, "cnt"));
        } catch (Throwable e) {
        } finally {
            closeCursor(c);
        }
        return 0;
    }

    public int getRecentRealLoseCount(int lookback) {
        int n = Math.max(1, lookback);
        Cursor c = null;
        try {
            SQLiteDatabase db = readable();
            ensureLinkModificationV2Ready(db);
            c =
                    db.rawQuery(
                            "SELECT SUM(CASE WHEN decision='BET' AND stat_scope='REAL_TRADE' AND"
                                + " real_trade=1 AND settled=1 AND result IN ('MISS','LOSE','未中')"
                                + " THEN 1 ELSE 0 END) AS cnt FROM (SELECT * FROM "
                                    + DbHelper.TABLE_DECISIONS
                                    + " ORDER BY created_at DESC LIMIT "
                                    + n
                                    + ")",
                            null);
            if (c != null && c.moveToFirst()) return Math.max(0, getInt(c, "cnt"));
        } catch (Throwable e) {
        } finally {
            closeCursor(c);
        }
        return 0;
    }

    public void saveShadowDomainStat(Models.ShadowDomainStat x) {
        if (x == null || x.domainKey == null || x.domainKey.length() == 0) return;
        SQLiteDatabase db = writable();
        ensureLinkModificationV2Ready(db);
        ContentValues cv = new ContentValues();
        cv.put("domain_key", x.domainKey);
        cv.put("parent_domain_key", Models.safeText(x.parentDomainKey));
        cv.put("promotion_status", Models.safeText(x.promotionStatus));
        cv.put("version", Models.safeText(x.version));
        cv.put("sample_count", Math.max(0, x.sampleCount));
        cv.put("hit_count", Math.max(0, x.hitCount));
        cv.put("miss_count", Math.max(0, x.missCount));
        cv.put("recent_sample_count", Math.max(0, x.recentSampleCount));
        cv.put("recent_hit_count", Math.max(0, x.recentHitCount));
        cv.put("misleading_streak", Math.max(0, x.misleadingStreak));
        cv.put("hit_rate", AppConfig.clampProbability(x.hitRate));
        cv.put("recent_hit_rate", AppConfig.clampProbability(x.recentHitRate));
        cv.put("posterior_mean", AppConfig.clampProbability(x.posteriorMean));
        cv.put("wilson_lower_bound", AppConfig.clampProbability(x.wilsonLowerBound));
        cv.put("drift_score", AppConfig.clampScore01(x.driftScore));
        cv.put("last_settled_issue", Models.safeIssue(x.lastSettledIssue));
        cv.put("updated_at", x.updatedAt > 0L ? x.updatedAt : System.currentTimeMillis());
        insertFiltered(db, TABLE_V20_SHADOW_DOMAIN_STAT, cv, SQLiteDatabase.CONFLICT_REPLACE);
        /*
         * 同一批“已结算SHADOW反事实”同时形成三种只读证据画像。
         * 这里只镜像结算后的统计，不计算当前期概率、不生成BET、不写真实盈亏。
         */
        try {
            Models.DomainEvidenceStat base = new Models.DomainEvidenceStat();
            base.domainKey = x.domainKey;
            base.parentDomainKey = Models.safeText(x.parentDomainKey);
            base.coarseDomainKey = buildCoarseDomainKeyV20(x.domainKey, x.parentDomainKey);
            base.sampleCount = Math.max(0, x.sampleCount);
            base.hitCount = Math.max(0, x.hitCount);
            base.missCount = Math.max(0, x.missCount);
            base.recentSampleCount = Math.max(0, x.recentSampleCount);
            base.recentHitCount = Math.max(0, x.recentHitCount);
            base.misleadingStreak = Math.max(0, x.misleadingStreak);
            base.hitRate = AppConfig.clampProbability(x.hitRate);
            base.recentHitRate = AppConfig.clampProbability(x.recentHitRate);
            base.posteriorMean =
                    AppConfig.clampProbability(
                            x.posteriorMean > 0.0d ? x.posteriorMean : x.hitRate);
            base.lowerBound = AppConfig.clampProbability(x.wilsonLowerBound);
            base.driftScore = AppConfig.clampScore01(x.driftScore);
            base.updatedAt = x.updatedAt > 0L ? x.updatedAt : System.currentTimeMillis();

            base.status = resolveEvidenceDomainStatusV20(base, Models.safeText(x.promotionStatus));
            base.version = AppConfig.ACTIVE_TEACHER_VERSION;
            saveDomainEvidenceStat(DbHelper.TABLE_TEACHER_DOMAIN_STAT_V20, base);

            Models.DomainEvidenceStat dynamic = copyDomainEvidenceV20(base);
            dynamic.version = AppConfig.ACTIVE_DYNAMIC_DOMAIN_VERSION;
            saveDomainEvidenceStat(DbHelper.TABLE_DYNAMIC_DOMAIN_STAT_V20, dynamic);

            Models.DomainEvidenceStat promotion = copyDomainEvidenceV20(base);
            promotion.status =
                    Models.safeText(x.promotionStatus).length() == 0
                            ? base.status
                            : Models.safeText(x.promotionStatus);
            promotion.version = AppConfig.ACTIVE_PROMOTION_VERSION;
            saveDomainEvidenceStat(DbHelper.TABLE_PROMOTION_DOMAIN_STAT_V20, promotion);
        } catch (Throwable ignore) {
        }
    }

    private Models.DomainEvidenceStat copyDomainEvidenceV20(Models.DomainEvidenceStat src) {
        Models.DomainEvidenceStat x = new Models.DomainEvidenceStat();
        if (src == null) return x;
        x.domainKey = src.domainKey;
        x.parentDomainKey = src.parentDomainKey;
        x.coarseDomainKey = src.coarseDomainKey;
        x.status = src.status;
        x.version = src.version;
        x.sampleCount = src.sampleCount;
        x.hitCount = src.hitCount;
        x.missCount = src.missCount;
        x.recentSampleCount = src.recentSampleCount;
        x.recentHitCount = src.recentHitCount;
        x.misleadingStreak = src.misleadingStreak;
        x.hitRate = src.hitRate;
        x.recentHitRate = src.recentHitRate;
        x.posteriorMean = src.posteriorMean;
        x.lowerBound = src.lowerBound;
        x.driftScore = src.driftScore;
        x.updatedAt = src.updatedAt;
        return x;
    }

    private String resolveEvidenceDomainStatusV20(
            Models.DomainEvidenceStat x, String promotionStatus) {
        String p = promotionStatus == null ? "" : promotionStatus.toUpperCase(java.util.Locale.US);
        if (p.indexOf("BLOCK") >= 0 || p.indexOf("FREEZE") >= 0 || p.indexOf("RISK") >= 0)
            return "RISK";
        if (x == null || x.sampleCount <= 0) return "NO_SAMPLE";
        if (x.misleadingStreak >= 5 || x.posteriorMean < 0.285d) return "RISK";
        if (x.sampleCount >= AppConfig.PROBE_STRONG_MIN_SETTLED_SAMPLE
                && x.posteriorMean >= AppConfig.PROBE_STRONG_MIN_SETTLED_RATE
                && x.lowerBound >= 0.320d) return "STRONG";
        if (x.sampleCount >= AppConfig.PROBE_READY_MIN_SETTLED_SAMPLE
                && x.posteriorMean >= AppConfig.PROBE_READY_MIN_SETTLED_RATE
                && x.lowerBound >= 0.300d) return "READY";
        if (x.sampleCount >= AppConfig.PROBE_WATCH_MIN_SETTLED_SAMPLE
                && x.posteriorMean >= AppConfig.PROBE_WATCH_MIN_SETTLED_RATE) return "WATCH";
        return "WATCH";
    }

    private String buildCoarseDomainKeyV20(String domainKey, String parentDomainKey) {
        String p = Models.safeText(parentDomainKey);
        if (p.length() > 0) return p;
        String d = Models.safeText(domainKey);
        int cut = d.indexOf('|');
        return cut > 0 ? d.substring(0, cut) : d;
    }

    public List<Models.ShadowDomainStat> getShadowPromotionDomainStats() {
        return getShadowPromotionDomainStats("", 200);
    }

    public List<Models.ShadowDomainStat> getShadowPromotionDomainStats(
            String domainPrefix, int limit) {
        ArrayList<Models.ShadowDomainStat> out = new ArrayList<Models.ShadowDomainStat>();
        Cursor c = null;
        try {
            SQLiteDatabase db = readable();
            ensureLinkModificationV2Ready(db);
            String where = null;
            String[] args = null;
            if (domainPrefix != null && domainPrefix.length() > 0) {
                where = "domain_key LIKE ?";
                args = new String[] {domainPrefix + "%"};
            }
            c =
                    db.query(
                            TABLE_V20_SHADOW_DOMAIN_STAT,
                            null,
                            where,
                            args,
                            null,
                            null,
                            "sample_count DESC, updated_at DESC",
                            String.valueOf(Math.max(1, limit)));
            while (c != null && c.moveToNext()) {
                Models.ShadowDomainStat x = new Models.ShadowDomainStat();
                x.domainKey = getString(c, "domain_key");
                x.parentDomainKey = getString(c, "parent_domain_key");
                x.promotionStatus = getString(c, "promotion_status");
                x.version = getString(c, "version");
                x.sampleCount = getInt(c, "sample_count");
                x.hitCount = getInt(c, "hit_count");
                x.missCount = getInt(c, "miss_count");
                x.recentSampleCount = getInt(c, "recent_sample_count");
                x.recentHitCount = getInt(c, "recent_hit_count");
                x.misleadingStreak = getInt(c, "misleading_streak");
                x.hitRate = getDouble(c, "hit_rate");
                x.recentHitRate = getDouble(c, "recent_hit_rate");
                x.posteriorMean = getDouble(c, "posterior_mean");
                x.wilsonLowerBound = getDouble(c, "wilson_lower_bound");
                x.driftScore = getDouble(c, "drift_score");
                x.lastSettledIssue = getString(c, "last_settled_issue");
                x.updatedAt = getLong(c, "updated_at");
                out.add(x);
            }
        } catch (Throwable e) {
        } finally {
            closeCursor(c);
        }
        return out;
    }

    /* 实时裁决提速与有效域修复 · DbStore只做读写、修复和诊断对账。 */
    public static final String REALTIME_VALID_DOMAIN_DBSTORE_VERSION =
            "V48_POINT_IN_TIME_MODEL_LAB_SINGLE_ACTION_CHAMPION_FULL_DIAGNOSIS_20260721_48072148";

    public void resetRealtimeStageCosts(String targetIssue) {
        String issue = Models.safeIssue(targetIssue);
        setState("analysis_cost_issue_v3", issue);
        setState("analysis_cost_summary_v3", "");
        setState("analysis_cost_total_ms_v3", "0");
        setState("analysis_cost_stage_count_v3", "0");
        setState("analysis_cost_version_v3", REALTIME_VALID_DOMAIN_DBSTORE_VERSION);
    }

    public void saveRealtimeStageCost(String targetIssue, String stage, long costMs) {
        String issue = Models.safeIssue(targetIssue);
        String current = getState("analysis_cost_issue_v3", "");
        if (!issue.equals(current)) resetRealtimeStageCosts(issue);
        String safeStage = AppConfig.safeKeyPart(stage == null ? "UNKNOWN" : stage);
        long cost = Math.max(0L, costMs);
        setState("analysis_cost_stage_" + safeStage, String.valueOf(cost));
        String summary = getState("analysis_cost_summary_v3", "");
        String item = safeStage + "=" + cost + "ms";
        if (summary.length() == 0) summary = item;
        else if (summary.indexOf(safeStage + "=") < 0) summary = summary + "｜" + item;
        else {
            String[] parts = summary.split("｜");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                String p = parts[i];
                if (p != null && p.indexOf(safeStage + "=") == 0) p = item;
                if (p == null || p.length() == 0) continue;
                if (sb.length() > 0) sb.append("｜");
                sb.append(p);
            }
            summary = sb.toString();
        }
        setState("analysis_cost_summary_v3", summary);
        long total = 0L;
        try {
            total = Long.parseLong(getState("analysis_cost_total_ms_v3", "0"));
        } catch (Throwable e) {
            total = 0L;
        }
        setState("analysis_cost_total_ms_v3", String.valueOf(Math.max(0L, total + cost)));
        int count = 0;
        try {
            count = Integer.parseInt(getState("analysis_cost_stage_count_v3", "0"));
        } catch (Throwable e) {
            count = 0;
        }
        setState("analysis_cost_stage_count_v3", String.valueOf(Math.max(0, count + 1)));
        setState("analysis_cost_updated_at_v3", String.valueOf(System.currentTimeMillis()));
    }

    public String getRealtimeStageCostSummary(String targetIssue) {
        String issue = Models.safeIssue(targetIssue);
        String current = getState("analysis_cost_issue_v3", "");
        if (issue.length() > 0 && !issue.equals(current)) return "";
        return getState("analysis_cost_summary_v3", "");
    }

    public Models.LayerSnapshot loadLayerSnapshot(String snapshotKey) {
        if (snapshotKey == null || snapshotKey.length() == 0) return null;
        Cursor c = null;
        try {
            SQLiteDatabase db = readable();
            ensureLinkModificationV2Ready(db);
            c =
                    db.query(
                            TABLE_V20_LAYER_SNAPSHOT,
                            null,
                            "snapshot_key=?",
                            new String[] {snapshotKey},
                            null,
                            null,
                            "created_at DESC",
                            "1");
            if (c != null && c.moveToFirst()) {
                Models.LayerSnapshot x = new Models.LayerSnapshot();
                x.snapshotKey = getString(c, "snapshot_key");
                x.layer = getString(c, "layer");
                x.asOfIssue = getString(c, "as_of_issue");
                x.maxIncludedIssue = getString(c, "max_included_issue");
                x.schemaVersion = getString(c, "schema_version");
                x.strategyVersion = getString(c, "strategy_version");
                x.freshnessStatus = getString(c, "freshness_status");
                x.payload = getString(c, "payload");
                x.sampleCount = getInt(c, "sample_count");
                x.createdAt = getLong(c, "created_at");
                x.expiresAt = getLong(c, "expires_at");
                return x;
            }
        } catch (Throwable e) {
        } finally {
            closeCursor(c);
        }
        return null;
    }

    public boolean isLayerSnapshotDue(String snapshotKey, long refreshMs) {
        Models.LayerSnapshot x = loadLayerSnapshot(snapshotKey);
        if (x == null || x.createdAt <= 0L) return true;
        return System.currentTimeMillis() - x.createdAt >= Math.max(1000L, refreshMs);
    }

    public int updateShadowSettlementEvidence(
            String targetIssue,
            int position,
            int k3Mask,
            int actualNumber,
            boolean wouldHit,
            String domainKey,
            String promotionStatus) {
        if (targetIssue == null
                || targetIssue.length() == 0
                || position < AppConfig.MIN_POSITION
                || k3Mask <= 0) return 0;
        SQLiteDatabase db = writable();
        ensureLinkModificationV2Ready(db);
        ContentValues cv = new ContentValues();
        cv.put("actual_number", actualNumber);
        cv.put("hit", wouldHit ? 1 : 0);
        cv.put("result", wouldHit ? AppConfig.RESULT_WIN : AppConfig.RESULT_MISS);
        cv.put("settled", 1);
        cv.put("learned", 1);
        cv.put("settled_at", System.currentTimeMillis());
        cv.put("shadow_promotion_domain", AppConfig.sanitizePromotionDomainKey(domainKey));
        cv.put("shadow_promotion_status", promotionStatus == null ? "" : promotionStatus);
        cv.put(
                "promotion_domain_available",
                AppConfig.sanitizePromotionDomainKey(domainKey).length() > 0 ? 1 : 0);
        return updateFiltered(
                db,
                DbHelper.TABLE_SHADOW,
                cv,
                "target_issue=? AND position=? AND k3_mask=?",
                new String[] {targetIssue, String.valueOf(position), String.valueOf(k3Mask)});
    }

    public int repairShadowEvidenceFromCandidateSnapshot(String targetIssue, int limit) {
        if (targetIssue == null || targetIssue.length() == 0) return 0;
        SQLiteDatabase db = writable();
        ensureLinkModificationV2Ready(db);
        Cursor c = null;
        int changed = 0;
        try {
            c =
                    db.query(
                            TABLE_MAIN_CHAIN_CANDIDATE_SNAPSHOT,
                            null,
                            "target_issue=?",
                            new String[] {targetIssue},
                            null,
                            null,
                            "candidate_rank ASC",
                            String.valueOf(Math.max(1, limit)));
            while (c != null && c.moveToNext()) {
                int p = getInt(c, "position");
                int mask = getInt(c, "k3_mask");
                if (p < AppConfig.MIN_POSITION || mask <= 0) continue;
                ContentValues cv = new ContentValues();
                cv.put("coverage_quality", getDouble(c, "coverage_quality"));
                cv.put("third_quality", getDouble(c, "third_quality"));
                cv.put("need_supply_score", getDouble(c, "need_supply_score"));
                cv.put("counter_risk", getDouble(c, "counter_risk"));
                cv.put("base_gate", getDouble(c, "base_gate"));
                cv.put("risk_adjusted_gate", getDouble(c, "risk_adjusted_gate"));
                cv.put("effective_probe_gate", getDouble(c, "effective_probe_gate"));
                cv.put("raw_edge", getDouble(c, "raw_edge"));
                String domain =
                        AppConfig.sanitizePromotionDomainKey(
                                getString(c, "shadow_promotion_domain"));
                if (domain.length() == 0 || AppConfig.isUnsafePromotionDomainKey(domain)) {
                    domain = buildSafePreDrawPromotionDomainFromCursor(c);
                }
                cv.put("shadow_promotion_domain", domain);
                cv.put("promotion_domain_available", domain.length() > 0 ? 1 : 0);
                cv.put("coverage_available", getDouble(c, "coverage_quality") > 0.0d ? 1 : 0);
                cv.put("third_quality_available", getDouble(c, "third_quality") > 0.0d ? 1 : 0);
                changed +=
                        updateFiltered(
                                db,
                                DbHelper.TABLE_SHADOW,
                                cv,
                                "target_issue=? AND position=? AND k3_mask=?",
                                new String[] {
                                    targetIssue, String.valueOf(p), String.valueOf(mask)
                                });
            }
        } catch (Throwable e) {
        } finally {
            closeCursor(c);
        }
        return changed;
    }

    public String ensureSafePreDrawPromotionDomain(Object obj) {
        if (obj == null) return "";
        String existing =
                AppConfig.sanitizePromotionDomainKey(
                        getOptionalStringField(obj, "shadowPromotionDomain", ""));
        if (existing.length() == 0 || AppConfig.isUnsafePromotionDomainKey(existing)) {
            existing = buildSafePreDrawPromotionDomain(obj);
        }
        setOptionalStringField(obj, "shadowPromotionDomain", existing);
        setOptionalStringField(obj, "shadowPromotionDomainKey", existing);
        setOptionalIntField(obj, "promotionDomainAvailable", existing.length() > 0 ? 1 : 0);
        return existing;
    }

    public String buildSafePreDrawPromotionDomain(Object obj) {
        if (obj == null) return "";
        int position = getOptionalIntField(obj, "position", 0);
        if (!AppConfig.isValidPosition(position)) return "";
        double p =
                maxDouble(
                        getOptionalDoubleField(obj, "finalP", 0.0d),
                        getOptionalDoubleField(obj, "calibratedP", 0.0d),
                        getOptionalDoubleField(obj, "rawP", 0.0d),
                        getOptionalDoubleField(obj, "modelP", 0.0d));
        if (p <= 0.0d) p = AppConfig.RANDOM_K3_BASELINE;
        double gate = getOptionalDoubleField(obj, "riskAdjustedGate", 0.0d);
        if (gate <= 0.0d)
            gate = getOptionalDoubleField(obj, "dynamicGate", AppConfig.DEFAULT_DYNAMIC_GATE);
        if (gate <= 0.0d) gate = AppConfig.DEFAULT_DYNAMIC_GATE;
        double gap = gate - p;
        double cover =
                maxDouble(
                        getOptionalDoubleField(obj, "coverageQuality", 0.0d),
                        getOptionalDoubleField(obj, "coverageQualityScore", 0.0d),
                        getOptionalDoubleField(obj, "finalDecisionCoverageQuality", 0.0d),
                        getOptionalDoubleField(obj, "needSupplyMatchScore", 0.0d));
        double third =
                maxDouble(
                        getOptionalDoubleField(obj, "thirdQuality", 0.0d),
                        getOptionalDoubleField(obj, "thirdNumberQualityScore", 0.0d),
                        getOptionalDoubleField(obj, "finalDecisionThirdNumberQuality", 0.0d),
                        getOptionalDoubleField(obj, "needSupplyThirdQuality", 0.0d));
        double counter =
                maxDouble(
                        getOptionalDoubleField(obj, "counterRisk", 0.0d),
                        getOptionalDoubleField(obj, "counterEvidenceScore", 0.0d),
                        getOptionalDoubleField(obj, "counterfactualRisk", 0.0d),
                        getOptionalDoubleField(obj, "counterfactualWhyNotRisk", 0.0d));
        double need =
                maxDouble(
                        getOptionalDoubleField(obj, "needSupplyScore", 0.0d),
                        getOptionalDoubleField(obj, "positionNeedScore", 0.0d),
                        getOptionalDoubleField(obj, "numberSupplyScore", 0.0d),
                        getOptionalDoubleField(obj, "needSupplyMatchScore", 0.0d));
        String stateKey =
                firstNonEmpty(
                        getOptionalStringField(obj, "windowStateKey", ""),
                        firstNonEmpty(
                                getOptionalStringField(obj, "dynamicWindowStateKey", ""),
                                getOptionalStringField(obj, "stateKey", "")));
        if (stateKey.length() == 0) stateKey = "STATE_ANY";
        String state = "S" + Math.abs(stateKey.hashCode() % 97);
        String pBand = p >= 0.38d ? "P38P" : (p >= 0.36d ? "P36_38" : "P_LT36");
        String gapBand = gap <= 0.010d ? "GAP01" : (gap <= 0.020d ? "GAP02" : "GAP_GT02");
        String qualityBand =
                cover >= AppConfig.REAL_BET_PROBE_MIN_COVERAGE
                                && third >= AppConfig.REAL_BET_PROBE_MIN_THIRD
                        ? "Q_OK"
                        : "Q_WEAK";
        String riskBand = counter <= AppConfig.REAL_BET_PROBE_MAX_COUNTER_RISK ? "R_OK" : "R_HIGH";
        String needBand = need >= AppConfig.REAL_BET_PROBE_MIN_NEED_SUPPLY ? "N_OK" : "N_WEAK";
        String adaptive = getOptionalStringField(obj, "adaptiveStatus", "").toUpperCase(java.util.Locale.ROOT);
        String adaptiveBand =
                adaptive.indexOf("FREEZE") >= 0 || adaptive.indexOf("REJECT") >= 0
                        ? "A_RISK"
                        : "A_OK";
        String rule = getOptionalStringField(obj, "ruleHealthStatus", "").toUpperCase(java.util.Locale.ROOT);
        String ruleBand =
                rule.indexOf("HARD_RISK") >= 0 || rule.indexOf("FREEZE") >= 0 ? "RH_RISK" : "RH_OK";
        return AppConfig.sanitizePromotionDomainKey(
                "SD_P"
                        + position
                        + "_"
                        + state
                        + "_"
                        + pBand
                        + "_"
                        + gapBand
                        + "_"
                        + qualityBand
                        + "_"
                        + riskBand
                        + "_"
                        + needBand
                        + "_"
                        + adaptiveBand
                        + "_"
                        + ruleBand);
    }

    private String buildSafePreDrawPromotionDomainFromCursor(Cursor c) {
        if (c == null) return "";
        int position = getInt(c, "position");
        if (!AppConfig.isValidPosition(position)) return "";
        double p =
                Math.max(
                        getDouble(c, "final_p"),
                        Math.max(getDouble(c, "calibrated_p"), getDouble(c, "raw_p")));
        if (p <= 0.0d) p = AppConfig.RANDOM_K3_BASELINE;
        double gate = getDouble(c, "risk_adjusted_gate");
        if (gate <= 0.0d) gate = getDouble(c, "dynamic_gate");
        if (gate <= 0.0d) gate = AppConfig.DEFAULT_DYNAMIC_GATE;
        double gap = gate - p;
        double cover = getDouble(c, "coverage_quality");
        double third = getDouble(c, "third_quality");
        double counter =
                Math.max(getDouble(c, "counter_risk"), getDouble(c, "counter_evidence_score"));
        double need =
                Math.max(
                        getDouble(c, "need_supply_score"),
                        Math.max(
                                getDouble(c, "position_need_score"),
                                getDouble(c, "number_supply_score")));
        String stateKey = getString(c, "window_state_key");
        if (stateKey.length() == 0) stateKey = getString(c, "state_key");
        if (stateKey.length() == 0) stateKey = "STATE_ANY";
        String state = "S" + Math.abs(stateKey.hashCode() % 97);
        String pBand = p >= 0.38d ? "P38P" : (p >= 0.36d ? "P36_38" : "P_LT36");
        String gapBand = gap <= 0.010d ? "GAP01" : (gap <= 0.020d ? "GAP02" : "GAP_GT02");
        String qualityBand =
                cover >= AppConfig.REAL_BET_PROBE_MIN_COVERAGE
                                && third >= AppConfig.REAL_BET_PROBE_MIN_THIRD
                        ? "Q_OK"
                        : "Q_WEAK";
        String riskBand = counter <= AppConfig.REAL_BET_PROBE_MAX_COUNTER_RISK ? "R_OK" : "R_HIGH";
        String needBand = need >= AppConfig.REAL_BET_PROBE_MIN_NEED_SUPPLY ? "N_OK" : "N_WEAK";
        String adaptive = getString(c, "adaptive_status").toUpperCase(java.util.Locale.ROOT);
        String adaptiveBand =
                adaptive.indexOf("FREEZE") >= 0 || adaptive.indexOf("REJECT") >= 0
                        ? "A_RISK"
                        : "A_OK";
        String rule = getString(c, "rule_health_status").toUpperCase(java.util.Locale.ROOT);
        String ruleBand =
                rule.indexOf("HARD_RISK") >= 0 || rule.indexOf("FREEZE") >= 0 ? "RH_RISK" : "RH_OK";
        return AppConfig.sanitizePromotionDomainKey(
                "SD_P"
                        + position
                        + "_"
                        + state
                        + "_"
                        + pBand
                        + "_"
                        + gapBand
                        + "_"
                        + qualityBand
                        + "_"
                        + riskBand
                        + "_"
                        + needBand
                        + "_"
                        + adaptiveBand
                        + "_"
                        + ruleBand);
    }

    public String getCurrentDecisionTradeLinkStatus(String targetIssue) {
        Models.Decision d = getDecisionByTargetIssue(targetIssue);
        if (d == null) return "NO_DECISION";
        if (!AppConfig.DECISION_BET.equals(AppConfig.normalizeDecision(d.decision)))
            return AppConfig.TRADE_LINK_SKIP_NOT_REAL_BET;
        Models.Trade t = getTradeByTargetIssue(targetIssue);
        return t == null ? "REAL_BET_TRADE_MISSING" : "REAL_BET_TRADE_OK";
    }

    public int countUnsafePromotionDomains() {
        Cursor c = null;
        try {
            SQLiteDatabase db = readable();
            ensureLinkModificationV2Ready(db);
            c =
                    db.query(
                            TABLE_V20_SHADOW_DOMAIN_STAT,
                            new String[] {"domain_key"},
                            null,
                            null,
                            null,
                            null,
                            null);
            int count = 0;
            while (c != null && c.moveToNext())
                if (AppConfig.isUnsafePromotionDomainKey(getString(c, "domain_key"))) count++;
            return count;
        } catch (Throwable e) {
            return 0;
        } finally {
            closeCursor(c);
        }
    }

    /* V20 后台常驻与字段闭环辅助：只读写，不计算概率、不生成BET。 */
    public void setServiceUserEnabled(boolean enabled) {
        setState(AppConfig.STATE_SERVICE_USER_ENABLED, enabled ? "1" : "0");
    }

    public boolean isServiceUserEnabled() {
        String v = getState(AppConfig.STATE_SERVICE_USER_ENABLED, "1");
        return !"0".equals(v);
    }

    public void saveServiceHeartbeat(String source) {
        long now = System.currentTimeMillis();
        HashMap<String, String> values = new HashMap<String, String>();
        values.put(AppConfig.STATE_SERVICE_HEARTBEAT_AT, String.valueOf(now));
        values.put("service_heartbeat_source", source == null ? "" : source);
        values.put("service_heartbeat_status", "ALIVE");
        values.put("service_heartbeat_version", AppConfig.FINAL_WATCHDOG_EVIDENCE_VERSION);
        setStates(values);
    }

    public long getServiceHeartbeatAgeMs() {
        long at = 0L;
        try {
            at = Long.parseLong(getState(AppConfig.STATE_SERVICE_HEARTBEAT_AT, "0"));
        } catch (Throwable e) {
            at = 0L;
        }
        if (at <= 0L) return Long.MAX_VALUE;
        long age = System.currentTimeMillis() - at;
        return age < 0L ? 0L : age;
    }

    public void incrementServiceRestartCount(String reason) {
        int count = 0;
        try {
            count = Integer.parseInt(getState(AppConfig.STATE_SERVICE_RESTART_COUNT, "0"));
        } catch (Throwable e) {
            count = 0;
        }
        HashMap<String, String> values = new HashMap<String, String>();
        values.put(AppConfig.STATE_SERVICE_RESTART_COUNT, String.valueOf(count + 1));
        values.put("service_restart_reason", reason == null ? "" : reason);
        values.put("service_restart_at", String.valueOf(System.currentTimeMillis()));
        setStates(values);
    }

    public void saveBackgroundSliceState(
            String taskKey,
            String status,
            int processed,
            int pending,
            long costMs,
            String summary) {
        if (taskKey == null || taskKey.length() == 0) return;
        String key = AppConfig.safeKeyPart(taskKey).toLowerCase(java.util.Locale.ROOT);
        HashMap<String, String> values = new HashMap<String, String>();
        values.put("background_slice_" + key + "_status", status == null ? "" : status);
        values.put("background_slice_" + key + "_processed", String.valueOf(processed));
        values.put("background_slice_" + key + "_pending", String.valueOf(pending));
        values.put("background_slice_" + key + "_cost_ms", String.valueOf(Math.max(0L, costMs)));
        values.put("background_slice_" + key + "_summary", summary == null ? "" : summary);
        values.put(
                "background_slice_" + key + "_updated_at",
                String.valueOf(System.currentTimeMillis()));
        // Teacher snapshot/cursor owners have already returned their durable results.
        // This independent dashboard projection cannot turn them into failed work.
        trySetTelemetryStatesR77(values);
    }

    public String getBackgroundSliceSummary(String taskKey) {
        if (taskKey == null || taskKey.length() == 0) return "";
        String key = AppConfig.safeKeyPart(taskKey).toLowerCase(java.util.Locale.ROOT);
        return "status="
                + getState("background_slice_" + key + "_status", "")
                + "|processed="
                + getState("background_slice_" + key + "_processed", "0")
                + "|pending="
                + getState("background_slice_" + key + "_pending", "0")
                + "|cost="
                + getState("background_slice_" + key + "_cost_ms", "0")
                + "ms"
                + "|updated="
                + getState("background_slice_" + key + "_updated_at", "0");
    }

    public Models.ServiceHealthState loadServiceHealthState() {
        Models.ServiceHealthState x = new Models.ServiceHealthState();
        x.userEnabled = isServiceUserEnabled() ? 1 : 0;
        x.heartbeatAt = parseLongSafeV20(getState(AppConfig.STATE_SERVICE_HEARTBEAT_AT, "0"), 0L);
        x.heartbeatAgeMs =
                x.heartbeatAt <= 0L
                        ? Long.MAX_VALUE
                        : Math.max(0L, System.currentTimeMillis() - x.heartbeatAt);
        x.alarmStatus = getState(AppConfig.STATE_WATCHDOG_ALARM_STATUS, "UNKNOWN");
        x.jobStatus = getState(AppConfig.STATE_WATCHDOG_JOB_STATUS, "UNKNOWN");
        x.lastReason = getState(AppConfig.STATE_WATCHDOG_LAST_REASON, "");
        x.restartCount = parseIntSafeV20(getState(AppConfig.STATE_SERVICE_RESTART_COUNT, "0"), 0);
        x.updatedAt = System.currentTimeMillis();
        return x;
    }

    public Models.TeacherSliceState loadTeacherSliceState(String layer) {
        String l = layer == null ? "" : layer.trim().toUpperCase(java.util.Locale.ROOT);
        String key = "teacher_" + l.toLowerCase(java.util.Locale.ROOT);
        Models.TeacherSliceState x = new Models.TeacherSliceState();
        x.layer = l;
        x.status = getState("background_slice_" + key + "_status", "IDLE");
        Models.BackgroundCursor c = loadBackgroundCursor("TEACHER_" + l + "_CURSOR");
        if (c != null) {
            x.cursorIssue = c.cursorIssue;
            x.processed = c.processedCount;
            x.pending = c.pendingCount;
            if (x.status.length() == 0 || "IDLE".equals(x.status)) x.status = c.status;
        }
        x.required =
                "WARM".equals(l)
                        ? AppConfig.TEACHER_WARM_REQUIRED_DRAWS
                        : ("COLD".equals(l)
                                ? AppConfig.TEACHER_COLD_REQUIRED_DRAWS
                                : AppConfig.TEACHER_DEEP_REQUIRED_DRAWS);
        x.costMs = parseLongSafeV20(getState("background_slice_" + key + "_cost_ms", "0"), 0L);
        x.updatedAt =
                parseLongSafeV20(getState("background_slice_" + key + "_updated_at", "0"), 0L);
        x.summary = getState("background_slice_" + key + "_summary", "");
        return x;
    }

    public void saveProbabilityAuditState(Models.ComboCandidate c, long costMs) {
        if (c == null) return;
        String target = AppConfig.safeIssue(c.targetIssue);
        HashMap<String, String> values = new HashMap<String, String>();
        values.put("probability_audit_target", target);
        values.put(
                "probability_audit_cohort",
                c.fixCohort == null ? AppConfig.FIX_COHORT : c.fixCohort);
        values.put(
                "probability_audit_pipeline",
                c.probabilityPipelineVersion == null
                        ? AppConfig.PROBABILITY_PIPELINE_VERSION
                        : c.probabilityPipelineVersion);
        values.put("probability_audit_before_teacher", String.valueOf(c.probabilityBeforeTeacher));
        values.put("probability_audit_after_teacher", String.valueOf(c.probabilityAfterTeacher));
        values.put("probability_audit_before_risk", String.valueOf(c.probabilityBeforeRisk));
        values.put("probability_audit_after_risk", String.valueOf(c.probabilityAfterRisk));
        values.put("probability_audit_final", String.valueOf(c.finalP));
        values.put("probability_audit_cost_ms", String.valueOf(Math.max(0L, costMs)));
        values.put("probability_audit_updated_at", String.valueOf(System.currentTimeMillis()));
        setStates(values);
    }

    public void normalizeExplicitCoverageAndRejectFields(Object obj) {
        if (obj == null) return;
        double quality = getOptionalDoubleField(obj, "coverageQuality", 0.0d);
        if (quality <= 0.0d) quality = getOptionalDoubleField(obj, "coverageQualityScore", 0.0d);
        double required = getOptionalDoubleField(obj, "coverageRequired", 0.0d);
        if (required <= 0.0d || required > 1.0d) required = AppConfig.COVERAGE_REQUIRED_DEFAULT;
        setOptionalDoubleField(obj, "coverageQuality", quality);
        setOptionalDoubleField(obj, "coverageQualityScore", quality);
        setOptionalDoubleField(obj, "coverageRequired", required);
        String scan = getOptionalStringField(obj, "scanRejectCode", "");
        String fin = getOptionalStringField(obj, "finalDecisionRejectCode", "");
        String display = fin.length() > 0 ? fin : scan;
        setOptionalStringField(obj, "displayRejectCode", display);
        setOptionalStringField(obj, "fixCohort", AppConfig.FIX_COHORT);
        setOptionalStringField(
                obj, "probabilityPipelineVersion", AppConfig.PROBABILITY_PIPELINE_VERSION);
    }

    private static long parseLongSafeV20(String s, long fallback) {
        try {
            return Long.parseLong(s == null ? "" : s.trim());
        } catch (Throwable e) {
            return fallback;
        }
    }

    private static int parseIntSafeV20(String s, int fallback) {
        try {
            return Integer.parseInt(s == null ? "" : s.trim());
        } catch (Throwable e) {
            return fallback;
        }
    }

    public void normalizeClosedLoopEvidenceAliases(Object obj) {
        if (obj == null) return;
        syncFieldChainAliases(obj);
        normalizeExplicitCoverageAndRejectFields(obj);

        double coverage =
                maxOptionalDouble(
                        obj,
                        new String[] {
                            "coverageQuality",
                            "coverageQualityScore",
                            "finalDecisionCoverageQuality",
                            "dynamicGateCoverageQualityScore"
                        });
        if (coverage > 0.0d) {
            setOptionalDoubleField(obj, "coverageQuality", coverage);
            setOptionalDoubleField(obj, "coverageQualityScore", coverage);
            setOptionalDoubleField(obj, "finalDecisionCoverageQuality", coverage);
            setOptionalIntField(obj, "coverageAvailable", 1);
        }

        double third =
                maxOptionalDouble(
                        obj,
                        new String[] {
                            "thirdQuality",
                            "thirdNumberQualityScore",
                            "finalDecisionThirdNumberQuality",
                            "needSupplyThirdQuality",
                            "dynamicGateThirdNumberQuality"
                        });
        if (third > 0.0d) {
            setOptionalDoubleField(obj, "thirdQuality", third);
            setOptionalDoubleField(obj, "thirdNumberQualityScore", third);
            setOptionalDoubleField(obj, "finalDecisionThirdNumberQuality", third);
            setOptionalIntField(obj, "thirdQualityAvailable", 1);
        }

        double need =
                maxOptionalDouble(
                        obj,
                        new String[] {
                            "needSupplyScore",
                            "needSupplyMatchScore",
                            "positionNeedScore",
                            "numberSupplyScore"
                        });
        if (need > 0.0d) {
            setOptionalDoubleField(obj, "needSupplyScore", need);
            setOptionalDoubleField(obj, "needSupplyMatchScore", need);
        }

        double counter =
                maxOptionalDouble(
                        obj,
                        new String[] {
                            "counterRisk",
                            "counterEvidenceScore",
                            "counterfactualRisk",
                            "counterfactualWhyNotRisk"
                        });
        if (counter > 0.0d) {
            setOptionalDoubleField(obj, "counterRisk", counter);
            setOptionalDoubleField(obj, "counterEvidenceScore", counter);
        }

        String reject =
                firstNonEmpty(
                        getOptionalStringField(obj, "finalDecisionRejectCode", ""),
                        firstNonEmpty(
                                getOptionalStringField(obj, "rejectCode", ""),
                                getOptionalStringField(obj, "blockCode", "")));
        String decision =
                AppConfig.normalizeDecision(
                        getOptionalStringField(
                                obj, "decision", getOptionalStringField(obj, "action", "")));
        if (!AppConfig.DECISION_BET.equals(decision) && reject.length() == 0)
            reject = "FINAL_GATE_NOT_PASSED";
        if (reject.length() > 0) {
            setOptionalStringField(obj, "finalDecisionRejectCode", reject);
            setOptionalStringField(obj, "rejectCode", reject);
        }

        String domain = ensureSafePreDrawPromotionDomain(obj);
        setOptionalIntField(obj, "promotionDomainAvailable", domain.length() > 0 ? 1 : 0);
        setOptionalStringField(
                obj,
                "fieldChainVersion",
                "V48_POINT_IN_TIME_MODEL_LAB_SINGLE_ACTION_CHAMPION_FULL_DIAGNOSIS_20260721_48072148");
    }

    public Models.DatabaseQueueState loadDatabaseQueueState() {
        return coordinator.snapshot();
    }

    public void setStates(final Map<String, String> values) {
        if (values == null || values.size() == 0) return;
        // Propagate failure into an enclosing transaction too. A swallowed failure
        // would let its owner commit only the first half of an observation batch.
        coordinator.runAtomicWriteOrThrowF01(
                "R914_TELEMETRY_STATE_BATCH",
                new DatabaseCoordinator.WriteCallable<Boolean>() {
                    @Override public Boolean call(SQLiteDatabase db) {
                        long now = System.currentTimeMillis();
                        for (Map.Entry<String, String> e : values.entrySet()) {
                            if (e == null || e.getKey() == null || e.getKey().length() == 0)
                                continue;
                            ContentValues cv = new ContentValues();
                            cv.put("state_key", e.getKey());
                            cv.put("state_value", e.getValue() == null ? "" : e.getValue());
                            cv.put("updated_at", now);
                            long inserted = db.insertWithOnConflict(
                                    DbHelper.TABLE_APP_STATE, null, cv,
                                    SQLiteDatabase.CONFLICT_REPLACE);
                            if (inserted < 0L)
                                throw new IllegalStateException("R77_STATE_BATCH_INSERT_FAILED");
                        }
                        return Boolean.TRUE;
                    }
                });
    }

    /** Optional display projection after its authoritative owner has committed.
     * Failure rolls back the entire projection and must not reinterpret the owner's
     * committed result. Never use for cursors, leases, receipts or eligibility.
     * Nested use is rejected before writing so no outer transaction can commit a
     * swallowed partial batch.
     */
    public boolean trySetTelemetryStatesR77(final Map<String, String> values) {
        if (values == null || values.isEmpty()) return true;
        final SQLiteDatabase db;
        try { db = readable(); }
        catch (RuntimeException unavailable) { return false; }
        if (db.inTransaction())
            throw new IllegalStateException("R77_TELEMETRY_REQUIRES_INDEPENDENT_TRANSACTION");
        try {
            setStates(values);
            return true;
        } catch (RuntimeException unavailable) {
            return false;
        }
    }

    public void saveDomainEvidenceStat(final String table, final Models.DomainEvidenceStat x) {
        if (table == null || x == null || x.domainKey == null || x.domainKey.length() == 0) return;
        SQLiteDatabase db = writable();
        ensureLinkModificationV2Ready(db);
        ContentValues cv = new ContentValues();
        cv.put("domain_key", x.domainKey);
        cv.put("parent_domain_key", x.parentDomainKey);
        cv.put("coarse_domain_key", x.coarseDomainKey);
        cv.put("status", x.status);
        cv.put("version", x.version);
        cv.put("sample_count", x.sampleCount);
        cv.put("hit_count", x.hitCount);
        cv.put("miss_count", x.missCount);
        cv.put("recent_sample_count", x.recentSampleCount);
        cv.put("recent_hit_count", x.recentHitCount);
        cv.put("misleading_streak", x.misleadingStreak);
        cv.put("hit_rate", x.hitRate);
        cv.put("recent_hit_rate", x.recentHitRate);
        cv.put("posterior_mean", x.posteriorMean);
        cv.put("lower_bound", x.lowerBound);
        cv.put("upper_bound", x.upperBound);
        cv.put("drift_score", x.driftScore);
        cv.put("updated_at", x.updatedAt);
        try (DatabaseCoordinator.WriterGuardCP3 cp3WriterGuard6 = DatabaseCoordinator.acquireWriterGuardCP3()) {
            db.insertWithOnConflict(table, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    public Models.DomainEvidenceStat loadDomainEvidenceStat(String table, String domainKey) {
        if (table == null || domainKey == null || domainKey.length() == 0) return null;
        Cursor c = null;
        try {
            c =
                    readable()
                            .query(
                                    table,
                                    null,
                                    "domain_key=?",
                                    new String[] {domainKey},
                                    null,
                                    null,
                                    null,
                                    "1");
            if (c != null && c.moveToFirst()) {
                Models.DomainEvidenceStat x = new Models.DomainEvidenceStat();
                x.domainKey = getString(c, "domain_key");
                x.parentDomainKey = getString(c, "parent_domain_key");
                x.coarseDomainKey = getString(c, "coarse_domain_key");
                x.status = getString(c, "status");
                x.version = getString(c, "version");
                x.sampleCount = getInt(c, "sample_count");
                x.hitCount = getInt(c, "hit_count");
                x.missCount = getInt(c, "miss_count");
                x.recentSampleCount = getInt(c, "recent_sample_count");
                x.recentHitCount = getInt(c, "recent_hit_count");
                x.misleadingStreak = getInt(c, "misleading_streak");
                x.hitRate = getDouble(c, "hit_rate");
                x.recentHitRate = getDouble(c, "recent_hit_rate");
                x.posteriorMean = getDouble(c, "posterior_mean");
                x.lowerBound = getDouble(c, "lower_bound");
                x.upperBound = getDouble(c, "upper_bound");
                x.driftScore = getDouble(c, "drift_score");
                x.updatedAt = getLong(c, "updated_at");
                return x;
            }
        } finally {
            closeCursor(c);
        }
        return null;
    }

    public void saveServiceRecoveryEvent(Models.ServiceRecoveryEvent e) {
        if (e == null) return;
        ContentValues cv = new ContentValues();
        cv.put("event_at", e.eventAt);
        cv.put("heartbeat_age_ms", e.heartbeatAgeMs);
        cv.put("source", e.source);
        cv.put("stage", e.stage);
        cv.put("result", e.result);
        cv.put("detail", e.detail);
        cv.put("version", e.version);
        writable().insert(DbHelper.TABLE_SERVICE_RECOVERY_EVENT_V20, null, cv);
    }

    /** 73487246：只记录恢复阶段证据，不运行策略、不创建BET。 */
    public void saveServiceRecoveryEventV20(
            String source, String stage, String result, String detail, long heartbeatAgeMs) {
        try {
            SQLiteDatabase db = writable();
            ContentValues cv = new ContentValues();
            cv.put("event_at", System.currentTimeMillis());
            cv.put("heartbeat_age_ms", Math.max(0L, heartbeatAgeMs));
            cv.put("source", source == null ? "" : source);
            cv.put("stage", stage == null ? "" : stage);
            cv.put("result", result == null ? "" : result);
            cv.put("detail", detail == null ? "" : detail);
            cv.put("version", AppConfig.FULL_CLOSED_LOOP_18_VERSION);
            insertFiltered(
                    db,
                    DbHelper.TABLE_SERVICE_RECOVERY_EVENT_V20,
                    cv,
                    SQLiteDatabase.CONFLICT_NONE);
        } catch (Throwable ignore) {
        }
    }

    /** 统一口径：只数已经开奖且有wouldHit结果的SHADOW。 */
    public int countSettledShadowSamplesV20() {
        Cursor c = null;
        try {
            SQLiteDatabase db = readable();
            c =
                    db.rawQuery(
                            "SELECT COUNT(1) FROM "
                                    + DbHelper.TABLE_SHADOW
                                    + " WHERE target_issue IS NOT NULL AND target_issue<>'' AND"
                                    + " (settled=1 OR actual_number>0)",
                            null);
            return c != null && c.moveToFirst() ? c.getInt(0) : 0;
        } catch (Throwable e) {
            return 0;
        } finally {
            closeCursor(c);
        }
    }

    /** 最近N个有效Decision口径，不依赖全局累计状态。 */
    public int getRecentValidDecisionCountV17(int lookback) {
        Cursor c = null;
        try {
            c =
                    readable()
                            .rawQuery(
                                    "SELECT COUNT(1) FROM (SELECT 1 FROM "
                                            + DbHelper.TABLE_DECISIONS
                                            + " WHERE target_issue IS NOT NULL AND target_issue<>''"
                                            + " ORDER BY created_at DESC LIMIT "
                                            + Math.max(1, lookback)
                                            + ")",
                                    null);
            return c != null && c.moveToFirst() ? Math.max(0, c.getInt(0)) : 0;
        } catch (Throwable e) {
            return 0;
        } finally {
            closeCursor(c);
        }
    }

    public int getRecentRealBetCountBeforeCurrentV17(int previousWindow) {
        return getRecentRealBetCount(Math.max(1, previousWindow));
    }

    /** 固定域键只生成一次并由候选/Decision/Trade共用。 */
    public String[] ensureStableEvidenceDomainsV17(Object obj) {
        if (obj == null) return new String[] {"", "", ""};
        Models.stampStableEvidenceDomains(obj);
        String coarse = getOptionalStringField(obj, "promotionCoarseDomain", "");
        String mid = getOptionalStringField(obj, "promotionMidDomain", "");
        String fine = getOptionalStringField(obj, "promotionFineDomain", "");
        return new String[] {coarse, mid, fine};
    }

    /** 对旧已结算SHADOW做幂等三级域回填。只写反事实统计，不改真实盈亏。 */
    public synchronized int backfillSettledShadowDomainsV17(int limit) {
        String done = getState(AppConfig.EVIDENCE_BACKFILL_STATE_KEY, "");
        if ("DONE".equals(done)) return 0;
        int batch = Math.max(1, Math.min(AppConfig.BACKGROUND_MAINTENANCE_BATCH, limit));
        String cursor = getState(AppConfig.EVIDENCE_BACKFILL_STATE_KEY + "_cursor", "");
        Cursor c = null;
        int scanned = 0, changed = 0, failed = 0;
        String last = cursor;
        try {
            SQLiteDatabase db = readable();
            String where = "settled=1 AND actual_number>0";
            String[] args = null;
            if (cursor.length() > 0) {
                where += " AND target_issue>?";
                args = new String[] {cursor};
            }
            c =
                    db.query(
                            DbHelper.TABLE_SHADOW,
                            null,
                            where,
                            args,
                            null,
                            null,
                            "target_issue ASC",
                            String.valueOf(batch));
            while (c != null && c.moveToNext()) {
                scanned++;
                last = getString(c, "target_issue");
                try {
                    Models.ComboCandidate cc = readShadowCandidate(c);
                    int actual = getInt(c, "actual_number");
                    boolean hit = actual == cc.n1 || actual == cc.n2 || actual == cc.n3;
                    String[] domains = ensureStableEvidenceDomainsV17(cc);
                    for (int i = 0; i < domains.length; i++)
                        if (domains[i].length() > 0) {
                            Models.ShadowDomainStat st = loadOneShadowDomainV17(domains[i]);
                            if (st == null) st = new Models.ShadowDomainStat();
                            st.domainKey = domains[i];
                            st.parentDomainKey = i == 2 ? domains[1] : (i == 1 ? domains[0] : "");
                            st.sampleCount++;
                            if (hit) st.hitCount++;
                            else st.missCount++;
                            st.hitRate =
                                    st.sampleCount <= 0
                                            ? 0.0d
                                            : ((double) st.hitCount) / st.sampleCount;
                            st.posteriorMean =
                                    ((double) st.hitCount + 3.0d) / (st.sampleCount + 10.0d);
                            st.wilsonLowerBound =
                                    Models.calcWilsonLowerBound(st.hitCount, st.sampleCount);
                            st.recentSampleCount =
                                    Math.min(AppConfig.FREQ_LOOKBACK_SHORT, st.sampleCount);
                            st.recentHitCount = Math.min(st.hitCount, st.recentSampleCount);
                            st.recentHitRate =
                                    st.recentSampleCount <= 0
                                            ? 0.0d
                                            : ((double) st.recentHitCount) / st.recentSampleCount;
                            st.lastSettledIssue = cc.targetIssue;
                            st.updatedAt = System.currentTimeMillis();
                            st.version = AppConfig.ACTIVE_EVIDENCE_DOMAIN_SCHEMA;
                            saveShadowDomainStat(st);
                            changed++;
                        }
                } catch (Throwable one) {
                    failed++;
                }
            }
            if (last.length() > 0)
                setState(AppConfig.EVIDENCE_BACKFILL_STATE_KEY + "_cursor", last);
            if (scanned < batch) setState(AppConfig.EVIDENCE_BACKFILL_STATE_KEY, "DONE");
            else setState(AppConfig.EVIDENCE_BACKFILL_STATE_KEY, "RUNNING");
        } catch (Throwable e) {
            setState(AppConfig.EVIDENCE_BACKFILL_STATE_KEY, "ERROR:" + String.valueOf(e));
        } finally {
            closeCursor(c);
        }
        setState(
                "evidence_backfill_summary_v20",
                "cursor="
                        + last
                        + "|scanned="
                        + scanned
                        + "|changed="
                        + changed
                        + "|failed="
                        + failed);
        return changed;
    }

    private Models.ShadowDomainStat loadOneShadowDomainV17(String domain) {
        List<Models.ShadowDomainStat> l = getShadowPromotionDomainStats(domain, 1);
        return l != null && l.size() > 0 && domain.equals(l.get(0).domainKey) ? l.get(0) : null;
    }

    /** 把Decision证据补回同目标期Trade，只修审计字段，不改结果和盈亏。 */
    public synchronized int repairDecisionTradeEvidenceV17(int limit) {
        return 0; // CP4 read-only diagnosis; immutable ledger evidence is never filled after draw.
    }

    public Models.FrequencyState loadRollingFrequencyStateV20() {
        Models.FrequencyState x = new Models.FrequencyState();
        x.stateKey = "MAIN";
        x.version = AppConfig.ACTIVE_QUALITY_BUILD_VERSION;
        x.updatedAt = System.currentTimeMillis();
        Cursor c = null;
        int index = 0;
        int qualified = 0;
        boolean foundReal = false, foundFloor = false, leadingFloor = true;
        try {
            SQLiteDatabase db = readable();
            c =
                    db.query(
                            DbHelper.TABLE_DECISIONS,
                            new String[] {
                                "decision",
                                "stat_scope",
                                "real_trade",
                                "final_p",
                                "target_issue",
                                "bet_domain",
                                "decision_path",
                                "decision_quality_path"
                            },
                            null,
                            null,
                            null,
                            null,
                            "created_at DESC",
                            String.valueOf(AppConfig.FREQ_WINDOW_300));
            while (c != null && c.moveToNext()) {
                index++;
                boolean bet =
                        AppConfig.DECISION_BET.equals(getString(c, "decision"))
                                && AppConfig.STAT_SCOPE_REAL_TRADE.equals(
                                        getString(c, "stat_scope"))
                                && getInt(c, "real_trade") == 1;
                boolean floor =
                        bet
                                && (AppConfig.isQualifiedFloorPath(
                                                getString(c, "bet_domain"),
                                                getString(c, "decision_path"))
                                        || AppConfig.DECISION_QUALITY_FLOOR.equals(
                                                getString(c, "decision_quality_path")));
                if (bet && !foundReal) {
                    x.issuesSinceLastRealBet = index - 1;
                    foundReal = true;
                }
                if (floor && !foundFloor) {
                    x.issuesSinceLastQualifiedFloorBet = index - 1;
                    foundFloor = true;
                }
                if (leadingFloor && floor) x.consecutiveQualifiedFloorBets++;
                else leadingFloor = false;
                double p = getDouble(c, "final_p");
                if (p >= AppConfig.QUALIFIED_FLOOR_MIN_FINAL_P) qualified++;
                if (index <= 20) {
                    x.observed20++;
                    if (bet) x.betCount20++;
                }
                if (index <= 50) {
                    x.observed50++;
                    if (bet) x.betCount50++;
                }
                if (index <= 100) {
                    x.observed100++;
                    if (bet) x.betCount100++;
                }
                if (index <= 300) {
                    x.observed300++;
                    if (bet) x.betCount300++;
                }
                if (index == 1) x.asOfIssue = getString(c, "target_issue");
            }
            if (!foundReal) x.issuesSinceLastRealBet = Math.max(999, index);
            if (!foundFloor) x.issuesSinceLastQualifiedFloorBet = Math.max(999, index);
        } catch (Throwable e) {
            setState("rolling_frequency_error_v20", String.valueOf(e));
        } finally {
            closeCursor(c);
        }
        x.frequency20 = x.observed20 <= 0 ? 0.0d : ((double) x.betCount20) / x.observed20;
        x.frequency50 = x.observed50 <= 0 ? 0.0d : ((double) x.betCount50) / x.observed50;
        x.frequency100 = x.observed100 <= 0 ? 0.0d : ((double) x.betCount100) / x.observed100;
        x.frequency300 = x.observed300 <= 0 ? 0.0d : ((double) x.betCount300) / x.observed300;
        x.floorDebt20 = AppConfig.frequencyFloorDebt(20, x.observed20, x.betCount20);
        x.floorDebt50 = AppConfig.frequencyFloorDebt(50, x.observed50, x.betCount50);
        x.floorDebt100 = AppConfig.frequencyFloorDebt(100, x.observed100, x.betCount100);
        x.floorDebt300 = AppConfig.frequencyFloorDebt(300, x.observed300, x.betCount300);
        x.targetDebt20 = AppConfig.frequencyTargetDebt(20, x.observed20, x.betCount20);
        x.targetDebt50 = AppConfig.frequencyTargetDebt(50, x.observed50, x.betCount50);
        x.targetDebt100 = AppConfig.frequencyTargetDebt(100, x.observed100, x.betCount100);
        x.targetDebt300 = AppConfig.frequencyTargetDebt(300, x.observed300, x.betCount300);
        x.urgentWindow =
                AppConfig.selectUrgentFrequencyWindow(
                        x.observed20,
                        x.betCount20,
                        x.observed50,
                        x.betCount50,
                        x.observed100,
                        x.betCount100,
                        x.observed300,
                        x.betCount300);
        x.floorPacingMinGap =
                AppConfig.qualifiedFloorPacingMinGap(
                        x.floorDebt20,
                        x.floorDebt50,
                        x.floorDebt100,
                        x.floorDebt300,
                        x.targetDebt20,
                        x.targetDebt50,
                        x.targetDebt100,
                        x.targetDebt300);
        x.floorPacingReady =
                AppConfig.isQualifiedFloorPacingReady(
                                x.issuesSinceLastRealBet,
                                x.consecutiveQualifiedFloorBets,
                                x.floorPacingMinGap)
                        ? 1
                        : 0;
        if (x.urgentWindow == null || x.urgentWindow.length() == 0) x.floorPacingReason = "NO_DEBT";
        else if (x.consecutiveQualifiedFloorBets >= AppConfig.QUALIFIED_FLOOR_MAX_CONSECUTIVE)
            x.floorPacingReason = "CONSECUTIVE_FLOOR_WAIT";
        else if (x.floorPacingReady == 0)
            x.floorPacingReason =
                    "MIN_GAP_WAIT_" + x.floorPacingMinGap + "_AGE_" + x.issuesSinceLastRealBet;
        else x.floorPacingReason = "READY_" + x.urgentWindow + "_GAP_" + x.floorPacingMinGap;
        x.qualifiedFloorCandidateCount = qualified;
        x.validIssueCount = x.observed100;
        x.realBetCount = x.betCount100;
        x.requiredFloorBetCount = AppConfig.frequencyFloorCount(100, x.observed100);
        x.targetBetCount = AppConfig.frequencyTargetCount(100, x.observed100);
        x.floorDebtCount = x.floorDebt100;
        x.targetDebtCount = x.targetDebt100;
        x.noUpperCap = 1;
        x.frequencyDebt =
                Math.max(
                        Math.max(
                                AppConfig.frequencyDebtUrgency(20, x.observed20, x.betCount20),
                                AppConfig.frequencyDebtUrgency(50, x.observed50, x.betCount50)),
                        Math.max(
                                AppConfig.frequencyDebtUrgency(100, x.observed100, x.betCount100),
                                AppConfig.frequencyDebtUrgency(300, x.observed300, x.betCount300)));
        x.status =
                x.urgentWindow.length() > 0
                        ? AppConfig.FREQ_STATE_LOW_PRESSURE
                        : AppConfig.FREQ_STATE_NORMAL;
        try {
            saveFrequencyState(x);
        } catch (Throwable ignore) {
        }
        return x;
    }

    public void registerModuleBuildV20(String moduleName, String version, String detail) {
        if (moduleName == null || moduleName.length() == 0) return;
        SQLiteDatabase db = writable();
        ensureModuleRegistryR363(db);
        ContentValues cv = new ContentValues();
        cv.put("module_name", moduleName);
        cv.put("version", version == null ? "" : version);
        cv.put("cohort", AppConfig.ACTIVE_QUALITY_BUILD_COHORT);
        cv.put("status", AppConfig.ACTIVE_QUALITY_BUILD_VERSION.equals(version) ? "OK" : "MIXED");
        cv.put("detail", detail == null ? "" : detail);
        cv.put("registered_at", System.currentTimeMillis());
        insertFiltered(db, DbHelper.TABLE_V20_MODULE_REGISTRY, cv, SQLiteDatabase.CONFLICT_REPLACE);
        try {
            setState("module_build_" + moduleName, version == null ? "" : version);
        } catch (Throwable ignore) {
        }
    }

    /** The build-identity write must not trigger the full historical link-schema migration. */
    private void ensureModuleRegistryR363(SQLiteDatabase db) {
        if (db == null) throw new IllegalArgumentException("MODULE_REGISTRY_DB_NULL");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + DbHelper.TABLE_V20_MODULE_REGISTRY
                        + " (module_name TEXT PRIMARY KEY)");
        addColumnIfMissing(db, DbHelper.TABLE_V20_MODULE_REGISTRY, "version", "TEXT");
        addColumnIfMissing(db, DbHelper.TABLE_V20_MODULE_REGISTRY, "cohort", "TEXT");
        addColumnIfMissing(db, DbHelper.TABLE_V20_MODULE_REGISTRY, "status", "TEXT");
        addColumnIfMissing(db, DbHelper.TABLE_V20_MODULE_REGISTRY, "detail", "TEXT");
        addColumnIfMissing(
                db,
                DbHelper.TABLE_V20_MODULE_REGISTRY,
                "registered_at",
                "INTEGER DEFAULT 0");
    }

    public String checkModuleBuildConsistencyV20() {
        return getModuleBuildConsistencyV20();
    }

    public String getModuleBuildConsistencyV20() {
        Cursor c = null;
        int total = 0, mixed = 0, missing = 0;
        StringBuilder sb = new StringBuilder();
        try {
            SQLiteDatabase db = readable();
            c =
                    db.query(
                            DbHelper.TABLE_V20_MODULE_REGISTRY,
                            null,
                            null,
                            null,
                            null,
                            null,
                            "module_name ASC");
            while (c != null && c.moveToNext()) {
                total++;
                String m = getString(c, "module_name"), v = getString(c, "version");
                if (!AppConfig.ACTIVE_QUALITY_BUILD_VERSION.equals(v)) {
                    mixed++;
                    if (sb.length() > 0) sb.append(',');
                    sb.append(m).append('=').append(v);
                }
            }
        } catch (Throwable e) {
            return "MIXED_BUILD:REGISTRY_ERROR";
        } finally {
            closeCursor(c);
        }
        String[] required =
                new String[] {
                    "AppConfig",
                    "AutoFetchService",
                    "DbStore",
                    "TeacherNumberMatrixEngine",
                    "DynamicCauseChainEngine",
                    "DecisionGateEngine",
                    "SettlementEngine",
                    "RealBetDiagnosisEngine"
                };
        for (int i = 0; i < required.length; i++) {
            String v = getState("module_build_" + required[i], "");
            if (v.length() == 0) {
                missing++;
                if (sb.length() > 0) sb.append(',');
                sb.append(required[i]).append("=MISSING");
            } else if (!AppConfig.ACTIVE_QUALITY_BUILD_VERSION.equals(v)) {
                mixed++;
                if (sb.length() > 0) sb.append(',');
                sb.append(required[i]).append('=').append(v);
            }
        }
        return mixed > 0 || missing > 0
                ? "MIXED_BUILD:" + sb.toString()
                : "BUILD_OK:" + AppConfig.ACTIVE_QUALITY_BUILD_VERSION + "|registered=" + total;
    }

    public int runEvidenceMaintenanceSliceV20() {
        if ("1".equals(getState("realtime_analysis_busy_v20", "0"))) return 0;
        int n = 0;
        n += backfillSettledShadowDomainsV17(AppConfig.BACKGROUND_MAINTENANCE_BATCH);
        if ("1".equals(getState("realtime_analysis_busy_v20", "0"))) return n;
        n += repairDecisionTradeEvidenceV17(AppConfig.BACKGROUND_MAINTENANCE_BATCH);
        return n;
    }

    /* STARCORE_TIANSHU_B03_DBSTORE_BEGIN */
    public static final String TIANSHU_B03_DBSTORE_VERSION = "V57.3.0_TIANSHU_B03_DBSTORE";
    public static final String TIANSHU_B03_GAME_CODE = "Binance1-PK10";

    public static final class B03StoreRule {
        public final String id, owner, invariant;
        public final boolean mandatory;
        public final int severity;

        B03StoreRule(String id, String owner, String invariant, boolean mandatory, int severity) {
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

    public static final class B03StorageStatus {
        public final boolean ready,
                integrityValid,
                gameIsolated,
                immutableTriggers,
                shadowOnly,
                modelProven,
                realBetDisabled;
        public final int predictionBundles, settlements, quarantineRows, transactionAudits;
        public final String schemaHash, replayHash, detail;

        B03StorageStatus(
                boolean ready,
                boolean integrity,
                boolean game,
                boolean triggers,
                boolean shadow,
                boolean proven,
                boolean disabled,
                int bundles,
                int settlements,
                int quarantine,
                int audits,
                String schema,
                String replay,
                String detail) {
            this.ready = ready;
            integrityValid = integrity;
            gameIsolated = game;
            immutableTriggers = triggers;
            shadowOnly = shadow;
            modelProven = proven;
            realBetDisabled = disabled;
            predictionBundles = bundles;
            this.settlements = settlements;
            quarantineRows = quarantine;
            transactionAudits = audits;
            schemaHash = schema;
            replayHash = replay;
            this.detail = detail;
        }
    }

    public void ensureTianshuB03Storage() {
        SQLiteDatabase db = writable();
        DbHelper.ensureTianshuB03(db);
        V51Repository.ensure(db);
    }

    public V51Repository.PersistReport persistTianshuB03Prediction(V51RuntimeResult result) {
        ensureTianshuB03Storage();
        return V51Repository.persistWithReport(this, result);
    }

    public V51Repository.SettlementReport settleTianshuB03Shadow(
            String targetIssue, String actualPermutation, String result, long settledAt) {
        ensureTianshuB03Storage();
        return V51Repository.settleShadow(this, targetIssue, actualPermutation, result, settledAt);
    }

    public DatabaseMigrationEngine.LegacyDrawMigrationReport migrateLegacyDrawsToQuarantine(
            SQLiteDatabase legacy, String sourceDatabase, String sourceGameCode, int limit) {
        ensureTianshuB03Storage();
        return DatabaseMigrationEngine.migrateLegacyDraws(
                writable(), legacy, sourceDatabase, sourceGameCode, limit);
    }

    public DatabaseIntegrityVerifier.B03IntegrityReport verifyTianshuB03Storage() {
        ensureTianshuB03Storage();
        DatabaseIntegrityVerifier.B03IntegrityReport r =
                DatabaseIntegrityVerifier.verifyB03(readable());
        try {
            DatabaseIntegrityVerifier.persistSnapshot(writable(), r);
        } catch (Throwable ignore) {
        }
        return r;
    }

    public B03StorageStatus getTianshuB03StorageStatus() {
        ensureTianshuB03Storage();
        SQLiteDatabase db = readable();
        DatabaseIntegrityVerifier.B03IntegrityReport r = DatabaseIntegrityVerifier.verifyB03(db);
        int bundles = b03Count(db, DbHelper.TABLE_B03_PREDICTION_BUNDLE);
        int settlements = b03Count(db, DbHelper.TABLE_B03_SETTLEMENT_EVENT);
        int quarantine = b03Count(db, DbHelper.TABLE_B03_DRAW_QUARANTINE);
        int audits = b03Count(db, DbHelper.TABLE_B03_TRANSACTION_AUDIT);
        boolean ready =
                r.valid
                        && DatabaseCoordinator.selfTest()
                        && DatabaseMigrationEngine.selfTest()
                        && DatabaseIntegrityVerifier.selfTest()
                        && V51Repository.selfTest();
        return new B03StorageStatus(
                ready,
                r.valid,
                DatabaseIntegrityVerifier.isGameIsolated(db),
                DatabaseIntegrityVerifier.hasImmutableTriggers(db),
                V51BuildManifest.SHADOW_ONLY,
                V51BuildManifest.MODEL_PROVEN,
                V51BuildManifest.REAL_BET_HARD_DISABLED,
                bundles,
                settlements,
                quarantine,
                audits,
                r.schemaHash,
                r.replayHash,
                r.detail);
    }

    public String replayTianshuB03Target(String targetIssue) {
        ensureTianshuB03Storage();
        String hash = V51Repository.replayHash(readable(), targetIssue);
        SQLiteDatabase db = writable();
        ContentValues cv = new ContentValues();
        cv.put("game_code", DatabaseCoordinator.GAME_CODE);
        cv.put("target_issue", targetIssue == null ? "" : targetIssue);
        cv.put("replay_hash", hash);
        cv.put(
                "bundle_hash",
                b03Scalar(
                        db,
                        "SELECT bundle_hash FROM "
                                + DbHelper.TABLE_B03_PREDICTION_BUNDLE
                                + " WHERE game_code=? AND target_issue=?",
                        new String[] {DatabaseCoordinator.GAME_CODE, targetIssue}));
        cv.put(
                "contract_hash",
                b03Scalar(
                        db,
                        "SELECT contract_hash FROM "
                                + DbHelper.TABLE_B03_PREDICTION_BUNDLE
                                + " WHERE game_code=? AND target_issue=?",
                        new String[] {DatabaseCoordinator.GAME_CODE, targetIssue}));
        cv.put("status", "REPLAYED");
        cv.put("detail", "DETERMINISTIC_LEDGER_REPLAY");
        cv.put("created_at", System.currentTimeMillis());
        db.insertWithOnConflict(
                DbHelper.TABLE_B03_REPLAY_AUDIT, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        return hash;
    }

    public java.util.ArrayList<ContentValues> listLegacyDrawQuarantine(int limit) {
        ensureTianshuB03Storage();
        java.util.ArrayList<ContentValues> out = new java.util.ArrayList<ContentValues>();
        Cursor c = null;
        try {
            c =
                    readable()
                            .query(
                                    DbHelper.TABLE_B03_DRAW_QUARANTINE,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    "created_at DESC",
                                    String.valueOf(Math.max(1, Math.min(1000, limit))));
            while (c.moveToNext()) out.add(cursorToContentValuesB03(c));
        } finally {
            closeCursor(c);
        }
        return out;
    }

    public boolean markLegacyDrawReview(String rowHash, String status) {
        if (rowHash == null || rowHash.length() == 0) return false;
        String s = status == null ? "" : status.trim();
        if (!"PENDING".equals(s) && !"ACCEPT_DRAW_ONLY".equals(s) && !"REJECT".equals(s))
            return false;
        ContentValues cv = new ContentValues();
        cv.put("review_status", s);
        try {
            int n =
                    writable()
                            .update(
                                    DbHelper.TABLE_B03_DRAW_QUARANTINE,
                                    cv,
                                    "row_hash=?",
                                    new String[] {rowHash});
            return n == 1;
        } catch (Throwable e) {
            return false;
        }
    }

    public boolean hasTianshuB03Prediction(String targetIssue) {
        ensureTianshuB03Storage();
        return b03ScalarInt(
                        readable(),
                        "SELECT COUNT(*) FROM "
                                + DbHelper.TABLE_B03_PREDICTION_BUNDLE
                                + " WHERE game_code=? AND target_issue=?",
                        new String[] {DatabaseCoordinator.GAME_CODE, targetIssue})
                == 1;
    }

    public boolean hasTianshuB03Settlement(String targetIssue) {
        ensureTianshuB03Storage();
        return b03ScalarInt(
                        readable(),
                        "SELECT COUNT(*) FROM "
                                + DbHelper.TABLE_B03_SETTLEMENT_EVENT
                                + " WHERE game_code=? AND target_issue=?",
                        new String[] {DatabaseCoordinator.GAME_CODE, targetIssue})
                == 1;
    }

    private static int b03Count(SQLiteDatabase db, String table) {
        return b03ScalarInt(db, "SELECT COUNT(*) FROM " + table, null);
    }

    private static int b03ScalarInt(SQLiteDatabase db, String sql, String[] args) {
        Cursor c = null;
        try {
            c = db.rawQuery(sql, args);
            return c.moveToFirst() ? c.getInt(0) : 0;
        } catch (Throwable e) {
            return -1;
        } finally {
            if (c != null) c.close();
        }
    }

    private static String b03Scalar(SQLiteDatabase db, String sql, String[] args) {
        Cursor c = null;
        try {
            c = db.rawQuery(sql, args);
            return c.moveToFirst() && !c.isNull(0) ? c.getString(0) : "";
        } catch (Throwable e) {
            return "";
        } finally {
            if (c != null) c.close();
        }
    }

    private static ContentValues cursorToContentValuesB03(Cursor c) {
        ContentValues cv = new ContentValues();
        String[] names = c.getColumnNames();
        for (int i = 0; i < names.length; i++) {
            if (c.isNull(i)) cv.putNull(names[i]);
            else {
                int type = c.getType(i);
                if (type == Cursor.FIELD_TYPE_INTEGER) cv.put(names[i], c.getLong(i));
                else if (type == Cursor.FIELD_TYPE_FLOAT) cv.put(names[i], c.getDouble(i));
                else if (type == Cursor.FIELD_TYPE_BLOB) cv.put(names[i], c.getBlob(i));
                else cv.put(names[i], c.getString(i));
            }
        }
        return cv;
    }

    public static java.util.List<B03StoreRule> tianshuB03StoreRules() {
        java.util.ArrayList<B03StoreRule> rules = new java.util.ArrayList<B03StoreRule>();
        rules.add(new B03StoreRule("B03-STORE-001", "DbStore", "DRAW_OWNER", true, 1));
        rules.add(new B03StoreRule("B03-STORE-002", "V51Repository", "PREDICTION_OWNER", true, 2));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-003", "DatabaseCoordinator", "SETTLEMENT_OWNER", true, 3));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-004", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 4));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-005", "DatabaseIntegrityVerifier", "INTEGRITY_OWNER", true, 5));
        rules.add(new B03StoreRule("B03-STORE-006", "DbStore", "IDEMPOTENCY_OWNER", true, 6));
        rules.add(new B03StoreRule("B03-STORE-007", "V51Repository", "REPLAY_OWNER", true, 7));
        rules.add(
                new B03StoreRule("B03-STORE-008", "DatabaseCoordinator", "NO_BET_OWNER", true, 1));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-009",
                        "DatabaseMigrationEngine",
                        "GAME_ISOLATION_OWNER",
                        true,
                        2));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-010", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 3));
        rules.add(new B03StoreRule("B03-STORE-011", "DbStore", "DRAW_OWNER", true, 4));
        rules.add(new B03StoreRule("B03-STORE-012", "V51Repository", "PREDICTION_OWNER", true, 5));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-013", "DatabaseCoordinator", "SETTLEMENT_OWNER", true, 6));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-014", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 7));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-015", "DatabaseIntegrityVerifier", "INTEGRITY_OWNER", true, 1));
        rules.add(new B03StoreRule("B03-STORE-016", "DbStore", "IDEMPOTENCY_OWNER", true, 2));
        rules.add(new B03StoreRule("B03-STORE-017", "V51Repository", "REPLAY_OWNER", true, 3));
        rules.add(
                new B03StoreRule("B03-STORE-018", "DatabaseCoordinator", "NO_BET_OWNER", true, 4));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-019",
                        "DatabaseMigrationEngine",
                        "GAME_ISOLATION_OWNER",
                        true,
                        5));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-020", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 6));
        rules.add(new B03StoreRule("B03-STORE-021", "DbStore", "DRAW_OWNER", true, 7));
        rules.add(new B03StoreRule("B03-STORE-022", "V51Repository", "PREDICTION_OWNER", true, 1));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-023", "DatabaseCoordinator", "SETTLEMENT_OWNER", true, 2));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-024", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 3));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-025", "DatabaseIntegrityVerifier", "INTEGRITY_OWNER", true, 4));
        rules.add(new B03StoreRule("B03-STORE-026", "DbStore", "IDEMPOTENCY_OWNER", true, 5));
        rules.add(new B03StoreRule("B03-STORE-027", "V51Repository", "REPLAY_OWNER", true, 6));
        rules.add(
                new B03StoreRule("B03-STORE-028", "DatabaseCoordinator", "NO_BET_OWNER", true, 7));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-029",
                        "DatabaseMigrationEngine",
                        "GAME_ISOLATION_OWNER",
                        true,
                        1));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-030", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 2));
        rules.add(new B03StoreRule("B03-STORE-031", "DbStore", "DRAW_OWNER", true, 3));
        rules.add(new B03StoreRule("B03-STORE-032", "V51Repository", "PREDICTION_OWNER", true, 4));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-033", "DatabaseCoordinator", "SETTLEMENT_OWNER", true, 5));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-034", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 6));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-035", "DatabaseIntegrityVerifier", "INTEGRITY_OWNER", true, 7));
        rules.add(new B03StoreRule("B03-STORE-036", "DbStore", "IDEMPOTENCY_OWNER", true, 1));
        rules.add(new B03StoreRule("B03-STORE-037", "V51Repository", "REPLAY_OWNER", true, 2));
        rules.add(
                new B03StoreRule("B03-STORE-038", "DatabaseCoordinator", "NO_BET_OWNER", true, 3));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-039",
                        "DatabaseMigrationEngine",
                        "GAME_ISOLATION_OWNER",
                        true,
                        4));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-040", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 5));
        rules.add(new B03StoreRule("B03-STORE-041", "DbStore", "DRAW_OWNER", true, 6));
        rules.add(new B03StoreRule("B03-STORE-042", "V51Repository", "PREDICTION_OWNER", true, 7));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-043", "DatabaseCoordinator", "SETTLEMENT_OWNER", true, 1));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-044", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 2));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-045", "DatabaseIntegrityVerifier", "INTEGRITY_OWNER", true, 3));
        rules.add(new B03StoreRule("B03-STORE-046", "DbStore", "IDEMPOTENCY_OWNER", true, 4));
        rules.add(new B03StoreRule("B03-STORE-047", "V51Repository", "REPLAY_OWNER", true, 5));
        rules.add(
                new B03StoreRule("B03-STORE-048", "DatabaseCoordinator", "NO_BET_OWNER", true, 6));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-049",
                        "DatabaseMigrationEngine",
                        "GAME_ISOLATION_OWNER",
                        true,
                        7));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-050", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 1));
        rules.add(new B03StoreRule("B03-STORE-051", "DbStore", "DRAW_OWNER", true, 2));
        rules.add(new B03StoreRule("B03-STORE-052", "V51Repository", "PREDICTION_OWNER", true, 3));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-053", "DatabaseCoordinator", "SETTLEMENT_OWNER", true, 4));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-054", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 5));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-055", "DatabaseIntegrityVerifier", "INTEGRITY_OWNER", true, 6));
        rules.add(new B03StoreRule("B03-STORE-056", "DbStore", "IDEMPOTENCY_OWNER", true, 7));
        rules.add(new B03StoreRule("B03-STORE-057", "V51Repository", "REPLAY_OWNER", true, 1));
        rules.add(
                new B03StoreRule("B03-STORE-058", "DatabaseCoordinator", "NO_BET_OWNER", true, 2));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-059",
                        "DatabaseMigrationEngine",
                        "GAME_ISOLATION_OWNER",
                        true,
                        3));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-060", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 4));
        rules.add(new B03StoreRule("B03-STORE-061", "DbStore", "DRAW_OWNER", true, 5));
        rules.add(new B03StoreRule("B03-STORE-062", "V51Repository", "PREDICTION_OWNER", true, 6));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-063", "DatabaseCoordinator", "SETTLEMENT_OWNER", true, 7));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-064", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 1));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-065", "DatabaseIntegrityVerifier", "INTEGRITY_OWNER", true, 2));
        rules.add(new B03StoreRule("B03-STORE-066", "DbStore", "IDEMPOTENCY_OWNER", true, 3));
        rules.add(new B03StoreRule("B03-STORE-067", "V51Repository", "REPLAY_OWNER", true, 4));
        rules.add(
                new B03StoreRule("B03-STORE-068", "DatabaseCoordinator", "NO_BET_OWNER", true, 5));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-069",
                        "DatabaseMigrationEngine",
                        "GAME_ISOLATION_OWNER",
                        true,
                        6));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-070", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 7));
        rules.add(new B03StoreRule("B03-STORE-071", "DbStore", "DRAW_OWNER", true, 1));
        rules.add(new B03StoreRule("B03-STORE-072", "V51Repository", "PREDICTION_OWNER", true, 2));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-073", "DatabaseCoordinator", "SETTLEMENT_OWNER", true, 3));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-074", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 4));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-075", "DatabaseIntegrityVerifier", "INTEGRITY_OWNER", true, 5));
        rules.add(new B03StoreRule("B03-STORE-076", "DbStore", "IDEMPOTENCY_OWNER", true, 6));
        rules.add(new B03StoreRule("B03-STORE-077", "V51Repository", "REPLAY_OWNER", true, 7));
        rules.add(
                new B03StoreRule("B03-STORE-078", "DatabaseCoordinator", "NO_BET_OWNER", true, 1));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-079",
                        "DatabaseMigrationEngine",
                        "GAME_ISOLATION_OWNER",
                        true,
                        2));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-080", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 3));
        rules.add(new B03StoreRule("B03-STORE-081", "DbStore", "DRAW_OWNER", true, 4));
        rules.add(new B03StoreRule("B03-STORE-082", "V51Repository", "PREDICTION_OWNER", true, 5));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-083", "DatabaseCoordinator", "SETTLEMENT_OWNER", true, 6));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-084", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 7));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-085", "DatabaseIntegrityVerifier", "INTEGRITY_OWNER", true, 1));
        rules.add(new B03StoreRule("B03-STORE-086", "DbStore", "IDEMPOTENCY_OWNER", true, 2));
        rules.add(new B03StoreRule("B03-STORE-087", "V51Repository", "REPLAY_OWNER", true, 3));
        rules.add(
                new B03StoreRule("B03-STORE-088", "DatabaseCoordinator", "NO_BET_OWNER", true, 4));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-089",
                        "DatabaseMigrationEngine",
                        "GAME_ISOLATION_OWNER",
                        true,
                        5));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-090", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 6));
        rules.add(new B03StoreRule("B03-STORE-091", "DbStore", "DRAW_OWNER", true, 7));
        rules.add(new B03StoreRule("B03-STORE-092", "V51Repository", "PREDICTION_OWNER", true, 1));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-093", "DatabaseCoordinator", "SETTLEMENT_OWNER", true, 2));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-094", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 3));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-095", "DatabaseIntegrityVerifier", "INTEGRITY_OWNER", true, 4));
        rules.add(new B03StoreRule("B03-STORE-096", "DbStore", "IDEMPOTENCY_OWNER", true, 5));
        rules.add(new B03StoreRule("B03-STORE-097", "V51Repository", "REPLAY_OWNER", true, 6));
        rules.add(
                new B03StoreRule("B03-STORE-098", "DatabaseCoordinator", "NO_BET_OWNER", true, 7));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-099",
                        "DatabaseMigrationEngine",
                        "GAME_ISOLATION_OWNER",
                        true,
                        1));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-100", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 2));
        rules.add(new B03StoreRule("B03-STORE-101", "DbStore", "DRAW_OWNER", true, 3));
        rules.add(new B03StoreRule("B03-STORE-102", "V51Repository", "PREDICTION_OWNER", true, 4));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-103", "DatabaseCoordinator", "SETTLEMENT_OWNER", true, 5));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-104", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 6));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-105", "DatabaseIntegrityVerifier", "INTEGRITY_OWNER", true, 7));
        rules.add(new B03StoreRule("B03-STORE-106", "DbStore", "IDEMPOTENCY_OWNER", true, 1));
        rules.add(new B03StoreRule("B03-STORE-107", "V51Repository", "REPLAY_OWNER", true, 2));
        rules.add(
                new B03StoreRule("B03-STORE-108", "DatabaseCoordinator", "NO_BET_OWNER", true, 3));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-109",
                        "DatabaseMigrationEngine",
                        "GAME_ISOLATION_OWNER",
                        true,
                        4));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-110", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 5));
        rules.add(new B03StoreRule("B03-STORE-111", "DbStore", "DRAW_OWNER", true, 6));
        rules.add(new B03StoreRule("B03-STORE-112", "V51Repository", "PREDICTION_OWNER", true, 7));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-113", "DatabaseCoordinator", "SETTLEMENT_OWNER", true, 1));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-114", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 2));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-115", "DatabaseIntegrityVerifier", "INTEGRITY_OWNER", true, 3));
        rules.add(new B03StoreRule("B03-STORE-116", "DbStore", "IDEMPOTENCY_OWNER", true, 4));
        rules.add(new B03StoreRule("B03-STORE-117", "V51Repository", "REPLAY_OWNER", true, 5));
        rules.add(
                new B03StoreRule("B03-STORE-118", "DatabaseCoordinator", "NO_BET_OWNER", true, 6));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-119",
                        "DatabaseMigrationEngine",
                        "GAME_ISOLATION_OWNER",
                        true,
                        7));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-120", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 1));
        rules.add(new B03StoreRule("B03-STORE-121", "DbStore", "DRAW_OWNER", true, 2));
        rules.add(new B03StoreRule("B03-STORE-122", "V51Repository", "PREDICTION_OWNER", true, 3));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-123", "DatabaseCoordinator", "SETTLEMENT_OWNER", true, 4));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-124", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 5));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-125", "DatabaseIntegrityVerifier", "INTEGRITY_OWNER", true, 6));
        rules.add(new B03StoreRule("B03-STORE-126", "DbStore", "IDEMPOTENCY_OWNER", true, 7));
        rules.add(new B03StoreRule("B03-STORE-127", "V51Repository", "REPLAY_OWNER", true, 1));
        rules.add(
                new B03StoreRule("B03-STORE-128", "DatabaseCoordinator", "NO_BET_OWNER", true, 2));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-129",
                        "DatabaseMigrationEngine",
                        "GAME_ISOLATION_OWNER",
                        true,
                        3));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-130", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 4));
        rules.add(new B03StoreRule("B03-STORE-131", "DbStore", "DRAW_OWNER", true, 5));
        rules.add(new B03StoreRule("B03-STORE-132", "V51Repository", "PREDICTION_OWNER", true, 6));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-133", "DatabaseCoordinator", "SETTLEMENT_OWNER", true, 7));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-134", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 1));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-135", "DatabaseIntegrityVerifier", "INTEGRITY_OWNER", true, 2));
        rules.add(new B03StoreRule("B03-STORE-136", "DbStore", "IDEMPOTENCY_OWNER", true, 3));
        rules.add(new B03StoreRule("B03-STORE-137", "V51Repository", "REPLAY_OWNER", true, 4));
        rules.add(
                new B03StoreRule("B03-STORE-138", "DatabaseCoordinator", "NO_BET_OWNER", true, 5));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-139",
                        "DatabaseMigrationEngine",
                        "GAME_ISOLATION_OWNER",
                        true,
                        6));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-140", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 7));
        rules.add(new B03StoreRule("B03-STORE-141", "DbStore", "DRAW_OWNER", true, 1));
        rules.add(new B03StoreRule("B03-STORE-142", "V51Repository", "PREDICTION_OWNER", true, 2));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-143", "DatabaseCoordinator", "SETTLEMENT_OWNER", true, 3));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-144", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 4));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-145", "DatabaseIntegrityVerifier", "INTEGRITY_OWNER", true, 5));
        rules.add(new B03StoreRule("B03-STORE-146", "DbStore", "IDEMPOTENCY_OWNER", true, 6));
        rules.add(new B03StoreRule("B03-STORE-147", "V51Repository", "REPLAY_OWNER", true, 7));
        rules.add(
                new B03StoreRule("B03-STORE-148", "DatabaseCoordinator", "NO_BET_OWNER", true, 1));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-149",
                        "DatabaseMigrationEngine",
                        "GAME_ISOLATION_OWNER",
                        true,
                        2));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-150", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 3));
        rules.add(new B03StoreRule("B03-STORE-151", "DbStore", "DRAW_OWNER", true, 4));
        rules.add(new B03StoreRule("B03-STORE-152", "V51Repository", "PREDICTION_OWNER", true, 5));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-153", "DatabaseCoordinator", "SETTLEMENT_OWNER", true, 6));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-154", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 7));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-155", "DatabaseIntegrityVerifier", "INTEGRITY_OWNER", true, 1));
        rules.add(new B03StoreRule("B03-STORE-156", "DbStore", "IDEMPOTENCY_OWNER", true, 2));
        rules.add(new B03StoreRule("B03-STORE-157", "V51Repository", "REPLAY_OWNER", true, 3));
        rules.add(
                new B03StoreRule("B03-STORE-158", "DatabaseCoordinator", "NO_BET_OWNER", true, 4));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-159",
                        "DatabaseMigrationEngine",
                        "GAME_ISOLATION_OWNER",
                        true,
                        5));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-160", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 6));
        rules.add(new B03StoreRule("B03-STORE-161", "DbStore", "DRAW_OWNER", true, 7));
        rules.add(new B03StoreRule("B03-STORE-162", "V51Repository", "PREDICTION_OWNER", true, 1));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-163", "DatabaseCoordinator", "SETTLEMENT_OWNER", true, 2));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-164", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 3));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-165", "DatabaseIntegrityVerifier", "INTEGRITY_OWNER", true, 4));
        rules.add(new B03StoreRule("B03-STORE-166", "DbStore", "IDEMPOTENCY_OWNER", true, 5));
        rules.add(new B03StoreRule("B03-STORE-167", "V51Repository", "REPLAY_OWNER", true, 6));
        rules.add(
                new B03StoreRule("B03-STORE-168", "DatabaseCoordinator", "NO_BET_OWNER", true, 7));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-169",
                        "DatabaseMigrationEngine",
                        "GAME_ISOLATION_OWNER",
                        true,
                        1));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-170", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 2));
        rules.add(new B03StoreRule("B03-STORE-171", "DbStore", "DRAW_OWNER", true, 3));
        rules.add(new B03StoreRule("B03-STORE-172", "V51Repository", "PREDICTION_OWNER", true, 4));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-173", "DatabaseCoordinator", "SETTLEMENT_OWNER", true, 5));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-174", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 6));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-175", "DatabaseIntegrityVerifier", "INTEGRITY_OWNER", true, 7));
        rules.add(new B03StoreRule("B03-STORE-176", "DbStore", "IDEMPOTENCY_OWNER", true, 1));
        rules.add(new B03StoreRule("B03-STORE-177", "V51Repository", "REPLAY_OWNER", true, 2));
        rules.add(
                new B03StoreRule("B03-STORE-178", "DatabaseCoordinator", "NO_BET_OWNER", true, 3));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-179",
                        "DatabaseMigrationEngine",
                        "GAME_ISOLATION_OWNER",
                        true,
                        4));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-180", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 5));
        rules.add(new B03StoreRule("B03-STORE-181", "DbStore", "DRAW_OWNER", true, 6));
        rules.add(new B03StoreRule("B03-STORE-182", "V51Repository", "PREDICTION_OWNER", true, 7));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-183", "DatabaseCoordinator", "SETTLEMENT_OWNER", true, 1));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-184", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 2));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-185", "DatabaseIntegrityVerifier", "INTEGRITY_OWNER", true, 3));
        rules.add(new B03StoreRule("B03-STORE-186", "DbStore", "IDEMPOTENCY_OWNER", true, 4));
        rules.add(new B03StoreRule("B03-STORE-187", "V51Repository", "REPLAY_OWNER", true, 5));
        rules.add(
                new B03StoreRule("B03-STORE-188", "DatabaseCoordinator", "NO_BET_OWNER", true, 6));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-189",
                        "DatabaseMigrationEngine",
                        "GAME_ISOLATION_OWNER",
                        true,
                        7));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-190", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 1));
        rules.add(new B03StoreRule("B03-STORE-191", "DbStore", "DRAW_OWNER", true, 2));
        rules.add(new B03StoreRule("B03-STORE-192", "V51Repository", "PREDICTION_OWNER", true, 3));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-193", "DatabaseCoordinator", "SETTLEMENT_OWNER", true, 4));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-194", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 5));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-195", "DatabaseIntegrityVerifier", "INTEGRITY_OWNER", true, 6));
        rules.add(new B03StoreRule("B03-STORE-196", "DbStore", "IDEMPOTENCY_OWNER", true, 7));
        rules.add(new B03StoreRule("B03-STORE-197", "V51Repository", "REPLAY_OWNER", true, 1));
        rules.add(
                new B03StoreRule("B03-STORE-198", "DatabaseCoordinator", "NO_BET_OWNER", true, 2));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-199",
                        "DatabaseMigrationEngine",
                        "GAME_ISOLATION_OWNER",
                        true,
                        3));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-200", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 4));
        rules.add(new B03StoreRule("B03-STORE-201", "DbStore", "DRAW_OWNER", true, 5));
        rules.add(new B03StoreRule("B03-STORE-202", "V51Repository", "PREDICTION_OWNER", true, 6));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-203", "DatabaseCoordinator", "SETTLEMENT_OWNER", true, 7));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-204", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 1));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-205", "DatabaseIntegrityVerifier", "INTEGRITY_OWNER", true, 2));
        rules.add(new B03StoreRule("B03-STORE-206", "DbStore", "IDEMPOTENCY_OWNER", true, 3));
        rules.add(new B03StoreRule("B03-STORE-207", "V51Repository", "REPLAY_OWNER", true, 4));
        rules.add(
                new B03StoreRule("B03-STORE-208", "DatabaseCoordinator", "NO_BET_OWNER", true, 5));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-209",
                        "DatabaseMigrationEngine",
                        "GAME_ISOLATION_OWNER",
                        true,
                        6));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-210", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 7));
        rules.add(new B03StoreRule("B03-STORE-211", "DbStore", "DRAW_OWNER", true, 1));
        rules.add(new B03StoreRule("B03-STORE-212", "V51Repository", "PREDICTION_OWNER", true, 2));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-213", "DatabaseCoordinator", "SETTLEMENT_OWNER", true, 3));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-214", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 4));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-215", "DatabaseIntegrityVerifier", "INTEGRITY_OWNER", true, 5));
        rules.add(new B03StoreRule("B03-STORE-216", "DbStore", "IDEMPOTENCY_OWNER", true, 6));
        rules.add(new B03StoreRule("B03-STORE-217", "V51Repository", "REPLAY_OWNER", true, 7));
        rules.add(
                new B03StoreRule("B03-STORE-218", "DatabaseCoordinator", "NO_BET_OWNER", true, 1));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-219",
                        "DatabaseMigrationEngine",
                        "GAME_ISOLATION_OWNER",
                        true,
                        2));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-220", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 3));
        rules.add(new B03StoreRule("B03-STORE-221", "DbStore", "DRAW_OWNER", true, 4));
        rules.add(new B03StoreRule("B03-STORE-222", "V51Repository", "PREDICTION_OWNER", true, 5));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-223", "DatabaseCoordinator", "SETTLEMENT_OWNER", true, 6));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-224", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 7));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-225", "DatabaseIntegrityVerifier", "INTEGRITY_OWNER", true, 1));
        rules.add(new B03StoreRule("B03-STORE-226", "DbStore", "IDEMPOTENCY_OWNER", true, 2));
        rules.add(new B03StoreRule("B03-STORE-227", "V51Repository", "REPLAY_OWNER", true, 3));
        rules.add(
                new B03StoreRule("B03-STORE-228", "DatabaseCoordinator", "NO_BET_OWNER", true, 4));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-229",
                        "DatabaseMigrationEngine",
                        "GAME_ISOLATION_OWNER",
                        true,
                        5));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-230", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 6));
        rules.add(new B03StoreRule("B03-STORE-231", "DbStore", "DRAW_OWNER", true, 7));
        rules.add(new B03StoreRule("B03-STORE-232", "V51Repository", "PREDICTION_OWNER", true, 1));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-233", "DatabaseCoordinator", "SETTLEMENT_OWNER", true, 2));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-234", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 3));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-235", "DatabaseIntegrityVerifier", "INTEGRITY_OWNER", true, 4));
        rules.add(new B03StoreRule("B03-STORE-236", "DbStore", "IDEMPOTENCY_OWNER", true, 5));
        rules.add(new B03StoreRule("B03-STORE-237", "V51Repository", "REPLAY_OWNER", true, 6));
        rules.add(
                new B03StoreRule("B03-STORE-238", "DatabaseCoordinator", "NO_BET_OWNER", true, 7));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-239",
                        "DatabaseMigrationEngine",
                        "GAME_ISOLATION_OWNER",
                        true,
                        1));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-240", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 2));
        rules.add(new B03StoreRule("B03-STORE-241", "DbStore", "DRAW_OWNER", true, 3));
        rules.add(new B03StoreRule("B03-STORE-242", "V51Repository", "PREDICTION_OWNER", true, 4));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-243", "DatabaseCoordinator", "SETTLEMENT_OWNER", true, 5));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-244", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 6));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-245", "DatabaseIntegrityVerifier", "INTEGRITY_OWNER", true, 7));
        rules.add(new B03StoreRule("B03-STORE-246", "DbStore", "IDEMPOTENCY_OWNER", true, 1));
        rules.add(new B03StoreRule("B03-STORE-247", "V51Repository", "REPLAY_OWNER", true, 2));
        rules.add(
                new B03StoreRule("B03-STORE-248", "DatabaseCoordinator", "NO_BET_OWNER", true, 3));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-249",
                        "DatabaseMigrationEngine",
                        "GAME_ISOLATION_OWNER",
                        true,
                        4));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-250", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 5));
        rules.add(new B03StoreRule("B03-STORE-251", "DbStore", "DRAW_OWNER", true, 6));
        rules.add(new B03StoreRule("B03-STORE-252", "V51Repository", "PREDICTION_OWNER", true, 7));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-253", "DatabaseCoordinator", "SETTLEMENT_OWNER", true, 1));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-254", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 2));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-255", "DatabaseIntegrityVerifier", "INTEGRITY_OWNER", true, 3));
        rules.add(new B03StoreRule("B03-STORE-256", "DbStore", "IDEMPOTENCY_OWNER", true, 4));
        rules.add(new B03StoreRule("B03-STORE-257", "V51Repository", "REPLAY_OWNER", true, 5));
        rules.add(
                new B03StoreRule("B03-STORE-258", "DatabaseCoordinator", "NO_BET_OWNER", true, 6));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-259",
                        "DatabaseMigrationEngine",
                        "GAME_ISOLATION_OWNER",
                        true,
                        7));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-260", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 1));
        rules.add(new B03StoreRule("B03-STORE-261", "DbStore", "DRAW_OWNER", true, 2));
        rules.add(new B03StoreRule("B03-STORE-262", "V51Repository", "PREDICTION_OWNER", true, 3));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-263", "DatabaseCoordinator", "SETTLEMENT_OWNER", true, 4));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-264", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 5));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-265", "DatabaseIntegrityVerifier", "INTEGRITY_OWNER", true, 6));
        rules.add(new B03StoreRule("B03-STORE-266", "DbStore", "IDEMPOTENCY_OWNER", true, 7));
        rules.add(new B03StoreRule("B03-STORE-267", "V51Repository", "REPLAY_OWNER", true, 1));
        rules.add(
                new B03StoreRule("B03-STORE-268", "DatabaseCoordinator", "NO_BET_OWNER", true, 2));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-269",
                        "DatabaseMigrationEngine",
                        "GAME_ISOLATION_OWNER",
                        true,
                        3));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-270", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 4));
        rules.add(new B03StoreRule("B03-STORE-271", "DbStore", "DRAW_OWNER", true, 5));
        rules.add(new B03StoreRule("B03-STORE-272", "V51Repository", "PREDICTION_OWNER", true, 6));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-273", "DatabaseCoordinator", "SETTLEMENT_OWNER", true, 7));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-274", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 1));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-275", "DatabaseIntegrityVerifier", "INTEGRITY_OWNER", true, 2));
        rules.add(new B03StoreRule("B03-STORE-276", "DbStore", "IDEMPOTENCY_OWNER", true, 3));
        rules.add(new B03StoreRule("B03-STORE-277", "V51Repository", "REPLAY_OWNER", true, 4));
        rules.add(
                new B03StoreRule("B03-STORE-278", "DatabaseCoordinator", "NO_BET_OWNER", true, 5));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-279",
                        "DatabaseMigrationEngine",
                        "GAME_ISOLATION_OWNER",
                        true,
                        6));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-280", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 7));
        rules.add(new B03StoreRule("B03-STORE-281", "DbStore", "DRAW_OWNER", true, 1));
        rules.add(new B03StoreRule("B03-STORE-282", "V51Repository", "PREDICTION_OWNER", true, 2));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-283", "DatabaseCoordinator", "SETTLEMENT_OWNER", true, 3));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-284", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 4));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-285", "DatabaseIntegrityVerifier", "INTEGRITY_OWNER", true, 5));
        rules.add(new B03StoreRule("B03-STORE-286", "DbStore", "IDEMPOTENCY_OWNER", true, 6));
        rules.add(new B03StoreRule("B03-STORE-287", "V51Repository", "REPLAY_OWNER", true, 7));
        rules.add(
                new B03StoreRule("B03-STORE-288", "DatabaseCoordinator", "NO_BET_OWNER", true, 1));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-289",
                        "DatabaseMigrationEngine",
                        "GAME_ISOLATION_OWNER",
                        true,
                        2));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-290", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 3));
        rules.add(new B03StoreRule("B03-STORE-291", "DbStore", "DRAW_OWNER", true, 4));
        rules.add(new B03StoreRule("B03-STORE-292", "V51Repository", "PREDICTION_OWNER", true, 5));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-293", "DatabaseCoordinator", "SETTLEMENT_OWNER", true, 6));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-294", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 7));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-295", "DatabaseIntegrityVerifier", "INTEGRITY_OWNER", true, 1));
        rules.add(new B03StoreRule("B03-STORE-296", "DbStore", "IDEMPOTENCY_OWNER", true, 2));
        rules.add(new B03StoreRule("B03-STORE-297", "V51Repository", "REPLAY_OWNER", true, 3));
        rules.add(
                new B03StoreRule("B03-STORE-298", "DatabaseCoordinator", "NO_BET_OWNER", true, 4));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-299",
                        "DatabaseMigrationEngine",
                        "GAME_ISOLATION_OWNER",
                        true,
                        5));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-300", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 6));
        rules.add(new B03StoreRule("B03-STORE-301", "DbStore", "DRAW_OWNER", true, 7));
        rules.add(new B03StoreRule("B03-STORE-302", "V51Repository", "PREDICTION_OWNER", true, 1));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-303", "DatabaseCoordinator", "SETTLEMENT_OWNER", true, 2));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-304", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 3));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-305", "DatabaseIntegrityVerifier", "INTEGRITY_OWNER", true, 4));
        rules.add(new B03StoreRule("B03-STORE-306", "DbStore", "IDEMPOTENCY_OWNER", true, 5));
        rules.add(new B03StoreRule("B03-STORE-307", "V51Repository", "REPLAY_OWNER", true, 6));
        rules.add(
                new B03StoreRule("B03-STORE-308", "DatabaseCoordinator", "NO_BET_OWNER", true, 7));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-309",
                        "DatabaseMigrationEngine",
                        "GAME_ISOLATION_OWNER",
                        true,
                        1));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-310", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 2));
        rules.add(new B03StoreRule("B03-STORE-311", "DbStore", "DRAW_OWNER", true, 3));
        rules.add(new B03StoreRule("B03-STORE-312", "V51Repository", "PREDICTION_OWNER", true, 4));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-313", "DatabaseCoordinator", "SETTLEMENT_OWNER", true, 5));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-314", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 6));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-315", "DatabaseIntegrityVerifier", "INTEGRITY_OWNER", true, 7));
        rules.add(new B03StoreRule("B03-STORE-316", "DbStore", "IDEMPOTENCY_OWNER", true, 1));
        rules.add(new B03StoreRule("B03-STORE-317", "V51Repository", "REPLAY_OWNER", true, 2));
        rules.add(
                new B03StoreRule("B03-STORE-318", "DatabaseCoordinator", "NO_BET_OWNER", true, 3));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-319",
                        "DatabaseMigrationEngine",
                        "GAME_ISOLATION_OWNER",
                        true,
                        4));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-320", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 5));
        rules.add(new B03StoreRule("B03-STORE-321", "DbStore", "DRAW_OWNER", true, 6));
        rules.add(new B03StoreRule("B03-STORE-322", "V51Repository", "PREDICTION_OWNER", true, 7));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-323", "DatabaseCoordinator", "SETTLEMENT_OWNER", true, 1));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-324", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 2));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-325", "DatabaseIntegrityVerifier", "INTEGRITY_OWNER", true, 3));
        rules.add(new B03StoreRule("B03-STORE-326", "DbStore", "IDEMPOTENCY_OWNER", true, 4));
        rules.add(new B03StoreRule("B03-STORE-327", "V51Repository", "REPLAY_OWNER", true, 5));
        rules.add(
                new B03StoreRule("B03-STORE-328", "DatabaseCoordinator", "NO_BET_OWNER", true, 6));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-329",
                        "DatabaseMigrationEngine",
                        "GAME_ISOLATION_OWNER",
                        true,
                        7));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-330", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 1));
        rules.add(new B03StoreRule("B03-STORE-331", "DbStore", "DRAW_OWNER", true, 2));
        rules.add(new B03StoreRule("B03-STORE-332", "V51Repository", "PREDICTION_OWNER", true, 3));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-333", "DatabaseCoordinator", "SETTLEMENT_OWNER", true, 4));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-334", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 5));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-335", "DatabaseIntegrityVerifier", "INTEGRITY_OWNER", true, 6));
        rules.add(new B03StoreRule("B03-STORE-336", "DbStore", "IDEMPOTENCY_OWNER", true, 7));
        rules.add(new B03StoreRule("B03-STORE-337", "V51Repository", "REPLAY_OWNER", true, 1));
        rules.add(
                new B03StoreRule("B03-STORE-338", "DatabaseCoordinator", "NO_BET_OWNER", true, 2));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-339",
                        "DatabaseMigrationEngine",
                        "GAME_ISOLATION_OWNER",
                        true,
                        3));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-340", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 4));
        rules.add(new B03StoreRule("B03-STORE-341", "DbStore", "DRAW_OWNER", true, 5));
        rules.add(new B03StoreRule("B03-STORE-342", "V51Repository", "PREDICTION_OWNER", true, 6));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-343", "DatabaseCoordinator", "SETTLEMENT_OWNER", true, 7));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-344", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 1));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-345", "DatabaseIntegrityVerifier", "INTEGRITY_OWNER", true, 2));
        rules.add(new B03StoreRule("B03-STORE-346", "DbStore", "IDEMPOTENCY_OWNER", true, 3));
        rules.add(new B03StoreRule("B03-STORE-347", "V51Repository", "REPLAY_OWNER", true, 4));
        rules.add(
                new B03StoreRule("B03-STORE-348", "DatabaseCoordinator", "NO_BET_OWNER", true, 5));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-349",
                        "DatabaseMigrationEngine",
                        "GAME_ISOLATION_OWNER",
                        true,
                        6));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-350", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 7));
        rules.add(new B03StoreRule("B03-STORE-351", "DbStore", "DRAW_OWNER", true, 1));
        rules.add(new B03StoreRule("B03-STORE-352", "V51Repository", "PREDICTION_OWNER", true, 2));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-353", "DatabaseCoordinator", "SETTLEMENT_OWNER", true, 3));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-354", "DatabaseMigrationEngine", "MIGRATION_OWNER", true, 4));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-355", "DatabaseIntegrityVerifier", "INTEGRITY_OWNER", true, 5));
        rules.add(new B03StoreRule("B03-STORE-356", "DbStore", "IDEMPOTENCY_OWNER", true, 6));
        rules.add(new B03StoreRule("B03-STORE-357", "V51Repository", "REPLAY_OWNER", true, 7));
        rules.add(
                new B03StoreRule("B03-STORE-358", "DatabaseCoordinator", "NO_BET_OWNER", true, 1));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-359",
                        "DatabaseMigrationEngine",
                        "GAME_ISOLATION_OWNER",
                        true,
                        2));
        rules.add(
                new B03StoreRule(
                        "B03-STORE-360", "DatabaseIntegrityVerifier", "HASH_OWNER", true, 3));
        return java.util.Collections.unmodifiableList(rules);
    }

    public static String tianshuB03StoreRuleHash() {
        StringBuilder b = new StringBuilder();
        java.util.List<B03StoreRule> x = tianshuB03StoreRules();
        for (int i = 0; i < x.size(); i++) {
            if (i > 0) b.append('|');
            b.append(x.get(i).canonical());
        }
        return V51ChecksumEngine.sha256(b.toString());
    }

    public static boolean tianshuB03SelfTest() {
        return tianshuB03StoreRules().size() == 360
                && tianshuB03StoreRuleHash().length() == 64
                && "binance_k3_nova_one_pro_v57.db".equals(AppConfig.DB_NAME)
                && V51BuildManifest.SHADOW_ONLY
                && !V51BuildManifest.MODEL_PROVEN
                && V51BuildManifest.REAL_BET_HARD_DISABLED
                && !V51BuildManifest.CANDIDATE_REAL_AUTH_ENABLED;
    }
    /* STARCORE_TIANSHU_B03_DBSTORE_END */

public static final long CP12_DRAW_RETENTION_DEFERRED=-3L,CP12_DRAW_OUTSIDE_HISTORY_TAIL=-4L;

public int replayRetentionDeferredDrawsCP12(int limit){
        if(android.os.Looper.myLooper()==android.os.Looper.getMainLooper())throw new IllegalStateException("DRAW_RETENTION_REPLAY_MAIN_THREAD");
        ArrayList<Models.Draw> pending=new ArrayList<>();
        String sql="SELECT p.parsed_issue,p.server_open_time,p.observed_at,p.source_line,p.permutation,p.raw_payload_sha256,p.draw_hash FROM "+DbHelper.TABLE_R811_DRAW_PROVENANCE+" p JOIN "+DbHelper.TABLE_R811_DRAW_CANONICAL+" c ON c.target_issue=p.parsed_issue WHERE c.status='CONFIRMED_CANONICAL_DRAW' AND NOT EXISTS(SELECT 1 FROM draws d WHERE d.issue=p.parsed_issue) AND p.draw_hash=c.canonical_draw_hash AND ((SELECT COUNT(*) FROM draws WHERE dirty=0)<5000 OR p.parsed_issue>(SELECT MIN(issue) FROM draws WHERE dirty=0)) AND p.provenance_id=(SELECT v.provenance_id FROM "+DbHelper.TABLE_R811_DRAW_PROVENANCE+" v WHERE v.parsed_issue=p.parsed_issue AND v.draw_hash=p.draw_hash ORDER BY v.observed_at,v.provenance_id LIMIT 1) ORDER BY p.parsed_issue DESC LIMIT "+Math.max(1,Math.min(8,limit));
        try(Cursor c=readable().rawQuery(sql,null)){while(c.moveToNext()){
            Models.Draw d=new Models.Draw();d.issue=c.getString(0);d.openTime=c.getLong(1);d.fetchTime=c.getLong(2);d.sourceLine=c.getString(3);d.rawJson="";
            String text=safeString(c.getString(4));if(text.length()>40)throw new IllegalStateException("DRAW_RETENTION_PERMUTATION_BOUND");String[] values=text.split(",",-1);if(values.length!=10)throw new IllegalStateException("DRAW_RETENTION_PERMUTATION_COUNT");
            for(int i=0;i<10;i++)d.numbers[i]=Integer.parseInt(values[i].trim());if(!d.isValid())throw new IllegalStateException("DRAW_RETENTION_PROVENANCE_INVALID");FetchLedgerR925.restoreDeferredDrawR82(readable(),d,c.getString(5),c.getString(6));pending.add(d);
        }}
        java.util.Collections.reverse(pending); // Most recent page, applied in chronological order.
        int applied=0;for(Models.Draw draw:pending){long saved=saveDraw(draw);if(saved==CP12_DRAW_RETENTION_DEFERRED)break;if(saved>=0)applied++;}return applied;
    }
}
