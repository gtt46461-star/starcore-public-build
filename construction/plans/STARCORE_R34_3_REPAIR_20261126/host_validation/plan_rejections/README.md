# R34.3 方案验证器：真实正例和拒例

## 实际结果

本次对当前 `tools/verify_plan.py` 的**未修改副本**执行完整命令行验证：**1 个封印正例通过，8 个拒例均在指定门拒绝**。结果为 `actual/result.json`；每例原始 argv、stdout、stderr、封印清单及变更说明保存在 `actual/cases/<case>/`。

正例包含 273 个清单成员、44 个问题、96 个检测、125 个源锚点，以及 9 组／65 个修改步骤／62 个唯一文件的真实累计 `git apply --check` 和 `git apply`。这些数量是本次快照结果；脚本重跑时读取实际计划，不固定封印成员数量。

| 用例 | 封印处理 | 实际拒绝门 |
|---|---|---|
| 成员内容篡改 | 保留原封印，修改 README | `MEMBER_HASH:README.md` |
| 缺原检测报告 | 删除报告后合法重封印 | `FileNotFoundError`，精确缺失 `ORIGINAL_DIAGNOSTIC_173.zip` |
| 额外文件 | 增加未在封印中的普通文件 | `MEMBER_SET` |
| 96 个检测漏一项 | 修改内容和本地count后合法重封印 | `CHECK_SET` |
| 问题源锚点错误 | 保留真实source SHA，替换为基线没有的锚点，再合法重封印 | `ISSUE_SOURCE_ANCHOR` |
| 累计before错误 | 修改第二次出现文件的before原件及对应SHA，使单文件hash本地正确但不等于前组after，再合法重封印 | `CUMULATIVE_BEFORE` |
| 工程验收case漏一项 | 同步改本地计数后合法重封印 | `ACCEPTANCE_COUNT` |
| 向封印内写输出 | 有效封印不变，只传封印内 `--out` | `OUTPUT_MUST_BE_OUTSIDE_SEALED_PLAN`；输出文件未创建，副本库存不变 |

五个内容语义拒例在调用验证器前均再次核实**整个封印有效**，不是被成员哈希提前挡住。原始计划文件库存与源仓库状态在测试前后保持一致。临时方案副本已自动删除；原始执行日志保留。

## 可重复执行

```bash
python test_plan_rejections.py \
  --plan /absolute/path/to/STARCORE_R34_3_REPAIR_20261126 \
  --source-repo /absolute/path/to/source-repository \
  --out /absolute/external/path/plan-verifier-rejections
```

仅需要 Python 标准库和 Git；源仓库必须已有验证器指定的173基线对象。`--out` 是目录，必须位于原方案和源仓库之外。输入方案可已封印或未封印；脚本始终只在独立副本按 `STARCORE_R343_PLAN_MANIFEST_1` 建立测试封印。执行成功退出0，任一实际门与预期不符退出1，并保存 `result.json`。

输出目录包含结果及逐例日志，不包含完整变异方案副本。临时路径、耗时与日志SHA随重跑变化；拒绝类别、实际source/patch合同和正反结果可重复核验。

## 证据范围

这是**方案验证器合同测试**，没有修改验证器、生产补丁、真实方案或源代码；没有扩大验证器合同。它验证既定输入完整性、报告存在、覆盖数量、源码锚点、累计补丁身份、工程case数量和输出位置门的实际行为。

它不证明所有未列举畸形输入都会被拒绝，不把内容作者自行重封印当成真实性认证，也不证明所有施工语义正确。Android构建、设备运行、真实策略训练/结算和自然质量均未执行：`Android=NOT_RUN`、`physical_device=NOT_RUN`、`natural_quality=NOT_PROVEN`。
