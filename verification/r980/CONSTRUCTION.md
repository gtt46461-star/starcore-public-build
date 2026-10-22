# R980 工程施工台账（不以代码存在作为完成）

唯一输入：用户5.4.2.22/R979总包；原分支 construction/r967-unified-plan-20260920；基线commit 3ec2a4a99b67af4356d326b1a3d980660bd4a174。
源码ZIP与Git bundle树逐文件比对：3297文件一致。远端分支与随包源码不同，不以远端覆盖交付基线。

## 真实运行链
AutoFetchService（唯一研究Handler）→ResearchLearningContinuityR912（持久化22维护通道）→各已有owner。
线上数值链：唯一开奖/结算提交→ResearchDirectorR952.onSettlementCompleted→ResearchOutboxR952→ResearchTrainingHookRegistryR952→模型/游标/ACK/POST同事务→PostSettlementResearchLoopR952→KnowledgeLedger→下一轮进化。
历史算法链：lane18→ResearchDynamicBootstrapR922→冻结历史数据→AlgorithmInventionLabR908.trainDevelopmentStep→真实VM参数/游标提交→ResearchPopulationR966→子代注册→ResearchLifecycleR932→下一期赛前捕获。
证明链：赛前freezeChampion/freezePair→CandidateCalibrationR932（120+1+120）→lane6固定窗口维护→固定窗口统计→挑战→证书发布→主链只消费有效证书→DecisionGateEngine唯一决策→SettlementEngine唯一结算。
研究lane完成回执不等于训练次数、未来样本或证书。当前工程仍保留旧版本源文件，只有已追踪断点属于本次修复范围；不得宣布全工程冲突清理完成。

## 依赖顺序 / 初始清单
01 ResearchTaskQueueR967.java：数据队列满时维护任务无法入队；隔离有限控制槽位，不删除数据、不制造ACK。
02 ResearchPopulationR966.java：64个活跃算法均未达资源替换样本数；为完成历史训练但无任何前瞻证据的自身父代提供受保护的资源交接，不作质量晋级。
03 ResearchDynamicBootstrapR922.java：接入上述同事务交接；父程序/谱系保留，子代future_n=0。
04 CandidateCalibrationR932.java：增加与待结算真实查询匹配的索引，保持每条冻结预测和计数更新同事务；不修改241样本或质量阈值。
05 FixedResearchWindowR932.java：按实际剩余预算处理多个独立小页，避免固定两期上限和无关空队列事务占用消费预算；只消费已有开奖。
06 ResearchLearningContinuityR912.java：同一调度器为标签/训练/历史开发提供有界公平服务；活跃通道不重复昂贵的就绪检查；仍保持22通道普通轮转。
07 release / semantic / SourceManifest / Manifest构建注入 / build scripts：最终版本、数值连续性审查、源码索引和真实构建闭环。
08 verification/r980：原版失败复现、真实报告表解析、生产类+真实SQLite回归、完整V3模拟链；各结果PASS/FAIL/NOT RUN。

## 明确边界
不合并旧校准小样本；不把历史训练变为OOS；不放宽HR/Brier/ECE；不跳过证书；不为数字非零改UI。
手机覆盖安装、真实UI渲染、息屏/杀进程/设备重启、真实网络长期稳定性：未执行即NOT RUN。

后续记录由实际diff、命令、输出及测试生成，不预填“通过”。

## 最终逐文件索引

以下实际路径对应同分支暂存差异；`Rxxx` 为历史类名，不表示使用了另一套源码。
核心六个owner已在相同最终源码的宿主回放中验证；真机相关状态仍为 NOT RUN。
状态 `M`=修改，`A`=新增，`R`=改名并修改；改名不代表两套当前构建入口同时运行。

