# 当前唯一施工入口：STARCORE R34.3-173 · 安装173现场返修 · 修订2

**2026-11-26｜44项主问题、50条审计发现｜12批施工｜10组精确补丁、67步文件修改、64个不同文件。**

应用基线固定为 **577311173 / `4d1de9d83692635b5c5ce39144fece1c8a31c8ac`**。main维护方案；应用从该提交或已经核验的后代增量施工。禁止旧152/122整文件后态覆盖173。

1. [完整施工方案](construction/plans/STARCORE_R34_3_REPAIR_20261126/CONSTRUCTION_PLAN.md)
2. [全部问题、报告/源码对照和处理](construction/plans/STARCORE_R34_3_REPAIR_20261126/ISSUES.md)
3. [可直接交施工方的完整提示词](construction/plans/STARCORE_R34_3_REPAIR_20261126/CODEX_EXECUTE.md)
4. [12批施工顺序](construction/plans/STARCORE_R34_3_REPAIR_20261126/BATCH_INDEX.md)
5. [精确补丁及完整替换文件](construction/plans/STARCORE_R34_3_REPAIR_20261126/PATCHSET.json) / [逐文件修改矩阵](construction/plans/STARCORE_R34_3_REPAIR_20261126/FILE_MODIFICATION_MATRIX.md)
6. [24个F族+15个现代族的实际数学实现](construction/plans/STARCORE_R34_3_REPAIR_20261126/specs/MATHEMATICAL_MECHANISMS.md)
7. [96/44/165/63/801全量覆盖](construction/plans/STARCORE_R34_3_REPAIR_20261126/coverage/README.md)
8. [旧57问题在173的现状](construction/plans/STARCORE_R34_3_REPAIR_20261126/inheritance/R342_ISSUES_57_CURRENT_STATUS.md) / [继承合同](construction/plans/STARCORE_R34_3_REPAIR_20261126/inheritance/README.md)
9. [验收矩阵](construction/plans/STARCORE_R34_3_REPAIR_20261126/ACCEPTANCE_MATRIX.md) / [实际自检](construction/plans/STARCORE_R34_3_REPAIR_20261126/SELF_CHECK.md)
10. [原始上传检测报告ZIP，字节未改](construction/plans/STARCORE_R34_3_REPAIR_20261126/report/ORIGINAL_DIAGNOSTIC_173.zip)
11. [修订2续审与候选代码修正](construction/plans/STARCORE_R34_3_REPAIR_20261126/CONTINUATION_REVIEW.md)
12. [构建身份完整命令与逐角色审阅](construction/plans/STARCORE_R34_3_REPAIR_20261126/patches/semantic_support/REVIEW_AND_TESTS.md)

计划名R34.3-173与APK系列分开；保留原R34.2-B01…B12版本系列、原包名签名和四代封印，新增fieldRepair173身份。delivery与semantic_support在B01先行；每批执行当批源码→四文件版本→原R967/R82当前身份→提交构建，不等B12才发现构建阻断。

本修订通过24条统一主机命令、128源码锚点与累计补丁核验，44指标/96检测逐项生产职责通过2686条追溯断言。测试范围和原始结果见SELF_CHECK。

本次发布的是审计、方案和拟实施补丁，尚未部署应用、构建APK或完成真机/自然质量验收。每一层按真实原件签收；不能承诺高胜率、每代必强或把主机测试写成软件全部完成。

旧[R34.2-152方案](construction/plans/STARCORE_R34_2_REPAIR_20261125/README.md)及更早要求继续作为历史合同保留，不再作为当前应用开工底座。
