# STARCORE R34.4-211 · 连续完成全部返修的施工指令

本文件是给具备仓库、Android 构建和实际验证能力的施工方的完整执行任务。资料已获用户明确授权完整上传；原始诊断、全部审计、46张问题卡、12批和5组候选均在本次完整发布范围。**本文件要求后续真实实施，不表示这些产品工程已经完成。**

## 1. 任务、权限和完成定义

请在 `gtt46461-star/starcore-public-build` 实际完成 STARCORE R34.4-211 升级返修。按本完整计划的全部46卡、B00—B11执行，将已有创造、数值训练、精确结算、反馈研究、结构变异、交叉融合、冻结发布、独立未来与唯一真实主链消费接通，同时保留原有有效逻辑、文件、数据与检测报告导出。用户已授权本轮施工及向该仓库提交；满足已有权限和真实发布门后持续交付，不重复询问是否开始或是否继续下一批。

这不是只补文档、只应用5个候选、只编译、只打印PASS的任务。每张卡必须实际实现并取证，或证明合法后代已有有效实现并回归保留，或列明具体外部缺件、受阻范围、已完成部分及下一动作。5个候选只是5处局部修复的起点，其余卡仍需落实。不得因没有现成补丁而跳过。

按依赖持续推进所有可执行工作，直到B11和逐卡遗漏检查完成。缺某个旧root、教师、自然窗口、原APK或设备时，只阻断依赖它的验收及发布；继续独立工程。真正缺权限或外部原件时准确报告，禁止绕过访问控制、虚造原件、虚报通过。工程完成、Android/迁移、真机连续运行、自然质量、可下载APK交付分别签收。

## 2. 固定资料与应用底座

- 完整计划路径：`construction/plans/STARCORE_R34_4_REPAIR_20261128_COMPLETE`。
- 原始完整包路径：`construction/packages/STARCORE_R34_4_211_20261128`，27个分片由脚本自动还原。原完整ZIP SHA256：`4770e865a14cb3c4411e24d180368077c4585cdaa40255945a72742d615f9915`。
- 精确211应用源码：`5c7f1b36eaee6036df280e85835bb0fb4ce0b40d`；原安装versionCode：`577311211`；原包名：`com.starcore.research.r13`。
- 原诊断外层ZIP SHA256：`e0e3ffdb5a58a33175d6ee305d788bfaea3bd6186495c1828a0e1edddb6112b0`。
- 计划所在提交只负责资料分发。**它不是211应用源码提交。** main中的旧应用目录不能覆盖211。若已有实际交付且逐文件核实的211后代，保留其合法改动，先核祖先关系、实现差异与现场版本，不能把现有软件倒回211再重做。

先将本次完整计划的准确commit固定到只读方案工作树，记录其MANIFEST SHA。原包中的旧 `PUBLICATION_RECEIPT.json` 和旧公开版描述是此前发布范围的历史记录，不是本次授权限制；当前入口及 `PUBLICATION_AUTHORIZATION.json` 才说明本轮分发范围。不要改原ZIP或旧封印以匹配新说明。

必须先读：README、CONSTRUCTION_PLAN、ISSUES.json及46张卡、BATCHES.json及12个批次、三份contracts、ACCEPTANCE_MATRIX、PATCHSET、CANDIDATES、SOURCE_ANCHORS、FILE_MODIFICATION_MATRIX、SELF_CHECK、coverage、inheritance，并读取本轮必执行附约 `B00_EXECUTION_ADDENDUM.md` 与 `CREATION_EXECUTION_ADDENDUM.md`。新附约补足执行链，不减少原卡验收或保全要求。

## 3. 可直接执行的资料检查入口

在完整方案的固定提交工作树中执行。`REPO_DIR`是该工作树；脚本不访问网络、不改应用。运行回执输出目录位于方案外，避免污染冻结清单。prepare_evidence是唯一的原件补齐步骤：按固定hash写回方案内evidence/ORIGINAL_DIAGNOSTIC_211.zip，已有不同字节会拒绝覆盖。

```bash
REPO_DIR="$(git rev-parse --show-toplevel)"
PLAN_DIR="$REPO_DIR/construction/plans/STARCORE_R34_4_REPAIR_20261128_COMPLETE"
PLAN_COMMIT="$(git -C "$REPO_DIR" rev-parse HEAD)"
EVIDENCE_DIR="$(mktemp -d -t starcore-r344-evidence-XXXXXX)"
python3 "$PLAN_DIR/tools/prepare_evidence.py" --repo-root "$REPO_DIR"
(cd "$PLAN_DIR" && sha256sum -c MANIFEST.sha256)
python3 "$PLAN_DIR/tools/verify_plan.py" --output "$EVIDENCE_DIR/plan-check.json"
python3 "$PLAN_DIR/tools/unpack_report.py" --output "$EVIDENCE_DIR/original-report"
```

