# 33 条原验收的外部证据操作规程

**状态：32 条 NOT_RUN，1 条自然资格 NOT_PROVEN；本文件没有执行、签收或新增任何原 JUnit/HOST 自动绑定。**

33 条全部保持原 P5 归属、原 ID 和真实 222 条原文，分别保存原文 SHA256、拆分前原卡文本及 SHA256、当前 observable 及 SHA256。每条说明在哪里操作、需要什么输入、保留什么原件和如何判断；没有现成完整自动 runner 的部分明确按人工/调试器/真实观测执行并保留缺口。

## 工程与外部签收边界

- 这33条是外部证据操作规程；不冒充179个原JUnit@Test或41个原HOST绑定，不改222原ID/原文hash。
- 真实222原文SHA256与拆分前卡文本SHA256分列；旧textSha256原样保留，不能拿它当a/b拆分子条款或当前observable的摘要。原EMU/DEVICE/NATURAL层级不变。
- P1-P5源码工程签收只依赖各批当批已可执行的工程验证；没有设备、72h或未来自然证据时不阻塞源码施工。
- 工程签收与最终设备/自然业务签收分栏；原publication_requires_device_evidence及自然资格规则保持，不能因工程完成宣称成品业务全部通过。
- 命令均为待操作者执行的模板，R325_*变量必须来自真实冻结输入；未预置任何设备、APK路径、时钟、样本或结果。
- 既有Android方法仅作具体支持入口，各条仍须满足整条原文及真实环境；EMU不能冒名V2353A，合成fixture不能冒名自然未来。
- 首次结果不通过应记录FAIL/BLOCKED/INSUFFICIENT；修复重跑建新回执，原失败原件保留，不修改原阈值/分母/源hash过门。

## 执行前变量

`R325_EVIDENCE` 为本次全新证据目录；`R325_SOURCE_COMMIT`、`R325_APP_APK`、`R325_TEST_APK`、`R325_APK_SHA256` 来自同一冻结P5构建。`ANDROID_SERIAL` 指向明确目标；每台设备分别记录型号/API/boot。`R325_API`只填写实际API。目录先由操作者创建，不能覆盖旧失败原件。

日历的 `R325_ANCHOR_ISSUE`、`R325_ANCHOR_OPEN_MS`、`R325_REGISTERED_DEVICE_MS`、`R325_SCHEDULE_EVIDENCE` 均来自真实已发生的provider/设备观察。`R325_OPPORTUNITY_COUNT` 为1440或4320，`R325_DURATION_SECONDS`应覆盖完整登记窗口并保留末端余量，`R325_DB_EVERY`按实际时效采证需求登记。没有这些输入时保持NOT_RUN，不填合成样本冒名执行。

## 共用操作：P5_IDENTITY — 同一P5源码、构建、APK与安装身份

已有源码/工具：`app/build.gradle`、`release/release_manifest.json`、`scripts/r982_source_call_asset.py`、`tools/r31/package_release.py`、`.github/workflows/r31-verification.yml`。

**所需输入**

- 已完成P5源码工程验收并冻结的干净commit
- 原构建要求的SDK/NDK/JDK/Gradle依赖
- 实际调试APK、测试APK；覆盖既有安装另需同包名同签名且versionCode递增的真实目标APK
- 设备序列号、型号、API、安装前原APK与数据保全回执

**待执行命令**

以下命令按需分终端/分阶段执行，不是自动串行一键发布脚本。

```bash
git rev-parse HEAD > "$R325_EVIDENCE/source-commit.txt"
```

```bash
git status --porcelain > "$R325_EVIDENCE/source-status.txt"
```

```bash
python3 scripts/r982_source_call_asset.py
```

```bash
./gradlew --no-daemon -Pr30Instrumentation=true :app:assembleDebug :app:assembleDebugAndroidTest
```

```bash
sha256sum "$R325_APP_APK" "$R325_TEST_APK" > "$R325_EVIDENCE/apk-sha256.txt"
```

```bash
"$ANDROID_HOME/build-tools/36.0.0/apksigner" verify --verbose --print-certs "$R325_APP_APK" > "$R325_EVIDENCE/apk-signature.txt"
```

```bash
"$ANDROID_HOME/build-tools/36.0.0/aapt" dump badging "$R325_APP_APK" > "$R325_EVIDENCE/apk-badging.txt"
```

```bash
adb get-serialno > "$R325_EVIDENCE/device-serial.txt"
```

```bash
adb shell getprop ro.product.model > "$R325_EVIDENCE/device-model.txt"
```

```bash
adb shell getprop ro.build.version.sdk > "$R325_EVIDENCE/device-api.txt"
```

```bash
adb shell dumpsys package com.starcore.research.r13 > "$R325_EVIDENCE/installed-package.txt"
```

```bash
adb shell pm path com.starcore.research.r13 > "$R325_EVIDENCE/installed-apk-paths.txt"
```

**适用范围与不足**

- APK构建不加-Pr312Development=true；该模式在现有build.gradle中明确禁止assemble/package/sign任务。
- 调试APK用于隔离机制验证；不能把不同签名调试APK当作现场032覆盖升级包。
- 实际安装与签名原件缺失时外部身份项BLOCKED，工程源码签收不等待它。签名密钥和API凭据沿用原安全配置，不写入采证文件。
- SOURCE_COMMIT/APK_SHA字符串一致还不证明安装身份；须从pm path所指实际base.apk回取并核摘要/签名。

## 共用操作：DEVICE_WINDOW — 独立机会日历、真机采样、原件合并与设备判定

已有源码/工具：`tools/r31/collect_device_receipts.py`、`tools/r31/accept_device_receipts.py`、`construction/STARCORE_R31_FINAL_20261104/ledgers/acceptance_config.json`。

**所需输入**

- 从真实provider取得且已开出的anchor issue/open_time及原始schedule_evidence.json
- 实际设备登记时钟、同一源码commit、同一实际APK
- 事先登记的场景/负载/排除条件与原件hash
- 真实采样区间，24h为初步观察，72h为完整长期观察

**待执行命令**

以下命令按需分终端/分阶段执行，不是自动串行一键发布脚本。

```bash
python3 tools/r31/collect_device_receipts.py calendar --anchor-issue "$R325_ANCHOR_ISSUE" --anchor-open-ms "$R325_ANCHOR_OPEN_MS" --count "$R325_OPPORTUNITY_COUNT" --registered-at-ms "$R325_REGISTERED_DEVICE_MS" --source-commit "$R325_SOURCE_COMMIT" --apk "$R325_APP_APK" --schedule-evidence "$R325_SCHEDULE_EVIDENCE" --out "$R325_EVIDENCE/opportunities.json"
```

```bash
python3 tools/r31/collect_device_receipts.py collect --out "$R325_EVIDENCE/foreground.jsonl" --condition FOREGROUND --interval 60 --duration "$R325_DURATION_SECONDS" --db-every "$R325_DB_EVERY" --source-commit "$R325_SOURCE_COMMIT" --apk "$R325_APP_APK" --opportunities "$R325_EVIDENCE/opportunities.json"
```

```bash
python3 tools/r31/collect_device_receipts.py merge "$R325_EVIDENCE/foreground.jsonl" "$R325_EVIDENCE/screen_off.jsonl" "$R325_EVIDENCE/background.jsonl" "$R325_EVIDENCE/battery_restricted.jsonl" "$R325_EVIDENCE/network_recovery.jsonl" "$R325_EVIDENCE/cleanup.jsonl" "$R325_EVIDENCE/force_stop.jsonl" --source-commit "$R325_SOURCE_COMMIT" --apk-sha "$R325_APK_SHA256" --out "$R325_EVIDENCE/merged.json"
```

```bash
python3 tools/r31/accept_device_receipts.py "$R325_EVIDENCE/merged.json" --phase 72h --out "$R325_EVIDENCE/verdict.json"
```

**适用范围与不足**

- 24h/72h日历count分别1440/4320；采样需覆盖日历首尾并留末端完成时间。
- 各场景单独运行collect并真实改变设备状态；只改condition标签不构成场景证据。只有实际创建的段才传给merge，缺段保留缺口。
- 默认每10次做全库快照不保证普通shadow的逐分钟及时性；只有实际存在的完整typed主链账本或同原件截止前观察可以证明时效。无证据保留UNKNOWN/BLOCKED，不为覆盖率开启REAL。
- DB/WAL复制前/副本/复制后摘要必须相等。持续写入无法取得稳定副本时记录采集失败，不把快照标完整。
- 24h报告的内存/两日增量可合理BLOCKED：原门要求2h预热后两个24h窗口，以及末两天增长。该状态不阻塞P1-P5工程。

## 共用操作：DEVICE_ORIGINALS — 日志、运行态、完整窗口原件与可复算分母

已有源码/工具：`tools/r31/collect_device_receipts.py`、`app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportSnapshotR30.java`、`app/src/main/java/com/ethk3/novaonepro/strategyv2/IssueTimelineWriterR30.java`。

**所需输入**

- 已绑定身份的真机
- 准确测量起止时间、bootId、工作负载
- 应用实际报告导出

**待执行命令**

以下命令按需分终端/分阶段执行，不是自动串行一键发布脚本。

```bash
adb logcat -v epoch > "$R325_EVIDENCE/logcat-live.txt"
```

```bash
adb shell dumpsys activity exit-info com.starcore.research.r13 > "$R325_EVIDENCE/exit-info.txt"
```

