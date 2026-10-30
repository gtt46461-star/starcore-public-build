package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Incremental journaled migration, called only inside ResearchSchemaCoordinator's writer. */
public final class StrategySchemaSU1 {
    public static final String JOURNAL="su1_migration_unit_v2";
    public static final String STEP="SU1_STORAGE_FOUNDRY_RUNTIME_V1";
    private static final String JOURNAL_DDL="CREATE TABLE IF NOT EXISTS su1_migration_unit_v2(unit INTEGER PRIMARY KEY,sql_hash TEXT NOT NULL,installed_at INTEGER NOT NULL)";
    private static final java.util.regex.Pattern CREATE_OBJECT=java.util.regex.Pattern.compile("^CREATE (?:UNIQUE )?(TABLE|INDEX|TRIGGER) IF NOT EXISTS ([a-z0-9_]+)");
    private static final java.util.regex.Pattern SPACE=java.util.regex.Pattern.compile("\\s+");
    private static final java.util.regex.Pattern TRAILING=java.util.regex.Pattern.compile(";+$");
    private static final String[] JOURNAL_GUARDS={
        "CREATE TRIGGER IF NOT EXISTS su1_migration_v2_no_update BEFORE UPDATE ON su1_migration_unit_v2 BEGIN SELECT RAISE(ABORT,'SU1_MIGRATION_JOURNAL_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS su1_migration_v2_no_delete BEFORE DELETE ON su1_migration_unit_v2 BEGIN SELECT RAISE(ABORT,'SU1_MIGRATION_JOURNAL_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS su1_migration_v2_no_replace BEFORE INSERT ON su1_migration_unit_v2 WHEN EXISTS(SELECT 1 FROM su1_migration_unit_v2 WHERE unit=NEW.unit) BEGIN SELECT RAISE(ABORT,'SU1_MIGRATION_JOURNAL_IMMUTABLE'); END"
    };
    private StrategySchemaSU1() {}
    public static boolean advance(SQLiteDatabase db){
        boolean research=ResearchRouteR29.isResearchDatabase(db);
        StrategySchemaDomainR29.prepareResearch(db,research);
        ensureJournal(db);
        if(upgradePreview(db))return false;
        int installed;
        try(Cursor c=db.rawQuery("SELECT count(*) FROM su1_migration_unit_v2",null)){c.moveToFirst();installed=c.getInt(0);}
        if(installed>SqlStatementsSU1.SQL.length)throw new IllegalStateException("SU1_MIGRATION_AHEAD");
        // Exactly one complete DDL/DML statement plus its receipt per bounded owner invocation.
        if(installed<SqlStatementsSU1.SQL.length){
            db.beginTransaction();
            try{
                String sql=SqlStatementsSU1.SQL[installed];Proof proof=SQL_PROOFS[installed];
                if(proof==null||!StrategySchemaDomainR29.requiredHere(research,sql)||!proof.exists(db))StrategySchemaDomainR29.execute(db,sql,"SU1_STORAGE");
                db.execSQL("INSERT INTO su1_migration_unit_v2 VALUES(?,?,?)",new Object[]{installed,SQL_HASHES[installed],System.currentTimeMillis()});
                db.setTransactionSuccessful();
            }finally{db.endTransaction();}
            // This successful, immutable insertion still leaves an incomplete journal. ready()
            // would return false from that same count; final-unit and subsequent READY calls
            // retain the full current journal/object/domain proof without a stored readiness flag.
            if(installed+1<SqlStatementsSU1.SQL.length)return false;
        }
        return ready(db);
    }
    public static boolean ready(SQLiteDatabase db){
        boolean research=ResearchRouteR29.isResearchDatabase(db);
        if(!StrategySchemaDomainR29.domainReady(db,research))return false;
        try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name='su1_migration_unit_v2'",null)){if(!c.moveToFirst())return false;}
        // An incomplete journal can never be READY. Do not rehash its growing prefix on
        // every service quantum; verify every immutable unit and object once it is complete.
        try(Cursor c=db.rawQuery("SELECT count(*) FROM su1_migration_unit_v2",null)){if(!c.moveToFirst()||c.getInt(0)!=SqlStatementsSU1.SQL.length)return false;}
        try(Cursor c=db.rawQuery("SELECT unit,sql_hash FROM su1_migration_unit_v2 ORDER BY unit",null)){
            int n=0;while(c.moveToNext()){if(n>=SqlStatementsSU1.SQL.length||c.getInt(0)!=n||!SQL_HASHES[n].equals(c.getString(1)))return false;n++;}
            if(n!=SqlStatementsSU1.SQL.length)return false;
        }catch(android.database.sqlite.SQLiteException absent){return false;}
        // A fresh single census belongs to this invocation's SQLite snapshot. schema_version/READY caches
        // cannot prove guards after rollback/ABA; no completed prefix causes hundreds of tiny pool leases.
        java.util.Map<String,String[]> actual=objects(db,false);
        if(!JOURNAL_PROOF.matches(actual.get(JOURNAL_PROOF.name)))return false;
        for(Proof guard:GUARD_PROOFS)if(!guard.matches(actual.get(guard.name)))return false;
        for(int i=0;i<SqlStatementsSU1.SQL.length;i++){
            Proof proof=SQL_PROOFS[i];
            if(proof!=null&&StrategySchemaDomainR29.requiredHere(research,SqlStatementsSU1.SQL[i])&&!proof.matches(actual.get(proof.name)))return false;
        }
        return true;
    }
    static final class Proof {
        final String name,type,sql;
        Proof(String source){java.util.regex.Matcher match=CREATE_OBJECT.matcher(source);if(!match.find())throw new IllegalArgumentException("SU1_SCHEMA_OBJECT_DDL");type=match.group(1).toLowerCase(java.util.Locale.ROOT);name=match.group(2);sql=normal(source);}
        boolean matches(String[] actual){return actual!=null&&type.equals(actual[0])&&sql.equals(normal(actual[1]));}
        boolean exists(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT type,sql FROM main.sqlite_master WHERE name=?",new String[]{name})){return c.moveToFirst()&&matches(new String[]{c.getString(0),c.getString(1)})&&!c.moveToNext();}}
    }
    private static final String[] SQL_HASHES=new String[SqlStatementsSU1.SQL.length];
    private static final Proof[] SQL_PROOFS=new Proof[SqlStatementsSU1.SQL.length],GUARD_PROOFS=new Proof[JOURNAL_GUARDS.length];
    private static final Proof JOURNAL_PROOF=new Proof(JOURNAL_DDL);
    private static final String[] OBJECT_NAMES;
    private static final String OBJECT_QUERY;
    static {
        for(int i=0;i<SqlStatementsSU1.SQL.length;i++){SQL_HASHES[i]=HashingSU1.text(SqlStatementsSU1.SQL[i]);if(CREATE_OBJECT.matcher(SqlStatementsSU1.SQL[i]).find())SQL_PROOFS[i]=new Proof(SqlStatementsSU1.SQL[i]);}
        for(int i=0;i<JOURNAL_GUARDS.length;i++)GUARD_PROOFS[i]=new Proof(JOURNAL_GUARDS[i]);
        java.util.Set<String> names=new java.util.TreeSet<>();names.add(JOURNAL_PROOF.name);for(Proof p:GUARD_PROOFS)names.add(p.name);for(Proof p:SQL_PROOFS)if(p!=null)names.add(p.name);OBJECT_NAMES=names.toArray(new String[0]);
        StringBuilder query=new StringBuilder("SELECT name,type,sql FROM main.sqlite_master WHERE name IN (");for(int i=0;i<OBJECT_NAMES.length;i++){if(i>0)query.append(',');query.append('?');}OBJECT_QUERY=query.append(')').toString();
    }
    private static java.util.Map<String,String[]> objects(SQLiteDatabase db,boolean journalOnly){
        java.util.Map<String,String[]> found=new java.util.HashMap<>();
        try(Cursor c=db.rawQuery(journalOnly?"SELECT name,type,sql FROM main.sqlite_master WHERE name IN ('su1_migration_unit_v2','su1_migration_v2_no_update','su1_migration_v2_no_delete','su1_migration_v2_no_replace')":OBJECT_QUERY,journalOnly?null:OBJECT_NAMES)){
            while(c.moveToNext())found.put(c.getString(0),new String[]{c.getString(1),c.getString(2)});
        }return found;
    }
    private static void ensureJournal(SQLiteDatabase db){
        java.util.Map<String,String[]> actual=objects(db,true);boolean installed=false;
        if(actual.containsKey(JOURNAL_PROOF.name)){
            if(!JOURNAL_PROOF.matches(actual.get(JOURNAL_PROOF.name)))throw new IllegalStateException("SU1_MIGRATION_JOURNAL_SCHEMA_MISMATCH");
        }else{db.execSQL(JOURNAL_DDL);installed=true;}
        for(int i=0;i<GUARD_PROOFS.length;i++){
            Proof proof=GUARD_PROOFS[i];if(actual.containsKey(proof.name)){
                if(!proof.matches(actual.get(proof.name)))throw new IllegalStateException("SU1_MIGRATION_JOURNAL_GUARD_MISMATCH:"+proof.name);
            }else{db.execSQL(JOURNAL_GUARDS[i]);installed=true;}
        }
        if(installed){actual=objects(db,true);if(!JOURNAL_PROOF.matches(actual.get(JOURNAL_PROOF.name)))throw new IllegalStateException("SU1_MIGRATION_JOURNAL_INSTALL_FAILED");for(Proof proof:GUARD_PROOFS)if(!proof.matches(actual.get(proof.name)))throw new IllegalStateException("SU1_MIGRATION_JOURNAL_GUARD_INSTALL_FAILED:"+proof.name);}
    }
    private static boolean column(SQLiteDatabase db,String table,String name){try(Cursor c=db.rawQuery("PRAGMA table_info("+table+")",null)){while(c.moveToNext())if(name.equals(c.getString(1)))return true;return false;}}
    private static boolean upgradePreview(SQLiteDatabase db){
        // Preserve the immutable V1 journal and all its evidence. Each ALTER is one atomic
        // statement. The separate V2 journal validates the complete final schema afterwards.
        java.util.Set<String> tables=new java.util.HashSet<>();String cohortGuard=null;
        try(Cursor c=db.rawQuery("SELECT name,type,sql FROM main.sqlite_master WHERE name IN ('su1_runtime_program','su1_cohort_runtime','su1_cohort_runtime_immutable')",null)){
            while(c.moveToNext()){
                if("table".equals(c.getString(1)))tables.add(c.getString(0));
                else if("su1_cohort_runtime_immutable".equals(c.getString(0))&&"trigger".equals(c.getString(1)))cohortGuard=c.getString(2);
            }
        }
        if(tables.contains("su1_runtime_program")&&!column(db,"su1_runtime_program","kernel_hash")){
            db.execSQL("ALTER TABLE su1_runtime_program ADD COLUMN kernel_hash TEXT NOT NULL DEFAULT '0000000000000000000000000000000000000000000000000000000000000000' CHECK(length(kernel_hash)=64)");return true;
        }
        if(tables.contains("su1_cohort_runtime")){
            java.util.Set<String> columns=new java.util.HashSet<>();try(Cursor c=db.rawQuery("PRAGMA table_info(su1_cohort_runtime)",null)){while(c.moveToNext())columns.add(c.getString(1));}
            if(!columns.contains("state")){db.execSQL("ALTER TABLE su1_cohort_runtime ADD COLUMN state TEXT NOT NULL DEFAULT 'OPEN' CHECK(state IN ('OPEN','STOPPED','CLOSED'))");return true;}
            if(!columns.contains("stop_reason")){db.execSQL("ALTER TABLE su1_cohort_runtime ADD COLUMN stop_reason TEXT NOT NULL DEFAULT ''");return true;}
            if(cohortGuard!=null&&"73a1240a54fc0d794b2aaeb2893b79509ee8c07c965bb0672a4746b2de509799".equals(HashingSU1.text(normal(cohortGuard)))){db.execSQL("DROP TRIGGER su1_cohort_runtime_immutable");return true;}
        }return false;
    }
    private static String normal(String sql){if(sql==null)return "";return TRAILING.matcher(SPACE.matcher(sql.replace("DEFAULT '0000000000000000000000000000000000000000000000000000000000000000'","").replace("IF NOT EXISTS ","")).replaceAll("")).replaceAll("");}
}
