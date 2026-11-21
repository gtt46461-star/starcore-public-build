package com.ethk3.novaonepro;

import java.io.*;
import java.nio.channels.*;
import java.util.*;

/** One OS lock description per JVM/path. All r926 creators/readers take a shared
 * lease; final retirement takes an exclusive lease without waiting under actors.
 * A JSON flag or a restored receipt cannot construct this capability. */
final class LegacyRecoveryPointLeaseR335 implements AutoCloseable {
    private static final Map<String,Entry> OPEN=new HashMap<>();
    private static final class Entry {
        final RandomAccessFile file;final FileLock lock;final boolean exclusive;int refs=1;
        Entry(RandomAccessFile f,FileLock l,boolean x){file=f;lock=l;exclusive=x;}
    }
    private final String path;private final Entry entry;private boolean closed;
    private LegacyRecoveryPointLeaseR335(String p,Entry e){path=p;entry=e;}
    private static String path(File database)throws IOException {
        File supplied=database.getAbsoluteFile();if(supplied.getParentFile()==null)throw new IOException("R335_RECOVERY_LEASE_SOURCE");
        // Context may legitimately use /data/user/0 while the OS resolves its
        // trusted parent to /data/data. Reject a symlink at the DB/lock leaf,
        // not Android's canonical application-directory alias.
        File db=new File(supplied.getParentFile().getCanonicalFile(),supplied.getName());
        if(!db.equals(db.getCanonicalFile())||!AppConfig.DB_NAME.equals(db.getName())||!db.getParentFile().isDirectory())
            throw new IOException("R335_RECOVERY_LEASE_SOURCE");
        File lock=new File(db.getParentFile(),db.getName()+".r335-recovery-point.lock");
        if(!lock.equals(lock.getCanonicalFile()))throw new IOException("R335_RECOVERY_LEASE_SYMLINK");
        return lock.getPath();
    }
    static LegacyRecoveryPointLeaseR335 shared(File database)throws IOException {
        LegacyRecoveryPointLeaseR335 result=acquire(database,false);
        if(result==null)throw new IOException("WAIT_R335_RECOVERY_POINT_RETIRING");return result;
    }
    static LegacyRecoveryPointLeaseR335 exclusive(File database)throws IOException {
        LegacyRecoveryPointLeaseR335 result=acquire(database,true);
        if(result==null)throw new IOException("WAIT_R335_RECOVERY_POINT_IN_USE");return result;
    }
    private static synchronized LegacyRecoveryPointLeaseR335 acquire(File database,boolean exclusive)throws IOException {
        String path=path(database);Entry old=OPEN.get(path);
        if(old!=null){if(exclusive||old.exclusive)return null;old.refs++;return new LegacyRecoveryPointLeaseR335(path,old);}
        RandomAccessFile file=new RandomAccessFile(path,"rw");
        try{FileLock lock;try{lock=file.getChannel().tryLock(0,Long.MAX_VALUE,!exclusive);}catch(OverlappingFileLockException busy){lock=null;}
            if(lock==null){file.close();return null;}Entry entry=new Entry(file,lock,exclusive);OPEN.put(path,entry);return new LegacyRecoveryPointLeaseR335(path,entry);
        }catch(IOException|RuntimeException failure){try{file.close();}catch(IOException suppressed){failure.addSuppressed(suppressed);}throw failure;}
    }
    synchronized void requireExclusive(File database)throws IOException {
        synchronized(LegacyRecoveryPointLeaseR335.class){if(closed||!entry.exclusive||OPEN.get(path)!=entry||!entry.lock.isValid()||!path.equals(path(database)))throw new IOException("R335_RECOVERY_RETIREMENT_LEASE_REQUIRED");}
    }
    @Override public void close()throws IOException {
        synchronized(LegacyRecoveryPointLeaseR335.class){if(closed)return;closed=true;if(--entry.refs!=0)return;
            try{entry.lock.release();}finally{try{entry.file.close();}finally{OPEN.remove(path);}}}
    }
}
