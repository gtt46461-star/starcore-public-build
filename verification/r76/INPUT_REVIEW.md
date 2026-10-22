# R76 输入报告复核（只读）

本轮收到的是 **R75 的真实手机报告**，不是 R76 安装结果。APK SHA 与 R75 原签名构建完全一致。报告完整、96 项全部执行、0 查询错误；业务仍有 **3 FAIL、6 WARN、22 NOT_PROVEN**。本审计不改产品、不重跑全量测试，不把检查数量减少当作完整闭环。

## 身份、时间与证据边界

- 输入 ZIP SHA256：`a4acc60904ecef8b88fcd5e5b4c3b5707a057652c4fdf7ee2bb6d694eaf68825`，已对上传文件重新计算。
- 版本：`V57.21.2.0.75-R5.4.2.75-1013-O09` / `577251034`；APK SHA256：`4cf8eac63ff37ea4b320426e55efbb4ad87010e4b2e570e2818e83f83139abaa`。
- 快照：`2723e68fa2fc8ec272b89912662e169e8622e37654ebb59465281023a7f1c013`，4,277,227,520 字节，418 表、6,702,647 行。42 个原始导出成员、9 个固定附件、动态章节引用及 manifest 本体 hash 均核验；62 组完整元数据无截断。未携带原始 SQLite/APK/模型二进制是声明的诊断范围，不是遗漏错误。
- 以下时间按 UTC+08 显示。15:53:12 系统记录安装替换，15:53:17.205 开始 release epoch，15:53:18.760 服务启动。运行观测为 **16:04:14.764**，距离服务启动约 **10 分 56 秒**；数据库最新开奖483、目标484。FILE_STATE 是导出时的活文件观察，不能冒充相同时间的数据库快照。
- 15:57 两张截图显示 `LIGHT_SNAPSHOT_TRUTH_NULL / READ_ERROR`，确有界面取值异常，发生在上述报告观测之前。16:15 截图显示当前494/目标495，比报告同角色期号晚 **11 期**；它已有第6位置03/09/10的影子观察和“V51号码概率/1200动作排序”，公式资格为 `MAINLINE_FORMULA_UNQUALIFIED`。不能把取值失败、未合格公式或旧报告解释成“完全没有算法/没有出号”。
- `crashCount=172`、`lastAt=1791806924296` 是10月12日20:08:44的旧累计，未发生变化；本版新增系统退出项是 `installPackageLI`。保留的 `windowCrashCount10m=1` 不能脱离旧 lastAt 当作新崩溃。本输入没有证明新增fatal，也不能保证所有崩溃都不存在。

## 当前可以定位的阻塞

| 优先级 | 事实与准确定位 | 修复/验收方向 |
|---|---|---|
| P0 | RSEC022/033/052：本版9个分析事件，P95=26214ms。483期 PRE_HISTORY_METADATA=15844ms/CPU263ms，SELECTION_POLICY=8069ms/CPU568ms；484重试首先8行历史写入 CP3_DB_BUSY_DEADLINE，成功时DB_CANONICAL_WAIT=5258ms/CPU83ms。该两期bundle打开仅3–21ms。 | 查历史metadata/历史补写/提交争用和调度壁钟，不能仅优化模型算术或重复归因冷bundle。保持deadline、持久请求和真实源身份。 |
| P0 | 元数据 `research_capture_gap_v2`：升级后新记录6条，2条是旧354缺口到474才被发现；另外4条是两个候选各漏480和482。`v57_issue_projection_r926` 中480/482 canonical_seen=0，均LEARNING_PENDING。 | 修持续赛前捕获与分析时效。旧缺口与本版缺口分开，绝不事后补写冻结预测。 |
| P0 | 截图01/02轻量truth为空；截图03标题“心跳异常”等待恢复，同时内文RUNNING/手动启动待确认。 | 修轻量快照输入与共享健康状态呈现，保留UNKNOWN/READ_ERROR；不把读取失败转换为0或无算法。 |
| P1 | modern input/protocol=6（本版新建5，旧1）、feature cursor=2且next_index仅3/16；development receipt/artifact identity仍0。矩阵spec26/checkpoint15，15个checkpoint全为本版。 | 代码已经真实产生准备和矩阵进度，仍需查特征恢复/调度预算及真实规模输入耗时，验完成训练冻结与重启后持续消费。 |
| P1 | RSEC006：4.277GB DB，revision 3,617,409行，archive=0。`REPORT_STAGE_TIMEOUT` 的更新时间早于本版epoch约277秒，新回收未释放页。 | 查当前归档请求/备份IO及磁盘占用；不能把旧timeout说成新异常，也不能把行数直接换算各表字节。 |
| P1 | RSEC052/074在HEALTHY下仍FAIL：源码 `collectRuntimeSnapshotCP4` 的P95>5000门确未通过；Finding却使用通用“service failure”文案。RSEC078也把旧入口IDLE与现代未完成混在一起。 | 精确报告失败谓词、source/read状态与现代/旧学习角色，不靠改绿掩盖SLA。 |
| 自然证据 | 候选cal model、quality、formal window/pair、证书、正权重贡献均0。203个CAL失败全部SCHEDULE_GAP，跨study settled总量不是同一模型241期。 | 先保连续、训练冻结，再独立CAL/Future和质量准入。10分钟本版运行不能证明这些长期效果；不得改PROVEN、继承Future或强制REAL。 |