这一步无source参数时只验证资料，`source_verification="NOT_RUN"`（reason为`SOURCE_ROOT_NOT_PROVIDED`）不能算源码通过。另建干净211只读基线用于原锚和候选检查，施工树单独建分支；不能在已经修改的施工树上硬跑“逐字节等于211”后修改验证器使它通过。

```bash
APP_BASE="5c7f1b36eaee6036df280e85835bb0fb4ce0b40d"
WORK_PARENT="$(mktemp -d -t starcore-r344-work-XXXXXX)"
BASELINE_DIR="$WORK_PARENT/baseline-211"
git -C "$REPO_DIR" cat-file -e "$APP_BASE^{commit}"
git -C "$REPO_DIR" worktree add --detach "$BASELINE_DIR" "$APP_BASE"
python3 "$PLAN_DIR/tools/verify_plan.py" --source-root "$BASELINE_DIR" --output "$EVIDENCE_DIR/baseline-check.json"
python3 "$PLAN_DIR/tools/check_candidates.py" --source-root "$BASELINE_DIR" --output "$EVIDENCE_DIR/candidate-check.json"
```

若本地缺准确commit，先从该仓库取回该对象并核身份。核实无更晚已交付后代后，以211新建独立施工分支和工作树；否则以已核后代施工。`check_candidates.py`只在临时副本累计应用5组补丁，不修改基线。它通过不代表Android产品通过。

完成初始化后，将本轮施工分支、干净基线、当前应用树、冻结计划、外部版本journal、构建/测试输出分开保存。源码和必要回执按仓库既有结构提交，原计划和其验收待定状态保持冻结；实际进度写新的执行台账。

## 4. B00必须先把本轮构建和交付工具真正接通

211仍包含173计划、旧B01—B12及field173绑定。当前计划为211、B00—B11。原 `ACCEPTANCE_MATRIX.json`顶层为`issues`，旧runner读取`cases`并限制R343 case ID；原CI仅旧B12执行签名报告campaign。不能只增加一个binding文件、把B00改叫旧B01或打印PASS来应付。

按B00附约实际实现增量兼容，贯通：当前计划pin→版本事务→总build identity→APK内身份→实际case/runner→CI构建回执→封包→发布索引→下载读回。保留旧173与其他历史入口和seal；原数学、ABI、PIT、资格门不变。若新增R34.4版本系列，所有解析/生成/消费方一致兼容；若保留既有APK系列，旧transport stage与本轮repair batch分字段精确映射，不覆盖旧批次原件。

版本推进继续使用既有仓库锁、四文件一致更新、外部journal、前后hash、崩溃续跑与用户编辑冲突保护。新versionCode高于实际源码、已安装和已发布的最高值，不预占577311212，不四处手改，不因重复resume重复加版本。

每批更新当前R967/R82语义身份，逐个登记新增生产文件角色和实际输入字节。原NUMERIC文件不得无证据降为PRESENTATION；不覆写历史训练身份，不把学习规则改变后的旧资格无条件迁移。

B00先完成46卡到执行合同的映射、case catalog/runner驱动基础设施及本批实际runner；后续批次在实现自身业务时补齐对应真实runner，不要求B01—B11业务先完成才能签收B00。所有实际验收关联真实输入、生产路径、timeout、输出、数据库读回、executionId和本批APK。既有runner机制可复用，不能把旧173回执改标签当本轮证据。B00提交新增工具实际路径、`--help`、实跑命令和正反例，不保留虚构CLI。

原签名workflow ref、仓库限定、来源祖先检查、允许列表和signer preflight保留。仅增加本轮准确源ref/计划pin/211基线准入，不允许任意提交调用签名，不搬出私钥、不换签名、不删门。缺权限或签名材料准确记阻断并继续独立工程。

## 5. 46卡与12批的完整工作表

