package com.ethk3.novaonepro;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import com.ethk3.novaonepro.strategyv2.IssueTimelineWriterR30;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Identifies the exact observation owner and, when available, the immutable SQL image. */
public final class RuntimeObservationScopeR341 {
    static final String DATABASE = "r341_observation_database";
    static final String EVENT_SCOPE = "r341_runtime_event_scope";
    static final String UNRESOLVED = "UNRESOLVED";
    static final String LEGACY_UNSCOPED = "LEGACY_UNSCOPED";
    private static final Map<SQLiteDatabase, String> KNOWN_DATABASES = new WeakHashMap<>();

    public enum Kind { PROCESS_SAMPLE, SQL_SNAPSHOT }

    public interface Clock {
        long wall();
        long monotonic();
    }

    public static final Clock SYSTEM = new Clock() {
        @Override public long wall() { return TimeSourceR811.wallNowMs(); }
        @Override public long monotonic() { return TimeSourceR811.monoNowMs(); }
    };

    public final String releaseId;
    public final String sourceCommit;
    public final String processEpoch;
    public final String bootId;
    public final String databaseIdentity;
    public final Kind scopeKind;
    public final long sampleWallAt;
    public final long sampleMonoAt;
    public final String sqlWatermark;

    RuntimeObservationScopeR341(String releaseId, String sourceCommit, String processEpoch,
            String bootId, String databaseIdentity, long wall, long monotonic, String sqlWatermark) {
        this.releaseId = releaseId;
        this.sourceCommit = sourceCommit;
        this.processEpoch = processEpoch;
        this.bootId = bootId;
        this.databaseIdentity = databaseIdentity;
        this.sampleWallAt = wall;
        this.sampleMonoAt = monotonic;
        this.sqlWatermark = sqlWatermark;
        this.scopeKind = sqlWatermark == null ? Kind.PROCESS_SAMPLE : Kind.SQL_SNAPSHOT;
    }

    public static RuntimeObservationScopeR341 capture(Context context, String databaseIdentity,
            String sqlWatermark, Clock clock) {
        return capture(databaseIdentity, sqlWatermark, clock);
    }

    static RuntimeObservationScopeR341 capture(String databaseIdentity, String sqlWatermark, Clock clock) {
        if (sqlWatermark != null && !sqlWatermark.matches("[a-f0-9]{64}")) {
            throw new IllegalArgumentException("R341_SNAPSHOT_HASH_REQUIRED");
        }
        return new RuntimeObservationScopeR341(ReleaseManifestR926.MANIFEST_SHA256,
                SourceManifestR815.observationSourceCommit(), ProcessBootIdentityR906.id(),
                IssueTimelineWriterR30.bootId(), knownOrUnresolved(databaseIdentity),
                clock.wall(), clock.monotonic(), sqlWatermark);
    }

    static String knownOrUnresolved(String value) {
        return value == null || value.isEmpty() ? UNRESOLVED : value;
    }

    public boolean identified() {
        return sourceCommit != null && sourceCommit.matches("[a-f0-9]{40}")
                && releaseId != null && releaseId.matches("[a-f0-9]{64}")
                && !unknown(databaseIdentity) && !unknown(bootId) && !unknown(processEpoch)
                && sampleWallAt > 0 && sampleMonoAt >= 0;
    }

    private static boolean unknown(String value) {
        return value == null || value.isEmpty() || UNRESOLVED.equals(value) || LEGACY_UNSCOPED.equals(value);
    }

    /** Ordered comparison only. A heartbeat and a report may be shown together as separate observations. */
    public String validateJoin(RuntimeObservationScopeR341 later) {
        if (later == null || !identified() || !later.identified()) return "IDENTITY_UNAVAILABLE";
        if (!databaseIdentity.equals(later.databaseIdentity)) return "DATABASE_CHANGED";
        if (!releaseId.equals(later.releaseId) || !sourceCommit.equals(later.sourceCommit)) return "RELEASE_CHANGED";
        if (!bootId.equals(later.bootId)) return "BOOT_CHANGED";
        if (later.sampleMonoAt < sampleMonoAt || later.sampleWallAt < sampleWallAt) return "CLOCK_ORDER_UNVERIFIED";
        if (scopeKind != later.scopeKind) return "SNAPSHOT_AND_PROCESS_SAMPLE_DIFFER";
        if (scopeKind == Kind.SQL_SNAPSHOT) {
            return sqlWatermark.equals(later.sqlWatermark) ? "COMPATIBLE" : "SQL_WATERMARK_CHANGED";
        }
        return processEpoch.equals(later.processEpoch) ? "COMPATIBLE" : "PROCESS_CHANGED";
    }

    public Map<String, Object> exportEnvelope() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("schema", "R341_RUNTIME_SCOPE_1");
        values.put("releaseId", releaseId);
        values.put("sourceCommit", sourceCommit);
        values.put("processEpoch", processEpoch);
        values.put("bootId", bootId);
        values.put("databaseIdentity", databaseIdentity);
        values.put("sampleWallAt", sampleWallAt);
        values.put("sampleMonoAt", sampleMonoAt);
        values.put("sqlWatermark", sqlWatermark);
        values.put("scopeKind", scopeKind.name());
        return values;
    }

    static void installWithinTransaction(SQLiteDatabase db) {
        if (!db.inTransaction()) throw new IllegalStateException("R341_SCOPE_MIGRATION_WRITER");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + DATABASE
                + "(singleton INTEGER PRIMARY KEY CHECK(singleton=1), identity TEXT NOT NULL)");
        db.execSQL("INSERT OR IGNORE INTO " + DATABASE + " VALUES(1,?)",
                new Object[]{java.util.UUID.randomUUID().toString()});
        db.execSQL("CREATE TABLE IF NOT EXISTS " + EVENT_SCOPE
                + "(event_id TEXT PRIMARY KEY,release_id TEXT NOT NULL,source_commit TEXT NOT NULL,"
                + "process_epoch TEXT NOT NULL,boot_id TEXT NOT NULL,database_identity TEXT NOT NULL,"
                + "sample_mono_at INTEGER NOT NULL,sample_wall_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS r341_observation_release ON " + EVENT_SCOPE
                + "(release_id,source_commit,database_identity,sample_wall_at)");
    }

    /** Live handles can be reused; a report's independently opened immutable handle reads its own identity. */
    public static String databaseIdentity(SQLiteDatabase db) {
        if (db == null) return UNRESOLVED;
        synchronized (KNOWN_DATABASES) {
            String cached = KNOWN_DATABASES.get(db);
            if (cached != null) return cached;
        }
        try (Cursor row = db.rawQuery("SELECT identity FROM " + DATABASE + " WHERE singleton=1", null)) {
            if (!row.moveToFirst()) return UNRESOLVED;
            String id = row.getString(0);
            if (id == null || !id.matches("[a-f0-9-]{36}")) return UNRESOLVED;
            synchronized (KNOWN_DATABASES) {
                KNOWN_DATABASES.put(db, id);
            }
            return id;
        } catch (SQLiteException failure) {
            if (failure.getMessage() != null && failure.getMessage().contains("no such table: " + DATABASE)) {
                return UNRESOLVED;
            }
            throw failure;
        }
    }
}
