# FR-B03-01 唯一schemaOwner增加研究物化表

优先级：P0。状态：设计交付，尚未Android/设备验收。原需求：FIX-03/FIX-12。

前驱：B02。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/SchemaR27.java`

动作：**MODIFY**。施工范围：ensure。

问题：新增研究表不能由报告/页面临时创建。

修改方式：在既有唯一writer里幂等执行ResearchSchemaR30R1.DDL，迁移前后校验。

原方法锚点：SchemaR27.ensure(SQLiteDatabase db) L73–98

基线SHA256：`a9be5a2b116922f478554fff3e0d990ff53e433d278e0bd94118458a2d0cc305`。共享合并顺序：FR-B03-01 → FR-B10-04。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchSchemaR30R1.java`

动作：**ADD**。施工范围：完整新增类。

问题：需要分析产物/处方/消费/图缓存/任务事实的最少物理存储。

修改方式：只新增6表+索引/不可变消费触发器；16逻辑视图复用原表，不额外造16套真相。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`NEW_FILE`。共享合并顺序：FR-B03-01。

## 实施步骤

1. 应用B03 schema补丁，旧库已有行不清理。
2. schema readiness先于任何新任务/新页面；失败维持BLOCKED_SCHEMA，不能在readonly中补表。

## 测试与反证

- 连续执行两次DDL、升级中断、老库、磁盘满；SQLite完整性及原行数不变。

## 验收

新表幂等可恢复且只有一个schema writer。

## 恢复与回滚

保持原schema和原数据可用；新增表停止消费，不DROP保护数据。

## 关键代码

顺序补丁：`patches/B03_typed_progress_schema.patch`。必须按SERIES顺序应用，一份跨文件补丁只应用一次。

已预写目标文件：`code/app/src/main/java/com/ethk3/novaonepro/strategyv2/SchemaR27.java`。它是本包关键补丁叠加后的组合目标，不是允许覆盖其他卡修改的单独发布文件。

已预写目标文件：`code/app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchSchemaR30R1.java`。它是本包关键补丁叠加后的组合目标，不是允许覆盖其他卡修改的单独发布文件。

### 可直接审阅的关键代码

`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchSchemaR30R1.java`（关键组合目标；集成限制见上）：

```java
package com.ethk3.novaonepro.strategyv2;

/** Additive DDL only. Invoke from SchemaR27.ensure's existing writer, never from a report read. */
public final class ResearchSchemaR30R1 {
    private ResearchSchemaR30R1(){}
    public static final int REVISION=1;
    public static final String[] DDL={
      "CREATE TABLE IF NOT EXISTS r30r1_analysis_run(run_id TEXT PRIMARY KEY,dataset_hash TEXT NOT NULL,policy_hash TEXT NOT NULL,role TEXT NOT NULL CHECK(role IN ('TRAIN','SELECT','ONLINE_RESEARCH','CONFIRM','FUTURE','SYNTHETIC')),source_commit TEXT NOT NULL,source_revision INTEGER NOT NULL,from_ordinal INTEGER NOT NULL,through_ordinal INTEGER NOT NULL,contract_hash TEXT NOT NULL,status TEXT NOT NULL,summary_json TEXT NOT NULL,recorded_at INTEGER NOT NULL,CHECK(from_ordinal<=through_ordinal))",
      "CREATE TABLE IF NOT EXISTS r30r1_research_measure(run_id TEXT NOT NULL,measure_key TEXT NOT NULL,kind TEXT NOT NULL,parent_hash TEXT NOT NULL,block_id TEXT NOT NULL,from_ordinal INTEGER NOT NULL,to_ordinal INTEGER NOT NULL,evaluated_calendar_issues INTEGER NOT NULL CHECK(evaluated_calendar_issues>=0),mean_gain REAL,lower95 REAL,upper95 REAL,payload_json TEXT NOT NULL,PRIMARY KEY(run_id,measure_key),FOREIGN KEY(run_id) REFERENCES r30r1_analysis_run(run_id),CHECK(from_ordinal<=to_ordinal),CHECK(lower95 IS NULL OR upper95 IS NULL OR lower95<=upper95))",
      "CREATE TABLE IF NOT EXISTS r30r1_proposal(proposal_id TEXT PRIMARY KEY,run_id TEXT NOT NULL,parent_hash TEXT NOT NULL,kind TEXT NOT NULL,payload_json TEXT NOT NULL,payload_hash TEXT NOT NULL,state TEXT NOT NULL CHECK(state IN ('READY','CONSUMED','REJECTED','STALE')),reason TEXT NOT NULL,created_at INTEGER NOT NULL,FOREIGN KEY(run_id) REFERENCES r30r1_analysis_run(run_id))",
      "CREATE TABLE IF NOT EXISTS r30r1_consume(proposal_id TEXT PRIMARY KEY,child_policy_hash TEXT NOT NULL,source_generation INTEGER NOT NULL,consumer_version TEXT NOT NULL,receipt_hash TEXT NOT NULL,consumed_at INTEGER NOT NULL,FOREIGN KEY(proposal_id) REFERENCES r30r1_proposal(proposal_id))",
      "CREATE TABLE IF NOT EXISTS r30r1_chart_cache(view_id TEXT NOT NULL,frame_id TEXT NOT NULL,run_id TEXT NOT NULL,payload_json TEXT NOT NULL,payload_hash TEXT NOT NULL,updated_at INTEGER NOT NULL,PRIMARY KEY(view_id,frame_id),FOREIGN KEY(run_id) REFERENCES r30r1_analysis_run(run_id))",
      "CREATE TABLE IF NOT EXISTS r30r1_task_progress(task_id TEXT PRIMARY KEY,owner_version TEXT NOT NULL,work_unit TEXT NOT NULL,work_cursor INTEGER NOT NULL,known_latest INTEGER NOT NULL,pending INTEGER,oldest_pending_ordinal INTEGER,last_commit_clock_ordinal INTEGER,completed_input_ordinal INTEGER,fence INTEGER NOT NULL,session_id TEXT NOT NULL,updated_at INTEGER NOT NULL,CHECK(work_cursor>=0 AND known_latest>=work_cursor),CHECK(pending IS NULL OR pending>=0))",
      "CREATE INDEX IF NOT EXISTS r30r1_proposal_ready ON r30r1_proposal(state,created_at,proposal_id)",
      "CREATE INDEX IF NOT EXISTS r30r1_measure_parent ON r30r1_research_measure(parent_hash,kind,from_ordinal)",
      "CREATE INDEX IF NOT EXISTS r30r1_chart_latest ON r30r1_chart_cache(view_id,updated_at DESC)",
      "CREATE TRIGGER IF NOT EXISTS r30r1_consume_immutable BEFORE UPDATE ON r30r1_consume BEGIN SELECT RAISE(ABORT,'RESEARCH_CONSUMPTION_IMMUTABLE'); END"
    };
}

```

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
