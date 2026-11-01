# FR-B02-06 跨文件批次原子性与组合守卫

优先级：P0。状态：设计交付，尚未Android/设备验收。原需求：FIX-04/FIX-19。

前驱：B01。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchWriteQueueR27.java`

动作：**MODIFY**。施工范围：Batch/submit/execute。

问题：before字段可被后续helper覆盖；整逻辑batch不能内部切成独立提交。

修改方式：新增guardBefore组合API；保存原fence再附加source/消费守卫；只在任务计算边界分片。

原方法锚点：ResearchWriteQueueR27$Batch.add(String sql,Object... args) L35–35；ResearchWriteQueueR27$Batch.isEmpty() L36–36；ResearchWriteQueueR27.submit(String operation,Batch batch,java.util.function.BooleanSupplier cancelled) L46–46；ResearchWriteQueueR27.submit(String operation,Batch batch,java.util.function.BooleanSupplier cancelled,boolean freezeWrite) L50–73；ResearchWriteQueueR27.execute(SQLiteDatabase db,Batch batch,String operation) L80–85

基线SHA256：`83540cb7d111b76a31f00e38f41bb27a836af54a348700439fa55deb4536de54`。共享合并顺序：FR-B02-06。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/CommitEnvelopeR30R1.java`

动作：**ADD**。施工范围：完整新增类。

问题：提交失败时内存状态可能领先落库游标。

修改方式：用CAS单飞信封，RETRY不发布、SOURCE_CONFLICT丢计算重建；已提交状态防御复制。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`NEW_FILE`。共享合并顺序：FR-B02-06。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchContractR30R1.java`

动作：**ADD**。施工范围：完整新增类。

问题：跨模块输入、角色与revision身份不够明确。

修改方式：不可变输入合同，role/窗口/source/ABI参与hash，未来及确认数据不得驱动研究处方。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`NEW_FILE`。共享合并顺序：FR-B02-06。

## 实施步骤

1. 应用B02守卫补丁；逐个核对旧.before赋值的最终Owner，不能只改API不改调用。
2. SQL、artifact、游标、消费回执属于一个逻辑提交；大对象需stage+发布合同，不在队列按500行强拆。

## 测试与反证

- 原fence失败、新guard失败、源revision改变、异常重试、两个线程并发提交；未提交状态不可见。

## 验收

原子性、幂等和守卫没有被跨文件helper覆盖；每阶段只有一个可见发布点。

## 恢复与回滚

关闭新信封路由，恢复最后成功提交状态，不重复消耗标签。

## 关键代码

顺序补丁：`patches/B02_atomic_step_guards.patch`。必须按SERIES顺序应用，一份跨文件补丁只应用一次。

已预写目标文件：`code/app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchWriteQueueR27.java`。它是本包关键补丁叠加后的组合目标，不是允许覆盖其他卡修改的单独发布文件。

已预写目标文件：`code/app/src/main/java/com/ethk3/novaonepro/strategyv2/CommitEnvelopeR30R1.java`。它是本包关键补丁叠加后的组合目标，不是允许覆盖其他卡修改的单独发布文件。

已预写目标文件：`code/app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchContractR30R1.java`。它是本包关键补丁叠加后的组合目标，不是允许覆盖其他卡修改的单独发布文件。

### 可直接审阅的关键代码

`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchWriteQueueR27.java`（关键组合目标；集成限制见上）：

```java
package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.util.ArrayList;
import java.util.List;

/** Research batch boundary. A batch's CAS, outputs and cursor are ONE transaction, including batches over 500
 * statements. Callers must prepare bounded slices before submitting; splitting a logical batch inside this queue
 * exposed partial model state and let a retry apply non-idempotent increments twice. R29-005's independent database
 * cutover is still pending: this correctness fix deliberately retains the existing writer and its pressure guard. */
