package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import java.nio.file.*;
import java.util.*;

/** Actual report merge SQL over two disposable copies. Native Android backup is tested separately on-device. */
public final class ReportSnapshotR29HostTest {
    static int checks;
    static void check(String name,boolean ok){System.out.println("R29_REPORT|"+name+"|"+(ok?"OK":"FAILED"));if(!ok)throw new AssertionError(name);checks++;}
    static long n(SQLiteDatabase db,String sql){return EventBridgeR29HostTest.count(db,sql);}
    static SQLiteDatabase fixture()throws Exception{
        SQLiteDatabase db=CutoverR29HostTest.open();EventBridgeR29HostTest.draw(db,"202610300001");
        db.execSQL(RuntimeSchemaR26.FEEDBACK);
        db.execSQL("INSERT INTO r26_task(task_id,lane,status) VALUES('old-R27','R27','READY'),('legacy','P2','READY')");
        db.execSQL("INSERT INTO r26_evolution(attempt_id,policy_hash) VALUES('R27:OLD:1','OLD'),('R26:LEGACY:1','L')");
        db.execSQL("CREATE TABLE r29_typed_report(id INTEGER PRIMARY KEY,value BLOB,n TEXT)");
        db.execSQL("CREATE TABLE su2_campaign(a TEXT NOT NULL,b INTEGER NOT NULL,payload BLOB,PRIMARY KEY(a,b)) WITHOUT ROWID");
        for(int i=0;i<300;i++)db.execSQL("INSERT INTO su2_campaign VALUES(?,?,?)",new Object[]{"fixed",i,new byte[]{(byte)i}});
        db.execSQL("INSERT INTO r26_feedback VALUES('terminal',1,'root-authority','root-payload',1)");
        db.execSQL("CREATE TABLE app_state(state_key TEXT PRIMARY KEY,state_value TEXT,updated_at INTEGER)");
        db.execSQL("INSERT INTO app_state VALUES('user_research_enabled','0',1),('r27_legacy_research_enabled','0',1),('r48_draw_ingest_pending_issue','root',1),('r941_continuity_lane_22','root',1),('r962_research_next_lane','0',1),('r975_director_priority','TRAIN',1)");
        db.execSQL("INSERT INTO r29_typed_report VALUES(-5,?,NULL)",new Object[]{new byte[]{0,1,(byte)255}});return db;
    }
    static SQLiteDatabase merged(Path research)throws Exception{
        SQLiteDatabase db=fixture();db.execSQL("INSERT INTO r29_database_route VALUES(1,'ACTIVE',?,'schema','root',42,1)",new Object[]{ResearchRouteR29.FILE_NAME});
        db.execSQL("ATTACH DATABASE ? AS research",new Object[]{research.toString()});return db;
    }
    public static void main(String[]args)throws Exception{
        Path directory=Files.createTempDirectory("r29-report-host"),research=directory.resolve("research-copy.db");
        try(SQLiteDatabase rs=fixture()){
            rs.execSQL(ResearchRouteR29.OWNER_DDL);rs.execSQL("INSERT INTO r29_research_owner VALUES(1,'READY','schema','root',42,1)");
            rs.execSQL("UPDATE r26_task SET task_id='new-R27',cursor=7 WHERE lane='R27'");
            rs.execSQL("UPDATE r26_task SET cursor=99 WHERE task_id='legacy'");
            rs.execSQL("UPDATE r26_feedback SET source_hash='lagging-research-copy'");
            rs.execSQL("UPDATE su2_campaign SET payload=x'FF' WHERE b=299");
            rs.execSQL("UPDATE app_state SET state_value='stale-research-setting' WHERE state_key IN('user_research_enabled','r27_legacy_research_enabled','r48_draw_ingest_pending_issue','r941_continuity_lane_22')");
            rs.execSQL("UPDATE app_state SET state_value='8' WHERE state_key='r962_research_next_lane'");rs.execSQL("UPDATE app_state SET state_value='POST' WHERE state_key='r975_director_priority'");rs.execSQL("INSERT INTO app_state VALUES('r941_continuity_lane_21','REVIEW_ONLY',2)");
            rs.execSQL("DELETE FROM r26_evolution WHERE attempt_id LIKE 'R27:%'");rs.execSQL("INSERT INTO r26_evolution(attempt_id,policy_hash) VALUES('R27:NEW:2','NEW')");
            rs.execSQL("UPDATE r29_typed_report SET value=?",new Object[]{new byte[]{9,8,7}});
            rs.execSQL("VACUUM INTO ?",new Object[]{research.toString()});
        }
        String before=StatsR27.sha256(Files.readAllBytes(research));
        try{
            try(SQLiteDatabase db=merged(research)){
                Map<String,Object> frame=new LinkedHashMap<>();ResearchReportSnapshotR29.mergeAttachedCopies(db,frame,()->true);
                check("R1_MERGED_REPORT_CONTAINS_AUTHORITATIVE_RESEARCH_TASK",n(db,"SELECT cursor FROM r26_task WHERE task_id='new-R27'")==7&&n(db,"SELECT count(*) FROM r26_task WHERE task_id='old-R27'")==0);
                check("R2_LEGACY_TASK_AND_EVOLUTION_ARE_PRESERVED",n(db,"SELECT count(*) FROM r26_task WHERE task_id='legacy'")==1&&n(db,"SELECT count(*) FROM r26_evolution WHERE attempt_id='R26:LEGACY:1'")==1&&n(db,"SELECT count(*) FROM r26_evolution WHERE attempt_id='R27:NEW:2'")==1);
                check("R3_NATIVE_CATALOG_SEES_SAME_TYPED_ROWS",n(db,"SELECT count(*) FROM main.r29_typed_report WHERE id=-5 AND value=x'090807' AND n IS NULL")==1);
                check("R4_READER_BINDING_AND_FRAME_ARE_DURABLE",ResearchRouteR29.isResearchDatabase(db)&&n(db,"SELECT count(*) FROM r29_report_frame")==1&&((Number)frame.get("epoch")).longValue()==42);
                check("R5_REALTIME_RESULTS_ARE_PRESERVED",n(db,"SELECT count(*) FROM draws")==1);
                check("R10_WITHOUT_ROWID_COMPOSITE_KEYS_COPY_ALL_PAGES",n(db,"SELECT count(*) FROM su2_campaign")==300&&n(db,"SELECT count(*) FROM su2_campaign WHERE b=299 AND payload=x'FF'")==1);
                check("R11_LEGACY_RESEARCH_CURSOR_COMES_FROM_RESEARCH_OWNER",n(db,"SELECT cursor FROM r26_task WHERE task_id='legacy'")==99);
                check("R12_REALTIME_TERMINAL_AUTHORITY_IS_NOT_REPLACED_BY_MIRROR",n(db,"SELECT count(*) FROM r26_feedback WHERE source_hash='root-authority'")==1);
                check("R14_REPORT_USES_CURRENT_RESEARCH_LANE_AND_DIRECTOR_CURSORS",n(db,"SELECT count(*) FROM app_state WHERE state_key='r962_research_next_lane' AND state_value='8'")==1&&n(db,"SELECT count(*) FROM app_state WHERE state_key='r975_director_priority' AND state_value='POST'")==1&&n(db,"SELECT count(*) FROM app_state WHERE state_key='r941_continuity_lane_21' AND state_value='REVIEW_ONLY'")==1);
                check("R15_STALE_RESEARCH_APP_STATE_CANNOT_REPLACE_REALTIME_SETTINGS",n(db,"SELECT count(*) FROM app_state WHERE state_key IN('user_research_enabled','r27_legacy_research_enabled') AND state_value='0'")==2&&n(db,"SELECT count(*) FROM app_state WHERE state_key IN('r48_draw_ingest_pending_issue','r941_continuity_lane_22') AND state_value='root'")==2);
            }
            check("R6_RESEARCH_COPY_NOT_MODIFIED_BY_REPORT",before.equals(StatsR27.sha256(Files.readAllBytes(research))));
            try(SQLiteDatabase db=merged(research)){
                db.execSQL("CREATE TABLE research.r26_live_model(policy_hash TEXT PRIMARY KEY,through_ordinal INTEGER)");
                db.execSQL("INSERT INTO research.r26_live_model VALUES('future-source',?)",new Object[]{com.ethk3.novaonepro.TargetIssueResolver.sequenceOrdinalSU1("202610300001")+1});
                boolean rejected=false;try{ResearchReportSnapshotR29.mergeAttachedCopies(db,new LinkedHashMap<>(),()->true);}catch(java.io.IOException expected){rejected=expected.getMessage().equals("REPORT_RESEARCH_FRAME_RETRY");}
                check("R13_MIGRATED_MODEL_NEWER_THAN_REALTIME_COPY_REQUIRES_NEW_FRAME",rejected&&n(db,"SELECT count(*) FROM main.sqlite_master WHERE name='r29_report_frame'")==0);
                db.execSQL("DROP TABLE research.r26_live_model");
            }
            try(SQLiteDatabase db=merged(research)){
                int[] calls={0};boolean rejected=false;try{ResearchReportSnapshotR29.mergeAttachedCopies(db,new LinkedHashMap<>(),()->++calls[0]<8);}catch(java.io.IOException expected){rejected=true;}
                check("R7_CANCEL_DURING_MERGE_ROLLS_BACK_SNAPSHOT",rejected&&n(db,"SELECT count(*) FROM r26_task WHERE task_id='old-R27'")==1&&n(db,"SELECT count(*) FROM main.sqlite_master WHERE name='r29_report_frame'")==0);
            }
            try(SQLiteDatabase db=merged(research)){
                db.execSQL("UPDATE research.r29_research_owner SET epoch=43");boolean rejected=false;
                try{ResearchReportSnapshotR29.mergeAttachedCopies(db,new LinkedHashMap<>(),()->true);}catch(java.io.IOException expected){rejected=true;}
                check("R8_EPOCH_MISMATCH_NEVER_PUBLISHES_MIXED_REPORT",rejected&&n(db,"SELECT count(*) FROM main.sqlite_master WHERE name='r29_report_frame'")==0);
                db.execSQL("UPDATE research.r29_research_owner SET epoch=42");db.execSQL("INSERT INTO research.r29_event_cursor VALUES('REALTIME',99,1)");
                rejected=false;try{ResearchReportSnapshotR29.mergeAttachedCopies(db,new LinkedHashMap<>(),()->true);}catch(java.io.IOException expected){rejected=true;}
                check("R9_RESEARCH_AHEAD_OF_REALTIME_SNAPSHOT_RETRIES",rejected&&n(db,"SELECT count(*) FROM main.sqlite_master WHERE name='r29_report_frame'")==0);
            }
        }finally{Files.deleteIfExists(research);Files.deleteIfExists(directory);}
        System.out.println("R29_REPORT|ALL_PASS|checks="+checks+"|HOST_SQL_ONLY_ANDROID_NATIVE_SNAPSHOT_PENDING");
    }
}
