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
    private static void clear(File f){File[] files=f.listFiles();if(files!=null)for(File child:files)clear(child);assertTrue(f.delete());}
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
    @Test public void fullReportInputPinDefersCheckpointExpiryWithoutBlockingWriters()throws Exception {
        File directory=root();try{
            CheckpointArtifactsR26 files=new CheckpointArtifactsR26(new File(directory,"r26-checkpoints"));byte[] ref=files.prepare(bytes("snapshot-referenced-state"));
            try(com.ethk3.novaonepro.ReportInputsLeaseCP12 snapshot=com.ethk3.novaonepro.ReportInputsLeaseCP12.acquire(directory)){
                assertNull(CheckpointPruneGuardR26.tryAcquire(directory));assertArrayEquals(bytes("snapshot-referenced-state"),files.resolve(ref));
                try(com.ethk3.novaonepro.ReportInputsLeaseCP12 nested=com.ethk3.novaonepro.ReportInputsLeaseCP12.acquire(directory)){assertNull(CheckpointPruneGuardR26.tryAcquire(directory));}
                assertNull(CheckpointPruneGuardR26.tryAcquire(directory));
                try(CheckpointArtifactsR26.Prepared writer=files.prepareLease(bytes("next-concurrent-state"))){assertNotNull(writer.reference());}
            }
            try(CheckpointPruneGuardR26 cleanup=CheckpointPruneGuardR26.tryAcquire(directory)){assertNotNull(cleanup);assertNull(CheckpointPruneGuardR26.tryAcquire(directory));assertTrue(files.prune(Collections.emptySet(),System.currentTimeMillis()+120000,60000,64)>0);}
        }finally{clear(directory);}
    }
    @Test public void fullBackupReferenceCensusIncludesCurrentParentAndDurableOutputsOnly()throws Exception {
        File directory=root();try(SQLiteDatabase db=SQLiteDatabase.create(null)){
            CheckpointArtifactsR26 files=new CheckpointArtifactsR26(directory);ModelGenomeSU2 genome=FamilyRegistryR26.genome("F17").bindBirth(PolicyIdentityR26.sha256("SYNTHETIC_SOURCE"),0);LiveStateR26 model=new LiveStateR26(genome);int[] draw={1,2,3,4,5,6,7,8,9,10};model.forecast();model.learn(draw);byte[] before=model.encode();model.forecast();model.learn(draw);byte[] after=model.encode();
            try(CheckpointArtifactsR26.Prepared parent=files.prepareLease(before);CheckpointArtifactsR26.Prepared current=files.prepareLease(after);CheckpointArtifactsR26.Prepared output=files.prepareLease(bytes("SYNTHETIC_DURABLE_OUTPUT"))){
                files.prepare(bytes("unreferenced-file"));db.beginTransaction();try{RuntimeSchemaR26.ensure(db);db.execSQL("INSERT INTO r26_live_model(policy_hash,canonical_text,state,state_hash,checkpoint_hash,parent_hash,through_ordinal,genesis_ordinal,born_ordinal,updated_at) VALUES(?,?,?,?, 'current','parent',2,1,3,1)",new Object[]{genome.key(),genome.text(),current.reference(),LiveStateR26.hash(after)});
                    CheckpointManifestR26.record(db,"parent",genome.key(),"SYNTHETIC_SOURCE","",1,parent.reference());CheckpointManifestR26.record(db,"current",genome.key(),"SYNTHETIC_SOURCE","parent",2,current.reference());db.execSQL("INSERT INTO r26_replay_output_chunk VALUES('SYNTHETIC','member',0,0,1,?)",new Object[]{output.reference()});db.setTransactionSuccessful();}finally{db.endTransaction();}
                Set<String> expected=new TreeSet<>(Arrays.asList(parent.hash(),current.hash(),output.hash()));assertEquals(expected,CheckpointReferencesR26.hashes(db));for(String hash:expected)assertNotNull(files.readHash(hash));
            }
        }finally{clear(directory);}
    }
    @Test public void streamedCheckpointPinsAllPartsAndRetainedRootsCloseTheBackupGraph()throws Exception {
        File directory=root();try{
            CheckpointArtifactsR26 files=new CheckpointArtifactsR26(directory);byte[] state=new byte[2<<20];new Random(2626).nextBytes(state);byte[] reference;String root;
            try(CheckpointArtifactsR26.Prepared checkpoint=files.prepareState(out->out.write(state))){reference=checkpoint.reference();root=checkpoint.hash();assertTrue(files.reachableHashes(Collections.singleton(root)).size()>2);assertEquals(0,files.prune(Collections.emptySet(),System.currentTimeMillis()+120000,60000,8192));}
            files.prepare(bytes("unreachable"));assertEquals(11,files.prune(Collections.singleton(root),System.currentTimeMillis()+120000,60000,8192));
            try(InputStream input=files.openState(reference);ByteArrayOutputStream restored=new ByteArrayOutputStream()){byte[] buffer=new byte[7777];int n;while((n=input.read(buffer))!=-1)restored.write(buffer,0,n);assertArrayEquals(state,restored.toByteArray());}
            assertTrue(files.prune(Collections.emptySet(),System.currentTimeMillis()+120000,60000,8192)>0);assertEquals(0,files.bytes());
        }finally{clear(directory);}
    }
    @Test public void corruptStreamPartCannotRestoreBehindAnIntactRoot()throws Exception {
        File directory=root();try{
            CheckpointArtifactsR26 files=new CheckpointArtifactsR26(directory);byte[] state=new byte[100000];new Random(2627).nextBytes(state);
            try(CheckpointArtifactsR26.Prepared checkpoint=files.prepareState(out->out.write(state))){String part=ChunkedCheckpointR26.children(files.resolve(checkpoint.reference())).iterator().next();File path=new File(directory,part+".bin");try(RandomAccessFile corrupt=new RandomAccessFile(path,"rw")){corrupt.seek(path.length()-8);int original=corrupt.readUnsignedByte();corrupt.seek(path.length()-8);corrupt.write(original^1);corrupt.getFD().sync();}
                try(InputStream input=files.openState(checkpoint.reference())){byte[] buffer=new byte[8192];while(input.read(buffer)!=-1){}fail("corrupt child accepted through intact root");}catch(IOException expected){assertTrue(expected.getMessage().contains("CORRUPT"));}
            }
        }finally{clear(directory);}
    }
    @Test public void privatePolicyUsesStreamedStateAndKeepsTheNextLearnedUpdate()throws Exception {
        File directory=root();try{
            CheckpointArtifactsR26 files=new CheckpointArtifactsR26(directory);ModelGenomeSU2 genome=FamilyRegistryR26.genome("F17").bindBirth(PolicyIdentityR26.sha256("SYNTHETIC_PRIVATE_SOURCE"),1000);LegacyReplayStateR26 state=new LegacyReplayStateR26(Collections.singletonList(genome.text()),1000);int[] draw={1,2,3,4,5,6,7,8,9,10};for(int i=0;i<40;i++)state.advance(draw);PolicyRuntimeR26 live=PolicyRuntimeR26.legacy(state),resumed;
            try(CheckpointArtifactsR26.Prepared checkpoint=PolicyCheckpointR26.prepare(files,live)){byte[] root=files.resolve(checkpoint.reference());assertTrue(ChunkedCheckpointR26.isManifest(root));resumed=PolicyCheckpointR26.restore(files,genome.text(),checkpoint.reference(),LiveStateR26.hash(root));}
            ModelLiveSU2.Decision left=live.forecast(),right=resumed.forecast();assertEquals(left.position,right.position);assertEquals(left.mask,right.mask);assertEquals(left.phat,right.phat,0);live.learn(draw,1040);resumed.learn(draw,1040);
            try(CheckpointArtifactsR26.Prepared a=PolicyCheckpointR26.prepare(files,live);CheckpointArtifactsR26.Prepared b=PolicyCheckpointR26.prepare(files,resumed)){assertEquals(a.hash(),b.hash());}
            LiveStateR26 family=new LiveStateR26(genome);family.forecast();family.learn(draw);byte[] bytes=family.encode();try(CheckpointArtifactsR26.Prepared inline=files.prepareLease(bytes)){PolicyRuntimeR26 restored=PolicyCheckpointR26.restore(files,genome.text(),inline.reference(),LiveStateR26.hash(bytes));assertNull(restored.legacyState());assertEquals(family.forecast().mask,restored.forecast().mask);}
        }finally{clear(directory);}
    }
}
