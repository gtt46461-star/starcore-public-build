package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DbStore;
import com.ethk3.novaonepro.SchemaDomainCoordinatorR926;
import com.ethk3.novaonepro.DatabaseCoordinator;
import java.io.*;
import java.util.*;

/** Existing single writer owns archive/proof changes; file census and unlinking run outside its transactions. */
public final class ResearchRetentionStoreR30 {
    private ResearchRetentionStoreR30() {}
    public static final String INDEX_DDL="CREATE TABLE IF NOT EXISTS r30_retention_archive_index(source_table TEXT PRIMARY KEY,archive_table TEXT NOT NULL,schema_hash TEXT NOT NULL,columns_json TEXT NOT NULL,retained_rows INTEGER NOT NULL DEFAULT 0,updated_at INTEGER NOT NULL)";
    public static final String RUN_DDL="CREATE TABLE IF NOT EXISTS r30_retention_run(run_id TEXT PRIMARY KEY,measured_at INTEGER NOT NULL,budget_bytes INTEGER NOT NULL,report_json TEXT NOT NULL)";
    public static final String FILE_DDL="CREATE TABLE IF NOT EXISTS r30_retention_file_action(action_id TEXT PRIMARY KEY,relative_path TEXT NOT NULL,kind TEXT NOT NULL,bytes INTEGER NOT NULL,modified_at INTEGER NOT NULL,copy_reference TEXT NOT NULL,state TEXT NOT NULL,recorded_at INTEGER NOT NULL,finished_at INTEGER NOT NULL DEFAULT 0)";
    static final String BOOT_DDL="CREATE TABLE IF NOT EXISTS r30_retention_boot_checkpoint(process_nonce TEXT PRIMARY KEY,snapshot_at INTEGER NOT NULL,retained_rows INTEGER NOT NULL)";
    private static final String PROCESS_CHECKPOINT_SNAPSHOT=UUID.randomUUID().toString();
    static String q(String value){return "\""+value.replace("\"","\"\"")+"\"";}
    static String lit(String value){return "'"+value.replace("'","''")+"'";}
    static long schema(SQLiteDatabase db){try(Cursor c=db.rawQuery("PRAGMA schema_version",null)){if(!c.moveToFirst())throw new IllegalStateException("R30_RETENTION_SCHEMA_UNKNOWN");return c.getLong(0);}}
    static boolean exists(SQLiteDatabase db,String table){try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{table})){return c.moveToFirst();}}
    static List<String[]> columns(SQLiteDatabase db,String table){List<String[]> out=new ArrayList<>();try(Cursor c=db.rawQuery("PRAGMA table_info("+q(table)+")",null)){while(c.moveToNext())out.add(new String[]{c.getString(1),c.getString(2)==null?"":c.getString(2),c.getString(5)});}return out;}
    /** The same builder installs and authenticates guards; existing unknown SQL is never repaired by name. */
    private static LinkedHashMap<String,String> guardsR31(String source,List<String[]> columns,String hash){
        String target="r30_keep_"+source,prefix=target+"_"+hash.substring(0,12);
        String now="CAST((julianday('now')-2440587.5)*86400000 AS INTEGER)";
        StringJoiner names=new StringJoiner(","),values=new StringJoiner(","),clears=new StringJoiner(" OR "),payloadColumns=new StringJoiner(",");
        for(String[] column:columns){String name=column[0];names.add(q(name));values.add("OLD."+q(name));if(column[1].toUpperCase(Locale.ROOT).contains("BLOB")||name.equals("snapshot")||name.endsWith("_json")){
            payloadColumns.add(q(name));clears.add("(length(OLD."+q(name)+")>0 AND (NEW."+q(name)+" IS NULL OR length(NEW."+q(name)+")=0))");}}
        String enabled="(SELECT schema_hash FROM r30_retention_archive_index WHERE source_table="+lit(source)+")="+lit(hash);
        String body="INSERT INTO "+q(target)+"(__r30_at,__r30_reason,"+names+") VALUES("+now+",REASON,"+values+"); UPDATE r30_retention_archive_index SET retained_rows=retained_rows+1,updated_at="+now+" WHERE source_table="+lit(source)+";";
        LinkedHashMap<String,String> guards=new LinkedHashMap<>();
        guards.put(prefix+"_delete","CREATE TRIGGER IF NOT EXISTS "+q(prefix+"_delete")+" BEFORE DELETE ON "+q(source)+" WHEN "+enabled+" BEGIN "+body.replace(",REASON,",","+lit("BEFORE_ORIGINAL_DELETE")+",")+" END");
        if(payloadColumns.length()>0)guards.put(prefix+"_clear","CREATE TRIGGER IF NOT EXISTS "+q(prefix+"_clear")+" BEFORE UPDATE OF "+payloadColumns+" ON "+q(source)+" WHEN "+enabled+" AND ("+clears+") BEGIN "+body.replace(",REASON,",","+lit("BEFORE_ORIGINAL_PAYLOAD_CLEAR")+",")+" END");
        for(String action:new String[]{"update","delete"})guards.put(target+"_immutable_"+action,"CREATE TRIGGER IF NOT EXISTS "+q(target+"_immutable_"+action)+" BEFORE "+action.toUpperCase(Locale.ROOT)+" ON "+q(target)+" BEGIN SELECT RAISE(ABORT,'R30_PROTECTED_ARCHIVE_IMMUTABLE'); END");
        return guards;
    }
    private static String columnsHashR31(List<String[]> columns){List<List<String>> metadata=new ArrayList<>();for(String[] column:columns)metadata.add(Arrays.asList(column));return StatsR27.sha256(JsonR27.write(metadata));}
    private static String normalizedGuardR31(String sql){if(sql==null)return "";String value=sql.trim(),header="CREATE TRIGGER IF NOT EXISTS ";return value.startsWith(header)?"CREATE TRIGGER "+value.substring(header.length()):value;}
    /** Read-only owner contract used by consumers to distinguish known archive guards from unknown work. */
    public static String[][] sourceProtectionTriggersR31(SQLiteDatabase db,String source){
        if(!ResearchRetentionR30.PROTECTED_TABLES.contains(source))throw new IllegalArgumentException("R31_RETENTION_SOURCE_NOT_PROTECTED");
        if(!exists(db,source))return new String[0][];
        List<String[]> columns=columns(db,source);
        LinkedHashMap<String,String> guards=GapReferenceR31.expectedGuards(db,source,KeepArchiveRetirementR31.expectedGuards(db,source,KeepMismatchReferenceR31.expectedGuards(db,source,guardsR31(source,columns,columnsHashR31(columns)))));
        List<String[]> attached=new ArrayList<>();String archive="r30_keep_"+source;
        for(Map.Entry<String,String> guard:guards.entrySet())if(!guard.getKey().startsWith(archive+"_immutable_")&&!guard.getKey().equals(KeepMismatchReferenceR31.RETIRE_GUARD)&&!(KeepArchiveRetirementR31.eligible(source)&&guard.getKey().equals(KeepArchiveRetirementR31.guardName(source))))attached.add(new String[]{guard.getKey(),guard.getValue()});
        return attached.toArray(new String[0][]);
    }
    /** Return only exact guards attached to Critical's protected feedback table, not archive-table guards. */
    public static String[][] criticalProtectionTriggersR31(SQLiteDatabase db){return sourceProtectionTriggersR31(db,"r26_feedback");}
    /** Missing is allowed only before installation; every existing object must have the exact owner SQL. */
    public static boolean requireGuardDefinitionsR31(SQLiteDatabase db,String[][] definitions,boolean requireAll){
        boolean complete=true;
        for(String[] definition:definitions){
            try(Cursor c=db.rawQuery("SELECT type,sql FROM sqlite_master WHERE name=?",new String[]{definition[0]})){
                if(!c.moveToFirst()){complete=false;if(requireAll)throw new DatabaseCoordinator.SchemaPendingR31("R31_RETENTION","MISSING_GUARD:"+definition[0]);}
                else if(!"trigger".equals(c.getString(0))||!normalizedGuardR31(definition[1]).equals(normalizedGuardR31(c.getString(1))))throw new DatabaseCoordinator.SchemaPendingR31("R31_RETENTION","EXACT_GUARD_MISMATCH:"+definition[0]);
            }
        }
        return complete;
    }
    private static String archiveDdlR31(String target,List<String[]> columns){
        StringBuilder sql=new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(q(target)).append("(__r30_id INTEGER PRIMARY KEY AUTOINCREMENT,__r30_at INTEGER NOT NULL,__r30_reason TEXT NOT NULL");
        for(String[] column:columns)sql.append(',').append(q(column[0]));return sql.append(')').toString();
    }
    /** Ignore only SQL layout outside quoted tokens; literals and quoted identifiers remain byte exact. */
    private static String archiveDefinitionR31(String sql){
        if(sql==null)return "";String value=sql.trim(),header="CREATE TABLE IF NOT EXISTS ";
        if(value.startsWith(header))value="CREATE TABLE "+value.substring(header.length());
        StringBuilder out=new StringBuilder();char quote=0;
        for(int i=0;i<value.length();i++){
            char c=value.charAt(i);
            if(quote!=0){out.append(c);if(c==quote){if(i+1<value.length()&&value.charAt(i+1)==quote){out.append(value.charAt(++i));}else quote=0;}}
            else if(c=='\''||c=='"'||c=='`'){quote=c;out.append(c);}
            else if(c=='['){quote=']';out.append(c);}
            else if(!Character.isWhitespace(c))out.append(c);
        }
        return out.toString();
    }
    /** An existing archive is evidence. Refuse affinity/constraint drift instead of silently coercing or rebuilding it. */
    private static void requireArchiveDefinitionR31(SQLiteDatabase db,String target){
        try(Cursor c=db.rawQuery("SELECT type,name,sql FROM sqlite_master WHERE tbl_name=? AND type IN ('trigger','index') AND name NOT LIKE 'sqlite_%'",new String[]{target})){
            while(c.moveToNext()){
                String name=c.getString(1);
                if(!"trigger".equals(c.getString(0))||!(name.equals(target+"_immutable_update")||name.equals(target+"_immutable_delete")||KeepMismatchReferenceR31.recognizedArchiveAttachment(db,target,name)||CheckpointGcStoreR31.knownReferenceGuard(db,target,c.getString(0),name,c.getString(2))||KeepArchiveRetirementR31.knownGuard(db,target,c.getString(0),name,c.getString(2))))
                    throw new DatabaseCoordinator.SchemaPendingR31("R31_RETENTION","UNKNOWN_ARCHIVE_ATTACHMENT:"+target+":"+name);
            }
        }
        List<String[]> payload=new ArrayList<>();int index=0;
        String[] metadata={"__r30_id","__r30_at","__r30_reason"},types={"INTEGER","INTEGER","TEXT"};
        try(Cursor c=db.rawQuery("PRAGMA table_info("+q(target)+")",null)){
            while(c.moveToNext()){
                String name=c.getString(1),type=c.getString(2)==null?"":c.getString(2).trim().toUpperCase(Locale.ROOT);
                if(index<3){
                    if(!metadata[index].equals(name)||!types[index].equals(type)||c.getInt(3)!=(index==0?0:1)||!c.isNull(4)||c.getInt(5)!=(index==0?1:0))
                        throw new DatabaseCoordinator.SchemaPendingR31("R31_RETENTION","ARCHIVE_METADATA_CONTRACT:"+target+":"+name);
                }else{
                    if(name.startsWith("__r30_")||!type.isEmpty()||c.getInt(3)!=0||!c.isNull(4)||c.getInt(5)!=0)
                        throw new DatabaseCoordinator.SchemaPendingR31("R31_RETENTION","ARCHIVE_STORAGE_CLASS_CONTRACT:"+target+":"+name);
                    payload.add(new String[]{name,"","0"});
                }
                index++;
            }
        }
        if(index<3)throw new DatabaseCoordinator.SchemaPendingR31("R31_RETENTION","ARCHIVE_METADATA_MISSING:"+target);
        try(Cursor c=db.rawQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name=?",new String[]{target})){
            if(!c.moveToFirst()||!archiveDefinitionR31(archiveDdlR31(target,payload)).equals(archiveDefinitionR31(c.getString(0))))
                throw new DatabaseCoordinator.SchemaPendingR31("R31_RETENTION","EXACT_ARCHIVE_DEFINITION:"+target);
        }
    }
    /** Read-only exact archive contract used before preparing or committing migration work. */
    public static void requireKeepMigrationSourceR31(SQLiteDatabase db,String archive){
        String source=KeepMigrationStoreR31.source(archive);
        if(!exists(db,source)||!exists(db,archive))throw new DatabaseCoordinator.SchemaPendingR31("R31_KEEP","SOURCE_NOT_PRESENT");
        List<String[]> columns=columns(db,source);String hash=columnsHashR31(columns);
        try(Cursor c=db.rawQuery("SELECT 1 FROM r30_retention_archive_index WHERE source_table=? AND archive_table=? AND schema_hash=?",new String[]{source,archive,KeepMismatchReferenceR31.effectiveSchemaHash(db,source,hash)})){if(!c.moveToFirst())throw new DatabaseCoordinator.SchemaPendingR31("R31_KEEP","OWNER_INDEX_CHANGED");}
        requireArchiveDefinitionR31(db,archive);LinkedHashMap<String,String> guards=GapReferenceR31.expectedGuards(db,source,KeepArchiveRetirementR31.expectedGuards(db,source,KeepMismatchReferenceR31.expectedGuards(db,source,guardsR31(source,columns,hash))));String[][] definitions=new String[guards.size()][];int n=0;for(Map.Entry<String,String> entry:guards.entrySet())definitions[n++]=new String[]{entry.getKey(),entry.getValue()};requireGuardDefinitionsR31(db,definitions,true);
    }
    /** Added archive columns have NONE affinity: SQLite storage classes, including full BLOB bytes, are preserved. */
    public static void ensureProtected(SQLiteDatabase db){
        ensureProtected(db,null);
    }
    public static void ensureProtected(SQLiteDatabase db,StartupCheckpointIdentityR31.Prepared startup){
        SchemaDomainCoordinatorR926.migrateR31RetentionProtection(db,x->ensureProtectedObjectsR31(x,startup));
    }
    private static void ensureProtectedObjectsR31(SQLiteDatabase db){
        ensureProtectedObjectsR31(db,null);
    }
    private static void ensureProtectedObjectsR31(SQLiteDatabase db,StartupCheckpointIdentityR31.Prepared startup){
        if(!db.inTransaction())throw new IllegalStateException("R30_RETENTION_EXISTING_WRITER_REQUIRED");
        db.execSQL(INDEX_DDL);db.execSQL(RUN_DDL);db.execSQL(FILE_DDL);
        for(String source:ResearchRetentionR30.PROTECTED_TABLES){
            if(!exists(db,source))continue;List<String[]> columns=columns(db,source);if(columns.isEmpty())throw new IllegalStateException("R30_RETENTION_SOURCE_COLUMNS:"+source);
            String target="r30_keep_"+source;List<List<String>> metadata=new ArrayList<>();for(String[] c:columns)metadata.add(Arrays.asList(c));String encoded=JsonR27.write(metadata);String hash=StatsR27.sha256(encoded);
            LinkedHashMap<String,String> guards=GapReferenceR31.expectedGuards(db,source,KeepArchiveRetirementR31.expectedGuards(db,source,KeepMismatchReferenceR31.expectedGuards(db,source,guardsR31(source,columns,hash))));
            String[][] definitions=new String[guards.size()][];int guardIndex=0;for(Map.Entry<String,String> guard:guards.entrySet())definitions[guardIndex++]=new String[]{guard.getKey(),guard.getValue()};
            boolean complete=requireGuardDefinitionsR31(db,definitions,false);
            if(exists(db,target))requireArchiveDefinitionR31(db,target);
            boolean same=false;try(Cursor c=db.rawQuery("SELECT 1 FROM r30_retention_archive_index WHERE source_table=? AND schema_hash=? AND archive_table=?",new String[]{source,KeepMismatchReferenceR31.effectiveSchemaHash(db,source,hash),target})){same=c.moveToFirst();}
            if(same&&exists(db,target)){
                Set<String> archived=new HashSet<>();for(String[] column:columns(db,target))archived.add(column[0]);
                for(String[] column:columns)if(!archived.contains(column[0]))throw new DatabaseCoordinator.SchemaPendingR31("R31_RETENTION","ARCHIVE_DECLARED_COLUMN_MISSING:"+target+":"+column[0]);
                if(complete)continue;
            }
            StringBuilder ddl=new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(q(target)).append("(__r30_id INTEGER PRIMARY KEY AUTOINCREMENT,__r30_at INTEGER NOT NULL,__r30_reason TEXT NOT NULL");
            for(String[] column:columns){if(column[0].startsWith("__r30_"))throw new IllegalStateException("R30_RETENTION_RESERVED_COLUMN:"+source);ddl.append(',').append(q(column[0]));}ddl.append(')');db.execSQL(ddl.toString());
            Set<String> archivedColumns=new HashSet<>();for(String[] column:columns(db,target))archivedColumns.add(column[0]);
            for(String[] column:columns)if(!archivedColumns.contains(column[0]))db.execSQL("ALTER TABLE "+q(target)+" ADD COLUMN "+q(column[0]));
            db.execSQL("INSERT OR IGNORE INTO r30_retention_archive_index(source_table,archive_table,schema_hash,columns_json,retained_rows,updated_at) VALUES(?,?,?,?,0,?)",new Object[]{source,target,KeepMismatchReferenceR31.effectiveSchemaHash(db,source,hash),encoded,System.currentTimeMillis()});
            db.execSQL("UPDATE r30_retention_archive_index SET schema_hash=?,columns_json=?,updated_at=?,archive_table=? WHERE source_table=? AND (schema_hash<>? OR archive_table<>?)",new Object[]{KeepMismatchReferenceR31.effectiveSchemaHash(db,source,hash),encoded,System.currentTimeMillis(),target,source,KeepMismatchReferenceR31.effectiveSchemaHash(db,source,hash),target});
            requireArchiveDefinitionR31(db,target);
            for(String sql:guards.values())db.execSQL(sql);
            requireGuardDefinitionsR31(db,definitions,true);
        }
        preserveStartupCheckpoint(db,startup);
    }
    /** Preserve the incoming version's complete checkpoint before ordinary training replaces it.
     * The marker commits with the archive: a failed startup can retry, while repeated schema/retention
     * checks in this process never copy every intermediate training checkpoint. */
    private static void preserveStartupCheckpoint(SQLiteDatabase db){
        preserveStartupCheckpoint(db,null);
    }
    private static void preserveStartupCheckpoint(SQLiteDatabase db,StartupCheckpointIdentityR31.Prepared startup){
        String source="r29_model_checkpoint",target="r30_keep_"+source;
        if(!exists(db,source)||!exists(db,target))return;
        db.execSQL(BOOT_DDL);
        try(Cursor c=db.rawQuery("SELECT 1 FROM r30_retention_boot_checkpoint WHERE process_nonce=?",new String[]{PROCESS_CHECKPOINT_SNAPSHOT})){
            if(c.moveToFirst())return;
        }
        if(startup==null&&KeepArchiveRetirementR31.MODE.equals(KeepArchiveRetirementR31.mode(db,source)))
            throw new DatabaseCoordinator.SchemaPendingR31("R31_STARTUP_CHECKPOINT","CAS_EQUALITY_PREPARATION_REQUIRED");
        List<String[]> columns=columns(db,source);
        StringJoiner names=new StringJoiner(","),values=new StringJoiner(","),same=new StringJoiner(" AND ");
        for(String[] column:columns){
            String name=q(column[0]);names.add(name);values.add("incoming."+name);
            same.add("(typeof(saved."+name+")=typeof(incoming."+name+") AND saved."+name+" IS incoming."+name+")");
        }
        long now=System.currentTimeMillis();
        List<Object> bindings=new ArrayList<>();bindings.add(now);bindings.add("PRE_TRAINING_STARTUP_CHECKPOINT");
        String casExcluded=startup==null?"":" AND NOT ("+startup.excludeSql(db,"incoming",bindings)+")";
        db.execSQL("INSERT INTO "+q(target)+"(__r30_at,__r30_reason,"+names+") SELECT ?,?,"+values
                +" FROM "+q(source)+" incoming WHERE length(incoming.artifact)>0 AND NOT EXISTS(SELECT 1 FROM "+q(target)+" saved WHERE "+same+")"+casExcluded,
                bindings.toArray());
        long retained;
        try(Cursor c=db.rawQuery("SELECT changes()",null)){
            if(!c.moveToFirst())throw new IllegalStateException("R30_CHECKPOINT_ARCHIVE_COUNT_UNKNOWN");
            retained=c.getLong(0);
        }
        db.execSQL("UPDATE r30_retention_archive_index SET retained_rows=retained_rows+?,updated_at=? WHERE source_table=?",new Object[]{retained,now,source});
        db.execSQL("INSERT INTO r30_retention_boot_checkpoint VALUES(?,?,?)",new Object[]{PROCESS_CHECKPOINT_SNAPSHOT,now,retained});
        if(startup!=null)startup.recordWithin(db,PROCESS_CHECKPOINT_SNAPSHOT,retained);
    }
    static String verifiedFoldback(SQLiteDatabase db){
        try{
            try(Cursor route=db.rawQuery("SELECT phase FROM r29_database_route WHERE singleton=1",null)){if(!route.moveToFirst()||!"RETIRED_BY_R30".equals(route.getString(0)))return null;}
            String run;long expectedTables=-1,expectedRows=-1;
            try(Cursor state=db.rawQuery("SELECT state,run_id,detail,tables,rows FROM r30_foldback_state WHERE singleton=1",null)){
                if(!state.moveToFirst()||state.getString(2)==null||!state.getString(2).isEmpty())return null;
                if("FOLDED_BACK".equals(state.getString(0))){run=state.getString(1);expectedTables=state.getLong(3);expectedRows=state.getLong(4);}
                else if("NO_ACTIVE_R29_ROUTE:RETIRED_BY_R30".equals(state.getString(0))){
                    // The original second-start no-op overwrites only the last-state row. Its RETIRED route is
                    // committed exclusively after the entire copy succeeds, and original copy receipts survive.
                    try(Cursor latest=db.rawQuery("SELECT run_id FROM r30_copy_log WHERE direction='R29_RP_TO_ROOT' GROUP BY run_id ORDER BY max(finished_at) DESC,run_id DESC LIMIT 1",null)){if(!latest.moveToFirst())return null;run=latest.getString(0);}
                }else return null;
            }
            try(Cursor log=db.rawQuery("SELECT count(*),sum(CASE WHEN state='COPIED' AND source_rows=copied_rows AND missing_columns IN ('','[]') THEN 0 ELSE 1 END),sum(copied_rows) FROM r30_copy_log WHERE run_id=? AND direction='R29_RP_TO_ROOT'",new String[]{run})){
                if(!log.moveToFirst()||log.getLong(0)==0||log.getLong(1)!=0||(expectedTables>=0&&log.getLong(0)!=expectedTables)||(expectedRows>=0&&log.getLong(2)!=expectedRows))return null;
            }return run;
        }catch(RuntimeException missing){return null;}
    }
    public interface Writer {void write(String operation,ResearchWriteQueueR27.Batch batch)throws Exception;}
    /** Adapter also used by real SQLite/file regressions. No open writer transaction is allowed across file IO. */
    public static ResearchRetentionR30.Result cleanFiles(SQLiteDatabase read,File appData,long budget,Writer writer)throws Exception{
        if(read.inTransaction())throw new IllegalStateException("R30_FILE_RETENTION_CALLED_WITH_TRANSACTION");
        Map<String,String> actions=new HashMap<>();
        return ResearchRetentionR30.clean(appData,budget,new ResearchRetentionR30.Audit(){
            public String retiredCopyReference(ResearchRetentionR30.Candidate candidate){return verifiedFoldback(read);}
            public void beforeDelete(ResearchRetentionR30.Candidate candidate,String reference)throws Exception{
                String id=UUID.randomUUID().toString();actions.put(candidate.relative,id);ResearchWriteQueueR27.Batch batch=new ResearchWriteQueueR27.Batch();
                batch.add("INSERT INTO r30_retention_file_action(action_id,relative_path,kind,bytes,modified_at,copy_reference,state,recorded_at) VALUES(?,?,?,?,?,?,'DELETE_PENDING',?)",id,candidate.relative,candidate.kind,candidate.bytes,candidate.modified,reference,System.currentTimeMillis());
                if(candidate.kind.equals("VERIFIED_RETIRED_DUPLICATE"))batch.add("INSERT INTO r30_copy_log(run_id,table_name,direction,source_rows,copied_rows,missing_columns,state,finished_at) SELECT ?,?,'R30_RETIRED_FILE_DELETE_REFERENCE',coalesce(sum(source_rows),0),coalesce(sum(copied_rows),0),'[]',?,? FROM r30_copy_log WHERE run_id=? AND direction='R29_RP_TO_ROOT'",id,"@FILE:"+candidate.relative,"ORIGINAL_FOLDBACK_COPY_REFERENCE:"+reference,System.currentTimeMillis(),reference);
                writer.write("R30_RETENTION_FILE_APPROVAL",batch);
            }
            public void afterDelete(ResearchRetentionR30.Candidate candidate,String reference,boolean deleted)throws Exception{
                ResearchWriteQueueR27.Batch batch=new ResearchWriteQueueR27.Batch();batch.add("UPDATE r30_retention_file_action SET state=?,finished_at=? WHERE action_id=? AND state='DELETE_PENDING'",deleted?"DELETED":"PRESERVED_FILE_CHANGED_OR_DELETE_FAILED",System.currentTimeMillis(),actions.get(candidate.relative));writer.write("R30_RETENTION_FILE_RESULT",batch);
            }
        });
    }
    public static String visualCacheDeleteSql(){
        // R31-B05-C08: only panels classified DISPLAY; business state (mainline/choice), checkpoints and timelines are never scanned.
        StringJoiner panels=new StringJoiner(",");for(String panel:ChartCacheR27.displayPanelsR31())panels.add(lit(panel));
        return "DELETE FROM r27_chart_cache WHERE rowid IN (SELECT rowid FROM r27_chart_cache WHERE panel_id IN ("+panels+") ORDER BY updated_at,panel_id,key LIMIT 500)";
    }
    public static ResearchRetentionR30.Result step(DbStore store,RuntimeCoordinatorR26.Claim claim)throws Exception{
        File data=store.contextForR26().getFilesDir().getParentFile();
        long budget=store.contextForR26().getSharedPreferences("r30_research_retention",0).getLong("total_budget_bytes",ResearchRetentionR30.DEFAULT_BUDGET_BYTES);
        Writer writer=(operation,batch)->store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(operation,db->{RuntimeCoordinatorR26.requireFence(db,claim);ResearchWriteQueueR27.exec(db,batch);return 0;});
        ResearchRetentionR30.Result result=cleanFiles(store.readable(),data,budget,writer);
        ResearchWriteQueueR27.Batch batch=new ResearchWriteQueueR27.Batch();
        if(result.after.complete&&budget>0&&result.after.bytes>budget)batch.add(visualCacheDeleteSql());
        batch.add("INSERT INTO r30_retention_run(run_id,measured_at,budget_bytes,report_json) VALUES(?,?,?,?)",UUID.randomUUID().toString(),System.currentTimeMillis(),budget,JsonR27.write(result.json()));
        writer.write("R30_RETENTION_BUDGET_RESULT",batch);return result;
    }
}
