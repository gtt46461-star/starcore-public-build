# B00 正式施工附约：211 构建身份、逐批 APK 与报告导出保全

## 0. 效力、基线与完成边界

本附约补充本完整包的 `CODEX_EXECUTE.md`、`BATCHES.json` 和 `contracts/EXPORT_AND_STORAGE.md`，是 **B00 以及后续每批交付必须执行的合同**。下列9项是既有B00工作的执行缺口，不新增或重编号本计划的46项主问题，不替代其他批次的业务施工与验收。

准确应用底座为 `5c7f1b36eaee6036df280e85835bb0fb4ce0b40d`，版本 `577311211`，包名 `com.starcore.research.r13`。本附约的源码事实均从该commit核得；不能把方案分支的应用目录、main旧源树或173整文件后态当作211底座。采用211合法后代时，必须先核祖先关系与逐文件差异并保留其已有效修复。[S01][S02]

**211现存field repair构建通道固定绑定旧173计划、旧身份与B01–B12，不能不经适配直接作为本轮211、B00–B11的交付通道。** “沿用原工具”指保留并增量兼容原版本事务、身份、签名、封包和读回合同，不是将新计划直接传给旧CLI，也不是改名冒用旧批次。B00必须完成本轮可运行接入；只核方案哈希、检查候选适用性或输出构建方案不能签收B00。[S03][S04][S05][S06]

本文规定**待施工、待实跑的能力与验收**，不表示新工具、新schema、CI适配、APK或设备验收已经实现。新增命令的真实名称、参数和调用方式必须在B00实施后随源码及实跑日志交付，不得先编造一个尚不存在的命令宣称可用。本完整包现有只读验证脚本通过，只能证明其明确列出的方案/原件/候选层级。

## 1. 必须修补的9项兼容缺口

