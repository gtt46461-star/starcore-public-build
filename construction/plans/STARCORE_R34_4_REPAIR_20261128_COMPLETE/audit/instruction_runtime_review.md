# R34.4-211 施工提示词独立复核：构建身份、逐批 APK 与报告导出

## 复核结论与范围

本次只读核对已公开计划 commit `9558b6ddbcf9691b2e978bc76b4809fe71163cde` 的 `CODEX_EXECUTE.md`、`BATCHES.json`、`ACCEPTANCE_MATRIX.json`、报告专项合同，以及安装版本 211 的准确源码 commit `5c7f1b36eaee6036df280e85835bb0fb4ce0b40d` 中的构建、版本事务、语义身份、CI、封包和发布读回工具。恢复后的完整交付包 `complete_plan/CODEX_EXECUTE.md` 也已读取。未修改应用源码、未触发 CI、未构建 APK、未安装、未上传或修改远端；本文件不是产品验收结果。

**必须在最终施工提示词中明确：B00 包含真实的 R34.4 构建与交付链适配工作，不能仅运行旧工具或核对方案文件后宣告 B00 完成。** 211 的旧工具仍明确绑定 173、旧计划、旧批次与旧 runner；原 CI 只有旧 B12 执行签名 APK 的实际报告 UI→SAF 回归。现有提示词已提出“增量兼容”“每批构建”，方向正确，但没有把这些确定的执行障碍逐个列清。应新增 B00 执行补充合同，保留旧版本交付能力，建立可实跑的新入口。这里不给尚未实现的工具编造命令名或成功回执。

用户现已明确授权上传完整资料，旧公开版页首“原件不能公开提交”的描述属于前一轮交付范围，不能继续当作新的授权限制。完整包和旧公开包仍须按各自清单分别核验；不能把五候选和原报告直接塞进旧四候选 PUBLIC_SCOPE 后声称旧公开包校验通过。新增完整发布应有独立文件集合、哈希清单、最终 commit 和交付回执。

## 一、已确认的执行缺口

