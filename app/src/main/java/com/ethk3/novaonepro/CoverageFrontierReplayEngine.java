package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/** Read-only settled OOS coverage frontier. Never changes a live gate. */
public final class CoverageFrontierReplayEngine {
    public static final String MODULE_VERSION = "V46_COVERAGE_FRONTIER_REPLAY_20260721_46072130";

    private CoverageFrontierReplayEngine() {}

    private static final class R {
        double p;
        int hit;
    }

    public static String build(DbStore store) {
        ArrayList<R> rows = new ArrayList<R>();
        Cursor c = null;
        try {
            if (store != null) {
                SQLiteDatabase db = store.readable();
                c =
                        db.query(
                                DbHelper.TABLE_OOS_ACTION_LOG,
                                new String[] {"honest_probability", "hit"},
                                "settled=1 AND sample_source=?",
                                new String[] {OosActionLogEngine.SOURCE_LIVE_REALTIME},
                                null,
                                null,
                                "target_issue DESC",
                                "4000");
                while (c != null && c.moveToNext()) {
                    R r = new R();
                    r.p = c.getDouble(0);
                    r.hit = c.getInt(1) == 1 ? 1 : 0;
                    rows.add(r);
                }
            }
        } catch (Throwable ignore) {
        } finally {
            if (c != null)
                try {
                    c.close();
                } catch (Throwable ignore) {
                }
        }
        Collections.sort(
                rows,
                new Comparator<R>() {
                    public int compare(R a, R b) {
                        return Double.compare(b.p, a.p);
                    }
                });
        int[] pct = new int[] {5, 10, 15, 20, 25};
        StringBuilder sb = new StringBuilder("V46覆盖率前沿");
        for (int k = 0; k < pct.length; k++) {
            int n = (int) Math.floor(rows.size() * pct[k] / 100.0d);
            if (n < 1 && rows.size() > 0) n = 1;
            int h = 0;
            double pnl = 0.0d;
            for (int i = 0; i < n && i < rows.size(); i++) {
                h += rows.get(i).hit;
                pnl += rows.get(i).hit == 1 ? AppConfig.WIN_NET_PROFIT : AppConfig.LOSE_NET_PROFIT;
            }
            double rate = n > 0 ? h / (double) n : 0.0d;
            sb.append("|")
                    .append(pct[k])
                    .append("%:n=")
                    .append(n)
                    .append(",rate=")
                    .append(rate)
                    .append(",pnl=")
                    .append(pnl);
        }
        try {
            if (store != null) {
                store.setState("v46_coverage_frontier_version", MODULE_VERSION);
                store.setState("v46_coverage_frontier", sb.toString());
            }
        } catch (Throwable ignore) {
        }
        return sb.toString();
    }
}
