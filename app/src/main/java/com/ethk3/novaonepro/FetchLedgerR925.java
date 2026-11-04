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
    public static final int R31_TIMING_REVISION=1,R31_TIMING_MAX_ROWS=16384;
    public static final String[] R31_TIMING_DDL={
        "CREATE TABLE IF NOT EXISTS r31_fetch_timing(request_id TEXT PRIMARY KEY,boot_id TEXT NOT NULL,payload_hash TEXT NOT NULL,request_wall_ms INTEGER,request_mono_ms INTEGER,headers_wall_ms INTEGER,headers_mono_ms INTEGER,first_byte_wall_ms INTEGER,first_byte_mono_ms INTEGER,body_complete_wall_ms INTEGER,body_complete_mono_ms INTEGER,parse_wall_ms INTEGER,parse_mono_ms INTEGER,validated_wall_ms INTEGER,validated_mono_ms INTEGER,finished_wall_ms INTEGER,finished_mono_ms INTEGER,issue TEXT NOT NULL,accepted INTEGER NOT NULL,body_truncated INTEGER NOT NULL,observation_contract TEXT NOT NULL)",
        "CREATE INDEX IF NOT EXISTS r31_fetch_timing_issue ON r31_fetch_timing(issue,request_mono_ms)"
    };
    public static void ensureR31Timing(SQLiteDatabase db){if(!db.inTransaction())throw new IllegalStateException("R31_TIMING_MIGRATION_OWNER_REQUIRED");for(String sql:R31_TIMING_DDL)db.execSQL(sql);}
    private static final java.util.LinkedHashMap<String,TransportTraceR31> TRANSPORT_R31=new java.util.LinkedHashMap<>();
    private static final AtomicLong traceEvictionsR31=new AtomicLong(),traceDeferredR31=new AtomicLong(),traceAdmissionDropsR31=new AtomicLong();
    private static final class TransportTraceR31 {
        final String id,boot,payload;final long[] clocks;final boolean truncated;String issue="";boolean accepted;
        TransportTraceR31(String id,ApiClient.RawHttpResult raw){this.id=id;boot=raw.bootId;payload=V51ChecksumEngine.sha256(raw.body==null?"":raw.body);truncated=raw.bodyTruncated;clocks=new long[]{raw.requestStartWallMs,raw.requestStartMonoMs,raw.headersWallMs,raw.headersMonoMs,raw.firstByteWallMs,raw.firstByteMonoMs,raw.bodyCompleteWallMs,raw.bodyCompleteMonoMs,0,0,0,0,raw.finishedWallMs,raw.finishedMonoMs};}
        TransportTraceR31(TransportTraceR31 row){id=row.id;boot=row.boot;payload=row.payload;clocks=row.clocks.clone();truncated=row.truncated;issue=row.issue;accepted=row.accepted;}
    }
    public static java.util.Map<String,Object> transportTimingSnapshotR31(){java.util.Map<String,Object> out=new java.util.LinkedHashMap<>();synchronized(TRANSPORT_R31){out.put("pendingTraceCount",TRANSPORT_R31.size());}out.put("memoryTraceEvictions",traceEvictionsR31.get());out.put("requestReceiptEvictions",requestReceiptDropsR31.get());out.put("deferredOrRejectedWrites",traceDeferredR31.get());out.put("capacityAdmissionDrops",traceAdmissionDropsR31.get());out.put("maximumPersistedRows",R31_TIMING_MAX_ROWS);out.put("missingTimingIsUnknown",true);return java.util.Collections.unmodifiableMap(out);}
    private static void observeTransportR31(DbStore store,String id,ApiClient.RawHttpResult raw){
        if(raw==null||safe(id).isEmpty())return;TransportTraceR31 trace=new TransportTraceR31(id,raw);
        synchronized(TRANSPORT_R31){TRANSPORT_R31.put(id,trace);while(TRANSPORT_R31.size()>128){TRANSPORT_R31.remove(TRANSPORT_R31.keySet().iterator().next());traceEvictionsR31.incrementAndGet();}}
        persistTraceR31(store,trace);
    }
    private static void observeStageR31(DbStore store,String id,String issue,boolean accepted,boolean parsed){
        TransportTraceR31 snapshot;long wall=TimeSourceR811.wallNowMs(),mono=TimeSourceR811.monoNowMs();
        synchronized(TRANSPORT_R31){TransportTraceR31 row=TRANSPORT_R31.get(id);if(row==null)return;int offset=parsed?8:10;if(row.clocks[offset]==0){row.clocks[offset]=wall;row.clocks[offset+1]=mono;}if(!safe(issue).isEmpty())row.issue=issue;row.accepted|=accepted;snapshot=new TransportTraceR31(row);}
        persistTraceR31(store,snapshot);
    }
    private static Long knownTimeR31(long value){return value>0?value:null;}
    private static void persistTraceR31(DbStore store,TransportTraceR31 trace){
        DatabaseCoordinator timingOwner=store==null?DatabaseCoordinator.peek():store.coordinatorR36();if(timingOwner==null)return;
        try{timingOwner.submitBackgroundWrite("R31_FETCH_TIMING",db->{
            try{
            boolean exists;try(Cursor row=db.rawQuery("SELECT 1 FROM r31_fetch_timing WHERE request_id=?",new String[]{trace.id})){exists=row.moveToFirst();}
            if(!exists){try(Cursor count=db.rawQuery("SELECT count(*) FROM r31_fetch_timing",null)){if(count.moveToFirst()&&count.getLong(0)>=R31_TIMING_MAX_ROWS){traceAdmissionDropsR31.incrementAndGet();db.execSQL("INSERT OR IGNORE INTO app_state(state_key,state_value,updated_at) VALUES('r31_fetch_timing_dropped','0',?)",new Object[]{System.currentTimeMillis()});db.execSQL("UPDATE app_state SET state_value=CAST(state_value AS INTEGER)+1,updated_at=? WHERE state_key='r31_fetch_timing_dropped'",new Object[]{System.currentTimeMillis()});return;}}
                Object[] args=new Object[21];args[0]=trace.id;args[1]=trace.boot;args[2]=trace.payload;for(int i=0;i<14;i++)args[i+3]=knownTimeR31(trace.clocks[i]);args[17]=trace.issue;args[18]=trace.accepted?1:0;args[19]=trace.truncated?1:0;args[20]="FIRST_SUCCESSFUL_INPUTSTREAM_READ; BODY_EOF_BEFORE_CLOSE; MONOTONIC_SAME_BOOT_ONLY; ZERO_IS_UNKNOWN";
                db.execSQL("INSERT INTO r31_fetch_timing VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",args);
            }else db.execSQL("UPDATE r31_fetch_timing SET parse_wall_ms=coalesce(parse_wall_ms,?),parse_mono_ms=coalesce(parse_mono_ms,?),validated_wall_ms=coalesce(validated_wall_ms,?),validated_mono_ms=coalesce(validated_mono_ms,?),issue=CASE WHEN issue='' THEN ? ELSE issue END,accepted=max(accepted,?) WHERE request_id=? AND payload_hash=?",new Object[]{knownTimeR31(trace.clocks[8]),knownTimeR31(trace.clocks[9]),knownTimeR31(trace.clocks[10]),knownTimeR31(trace.clocks[11]),trace.issue,trace.accepted?1:0,trace.id,trace.payload});
            }catch(RuntimeException unavailable){Error fatal=com.ethk3.novaonepro.strategyv2.FailureDispositionR31.processFatalCause(unavailable);if(fatal!=null)throw fatal;traceDeferredR31.incrementAndGet();}
        });}catch(RuntimeException unavailable){Error fatal=com.ethk3.novaonepro.strategyv2.FailureDispositionR31.processFatalCause(unavailable);if(fatal!=null)throw fatal;traceDeferredR31.incrementAndGet();}
    }

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

    static final int BEGIN_ATTEMPTS = 3; // Historical contract identifier; network admission no longer waits on these retries.
    private static final java.util.LinkedHashMap<String,PendingBeginR31> PENDING_BEGIN_R31=new java.util.LinkedHashMap<>();
    private static final java.util.LinkedHashMap<String,PendingResponseR31> PENDING_RESPONSE_R31=new java.util.LinkedHashMap<>();
    private static final AtomicLong requestReceiptDropsR31=new AtomicLong();
    private static final class PendingBeginR31 {
        final String id,verb,path,role;final long wall;
        PendingBeginR31(String id,String verb,String path,String role,long wall){this.id=id;this.verb=verb;this.path=path;this.role=role;this.wall=wall;}
    }
    private static final class PendingResponseR31 {
        final String id,hash,status,error,boot;final long wall,mono,elapsed;final int code;
        PendingResponseR31(String id,String hash,String status,String error,String boot,long wall,long mono,long elapsed,int code){this.id=id;this.hash=hash;this.status=status;this.error=error;this.boot=boot;this.wall=wall;this.mono=mono;this.elapsed=elapsed;this.code=code;}
    }
    public static String begin(final DbStore store,final String method,String url){
        if(store==null)return "";
        final String id=RuntimeObservabilityR925.PROCESS_ID+"-"+sequence.incrementAndGet();
        final PendingBeginR31 start=new PendingBeginR31(id,safe(method).isEmpty()?"GET":safe(method),endpoint(url),com.ethk3.novaonepro.strategyv2.IssueTimelineWriterR30.requestRole().name(),TimeSourceR811.wallNowMs());
        synchronized(PENDING_BEGIN_R31){PENDING_BEGIN_R31.put(id,start);while(PENDING_BEGIN_R31.size()>128){PENDING_BEGIN_R31.remove(PENDING_BEGIN_R31.keySet().iterator().next());requestReceiptDropsR31.incrementAndGet();}}
        // The existing bounded writer may persist this attempt while HTTP proceeds. Canonical
        // draw binding still requires the exact durable request/payload before any draw commit.
        try{store.coordinatorR36().submitBackgroundWrite("R31_FETCH_BEGIN_ASYNC",db->insertBeginR31(db,start));}
        catch(RuntimeException unavailable){Error fatal=com.ethk3.novaonepro.strategyv2.FailureDispositionR31.processFatalCause(unavailable);if(fatal!=null)throw fatal;traceDeferredR31.incrementAndGet();}
        return id;
    }
    private static void insertBeginR31(SQLiteDatabase db,PendingBeginR31 start){
        db.execSQL("INSERT OR IGNORE INTO "+SchemaRevisionR925.TABLE_FETCH_LEDGER+"(request_id,method,endpoint,request_at,status,error_code,clock_epoch,label_version,request_role) VALUES(?,?,?,?,?,?,?,?,?)",new Object[]{start.id,start.verb,start.path,start.wall,"REQUESTED","",CLOCK_EPOCH,SOURCE_EVENT_LABEL_VERSION,start.role});
    }
    private static void ensureReceiptWithinR31(SQLiteDatabase db,String id){
        PendingBeginR31 start;PendingResponseR31 response;
        synchronized(PENDING_BEGIN_R31){start=PENDING_BEGIN_R31.get(id);}synchronized(PENDING_RESPONSE_R31){response=PENDING_RESPONSE_R31.get(id);}
        if(start!=null)insertBeginR31(db,start);
        if(response!=null)db.execSQL("UPDATE "+SchemaRevisionR925.TABLE_FETCH_LEDGER+" SET response_at=coalesce(response_at,?),received_at=CASE WHEN received_at>0 THEN received_at ELSE ? END,latency_ms=?,http_code=?,response_hash=?,payload_hash=?,status=?,error_code=?,clock_epoch=?,label_version=?,received_mono_ms=coalesce(received_mono_ms,?),received_boot_id=coalesce(received_boot_id,?) WHERE request_id=?",new Object[]{response.wall,response.wall,response.elapsed,response.code,response.hash,response.hash,response.status,response.error,CLOCK_EPOCH,SOURCE_EVENT_LABEL_VERSION,response.mono,response.boot,id});
        try(Cursor existing=db.rawQuery("SELECT payload_hash FROM "+SchemaRevisionR925.TABLE_FETCH_LEDGER+" WHERE request_id=?",new String[]{id})){
            if(!existing.moveToFirst()||!safe(existing.getString(0)).matches("[0-9a-f]{64}")||response!=null&&!response.hash.equals(existing.getString(0)))throw new IllegalStateException("R31_DEPENDENCY_HTTP_RECEIPT_NOT_DURABLE");
        }
    }
    private static void receiptDurableR31(String id){synchronized(PENDING_BEGIN_R31){PENDING_BEGIN_R31.remove(id);}synchronized(PENDING_RESPONSE_R31){PENDING_RESPONSE_R31.remove(id);}}

    public static void complete(
            final DbStore store, final String id, final long elapsed, final ApiClient.RawHttpResult raw) {
        if (store == null || safe(id).length() == 0) return;
        final int httpCode = raw == null ? 0 : raw.httpCode;
        final boolean transportOk = raw != null && raw.ok;
        final String body = raw == null || raw.body == null ? "" : raw.body;
        final long receivedAt = raw!=null&&raw.bodyCompleteWallMs>0?raw.bodyCompleteWallMs:TimeSourceR811.wallNowMs();
        final long receivedMono=raw!=null&&raw.bodyCompleteMonoMs>0?raw.bodyCompleteMonoMs:android.os.SystemClock.elapsedRealtime();final String receivedBoot=com.ethk3.novaonepro.strategyv2.IssueTimelineWriterR30.bootId();
        observeTransportR31(store,id,raw);
        final String payloadHash = V51ChecksumEngine.sha256(body);
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
        final PendingResponseR31 response=new PendingResponseR31(id,payloadHash,status,error,receivedBoot,receivedAt,receivedMono,Math.max(0,elapsed),httpCode);
        synchronized(PENDING_RESPONSE_R31){PENDING_RESPONSE_R31.put(id,response);while(PENDING_RESPONSE_R31.size()>128){PENDING_RESPONSE_R31.remove(PENDING_RESPONSE_R31.keySet().iterator().next());requestReceiptDropsR31.incrementAndGet();}}
        try{
            boolean saved=store.coordinatorR36().runAtomicWrite("R925_FETCH_COMPLETE",db->ensureReceiptWithinR31(db,id));
            if(saved){receiptDurableR31(id);lastWriteError="";}
            else lastWriteError="COMPLETE:WRITE_REJECTED:"+rejectedR27(store,"COMPLETE");
        }catch(Throwable failure){Error fatal=com.ethk3.novaonepro.strategyv2.FailureDispositionR31.processFatalCause(failure);if(fatal!=null)throw fatal;lastWriteError="COMPLETE:"+failure.getClass().getSimpleName();}
    }

    public static void payload(
            final DbStore store,
            final String id,
            final String source,
            final String issue,
            final boolean accepted,
            final String reason) {
        payload(store,id,source,issue,accepted,reason,null);
    }
    public static void payload(final DbStore store,final String id,final String source,final String issue,final boolean accepted,final String reason,final String observedDrawHash) {
        if (store == null || safe(id).length() == 0) return;
        observeStageR31(store,id,issue,accepted,false);
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
                            ensureReceiptWithinR31(db,id);
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

                            String classified = Batch2ContractCoreR926.classifyFetch(
                                    true, httpCode, accepted, reason);
                            // PayloadGate acceptance precedes DbStore's transaction; never
                            // advertise that transport parsing as canonical business commit.
                            String status=accepted?"HTTP_PAYLOAD_ACCEPTED":classified;
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
                            // R30-B02-01 FIRST_OBSERVED: the issue is only known once this response is parsed, so the
                            // mark sits in this fetch-ledger callback (R925_FETCH_COMPLETE has the raw body only). Wall time
                            // is the first accepted availability; first writer wins on the timeline row.
                            if(accepted&&TargetIssueResolver.isValid(issue)&&observedDrawHash!=null)timelineDrawR30(db,id,issue,observedDrawHash,com.ethk3.novaonepro.strategyv2.IssueTimelineR30.Stage.FIRST_OBSERVED);
                            if (owns) db.setTransactionSuccessful();
                            } finally { if (owns) db.endTransaction(); }
                        }
                    });
            if(saved)receiptDurableR31(id);
            if (!saved) {
                lastWriteError = "PAYLOAD:WRITE_REJECTED:" + rejectedR27(store, "PAYLOAD");
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
            if (!saved) lastWriteError = "RECONCILE:WRITE_REJECTED:" + rejectedR27(store, "RECONCILE");
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

    private static final java.util.Map<Models.ApiResult,RawBindingR82> RAW_R82=
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<Models.ApiResult,RawBindingR82>());
    private static final java.util.Map<Models.Draw,DrawBindingR82> DRAW_R82=
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<Models.Draw,DrawBindingR82>());
    private static final class RawBindingR82 {
        final String request,payload;RawBindingR82(String r,String p){request=r;payload=p;}
    }
    private static final class DrawBindingR82 {
        final String request,payload,rawHash,fingerprint,source,issue,drawHash;final long drawAt,observedAt;
        final boolean restored;
        DrawBindingR82(String request,String payload,Models.Draw d,String rawHash,boolean restored){
            this.request=safe(request);this.payload=safe(payload);this.rawHash=rawHash;
            source=safe(d.sourceLine);issue=safe(d.issue);drawAt=d.openTime;observedAt=d.fetchTime;
            drawHash=DrawHashR36.sha256V2(d);fingerprint=drawFingerprintR82(d,rawHash);this.restored=restored;
        }
    }
    /** Only the exact successful raw transport object supplies its request identity. */
    static void bindRawResultR82(Models.ApiResult raw,String request) {
        if(raw!=null&&request!=null)RAW_R82.put(raw,new RawBindingR82(request,V51ChecksumEngine.sha256(safe(raw.rawJson))));
    }
    /** No SQL and no lookup by issue: bind the actual objects returned by ApiParser. */
    static void bindParsedResultR82(Models.ApiResult raw,Models.ApiResult parsed) {
        RawBindingR82 b=RAW_R82.get(raw);if(b==null||parsed==null)return;
        observeStageR31(null,b.request,parsed.latestDraw==null?"":parsed.latestDraw.issue,false,true);
        if(!b.payload.equals(V51ChecksumEngine.sha256(safe(raw.rawJson))))throw new IllegalStateException("HTTP_RAW_BINDING_MUTATED");
        java.util.Set<Models.Draw> seen=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Models.Draw,Boolean>());
        if(parsed.latestDraw!=null)seen.add(parsed.latestDraw);
        if(parsed.history!=null)seen.addAll(parsed.history);if(parsed.draws!=null)seen.addAll(parsed.draws);
        for(Models.Draw draw:seen)if(draw!=null&&draw.isValid())DRAW_R82.put(draw,new DrawBindingR82(b.request,b.payload,draw,V51ChecksumEngine.sha256(safe(draw.rawJson)),false));
    }
    public static boolean hasDrawBindingR82(Models.Draw draw){return draw!=null&&DRAW_R82.containsKey(draw);}
    public static String canonicalBindingSummaryR82(Models.Draw draw) {
        DrawBindingR82 b=DRAW_R82.get(draw);
        return b==null?"http_request_scope=LEGACY_UNKNOWN|request_id=LEGACY_UNKNOWN|http_payload_sha256=LEGACY_UNKNOWN":
                "http_request_scope=EXACT_REQUEST_BOUND_R82|request_id="+b.request+"|http_payload_sha256="+b.payload+"|draw_fingerprint="+b.fingerprint;
    }
    private static String drawFingerprintR82(Models.Draw d,String rawHash) {
        return V51ChecksumEngine.sha256("REQUEST_DRAW_R82|"+safe(d.issue)+"|"+DrawHashR36.sha256V2(d)+"|"+safe(d.sourceLine)+"|"+d.openTime+"|"+d.fetchTime+"|"+rawHash);
    }
    private static DrawBindingR82 checkedBindingR82(SQLiteDatabase db,Models.Draw draw) {
        if(db==null||!db.inTransaction())throw new IllegalStateException("HTTP_CANONICAL_BINDING_TRANSACTION_REQUIRED");
        DrawBindingR82 b=DRAW_R82.get(draw);if(b==null)return null;
        String rawHash=V51ChecksumEngine.sha256(safe(draw.rawJson));
        if(!b.fingerprint.equals(drawFingerprintR82(draw,b.rawHash))||(!b.rawHash.equals(rawHash)&&!(b.restored&&safe(draw.rawJson).isEmpty())))
            throw new IllegalStateException("HTTP_DRAW_BINDING_MUTATED");
        if(b.request.isEmpty()||!b.payload.matches("[0-9a-f]{64}"))throw new IllegalStateException("HTTP_REQUEST_RECEIPT_NOT_DURABLE");
        try(Cursor c=db.rawQuery("SELECT payload_hash,source_line,status,http_code,received_at FROM "+SchemaRevisionR925.TABLE_FETCH_LEDGER+" WHERE request_id=?",new String[]{b.request})){
            if(!c.moveToFirst()||!b.payload.equals(c.getString(0)))throw new IllegalStateException("HTTP_REQUEST_PAYLOAD_IDENTITY_MISMATCH");
            String source=safe(c.getString(1)),status=safe(c.getString(2));int http=c.getInt(3);
            if((!source.isEmpty()&&!source.equals(b.source))||http<200||http>=300||c.getLong(4)<=0
                    ||!(status.equals("HTTP_PAYLOAD_ACCEPTED")||status.equals("HTTP_OK_BODY_RECEIVED")||status.equals("BUSINESS_ACCEPTED")||status.equals("ACCEPTED_DUPLICATE")))
                throw new IllegalStateException("HTTP_REQUEST_NOT_ACCEPTED_SOURCE_RECEIPT");
        }
        return b;
    }
    /** Telemetry reads the exact original request. It never derives a monotonic instant from wall time. */
    private static void timelineDrawR30(SQLiteDatabase db,String request,String issue,String drawHash,com.ethk3.novaonepro.strategyv2.IssueTimelineR30.Stage stage){
        try(Cursor c=db.rawQuery("SELECT request_role,latest_issue,received_at,received_mono_ms,received_boot_id FROM "+SchemaRevisionR925.TABLE_FETCH_LEDGER+" WHERE request_id=?",new String[]{request})){
            if(!c.moveToFirst())return;
            com.ethk3.novaonepro.strategyv2.IssueTimelineR30.EventRole role;
            try{role=com.ethk3.novaonepro.strategyv2.IssueTimelineR30.EventRole.valueOf(c.getString(0));}catch(RuntimeException unknown){role=com.ethk3.novaonepro.strategyv2.IssueTimelineR30.EventRole.UNKNOWN;}
            if(!issue.equals(c.getString(1)))role=com.ethk3.novaonepro.strategyv2.IssueTimelineR30.EventRole.BACKFILL;
            if(!c.isNull(3)&&!c.isNull(4))com.ethk3.novaonepro.strategyv2.IssueTimelineWriterR30.markEventAt(db,issue,role,com.ethk3.novaonepro.strategyv2.IssueTimelineR30.EventKind.DRAW,request,null,null,drawHash,com.ethk3.novaonepro.strategyv2.IssueTimelineR30.Stage.FIRST_OBSERVED,c.getLong(2),c.getLong(3),c.getString(4));
            if(stage!=com.ethk3.novaonepro.strategyv2.IssueTimelineR30.Stage.FIRST_OBSERVED)com.ethk3.novaonepro.strategyv2.IssueTimelineWriterR30.markEventAt(db,issue,role,com.ethk3.novaonepro.strategyv2.IssueTimelineR30.EventKind.DRAW,request,null,null,drawHash,stage,System.currentTimeMillis(),android.os.SystemClock.elapsedRealtime(),com.ethk3.novaonepro.strategyv2.IssueTimelineWriterR30.bootId());
        }catch(RuntimeException unavailable){android.util.Log.w("STARCORE_R30","original request clock unavailable: "+unavailable.getClass().getSimpleName());}
    }
    private static String bindingJsonR82(DrawBindingR82 b,long row)throws org.json.JSONException {
        org.json.JSONObject o=new org.json.JSONObject();o.put("version","HTTP_CANONICAL_BINDING_R82_1");o.put("request_id",b.request);
        o.put("http_payload_sha256",b.payload);o.put("draw_raw_sha256",b.rawHash);o.put("draw_hash",b.drawHash);
        o.put("draw_fingerprint",b.fingerprint);o.put("issue",b.issue);o.put("source",b.source);o.put("draw_at",b.drawAt);o.put("draw_observed_at",b.observedAt);
        if(row>0)o.put("draw_rowid",row);return o.toString();
    }
    private static void exactAppendR82(SQLiteDatabase db,RuntimeEventLedgerR926.Event e) {
        RuntimeEventLedgerR926.appendWithinTransaction(db,e);
        try(Cursor c=db.rawQuery("SELECT payload_hash FROM "+RuntimeEventLedgerR926.TABLE_EVENT+" WHERE dedup_key=?",new String[]{e.dedupKey})){
            if(!c.moveToFirst()||!e.payloadHash.equals(c.getString(0)))throw new IllegalStateException("HTTP_BINDING_EVENT_IDEMPOTENCY_CONFLICT");
        }
    }
    /** Persisted even when a current draw is deferred by retention; rolled back with its owner TX. */
    public static void observeDrawWithinR82(SQLiteDatabase db,Models.Draw draw) {
        // R30-B02-01 VALIDATED: called first inside DbStore's R914_DRAW_SAVE_ATOMIC (and history batch) transaction,
        // after the draw passed normalization/validation and before its first INSERT. DbStore stays byte-identical
        // (main-chain red line, R27QualityTest.h15MainlineRedLines).
        DrawBindingR82 b=checkedBindingR82(db,draw);if(b==null)return;
        timelineDrawR30(db,b.request,b.issue,b.drawHash,com.ethk3.novaonepro.strategyv2.IssueTimelineR30.Stage.VALIDATED);
        try{exactAppendR82(db,RuntimeEventLedgerR926.Event.of(b.issue,b.fingerprint,b.request,"FETCH_BINDING_R82","PARSED_DRAW",RuntimeEventLedgerR926.RANK_REQUESTED,
                bindingJsonR82(b,0),"ApiClient.ApiParser","HTTP_DRAW_BOUND_R82|"+b.request+"|"+b.fingerprint));}
        catch(org.json.JSONException invalid){throw new IllegalStateException("HTTP_BINDING_JSON",invalid);}
    }
    /** Same transaction as draws/settlement enqueue and canonical acceptance event. */
    public static void canonicalDrawWithinR82(SQLiteDatabase db,Models.Draw draw) {
        // R30-B02-01 DRAW_COMMITTED: same transaction, called after the draw row is written (DbStore skips this call when
        // the draw is blocked/deferred); the commit follows the settlement enqueue and acceptance event in that body.
        DrawBindingR82 b=checkedBindingR82(db,draw);if(b==null)return;
        timelineDrawR30(db,b.request,b.issue,b.drawHash,com.ethk3.novaonepro.strategyv2.IssueTimelineR30.Stage.DRAW_COMMITTED);
        long row=0;try(Cursor c=db.rawQuery("SELECT rowid,draw_hash_v2,draw_hash_version,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10 FROM draws WHERE issue=?",new String[]{b.issue})){
            if(!c.moveToFirst()||!DrawHashR36.matchesPersisted(c.getString(1),c.getInt(2),draw))throw new IllegalStateException("HTTP_CANONICAL_DRAW_NOT_DURABLE");row=c.getLong(0);
            for(int i=0;i<10;i++)if(c.getInt(i+3)!=draw.numbers[i])throw new IllegalStateException("HTTP_CANONICAL_NUMBERS_CONFLICT");
        }
        try{exactAppendR82(db,RuntimeEventLedgerR926.Event.of(b.issue,b.fingerprint,b.request,"FETCH_CANONICAL_BINDING_R82","DRAW_COMMIT_LINKED",RuntimeEventLedgerR926.RANK_ACCEPTED,
                bindingJsonR82(b,row),"DbStore.saveDraw/HistoryBatch","CANONICAL_REQUEST_LINK_R82|"+b.request+"|"+b.fingerprint));}
        catch(org.json.JSONException invalid){throw new IllegalStateException("HTTP_CANONICAL_JSON",invalid);}
    }
    /** Restore only the exact immutable source tuple, never just a matching issue. */
    static void restoreDeferredDrawR82(SQLiteDatabase db,Models.Draw draw,String rawHash,String originalDrawHash) {
        if(!originalDrawHash.equals(DrawHashR36.sha256V2(draw)))throw new IllegalStateException("HTTP_DEFERRED_DRAW_HASH_MISMATCH");
        String fingerprint=drawFingerprintR82(draw,rawHash);
        try(Cursor c=db.rawQuery("SELECT attempt_id,payload_text FROM "+RuntimeEventLedgerR926.TABLE_EVENT+" WHERE target_issue=? AND epoch_id=? AND event_type='FETCH_BINDING_R82' AND stage='PARSED_DRAW' ORDER BY seq LIMIT 1",new String[]{draw.issue,fingerprint})){
            if(!c.moveToFirst())return; // Old rows stay explicitly unbound; no synthetic request is invented.
            org.json.JSONObject o=new org.json.JSONObject(c.getString(1));String request=c.getString(0);
            if(!"HTTP_CANONICAL_BINDING_R82_1".equals(o.getString("version"))||!safe(draw.issue).equals(o.getString("issue"))||draw.openTime!=o.getLong("draw_at")||draw.fetchTime!=o.getLong("draw_observed_at")||!request.equals(o.getString("request_id"))||!fingerprint.equals(o.getString("draw_fingerprint"))||!rawHash.equals(o.getString("draw_raw_sha256"))||!originalDrawHash.equals(o.getString("draw_hash"))||!safe(draw.sourceLine).equals(o.getString("source")))throw new IllegalStateException("HTTP_DEFERRED_BINDING_IDENTITY");
            DRAW_R82.put(draw,new DrawBindingR82(request,o.getString("http_payload_sha256"),draw,rawHash,true));
        }catch(org.json.JSONException invalid){throw new IllegalStateException("HTTP_DEFERRED_BINDING_JSON",invalid);}
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

    // R27-048: WRITE_REJECTED is split by cause (busy deadline / duplicate-or-conflict / maintenance / other) and
    // counted; the R27 health task persists the counts into r27_fetch_reject for the report FETCH chapter.
    private static final java.util.concurrent.ConcurrentHashMap<String, long[]> REJECTS_R27 = new java.util.concurrent.ConcurrentHashMap<>();
    private static String rejectedR27(DbStore store, String phase) {
        String error = "";
        try { error = store.coordinatorR36().snapshotB03().lastError; } catch (RuntimeException ignored) { }
        String cause = error.contains("CP3_DB_BUSY_DEADLINE") || error.contains("locked") || error.contains("BUSY") ? "BUSY_DEADLINE"
                : error.contains("UNIQUE") || error.contains("constraint") || error.contains("CONFLICT") ? "DUPLICATE_OR_CONFLICT"
                : error.contains("MAINTENANCE") ? "MAINTENANCE" : error.isEmpty() ? "UNKNOWN_NO_COORDINATOR_ERROR" : "OTHER";
        String key = phase + ":" + cause;
        long[] c = REJECTS_R27.computeIfAbsent(key, k -> new long[2]);
        synchronized (c) { c[0]++; c[1] = System.currentTimeMillis(); }
        return cause;
    }
    /** Snapshot of the counts since process start: reason → {count, lastAt}. */
    public static java.util.Map<String, long[]> rejectCountsR27() {
        java.util.Map<String, long[]> out = new java.util.TreeMap<>();
        for (java.util.Map.Entry<String, long[]> e : REJECTS_R27.entrySet()) synchronized (e.getValue()) { out.put(e.getKey(), e.getValue().clone()); }
        return out;
    }
}
