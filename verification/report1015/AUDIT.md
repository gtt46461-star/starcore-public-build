# 10月15日检测报告与 R78 修复验收记录

本记录对应原报告 `10.15检测报告.zip`、工程师文档 `01-10-14-.docx` 和 GitHub R77 源码基线 `35e2e72a369c48fd6b95740689aca3808d5105e0`。原报告内部清单45个成员的大小、SHA-256全部一致。工程师文档基于较早快照，且明确没有拿到完整源码；其中推断不能当成当前源码已经证实的根因。

**22项是去重后的修复、能力研究和验收工作项，不是22个均已证明的代码错误。实际完成9组产品代码改动及对应主机回归，不能写成22项全部闭环。真机长期运行、自然未来预测质量、数据库实际缩小和正式出手均未验收。**

## 排序与逐项状态

下表 Java 文件除注明外位于 `app/src/main/java/com/ethk3/novaonepro/`。列出的“涉及代码”包含待核实责任链；只有明确写出代码缺陷的行才表示已确认该处出错。

|序号|优先级与问题|报告事实、根因判断|涉及代码及处理|当前验收状态|
|---|---|---|---|---|
|1|P0 主链路延迟|最近200次请求到提交 P95=27103ms，目标5000ms。确认 motif 全排列相似距离在100个位置号码单元重复求解，且内层反复生成逆排列；这是一处实质冗余，不能认定它解释全部27秒。|`V51MotifMatrixProfileEngine.scoreSurface` 一次求解四个长度的共有距离；`V51MotifNumberExpert.evaluate` 复用整面。保留加法顺序和并列选择。|独立旧标量实现逐位、逐号码、逐比特一致；真机P95待验。|
|2|P0 漏预测与超时|最近30期缺4期、120期缺18期。SNAPSHOT_STREAM超时157次；SELECTION_POLICY、NUMBER_EXPERT等均有失败。不是单一错误码，也不能从计数认定最新全部仍在发生。|涉及 `V51RuntimeOrchestrator`、`BusinessReconcilerR36`、`DeferredEvidenceSnapshotR36`；第1、3、8项降低冗余开销。不得补造错过的盘前预测。|未证明漏期消除；需同设备连续120期验收。|
|3|P0 数据库增长与归档失效|DB=14,028,316,672字节、21,586,596行；修订表11,408,058行；归档报STALE_PAGE，压缩前后大小相同。确认重复相同开奖仍尝试写入；canonical 的REPLACE改变行身份，触发修订和归档代次工作。不能把这当成归档失败唯一根因。|`DrawCanonicalizerR811.upsert` 相同内容不写、变化UPDATE保留rowid；`DbStore.insertDrawInternal` 同业务数据先走已有元数据修复，避免ignored AUTOINCREMENT INSERT。`DataRetentionGovernorR900`、`RetentionSnapshotCacheR73`、native页拷贝仍需大库验收。|实际SQLite156条断言通过；重复写哨兵证明未尝试插入。14GB库缩小未验证，未删除用户数据。|
|4|P0 候选训练长期停滞|15个任务最老46.77小时，报告缺部分特征载荷，不能伪称复现手机原checkpoint。已用原5000开奖和各原header重建15份CAS，全部原hash/大小匹配。重算19800行后14个训练完成、1个合法拒绝。拒绝原因是单一条件特征导致3行全缺失，不是CAS损坏。|`r952/ResearchTrainingHookRegistryR952`、`DevelopmentProductionCP12`、`DevelopmentTrainerCP9.Dataset`；既有严格缺失门槛保留。第20项改变新候选特征结构。|原输入主机功能回放通过；真机调度速度和自动修复旧失败候选尚未闭环。|
|5|P1 在线训练outbox积压|112未完成，111PENDING+1RETRY_FAILED；不能把现代AST3子代训练与旧RECENCY_BLEND在线权重更新合并计数。|`r952/ResearchOutboxR952`、`ResearchTrainingHookRegistryR952.drainTrainingSlotCP11`。现有独立现代训练轮与旧outbox轮需在真实压力下分别测服务率。|沿用既有恢复代码的公共worker回归通过：13次合法数值训练、13次POST、崩溃租约恢复、P0打断恢复、坏候选隔离。未证明手机112条积压清零。|
|6|P1 POST反馈积压|325任务，最老11.87小时。确认原POST槽一次仅服务一个持久化单元；在原250ms量子内尚有余量也退出。|`r952/ResearchDirectorR952` POST槽改为最多16个独立提交单元，保留50ms余量、逐单元checkpoint和P0让行。无新增线程。|16上限、过期0提交、中断后恢复、已提交前缀不丢失、无RUNNING残留通过。手机积压消退待验。|
|7|P1 维护任务未完成|22个维护控制任务，最老created_at距今76.62小时。仅任务创建年龄不能证明一直卡死；必须结合desired/served期号、updated_at、租约与last_outcome判断落后。可能受实时压力、阶段预算、依赖阻塞共同影响。|`ResearchLearningContinuityR912`、`ResearchTaskQueueR967`、`FixedResearchWindowR932.maintainReviewed`、`RuntimeChainDiagnosisR975`及实际维护队列责任链。|公共校准维护120→重启→241期通过；这不等于报告22个维护任务均恢复。|
|8|P1 校准盘前捕获缺口|2695个reservation失败归因schedule gap；另有8个ACTIVE。确认同一次批量捕获对相同lookback重复获取PIT，放大读和内存预算。|`FrozenCandidateAdapterR958.captureReservedCP12` 批内同lookback共享一份不可变PIT，提前检查空队列，使用同一有界内存租约；所有目标期、盘前时间、模型、代次、事务检查仍逐模型执行。|8模型实际捕获、1次完整PIT读取、回调1次、内存归还；及时/过期/跨截止三类通过。自然缺口率待验。|
|9|P1 校准无可用模型|3736研究：3004 BLOCKED_INVALID_INPUT，732 COLLECTING，0READY；2695 schedule gap，309 strict-time reject。单靠降低门槛不能修复合法性。|`CandidateCalibrationR932`、`GlobalResearchScheduleR932`、`FixedResearchWindowR932`。|真实公共链路241次捕获、结算、1200动作审计，跨新JVM恢复并拟合一个模型。该合成模型仍FITTED_BUT_NOT_QUALIFIED；未伪造READY。|
|10|P1 矩阵输入未投影即过期|30条MATRIX_INPUT_EXPIRED_WITHOUT_PROJECTION。系统保留明确终止证据，没有冒充数值ACK；但输入未产生研究价值。|`r952/MatrixSnapshotBusR952.drainOneCP4/expireInputsCP12`、`MatrixSourceRouterCP12`。需区分READY饥饿、REPLAY_REQUIRED和QUARANTINED的具体分布及服务率。|待运行验收；未放宽过期、归档和ACK规则。|
|11|P2 矩阵运行覆盖不足|730注册spec覆盖13/20类型；187checkpoint只覆盖8类型。静态注册不是运行消费证据；并非每种矩阵都必须无依赖强行运行。|`MatrixRegistrationR75`、`r952/MatrixSourceRouterCP12`、`MatrixBindingsCP12`、`MatrixHistoricalDrawCP12`。|实际20类来源、投影和checkpoint共79条断言通过；矩阵绑定图消费29条、跨位置训练/实时重放一致8条通过。手机上的依赖和覆盖仍待验。另发现latestRow选择器需按版本协议进一步核对，未静默改变旧AST数值语义。|
|12|P2 绑定原子无准入证据|25350原子中25220 DISCOVERED，130 REQUIRES_BINDING；binding/admission为0。不能把130个绑定原子的问题说成25350全部不可用。|`r952/AtomAdmissionCP12`、`MatrixBindingsCP12`、`AtomAdmissionAuditR932`。|新候选已真实消费普通注册原子；130绑定原子未全部验收。|
|13|P2 家族创建与教师依赖|60提案、54注册；BANDIT/EXPERT/ENSEMBLE等待同位置、精确冻结教师。4200分子资产数量不等于训练完成数量。|`r952/ModernFamilyFactoryCP12`、`ModernSeedFactoryCP12`、`r952/EvolutionTeachersCP12`。|多尺度新图创建/编译/注册通过；教师家族不能拿未冻结或错位置模型充数，待完整教师证据。|
|14|P2 动态进化反馈闭环|39已开发现代候选与15训练候选，不等于已经证明代代更强。冻结AST3通过子代重新训练迭代，不能把旧模型在线更新数当现代进化。训练特征覆盖失败目前仍进入通用错误隔离；新增DEV覆盖诊断入口已接通已有RESEARCH/EVOLVE/TRAIN：只接受精确缺失失败码，核验全部原特征哈希和prefix，移除训练期全缺失的输出，追加多尺度输入，注册不同结构的子代；不篡改父代或继承Future。|`ResearchFeedbackCP12`、`CalibrationRepairR75`、`r952/MutationGraphCP12`、`ResearchDirectorR952.failCandidateTaskCP12`。需为合法特征覆盖失败建立独立诊断和修复提案，保留真实原始失败，避免和损坏输入混用、无限重试。|原报告真实失败候选→公共RESEARCH诊断→EVOLVE注册→独立训练→冻结通过18条断言；P0中断和重复恢复通过；损坏原特征拒绝通过4条断言。后续自然Future比较仍待真实数据。|
|15|P2 创造算法质量与准入|固定主链路影子3421结算、1043命中=30.488%；它只说明固定策略，不是创造算法总胜率。尚无足够证据证明新候选稳定超越基线。|`r952/CertificateEvaluatorR952`、`FixedWindowCertificatePublisherR932`、`CandidateCalibrationR932`。|不可把DEV或重放写成自然Future。未生成新的合格证书。|
|16|P2 主链路正式使用|正式plan、policy、paid claim、window、certificate、contribution、trades均0；报告NO_CERTIFICATE/MODEL_NOT_PROVEN。源码有精确证书统一选择路线；旧ResearchMainlineGovernor facade已退休，不应把它重新并联成第二授权者。|`UnifiedResearchChoiceR932`→`CertifiedPredictionR974`→`V51CompiledActionContract`→`DecisionGateEngine`/`V51CandidateRealGateR915`→唯一写入者。|合成资格夹具驱动实际发布器、统一选择、唯一决策门/写入者，连续3期授权→入库→结算通过；撤销竞态、开奖竞态和重复拦截通过。这证明下游代码可执行，不是手机自然产生了合格资格；尚未达到用户要求的自然创造算法真实出手。|
|17|P2 消融、冲突与贡献证据|没有足够正式对照和贡献证据；方法存在或静态调用图可达不能证明生效。|`r952/MatrixAttributionCP12`、`CandidateDiversityContributionR930`、`ResearchMainlineContributionAuditR922`。|待固定协议成对比较；不得把相关多周期预测加成独立样本。|
|18|P3 批次1–9工程验收未关闭|1832要求中1770未检查、62已改待验；299卡片中296未检查、3已改待验；0已验证。工程标签不是所有功能未实现的直接证据。|`construction/r997/REQUIREMENTS_LEDGER.csv.gz`、`CARD_LEDGER.csv.gz`、`SUBITEMS.csv`及 `scripts/r997_construction_assets.py`。|保留原要求文本和状态；本次不能批量改为已验证。|
|19|P0 新发现构建身份陈旧|已复现：生成类hash变化但未重编译的消费者仍保留旧Java编译期常量，形成运行身份与当前资源混用；同类风险还影响训练规则和校准身份。|`scripts/r815_generate_provenance.py`、`scripts/r967_semantic_identity.py` 生成运行期final字段；完整重编译消费者。|独立两代javac复现旧缺陷/验证修复；54个单测包含当前训练身份一致性；13个DEV前驱精确允许，未知/重复/畸形拒绝。|
|20|能力：多维度、多尺度|旧learned factory只保留第一个父特征，实际模型常为1特征+1mask。新图保留最多32个父标量，再追加频率20/120/500、熵20/500、bounded遗漏；HYBRID保留交叉项。|`r952/ModernFamilyFactoryCP12.learnedGraphCP12`；新图独立hash、manifest、维度、训练及冻结，不修改已有模型和持久化AST。重复相同扩展NO_OP，防节点堆积。|用报告失败父图：1→7特征，14输入维度，声明500期历史；1320特征行、1200训练、120验证、冻结成功；Future=0，未继承父胜率。|
|21|能力：多周期探路|固定48/240/960窗口、1/2/4/8期ahead、等权、单位置K3，留出最后1200期；每个预测按原锚点生成后结算，避免事后挑最好周期。|`verification/report1015/probe_replay.py` 为研究工具，尚未冒称已经接入App。落地必须有独立PROBE角色、目标期、锚点、模型版本、冻结时间、结算、覆盖率和错失机会；仅已结算的先前探路残差能作为后续输入。|单期29.58%、四周期合流30.08%，Wilson95%下界27.56%；0.40阈值覆盖0。没有证明高概率，当前不晋升生产功能。|
|22|能力研究：多位置多号码交集|找到现有 `CrossPositionAssignmentResearchEngine.locationVote/kBestAssignmentSupport`、`CrossPositionPrimitiveCacheR815` 等底层能力；尚未确认它就是用户记忆中35%以上的完整历史配置和统计口径。|实际生产组件固定窗口20/120/500、位置1/K3、8条合法分配路径，留出1200期独立研究。不是对所有位置任一命中口径，不事后挑选位置。|平均频率27.92%，交集投票28.50%，合法分配29.75%；不能声称复现35%。按用户“无用就不强加”的要求，不把未证明有效的组件堆入生产图。|

