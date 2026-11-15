# STARCORE R32.5.1 当前最终施工方案

本次对32.5完整代码、两份原始检测报告和实际仓库继续复核，直接修复了**显式文件权限/续施工判定**和**报告证据说明的错误路径**，补充原报告重抽核验工具。五批36条声明命令重新全部通过。当前交付修订为R32.5.1-C1；生产施工代码仍R32.5，候选577311033。

先读[当前施工入口](REPOSITORY_ENTRY.md)、[本次最终复核](FINAL_REVIEW_R3251.md)、[实际验证](VERIFICATION.json)和[完整施工提示词](CODEX_PROMPT.txt)。旧R31/R32文档保全原需求，不再并行指挥修改。

[查看五批问题、具体改法及全部312个文件](BATCH_FILE_EXPLANATION.md)。C1补齐两张原样保全文件卡，新增15项验收/覆盖工具回归；应用源码字节不变。

完整总包为**291,124,244字节，2021个文件成员**，包含完整基线、完整候选、逐文件代码卡、原报告、五批合同和本次原始执行记录。应用层范围保持**5批、18个联合问题、312件受控路径、410份完整文件卡、120件实际改动**；新增的方案工具修订不冒充新生产功能。

## 一条命令取得完整总包

从本发布目录执行，输出目录必须不存在：

```bash
python3 restore_package.py --extract R32_5_1_restored
cd R32_5_1_restored/STARCORE_R32_5_FINAL_REPAIR_20261115
```

恢复脚本按[当前分片清单](PACKAGE_PARTS.json)核每片、完整ZIP SHA及CRC，再安全解压。共61个有序分片，复用33个未变原件；这是一个完整ZIP的字节分片，执行者不需选择或合并两套施工方案。

```text
ceb5e3516452524de2519781bff04ba9f844bb95f95052bd4c2778928b856966
```

上面是`STARCORE_R32_5_1_FINAL_REPAIR_20261115.zip`的SHA-256。[交付回执](DELIVERY.json)另给总包清单SHA与源码身份。仓库此目录只展开导读、关键审计和审阅工具；`issues/`、`batches/`、`code/`、完整验收/需求清单及源码ZIP在恢复后的总包内。

## 核验与施工

在恢复后的完整总包中，先执行：

```bash
python3 tools/verify_package.py --package "$PWD" --sealed
python3 tools/verify_report_inputs.py --package "$PWD"
```

然后在精确基线`7424eeae6354eb6599dd8498e123c059bd410c90`的隔离施工工作树按P1→P5执行，先用本包`inspect_resume.py`定位现状，再运行当批新源码及回归。原码清单SHA仍为`bcacd4b40282ff3e81775e53e081768e90188ba068843d2b726dd60b5c87df70`，因为本次未擅改已经复验的生产后态。

39个应用器、13个续施工测试、源码组合内容/权限和两份报告来源检查已通过。工程宿主通过不代表正式Android APK、真机24/72小时或自然高胜率已经通过；[外部条件](OPEN_ITEMS.md)与222条原验收保持原门。
