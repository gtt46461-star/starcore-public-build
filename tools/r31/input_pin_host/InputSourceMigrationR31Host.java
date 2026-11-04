package com.ethk3.novaonepro;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.ethk3.novaonepro.strategyv2.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Actual Critical predecessor schema methods and source Domain migration, not placeholder objects. */
public final class InputSourceMigrationR31Host {
    static int assertions;static final List<Object> facts=new ArrayList<>();
    static void need(boolean ok,String why){assertions++;if(!ok)throw new AssertionError(why);}
    static long count(SQLiteDatabase db,String sql){return MainChainSchemaR31Host.count(db,sql);}
    static String value(SQLiteDatabase db,String sql){return MainChainSchemaR31Host.value(db,sql);}
    static String stamp(SQLiteDatabase db){return value(db,"SELECT fingerprint FROM "+SchemaDomainCoordinatorR926.TABLE_DOMAIN+" WHERE domain='CRITICAL'");}
    static String[] oldSql()throws Exception{Method m=ResearchSourceJournalR31.class.getDeclaredMethod("legacySql");m.setAccessible(true);return (String[])m.invoke(null);}
    static void sourceRevisionSchema(SQLiteDatabase db)throws Exception{Method m=SchemaR27.class.getDeclaredMethod("ensureLiveSourceRevision",SQLiteDatabase.class);m.setAccessible(true);m.invoke(null,db);}
    static void write(SQLiteDatabase db,Runnable body,boolean commit){db.beginTransaction();try{body.run();if(commit)db.setTransactionSuccessful();}finally{db.endTransaction();}}
    static final class FailingDb extends MainChainSchemaR31Host.Db {
        boolean fail;
        FailingDb(Connection c){super(c);}
        @Override public void execSQL(String sql,Object[] args){if(fail&&sql.contains("r30r1_pin_draws_update")&&sql.contains("INSERT INTO r31_source_event"))throw new android.database.sqlite.SQLiteException("INJECTED_SOURCE_TRIGGER_UPGRADE_FAILURE");super.execSQL(sql,args);}
    }
    static void criticalSteps(SQLiteDatabase db)throws Exception{
        SchemaDomainCoordinatorR926.Contract c=SchemaDomainCoordinatorR926.criticalContract();
        SchemaDomainCoordinatorR926.runStep(db,c,"F02_SETTLEMENT_RECOVERY_AUDIT_V1",SettlementTaskStoreR36::ensureRepairAuditSchema);
        SchemaDomainCoordinatorR926.runStep(db,c,"CP12_CRITICAL_CANONICAL_REASON_V1",com.ethk3.novaonepro.r952.CanonicalSchemaCP12::installReasons);
        SchemaDomainCoordinatorR926.runStep(db,c,"R35_SCHEMA",DbHelper::ensureR35Schema);
        SchemaDomainCoordinatorR926.runStep(db,c,"R36_SCHEMA",DbHelper::ensureR36Schema);
        SchemaDomainCoordinatorR926.runStep(db,c,"R811_CRITICAL_SCHEMA",DbHelper::ensureR811CriticalSchema);
        SchemaDomainCoordinatorR926.runStep(db,c,"R815_CRITICAL_SCHEMA",DbHelper::ensureR815CriticalSchema);
        SchemaDomainCoordinatorR926.runStep(db,c,"R906_CRITICAL_SCHEMA",DbHelper::ensureR906CriticalSchema);
        SchemaDomainCoordinatorR926.runStep(db,c,"V51_RUNTIME_SCHEMA",V51Repository::ensure);
        SchemaDomainCoordinatorR926.runStep(db,c,"B03_RUNTIME_SCHEMA",DbHelper::ensureTianshuB03);
        SchemaDomainCoordinatorR926.runStep(db,c,"R925_CRITICAL_SCHEMA",SchemaRevisionR925::ensureCriticalTables);
        SchemaDomainCoordinatorR926.runStep(db,c,"R926_EVENT_LEDGER_SCHEMA",RuntimeEventLedgerR926::ensureSchema);
        SchemaDomainCoordinatorR926.runStep(db,c,"R26_FEEDBACK_SCHEMA",d->d.execSQL(RuntimeSchemaR26.FEEDBACK));
        SchemaDomainCoordinatorR926.verifyObjects(db,c);
    }
    static MainChainSchemaR31Host.Owner legacy(Path path,boolean addition,boolean stamped)throws Exception{
        MainChainSchemaR31Host.Owner owner=new MainChainSchemaR31Host.Owner(path);MainChainSchemaR31Host.createLegacyBase(owner);
        FailingDb db=new FailingDb(owner.db.jdbc);MainChainSchemaR31Host.set(owner,"db",db);SQLiteOpenHelper.hostDatabase=db;
        db.execSQL("CREATE TABLE r27_history_import(issue TEXT PRIMARY KEY,ordinal INTEGER,numbers TEXT,status TEXT)");for(String ddl:ResearchSchemaR30R1.DDL)db.execSQL(ddl);
        db.beginTransaction();try{sourceRevisionSchema(db);criticalSteps(db);if(stamped&&addition)SchemaDomainCoordinatorR926.verifyAndStamp(db,SchemaDomainCoordinatorR926.criticalContract());for(String sql:oldSql())db.execSQL(sql);db.execSQL("CREATE TRIGGER IF NOT EXISTS r30r1_pin_immutable BEFORE UPDATE ON r30r1_analysis_run WHEN OLD.status='INPUT_DATASET_PINNED' BEGIN SELECT RAISE(ABORT,'RESEARCH_INPUT_PIN_IMMUTABLE'); END");if(stamped&&!addition)SchemaDomainCoordinatorR926.verifyAndStamp(db,SchemaDomainCoordinatorR926.criticalContract());db.setTransactionSuccessful();}finally{db.endTransaction();}
        need("OLD_EXACT".equals(ResearchSourceJournalR31.inspectUpgradeR31(db)),"exact legacy four triggers");
        // Real old invalidation evidence is written by the exact old production trigger, not seeded/reset.
        long ord=TargetIssueResolver.sequenceOrdinalSU1("202611040006");String id=StatsR27.sha256("legacy-migration-engineering-pin");
        db.execSQL("INSERT INTO draws(issue,open_time,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10) VALUES('202611040006',0,0,1,2,3,4,5,6,7,8,9,10)");
        db.execSQL("INSERT INTO r30r1_analysis_run VALUES(?,?,?,?,?,?,?,?,?,?,?,0)",new Object[]{id,id,id,"SELECT",id,1,ord,ord,id,ResearchInputPinR30R1.STATUS,"{}"});
        db.execSQL("UPDATE draws SET n1=2,n2=1 WHERE issue='202611040006'");need(count(db,"SELECT count(*) FROM r30r1_research_measure WHERE kind='INPUT_INVALIDATION'")==1,"legacy trigger produced actual invalidation");return owner;
    }
    static void upgrade(boolean addition)throws Exception{
        Path file=Files.createTempFile("r31-critical-source-",".sqlite");String after;
        try{MainChainSchemaR31Host.Owner owner=legacy(file,addition,true);SQLiteDatabase db=owner.db;String before=stamp(db);String prior=SchemaDomainCoordinatorR926.matchReason(db,SchemaDomainCoordinatorR926.criticalContract());need((addition?"MATCH_R30_PIN_DRAW_ADDITION":"MATCH").equals(prior),"old trusted Critical reason:"+prior);write(db,()->ResearchInputPinR30R1.ensure(db),true);need("R31_EXACT".equals(ResearchSourceJournalR31.inspectUpgradeR31(db)),"new exact trigger contract");need(SchemaDomainCoordinatorR926.matches(db,SchemaDomainCoordinatorR926.criticalContract()),"Critical verifies after migration");need(SchemaDomainCoordinatorR926.matches(db,SchemaDomainCoordinatorR926.r31SourceJournalContract()),"source Domain ready");after=stamp(db);need(!before.equals(after),"Critical stamp changed only declared trigger delta");need(count(db,"SELECT count(*) FROM r30r1_research_measure WHERE kind='INPUT_INVALIDATION'")==1,"old invalidation retained");need(count(db,"SELECT count(*) FROM r30r1_analysis_run WHERE status='INPUT_DATASET_PINNED'")==1,"old pin retained");need(count(db,"SELECT count(*) FROM "+SchemaDomainCoordinatorR926.TABLE_JOURNAL+" WHERE domain='CRITICAL' AND step_id='R31_SOURCE_DRAW_CUTOVER_V1' AND status='COMPLETED'")==1,"cutover journal committed atomically");MainChainSchemaR31Host.originalRows(owner);db.close();
            MainChainSchemaR31Host.Owner reopened=new MainChainSchemaR31Host.Owner(file);need(after.equals(stamp(reopened.db)),"same Critical stamp after physical reopen");SchemaDomainCoordinatorR926.verifyObjects(reopened.db,SchemaDomainCoordinatorR926.criticalContract());need(SchemaDomainCoordinatorR926.matches(reopened.db,SchemaDomainCoordinatorR926.criticalContract()),"physical reopen Critical matches");write(reopened.db,()->ResearchInputPinR30R1.ensure(reopened.db),true);need(after.equals(stamp(reopened.db)),"repeat migration preserves authenticated stamp");reopened.db.close();facts.add(Map.of("case",addition?"legacy_pin_addition_trusted_cutover":"already_stamped_old_pin_cutover","beforeReason",prior,"beforeCritical",before,"afterCritical",after,"physicalReopen",true));
        }finally{Files.deleteIfExists(file);}
    }
    static void rollbackAndTamper()throws Exception{
        Path file=Files.createTempFile("r31-critical-rollback-",".sqlite");try{MainChainSchemaR31Host.Owner owner=legacy(file,false,true);FailingDb db=(FailingDb)owner.db;String before=stamp(db);long journal=count(db,"SELECT count(*) FROM "+SchemaDomainCoordinatorR926.TABLE_JOURNAL);db.fail=true;
            try{write(db,()->ResearchInputPinR30R1.ensure(db),true);throw new AssertionError("injected partial source migration succeeded");}catch(android.database.sqlite.SQLiteException expected){need(expected.getMessage().contains("INJECTED_SOURCE"),"actual injected source failure");}db.fail=false;need("OLD_EXACT".equals(ResearchSourceJournalR31.inspectUpgradeR31(db)),"rollback restores all old triggers");need(before.equals(stamp(db)),"rollback leaves original Critical stamp");need(count(db,"SELECT count(*) FROM sqlite_master WHERE name='r31_source_event'")==0,"rollback leaves no partial new evidence table");need(count(db,"SELECT count(*) FROM "+SchemaDomainCoordinatorR926.TABLE_JOURNAL)==journal,"rollback does not persist false cutover journal");need(SchemaDomainCoordinatorR926.matches(db,SchemaDomainCoordinatorR926.criticalContract()),"Critical still verifies after source rollback");
            db.execSQL("CREATE TRIGGER unrelated_critical_tamper AFTER UPDATE ON draws BEGIN SELECT 1; END");need("FINGERPRINT_MISMATCH".equals(SchemaDomainCoordinatorR926.matchReason(db,SchemaDomainCoordinatorR926.criticalContract())),"unrelated Critical drift detected");try{write(db,()->ResearchInputPinR30R1.ensure(db),true);throw new AssertionError("unrelated critical drift recertified");}catch(IllegalStateException expected){need(expected.getMessage().startsWith("R31_SOURCE_UPGRADE_UNTRUSTED_CRITICAL_STAMP"),"strict unrelated drift failure:"+expected.getMessage());}need(before.equals(stamp(db)),"tamper cannot obtain new Critical stamp");need("OLD_EXACT".equals(ResearchSourceJournalR31.inspectUpgradeR31(db)),"failed tamper cutover rolled back");need(count(db,"SELECT count(*) FROM sqlite_master WHERE name='r31_source_event'")==0,"failed tamper cutover publishes no source table");db.close();facts.add(Map.of("case","partial_upgrade_and_unrelated_critical_tamper","rollbackPreservesStampAndLegacyEvidence",true));
        }finally{Files.deleteIfExists(file);}
    }
    static void missingStamp()throws Exception{Path file=Files.createTempFile("r31-critical-unstamped-",".sqlite");try{MainChainSchemaR31Host.Owner owner=legacy(file,false,false);write(owner.db,()->ResearchInputPinR30R1.ensure(owner.db),true);need(ResearchInputPinR30R1.triggersValid(owner.db),"source can install without fabricating Critical");need(count(owner.db,"SELECT count(*) FROM "+SchemaDomainCoordinatorR926.TABLE_DOMAIN+" WHERE domain='CRITICAL'")==0,"missing original Critical stamp is not invented");owner.db.close();facts.add(Map.of("case","missing_critical_stamp","CriticalStamp","STILL_MISSING"));}finally{Files.deleteIfExists(file);}}
    public static void main(String[] args)throws Exception{Class.forName("org.sqlite.JDBC");try{upgrade(false);upgrade(true);rollbackAndTamper();missingStamp();System.out.println(JsonR27.write(Map.of("status","PASS","assertions",assertions,"facts",facts,"scope","ACTUAL_CRITICAL_PREDECESSOR_DDL_AND_DOMAIN_NATIVE_SQLITE_NOT_ANDROID_STARTUP_OR_DEVICE")));}finally{if(args.length>0)Files.write(Paths.get(args[0]),SQLiteDatabase.trace);}}
}
