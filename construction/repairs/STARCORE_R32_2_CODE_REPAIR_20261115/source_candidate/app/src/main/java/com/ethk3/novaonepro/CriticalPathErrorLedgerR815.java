package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Durable, bounded critical-path errors with a no-allocation-ish process fallback ring. */
public final class CriticalPathErrorLedgerR815 {
    private static final String TAG = "R815CriticalError";
    private static final int MAX_DETAIL = 512;
    private static final int RING_SIZE = 64;
    private static final Object RING_LOCK = new Object();
    private static final Event[] RING = new Event[RING_SIZE];
    private static int nextRingSlot;
    private static final AtomicInteger FALLBACK_COUNT = new AtomicInteger();
    private static volatile long lastFallbackLogAt;

    public static final class Event {
        public final String domain;
        public final String code;
        public final String targetIssue;
        public final long generation;
        public final boolean retryable;
        public final String throwableClass;
        public final String detail;
        public final long createdAt;
        public final String eventKey;

        public Event(
                String domain,
                String code,
                String targetIssue,
                long generation,
                boolean retryable,
                Throwable cause,
                String detail) {
            this(
                    domain,
                    code,
                    targetIssue,
                    generation,
                    retryable,
                    cause == null ? "" : cause.getClass().getSimpleName(),
                    detail,
                    System.currentTimeMillis());
        }

        public Event(
                String domain,
                String code,
                String targetIssue,
                long generation,
                boolean retryable,
                String throwableClass,
                String detail,
                long createdAt) {
            this.domain = required("domain", domain, 64);
            this.code = required("code", code, 96);
            this.targetIssue = validTarget(targetIssue);
            this.generation = Math.max(0L, generation);
            this.retryable = retryable;
            this.throwableClass = bounded(throwableClass, 96);
            this.detail = bounded(detail, MAX_DETAIL);
            this.createdAt = createdAt > 0L ? createdAt : System.currentTimeMillis();
            this.eventKey =
                    V51ChecksumEngine.sha256(
                            "R815_ERROR|"
                                    + this.domain
                                    + '|'
                                    + this.code
                                    + '|'
                                    + this.targetIssue
                                    + '|'
                                    + this.generation
                                    + '|'
                                    + (this.retryable ? 1 : 0)
                                    + '|'
                                    + this.throwableClass
                                    + '|'
                                    + this.detail);
        }

        public String summary() {
            return domain
                    + '|'
                    + code
                    + "|target="
                    + targetIssue
                    + "|generation="
                    + generation
                    + "|retryable="
                    + (retryable ? 1 : 0)
                    + "|key="
                    + eventKey;
        }
    }

    private CriticalPathErrorLedgerR815() {}

    public static boolean record(final DbStore store, final Event event) {
        if (event == null) {
            fallback(
                    new Event(
                            "LEDGER", "EVENT_NULL", "", 0L, false, "", "record received null", 0L));
            return false;
        }
        if (store == null) {
            fallback(event);
            return false;
        }
        try {
            Boolean written =
                    store.coordinatorR36()
                            .runWrite(
                                    "R815_CRITICAL_ERROR",
                                    new DatabaseCoordinator.WriteCallable<Boolean>() {
                                        @Override
                                        public Boolean call(SQLiteDatabase db) {
                                            ContentValues values = new ContentValues();
                                            values.put("event_key", event.eventKey);
                                            values.put("domain", event.domain);
                                            values.put("target_issue", event.targetIssue);
                                            values.put("generation", event.generation);
                                            values.put("error_code", event.code);
                                            values.put("throwable_class", event.throwableClass);
                                            values.put("detail", event.detail);
                                            values.put("retryable", event.retryable ? 1 : 0);
                                            values.put("created_at", event.createdAt);
                                            values.put("resolved_at", 0L);
                                            long inserted =
                                                    db.insertWithOnConflict(
                                                            DbHelper.TABLE_R815_CRITICAL_ERROR,
                                                            null,
                                                            values,
                                                            SQLiteDatabase.CONFLICT_IGNORE);
                                            if (inserted >= 0L) return Boolean.TRUE;
                                            Cursor cursor = null;
                                            try {
                                                cursor =
                                                        db.rawQuery(
                                                                "SELECT event_key FROM "
                                                                        + DbHelper.TABLE_R815_CRITICAL_ERROR
                                                                        + " WHERE event_key=? LIMIT 1",
                                                                new String[] {event.eventKey});
                                                return Boolean.valueOf(cursor.moveToFirst());
                                            } finally {
                                                if (cursor != null) cursor.close();
                                            }
                                        }
                                    },
                                    Boolean.FALSE);
            if (written != null && written.booleanValue()) return true;
        } catch (RuntimeException failure) {
            // The fallback below records an explicit durable-failure signal without rethrowing into
            // a process-critical path that is already handling another failure.
            Log.w(TAG, "durable critical-error write failed; preserving fallback event", failure);
        }
        fallback(event);
        return false;
    }

