package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;
import java.util.regex.*;

/** Keeps the original SU1/SU2 SQL and journals intact while installing only the current database's objects.
 * Root controls never become a research write authority; their optional read replicas come from Root owners. */
public final class StrategySchemaDomainR29 {
    private StrategySchemaDomainR29(){}
    private static final Set<String> ROOT=Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "su1_mainline_binding","su1_source_clock","su2_live_decision","su2_mainline_choice","su2_mainline_binding")));
    private static final Set<String> CLOCK_TRIGGERS=Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "su1_draw_insert_clock","su1_draw_update_clock","su1_draw_delete_clock")));
    private static final Pattern TABLE=Pattern.compile("(?is)^\\s*CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?([a-z0-9_]+)");
    private static final Pattern INDEX=Pattern.compile("(?is)^\\s*CREATE\\s+(?:UNIQUE\\s+)?INDEX\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?([a-z0-9_]+)\\s+ON\\s+([a-z0-9_]+)");
    private static final Pattern TRIGGER=Pattern.compile("(?is)^\\s*CREATE\\s+TRIGGER\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?([a-z0-9_]+)\\s+.*?\\bON\\s+([a-z0-9_]+)");
    private static final Pattern INSERT=Pattern.compile("(?is)^\\s*INSERT\\s+(?:OR\\s+(?:IGNORE|REPLACE|ABORT|FAIL|ROLLBACK)\\s+)?INTO\\s+([a-z0-9_]+)");
    static final class Control {final String object,target;Control(String object,String target){this.object=object;this.target=target;}}
    private static Control control(String sql){
        Matcher m=TABLE.matcher(sql);if(m.find())return ROOT.contains(m.group(1))?new Control(m.group(1),m.group(1)):null;
        m=INDEX.matcher(sql);if(m.find())return ROOT.contains(m.group(2))?new Control(m.group(1),m.group(2)):null;
        m=TRIGGER.matcher(sql);if(m.find()){
            if(CLOCK_TRIGGERS.contains(m.group(1)))return new Control(m.group(1),"su1_source_clock");
            return ROOT.contains(m.group(2))?new Control(m.group(1),m.group(2)):null;
        }
        m=INSERT.matcher(sql);if(m.find()&&ROOT.contains(m.group(1)))return new Control("INSERT:"+m.group(1),m.group(1));
        return null;
    }
    static boolean requiredHere(SQLiteDatabase db,String sql){return !ResearchRouteR29.isResearchDatabase(db)||control(sql)==null;}
    static void execute(SQLiteDatabase db,String sql,String phase){
        if(!ResearchRouteR29.isResearchDatabase(db)){db.execSQL(sql);return;}
        Control control=control(sql);if(control==null){db.execSQL(sql);return;}
        if(!domainReady(db))throw new IllegalStateException("R29_EMPTY_STRATEGY_COPY_RETENTION_REQUIRED");
        String hash=StatsR27.sha256(sql),reason="ROOT_STRATEGY_CONTROL_RETAINED";
        db.execSQL("CREATE TABLE IF NOT EXISTS r29_schema_domain_omission(phase TEXT NOT NULL,sql_sha256 TEXT NOT NULL,object_name TEXT NOT NULL,target_table TEXT NOT NULL,disposition TEXT NOT NULL,original_sql TEXT NOT NULL,recorded_at INTEGER NOT NULL,PRIMARY KEY(phase,sql_sha256))");
        db.execSQL("INSERT OR IGNORE INTO r29_schema_domain_omission VALUES(?,?,?,?,?,?,?)",new Object[]{phase,hash,control.object,control.target,reason,sql,System.currentTimeMillis()});
        try(Cursor c=db.rawQuery("SELECT object_name,target_table,disposition,original_sql FROM r29_schema_domain_omission WHERE phase=? AND sql_sha256=?",new String[]{phase,hash})){
            if(!c.moveToFirst()||!control.object.equals(c.getString(0))||!control.target.equals(c.getString(1))||!reason.equals(c.getString(2))||!sql.equals(c.getString(3)))throw new IllegalStateException("R29_STRATEGY_SCHEMA_OMISSION_CONFLICT:"+control.object);
        }
    }
    private static final String[] BINDINGS={"su1_mainline_binding","su2_mainline_choice","su2_mainline_binding"};
    private static boolean exists(SQLiteDatabase db,String name){try(Cursor c=db.rawQuery("SELECT 1 FROM main.sqlite_master WHERE type='table' AND name=?",new String[]{name})){return c.moveToFirst();}}
    static boolean domainReady(SQLiteDatabase db){
        if(!ResearchRouteR29.isResearchDatabase(db))return true;
        boolean ready=true;for(String name:BINDINGS)if(exists(db,name)){
            if(MigrationCopyR29.scalar(db,"SELECT count(*) FROM "+name,null)!=0)throw new IllegalStateException("R29_MISPLACED_ROOT_STRATEGY_AUTHORITY_PRESERVED:"+name);
            ready=false;
        }return ready;
    }
    /** Only the previously-created, empty RP copies can be retired automatically. The transaction retains
     * their exact original object DDL and an immutable renamed table; a populated copy is never discarded. */
    static void prepareResearch(SQLiteDatabase db){
        if(!ResearchRouteR29.isResearchDatabase(db)||domainReady(db))return;
        if(!db.inTransaction()||exists(db,"r29_database_route"))throw new IllegalStateException("R29_EMPTY_STRATEGY_COPY_REQUIRES_EXCLUSIVE_RESEARCH_TRANSACTION");
        try(Cursor c=db.rawQuery("SELECT phase,schema_hash,source_root,epoch FROM r29_research_owner WHERE singleton=1",null)){
            if(!c.moveToFirst()||(!"COPYING".equals(c.getString(0))&&!"READY".equals(c.getString(0)))||!c.getString(1).matches("[0-9a-f]{64}")||!c.getString(2).matches("[0-9a-f]{64}")||c.getLong(3)<=0)throw new IllegalStateException("R29_EMPTY_STRATEGY_COPY_REQUIRES_VALID_RESEARCH_OWNER");
        }
        db.execSQL("CREATE TABLE IF NOT EXISTS r29_strategy_root_control_retention(original_table TEXT PRIMARY KEY,retained_table TEXT NOT NULL UNIQUE,original_sql TEXT NOT NULL,original_sha256 TEXT NOT NULL,object_manifest TEXT NOT NULL,manifest_sha256 TEXT NOT NULL,original_count INTEGER NOT NULL CHECK(original_count=0),retained_at INTEGER NOT NULL)");
        for(String event:new String[]{"UPDATE","DELETE"})db.execSQL("CREATE TRIGGER IF NOT EXISTS r29_strategy_retention_no_"+event+" BEFORE "+event+" ON r29_strategy_root_control_retention BEGIN SELECT RAISE(ABORT,'R29_STRATEGY_RETENTION_AUDIT_IMMUTABLE'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r29_strategy_retention_no_replace BEFORE INSERT ON r29_strategy_root_control_retention WHEN EXISTS(SELECT 1 FROM r29_strategy_root_control_retention WHERE original_table=NEW.original_table) BEGIN SELECT RAISE(ABORT,'R29_STRATEGY_RETENTION_AUDIT_IMMUTABLE'); END");
        for(String name:BINDINGS)if(exists(db,name)){
            if(MigrationCopyR29.scalar(db,"SELECT count(*) FROM "+name,null)!=0)throw new IllegalStateException("R29_MISPLACED_ROOT_STRATEGY_AUTHORITY_PRESERVED:"+name);
            String retained="r29_retained_root_"+name;if(exists(db,retained))throw new IllegalStateException("R29_RETAINED_STRATEGY_COPY_CONFLICT:"+name);
            String original=null;List<Map<String,Object>> objects=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT type,name,sql FROM main.sqlite_master WHERE tbl_name=? AND sql IS NOT NULL ORDER BY type,name",new String[]{name})){while(c.moveToNext()){
                Map<String,Object> object=new LinkedHashMap<>();object.put("type",c.getString(0));object.put("name",c.getString(1));object.put("sql",c.getString(2));objects.add(object);if("table".equals(c.getString(0)))original=c.getString(2);
            }}if(original==null)throw new IllegalStateException("R29_STRATEGY_ORIGINAL_DDL_REQUIRED:"+name);
            String manifest=JsonR27.write(objects);db.execSQL("INSERT INTO r29_strategy_root_control_retention VALUES(?,?,?,?,?,?,0,?)",new Object[]{name,retained,original,StatsR27.sha256(original),manifest,StatsR27.sha256(manifest),System.currentTimeMillis()});
            db.execSQL("ALTER TABLE "+name+" RENAME TO "+retained);
            for(String event:new String[]{"INSERT","UPDATE","DELETE"})db.execSQL("CREATE TRIGGER r29_retained_strategy_"+name+"_"+event+" BEFORE "+event+" ON "+retained+" BEGIN SELECT RAISE(ABORT,'R29_RETAINED_ROOT_STRATEGY_COPY_IMMUTABLE'); END");
            if(MigrationCopyR29.scalar(db,"SELECT count(*) FROM "+retained,null)!=0||exists(db,name))throw new IllegalStateException("R29_EMPTY_STRATEGY_COPY_RETENTION_VERIFICATION:"+name);
        }
        for(String sql:SqlStatementsSU1.SQL)if(control(sql)!=null)execute(db,sql,"SU1_STORAGE");
        for(String sql:StrategySchemaSU2.DDL)if(control(sql)!=null)execute(db,sql,"SU2_STORAGE");
    }
    /** Before cutover/capture, install the unchanged Root controls inside its original short transaction.
     * This is deliberately never called from a research writer or via a research scope. */
    public static void ensureRealtime(SQLiteDatabase root){
        if(ResearchRouteR29.isResearchDatabase(root)||!root.inTransaction())throw new IllegalStateException("R29_STRATEGY_ROOT_CONTROL_TRANSACTION_REQUIRED");
        for(String sql:SqlStatementsSU1.SQL)if(control(sql)!=null)root.execSQL(sql);
        for(String sql:StrategySchemaSU2.DDL)if(control(sql)!=null)root.execSQL(sql);
    }
}