|附约编号|精确211证据|当前缺口|必须实施的增量适配|
|---|---|---|---|
|B00-C01|`tools/r343/build_identity.py:8–14,35–62`；`prepare_field_batch.py:62–74,108–117`。[S03][S04]|BASE为`4d1de9…`、baseline为173、PLAN_PATH是R34.3，schema为field173；只接受旧R34.2系列B01–B12。|新增明确绑定211、当前完整计划及B00–B11的活动身份。保留旧计划和旧封印；不得把211改称173、偷换计划路径、修改旧seal或用旧批次名称掩盖本轮批次。|
|B00-C02|`tools/r33/verify_plan_binding.py:216–254`；`tools/r343/field_delivery_contract.py:33–49`。[S05][S07]|总identity按旧series分派，版本高于173或旧binding存在就调用旧field173当前identity。只增加一个新binding文件仍会走旧逻辑。|总identity、APK嵌入资产、CI、封包和发布消费者统一区分“历史seal”与“当前活动repair”。保留历史验证，明确唯一活动合同；未知或互相冲突的活动绑定应拒绝，不能静默降级为旧173。|
|B00-C03|`tools/r343/rebind_semantic_inputs.py:12–20,36,131–177,303–328`。[S06]|rebind基于旧173的PATCHSET和显式NEW_ROLES/REVIEWED_EXISTING；不能直接审核本轮新文件或实际后续改动。|对211后的真实变更登记本轮增量语义审查，更新当前R967/R82身份。新文件逐一登记角色及理由；保留旧角色、动态依赖和数值失效政策，不能把新改动全标PRESENTATION求通过。|
|B00-C04|`tools/r343/field_delivery_contract.py:61–82,85–123,164–254`。[S07]|旧driver读取`ACCEPTANCE_MATRIX.json["cases"]`、旧R343 case ID及`release/field173_test_runners.json`；本轮矩阵顶层为`schema/issues/principle`，不是现成可执行runner catalog。|根据本轮卡建立可执行case catalog和提交在源码中的runner，关联issue/batch/level/assertions；保存实际命令、输入、执行上下文、输出和数据库读回。保留旧case回归，不把新case伪装为旧case，不把文字数量当断言。|
|B00-C05|`.github/workflows/build-apk.yml:328–377,393–413`；`tools/r342/ci_plan_inputs.sh:48–60`。[S08][S09]|签名工作流固定授权ref `construction/r27-20261028`，source须属于固定r342/r343来源允许集合，批次只接B01–B12；输入只增加旧173计划/APK。|在现有授权边界内新增本轮明确source ref、211祖先关系、计划pin及输入恢复。保留指定仓库、授权signing ref、允许列表、祖先检查和原signer preflight；不得删除allowlist或让任意提交借用签名。|
|B00-C06|`field_delivery_contract.py:13–18,53–58`；`package_ci_candidate.py:269–280`；`ci_candidate_tests.py:282–287`。[S07][S10][S11]|活动field173只认baseline173.apk，现成覆盖安装不证明211→新包，更不证明实际前一批→本批。|新增准确211原签名APK身份，另记录实际当前安装与前一交付批次。保留033/173等历史原件用于原合同验证，但不得把它们冒充本轮升级基线。原211APK缺失必须保留缺件，不能重编译或改名造原件。|
|B00-C07|`tools/r342/ci_candidate_tests.py:38–63,89–117,286`。[S11]|只有旧`stage == 'B12'`执行`signed_report_campaign`。其他批次CI绿色不能证明该批签名release能实际导出。|每个新批次都执行同一实际交付签名release的UI→报告作业→ZIP→SAF读回；绑定source/APK SHA。B01/B02按4.2的阶段归属执行专项回归，B02关闭存储/排空子断言；不能拿debug或旧B12回执代替。|
|B00-C08|`tools/r342/ci_candidate_tests.py:43–63,66–85,279–281`。[S11]|现成脚本为专用API34模拟器，包含安装旧基线、root、断网、卸载后再安装debug/test包。不能在用户设备照跑。|把可破坏夹具限定于明确的disposable emulator。用户设备覆盖安装并保全原数据、业务原件及引用，不卸载、不清数据、不强制版本降级，不复制模拟器的root/断网清理流程。|
|B00-C09|`run_release_build.py:13–22,52–89`；`package_ci_candidate.py:282–320,336–356`；`release_delivery.py:34–80,128–140`。[S10][S12][S13][S14]|构建、嵌入身份、原工程证据、source archive/bundle、发布索引和下载读回均有旧身份门；只改前端CLI或只生成APK不能完成交付。|贯通所有生产/消费边界，保存原构建结果、日志、source tree、签名、APK和case证据；新增身份必须被包装和下载读回再次验证。不以删除旧校验解决新schema不兼容。|

这些是源码可确认的兼容边界，不表示本轮已实测某次构建失败，也不能据此宣称现有211 APK不能导出。

## 2. B00实施顺序与不可省略的产物

### 2.1 锁定原件，保留干净基线

施工方必须先建立并记录以下独立输入：

1. **最终完整计划**：准确plan commit、目录、清单和总hash、五候选、原诊断和逐条证据。旧四候选公开包是独立历史版本，不得把新文件塞入旧PUBLIC_SCOPE后声称旧公开清单通过。
2. **211只读基线**：完整源树、1407生产Java的基线清单及现有旧修复。新的施工差异另立审查记录，不修改原基线清单来迁就施工后的源码。
3. **隔离施工树**：从211或已审查后代建立施工分支，保留现存合法差异；不得覆盖main旧应用源或整包复套173后态。
4. **准确安装与发布原件**：211原APK的真实字节、签名和版本证据；实际已安装最高versionCode、实际已发布最高versionCode和前一批交付身份。缺少某项必须记录已核位置、期望身份与受影响验收。
5. **外部执行输出**：版本/语义journal、构建日志、设备原件、case输出和交付包放在施工源树及封印计划之外，保留干净构建输入；输出目录必须遵守原工具的fresh/external约束。