```bash
adb shell dumpsys meminfo -d com.starcore.research.r13 > "$R325_EVIDENCE/meminfo.txt"
```

```bash
adb shell dumpsys activity services com.starcore.research.r13 > "$R325_EVIDENCE/services.txt"
```

```bash
adb shell dumpsys jobscheduler > "$R325_EVIDENCE/jobscheduler.txt"
```

```bash
adb shell pidof com.starcore.research.r13 > "$R325_EVIDENCE/pid.txt"
```

**适用范围与不足**

- logcat-live命令为独立终端持续记录，结束时保留停止时间；不先清掉异常日志。
- 报告的展示页可能有行数上限（例如r31FetchTiming默认24行），不能用于1000次/72h完整分母。
- 需要完整原表时，在测量结束后单独记录停机边界，停止并确认所有同包进程退出，再从同一静止代保全DB、WAL、SHM及原始hash/缺失事实；保留只读原件，在工作副本上用Python sqlite3读取全窗。这个停机段不计为健康实时连续运行。
- 原表不存在、字段缺失、收集权限不足或查询截断时列BLOCKED；不得重命名成功行形成“完整”数据。
- 异常、失败、取消、超时、缺期、未知数值都保留；P95/P99注明分母、单位与boot范围，不对已经聚合的P95再平均。

## 共用操作：ANDROID_PROTOCOLS — 现有真实Android机制协议，范围为一次性模拟器

已有源码/工具：`tools/r31/run_android_joint_protocols.py`、`tools/r31/emulator_matrix.sh`、`tools/r30/run_fault_matrix.sh`、`tools/r30-repair/run_research_fault_matrix.sh`、`.github/workflows/r31-verification.yml`。

**所需输入**

- 当前P5真实构建的应用APK和androidTest APK
- 一次性API29/35模拟器；原工作流另含API34
- 明确ANDROID_SERIAL、全新out目录和sandbox-prefix

**待执行命令**

以下命令按需分终端/分阶段执行，不是自动串行一键发布脚本。

```bash
adb install -r -g "$R325_APP_APK"
```

```bash
adb install -r -g "$R325_TEST_APK"
```

```bash
python3 tools/r31/run_android_joint_protocols.py --out "$R325_EVIDENCE/joint" --sandbox-prefix r325-protocol
```

```bash
bash tools/r31/emulator_matrix.sh "$R325_API" "$R325_APP_APK" "$R325_TEST_APK" "$R325_EVIDENCE/emulator-matrix"
```

**适用范围与不足**

- 现有joint脚本检查ro.kernel.qemu=1；不要移除该限制来把EMU回执冒名为真机。
- fault matrix与联合小协议分别执行；一个组通过不代替全部F01-F10或原故障矩阵。
- 现场自然数据与合成instrumentation资料分开；框架/fixture会执行受控写入，使用隔离测试资料。
- API29与API35各自产出完整回执，不能把一次API35通过复制为API29。

## 共用操作：UI_AND_HEAP — 真实界面图证、生命周期与等负载内存证据

已有源码/工具：`app/src/androidTest/java/com/ethk3/novaonepro/R31ChartLifecycleInstrumentedTest.java`、`app/src/androidTest/java/com/ethk3/novaonepro/R30ChartsAndroidTest.java`、`app/src/androidTest/java/com/ethk3/novaonepro/R30RepairChartsAndroidTest.java`、`app/src/main/java/com/ethk3/novaonepro/ChartRegistryR31.java`。

**所需输入**

- 固定设备、方向、场景、工作负载与屏幕刷新条件
- 切页/停止/恢复前后的实际截图和heap原件
- 原ChartRegistry 47项ID表与同次导出的frame数据

**待执行命令**

以下命令按需分终端/分阶段执行，不是自动串行一键发布脚本。

```bash
adb exec-out screencap -p > "$R325_EVIDENCE/screen.png"
```

```bash
adb shell dumpsys gfxinfo com.starcore.research.r13 framestats > "$R325_EVIDENCE/gfxinfo-framestats.txt"
```

```bash
adb shell am dumpheap com.starcore.research.r13 /data/local/tmp/r325-observed.hprof
```

```bash
adb pull /data/local/tmp/r325-observed.hprof "$R325_EVIDENCE/observed.hprof"
```

```bash
adb shell am instrument -w -r -e r30Sandbox r325-lifecycle -e class com.ethk3.novaonepro.R31ChartLifecycleInstrumentedTest#hundredRealTabBackgroundRotationCyclesCloseTheirActualOwners com.starcore.research.r13.test/com.ethk3.novaonepro.R30FaultAndroidRunner
```

**适用范围与不足**

- heap读取权限不具备时可用同一设备允许的Android Studio profiler取得原件；没有真实dump/保留路径不能签dominator通过。
- live heap取已观察到major GC后的live set，或隔离受控heap检查；不得在每期强制GC掩盖增长。
- 图表fixture测试产生合成/缺失状态截图，证明渲染机制，不证明当前自然策略质量；自然截图需另外使用原业务数据。
- 截图像素hash、业务frame hash、完整snapshot hash是不同标识，不能互相替代。
- 物理设备生命周期条款必须记录该设备真实执行；EMU日志保留EMU标签。

## 共用操作：UPGRADE_PROTOCOL — 覆盖安装、崩溃切点与旧资产读取

已有源码/工具：`tools/r31/run_emulator_upgrade_matrix.py`、`tools/r31/upgrade_matrix.py`、`app/src/androidTest/java/com/ethk3/novaonepro/R31PlatformReadinessInstrumentedTest.java`、`app/src/main/java/com/ethk3/novaonepro/SchemaDomainCoordinatorR926.java`。

**所需输入**

- 真实032旧APK/旧数据及独立保全hash
- 同身份且更高versionCode的候选APK
- 隔离恢复副本、已明确的迁移持久切点表与恢复期望

**待执行命令**

以下命令按需分终端/分阶段执行，不是自动串行一键发布脚本。

```bash
adb install -r "$R325_APP_APK"
```

```bash
adb shell am start -W -n com.starcore.research.r13/com.ethk3.novaonepro.MainActivity
```

```bash
python3 tools/r31/run_emulator_upgrade_matrix.py --apk "$R325_APP_APK" --out "$R325_EVIDENCE/upgrade-emu" --api "$R325_API"
```

**适用范围与不足**

- 现有emulator升级脚本只准rooted disposable emulator，按0.3/0.8/1.5/3/6秒启动延时杀进程；这不是每个持久切点都命中的证明。
- 该脚本主要是同一APK再次覆盖自身数据；要证明032→033真实升级，应先安装真实032并恢复已验证旧资料，再覆盖候选。
- 逐切点覆盖需记录真实断点位置/数据库journal前态、确认旧PID消失，再恢复；没有命中证据的切点保持NOT_RUN。没有现成自动切点runner就如实保留人工/调试器步骤，不补造自动PASS。
- 旧APK/约600MB真实现场库缺失时相关真实升级项BLOCKED，模拟库/host切点结果不能补成现场通过。

## 共用操作：NATURAL_QUALIFICATION — 独立自然未来质量与频率合同原证据

已有源码/工具：`app/src/main/java/com/ethk3/novaonepro/strategyv2/EvidenceContractR31.java`、`app/src/main/java/com/ethk3/novaonepro/strategyv2/EvidenceContractStoreR31.java`、`app/src/main/java/com/ethk3/novaonepro/strategyv2/CertificateGateR27.java`、`app/src/main/java/com/ethk3/novaonepro/strategyv2/GateV2R29.java`、`app/src/main/java/com/ethk3/novaonepro/strategyv2/QualificationInboxR29.java`、`construction/STARCORE_R31_FINAL_20261104/ledgers/acceptance_config.json`。

**所需输入**

- 观察前已冻结的策略/模型/规则ABI、实验身份、预注册质量与频率合同
- 原合同的minSupport、置信/错误预算、停止规则、校准、风险、适用域与alpha消耗账本
- 独立未来真实机会、冻结、结算、弃权、缺失与源revision原件
**适用范围与不足**

- 没有“执行一次命令就创造高胜率”的自动runner。使用产品原注册/冻结/资格通道，让未来数据自然到达，再导出原账本复核。
- 保持R27_GATE_V1及已正式启用的原版本协议；新门只有原注册、测试、验证、显式版本切换都完成才可启用。
- quality_target_frequency=0.15是原目标，不强凑出手；质量门和频率合同必须同时通过，不能只满足一个。
- 禁止用SELECT/CONFIRM重复使用、历史重放、改名/重启、同源子策略或Q08合成实验冒充独立自然未来样本。
- 无自然原件/不足原合同样本时NOT_PROVEN或INSUFFICIENT；不降低阈值、不重置alpha、不伪造授证。此状态不阻塞工程源码批次。

## 逐条规程

### 01. R31-B01-C03#A3b

**原文：** 真机连续24小时无永久无声停摆。

真实222原文 SHA256：`fdde3fcd45664cd97dd45bc55e98a7197a46d54b22be2e5e0cd697beaec93382`。归属：P5。原级别：`DEVICE`。状态：**NOT_RUN**。工程批次前置：**否**。证据层：`PHYSICAL_DEVICE`。

拆分前原条款：`R31-B01-C03#A3`；原卡文本 SHA256（也是保留的旧 textSha256）：`2781acbb24d800d25e648ee21d649c95df0e80f7ce1f67422973975019d394fc`。

