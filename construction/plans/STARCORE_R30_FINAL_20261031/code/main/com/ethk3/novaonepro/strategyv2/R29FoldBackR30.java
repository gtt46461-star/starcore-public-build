package com.ethk3.novaonepro.strategyv2;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DbStore;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * R30-B00-03 回收 R29 CP07—CP16 测试包留下的独立研究库与 Root 触发器。
 *
 * 背景：R30 以 CP06(bb607cf) 为基线，单库运行。任何安装过 R29 CP07 及以后检查点 APK 的手机，可能已经：
 *  (a) 在 Root 库建立 r29_copy_track_* / r29_critical_track_* / r29_critical_input_write_guard* /
 *      r29_legacy_guard_migration* / r29_retained_formal_copy* 触发器（其中 guard 类会 RAISE(ABORT)，CP06 代码写入会直接失败）；
 *  (b) 发布 r29_database_route(phase='ACTIVE') 并把研究写入转到 starcore-r29-research.db。
 * 本类在 RuntimeCoordinatorR26.initialize() 第一行、任何研究 worker 启动之前执行一次：
 *  1. 删除上述 R29 后期触发器（不动 CP06 自带的 r29_legacy_model_no_update / r29_legacy_model_no_delete）；
 *  2. 若路由 ACTIVE 且 RP 文件存在：RP 中与 Root 同名的研究表按 rowid 分页 INSERT OR REPLACE 回 Root（TableCopierR30，不用 ATTACH）；
 *     RP 中 Root 没有的表按 RP 的 DDL 在 Root 建表后复制；r29_copy_* / r29_migration_* 等迁移自身记账表不回收；
 *  3. 路由改为 RETIRED_BY_R30；RP 文件改名为 *.r30-retired 保留（不删除，由 B08 存储卡按空间预算处理）；
 *  4. 全程写 r30_copy_log 和状态 r30_foldback_state；任一表失败则路由保持 ACTIVE 不改、下次启动重做（INSERT OR REPLACE 幂等）。
 * 未安装过 R29 后期包的手机：两步都是空操作，耗时 < 50ms。
 */
public final class R29FoldBackR30 {
    public static final String RP_FILE = "starcore-r29-research.db";
    public static final String[] DROP_TRIGGER_PREFIXES = {"r29_copy_track_", "r29_critical_track_", "r29_critical_input_write_guard",
            "r29_legacy_guard_migration", "r29_retained_formal_copy", "r29_critical_replica_"};
    public static final String[] KEEP_TRIGGERS = {"r29_legacy_model_no_update", "r29_legacy_model_no_delete"};
    public static final String[] SKIP_TABLE_PREFIXES = {"sqlite_", "android_metadata", "r29_copy_", "r29_migration_", "r29_database_route",
            "r29_source_watermark", "r29_critical_input_", "r29_projection_applied", "r29_research_heartbeat", "r29_scheduler_owner"};
    public static final String STATE_DDL = "CREATE TABLE IF NOT EXISTS r30_foldback_state(singleton INTEGER PRIMARY KEY CHECK(singleton=1),"
            + "state TEXT NOT NULL,run_id TEXT NOT NULL,tables INTEGER NOT NULL,rows INTEGER NOT NULL,triggers_dropped INTEGER NOT NULL,detail TEXT NOT NULL,updated_at INTEGER NOT NULL)";
    private R29FoldBackR30() {}