## 已完成的9组代码改动

1. 共有motif距离整面计算，保持旧输出逐比特一致。
2. 相同开奖与canonical幂等写入，保留rowid和first_seen，冲突仍硬拦截。
3. 保留逐模型证据边界的批内PIT复用。
4. 原250ms POST量子内服务多个独立单元，P0随时可打断。
5. 精确复核R65–R77的13个DEV兼容前驱；不迁移证书、校准或Future资格。
6. 生成构建/训练身份防Java常量陈旧；完整重编译并核对消费者。
7. 新候选的多特征、多尺度生成，结构幂等，有界节点，独立训练冻结。
8. 原失败特征覆盖诊断→持久修复提案→公共进化注册→独立重训冻结；原失败不擦除，父模型继续隔离，新子代Future=0。
9. 同一状态入口优先显示现代候选与实际训练/冻结/校准/资格计数；保留并明确标注历史档案。缺表显示UNKNOWN，报告读取仍限定自身快照。

以上9组均有主机回归；“主机通过”不是“手机所有问题解决”。没有把旧候选缺失值填0，没有重写旧模型数学含义，没有设置PROVEN=true，没有扩充线程抢主链路，也没有以5分钟游戏掩盖性能问题。

## 四个能力目前真实走到哪一步

资产层（注册原子、分子、类型和矩阵证据）→创造层（不可变AST、结构去重、家族/变异/融合）→训练层（盘前历史边界、原始CAS、分片特征、数值checkpoint、验证、冻结）→反馈与准入层（逐期合法捕获结算、校准、固定Future、对照、证书、统一主链路选择）。

