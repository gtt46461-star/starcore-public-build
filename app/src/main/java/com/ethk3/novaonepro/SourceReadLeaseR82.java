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
        RandomAccessFile file;FileLock os;int references;
    }
    private final Entry entry;private final boolean exclusive;private boolean closed;
    private SourceReadLeaseR82(Entry e,boolean x){entry=e;exclusive=x;}
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
        File db=database.getCanonicalFile(),parent=db.getParentFile();
        if(!AppConfig.DB_NAME.equals(db.getName())||parent==null||!"databases".equals(parent.getName())||parent.getParentFile()==null||!"com.ethk3.novaonepro".equals(parent.getParentFile().getName()))
            throw new IOException("untrusted source database path");
        File lockFile=new File(parent,db.getName()+".r82-source-read.lock");
        if(!lockFile.getCanonicalFile().equals(lockFile))throw new IOException("source lock symlink refused");
        Entry e;synchronized(ENTRIES){e=ENTRIES.get(db.getPath());if(e==null){e=new Entry();ENTRIES.put(db.getPath(),e);}}
        java.util.concurrent.locks.Lock local=exclusive?e.lock.writeLock():e.lock.readLock();
        boolean held=false,registered=false;
        try{
            long end=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(maxWaitMs);
            while(!(held=local.tryLock(50,TimeUnit.MILLISECONDS))){check(cancel,continuing);if(System.nanoTime()>end)throw new IOException("SOURCE_READ_LEASE_TIMEOUT");}
            check(cancel,continuing);
            synchronized(e){
                if(e.references==0){
                    e.file=new RandomAccessFile(lockFile,"rw");
                    try{
                        while(e.os==null){check(cancel,continuing);if(System.nanoTime()>end)throw new IOException("SOURCE_READ_LEASE_TIMEOUT");
                            e.os=e.file.getChannel().tryLock(0,Long.MAX_VALUE,!exclusive);if(e.os==null)Thread.sleep(25);}
                    }catch(Throwable failure){try{e.file.close();}catch(IOException suppressed){failure.addSuppressed(suppressed);}e.file=null;e.os=null;throw failure;}
                }
                e.references++;registered=true;
            }
            return new SourceReadLeaseR82(e,exclusive);
        }catch(InterruptedException e1){Thread.currentThread().interrupt();throw new InterruptedIOException("source lease interrupted");}
        catch(IOException|RuntimeException|Error failure){throw failure;}
        finally{if(held&&!registered)local.unlock();}
    }
    @Override public void close()throws IOException {
        if(closed)return;closed=true;IOException failure=null;
        try{synchronized(entry){if(--entry.references==0){try{entry.os.release();}catch(IOException e){failure=e;}finally{entry.os=null;try{entry.file.close();}catch(IOException e){if(failure==null)failure=e;else failure.addSuppressed(e);}entry.file=null;}}}}
        finally{if(exclusive)entry.lock.writeLock().unlock();else entry.lock.readLock().unlock();}
        if(failure!=null)throw failure;
    }
}