    public static String run(DbStore store) throws Exception {
        Context ctx = store.contextForR26();
        SQLiteDatabase ro = store.coordinatorForResearchR952().readable();
        final String runId = "FB-" + System.currentTimeMillis();
        TableCopierR30.Writer root = (op, body) -> store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(op, db -> { body.run(db); return 0; });

        final List<String> drop = r29Triggers(ro);
        if (!drop.isEmpty()) root.tx("R30_FOLDBACK_DROP_TRIGGERS", db -> { for (String t : drop) db.execSQL("DROP TRIGGER IF EXISTS " + TableCopierR30.q(t)); });

        String phase = routePhase(ro);
        if (!"ACTIVE".equals(phase)) { state(root, "NO_ACTIVE_R29_ROUTE:" + phase, runId, 0, 0, drop.size(), ""); return "NONE"; }
        File rp = ctx.getDatabasePath(RP_FILE);
        if (!rp.isFile()) {
            root.tx("R30_FOLDBACK_ROUTE_MISSING", db -> db.execSQL("UPDATE r29_database_route SET phase='RETIRED_RP_MISSING_BY_R30' WHERE singleton=1"));
            state(root, "RP_MISSING_ROOT_RETAINED", runId, 0, 0, drop.size(), rp.getAbsolutePath());
            return "RP_MISSING";
        }
        int tables = 0; long rows = 0; StringBuilder detail = new StringBuilder();
        SQLiteDatabase source = SQLiteDatabase.openDatabase(rp.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
        try {
            for (String table : tables(source)) {
                TableCopierR30.Outcome o = TableCopierR30.copy(source, ro, root, table, runId, "R29_RP_TO_ROOT");
                tables++; rows += o.copiedRows;
                if (!o.state.startsWith("COPIED")) detail.append(table).append(':').append(o.state).append(';');
            }
        } finally { source.close(); }
        if (detail.length() > 0) { state(root, "FAILED_ROUTE_KEPT", runId, tables, rows, drop.size(), detail.toString()); throw new IllegalStateException("R30_FOLDBACK_INCOMPLETE:" + detail); }
        root.tx("R30_FOLDBACK_RETIRE_ROUTE", db -> db.execSQL("UPDATE r29_database_route SET phase='RETIRED_BY_R30' WHERE singleton=1"));
        File retired = new File(rp.getParentFile(), RP_FILE + ".r30-retired");
        for (String suffix : new String[] {"", "-wal", "-shm", "-journal"}) {
            File f = new File(rp.getPath() + suffix);
            if (f.exists() && !f.renameTo(new File(retired.getPath() + suffix))) detail.append("RENAME_FAILED:").append(f.getName()).append(';');
        }
        state(root, "FOLDED_BACK", runId, tables, rows, drop.size(), detail.toString());
        return "FOLDED_BACK:" + tables + ":" + rows;
    }

    static List<String> r29Triggers(SQLiteDatabase db) {
        List<String> out = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='trigger' AND name LIKE 'r29\\_%' ESCAPE '\\'", null)) {
            while (c.moveToNext()) {
                String n = c.getString(0);
                boolean keep = false; for (String k : KEEP_TRIGGERS) if (k.equals(n)) keep = true;
                if (keep) continue;
                for (String p : DROP_TRIGGER_PREFIXES) if (n.startsWith(p)) { out.add(n); break; }
            }
        }
        return out;
    }

    static String routePhase(SQLiteDatabase db) {
        try (Cursor c = db.rawQuery("SELECT phase FROM r29_database_route WHERE singleton=1", null)) { return c.moveToFirst() ? c.getString(0) : "NO_ROW"; }
        catch (android.database.sqlite.SQLiteException noTable) { return "NO_TABLE"; }
    }

    static List<String> tables(SQLiteDatabase source) {
        List<String> out = new ArrayList<>();
        try (Cursor c = source.rawQuery("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name", null)) {
            while (c.moveToNext()) {
                String n = c.getString(0); boolean skip = false;
                for (String p : SKIP_TABLE_PREFIXES) if (n.startsWith(p)) { skip = true; break; }
                if (!skip) out.add(n);
            }
        }
        return out;
    }

    static void state(TableCopierR30.Writer root, String state, String runId, int tables, long rows, int triggers, String detail) throws Exception {
        root.tx("R30_FOLDBACK_STATE", db -> {
            db.execSQL(STATE_DDL);
            db.execSQL("INSERT OR REPLACE INTO r30_foldback_state VALUES(1,?,?,?,?,?,?,?)", new Object[] {state, runId, tables, rows, triggers, detail, System.currentTimeMillis()});
        });
    }
}
