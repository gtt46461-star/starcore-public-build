# 四类证书适配域与唯一决策门兼容合同

## 1. 核验范围与禁止混用的含义

本文件核对应用基线 **577311152 / eb8f82a8bda8db22f1f122095217132f07053bcb** 的真实生产调用，不把类名、历史证书记录或测试中的合成证书当作当前现场已获准 REAL。当前代码存在四类明确的精确预测适配入口，最终都进入同一 `DecisionGateEngine` 和同一 canonical 提交链。现场是否有有效证书、是否完成当期事前冻结、是否满足用户开关和时效，必须另查运行原件。

四类适配入口不是四条可独立下注的主链。`CertifiedPredictionR974.prepare` 按已冻结候选的身份分派；不能用一种证书域的样本、门槛或校准结果替另一域发证。下列数值是用于审查差异的摘要；**完整合同是本文件第7节所列固定提交源码中的发行、读取、依赖、适配和最终授权方法的合取**，任何摘要都不授权省略其它门。

## 2. 生产适配域总表

| 适配域及身份 | 当前质量合同 | 原生产适配与复核 | 保留边界 |
|---|---|---|---|
| R27 typed；`R31-R27-*` | `R27_GATE_V1`，精确 `policyHash`、`thresholdContractHash`、qualification binding | `MainlineFreezeProofR31` → `MainlineAdapterR31` → `CertifiedPredictionR974` | 只接受当前版本、ACTIVE且未过期证书；重算原门；保留原冻结K3、校准、来源revision/schema与selection fence；`allows`还要求用户真实开关开启 |
| SU1；`SU1-F-*` | `CertificateSU1.POLICY`：`SU1_CERT_V1\|FIXED_PAIRED_CALENDAR\|NULL_AND_PARENT\|CAL_120_1_120\|BRIER_ECE\|4_BLOCKS\|NULL_CONTROL\|RESOURCE_7\|EXPIRY_24H` 的哈希 | `CertificateSU1.read`、`MainlineAdapterSU1.authority/prepare/verifyFrozen`；`ResearchPlatformStoreR929.authorizeCurrent`有SU1专用桥 | 只能使用原mainline head、原runtime control合同、当前训练代码身份、精确artifact/selector/calibration/预测；不能将SU2的选择性覆盖门改写为SU1门 |
| SU2；`SU2-F-*` | 当前发行协议 `R26_PREREGISTERED_AGGREGATE_LCB_V3`；birth时预登记日历窗口、incumbent和family alpha | `CertificateSU2.read/maintainPolicy/issueWithin` → `MainlineAdapterSU2` → `RealAuthoritySU2` | 保留用户开关、原league选择、当期未结算ACT、完整事前freeze、未开奖与无重复REAL；不能凭旧`su2_certificate`行存在就准入 |
| R929/CP4精确模型；`R958-F-*` | CP3 canonical统计策略 + CP4冻结lifetime/identity + 资源/域/校准/精确选择合同 | `ResearchGatePlanR967`、`CertificateBundleServiceR929`、`ResearchPlatformStoreR929`、`FrozenCandidateAdapterR958`、`CertifiedPredictionR974` | 完整验证每条gate及hash、CP4 lifetime、当前bundle、精确模型/选择器/校准scope和消费域；不能拿早期CP3或legacy R929的一张PASS表绕过CP4 exact所需依赖 |

源码定位：`CertifiedPredictionR974.java:262–301`包含四个分派分支，`:358–390`逐域复核；`UnifiedResearchChoiceR932.java:108–179`准备候选与记录拒绝。现有优先顺序是先合格R958候选，再SU1，最后现有league中的R27或SU2；本方案保留这一选择所有权，不新增横跨不同统计合同的“挑最低门槛发证器”。更改排序必须是单独的、事前固定的选择合同变更并重新评价，不能在结果出现后择优。

## 3. 各域必须保留的质量条件

### 3.1 R27_GATE_V1

`CertificateGateR27.evaluate`逐项要求：gateVersion精确相同；序贯状态`QUALIFIED`；已结算FUTURE ACT至少250；ACT命中率至少0.34；ECE有限且在0至0.04；最差720块命中率至少0.26且不大于1；最长连败在0至6；语义ABI等价；相对I00、相对打乱标签对照的配对下界都严格大于0。证书有效时间常量为24小时。`choose`只在同一 `R27_GATE_V1` 内按原规则选择，不把其它统计合同的分数直接混排。

