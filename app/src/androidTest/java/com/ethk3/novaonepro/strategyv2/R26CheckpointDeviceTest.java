package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Real Android fsync/rename + SQLite recovery; isolated from the running application's owner DB. */
@RunWith(AndroidJUnit4.class)
public final class R26CheckpointDeviceTest {
    private static File root(){File f=new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir(),"r26-cas-test-"+UUID.randomUUID());assertTrue(f.mkdirs());return f;}
    private static void clear(File f){File[] files=f.listFiles();if(files!=null)for(File child:files)assertTrue(child.delete());assertTrue(f.delete());}
    private static byte[] bytes(String x)throws Exception{return x.getBytes("UTF-8");}
    private static byte[] active(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT ref FROM active WHERE id=1",null)){assertTrue(c.moveToFirst());return c.getBlob(0);}}
    private static void schema(SQLiteDatabase db){db.execSQL("CREATE TABLE active(id INTEGER PRIMARY KEY,ref BLOB NOT NULL,parent TEXT NOT NULL)");db.execSQL("CREATE TABLE consumed(event TEXT PRIMARY KEY)");}

    @Test public void crashBeforeAndDuringDatabaseCommitKeepsOldActiveCheckpoint()throws Exception {
        File directory=root();try(SQLiteDatabase db=SQLiteDatabase.create(null)){
            CheckpointArtifactsR26 files=new CheckpointArtifactsR26(directory);schema(db);byte[] old=bytes("complete-old-state"),next=bytes("complete-new-state");
            try(CheckpointArtifactsR26.Prepared first=files.prepareLease(old)){db.execSQL("INSERT INTO active VALUES(1,?,'genesis')",new Object[]{first.reference()});}
            byte[] prepared=files.prepare(next);assertArrayEquals(old,files.resolve(active(db)));
            // Death before setTransactionSuccessful: neither pointer nor consumption can survive.
            db.beginTransaction();try{db.execSQL("UPDATE active SET ref=?,parent='old' WHERE id=1",new Object[]{prepared});db.execSQL("INSERT INTO consumed VALUES('event')");}finally{db.endTransaction();}
            CheckpointArtifactsR26 restarted=new CheckpointArtifactsR26(directory);assertArrayEquals(old,restarted.resolve(active(db)));
            try(Cursor c=db.rawQuery("SELECT count(*) FROM consumed",null)){c.moveToFirst();assertEquals(0,c.getInt(0));}
            db.beginTransaction();try{db.execSQL("UPDATE active SET ref=?,parent='old' WHERE id=1",new Object[]{prepared});db.execSQL("INSERT INTO consumed VALUES('event')");db.setTransactionSuccessful();}finally{db.endTransaction();}
            assertArrayEquals(next,new CheckpointArtifactsR26(directory).resolve(active(db)));
            assertArrayEquals(old,files.resolve(old)); // old inline checkpoints remain readable
        }finally{clear(directory);}
    }
    @Test public void staleParentCannotCommitOrConsumeAnEvent()throws Exception {
        File directory=root();try(SQLiteDatabase db=SQLiteDatabase.create(null)){
            CheckpointArtifactsR26 files=new CheckpointArtifactsR26(directory);schema(db);byte[] state=bytes("active-state");db.execSQL("INSERT INTO active VALUES(1,?,'current')",new Object[]{files.prepare(state)});
            try(CheckpointArtifactsR26.Prepared abandoned=files.prepareLease(bytes("stale-worker-result"))){
                db.beginTransaction();try{db.execSQL("UPDATE active SET ref=? WHERE id=1 AND parent='superseded'",new Object[]{abandoned.reference()});try(Cursor c=db.rawQuery("SELECT changes()",null)){c.moveToFirst();assertEquals(0,c.getInt(0));}throw new IOException("injected stale parent rejection");}catch(IOException expected){}finally{db.endTransaction();}
            }
            assertArrayEquals(state,files.resolve(active(db)));
            try(Cursor c=db.rawQuery("SELECT count(*) FROM consumed",null)){c.moveToFirst();assertEquals(0,c.getInt(0));}
        }finally{clear(directory);}
    }
    @Test public void garbageCollectionProtectsPreparedAndCommittedFiles()throws Exception {
        File directory=root();try{
            CheckpointArtifactsR26 files=new CheckpointArtifactsR26(directory);byte[] state=bytes("prepared-before-db-commit"),orphan=bytes("rolled-back-file");
            try(CheckpointArtifactsR26.Prepared prepared=files.prepareLease(state)){
                files.prepare(orphan);assertEquals(orphan.length,files.prune(Collections.emptySet(),System.currentTimeMillis()+120000,60000,64));assertArrayEquals(state,files.resolve(prepared.reference()));
                Set<String> refs=Collections.singleton(prepared.hash());assertEquals(0,files.prune(refs,System.currentTimeMillis()+120000,60000,64));
            }
            assertEquals(state.length,files.prune(Collections.emptySet(),System.currentTimeMillis()+120000,60000,64));assertEquals(0,files.bytes());
        }finally{clear(directory);}
    }
    @Test public void corruptOrMissingArtifactNeverRestoresAsValidState()throws Exception {
        File directory=root();try{
            CheckpointArtifactsR26 files=new CheckpointArtifactsR26(directory);byte[] ref=files.prepare(bytes("state whose digest is frozen"));File target=new File(directory,CheckpointArtifactsR26.referenceHash(ref)+".bin");
            try(RandomAccessFile output=new RandomAccessFile(target,"rw")){output.seek(1);output.write(0);output.getFD().sync();}
            try{files.resolve(ref);fail("corrupted state restored");}catch(IOException expected){assertTrue(expected.getMessage().contains("CORRUPT"));}
            assertTrue(target.delete());try{files.resolve(ref);fail("missing state restored");}catch(IOException expected){}
            byte[] malformed=bytes("R26CAS1:"+String.join("",Collections.nCopies(64,"/")));try{files.resolve(malformed);fail("path traversal reference accepted");}catch(IOException expected){assertTrue(expected.getMessage().contains("HASH"));}
        }finally{clear(directory);}
    }
    @Test public void concurrentSameContentHasOneAtomicArtifactAndAccurateQuota()throws Exception {
        File directory=root();ExecutorService workers=Executors.newFixedThreadPool(4);try{
            CheckpointArtifactsR26 files=new CheckpointArtifactsR26(directory);byte[] state=new byte[32768];new Random(26).nextBytes(state);List<Future<byte[]>> all=new ArrayList<>();
            for(int i=0;i<8;i++)all.add(workers.submit(()->{try(CheckpointArtifactsR26.Prepared p=files.prepareLease(state)){assertArrayEquals(state,files.resolve(p.reference()));return p.reference();}}));
            byte[] reference=all.get(0).get(10,TimeUnit.SECONDS);for(Future<byte[]> future:all)assertArrayEquals(reference,future.get(10,TimeUnit.SECONDS));assertEquals(1,directory.listFiles().length);assertEquals(state.length,files.bytes());
        }finally{workers.shutdownNow();clear(directory);}
    }
}
