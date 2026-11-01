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
        public final Map<String, Long> bytes = new TreeMap<>(), detailBytes=new TreeMap<>(); public final List<String> largest = new ArrayList<>(); public final Set<String> incompleteReasons=new TreeSet<>();public long total,sampleStartedAt,sampleFinishedAt; public int entries,excludedInstallationLinks; public boolean truncated;
        public String line() {
            StringBuilder b = new StringBuilder("STORAGE_CENSUS_SU2|total=").append(total).append("|entries=").append(entries).append("|truncated=").append(truncated);
            for (Map.Entry<String, Long> e : bytes.entrySet()) b.append('|').append(e.getKey()).append('=').append(e.getValue());
            for(Map.Entry<String,Long> e:detailBytes.entrySet())b.append("|detail.").append(e.getKey()).append('=').append(e.getValue());
            b.append("|sampleStartedAt=").append(sampleStartedAt).append("|sampleFinishedAt=").append(sampleFinishedAt).append("|complete=").append(!truncated).append("|incompleteReasons=").append(incompleteReasons).append("|excludedInstallationLinks=").append(excludedInstallationLinks);
            b.append("|owners=MAIN_DB:DatabaseCoordinator;BACKUP:offline-migration-with-verified-recovery;REPORT_SNAPSHOT:immutable-report-job-reference;ARCHIVE:reference-closure;MODEL:original-model-owner;CACHE:explicit-recomputable-only");
            b.append("|largest=").append(String.join(";", largest)); return b.toString();
        }
    }
    static String detail(String path){String p=path.toLowerCase(Locale.ROOT);
        if(p.contains("snapshot")&&p.endsWith(".db"))return "REPORT_OR_RETENTION_SNAPSHOT";
        if(p.contains("retired"))return "RETIRED_DATABASE_PROTECTED_UNTIL_COPY_PROOF";
        if(p.endsWith(".bak")||p.contains("backup")||p.contains(".old")||p.contains(".pre-"))return "OLD_BACKUP_PROTECTED_UNTIL_RECOVERY_PROOF";
        if(p.startsWith("databases/")&&p.endsWith("-wal"))return "MAIN_WAL";
        if(p.startsWith("databases/"))return "DATABASE_AND_SIDECARS";
        if(p.contains("model-bundles")||p.contains("training")||p.contains("research-input"))return "MODEL_AND_TRAINING_ORIGINALS";
        if(p.contains("cas-cp12")||p.contains("archive"))return "REFERENCED_ARCHIVES";
        if(p.contains("r981_reports"))return "REPORT_DELIVERY_AND_RECOVERY";
        if(p.startsWith("cache/")||p.startsWith("code_cache/"))return "CACHE_CLASSIFICATION_IS_NOT_DELETION_AUTHORITY";return "OTHER_PROTECTED_UNCLASSIFIED";
    }
    private static final class Entry{final String path;final long bytes;Entry(String p,long b){path=p;bytes=b;}}

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
        Census c = new Census();c.sampleStartedAt=System.currentTimeMillis(); for (String k : CATEGORIES) c.bytes.put(k, 0L);
        PriorityQueue<Entry> top = new PriorityQueue<>(8, Comparator.comparingLong(a -> a.bytes));
        Deque<Object[]> stack = new ArrayDeque<>(); stack.push(new Object[]{appDataDir, "", 0});
        while (!stack.isEmpty()) {
            Object[] it = stack.pop(); File f = (File) it[0]; String rel = (String) it[1]; int depth = (Integer) it[2];
            if(Thread.currentThread().isInterrupted()){c.truncated=true;c.incompleteReasons.add("CANCELLED");break;}
            File[] kids = f.listFiles(); if (kids == null){c.truncated=true;c.incompleteReasons.add("UNREADABLE_DIRECTORY");continue;}
            for (File k : kids) {
                if (++c.entries > 200_000) { c.truncated = true;c.incompleteReasons.add("ENTRY_BUDGET_200000"); stack.clear(); break; }
                String r = rel.isEmpty() ? k.getName() : rel + "/" + k.getName();
                try { if (!k.getAbsoluteFile().equals(k.getCanonicalFile())){if(r.equals("lib"))c.excludedInstallationLinks++;else{c.truncated=true;c.incompleteReasons.add("SYMLINK_NOT_FOLLOWED");}continue;} } catch (java.io.IOException e) {c.truncated=true;c.incompleteReasons.add("CANONICAL_PATH_UNRESOLVED");continue;}
                if (k.isDirectory()) { if (depth < 12) stack.push(new Object[]{k, r, depth + 1});else{c.truncated=true;c.incompleteReasons.add("DEPTH_BUDGET_12");} continue; }
                if(!k.isFile()){c.truncated=true;c.incompleteReasons.add("UNRESOLVED_FILE_TYPE");continue;}
                long len = k.length(); String cat = category(r);
                c.bytes.put(cat, c.bytes.get(cat) + len); c.total += len;
                c.detailBytes.merge(detail(r),len,Long::sum);top.add(new Entry(r,len)); if (top.size() > 8) top.poll();
            }
        }
        List<Entry> t = new ArrayList<>(top); t.sort((a, b) -> Long.compare(b.bytes,a.bytes));
        for (Entry x : t) c.largest.add(x.path+":"+x.bytes);c.sampleFinishedAt=System.currentTimeMillis();
        return c;
    }
    /** Immutable copy metadata, not physical app bytes and not a guessed largest-table scan. */
    public static Map<String,Object> sqliteSpace(android.database.sqlite.SQLiteDatabase db){Map<String,Object> out=new LinkedHashMap<>();out.put("sampleWallMs",System.currentTimeMillis());for(String key:new String[]{"page_count","page_size","freelist_count"})try(android.database.Cursor q=db.rawQuery("PRAGMA "+key,null)){out.put(key,q.moveToFirst()?q.getLong(0):null);}catch(RuntimeException error){out.put(key,null);out.put(key+"Error",error.getClass().getSimpleName());}if(out.get("page_count") instanceof Number&&out.get("page_size") instanceof Number&&out.get("freelist_count") instanceof Number){long size=((Number)out.get("page_size")).longValue();out.put("logicalPageBytes",Math.multiplyExact(size,((Number)out.get("page_count")).longValue()));out.put("freePageBytes",Math.multiplyExact(size,((Number)out.get("freelist_count")).longValue()));}out.put("scope","IMMUTABLE_REPORT_COPY; NOT_LIVE_FILE_OR_FREE_DISK; TABLE_PAYLOAD_DISTRIBUTION_UNKNOWN_WITHOUT_ORIGINAL_DBSTAT");return out;}
}
