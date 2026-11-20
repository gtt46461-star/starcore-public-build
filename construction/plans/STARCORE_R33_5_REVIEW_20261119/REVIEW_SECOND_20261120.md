# R33.5 第二次复核与逐批修订

复核起点是方案提交 `325fe67c7999ee8ea5fec5b9c25a24f4af57bbe1` 和正式 033 源码 `bb7b49bbcc9266bad548ec37e71403256fc06a4d`。本轮补入 V19—V25。九阶段和 66 项业务工单继续使用原 ID。有效施工清单现在有 164 次文件操作、138 条生产路径，其中 B02 从 37 次增至 40 次。累计审阅 15 份 Java 全文和 1 份 Android 清单。可从[当前工单与完整代码索引](WORK_ORDERS.json)找到各问题的原始工单、批次、相应操作、完整前后源码位置、模式和摘要。

## 两份现场报告给出的起点

| 033 原件 | 外层 ZIP SHA-256 | 原始现象 |
|---|---|---|
| A · e476ca61 | `e164852debfeab84eaffa4f98e215ce855bbeec5ff20334da5b0e0aab909f19a` | 心跳为 FETCH_STALE，冻结观测缺失。 |
| B · d8ca0632 | `da57ec5917c8922132c331c600f607d0833054879955895a8550897174c2b18f` | 抓取恢复后为 SETTLEMENT_STALE，冻结观测仍缺失。 |

两份设备原件均可完整解包，索引均为 `checksComplete=true`、`allChecksPassed=false`。两份均有 0 条独立未来期、0 条在线训练行、0 张有效证书，未来命中率为空。逐份核实 203 条处方，共 406 条，再按 JSON Pointer 复核 2,474 项原字段。报告可以定位停点。下面的时序和交接缺口则由生产源码与复现实验定位，旧报告没有记录交易提交返回的具体耗时。原报告里的家族训练停滞、进化消费缺失、空表和磁盘增长继续归于 B01、B03—B07 的原工单。[双报告逐项对照](reports/REPORT_RECONCILIATION.md)

## 本轮工单、生产代码与签收

