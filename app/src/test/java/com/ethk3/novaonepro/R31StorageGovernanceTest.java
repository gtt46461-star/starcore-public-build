package com.ethk3.novaonepro;

import org.junit.Test;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.Assert.*;

/** Real temporary files. Small explicit capacities/clock samples are fixtures, never handset soak evidence. */
public class R31StorageGovernanceTest {
    private static File root()throws Exception{return Files.createTempDirectory("r31-storage-unit-").toFile();}
    private static StorageBudgetR31 budget(File root,long hard,StorageBudgetR31.Space space)throws Exception{
        return new StorageBudgetR31(root,new StorageBudgetR31.Config(hard,hard,1024,8192,"HOST_CAPACITY_FIXTURE"),space);
    }
    private static File write(File root,String name,int bytes)throws Exception{
        File path=new File(root,name);path.getParentFile().mkdirs();try(OutputStream out=new FileOutputStream(path)){out.write(new byte[bytes]);}return path;
    }
    interface Checked{void run()throws Exception;}
    private static IOException rejected(Checked work,String reason)throws Exception{
        try{work.run();fail("Expected "+reason);return null;}catch(IOException expected){assertTrue(expected.toString(),expected.toString().contains(reason));return expected;}
    }
    @Test public void categoriesAndScopesUseTheSameObservedFileLengths()throws Exception{
        File root=root();write(root,"databases/main.db",100);write(root,"databases/main.db-wal",20);write(root,"databases/main.db-shm",30);write(root,"files/r981_reports/jobs/x/snapshot.db",40);write(root,"files/r981_reports/recovery/x/recovery.zip",50);write(root,"cache/rebuildable.bin",60);
        StorageCensusSU2.Census c=StorageCensusSU2.measure(root,Arrays.asList("databases","files/r981_reports"));
        assertFalse(c.line(),c.truncated);assertTrue(StorageCensusSU2.measure(root,Collections.emptyList(),()->true).incompleteReasons.contains("CANCELLED"));assertEquals(300,c.total);assertEquals(150L,(long)c.scopeBytes.get("databases"));assertEquals(90L,(long)c.scopeBytes.get("files/r981_reports"));assertEquals(30L,(long)c.bytes.get("SHM"));
        assertEquals(c.total,c.bytes.values().stream().mapToLong(Long::longValue).sum());assertFalse("Host must not invent Android kernel allocation",c.allocatedBytesKnown);
    }
    @Test public void symlinksAndInterruptedOrDeepCensusStayExplicit()throws Exception{
        File root=root(),outside=root();write(outside,"original.bin",4096);Files.createSymbolicLink(new File(root,"foreign").toPath(),outside.toPath());StorageCensusSU2.Census c=StorageCensusSU2.measure(root);assertTrue(c.truncated);assertEquals(0,c.total);assertTrue(new File(outside,"original.bin").isFile());
        rejected(()->StorageBudgetR31.scopeBytes(root),"SYMLINK");
        File deep=root();String name="";for(int n=0;n<18;n++){name+=(name.isEmpty()?"":"/")+"d";}write(deep,name+"/file",1);assertTrue(StorageCensusSU2.measure(deep).truncated);rejected(()->StorageBudgetR31.scopeBytes(deep),"DEPTH");
        Thread.currentThread().interrupt();try{assertTrue(StorageCensusSU2.measure(outside).incompleteReasons.contains("CANCELLED"));rejected(()->StorageBudgetR31.scopeBytes(outside),"CANCELLED");}finally{Thread.interrupted();}
    }
    @Test public void pre26UnsupportedEnumerationFailsExplicitlyWithoutListFiles()throws Exception{
        File root=root();String vm=System.getProperty("java.vm.name");System.setProperty("java.vm.name","Dalvik");
        try{rejected(()->DirectoryCursorR31.open(root),"DIRECTORY_STREAM_");}finally{System.setProperty("java.vm.name",vm);}
    }
    @Test public void directoryEntryBoundIsAppliedBeforeResultAllocation()throws Exception{
        File root=root();for(int n=0;n<20;n++)write(root,"f"+n,1);
        rejected(()->DirectoryCursorR31.children(root,2),"DIRECTORY_ENTRY_LIMIT:2");assertEquals(20,DirectoryCursorR31.children(root,20).length);
    }
    @Test public void productionAdmissionDeniesUnknownAllocatedBytesOnHost()throws Exception{
        File root=root();StorageBudgetR31 production=new StorageBudgetR31(root);
        rejected(()->production.reserve("host",1,StorageBudgetR31.Category.REPORT_WORKSPACE,"files/job",4096,true),"ALLOCATED_BYTES_UNKNOWN");assertEquals("UNKNOWN_ALLOCATION_DENY_ADMISSION",production.snapshot().get("state"));
    }
    @Test public void overlappingOwnerAndPhysicalGrowthAreDeniedBeforeTheWrite()throws Exception{
        File root=root();StorageBudgetR31 budget=budget(root,2L<<20,f->100L<<20);File output=new File(root,"files/report/output.bin");output.getParentFile().mkdirs();
        try(StorageBudgetR31.Reservation reservation=budget.reserve("report",7,StorageBudgetR31.Category.REPORT_WORKSPACE,"files/report",65536,131072,true)){
            rejected(()->budget.reserve("other",8,StorageBudgetR31.Category.REPORT_WORKSPACE,"files/report/sub",4096,true),"SCOPE_ALREADY_OWNED");
            try(OutputStream out=reservation.wrap(output,new FileOutputStream(output))){out.write(new byte[65536]);rejected(()->out.write(1),"PHYSICAL_GROWTH_BOUND");}assertEquals(65536,output.length());
            assertEquals(0,budget.recoverReservations());
        }assertEquals(0,((List<?>)budget.snapshot().get("owners")).size());assertEquals(65536,output.length());
    }
    @Test public void actualBytesAndOutstandingGrantAreNotDoubleCounted()throws Exception{
        File root=root();StorageBudgetR31 budget=budget(root,2L<<20,f->100L<<20);File output=new File(root,"files/report/output");output.getParentFile().mkdirs();
        try(StorageBudgetR31.Reservation r=budget.reserve("report",1,StorageBudgetR31.Category.REPORT_WORKSPACE,"files/report",262144,true)){
            try(OutputStream out=r.wrap(output,new FileOutputStream(output))){out.write(new byte[131072]);}
            Map<String,Object> snapshot=budget.snapshot();assertEquals(131072L,((Number)snapshot.get("unallocatedReservationBytes")).longValue());
            assertTrue(((Number)snapshot.get("accountedPlusPendingBytes")).longValue()<300000);assertEquals(131072,output.length());
        }
    }
    @Test public void freeSpaceChangeStopsFurtherOutputAndProtectedDebtPreservesOriginal()throws Exception{
        File root=root();AtomicLong free=new AtomicLong(4L<<20);StorageBudgetR31 budget=budget(root,2L<<20,f->free.get());
        try(StorageBudgetR31.Reservation r=budget.reserve("report",1,StorageBudgetR31.Category.REPORT_WORKSPACE,"files/report",65536,true)){free.set(1024);rejected(()->r.beforeChunk(1),"FREE_SPACE_RESERVE");}
        File original=write(root,"databases/original.db",262144);StorageBudgetR31 small=budget(root,131072,f->100L<<20);rejected(()->small.reserve("optional",2,StorageBudgetR31.Category.REPORT_WORKSPACE,"files/new",1024,true),"BLOCKED_PROTECTED_DEBT");assertEquals(262144,original.length());
    }
    @Test public void closedOldOwnerCannotWriteIntoNewReservation()throws Exception{
        File root=root();StorageBudgetR31 budget=budget(root,2L<<20,f->100L<<20);StorageBudgetR31.Reservation old=budget.reserve("report",1,StorageBudgetR31.Category.REPORT_WORKSPACE,"files/report",4096,true);old.close();
        try(StorageBudgetR31.Reservation next=budget.reserve("report",2,StorageBudgetR31.Category.REPORT_WORKSPACE,"files/report",4096,true)){rejected(()->old.beforeChunk(1),"OWNER_CLOSED");next.beforeChunk(1);assertEquals(1,((List<?>)budget.snapshot().get("owners")).size());}
    }
    @Test public void unregisteredOvershootIsReportedAtFinishAndNeverDeletesEvidence()throws Exception{
        File root=root();StorageBudgetR31 b=budget(root,2L<<20,f->100L<<20);
        StorageBudgetR31.Reservation r=b.reserve("violating-fixture",1,StorageBudgetR31.Category.REPORT_WORKSPACE,"files/job",4096,true);
        File output=write(root,"files/job/output",8192);rejected(r::close,"FINISHED_PHYSICAL_GROWTH_BOUND_EXCEEDED");
        assertEquals(8192,output.length());Map<String,Object> state=b.snapshot();assertTrue(((List<?>)state.get("owners")).isEmpty());assertTrue(state.get("receipts").toString().contains("FINISHED_BOUND_EXCEEDED_FILES_PRESERVED"));
    }
    @Test public void metadataSymlinkAndCorruptLedgerArePreservedAndRejected()throws Exception{
        File root=root(),outside=root();File sentinel=write(outside,"original",7),target=new File(root,"metadata");Files.createSymbolicLink(new File(root,"metadata.new").toPath(),sentinel.toPath());rejected(()->StorageBudgetR31.atomicWrite(target,new byte[]{1,2}),"SYMLINK");assertEquals(7,sentinel.length());Files.delete(new File(root,"metadata.new").toPath());
        StorageBudgetR31 b=budget(root,2L<<20,f->100L<<20);try(StorageBudgetR31.Reservation r=b.reserve("report",1,StorageBudgetR31.Category.REPORT_WORKSPACE,"files/job",4096,true)){}
        File ledger=new File(root,"files/r31-storage/ledger.bin");try(RandomAccessFile io=new RandomAccessFile(ledger,"rw")){io.seek(ledger.length()-1);int before=io.read();io.seek(ledger.length()-1);io.write(before^1);}rejected(b::snapshot,"CORRUPT_PRESERVED");assertTrue(ledger.isFile());
    }
    @Test public void realAdmissionRejectionsUseSharedDispositionWithoutFaultQuarantine()throws Exception{
        File root=root();StorageBudgetR31 b=budget(root,2L<<20,f->100L<<20);
        StorageBudgetR31.Reservation owner=b.reserve("actual-owner",41,StorageBudgetR31.Category.REPORT_WORKSPACE,"files/job",4096,true);
        try{try{b.reserve("contender",42,StorageBudgetR31.Category.REPORT_WORKSPACE,"files/job",4096,true);fail("Overlap");}catch(StorageBudgetR31.Rejected rejected){
            assertEquals("contender",rejected.owner);assertEquals(42,rejected.fence);assertEquals("SCOPE_ALREADY_OWNED",rejected.reason);
            for(int n=0;n<8;n++){com.ethk3.novaonepro.strategyv2.FailureDispositionR31.Decision d=com.ethk3.novaonepro.strategyv2.FailureDispositionR31.classify(rejected);assertEquals(com.ethk3.novaonepro.strategyv2.FailureDispositionR31.Disposition.DEPENDENCY,d.disposition);assertFalse(d.consumesFaultBudget());}
        }}finally{owner.close();}
        try(StorageBudgetR31.Reservation replacement=b.reserve("contender",42,StorageBudgetR31.Category.REPORT_WORKSPACE,"files/job",4096,true)){replacement.beforeChunk(1);}
        try{owner.beforeChunk(1);fail("Closed owner");}catch(StorageBudgetR31.Rejected rejected){assertEquals(com.ethk3.novaonepro.strategyv2.FailureDispositionR31.Disposition.STALE_OWNER,com.ethk3.novaonepro.strategyv2.FailureDispositionR31.classify(rejected).disposition);}
        Thread.currentThread().interrupt();try{try{b.reserve("cancelled",43,StorageBudgetR31.Category.REPORT_WORKSPACE,"files/new",4096,true);fail("Cancelled");}catch(StorageBudgetR31.Rejected rejected){assertEquals(com.ethk3.novaonepro.strategyv2.FailureDispositionR31.Disposition.CANCELLED,com.ethk3.novaonepro.strategyv2.FailureDispositionR31.classify(rejected).disposition);}}finally{Thread.interrupted();}
        assertEquals(com.ethk3.novaonepro.strategyv2.FailureDispositionR31.Disposition.FAULT,com.ethk3.novaonepro.strategyv2.FailureDispositionR31.classify(new IOException("STORAGE_LEDGER_HASH_CORRUPT")).disposition);
    }
    @Test public void fixedRingsDoNotGrowBeyond288PointsAnd720Hours()throws Exception{
        File root=root();write(root,"databases/main.db",11);StorageCensusSU2.Census census=StorageCensusSU2.measure(root);StorageLedgerR31 ledger=new StorageLedgerR31(root);
        for(int n=0;n<725;n++)ledger.record(census,"HOST_RING_TEST","fixture",n*3600000L,100000,0,"FIXTURE","SYNTHETIC_CLOCK_NOT_ELAPSED_RUNTIME");
        assertEquals(288,ledger.history(288).size());Map<String,Object> latest=ledger.latest();assertEquals(725L,latest.get("sequence"));assertEquals(11L,latest.get("regularFileBytes"));assertNull(latest.get("allocatedBlocksBytes"));assertFalse((Boolean)latest.get("physicalMeasurementComplete"));
        File[] files=DirectoryCursorR31.children(new File(root,"files/r31-storage-samples"),1024);assertEquals(1010,files.length);assertTrue(StorageBudgetR31.scopeBytes(new File(root,"files/r31-storage-samples"))<2L*1024*1024);
    }
}