01. `app/build/generated/r967/semantic_manifest.json` — M；版本/训练连续性审查/生成身份/真实源码索引；依赖最终产品源树，受编译输入哈希约束。
02. `app/build/generated/source/r967/com/ethk3/novaonepro/ResearchSemanticsGeneratedR967.java` — M；版本/训练连续性审查/生成身份/真实源码索引；依赖最终产品源树，受编译输入哈希约束。
03. `app/src/main/assets/research/r975_source_calls.tsv` — M；版本/训练连续性审查/生成身份/真实源码索引；依赖最终产品源树，受编译输入哈希约束。
04. `app/src/main/java/com/ethk3/novaonepro/CandidateCalibrationR932.java` — M；待结算标签分页索引；影响查询成本，不合并校准身份、不改标签/样本/阈值。
05. `app/src/main/java/com/ethk3/novaonepro/FixedResearchWindowR932.java` — M；依赖标签索引及原有结算owner；预算内有限多页消费，每族独立事务，未清空不ACK。
06. `app/src/main/java/com/ethk3/novaonepro/ReleaseManifestR926.java` — M；版本/训练连续性审查/生成身份/真实源码索引；依赖最终产品源树，受编译输入哈希约束。
07. `app/src/main/java/com/ethk3/novaonepro/ResearchDynamicBootstrapR922.java` — M；依赖上述资源位选择，实际历史训练与子代发布/父代归档同事务；故障注入验证整体回滚。
08. `app/src/main/java/com/ethk3/novaonepro/ResearchLearningContinuityR912.java` — M；依赖队列、历史训练、标签结算；同一调度器保留全通道轮转并给真实消费者保留服务。
09. `app/src/main/java/com/ethk3/novaonepro/ResearchPopulationR966.java` — M；只交接无前瞻证据的自身父代资源位；依赖原有生命周期和事务；不更改质量晋级。
10. `app/src/main/java/com/ethk3/novaonepro/ResearchTaskQueueR967.java` — M；载荷与22维护控制槽分开；影响积压时维护准入，保留所有任务/输入pin/ACK约束。
11. `app/src/main/java/com/ethk3/novaonepro/SourceManifestR815.java` — M；版本/训练连续性审查/生成身份/真实源码索引；依赖最终产品源树，受编译输入哈希约束。
12. `release/r979_semantic_baseline.json` — A；版本/训练连续性审查/生成身份/真实源码索引；依赖最终产品源树，受编译输入哈希约束。
13. `release/r980_training_continuation.json` — A；版本/训练连续性审查/生成身份/真实源码索引；依赖最终产品源树，受编译输入哈希约束。
14. `release/release_identity.properties` — M；版本/训练连续性审查/生成身份/真实源码索引；依赖最终产品源树，受编译输入哈希约束。
15. `release/release_manifest.json` — M；版本/训练连续性审查/生成身份/真实源码索引；依赖最终产品源树，受编译输入哈希约束。
16. `scripts/build_current.py` — R091；替代路径 `scripts/r979_staged_build.py`；唯一当前APK构建脚本及同一脚本的分阶段驱动；755个主Java全编译，不保留旧DEX。
17. `scripts/build_current.sh` — R088；替代路径 `scripts/r979_chain_repair_build.sh`；唯一当前APK构建脚本及同一脚本的分阶段驱动；755个主Java全编译，不保留旧DEX。
18. `scripts/r967_semantic_identity.py` — M；版本/训练连续性审查/生成身份/真实源码索引；依赖最终产品源树，受编译输入哈希约束。
19. `verification/r36-stubs/com/ethk3/novaonepro/BuildConfig.java` — M；测试/测试环境/记录工具，不能作为手机功能实现；详见运行清单。
20. `verification/r977/MaintenanceHarnessR977.java` — M；测试/测试环境/记录工具，不能作为手机功能实现；详见运行清单。
21. `verification/r979/ContinuationHarnessR979.java` — M；测试/测试环境/记录工具，不能作为手机功能实现；详见运行清单。
22. `verification/r979/RootBlockerHarnessR979.java` — M；测试/测试环境/记录工具，不能作为手机功能实现；详见运行清单。
23. `verification/r980/BacklogHarnessR980.java` — A；测试/测试环境/记录工具，不能作为手机功能实现；详见运行清单。
24. `verification/r980/BusinessClosureHarnessR980.java` — A；测试/测试环境/记录工具，不能作为手机功能实现；详见运行清单。
25. `verification/r980/CONSTRUCTION.md` — A；测试/测试环境/记录工具，不能作为手机功能实现；详见运行清单。
26. `verification/r980/README.md` — A；测试/测试环境/记录工具，不能作为手机功能实现；详见运行清单。
27. `verification/r980/ResourceClosureHarnessR980.java` — A；测试/测试环境/记录工具，不能作为手机功能实现；详见运行清单。
28. `verification/r980/android/os/Process.java` — A；测试/测试环境/记录工具，不能作为手机功能实现；详见运行清单。
29. `verification/r980/extract_report.py` — A；测试/测试环境/记录工具，不能作为手机功能实现；详见运行清单。
30. `verification/r980/fixtures/manifest.json` — A；报告原始表快照/哈希；仅宿主测试输入，不打入生产APK。
31. `verification/r980/fixtures/research_candidate_cal_model_r932.json` — A；报告原始表快照/哈希；仅宿主测试输入，不打入生产APK。
32. `verification/r980/fixtures/research_candidate_cal_observation_r932.json` — A；报告原始表快照/哈希；仅宿主测试输入，不打入生产APK。
33. `verification/r980/fixtures/research_candidate_cal_study_r932.json` — A；报告原始表快照/哈希；仅宿主测试输入，不打入生产APK。
34. `verification/r980/fixtures/research_lifecycle_r932.json` — A；报告原始表快照/哈希；仅宿主测试输入，不打入生产APK。
35. `verification/r980/fixtures/v57_algorithm_genome_r908.json` — A；报告原始表快照/哈希；仅宿主测试输入，不打入生产APK。
36. `verification/r980/run.py` — A；测试/测试环境/记录工具，不能作为手机功能实现；详见运行清单。
37. `verification/r980/verify_delivery.py` — A；测试/测试环境/记录工具，不能作为手机功能实现；详见运行清单。