|编号|准确源码证据|对施工的影响|必须补入提示词的动作|
|---|---|---|---|
|IR-01|`tools/r343/build_identity.py:8–14,35–62` 固定 `4d1de9… / 577311173`、R34.3 计划、field173 schema；versionName 只接收 `R34.2-B01…B12`。`prepare_field_batch.py:62–74,108–117` 同样固定旧批次、旧计划及其 seal。|新计划是 211、B00–B11，不能直接传 `--batch B00`，也不能把新计划传入旧 `--plan` 就获得新身份。|新增明确的 R34.4 活动计划绑定与版本批次合同；旧字段与封印保留为历史继承依据。不得把211改称173、把本轮B00偷写成旧B01，或改旧计划内容迎合旧校验。|
|IR-02|`tools/r33/verify_plan_binding.py:216–254` 根据 release 名称选择旧 series，版本号高于173或 field173 binding 存在时仍调用 r343 当前 identity；`tools/r343/field_delivery_contract.py:33–49` 也把这一路径视为活动 field。|仅新增一个 `field211_plan_binding.json` 而不贯通总 identity 与交付分派无效；更改 release 名称还可能触发历史 series 绑定拒绝。|明确区分历史 seal 验证和当前活动 repair identity；总 identity、生成/嵌入 APK 的身份、CI 回执、封包与发布消费者采用同一受审查分派。旧 r33/r34/r341/r342/field173 的原 seal 不改、不删。|
|IR-03|`tools/r343/rebind_semantic_inputs.py:12–20,36,131–177,303–328` 使用旧173 BASE/PATCHSET、显式 NEW_ROLES 与 REVIEWED_EXISTING，写入当前 R967/R82 两个身份文件。|新类或本轮新改动不在旧白名单；旧 PATCHSET 也不是本轮五候选或后续完整实施清单。仅重跑旧 rebind 会拒绝，强行照旧后态覆盖则损坏211后续改动。|为211后的实际批次修改建立增量语义审查记录；逐个新生产文件登记角色与理由，旧角色保持既有语义。先核实际输入字节与本批变更，再更新当前身份。保留旧数学/ABI/PIT/资格与动态依赖审查规则，不以全标 PRESENTATION 消除失效。|
|IR-04|`tools/r343/field_delivery_contract.py:61–82,85–123,164–254` 读取旧计划 `ACCEPTANCE_MATRIX.json["cases"]`，限定 `R343-…-POS/NEG/KEEP`，runner 位于 `release/field173_test_runners.json`。新计划 `ACCEPTANCE_MATRIX.json` 顶层为 `schema/issues/principle`，每项是保全/验收文本和未执行状态。|新计划本身尚不是可直接喂给旧工程驱动的执行 catalog；不能把文字行数或构造的 PASS JSON 当真实断言。|在B00将新卡的具体工程/设备/自然验收映射为可运行 case catalog 和提交在源码中的 runner；每个 runner 调用实际生产路径或明确隔离的生产方法测试，绑定输入、命令、超时、输出与数据库读回。旧case保留回归，新增case不能伪装为旧case。|
|IR-05|`.github/workflows/build-apk.yml:328–377` 限定授权签名工作流 ref `construction/r27-20261028`；source 必须属于固定 r342/r343 建设 ref 的允许祖先集合；批次只接受B01–B12。`tools/r342/ci_plan_inputs.sh:48–60` 只增加旧173计划和 baseline173.apk。|在新计划分支直接 dispatch 不能假定进入签名构建；新施工提交也可能不属于允许集合。|在授权能力内给本轮源 ref/计划 pin/211 基线新增严格准入；保留授权签名 ref、指定仓库、祖先验证和历史 seals。不得删除 allowlist、允许任意提交借 signer、改钥匙或搬出私钥。若权限或原 signer cache 缺失，记录真实阻断并继续独立工程。|
|IR-06|`tools/r343/field_delivery_contract.py:13–18,53–58` 固定 baseline173；`tools/r33/package_ci_candidate.py:269–280` 活动 field173 时要求该基线；`tools/r342/ci_candidate_tests.py:282–287` 选择它做覆盖安装。|173→新包的测试不能证明211→新包数据兼容，更不能证明实际前一批→后一批升级。|保留原033/历史基线原件用于历史签名链验证，同时新增准确211原签名 APK 身份和实际当前安装观察；每批另验前一实际交付批次→本批。不存在原211 APK时诚实保留缺件，不能重编译一份或改名173冒充已安装原件。|
|IR-07|`tools/r342/ci_candidate_tests.py:286` 只有 `stage == 'B12'` 执行 `signed_report_campaign`；该campaign在`:89–117`核实际签名包 source-manifest/apkSha，`:38–63`做 baseline compact 和候选 compact/full/compact。|旧CI多数批次的绿色不证明本批签名 APK 能导出；debug instrumentation 与签名release报告属于不同执行原件。|把本批签名release的真实 UI→导出作业→ZIP→SAF读回纳入每个新批次。B01/B02及后续影响导出的修改跑完整专项场景；其他批次至少正常、连续重复、取消后重试和分享读回。|
|IR-08|`tools/r342/ci_candidate_tests.py:43–63,66–85,279–281` 明确仅为 disposable API34 emulator，含 `adb root`、断网、安装旧基线、卸载包；之后再装 debug与测试APK。|直接把这段CI脚本复制到用户设备会违背数据保全；模拟器成功也不能冒充用户真机和大库测试。|把破坏性夹具只放专用模拟器。用户设备保持原包名/签名，覆盖安装前记录与保全原数据；不得卸载、清数据、回滚版本或调用CI的断网/root清理流程。真机、模拟器和主机结果分别记录。|
|IR-09|`tools/r33/run_release_build.py:13–22,52–89` pin现存证书、要求干净source，实际执行Release/Debug/AndroidTest，构建前后比较身份；`package_ci_candidate.py:282–320,336–356`核真日志、APK、source archive/bundle与工程原件；`tools/r342/release_delivery.py:34–80,128–140`和r341发布脚本完成下载读回。|只生成APK或只上传文件仍未闭合交付；新identity若不贯通封包与读回，会在后段失败。|B00贯通构建、签名、嵌入身份、工程证据、source archive/bundle、上传和下载重验。旧功能复用，新增schema/字段要所有消费者一致识别，不以删除校验“修复兼容”。|

