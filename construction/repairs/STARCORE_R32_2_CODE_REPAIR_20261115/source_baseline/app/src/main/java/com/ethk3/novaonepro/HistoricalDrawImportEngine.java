package com.ethk3.novaonepro;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/** One-time bounded import of the bundled strict historical seed. */
public final class HistoricalDrawImportEngine {
    public static final String MODULE_VERSION =
            V50BuildManifest.STRATEGY_VERSION + ":HISTORY_IMPORT";
    public static final String STATE_DONE = "v50_history_seed_import_done";
    public static final String LEGACY_STATE_DONE = "v49_history_seed_import_done";
    public static final String FIRST_SEED_ISSUE = "202603010251";
    public static final String LAST_SEED_ISSUE = "202603110295";
    public static final int EXPECTED_SEED_ROWS = 10000;
    public static final int V50_RUNTIME_MINIMUM_ROWS = 96;
    // AGP expands the source .gz asset while merging assets into the APK.
    static final String SEED_ASSET_PATH = "history_seed.csv";
    private static final int BATCH = 250;
    private static final Object IMPORT_LOCK = new Object();

    public Report importIfNeeded(Context context, DbStore store) {
        synchronized (IMPORT_LOCK) {
            return importLocked(context, store);
        }
    }

    private Report importLocked(Context context, DbStore store) {
        Report r = new Report();
        if (context == null || store == null) {
            r.status = "INVALID_INPUT";
            return r;
        }
        r.before = store.getValidDrawCount();
        boolean boundariesReady =
                store.getDrawByIssue(FIRST_SEED_ISSUE) != null
                        && store.getDrawByIssue(LAST_SEED_ISSUE) != null;
        if ("1".equals(store.getState(STATE_DONE, "0"))
                && boundariesReady
                && r.before >= V50_RUNTIME_MINIMUM_ROWS) {
            r.status = "ALREADY_DONE";
            r.after = r.before;
            r.read = EXPECTED_SEED_ROWS;
            return r;
        }
        InputStream raw = null;
        BufferedReader br = null;
        try {
            raw = context.getAssets().open(SEED_ASSET_PATH);
            br = seedReader(raw);
            String line = br.readLine();
            ArrayList<Models.Draw> batch = new ArrayList<Models.Draw>();
            while ((line = br.readLine()) != null) {
                Models.Draw d = parse(line);
                if (d == null) continue;
                batch.add(d);
                r.read++;
                if (batch.size() >= BATCH) {
                    r.inserted += store.saveDraws(batch);
                    batch.clear();
                    try {
                        Thread.sleep(8L);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw interrupted;
                    }
                }
            }
            if (batch.size() > 0) r.inserted += store.saveDraws(batch);
            r.after = store.getValidDrawCount();
            boolean firstReady = store.getDrawByIssue(FIRST_SEED_ISSUE) != null;
            boolean lastReady = store.getDrawByIssue(LAST_SEED_ISSUE) != null;
            if (r.read != EXPECTED_SEED_ROWS)
                throw new IllegalStateException("SEED_ROW_COUNT_" + r.read);
            if (!firstReady || !lastReady) throw new IllegalStateException("SEED_BOUNDARY_MISSING");
            if (r.after < V50_RUNTIME_MINIMUM_ROWS)
                throw new IllegalStateException("HISTORY_LT_" + V50_RUNTIME_MINIMUM_ROWS);
            r.status = "OK";
            store.setState(STATE_DONE, "1");
            store.setState(LEGACY_STATE_DONE, "1");
            store.setState("v50_history_seed_import_summary", r.summary());
            store.setState("v50_history_seed_import_version", MODULE_VERSION);
            store.setState("v49_history_seed_import_summary", r.summary());
            store.setState("v49_history_seed_import_version", MODULE_VERSION);
        } catch (Throwable e) {
            r.after = store.getValidDrawCount();
            r.status = "ERROR:" + e.getClass().getSimpleName() + ":" + safe(e.getMessage());
            try {
                store.setState("v50_history_seed_import_summary", r.summary());
                store.setState("v50_history_seed_import_version", MODULE_VERSION);
                store.setState(STATE_DONE, "0");
            } catch (Throwable ignore) {
            }
        } finally {
            try {
                if (br != null) br.close();
                else if (raw != null) raw.close();
            } catch (Throwable ignore) {
            }
        }
        return r;
    }

    static BufferedReader seedReader(InputStream raw) throws IOException {
        return new BufferedReader(new InputStreamReader(raw, "UTF-8"), 32768);
    }

    private static Models.Draw parse(String line) {
        if (line == null || line.length() == 0) return null;
        ArrayList<String> f = csv(line);
        if (f.size() < 8 || !"1".equals(f.get(6))) return null;
        String[] nums = f.get(3).split(",");
        if (nums.length != 10) return null;
        Models.Draw d = new Models.Draw();
        d.issue = f.get(0);
        d.openTime = longValue(f.get(1));
        d.fetchTime = longValue(f.get(7));
        d.sourceLine = "V49_BUNDLED_HISTORY_SEED";
        d.rawJson = "";
        for (int i = 0; i < 10; i++) d.numbers[i] = intValue(nums[i]);
        d.normalizeDirty();
        return d.isValid() ? d : null;
    }

    private static ArrayList<String> csv(String line) {
        ArrayList<String> out = new ArrayList<String>();
        StringBuilder b = new StringBuilder();
        boolean q = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (q && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    b.append('"');
                    i++;
                } else q = !q;
            } else if (c == ',' && !q) {
                out.add(b.toString());
                b.setLength(0);
            } else b.append(c);
        }
        out.add(b.toString());
        return out;
    }

    private static int intValue(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Throwable e) {
            return 0;
        }
    }

    private static long longValue(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (Throwable e) {
            return 0L;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace('|', '/');
    }

    public static final class Report {
        public String status = "NOT_RUN";
        public int before;
        public int read;
        public int inserted;
        public int after;

        public boolean ready() {
            return after >= V50_RUNTIME_MINIMUM_ROWS && status.indexOf("ERROR") != 0;
        }

        public String summary() {
            return "status="
                    + status
                    + "|before="
                    + before
                    + "|read="
                    + read
                    + "|inserted="
                    + inserted
                    + "|after="
                    + after
                    + "|ready="
                    + (ready() ? 1 : 0);
        }
    }
}
