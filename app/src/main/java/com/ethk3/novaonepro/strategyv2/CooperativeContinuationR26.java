package com.ethk3.novaonepro.strategyv2;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;

/** A private computation parked outside DB critical sections. Durable progress is its checkpoint.
 * Cancellation requests do not prove termination and never return a compute permit. */
public final class CooperativeContinuationR26<T> implements AutoCloseable, BudgetTokenSU1.PauseClock {
    public static final long SLICE_MS = 1000;
    /** Accepted continuation quantum; SLICE_MS is the enclosing service budget. */
    public static final long CONTINUATION_SLICE_MS = 200;
    public static volatile Runnable threadStartHook;

    public enum State { PAUSED, RUNNING, CANCELLING, TERMINATED }
    /** Implementations must make close idempotent; only the observed dead worker returns this permit. */
    public interface ComputePermit extends AutoCloseable { @Override void close(); }
    public interface PermitProvider { ComputePermit tryAcquire(String owner, String workName); }
    public static final class CapacityUnavailable extends IllegalStateException {
        public CapacityUnavailable() { super("R31_COMPUTE_CAPACITY_WAIT"); }
    }
    private static volatile PermitProvider permitProvider;
    private static final ComputePermit UNMETERED_HOST_PERMIT = () -> { };
    private static final ThreadLocal<CooperativeContinuationR26<?>> CURRENT = new ThreadLocal<>();
    private static final Map<String, Set<CooperativeContinuationR26<?>>> OWNED = new HashMap<>();

    /** Android startup installs ResearchPoolR27's one static controller before creating any worker.
     * The uninstalled path is retained for existing standalone numerical/host callers. */
    public static void installPermitProvider(PermitProvider provider) { permitProvider = Objects.requireNonNull(provider); }
    public static boolean hasPermitProvider() { return permitProvider != null; }

    private final String owner;
    private final Object monitor = new Object();
    private final Callable<T> body;
    private final BooleanSupplier preempt;
    private final Runnable resume;
    private final LongSupplier cpuClock;
    private final Thread thread;
    private final ComputePermit computePermit;
    private final AtomicBoolean permitReleased = new AtomicBoolean();
    private final AtomicBoolean slicing = new AtomicBoolean();
    private volatile boolean closed, done, paused = true, started;
    private volatile long endNanos, pausedSince, totalPaused, cpuTotal, cpuOrigin, maxPollGap, lastPoll, lastCheckpoint;
    private volatile int criticalDepth;
    private volatile Throwable error;
    private volatile T result;
    private volatile String phase = "CREATED", checkpoint = "NONE", cleanupFailure = "";

