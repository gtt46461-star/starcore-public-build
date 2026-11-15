package com.ethk3.novaonepro;
import java.io.*;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.Map;
/** Read pins span the SQLite snapshot and file export; expiry takes the same exclusive lock. */
public final class ReportInputsLeaseCP12 implements Closeable {
    // Keep one descriptor per path. Closing a second descriptor can release POSIX locks
    // held by the first, even while its Java FileLock is still reported as valid.
    private static final Map<String,Entry> OPEN=new HashMap<>();
    private static final Map<Long,Integer> THREAD_READERS=new HashMap<>();
    private static final Map<Long,Map<String,Integer>> THREAD_INPUT_PATHS_R31=new HashMap<>();
    private static final class Entry {
        final RandomAccessFile io;final FileLock lock;final boolean shared;int references=1;
        Entry(RandomAccessFile io,FileLock lock,boolean shared){this.io=io;this.lock=lock;this.shared=shared;}
    }
    private final String path;private final Entry entry;private final long acquiringThread;private boolean closed;
    private ReportInputsLeaseCP12(String path,Entry entry){this.path=path;this.entry=entry;acquiringThread=Thread.currentThread().getId();THREAD_READERS.put(acquiringThread,THREAD_READERS.getOrDefault(acquiringThread,0)+1);
        if(entry.shared){Map<String,Integer> paths=THREAD_INPUT_PATHS_R31.computeIfAbsent(acquiringThread,key->new HashMap<>());paths.put(path,paths.getOrDefault(path,0)+1);}}
    /** Cleanup must be called after the report owner's own input-pin scope exits. Other processes are independent. */
    public static synchronized boolean currentThreadHoldsInput(){return THREAD_READERS.getOrDefault(Thread.currentThread().getId(),0)>0;}
    /** A pin for another files directory grants no access to this source.
     * Pure observation: never creates a directory or opens another lock fd. */
    public static synchronized boolean currentThreadHoldsInput(File filesDir)throws IOException {
        String path=new File(new File(filesDir,"retention-cas-cp12"),"publication.lock").getCanonicalPath();
        Map<String,Integer> paths=THREAD_INPUT_PATHS_R31.get(Thread.currentThread().getId());Entry held=OPEN.get(path);
        return paths!=null&&paths.getOrDefault(path,0)>0&&held!=null&&held.shared&&held.lock.isValid();
    }
    public static ReportInputsLeaseCP12 acquire(File filesDir)throws IOException {
        ReportInputsLeaseCP12 lease=acquire(filesDir,true);if(lease==null)throw new IOException("REPORT_INPUT_LEASE_BUSY");return lease;
    }
    public static ReportInputsLeaseCP12 tryAcquireExclusive(File filesDir)throws IOException{return acquire(filesDir,false);}
    private static synchronized ReportInputsLeaseCP12 acquire(File filesDir,boolean shared)throws IOException {
        File root=new File(filesDir,"retention-cas-cp12");if(!root.isDirectory()&&!root.mkdirs()&&!root.isDirectory())throw new IOException("REPORT_INPUT_LEASE_DIRECTORY");
        File file=ReportArchiveVerifierCP12.inside(root.getCanonicalFile(),"publication.lock");String path=file.getPath();Entry existing=OPEN.get(path);
        if(existing!=null){if(!shared||!existing.shared)return null;existing.references++;return new ReportInputsLeaseCP12(path,existing);}
        RandomAccessFile io=new RandomAccessFile(file,"rw");
        try{FileLock lock;try{lock=io.getChannel().tryLock(0,Long.MAX_VALUE,shared);}catch(OverlappingFileLockException busy){lock=null;}
            if(lock==null){io.close();return null;}Entry entry=new Entry(io,lock,shared);OPEN.put(path,entry);return new ReportInputsLeaseCP12(path,entry);
        }catch(IOException|RuntimeException failure){try{io.close();}catch(IOException close){failure.addSuppressed(close);}throw failure;}
    }
    @Override public void close()throws IOException {
        synchronized(ReportInputsLeaseCP12.class){if(closed)return;closed=true;int readers=THREAD_READERS.getOrDefault(acquiringThread,0);if(readers<=1)THREAD_READERS.remove(acquiringThread);else THREAD_READERS.put(acquiringThread,readers-1);
            if(entry.shared){Map<String,Integer> paths=THREAD_INPUT_PATHS_R31.get(acquiringThread);if(paths==null||paths.getOrDefault(path,0)<1)throw new IllegalStateException("REPORT_INPUT_SOURCE_PIN_ACCOUNTING");int count=paths.get(path);if(count==1)paths.remove(path);else paths.put(path,count-1);if(paths.isEmpty())THREAD_INPUT_PATHS_R31.remove(acquiringThread);}
            if(--entry.references!=0)return;
            try{entry.lock.release();}finally{try{entry.io.close();}finally{OPEN.remove(path);}}}
    }
}
