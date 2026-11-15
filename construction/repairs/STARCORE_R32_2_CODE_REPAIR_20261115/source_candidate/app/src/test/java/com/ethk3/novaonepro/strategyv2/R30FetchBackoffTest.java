package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

/** R30-B02-02 宿主测试。复制到 app/src/test/java/com/ethk3/novaonepro/strategyv2/。 */
public class R30FetchBackoffTest {
    @Test public void singleFlightRejectsConcurrentRequests() throws Exception {
        final FetchBackoffR30 f = new FetchBackoffR30(() -> 500_000L);
        final AtomicInteger admitted = new AtomicInteger(), maxInFlight = new AtomicInteger(), inFlight = new AtomicInteger();
        final CountDownLatch go = new CountDownLatch(1), done = new CountDownLatch(16);
        for (int t = 0; t < 16; t++) new Thread(() -> {
            try { go.await(); for (int i = 0; i < 2000; i++) if (f.tryBegin()) { admitted.incrementAndGet(); int now = inFlight.incrementAndGet(); maxInFlight.accumulateAndGet(now, Math::max); inFlight.decrementAndGet(); f.end(true); } }
            catch (InterruptedException ignored) {} finally { done.countDown(); }
        }).start();
        go.countDown(); done.await();
        assertEquals(1, maxInFlight.get());
        assertTrue(admitted.get() > 0);
    }

    @Test public void backoffGrowsCapsAndResets() {
        FetchBackoffR30 f = new FetchBackoffR30(() -> 500_000L); // 抖动中点 = 0
        assertEquals(1000, f.nextDelayMs(false, Long.MIN_VALUE));
        long[] expect = {1000, 2000, 4000, 8000, 15000, 15000};
        for (long e : expect) { assertTrue(f.tryBegin()); f.end(false); assertEquals(e, f.nextDelayMs(false, Long.MIN_VALUE)); }
        assertTrue(f.tryBegin()); f.end(true);
        assertEquals(1000, f.nextDelayMs(false, Long.MIN_VALUE));
        assertEquals(3000, f.nextDelayMs(true, 45_000));
        assertEquals(1000, f.nextDelayMs(true, 15_000));
        FetchBackoffR30 lo = new FetchBackoffR30(() -> 0L), hi = new FetchBackoffR30(() -> 999_999L);
        for (int i = 0; i < 3; i++) { lo.tryBegin(); lo.end(false); hi.tryBegin(); hi.end(false); }
        assertTrue(lo.nextDelayMs(false, 0) >= 3200 && lo.nextDelayMs(false, 0) <= 3201);
        assertTrue(hi.nextDelayMs(false, 0) >= 4799 && hi.nextDelayMs(false, 0) <= 4800);
    }
}
