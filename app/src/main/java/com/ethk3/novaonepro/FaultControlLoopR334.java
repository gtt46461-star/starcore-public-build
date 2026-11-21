package com.ethk3.novaonepro;

/** Single-owner periodic control. The scheduler MUST use a dedicated Looper.
 * A stopped generation cannot enqueue another tick after a restart. This class
 * controls scheduling only; every SQL write still uses DatabaseCoordinator.
 */
public final class FaultControlLoopR334 {
    public interface Clock { long elapsedMs(); }
    public interface Scheduler {
        boolean postDelayed(Runnable task, long delayMs);
        void remove(Runnable task);
    }
    private final long periodMs;
    private final Clock clock;
    private final Scheduler scheduler;
    private final Runnable slice;
    private long generation, nextAt, completed, missedSlots, rejectedPosts;
    private boolean active;
    private Runnable pending;

    public FaultControlLoopR334(long periodMs, Clock clock, Scheduler scheduler, Runnable slice) {
        if (periodMs <= 0 || clock == null || scheduler == null || slice == null)
            throw new IllegalArgumentException("R334_CONTROL_ARGUMENT");
        this.periodMs = periodMs;
        this.clock = clock;
        this.scheduler = scheduler;
        this.slice = slice;
    }

    public synchronized void start() {
        if (active) return;
        active = true;
        generation++;
        nextAt = clock.elapsedMs();
        post(generation, 0L);
    }

    public synchronized void stop() {
        active = false;
        generation++;
        if (pending != null) scheduler.remove(pending);
        pending = null;
    }

    private void post(final long ticket, long delay) {
        pending = new Runnable() { @Override public void run() { tick(ticket); } };
        if (!scheduler.postDelayed(pending, delay)) {
            rejectedPosts++;
            pending = null;
            active = false;
        }
    }

    private void tick(long ticket) {
        synchronized (this) {
            if (!active || generation != ticket) return;
            pending = null;
        }
        try {
            slice.run();
        } finally {
            synchronized (this) {
                if (active && generation == ticket) {
                    completed++;
                    long now = clock.elapsedMs();
                    nextAt += periodMs;
                    if (nextAt <= now) {
                        long skip = (now - nextAt) / periodMs + 1L;
                        missedSlots += skip;
                        nextAt += skip * periodMs;
                    }
                    post(ticket, Math.max(0L, nextAt - now));
                }
            }
        }
    }

    public synchronized long[] snapshot() {
        return new long[] { active ? 1L : 0L, generation, completed, missedSlots, rejectedPosts };
    }
}
