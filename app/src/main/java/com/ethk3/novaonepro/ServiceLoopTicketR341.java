package com.ethk3.novaonepro;

import android.os.Handler;
import java.util.function.LongSupplier;

/** Owns one pending callback on one service lane, fenced by the service generation. */
final class ServiceLoopTicketR341 {
    interface Queue {
        boolean post(Runnable callback, long delayMs);
        void remove(Runnable callback);
    }

    enum Result { POSTED, RETAINED, REJECTED, STALE }

    private final LongSupplier elapsed;
    private long generation;
    private Ticket pending;
    private Result lastResult = Result.STALE;

    ServiceLoopTicketR341(LongSupplier elapsed) {
        this.elapsed = elapsed;
    }

    synchronized void reset(long newGeneration) {
        Ticket previous = pending;
        pending = null;
        generation = newGeneration;
        lastResult = Result.STALE;
        if (previous != null) previous.queue.remove(previous);
    }

    Result request(long generation, Handler handler, long delayMs, boolean advance, Runnable work) {
        if (handler == null) return Result.REJECTED;
        return request(generation, handler, new Queue() {
            @Override public boolean post(Runnable callback, long delay) {
                return handler.postDelayed(callback, delay);
            }

            @Override public void remove(Runnable callback) {
                handler.removeCallbacks(callback);
            }
        }, delayMs, advance, work);
    }

    synchronized Result request(long requestedGeneration, Object owner, Queue queue,
            long delayMs, boolean advance, Runnable work) {
        if (generation != requestedGeneration) return Result.STALE;
        long delay = Math.max(0L, delayMs);
        long due = elapsed.getAsLong() + delay;
        Ticket previous = pending;
        if (previous != null && previous.owner == owner && (!advance || previous.due <= due)) {
            return Result.RETAINED;
        }
        Ticket replacement = new Ticket(requestedGeneration, owner, queue, due, work);
        pending = replacement;
        if (!queue.post(replacement, delay)) {
            pending = previous;
            lastResult = Result.REJECTED;
            return lastResult;
        }
        lastResult = Result.POSTED;
        if (previous != null) previous.queue.remove(previous);
        return lastResult;
    }

    synchronized String snapshot() {
        return "generation=" + generation + ",dueElapsedMs=" + (pending == null ? 0L : pending.due)
                + ",lastPost=" + lastResult.name();
    }

    private synchronized boolean claim(Ticket ticket) {
        if (ticket != pending || ticket.generation != generation) return false;
        pending = null;
        return true;
    }

    private final class Ticket implements Runnable {
        final long generation;
        final Object owner;
        final Queue queue;
        final long due;
        final Runnable work;

        Ticket(long generation, Object owner, Queue queue, long due, Runnable work) {
            this.generation = generation;
            this.owner = owner;
            this.queue = queue;
            this.due = due;
            this.work = work;
        }

        @Override public void run() {
            if (claim(this)) work.run();
        }
    }
}
