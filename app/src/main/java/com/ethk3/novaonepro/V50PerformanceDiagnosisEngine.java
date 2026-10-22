package com.ethk3.novaonepro;

import android.database.Cursor;

import java.util.ArrayList;
import java.util.Collections;

/** Read-only inclusive/exclusive P50/P90/P95 performance report. */
public final class V50PerformanceDiagnosisEngine {
    public String diagnose(DbStore store, int limit) {
        if (store == null) return "V50_STORE_NULL";
        Cursor cursor = null;
        ArrayList<Row> rows = new ArrayList<Row>();
        try {
            cursor =
                    store.readable()
                            .query(
                                    SchemaContractRegistry.TABLE_PERFORMANCE,
                                    new String[] {
                                        "owner", "inclusive_nanos", "exclusive_nanos", "success"
                                    },
                                    null,
                                    null,
                                    null,
                                    null,
                                    "created_at DESC",
                                    String.valueOf(Math.max(1, limit)));
            while (cursor.moveToNext()) {
                rows.add(
                        new Row(
                                cursor.getString(0),
                                cursor.getLong(1),
                                cursor.getLong(2),
                                cursor.getInt(3) == 1));
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        if (rows.size() == 0) return "V50_PERFORMANCE_EMPTY";
        String[] owners = owners(rows);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < owners.length; i++) {
            ArrayList<Long> inclusive = new ArrayList<Long>();
            ArrayList<Long> exclusive = new ArrayList<Long>();
            int errors = 0;
            for (int j = 0; j < rows.size(); j++) {
                Row row = rows.get(j);
                if (!owners[i].equals(row.owner)) continue;
                inclusive.add(Long.valueOf(row.inclusive));
                exclusive.add(Long.valueOf(row.exclusive));
                if (!row.success) errors++;
            }
            Collections.sort(inclusive);
            Collections.sort(exclusive);
            if (out.length() > 0) out.append('\n');
            out.append(owners[i])
                    .append("|n=")
                    .append(inclusive.size())
                    .append("|P50/P90/P95=")
                    .append(ms(percentile(inclusive, 0.50d)))
                    .append('/')
                    .append(ms(percentile(inclusive, 0.90d)))
                    .append('/')
                    .append(ms(percentile(inclusive, 0.95d)))
                    .append("ms|exclusiveP95=")
                    .append(ms(percentile(exclusive, 0.95d)))
                    .append("ms|errors=")
                    .append(errors);
        }
        return out.toString();
    }

    private static String[] owners(ArrayList<Row> rows) {
        ArrayList<String> values = new ArrayList<String>();
        for (int i = 0; i < rows.size(); i++) {
            String owner = rows.get(i).owner;
            if (!values.contains(owner)) values.add(owner);
        }
        Collections.sort(values);
        return values.toArray(new String[values.size()]);
    }

    private static long percentile(ArrayList<Long> values, double p) {
        if (values.size() == 0) return 0L;
        int index = (int) Math.ceil(p * values.size()) - 1;
        index = Math.max(0, Math.min(values.size() - 1, index));
        return values.get(index).longValue();
    }

    private static long ms(long nanos) {
        return Math.round(nanos / 1000000.0d);
    }

    private static final class Row {
        final String owner;
        final long inclusive;
        final long exclusive;
        final boolean success;

        Row(String owner, long inclusive, long exclusive, boolean success) {
            this.owner = owner == null ? "" : owner;
            this.inclusive = inclusive;
            this.exclusive = exclusive;
            this.success = success;
        }
    }
}