本次已对新图的创造→注册→特征→训练→冻结及公共捕获→结算→重启→校准做真实代码验收。缺失的闭环不能用语言补足：训练覆盖失败自动修复现已通过原报告候选的主机验收；全部矩阵消费、自然Future合格及正式主链路出手仍未达到验收条件。“每代必然更强”也不能由代码保证，正确机制是固定对照后拒绝退化子代、保留合格父代，再测试新假设。

## 一分钟还是五分钟

当前证据不能认定“一分钟游戏本身不够”。P95为27.1秒已经超过软件自身5秒目标；重复计算、重复写放大、后台服务率不足及错过盘前窗口是工程问题。换五分钟可以给计算更多时间，但不会修好幂等、身份、归档或质量问题。先在相同设备测请求到提交、所有阶段P95/最大值、漏期率、后台每分钟产消率、训练游标推进和内存，再决定是否支持另一种期长；不同游戏与期号协议的数据不能混用。

## 验收证据及限制

- Gradle完整Java重编译及54个单测：0失败、0错误。
- 实际Gradle产品类（不覆盖产品类）+真实SQLite传输：8组owner回归通过。
- 原15份CAS输入hash/字节数完全匹配，19800行特征重算；14完成、1正确拒绝全特征缺失。
- 多尺度新候选12条断言：注册、1320行、数值训练、冻结、Future=0、无伪证书/交易。
- 校准120期后换新JVM继续至241期；计数、模型身份、1200动作审计、一次拟合、幂等维护通过。数据为隔离合成夹具，不是241期真实线上结果。
- 13个DEV前驱正常接受；14个、重复、畸形输入分别拒绝；产品字节码未被测试替身覆盖。
- 原签名材料及最终APK证书与R77 APK一致；本轮最终APK已通过正常发布构建和包内来源检查，详见下方交付收据。
- 追加7组公共恢复与证书主链路回归全部通过；证书测试显式插入合成资格，不得混入报告或手机证据。
- 当前无手机、adb或模拟器；不能声称已做真机安装、14GB原库迁移、长期无卡顿或自然未来高胜率验收。

