package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;

/** Current production bytecode on real SQLite. Synthetic inputs stay in a disposable
 * copy; this contract never claims Android, natural Future or REAL acceptance. */
public final class DrawDependencyRollingCP12 {
    private static int checks;
    private static void check(boolean ok,String reason) {
        if(!ok)throw new AssertionError(reason);
        checks++;System.out.println("PASS "+reason);
    }
    private static long count(SQLiteDatabase db,String table) {
        try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM "+table,null)){c.moveToFirst();return c.getLong(0);}
    }
    private static String oldest(SQLiteDatabase db) {
        try(Cursor c=db.rawQuery("SELECT MIN(issue) FROM draws WHERE dirty=0",null)){c.moveToFirst();return c.getString(0);}
    }
    private static Models.Draw next(int i) {
        Models.Draw d=new Models.Draw();d.issue=String.format(java.util.Locale.ROOT,"20261011%04d",i);
        d.openTime=System.currentTimeMillis()-1000L;d.fetchTime=System.currentTimeMillis();
        d.sourceLine="SYNTHETIC_DEPENDENCY_ROLLING_CONTRACT";
        for(int n=0;n<10;n++)d.numbers[n]=(n+i)%10+1;
        return d;
    }
    private static String pin(SQLiteDatabase db,String issue,int i) {
        db.beginTransaction();try {
            String id=ResearchTaskQueueR967.enqueue(db,"POST_TRAIN","CONTRACT:"+i,issue,"contract","input","authority");
            ResearchTaskQueueR967.pin(db,id,"draws",issue);db.setTransactionSuccessful();return id;
        }finally{db.endTransaction();}
    }
    public static void main(String[] args)throws Exception {
        SQLiteDatabase db=new SQLiteDatabase(args[0],args[1]);
        try {
            DbStore store=HostProductR974.store(db);
            db.beginTransaction();try {DbHelper.ensureR815CriticalSchema(db);db.setTransactionSuccessful();}finally{db.endTransaction();}
            check(count(db,"draws")==5000,"REPORT_HISTORY_STARTS_AT_5000");
            List<String> tasks=new ArrayList<>(),issues=new ArrayList<>();
            long trades=count(db,"trades"),shadows=count(db,DbHelper.TABLE_R35_SHADOW_OBSERVATION);
            for(int i=1;i<=64;i++) {
                String issue=oldest(db);Models.Draw before=store.getDrawByIssue(issue);
                String task=pin(db,issue,i);tasks.add(task);issues.add(issue);
                long firstSeen;
                try(Cursor c=db.rawQuery("SELECT first_seen_at FROM draws WHERE issue=?",new String[]{issue})){c.moveToFirst();firstSeen=c.getLong(0);}
                check(store.saveDraw(next(i))>0,"PINNED_OLDEST_DOES_NOT_STOP_NEW_INGRESS_"+i);
                check(count(db,"draws")==5000&&!oldest(db).equals(issue),"EXACT_OLDEST_ROLLS_AT_CAP_"+i);
                Models.Draw retained=store.getDrawByIssue(issue);
                check(retained!=null&&DrawHashR36.sha256V2(before).equals(DrawHashR36.sha256V2(retained))
                        &&before.openTime==retained.openTime&&before.fetchTime==retained.fetchTime,
                        "EXACT_CONSUMER_READS_ORIGINAL_INPUT_"+i);
                try(Cursor c=db.rawQuery("SELECT first_seen_at,raw_json FROM "+DrawDependencyInputCP12.SOURCE+" WHERE issue=?",new String[]{issue})){
                    check(c.moveToFirst()&&c.getLong(0)==firstSeen&&"".equals(c.getString(1)),"ORIGINAL_FIRST_SEEN_WITHOUT_PROVIDER_PAYLOAD_"+i);
                }
            }
            check(count(db,DrawDependencyInputCP12.TABLE)==64,"ALL_64_INFLIGHT_INPUTS_RETAINED");
            long dependencies=count(db,DrawDependencyInputCP12.TABLE);
            check(store.saveDraw(next(64))==-1&&count(db,"draws")==5000
                    &&count(db,DrawDependencyInputCP12.TABLE)==dependencies,"DUPLICATE_DOES_NOT_EVICT_OR_RECOPY");
            boolean blocked=false;
            try{db.execSQL("UPDATE "+DrawDependencyInputCP12.TABLE+" SET n1=9 WHERE issue=?",new Object[]{issues.get(0)});}catch(RuntimeException expected){blocked=true;}
            check(blocked,"DEPENDENCY_INPUT_IS_IMMUTABLE");
            blocked=false;
            try{db.execSQL("DELETE FROM "+DrawDependencyInputCP12.TABLE+" WHERE issue=?",new Object[]{issues.get(0)});}catch(RuntimeException expected){blocked=true;}
            check(blocked,"UNFINISHED_CONSUMER_DELETE_IS_BLOCKED");
            String first=oldest(db);String failedTask=pin(db,first,65);
            db.execSQL("CREATE TRIGGER test_dependency_reject BEFORE INSERT ON draws WHEN NEW.issue='202610110065' BEGIN SELECT RAISE(ABORT,'TEST_INSERT_FAILURE'); END");
            blocked=false;try{store.saveDraw(next(65));}catch(RuntimeException expected){blocked=true;}
            check(blocked&&first.equals(oldest(db))&&count(db,"draws")==5000
                    &&count(db,DrawDependencyInputCP12.TABLE)==dependencies,"FAILED_INSERT_ROLLS_BACK_COPY_AND_EVICTION");
            db.execSQL("DROP TRIGGER test_dependency_reject");
            check(store.saveDraw(next(65))>0&&count(db,DrawDependencyInputCP12.TABLE)==65,"SAME_INPUT_RETRIES_AFTER_INSERT_FAILURE");
            for(String task:tasks)db.execSQL("UPDATE "+ResearchTaskQueueR967.TABLE+" SET status='COMPLETED' WHERE task_id=?",new Object[]{task});
            db.execSQL("UPDATE "+ResearchTaskQueueR967.TABLE+" SET status='COMPLETED' WHERE task_id=?",new Object[]{failedTask});
            for(int i=66;i<=74;i++)check(store.saveDraw(next(i))>0,"INGRESS_CONTINUES_WHILE_RELEASE_IS_BOUNDED_"+i);
            check(count(db,DrawDependencyInputCP12.TABLE)==0,"INPUTS_RELEASE_ONLY_AFTER_ALL_CONSUMERS_FINISH");
            String shared=oldest(db),sharedTask=pin(db,shared,76);
            // Synthetic pending label in the disposable fixture only. It checks that
            // completing one consumer cannot release another consumer's exact input.
            db.execSQL("INSERT INTO atomic_tool_context_r943(target_issue,base_issue,position,n1,n2,n3,features,probabilities,"
                    + "network_probability,ablation,network_hash,trained_until,record_hash)"
                    + " VALUES(?,?,1,1,2,3,X'00',X'00',.3,X'00','TEST','TEST','TEST')",new Object[]{shared,shared});
            check(store.saveDraw(next(75))>0&&count(db,DrawDependencyInputCP12.TABLE)==1,"MULTIPLE_CONSUMERS_SHARE_EXACT_EVICTED_INPUT");
            db.execSQL("UPDATE "+ResearchTaskQueueR967.TABLE+" SET status='COMPLETED' WHERE task_id=?",new Object[]{sharedTask});
            check(store.saveDraw(next(76))>0&&store.getDrawByIssue(shared)!=null
                    &&count(db,DrawDependencyInputCP12.TABLE)==1,"PENDING_ATOM_LABEL_SURVIVES_TASK_COMPLETION");
            db.execSQL("UPDATE atomic_tool_context_r943 SET hit=0,settled_at=1 WHERE target_issue=?",new Object[]{shared});
            check(store.saveDraw(next(77))>0&&count(db,DrawDependencyInputCP12.TABLE)==0,"FINAL_LABEL_COMPLETION_RELEASES_SHARED_INPUT");
            check(count(db,"trades")==trades&&count(db,DbHelper.TABLE_R35_SHADOW_OBSERVATION)==shadows,"TEST_CREATES_NO_REAL_OR_POSTDRAW_SHADOW");
            check(store.getRecentDraws(6000).size()==5000,"NORMAL_HISTORY_EXCLUDES_TASK_INPUTS");
            // Critical ingress must not depend on optional Research startup.
            for(String table:new String[]{"v57_research_input_pin_r967","v57_research_task_r967","r952_candidate_predraw","r958_frozen_predraw","r952_research_outbox"})
                db.execSQL("DROP TABLE "+table);
            check(store.saveDraw(next(78))>0&&count(db,"draws")==5000,"CRITICAL_ONLY_SCHEMA_CONTINUES_ROLLING");
            try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM app_state WHERE state_key='cp12_draw_dependency_release_issue'",null)) {
                check(c.moveToFirst()&&c.getLong(0)==0,"NO_RELEASE_PERMISSION_SURVIVES_TRANSACTION");
            }
            try(Cursor c=db.rawQuery("PRAGMA integrity_check",null)){check(c.moveToFirst()&&"ok".equals(c.getString(0)),"SQLITE_INTEGRITY_OK");}
            System.out.println("DRAW_DEPENDENCY_ROLLING_PASS checks="+checks+" scope=HOST_REAL_SQLITE_NOT_ANDROID_NOT_NATURAL");
        }finally{db.shutdown();}
    }
}