使用共用操作：`P5_IDENTITY`、`DEVICE_WINDOW`、`DEVICE_ORIGINALS`。

**具体执行步骤**

1. 固定支持的前台/后台负载和独立机会日历，连续观察不少于24小时；每分钟采任务状态、cursor、last_progress和真实提交回执。
2. 对出现ready积压的任务逐期关联原owner/fence/提交游标；状态RUNNING、活着的线程或心跳不能代替实际推进。
3. 逐个记录等待/停止/缺依赖及解除时刻，解除后观察同一任务恢复推进；保留任何整段无进展。

**必须保留的原件**

- 全部JSONL与calendar/merged/verdict
- r26_task、r31_task_observation、r31_runtime_fault、r31_commit_receipt全窗原件
- 日志、退出历史、起止任务水位

**判断条件**

- 24h完整观测、支持场景内没有永久无理由停摆；有积压而无提交的区间逐项判定，不用成功任务掩盖失败任务。
- 没满24h或缺采样NOT_RUN/BLOCKED；任务等待理由存在但条件已解除后仍不推进为FAIL。

**对保全工作副本的只读查询**

参数使用真实登记窗；仅对工作副本运行，原件保持hash不变。

```sql
SELECT * FROM r26_task ORDER BY task_id;
SELECT * FROM r31_task_observation;
SELECT * FROM r31_runtime_fault;
```

### 02. R31-B01-C04#A1b

**原文：** UI上停止按钮可用且显示“停止未完成”。

真实222原文 SHA256：`c365ea49184a87a20ec1e18fe52dfc632a77e9d3e96c1ba541ae9dd16693c1c2`。归属：P5。原级别：`EMU`。状态：**NOT_RUN**。工程批次前置：**否**。证据层：`ANDROID_EMULATOR_OR_PHYSICAL_DECLARED_SEPARATELY`。

拆分前原条款：`R31-B01-C04#A1`；原卡文本 SHA256（也是保留的旧 textSha256）：`08eb6d2bafd0be58506a9fcb73f143268ac4ec62060971e78943ed2f0cf60d21`。

使用共用操作：`P5_IDENTITY`、`UI_AND_HEAP`、`DEVICE_ORIGINALS`。

**具体执行步骤**

1. 在真实运行/训练/报告仍持有资源时从应用原控制界面点击停止，记录点击前后录像与精确时刻。
2. 在旧owner尚未退出的窗口检查停止按钮仍响应、页面能继续读取，并显示原要求的“停止未完成”或明确未结束状态；不把cancel已请求显示成已停止。
3. 等旧owner真正退出后核对线程/队列资源释放及最终状态；再次启动不得产生新旧owner重叠。

**必须保留的原件**

- 点击录像/截图、UI层次与时间
- 停止请求/旧owner退出/最终状态日志
- 前后task/owner/fence/队列原件

**判断条件**

- 按钮不可用、状态假完成、旧owner未退出即重启为FAIL。
- 若当前产品根本没有所需按钮或“停止未完成”可观测状态，记录实际缺陷，不用文案说明或host cancel测试签收。

**明确缺口**

- 本包未提供自动验证该中文状态与实际owner退出关系的完整Android runner；原条款级别EMU，本条可在一次性Android模拟器按明确人工操作采证。

### 03. R31-B01-C04#A3b

**原文：** 设备长时间运行下线程/队列/prepared上限。

真实222原文 SHA256：`78463b674a0fa5da6f0fa6585307b55491ab7ee786e0f3a80341f1048e781e23`。归属：P5。原级别：`DEVICE`。状态：**NOT_RUN**。工程批次前置：**否**。证据层：`PHYSICAL_DEVICE`。

拆分前原条款：`R31-B01-C04#A3`；原卡文本 SHA256（也是保留的旧 textSha256）：`36f13b01e7067e3e86ea1b50ceb884de7a86d76a539f5248b71046d3f48416f0`。

使用共用操作：`P5_IDENTITY`、`DEVICE_WINDOW`、`DEVICE_ORIGINALS`。

**具体执行步骤**

1. 先冻结设备worker数/队列容量/prepared字节预算，记录原配置hash。
2. 在24/72h持续任务、取消、重试、停止恢复与并行导出负载下逐段取线程数、队列深度、prepared count/bytes和owner占用。
3. 对峰值时刻抓取真实线程/heap原件并关联当时工作项；每次停止后等待旧owner退出再测基线回落。

**必须保留的原件**

- 队列/线程/prepared逐时序样本与峰值原件
- 动态maxMemory、worker数、容量配置
- 关闭前后owner清单

**判断条件**

- 队列不超过实际声明容量；初始配置按worker×2、写入初始64ops及min(262144,maxMemory/512)bind字节核对，例外必须有原预算记录。
- 不能只给平均值；超过上界、无owner长期保留或缺峰值证据分别FAIL/BLOCKED。

### 04. R31-B02-C01#A2b

**原文：** 真机实时60分钟竞争测试。

真实222原文 SHA256：`8f0ef6dda0cc7ec16288e1c454d86c88429f425ad1ee82779bec0362f1af2214`。归属：P5。原级别：`DEVICE`。状态：**NOT_RUN**。工程批次前置：**否**。证据层：`PHYSICAL_DEVICE`。

拆分前原条款：`R31-B02-C01#A2`；原卡文本 SHA256（也是保留的旧 textSha256）：`f6846fa77513808981426cb6532a1d9af19048b119d64365d3c44ea662e65c59`。

使用共用操作：`P5_IDENTITY`、`DEVICE_WINDOW`、`DEVICE_ORIGINALS`。

**具体执行步骤**

1. 在真实设备持续60分钟同时开启原在线抓取/冻结/结算与正常研究、报告读任务，固定负载。
2. 保留所有写入等待、hold、busy/retry、失败/取消及关键业务提交时钟；标出竞争峰值。
3. 比较竞争前、中、后期末游标与幂等回执，核对单写owner不被绕开。

**必须保留的原件**

- 60分钟全窗writer histogram/事件
- 关键冻结与结算原件
- task owner/fence/队列和设备CPU内存曲线

**判断条件**

- 60分钟期间无重复/跨owner提交、无关键业务永久饥饿。
- 按原writer_hold_p99_target_ms=50及>200ms必须有fault receipt判定；无法从原件复算分位数则BLOCKED。

**已核对存在的Android支持入口（均未执行，不是新增原JUnit绑定）**

- `com.ethk3.novaonepro.R31OwnerFenceProtocolInstrumentedTest#independentSqliteWriterContentionReleasesBeforeSameInputRetry`

### 05. R31-B02-C02#A3a

**原文：** Android实际SQLite：受保护触发器、WAL、checkpoint并发。

真实222原文 SHA256：`211c911148c0aa641fac29722e7a53c04e47cdf62ba47805d7a6e194ca2ca211`。归属：P5。原级别：`EMU`。状态：**NOT_RUN**。工程批次前置：**否**。证据层：`ANDROID_EMULATOR_OR_PHYSICAL_DECLARED_SEPARATELY`。

拆分前原条款：`R31-B02-C02#A3`；原卡文本 SHA256（也是保留的旧 textSha256）：`2286ab50ee4ad915a9fc005012dbe1499c33dc60409962812f267dd6ba01d2cb`。

使用共用操作：`P5_IDENTITY`、`ANDROID_PROTOCOLS`、`DEVICE_ORIGINALS`。

**具体执行步骤**

1. 在API29/35隔离Android环境运行现有runtime/storage/owner协议及storage-diagnostic-process组。
2. 读取该Android实际sqlite_master与schema fingerprint，确认受保护触发器存在且违规写被原owner拒绝；记录平台SQLite版本。
3. 在长读者、并发主写入、原诊断进程PASSIVE checkpoint并存时核对WAL恢复、原主库完整性和业务水位；缺并发实测的部分仍留NOT_RUN。

**必须保留的原件**

- JOINT-ANDROID-RECEIPT与组日志
- 真实SQLite版本/触发器SQL/迁移journal
- WAL大小、checkpoint返回、读者/写者时序、原库检查

**判断条件**

- 实际Android协议通过且原触发器/WAL/并发不变量成立；host sqlite不能替代。
- 现有测试未覆盖的具体并发交错不自动签收；主进程非法第二SQLite引擎能力必须拒绝。

**已核对存在的Android支持入口（均未执行，不是新增原JUnit绑定）**

- `com.ethk3.novaonepro.R31StorageCheckpointAndroidTest#passiveUsesRealBoundDiagnosticProcessAndPreservesPlatformDatabase`
- `com.ethk3.novaonepro.R31StorageCheckpointAndroidTest#applicationCannotUseDiagnosticSqliteCapability`
- `com.ethk3.novaonepro.R31RuntimeProtocolInstrumentedTest#migrationIsAdditiveAndIdempotent`

### 06. R31-B02-C02#A3b

**原文：** 设备事务时延（host sqlite结果不能代替）。

真实222原文 SHA256：`503a0c324c6d04973f7a145f56cddd0ff425481455f804f6e4aca3191b031463`。归属：P5。原级别：`DEVICE`。状态：**NOT_RUN**。工程批次前置：**否**。证据层：`PHYSICAL_DEVICE`。

拆分前原条款：`R31-B02-C02#A3`；原卡文本 SHA256（也是保留的旧 textSha256）：`2286ab50ee4ad915a9fc005012dbe1499c33dc60409962812f267dd6ba01d2cb`。

