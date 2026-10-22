package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.StatFs;

import java.io.File;

/** Read-only DB/WAL/free-space truth. It never deletes canonical or OOS data. */
public final class StorageHealthR811 {
    private static final long MIN_FREE_BYTES = 100L * 1024L * 1024L;
    private StorageHealthR811() {}

    public static Snapshot snapshot(DbStore store) {
        if (store == null) return Snapshot.error("STORE_NULL");
        try {
            SQLiteDatabase db = store.readable(); File main = new File(db.getPath()); File parent = main.getParentFile();
            StatFs fs = new StatFs((parent == null ? main : parent).getAbsolutePath());
            long free = fs.getAvailableBytes(), total = fs.getTotalBytes();
            long dbBytes = main.exists() ? main.length() : 0L;
            File wal = new File(main.getPath() + "-wal"), shm = new File(main.getPath() + "-shm");
            double ratio = total <= 0L ? 0.0d : ((double) free) / total;
            return new Snapshot(dbBytes, wal.exists() ? wal.length() : 0L, shm.exists() ? shm.length() : 0L, free, total, ratio, free < MIN_FREE_BYTES || ratio < 0.05d, "OK");
        } catch (Throwable e) { return Snapshot.error(e.getClass().getSimpleName()); }
    }
    public static boolean lowStorage(DbStore store) { return snapshot(store).lowStorage; }
    public static long walBytes(DbStore store) { return snapshot(store).walBytes; }
    public static long dbBytes(DbStore store) { return snapshot(store).dbBytes; }
    public static WalCheckpoint checkpointWalQuiet(final DbStore store) {
        if (store == null) return WalCheckpoint.blocked("STORE_NULL");
        if (RealtimePressureGateR811.isUnderPressure())
            return WalCheckpoint.blocked("YIELD_REALTIME");
        return store.coordinatorR36().runWrite(
                "R811_WAL_PASSIVE_CHECKPOINT",
                new DatabaseCoordinator.WriteCallable<WalCheckpoint>() {
                    public WalCheckpoint call(SQLiteDatabase db) {
                        if (RealtimePressureGateR811.isUnderPressure())
                            return WalCheckpoint.blocked("YIELD_REALTIME");
                        Cursor cursor = null;
                        try {
                            cursor = db.rawQuery("PRAGMA wal_checkpoint(PASSIVE)", null);
                            if (!cursor.moveToFirst())
                                return WalCheckpoint.blocked("CHECKPOINT_NO_RESULT");
                            int busy = cursor.getInt(0);
                            int logFrames = cursor.getInt(1);
                            int checkpointed = cursor.getInt(2);
                            return new WalCheckpoint(
                                    busy == 0,
                                    busy,
                                    logFrames,
                                    checkpointed,
                                    busy == 0 ? "PASSIVE_OK" : "PASSIVE_BUSY");
                        } finally {
                            if (cursor != null) cursor.close();
                        }
                    }
                },
                WalCheckpoint.blocked("WRITE_FAILED"));
    }
    public static final class Snapshot {
        public final long dbBytes, walBytes, shmBytes, freeBytes, totalBytes; public final double freeRatio; public final boolean lowStorage; public final String status;
        Snapshot(long db,long wal,long shm,long free,long total,double ratio,boolean low,String status){dbBytes=db;walBytes=wal;shmBytes=shm;freeBytes=free;totalBytes=total;freeRatio=ratio;lowStorage=low;this.status=status;}
        static Snapshot error(String code){return new Snapshot(0,0,0,0,0,0.0d,true,"READ_ERROR:"+code);}
    }
    public static final class WalCheckpoint {
        public final boolean completed;
        public final int busy, logFrames, checkpointedFrames;
        public final String status;
        WalCheckpoint(boolean completed,int busy,int logFrames,int checkpointed,String status){this.completed=completed;this.busy=busy;this.logFrames=logFrames;checkpointedFrames=checkpointed;this.status=status;}
        static WalCheckpoint blocked(String status){return new WalCheckpoint(false,-1,0,0,status);}
    }
}