上述均是静态源码可确认的执行边界，不等于本轮已触发一次实际失败，更不等于用户现有211 APK无法构建或导出。

## 二、B00 应形成的增量施工合同

### 2.1 正确输入与目录边界

1. 读取最终完整发布的 commit、MANIFEST/总SHA、原报告与五候选，保留旧公开版作为独立历史输入。先跑完整包现有 `tools/verify_plan.py`、`tools/check_candidates.py`、`tools/unpack_report.py`，保存原输出；这些结果只证明方案/候选/原件层级。
2. 从准确211建立隔离工作树；若采用已部署后代，先证明211祖先关系和逐文件增量，保留后代所有合法修复。计划commit不是应用sourcecommit。禁止用main旧应用文件替换211、整包复套173 after或强制覆盖无法解释的差异。
3. 为干净211只读基线、当前可变施工树和构建/版本事务/测试输出分别设置目录。外部 journal 与构建回执不混入冻结计划；不为通过只读 verifier 临时篡改计划基线或删改清单。实际新施工差异另立“从211到本批”的审查记录。
4. 真实获取211原APK和已安装/已发布最高版本观察。原诊断未带安装APK或完整5GB库时，明确缺件；报告中的安装身份不能代替 APK 字节或签名验证。原报告可供定位，不能自动作为设备端全量数据库重放输入。

### 2.2 当前身份模型

新增本轮补充 binding 至少可关联：计划commit与清单hash、211 source baseline、原报告hash、当前应用source commit/tree、本批B00–B11、实际versionCode/name、包名、公开证书指纹、原211 APK hash及实际升级来源、当前语义身份与runner catalog hash。

字段命名与 schema 由施工方按既有风格落实并列入审查，**不能在提示词里假装这些新字段/CLI已存在**。身份必须贯穿下面所有边界：

`准备版本与计划绑定 → 总build identity → APK内身份资产 → 本批工程runner → 构建回执 → 封包DELIVERY → 发布索引 → 下载读回`。

必须先选择并实施一种完整的版本系列方案：保留现有APK系列并显式映射本轮批次，或者新增经审查的新系列。若保留旧系列，必须把“旧series transport stage”和“本轮repair batch”分字段记录，映射一一对应、可回查且不覆盖旧batch证据；若新增R34.4系列，则所有解析器、版本工具、Gradle生成器、嵌入与消费方一起兼容。**两者都不能直接把B00改名旧B01后丢掉真实批次身份。** 不要求改写历史sealed输入来完成新系列接入。

现有签名检查器公开 pin 为 `d8d8cee7f406267b2bf18fc9aef0c6ab760423cf205ccd6613dd44085bcc74e7`（211 `run_release_build.py:13`）。施工时还应对原211 APK和本批APK实际执行 `apksigner verify --verbose --print-certs`，核一致；本次只读审阅没有重新验证APK字节或证明私钥当前可用。

### 2.3 版本与语义身份必须用真实事务推进

复用 `tools/r33/advance_release_version.py` 现有仓库锁、四文件一致生成、外部journal、before/after hash、崩溃续跑和用户编辑冲突保护。四个受控文件是：

- `release/release_manifest.json`
- `release/release_identity.properties`
- `app/src/main/java/com/ethk3/novaonepro/ReleaseManifestR926.java`
- `verification/r36-stubs/com/ethk3/novaonepro/BuildConfig.java`

新code按真实当前源、设备安装、已发布最高值取最大值后推进；不能在提示词预占577311212，也不能为恢复journal重复分配多个版本。旧工具只认R33.5/R34/R34.1/R34.2且field系列从B01开始，新增接入须保留上述事务性质，不能四处手改版本号。

当前R967/R82身份每批与该批真实源字节一致，新增类、native接口、SQL/读取路径等按现有语义分类政策审查。旧文件已标NUMERIC的，不能因本次看起来是“只读报告”就无证明降级角色；现有封印与过去训练身份不改。学习规则改变需要新的当前身份及适用的失效/新实验，不把旧资格无条件移植过去。

