package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.sql.*;
import java.util.*;

/** Production R929/R932 callers and schema cache on native SQLite; Android/device latency remains untested. */
public final class R929BusinessSchemaHost {
    static int assertions;
    static void need(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    static long scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){need(c.moveToFirst(),sql);return c.getLong(0);}}
    static SQLiteDatabase db()throws Exception{return new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite::memory:"));}
    static void tx(SQLiteDatabase db,Runnable body,boolean commit){TransactionSchemaCacheR941.Scope scope;db.beginTransaction();scope=TransactionSchemaCacheR941.enter(db);try{body.run();if(commit)db.setTransactionSuccessful();}finally{db.endTransaction();if(commit)TransactionSchemaCacheR941.afterCommit(scope);TransactionSchemaCacheR941.leave(scope);}}
    static void install(SQLiteDatabase db){tx(db,()->ResearchPlatformStoreR929.createSchema(db),true);}
    static void reset(SQLiteDatabase db){db.ddlStatements.clear();db.ddlCount=0;}
    static void fixtureChoice(SQLiteDatabase db,String issue){
        db.execSQL("CREATE TABLE IF NOT EXISTS r974_prediction_rejection(target_issue TEXT,candidate_id TEXT,certificate_hash TEXT,reason TEXT,recorded_at INTEGER)");
        db.execSQL("INSERT INTO "+UnifiedResearchChoiceR932.TABLE+" VALUES(?,?,?,'','',?,1)",new Object[]{issue,ModelBundleRuntimeR932.current().hash,"",V51ChecksumEngine.sha256("controlled schema engineering fixture")});
    }
    static boolean freeze(DbStore store,String issue){try(ModelBundleRuntimeR932.Scope scope=ModelBundleRuntimeR932.forEvidence(ModelBundleRuntimeR932.current().hash,issue,System.currentTimeMillis())){return UnifiedResearchChoiceR932.freeze(store,issue);}}
    static void baseline()throws Exception{
        try(SQLiteDatabase db=db()){
            install(db);db.execSQL("CREATE TABLE unrelated_schema_version_change(id INTEGER)");reset(db);
            tx(db,()->ResearchPlatformStoreR929.activateRuntimeBundle(db,ModelBundleRuntimeR932.current(),"202611040100",100),true);
            need(db.ddlCount>0,"baseline activate expected runtime DDL");System.out.println("BASELINE_D05|businessDdl="+db.ddlCount+"|schemaVersionDrift=true");
        }
        try(SQLiteDatabase db=db()){
            DbStore store=new DbStore(db);reset(db);freeze(store,"202611040100");
            need(db.ddlCount>0&&store.coordinator.migrations>0,"baseline freeze expected lazy migration");System.out.println("BASELINE_D06|businessDdl="+db.ddlCount+"|migrationCallbacks="+store.coordinator.migrations+"|missingTablesCreated=true");
        }
    }
    static void readyAndDrift()throws Exception{
        try(SQLiteDatabase db=db()){
            DbStore store=new DbStore(db);ResearchPlatformStoreR929.ensureSchema(store);need(store.coordinator.migrations==1,"explicit migration owner entered");need(ResearchPlatformStoreR929.schemaReadyReadOnlyR31(db),"actual full schema ready");
            db.execSQL("CREATE TABLE unrelated_schema_version_change(id INTEGER)");reset(db);
            tx(db,()->ResearchPlatformStoreR929.activateRuntimeBundle(db,ModelBundleRuntimeR932.current(),"202611040100",100),true);
            need(db.ddlCount==0,"activate after schema_version drift is DDL-free");need(scalar(db,"SELECT count(*) FROM "+ResearchPlatformStoreR929.TABLE_BUNDLE+" WHERE state='ACTIVE'")==1,"verified bundle registry mutation retained");
            fixtureChoice(db,"202611040101");reset(db);need(freeze(store,"202611040101"),"immutable frozen choice remains usable");need(db.ddlCount==0,"freeze executes no DDL on drift/cache miss");need(store.coordinator.migrations==1,"business freeze never requests migration");
            String original=String.valueOf(scalar(db,"SELECT frozen_at FROM "+UnifiedResearchChoiceR932.TABLE+" WHERE target_issue='202611040101'"));need(freeze(store,"202611040101"),"freeze retry reuses existing choice");need(original.equals(String.valueOf(scalar(db,"SELECT frozen_at FROM "+UnifiedResearchChoiceR932.TABLE+" WHERE target_issue='202611040101'"))),"immutable freeze unchanged");
            db.execSQL("DROP INDEX idx_r929_bundle_one_active");reset(db);need(!ResearchPlatformStoreR929.schemaReadyReadOnlyR31(db),"missing uniqueness index not cached ready");need(db.ddlCount==0,"readiness never recreates index");ResearchPlatformStoreR929.ensureSchema(store);need(ResearchPlatformStoreR929.schemaReadyReadOnlyR31(db),"explicit owner repairs missing index");
            db.execSQL("ALTER TABLE "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+" RENAME COLUMN contract_revision TO legacy_revision");reset(db);need(!ResearchPlatformStoreR929.schemaReadyReadOnlyR31(db),"missing contract column not ready");need(db.ddlCount==0,"readiness does not ALTER missing column");ResearchPlatformStoreR929.ensureSchema(store);need(ResearchPlatformStoreR929.schemaReadyReadOnlyR31(db),"explicit owner retains additive contract-column migration");
            String certificateSql;try(Cursor definition=db.rawQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name=?",new String[]{ResearchPlatformStoreR929.TABLE_CERTIFICATE})){need(definition.moveToFirst(),"certificate source DDL");certificateSql=definition.getString(0);}
            String noUnique=certificateSql.replace("certificate_hash TEXT NOT NULL UNIQUE","certificate_hash TEXT NOT NULL");need(!noUnique.equals(certificateSql),"fixture removes global certificate hash uniqueness");
            db.execSQL("DROP TABLE "+ResearchPlatformStoreR929.TABLE_CERTIFICATE);db.execSQL(noUnique);db.execSQL("CREATE UNIQUE INDEX partial_certificate_hash_only ON "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+"(certificate_hash) WHERE state='ACTIVE'");
            ResearchPlatformStoreR929.createSchema(db);reset(db);need(!ResearchPlatformStoreR929.schemaReadyReadOnlyR31(db),"partial unique cannot replace global uniqueness");need(db.ddlCount==0,"bad uniqueness check stays read only");
            System.out.println("R929_READY_DRIFT_PASS|runtimeCreateAlterDrop=0|bundleRegistryPreserved=true|freezeImmutable=true|explicitRepair=true");
        }
    }
    static void missingAndRollback()throws Exception{
        try(SQLiteDatabase db=db()){
            DbStore store=new DbStore(db);reset(db);need(!ResearchPlatformStoreR929.schemaReadyReadOnlyR31(db),"missing schema false");need(!freeze(store,"202611040102"),"missing freeze dependency");need(ResearchPlatformStoreR929.SCHEMA_PENDING_R31.equals(store.getState("r932_choice_freeze_status","")),"explicit schema dependency status");need(scalar(db,"SELECT count(*) FROM sqlite_master WHERE name LIKE 'research_%'")==0,"missing business schema not created");need(db.ddlCount==0&&store.coordinator.migrations==0,"missing freeze has no DDL or migration callback");
            db.beginTransaction();try{try{ResearchPlatformStoreR929.activateRuntimeBundle(db,ModelBundleRuntimeR932.current(),"202611040102",100);throw new AssertionError("missing activate accepted");}catch(IllegalStateException expected){need(ResearchPlatformStoreR929.SCHEMA_PENDING_R31.equals(expected.getMessage()),"activate explicit dependency");}}finally{db.endTransaction();}
            tx(db,()->{ResearchPlatformStoreR929.createSchema(db);need(ResearchPlatformStoreR929.schemaReadyReadOnlyR31(db),"uncommitted real schema verified locally");},false);
            reset(db);need(!ResearchPlatformStoreR929.schemaReadyReadOnlyR31(db),"rollback does not publish ready");need(db.ddlCount==0,"rollback readiness remains read only");
            ResearchPlatformStoreR929.ensureSchema(store);need(ResearchPlatformStoreR929.schemaReadyReadOnlyR31(db),"normal explicit recovery after rollback");
            db.beginTransaction();TransactionSchemaCacheR941.Scope scope=TransactionSchemaCacheR941.enter(db);try{db.execSQL("DROP TABLE "+UnifiedResearchChoiceR932.TABLE);reset(db);need(!ResearchPlatformStoreR929.schemaReadyReadOnlyR31(db),"in-transaction DROP invalidates committed cache");need(db.ddlCount==0,"dropped choice not recreated in read check");}finally{db.endTransaction();TransactionSchemaCacheR941.leave(scope);}
            need(ResearchPlatformStoreR929.schemaReadyReadOnlyR31(db),"rolled-back DROP restores actual committed schema");System.out.println("R929_MISSING_ROLLBACK_PASS|missingCreates=0|dependencyExplicit=true|rollbackNoFalseStamp=true");
        }
    }
    static void revision17AndRepair()throws Exception{
        SchemaDomainCoordinatorR926.Contract actual=SchemaDomainCoordinatorR926.researchContract();need(actual.revision==17&&Arrays.asList(actual.objects).contains(UnifiedResearchChoiceR932.TABLE),"actual startup contract revision17 includes choice");
        try(SQLiteDatabase db=db()){
            install(db);SchemaDomainCoordinatorR926.Contract subset=new SchemaDomainCoordinatorR926.Contract("RESEARCH",17,new String[]{ResearchPlatformStoreR929.TABLE_BUNDLE,UnifiedResearchChoiceR932.TABLE});
            SchemaDomainCoordinatorR926.runStep(db,subset,"R932_UNIFIED",x->ResearchPlatformStoreR929.createSchema(x));SchemaDomainCoordinatorR926.verifyAndStamp(db,subset);need(SchemaDomainCoordinatorR926.matches(db,subset),"actual schema-domain stamp fixture");
            db.execSQL("DROP TABLE "+UnifiedResearchChoiceR932.TABLE);reset(db);need(!ResearchPlatformStoreR929.schemaReadyReadOnlyR31(db),"rev17 missing choice is pending");need(db.ddlCount==0,"business readiness leaves choice absent");need(SchemaDomainCoordinatorR926.prepareRepairIfDrift(db,subset),"existing owner re-arms drift journal");need(!SchemaDomainCoordinatorR926.stepComplete(db,subset,"R932_UNIFIED"),"old completed stamp not trusted");
            SchemaDomainCoordinatorR926.runStep(db,subset,"R932_UNIFIED",x->ResearchPlatformStoreR929.createSchema(x));SchemaDomainCoordinatorR926.verifyAndStamp(db,subset);need(ResearchPlatformStoreR929.schemaReadyReadOnlyR31(db),"original owner restores choice");System.out.println("R929_RESEARCH_REV17_REPAIR_PASS|actualContractIncludesChoice=true|existingRepairJournalRearmed=true|fullAndroidAdvance=NOT_RUN");
        }
    }
    public static void main(String[] args)throws Exception{Class.forName("org.sqlite.JDBC");if(args.length>0&&args[0].equals("baseline")){baseline();return;}readyAndDrift();missingAndRollback();revision17AndRepair();System.out.println("R929_SCHEMA_HOST_PASS|assertions="+assertions+"|backend=native-sqlite-jdbc|android=NOT_RUN|notNaturalEvidence=true");}
}