完整包现有验证工具按实际参数先执行并保存原输出：`tools/verify_plan.py --source-root`应指向保留的准确211基线；`tools/check_candidates.py --source-root`用于其声明的临时副本累计检查；`tools/unpack_report.py --output`输出到新目录。施工后的实际源树另用增量合同验证，不得为使基线工具变绿而还原、遮蔽或篡改实际源文件。上述检查不是编译、签名、真机或业务验收。

### 2.2 先定义唯一活动身份，再接入全部消费者

B00必须提交当前活动身份规范、实现与正反例。规范至少绑定以下内容，具体schema与字段命名须在实现时审查，不能假称现有工具已经提供：

|身份内容|必须证明的关系|
|---|---|
|计划commit、计划目录与清单hash|构建使用最终完整计划原字节，不能引用浮动分支或另一份同名计划。|
|211 source baseline、当前source commit/tree|当前实现来源可追溯，实际构建字节与提交一致。|
|当前repair批次B00–B11及版本series/stage|本轮批次不可丢失、不可与历史批次混用。|
|applicationId、versionCode/name、证书指纹|保持原包名与原签名，新版本高于真实安装/发布最高值。|
|原211APK、实际升级来源及前一批APK|覆盖安装与数据保全能回指真实原包，而非文件名。|
|原诊断hash、当前语义identity、case catalog/runner hash|工程证据属于明确输入、真实规则和受审查执行器。|
|build run、APK SHA、执行ID、输出与下载回执|源码、构建、测试、设备运行和交付物不能跨批拼接。|

身份按同一规范贯通：版本/计划准备、总build identity、APK内资产、工程runner、构建回执、封包DELIVERY、发布索引和下载读回。只适配其中一层不得签收。

历史r33/r34/r341/r342/field173封印必须继续验证。新活动binding存在并合法时，旧field173只按其历史继承身份使用，不能因版本号大于173继续宣称它是本轮活动计划。旧入口在其原输入下仍应工作；新入口若发现多个冲突活动绑定或未知系列，应明确拒绝，不静默回退旧路径。[S03][S05][S07]

实施方必须在B00选择并提交一种完整的series策略：

- 新增明确的本轮系列，贯通所有版本解析、生成器、Gradle/资产生成、CI、包装和读回消费者；或
- 在保留既有APK series的情况下新增独立repair batch字段，显式、唯一地映射旧transport stage与本轮B00–B11，并保存不可混淆的双层身份及历史证据。

不能只将B00字符串换成旧B01作为“兼容”；不能在构建入口、runner和发布索引各自使用不同映射；不能覆盖历史同名批次原件。选择任何方案都必须保留旧seal与签名门，测试证明任意错映射会被拒绝。

### 2.3 版本推进保留原四文件事务

沿 `tools/r33/advance_release_version.py` 的仓库锁、原生成器、外部journal、before/after hash、原子应用、崩溃续跑与冲突拒绝扩展本轮入口。受控四文件仍为：[S15]

- `release/release_manifest.json`
- `release/release_identity.properties`
- `app/src/main/java/com/ethk3/novaonepro/ReleaseManifestR926.java`
- `verification/r36-stubs/com/ethk3/novaonepro/BuildConfig.java`

新versionCode按当前源版本、真实安装版本、实际已发布最高版本和适用系列下界取最大值后推进，遵守Android版本上限。不得预占固定下一编号，不得手改四处版本，不得为一次journal恢复再次涨版本。resume只接受其封存的before/after字节，用户编辑或身份分歧时保留原文件并报出具体冲突。

原工具仅识别R33.5/R34/R34.1/R34.2，field系列从B01开始；若增加系列/批次支持，应保留原生成和事务行为，不能通过绕过原prepare/apply校验来接入B00。[S04][S15]

### 2.4 当前R967/R82语义身份逐批更新

每批在最终提交前核真实源字节，登记新增生产文件、已有文件变更、native接口与构建合同的角色和理由，生成与该批实现一致的当前R967/R82身份。不得用旧173 PATCHSET替代本轮实施清单；五候选只覆盖五处局部修复，不能当作全部实际变更范围。[S06]

