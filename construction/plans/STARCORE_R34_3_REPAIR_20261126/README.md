# STARCORE R34.3-173 修订2 · 返修方案与原始检测报告

**从安装173开始施工，不从main旧应用树或152/122后态覆盖。** 修订2共10组、67步、64个不同文件。本包包含44项主问题的报告/源码对照、完整补丁和替换文件、12批合同、逐项覆盖清单、数学机制映射、验收和原始报告。

1. [施工总方案](CONSTRUCTION_PLAN.md)
2. [44项问题及具体原因](ISSUES.md)
3. [12批施工顺序](BATCH_INDEX.md)
4. [完整施工提示词](CODEX_EXECUTE.md)
5. [补丁应用顺序及完整文件](PATCHSET.json) / [文件修改矩阵](FILE_MODIFICATION_MATRIX.md)
6. [39个数学族的真实实现](specs/MATHEMATICAL_MECHANISMS.md)
7. [验收矩阵与状态边界](ACCEPTANCE_MATRIX.md)
8. [旧57问题在173的现状](inheritance/R342_ISSUES_57_CURRENT_STATUS.md)
9. [本次实际自检](SELF_CHECK.md)
10. [原始上传检测ZIP，字节未改](report/ORIGINAL_DIAGNOSTIC_173.zip)
11. [本次续审、候选代码修正与范围澄清](CONTINUATION_REVIEW.md)
12. [原R967/R82构建身份接线与精确命令](patches/semantic_support/REVIEW_AND_TESTS.md)

源底座：`4d1de9d83692635b5c5ce39144fece1c8a31c8ac`，已安装versionCode `577311173`。计划R34.3-173与APK系列R34.2分开；原签名、旧封印、固定K3与唯一主链保持。

先运行`python3 tools/verify_plan.py --plan . --source-repo <173工作树> --out <计划外验证输出>`。该验证器不签Android或自然质量PASS。应用器默认检查，显式`--apply`才改指定工作树。

本次交付是审计、方案和拟实施代码。没有把应用改动部署到手机，没有完整Android构建/真机/自然未来验证；这些须按批次产出真实证据后才能关闭。
