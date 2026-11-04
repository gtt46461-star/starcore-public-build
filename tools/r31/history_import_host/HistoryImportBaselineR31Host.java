package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DbStore;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;

/** Exact old importer negative control; synthetic legal rows are engineering input only. */
public final class HistoryImportBaselineR31Host {
    static long scalar(SQLiteDatabase db, String sql) { try (Cursor c = db.rawQuery(sql, null)) { return c.moveToFirst() ? c.getLong(0) : 0; } }
    static String issue(int row) { return java.time.LocalDate.of(2020, 1, 1).plusDays(row / 1440).toString().replace("-", "") + String.format(java.util.Locale.ROOT, "%04d", row % 1440 + 1); }
    static byte[] csv(int rows) {
        StringBuilder s = new StringBuilder("period,open_time_utc_ms,open_time_local,open_number,block,hash,valid,fetch_time\n");
        for (int i = 0; i < rows; i++) s.append(issue(i)).append(",0,,\"1,2,3,4,5,6,7,8,9,10\",HOST,HOST,1,0\n");
        return s.toString().getBytes(StandardCharsets.UTF_8);
    }
    static void canonical(SQLiteDatabase db, int rows) {
        db.execSQL("CREATE TABLE IF NOT EXISTS draws(issue TEXT PRIMARY KEY,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER,dirty INTEGER DEFAULT 0,draw_hash_v2 TEXT)");
        db.execSQL("INSERT OR IGNORE INTO draws VALUES(?,1,2,3,4,5,6,7,8,9,10,0,'HOST_MATCH')", new Object[] {issue(rows - 2)});
        db.execSQL("INSERT OR IGNORE INTO draws VALUES(?,10,9,8,7,6,5,4,3,2,1,0,'HOST_CONFLICT')", new Object[] {issue(rows - 1)});
    }
    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC"); File path = new File(args[1]); path.getParentFile().mkdirs(); int rows = Integer.parseInt(args[2]);
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + path)) {
            SQLiteDatabase db = new SQLiteDatabase(connection); canonical(db, rows);
            try (R31OwnedRuntimeFixture owner = new R31OwnedRuntimeFixture(db, "HOST_HISTORY_IMPORT_BASELINE")) {
                DbStore store = new DbStore(owner.coordinator, db, new android.app.Application(path));
                SQLiteDatabase.traceHistoryR31 = true;
                if (args[0].equals("crash-after-commit")) SQLiteDatabase.afterCommitR31 = () -> Runtime.getRuntime().halt(73);
                String result = HistoryImportR27.importCsv(store, new ByteArrayInputStream(csv(rows)));
                long first = scalar(db, "SELECT count FROM r27_fetch_reject WHERE reason='HISTORY_IMPORT_CONFLICT'");
                if (args[0].equals("repeat")) HistoryImportR27.importCsv(store, new ByteArrayInputStream(csv(rows)));
                long second = scalar(db, "SELECT count FROM r27_fetch_reject WHERE reason='HISTORY_IMPORT_CONFLICT'");
                System.out.println("HISTORY_OLD_NEGATIVE_CONTROL|runtimeDdl=" + SQLiteDatabase.historyDdlR31 + "|maxSqlOpsInTx=" + SQLiteDatabase.maxHistoryOpsR31 + "|maxNativeTxMs=" + SQLiteDatabase.maxHistoryNanosR31 / 1e6 + "|physicalRows=" + scalar(db, "SELECT count(*) FROM r27_history_import") + "|conflictCountBeforeRepeat=" + first + "|conflictCountAfter=" + second + "|canonicalRows=" + scalar(db, "SELECT count(*) FROM draws") + "|scope=HOST_SYNTHETIC_NOT_ANDROID_OR_QUALITY");
                System.out.println(result);
            }
        }
    }
}