这些门还不等于当期可执行：`MainlineFreezeProofR31.certificate/read/allows`复核证书摘要、原门重算、原qualification/e-process绑定、模型原件、校准原件、语义指纹、来源版本和原事前预测；`MainlineAdapterR31`把该原冻结预测接入现有1200动作空间。该路径通过 `RealAuthoritySU2.USER_SWITCH` 读取同一个用户真实开关。开关默认关闭、缺原件、证书过期或模型/来源变更都不能由新适配器绕过。

R27的250是本域ACT门。120可能是其它合同中的校准段、首个观察端点或证书有效期，它不是跨所有域通用的发证门；250也不是跨所有域通用门。

### 3.2 SU1

`CertificateSU1.read`要求候选角色、FROZEN校准、未撤销且当前时刻在24小时有效期内；当前代码kernel与descriptor hash一致；主线调用还要求 `MAINLINE_ADAPTER` 模式的原合同和当前mainline head。`EvidenceQualitySU1.verifiedPass`分别验证主证据和baseline证据：原配对日历、原family/global/look预算重算、原半径和保守均值、严格正下界、PASS状态以及配对覆盖至少0.95。

`CertificateSU1.metrics`要求概率样本至少240；Brier有限、不高于0.24且严格好于同窗baseline Brier；ECE有限且不高于0.04；最长连败不超过30；恰好四块，每块样本至少30且命中率严格高于0.30。`CalibrationSU1.fit/valid/decode`保留120个以上拟合样本、时间隔离、120个以上验证样本、canonical编码、有限参数与原Brier/ECE门。`controlsComplete`要求同一family、同一起点与计划机会数的两个负对照类型都完成原FAIL证据；存在NULL_CONTROL的PASS则阻断。

资源证据要求7次运行、P95不高于100ms、原策略与kernel一致；`su1_runtime_program`还要求原kernel和coverage至少0.999。这个coverage是该域当前执行支持合同，不能当作SU2选择性ACT频率，也不能因SU2支持15%覆盖就把SU1的0.999直接降低。`MainlineAdapterSU1.verifyFrozen`继续验证原prediction、record hash、source generation、selector、calibration、精确K3和概率，不临时换一个预测借用证书。

### 3.3 SU2 当前 R26 V3

当前有效读取必须同时存在 `r26_certificate_protocol` 的 `R26_PREREGISTERED_AGGREGATE_LCB_V3`记录、未撤销、未过期、league成员、`CurrentPolicyR26.matches`和program hash一致。`QualificationWindowR26`固定 **69120个日历期**，每720期一个固定滑动端点；出生时登记窗口、incumbent和family alpha，不能事后移动窗口或重置预算。

`EvidenceBlockR26.evaluate`的当前聚合发行路径要求：没有pending label、没有日历缺失；比较路径没有benchmark缺失；ACT至少600；ACT/日历coverage至少0.15；ACT命中率至少0.35；使用原look alpha得到的Hoeffding下界严格大于0.30；四个日历块的最差ACT命中率至少0.28；相对同日历incumbent的配对收益差保守下界至少-0.01。原 `pairedTenths` 定义必须保留，不能用名字相近的纯命中差或旧helper公式替代。校准拟合和后段验证ACT各至少120且验证ECE有限、不高于0.04。`issueWithin`在原短事务内再次复核注册窗口、program、digest、原look、预算、聚合与校准后发证；有效期24小时。

`RealAuthoritySU2.allows/completeFreezeAndNoRealR30`还要求用户开关为1、尚未到deadline、当前league精确选中该成员、当期原ACT记录未结算、完整冻结投影/模型/selector/act-rule/deadline一致，并且不存在对应canonical REAL或旧trade。合格后仍须由唯一DecisionGate授权。选择性ABSTAIN不得被改成强制ACT，原incumbent也不能凭“成熟”免验。

### 3.4 CP3统计策略与CP4精确冻结域

`ResearchGatePlanR967.Policy`对 `CP3_PREQUENTIAL_EPROCESS_V1`只接受固定canonical JSON字节；`isCp3=true`。CP4在这个统计合同外增加冻结lifetime、identity、window binding与精确执行依赖。**CP3和CP4不是两套可随意互换的低门槛/高门槛参数**。旧generation继续读取其原冻结policy，不能把本轮新模板套回旧证据。固定计划端点是120、360、600、840、1200；当前CP4最早发证至少360，而不是首端点120。

`evaluateCp4/validatedCp4Cap`要求：FUTURE_N和PAIRED_N各至少360，独立窗口至少2；HR至少0.34；Wilson下界严格大于0.30；覆盖至少0.95；Brier不高于0.24且不劣于同窗常数0.30 baseline；ECE不高于0.04；最大连败不超过12；残差回撤不超过8；固定窗口最小命中率至少0.30；滚动20/50/100/300期门分别至少0.15/0.22/0.27/0.34。保留全局可求和lifetime alpha、当前log-e跨过原门、anytime-p门，不能只靠Wilson下界代替序贯证据。

