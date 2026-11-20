# R33.5 B07 报告生产闭环复核增补

本增补修复两个经当前完整后态源码确认的遗漏：方案要求的故障原件到对象诊断没有生产者；需求到新增能力提案只有宿主合同夹具，没有应用内出口。施工仍归 **B07**，使用本目录的 7 个完整生产后态，不增加需要未来 B08 才能编译的依赖。原始两份 033 报告保留原字节。

## 原工单内补正与完整文件

本次两条 `R335-REV-R01/R02` 是复核补正子记录，归入原有 `R335-D10 / R335-D11 / R335-RT09`，**不新增业务工单、不扩阶段**。原 66 项业务工单、9 阶段口径保持；不得把两条补正再相加宣称 68 项业务工单。

| 补正子记录及原工单 | 已发现的断点 | 本次生产代码 |
|---|---|---|
| R335-REV-R01；归 D10、RT09 | 原方案的 `DiagnosisTraceReaderR33` 不存在，`verifiedFaultTraceR33` 仅被 `RepairDiagnosisR32` 读取，没有实际采样/绑定调用 | 新增 `DiagnosisTraceReaderR33.java`；在 `ReportSnapshotR30.capture` 的同一 SELECT 完成和水位核验后捕获任务/故障投影；`ReportObjectIndexR30.buildProductionR31` 绑定对象；`DiagnosisProofR33.classify` 区分共享 owner 观察和未确认因果。 |
| R335-REV-R02；归 D11、RT09 | `NEW_CAPABILITY_PROPOSAL`、`EXISTING_CAPABILITY_BINDING_GAP` 只在 Python 夹具；应用没有 `candidateCapabilityProposalsR33` | 新增 `CapabilityNeedRegistryR335.java`、`CapabilityNeedDiagnosticsR335.java`；从 8 条已审阅有限需求检查实际类与精确声明签名；`ReportObjectIndexR30` 接生产者；`RepairDiagnosisR32` 同时输出 JSON 和 Markdown。 |

完整路径、旧 B07 终态输入 SHA、新后态 SHA、原始 B07 前态 SHA 与已有批次链见 `REPORT_OVERLAY_MANIFEST.json`。新类 3 个，既有类变更 4 个。`ReportSnapshotR30` 继承 B05 后态再在 B07 修改；`ReportObjectIndexR30` 原先未在应用清单变更，本次新增 B07 替换。`RepairDiagnosisR32` 与 `DiagnosisProofR33` 修改既有 B07 后态，保留原始 B07 前态供正式阶段安装器核验。

## 生产读取边界和真实状态

`captureSingleSelect(watermark, captured, cancellation)` 是本次生效入口。它只消费 `ReportSnapshotR30` 已取得的同一 SELECT 原始投影，在 hot/cold 原件替换或进程观测写入前调用。原方案文字中的 `DiagnosisTraceReaderR33.readPinned(SQLiteDatabase,...)` **由此入口替代**；不再逐对象重新查询数据库，不添加空壳 `readPinned`。这里无数据库写动作、资格修改或数值复演。

冷归档专有原件不在这个同一 SELECT 投影中，仍由既有 hot/cold 附件链单独保全，不能把本次 trace 缺口当作历史无故障。

最多保留 100 个任务和 20 个故障投影，原始投影总预算 192 KiB，单行 32 KiB。每条保留实际表名、主键、规范 JSON 哈希；超过预算、缺来源、截断引用、未出现在有界样本中均保留缺口。取消沿原 `CancellationSignal` 传播。共享源包只验一次哈希，随后绑定对象，避免按 165 对象反复散列整个包。

故障证据按实际 `task_id` 关联，再分别记录 `owner_epoch` 与 `fence` 是否同当前任务匹配。F 家族、I 思路族、矩阵和研究图可以共用任务，因此明确标 `SHARED_OWNER_TASK_NOT_OBJECT_ROOT_CAUSE`。任务所存原因可能是正常等待或旧原因，不能直接视为故障。专家任务按精确 `expert-E##` 匹配，也仍不证明数值输入已经复演。没有模型原始输入字节、独立执行回执时，根因保留未确认，不能把原行哈希当作模型输入哈希。

