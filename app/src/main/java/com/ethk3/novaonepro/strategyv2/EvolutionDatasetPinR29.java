package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DatabaseCoordinator;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Immutable replay inputs committed in the same research transaction as their evolution checkpoint.
 * Codec/restore work is pure and cooperative; SQL capture never opens or attaches another database. */
public final class EvolutionDatasetPinR29 {
    private EvolutionDatasetPinR29(){}
    public static final int CHUNK_BYTES=128*1024,MAX_BYTES=64*1024*1024,MAX_ROWS=1000000;
    public static final String META="r27_evolution_dataset_pin",CHUNKS="r27_evolution_dataset_chunk";
    private static final int MAGIC=0x52323944,VERSION=1,MAX_ISSUE_BYTES=1024*1024;
    public static final class Prepared {
        public final String datasetHash,hash;public final byte[] bytes;public final int rows;
        private final byte[] encoded;private final int rejected;
        private Prepared(String datasetHash,byte[] bytes,int rows,int rejected){this.datasetHash=datasetHash;this.encoded=bytes;this.bytes=bytes.clone();this.rows=rows;this.rejected=rejected;hash=sha(bytes);}
    }
    public static final class Stored {
        public final String datasetHash,hash;public final byte[] bytes;public final int rows;
        private final byte[] encoded;private final int rejected;
        private Stored(String datasetHash,String hash,byte[] bytes,int rows,int rejected){this.datasetHash=datasetHash;this.hash=hash;this.encoded=bytes;this.bytes=bytes.clone();this.rows=rows;this.rejected=rejected;}
    }
    private static void need(boolean ok,String code){if(!ok)throw new IllegalStateException("R29_DATASET_PIN_"+code);}
    private static boolean hash(String value){return value!=null&&value.matches("[0-9a-f]{64}");}
    private static String sha(byte[] bytes){try{MessageDigest digest=MessageDigest.getInstance("SHA-256");for(int at=0;at<bytes.length;at+=CHUNK_BYTES){CooperativeContinuationR26.poll();digest.update(bytes,at,Math.min(CHUNK_BYTES,bytes.length-at));}StringBuilder text=new StringBuilder(64);for(byte b:digest.digest())text.append(Character.forDigit((b>>>4)&15,16)).append(Character.forDigit(b&15,16));return text.toString();}catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}}
    /** No SQLite access: capture an exact private copy and encode it on the compute continuation. */
    public static Prepared prepare(ReplayDatasetR27 data){
        need(data!=null&&data.size()<=MAX_ROWS&&data.issues.length==data.size()&&data.ordinals.length==data.size(),"SOURCE_SHAPE");
        List<ReplayDatasetR27.Row> rows=new ArrayList<>(data.size());for(int i=0;i<data.size();i++){CooperativeContinuationR26.poll();rows.add(new ReplayDatasetR27.Row(data.issues[i],data.ordinals[i],data.draws[i]));}
        ReplayDatasetR27 exact=ReplayDatasetR27.pinnedExact(rows,data.rejectedRows);need(exact.datasetHash.equals(data.datasetHash),"SOURCE_HASH_CHANGED");
        try{ByteArrayOutputStream bytes=new ByteArrayOutputStream(Math.min(MAX_BYTES,Math.max(128,data.size()*40)));DataOutputStream out=new DataOutputStream(bytes);
            out.writeInt(MAGIC);out.writeInt(VERSION);out.writeInt(exact.size());out.writeInt(exact.rejectedRows);out.write(exact.datasetHash.getBytes(StandardCharsets.US_ASCII));
            for(int i=0;i<exact.size();i++){CooperativeContinuationR26.poll();String issue=exact.issues[i];if(issue==null)out.writeInt(-1);else{byte[] text=issue.getBytes(StandardCharsets.UTF_8);need(text.length<=MAX_ISSUE_BYTES&&issue.equals(new String(text,StandardCharsets.UTF_8)),"ISSUE_UTF8");out.writeInt(text.length);out.write(text);}out.writeLong(exact.ordinals[i]);for(int n:exact.draws[i])out.writeByte(n);need(bytes.size()<=MAX_BYTES,"BYTE_BUDGET");}
            out.flush();return new Prepared(exact.datasetHash,bytes.toByteArray(),exact.size(),exact.rejectedRows);
        }catch(IOException impossible){throw new IllegalStateException("R29_DATASET_PIN_ENCODE",impossible);}
    }
    public static void install(SQLiteDatabase db){
        need(db.inTransaction(),"SCHEMA_TRANSACTION_REQUIRED");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+META+"(dataset_hash TEXT PRIMARY KEY CHECK(length(dataset_hash)=64),format_version INTEGER NOT NULL,blob_sha256 TEXT NOT NULL CHECK(length(blob_sha256)=64),byte_count INTEGER NOT NULL CHECK(byte_count>=80),row_count INTEGER NOT NULL CHECK(row_count>=0),rejected_rows INTEGER NOT NULL CHECK(rejected_rows>=0),chunk_count INTEGER NOT NULL CHECK(chunk_count>0),created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+CHUNKS+"(dataset_hash TEXT NOT NULL,chunk_index INTEGER NOT NULL CHECK(chunk_index>=0),byte_count INTEGER NOT NULL CHECK(byte_count BETWEEN 1 AND "+CHUNK_BYTES+" AND byte_count=length(chunk_blob)),chunk_sha256 TEXT NOT NULL CHECK(length(chunk_sha256)=64),chunk_blob BLOB NOT NULL,PRIMARY KEY(dataset_hash,chunk_index))");
        for(String table:new String[]{META,CHUNKS}){
            for(String event:new String[]{"UPDATE","DELETE"})db.execSQL("CREATE TRIGGER IF NOT EXISTS "+table+"_no_"+event+" BEFORE "+event+" ON "+table+" BEGIN SELECT RAISE(ABORT,'R29_DATASET_PIN_IMMUTABLE'); END");
            String key=table.equals(META)?"dataset_hash=NEW.dataset_hash":"dataset_hash=NEW.dataset_hash AND chunk_index=NEW.chunk_index";
            db.execSQL("CREATE TRIGGER IF NOT EXISTS "+table+"_no_replace BEFORE INSERT ON "+table+" WHEN EXISTS(SELECT 1 FROM "+table+" WHERE "+key+") BEGIN SELECT RAISE(ABORT,'R29_DATASET_PIN_NO_REPLACE'); END");
        }
    }
    private static boolean table(SQLiteDatabase db,String name){try(Cursor c=db.rawQuery("SELECT 1 FROM main.sqlite_master WHERE type='table' AND name=?",new String[]{name})){return c.moveToFirst();}}
    private static void same(Prepared prepared,Stored stored){need(stored!=null&&prepared.datasetHash.equals(stored.datasetHash)&&prepared.hash.equals(stored.hash)&&prepared.rows==stored.rows&&prepared.rejected==stored.rejected&&Arrays.equals(prepared.encoded,stored.encoded),"EXISTING_IDENTITY_CONFLICT");}
    /** Adds only operations to the caller's indivisible cursor/snapshot transaction. Existing corrupt pins
     * fail before writes; no missing chunk is silently repaired and no historical pin is overwritten. */
    public static void append(ResearchWriteQueueR27.Batch batch,Prepared prepared){
        need(batch!=null&&prepared!=null&&hash(prepared.datasetHash),"APPEND_ARGUMENT");
        DatabaseCoordinator.WriteCallable<Integer> before=batch.before,after=batch.after;
        batch.before=db->{Integer prior=before==null?0:before.call(db);if(prior!=null&&prior<0)return prior;install(db);Stored existing=read(db,prepared.datasetHash);if(existing!=null)same(prepared,existing);return prior;};
        int count=(prepared.encoded.length+CHUNK_BYTES-1)/CHUNK_BYTES;
        batch.add("INSERT INTO "+META+" SELECT ?,1,?,?,?,?,?,? WHERE NOT EXISTS(SELECT 1 FROM "+META+" WHERE dataset_hash=?)",prepared.datasetHash,prepared.hash,prepared.encoded.length,prepared.rows,prepared.rejected,count,System.currentTimeMillis(),prepared.datasetHash);
        for(int i=0;i<count;i++){CooperativeContinuationR26.poll();byte[] chunk=Arrays.copyOfRange(prepared.encoded,i*CHUNK_BYTES,Math.min(prepared.encoded.length,(i+1)*CHUNK_BYTES));batch.add("INSERT INTO "+CHUNKS+" SELECT ?,?,?,?,? WHERE NOT EXISTS(SELECT 1 FROM "+CHUNKS+" WHERE dataset_hash=? AND chunk_index=?)",prepared.datasetHash,i,chunk.length,sha(chunk),chunk,prepared.datasetHash,i);}
        batch.after=db->{same(prepared,read(db,prepared.datasetHash));return after==null?0:after.call(db);};
    }
    /** DB capture phase, outside compute. Each CursorWindow cell is at most 128 KiB, including large pins. */
    public static Stored read(SQLiteDatabase db,String datasetHash){
        need(db!=null&&hash(datasetHash),"READ_ARGUMENT");boolean meta=table(db,META),chunks=table(db,CHUNKS);if(!meta&&!chunks)return null;need(meta&&chunks,"PARTIAL_SCHEMA");
        String hash;long bytes,rows,rejected,count;
        try(Cursor c=db.rawQuery("SELECT format_version,blob_sha256,byte_count,row_count,rejected_rows,chunk_count FROM "+META+" WHERE dataset_hash=?",new String[]{datasetHash})){
            if(!c.moveToFirst()){try(Cursor orphan=db.rawQuery("SELECT 1 FROM "+CHUNKS+" WHERE dataset_hash=? LIMIT 1",new String[]{datasetHash})){need(!orphan.moveToFirst(),"ORPHAN_CHUNKS");}return null;}
            need(c.getInt(0)==VERSION,"FORMAT");hash=c.getString(1);bytes=c.getLong(2);rows=c.getLong(3);rejected=c.getLong(4);count=c.getLong(5);
        }
        need(hash(hash)&&bytes>=80&&bytes<=MAX_BYTES&&rows>=0&&rows<=MAX_ROWS&&rejected>=0&&rejected<=Integer.MAX_VALUE&&count==(bytes+CHUNK_BYTES-1)/CHUNK_BYTES,"METADATA");
        byte[] encoded=new byte[(int)bytes];int at=0,index=0;
        try(Cursor c=db.rawQuery("SELECT chunk_index,byte_count,chunk_sha256,chunk_blob FROM "+CHUNKS+" WHERE dataset_hash=? ORDER BY chunk_index",new String[]{datasetHash})){
            while(c.moveToNext()){need(index<count&&c.getInt(0)==index,"CHUNK_ORDER");int size=c.getInt(1);need(size==Math.min(CHUNK_BYTES,encoded.length-at)&&size>0,"CHUNK_SIZE");byte[] chunk=c.getBlob(3);need(chunk!=null&&chunk.length==size&&sha(chunk).equals(c.getString(2)),"CHUNK_CHECKSUM");System.arraycopy(chunk,0,encoded,at,size);at+=size;index++;}
        }
        need(index==count&&at==encoded.length&&sha(encoded).equals(hash),"FULL_CHECKSUM");
        try{DataInputStream header=new DataInputStream(new ByteArrayInputStream(encoded));byte[] identity=new byte[64];
            need(header.readInt()==MAGIC&&header.readInt()==VERSION&&header.readInt()==rows&&header.readInt()==rejected,"HEADER_METADATA");header.readFully(identity);need(datasetHash.equals(new String(identity,StandardCharsets.US_ASCII)),"HEADER_METADATA");
        }catch(IOException invalid){throw new IllegalStateException("R29_DATASET_PIN_TRUNCATED",invalid);}
        return new Stored(datasetHash,hash,encoded,(int)rows,(int)rejected);
    }
    /** No SQLite access: restore all original rows/ordinals/rejections and independently recheck datasetHash. */
    public static ReplayDatasetR27 decode(Stored stored){
        need(stored!=null&&sha(stored.encoded).equals(stored.hash),"DECODE_CHECKSUM");
        try{DataInputStream in=new DataInputStream(new ByteArrayInputStream(stored.encoded));need(in.readInt()==MAGIC&&in.readInt()==VERSION,"HEADER");int rows=in.readInt(),rejected=in.readInt();byte[] identity=new byte[64];in.readFully(identity);need(rows==stored.rows&&rejected==stored.rejected&&stored.datasetHash.equals(new String(identity,StandardCharsets.US_ASCII)),"HEADER_METADATA");
            List<ReplayDatasetR27.Row> data=new ArrayList<>(rows);for(int i=0;i<rows;i++){CooperativeContinuationR26.poll();int length=in.readInt();need(length>=-1&&length<=MAX_ISSUE_BYTES&&length<=in.available(),"ISSUE_LENGTH");String issue=null;if(length>=0){byte[] text=new byte[length];in.readFully(text);issue=new String(text,StandardCharsets.UTF_8);need(Arrays.equals(text,issue.getBytes(StandardCharsets.UTF_8)),"ISSUE_UTF8");}long ordinal=in.readLong();int[] numbers=new int[10];for(int p=0;p<10;p++)numbers[p]=in.readUnsignedByte();data.add(new ReplayDatasetR27.Row(issue,ordinal,numbers));}
            need(in.available()==0,"TRAILING_BYTES");ReplayDatasetR27 result=ReplayDatasetR27.pinnedExact(data,rejected);need(result.datasetHash.equals(stored.datasetHash),"DATASET_HASH");return result;
        }catch(IOException truncated){throw new IllegalStateException("R29_DATASET_PIN_TRUNCATED",truncated);}
    }
}