public final class ResearchWriteQueueR27 {
    public static final int MAX_ROWS = 500; public static final long MAX_CHUNK_MS = 20, MAX_WAIT_MS = 200, DEADLINE_GUARD_MS = 5000;
    private final DbStore store; private volatile long nextDeadline;
    interface AtomicWriter { void write(String operation, Batch batch); }
    private final AtomicWriter writer;
    private final java.util.function.BooleanSupplier critical;
    private long hourStart, submits, longWaits, maxWait, requeues;

    public ResearchWriteQueueR27(DbStore store) {
        this.store = store; this.critical = ResearchWriteQueueR27::p0Writing;
        this.writer = (operation, batch) -> store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(
                "R27_" + operation, db -> { execute(db, batch, operation); return 0; });
    }
    // Same production submit path with a transactional test sink, without an Android process/DbStore.
    ResearchWriteQueueR27(AtomicWriter writer, java.util.function.BooleanSupplier critical) {
        this.store = null; this.writer = writer; this.critical = critical;
    }

    /** One prepared statement and its bind arguments. */
    public static final class Op { final String sql; final Object[] args; public Op(String sql, Object... args) { this.sql = sql; this.args = args; } }
    /** A batch built in memory; optional callback runs first inside the same transaction (fence checks, CAS). */
    public static final class Batch {
        public final List<Op> ops = new ArrayList<>(); public DatabaseCoordinator.WriteCallable<Integer> before, after;
        public Batch add(String sql, Object... args) { ops.add(new Op(sql, args)); return this; }
        public boolean isEmpty() { return ops.isEmpty() && before == null && after == null; }
        /** Compose preconditions; a later helper must not overwrite the original fence. */
        public Batch guardBefore(DatabaseCoordinator.WriteCallable<Integer> guard) {
            if(guard==null)throw new IllegalArgumentException("R30_BATCH_GUARD_REQUIRED");
            final DatabaseCoordinator.WriteCallable<Integer> prior=before;
            before=db->{if(prior!=null){Integer r=prior.call(db);if(r==null||r!=0)return r;}return guard.call(db);};return this;
        }
    }

    public void setNextDeadline(long wallMs) { nextDeadline = wallMs; }
    public long nextDeadline() { return nextDeadline; }
    public boolean freezeWindow(long now) { long d = nextDeadline; return d > 0 && now < d && d - now < DEADLINE_GUARD_MS; }
    public static boolean p0Writing() { return SettlementExecutionGateR819.isActive() || RealtimePressureGateR811.hasActiveCriticalWork(); }

    /** Blocks the calling research thread for at most 200 ms per attempt; returns false if it had to re-queue
     * (the caller keeps the batch and retries on its next slice). */
    public boolean submit(String operation, Batch batch, java.util.function.BooleanSupplier cancelled) { return submit(operation, batch, cancelled, false); }