资源原始行：`report-text-9d53f39df519e380c66b955a6a9e5f3db6dfc83f4fed4dbd6cca37246eae6273.txt`；每个execution_id、行号和长span已进入 `report_findings.json.performance.resources`。完整31个未通过项在同JSON的 `all_nonpass_checks`，每项包含原catalog文件/行号、源方法、证据hash、截取、处理类别与验收义务。

## 与上份 R74 报告及 R75 施工预期对照

| 证据 | R74 报告 | 本份 R75 报告 | 解释 |
|---|---:|---:|---|
| FAIL / WARN / NOT_PROVEN | 6 / 8 / 20 | 3 / 6 / 22 | 各状态变化要逐项看，NOT_PROVEN增加不等于新增崩溃。 |
| 当前release分析事件/P95 | 2 / 7883ms | 9 / 26214ms | 不同短样本不可当控制实验，但本版确超5秒。 |
| 现代input/protocol | 0 / 0 | 6 / 6 | 5套本版创建，另1套在上份报告之后但升级之前创建；不把两份报告间全部变化归R75。 |
| feature cursor / development receipt | 0 / 0 | 2 / 0 | 有真实特征准备，尚无训练完成收据。 |
| matrix spec/checkpoint | 0 / 0 | 26 / 15 | 真实接线已运行，不能当20类型全闭环。 |
| 旧数值artifact / 已结算online标签 | 413 / 421 | 415 / 428 | 旧学习存在；同报告FTRL更新1114到483。不能统称没有深度学习/算法代码。 |
| deferred pending | 57 | 94 | 当前最新目标full可通过，不代表历史积压已消失。 |
| CAL失败 / cal模型 / 证书 | 186 / 0 / 0 | 203 / 0 / 0 | 失败都为调度缺口，不能归咎“最佳公式胜率差”，也不能解锁REAL。 |
| archive / revision | 0 / 3354442 | 0 / 3617409 | 有界清理主机通过不等于手机归档已经释放空间。 |

R75受控HTTP/最终字节码/合成SQLite测试证明指定分支与拒绝边界可执行，并不承诺本机源端必在5秒内提供数据。实际本版最新482/483由BACKUP在开奖后约4.3/3.7秒入库，同时仍存在更慢期和后续分析IO阻塞，应分开测量“源可用/首次抓到/落库/分析/显示”。本轮报告并未给出截图16:15当刻的完整流水。

未通过检查索引：

