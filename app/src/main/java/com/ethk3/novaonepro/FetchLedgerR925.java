package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Durable request/source-event ledger separating transport receipt from business acceptance.
 *
 * <p>E02B provenance is append-per-request. Late corrections create a new request row and never
 * rewrite a historical request's pre-draw availability. first_available_at is the earliest durable
 * accepted receipt for the same immutable issue+payload version, while received_at remains the exact
 * receipt of this request.</p>
 */
public final class FetchLedgerR925 {
    public static final String SOURCE_EVENT_LABEL_VERSION = "R926_SOURCE_EVENT_V2";
    private static final AtomicLong sequence = new AtomicLong();
    private static final AtomicBoolean interruptedReconciled = new AtomicBoolean(false);
    private static final long INTERRUPTED_AFTER_MS = 120000L;
    private static final String CLOCK_EPOCH = V51ChecksumEngine.sha256(
            "R926_CLOCK_EPOCH|" + RuntimeObservabilityR925.PROCESS_ID);
    public static volatile String lastWriteError = "";

    private FetchLedgerR925() {}

    static String endpoint(String raw) {
        try {
            URI u = new URI(raw == null ? "" : raw);
            String scheme = u.getScheme() == null ? "" : u.getScheme();
            String host = u.getHost() == null ? "" : u.getHost();
            return scheme + "://" + host + (u.getPort() < 0 ? "" : ":" + u.getPort())
                    + (u.getPath() == null ? "" : u.getPath());
        } catch (Exception invalid) {
            return "INVALID_ENDPOINT";
        }
    }

    public static String begin(final DbStore store, final String method, String url) {
        if (store == null) return "";
        reconcileInterruptedOnce(store);
        final String id = RuntimeObservabilityR925.PROCESS_ID + "-" + sequence.incrementAndGet();
        final String path = endpoint(url);
        final String verb = safe(method).length() == 0 ? "GET" : safe(method);
        final long requestAt = TimeSourceR811.wallNowMs();
        try {
            boolean saved = store.coordinatorR36().runWrite(
                    "R925_FETCH_BEGIN",
                    new DatabaseCoordinator.WriteRunnable() {
                        @Override public void run(SQLiteDatabase db) {
                            db.execSQL(
                                    "INSERT INTO " + SchemaRevisionR925.TABLE_FETCH_LEDGER
                                            + "(request_id,method,endpoint,request_at,status,error_code,clock_epoch,label_version)"
                                            + " VALUES (?,?,?,?,?,?,?,?)",
                                    new Object[] {id, verb, path, requestAt, "REQUESTED", "",
                                            CLOCK_EPOCH, SOURCE_EVENT_LABEL_VERSION});
                        }
                    });
            if (!saved) {
                lastWriteError = "BEGIN:WRITE_REJECTED";
                return "";
            }
            return id;
        } catch (Throwable failure) {
            lastWriteError = "BEGIN:" + failure.getClass().getSimpleName();
            return "";
        }
    }

