package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;
import com.ethk3.novaonepro.DatabaseCoordinator;
import com.ethk3.novaonepro.DbStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * R30-B01-02 写锁账本落库。关闭的完整窗口保留到所有有限片确证提交，重试不重新取样。
 * 调用点（唯一）：ResearchPoolR27.recordThreadCpu(...) 末尾 writes.flush() 之后一行：HoldLedgerFlushR30.maybeFlush(store);
 * 失败只记日志并保留内存窗口到下一次，不影响研究线程。
 */
public final class HoldLedgerFlushR30 {
    public static final long PERIOD_MS = 60_000L;
    public static final long RETAIN_MS = 7L * 24 * 3600_000L;
    public static final String DDL = "CREATE TABLE IF NOT EXISTS r30_writer_hold(window_start INTEGER NOT NULL,window_end INTEGER NOT NULL,"
            + "operation TEXT NOT NULL,critical INTEGER NOT NULL,count INTEGER NOT NULL,hold_p50 INTEGER,hold_p95 INTEGER,hold_p99 INTEGER,"
            + "hold_max INTEGER NOT NULL,wait_p95 INTEGER,wait_max INTEGER NOT NULL,over_budget INTEGER NOT NULL,over_hard INTEGER NOT NULL,"
            + "failures INTEGER NOT NULL,histogram_json TEXT,process_session TEXT,PRIMARY KEY(window_start,operation,critical))";
    public static final String INSERT = "INSERT OR REPLACE INTO r30_writer_hold(window_start,window_end,operation,critical,count,hold_p50,hold_p95,hold_p99,"
            + "hold_max,wait_p95,wait_max,over_budget,over_hard,failures,histogram_json,process_session) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    public static final String PRUNE = "DELETE FROM r30_writer_hold WHERE window_end<?";
    static final int MAX_OPS_R31 = 64;
    // Includes receipt lookup/insert, two owner fences and the three readiness queries.
    // The remaining allowance covers the existing coordinator's non-R27 progress adapter.
    static final int PROTOCOL_OPS_R31 = 12;
    private static final String INSERT_ONCE_R31 = INSERT.replace("INSERT OR REPLACE", "INSERT OR IGNORE");
    private static final String HOLD_COLUMNS_R31 = "window_start,window_end,operation,critical,count,hold_p50,hold_p95,hold_p99,hold_max,wait_p95,wait_max,over_budget,over_hard,failures,histogram_json,process_session";
    private static volatile long lastRotationElapsedR31 = -1;
    private static final java.util.concurrent.atomic.AtomicBoolean FLUSHING=new java.util.concurrent.atomic.AtomicBoolean();
    private static volatile Pending pending;
    private static final class Pending {
        final long start,end;final List<WriterHoldLedgerR30.Stat> rows;
        int cursor; ChunkR31 chunk;
        Pending(long start,long end,List<WriterHoldLedgerR30.Stat> rows){this.start=start;this.end=end;this.rows=Collections.unmodifiableList(new ArrayList<>(rows));}
    }
    private static volatile Long pruneBeforeR31;
    private static volatile String flushReasonR31 = "NOT_SAMPLED";
    private static volatile int lastOpsR31;
    private static volatile long lastBindBytesR31;

