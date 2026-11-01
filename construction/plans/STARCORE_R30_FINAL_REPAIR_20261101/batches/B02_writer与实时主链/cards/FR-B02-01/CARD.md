# FR-B02-01 防止写锁遥测旋转丢数与失败丢窗口

优先级：P0。状态：设计交付，尚未Android/设备验收。原需求：FIX-04。

前驱：B01。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/WriterHoldLedgerR30.java`

动作：**MODIFY**。施工范围：record/rotate。

问题：record未与rotate同锁，窗口交换可漏记。

修改方式：只在数据库锁外的极短内存区同步record与rotate；不在该监视器下请求DB。

原方法锚点：WriterHoldLedgerR30.record(String operation,boolean critical,long waitMs,long holdMs,boolean failed) L63–85；WriterHoldLedgerR30.rotate() L164–169

基线SHA256：`799bb643c746f70f075b331b80256a487de62203510dc957cac7b84f11096965`。共享合并顺序：FR-B02-01。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/HoldLedgerFlushR30.java`

动作：**MODIFY**。施工范围：maybeFlush。

问题：先rotate再写库，catch后未保留closed，注释所称重试不成立。

修改方式：使用单飞AtomicBoolean和一个有界pending快照；原窗口主键幂等重试；成功后才清pending。

原方法锚点：HoldLedgerFlushR30.maybeFlush(DbStore store) L26–46

基线SHA256：`59c7895e289ab4f273022dd914d5fa5f0d414bf197361592a6e9ef45ce194c7f`。共享合并顺序：FR-B02-01。

## 实施步骤

1. 应用B02遥测补丁。
2. 区分进程内重试保全与进程死亡窗口未知；不能宣称未落库遥测可跨进程无损。
3. 开口分位桶显示>30000ms，不把Long.MAX_VALUE当真实耗时；窗口汇总用直方图合同。

## 测试与反证

- 16万次并发record/rotate总数无损；首次DB写拒绝后重试保留原12条和新3条；主线程和DB锁顺序不反转。

## 验收

遥测重试不丢数/重复计数，开口桶不显示9e18ms。

## 恢复与回滚

保留pending原件，撤回展示/刷新调度，不清历史遥测。

## 关键代码

顺序补丁：`patches/B02_writer_telemetry_retry.patch`。必须按SERIES顺序应用，一份跨文件补丁只应用一次。

已预写目标文件：`code/app/src/main/java/com/ethk3/novaonepro/strategyv2/WriterHoldLedgerR30.java`。它是本包关键补丁叠加后的组合目标，不是允许覆盖其他卡修改的单独发布文件。

已预写目标文件：`code/app/src/main/java/com/ethk3/novaonepro/strategyv2/HoldLedgerFlushR30.java`。它是本包关键补丁叠加后的组合目标，不是允许覆盖其他卡修改的单独发布文件。

### 可直接审阅的关键代码

`app/src/main/java/com/ethk3/novaonepro/strategyv2/WriterHoldLedgerR30.java`（关键组合目标；集成限制见上）：

```java
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
    public static synchronized void record(String operation, boolean critical, long waitMs, long holdMs, boolean failed) {
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

```

`app/src/main/java/com/ethk3/novaonepro/strategyv2/HoldLedgerFlushR30.java`（关键组合目标；集成限制见上）：

