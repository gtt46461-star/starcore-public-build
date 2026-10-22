# R76 工程验收记录

状态：固定源码 `ede9746f9172033d27cf6ed15456780d4a92ca4d` 的严格 release 构建、51项单元测试、既有基线下 lint、原签名及12组最终字节码主机回归通过；APK 已保存。远端 Android 构建/滚动回滚/独立运行测试步骤通过，但最后日志 artifact 上传失败，整条 CI conclusion 仍为 failure。手机现场验收未完成。本文不改写用户原报告，不是全部 1–9 施工验收通过证明。

交付版本 `V57.21.2.0.76-R5.4.2.76-1013-O09`，versionCode `577251035`；APK 25,727,289字节，SHA-256 `757ff09c339358398146f7bebb8c5ed894356a02d22a2e1b6c67525802480e8e`。签名 SHA-256 `ae7e1b4a5a71f1f299d4a0b2472c6a36513873514e78190d42d6cfba3340116f` 与前版一致，实际覆盖安装未测。

## 输入与时间范围

用户本次上传的 ZIP SHA-256 为 `a4acc60904ecef8b88fcd5e5b4c3b5707a057652c4fdf7ee2bb6d694eaf68825`，实际运行 APK 是 R75（577251034），其安装 APK 哈希与上轮交付一致。42 个导出成员、9 个附件、14 个动态引用、manifest body 校验通过；96 项中仍有 **3 FAIL、6 WARN、22 NOT_PROVEN**。逐项原始文件、行号、方法、hash 保存在 `report_findings.json`。

本版服务观测约 10 分 56 秒，报告最新开奖 483、目标 484；16:15 截图最新开奖 494、目标 495，比报告晚 11 期。截图 `LIGHT_SNAPSHOT_TRUTH_NULL` 是显示读取不可用，不能推导没有算法或没有分析；报告已有旧 FTRL/在线数值更新，以及现代协议和特征进度。crashCount=172 与上一报告相同，最后一次为 10 月 12 日旧记录，不能当新 R75 崩溃。

## 当前工程变更与开发证据

| 工作 | 已证实范围 | 仍待证实 |
|---|---|---|
| UI | 40 项真实 Activity 入口检查；PENDING、同 session 源 clock、目标绑定缓存、两次取 clock 的渲染 fence、异步 badge fence。旧代码与无第二 fence 均有负例。 | Android 布局/完整生命周期、自然连续倒计时与抓取 SLA。 |
| 研究中文快照 | 7 模式 90 项只读显示检查；区分在线学习、现代开发、子代、独立评价、原子目录/准入、矩阵定义/投影、校准/证书。 | 不产生训练、Future、证书或 REAL；段间 350ms 预算不是 SQLite 硬超时。 |
| 待执行队列 | 9 项语义及真实 claim/yield；3537 报告任务 maintenance VM 步 72342→1137，+5万完成历史 1222198→1029，同22个ready。 | 本地orphan查询10625→149（+5万历史160623→147）；live token保护、generation1→2恢复及旧token拒绝通过；不是手机时延。 |
| 真实开发续跑 | 认证报告 6 个原始输入 hash 与两个 prefix；最终 release 字节码在新 JVM 续跑 7920 特征、6 收据/产物/冻结，每个1200开发+120验证，第三 JVM 无重复。 | 仅隔离重建测试，非手机完成；未增加自然Future、证书或REAL。 |
| 校准待结算 head | 9 项公共入口检查；5000×8KiB 历史 VM 步270169→179，保持热表优先与脏行遮蔽。 | 自然连续校准捕获、现场延迟；480/482缺口不可补造。 |
| 校准维护续跑 | 19 项；合成 host 241 前瞻输入，第120期换 JVM，真实捕获/结算/公共维护/拟合。ECE约0.12118，质量仍不合格。 | 不是手机自然241期；formal pair/cert/REAL仍0。 |
| 观测 I/O | 81 项；12类观测 apply 即时内存可见，权限/停启/lease仍 commit；pressure无每次fsync，session仍持久；失败保旧与实际 reader边界。 | 显式注入主机延迟，不能解释全部手机秒级停顿。 |
| 归档页复制 | 最终 JNI主机库+release字节码49项输入/复制、52项归档CAS、8项WAL/大单行边界通过；开发4,590,436,352字节库的128条小记录仅生成36,864字节快照，原库字节不变。 | Android设备归档/手机存储压测未验；宿主耗时不是手机指标。 |
| FAIL 原因精准化 | 最终字节码23项通过；HEALTHY但P95超标时显示实际延迟、失败/心跳事实，真实READ_ERROR保留。 | 不降低失败阈值；原报告FAIL仍保留，实际手机P95未验证改善。 |