使用共用操作：`P5_IDENTITY`、`DEVICE_WINDOW`、`DEVICE_ORIGINALS`。

**具体执行步骤**

1. 在实际目标设备按前台、研究并发、导出并发分别录制事务等待开始/获得锁/事务结束时刻。
2. 以原operation和critical分类统计完整count、失败数、P95/P99/max与最慢原件；不把不同业务的分位数混合或平均。
3. 把超200ms的每次写入与原故障回执匹配，复核单写writer和关键截止裕量。

**必须保留的原件**

- r30_writer_hold全窗histogram_json及原事件
- 关键任务deadline/commit时序
- 同设备状态与负载原件

**判断条件**

- 按原50ms writer hold P99目标与>200ms故障回执规则逐项判断。
- 只有宿主/模拟器数字时物理设备本条仍NOT_RUN。

### 07. R31-B03-C01#A2b

**原文：** 真机真实网络1000次测量分布。

真实222原文 SHA256：`7fdf1e63b9dfe4194dc80a5cb798f89b0ca81bae7d2cafdf60ea2692c6dfed41`。归属：P5。原级别：`DEVICE`。状态：**NOT_RUN**。工程批次前置：**否**。证据层：`PHYSICAL_DEVICE`。

拆分前原条款：`R31-B03-C01#A2`；原卡文本 SHA256（也是保留的旧 textSha256）：`0230fc5285e6d8b2d92f1dff13cd8f758809967b2335c0e1dbbc2714a7aed921`。

使用共用操作：`P5_IDENTITY`、`DEVICE_ORIGINALS`。

**具体执行步骤**

1. 用原真实provider和原轮询/超时参数采不少于1000次请求，成功/错误/超时/解析失败都进入尝试分母。
2. 保存r31_fetch_timing完整窗口，包含request/headers/first_byte/body_complete/parse/validated/finished与boot标识；展示页最新24行不够。
3. 从原行复算每段与端到端P50/P95/P99/max、成功/失败分母和最差20个request_id。

**必须保留的原件**

- 至少1000次原请求时序及响应摘要/错误
- 真实网络条件/设备型号/起止boot
- 全量统计表与最差20原件索引

**判断条件**

- 1000真实尝试齐备、所有失败保留、同boot单调时间正确。
- validated_response_to_draw_commit P95≤1000ms/P99≤3000ms按原定义起点body_complete复算；网络与解析阶段另列，不能改起点缩短。

**对保全工作副本的只读查询**

参数使用真实登记窗；仅对工作副本运行，原件保持hash不变。

```sql
SELECT * FROM r31_fetch_timing WHERE request_wall_ms>=:start_ms AND request_wall_ms<:end_ms ORDER BY request_wall_ms,request_id;
```

**明确缺口**

- 采样器并不自动导出1000条fetch时序，必须取完整报告原表/保全SQLite工作副本。

### 08. R31-B03-C02#A2b

**原文：** 设备/网络/正常资源条件下可预测覆盖≥99%。

真实222原文 SHA256：`694fa398e0149b47b69a25c12ec3ceb10991d719a89b90a87035925e0be9a50a`。归属：P5。原级别：`DEVICE`。状态：**NOT_RUN**。工程批次前置：**否**。证据层：`PHYSICAL_DEVICE`。

拆分前原条款：`R31-B03-C02#A2`；原卡文本 SHA256（也是保留的旧 textSha256）：`9d6818216356240342244badd612f2eebc6ae0409a5b7318cab5cff9d5c15d5a`。

使用共用操作：`P5_IDENTITY`、`DEVICE_WINDOW`、`DEVICE_ORIGINALS`。

**具体执行步骤**

1. 事先登记健康设备/网络/资源条件及完整目标机会；采样不能以冻结成功行反推分母。
2. 对每个机会关联同策略/同target的冻结原件，使用完整typed post-COMMIT证据或同原件截止前稳定观察。
3. 全量保留缺冻结、迟到、未知、排除及原证据，复算覆盖下界/上界。

**必须保留的原件**

- opportunities、JSONL、merged、FREEZE gate明细
- 逐策略target冻结及原时序账本
- 事先排除条件与证据hash

**判断条件**

- 覆盖≥0.99且原5000ms predraw裕量成立；scope/unknown未闭合不可PASS。
- 普通shadow无及时原件时BLOCKED，不强开REAL、不把后补早时间戳当及时。

### 09. R31-B04-C02#A1

**原文：** V2353A实际设备72小时：亮屏/熄屏/省电/断网/重连/滑掉任务/普通进程kill/force-stop/重启分别测试，并留adb/退出历史/逐期水位。

真实222原文 SHA256：`90d6789a5e9afd218d47fe694e0f7e8227c959c9e29547f1e0278792bd010217`。归属：P5。原级别：`DEVICE`。状态：**NOT_RUN**。工程批次前置：**否**。证据层：`PHYSICAL_DEVICE`。

拆分前原条款：`R31-B04-C02#A1`；原卡文本 SHA256（也是保留的旧 textSha256）：`90d6789a5e9afd218d47fe694e0f7e8227c959c9e29547f1e0278792bd010217`。

使用共用操作：`P5_IDENTITY`、`DEVICE_WINDOW`、`DEVICE_ORIGINALS`。

**具体执行步骤**

1. 先核设备确为V2353A，并登记72h场景时间表；亮屏、熄屏、省电、断网、重连、滑掉任务、普通进程kill、force-stop、重启各有单独段。
2. 每次切换前后记录adb命令或实机操作录像、实际PID/boot、退出历史与逐期业务/学习提交水位。
3. 用户force-stop按停止且重新打开恢复判定；普通kill/重启则按真实系统限制和原恢复设计判断，不混淆原因。

**必须保留的原件**

- V2353A型号与完整72h采样
- 九类场景逐段操作/录像/adb/exit-info
- 逐期机会、水位、停止理由、恢复后的首个真实提交

**判断条件**

- 九类场景都有实际原件；支持条件下不永久无声丢任务，受限状态准确披露。
- 任何场景未执行均NOT_RUN；不是V2353A不能冒称本型号完成；force-stop预期停止不算后台覆盖成功。

### 10. R31-B04-C02#A2b

**原文：** 真机后台切换与stop行为。

真实222原文 SHA256：`164b1cba0706e306402785ae1019c1b08fdccd3f37e231cb90e8c3d1c5c0498f`。归属：P5。原级别：`DEVICE`。状态：**NOT_RUN**。工程批次前置：**否**。证据层：`PHYSICAL_DEVICE`。

拆分前原条款：`R31-B04-C02#A2`；原卡文本 SHA256（也是保留的旧 textSha256）：`403e4d93d5626624f31fb23fc9751b18a4e501a468b03f7c22990b9ad4703ec0`。

使用共用操作：`P5_IDENTITY`、`UI_AND_HEAP`、`DEVICE_ORIGINALS`。

**具体执行步骤**

1. 真机运行中切前台/后台/熄屏/再前台，在每个状态点击原停止控制并观察实际worker结束。
2. 检查通知、界面、服务、任务owner对停止原因的呈现是否一致；重开仅在旧owner退出或原隔离规则许可后推进。
3. 保留有进行中I/O/写入时的停止窗口，不只测空闲秒停。

**必须保留的原件**

- 界面/通知录像
- stop请求/取消/owner退出时序
- 服务/任务状态与恢复提交

**判断条件**

- 实际前后台和停止行为符合原状态机；假完成、重复owner、停止后静默继续修改为FAIL。
- 等待时显示真实理由；未观察到进行中停止场景则该子项NOT_RUN。

### 11. R31-B04-C02#A3

**原文：** force-stop测试预期为停止且再打开恢复；不得把这个预期当缺陷跳过或虚报100%后台覆盖。

真实222原文 SHA256：`716392be74ff27b5438727c19c2423d75869dd7fa636a74420102218c6ea54af`。归属：P5。原级别：`DEVICE`。状态：**NOT_RUN**。工程批次前置：**否**。证据层：`PHYSICAL_DEVICE`。

拆分前原条款：`R31-B04-C02#A3`；原卡文本 SHA256（也是保留的旧 textSha256）：`716392be74ff27b5438727c19c2423d75869dd7fa636a74420102218c6ea54af`。

使用共用操作：`P5_IDENTITY`、`DEVICE_WINDOW`、`DEVICE_ORIGINALS`。

**具体执行步骤**

1. 记录force-stop前PID/任务水位，在事先登记段执行adb shell am force-stop com.starcore.research.r13。
2. 确认同包所有进程停止并保持，不能等待系统自行复活来当通过。
3. 由用户明确重新打开应用，按原运行开关恢复；核对未完成任务重入与真实提交连续性、无重复结算。

**必须保留的原件**

- force-stop命令/时间/PID退出证据
- 停止期间无业务提交原件
- 用户重新打开录像与恢复水位

**判断条件**

- 停止期间保持停止；用户重开后按原设置恢复或显示真实必要条件。
- 自动重启造成违背用户停止、或把停止期计入100%后台成功为FAIL。

### 12. R31-B05-C02#A2b

**原文：** 真机24h产物数量/bytes上界。

真实222原文 SHA256：`bfb2b0d5dbbba737c90a799fd46c7af942908de3611c9aaac21b1e3f1c65e525`。归属：P5。原级别：`DEVICE`。状态：**NOT_RUN**。工程批次前置：**否**。证据层：`PHYSICAL_DEVICE`。

