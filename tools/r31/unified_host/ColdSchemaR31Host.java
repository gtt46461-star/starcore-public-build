package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.lang.reflect.*;
import java.sql.*;

/** Invoke the real cold initializer, including its original repair/default SQL.
 * Native SQLite plus Android API adapters; not an Android or handset claim. */
public final class ColdSchemaR31Host {
    static int checks;
    static void need(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
    static long scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){need(c.moveToFirst(),sql);return c.getLong(0);}}
    static DbHelper helper()throws Exception{
        Class<?> type=Class.forName("sun.misc.Unsafe");Field f=type.getDeclaredField("theUnsafe");f.setAccessible(true);
        return (DbHelper)type.getMethod("allocateInstance",Class.class).invoke(f.get(null),DbHelper.class);
    }
    static void initialize(DbHelper helper,SQLiteDatabase db)throws Throwable{
        Method method=DbHelper.class.getDeclaredMethod("ensureSchema",SQLiteDatabase.class);method.setAccessible(true);
        try{method.invoke(helper,db);}catch(InvocationTargetException failed){throw failed.getCause();}
    }
    static final class FaultDb extends SQLiteDatabase {
        String blocked="";
        FaultDb(Connection c){super(c);}
        @Override public void execSQL(String sql){if(!blocked.isEmpty()&&sql.trim().startsWith(blocked))throw new android.database.sqlite.SQLiteException("INJECTED_COLD_SQL_FAILURE");super.execSQL(sql);}
    }
    public static void main(String[] args)throws Throwable{
        Class.forName("org.sqlite.JDBC");
        try(FaultDb db=new FaultDb(DriverManager.getConnection("jdbc:sqlite:"+args[0]))){
            DbHelper helper=helper();initialize(helper,db);
            for(String table:new String[]{"draws","decisions","trades","app_state"})need(scalar(db,"SELECT count(*) FROM sqlite_master WHERE type='table' AND name='"+table+"'")==1,"cold initializer returned without committed "+table);
            need(!db.inTransaction(),"cold transaction actually ended");
            db.execSQL("UPDATE app_state SET state_value='ORIGINAL_USER_SETTING' WHERE state_key='service_status'");
            need(scalar(db,"SELECT count(*) FROM sqlite_master WHERE name='idx_v28_oos_target_candidate'")==1,"replacement candidate index is installed");
            // The old owner explicitly retires just this obsolete index.
            db.execSQL("CREATE INDEX idx_v26_oos_one_action_target ON "+DbHelper.TABLE_OOS_ACTION_LOG+"(target_issue)");
            initialize(helper,db);need(scalar(db,"SELECT count(*) FROM sqlite_master WHERE name='idx_v26_oos_one_action_target'")==0,"original obsolete-index retirement remains executable");
            need(scalar(db,"SELECT count(*) FROM sqlite_master WHERE name='idx_v28_oos_target_candidate'")==1,"current unique candidate index preserved");
            need(scalar(db,"SELECT count(*) FROM app_state WHERE state_key='service_status' AND state_value='ORIGINAL_USER_SETTING'")==1,"default retry preserves the original user setting");
            Method sql=DbHelper.class.getDeclaredMethod("execQuiet",SQLiteDatabase.class,String.class);sql.setAccessible(true);boolean refused=false;
            try{sql.invoke(helper,db,"UPDATE app_state SET state_value='FORBIDDEN' WHERE state_key='service_status'");}catch(InvocationTargetException expected){refused=expected.getCause().getMessage().equals("R31_LEGACY_DML_REQUIRES_COLD_INITIALIZER");}
            need(refused,"historical DML cannot escape its cold owner");
            need(scalar(db,"SELECT count(*) FROM app_state WHERE state_key='service_status' AND state_value='ORIGINAL_USER_SETTING'")==1,"rejected DML performs no mutation");
        }
        int number=0;for(String failure:new String[]{"CREATE TABLE IF NOT EXISTS draws","ALTER TABLE draws ADD COLUMN open_time","UPDATE "})try(FaultDb db=new FaultDb(DriverManager.getConnection("jdbc:sqlite:"+args[0]+"-fault-"+(number++)))){
            DbHelper helper=helper();db.blocked=failure;boolean failed=false;
            try{initialize(helper,db);}catch(android.database.sqlite.SQLiteException expected){failed=true;need(expected.getMessage().equals("INJECTED_COLD_SQL_FAILURE"),"first original failure is preserved");}
            need(failed,"a failed cold transaction cannot report success");need(!db.inTransaction(),"failed transaction releases its connection");
            need(scalar(db,"SELECT count(*) FROM sqlite_master WHERE name='draws'")==0,"failed initial schema is rolled back");
            db.blocked="";initialize(helper,db);need(scalar(db,"SELECT count(*) FROM sqlite_master WHERE name='draws'")==1,"same initializer resumes after the actual fault is removed");
        }
        System.out.println("COLD_SCHEMA_NATIVE_PASS assertions="+checks+" actualColdInitializer=true firstFailurePreserved=true rollbackThenRetry=true scope=HOST_NOT_ANDROID");
    }
}
