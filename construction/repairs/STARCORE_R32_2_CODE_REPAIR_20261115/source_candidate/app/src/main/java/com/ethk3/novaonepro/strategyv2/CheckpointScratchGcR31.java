package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.security.*;
import java.util.*;

/** Recovery of abandoned checkpoint prepare scratch. The caller holds the
 * checkpoint root's nonblocking GC admission flag, no Prepared leases, and the
 * report-input exclusive lease. Only .tmp-UUID direct regular children qualify;
 * a checksum/size/mtime claim commits before IO, and a failed unlink stays
 * DELETING. Canonical hash.bin files are never considered by this owner. */
public final class CheckpointScratchGcR31 {
    private CheckpointScratchGcR31() {}
    public static final String VERSION="R31_CHECKPOINT_SCRATCH_GC_1",TABLE="r31_gc_checkpoint_scratch";
    public static final int PAGE=64,ROW_LIMIT=65536;
    public static final long MAX_FILE_BYTES=32L<<20,MAX_HASH_BYTES=64L<<20;
    private static final String NAME="\\.tmp-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
    public static String[] ddlR31(){return new String[]{
        "CREATE TABLE IF NOT EXISTS "+TABLE+"(name TEXT PRIMARY KEY,bytes INTEGER NOT NULL CHECK(bytes BETWEEN 0 AND 33554432),modified_at INTEGER NOT NULL,sha256 TEXT NOT NULL CHECK(length(sha256)=64),state TEXT NOT NULL CHECK(state IN ('DELETING','DELETED')),version INTEGER NOT NULL CHECK(version>=0),attempts INTEGER NOT NULL DEFAULT 0,released_bytes INTEGER NOT NULL DEFAULT 0 CHECK(released_bytes>=0),claimed_at INTEGER NOT NULL,updated_at INTEGER NOT NULL,reason TEXT NOT NULL,CHECK((state='DELETING' AND released_bytes=0) OR (state='DELETED' AND released_bytes=bytes)))",
        "CREATE TRIGGER IF NOT EXISTS r31_gc_scratch_identity BEFORE UPDATE ON "+TABLE+" WHEN NEW.name IS NOT OLD.name OR NEW.bytes IS NOT OLD.bytes OR NEW.modified_at IS NOT OLD.modified_at OR NEW.sha256 IS NOT OLD.sha256 OR NEW.claimed_at IS NOT OLD.claimed_at OR OLD.state='DELETED' OR NEW.version<>OLD.version+1 OR NEW.attempts<OLD.attempts BEGIN SELECT RAISE(ABORT,'R31_GC_SCRATCH_IDENTITY_OR_TERMINAL_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_gc_scratch_no_delete BEFORE DELETE ON "+TABLE+" BEGIN SELECT RAISE(ABORT,'R31_GC_SCRATCH_RECEIPT_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_gc_scratch_bound BEFORE INSERT ON "+TABLE+" WHEN NOT EXISTS(SELECT 1 FROM "+TABLE+" WHERE name=NEW.name) AND (SELECT count(*) FROM "+TABLE+")>=65536 BEGIN SELECT RAISE(ABORT,'DEPENDENCY:R31_GC_SCRATCH_RECEIPT_CAPACITY'); END"
    };}
    public static boolean ready(SQLiteDatabase db){
        try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{TABLE})){if(!c.moveToFirst())return false;}
        try(Cursor c=db.rawQuery("SELECT name,bytes,modified_at,sha256,state,version,attempts,released_bytes,claimed_at,updated_at,reason FROM "+TABLE+" LIMIT 0",null)){return c.getColumnCount()==11;}
    }
    public static final class Result {
        public long releasedBytes,physicalUnlinkedBytes,physicalUnlinkedFiles,claimed,retried,identityPreserved,hashVerifiedBytes;
        public String state="COMPLETED_BOUNDED_SCRATCH_SLICE";
        public Map<String,Object> json(){Map<String,Object> m=new LinkedHashMap<>();m.put("owner",VERSION);m.put("state",state);m.put("releasedBytesCommittedThisSlice",releasedBytes);m.put("physicalUnlinkedBytesThisSlice",physicalUnlinkedBytes);m.put("physicalUnlinkedFilesThisSlice",physicalUnlinkedFiles);m.put("claimedThisSlice",claimed);m.put("retriedThisSlice",retried);m.put("changedIdentitiesPreserved",identityPreserved);m.put("hashVerifiedBytesThisSlice",hashVerifiedBytes);m.put("canonicalArtifactsConsidered",0);return m;}
    }
    private static final class Identity {
        final String name,sha;final long bytes,modified,version;
        Identity(String name,String sha,long bytes,long modified,long version){this.name=name;this.sha=sha;this.bytes=bytes;this.modified=modified;this.version=version;}
    }
    private static File file(File root,String name)throws IOException {
        if(name==null||!name.matches(NAME))throw new IOException("R31_SCRATCH_NAME_NOT_ADMITTED");File file=new File(root,name);
        if(!file.getAbsoluteFile().equals(file.getCanonicalFile())||!root.equals(file.getCanonicalFile().getParentFile()))throw new IOException("R31_SCRATCH_CANONICAL_PATH_CHANGED");return file;
    }
    private static String sha(byte[] digest){char[] alphabet="0123456789abcdef".toCharArray(),out=new char[64];for(int i=0;i<digest.length;i++){out[2*i]=alphabet[(digest[i]>>>4)&15];out[2*i+1]=alphabet[digest[i]&15];}return new String(out);}
    private static Identity verify(File root,String name,Result work)throws IOException {
        File file=file(root,name);if(!file.isFile())throw new IOException("R31_SCRATCH_NOT_REGULAR");long bytes=file.length(),modified=file.lastModified();
        if(bytes<0||bytes>MAX_FILE_BYTES)throw new IOException("R31_SCRATCH_FILE_BOUND");if(bytes>MAX_HASH_BYTES-work.hashVerifiedBytes)return null;
        MessageDigest digest;try{digest=MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
        long count=0;try(InputStream input=new FileInputStream(file)){byte[] buffer=new byte[32768];int n;while((n=input.read(buffer))!=-1){ExecutionBudgetR26.poll();count+=n;work.hashVerifiedBytes+=n;if(count>bytes||work.hashVerifiedBytes>MAX_HASH_BYTES)throw new IOException("R31_SCRATCH_CHANGED_WHILE_HASHING");digest.update(buffer,0,n);}}
        if(count!=bytes||!file.isFile()||file.length()!=bytes||file.lastModified()!=modified)throw new IOException("R31_SCRATCH_CHANGED_WHILE_HASHING");
        return new Identity(name,sha(digest.digest()),bytes,modified,0);
    }
    private static void writer(SQLiteDatabase db){if(!db.inTransaction())throw new IllegalStateException("R31_SCRATCH_EXISTING_WRITER_REQUIRED");}
    private static int changes(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT changes()",null)){if(!c.moveToFirst())throw new IllegalStateException("R31_SCRATCH_CHANGES_UNKNOWN");return c.getInt(0);}}
    private static Identity pending(SQLiteDatabase db,String name){
        try(Cursor c=db.rawQuery("SELECT sha256,bytes,modified_at,version,state FROM "+TABLE+" WHERE name=?",new String[]{name})){
            if(!c.moveToFirst()||!"DELETING".equals(c.getString(4)))return null;return new Identity(name,c.getString(0),c.getLong(1),c.getLong(2),c.getLong(3));}
    }
    private static List<String> pending(SQLiteDatabase db,int count){List<String> names=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT name FROM "+TABLE+" WHERE state='DELETING' ORDER BY updated_at,name LIMIT "+count,null)){while(c.moveToNext())names.add(c.getString(0));}return names;}
    private static boolean known(SQLiteDatabase db,String name){try(Cursor c=db.rawQuery("SELECT 1 FROM "+TABLE+" WHERE name=?",new String[]{name})){return c.moveToFirst();}}
    private static void recordRetry(CheckpointArtifactsR26.GcWriterR31 writer,Identity claim,String reason,long at){
        writer.write("R31_GC_SCRATCH_RETRY",db->{writer(db);db.execSQL("UPDATE "+TABLE+" SET version=version+1,attempts=attempts+1,reason=?,updated_at=? WHERE name=? AND state='DELETING' AND version=? AND sha256=?",new Object[]{reason,at,claim.name,claim.version,claim.sha});if(changes(db)!=1)throw new IllegalStateException("DEPENDENCY:R31_SCRATCH_RETRY_SOURCE_CHANGED");return 0;});
    }
    private static void finish(SQLiteDatabase read,File root,CheckpointArtifactsR26.GcWriterR31 writer,String name,long at,Result result)throws IOException {
        Identity claim=pending(read,name);if(claim==null)return;File file;
        try{file=file(root,name);}catch(IOException changed){recordRetry(writer,claim,"PATH_IDENTITY_CHANGED_PRESERVED",at);result.identityPreserved++;return;}
        boolean gone=!file.exists();
        if(!gone){
            Identity actual;
            try{actual=verify(root,name,result);}catch(IOException changed){recordRetry(writer,claim,"FILE_IDENTITY_UNREADABLE_OR_CHANGED_PRESERVED",at);result.identityPreserved++;return;}
            if(actual==null){result.state="MORE_BOUNDED_SCRATCH_WORK";return;}
            if(actual.bytes!=claim.bytes||actual.modified!=claim.modified||!actual.sha.equals(claim.sha)){recordRetry(writer,claim,"FILE_IDENTITY_CHANGED_PRESERVED",at);result.identityPreserved++;return;}
            if(file.delete()){result.physicalUnlinkedBytes+=claim.bytes;result.physicalUnlinkedFiles++;}
            gone=!file.exists();
        }
        if(!gone){recordRetry(writer,claim,"UNLINK_FAILED_TOMBSTONE_RETRY",at);result.retried++;return;}
        CheckpointArtifactsR26.syncDirectory(root);
        result.releasedBytes+=writer.write("R31_GC_SCRATCH_UNLINK_COMMIT",db->{writer(db);Identity current=pending(db,name);if(current==null)return 0;
            if(current.version!=claim.version||current.bytes!=claim.bytes||current.modified!=claim.modified||!current.sha.equals(claim.sha))throw new IllegalStateException("DEPENDENCY:R31_SCRATCH_UNLINK_SOURCE_CHANGED");
            db.execSQL("UPDATE "+TABLE+" SET state='DELETED',version=version+1,released_bytes=bytes,reason='UNLINK_VERIFIED_ABSENT',updated_at=? WHERE name=? AND state='DELETING' AND version=?",new Object[]{at,name,claim.version});if(changes(db)!=1)throw new IllegalStateException("DEPENDENCY:R31_SCRATCH_UNLINK_CAS");return claim.bytes;
        });
    }
    /** One bounded production pass. A failed old tombstone cannot consume all
     * new-work slots. Reused names are preserved; this owner never admits a new
     * incarnation under a terminal scratch receipt's original identity. */
    public static Result step(SQLiteDatabase read,File directory,CheckpointArtifactsR26.GcWriterR31 writer,long now,long grace,int limit)throws IOException {
        if(read.inTransaction())throw new IllegalStateException("R31_SCRATCH_IO_INSIDE_TRANSACTION");if(!ready(read))throw new IOException("DEPENDENCY:R31_SCRATCH_SCHEMA_PENDING");
        if(grace<0||limit<1)throw new IllegalArgumentException("R31_SCRATCH_SLICE_ARGUMENTS");File root=directory.getCanonicalFile();if(!root.isDirectory())throw new IOException("R31_SCRATCH_ROOT_MISSING");Result result=new Result();int bound=Math.min(PAGE,limit),attempts=0;
        for(String name:pending(read,Math.max(1,bound/2))){ExecutionBudgetR26.poll();finish(read,root,writer,name,now,result);attempts++;}
        File[] files=com.ethk3.novaonepro.DirectoryCursorR31.children(root,16384);Arrays.sort(files,Comparator.comparing(File::getName));
        for(File file:files){ExecutionBudgetR26.poll();if(attempts>=bound||result.hashVerifiedBytes>=MAX_HASH_BYTES){result.state="MORE_BOUNDED_SCRATCH_WORK";break;}
            String name=file.getName();if(!name.matches(NAME)||known(read,name)||now<file.lastModified()||now-file.lastModified()<grace)continue;
            Identity original;
            try{original=verify(root,name,result);}catch(IOException changed){result.identityPreserved++;continue;}
            if(original==null){result.state="MORE_BOUNDED_SCRATCH_WORK";break;}attempts++;
            long accepted=writer.write("R31_GC_SCRATCH_CLAIM",db->{writer(db);if(known(db,name))return 0;db.execSQL("INSERT INTO "+TABLE+" VALUES(?,?,?,?,'DELETING',0,0,0,?,?,'CLAIMED_EXACT_SCRATCH_IDENTITY')",new Object[]{original.name,original.bytes,original.modified,original.sha,now,now});return 1;});
            if(accepted!=0){result.claimed++;finish(read,root,writer,name,now,result);}
        }
        return result;
    }
    /** Fixed fields and aggregates only. Missing schema is not zero completion. */
    public static Map<String,Object> report(SQLiteDatabase db){Map<String,Object> m=new LinkedHashMap<>();m.put("owner",VERSION);m.put("scope","ABANDONED_CHECKPOINT_TMP_UUID_FILES_ONLY");if(!ready(db)){m.put("state","SCHEMA_PENDING");return m;}
        try(Cursor c=db.rawQuery("SELECT state,count(*),sum(bytes),sum(released_bytes),sum(attempts) FROM "+TABLE+" GROUP BY state LIMIT 3",null)){while(c.moveToNext()){Map<String,Object> row=new LinkedHashMap<>();row.put("files",c.getLong(1));row.put("bytes",c.getLong(2));row.put("releasedBytes",c.getLong(3));row.put("attempts",c.getLong(4));m.put(c.getString(0),row);}}
        m.put("state","DURABLE_SCRATCH_RECEIPTS_NOT_A_HEALTH_VERDICT");return m;
    }
}