其它必须一起通过的门包括：PIT/确定性/数值一致性；原域支持数在1至15、状态块支持与稳健性；漂移不高于0.1；至少50次资源测量、时延P95不高于700ms、heap增量不高于67108864字节；ATOM_BINDING_ADMISSION、预测相似性、误差相似性和多样性；相似度至少0.98且配对优势不大于0的重复候选不得放行。固定policy还保留校准120/隔离1/验证120、完整窗口、支持状态块样本50及命中0.27门、120期证书有效范围与120个新未来样本续期要求、原e-process备择及固定权重、缺测资源阻断规则。缺任一必需门、重复gate id、非有限观测或任一失败都不得获得cap。完整外部门仍以 `cp4ExternalGates`、`CandidateResourceEvidenceCP4`和其返回的原gate集合为准，不能只执行上述数值摘要。

消费还要通过 `CertifiedPredictionR974.certificateContract/exactCertificateScope/readExactCalibration/verifyCertificate`：gate hash、EXACT_SELECTION_SCOPE、EXACT_CALIBRATION_SCOPE、CP4 lifetime的exact scope、冻结AST和模型、原selection rule、独立校准数据、当前bundle与允许的目标域都必须一致。普通CP4证书发行器只给固定0.05的受控影响cap，不授予REAL；R974精确模型路径使用其单独绑定的完整预测recipe（源码为weight=1.0），再由最终门决定。两者是不同消费语义，不能以普通混合cap改写已经验证的精确模型，也不能拿0.05影响许可直接创建REAL。

## 4. 发行器不授权REAL，不等于最终门永远拒绝REAL

`ResearchGatePlanR967`的 `real_authority=false`，以及 `CertificateBundleServiceR929`的 `MAY_CREATE_REAL=false` / `CP4_CERTIFIED_FIXED_CAP_0_05_NO_REAL_AUTHORITY`，说明证书发行器和研究模块没有下注写入权。合法精确证书经R974复核后，仍可以由原最终门按完整条件授权。

实际生产时序如下：

1. `UnifiedResearchChoiceR932.freeze`在原owner内冻结一次选择及拒绝原件；`CertifiedPredictionR974.prepare`产生与该选择一致的精确proof。
2. `V51RuntimeOrchestrator`在279–290行调用 `CertifiedPredictionR974.evaluate`；合法proof并满足治理条件时形成 `CONTROLLED_PROBE / realEligible=true` 的编译合同。无合法proof则保留合法SHADOW基线。
3. `DecisionGateEngine.b12Reject`先查精确proof、固定K3、PIT；CONTROLLED_PROBE分支在11329行交给 `V51CandidateRealGateR915.evaluate`。该方法继续交给R974精确证书检查。SU2的原用户权限仍会重查。
4. `decideV51B12OriginalR30`在11273–11295行对通过的CONTROLLED_PROBE执行唯一授权并反向验签。它不是依赖后面的旧全局 `R35_REAL_AUTO_UNLOCK_ENABLED` 常量给所有域统一放行。因此不得通过全局修改该常量、所有artifact的PROVEN标志或删门来“接通”新模型。
5. `CertifiedPredictionR974.beforeInsert`及既有canonical事务在提交边界再检身份、deadline和重复；其中SU2继续调用 `Su2CommitAuthorityR30.verifyFrozen`。研究发行器、适配器和报表均不得新增第二个REAL写入入口。

`V51CandidateRealGateR915`的历史raw-score阈值是诊断常量；其不带精确proof的重载明确返回 `NO_QUALIFIED_MODEL_PROOF`。`CONTROLLED_PROBE`是原授权车道名称，并非第五类可绕过证书的候选算法。

## 5. 本轮适配与验收要求

