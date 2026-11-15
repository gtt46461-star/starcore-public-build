package com.ethk3.novaonepro;

import android.content.Context;
import android.os.CancellationSignal;
import java.io.*;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/** A source reader holds one shared OS lease for its entire SQLite connection.
 * All readers in one JVM share one FileLock; this avoids OverlappingFileLockException.
 * Offline swap holds the exclusive OS lease, including the two durable renames. */
public final class SourceReadLeaseR82 implements AutoCloseable {
    private static final HashMap<String,Entry> ENTRIES=new HashMap<String,Entry>();
    private static final class Entry {
        final ReentrantReadWriteLock lock=new ReentrantReadWriteLock(true);
        final java.util.concurrent.locks.ReentrantLock metadata=new java.util.concurrent.locks.ReentrantLock();
        RandomAccessFile file;FileLock os;int references;
        final java.util.concurrent.ConcurrentMap<SourceReadLeaseR82,Boolean> readers=new java.util.concurrent.ConcurrentHashMap<>();
    }
    private static final java.util.concurrent.ThreadPoolExecutor CANCELLERS=new java.util.concurrent.ThreadPoolExecutor(1,1,0L,TimeUnit.MILLISECONDS,new java.util.concurrent.ArrayBlockingQueue<Runnable>(32),r->{Thread t=new Thread(r,"source-reader-owner-cancel-r31");t.setDaemon(true);return t;},new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());
    private final Entry entry;private final boolean exclusive;private volatile boolean closed;
    private final CancellationSignal cancellation;private final java.util.function.BooleanSupplier continuing;
    private volatile boolean cancellationRequested;private volatile long lastProgressMs=System.nanoTime()/1000000L,completedWork=-1;
    private SourceReadLeaseR82(Entry e,boolean x,CancellationSignal cancel,java.util.function.BooleanSupplier continuing){entry=e;exclusive=x;cancellation=cancel;this.continuing=continuing;if(!x)e.readers.put(this,Boolean.TRUE);}
    /** An owner polls this in its own native/Java read loop and closes its own
     * connection on cancellation. Maintenance never closes another connection. */
    public boolean shouldContinue(){return !closed&&!cancellationRequested&&(cancellation==null||!cancellation.isCanceled())&&(continuing==null||continuing.getAsBoolean());}
    boolean exclusiveCheckpointR31(){return exclusive&&!closed;}
    /** Only actual increasing page/byte completion renews a reader, not heartbeats. */
    public void progress(long completed){if(completed>completedWork){completedWork=completed;lastProgressMs=System.nanoTime()/1000000L;}}
    public static boolean requestStaleReaders(File database,long staleMs)throws IOException {
        if(staleMs<1)throw new IllegalArgumentException("SOURCE_READER_STALE_BOUND");Entry e;synchronized(ENTRIES){e=ENTRIES.get(database.getCanonicalPath());}if(e==null)return true;long now=System.nanoTime()/1000000L;
        for(SourceReadLeaseR82 reader:e.readers.keySet())if(!reader.closed&&!reader.cancellationRequested&&((reader.cancellation!=null&&reader.cancellation.isCanceled())||now-reader.lastProgressMs>=staleMs)){
            reader.cancellationRequested=true;
            // CancellationSignal listeners run on a bounded owner-cancel worker;
            // an arbitrary listener must not block a writer or a maintenance slice.
            if(reader.cancellation!=null)try{CANCELLERS.execute(reader.cancellation::cancel);}catch(java.util.concurrent.RejectedExecutionException saturated){/* Still pinned until the owner actually closes. */}
        }
        return e.readers.isEmpty();
    }
    private static File trusted(File database)throws IOException {
        File db=database.getCanonicalFile(),parent=db.getParentFile();
        if(!AppConfig.DB_NAME.equals(db.getName())||parent==null||!"databases".equals(parent.getName())||parent.getParentFile()==null||!ReleaseManifestR926.APPLICATION_ID.equals(parent.getParentFile().getName()))throw new IOException("untrusted source database path");return db;
    }
    /** A single try, never the 30-second offline lease. Unknown cross-process
     * source readers keep their OS shared lock, so no drain is inferred. */
    public static SourceReadLeaseR82 tryExclusiveCheckpoint(File database,java.util.function.BooleanSupplier continuing)throws IOException {
        return tryCheckpoint(database,true,continuing);
    }
    public static SourceReadLeaseR82 trySharedCheckpoint(File database,java.util.function.BooleanSupplier continuing)throws IOException{return tryCheckpoint(database,false,continuing);}
    private static SourceReadLeaseR82 tryCheckpoint(File database,boolean exclusive,java.util.function.BooleanSupplier continuing)throws IOException {
        File db=trusted(database);check(null,continuing);Entry e;synchronized(ENTRIES){e=ENTRIES.get(db.getPath());if(e==null){e=new Entry();ENTRIES.put(db.getPath(),e);}}
        java.util.concurrent.locks.Lock local=exclusive?e.lock.writeLock():e.lock.readLock();if(!local.tryLock())return null;boolean registered=false,metadata=false,opened=false;
        try{if(!(metadata=e.metadata.tryLock()))return null;if(exclusive&&e.references!=0)return null;
            if(e.references==0){File path=new File(db.getParentFile(),db.getName()+".r82-source-read.lock");if(!path.getCanonicalFile().equals(path))throw new IOException("source lock symlink refused");e.file=new RandomAccessFile(path,"rw");opened=true;
                try{check(null,continuing);e.os=e.file.getChannel().tryLock(0,Long.MAX_VALUE,!exclusive);}catch(OverlappingFileLockException busy){e.os=null;}
                if(e.os==null){e.file.close();e.file=null;return null;}}
            e.references++;registered=true;return new SourceReadLeaseR82(e,exclusive,null,continuing);
        }catch(IOException|RuntimeException|Error failure){if(opened&&!registered){if(e.os!=null)try{e.os.release();}catch(IOException suppressed){failure.addSuppressed(suppressed);}e.os=null;if(e.file!=null)try{e.file.close();}catch(IOException suppressed){failure.addSuppressed(suppressed);}e.file=null;}throw failure;}
        finally{if(metadata)e.metadata.unlock();if(!registered)local.unlock();}
    }
    public static SourceReadLeaseR82 acquire(Context context,File database,CancellationSignal cancel)throws IOException {
        File expected=context.getApplicationContext().getDatabasePath(AppConfig.DB_NAME).getCanonicalFile();
        if(!expected.equals(database.getCanonicalFile()))throw new IOException("source lease database mismatch");
        return acquire(database,false,cancel,null,30000L);
    }
    public static SourceReadLeaseR82 acquire(File database)throws IOException{return acquire(database,false,null,null,30000L);}
    public static SourceReadLeaseR82 acquire(File database,CancellationSignal cancel)throws IOException{return acquire(database,false,cancel,null,30000L);}
    public static SourceReadLeaseR82 exclusive(File database)throws IOException{return acquire(database,true,null,null,30000L);}
    public static SourceReadLeaseR82 acquire(File database,java.util.function.BooleanSupplier continuing,long maxWaitMs)throws IOException {
        if(continuing==null||maxWaitMs<1||maxWaitMs>30000)throw new IllegalArgumentException("SOURCE_LEASE_BOUND");
        return acquire(database,false,null,continuing,maxWaitMs);
    }
    private static void check(CancellationSignal cancel,java.util.function.BooleanSupplier continuing)throws IOException {
        if(cancel!=null)cancel.throwIfCanceled();
        if(continuing!=null&&!continuing.getAsBoolean())throw new InterruptedIOException("SOURCE_READ_LEASE_CANCELLED");
    }
    private static SourceReadLeaseR82 acquire(File database,boolean exclusive,CancellationSignal cancel,java.util.function.BooleanSupplier continuing,long maxWaitMs)throws IOException {
        File db=trusted(database),parent=db.getParentFile();
        File lockFile=new File(parent,db.getName()+".r82-source-read.lock");
        if(!lockFile.getCanonicalFile().equals(lockFile))throw new IOException("source lock symlink refused");
        Entry e;synchronized(ENTRIES){e=ENTRIES.get(db.getPath());if(e==null){e=new Entry();ENTRIES.put(db.getPath(),e);}}
        java.util.concurrent.locks.Lock local=exclusive?e.lock.writeLock():e.lock.readLock();
        boolean held=false,registered=false;
        try{
            long end=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(maxWaitMs);
            while(!(held=local.tryLock(Math.max(1,Math.min(50,TimeUnit.NANOSECONDS.toMillis(end-System.nanoTime()))),TimeUnit.MILLISECONDS))){check(cancel,continuing);if(System.nanoTime()>end)throw new IOException("SOURCE_READ_LEASE_TIMEOUT");}
            check(cancel,continuing);
            if(!e.metadata.tryLock(Math.max(1,TimeUnit.NANOSECONDS.toMillis(end-System.nanoTime())),TimeUnit.MILLISECONDS))throw new IOException("SOURCE_READ_LEASE_TIMEOUT");
            try{
                if(e.references==0){
                    e.file=new RandomAccessFile(lockFile,"rw");
                    try{
                        while(e.os==null){check(cancel,continuing);if(System.nanoTime()>end)throw new IOException("SOURCE_READ_LEASE_TIMEOUT");
                            e.os=e.file.getChannel().tryLock(0,Long.MAX_VALUE,!exclusive);if(e.os==null)Thread.sleep(Math.max(1,Math.min(25,TimeUnit.NANOSECONDS.toMillis(end-System.nanoTime()))));}
                    }catch(Throwable failure){try{e.file.close();}catch(IOException suppressed){failure.addSuppressed(suppressed);}e.file=null;e.os=null;throw failure;}
                }
                e.references++;registered=true;
            }finally{e.metadata.unlock();}
            return new SourceReadLeaseR82(e,exclusive,cancel,continuing);
        }catch(InterruptedException e1){Thread.currentThread().interrupt();throw new InterruptedIOException("source lease interrupted");}
        catch(IOException|RuntimeException|Error failure){throw failure;}
        finally{if(held&&!registered)local.unlock();}
    }
    @Override public void close()throws IOException {
        if(closed)return;closed=true;entry.readers.remove(this);IOException failure=null;
        entry.metadata.lock();
        try{if(--entry.references==0){try{entry.os.release();}catch(IOException e){failure=e;}finally{entry.os=null;try{entry.file.close();}catch(IOException e){if(failure==null)failure=e;else failure.addSuppressed(e);}entry.file=null;}}}
        finally{entry.metadata.unlock();if(exclusive)entry.lock.writeLock().unlock();else entry.lock.readLock().unlock();}
        if(failure!=null)throw failure;
    }
}
