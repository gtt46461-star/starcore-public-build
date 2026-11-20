# R33.5 独立交叉复核记录

复核范围：B04 历史训练计数与恢复游标、百期窗口的标签时序与停用分母、生产覆盖层身份生成器，以及报告侧 7 个新增或修改的 Java 后态。未重复整套旧验收；本记录不代表 Android SQL 实机、APK、自然运行效果或预测胜率已经通过。

## 1. 本轮已闭合的具体反例

| 反例 | 修订与证据 |
| --- | --- |
| 首个 64 样本 Adam 批次只访问 55 个历史期，却提前报告全部 160 个训练期 | B04 报告按实际 shuffle 前缀计算去重历史期；完成一轮后最多报告 `ceil(trainCount/10)`。`b04/legacy-result.json` 和 `b04/fixed-result.json` 留存原型与修后实际 Java 结果。 |
| 正确散列但 `TRAIN/offset=64/order=null` 的 checkpoint 被接受，恢复后重洗牌且跳过原游标之前的样本 | B04 拒绝非零游标缺 shuffle 和不可能的终端 TRAIN 游标。实际恢复后下一批权重分歧的反例已验证。未改变优化器或正常数值公式。 |
| 百期独立 window bundle 的标签在开奖前落库、任务在开奖后完成，可通过旧 window 校验 | `check_window` 直接调用共享严格时序校验，要求标签 `settled_at >= max(open_time, first_seen_at)`，使用 manifest 绑定的同一冻结数据库并前后复核 SHA。 |
| 对完整未过滤日历强制 `closed.outcome==PASS`，误拒 100 个启用闭环期加 20 个合法停用期 | 仅启用机会的 trace failure 阻断通过；停用期失败保留单独列表。有链原件仍全部接受严格标签时序检查。120 期正例与晚停用 1 秒负例已真实执行。 |
| identity authoring 输入阶段误写 `BO4` 会被九阶段循环漏掉 | 根代理加精确阶段集、未知阶段拒绝与 consumed 全集校验；本代理直接执行真实 CLI 的 `BO4` 负例，确认 `UNKNOWN_REVIEW_STAGE:BO4` 且输出目录未建立。见 `OVERLAY_GUARD_RESULT.json`。 |
| 能力需求目录中三条施工文档路径不存在；临时生成脚本可重建旧目录或覆盖新接线 | 报告代理将三条路径改为真实 `README_R335.md`，生成器验证 8 条 sourceContract 存在并校验 Java 完整字节；临时 `build_payload.py` 已删除。 |

## 2. B04 边界与职责

固定生产文件 SHA-256：`f61d69c3e879a8aff104b39a0efeaea47b0ae8852170d59db2be12d0fecc18d6`。

增加的实际-owner边界检查覆盖 `trainCount=69`、首批的实际 shuffle 去重、整轮 69 个位置对应 7 个历史训练期、有效 checkpoint 解码、后续 epoch 的不一致计数降为未知，以及 online 更新不灌入历史 TRAIN 期数。历史期数只表示已访问历史训练输入，不表示独立未来证据。原数值探针仍通过 17,763 项检查，权重 SHA-256 保持 `5b17ecac4f369f0071031aa07e156232b5894be03363202146fa55308242705b`。

## 3. 生产身份生成器

对 `production/rebuild_overlay.py` 的当前后态复核确认：使用固定原 R967/R815 算法与原 Java 基线校验；逐阶段应用完整生产后态后调用原 `identity.identity()`、`write_outputs()` 和 `verify_outputs()`；策略哈希保持不变，旧资格兼容前身保持空集；B05 已有 pair 修订纳入累计源码，不以文字替换或硬编码训练规则哈希代替重算。

未知阶段、重复新增路径、重复原批次路径、重复原 source review、未消费输入均有拒绝条件。`BO4` 负例执行时生成器 SHA-256 为 `c2c1d48247834474e2c13a90716293ca45a7b969b9a8695ed78f6b45daf56f96`。全阶段重算与集成编译由根代理执行；这里不重复认领其结果。

## 4. 报告生产接线与证据界限

实际接线为 `ReportSnapshotR30` 的单次 SELECT 解码数据 → `captureSingleSelect` → `ReportObjectIndexR30.attach/capture` → `RepairDiagnosisR32` 的 JSON 与 Markdown 输出。capture 位于 hot/cold 故障行替换前，故冷档案查询结果不会被伪称为同一 SELECT 原件。

输入和输出都有具体上限：原 SELECT 对任务与故障投影实施行数、单元格和 inline 字节预算；新增 trace 最多 100 个任务、20 个故障、196,608 个投影字节、每行 32,768 字节；有限需求目录最多 64 项、每项最多 8 个 owner。共享 packet 只在 attach 入口验证一次，不对每个对象反复散列完整原件。

源码反射明确只证明指定类与精确参数签名的声明存在；使用 `Class.forName(..., false, loader)`，不执行反射目标。当前 inspection commit 由 BuildConfig 提供，与旧报告 source commit 不同即为 `EVIDENCE_MISSING`。owner 阻因、共享任务与历史 fence 被保留为观测，不能自动升级成对象根因、独立 replay 或运行效果；`mayAuthorizeReal` 保持 false。未知 commit 或非正整数水位不能绑定已验证观测。

最终报告 manifest SHA-256：`6dc94d10843247071ad066c8f0e0ebcd09bf3ad61e5448b4583e59a54bd7857b`。本代理重新散列 7 个生产文件，均与最终 manifest 一致。独立回读 `agents/reports/evidence/full-run-final/REPORT_CLOSURE_RECEIPT.json`，其 SHA-256 为 `743755a1cb33bb8b85388aaa1a153db56f76c1c7c7f37f7a338d27a5712db8ae`，记录 396 项生产 Java 宿主断言通过、1,359 个 Java 源文件类型检查通过、7 个生产文件字节验证通过。

本次指定范围内没有留下已确认而未交付修订的阻断缺陷。最终整合必须以新 production manifest、新 window checker 和最终 receipt 为准，不应回用早期 395 断言的便捷副本。
