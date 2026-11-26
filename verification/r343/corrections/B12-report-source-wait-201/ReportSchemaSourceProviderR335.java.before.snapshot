package com.ethk3.novaonepro;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;

import java.io.IOException;
import java.io.InterruptedIOException;

/** Coordinates a cold report with the registered business-process migration owner before native backup. */
public final class ReportSchemaSourceProviderR335 extends ContentProvider {
    private static final String STEP = "advance_registered_report_sources";
    private static final String AVAILABLE = "REGISTERED_REPORT_SOURCES_AVAILABLE";
    private static final String PENDING = "RESEARCH_SCHEMA_PREPARING";
    private static final long STALLED_MS = 120000L;
    private static final String JOURNAL = SchemaDomainCoordinatorR926.TABLE_JOURNAL;

    interface Checkpoint {
        void check() throws IOException;
    }

    @Override public boolean onCreate() { return true; }

    /** Runs on a Binder thread in the main process. The snapshot process never opens a writable handle. */
    @Override public Bundle call(String method, String argument, Bundle extras) {
        if (Binder.getCallingUid() != Process.myUid()) throw new SecurityException("REPORT_SCHEMA_CALLER");
        if (!STEP.equals(method) || !ReleaseManifestR926.MANIFEST_SHA256.equals(argument))
            throw new IllegalArgumentException("REPORT_SCHEMA_REQUEST");
        if (Looper.myLooper() == Looper.getMainLooper()) throw new IllegalStateException("REPORT_SCHEMA_MAIN_THREAD");
        Context app = getContext();
        DatabaseCoordinator owner = DatabaseCoordinator.get(app);
        // A restored request can be the first use of the main process after an upgrade.
        if (!owner.isCriticalSchemaReady()) new DbStore(app).close();
        return advance(owner);
    }

    static Bundle advance(DatabaseCoordinator owner) {
        SQLiteDatabase db = owner.readable();
        if (sourcesCommitted(db)) return result(AVAILABLE, 0L);
        ResearchWorkBudgetR927.Scope budget = ResearchWorkBudgetR927.enter(250L);
        try {
            ResearchSchemaCoordinatorR926.StepResult step = ResearchSchemaCoordinatorR926.ensureOneStepResult(owner);
            if (sourcesCommitted(db)) return result(AVAILABLE, 0L);
            if (step.ready || step.cause != null && !"WRITER_BUSY".equals(step.status))
                return result("REGISTERED_SOURCE_OWNER_" + step.status, 0L);
        } catch (ResearchWorkBudgetR927.Yield | RuntimeResourceAuditR927.BudgetExceeded retry) {
            // An active fetch or settlement takes priority. The child journal survives a yield.
        } finally {
            ResearchWorkBudgetR927.leave(budget);
        }
        return result(PENDING, committedSteps(db));
    }

    private static boolean journalExists(SQLiteDatabase db) {
        try (Cursor c = db.rawQuery("SELECT 1 FROM main.sqlite_master WHERE type='table' AND name=?",
                new String[]{JOURNAL})) {
            return c.moveToFirst();
        }
    }

    static boolean sourcesCommitted(SQLiteDatabase db) {
        if (!journalExists(db)) return false;
        SchemaDomainCoordinatorR926.Contract contract = SchemaDomainCoordinatorR926.researchContract();
        try (Cursor row = db.rawQuery("SELECT 1 FROM " + JOURNAL +
                " WHERE domain=? AND revision=? AND step_id='R952_EXTENSION' AND status='COMPLETED'",
                new String[]{contract.domain, String.valueOf(contract.revision)})) {
            if (!row.moveToFirst()) return false;
        }
        // Re-read the actual inventory. An old journal alone cannot prove a restored database has its inputs.
        try (Cursor actual = db.rawQuery("SELECT COUNT(*) FROM main.sqlite_master WHERE type='table' AND name IN " +
                "('r952_research_outbox','r952_candidate','r952_candidate_funnel','r952_learning_cursor'," +
                "'r952_artifact','research_historical_genome_archive_r967','research_historical_genome_use_r967'," +
                "'research_exact_probability_f05')", null)) {
            return actual.moveToFirst() && actual.getInt(0) == 8;
        }
    }

    private static long committedSteps(SQLiteDatabase db) {
        if (!journalExists(db)) return 0L;
        SchemaDomainCoordinatorR926.Contract contract = SchemaDomainCoordinatorR926.researchContract();
        try (Cursor row = db.rawQuery("SELECT COUNT(*) FROM " + JOURNAL +
                " WHERE domain=? AND revision=? AND status='COMPLETED'",
                new String[]{contract.domain, String.valueOf(contract.revision)})) {
            return row.moveToFirst() ? row.getLong(0) : 0L;
        }
    }

    private static Bundle result(String status, long completed) {
        Bundle answer = new Bundle();
        answer.putString("status", status);
        answer.putLong("completed", completed);
        return answer;
    }

    /** Unavailable sources are still exported and diagnosed by the original checks, with the cause recorded. */
    static String prepareBeforeSnapshot(Context context, Checkpoint cancellation) throws IOException {
        Uri uri = new Uri.Builder().scheme("content").authority(context.getPackageName() + ".report.schema").build();
        long started = SystemClock.elapsedRealtime(), progressed = started, lastSteps = -1L;
        while (true) {
            cancellation.check();
            Bundle reply;
            try {
                reply = context.getContentResolver().call(uri, STEP, ReleaseManifestR926.MANIFEST_SHA256, null);
            } catch (RuntimeException failure) {
                android.util.Log.w("ReportSchema", "Business-process schema owner unavailable", failure);
                return "REGISTERED_SOURCE_OWNER_UNAVAILABLE_" + failure.getClass().getSimpleName();
            }
            if (reply == null || reply.getString("status") == null) return "REGISTERED_SOURCE_OWNER_NO_RESPONSE";
            String status = reply.getString("status");
            if (!PENDING.equals(status)) return status;
            long now = SystemClock.elapsedRealtime(), steps = reply.getLong("completed", 0L);
            if (steps != lastSteps) { progressed = now; lastSteps = steps; }
            if (now - progressed >= STALLED_MS) return "REGISTERED_SOURCE_OWNER_STALLED";
            if (now - started >= ReportBudgetSU2.FULL_REPORT_STAGE_MS / 2) return "REGISTERED_SOURCE_PREPARATION_WINDOW_ELAPSED";
            try { Thread.sleep(100L); }
            catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                InterruptedIOException failure = new InterruptedIOException("REPORT_WORKER_INTERRUPTED");
                failure.initCause(interrupted);
                throw failure;
            }
        }
    }

    @Override public Cursor query(Uri uri,String[] columns,String selection,String[] args,String order) { throw new UnsupportedOperationException(); }
    @Override public String getType(Uri uri) { return null; }
    @Override public Uri insert(Uri uri,ContentValues content) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri,ContentValues content,String where,String[] args) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri,String where,String[] args) { throw new UnsupportedOperationException(); }
}
