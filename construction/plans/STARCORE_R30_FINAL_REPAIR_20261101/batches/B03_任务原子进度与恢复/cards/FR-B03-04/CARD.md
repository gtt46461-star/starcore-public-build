# FR-B03-04 有界重试和状态发布协议

优先级：P0。状态：设计交付，尚未Android/设备验收。原需求：FIX-19/FIX-04。

前驱：B02。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeR27.java`

动作：**MODIFY**。施工范围：submit/EvolutionTask/DatasetTask/AtomNetTask。

问题：submit while循环可长期占研究worker；evo先变内存再提交有失败恢复风险。

修改方式：逐任务使用CommitEnvelope保存prepared step；一次提交预算200ms后返回RETRY；失败恢复同一原始状态，成功才发布。

原方法锚点：RuntimeR27.submit(ResearchPoolR27.Worker w,String op,ResearchWriteQueueR27.Batch b) L57–59；RuntimeR27$DatasetTask.id() L63–63；RuntimeR27$DatasetTask.step(ResearchPoolR27.Worker w) L64–81；RuntimeR27$EvolutionTask.id() L86–86；RuntimeR27$EvolutionTask.step(ResearchPoolR27.Worker w) L87–125；RuntimeR27$AtomNetTask.id() L343–343；RuntimeR27$AtomNetTask.step(ResearchPoolR27.Worker w) L344–344

基线SHA256：`ce5740d0d39e3e7f1ec6824614348713ee258535a92b8a46a600e4a423c71d2b`。共享合并顺序：FR-B02-05 → FR-B03-04 → FR-B04-05 → FR-B06-02 → FR-B07-04。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchPoolR27.java`

动作：**MODIFY**。施工范围：loop/next。

问题：一个任务写重试可饿死其他任务。

修改方式：重试记notBefore且让出worker；prepared step有界，取消/源冲突可恢复。

原方法锚点：ResearchPoolR27.loop(Worker w) L86–118；ResearchPoolR27.next() L123–134

基线SHA256：`e80ba04df767f5130b537c3aa70fa5f50a9c9f4cc1b5bd309ca13815b0a85f05`。共享合并顺序：FR-B03-03 → FR-B03-04 → FR-B03-05 → FR-B07-04 → FR-B08-05。

## 实施步骤

1. 不能把submit简单改成return false让旧调用者继续发布；必须先逐Owner改成提交后发布。
2. Evolution恢复完整snapshot+RNG+pending列表；dataset/cache绑定同一版本；AtomNet原copy-before-compute保留。

## 测试与反证

- 每个阶段注入BUSY/取消/SQLite异常后与不中断轨迹一致；其他活跃任务持续获服务。

## 验收

单次worker占用有界、重复学习0、失败不跳阶段。

## 恢复与回滚

恢复最近成功数据库快照，不保留领先内存状态。

## 关键代码

本卡尚未给出完整生产实现；按本卡明确接口与测试施工。不能标“已完成”。

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
