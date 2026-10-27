package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.File;

/**
 * Database file actually shrinks (P32).
 * Root cause: DbHelper.onConfigure ran "PRAGMA auto_vacuum=INCREMENTAL" on every open,
 * but on an existing database created with auto_vacuum=NONE this has no effect until a
 * full VACUUM; StorageReclaimR943 then returned LEGACY_COMPACTION_REQUIRED for mode 0
 * and never compacted. After the 5000-period cap deletes rows the file stays at its
 * high-water mark forever.
 * Fix: one-time conversion (auto_vacuum=INCREMENTAL + VACUUM) in a quiet window when
 * the free list is large; afterwards StorageReclaimR943's incremental path works.
 */
public final class DatabaseCompactionSU2 {
    private DatabaseCompactionSU2() {}
    public static final long MAX_INLINE_BYTES = 768L << 20;   // larger files use the explicit offline R82 rebuild

    static long scalar(SQLiteDatabase db, String pragma) { try (Cursor c = db.rawQuery("PRAGMA " + pragma, null)) { return c.moveToFirst() ? c.getLong(0) : 0; } }

    /** Decide; executed by StorageReclaimR943.quiet() on the non-transactional writer handle. */
    public static String convertIfNeeded(SQLiteDatabase db, long freeDiskBytes) {
        if (db.inTransaction()) return "YIELD_TRANSACTION";
        if (RealtimePressureGateR811.hasActiveCriticalWork()) return "YIELD_REALTIME";
        long mode = scalar(db, "auto_vacuum"), pages = scalar(db, "page_count"), free = scalar(db, "freelist_count"), page = scalar(db, "page_size");
        long bytes = new File(db.getPath()).length();
        if (mode == 2) return "ALREADY_INCREMENTAL";
        boolean worth = free > 1024 && free * 4 > pages;                // >= 25% reusable
        if (!worth && mode == 0 && pages * page < (64L << 20)) return "SMALL_NO_ACTION";
        if (bytes > MAX_INLINE_BYTES) return "OFFLINE_REBUILD_REQUIRED|bytes=" + bytes;     // R82 explicit maintenance
        if (freeDiskBytes < 2 * bytes + (64L << 20)) return "DISK_TOO_FULL_FOR_VACUUM|need=" + (2 * bytes + (64L << 20));
        db.execSQL("PRAGMA auto_vacuum=INCREMENTAL");
        db.execSQL("VACUUM");                                           // rewrites the file; auto_vacuum now takes effect
        try (Cursor c = db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null)) { c.moveToFirst(); }
        return "CONVERTED|before=" + bytes + "|after=" + new File(db.getPath()).length() + "|mode=" + scalar(db, "auto_vacuum");
    }
}
