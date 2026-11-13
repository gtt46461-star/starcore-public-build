package com.ethk3.novaonepro;

import java.io.*;
import java.nio.channels.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.*;

/** Small recovery records use the metadata headroom precharged by every normal
 * admission. They never wait for a workspace census or overlap a workspace grant.
 * The separate OS lease bounds their aggregate physical footprint, including
 * AtomicFile backup/new versions. This is not an unaccounted bulk-output path. */
final class ReportControlBudgetR31 {
    private ReportControlBudgetR31() {}
    private static final ReentrantLock JVM=new ReentrantLock(true);
    private static final long QUOTA=2L<<20,RECORD_LIMIT=65536;
    static boolean isProgress(File root,File file)throws IOException {
        String prefix=root.getCanonicalPath()+File.separator,path=file.getCanonicalPath();
        return path.startsWith(prefix)&&path.substring(prefix.length()).matches("jobs/[0-9a-f]{64}/(?:stage|backup)-progress\\.json");
    }
    static boolean accepts(File root,File file)throws IOException {
        if(!file.getCanonicalPath().startsWith(root.getCanonicalPath()+File.separator))return false;
        String relative=file.getCanonicalPath().substring(root.getCanonicalPath().length()+1);
        return isProgress(root,file)||relative.matches("requests/[0-9a-f]{64}(?:\\.ack)?\\.json")
                || relative.matches("jobs/[0-9a-f]{64}/job\\.json")
                || relative.matches("quarantine/[0-9a-f]{64}\\.json")
                || relative.matches("cancel-commands/[0-9a-f]{64}\\.json")
                || relative.matches("control-r69/(?:command|status|cancel)-[0-9a-f]{64}\\.json")
                || relative.matches("(?:request-queue|interactive-pending-r69|current-job|cancel)\\.json");
    }
    static Closeable admit(File root,File file,long bytes,StorageBudgetR31 budget)throws IOException {
        if(bytes<0||bytes>RECORD_LIMIT||!accepts(root,file))throw new IOException("REPORT_CONTROL_RECORD_BOUND");
        if(file.exists()&&!file.isFile())throw new IOException("REPORT_CONTROL_TARGET_TYPE");
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);boolean entered=false;
        RandomAccessFile io=null;FileLock lease=null;
        try {
            try{entered=JVM.tryLock(5,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException("REPORT_CONTROL_CANCELLED",e);}
            if(!entered)throw new IOException("REPORT_CONTROL_BUSY");
            File lockFile=new File(root,"control-budget.lock");if(!lockFile.getAbsoluteFile().equals(lockFile.getCanonicalFile()))throw new IOException("REPORT_CONTROL_LOCK_PATH");
            io=new RandomAccessFile(lockFile,"rw");
            while(lease==null) {
                try{lease=io.getChannel().tryLock();}catch(OverlappingFileLockException busy){/* bounded wait */}
                if(lease!=null)break;
                if(Thread.currentThread().isInterrupted())throw new IOException("REPORT_CONTROL_CANCELLED");
                if(System.nanoTime()>=deadline)throw new IOException("REPORT_CONTROL_BUSY");
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(5));
            }
            long used=0;int entries=0;
            // User stop/clear must be durable before a new export is submitted.
            // Its exact command/status/cancel records share this same precharged
            // quota; count every existing child, including AtomicFile .bak/.new.
            for(String name:new String[]{"requests","cancel-commands","quarantine","control-r69"}) {
                File directory=new File(root,name);if(!directory.exists())continue;
                try(DirectoryCursorR31 cursor=DirectoryCursorR31.open(directory)) {
                    File child;while((child=cursor.next())!=null) {
                        if(++entries>2048||!child.getAbsoluteFile().equals(child.getCanonicalFile()))throw new IOException("REPORT_CONTROL_SCOPE");
                        used=Math.addExact(used,accountedControlEntry(child));
                    }
                }
            }
            // The job journal must be durable before its bulk workspace can
            // be admitted. Charge only this fixed control record (and atomic
            // backup/new versions), never report payloads, to the same existing
            // metadata headroom and quota. Do not census bulk job directories.
            File jobs=new File(root,"jobs");
            if(jobs.exists())try(DirectoryCursorR31 cursor=DirectoryCursorR31.open(jobs)) {
                File job;while((job=cursor.next())!=null) {
                    if(++entries>2048||!job.getName().matches("[0-9a-f]{64}")||!job.getAbsoluteFile().equals(job.getCanonicalFile())||!job.isDirectory())throw new IOException("REPORT_CONTROL_SCOPE");
                    for(String name:new String[]{"job.json","stage-progress.json","backup-progress.json"})
                        for(String suffix:new String[]{"",".bak",".new"})used=Math.addExact(used,accountedControlEntryIfPresent(new File(job,name+suffix)));
                }
            }
            for(String name:new String[]{"request-queue","interactive-pending-r69","current-job","cancel"})
                for(String suffix:new String[]{".json",".json.bak",".json.new"})used=Math.addExact(used,accounted(new File(root,name+suffix)));
            // The base/backup can coexist with the new version until fd.sync.
            long growth=Math.max(4096L,((bytes+4095L)/4096L)*4096L);
            if(Math.addExact(used,growth)>Math.min(QUOTA,budget.controlHeadroomR31()))throw new IOException("REPORT_CONTROL_QUOTA");
            if(budget.controlAvailableR31()<budget.controlFloorR31()+growth)throw new IOException("REPORT_CONTROL_FREE_SPACE");
            final RandomAccessFile ownedIo=io;final FileLock ownedLease=lease;
            return ()->{try{ownedLease.release();}finally{try{ownedIo.close();}finally{JVM.unlock();}}};
        }catch(IOException|RuntimeException|Error error) {
            if(lease!=null)try{lease.release();}catch(IOException e){error.addSuppressed(e);}
            if(io!=null)try{io.close();}catch(IOException e){error.addSuppressed(e);}
            if(entered)JVM.unlock();throw error;
        }
    }
    private static long accountedControlEntryIfPresent(File file)throws IOException {
        return file.exists()?accountedControlEntry(file):0;
    }
    private static long accounted(File file)throws IOException {
        if(!file.exists())return 0;
        if(!file.getAbsoluteFile().equals(file.getCanonicalFile())||!file.isFile())throw new IOException("REPORT_CONTROL_PATH");
        long allocated=StorageCensusSU2.allocatedBytes(file);
        // Block-rounded upper bound also covers host filesystems lacking st_blocks.
        return Math.max(Math.max(0L,allocated),Math.max(4096L,((file.length()+4095L)/4096L)*4096L));
    }
    private static long accountedControlEntry(File file)throws IOException {
        if(file.isFile())return accounted(file);
        // An empty wrong-type ACK remains unwritable, but its actual directory
        // blocks must not poison a different job's durable READY journal.
        // Unknown nonempty trees remain refused and are never traversed/deleted.
        if(!file.isDirectory())throw new IOException("REPORT_CONTROL_SCOPE");
        try(DirectoryCursorR31 cursor=DirectoryCursorR31.open(file)){if(cursor.next()!=null)throw new IOException("REPORT_CONTROL_SCOPE");}
        long allocated=StorageCensusSU2.allocatedBytes(file);
        return Math.max(4096L,Math.max(0L,allocated));
    }
}
