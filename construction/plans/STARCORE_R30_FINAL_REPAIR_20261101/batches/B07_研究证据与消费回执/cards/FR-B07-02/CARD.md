# FR-B07-02 分析、处方、消费和图缓存同事务

优先级：P1。状态：设计交付，尚未Android/设备验收。原需求：FIX-14/FIX-19。

前驱：B04, B06。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchStoreR30R1.java`

动作：**EXTEND_NEW_FILE**。施工范围：paired/consume。

问题：新增链路可能多writer或部分提交。

修改方式：分析run/measure/proposal/chart同一现有writer；消费与新子代/游标同batch；源revision/fence组合守卫。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`NEW_FILE`。共享合并顺序：FR-B03-03 → FR-B07-02 → FR-B07-04 → FR-B07-05 → FR-B08-05。

## 实施步骤

1. 应用完整Store关键代码并合并B03任务事实接口，不用整文件覆盖其前序增量。
2. 角色/输入/candidate/分析合同hash进身份，proposal被消费一次；同一输入重复执行不增加自然样本。

## 测试与反证

- 分析写成功但proposal写失败全回滚；消费后cursor失败全回滚；源版本冲突不发布。

## 验收

输入→分析→处方→实际子代→消费回执可追溯，无半条链。

## 恢复与回滚

冻结新proposal消费，不删证据或重置已消费状态。

## 关键代码

顺序补丁：`patches/B07_paired_research_receipts.patch`。必须按SERIES顺序应用，一份跨文件补丁只应用一次。

已预写目标文件：`code/app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchStoreR30R1.java`。它是本包关键补丁叠加后的组合目标，不是允许覆盖其他卡修改的单独发布文件。

### 可直接审阅的关键代码

`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchStoreR30R1.java`（关键组合目标；集成限制见上）：

```java
package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;

/** One existing research writer commits the analytical result, proposal and cache frame together. */
public final class ResearchStoreR30R1 {
    private ResearchStoreR30R1(){}
    public static ResearchWriteQueueR27.Batch paired(ResearchContractR30R1 c,ResearchGainR30R1.Estimate e,
            ResearchAdviceR30R1 advice,String measuredCounterfactualJson,String measuredCounterfactualHash,long now){
        if(!advice.evidenceKey.equals(c.key("PAIRED_COMPONENT_ADVICE_V1|"+e.analysisContractHash)))throw new IllegalStateException("ANALYSIS_RECEIPT_IDENTITY");
        PolicySpecR27 without=PolicySpecR27.parse(measuredCounterfactualJson);
        if(!without.policyHash().equals(ResearchContractR30R1.sha(measuredCounterfactualHash)))throw new IllegalStateException("ANALYSIS_CANDIDATE_HASH");
        String run=c.key("PAIRED_COMPONENT_ANALYSIS_V1|"+e.analysisContractHash+'|'+advice.componentPath);Map<String,Object> summary=new LinkedHashMap<>();
        summary.put("status",e.status);summary.put("observedCalendarIssues",(long)e.observedCalendarIssues);summary.put("evaluatedIssues",(long)e.evaluatedIssues);summary.put("completeBlocks",(long)e.completeBlocks);summary.put("incompleteBlocks",(long)e.incompleteBlocks);summary.put("incompleteFullBlocks",(long)e.incompleteFullBlocks);summary.put("excludedTailIssues",(long)e.excludedTailIssues);summary.put("missingIssues",(long)e.missingIssues);summary.put("mean",e.meanBlockGain);summary.put("lower95",e.lower95);summary.put("upper95",e.upper95);
        ResearchWriteQueueR27.Batch b=new ResearchWriteQueueR27.Batch();
        b.guardBefore(db->SchemaR27.liveSourceRevision(db)==c.sourceRevision&&SchemaR27.liveSourceTriggersValid(db)?0:-1);
        b.add("INSERT OR IGNORE INTO r30r1_analysis_run VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",run,c.datasetHash,c.policyHash,c.role.name(),c.sourceCommit,c.sourceRevision,c.fromOrdinal,c.throughOrdinal,e.analysisContractHash,"COMMITTED",JsonR27.write(summary),now);
        b.add("INSERT OR IGNORE INTO r30r1_research_measure VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",run,advice.componentPath,"ATOM_PAIRED_GAIN",c.policyHash,"REGISTERED_CALENDAR_BLOCKS",c.fromOrdinal,c.throughOrdinal,e.evaluatedIssues,e.meanBlockGain,e.lower95,e.upper95,JsonR27.write(summary));
        if(advice.action==ResearchAdviceR30R1.Action.PRUNE_COMPONENT){Map<String,Object> p=new LinkedHashMap<>();p.put("componentPath",advice.componentPath);p.put("candidate",measuredCounterfactualJson);p.put("candidateHash",measuredCounterfactualHash);p.put("evidenceKey",advice.evidenceKey);
            String payload=JsonR27.write(p);b.add("INSERT OR IGNORE INTO r30r1_proposal VALUES(?,?,?,?,?,?,?,?,?)",advice.proposalId,run,c.policyHash,advice.action.name(),payload,ResearchContractR30R1.hash(payload),"READY",advice.reason,now);}
        Map<String,Object> frame=new LinkedHashMap<>(summary);frame.put("viewId","R06");frame.put("runId",run);frame.put("role",c.role.name());frame.put("parentHash",c.policyHash);frame.put("componentPath",advice.componentPath);frame.put("proposalId",advice.proposalId);frame.put("advice",advice.action.name());frame.put("reason",advice.reason);frame.put("researchOnly",true);
        String payload=JsonR27.write(frame),hash=ResearchContractR30R1.hash(payload);
        b.add("INSERT OR IGNORE INTO r30r1_chart_cache VALUES(?,?,?,?,?,?)","R06",hash,run,payload,hash,now);return b;
    }
    /** Invoke on the SAME batch that writes the child policy, pending generation and evolution cursor. */
    public static void consume(ResearchWriteQueueR27.Batch batch,String proposal,String child,int generation,String consumer,long now){
        ResearchContractR30R1.sha(proposal);ResearchContractR30R1.sha(child);
        batch.guardBefore(db->{try(Cursor c=db.rawQuery("SELECT state FROM r30r1_proposal WHERE proposal_id=?",new String[]{proposal})){return c.moveToFirst()&&"READY".equals(c.getString(0))?0:-1;}});
        String receipt=ResearchContractR30R1.hash(proposal+'|'+child+'|'+generation+'|'+consumer);
        batch.add("INSERT INTO r30r1_consume VALUES(?,?,?,?,?,?)",proposal,child,generation,consumer,receipt,now);
        batch.add("UPDATE r30r1_proposal SET state='CONSUMED' WHERE proposal_id=? AND state='READY'",proposal);
    }
}

```

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
