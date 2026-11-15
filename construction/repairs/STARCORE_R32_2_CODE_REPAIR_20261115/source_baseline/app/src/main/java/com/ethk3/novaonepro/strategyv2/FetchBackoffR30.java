package com.ethk3.novaonepro.strategyv2;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * R30-B02-02 抓取节奏与单飞（纯Java，HOST可测）。
 *
 * 唯一规则：
 *  - 全进程同一时刻最多一个开奖请求在途（{@link #tryBegin()} / {@link #end}）；第二个调用直接返回 false，不排队。
 *  - 健康：每 1000ms 一次（AppConfig.REALTIME_FETCH_INTERVAL_MS 保持不变）。
 *  - 连续失败 n 次：min(15000, 1000·2^(n−1)) ± 20% 抖动；成功一次立即回到 1000ms。
 *  - 已拿到当期且距下一期截止 > 20s：放慢到 3000ms（省电），进入截止前 20s 恢复 1000ms。
 *  - 请求超时固定 4000ms（由调用方 HTTP 客户端设置），超时计一次失败。
 */
public final class FetchBackoffR30 {
    public static final long HEALTHY_MS = 1000L, RELAXED_MS = 3000L, MAX_BACKOFF_MS = 15000L, RELAX_WINDOW_MS = 20000L, REQUEST_TIMEOUT_MS = 4000L;
    private final AtomicBoolean inFlight = new AtomicBoolean(false);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong rejectedConcurrent = new AtomicLong(0);
    private final java.util.function.LongSupplier random; // 返回 [0, 1_000_000)

    public FetchBackoffR30(java.util.function.LongSupplier randomMicros) { this.random = randomMicros; }
    public FetchBackoffR30() { this(() -> java.util.concurrent.ThreadLocalRandom.current().nextLong(1_000_000L)); }

    /** 单飞入口：返回 true 才能发请求；必须在 finally 中调用 {@link #end}。 */
    public boolean tryBegin() {
        if (inFlight.compareAndSet(false, true)) return true;
        rejectedConcurrent.incrementAndGet();
        return false;
    }

    public void end(boolean success) {
        if (success) consecutiveFailures.set(0); else consecutiveFailures.incrementAndGet();
        inFlight.set(false);
    }

    /**
     * 下一次请求延迟。
     * @param haveCurrentDraw 本地已有最新可预期的一期开奖
     * @param msUntilNextDeadline 距下一期截止的毫秒数，未知传 Long.MIN_VALUE
     */
    public long nextDelayMs(boolean haveCurrentDraw, long msUntilNextDeadline) {
        int n = consecutiveFailures.get();
        if (n > 0) {
            long base = Math.min(MAX_BACKOFF_MS, HEALTHY_MS << Math.min(n - 1, 4));
            long jitter = (base * 2 * random.getAsLong()) / 5_000_000L - base / 5; // ±20%
            return Math.max(HEALTHY_MS, Math.min(MAX_BACKOFF_MS + MAX_BACKOFF_MS / 5, base + jitter));
        }
        if (haveCurrentDraw && msUntilNextDeadline != Long.MIN_VALUE && msUntilNextDeadline > RELAX_WINDOW_MS) return RELAXED_MS;
        return HEALTHY_MS;
    }

    public int consecutiveFailures() { return consecutiveFailures.get(); }
    public long rejectedConcurrent() { return rejectedConcurrent.get(); }
    public boolean inFlight() { return inFlight.get(); }
}