拆分前原条款：`R31-B05-C02#A2`；原卡文本 SHA256（也是保留的旧 textSha256）：`336a3d3ccd98cda18a88b7b076aaf05d4db9ee3cc4d390299f118c01e09dff6a`。

使用共用操作：`P5_IDENTITY`、`DEVICE_WINDOW`、`DEVICE_ORIGINALS`。

**具体执行步骤**

1. 固定24h正常研究与报告负载，记录全部物理目录与原产物对象数量/字节。
2. 覆盖运行、导出峰值、取消/失败回收、稳定态四阶段，持续记录pending/claimed/pinned/protected债务。
3. 每个增长来源关联owner及原manifest，不把同一物理文件在多个目录逻辑角色重复加总，也不扣除protected字节。

**必须保留的原件**

- 每分钟目录物理字节、文件/对象全量索引
- 产物owner/状态/生命周期日志
- 峰值与回落、受保护债务

**判断条件**

- 原soft1536MiB/hard2048MiB正常预算、transient3072/4096MiB、空闲256MiB分别按情景报告。
- 保护债务超限为BLOCKED_PROTECTED_DEBT，不删除原件换PASS；24h仅核24h范围，末两日增长另验。

### 13. R31-B05-C11#A3b

**原文：** 设备heap dominator无Activity/旧session滞留。

真实222原文 SHA256：`9897cf761bf32e9f9fedf7e59061db204c6f8ddcad68a5d76c9fe6fd057f0773`。归属：P5。原级别：`DEVICE`。状态：**NOT_RUN**。工程批次前置：**否**。证据层：`PHYSICAL_DEVICE`。

拆分前原条款：`R31-B05-C11#A3`；原卡文本 SHA256（也是保留的旧 textSha256）：`def6e7da15ab8753196d698cf759b611acfb40b7e2114457e9d43ce39610531e`。

使用共用操作：`P5_IDENTITY`、`UI_AND_HEAP`。

**具体执行步骤**

1. 在固定负载下取基线heap；执行100次切页/后台/旋转/停止恢复后等待真实owner终止。
2. 在观察到major GC后或受控隔离检查取得新heap，使用heap分析器看Activity/旧session/Executor/Window的dominator和到GC root路径。
3. 把仍保留对象逐个归到正在使用的owner或已废弃owner，不仅比较总MB。

**必须保留的原件**

- 前后hprof原件/hash/采样时点
- dominator/retained-size及GC root路径导出
- 已销毁Activity与旧session ID、线程结束日志

**判断条件**

- 不存在已销毁Activity/旧session的非预期长保留链；活跃对象不能误判泄漏。
- 只有PSS回落或调用过System.gc不能签本条；拿不到heap原件则BLOCKED。

### 14. R31-B05-C11#A5b

**原文：** native/FD实测回到基线包络。

真实222原文 SHA256：`73346b73ec7e2d6eadff92cb14a62a67566c374ab25d0305c03313521529f7d7`。归属：P5。原级别：`DEVICE`。状态：**NOT_RUN**。工程批次前置：**否**。证据层：`PHYSICAL_DEVICE`。

拆分前原条款：`R31-B05-C11#A5`；原卡文本 SHA256（也是保留的旧 textSha256）：`606cf26f53c0f7fa2c8d10864a9fef88a1fe5b58bca131afd9b66c7a7aa05581`。

使用共用操作：`P5_IDENTITY`、`DEVICE_WINDOW`、`DEVICE_ORIGINALS`、`UI_AND_HEAP`。

**具体执行步骤**

1. 在冻结的相同负载基线记录native/PSS、FD数与FD目标分类。
2. 重复真实训练/查询/导出/取消/恢复，在运行峰值和owner关闭后分别采native与/proc实际FD原件。
3. 对未回落的FD/native分配逐owner追溯，区分当前合法保留与已退出任务残留。

**必须保留的原件**

- meminfo/native profiler原件
- 同PID/同boot FD清单及数量时序
- owner释放与等负载基线包络

**判断条件**

- 关闭后实测回到事先固定的基线包络，native预算按实际owner reservation而不是Java maxMemory猜测。
- 未采FD或native、事后放宽包络均不能通过。

**明确缺口**

- 现有collect只读PSS/Java摘要；FD与native归属需额外Android profiler或允许的/proc只读观测，不能伪称脚本已自动覆盖。

### 15. R31-B05-C11#A6

**原文：** ST-M06 72h OOM=0；对等负载live heap/PSS无持续增长，末24h等负载P95 live heap≤稳定首24h110%且不越动态高水位；未实测NOT_RUN

真实222原文 SHA256：`799cd9a56f6d367a5d6a028761156a0c8ab0b54490a0ea2ca3c0f1a8236a5676`。归属：P5。原级别：`DEVICE`。状态：**NOT_RUN**。工程批次前置：**否**。证据层：`PHYSICAL_DEVICE`。

拆分前原条款：`R31-B05-C11#A6`；原卡文本 SHA256（也是保留的旧 textSha256）：`799cd9a56f6d367a5d6a028761156a0c8ab0b54490a0ea2ca3c0f1a8236a5676`。

使用共用操作：`P5_IDENTITY`、`DEVICE_WINDOW`、`DEVICE_ORIGINALS`、`UI_AND_HEAP`。

**具体执行步骤**

1. 登记同型号/同负载/同配置校准，连续72h观察，保留每次退出/低内存/OOM/ANR。
2. 预热后选择稳定首24h与末24h等负载窗口，实际live set/PSS按样本复算P95；强制停止样本分开，不混进等负载内存分母。
3. 同时检查动态高水位与realtime headroom，不把总PSS≤110%当作live heap≤高水位。

**必须保留的原件**

- 72h完整曲线与每个样本负载/GC时点
- 两窗口live heap/PSS原始分母与P95
- OOM/ANR/crash/exit-info、动态水位原值

**判断条件**

- OOM=0；末24h等负载P95 live heap和PSS各≤首稳定24h的1.10，且不越原动态高水位、无未解释持续增长。
- collect只算PSS部分，缺live heap/GC原件则本条仍NOT_RUN/BLOCKED，不以PSS gate替代。

### 16. R31-B06-C02#A3b

**原文：** 真机CPU吞吐、峰值内存、实时延迟达到性能门。

真实222原文 SHA256：`0a63d7b01d4976e05c795514b12e6afb08108df175850e4e091a75de4c6f5f70`。归属：P5。原级别：`DEVICE`。状态：**NOT_RUN**。工程批次前置：**否**。证据层：`PHYSICAL_DEVICE`。

拆分前原条款：`R31-B06-C02#A3`；原卡文本 SHA256（也是保留的旧 textSha256）：`3d622d1df0c703b7c69947f56b4337fc7181d96140c3e7d04b18d85e7a9e7801`。

使用共用操作：`P5_IDENTITY`、`DEVICE_ORIGINALS`、`UI_AND_HEAP`。

**本条额外输入**

- 原PerformanceGate已批准的吞吐下界/内存上界/实时P95上界

**具体执行步骤**

1. 先冻结目标设备上的原family性能门与同一数据/模型/预算；不能把单元测试中500/96MiB/1000等fixture常量当产品批准值。
2. 实际跑相同回放/训练工作量，记录CPU处理样本/秒、peak heap/native与并行实时P95延迟；每项保留独立原时序。
3. 将设备回执ID、设备/源码/ABI/负载与三项测量一起交给原ReplayCheckpointR31.PerformanceGate决定；缺一保留原预算。

**必须保留的原件**

- 设备性能原trace/计数/CPU时间
- peak heap/native原件与实时P95分母
- 原gate配置/决定/receiptId

**判断条件**

- 三门同时达到原已批准目标才可判定；缺字段/非DEVICE/任一未达标均不得扩family预算。
- 未建立合法设备校准目标时BLOCKED，不由本规程发明新门槛。

**明确缺口**

- 已有JUnit只验证性能回执判定逻辑，未提供实际CPU吞吐物理测量runner。

### 17. R31-B06-C02#A4b

**原文：** 设备堆/native实测下降。

真实222原文 SHA256：`3c0a3f0dece6bc235ad970c958c6b1224d7396b3f6d90eaa7d7ce8d08616ec86`。归属：P5。原级别：`DEVICE`。状态：**NOT_RUN**。工程批次前置：**否**。证据层：`PHYSICAL_DEVICE`。

拆分前原条款：`R31-B06-C02#A4`；原卡文本 SHA256（也是保留的旧 textSha256）：`31c63e9a4c3d58b2343ce8c4634d0c0310f880da6b0e1b7116de3c1b2c1908d1`。

使用共用操作：`P5_IDENTITY`、`UI_AND_HEAP`、`DEVICE_ORIGINALS`。

**具体执行步骤**

1. 在同设备、同完整输入、同模型状态、同负载下分别测对照版本与候选版本，原件各自独立。
2. 对回放/训练相同阶段记录heap/native峰值和结束稳定值，并记录是否完成同样工作量。
3. 排除少跑样本、取消提前退出、改预算造成的假下降，保留每个重复样本。

**必须保留的原件**

- 对照与候选APK/source/负载身份
- 配对heap/native曲线与工作量/完成状态
- 差值及重复性统计

**判断条件**

- 设备原件证明相同工作量下实测下降，且原质量/恢复/实时门不退化。
- 无对照原件或不等工作量只能NOT_PROVEN，不能用代码删除行数推断内存下降。

