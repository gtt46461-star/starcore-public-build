package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.sql.DriverManager;
import java.nio.file.*;

/** Real SQLite production projection installers, not an Android connection-pool timing claim. */
public final class ProjectionSchemaR29HostTest {
    static int checks;
    static void check(String name,boolean ok){System.out.println("R29_PROJECTION_SCHEMA|"+name+"|"+(ok?"OK":"FAILED"));if(!ok)throw new AssertionError(name);checks++;}
    static final class CountDb extends SQLiteDatabase {
        int ddl,catalog;boolean rejectDdl;
        CountDb(String file)throws Exception{super(DriverManager.getConnection("jdbc:sqlite:"+file));}
        @Override public void execSQL(String sql,Object[] args){String upper=sql.trim().toUpperCase(java.util.Locale.ROOT);if(upper.startsWith("CREATE ")||upper.startsWith("DROP ")||upper.startsWith("ALTER ")){ddl++;if(rejectDdl)throw new IllegalStateException("MODELED_ANDROID_RW_POOL_SCHEMA_INVALIDATION");}super.execSQL(sql,args);}
        @Override public Cursor rawQuery(String sql,String[] args){if(sql.startsWith("SELECT name,type,tbl_name,sql FROM main.sqlite_master WHERE name IN ("))catalog++;return super.rawQuery(sql,args);}
        void reset(){ddl=0;catalog=0;}
    }
    interface Work{void run()throws Exception;}
    static void tx(CountDb db,Work work,boolean rollback)throws Exception{db.beginTransactionNonExclusive();try{work.run();if(!rollback)db.setTransactionSuccessful();}finally{db.endTransaction();}}
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){if(!c.moveToFirst())throw new AssertionError(sql);return c.getLong(0);}}
    static String s(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){if(!c.moveToFirst())throw new AssertionError(sql);return c.getString(0);}}
    static void realtime(CountDb db){LegacyAuthorityProjectionR29.ensureRealtime(db);LegacyLiveProjectionR29.ensureRealtime(db);RealtimeResearchProjectionR29.ensureRealtime(db);}
    static void sourceTables(CountDb db){
        db.execSQL("CREATE TABLE r26_live_model(policy_hash TEXT PRIMARY KEY,checkpoint_hash TEXT,state_hash TEXT,through_ordinal INTEGER,status TEXT,state BLOB)");
        for(String table:new String[]{"r27_live_shadow","r27_eprocess","r27_certificate"})db.execSQL("CREATE TABLE "+table+"(policy_hash TEXT PRIMARY KEY)");
        db.execSQL("CREATE TABLE r27_live_frozen(policy_hash TEXT,target_ordinal INTEGER,PRIMARY KEY(policy_hash,target_ordinal))");
        db.execSQL("CREATE TABLE r27_chart_cache(panel_id TEXT,key TEXT,PRIMARY KEY(panel_id,key))");
        db.execSQL("CREATE TABLE r26_evolution(attempt_id TEXT PRIMARY KEY)");
        db.execSQL("CREATE TABLE su1_certificate(id TEXT PRIMARY KEY)");db.execSQL("CREATE TABLE su2_certificate(id TEXT PRIMARY KEY)");
    }
    static void research(CountDb db){LegacyAuthorityProjectionR29.installResearch(db);LegacyLiveProjectionR29.installResearch(db);RealtimeResearchProjectionR29.installResearch(db);}
    static boolean conflict(CountDb db,Work work)throws Exception{try{work.run();return false;}catch(IllegalStateException failure){if(!failure.getMessage().startsWith("R29_PROJECTION_SCHEMA_CONFLICT:"))throw failure;return true;}}
    static void rootControl()throws Exception{
        try(CountDb db=new CountDb(":memory:")){realtime(db);db.reset();for(int i=0;i<1000;i++)realtime(db);System.out.println("R29_PROJECTION_SCHEMA|ROOT_1000_COUNTERS|ddl="+db.ddl+"|freshCatalog="+db.catalog);check("P1_1000_ROOT_DRAIN_SCHEMA_CHECKS_ZERO_DDL",db.ddl==0);}
    }
    static void researchControl()throws Exception{
        try(CountDb db=new CountDb(":memory:")){sourceTables(db);tx(db,()->research(db),false);db.reset();tx(db,()->{for(int i=0;i<100;i++)research(db);},false);System.out.println("R29_PROJECTION_SCHEMA|RESEARCH_100_COUNTERS|ddl="+db.ddl+"|freshCatalog="+db.catalog);check("P2_100_RESEARCH_SETUP_REPEATS_ZERO_DDL",db.ddl==0);}
    }
    public static void main(String[] args)throws Exception{
        if(args.length>0){if(args[0].equals("--old-root-control"))rootControl();else researchControl();return;}
        rootControl();researchControl();
        try(CountDb db=new CountDb(":memory:")){
            db.reset();realtime(db);check("P3_COLD_ROOT_INSTALL_RETAINS_ORIGINAL_FIVE_TABLES",db.ddl==5);
            db.reset();db.rejectDdl=true;for(int i=0;i<1000;i++)realtime(db);db.rejectDdl=false;
            check("P4_INSTALLED_ROOT_SCHEMA_NEVER_ENTERS_MODELED_RW_POOL_INVALIDATION",db.ddl==0&&db.catalog==3000);
            db.execSQL("INSERT INTO r29_legacy_live_projection VALUES('p',7,4,'checkpoint','hash',29,'ACTIVE','payload',100)");
            db.execSQL("DROP TABLE r29_research_heartbeat");db.reset();realtime(db);check("P5_ONLY_MISSING_ROOT_OBJECT_CREATED_AND_EXISTING_ROWS_RETAINED",db.ddl==1&&n(db,"SELECT revision FROM r29_legacy_live_projection WHERE policy_hash='p'")==4);
            String exact=s(db,"SELECT sql FROM sqlite_master WHERE name='r29_projection_applied'");
            long version=n(db,"PRAGMA schema_version");db.execSQL("DROP TABLE r29_projection_applied");db.execSQL("CREATE TABLE r29_projection_applied(fake TEXT)");db.execSQL("PRAGMA schema_version="+version);db.reset();
            check("P6_SAME_VERSION_WRONG_ROOT_TABLE_FAILS_BEFORE_DDL",conflict(db,()->RealtimeResearchProjectionR29.ensureRealtime(db))&&db.ddl==0);
            db.execSQL("DROP TABLE r29_projection_applied");db.execSQL(exact);
            tx(db,()->{db.execSQL("DROP TABLE r29_research_heartbeat");RealtimeResearchProjectionR29.ensureRealtime(db);},true);db.reset();realtime(db);check("P7_ROLLED_BACK_REPAIR_PUBLISHES_NO_FAKE_READY",db.ddl==0);
        }
        try(CountDb db=new CountDb(":memory:")){
            sourceTables(db);tx(db,()->research(db),false);db.reset();tx(db,()->{for(int i=0;i<1000;i++)research(db);},false);
            check("P8_1000_ALL_RESEARCH_INSTALLERS_KEEP_ORIGINAL_DDL_STABLE",db.ddl==0&&db.catalog==3000);
            long revision=n(db,"SELECT revision FROM r29_legacy_authority_source");db.execSQL("INSERT INTO su1_certificate VALUES('a')");db.execSQL("INSERT INTO su2_certificate VALUES('b')");
            check("P9_AUTHORITY_TRIGGERS_STILL_ADVANCE_TRUE_REVISION",n(db,"SELECT revision FROM r29_legacy_authority_source")==revision+2);
            db.execSQL("INSERT INTO r26_live_model VALUES('p','c1','h1',29,'ACTIVE',X'01')");db.execSQL("UPDATE r29_legacy_live_pending SET acked_revision=revision");long before=n(db,"SELECT revision FROM r29_legacy_live_pending WHERE policy_hash='p'");
            tx(db,()->{research(db);db.execSQL("INSERT OR REPLACE INTO r26_live_model VALUES('p','c2','h2',30,'ACTIVE',X'02')");},false);
            check("P10_LIVE_OUTER_REPLACE_CANNOT_RESET_REVISION_OR_ACK",n(db,"SELECT revision FROM r29_legacy_live_pending WHERE policy_hash='p'")==before+1&&n(db,"SELECT acked_revision FROM r29_legacy_live_pending WHERE policy_hash='p'")==before);
            db.execSQL("INSERT INTO r27_live_shadow VALUES('p')");db.execSQL("UPDATE r29_projection_pending SET acked_revision=revision WHERE table_name='r27_live_shadow'");long queued=n(db,"SELECT revision FROM r29_projection_pending WHERE table_name='r27_live_shadow'");
            tx(db,()->{research(db);db.execSQL("INSERT OR REPLACE INTO r27_live_shadow VALUES('p')");},false);
            check("P11_GENERIC_PROJECTION_TRIGGER_AND_ACK_KEEP_ORIGINAL_SEMANTICS",n(db,"SELECT revision FROM r29_projection_pending WHERE table_name='r27_live_shadow'")==queued+1&&n(db,"SELECT acked_revision FROM r29_projection_pending WHERE table_name='r27_live_shadow'")==queued);
            String guard=s(db,"SELECT sql FROM sqlite_master WHERE name='r29_authority_su1_certificate_insert'");
            db.execSQL("DROP TRIGGER r29_authority_su1_certificate_insert");db.reset();tx(db,()->LegacyAuthorityProjectionR29.installResearch(db),false);check("P12_MISSING_AUTHORITY_TRIGGER_REINSTALLED_ONLY_ONCE",db.ddl==1);
            db.execSQL("DROP TRIGGER r29_authority_su1_certificate_insert");db.execSQL("CREATE TRIGGER r29_authority_su1_certificate_insert AFTER INSERT ON su1_certificate BEGIN SELECT 1; END");db.reset();
            check("P13_WRONG_AUTHORITY_GUARD_IS_NOT_SILENTLY_TRUSTED",conflict(db,()->LegacyAuthorityProjectionR29.installResearch(db))&&db.ddl==0);
            db.execSQL("DROP TRIGGER r29_authority_su1_certificate_insert");db.execSQL(guard);
            String live=s(db,"SELECT sql FROM sqlite_master WHERE name='r29_legacy_live_update'");db.execSQL("DROP TRIGGER r29_legacy_live_update");db.execSQL("CREATE TRIGGER r29_legacy_live_update AFTER UPDATE ON r26_live_model BEGIN SELECT 1; END");
            boolean rejected=false;try{LegacyLiveProjectionR29.installResearch(db);}catch(IllegalStateException error){rejected=error.getMessage().equals("R29_LEGACY_PROJECTION_TRIGGER_UPGRADE_REQUIRES_TRANSACTION");}
            check("P14_LEGACY_TRIGGER_UPGRADE_STILL_REQUIRES_TRANSACTION",rejected);
            db.reset();tx(db,()->LegacyLiveProjectionR29.installResearch(db),false);check("P15_EXPLICIT_LEGACY_TRIGGER_UPGRADE_PRESERVED_EXACTLY",db.ddl==2&&live.equals(s(db,"SELECT sql FROM sqlite_master WHERE name='r29_legacy_live_update'")));
            String generic=s(db,"SELECT sql FROM sqlite_master WHERE name='r29_project_r27_live_shadow_update'");db.execSQL("DROP TRIGGER r29_project_r27_live_shadow_update");db.execSQL("CREATE TRIGGER r29_project_r27_live_shadow_update AFTER UPDATE ON r27_live_shadow BEGIN SELECT 'now  text'; END");db.reset();tx(db,()->RealtimeResearchProjectionR29.installResearch(db),false);
            check("P16_EXPLICIT_GENERIC_TRIGGER_UPGRADE_AND_LITERAL_PROOF_PRESERVED",db.ddl==2&&generic.equals(s(db,"SELECT sql FROM sqlite_master WHERE name='r29_project_r27_live_shadow_update'")));
            tx(db,()->{db.execSQL("DROP TRIGGER r29_authority_su1_certificate_insert");LegacyAuthorityProjectionR29.installResearch(db);},true);db.reset();tx(db,()->research(db),false);check("P17_RESEARCH_REPAIR_ROLLBACK_HAS_NO_SCHEMA_CACHE",db.ddl==0);
            db.execSQL("DROP TABLE r29_su1_sealed_binding");db.execSQL("CREATE VIEW r29_su1_sealed_binding AS SELECT 'wrong' AS prediction_id");db.reset();check("P18_SAME_NAME_WRONG_OBJECT_TYPE_REJECTED",conflict(db,()->LegacyAuthorityProjectionR29.installResearch(db))&&db.ddl==0);
        }
        try(CountDb db=new CountDb(":memory:")){tx(db,()->realtime(db),true);check("P19_COLD_INSTALL_ROLLBACK_RETAINS_NO_TABLE",n(db,"SELECT count(*) FROM sqlite_master WHERE name LIKE 'r29_%'")==0);db.reset();realtime(db);check("P20_COLD_ROLLBACK_RETRY_INSTALLS_ACTUAL_OBJECTS",db.ddl==5);}
        Path file=Files.createTempFile("r29-projection-schema-",".db");try{try(CountDb db=new CountDb(file.toString())){realtime(db);}try(CountDb db=new CountDb(file.toString())){db.reset();realtime(db);check("P21_REOPEN_PROVES_CURRENT_SCHEMA_WITH_ZERO_DDL",db.ddl==0&&db.catalog==3);}}finally{Files.deleteIfExists(file);}
        System.out.println("R29_PROJECTION_SCHEMA|ALL_PASS|checks="+checks+"|REAL_SQLITE_PROJECTION_INSTALLERS_HOST_ONLY_ANDROID_POOL_ACCEPTANCE_SEPARATE");
    }
}
