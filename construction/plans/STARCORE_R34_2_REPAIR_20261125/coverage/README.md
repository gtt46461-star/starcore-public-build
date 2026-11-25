# 检测报告与升级要求全量覆盖

这些表逐条保留原始身份、状态和要求，并映射到当前57项施工问题。它们不是把每条观察都另算一个缺陷。

|覆盖来源|数量|逐项入口|
|---|---:|---|
|检测检查|96|[完整检查与处置](REPORT_CHECKS_96_CROSSWALK.md)；[结构化表](REPORT_CHECKS_96_CROSSWALK.json)|
|质量/工程指标|44|[原定义、状态与补证](METRICS_44_CROSSWALK.md)；[结构化表](METRICS_44_CROSSWALK.json)|
|升级方向对象|165|[原owner、nextAction与验证入口](DIRECTIONS_165_CROSSWALK.md)；[结构化表](DIRECTIONS_165_CROSSWALK.json)|
|九层失败诊断|9|[逐层事实源、归因实验与边界](NINE_FAILURE_LAYERS.md)；[结构化表](NINE_FAILURE_LAYERS.json)|
|旧R34.1工单/验收/文件卡|62/152/295|[旧工单状态与承接](../inheritance/R341_WORK_ORDER_STATUS.md)；[文件卡计数边界](../inheritance/R341_FILE_CARD_COUNT_SCOPE.json)|
|报告历史需求/文件卡|1832/299|[逐行原字节保留说明](../inheritance/REPORT_LEDGER_INHERITANCE.md)|

## 解释规则

- 96项的4 PASS、4 FAIL、7 WARN、21 NOT_PROVEN、44 OBSERVED、16 NOT_APPLICABLE来自`health`归一，原`domain_outcome`更细状态另存。执行完成不等于检查健康。
- 44指标保留原数值、定义、角色、分母和测量来源；有计数不等于经过质量验证。缺来源、未部署、陈旧合成来源分别保持原状态。
- 165对象的键是`group + id`。82能力内含20矩阵，另有20矩阵投影分组；它们不是40种不同矩阵。81静态callable不能代替实际生产和消费。
- RSEC-074的14子Finding逐项保留。RSEC-010旧口径7/30与RSEC-052严格当前1/30须按窗口/过滤条件对齐；不得选较高值签通过。RSEC-029流程文字不是实际调度收据，主链顺序以152生产源码和运行证据为准。
- 旧295文件卡归一294实际路径：历史F238工具替代路径与F247同归`tools/r341/verify_integrated_chain.py`。两条原合同都保留，同一文件只有一条顺序变更链。
- PASS或不适用项仍保留回归/参考责任；只有新的证据才重新判定。原历史“未检查”不能变成确定未实现或自动完成。

每个附录保留原RT/RES/PLAN/ROOT问题身份，并补最终R342施工编号。全部57项的具体文件和方法见[增量文件矩阵](../FILE_MODIFICATION_MATRIX.md)。