### 18. R31-B07-C03#A3b

**原文：** 真机实时SLO不劣化。

真实222原文 SHA256：`b99bde1db27d31cd117a64686c24b38c60650936d6b1d9ddfb5b353e46c26d87`。归属：P5。原级别：`DEVICE`。状态：**NOT_RUN**。工程批次前置：**否**。证据层：`PHYSICAL_DEVICE`。

拆分前原条款：`R31-B07-C03#A3`；原卡文本 SHA256（也是保留的旧 textSha256）：`2e26d2f051cc536d7045d37357865e3673ff7391a49e649b50cc15fbeb71f37a`。

使用共用操作：`P5_IDENTITY`、`DEVICE_WINDOW`、`DEVICE_ORIGINALS`。

**具体执行步骤**

1. 冻结同一正常运行场景，对照与候选开启相同创造/迭代负载，逐期记录抓取、结算、下一期冻结。
2. 全窗复算base commit→next freeze、validated response→draw commit、draw commit→settlement的P95/P99和缺期。
3. 将新增研究工作每次占用/让步/取消与关键链时点关联，检查峰值期间的SLO。

**必须保留的原件**

- 配对设备/负载/版本身份
- 真实业务段计时、机会分母、失败与迟到
- 研究任务资源/让步事件

**判断条件**

- 不劣于原已冻结SLO；按原1000ms/3000ms抓取结算门、5000ms下一冻结P95与5000ms裕量分别判断。
- 不能用总吞吐提升抵消关键链劣化，也不能删掉拥塞窗口。

### 19. R31-B07-C06#A5

**原文：** 新功能只有独立自然未来质量与频率合同共同通过后才能宣称品质达标，工程可运行与自然有效分栏。

真实222原文 SHA256：`32c437f16cf916bf8858913b5efa274375fd2d08ef72b62caa10cf9c160d89a4`。归属：P5。原级别：`NATURAL`。状态：**NOT_PROVEN**。工程批次前置：**否**。证据层：`NATURAL_FUTURE`。

拆分前原条款：`R31-B07-C06#A5`；原卡文本 SHA256（也是保留的旧 textSha256）：`32c437f16cf916bf8858913b5efa274375fd2d08ef72b62caa10cf9c160d89a4`。

使用共用操作：`P5_IDENTITY`、`NATURAL_QUALIFICATION`、`DEVICE_ORIGINALS`。

**具体执行步骤**

1. 在真实未来观察开始前，通过原产品协议冻结新功能产生的候选、模型、规则、适用域、质量门/频率合同及所有曝光预算，保存原账本hash。
2. 让独立未来数据自然到达；记录全部应有机会、真实冻结、弃权、缺失、出手、结算，维持原PIT/SELECT/CONFIRM/自然角色隔离。
3. 沿原资格owner复算最小支持、错误预算/置信边界、停止/校准/风险、频率区间和alpha账本；质量与频率分别列状态。
4. 只有同一原合同与适用域下两个门都通过，才声明该资产自然品质达标；进入真实出手仍由原证书和主链authority决定。

**必须保留的原件**

- 预注册合同/候选/模型/规则/ABI原件及冻结时刻
- 自然机会/冻结/结算/弃权/缺失全量账本
- 原证书/序贯证据/频率报告/撤销与alpha消费记录

**判断条件**

- 工程可运行单独列；自然证据不足保持NOT_PROVEN/INSUFFICIENT，零新授证可以是正确结果。
- 不能把Q08合成6arms×20seeds、训练回放胜率、重复确认、改名重启或同源子策略当新独立自然证据。
- 保持原R27_GATE_V1/合法版本、0.15频率目标但不强凑出手，不降低门槛，不重置原曝光预算。

**明确缺口**

- 没有自动“等待若干小时必能授证”的runner或获胜保证。必须由原运行协议产生将来尚不存在的真实原件。

### 20. R31-B05-C07#A3b

**原文：** 真机24h日志上界。

真实222原文 SHA256：`34007296320046c8df35d10249d5f58cbd6a0559932658450c9ce3cbbc91bca8`。归属：P5。原级别：`DEVICE`。状态：**NOT_RUN**。工程批次前置：**否**。证据层：`PHYSICAL_DEVICE`。

拆分前原条款：`R31-B05-C07#A3`；原卡文本 SHA256（也是保留的旧 textSha256）：`fa0dcf3f7b7260b55489c9c69b4327f58ac3e880e964ff8cf33b2d9610db1f22`。

使用共用操作：`P5_IDENTITY`、`DEVICE_WINDOW`、`DEVICE_ORIGINALS`。

**具体执行步骤**

1. 固定24h负载，记录日志类文件/数据库日志行/归档/临时文件的数量及物理字节。
2. 覆盖稳定、连续错误、取消/重试和导出四种真实日志压力；按原retention规则观察滚动/归档与protected保留。
3. 检查到达上界后继续产生事件时可持续推进且旧业务原件不被当缓存删除。

**必须保留的原件**

- 24h日志物理数量/bytes时序及原目录清单
- 日志/归档owner与保留规则hash
- 压力/取消恢复原日志

**判断条件**

- 按实际配置上界判断且无未解释持续增长；不能用重装/清数据/删除受保护证据制造回落。
- 缺实际24h字节原件为NOT_RUN，只有ring常量/host模拟负载不够。

### 21. R31-B05-C09#A2b

**原文：** 真机压力下实时冻结/结算延迟门。

真实222原文 SHA256：`46e7e62cb7dc45ab13acde9b45d054967bdae429a5020411bd8fb77876eb263b`。归属：P5。原级别：`DEVICE`。状态：**NOT_RUN**。工程批次前置：**否**。证据层：`PHYSICAL_DEVICE`。

拆分前原条款：`R31-B05-C09#A2`；原卡文本 SHA256（也是保留的旧 textSha256）：`51688e3c959d93ce5ba897437ace71196f30f3ba6c9d815a3d5d57633a6af330`。

使用共用操作：`P5_IDENTITY`、`DEVICE_WINDOW`、`DEVICE_ORIGINALS`。

**具体执行步骤**

1. 真机并行训练、报告、WAL长读与存储回收压力，保存开始/结束负载与临时空间。
2. 记录每期真实冻结和结算提交时点、writer等待/hold及checkpoint owner，复算实时分布。
3. 对超时/取消/空间不足保留原故障回执，检查关键链在压力解除后按原owner恢复。

**必须保留的原件**

- 压力场景时序与空间原件
- 冻结/结算/等待/hold完整时钟
- checkpoint/GC owner及异常回执

**判断条件**

- base commit→next freeze P95≤5000ms、有效冻结原件结算覆盖1.0，结算P95≤1000ms/P99≤3000ms；不以host延迟替代。
- 原受保护债务或未证明设备checkpoint条件明确BLOCKED，不能通过强制删原件过门。

### 22. R31-B09-C02#A1b

**原文：** UI中其余面板实际可见。

真实222原文 SHA256：`72e499c87d809098549f88cd8c2497cd10cea13f18a25bf75c86de5404620220`。归属：P5。原级别：`EMU`。状态：**NOT_RUN**。工程批次前置：**否**。证据层：`ANDROID_EMULATOR_OR_PHYSICAL_DECLARED_SEPARATELY`。

拆分前原条款：`R31-B09-C02#A1`；原卡文本 SHA256（也是保留的旧 textSha256）：`de28a0236fd2cca92904b0362969c35bf6789ad62f88f49384c2b0f286855dea`。

使用共用操作：`P5_IDENTITY`、`UI_AND_HEAP`。

**具体执行步骤**

1. 在明确标记的Android模拟器或实际设备上打开原图表页和研究工作台，逐组逐页滚动，按ChartRegistryR31稳定ID勾验全部非故障面板。
2. 对一个空数据/超时/坏帧面板记录真实错误状态，再检查其他面板仍在真实视口可见并能操作。
3. 每张截图保存实际标题、ID、状态与屏幕几何，避免只保存离屏View或相同截图。

**必须保留的原件**

- 稳定ID→屏幕截图/录像索引
- 故障面板与相邻健康面板图证
- 页面状态与frame来源

**判断条件**

- 其余面板实际可见/可交互；单个失败不能拖垮整页。
- 仅registry有47项或View创建成功不能签收，未拍到的ID仍NOT_RUN。

### 23. R31-B09-C02#A2b

**原文：** 真实Activity切tab/后台/旋转100次无BadToken与泄漏。

真实222原文 SHA256：`453dab9d7d67a66cdf713627b5f9c22cb93e5c2cfd60bc02205adc82249bc13b`。归属：P5。原级别：`EMU`。状态：**NOT_RUN**。工程批次前置：**否**。证据层：`ANDROID_EMULATOR_OR_PHYSICAL_DECLARED_SEPARATELY`。

拆分前原条款：`R31-B09-C02#A2`；原卡文本 SHA256（也是保留的旧 textSha256）：`4a2fecea72253a136de28aba92999ae64f6875b3ea42cdafe4e7b8b572415a71`。

使用共用操作：`P5_IDENTITY`、`UI_AND_HEAP`、`DEVICE_ORIGINALS`。

**具体执行步骤**

1. 在目标Android模拟器或实际设备的隔离测试资料上安装当前测试APK，执行现有100次Activity生命周期方法，并保留实际环境身份。
2. 对100次切tab、进入后台、旋转逐轮记录完成与旧Executor终止/旧窗口detach；补充前后heap retained路径。
3. 检查BadToken、WindowLeaked、旧UI回调、旧session、线程终止；停止中仍未退出的owner必须等待或明确未完成。

