package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DatabaseCoordinator;
import java.io.*;
import java.security.*;
import java.util.*;

/** Startup's complete typed equality is prepared from actual source/CAS bytes,
 * outside the writer. Only a small source-generation guard crosses the commit.
 * State hashes are never evidence that two checkpoint BLOBs are equal. */
public final class StartupCheckpointIdentityR31 {
    private StartupCheckpointIdentityR31(){}
    public static final String SOURCE="r29_model_checkpoint",ARCHIVE="r30_keep_"+SOURCE;
    public static final String VERSION="R31_STARTUP_CHECKPOINT_TYPED_EQUALITY_1";
    public static final int MAX_OWNERS=64,MAX_CAS_ROWS=16384,CHUNK=65536;
    public static final String[] DDL={
        "CREATE TABLE IF NOT EXISTS r31_startup_checkpoint_source(singleton INTEGER PRIMARY KEY CHECK(singleton=1),generation INTEGER NOT NULL CHECK(typeof(generation)='integer' AND generation>=0))",
        "CREATE TRIGGER IF NOT EXISTS r31_startup_checkpoint_insert AFTER INSERT ON r29_model_checkpoint BEGIN UPDATE r31_startup_checkpoint_source SET generation=generation+1 WHERE singleton=1; END",
        "CREATE TRIGGER IF NOT EXISTS r31_startup_checkpoint_update AFTER UPDATE ON r29_model_checkpoint BEGIN UPDATE r31_startup_checkpoint_source SET generation=generation+1 WHERE singleton=1; END",
        "CREATE TRIGGER IF NOT EXISTS r31_startup_checkpoint_delete AFTER DELETE ON r29_model_checkpoint BEGIN UPDATE r31_startup_checkpoint_source SET generation=generation+1 WHERE singleton=1; END",
        "CREATE TABLE IF NOT EXISTS r31_startup_checkpoint_receipt(process_nonce TEXT PRIMARY KEY,source_generation INTEGER NOT NULL,source_schema_version INTEGER NOT NULL,column_contract_hash TEXT NOT NULL,source_rows INTEGER NOT NULL,cas_matched_rows INTEGER NOT NULL,actual_new_archive_rows INTEGER NOT NULL,compared_original_rows INTEGER NOT NULL,compared_original_bytes INTEGER NOT NULL,cas_references_json TEXT NOT NULL CHECK(length(CAST(cas_references_json AS BLOB))<=1048576),committed_at INTEGER NOT NULL,CHECK(source_rows>=0 AND cas_matched_rows>=0 AND actual_new_archive_rows>=0 AND cas_matched_rows+actual_new_archive_rows<=source_rows))"
    };
    public static String[] ddlR31(){return DDL.clone();}
    private static String q(String value){return "\""+value.replace("\"","\"\"")+"\"";}
    private static String exact(String value){return value==null?"":value.trim().replaceFirst("^(CREATE (?:TABLE|TRIGGER)) IF NOT EXISTS ","$1 ");}
    private static boolean exists(SQLiteDatabase db,String name){try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE name=?",new String[]{name})){return c.moveToFirst();}}
    private static void definitions(SQLiteDatabase db){
        for(String sql:DDL){java.util.regex.Matcher definition=java.util.regex.Pattern.compile("^CREATE (TABLE|TRIGGER) IF NOT EXISTS ([A-Za-z0-9_]+)").matcher(sql);if(!definition.find())throw new AssertionError("R31_STARTUP_DDL_HEADER");String type=definition.group(1).toLowerCase(Locale.ROOT),name=definition.group(2);
            try(Cursor c=db.rawQuery("SELECT type,sql FROM sqlite_master WHERE name=?",new String[]{name})){if(!c.moveToFirst()||!type.equals(c.getString(0))||!exact(sql).equals(exact(c.getString(1))))throw new DatabaseCoordinator.SchemaPendingR31("R31_STARTUP_CHECKPOINT","EXACT_DEFINITION:"+name);}
        }
    }
    public static boolean ready(SQLiteDatabase db){
        if(!exists(db,SOURCE)||!exists(db,"r31_startup_checkpoint_source"))return false;definitions(db);columns(db);
        try(Cursor c=db.rawQuery("SELECT generation,typeof(generation) FROM r31_startup_checkpoint_source WHERE singleton=1",null)){return c.moveToFirst()&&"integer".equals(c.getString(1))&&c.getLong(0)>=0;}
    }
    /** DML only, after this owner's DDL units; no source mutation or payload copy. */
    public static void seedWithinMigrationR31(SQLiteDatabase db){if(!db.inTransaction())throw new IllegalStateException("R31_STARTUP_CHECKPOINT_MIGRATION_WRITER");definitions(db);db.execSQL("INSERT OR IGNORE INTO r31_startup_checkpoint_source VALUES(1,0)");}
    private static long generation(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT generation,typeof(generation) FROM r31_startup_checkpoint_source WHERE singleton=1",null)){if(!c.moveToFirst()||!"integer".equals(c.getString(1))||c.getLong(0)<0)throw new IllegalStateException("R31_STARTUP_CHECKPOINT_GENERATION_UNKNOWN");return c.getLong(0);}}
    private static long schema(SQLiteDatabase db){try(Cursor c=db.rawQuery("PRAGMA main.schema_version",null)){if(!c.moveToFirst())throw new IllegalStateException("R31_STARTUP_CHECKPOINT_SCHEMA_UNKNOWN");return c.getLong(0);}}
    private static String identity(SQLiteDatabase db){
        String main=null;try(Cursor c=db.rawQuery("PRAGMA database_list",null)){while(c.moveToNext()){String name=c.getString(1);if("main".equals(name))main=c.getString(2);else if(!"temp".equals(name))throw new IllegalStateException("R31_STARTUP_CHECKPOINT_ATTACHED_DATABASE");}}
        try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_temp_master LIMIT 1",null)){if(c.moveToFirst())throw new IllegalStateException("R31_STARTUP_CHECKPOINT_TEMP_SCHEMA");}
        if(main==null)throw new IllegalStateException("R31_STARTUP_CHECKPOINT_DATABASE_UNKNOWN");return main.isEmpty()?"MEMORY@"+System.identityHashCode(db):main;
    }
    private static LinkedHashMap<String,String> columns(SQLiteDatabase db){
        String create;try(Cursor c=db.rawQuery("SELECT CASE WHEN length(CAST(sql AS BLOB))<=65536 THEN sql ELSE NULL END FROM sqlite_master WHERE type='table' AND name=?",new String[]{SOURCE})){if(!c.moveToFirst()||c.isNull(0))throw new IllegalStateException("R31_STARTUP_CHECKPOINT_SOURCE_MISSING_OR_CONTRACT_BOUND");create=c.getString(0);}
        LinkedHashMap<String,String> result=new LinkedHashMap<>();
        try(Cursor c=db.rawQuery("PRAGMA main.table_info("+q(SOURCE)+")",null)){while(c.moveToNext()){String name=c.getString(1);if(name==null||name.length()>512||result.containsKey(name)||result.size()>=253)throw new IllegalStateException("R31_STARTUP_CHECKPOINT_COLUMN_BOUND");if(c.getInt(5)!=("owner".equals(name)?1:0))throw new IllegalStateException("R31_STARTUP_CHECKPOINT_SINGLE_OWNER_PRIMARY_KEY_REQUIRED");List<Object> fields=new ArrayList<>();for(int i=0;i<c.getColumnCount();i++)fields.add(c.isNull(i)?null:c.getString(i));result.put(name,JsonR27.write(fields));}}
        if(!result.keySet().containsAll(Arrays.asList("owner","feature_abi","dataset_hash","trained_through","state_hash","artifact","updated_at")))throw new IllegalStateException("R31_STARTUP_CHECKPOINT_SOURCE_CONTRACT");
        result.put("\u0000CREATE_SQL",create);return result;
    }
    private static void checkpoint(BudgetTokenSU1 budget)throws IOException{if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("R31_STARTUP_CHECKPOINT_CANCELLED");if(budget!=null)budget.checkpointOrYield();}
    private static final class Match {
        final String owner,hash;final long row,bytes;
        Match(String owner,long row,String hash,long bytes){this.owner=owner;this.row=row;this.hash=hash;this.bytes=bytes;}
    }
    public static final class Prepared {
        private final String database;private final long revision,schemaVersion;private final LinkedHashMap<String,String> columnContract;
        private final List<Match> matches;public final int sourceRows,examinedOriginalRows;public final long comparedOriginalBytes;
        private Prepared(String database,long revision,long schemaVersion,LinkedHashMap<String,String> columns,List<Match> matches,int sourceRows,int examined,long bytes){this.database=database;this.revision=revision;this.schemaVersion=schemaVersion;columnContract=new LinkedHashMap<>(columns);this.matches=Collections.unmodifiableList(new ArrayList<>(matches));this.sourceRows=sourceRows;examinedOriginalRows=examined;comparedOriginalBytes=bytes;}
        /** Small actual SQL guards only. Never opens a CAS file or selects artifact. */
        public void requireWithin(SQLiteDatabase db){
            if(!db.inTransaction())throw new IllegalStateException("R31_STARTUP_CHECKPOINT_COMMIT_WRITER_REQUIRED");
            if(!database.equals(identity(db))||!ready(db)||revision!=generation(db)||schemaVersion!=schema(db)||!columnContract.equals(columns(db)))throw new IllegalStateException("R31_STARTUP_CHECKPOINT_SOURCE_CHANGED");
            for(Match match:matches)try(Cursor c=db.rawQuery("SELECT 1 FROM r31_keep_original WHERE archive_table=? AND row_id=? AND row_hash=? AND bytes=? AND state='MIGRATED_ORIGINAL_RETAINED'",new String[]{ARCHIVE,Long.toString(match.row),match.hash,Long.toString(match.bytes)})){if(!c.moveToFirst())throw new IllegalStateException("R31_STARTUP_CHECKPOINT_CAS_METADATA_CHANGED");}
        }
        /** Caller adds AND NOT (<this>) to the original complete legacy equality
         * predicate, in the same writer callback immediately after requireWithin. */
        public String excludeSql(SQLiteDatabase db,String incomingAlias,List<Object> bindings){
            requireWithin(db);if(incomingAlias==null||!incomingAlias.matches("[A-Za-z_][A-Za-z0-9_]*"))throw new IllegalArgumentException("R31_STARTUP_CHECKPOINT_ALIAS");
            StringJoiner terms=new StringJoiner(" OR ");for(Match match:matches){terms.add("(typeof("+incomingAlias+".\"owner\")='text' AND "+incomingAlias+".\"owner\" IS ?)");bindings.add(match.owner);}return matches.isEmpty()?"0":terms.toString();
        }
        /** Called after the original INSERT/changes/boot marker, in that same
         * transaction. This records actual consumption, with no model payload. */
        public void recordWithin(SQLiteDatabase db,String processNonce,long actualNewRows){
            requireWithin(db);if(processNonce==null||processNonce.isEmpty()||actualNewRows<0||actualNewRows>sourceRows-matches.size())throw new IllegalArgumentException("R31_STARTUP_CHECKPOINT_RECEIPT_COUNTS");
            try(Cursor c=db.rawQuery("SELECT retained_rows FROM r30_retention_boot_checkpoint WHERE process_nonce=?",new String[]{processNonce})){if(!c.moveToFirst()||c.getLong(0)!=actualNewRows)throw new IllegalStateException("R31_STARTUP_CHECKPOINT_BOOT_RECEIPT_REQUIRED");}
            List<Object> refs=new ArrayList<>();for(Match match:matches){Map<String,Object> ref=new LinkedHashMap<>();ref.put("archive",ARCHIVE);ref.put("rowId",match.row);ref.put("rowHash",match.hash);ref.put("bytes",match.bytes);refs.add(ref);}
            db.execSQL("INSERT INTO r31_startup_checkpoint_receipt VALUES(?,?,?,?,?,?,?,?,?,?,?)",new Object[]{processNonce,revision,schemaVersion,StatsR27.sha256(JsonR27.write(columnContract)),sourceRows,matches.size(),actualNewRows,examinedOriginalRows,comparedOriginalBytes,JsonR27.write(refs),System.currentTimeMillis()});
        }
        public Map<String,Object> evidence(){Map<String,Object> out=new LinkedHashMap<>();out.put("version",VERSION);out.put("scope","ACTUAL_COMPLETE_TYPED_SOURCE_CAS_COMPARISON_NOT_STATE_HASH_EQUALITY");out.put("sourceRows",sourceRows);out.put("matchedSourceRows",matches.size());out.put("examinedOriginalRows",examinedOriginalRows);out.put("comparedOriginalBytes",comparedOriginalBytes);out.put("sourceGeneration",revision);out.put("sourceSchemaVersion",schemaVersion);out.put("writerCasReads",0);out.put("permanentPayloadCopies",0);out.put("wholeApplicationOrNaturalQualityProven",false);return out;}
    }
    /** Bounded read-only projection of actual committed startup consumption.
     * Missing/new schema never becomes zero or a whole-application PASS. */
    public static Map<String,Object> report(SQLiteDatabase db){
        Map<String,Object> out=new LinkedHashMap<>();out.put("version",VERSION);out.put("scope","LAST_16_REAL_PROCESS_STARTUP_CHECKPOINT_RECEIPTS");out.put("readOnly",true);out.put("naturalQuality","NOT_PROVEN");
        if(!exists(db,"r31_startup_checkpoint_receipt")){out.put("status","NOT_READY_STARTUP_CHECKPOINT_RECEIPTS");out.put("receipts",null);return out;}
        List<Object> rows=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT process_nonce,source_generation,source_schema_version,column_contract_hash,source_rows,cas_matched_rows,actual_new_archive_rows,compared_original_rows,compared_original_bytes,committed_at FROM r31_startup_checkpoint_receipt ORDER BY committed_at DESC,process_nonce LIMIT 16",null)){while(c.moveToNext()){Map<String,Object> row=new LinkedHashMap<>();for(int i=0;i<c.getColumnCount();i++)row.put(c.getColumnName(i),i==0||i==3?c.getString(i):c.getLong(i));rows.add(row);}}
        out.put("status",rows.isEmpty()?"NO_COMMITTED_CAS_AWARE_STARTUP_OBSERVED":"ACTUAL_COMMITTED_STARTUP_CONSUMPTION");out.put("receipts",rows);return out;
    }
    public static Prepared prepare(SQLiteDatabase read,File verifiedCasRoot)throws IOException{return prepare(read,verifiedCasRoot,null);}
    public static Prepared prepare(SQLiteDatabase read,File verifiedCasRoot,BudgetTokenSU1 budget)throws IOException{
        if(read.inTransaction())throw new IllegalStateException("R31_STARTUP_CHECKPOINT_PREPARE_OUTSIDE_WRITER");if(!ready(read))throw new DatabaseCoordinator.SchemaPendingR31("R31_STARTUP_CHECKPOINT","SOURCE_GUARD_NOT_READY");
        String database=identity(read);long version=schema(read),revision=generation(read);LinkedHashMap<String,String> contract=columns(read);LinkedHashSet<String> names=new LinkedHashSet<>(contract.keySet());names.remove("\u0000CREATE_SQL");
        if(!exists(read,"r31_keep_original"))throw new DatabaseCoordinator.SchemaPendingR31("R31_STARTUP_CHECKPOINT","CAS_METADATA_NOT_READY");
        LinkedHashSet<String> owners=new LinkedHashSet<>();try(Cursor c=read.rawQuery("SELECT typeof(owner),owner FROM r29_model_checkpoint WHERE length(artifact)>0 ORDER BY owner LIMIT 65",null)){while(c.moveToNext()){if(!"text".equals(c.getString(0))||c.getString(1)==null||c.getString(1).length()>4096)throw new IOException("R31_STARTUP_CHECKPOINT_OWNER_STORAGE_CLASS_OR_BOUND");owners.add(c.getString(1));if(owners.size()>MAX_OWNERS)throw new IOException("R31_STARTUP_CHECKPOINT_SOURCE_ROW_BOUND");}}
        File root=verifiedCasRoot.getCanonicalFile();if(!root.isDirectory())throw new IOException("R31_STARTUP_CHECKPOINT_CAS_ROOT_UNAVAILABLE");
        List<Match> matches=new ArrayList<>();Set<String> found=new HashSet<>();long after=0,totalBytes=0;int examined=0;
        MemoryBudgetR31.Admission admission=MemoryBudgetR31.processBudget().tryAcquire("startup-checkpoint-typed-compare",MemoryBudgetR31.Kind.CHECKPOINT,1L<<20,()->Thread.currentThread().isInterrupted());if(!admission.acquired())throw new MemoryBudgetR31.CapacityWait(admission.reason);
        try(MemoryBudgetR31.Lease memory=admission.lease){
            while(found.size()<owners.size()){
                checkpoint(budget);List<Match> page=new ArrayList<>();
                try(Cursor c=read.rawQuery("SELECT row_id,row_hash,bytes FROM r31_keep_original WHERE archive_table=? AND state='MIGRATED_ORIGINAL_RETAINED' AND row_id>? ORDER BY row_id LIMIT 64",new String[]{ARCHIVE,Long.toString(after)})){while(c.moveToNext()){String hash=c.getString(1);long size=c.getLong(2);if(c.getLong(0)<=after||hash==null||!hash.matches("[0-9a-f]{64}")||size<1||size>KeepMigrationStoreR31.MAX_ROW_BYTES)throw new IOException("R31_STARTUP_CHECKPOINT_CAS_METADATA");page.add(new Match(null,c.getLong(0),hash,size));}}
                if(page.isEmpty())break;
                for(Match original:page){checkpoint(budget);if(++examined>MAX_CAS_ROWS||totalBytes>KeepMigrationStoreR31.MAX_ROOT_BYTES-original.bytes)throw new IOException("R31_STARTUP_CHECKPOINT_CAS_SCAN_BOUND");totalBytes+=original.bytes;after=original.row;
                    String owner=compare(read,root,original,names,owners,found,budget);if(owner!=null){found.add(owner);matches.add(new Match(owner,original.row,original.hash,original.bytes));}if(found.size()==owners.size())break;
                }
            }
        }
        if(!database.equals(identity(read))||revision!=generation(read)||version!=schema(read)||!contract.equals(columns(read)))throw new IOException("R31_STARTUP_CHECKPOINT_SOURCE_CHANGED_DURING_PREPARATION");definitions(read);
        return new Prepared(database,revision,version,contract,matches,owners.size(),examined,totalBytes);
    }
    private static String compare(SQLiteDatabase read,File root,Match original,Set<String> names,Set<String> owners,Set<String> found,BudgetTokenSU1 budget)throws IOException{
        File file=new File(root,original.hash+".row");if(!file.getAbsoluteFile().equals(file.getCanonicalFile())||!file.isFile()||file.length()!=original.bytes)throw new IOException("R31_STARTUP_CHECKPOINT_CAS_FILE_IDENTITY");
        MessageDigest digest;try{digest=MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
        String owner=null;boolean equal=true;Set<String> fields=new HashSet<>();byte[] buffer=new byte[CHUNK];
        try(DataInputStream in=new DataInputStream(new BufferedInputStream(new DigestInputStream(new FileInputStream(file),digest),CHUNK))){
            if(!KeepMigrationStoreR31.VERSION.equals(in.readUTF())||!ARCHIVE.equals(in.readUTF())||in.readLong()!=original.row)throw new IOException("R31_STARTUP_CHECKPOINT_CAS_HEADER");int count=in.readInt();if(count<1||count>256)throw new IOException("R31_STARTUP_CHECKPOINT_CAS_COLUMN_BOUND");
            for(int i=0;i<count;i++){
                checkpoint(budget);String name=in.readUTF(),type=in.readUTF();if(name.length()>512||!fields.add(name))throw new IOException("R31_STARTUP_CHECKPOINT_CAS_DUPLICATE_COLUMN");boolean source=names.contains(name);
                if((name.equals("__r30_id")||name.equals("__r30_at"))&&!type.equals("integer")||name.equals("__r30_reason")&&!type.equals("text"))throw new IOException("R31_STARTUP_CHECKPOINT_CAS_METADATA_STORAGE_CLASS");
                if(name.equals("owner")){
                    if(!type.equals("text"))throw new IOException("R31_STARTUP_CHECKPOINT_CAS_OWNER_TYPE");long length=in.readLong();if(length<0||length>65536)throw new IOException("R31_STARTUP_CHECKPOINT_OWNER_BOUND");byte[] bytes=new byte[(int)length];in.readFully(bytes);owner=java.nio.charset.StandardCharsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes)).toString();if(!owners.contains(owner)||found.contains(owner))equal=false;
                    if(equal)try(Cursor c=read.rawQuery("SELECT typeof(owner),CAST(owner AS BLOB) FROM r29_model_checkpoint WHERE owner=?",new String[]{owner})){if(!c.moveToFirst()||!"text".equals(c.getString(0))||!Arrays.equals(bytes,c.getBlob(1)))equal=false;}continue;
                }
                String actualType=null;long actualLength=-1;
                if(source&&owner!=null&&equal)try(Cursor c=read.rawQuery("SELECT typeof("+q(name)+"),length(CAST("+q(name)+" AS BLOB)) FROM r29_model_checkpoint WHERE owner=?",new String[]{owner})){if(!c.moveToFirst())equal=false;else{actualType=c.getString(0);actualLength=c.isNull(1)?-1:c.getLong(1);if(!type.equals(actualType))equal=false;}}
                if(source&&owner==null)equal=false;
                if(type.equals("null"))continue;
                if(type.equals("integer")||type.equals("real")){long stored=in.readLong();if(source&&equal)try(Cursor c=read.rawQuery("SELECT "+q(name)+" FROM r29_model_checkpoint WHERE owner=?",new String[]{owner})){if(!c.moveToFirst()||stored!=(type.equals("integer")?c.getLong(0):Double.doubleToRawLongBits(c.getDouble(0))))equal=false;}continue;}
                if(!type.equals("text")&&!type.equals("blob"))throw new IOException("R31_STARTUP_CHECKPOINT_CAS_STORAGE_CLASS");long length=in.readLong();if(length<0||length>KeepMigrationStoreR31.MAX_ROW_BYTES)throw new IOException("R31_STARTUP_CHECKPOINT_CAS_VALUE_BOUND");if(source&&equal&&length!=actualLength)equal=false;
                for(long offset=0;offset<length;offset+=CHUNK){checkpoint(budget);int n=(int)Math.min(CHUNK,length-offset);in.readFully(buffer,0,n);
                    if(source&&equal)try(Cursor c=read.rawQuery("SELECT substr(CAST("+q(name)+" AS BLOB),?,?) FROM r29_model_checkpoint WHERE owner=?",new String[]{Long.toString(offset+1),Integer.toString(n),owner})){if(!c.moveToFirst())equal=false;else{byte[] actual=c.getBlob(0);if(actual==null||actual.length!=n)equal=false;else for(int k=0;k<n;k++)if(actual[k]!=buffer[k]){equal=false;break;}}}
                }
            }
            if(in.read()!=-1)throw new IOException("R31_STARTUP_CHECKPOINT_CAS_TRAILING_BYTES");
        }
        StringBuilder hash=new StringBuilder();for(byte value:digest.digest())hash.append(String.format(Locale.ROOT,"%02x",value&255));if(!original.hash.equals(hash.toString()))throw new IOException("R31_STARTUP_CHECKPOINT_CAS_HASH");
        Set<String> expected=new HashSet<>(names);expected.addAll(Arrays.asList("__r30_id","__r30_at","__r30_reason"));return equal&&owner!=null&&fields.equals(expected)?owner:null;
    }
}
