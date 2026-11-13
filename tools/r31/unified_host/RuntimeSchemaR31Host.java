package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;

/** Actual DbStore schema units, with constructor-only Android dependencies
 * supplied by the native platform adapter. No replacement migration logic. */
public final class RuntimeSchemaR31Host {
    static void set(Object owner,String name,Object value)throws Exception{Field field=owner.getClass().getDeclaredField(name);field.setAccessible(true);field.set(owner,value);}
    static long scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){if(!c.moveToFirst())throw new AssertionError(sql);return c.getLong(0);}}
    static int mutations(int from){int n=0;for(String call:SQLiteDatabase.trace.subList(from,SQLiteDatabase.trace.size()))if(call.matches("(?is).*\\t(?:execSQL|insert|update|delete)\\t\\s*(?:INSERT|UPDATE|DELETE|REPLACE).*"))n++;return n;}
    public static void main(String[] args)throws Throwable{
        Class.forName("org.sqlite.JDBC");
        try(SQLiteDatabase db=new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite:"+args[0]))){
            DbHelper helper=ColdSchemaR31Host.helper();ColdSchemaR31Host.initialize(helper,db);SchemaDomainCoordinatorR926.ensureInfrastructure(db);
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
            System.out.println("ACTUAL_DBSTORE_RUNTIME_SCHEMA_NATIVE_PASS units="+units.size()+" slices="+slices+" readOnlyAuditDml=0 criticalCommitsBetweenSlices="+(slices-1)+" scope=HOST_NOT_ANDROID");
        }
    }
}
