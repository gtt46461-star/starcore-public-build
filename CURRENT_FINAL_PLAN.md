# STARCORE R30 实现与验收入口

当前候选版本：`577310106 / R30-FINAL-2026.10.31-106`。应用源码在 [construction/r30-20261031](https://github.com/gtt46461-star/starcore-public-build/tree/construction/r30-20261031)，真实进度见 [STATUS.md](construction/r30/STATUS.md)、[FINAL.json](construction/r30/FINAL.json)、[BUILD_PROGRESS.json](construction/r30/BUILD_PROGRESS.json)。

B00–B08的48张施工卡已完成工程检查，B09源码已实现，B10正进行最终交付验证。106版完整本地269项单测/70XML零失败、零错误、零跳过，lint没有新增错误，原51项基线及7项未匹配保留；原冻结适配器已恢复原字节，新增SU2只读校验位于独立R30类。Android测试真实投递原看门狗广播通过，测试库原收据迁移域补齐后已重新编译。历史失败完整保留。

103版两证书四项CP06/CP16覆盖—回退—再覆盖均通过，但不替代106版最终验收。仅使用既有R13官方与R27证书执行最终相同源码完整CI；两边实际Android17项、48张图表截图、14格故障及四项升级演练全部通过后才统一发布两份release APK。当前暂无最终APK直链。

V2353A前台60分钟、息屏6小时、连续72小时没有设备，均为NOT_RUN；Q01–Q07自然质量证据不足。B11三项真机触发条件未知，切库未启用，未宣称100%全指标验收。

方案：[STARCORE_R30_FINAL_20261031](construction/plans/STARCORE_R30_FINAL_20261031/README.md)，57张卡含B11两张条件卡、44指标，只读方案树未变。
