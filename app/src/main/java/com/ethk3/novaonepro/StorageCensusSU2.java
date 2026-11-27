package com.ethk3.novaonepro;

import java.io.File;
import java.util.*;

/**
 * File-length and independently measured allocated-block census of the whole app (P31). The 5.4.3 report measured only the
 * database (272,158,720 bytes) and WAL; it could not show where tens of GB were.
 * The census walks the app data directory (bounded: 200,000 entries, depth 12,
 * symlinks not followed), classifies every byte into the budget categories of
 * su2_storage_budget and is printed on the first page of every report and in the UI.
 */
public final class StorageCensusSU2 {
    private StorageCensusSU2() {}
    public static final String[] CATEGORIES = {"DATABASE", "WAL", "SHM", "REPORTS", "REPORT_RECOVERY", "RESEARCH_CAS", "TRAINING_CAS", "MODEL_BUNDLES", "CACHE", "OTHER"};

    public static final class Census {
        public final Map<String, Long> bytes = new TreeMap<>(), detailBytes=new TreeMap<>(),scopeBytes=new TreeMap<>(); public final List<String> largest = new ArrayList<>(); public final Set<String> incompleteReasons=new TreeSet<>();public long total,allocatedFileAndDirectoryBytes;public boolean allocatedBytesKnown=true;public final Map<String,Long> allocatedCategories=new TreeMap<>(),allocatedScopeBytes=new TreeMap<>();public long sampleStartedAt,sampleFinishedAt,startedMonotonicNanos,finishedMonotonicNanos;public final String bootId=StorageBudgetR31.boot(); public int entries,excludedInstallationLinks; public boolean truncated;
        public final Map<String,Long> protectionBytes = new TreeMap<>();
        public String line() {
            StringBuilder b = new StringBuilder("STORAGE_CENSUS_SU2|total=").append(total).append("|entries=").append(entries).append("|truncated=").append(truncated);
            for (Map.Entry<String, Long> e : bytes.entrySet()) b.append('|').append(e.getKey()).append('=').append(e.getValue());
            for(Map.Entry<String,Long> e:detailBytes.entrySet())b.append("|detail.").append(e.getKey()).append('=').append(e.getValue());
            b.append("|classificationRegularBytes=").append(protectionBytes);
            b.append("|sampleStartedAt=").append(sampleStartedAt).append("|sampleFinishedAt=").append(sampleFinishedAt).append("|complete=").append(!truncated).append("|incompleteReasons=").append(incompleteReasons).append("|excludedInstallationLinks=").append(excludedInstallationLinks);
            b.append("|bootId=").append(bootId).append("|startedMonotonicNanos=").append(startedMonotonicNanos).append("|finishedMonotonicNanos=").append(finishedMonotonicNanos).append("|method=REGULAR_FILE_LENGTH|allocatedBlocksBytes="+(allocatedBytesKnown&&!truncated?allocatedFileAndDirectoryBytes:"UNKNOWN")+"|allocatedMethod=ANDROID_OS_STAT_ST_BLOCKS_TIMES_512_FILES_AND_DIRECTORIES|sampling=INTERVAL_NOT_ATOMIC_SNAPSHOT|memoryMetric=false");
            b.append("|owners=MAIN_DB:DatabaseCoordinator;BACKUP:offline-migration-with-verified-recovery;REPORT_SNAPSHOT:immutable-report-job-reference;ARCHIVE:reference-closure;MODEL:original-model-owner;CACHE:explicit-recomputable-only");
            b.append("|largest=").append(String.join(";", largest)); return b.toString();
        }
    }
    static String detail(String path){String p=path.toLowerCase(Locale.ROOT);
        if(p.contains("snapshot")&&p.endsWith(".db"))return "REPORT_OR_RETENTION_SNAPSHOT";
        if(p.contains("retired"))return "RETIRED_DATABASE_PROTECTED_UNTIL_COPY_PROOF";
        if(p.endsWith(".bak")||p.contains("backup")||p.contains(".old")||p.contains(".pre-"))return "OLD_BACKUP_PROTECTED_UNTIL_RECOVERY_PROOF";
        if(p.startsWith("databases/")&&p.endsWith("-wal"))return "MAIN_WAL";
        if(p.startsWith("databases/")&&p.endsWith("-shm"))return "MAIN_SHM";
        if(p.startsWith("databases/"))return "DATABASE_AND_SIDECARS";
        if(p.contains("model-bundles")||p.contains("training")||p.contains("research-input"))return "MODEL_AND_TRAINING_ORIGINALS";
        if(p.contains("cas-cp12")||p.contains("archive"))return "REFERENCED_ARCHIVES";
        if(p.contains("r981_reports"))return "REPORT_DELIVERY_AND_RECOVERY";
        if(p.startsWith("cache/")||p.startsWith("code_cache/"))return "CACHE_CLASSIFICATION_IS_NOT_DELETION_AUTHORITY";return "OTHER_PROTECTED_UNCLASSIFIED";
    }
    private static final class Entry{final String path;final long bytes;Entry(String p,long b){path=p;bytes=b;}}