语义/版本变更提交后才执行严格干净source构建。构建日志、源码commit/tree、计划pin、APK hash和case执行原件在外部回执绑定，避免把最终APK SHA反向写进APK造成自引用。

### 2.4 本轮实际 runner 与 B00 正反例

从本轮验收文本落实真实的工程case列表并记录其来源卡。每case必须有受审查的执行命令、受控输入文件及hash、有界timeout、实际输出、必要数据库读回、唯一executionId；不能只输出 `CASE_ASSERTIONS_COMPLETE/PASS` 而不执行断言。旧R343事件格式可以继承机制，但本轮case/plan/source/APK身份必须独立准确。

B00 至少实际覆盖：

- 错误main/173应用底座拒绝；211或审查后代接受；不同应用commit不能共用旧回执。
- 新plan绑定、旧seal验证与当前activity分派；旧版构建入口继续工作。
- 版本四文件在准备后/部分应用后中断可恢复；文件被用户改动后拒绝覆盖；重复resume不再次涨版本。
- 新类未登记、已有角色被无依据修改、语义文件过期时拒绝；受审查的本批输入生成一致身份。
- 非允许source/ref、错计划pin、错签名、错误211原APK、runner/case集合缺失及外批原件均拒绝。
- 新B00–B11身份从CI入口到APK、工程回执、封包、发布索引及下载重验保持一致。
- 原签名release至少能覆盖211启动、保留数据和完成一次真实导出；只在模拟器完成则明确是模拟器工程证据。

新增R34.4兼容工具的实际路径、`--help`与完整命令必须随B00提交，之后批次直接使用经实跑的命令。不得在最终施工说明中仍只留“核当前工具用法”作为已经解决的交付链。

### 2.5 CI 与签名授权边界

现有授权签名ref、仓库名、source祖先检查、固定来源允许列表和原 signer preflight 均是需要保留的行为。新增本轮源分支与source baseline时应是**明确、最窄的允许项**，并以正反例证实任意分支/任意提交仍不能调用签名。不能因为用户授权“所有”就把签名cache、token或私钥移出受控执行环境。

CI输入恢复新增本轮封印计划与原211 APK；033/历史计划继续只读复核。所有旧源码/旧计划都按pin读取，不将浮动main当源。当前211工作流工具链已指定JDK17、Android platform36/build-tools36.0.0、NDK28.2.13676358、CMake3.22.1、Python3.12；这是准确211工作流声明，施工时按锁定配置核实际可用性，缺依赖先合理补齐而非立即放弃。

有现成合法构建/签名通道时继续完成；若签名cache缺失、权限不允许改受保护workflow、网络/依赖仓库不可达或原APK缺失，保留具体错误。可以交付未签名构建或主机测试作诊断，但不能把它称为可覆盖安装的正式APK或删掉签名门。

## 三、每批的可执行交付循环

最终提示词应明确 B00 完成后，B01–B11 采用以下循环，不停在“已打候选补丁”或“文档已上传”。

1. 根据本批卡读取真实方法、生产owner、数据来源及原逻辑，确认当前树与上一交付批次差异；修改应小而可解释，新增类须有必要性。先确认本批依赖是工程前置、特定原件前置还是现场自然观察，不能把三类门混成全局等待。
2. 完成本批实现，运行真正覆盖风险的正例、反例、旧功能保全与崩溃/重复提交恢复测试。基于已确认风险选择测试，不反复运行没有新增信息的测试。SQL主机夹具与目标Android SQLite分开，JNI需设备/模拟器实际执行；模拟源或stub不能签成真实生产路径通过。
3. 通过本批当前语义/版本事务生成并提交干净source，记录对应计划pin、变更前后hash及review理由。该source进入原签名构建，保存原Gradle日志和退出码，核构建前后source不变及实际APK唯一性。
4. 对真正交付的releaseAPK做包名、versionCode/name、证书、CRC、SHA与嵌入身份校验。精确211→本批，以及实际前一批→本批的升级路径分别说明；签名匹配和UID不变只是兼容证据的一部分，还要读回真实保全数据或专用迁移fixture中的关键原件与引用。
5. 用本批签名release运行报告回归，核导出 `source-manifest.json` 的APK hash/source；FULL_RAW还核归档APK与要求的原库覆盖。每批的UI读回必须是这个APK，不用debug或旧B12原件代替。
6. 将source archive、可恢复git bundle、APK、构建/签名/测试/报告原件、差异、迁移和可行回退说明组合为本批交付。不能把版本降级安装或恢复过时数据库写成默认回退方法；涉及schema时须说明实际兼容与无损恢复边界。
7. 授权范围内上传该批source和交付物，下载实际URL重新核字节数、SHA、CRC、身份与可读性，记录source commit、CI run、APK SHA、URL和下载回执。push成功不等于APK已交付，生成链接也不等于链接内容已验证。
8. 立即推进已满足输入门的下一批。已确认工程阻断只阻相应范围；训练/独立新实验不等所有旧pin或检查点恢复，B09工程也不等24/72h自然窗口全部完成。没有自然证据保持NOT_PROVEN，继续可运行的工程与观察，不改资格门。

