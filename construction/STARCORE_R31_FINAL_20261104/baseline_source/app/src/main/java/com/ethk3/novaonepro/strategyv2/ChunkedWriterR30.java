package com.ethk3.novaonepro.strategyv2;

import java.util.List;

/**
 * R30-B01-03 通用切片写入器（纯Java核心，HOST可测）。
 *
 * 用途：把 WriterHoldLedgerR30.offenders() 列出的“后台长事务”改成多个短事务。
 * 规则（施工方不得改动）：
 *  1. 每个事务最多 maxRows 行，且事务体运行超过 maxMillis 后在当前行结束时提交；
 *  2. 游标（cursorKey → 最后已提交的主键）与该片数据在同一事务内提交，杀进程后从游标续做，不重复、不丢；
 *  3. 关键业务写等待时（{@link Pressure#criticalWaiting()}），在片与片之间让出，不在事务体内部让出；
 *  4. 只适用于“逐行幂等、行间无原子性要求”的写；需要整批原子可见的写，改用 manifest+最后提交可见标记（见 docs/B01）。
 */
public final class ChunkedWriterR30 {
    private ChunkedWriterR30() {}

    /** 一行待写数据，key 必须单调递增（主键或 ordinal）。 */
    public interface Row { long key(); }

    /** 由调用方实现：读取 key > after 的最多 limit 行（事务外只读连接）。 */
    public interface Source<R extends Row> { List<R> next(long after, int limit) throws Exception; }

    /** 由调用方实现：在已开启的写事务内写入一行。 */
    public interface Sink<R extends Row> { void write(Tx tx, R row) throws Exception; }

    /** 写事务抽象；Android 实现见 docs/B01 的 CoordinatorTxR30 适配器。 */
    public interface Tx {
        long readCursor(String cursorKey) throws Exception;
        void writeCursor(String cursorKey, long value) throws Exception;
    }

    public interface TxRunner { <T> T inTransaction(String operation, TxBody<T> body) throws Exception; }
    public interface TxBody<T> { T run(Tx tx) throws Exception; }

    public interface Pressure { boolean criticalWaiting(); boolean cancelled(); }
    public interface Clock { long monoMs(); }

    public static final class Result {
        public final int transactions, rows; public final long lastKey; public final String stop;
        Result(int transactions, int rows, long lastKey, String stop) { this.transactions = transactions; this.rows = rows; this.lastKey = lastKey; this.stop = stop; }
        @Override public String toString() { return "tx=" + transactions + " rows=" + rows + " last=" + lastKey + " stop=" + stop; }
    }

    /**
     * 运行至多 sliceBudgetMs（整个调用的总预算），每片一个事务。
     * stop 取值：DRAINED（源已读完）/ SLICE_BUDGET / CRITICAL_YIELD / CANCELLED。
     */
    public static <R extends Row> Result run(final String operation, final String cursorKey,
                                             final Source<R> source, final Sink<R> sink, TxRunner runner,
                                             final int maxRows, final long maxMillis, long sliceBudgetMs,
                                             Pressure pressure, final Clock clock) throws Exception {
        if (maxRows < 1 || maxRows > 2000) throw new IllegalArgumentException("R30_CHUNK_ROWS_1_2000");
        if (maxMillis < 1 || maxMillis > 200) throw new IllegalArgumentException("R30_CHUNK_MS_1_200");
        final long sliceEnd = clock.monoMs() + Math.max(1L, sliceBudgetMs);
        int txCount = 0, rowCount = 0;
        long last = runner.inTransaction(operation + ":CURSOR", new TxBody<Long>() {
            @Override public Long run(Tx tx) throws Exception { return tx.readCursor(cursorKey); }
        });
        while (true) {
            if (pressure.cancelled()) return new Result(txCount, rowCount, last, "CANCELLED");
            if (pressure.criticalWaiting()) return new Result(txCount, rowCount, last, "CRITICAL_YIELD");
            if (clock.monoMs() >= sliceEnd) return new Result(txCount, rowCount, last, "SLICE_BUDGET");
            final List<R> rows = source.next(last, maxRows);
            if (rows == null || rows.isEmpty()) return new Result(txCount, rowCount, last, "DRAINED");
            final long before = last;
            final int[] written = {0};
            long committed = runner.inTransaction(operation, new TxBody<Long>() {
                @Override public Long run(Tx tx) throws Exception {
                    long stored = tx.readCursor(cursorKey);
                    if (stored != before) throw new IllegalStateException("R30_CHUNK_CURSOR_MOVED:" + stored + "!=" + before);
                    long started = clock.monoMs(), k = before;
                    for (R r : rows) {
                        if (r.key() <= k) throw new IllegalStateException("R30_CHUNK_KEY_NOT_INCREASING:" + r.key());
                        sink.write(tx, r);
                        k = r.key();
                        written[0]++;
                        if (clock.monoMs() - started >= maxMillis) break;
                    }
                    tx.writeCursor(cursorKey, k);
                    return k;
                }
            });
            txCount++;
            rowCount += written[0];
            last = committed;
        }
    }
}