|批次|卡数|必须形成的真实结果|
|---|---:|---|
|B00|2|正确211/合法后代、旧功能继承、可运行的本轮版本/语义/runner/CI/封包与交付绑定|
|B01|6|覆盖SQL、完整scope、typed共享依赖、独立预算、冻结时钟、真实可取消/重复的报告导出|
|B02|5|工作快照回收、磁盘总预算、fault热配额与可验证原件封存排空、启动journal|
|B03|4|精确RESOURCE_QUEUE分类、公平writer准入、冷启恢复、真实任务推进而非heartbeat|
|B04|7|实时抓取/推理/落库、下一期赛前窗口、163个LIVE_RETRY逐条处理、后台/缓存/内存保全|
|B05|5|逐pin/root的原件恢复与隔离、typed load、精确投影快路、合法ColdRebirth、连续数据新epoch|
|B06|2|已有TRAIN继任推进，特征→数值状态→验证tape→receipt→模型artifact→首次冻结|
|B07|4|真实结构创造、变异/交叉/融合、滚动学习、typed rule、冻结教师与失败研究action消费|
|B08|7|18研究图、20矩阵、29旧图、16逻辑表的真实生产/读取/消费，AtomNet、CPU与数值展示|
|B09|2|独立Future、校准/曝光/alpha永久账本、质量证书与唯一主链实际消费|
|B10|1|96检查、44指标、165对象、826表及1832需求/299旧卡的证据和状态汇总|
|B11|1|最终源码、真实签名APK、保数据升级、同APK导出、可恢复源码与下载重验交付|

按BATCHES依赖交接工程，不擅自改DAG求快，也不把长期自然验收加成所有工程的总锁。B06不等全部旧root；B07需要旧donor的个别路径单独等待；B08正式只读图可为PENDING_B09，不能反过来阻B09生成证据。B09自然消费需要B04真实PIT/唯一owner/精确结算工程条件，工程夹具不等72小时自然窗口。

## 6. 原有功能、文件和数据的保全

保留211的有效代码、1407生产Java基线清单、旧49项逐字节修复与15个后续合法差异，按46卡对应范围最小修改。不重构全部目录、不重复造新owner、不删旧类/有效图表/导出入口。卡外文件必须说明必要调用链、关联卡与源锚，增量审查后再改。

保留唯一REAL决策与结算owner、单位置K3、PIT cutoff、SHADOW/REAL分账、不可变冻结模型、证书与历史原件。不能删库、清失败队列、覆盖FAILED_TERMINAL、补造过去预测/冻结/结算、用新数据冒旧pin、把UNKNOWN/null/EMPTY/真实0互换。

资源等待继续走原协调器。只对精确原因、ADMISSION、ROLLED_BACK的已知writer忙做合法排队；UNKNOWN提交先查receipt，不能重学、重写或跳cursor。实时截止、fence、取消、盘容量与唯一原件背压继续生效；物理磁盘满时如实暂停相关写者。

## 7. 报告导出是每批发布门

原96检查、44指标、章节、附件、index/archive manifest、单ZIP/分卷、DIAGNOSTIC/FULL_RAW、取消/重试/SAF分享全部保留。检查有失败、UNKNOWN或业务未通过也必须能形成诚实诊断；不得以“全PASS”“checksComplete”“训练完成”作为导出前置。外部catalog仍为`EXECUTED / READ_ERROR / CANCELLED`，内部状态通过原兼容字段表达。

每个拟交付批次都用**该批真正签名release APK**走原UI入口→导出作业→ZIP/index→SAF保存→URI实际读回，验证CRC、成员长度/SHA、语义回执及source/APK/snapshot/attempt/boot/PID/database身份。debug、底层zip库测试、旧B12回执不能代替。至少执行正常导出、重复导出、取消后重试及分享后读回。

B01、B02及以后任何影响导出路径的变更，必须覆盖专项10组：连续5次正常导出；慢查询/共享依赖失败；取消后重试；写ZIP中进程终止；分享后进程退出；低盘/配额；fault满；当前/历史scope；原大库实际执行；新旧profile与单/分卷/SAF兼容。原5GB库缺失就该项标未测，不能拿小夹具冒充大库验证。

依赖缓存只在同snapshot、版本、真实参数与attempt范围内复用typed成功/失败；新fresh run可重试，不永久缓存UNAVAILABLE。取消后确认当前runner真正退出、Cursor关闭再继续，不把cancel调用等同退出。报告读取/压缩/传输不占实时writer。

工作快照清理按job/profile/manifest/语义回执/pin逐一证明。DIAGNOSTIC不强制留完整5GB原库直到全检查PASS；FULL_RAW、独有pin和唯一原件没有合法替代证明就不能删。READY/FAILED/取消各自处理，不把所有现场快照假定为同一故障。

## 8. 独立开发必须能起步，首次冻结不得循环等待