建议为每批维护机器可读交付记录，至少有 `batch / issueIds / sourceCommit / sourceTree / planCommit / planHash / predecessorApk / apkHash / package / versionCode / certificateHash / buildRun / commands / inputHashes / outputHashes / engineering / android / field / natural / blockers / nextAction / downloadReadback`。这是建议新增合同，不表示当前仓库已有同名schema。

## 四、报告导出保护必须写成可观察的通过条件

现有报告专项合同已充分区分“ZIP能交付”“检查都执行”“检查健康”“业务运行验收”。最终提示词需把它放到每批发布门，尤其防止一次新训练修复意外破坏报告。

### 每个可安装批次的共同门

- 签名release真实启动并从原入口请求导出；ZIP/index、内部清单、逐文件SHA/bytes和CRC、语义回执正确，SAF保存后从交付URI实际读回。仅调用底层zip库不足。
- 96个检查编号及44指标的原有外部合同、附件、单卷/分卷、DIAGNOSTIC/FULL_RAW、取消/重试和分享行为保留。检查失败/UNKNOWN仍可形成诚实可分享诊断，不添加全PASS或checksComplete门槛。
- 连续重复导出与取消后重试，不挂死、不静默跳过检查、不复用取消对象。报告的分析/压缩/传输不持有实时writer；预测、结算同期时序纳入观察。
- 结果精确绑定本次snapshot/attempt/source/APK/boot/PID/database；历史快照不冒充当前健康，scope必须可完整解析。格式兼容无破坏，新字段缺失时旧job仍能导出。

### B01/B02及影响导出路径的后续改动，须跑专项10场景

连续5次正常导出、慢查询/共享依赖失败、取消后重试、写ZIP中途进程终止、分享后进程退出、低盘/配额、fault满、当前与历史scope、大库真实执行、新旧profile及单/分卷/SAF兼容。每场景保留实际输入/输出/日志/进程身份和APK SHA，未具备目标5GB快照的情况下保留该项未测，不以小夹具替代。

实施细节必须保留以下约束：

- memo typed失败限定同snapshot、生产版本、真实参数和attempt，不能跨fresh run永久缓存UNAVAILABLE。新的显式同快照重试可以按策略重新执行，不篡改旧attempt失败原件。
- 新内部状态不越过原catalog `EXECUTED/READ_ERROR/CANCELLED`；依赖失败按既有READ_ERROR/domain/UNKNOWN/errorRef表达。不能把所有失败变成0或健康PASS。
- 超时取消后必须确认worker不再用原连接，再继续或关闭；只调用Future.cancel不足。不能并行复用同一SQLite连接，也不能为每个检查复制新的5GB快照。
- freeze时钟必须来自真正SQLite pin的窄区间，并绑定生产者与boot。`CLOCK_MONOTONIC`不得冒充Android elapsedRealtime；采样失败保持UNKNOWN，不能让新增sidecar失败反过来阻断旧协议导出。
- 快照回收按profile与引用闭环执行。DIAGNOSTIC真实完成原请求、ZIP/清单/语义已验且无额外原件引用，可沿现有cleanRaw退役工作快照，不要求全部检查PASS或强塞原5GB库；FULL_RAW或唯一独立原件则需要同hash原库替代及读回。不要无限保留，也不能按时间或另一个成功job删除唯一原件。
- cleanup失败不得把已合法READY作业倒退，分享仍在读时不删ZIP；新请求需要新水位时不能假装复用旧快照。实际空间不足诚实等待并推进可安全回收债务。