**必须保留的原件**

- 实际instrumentation完整stdout/计数/退出码
- 100轮生命周期事件与异常日志
- 前后heap/线程/Window原件

**判断条件**

- 真实100轮完成，旧owner无复活、旧Window已detach、无BadToken/泄漏；不能用Future.cancel返回值当终止。
- 本条原级别EMU，真实Android模拟器可提供该层证据；不因此声称另一个DEVICE长期heap条款通过。未取heap只能通过方法所覆盖部分。

**已核对存在的Android支持入口（均未执行，不是新增原JUnit绑定）**

- `com.ethk3.novaonepro.R31ChartLifecycleInstrumentedTest#hundredRealTabBackgroundRotationCyclesCloseTheirActualOwners`

### 24. R31-B09-C02#A3

**原文：** 所有耗时目标由设备P95回执而非代码常量签收

真实222原文 SHA256：`64467b7c8abe30671abc481582dc297c32788b589d21791aad5d5b43be3ae8c2`。归属：P5。原级别：`DEVICE`。状态：**NOT_RUN**。工程批次前置：**否**。证据层：`PHYSICAL_DEVICE`。

拆分前原条款：`R31-B09-C02#A3`；原卡文本 SHA256（也是保留的旧 textSha256）：`64467b7c8abe30671abc481582dc297c32788b589d21791aad5d5b43be3ae8c2`。

使用共用操作：`P5_IDENTITY`、`UI_AND_HEAP`、`DEVICE_ORIGINALS`。

**具体执行步骤**

1. 先登记每个耗时指标定义/起止事件/单位/原目标，例如首个ready面板与每面板query。
2. 真机在冷开/热开/切页/后台恢复及真实并发负载下采重复请求原计时，不修改超时常量充当数据。
3. 按每项完整成功/失败/取消/超时分母复算P95/P99/max；实际frame提交与数据query完成分别报告。

**必须保留的原件**

- 每项设备计时原行和故障样本
- 冷热/并发场景标签
- 分位数计算输入、目标配置hash、gfx/frame原件

**判断条件**

- 原first_ready_panel_target_ms=3000及per_panel_query_timeout_ms=3000分别由设备原件判断。
- 只能证明测到的指标；只有常量、平均值或桌面host时长，均不能签全部耗时目标。

### 25. R31-B09-C05#A1b

**原文：** 47图渲染图证。

真实222原文 SHA256：`605157694a1c64fbe65cc877bcadd8f071fd694f34bc570bc8d2e4701d20f4ee`。归属：P5。原级别：`EMU`。状态：**NOT_RUN**。工程批次前置：**否**。证据层：`ANDROID_EMULATOR_OR_PHYSICAL_DECLARED_SEPARATELY`。

拆分前原条款：`R31-B09-C05#A1`；原卡文本 SHA256（也是保留的旧 textSha256）：`638c718520494fd65b66ee521e29cac8a91e076f307d0632dc385f7fbb6fab5c`。

使用共用操作：`P5_IDENTITY`、`UI_AND_HEAP`。

**具体执行步骤**

1. 使用ChartRegistryR31的47个对象ID建立拍摄清单：29个旧页对象和18个研究视图，逐项真实滚动到视口。
2. 运行现有R30ChartsAndroidTest和R30RepairChartsAndroidTest可获取渲染机制图证；标记合成/EMPTY/INSUFFICIENT/READ_ERROR，不混成自然业务图。
3. 再对实际业务资料逐项取当前真实屏幕，保留标题、geometry、像素hash与原frame ID；空缺数据也应拍明确状态。

**必须保留的原件**

- 47ID截图索引与全部实际PNG/hash
- 两套原截图测试日志/场景标签
- 当前真实业务frame/缺失理由

**判断条件**

- 47对象各有可复核图证，标题和目标区域可见，无重复图片冒名。
- 合成渲染图只证明相应显示机制，不证明自然策略质量或当前业务数据已充分。

**已核对存在的Android支持入口（均未执行，不是新增原JUnit绑定）**

- `com.ethk3.novaonepro.R30ChartsAndroidTest#allFourteenLegacyAndTwoSnapshotChartsOpenInThreeStatesAndSaveScreenshots`
- `com.ethk3.novaonepro.R30RepairChartsAndroidTest#eighteenViewsRenderCommittedAnalysisOrExplicitMissingStatesAndCancelCleanly`

### 26. R31-B09-C05#A2

**原文：** 设备截图数值与导出frame hash指向同一数据；单位/role/期范围不矛盾

真实222原文 SHA256：`41c2a4feb32ac525635ad7f1f5eaee728ac045be310ab05a037915d1d978da57`。归属：P5。原级别：`DEVICE`。状态：**NOT_RUN**。工程批次前置：**否**。证据层：`PHYSICAL_DEVICE`。

拆分前原条款：`R31-B09-C05#A2`；原卡文本 SHA256（也是保留的旧 textSha256）：`41c2a4feb32ac525635ad7f1f5eaee728ac045be310ab05a037915d1d978da57`。

使用共用操作：`P5_IDENTITY`、`UI_AND_HEAP`、`DEVICE_ORIGINALS`。

**具体执行步骤**

1. 在同一次UI读取/导出快照内绑定稳定panel ID、frame ID、payload/frame hash、source/run/revision及watermark。
2. 实际截图可见数字逐项对照导出的同frame原值、单位、role、期范围；不能以相邻时刻另一个快照代替。
3. 若UI发生刷新，保留各自epoch与frame，重新选择同帧证据，不把旧图与新JSON拼接。

**必须保留的原件**

- 截图与同次frame/snapshot原件及hash
- ID/epoch/run/revision/watermark关联表
- 逐字段数值/单位/role/范围核对结果

**判断条件**

- 截图所见与确切frame一致；缺值/裁剪/抽样必须有原说明，不能填0或跨role比较。
- 像素hash不等于frame hash，单有两个hash格式合法也不算绑定成立。

### 27. R31-B12-C01#A1b

**原文：** Android覆盖安装后各切点杀进程恢复。

真实222原文 SHA256：`fbc44db976669dc03117d8711afea84a7c30bc78bda0d0ee8d147fc72d486de7`。归属：P5。原级别：`EMU`。状态：**NOT_RUN**。工程批次前置：**否**。证据层：`ANDROID_UPGRADE_WITH_PHYSICAL_SCOPE_RECORDED`。

拆分前原条款：`R31-B12-C01#A1`；原卡文本 SHA256（也是保留的旧 textSha256）：`10cf74e52b71466447c17fdf7bee8bee6be8319434bea6e191975af94da9f801`。

使用共用操作：`P5_IDENTITY`、`UPGRADE_PROTOCOL`、`DEVICE_ORIGINALS`。

**具体执行步骤**

1. 在隔离Android恢复环境先安装真实032并恢复经验证旧数据，记录所有受保护资产和迁移前身份。
2. 列出候选迁移每个持久切点；逐切点使用独立的已保全032恢复副本，先同签名覆盖候选，再以真实调试器断点/已有明确故障注入处启动迁移。确认进入指定切点后杀该迁移进程，保留实际日志/journal/切点与旧PID已消失的证据。
3. 在该切点已发生中断的同一副本上重新启动候选，核对无丢失/重复、迁移幂等、旧资产仍读；未命中切点不得算执行。开始下一个切点前另建隔离副本，不把已经迁移完成的库当下一次032前态。
4. 现有emulator升级脚本先作启动延时kill补充，scope严格保留EMU与非逐切点。

**必须保留的原件**

- 032旧APK/原库/模型/证书清单及hash
- 每一切点的位置、PID死亡、journal前后、恢复原件
- 覆盖安装/签名/versionCode与逐项差异

**判断条件**

- 所有原要求切点真正命中且恢复后保护不变量成立，才签该覆盖范围。
- 真实旧包/库或某切点注入入口缺失时BLOCKED/NOT_RUN；host模拟/5个延时kill不替代完整切点覆盖。

**明确缺口**

- 现有run_emulator_upgrade_matrix.py不是所有durable cut point自动runner；逐切点部分仍需操作人员真实断点/既有注入定位。

### 28. R31-B12-C01#A2

**原文：** 覆盖安装后UI可启动，实时关键schema优先可用，旧历史训练资产可继续读。

真实222原文 SHA256：`b769701954361ce4229075be6a73d7ce88c9d6f7446f992bfb043b8c6d1b0aeb`。归属：P5。原级别：`EMU`。状态：**NOT_RUN**。工程批次前置：**否**。证据层：`ANDROID_UPGRADE_WITH_PHYSICAL_SCOPE_RECORDED`。

拆分前原条款：`R31-B12-C01#A2`；原卡文本 SHA256（也是保留的旧 textSha256）：`b769701954361ce4229075be6a73d7ce88c9d6f7446f992bfb043b8c6d1b0aeb`。

使用共用操作：`P5_IDENTITY`、`UPGRADE_PROTOCOL`、`DEVICE_ORIGINALS`。

**具体执行步骤**

1. 保持原数据执行同身份覆盖安装，首次启动录屏并记录CRITICAL/研究/旧域各自READY时序。
2. 确认UI先可用、关键实时schema优先可用，研究迁移等待时可看到真实进度而非全软件卡死。
3. 用真实旧历史、旧已训练模型/证书/学习游标执行只读恢复与后续合法训练，并核前后hash/数量/边界。