R26旧家族/原件、R27固定回放/进化、AST3现代开发/发布各走自己的真实输入门。旧pin续跑要原pin；有cursor的旧ColdRebirth要原state/hash；旧TRAIN恢复要精确任务身份与原.scdev。缺失只隔离对应旧对象。

新基础实验通过现有RuntimeCoordinator→ResearchLearningContinuity→ResearchDirector IMPORT/TRAIN→ModernFamilyFactory/DevelopmentProduction开工。只要自己的canonical历史、catalog、特征/ABI、协议与资源合法，不等待REAL、证书、所有旧root、18张正式图或长期摘要齐全。

必须执行REAL=0、证书=0、旧期全NO_PREDRAW、无关旧checkpoint全部缺失但合法历史充足的正例：fixed graph和至少一个无需教师的学习内核完成DEV；rolling无COMPLETED时保持0，不造事件。首次`FREEZE_CANDIDATE`沿现有development freeze入口按自己的产物和截止推进，不能额外等rolling event/version或自然证书；`R343_ROLLING_FREEZE`是另一条后续路径。冻结不自动授予REAL资格。

## 9. 必须发生真实数值训练

先推进已经存在的attempt0恢复继任，逐段检查子槽/claim、输入解码、feature page、数值checkpoint、commit、receipt、artifact及冻结入队。保留原六槽/五子槽、prepared身份、commitKey/payloadHash、owner/fence和cursor。已有1100特征不等于模型完成。

每个完成对象可串回candidate/AST、kernel/feature ABI、input manifest、训练/验证边界、normalizer、数值状态、验证tape、开发receipt和模型原件。fixed+一个learned只是首次贯通里程碑，最终须验证每个已启用ModelKind，不通过不能关闭它来缩小范围：

|ModelKind|必须保留并实际验证|
|---|---|
|FIXED_GRAPH|typed PIT图前向、概率、tape、身份和回执；不伪造梯度步|
|SOFTMAX_LINEAR|训练集标准化、交叉熵、权重/偏置更新与恢复|
|OVR_LOGISTIC|逐类逻辑模型更新、维度/概率合同与恢复|
|HISTOGRAM_TREE|分位候选、Gini切分、叶概率、容量/深度与恢复|
|MLP_D32_16_10|隐藏层前向/反向、Adam状态、有限值及checkpoint恢复|
|EXPERT_HEDGE|真实冻结教师、目标对齐、专家概率、权重更新与恢复|

保留CP9训练/验证/embargo、只用训练集拟合标准化、有限步预算和停止规则。历史行数不冒充连续期数；短窗家族不强等最长窗。教师缺失只等待对应分支，工程夹具验证实现但不进入用户自然质量账本。中断恢复必须与无中断结果在既定数值合同下相符，不能丢优化器状态后称无损。

## 10. 创造、变异、交叉、融合与反馈都要有可消费产物

沿已有MutationGraph、EvolutionController、ResearchDirector、TypedGenerationRule、CreationChangeKind和失败action链推进。一次提议持久化exact proposal，固定父代、教师、roster、seed；恢复不得换父代或重采更高分。typed compile→固定输入/协议→TRAIN→开发产物→独立冻结→后续资格/消费都有原件引用。

参数/窗口变化、结构变异、交叉、融合、机制扩展、NO_OP分别记录。改名、复制父代、同构重排、清40个seed variant游标不算持续创造。交叉实际组合兼容子图；融合实际消费不同冻结父模型/子策略输出。使用已有有界类型化图，不动态执行任意生成Java，不绕过ABI与未来隔离。

至少一个真实结构子代完成编译、训练/开发评分并追到父代和修改位置；每种已有操作有工程正例及必要拒绝例。失败、负收益和淘汰允许如实出现，不强制每个子代变强。typed rule、donor proof、pair尝试与未知值按原合同处理，值对象不新增数据库写者。

RollingLearner只消费合法COMPLETED和自己的开发模型，REAL可为0；同issue/hash/receipt不二次学习，异hash同issue拒绝。新学习头与旧不可变冻结版本/未来窗口分开；不能重写旧预测。失败研究action必须被proposal/child/TRAIN真正消费，入队、图表刷新、heartbeat或表行数不代替反馈学习。

## 11. 图表、矩阵与自然质量必须保持真实

18研究图、20矩阵、29旧图、16逻辑表逐对象核生产owner、真实输入、计算、原件、读取和消费。82能力可调用不等于实际生产。BORN/ACTION原hash、帧COMMITTED、R06独立生产者保留；R11/R14/R18正式读层不得回流训练。AtomNet、CPU、校准和自然父子claim各由真实owner出原件，不能借rolling或计数替代。