已有NUMERIC/BUILD_CONTRACT等角色按原政策保留；只有经过明确审查及验证的语义变更才能改角色，不能把报告或调度代码无依据降级为PRESENTATION逃避数值失效。保留动态依赖审核、PIT/ABI与既有资格失效政策；旧训练身份、封印和原件不改成当前身份。

语义identity的更新同样有外部journal、完整before/after和恢复冲突检查。构建前提交干净source；构建期间不修改源文件或语义身份。最终APK SHA及source commit/tree通过生成资产与外部回执按现有时序绑定，不把最终APK SHA反向写进APK造成自引用。

### 2.5 新case catalog与真实runner

本轮矩阵是验收合同，不是现成可执行测试清单。B00必须建立正式case ID、所属issue/batch、验收层级及runner的映射规范和驱动，并完成本批全部适用case的真实执行器。后续批次的合同先登记，尚未实现的执行器明确标记待实施，在该批签收前补齐；不得把未来批次全部业务实现反设为B00前置，也不得将当前批次缺runner的case跳过后签通过。设备、长时间及自然质量的非工程项目保留独立状态，不能由host runner签收。[S07]

每个runner规范至少包含：明确argv、受控inputFiles及hash、有界timeout、实际生产类/方法或隔离的生产路径、唯一executionId、source/plan/APK作用域、stdout/stderr原件、实际输出与必要数据库读回。执行上下文与输出manifest必须相互核验。输入涉及target/cutoff/database时保存准确值；不适用时记录具体原因，不用空字符串隐藏身份。

runner必须真实调用被测生产实现并完成断言。单独打印`CASE_ENTER`、`CASE_ASSERTIONS_COMPLETE`、`assertionCount`或PASS JSON不能验收。主机stub、SQLite夹具、Android实际SQLite、JNI执行、签名release UI路径和真机自然运行分别标层级，不互相顶替。保留旧case与其原结果；新增case不能伪装成R343旧case，也不能把旧结果复制为本批通过。

### 2.6 CI、原签名与原211 APK接入

保留授权signing ref `construction/r27-20261028`、指定仓库、source祖先检查、允许来源集合、原signer preflight和其密钥隔离。新增本轮source来源应是明确受审查的允许项，绑定211祖先与本轮计划；必须用反例证明任意分支、任意commit、错plan pin仍不能使用signer。不得删除allowlist、拓宽为任意ref或复制签名cache/私钥到公开产物。[S08]

211工作流声明JDK17、Python3.12、Android platform36/build-tools36.0.0、NDK28.2.13676358、CMake3.22.1。施工时核实际可用性并补齐合法依赖；不是看到本地缺SDK就停止全部工程。CI独立恢复本轮sealed计划、准确211原APK以及需要的历史033/173原件，逐个核hash；不能改历史输入URL使其返回另一份同名APK。[S08][S09]

现有211构建检查器公开证书pin为`d8d8cee7f406267b2bf18fc9aef0c6ab760423cf205ccd6613dd44085bcc74e7`。这是源码检查常量，不能替代真实APK签名验证。原211 APK和本批签名APK都必须实际执行`apksigner verify --verbose --print-certs`并核证书相同、包名正确、版本递增；记录APK SHA和原工具输出。[S12]

报告中的版本/source身份不能替代原APK字节。无法取得准确原211 APK时，可继续不依赖其字节的实现与测试，但不得声称211覆盖安装通过；不得重新编译211后改名为baseline173或baseline211来填补原件缺口。

### 2.7 贯通包装、发布与下载读回

保留原真实Release/Debug/AndroidTest构建任务、干净source和前后不变验证、原Gradle日志及退出码。实际release APK唯一且可执行，APK成员/CRC/签名/版本/嵌入身份必须一致。编译失败时保存真实FAIL回执，不复用旧APK冒充新构建。[S10][S12]