    public static final class Slice<T> {
        public final boolean done;
        public final T result;
        public final Throwable error;
        public final long cpuNanos, maxPollGapNanos;
        public final State state;
        Slice(boolean done, T result, Throwable error, long cpu, long gap, State state) {
            this.done = done; this.result = result; this.error = error;
            cpuNanos = cpu; maxPollGapNanos = gap; this.state = state;
        }
    }
    public static final class DiagnosticSnapshot {
        public final String owner, workName, phase, checkpoint, threadState, cleanupFailure;
        public final State state;
        public final long observedAtNanos, lastPollNanos, maxPollGapNanos, lastCheckpointNanos, cpuNanos;
        public final int criticalDepth;
        public final boolean resultReady;
        public final List<String> stack;
        private DiagnosticSnapshot(CooperativeContinuationR26<?> work) {
            observedAtNanos = System.nanoTime();
            owner = work.owner; workName = work.thread.getName(); phase = work.phase; checkpoint = work.checkpoint;
            state = work.state(); threadState = work.thread.getState().name(); resultReady = work.done;
            lastPollNanos = work.lastPoll; maxPollGapNanos = work.maxPollGap;
            lastCheckpointNanos = work.lastCheckpoint; cpuNanos = work.cpuTotal;
            criticalDepth = work.criticalDepth; cleanupFailure = work.cleanupFailure;
            List<String> frames = new ArrayList<>();
            StackTraceElement[] trace = work.thread.getStackTrace();
            for (int i = 0; i < Math.min(16, trace.length); i++) frames.add(bounded(trace[i].toString(), 256));
            stack = Collections.unmodifiableList(frames);
        }
        public Map<String,Object> asMap() {
            Map<String,Object> out = new LinkedHashMap<>();
            out.put("owner", owner); out.put("workName", workName); out.put("state", state.name());
            out.put("phase", phase); out.put("checkpoint", checkpoint); out.put("threadState", threadState);
            out.put("observedAtNanos", observedAtNanos); out.put("clock", "PROCESS_MONOTONIC_NS");
            out.put("lastPollNanos", lastPollNanos); out.put("lastPollAgeNanos", lastPollNanos == 0 ? null : Math.max(0, observedAtNanos - lastPollNanos));
            out.put("maxPollGapNanos", maxPollGapNanos); out.put("lastCheckpointNanos", lastCheckpointNanos);
            out.put("checkpointEvidence", lastCheckpointNanos == 0 ? "NOT_RECORDED" : "DURABLE_CHECKPOINT_RETURNED_SUCCESS");
            out.put("cpuNanos", cpuNanos); out.put("criticalDepth", criticalDepth); out.put("resultReady", resultReady);
            out.put("cleanupFailure", cleanupFailure); out.put("stack", stack);
            return Collections.unmodifiableMap(out);
        }
    }
    public CooperativeContinuationR26(String name, Callable<T> body, BooleanSupplier preempt, Runnable resume, LongSupplier cpuClock) {
        this(name, body, preempt, resume, cpuClock, "");
    }
    public CooperativeContinuationR26(String name, Callable<T> body, BooleanSupplier preempt, Runnable resume, LongSupplier cpuClock, String owner) {
        this.owner = Objects.requireNonNull(owner); this.body = Objects.requireNonNull(body);
        this.preempt = preempt; this.resume = resume; this.cpuClock = Objects.requireNonNull(cpuClock);
        Objects.requireNonNull(name);
        reapTerminated();
        thread = new Thread(this::run, name); thread.setDaemon(true);
        PermitProvider provider = permitProvider;
        computePermit = provider == null ? UNMETERED_HOST_PERMIT : provider.tryAcquire(owner, name);
        if (computePermit == null) throw new CapacityUnavailable();
        pausedSince = System.nanoTime();
        synchronized (OWNED) {
            OWNED.computeIfAbsent(owner, key -> new HashSet<>()).add(this);
            try { thread.start(); started = true; }
            catch (RuntimeException | Error failed) {
                OWNED.get(owner).remove(this); if (OWNED.get(owner).isEmpty()) OWNED.remove(owner);
                try { computePermit.close(); } catch (Throwable cleanup) { failed.addSuppressed(cleanup); }
                throw failed;
            }
        }
    }
    /** No helper thread is spawned to reap helpers. Runtime ticks, new work and bounded joins reap them. */
    public static void reapTerminated() {
        List<CooperativeContinuationR26<?>> finished = new ArrayList<>();
        synchronized (OWNED) {
            for (Set<CooperativeContinuationR26<?>> group : OWNED.values())
                for (CooperativeContinuationR26<?> work : group) if (work.terminated()) finished.add(work);
        }
        for (CooperativeContinuationR26<?> work : finished) {
            if (!work.permitReleased.compareAndSet(false, true)) continue;
            try { work.computePermit.close(); }
            catch (Throwable failed) {
                work.cleanupFailure = failed.getClass().getName(); work.permitReleased.set(false); continue;
            }
            synchronized (OWNED) {
                Set<CooperativeContinuationR26<?>> group = OWNED.get(work.owner);
                if (group != null) { group.remove(work); if (group.isEmpty()) OWNED.remove(work.owner); }
            }
        }
    }
    private boolean terminated() { return started && !thread.isAlive(); }
    public boolean isTerminated() { boolean stopped = terminated(); if (stopped) reapTerminated(); return stopped; }
    public boolean cancellationRequested(){return closed;}
    public static boolean requestTaskCancellationR31(String owner,String workName){boolean found=false;for(CooperativeContinuationR26<?> work:owned(owner))if(work.thread.getName().equals(workName)){found=true;work.close();}return found;}
    public static boolean isTaskTerminatedR31(String owner,String workName){for(CooperativeContinuationR26<?> work:owned(owner))if(work.thread.getName().equals(workName)&&!work.terminated())return false;return true;}
    public State state() { return observedState(); }
    private State observedState() {
        if (terminated()) return State.TERMINATED;
        if (closed) return State.CANCELLING;
        return paused && !done ? State.PAUSED : State.RUNNING;
    }
    public DiagnosticSnapshot snapshot() { return new DiagnosticSnapshot(this); }
    public static List<DiagnosticSnapshot> snapshots(String owner) {
        List<CooperativeContinuationR26<?>> live = owned(owner);
        List<DiagnosticSnapshot> out = new ArrayList<>();
        for (int i = 0; i < Math.min(64, live.size()); i++) out.add(live.get(i).snapshot());
        return Collections.unmodifiableList(out);
    }
    private static List<CooperativeContinuationR26<?>> owned(String owner) {
        reapTerminated();
        synchronized (OWNED) { Set<CooperativeContinuationR26<?>> group = OWNED.get(owner); return group == null ? Collections.emptyList() : new ArrayList<>(group); }
    }
    public boolean awaitTermination(long millis) {
        if (millis < 0) throw new IllegalArgumentException("R31_NEGATIVE_TERMINATION_WAIT");
        if (Thread.currentThread() == thread) return false;
        long start = System.nanoTime(), timeout = TimeUnit.MILLISECONDS.toNanos(millis);
        while (thread.isAlive()) {
            long left = timeout - (System.nanoTime() - start); if (left <= 0) return false;
            try { TimeUnit.NANOSECONDS.timedJoin(thread, left); }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); return false; }
        }
        reapTerminated(); return true;
    }
    public static boolean awaitOwnerStopped(String owner, long millis) {
        if (millis < 0) throw new IllegalArgumentException("R31_NEGATIVE_TERMINATION_WAIT");
        long start = System.nanoTime(), timeout = TimeUnit.MILLISECONDS.toNanos(millis);
        for (;;) {
            List<CooperativeContinuationR26<?>> tasks = owned(owner); if (tasks.isEmpty()) return true;
            boolean allStopped = true; for (CooperativeContinuationR26<?> task : tasks) allStopped &= task.terminated();
            if (allStopped) return true;
            for (CooperativeContinuationR26<?> task : tasks) {
                if (task.terminated()) continue;
                long left = timeout - (System.nanoTime() - start);
                if (left <= 0 || Thread.currentThread() == task.thread) return false;
                try { TimeUnit.NANOSECONDS.timedJoin(task.thread, left); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); return false; }
            }
        }
    }
    public Slice<T> slice(long millis) throws InterruptedException {
        if (millis < 1 || millis > CONTINUATION_SLICE_MS) throw new IllegalArgumentException("R26_CONTINUATION_QUANTUM");
        if (!slicing.compareAndSet(false, true)) throw new IllegalStateException("R31_CONTINUATION_SLICE_IN_FLIGHT");
        try {
            synchronized (monitor) {
                long cpuBefore = cpuTotal;
                if (!done && !closed) {
                    endNanos = System.nanoTime() + millis * 1000000L; paused = false; monitor.notifyAll();
                    long hard = System.nanoTime() + (millis + 1000) * 1000000L;
                    while (!done && !paused && !closed) {
                        long remaining = hard - System.nanoTime();
                        if (remaining <= 0) { close(); throw new IllegalStateException("R26_UNSLICED_COMPUTATION_CAPACITY"); }
                        TimeUnit.NANOSECONDS.timedWait(monitor, remaining);
                    }
                }
                return new Slice<>(done, result, error, Math.max(0, cpuTotal - cpuBefore), maxPollGap, observedState());
            }
        } catch (InterruptedException interrupted) { close(); throw interrupted; }
        finally { slicing.set(false); }
    }
    private void run(){
        try {
            // R27.4: every continuation inherits the Android background priority hook.
            Runnable hook = threadStartHook; if (hook != null) try { hook.run(); } catch (Throwable denied) { /* OS may deny priority. */ }
            CURRENT.set(this); BudgetTokenSU1.setPauseClock(this); cpuOrigin = cpuClock.getAsLong();
            awaitPermit(); ExecutionBudgetR26.enter(new BudgetTokenSU1(3600000, Long.MAX_VALUE / 4, () -> closed));
            lastPoll = System.nanoTime(); phase = "BODY"; result = body.call();
        } catch (Throwable failure) { error = failure; }
        finally {
            ExecutionBudgetR26.leave(); BudgetTokenSU1.setPauseClock(null); CURRENT.remove();
            synchronized (monitor) {
                try { cpuTotal = Math.max(0, cpuClock.getAsLong() - cpuOrigin); }
                catch (Throwable failed) { if (error == null) error = failed; else if (error != failed) error.addSuppressed(failed); }
                done = true; paused = true; phase = "BODY_EXITED"; monitor.notifyAll();
            }
            // OWNED and the global permit remain owned until an observer sees Thread.isAlive()==false.
        }
    }
    private void awaitPermit() throws InterruptedException {
        synchronized (monitor) {
            while ((paused || endNanos == 0) && !closed) monitor.wait();
            if (closed) throw new InterruptedException("R26_CONTINUATION_CANCELLED");
            long now = System.nanoTime(); if (pausedSince != 0) { totalPaused += now - pausedSince; pausedSince = 0; }
            if (resume != null) resume.run(); lastPoll = now;
        }
    }
    private void checkpoint() {
        if (closed || Thread.currentThread().isInterrupted()) throw new BudgetTokenSU1.Yield("R26_CONTINUATION_CANCELLED");
        long now = System.nanoTime(); maxPollGap = Math.max(maxPollGap, now - lastPoll); lastPoll = now;
        if (criticalDepth > 0) return;
        if (now < endNanos && (preempt == null || !preempt.getAsBoolean())) return;
        long cpu = cpuClock.getAsLong();
        synchronized (monitor) { paused = true; pausedSince = System.nanoTime(); cpuTotal = Math.max(0, cpu - cpuOrigin); monitor.notifyAll(); }
        try { awaitPermit(); }
        catch (InterruptedException stopped) { Thread.currentThread().interrupt(); throw new BudgetTokenSU1.Yield("R26_CONTINUATION_CANCELLED"); }
    }
    public static void poll() {
        CooperativeContinuationR26<?> task = CURRENT.get();
        if (task != null) task.checkpoint();
        else if (Thread.currentThread().isInterrupted()) throw new CancellationException("R31_EXECUTION_INTERRUPTED");
    }
    public static void phase(String phase) { CooperativeContinuationR26<?> task = CURRENT.get(); if (task != null) task.phase = bounded(phase, 96); }
    /** Call only after the durable checkpoint succeeds; phase markers alone are not checkpoint evidence. */
    public static void recordCheckpoint(String checkpoint) {
        CooperativeContinuationR26<?> task = CURRENT.get(); if (task != null) { task.checkpoint = bounded(checkpoint, 96); task.lastCheckpoint = System.nanoTime(); }
    }
    private static String bounded(String text, int length) { if (text == null) return "UNKNOWN"; return text.substring(0, Math.min(length, text.length())).replace('\n', ' ').replace('\r', ' '); }
    public static void yieldPreparedWrite() {
        CooperativeContinuationR26<?> task = CURRENT.get();
        if (task == null) throw new IllegalStateException("R30_PREPARED_RETRY_REQUIRES_CONTINUATION");
        if (task.criticalDepth != 0) throw new IllegalStateException("R30_YIELD_INSIDE_WRITER");
        task.endNanos = 0; task.checkpoint();
    }
    public static boolean installed() { return CURRENT.get() != null; }
    public static final class Critical implements AutoCloseable {
        private final CooperativeContinuationR26<?> task;
        private final Thread owner = Thread.currentThread();
        private boolean closed;
        private Critical(CooperativeContinuationR26<?> task) { this.task = task; if (task != null) task.criticalDepth++; }
        public void close() {
            if (Thread.currentThread() != owner) throw new IllegalStateException("R31_CRITICAL_WRONG_THREAD");
            if (!closed) { closed = true; if (task != null) task.criticalDepth--; }
        }
    }
    public static Critical critical() { return new Critical(CURRENT.get()); }
    @Override public long pausedNanos() { return totalPaused + (pausedSince == 0 ? 0 : System.nanoTime() - pausedSince); }
    /** Result availability only. Resource owners must use isTerminated/awaitTermination. */
    public boolean isDone() { return done; }
    @Override public void close() { closed = true; synchronized (monitor) { monitor.notifyAll(); } thread.interrupt(); }
}
