# STARCORE 当前唯一施工方案：R32.5.1

当前入口仍为 **`construction/repairs/STARCORE_R32_5_FINAL_REPAIR_20261115/`**，已更新为**R32.5.1最终复核修订**。旧R31/R32只保全历史需求与证据，不并行指挥施工。生产代码候选仍R32.5/577311033。

- [打开当前施工入口](construction/repairs/STARCORE_R32_5_FINAL_REPAIR_20261115/REPOSITORY_ENTRY.md)
- [本次深度复核与直接修订](construction/repairs/STARCORE_R32_5_FINAL_REPAIR_20261115/FINAL_REVIEW_R3251.md)
- [取得完整ZIP和恢复说明](construction/repairs/STARCORE_R32_5_FINAL_REPAIR_20261115/README.md)
- [Codex施工提示词](construction/repairs/STARCORE_R32_5_FINAL_REPAIR_20261115/CODEX_PROMPT.txt)
- [实际验证范围](construction/repairs/STARCORE_R32_5_FINAL_REPAIR_20261115/VERIFICATION.json)
- [交付身份及精确计数](construction/repairs/STARCORE_R32_5_FINAL_REPAIR_20261115/DELIVERY.json)
- [外部条件与未签收范围](construction/repairs/STARCORE_R32_5_FINAL_REPAIR_20261115/OPEN_ITEMS.md)

保持5批、18个联合问题、312件受控路径、410份完整文件卡。直接修复声明权限未执行/续施工忽略权限以及报告证据路径错误，新增原报告输入重抽校验，完整重跑36条五批命令；39个应用器与13个续施工回归通过。

完整ZIP：`STARCORE_R32_5_1_FINAL_REPAIR_20261115.zip`，291,124,244字节，2021个文件成员。C1已补齐两张保全说明卡，当前410张卡覆盖312路径；[逐批问题与全部文件](construction/repairs/STARCORE_R32_5_FINAL_REPAIR_20261115/BATCH_FILE_EXPLANATION.md)。源码、原报告及全部施工代码都在总包中；仓库发布页只展开导读和关键审计。

```bash
cd construction/repairs/STARCORE_R32_5_FINAL_REPAIR_20261115
python3 restore_package.py --extract R32_5_1_restored
cd R32_5_1_restored/STARCORE_R32_5_FINAL_REPAIR_20261115
```

总包SHA-256：`ceb5e3516452524de2519781bff04ba9f844bb95f95052bd4c2778928b856966`。

精确源码基线仍为`7424eeae6354eb6599dd8498e123c059bd410c90`；先在隔离源码工作树检查现状，再执行P1→P5。宿主工程通过不代签正式APK、实际设备24/72小时或新资产自然高胜率；全部原门保持。