    public static void complete(
            final DbStore store, final String id, final long elapsed, final ApiClient.RawHttpResult raw) {
        if (store == null || safe(id).length() == 0) return;
        final int httpCode = raw == null ? 0 : raw.httpCode;
        final boolean transportOk = raw != null && raw.ok;
        final String body = raw == null || raw.body == null ? "" : raw.body;
        final String payloadHash = V51ChecksumEngine.sha256(body);
        final long receivedAt = TimeSourceR811.wallNowMs();
        String rawReason = raw == null ? "TRANSPORT_NULL_RESULT" : safe(raw.errorMessage);
        if (rawReason.length() == 0) {
            rawReason = httpCode == 503 ? "HTTP_503" : "TRANSPORT_EXCEPTION";
        }
        final String transportReason = rawReason;
        final String status = transportOk
                ? "HTTP_OK_BODY_RECEIVED"
                : Batch2ContractCoreR926.classifyFetch(false, httpCode, false, transportReason);
        final String error = transportOk
                ? ""
                : status + (transportReason.length() == 0 ? "" : ":" + trim(transportReason, 160));
        try {
            boolean saved = store.coordinatorR36().runWrite(
                    "R925_FETCH_COMPLETE",
                    new DatabaseCoordinator.WriteRunnable() {
                        @Override public void run(SQLiteDatabase db) {
                            db.execSQL(
                                    "UPDATE " + SchemaRevisionR925.TABLE_FETCH_LEDGER
                                            + " SET response_at=?,received_at=?,latency_ms=?,http_code=?,"
                                            + "response_hash=?,payload_hash=?,status=?,error_code=?,clock_epoch=?,label_version=?"
                                            + " WHERE request_id=?",
                                    new Object[] {receivedAt, receivedAt, Math.max(0L, elapsed), httpCode,
                                            payloadHash, payloadHash, status, error, CLOCK_EPOCH,
                                            SOURCE_EVENT_LABEL_VERSION, id});
                        }
                    });
            if (!saved) lastWriteError = "COMPLETE:WRITE_REJECTED";
        } catch (Throwable failure) {
            lastWriteError = "COMPLETE:" + failure.getClass().getSimpleName();
        }
    }

    public static void payload(
            final DbStore store,
            final String id,
            final String source,
            final String issue,
            final boolean accepted,
            final String reason) {
        if (store == null || safe(id).length() == 0) return;
        final String[] finalStatus = new String[] {"PAYLOAD_REJECTED_BUSINESS"};
        final long[] evidenceTimes = new long[] {0L, 0L, 0L}; // firstAvailable, received, draw
        final String[] payloadHash = new String[] {""};
        try {
            boolean saved = store.coordinatorR36().runWrite(
                    "R925_FETCH_PAYLOAD",
                    new DatabaseCoordinator.WriteRunnable() {
                        @Override public void run(SQLiteDatabase db) {
                            boolean owns = !db.inTransaction();
                            if (owns) db.beginTransaction();
                            try {
                            int httpCode = 200;
                            long receivedAt = TimeSourceR811.wallNowMs();
                            String bodyHash = "";
                            Cursor c = null;
                            try {
                                c = db.rawQuery(
                                        "SELECT COALESCE(http_code,200),COALESCE(received_at,0),"
                                                + "COALESCE(payload_hash,'') FROM "
                                                + SchemaRevisionR925.TABLE_FETCH_LEDGER
                                                + " WHERE request_id=? LIMIT 1",
                                        new String[] {id});
                                if (c.moveToFirst()) {
                                    httpCode = c.getInt(0);
                                    if (c.getLong(1) > 0L) receivedAt = c.getLong(1);
                                    bodyHash = safe(c.getString(2));
                                }
                            } finally {
                                if (c != null) c.close();
                            }

                            String status = Batch2ContractCoreR926.classifyFetch(
                                    true, httpCode, accepted, reason);
                            finalStatus[0] = status;
                            long drawAt = drawAt(db, issue);
                            long firstAvailableAt = accepted
                                    ? firstAcceptedAvailability(db, issue, bodyHash, receivedAt)
                                    : 0L;
                            evidenceTimes[0] = firstAvailableAt;
                            evidenceTimes[1] = receivedAt;
                            evidenceTimes[2] = drawAt;
                            payloadHash[0] = bodyHash;

                            db.execSQL(
                                    "UPDATE " + SchemaRevisionR925.TABLE_FETCH_LEDGER
                                            + " SET source_line=?,provider=?,latest_issue=?,first_available_at=?,"
                                            + "received_at=?,draw_at=?,clock_epoch=?,label_version=?,status=?,error_code=?"
                                            + " WHERE request_id=?",
                                    new Object[] {safe(source), safe(source), safe(issue), firstAvailableAt,
                                            receivedAt, drawAt, CLOCK_EPOCH, SOURCE_EVENT_LABEL_VERSION, status,
                                            accepted ? ("ACCEPTED_DUPLICATE".equals(status) ? status : "")
                                                    : safe(reason), id});
                            appendIssueEventWithin(db, id, source, issue, status, reason,
                                    bodyHash, firstAvailableAt, receivedAt, drawAt);
                            if (owns) db.setTransactionSuccessful();
                            } finally { if (owns) db.endTransaction(); }
                        }
                    });
            if (!saved) {
                lastWriteError = "PAYLOAD:WRITE_REJECTED";
                return;
            }

        } catch (Throwable failure) {
            lastWriteError = "PAYLOAD:" + failure.getClass().getSimpleName();
        }
    }

