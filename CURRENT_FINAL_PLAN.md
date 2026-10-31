# STARCORE R30 应用源码、APK 与验收入口

最终版本：`577310107 / R30-FINAL-2026.10.31-107`。两份 APK 来自同一源码 [`b225ab204db0`](https://github.com/gtt46461-star/starcore-public-build/commit/b225ab204db043b347f9d76d6c5c1376fd9a2590)，按当前安装证书选择对应版本。

- [R13/R26 官方既有证书 APK](https://github.com/gtt46461-star/starcore-public-build/releases/download/r30-final-577310107-b225ab204db0/STARCORE-R30-FINAL-OFFICIAL_R13-b225ab204db0.apk) · SHA-256 `62b280d2785a8cc453bcc894a08319b5caf6434a68bfb28dce708c845b51590d`
- [R27 既有证书 APK](https://github.com/gtt46461-star/starcore-public-build/releases/download/r30-final-577310107-b225ab204db0/STARCORE-R30-FINAL-EXISTING_R27-b225ab204db0.apk) · SHA-256 `c414a664dec8a8c58f5e3f573f875e593a3c5d7c156b8df19e856bf5109c9a94`

源码与完整验收账本：[construction/r30-20261031](https://github.com/gtt46461-star/starcore-public-build/tree/construction/r30-20261031) · [FINAL.json](https://github.com/gtt46461-star/starcore-public-build/blob/construction/r30-20261031/construction/r30/FINAL.json) · [不可变源码ZIP](https://github.com/gtt46461-star/starcore-public-build/archive/b225ab204db043b347f9d76d6c5c1376fd9a2590.zip)。main 保存这一交付索引。

两证书均完成 Android 17 项回归、48 张实际图表页截图、14 格故障矩阵，以及各自真实 CP06/CP16 覆盖—回退—再覆盖验证。迁移保护大模型原字节；缺字段保留原源库及路由，不以部分复制当成功。完整回执与公开签名见 release 的 FINAL_BUILD.json、EVIDENCE_RECEIPTS.json 和 delivery.json。原 lint 51 条基线及 7 条未匹配保留，新增错误为零。

V2353A 前台60分钟、息屏6小时、连续72小时尚未执行，Q01–Q07 自然质量证据不足；B11 三项真机触发条件未知，切库未启用。未宣称100%全指标验收。

方案：[STARCORE_R30_FINAL_20261031](construction/plans/STARCORE_R30_FINAL_20261031/README.md) · [逐步说明](construction/plans/STARCORE_R30_FINAL_20261031/START_HERE_施工方必读.md) · [完整ZIP](construction/plans/STARCORE_R30_FINAL_20261031.zip)。57 张卡（含 B11 条件卡）、44 指标，基线 CP06 `bb607cf5ab69`，方案树未改。
