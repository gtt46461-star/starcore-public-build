# 成熟功能保留与回归合同

“保留”指保留合法行为、数据和权威所有者，不指冻结所有实现细节。某段已有代码也可能需要修复；修复必须证明对应成熟行为未丢失。本清单不把历史产出冒充152当前进程成功运行。

| 保留项 | 当前证据或源码依据 | 修改前后必须比较 |
|---|---|---|
| 单位置固定K3、完整1200动作空间 | RSEC-027/037/076、DecisionGateEngine、K3ActionSpaceEngine | 固定输入得到1200个唯一动作；无K4/多位置REAL/追单/马丁 |
| 唯一分析、决策、结算owner | RSEC-036、AnalysisSingleFlightR36、DecisionGateEngine、SettlementEngine | 竞争请求、重复开奖、进程重启仍最多一次canonical/一次对应结算 |
| PIT和赛前首写冻结 | V51RuntimeOrchestrator、V51Repository、OosAfterDrawWriterR36 | 截止前后边界、模型发布竞态、开奖后重试不产生补造事前记录 |
| 合法SHADOW基线与REAL分账 | 1858条已结算SHADOW、RSEC-306/310 | 原命中/未中数可复算；SHADOW不进入REAL胜率和盈亏；无证书仍继续合法影子 |
| 历史5000期限制及合法补齐 | DIAGNOSTIC_SCOPE.draws=5000 | 热表≤5000；缺口有任务/原因；不清空模型、父代和证据 |
| 既有FTRL训练和R952在线模型更新 | 报告记录FTRL更新；R95225个数值更新产物 | 同输入前后模型哈希/数值/游标一致；重复投递不二次更新；不得把25条训练记作25个封存未来样本 |
| 长周期预测/发布与矩阵投影 | research_long_publication_r80=1041、research_champion_probability_r82=717等累计证据 | 保留既有计算产物和读取路径，统一角色/版本过滤；不因新增家族删除成熟路径 |
| 既有AtomNet样本/发布与恢复检查 | r30_atomnet_sample=2150、r31_atomnet_publication=19等累计证据 | 在可取得原件时验证恢复和消费；无法取到的原件保留缺口，不伪造 |
| 静态多家族、解释器、变异/融合算子 | FamilyRegistryR26、ModernFamilyFactoryCP12、EvolutionLoopR27 | 对现有家族和算子逐一固定输入回归；新增开放生成规则不取消旧家族 |
| 保护原件、故障持久化、恢复fence与CAS | fault originals/keep originals、DatabaseCoordinator、ResearchRetentionStoreR30 | 注入中断仍不丢原件、不放行损坏checkpoint、不重复确认commit |
| 服务、倒计时、启动日志及独立故障排空线程 | AutoFetchService、ServiceLoopTicketR341、StartupStageJournalR341 | 息屏、回桌面、划掉任务、系统杀进程后业务继续或明确恢复；不添加第二个服务写相同数据 |
| 报告完整ZIP、校验清单和证据身份 | 本次57成员、56载荷逐个SHA256通过 | 导出、取消、重新导出、解压、校验可重复；保留未知与失败状态，不为了全绿删检测 |
| 旧方案62工单、152验收及295文件卡 | 本轮逐项crosswalk及文件差异清单 | 既有136命名入口、10个B12分派用例、6外部验证合同仍有去向；每个变更明确保留/加强/合并原因 |

## 禁止的处理方式

不得通过删库、清空历史、重置未来样本、擦除故障、取消证书门槛、把常量标记改为PASS、只改版本号，或把有用页面整批移除来“修好”问题。

图表保留有用的账本、历史、模型、训练、演化和诊断视图。重叠无用的纯统计展示可合并，但每个旧图的原查询、指标及使用入口都需映射到新的决策用途；不把18张研究图用模拟数据填满。

## 保留回归的实际边界

本轮是方案及证据核验。没有原始SQLite/模型/APK，不能在本环境声称已经完成设备数值回放、覆盖安装或长时后台验收。施工批次必须从用户现有数据制作可恢复、可验证的副本后执行对应回归；出现差异先定界角色、版本、水位，再判断是否回归。
