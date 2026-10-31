package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DbStore;
import java.util.List;

/**
 * R30-B01-02 写锁账本落库：每 60 秒最多一次，一个后台短事务（≤ 512 行）。
 * 调用点（唯一）：ResearchPoolR27.recordThreadCpu(...) 末尾 writes.flush() 之后一行：HoldLedgerFlushR30.maybeFlush(store);
 * 失败只记日志并保留内存窗口到下一次，不影响研究线程。
 */
public final class HoldLedgerFlushR30 {
    public static final long PERIOD_MS = 60_000L;
    public static final long RETAIN_MS = 7L * 24 * 3600_000L;
    public static final String DDL = "CREATE TABLE IF NOT EXISTS r30_writer_hold(window_start INTEGER NOT NULL,window_end INTEGER NOT NULL,"
            + "operation TEXT NOT NULL,critical INTEGER NOT NULL,count INTEGER NOT NULL,hold_p50 INTEGER,hold_p95 INTEGER,hold_p99 INTEGER,"
            + "hold_max INTEGER NOT NULL,wait_p95 INTEGER,wait_max INTEGER NOT NULL,over_budget INTEGER NOT NULL,over_hard INTEGER NOT NULL,"
            + "failures INTEGER NOT NULL,PRIMARY KEY(window_start,operation,critical))";
    public static final String INSERT = "INSERT OR REPLACE INTO r30_writer_hold(window_start,window_end,operation,critical,count,hold_p50,hold_p95,hold_p99,"
            + "hold_max,wait_p95,wait_max,over_budget,over_hard,failures) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    public static final String PRUNE = "DELETE FROM r30_writer_hold WHERE window_end<?";
    private static volatile long lastFlush;
    private HoldLedgerFlushR30() {}

    public static void maybeFlush(DbStore store) {
        long now = System.currentTimeMillis();
        if (now - lastFlush < PERIOD_MS) return;
        lastFlush = now;
        final long start = WriterHoldLedgerR30.windowStartMs();
        final List<WriterHoldLedgerR30.Stat> closed = WriterHoldLedgerR30.rotate();
        if (closed.isEmpty()) return;
        try {
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R30_WRITER_HOLD_FLUSH", db -> {
                db.execSQL(DDL);
                for (WriterHoldLedgerR30.Stat s : closed) {
                    db.execSQL(INSERT, new Object[] {start, now, s.operation, s.critical ? 1 : 0, s.count(), nz(s.holdP(0.5)), nz(s.holdP(0.95)),
                            nz(s.holdP(0.99)), s.holdMax(), nz(s.waitP(0.95)), s.waitMax(), s.overBudget(), s.overHard(), s.failures()});
                }
                db.execSQL(PRUNE, new Object[] {now - RETAIN_MS});
                return 0;
            });
        } catch (RuntimeException busy) {
            android.util.Log.w("STARCORE_R30", "writer hold ledger deferred", busy);
        }
    }

    private static Long nz(long v) { return v < 0 ? null : v; }

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
