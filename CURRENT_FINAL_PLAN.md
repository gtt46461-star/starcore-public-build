# STARCORE R30 返修003实现与验收入口

[不可变60卡方案](construction/plans/STARCORE_R30_FINAL_REPAIR_20261101/README.md) · [当前真实进度](construction/r30-repair/STATUS.md) · [逐卡证据](construction/r30-repair/ALL60_EVIDENCE.json) · [交付身份](construction/r30-repair/DELIVERY.json) · [验收范围](construction/r30-repair/FINAL.json)

当前公开003 `577311003 / R30-REPAIR1-2026.11.01-003`：[下载APK](https://github.com/gtt46461-star/starcore-public-build/releases/download/r30-repair1-577311003-63ad6291711d/STARCORE-R30-REPAIR1-EXISTING_R27-63ad6291711d.apk) · [发布页](https://github.com/gtt46461-star/starcore-public-build/releases/tag/r30-repair1-577311003-63ad6291711d)。应用与测试同源 `63ad6291711db607fd604443b4f50492882f09e8`，实际CI run41365268452。

347项实际HOST单测、原全部门禁和13组repair native通过。Android运行、26故障和三代九步升级为模拟器证据；188个目标场景真实采集并检查存在图形的原生绘制成功。已实际查看R03/R12及记录的其他抽样共9个目标，具体为READY-R03.png、READY-R12.png、READY-R02.png、READY-R01.png、EMPTY-R04.png、INSUFFICIENT-R13.png、READ_ERROR-R08.png、READY-28.png、READY-01.png；旧图缺ABI/读取失败和研究合法不足状态保留。原51项lint基线与四保护文件字节未改，002双次假绿人工否决历史完整保留。

V2353A72小时、原用户约26GB库重测、自然Q01-Q07与主链效果未证明；逐卡未直接覆盖场景保留。Q08为commit `1342b77b0c3ffeb73e68d892fee93987dc4a4129`的历史20独立合成机制试验，源哈希文件数单独记录，非003整源重跑。不宣称全部产品验收完成。
