# 能否识别真正优于随机的资产

## 两份报告能支持的结论

两份 `UPGRADE_DIAGNOSIS.json` 的 `capabilityRepairR32.operatingSummary.independentFuture` 均为策略对象 0、独立未来期 0、出手 0、合格实验 0、有效证书 0、胜率 null。两份 `R26_REPORT.json#strategyAssessment` 均给出累计 50 次演化尝试、10 个结构结果、0 次研究接纳，导出的策略对象为 0。跨观察窗口没有新增的相应累计记账。当前没有一条能够用这些证据证明优于随机的、由工厂新产生的资产。未进入独立验证的对象无法估计胜率，也无法据此推断所有可能公式都无效。

两次快照之间新增 467 个开奖目标，372 条有 R35 赛前影子和同一目标的结算，127 条命中，条件命中率 34.14%。另 95 期缺 R35 赛前原件。这组数据用于定位业务中断，无法归属为某个预注册新子代的未来表现。报表须保留这三个分母，分别列完整目标日历、实际冻结且结算的影子、具有独立实验身份的候选。单位置从 10 个号码事前选 3 个，在下一期为独立均匀排列的零假设下，命中概率为 3/10。依据本期是否命中再选策略、窗口或入账期，不能用来检验这一基准。

## B09 的两处补正

**1. 同一条模拟开奖流不能算作两次独立验证。** 第三版已要求跑满 6 臂 × 20 个种子。固定 122 的 `FactoryCapabilityExperimentR31.seed/ensureEngine/summary` 各臂复用相同 20 个 seed，再把三个零假设臂的判定放入以 60 为分母的原协议区间。`ResearchCapabilityExperimentR31.generateStream` 对 NULL_UNIFORM 和 SIGNAL_DISABLED 都置 eps=0。同 seed 对应的全部 9,000 个十号码排列以及日历相同。NULL_TIME_SHUFFLED 另走打乱分支，也沿用该 seed。此次把固定源码中的生成方法、排列方法和随机数生成器单独运行，60 份输入流的 SHA-256 全部与验收工具独立复算一致，20 对均匀/关信号输入完全相同。这里只运行了输入生成器。

`QualityExperimentR341.run/summarize` 调用原工厂正式实现。在真实 trial 取到输入时，导出三个零假设臂每行 10 个无符号字节、依次包含 3,000 条热身及 6,000 条预定未来的二进制原件。记录同 trial 的源码、协议、选择轨迹摘要、判定、输入原件路径及摘要。内容相同的文件允许共享，arm/seed 两个执行键和各自判定都需保留。按原协议重算各臂和原名义指标，标注跨臂共同 seed；再按 seed 取该组任何零臂出现 DETECTED 的事件数，报告 20 个 seed 组的单侧 95% 上界（依赖不同 seed 可作为独立重复的模拟假设）。这个补充量度描述一组发生至少一次误检的风险，原单臂/名义合计的估计量继续单列。下一个试验若要合并成独立重复，应事前分配独立随机流、登记新的协议身份，旧账保留；不得改写历史合同或发证门槛。

**2. 分辨“算法工厂能发现信号”和“统计检验能识别已给定的命中流”。** 两份 `R30_REPORT.json#/chapters/11/fullFactorySyntheticExperiment/payload` 都来自旧提交 `bb7b49bbcc9266bad548ec37e71403256fc06a4d`，计划 120 个、完成 0 个。`NullPipelineSimulationR30.powerTrial` 直接生成命中/未命中，再调用 `SequentialQualificationR27.State.observe`，固定 globalIndex=1、目标出手率=1、每期 ACT、最多 5,000 期。34%/37%/40% 三档各 20 次是该单一统计环节的诊断。真实出手覆盖、长期不断增长的 globalIndex、冷启、候选搜索和完整资格门在这段代码中都没有被运行。另一条 `FactoryCapabilityExperimentR31.finish` 才会区分没有冠军/待确认对象、原确认失败、未来检验或证书门失败、DETECTED；它的 `signalStrength=.30` 表示把指定位置的号码替成上一期同位置号码的概率，这个参数不能直接解释为某个生成策略的命中率。

在现有合成研究命令中同时输出两张结果。序贯诊断逐行给出理论输入命中率、seed 或重复号、固定 index 和出手率、观测期数、判定。工厂按 arm/seed 导出原搜索、冠军、独立确认、冻结未来各阶段的首个缺项或拒因。在有明确冠军的情形保留公式、同机会对照和正式的未来原件。固定开发夹具分别走无冠军、确认失败、证书数学门失败及有效植入路径，检验计数守恒和真实来源。另行登记不同 index 或低覆盖灵敏度时，沿用生产 alpha 公式并列 index / ACT / 时限，不回填原试验。工程通过取决于真实执行和正确归因，数值不佳照录。决定真实出手继续走 B09 的既有自然注册、证书和主链。

## B09-05-A02 回执接口

执行原件的 `q08_component_artifact` 指向本次 Q08 组件。组件保留原有 `factory`、`power_trials`、`power_source_commit`，增加 `dependence_artifact` 指向 `R341_Q08_DEPENDENCE_1` 原件。三个零臂每条 `factory.trialResults` 附 `draw_stream_artifact, draw_stream_sha256`，实际二进制原件列在正式回执 `artifacts` 中。主函数 `check_q08_dependencies(src, result, originals)` 核对输入字节摘要以及分组计数，`result.source_commit` 为实际运行源码。工程执行与同版自然质量投影的 Q08 分项均读取同一格式。旧版按原封印追溯。

原件字段为 `schema, source_commit, protocol_hash, natural_authority=false, nominal_trials=60, seed_groups=20, identical_uniform_disabled_pairs=20, any_null_detected_seeds, any_null_per_seed_upper95, per_arm_detections`；`nominal_estimand=REGISTERED_POOLED_VALUE_DEPENDENT_ARMS`、`estimand=ANY_NULL_DETECTION_PER_REGISTERED_SEED`、`power_scope=SEQUENTIAL_ONLY_INDEX1_ALL_ACT`。示例中三臂在同一 seed 各误报一次，其余均未误报，名义合计 3/60，各臂 1/20，按组为 1/20，上界约 0.216106。按组的不同计数不抹去原始三次执行结果。新字段无自然授证或撤证权限。

施工者可运行 `python3 -B tools/test_q08_receipt.py`（从方案目录执行），然后用实际生产工厂输出喂正式回执。参考检查器依据原协议和源码复算输入。若新研究需修改生成器，建立独立试验版本；不得沿用旧协议的输入身份。
