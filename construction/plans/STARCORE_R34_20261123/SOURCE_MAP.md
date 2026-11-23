# 来源、路径与原件定位

## 仓库和下载包的同一结构

本目录位于`construction/plans/STARCORE_R34_20261123/`；三份原始报告位于`construction/evidence/STARCORE_R34_20261123/raw/`。完整下载包保留此目录层级。原始ZIP只是去掉上传文件名中的重复后缀(3)，字节没有变化。

[原件清单](evidence/ORIGINAL_INPUT_MANIFEST.json)包含3份外层ZIP、内层报告ZIP的大小、SHA256和原始成员名；[完整性核验](evidence/report_integrity.json)包含150个普通成员的核对结果。没有原始设备SQLite或APK被夹带在报告内，不可据报告文本声称已重放整个数据库。

## 原始报告映射

|审计简称|外层ZIP标识|内层普通文件位置|
|---|---|---|
|A / 870cd99|STARCORE-870cd99f75df4acdcdc2beb7da9b6cf9b789c3d8093.zip|解压外层中的STARCORE_FULL_REPORT.zip，再按内部原路径读取|
|B / 89d1cac|STARCORE-89d1cac2cb8584b156b12f7a2ce87904c0609dcbe05.zip|同上|
|C / fabedd0|STARCORE-fabedd03aa2cfca6287b650987712825d2b6be609a5.zip|同上|

审计中的`reports/<简称>/...`是当时展开后的工作目录简称，不是新增权威原件；报告文件名、章节、字段和行号用于返回上述原ZIP核验。`evidence/check_evidence/`保留本轮引用的原始章节。runtime采样时间和facts读取时间分别使用，不能将两个钟的差异当作一份同步快照。

## 源码、前态与旧复核

应用095精确来源为`construction/r335-field-095@273641e838e0b8dd842b42a67a674e184942a30a`。`source_before/`仅保存本方案涉及的已有文件前态，不能独立充当可构建Android工程。全工程需按CODEX_EXECUTE从已指定Git提交检出；`evidence/SOURCE_095_JAVA_INDEX.json`是1362个生产Java输入的索引，不是逐行语义全审计声明。

原R33.5复核保留在仓库`construction/plans/STARCORE_R33_5_REVIEW_20261119/`。旧`runThroughReview`路径有些来自其封存基础总包，本次没有擅自将其标成095现成文件。`evidence/legacy_test_contracts.json`逐项列现有095局部测试与需新增行为适配器；原32门/96断言/288场景仍需恢复材料并针对真实R34实施重验，不能被本包小规模helper测试代替。

审计里`analyses/`属于方案编写时路径；当前实际可执行材料以CODE_PAYLOAD和TEST_RUNBOOK内的`payload/`路径为准。已归档初次审查意见保留当时状态；修订闭环见REVIEW_RESOLUTION，不能断章引用初次结果当最终验收，也不能倒过来假称初次就通过。
