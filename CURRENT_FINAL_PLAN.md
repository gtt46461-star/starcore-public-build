# STARCORE R31 唯一施工入口

当前方案：**R31.1-EXECUTION-20261105，13批、56卡**。本入口不再指向R30。冻结方案和既有应用源码分别读取，禁止把main当作最新应用源码。

- [R31.1方案总入口](https://github.com/gtt46461-star/starcore-public-build/blob/main/construction/STARCORE_R31_FINAL_20261104/README.md) · [全部分册与施工卡](https://github.com/gtt46461-star/starcore-public-build/tree/main/construction/STARCORE_R31_FINAL_20261104)
- [当前R31源码与验收记录](https://github.com/gtt46461-star/starcore-public-build/tree/construction/r31-final-20261104)
- [施工流程纠偏与接续指令](https://github.com/gtt46461-star/starcore-public-build/blob/construction/r31-final-20261104/construction/r31/PROCESS_ADJUSTMENT_20261105.md) · [下一步最小清单](https://github.com/gtt46461-star/starcore-public-build/blob/construction/r31-final-20261104/construction/r31/NEXT_ACTION.json)
- [逐卡真实进度](https://github.com/gtt46461-star/starcore-public-build/blob/construction/r31-final-20261104/construction/r31/PROGRESS.json) · [执行记录](https://github.com/gtt46461-star/starcore-public-build/blob/construction/r31-final-20261104/construction/r31/EXECUTION_LEDGER.json)

## 已核实进度

核查源提交[`05391e682660`](https://github.com/gtt46461-star/starcore-public-build/commit/05391e68266083d156612b014f8d1c84cfcf2fb3)，北京时间2026-11-05 11:14:53：B00两卡完成限定源码/HOST合同验收；21/56卡已开始，35卡未开始；最终强制闭环0卡。后续变化以当前分支账本为准。已改代码、源码验收、构建、联合工程验收、真机和自然质量分别统计。

当前收敛**P01（B01/B02七卡联合协议）**。CI015已经失败：其485项单元中8项失败；另一后续本地源码全量为13项失败，不得混为同一源码。API29/35各10项协议均2过8败，共同前置为Certificate域指纹不匹配。当前完整工程验收及最终成品仍未通过。

## 执行约束

继续现有工程并保留未推送改动，不从旧基线重做。先修当前共同前置和内存生命周期，再进行匹配源码的针对性与全量工程检查；本地修红后批量冻结和CI。共享文件单一owner，生产者/消费者协议不得拆成不同版本。真机、24h/72h及自然质量单列，不伪记完成；无设备不妨碍源码修复。具体执行以流程纠偏、冻结方案及当前卡合同为准。

旧R30发布是历史交付，不代表R31完成。R31检查点014/015有未通过工程门，不作为最终成品推荐下载。

本次已直接修订R31原目录的总纲、实施协议、拓扑与机器执行规则，同步离线HTML及SHA256清单。详见[正式方案施工纠偏分册](https://github.com/gtt46461-star/starcore-public-build/blob/main/construction/STARCORE_R31_FINAL_20261104/docs/14_R31.1施工纠偏与接续.md)；1220项方案完整性检查通过。原版完整保存在[原R31归档](https://github.com/gtt46461-star/starcore-public-build/tree/main/construction/archives/STARCORE_R31_FINAL_20261104)。方案修订不代表应用故障已修复。
