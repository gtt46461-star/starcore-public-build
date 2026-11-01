# 施工方必读

先读最终HTML或FINAL_IMPLEMENTATION_SPEC.md，再按batches/B00起执行。

本包是60卡最终施工设计及关键代码，不是已修APK。11补丁28文件只覆盖关键实现，不代替全部60卡。固定b225运行源码，不在main索引树上盲改。每批一个集成Owner，同文件按FILE_OWNERS增量合并；跨文件补丁按SERIES只应用一次。

原29图全部保留并校正，新增18图分3组，总47图。R06有实际配对研究与精确反事实代码，其他图仍要完成分析产物/任务/消费者；不能把WAITING页面验收为研究完成。

verification是真实主机验证及受限适配器，非Android/真机/自然效果证据。source_audit的全量语法扫描不等全部语义证明。原始矩阵来源/完整设备日志不足的地方保留BLOCKED/UNKNOWN。

施工顺序、逐文件问题/改法/锚点、回滚、反证与阶段门均在相应卡。工程与自然质量分开验收，不能改门槛/补未来冻结/重置alpha制造高分。

关键入口：

- FINAL_IMPLEMENTATION_SPEC.md：完整报告、18图/16表/29旧图/20矩阵/60卡/44指标/82能力。
- registries/CARDS_60.json、FILE_ACTIONS.csv、FILE_OWNERS.json、METHOD_CHANGE_MAP.json：机器可读施工登记。
- patches/SERIES.json：关键补丁顺序；ALL_KEY_CODE.patch仅用于完整顺序检查，不与单补丁重复应用。
- batches：每卡独立文件与对应code目标。
- evidence：两份独立快照事实与旧表/旧卡清单。
- verification：实际测试源码与日志，以及准确范围。

