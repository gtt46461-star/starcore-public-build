# 接口、SQL、状态机、测试与发布合同

## 1. 拟新增接口（不是声称仓库已有这些文件）

```java
interface StrategyKernelR25 {
  CapabilityManifest capability();
  Prediction predict(EventFrame frozenFrame, CommittedSnapshot state, DeadlineToken token);
  PreparedCheckpoint trainSlice(CommittedSnapshot base, FeedbackBatch labels, SliceBudget budget);
  MutationProposal mutate(CommittedSnapshot parent, TypedStructuralProposal proposal);
  AblationSpec ablate(AblationRequest request);
  byte[] serialize(CommittedSnapshot state);
  CommittedSnapshot restore(ArtifactManifest manifest, byte[] bytes);
}
```

Prediction包含policyHash/checkpointHash/featureFrameHash/inputThrough/target、10×10边际、必要风险分数、可执行单位置K3或明确拒绝、缺尺度与gap说明。校准概率、边际和风险分数不可混用字段。infer不得修改CommittedSnapshot；想更新状态必须返回PreparedCheckpoint。

PreparedCheckpoint包含baseVersion、新完整状态、训练事件列表、训练游标、耗时、取消状态和内容hash。必须走原数据库Owner的commit路径：旧版本已变则CAS失败，重读重算或按已注册确定性合并规则，不允许“最后写者赢”。

```text
begin short writer transaction
  verify active_snapshot.version == expectedVersion
  verify every consumed event is committed and matches draw_hash
  insert checkpoint manifest referring to already verified immutable bytes
  insert feedback_consumed(eventId, policyHash, checkpointHash)  // uniqueness
  update active_snapshot ... where version=expectedVersion
  require affectedRows == 1
commit
publish immutable snapshot from committed pointer
```

重复标签与冲突标签不同：前者幂等，后者隔离并报警，不用ON CONFLICT REPLACE静默覆盖。多模型独立消费按(event,policy)区分；公共特征统计由独立consumerId和游标控制，不因8个策略使用同特征而更新8次。

## 2. SQL参考的范围

`sql/r25_research_metadata.sql`只提供10张研究元数据/关联表；它不创建第二套draw、REAL、正式settlement，也不授权新数据库文件。正式链中的既有表名、主键及FK要在B00拿到全文后绑定。DDL由既有版本化单语句迁移执行，不得在业务循环临时execSQL整包。

AlphaReservation必须在揭示数据前事务化登记并受永久root余额限制。唯一键只能防重复ID，不能自行证明总alpha不超支；预算引擎要校验candidate/endpoint分配、同policy禁重复出生、关闭任务不回收已花预算。

## 3. 测试矩阵：按功能而不是按类数验收

| 类别 | 必测反例/正例 | 所需证据 |
|---|---|---|
| 身份 | 相近double、NaN/Inf、类型差异、外部map变化、跨语言编码、旧协议只读 | 黄金字节、hash、旧资产映射 |
| 时序 | 未来标签/归一化/索引/校准泄漏；前缀扰动 | 每层inputThrough与prefix diff |
| 原子/家族 | 输出形状、状态更新、恢复、结构消融、随机种子/死代码假多样性 | 逐家族独立结果，不用一个公共测试替代所有机制 |
| 精确读出 | 小n全排列、梯度差分、10×10行列和、1200集合概率 | 绝对/相对误差和可复算输入 |
| 训练 | 预测先于标签、消费幂等、真实权重/状态变化、取消任务不发布 | feedback/checkpoint链 |
| 恢复 | 文件写前/后、DB提交前/后、发布前/后分别崩溃 | 与不中断参考的下一预测及更新对比 |
| 证据 | UCB误判反例、低样本、高缺失、连续look、多候选、已暴露测试集 | 永久budget与cohort ledger |
| 稳定性 | 暂停/改名/换冠军不清零、假变点、长跑漂移 | 全局ACT连挂和风险状态 |
| 授权 | 全条件满足能入；每个条件删掉都不能入 | DecisionGate→canonical→Settlement全链，合成与真实分表 |
| 资源 | 全24家族登记但限并发、bytecap、报告压力、取消 | 设备P50/P95/P99、OOM、CPU/温度、writer等待 |
| 数据 | 重复、冲突、112缺口、跨日、5001→5000 | 原始hash与事务审计 |
| 报告 | 错误段、截断、分卷、取消、未知与0区别 | 28章齐全和数量守恒 |

负控必须跑“整个搜索与选优流程”，不只是用一个固定模型在随机数据测30%；否则无法测到挑选偏差。植入测试需至少包括跨位置滞后、状态持续时间、变点和组合模式四种已知机制，并验证相应消融使信号消失。合成测试检验能力，不增加一条自然样本。

## 4. 安卓设备与运行场景

沿用原T29—T34，不改旧ID：在实际可得设备上覆盖支持的API、低内存、用户当前机型同级配置。每次绑定设备型号/API/targetSdk/APKhash/数据库升级来源/省电设置/网络/屏幕状态/样本数。场景至少包括干净安装、保留数据覆盖安装、旧大报告取消、熄屏长跑、网络中断恢复、系统回收、用户强停后重开、前台服务timeout、数据库空间不足、时钟调整。

系统不允许后台运行时应显示限制与漏期原因，而不是制造“零漏期”。用户强制停止与普通任务被回收区别处理。真实无法测试的机型填NOT_RUN，不能用一个模拟器结果复制六行PASS。

## 5. CI与发布

流水线：source identity → secrets scan → unit/property tests → Java/Python parity → legacy migration fixtures → Android build → install/startup smoke → performance/device scenarios → report validation → reproducible manifest → signed artifact。

未授权修改公开/私有库或上传密钥。签名材料不进入源码/报告/压缩包。本次只交施工文档、参考代码和证据；没有签名或生成新APK。未来每批交付绑定提交、生成代码hash、APKhash、测试日志、未完成项，不能将旧APK更名冒充新版本。

回滚只切回此前仍有效的已提交模型指针及兼容App版本；数据库已前向迁移时必须验证旧App能读，不能默认安装旧APK即安全。模型回滚不回滚时间、证据、连挂或预算。

## 6. 一条本地复核命令

```bash
python run_host_checks.py
```

需要Python（R25纯标准库）、JDK；重跑R23神经参考还需要numpy与torch。依赖缺失应明确FAIL/NOT_RUN，不假装通过；可分别执行R25标准库测试。所有日志写入新的`evidence/current_run/`，不覆盖本轮保留的原始证据。该脚本只跑主机验证，不会授权REAL、上传仓库或操作用户设备。
