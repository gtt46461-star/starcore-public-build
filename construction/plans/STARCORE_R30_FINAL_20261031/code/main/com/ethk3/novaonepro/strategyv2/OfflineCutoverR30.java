package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DbStore;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * R30-B11（条件批次）停机式离线切库：只迁移 R27 研究引擎的 r27_* 表到 starcore-r30-research.db。
 *
 * 触发条件（docs/B11 第1节，三项同时满足才允许施工）：B01+B02 完成后真机24h报告中
 *   (1) M07 关键写等待 P95 > 200ms；(2) r30_writer_hold 显示超时持有的前5名后台操作中 ≥3 个属于 R27 研究写；
 *   (3) 对这些操作按 ChunkedWriterR30 切片后复测仍不达标。否则 B11 不施工，写 NOT_REQUIRED。
 *
 * 执行时点：RuntimeCoordinatorR26.initialize() 中 R29FoldBackR30.run 之后、任何研究 worker 启动之前；
 * 只有这一个调用点。整个过程中 R27 研究写者不存在（worker 尚未启动），实时链只写实时表，不受影响。
 *
 * 步骤（每一步都可重入）：
 *  S1 冻结：在 Root 为每张待迁表建 BEFORE INSERT/UPDATE/DELETE 触发器，r30_research_freeze.state>=1 时 RAISE(ABORT,'R30_RESEARCH_TABLE_FROZEN:t')；
 *     置 state=1。此后任何漏网写者立即失败并带表名（而不是像R29那样事后发现哈希漂移）。
 *  S2 复制：TableCopierR30 逐表复制到研究库（研究库自身单写者）；
 *  S3 校验：逐表 count 相等，且按 rowid 顺序的整表内容哈希相等（ContentHash）；任一不等 → state 回到 0、删研究库、下次重来；
 *  S4 发布：Root 写 r30_research_route(ACTIVE, file, verifiedAt)，state=2（Root 中这些表永久只读，冻结触发器保留）；
 *  S5 之后 ResearchWriteQueueR27 的 writer 与 StoreR27 读写连接改指研究库（docs/B11 卡 R30-B11-02 的唯一路由点）。
 * 回滚：state=2 → 管理员开关 r30.research.rollback=1 时，反向复制研究库 → Root，删除冻结触发器，路由 RETIRED。
 */
public final class OfflineCutoverR30 {
    public static final String FILE_NAME = "starcore-r30-research.db";
    public static final String[] EXCLUDED = {"r27_fetch_reject", "r27_chart_cache"};
    public static final String FREEZE_DDL = "CREATE TABLE IF NOT EXISTS r30_research_freeze(singleton INTEGER PRIMARY KEY CHECK(singleton=1),state INTEGER NOT NULL,updated_at INTEGER NOT NULL)";
    public static final String ROUTE_DDL = "CREATE TABLE IF NOT EXISTS r30_research_route(singleton INTEGER PRIMARY KEY CHECK(singleton=1),phase TEXT NOT NULL,file_name TEXT NOT NULL,tables INTEGER NOT NULL,rows INTEGER NOT NULL,verified_at INTEGER NOT NULL)";
    private OfflineCutoverR30() {}

    public static String freezeTrigger(String table, String event) {
        String name = "r30_frozen_" + table + "_" + event.toLowerCase(java.util.Locale.ROOT);
        return "CREATE TRIGGER IF NOT EXISTS " + TableCopierR30.q(name) + " BEFORE " + event + " ON " + TableCopierR30.q(table)
                + " WHEN (SELECT state FROM r30_research_freeze WHERE singleton=1)>=1 BEGIN SELECT RAISE(ABORT,'R30_RESEARCH_TABLE_FROZEN:" + table + "'); END";
    }

    public static List<String> movedTables(SQLiteDatabase root) {
        List<String> out = new ArrayList<>();
        try (Cursor c = root.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name LIKE 'r27\\_%' ESCAPE '\\' ORDER BY name", null)) {
            while (c.moveToNext()) { String n = c.getString(0); boolean ex = false; for (String e : EXCLUDED) if (e.equals(n)) ex = true; if (!ex) out.add(n); }
        }
        return out;
    }

    public static String phase(SQLiteDatabase root) {
        try (Cursor c = root.rawQuery("SELECT phase FROM r30_research_route WHERE singleton=1", null)) { return c.moveToFirst() ? c.getString(0) : "NONE"; }
        catch (android.database.sqlite.SQLiteException noTable) { return "NONE"; }
    }