    private static String protection(String path, String category) {
        if (path.startsWith("code_cache/") || path.startsWith("files/r335-prepared-canonical/")) return "RECOMPUTABLE";
        if (!"OTHER".equals(category) && !"CACHE".equals(category)) return "PROTECTED_ORIGINAL";
        return "UNKNOWN";
    }

    public static String category(String relative) {
        String r = relative.replace('\\', '/');
        if (r.startsWith("databases/") && r.endsWith("-wal")) return "WAL";
        if (r.startsWith("databases/") && r.endsWith("-shm")) return "SHM";
        if (r.startsWith("databases/")) return "DATABASE";
        if (r.startsWith("files/r981_reports/recovery/") || r.startsWith("files/r981_reports/saf-parts/") || r.startsWith("files/r981_reports/failures/")) return "REPORT_RECOVERY";
        if (r.startsWith("files/r981_reports/") || r.startsWith("cache/r923_reports/")) return "REPORTS";
        if (r.startsWith("files/retention-cas-cp12/") || r.startsWith("files/retention-snapshot-r73/")) return "RESEARCH_CAS";
        if (r.startsWith("files/research-input-v2/")) return "TRAINING_CAS";
        if (r.startsWith("files/model-bundles-r929/")) return "MODEL_BUNDLES";
        if (r.startsWith("cache/") || r.startsWith("code_cache/")) return "CACHE";
        return "OTHER";
    }