原报告只有约19MB诊断载荷，不含用户原5GB SQLite，已有约5GB规模是待实测场景。现有CI emulator仅8G磁盘配置，不能假定它能同时容纳真实数据库、快照和完整原件包；应在大库测试前测可用空间并按真实峰值配置独立测试资源，宿主机也需单独核构建与交付物空间，不能为了省空间先删唯一原件。

## 五、真实阻断的处理要求

|阻断类型|应继续完成的工作|不能宣称什么|需要留下的原始证据|
|---|---|---|---|
|旧构建工具不认识R34.4/B00|实施上述新绑定、分派、runner、CI与封包的增量兼容，保留旧正反回归|不能把“工具不支持”永远作为停止施工理由；也不能删门或冒用旧batch|准确错误/代码锚点、增量设计、实际补丁、回归输出|
|构建依赖/SDK/网络缺失|检查项目既有工具链、允许的安装/缓存与合法CI通道，修可修的环境；独立源码实现继续|不能把未运行Gradle写成构建通过，不能伪造APK|实际命令、退出码、日志、缺依赖及合法恢复动作|
|原签名材料/受保护工作流权限不可用|使用既有允许签名路径；无权限时保留source、工程原件与具体受阻操作|不能新造签名冒充覆盖兼容，不能移出密钥、删除授权ref限制|授权通道实际错误、缺项、可继续范围与解除所需动作|
|准确211APK/原DB/模型原字节缺失|从已授权原发布/设备/完整包读取；同时做不依赖该原件的实现与工程反例|不能把重编译APK称原安装APK，不能由摘要重造数据库/模型|期望身份/hash、已核位置、缺失项、受影响case|
|用户设备不可接入|专用模拟器上实际编译/安装/迁移/导出；提供可验证APK与准确操作步骤|不能把disposable API34、debug instrumentation或host测试当用户真机PASS|模拟器和真机分栏，具体命令/包SHA，未测项|
|24/72h或自然独立未来尚未积累|保留可持续运行版本与现场监测、交付已完成工程，真实样本继续推进|不能缩短时间、用合成数据冒签现场，不能强制REAL或降低资格|开始/截止、窗口/样本来源、现场计数、尚缺数量与原因|
|自动审批或平台访问控制拒绝|停止该具体被拒动作，只选择实质更安全且合法的替代，保留所有已完成工作|不能不断换shell/接口重试同一被拒动作绕过审查|被拒操作、自动审批原因、替代方案及剩余真实阻断|

用户对完成施工和上传已授权的范围不应反复请示。只有真实缺少无法自行取得的输入/能力或遇到授权边界，才给出具体缺项和影响；这种记录不得把可独立完成的批次一并冻结。不得靠循环提交、重复CI或反复延长超时制造“持续施工”。

## 六、可直接加入最终施工提示词的强化段