    public static int reconcileInterrupted(final DbStore store, final long olderThanMs) {
        if (store == null) return 0;
        final int[] changed = new int[] {0};
        final long cutoff = TimeSourceR811.wallNowMs() - Math.max(1000L, olderThanMs);
        try {
            boolean saved = store.coordinatorR36().runWrite(
                    "R925_FETCH_RECONCILE_INTERRUPTED",
                    new DatabaseCoordinator.WriteRunnable() {
                        @Override public void run(SQLiteDatabase db) {
                            android.content.ContentValues cv = new android.content.ContentValues();
                            cv.put("status", "PROCESS_INTERRUPTED");
                            cv.put("error_code", "PROCESS_DIED_BEFORE_RESPONSE_DURABLE");
                            changed[0] = db.update(
                                    SchemaRevisionR925.TABLE_FETCH_LEDGER, cv,
                                    "status='REQUESTED' AND request_at<?",
                                    new String[] {String.valueOf(cutoff)});
                        }
                    });
            if (!saved) lastWriteError = "RECONCILE:WRITE_REJECTED";
        } catch (Throwable failure) {
            lastWriteError = "RECONCILE:" + failure.getClass().getSimpleName();
        }
        return changed[0];
    }

    public static String report(DbStore store, int limit) {
        reconcileInterruptedOnce(store);
        int capped = Math.max(1, Math.min(500, limit));
        String summary = RuntimeObservabilityR925.query(
                store,
                "SELECT status,COUNT(*) AS n,MAX(request_at) AS last_at FROM "
                        + SchemaRevisionR925.TABLE_FETCH_LEDGER
                        + " GROUP BY status ORDER BY n DESC,status",
                false);
        return "逐HTTP/SourceEvent账本｜传输/收到/业务接受/首次可用/开奖时间/时钟纪元分层；REQUESTED超时遗留会标记PROCESS_INTERRUPTED\n"
                + "写入异常=" + (lastWriteError.length() == 0 ? "无已知异常" : lastWriteError) + "\n"
                + "状态汇总\n" + summary
                + "最近请求\n"
                + RuntimeObservabilityR925.query(
                        store,
                        "SELECT request_id,method,endpoint,provider,request_at,received_at,first_available_at,"
                                + "draw_at,latency_ms,http_code,status,error_code,source_line,latest_issue,"
                                + "payload_hash,clock_epoch,label_version FROM "
                                + SchemaRevisionR925.TABLE_FETCH_LEDGER
                                + " ORDER BY request_at DESC LIMIT " + capped,
                        false);
    }

    /**
     * Earliest availability is scoped to the immutable payload version, not merely to the issue.
     * Therefore a later correction with a different payload hash starts a new availability epoch
     * and can never inherit the earlier version's pre-draw timestamp. Legal duplicate receipts of
     * the exact same payload keep the original first availability.
     */
    private static long firstAcceptedAvailability(
            SQLiteDatabase db, String issue, String immutablePayloadHash, long currentReceivedAt) {
        String target = safe(issue);
        String payload = safe(immutablePayloadHash);
        if (!TargetIssueResolver.isValid(target) || !Batch2ContractCoreR926.isSha256(payload))
            return Math.max(0L, currentReceivedAt);
        Cursor c = null;
        try {
            c = db.rawQuery(
                    "SELECT MIN(first_available_at) FROM " + SchemaRevisionR925.TABLE_FETCH_LEDGER
                            + " WHERE latest_issue=? AND payload_hash=? AND first_available_at>0",
                    new String[] {target, payload});
            if (c.moveToFirst() && !c.isNull(0) && c.getLong(0) > 0L)
                return Math.min(c.getLong(0), Math.max(0L, currentReceivedAt));
        } finally {
            if (c != null) c.close();
        }
        return Math.max(0L, currentReceivedAt);
    }

