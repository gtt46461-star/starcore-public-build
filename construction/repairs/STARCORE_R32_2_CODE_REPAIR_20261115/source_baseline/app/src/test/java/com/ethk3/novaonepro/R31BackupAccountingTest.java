package com.ethk3.novaonepro;

import org.junit.Test;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.Assert.*;

/** Regression for handset request 41744350...: a block delta exceeds a page hint.
 * These are explicit HOST capacity fixtures, not a claim about that handset. */
public class R31BackupAccountingTest {
    static final long MiB=1L<<20;
    static File root()throws Exception{return Files.createTempDirectory("r31-backup-accounting-").toFile();}
    static StorageBudgetR31 budget(File root,AtomicLong available)throws Exception{
        return new StorageBudgetR31(root,new StorageBudgetR31.Config(8*MiB,8*MiB,1024,8192,"HOST_BACKUP_ACCOUNTING"),f->available.get());
    }
    static void write(File root,int bytes)throws Exception{File f=new File(root,"files/job/snapshot.db");f.getParentFile().mkdirs();try(FileOutputStream out=new FileOutputStream(f)){out.write(new byte[bytes]);out.getFD().sync();}}
    static long charged(StorageBudgetR31 b)throws Exception{return ((Number)((Map<?,?>)((List<?>)b.snapshot().get("owners")).get(0)).get("chargedWriteBytes")).longValue();}
    interface Checked{void run()throws Exception;}
    static void rejected(Checked c,String why)throws Exception{try{c.run();fail(why);}catch(IOException e){assertTrue(e.toString(),e.toString().contains(why));}}
    @Test public void physicalDeltaAbovePageHintUsesExistingEnvelope()throws Exception{
        File root=root();StorageBudgetR31 b=budget(root,new AtomicLong(16*MiB));
        try(StorageBudgetR31.Reservation r=b.reserve("backup",1,StorageBudgetR31.Category.REPORT_WORKSPACE,"files/job",MiB,2*MiB,true)){
            r.beforeChunk(8192);write(root,65536);
            // The old generic refund reproducibly rejects this observed condition.
            try{r.refundUnwritten(8192,65536);fail("old counterexample missing");}catch(IllegalArgumentException expected){assertEquals("STORAGE_CHUNK_RECONCILIATION",expected.getMessage());}
            r.reconcileBackupChunk(8192,65536);
            assertEquals(65536,charged(b));assertEquals(MiB,r.maximumGrowthBytes());
            assertEquals(1,((List<?>)b.snapshot().get("owners")).size());
            r.beforeChunk(8192);r.reconcileBackupChunk(8192,0);assertEquals(65536,charged(b));
        }
        assertTrue(((List<?>)b.snapshot().get("owners")).isEmpty());assertEquals(65536,new File(root,"files/job/snapshot.db").length());
    }
    @Test public void ownerPhysicalBoundStillRejectsAndDoesNotChangeLedger()throws Exception{
        File root=root();StorageBudgetR31 b=budget(root,new AtomicLong(16*MiB));
        try(StorageBudgetR31.Reservation r=b.reserve("backup",2,StorageBudgetR31.Category.REPORT_WORKSPACE,"files/job",16384,MiB,true)){
            r.beforeChunk(8192);rejected(()->r.reconcileBackupChunk(8192,32768),"PHYSICAL_GROWTH_BOUND");assertEquals(8192,charged(b));assertEquals(16384,r.maximumGrowthBytes());
        }
    }
    @Test public void writeBoundAndFreeFloorStillRejectWithoutIncreasingGrant()throws Exception{
        File root=root();AtomicLong free=new AtomicLong(16*MiB);StorageBudgetR31 b=budget(root,free);
        try(StorageBudgetR31.Reservation r=b.reserve("backup",3,StorageBudgetR31.Category.REPORT_WORKSPACE,"files/job",16384,16384,true)){
            r.beforeChunk(8192);rejected(()->r.reconcileBackupChunk(8192,32768),"WRITE_BOUND_EXCEEDED");assertEquals(8192,charged(b));
            free.set(0);rejected(()->r.reconcileBackupChunk(8192,12288),"FREE_SPACE_RESERVE");free.set(16*MiB);assertEquals(8192,charged(b));assertEquals(16384,r.maximumGrowthBytes());
        }
    }
    @Test public void unadmittedAndClosedOwnerRemainFailures()throws Exception{
        File root=root();StorageBudgetR31 b=budget(root,new AtomicLong(16*MiB));StorageBudgetR31.Reservation r=b.reserve("backup",4,StorageBudgetR31.Category.REPORT_WORKSPACE,"files/job",MiB,true);
        try{rejected(()->r.reconcileBackupChunk(8192,16384),"CHUNK_RECONCILIATION_ORDER");r.beforeChunk(8192);rejected(()->r.reconcileBackupChunk(16384,32768),"CHUNK_RECONCILIATION_ORDER");assertEquals(8192,charged(b));}finally{r.close();}
        rejected(()->r.reconcileBackupChunk(8192,16384),"OWNER_CLOSED");
    }
}
