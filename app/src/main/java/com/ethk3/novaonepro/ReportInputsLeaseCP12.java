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
    private static final class Entry {
        final RandomAccessFile io;final FileLock lock;final boolean shared;int references=1;
        Entry(RandomAccessFile io,FileLock lock,boolean shared){this.io=io;this.lock=lock;this.shared=shared;}
    }
    private final String path;private final Entry entry;private boolean closed;
    private ReportInputsLeaseCP12(String path,Entry entry){this.path=path;this.entry=entry;}
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
        synchronized(ReportInputsLeaseCP12.class){if(closed)return;closed=true;if(--entry.references!=0)return;
            try{entry.lock.release();}finally{try{entry.io.close();}finally{OPEN.remove(path);}}}
    }
}
