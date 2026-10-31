package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** R27-018 offline history import (docs/03 3.1). Each CSV row is compared with the database row of the same issue:
 * identical permutation → MATCH (nothing imported, the database stays the source); different → CONFLICT (listed,
 * never used); absent from the database → IMPORTED into r27_history_import, a replay-only table (the draws table,
 * settlement and P0 are untouched). Replay datasets use imported rows only for issues older than the database. */
public final class HistoryImportR27 {
    private HistoryImportR27() {}
    public static final String DDL = "CREATE TABLE IF NOT EXISTS r27_history_import(issue TEXT PRIMARY KEY,ordinal INTEGER NOT NULL,numbers TEXT NOT NULL,status TEXT NOT NULL CHECK(status IN ('IMPORTED','CONFLICT','MATCH')),db_numbers TEXT,source TEXT NOT NULL,imported_at INTEGER NOT NULL)";

    /** R30-B03-02 import governance, pure (host-testable): rows sorted by ordinal (any input order, across days),
     * illegal rows and bad issues dropped, duplicate issues kept once (a differing duplicate is a conflict), gaps counted. */
    public static final class Governed {
        public final List<ReplayDatasetR27.Row> rows = new java.util.ArrayList<>();
        public int invalid, duplicates, duplicateConflicts, gaps, missingIssues;
        public final List<String> conflictIssues = new java.util.ArrayList<>();
    }
    public static Governed govern(List<ReplayDatasetR27.Row> input) {
        Governed g = new Governed(); java.util.Map<String, ReplayDatasetR27.Row> byIssue = new java.util.HashMap<>();
        for (ReplayDatasetR27.Row r : input) {
            if (r == null || !ReplayDatasetR27.legal(r.numbers) || !TargetIssueResolver.isValid(r.issue)) { g.invalid++; continue; }
            ReplayDatasetR27.Row first = byIssue.get(r.issue);
            if (first == null) { byIssue.put(r.issue, r); continue; }
            g.duplicates++;
            if (!java.util.Arrays.equals(first.numbers, r.numbers)) { g.duplicateConflicts++; g.conflictIssues.add(r.issue); }
        }
        for (String bad : g.conflictIssues) byIssue.remove(bad); // a self-contradicting CSV issue is never imported
        g.rows.addAll(byIssue.values());
        java.util.Collections.sort(g.rows, (a, b) -> Long.compare(TargetIssueResolver.sequenceOrdinalSU1(a.issue), TargetIssueResolver.sequenceOrdinalSU1(b.issue)));
        for (int i = 1; i < g.rows.size(); i++) {
            long d = TargetIssueResolver.sequenceOrdinalSU1(g.rows.get(i).issue) - TargetIssueResolver.sequenceOrdinalSU1(g.rows.get(i - 1).issue);
            if (d > 1) { g.gaps++; g.missingIssues += (int) (d - 1); }
        }
        return g;
    }

    public static String importCsv(DbStore store, InputStream in) throws Exception {
        Governed governed = govern(ReplayDatasetR27.readCsv(new InputStreamReader(in, StandardCharsets.UTF_8)));
        List<ReplayDatasetR27.Row> rows = governed.rows;
        int match = 0, conflict = 0, imported = 0, invalid = governed.invalid; StringBuilder conflicts = new StringBuilder();
        ResearchWriteQueueR27.Batch b = new ResearchWriteQueueR27.Batch(); long now = System.currentTimeMillis();
        b.add(DDL);
        SQLiteDatabase db = store.readable();
        for (ReplayDatasetR27.Row r : rows) {
            String nums = join(r.numbers), dbNums = null;
            try (Cursor c = db.rawQuery("SELECT n1,n2,n3,n4,n5,n6,n7,n8,n9,n10 FROM draws WHERE issue=? AND dirty=0", new String[] {r.issue})) {
                if (c.moveToFirst()) { int[] p = new int[10]; for (int i = 0; i < 10; i++) p[i] = c.getInt(i); dbNums = join(p); }
            }
            String status = dbNums == null ? "IMPORTED" : dbNums.equals(nums) ? "MATCH" : "CONFLICT";
            if (status.equals("MATCH")) match++; else if (status.equals("CONFLICT")) { conflict++; if (conflicts.length() < 400) conflicts.append(r.issue).append(' '); } else imported++;
            b.add("INSERT OR REPLACE INTO r27_history_import(issue,ordinal,numbers,status,db_numbers,source,imported_at) VALUES(?,?,?,?,?,?,?)", r.issue, TargetIssueResolver.sequenceOrdinalSU1(r.issue), nums, status, dbNums, "SAF_CSV", now);
        }
        // R30-B03-02: conflicts (database mismatch or self-contradicting CSV duplicates) are also counted in r27_fetch_reject.
        int rejectedConflicts = conflict + governed.duplicateConflicts;
        if (rejectedConflicts > 0) {
            String last = governed.conflictIssues.isEmpty() ? conflicts.toString().trim().replaceAll(".* ", "") : governed.conflictIssues.get(governed.conflictIssues.size() - 1);
            b.add("INSERT OR IGNORE INTO r27_fetch_reject(reason,count,last_issue,last_at) VALUES('HISTORY_IMPORT_CONFLICT',0,?,?)", last, now);
            b.add("UPDATE r27_fetch_reject SET count=count+?,last_issue=?,last_at=? WHERE reason='HISTORY_IMPORT_CONFLICT'", rejectedConflicts, last, now);
        }
        ResearchWriteQueueR27 q = new ResearchWriteQueueR27(store);
        while (!q.submit("HISTORY_IMPORT", b, () -> false)) Thread.sleep(50);
        return "历史CSV：比对一致 " + match + "；冲突 " + conflict + "（不导入）" + (conflict > 0 ? "：" + conflicts : "") + "；新增回放专用 " + imported + "；非法行 " + invalid
                + "；重复期 " + governed.duplicates + "（其中自相矛盾 " + governed.duplicateConflicts + "，不导入）；缺期 " + governed.missingIssues + "（" + governed.gaps + " 处断档，回放按段处理）。";
    }

    static String join(int[] p) { StringBuilder s = new StringBuilder(); for (int i = 0; i < p.length; i++) { if (i > 0) s.append(','); s.append(p[i]); } return s.toString(); }

    /** Imported rows older than the oldest database draw (replay only). */
    public static void appendOlder(SQLiteDatabase ro, List<ReplayDatasetR27.Row> rows, String oldestDbIssue, int limit) {
        try (Cursor c = ro.rawQuery("SELECT issue,ordinal,numbers FROM r27_history_import WHERE status='IMPORTED' AND issue<? ORDER BY issue DESC LIMIT " + limit, new String[] {oldestDbIssue})) {
            while (c.moveToNext()) { String[] t = c.getString(2).split(","); int[] p = new int[10]; for (int i = 0; i < 10; i++) p[i] = Integer.parseInt(t[i]); rows.add(new ReplayDatasetR27.Row(c.getString(0), c.getLong(1), p)); }
        } catch (RuntimeException absent) { /* no import yet */ }
    }

    public static int retryQuarantined(DbStore store) {
        return store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R27_RETRY_QUARANTINED", db -> {
            int n = 0; try (Cursor c = db.rawQuery("SELECT count(*) FROM r26_task WHERE status='QUARANTINED'", null)) { if (c.moveToFirst()) n = c.getInt(0); }
            db.execSQL("UPDATE r26_task SET status='READY',wait_kind='',error_count=0,error_window_start=0,next_at=0 WHERE status='QUARANTINED'"); return n; });
    }
}