封包必须包含准确source archive、可恢复git bundle、APK、构建/签名/工程原件、DELIVERY与成员哈希；source archive/tree和bundle可回查到同一source。发布索引绑定同一build run、plan、batch和APK；上传后从实际用户可用URL下载原字节，再验bytes/SHA/CRC、内层成员和身份。推送commit、生成下载链接或CI上传artifact本身不能代替下载读回。[S10][S13][S14]

## 3. B00正例、反例与签收

以下验收须有真实原件。编号属于本附约验收，不改变46项主问题集合。

|验收|正例|必须拒绝或保持真实未验的反例|
|---|---|---|
|B00-A01 源底座|准确211或逐文件审查的合法后代，构建字节与提交一致。|main旧应用树、173后态覆盖、错source/tree、未解释后代改动。|
|B00-A02 活动身份与继承|新活动repair全链一致，旧seal不变；旧入口在原输入下仍工作。|只新增binding但消费者仍当field173、多个冲突活动绑定、错plan pin、静默降级。|
|B00-A03 批次映射|B00–B11在版本、runner、CI、嵌入和DELIVERY中可一一回查。|仅改名旧B01、错stage、丢失repair batch、覆盖历史同名回执。|
|B00-A04 版本事务|prepare后/部分应用后中断可resume，四文件一致；重复resume不再涨版本。|任一文件用户编辑、旧journal套新批次、before/after不符、版本低于实际安装/发布值。|
|B00-A05 语义审查|本批真实改动和新角色已审查，当前R967/R82身份一致。|未登记新类、旧身份过期、无依据降角色、用旧PATCHSET或另一批语义回执。|
|B00-A06 签名与source准入|允许的本轮source/ref、准确计划和原signer进入构建。|任意ref/commit、错祖先、错计划、删allowlist、换签名或泄出密钥。|
|B00-A07 runner原件|真实生产路径断言，case集合、执行上下文、输入/输出和读回相互绑定。|只打印PASS、缺case、不同source/APK/批次原件、缺必需读回、超时却报通过。|
|B00-A08 211升级|准确原211→本批覆盖安装启动，原数据与引用读回；保留真实包SHA/签名/版本。|只验173→新包、重编译包冒充旧原件、卸载清数据后称无损升级。|
|B00-A09 本批报告|实际签名release的原UI入口导出，ZIP/index/manifest/SHA/CRC和SAF读回可核。|debug或旧B12结果替代，只有zip函数测试，导出source/APK不符，取消后挂死，失败检查被改绿。|
|B00-A10 完整交付|source、APK、build/run/case原件和DELIVERY一致，实际URL下载读回通过。|旧APK改名、只推源码/链接、上传后未核原字节、把package完成当真机或自然质量通过。|

B00工程签收必须覆盖适用的上述正反例，交付真实原签名APK及本批报告回归。准确211原件、授权signer或执行设备不可用时，B00保持具体受阻状态，不得填工程PASS；同时继续可独立完成的实现与测试，不把B00受阻误写为所有源码工作都不可进行。

B00不要求预先修完B01业务检查失败，也不要求原96检查全绿。只要原报告合同仍可交付，诊断中的真实READ_ERROR/UNKNOWN应保留。本轮构建入口的接入不能制造新的导出退化。用户真机、24/72h和自然质量单独签收，不是模拟器B00工程通过的隐含结论。

## 4. 每批同签名release的导出门

原211只在旧B12执行签名APK报告campaign，因此本轮必须新增每批执行门。每批测试对象必须是**实际交付的同一签名release APK**；安装后核其真实SHA及source身份，报告`source-manifest.json`对应同一包。FULL_RAW按原合同校验归档APK及原库覆盖。debug instrumentation仍可验证局部路径，但不得代替release导出。[S11]

### 4.1 每个可安装批次至少执行

