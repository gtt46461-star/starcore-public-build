package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

public class R29ResearchSlotsTest {
    static void contend(int limit) throws Exception {
        AtomicInteger active = new AtomicInteger(), peak = new AtomicInteger(), completed = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1); ExecutorService pool = Executors.newFixedThreadPool(8);
        List<Future<?>> jobs = new ArrayList<>();
        try {
            for (int w = 0; w < 8; w++) jobs.add(pool.submit(() -> {
                try { start.await(); } catch (InterruptedException e) { throw new AssertionError(e); }
                for (int i = 0; i < 300; i++) {
                    while (!ResearchSlotsR29.tryEnter(active, limit)) Thread.yield();
                    try { peak.accumulateAndGet(active.get(), Math::max); Thread.yield(); completed.incrementAndGet(); }
                    finally { active.decrementAndGet(); }
                }
            }));
            start.countDown(); for (Future<?> job : jobs) job.get(15, TimeUnit.SECONDS);
            assertEquals(2400, completed.get()); assertEquals(0, active.get()); assertTrue(peak.get() >= 1); assertTrue(peak.get() <= limit);
        } finally { pool.shutdownNow(); }
    }
    @Test public void p0ThermalSingleWorkerLimitUnderContention() throws Exception { contend(1); }
    @Test public void normalTwoWorkerLimitUnderContention() throws Exception { contend(2); }
}
