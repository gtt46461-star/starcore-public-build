package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;

/**
 * R30-B02-01 时间线写入（Android 侧）。只能在已有写事务内调用（与业务写同一事务，不新增事务）。
 * 失败只记日志，不能让业务事务回滚：本方法内部吞掉 SQL 异常前先确认不是事务已损坏（SQLiteException 一律吞，诊断表缺失时由 SchemaR27 补建）。
 *
 * 调用点（唯一清单，施工方不得增减；行号以 bb607cf 为准，施工时按方法名定位）：
 *   FIRST_OBSERVED  FetchLedgerR925.java:99   "R925_FETCH_COMPLETE" 回调体末尾，issue = 本次响应解析出的最新期号
 *   VALIDATED       DbStore.java:2191         "R914_DRAW_SAVE_ATOMIC" 回调体：完成期号/号码校验之后、首条 INSERT 之前
 *   DRAW_COMMITTED  DbStore.java:2191         同一回调体最后一条语句之后（提交时刻≈回调结束，误差<事务提交耗时）
 *   SETTLED         DatabaseCoordinator.java:1037 "R36_SETTLEMENT_CORE_R911" 的 task 体末尾，issue = 被结算期号
 *   NEXT_REQUESTED  AnalysisRequestStoreR36.java:59 "R36_ANALYSIS_REQUEST" 回调体末尾，issue = 目标期号；同时写 NEXT_DEADLINE
 *   NEXT_PREPARED   调用 runR36Transaction("R36_CANONICAL",...) 之前一行先取 preparedWall/preparedMono，进入事务后用 markAt 写入
 *   NEXT_FROZEN     DatabaseCoordinator.java:1006 "R36_CANONICAL" 的 task 体末尾
 * 时间线的 issue 统一用“目标期号”字符串（与 r36 表一致），NEXT_* 写在目标期那一行，前四项写在开奖期那一行；
 * 报告按“开奖期 i 的 SETTLED → 目标期 i+1 的 NEXT_FROZEN”配对（ReportR29 第3章，docs/B02 给出SQL）。
 */
public final class IssueTimelineWriterR30 {
    private IssueTimelineWriterR30() {}
    private static final String BOOT_ID = Long.toString((System.currentTimeMillis() - SystemClock.elapsedRealtime()) / 10_000L);

    public static String bootId() { return BOOT_ID; }

    public static void mark(SQLiteDatabase db, String issue, IssueTimelineR30.Stage stage) {
        markAt(db, issue, stage, System.currentTimeMillis(), SystemClock.elapsedRealtime());
    }

    public static void markAt(SQLiteDatabase db, String issue, IssueTimelineR30.Stage stage, long wallMs, long monoMs) {
        if (db == null || issue == null || issue.isEmpty() || !db.inTransaction()) return;
        try {
            long now = System.currentTimeMillis();
            db.execSQL(IssueTimelineR30.SQL_ENSURE, new Object[] {issue, BOOT_ID, now});
            db.execSQL(IssueTimelineR30.sqlMark(stage), IssueTimelineR30.markArgs(stage, issue, wallMs, monoMs, BOOT_ID, now));
        } catch (android.database.sqlite.SQLiteException diagnosticOnly) {
            android.util.Log.w("STARCORE_R30", "timeline mark skipped " + stage, diagnosticOnly);
        }
    }

    public static void deadline(SQLiteDatabase db, String targetIssue, long deadlineWallMs) {
        markAt(db, targetIssue, IssueTimelineR30.Stage.NEXT_DEADLINE, deadlineWallMs, 0L);
    }
}