    /** @param freezeWrite the R27 live freeze itself: exempt from the deadline guard (it is the work the guard protects),
     *                     still yields to P0 settlement for at most 200 ms. */
    public boolean submit(String operation, Batch batch, java.util.function.BooleanSupplier cancelled, boolean freezeWrite) {
        if (batch.isEmpty()) return true;
        long start = System.nanoTime();
        while (critical.getAsBoolean() || (!freezeWrite && freezeWindow(System.currentTimeMillis()))) {
            long waited=(System.nanoTime()-start)/1_000_000L;
            if (cancelled.getAsBoolean()) { stat(waited,true); return false; }
            long remaining=MAX_WAIT_MS-waited;
            if (remaining<=0) { stat(waited,true); return false; }
            // Yield outside the writer and bound the last sleep by the unchanged 200ms allowance.
            try { Thread.sleep(Math.min(4L,remaining)); }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt();stat((System.nanoTime()-start)/1_000_000L,true);return false; }
        }
        long waited=(System.nanoTime()-start)/1_000_000L,remaining=MAX_WAIT_MS-waited;
        if (cancelled.getAsBoolean() || remaining<=0) { stat(waited,true);return false; }
        // Gate waiting consumes the same allowance as acquiring the existing writer; never grant another 250ms.
        try (DatabaseCoordinator.WriterDeadlineScopeCP3 deadline=store==null?null:DatabaseCoordinator.withWriterDeadlineCP3(TimeSourceR811.monoNowMs()+remaining)) {
            writer.write(operation, batch);
        } catch (IllegalStateException failure) {
            if (!writerBusy(failure)) throw failure;
            stat((System.nanoTime()-start)/1_000_000L,true); return false;
        }
        stat(waited, false);
        return true;
    }

    /** In-memory counters from actual submit attempts; health flush keeps the existing durable hourly format. */
    public synchronized java.util.Map<String,Object> waitSnapshotR30(){
        java.util.Map<String,Object> out=new java.util.LinkedHashMap<>();out.put("hourStart",hourStart);out.put("submits",submits);out.put("rejectedOrRequeued",requeues);out.put("waitsAtLeast200ms",longWaits);out.put("maxObservedGateOrRejectedAttemptMs",maxWait);out.put("maxWaitAllowanceMs",MAX_WAIT_MS);return java.util.Collections.unmodifiableMap(out);
    }

    static void execute(SQLiteDatabase db, Batch batch, String operation) throws Exception {
        if (!db.inTransaction()) throw new IllegalStateException("R29_BATCH_TRANSACTION_REQUIRED");
        if (batch.before != null) require(batch.before.call(db), operation);
        exec(db, batch);
        if (batch.after != null) require(batch.after.call(db), operation);
    }
    private static void require(Integer ok, String operation) {
        if (ok != null && ok < 0) throw new IllegalStateException("R27_WRITE_PRECONDITION:" + operation);
    }

    /** True when the failure is the single writer being busy (deadline/interrupt), as opposed to a real write error. */
    static boolean writerBusy(Throwable failure) {
        for (Throwable t = failure; t != null; t = t.getCause()) {
            String m = String.valueOf(t.getMessage());
            if (m.contains("CP3_DB_BUSY_") || t instanceof InterruptedException || t instanceof android.database.sqlite.SQLiteDatabaseLockedException) return true;
            if (t.getCause() == t) break;
        }
        return false;
    }

    private synchronized void stat(long waited, boolean requeued) {
        long hour = System.currentTimeMillis() / 3_600_000L * 3_600_000L;
        if (hour != hourStart) { flush(); hourStart = hour; submits = longWaits = maxWait = requeues = 0; }
        submits++; if (waited >= MAX_WAIT_MS) longWaits++; maxWait = Math.max(maxWait, waited); if (requeued) requeues++;
    }
    /** Persists the hourly write-wait counters (called by the health task). */
    public synchronized void flush() {
        if (hourStart == 0 || store == null) return;
        final long h = hourStart, s = submits, w = longWaits, m = maxWait, r = requeues;
        try {
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R27_WRITE_WAIT", db -> {
                // minSdk 21: no UPSERT (SQLite < 3.24 on old devices).
                db.execSQL("INSERT OR IGNORE INTO r27_write_wait(hour_start) VALUES(?)", new Object[] {h});
                db.execSQL("UPDATE r27_write_wait SET submits=?,waits_over_200ms=?,max_wait_ms=max(max_wait_ms,?),requeues=? WHERE hour_start=?", new Object[] {s, w, m, r, h});
                return 0;
            });
        } catch (RuntimeException busy) { android.util.Log.w("STARCORE_R27", "write-wait receipt deferred", busy); }
    }

    static void exec(SQLiteDatabase db, Batch b) { for (Op op : b.ops) db.execSQL(op.sql, op.args); }
}

```

`app/src/main/java/com/ethk3/novaonepro/strategyv2/CommitEnvelopeR30R1.java`（关键组合目标；集成限制见上）：

```java
package com.ethk3.novaonepro.strategyv2;

