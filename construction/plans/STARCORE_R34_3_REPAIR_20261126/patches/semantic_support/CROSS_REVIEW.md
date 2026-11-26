# Semantic rebinder 交叉复核

## 结论

对当前候选 `rebind_semantic_inputs.py` 与 installed173 原 `scripts/r967_semantic_identity.py` 的 `build`、`invalidation_r82`、`controlled_paths`，及原 `tools/r33/advance_release_version.py` 的 `repo_version_lock` 完整对读后，没有发现需要再修改的确定实现缺陷。

已独立执行候选测试：**70 assertions、12 batches PASS**。本次复跑使用已封印的 `recovered/plan/STARCORE_R34_3_REPAIR_20261126`；不是最终重封后的 plan_work 源码身份验收。最新3处after变更、semantic support组、最终manifest身份应由作者/root的最终轮测试覆盖。

## 已核对的边界

- source baseline固定为 `4d1de9d83692635b5c5ce39144fece1c8a31c8ac`，每批源码是sealed PATCHSET的精确累积态；新叶子和既有修改叶子均需显式审查。
- 原1589条分类角色不变；12个新增生产叶子逐路径分类，不借此将既有NUMERIC降为PROVENANCE。
- 原R967 verifier、immutable R81 baseline、历史SU1 review和统计policy原件保持不变；生成当前两份identity后仍必须通过原R967验证。
- R82 proof的predecessors为空，没有旧资格或证书继承；历史DEV模型原件不因此变成新版本未来证据。
- 纯version元数据变更保持原identity字节与training rule；真正数值语义修改按原保守合同产生新rule。
- journal before必须是已审查的整对prefix；恢复逐项仅允许exact before/after，拒绝用户另改的identity、篡改journal、伪造before和repo内journal；首个identity替换后中断能够恢复。
- 复用原Git版本锁；不另增写入所有权。

## 验证缺口移交

候选测试最初直接调用evaluate/apply_journal，没有穿过main的published plan pin、两份sealed plan检查及import入口。已向作者提出，作者已承接真实CLI preflight/apply/check正例和错seal反例；这是入口集成验收补充，不是本轮已经确认的代码缺陷。

本交叉复核没有执行Android构建、设备验收、自然质量或真实出手试验，不能从host PASS推导这些验收已完成。

## 本轮审查脚本身份

- rebinder SHA256：`0b0fb61f156dcac98eab3b64fa60e5777468eff4b382fb10cfc5880b90946df0`
- test SHA256：`610082ea6a5798109c405feaedc976140c335d5c4dc66ead2b3c89d94081de92`
- 原始结果：`audit/research/semantic_crosscheck_result.json`
