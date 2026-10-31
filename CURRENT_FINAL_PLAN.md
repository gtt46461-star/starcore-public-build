# STARCORE R30 实现与验收入口

当前候选版本为 `577310103 / R30-FINAL-2026.10.31-103`。应用源码在 [construction/r30-20261031](https://github.com/gtt46461-star/starcore-public-build/tree/construction/r30-20261031)，逐卡实现、真实失败与缺口见 [STATUS.md](construction/r30/STATUS.md)、[FINAL.json](construction/r30/FINAL.json) 和 [BUILD_PROGRESS.json](construction/r30/BUILD_PROGRESS.json)。

103版补齐原SU2合格证书在冻结提交与读取端的命名空间桥接，继续使用原证书读取器和原DGE全部守卫。产品Java 265项单测、lint、Android编译及正式SQLite正反例通过。启动探针明确区分缺文件、损坏、备份及暂时写入，失败仍保存回执；原时间/PID/心跳门槛保持。

仅使用既有 R13 官方证书与 R27 证书构建两份最终 APK；两个流程的实际 Android 和 CP06/CP16 覆盖回退全部通过后统一发布。101、102版失败候选留证，不作为最终交付。当前暂无最终APK直链。

V2353A 前台60分钟、息屏6小时、连续72小时缺设备，均为 NOT_RUN；Q01–Q07 自然质量证据不足。B11 三项真机触发条件未知，切库未启用。未宣称100%全指标验收。

方案：[STARCORE_R30_FINAL_20261031](construction/plans/STARCORE_R30_FINAL_20261031/README.md)，57张卡含B11两张条件卡、44指标，方案副本保持只读。

103版冻结源码：[`7b50de44e381`](https://github.com/gtt46461-star/starcore-public-build/commit/7b50de44e381b4de3409f263d1db80ae63eca8fa)。实际任务：[R27最终构建41235591318](https://github.com/gtt46461-star/starcore-public-build/actions/runs/41235591318)、[官方R13最终构建41235634221](https://github.com/gtt46461-star/starcore-public-build/actions/runs/41235634221)、[内部Android预检41235591335](https://github.com/gtt46461-star/starcore-public-build/actions/runs/41235591335)。内部预检不替代最终release APK验收。
