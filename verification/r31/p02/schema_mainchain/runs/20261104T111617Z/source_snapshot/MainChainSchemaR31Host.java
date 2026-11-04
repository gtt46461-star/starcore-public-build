package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.lang.reflect.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import sun.misc.Unsafe;

/** Fresh production DbStore/Legacy/DbHelper methods over native SQLite/JDBC.
 * Android connection/lifecycle setup is replaced; schema and business methods are not.
 */
public final class MainChainSchemaR31Host {
    static void require(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static final Unsafe UNSAFE;
    static{try{Field field=Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);UNSAFE=(Unsafe)field.get(null);}catch(Exception error){throw new ExceptionInInitializerError(error);}}
    static void set(Object object,String name,Object value)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);f.set(object,value);}
    static final class Db extends SQLiteDatabase {
        boolean failMainIndex;
        Db(Connection connection){super(connection);}
        @Override public void execSQL(String sql,Object[] args){
            if(failMainIndex&&sql.toUpperCase(Locale.ROOT).contains("CREATE INDEX")&&sql.contains("idx_mccs_target_rank"))
                throw new android.database.sqlite.SQLiteException("INJECTED_OWNER_MAIN_INDEX_FAILURE");
            super.execSQL(sql,args);
        }
    }
    static final class Owner {
        final Db db;final DbHelper helper;final DatabaseCoordinator coordinator;final DbStore store;
        Owner(Path path)throws Exception{
            db=new Db(DriverManager.getConnection("jdbc:sqlite:"+path));SQLiteOpenHelper.hostDatabase=db;
            helper=(DbHelper)UNSAFE.allocateInstance(DbHelper.class);
            coordinator=(DatabaseCoordinator)UNSAFE.allocateInstance(DatabaseCoordinator.class);set(coordinator,"helper",helper);
            store=(DbStore)UNSAFE.allocateInstance(DbStore.class);set(store,"coordinator",coordinator);set(store,"helper",helper);set(store,"columnCache",new HashMap<String,HashSet<String>>());
        }
    }
    static long count(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){require(c.moveToFirst(),"COUNT_MISSING");return c.getLong(0);}}
    static String value(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){require(c.moveToFirst(),"VALUE_MISSING:"+sql);return c.getString(0);}}
    static String quote(String text){return "'"+text.replace("'","''")+"'";}
    static Models.ComboCandidate candidate(){Models.ComboCandidate c=new Models.ComboCandidate();c.position=1;c.n1=1;c.n2=2;c.n3=3;c.k3Mask=Models.buildK3Mask(1,2,3);c.rank=1;c.baseIssue="202611040100";c.targetIssue="202611040101";c.finalP=.31;c.dynamicGate=.345;return c;}
    static List<String> stage(String name)throws Exception{
        List<String> out=new ArrayList<>();for(String line:SQLiteDatabase.trace)if(line.startsWith(name+"\t"))out.add(line);return out;
    }
    static void noDdl(String stage)throws Exception{
        for(String line:stage(stage)){
            String sql=line.split("\t",3)[2].trim().toUpperCase(Locale.ROOT);
            require(!sql.matches("^(CREATE|ALTER|DROP|REINDEX|VACUUM|ATTACH|DETACH).*"),"BUSINESS_DDL:"+line);
        }
    }
    static void missingBusinessCreatesNothing(Owner owner)throws Exception{
        SQLiteDatabase.stage="BUSINESS_MISSING";long before=count(owner.db,"SELECT COUNT(*) FROM sqlite_master");
        try{owner.store.saveShadowCandidates("202611040100","202611040101",Collections.singletonList(candidate()),1);throw new AssertionError("MISSING_SCHEMA_ACCEPTED");}
        catch(DatabaseCoordinator.SchemaPendingR31 expected){require(expected.getMessage().startsWith("SCHEMA_PENDING:MAIN_CHAIN_CLOSED_LOOP:"),"UNCLASSIFIED_SCHEMA_WAIT");System.out.println("MISSING_REASON "+expected.getMessage());}
        require(count(owner.db,"SELECT COUNT(*) FROM sqlite_master")==before,"BUSINESS_CREATED_OBJECTS");noDdl("BUSINESS_MISSING");
        System.out.println("CHECK missing_business_schema_pending_zero_ddl_zero_objects");
    }
    static void createLegacyBase(Owner owner)throws Exception{
        SQLiteDatabase.stage="HOST_LEGACY_BASE";
        Method base=DbHelper.class.getDeclaredMethod("createBaseTables",SQLiteDatabase.class);base.setAccessible(true);base.invoke(owner.helper,owner.db);
        // The new Legacy unit requires real Critical predecessor objects. Execute
        // their existing schema methods on this isolated empty database, never
        // substitute same-named placeholder tables for triggers/views.
        SQLiteDatabase.stage="HOST_CRITICAL_PREDECESSORS";owner.db.beginTransaction();
        try{
            for(String name:new String[]{"migrateDrawColumns","migrateAppStateColumns"}){Method m=DbHelper.class.getDeclaredMethod(name,SQLiteDatabase.class);m.setAccessible(true);m.invoke(owner.helper,owner.db);}
            DbHelper.ensureR35Schema(owner.db);DbHelper.ensureR36Schema(owner.db);DbHelper.ensureR811CriticalSchema(owner.db);
            DbHelper.ensureR815CriticalSchema(owner.db);DbHelper.ensureR906CriticalSchema(owner.db);V51Repository.ensure(owner.db);
            DbHelper.ensureTianshuB03(owner.db);SchemaRevisionR925.ensureCriticalTables(owner.db);RuntimeEventLedgerR926.ensureSchema(owner.db);
            owner.db.execSQL(com.ethk3.novaonepro.strategyv2.RuntimeSchemaR26.FEEDBACK);
            SchemaDomainCoordinatorR926.verifyObjects(owner.db,SchemaDomainCoordinatorR926.criticalContract());
            owner.db.setTransactionSuccessful();
        }finally{owner.db.endTransaction();}
        SQLiteDatabase.stage="HOST_LEGACY_SENTINELS";
        owner.db.execSQL("ALTER TABLE "+DbHelper.TABLE_MAIN_CHAIN_CANDIDATE_SNAPSHOT+" ADD COLUMN legacy_payload BLOB");
        owner.db.execSQL("INSERT INTO "+DbHelper.TABLE_MAIN_CHAIN_CANDIDATE_SNAPSHOT+"(snapshot_key,legacy_payload) VALUES('original-snapshot',x'000102FEFF')");
        owner.db.execSQL("ALTER TABLE "+DbHelper.TABLE_CAUSAL_FACTOR_MATRIX+" ADD COLUMN factor_name TEXT");
        owner.db.execSQL("ALTER TABLE "+DbHelper.TABLE_CAUSAL_FACTOR_MATRIX+" ADD COLUMN sample_count INTEGER DEFAULT 0");
        owner.db.execSQL("INSERT INTO "+DbHelper.TABLE_CAUSAL_FACTOR_MATRIX+"(causal_key,factor_name,sample_count) VALUES('original-factor','ORIGINAL_FACTOR',37)");
        owner.db.execSQL("ALTER TABLE "+DbHelper.TABLE_DIAG_LOG+" ADD COLUMN legacy_diagnostic TEXT");
        owner.db.execSQL("INSERT INTO "+DbHelper.TABLE_DIAG_LOG+"(legacy_diagnostic) VALUES('ORIGINAL_DIAGNOSTIC')");
    }
    static void originalRows(Owner owner){
        require("000102FEFF".equals(value(owner.db,"SELECT hex(legacy_payload) FROM "+DbHelper.TABLE_MAIN_CHAIN_CANDIDATE_SNAPSHOT+" WHERE snapshot_key='original-snapshot'")),"SNAPSHOT_ORIGINAL_BYTES_LOST");
        require("ORIGINAL_FACTOR:37".equals(value(owner.db,"SELECT factor_name||':'||sample_count FROM "+DbHelper.TABLE_CAUSAL_FACTOR_MATRIX+" WHERE causal_key='original-factor'")),"FACTOR_ORIGINAL_LOST");
        require("ORIGINAL_DIAGNOSTIC".equals(value(owner.db,"SELECT legacy_diagnostic FROM "+DbHelper.TABLE_DIAG_LOG+" WHERE legacy_diagnostic IS NOT NULL")),"DIAGNOSTIC_ORIGINAL_LOST");
    }
    static void migrateAndRepeat(Owner owner)throws Exception{
        SQLiteDatabase.stage="MIGRATION_OWNER";
        require(LegacySchemaCoordinatorR926.ensure(owner.coordinator,db->{}),"LEGACY_OWNER_NOT_READY");
        LegacySchemaCoordinatorR926.requireMainChainClosedLoopReadyR31(owner.db);originalRows(owner);
        require("COMPLETED".equals(value(owner.db,"SELECT status FROM "+SchemaDomainCoordinatorR926.TABLE_JOURNAL+" WHERE domain='LEGACY' AND step_id="+quote(LegacySchemaCoordinatorR926.MAIN_CHAIN_STEP_R31))),"MAIN_STEP_NOT_JOURNALED");
        SQLiteDatabase.stage="MIGRATION_REPEAT";require(LegacySchemaCoordinatorR926.ensure(owner.coordinator,db->{throw new AssertionError("COMPLETED_INITIALIZER_REPLAYED");}),"REPEAT_NOT_READY");
        noDdl("MIGRATION_REPEAT");originalRows(owner);System.out.println("CHECK owner_journal_migration_repeat_zero_ddl_original_snapshot_factor_diagnostics_retained");
    }
    static void readyBusiness(Owner owner)throws Exception{
        SQLiteDatabase.stage="BUSINESS_READY";long before=count(owner.db,"SELECT COUNT(*) FROM sqlite_master");
        owner.store.saveShadowCandidates("202611040100","202611040101",Collections.singletonList(candidate()),1);
        require(count(owner.db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_MAIN_CHAIN_CANDIDATE_SNAPSHOT+" WHERE target_issue='202611040101'")==1,"SNAPSHOT_NOT_SAVED");
        require(count(owner.db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_SHADOW+" WHERE target_issue='202611040101'")==1,"SHADOW_NOT_SAVED");
        require(count(owner.db,"SELECT COUNT(*) FROM sqlite_master")==before,"READY_BUSINESS_CHANGED_OBJECTS");noDdl("BUSINESS_READY");originalRows(owner);
        System.out.println("CHECK ready_actual_saveShadowCandidates_snapshot_shadow_written_business_ddl_zero");
    }
    static void matchingStampCannotSkipNewStep(Owner owner)throws Exception{
        SQLiteDatabase.stage="HOST_OLD_READY_STAMP";SchemaDomainCoordinatorR926.Contract contract=SchemaDomainCoordinatorR926.legacyContract();
        owner.db.execSQL("DELETE FROM "+SchemaDomainCoordinatorR926.TABLE_JOURNAL+" WHERE domain=? AND revision=? AND step_id=?",new Object[]{contract.domain,contract.revision,LegacySchemaCoordinatorR926.MAIN_CHAIN_STEP_R31});
        SchemaDomainCoordinatorR926.verifyAndStamp(owner.db,contract);require(SchemaDomainCoordinatorR926.matches(owner.db,contract),"FIXTURE_STAMP_DOES_NOT_MATCH");
        SQLiteDatabase.stage="MIGRATION_MATCHING_OLD_STAMP";
        require(LegacySchemaCoordinatorR926.ensure(owner.coordinator,db->{throw new AssertionError("OLD_COMPLETED_INITIALIZER_REPLAYED");}),"OLD_READY_DID_NOT_MIGRATE_NEW_STEP");
        require(SchemaDomainCoordinatorR926.stepComplete(owner.db,contract,LegacySchemaCoordinatorR926.MAIN_CHAIN_STEP_R31),"MATCHING_OLD_STAMP_SKIPPED_MAIN_STEP");originalRows(owner);
        System.out.println("CHECK matching_ready_stamp_cannot_skip_new_journal_initializer");
    }
    static void interruptedOwnerRetry(Owner owner)throws Exception{
        SQLiteDatabase.stage="HOST_DRIFT_INJECTION";owner.db.execSQL("DROP INDEX idx_mccs_target_rank");owner.db.failMainIndex=true;
        SQLiteDatabase.stage="MIGRATION_FAILURE";require(!LegacySchemaCoordinatorR926.ensure(owner.coordinator,db->{}),"IGNORED_DDL_FAILURE_REPORTED_READY");
        require("FAILED".equals(value(owner.db,"SELECT state FROM "+SchemaDomainCoordinatorR926.TABLE_DOMAIN+" WHERE domain='LEGACY'")),"OWNER_FAILURE_NOT_RECORDED");
        require("FAILED".equals(value(owner.db,"SELECT status FROM "+SchemaDomainCoordinatorR926.TABLE_JOURNAL+" WHERE domain='LEGACY' AND step_id="+quote(LegacySchemaCoordinatorR926.MAIN_CHAIN_STEP_R31))),"STEP_FAILURE_NOT_RECORDED");
        originalRows(owner);owner.db.failMainIndex=false;
        SQLiteDatabase.stage="MIGRATION_RETRY";require(LegacySchemaCoordinatorR926.ensure(owner.coordinator,db->{}),"OWNER_RETRY_DID_NOT_REPAIR");
        LegacySchemaCoordinatorR926.requireMainChainClosedLoopReadyR31(owner.db);originalRows(owner);
        require(count(owner.db,"SELECT attempt FROM "+SchemaDomainCoordinatorR926.TABLE_JOURNAL+" WHERE domain='LEGACY' AND step_id="+quote(LegacySchemaCoordinatorR926.MAIN_CHAIN_STEP_R31))>=2,"RETRY_ATTEMPT_NOT_ADVANCED");
        System.out.println("CHECK migration_injected_ddl_failure_not_ready_retry_recovers_original_data_preserved");
    }
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");Owner owner=new Owner(Paths.get(args[0]));
        try{missingBusinessCreatesNothing(owner);createLegacyBase(owner);migrateAndRepeat(owner);readyBusiness(owner);matchingStampCannotSkipNewStep(owner);interruptedOwnerRetry(owner);}
        finally{Files.write(Paths.get(args[1]),SQLiteDatabase.trace);owner.db.close();}
        System.out.println("MAIN_CHAIN_NATIVE_SQLITE_HOST_PASS_NOT_ANDROID_NOT_DEVICE_NOT_FULL_B02_BATCH_ACCEPTANCE");
    }
}