    public static Census measure(File appDataDir) {return measure(appDataDir,Collections.<String>emptyList());}
    /** Scope totals share each observed file length with the total, avoiding reservation double-counting. */
    public static Census measure(File appDataDir,Collection<String> scopes) {return measure(appDataDir,scopes,()->false);}
    public static Census measure(File appDataDir,Collection<String> scopes,java.util.function.BooleanSupplier cancelled) {
        if(cancelled==null)throw new IllegalArgumentException("STORAGE_CANCEL_REQUIRED");
        Census c = new Census();c.sampleStartedAt=System.currentTimeMillis();c.startedMonotonicNanos=System.nanoTime(); for (String k : CATEGORIES){c.bytes.put(k, 0L);c.allocatedCategories.put(k,0L);}
        if(scopes==null||scopes.size()>65)throw new IllegalArgumentException("STORAGE_SCOPE_BOUND");for(String scope:scopes){if(scope==null||scope.isEmpty()||scope.startsWith("/")||scope.contains("..")||scope.indexOf('\\')>=0)throw new IllegalArgumentException("STORAGE_SCOPE_PATH");c.scopeBytes.put(scope,0L);c.allocatedScopeBytes.put(scope,0L);}
        PriorityQueue<Entry> top = new PriorityQueue<>(8, Comparator.comparingLong(a -> a.bytes));
        File canonicalRoot;try{canonicalRoot=appDataDir.getCanonicalFile();}catch(java.io.IOException|SecurityException unresolved){c.truncated=true;c.incompleteReasons.add("ROOT_CANONICAL_PATH_UNRESOLVED");c.sampleFinishedAt=System.currentTimeMillis();c.finishedMonotonicNanos=System.nanoTime();return c;}
        recordAllocated(c,"",Allocated.bytes(canonicalRoot));
        final class Frame {final DirectoryCursorR31 cursor;final String relative;final int depth;Frame(File directory,String relative,int depth)throws java.io.IOException{cursor=DirectoryCursorR31.open(directory);this.relative=relative;this.depth=depth;}}
        Deque<Frame> stack=new ArrayDeque<>();
        try {
            stack.push(new Frame(canonicalRoot,"",0));
            while(!stack.isEmpty()) {
                if(Thread.currentThread().isInterrupted()||cancelled.getAsBoolean()){c.truncated=true;c.incompleteReasons.add("CANCELLED");break;}
                Frame frame=stack.peek();File k;
                try{k=frame.cursor.next();}catch(java.io.IOException unreadable){c.truncated=true;c.incompleteReasons.add("UNREADABLE_DIRECTORY:"+unreadable.getClass().getSimpleName());frame.cursor.close();stack.pop();continue;}
                if(k==null){frame.cursor.close();stack.pop();continue;}
                if(++c.entries>200000){c.truncated=true;c.incompleteReasons.add("ENTRY_BUDGET_200000");break;}
                String r=frame.relative.isEmpty()?k.getName():frame.relative+"/"+k.getName();
                try {
                    if(!k.getAbsoluteFile().equals(k.getCanonicalFile())){if(r.equals("lib"))c.excludedInstallationLinks++;else{c.truncated=true;c.incompleteReasons.add("SYMLINK_NOT_FOLLOWED");}continue;}
                    EntryStat stat=Allocated.readEntry(k);
                    // lstat supplies the type, length and blocks from one observation on Android.
                    // The canonical ancestor check above still applies to each entry.
                    if(stat!=null&&stat.kind==0120000){
                        c.truncated=true;c.incompleteReasons.add("SYMLINK_NOT_FOLLOWED");continue;
                    }
                    recordAllocated(c,r,stat==null?Allocated.bytes(k):stat.allocated);
                    if(stat==null?k.isDirectory():stat.kind==0040000){if(frame.depth>=12){c.truncated=true;c.incompleteReasons.add("DEPTH_BUDGET_12");}else stack.push(new Frame(k,r,frame.depth+1));continue;}
                    if(!(stat==null?k.isFile():stat.kind==0100000)){c.truncated=true;c.incompleteReasons.add("UNRESOLVED_FILE_TYPE");continue;}
                    long len=stat==null?k.length():stat.length;String cat=category(r);c.bytes.put(cat,Math.addExact(c.bytes.get(cat),len));c.total=Math.addExact(c.total,len);
                    c.protectionBytes.merge(protection(r,cat),len,Math::addExact);
                    for(String scope:c.scopeBytes.keySet())if(r.equals(scope)||r.startsWith(scope+"/"))c.scopeBytes.put(scope,Math.addExact(c.scopeBytes.get(scope),len));
                    c.detailBytes.merge(detail(r),len,Long::sum);top.add(new Entry(r,len));if(top.size()>8)top.poll();
                }catch(java.io.IOException|SecurityException unresolved){c.truncated=true;c.incompleteReasons.add("PATH_UNREADABLE:"+unresolved.getClass().getSimpleName());}
                catch(ArithmeticException overflow){c.truncated=true;c.incompleteReasons.add("BYTE_COUNT_OVERFLOW");break;}
            }
        }catch(java.io.IOException|SecurityException unavailable){c.truncated=true;c.incompleteReasons.add(String.valueOf(unavailable.getMessage()));}
        finally{while(!stack.isEmpty())try{stack.pop().cursor.close();}catch(java.io.IOException close){c.truncated=true;c.incompleteReasons.add("DIRECTORY_CLOSE_FAILED");}}
        List<Entry> t = new ArrayList<>(top); t.sort((a, b) -> Long.compare(b.bytes,a.bytes));
        for (Entry x : t) c.largest.add(x.path+":"+x.bytes);c.sampleFinishedAt=System.currentTimeMillis();c.finishedMonotonicNanos=System.nanoTime();
        return c;
    }
    /** Native kernel allocation on Android. Host/JDK absence is UNKNOWN, never invented as file length. */
    static long allocatedBytes(File file){return Allocated.bytes(file);}
    private static void recordAllocated(Census census,String relative,long bytes){
        if(bytes<0){census.allocatedBytesKnown=false;return;}
        try{census.allocatedFileAndDirectoryBytes=Math.addExact(census.allocatedFileAndDirectoryBytes,bytes);
            String category=relative.isEmpty()?"OTHER":category(relative);
            census.allocatedCategories.put(category,Math.addExact(census.allocatedCategories.get(category),bytes));
            for(String scope:census.allocatedScopeBytes.keySet())if(relative.equals(scope)||relative.startsWith(scope+"/"))census.allocatedScopeBytes.put(scope,Math.addExact(census.allocatedScopeBytes.get(scope),bytes));
        }catch(ArithmeticException overflow){census.allocatedBytesKnown=false;census.incompleteReasons.add("ALLOCATED_BYTES_OVERFLOW");}
    }
    private static final class EntryStat {
        final int kind;final long length,allocated;
        EntryStat(int kind,long length,long allocated){this.kind=kind;this.length=length;this.allocated=allocated;}
    }
    private static final class Allocated {
        static final java.lang.reflect.Method LSTAT;
        static final java.lang.reflect.Field MODE,SIZE;
        static final java.lang.reflect.Method STAT;static final java.lang.reflect.Field BLOCKS;
        static{java.lang.reflect.Method stat=null;java.lang.reflect.Field blocks=null;
            String vm=System.getProperty("java.vm.name","").toLowerCase(Locale.ROOT),runtime=System.getProperty("java.runtime.name","").toLowerCase(Locale.ROOT);
            if(vm.contains("dalvik")||runtime.contains("android"))try{stat=Class.forName("android.system.Os").getMethod("stat",String.class);blocks=Class.forName("android.system.StructStat").getField("st_blocks");}catch(ReflectiveOperationException|LinkageError|SecurityException unavailable){stat=null;blocks=null;}
            java.lang.reflect.Method lstat=null;java.lang.reflect.Field mode=null,size=null;
            if(stat!=null&&blocks!=null)try{
                mode=blocks.getDeclaringClass().getField("st_mode");
                size=blocks.getDeclaringClass().getField("st_size");
                lstat=stat.getDeclaringClass().getMethod("lstat",String.class);
            }catch(ReflectiveOperationException|SecurityException unavailable){lstat=null;}
            LSTAT=lstat;MODE=mode;SIZE=size;
            STAT=stat;BLOCKS=blocks;
        }
        static EntryStat readEntry(File file)throws java.io.IOException {
            if(LSTAT==null)return null;
            try{
                Object stat=LSTAT.invoke(null,file.getPath());
                long blocks=BLOCKS.getLong(stat);
                return new EntryStat(MODE.getInt(stat)&0170000,SIZE.getLong(stat),blocks<0?-1:Math.multiplyExact(blocks,512L));
            }catch(ReflectiveOperationException failure){throw new java.io.IOException("ENTRY_STAT_FAILED",failure);}
        }
        static long bytes(File file){if(STAT==null)return -1;try{long blocks=BLOCKS.getLong(STAT.invoke(null,file.getPath()));return blocks<0?-1:Math.multiplyExact(blocks,512L);}catch(ReflectiveOperationException|RuntimeException unavailable){return -1;}}
    }
    /** Immutable copy metadata, not physical app bytes and not a guessed largest-table scan. */
    public static void writeSqliteSpaceR342(android.database.sqlite.SQLiteDatabase copy,
            java.io.Writer out, android.os.CancellationSignal cancellation) throws java.io.IOException {
        Map<String,Object> meta=sqlitePageCountsR342(copy,cancellation);
        int total;
        try(android.database.Cursor count=copy.rawQuery("SELECT count(*) FROM sqlite_master WHERE rootpage>0",null,cancellation)){
            total=count.moveToFirst()?count.getInt(0):0;
        }
        Map<String,Long> bytesByTable=new TreeMap<>();
        int observed=0, measured=0;
        boolean dbstatSupported=true;
        try(android.database.Cursor probe=copy.rawQuery("SELECT name FROM dbstat LIMIT 1",null,cancellation)){
            probe.moveToFirst();
        }catch(android.database.sqlite.SQLiteException unavailable){
            dbstatSupported=false;
            meta.put("dbstatReason",unavailable.getClass().getSimpleName());
        }
        boolean aggregate=false;
        if(dbstatSupported)try(android.database.Cursor probe=copy.rawQuery(
                "SELECT aggregate FROM dbstat WHERE aggregate=1 LIMIT 0",null,cancellation)){
            probe.moveToFirst();aggregate=true;
        }catch(android.database.sqlite.SQLiteException legacy){/* SQLite versions before aggregate mode. */}
        out.write("{\"dbstatObjects\":[");
        String after="";
        while(observed<total){
            if(cancellation!=null)cancellation.throwIfCanceled();
            List<String[]> page=new ArrayList<>();
            try(android.database.Cursor objects=copy.rawQuery(
                    "SELECT name,tbl_name,type FROM sqlite_master WHERE rootpage>0 AND name COLLATE BINARY>? "
                    +"ORDER BY name COLLATE BINARY LIMIT 128",new String[]{after},cancellation)){
                while(objects.moveToNext())page.add(new String[]{objects.getString(0),objects.getString(1),objects.getString(2)});
            }
            if(page.isEmpty())break;
            Map<String,Long> pageBytes=new HashMap<>();
            if(dbstatSupported){
                List<String> names=new ArrayList<>(page.size());
                for(String[] object:page)names.add(object[0]);
                String placeholders=String.join(",",Collections.nCopies(names.size(),"?"));
                String sizesSql=aggregate?"SELECT name,pgsize FROM dbstat WHERE aggregate=1 AND name IN ("+placeholders+")"
                        :"SELECT name,sum(pgsize) FROM dbstat WHERE name IN ("+placeholders+") GROUP BY name";
                try(android.database.Cursor sizes=copy.rawQuery(sizesSql,names.toArray(new String[0]),cancellation)){
                    while(sizes.moveToNext())pageBytes.put(sizes.getString(0),sizes.getLong(1));
                }catch(android.database.sqlite.SQLiteException unavailable){
                    dbstatSupported=false;
                    meta.put("dbstatReason",unavailable.getClass().getSimpleName());
                }
            }
            for(String[] object:page){
                Map<String,Object> row=new LinkedHashMap<>();
                row.put("object",object[0]);row.put("ownerTable",object[1]);row.put("type",object[2]);
                Long physical=pageBytes.get(object[0]);
                row.put("btreePageBytes",physical);
                if(physical!=null){measured++;bytesByTable.merge(object[1],physical,Math::addExact);}
                if(observed++>0)out.write(',');
                out.write(com.ethk3.novaonepro.strategyv2.JsonR27.write(row));
                after=object[0];
            }
        }
        meta.put("objectDenominator",total);meta.put("objectsEnumerated",observed);
        meta.put("objectsWithMeasuredBytes",measured);
        meta.put("dbstatStatus",dbstatSupported&&measured==total&&observed==total
                ?"MEASURED_IMMUTABLE_COPY_BTREE_PAGES":"OBJECT_BYTES_PARTIAL_OR_UNAVAILABLE");
        meta.put("dbstatObjectCoverageComplete",dbstatSupported&&measured==total&&observed==total);
        meta.put("inventoryNameScope","SQLITE_MASTER_ROOTPAGE_OBJECTS;SQLITE_SCHEMA_METADATA_PAGE_EXCLUDED");
        meta.put("enumerationCursor",after);
        meta.put("measuredBytesByOwnerTable",bytesByTable);
        out.write("],");
        String summary=com.ethk3.novaonepro.strategyv2.JsonR27.write(meta);
        out.write(summary,1,summary.length()-1);
    }