    public static boolean resolve(final DbStore store, final String eventKey) {
        if (store == null || !sha256(eventKey)) return false;
        try {
            Boolean result =
                    store.coordinatorR36()
                            .runWrite(
                                    "R815_CRITICAL_ERROR_RESOLVE",
                                    new DatabaseCoordinator.WriteCallable<Boolean>() {
                                        @Override
                                        public Boolean call(SQLiteDatabase db) {
                                            ContentValues values = new ContentValues();
                                            values.put("resolved_at", System.currentTimeMillis());
                                            return Boolean.valueOf(
                                                    db.update(
                                                                    DbHelper.TABLE_R815_CRITICAL_ERROR,
                                                                    values,
                                                                    "event_key=? AND resolved_at=0",
                                                                    new String[] {eventKey})
                                                            <= 1);
                                        }
                                    },
                                    Boolean.FALSE);
            return result != null && result.booleanValue();
        } catch (RuntimeException failure) {
            fallback(
                    new Event(
                            "LEDGER",
                            "RESOLVE_FAILED",
                            "",
                            0L,
                            true,
                            failure,
                            bounded(eventKey, 64)));
            return false;
        }
    }

    public static List<Event> recent(DbStore store, int limit) {
        int capped = Math.max(1, Math.min(64, limit));
        ArrayList<Event> result = new ArrayList<Event>();
        if (store != null) {
            Cursor cursor = null;
            try {
                cursor =
                        store.readable()
                                .rawQuery(
                                        "SELECT domain,error_code,target_issue,generation,retryable,throwable_class,detail,created_at"
                                                + " FROM "
                                                + DbHelper.TABLE_R815_CRITICAL_ERROR
                                                + " ORDER BY created_at DESC,id DESC LIMIT "
                                                + capped,
                                        null);
                while (cursor.moveToNext()) {
                    result.add(
                            new Event(
                                    cursor.getString(0),
                                    cursor.getString(1),
                                    cursor.getString(2),
                                    cursor.getLong(3),
                                    cursor.getInt(4) == 1,
                                    cursor.getString(5),
                                    cursor.getString(6),
                                    cursor.getLong(7)));
                }
            } catch (RuntimeException failure) {
                fallback(
                        new Event(
                                "LEDGER",
                                "RECENT_READ_FAILED",
                                "",
                                0L,
                                true,
                                failure,
                                "recent"));
            } finally {
                if (cursor != null) cursor.close();
            }
        }
        if (result.isEmpty()) {
            synchronized (RING_LOCK) {
                for (int i = 0; i < RING_SIZE && result.size() < capped; i++) {
                    int index = (nextRingSlot - 1 - i + RING_SIZE) % RING_SIZE;
                    if (RING[index] != null) result.add(RING[index]);
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    public static int fallbackCount() {
        return FALLBACK_COUNT.get();
    }

    private static void fallback(Event event) {
        Event safeEvent = event == null
                ? new Event("LEDGER", "FALLBACK_NULL", "", 0L, false, "", "", 0L)
                : event;
        synchronized (RING_LOCK) {
            RING[nextRingSlot] = safeEvent;
            nextRingSlot = (nextRingSlot + 1) % RING_SIZE;
        }
        FALLBACK_COUNT.incrementAndGet();
        long now = System.currentTimeMillis();
        if (now - lastFallbackLogAt >= 60000L) {
            lastFallbackLogAt = now;
            Log.e(TAG, "R815_LEDGER_FALLBACK|" + safeEvent.summary());
        }
    }

    private static String required(String name, String value, int max) {
        String clean = bounded(value, max);
        if (clean.length() == 0) throw new IllegalArgumentException("R815_ERROR_" + name + "_EMPTY");
        return clean;
    }

    private static String validTarget(String issue) {
        String clean = bounded(issue, 12);
        if (clean.length() == 0 || TargetIssueResolver.isValid(clean)) return clean;
        throw new IllegalArgumentException("R815_ERROR_TARGET_ISSUE_INVALID");
    }

    private static boolean sha256(String value) {
        if (value == null || value.length() != 64) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) return false;
        }
        return true;
    }

    private static String bounded(String value, int max) {
        String clean = value == null ? "" : value.trim();
        return clean.length() <= max ? clean : clean.substring(0, max);
    }
}
