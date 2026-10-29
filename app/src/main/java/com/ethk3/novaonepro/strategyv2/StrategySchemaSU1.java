package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Incremental journaled migration, called only inside ResearchSchemaCoordinator's writer. */
public final class StrategySchemaSU1 {
    public static final String JOURNAL="su1_migration_unit_v2";
    public static final String STEP="SU1_STORAGE_FOUNDRY_RUNTIME_V1";
    private static final java.util.Map<SQLiteDatabase,Integer> VERIFIED=java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
    private static final String[] JOURNAL_GUARDS={
        "CREATE TRIGGER IF NOT EXISTS su1_migration_v2_no_update BEFORE UPDATE ON su1_migration_unit_v2 BEGIN SELECT RAISE(ABORT,'SU1_MIGRATION_JOURNAL_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS su1_migration_v2_no_delete BEFORE DELETE ON su1_migration_unit_v2 BEGIN SELECT RAISE(ABORT,'SU1_MIGRATION_JOURNAL_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS su1_migration_v2_no_replace BEFORE INSERT ON su1_migration_unit_v2 WHEN EXISTS(SELECT 1 FROM su1_migration_unit_v2 WHERE unit=NEW.unit) BEGIN SELECT RAISE(ABORT,'SU1_MIGRATION_JOURNAL_IMMUTABLE'); END"
    };
    private StrategySchemaSU1() {}
    public static boolean advance(SQLiteDatabase db){
        db.execSQL("CREATE TABLE IF NOT EXISTS su1_migration_unit_v2(unit INTEGER PRIMARY KEY,sql_hash TEXT NOT NULL,installed_at INTEGER NOT NULL)");
        for(String guard:JOURNAL_GUARDS)db.execSQL(guard);
        if(upgradePreview(db))return false;
        int installed;
        try(Cursor c=db.rawQuery("SELECT count(*) FROM su1_migration_unit_v2",null)){c.moveToFirst();installed=c.getInt(0);}
        if(installed>SqlStatementsSU1.SQL.length)throw new IllegalStateException("SU1_MIGRATION_AHEAD");
        // Exactly one complete DDL/DML statement plus its receipt per bounded owner invocation.
        if(installed<SqlStatementsSU1.SQL.length){
            db.beginTransaction();
            try{
                String sql=SqlStatementsSU1.SQL[installed];db.execSQL(sql);
                db.execSQL("INSERT INTO su1_migration_unit_v2 VALUES(?,?,?)",new Object[]{installed,HashingSU1.text(sql),System.currentTimeMillis()});
                db.setTransactionSuccessful();
            }finally{db.endTransaction();}
        }
        return ready(db);
    }
    public static boolean ready(SQLiteDatabase db){
        try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name='su1_migration_unit_v2'",null)){if(!c.moveToFirst())return false;}
        // An incomplete journal can never be READY. Do not rehash its growing prefix on
        // every service quantum; verify every immutable unit and object once it is complete.
        try(Cursor c=db.rawQuery("SELECT count(*) FROM su1_migration_unit_v2",null)){if(!c.moveToFirst()||c.getInt(0)!=SqlStatementsSU1.SQL.length)return false;}
        int version;try(Cursor c=db.rawQuery("PRAGMA schema_version",null)){if(!c.moveToFirst())return false;version=c.getInt(0);}
        Integer known=VERIFIED.get(db);if(known!=null&&known==version)return true;
        try(Cursor c=db.rawQuery("SELECT unit,sql_hash FROM su1_migration_unit_v2 ORDER BY unit",null)){
            int n=0;while(c.moveToNext()){if(n>=SqlStatementsSU1.SQL.length||c.getInt(0)!=n||!HashingSU1.text(SqlStatementsSU1.SQL[n]).equals(c.getString(1)))return false;n++;}
            if(n!=SqlStatementsSU1.SQL.length)return false;
        }catch(android.database.sqlite.SQLiteException absent){return false;}
        for(String sql:JOURNAL_GUARDS)if(!objectMatches(db,sql))return false;
        for(String sql:SqlStatementsSU1.SQL)if(!objectMatches(db,sql))return false;
        VERIFIED.put(db,version);
        return true;
    }
    private static boolean objectMatches(SQLiteDatabase db,String sql){java.util.regex.Matcher m=java.util.regex.Pattern.compile("^CREATE (?:UNIQUE )?(?:TABLE|INDEX|TRIGGER) IF NOT EXISTS ([a-z0-9_]+)").matcher(sql);if(!m.find())return true;try(Cursor c=db.rawQuery("SELECT sql FROM sqlite_master WHERE name=?",new String[]{m.group(1)})){return c.moveToFirst()&&normal(sql).equals(normal(c.getString(0)));}}
    private static boolean column(SQLiteDatabase db,String table,String name){try(Cursor c=db.rawQuery("PRAGMA table_info("+table+")",null)){while(c.moveToNext())if(name.equals(c.getString(1)))return true;return false;}}
    private static boolean upgradePreview(SQLiteDatabase db){
        // Preserve the immutable V1 journal and all its evidence. Each ALTER is one atomic
        // statement. The separate V2 journal validates the complete final schema afterwards.
        if(StrategyRepositorySU1.exists(db,"SELECT 1 FROM sqlite_master WHERE type='table' AND name='su1_runtime_program'")&&!column(db,"su1_runtime_program","kernel_hash")){
            db.execSQL("ALTER TABLE su1_runtime_program ADD COLUMN kernel_hash TEXT NOT NULL DEFAULT '0000000000000000000000000000000000000000000000000000000000000000' CHECK(length(kernel_hash)=64)");return true;
        }
        if(StrategyRepositorySU1.exists(db,"SELECT 1 FROM sqlite_master WHERE type='table' AND name='su1_cohort_runtime'")){
            if(!column(db,"su1_cohort_runtime","state")){db.execSQL("ALTER TABLE su1_cohort_runtime ADD COLUMN state TEXT NOT NULL DEFAULT 'OPEN' CHECK(state IN ('OPEN','STOPPED','CLOSED'))");return true;}
            if(!column(db,"su1_cohort_runtime","stop_reason")){db.execSQL("ALTER TABLE su1_cohort_runtime ADD COLUMN stop_reason TEXT NOT NULL DEFAULT ''");return true;}
            try(Cursor c=db.rawQuery("SELECT sql FROM sqlite_master WHERE name='su1_cohort_runtime_immutable' AND type='trigger'",null)){
                if(c.moveToFirst()&&"73a1240a54fc0d794b2aaeb2893b79509ee8c07c965bb0672a4746b2de509799".equals(HashingSU1.text(normal(c.getString(0))))){db.execSQL("DROP TRIGGER su1_cohort_runtime_immutable");return true;}
            }
        }return false;
    }
    private static String normal(String sql){return sql.replace("DEFAULT '0000000000000000000000000000000000000000000000000000000000000000'","").replace("IF NOT EXISTS ","").replaceAll("\\s+","").replaceAll(";+$","");}
}