    private static Map<String,Object> sqlitePageCountsR342(android.database.sqlite.SQLiteDatabase db,
            android.os.CancellationSignal cancellation){
        Map<String,Object> summary=new LinkedHashMap<>();
        summary.put("sampleWallMs",System.currentTimeMillis());
        for(String key:new String[]{"page_count","page_size","freelist_count"})
            try(android.database.Cursor row=db.rawQuery("PRAGMA "+key,null,cancellation)){
                summary.put(key,row.moveToFirst()?row.getLong(0):null);
            }catch(android.database.sqlite.SQLiteException unavailable){
                summary.put(key,null);
                summary.put(key+"Error",unavailable.getClass().getSimpleName());
            }
        if(summary.get("page_size") instanceof Number&&summary.get("page_count") instanceof Number
                &&summary.get("freelist_count") instanceof Number){
            long size=((Number)summary.get("page_size")).longValue();
            summary.put("logicalPageBytes",Math.multiplyExact(size,((Number)summary.get("page_count")).longValue()));
            summary.put("freePageBytes",Math.multiplyExact(size,((Number)summary.get("freelist_count")).longValue()));
        }
        summary.put("scope","IMMUTABLE_REPORT_COPY_BTREE;FREE_PAGES_AND_SIDECARS_SEPARATE");
        return summary;
    }

