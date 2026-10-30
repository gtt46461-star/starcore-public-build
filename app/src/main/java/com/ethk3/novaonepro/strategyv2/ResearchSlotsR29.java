package com.ethk3.novaonepro.strategyv2;

import java.util.concurrent.atomic.AtomicInteger;

/** Shared worker admission operation. A check followed by an unconditional increment allowed two simultaneous
 * workers through a one-worker pressure limit. This CAS is the operation used by the Android research pool. */
public final class ResearchSlotsR29 {
    private ResearchSlotsR29() {}
    public static boolean tryEnter(AtomicInteger active, int limit) {
        if (limit < 1) throw new IllegalArgumentException("R29_WORKER_LIMIT");
        int current = active.get();
        return current < limit && active.compareAndSet(current, current + 1);
    }
}
