# FR-B03-02 57任务分清工作单位与日历新鲜度

优先级：P0。状态：设计交付，尚未Android/设备验收。原需求：FIX-03。

前驱：B02。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/TaskProgressR30R1.java`

动作：**ADD**。施工范围：完整新增类。

问题：phase/gen/policy游标被当开奖序号会产生错误滞后。

修改方式：区分workCursor、knownLatest、pending、oldestPendingOrdinal、commitClockOrdinal；M11与M12不同。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`NEW_FILE`。共享合并顺序：FR-B03-02。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeReportR26.java`

动作：**MODIFY**。施工范围：taskLivenessR30。

问题：schema无pending/last_committed_seq导致全UNKNOWN。

修改方式：改读新typed progress来源；旧字段缺失保持未知，不能统一max(cursor差)推断。

原方法锚点：RuntimeReportR26.taskLivenessR30(SQLiteDatabase db) L161–181

基线SHA256：`61358ab2b73ca5ba7b9a27146091d5b0fbb45724d6b5508b5bb70b8e89529610`。共享合并顺序：FR-B03-02。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportSnapshotR30.java`

动作：**MODIFY**。施工范围：taskLiveness/rowsets。

问题：只读不存在字段，57项无可判事实。

修改方式：连接typed task事实与Owner表，逐任务报告单位、依赖与源时间。

原方法锚点：ReportSnapshotR30.rowsets(Query q) L107–137；ReportSnapshotR30.taskLiveness(Object raw,String latest,Object total) L148–151

基线SHA256：`2dfb38a9bcb05d76cf5b72432dacd6428697d1a152ecf4aef53682deb9010360`。共享合并顺序：FR-B01-05 → FR-B03-02 → FR-B05-01。

## 实施步骤

1. r27-evolution使用PHASE，atomnet使用POSITION_SAMPLE，ABI/校准用POLICY，chart/health用TIME_TICK。
2. 57原入口逐项标ACTIVE/DELEGATED/BLOCKED/WAITING，委托项指向实际Owner。

## 测试与反证

- phase18460与calendar1200不相减；等新输入无误报；有pending五期不提交告警；最老训练源滞后单列。

## 验收

活跃任务事实可判或逐对象说明缺源；不把UNKNOWN计健康0。

## 恢复与回滚

退回只读UNKNOWN而非猜测；原任务继续运行。

## 关键代码

顺序补丁：`patches/B03_typed_progress_schema.patch`。必须按SERIES顺序应用，一份跨文件补丁只应用一次。

已预写目标文件：`code/app/src/main/java/com/ethk3/novaonepro/strategyv2/TaskProgressR30R1.java`。它是本包关键补丁叠加后的组合目标，不是允许覆盖其他卡修改的单独发布文件。

### 可直接审阅的关键代码

`app/src/main/java/com/ethk3/novaonepro/strategyv2/TaskProgressR30R1.java`（关键组合目标；集成限制见上）：

```java
package com.ethk3.novaonepro.strategyv2;

/** Work cursor, source watermark and commit clock are different units. */
public final class TaskProgressR30R1 {
    public enum Unit { ISSUE, POSITION_SAMPLE, GENERATION, PHASE, POLICY, TIME_TICK }
    public enum State { UNKNOWN, BLOCKED_DEPENDENCY, IDLE_WAITING_INPUT, RUNNING, BACKLOG, STALLED }
    public final Unit unit;public final long cursor,latest,pending,oldestPendingOrdinal,commitClockOrdinal;
    public TaskProgressR30R1(Unit unit,long cursor,long latest,long pending,long oldest,long committedClock){
        this.unit=java.util.Objects.requireNonNull(unit);this.cursor=cursor;this.latest=latest;this.pending=pending;oldestPendingOrdinal=oldest;commitClockOrdinal=committedClock;
        if(cursor<0||latest<cursor||pending<0||oldest< -1||committedClock< -1)throw new IllegalArgumentException("TASK_PROGRESS_UNITS");
        if(pending==0&&oldest!=-1)throw new IllegalArgumentException("TASK_EMPTY_HAS_OLDEST");
    }
    public State state(long observedCalendarOrdinal,boolean dependency,boolean running){
        if(dependency)return State.BLOCKED_DEPENDENCY;
        if(pending==0)return State.IDLE_WAITING_INPUT;
        if(commitClockOrdinal<0||observedCalendarOrdinal<commitClockOrdinal)return State.UNKNOWN;
        if(observedCalendarOrdinal-commitClockOrdinal>=5)return State.STALLED;
        return running?State.RUNNING:State.BACKLOG;
    }
    public Long oldestLag(long observedCalendarOrdinal){return pending==0?null:oldestPendingOrdinal<0||observedCalendarOrdinal<oldestPendingOrdinal?null:observedCalendarOrdinal-oldestPendingOrdinal;}
}

```

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