| 复核项与归属 | 发现的缺口 | 施工落点与验收判据 |
|---|---|---|
| V19 · B02 / RT05、RT06、RT08、C01 | 百期验收已校验结算时刻，但标签写入、同哈希重试及训练读取没有统一校验开奖及首次观测两道时钟。 | `OosAfterDrawWriterR36` 按期号、来源开奖时刻、有效版本、draw hash 读取 first_seen_at，先核双时钟再产生标签。缺赛前原件的分支同样受约束。重试读回首次 settled_at，旧标签时钟非法则保持原件并记录原因。`SettlementEngine` 校验完成时刻。`FrozenFeedbackProofR335`、`strategyv2/RuntimeSchemaR26`、`DrawDependencyInputCP12` 从生产端复核来源、准确前期、字段类型和终态，合格新消费走 V3 身份。正例可恢复，提前写入、错前期、早完成均拒绝。 |
| V20 · B02 / C01、RE21，B08 / D06 | 事务内已填完成时间，SQLite 提交可以在更晚才返回。以事务内字段签收“1 秒落库”会漏报超限。 | `SettlementEngine`、`V51RuntimeOrchestrator` 在正常提交或恢复读回后记录同一请求、期号、结果身份和实测返回时间；`runtime/check_window.py` 独立核实冻结与结算两个返回观察、行摘要、日历和启用分母。分别输出缺数、P95、最大值与全部超限。至少 100 个启用机会均须有链，前期开奖到冻结可观测 ≤5,000ms，本期开奖到结算返回 ≤1,000ms。事务内 450ms、实际返回 1,001ms 的反例必须输出 INCOMPLETE。异步写日志的入库时刻单列，不能充作提交返回。 |
| V21 · B02 / RT05、RE21，B08 / C07、D06 | B08 的独立短链可能重新载入原卷旧检查器。遇延迟开奖，REAL 概率或交易回执超过固定截止，短链仍可通过。 | `research/check_research.py` 的 trace 与 `replay/final_review.py` 共用经 SHA 固定的当前 `verification/mainline/check_trace.py`。测试单独重算短窗的整份原件，两个 REAL 时点在截止前 1ms 可接受，等于截止或超过均拒绝。长短窗可使用各自原件，各自须通过。 |
| V22 · B02 / RE21，B08 / D06 | 原独立启用日记采集只调用 run-as，原签非调试版在标准设备上无法用该路线读取私有文件。 | `AndroidManifest.xml` 和 `EnabledWindowCaptureProviderR335` 提供受 DUMP 权限及 shell UID 约束的只读 URI，只投影已落盘的三项启用字段。写入中、字段重复、缺件、日记或状态不一致均拒绝。`runtime/prepare_tools.py` 展开后的采集器默认用 APP_OWNED，同一 APK、设备、日历、采集来源、START/END 双读与时钟均受核。原签手机须实际执行 START→运行→END；保留全部机会，包括明确停用与漏期。 |
| V23 · P00 / D01，B08 / D05、D07、C07 | 接收方复验 APK 时，所给源码 ZIP 或 bundle 可被替换，原包装时的成功回执不能识别后来收到的文件。 | `source/check_source_delivery.py`、`source/verify_delivery.py` 及 `replay/final_review.py` 重开收到的 ZIP，逐文件核 Git blob、模式和成员，核 bundle 摘要后在空 bare 仓恢复 HEAD、tree、033 祖先并执行 fsck。拒绝重算摘要后的错 ZIP、错树、依赖现有对象的增量 bundle。正式 B08 收据保存 `receivedSources`。 |
| V24 · P00 / D01、D04，后续各批交接 | 从关联工作树中的浅克隆开始，旧安装器可产出 P00 目录，B01 查找父提交时失败。 | `replay/stage_checked.py` 从 Git 公共对象库单独取本批输入提交，在新目录核已可见历史长度及浅边界，然后验证补丁、暂存区和已存在输出。`replay/verify_effective_stages.py` 同样使用此入口。全克隆、浅克隆工作树、重用和已有用户改动都有宿主回归。正式发布仍须交齐能从空库恢复的历史。 |
| V25 · P00 / D01、D13，所有批次 | 原问题卡上的完整源码对应原卷，经过前次及本次修订后，施工方按旧卡抄文件可能覆盖已修的后态。 | `WORK_ORDERS.json` 同时列出 66 项问题、所属批、原卡和原卡 SHA，以及每次真实操作的当前前后全文位置、SHA 和工单。`verification/check_work_orders.py` 通过相同安装逻辑重算并验证全部映射；任一未归属操作、跨批错配或索引漂移立即失败。B06、B08 原问题卡和独立验收合同均保留。 |

## 工程方取用

准备完成后，`PLAN` 为发布原卷展开目录，`REVIEW` 为同级 `current-review`。施工前可将全部当前前后态输出到一个全新的外部目录，逐文件对照。自动安装仍走 `replay/stage_checked.py`。操作按 P00 到 B08 依次为 **18、37、40、9、8、37、0、15、0**。B06 和 B08 分别完成自然资格和最终交付验收，另有各自版本事务和 APK 要求。

```bash
python3 -B "$REVIEW/check_plan.py" "$PLAN"
python3 -B "$REVIEW/verification/check_work_orders.py" "$PLAN" --emit-to "$NEW_EFFECTIVE"
python3 -B "$REVIEW/runtime/run_source_probes.py" --prefix "$PREFIX_WORK" \
  --android-jar "$ANDROID_JAR" --java "$JAVA_HOME/bin/java"
```

完整原件重组后复跑 21 项总入口检查。正式 033 和九批累计源码共 10 次全量 Java 类型检查、70 次原生成器命令；最终编译输入有 1,360 份 Java 和 2 份编译夹具。使用真实 Git 的九批版本继承、原 R967 身份、完整源码终验，以及 396 项报告断言、115 项原件保全检查均有回执。另用 JVM 运行当前结算、反馈及日记读取源码的 24 个断言。SQLite/IPC 等平台边界使用适配夹具。原签 APK 的安装、Android IPC、vivo V2353A 后台 24/72 小时、真实百期和新候选独立自然效果待逐批实测。[复验回执和覆盖范围](../../verification/STARCORE_R33_5_20261120/second-review/README.md)