    public static String run(DbStore store) throws Exception {
        final SQLiteDatabase ro = store.coordinatorForResearchR952().readable();
        if ("ACTIVE".equals(phase(ro))) return "ACTIVE";
        TableCopierR30.Writer root = (op, body) -> store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(op, db -> { body.run(db); return 0; });
        final List<String> tables = movedTables(ro);
        // S1
        root.tx("R30_CUTOVER_FREEZE", db -> {
            db.execSQL(FREEZE_DDL); db.execSQL(ROUTE_DDL);
            for (String t : tables) for (String ev : new String[] {"INSERT", "UPDATE", "DELETE"}) db.execSQL(freezeTrigger(t, ev));
            db.execSQL("INSERT OR REPLACE INTO r30_research_freeze VALUES(1,1,?)", new Object[] {System.currentTimeMillis()});
        });
        File file = store.contextForR26().getDatabasePath(FILE_NAME);
        if (file.exists()) for (String s : new String[] {"", "-wal", "-shm", "-journal"}) new File(file.getPath() + s).delete(); // 未发布的半成品，每次重来
        SQLiteDatabase research = SQLiteDatabase.openOrCreateDatabase(file, null);
        long rows = 0;
        try {
            research.enableWriteAheadLogging();
            final SQLiteDatabase rw = research;
            TableCopierR30.Writer dest = (op, body) -> { synchronized (rw) { rw.beginTransactionNonExclusive(); try { body.run(rw); rw.setTransactionSuccessful(); } finally { rw.endTransaction(); } } };
            String runId = "CUT-" + System.currentTimeMillis();
            for (String t : tables) {
                TableCopierR30.Outcome o = TableCopierR30.copy(ro, research, dest, t, runId, "ROOT_TO_R30_RESEARCH");
                if (!"COPIED".equals(o.state)) throw new IllegalStateException("R30_CUTOVER_COPY:" + t + ":" + o.state);
                String a = ContentHash.of(ro, t), b = ContentHash.of(research, t);
                if (!a.equals(b)) throw new IllegalStateException("R30_CUTOVER_HASH:" + t);
                rows += o.copiedRows;
            }
            try (Cursor c = research.rawQuery("PRAGMA integrity_check", null)) { if (!c.moveToFirst() || !"ok".equals(c.getString(0))) throw new IllegalStateException("R30_CUTOVER_INTEGRITY"); }
        } catch (Exception failed) {
            research.close();
            root.tx("R30_CUTOVER_UNFREEZE", db -> db.execSQL("UPDATE r30_research_freeze SET state=0,updated_at=? WHERE singleton=1", new Object[] {System.currentTimeMillis()}));
            throw failed;
        }
        research.close();
        final int n = tables.size(); final long total = rows;
        // S4
        root.tx("R30_CUTOVER_PUBLISH", db -> {
            db.execSQL("INSERT OR REPLACE INTO r30_research_route VALUES(1,'ACTIVE',?,?,?,?)", new Object[] {FILE_NAME, n, total, System.currentTimeMillis()});
            db.execSQL("UPDATE r30_research_freeze SET state=2,updated_at=? WHERE singleton=1", new Object[] {System.currentTimeMillis()});
        });
        return "ACTIVE:" + n + ":" + total;
    }

    /** 表内容哈希：按 rowid（或主键）顺序，逐列类型+值规范化后 SHA-256。 */
    public static final class ContentHash {
        public static String of(SQLiteDatabase db, String table) {
            List<String> cols = TableCopierR30.columns(db, table);
            StringBuilder sel = new StringBuilder("SELECT ");
            for (int i = 0; i < cols.size(); i++) { if (i > 0) sel.append(','); sel.append(TableCopierR30.q(cols.get(i))); }
            sel.append(" FROM ").append(TableCopierR30.q(table)).append(" ORDER BY ")
                    .append(TableCopierR30.hasRowid(db, table) ? "rowid" : TableCopierR30.pkOrder(db, table));
            try {
                java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
                try (Cursor c = db.rawQuery(sel.toString(), null)) {
                    while (c.moveToNext()) {
                        for (int i = 0; i < cols.size(); i++) {
                            int type = c.getType(i); md.update((byte) type);
                            byte[] v = type == Cursor.FIELD_TYPE_NULL ? new byte[0] : type == Cursor.FIELD_TYPE_BLOB ? c.getBlob(i)
                                    : type == Cursor.FIELD_TYPE_INTEGER ? Long.toString(c.getLong(i)).getBytes(java.nio.charset.StandardCharsets.UTF_8)
                                    : type == Cursor.FIELD_TYPE_FLOAT ? Long.toString(Double.doubleToLongBits(c.getDouble(i))).getBytes(java.nio.charset.StandardCharsets.UTF_8)
                                    : c.getString(i).getBytes(java.nio.charset.StandardCharsets.UTF_8);
                            md.update(java.nio.ByteBuffer.allocate(4).putInt(v.length).array()); md.update(v);
                        }
                        md.update((byte) 0x1e);
                    }
                }
                StringBuilder hx = new StringBuilder(64); for (byte x : md.digest()) hx.append(Character.forDigit((x >> 4) & 15, 16)).append(Character.forDigit(x & 15, 16)); return hx.toString();
            } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
        }
    }
}