> 你应实际完成本包施工、构建和逐批交付，不能停在阅读报告、生成计划、应用五个候选、主机夹具通过或给出未运行命令。首先把B00当作必须实现的工程批次：211当前公开工具硬绑定旧173计划/B01–B12，原CI也只在旧B12跑签名APK的报告UI→SAF；你必须新增兼容的本轮计划身份、B00–B11映射、语义输入审查、可执行case/runner、CI/source准入、211与实际前一批覆盖安装、封包和发布读回，并证明各边界一致。旧173工具、旧sealed计划、原签名与授权来源限制保留；不得改名冒用、删allowlist、换钥匙或用新编译APK假冒旧原件。
>
> 从准确211或已审查后代建立独立应用工作树。计划与source分别pin；原报告与五候选按完整包清单验证。完整方案检查和候选检查不等于软件通过。每次实际代码修改都以小补丁保留原有效逻辑与历史原件，落实当前R967/R82身份及版本四文件可恢复事务。真实versionCode必须高于已观察到的当前源、安装和已发布最高值，不在这里固定下一编号。
>
> 每批实现后提交干净source，实际构建原签名release，校验包名/版本/证书/嵌入身份/APK SHA，保留真实Gradle日志、同批runner输入输出和数据库读回。每批都用这个签名APK实际从报告入口导出、保存并读回ZIP；debug或旧B12回执不能代替。B01/B02和影响报告的改动完成专项10场景，其他批次也必须完成正常、重复、取消重试与分享回归。保留96检查、44指标、全部原模式和失败可导出语义，不通过删检查、填0、改PASS或增加全绿门解决问题。若任何变更破坏导出，应先修复该回归再作为可用升级交付。
>
> 专用模拟器可以安装基线、清理fixture和故障注入；用户设备必须覆盖安装并保留原数据，不能复用CI卸载/root/断网流程。大库、真机、长时间和自然质量分别有证据才签收。按profile和活引用闭环释放可退役快照，保护唯一原件；不能无限保留所有快照，也不能删除失败原件来消除背压。
>
> 每批交付source、APK、测试/报告原件、回退边界和可下载链接，上传后下载原字节重验，并马上继续满足输入门的下一批。不因某个旧pin、模型原件、签名环境或自然观察未完成而停止所有独立工程；也不得将缺证项写通过。真实阻断写清具体卡/命令/错误/输入/影响和下一动作，已授权范围不重复询问。保留唯一REAL/结算owner、PIT和资格门，不用模拟证据、伪造过去预测或强制出手冒充闭环恢复。

## 七、只读核查来源

以下引用均为本次实际通过GitHub只读文件接口取得的精确commit内容；行号来自对应原文。

- [公开 CODEX_EXECUTE，commit9558](https://github.com/gtt46461-star/starcore-public-build/blob/9558b6ddbcf9691b2e978bc76b4809fe71163cde/construction/plans/STARCORE_R34_4_REPAIR_20261128/CODEX_EXECUTE.md)
- [本轮 BATCHES](https://github.com/gtt46461-star/starcore-public-build/blob/9558b6ddbcf9691b2e978bc76b4809fe71163cde/construction/plans/STARCORE_R34_4_REPAIR_20261128/BATCHES.json) / [ACCEPTANCE_MATRIX](https://github.com/gtt46461-star/starcore-public-build/blob/9558b6ddbcf9691b2e978bc76b4809fe71163cde/construction/plans/STARCORE_R34_4_REPAIR_20261128/ACCEPTANCE_MATRIX.json) / [EXPORT_AND_STORAGE](https://github.com/gtt46461-star/starcore-public-build/blob/9558b6ddbcf9691b2e978bc76b4809fe71163cde/construction/plans/STARCORE_R34_4_REPAIR_20261128/contracts/EXPORT_AND_STORAGE.md)
- [211 build_identity.py](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/tools/r343/build_identity.py) / [prepare_field_batch.py](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/tools/r343/prepare_field_batch.py)
- [211 rebind_semantic_inputs.py](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/tools/r343/rebind_semantic_inputs.py) / [field_delivery_contract.py](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/tools/r343/field_delivery_contract.py)
- [211 verify_plan_binding.py](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/tools/r33/verify_plan_binding.py) / [advance_release_version.py](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/tools/r33/advance_release_version.py)
- [211 build-apk.yml](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/.github/workflows/build-apk.yml) / [ci_plan_inputs.sh](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/tools/r342/ci_plan_inputs.sh) / [ci_candidate_tests.py](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/tools/r342/ci_candidate_tests.py)
- [211 run_release_build.py](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/tools/r33/run_release_build.py) / [package_ci_candidate.py](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/tools/r33/package_ci_candidate.py)
- [211 release_delivery.py](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/tools/r342/release_delivery.py) / [publish_delivery_manifest.py](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/tools/r341/publish_delivery_manifest.py)
- [211 release_manifest.json](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/release/release_manifest.json)

复核产物只新增本文。应用源码、已有计划JSON/封印/候选、远端仓库和用户设备均未改动。本次结论只能签为“施工指令/构建源码合同只读复核”，不能签为Android构建、设备导出或实际返修完成。
