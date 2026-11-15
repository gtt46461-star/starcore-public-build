package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DbStore;

/**
 * R30-B01-03 ChunkedWriterR30 的 Android 适配器：每个片 = DatabaseCoordinator 唯一写锁下的一个原子事务。
 * 游标存 r30_chunk_cursor(cursor_key PK, value, updated_at)，与该片数据同事务提交。
 *
 * 用法（示例：B01-04 结算切片）：
 * <pre>
 *   CoordinatorTxR30 tx = new CoordinatorTxR30(store);
 *   ChunkedWriterR30.Result r = ChunkedWriterR30.run("R27_LIVE_SETTLE_CHUNK", "r27_live_settle:" + latestOrd,
 *       source, sink, tx, 160, 40, 2000, CoordinatorTxR30.REALTIME_PRESSURE, CoordinatorTxR30.MONO);
 * </pre>
 * 操作名必须以 R27_ / R26_ 开头或含 RESEARCH，确保 DatabaseCoordinator.isBackgroundOperationR909 识别为后台写。
 */
public final class CoordinatorTxR30 implements ChunkedWriterR30.TxRunner {
    public static final String DDL = "CREATE TABLE IF NOT EXISTS r30_chunk_cursor(cursor_key TEXT PRIMARY KEY,value INTEGER NOT NULL,updated_at INTEGER NOT NULL)";
    private final DbStore store;

    public CoordinatorTxR30(DbStore store) { this.store = store; }

    /** Sink 内写数据用：((CoordinatorTxR30.SqlTx) tx).db —— 就是本片事务的连接。 */
    public static final class SqlTx implements ChunkedWriterR30.Tx {
        public final SQLiteDatabase db;
        SqlTx(SQLiteDatabase db) { this.db = db; }
        @Override public long readCursor(String key) {
            try (Cursor c = db.rawQuery("SELECT value FROM r30_chunk_cursor WHERE cursor_key=?", new String[] {key})) {
                return c.moveToFirst() ? c.getLong(0) : 0L;
            }
        }
        @Override public void writeCursor(String key, long value) {
            long now = System.currentTimeMillis();
            db.execSQL("INSERT OR IGNORE INTO r30_chunk_cursor(cursor_key,value,updated_at) VALUES(?,?,?)", new Object[] {key, 0L, now});
            db.execSQL("UPDATE r30_chunk_cursor SET value=?,updated_at=? WHERE cursor_key=?", new Object[] {value, now, key});
        }
    }

    @Override public <T> T inTransaction(String operation, final ChunkedWriterR30.TxBody<T> body) throws Exception {
        return store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(operation, db -> {
            db.execSQL(DDL);
            return body.run(new SqlTx(db));
        });
    }

    public static final ChunkedWriterR30.Pressure REALTIME_PRESSURE = new ChunkedWriterR30.Pressure() {
        @Override public boolean criticalWaiting() {
            return com.ethk3.novaonepro.RealtimePressureGateR811.hasActiveCriticalWork() || com.ethk3.novaonepro.SettlementExecutionGateR819.isActive();
        }
        @Override public boolean cancelled() { return Thread.currentThread().isInterrupted(); }
    };
    public static final ChunkedWriterR30.Clock MONO = android.os.SystemClock::elapsedRealtime;
}