开发续跑与241期维护属于现有真实Owner的验证，不表示重写全部模型或质量门；直接产品修复与验证-only在JSON分别标记。

最终12组主机回归覆盖上述9项，以及真实抓取回调33项、11个受审阅旧训练身份与3个非法列表反例、共享训练调度66项/旧outbox恢复10项/旧NEURAL协议数值训练。3,419个release类在测试及补跑前后哈希完全一致，无测试类覆盖产品类。实际Java/SQLite/JNI入口使用显式主机Android适配器，不冒充真机。

保留两次非产品失败：首次严格构建已成功，但输出目录有旧R75包，发布选择器按规则拒绝 `FINAL_APK_NOT_UNIQUE`；移开旧包后完整严格构建/签名校验通过。首次共享旧协议测试错误使用新报告，候选已由PROPOSED变为WAIT_FUTURE，真实CAS拒绝重复登记；使用该旧测试原有R74报告后通过，新报告6协议续跑另有独立通过证据。未删断言、未改门槛。详见 `evidence/final/first-suite-attempt.json`、`suite.json` 和 `build-first-attempt.json`。

各项精确源码、receipt 路径、hash、范围和限制见 `evidence_map.json`，不得把测试断言总数相加当成业务要求完成数。UI 时钟修复采用已有认证源 anchor，不从墙钟编造新一分钟。

## 必须保留的现场与资格限制

- 本轮无法保证上游在开奖后 5 秒发布；剩余 55 秒抓取到新开奖的连续手机证据尚未取得。需要同一自然期完整源时间、首次抓取、落库、分析、冻结、结算收据。
- 报告当前 release 的 9 次分析事件 P95=26214ms 是真实未达标，不因开发查询/IO回归通过而变成手机 SLA 通过。导出的 28 条 DBTX75 中 BEGIN 最大1.228ms、body最大333.809ms、END最大96.645ms，不能把未观测耗时推成全部 SQLite BEGIN。
- 原报告现代开发收据和 artifact identity 为0；隔离测试生成的6模型/7920特征没有写回用户手机。连续训练、后台恢复和自然新期反馈仍需本版现场收据。
- 没有同一冻结候选的独立 CAL/Future/质量证书时，不能把未知胜率当低胜率，也不能强制解除 REAL 门。合成 host 捕获与训练验证不是自然 REAL 出手。
- 旧两大升级备份合计3,711,119,360字节保留。单纯版本号变化，在critical revision/contract/DDL未变时不触发新整库升级备份；不称已回收这3.71GB。报告cache约551KB不是8.2GB主因。
- 归档状态中的旧 `REPORT_STAGE_TIMEOUT` 比R75进程epoch早约277秒，不是本版新超时证明。live文件inventory与DB snapshot时点不同；无rawDB/dbstat，不能按行数推断各表占用字节。
- 1–9方案与1832要求存在于仓库；台账unknown不等于未施工，也不等于已验收。本轮没有全业务无冲突/全部门槛通过证据。

## 原报告31项非通过义务

下表只登记覆盖关系，不批量把原 FAIL/WARN/NOT_PROVEN 改为 PASS。所有项仍 `closed=false`；精确原始定位在 JSON。