**必须保留的原件**

- 安装/首次启动视频、schema journal及READY时刻
- 旧历史/模型/证书/游标可读证据
- 恢复后的新提交与原受保护资产清单

**判断条件**

- UI、关键schema和旧资产三项都有实际原件；一个启动截图不能代替旧资产可读/可续训。
- 清空数据后启动成功、fresh空库或仅同一APK二次安装不能签032覆盖条款。

**已核对存在的Android支持入口（均未执行，不是新增原JUnit绑定）**

- `com.ethk3.novaonepro.R31PlatformReadinessInstrumentedTest#normalFirstStoreAndBundleCaptureHaveARealStartupOwner`

### 29. R31-B12-C02#A3b

**原文：** Android全链故障注入矩阵（API29/35）。

真实222原文 SHA256：`4caf84900635c59df4628b74b7176d9fd8977af0b132c9694aca328e792990f9`。归属：P5。原级别：`EMU`。状态：**NOT_RUN**。工程批次前置：**否**。证据层：`ANDROID_EMULATOR_API29_AND_API35`。

拆分前原条款：`R31-B12-C02#A3`；原卡文本 SHA256（也是保留的旧 textSha256）：`43c7cbd06037cbe47e03c67ca8d5e5443c33b90414b30d25ccbc9b1dec8c677f`。

使用共用操作：`P5_IDENTITY`、`ANDROID_PROTOCOLS`、`DEVICE_ORIGINALS`。

**本条额外输入**

- tools/r31/fault_injection_manifest.json原场景定义

**具体执行步骤**

1. 在API29与API35分别运行现有emulator_matrix.sh，并核对原fault_injection_manifest的F01-F10与原runtime/research矩阵逐格。
2. 保留process kill前后真实PID、原SQLite FULL、锁竞争、陈旧fence、乱序、时钟变更、图超时、恢复/重复消费等实际注入原件。
3. 对联合协议未触及的组合单独列UNREACHED/NOT_RUN，不按总exit0自动把所有场景填PASS。

**必须保留的原件**

- 两API独立完整输出与APK/source身份
- fault_injection_manifest逐格执行账本
- kill/恢复/原SQLite/保护原件及异常

**判断条件**

- 每格明确命中且无重复提交/结算、无非法预测、无受保护删除、恢复状态等于未中断对照。
- EMU是实际Android机制层，不能写成物理72h或自然策略有效；任一未执行格不签整体全链。

### 30. R31-B12-C05#A1b

**原文：** 设备安装身份与收据一致。

真实222原文 SHA256：`9f9377b7d8350ff9c571116e2a62e704819ec5982b3e0a9c1d606a35c1d19994`。归属：P5。原级别：`DEVICE`。状态：**NOT_RUN**。工程批次前置：**否**。证据层：`PHYSICAL_DEVICE`。

拆分前原条款：`R31-B12-C05#A1`；原卡文本 SHA256（也是保留的旧 textSha256）：`6d493b4749fa9cf332ec8088ee4e4bf9e739320e6f40278570721a7039c5c86d`。

使用共用操作：`P5_IDENTITY`、`DEVICE_ORIGINALS`。

**具体执行步骤**

1. 记录交付ZIP/源码/构建输入/APK摘要、包名、签名、版本与原签收回执。
2. 在目标真机安装后用pm path找到实际安装的base.apk，回取真实字节并与交付APK比较hash/签名/versionCode；若split则同时保留全部split列表。
3. 启动后导出应用实际BuildConfig/source/plan身份，核对同一安装而不是手工参数或邻近构建。

**必须保留的原件**

- 交付ZIP/APK/源码身份与原receipt
- pm path/dumpsys package/实际安装APK与签名原件
- 实际应用运行导出的身份与启动截图

**判断条件**

- 同一source/build/APK/签名/version/安装互证，覆盖身份符合原规则。
- 只把--source-commit或--apk参数填一致不能通过；安装APK无法回取/运行身份不明则BLOCKED。

### 31. R31-B12-C03#A1

**原文：** 总纲指标按明确分母与时点复算，所有异常样本保留。

真实222原文 SHA256：`c296bdc8eb89673639239fb56848cc48e2f9aafaeafff148208e17df0f67423d`。归属：P5。原级别：`DEVICE`。状态：**NOT_RUN**。工程批次前置：**否**。证据层：`PHYSICAL_DEVICE`。

拆分前原条款：`R31-B12-C03#A1`；原卡文本 SHA256（也是保留的旧 textSha256）：`c296bdc8eb89673639239fb56848cc48e2f9aafaeafff148208e17df0f67423d`。

使用共用操作：`P5_IDENTITY`、`DEVICE_WINDOW`、`DEVICE_ORIGINALS`。

**具体执行步骤**

1. 对总纲每一指标先记录原定义/单位/起止时钟/分母/排除规则，并引用acceptance_config原hash。
2. 用完整calendar、fetch、freeze、settlement、任务提交、内存与物理存储原件逐项复算，不用报告展示截断页或已聚合P95均值。
3. 把所有错误/超时/取消/缺期/未知/异常原样保留并给出所在分母；未知不得填0或100%。

**必须保留的原件**

- 指标ID→完整输入/计算步骤/分母/输出映射
- 全量异常原件索引及hash
- 配置身份、时钟与snapshot边界

**判断条件**

- 每项都有明确可复算分母与时点，异常未从分母消失；不能用整体报告生成成功替代指标真实性。
- 原件不够的项为BLOCKED/INSUFFICIENT，其余工程结果可独立保留。

### 32. R31-B12-C03#A2

**原文：** 任务隔离没有永久静默停摆；停止/缺依赖有真实理由，满足恢复条件后推进。

真实222原文 SHA256：`eae9776065dfc4aae3e626f4a760c07dc9c293e89d4a302ef2a95427f15cfe73`。归属：P5。原级别：`DEVICE`。状态：**NOT_RUN**。工程批次前置：**否**。证据层：`PHYSICAL_DEVICE`。

拆分前原条款：`R31-B12-C03#A2`；原卡文本 SHA256（也是保留的旧 textSha256）：`eae9776065dfc4aae3e626f4a760c07dc9c293e89d4a302ef2a95427f15cfe73`。

使用共用操作：`P5_IDENTITY`、`DEVICE_WINDOW`、`DEVICE_ORIGINALS`。

**具体执行步骤**

1. 逐任务登记任务族/owner/ready依赖/真实提交cursor，持续观察停止与缺依赖状态。
2. 只对已确认可安全恢复的条件执行恢复操作，例如恢复网络/资源/原缺件；记录同一任务何时满足条件以及之后提交。
3. 把归档r27-atomnet-archive的对象故障与r27-atomnet学习状态分开，核好对象/冷启动/训练能继续，故障原件仍可追踪。

**必须保留的原件**

- 逐任务状态与实际提交水位全窗
- 停止/依赖/对象故障原因及解除时刻
- archiveMaintenance与learner分栏原报告

**判断条件**

- 没有永久无理由停摆；依赖恢复后按原公平调度推进，不能用任意心跳/重命名任务代替。
- 未知/完整性/物理故障不得吞；缺件未补齐就明确BLOCKED，不伪造原件解锁。

### 33. R31-B12-C03#A3

**原文：** 后两日增量与常态预算合格，或明示BLOCKED，不能发最终通过收据。

真实222原文 SHA256：`57fef9254cd16d134dd57f3e87e646eab08c688ee995d08b3d24ea23161470c3`。归属：P5。原级别：`DEVICE`。状态：**NOT_RUN**。工程批次前置：**否**。证据层：`PHYSICAL_DEVICE`。

拆分前原条款：`R31-B12-C03#A3`；原卡文本 SHA256（也是保留的旧 textSha256）：`57fef9254cd16d134dd57f3e87e646eab08c688ee995d08b3d24ea23161470c3`。

使用共用操作：`P5_IDENTITY`、`DEVICE_WINDOW`、`DEVICE_ORIGINALS`、`UI_AND_HEAP`。

**具体执行步骤**

1. 完成72h完整窗口并保留预热/稳定首24h/末24h，以及末两天物理存储样本。
2. 从全部目录含protected和UNCLASSIFIED字节复算末两天平均每日增长；并行核对等负载内存、任务进展和资源债务。
3. 输出各gate及总体状态；失败后的新窗口另立回执并保留旧失败，不与旧好样本平均抹平。

**必须保留的原件**

- 72h全部原JSONL/merged/verdict与配置hash
- 末两天物理字节起止/峰值/每日增量
- 等负载内存与保护债务原件

**判断条件**

- 正常预算与末两天每日增长≤50MiB按原门判断；保护债务超限必须BLOCKED_PROTECTED_DEBT。
- 未满两天/缺样本/缺目录/未知live heap时不能发最终通过回执；允许如实BLOCKED并保留工程批次通过。

## 来源核验与实际执行状态

本规程引用的 34 个源码/工具路径均已在给定候选源码中检查存在；涉及的Android支持方法也已检查真实声明。逐文件SHA256见JSON的source_references。该检查只证明入口存在，没有运行Android、设备、长稳、安装或自然协议。

原222定位仍维持189条原显式绑定和33条外部项；本文件没有把33条改写成已自动覆盖，也不声称每条已有完整自动runner。各项实际执行以后，应另存带源码/APK/设备/输入/命令/起止/原件hash/分母/结果的独立回执。
