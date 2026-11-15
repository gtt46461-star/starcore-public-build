package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.strategyv2.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

/** Baseline actual production startup/schema callbacks on native SQLite, not a device result. */
public final class R929StartupOrderR31Host {
    static int assertions;static void need(boolean ok,String why){assertions++;if(!ok)throw new AssertionError(why);}
    static void call(Class<?> type,String name,SQLiteDatabase db)throws Exception{Method m=type.getDeclaredMethod(name,SQLiteDatabase.class);m.setAccessible(true);try{m.invoke(null,db);}catch(InvocationTargetException error){Throwable cause=error.getCause();if(cause instanceof Error)throw(Error)cause;if(cause instanceof Exception)throw(Exception)cause;throw error;}}
    static Map<String,Object> metadata(SQLiteDatabase db)throws Exception{
        Map<String,Object> out=new LinkedHashMap<>();List<Object> objects=new ArrayList<>();
        for(String field:new String[]{"R31_TABLE_COLUMNS","R31_INDEXES"}){Field f=ResearchPlatformStoreR929.class.getDeclaredField(field);f.setAccessible(true);for(String[] required:(String[][])f.get(null)){Map<String,Object> row=new LinkedHashMap<>();row.put("name",required[0]);try(Cursor q=db.rawQuery("SELECT type,sql FROM sqlite_master WHERE name=?",new String[]{required[0]})){if(q.moveToFirst()){row.put("type",q.getString(0));row.put("sql",q.getString(1));}else row.put("type","MISSING");}if(field.equals("R31_TABLE_COLUMNS")){List<String> columns=new ArrayList<>();Map<String,Integer> primary=new LinkedHashMap<>();try(Cursor q=db.rawQuery("PRAGMA table_info("+required[0]+")",null)){while(q.moveToNext()){columns.add(q.getString(1));if(q.getInt(5)>0)primary.put(q.getString(1),q.getInt(5));}}row.put("actualColumns",columns);row.put("actualPk",primary);row.put("requiredColumns",required[1]);row.put("requiredPk",required[2]);}objects.add(row);}}
        out.put("requiredObjects",objects);out.put("productionReady",ResearchPlatformStoreR929.schemaReadyReadOnlyR31(db));try(Cursor q=db.rawQuery("PRAGMA schema_version",null)){need(q.moveToFirst(),"schema_version");out.put("schemaVersion",q.getLong(0));}return out;
    }
    static void tx(SQLiteDatabase db,Throwing body)throws Exception{db.beginTransaction();try{body.run();db.setTransactionSuccessful();}finally{db.endTransaction();}}
    interface Throwing{void run()throws Exception;}
    public static void main(String[] args)throws Exception{
        Path file=Paths.get(args[0]);MainChainSchemaR31Host.Owner owner=new MainChainSchemaR31Host.Owner(file);Map<String,Object> result=new LinkedHashMap<>();
        try{
            MainChainSchemaR31Host.createLegacyBase(owner);SchemaDomainCoordinatorR926.verifyObjects(owner.db,SchemaDomainCoordinatorR926.criticalContract());result.put("afterActualCriticalPredecessors",metadata(owner.db));System.out.println(JsonR27.write(Map.of("stage","ACTUAL_CRITICAL_PREDECESSORS","metadata",result.get("afterActualCriticalPredecessors"))));
            SQLiteDatabase.stage="ACTUAL_FAULT_FIXTURE_SEED";
            tx(owner.db,()->{RuntimeSchemaR26.ensure(owner.db);for(String sql:SqlStatementsSU1.SQL)owner.db.execSQL(sql);StrategySchemaSU2.ensure(owner.db);SchemaR27.ensure(owner.db);owner.db.execSQL("CREATE TABLE IF NOT EXISTS r30_fault_disk(id INTEGER PRIMARY KEY,payload BLOB)");});
            result.put("afterActualR30FaultSeedSchema",metadata(owner.db));System.out.println(JsonR27.write(Map.of("stage","ACTUAL_R30_FAULT_SEED_SCHEMA","metadata",result.get("afterActualR30FaultSeedSchema"))));need(!ResearchPlatformStoreR929.schemaReadyReadOnlyR31(owner.db),"baseline seed expected to reproduce source readiness failure");
            SQLiteDatabase.stage="BASELINE_BUSINESS_CAPTURE";long ddlBefore=SQLiteDatabase.trace.stream().filter(s->s.contains("\texecSQL\tCREATE")||s.contains("\texecSQL\tALTER")).count();
            try{tx(owner.db,()->ResearchPlatformStoreR929.activateRuntimeBundle(owner.db,ModelBundleRuntimeR932.current(),"202610310241",100));throw new AssertionError("baseline capture unexpectedly accepted");}catch(IllegalStateException failure){need(ResearchPlatformStoreR929.SCHEMA_PENDING_R31.equals(failure.getMessage()),"actual pending cause:"+failure);result.put("actualCaptureFailure",failure.toString());}
            long ddlAfter=SQLiteDatabase.trace.stream().filter(s->s.contains("\texecSQL\tCREATE")||s.contains("\texecSQL\tALTER")).count();need(ddlBefore==ddlAfter,"business failure must not create schema");
            SQLiteDatabase.stage="ACTUAL_RESEARCH_R932_UNIFIED_CALLBACK";tx(owner.db,()->call(ResearchSchemaCoordinatorR926.class,"ensureUnifiedResearchSchema",owner.db));result.put("afterActualResearchOwnerCallback",metadata(owner.db));need(ResearchPlatformStoreR929.schemaReadyReadOnlyR31(owner.db),"actual research owner establishes strict contract");
            tx(owner.db,()->ResearchPlatformStoreR929.activateRuntimeBundle(owner.db,ModelBundleRuntimeR932.current(),"202610310241",100));result.put("captureAfterActualOwner","PASS");result.put("status","BASELINE_STARTUP_GAP_REPRODUCED_ACTUAL_OWNER_RECOVERS");result.put("assertions",assertions);result.put("scope","ACTUAL_CRITICAL_AND_R30_FIXTURE_SCHEMA_AND_RESEARCH_OWNER_NATIVE_SQLITE_NOT_ANDROID_BINDING_OR_DEVICE");System.out.println(JsonR27.write(result));
        }finally{Files.write(Paths.get(args[1]),SQLiteDatabase.trace);owner.db.close();}
    }
}