开发历史分、120行验证early-stop、SELECT/CONFIRM与Future分别履行原角色。冻结时点、校准身份、曝光/alpha账本及永久失败记录保留。不得为提高出手频率降低证书门槛、移植资格、补写旧冻结或重置账本；工程目标不是保证未来获利。

工程合法夹具只签`ENGINEERING_PATH_VERIFIED`，自然不足/不达标保持`NOT_PROVEN`。出现合法自然资格时，核同frozen/model/protocol/certificate由唯一主链在下一合法边界实际决策并精确结算，不止于证书或榜单。

## 12. 每批必须完成的实际交付循环

1. 对照本批每卡、当前源码与前批差异，核输入门和保全项；只改必要文件，记录原owner/方法及before/after hash。
2. 实现缺口，执行风险对应的正例、反例、重复提交/中断恢复及旧功能回归。主机、Android SQLite、JNI、模拟器和真机分别记录；stub不得冒充生产全路径。
3. 贯通本批语义/版本事务，提交干净source；沿B00已适配且有权限的原签名链构建，保存原日志/退出码、source commit/tree、计划pin和构建前后不变证明。
4. 核真实APK的包名、versionCode/name、原证书、bytes/SHA/CRC及嵌入身份；版本高于当前安装/已发布最高值。
5. 每个拟独立交付批次，在隔离验证环境分别验准确211→本批、实际前一交付批次→本批的适用保数据升级路径；不得让用户设备反复降级来造升级起点。不存在原211APK不能重编或改名173冒充。用户设备不得卸载、清数据、强降级、root或执行CI破坏性清理；此类夹具只在专用模拟器。
6. 按第7节在本批签名release上实际导出、保存和读回。报告不能导出或发生数据/身份退化就修复并重验后再发布该批；不能交付一个已知不保全的APK。
7. 整理source archive、可恢复git bundle、APK、构建/签名/测试/报告原件、差异、迁移与可行回退说明，上传到授权仓库/既有交付入口。默认回退不能是降版本或恢复过时数据库；说明兼容边界及前向修复方案。
8. 从真实发布URL重新下载并核字节数、SHA、CRC与身份。登记commit、CI run、APK版本/签名/SHA、URL、下载回执和剩余未验，再推进下一批。

没有某外部能力时可记录未签名构建或主机验证作阶段诊断，不能叫正式可覆盖安装APK。受阻发布不妨碍隔离分支上的独立后续工程；相关最终产品验收必须仍标未完成。

## 13. 台账与最后的严格自检

复制 `EXECUTION_LEDGER_TEMPLATE.json` 到新的执行输出目录使用，勿修改冻结模板/计划矩阵冒签通过。每卡记录实际源commit、输入/输出hash、实现或保留理由、变更文件、真实命令/退出码、证据路径、反例、各验收层结果、阻断和下一动作；每批记录前一APK、本批source/tree/plan/版本/证书/build run/报告和下载读回。

第7节报告专项10组与CREATION_EXECUTION_ADDENDUM的E01—E10是两套不同验收，必须分别逐例记录执行身份、输入输出、原件和验收层级，不能相互替代。

每批对照46卡集合做遗漏检查；最终必须46卡都有处置，12批有对应交付/明确阻断。96检查、44指标、165对象、826表、1832需求、299历史卡保持原集合与原状态，不删项、不批量PASS，不把本轮46卡等同所有旧需求全部验完。

最后至少逐项回答：

- 实际改了哪些文件、为什么；哪些原实现保留；哪些候选调整/未用及依据？
- 每个批次和卡实际完成到哪一验收层？有哪些未跑、失败、外部阻断或自然待证？
- 创造→typed编译→TRAIN→artifact→首次冻结、COMPLETED→rolling→反馈action、自然资格→唯一主链消费，哪些路径已有真实原件？
- 每个已交付APK是否由准确源码和原签名构建，覆盖安装保数据，且同APK报告能反复导出和分享读回？
- 有无把UNKNOWN改0、清队列、覆旧seal、跳过case/签名门、重置variant/资格、冒用旧APK或回执？出现则修正后重验。
- 最终source commit、APK与源码包下载链接、SHA、版本、签名、CI与验收原件在哪里？下载回读是否一致？

最终回复先给真实完成范围、源码与APK入口，再逐项列未完成和原因。缺证据不写“全部完成”；只完成计划或五候选检查不得结束为产品交付。持续完成所有现有权限、原件和设备条件下可执行的施工，保留可复核的事实边界。
