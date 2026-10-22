package com.ethk3.novaonepro;

import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.nio.file.*;

/** Runs production queries and renderers against the real SQLite bridge. No copied policy SQL. */
public final class R927AuditSqlHarness {
    static void check(boolean ok, String name) {
        if (!ok) throw new AssertionError(name);
        System.out.println(name + "=PASS");
    }
    public static void main(String[] args) throws Exception {
        SQLiteDatabase db = new SQLiteDatabase(args[0]);
        DbStore store = new DbStore(db);
        try {
            for (String line : Files.readAllLines(Paths.get(args[1]))) if (!line.isEmpty()) db.execSQL(line);
            RuntimeObservabilityR925.timing(db,"cold",100000,50,1,30,10,"cold",10,30,110);
            check(RuntimeObservabilityR925.query(store,"SELECT total_ms FROM v57_prediction_timing_r925 WHERE target_issue='cold'",false).contains("\n100\n"),"WALL_CLOCK_JUMP_MONOTONIC_100MS");
            check(!"COMPLETE_120".equals(RuntimeAttemptObservabilityR926.acceptanceWindow(store)),"SHORT_WINDOW_BLOCKED");
            for(int i=0;i<120;i++) {
                String id="a"+i;
                db.execSQL("INSERT INTO v57_runtime_event_r926(attempt_id,game_code,event_type,created_at) VALUES(?,?,?,?)",new Object[]{id,ReleaseManifestR926.GAME_CODE,"REQUESTED",200+i});
                db.execSQL("INSERT INTO v57_runtime_event_r926(attempt_id,game_code,event_type,created_at) VALUES(?,?,?,?)",new Object[]{id,ReleaseManifestR926.GAME_CODE,"CANONICAL",400+i});
                RuntimeObservabilityR925.timing(db,id,10000,50,1,30,10,id,10,30,110);
                db.execSQL("INSERT INTO v57_attempt_resource_r927(execution_id,attempt_id,target_issue,process_id,release_code,started_at,status,budget_hash) VALUES(?,?,?,?,?,?,?,?)",new Object[]{id,id,id,"test",BuildConfig.VERSION_CODE,200+i,"CANONICAL_COMMITTED","budget"});
            }
            check("COMPLETE_120".equals(RuntimeAttemptObservabilityR926.acceptanceWindow(store)),"120_COMPLETE_WARM_ATTEMPTS");
            db.execSQL("INSERT INTO v57_runtime_event_r926(attempt_id,game_code,event_type,created_at) VALUES('a119','Binance1-PK10','REJECTED',900)");
            check(!"COMPLETE_120".equals(RuntimeAttemptObservabilityR926.acceptanceWindow(store)),"FAILED_ATTEMPT_NOT_HIDDEN_BY_SUCCESS");
            db.execSQL("DELETE FROM v57_runtime_event_r926 WHERE event_type='REJECTED'");
            db.execSQL("UPDATE v57_attempt_resource_r927 SET status='NOT_COMMITTED' WHERE attempt_id='a119'");
            check(!"COMPLETE_120".equals(RuntimeAttemptObservabilityR926.acceptanceWindow(store)),"FAILED_RESOURCE_EXECUTION_CANNOT_HIDE_BEHIND_CANONICAL");
            db.execSQL("UPDATE v57_attempt_resource_r927 SET status='CANONICAL_COMMITTED' WHERE attempt_id='a119'");
            db.execSQL("UPDATE v57_prediction_timing_r925 SET sample_class='REHYDRATED_UNMEASURED' WHERE attempt_id='a119'");
            check(!"COMPLETE_120".equals(RuntimeAttemptObservabilityR926.acceptanceWindow(store)),"RESTART_UNKNOWN_CLOCK_NOT_ZERO_LATENCY");
            Batch2ContractCoreR926.Continuity result=BacklogConvergenceR925.continuity(store,240,0);
            check(result.expected==2216 && result.holes==1 && result.firstHole.equals("202609090010"),"OLD_HOLE_BEYOND_240_NOT_HIDDEN");
            check(result.continuousWatermark.equals("202609090009"),"CONTINUOUS_WATERMARK_BEFORE_HOLE");
            db.execSQL("UPDATE v57_issue_projection_r926 SET full_seen=1");
            check(BacklogConvergenceR925.continuity(store,240,0).closure,"2216_BACKLOG_FULLY_ACCOUNTED");
            check(!BacklogConvergenceR925.continuity(store,240,1).closure,"TERMINAL_FAILURE_PREVENTS_FALSE_CLOSURE");
            String all=AlgorithmLifecycleReportR927.capture(store,true);
            check(all.contains("END_ALL_ALGORITHMS|rows=401|status=COMPLETE"),"401_ALGORITHMS_NOT_CAPPED_AT_320");
            check(all.contains("END_ALL_RESEARCH_METHODS|rows=1|status=COMPLETE") && all.contains("归档优质样例") && all.contains("ARCHIVED") && all.contains("0.5"),"ARCHIVED_METHOD_NAME_SCORE_REASON_VISIBLE");
            StringBuilder streamed=new StringBuilder();AlgorithmLifecycleReportR927.writeDetails(store,streamed);
            check(streamed.toString().contains("rows=401") && streamed.toString().contains("归档优质样例"),"STREAMED_DETAILS_INCLUDE_ALL_STATUSES");
            String raw=FullResearchInventoryR923.capture(store);
            check(!raw.contains("READ_ERROR") && raw.contains("status=OK") && raw.contains("v57_promotion_history_r925"),"FULL_RAW_INVENTORY_AND_PROMOTION_HISTORY");
            Appendable fail=new Appendable(){public Appendable append(CharSequence x)throws IOException{throw new IOException("disk full");}public Appendable append(CharSequence x,int a,int b)throws IOException{throw new IOException("disk full");}public Appendable append(char x)throws IOException{throw new IOException("disk full");}};
            boolean failed=false;try{FullResearchInventoryR923.writeTo(store,fail);}catch(IOException expected){failed=true;}
            check(failed,"WRITE_FAILURE_PROPAGATES_NO_FAKE_EXPORT");
            check("a\\tb\\nc\\\\d".equals(FullResearchInventoryR923.escape("a\tb\nc\\d")),"TSV_NEWLINE_TAB_BACKSLASH_LOSSLESS");
            System.out.println("R927_AUDIT_SQL_HARNESS_PASS");
        } finally { db.shutdown(); }
    }
}
