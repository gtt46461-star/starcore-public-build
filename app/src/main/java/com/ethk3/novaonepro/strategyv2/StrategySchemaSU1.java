package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Incremental journaled migration, called only inside ResearchSchemaCoordinator's writer. */
public final class StrategySchemaSU1 {
    public static final String STEP="SU1_STORAGE_FOUNDRY_RUNTIME_V1";
    private static final java.util.Map<SQLiteDatabase,Integer> VERIFIED=java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
    private StrategySchemaSU1() {}
    public static boolean advance(SQLiteDatabase db){
        db.execSQL("CREATE TABLE IF NOT EXISTS su1_migration_unit(unit INTEGER PRIMARY KEY,sql_hash TEXT NOT NULL,installed_at INTEGER NOT NULL)");
        int installed;
        try(Cursor c=db.rawQuery("SELECT count(*) FROM su1_migration_unit",null)){c.moveToFirst();installed=c.getInt(0);}
        if(installed>SqlStatementsSU1.SQL.length)throw new IllegalStateException("SU1_MIGRATION_AHEAD");
        // Exactly one complete DDL/DML statement plus its receipt per bounded owner invocation.
        if(installed<SqlStatementsSU1.SQL.length){
            db.beginTransaction();
            try{
                String sql=SqlStatementsSU1.SQL[installed];db.execSQL(sql);
                db.execSQL("INSERT INTO su1_migration_unit VALUES(?,?,?)",new Object[]{installed,HashingSU1.text(sql),System.currentTimeMillis()});
                db.setTransactionSuccessful();
            }finally{db.endTransaction();}
        }
        return ready(db);
    }
    public static boolean ready(SQLiteDatabase db){
        int version;
        try(Cursor c=db.rawQuery("PRAGMA schema_version",null)){if(!c.moveToFirst())return false;version=c.getInt(0);}
        Integer known=VERIFIED.get(db);if(known!=null&&known==version)return true;
        try(Cursor c=db.rawQuery("SELECT unit,sql_hash FROM su1_migration_unit ORDER BY unit",null)){
            int n=0;while(c.moveToNext()){if(n>=SqlStatementsSU1.SQL.length||c.getInt(0)!=n||!HashingSU1.text(SqlStatementsSU1.SQL[n]).equals(c.getString(1)))return false;n++;}
            if(n!=SqlStatementsSU1.SQL.length)return false;
        }catch(android.database.sqlite.SQLiteException absent){return false;}
        for(String sql:SqlStatementsSU1.SQL){
            java.util.regex.Matcher m=java.util.regex.Pattern.compile("^CREATE (?:UNIQUE )?(?:TABLE|INDEX|TRIGGER) IF NOT EXISTS ([a-z0-9_]+)").matcher(sql);
            if(!m.find())continue;
            try(Cursor c=db.rawQuery("SELECT sql FROM sqlite_master WHERE name=?",new String[]{m.group(1)})){if(!c.moveToFirst()||!normal(sql).equals(normal(c.getString(0))))return false;}
        }
        VERIFIED.put(db,version);
        return true;
    }
    private static String normal(String sql){return sql.replace("IF NOT EXISTS ","").replaceAll("\\s+","").replaceAll(";+$","");}
}