- 从原UI入口创建报告，到实际作业终态、ZIP/index、内部清单、逐文件SHA/bytes及CRC，再经原SAF/分享入口保存并实际读回URI。
- 正常导出、连续重复导出、取消后重试、分享读回。验证没有新挂死、无静默缺项、无重复巨型快照失控；保留本次source/APK/snapshot/attempt与进程身份。
- 96检查编号、44指标及原外部catalog合同、附件、单ZIP/分卷、默认诊断/FULL_RAW、取消/重试和分享能力保留。失败/UNKNOWN仍可生成诚实可分享诊断，不新增“所有检查PASS”或`checksComplete=true`交付门。
- 报告分析、压缩、校验和传输不占用实时writer；同时记录预测与结算的时序影响，不能以报告成功掩盖主链被拖慢。

### 4.2 B01/B02及后续影响导出的修改执行专项10场景

专项10场景按子断言的实现归属签收，不改变原B01→B02依赖。B01必须验证同批签名APK的正常/重复导出、ZIP/index/分享读回、既有READY清理不退化、检查预算耗尽后的同快照续跑、取消真实退出、半成品不发布、低盘/fault满时诚实等待或失败且不递归放大/不删活引用原件。新增工作快照回收、历史失败快照退役、作业级恢复避免再次复制巨大快照、总盘/共享配额及fault真实排空属于B02；这些子断言在B01记录PENDING_B02并保留原件和交接引用，不阻B01工程交接，也不冒签通过。B02须用自己的实际签名release补验可回收占用/重复制受到约束、原件封存读回、真实pending下降及正常报告读回；B02以后的相关回归保留完整断言。不能把B01已负责的检查预算续跑或原成功导出/清理退化也延期。

|场景|必需观察|
|---|---|
|连续5次正常导出|每次ZIP/索引/回执可读，可合法退役的工作快照不按次数永久累加。|
|慢查询与共享依赖失败|96编号完整，原失败引用清楚，独立检查继续，诚实诊断仍可分享。|
|取消后重试|取消态明确，旧worker退出，新attempt独立，不覆盖旧失败原件。|
|写ZIP中途进程终止|不发布半成品；从持久阶段恢复或明确失败，避免重复复制大快照。|
|分享后进程退出|既有URI有效期和消费者读引用保留，不提前清理ZIP。|
|低盘/配额压力|不删活引用或唯一原件，不无限复制；阻断原因与空间恢复后的续跑可核。|
|fault队列满|B01保全原准入、可取消/诚实等待且遥测不递归放大；B02核真实封存/读回与pending下降，使用B02自己的签名APK关闭交接断言。|
|当前与历史scope|完整JSON可解析，snapshot/PID/boot/source/database身份可区分，历史不冒充当前。|
|5GB级大库|目标Android上实际测各项时延、内存、取消及主链影响；小夹具不能代替。|
|旧新模式与交付路径|默认诊断、FULL_RAW、单卷/分卷、外部SAF和内置分享均按原合同可用。|

完整实施要求以 `contracts/EXPORT_AND_STORAGE.md` 为准，特别保留：memo失败只在相同snapshot/参数/attempt作用域去重；不能跨fresh run永久缓存不可用。对外执行枚举仍为`EXECUTED/READ_ERROR/CANCELLED`；新内部进度不得破坏序列化。超时后须确认worker不再使用该连接，再复用或关闭，不能把`Future.cancel`当退出证明。

新增快照时钟回执不能反过来破坏旧job导出；没有可靠SQL pin时点与同域时钟时保持UNKNOWN。快照回收按profile及引用闭环：DIAGNOSTIC完成原请求、ZIP/清单/语义已验且无额外引用，可按原cleanRaw释放工作副本，不强加原5GB库或全PASS门；FULL_RAW或唯一独立原件须有同hash替代与读回。cleanup失败不倒退READY，分享仍在读时不删ZIP。

原诊断没有附带用户完整5GB SQLite，不能把解包报告视为已有全量重放输入。211CI模拟器配置8G磁盘，不保证能同时容纳原库、快照与完整原件包；测试前核设备和宿主的实际可用空间及峰值需求。大库原件或执行资源缺失时将该case保留未测，不能缩小夹具后仍签大库PASS。[S08][S16]

## 5. 升级保全与逐批交付循环

