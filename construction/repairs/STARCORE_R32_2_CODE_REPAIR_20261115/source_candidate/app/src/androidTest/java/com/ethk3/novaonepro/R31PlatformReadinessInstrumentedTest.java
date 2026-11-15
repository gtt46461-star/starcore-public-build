package com.ethk3.novaonepro;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ethk3.novaonepro.strategyv2.JsonR27;
import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Real ART/SQLite metadata and normal startup owner; engineered fixtures are not quality evidence. */
@RunWith(AndroidJUnit4.class)
public final class R31PlatformReadinessInstrumentedTest {
    static Context app()throws Exception{
        assertTrue(InstrumentationRegistry.getInstrumentation() instanceof R30FaultAndroidRunner);
        Context app=ApplicationProvider.getApplicationContext();
        assertTrue("isolated routed database required",app.getDatabasePath(AppConfig.DB_NAME).getCanonicalPath().contains("/r30-instrumentation/"));
        return app;
    }
    static long n(SQLiteDatabase db,String sql){try(Cursor q=db.rawQuery(sql,null)){assertTrue(q.moveToFirst());return q.getLong(0);}}
    static void missing(SQLiteDatabase db,String expected){Map<String,Object> report=ResearchPlatformStoreR929.schemaReadinessReportR31(db);assertEquals(Boolean.FALSE,report.get("ready"));assertTrue(report.toString(),report.get("issues").toString().contains(expected));try{ResearchPlatformStoreR929.requireSchemaReadOnlyR31(db);fail("bad schema accepted");}catch(ResearchPlatformStoreR929.SchemaNotReadyR31 pending){assertEquals("DEPENDENCY",pending.failureCategory());assertTrue(pending.toString(),pending.readiness.toString().contains(expected));}}
    static void explicitFixtureMigration(SQLiteDatabase db){db.beginTransaction();try{ResearchPlatformStoreR929.createSchema(db);db.setTransactionSuccessful();}finally{db.endTransaction();}}
    @Test public void normalFirstStoreAndBundleCaptureHaveARealStartupOwner()throws Exception{
        Context app=app();DbStore store=new DbStore(app);
        try{
            SQLiteDatabase db=store.readable();assertEquals(app.getDatabasePath(AppConfig.DB_NAME).getCanonicalPath(),new File(db.getPath()).getCanonicalPath());
            Field field=ModelBundleRuntimeR932.class.getDeclaredField("filesRoot");field.setAccessible(true);assertEquals(app.getFilesDir().getCanonicalPath(),((File)field.get(null)).getCanonicalPath());
            Map<String,Object> report=ResearchPlatformStoreR929.schemaReadinessReportR31(db);assertEquals(report.toString(),Boolean.TRUE,report.get("ready"));
            assertTrue(SchemaDomainCoordinatorR926.matches(db,SchemaDomainCoordinatorR926.r31PlatformPrerequisitesContract()));
            assertEquals("small prerequisite must not certify all Research",0,n(db,"SELECT count(*) FROM "+SchemaDomainCoordinatorR926.TABLE_DOMAIN+" WHERE domain='RESEARCH' AND state='READY'"));
            String target="202611041001",beforeTarget=ModelBundleRuntimeR932.currentTarget();String hash;
            try(ModelBundleRuntimeR932.Scope scope=ModelBundleRuntimeR932.open(store,target,System.currentTimeMillis())){hash=ModelBundleRuntimeR932.current().hash;assertEquals(target,ModelBundleRuntimeR932.currentTarget());assertEquals(target,store.getState("r932_bundle_capture_issue",""));}
            assertEquals(beforeTarget,ModelBundleRuntimeR932.currentTarget());String captured=store.getState("r932_bundle_capture_hash","");
            try(ModelBundleRuntimeR932.Scope scope=ModelBundleRuntimeR932.open(store,target,System.currentTimeMillis())){assertEquals(hash,ModelBundleRuntimeR932.current().hash);assertEquals(captured,store.getState("r932_bundle_capture_hash",""));}
            assertEquals("one active verified bundle remains",1,n(db,"SELECT count(*) FROM "+ResearchPlatformStoreR929.TABLE_BUNDLE+" WHERE state='ACTIVE'"));
            long schema=n(db,"PRAGMA schema_version");ResearchSchemaCoordinatorR926.ensureRuntimeBundlePrerequisitesR31(store.coordinatorForResearchR952());assertEquals("ready startup is metadata-only",schema,n(db,"PRAGMA schema_version"));
            System.out.println("R31_PLATFORM_NORMAL_STARTUP|db="+db.getPath()+"|context="+app.getDatabasePath(AppConfig.DB_NAME)+"|pid="+android.os.Process.myPid()+"|metadata="+JsonR27.write(report)+"|ACTUAL_STORE_DOMAIN_CAPTURE_REPEAT_PASS");
        }finally{store.close();}
    }
    @Test public void missingAndDriftMetadataNeverCreateOrWeakenTheContract()throws Exception{
        app();try(SQLiteDatabase db=SQLiteDatabase.create(null)){
            long before=n(db,"PRAGMA schema_version"),objects=n(db,"SELECT count(*) FROM sqlite_master");missing(db,"MISSING_TABLE:research_trial_registry_r929");assertEquals(before,n(db,"PRAGMA schema_version"));assertEquals(objects,n(db,"SELECT count(*) FROM sqlite_master"));
            explicitFixtureMigration(db);assertTrue(ResearchPlatformStoreR929.schemaReadyReadOnlyR31(db));
            db.beginTransaction();try{db.execSQL("DROP INDEX idx_r929_bundle_one_active");missing(db,"MISSING_INDEX:idx_r929_bundle_one_active");}finally{db.endTransaction();}assertTrue("rolled-back schema drift cannot poison/publish readiness",ResearchPlatformStoreR929.schemaReadyReadOnlyR31(db));
            db.execSQL("DROP INDEX idx_r929_bundle_one_active");before=n(db,"PRAGMA schema_version");missing(db,"MISSING_INDEX:idx_r929_bundle_one_active");assertEquals(before,n(db,"PRAGMA schema_version"));
            explicitFixtureMigration(db);assertTrue(ResearchPlatformStoreR929.schemaReadyReadOnlyR31(db));
            String original;try(Cursor q=db.rawQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name=?",new String[]{ResearchPlatformStoreR929.TABLE_CERTIFICATE})){assertTrue(q.moveToFirst());original=q.getString(0);}
            String noRevision=original.replace(",contract_revision TEXT NOT NULL DEFAULT ''","");assertNotEquals(original,noRevision);db.execSQL("DROP TABLE "+ResearchPlatformStoreR929.TABLE_CERTIFICATE);db.execSQL(noRevision);missing(db,"MISSING_COLUMN:research_certificate_r929:contract_revision");explicitFixtureMigration(db);assertTrue(ResearchPlatformStoreR929.schemaReadyReadOnlyR31(db));
            String sql;try(Cursor q=db.rawQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name=?",new String[]{ResearchPlatformStoreR929.TABLE_CERTIFICATE})){assertTrue(q.moveToFirst());sql=q.getString(0);}
            String broken=sql.replace("certificate_hash TEXT NOT NULL UNIQUE","certificate_hash TEXT NOT NULL");assertNotEquals(sql,broken);db.execSQL("DROP TABLE "+ResearchPlatformStoreR929.TABLE_CERTIFICATE);db.execSQL(broken);db.execSQL("CREATE UNIQUE INDEX partial_certificate_hash_only ON "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+"(certificate_hash) WHERE state='ACTIVE'");
            missing(db,"GLOBAL_UNIQUE_NOT_PROVEN:research_certificate_r929:certificate_hash");
            String noPk=original.replace("candidate_key TEXT PRIMARY KEY NOT NULL","candidate_key TEXT NOT NULL");assertNotEquals(original,noPk);db.execSQL("DROP TABLE "+ResearchPlatformStoreR929.TABLE_CERTIFICATE);db.execSQL(noPk);missing(db,"PRIMARY_KEY_MISMATCH:research_certificate_r929");
            System.out.println("R31_PLATFORM_NEGATIVE_CONTROLS|MISSING_COLUMN_PK_INDEX_GLOBAL_UNIQUE_AND_ROLLBACK_CACHE|NO_RUNTIME_REPAIR|PASS");
        }
    }
    @Test public void failedMigrationRollsBackAndOriginalOwnerCanRetry()throws Exception{
        app();try(SQLiteDatabase db=SQLiteDatabase.create(null)){
            db.beginTransaction();try{SchemaDomainCoordinatorR926.migrateR31PlatformPrerequisites(db);assertTrue(ResearchPlatformStoreR929.schemaReadyReadOnlyR31(db));}finally{db.endTransaction();}
            assertFalse(ResearchPlatformStoreR929.schemaReadyReadOnlyR31(db));assertEquals(0,n(db,"SELECT count(*) FROM sqlite_master WHERE name='research_model_bundle_r929'"));
            db.beginTransaction();try{SchemaDomainCoordinatorR926.migrateR31PlatformPrerequisites(db);db.setTransactionSuccessful();}finally{db.endTransaction();}
            assertTrue(ResearchPlatformStoreR929.schemaReadyReadOnlyR31(db));assertTrue(SchemaDomainCoordinatorR926.matches(db,SchemaDomainCoordinatorR926.r31PlatformPrerequisitesContract()));assertEquals(0,n(db,"SELECT count(*) FROM "+SchemaDomainCoordinatorR926.TABLE_DOMAIN+" WHERE domain='RESEARCH' AND state='READY'"));
            System.out.println("R31_PLATFORM_ROLLBACK_RETRY|ACTUAL_DOMAIN_OWNER|NO_FALSE_READY|PASS");
        }
    }
}
