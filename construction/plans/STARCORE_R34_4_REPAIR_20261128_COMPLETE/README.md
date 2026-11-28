# STARCORE R34.4-211 · 完整授权发布与施工入口

**2026-11-28（Asia/Shanghai）｜46项处置卡｜50条原发现｜12批｜136个211源码锚｜5组候选补丁。**

用户已明确授权全部资料上传。本目录发布原完整方案、逐条审计、C01—C05全部候选与加强后的施工指令；原始完整交付ZIP以27片保存在同仓库，可自动还原并验证原字节。原报告仍完整包含其中。本次上传不修改应用源码，尚未构建返修APK，不能将方案自检等同产品施工完成。

## 直接入口

1. [完整施工指令 CODEX_EXECUTE.md](CODEX_EXECUTE.md)：给Codex/施工方连续完成B00—B11，包含可运行资料检查命令、逐卡台账、实际APK与报告导出验收。
2. [B00构建与交付附约](B00_EXECUTION_ADDENDUM.md)：211中的旧173工具/批次/runner/CI准入兼容缺口，以及本轮必须实现的链路。
3. [创造训练与反馈附约](CREATION_EXECUTION_ADDENDUM.md)：独立DEV、首次冻结、六ModelKind、真实结构变异/交叉/融合与工程/自然分层。
4. [施工总方案](CONSTRUCTION_PLAN.md) / [46项逐项问题](ISSUES.md) / [12批顺序](BATCH_INDEX.md)。
5. [逐文件修改矩阵](FILE_MODIFICATION_MATRIX.md) / [精确源码锚](SOURCE_ANCHORS.json)。
6. [唯一主链与保全](contracts/PIPELINE_AND_PRESERVATION.md) / [创造训练与质量](contracts/CREATION_TRAINING_AND_QUALITY.md) / [报告导出与存储](contracts/EXPORT_AND_STORAGE.md)。
7. [全量覆盖](coverage/README.md) / [旧方案继承](inheritance/README.md)：96检查、44指标、165对象、826表、1832需求、299历史卡保留。
8. [候选说明](CANDIDATES.md) / [5组候选及前后hash](PATCHSET.json) / [验收矩阵](ACCEPTANCE_MATRIX.md)。
9. [执行台账模板](EXECUTION_LEDGER_TEMPLATE.json) / [实际自检及限制](SELF_CHECK.md) / [本次完整授权范围](PUBLICATION_AUTHORIZATION.json)。
10. [原始完整包及还原说明](../../packages/STARCORE_R34_4_211_20261128/README.md) / [诊断原件完整性](evidence/REPORT_INTEGRITY.json) / [仓库物化合同](REPOSITORY_PACKAGE.json)。

## 正确底座

精确已安装211应用源码：`5c7f1b36eaee6036df280e85835bb0fb4ce0b40d`，versionCode `577311211`，包名 `com.starcore.research.r13`。后续可采用经逐文件核实的合法后代。

本目录所在的main/方案提交负责分发资料，**不是211应用源提交**。本次只新增完整计划、原包及入口，原main应用源保持未改。施工在211或其已核后代的独立工作树中进行，旧173整文件后态不能覆写已安装实现。

旧目录 `STARCORE_R34_4_REPAIR_20261128` 与原ZIP的历史回执保持原样。原ZIP中“4组公开候选”等是旧发布事实；当前完整授权范围以本目录为准。

## 一次准备后严格验证

先完整检出本次资料所在commit。在仓库根目录执行：

```bash
python3 construction/plans/STARCORE_R34_4_REPAIR_20261128_COMPLETE/tools/prepare_evidence.py
cd construction/plans/STARCORE_R34_4_REPAIR_20261128_COMPLETE
sha256sum -c MANIFEST.sha256
python3 tools/verify_plan.py
```

`prepare_evidence.py`从已逐片及总SHA验证的原包读取固定诊断成员，放入本目录的 `evidence/ORIGINAL_DIAGNOSTIC_211.zip`。该文件由包内原字节恢复，在Git中被忽略；它仍受完整MANIFEST和原报告验证器检查，未作为第二份19.5MB重复上传。脚本不会覆盖任何已有不同字节的文件。

不传 `--source-root` 时，资料完整性可以验证，源码层明确为NOT_RUN。按CODEX_EXECUTE建立干净211工作树后，传准确源码运行源锚与候选检查；所有执行结果写到冻结计划外。

## 当前结果的含义

完整原包保存了此前报告/源码锚/候选和主机自检的原始记录。本次复核与新增脚本验证记入本目录validation；历史源码验证、当前包完整性、Android/真机/自然质量分别记录。原.scdev、原安装APK及完整5GB数据库并未因本次分发凭空补齐。真实缺件保留为相应任务输入门，不能阻断无依赖的独立工程，也不能伪造通过。