用户设备维持原包名、原签名及业务数据，采用合法更高版本覆盖安装。不得卸载应用、`pm clear`、强制降级、先清库后称升级成功，或把模拟器root/断网/卸载脚本应用到用户设备。专用模拟器可以为受控夹具执行上述隔离操作，但其证据必须标明disposable环境。[S11]

升级前后核关键数据与原件引用，包括既有策略/模型身份、已冻结记录、结算/研究任务、checkpoint/谱系、报告作业/恢复引用及已有有效图表读取；核验集合按原合同和本批涉及的真实owner确定，不能仅比较总行数或UID。不得为造“覆盖兼容”先清空未知数据。未知提交先读回原回执，再判定重试，避免重复学习或重复结算。

每批严格执行如下循环：

1. 读取本批卡与当前实际源码，明确工程前置、特定原件前置和现场观察前置；保持小补丁和可解释差异，保留原有效逻辑。
2. 完成实际实现及对应正例、反例、重复/中断恢复和旧功能保全；每个结果有真实生产方法、输入和输出来源。
3. 更新本批版本与当前语义身份，提交干净source；实际执行原签名构建并保留完整日志、退出码和前后身份核验。
4. 验APK包名、证书、版本、CRC/SHA与嵌入身份；核准确211→本批和实际前一交付批次→本批的适用升级路径，不以一种基线替代另一种。
5. 用同一签名release执行本节报告回归及本批设备工程case；发现导出退化先修复，再作为可用升级交付。
6. 组合source archive/bundle、APK、构建/签名/测试/报告原件、diff、迁移与可行恢复说明；授权范围内上传后实际下载核原字节和内层身份。
7. 回填本批与关联问题的实际状态，立即继续已满足输入门的工程。独立现场长测、原件待补或自然质量未完成保持真实状态，不拖住可独立执行的开发，也不降低门槛求完成。

每批机器可读回执必须覆盖：issue/batch、source commit/tree、计划commit/hash、升级来源、APK SHA、包名/版本/证书、build run、实际命令、输入/输出hash、工程/Android/真机/自然层级、阻断和下一动作、下载读回。该回执格式由B00实现并以正反例验证，不能只有手写总PASS。

回退说明必须与实际数据库/原件兼容性一致。旧APK降级安装或恢复过时数据库不是默认安全回退；涉及schema时应给出经验证的兼容和无损恢复路径，并保留新的真实业务原件。

## 6. 真实阻断与继续施工规则

|实际阻断|必须执行的处理|不得冒签的状态|
|---|---|---|
|旧工具不识别R34.4/B00|这是本附约要求实施的兼容工作，按上述合同补齐；保存错误与回归，不以“旧工具不支持”无限停止。|不能删身份/签名门、冒用旧batch或仅改文件名后称接入完成。|
|SDK、依赖或网络缺失|核锁定工具链、合法缓存/安装与现有CI；修可自行修复的环境，继续独立实现。|未执行Gradle不能写PASS；旧APK、unsigned或debug不能冒充原签名release。|
|签名cache或受保护工作流权限缺失|使用现有合法授权通道；保存具体失败、source与原件，说明解除所需能力。|不换钥匙、不移出私钥、不删除授权ref或source允许列表。|
|准确211APK、原DB或其他必需原件缺失|查已授权原发布/设备/完整包；列期望hash/位置与受影响case，继续独立工程。|不由摘要重造原件，不以重编译APK冒充原安装包，不将缺证验收写通过。|
|用户设备不可接入|专用模拟器实际完成可执行工程，交付准确APK与操作步骤，真机单独待验。|模拟器、debug instrumentation和host测试不能写用户真机PASS。|
|24/72h或自然观察未完成|保持可持续运行版本和原始监测，记录开始/截止/实际样本，继续独立工程。|不能压缩时间、移植旧证据、用合成数据冒签现场或降低资格门。|
|自动审批或访问控制明确拒绝|停止该具体被拒动作，采用实质更安全且合法的替代；记录被拒操作与原因。|不能换shell、接口或脚本反复绕过同一拒绝。|

