package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;

/** Actual DbStore schema units, with constructor-only Android dependencies
 * supplied by the native platform adapter. No replacement migration logic. */
public final class RuntimeSchemaR31Host {
    static final class FaultDb extends SQLiteDatabase {
        boolean failCriticalJournal;
        FaultDb(Connection connection){super(connection);}
        @Override public void execSQL(String sql,Object[] args){
            if(failCriticalJournal&&sql.contains("INTO "+SchemaDomainCoordinatorR926.TABLE_JOURNAL)
                    &&args!=null&&args.length>2&&String.valueOf(args[2]).startsWith("R31_LEGACY_CRITICAL_DDL:"))
                throw new android.database.sqlite.SQLiteException("INJECTED_LEGACY_CRITICAL_JOURNAL_FAILURE");
            super.execSQL(sql,args);
        }
    }
    static void set(Object owner,String name,Object value)throws Exception{Field field=owner.getClass().getDeclaredField(name);field.setAccessible(true);field.set(owner,value);}
    static long scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){if(!c.moveToFirst())throw new AssertionError(sql);return c.getLong(0);}}
    static int mutations(int from){int n=0;for(String call:SQLiteDatabase.trace.subList(from,SQLiteDatabase.trace.size()))if(call.matches("(?is).*\\t(?:execSQL|insert|update|delete)\\t\\s*(?:INSERT|UPDATE|DELETE|REPLACE).*"))n++;return n;}
    static void originalCritical(SQLiteDatabase db)throws Exception{
        SchemaDomainCoordinatorR926.Contract c=SchemaDomainCoordinatorR926.criticalContract();
        SchemaDomainCoordinatorR926.runStep(db,c,"R35_SCHEMA",DbHelper::ensureR35Schema);
        SchemaDomainCoordinatorR926.runStep(db,c,"R36_SCHEMA",DbHelper::ensureR36Schema);
        SchemaDomainCoordinatorR926.runStep(db,c,"R811_CRITICAL_SCHEMA",DbHelper::ensureR811CriticalSchema);
        SchemaDomainCoordinatorR926.runStep(db,c,"R815_CRITICAL_SCHEMA",DbHelper::ensureR815CriticalSchema);
        SchemaDomainCoordinatorR926.runStep(db,c,"R906_CRITICAL_SCHEMA",DbHelper::ensureR906CriticalSchema);
        SchemaDomainCoordinatorR926.runStep(db,c,"V51_RUNTIME_SCHEMA",V51Repository::ensure);
        SchemaDomainCoordinatorR926.runStep(db,c,"B03_RUNTIME_SCHEMA",DbHelper::ensureTianshuB03);
        SchemaDomainCoordinatorR926.runStep(db,c,"R925_CRITICAL_SCHEMA",SchemaRevisionR925::ensureCriticalTables);
        SchemaDomainCoordinatorR926.runStep(db,c,"R926_EVENT_LEDGER_SCHEMA",RuntimeEventLedgerR926::ensureSchema);
        SchemaDomainCoordinatorR926.runStep(db,c,"R26_FEEDBACK_SCHEMA",x->x.execSQL(com.ethk3.novaonepro.strategyv2.RuntimeSchemaR26.FEEDBACK));
        SchemaDomainCoordinatorR926.runStep(db,c,"F02_SETTLEMENT_RECOVERY_AUDIT_V1",SettlementTaskStoreR36::ensureRepairAuditSchema);
        SchemaDomainCoordinatorR926.runStep(db,c,"CP12_CRITICAL_CANONICAL_REASON_V1",com.ethk3.novaonepro.r952.CanonicalSchemaCP12::installReasons);
        SchemaDomainCoordinatorR926.verifyAndStamp(db,c);
    }
    public static void main(String[] args)throws Throwable{
        Class.forName("org.sqlite.JDBC");
        try(FaultDb db=new FaultDb(DriverManager.getConnection("jdbc:sqlite:"+args[0]))){
            DbHelper helper=ColdSchemaR31Host.helper();ColdSchemaR31Host.initialize(helper,db);SchemaDomainCoordinatorR926.ensureInfrastructure(db);
            // The real startup stamps Critical before the asynchronous Legacy owner.
            originalCritical(db);
            Class<?> type=Class.forName("sun.misc.Unsafe");Field unsafe=type.getDeclaredField("theUnsafe");unsafe.setAccessible(true);
            DbStore store=(DbStore)type.getMethod("allocateInstance",Class.class).invoke(unsafe.get(null),DbStore.class);
            set(store,"helper",helper);set(store,"columnCache",new HashMap<String,HashSet<String>>());set(store,"legacyColumnFailureR31",new ThreadLocal<Throwable>());set(store,"legacyColumnAuditR31",new ThreadLocal<Boolean>());
            Method method=DbStore.class.getDeclaredMethod("legacyRuntimeUnitsR31",SQLiteDatabase.class);method.setAccessible(true);
            @SuppressWarnings("unchecked") LinkedHashMap<String,Runnable> units=(LinkedHashMap<String,Runnable>)method.invoke(store,db);
            int slices=0;boolean ready=false;
            for(;slices<5000&&!ready;slices++){
                try{ready=LegacySchemaCoordinatorR926.advanceUnitsR31(db,"UF_ACTUAL_RUNTIME_",units,false);}
                catch(Throwable failure){String next="UNKNOWN";for(String key:units.keySet())if(scalar(db,"SELECT count(*) FROM v57_schema_migration_journal_r926 WHERE step_id='UF_ACTUAL_RUNTIME_"+key+"' AND status='COMPLETED'")==0){next=key;break;}System.err.println("ACTUAL_RUNTIME_UNIT_FAILED "+next);throw failure;}
                if(!ready)db.execSQL("UPDATE app_state SET state_value='NATIVE_CRITICAL_COMMIT' WHERE state_key='service_status'");
            }
            if(!ready)throw new AssertionError("Actual DbStore runtime units did not converge");
            int before=SQLiteDatabase.trace.size();LegacySchemaCoordinatorR926.validateUnitsReadOnlyR31(db,units);
            if(mutations(before)!=0)throw new AssertionError("Read-only production unit audit performed "+mutations(before)+" DML mutations");
            Method registry=DbStore.class.getDeclaredMethod("ensureModuleRegistryR363",SQLiteDatabase.class);registry.setAccessible(true);
            before=SQLiteDatabase.trace.size();registry.invoke(store,db);
            if(mutations(before)!=0||SQLiteDatabase.trace.subList(before,SQLiteDatabase.trace.size()).stream().anyMatch(x->x.matches("(?is).*\\b(CREATE|ALTER|DROP)\\b.*")))throw new AssertionError("Cold module identity readiness attempted a schema/data repair");
            if(scalar(db,"SELECT count(*) FROM app_state WHERE state_key='service_status' AND state_value='NATIVE_CRITICAL_COMMIT'")!=1)throw new AssertionError("Original app state lost");
            SchemaDomainCoordinatorR926.Contract critical=SchemaDomainCoordinatorR926.criticalContract();
            if(!SchemaDomainCoordinatorR926.matches(db,critical))throw new AssertionError("Actual DbStore Legacy units invalidated the authenticated Critical startup stamp");
            db.beginTransaction();try{com.ethk3.novaonepro.strategyv2.RuntimeSchemaR26.ensure(db);db.setTransactionSuccessful();}finally{db.endTransaction();}
            if(!SchemaDomainCoordinatorR926.matches(db,critical))throw new AssertionError("Original research initialization lost Critical trust after Legacy readiness");
            String first=SchemaDomainCoordinatorR926.fingerprint(db,critical);
            try(LegacyDdlR31.Scope slice=new LegacyDdlR31.Scope(db,false)){
                LegacyDdlR31.exec(db,"ALTER TABLE draws ADD COLUMN r31_owned_fixture INTEGER NOT NULL DEFAULT 0");slice.check();
            }
            if(first.equals(SchemaDomainCoordinatorR926.fingerprint(db,critical))||!SchemaDomainCoordinatorR926.matches(db,critical))throw new AssertionError("Verified Legacy DDL left the actual Critical stamp stale");
            String committed=SchemaDomainCoordinatorR926.fingerprint(db,critical);db.failCriticalJournal=true;
            try(LegacyDdlR31.Scope slice=new LegacyDdlR31.Scope(db,false)){
                try{LegacyDdlR31.exec(db,"ALTER TABLE draws ADD COLUMN r31_rollback_fixture INTEGER NOT NULL DEFAULT 0");throw new AssertionError("Journal failure was not injected");}
                catch(android.database.sqlite.SQLiteException expected){if(!expected.getMessage().contains("INJECTED_LEGACY_CRITICAL"))throw expected;}
            }finally{db.failCriticalJournal=false;}
            if(!committed.equals(SchemaDomainCoordinatorR926.fingerprint(db,critical))||!SchemaDomainCoordinatorR926.matches(db,critical))throw new AssertionError("Failed stamp journal did not roll back its original DDL");
            try(LegacyDdlR31.Scope slice=new LegacyDdlR31.Scope(db,false)){
                LegacyDdlR31.exec(db,"ALTER TABLE draws ADD COLUMN r31_rollback_fixture INTEGER NOT NULL DEFAULT 0");slice.check();
            }
            if(!SchemaDomainCoordinatorR926.matches(db,critical))throw new AssertionError("Owned DDL cannot resume after rollback");
            db.execSQL("CREATE INDEX r31_untrusted_fixture ON draws(issue)");
            String sealed;try(Cursor row=db.rawQuery("SELECT fingerprint FROM "+SchemaDomainCoordinatorR926.TABLE_DOMAIN+" WHERE domain='CRITICAL'",null)){if(!row.moveToFirst())throw new AssertionError("Missing original stamp");sealed=row.getString(0);}
            try(LegacyDdlR31.Scope slice=new LegacyDdlR31.Scope(db,false)){
                try{LegacyDdlR31.exec(db,"ALTER TABLE draws ADD COLUMN r31_untrusted_fixture_probe INTEGER NOT NULL DEFAULT 0");throw new AssertionError("Existing unknown drift allowed another Critical mutation");}
                catch(DatabaseCoordinator.SchemaPendingR31 expected){if(!expected.getMessage().contains("UNTRUSTED_CRITICAL:FINGERPRINT_MISMATCH"))throw expected;}
            }
            if(SchemaDomainCoordinatorR926.matches(db,critical))throw new AssertionError("Unknown Critical drift was authenticated by an unrelated Legacy DDL");
            try(Cursor row=db.rawQuery("SELECT fingerprint FROM "+SchemaDomainCoordinatorR926.TABLE_DOMAIN+" WHERE domain='CRITICAL'",null)){if(!row.moveToFirst()||!sealed.equals(row.getString(0)))throw new AssertionError("Untrusted stamp was replaced");}
            System.out.println("ACTUAL_LEGACY_CRITICAL_CUTOVER_NATIVE_PASS originalDDLAndStampAtomic=true journalFailureRollsBackDDL=true retryResumes=true unrelatedDriftNeverRestamped=true");
            System.out.println("ACTUAL_DBSTORE_RUNTIME_SCHEMA_NATIVE_PASS units="+units.size()+" slices="+slices+" readOnlyAuditDml=0 criticalCommitsBetweenSlices="+(slices-1)+" scope=HOST_NOT_ANDROID");
        }
    }
}
