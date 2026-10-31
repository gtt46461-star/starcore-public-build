# STARCORE R30 最终实现与验收入口

施工方案：[STARCORE_R30_FINAL_20261031](construction/plans/STARCORE_R30_FINAL_20261031/README.md)。57 张卡含 B11 两张条件卡，44 项指标；方案副本保持只读。

当前应用源码分支：[construction/r30-20261031](https://github.com/gtt46461-star/starcore-public-build/tree/construction/r30-20261031)。逐卡实现、真实失败、验证与缺口见 [施工账本](construction/r30/STATUS.md) 和 [FINAL.json](construction/r30/FINAL.json)。main 的入口文件用于定位源码与交付。

最终候选版本为 `577310102 / R30-FINAL-2026.10.31-102`。仅用既有 R13 官方证书与 R27 证书构建两份 APK，待两个流程的 Android 与 CP06/CP16 覆盖/回退全部通过后统一发布；当前 APK 直链待真实构建收据回填。

V2353A 前台60分钟、息屏6小时、连续72小时缺设备，均为 NOT_RUN；Q01–Q07 自然质量证据不足。B11 三项真机触发条件未知，切库未启用。未宣称100%全指标通过，也未调整资格数学或补 ACT 配额。

101 版真实 Android 验收失败已保留。102 版修复测试入口、启动前检查点保护及原子偏好读取，265项单测、lint、Android编译与正式SQLite/JVM重开通过；最终两证书APK仍须全部运行门禁通过后统一发布。

102 版不可变源码：[`8bfd2d515a99`](https://github.com/gtt46461-star/starcore-public-build/commit/8bfd2d515a99753022de6474958aba59ee3b3cdd)。实际最终构建：[R27 41232640964](https://github.com/gtt46461-star/starcore-public-build/actions/runs/41232640964)、[官方 R13 41232674605](https://github.com/gtt46461-star/starcore-public-build/actions/runs/41232674605)；[内部 Android 预检 41232640998](https://github.com/gtt46461-star/starcore-public-build/actions/runs/41232640998)不替代最终验收。APK尚未通过最终运行门禁，暂无最终直链。