/** One prepared logical step. Caller publishes only the state returned after atomic commit success. */
public final class CommitEnvelopeR30R1 {
    public enum Outcome { COMMITTED, RETRY_LATER, SOURCE_CONFLICT }
    public interface AtomicSink { Outcome commit(String stepId,long fence,long revision,byte[] immutableNextState); }
    private final String id;private final long fence,revision;private final byte[] next;
    private final java.util.concurrent.atomic.AtomicInteger state=new java.util.concurrent.atomic.AtomicInteger();
    public CommitEnvelopeR30R1(String id,long fence,long revision,byte[] next){
        this.id=ResearchContractR30R1.sha(id);if(fence<0||revision<0||next==null)throw new IllegalArgumentException("STEP_FENCE");
        this.fence=fence;this.revision=revision;this.next=next.clone();
    }
    public Outcome attempt(AtomicSink sink){
        int current=state.get();if(current==2)return Outcome.COMMITTED;if(current==3)return Outcome.SOURCE_CONFLICT;
        if(!state.compareAndSet(0,1))return Outcome.RETRY_LATER;
        try {
            Outcome result=java.util.Objects.requireNonNull(sink.commit(id,fence,revision,next.clone()));
            state.set(result==Outcome.COMMITTED?2:result==Outcome.SOURCE_CONFLICT?3:0);return result;
        } catch(RuntimeException|Error failure){state.set(0);throw failure;}
    }
    public byte[] publishedState(){if(state.get()!=2)throw new IllegalStateException("UNCOMMITTED_STATE");return next.clone();}
    public String id(){return id;}
}

```

`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchContractR30R1.java`（关键组合目标；集成限制见上）：

```java
package com.ethk3.novaonepro.strategyv2;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;

/** Immutable research identity. This object can never grant REAL authority. */
public final class ResearchContractR30R1 {
    public enum Role { TRAIN, SELECT, ONLINE_RESEARCH, CONFIRM, FUTURE, SYNTHETIC }
    public final String datasetHash, policyHash, abi, sourceCommit, inputHash;
    public final Role role;
    public final long fromOrdinal, throughOrdinal, availableAt, sourceRevision;

    public ResearchContractR30R1(String dataset, String policy, String abi, String source,
            String input, Role role, long from, long through, long available, long revision) {
        this.datasetHash=sha(dataset); this.policyHash=sha(policy); this.inputHash=sha(input);
        if(abi==null||abi.isEmpty()||source==null||!source.matches("[0-9a-f]{40}"))
            throw new IllegalArgumentException("RESEARCH_IDENTITY");
        if(from<0||through<from||available<0||revision<0)throw new IllegalArgumentException("RESEARCH_BOUNDARY");
        this.abi=abi;sourceCommit=source;this.role=Objects.requireNonNull(role);
        fromOrdinal=from;throughOrdinal=through;availableAt=available;sourceRevision=revision;
    }
    public void requireAdvice(long targetOrdinal,long preparedAt) {
        if(role!=Role.TRAIN&&role!=Role.SELECT&&role!=Role.ONLINE_RESEARCH)
            throw new IllegalStateException("FORMAL_OR_SYNTHETIC_LABELS_CANNOT_DRIVE_RESEARCH_ADVICE");
        if(throughOrdinal>=targetOrdinal||availableAt>preparedAt)
            throw new IllegalStateException("RESEARCH_ADVICE_PIT");
    }
    public String key(String consumerVersion) {
        if(consumerVersion==null||consumerVersion.isEmpty())throw new IllegalArgumentException("CONSUMER_VERSION");
        return hash(datasetHash+'|'+policyHash+'|'+abi+'|'+sourceCommit+'|'+inputHash+'|'+role.name()
                +'|'+fromOrdinal+'|'+throughOrdinal+'|'+availableAt+'|'+sourceRevision+'|'+consumerVersion);
    }
    public static String sha(String s){if(s==null||!s.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("SHA256_REQUIRED");return s;}
    public static String hash(String s){try{byte[] b=MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));StringBuilder x=new StringBuilder();for(byte v:b)x.append(String.format(java.util.Locale.ROOT,"%02x",v&255));return x.toString();}catch(java.security.NoSuchAlgorithmException e){throw new AssertionError(e);}}
}

```

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