| 原检查 | 原状态 | 本轮关联证据 | 剩余验收义务 |
|---|---|---|---|
| RSEC-009 | NOT_PROVEN | UI, RESEARCH_UI, QUEUE_READY, TRAIN_RESUME, CAL_HEAD, OBSERVATION_IO, RETENTION | 1–9/1832复合要求逐项绑定最终源及现场证据；未知不能当未施工或已验收。 |
| RSEC-019 | NOT_PROVEN | TRAIN_RESUME, CAL_MAINTENANCE, RESEARCH_UI | 修复连续捕获后积累同一冻结候选独立CAL/Future；模型和质量证书为0。 |
| RSEC-020 | NOT_PROVEN | CAL_MAINTENANCE | 冻结策略同窗口验证覆盖/命中/ECE/稳定性；不得用频率债或强制REAL代替。 |
| RSEC-021 | NOT_PROVEN | UI, CAL_HEAD, QUEUE_READY, OBSERVATION_IO | 期级影子/赛前冻结/结算证据需齐全，当前480和482缺canonical_seen。 |
| RSEC-022 | NOT_PROVEN | QUEUE_READY, CAL_HEAD, OBSERVATION_IO, RETENTION | 优先查历史metadata、writer/commit争用、policy壁钟开销，冷bundle已非该样本主瓶颈。 |
| RSEC-030 | NOT_PROVEN | UI, CAL_HEAD, QUEUE_READY, OBSERVATION_IO | 覆盖8/30混合升级前期，另独立验474–484当前期；不得补造漏期冻结。 |
| RSEC-033 | WARN | QUEUE_READY, CAL_HEAD, OBSERVATION_IO, RETENTION | 当前release 9事件P95=26214ms；需要真实设备首抓→落库→分析SLA，不是旧累计。 |
| RSEC-034 | NOT_PROVEN | CAL_MAINTENANCE | 1131个严格影子OOS不等于候选CAL/证书；保留N=1131/Hit357，补合法冻结概率才能评价Brier/ECE。 |
| RSEC-035 | NOT_PROVEN | CAL_HEAD, TRAIN_RESUME, RESEARCH_UI | 先消除当前期漏捕与写入/时延，再验独立模型证据，不把未知质量当胜率差。 |
| RSEC-037 | NOT_PROVEN | QUEUE_READY, OBSERVATION_IO, RETENTION | deferred pending57→94，需要有界消费追平，保持1200动作集合及每期唯一身份。 |
| RSEC-039 | NOT_PROVEN | RESEARCH_UI, CAL_MAINTENANCE | Exact gate没有合法证书时保持SHADOW；不能自动解锁以制造真实出手。 |
| RSEC-052 | FAIL | QUEUE_READY, CAL_HEAD, OBSERVATION_IO, RETENTION, RUNTIME_REASON | HEALTHY表示循环活跃；P95>5000仍触发FAIL。定位新鲜SLA瓶颈和484历史写失败重试。 |
| RSEC-054 | NOT_PROVEN | 无新增直接专项 | 分布/Sinkhorn运行指标没有持久样本；核查当前Owner写出与report读取，不把静态实现或别表行数冒充。 |
| RSEC-055 | WARN | 无新增直接专项 | relation/permutation/transition报告无样本；核对精确目标/PIT源和当前指标消费者。 |
| RSEC-056 | WARN | 无新增直接专项 | 静态K=9..3不是逐期funnel证据，核查持久指标入口并回读本期。 |
| RSEC-058 | NOT_PROVEN | 无新增直接专项 | Ranker无runtime sample；需要同一期完整动作排名指标及冻结身份。 |
| RSEC-059 | NOT_PROVEN | CAL_HEAD, CAL_MAINTENANCE, QUEUE_READY | 203个gap中4条为本版480/482漏期，2条为旧354缺口本版发现；先解决捕获连续性，不能拼跨study241。 |
| RSEC-060 | NOT_PROVEN | RESEARCH_UI | REAL账本total=0，空账本无重复不等于已经实际出手；需要自然合格候选经唯一gate。 |
| RSEC-061 | NOT_PROVEN | RESEARCH_UI | 当前动态诊断已转模型证据未完整；相同诊断被多个章节复用，不计独立六个故障。 |
| RSEC-062 | NOT_PROVEN | RESEARCH_UI | 与061同诊断hash；须真实CAL/Future/证书，非另一个独立运行崩溃。 |
| RSEC-063 | NOT_PROVEN | RESEARCH_UI | 与061同诊断hash；保留真实证据不足。 |
| RSEC-064 | NOT_PROVEN | RESEARCH_UI | 与061同诊断hash；保留正式门禁，不能凭修复数保证质量。 |
| RSEC-065 | NOT_PROVEN | RESEARCH_UI | 与061同诊断hash；反馈/研究可继续，生产须独立资格。 |
| RSEC-066 | FAIL | TRAIN_RESUME, RESEARCH_UI | NumberQ/Ranker/SelectedHitP显示工程种子未证明；实际训练冻结并按各自自然OOS合同验收，不靠改PROVEN字段。 |
| RSEC-068 | NOT_PROVEN | 无新增直接专项 | 无冻结OOS消融，策略增益/冲突/冗余不能编造；现代矩阵少量checkpoint亦不等于正式消融。 |
| RSEC-069 | WARN | CAL_HEAD, RESEARCH_UI | 8个WATCH/UNAVAILABLE涵盖模型/校准/Sinkhorn/PIT/离线parity；按精确原条款分别补证。 |
| RSEC-070 | WARN | 无新增直接专项 | Owner审计11PASS/5WATCH/1NA/0BLOCK，重点PIT状态与未证明模型；不能作为全代码无冲突证书。 |
| RSEC-071 | WARN | 无新增直接专项 | 与070同一Owner审计，避免重复计算为独立代码冲突。 |
| RSEC-073 | NOT_PROVEN | TRAIN_RESUME, CAL_MAINTENANCE, RESEARCH_UI | Challenger建议不等于候选已合格；需新训练/冻结/CAL/独立Future比较和证书。 |
| RSEC-074 | FAIL | RUNTIME_REASON | F020的FAIL由P95门触发；HEALTHY时通用“service failure”原因过宽，需显示实际SLA证据而非假称服务已死。 |
| RSEC-078 | NOT_PROVEN | RESEARCH_UI, TRAIN_RESUME, QUEUE_READY | 现代input/protocol已有6、feature cursor2但development receipt0；旧FTRL/online有数值更新。分开写现代未完成和旧学习存在，不概括无算法。 |

