`R26_REPORT.json` 的 `strategyAssessment` 是逐策略检查结果，不授予运行资格。它随现有40章报告一起导出。

- `observed`：保留窗口内的真实独立期数、ACT/命中数、命中率、覆盖率、每720期分段，以及256档原始概率近似Brier。它不是终身统计或正式资格窗口；零样本的胜率为null。
- `qualification` 与 `formalLowerBound`：最近一次已保存的预登记检验，单独保留起止期、样本、look、alpha、配对下界和校准结果。它不是当前资格，报告不重新审计完整上游来源链。
- `canonicalPolicy`、`implementation`、`lineage`：实际政策定义、实现及保留的谱系记录。研究采纳、资格记录和提升记录分别计数，不能从生成次数推断一代比一代强。
- `nextResearch`：按缺口、低覆盖、低命中、不稳定、配对退化或校准失败给出建议、假设和反证条件；明确标记建议尚未执行。调整模型或选择规则后需要新的政策身份和出生点。
- 错误记录清空相应质量数据；一项策略错误不掩盖其他策略。最多64项策略，明确列出未导出的数量。

验证：`ReportAssessmentHost verification/r64/sqlite_bridge.py` 调用与Android原生用例相同的检查，覆盖空数据、一次命中、正式窗口、坏通过记录、缺段、65策略截断与错误隔离。`synthetic-report.json.gz` 是故意含坏数据的合成测试样本，不代表用户软件胜率。

`failureDirectedResearch` 记录已登记的失败驱动研究。`birthRecord` 保留原失败窗口、样本、证据摘要和反证条件；`currentChildren` 从实际候选表读取训练状态、已消费标签和保留的诊断条数。固定窗口只消费一次，重新出生不继承父代胜率或资格。普通种子与对照定义相同时共用同一候选。`FailureResearchHost` 调用与原生相同的工厂出生、失败回滚、重复游标及子代真实训练用例。它使用合成失败记录验证执行能力，不证明策略质量。
