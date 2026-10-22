package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Minimal immutable input for unfinished consumers after a draw leaves hot history.
 * Schema is installed by the existing Critical migration owner. This is not a second
 * history, fetch, settlement or training owner. Normal history reads remain at 5000.
 */
public final class DrawDependencyInputCP12 {
    public static final String TABLE = "draw_dependency_input_cp12";
    public static final String SOURCE = "draw_dependency_source_cp12";
    private static final String COLUMNS = "_id,issue,open_time,fetch_time,source_line,"
            + "n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,dirty,dirty_reason,"
            + "first_seen_at,draw_hash_version,draw_hash_v2";
    private DrawDependencyInputCP12() {}

    /** Called only by DbHelper.ensureR815CriticalSchema, never during ingress. */
    static void schemaWithinMigration(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE + "("
                + "_id INTEGER NOT NULL,issue TEXT PRIMARY KEY,open_time INTEGER NOT NULL,"
                + "fetch_time INTEGER NOT NULL,source_line TEXT,"
                + "n1 INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,n4 INTEGER NOT NULL,"
                + "n5 INTEGER NOT NULL,n6 INTEGER NOT NULL,n7 INTEGER NOT NULL,n8 INTEGER NOT NULL,"
                + "n9 INTEGER NOT NULL,n10 INTEGER NOT NULL,dirty INTEGER NOT NULL CHECK(dirty=0),"
                + "dirty_reason TEXT,first_seen_at INTEGER NOT NULL,draw_hash_version INTEGER NOT NULL,"
                + "draw_hash_v2 TEXT NOT NULL,input_hash TEXT NOT NULL,retained_at INTEGER NOT NULL)");
        db.execSQL("CREATE VIEW IF NOT EXISTS " + SOURCE + " AS SELECT " + COLUMNS
                + ",raw_json FROM draws UNION ALL SELECT " + COLUMNS + ",'' AS raw_json FROM "
                + TABLE + " e WHERE NOT EXISTS(SELECT 1 FROM draws d WHERE d.issue=e.issue)");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS cp12_draw_dependency_immutable BEFORE UPDATE ON "
                + TABLE + " BEGIN SELECT RAISE(ABORT,'CP12_DRAW_DEPENDENCY_IMMUTABLE'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS cp12_draw_dependency_delete_guard BEFORE DELETE ON "
                + TABLE + " WHEN NOT EXISTS(SELECT 1 FROM app_state WHERE state_key='cp12_draw_dependency_release_issue'"
                + " AND state_value=OLD.issue) BEGIN SELECT RAISE(ABORT,'CP12_DRAW_DEPENDENCY_IN_USE'); END");
    }

    /** Preserve exactly the evicted input before deletion, in the SAME writer transaction.
     * Do not copy provider raw_json. All first-observation and label boundaries are retained.
     */
    static void preserveIfReferencedWithinWrite(SQLiteDatabase db, long rowid, String issue) {
        requireWriter(db);
        Reference reference = referenceSql(db, "?");
        String[] issues = new String[reference.count];
        java.util.Arrays.fill(issues, issue);
        try (Cursor referenced = db.rawQuery("SELECT 1 WHERE " + reference.sql, issues)) {
            if (!referenced.moveToFirst()) return;
        }
        String inputHash;
        try (Cursor row = db.rawQuery("SELECT " + COLUMNS + " FROM draws WHERE rowid=? AND issue=? AND dirty=0",
                new String[]{Long.toString(rowid), issue})) {
            if (!row.moveToFirst()) throw new IllegalStateException("CP12_DRAW_DEPENDENCY_SOURCE_MISSING");
            inputHash = inputHash(row);
        }
        db.execSQL("INSERT OR IGNORE INTO " + TABLE + "(" + COLUMNS + ",input_hash,retained_at) SELECT "
                + COLUMNS + ",?,? FROM draws WHERE rowid=? AND issue=? AND dirty=0",
                new Object[]{inputHash, System.currentTimeMillis(), rowid, issue});
        try (Cursor copied = db.rawQuery("SELECT " + COLUMNS + ",input_hash FROM " + TABLE + " WHERE issue=?",
                new String[]{issue})) {
            if (!copied.moveToFirst() || !inputHash.equals(copied.getString(20))
                    || !inputHash.equals(inputHash(copied)))
                throw new IllegalStateException("CP12_DRAW_DEPENDENCY_COPY_DRIFT");
        }
    }

    /** Release a bounded number only after ALL actual consumers have released the input.
     * Labels, predictions, tasks, receipts and results are never changed by this operation.
     */
    static int releaseConsumedWithinWrite(SQLiteDatabase db, int limit) {
        requireWriter(db);
        Reference reference = referenceSql(db, "e.issue");
        List<String> releasable = new ArrayList<>();
        try (Cursor rows = db.rawQuery("SELECT e.issue FROM " + TABLE + " e WHERE NOT (" + reference.sql
                + ") ORDER BY e.issue LIMIT " + Math.max(1, Math.min(32, limit)), null)) {
            while (rows.moveToNext()) releasable.add(rows.getString(0));
        }
        int removed = 0;
        for (String issue : releasable) {
            db.execSQL("INSERT OR REPLACE INTO app_state(state_key,state_value,updated_at)"
                    + " VALUES('cp12_draw_dependency_release_issue',?,?)", new Object[]{issue, System.currentTimeMillis()});
            try { removed += db.delete(TABLE, "issue=?", new String[]{issue}); }
            finally { db.execSQL("DELETE FROM app_state WHERE state_key='cp12_draw_dependency_release_issue'"); }
        }
        return removed;
    }

    private static final class Reference {
        final String sql; final int count;
        Reference(List<String> clauses) {
            StringBuilder joined = new StringBuilder();
            for (String clause : clauses) {
                if (joined.length() > 0) joined.append(" OR ");
                joined.append(clause);
            }
            sql = joined.toString(); count = clauses.size();
        }
    }
    /** Critical ingress must also work before the additive Research schema is ready.
     * Missing optional tables have no inputs to preserve. An incomplete pin pair is
     * rejected, rather than losing a real reference. No hot-path DDL is performed.
     */
    private static Reference referenceSql(SQLiteDatabase db,String issue) {
        Set<String> installed = new HashSet<>();
        try (Cursor schema = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name IN("
                + "'v57_research_input_pin_r967','v57_research_task_r967','r952_candidate_predraw',"
                + "'r958_frozen_predraw','r952_research_outbox','atomic_tool_context_r943',"
                + "'v57_algorithm_predraw_r908','v57_research_predraw_r817','research_new_asset_freeze_v41',"
                + "'research_fixed_pair_r932','research_candidate_cal_observation_r932','research_candidate_cal_study_r932',"
                + "'research_challenge_prediction_r932','research_legacy_asset_head_audit_r80','research_bound_source_r80','research_bound_cohort_r80','research_bound_input_r80','r80_probe_forecast','r80_probe_label')",null)) {
            while(schema.moveToNext())installed.add(schema.getString(0));
        }
        List<String> clauses = new ArrayList<>();
        if(installed.contains("v57_research_input_pin_r967")!=installed.contains("v57_research_task_r967"))
            throw new IllegalStateException("CP12_DRAW_DEPENDENCY_PARTIAL_PIN_SCHEMA");
        if(installed.contains("v57_research_input_pin_r967"))clauses.add("EXISTS(SELECT 1 FROM v57_research_input_pin_r967 p"
                + " JOIN v57_research_task_r967 t ON t.task_id=p.task_id WHERE p.source_table='draws' AND p.source_issue=" + issue
                + " AND t.status NOT IN('COMPLETED','EXPIRED','FAILED_TERMINAL'))");
        if(installed.contains("r952_candidate_predraw"))clauses.add("EXISTS(SELECT 1 FROM r952_candidate_predraw p WHERE p.target_issue=" + issue + " AND p.settled=0)");
        if(installed.contains("r958_frozen_predraw"))clauses.add("EXISTS(SELECT 1 FROM r958_frozen_predraw p WHERE p.target_issue=" + issue + " AND p.settled=0)");
        if(installed.contains("r952_research_outbox"))clauses.add("EXISTS(SELECT 1 FROM r952_research_outbox o WHERE o.target_issue=" + issue
                + " AND o.state NOT IN('COMPLETED','DEPENDENCY_RETIRED'))");
        // Unsettled bound sources need original draw input. Settled sources contain
        // their own immutable authoritative label snapshot. Optional research
        // tables may be absent during critical-only draw ingress/startup.
        if(installed.contains("research_legacy_asset_head_audit_r80")&&installed.contains("research_new_asset_freeze_v41"))clauses.add("EXISTS(SELECT 1 FROM research_new_asset_freeze_v41 p JOIN research_legacy_asset_head_audit_r80 a ON a.asset_id=p.asset_id WHERE p.target_issue="+issue+" AND p.hit_label IN(0,1) AND a.status IN('COPYING','RUNNING','STALE'))");
        if(installed.contains("research_bound_source_r80"))clauses.add("EXISTS(SELECT 1 FROM research_bound_source_r80 p WHERE p.target_issue=" + issue + " AND p.settled=0 AND p.proof_error='')");
        if(installed.contains("research_bound_cohort_r80")&&installed.contains("research_bound_input_r80"))clauses.add("EXISTS(SELECT 1 FROM research_bound_input_r80 i JOIN research_bound_cohort_r80 c ON c.cohort_hash=i.cohort_hash WHERE i.target_issue="+issue+" AND c.state IN('READY','PROPOSED') AND i.target_issue<=c.selection_issue AND i.target_issue IN(SELECT f.target_issue FROM research_bound_input_r80 f WHERE f.cohort_hash=c.cohort_hash AND f.target_issue<=c.selection_issue ORDER BY f.target_issue DESC LIMIT 921))");
        if(installed.contains("r80_probe_forecast")){
            // A partial optional schema must preserve input rather than fail ingress.
            String unresolved=installed.contains("r80_probe_label")
                    ? " AND NOT EXISTS(SELECT 1 FROM r80_probe_label l WHERE l.forecast_hash=p.forecast_hash)" : "";
            clauses.add("EXISTS(SELECT 1 FROM r80_probe_forecast p WHERE p.target_issue=" + issue + unresolved + ")");
        }
        String[][] pendingLabels = {
            {"atomic_tool_context_r943", "hit=-1"},
            {"v57_algorithm_predraw_r908", "hit_label=-1"},
            {"v57_research_predraw_r817", "hit_label=-1"},
            {"research_new_asset_freeze_v41", "hit_label=-1"},
            {"research_fixed_pair_r932", "(candidate_hit<0 OR champion_hit<0)"},
            {"research_candidate_cal_observation_r932", "hit=-1"},
            {"research_challenge_prediction_r932", "hit=-1"}
        };
        for (String[] pending : pendingLabels) if (installed.contains(pending[0]))
            clauses.add("EXISTS(SELECT 1 FROM " + pending[0] + " p WHERE p.target_issue="
                    + issue + " AND " + pending[1] + ")");
        // A settled CAL label still needs its original observation clock until the
        // fixed fit has authenticated all 241 inputs. Do not release that input
        // merely because settlement was faster than the research fitting worker.
        if(installed.contains(CandidateCalibrationR932.OBS)&&installed.contains(CandidateCalibrationR932.STUDY))
            clauses.add("EXISTS(SELECT 1 FROM "+CandidateCalibrationR932.OBS+" p JOIN "+CandidateCalibrationR932.STUDY
                    +" s ON s.study_hash=p.study_hash WHERE p.target_issue="+issue
                    +" AND p.hit IN(0,1) AND s.state IN('COLLECTING','READY'))");
        clauses.add("EXISTS(SELECT 1 FROM " + DbHelper.TABLE_R36_SETTLEMENT_TASK + " t WHERE t.target_issue=" + issue
                + " AND t.status NOT IN('COMPLETED','FAILED_TERMINAL','NO_PREDRAW_CANONICAL_TERMINAL','INVALID_DRAW_TERMINAL'))");
        clauses.add("EXISTS(SELECT 1 FROM " + DbHelper.TABLE_R35_SHADOW_OBSERVATION + " s WHERE s.target_issue=" + issue
                + " AND s.pre_draw_frozen=1 AND s.settled=0)");
        clauses.add("EXISTS(SELECT 1 FROM " + DbHelper.TABLE_TRADES + " t WHERE t.target_issue=" + issue + " AND t.settled=0)");
        clauses.add("EXISTS(SELECT 1 FROM " + DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB + " j WHERE j.target_issue=" + issue
                + " AND j.status NOT IN('COMPLETED','FAILED_TERMINAL'))");
        return new Reference(clauses);
    }

    private static String inputHash(Cursor row) {
        StringBuilder framed = new StringBuilder("CP12_DRAW_DEPENDENCY_INPUT_V1");
        for (int i = 0; i < 20; i++) {
            String value = row.isNull(i) ? null : row.getString(i);
            framed.append('|');
            if (value == null) framed.append("-1:");
            else framed.append(value.length()).append(':').append(value);
        }
        return V51ChecksumEngine.sha256(framed.toString());
    }
    /** Narrow indexed read for calibration's prospective-time revalidation.
     * The immutable input hash includes source timestamps as well as the outcome;
     * a positive open_time alone cannot authenticate an evicted draw.
     */
    static long verifiedOpenTimeR77(SQLiteDatabase db,String issue) {
        try(Cursor row=db.rawQuery("SELECT "+COLUMNS+",input_hash FROM "+TABLE+" WHERE issue=?",new String[]{issue})){
            if(!row.moveToFirst())return 0L;
            Models.Draw draw=new Models.Draw();draw.issue=row.getString(1);draw.openTime=row.getLong(2);
            for(int i=0;i<10;i++)draw.numbers[i]=row.getInt(5+i);
            int version=row.getInt(18);String hash=row.getString(19),input=row.getString(20);
            if(!issue.equals(draw.issue)||!TargetIssueResolver.isValid(draw.issue)||!draw.isValid()
                    ||row.getInt(15)!=0||draw.openTime<=0L||input==null||!input.matches("[0-9a-f]{64}")
                    ||!input.equals(inputHash(row))||(version!=1&&version!=DrawHashR36.CURRENT_VERSION)
                    ||((version>=DrawHashR36.CURRENT_VERSION||(hash!=null&&!hash.isEmpty()))
                        &&!DrawHashR36.sha256V2(draw).equals(hash)))
                throw new IllegalStateException("R77_CAL_RETAINED_INPUT_INVALID");
            return draw.openTime;
        }
    }
    private static void requireWriter(SQLiteDatabase db) {
        if (db == null || !db.inTransaction()) throw new IllegalStateException("CP12_DRAW_DEPENDENCY_WRITER");
    }
}
