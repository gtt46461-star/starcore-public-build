package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Actual Gradle product observer and coordinator; synthetic host SQLite, not device latency. */
public final class CompletedStageObservationR79 {
    private static int checks;
    private static long generation;

    private static void require(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError(reason);
    }

    private static int occurrences(String text, String part) {
        return text.split(java.util.regex.Pattern.quote(part), -1).length - 1;
    }

    private static void observe(String stage, long elapsed, boolean before) throws Exception {
        if (before) {
            RuntimeResourceAuditR927.stageEnd(stage, elapsed, true);
            return;
        }
        Method method = RuntimeResourceAuditR927.class.getDeclaredMethod(
                "completedBeforeScopeR79", String.class, long.class);
        try {
            method.invoke(null, stage, elapsed);
        } catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw failure;
        }
    }

    private static AnalysisSingleFlightR36.Lease lease(boolean expired) {
        long now = System.currentTimeMillis();
        long id = ++generation;
        return AnalysisSingleFlightR36.Lease.acquire(new AnalysisSingleFlightR36.Request(
                "202610150100", "202610150101", id, now,
                expired ? now - 1 : now + 120000,
                "EXPLICIT_PRE_SCOPE_FIXTURE", V51ChecksumEngine.sha256("PRE_SCOPE_FIXTURE_" + id), ""));
    }

    private static String persisted(SQLiteDatabase db, AnalysisSingleFlightR36.Lease lease) {
        try (Cursor cursor = db.rawQuery("SELECT stage_samples FROM "
                + RuntimeResourceAuditR927.TABLE + " WHERE execution_id=?",
                new String[]{lease.ownerId})) {
            require(cursor.moveToFirst(), "Actual product resource row persisted");
            return cursor.getString(0);
        }
    }

    private static String initialObservations(DbStore store, SQLiteDatabase db, boolean before)
            throws Exception {
        AnalysisSingleFlightR36.Lease lease = lease(false);
        RuntimeResourceAuditR927.Scope scope = RuntimeResourceAuditR927.begin(store, lease);
        try {
            observe("OWNER_DISPATCH", 12, before);
            RuntimeResourceAuditR927.measuredQueueSpanCP12("OWNER_DISPATCH", 100, 112);
            observe("DURABLE_CLAIM", 23, before);
            observe("REQUEST_TO_WORKER", 56, before);
            RuntimeResourceAuditR927.measuredQueueSpanCP12("REQUEST_BEFORE_OWNER", 56, 100);
            RuntimeResourceAuditR927.stageBegin("HISTORY");
            RuntimeResourceAuditR927.stageBegin("NUMBER_MODEL");
            RuntimeResourceAuditR927.stageEnd("NUMBER_MODEL", 7, true);
            RuntimeResourceAuditR927.stageEnd("HISTORY", 11, true);
        } finally {
            RuntimeResourceAuditR927.finish(store, scope, false, "EXPLICIT_PRE_SCOPE_FIXTURE");
        }
        String spans = persisted(db, lease);
        require(spans.contains("OWNER_DISPATCH:12/0:OK;")
                        && spans.contains("DURABLE_CLAIM:23/0:OK;")
                        && spans.contains("REQUEST_TO_WORKER:56/0:OK;"),
                "Original inclusive names, durations, SLO and success status preserved");
        require(spans.contains("CP12EX1|OWNER_DISPATCH|12000000|-1|")
                        && spans.contains("CP12EX1|REQUEST_BEFORE_OWNER|44000000|-1|"),
                "Original disjoint queue intervals retained with CPU unavailable");
        require(spans.contains("CP12EX1|HISTORY|") && spans.contains("CP12EX1|NUMBER_MODEL|"),
                "Nested synchronous stages remain measured");
        String[] entries = spans.split(";", -1);
        require(entries[2].startsWith("CP12EX1|OWNER_DISPATCH|")
                        && entries[7].startsWith("CP12EX1|REQUEST_BEFORE_OWNER|")
                        && entries[9].startsWith("CP12EX1|NUMBER_MODEL|")
                        && entries[11].startsWith("CP12EX1|HISTORY|"),
                "Original queue and synchronous entry positions remain stable for latency receipts");
        require(occurrences(spans, "STAGE_STACK_MISMATCH") == (before ? 3 : 0),
                "Exactly three baseline mismatches and zero repaired mismatches");
        if (!before) {
            require(!spans.contains("CP12EX1|DURABLE_CLAIM|")
                            && !spans.contains("CP12EX1|REQUEST_TO_WORKER|"),
                    "No fabricated CPU/exclusive values or double-counted request span");
            require(occurrences(spans, "CP12EX1|OWNER_DISPATCH|") == 1,
                    "Dispatch has only its existing disjoint queue observation");
            for (String stage : new String[]{"OWNER_DISPATCH", "DURABLE_CLAIM", "REQUEST_TO_WORKER"}) {
                require(spans.contains("CP12INCLUSIVE_ONLY|" + stage + "|PRE_SCOPE_WALL_ONLY;"),
                        "Explicit inclusive-only provenance: " + stage);
            }
            require(!spans.contains("CP12EX_UNAVAILABLE|"), "No synthetic stack error or incomplete span");
        }
        return spans;
    }

    private static void liveStackAndSlo(DbStore store, SQLiteDatabase db) throws Exception {
        AnalysisSingleFlightR36.Lease lease = lease(false);
        RuntimeResourceAuditR927.Scope scope = RuntimeResourceAuditR927.begin(store, lease);
        try {
            RuntimeResourceAuditR927.stageBegin("HISTORY");
            RuntimeResourceAuditR927.stageBegin("NUMBER_MODEL");
            long[] history = scope.active.get("HISTORY");
            long[] number = scope.active.get("NUMBER_MODEL");
            long deadline = scope.totalDeadline;
            String budgetHash = scope.budgetHash;
            // Same-name observation while a child is open must not remove/pop either live stage.
            synchronized (scope) {
                long samples = scope.heapSamples;
                observe("HISTORY", 1001, false);
                require(scope.heapSamples == samples + 1, "Observation retains exactly one heap sample");
            }
            require(scope.active.size() == 2 && scope.active.get("HISTORY") == history
                            && scope.active.get("NUMBER_MODEL") == number,
                    "Active synchronous stage metadata is untouched, including same-name stages");
            require(scope.totalDeadline == deadline && scope.budgetHash.equals(budgetHash),
                    "Immutable deadline and frozen budget identity unchanged");
            require(scope.stages.toString().contains("HISTORY:1001/1000:OVER_BUDGET;"),
                    "Original stage SLO overrun remains evidence rather than a new cancellation");
            require(!scope.stages.toString().contains("CP12EX1|HISTORY|"),
                    "Observation cannot invent an exclusive or CPU interval for a live stage");
            observe("HISTORY", 1000, false);
            require(scope.stages.toString().contains("HISTORY:1000/1000:OK;"),
                    "Original exact SLO boundary is preserved");
            RuntimeResourceAuditR927.stageEnd("NUMBER_MODEL", 3, true);
            RuntimeResourceAuditR927.stageEnd("HISTORY", 4, true);
            require(scope.active.isEmpty(), "Original synchronous ends clear their own stage metadata");
        } finally {
            RuntimeResourceAuditR927.finish(store, scope, false, "NESTED_STACK_AND_SLO_FIXTURE");
        }
        String spans = persisted(db, lease);
        require(occurrences(spans, "CP12EX1|HISTORY|") == 1
                        && occurrences(spans, "CP12EX1|NUMBER_MODEL|") == 1,
                "Nested exclusive spans survive the same-name observation exactly once");
        require(!spans.contains("CP12EX_UNAVAILABLE|"), "No mismatch or unfinished span after live observation");
    }

    private static void deadlinesAndCommit(DbStore store, SQLiteDatabase db) throws Exception {
        AnalysisSingleFlightR36.Lease lease = lease(true);
        RuntimeResourceAuditR927.Scope scope = RuntimeResourceAuditR927.begin(store, lease);
        boolean expired = false;
        try {
            synchronized (scope) {
                long samples = scope.heapSamples;
                try {
                    observe("DURABLE_CLAIM", 1, false);
                } catch (RuntimeResourceAuditR927.BudgetExceeded failure) {
                    expired = failure.getMessage().contains("DEADLINE_EXCEEDED:DURABLE_CLAIM");
                }
                require(scope.heapSamples == samples + 1, "Expired observation still samples heap before cancellation");
            }
            require(expired, "Successful observation enforces the immutable request deadline");
            require(scope.active.isEmpty(), "Deadline observation creates no active synchronous stage");
        } finally {
            RuntimeResourceAuditR927.finish(store, scope, false, "EXPIRED_OBSERVATION_FIXTURE");
        }
        String spans = persisted(db, lease);
        require(spans.contains("DURABLE_CLAIM:1/0:OK;")
                        && spans.contains("CP12INCLUSIVE_ONLY|DURABLE_CLAIM|PRE_SCOPE_WALL_ONLY;"),
                "Inclusive observation and unavailable provenance persist before timeout");
        require(!spans.contains("CP12EX1|DURABLE_CLAIM|")
                        && !spans.contains("CP12EX_UNAVAILABLE|"),
                "Deadline failure does not fabricate or damage exclusive timing");

        lease = lease(true);
        scope = RuntimeResourceAuditR927.begin(store, lease);
        try {
            lease.token.markCanonicalCommitted();
            observe("DURABLE_CLAIM", 2, false);
            require(scope.stages.toString().contains("DURABLE_CLAIM:2/0:OK;"),
                    "Already committed scope preserves the original post-commit deadline bypass");
        } finally {
            RuntimeResourceAuditR927.finish(store, scope, true, "COMMITTED_OBSERVATION_FIXTURE");
        }
    }

    public static void main(String[] args) throws Exception {
        boolean before = args.length > 2 && "before".equals(args[2]);
        if (!before) {
            observe("OWNER_DISPATCH", 1, false);
            require(RuntimeResourceAuditR927.remainingMs() == Long.MAX_VALUE,
                    "No-scope observation is a harmless no-op");
        }
        String spans;
        try (SQLiteDatabase db = new SQLiteDatabase(args[0], args[1])) {
            DbStore store = HostProductR974.store(db);
            spans = initialObservations(store, db, before);
            if (!before) {
                liveStackAndSlo(store, db);
                deadlinesAndCommit(store, db);
                observe("REQUEST_TO_WORKER", 1, false);
                require(RuntimeResourceAuditR927.remainingMs() == Long.MAX_VALUE,
                        "Finished scope is not resurrected by an observation");
            }
        }
        System.out.println(spans);
        System.out.println("{\"status\":\"PASS\",\"checks\":" + checks
                + ",\"expected_stack_mismatches\":" + (before ? 3 : 0)
                + ",\"mode\":\"" + (before ? "BASELINE_REPRODUCTION" : "REPAIRED_PRODUCT")
                + "\",\"scope\":\"ACTUAL_GRADLE_PRODUCT_HOST_SQLITE_NOT_DEVICE_LATENCY\"}");
    }
}