    private static long drawAt(SQLiteDatabase db, String issue) {
        String target = safe(issue);
        if (!TargetIssueResolver.isValid(target)) return 0L;
        Cursor c = null;
        try {
            c = db.rawQuery(
                    "SELECT COALESCE(open_time,0) FROM " + DbHelper.TABLE_DRAWS
                            + " WHERE issue=? LIMIT 1",
                    new String[] {target});
            if (!c.moveToFirst()) return 0L;
            return ProviderFreshnessGateR3611.normalizeEpochMs(c.getLong(0));
        } finally {
            if (c != null) c.close();
        }
    }

    private static void reconcileInterruptedOnce(DbStore store) {
        if (store != null && interruptedReconciled.compareAndSet(false, true))
            reconcileInterrupted(store, INTERRUPTED_AFTER_MS);
    }

    private static void appendIssueEventWithin(
            SQLiteDatabase db, String requestId, String source, String issue, String status, String reason,
            String payloadHash, long firstAvailableAt, long receivedAt, long drawAt) {
        if (db == null || !TargetIssueResolver.isValid(issue)) return;
        String payload = "game=" + ReleaseManifestR926.GAME_CODE
                + "|issue=" + safe(issue)
                + "|provider=" + safe(source)
                + "|request_id=" + safe(requestId)
                + "|payload_hash=" + safe(payloadHash)
                + "|first_available_at=" + firstAvailableAt
                + "|received_at=" + receivedAt
                + "|draw_at=" + drawAt
                + "|clock_epoch=" + CLOCK_EPOCH
                + "|label_version=" + SOURCE_EVENT_LABEL_VERSION
                + "|status=" + safe(status)
                + "|reason=" + trim(reason, 500);
        RuntimeEventLedgerR926.appendWithinTransaction(
                db,
                RuntimeEventLedgerR926.Event.of(
                        issue, "", "", "FETCH", safe(status),
                        RuntimeEventLedgerR926.RANK_ACCEPTED,
                        payload, "FetchLedgerR925",
                        "FETCH|" + safe(issue) + '|' + safe(requestId) + '|' + safe(status)));
    }

    private static String trim(String s, int max) {
        String x = safe(s);
        return x.length() <= max ? x : x.substring(0, max);
    }

    private static String safe(String s) { return s == null ? "" : s.trim(); }

public static String reportSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, int limit) {
        /* Snapshot diagnosis never reconciles live fetch rows. */
        int capped = Math.max(1, Math.min(500, limit));
        String summary = RuntimeObservabilityR925.querySnapshotCP4(report, 
                store,
                "SELECT status,COUNT(*) AS n,MAX(request_at) AS last_at FROM "
                        + SchemaRevisionR925.TABLE_FETCH_LEDGER
                        + " GROUP BY status ORDER BY n DESC,status",
                false);
        return "逐HTTP/SourceEvent账本｜传输/收到/业务接受/首次可用/开奖时间/时钟纪元分层；REQUESTED超时遗留会标记PROCESS_INTERRUPTED\n"
                + "写入异常=" + (report.observationString("fetchWriteError")) + "\n"
                + "状态汇总\n" + summary
                + "最近请求\n"
                + RuntimeObservabilityR925.querySnapshotCP4(report, 
                        store,
                        "SELECT request_id,method,endpoint,provider,request_at,received_at,first_available_at,"
                                + "draw_at,latency_ms,http_code,status,error_code,source_line,latest_issue,"
                                + "payload_hash,clock_epoch,label_version FROM "
                                + SchemaRevisionR925.TABLE_FETCH_LEDGER
                                + " ORDER BY request_at DESC LIMIT " + capped,
                        false);
    }

public static String reportWriteErrorObservationCP12(){return lastWriteError;}
}