用户对施工和上传已授权的范围不重复请示。只有真正无法自行取得的必需输入/权限才明确说明缺项、影响、已完成部分和下一动作；不得把一个路径的阻断扩散成全工程停工。禁止用重复提交、无变化重跑CI、盲目延长超时替代定位和修复。

最终交付必须分别说明：方案/候选静态验证、源码实现、真实Android构建、签名APK、模拟器/真机运行、报告导出、长时间连续性与自然质量。任何一层未执行均保留未执行或具体阻断；本附约文件存在、仓库上传成功和CI启动成功都不表示软件返修完成。

## 7. 已核验源码与合同来源

以下链接固定到实际核查commit。源码事实对应211，不引用浮动main。旧公开方案链接仅作为本轮文本结构的历史核查来源；当前施工计划pin以最终完整发布为准。

- **S01**：[211 release_manifest.json](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/release/release_manifest.json)
- **S02**：[已核公开版 CODEX_EXECUTE.md](https://github.com/gtt46461-star/starcore-public-build/blob/9558b6ddbcf9691b2e978bc76b4809fe71163cde/construction/plans/STARCORE_R34_4_REPAIR_20261128/CODEX_EXECUTE.md)
- **S03**：[211 tools/r343/build_identity.py](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/tools/r343/build_identity.py)
- **S04**：[211 tools/r343/prepare_field_batch.py](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/tools/r343/prepare_field_batch.py)
- **S05**：[211 tools/r33/verify_plan_binding.py](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/tools/r33/verify_plan_binding.py)
- **S06**：[211 tools/r343/rebind_semantic_inputs.py](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/tools/r343/rebind_semantic_inputs.py)
- **S07**：[211 tools/r343/field_delivery_contract.py](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/tools/r343/field_delivery_contract.py)
- **S08**：[211 .github/workflows/build-apk.yml](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/.github/workflows/build-apk.yml)
- **S09**：[211 tools/r342/ci_plan_inputs.sh](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/tools/r342/ci_plan_inputs.sh)
- **S10**：[211 tools/r33/package_ci_candidate.py](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/tools/r33/package_ci_candidate.py)
- **S11**：[211 tools/r342/ci_candidate_tests.py](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/tools/r342/ci_candidate_tests.py)
- **S12**：[211 tools/r33/run_release_build.py](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/tools/r33/run_release_build.py)
- **S13**：[211 tools/r342/release_delivery.py](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/tools/r342/release_delivery.py)
- **S14**：[211 tools/r341/publish_delivery_manifest.py](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/tools/r341/publish_delivery_manifest.py)
- **S15**：[211 tools/r33/advance_release_version.py](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/tools/r33/advance_release_version.py)
- **S16**：[已核报告与存储合同](https://github.com/gtt46461-star/starcore-public-build/blob/9558b6ddbcf9691b2e978bc76b4809fe71163cde/construction/plans/STARCORE_R34_4_REPAIR_20261128/contracts/EXPORT_AND_STORAGE.md)；[已核本轮验收矩阵结构](https://github.com/gtt46461-star/starcore-public-build/blob/9558b6ddbcf9691b2e978bc76b4809fe71163cde/construction/plans/STARCORE_R34_4_REPAIR_20261128/ACCEPTANCE_MATRIX.json)

本附约仅增列实际施工要求与验收门。新工具、APK、设备执行和发布结果须由后续真实施工原件证明，不能凭本文件预先签收。

## 修订2初始化执行要求

以CODEX_EXECUTE第3节的单一Bash执行单元开工：外部预期commit、HEAD、Git对象与真实工作字节一致后才执行计划Python；git status不能代替字节证明。保留用户改动，不自动reset/clean。新基线与施工树关闭继承的稀疏检出后再验完整源。验证输出为计划/source外的新文件，缺必需清单或任何失败必须停下该初始化段。PENDING_B02仅为施工台账的分阶段子断言，不新增应用catalog枚举。
