package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;
import com.ethk3.novaonepro.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** R27-003/005/006/007/009 research thread pool (docs/02 2.2). max(1,min(4,cores−2)) worker threads at
 * THREAD_PRIORITY_BACKGROUND+1, each with its own read-only WAL connection (PRAGMA query_only=1). Workers never
 * look at the realtime-pressure gate while computing; only writes yield (ResearchWriteQueueR27). Session-level
 * blocking exists only for real resources (memory, thermal) and user stop. Inside the last 3 s before a freeze
 * deadline the pool runs with one active worker, never zero. Every slice records a wait kind (WaitKindR27) on its
 * r26_task row (lane 'R27'); errors keep their full stack in r27_task_error and quarantine after 5 per 10 minutes. */
public final class ResearchPoolR27 implements AutoCloseable {
    public interface Task {
        String id();
        /** One bounded slice on the calling worker thread. */
        Outcome step(Worker w) throws Exception;
    }
    public static final class Outcome {
        public final long units; public final String reason; public final long cursor, latest;
        public Outcome(long units, String reason, long cursor, long latest) { this.units = units; this.reason = reason; this.cursor = cursor; this.latest = latest; }
        public static Outcome progress(long units, long cursor) { return new Outcome(units, null, cursor, -1); }
        public static Outcome idle(String reason, long cursor, long latest) { return new Outcome(0, reason, cursor, latest); }
    }
    /** Per-thread context: read-only connection and cancellation. */
    public final class Worker {
        public final SQLiteDatabase ro; public final String name;
        Worker(SQLiteDatabase ro, String name) { this.ro = ro; this.name = name; }
        public boolean cancelled() { return closed || Thread.currentThread().isInterrupted(); }
        public ResearchWriteQueueR27 writes() { return writes; }
        public DbStore store() { return store; }
        public ResearchPoolR27 pool() { return ResearchPoolR27.this; }
    }

    private final DbStore store; private final String session; private final ResearchWriteQueueR27 writes;
    private final List<Task> tasks = new ArrayList<>(); private final Map<String, Boolean> running = new HashMap<>();
    private final Map<String, Long> notBefore = new HashMap<>();
    private final List<Thread> threads = new ArrayList<>(); private final List<SQLiteDatabase> connections = new ArrayList<>();
    private volatile boolean closed; private volatile String resource;
    private volatile int parallelismLimit = 2; private volatile String thermalReason = "NORMAL";
    private final AtomicInteger active = new AtomicInteger();
    public final int size;
    private final ExecutorService compute;

    public static final int BACKGROUND_PRIORITY = android.os.Process.THREAD_PRIORITY_BACKGROUND + 1;
    public static int defaultThreads() { int cores = Runtime.getRuntime().availableProcessors(); return Math.max(1, Math.min(2, cores - 1)); }