```java
package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DbStore;
import java.util.List;

/**
 * R30-B01-02 写锁账本落库：每 60 秒最多一次，一个后台短事务（≤ 512 行）。
 * 调用点（唯一）：ResearchPoolR27.recordThreadCpu(...) 末尾 writes.flush() 之后一行：HoldLedgerFlushR30.maybeFlush(store);
 * 失败只记日志并保留内存窗口到下一次，不影响研究线程。
 */
public final class HoldLedgerFlushR30 {
    public static final long PERIOD_MS = 60_000L;
    public static final long RETAIN_MS = 7L * 24 * 3600_000L;
    public static final String DDL = "CREATE TABLE IF NOT EXISTS r30_writer_hold(window_start INTEGER NOT NULL,window_end INTEGER NOT NULL,"
            + "operation TEXT NOT NULL,critical INTEGER NOT NULL,count INTEGER NOT NULL,hold_p50 INTEGER,hold_p95 INTEGER,hold_p99 INTEGER,"
            + "hold_max INTEGER NOT NULL,wait_p95 INTEGER,wait_max INTEGER NOT NULL,over_budget INTEGER NOT NULL,over_hard INTEGER NOT NULL,"
            + "failures INTEGER NOT NULL,PRIMARY KEY(window_start,operation,critical))";
    public static final String INSERT = "INSERT OR REPLACE INTO r30_writer_hold(window_start,window_end,operation,critical,count,hold_p50,hold_p95,hold_p99,"
            + "hold_max,wait_p95,wait_max,over_budget,over_hard,failures) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    public static final String PRUNE = "DELETE FROM r30_writer_hold WHERE window_end<?";
    private static volatile long lastFlush;
    private static final java.util.concurrent.atomic.AtomicBoolean FLUSHING=new java.util.concurrent.atomic.AtomicBoolean();
    private static volatile Pending pending;
    private static final class Pending {
        final long start,end;final List<WriterHoldLedgerR30.Stat> rows;
        Pending(long start,long end,List<WriterHoldLedgerR30.Stat> rows){this.start=start;this.end=end;this.rows=rows;}
    }
    private HoldLedgerFlushR30() {}

    public static void maybeFlush(DbStore store) {
        long now=System.currentTimeMillis();
        if(now-lastFlush<PERIOD_MS||!FLUSHING.compareAndSet(false,true))return;
        try {
            if(now-lastFlush<PERIOD_MS)return;
            lastFlush=now;
            if(pending==null){long start=WriterHoldLedgerR30.windowStartMs();List<WriterHoldLedgerR30.Stat> rows=WriterHoldLedgerR30.rotate();if(rows.isEmpty())return;pending=new Pending(start,now,rows);}
            final Pending closed=pending;
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R30_WRITER_HOLD_FLUSH",db->{
                db.execSQL(DDL);
                for(WriterHoldLedgerR30.Stat s:closed.rows)db.execSQL(INSERT,new Object[]{closed.start,closed.end,s.operation,s.critical?1:0,s.count(),nz(s.holdP(0.5)),nz(s.holdP(0.95)),nz(s.holdP(0.99)),s.holdMax(),nz(s.waitP(0.95)),s.waitMax(),s.overBudget(),s.overHard(),s.failures()});
                db.execSQL(PRUNE,new Object[]{closed.end-RETAIN_MS});return 0;
            });
            pending=null; // ONLY after a successful atomic write; retry keeps the same idempotent window key.
        }catch(RuntimeException busy){android.util.Log.w("STARCORE_R30","writer hold ledger pending retained",busy);}
        finally{FLUSHING.set(false);}
    }

    private static Long nz(long v) { return v < 0 ? null : v; }

    /** 报告第3章：最近24小时后台超时操作（按超硬限次数降序，最多50行）。 */
    public static final String REPORT_TOP =
            "SELECT operation,sum(count),max(hold_max),max(hold_p95),sum(over_budget),sum(over_hard) FROM r30_writer_hold "
            + "WHERE critical=0 AND window_end>=? GROUP BY operation HAVING sum(over_budget)>0 ORDER BY sum(over_hard) DESC,sum(over_budget) DESC LIMIT 50";
    /** 报告第3章：关键业务写等待（M07）。 */
    public static final String REPORT_CRITICAL =
            "SELECT operation,sum(count),max(wait_p95),max(wait_max),max(hold_max) FROM r30_writer_hold WHERE critical=1 AND window_end>=? GROUP BY operation ORDER BY max(wait_p95) DESC";

    public static long overHardSince(SQLiteDatabase ro, long since) {
        try (Cursor c = ro.rawQuery("SELECT coalesce(sum(over_hard),0) FROM r30_writer_hold WHERE critical=0 AND window_end>=?", new String[] {Long.toString(since)})) {
            return c.moveToFirst() ? c.getLong(0) : -1L;
        } catch (RuntimeException missingTable) { return -1L; }
    }
}

```

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
