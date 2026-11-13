package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** Production adapter for the original keep rows. File preparation precedes the
 * short fenced metadata transaction. Historical archives have no sealed row
 * digest: their baseline is explicitly a current immutable-owner observation,
 * followed by a second independent pass, never an invented historical proof.
 * All original SQLite storage classes and full BLOB bytes survive the codec. */
public final class KeepMigrationStoreR31 implements KeepMigrationR31.Store {
    public static final String VERSION="R31_KEEP_SQLITE_CAS_1";
    public static final int CHUNK=65536,MAX_ROW_BYTES=64<<20;
    public static final long MAX_ROOT_BYTES=256L<<20;
    public static final String[] DDL={
        "CREATE TABLE IF NOT EXISTS r31_keep_checkpoint(archive_table TEXT PRIMARY KEY,revision INTEGER NOT NULL,state_json TEXT NOT NULL,updated_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r31_keep_original(archive_table TEXT NOT NULL,row_id INTEGER NOT NULL,row_hash TEXT NOT NULL,bytes INTEGER NOT NULL,checkpoint_refs_json TEXT NOT NULL,state TEXT NOT NULL,captured_at INTEGER NOT NULL,migrated_at INTEGER NOT NULL DEFAULT 0,PRIMARY KEY(archive_table,row_id))",
        "CREATE INDEX IF NOT EXISTS idx_r31_keep_original_state ON r31_keep_original(archive_table,state,row_id)",
        "CREATE INDEX IF NOT EXISTS idx_r31_keep_original_hash ON r31_keep_original(row_hash)"
    };
    public interface Work {void run(SQLiteDatabase db);}
    public interface Writer {void write(String operation,Work work);}
    private final SQLiteDatabase read;private final File root;private final Writer writer;
    private final Map<Long,Prepared> prepared=new LinkedHashMap<>();
    private String active,oldJson;private long revision=-1,upper,lower;private Map<String,Object> loaded;
    static final class Prepared {final long id,bytes;final String hash,refs;Prepared(long id,long bytes,String hash,String refs){this.id=id;this.bytes=bytes;this.hash=hash;this.refs=refs;}}
    public KeepMigrationStoreR31(SQLiteDatabase read,File root,Writer writer)throws IOException {
        this.read=read;this.root=root.getCanonicalFile();this.writer=writer;
        if(read.inTransaction())throw new IllegalStateException("R31_KEEP_PREPARE_INSIDE_WRITER");
        if(!this.root.isDirectory()&&!this.root.mkdirs())throw new IOException("R31_KEEP_CAS_DIRECTORY");registerLiveRoot(read,this.root);
    }
    private static String q(String s){if(s==null||!s.matches("[A-Za-z_][A-Za-z0-9_]*"))throw new IllegalArgumentException("R31_KEEP_IDENTIFIER");return '"'+s+'"';}
    static String source(String archive){if(archive==null||!archive.startsWith("r30_keep_")||!ResearchRetentionR30.PROTECTED_TABLES.contains(archive.substring(9)))throw new IllegalArgumentException("R31_KEEP_NOT_REGISTERED");return archive.substring(9);}
    public static boolean ready(SQLiteDatabase db){
        try(Cursor c=db.rawQuery("SELECT count(*) FROM sqlite_master WHERE type='table' AND name IN ('r31_keep_checkpoint','r31_keep_original')",null)){return c.moveToFirst()&&c.getInt(0)==2;}
    }
    static long scalar(SQLiteDatabase db,String sql,String...args){try(Cursor c=db.rawQuery(sql,args)){if(!c.moveToFirst())throw new IllegalStateException("R31_KEEP_SCALAR_UNAVAILABLE");return c.getLong(0);}}
    @SuppressWarnings("unchecked") public Map<String,Object> checkpoint(String table){
        source(table);if(table.equals(active))return loaded==null?null:new LinkedHashMap<>(loaded);
        active=table;prepared.clear();revision=-1;oldJson=null;loaded=null;
        try(Cursor c=read.rawQuery("SELECT revision,state_json FROM r31_keep_checkpoint WHERE archive_table=?",new String[]{table})){if(c.moveToFirst()){revision=c.getLong(0);oldJson=c.getString(1);Object parsed=JsonR27.parse(oldJson);if(!(parsed instanceof Map))throw new IllegalStateException("R31_KEEP_CHECKPOINT_CODEC");loaded=new LinkedHashMap<>((Map<String,Object>)parsed);}}
        long latest=scalar(read,"SELECT coalesce(max(__r30_id),0) FROM "+q(table));
        upper=loaded==null?latest:((Number)loaded.get("upperRowId")).longValue();lower=number(loaded,"lowerRowId",0);
        if(loaded!=null&&"SWITCH".equals(loaded.get("stage"))&&latest>upper){lower=upper;upper=latest;loaded=new LinkedHashMap<>(loaded);loaded.putAll(KeepMigrationR31.cp(KeepMigrationR31.Stage.VERIFY_HASH,-1,0,0));loaded.put("lowerRowId",lower);loaded.put("upperRowId",upper);loaded.put("verificationPhase","CAPTURE");loaded.put("verifyCursor",lower);loaded.put("rollingDigest",StatsR27.sha256(VERSION+"|"+table+"|"+lower+"|"+upper));loaded.remove("blockedReason");}
        return loaded==null?null:new LinkedHashMap<>(loaded);
    }
    /** Intentionally unavailable in production: the adapter must use keyset pages. */
    public List<Long> rowIds(String table){throw new IllegalStateException("R31_KEEP_FULL_TABLE_ENUMERATION_FORBIDDEN");}
    public List<Long> page(String table,long cursor,int limit){
        checkpoint(table);if(limit<1||limit>KeepMigrationR31.PAGE_ROWS)throw new IllegalArgumentException("R31_KEEP_PAGE_BOUND");
        // One original per slice bounds a large legacy checkpoint independently
        // of the number of archived rows; BLOB reads are always <=64 KiB.
        List<Long> ids=new ArrayList<>();try(Cursor c=read.rawQuery("SELECT __r30_id FROM "+q(table)+" WHERE __r30_id>? AND __r30_id<=? ORDER BY __r30_id LIMIT 1",new String[]{Long.toString(Math.max(lower,cursor)),Long.toString(upper)})){if(c.moveToFirst())ids.add(c.getLong(0));}return ids;
    }
    public boolean pageComplete(String table,long cursor,int size){return size==0||cursor>=upper;}
    public long lastRowId(String table){checkpoint(table);return upper==0?-1:upper;}
    private List<String> names(String table){List<String> names=new ArrayList<>();try(Cursor c=read.rawQuery("PRAGMA table_info("+q(table)+")",null)){while(c.moveToNext()){if(names.size()>=256)throw new IllegalStateException("R31_KEEP_COLUMN_BOUND");names.add(c.getString(1));}}if(names.isEmpty())throw new IllegalStateException("R31_KEEP_SOURCE_NOT_PRESENT");return names;}
    private static String hex(byte[] value){StringBuilder b=new StringBuilder();for(byte v:value)b.append(String.format(Locale.ROOT,"%02x",v&255));return b.toString();}
    private static MessageDigest sha(){try{return MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException e){throw new AssertionError(e);}}
    private static void syncDirectory(File directory)throws IOException{java.io.FileDescriptor fd=null;try{fd=android.system.Os.open(directory.getPath(),android.system.OsConstants.O_RDONLY,0);android.system.Os.fsync(fd);}catch(android.system.ErrnoException e){throw new IOException("R31_KEEP_DIRECTORY_FSYNC",e);}finally{if(fd!=null)try{android.system.Os.close(fd);}catch(android.system.ErrnoException ignored){}}}
    private static String hash(File file)throws IOException{MessageDigest digest=sha();try(InputStream in=new FileInputStream(file)){byte[] b=new byte[CHUNK];long bytes=0;int n;while((n=in.read(b))!=-1){bytes+=n;if(bytes>MAX_ROW_BYTES)throw new IOException("R31_KEEP_ROW_BYTE_BOUND");ExecutionBudgetR26.poll();digest.update(b,0,n);}}return hex(digest.digest());}
    private File artifact(String hash)throws IOException{if(hash==null||!hash.matches("[0-9a-f]{64}"))throw new IOException("R31_KEEP_HASH");return new File(root,hash+".row");}
    private Prepared prepare(String table,long id)throws IOException {
        MemoryBudgetR31.Admission capacity=MemoryBudgetR31.processBudget().tryAcquire("keep-original-prepare",MemoryBudgetR31.Kind.CHECKPOINT,1L<<20,()->Thread.currentThread().isInterrupted());if(!capacity.acquired())throw new MemoryBudgetR31.CapacityWait(capacity.reason);
        try(MemoryBudgetR31.Lease memory=capacity.lease){return prepareReserved(table,id);}
    }
    private static void requireRootCapacity(File directory,long incoming)throws IOException{
        long bytes=0;int count=0;for(File file:DirectoryCursorR31.children(directory,16384)){if(!file.isFile()||!file.getAbsoluteFile().equals(file.getCanonicalFile()))throw new IOException("R31_KEEP_UNKNOWN_CAS_ENTRY");bytes=Math.addExact(bytes,file.length());if(++count>=16384)throw new MemoryBudgetR31.CapacityWait(MemoryBudgetR31.WaitReason.DECLARED_CAPACITY);}
        if(incoming<1||incoming>MAX_ROW_BYTES||bytes>MAX_ROOT_BYTES-incoming)throw new MemoryBudgetR31.CapacityWait(MemoryBudgetR31.WaitReason.DECLARED_CAPACITY);
    }
    private Prepared prepareReserved(String table,long id)throws IOException {
        List<String> names=names(table);String known=null;try(Cursor c=read.rawQuery("SELECT row_hash FROM r31_keep_original WHERE archive_table=? AND row_id=?",new String[]{table,Long.toString(id)})){if(c.moveToFirst())known=c.getString(0);}
        if(known==null){long estimate=65536;for(String name:names)try(Cursor c=read.rawQuery("SELECT typeof("+q(name)+"),length(CAST("+q(name)+" AS BLOB)) FROM "+q(table)+" WHERE __r30_id=?",new String[]{Long.toString(id)})){if(!c.moveToFirst())throw new IOException("R31_KEEP_ORIGINAL_DISAPPEARED");estimate=Math.addExact(estimate,c.getString(0).equals("integer")||c.getString(0).equals("real")?8:c.isNull(1)?0:c.getLong(1));}requireRootCapacity(root,estimate);}
        File temp=known==null?new File(root,".prepare-"+UUID.randomUUID()):null;MessageDigest digest=sha();Set<String> refs=new TreeSet<>();long total=0;
        try {
            try(OutputStream raw=temp==null?new OutputStream(){public void write(int value){}public void write(byte[] b,int offset,int length){}}:new FileOutputStream(temp);DataOutputStream out=new DataOutputStream(new DigestOutputStream(raw,digest))){
                out.writeUTF(VERSION);out.writeUTF(table);out.writeLong(id);out.writeInt(names.size());
                for(String name:names){
                    ExecutionBudgetR26.poll();out.writeUTF(name);String type;long length=0;
                    try(Cursor c=read.rawQuery("SELECT typeof("+q(name)+"),length(CAST("+q(name)+" AS BLOB)) FROM "+q(table)+" WHERE __r30_id=?",new String[]{Long.toString(id)})){if(!c.moveToFirst())throw new IOException("R31_KEEP_ORIGINAL_DISAPPEARED");type=c.getString(0);if(!c.isNull(1))length=c.getLong(1);}
                    out.writeUTF(type);
                    if(type.equals("null"))continue;
                    if(type.equals("integer")||type.equals("real")){
                        try(Cursor c=read.rawQuery("SELECT "+q(name)+" FROM "+q(table)+" WHERE __r30_id=?",new String[]{Long.toString(id)})){if(!c.moveToFirst())throw new IOException("R31_KEEP_ORIGINAL_DISAPPEARED");out.writeLong(type.equals("integer")?c.getLong(0):Double.doubleToRawLongBits(c.getDouble(0)));}total+=8;continue;
                    }
                    if(!type.equals("blob")&&!type.equals("text"))throw new IOException("R31_KEEP_STORAGE_CLASS");
                    total=Math.addExact(total,length);if(length<0||total>MAX_ROW_BYTES-65536)throw new IOException("R31_KEEP_ROW_BYTE_BOUND");out.writeLong(length);
                    for(long at=0;at<length;at+=CHUNK){
                        ExecutionBudgetR26.poll();byte[] chunk;
                        try(Cursor c=read.rawQuery("SELECT substr(CAST("+q(name)+" AS BLOB),?,?) FROM "+q(table)+" WHERE __r30_id=?",new String[]{Long.toString(at+1),Integer.toString((int)Math.min(CHUNK,length-at)),Long.toString(id)})){if(!c.moveToFirst())throw new IOException("R31_KEEP_ORIGINAL_DISAPPEARED");chunk=c.getBlob(0);}
                        if(chunk==null||chunk.length!=Math.min(CHUNK,length-at))throw new IOException("R31_KEEP_ORIGINAL_CHANGED");out.write(chunk);
                        if(at==0&&chunk.length==72&&CheckpointArtifactsR26.reference(chunk))refs.add(CheckpointArtifactsR26.referenceHash(chunk));
                    }
                }
                out.flush();if(raw instanceof FileOutputStream)((FileOutputStream)raw).getFD().sync();
            }
            if(temp!=null&&temp.length()>MAX_ROW_BYTES)throw new IOException("R31_KEEP_ROW_BYTE_BOUND");String key=hex(digest.digest());if(known!=null&&!known.equals(key))throw new IOException("R31_KEEP_ORIGINAL_CHANGED_AFTER_CAPTURE");File target=artifact(key);
            if(target.exists()){if(!hash(target).equals(key))throw new IOException("R31_KEEP_EXISTING_CAS_CORRUPT");}
            else {if(temp==null)throw new IOException("R31_KEEP_REFERENCED_ORIGINAL_MISSING");if(!temp.renameTo(target))throw new IOException("R31_KEEP_CAS_RENAME");syncDirectory(root);}
            if(!hash(target).equals(key))throw new IOException("R31_KEEP_CAS_READBACK");
            return new Prepared(id,target.length(),key,JsonR27.write(new ArrayList<>(refs)));
        } finally {if(temp!=null&&temp.exists()&&!temp.delete())throw new IOException("R31_KEEP_PREPARE_CLEANUP");}
    }
    static Prepared prepareValues(File root,String table,long id,String[] names,Object[] values)throws IOException{
        if(names.length!=values.length||names.length>256)throw new IOException("R31_KEEP_COLUMN_BOUND");File canonical=root.getCanonicalFile();if(!canonical.isDirectory()&&!canonical.mkdirs())throw new IOException("R31_KEEP_CAS_DIRECTORY");long estimate=65536;for(Object value:values)estimate+=value instanceof byte[]?((byte[])value).length:value instanceof String?3L*((String)value).length():8;requireRootCapacity(canonical,estimate);File temp=new File(canonical,".prepare-"+UUID.randomUUID());MessageDigest digest=sha();
        try{try(FileOutputStream raw=new FileOutputStream(temp);DataOutputStream out=new DataOutputStream(new DigestOutputStream(raw,digest))){out.writeUTF(VERSION);out.writeUTF(table);out.writeLong(id);out.writeInt(names.length);long total=0;
            for(int i=0;i<names.length;i++){q(names[i]);out.writeUTF(names[i]);Object value=values[i];if(value==null){out.writeUTF("null");continue;}if(value instanceof Byte||value instanceof Short||value instanceof Integer||value instanceof Long){out.writeUTF("integer");out.writeLong(((Number)value).longValue());continue;}if(value instanceof Float||value instanceof Double){out.writeUTF("real");out.writeLong(Double.doubleToRawLongBits(((Number)value).doubleValue()));continue;}
                byte[] bytes=value instanceof byte[]?(byte[])value:value instanceof String?((String)value).getBytes(StandardCharsets.UTF_8):null;if(bytes==null)throw new IOException("R31_KEEP_STORAGE_CLASS");total+=bytes.length;if(total>MAX_ROW_BYTES-65536)throw new IOException("R31_KEEP_ROW_BYTE_BOUND");out.writeUTF(value instanceof byte[]?"blob":"text");out.writeLong(bytes.length);out.write(bytes);
            }out.flush();raw.getFD().sync();}
            String key=hex(digest.digest());File target=new File(canonical,key+".row");if(target.exists()){if(!hash(target).equals(key))throw new IOException("R31_KEEP_EXISTING_CAS_CORRUPT");}else {if(!temp.renameTo(target))throw new IOException("R31_KEEP_CAS_RENAME");syncDirectory(canonical);}if(!hash(target).equals(key))throw new IOException("R31_KEEP_CAS_READBACK");return new Prepared(id,target.length(),key,"[]");
        }finally{if(temp.exists()&&!temp.delete())throw new IOException("R31_KEEP_PREPARE_CLEANUP");}
    }
    public String rowDigest(String table,long rowId){
        Prepared prior=prepared.get(rowId);if(prior!=null)return prior.hash;
        try{Prepared p=prepare(table,rowId);prepared.put(rowId,p);return p.hash;}catch(IOException e){throw new IllegalStateException("R31_KEEP_ORIGINAL_PREPARATION",e);}
    }
    public String recordedDigest(String table){checkpoint(table);return loaded==null?null:(String)loaded.get("observedBaselineDigest");}
    private static long number(Map<String,Object> m,String key,long fallback){return m!=null&&m.get(key) instanceof Number?((Number)m.get(key)).longValue():fallback;}
    /** Each capture/verify unit reads at most one bounded original; checkpoints
     * retain the rolling digest and sealed high-water independently of appends. */
    public Map<String,Object> verifyHashUnit(String table,Map<String,Object> cp){
        checkpoint(table);String phase=cp==null?"CAPTURE":String.valueOf(cp.get("verificationPhase"));long cursor=number(cp,"verifyCursor",-1);
        String rolling=cp==null?StatsR27.sha256(VERSION+"|"+table+"|"+lower+"|"+upper):(String)cp.get("rollingDigest");
        List<Long> ids=page(table,cursor,1);long last=cursor;
        for(long id:ids){String current=rowDigest(table,id);if(phase.equals("VERIFY"))try(Cursor c=read.rawQuery("SELECT row_hash FROM r31_keep_original WHERE archive_table=? AND row_id=?",new String[]{table,Long.toString(id)})){if(!c.moveToFirst()||!current.equals(c.getString(0)))throw new IllegalStateException("R31_KEEP_ORIGINAL_HASH_MISMATCH:"+table);}
            rolling=StatsR27.sha256(rolling+"|"+id+"="+current);last=id;}
        boolean end=ids.isEmpty()||last>=upper;
        Map<String,Object> next=cp==null?KeepMigrationR31.cp(KeepMigrationR31.Stage.VERIFY_HASH,-1,0,0):new LinkedHashMap<>(cp);
        next.put("storageVersion",VERSION);next.put("lowerRowId",lower);next.put("upperRowId",upper);next.put("evidenceOrigin","CURRENT_IMMUTABLE_OWNER_SNAPSHOT_NOT_HISTORICAL_SEAL");
        next.put("verificationPhase",phase);next.put("verifyCursor",last);next.put("rollingDigest",rolling);
        if(end&&phase.equals("CAPTURE")){next.put("observedBaselineDigest",rolling);next.put("verificationPhase","VERIFY");next.put("verifyCursor",lower);next.put("rollingDigest",StatsR27.sha256(VERSION+"|"+table+"|"+lower+"|"+upper));}
        else if(end){if(!Objects.equals(next.get("observedBaselineDigest"),rolling))throw new IllegalStateException("R31_KEEP_ORIGINAL_HASH_MISMATCH:"+table);next.put("stage",KeepMigrationR31.Stage.REGISTER_REFERENCE.name());next.put("verifiedOriginalRows",scalar(read,"SELECT count(*) FROM r31_keep_original WHERE archive_table=? AND row_id<=?",table,Long.toString(upper)));}
        return next;
    }
    public void commit(String table,Map<String,Object> checkpoint,Map<Long,String> referenced,List<Long> migrated,boolean switchTrigger){
        checkpoint(table);Map<String,Object> next=new LinkedHashMap<>();if(loaded!=null)next.putAll(loaded);next.putAll(checkpoint);
        next.put("lowerRowId",lower);next.put("upperRowId",upper);next.put("storageVersion",VERSION);next.put("evidenceOrigin","CURRENT_IMMUTABLE_OWNER_SNAPSHOT_NOT_HISTORICAL_SEAL");
        // Original rows remain available to all old readers until a schema-owned
        // reader/producer cutover has been declared. A prepared CAS copy alone
        // cannot authorize deleting an immutable archive or fake a DONE stamp.
        if(switchTrigger||KeepMigrationR31.Stage.SWITCH.name().equals(next.get("stage"))){next.put("stage",KeepMigrationR31.Stage.SWITCH.name());if(KeepArchiveRetirementR31.MODE.equals(KeepArchiveRetirementR31.mode(read,source(table)))){next.put("blockedReason",null);next.put("duplicateRetirement",KeepArchiveRetirementR31.MODE);next.put("newWriteDisposition","UNCHANGED_LEGACY_COPY_NOT_REFERENCE_ONLY");}else{next.put("blockedReason","WAIT_SCHEMA_OWNED_READER_PRODUCER_AND_P05_CUTOVER");next.put("newWriteDisposition","LEGACY_PROTECTED_COPY_UNTIL_AUTHORIZED_SWITCH");}}
        for(long id:migrated){String actual=rowDigest(table,id);try(Cursor c=read.rawQuery("SELECT row_hash FROM r31_keep_original WHERE archive_table=? AND row_id=?",new String[]{table,Long.toString(id)})){if(!c.moveToFirst()||!actual.equals(c.getString(0)))throw new IllegalStateException("R31_KEEP_MIGRATION_REFERENCE_CHANGED");}}
        final Map<Long,Object[]> mismatchValues=new LinkedHashMap<>();
        if(KeepMismatchReferenceR31.ARCHIVE.equals(table)&&KeepMismatchReferenceR31.ready(read))for(long id:migrated){try(Cursor c=read.rawQuery("SELECT "+KeepMismatchReferenceR31.COLUMNS+" FROM "+q(table)+" WHERE __r30_id=?",new String[]{Long.toString(id)})){if(!c.moveToFirst())throw new IllegalStateException("R31_KEEP_ORIGINAL_DISAPPEARED");mismatchValues.put(id,values(c));}}
        final long expected=revision;final String json=JsonR27.write(next);final List<Prepared> originals=new ArrayList<>(prepared.values());
        writer.write("R31_KEEP_MIGRATION_UNIT",db->{
            if(!db.inTransaction())throw new IllegalStateException("R31_KEEP_WRITER_REQUIRED");
            ResearchRetentionStoreR30.requireKeepMigrationSourceR31(db,table);
            try(Cursor c=db.rawQuery("SELECT revision,state_json FROM r31_keep_checkpoint WHERE archive_table=?",new String[]{table})){if(expected<0?c.moveToFirst():!c.moveToFirst()||c.getLong(0)!=expected||!Objects.equals(c.getString(1),oldJson))throw new IllegalStateException("R31_KEEP_CHECKPOINT_CAS");}
            for(Prepared p:originals){
                db.execSQL("INSERT OR IGNORE INTO r31_keep_original(archive_table,row_id,row_hash,bytes,checkpoint_refs_json,state,captured_at) VALUES(?,?,?,?,?,'CAPTURED',?)",new Object[]{table,p.id,p.hash,p.bytes,p.refs,System.currentTimeMillis()});
                try(Cursor c=db.rawQuery("SELECT row_hash,bytes,checkpoint_refs_json FROM r31_keep_original WHERE archive_table=? AND row_id=?",new String[]{table,Long.toString(p.id)})){if(!c.moveToFirst()||!p.hash.equals(c.getString(0))||p.bytes!=c.getLong(1)||!p.refs.equals(c.getString(2)))throw new IllegalStateException("R31_KEEP_ORIGINAL_IDENTITY_CONFLICT");}
            }
            for(Map.Entry<Long,String> ref:referenced.entrySet())db.execSQL("UPDATE r31_keep_original SET state='REFERENCED' WHERE archive_table=? AND row_id=? AND row_hash=? AND state='CAPTURED'",new Object[]{table,ref.getKey(),ref.getValue()});
            for(long id:migrated)db.execSQL("UPDATE r31_keep_original SET state='MIGRATED_ORIGINAL_RETAINED',migrated_at=? WHERE archive_table=? AND row_id=? AND state='REFERENCED'",new Object[]{System.currentTimeMillis(),table,id});
            for(Map.Entry<Long,Object[]> entry:mismatchValues.entrySet())KeepMismatchReferenceR31.indexLegacyWithin(db,entry.getKey(),prepared.get(entry.getKey()).hash,entry.getValue());
            db.execSQL("INSERT OR REPLACE INTO r31_keep_checkpoint VALUES(?,?,?,?)",new Object[]{table,expected+1,json,System.currentTimeMillis()});
        });revision=expected+1;oldJson=json;loaded=next;prepared.clear();
    }
    /** Rooted checkpoint references retained by migrated original rows. Reads
     * only compact validated metadata, never the large original payload. */
    public static Set<String> checkpointArtifactHashes(SQLiteDatabase db){
        Set<String> hashes=new TreeSet<>();if(!ready(db))return hashes;
        try(Cursor c=db.rawQuery("SELECT checkpoint_refs_json FROM r31_keep_original",null)){int n=0;while(c.moveToNext()){if(++n>200000)throw new IllegalStateException("R31_KEEP_ROOT_CENSUS_BOUND");Object parsed=JsonR27.parse(c.getString(0));if(!(parsed instanceof List))throw new IllegalStateException("R31_KEEP_ROOT_CODEC");for(Object h:(List<?>)parsed){if(!(h instanceof String)||!((String)h).matches("[0-9a-f]{64}"))throw new IllegalStateException("R31_KEEP_ROOT_HASH");hashes.add((String)h);}}}return Collections.unmodifiableSet(hashes);
    }
    public static final class OriginalRow {
        public final long id;public final Object[] values;public final String originalHash;
        OriginalRow(long id,Object[] values,String hash){this.id=id;this.values=values;originalHash=hash;}
    }
    private static final Map<String,Root> ROOTS=new LinkedHashMap<>();
    private static final class Root {final File directory;int leases;boolean live;Root(File directory){this.directory=directory;}}
    private static String databaseKey(SQLiteDatabase db)throws IOException{return new File(db.getPath()).getCanonicalPath();}
    public static final class ReaderRootLease implements AutoCloseable {private final String key;private boolean closed;ReaderRootLease(String key){this.key=key;}public void close(){synchronized(ROOTS){if(closed)return;closed=true;Root r=ROOTS.get(key);if(r==null||r.leases<1)throw new IllegalStateException("R31_KEEP_READER_ROOT_LEASE");if(--r.leases==0&&!r.live)ROOTS.remove(key);}}}
    private static Root root(String key,File directory)throws IOException{File canonical=directory.getCanonicalFile();if(!canonical.isDirectory())throw new IOException("R31_KEEP_READER_DIRECTORY_MISSING");Root old=ROOTS.get(key);if(old!=null&&!old.directory.equals(canonical))throw new IOException("R31_KEEP_READER_ROOT_CONFLICT");if(old==null){if(ROOTS.size()>=32)throw new IOException("R31_KEEP_READER_ROOT_CAPACITY");old=new Root(canonical);ROOTS.put(key,old);}return old;}
    public static void registerLiveRoot(SQLiteDatabase db,File directory)throws IOException{synchronized(ROOTS){root(databaseKey(db),directory).live=true;}}
    public static ReaderRootLease openReaderRoot(SQLiteDatabase snapshot,File verifiedDirectory)throws IOException{synchronized(ROOTS){String key=databaseKey(snapshot);root(key,verifiedDirectory).leases++;return new ReaderRootLease(key);}}
    public static void unregisterLiveRoot(SQLiteDatabase db)throws IOException{synchronized(ROOTS){String key=databaseKey(db);Root r=ROOTS.get(key);if(r!=null){r.live=false;if(r.leases==0)ROOTS.remove(key);}}}
    private static File originalsRoot(SQLiteDatabase db)throws IOException{synchronized(ROOTS){Root r=ROOTS.get(databaseKey(db));if(r==null)throw new IOException("R31_KEEP_READER_ROOT_NOT_REGISTERED");return r.directory;}}
    static Object[] values(Cursor c){Object[] row=new Object[c.getColumnCount()];for(int i=0;i<row.length;i++)row[i]=c.isNull(i)?null:c.getType(i)==Cursor.FIELD_TYPE_BLOB?c.getBlob(i):c.getType(i)==Cursor.FIELD_TYPE_INTEGER?c.getLong(i):c.getType(i)==Cursor.FIELD_TYPE_FLOAT?c.getDouble(i):c.getString(i);return row;}
    /** Real consumers use the complete CAS original when it has migrated. New
     * append-only rows still read through the exact legacy owner until capture. */
    public static Object[] readOriginal(SQLiteDatabase db,String archive,long id,String...selected){
        source(archive);String key=null;
        if(ready(db))try(Cursor c=db.rawQuery("SELECT row_hash FROM r31_keep_original WHERE archive_table=? AND row_id=? AND state='MIGRATED_ORIGINAL_RETAINED'",new String[]{archive,Long.toString(id)})){if(c.moveToFirst())key=c.getString(0);}
        if(key==null){Set<String> present=new HashSet<>();try(Cursor info=db.rawQuery("PRAGMA table_info("+q(archive)+")",null)){while(info.moveToNext())present.add(info.getString(1));}StringJoiner cols=new StringJoiner(",");for(String col:selected){if(!present.contains(col))throw new IllegalArgumentException("R31_KEEP_UNKNOWN_PROJECTION_COLUMN");cols.add(q(col));}try(Cursor c=db.rawQuery("SELECT "+cols+" FROM "+q(archive)+" WHERE __r30_id=?",new String[]{Long.toString(id)})){return c.moveToFirst()?values(c):null;}}
        if(db.inTransaction())throw new IllegalStateException("R31_KEEP_CAS_READ_REQUIRES_PREPARATION_OUTSIDE_WRITER");
        try{return readArtifact(originalsRoot(db),archive,id,key,selected);}catch(IOException e){throw new IllegalStateException("R31_KEEP_ORIGINAL_READER_INTEGRITY",e);}
    }
    /** Public for explicit snapshot readers: a directory must be supplied and
     * matched to the caller's snapshot; no directory is created by this reader. */
    public static Object[] readArtifact(File root,String archive,long id,String key,String...selected)throws IOException {
        if(key==null||!key.matches("[0-9a-f]{64}"))throw new IOException("R31_KEEP_HASH");File file=new File(root,key+".row");
        MemoryBudgetR31.Admission admission=MemoryBudgetR31.processBudget().tryAcquire("keep-original-reader",MemoryBudgetR31.Kind.CHECKPOINT,1L<<20,()->Thread.currentThread().isInterrupted());if(!admission.acquired())throw new MemoryBudgetR31.CapacityWait(admission.reason);
        try(MemoryBudgetR31.Lease memory=admission.lease){
        if(!file.isFile()||!hash(file).equals(key))throw new IOException("R31_KEEP_ORIGINAL_READBACK_HASH");
        Map<String,Integer> wanted=new HashMap<>();for(int i=0;i<selected.length;i++){q(selected[i]);if(wanted.put(selected[i],i)!=null)throw new IOException("R31_KEEP_DUPLICATE_PROJECTION");}
        Object[] result=new Object[selected.length];Set<String> found=new HashSet<>();
        try(DataInputStream in=new DataInputStream(new BufferedInputStream(new FileInputStream(file),CHUNK))){
            if(!VERSION.equals(in.readUTF())||!archive.equals(in.readUTF())||id!=in.readLong())throw new IOException("R31_KEEP_ORIGINAL_HEADER");int columns=in.readInt();if(columns<1||columns>256)throw new IOException("R31_KEEP_COLUMN_BOUND");
            for(int i=0;i<columns;i++){
                String column=in.readUTF(),type=in.readUTF();Integer index=wanted.get(column);Object value=null;
                if(type.equals("integer"))value=in.readLong();else if(type.equals("real"))value=Double.longBitsToDouble(in.readLong());
                else if(type.equals("text")||type.equals("blob")){long length=in.readLong();if(length<0||length>MAX_ROW_BYTES)throw new IOException("R31_KEEP_ROW_BYTE_BOUND");
                    if(index!=null){MemoryBudgetR31.Admission grow=memory.grow(256L+3L*length);if(!grow.acquired())throw new MemoryBudgetR31.CapacityWait(grow.reason);byte[] bytes=new byte[(int)length];in.readFully(bytes);value=type.equals("blob")?bytes:StandardCharsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes)).toString();}
                    else {long left=length;byte[] buffer=new byte[CHUNK];while(left>0){int n=in.read(buffer,0,(int)Math.min(left,buffer.length));if(n<0)throw new EOFException();left-=n;}}
                }else if(!type.equals("null"))throw new IOException("R31_KEEP_STORAGE_CLASS");
                if(index!=null){found.add(column);result[index]=value;}
            }
            if(in.read()!=-1||found.size()!=selected.length)throw new IOException("R31_KEEP_ORIGINAL_PROJECTION");
        }return result;}
    }
    public static long maximumOriginalId(SQLiteDatabase db,String archive){source(archive);long n=scalar(db,"SELECT coalesce(max(__r30_id),0) FROM "+q(archive));return ready(db)?Math.max(n,scalar(db,"SELECT coalesce(max(row_id),0) FROM r31_keep_original WHERE archive_table=?",archive)):n;}
    public static List<OriginalRow> originalRows(SQLiteDatabase db,String archive,long after,long upper,int limit,String...columns){
        source(archive);if(limit<1||limit>64)throw new IllegalArgumentException("R31_KEEP_READER_PAGE_BOUND");List<Long> ids=new ArrayList<>();String sql="SELECT __r30_id AS id FROM "+q(archive);
        if(ready(db))sql+=" UNION SELECT row_id FROM r31_keep_original WHERE archive_table=? AND state='MIGRATED_ORIGINAL_RETAINED'";
        sql="SELECT id FROM ("+sql+") WHERE id>? AND id<=? ORDER BY id LIMIT "+limit;
        String[] args=ready(db)?new String[]{archive,Long.toString(after),Long.toString(upper)}:new String[]{Long.toString(after),Long.toString(upper)};
        try(Cursor c=db.rawQuery(sql,args)){while(c.moveToNext())ids.add(c.getLong(0));}
        List<OriginalRow> rows=new ArrayList<>();for(long id:ids){Object[] values=readOriginal(db,archive,id,columns);if(values==null)throw new IllegalStateException("R31_KEEP_ORIGINAL_READER_DISAPPEARED");rows.add(new OriginalRow(id,values,null));}return rows;
    }
    public static final class ArtifactRef {
        public final String hash,scope;public final long bytes;
        ArtifactRef(String hash,long bytes){this.hash=hash;this.bytes=bytes;scope="COMPLETE_ORIGINAL_SQLITE_ROW_CAS";}
    }
    /** Read-only keyset page for the real full-report attachment owner. Include
     * every persisted state, including uncertain producer preparation, so an
     * interrupted publication cannot silently omit a still-needed original. */
    public static List<ArtifactRef> snapshotArtifactPage(SQLiteDatabase db,String afterHash,int limit){
        if(limit<1||limit>64||afterHash==null||!afterHash.isEmpty()&&!afterHash.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("R31_KEEP_SNAPSHOT_PAGE");
        List<ArtifactRef> page=new ArrayList<>();if(!ready(db))return page;
        try(Cursor c=db.rawQuery("SELECT row_hash,min(bytes),max(bytes) FROM r31_keep_original WHERE row_hash>? GROUP BY row_hash ORDER BY row_hash LIMIT "+limit,new String[]{afterHash})){
            while(c.moveToNext()){String key=c.getString(0);long bytes=c.getLong(1);if(key==null||!key.matches("[0-9a-f]{64}")||bytes<1||bytes>MAX_ROW_BYTES||bytes!=c.getLong(2))throw new IllegalStateException("R31_KEEP_SNAPSHOT_ARTIFACT_IDENTITY");page.add(new ArtifactRef(key,bytes));}
        }return Collections.unmodifiableList(page);
    }
    private static int registeredRootLeases(){synchronized(ROOTS){int total=0;for(Root r:ROOTS.values())total+=r.leases;return total;}}
    public static Map<String,Object> report(SQLiteDatabase db){
        Map<String,Object> out=new LinkedHashMap<>();out.put("version",VERSION);out.put("readOnly",true);out.put("originalEvidenceArtifactsDeleted",0);out.put("historicalSealInvented",false);out.put("maximumCasRootBytes",MAX_ROOT_BYTES);out.put("capacityReleaseCondition","Only unreferenced preparation or an owner-declared original lifecycle may release bytes; rooted originals are never deleted to meet the budget");List<Object> rows=new ArrayList<>();out.put("tables",rows);
        if(!ready(db)){out.put("state","SCHEMA_PENDING");return out;}
        try(Cursor c=db.rawQuery("SELECT archive_table,revision,state_json,updated_at FROM r31_keep_checkpoint ORDER BY archive_table LIMIT 64",null)){while(c.moveToNext()){Map<String,Object> row=new LinkedHashMap<>();row.put("archive",c.getString(0));row.put("revision",c.getLong(1));row.put("checkpoint",JsonR27.parse(c.getString(2)));row.put("updatedAt",c.getLong(3));rows.add(row);}}
        out.put("state",rows.isEmpty()?"NOT_STARTED":"CURRENT_PERSISTED_CHECKPOINTS");out.put("casOriginalRows",scalar(db,"SELECT count(*) FROM r31_keep_original"));out.put("casOriginalBytes",scalar(db,"SELECT coalesce(sum(bytes),0) FROM r31_keep_original"));out.put("cutoverState",KeepMismatchReferenceR31.mode(db));out.put("cutoverScope",KeepMismatchReferenceR31.SOURCE);out.put("otherTables","PER_TABLE_ACTUAL_CHECKPOINT_AND_PROTOCOL;UNCHANGED_SOURCE_COPY_REMAINS_EXPLICIT");out.put("registeredRootLeases",registeredRootLeases());List<Object> policies=new ArrayList<>();for(ResearchRetentionR30.TablePolicyR31 policy:ResearchRetentionR30.POLICIES_R31)policies.add(policy.json());out.put("policies",policies);List<Object> readerStates=new ArrayList<>();for(String source:ResearchRetentionR30.PROTECTED_TABLES){Map<String,Object> entry=new LinkedHashMap<>();entry.put("source",source);entry.put("keepReaders",KeepArchiveRetirementR31.readerContract(source));entry.put("sourceWriteProtocol",source.equals(KeepMismatchReferenceR31.SOURCE)?KeepMismatchReferenceR31.mode(db):source.equals(GapReferenceR31.SOURCE)&&GapReferenceR31.active(db)?GapReferenceR31.MODE:"UNCHANGED_LEGACY_COPY_NOT_REFERENCE_ONLY");entry.put("duplicateRetirement",KeepArchiveRetirementR31.mode(db,source));entry.put("blockedReason",source.equals("r29_model_checkpoint")&&!StartupCheckpointIdentityR31.ready(db)?"STARTUP_CHECKPOINT_TYPED_ORIGINAL_READER_SCHEMA_NOT_READY":null);readerStates.add(entry);}out.put("readerAndProducerStates",readerStates);out.put("gapReferenceProducer",GapReferenceR31.report(db));out.put("startupCheckpointOriginalConsumer",StartupCheckpointIdentityR31.report(db));if(KeepArchiveRetirementR31.ready(db)){out.put("sqliteDuplicateRowsRetired",scalar(db,"SELECT count(*) FROM r31_keep_row_retirement"));out.put("physicalBytesReleasedBySQLiteRetirement",0L);}return out;
    }
    public static long step(DbStore store,RuntimeCoordinatorR26.Claim claim,BudgetTokenSU1 budget)throws IOException {
        SQLiteDatabase read=store.readable();if(ready(read)){long retired=KeepArchiveRetirementR31.step(store,claim,budget);if(retired>=0)return retired;long mismatch=KeepMismatchReferenceR31.step(store,claim,budget);if(mismatch>=0)return mismatch;}if(!ready(read)){claim.reason("SCHEMA_PENDING:R31_KEEP_LIFECYCLE");return 0;}budget.checkpointOrYield();String table=null;
        try(Cursor c=read.rawQuery("SELECT a.archive_table,k.state_json FROM r30_retention_archive_index a LEFT JOIN r31_keep_checkpoint k ON k.archive_table=a.archive_table ORDER BY coalesce(k.updated_at,0),a.archive_table LIMIT 64",null)){
            while(c.moveToNext()){String candidate=c.getString(0);source(candidate);if(c.isNull(1)){table=candidate;break;}Map<?,?> state=(Map<?,?>)JsonR27.parse(c.getString(1));if(!"DONE".equals(state.get("stage"))&&(!"SWITCH".equals(state.get("stage"))||scalar(read,"SELECT coalesce(max(__r30_id),0) FROM "+q(candidate))>((Number)state.get("upperRowId")).longValue())){table=candidate;break;}}
        }
        if(table==null){claim.reason("R31_KEEP_WAIT_SCHEMA_OWNED_CUTOVER");return 0;}
        ResearchRetentionStoreR30.requireKeepMigrationSourceR31(read,table);
        Writer writer=(operation,work)->store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.RETENTION,operation),db->{RuntimeCoordinatorR26.requireFence(db,claim);work.run(db);RuntimeCoordinatorR26.requireFence(db,claim);return 0;});
        KeepMigrationStoreR31 adapter=new KeepMigrationStoreR31(read,new File(store.contextForR26().getFilesDir(),"r31-keep"),writer);KeepMigrationR31.Stage stage=KeepMigrationR31.step(adapter,table);claim.reason("R31_KEEP_ORIGINAL_UNIT:"+table+":"+stage);return 1;
    }
}
