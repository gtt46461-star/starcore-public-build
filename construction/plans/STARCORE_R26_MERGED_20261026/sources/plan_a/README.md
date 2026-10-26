本文件属于轻量施工包。两份历史原始ZIP未随包提供，详见OMITTED_ORIGINALS.json；如需原件完全自包含，请使用完整包10.26方案.zip。以下保全说明描述完整包，轻量包仅保留其中140个R24原文件。

# STARCORE R25 施工入口

本包是更新后的最终施工方案，不是已经接入Android的新APK。

1. 先读顶层 STARCORE_R25_最终施工蓝图.docx 或 upgrade_r25/docs/01_最终施工蓝图.md。
2. 施工顺序以 upgrade_r25/registries/unified_80_cards.csv 为准，原R24 48卡与新增32卡共80卡。
3. 新卡详见 upgrade_r25/docs/02_32张施工卡.md；38主题融合、冲突和方法定位同在docs。
4. baseline_r24 完整原样保留10.26附件中的R24包。旧1832需求、299卡、P001至P124、24家族、16专家及全部历史材料不删。
5. 参考实现位于 upgrade_r25/code，SQL位于schema，测试位于tests。它们是HOST规范，不是已接入Android的产品源码。
6. 现有两份R24窄补丁保留；新增Foundry窄补丁有SHA前置条件。不能把三份补丁当整包施工完成。
7. 实际执行日志与边界见 upgrade_r25/results/delivery_validation.json。原始APK、用户真机与自然高胜率证明仍未完成。
8. 原始历史保留、正式REAL授权和正式结算继续遵守唯一Owner，不使用备用链。

复核本包：python3 upgrade_r25/code/verify_delivery.py 本包根目录
运行新增合同：python3 -m unittest discover -s upgrade_r25/tests -v
运行Java合同：python3 upgrade_r25/code/run_java_contracts.py

完整包包括baseline_r24内的历史原件。轻量施工包保留当前施工文档、全部要求表、源码证据、补丁、旧实验记录和HOST测试，仅省略两份历史原始ZIP，其文件名、大小和hash见轻量包的OMITTED_ORIGINALS.json；轻量包不得宣称是原件完全自包含包。
