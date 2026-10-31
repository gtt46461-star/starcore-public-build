package com.ethk3.novaonepro.strategyv2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * R30-B01-01 写锁等待/持有时长账本（纯Java，无Android依赖，可HOST测试）。
 *
 * 每一次经过 DatabaseCoordinator 唯一写锁的写操作，在释放锁之后调用一次 {@link #record}。
 * 账本只做内存累计，不写库、不抛异常、不阻塞调用方；落库由 HoldLedgerFlushR30 每60秒一次短事务完成。
 *
 * 分桶：0,1,2,5,10,20,50,100,200,500,1000,2000,5000,10000,30000,+inf 毫秒（16桶），
 * 分位数取桶上界（保守估计，不会把慢的写成快的）。
 */
public final class WriterHoldLedgerR30 {
    public static final String VERSION = "R30_WRITER_HOLD_LEDGER_V1";
    /** 后台写事务持有写锁的目标上限；超过即记 overBudget，用于B01切片清单。 */
    public static final long BACKGROUND_HOLD_BUDGET_MS = 50L;
    /** 后台写事务硬上限；超过记 overHard，B01验收要求24h内此类为0。 */
    public static final long BACKGROUND_HOLD_HARD_MS = 200L;
    static final long[] BOUNDS = {0, 1, 2, 5, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000, 30000, Long.MAX_VALUE};
    private static final int MAX_OPERATIONS = 512;
    private static final String OVERFLOW = "__OTHER__";

    private WriterHoldLedgerR30() {}

    public static final class Stat {
        public final String operation;
        public final boolean critical;
        final AtomicLong count = new AtomicLong();
        final AtomicLong holdSum = new AtomicLong();
        final AtomicLong holdMax = new AtomicLong();
        final AtomicLong waitSum = new AtomicLong();
        final AtomicLong waitMax = new AtomicLong();
        final AtomicLong overBudget = new AtomicLong();
        final AtomicLong overHard = new AtomicLong();
        final AtomicLong failures = new AtomicLong();
        final AtomicLongArray holdHist = new AtomicLongArray(BOUNDS.length);
        final AtomicLongArray waitHist = new AtomicLongArray(BOUNDS.length);
        Stat(String operation, boolean critical) { this.operation = operation; this.critical = critical; }
        public long count() { return count.get(); }
        public long holdMax() { return holdMax.get(); }
        public long waitMax() { return waitMax.get(); }
        public long overBudget() { return overBudget.get(); }
        public long overHard() { return overHard.get(); }
        public long failures() { return failures.get(); }
        public long holdP(double q) { return percentile(holdHist, q); }
        public long waitP(double q) { return percentile(waitHist, q); }
        public double holdMean() { long n = count.get(); return n == 0 ? 0 : holdSum.get() / (double) n; }
    }

    private static final ConcurrentHashMap<String, Stat> STATS = new ConcurrentHashMap<>();
    private static final AtomicLong WINDOW_START_MS = new AtomicLong(System.currentTimeMillis());

    /** 调用方：DatabaseCoordinator.runWrite 的 finally（已释放写锁之后）。任何异常都被吞掉，诊断不能改变业务结果。 */
    public static void record(String operation, boolean critical, long waitMs, long holdMs, boolean failed) {
        try {
            String key = normalize(operation, critical);
            Stat s = STATS.get(key);
            if (s == null) {
                if (STATS.size() >= MAX_OPERATIONS) key = (critical ? "C|" : "B|") + OVERFLOW;
                s = STATS.computeIfAbsent(key, k -> new Stat(k.substring(2), k.charAt(0) == 'C'));
            }
            long hold = Math.max(0L, holdMs), wait = Math.max(0L, waitMs);
            s.count.incrementAndGet();
            s.holdSum.addAndGet(hold);
            s.waitSum.addAndGet(wait);
            max(s.holdMax, hold);
            max(s.waitMax, wait);
            s.holdHist.incrementAndGet(bucket(hold));
            s.waitHist.incrementAndGet(bucket(wait));
            if (failed) s.failures.incrementAndGet();
            if (!critical && hold > BACKGROUND_HOLD_BUDGET_MS) s.overBudget.incrementAndGet();
            if (!critical && hold > BACKGROUND_HOLD_HARD_MS) s.overHard.incrementAndGet();
        } catch (RuntimeException ignored) {
            // 诊断失败不能影响写入结果。
        }
    }

    static String normalize(String operation, boolean critical) {
        String op = operation == null ? "" : operation;
        if (op.length() > 64) op = op.substring(0, 64);
        StringBuilder b = new StringBuilder(op.length() + 2).append(critical ? "C|" : "B|");
        for (int i = 0; i < op.length(); i++) {
            char c = op.charAt(i);
            b.append(Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.' ? c : '_');
        }
        return b.toString();
    }

    static int bucket(long ms) {
        for (int i = 0; i < BOUNDS.length; i++) if (ms <= BOUNDS[i]) return i;
        return BOUNDS.length - 1;
    }

    static long percentile(AtomicLongArray hist, double q) {
        long total = 0;
        for (int i = 0; i < hist.length(); i++) total += hist.get(i);
        if (total == 0) return -1L;
        long rank = (long) Math.ceil(Math.max(0.0, Math.min(1.0, q)) * total);
        if (rank < 1) rank = 1;
        long seen = 0;
        for (int i = 0; i < hist.length(); i++) {
            seen += hist.get(i);
            if (seen >= rank) return BOUNDS[i];
        }
        return BOUNDS[BOUNDS.length - 1];
    }

    private static void max(AtomicLong target, long value) {
        long cur;
        while (value > (cur = target.get()) && !target.compareAndSet(cur, value)) { /* retry */ }
    }

    /** 当前窗口所有操作的快照（按 holdMax 降序）。 */
    public static List<Stat> snapshot() {
        List<Stat> out = new ArrayList<>(STATS.values());
        Collections.sort(out, new Comparator<Stat>() {
            @Override public int compare(Stat a, Stat b) {
                int c = Long.compare(b.holdMax(), a.holdMax());
                return c != 0 ? c : a.operation.compareTo(b.operation);
            }
        });
        return out;
    }

    /** B01切片清单：后台操作中 holdMax 超过预算的，按超限次数排序。 */
    public static List<Stat> offenders() {
        List<Stat> out = new ArrayList<>();
        for (Stat s : snapshot()) if (!s.critical && s.overBudget() > 0) out.add(s);
        Collections.sort(out, new Comparator<Stat>() {
            @Override public int compare(Stat a, Stat b) {
                int c = Long.compare(b.overHard(), a.overHard());
                if (c != 0) return c;
                c = Long.compare(b.overBudget(), a.overBudget());
                return c != 0 ? c : a.operation.compareTo(b.operation);
            }
        });
        return out;
    }

    /** 关键业务写（抓取/结算/冻结/决策）等待写锁的P95，M07验收用。无样本返回-1，不得写成0。 */
    public static long criticalWaitP95() {
        AtomicLongArray merged = new AtomicLongArray(BOUNDS.length);
        boolean any = false;
        for (Stat s : STATS.values()) {
            if (!s.critical) continue;
            any = true;
            for (int i = 0; i < BOUNDS.length; i++) merged.addAndGet(i, s.waitHist.get(i));
        }
        return any ? percentile(merged, 0.95) : -1L;
    }

    public static long windowStartMs() { return WINDOW_START_MS.get(); }

    /** 每次持久化之后开启新窗口；返回被关闭窗口的快照。 */
    public static synchronized List<Stat> rotate() {
        List<Stat> closed = snapshot();
        STATS.clear();
        WINDOW_START_MS.set(System.currentTimeMillis());
        return closed;
    }

    /** 报告/诊断用的稳定结构；值未知时为 null。 */
    public static Map<String, Object> toMap(Stat s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("operation", s.operation);
        m.put("critical", s.critical);
        m.put("count", s.count());
        m.put("holdP50", nullIfNone(s.holdP(0.50)));
        m.put("holdP95", nullIfNone(s.holdP(0.95)));
        m.put("holdP99", nullIfNone(s.holdP(0.99)));
        m.put("holdMax", s.holdMax());
        m.put("waitP95", nullIfNone(s.waitP(0.95)));
        m.put("waitMax", s.waitMax());
        m.put("overBudget50ms", s.overBudget());
        m.put("overHard200ms", s.overHard());
        m.put("failures", s.failures());
        return m;
    }

    private static Long nullIfNone(long v) { return v < 0 ? null : v; }

    /** 仅测试使用。 */
    static void resetForTest() { STATS.clear(); WINDOW_START_MS.set(System.currentTimeMillis()); }
}
