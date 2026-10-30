package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.nio.file.*;
import java.sql.DriverManager;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/** Real WAL files plus an explicitly modelled Android primary-connection lease. This does not execute Android.
 * Android prepares PRAGMA using the handle's primary pool connection; ordinary SELECT is a read connection.
 * The held lease is deliberately shared only by the RT writer's RW handle, not a separately opened RO handle. */
public final class RootReadPoolR29HostTest {
    static int checks;
    static void check(String id,boolean ok){System.out.println("R29_ROOT_READ_POOL|"+id+"|"+(ok?"OK":"FAILED"));if(!ok)throw new AssertionError(id);checks++;}
    static final class AndroidPrimary extends SQLiteDatabase {
        final ReentrantLock primary;int primaryReads;
        AndroidPrimary(java.sql.Connection connection,ReentrantLock primary){super(connection);this.primary=primary;}
        @Override public Cursor rawQuery(String sql,String[] args){
            boolean lease=primary!=null&&sql.trim().toUpperCase(java.util.Locale.ROOT).startsWith("PRAGMA");
            if(lease){try{primary.lockInterruptibly();primaryReads++;}catch(InterruptedException cancelled){Thread.currentThread().interrupt();throw new IllegalStateException("PRIMARY_CAPTURE_CANCELLED",cancelled);}}
            try{return super.rawQuery(sql,args);}finally{if(lease)primary.unlock();}
        }
    }
    public static void main(String[] args)throws Exception{
        boolean legacy=args.length>0&&args[0].equals("--shared-rw-control");Path dir=Files.createTempDirectory("r29-root-read-pool-");ReentrantLock primary=new ReentrantLock();ExecutorService worker=Executors.newSingleThreadExecutor();
        try(AndroidPrimary root=new AndroidPrimary(DriverManager.getConnection("jdbc:sqlite:"+dir.resolve("root.db")),primary)){
            root.execSQL("PRAGMA journal_mode=WAL");root.execSQL("CREATE TABLE input(value INTEGER)");root.execSQL("INSERT INTO input VALUES(1)");
            try(AndroidPrimary readonly=new AndroidPrimary(DriverManager.getConnection("jdbc:sqlite:"+dir.resolve("root.db")),null);SQLiteDatabase research=new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite:"+dir.resolve("research.db")))){
                readonly.execSQL("PRAGMA query_only=1");research.execSQL("CREATE TABLE state(cursor INTEGER)");research.execSQL("INSERT INTO state VALUES(4)");
                SQLiteDatabase selected=legacy?root:ResearchRouteR29.rootSnapshotSource(root,readonly);
                check("R1_SEPARATE_ROOT_HANDLE_IS_SELECTED",legacy||selected==readonly);
                primary.lock();root.jdbc.setAutoCommit(false);root.execSQL("UPDATE input SET value=2");Future<Long> capture=null;
                try{
                    capture=worker.submit(()->{long version=MigrationCopyR29.scalar(selected,"PRAGMA schema_version",null);long committed=MigrationCopyR29.scalar(selected,"SELECT value FROM input",null);if(committed!=1)throw new AssertionError("UNCOMMITTED_RT_INPUT_VISIBLE");research.jdbc.setAutoCommit(false);try{research.execSQL("UPDATE state SET cursor=6");research.jdbc.commit();}finally{research.jdbc.setAutoCommit(true);}return version;});
                    long version;
                    try{version=capture.get(250,TimeUnit.MILLISECONDS);}catch(TimeoutException blocked){throw new AssertionError("PRIMARY_POOL_CAPTURE_WAITED_FOR_RT_WRITER",blocked);}
                    check("R2_PRAGMA_CAPTURE_AND_RP_COMMIT_FINISH_WHILE_RT_LEASE_HELD",version>0&&MigrationCopyR29.scalar(research,"SELECT cursor FROM state",null)==6);
                    check("R3_CAPTURE_NEVER_LEASES_THE_SHARED_RT_PRIMARY",root.primaryReads==0);
                }finally{if(capture!=null&&!capture.isDone())capture.cancel(true);root.jdbc.rollback();root.jdbc.setAutoCommit(true);primary.unlock();}
                boolean rejected=false;try{ResearchRouteR29.rootSnapshotSource(root,research);}catch(IllegalStateException expected){rejected=expected.getMessage().contains("PATH_MISMATCH");}
                check("R4_ANOTHER_DATABASE_CANNOT_IMPERSONATE_ROOT_SNAPSHOT",rejected);
                rejected=false;try{ResearchRouteR29.rootSnapshotSource(root,root);}catch(IllegalStateException expected){rejected=expected.getMessage().contains("QUERY_ONLY");}
                check("R5_SHARED_WRITABLE_HANDLE_IS_REJECTED",rejected);
            }
        }finally{worker.shutdownNow();try(java.util.stream.Stream<Path> files=Files.walk(dir)){files.sorted(java.util.Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(Exception ignored){}});}}
        System.out.println("R29_ROOT_READ_POOL|ALL_PASS|checks="+checks+"|REAL_SQLITE_WAL_WITH_MODELLED_ANDROID_PRIMARY_LEASE;ANDROID_RETEST_PENDING");
    }
}
