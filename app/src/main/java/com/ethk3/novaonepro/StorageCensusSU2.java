package com.ethk3.novaonepro;

import java.io.File;
import java.util.*;

/**
 * Physical byte census of the whole app (P31). The 5.4.3 report measured only the
 * database (272,158,720 bytes) and WAL; it could not show where tens of GB were.
 * The census walks the app data directory (bounded: 200,000 entries, depth 12,
 * symlinks not followed), classifies every byte into the budget categories of
 * su2_storage_budget and is printed on the first page of every report and in the UI.
 */
public final class StorageCensusSU2 {
    private StorageCensusSU2() {}
    public static final String[] CATEGORIES = {"DATABASE", "WAL", "REPORTS", "REPORT_RECOVERY", "RESEARCH_CAS", "TRAINING_CAS", "MODEL_BUNDLES", "CACHE", "OTHER"};

    public static final class Census {
        public final Map<String, Long> bytes = new TreeMap<>(); public final List<String> largest = new ArrayList<>(); public long total; public int entries; public boolean truncated;
        public String line() {
            StringBuilder b = new StringBuilder("STORAGE_CENSUS_SU2|total=").append(total).append("|entries=").append(entries).append("|truncated=").append(truncated);
            for (Map.Entry<String, Long> e : bytes.entrySet()) b.append('|').append(e.getKey()).append('=').append(e.getValue());
            b.append("|largest=").append(String.join(";", largest)); return b.toString();
        }
    }

    public static String category(String relative) {
        String r = relative.replace('\\', '/');
        if (r.startsWith("databases/") && r.endsWith("-wal")) return "WAL";
        if (r.startsWith("databases/")) return "DATABASE";
        if (r.startsWith("files/r981_reports/recovery/") || r.startsWith("files/r981_reports/saf-parts/") || r.startsWith("files/r981_reports/failures/")) return "REPORT_RECOVERY";
        if (r.startsWith("files/r981_reports/") || r.startsWith("cache/r923_reports/")) return "REPORTS";
        if (r.startsWith("files/retention-cas-cp12/") || r.startsWith("files/retention-snapshot-r73/")) return "RESEARCH_CAS";
        if (r.startsWith("files/research-input-v2/")) return "TRAINING_CAS";
        if (r.startsWith("files/model-bundles-r929/")) return "MODEL_BUNDLES";
        if (r.startsWith("cache/") || r.startsWith("code_cache/")) return "CACHE";
        return "OTHER";
    }

    public static Census measure(File appDataDir) {
        Census c = new Census(); for (String k : CATEGORIES) c.bytes.put(k, 0L);
        PriorityQueue<long[]> top = new PriorityQueue<>(Comparator.comparingLong(a -> a[0]));
        List<String> names = new ArrayList<>();
        Deque<Object[]> stack = new ArrayDeque<>(); stack.push(new Object[]{appDataDir, "", 0});
        while (!stack.isEmpty()) {
            Object[] it = stack.pop(); File f = (File) it[0]; String rel = (String) it[1]; int depth = (Integer) it[2];
            File[] kids = f.listFiles(); if (kids == null) continue;
            for (File k : kids) {
                if (++c.entries > 200_000) { c.truncated = true; stack.clear(); break; }
                String r = rel.isEmpty() ? k.getName() : rel + "/" + k.getName();
                try { if (!k.getAbsoluteFile().equals(k.getCanonicalFile())) continue; } catch (java.io.IOException e) { continue; }
                if (k.isDirectory()) { if (depth < 12) stack.push(new Object[]{k, r, depth + 1}); continue; }
                long len = k.length(); String cat = category(r);
                c.bytes.put(cat, c.bytes.get(cat) + len); c.total += len;
                names.add(r); top.add(new long[]{len, names.size() - 1}); if (top.size() > 8) top.poll();
            }
        }
        List<long[]> t = new ArrayList<>(top); t.sort((a, b) -> Long.compare(b[0], a[0]));
        for (long[] x : t) c.largest.add(names.get((int) x[1]) + ":" + x[0]);
        return c;
    }
}