## 10月15日18:33截图补充

截图13,490冻结、6,539待结算、6,951已结算的统计来自旧R908算法预测表；697冻结、262结算来自旧R915创生表。源码责任点是RuntimeTruthSnapshotR906.researchSummary/researchSummarySnapshotCP4。它们不是现代AST3开发样本计数。旧待结算仍需按盘前执行证明、权威开奖、归档可取回性逐条核对；不能靠填标签冒充训练，也不能把旧档案清空冒充已恢复。第9组修正已在同一页面优先显示当前候选、历史训练完成/处理中/失败、逐期训练和POST待办、校准和正式证书/出手收据，并保留历史记录。

追加归档验收：未覆盖产品类，实际JNI页快照、SCROW、代次CAS、取消、原库不被意外删除、5000期绘图尾部等共109条断言通过。夹具原库约151MB，不能据此声称手机14GB库已回收。

## R78发布包验收与交付

- 版本：`V57.21.2.0.78-R5.4.2.78-1015-O09`，versionCode `577251037`，包名 `com.ethk3.novaonepro`。
- 正常离线执行 `:app:testDebugUnitTest :app:lintRelease :app:assembleRelease`，没有跳过来源、语义、目录资产或施工账本门禁；2分40秒构建成功，54个单测全部通过。
- Lint没有新增问题；继承的原有基线仍过滤55条记录，另有3条旧基线记录未再命中。没有删除基线或把这个结果写成全项目零问题。
- 发布字节码3426个类全部位于APK的两个DEX内；除预期不同的BuildConfig外，3425个类与已回归测试的Gradle字节码逐文件哈希一致。
- 原签名v1/v2校验、ZIP全成员CRC、16KiB ZIP对齐、arm64-v8a/armeabi-v7a/x86_64三份原生库的ELF架构检查全部通过。
- 包内施工来源指向真实源码提交 `0d05243ac9cf1c4a347d90764ae5b77dfc781f03`，树 `0179017f281db9802214af13ea9c1554498f11f9`；1307个构建输入逐文件核对。训练规则与Java源码身份均出现在实际DEX中，静态调用索引大小和SHA-256一致。
- 交付文件 `STARCORE-R78-1015-O09.apk`，25,748,898字节；SHA-256：`da3ce66698ac29db7f71607ba8530a29618ab78bbca42efaec9e93beaa14c7a3`。
- 可重跑核验脚本：`verification/report1015/verify_apk.py`；公共证据：`evidence/apk-release.json`、`evidence/gradle-release.log`、`evidence/release-bytecode-comparison.json`。

这是已构建、已签名、已做上述主机与包内检查的修复候选包。没有用户设备运行环境，因此手机积压消退、实际数据库回收、长期无漏期、自然Future资格及自然创造算法正式出手仍未验收；22项不能标记为全部完成。