    /** Shared only by these two telemetry owners. No second queue or migration owner. */
    static final class PreparedRowR31 {
        final String sql,verifySql; final Object[] args; final String[] verifyArgs;
        final int ops; final long bytes;
        PreparedRowR31(String sql,Object[] args,String verifySql,String[] verifyArgs) {
            this.sql=sql;this.args=args.clone();this.verifySql=verifySql;
            this.verifyArgs=verifyArgs==null?null:verifyArgs.clone();
            ops=(sql==null?0:1)+(verifySql==null?0:1);
            bytes=bindBytesR31(this.args)+bindBytesR31(this.verifyArgs);
        }
        void apply(SQLiteDatabase db) {
            if(sql!=null)db.execSQL(sql,args);
            if(verifySql!=null)try(Cursor c=db.rawQuery(verifySql,verifyArgs)) {
                if(!c.moveToFirst())throw new PreparedConflictR31();
            }
        }
        Object descriptor(){return java.util.Arrays.asList(sql,java.util.Arrays.asList(args),verifySql,verifyArgs==null?null:java.util.Arrays.asList(verifyArgs));}
    }
    static final class PreparedConflictR31 extends IllegalStateException {
        PreparedConflictR31(){super("R31_TELEMETRY_PREPARED_SOURCE_CHANGED");}
    }
    static final class ChunkR31 {
        final List<PreparedRowR31> rows;final String key,payload,input,task;final long seq;
        final int ops;final long bytes;
        ChunkR31(String task,String input,long seq,List<PreparedRowR31> rows) {
            this.task=task;this.input=input;this.seq=seq;this.rows=Collections.unmodifiableList(new ArrayList<>(rows));
            List<Object> descriptor=new ArrayList<>();int operations=0;long binds=0;
            for(PreparedRowR31 row:rows){descriptor.add(row.descriptor());operations+=row.ops;binds+=row.bytes;}
            payload=HashingSU1.fields("R31_TELEMETRY_CHUNK_V1",task,input,Long.toString(seq),JsonR27.write(descriptor));
            key=HashingSU1.fields("R31_TELEMETRY_RECEIPT_V1",task,input,Long.toString(seq),payload);
            ops=operations;bytes=binds;
        }
    }
    static long bindCapR31(){return Math.min(262144L,Runtime.getRuntime().maxMemory()/512L);}
    static long bindBytesR31(Object[] values) {
        long n=0;if(values!=null)for(Object v:values)n+=v instanceof String?((String)v).getBytes(java.nio.charset.StandardCharsets.UTF_8).length:v instanceof byte[]?((byte[])v).length:8;
        return n;
    }
    static long protocolBindBytesR31() {
        RuntimeCoordinatorR26.Claim c=RuntimeCoordinatorR26.currentClaimR30();
        // Conservatively covers receipt fields, schema identifiers, owner-fence binds and progress metadata.
        return 4096L+12*bindBytesR31(new Object[]{c==null?"":c.task,c==null?"":c.owner,IssueTimelineWriterR30.bootId()});
    }
    static boolean fitsR31(int operations,long bytes){return operations+PROTOCOL_OPS_R31<=MAX_OPS_R31&&bytes+protocolBindBytesR31()<=bindCapR31();}
    static void requireReadyR31(SQLiteDatabase db,boolean hold) {
        DatabaseCoordinator.requireSchemaObjectsR31(db,hold?"R30_WRITER_HOLD_FLUSH":"R30_TIMELINE_COMMIT_OBSERVATIONS",
                new String[][]{{hold?"r30_writer_hold":"r27_chart_cache",hold?HOLD_COLUMNS_R31:"panel_id,key,payload_json,updated_at"},
                    {"r31_commit_receipt","commit_key,payload_hash,operation,task_id,owner_epoch,fence,input_snapshot,chunk_seq,operator_version,committed_at"}},new String[0][]);
    }
    static DatabaseCoordinator.CommitResultR31<Integer> commitR31(DbStore store,String operation,boolean hold,ChunkR31 chunk) {
        if(!fitsR31(chunk.ops,chunk.bytes))throw new IllegalStateException("R31_TELEMETRY_BIND_OR_OP_BUDGET");
        RuntimeCoordinatorR26.Claim c=RuntimeCoordinatorR26.currentClaimR30();
        DatabaseCoordinator.CommitIdentityR31 id=new DatabaseCoordinator.CommitIdentityR31(chunk.key,chunk.payload,chunk.task,
                c==null?WriterHoldLedgerR30.PROCESS_SESSION:c.owner,c==null?0:c.fence,chunk.input,chunk.seq,"R31_TELEMETRY_CHUNK_V1");
        return store.coordinatorForResearchR952().runIdempotentAtomicWriteR31(
                DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.TELEMETRY,operation),id,db->{
                    requireReadyR31(db,hold);
                    for(PreparedRowR31 row:chunk.rows)row.apply(db);
                    return chunk.rows.size();
                },chunk.rows.size());
    }
    static void rethrowFatalR31(Throwable failure){Error fatal=FailureDispositionR31.processFatalCause(failure);if(fatal!=null)throw fatal;}
    static boolean preparedConflictR31(Throwable failure) {
        for(int n=0;failure!=null&&n<16;n++,failure=failure.getCause())if(failure instanceof PreparedConflictR31)return true;
        return false;
    }
    private HoldLedgerFlushR30() {}

    public static void maybeFlush(DbStore store) {
        IssueTimelineWriterR30.flushCommitted(store);
        if(!FLUSHING.compareAndSet(false,true))return;
        try {
            requireReadyR31(store.readable(),true); // read only; missing schema never rotates away the live window
            long elapsed=SystemClock.elapsedRealtime();
            if(pending==null&&(lastRotationElapsedR31<0||elapsed-lastRotationElapsedR31>=PERIOD_MS)) {
                synchronized(WriterHoldLedgerR30.class) {
                    long start=WriterHoldLedgerR30.windowStartMs();List<WriterHoldLedgerR30.Stat> rows=WriterHoldLedgerR30.rotate();
                    long end=System.currentTimeMillis();lastRotationElapsedR31=elapsed;
                    if(!rows.isEmpty())pending=new Pending(start,end,rows);
                }
            }
            if(pending==null){pruneOneR31(store);return;}
            final Pending closed=pending;
            if(closed.chunk==null)closed.chunk=prepareHoldR31(closed);
            lastOpsR31=closed.chunk.ops+PROTOCOL_OPS_R31;lastBindBytesR31=closed.chunk.bytes+protocolBindBytesR31();
            DatabaseCoordinator.CommitResultR31<Integer> committed=commitR31(store,"R30_WRITER_HOLD_FLUSH",true,closed.chunk);
            flushReasonR31=committed.outcome.name();closed.cursor+=closed.chunk.rows.size();closed.chunk=null;
            if(closed.cursor==closed.rows.size()){pruneBeforeR31=closed.end-RETAIN_MS;pending=null;}
        }catch(RuntimeException busy){rethrowFatalR31(busy);flushReasonR31=busy.getMessage();android.util.Log.w("STARCORE_R30","writer hold ledger pending retained",busy);}
        finally{FLUSHING.set(false);}
    }

    private static ChunkR31 prepareHoldR31(Pending closed) {
        List<PreparedRowR31> rows=new ArrayList<>();int ops=0;long bytes=0;
        for(int index=closed.cursor;index<closed.rows.size();index++) {
            WriterHoldLedgerR30.Stat s=closed.rows.get(index);
            Object[] args={closed.start,closed.end,s.operation,s.critical?1:0,s.count(),nz(s.holdP(0.5)),nz(s.holdP(0.95)),nz(s.holdP(0.99)),s.holdMax(),nz(s.waitP(0.95)),s.waitMax(),s.overBudget(),s.overHard(),s.failures(),JsonR27.write(s.histogram()),WriterHoldLedgerR30.PROCESS_SESSION};
            StringBuilder verify=new StringBuilder("SELECT 1 FROM r30_writer_hold WHERE ");List<String> bound=new ArrayList<>();String[] columns=HOLD_COLUMNS_R31.split(",");
            for(int i=0;i<columns.length;i++){if(i>0)verify.append(" AND ");verify.append(columns[i]);if(args[i]==null)verify.append(" IS NULL");else{verify.append(" IS ?");bound.add(String.valueOf(args[i]));}}
            PreparedRowR31 row=new PreparedRowR31(INSERT_ONCE_R31,args,verify.toString(),bound.toArray(new String[0]));
            if(!fitsR31(ops+row.ops,bytes+row.bytes))break;
            rows.add(row);ops+=row.ops;bytes+=row.bytes;
        }
        if(rows.isEmpty())throw new IllegalStateException("R31_TELEMETRY_SINGLE_ROW_BUDGET_DEBT");
        return new ChunkR31("writer-hold-ledger",HashingSU1.fields(WriterHoldLedgerR30.PROCESS_SESSION,Long.toString(closed.start),Long.toString(closed.end)),closed.cursor,rows);
    }
    private static void pruneOneR31(DbStore store) {
        Long cutoff=pruneBeforeR31;if(cutoff==null)return;
        List<PreparedRowR31> rows=new ArrayList<>();int ops=0;long bytes=0;
        // The existing primary key starts with window_start. Exclude uncertain/reversed clock windows;
        // normal expired windows are selected from its bounded oldest key range, outside the writer.
        try(Cursor c=store.readable().rawQuery("SELECT window_start,operation,critical,window_end,process_session FROM r30_writer_hold WHERE window_start<? AND window_end<? AND window_start<=window_end ORDER BY window_start,operation,critical LIMIT 52",new String[]{Long.toString(cutoff),Long.toString(cutoff)})) {
            while(c.moveToNext()) {
                String session=c.getString(4);Object[] args=session==null?new Object[]{c.getLong(0),c.getString(1),c.getInt(2),c.getLong(3),cutoff}:new Object[]{c.getLong(0),c.getString(1),c.getInt(2),c.getLong(3),cutoff,session};
                String sql="DELETE FROM r30_writer_hold WHERE window_start=? AND operation=? AND critical=? AND window_end=? AND window_end<? AND process_session"+(session==null?" IS NULL":" IS ?");
                PreparedRowR31 row=new PreparedRowR31(sql,args,null,null);
                if(!fitsR31(ops+row.ops,bytes+row.bytes))break;
                rows.add(row);ops+=row.ops;bytes+=row.bytes;
            }
        }
        if(rows.isEmpty()){pruneBeforeR31=null;return;}
        ChunkR31 chunk=new ChunkR31("writer-hold-retention",HashingSU1.fields("EXPIRED_WINDOW",Long.toString(cutoff)),0,rows);
        lastOpsR31=chunk.ops+PROTOCOL_OPS_R31;lastBindBytesR31=chunk.bytes+protocolBindBytesR31();
        flushReasonR31=commitR31(store,"R30_WRITER_HOLD_FLUSH",true,chunk).outcome.name();
    }
    public static Map<String,Object> flushStateR31() {
        Map<String,Object> m=new java.util.LinkedHashMap<>();Pending p=pending;
        m.put("pendingRows",p==null?0:p.rows.size()-p.cursor);m.put("windowStart",p==null?null:p.start);m.put("windowEnd",p==null?null:p.end);
        m.put("reason",flushReasonR31);m.put("lastPreparedOpsIncludingAllowance",lastOpsR31);m.put("lastPreparedBindBytesIncludingAllowance",lastBindBytesR31);
        m.put("bindCap",bindCapR31());m.put("protocolOpsAllowance",PROTOCOL_OPS_R31);m.put("scope","Actual pending owner window; no uptime, device timing or quality claim");return m;
    }

    private static Long nz(long v) { return v < 0||v==Long.MAX_VALUE ? null : v; }

    /** 报告第3章：最近24小时后台超时操作（按超硬限次数降序，最多50行）。 */
    public static final String REPORT_TOP =
            "SELECT operation,sum(count),max(hold_max),max(hold_p95),sum(over_budget),sum(over_hard) FROM r30_writer_hold "
            + "WHERE critical=0 AND window_end>=? GROUP BY operation HAVING sum(over_budget)>0 ORDER BY sum(over_hard) DESC,sum(over_budget) DESC LIMIT 50";
    /** 报告第3章：关键业务写等待（M07）。 */
    public static final String REPORT_CRITICAL =
            "SELECT operation,sum(count),max(wait_p95),max(wait_max),max(hold_max) FROM r30_writer_hold WHERE critical=1 AND window_end>=? GROUP BY operation ORDER BY max(wait_p95) DESC";

    public static long overHardSince(SQLiteDatabase ro, long since) {
        try (Cursor c = ro.rawQuery("SELECT coalesce(sum(over_hard),0) FROM r30_writer_hold WHERE critical=0 AND window_end>=?", new String[] {Long.toString(since)})) {
            return c.moveToFirst() ? c.getLong(0) : -1L;
        } catch (RuntimeException missingTable) { return -1L; }
    }
}