需求目录由已审阅的 `CAPABILITY_NEEDS_R335.json` 生成，8 条分别覆盖家族执行、十位置实训、研究动作消费、独立资格、唯一决策结算、逻辑表审计、故障追踪、需求诊断。生成器逐条检查其 `sourceContract` 真正存在，并固定目录哈希。它是有限的已登记需求目录，不声称自动穷举所有未知未来功能。

检查生产类时使用 `Class.forName(name, false, loader)` 和精确参数类型列表匹配 `getDeclaredMethods`。只检查声明，不初始化类、不反射执行方法。**`CapabilityNeedDiagnosticsR335.java` 必须在 R967 受控输入中声明 `dynamicReview=true`，并绑定此后态 SHA**；这是固定有限目录的明确动态代码审阅，不能删正则命中或修改生成器跳过审阅。

反射所用构建 `BuildConfig.SOURCE_COMMIT_R26` 必须与报告 `sourceCommit` 完全相同；否则 `EVIDENCE_MISSING`。全部规定 owner 都可检查且规定入口全部缺失，才输出限定范围的 `NEW_CAPABILITY_PROPOSAL`；仅部分缺失为 `EXISTING_CAPABILITY_BINDING_GAP`；有声明而有适用阻因时为 `EXISTING_CAPABILITY_BLOCKED`；有声明尚无有效运行证据仍是 `EXISTING_CAPABILITY_DECLARED_RUNTIME_UNVERIFIED`。缺原对象、重复对象、加载异常、错版本不得被改判为新增能力。基础诊断需求不会借关联 F01 的训练阻断宣称自身阻断。

提案保存在独立的 `candidateCapabilityProposalsR33` 数组，包含需求、范围、最近既有对象、输入、责任批次、目标文件/方法签名、正反例和下一动作。固定 165 对象索引保持不变。声明、提案、原故障观察、工程运行与自然效果分别计证。

## 可直接复跑

正式总安装器应将本增补合入 B07 的完整后态，再统一重算清单、R82/R967 身份及关联工单副本。本目录的独立安装工具只适用于**原有 B07 已完整应用、尚未应用本增补**的源码，先检查全部输入再写入；相同后态可幂等重跑，任何第三种字节拒绝。它不做 Git 提交和 APK 打包。

```bash
python3 apply_report_payload.py --source "$SOURCE"
python3 apply_report_payload.py --source "$SOURCE" --check
python3 generate_need_registry.py --plan "$PLAN" --check
python3 run_report_closure.py --source "$SOURCE" --plan "$PLAN" \
  --android-jar "$ANDROID_JAR" --annotation-jar "$ANNOTATION_JAR" \
  --java "$JDK17_JAVA" --out "$FRESH_OUTPUT"
```

运行器不依赖本次机器的绝对路径或历史 classpath。它校验 7 件安装字节、目录源码，再调用完整方案的 Java 类型检查器重新编译全部源码，以新编译类执行生产测试。两份原报告从方案的 `evidence/original` 解包前核对固定外包 SHA，只取原 `R30_REPORT.json` 作只读宿主回读。

`REPORT_CLOSURE_RECEIPT.json` 记录完整命令、输入 SHA、源文件数和结果。测试覆盖缺 owner、缺精确签名、部分绑定、缺来源、错源码、重复需求、原行/信封篡改、错水位、旧 fence、共享 owner、行数/字节上限、原两报告的生产诊断消费和 JSON/Markdown 出口。断言数包含两次 165 行逐项检查，不等于同等数量独立故障场景。

在本次原报告宿主回读中，两份各得到 72 个已有任务/故障投影，分别让 120、116 个报告对象关联到明确限定范围的 owner 观察；其中 104 个使用共享 owner，不计为独立根因。新构建反射与 033 报告版本不一致，所以这些回读中的 8 条需求分类均保持 `EVIDENCE_MISSING`，不冒认旧源码具有新代码。这验证的是取原件、接线和诚实分类。

完整 Android SQL/进程执行、安装、24/72 小时连续运行、实际故障数值复演与自然未来效果继续由原 B07/B08 及质量协议取得现场证据，宿主断言不代替这些结果。
