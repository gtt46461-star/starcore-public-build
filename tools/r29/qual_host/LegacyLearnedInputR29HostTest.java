package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.nio.file.*;
import java.sql.DriverManager;

/** Root historical learning truth remains Root-only. Real SQLite pages, guard migration and rollback. */
public final class LegacyLearnedInputR29HostTest {
    static int checks;static final String TINY="ResearchTinyDeepLearnerR909",FTRL="ResearchAdaptiveLearnerStoreR811",COMPACT=DbHelper.TABLE_R906_RESEARCH_COMPACT_LABEL;
    interface Body{void run()throws Exception;}
    static void tx(SQLiteDatabase db,Body body,boolean kill)throws Exception{db.jdbc.setAutoCommit(false);try{body.run();if(kill)throw new IllegalStateException("SIMULATED_KILL");db.jdbc.commit();}catch(Throwable failure){db.jdbc.rollback();throw failure;}finally{db.jdbc.setAutoCommit(true);}}
    static void check(String name,boolean ok){System.out.println("R29_LEARNED_INPUT|"+name+"|"+(ok?"OK":"FAILED"));if(!ok)throw new AssertionError(name);checks++;}
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    static boolean rejects(Body body)throws Exception{try{body.run();return false;}catch(RuntimeException expected){return true;}}
    static String issue(int n){return String.format(java.util.Locale.ROOT,"20261030%04d",n);}
    static String hash(String value){return StatsR27.sha256(value);}
    static SQLiteDatabase open(String path)throws Exception{return new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite:"+path));}
    static void source(SQLiteDatabase root){root.execSQL("CREATE TABLE v57_runtime_event_r926(seq INTEGER PRIMARY KEY,event_id TEXT UNIQUE,dedup_key TEXT,game_code TEXT,target_issue TEXT,epoch_id TEXT,attempt_id TEXT,event_type TEXT,stage TEXT,state_rank INTEGER,payload_text TEXT,payload_hash TEXT,source TEXT,created_at INTEGER)");}
    static void event(SQLiteDatabase root,int seq,int target,String source,String payload){root.execSQL("INSERT INTO v57_runtime_event_r926 VALUES(?,?,?,?,?,'epoch','attempt','LEARNED','LEARNED_EXACTLY_ONCE',90,?,?,?,?)",new Object[]{seq,hash("original event "+seq),"dedup:"+seq,ReleaseManifestR926.GAME_CODE,issue(target),payload,hash(payload),source,1000+seq});}
    static String oldGuard(String name,String operation){return "CREATE TRIGGER "+name+" BEFORE "+operation+" ON "+COMPACT+" WHEN OLD.asset_id='PRODUCTION_SELECTED' AND EXISTS(SELECT 1 FROM v57_runtime_event_r926 WHERE target_issue=OLD.target_issue AND source='"+TINY+"') BEGIN SELECT RAISE(ABORT,'OLD_ROOT_LEARNED_FREEZE'); END";}
    static void compact(SQLiteDatabase db){db.execSQL("CREATE TABLE "+COMPACT+"(asset_id TEXT,target_issue TEXT,feature_blob BLOB,hit_label INTEGER,note TEXT,PRIMARY KEY(asset_id,target_issue))");db.execSQL("INSERT INTO "+COMPACT+" VALUES('PRODUCTION_SELECTED',?,x'01',1,'old'),('PRODUCTION_SELECTED',?,x'01',1,'old')",new Object[]{issue(10),issue(400)});}
    public static void main(String[]args)throws Exception{
        Path directory=Files.createTempDirectory("r29-learned-input-"),file=directory.resolve("research.db");
        try(SQLiteDatabase root=open(":memory:");SQLiteDatabase research=open(file.toString())){
            source(root);compact(root);compact(research);research.execSQL("CREATE TABLE r29_research_owner(singleton INTEGER PRIMARY KEY)");
            String update=oldGuard("trg_r927_e02c_tiny_label_update_freeze","UPDATE"),delete=oldGuard("trg_r927_e02c_tiny_label_delete_freeze","DELETE");
            root.execSQL(update);root.execSQL(delete);research.execSQL(update);research.execSQL(delete);
            for(int i=1;i<=300;i++)event(root,i,i,i%2==0?TINY:FTRL,"original historical payload "+i);
            root.execSQL("INSERT INTO v57_runtime_event_r926 VALUES(550,?,'other',?,?, '', '', 'ACCEPTED','ACCEPTED',20,'other',?,'other',2000)",new Object[]{hash("other"),ReleaseManifestR926.GAME_CODE,issue(301),hash("other")});
            LegacyLearnedInputR29.installResearch(research);
            check("B1_COPIED_ROOT_TINY_GUARDS_ARE_REPLACED_AND_AUDITED",n(research,"SELECT count(*) FROM r29_legacy_guard_migration")==2&&n(research,"SELECT count(*) FROM sqlite_master WHERE name='v57_runtime_event_r926'")==0&&!LegacyLearnedInputR29.ready(research));
            check("B2_BOOTSTRAP_BLOCKS_PRODUCTION_INSERT_UPDATE_AND_DELETE",rejects(()->research.execSQL("INSERT INTO "+COMPACT+" VALUES('PRODUCTION_SELECTED',?,x'00',0,'new')",new Object[]{issue(401)}))&&rejects(()->research.execSQL("UPDATE "+COMPACT+" SET note='metadata' WHERE target_issue=?",new Object[]{issue(400)}))&&rejects(()->research.execSQL("DELETE FROM "+COMPACT+" WHERE target_issue=?",new Object[]{issue(400)})));
            research.execSQL("INSERT INTO "+COMPACT+" VALUES('OTHER_RESEARCH',?,x'00',0,'allowed')",new Object[]{issue(401)});
            LegacyLearnedInputR29.Capture first=LegacyLearnedInputR29.capture(root,research);
            check("B3_CAPTURE_IS_BOUNDED_AND_BOOTSTRAP_CANNOT_DECLARE_EMPTY",first.facts.size()==128&&!first.ready&&first.head==550&&first.previousSeq==0);
            try{tx(research,()->LegacyLearnedInputR29.apply(research,first),true);}catch(IllegalStateException expected){}
            check("B4_INTERRUPTED_FACT_AND_CURSOR_TRANSACTION_ROLLS_BACK",n(research,"SELECT count(*) FROM "+LegacyLearnedInputR29.TABLE)==0&&n(research,"SELECT source_seq FROM "+LegacyLearnedInputR29.STATE)==0&&!LegacyLearnedInputR29.ready(research));
            tx(research,()->LegacyLearnedInputR29.apply(research,first),false);
            check("B5_COMMITTED_PAGE_HAS_REAL_FACTS_BUT_IS_NOT_READY",n(research,"SELECT count(*) FROM "+LegacyLearnedInputR29.TABLE)==128&&!LegacyLearnedInputR29.ready(research)&&n(research,"SELECT source_seq FROM "+LegacyLearnedInputR29.STATE)==128);
            check("B6_STALE_CAPTURE_CANNOT_REWIND_A_PUBLISHED_CURSOR",rejects(()->tx(research,()->LegacyLearnedInputR29.apply(research,first),false)));
            int pages=1;while(!LegacyLearnedInputR29.ready(research)&&pages<10){LegacyLearnedInputR29.Capture page=LegacyLearnedInputR29.capture(root,research);tx(research,()->LegacyLearnedInputR29.apply(research,page),false);pages++;}
            check("B7_ALL_HISTORICAL_FACTS_SURVIVE_PAGING_AND_NONLEARNED_GAPS",pages==3&&n(research,"SELECT count(*) FROM "+LegacyLearnedInputR29.TABLE)==300&&n(research,"SELECT source_seq FROM "+LegacyLearnedInputR29.STATE)==550&&LegacyLearnedInputR29.ready(research));
            long schema=n(research,"PRAGMA schema_version"),changes=n(research,"SELECT total_changes()");LegacyLearnedInputR29.installResearch(research);LegacyLearnedInputR29.installResearch(research);
            check("B8_CURRENT_SCHEMA_FASTPATH_DOES_NOT_REPEAT_DDL_OR_DML",LegacyLearnedInputR29.capture(root,research).noChange&&schema==n(research,"PRAGMA schema_version")&&changes==n(research,"SELECT total_changes()"));
            check("B9_CAPTURED_ROOT_FACTS_AND_CURSOR_ARE_NOT_GENERAL_DML",rejects(()->research.execSQL("UPDATE "+LegacyLearnedInputR29.TABLE+" SET payload_hash='changed'"))&&rejects(()->research.execSQL("DELETE FROM "+LegacyLearnedInputR29.TABLE))&&rejects(()->research.execSQL("UPDATE "+LegacyLearnedInputR29.STATE+" SET ready=0")));
            check("B10_HISTORICALLY_LEARNED_TINY_INPUT_IS_FROZEN",rejects(()->research.execSQL("UPDATE "+COMPACT+" SET feature_blob=x'00' WHERE target_issue=?",new Object[]{issue(10)})));
            research.execSQL("UPDATE "+COMPACT+" SET feature_blob=x'09' WHERE target_issue=?",new Object[]{issue(400)});
            check("B11_UNLEARNED_INPUT_IS_NOT_FALSELY_FROZEN_AFTER_BOOTSTRAP",n(research,"SELECT count(*) FROM "+COMPACT+" WHERE target_issue='"+issue(400)+"' AND feature_blob=x'09'")==1);
            String identity=hash("local learning"),pre=hash("pre"),post=hash("post");String localIssue=issue(401);
            research.execSQL("INSERT INTO "+COMPACT+" VALUES('PRODUCTION_SELECTED',?,x'01',1,'local')",new Object[]{localIssue});
            tx(research,()->LegacyLearnedInputR29.recordLocalWithin(research,localIssue,TINY,identity,pre,post,localIssue,"detail"),false);
            research.execSQL("UPDATE "+COMPACT+" SET feature_blob=x'02' WHERE asset_id='PRODUCTION_SELECTED' AND target_issue=?",new Object[]{localIssue});
            check("B12_UNACKNOWLEDGED_LOCAL_MODEL_CANNOT_FREEZE_ROOT_INPUT_MIRROR",n(research,"SELECT root_acked_at FROM "+LegacyLearnedInputR29.LOCAL)==0);
            event(root,551,401,TINY,"learning_identity="+identity+"|pre_state_hash="+pre+"|post_state_hash="+post+"|trained_until="+localIssue+"|detail=verified");LegacyLearnedInputR29.Capture learned=LegacyLearnedInputR29.capture(root,research);tx(research,()->LegacyLearnedInputR29.apply(research,learned),false);
            check("B13_EXACT_ROOT_IDENTITY_ACKS_LOCAL_AND_FREEZES_INPUT",n(research,"SELECT root_acked_at FROM "+LegacyLearnedInputR29.LOCAL)>0&&rejects(()->research.execSQL("UPDATE "+COMPACT+" SET feature_blob=x'03' WHERE asset_id='PRODUCTION_SELECTED' AND target_issue=?",new Object[]{localIssue})));
            research.execSQL("DROP TRIGGER trg_r927_e02c_tiny_label_update_freeze");research.execSQL(update);LegacyLearnedInputR29.installResearch(research);
            check("B14_RESTORED_OLD_TRIGGER_IS_DETECTED_BY_SCHEMA_VERSION",rejects(()->research.execSQL("UPDATE "+COMPACT+" SET hit_label=0 WHERE target_issue=?",new Object[]{issue(10)}))&&n(research,"SELECT count(*) FROM sqlite_master WHERE type='table' AND name='v57_runtime_event_r926'")==0);
            try(Cursor c=root.rawQuery("SELECT sql FROM sqlite_master WHERE name='trg_r927_e02c_tiny_label_update_freeze'",null)){check("B15_ORIGINAL_ROOT_FREEZE_TRIGGER_IS_UNCHANGED",c.moveToFirst()&&update.equals(c.getString(0)));}
            event(root,552,401,TINY,"learning_identity="+identity+"|pre_state_hash="+pre+"|post_state_hash="+hash("conflicting post state")+"|trained_until="+localIssue+"|detail=conflict");LegacyLearnedInputR29.Capture conflict=LegacyLearnedInputR29.capture(root,research);
            check("B18_ROOT_FACT_CANNOT_ACK_A_DIFFERENT_LOCAL_MODEL_STATE",rejects(()->tx(research,()->LegacyLearnedInputR29.apply(research,conflict),false))&&n(research,"SELECT source_seq FROM "+LegacyLearnedInputR29.STATE)==551&&n(research,"SELECT count(*) FROM "+LegacyLearnedInputR29.TABLE)==301);
            event(root,553,402,FTRL,"valid bytes before lower-level corruption");root.execSQL("UPDATE v57_runtime_event_r926 SET payload_hash=? WHERE seq=553",new Object[]{hash("wrong bytes")});
            check("B19_BAD_ROOT_PAYLOAD_HASH_IS_NOT_MIRRORED_OR_ACKNOWLEDGED",rejects(()->LegacyLearnedInputR29.capture(root,research))&&n(research,"SELECT source_seq FROM "+LegacyLearnedInputR29.STATE)==551);
            LegacyLearnedInputR29.Fact f=first.facts.get(0);
            check("B20_DIRECT_FACT_INSERT_OUTSIDE_CAPTURE_IS_REJECTED",rejects(()->research.execSQL("INSERT INTO "+LegacyLearnedInputR29.TABLE+" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{99999,hash("forged"),f.dedup,f.game,f.issue,f.epoch,f.attempt,f.type,f.stage,f.rank,f.payload,f.payloadHash,f.source,f.createdAt,f.identityHash})));
        }
        try(SQLiteDatabase research=open(file.toString());SQLiteDatabase missing=open(":memory:")){
            check("B16_REOPEN_RETAINS_FACTS_READY_AND_CURSOR",LegacyLearnedInputR29.ready(research)&&n(research,"SELECT count(*) FROM "+LegacyLearnedInputR29.TABLE)==301&&n(research,"SELECT source_seq FROM "+LegacyLearnedInputR29.STATE)==551);
            check("B17_MISSING_ROOT_LEDGER_IS_NOT_AN_EMPTY_SUCCESS",rejects(()->LegacyLearnedInputR29.capture(missing,research)));
        }finally{Files.deleteIfExists(file);Files.deleteIfExists(directory);}
        System.out.println("R29_LEARNED_INPUT|ALL_PASS|checks="+checks+"|HOST_ONLY_ANDROID_BOOTSTRAP_PENDING");
    }
}
