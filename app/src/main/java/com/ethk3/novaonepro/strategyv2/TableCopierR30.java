package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DatabaseCoordinator;
import java.util.ArrayList;
import java.util.List;

/**
 * R30 通用整表复制（B00 回收R29独立库、B11 条件切库共用）。
 *
 * 不使用 ATTACH：Android 在 WAL 库上执行 ATTACH 会自动关闭 WAL（SQLiteDatabase.executeSql 对 ATTACH 的处理），
 * 因此源库以独立只读连接打开，按 rowid 分页读出，在目标库的短事务里 INSERT OR REPLACE，每页 ≤ 500 行。
 * 列按名称对齐：只复制两侧都存在的列；目标缺失的列记 MISMATCH，不静默丢弃（写入 r30_copy_log）。
 */
public final class TableCopierR30 {
    public static final int PAGE_ROWS = 500;
    public static final String LOG_DDL = "CREATE TABLE IF NOT EXISTS r30_copy_log(run_id TEXT NOT NULL,table_name TEXT NOT NULL,direction TEXT NOT NULL,"
            + "source_rows INTEGER NOT NULL,copied_rows INTEGER NOT NULL,missing_columns TEXT NOT NULL,state TEXT NOT NULL,finished_at INTEGER NOT NULL,"
            + "PRIMARY KEY(run_id,table_name,direction))";
    private TableCopierR30() {}

    /** 目标写入口：B00 回收用 DatabaseCoordinator（Root 唯一写者）；B11 用研究库自身单写者。 */
    public interface Writer { void tx(String operation, DatabaseCoordinator.WriteRunnable body) throws Exception; }

    public static final class Outcome {
        public final String table; public final long sourceRows, copiedRows; public final List<String> missingColumns; public final String state;
        Outcome(String t, long s, long c, List<String> m, String st) { table = t; sourceRows = s; copiedRows = c; missingColumns = m; state = st; }
    }

    public static List<String> columns(SQLiteDatabase db, String table) {
        List<String> out = new ArrayList<>();
        try (Cursor c = db.rawQuery("PRAGMA table_info(" + q(table) + ")", null)) { while (c.moveToNext()) out.add(c.getString(1)); }
        return out;
    }

    public static boolean hasRowid(SQLiteDatabase db, String table) {
        try (Cursor c = db.rawQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name=?", new String[] {table})) {
            return c.moveToFirst() && !String.valueOf(c.getString(0)).toUpperCase(java.util.Locale.ROOT).replaceAll("\\s+", " ").contains("WITHOUT ROWID");
        }
    }

    public static long count(SQLiteDatabase db, String table) {
        try (Cursor c = db.rawQuery("SELECT count(*) FROM " + q(table), null)) { return c.moveToFirst() ? c.getLong(0) : 0; }
    }

    /** 复制一张表；目标表不存在时用源表 DDL 建表。返回结果并写 r30_copy_log。 */
    public static Outcome copy(final SQLiteDatabase source, final SQLiteDatabase targetRo, Writer target, final String table,
                               final String runId, final String direction) throws Exception {
        final List<String> src = columns(source, table);
        List<String> dst = columns(targetRo, table);
        if (dst.isEmpty()) {
            final String ddl;
            try (Cursor c = source.rawQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name=?", new String[] {table})) {
                ddl = c.moveToFirst() ? c.getString(0) : null;
            }
            if (ddl == null) throw new IllegalStateException("R30_COPY_SOURCE_DDL_MISSING:" + table);
            target.tx("R30_COPY_CREATE", db -> db.execSQL(ddl));
            dst = columns(targetRo, table);
        }
        final List<String> common = new ArrayList<>(), missing = new ArrayList<>();
        for (String c : src) if (dst.contains(c)) common.add(c); else missing.add(c);
        if (common.isEmpty()) throw new IllegalStateException("R30_COPY_NO_COMMON_COLUMNS:" + table);
        StringBuilder cols = new StringBuilder(), marks = new StringBuilder();
        for (int i = 0; i < common.size(); i++) { if (i > 0) { cols.append(','); marks.append(','); } cols.append(q(common.get(i))); marks.append('?'); }
        final String insert = "INSERT OR REPLACE INTO " + q(table) + "(" + cols + ") VALUES(" + marks + ")";
        final boolean rowid = hasRowid(source, table);
        final long sourceRows = count(source, table);
        long copied = 0, lastRowid = Long.MIN_VALUE; int offset = 0;
        while (true) {
            final List<Object[]> page = new ArrayList<>(PAGE_ROWS);
            String select = rowid
                    ? "SELECT rowid," + cols + " FROM " + q(table) + " WHERE rowid>? ORDER BY rowid LIMIT " + PAGE_ROWS
                    : "SELECT 0," + cols + " FROM " + q(table) + " ORDER BY " + pkOrder(source, table) + " LIMIT " + PAGE_ROWS + " OFFSET " + offset;
            try (Cursor c = source.rawQuery(select, rowid ? new String[] {Long.toString(lastRowid)} : null)) {
                while (c.moveToNext()) {
                    Object[] row = new Object[common.size()];
                    for (int i = 0; i < row.length; i++) row[i] = value(c, i + 1);
                    page.add(row);
                    if (rowid) lastRowid = c.getLong(0);
                }
            }
            if (page.isEmpty()) break;
            target.tx("R30_COPY_PAGE", db -> { for (Object[] r : page) db.execSQL(insert, r); });
            copied += page.size();
            offset += page.size();
            if (page.size() < PAGE_ROWS) break;
        }
        final String state = copied != sourceRows ? "COUNT_MISMATCH" : missing.isEmpty() ? "COPIED" : "COPIED_WITH_MISSING_COLUMNS";
        final long copiedFinal = copied;
        target.tx("R30_COPY_LOG", db -> {
            db.execSQL(LOG_DDL);
            db.execSQL("INSERT OR REPLACE INTO r30_copy_log VALUES(?,?,?,?,?,?,?,?)",
                    new Object[] {runId, table, direction, sourceRows, copiedFinal, String.join(",", missing), state, System.currentTimeMillis()});
        });
        return new Outcome(table, sourceRows, copied, missing, state);
    }

    /** WITHOUT ROWID 表按主键列顺序分页（主键列从 PRAGMA table_info 的 pk 序号取得）。 */
    static String pkOrder(SQLiteDatabase db, String table) {
        java.util.TreeMap<Integer, String> pk = new java.util.TreeMap<>();
        try (Cursor c = db.rawQuery("PRAGMA table_info(" + q(table) + ")", null)) { while (c.moveToNext()) if (c.getInt(5) > 0) pk.put(c.getInt(5), q(c.getString(1))); }
        if (pk.isEmpty()) throw new IllegalStateException("R30_COPY_WITHOUT_ROWID_NO_PK:" + table);
        return String.join(",", pk.values());
    }

    static Object value(Cursor c, int i) {
        switch (c.getType(i)) {
            case Cursor.FIELD_TYPE_NULL: return null;
            case Cursor.FIELD_TYPE_INTEGER: return c.getLong(i);
            case Cursor.FIELD_TYPE_FLOAT: return c.getDouble(i);
            case Cursor.FIELD_TYPE_BLOB: return c.getBlob(i);
            default: return c.getString(i);
        }
    }

    static String q(String identifier) { return "\"" + identifier.replace("\"", "\"\"") + "\""; }
}