    public static Map<String,Object> sqliteSpace(android.database.sqlite.SQLiteDatabase db){return sqliteSpace(db,null);}
    public static Map<String,Object> sqliteSpace(android.database.sqlite.SQLiteDatabase db,android.os.CancellationSignal cancellation){Map<String,Object> out=new LinkedHashMap<>();out.put("sampleWallMs",System.currentTimeMillis());for(String key:new String[]{"page_count","page_size","freelist_count"})try(android.database.Cursor q=db.rawQuery("PRAGMA "+key,null,cancellation)){out.put(key,q.moveToFirst()?q.getLong(0):null);}catch(RuntimeException error){if(error instanceof android.os.OperationCanceledException)throw error;out.put(key,null);out.put(key+"Error",error.getClass().getSimpleName());}if(out.get("page_count") instanceof Number&&out.get("page_size") instanceof Number&&out.get("freelist_count") instanceof Number){long size=((Number)out.get("page_size")).longValue();out.put("logicalPageBytes",Math.multiplyExact(size,((Number)out.get("page_count")).longValue()));out.put("freePageBytes",Math.multiplyExact(size,((Number)out.get("freelist_count")).longValue()));}
        List<Map<String,Object>> pages=new ArrayList<>();try(android.database.Cursor q=db.rawQuery("SELECT name,SUM(pgsize) FROM dbstat GROUP BY name ORDER BY name LIMIT 1025",null,cancellation)){while(q.moveToNext()){if(pages.size()==1024){out.put("dbstatStatus","TRUNCATED_EXPLICIT_1024_OBJECT_LIMIT");break;}Map<String,Object> row=new LinkedHashMap<>();row.put("object",q.getString(0));row.put("btreePageBytes",q.getLong(1));pages.add(row);}if(!out.containsKey("dbstatStatus"))out.put("dbstatStatus","MEASURED_IMMUTABLE_COPY_BTREE_PAGES");out.put("dbstatObjects",pages);}catch(RuntimeException unavailable){if(unavailable instanceof android.os.OperationCanceledException)throw unavailable;out.put("dbstatStatus","UNSUPPORTED_OR_UNAVAILABLE");out.put("dbstatReason",unavailable.getClass().getSimpleName());out.put("dbstatObjects",null);}
        out.put("dbstatObjectCoverageComplete","MEASURED_IMMUTABLE_COPY_BTREE_PAGES".equals(out.get("dbstatStatus")));
        out.put("scope","IMMUTABLE_REPORT_COPY; NOT_LIVE_FILE_OR_FREE_DISK; DBSTAT_EXCLUDES_FREELIST_AND_SIDECARS");return out;}
}