特别注意：054/055/056/058 的当前期概率/跨位结构/漏斗/Ranker运行指标、068正式消融、070/071全部Owner与计划验收，目前没有新增直接专项证明；066的NumberQ/Ranker/SelectedHitP不由六个现代候选开发产物自动证明；078中文UI已经分链显示，但原R811报告 evaluator 结论尚不能宣称已被修正。

## 最终发布门

| 门 | 状态 |
|---|---|
| 固定源提交 | PASS：ede9746f9172033d27cf6ed15456780d4a92ca4d，正常分支更新并回读 |
| 严格 release 构建、unit、lint | PASS：51测试/0失败/0跳过；lint沿用原基线，55个已有错误被基线过滤，不称零历史问题 |
| APK 签名/身份 | PASS：577251035、三ABI、原签名、目录资产和生成身份验证 |
| 最终 release classes 无 overlay 回归 | PASS：12组，3419类字节不变；主机适配器范围 |
| Android CI | 验证步骤7–12 SUCCESS；第13步日志artifact上传 FAILURE，因此整条CI仍FAILURE；run 38376578405，同源码提交 |
| APK 交付 | SAVED：STARCORE-R76-20261013.apk，与构建收据SHA一致 |
| 手机连续55秒/后台/逐期闭环 | NOT_VERIFIED |
| 自然CAL/Future/合格REAL主链 | NOT_VERIFIED |
| 全部1–9复合业务验收 | NOT_ESTABLISHED |

发布项均以本轮实际收据更新，没有继承上轮R75构建或手机状态。CI job 115226361570 已完成；独立Android运行步骤耗时17分52秒并SUCCESS，滚动/回滚步骤SUCCESS。第12步日志保全SUCCESS，第13步artifact上传FAILURE。完整job日志接口返回Transport closed，annotations读取端点不可用，未取得上传失败的具体原因，也未取得远端逐项JSON；不猜测为配额问题。`evidence/final/ci-result.json` 保存官方run/job状态与各步骤结果。不能将整条CI改写为全绿；上述现场与自然资格限制继续保留。
