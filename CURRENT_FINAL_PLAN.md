<!-- R30_CURRENT_PLAN_POINTER -->
# 当前最终施工方案：STARCORE R30（2026-10-31）

**执行入口：[construction/plans/STARCORE_R30_FINAL_20261031/README.md](construction/plans/STARCORE_R30_FINAL_20261031/README.md)** · 施工方逐步指令：[START_HERE_施工方必读.md](construction/plans/STARCORE_R30_FINAL_20261031/START_HERE_施工方必读.md) · 单文件可读版：[STARCORE_R30_完整施工方案.html](construction/plans/STARCORE_R30_FINAL_20261031/STARCORE_R30_完整施工方案.html) · 完整ZIP：[STARCORE_R30_FINAL_20261031.zip](construction/plans/STARCORE_R30_FINAL_20261031.zip)

- 12 批次（B00—B10 + 条件批次 B11）、57 张施工卡、44 项指标（ID 与目标不变）；R29 的 52 张卡全部映射。
- **应用基线：已验收的 CP06 `bb607cf5ab69`**；施工分支 `construction/r30-20261031`（由 B00-01 新建）。R29 CP07—CP16（`construction/r29-20261029@9ff38ff`）不并入，打标签封存。
- B01 改为“唯一写锁持有时长治理 + 后台长事务切片”，不拆库；独立研究库降为有真机数据门槛的条件批次 B11。手机上装过 R29 CP07+ 包的遗留触发器/独立库由 B00-03 启动时回收。
- 关键代码（18 个新类、4 个按卡补丁、12 个测试）已对 bb607cf 编译并测试：HOST 31/31、SQL 合同 19/19、逐卡累积编译 12/12、补丁按序应用 4/4、方案自检 9/9。复现：`bash construction/plans/STARCORE_R30_FINAL_20261031/tools/run_host_verification.sh <bb607cf检出>`。
- 本次只交付方案：未改应用分支、未构建 APK、未上设备；设备指标全部 NOT_RUN。≥34% 命中与约 15% 频率仍是待验证目标，证书门与 REAL 规则不变。

以下为此前 R29 入口（历史保留）。
<!-- /R30_CURRENT_PLAN_POINTER -->

<!-- R29_CHECKPOINT06_POINTER -->
> **当前 Android 应用源码：[bb607cf5ab69](https://github.com/gtt46461-star/starcore-public-build/commit/bb607cf5ab69f7286f859e43f87ada85a5bc456d)，施工分支 [`construction/r29-20261029`](https://github.com/gtt46461-star/starcore-public-build/tree/construction/r29-20261029)。** main仅发布方案入口，不是最新应用代码。
>
> R29.1 checkpoint06（577291006）是修复验证检查点，**尚未完整签收11批/52卡/44指标，也未验证自然命中率**。[施工账本](https://github.com/gtt46461-star/starcore-public-build/blob/construction/r29-20261029/construction/r29/STATUS.md)。
>
> APK按原安装证书选择：[原R13/R26证书](https://github.com/gtt46461-star/starcore-public-build/releases/download/r29-official-577291006-bb607cf5ab69/STARCORE-R29.1-bb607cf5ab69-release.apk)；[既有R27证书](https://github.com/gtt46461-star/starcore-public-build/releases/download/r29-CP06-R27-bb607cf5ab69/STARCORE-R29.1-bb607cf5ab69-release.apk)。二者不能交叉覆盖。真机长测与自然质量分别待验，不能把此检查点称为全部方案已完成。
<!-- /R29_CHECKPOINT06_POINTER -->

# 当前最终施工方案：STARCORE R29 · R29.1复核修订（2026-10-29）

执行入口：[construction/plans/STARCORE_R29_FINAL_20261029/README.md](construction/plans/STARCORE_R29_FINAL_20261029/README.md)。

完整ZIP：[STARCORE_R29_FINAL_20261029.zip](construction/plans/STARCORE_R29_FINAL_20261029.zip)。

11批次、52卡、44项指标；原1832需求/299文件卡保全，R28的42卡逐项映射。

方案发布时核对的历史应用基线为 `construction/r27-20261028@f9bdddb97291d9d96b5aba940b31d03369c220e2`，报告源码为 `7d3e0e2271ce2a734ca69f2fca8527c00222bba7`。**main当前仅更新方案，不能当作最新应用源分支。** 开工重查远端差异。

本次仅交付方案与复核工具，未修改应用代码、未生成新APK。高胜率/稳定性是待验证目标。与旧方案冲突按R29 docs/01裁决；历史功能、历史训练、图表、原子矩阵及原账本不得丢弃。

当前规则提案为R29_GATE_V2（未实施/未启用）。复核修订：[docs/12_最终复核与修订.md](construction/plans/STARCORE_R29_FINAL_20261029/docs/12_最终复核与修订.md)，补齐校准支持、迟到结果恢复、乱序资格消费和频率分母。完整ZIP与解压内容同步；原有功能与原账本继续保全。

