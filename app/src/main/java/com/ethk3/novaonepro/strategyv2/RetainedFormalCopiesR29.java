package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DbStore;
import com.ethk3.novaonepro.LegacyLearningCommitR29;
import java.util.*;

/** Quarantines pre-fix FULL-RP copies of Root formal receipts without deleting a row or feeding them back to
 * Root. Only main physical tables are considered; combined-reader TEMP authority views are never archived.
 * Rename, full typed verification and immutable audit share one RP transaction. */
public final class RetainedFormalCopiesR29 {
    private RetainedFormalCopiesR29(){}
    public static final String AUDIT="r29_retained_formal_copy";
    public static final Set<String> SOURCES=Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList("r974_exact_real_receipt","research_exact_probability_f05")));
    public static String retainedName(String source){if(!SOURCES.contains(source))throw new IllegalArgumentException("R29_UNKNOWN_FORMAL_COPY");return "r29_retained_root_"+source;}
    static String q(String s){return MigrationCopyR29.id(s);}
    static long n(SQLiteDatabase db,String sql){return MigrationCopyR29.scalar(db,sql,null);}
    static String tableSql(SQLiteDatabase db,String name){try(Cursor c=db.rawQuery("SELECT sql FROM main.sqlite_master WHERE type='table' AND name=?",new String[]{name})){return c.moveToFirst()?c.getString(0):null;}}
    static boolean object(SQLiteDatabase db,String name){try(Cursor c=db.rawQuery("SELECT 1 FROM main.sqlite_master WHERE name=?",new String[]{name})){return c.moveToFirst();}}
    static boolean copies(SQLiteDatabase db){for(String source:SOURCES)if(tableSql(db,source)!=null)return true;return false;}
    static void owner(SQLiteDatabase db){
        if(!ResearchRouteR29.isResearchDatabase(db)||tableSql(db,"r29_database_route")!=null)throw new IllegalStateException("R29_RETAINED_FORMAL_REQUIRES_RESEARCH_OWNER");
        try(Cursor c=db.rawQuery("SELECT phase,epoch FROM main.r29_research_owner WHERE singleton=1",null)){if(!c.moveToFirst()||c.getLong(1)<=0||!("READY".equals(c.getString(0))||"COPYING".equals(c.getString(0))))throw new IllegalStateException("R29_RETAINED_FORMAL_REQUIRES_RESEARCH_OWNER");}
        // ResearchRows uses the supplied physical connection. With no attached domain or same-name TEMP
        // shadow, every content lookup is necessarily main's physical table, never a Root authority view.
        try(Cursor c=db.rawQuery("PRAGMA database_list",null)){while(c.moveToNext())if(!"main".equals(c.getString(1))&&!"temp".equals(c.getString(1)))throw new IllegalStateException("R29_RETAINED_FORMAL_REQUIRES_PHYSICAL_RP_WRITER");}
        for(String source:SOURCES)try(Cursor c=db.rawQuery("SELECT 1 FROM temp.sqlite_master WHERE name IN(?,?)",new String[]{source,retainedName(source)})){if(c.moveToFirst())throw new IllegalStateException("R29_RETAINED_FORMAL_TEMP_SHADOW_FORBIDDEN");}
    }
    static final class Content{long count,bytes;String hash;Content(long count,long bytes,String hash){this.count=count;this.bytes=bytes;this.hash=hash;}}
    static Content content(SQLiteDatabase db,MigrationCopyR29.Table table,long deadline){
        String chain=StatsR27.sha256("R29_RETAINED_FORMAL_TYPED_ROWS_1|"+JsonR27.write(table.columns));long count=0,bytes=0;Long after=null;
        Runnable check=()->{if(Thread.currentThread().isInterrupted()||System.nanoTime()>deadline)throw new IllegalStateException("R29_RETAINED_FORMAL_BUDGET_OR_CANCELLED");};
        for(;;){check.run();List<MigrationCopyR29.Row> rows=ResearchRowsR29.readPage(db,table,after,"1=1",32,2L<<20,Long.MAX_VALUE,check);if(rows.isEmpty())break;
            for(MigrationCopyR29.Row row:rows){bytes+=row.payload.length;if(bytes>(64L<<20))throw new IllegalStateException("R29_RETAINED_FORMAL_SCAN_BUDGET");chain=MigrationCopyR29.chain(chain,row.rowid,row.hash);after=row.rowid;count++;}}
        return new Content(count,bytes,chain);
    }
    static List<Object> objects(SQLiteDatabase db,String table){List<Object> out=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT type,name,sql FROM main.sqlite_master WHERE tbl_name=? AND type IN('index','trigger') AND sql IS NOT NULL ORDER BY type,name",new String[]{table})){while(c.moveToNext())out.add(Arrays.asList(c.getString(0),c.getString(1),c.getString(2)));}return out;}
    static boolean audited(SQLiteDatabase db,String source){if(tableSql(db,AUDIT)==null)return false;try(Cursor c=db.rawQuery("SELECT 1 FROM main."+AUDIT+" WHERE source_name=?",new String[]{source})){return c.moveToFirst();}}
    static void archiveGuards(SQLiteDatabase db,String source,String retained){for(String event:new String[]{"INSERT","UPDATE","DELETE"})db.execSQL("CREATE TRIGGER IF NOT EXISTS "+q("r29_retained_formal_"+source+"_"+event)+" BEFORE "+event+" ON main."+q(retained)+" BEGIN SELECT RAISE(ABORT,'R29_RETAINED_FORMAL_COPY_IMMUTABLE'); END");}
    static void auditSchema(SQLiteDatabase db){
        db.execSQL("CREATE TABLE IF NOT EXISTS "+AUDIT+"(source_name TEXT PRIMARY KEY,retained_name TEXT NOT NULL UNIQUE,original_ddl TEXT NOT NULL,original_ddl_hash TEXT NOT NULL,original_schema_json TEXT NOT NULL,row_count INTEGER NOT NULL,typed_content_hash TEXT NOT NULL,typed_bytes INTEGER NOT NULL,original_objects_json TEXT NOT NULL,retained_ddl_hash TEXT NOT NULL,owner_epoch INTEGER NOT NULL,retained_at INTEGER NOT NULL,scope TEXT NOT NULL)");
        for(String event:new String[]{"UPDATE","DELETE"})db.execSQL("CREATE TRIGGER IF NOT EXISTS r29_retained_formal_audit_"+event+" BEFORE "+event+" ON "+AUDIT+" BEGIN SELECT RAISE(ABORT,'R29_RETAINED_FORMAL_AUDIT_IMMUTABLE'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r29_retained_formal_audit_first_insert BEFORE INSERT ON "+AUDIT+" WHEN EXISTS(SELECT 1 FROM "+AUDIT+" WHERE source_name=NEW.source_name) BEGIN SELECT RAISE(ABORT,'R29_RETAINED_FORMAL_AUDIT_FIRST_WRITE'); END");
    }
    static void verifyExisting(SQLiteDatabase db,String source,String retained,long deadline){
        if(tableSql(db,AUDIT)==null)throw new IllegalStateException("R29_UNATTRIBUTED_RETAINED_FORMAL_COPY:"+source);
        try(Cursor c=db.rawQuery("SELECT retained_name,row_count,typed_content_hash,retained_ddl_hash FROM main."+AUDIT+" WHERE source_name=?",new String[]{source})){
            String ddl=tableSql(db,retained);if(!c.moveToFirst()||!retained.equals(c.getString(0))||ddl==null||!StatsR27.sha256(ddl).equals(c.getString(3)))throw new IllegalStateException("R29_RETAINED_FORMAL_AUDIT_CONFLICT:"+source);
            Content actual=content(db,MigrationCopyR29.table(db,retained,ddl),deadline);if(actual.count!=c.getLong(1)||!actual.hash.equals(c.getString(2)))throw new IllegalStateException("R29_RETAINED_FORMAL_CONTENT_CONFLICT:"+source);
        }
    }
    /** Caller owns the independent RP transaction. Root authority handles are explicitly rejected. */
    public static void isolate(SQLiteDatabase rp){
        owner(rp);if(!rp.inTransaction())throw new IllegalStateException("R29_RETAINED_FORMAL_TRANSACTION_REQUIRED");long deadline=System.nanoTime()+30_000_000_000L;
        boolean any=copies(rp);if(any){
            // ALTER reparses the schema. Fix the precisely identified old cross-Root FTRL copies and TinyDeep
            // guards through their real fact owners first; never create a fake Root ledger/trades to satisfy SQL.
            CriticalInputMirrorR29.installResearch(rp);LegacyLearnedInputR29.installResearch(rp);LegacyLearningCommitR29.installResearch(rp);
        }
        for(String source:SOURCES){String retained=retainedName(source),ddl=tableSql(rp,source);
            if(ddl==null){if(object(rp,retained)||audited(rp,source))verifyExisting(rp,source,retained,deadline);continue;}
            if(object(rp,retained))throw new IllegalStateException("R29_RETAINED_FORMAL_NAME_CONFLICT:"+source);
            if(tableSql(rp,AUDIT)!=null)try(Cursor c=rp.rawQuery("SELECT 1 FROM main."+AUDIT+" WHERE source_name=?",new String[]{source})){if(c.moveToFirst())throw new IllegalStateException("R29_RECREATED_FORMAL_COPY_CONFLICT:"+source);}
            // SQLite would automatically retarget dependent views to the retained copy. Such a view could then
            // expose old RP rows under a different live-looking name; require an explicit owner repair instead.
            try(Cursor c=rp.rawQuery("SELECT name,sql FROM main.sqlite_master WHERE type IN('view','trigger') AND tbl_name<>? AND sql IS NOT NULL",new String[]{source})){while(c.moveToNext())if(c.getString(1).toLowerCase(Locale.ROOT).contains(source.toLowerCase(Locale.ROOT)))throw new IllegalStateException("R29_FORMAL_COPY_DEPENDENT_OBJECT_REQUIRES_REVIEW:"+c.getString(0));}
            MigrationCopyR29.Table before=MigrationCopyR29.table(rp,source,ddl);Content old=content(rp,before,deadline);List<Object> oldObjects=objects(rp,source);auditSchema(rp);
            rp.execSQL("ALTER TABLE main."+q(source)+" RENAME TO "+q(retained));String renamed=tableSql(rp,retained);if(renamed==null||tableSql(rp,source)!=null)throw new IllegalStateException("R29_FORMAL_RENAME_NOT_EXCLUSIVE:"+source);
            MigrationCopyR29.Table after=MigrationCopyR29.table(rp,retained,renamed);Content actual=content(rp,after,deadline);
            if(!before.columns.equals(after.columns)||old.count!=actual.count||old.bytes!=actual.bytes||!old.hash.equals(actual.hash))throw new IllegalStateException("R29_FORMAL_RENAME_CHANGED_TYPED_CONTENT:"+source);
            long epoch=n(rp,"SELECT epoch FROM main.r29_research_owner WHERE singleton=1");
            rp.execSQL("INSERT INTO "+AUDIT+" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{source,retained,ddl,StatsR27.sha256(ddl),before.schemaJson,old.count,old.hash,old.bytes,JsonR27.write(oldObjects),StatsR27.sha256(renamed),epoch,System.currentTimeMillis(),"RETAINED_PRE_FIX_RP_COPY_NOT_ROOT_AUTHORITY_NOT_NEW_REAL"});archiveGuards(rp,source,retained);
        }
    }
    public static void ensure(DbStore store){
        SQLiteDatabase root=store.coordinatorForResearchR952().realtimeReadOnlyR29();if(!ResearchRouteR29.active(root))return;
        SQLiteDatabase reader=ResearchRouteR29.reader(root);
        if(!copies(reader))return;ResearchRouteR29.write(store,"R29_RETAIN_FORMER_ROOT_FORMAL_COPIES",rp->{isolate(rp);return 0;});
    }
}