| 检查 | 原状态 | 标题 | 处理类别 |
|---|---|---|---|
| RSEC-009 | NOT_PROVEN | 【0.当前总判定】 | FINAL_REQUIREMENT_AND_DEVICE_EVIDENCE |
| RSEC-019 | NOT_PROVEN | 【E05-A.模型准确率卡】 | NATURAL_MODEL_EVIDENCE |
| RSEC-020 | NOT_PROVEN | 【E05-B.策略效果卡】 | NATURAL_POLICY_EVIDENCE |
| RSEC-021 | NOT_PROVEN | 【E05-C.期级链路卡】 | CODE_INVESTIGATION_AND_DEVICE_RETEST |
| RSEC-022 | NOT_PROVEN | 【E05-D.资源效率卡】 | CODE_INVESTIGATION_AND_DEVICE_RETEST |
| RSEC-030 | NOT_PROVEN | 【4.R3.6硬门】 | CODE_INVESTIGATION_AND_DEVICE_RETEST |
| RSEC-033 | WARN | 【7.R3.6运行健康账本】 | CODE_INVESTIGATION_AND_DEVICE_RETEST |
| RSEC-034 | NOT_PROVEN | 【8.严格OOS/Wilson/ECE/Brier/Coverage】 | NATURAL_MODEL_EVIDENCE |
| RSEC-035 | NOT_PROVEN | 【9.模型有效性与根因优先级】 | MIXED_RUNTIME_AND_NATURAL_EVIDENCE |
| RSEC-037 | NOT_PROVEN | 【11.1200正式动作与实时/延后写分离】 | CODE_INVESTIGATION_AND_DEVICE_RETEST |
| RSEC-039 | NOT_PROVEN | 【13.REAL与SHADOW分账】 | NATURAL_QUALIFICATION_REQUIRED |
| RSEC-052 | FAIL | 【第2章 Service与业务运行】 | CODE_INVESTIGATION_AND_DEVICE_RETEST |
| RSEC-054 | NOT_PROVEN | 【第4章 100号码概率面】 | RUNTIME_METRIC_AND_NATURAL_EVIDENCE |
| RSEC-055 | WARN | 【第5章 跨位置结构】 | RUNTIME_METRIC_AND_NATURAL_EVIDENCE |
| RSEC-056 | WARN | 【第6章 固定K3漏斗】 | RUNTIME_METRIC_AND_NATURAL_EVIDENCE |
| RSEC-058 | NOT_PROVEN | 【第8章 动作Ranker】 | RUNTIME_METRIC_AND_NATURAL_EVIDENCE |
| RSEC-059 | NOT_PROVEN | 【第9章 选择后校准与严格OOS】 | CODE_INVESTIGATION_AND_NATURAL_EVIDENCE |
| RSEC-060 | NOT_PROVEN | 【第10章 频率与REAL硬门】 | NATURAL_REAL_EVIDENCE |
| RSEC-061 | NOT_PROVEN | 【第11章 失败树与反证】 | NATURAL_QUALIFICATION_REQUIRED |
| RSEC-062 | NOT_PROVEN | 【B14动态故障诊断】 | NATURAL_QUALIFICATION_REQUIRED |
| RSEC-063 | NOT_PROVEN | 【确认事实/假设分离】 | NATURAL_QUALIFICATION_REQUIRED |
| RSEC-064 | NOT_PROVEN | 【因果故障图】 | NATURAL_QUALIFICATION_REQUIRED |
| RSEC-065 | NOT_PROVEN | 【修复优先级】 | NATURAL_QUALIFICATION_REQUIRED |
| RSEC-066 | FAIL | 【K3失败九类拆解】 | TRAINING_AND_NATURAL_QUALITY_REQUIRED |
| RSEC-068 | NOT_PROVEN | 【策略贡献/冲突/冗余/消融】 | NATURAL_ABLATION_REQUIRED |
| RSEC-069 | WARN | 【公式/算法/概率审计】 | MIXED_RUNTIME_AND_NATURAL_EVIDENCE |
| RSEC-070 | WARN | 【第13章 代码Owner完整性】 | MIXED_RUNTIME_AND_NATURAL_EVIDENCE |
| RSEC-071 | WARN | 【代码Owner与完整性审计】 | MIXED_RUNTIME_AND_NATURAL_EVIDENCE |
| RSEC-073 | NOT_PROVEN | 【改进Challenger计划】 | NATURAL_CHALLENGER_EVIDENCE |
| RSEC-074 | FAIL | 【第15章 Finding完整施工账本】 | CODE_AND_DIAGNOSTIC_PRECISION |
| RSEC-078 | NOT_PROVEN | 【第19章 R811结算、证据、SHADOW与Research统一真值】 | CODE_AND_DIAGNOSTIC_PRECISION |

`completes_contract=true` 是该诊断项完成执行，不代表它覆盖的原始业务合同通过。062–065复用061的同一动态诊断，070/071复用同一Owner审计，应避免重复计为独立故障。台账UNKNOWN不能改成未施工，更不能批量改成已完成。当前验收仍缺持续手机运行、准确取值、每期收敛、自然独立资格、真实主链贡献及1–9/1832条适用合同的合取证据。
