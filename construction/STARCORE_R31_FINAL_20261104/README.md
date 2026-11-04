# STARCORE R31 最终返修施工总包

版本：R31-FINAL-20261104。源提交：5237b265a27fe5ad27fd842901c445d5f8c27749。

先打开 **START_HERE.html**：离线阅读入口，包含全部最终分册、可搜索的逐文件施工卡、47图合同和40项要求导航。无需联网。手机可先解压，再用支持本地HTML的浏览器打开；也可直接阅读docs里的Markdown。

## 交付内容

- **13批、56张施工卡**，涉及122个现有/拟新增文件；19个跨批共享文件单独列冲突与owner规则。
- 原报告713表、60任务、44指标、96检查的账本；47图逐项数据生产、消费和验收；16逻辑研究表。
- 190个精确源码blob及摘要，1236个Java文件完整目录。只对已取得文件的具体方法与合同开展本次核查，**未声称逐行审完1236文件**。
- 原报告49个文件完整保留；48个manifest内成员逐字节与SHA256核验通过。原编码块、SQLite页、安装APK和全量模型状态不在原附件中，未冒称复验。
- 40项合并要求设计映射；1832历史要求与299旧卡保全；原参考方案归档与逐项纠偏。
- 可执行Python/SQLite参考合同、SQL、关键Java设计片段；30项合同测试与12项存储协议测试记录；5000期真实报告数据、固定100候选历史复算和门槛功效脚本。

## 阅读与施工顺序

1. `docs/00_最终施工总纲.md`：基线、事实、闭环、不变量、13批与预算。
2. `docs/01_参考方案纠偏与保全.md`、`02_严格验收与实施协议.md`：必须纠正的旧结论和验收分层。
3. `docs/03_真实数据复算与门槛功效.md`、`04_能力升级与闭环产物.md`：真实数据边界、创造与持续升级如何证明。
4. `docs/05_批次拓扑与需求对照.md`：P00–P08无环执行图、同版集成组、文件owner和全部卡目录。
5. `docs/10`至`13`开头的四份专题：运行/后台、磁盘与内存、训练创造进化、图表矩阵与报告的详细实现。
6. `cards/B00`至`cards/B12`：逐卡文件/方法/修改前后/步骤/验收/依赖/回滚。

数值阈值唯一优先级：`ledgers/acceptance_config.json` → 总纲解释 → 专题示例。卡号以`ledgers/cards.json`为准。实施依赖以`execution_phases.json`及每卡`implementation_predecessor_phases`为准；专题的联合验证要求不能再展开成前置循环。

## 关键施工约束

先修任务owner/fence和写事务合同，再提升吞吐；先迁出主链对`r27_chart_cache/mainline/choice`的权威依赖，再删除缓存。已提交事务的诊断失败不能触发重复训练。缺赛前冻结的历史数据只用于合法训练，不能补造过去预测。无用数据回收按引用闭包和预算执行；heap/native/PSS与磁盘分别测量，有用资产也不能无限保留全部中间版本。

所有“新增”类/SQL与Java片段都是待集成设计，不能在活库直接执行。Python合同是独立参考模型，只有其明确测试范围已验证；它不代表Android原工程已经打补丁。源码快照用于锚点和复核；不应整目录覆盖当前仓库。

## 复验

在解压根目录执行（Python 3，历史实验另需NumPy；依赖版本见结果记录）：

```bash
python3 tools/validate_package.py
python3 reference_code/contracts/test_contracts.py
python3 reference_code/contracts/run_examples.py
python3 reference_code/storage/verify_storage_contract.py
python3 tools/summarize_evidence.py --input evidence/original_report --output /tmp/r31_evidence_rebuild
python3 tools/run_historical_review.py
```

后五项会生成新测试/实验收据，建议在总包副本内执行；原始封包`MANIFEST_SHA256.json`用于检查交付时文件身份，重跑产生的时长/环境差异不能冒称原封包损坏或性能回归。历史实验是固定协议诊断，不是生产R27/SU2全模型复现，也不是未来出手。

## 当前完成边界

本包完成证据整理、返修/能力升级方案与独立参考合同验证。Android源码实施、APK构建、覆盖安装、72h真机和独立自然质量验收仍为待执行。工程测试通过与高胜率证据分别判定；方案不会把高历史分数、影子记录或新增代数写成已实现的自然优势。

包内不添加无意义填充文件。大小来自原始证据、源码锚点、详细施工材料与可复跑验证。
