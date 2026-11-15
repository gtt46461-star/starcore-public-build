package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;

/** R30-B01-03 宿主测试。复制到 app/src/test/java/com/ethk3/novaonepro/strategyv2/。 */
public class R30ChunkedWriterTest {
    /** 带回滚语义的内存“数据库”：事务内修改先写副本，提交时替换。 */
    static final class MemDb implements ChunkedWriterR30.TxRunner {
        final TreeMap<Long, String> rows = new TreeMap<>(); final Map<String, Long> cursors = new HashMap<>();
        int commits; RuntimeException failAtKey; long failKey = -1;
        @Override public <T> T inTransaction(String op, ChunkedWriterR30.TxBody<T> body) throws Exception {
            final TreeMap<Long, String> r2 = new TreeMap<>(rows); final Map<String, Long> c2 = new HashMap<>(cursors);
            T v = body.run(new ChunkedWriterR30.Tx() {
                public long readCursor(String k) { Long x = c2.get(k); return x == null ? 0L : x; }
                public void writeCursor(String k, long value) { c2.put(k, value); }
                @SuppressWarnings("unused") void put(long key, String s) { r2.put(key, s); }
            });
            // 提交
            rows.clear(); rows.putAll(r2); cursors.clear(); cursors.putAll(c2); commits++;
            return v;
        }
    }
    static final class R implements ChunkedWriterR30.Row { final long k; R(long k) { this.k = k; } public long key() { return k; } }

    static ChunkedWriterR30.Source<R> source(final int n) {
        return (after, limit) -> { List<R> out = new ArrayList<>(); for (long k = after + 1; k <= n && out.size() < limit; k++) out.add(new R(k)); return out; };
    }
    static final ChunkedWriterR30.Pressure NONE = new ChunkedWriterR30.Pressure() { public boolean criticalWaiting() { return false; } public boolean cancelled() { return false; } };
    static final ChunkedWriterR30.Clock FROZEN = () -> 0L;

    @Test public void drainsInBoundedTransactions() throws Exception {
        MemDb db = new MemDb(); final List<Long> written = new ArrayList<>();
        ChunkedWriterR30.Result r = ChunkedWriterR30.run("T", "c", source(1050), (tx, row) -> written.add(row.key()), db, 200, 50, 10_000, NONE, FROZEN);
        assertEquals("DRAINED", r.stop);
        assertEquals(1050, r.rows);
        assertEquals(6, r.transactions);
        assertEquals(1050L, (long) db.cursors.get("c"));
        assertEquals(1050, written.size());
    }

    @Test public void failedChunkRollsBackCursorAndResumesWithoutDuplicates() throws Exception {
        final MemDb db = new MemDb(); final List<Long> committed = new ArrayList<>(); final List<Long> pending = new ArrayList<>();
        final boolean[] armed = {true};
        ChunkedWriterR30.TxRunner runner = new ChunkedWriterR30.TxRunner() {
            public <T> T inTransaction(String op, ChunkedWriterR30.TxBody<T> body) throws Exception {
                pending.clear(); T v = db.inTransaction(op, body); committed.addAll(pending); return v;
            }
        };
        ChunkedWriterR30.Sink<R> sink = (tx, row) -> { if (armed[0] && row.key() == 250) { armed[0] = false; throw new IllegalStateException("crash"); } pending.add(row.key()); };
        try { ChunkedWriterR30.run("T", "c", source(500), sink, runner, 100, 50, 10_000, NONE, FROZEN); fail("expected crash"); }
        catch (IllegalStateException expected) { assertEquals("crash", expected.getMessage()); }
        assertEquals(200L, (long) db.cursors.get("c"));
        ChunkedWriterR30.Result r = ChunkedWriterR30.run("T", "c", source(500), sink, runner, 100, 50, 10_000, NONE, FROZEN);
        assertEquals("DRAINED", r.stop);
        assertEquals(500, committed.size());
        for (int i = 0; i < 500; i++) assertEquals((long) (i + 1), (long) committed.get(i));
    }

    @Test public void yieldsBetweenChunksWhenCriticalWriterWaits() throws Exception {
        MemDb db = new MemDb(); final int[] calls = {0};
        ChunkedWriterR30.Pressure p = new ChunkedWriterR30.Pressure() { public boolean criticalWaiting() { return ++calls[0] > 2; } public boolean cancelled() { return false; } };
        ChunkedWriterR30.Result r = ChunkedWriterR30.run("T", "c", source(1000), (tx, row) -> {}, db, 100, 50, 10_000, p, FROZEN);
        assertEquals("CRITICAL_YIELD", r.stop);
        assertEquals(2, r.transactions);
        assertEquals(200L, r.lastKey);
    }

    @Test public void timeBudgetCutsChunkAtRowBoundary() throws Exception {
        MemDb db = new MemDb(); final AtomicLong clock = new AtomicLong();
        ChunkedWriterR30.Result r = ChunkedWriterR30.run("T", "c", source(100), (tx, row) -> clock.addAndGet(10), db, 100, 50, 1_000_000, NONE, clock::get);
        assertEquals("DRAINED", r.stop);
        assertEquals(20, r.transactions); // 每片 5 行（第5行后≥50ms）
    }

    @Test(expected = IllegalStateException.class) public void nonIncreasingKeysRejected() throws Exception {
        MemDb db = new MemDb();
        ChunkedWriterR30.run("T", "c", (after, limit) -> { List<R> l = new ArrayList<>(); if (after == 0) { l.add(new R(5)); l.add(new R(5)); } return l; }, (tx, row) -> {}, db, 10, 50, 1000, NONE, FROZEN);
    }

    @Test(expected = IllegalArgumentException.class) public void oversizedChunksRejected() throws Exception {
        ChunkedWriterR30.run("T", "c", source(1), (tx, row) -> {}, new MemDb(), 5000, 50, 1000, NONE, FROZEN);
    }
}