- 保留四类域及原选择所有权；新增字段显式记录 `certificate_domain / protocol_or_gate_version / policy_hash / model_hash / selection_hash / calibration_hash / evidence_role / lifetime_or_epoch / target / expiry / source_revision / original_authorization_receipt`。部分字段在不同域的原件中位置不同，适配器应映射原身份，不能制造一个外观统一但丢失域语义的通用PASS。
- 每域至少一组隔离工程正例通过真实生产选择→prepare→最终DecisionGate→canonical→结算，以及过期、撤销、跨模型、跨目标、改校准、改来源版本、缺原件、无用户权限、重复目标、过deadline的对应反例。合成正例必须标为SYNTHETIC，绝不能写入自然质量分母。
- 缺证书时验证SHADOW仍持续；证书合格且可执行时验证原选择器实际考虑它或持久记录具体拒绝理由。拒绝可能源自另一个优先候选、用户开关、时效或域不符，不能全都诊断成“全局常量锁死”。
- 分别回放各原合同阈值的边界值和缺项，证明新增桥没有降低门、没有加一个跨域250/120门，也没有通过取四域最低值来发证。不能在不同域之间合并ACT、日历期、未来机会、校准样本或影响权重。
- 新模型、结构、数值状态、选择规则或源语义变化时沿用本域原失效/重新冻结规则；旧证书保留审计原件，不自动继承给下一代。

## 6. 对本轮问题判断的限制

本次确认的是固定152源码的存在、调用与门合同，没有在本环境执行四域设备正例，也没有证明现场当前存在任何有效证书。修复后台/训练链可以恢复产生候选和证据的能力；不能据此承诺必发证、必REAL或必然每代更强。无证书的正确拒绝不算功能故障；“具有合法证书仍因错误身份、冻结或调度而永远不被检查”才需要按原拒绝证据定位。

## 7. 固定提交的完整源码合同索引

以下路径相对仓库根；所有引用固定于本文件第1节提交。方法名用于定位，行号仅是该提交的辅助定位。实现修改后必须重新绑定source_after及文件哈希，不能复用旧行号冒充验收。

| 合同层 | 必读完整源码及方法 |
|---|---|
| 四类分派与提交重查 | `app/src/main/java/com/ethk3/novaonepro/CertifiedPredictionR974.java`：`prepare`、`certificateContract`、`exactCertificateScope`、`readExactCalibration`、`evaluate`、`verifyCertificate`、`beforeInsert` |
| 选择与统一验权 | `app/src/main/java/com/ethk3/novaonepro/UnifiedResearchChoiceR932.java`：`freeze`、`prepareChoiceCP12`、`selected`；`app/src/main/java/com/ethk3/novaonepro/ResearchPlatformStoreR929.java`：`authorizeCurrent` |
| R27质量与冻结 | `app/src/main/java/com/ethk3/novaonepro/strategyv2/CertificateGateR27.java`：`evaluate/contract/choose`；同目录 `SequentialQualificationR27.java`；`MainlineFreezeProofR31.java`：`certificate/freeze/read/allows`；`MainlineAdapterR31.java`：`prepare/verifyFrozen` |
| SU1质量与精确适配 | `app/src/main/java/com/ethk3/novaonepro/strategyv2/CertificateSU1.java`：`POLICY/metrics/controlsComplete/read/maintain`；同目录 `EvidenceQualitySU1.java`、`CalibrationSU1.java`、`MainlineAdapterSU1.java`、`MainlineHeadSU1.java`、`AlphaLedgerSU1.java` |
| SU2正式发行与权限 | `app/src/main/java/com/ethk3/novaonepro/strategyv2/CertificateSU2.java`：`read/preregister/maintainPolicy/issueWithin`；同目录 `QualificationWindowR26.java`、`EvidenceBlockR26.java`、`EvidenceAccumulatorR26.java`、`SelectiveEvidenceSU2.java`、`SelectiveCalibrationSU2.java`、`CurrentPolicyR26.java`、`MainlineAdapterSU2.java`、`MainlineBridgeSU2.java`、`RealAuthoritySU2.java`、`Su2CommitAuthorityR30.java` |
| CP3/CP4冻结与完整gate | `app/src/main/java/com/ethk3/novaonepro/ResearchGatePlanR967.java`：`CP3_POLICY_CANONICAL/Policy/policyForGeneration/forWindow/bindCp4/planCp4`；同包 `CertificateBundleServiceR929.java`：`evaluateCp4/validatedInfluenceCap/validatedCp4Cap/cp4ExternalGates`；`CandidateResourceEvidenceCP4.java`、`CertificateDependenciesR932.java`、`ResearchEvaluationRunR967.java`、`FrozenCandidateAdapterR958.java`、`CandidateCalibrationR932.java` |
| 唯一正式决策与canonical | `app/src/main/java/com/ethk3/novaonepro/V51RuntimeOrchestrator.java`：`runR36`及exact action stage；同包 `V51CandidateRealGateR915.java`：`evaluate/releaseBoundary/verifyAuthorized`；`DecisionGateEngine.java`：`decideV51B12/b12Reject/decideV51B12OriginalR30`；`V51AuthorizationCore.java`；`V51Repository.java` |