    public ResearchPoolR27(DbStore store, String session, int threads) {
        this.store = store; this.session = session; this.size = Math.max(1, threads); this.writes = new ResearchWriteQueueR27(store);
        // Shared helper executor for data-parallel replay inside one task (evolution REPLAY step).
        // R27.2: the helpers run at the workers' background priority. They used to inherit the default priority and,
        // with up to four of them replaying at once, took the CPU from the realtime analysis thread on device.
        compute = new ThreadPoolExecutor(size, size, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), r -> { Thread t = new Thread(() -> { try { android.os.Process.setThreadPriority(BACKGROUND_PRIORITY); } catch (Throwable ignored) { } r.run(); }, "starcore-r27-compute"); t.setDaemon(true); return t; });
    }
    public ResearchWriteQueueR27 writes() { return writes; }
    public ExecutorService compute() { return compute; }
    public synchronized void register(Task t) { tasks.add(t); running.put(t.id(), false); }
    public void setResource(String r) { resource = r; }
    public void setThermalLimit(int workers, String reason) { parallelismLimit = Math.max(1, Math.min(size, workers)); thermalReason = reason; }
    public int workerLimit() { return parallelismLimit; }
    public String thermalReason() { return thermalReason; }

    public void start() {
        String path = store.contextForR26().getDatabasePath(AppConfig.DB_NAME).getPath();
        for (int i = 0; i < size; i++) {
            SQLiteDatabase ro = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY);
            try (Cursor c = ro.rawQuery("PRAGMA query_only=1", null)) { c.moveToFirst(); } catch (RuntimeException ignored) { }
            connections.add(ro);
            final Worker w = new Worker(ro, "starcore-r27-research-" + i);
            Thread t = new Thread(() -> loop(w), w.name); t.setDaemon(true); threads.add(t); t.start();
        }
    }

    private void loop(Worker w) {
        android.os.Process.setThreadPriority(BACKGROUND_PRIORITY);
        long cpuStart = android.os.Debug.threadCpuTimeNanos(), wallStart = SystemClock.elapsedRealtime(), flushed = wallStart;
        long slices = 0;
        while (!closed) {
            try {
                if (resource != null) { sleep(500); continue; }
                // Freeze guard: the last 3 s before a deadline run with one active research worker (not zero).
                // R27.2: the same one-worker floor while P0 analysis/settlement runs (device: request→commit P95 20.5 s with
                // MODEL_BUNDLE_OPEN 10.6 s wall / 0.13 s CPU while every worker ran). Research never drops to zero.
                int limit = narrow(System.currentTimeMillis()) ? 1 : Math.min(size, parallelismLimit);
                if (!ResearchSlotsR29.tryEnter(active, limit)) { sleep(100); continue; }
                Task t;
                try { t = next(); } catch (Throwable failure) { active.decrementAndGet(); throw failure; }
                if (t == null) { active.decrementAndGet(); sleep(250); continue; }
                Outcome o = null; Throwable failure = null;
                boolean quarantined = false;
                try {
                    try (Cursor c = w.ro.rawQuery("SELECT status FROM r26_task WHERE task_id=?", new String[] {t.id()})) {
                        quarantined = c.moveToFirst() && WaitKindR27.QUARANTINED.equals(c.getString(0));
                    }
                    if (!quarantined) o = t.step(w);
                } catch (Throwable e) { failure = e; }
                finally { active.decrementAndGet(); synchronized (this) { running.put(t.id(), false); } }
                if (quarantined) { synchronized (this) { notBefore.put(t.id(), System.currentTimeMillis() + 5000); } continue; }
                slices++;
                record(t.id(), o, failure);
                if (o == null || o.units == 0) synchronized (this) { notBefore.put(t.id(), System.currentTimeMillis() + (failure != null ? 5000 : 1000)); }
            } catch (Throwable unexpected) { android.util.Log.w("STARCORE_R27", "worker " + w.name, unexpected); sleep(500); }
            long now = SystemClock.elapsedRealtime();
            if (now - flushed > 15_000) { flushCpu(w.name, android.os.Debug.threadCpuTimeNanos() - cpuStart, now - wallStart, slices); flushed = now; }
        }
    }

    /** Freeze window or active P0 work: the pool runs with one worker (R27.2). */
    boolean narrow(long now) { return parallelismLimit == 1 || writes.freezeWindow(now) || ResearchWriteQueueR27.p0Writing(); }

    private synchronized Task next() {
        long now = System.currentTimeMillis();
        for (Task t : tasks) {
            if (Boolean.TRUE.equals(running.get(t.id()))) continue;
            Long nb = notBefore.get(t.id()); if (nb != null && nb > now) continue;
            running.put(t.id(), true); notBefore.remove(t.id());
            // Round-robin fairness: move the claimed task to the end.
            tasks.remove(t); tasks.add(t);
            return t;
        }
        return null;
    }
    public synchronized void wake(String id) { notBefore.remove(id); }

    private void record(String id, Outcome o, Throwable failure) {
        WaitKindR27.Wait wait = WaitKindR27.classify(o == null ? 0 : o.units, o == null ? null : o.reason, failure, o == null ? 0 : o.cursor, o == null ? -1 : o.latest, resource, false);
        final long now = System.currentTimeMillis(); final String stack = failure == null ? null : stack(failure);
        try {
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R27_TASK_RECEIPT", db -> {
                db.execSQL("INSERT OR IGNORE INTO r26_task(task_id,lane,mechanism,status,registered_at) VALUES(?,'R27',?,'READY',?)", new Object[] {id, id, now});
                if (failure != null) {
                    long ws = 0, cnt = 0;
                    try (Cursor c = db.rawQuery("SELECT error_window_start,error_count FROM r26_task WHERE task_id=?", new String[] {id})) { if (c.moveToFirst()) { ws = c.getLong(0); cnt = c.getLong(1); } }
                    long[] e = WaitKindR27.recordError(ws, cnt, now);
                    boolean q = WaitKindR27.quarantine(e[1]);
                    db.execSQL("INSERT INTO r27_task_error(task_id,error_class,message,stack,recorded_at) VALUES(?,?,?,?,?)", new Object[] {id, failure.getClass().getName(), String.valueOf(failure.getMessage()), stack, now});
                    db.execSQL("DELETE FROM r27_task_error WHERE error_id<(SELECT max(error_id)-2000 FROM r27_task_error)");
                    db.execSQL("UPDATE r26_task SET error_window_start=?,error_count=?,status=?,wait_kind=?,wait_detail=?,blocked_reason=?,wait_since=CASE WHEN wait_kind=? THEN wait_since ELSE ? END,last_served=? WHERE task_id=?",
                            new Object[] {e[0], e[1], q ? WaitKindR27.QUARANTINED : "BLOCKED", q ? WaitKindR27.QUARANTINED : wait.kind, wait.detail, wait.blockedReason(), wait.kind, now, now, id});
                } else {
                    boolean progress = wait.kind.equals(WaitKindR27.PROGRESS);
                    db.execSQL("UPDATE r26_task SET status=?,wait_kind=?,wait_detail=?,blocked_reason=?,wait_since=CASE WHEN wait_kind=? THEN wait_since ELSE ? END,last_served=?,"
                            + "cursor=CASE WHEN ?>cursor THEN ? ELSE cursor END,last_progress=CASE WHEN ? THEN ? ELSE last_progress END,steps=steps+?,first_progress_at=CASE WHEN first_progress_at=0 AND ? THEN ? ELSE first_progress_at END WHERE task_id=? AND status<>?",
                            new Object[] {progress ? "YIELDED" : "BLOCKED", wait.kind, wait.detail, wait.blockedReason(), wait.kind, now, now,
                                    o.cursor, o.cursor, progress ? 1 : 0, now, progress ? 1 : 0, progress ? 1 : 0, now, id, WaitKindR27.QUARANTINED});
                }
                return 0;
            });
        } catch (RuntimeException busy) { android.util.Log.w("STARCORE_R27", "task receipt deferred " + id, busy); }
        if (failure != null) android.util.Log.w("STARCORE_R27", id, failure);
    }

    /** Manual retry from the UI: clears a quarantine. */
    public static void retry(DbStore store, String taskId) {
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R27_TASK_RETRY", db -> { db.execSQL("UPDATE r26_task SET status='READY',wait_kind='',error_count=0,error_window_start=0 WHERE task_id=? AND lane='R27'", new Object[] {taskId}); return 0; });
    }
    public boolean quarantined(String id) {
        try (Cursor c = store.readable().rawQuery("SELECT 1 FROM r26_task WHERE task_id=? AND status=?", new String[] {id, WaitKindR27.QUARANTINED})) { return c.moveToFirst(); }
        catch (RuntimeException e) { return false; }
    }

    private void flushCpu(String thread, long cpuNs, long wallMs, long slices) {
        final long cpu = cpuNs / 1_000_000L, now = System.currentTimeMillis();
        try {
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R27_THREAD_CPU", db -> {
                db.execSQL("INSERT OR IGNORE INTO r27_thread_cpu(session_id,thread_name,updated_at) VALUES(?,?,?)", new Object[] {session, thread, now});
                db.execSQL("UPDATE r27_thread_cpu SET cpu_ms=?,wall_ms=?,slices=?,updated_at=? WHERE session_id=? AND thread_name=?", new Object[] {cpu, wallMs, slices, now, session, thread});
                return 0;
            });
        } catch (RuntimeException busy) { android.util.Log.w("STARCORE_R27", "cpu receipt deferred", busy); }
        writes.flush();
    }

    static String stack(Throwable t) { StringWriter s = new StringWriter(); t.printStackTrace(new PrintWriter(s)); String v = s.toString(); return v.length() > 16000 ? v.substring(0, 16000) : v; }
    private static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }

    /** Cancel token first, then wait up to 10 s for workers, then close read-only connections (R27-009). */
    @Override public void close() {
        closed = true;
        for (Thread t : threads) t.interrupt();
        long deadline = SystemClock.elapsedRealtime() + 10_000;
        for (Thread t : threads) { long left = deadline - SystemClock.elapsedRealtime(); if (left > 0) try { t.join(left); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
        compute.shutdownNow();
        for (SQLiteDatabase c : connections) try { c.close(); } catch (RuntimeException ignored) { }
        connections.clear();
    }
    public boolean isClosed() { return closed; }
    public String session() { return session; }
}
