package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;

/**
 * Three boards (P03/P30): research (history replay), shadow (live frozen, not REAL) and
 * real (bound to the mainline). Every row shows N, rate, lower bound, frequency, longest
 * miss and the time range, so a figure measured on 72 periods can never look like one
 * measured on 3,000. The "display" caliber of the old reports (best of many on a short
 * window) is shown next to the forward caliber for the same strategies.
 */
public final class LeagueBoardSU2 {
    private LeagueBoardSU2() {}

    /** Dashboard entry: the board as of the newest frozen period. */
    public static String render(SQLiteDatabase db) {
        if (!StrategySchemaSU2.ready(db)) return "";
        try (Cursor c = db.rawQuery("SELECT coalesce(max(sequence_ordinal),0) FROM su2_live_decision WHERE hit IS NOT NULL", null)) {
            return render(db, c.moveToFirst() ? c.getLong(0) + 1 : 1);
        }
    }

    public static String render(SQLiteDatabase db, long seq) {
        if (!StrategySchemaSU2.ready(db)) return "SU2 未就绪\n";
        StringBuilder b = new StringBuilder("\n【策略联赛（SU2）】当前主链选择：").append(ModelStoreSU2.currentChoice(db, seq)).append('\n');
        long from = seq - MainlineBridgeSU2.WINDOW;
        for (String[] m : ModelStoreSU2.league(db)) {
            boolean[][] r = m[1].equals("SU2") || m[1].equals("XI_INCUMBENT") ? ModelStoreSU2.record(db, m[3], from, seq) : MainlineBridgeSU2.su1Record(db, m[0].substring(m[0].indexOf(':') + 1), from, seq);
            int n = 0, k = 0, run = 0, worst = 0, periods = 0;
            for (int t = 0; t < r[0].length; t++) { periods++; if (r[0][t]) { n++; if (r[1][t]) { k++; run = 0; } else { run++; worst = Math.max(worst, run); } } }
            double rate = n == 0 ? 0 : k / (double) n, lower = SelectiveEvidenceSU2.hoeffdingLower(k, n, 0.025);
            b.append(String.format(Locale.ROOT, "%s%-28s 实时影子 N=%d 命中=%d 胜率=%.2f%% 下界=%.2f%% 频率=%.1f%% 最长连挂=%d 区间=最近%d期%n",
                m[1].equals("XI_INCUMBENT") ? "★" : " ", m[2], n, k, rate * 100, lower * 100, 100.0 * n / Math.max(1, periods), worst, periods));
        }
        try (Cursor c = db.rawQuery("SELECT genome_key,acts,hits,lower_bound,coverage,issued_at,expires_at FROM su2_certificate ORDER BY issued_at DESC LIMIT 3", null)) {
            while (c.moveToNext()) b.append(String.format(Locale.ROOT, "证书 %s… N=%d 胜率=%.2f%% 下界=%.2f%% 频率=%.1f%%%n", c.getString(0).substring(0, 12), c.getInt(1), 100.0 * c.getInt(2) / Math.max(1, c.getInt(1)), c.getDouble(3) * 100, c.getDouble(4) * 100));
        }
        try (Cursor c = db.rawQuery("SELECT count(*),coalesce(sum(CASE WHEN state='LEAGUE' THEN 1 ELSE 0 END),0),coalesce(sum(CASE WHEN state='HALL_OF_FAME' THEN 1 ELSE 0 END),0) FROM su2_model_genome", null)) {
            if (c.moveToFirst()) b.append("已创造策略 ").append(c.getInt(0)).append("（联赛 ").append(c.getInt(1)).append("，名人堂 ").append(c.getInt(2)).append("）\n");
        }
        try (Cursor c = db.rawQuery("SELECT note FROM su2_lineage WHERE note<>'' LIMIT 3", null)) {
            while (c.moveToNext()) b.append("反馈修复：").append(c.getString(0)).append('\n');
        }
        return b.toString();
    }
}
