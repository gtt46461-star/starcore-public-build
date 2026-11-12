# STARCORE R31 统一最终施工入口

R31.1/.2/.3/.4为同一累计功能方案的施工控制修订。当前唯一统合版本：**STARCORE_R31_UNIFIED_FINAL_20261112**，保留原56+2卡、194条原验收、222项现验收及原阈值；沿用31.4的11批和31.3先施工后批末验收。

- [最终施工方案](https://github.com/gtt46461-star/starcore-public-build/blob/construction/r31-final-20261104/construction/repairs/STARCORE_R31_UNIFIED_FINAL_20261112/FINAL_CONSTRUCTION_PLAN.md)
- [最新版源码58卡落实情况：已实现、缺口、返修及待验证](https://github.com/gtt46461-star/starcore-public-build/blob/construction/r31-final-20261104/construction/repairs/STARCORE_R31_UNIFIED_FINAL_20261112/CURRENT_SOURCE_STATUS_58.md)
- [Codex完整施工提示词](https://github.com/gtt46461-star/starcore-public-build/blob/construction/r31-final-20261104/construction/repairs/STARCORE_R31_UNIFIED_FINAL_20261112/CODEX_CONSTRUCTION_PROMPT.txt)
- [20张代码返修卡](https://github.com/gtt46461-star/starcore-public-build/blob/construction/r31-final-20261104/construction/repairs/STARCORE_R31_UNIFIED_FINAL_20261112/REPAIR_CARDS_20.json) · [功能保全证明](https://github.com/gtt46461-star/starcore-public-build/blob/construction/r31-final-20261104/construction/repairs/STARCORE_R31_UNIFIED_FINAL_20261112/PLAN_PRESERVATION_PROOF.json)
- [原31.4完整方案](https://github.com/gtt46461-star/starcore-public-build/blob/construction/r31-final-20261104/construction/STARCORE_R31_FINAL_20261104/README.md) · [执行状态](https://github.com/gtt46461-star/starcore-public-build/blob/construction/r31-final-20261104/construction/r31/NEXT_ACTION.json)

最新应用源码分支为construction/r31-final-20261104，main只是索引。当前已保存的源码检查点762de13d为未完成工程状态，不能拿旧K01—K11 LOCAL收据、helper测试、类存在、心跳或文件生成成功宣称全部功能正常。当前原机APK577311020不是本轮返修成品。

源码审阅确认部分owner/fence、隔离/恢复、研究健康及历史训练实现真实存在，需要保留。已定位业务同步DDL/迁移完成条件、报告请求/ACK/发布恢复、新报告/研究/生命周期/主链辅助API的生产接线缺口，逐项见58卡现状与20张返修卡。继续真实代码施工、集成、故障验收和同签名APK交付，禁止只更新文档、版本号或旧PASS。

方案完整保存不等于软件已完成。DEVICE、原机670MB旧库、24/72小时与自然质量无实测时如实列NOT_RUN/NOT_PROVEN。
