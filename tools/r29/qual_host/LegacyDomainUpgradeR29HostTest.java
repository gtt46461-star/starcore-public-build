package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.sql.DriverManager;
import java.util.*;
import java.util.concurrent.atomic.*;

/** Real SQLite two-domain upgrade, with rollback and retained RP state. Synthetic engineering only. */
public final class LegacyDomainUpgradeR29HostTest {
    static int checks;static void check(String name,boolean ok){System.out.println("R29_DOMAIN_UPGRADE|"+name+"|"+(ok?"OK":"FAIL"));if(!ok)throw new AssertionError(name);checks++;}
    static SQLiteDatabase open()throws Exception{return new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite::memory:"));}
    static long n(SQLiteDatabase db,String query){try(Cursor c=db.rawQuery(query,null)){return c.moveToFirst()?c.getLong(0):0;}}
    static LegacyDomainUpgradeR29.Check unlimited(){return new LegacyDomainUpgradeR29.Check(()->0,Long.MAX_VALUE,()->false);}
    static void source(SQLiteDatabase db){
        db.execSQL("CREATE TABLE r26_task(task_id TEXT PRIMARY KEY,lane TEXT,cursor INTEGER,state BLOB)");db.execSQL("INSERT INTO r26_task VALUES('old-model','P2',739,X'0001FF'),('old-r27','R27',10,X'10')");
        db.execSQL("CREATE TABLE r26_evolution(attempt_id TEXT PRIMARY KEY,value INTEGER)");db.execSQL("INSERT INTO r26_evolution VALUES('LEGACY:1',41),('R27:stale-root',3)");
        db.execSQL("CREATE TABLE r26_runtime(k TEXT PRIMARY KEY,n INTEGER,v REAL,b BLOB,s TEXT)");db.execSQL("INSERT INTO r26_runtime VALUES('complete-state',501,1.25,X'0000FF','original state'),('nullable',NULL,NULL,NULL,NULL)");
        db.execSQL("CREATE TABLE su2_tape(a TEXT,b INTEGER,v BLOB,PRIMARY KEY(a,b)) WITHOUT ROWID");db.execSQL("INSERT INTO su2_tape VALUES('x',7,X'00'),('y',-9,X'FF')");
        db.execSQL("CREATE TABLE research_bag(v TEXT)");db.execSQL("INSERT INTO research_bag VALUES('duplicate'),('duplicate'),(NULL)");
        db.execSQL("CREATE TABLE r26_log(id INTEGER PRIMARY KEY AUTOINCREMENT,value TEXT)");db.execSQL("INSERT INTO r26_log VALUES(8,'kept'),(99,'deleted ID remains reserved')");db.execSQL("DELETE FROM r26_log WHERE id=99");
        db.execSQL("CREATE TABLE r27_legacy_roster_alias(checkpoint_hash TEXT PRIMARY KEY,minted_roster TEXT)");db.execSQL("INSERT INTO r27_legacy_roster_alias VALUES('original-checkpoint','original-roster')");
        db.execSQL("CREATE TABLE r26_feedback(target INTEGER PRIMARY KEY,value TEXT)");db.execSQL("INSERT INTO r26_feedback VALUES(1,'REALTIME_AUTHORITY')");
        db.execSQL("CREATE TABLE r27_policy(k TEXT PRIMARY KEY,v TEXT)");db.execSQL("INSERT INTO r27_policy VALUES('stale-root-policy','old')");
        db.execSQL("CREATE INDEX original_runtime_value ON r26_runtime(n)");
    }
    static void oldResearch(SQLiteDatabase db){
        db.execSQL(ResearchRouteR29.OWNER_DDL);
        db.execSQL("INSERT INTO r29_research_owner VALUES(1,'READY',? ,?,17,1)",new Object[]{StatsR27.sha256("original-CP07-schema"),StatsR27.sha256("original-CP07-source")});
        db.execSQL("CREATE TABLE r26_task(task_id TEXT PRIMARY KEY,lane TEXT,cursor INTEGER,state BLOB)");db.execSQL("INSERT INTO r26_task VALUES('rp-r27','R27',9999,X'7777')");
        db.execSQL("CREATE TABLE r26_evolution(attempt_id TEXT PRIMARY KEY,value INTEGER)");db.execSQL("INSERT INTO r26_evolution VALUES('R27:advanced',888)");
        db.execSQL("CREATE TABLE r27_policy(k TEXT PRIMARY KEY,v TEXT)");db.execSQL("INSERT INTO r27_policy VALUES('rp-learning','advanced')");
    }
    static void apply(SQLiteDatabase target,LegacyDomainUpgradeR29.Snapshot capture,LegacyDomainUpgradeR29.Check check,LegacyDomainUpgradeR29.Fault fault){target.beginTransactionNonExclusive();try{LegacyDomainUpgradeR29.mergeWithin(target,capture,17,check,fault);target.setTransactionSuccessful();}finally{target.endTransaction();}}
    static boolean fails(Runnable work,String reason){try{work.run();return false;}catch(RuntimeException expected){return String.valueOf(expected.getMessage()).contains(reason);}}
    public static void main(String[] args)throws Exception{
        try(SQLiteDatabase root=open();SQLiteDatabase target=open()){
            source(root);oldResearch(target);LegacyDomainUpgradeR29.Snapshot capture=LegacyDomainUpgradeR29.capture(root,unlimited(),1L<<20);
            check("U1_CAPTURE_OMITS_ROOT_REALTIME_AND_R27_AUTHORITIES",capture.tables.stream().noneMatch(s->s.table.name.equals("r26_feedback")||s.table.name.equals("r27_policy")));
            check("U2_KILLED_EXPANSION_ROLLS_BACK_DATA_AND_MARKER",fails(()->apply(target,capture,unlimited(),name->{if(name.equals("r26_runtime"))throw new IllegalStateException("INJECTED_PROCESS_KILL");}),"INJECTED_PROCESS_KILL")&&!LegacyDomainUpgradeR29.table(target,"r29_legacy_domain_owner")&&!LegacyDomainUpgradeR29.table(target,"r26_runtime")&&n(target,"SELECT cursor FROM r26_task WHERE task_id='rp-r27'")==9999);
            apply(target,capture,unlimited(),name->{});
            check("U3_FULL_MARKER_EXISTS_ONLY_AFTER_TYPED_VERIFICATION",n(target,"SELECT count(*) FROM r29_legacy_domain_owner WHERE phase='FULL' AND route_epoch=17")==1);
            check("U4_R27_POLICY_AND_TASK_PROGRESS_ARE_PRESERVED",n(target,"SELECT cursor FROM r26_task WHERE task_id='rp-r27'")==9999&&n(target,"SELECT value FROM r26_evolution WHERE attempt_id='R27:advanced'")==888&&n(target,"SELECT count(*) FROM r27_policy WHERE k='rp-learning'")==1&&n(target,"SELECT count(*) FROM r27_policy WHERE k='stale-root-policy'")==0);
            check("U5_ROOT_STALE_R27_PARTITIONS_ARE_NOT_COPIED",n(target,"SELECT count(*) FROM r26_task WHERE task_id='old-r27'")==0&&n(target,"SELECT count(*) FROM r26_evolution WHERE attempt_id='R27:stale-root'")==0);
            check("U6_COMPLETE_NUMERICAL_STATE_AND_TYPED_VALUES_SURVIVE",n(target,"SELECT n FROM r26_runtime WHERE k='complete-state'")==501&&n(target,"SELECT count(*) FROM r26_runtime WHERE k='nullable' AND n IS NULL AND v IS NULL AND b IS NULL AND s IS NULL")==1&&n(target,"SELECT count(*) FROM r26_runtime WHERE b=X'0000FF' AND v=1.25")==1);
            check("U7_IMPLICIT_ROWID_COLLISIONS_DO_NOT_OVERWRITE_R27",n(target,"SELECT cursor FROM r26_task WHERE task_id='old-model'")==739&&n(target,"SELECT count(*) FROM r26_task")==2);
            check("U8_WITHOUT_ROWID_AND_DUPLICATE_BAGS_COPY_EXACTLY",n(target,"SELECT count(*) FROM su2_tape")==2&&n(target,"SELECT count(*) FROM research_bag WHERE v='duplicate'")==2&&n(target,"SELECT count(*) FROM research_bag WHERE v IS NULL")==1);
            check("U9_ORIGINAL_SOURCE_AND_CONTROL_ALIAS_REMAIN",n(root,"SELECT cursor FROM r26_task WHERE task_id='old-model'")==739&&n(target,"SELECT count(*) FROM r27_legacy_roster_alias WHERE minted_roster='original-roster'")==1);
            check("U10_AUTOINCREMENT_HIGH_WATERMARK_IS_RETAINED",n(target,"SELECT seq FROM sqlite_sequence WHERE name='r26_log'")==99&&n(target,"SELECT count(*) FROM sqlite_master WHERE name='original_runtime_value'")==1);
            apply(target,capture,unlimited(),name->{});check("U11_REPEATED_SAME_UPGRADE_CANNOT_DUPLICATE_STATE",n(target,"SELECT count(*) FROM research_bag")==3&&n(target,"SELECT count(*) FROM r29_legacy_domain_owner")==1);
            check("U12_CAPTURE_STORAGE_LIMIT_FAILS_WITHOUT_A_MARKER",fails(()->LegacyDomainUpgradeR29.capture(root,unlimited(),1),"STORAGE_BUDGET"));
        }
        try(SQLiteDatabase root=open();SQLiteDatabase target=open()){
            source(root);oldResearch(target);target.execSQL("INSERT INTO r26_task VALUES('old-model','P2',999999,X'99')");LegacyDomainUpgradeR29.Snapshot capture=LegacyDomainUpgradeR29.capture(root,unlimited(),1L<<20);
            check("U13_DIFFERENT_EXISTING_CHECKPOINT_FAILS_CLOSED",fails(()->apply(target,capture,unlimited(),name->{}),"EXISTING_STATE_CONFLICT")&&!LegacyDomainUpgradeR29.table(target,"r29_legacy_domain_owner")&&n(target,"SELECT cursor FROM r26_task WHERE task_id='old-model'")==999999);
        }
        try(SQLiteDatabase root=open();SQLiteDatabase target=open()){
            source(root);oldResearch(target);LegacyDomainUpgradeR29.Snapshot capture=LegacyDomainUpgradeR29.capture(root,unlimited(),1L<<20);AtomicBoolean cancel=new AtomicBoolean();
            check("U14_CANCEL_DURING_EXPANSION_ROLLS_BACK_ALL_TABLES",fails(()->apply(target,capture,new LegacyDomainUpgradeR29.Check(()->0,Long.MAX_VALUE,cancel::get),name->cancel.set(true)),"CANCELLED")&&!LegacyDomainUpgradeR29.table(target,"r29_legacy_domain_owner")&&!LegacyDomainUpgradeR29.table(target,"r26_runtime"));
            AtomicLong clock=new AtomicLong();check("U15_EXPIRED_TRANSACTION_BUDGET_ROLLS_BACK_ALL_TABLES",fails(()->apply(target,capture,new LegacyDomainUpgradeR29.Check(clock::get,1,()->false),name->clock.set(2)),"BUDGET")&&!LegacyDomainUpgradeR29.table(target,"r29_legacy_domain_owner"));
        }
        try(SQLiteDatabase root=open();SQLiteDatabase target=open()){
            source(root);oldResearch(target);target.execSQL("ALTER TABLE r26_task ADD COLUMN unreviewed TEXT");LegacyDomainUpgradeR29.Snapshot capture=LegacyDomainUpgradeR29.capture(root,unlimited(),1L<<20);
            check("U16_SCHEMA_DIVERGENCE_DOES_NOT_RESET_MODELS",fails(()->apply(target,capture,unlimited(),name->{}),"SCHEMA_CONFLICT")&&!LegacyDomainUpgradeR29.table(target,"r29_legacy_domain_owner")&&n(target,"SELECT cursor FROM r26_task WHERE task_id='rp-r27'")==9999);
        }
        try(SQLiteDatabase root=open()){
            source(root);check("U17_CHANGED_SOURCE_BETWEEN_CAPTURE_AND_VERIFICATION_IS_REJECTED",fails(()->LegacyDomainUpgradeR29.capture(root,unlimited(),1L<<20,()->root.execSQL("UPDATE r26_runtime SET n=999 WHERE k='complete-state'")),"SOURCE_CHANGED"));
            check("U18_CHANGED_AUTOINCREMENT_METADATA_IS_REJECTED",fails(()->LegacyDomainUpgradeR29.capture(root,unlimited(),1L<<20,()->root.execSQL("UPDATE sqlite_sequence SET seq=12345 WHERE name='r26_log'")),"SOURCE_METADATA_CHANGED"));
        }
        System.out.println("R29_DOMAIN_UPGRADE|ALL_PASS|checks="+checks+"|HOST_SQL_ONLY_ANDROID_UPGRADE_PENDING");
    }
}
