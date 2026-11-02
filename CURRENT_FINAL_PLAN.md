# STARCORE R30 返修实施与验收入口

当前方案为 [R30 最终返修方案](construction/plans/STARCORE_R30_FINAL_REPAIR_20261101/README.md)，60卡、12批次 B00–B11。实际施工在 construction/r30-repair-20261101 分支。

002版已停止作为最终交付推荐：人工查看正确定位后的R03截图，发现100节点超过旧64节点绘图预算，页面显示 IllegalArgumentException；旧测试没有检查onDraw兜底错误。真实失败见 [人工复核记录](evidence/r30-repair/B11/MANUAL_002_GALLERY_REJECTION.json)。不得将旧188PNG数量/CI绿色当作全部图已正确绘制。

正在构建003：保留旧图64节点合同，新研究图显式允许有界120节点；截图检查真实绘制次数和原始绘图异常。R12用已有配对残差补齐热图轴，数值与研究消费者不改。003将重新运行全部原门禁、单测、188图、26故障格及CP06/CP16/当前R30-107三代九步覆盖升级；通过并独立查看公开实际图片后才推荐新的APK。

原R27签名loader保持原字节，不生成新密钥，代理不读取或上传签名私钥。DecisionGateEngine、SettlementEngine、SequentialQualificationR27与原51项lint基线保持原字节。

Vivo V2353A 72小时、原约26GB用户库性能/空间及自然Q01–Q07/主链效果仍未验收，不宣称60卡百分百闭环。逐卡范围见 [STATUS](construction/r30-repair/STATUS.json)；不可变方案目录不改。
