package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * R911 runtime SQL contract gate.  Java compilation cannot prove Android SQLite schema/query
 * compatibility, so Research must successfully prepare its real hot-path SELECTs before a tick is
 * allowed to execute.  Failure is Research-only and never blocks Production fetch/analysis/
 * settlement.
 */
public final class ResearchRuntimeSqlGateR911 {
    private ResearchRuntimeSqlGateR911() {}
    private static final java.util.Map<SQLiteDatabase, VerifiedSchema> VERIFIED =
            new java.util.WeakHashMap<SQLiteDatabase, VerifiedSchema>();
    private static final class VerifiedSchema {
        final long revision;
        final Report report;
        VerifiedSchema(long r, Report p) { revision=r; report=p; }
    }

    public static Report verify(DbStore store) {
        if (store == null) return Report.fail("STORE_NULL");
        try {
            SQLiteDatabase db = store.readable();
            ResearchCaptureQueueR946.verifySqlContract(db);
            prepare(db,
                    "SELECT e.target_issue,e.raw_action_p_value,e.rank_score_value,e.rank_status"
                            + " FROM " + DbHelper.TABLE_R906_SELECTED_EVIDENCE + " e LIMIT 0");
            prepare(db,
                    "SELECT o.target_issue,e.rank_status,t.status,s.settled"
                            + " FROM " + DbHelper.TABLE_R35_OOS_LABEL + " o"
                            + " JOIN " + DbHelper.TABLE_R906_SELECTED_EVIDENCE
                            + " e ON e.target_issue=o.target_issue"
                            + " JOIN " + DbHelper.TABLE_R36_SETTLEMENT_TASK
                            + " t ON t.target_issue=o.target_issue"
                            + " JOIN " + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                            + " s ON s.target_issue=o.target_issue"
                            + " WHERE o.strict_oos=1 AND t.status='COMPLETED'"
                            + " AND s.pre_draw_frozen=1 AND s.settled=1 LIMIT 0");
            prepare(db,
                    "SELECT target_issue,feature_blob,hit_label FROM "
                            + DbHelper.TABLE_R906_RESEARCH_COMPACT_LABEL
                            + " WHERE asset_id='PRODUCTION_SELECTED' LIMIT 0");
            prepare(db,
                    "SELECT artifact_blob,artifact_hash,trained_until_issue FROM"
                            + " v57_research_artifact_r811 WHERE artifact_type='FTRL_SHADOW' LIMIT 0");
            prepare(db,
                    "SELECT genome_id,target_issue,hit_label FROM "
                            + DbHelper.TABLE_R908_ALGORITHM_PREDRAW + " LIMIT 0");
            prepare(db,
                    "SELECT display_name_zh,mechanism_spec_zh,future_n,future_hit_rate,"
                            + "future_wilson_lower,future_brier,future_ece,future_coverage,"
                            + "future_block_min,future_regime_min,future_max_miss,"
                            + "paired_mainline_n,paired_mainline_delta,lifecycle_reason FROM "
                            + DbHelper.TABLE_R908_ALGORITHM_GENOME + " LIMIT 0");
            prepare(db,
                    "SELECT shape_lifecycle,shape_prototype,shape_run_length,shape_change_risk,shape_confidence"
                            + " FROM " + V51Repository.SELECTED + " LIMIT 0");
            prepare(db,
                    "SELECT target_issue FROM (SELECT target_issue FROM "
                            + DbHelper.TABLE_R817_RESEARCH_PREDRAW + " WHERE hit_label=-1 UNION "
                            + "SELECT target_issue FROM " + DbHelper.TABLE_R908_ALGORITHM_PREDRAW
                            + " WHERE hit_label=-1) pending LIMIT 0");
            prepare(db,
                    "SELECT candidate_id,target_issue,hit_label FROM "
                            + DbHelper.TABLE_R817_RESEARCH_PREDRAW + " LIMIT 0");
            prepare(db,"SELECT candidate_key,target_issue,structural_hash,input_hash,action_count,payload,proof_hash FROM "+CandidateExecutionAuditR932.TABLE+" LIMIT 0");
            prepare(db,"SELECT window_id,candidate_key,state,cursor_issue,verified_n,certificate_hash FROM "+FixedWindowCertificatePublisherR932.TABLE+" LIMIT 0");
            prepare(db,"SELECT candidate_id,target_issue,frozen_features,input_hash,evidence_role,proof_error FROM r952_candidate_predraw LIMIT 0");
            prepare(db,"SELECT artifact_hash,model_payload,numeric_updates FROM r952_artifact LIMIT 0");
            return new Report(true, "PASS", "R958_RUNTIME_SQL_CONTRACT_V4");
        } catch (ResearchWorkBudgetR927.Yield yielded) {
            throw yielded;
        } catch (RuntimeResourceAuditR927.BudgetExceeded yielded) {
            throw yielded;
        } catch (RuntimeException failure) {
            return Report.fail(failure.getClass().getSimpleName() + ':' + safe(failure.getMessage()));
        }
    }

    public static Report verifyAndPublish(DbStore store) {
        Report r;
        try {
            SQLiteDatabase db=store==null?null:store.readable();
            long revision=schemaRevision(db);
            synchronized(VERIFIED) {
                VerifiedSchema previous=VERIFIED.get(db);
                if(previous!=null && previous.revision==revision) return previous.report;
            }
            r=verify(store);
            // Cache only a complete successful preparation against an unchanged schema.
            // Any DDL, reopened connection or process restart requires fresh verification.
            if(r.passed && revision==schemaRevision(db)) {
                synchronized(VERIFIED) { VERIFIED.put(db,new VerifiedSchema(revision,r)); }
            } else if(r.passed) r=Report.fail("SCHEMA_CHANGED_DURING_VERIFICATION");
        } catch (ResearchWorkBudgetR927.Yield yielded) { throw yielded;
        } catch (RuntimeResourceAuditR927.BudgetExceeded yielded) { throw yielded;
        } catch (RuntimeException failure) {
            r=Report.fail(failure.getClass().getSimpleName()+':'+safe(failure.getMessage()));
        }
        if (store != null) {
            try {
                store.setState("r911_research_sql_gate", r.passed ? "PASS" : "FAIL:" + r.detail);
                store.setState("r911_research_sql_gate_at", String.valueOf(System.currentTimeMillis()));
            } catch (Throwable ignore) {}
        }
        return r;
    }

    private static long schemaRevision(SQLiteDatabase db) {
        if(db==null)throw new IllegalStateException("STORE_NULL");
        try(Cursor c=db.rawQuery("PRAGMA schema_version",null)) {
            if(!c.moveToFirst())throw new IllegalStateException("SCHEMA_VERSION_UNAVAILABLE");
            return c.getLong(0);
        }
    }

    private static void prepare(SQLiteDatabase db, String sql) {
        Cursor c = null;
        try {
            c = db.rawQuery(sql, null);
            if (c != null) c.getColumnCount();
        } finally {
            if (c != null) c.close();
        }
    }

    public static final class Report {
        public final boolean passed;
        public final String status, detail;
        Report(boolean p, String s, String d) { passed=p; status=safe(s); detail=safe(d); }
        static Report fail(String d) { return new Report(false, "FAIL", d); }
        public String canonical() {
            return "R911_RESEARCH_SQL_GATE=" + status + "|detail=" + detail;
        }
    }

    private static String safe(String x) { return x == null ? "" : x.trim(); }
}
