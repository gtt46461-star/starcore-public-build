package com.ethk3.novaonepro;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * DecisionGateEngine
 *
 * <p>V20 满级诊断 + 智能审判闭环 · 最终裁决门满血版（第14/14）
 *
 * <p>本次重点修复： 1. prepareCandidates() 中 raw.copy() 可能丢失 TeacherNumberMatrixEngine 写入的
 * teacherMatrix/teacherNumber/V20 动态字段。 2. 补齐 copyRuntimeBridgeFields()，把 raw ComboCandidate 的
 * public 字段完整桥接到 copy 后候选。 3. fillTeacherMatrixContext() 同时读取 teacherMatrix / teacherNumber /
 * teacher 三套字段。 4. TeacherNumberMatrixEngine 输出 NORMAL 时，在 DecisionGateEngine 内按 READY 画像承接。 5.
 * teacherMatrixRiskPenalty 为空时，不再误判为 0，而是读取 teacherMatrixRisk / teacherNumberRisk。 6.
 * fromCandidate() 写 Decision 时同步 teacherMatrixScore / teacherMatrixRisk / teacherNumberSample /
 * teacherNumberHitRate 等别名。 7. 第14/14升级：最终BET必须通过
 * Probability诊断、DynamicGate质量门、Teacher五级状态、V20因链、冻结矩阵、智能审判。 8.
 * 第14/14升级：低频不救援，高频不硬封顶，只有真实强证据低反证可BET；假强/冻结/RISK/FREEZE转SHADOW/HOLD。 9. 第14/14升级：最终Decision同步写
 * finalDecisionStatus / finalDecisionQualityScore / finalDecisionRiskScore /
 * finalDecisionActionAdvice。
 *
 * <p>红线： - 不创建 Trade。 - 不保存 Decision。 - 不结算。 - 不写真实盈亏。 - 不写真实校准。 - 最终真实 BET 仍只由 allowBet 通过。 -
 * 第3/4字段链联调：即使新增字段未完全穿透，也必须按现象兜底拦截弱字段 BET。 - SHADOW / HOLD 不计真实统计。
 *
 * <p>Java7 / AIDE 兼容。
 */
public final class DecisionGateEngine {
    public static final String OUMAN_GALAXY_REBUILD_VERSION =
            AppConfig.OUMAN_GALAXY_REBUILD_VERSION;

    public static final String DEADLOCK_RECOVERY_BUILD =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";

    private static final int PICK_LIMIT =
            120; // V42 scans the complete live shortlist; no eligible tail candidate is silently
                 // dropped.
    private static final int MAX_REASON_LEN = 1500;

    private static final double STRONG_MIN_P = 0.345d;
    private static final double READY_MIN_P = 0.365d;
    private static final double NORMAL_MIN_P = 0.375d;
    private static final double ELITE_MIN_P = 0.390d;
    private static final double HIGH_FREQ_MIN_P = 0.395d;
    private static final double OVERHEAT_MIN_P = 0.398d;

    private static final double RAW_FLOOR_STRONG = 0.335d;
    private static final double RAW_FLOOR_READY = 0.340d;
    private static final double RAW_FLOOR_NORMAL = 0.345d;
    private static final double RAW_FLOOR_ELITE = 0.350d;

    private static final double EDGE_ALLOW_STRONG = -0.004d;
    private static final double EDGE_ALLOW_READY = -0.002d;
    private static final double EDGE_MIN_NORMAL = 0.000d;
    private static final double EDGE_MIN_ELITE = 0.002d;

    private static final double GATE_STRONG_CAP = 0.365d;
    private static final double GATE_READY_CAP = 0.375d;
    private static final double GATE_NORMAL_CAP = 0.392d;
    private static final double GATE_ELITE_CAP = 0.405d;
    private static final double GATE_RISK_CAP = 0.430d;

    private static final double EVIDENCE_STRONG = 0.620d;
    private static final double EVIDENCE_READY = 0.540d;
    private static final double EVIDENCE_RISK_HIGH = 0.680d;

    private static final double LEARNING_STRONG = 0.620d;
    private static final double LEARNING_WEAK = 0.300d;

    private static final int TEACHER_MATRIX_MIN_SAMPLE = 30;
    private static final int TEACHER_MATRIX_STRONG_SAMPLE = 80;
    private static final int TEACHER_MATRIX_COMBO_MIN_SAMPLE = 12;
    private static final int TEACHER_MATRIX_COMBO_STRONG_SAMPLE = 28;

    private static final double TEACHER_READY_HIT_RATE = 0.365d;
    private static final double TEACHER_STRONG_HIT_RATE = 0.395d;
    private static final double TEACHER_RISK_HIT_RATE = 0.285d;
    private static final double TEACHER_READY_CONF = 0.340d;
    private static final double TEACHER_STRONG_CONF = 0.480d;

    private static final double FREQ_LOW = 0.060d;
    private static final double FREQ_IDEAL_LOW = 0.080d;
    private static final double FREQ_IDEAL_HIGH = 0.180d;
    private static final double FREQ_STRONG_HIGH = 0.350d;
    private static final double FREQ_OVERHEAT = 0.500d;

    private static final double V20_READY_SCORE = 0.540d;
    private static final double V20_STRONG_SCORE = 0.620d;
    private static final double V20_ELITE_SCORE = 0.700d;
    private static final double V20_RISK_HIGH = 0.680d;
    private static final double V20_RISK_HARD = 0.780d;
    private static final int DECISION_WINDOW_12 = 12;
    private static final int DECISION_WINDOW_24 = 24;
    private static final int DECISION_WINDOW_48 = 48;
    private static final int DECISION_WINDOW_96 = 96;
    private static final int DECISION_WINDOW_360 = 360;
    private static final double DECISION_EVIDENCE_READY = 0.540d;
    private static final double DECISION_EVIDENCE_STRONG = 0.620d;
    private static final double DECISION_EVIDENCE_ELITE = 0.700d;
    private static final double DECISION_COUNTER_RISK_HIGH = 0.620d;
    private static final double DECISION_COUNTER_RISK_HARD = 0.760d;
    private static final double FINAL_DECISION_MIN_QUALITY = 0.520d;
    private static final double FINAL_DECISION_STRONG_QUALITY = 0.620d;
    private static final double FINAL_DECISION_RISK_LINE = 0.620d;
    private static final double FINAL_DECISION_HARD_RISK_LINE = 0.760d;
    private static final double FINAL_DIAG_COUNTER_RISK = 0.680d;
    private static final double FINAL_DIAG_SAMPLE_MIN = 0.240d;
    private static final double FINAL_DIAG_RECENT_MIN = 0.240d;
    private static final double FINAL_FREEZE_RISK_LINE = 0.620d;
    private static final double FINAL_SHADOW_STRONG_BOOST = 0.006d;
    private static final String V20_DECISION_GATE_VERSION = AppConfig.ACTIVE_QUALITY_BUILD_VERSION;
    private static final String V20_DECISION_GATE_PREFERRED_REVIEW_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";
    private static final double FINAL_REVIEW_BET_MIN_P = 0.380d;
    private static final double FINAL_REVIEW_BET_MIN_RAW = 0.340d;
    private static final double FINAL_REVIEW_BET_MIN_COVER = 0.500d;
    private static final double FINAL_REVIEW_BET_WEAK_V20_MIN_COVER = 0.550d;
    private static final double FINAL_REVIEW_BET_MAX_RISK = 0.350d;
    private static final double FINAL_REVIEW_BET_WEAK_V20_MAX_RISK = 0.250d;
    private static final double FINAL_REVIEW_BET_MIN_THIRD = 0.100d;
    private static final double FINAL_REVIEW_BET_WEAK_V20_MIN_THIRD = 0.105d;
    private static final double FINAL_REVIEW_BET_MIN_EDGE = -0.012d;
    private static final double FINAL_REVIEW_BET_MAX_EDGE = 0.020d;
    private static final String V20_FIELD_CHAIN_DECISION_GATE_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";
    private static final double FIELD_CHAIN_HARD_EDGE_FALSE_STRONG = 0.020d;
    private static final double FIELD_CHAIN_HARD_P_WEAK = 0.380d;
    private static final double FIELD_CHAIN_HARD_P_NO_STRONG = 0.415d;
    private static final double FIELD_CHAIN_HARD_P_COVER_EMPTY = 0.425d;
    private static final double FINAL_EDGE_FALSE_STRONG_LINE = 0.020d;
    private static final double FINAL_PREFERRED_P_MIN = 0.380d;
    private static final double FINAL_PREFERRED_P_MAX = 0.400d;
    private static final double FINAL_PREFERRED_EDGE_MIN = 0.010d;
    private static final double FINAL_PREFERRED_EDGE_MAX = 0.020d;
    private static final double FINAL_K3_COVERAGE_RISK_LINE = 0.620d;
    private static final double FINAL_K3_COVERAGE_HARD_RISK = 0.700d;
    private static final double FINAL_THIRD_NUMBER_MIN = 0.088d;
    private static final double FINAL_COVERAGE_QUALITY_MIN = 0.330d;
    private static final double FINAL_P40_FALSE_HIGH_LINE = 0.400d;
    private static final String V20_DYNAMIC_CAUSAL_DECISION_GATE_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";
    private static final String V20_DECISION_GATE_MAIN_CHAIN_FIX_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";
    private static final String V20_DECISION_GATE_CLOSED_LOOP_FIX_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";
    private static final String V20_DECISION_GATE_ONLY_REAL_BET_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";
    private static final String FINAL_CLOSURE_DECISION_GATE_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";
    private static final String BACKGROUND_EVIDENCE_DECISION_GATE_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";
    private static final String REALTIME_VALID_DOMAIN_DECISION_GATE_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";
    private static final String LINK_MODIFICATION_V2_DECISION_GATE_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";
    private static final String STATUS_NO_SAMPLE_NEUTRAL = "NO_SAMPLE_NEUTRAL";
    private static final String STATUS_WATCH_NEUTRAL = "WATCH_NEUTRAL";
    private static final String FINAL_REJECT_OK = "FINAL_DECISION_OK";
    private static final double DYNAMIC_CAUSAL_RANDOM_BASELINE = 0.300d;
    private static final double DYNAMIC_CAUSAL_READY_HIT = 0.370d;
    private static final double DYNAMIC_CAUSAL_STRONG_HIT = 0.400d;
    private static final double DYNAMIC_CAUSAL_RECENT_READY = 0.340d;
    private static final int DYNAMIC_CAUSAL_MIN_SAMPLE_WATCH = 12;
    private static final int DYNAMIC_CAUSAL_MIN_SAMPLE_READY = 30;
    private static final int DYNAMIC_CAUSAL_MIN_SAMPLE_STRONG = 80;
    private static final int DYNAMIC_CAUSAL_MAX_LOSE_READY = 6;
    private static final int DYNAMIC_CAUSAL_MAX_LOSE_FREEZE = 9;
    private static final double DYNAMIC_CAUSAL_NEED_MIN = 0.420d;
    private static final double DYNAMIC_CAUSAL_NEED_READY = 0.560d;
    private static final double DYNAMIC_CAUSAL_THIRD_MIN = 0.280d;
    private static final double DYNAMIC_CAUSAL_THIRD_READY = 0.380d;
    private static final double DYNAMIC_CAUSAL_NEED_HARD_RISK = 0.680d;
    private static final double DYNAMIC_CAUSAL_COUNTER_HARD = 0.680d;
    private static final int DYNAMIC_CAUSAL_FALSE_CAUSE_HARD = 4;
    private static final double DYNAMIC_CAUSAL_READY_QUALITY = 0.520d;
    private static final double DYNAMIC_CAUSAL_STRONG_QUALITY = 0.650d;
    private static final double DYNAMIC_CAUSAL_HARD_RISK = 0.680d;

    private final DataGuard dataGuard;
    private final SimilarShapeEngine similarShapeEngine;
    private final ShapeMemoryEngine memoryEngine;
    private final BreakRecoveryEngine recoveryEngine;
    private final CalibrationEngine calibrationEngine;
    private final TrendStateMachine trendStateMachine;
    private final DecisionAuthorizationEngine authorizationEngine;

    public DecisionGateEngine() {
        dataGuard = new DataGuard();
        similarShapeEngine = new SimilarShapeEngine();
        memoryEngine = new ShapeMemoryEngine();
        recoveryEngine = new BreakRecoveryEngine();
        calibrationEngine = new CalibrationEngine();
        trendStateMachine = new TrendStateMachine();
        authorizationEngine = new DecisionAuthorizationEngine();
    }

    /**
     * The only V50 method allowed to create DecisionAction.BET. The typed V50 path never calls any
     * V20/V41/V42/V47/V48/V49 gate, rescue or frequency debt code.
     */
    // R30_READONLY_BEGIN: decideV50
    public V50Models.AuthorizedDecision decideV50(
            DbStore store, V50CompiledActionContract compiled) {
        V50Models.AuthorizedDecision original = decideV50OriginalR30(store, compiled);
        DecisionExplanationR30.afterV50(store, compiled, original);
        return original;
    }
    // R30_READONLY_END

    private V50Models.AuthorizedDecision decideV50OriginalR30(
            DbStore store, V50CompiledActionContract compiled) {
        V50Enums.DecisionAction action = V50Enums.DecisionAction.HOLD;
        V50Enums.RejectCode reject = V50Enums.RejectCode.DATA_INCOMPLETE;
        V50PredictionContract.Result validation =
                compiled == null
                        ? new V50PredictionContract.Result(
                                false,
                                V50Enums.RejectCode.DATA_INCOMPLETE,
                                "V50_COMPILED_CONTRACT_NULL")
                        : compiled.validate();
        if (validation.valid) {
            action = V50Enums.DecisionAction.SHADOW;
            reject =
                    compiled.policy == null
                            ? V50Enums.RejectCode.DATA_INCOMPLETE
                            : compiled.policy.rejectCode;
            V50Repository.ReadyReport ready = V50Repository.ensure(store);
            if (!ready.ready) {
                reject = V50Enums.RejectCode.LEDGER_INCOMPLETE;
            } else if (compiled.policy != null
                    && compiled.policy.realEligible
                    && compiled.policy.lane != V50Enums.Lane.SHADOW
                    && V50BuildManifest.ENGINEERING_COMPLETE
                    && V50BuildManifest.MODEL_PROVEN
                    && V50BuildManifest.REALTIME_PROVEN
                    && V50SelectivePolicyArtifact.MODEL_GATE_PASSED) {
                String targetIssue = compiled.envelope.context.targetIssue;
                Models.Trade existing =
                        store == null ? null : store.getTradeByTargetIssue(targetIssue);
                if (existing != null) {
                    reject = V50Enums.RejectCode.DUPLICATE_TRADE;
                } else if (store != null && store.hasOpenRealTrade()) {
                    reject = V50Enums.RejectCode.UNSETTLED_TRADE_EXISTS;
                } else {
                    action = V50Enums.DecisionAction.BET;
                    reject = V50Enums.RejectCode.NONE;
                }
            } else if (reject == V50Enums.RejectCode.NONE) {
                reject = V50Enums.RejectCode.MODEL_ARTIFACT_UNPROVEN;
            }
        } else {
            reject = validation.rejectCode;
            if (compiled != null
                    && compiled.envelope != null
                    && compiled.envelope.action != null
                    && compiled.envelope.action.validFixedK3()) {
                action = V50Enums.DecisionAction.SHADOW;
            }
        }
        return authorizationEngine.authorizeV50(compiled, action, reject);
    }

    public V50Models.AuthorizedDecision blockV50(
            V50CompiledActionContract compiled, V50Enums.RejectCode rejectCode) {
        return authorizationEngine.authorizeV50(
                compiled,
                V50Enums.DecisionAction.HOLD,
                rejectCode == null ? V50Enums.RejectCode.LEDGER_INCOMPLETE : rejectCode);
    }

    /**
     * Compatibility adapter for existing storage/UI. It mirrors typed V50 values but never
     * recalculates them, and this gate remains the owner of the legacy BET/SHADOW/HOLD action
     * string.
     */
    public Models.Decision toLegacyDecisionV50(V50Models.RuntimeResult runtime) {
        Models.Decision out = new Models.Decision();
        if (runtime == null
                || runtime.authorized == null
                || runtime.authorized.envelope == null
                || runtime.authorized.envelope.action == null
                || runtime.authorized.envelope.context == null) {
            out.decision = AppConfig.DECISION_HOLD;
            out.action = AppConfig.ACTION_HOLD;
            out.blockCode = "V50_RUNTIME_RESULT_INCOMPLETE";
            out.finalDecisionRejectCode = out.blockCode;
            out.reasonSummary = "V50 typed runtime result incomplete";
            return out;
        }
        V50Models.AuthorizedDecision authorized = runtime.authorized;
        V50Models.SelectedActionEnvelope envelope = authorized.envelope;
        V50Models.PositionAction selected = envelope.action;
        out.baseIssue = envelope.context.baseIssue;
        out.targetIssue = envelope.context.targetIssue;
        out.createdAt = authorized.authorizationTime;
        out.decision = authorized.action.name();
        out.action = authorized.action.name();
        out.position = selected.position;
        out.n1 = selected.top3[0];
        out.n2 = selected.top3[1];
        out.n3 = selected.top3[2];
        out.k3Mask = AppConfig.buildK3Mask(out.n1, out.n2, out.n3);
        out.rawP = selected.rawActionP;
        out.modelP = selected.rankAdjustedP;
        out.rawRankScore = selected.rankScore;
        out.calibratedP = envelope.selectedHitP;
        out.finalP = envelope.selectedHitP;
        out.honestProbability = envelope.selectedHitP;
        out.calibratedHitProbability = envelope.selectedHitP;
        out.dynamicGate = V50SelectivePolicyArtifact.GLOBAL_SELECTED_P_THRESHOLD;
        out.edge = envelope.selectedHitP - out.dynamicGate;
        out.v46EventProbability = envelope.selectedHitP;
        out.v46ActionLcb =
                Math.max(
                        0.0d,
                        envelope.selectedHitP
                                - (runtime.uncertainty == null ? 1.0d : runtime.uncertainty.total)
                                        * 0.08d);
        out.v46ProbabilityUncertainty = envelope.uncertainty;
        out.v46ExpectedValue = AppConfig.ODDS * envelope.selectedHitP - AppConfig.BET_COST;
        out.v46FinalRank = selected.rank;
        out.v46FormalCandidateCount = 10;
        out.v46FinalContractHash = envelope.contractHash;
        out.v46ContractHash = envelope.contractHash;
        out.v46Lane = envelope.lane.name();
        out.laneCandidate = envelope.lane.name();
        out.betDomain = envelope.lane.name();
        out.finalDecisionBetDomain = envelope.lane.name();
        out.stateKey = selected.dynamicDomain;
        out.trendLife =
                runtime.states != null
                                && selected.position <= runtime.states.length
                                && runtime.states[selected.position - 1] != null
                        ? runtime.states[selected.position - 1].fineRunLength
                        : 0.0d;
        out.driftRisk =
                runtime.states != null
                                && selected.position <= runtime.states.length
                                && runtime.states[selected.position - 1] != null
                        ? runtime.states[selected.position - 1].transitionRisk
                        : 1.0d;
        out.buildId = V50BuildManifest.BUILD_ID;
        out.cohortId = V50BuildManifest.COHORT_ID;
        out.schemaVersion = V50BuildManifest.SCHEMA_ID;
        out.featureSchemaVersion = V50BuildManifest.FEATURE_SCHEMA;
        out.rankingVersion = V50ActionRankerArtifact.ARTIFACT_ID;
        out.calibrationVersion = V50SelectedCalibrationArtifact.ARTIFACT_ID;
        out.gatePolicyVersion = V50SelectivePolicyArtifact.ARTIFACT_ID;
        out.pipelineVersion = V50BuildManifest.STRATEGY_VERSION;
        out.v43StrategyVersion = V50BuildManifest.STRATEGY_VERSION;
        out.contractDecisionId = authorized.decisionId;
        out.candidateHash = runtime.compiledContractChecksum;
        out.snapshotMaxIssue = envelope.context.historyMaxIssue;
        out.trainedUntilIssue = V50NumberModelArtifact.TRAINING_PERIOD_END;
        out.contractValid = 1;
        out.contractStatus = "V50_TYPED_CONTRACT_VALID";
        out.authorizationId = authorized.authorizationSignature;
        out.authorizationValid = authorized.action == V50Enums.DecisionAction.BET ? 1 : 0;
        out.authorizedMode = authorized.action.name();
        out.authorizationRejectCode = authorized.rejectCode.name();
        out.authorizedAt = authorized.authorizationTime;
        out.authorizationProbability = envelope.selectedHitP;
        out.authorizationGate = out.dynamicGate;
        out.authorizationEdge = out.edge;
        out.dataOk = runtime.success ? 1 : 0;
        out.settled = 0;
        out.result = AppConfig.RESULT_WAIT;
        out.probabilityMode = "V50_SELECTED_ACTION_P";
        out.gateMode = "V50_SELECTIVE_RISK_ARTIFACT";
        out.blockCode =
                authorized.rejectCode == V50Enums.RejectCode.NONE
                        ? ""
                        : authorized.rejectCode.name();
        out.finalDecisionRejectCode = out.blockCode;
        out.reasonSummary =
                envelope.primaryCause
                        + "｜"
                        + envelope.secondaryCause
                        + "｜反证="
                        + envelope.counterEvidence;
        out.reasonText = out.reasonSummary;
        out.preDrawReasonFingerprint = envelope.contractHash;
        out.preDrawReasonText = out.reasonSummary;
        out.learningSummary = "V50 1/10/100 full-information ledger";
        out.selfDiagnosisText =
                "release="
                        + V50BuildManifest.RELEASE_STATE
                        + "|rankArtifact="
                        + V50ActionRankerArtifact.ARTIFACT_ID
                        + "|calibrationArtifact="
                        + V50SelectedCalibrationArtifact.ARTIFACT_ID;
        out.statScope =
                authorized.action == V50Enums.DecisionAction.BET
                        ? AppConfig.STAT_SCOPE_REAL_TRADE
                        : (authorized.action == V50Enums.DecisionAction.SHADOW
                                ? AppConfig.STAT_SCOPE_SHADOW
                                : AppConfig.STAT_SCOPE_HOLD);
        out.realtimeCostMs = runtime.totalCostMs;
        return out;
    }

    /** V46唯一真实裁决入口。旧pickDecisionCandidate/finalCheck链仅保留为后台诊断， 本方法永不调用它们，也不在V46合同失败后运行第二套门。 */
    public Models.Decision decide(
            DbStore store,
            Models.CandidateBatch batch,
            List<Models.Draw> history,
            List<TrendStateMachine.TrendTrace> traces,
            List<BreakRecoveryEngine.RecoveryResult> recoveries) {
        long started = System.currentTimeMillis();
        ArrayList<Models.Draw> clean = dataGuard.normalizeHistory(history);
        if (clean == null || clean.size() == 0) {
            return finishOnlyState(
                    store,
                    formalHold("", "", AppConfig.BLOCK_DATA_ERROR, "V46历史为空，当前目标期正式HOLD", 0),
                    started);
        }
        Models.Draw latest = clean.get(clean.size() - 1);
        String baseIssue = latest == null ? "" : safeText(latest.issue);
        String targetIssue = dataGuard.buildNextIssue(baseIssue);
        String identity = store == null ? "" : store.getState(BuildIdentity.STATE_STATUS, "");
        if (!BuildIdentity.isBetCompatible(identity)) {
            Models.ComboCandidate diagnostic =
                    batch == null ? null : firstValidFinalCandidate(batch.candidates);
            if (diagnostic != null) {
                Models.Decision shadow =
                        makeShadowDecision(diagnostic, "V48身份硬阻断真实BET，但诊断观察/SHADOW继续｜" + identity);
                shadow.blockCode = AppConfig.V48_ROOT_IDENTITY;
                return finishOnlyState(store, shadow, started);
            }
            return finishOnlyState(
                    store,
                    formalHold(
                            baseIssue,
                            targetIssue,
                            AppConfig.BLOCK_DATA_ERROR,
                            "V48_BUILD_IDENTITY_BLOCK｜" + identity,
                            0),
                    started);
        }
        Models.DataCheck check = dataGuard.checkCanAnalyze(clean);
        if (check == null || !check.ok) {
            return finishOnlyState(
                    store,
                    formalHold(
                            baseIssue,
                            targetIssue,
                            AppConfig.BLOCK_SAMPLE_WEAK,
                            check == null ? "V46历史不足30期" : check.reason,
                            1),
                    started);
        }
        if (batch == null || batch.candidates == null || batch.candidates.size() == 0) {
            return finishOnlyState(
                    store,
                    formalHold(
                            baseIssue, targetIssue, AppConfig.BLOCK_COMBO_WEAK, "V46最终40候选为空", 1),
                    started);
        }
        GateRuntimeContext runtime =
                buildRuntimeContext(store, clean, baseIssue, targetIssue, started);
        if (runtime.openTradeExists) {
            return finishOnlyState(
                    store,
                    formalHold(
                            baseIssue,
                            targetIssue,
                            AppConfig.BLOCK_DATA_ERROR,
                            "目标期已有未结算真实BET，禁止重复下注",
                            1),
                    started);
        }
        forceCurrentIssue(batch.candidates, baseIssue, targetIssue);
        LanePolicyCompiler.clearIssue(targetIssue);
        V48PipelineLedger.laneChecked(
                store, batch.candidates == null ? 0 : batch.candidates.size());
        Models.Decision authorized =
                tryV28HonestUtilityAuthorizedDecision(
                        store, batch.candidates, clean, baseIssue, targetIssue);
        if (authorized != null) {
            try {
                if (store != null)
                    store.setState("v46_decision_path", "V46_SINGLE_LANE_AUTHORIZED");
            } catch (Throwable ignore) {
            }
            V48PipelineLedger.authorized(store, 1);
            return finishOnlyState(store, authorized, started);
        }

        Models.ComboCandidate best = firstValidFinalCandidate(batch.candidates);
        if (best == null) {
            return finishOnlyState(
                    store,
                    formalHold(
                            baseIssue,
                            targetIssue,
                            AppConfig.BLOCK_COMBO_WEAK,
                            "V46最终候选中没有合法单位置K3",
                            1),
                    started);
        }
        ActionUtilityEngine.Result u = ActionUtilityEngine.evaluate(store, best);
        LanePolicyCompiler.Result lane = LanePolicyCompiler.compile(store, best, u);
        Models.Decision shadow =
                makeShadowDecision(
                        best,
                        "V46唯一Lane没有合格真实合同；不运行旧DecisionGate回退。"
                                + (lane == null ? "" : "｜" + lane.summary()));
        shadow.baseIssue = baseIssue;
        shadow.targetIssue = targetIssue;
        shadow.blockCode = lane == null ? "V46_NO_ELIGIBLE_CONTRACT" : safeText(lane.code);
        shadow.finalDecisionRejectCode = shadow.blockCode;
        shadow.v46Lane = lane == null ? "SHADOW" : lane.lane;
        shadow.v46RootCauseCode = best.v46RootCauseCode;
        shadow.v46RootCauseOwner = best.v46RootCauseOwner;
        shadow.reasonSummary =
                appendReason(
                        shadow.reasonSummary,
                        "旧pickDecisionCandidate/finalCheck真实路径=DISABLED；SHADOW不计真实胜率、频率、盈亏");
        try {
            if (store != null) {
                store.setState("v46_decision_path", "V46_SINGLE_LANE_SHADOW");
                store.setState("v46_legacy_gate_invoked", "0");
                store.setState("v46_decision_reject", shadow.blockCode);
            }
        } catch (Throwable ignore) {
        }
        return finishOnlyState(store, shadow, started);
    }

    private Models.ComboCandidate firstValidFinalCandidate(List<Models.ComboCandidate> list) {
        if (list == null) return null;
        Models.ComboCandidate best = null;
        for (int i = 0; i < list.size(); i++) {
            Models.ComboCandidate c = list.get(i);
            if (c == null || !c.isValidK3()) continue;
            if (best == null
                    || FinalCandidateRankingEngine.eventP(c)
                            > FinalCandidateRankingEngine.eventP(best)
                    || (Math.abs(
                                            FinalCandidateRankingEngine.eventP(c)
                                                    - FinalCandidateRankingEngine.eventP(best))
                                    < 0.0000001d
                            && FinalCandidateRankingEngine.actionLcb(c)
                                    > FinalCandidateRankingEngine.actionLcb(best))) {
                best = c;
            }
        }
        return best;
    }

    private Models.Decision decideLegacyDiagnosticOnly(
            DbStore store,
            Models.CandidateBatch batch,
            List<Models.Draw> history,
            List<TrendStateMachine.TrendTrace> traces,
            List<BreakRecoveryEngine.RecoveryResult> recoveries) {
        try {
            if (store != null)
                store.registerModuleBuildV20(
                        "DecisionGateEngine",
                        AppConfig.ACTIVE_QUALITY_BUILD_VERSION,
                        "NORMAL + QUALIFIED_FLOOR + BOUNDED_REAL_PROBE");
        } catch (Throwable ignore) {
        }

        long startAt = System.currentTimeMillis();

        ArrayList<Models.Draw> clean = dataGuard.normalizeHistory(history);

        if (clean.size() == 0) {
            return finishOnlyState(
                    store,
                    formalHold("", "", AppConfig.BLOCK_DATA_ERROR, "没有有效历史开奖，当前目标期正式HOLD", 0),
                    startAt);
        }

        Models.Draw latest = clean.get(clean.size() - 1);
        String baseIssue = latest.issue == null ? "" : latest.issue;
        String targetIssue = dataGuard.buildNextIssue(baseIssue);
        String buildIdentity = store == null ? "" : store.getState(BuildIdentity.STATE_STATUS, "");
        if (!BuildIdentity.isBetCompatible(buildIdentity)) {
            return finishOnlyState(
                    store,
                    formalHold(
                            baseIssue,
                            targetIssue,
                            AppConfig.BLOCK_DATA_ERROR,
                            "BUILD_IDENTITY_BLOCK：包名或代码批次不一致，禁止真实BET｜" + buildIdentity,
                            0),
                    startAt);
        }
        try {
            if (store != null) {
                store.setState("decision_build_identity_status", buildIdentity);
                store.setState("decision_build_identity_compatible", "1");
            }
        } catch (Throwable ignore) {
        }

        setGateRunningState(store, targetIssue);

        Models.DataCheck latestCheck = dataGuard.checkLatest(latest);
        if (latestCheck == null || !latestCheck.ok) {
            String reason = latestCheck == null ? "最新开奖校验失败，当前目标期正式HOLD" : latestCheck.reason;
            return finishOnlyState(
                    store,
                    formalHold(baseIssue, targetIssue, AppConfig.BLOCK_DATA_ERROR, reason, 0),
                    startAt);
        }

        Models.DataCheck check = dataGuard.checkCanAnalyze(clean);
        if (check == null || !check.ok) {
            String reason = check == null ? "历史数据不足或异常，当前目标期正式HOLD" : check.reason;
            return finishOnlyState(
                    store,
                    formalHold(baseIssue, targetIssue, AppConfig.BLOCK_SAMPLE_WEAK, reason, 1),
                    startAt);
        }

        if (batch == null || batch.candidates == null || batch.candidates.size() == 0) {
            return finishOnlyState(
                    store,
                    formalHold(
                            baseIssue,
                            targetIssue,
                            AppConfig.BLOCK_COMBO_WEAK,
                            "候选池为空，当前目标期正式HOLD",
                            1),
                    startAt);
        }

        GateRuntimeContext runtime =
                buildRuntimeContext(store, clean, baseIssue, targetIssue, startAt);

        if (runtime.openTradeExists) {
            return finishOnlyState(
                    store,
                    formalHold(
                            baseIssue,
                            targetIssue,
                            AppConfig.BLOCK_DATA_ERROR,
                            "目标期已有未结算真实BET，禁止重复下注",
                            1),
                    startAt);
        }

        forceCurrentIssue(batch.candidates, baseIssue, targetIssue);

        /* V40 primary path: after the action-level OOS model is mature, selection is
         * made by the explicit calibrated authorization contract.  Legacy diagnostic
         * gates may still explain SHADOW, but cannot veto an otherwise fully typed,
         * risk-clean OOS authorization merely because an old WATCH label remained. */
        Models.Decision v28Authorized =
                tryV28HonestUtilityAuthorizedDecision(
                        store, batch.candidates, clean, baseIssue, targetIssue);
        if (v28Authorized != null) {
            return finishOnlyState(store, v28Authorized, startAt);
        }

        long pickStart = System.currentTimeMillis();

        DecisionPick pick =
                pickDecisionCandidate(
                        store,
                        runtime,
                        batch.candidates,
                        clean,
                        traces,
                        recoveries,
                        baseIssue,
                        targetIssue);

        long pickCost = System.currentTimeMillis() - pickStart;
        saveGateCostState(store, runtime, pickCost);

        if (pick == null
                || pick.candidate == null
                || !pick.candidate.isValidK3()
                || pick.check == null) {
            Models.ComboCandidate fallbackShadow =
                    pickBestValidShadowCandidate(store, batch.candidates, baseIssue, targetIssue);
            if (fallbackShadow != null) {
                Models.Decision shadow =
                        makeShadowDecision(
                                fallbackShadow,
                                "最终门未选出真实BET，但候选池存在合法固定K3；保留最优候选为SHADOW，不丢失策略字段，不计真实胜率/频率/盈亏");
                shadow.baseIssue = baseIssue;
                shadow.targetIssue = targetIssue;
                shadow.blockCode = "FINAL_NO_PICK_BUT_VALID_CANDIDATE_SHADOW";
                writeString(shadow, "finalDecisionRejectCode", shadow.blockCode);
                writeString(
                        shadow, "decisionGateVersion", BACKGROUND_EVIDENCE_DECISION_GATE_VERSION);
                return finishOnlyState(store, shadow, startAt);
            }
            return finishOnlyState(
                    store,
                    formalHold(
                            baseIssue,
                            targetIssue,
                            AppConfig.BLOCK_COMBO_WEAK,
                            "候选池中确实不存在合法固定K3，当前目标期正式HOLD",
                            1),
                    startAt);
        }

        Models.ComboCandidate best = pick.candidate;
        best.baseIssue = baseIssue;
        best.targetIssue = targetIssue;
        repairCandidateK3(best);

        FinalCheck finalCheck = pick.check;

        if (finalCheck.allowBet) {
            FinalCheck verify =
                    selectedBetFinalVerify(store, runtime, best, clean, traces, recoveries);

            if (verify != null && !verify.allowBet) {
                finalCheck = verify;
            }
        }

        finalCheck.reason =
                buildSelectedFullReason(
                        store, runtime, best, clean, traces, recoveries, finalCheck);

        if (!finalCheck.allowBet) {
            String decisionType =
                    finalCheck.allowShadow ? AppConfig.DECISION_SHADOW : AppConfig.DECISION_HOLD;

            Models.Decision d = fromCandidate(best, decisionType);
            String structuredReject = finalCheck.blockCode == null ? "" : finalCheck.blockCode;
            if (structuredReject.length() == 0)
                structuredReject = buildStructuredNonBetRejectCode(best);
            d.blockCode = structuredReject;
            writeString(d, "finalDecisionRejectCode", structuredReject);
            d.reasonSummary = finalCheck.reason;

            if (finalCheck.allowShadow) {
                d.reasonSummary =
                        appendReason(
                                d.reasonSummary,
                                "本期不进入真实BET，仅SHADOW观察学习；SHADOW不计真实胜率、不计真实频率、不计真实盈亏");
            }

            d.reasonText = d.reasonSummary;
            d.targetIssue = targetIssue;
            d.baseIssue = baseIssue;
            d.createdAt = System.currentTimeMillis();
            d.dataOk = 1;
            d.result = AppConfig.RESULT_WAIT;
            d.settled = 0;

            applyDecisionScope(d);
            repairDecisionK3(d);
            d.syncAction();

            return finishOnlyState(store, d, startAt);
        }

        Models.Decision decision = fromCandidate(best, AppConfig.DECISION_BET);
        decision.blockCode = "";
        decision.reasonSummary = finalCheck.reason;
        decision.reasonText = finalCheck.reason;
        decision.targetIssue = targetIssue;
        decision.baseIssue = baseIssue;
        decision.createdAt = System.currentTimeMillis();
        decision.dataOk = 1;
        decision.statScope = AppConfig.STAT_SCOPE_REAL_TRADE;
        decision.result = AppConfig.RESULT_WAIT;
        decision.settled = 0;

        repairDecisionK3(decision);
        PredictionContractEngine.stampDecision(best, decision);
        Models.DecisionAuthorization authorization =
                authorizationEngine.authorize(store, best, decision);
        if (authorization == null || authorization.valid != 1) {
            String reject = authorization == null ? "AUTHORIZATION_NULL" : authorization.rejectCode;
            DecisionAuthorizationEngine.downgradeUnauthorized(decision, reject);
            decision.reasonSummary =
                    appendReason(decision.reasonSummary, "星河新版授权拒绝：" + reject + "；只能SHADOW，不计真实统计");
            decision.reasonText = decision.reasonSummary;
        }
        decision.syncAction();

        return finishOnlyState(store, decision, startAt);
    }

    /**
     * V41 calibrated-event-probability + utility action selector. It does not manufacture
     * opportunity: every candidate must independently pass contract, OOS calibration, hierarchical
     * posterior, typed Teacher/Dynamic/Adaptive policies, coverage/third quality and edge rules.
     * Frequency debt is only consumed by an already qualified floor candidate inside
     * OosEligibilityEngine.
     */
    private Models.Decision tryV28HonestUtilityAuthorizedDecision(
            DbStore store,
            List<Models.ComboCandidate> candidates,
            List<Models.Draw> clean,
            String baseIssue,
            String targetIssue) {
        if (store == null || candidates == null || candidates.size() == 0) return null;

        Models.ComboCandidate best = null;
        OosEligibilityEngine.Evaluation bestEval = null;
        double bestScore = -999.0d;
        int checked = 0;
        int eligible = 0;
        String firstReject = "";
        java.util.HashMap<String, Integer> rejectCounts = new java.util.HashMap<String, Integer>();
        OpportunityMinerEngine.Funnel opportunityFunnel = new OpportunityMinerEngine.Funnel();
        int cap = Math.min(PICK_LIMIT, candidates.size());
        for (int i = 0; i < cap; i++) {
            Models.ComboCandidate c = candidates.get(i);
            if (c == null || !c.isValidK3()) continue;
            checked++;
            c.baseIssue = baseIssue;
            c.targetIssue = targetIssue;
            repairCandidateK3(c);
            PredictionContractEngine.stampCandidate(c, clean);
            OosEligibilityEngine.Evaluation e = OosEligibilityEngine.applyAndEvaluate(store, c);
            OosActionLogEngine.updateV42Policy(store, c, e);
            opportunityFunnel.observe(e);
            if (e == null || !e.eligible) {
                if (firstReject.length() == 0 && e != null) firstReject = e.code;
                String rc = e == null ? "OOS_EVALUATION_NULL" : safeText(e.code);
                Integer old = rejectCounts.get(rc);
                rejectCounts.put(rc, Integer.valueOf(old == null ? 1 : old.intValue() + 1));
                continue;
            }
            eligible++;
            double score =
                    e.probability * 1000000.0d
                            + RankingAuditEngine.expectedValue(e.probability) * 10000.0d
                            + FinalCandidateRankingEngine.actionLcb(c) * 100.0d
                            + c.v46CalibrationConfidence;
            if (best == null || betterV46Authorized(c, e, best, bestEval)) {
                best = c;
                bestEval = e;
                bestScore = score;
            }
        }

        try {
            store.setState("v28_decision_oos_checked", String.valueOf(checked));
            store.setState("v28_decision_oos_eligible", String.valueOf(eligible));
            store.setState("v28_decision_oos_first_reject", firstReject);
            store.setState("v40_decision_oos_checked", String.valueOf(checked));
            store.setState("v40_decision_oos_eligible", String.valueOf(eligible));
            store.setState("v40_decision_oos_first_reject", firstReject);
            store.setState("v41_decision_oos_reject_funnel", rejectCounts.toString());
            store.setState("v42_decision_oos_reject_funnel", rejectCounts.toString());
            OpportunityMinerEngine.persist(store, opportunityFunnel);
        } catch (Throwable ignore) {
        }
        if (best == null || bestEval == null) return null;

        Models.Decision d = fromCandidate(best, AppConfig.DECISION_BET);
        d.baseIssue = baseIssue;
        d.targetIssue = targetIssue;
        d.createdAt = System.currentTimeMillis();
        d.dataOk = 1;
        d.statScope = AppConfig.STAT_SCOPE_REAL_TRADE;
        d.result = AppConfig.RESULT_WAIT;
        d.settled = 0;
        d.blockCode = "";
        d.finalDecisionRejectCode = "";
        d.betDomain =
                bestEval.realProbe
                        ? AppConfig.BET_DOMAIN_REAL_BET_PROBE
                        : (bestEval.qualifiedFloor
                                ? AppConfig.BET_DOMAIN_QUALIFIED_FLOOR
                                : AppConfig.BET_DOMAIN_NORMAL);
        d.finalDecisionBetDomain = d.betDomain;
        d.decisionPath =
                bestEval.realProbe
                        ? AppConfig.DECISION_PATH_REAL_BET_PROBE
                        : (bestEval.qualifiedFloor
                                ? AppConfig.DECISION_PATH_QUALIFIED_FLOOR
                                : AppConfig.DECISION_PATH_NORMAL);
        d.reasonSummary =
                "V46 EventP/ActionLCB单一Lane授权候选："
                        + bestEval.summary()
                        + "｜EventP不被风险改写；ActionLCB独立；最终排序只执行一次；旧真实门不回退";
        d.reasonText = d.reasonSummary;
        repairDecisionK3(d);
        PredictionContractEngine.stampDecision(best, d);
        Models.DecisionAuthorization a = authorizationEngine.authorize(store, best, d);
        if (a == null || a.valid != 1) {
            try {
                store.setState(
                        "v28_decision_oos_authorization_reject",
                        a == null ? "AUTHORIZATION_NULL" : safeText(a.rejectCode));
            } catch (Throwable ignore) {
            }
            return null;
        }
        d.decision = AppConfig.DECISION_BET;
        d.action = AppConfig.DECISION_BET;
        d.syncAction();
        try {
            store.setState("v28_decision_oos_authorized", "1");
            store.setState("v40_decision_oos_authorized", "1");
            store.setState("v42_decision_oos_authorized", "1");
            store.setState("v28_decision_oos_authorized_id", safeText(d.contractDecisionId));
            store.setState("v28_decision_oos_authorized_summary", d.reasonSummary);
        } catch (Throwable ignore) {
        }
        return d;
    }

    private static boolean betterV46Authorized(
            Models.ComboCandidate c,
            OosEligibilityEngine.Evaluation e,
            Models.ComboCandidate best,
            OosEligibilityEngine.Evaluation bestEval) {
        if (best == null || bestEval == null) return true;
        int x = Double.compare(e.probability, bestEval.probability);
        if (x != 0) return x > 0;
        x =
                Double.compare(
                        RankingAuditEngine.expectedValue(e.probability),
                        RankingAuditEngine.expectedValue(bestEval.probability));
        if (x != 0) return x > 0;
        x =
                Double.compare(
                        FinalCandidateRankingEngine.actionLcb(c),
                        FinalCandidateRankingEngine.actionLcb(best));
        if (x != 0) return x > 0;
        x = Double.compare(c.v46CalibrationConfidence, best.v46CalibrationConfidence);
        if (x != 0) return x > 0;
        if (c.position != best.position) return c.position < best.position;
        return c.k3Mask < best.k3Mask;
    }

    private static String safeText(String s) {
        return s == null ? "" : s;
    }

    public Models.Decision decideFromBest(
            DbStore store,
            Models.ComboCandidate candidate,
            List<Models.Draw> history,
            List<TrendStateMachine.TrendTrace> traces,
            List<BreakRecoveryEngine.RecoveryResult> recoveries) {

        long startAt = System.currentTimeMillis();

        ArrayList<Models.Draw> clean = dataGuard.normalizeHistory(history);

        String baseIssue = "";
        String targetIssue = "";

        if (clean.size() > 0) {
            Models.Draw latest = clean.get(clean.size() - 1);
            baseIssue = latest.issue == null ? "" : latest.issue;
            targetIssue = dataGuard.buildNextIssue(baseIssue);
        }

        if (candidate == null || !candidate.isValidK3()) {
            return finishOnlyState(
                    store,
                    formalHold(
                            baseIssue,
                            targetIssue,
                            AppConfig.BLOCK_COMBO_WEAK,
                            "候选为空或K3非法，当前目标期正式HOLD",
                            1),
                    startAt);
        }

        candidate.baseIssue = baseIssue;
        candidate.targetIssue = targetIssue;
        repairCandidateK3(candidate);

        GateRuntimeContext runtime =
                buildRuntimeContext(
                        store, clean, baseIssue, targetIssue, System.currentTimeMillis());

        ArrayList<Models.ComboCandidate> v28One = new ArrayList<Models.ComboCandidate>();
        v28One.add(candidate);
        Models.Decision v28Authorized =
                tryV28HonestUtilityAuthorizedDecision(store, v28One, clean, baseIssue, targetIssue);
        if (v28Authorized != null) {
            return finishOnlyState(store, v28Authorized, startAt);
        }

        FinalCheck check =
                finalCheckInternal(store, runtime, candidate, clean, traces, recoveries, false);

        if (check == null) {
            check = new FinalCheck();
            deny(check, AppConfig.BLOCK_COMBO_WEAK, "最终门检查为空，HOLD", false);
        }

        if (check.allowBet) {
            FinalCheck verify =
                    selectedBetFinalVerify(store, runtime, candidate, clean, traces, recoveries);

            if (verify != null && !verify.allowBet) {
                check = verify;
            }
        }

        check.reason =
                buildSelectedFullReason(
                        store, runtime, candidate, clean, traces, recoveries, check);

        String decisionType =
                check.allowBet
                        ? AppConfig.DECISION_BET
                        : (check.allowShadow ? AppConfig.DECISION_SHADOW : AppConfig.DECISION_HOLD);

        Models.Decision d = fromCandidate(candidate, decisionType);
        String structuredReject =
                check.allowBet ? "" : (check.blockCode == null ? "" : check.blockCode);
        if (!check.allowBet && structuredReject.length() == 0)
            structuredReject = buildStructuredNonBetRejectCode(candidate);
        d.blockCode = structuredReject;
        writeString(d, "finalDecisionRejectCode", structuredReject);
        d.reasonSummary = check.reason;

        if (!check.allowBet && check.allowShadow) {
            d.reasonSummary =
                    appendReason(
                            d.reasonSummary,
                            "本期仅SHADOW观察，不计真实胜率、不计真实频率、不计真实盈亏，开奖后进入Teacher Matrix和V20因链反推学习");
        }

        d.reasonText = d.reasonSummary;
        d.dataOk = 1;
        d.result = AppConfig.RESULT_WAIT;
        d.settled = 0;

        applyDecisionScope(d);
        repairDecisionK3(d);
        PredictionContractEngine.stampDecision(candidate, d);
        if (AppConfig.DECISION_BET.equals(d.decision)) {
            Models.DecisionAuthorization authorization =
                    authorizationEngine.authorize(store, candidate, d);
            if (authorization == null || authorization.valid != 1) {
                String reject =
                        authorization == null ? "AUTHORIZATION_NULL" : authorization.rejectCode;
                DecisionAuthorizationEngine.downgradeUnauthorized(d, reject);
                d.reasonSummary = appendReason(d.reasonSummary, "星河新版授权拒绝：" + reject + "；只能SHADOW");
                d.reasonText = d.reasonSummary;
            }
        } else {
            authorizationEngine.refuse(store, d, structuredReject);
        }
        d.syncAction();

        return finishOnlyState(store, d, startAt);
    }

    public Models.Decision makeShadowDecision(Models.ComboCandidate candidate, String reason) {
        if (candidate == null || !candidate.isValidK3()) {
            return formalHold("", "", AppConfig.BLOCK_COMBO_WEAK, "SHADOW候选为空", 1);
        }

        repairCandidateK3(candidate);

        Models.Decision d = fromCandidate(candidate, AppConfig.DECISION_SHADOW);

        d.reasonSummary =
                reason == null || reason.length() == 0 ? "影子候选，仅学习，不进入真实胜率、真实频率、真实盈亏" : reason;

        d.reasonText = d.reasonSummary;
        d.blockCode = "";
        d.dataOk = 1;
        d.statScope = AppConfig.STAT_SCOPE_SHADOW;
        d.result = AppConfig.RESULT_WAIT;
        d.settled = 0;
        d.syncAction();

        return d;
    }

    public boolean canBet(
            DbStore store,
            Models.ComboCandidate candidate,
            List<Models.Draw> history,
            List<TrendStateMachine.TrendTrace> traces,
            List<BreakRecoveryEngine.RecoveryResult> recoveries) {

        ArrayList<Models.Draw> clean = dataGuard.normalizeHistory(history);

        String baseIssue = "";
        String targetIssue = "";

        if (clean.size() > 0) {
            Models.Draw latest = clean.get(clean.size() - 1);
            baseIssue = latest.issue == null ? "" : latest.issue;
            targetIssue = dataGuard.buildNextIssue(baseIssue);
        }

        GateRuntimeContext runtime =
                buildRuntimeContext(
                        store, clean, baseIssue, targetIssue, System.currentTimeMillis());

        FinalCheck check =
                finalCheckInternal(store, runtime, candidate, clean, traces, recoveries, false);

        if (check == null || !check.allowBet) {
            return false;
        }

        FinalCheck verify =
                selectedBetFinalVerify(store, runtime, candidate, clean, traces, recoveries);

        return verify == null || verify.allowBet;
    }

    public String explainFinal(
            DbStore store,
            Models.ComboCandidate candidate,
            List<Models.Draw> history,
            List<TrendStateMachine.TrendTrace> traces,
            List<BreakRecoveryEngine.RecoveryResult> recoveries) {

        ArrayList<Models.Draw> clean = dataGuard.normalizeHistory(history);

        String baseIssue = "";
        String targetIssue = "";

        if (clean.size() > 0) {
            Models.Draw latest = clean.get(clean.size() - 1);
            baseIssue = latest.issue == null ? "" : latest.issue;
            targetIssue = dataGuard.buildNextIssue(baseIssue);
        }

        GateRuntimeContext runtime =
                buildRuntimeContext(
                        store, clean, baseIssue, targetIssue, System.currentTimeMillis());

        FinalCheck check =
                finalCheckInternal(store, runtime, candidate, clean, traces, recoveries, true);

        if (check == null) {
            return "";
        }

        if (check.allowBet) {
            FinalCheck verify =
                    selectedBetFinalVerify(store, runtime, candidate, clean, traces, recoveries);

            if (verify != null && !verify.allowBet) {
                check = verify;
            }
        }

        return buildSelectedFullReason(store, runtime, candidate, clean, traces, recoveries, check);
    }

    private GateRuntimeContext buildRuntimeContext(
            DbStore store,
            ArrayList<Models.Draw> clean,
            String baseIssue,
            String targetIssue,
            long startAt) {

        GateRuntimeContext ctx = new GateRuntimeContext();

        ctx.clean = clean == null ? new ArrayList<Models.Draw>() : clean;
        ctx.baseIssue = baseIssue == null ? "" : baseIssue;
        ctx.targetIssue = targetIssue == null ? "" : targetIssue;
        ctx.startAt = startAt;

        long contextStart = System.currentTimeMillis();

        long t = System.currentTimeMillis();
        ctx.openTradeExists = hasOpenTradeForTarget(store, ctx.targetIssue);
        ctx.costOpenTradeMs = System.currentTimeMillis() - t;

        t = System.currentTimeMillis();
        ctx.frequencyState = loadFrequencyStateSafe(store);
        ctx.freq50 = ctx.frequencyState.frequency50;
        ctx.freq100 = ctx.frequencyState.frequency100;
        ctx.freq300 = ctx.frequencyState.frequency300;
        /* 保留旧别名。 */
        ctx.freq500 = ctx.freq50;
        ctx.freq1000 = ctx.freq300;
        if (store != null) {
            try {
                ctx.probeFrequency20 = store.getRecentProbeFrequency(AppConfig.PROBE_RECENT_WINDOW);
            } catch (Throwable e) {
                ctx.probeFrequency20 = 0.0d;
            }
            try {
                ctx.probeFrequency100 =
                        store.getRecentProbeFrequency(AppConfig.FREQ_LOOKBACK_ISSUES);
            } catch (Throwable e) {
                ctx.probeFrequency100 = 0.0d;
            }
            try {
                ctx.recentLoseCount6 = store.getRecentRealLoseCount(6);
            } catch (Throwable e) {
                ctx.recentLoseCount6 = 0;
            }
        }
        ctx.costFreqMs = System.currentTimeMillis() - t;

        t = System.currentTimeMillis();

        if (store != null) {
            try {
                ctx.learningStateText = store.getState("learning_state_key", "");
                ctx.learningReasonText = store.getState("learning_reason_detail", "");
                ctx.learningAdviceText = store.getState("learning_next_advice", "");
                ctx.probabilityAuditStatus = store.getState("probability_audit_status", "");
                ctx.moduleEffectStatus = store.getState("module_effect_status", "");
                ctx.moduleEffectSummary = store.getState("module_effect_summary", "");
                ctx.shadowPromotionStatus = store.getState("shadow_promotion_status", "");
                ctx.shadowPromotionSummary = store.getState("shadow_promotion_summary", "");
                ctx.dynamicFreezeStatus = store.getState("dynamic_freeze_status", "");
                ctx.dynamicFreezeSummary = store.getState("dynamic_freeze_summary", "");
                ctx.adaptiveJudgeStatus = store.getState("adaptive_judge_status", "");
                ctx.adaptiveJudgeSummary = store.getState("adaptive_judge_summary", "");
                ctx.diagnosisClosedLoopSummary =
                        store.getState("diagnosis_closed_loop_summary", "");
            } catch (Throwable e) {
                ctx.learningStateText = "";
                ctx.learningReasonText = "";
                ctx.learningAdviceText = "";
                ctx.probabilityAuditStatus = "";
                ctx.moduleEffectStatus = "";
                ctx.moduleEffectSummary = "";
                ctx.shadowPromotionStatus = "";
                ctx.shadowPromotionSummary = "";
                ctx.dynamicFreezeStatus = "";
                ctx.dynamicFreezeSummary = "";
                ctx.adaptiveJudgeStatus = "";
                ctx.adaptiveJudgeSummary = "";
                ctx.diagnosisClosedLoopSummary = "";
            }
        }

        ctx.costLearningStateMs = System.currentTimeMillis() - t;
        ctx.costContextMs = System.currentTimeMillis() - contextStart;

        return ctx;
    }

    private void forceCurrentIssue(
            List<Models.ComboCandidate> candidates, String baseIssue, String targetIssue) {
        if (candidates == null) {
            return;
        }

        for (int i = 0; i < candidates.size(); i++) {
            Models.ComboCandidate c = candidates.get(i);

            if (c == null) {
                continue;
            }

            c.baseIssue = baseIssue == null ? "" : baseIssue;
            c.targetIssue = targetIssue == null ? "" : targetIssue;

            repairCandidateK3(c);
        }
    }

    private DecisionPick pickDecisionCandidate(
            DbStore store,
            GateRuntimeContext runtime,
            List<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            List<TrendStateMachine.TrendTrace> traces,
            List<BreakRecoveryEngine.RecoveryResult> recoveries,
            String baseIssue,
            String targetIssue) {

        ArrayList<Models.ComboCandidate> prepared =
                prepareCandidates(candidates, baseIssue, targetIssue, runtime);

        if (prepared.size() == 0) {
            return null;
        }

        DecisionPick bestBet = null;
        DecisionPick bestShadow = null;
        DecisionPick bestHold = null;

        int limit = Math.min(prepared.size(), PICK_LIMIT);

        for (int i = 0; i < limit; i++) {
            Models.ComboCandidate c = prepared.get(i);

            if (c == null || !c.isValidK3()) {
                continue;
            }

            if (runtime != null) {
                runtime.fastCheckedCount++;
                runtime.fullCheckCount++;
            }

            FinalCheck check =
                    finalCheckInternal(store, runtime, c, history, traces, recoveries, false);

            if (check == null) {
                check = new FinalCheck();
                deny(check, AppConfig.BLOCK_LOW_PROBABILITY, "最终门检查为空，HOLD", true);
            }

            double score = finalDecisionScore(c, check);

            DecisionPick pick = new DecisionPick();
            pick.candidate = c;
            pick.check = check;
            pick.score = score;

            if (check.allowBet) {
                if (bestBet == null || pick.score > bestBet.score) {
                    bestBet = pick;
                }

                if (isFastAcceptBet(c, check, runtime)) {
                    break;
                }
            } else if (check.allowShadow) {
                if (bestShadow == null || pick.score > bestShadow.score) {
                    bestShadow = pick;
                }
            } else {
                if (bestHold == null || pick.score > bestHold.score) {
                    bestHold = pick;
                }
            }
        }

        if (bestBet != null) {
            try {
                if (store != null) {
                    store.setState("b07r2_real_action_owner", "DecisionGateEngine");
                    store.setState("b07r2_real_action_status", "BET_ALLOWED_BY_STRICT_GATE");
                    store.setState(
                            "b07r2_real_action_target",
                            bestBet.candidate == null || bestBet.candidate.targetIssue == null
                                    ? ""
                                    : bestBet.candidate.targetIssue);
                    store.setState(
                            "b07r2_real_action_at", String.valueOf(System.currentTimeMillis()));
                }
            } catch (Throwable ignore) {
            }
            bestBet.check.reason =
                    appendReason(
                            bestBet.check.reason,
                            "最终门横向筛选后选择当前最优真实BET；版本=V20_DYNAMIC_CAUSE_DECISION_GATE；频率只软调门；TeacherMatrix为空中性；真实胜率不硬刹车；V20因链只参与证据和风险");
            return bestBet;
        }

        if (bestShadow != null) {
            bestShadow.check.reason =
                    appendReason(bestShadow.check.reason, "候选池内未发现可真实BET项，保留最优SHADOW用于开奖后反推学习");
            return bestShadow;
        }

        return bestHold;
    }

    private ArrayList<Models.ComboCandidate> prepareCandidates(
            List<Models.ComboCandidate> candidates,
            String baseIssue,
            String targetIssue,
            GateRuntimeContext runtime) {

        ArrayList<Models.ComboCandidate> out = new ArrayList<Models.ComboCandidate>();

        if (candidates == null || candidates.size() == 0) {
            return out;
        }

        for (int i = 0; i < candidates.size(); i++) {
            Models.ComboCandidate raw = candidates.get(i);

            if (raw == null || !raw.isValidK3()) {
                continue;
            }

            Models.ComboCandidate c = raw.copy();

            /*
             * 关键修复：
             * raw.copy() 很可能只复制老字段，不复制 V20 后续通过反射写入的字段。
             * 教师矩阵、教师号码、V20因链、规则健康、第三防线、伪强冻结等字段必须在这里桥接回来。
             */
            copyRuntimeBridgeFields(raw, c);

            c.baseIssue = baseIssue == null ? "" : baseIssue;
            c.targetIssue = targetIssue == null ? "" : targetIssue;

            repairCandidateK3(c);

            if (!c.isValidK3()) {
                continue;
            }

            c.dynamicGate = cleanIncomingGate(c.dynamicGate, AppConfig.DEFAULT_DYNAMIC_GATE);

            if (c.finalP <= 0.0d && c.rawP > 0.0d) {
                c.finalP = c.rawP;
            }

            if (c.rawP <= 0.0d && c.finalP > 0.0d) {
                c.rawP = c.finalP;
            }

            double fallbackP = strongestAvailableProbability(c);
            if (c.finalP <= 0.0d) c.finalP = fallbackP;
            if (c.rawP <= 0.0d) c.rawP = fallbackP;
            c.finalP = AppConfig.clampProbability(c.finalP);
            c.rawP = AppConfig.clampProbability(c.rawP);
            c.edge = c.finalP - c.dynamicGate;

            /*
             * 低概率不是非法候选。它不能BET/不能频率探针，但必须保留为SHADOW，
             * 以便完整记录FinalP/Gate/Edge/Cover/Third/Domain并参与开奖后反事实学习。
             */
            /* 73487246：低FinalP只影响排序和NORMAL_BET，不得预先杀死15%硬频率下限。 */
            if (c.finalP < AppConfig.V42_PROBE_MIN_P
                    && (c.probeRejectReason == null || c.probeRejectReason.length() == 0)) {
                c.probeRejectReason = "NORMAL_PROBE_P_LOW_FLOOR_STILL_ELIGIBLE";
            }

            addUniqueCandidate(out, c);
        }

        Collections.sort(
                out,
                new Comparator<Models.ComboCandidate>() {
                    public int compare(Models.ComboCandidate a, Models.ComboCandidate b) {
                        double sa = quickFinalScore(a, null, null);
                        double sb = quickFinalScore(b, null, null);

                        if (sa < sb) {
                            return 1;
                        }

                        if (sa > sb) {
                            return -1;
                        }

                        return compareCandidateTie(a, b);
                    }
                });

        if (runtime != null) {
            runtime.preparedCandidateCount = out.size();
        }

        return out;
    }

    private void copyRuntimeBridgeFields(Models.ComboCandidate from, Models.ComboCandidate to) {
        if (from == null || to == null) {
            return;
        }

        try {
            Field[] fs = from.getClass().getFields();

            if (fs == null) {
                return;
            }

            for (int i = 0; i < fs.length; i++) {
                Field src = fs[i];

                if (src == null) {
                    continue;
                }

                try {
                    Field dst = to.getClass().getField(src.getName());
                    Object v = src.get(from);
                    dst.set(to, v);
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable e) {
        }
    }

    private FinalCheck finalCheckInternal(
            DbStore store,
            GateRuntimeContext runtime,
            Models.ComboCandidate candidate,
            List<Models.Draw> history,
            List<TrendStateMachine.TrendTrace> traces,
            List<BreakRecoveryEngine.RecoveryResult> recoveries,
            boolean buildFullReason) {

        FinalCheck check = new FinalCheck();

        if (buildFullReason && runtime != null) {
            runtime.fullReasonCount++;
        }

        if (candidate == null || !candidate.isValidK3()) {
            deny(
                    check,
                    AppConfig.BLOCK_COMBO_WEAK,
                    buildFullReason ? "最终门：候选为空或K3非法，HOLD" : "候选为空或K3非法",
                    false);
            return check;
        }

        repairCandidateK3(candidate);

        ArrayList<Models.Draw> clean;

        if (runtime != null && runtime.clean != null) {
            clean = runtime.clean;
        } else {
            clean = dataGuard.normalizeHistory(history);
        }

        if (clean.size() < AppConfig.MIN_DRAWS_ANALYZE) {
            deny(
                    check,
                    AppConfig.BLOCK_SAMPLE_WEAK,
                    buildFullReason
                            ? "最终门：历史少于" + AppConfig.MIN_DRAWS_ANALYZE + "期，只允许HOLD"
                            : "历史不足",
                    false);
            return check;
        }

        if (candidate.targetIssue == null || candidate.targetIssue.length() == 0) {
            Models.Draw latest = clean.get(clean.size() - 1);
            candidate.baseIssue = latest.issue == null ? "" : latest.issue;
            candidate.targetIssue = dataGuard.buildNextIssue(candidate.baseIssue);
        }

        if (runtime != null && runtime.openTradeExists) {
            deny(
                    check,
                    AppConfig.BLOCK_DATA_ERROR,
                    buildFullReason ? "最终门：目标期已有未结算BET，禁止重复下注" : "目标期已有未结算BET",
                    false);
            return check;
        }

        if (runtime == null && hasOpenTradeForTarget(store, candidate.targetIssue)) {
            deny(
                    check,
                    AppConfig.BLOCK_DATA_ERROR,
                    buildFullReason ? "最终门：目标期已有未结算BET，禁止重复下注" : "目标期已有未结算BET",
                    false);
            return check;
        }

        TrendStateMachine.TrendTrace trace =
                trendStateMachine.findTraceForPosition(traces, candidate.position);
        BreakRecoveryEngine.RecoveryResult recovery =
                recoveryEngine.findForPosition(recoveries, candidate.position);

        Models.SimilarResult similar = null;
        ShapeMemoryEngine.MemorySignal memory = null;
        CalibrationEngine.CalibrationSignal calibration = null;

        if (buildFullReason) {
            similar = safeSimilar(clean, candidate);
            memory = safeMemory(candidate, clean);
            calibration = safeCalibration(store, candidate.rawP);
        }

        DecisionContext ctx =
                buildDecisionContext(
                        runtime,
                        candidate,
                        trace,
                        recovery,
                        similar,
                        memory,
                        calibration,
                        clean,
                        buildFullReason);

        double freq100 =
                runtime == null ? recentBetFrequency(store, freqWindowShort()) : runtime.freq100;
        double freq500 =
                runtime == null ? recentBetFrequency(store, freqWindowMid()) : runtime.freq500;
        double freq1000 =
                runtime == null ? recentBetFrequency(store, freqWindowLong()) : runtime.freq1000;

        candidate.freq100 = freq100;
        candidate.freq500 = freq500;
        candidate.freq1000 = freq1000;

        boolean strong = isStrongEnoughForRealtime(candidate, trace, recovery, ctx);
        boolean promising = isPromisingCandidate(candidate, trace, recovery, ctx);
        boolean preferredReviewBet = finalPreferredReviewBetDomain(candidate, ctx);
        if (preferredReviewBet) {
            promising = true;
            markFinalPreferredReviewBetDomain(candidate, "FINAL_CHECK");
        }
        boolean lowFreqRescue = lowFreqRescueAllowed(runtime, candidate, ctx, strong, promising);
        boolean highFreqElite = highFreqEliteAllowed(runtime, candidate, ctx, strong, promising);

        QualifiedFrequencyFloorResult frequencyFloor =
                evaluateQualifiedFrequencyFloor(store, runtime, candidate, ctx);

        FinalCheck strictOos =
                strictOosFinalCheck(store, candidate, frequencyFloor, buildFullReason);
        if (strictOos != null && strictOos.allowBet) {
            return strictOos;
        }

        String hardBlock =
                hardBlockCode(
                        candidate,
                        trace,
                        recovery,
                        similar,
                        memory,
                        calibration,
                        runtime,
                        strong,
                        promising,
                        ctx);

        if (hardBlock.length() > 0
                && !(frequencyFloor.allowed && qualifiedProbeMayOverrideLegacyBlock(hardBlock))) {
            deny(
                    check,
                    hardBlock,
                    buildFullReason
                            ? buildHoldReason(
                                    candidate,
                                    trace,
                                    recovery,
                                    similar,
                                    memory,
                                    calibration,
                                    hardBlock,
                                    ctx)
                            : shortBlockReason(candidate, hardBlock),
                    canShadowObserve(candidate, hardBlock, clean));
            return check;
        }

        if (ctx.finalDecisionHardRisk && !ctx.finalDecisionStrong && !frequencyFloor.allowed) {
            deny(
                    check,
                    ctx.finalDecisionFreeze ? "FINAL_DECISION_FREEZE" : "FINAL_DECISION_HARD_RISK",
                    buildFullReason
                            ? "最终门：满级诊断/智能审判闭环判定当前候选存在硬风险或冻结风险，禁止真实BET；"
                                    + buildEvidence(
                                            candidate,
                                            trace,
                                            recovery,
                                            similar,
                                            memory,
                                            calibration,
                                            ctx)
                            : shortBlockReason(
                                    candidate,
                                    ctx.finalDecisionFreeze
                                            ? "FINAL_DECISION_FREEZE"
                                            : "FINAL_DECISION_HARD_RISK"),
                    true);
            return check;
        }

        if (ctx.finalDiagnosisRiskHigh
                && !ctx.finalDecisionStrong
                && candidate.finalP < 0.420d
                && !frequencyFloor.allowed) {
            deny(
                    check,
                    "FINAL_DIAGNOSIS_COUNTER_RISK",
                    buildFullReason
                            ? "最终门：概率诊断/模块效果/冻结矩阵反证偏高，且最终质量不足以对冲，转SHADOW/HOLD；"
                                    + buildEvidence(
                                            candidate,
                                            trace,
                                            recovery,
                                            similar,
                                            memory,
                                            calibration,
                                            ctx)
                            : shortBlockReason(candidate, "FINAL_DIAGNOSIS_COUNTER_RISK"),
                    true);
            return check;
        }

        String finalStrictBlock = finalBetStrictBlockCode(candidate, ctx);
        if (finalStrictBlock.length() > 0
                && !(frequencyFloor.allowed
                        && qualifiedProbeMayOverrideLegacyBlock(finalStrictBlock))) {
            deny(
                    check,
                    finalStrictBlock,
                    buildFullReason
                            ? "最终门：第5/6最终BET审判闭环拦截，禁止普通真实BET，转SHADOW/HOLD；"
                                    + finalBetStrictReason(candidate, ctx, finalStrictBlock)
                                    + buildEvidence(
                                            candidate,
                                            trace,
                                            recovery,
                                            similar,
                                            memory,
                                            calibration,
                                            ctx)
                            : shortBlockReason(candidate, finalStrictBlock),
                    true);
            return check;
        }

        double oldDynamicGate = candidate.dynamicGate;
        double execGate =
                calculateExecutionGate(
                        runtime, candidate, strong, promising, lowFreqRescue, highFreqElite, ctx);
        double minP =
                calculateMinProbability(
                        runtime, strong, promising, lowFreqRescue, highFreqElite, ctx);
        double minRaw = calculateRawFloor(strong, promising, lowFreqRescue, highFreqElite, ctx);
        double minEdge = calculateEdgeFloor(strong, promising, lowFreqRescue, highFreqElite, ctx);

        candidate.dynamicGate = execGate;
        candidate.edge = candidate.finalP - candidate.dynamicGate;

        if (!strong && !promising && !lowFreqRescue && !highFreqElite && !frequencyFloor.allowed) {
            deny(
                    check,
                    AppConfig.BLOCK_LOW_PROBABILITY,
                    buildFullReason
                            ? "最终门：候选未达到强候选/可用候选/低频救援/高频Elite标准；"
                                    + buildEvidence(
                                            candidate,
                                            trace,
                                            recovery,
                                            similar,
                                            memory,
                                            calibration,
                                            ctx)
                                    + "；原DynamicGate="
                                    + UiFormatter.probability(oldDynamicGate)
                                    + "；执行门="
                                    + UiFormatter.probability(execGate)
                            : shortBlockReason(candidate, AppConfig.BLOCK_LOW_PROBABILITY),
                    true);
            return check;
        }

        if (candidate.rawP < minRaw && !frequencyFloor.allowed) {
            deny(
                    check,
                    AppConfig.BLOCK_LOW_PROBABILITY,
                    buildFullReason
                            ? "最终门：rawP原始概率支撑不足；rawP="
                                    + UiFormatter.probability(candidate.rawP)
                                    + "，最低raw="
                                    + UiFormatter.probability(minRaw)
                                    + "；"
                                    + buildEvidence(
                                            candidate,
                                            trace,
                                            recovery,
                                            similar,
                                            memory,
                                            calibration,
                                            ctx)
                            : shortBlockReason(candidate, AppConfig.BLOCK_LOW_PROBABILITY),
                    true);
            return check;
        }

        if (candidate.finalP < minP && !frequencyFloor.allowed) {
            deny(
                    check,
                    AppConfig.BLOCK_LOW_PROBABILITY,
                    buildFullReason
                            ? "最终门：FinalP低于最低概率线；最低线="
                                    + UiFormatter.probability(minP)
                                    + "；"
                                    + buildFrequencyReason(runtime)
                                    + "；"
                                    + buildEvidence(
                                            candidate,
                                            trace,
                                            recovery,
                                            similar,
                                            memory,
                                            calibration,
                                            ctx)
                            : shortBlockReason(candidate, AppConfig.BLOCK_LOW_PROBABILITY),
                    true);
            return check;
        }

        if (candidate.finalP < execGate
                && !(frequencyFloor.allowed && candidate.finalP >= frequencyFloor.effectiveGate)) {
            deny(
                    check,
                    AppConfig.BLOCK_LOW_PROBABILITY,
                    buildFullReason
                            ? "最终门：FinalP没有超过动态执行门；执行门="
                                    + UiFormatter.probability(execGate)
                                    + "；原DynamicGate="
                                    + UiFormatter.probability(oldDynamicGate)
                                    + "；"
                                    + buildFrequencyReason(runtime)
                                    + "；"
                                    + buildEvidence(
                                            candidate,
                                            trace,
                                            recovery,
                                            similar,
                                            memory,
                                            calibration,
                                            ctx)
                            : shortBlockReason(candidate, AppConfig.BLOCK_LOW_PROBABILITY),
                    true);
            return check;
        }

        if (candidate.edge < minEdge
                && !(frequencyFloor.allowed
                        && frequencyFloor.rawEdge >= frequencyFloor.maxNegEdge)) {
            deny(
                    check,
                    AppConfig.BLOCK_LOW_PROBABILITY,
                    buildFullReason
                            ? "最终门：Edge不足；Edge="
                                    + UiFormatter.signedProbability(candidate.edge)
                                    + "，最低Edge="
                                    + UiFormatter.signedProbability(minEdge)
                                    + "；"
                                    + buildEvidence(
                                            candidate,
                                            trace,
                                            recovery,
                                            similar,
                                            memory,
                                            calibration,
                                            ctx)
                            : shortBlockReason(candidate, AppConfig.BLOCK_LOW_PROBABILITY),
                    true);
            return check;
        }

        if (!calibrationAllowsSoft(
                        calibration,
                        candidate.edge,
                        strong,
                        promising,
                        lowFreqRescue,
                        highFreqElite,
                        ctx)
                && !frequencyFloor.allowed) {
            deny(
                    check,
                    AppConfig.BLOCK_LOW_PROBABILITY,
                    buildFullReason
                            ? "最终门：校准误差偏高且优势不足；"
                                    + buildEvidence(
                                            candidate,
                                            trace,
                                            recovery,
                                            similar,
                                            memory,
                                            calibration,
                                            ctx)
                            : shortBlockReason(candidate, AppConfig.BLOCK_LOW_PROBABILITY),
                    true);
            return check;
        }

        if (ctx.teacherMatrixRisk
                && !ctx.teacherMatrixStrong
                && candidate.finalP < 0.400d
                && candidate.edge < 0.006d
                && !frequencyFloor.allowed) {
            deny(
                    check,
                    teacherMatrixBlockCode(),
                    buildFullReason
                            ? "最终门：Teacher Matrix显示当前状态+号码画像偏弱，且FinalP/Edge不足以对冲，暂不进入真实BET；"
                                    + buildEvidence(
                                            candidate,
                                            trace,
                                            recovery,
                                            similar,
                                            memory,
                                            calibration,
                                            ctx)
                            : shortBlockReason(candidate, teacherMatrixBlockCode()),
                    true);
            return check;
        }

        if (ctx.v20HardRisk
                && candidate.finalP < 0.430d
                && candidate.edge < 0.018d
                && !ctx.v20Elite) {
            deny(
                    check,
                    ctx.v20HardBlockCode.length() > 0
                            ? ctx.v20HardBlockCode
                            : "V20_CAUSE_HARD_RISK",
                    buildFullReason
                            ? "最终门：V20因链/规则/防线/伪强冻结出现硬风险，且FinalP/Edge不足以对冲；"
                                    + buildEvidence(
                                            candidate,
                                            trace,
                                            recovery,
                                            similar,
                                            memory,
                                            calibration,
                                            ctx)
                            : shortBlockReason(
                                    candidate,
                                    ctx.v20HardBlockCode.length() > 0
                                            ? ctx.v20HardBlockCode
                                            : "V20_CAUSE_HARD_RISK"),
                    true);
            return check;
        }

        if (ctx.decisionHardRisk
                && !ctx.decisionEvidenceElite
                && candidate.finalP < 0.430d
                && candidate.edge < 0.018d
                && !frequencyFloor.allowed) {
            deny(
                    check,
                    "V20_DECISION_EVIDENCE_HARD_RISK",
                    buildFullReason
                            ? "最终门：V20最终裁决证据矩阵出现硬风险，证据贡献不足以压过反证，禁止真实 BET；"
                                    + buildEvidence(
                                            candidate,
                                            trace,
                                            recovery,
                                            similar,
                                            memory,
                                            calibration,
                                            ctx)
                            : shortBlockReason(candidate, "V20_DECISION_EVIDENCE_HARD_RISK"),
                    true);
            return check;
        }

        if (ctx.decisionCounterRiskHigh
                && !ctx.decisionEvidenceStrong
                && !ctx.teacherMatrixStrong
                && !ctx.v20Strong
                && candidate.finalP < 0.410d
                && !frequencyFloor.allowed) {
            deny(
                    check,
                    "V20_DECISION_COUNTER_EVIDENCE",
                    buildFullReason
                            ? "最终门：V20最终裁决反证贡献偏高，且无强教师矩阵/V20因链/证据矩阵对冲，转 SHADOW/HOLD；"
                                    + buildEvidence(
                                            candidate,
                                            trace,
                                            recovery,
                                            similar,
                                            memory,
                                            calibration,
                                            ctx)
                            : shortBlockReason(candidate, "V20_DECISION_COUNTER_EVIDENCE"),
                    true);
            return check;
        }

        if (ctx.v20RiskHigh
                && !ctx.v20Strong
                && !ctx.teacherMatrixStrong
                && !ctx.learningStrong
                && candidate.finalP < 0.410d
                && !frequencyFloor.allowed) {
            deny(
                    check,
                    "V20_CAUSE_RISK",
                    buildFullReason
                            ? "最终门：V20因链综合风险偏高且没有强因链/强矩阵/强画像对冲；"
                                    + buildEvidence(
                                            candidate,
                                            trace,
                                            recovery,
                                            similar,
                                            memory,
                                            calibration,
                                            ctx)
                            : shortBlockReason(candidate, "V20_CAUSE_RISK"),
                    true);
            return check;
        }

        if (ctx.learningWeak
                && !strong
                && !promising
                && !lowFreqRescue
                && candidate.edge < 0.002d
                && candidate.finalP < 0.395d
                && !frequencyFloor.allowed) {
            deny(
                    check,
                    AppConfig.BLOCK_BACKTEST_WEAK,
                    buildFullReason
                            ? "最终门：学习画像偏弱，且当前Edge不足以覆盖学习风险；"
                                    + buildEvidence(
                                            candidate,
                                            trace,
                                            recovery,
                                            similar,
                                            memory,
                                            calibration,
                                            ctx)
                            : shortBlockReason(candidate, AppConfig.BLOCK_BACKTEST_WEAK),
                    true);
            return check;
        }

        if (ctx.realRiskHigh
                && !ctx.evidenceStrong
                && !ctx.teacherMatrixStrong
                && !ctx.learningStrong
                && !ctx.v20Strong
                && candidate.finalP < 0.405d
                && !frequencyFloor.allowed) {
            deny(
                    check,
                    AppConfig.BLOCK_LOW_PROBABILITY,
                    buildFullReason
                            ? "最终门：证据风险偏高且没有强证据/强矩阵/强画像/V20强因链对冲；"
                                    + buildEvidence(
                                            candidate,
                                            trace,
                                            recovery,
                                            similar,
                                            memory,
                                            calibration,
                                            ctx)
                            : shortBlockReason(candidate, AppConfig.BLOCK_LOW_PROBABILITY),
                    true);
            return check;
        }

        if (!hasRealBetEvidence(candidate, ctx, strong, promising, lowFreqRescue, highFreqElite)
                && !frequencyFloor.allowed) {
            deny(
                    check,
                    AppConfig.BLOCK_LOW_PROBABILITY,
                    buildFullReason
                            ? "最终门：真实BET主链证据不足；频率只软调门，TeacherMatrix为空中性，但仍需FinalP/RawP/执行门/硬风险/V20因链通过；"
                                    + buildEvidence(
                                            candidate,
                                            trace,
                                            recovery,
                                            similar,
                                            memory,
                                            calibration,
                                            ctx)
                            : shortBlockReason(candidate, AppConfig.BLOCK_LOW_PROBABILITY),
                    true);
            return check;
        }

        if (!frequencyFloor.allowed) {
            double normalGate =
                    candidate.dynamicGate > 0.0d
                            ? candidate.dynamicGate
                            : AppConfig.DEFAULT_DYNAMIC_GATE;
            double normalEdge = candidate.finalP - normalGate;
            if (candidate.finalP < AppConfig.QUALIFIED_FLOOR_MIN_FINAL_P
                    || candidate.finalP < normalGate
                    || normalEdge < 0.0d) {
                deny(
                        check,
                        AppConfig.BLOCK_LOW_PROBABILITY,
                        "NORMAL_BET最终出口未通过FinalP>=35%、FinalP>=DynamicGate、Edge>=0",
                        true);
                return check;
            }
            candidate.betDomain = AppConfig.BET_DOMAIN_NORMAL;
            candidate.decisionPath = AppConfig.DECISION_QUALITY_NORMAL;
            candidate.decisionQualityPath = AppConfig.DECISION_QUALITY_NORMAL;
        }
        applyFrequencyFloorDecisionFields(candidate, frequencyFloor);
        check.allowBet = true;
        check.allowShadow = false;
        check.blockCode = "";

        if (buildFullReason) {
            check.reason =
                    safeReason(
                            buildBetReason(
                                            candidate,
                                            trace,
                                            recovery,
                                            similar,
                                            memory,
                                            calibration,
                                            ctx)
                                    + "；最终门=V20_DYNAMIC_CAUSE_DECISION_GATE通过"
                                    + "；原DynamicGate="
                                    + UiFormatter.probability(oldDynamicGate)
                                    + "；执行门="
                                    + UiFormatter.probability(execGate)
                                    + "；最低线="
                                    + UiFormatter.probability(minP)
                                    + "；最低Raw="
                                    + UiFormatter.probability(minRaw)
                                    + "；最低Edge="
                                    + UiFormatter.signedProbability(minEdge)
                                    + "；FinalP="
                                    + UiFormatter.probability(candidate.finalP)
                                    + "；RawP="
                                    + UiFormatter.probability(candidate.rawP)
                                    + "；Edge="
                                    + UiFormatter.signedProbability(candidate.edge)
                                    + "；"
                                    + buildFrequencyReason(runtime)
                                    + "；TeacherMatrix为空时中性，不作为拦截理由"
                                    + "；V20因链只做最终证据和风险，不绕过DecisionGate"
                                    + "；统计口径=真实BET才计入胜率、频率、盈亏");
        } else {
            check.reason = shortPassReason(candidate);
        }

        return check;
    }

    private FinalCheck strictOosFinalCheck(
            DbStore store,
            Models.ComboCandidate candidate,
            QualifiedFrequencyFloorResult frequencyFloor,
            boolean fullReason) {
        if (candidate == null) return null;
        if (frequencyFloor != null
                && frequencyFloor.allowed
                && candidate.frequencyDebtScore <= 0.0d) {
            candidate.frequencyDebtScore = 1.0d;
        }
        OosEligibilityEngine.Evaluation e = OosEligibilityEngine.applyAndEvaluate(store, candidate);
        try {
            if (store != null) {
                store.setState("v28_oos_eligibility_version", OosEligibilityEngine.MODULE_VERSION);
                store.setState("v28_oos_eligibility_last_code", e == null ? "NULL" : e.code);
                store.setState(
                        "v28_oos_eligibility_last_summary", e == null ? "NULL" : e.summary());
            }
        } catch (Throwable ignore) {
        }
        if (e == null || !e.eligible) return null;

        FinalCheck out = new FinalCheck();
        out.allowBet = true;
        out.allowShadow = false;
        out.blockCode = "";
        if (e.realProbe) {
            candidate.betDomain = AppConfig.BET_DOMAIN_REAL_BET_PROBE;
            candidate.finalDecisionBetDomain = candidate.betDomain;
            candidate.decisionPath = AppConfig.DECISION_PATH_REAL_BET_PROBE;
            candidate.probeEligible = 1;
            candidate.realProbeAllowed = 1;
        } else if (e.qualifiedFloor) {
            applyFrequencyFloorDecisionFields(candidate, frequencyFloor);
            candidate.betDomain = AppConfig.BET_DOMAIN_QUALIFIED_FLOOR;
            candidate.finalDecisionBetDomain = candidate.betDomain;
            candidate.decisionPath = AppConfig.DECISION_PATH_QUALIFIED_FLOOR;
        } else {
            candidate.betDomain = AppConfig.BET_DOMAIN_NORMAL;
            candidate.finalDecisionBetDomain = candidate.betDomain;
            candidate.decisionPath = AppConfig.DECISION_PATH_NORMAL;
        }
        candidate.edge = candidate.finalP - candidate.dynamicGate;
        writeString(candidate, "finalDecisionStatus", "V40_DISTRIBUTION_OOS_READY");
        candidate.finalDecisionRejectCode = "";
        writeString(
                candidate, "finalDecisionActionAdvice", "BET授权候选：仍需DecisionAuthorizationEngine签名");
        out.reason =
                fullReason
                        ? "严格OOS最终门通过："
                                + e.summary()
                                + "｜旧趋势/形态/专家文本只作为排序证据，不再对已经严格校准且无类型化风险的候选重复软否决"
                                + "｜频率只区分NORMAL或QUALIFIED_FLOOR，不抬概率、不降质量线、不把WATCH转BET"
                        : "V40_DISTRIBUTION_OOS_READY｜" + e.code;
        return out;
    }

    private FinalCheck selectedBetFinalVerify(
            DbStore store,
            GateRuntimeContext runtime,
            Models.ComboCandidate candidate,
            List<Models.Draw> history,
            List<TrendStateMachine.TrendTrace> traces,
            List<BreakRecoveryEngine.RecoveryResult> recoveries) {

        FinalCheck out = new FinalCheck();

        if (candidate == null || !candidate.isValidK3()) {
            deny(out, AppConfig.BLOCK_COMBO_WEAK, "最终确认：候选为空或K3非法，取消BET", false);
            return out;
        }

        repairCandidateK3(candidate);

        TrendStateMachine.TrendTrace trace =
                trendStateMachine.findTraceForPosition(traces, candidate.position);
        BreakRecoveryEngine.RecoveryResult recovery =
                recoveryEngine.findForPosition(recoveries, candidate.position);
        Models.SimilarResult similar = safeSimilar(history, candidate);
        ShapeMemoryEngine.MemorySignal memory = safeMemory(candidate, history);
        CalibrationEngine.CalibrationSignal calibration = safeCalibration(store, candidate.rawP);

        DecisionContext ctx =
                buildDecisionContext(
                        runtime,
                        candidate,
                        trace,
                        recovery,
                        similar,
                        memory,
                        calibration,
                        history,
                        true);

        boolean strong = isStrongEnoughForRealtime(candidate, trace, recovery, ctx);
        boolean promising = isPromisingCandidate(candidate, trace, recovery, ctx);
        boolean preferredReviewBet = finalPreferredReviewBetDomain(candidate, ctx);
        if (preferredReviewBet) {
            promising = true;
            markFinalPreferredReviewBetDomain(candidate, "FINAL_VERIFY");
        }
        boolean lowFreqRescue = lowFreqRescueAllowed(runtime, candidate, ctx, strong, promising);
        boolean highFreqElite = highFreqEliteAllowed(runtime, candidate, ctx, strong, promising);
        QualifiedFrequencyFloorResult frequencyFloor =
                evaluateQualifiedFrequencyFloor(store, runtime, candidate, ctx);
        FinalCheck strictOos = strictOosFinalCheck(store, candidate, frequencyFloor, true);
        if (strictOos != null && strictOos.allowBet) {
            strictOos.reason = appendReason(strictOos.reason, "最终二次核验：严格OOS合同、概率、后验、风险和授权前置字段再次通过");
            return strictOos;
        }

        if (ctx.finalDecisionHardRisk && !ctx.finalDecisionStrong && !frequencyFloor.allowed) {
            deny(
                    out,
                    ctx.finalDecisionFreeze ? "FINAL_DECISION_FREEZE" : "FINAL_DECISION_HARD_RISK",
                    "最终确认：满级诊断/智能审判闭环判定硬风险或冻结风险，取消真实BET，转SHADOW观察；"
                            + buildEvidence(
                                    candidate, trace, recovery, similar, memory, calibration, ctx),
                    true);
            return out;
        }

        if (ctx.finalDiagnosisRiskHigh
                && !ctx.finalDecisionStrong
                && candidate.finalP < 0.420d
                && !frequencyFloor.allowed) {
            deny(
                    out,
                    "FINAL_DIAGNOSIS_COUNTER_RISK",
                    "最终确认：概率诊断/模块效果/冻结矩阵反证偏高，最终质量不足以对冲，取消真实BET；"
                            + buildEvidence(
                                    candidate, trace, recovery, similar, memory, calibration, ctx),
                    true);
            return out;
        }

        String verifyStrictBlock = finalBetStrictBlockCode(candidate, ctx);
        if (verifyStrictBlock.length() > 0
                && !(frequencyFloor.allowed
                        && qualifiedProbeMayOverrideLegacyBlock(verifyStrictBlock))) {
            deny(
                    out,
                    toVerifyBlockCode(verifyStrictBlock),
                    "最终确认：第5/6最终BET审判闭环复核未通过，取消真实BET，转SHADOW观察；"
                            + finalBetStrictReason(candidate, ctx, verifyStrictBlock)
                            + buildEvidence(
                                    candidate, trace, recovery, similar, memory, calibration, ctx),
                    true);
            return out;
        }

        double minRaw = calculateRawFloor(strong, promising, lowFreqRescue, highFreqElite, ctx);
        double minEdge = calculateEdgeFloor(strong, promising, lowFreqRescue, highFreqElite, ctx);

        if (candidate.rawP < minRaw && !frequencyFloor.allowed) {
            deny(
                    out,
                    AppConfig.BLOCK_LOW_PROBABILITY,
                    "最终确认：rawP原始概率支撑不足，取消真实BET，转SHADOW观察；"
                            + buildEvidence(
                                    candidate, trace, recovery, similar, memory, calibration, ctx),
                    true);
            return out;
        }

        if (candidate.finalP < candidate.dynamicGate
                && !(frequencyFloor.allowed && candidate.finalP >= frequencyFloor.effectiveGate)) {
            deny(
                    out,
                    AppConfig.BLOCK_LOW_PROBABILITY,
                    "最终确认：FinalP未过动态执行门，取消真实BET，转SHADOW观察；"
                            + buildEvidence(
                                    candidate, trace, recovery, similar, memory, calibration, ctx),
                    true);
            return out;
        }

        if (candidate.edge < minEdge
                && !(frequencyFloor.allowed
                        && frequencyFloor.rawEdge >= frequencyFloor.maxNegEdge)) {
            deny(
                    out,
                    AppConfig.BLOCK_LOW_PROBABILITY,
                    "最终确认：Edge不足，取消真实BET，转SHADOW观察；"
                            + buildEvidence(
                                    candidate, trace, recovery, similar, memory, calibration, ctx),
                    true);
            return out;
        }

        if (trace != null && trace.hardBlock == 1 && !frequencyFloor.allowed) {
            deny(
                    out,
                    trendWeakBlockCode(),
                    "最终确认：趋势层硬拦截，取消真实BET；"
                            + buildEvidence(
                                    candidate, trace, recovery, similar, memory, calibration, ctx),
                    true);
            return out;
        }

        if (recovery != null && recoveryEngine.isHardBlock(recovery) && !frequencyFloor.allowed) {
            deny(
                    out,
                    recovery.blockCode == null || recovery.blockCode.length() == 0
                            ? AppConfig.BLOCK_RECOVERY_WEAK
                            : recovery.blockCode,
                    "最终确认：断点恢复层硬拦截，取消真实BET；"
                            + buildEvidence(
                                    candidate, trace, recovery, similar, memory, calibration, ctx),
                    true);
            return out;
        }

        if (similar != null
                && similar.effectiveSample >= AppConfig.EFFECTIVE_SAMPLE_HIGH
                && similar.similarHitRate < 0.270d
                && candidate.finalP < 0.400d
                && !strong
                && !ctx.teacherMatrixStrong
                && !ctx.learningStrong
                && !ctx.v20Strong
                && !frequencyFloor.allowed) {
            deny(
                    out,
                    AppConfig.BLOCK_SAMPLE_WEAK,
                    "最终确认：相似形态样本充足但命中率过低，取消真实BET；"
                            + buildEvidence(
                                    candidate, trace, recovery, similar, memory, calibration, ctx),
                    true);
            return out;
        }

        if (memory != null
                && !memoryEngine.memoryAllowsBet(memory)
                && candidate.finalP < 0.395d
                && candidate.edge < 0.002d
                && !strong
                && !ctx.teacherMatrixStrong
                && !ctx.learningStrong
                && !ctx.v20Strong
                && !frequencyFloor.allowed) {
            deny(
                    out,
                    AppConfig.BLOCK_COMBO_WEAK,
                    "最终确认：形态记忆明显不支持真实BET，取消真实BET；"
                            + buildEvidence(
                                    candidate, trace, recovery, similar, memory, calibration, ctx),
                    true);
            return out;
        }

        if (calibration != null
                && calibration.error > AppConfig.CALIBRATION_ERROR_HARD
                && candidate.edge < 0.006d
                && candidate.finalP < 0.400d
                && !strong
                && !ctx.teacherMatrixStrong
                && !ctx.learningStrong
                && !ctx.v20Strong
                && !frequencyFloor.allowed) {
            deny(
                    out,
                    AppConfig.BLOCK_LOW_PROBABILITY,
                    "最终确认：校准误差过高且Edge不足，取消真实BET；"
                            + buildEvidence(
                                    candidate, trace, recovery, similar, memory, calibration, ctx),
                    true);
            return out;
        }

        if (ctx.teacherMatrixRisk
                && !ctx.teacherMatrixStrong
                && candidate.finalP < 0.400d
                && candidate.edge < 0.006d
                && !frequencyFloor.allowed) {
            deny(
                    out,
                    teacherMatrixBlockCode(),
                    "最终确认：Teacher Matrix为RISK且未达到强证据对冲，取消真实BET；"
                            + buildEvidence(
                                    candidate, trace, recovery, similar, memory, calibration, ctx),
                    true);
            return out;
        }

        if (ctx.v20HardRisk
                && !ctx.v20Elite
                && candidate.finalP < 0.430d
                && candidate.edge < 0.018d
                && !frequencyFloor.allowed) {
            deny(
                    out,
                    ctx.v20HardBlockCode.length() > 0
                            ? ctx.v20HardBlockCode
                            : "V20_CAUSE_HARD_RISK",
                    "最终确认：V20因链/第三防线/伪强冻结/规则健康存在硬风险，取消真实BET；"
                            + buildEvidence(
                                    candidate, trace, recovery, similar, memory, calibration, ctx),
                    true);
            return out;
        }

        if (ctx.decisionHardRisk
                && !ctx.decisionEvidenceElite
                && candidate.finalP < 0.430d
                && candidate.edge < 0.018d
                && !frequencyFloor.allowed) {
            deny(
                    out,
                    "V20_DECISION_EVIDENCE_HARD_RISK",
                    "最终确认：V20最终裁决证据矩阵为硬风险，取消真实 BET，转 SHADOW 观察；"
                            + buildEvidence(
                                    candidate, trace, recovery, similar, memory, calibration, ctx),
                    true);
            return out;
        }

        if (ctx.decisionCounterRiskHigh
                && !ctx.decisionEvidenceStrong
                && !ctx.teacherMatrixStrong
                && !ctx.v20Strong
                && candidate.finalP < 0.410d
                && !frequencyFloor.allowed) {
            deny(
                    out,
                    "V20_DECISION_COUNTER_EVIDENCE",
                    "最终确认：V20最终裁决反证贡献偏高且强证据不足，取消真实 BET，转 SHADOW 观察；"
                            + buildEvidence(
                                    candidate, trace, recovery, similar, memory, calibration, ctx),
                    true);
            return out;
        }

        if (ctx.v20RiskHigh
                && !ctx.v20Strong
                && !ctx.teacherMatrixStrong
                && !ctx.learningStrong
                && candidate.finalP < 0.410d
                && !frequencyFloor.allowed) {
            deny(
                    out,
                    "V20_CAUSE_RISK",
                    "最终确认：V20综合风险偏高且没有强因链/强矩阵/强画像对冲，取消真实BET；"
                            + buildEvidence(
                                    candidate, trace, recovery, similar, memory, calibration, ctx),
                    true);
            return out;
        }

        if (ctx.realRiskHigh
                && !ctx.evidenceStrong
                && !ctx.teacherMatrixStrong
                && !ctx.learningStrong
                && !ctx.v20Strong
                && candidate.finalP < 0.405d
                && !frequencyFloor.allowed) {
            deny(
                    out,
                    AppConfig.BLOCK_LOW_PROBABILITY,
                    "最终确认：证据风险偏高且没有强证据/强矩阵/强画像/V20强因链对冲，取消真实BET；"
                            + buildEvidence(
                                    candidate, trace, recovery, similar, memory, calibration, ctx),
                    true);
            return out;
        }

        if (!frequencyFloor.allowed) {
            double normalGate =
                    candidate.dynamicGate > 0.0d
                            ? candidate.dynamicGate
                            : AppConfig.DEFAULT_DYNAMIC_GATE;
            double normalEdge = candidate.finalP - normalGate;
            if (candidate.finalP < AppConfig.QUALIFIED_FLOOR_MIN_FINAL_P
                    || candidate.finalP < normalGate
                    || normalEdge < 0.0d) {
                deny(
                        out,
                        AppConfig.BLOCK_LOW_PROBABILITY,
                        "NORMAL_BET二次确认未通过FinalP>=35%、FinalP>=DynamicGate、Edge>=0",
                        true);
                return out;
            }
            candidate.betDomain = AppConfig.BET_DOMAIN_NORMAL;
            candidate.decisionPath = AppConfig.DECISION_QUALITY_NORMAL;
            candidate.decisionQualityPath = AppConfig.DECISION_QUALITY_NORMAL;
        }
        applyFrequencyFloorDecisionFields(candidate, frequencyFloor);
        out.allowBet = true;
        out.allowShadow = false;
        out.blockCode = "";
        out.reason =
                "最终确认通过：rawP、FinalP、动态门、Edge、趋势硬风险、恢复硬风险、校准风险、TeacherMatrix风险、V20因链风险均通过；真实胜率不硬刹车；频率只软调门；TeacherMatrix为空不拦截";

        return out;
    }

    private String buildSelectedFullReason(
            DbStore store,
            GateRuntimeContext runtime,
            Models.ComboCandidate c,
            List<Models.Draw> history,
            List<TrendStateMachine.TrendTrace> traces,
            List<BreakRecoveryEngine.RecoveryResult> recoveries,
            FinalCheck check) {

        if (c == null) {
            return check == null ? "" : safeReason(check.reason);
        }

        try {
            TrendStateMachine.TrendTrace trace =
                    trendStateMachine.findTraceForPosition(traces, c.position);
            BreakRecoveryEngine.RecoveryResult recovery =
                    recoveryEngine.findForPosition(recoveries, c.position);
            Models.SimilarResult similar = safeSimilar(history, c);
            ShapeMemoryEngine.MemorySignal memory = safeMemory(c, history);
            CalibrationEngine.CalibrationSignal calibration = safeCalibration(store, c.rawP);

            DecisionContext ctx =
                    buildDecisionContext(
                            runtime,
                            c,
                            trace,
                            recovery,
                            similar,
                            memory,
                            calibration,
                            history,
                            true);

            double freq100 =
                    runtime == null
                            ? recentBetFrequency(store, freqWindowShort())
                            : runtime.freq100;
            double freq500 =
                    runtime == null ? recentBetFrequency(store, freqWindowMid()) : runtime.freq500;
            double freq1000 =
                    runtime == null
                            ? recentBetFrequency(store, freqWindowLong())
                            : runtime.freq1000;

            if (check != null && check.allowBet) {
                return safeReason(
                        buildBetReason(c, trace, recovery, similar, memory, calibration, ctx)
                                + "；最终门=V20_DYNAMIC_CAUSE_DECISION_GATE通过"
                                + "；最终确认=已通过rawP/FinalP/动态门/Edge/硬风险/校准/教师矩阵/V20因链风险检查"
                                + "；执行门="
                                + UiFormatter.probability(c.dynamicGate)
                                + "；FinalP="
                                + UiFormatter.probability(c.finalP)
                                + "；RawP="
                                + UiFormatter.probability(c.rawP)
                                + "；Edge="
                                + UiFormatter.signedProbability(c.edge)
                                + "；Freq100="
                                + UiFormatter.probability(freq100)
                                + "；Freq500="
                                + UiFormatter.probability(freq500)
                                + "；Freq1000="
                                + UiFormatter.probability(freq1000)
                                + "；"
                                + buildFrequencyReason(runtime)
                                + "；策略=低频强信号可放行，高频只轻抬门，TeacherMatrix为空中性，不用真实胜率硬刹车"
                                + "；V20因链/规则健康/第三防线/影子健康/伪强冻结只参与最终审计，不绕过最终门"
                                + "；统计口径=真实BET才计入胜率/频率/盈亏");
            }

            String block = check == null ? AppConfig.BLOCK_LOW_PROBABILITY : check.blockCode;
            String base =
                    buildHoldReason(c, trace, recovery, similar, memory, calibration, block, ctx);

            if (check != null && check.allowShadow) {
                base = appendReason(base, "最终门未允许真实BET，但允许SHADOW观察学习；SHADOW不计真实胜率、不计真实频率、不计真实盈亏");
            } else {
                base = appendReason(base, "最终门未允许真实BET，输出正式HOLD；HOLD不计真实胜率、不计真实频率、不计真实盈亏");
            }

            base = appendReason(base, buildFrequencyReason(runtime));
            base =
                    appendReason(
                            base,
                            "当前版本=V20_DYNAMIC_CAUSE_DECISION_GATE；不是用真实低胜率硬刹车；TeacherMatrix为空不拦截；V20因链只审计风险和证据");

            return safeReason(base);
        } catch (Throwable e) {
            if (check == null || check.reason == null || check.reason.length() == 0) {
                return shortBlockReason(
                        c, check == null ? AppConfig.BLOCK_LOW_PROBABILITY : check.blockCode);
            }
            return safeReason(check.reason);
        }
    }

    private Models.ComboCandidate pickBestValidShadowCandidate(
            DbStore store,
            List<Models.ComboCandidate> candidates,
            String baseIssue,
            String targetIssue) {
        if (candidates == null || candidates.size() == 0) return null;
        Models.ComboCandidate best = null;
        double bestScore = -999999.0d;
        for (int i = 0; i < candidates.size(); i++) {
            Models.ComboCandidate raw = candidates.get(i);
            if (raw == null || !raw.isValidK3()) continue;
            Models.ComboCandidate c = raw.copy();
            copyRuntimeBridgeFields(raw, c);
            try {
                if (store != null) store.normalizeClosedLoopEvidenceAliases(c);
            } catch (Throwable e) {
            }
            c.baseIssue = baseIssue == null ? "" : baseIssue;
            c.targetIssue = targetIssue == null ? "" : targetIssue;
            repairCandidateK3(c);
            if (!c.isValidK3()) continue;
            c.dynamicGate = cleanIncomingGate(c.dynamicGate, AppConfig.DEFAULT_DYNAMIC_GATE);
            double fallbackP = strongestAvailableProbability(c);
            if (c.finalP <= 0.0d) c.finalP = fallbackP;
            if (c.rawP <= 0.0d) c.rawP = fallbackP;
            c.finalP = AppConfig.clampProbability(c.finalP);
            c.rawP = AppConfig.clampProbability(c.rawP);
            c.edge = c.finalP - c.dynamicGate;
            Models.freezeStableEvidenceDomains(c, targetIssue);
            String domain = AppConfig.sanitizePromotionDomainKey(c.promotionFineDomain);
            c.shadowPromotionDomain = domain;
            c.promotionDomainAvailable = domain.length() > 0 ? 1 : 0;
            double score = quickFinalScore(c, null, null);
            if (best == null || score > bestScore) {
                best = c;
                bestScore = score;
            }
        }
        return best;
    }

    private double strongestAvailableProbability(Models.ComboCandidate c) {
        if (c == null) return AppConfig.RANDOM_K3_BASELINE;
        double p = c.finalP;
        if (c.calibratedP > p) p = c.calibratedP;
        if (c.rawP > p) p = c.rawP;
        if (c.modelP > p) p = c.modelP;
        if (c.comboP > p) p = c.comboP;
        if (c.shapeP > p) p = c.shapeP;
        if (c.recoveryP > p) p = c.recoveryP;
        if (c.similarP > p) p = c.similarP;
        if (c.memoryP > p) p = c.memoryP;
        if (p <= 0.0d) p = AppConfig.RANDOM_K3_BASELINE;
        return AppConfig.clampProbability(p);
    }

    private String buildSafePreDrawPromotionDomain(Models.ComboCandidate c) {
        if (c == null || !c.isValidK3()) return "";
        Models.freezeStableEvidenceDomains(c, c.targetIssue);
        return AppConfig.sanitizePromotionDomainKey(c.promotionFineDomain);
    }

    private String resolveQualifiedProbeRejectReason(
            Models.ComboCandidate c,
            double riskGate,
            double rawEdge,
            double coverage,
            double third,
            double need,
            double counter,
            String adaptive,
            String promotion,
            String rule,
            boolean recentLoseHigh,
            boolean hardStale) {
        if (hardStale) return "FINAL_SNAPSHOT_HARD_STALE";
        if (c == null || c.finalP < AppConfig.V42_PROBE_MIN_P)
            return "FINAL_P_BELOW_ABSOLUTE_FLOOR";
        if (rawEdge < AppConfig.REAL_BET_PROBE_MAX_NEG_EDGE) return "FINAL_EDGE_BELOW_PROBE_FLOOR";
        if (riskGate - c.finalP > AppConfig.REAL_BET_PROBE_MAX_GATE_GAP)
            return "FINAL_GATE_GAP_TOO_LARGE";
        if (coverage < AppConfig.REAL_BET_PROBE_MIN_COVERAGE) return "FINAL_COVERAGE_NOT_QUALIFIED";
        if (third < AppConfig.REAL_BET_PROBE_MIN_THIRD) return "FINAL_THIRD_NOT_QUALIFIED";
        if (need < AppConfig.REAL_BET_PROBE_MIN_NEED_SUPPLY)
            return "FINAL_NEED_SUPPLY_NOT_QUALIFIED";
        if (counter > AppConfig.REAL_BET_PROBE_MAX_COUNTER_RISK) return "FINAL_COUNTER_RISK_HIGH";
        if (readInt(c, "hardBlockFlag", 0) == 1
                || AppConfig.isExplicitHardBlockStatus(readString(c, "riskSeverity", "")))
            return "FINAL_STRUCTURED_HARD_BLOCK";
        if (AppConfig.isExplicitHardBlockStatus(adaptive)) return "FINAL_ADAPTIVE_HARD_RISK";
        if (AppConfig.isExplicitHardBlockStatus(rule)) return "FINAL_RULE_HEALTH_HARD_RISK";
        if (recentLoseHigh) return "FINAL_RECENT_LOSE_RISK_HIGH";
        return "FINAL_QUALIFIED_PROBE_FALSE";
    }

    private Models.FrequencyState loadFrequencyStateSafe(DbStore store) {
        Models.FrequencyState s = null;
        if (store != null) {
            try {
                s = store.loadFrequencyState();
            } catch (Throwable e) {
                s = null;
            }
        }
        if (s == null) s = new Models.FrequencyState();
        return s;
    }

    private QualifiedFrequencyFloorResult evaluateQualifiedFrequencyFloor(
            DbStore store,
            GateRuntimeContext runtime,
            Models.ComboCandidate c,
            DecisionContext ctx) {
        QualifiedFrequencyFloorResult r = new QualifiedFrequencyFloorResult();
        if (c == null || !c.isValidK3()) {
            r.reason = "INVALID_K3";
            return r;
        }
        Models.FrequencyState fs = null;
        try {
            fs = store == null ? null : store.loadRollingFrequencyStateV20();
        } catch (Throwable ignore) {
        }
        if (fs == null) fs = runtime != null ? runtime.frequencyState : null;
        if (fs == null) fs = new Models.FrequencyState();
        c.realBetFrequency20 = fs.frequency20;
        c.realBetFrequency50 = fs.frequency50;
        c.realBetFrequency100 = fs.frequency100;
        c.realBetFrequency300 = fs.frequency300;
        c.frequencyObserved20 = fs.observed20;
        c.frequencyObserved50 = fs.observed50;
        c.frequencyObserved100 = fs.observed100;
        c.frequencyObserved300 = fs.observed300;
        c.frequencyBetCount20 = fs.betCount20;
        c.frequencyBetCount50 = fs.betCount50;
        c.frequencyBetCount100 = fs.betCount100;
        c.frequencyBetCount300 = fs.betCount300;
        c.frequencyDebt20 = fs.floorDebt20;
        c.frequencyDebt50 = fs.floorDebt50;
        c.frequencyDebt100 = fs.floorDebt100;
        c.frequencyDebt300 = fs.floorDebt300;
        c.frequencyTargetDebt20 = fs.targetDebt20;
        c.frequencyTargetDebt50 = fs.targetDebt50;
        c.frequencyTargetDebt100 = fs.targetDebt100;
        c.frequencyTargetDebt300 = fs.targetDebt300;
        c.frequencyUrgentWindow = fs.urgentWindow;
        c.frequencyIssuesSinceLastRealBet = fs.issuesSinceLastRealBet;
        c.frequencyIssuesSinceLastFloorBet = fs.issuesSinceLastQualifiedFloorBet;
        c.frequencyConsecutiveFloorBets = fs.consecutiveQualifiedFloorBets;
        c.frequencyFloorMinGap = fs.floorPacingMinGap;
        c.frequencyFloorPacingReady = fs.floorPacingReady;
        c.frequencyFloorPacingReason = fs.floorPacingReason;
        c.frequencyFloorRequiredCount = AppConfig.frequencyFloorCount(100, fs.observed100);
        c.frequencyFloorTargetCount = AppConfig.frequencyTargetCount(100, fs.observed100);
        c.frequencyFloorActualCount = fs.betCount100;
        c.frequencyFloorDebtCount = fs.floorDebt100;
        c.frequencyTargetDebtCount = fs.targetDebt100;
        c.frequencyFloorNoUpperCap = 1;
        c.frequencyFloorVersion = AppConfig.ACTIVE_QUALITY_BUILD_VERSION;
        c.moduleBuildVersion = AppConfig.ACTIVE_QUALITY_BUILD_VERSION;
        if (fs.urgentWindow == null || fs.urgentWindow.length() == 0) {
            r.reason = "MULTI_WINDOW_FREQUENCY_SATISFIED_NO_CAP";
            return r;
        }
        if (fs.floorPacingReady == 0) {
            r.reason = "FLOOR_PACING_WAIT_" + fs.floorPacingReason;
            c.probeRejectReason = r.reason;
            c.observeReason = r.reason;
            return r;
        }
        if (runtime != null && runtime.openTradeExists) {
            r.reason = "OPEN_TRADE";
            c.probeRejectReason = r.reason;
            return r;
        }
        if (readInt(c, "hardBlockFlag", 0) == 1
                || AppConfig.isNonOverridableFloorBlock(readString(c, "hardRiskCode", ""))
                || AppConfig.isExplicitHardBlockStatus(readString(c, "riskSeverity", ""))) {
            r.reason = "STRUCTURED_HARD_BLOCK";
            c.probeRejectReason = r.reason;
            return r;
        }
        double normalGate =
                c.riskAdjustedGate > 0.0d
                        ? c.riskAdjustedGate
                        : (c.dynamicGate > 0.0d ? c.dynamicGate : AppConfig.DEFAULT_DYNAMIC_GATE);
        double normalEdge = c.finalP - normalGate;
        double floorReference = AppConfig.QUALIFIED_FLOOR_REFERENCE_P;
        double floorQualityEdge = AppConfig.qualifiedFloorQualityEdge(c.finalP);
        double raw = c.rawP > 0.0d ? c.rawP : c.calibratedP;
        double cover = c.coverageQuality > 0.0d ? c.coverageQuality : c.coverageQualityScore;
        double third = c.thirdQuality > 0.0d ? c.thirdQuality : c.thirdNumberQualityScore;
        double counter = c.counterRisk > 0.0d ? c.counterRisk : c.counterEvidenceScore;
        boolean coverageReady = c.coverageAvailable == 1 || cover > 0.0d;
        boolean thirdReady = c.thirdQualityAvailable == 1 || third > 0.0d;
        if (!coverageReady || !thirdReady) {
            r.reason = "QUALITY_FIELDS_UNAVAILABLE";
            c.probeRejectReason = r.reason;
            return r;
        }
        c.coverageAvailable = 1;
        c.thirdQualityAvailable = 1;
        if (!AppConfig.isQualifiedFloorQuality(
                c.finalP, raw, floorQualityEdge, cover, third, counter)) {
            r.reason = "BELOW_QUALIFIED_FLOOR_QUALITY";
            c.probeRejectReason = r.reason;
            return r;
        }
        int dynSample =
                Math.max(c.dynamicClusterSample, c.dynamicRealSample + c.dynamicShadowSample);
        double upper = readDouble(c, "dynamicWilsonUpperBound", 0.0d);
        if (AppConfig.isMatureDynamicHardBlock(
                c.dynamicClusterStatus, dynSample, c.dynamicClusterHitRate, upper)) {
            r.reason = "MATURE_DYNAMIC_HARD_BLOCK";
            c.probeRejectReason = r.reason;
            return r;
        }
        String teacher = (c.teacherStatus == null ? "" : c.teacherStatus).toUpperCase(java.util.Locale.ROOT);
        int teacherSample =
                Math.max(
                        c.teacherNumberHistorySample,
                        Math.max(c.teacherMatrixSample, c.teacherNumberSample));
        if (teacherSample >= AppConfig.DYNAMIC_HARD_BLOCK_MIN_SAMPLE
                && (teacher.indexOf("RISK") >= 0
                        || teacher.indexOf("FREEZE") >= 0
                        || teacher.indexOf("BLOCK") >= 0)) {
            r.reason = "MATURE_TEACHER_RISK";
            c.probeRejectReason = r.reason;
            return r;
        }
        Models.freezeStableEvidenceDomains(c, c.targetIssue);
        c.frequencyFloorRankScore = quickFinalScore(c, null, null);
        c.qualifiedProbe = 1;
        c.realProbeAllowed = 1;
        c.probeEligible = 1;
        c.shadowProbeEligible = 1;
        c.preDecisionOpportunity = 1;
        c.frequencyFloorMandatory =
                (fs.floorDebt20 + fs.floorDebt50 + fs.floorDebt100 + fs.floorDebt300) > 0 ? 1 : 0;
        c.blockTier = "QUALIFIED_FLOOR_" + fs.urgentWindow;
        c.observeReason =
                "ROLLING_WINDOW_DEBT_"
                        + fs.urgentWindow
                        + "|FloorRef="
                        + floorReference
                        + "|FloorEdge="
                        + floorQualityEdge
                        + "|NormalGate="
                        + normalGate
                        + "|NormalEdge="
                        + normalEdge;
        c.riskSeverity = AppConfig.EVIDENCE_SEVERITY_WATCH;
        c.probeLevel = "QUALIFIED_" + fs.urgentWindow;
        c.probeRejectReason = "";
        c.betDomain = AppConfig.BET_DOMAIN_QUALIFIED_FLOOR;
        c.decisionPath = AppConfig.DECISION_PATH_QUALIFIED_FLOOR;
        c.decisionQualityPath = AppConfig.DECISION_QUALITY_FLOOR;
        r.effectiveGate = floorReference;
        r.rawEdge = floorQualityEdge;
        r.maxNegEdge = AppConfig.QUALIFIED_FLOOR_MIN_EDGE;
        r.probeEdge = floorQualityEdge;
        r.allowed = true;
        r.betDomain = AppConfig.BET_DOMAIN_QUALIFIED_FLOOR;
        r.decisionPath = AppConfig.DECISION_PATH_QUALIFIED_FLOOR;
        r.reason = "QUALIFIED_MULTI_WINDOW_FLOOR_" + fs.urgentWindow;
        return r;
    }

    /*
     * 仅允许合格探针绕过旧链路中的“软样本/软门槛”阻断。
     * 数据、漂移、断点、趋势、冻结、硬反证、硬覆盖、硬第三号风险永远不可绕过。
     */
    private boolean qualifiedProbeMayOverrideLegacyBlock(String code) {
        return !AppConfig.isNonOverridableFloorBlock(code);
    }

    private double safeProbeFrequency(DbStore store, int lookback) {
        if (store == null) return 0.0d;
        try {
            return store.getRecentProbeFrequency(lookback);
        } catch (Throwable e) {
            return 0.0d;
        }
    }

    private void applyFrequencyFloorDecisionFields(
            Models.ComboCandidate c, QualifiedFrequencyFloorResult r) {
        if (c == null) return;
        if (r != null && r.allowed) {
            c.qualifiedProbe = 1;
            c.realProbeAllowed = 1;
            c.effectiveProbeGate = r.effectiveGate;
            c.rawEdge = r.rawEdge;
            c.effectiveProbeEdge = r.probeEdge;
            c.betDomain = r.betDomain;
            c.finalDecisionBetDomain = r.betDomain;
            c.decisionPath = r.decisionPath;
            c.probeRejectReason = "";
        } else {
            if (c.betDomain == null || c.betDomain.length() == 0)
                c.betDomain = AppConfig.BET_DOMAIN_NORMAL;
            if (c.finalDecisionBetDomain == null || c.finalDecisionBetDomain.length() == 0)
                c.finalDecisionBetDomain = c.betDomain;
            if (c.decisionPath == null || c.decisionPath.length() == 0)
                c.decisionPath = AppConfig.DECISION_PATH_NORMAL;
        }
        c.decisionVersionV2 = LINK_MODIFICATION_V2_DECISION_GATE_VERSION;
        c.probeEligibilityVersion = LINK_MODIFICATION_V2_DECISION_GATE_VERSION;
    }

    private void copyLinkModificationV2DecisionFields(Models.Decision d, Models.ComboCandidate c) {
        if (d == null || c == null) return;
        d.realBetFrequency50 = c.realBetFrequency50;
        d.realBetFrequency100 = c.realBetFrequency100;
        d.realBetFrequency300 = c.realBetFrequency300;
        d.frequencyDebtScore = c.frequencyDebtScore;
        d.frequencySupportScore = c.frequencySupportScore;
        d.frequencyGateAdjust = c.frequencyGateAdjust;
        d.qualifiedProbeScore = c.qualifiedProbeScore;
        d.shadowPromotionScore = c.shadowPromotionScore;
        d.baseGate = c.baseGate;
        d.riskAdjustedGate = c.riskAdjustedGate > 0.0d ? c.riskAdjustedGate : c.dynamicGate;
        d.effectiveProbeGate =
                c.effectiveProbeGate > 0.0d ? c.effectiveProbeGate : d.riskAdjustedGate;
        d.rawEdge = c.rawEdge != 0.0d ? c.rawEdge : c.finalP - d.riskAdjustedGate;
        d.effectiveProbeEdge =
                c.effectiveProbeEdge != 0.0d
                        ? c.effectiveProbeEdge
                        : c.finalP - d.effectiveProbeGate;
        d.needSupplyScore = c.needSupplyScore;
        d.counterRisk = c.counterRisk;
        d.qualifiedProbe = c.qualifiedProbe;
        d.coverageAvailable = c.coverageAvailable;
        d.thirdQualityAvailable = c.thirdQualityAvailable;
        d.promotionDomainAvailable = c.promotionDomainAvailable;
        d.frequencyState = safeTextLocal(c.frequencyState);
        d.frequencyStateVersion = LINK_MODIFICATION_V2_DECISION_GATE_VERSION;
        d.shadowPromotionDomain = safeTextLocal(c.shadowPromotionDomain);
        d.decisionPath = safeTextLocal(c.decisionPath);
        d.betDomain = safeTextLocal(c.betDomain);
        d.probeRejectReason = safeTextLocal(c.probeRejectReason);
        d.probeEligibilityVersion = LINK_MODIFICATION_V2_DECISION_GATE_VERSION;
        d.ruleHealthStatus = safeTextLocal(c.ruleHealthStatus);
        d.snapshotFreshness = safeTextLocal(c.snapshotFreshness);
        d.warmProfileFreshness = safeTextLocal(c.warmProfileFreshness);
        d.coldProfileFreshness = safeTextLocal(c.coldProfileFreshness);
        d.deepProfileFreshness = safeTextLocal(c.deepProfileFreshness);
        d.decisionVersionV2 = LINK_MODIFICATION_V2_DECISION_GATE_VERSION;
        d.evidenceAsOfIssue = safeTextLocal(c.evidenceAsOfIssue);
        d.snapshotAsOfIssue = safeTextLocal(c.snapshotAsOfIssue);
        d.snapshotCreatedAt = c.snapshotCreatedAt;
        d.frequencyStateUpdatedAt = c.frequencyStateUpdatedAt;
        d.realTrade =
                AppConfig.DECISION_BET.equals(AppConfig.normalizeDecision(d.decision)) ? 1 : 0;
        if (d.realTrade == 1 && (d.betDomain == null || d.betDomain.length() == 0)) {
            d.betDomain = AppConfig.BET_DOMAIN_NORMAL;
            d.decisionPath = AppConfig.DECISION_PATH_NORMAL;
        }
        d.finalDecisionBetDomain = d.betDomain;
        d.preDecisionOpportunity = c.preDecisionOpportunity;
        d.riskSeverity =
                c.riskSeverity == null ? AppConfig.EVIDENCE_SEVERITY_NEUTRAL : c.riskSeverity;
        d.hardBlockFlag = c.hardBlockFlag;
        d.hardBlockSource = c.hardBlockSource == null ? "" : c.hardBlockSource;
        d.evidenceSource = c.evidenceSource == null ? "NONE" : c.evidenceSource;
        d.evidenceSettledOnly = c.evidenceSettledOnly == 0 ? 1 : c.evidenceSettledOnly;
        d.evidenceSampleCount = c.evidenceSampleCount;
        d.evidenceHitCount = c.evidenceHitCount;
        d.evidenceMissCount = c.evidenceMissCount;
        d.promotionCoarseDomain = c.promotionCoarseDomain;
        d.promotionMidDomain = c.promotionMidDomain;
        d.promotionFineDomain = c.promotionFineDomain;
        d.frequencyFloorRequiredCount = c.frequencyFloorRequiredCount;
        d.frequencyFloorTargetCount = c.frequencyFloorTargetCount;
        d.frequencyFloorActualCount = c.frequencyFloorActualCount;
        d.frequencyFloorDebtCount = c.frequencyFloorDebtCount;
        d.frequencyTargetDebtCount = c.frequencyTargetDebtCount;
        d.frequencyFloorMandatory = c.frequencyFloorMandatory;
        d.frequencyFloorNoUpperCap = c.frequencyFloorNoUpperCap;
        d.frequencyFloorRankScore = c.frequencyFloorRankScore;
        d.frequencyFloorVersion = c.frequencyFloorVersion;
        d.historicalPriorP = c.historicalPriorP;
        d.historicalPriorConfidence = c.historicalPriorConfidence;
        d.historicalPriorSample = c.historicalPriorSample;
        d.historicalPriorCoarseKey = c.historicalPriorCoarseKey;
        d.historicalPriorMidKey = c.historicalPriorMidKey;
        d.historicalPriorSummary = c.historicalPriorSummary;
        d.basePBeforeFusion = c.basePBeforeFusion;
        d.finalPAfterFusion = c.finalPAfterFusion;
        d.fusionWeightBase = c.fusionWeightBase;
        d.fusionWeightPrior = c.fusionWeightPrior;
        d.fusionWeightTeacher = c.fusionWeightTeacher;
        d.fusionWeightDynamic = c.fusionWeightDynamic;
        d.fusionSummary = c.fusionSummary;
    }

    private String firstNonEmptyText(String a, String b, String c, String d) {
        if (a != null && a.length() > 0) return a;
        if (b != null && b.length() > 0) return b;
        if (c != null && c.length() > 0) return c;
        return d == null ? "" : d;
    }

    private String safeTextLocal(String s) {
        return s == null ? "" : s;
    }

    private boolean lowFreqRescueAllowed(
            GateRuntimeContext runtime,
            Models.ComboCandidate c,
            DecisionContext ctx,
            boolean strong,
            boolean promising) {
        /* V2：旧“低频救援”停用，只承接DynamicGate已验证的合格边缘域。 */
        if (runtime == null || c == null || !c.isValidK3()) return false;
        if (runtime.frequencyState == null || !runtime.frequencyState.isLowFrequencyPressure())
            return false;
        if (c.qualifiedProbe != 1 || c.realProbeAllowed != 1) return false;
        if (ctx != null
                && (ctx.finalDecisionHardRisk
                        || ctx.finalDecisionFreeze
                        || ctx.finalDiagnosisRiskHigh
                        || ctx.dynamicCausalHardRisk
                        || ctx.dynamicCausalFreeze
                        || ctx.adaptiveReject)) return false;
        return true;
    }

    private boolean highFreqEliteAllowed(
            GateRuntimeContext runtime,
            Models.ComboCandidate c,
            DecisionContext ctx,
            boolean strong,
            boolean promising) {

        if (c == null || !c.isValidK3()) {
            return false;
        }

        double f =
                runtime == null
                        ? 0.0d
                        : Math.max(runtime.freq100, Math.max(runtime.freq500, runtime.freq1000));

        if (f < FREQ_IDEAL_HIGH) {
            return true;
        }

        if (isTerminalBadState(c.stateMode)) {
            return false;
        }

        if (c.driftRisk > AppConfig.DRIFT_RISK_MAX + 4.0d) {
            return false;
        }

        if (c.breakDepth > AppConfig.BREAK_DEPTH_MAX + 4.0d) {
            return false;
        }

        boolean support =
                strong
                        || promising
                        || (ctx != null
                                && (ctx.teacherMatrixStrong
                                        || ctx.learningStrong
                                        || ctx.evidenceStrong
                                        || ctx.v20Strong));

        if (ctx != null
                && (ctx.finalDecisionHardRisk
                        || ctx.finalDecisionFreeze
                        || ctx.moduleMisleading
                        || ctx.freezeMatrixRisk
                        || ctx.adaptiveReject)) {
            return false;
        }

        if (f >= FREQ_OVERHEAT) {
            return c.finalP >= OVERHEAT_MIN_P
                    && c.rawP >= RAW_FLOOR_NORMAL
                    && c.edge >= EDGE_MIN_NORMAL
                    && c.evidenceRisk <= 0.700d
                    && support
                    && !isRealRiskHigh(c);
        }

        if (f >= FREQ_STRONG_HIGH) {
            return c.finalP >= HIGH_FREQ_MIN_P
                    && c.rawP >= RAW_FLOOR_NORMAL
                    && c.edge >= EDGE_MIN_NORMAL
                    && c.evidenceRisk <= 0.680d
                    && support;
        }

        return true;
    }

    private double calculateExecutionGate(
            GateRuntimeContext runtime,
            Models.ComboCandidate c,
            boolean strong,
            boolean promising,
            boolean lowFreqRescue,
            boolean highFreqElite,
            DecisionContext ctx) {

        double defaultGate = AppConfig.DEFAULT_DYNAMIC_GATE;

        if (defaultGate <= 0.0d || Double.isNaN(defaultGate) || Double.isInfinite(defaultGate)) {
            defaultGate = 0.385d;
        }

        double oldGate = c == null ? defaultGate : c.dynamicGate;
        oldGate = cleanIncomingGate(oldGate, defaultGate);

        double g = oldGate;

        boolean teacherStrong = ctx != null && ctx.teacherMatrixStrong;
        boolean teacherReady = ctx != null && ctx.teacherMatrixReady;
        boolean learningStrong = ctx != null && ctx.learningStrong;
        boolean evidenceStrong = ctx != null && ctx.evidenceStrong;
        boolean v20Strong = ctx != null && ctx.v20Strong;
        boolean v20Ready = ctx != null && ctx.v20Ready;
        boolean v20RiskHigh = ctx != null && ctx.v20RiskHigh;

        if (strong || teacherStrong || learningStrong || v20Strong || lowFreqRescue) {
            g = Math.min(g, GATE_STRONG_CAP);
        } else if (promising || teacherReady || evidenceStrong || v20Ready) {
            g = Math.min(g, GATE_READY_CAP);
        } else {
            g = Math.min(g, GATE_NORMAL_CAP);
        }

        if (ctx != null && ctx.teacherMatrixStrong) {
            g -= 0.010d;
        } else if (ctx != null && ctx.teacherMatrixReady) {
            g -= 0.006d;
        } else if (ctx != null && ctx.teacherMatrixRisk) {
            g += 0.010d;
        }

        if (ctx != null && ctx.v20Strong) {
            g -= 0.008d;
        } else if (ctx != null && ctx.v20Ready) {
            g -= 0.004d;
        } else if (v20RiskHigh) {
            g += 0.008d;
        }

        if (ctx != null && ctx.v20HardRisk) {
            g += 0.014d;
        }
        if (ctx != null && ctx.dynamicCausalStrong) {
            g -= 0.010d;
        } else if (ctx != null && ctx.dynamicCausalReady) {
            g -= 0.006d;
        } else if (ctx != null && ctx.dynamicCausalRiskHigh) {
            g += 0.010d;
        }
        if (ctx != null && ctx.dynamicCausalHardRisk) {
            g += 0.018d;
        }

        if (ctx != null && ctx.learningStrong) {
            g -= 0.006d;
        } else if (ctx != null && ctx.learningWeak && !strong) {
            g += 0.004d;
        }

        if (c != null) {
            if (c.driftRisk > AppConfig.DRIFT_RISK_MAX) {
                g += 0.004d;
            }

            if (c.breakDepth > AppConfig.BREAK_DEPTH_MAX) {
                g += 0.004d;
            }

            if (c.hotReverseRisk > AppConfig.HOT_REVERSE_RISK_MAX) {
                g += 0.003d;
            }

            if (c.falseStrongRisk > AppConfig.FALSE_STRONG_RISK_MAX) {
                g += 0.003d;
            }
        }

        g += frequencySoftGateAdjust(runtime, strong, promising, lowFreqRescue, ctx);
        g += finalDiagnosisGateAdjust(ctx, c, strong, promising, lowFreqRescue, highFreqElite);

        double floor;

        if (strong || teacherStrong || learningStrong || v20Strong || lowFreqRescue) {
            floor = 0.345d;
        } else if (promising || teacherReady || evidenceStrong || v20Ready) {
            floor = 0.355d;
        } else {
            floor = 0.365d;
        }

        double minExec = readAppDouble("MIN_EXEC_GATE", 0.345d);
        if (minExec > 0.0d && floor < minExec) {
            floor = minExec;
        }

        if (g < floor) {
            g = floor;
        }

        double cap;

        if (hasHardRisk(c, ctx)) {
            cap = GATE_RISK_CAP;
        } else if (strong || teacherStrong || learningStrong || v20Strong || lowFreqRescue) {
            cap = GATE_NORMAL_CAP;
        } else if (promising || teacherReady || evidenceStrong || v20Ready) {
            cap = GATE_ELITE_CAP;
        } else {
            cap = GATE_RISK_CAP;
        }

        if (g > cap) {
            g = cap;
        }

        return AppConfig.clampProbability(g);
    }

    private String finalFieldChainHardBlockCode(Models.ComboCandidate c, DecisionContext ctx) {
        if (c == null || !c.isValidK3()) {
            return AppConfig.BLOCK_COMBO_WEAK;
        }

        String reject = readString(c, "finalDecisionRejectCode", "");
        if (reject != null
                && reject.length() > 0
                && !containsText(reject, "PASS")
                && !containsText(reject, "READY")
                && !containsText(reject, "OK")) {
            return "FINAL_FIELD_CHAIN_REJECT_CODE";
        }

        int teacherWeakFlag =
                maxReadInt(
                        c,
                        new String[] {
                            "teacherWeakNoBetFlag",
                            "teacherMatrixWeakNoBetFlag",
                            "teacherMatrixOrdinaryBetBlockFlag"
                        });
        String teacherPolicy =
                firstReadString(
                        c,
                        new String[] {
                            "teacherOrdinaryBetPolicy",
                            "teacherMatrixOrdinaryBetPolicy",
                            "finalDecisionTeacherPolicy"
                        });
        String teacherStatus =
                firstReadString(
                        c,
                        new String[] {
                            "teacherMatrixStatus", "teacherNumberStatus", "teacherStatus"
                        });
        String v20Status =
                firstReadString(
                        c,
                        new String[] {
                            "v20Status",
                            "dynamicStatus",
                            "dynamicCauseStatus",
                            "finalDecisionV20Policy"
                        });
        String windowState =
                firstReadString(
                        c,
                        new String[] {
                            "windowStateKey",
                            "finalDecisionWindowStateKey",
                            "dynamicWindowStateKey",
                            "probabilityWindowStateKey",
                            "v20WindowStateKey"
                        });
        String edgePolicy =
                firstReadString(
                        c, new String[] {"dynamicGateEdgePolicy", "finalDecisionEdgePolicy"});
        double edge = finalEdge(c);
        double coverageRisk =
                maxReadDouble(
                        c,
                        new String[] {
                            "k3CoverageRisk", "dynamicGateCoverageRisk", "finalDecisionCoverageRisk"
                        });
        double coverageQuality =
                maxReadDouble(
                        c,
                        new String[] {
                            "coverageQualityScore",
                            "dynamicGateCoverageQualityScore",
                            "finalDecisionCoverageQuality"
                        });
        double thirdQuality =
                maxReadDouble(
                        c,
                        new String[] {
                            "thirdNumberQualityScore",
                            "dynamicGateThirdNumberQuality",
                            "finalDecisionThirdNumberQuality"
                        });
        int falseHigh =
                maxReadInt(
                        c,
                        new String[] {
                            "falseHighPFlag",
                            "probabilityFalseHighPFlag",
                            "dynamicGateFalseHighPFlag",
                            "finalDecisionFalseHighPFlag"
                        });

        boolean teacherNeutral =
                isTeacherNoSampleNeutralText(teacherStatus)
                        || isTeacherNoSampleNeutralForFinal(c, ctx);
        boolean v20Neutral =
                isV20WatchOrEmptyNeutralText(v20Status) || isV20WatchOrEmptyNeutralForFinal(c, ctx);
        boolean teacherHardRisk =
                isTeacherHardRiskText(teacherStatus) || isTeacherHardRiskForFinal(c, ctx);
        boolean v20HardRisk = isV20HardRiskText(v20Status) || isV20HardRiskForFinal(c, ctx);
        boolean teacherWeak =
                !teacherNeutral
                        && !teacherHardRisk
                        && (teacherWeakFlag > 0
                                || containsText(teacherPolicy, "WEAK_NO_BET")
                                || fieldChainTeacherWeakOrEmpty(teacherStatus));
        boolean v20Weak = !v20Neutral && !v20HardRisk && fieldChainV20WeakOrEmpty(v20Status);
        boolean windowEmpty =
                windowState == null || windowState.length() == 0 || "空".equals(windowState);
        boolean coverEmpty =
                coverageQuality <= 0.0001d && coverageRisk <= 0.0001d && thirdQuality <= 0.0001d;
        boolean strongOverride = fieldChainTrueStrongOverride(c, ctx);
        boolean preferredReviewOverride = finalPreferredReviewBetDomain(c, ctx);

        if (teacherHardRisk) {
            writeString(c, "finalDecisionRejectCode", "FINAL_FIELD_CHAIN_TEACHER_RISK_FREEZE");
            return "FINAL_FIELD_CHAIN_TEACHER_RISK_FREEZE";
        }
        if (v20HardRisk) {
            writeString(c, "finalDecisionRejectCode", "FINAL_FIELD_CHAIN_V20_RISK_FREEZE");
            return "FINAL_FIELD_CHAIN_V20_RISK_FREEZE";
        }
        if ((teacherWeakFlag > 0 || containsText(teacherPolicy, "WEAK_NO_BET"))
                && !teacherNeutral) {
            if (!strongOverride) {
                writeString(c, "finalDecisionRejectCode", "FINAL_FIELD_CHAIN_TEACHER_WEAK_NO_BET");
                return "FINAL_FIELD_CHAIN_TEACHER_WEAK_NO_BET";
            }
        }
        if (falseHigh > 0) {
            if (c.finalP < 0.425d || !strongOverride) {
                writeString(c, "finalDecisionRejectCode", "FINAL_FIELD_CHAIN_FALSE_HIGH_P");
                return "FINAL_FIELD_CHAIN_FALSE_HIGH_P";
            }
        }
        if (coverageRisk >= FINAL_K3_COVERAGE_HARD_RISK && c.finalP < 0.430d) {
            writeString(c, "finalDecisionRejectCode", "FINAL_FIELD_CHAIN_K3_COVERAGE_RISK");
            return "FINAL_FIELD_CHAIN_K3_COVERAGE_RISK";
        }
        if (coverageRisk >= FINAL_K3_COVERAGE_RISK_LINE
                && !fieldChainPreferredDomain(c)
                && !strongOverride) {
            writeString(c, "finalDecisionRejectCode", "FINAL_FIELD_CHAIN_K3_COVERAGE_RISK");
            return "FINAL_FIELD_CHAIN_K3_COVERAGE_RISK";
        }
        if (thirdQuality > 0.0d && thirdQuality < FINAL_THIRD_NUMBER_MIN && c.finalP < 0.420d) {
            writeString(c, "finalDecisionRejectCode", "FINAL_FIELD_CHAIN_THIRD_NUMBER_WEAK");
            return "FINAL_FIELD_CHAIN_THIRD_NUMBER_WEAK";
        }
        if (containsText(edgePolicy, "虚强") || containsText(edgePolicy, "SHADOW复核")) {
            if (!strongOverride) {
                writeString(c, "finalDecisionRejectCode", "FINAL_FIELD_CHAIN_EDGE_POLICY_SHADOW");
                return "FINAL_FIELD_CHAIN_EDGE_POLICY_SHADOW";
            }
        }
        if (teacherWeak && v20Weak && windowEmpty && c.finalP < 0.430d && !strongOverride) {
            writeString(
                    c,
                    "finalDecisionRejectCode",
                    "FINAL_FIELD_CHAIN_TEACHER_WEAK_V20_WINDOW_EMPTY");
            return "FINAL_FIELD_CHAIN_TEACHER_WEAK_V20_WINDOW_EMPTY";
        }
        if (teacherWeak
                && v20Weak
                && c.finalP < FIELD_CHAIN_HARD_P_NO_STRONG
                && !strongOverride
                && !preferredReviewOverride) {
            writeString(c, "finalDecisionRejectCode", "FINAL_FIELD_CHAIN_TEACHER_WEAK_V20_EMPTY");
            return "FINAL_FIELD_CHAIN_TEACHER_WEAK_V20_EMPTY";
        }
        if (teacherWeak
                && v20Weak
                && edge > FIELD_CHAIN_HARD_EDGE_FALSE_STRONG
                && !strongOverride) {
            writeString(c, "finalDecisionRejectCode", "FINAL_FIELD_CHAIN_EDGE_FALSE_STRONG");
            return "FINAL_FIELD_CHAIN_EDGE_FALSE_STRONG";
        }
        if (teacherWeak
                && v20Weak
                && c.finalP >= 0.360d
                && c.finalP < FIELD_CHAIN_HARD_P_WEAK
                && !strongOverride
                && !preferredReviewOverride) {
            writeString(c, "finalDecisionRejectCode", "FINAL_FIELD_CHAIN_P36_38_WEAK_DOMAIN");
            return "FINAL_FIELD_CHAIN_P36_38_WEAK_DOMAIN";
        }
        if (coverEmpty
                && teacherWeak
                && v20Weak
                && c.finalP < FIELD_CHAIN_HARD_P_COVER_EMPTY
                && !strongOverride) {
            writeString(c, "finalDecisionRejectCode", "FINAL_FIELD_CHAIN_COVER_EMPTY_WEAK_DOMAIN");
            return "FINAL_FIELD_CHAIN_COVER_EMPTY_WEAK_DOMAIN";
        }
        if (edge > FIELD_CHAIN_HARD_EDGE_FALSE_STRONG
                && maxReadInt(
                                c,
                                new String[] {
                                    "probabilityStrongCauseFlag",
                                    "decisionEvidenceStrong",
                                    "decisionEvidenceElite"
                                })
                        <= 0
                && !strongOverride
                && c.finalP < 0.430d) {
            writeString(c, "finalDecisionRejectCode", "FINAL_FIELD_CHAIN_EDGE_FALSE_STRONG");
            return "FINAL_FIELD_CHAIN_EDGE_FALSE_STRONG";
        }
        if (c.finalP >= FINAL_P40_FALSE_HIGH_LINE && teacherWeak && v20Weak && !strongOverride) {
            writeString(c, "finalDecisionRejectCode", "FINAL_FIELD_CHAIN_P40_NO_STRONG_CAUSE");
            return "FINAL_FIELD_CHAIN_P40_NO_STRONG_CAUSE";
        }
        return "";
    }

    private boolean fieldChainTrueStrongOverride(Models.ComboCandidate c, DecisionContext ctx) {
        if (c == null) {
            return false;
        }
        String teacher =
                firstReadString(
                        c,
                        new String[] {
                            "teacherMatrixStatus", "teacherNumberStatus", "teacherStatus"
                        });
        String v20 =
                firstReadString(
                        c,
                        new String[] {
                            "v20Status",
                            "dynamicStatus",
                            "dynamicCauseStatus",
                            "finalDecisionV20Policy"
                        });
        if (fieldChainTeacherWeakOrEmpty(teacher) || fieldChainV20WeakOrEmpty(v20)) {
            return false;
        }
        double risk =
                maxReadDouble(
                        c,
                        new String[] {
                            "k3CoverageRisk", "dynamicGateCoverageRisk", "finalDecisionCoverageRisk"
                        });
        if (risk >= FINAL_K3_COVERAGE_HARD_RISK) {
            return false;
        }
        if (ctx != null
                && (ctx.finalDecisionHardRisk
                        || ctx.finalDecisionFreeze
                        || ctx.v20HardRisk
                        || ctx.teacherMatrixRisk)) {
            return false;
        }
        return true;
    }

    private boolean fieldChainPreferredDomain(Models.ComboCandidate c) {
        if (c == null) {
            return false;
        }
        if (readInt(c, "dynamicGatePreferredEdgeBand", 0) == 1
                || readInt(c, "probabilityPreferredEdgeBand", 0) == 1) {
            return maxReadDouble(
                                    c,
                                    new String[] {
                                        "k3CoverageRisk",
                                        "dynamicGateCoverageRisk",
                                        "finalDecisionCoverageRisk"
                                    })
                            < 0.50d
                    && maxReadDouble(
                                    c,
                                    new String[] {
                                        "thirdNumberQualityScore",
                                        "dynamicGateThirdNumberQuality",
                                        "finalDecisionThirdNumberQuality"
                                    })
                            >= FINAL_THIRD_NUMBER_MIN;
        }
        double edge = finalEdge(c);
        return c.finalP >= FINAL_PREFERRED_P_MIN
                && c.finalP <= FINAL_PREFERRED_P_MAX
                && edge >= FINAL_PREFERRED_EDGE_MIN
                && edge <= FINAL_PREFERRED_EDGE_MAX
                && maxReadDouble(
                                c,
                                new String[] {
                                    "k3CoverageRisk",
                                    "dynamicGateCoverageRisk",
                                    "finalDecisionCoverageRisk"
                                })
                        < 0.50d
                && maxReadDouble(
                                c,
                                new String[] {
                                    "thirdNumberQualityScore",
                                    "dynamicGateThirdNumberQuality",
                                    "finalDecisionThirdNumberQuality"
                                })
                        >= FINAL_THIRD_NUMBER_MIN;
    }

    private boolean fieldChainTeacherWeakOrEmpty(String text) {
        if (isTeacherNoSampleNeutralText(text)) {
            return false;
        }
        if (isTeacherHardRiskText(text)) {
            return false;
        }
        if (text == null || text.length() == 0 || "空".equals(text)) {
            return false;
        }
        String s = text.toUpperCase(java.util.Locale.ROOT);
        if (s.indexOf("WEAK") >= 0 || s.indexOf("弱") >= 0) {
            return true;
        }
        return false;
    }

    private boolean fieldChainV20WeakOrEmpty(String text) {
        if (isV20WatchOrEmptyNeutralText(text)) {
            return false;
        }
        if (isV20HardRiskText(text)) {
            return false;
        }
        if (text == null || text.length() == 0 || "空".equals(text)) {
            return false;
        }
        String s = text.toUpperCase(java.util.Locale.ROOT);
        if (s.indexOf("WEAK") >= 0 || s.indexOf("弱") >= 0) {
            return true;
        }
        return false;
    }

    private int maxReadInt(Object obj, String[] names) {
        int best = 0;
        if (names == null) {
            return best;
        }
        for (int i = 0; i < names.length; i++) {
            int v = readInt(obj, names[i], 0);
            if (v > best) {
                best = v;
            }
        }
        return best;
    }

    private double maxReadDouble(Object obj, String[] names) {
        double best = 0.0d;
        if (names == null) {
            return best;
        }
        for (int i = 0; i < names.length; i++) {
            double v = readDouble(obj, names[i], 0.0d);
            if (v > best) {
                best = v;
            }
        }
        return best;
    }

    private String firstReadString(Object obj, String[] names) {
        if (names == null) {
            return "";
        }
        for (int i = 0; i < names.length; i++) {
            String v = readString(obj, names[i], "");
            if (v != null && v.length() > 0) {
                return v;
            }
        }
        return "";
    }

    private String finalBetStrictBlockCode(Models.ComboCandidate c, DecisionContext ctx) {
        if (c == null || !c.isValidK3()) {
            return AppConfig.BLOCK_COMBO_WEAK;
        }
        String dynamicCausalBlock = dynamicCausalFinalBlockCode(c, ctx);
        if (dynamicCausalBlock.length() > 0) {
            writeString(c, "finalDecisionRejectCode", dynamicCausalBlock);
            return dynamicCausalBlock;
        }
        String fieldChainHardBlock = finalFieldChainHardBlockCode(c, ctx);
        if (fieldChainHardBlock.length() > 0) {
            if (finalPreferredReviewBetDomain(c, ctx)
                    && finalPreferredReviewCanOverrideBlock(fieldChainHardBlock)) {
                writeString(c, "finalDecisionRejectCode", "");
                markFinalPreferredReviewBetDomain(c, "STRICT_FIELD_CHAIN_OVERRIDE");
            } else {
                return fieldChainHardBlock;
            }
        }

        if ((readInt(c, "teacherWeakNoBetFlag", 0) == 1
                        || readInt(c, "teacherMatrixWeakNoBetFlag", 0) == 1
                        || readInt(c, "teacherMatrixOrdinaryBetBlockFlag", 0) == 1
                        || containsText(
                                readString(c, "teacherOrdinaryBetPolicy", ""), "WEAK_NO_BET"))
                && !isTeacherNoSampleNeutralForFinal(c, ctx)
                && !isTeacherHardRiskForFinal(c, ctx)) {
            if (!finalHasStrongOverride(c, ctx)) {
                return "FINAL_GATE_TEACHER_WEAK_NO_BET";
            }
        }
        if (readInt(c, "falseHighPFlag", 0) == 1
                || readInt(c, "probabilityFalseHighPFlag", 0) == 1
                || readInt(c, "dynamicGateFalseHighPFlag", 0) == 1) {
            if (c.finalP < 0.425d || !finalHasStrongOverride(c, ctx)) {
                return "FINAL_GATE_FALSE_HIGH_P";
            }
        }
        double coverageRisk =
                readDouble(c, "k3CoverageRisk", readDouble(c, "dynamicGateCoverageRisk", 0.0d));
        double thirdQuality =
                readDouble(
                        c,
                        "thirdNumberQualityScore",
                        readDouble(c, "dynamicGateThirdNumberQuality", 1.0d));
        double coverageQuality =
                readDouble(
                        c,
                        "coverageQualityScore",
                        readDouble(c, "dynamicGateCoverageQualityScore", 0.0d));
        if (coverageRisk >= FINAL_K3_COVERAGE_HARD_RISK && c.finalP < 0.430d) {
            return "FINAL_GATE_K3_COVERAGE_RISK";
        }
        if (coverageRisk >= FINAL_K3_COVERAGE_RISK_LINE
                && !finalPreferredDomain(c)
                && !finalHasStrongOverride(c, ctx)) {
            return "FINAL_GATE_K3_COVERAGE_RISK";
        }
        if (thirdQuality > 0.0d && thirdQuality < FINAL_THIRD_NUMBER_MIN && c.finalP < 0.420d) {
            return "FINAL_GATE_THIRD_NUMBER_WEAK";
        }
        double rawThirdQualityForFinal =
                maxReadDouble(
                        c,
                        new String[] {
                            "thirdNumberQualityScore",
                            "dynamicGateThirdNumberQuality",
                            "finalDecisionThirdNumberQuality"
                        });
        if (rawThirdQualityForFinal > 0.0d
                && rawThirdQualityForFinal < FINAL_THIRD_NUMBER_MIN
                && c.finalP < 0.420d) {
            return "FINAL_GATE_RAW_THIRD_NUMBER_WEAK";
        }
        if (coverageQuality > 0.0d
                && coverageQuality < FINAL_COVERAGE_QUALITY_MIN
                && c.finalP >= 0.390d
                && !finalHasStrongOverride(c, ctx)) {
            return "FINAL_GATE_COVERAGE_QUALITY_LOW";
        }
        if (teacherWeakOrEmptyForFinal(c, ctx) && v20UnknownOrWeakForFinal(c, ctx)) {
            if (!finalPreferredDomain(c)
                    && c.finalP < 0.415d
                    && !finalPreferredReviewBetDomain(c, ctx)) {
                return "FINAL_GATE_TEACHER_WEAK_V20_UNKNOWN";
            }
            if (finalEdge(c) > FINAL_EDGE_FALSE_STRONG_LINE && !finalHasStrongOverride(c, ctx)) {
                return "FINAL_GATE_EDGE_FALSE_STRONG";
            }
            if (c.finalP >= FINAL_P40_FALSE_HIGH_LINE
                    && !finalHasStrongOverride(c, ctx)
                    && !finalPreferredReviewBetDomain(c, ctx)) {
                return "FINAL_GATE_P40_NO_STRONG_CAUSE";
            }
        }
        if (finalEdge(c) > FINAL_EDGE_FALSE_STRONG_LINE
                && readInt(c, "probabilityStrongCauseFlag", 0) == 0
                && !finalHasStrongOverride(c, ctx)
                && c.finalP < 0.430d) {
            return "FINAL_GATE_EDGE_FALSE_STRONG";
        }
        String edgePolicy = readString(c, "dynamicGateEdgePolicy", "");
        if ((containsText(edgePolicy, "虚强") || containsText(edgePolicy, "SHADOW复核"))
                && !finalHasStrongOverride(c, ctx)) {
            return "FINAL_GATE_EDGE_POLICY_SHADOW";
        }
        return "";
    }

    private String toVerifyBlockCode(String code) {
        if (code == null || code.length() == 0) {
            return AppConfig.BLOCK_LOW_PROBABILITY;
        }
        if ("FINAL_GATE_FALSE_HIGH_P".equals(code)) {
            return "FINAL_VERIFY_FALSE_HIGH_P";
        }
        if ("FINAL_GATE_EDGE_FALSE_STRONG".equals(code)) {
            return "FINAL_VERIFY_EDGE_FALSE_STRONG";
        }
        return code;
    }

    private boolean finalHasStrongOverride(Models.ComboCandidate c, DecisionContext ctx) {
        if (c == null) {
            return false;
        }
        if (isTeacherHardRiskForFinal(c, ctx)
                || isV20HardRiskForFinal(c, ctx)
                || isAdaptiveHardRiskForFinal(c, ctx)) {
            writeString(c, "finalDecisionStrongOverrideTrace", "HARD_RISK_OVERRIDE_DENIED");
            return false;
        }
        if (teacherWeakOrEmptyForFinal(c, null)
                && v20UnknownOrWeakForFinal(c, null)
                && !isTeacherNoSampleNeutralForFinal(c, ctx)
                && !isV20WatchOrEmptyNeutralForFinal(c, ctx)) {
            writeString(
                    c, "finalDecisionStrongOverrideTrace", "FIELD_CHAIN_STRONG_OVERRIDE_DENIED");
            return false;
        }

        boolean teacherStrong = ctx != null && ctx.teacherMatrixStrong;
        boolean v20Strong = ctx != null && (ctx.v20Strong || ctx.v20Elite);
        boolean evidenceStrong =
                ctx != null
                        && (ctx.evidenceStrong
                                || ctx.decisionEvidenceStrong
                                || ctx.decisionEvidenceElite);
        boolean learningStrong = ctx != null && ctx.learningStrong;
        boolean finalStrong =
                ctx != null
                        && ctx.finalDecisionStrong
                        && !ctx.finalDecisionHardRisk
                        && !ctx.finalDecisionFreeze;
        boolean dynamicStrong =
                ctx != null
                        && (ctx.dynamicCausalStrong || ctx.dynamicCausalReady)
                        && !ctx.dynamicCausalHardRisk
                        && !ctx.dynamicCausalFreeze;
        if (teacherStrong
                || v20Strong
                || evidenceStrong
                || learningStrong
                || finalStrong
                || dynamicStrong) {
            return readDouble(c, "probabilityCounterEvidence", 0.0d) < FINAL_DIAG_COUNTER_RISK
                    && readDouble(c, "k3CoverageRisk", 0.0d) < FINAL_K3_COVERAGE_HARD_RISK;
        }
        if (readInt(c, "probabilityStrongCauseFlag", 0) == 1
                && readDouble(c, "probabilityCounterEvidence", 0.0d) < 0.56d
                && readDouble(c, "k3CoverageRisk", 0.0d) < 0.56d) {
            return true;
        }
        return false;
    }

    private boolean finalPreferredReviewCanOverrideBlock(String code) {
        if (code == null || code.length() == 0) {
            return false;
        }
        return "FINAL_FIELD_CHAIN_TEACHER_WEAK_V20_EMPTY".equals(code)
                || "FINAL_FIELD_CHAIN_P36_38_WEAK_DOMAIN".equals(code)
                || "FINAL_GATE_TEACHER_WEAK_V20_UNKNOWN".equals(code)
                || "FINAL_GATE_P40_NO_STRONG_CAUSE".equals(code);
    }

    private boolean finalPreferredReviewBetDomain(Models.ComboCandidate c, DecisionContext ctx) {
        if (c == null || !c.isValidK3()) {
            return false;
        }
        if (c.finalP < FINAL_REVIEW_BET_MIN_P || c.rawP < FINAL_REVIEW_BET_MIN_RAW) {
            return false;
        }
        double edge = finalEdge(c);
        if (edge < FINAL_REVIEW_BET_MIN_EDGE || edge > FINAL_REVIEW_BET_MAX_EDGE) {
            return false;
        }
        String reject = readString(c, "finalDecisionRejectCode", "");
        if (reject != null
                && reject.length() > 0
                && !finalPreferredReviewCanOverrideBlock(reject)) {
            return false;
        }
        String teacher =
                readString(
                        c,
                        "teacherMatrixStatus",
                        readString(c, "teacherNumberStatus", readString(c, "teacherStatus", "")));
        String teacherPolicy =
                readString(
                        c,
                        "teacherOrdinaryBetPolicy",
                        readString(c, "teacherMatrixOrdinaryBetPolicy", ""));
        String v20 =
                readString(
                        c,
                        "v20Status",
                        readString(c, "dynamicStatus", readString(c, "dynamicCauseStatus", "")));
        String window =
                readString(
                        c,
                        "windowStateKey",
                        readString(
                                c,
                                "v20WindowStateKey",
                                readString(c, "dynamicWindowStateKey", "")));
        String gateQuality = readString(c, "dynamicGateQualityStatus", "");
        String edgePolicy = readString(c, "dynamicGateEdgePolicy", "");
        if (!teacherWatchReadyOrStrongForReview(teacher)) {
            return false;
        }
        if (containsText(teacherPolicy, "WEAK_NO_BET")
                || containsText(teacherPolicy, "RISK")
                || containsText(teacherPolicy, "FREEZE")) {
            return false;
        }
        if (!v20UsableForPreferredReview(v20)) {
            return false;
        }
        if (window == null || window.length() == 0 || "空".equals(window)) {
            return false;
        }
        if (containsText(gateQuality, "MISSING")
                || containsText(gateQuality, "HARD_RISK")
                || containsText(gateQuality, "EDGE_FALSE")
                || containsText(gateQuality, "FREEZE")) {
            return false;
        }
        if (containsText(edgePolicy, "虚强") || containsText(edgePolicy, "SHADOW复核")) {
            return false;
        }
        int falseHigh =
                maxReadInt(
                        c,
                        new String[] {
                            "falseHighPFlag",
                            "probabilityFalseHighPFlag",
                            "dynamicGateFalseHighPFlag",
                            "finalDecisionFalseHighPFlag"
                        });
        if (falseHigh > 0) {
            return false;
        }
        double cover =
                maxReadDouble(
                        c,
                        new String[] {
                            "coverageQualityScore",
                            "dynamicGateCoverageQualityScore",
                            "finalDecisionCoverageQuality"
                        });
        double risk =
                maxReadDouble(
                        c,
                        new String[] {
                            "k3CoverageRisk", "dynamicGateCoverageRisk", "finalDecisionCoverageRisk"
                        });
        double third =
                maxReadDouble(
                        c,
                        new String[] {
                            "thirdNumberQualityScore",
                            "dynamicGateThirdNumberQuality",
                            "finalDecisionThirdNumberQuality"
                        });
        if (cover < FINAL_REVIEW_BET_MIN_COVER
                || risk > FINAL_REVIEW_BET_MAX_RISK
                || third < FINAL_REVIEW_BET_MIN_THIRD) {
            return false;
        }
        String v = v20 == null ? "" : v20.toUpperCase(java.util.Locale.ROOT);
        if (v.indexOf("WEAK") >= 0) {
            if (cover < FINAL_REVIEW_BET_WEAK_V20_MIN_COVER
                    || risk > FINAL_REVIEW_BET_WEAK_V20_MAX_RISK
                    || third < FINAL_REVIEW_BET_WEAK_V20_MIN_THIRD) {
                return false;
            }
        }
        if (ctx != null) {
            if (ctx.finalDecisionHardRisk
                    || ctx.finalDecisionFreeze
                    || ctx.finalDiagnosisRiskHigh
                    || ctx.v20HardRisk
                    || ctx.teacherMatrixRisk
                    || ctx.moduleMisleading
                    || ctx.freezeMatrixRisk
                    || ctx.adaptiveReject
                    || !dynamicPreferredReviewOk(c, ctx)) {
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    private boolean teacherWatchReadyOrStrongForReview(String text) {
        if (text == null || text.length() == 0 || "空".equals(text)) {
            return false;
        }
        String s = text.toUpperCase(java.util.Locale.ROOT);
        if (s.indexOf("RISK") >= 0
                || s.indexOf("FREEZE") >= 0
                || s.indexOf("NO_SAMPLE") >= 0
                || s.indexOf("NONE") >= 0) {
            return false;
        }
        return s.indexOf("WATCH") >= 0
                || s.indexOf("READY") >= 0
                || s.indexOf("STRONG") >= 0
                || s.indexOf("PASS") >= 0;
    }

    private boolean v20UsableForPreferredReview(String text) {
        if (text == null || text.length() == 0 || "空".equals(text)) {
            return false;
        }
        String s = text.toUpperCase(java.util.Locale.ROOT);
        if (s.indexOf("RISK") >= 0
                || s.indexOf("FREEZE") >= 0
                || s.indexOf("UNKNOWN") >= 0
                || s.indexOf("EMPTY") >= 0) {
            return false;
        }
        return s.indexOf("WEAK") >= 0
                || s.indexOf("WATCH") >= 0
                || s.indexOf("READY") >= 0
                || s.indexOf("STRONG") >= 0
                || s.indexOf("PASS") >= 0;
    }

    private void markFinalPreferredReviewBetDomain(Models.ComboCandidate c, String stage) {
        if (c == null) {
            return;
        }
        writeString(c, "finalDecisionVersion", V20_DECISION_GATE_PREFERRED_REVIEW_VERSION);
        writeString(c, "finalDecisionBetDomain", "WATCH_HIGH_COVER_LOW_RISK_REVIEW_BET");
        writeString(c, "finalDecisionRejectCode", "");
        writeString(c, "finalDecisionShadowReason", "");
        writeString(
                c,
                "finalDecisionTeacherPolicy",
                readString(
                        c,
                        "teacherOrdinaryBetPolicy",
                        readString(c, "teacherMatrixOrdinaryBetPolicy", "")));
        writeString(
                c,
                "finalDecisionV20Policy",
                readString(c, "v20Status", readString(c, "dynamicStatus", "")));
        writeString(c, "finalDecisionEdgePolicy", readString(c, "dynamicGateEdgePolicy", ""));
        writeString(
                c,
                "finalDecisionStrongOverrideTrace",
                "PREFERRED_REVIEW_BET|"
                        + (stage == null ? "" : stage)
                        + "|Cover="
                        + UiFormatter.probability(
                                maxReadDouble(
                                        c,
                                        new String[] {
                                            "coverageQualityScore",
                                            "dynamicGateCoverageQualityScore",
                                            "finalDecisionCoverageQuality"
                                        }))
                        + "|Risk="
                        + UiFormatter.probability(
                                maxReadDouble(
                                        c,
                                        new String[] {
                                            "k3CoverageRisk",
                                            "dynamicGateCoverageRisk",
                                            "finalDecisionCoverageRisk"
                                        }))
                        + "|Third="
                        + UiFormatter.probability(
                                maxReadDouble(
                                        c,
                                        new String[] {
                                            "thirdNumberQualityScore",
                                            "dynamicGateThirdNumberQuality",
                                            "finalDecisionThirdNumberQuality"
                                        })));
    }

    private boolean finalPreferredDomain(Models.ComboCandidate c) {
        if (c == null) {
            return false;
        }
        if (finalPreferredReviewBetDomain(c, null)) {
            return true;
        }
        if (readInt(c, "dynamicGatePreferredEdgeBand", 0) == 1
                || readInt(c, "probabilityPreferredEdgeBand", 0) == 1) {
            return readDouble(c, "k3CoverageRisk", readDouble(c, "dynamicGateCoverageRisk", 1.0d))
                            < 0.50d
                    && readDouble(
                                    c,
                                    "thirdNumberQualityScore",
                                    readDouble(c, "dynamicGateThirdNumberQuality", 0.0d))
                            >= FINAL_THIRD_NUMBER_MIN;
        }
        double edge = finalEdge(c);
        return c.finalP >= FINAL_PREFERRED_P_MIN
                && c.finalP <= FINAL_PREFERRED_P_MAX
                && edge >= FINAL_PREFERRED_EDGE_MIN
                && edge <= FINAL_PREFERRED_EDGE_MAX
                && readDouble(c, "k3CoverageRisk", readDouble(c, "dynamicGateCoverageRisk", 1.0d))
                        < 0.50d
                && readDouble(
                                c,
                                "thirdNumberQualityScore",
                                readDouble(c, "dynamicGateThirdNumberQuality", 0.0d))
                        >= FINAL_THIRD_NUMBER_MIN;
    }

    private double finalEdge(Models.ComboCandidate c) {
        if (c == null) {
            return 0.0d;
        }
        double edge = c.edge;
        if (Double.isNaN(edge) || Double.isInfinite(edge) || Math.abs(edge) > 1.0d) {
            edge = c.finalP - c.dynamicGate;
        }
        return edge;
    }

    private boolean teacherWeakOrEmptyForFinal(Models.ComboCandidate c, DecisionContext ctx) {
        if (ctx != null
                && (ctx.teacherMatrixStrong
                        || ctx.teacherMatrixReady
                        || ctx.teacherNoSampleNeutral)) {
            return false;
        }
        if (isTeacherNoSampleNeutralForFinal(c, ctx) || isTeacherHardRiskForFinal(c, ctx)) {
            return false;
        }
        String s =
                readString(
                        c,
                        "teacherMatrixStatus",
                        readString(c, "teacherNumberStatus", readString(c, "teacherStatus", "")));
        if (s == null || s.length() == 0) {
            return false;
        }
        String u = s.toUpperCase(java.util.Locale.ROOT);
        return u.indexOf("WEAK") >= 0 || u.indexOf("弱") >= 0;
    }

    private boolean v20UnknownOrWeakForFinal(Models.ComboCandidate c, DecisionContext ctx) {
        if (ctx != null && (ctx.v20Strong || ctx.v20Ready || ctx.v20Elite || ctx.v20WatchNeutral)) {
            return false;
        }
        if (isV20WatchOrEmptyNeutralForFinal(c, ctx) || isV20HardRiskForFinal(c, ctx)) {
            return false;
        }
        String s =
                readString(
                        c,
                        "v20Status",
                        readString(c, "dynamicStatus", readString(c, "dynamicGrade", "")));
        if (s == null || s.length() == 0) {
            return false;
        }
        String u = s.toUpperCase(java.util.Locale.ROOT);
        return u.indexOf("WEAK") >= 0 || u.indexOf("弱") >= 0;
    }

    private String finalBetStrictReason(Models.ComboCandidate c, DecisionContext ctx, String code) {
        StringBuilder sb = new StringBuilder();
        sb.append("原因=").append(code == null ? "" : code);
        sb.append("；FinalP=").append(c == null ? "" : UiFormatter.probability(c.finalP));
        sb.append("；Gate=").append(c == null ? "" : UiFormatter.probability(c.dynamicGate));
        sb.append("；Edge=").append(c == null ? "" : UiFormatter.signedProbability(finalEdge(c)));
        sb.append("；TeacherPolicy=").append(readString(c, "teacherOrdinaryBetPolicy", ""));
        sb.append("；TeacherStatus=")
                .append(readString(c, "teacherMatrixStatus", readString(c, "teacherStatus", "")));
        sb.append("；V20=").append(readString(c, "v20Status", readString(c, "dynamicStatus", "")));
        sb.append("；覆盖风险=")
                .append(
                        UiFormatter.probability(
                                readDouble(
                                        c,
                                        "k3CoverageRisk",
                                        readDouble(c, "dynamicGateCoverageRisk", 0.0d))));
        sb.append("；第三号质量=")
                .append(
                        UiFormatter.probability(
                                readDouble(
                                        c,
                                        "thirdNumberQualityScore",
                                        readDouble(c, "dynamicGateThirdNumberQuality", 0.0d))));
        sb.append("；Edge策略=").append(readString(c, "dynamicGateEdgePolicy", ""));
        sb.append("；字段链版本=").append(V20_FIELD_CHAIN_DECISION_GATE_VERSION);
        sb.append("；优先复核版本=").append(V20_DECISION_GATE_PREFERRED_REVIEW_VERSION);
        sb.append("；PreferredReview=").append(finalPreferredReviewBetDomain(c, ctx) ? "YES" : "NO");
        sb.append("；最终策略=不救援普通BET，只有WATCH+高Cover+低Risk+Third有效+非虚强窄口可复核BET，否则转SHADOW反事实学习");
        return sb.toString();
    }

    private void normalizeFinalMainChainNeutralFields(Models.ComboCandidate c) {
        if (c == null) {
            return;
        }
        String teacher =
                firstReadString(
                        c,
                        new String[] {
                            "teacherMatrixStatus", "teacherNumberStatus", "teacherStatus"
                        });
        if (isTeacherNoSampleNeutralText(teacher)) {
            writeString(c, "teacherMatrixStatus", STATUS_NO_SAMPLE_NEUTRAL);
            writeString(c, "teacherNumberStatus", STATUS_NO_SAMPLE_NEUTRAL);
            writeString(c, "teacherStatus", STATUS_NO_SAMPLE_NEUTRAL);
            writeInt(c, "teacherNoSampleNeutralFlag", 1);
            writeInt(c, "teacherMatrixNoSampleNeutralFlag", 1);
            writeInt(c, "teacherWeakNoBetFlag", 0);
            writeInt(c, "teacherMatrixWeakNoBetFlag", 0);
            writeInt(c, "teacherMatrixOrdinaryBetBlockFlag", 0);
            writeString(c, "teacherOrdinaryBetPolicy", STATUS_NO_SAMPLE_NEUTRAL);
            writeString(c, "teacherMatrixOrdinaryBetPolicy", STATUS_NO_SAMPLE_NEUTRAL);
        }
        String v20 =
                firstReadString(
                        c, new String[] {"v20Status", "dynamicStatus", "dynamicCauseStatus"});
        if (isV20WatchOrEmptyNeutralText(v20)) {
            writeInt(c, "v20WatchNeutralFlag", 1);
        }
        String adaptive =
                firstReadString(
                        c,
                        new String[] {"adaptiveJudgeStatus", "adaptiveStatus", "adaptiveAction"});
        if (isAdaptiveWatchNeutralText(adaptive)) {
            writeInt(c, "adaptiveWatchNeutralFlag", 1);
        }
    }

    private boolean isTeacherNoSampleNeutralForFinal(Models.ComboCandidate c, DecisionContext ctx) {
        if (ctx != null && ctx.teacherNoSampleNeutral) {
            return true;
        }
        if (c == null) {
            return false;
        }
        String status =
                firstReadString(
                        c,
                        new String[] {
                            "teacherMatrixStatus", "teacherNumberStatus", "teacherStatus"
                        });
        int sample =
                maxReadInt(
                        c,
                        new String[] {
                            "teacherMatrixSample",
                            "teacherNumberSample",
                            "teacherSample",
                            "teacherMatrixComboSample"
                        });
        double evidence =
                maxReadDouble(
                        c,
                        new String[] {
                            "teacherEvidenceContribution",
                            "teacherMatrixEvidenceContribution",
                            "teacherNumberScore"
                        });
        double counter =
                maxReadDouble(
                        c,
                        new String[] {
                            "teacherCounterContribution",
                            "teacherMatrixCounterContribution",
                            "teacherMatrixRisk",
                            "teacherNumberRisk"
                        });
        return isTeacherNoSampleNeutralText(status)
                && sample <= 0
                && evidence <= 0.0001d
                && counter <= 0.0001d;
    }

    private boolean isTeacherNoSampleNeutralText(String text) {
        if (text == null || text.length() == 0 || "空".equals(text)) {
            return true;
        }
        String s = text.toUpperCase(java.util.Locale.ROOT);
        return s.indexOf("NO_SAMPLE") >= 0
                || s.indexOf("NO SAMPLE") >= 0
                || s.indexOf("NONE") >= 0
                || s.indexOf("EMPTY") >= 0
                || s.indexOf("UNKNOWN") >= 0
                || s.indexOf("NEUTRAL") >= 0
                || s.indexOf("中性") >= 0;
    }

    private boolean isTeacherHardRiskForFinal(Models.ComboCandidate c, DecisionContext ctx) {
        if (ctx != null
                && (ctx.teacherMatrixRisk || ctx.teacherMatrixFreeze)
                && !ctx.teacherNoSampleNeutral) {
            return true;
        }
        if (c == null) {
            return false;
        }
        String status =
                firstReadString(
                        c,
                        new String[] {
                            "teacherMatrixStatus", "teacherNumberStatus", "teacherStatus"
                        });
        if (isTeacherHardRiskText(status)) {
            return true;
        }
        return readInt(c, "teacherMatrixFreezeFlag", 0) == 1
                || readDouble(c, "teacherMatrixRisk", readDouble(c, "teacherNumberRisk", 0.0d))
                        >= 0.650d;
    }

    private boolean isTeacherHardRiskText(String text) {
        return AppConfig.isExplicitHardBlockStatus(text)
                || "RISK".equals(AppConfig.normalizeToken(text))
                || "FREEZE".equals(AppConfig.normalizeToken(text));
    }

    private boolean isV20WatchOrEmptyNeutralForFinal(Models.ComboCandidate c, DecisionContext ctx) {
        if (ctx != null && ctx.v20WatchNeutral) {
            return true;
        }
        if (c == null) {
            return false;
        }
        String status =
                firstReadString(
                        c,
                        new String[] {
                            "v20Status",
                            "dynamicStatus",
                            "dynamicCauseStatus",
                            "dynamicV20Action",
                            "v20Action"
                        });
        return isV20WatchOrEmptyNeutralText(status);
    }

    private boolean isV20WatchOrEmptyNeutralText(String text) {
        if (text == null || text.length() == 0 || "空".equals(text)) {
            return true;
        }
        String s = text.toUpperCase(java.util.Locale.ROOT);
        return s.indexOf("WATCH") >= 0
                || s.indexOf("PENDING") >= 0
                || s.indexOf("UNKNOWN") >= 0
                || s.indexOf("EMPTY") >= 0
                || s.indexOf("NEUTRAL") >= 0
                || s.indexOf("NO_SAMPLE") >= 0
                || s.indexOf("观察") >= 0
                || s.indexOf("中性") >= 0;
    }

    private boolean isV20HardRiskForFinal(Models.ComboCandidate c, DecisionContext ctx) {
        if (ctx != null && (ctx.v20HardRisk || ctx.v20RiskHigh) && !ctx.v20WatchNeutral) {
            return true;
        }
        if (c == null) {
            return false;
        }
        String status =
                firstReadString(
                        c,
                        new String[] {
                            "v20Status",
                            "dynamicStatus",
                            "dynamicCauseStatus",
                            "v20Action",
                            "dynamicV20Action"
                        });
        if (isV20HardRiskText(status)) {
            return true;
        }
        return readDouble(c, "v20RiskScore", readDouble(c, "dynamicRiskScore", 0.0d))
                        >= V20_RISK_HARD
                || readInt(c, "dynamicHardRisk", 0) == 1
                || readInt(c, "falseStrongHardFreeze", 0) == 1;
    }

    private boolean isV20HardRiskText(String text) {
        return AppConfig.isExplicitHardBlockStatus(text);
    }

    private boolean isAdaptiveWatchNeutralForFinal(Models.ComboCandidate c, DecisionContext ctx) {
        if (ctx != null && ctx.adaptiveWatchNeutral) {
            return true;
        }
        if (c == null) {
            return false;
        }
        String status =
                firstReadString(
                        c,
                        new String[] {
                            "adaptiveJudgeStatus",
                            "adaptiveStatus",
                            "adaptiveAction",
                            "adaptiveJudgeAction"
                        });
        return isAdaptiveWatchNeutralText(status);
    }

    private boolean isAdaptiveWatchNeutralText(String text) {
        if (text == null || text.length() == 0 || "空".equals(text)) {
            return true;
        }
        String s = text.toUpperCase(java.util.Locale.ROOT);
        return s.indexOf("WATCH") >= 0
                || s.indexOf("PENDING") >= 0
                || s.indexOf("NEUTRAL") >= 0
                || s.indexOf("NO_SAMPLE") >= 0
                || s.indexOf("观察") >= 0
                || s.indexOf("中性") >= 0;
    }

    private boolean isAdaptiveHardRiskForFinal(Models.ComboCandidate c, DecisionContext ctx) {
        if (c == null) return false;
        if (readInt(c, "hardBlockFlag", 0) == 1) return true;
        String severity = readString(c, "riskSeverity", "");
        if (AppConfig.isExplicitHardBlockStatus(severity)) return true;
        String status =
                firstReadString(
                        c,
                        new String[] {
                            "adaptiveJudgeStatus",
                            "adaptiveStatus",
                            "adaptiveAction",
                            "adaptiveJudgeAction"
                        });
        if (isAdaptiveWatchNeutralText(status)) return false;
        return AppConfig.isExplicitHardBlockStatus(status);
    }

    private boolean realBetProbeAllowedByConfig(Models.ComboCandidate c, DecisionContext ctx) {
        if (c == null || !c.isValidK3()) {
            return false;
        }
        if (isTeacherHardRiskForFinal(c, ctx)
                || isV20HardRiskForFinal(c, ctx)
                || isAdaptiveHardRiskForFinal(c, ctx)) {
            return false;
        }
        int warmup = readAppInt("REAL_BET_PROBE_MIN_ISSUES", 240);
        int sample = Math.max(c.effectiveSample, c.comboSample);
        sample =
                Math.max(
                        sample,
                        maxReadInt(
                                c,
                                new String[] {
                                    "similarSampleCount",
                                    "similarWindowSample",
                                    "promotionSampleCount",
                                    "shadowPromotionSampleCount"
                                }));
        if (sample < warmup
                && !(ctx != null
                        && (ctx.finalDecisionStrong
                                || ctx.teacherMatrixStrong
                                || ctx.v20Strong
                                || ctx.dynamicCausalStrong))) {
            return false;
        }
        double minP = readAppDouble("REAL_BET_PROBE_MIN_FINAL_P", 0.345d);
        if (c.finalP < minP
                || c.rawP < RAW_FLOOR_STRONG
                || c.finalP < c.dynamicGate
                || c.edge < EDGE_ALLOW_STRONG) {
            return false;
        }
        if (ctx != null
                && (ctx.finalDecisionHardRisk
                        || ctx.finalDecisionFreeze
                        || ctx.finalDiagnosisRiskHigh
                        || ctx.dynamicCausalHardRisk
                        || ctx.dynamicCausalFreeze)) {
            return false;
        }
        return true;
    }

    private boolean containsText(String text, String key) {
        if (text == null || key == null || key.length() == 0) {
            return false;
        }
        return text.toUpperCase(java.util.Locale.ROOT).indexOf(key.toUpperCase(java.util.Locale.ROOT)) >= 0;
    }

    private double finalDiagnosisGateAdjust(
            DecisionContext ctx,
            Models.ComboCandidate c,
            boolean strong,
            boolean promising,
            boolean lowFreqRescue,
            boolean highFreqElite) {
        if (ctx == null) {
            return 0.0d;
        }
        double adj = 0.0d;
        if (ctx.finalDecisionStrong && !ctx.finalDiagnosisRiskHigh && !ctx.finalDecisionFreeze) {
            adj -= 0.004d;
        } else if (ctx.finalDecisionReady && !ctx.finalDiagnosisRiskHigh) {
            adj -= 0.002d;
        }
        if (ctx.finalDiagnosisRiskHigh) {
            adj += 0.008d;
        }
        if (ctx.finalDecisionHardRisk || ctx.finalDecisionFreeze) {
            adj += 0.018d;
        }
        if (ctx.moduleMisleading) {
            adj += 0.006d;
        }
        if (ctx.freezeMatrixRisk) {
            adj += 0.012d;
        }
        if (ctx.teacherMatrixFreeze) {
            adj += 0.010d;
        }
        if (ctx.shadowPromotionStrong
                && ctx.finalDecisionRiskScore < FINAL_DECISION_RISK_LINE
                && (strong || promising || highFreqElite)) {
            adj -= Math.min(FINAL_SHADOW_STRONG_BOOST, Math.max(0.0d, ctx.shadowPromotionBoost));
        }
        return clamp(adj, -0.006d, 0.026d);
    }

    private double calculateMinProbability(
            GateRuntimeContext runtime,
            boolean strong,
            boolean promising,
            boolean lowFreqRescue,
            boolean highFreqElite,
            DecisionContext ctx) {

        boolean teacherStrong = ctx != null && ctx.teacherMatrixStrong;
        boolean teacherReady = ctx != null && ctx.teacherMatrixReady;
        boolean learningStrong = ctx != null && ctx.learningStrong;
        boolean evidenceStrong = ctx != null && ctx.evidenceStrong;
        boolean v20Strong = ctx != null && ctx.v20Strong;
        boolean v20Ready = ctx != null && ctx.v20Ready;

        double min;

        if (lowFreqRescue || strong || teacherStrong || learningStrong || v20Strong) {
            min = STRONG_MIN_P;
        } else if (promising || teacherReady || evidenceStrong || v20Ready) {
            min = READY_MIN_P;
        } else {
            min = NORMAL_MIN_P;
        }

        double f =
                runtime == null
                        ? 0.0d
                        : Math.max(runtime.freq100, Math.max(runtime.freq500, runtime.freq1000));

        if (f >= FREQ_OVERHEAT) {
            min = Math.max(min, OVERHEAT_MIN_P);
        } else if (f >= FREQ_STRONG_HIGH) {
            min = Math.max(min, HIGH_FREQ_MIN_P);
        } else if (f >= FREQ_IDEAL_HIGH) {
            min = Math.max(min, ELITE_MIN_P);
        }

        if (ctx != null && ctx.teacherMatrixRisk && !ctx.teacherMatrixStrong) {
            min += 0.004d;
        }

        if (ctx != null && ctx.v20RiskHigh && !ctx.v20Strong) {
            min += 0.004d;
        }

        if (ctx != null && ctx.v20HardRisk && !ctx.v20Elite) {
            min += 0.006d;
        }

        if (ctx != null && ctx.learningWeak && !learningStrong) {
            min += 0.002d;
        }
        if (ctx != null && ctx.dynamicCausalStrong) {
            min -= 0.004d;
        } else if (ctx != null && ctx.dynamicCausalReady) {
            min -= 0.002d;
        } else if (ctx != null && ctx.dynamicCausalRiskHigh) {
            min += 0.006d;
        }
        if (ctx != null && ctx.dynamicCausalHardRisk) {
            min += 0.012d;
        }

        double floor = minRealBetProbability();

        if (floor > 0.0d && min < floor) {
            min = floor;
        }

        if (min > 0.420d) {
            min = 0.420d;
        }

        return AppConfig.clampProbability(min);
    }

    private double calculateRawFloor(
            boolean strong,
            boolean promising,
            boolean lowFreqRescue,
            boolean highFreqElite,
            DecisionContext ctx) {

        if (lowFreqRescue
                || strong
                || (ctx != null
                        && (ctx.teacherMatrixStrong || ctx.learningStrong || ctx.v20Strong))) {
            return RAW_FLOOR_STRONG;
        }

        if (promising
                || (ctx != null
                        && (ctx.teacherMatrixReady || ctx.evidenceStrong || ctx.v20Ready))) {
            return RAW_FLOOR_READY;
        }

        if (highFreqElite) {
            return RAW_FLOOR_NORMAL;
        }

        return RAW_FLOOR_NORMAL;
    }

    private double calculateEdgeFloor(
            boolean strong,
            boolean promising,
            boolean lowFreqRescue,
            boolean highFreqElite,
            DecisionContext ctx) {

        if (lowFreqRescue
                || strong
                || (ctx != null
                        && (ctx.teacherMatrixStrong || ctx.learningStrong || ctx.v20Strong))) {
            return EDGE_ALLOW_STRONG;
        }

        if (promising
                || (ctx != null
                        && (ctx.teacherMatrixReady || ctx.evidenceStrong || ctx.v20Ready))) {
            return EDGE_ALLOW_READY;
        }

        if (highFreqElite) {
            return EDGE_MIN_NORMAL;
        }

        return EDGE_MIN_NORMAL;
    }

    private double frequencySoftGateAdjust(
            GateRuntimeContext runtime,
            boolean strong,
            boolean promising,
            boolean lowFreqRescue,
            DecisionContext ctx) {

        if (runtime == null) {
            return 0.0d;
        }

        double f = Math.max(runtime.freq100, Math.max(runtime.freq500, runtime.freq1000));

        /* 链路修改升级V2：低频不再降低正常执行门，只能走独立合格探针门。 */
        if (f <= FREQ_IDEAL_HIGH) {
            return 0.0d;
        }

        if (f < FREQ_STRONG_HIGH) {
            return 0.002d;
        }

        if (f < FREQ_OVERHEAT) {
            return 0.004d;
        }

        return 0.004d;
    }

    private String buildFrequencyReason(GateRuntimeContext runtime) {
        if (runtime == null) {
            return "频率状态：未读取";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("频率状态：Freq100=");
        sb.append(UiFormatter.probability(runtime.freq100));
        sb.append("，Freq500=");
        sb.append(UiFormatter.probability(runtime.freq500));
        sb.append("，Freq1000=");
        sb.append(UiFormatter.probability(runtime.freq1000));

        double f = Math.max(runtime.freq100, Math.max(runtime.freq500, runtime.freq1000));

        if (f <= FREQ_LOW) {
            sb.append("，低频强信号可软救援");
        } else if (f <= FREQ_IDEAL_HIGH) {
            sb.append("，正常频率");
        } else if (f <= FREQ_STRONG_HIGH) {
            sb.append("，频率偏高，仅轻抬门");
        } else if (f <= FREQ_OVERHEAT) {
            sb.append("，频率较高，要求候选更强但不硬拦");
        } else {
            sb.append("，频率过热，仍不硬拦，只要求FinalP/RawP/Edge/风险更扎实");
        }

        return sb.toString();
    }

    private boolean hasRealBetEvidence(
            Models.ComboCandidate c,
            DecisionContext ctx,
            boolean strong,
            boolean promising,
            boolean lowFreqRescue,
            boolean highFreqElite) {

        if (c == null || !c.isValidK3()) {
            return false;
        }

        if (isTerminalBadState(c.stateMode)) {
            return false;
        }

        if (c.driftRisk > AppConfig.DRIFT_RISK_MAX + 8.0d) {
            return false;
        }

        if (c.breakDepth > AppConfig.BREAK_DEPTH_MAX + 8.0d) {
            return false;
        }

        if (c.finalP < c.dynamicGate) {
            return false;
        }

        boolean teacherSupport = ctx != null && (ctx.teacherMatrixStrong || ctx.teacherMatrixReady);
        boolean learningSupport = ctx != null && ctx.learningStrong;
        boolean evidenceSupport =
                ctx != null
                        && (ctx.evidenceStrong
                                || ctx.decisionEvidenceStrong
                                || ctx.decisionEvidenceElite);
        boolean v20Support = ctx != null && (ctx.v20Strong || ctx.v20Ready);
        boolean decisionSupport =
                ctx != null && (ctx.decisionEvidenceStrong || ctx.decisionEvidenceElite);
        boolean dynamicSupport = ctx != null && (ctx.dynamicCausalReady || ctx.dynamicCausalStrong);

        if (ctx != null && ctx.v20HardRisk && !ctx.v20Elite) {
            return false;
        }

        if (ctx != null && ctx.decisionHardRisk && !ctx.decisionEvidenceElite) {
            return false;
        }

        if (ctx != null
                && ctx.decisionCounterRiskHigh
                && !decisionSupport
                && !teacherSupport
                && !v20Support
                && !dynamicSupport) {
            return false;
        }
        if (ctx != null && ctx.finalDecisionHardRisk && !ctx.finalDecisionStrong) {
            return false;
        }
        if (ctx != null && ctx.finalDiagnosisRiskHigh && !ctx.finalDecisionStrong) {
            return false;
        }
        if (ctx != null && ctx.finalDecisionFreeze) {
            return false;
        }
        if (ctx == null
                || !dynamicSupport
                || ctx.dynamicCausalHardRisk
                || ctx.dynamicCausalFreeze
                || ctx.dynamicCausalRiskHigh) {
            return false;
        }
        if (finalBetStrictBlockCode(c, ctx).length() > 0) {
            return false;
        }

        if (finalPreferredReviewBetDomain(c, ctx)
                && c.finalP >= FINAL_REVIEW_BET_MIN_P
                && c.rawP >= FINAL_REVIEW_BET_MIN_RAW
                && !isRealRiskHigh(c)
                && !isTerminalBadState(c.stateMode)) {
            markFinalPreferredReviewBetDomain(c, "REAL_BET_EVIDENCE");
            return true;
        }

        if (lowFreqRescue
                && c.finalP >= STRONG_MIN_P
                && c.rawP >= RAW_FLOOR_STRONG
                && c.evidenceRisk <= 0.700d) {
            return true;
        }

        if (strong
                && c.finalP >= STRONG_MIN_P
                && c.rawP >= RAW_FLOOR_STRONG
                && c.edge >= EDGE_ALLOW_STRONG
                && c.driftRisk <= AppConfig.DRIFT_RISK_MAX + 5.0d
                && c.breakDepth <= AppConfig.BREAK_DEPTH_MAX + 5.0d) {
            return true;
        }

        if (promising
                && c.finalP >= READY_MIN_P
                && c.rawP >= RAW_FLOOR_READY
                && c.edge >= EDGE_ALLOW_READY
                && c.evidenceRisk <= 0.700d
                && c.driftRisk <= AppConfig.DRIFT_RISK_MAX + 6.0d
                && c.breakDepth <= AppConfig.BREAK_DEPTH_MAX + 6.0d) {
            return true;
        }

        if (teacherSupport
                && c.finalP >= READY_MIN_P
                && c.rawP >= RAW_FLOOR_STRONG
                && c.edge >= EDGE_ALLOW_READY
                && c.evidenceRisk <= 0.720d
                && !isRealRiskHigh(c)) {
            return true;
        }

        if (learningSupport
                && c.finalP >= READY_MIN_P
                && c.rawP >= RAW_FLOOR_STRONG
                && c.edge >= EDGE_ALLOW_READY
                && c.evidenceRisk <= 0.720d
                && !isRealRiskHigh(c)) {
            return true;
        }

        if (evidenceSupport
                && c.finalP >= READY_MIN_P
                && c.rawP >= RAW_FLOOR_STRONG
                && c.edge >= EDGE_ALLOW_READY
                && c.evidenceRisk <= 0.720d) {
            return true;
        }

        if (dynamicSupport
                && c.finalP >= READY_MIN_P
                && c.rawP >= RAW_FLOOR_READY
                && c.edge >= EDGE_ALLOW_READY
                && c.evidenceRisk <= 0.720d
                && !isRealRiskHigh(c)) {
            return true;
        }

        if (v20Support
                && c.finalP >= READY_MIN_P
                && c.rawP >= RAW_FLOOR_STRONG
                && c.edge >= EDGE_ALLOW_READY
                && c.evidenceRisk <= 0.720d
                && (ctx == null || !ctx.v20RiskHigh || ctx.v20Strong)) {
            return true;
        }

        if (highFreqElite
                && c.finalP >= HIGH_FREQ_MIN_P
                && c.rawP >= RAW_FLOOR_NORMAL
                && c.edge >= EDGE_MIN_NORMAL
                && c.evidenceRisk <= 0.700d
                && !isTerminalBadState(c.stateMode)) {
            return true;
        }

        if (c.finalP >= NORMAL_MIN_P
                && c.rawP >= RAW_FLOOR_NORMAL
                && c.edge >= EDGE_MIN_NORMAL
                && c.evidenceRisk <= 0.700d
                && !isTerminalBadState(c.stateMode)) {
            return true;
        }

        return false;
    }

    private boolean hasHardRisk(Models.ComboCandidate c, DecisionContext ctx) {
        if (c == null) {
            return true;
        }

        if (c.driftRisk >= AppConfig.DRIFT_RISK_HARD_BLOCK) {
            return true;
        }

        if (c.breakDepth >= breakDepthHardBlock()) {
            return true;
        }

        if (isTerminalBadState(c.stateMode)) {
            return true;
        }

        if (c.evidenceRisk >= 0.780d) {
            return true;
        }

        if (ctx != null
                && ctx.teacherMatrixRisk
                && !ctx.teacherMatrixStrong
                && c.finalP < 0.400d
                && c.edge < 0.006d) {
            return true;
        }

        if (ctx != null && ctx.v20HardRisk && !ctx.v20Elite) {
            return true;
        }

        if (ctx != null && ctx.decisionHardRisk && !ctx.decisionEvidenceElite) {
            return true;
        }
        if (ctx != null
                && (ctx.dynamicCausalHardRisk
                        || ctx.dynamicCausalFreeze
                        || ctx.dynamicCausalRiskHigh)) {
            return true;
        }

        return false;
    }

    private boolean isStrongEnoughForRealtime(
            Models.ComboCandidate c,
            TrendStateMachine.TrendTrace trace,
            BreakRecoveryEngine.RecoveryResult recovery,
            DecisionContext ctx) {

        if (c == null || !c.isValidK3()) {
            return false;
        }

        if (isTerminalBadState(c.stateMode)) {
            return false;
        }

        if (c.driftRisk > AppConfig.DRIFT_RISK_MAX + 5.0d) {
            return false;
        }

        if (c.breakDepth > AppConfig.BREAK_DEPTH_MAX + 5.0d) {
            return false;
        }

        if (trace != null && trace.hardBlock == 1) {
            return false;
        }

        if (recovery != null && recoveryEngine.isHardBlock(recovery)) {
            return false;
        }

        if (ctx != null && ctx.v20HardRisk && !ctx.v20Elite) {
            return false;
        }

        if (c.evidenceRisk >= EVIDENCE_RISK_HIGH
                && (ctx == null
                        || (!ctx.evidenceStrong
                                && !ctx.teacherMatrixStrong
                                && !ctx.learningStrong
                                && !ctx.v20Strong))) {
            return false;
        }

        if (c.finalP < STRONG_MIN_P) {
            return false;
        }

        if (c.rawP < RAW_FLOOR_STRONG) {
            return false;
        }

        boolean trendOk = c.trendLife >= trendLifeMin();
        boolean recoveryOk = c.recoveryScore >= AppConfig.RECOVERY_SCORE_MIN;
        boolean sampleOk = c.effectiveSample >= 100 || c.comboSample >= 80;
        boolean hitRateOk = c.comboHitRate >= 0.355d || c.similarHitRate >= 0.355d;
        boolean teacherOk = ctx != null && ctx.teacherMatrixStrong;
        boolean learningOk = ctx != null && ctx.learningStrong;
        boolean evidenceOk = ctx != null && ctx.evidenceStrong;
        boolean v20Ok = ctx != null && ctx.v20Strong;

        if (c.finalP >= READY_MIN_P
                && c.rawP >= RAW_FLOOR_STRONG
                && c.evidenceRisk <= 0.700d
                && (trendOk
                        || recoveryOk
                        || sampleOk
                        || hitRateOk
                        || teacherOk
                        || learningOk
                        || evidenceOk
                        || v20Ok)) {
            return true;
        }

        if (c.finalP >= STRONG_MIN_P
                && c.rawP >= RAW_FLOOR_STRONG
                && c.driftRisk <= AppConfig.DRIFT_RISK_MAX
                && c.breakDepth <= AppConfig.BREAK_DEPTH_MAX
                && (c.trendLife >= 55.0d || teacherOk || learningOk || evidenceOk || v20Ok)) {
            return true;
        }

        if (c.finalP >= NORMAL_MIN_P
                && c.rawP >= RAW_FLOOR_NORMAL
                && c.evidenceRisk <= 0.680d
                && !isRealRiskHigh(c)) {
            return true;
        }

        return false;
    }

    private boolean isPromisingCandidate(
            Models.ComboCandidate c,
            TrendStateMachine.TrendTrace trace,
            BreakRecoveryEngine.RecoveryResult recovery,
            DecisionContext ctx) {

        if (c == null || !c.isValidK3()) {
            return false;
        }

        if (isTerminalBadState(c.stateMode)) {
            return false;
        }

        if (c.finalP < STRONG_MIN_P) {
            return false;
        }

        if (c.rawP < RAW_FLOOR_READY) {
            return false;
        }

        if (c.driftRisk > AppConfig.DRIFT_RISK_MAX + 7.0d) {
            return false;
        }

        if (c.breakDepth > AppConfig.BREAK_DEPTH_MAX + 7.0d) {
            return false;
        }

        if (trace != null && trace.hardBlock == 1) {
            return false;
        }

        if (recovery != null && recoveryEngine.isHardBlock(recovery)) {
            return false;
        }

        if (ctx != null && ctx.v20HardRisk && !ctx.v20Elite) {
            return false;
        }

        if (ctx != null
                && ctx.realRiskHigh
                && !ctx.evidenceStrong
                && !ctx.teacherMatrixStrong
                && !ctx.learningStrong
                && !ctx.v20Strong
                && c.finalP < 0.400d) {
            return false;
        }

        boolean teacherOk = ctx != null && (ctx.teacherMatrixStrong || ctx.teacherMatrixReady);
        boolean learningOk = ctx != null && ctx.learningStrong;
        boolean evidenceOk = ctx != null && ctx.evidenceStrong;
        boolean v20Ok = ctx != null && (ctx.v20Strong || ctx.v20Ready);

        if (c.finalP >= READY_MIN_P
                && c.rawP >= RAW_FLOOR_READY
                && c.evidenceRisk <= 0.720d
                && (c.trendLife >= 45.0d
                        || c.recoveryScore >= AppConfig.RECOVERY_SCORE_MIN
                        || c.effectiveSample >= 90
                        || c.comboHitRate >= 0.345d
                        || teacherOk
                        || learningOk
                        || evidenceOk
                        || v20Ok)) {
            return true;
        }

        if (c.finalP >= NORMAL_MIN_P && c.rawP >= RAW_FLOOR_NORMAL && c.evidenceRisk <= 0.700d) {
            return true;
        }

        return false;
    }

    private String hardBlockCode(
            Models.ComboCandidate c,
            TrendStateMachine.TrendTrace trace,
            BreakRecoveryEngine.RecoveryResult recovery,
            Models.SimilarResult similar,
            ShapeMemoryEngine.MemorySignal memory,
            CalibrationEngine.CalibrationSignal calibration,
            GateRuntimeContext runtime,
            boolean strong,
            boolean promising,
            DecisionContext ctx) {

        if (c == null || !c.isValidK3()) {
            return AppConfig.BLOCK_COMBO_WEAK;
        }

        if (c.blockCode != null && c.blockCode.length() > 0) {
            if (AppConfig.BLOCK_DRIFT_HIGH.equals(c.blockCode)
                    || AppConfig.BLOCK_BREAK_HIGH.equals(c.blockCode)
                    || AppConfig.BLOCK_RECOVERY_WEAK.equals(c.blockCode)
                    || AppConfig.BLOCK_DATA_ERROR.equals(c.blockCode)
                    || trendWeakBlockCode().equals(c.blockCode)) {
                return c.blockCode;
            }
        }

        if (c.driftRisk >= AppConfig.DRIFT_RISK_HARD_BLOCK) {
            return AppConfig.BLOCK_DRIFT_HIGH;
        }

        if (c.breakDepth >= breakDepthHardBlock()) {
            return AppConfig.BLOCK_BREAK_HIGH;
        }

        if (isTerminalBadState(c.stateMode)) {
            return trendWeakBlockCode();
        }

        if (trace != null && trace.hardBlock == 1) {
            return trendWeakBlockCode();
        }

        if (recovery != null && recoveryEngine.isHardBlock(recovery)) {
            return recovery.blockCode == null || recovery.blockCode.length() == 0
                    ? AppConfig.BLOCK_RECOVERY_WEAK
                    : recovery.blockCode;
        }

        if (ctx != null
                && ctx.teacherMatrixRisk
                && !ctx.teacherMatrixStrong
                && c.finalP < 0.400d
                && c.edge < 0.006d) {
            return teacherMatrixBlockCode();
        }

        if (ctx != null
                && ctx.v20HardRisk
                && !ctx.v20Elite
                && c.finalP < 0.430d
                && c.edge < 0.018d) {
            return ctx.v20HardBlockCode.length() > 0 ? ctx.v20HardBlockCode : "V20_CAUSE_HARD_RISK";
        }

        if (similar != null
                && similar.effectiveSample >= AppConfig.EFFECTIVE_SAMPLE_HIGH
                && similar.similarHitRate < 0.265d
                && c.finalP < 0.395d
                && !strong
                && (ctx == null
                        || (!ctx.teacherMatrixStrong && !ctx.learningStrong && !ctx.v20Strong))) {
            return AppConfig.BLOCK_SAMPLE_WEAK;
        }

        if (memory != null
                && !memoryEngine.memoryAllowsBet(memory)
                && c.finalP < 0.395d
                && c.edge < 0.002d
                && !strong
                && (ctx == null
                        || (!ctx.teacherMatrixStrong && !ctx.learningStrong && !ctx.v20Strong))) {
            return AppConfig.BLOCK_COMBO_WEAK;
        }

        if (calibration != null
                && calibration.error > AppConfig.CALIBRATION_ERROR_HARD
                && c.edge < 0.006d
                && c.finalP < 0.400d
                && !strong
                && (ctx == null
                        || (!ctx.teacherMatrixStrong && !ctx.learningStrong && !ctx.v20Strong))) {
            return AppConfig.BLOCK_LOW_PROBABILITY;
        }

        if (c.evidenceRisk >= 0.780d
                && c.evidenceScore < EVIDENCE_STRONG
                && c.finalP < 0.405d
                && !strong
                && (ctx == null
                        || (!ctx.teacherMatrixStrong && !ctx.learningStrong && !ctx.v20Strong))) {
            return AppConfig.BLOCK_LOW_PROBABILITY;
        }

        return "";
    }

    private boolean calibrationAllowsSoft(
            CalibrationEngine.CalibrationSignal calibration,
            double edge,
            boolean strong,
            boolean promising,
            boolean lowFreqRescue,
            boolean highFreqElite,
            DecisionContext ctx) {

        if (calibration == null) {
            return true;
        }

        if (calibration.error <= AppConfig.CALIBRATION_ERROR_MAX) {
            return true;
        }

        if (edge >= 0.006d && calibration.error <= 0.120d) {
            return true;
        }

        if ((strong || promising || lowFreqRescue)
                && edge >= -0.002d
                && calibration.error <= 0.130d) {
            return true;
        }

        if (ctx != null
                && (ctx.teacherMatrixStrong || ctx.learningStrong || ctx.v20Strong)
                && edge >= -0.002d
                && calibration.error <= 0.130d) {
            return true;
        }

        return false;
    }

    private int compareCandidateTie(Models.ComboCandidate a, Models.ComboCandidate b) {
        if (a == null && b == null) {
            return 0;
        }

        if (a == null) {
            return 1;
        }

        if (b == null) {
            return -1;
        }

        if (a.finalP < b.finalP) {
            return 1;
        }

        if (a.finalP > b.finalP) {
            return -1;
        }

        if (a.edge < b.edge) {
            return 1;
        }

        if (a.edge > b.edge) {
            return -1;
        }

        if (a.position != b.position) {
            return a.position - b.position;
        }

        return a.k3Mask - b.k3Mask;
    }

    private void addUniqueCandidate(
            ArrayList<Models.ComboCandidate> list, Models.ComboCandidate c) {
        if (list == null || c == null || !c.isValidK3()) {
            return;
        }

        if (candidateExists(list, c)) {
            return;
        }

        list.add(c);
    }

    private boolean candidateExists(
            ArrayList<Models.ComboCandidate> list, Models.ComboCandidate c) {
        if (list == null || c == null) {
            return false;
        }

        String key = candidateKey(c);

        for (int i = 0; i < list.size(); i++) {
            Models.ComboCandidate old = list.get(i);

            if (old == null) {
                continue;
            }

            if (key.equals(candidateKey(old))) {
                return true;
            }
        }

        return false;
    }

    private String candidateKey(Models.ComboCandidate c) {
        if (c == null) {
            return "";
        }

        repairCandidateK3(c);

        return String.valueOf(c.position) + "_" + String.valueOf(c.k3Mask);
    }

    private boolean isFastAcceptBet(
            Models.ComboCandidate c, FinalCheck check, GateRuntimeContext runtime) {
        if (c == null || check == null || !check.allowBet) {
            return false;
        }

        double f =
                runtime == null
                        ? 0.0d
                        : Math.max(runtime.freq100, Math.max(runtime.freq500, runtime.freq1000));

        if (f >= FREQ_STRONG_HIGH) {
            return false;
        }

        if (readInt(c, "finalDecisionHardRisk", 0) == 1
                || readInt(c, "finalDecisionFreezeFlag", 0) == 1) {
            return false;
        }

        if (readDouble(c, "finalDecisionRiskScore", 0.0d) >= FINAL_DECISION_RISK_LINE) {
            return false;
        }

        if (c.finalP >= 0.420d
                && c.rawP >= RAW_FLOOR_ELITE
                && c.edge >= 0.008d
                && c.evidenceRisk <= 0.580d
                && !isTerminalBadState(c.stateMode)) {
            return true;
        }

        if (c.finalP >= 0.405d
                && c.rawP >= RAW_FLOOR_NORMAL
                && c.edge >= 0.004d
                && isEvidenceStrong(c)
                && !isRealRiskHigh(c)
                && !isTerminalBadState(c.stateMode)) {
            return true;
        }

        String tmStatus = readString(c, "teacherMatrixStatus", "");
        if (("STRONG".equals(tmStatus) || "READY".equals(tmStatus) || "NORMAL".equals(tmStatus))
                && c.finalP >= 0.390d
                && c.rawP >= RAW_FLOOR_READY
                && c.edge >= EDGE_ALLOW_READY
                && !isRealRiskHigh(c)) {
            return true;
        }

        return isV20FastAccept(c);
    }

    private double finalDecisionScore(Models.ComboCandidate c, FinalCheck check) {
        if (c == null) {
            return -999.0d;
        }

        double score = quickFinalScore(c, null, null);

        score += c.finalP * 0.95d;
        score += c.rawP * 0.40d;
        score += c.edge * 1.20d;

        score += c.evidenceScore * 0.030d;
        score += c.profileScore * 0.010d;
        score += c.fallbackProfileScore * 0.006d;
        score += c.aPlusScore * 0.006d;
        score += c.promotionScore * 0.006d;

        score += v20DecisionScoreBonus(c);

        score -= c.evidenceRisk * 0.050d;
        score -= c.auditPenalty * 0.050d;
        score -= v20DecisionRiskPenalty(c);

        int tmApplied = readInt(c, "teacherMatrixApplied", 0);
        int tmSample = readInt(c, "teacherMatrixSample", readInt(c, "teacherNumberSample", 0));
        int tmComboSample = readInt(c, "teacherMatrixComboSample", tmSample);
        double tmHit =
                readDouble(c, "teacherMatrixHitRate", readDouble(c, "teacherNumberHitRate", 0.0d));
        double tmComboHit =
                readDouble(
                        c,
                        "teacherMatrixRecentHitRate",
                        readDouble(c, "teacherMatrixComboHitRate", tmHit));
        double tmConf =
                readDouble(
                        c,
                        "teacherMatrixConfidence",
                        readDouble(c, "teacherNumberConfidence", 0.0d));
        double tmRisk =
                readDouble(
                        c,
                        "teacherMatrixRiskPenalty",
                        readDouble(
                                c, "teacherMatrixRisk", readDouble(c, "teacherNumberRisk", 0.0d)));
        String tmStatus =
                readString(c, "teacherMatrixStatus", readString(c, "teacherNumberStatus", ""));

        if (tmApplied == 1 || tmSample > 0 || tmComboSample > 0 || tmConf > 0.0d) {
            score += tmHit * 0.012d;
            score += tmComboHit * 0.010d;
            score += tmConf * 0.006d;
            score -= tmRisk * 0.060d;

            if ("STRONG".equals(tmStatus)) {
                score += 0.020d;
            } else if ("READY".equals(tmStatus) || "NORMAL".equals(tmStatus)) {
                score += 0.012d;
            } else if ("RISK".equals(tmStatus)) {
                score -= 0.035d;
            }
        }

        if (check != null) {
            if (check.allowBet) {
                score += 10.0d;
            } else if (check.allowShadow) {
                score += 1.0d;
            } else {
                score -= 1.0d;
            }
        }

        return score;
    }

    private double quickFinalScore(
            Models.ComboCandidate c,
            TrendStateMachine.TrendTrace trace,
            BreakRecoveryEngine.RecoveryResult recovery) {

        if (c == null) {
            return -999.0d;
        }

        double score = c.finalP;

        score += c.rawP * 0.30d;
        score += c.edge * 0.50d;
        score += c.championGap * 0.08d;
        score += c.comboGap * 0.10d;
        score += c.trendLife / 100.0d * 0.018d;
        score += c.recoveryScore / 100.0d * 0.014d;
        score -= c.driftRisk / 100.0d * 0.018d;
        score -= c.breakDepth / 100.0d * 0.018d;

        score += c.deepScore * 0.006d;
        score += c.evidenceScore * 0.012d;
        score += c.aiJudgeScore * 0.006d;
        score += c.profileScore * 0.006d;
        score += c.fallbackProfileScore * 0.004d;
        score += c.promotionScore * 0.004d;
        score += c.aPlusScore * 0.004d;
        score -= c.evidenceRisk * 0.020d;
        score -= c.auditPenalty * 0.030d;
        score -= c.conflictLevel * 0.014d;

        score += v20QuickScoreBonus(c);
        score -= v20QuickRiskPenalty(c);

        String tmStatus =
                readString(c, "teacherMatrixStatus", readString(c, "teacherNumberStatus", ""));
        double tmRisk =
                readDouble(
                        c,
                        "teacherMatrixRiskPenalty",
                        readDouble(
                                c, "teacherMatrixRisk", readDouble(c, "teacherNumberRisk", 0.0d)));
        double tmHit =
                readDouble(c, "teacherMatrixHitRate", readDouble(c, "teacherNumberHitRate", 0.0d));
        double tmConf =
                readDouble(
                        c,
                        "teacherMatrixConfidence",
                        readDouble(c, "teacherNumberConfidence", 0.0d));
        int tmApplied = readInt(c, "teacherMatrixApplied", 0);
        int tmSample = readInt(c, "teacherMatrixSample", readInt(c, "teacherNumberSample", 0));
        int tmComboSample = readInt(c, "teacherMatrixComboSample", tmSample);

        if (tmApplied == 1 || tmSample > 0 || tmComboSample > 0 || tmConf > 0.0d) {
            score += tmHit * 0.006d;
            score += tmConf * 0.004d;
            score -= tmRisk * 0.040d;

            if ("STRONG".equals(tmStatus)) {
                score += 0.018d;
            } else if ("READY".equals(tmStatus) || "NORMAL".equals(tmStatus)) {
                score += 0.010d;
            } else if ("RISK".equals(tmStatus)) {
                score -= 0.030d;
            }
        }

        if (c.profileScore > 0.0d && c.profileScore < LEARNING_WEAK) {
            score -= 0.004d;
        }

        if (trace != null) {
            score += trace.trendContinuity / 100.0d * 0.006d;
            score -= trace.modeSwitchRisk / 100.0d * 0.008d;

            if (trace.hardBlock == 1) {
                score -= 0.060d;
            }
        }

        if (recovery != null) {
            if (recoveryEngine.isSafeRecovery(recovery)) {
                score += 0.006d;
            }

            if (recoveryEngine.isHardBlock(recovery)) {
                score -= 0.060d;
            }
        }

        if (isTerminalBadState(c.stateMode)) {
            score -= 0.050d;
        } else if (isChaosOnly(c.stateMode)) {
            score -= 0.006d;
        }

        if (c.blockCode != null
                && c.blockCode.length() > 0
                && !AppConfig.BLOCK_LOW_PROBABILITY.equals(c.blockCode)
                && !AppConfig.BLOCK_SAMPLE_WEAK.equals(c.blockCode)
                && !AppConfig.BLOCK_COMBO_WEAK.equals(c.blockCode)
                && !AppConfig.BLOCK_BACKTEST_WEAK.equals(c.blockCode)
                && !teacherMatrixBlockCode().equals(c.blockCode)
                && !"V20_CAUSE_RISK".equals(c.blockCode)
                && !"DYNAMIC_CAUSE_WEAK".equals(c.blockCode)
                && !"FALSE_STRONG_FREEZE".equals(c.blockCode)) {
            score -= 0.018d;
        }

        if (finalFieldChainHardBlockCode(c, null).length() > 0) {
            score -= 0.180d;
        }

        return score;
    }

    private double recentBetFrequency(DbStore store, int limit) {
        if (store == null) {
            return 0.0d;
        }

        if (limit <= 0) {
            limit = 100;
        }

        try {
            List<Models.Decision> list = store.getRecentDecisions(limit * 5);

            if (list == null || list.size() == 0) {
                return 0.0d;
            }

            ArrayList<String> totalIssues = new ArrayList<String>();
            ArrayList<String> betIssues = new ArrayList<String>();

            for (int i = 0; i < list.size(); i++) {
                Models.Decision d = list.get(i);

                if (d == null || d.targetIssue == null || d.targetIssue.length() == 0) {
                    continue;
                }

                if (isWeakPlaceholderDecision(d)) {
                    continue;
                }

                String decisionText = normalizeDecisionText(d.decision, d.action);

                if (AppConfig.DECISION_SHADOW.equals(decisionText)) {
                    continue;
                }

                if (!totalIssues.contains(d.targetIssue)) {
                    totalIssues.add(d.targetIssue);
                }

                if (AppConfig.DECISION_BET.equals(decisionText)
                        && isRealScope(d.statScope)
                        && isDecisionValidBet(d)
                        && !betIssues.contains(d.targetIssue)) {
                    betIssues.add(d.targetIssue);
                }

                if (totalIssues.size() >= limit) {
                    break;
                }
            }

            if (totalIssues.size() <= 0) {
                return 0.0d;
            }

            return ((double) betIssues.size()) / ((double) totalIssues.size());
        } catch (Throwable e) {
            return 0.0d;
        }
    }

    private boolean isWeakPlaceholderDecision(Models.Decision d) {
        if (d == null) {
            return true;
        }

        if (d.targetIssue == null || d.targetIssue.length() == 0) {
            return true;
        }

        String text = "";

        if (d.reasonSummary != null) {
            text += d.reasonSummary;
        }

        if (d.reasonText != null) {
            text += " " + d.reasonText;
        }

        if (d.blockCode != null) {
            text += " " + d.blockCode;
        }

        if (text.indexOf("临时") >= 0
                || text.indexOf("占位") >= 0
                || text.indexOf("正在生成") >= 0
                || text.indexOf("等待实时分析") >= 0
                || text.indexOf("未生成有效决策") >= 0
                || text.indexOf("快速观察") >= 0
                || text.indexOf("QUICK_OBSERVE") >= 0
                || text.indexOf("兜底") >= 0
                || text.indexOf("正式结果完成后覆盖") >= 0
                || text.indexOf("只解决界面有预测") >= 0) {
            return true;
        }

        if (AppConfig.DECISION_HOLD.equals(d.decision)
                && d.position <= 0
                && d.finalP <= 0.0001d
                && !Models.isValidK3Mask(d.k3Mask)) {
            return true;
        }

        return false;
    }

    private boolean isDecisionValidBet(Models.Decision d) {
        if (d == null) {
            return false;
        }

        String decisionText = normalizeDecisionText(d.decision, d.action);

        if (!AppConfig.DECISION_BET.equals(decisionText)) {
            return false;
        }

        if (!AppConfig.isValidPosition(d.position)) {
            return false;
        }

        repairDecisionK3(d);

        if (!Models.isValidK3Mask(d.k3Mask)) {
            return false;
        }

        if (!AppConfig.isValidNumber(d.n1)
                || !AppConfig.isValidNumber(d.n2)
                || !AppConfig.isValidNumber(d.n3)) {
            return false;
        }

        return true;
    }

    private Models.SimilarResult safeSimilar(List<Models.Draw> history, Models.ComboCandidate c) {
        try {
            return similarShapeEngine.evaluateCandidate(history, c);
        } catch (Throwable e) {
            return null;
        }
    }

    private ShapeMemoryEngine.MemorySignal safeMemory(
            Models.ComboCandidate c, List<Models.Draw> history) {
        try {
            return memoryEngine.evaluateCandidate(c, history);
        } catch (Throwable e) {
            return null;
        }
    }

    private CalibrationEngine.CalibrationSignal safeCalibration(DbStore store, double rawP) {
        try {
            return calibrationEngine.inspect(store, rawP);
        } catch (Throwable e) {
            return null;
        }
    }

    private boolean hasOpenTradeForTarget(DbStore store, String targetIssue) {
        if (store == null || targetIssue == null || targetIssue.length() == 0) {
            return false;
        }

        Object old = null;

        try {
            old = store.getTradeByTargetIssue(targetIssue);
        } catch (Throwable e) {
            old = null;
        }

        if (old == null) {
            return false;
        }

        String statScope = readString(old, "statScope", "");
        int settled = readInt(old, "settled", 0);
        String result = readString(old, "result", "");
        String action = readString(old, "action", "");

        if (!isRealScope(statScope)) {
            return false;
        }

        if (action != null && action.length() > 0) {
            String actionText = normalizeDecisionText(action, action);
            if (!AppConfig.DECISION_BET.equals(actionText)) {
                return false;
            }
        }

        if (settled == 0) {
            return true;
        }

        return AppConfig.RESULT_WAIT.equals(normalizeResultText(result));
    }

    private Models.Decision formalHold(
            String baseIssue, String targetIssue, String blockCode, String reason, int dataOk) {
        Models.Decision d = new Models.Decision();

        d.baseIssue = baseIssue == null ? "" : baseIssue;
        d.targetIssue = targetIssue == null ? "" : targetIssue;
        d.createdAt = System.currentTimeMillis();

        d.decision = AppConfig.DECISION_HOLD;
        d.action = AppConfig.DECISION_HOLD;

        d.position = 0;
        d.setNumbers(0, 0, 0);
        d.k3Mask = 0;

        d.rawP = 0.0d;
        d.calibratedP = 0.0d;
        d.finalP = 0.0d;
        d.dynamicGate = AppConfig.HIGH_ACCURACY_GATE;
        d.edge = -1.0d;

        d.blockCode = blockCode == null ? AppConfig.BLOCK_LOW_PROBABILITY : blockCode;
        d.reasonSummary = reason == null ? "当前目标期正式HOLD" : reason;
        d.reasonText = d.reasonSummary;

        d.stateMode = AppConfig.MODE_CHAOS;
        d.shapeKey = "";
        d.dataOk = dataOk;
        d.settled = 0;
        d.result = AppConfig.RESULT_WAIT;
        d.statScope = AppConfig.STAT_SCOPE_HOLD;

        d.syncAction();

        return d;
    }

    private void applyFieldChainDecisionBridge(Models.Decision d, Models.ComboCandidate c) {
        if (d == null || c == null) {
            return;
        }
        writeInt(d, "teacherWeakNoBetFlag", readInt(c, "teacherWeakNoBetFlag", 0));
        writeInt(d, "teacherMatrixWeakNoBetFlag", readInt(c, "teacherMatrixWeakNoBetFlag", 0));
        writeInt(
                d,
                "teacherMatrixOrdinaryBetBlockFlag",
                readInt(c, "teacherMatrixOrdinaryBetBlockFlag", 0));
        writeInt(
                d,
                "teacherMatrixSupportsFinalGate",
                readInt(c, "teacherMatrixSupportsFinalGate", 0));
        writeInt(d, "teacherMatrixSupportsBet", readInt(c, "teacherMatrixSupportsBet", 0));
        writeString(d, "teacherOrdinaryBetPolicy", readString(c, "teacherOrdinaryBetPolicy", ""));
        writeString(
                d,
                "teacherMatrixOrdinaryBetPolicy",
                readString(c, "teacherMatrixOrdinaryBetPolicy", ""));
        writeString(d, "teacherFieldSource", readString(c, "teacherFieldSource", ""));
        writeString(d, "teacherFieldVersion", readString(c, "teacherFieldVersion", ""));
        writeString(
                d,
                "teacherNumberStatus",
                readString(c, "teacherNumberStatus", readString(c, "teacherStatus", "")));
        writeString(
                d,
                "teacherStatus",
                readString(c, "teacherStatus", readString(c, "teacherMatrixStatus", "")));

        writeString(
                d,
                "v20Status",
                readString(
                        c,
                        "v20Status",
                        readString(c, "dynamicStatus", readString(c, "dynamicCauseStatus", ""))));
        writeString(
                d, "dynamicStatus", readString(c, "dynamicStatus", readString(c, "v20Status", "")));
        writeString(d, "dynamicCauseStatus", readString(c, "dynamicCauseStatus", ""));
        writeDouble(d, "v20SupportScore", readDouble(c, "v20SupportScore", 0.0d));
        writeDouble(d, "v20RiskScore", readDouble(c, "v20RiskScore", 0.0d));
        writeString(d, "v20WindowStateKey", readString(c, "v20WindowStateKey", ""));
        writeString(
                d,
                "windowStateKey",
                firstReadString(
                        c,
                        new String[] {
                            "windowStateKey",
                            "finalDecisionWindowStateKey",
                            "dynamicWindowStateKey",
                            "probabilityWindowStateKey",
                            "v20WindowStateKey"
                        }));

        writeDouble(d, "coverageQualityScore", readDouble(c, "coverageQualityScore", 0.0d));
        writeDouble(d, "thirdNumberQualityScore", readDouble(c, "thirdNumberQualityScore", 0.0d));
        writeDouble(d, "k3CoverageRisk", readDouble(c, "k3CoverageRisk", 0.0d));
        writeInt(d, "falseHighPFlag", readInt(c, "falseHighPFlag", 0));
        writeInt(d, "probabilityFalseHighPFlag", readInt(c, "probabilityFalseHighPFlag", 0));
        writeInt(d, "probabilityPreferredEdgeBand", readInt(c, "probabilityPreferredEdgeBand", 0));
        writeInt(
                d, "probabilityTeacherWeakOrEmpty", readInt(c, "probabilityTeacherWeakOrEmpty", 0));
        writeInt(d, "probabilityV20UnknownOrEmpty", readInt(c, "probabilityV20UnknownOrEmpty", 0));
        writeInt(d, "probabilityStrongCauseFlag", readInt(c, "probabilityStrongCauseFlag", 0));

        writeInt(d, "dynamicGateFalseHighPFlag", readInt(c, "dynamicGateFalseHighPFlag", 0));
        writeInt(d, "dynamicGatePreferredEdgeBand", readInt(c, "dynamicGatePreferredEdgeBand", 0));
        writeString(d, "dynamicGateEdgePolicy", readString(c, "dynamicGateEdgePolicy", ""));
        writeDouble(d, "dynamicGateCoverageRisk", readDouble(c, "dynamicGateCoverageRisk", 0.0d));
        writeDouble(
                d,
                "dynamicGateCoverageQualityScore",
                readDouble(c, "dynamicGateCoverageQualityScore", 0.0d));
        writeDouble(
                d,
                "dynamicGateThirdNumberQuality",
                readDouble(c, "dynamicGateThirdNumberQuality", 0.0d));
        writeDouble(
                d, "dynamicGateVirtualEdgeRisk", readDouble(c, "dynamicGateVirtualEdgeRisk", 0.0d));
        writeString(d, "dynamicGateVersion", readString(c, "dynamicGateVersion", ""));

        String block = finalFieldChainHardBlockCode(c, null);
        writeString(
                d,
                "finalDecisionVersion",
                finalPreferredReviewBetDomain(c, null)
                        ? V20_DECISION_GATE_PREFERRED_REVIEW_VERSION
                        : V20_FIELD_CHAIN_DECISION_GATE_VERSION);
        writeString(
                d,
                "finalDecisionEdgePolicy",
                readString(
                        c, "dynamicGateEdgePolicy", readString(c, "finalDecisionEdgePolicy", "")));
        writeString(
                d,
                "finalDecisionTeacherPolicy",
                firstReadString(
                        c,
                        new String[] {
                            "teacherOrdinaryBetPolicy",
                            "teacherMatrixOrdinaryBetPolicy",
                            "finalDecisionTeacherPolicy"
                        }));
        writeString(
                d,
                "finalDecisionV20Policy",
                firstReadString(
                        c,
                        new String[] {
                            "v20Status",
                            "dynamicStatus",
                            "dynamicCauseStatus",
                            "finalDecisionV20Policy"
                        }));
        writeString(
                d,
                "finalDecisionBetDomain",
                finalPreferredReviewBetDomain(c, null)
                        ? "WATCH_HIGH_COVER_LOW_RISK_REVIEW_BET"
                        : (fieldChainPreferredDomain(c)
                                ? "P38_40_EDGE1_2_COVER_OK"
                                : (block.length() > 0 ? "FIELD_CHAIN_REJECT" : "NORMAL_REVIEW")));
        writeString(
                d, "finalDecisionRejectCode", finalPreferredReviewBetDomain(c, null) ? "" : block);
        writeString(
                d,
                "finalDecisionShadowReason",
                finalPreferredReviewBetDomain(c, null)
                        ? ""
                        : (block.length() == 0 ? "" : finalBetStrictReason(c, null, block)));
        writeDouble(
                d,
                "finalDecisionCoverageRisk",
                maxReadDouble(
                        c,
                        new String[] {
                            "k3CoverageRisk", "dynamicGateCoverageRisk", "finalDecisionCoverageRisk"
                        }));
        writeDouble(
                d,
                "finalDecisionThirdNumberQuality",
                maxReadDouble(
                        c,
                        new String[] {
                            "thirdNumberQualityScore",
                            "dynamicGateThirdNumberQuality",
                            "finalDecisionThirdNumberQuality"
                        }));
        writeInt(
                d,
                "finalDecisionFalseHighPFlag",
                maxReadInt(
                        c,
                        new String[] {
                            "falseHighPFlag",
                            "dynamicGateFalseHighPFlag",
                            "probabilityFalseHighPFlag",
                            "finalDecisionFalseHighPFlag"
                        }));
    }

    private Models.Decision fromCandidate(Models.ComboCandidate c, String decisionText) {
        Models.Decision d = new Models.Decision();

        d.baseIssue = c.baseIssue == null ? "" : c.baseIssue;
        d.targetIssue = c.targetIssue == null ? "" : c.targetIssue;
        d.createdAt = System.currentTimeMillis();

        d.decision = normalizeDecisionText(decisionText, decisionText);
        d.action = d.decision;

        d.position = c.position;
        d.setNumbers(c.n1, c.n2, c.n3);
        d.k3Mask = c.k3Mask;

        repairDecisionK3(d);

        d.rawP = c.rawP;
        d.calibratedP = c.calibratedP;
        d.finalP = c.finalP;
        d.dynamicGate = c.dynamicGate;
        d.edge = c.edge;

        if (d.edge == 0.0d && d.finalP > 0.0d && d.dynamicGate > 0.0d) {
            d.edge = d.finalP - d.dynamicGate;
        }

        d.modelP = c.modelP;
        d.shapeP = c.shapeP;
        d.comboP = c.comboP;
        d.recoveryP = c.recoveryP;
        d.similarP = c.similarP;
        d.memoryP = c.memoryP;
        d.calibrationError = c.calibrationError;

        d.similarHitRate = c.similarHitRate;
        d.comboHitRate = c.comboHitRate;
        d.effectiveSample = c.effectiveSample;
        d.comboSample = c.comboSample;

        d.trendLife = c.trendLife;
        d.driftRisk = c.driftRisk;
        d.breakDepth = c.breakDepth;
        d.recoveryScore = c.recoveryScore;
        d.championGap = c.championGap;
        d.comboGap = c.comboGap;
        d.hotReverseRisk = c.hotReverseRisk;
        d.falseStrongRisk = c.falseStrongRisk;

        d.freq100 = c.freq100;
        d.freq500 = c.freq500;
        d.freq1000 = c.freq1000;

        d.stateMode = c.stateMode == null ? AppConfig.MODE_CHAOS : c.stateMode;
        d.shapeKey = c.shapeKey == null ? "" : c.shapeKey;
        d.blockCode = c.blockCode == null ? "" : c.blockCode;
        d.reasonSummary = c.reasonSummary == null ? "" : c.reasonSummary;
        d.reasonText = d.reasonSummary;

        d.backtestMode = c.backtestMode == null ? "" : c.backtestMode;
        d.probabilityMode = c.probabilityMode == null ? "" : c.probabilityMode;

        d.deepScore = c.deepScore;
        d.deepConfidence = c.deepConfidence;
        d.deepFeatureP = c.deepFeatureP;
        d.deepPositionScore = c.deepPositionScore;
        d.deepNumberScore = c.deepNumberScore;
        d.deepCycleScore = c.deepCycleScore;
        d.deepTransitionScore = c.deepTransitionScore;
        d.deepStateScore = c.deepStateScore;

        d.biasScore = c.biasScore;
        d.biasConfidence = c.biasConfidence;
        d.profileScore = c.profileScore;
        d.fallbackProfileScore = c.fallbackProfileScore;
        d.auditPenalty = c.auditPenalty;
        d.auditBoost = c.auditBoost;
        d.aiJudgeScore = c.aiJudgeScore;
        d.aiJudgeConfidence = c.aiJudgeConfidence;
        d.evidenceScore = c.evidenceScore;
        d.evidenceRisk = c.evidenceRisk;
        d.conflictLevel = c.conflictLevel;
        d.aPlusScore = c.aPlusScore;
        d.promotionScore = c.promotionScore;
        d.weakestNumberScore = c.weakestNumberScore;
        d.balanceScore = c.balanceScore;

        d.deepProfileKey = c.deepProfileKey == null ? "" : c.deepProfileKey;
        d.deepProfileHitRate = c.deepProfileHitRate;
        d.deepProfileSample = c.deepProfileSample;
        d.deepFallbackLevel = c.deepFallbackLevel;
        d.fallbackProfileKey = c.fallbackProfileKey == null ? "" : c.fallbackProfileKey;
        d.promotionStatus = c.promotionStatus == null ? "" : c.promotionStatus;
        d.aPlusStatus = c.aPlusStatus == null ? "" : c.aPlusStatus;
        d.candidateEvidenceKey = c.candidateEvidenceKey == null ? "" : c.candidateEvidenceKey;

        applyTeacherMatrixToDecision(d, c);
        applyV20FieldsToDecision(d, c);
        applyV20DecisionEvidenceToDecision(d, c);
        applyFieldChainDecisionBridge(d, c);
        applyDynamicCausalFieldsToDecision(d, c);

        d.dataOk = 1;
        d.settled = 0;
        d.result = AppConfig.RESULT_WAIT;

        applyDecisionScope(d);
        d.syncAction();

        writeString(d, "finalDecisionVersion", V20_DYNAMIC_CAUSAL_DECISION_GATE_VERSION);
        writeString(d, "finalDecisionEdgePolicy", readString(c, "dynamicGateEdgePolicy", ""));
        writeString(d, "finalDecisionTeacherPolicy", readString(c, "teacherOrdinaryBetPolicy", ""));
        writeString(
                d,
                "finalDecisionV20Policy",
                readString(c, "v20Status", readString(c, "dynamicStatus", "")));
        String finalRejectForDecision =
                AppConfig.DECISION_BET.equals(d.decision)
                        ? ""
                        : readString(c, "finalDecisionRejectCode", "");
        if (!AppConfig.DECISION_BET.equals(d.decision)
                && (finalRejectForDecision == null || finalRejectForDecision.length() == 0)) {
            finalRejectForDecision = c.blockCode == null ? "" : c.blockCode;
        }
        if (!AppConfig.DECISION_BET.equals(d.decision)
                && (finalRejectForDecision == null || finalRejectForDecision.length() == 0)) {
            finalRejectForDecision = buildStructuredNonBetRejectCode(c);
        }
        writeString(
                d,
                "finalDecisionBetDomain",
                (finalRejectForDecision != null && finalRejectForDecision.indexOf("DYNAMIC") >= 0)
                        ? "DYNAMIC_CAUSAL_REVIEW"
                        : (finalPreferredDomain(c) ? "P38_40_EDGE1_2_COVER_OK" : "NORMAL_REVIEW"));
        writeString(
                d,
                "finalDecisionRejectCode",
                finalRejectForDecision == null ? "" : finalRejectForDecision);
        writeString(
                d,
                "finalDecisionShadowReason",
                (finalRejectForDecision == null || finalRejectForDecision.length() == 0)
                        ? ""
                        : finalBetStrictReason(c, null, finalRejectForDecision));
        writeDouble(
                d,
                "finalDecisionCoverageRisk",
                readDouble(c, "k3CoverageRisk", readDouble(c, "dynamicGateCoverageRisk", 0.0d)));
        writeDouble(
                d,
                "finalDecisionThirdNumberQuality",
                readDouble(
                        c,
                        "thirdNumberQualityScore",
                        readDouble(c, "dynamicGateThirdNumberQuality", 0.0d)));
        writeInt(
                d,
                "finalDecisionFalseHighPFlag",
                readInt(c, "falseHighPFlag", readInt(c, "dynamicGateFalseHighPFlag", 0)));
        d.shadowPromotionDomain = AppConfig.sanitizePromotionDomainKey(d.shadowPromotionDomain);
        writeString(
                d, "decisionGateValidDomainVersion", REALTIME_VALID_DOMAIN_DECISION_GATE_VERSION);
        writeString(d, "decisionGateVersion", V20_DECISION_GATE_ONLY_REAL_BET_VERSION);
        writeString(d, "fieldChainVersion", V20_DECISION_GATE_CLOSED_LOOP_FIX_VERSION);
        writeString(
                d,
                "finalDecisionStatus",
                readString(
                        c,
                        "finalDecisionStatus",
                        AppConfig.DECISION_BET.equals(d.decision) ? "PASS" : "WATCH"));
        writeString(
                d,
                "finalDecisionActionAdvice",
                readString(
                        c,
                        "finalDecisionActionAdvice",
                        AppConfig.DECISION_BET.equals(d.decision)
                                ? "PASS_TO_REAL_BET_BY_DECISION_GATE"
                                : "SHADOW_OR_HOLD_BY_DECISION_GATE"));
        writeDouble(
                d, "finalDecisionQualityScore", readDouble(c, "finalDecisionQualityScore", 0.0d));
        writeDouble(d, "finalDecisionRiskScore", readDouble(c, "finalDecisionRiskScore", 0.0d));
        writeInt(d, "realProbeAllowed", readInt(c, "realProbeAllowed", 0));
        copyLinkModificationV2DecisionFields(d, c);
        return d;
    }

    private String buildStructuredNonBetRejectCode(Models.ComboCandidate c) {
        if (c == null) return "FINAL_NO_CANDIDATE";
        String freshness = safeTextLocal(c.snapshotFreshness).toUpperCase(java.util.Locale.ROOT);
        if (freshness.indexOf("HARD_STALE") >= 0) return "FINAL_SNAPSHOT_HARD_STALE";
        double coverage = Math.max(c.coverageQuality, c.coverageQualityScore);
        if (c.coverageAvailable == 0 || coverage < AppConfig.REAL_BET_PROBE_MIN_COVERAGE)
            return "FINAL_COVERAGE_NOT_QUALIFIED";
        double third = Math.max(c.thirdQuality, c.thirdNumberQualityScore);
        if (c.thirdQualityAvailable == 0 || third < AppConfig.REAL_BET_PROBE_MIN_THIRD)
            return "FINAL_THIRD_NOT_QUALIFIED";
        if (c.counterRisk > AppConfig.REAL_BET_PROBE_MAX_COUNTER_RISK)
            return "FINAL_COUNTER_RISK_HIGH";
        String adaptive = safeTextLocal(c.adaptiveStatus).toUpperCase(java.util.Locale.ROOT);
        if (adaptive.indexOf("FREEZE") >= 0 || adaptive.indexOf("REJECT") >= 0)
            return "FINAL_ADAPTIVE_HARD_RISK";
        String rule = safeTextLocal(c.ruleHealthStatus).toUpperCase(java.util.Locale.ROOT);
        if (rule.indexOf("HARD_RISK") >= 0 || rule.indexOf("FREEZE") >= 0)
            return "FINAL_RULE_HEALTH_HARD_RISK";
        String domain = AppConfig.sanitizePromotionDomainKey(c.shadowPromotionDomain);
        if (domain.length() == 0) return "FINAL_PROMOTION_DOMAIN_MISSING";
        String promotion = safeTextLocal(c.shadowPromotionStatus).toUpperCase(java.util.Locale.ROOT);
        if (!(promotion.indexOf("PROMOTE_WATCH") >= 0
                || promotion.indexOf("READY") >= 0
                || promotion.indexOf("STRONG") >= 0)) return "FINAL_PROMOTION_NOT_MATURE";
        double gate = c.riskAdjustedGate > 0.0d ? c.riskAdjustedGate : c.dynamicGate;
        if (c.finalP < AppConfig.V42_PROBE_MIN_P) return "FINAL_P_BELOW_ABSOLUTE_FLOOR";
        if (c.finalP - gate < AppConfig.REAL_BET_PROBE_MAX_NEG_EDGE)
            return "FINAL_EDGE_BELOW_PROBE_FLOOR";
        if (c.qualifiedProbe != 1) return "FINAL_QUALIFIED_PROBE_FALSE";
        if (c.realProbeAllowed != 1) return "FINAL_PROBE_BUDGET_OR_POLICY_REJECT";
        return "FINAL_GATE_NOT_PASSED";
    }

    private void applyTeacherMatrixToDecision(Models.Decision d, Models.ComboCandidate c) {
        if (d == null || c == null) {
            return;
        }

        int sample =
                readInt(
                        c,
                        "teacherMatrixSample",
                        readInt(c, "teacherNumberSample", readInt(c, "teacherSample", 0)));
        int comboSample = readInt(c, "teacherMatrixComboSample", sample);
        double hitRate =
                readDouble(
                        c,
                        "teacherMatrixHitRate",
                        readDouble(
                                c, "teacherNumberHitRate", readDouble(c, "teacherHitRate", 0.0d)));
        double recentRate =
                readDouble(
                        c,
                        "teacherMatrixRecentHitRate",
                        readDouble(c, "teacherMatrixComboHitRate", hitRate));
        double conf =
                readDouble(
                        c,
                        "teacherMatrixConfidence",
                        readDouble(
                                c,
                                "teacherNumberConfidence",
                                readDouble(c, "teacherConfidence", 0.0d)));
        double score =
                readDouble(
                        c,
                        "teacherMatrixScore",
                        readDouble(c, "teacherNumberScore", readDouble(c, "teacherScore", 0.0d)));
        double risk =
                readDouble(
                        c,
                        "teacherMatrixRisk",
                        readDouble(c, "teacherNumberRisk", readDouble(c, "teacherRisk", 0.0d)));
        double riskPenalty = readDouble(c, "teacherMatrixRiskPenalty", risk);
        String status =
                readString(
                        c,
                        "teacherMatrixStatus",
                        readString(c, "teacherNumberStatus", readString(c, "teacherStatus", "")));
        String summary =
                readString(
                        c,
                        "teacherMatrixSummary",
                        readString(c, "teacherNumberSummary", readString(c, "teacherSummary", "")));

        writeInt(d, "teacherMatrixSample", sample);
        writeInt(d, "teacherMatrixPositive", readInt(c, "teacherMatrixPositive", 0));
        writeInt(d, "teacherMatrixNegative", readInt(c, "teacherMatrixNegative", 0));
        writeInt(d, "teacherMatrixStateSample", readInt(c, "teacherMatrixStateSample", 0));
        writeInt(d, "teacherMatrixComboSample", comboSample);
        writeInt(
                d,
                "teacherMatrixApplied",
                sample > 0 || conf > 0.0d ? 1 : readInt(c, "teacherMatrixApplied", 0));

        writeDouble(d, "teacherMatrixHitRate", hitRate);
        writeDouble(d, "teacherMatrixRecentHitRate", recentRate);
        writeDouble(d, "teacherMatrixConfidence", conf);
        writeDouble(
                d,
                "teacherMatrixProbAdjust",
                readDouble(
                        c,
                        "teacherMatrixProbAdjust",
                        readDouble(c, "teacherNumberProbAdjust", 0.0d)));
        writeDouble(
                d,
                "teacherMatrixGateAdjust",
                readDouble(
                        c,
                        "teacherMatrixGateAdjust",
                        readDouble(c, "teacherNumberGateAdjust", 0.0d)));
        writeDouble(d, "teacherMatrixRiskPenalty", riskPenalty);
        writeDouble(
                d, "teacherMatrixPositionScore", readDouble(c, "teacherMatrixPositionScore", 0.0d));
        writeDouble(
                d, "teacherMatrixNumberScore", readDouble(c, "teacherMatrixNumberScore", score));
        writeDouble(d, "teacherMatrixComboScore", readDouble(c, "teacherMatrixComboScore", score));
        writeDouble(d, "teacherMatrixStateScore", readDouble(c, "teacherMatrixStateScore", 0.0d));
        writeDouble(d, "teacherMatrixScore", score);
        writeDouble(d, "teacherMatrixRisk", risk);

        writeString(d, "teacherMatrixStatus", status);
        writeString(d, "teacherMatrixProfileKey", readString(c, "teacherMatrixProfileKey", ""));
        writeString(d, "teacherMatrixComboKey", readString(c, "teacherMatrixComboKey", ""));
        writeString(d, "teacherMatrixSummary", summary);

        writeInt(d, "teacherNumberSample", sample);
        writeDouble(d, "teacherNumberHitRate", hitRate);
        writeDouble(d, "teacherNumberScore", score);
        writeDouble(d, "teacherNumberRisk", risk);
        writeDouble(d, "teacherNumberConfidence", conf);
        writeString(d, "teacherNumberStatus", status);
        writeString(d, "teacherNumberSummary", summary);
    }

    private void applyDecisionScope(Models.Decision d) {
        if (d == null) {
            return;
        }

        d.decision = normalizeDecisionText(d.decision, d.action);
        d.action = d.decision;

        if (AppConfig.DECISION_BET.equals(d.decision)) {
            d.statScope = AppConfig.STAT_SCOPE_REAL_TRADE;
            d.realTrade = 1;
        } else if (AppConfig.DECISION_SHADOW.equals(d.decision)) {
            d.statScope = AppConfig.STAT_SCOPE_SHADOW;
            d.realTrade = 0;
        } else {
            d.statScope = AppConfig.STAT_SCOPE_HOLD;
            d.realTrade = 0;
        }
    }

    private String buildBetReason(
            Models.ComboCandidate c,
            TrendStateMachine.TrendTrace trace,
            BreakRecoveryEngine.RecoveryResult recovery,
            Models.SimilarResult similar,
            ShapeMemoryEngine.MemorySignal memory,
            CalibrationEngine.CalibrationSignal calibration,
            DecisionContext ctx) {

        StringBuilder sb = new StringBuilder();

        sb.append("BET通过：当前目标期=");
        sb.append(c.targetIssue == null ? "" : c.targetIssue);
        sb.append("，单位置P");
        sb.append(c.position);
        sb.append("，号码");
        sb.append(UiFormatter.numberGroup(c.n1, c.n2, c.n3));
        sb.append("。");
        sb.append(buildEvidence(c, trace, recovery, similar, memory, calibration, ctx));

        return sb.toString();
    }

    private String buildHoldReason(
            Models.ComboCandidate c,
            TrendStateMachine.TrendTrace trace,
            BreakRecoveryEngine.RecoveryResult recovery,
            Models.SimilarResult similar,
            ShapeMemoryEngine.MemorySignal memory,
            CalibrationEngine.CalibrationSignal calibration,
            String blockCode,
            DecisionContext ctx) {

        StringBuilder sb = new StringBuilder();

        sb.append(
                UiFormatter.blockReasonText(
                        blockCode == null || blockCode.length() == 0
                                ? AppConfig.BLOCK_LOW_PROBABILITY
                                : blockCode));
        sb.append("。");
        sb.append(buildEvidence(c, trace, recovery, similar, memory, calibration, ctx));

        return sb.toString();
    }

    private String buildEvidence(
            Models.ComboCandidate c,
            TrendStateMachine.TrendTrace trace,
            BreakRecoveryEngine.RecoveryResult recovery,
            Models.SimilarResult similar,
            ShapeMemoryEngine.MemorySignal memory,
            CalibrationEngine.CalibrationSignal calibration,
            DecisionContext ctx) {

        StringBuilder sb = new StringBuilder();

        if (c != null) {
            sb.append("Base=");
            sb.append(c.baseIssue == null ? "" : c.baseIssue);
            sb.append("，Target=");
            sb.append(c.targetIssue == null ? "" : c.targetIssue);
            sb.append("，RawP=");
            sb.append(UiFormatter.probability(c.rawP));
            sb.append("，FinalP=");
            sb.append(UiFormatter.probability(c.finalP));
            sb.append("，Gate=");
            sb.append(UiFormatter.probability(c.dynamicGate));
            sb.append("，Edge=");
            sb.append(UiFormatter.signedProbability(c.edge));
            sb.append("，状态=");
            sb.append(c.stateMode == null ? "" : c.stateMode);
            sb.append("，TL=");
            sb.append(UiFormatter.score(c.trendLife));
            sb.append("，DR=");
            sb.append(UiFormatter.score(c.driftRisk));
            sb.append("，BD=");
            sb.append(UiFormatter.score(c.breakDepth));
            sb.append("，Recovery=");
            sb.append(UiFormatter.score(c.recoveryScore));
            sb.append("，ChampionGap=");
            sb.append(UiFormatter.probability(c.championGap));
            sb.append("，ComboGap=");
            sb.append(UiFormatter.probability(c.comboGap));
        }

        if (trace != null) {
            sb.append("；趋势=");
            sb.append(trace.transitionCode);
            sb.append("，连续=");
            sb.append(trace.sameModeCount);
            sb.append("，切换风险=");
            sb.append(UiFormatter.score(trace.modeSwitchRisk));
            sb.append("，早破风险=");
            sb.append(UiFormatter.score(trace.earlyBreakRisk));
        }

        if (recovery != null) {
            sb.append("；断点=");
            sb.append(recovery.breakType);
            sb.append("，恢复=");
            sb.append(recovery.recoveryType);
            sb.append("，恢复风险=");
            sb.append(UiFormatter.score(recovery.falseRecoveryRisk));
        }

        if (similar != null) {
            sb.append("；相似样本=");
            sb.append(similar.effectiveSample);
            sb.append("，相似率=");
            sb.append(UiFormatter.probability(similar.similarHitRate));
        }

        if (memory != null) {
            sb.append("；记忆=");
            sb.append(UiFormatter.score(memory.memoryScore));
            sb.append("，记忆率=");
            sb.append(UiFormatter.probability(memory.recentMemoryRate));
        }

        if (calibration != null) {
            sb.append("；校准误差=");
            sb.append(UiFormatter.probability(calibration.error));
        }

        if (ctx != null) {
            sb.append("；状态指纹=");
            sb.append(ctx.stateKey);
            sb.append("；号码形态=");
            sb.append(ctx.numberShape);

            if (ctx.actualPredictionLogic != null && ctx.actualPredictionLogic.length() > 0) {
                sb.append("；预测逻辑=");
                sb.append(ctx.actualPredictionLogic);
            }

            sb.append("；学习判断=");

            if (ctx.learningStrong) {
                sb.append("画像强，可作为共同证据参与最终门");
            } else if (ctx.learningWeak) {
                sb.append("画像弱");
            } else {
                sb.append("画像中性");
            }

            sb.append("，证据=");
            sb.append(ctx.evidenceStrong ? "强" : "普通");
            sb.append("，风险=");
            sb.append(ctx.realRiskHigh ? "高" : "可控");

            sb.append("；");
            sb.append(buildTeacherMatrixReason(ctx));

            sb.append("；");
            sb.append(buildV20Reason(ctx));

            sb.append("；");
            sb.append(buildDecisionEvidenceReason(ctx));

            sb.append("；");
            sb.append(buildDynamicCausalReason(ctx));

            if (ctx.learningAdviceText != null && ctx.learningAdviceText.length() > 0) {
                sb.append("；上次学习建议=");
                sb.append(ctx.learningAdviceText);
            } else if (ctx.nextAdvice != null && ctx.nextAdvice.length() > 0) {
                sb.append("；本次修正建议=");
                sb.append(ctx.nextAdvice);
            }
        }

        return sb.toString();
    }

    private String buildTeacherMatrixReason(DecisionContext ctx) {
        if (ctx == null || ctx.teacherMatrixApplied == 0) {
            return "Teacher Matrix=暂无匹配画像或样本不足；按老版规则视为中性，不加分、不降门、不拦截";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("Teacher Matrix=");
        sb.append(ctx.teacherMatrixStatus == null ? "" : ctx.teacherMatrixStatus);
        sb.append("，样本=");
        sb.append(ctx.teacherMatrixSample);
        sb.append("，组合样本=");
        sb.append(ctx.teacherMatrixComboSample);
        sb.append("，命中率=");
        sb.append(UiFormatter.probability(ctx.teacherMatrixHitRate));
        sb.append("，组合率=");
        sb.append(UiFormatter.probability(ctx.teacherMatrixComboHitRate));
        sb.append("，置信=");
        sb.append(UiFormatter.probability(ctx.teacherMatrixConfidence));
        sb.append("，风险=");
        sb.append(UiFormatter.probability(ctx.teacherMatrixRiskPenalty));
        sb.append("，门槛修正=");
        sb.append(UiFormatter.signedProbability(ctx.teacherMatrixGateAdjust));

        if (ctx.teacherMatrixStrong) {
            sb.append("，结论=矩阵强支持，可小幅参与降门/加证据");
        } else if (ctx.teacherMatrixReady) {
            sb.append("，结论=矩阵可用，可参与证据确认");
        } else if (ctx.teacherMatrixRisk) {
            sb.append("，结论=矩阵风险偏高，小幅抬门");
        } else {
            sb.append("，结论=矩阵中性观察");
        }

        if (ctx.teacherMatrixSummary != null && ctx.teacherMatrixSummary.length() > 0) {
            sb.append("，摘要=");
            sb.append(shortText(ctx.teacherMatrixSummary, 180));
        }

        return sb.toString();
    }

    private void fillFinalDiagnosisContext(
            DecisionContext ctx, Models.ComboCandidate c, GateRuntimeContext runtime) {
        if (ctx == null || c == null) {
            return;
        }

        ctx.finalPBeforeDiagnosis = readDouble(c, "finalPBeforeDiagnosis", c.finalP);
        ctx.finalPAfterDiagnosis =
                readDouble(
                        c, "finalPAfterDiagnosis", readDouble(c, "diagnosisAdjustedP", c.finalP));
        ctx.diagnosisAdjustedP = readDouble(c, "diagnosisAdjustedP", ctx.finalPAfterDiagnosis);
        ctx.moduleAdjustedP = readDouble(c, "moduleAdjustedP", 0.0d);
        ctx.freezeAdjustedP = readDouble(c, "freezeAdjustedP", 0.0d);
        ctx.shadowPromotionBoost = readDouble(c, "shadowPromotionBoost", 0.0d);
        ctx.probabilityEvidenceContribution =
                readDouble(c, "probabilityEvidenceContribution", 0.0d);
        ctx.probabilityCounterEvidence =
                readDouble(
                        c,
                        "probabilityCounterEvidence",
                        readDouble(c, "probabilityCounterContribution", 0.0d));
        ctx.probabilitySampleReliability = readDouble(c, "probabilitySampleReliability", 0.0d);
        ctx.probabilityRecentValidity = readDouble(c, "probabilityRecentValidity", 0.0d);
        ctx.probabilityActionAdvice = readString(c, "probabilityActionAdvice", "");
        ctx.probabilityDiagnosisSummary = readString(c, "probabilityDiagnosisSummary", "");

        ctx.qualityGateAdjust = readDouble(c, "qualityGateAdjust", 0.0d);
        ctx.diagnosisGateAdjust = readDouble(c, "diagnosisGateAdjust", 0.0d);
        ctx.moduleEffectGateAdjust = readDouble(c, "moduleEffectGateAdjust", 0.0d);
        ctx.freezeGateAdjust = readDouble(c, "freezeGateAdjust", 0.0d);
        ctx.shadowPromotionGateAdjust = readDouble(c, "shadowPromotionGateAdjust", 0.0d);
        ctx.dynamicGateQualitySummary = readString(c, "dynamicGateQualitySummary", "");
        ctx.dynamicGateFrequencyPolicy = readString(c, "dynamicGateFrequencyPolicy", "");
        ctx.dynamicGateActionAdvice =
                readString(
                        c,
                        "dynamicGateActionAdvice",
                        readString(c, "dynamicGateDecisionAdvice", ""));

        if (runtime != null) {
            ctx.moduleEffectStatus =
                    runtime.moduleEffectStatus == null ? "" : runtime.moduleEffectStatus;
            ctx.moduleEffectSummary =
                    runtime.moduleEffectSummary == null ? "" : runtime.moduleEffectSummary;
            ctx.shadowPromotionStatus =
                    runtime.shadowPromotionStatus == null ? "" : runtime.shadowPromotionStatus;
            ctx.shadowPromotionSummary =
                    runtime.shadowPromotionSummary == null ? "" : runtime.shadowPromotionSummary;
            ctx.dynamicFreezeStatus =
                    runtime.dynamicFreezeStatus == null ? "" : runtime.dynamicFreezeStatus;
            ctx.dynamicFreezeSummary =
                    runtime.dynamicFreezeSummary == null ? "" : runtime.dynamicFreezeSummary;
            ctx.adaptiveJudgeStatus =
                    runtime.adaptiveJudgeStatus == null ? "" : runtime.adaptiveJudgeStatus;
            ctx.adaptiveJudgeSummary =
                    runtime.adaptiveJudgeSummary == null ? "" : runtime.adaptiveJudgeSummary;
            ctx.diagnosisClosedLoopSummary =
                    runtime.diagnosisClosedLoopSummary == null
                            ? ""
                            : runtime.diagnosisClosedLoopSummary;
        }

        ctx.moduleMisleading =
                containsBadWord(ctx.moduleEffectStatus) || containsBadWord(ctx.moduleEffectSummary);
        ctx.shadowPromotionStrong =
                containsGoodWord(ctx.shadowPromotionStatus)
                        || containsGoodWord(ctx.shadowPromotionSummary)
                        || ctx.shadowPromotionBoost > 0.0d
                        || ctx.shadowPromotionGateAdjust < 0.0d;
        ctx.freezeMatrixRisk =
                containsFreezeWord(ctx.dynamicFreezeStatus)
                        || containsFreezeWord(ctx.dynamicFreezeSummary)
                        || ctx.freezeGateAdjust >= 0.010d
                        || ctx.freezeAdjustedP <= -0.012d;
        ctx.adaptiveWatchNeutral = isAdaptiveWatchNeutralForFinal(c, ctx);
        ctx.adaptiveFreeze =
                !ctx.adaptiveWatchNeutral
                        && (containsFreezeWord(ctx.adaptiveJudgeStatus)
                                || containsFreezeWord(ctx.adaptiveJudgeSummary));
        ctx.adaptiveReject =
                !ctx.adaptiveWatchNeutral
                        && (containsRejectWord(ctx.adaptiveJudgeStatus)
                                || containsRejectWord(ctx.adaptiveJudgeSummary));
        ctx.adaptivePass =
                containsPassWord(ctx.adaptiveJudgeStatus)
                        || containsPassWord(ctx.adaptiveJudgeSummary);
        if (ctx.adaptiveCandidatePass) ctx.adaptivePass = true;
        if (ctx.adaptiveCandidateFreeze) ctx.adaptiveFreeze = true;
        if (ctx.dynamicCausalHardRisk || ctx.dynamicCausalFreeze) ctx.freezeMatrixRisk = true;
        if (ctx.dynamicCausalRiskHigh) ctx.moduleMisleading = true;

        ctx.finalDiagnosisRiskHigh = false;
        if (ctx.probabilityCounterEvidence >= FINAL_DIAG_COUNTER_RISK
                && ctx.probabilityEvidenceContribution < DECISION_EVIDENCE_STRONG) {
            ctx.finalDiagnosisRiskHigh = true;
        }
        if (ctx.probabilitySampleReliability > 0.0d
                && ctx.probabilitySampleReliability < FINAL_DIAG_SAMPLE_MIN
                && c.finalP < 0.410d) {
            ctx.finalDiagnosisRiskHigh = true;
        }
        if (ctx.probabilityRecentValidity > 0.0d
                && ctx.probabilityRecentValidity < FINAL_DIAG_RECENT_MIN
                && c.finalP < 0.410d) {
            ctx.finalDiagnosisRiskHigh = true;
        }
        if (ctx.moduleMisleading
                || ctx.freezeMatrixRisk
                || ctx.adaptiveFreeze
                || ctx.adaptiveReject) {
            ctx.finalDiagnosisRiskHigh = true;
        }

        ctx.finalDecisionQualityScore = computeFinalDecisionQuality(c, ctx);
        ctx.finalDecisionRiskScore = computeFinalDecisionRisk(c, ctx);
        ctx.finalDecisionStrong =
                ctx.finalDecisionQualityScore >= FINAL_DECISION_STRONG_QUALITY
                        && ctx.finalDecisionRiskScore < FINAL_DECISION_RISK_LINE;
        ctx.finalDecisionReady =
                ctx.finalDecisionQualityScore >= FINAL_DECISION_MIN_QUALITY
                        && ctx.finalDecisionRiskScore < FINAL_DECISION_RISK_LINE;
        if (!ctx.finalDecisionReady
                && ctx.shadowPromotionStrong
                && c.finalP >= 0.380d
                && c.edge >= 0.010d
                && ctx.finalDecisionRiskScore < FINAL_DECISION_RISK_LINE
                && !ctx.freezeMatrixRisk
                && !ctx.adaptiveFreeze
                && !ctx.adaptiveReject
                && !ctx.teacherMatrixFreeze) {
            ctx.finalDecisionReady = true;
        }
        ctx.finalDecisionFreeze =
                ctx.freezeMatrixRisk
                        || ctx.adaptiveFreeze
                        || ctx.teacherMatrixFreeze
                        || ctx.falseStrongHardFreeze > 0
                        || ctx.falseStrongFreezeRisk >= FINAL_FREEZE_RISK_LINE
                        || ctx.dynamicCausalFreeze;
        ctx.finalDecisionHardRisk =
                ctx.finalDecisionFreeze
                        || ctx.adaptiveReject
                        || ctx.finalDecisionRiskScore >= FINAL_DECISION_HARD_RISK_LINE;
        ctx.finalDecisionActionAdvice = buildFinalDecisionActionAdvice(ctx);
        ctx.finalDecisionStatus =
                ctx.finalDecisionHardRisk
                        ? "FREEZE"
                        : (ctx.finalDecisionStrong
                                ? "PASS"
                                : (ctx.finalDecisionReady ? "WATCH" : "HOLD"));
    }

    private void mergeFinalDiagnosisWithDecisionEvidence(
            DecisionContext ctx, Models.ComboCandidate c, GateRuntimeContext runtime) {
        if (ctx == null) {
            return;
        }

        if (ctx.probabilityEvidenceContribution > 0.0d) {
            ctx.decisionEvidenceContribution =
                    Math.max(ctx.decisionEvidenceContribution, ctx.probabilityEvidenceContribution);
        }
        if (ctx.probabilityCounterEvidence > 0.0d) {
            ctx.decisionCounterContribution =
                    Math.max(ctx.decisionCounterContribution, ctx.probabilityCounterEvidence);
        }
        if (ctx.probabilitySampleReliability > 0.0d) {
            ctx.decisionSampleReliability =
                    Math.max(ctx.decisionSampleReliability, ctx.probabilitySampleReliability);
        }
        if (ctx.probabilityRecentValidity > 0.0d) {
            ctx.decisionRecentValidity =
                    Math.max(ctx.decisionRecentValidity, ctx.probabilityRecentValidity);
        }

        if (ctx.finalDecisionStrong) {
            ctx.decisionEvidenceStrong = true;
        }
        if (ctx.finalDecisionReady) {
            ctx.decisionEvidenceReady = true;
        }
        if (ctx.finalDecisionQualityScore >= DECISION_EVIDENCE_ELITE
                && ctx.finalDecisionRiskScore < FINAL_DECISION_RISK_LINE) {
            ctx.decisionEvidenceElite = true;
        }
        if (ctx.finalDiagnosisRiskHigh || ctx.finalDecisionRiskScore >= FINAL_DECISION_RISK_LINE) {
            ctx.decisionCounterRiskHigh = true;
        }
        if (ctx.finalDecisionHardRisk) {
            ctx.decisionHardRisk = true;
        }
    }

    private double computeFinalDecisionQuality(Models.ComboCandidate c, DecisionContext ctx) {
        if (c == null || ctx == null) {
            return 0.0d;
        }
        double q = 0.0d;
        q += clamp01((c.finalP - STRONG_MIN_P) * 3.0d) * 0.18d;
        q += clamp01((c.rawP - RAW_FLOOR_STRONG) * 4.2d) * 0.10d;
        q += clamp01((c.edge + 0.006d) * 20.0d) * 0.10d;
        q += ctx.probabilityEvidenceContribution * 0.12d;
        q += ctx.teacherEvidenceContribution * 0.10d;
        q += ctx.v20SupportScore * 0.12d;
        q += ctx.decisionEvidenceContribution * 0.12d;
        q +=
                maxValue(
                                ctx.probabilitySampleReliability,
                                ctx.teacherSampleReliability,
                                ctx.v20SampleReliability,
                                ctx.decisionSampleReliability)
                        * 0.08d;
        q +=
                maxValue(
                                ctx.probabilityRecentValidity,
                                ctx.teacherRecentValidity,
                                ctx.v20RecentValidity,
                                ctx.decisionRecentValidity)
                        * 0.08d;
        q += ctx.dynamicCausalQualityScore * 0.14d;
        q += (ctx.dynamicCausalStrong ? 0.08d : (ctx.dynamicCausalReady ? 0.04d : 0.0d));
        q += (ctx.shadowPromotionStrong ? 0.06d : 0.0d);
        q += (ctx.adaptivePass ? 0.04d : 0.0d);
        return clamp01(q);
    }

    private double computeFinalDecisionRisk(Models.ComboCandidate c, DecisionContext ctx) {
        if (c == null || ctx == null) {
            return 1.0d;
        }
        double r = 0.0d;
        r += clamp01(c.evidenceRisk) * 0.10d;
        r += ctx.probabilityCounterEvidence * 0.14d;
        r += ctx.teacherCounterContribution * 0.10d;
        r += ctx.v20RiskScore * 0.12d;
        r += ctx.decisionCounterContribution * 0.14d;
        r += ctx.falseStrongFreezeRisk * 0.12d;
        r += ctx.freezeMatrixRisk ? 0.12d : 0.0d;
        r += ctx.moduleMisleading ? 0.08d : 0.0d;
        r += (ctx.adaptiveFreeze || ctx.adaptiveReject) ? 0.12d : 0.0d;
        r += ctx.teacherMatrixFreeze ? 0.10d : 0.0d;
        r += ctx.v20HardRisk ? 0.12d : 0.0d;
        r += ctx.dynamicCausalRiskScore * 0.16d;
        r += ctx.dynamicCausalHardRisk ? 0.18d : 0.0d;
        r += ctx.dynamicCausalFreeze ? 0.16d : 0.0d;
        return clamp01(r);
    }

    private String buildFinalDecisionActionAdvice(DecisionContext ctx) {
        if (ctx == null) {
            return "HOLD：最终审判为空";
        }
        if (ctx.dynamicCausalHardRisk || ctx.dynamicCausalFreeze) {
            return "FREEZE：动态窗口簇/NeedSupply/反事实/晋级域判定硬风险，只能SHADOW复核";
        }
        if (ctx.finalDecisionFreeze || ctx.adaptiveFreeze) {
            return "FREEZE：冻结矩阵/智能审判判定假强，只能SHADOW复核";
        }
        if (ctx.adaptiveReject) {
            return "REJECT：智能审判拒绝真实BET";
        }
        if (ctx.finalDiagnosisRiskHigh) {
            return "DOWNGRADE_TO_SHADOW：概率诊断/模块/冻结反证偏高";
        }
        if (ctx.dynamicCausalStrong && ctx.finalDecisionStrong) {
            return "PASS_TO_BET_GATE：动态因果强支持且最终质量强，允许真实BET最终确认";
        }
        if (ctx.finalDecisionStrong) {
            return "PASS_TO_BET_GATE：最终质量强，允许进入真实BET最终确认";
        }
        if (ctx.finalDecisionReady) {
            return "WATCH_TO_GATE：质量可用但仍需最终门确认";
        }
        return "HOLD：最终质量不足";
    }

    private void refreshFinalDecisionContext(DecisionContext ctx, Models.ComboCandidate c) {
        if (ctx == null || c == null) {
            return;
        }
        ctx.finalDecisionQualityScore = computeFinalDecisionQuality(c, ctx);
        ctx.finalDecisionRiskScore = computeFinalDecisionRisk(c, ctx);
        ctx.finalDecisionStrong =
                ctx.finalDecisionQualityScore >= FINAL_DECISION_STRONG_QUALITY
                        && ctx.finalDecisionRiskScore < FINAL_DECISION_RISK_LINE;
        ctx.finalDecisionReady =
                ctx.finalDecisionQualityScore >= FINAL_DECISION_MIN_QUALITY
                        && ctx.finalDecisionRiskScore < FINAL_DECISION_RISK_LINE;
        if (ctx.teacherNoSampleNeutral || ctx.v20WatchNeutral || ctx.adaptiveWatchNeutral) {
            ctx.finalDiagnosisRiskHigh = false;
        }
        ctx.finalDecisionFreeze =
                ctx.freezeMatrixRisk
                        || ctx.adaptiveFreeze
                        || ctx.teacherMatrixFreeze
                        || ctx.falseStrongHardFreeze > 0
                        || ctx.falseStrongFreezeRisk >= FINAL_FREEZE_RISK_LINE
                        || ctx.dynamicCausalFreeze;
        ctx.finalDecisionHardRisk =
                ctx.finalDecisionFreeze
                        || ctx.adaptiveReject
                        || ctx.finalDecisionRiskScore >= FINAL_DECISION_HARD_RISK_LINE;
        ctx.finalDecisionActionAdvice = buildFinalDecisionActionAdvice(ctx);
        ctx.finalDecisionStatus =
                ctx.finalDecisionHardRisk
                        ? "FREEZE"
                        : (ctx.finalDecisionStrong
                                ? "PASS"
                                : (ctx.finalDecisionReady ? "WATCH" : "HOLD"));
    }

    private void writeFinalDecisionAuditFields(Models.ComboCandidate c, DecisionContext ctx) {
        if (c == null || ctx == null) {
            return;
        }
        writeString(c, "finalDecisionStatus", ctx.finalDecisionStatus);
        writeString(c, "finalDecisionActionAdvice", ctx.finalDecisionActionAdvice);
        writeDouble(c, "finalDecisionQualityScore", ctx.finalDecisionQualityScore);
        writeDouble(c, "finalDecisionRiskScore", ctx.finalDecisionRiskScore);
        writeInt(c, "finalDecisionHardRisk", ctx.finalDecisionHardRisk ? 1 : 0);
        writeInt(c, "finalDecisionFreezeFlag", ctx.finalDecisionFreeze ? 1 : 0);
        writeDouble(c, "finalDecisionEvidenceContribution", ctx.decisionEvidenceContribution);
        writeDouble(c, "finalDecisionCounterContribution", ctx.decisionCounterContribution);
        writeDouble(c, "finalDecisionSampleReliability", ctx.decisionSampleReliability);
        writeDouble(c, "finalDecisionRecentValidity", ctx.decisionRecentValidity);
        writeString(c, "finalDecisionSummary", buildFinalDecisionReason(ctx));
        writeString(c, "finalDecisionVersion", V20_DECISION_GATE_MAIN_CHAIN_FIX_VERSION);
        writeString(c, "decisionGateVersion", V20_DECISION_GATE_ONLY_REAL_BET_VERSION);
        writeString(c, "fieldChainVersion", V20_DECISION_GATE_CLOSED_LOOP_FIX_VERSION);
        writeInt(
                c,
                "teacherNoSampleNeutralFlag",
                ctx.teacherNoSampleNeutral ? 1 : readInt(c, "teacherNoSampleNeutralFlag", 0));
        writeInt(
                c,
                "v20WatchNeutralFlag",
                ctx.v20WatchNeutral ? 1 : readInt(c, "v20WatchNeutralFlag", 0));
        writeInt(
                c,
                "adaptiveWatchNeutralFlag",
                ctx.adaptiveWatchNeutral ? 1 : readInt(c, "adaptiveWatchNeutralFlag", 0));
        writeInt(c, "realProbeAllowed", realBetProbeAllowedByConfig(c, ctx) ? 1 : 0);
        if (readString(c, "finalDecisionRejectCode", "").length() == 0
                && !ctx.finalDecisionHardRisk) {
            writeString(c, "finalDecisionRejectCode", FINAL_REJECT_OK);
        }
        writeDynamicCausalAuditFields(c, ctx);
    }

    private String buildFinalDecisionReason(DecisionContext ctx) {
        if (ctx == null) {
            return "最终审判=空";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("最终审判=").append(ctx.finalDecisionStatus);
        sb.append("，质量=").append(UiFormatter.probability(ctx.finalDecisionQualityScore));
        sb.append("，风险=").append(UiFormatter.probability(ctx.finalDecisionRiskScore));
        sb.append("，概率证据/反证=")
                .append(UiFormatter.probability(ctx.probabilityEvidenceContribution))
                .append("/")
                .append(UiFormatter.probability(ctx.probabilityCounterEvidence));
        sb.append("，教师=")
                .append(ctx.teacherMatrixStatus)
                .append(" ")
                .append(UiFormatter.probability(ctx.teacherEvidenceContribution))
                .append("/")
                .append(UiFormatter.probability(ctx.teacherCounterContribution));
        sb.append("，V20=")
                .append(UiFormatter.probability(ctx.v20SupportScore))
                .append("/")
                .append(UiFormatter.probability(ctx.v20RiskScore));
        sb.append("，动态因果=FULL_LOOP|")
                .append(
                        ctx.dynamicCausalStrong
                                ? "STRONG"
                                : (ctx.dynamicCausalReady
                                        ? "READY"
                                        : (ctx.dynamicCausalFreeze ? "FREEZE" : "WATCH")))
                .append(" ")
                .append(UiFormatter.probability(ctx.dynamicCausalQualityScore))
                .append("/")
                .append(UiFormatter.probability(ctx.dynamicCausalRiskScore));
        if (ctx.moduleMisleading) {
            sb.append("，模块误导");
        }
        if (ctx.freezeMatrixRisk) {
            sb.append("，冻结矩阵风险");
        }
        if (ctx.shadowPromotionStrong) {
            sb.append("，SHADOW强域");
        }
        if (ctx.adaptiveJudgeStatus.length() > 0) {
            sb.append("，智能审判=").append(ctx.adaptiveJudgeStatus);
        }
        sb.append("，建议=").append(ctx.finalDecisionActionAdvice);
        return sb.toString();
    }

    private boolean containsBadWord(String text) {
        if (text == null) {
            return false;
        }
        String s = text.toUpperCase(java.util.Locale.ROOT);
        return s.indexOf("MISLEADING") >= 0
                || s.indexOf("RISK") >= 0
                || s.indexOf("WEAK") >= 0
                || s.indexOf("偏弱") >= 0
                || s.indexOf("误导") >= 0
                || s.indexOf("亏") >= 0;
    }

    private boolean containsFreezeWord(String text) {
        if (text == null) {
            return false;
        }
        String s = text.toUpperCase(java.util.Locale.ROOT);

        /*
         * FIX01:
         * 不能因为出现“冻结矩阵”四个字就判定冻结风险。
         * 自动诊断里常见：
         *   冻结矩阵：规则=13 硬风险=0 ... RECOVER=13
         *   智能审判：DOWNGRADE=0 冻结=0
         * 这些都是统计字段，不是硬拦截。
         */
        if (metricPositive(s, "FREEZE")
                || metricPositive(s, "HARD_FREEZE")
                || metricPositive(s, "HARD_RISK")
                || metricPositive(s, "硬风险")
                || metricPositive(s, "硬冻结")
                || metricPositive(s, "冻结")) {
            return true;
        }

        if (s.indexOf("FINAL_DECISION_FREEZE") >= 0
                || s.indexOf("FREEZE_ACTIVE") >= 0
                || s.indexOf("ACTIVE_FREEZE") >= 0
                || s.indexOf("HARD_FREEZE") >= 0
                || s.indexOf("冻结中") >= 0
                || s.indexOf("硬冻结") >= 0
                || s.indexOf("禁止真实BET") >= 0
                || s.indexOf("禁止真实 BET") >= 0
                || s.indexOf("禁止出手") >= 0) {
            return true;
        }

        return false;
    }

    private boolean containsRejectWord(String text) {
        if (text == null) {
            return false;
        }
        String s = text.toUpperCase(java.util.Locale.ROOT);

        if (metricPositive(s, "REJECT")
                || metricPositive(s, "DOWNGRADE")
                || metricPositive(s, "FAKE")
                || metricPositive(s, "拒绝")
                || metricPositive(s, "降级")) {
            return true;
        }

        if (s.indexOf("REJECT_FAKE_STRONG") >= 0
                || s.indexOf("DOWNGRADE_TO_SHADOW") >= 0
                || s.indexOf("REJECT_TO_SHADOW") >= 0
                || s.indexOf("拒绝真实BET") >= 0
                || s.indexOf("拒绝真实 BET") >= 0
                || s.indexOf("降级到SHADOW") >= 0
                || s.indexOf("降级为SHADOW") >= 0
                || s.indexOf("降级观察") >= 0) {
            return true;
        }

        return false;
    }

    private boolean containsPassWord(String text) {
        if (text == null) {
            return false;
        }
        String s = text.toUpperCase(java.util.Locale.ROOT);

        if (metricPositive(s, "PASS")
                || metricPositive(s, "PROMOTE")
                || metricPositive(s, "通过")
                || metricPositive(s, "晋级")) {
            return true;
        }

        if (s.indexOf("PASS_TO_BET") >= 0
                || s.indexOf("PASS_TO_GATE") >= 0
                || s.indexOf("PROMOTE_WATCH") >= 0
                || s.indexOf("允许进入") >= 0
                || s.indexOf("允许真实") >= 0) {
            return true;
        }

        return false;
    }

    private boolean containsGoodWord(String text) {
        if (text == null) {
            return false;
        }
        String s = text.toUpperCase(java.util.Locale.ROOT);

        if (metricPositive(s, "PROMOTE")
                || metricPositive(s, "PASS")
                || metricPositive(s, "STRONG")
                || metricPositive(s, "READY")
                || metricPositive(s, "晋级")
                || metricPositive(s, "强域")) {
            return true;
        }

        if (s.indexOf("PROMOTE_WATCH") >= 0
                || s.indexOf("PASS_TO_GATE") >= 0
                || s.indexOf("PASS_TO_BET") >= 0
                || s.indexOf("强域") >= 0
                || s.indexOf("可晋级") >= 0) {
            return true;
        }

        return false;
    }

    private boolean metricPositive(String text, String key) {
        if (text == null || key == null || key.length() == 0) {
            return false;
        }
        String s = text.toUpperCase(java.util.Locale.ROOT);
        String k = key.toUpperCase(java.util.Locale.ROOT);
        int from = 0;
        while (true) {
            int idx = s.indexOf(k, from);
            if (idx < 0) {
                return false;
            }
            int p = idx + k.length();
            while (p < s.length() && Character.isWhitespace(s.charAt(p))) {
                p++;
            }
            if (p < s.length()) {
                char sep = s.charAt(p);
                if (sep == '=' || sep == ':' || sep == '：') {
                    p++;
                    while (p < s.length() && Character.isWhitespace(s.charAt(p))) {
                        p++;
                    }
                    int start = p;
                    boolean dot = false;
                    while (p < s.length()) {
                        char ch = s.charAt(p);
                        if (ch >= '0' && ch <= '9') {
                            p++;
                            continue;
                        }
                        if (ch == '.' && !dot) {
                            dot = true;
                            p++;
                            continue;
                        }
                        break;
                    }
                    if (p > start) {
                        try {
                            double v = Double.parseDouble(s.substring(start, p));
                            if (v > 0.000001d) {
                                return true;
                            }
                        } catch (Throwable e) {
                        }
                    }
                }
            }
            from = idx + k.length();
        }
    }

    private DecisionContext buildDecisionContext(
            GateRuntimeContext runtime,
            Models.ComboCandidate c,
            TrendStateMachine.TrendTrace trace,
            BreakRecoveryEngine.RecoveryResult recovery,
            Models.SimilarResult similar,
            ShapeMemoryEngine.MemorySignal memory,
            CalibrationEngine.CalibrationSignal calibration,
            List<Models.Draw> history,
            boolean buildFullReason) {

        DecisionContext ctx = new DecisionContext();

        if (c == null) {
            return ctx;
        }

        ctx.stateKey = buildStateKey(c);
        ctx.numberShape = buildNumberShape(c);
        ctx.learningStrong = isLearningStrong(c);
        ctx.learningWeak = isLearningWeak(c);
        ctx.evidenceStrong = isEvidenceStrong(c);
        ctx.realRiskHigh = isRealRiskHigh(c);

        fillTeacherMatrixContext(ctx, c);
        fillV20Context(ctx, c);
        fillDynamicCausalDecisionContext(ctx, c, runtime);
        fillFinalDiagnosisContext(ctx, c, runtime);
        V20DecisionEvidence decisionEvidence = buildV20DecisionEvidence(c, history, runtime);
        applyV20DecisionEvidenceToCandidate(c, decisionEvidence);
        fillDecisionEvidenceContext(ctx, decisionEvidence);
        mergeFinalDiagnosisWithDecisionEvidence(ctx, c, runtime);
        refreshFinalDecisionContext(ctx, c);
        writeFinalDecisionAuditFields(c, ctx);

        if (buildFullReason) {
            ctx.actualPredictionLogic = buildPredictionLogic(c, trace, recovery, similar, memory);
            ctx.nextAdvice = buildNextAdvice(c, trace, recovery, similar, memory, calibration);
        } else {
            ctx.actualPredictionLogic = "";
            ctx.nextAdvice = "";
        }

        if (ctx.teacherMatrixStrong || ctx.teacherMatrixReady) {
            ctx.learningStrong = true;
        }

        if (ctx.teacherMatrixRisk) {
            ctx.realRiskHigh = true;
        }

        if (ctx.v20Strong || ctx.v20Ready) {
            ctx.learningStrong = true;
        }

        if (ctx.v20RiskHigh || ctx.v20HardRisk) {
            ctx.realRiskHigh = true;
        }

        if (ctx.decisionEvidenceStrong || ctx.decisionEvidenceElite) {
            ctx.learningStrong = true;
        }

        if (ctx.decisionCounterRiskHigh || ctx.decisionHardRisk) {
            ctx.realRiskHigh = true;
        }

        if (ctx.dynamicCausalStrong || ctx.dynamicCausalReady) {
            ctx.learningStrong = true;
        }

        if (ctx.dynamicCausalHardRisk || ctx.dynamicCausalFreeze || ctx.dynamicCausalRiskHigh) {
            ctx.realRiskHigh = true;
        }

        if (runtime != null) {
            ctx.learningStateText =
                    runtime.learningStateText == null ? "" : runtime.learningStateText;
            ctx.learningReasonText =
                    runtime.learningReasonText == null ? "" : runtime.learningReasonText;
            ctx.learningAdviceText =
                    runtime.learningAdviceText == null ? "" : runtime.learningAdviceText;
        }

        return ctx;
    }

    private void fillDynamicCausalDecisionContext(
            DecisionContext ctx, Models.ComboCandidate c, GateRuntimeContext runtime) {
        if (ctx == null || c == null) {
            return;
        }
        ctx.dynamicClusterId =
                firstReadString(
                        c, new String[] {"dynamicClusterId", "clusterId", "promotionClusterId"});
        ctx.dynamicClusterStatus =
                normalizeDynamicText(
                        firstReadString(
                                c,
                                new String[] {
                                    "dynamicClusterStatus",
                                    "clusterStatus",
                                    "promotionClusterStatus"
                                }));
        ctx.dynamicClusterPrototypeKey =
                firstReadString(
                        c,
                        new String[] {
                            "dynamicClusterPrototypeKey",
                            "dynamicClusterFeatureKey",
                            "windowFeatureKey",
                            "dynamicWindowStateKey"
                        });
        ctx.dynamicClusterSample =
                maxReadInt(
                        c,
                        new String[] {
                            "dynamicClusterSample", "clusterSample", "promotionClusterSample"
                        });
        ctx.dynamicClusterRecentSample =
                maxReadInt(
                        c,
                        new String[] {
                            "dynamicClusterRecentSample",
                            "clusterRecentSample",
                            "promotionClusterRecentSample"
                        });
        ctx.dynamicClusterHitRate =
                maxReadDouble(
                        c,
                        new String[] {
                            "dynamicClusterHitRate", "clusterHitRate", "promotionClusterHitRate"
                        });
        ctx.dynamicClusterRecentHitRate =
                maxReadDouble(
                        c,
                        new String[] {
                            "dynamicClusterRecentHitRate",
                            "clusterRecentHitRate",
                            "promotionClusterRecentHitRate"
                        });
        ctx.dynamicClusterConfidence =
                maxReadDouble(
                        c,
                        new String[] {
                            "dynamicClusterConfidence",
                            "clusterConfidence",
                            "promotionClusterConfidence"
                        });
        ctx.dynamicClusterDistance =
                maxReadDouble(
                        c,
                        new String[] {
                            "dynamicClusterDistance", "clusterDistance", "dynamicWindowDistance"
                        });
        ctx.dynamicClusterMaxLose =
                maxReadInt(
                        c,
                        new String[] {
                            "dynamicClusterMaxLose",
                            "clusterMaxLoseStreak",
                            "promotionClusterMaxLose"
                        });
        ctx.dynamicClusterCounterScore =
                maxReadDouble(
                        c,
                        new String[] {
                            "dynamicClusterCounterScore",
                            "clusterCounterScore",
                            "dynamicClusterRisk"
                        });
        ctx.dynamicClusterReason =
                firstReadString(
                        c,
                        new String[] {
                            "dynamicClusterReason", "clusterReason", "dynamicWindowClusterReason"
                        });
        ctx.needSupplyMatchScore =
                maxReadDouble(
                        c,
                        new String[] {
                            "needSupplyMatchScore", "needSupplyScore", "needSupplyMatch"
                        });
        ctx.needSupplyThirdQuality =
                maxReadDouble(
                        c,
                        new String[] {
                            "needSupplyThirdQuality",
                            "thirdNumberQualityScore",
                            "dynamicGateThirdNumberQuality"
                        });
        ctx.needSupplyRoleDiversity =
                maxReadDouble(
                        c,
                        new String[] {
                            "needSupplyRoleDiversity", "k3RoleDiversity", "roleDiversity"
                        });
        ctx.needSupplyRisk =
                maxReadDouble(
                        c,
                        new String[] {"needSupplyRisk", "numberSupplyRisk", "needSupplyHardRisk"});
        ctx.needSupplySummary =
                firstReadString(
                        c,
                        new String[] {
                            "needSupplySummary", "positionNeedSummary", "numberSupplySummary"
                        });
        ctx.counterfactualRank =
                maxReadInt(
                        c,
                        new String[] {
                            "counterfactualRank",
                            "counterfactualK3Rank",
                            "counterfactualPositionRank"
                        });
        ctx.counterfactualRankScore =
                maxReadDouble(
                        c,
                        new String[] {
                            "counterfactualRankScore",
                            "counterfactualCauseScore",
                            "counterfactualScore"
                        });
        ctx.counterfactualWhyNotRisk =
                maxReadDouble(
                        c,
                        new String[] {
                            "counterfactualWhyNotRisk",
                            "counterfactualRisk",
                            "counterfactualCounterScore"
                        });
        ctx.causeAblationScore =
                maxReadDouble(
                        c,
                        new String[] {
                            "causeAblationScore", "ablationScore", "causeAblationTotalScore"
                        });
        ctx.causeAblationFalseCauseCount =
                maxReadInt(
                        c,
                        new String[] {
                            "causeAblationFalseCauseCount", "falseCauseCount", "causeFalseCount"
                        });
        ctx.counterfactualSummary =
                firstReadString(
                        c,
                        new String[] {
                            "counterfactualSummary", "counterfactualWhyNot", "causeAblationSummary"
                        });
        ctx.adaptiveCandidateStatus =
                normalizeDynamicText(
                        firstReadString(
                                c,
                                new String[] {
                                    "adaptiveJudgeStatus", "adaptiveStatus", "adaptiveClusterStatus"
                                }));
        ctx.adaptiveCandidateAction =
                normalizeDynamicText(
                        firstReadString(
                                c,
                                new String[] {
                                    "adaptiveJudgeAction",
                                    "adaptiveAction",
                                    "adaptiveDecisionAction"
                                }));
        ctx.adaptiveCauseScore =
                maxReadDouble(
                        c,
                        new String[] {
                            "adaptiveCauseScore", "adaptivePromoteScore", "adaptiveJudgeScore"
                        });
        ctx.adaptiveCounterScore =
                maxReadDouble(
                        c,
                        new String[] {
                            "adaptiveCounterScore", "adaptiveFreezeScore", "adaptiveRiskScore"
                        });
        ctx.adaptiveCandidateReason =
                firstReadString(
                        c,
                        new String[] {
                            "adaptiveJudgeReason", "adaptiveReason", "dynamicCausalSummary"
                        });
        ctx.promotionClusterStatus =
                normalizeDynamicText(
                        firstReadString(
                                c,
                                new String[] {
                                    "promotionClusterStatus",
                                    "promotionStatus",
                                    "shadowPromotionStatus"
                                }));
        ctx.promotionAction =
                normalizeDynamicText(
                        firstReadString(
                                c,
                                new String[] {
                                    "promotionAction",
                                    "promotionClusterAction",
                                    "shadowPromotionAction"
                                }));
        ctx.promotionClusterHitRate =
                maxReadDouble(
                        c,
                        new String[] {
                            "promotionClusterHitRate", "promotionHitRate", "promotionDomainHitRate"
                        });
        ctx.promotionClusterRecentHitRate =
                maxReadDouble(
                        c,
                        new String[] {
                            "promotionClusterRecentHitRate",
                            "promotionRecentHitRate",
                            "promotionDomainRecentHitRate"
                        });
        ctx.promotionClusterSample =
                maxReadInt(
                        c,
                        new String[] {
                            "promotionClusterSample", "promotionSample", "promotionDomainSample"
                        });
        ctx.promotionReason =
                firstReadString(
                        c,
                        new String[] {
                            "promotionReason", "promotionClusterReason", "shadowPromotionReason"
                        });
        ctx.dynamicCausalFieldsPresent =
                ctx.dynamicClusterId.length() > 0
                        || ctx.dynamicClusterStatus.length() > 0
                        || ctx.needSupplyMatchScore > 0.0d
                        || ctx.counterfactualWhyNotRisk > 0.0d
                        || ctx.adaptiveCandidateStatus.length() > 0
                        || ctx.promotionClusterStatus.length() > 0;
        ctx.dynamicClusterReady =
                isReadyLike(ctx.dynamicClusterStatus)
                        || isReadyLike(ctx.promotionClusterStatus)
                        || (ctx.dynamicClusterSample >= DYNAMIC_CAUSAL_MIN_SAMPLE_READY
                                && ctx.dynamicClusterHitRate >= DYNAMIC_CAUSAL_READY_HIT
                                && (ctx.dynamicClusterRecentHitRate <= 0.0d
                                        || ctx.dynamicClusterRecentHitRate
                                                >= DYNAMIC_CAUSAL_RECENT_READY)
                                && ctx.dynamicClusterMaxLose <= DYNAMIC_CAUSAL_MAX_LOSE_READY);
        ctx.dynamicClusterStrong =
                isStrongLike(ctx.dynamicClusterStatus)
                        || isStrongLike(ctx.promotionClusterStatus)
                        || (ctx.dynamicClusterSample >= DYNAMIC_CAUSAL_MIN_SAMPLE_STRONG
                                && ctx.dynamicClusterHitRate >= DYNAMIC_CAUSAL_STRONG_HIT
                                && (ctx.dynamicClusterRecentHitRate <= 0.0d
                                        || ctx.dynamicClusterRecentHitRate
                                                >= DYNAMIC_CAUSAL_READY_HIT)
                                && ctx.dynamicClusterMaxLose <= DYNAMIC_CAUSAL_MAX_LOSE_READY);
        ctx.dynamicClusterRisk =
                isRiskLike(ctx.dynamicClusterStatus)
                        || isRiskLike(ctx.promotionClusterStatus)
                        || (ctx.dynamicClusterSample >= DYNAMIC_CAUSAL_MIN_SAMPLE_WATCH
                                && ctx.dynamicClusterHitRate > 0.0d
                                && ctx.dynamicClusterHitRate < DYNAMIC_CAUSAL_RANDOM_BASELINE)
                        || (ctx.dynamicClusterRecentHitRate > 0.0d
                                && ctx.dynamicClusterRecentHitRate < DYNAMIC_CAUSAL_RANDOM_BASELINE)
                        || ctx.dynamicClusterMaxLose >= DYNAMIC_CAUSAL_MAX_LOSE_FREEZE;
        ctx.needSupplyReady =
                ctx.needSupplyMatchScore >= DYNAMIC_CAUSAL_NEED_READY
                        && ctx.needSupplyThirdQuality >= DYNAMIC_CAUSAL_THIRD_READY
                        && ctx.needSupplyRisk < DYNAMIC_CAUSAL_NEED_HARD_RISK;
        ctx.needSupplyRiskHigh =
                ctx.needSupplyRisk >= DYNAMIC_CAUSAL_NEED_HARD_RISK
                        || (ctx.needSupplyMatchScore > 0.0d
                                && ctx.needSupplyMatchScore < DYNAMIC_CAUSAL_NEED_MIN)
                        || (ctx.needSupplyThirdQuality > 0.0d
                                && ctx.needSupplyThirdQuality < DYNAMIC_CAUSAL_THIRD_MIN);
        ctx.counterfactualReady =
                ctx.counterfactualWhyNotRisk > 0.0d
                        && ctx.counterfactualWhyNotRisk <= 0.560d
                        && ctx.causeAblationFalseCauseCount <= 2
                        && (ctx.causeAblationScore <= 0.0d || ctx.causeAblationScore >= 0.350d);
        ctx.counterfactualRiskHigh =
                ctx.counterfactualWhyNotRisk >= DYNAMIC_CAUSAL_COUNTER_HARD
                        || ctx.causeAblationFalseCauseCount >= DYNAMIC_CAUSAL_FALSE_CAUSE_HARD;
        ctx.adaptiveCandidatePass =
                isPassLike(ctx.adaptiveCandidateStatus) || isPassLike(ctx.adaptiveCandidateAction);
        ctx.adaptiveWatchNeutral = isAdaptiveWatchNeutralForFinal(c, ctx);
        ctx.adaptiveCandidateFreeze =
                !ctx.adaptiveWatchNeutral
                        && (isRiskLike(ctx.adaptiveCandidateStatus)
                                || isRiskLike(ctx.adaptiveCandidateAction)
                                || ctx.adaptiveCounterScore >= DYNAMIC_CAUSAL_HARD_RISK);
        ctx.promotionReady =
                isReadyLike(ctx.promotionClusterStatus)
                        || isReadyLike(ctx.promotionAction)
                        || (ctx.promotionClusterSample >= DYNAMIC_CAUSAL_MIN_SAMPLE_READY
                                && ctx.promotionClusterHitRate >= DYNAMIC_CAUSAL_READY_HIT);
        ctx.promotionFreeze =
                isRiskLike(ctx.promotionClusterStatus)
                        || isRiskLike(ctx.promotionAction)
                        || (ctx.promotionClusterSample >= DYNAMIC_CAUSAL_MIN_SAMPLE_WATCH
                                && ctx.promotionClusterHitRate > 0.0d
                                && ctx.promotionClusterHitRate < DYNAMIC_CAUSAL_RANDOM_BASELINE);
        ctx.dynamicCausalQualityScore = computeDynamicCausalQuality(ctx);
        ctx.dynamicCausalRiskScore = computeDynamicCausalRisk(ctx);
        ctx.dynamicCausalReady =
                ctx.dynamicClusterReady
                        && ctx.needSupplyReady
                        && !ctx.counterfactualRiskHigh
                        && !ctx.needSupplyRiskHigh
                        && !ctx.dynamicClusterRisk
                        && !ctx.adaptiveCandidateFreeze
                        && !ctx.promotionFreeze
                        && ctx.dynamicCausalQualityScore >= DYNAMIC_CAUSAL_READY_QUALITY
                        && ctx.dynamicCausalRiskScore < DYNAMIC_CAUSAL_HARD_RISK;
        ctx.dynamicCausalStrong =
                ctx.dynamicClusterStrong
                        && ctx.needSupplyReady
                        && ctx.counterfactualReady
                        && (ctx.adaptiveCandidatePass || ctx.promotionReady)
                        && ctx.dynamicCausalQualityScore >= DYNAMIC_CAUSAL_STRONG_QUALITY
                        && ctx.dynamicCausalRiskScore < 0.560d;
        ctx.dynamicCausalRiskHigh =
                ctx.dynamicCausalRiskScore >= 0.560d
                        || ctx.dynamicClusterRisk
                        || ctx.needSupplyRiskHigh
                        || ctx.counterfactualRiskHigh;
        ctx.dynamicCausalFreeze =
                ctx.dynamicClusterRisk
                        || ctx.promotionFreeze
                        || ctx.adaptiveCandidateFreeze
                        || (ctx.dynamicCausalRiskScore >= DYNAMIC_CAUSAL_HARD_RISK);
        ctx.dynamicCausalHardRisk =
                ctx.dynamicCausalFreeze
                        || ctx.counterfactualWhyNotRisk >= DYNAMIC_CAUSAL_COUNTER_HARD
                        || ctx.needSupplyRisk >= DYNAMIC_CAUSAL_NEED_HARD_RISK;
        ctx.dynamicCausalActionAdvice = buildDynamicCausalActionAdvice(ctx);
        ctx.dynamicCausalSummary = buildDynamicCausalReason(ctx);
    }

    private double computeDynamicCausalQuality(DecisionContext ctx) {
        if (ctx == null) return 0.0d;
        double cluster = 0.0d;
        if (ctx.dynamicClusterStrong) cluster = 1.0d;
        else if (ctx.dynamicClusterReady) cluster = 0.760d;
        else if (ctx.dynamicClusterHitRate > 0.0d)
            cluster =
                    clamp01(
                            (ctx.dynamicClusterHitRate - DYNAMIC_CAUSAL_RANDOM_BASELINE) * 4.0d
                                    + 0.38d);
        double need = clamp01(ctx.needSupplyMatchScore);
        double third = clamp01(ctx.needSupplyThirdQuality);
        double cf =
                ctx.counterfactualWhyNotRisk <= 0.0d
                        ? 0.30d
                        : clamp01(1.0d - ctx.counterfactualWhyNotRisk);
        double ab = clamp01(ctx.causeAblationScore);
        double adaptive = ctx.adaptiveCandidatePass ? 0.80d : clamp01(ctx.adaptiveCauseScore);
        double promote = ctx.promotionReady ? 0.76d : clamp01(ctx.promotionClusterHitRate);
        return clamp01(
                cluster * 0.30d
                        + need * 0.22d
                        + third * 0.10d
                        + cf * 0.14d
                        + ab * 0.08d
                        + adaptive * 0.08d
                        + promote * 0.08d);
    }

    private double computeDynamicCausalRisk(DecisionContext ctx) {
        if (ctx == null) return 1.0d;
        double r = 0.0d;
        if (!ctx.dynamicCausalFieldsPresent) r += 0.46d;
        if (ctx.dynamicClusterId.length() == 0) r += 0.18d;
        if (ctx.dynamicClusterRisk) r += 0.30d;
        if (ctx.dynamicClusterSample > 0
                && ctx.dynamicClusterSample < DYNAMIC_CAUSAL_MIN_SAMPLE_WATCH) r += 0.16d;
        if (ctx.needSupplyRiskHigh) r += 0.24d;
        if (ctx.counterfactualRiskHigh) r += 0.28d;
        if (ctx.adaptiveCandidateFreeze || ctx.promotionFreeze) r += 0.30d;
        if (ctx.dynamicClusterCounterScore > 0.0d)
            r += Math.min(0.16d, ctx.dynamicClusterCounterScore * 0.16d);
        return clamp01(r);
    }

    private boolean isMiniCoreOnlyForFinal(Models.ComboCandidate c, DecisionContext ctx) {
        String s = "";
        if (c != null) {
            s =
                    firstReadString(
                            c,
                            new String[] {
                                "teacherMatrixStatus",
                                "teacherStatus",
                                "v20Status",
                                "dynamicStatus",
                                "dynamicClusterStatus",
                                "adaptiveJudgeStatus",
                                "promotionClusterStatus",
                                "dynamicCausalDecisionStatus"
                            });
        }
        if (ctx != null) {
            s =
                    s
                            + "|"
                            + ctx.dynamicClusterStatus
                            + "|"
                            + ctx.adaptiveCandidateStatus
                            + "|"
                            + ctx.promotionClusterStatus;
        }
        String u = s == null ? "" : s.toUpperCase(java.util.Locale.ROOT);
        if (u.indexOf("MINI_CORE") < 0) {
            return false;
        }
        if (ctx == null) {
            return true;
        }
        if (ctx.needSupplyThirdQuality < DYNAMIC_CAUSAL_THIRD_READY) {
            return true;
        }
        if (ctx.needSupplyMatchScore < DYNAMIC_CAUSAL_NEED_READY) {
            return true;
        }
        if (!ctx.counterfactualReady) {
            return true;
        }
        if (!ctx.adaptiveCandidatePass && !ctx.promotionReady) {
            return true;
        }
        if (ctx.dynamicClusterId == null || ctx.dynamicClusterId.length() == 0) {
            return true;
        }
        return false;
    }

    private String dynamicCausalFinalBlockCode(Models.ComboCandidate c, DecisionContext ctx) {
        if (c == null || !c.isValidK3()) return AppConfig.BLOCK_COMBO_WEAK;
        /*
         * 73487246: absence of a mature dynamic sample is neutral, never a hard block.
         * A fresh install must be able to use NORMAL/QUALIFIED_FLOOR while SHADOW evidence
         * accumulates.  Only settled, sufficiently large evidence may veto a candidate.
         */
        if (ctx == null || !ctx.dynamicCausalFieldsPresent) return "";
        int settled =
                Math.max(
                        ctx.dynamicClusterSample,
                        Math.max(
                                readInt(c, "dynamicRealSample", 0),
                                readInt(c, "dynamicShadowSample", 0)));
        if (settled < 60) return "";
        if (ctx.dynamicClusterRisk || ctx.dynamicCausalFreeze || ctx.dynamicCausalHardRisk) {
            return ctx.dynamicClusterRisk
                    ? "FINAL_DYNAMIC_CLUSTER_MATURE_HARD_RISK"
                    : "FINAL_DYNAMIC_CAUSAL_MATURE_HARD_RISK";
        }
        if (ctx.dynamicClusterHitRate > 0.0d
                && ctx.dynamicClusterHitRate < 0.245d
                && ctx.dynamicClusterRecentHitRate > 0.0d
                && ctx.dynamicClusterRecentHitRate < 0.260d) {
            return "FINAL_DYNAMIC_CLUSTER_MATURE_BELOW_BASELINE";
        }
        if (ctx.dynamicClusterMaxLose >= DYNAMIC_CAUSAL_MAX_LOSE_FREEZE)
            return "FINAL_DYNAMIC_CLUSTER_MATURE_MAX_LOSE";
        if (ctx.needSupplyRisk >= DYNAMIC_CAUSAL_NEED_HARD_RISK
                && ctx.counterfactualWhyNotRisk >= DYNAMIC_CAUSAL_COUNTER_HARD) {
            return "FINAL_DYNAMIC_MATURE_COMBINED_HARD_RISK";
        }
        if (ctx.adaptiveCandidateFreeze || ctx.promotionFreeze)
            return "FINAL_DYNAMIC_MATURE_FREEZE";
        return "";
    }

    private String buildDynamicCausalReason(DecisionContext ctx) {
        if (ctx == null) return "动态因果=上下文为空";
        StringBuilder sb = new StringBuilder();
        sb.append("动态因果=");
        if (ctx.dynamicCausalStrong) sb.append("STRONG");
        else if (ctx.dynamicCausalReady) sb.append("READY");
        else if (ctx.dynamicCausalFreeze || ctx.dynamicCausalHardRisk) sb.append("FREEZE");
        else if (ctx.dynamicCausalRiskHigh) sb.append("RISK");
        else sb.append("WATCH");
        sb.append("，cluster=")
                .append(ctx.dynamicClusterId)
                .append("/")
                .append(ctx.dynamicClusterStatus);
        sb.append("，sample=").append(ctx.dynamicClusterSample);
        sb.append("，hit=").append(UiFormatter.probability(ctx.dynamicClusterHitRate));
        sb.append("，recent=").append(UiFormatter.probability(ctx.dynamicClusterRecentHitRate));
        sb.append("，maxLose=").append(ctx.dynamicClusterMaxLose);
        sb.append("，NeedSupply=").append(UiFormatter.probability(ctx.needSupplyMatchScore));
        sb.append("，Third=").append(UiFormatter.probability(ctx.needSupplyThirdQuality));
        sb.append("，NSRisk=").append(UiFormatter.probability(ctx.needSupplyRisk));
        sb.append("，CFRisk=").append(UiFormatter.probability(ctx.counterfactualWhyNotRisk));
        sb.append("，FalseCause=").append(ctx.causeAblationFalseCauseCount);
        sb.append("，Adaptive=")
                .append(ctx.adaptiveCandidateStatus)
                .append("/")
                .append(ctx.adaptiveCandidateAction);
        sb.append("，Promotion=")
                .append(ctx.promotionClusterStatus)
                .append("/")
                .append(ctx.promotionAction);
        sb.append("，质量=").append(UiFormatter.probability(ctx.dynamicCausalQualityScore));
        sb.append("，风险=").append(UiFormatter.probability(ctx.dynamicCausalRiskScore));
        sb.append("，建议=").append(ctx.dynamicCausalActionAdvice);
        return sb.toString();
    }

    private String buildDynamicCausalActionAdvice(DecisionContext ctx) {
        if (ctx == null) return "HOLD：动态因果上下文为空";
        if (!ctx.dynamicCausalFieldsPresent) return "HOLD：动态因果字段未穿透，禁止真实BET";
        if (ctx.dynamicCausalFreeze || ctx.dynamicCausalHardRisk)
            return "FREEZE：动态窗口簇/NeedSupply/反事实/晋级域存在硬风险";
        if (ctx.dynamicCausalStrong) return "PASS_TO_BET_GATE：动态窗口簇、需求供给、反事实、晋级域均强支持";
        if (ctx.dynamicCausalReady) return "WATCH_TO_GATE：动态因果达到READY，只能送最终门复核";
        return "WATCH：动态因果未达到READY，保持SHADOW/HOLD";
    }

    private boolean dynamicPreferredReviewOk(Models.ComboCandidate c, DecisionContext ctx) {
        if (c == null || ctx == null) return false;
        return ctx.dynamicCausalReady
                && !ctx.dynamicCausalHardRisk
                && !ctx.dynamicCausalFreeze
                && ctx.dynamicCausalRiskScore < 0.560d
                && ctx.needSupplyMatchScore >= DYNAMIC_CAUSAL_NEED_READY
                && ctx.counterfactualWhyNotRisk <= 0.560d
                && ctx.dynamicClusterHitRate >= DYNAMIC_CAUSAL_READY_HIT;
    }

    private boolean isReadyLike(String text) {
        String s = normalizeDynamicText(text);
        return s.indexOf("READY") >= 0
                || s.indexOf("PASS") >= 0
                || s.indexOf("PROMOTE") >= 0
                || (s.indexOf("STRONG") >= 0 && s.indexOf("MINI_CORE") < 0);
    }

    private boolean isStrongLike(String text) {
        String s = normalizeDynamicText(text);
        return s.indexOf("STRONG") >= 0 || s.indexOf("ELITE") >= 0;
    }

    private boolean isPassLike(String text) {
        String s = normalizeDynamicText(text);
        return s.indexOf("PASS") >= 0
                || s.indexOf("READY") >= 0
                || s.indexOf("STRONG") >= 0
                || s.indexOf("PROMOTE") >= 0;
    }

    private boolean isRiskLike(String text) {
        return AppConfig.isExplicitHardBlockStatus(text);
    }

    private String normalizeDynamicText(String text) {
        if (text == null) return "";
        return text.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private void writeDynamicCausalAuditFields(Models.ComboCandidate c, DecisionContext ctx) {
        if (c == null || ctx == null) return;
        writeString(
                c,
                "dynamicCausalDecisionStatus",
                ctx.dynamicCausalStrong
                        ? "STRONG"
                        : (ctx.dynamicCausalReady
                                ? "READY"
                                : (ctx.dynamicCausalFreeze ? "FREEZE" : "WATCH")));
        writeDouble(c, "dynamicCausalDecisionQuality", ctx.dynamicCausalQualityScore);
        writeDouble(c, "dynamicCausalDecisionRisk", ctx.dynamicCausalRiskScore);
        writeString(c, "dynamicCausalDecisionAdvice", ctx.dynamicCausalActionAdvice);
        writeString(c, "dynamicCausalDecisionSummary", ctx.dynamicCausalSummary);
    }

    private void applyDynamicCausalFieldsToDecision(Models.Decision d, Models.ComboCandidate c) {
        if (d == null || c == null) return;
        String[] stringFields =
                new String[] {
                    "dynamicClusterId",
                    "dynamicClusterStatus",
                    "dynamicClusterPrototypeKey",
                    "dynamicClusterFeatureKey",
                    "positionNeedSummary",
                    "numberSupplySummary",
                    "needSupplySummary",
                    "counterfactualWhyNot",
                    "causeAblationSummary",
                    "adaptiveJudgeStatus",
                    "adaptiveJudgeAction",
                    "adaptiveJudgeReason",
                    "adaptiveJudgeDomain",
                    "promotionClusterStatus",
                    "promotionAction",
                    "promotionReason",
                    "dynamicCausalSummary",
                    "dynamicCausalDecisionStatus",
                    "dynamicCausalDecisionAdvice",
                    "dynamicCausalDecisionSummary"
                };
        for (int i = 0; i < stringFields.length; i++)
            writeString(d, stringFields[i], readString(c, stringFields[i], ""));
        String[] doubleFields =
                new String[] {
                    "dynamicClusterHitRate",
                    "dynamicClusterRecentHitRate",
                    "dynamicClusterConfidence",
                    "dynamicClusterDistance",
                    "dynamicClusterCounterScore",
                    "needSupplyMatchScore",
                    "needSupplyThirdQuality",
                    "needSupplyRoleDiversity",
                    "needSupplyRisk",
                    "counterfactualRankScore",
                    "counterfactualWhyNotRisk",
                    "causeAblationScore",
                    "adaptiveCauseScore",
                    "adaptiveCounterScore",
                    "adaptivePromoteScore",
                    "adaptiveFreezeScore",
                    "promotionClusterHitRate",
                    "promotionClusterRecentHitRate",
                    "dynamicCausalDecisionQuality",
                    "dynamicCausalDecisionRisk"
                };
        for (int i = 0; i < doubleFields.length; i++)
            writeDouble(d, doubleFields[i], readDouble(c, doubleFields[i], 0.0d));
        String[] intFields =
                new String[] {
                    "dynamicClusterSample",
                    "dynamicClusterRecentSample",
                    "dynamicClusterMaxLose",
                    "counterfactualRank",
                    "causeAblationFalseCauseCount",
                    "promotionClusterSample",
                    "promotionClusterRecentSample"
                };
        for (int i = 0; i < intFields.length; i++)
            writeInt(d, intFields[i], readInt(c, intFields[i], 0));
        writeString(
                d,
                "finalDecisionDynamicCausalSummary",
                readString(c, "dynamicCausalDecisionSummary", ""));
    }

    private void fillTeacherMatrixContext(DecisionContext ctx, Models.ComboCandidate c) {
        if (ctx == null || c == null) {
            return;
        }

        ctx.teacherMatrixApplied = readInt(c, "teacherMatrixApplied", 0);
        ctx.teacherMatrixSample =
                readInt(
                        c,
                        "teacherMatrixSample",
                        readInt(c, "teacherNumberSample", readInt(c, "teacherSample", 0)));
        ctx.teacherMatrixComboSample =
                readInt(c, "teacherMatrixComboSample", ctx.teacherMatrixSample);

        ctx.teacherMatrixHitRate =
                readDouble(
                        c,
                        "teacherMatrixHitRate",
                        readDouble(
                                c, "teacherNumberHitRate", readDouble(c, "teacherHitRate", 0.0d)));
        ctx.teacherMatrixComboHitRate =
                readDouble(
                        c,
                        "teacherMatrixRecentHitRate",
                        readDouble(c, "teacherMatrixComboHitRate", ctx.teacherMatrixHitRate));

        ctx.teacherMatrixConfidence =
                readDouble(
                        c,
                        "teacherMatrixConfidence",
                        readDouble(
                                c,
                                "teacherNumberConfidence",
                                readDouble(c, "teacherConfidence", 0.0d)));
        ctx.teacherMatrixGateAdjust =
                readDouble(
                        c,
                        "teacherMatrixGateAdjust",
                        readDouble(c, "teacherNumberGateAdjust", 0.0d));
        ctx.teacherMatrixRiskPenalty =
                readDouble(
                        c,
                        "teacherMatrixRiskPenalty",
                        readDouble(
                                c,
                                "teacherMatrixRisk",
                                readDouble(
                                        c,
                                        "teacherNumberRisk",
                                        readDouble(c, "teacherRisk", 0.0d))));
        ctx.teacherMatrixStatus =
                readString(
                        c,
                        "teacherMatrixStatus",
                        readString(c, "teacherNumberStatus", readString(c, "teacherStatus", "")));
        ctx.teacherMatrixSummary =
                readString(
                        c,
                        "teacherMatrixSummary",
                        readString(c, "teacherNumberSummary", readString(c, "teacherSummary", "")));
        ctx.teacherNoSampleNeutral = false;
        if ((ctx.teacherMatrixStatus == null || ctx.teacherMatrixStatus.length() == 0)
                && ctx.teacherMatrixSample <= 0
                && ctx.teacherMatrixComboSample <= 0
                && ctx.teacherMatrixHitRate <= 0.0d
                && ctx.teacherMatrixConfidence <= 0.0d) {
            ctx.teacherMatrixStatus = STATUS_NO_SAMPLE_NEUTRAL;
        }
        if (isTeacherNoSampleNeutralText(ctx.teacherMatrixStatus)
                && ctx.teacherMatrixSample <= 0
                && ctx.teacherMatrixComboSample <= 0
                && readDouble(
                                c,
                                "teacherEvidenceContribution",
                                readDouble(c, "teacherMatrixEvidenceContribution", 0.0d))
                        <= 0.0d
                && readDouble(
                                c,
                                "teacherCounterContribution",
                                readDouble(c, "teacherMatrixCounterContribution", 0.0d))
                        <= 0.0d) {
            ctx.teacherNoSampleNeutral = true;
            ctx.teacherMatrixApplied = 0;
            ctx.teacherMatrixStrong = false;
            ctx.teacherMatrixReady = false;
            ctx.teacherMatrixRisk = false;
            ctx.teacherMatrixFreeze = false;
            writeString(c, "teacherMatrixStatus", STATUS_NO_SAMPLE_NEUTRAL);
            writeString(c, "teacherStatus", STATUS_NO_SAMPLE_NEUTRAL);
            writeString(c, "teacherNumberStatus", STATUS_NO_SAMPLE_NEUTRAL);
            writeInt(c, "teacherNoSampleNeutralFlag", 1);
            writeInt(c, "teacherMatrixNoSampleNeutralFlag", 1);
            writeInt(c, "teacherWeakNoBetFlag", 0);
            writeInt(c, "teacherMatrixWeakNoBetFlag", 0);
            writeInt(c, "teacherMatrixOrdinaryBetBlockFlag", 0);
            writeString(c, "teacherOrdinaryBetPolicy", STATUS_NO_SAMPLE_NEUTRAL);
            writeString(c, "teacherMatrixOrdinaryBetPolicy", STATUS_NO_SAMPLE_NEUTRAL);
            return;
        }

        if (ctx.teacherMatrixSample > 0
                || ctx.teacherMatrixComboSample > 0
                || ctx.teacherMatrixConfidence > 0.0d
                || ctx.teacherMatrixHitRate > 0.0d) {
            ctx.teacherMatrixApplied = 1;
        }

        ctx.teacherMatrixStrong = false;
        ctx.teacherMatrixReady = false;
        ctx.teacherMatrixRisk = false;

        if ("STRONG".equals(ctx.teacherMatrixStatus)) {
            ctx.teacherMatrixStrong = true;
        }

        if ("READY".equals(ctx.teacherMatrixStatus) || "NORMAL".equals(ctx.teacherMatrixStatus)) {
            ctx.teacherMatrixReady = true;
        }

        if ("RISK".equals(ctx.teacherMatrixStatus) || "FREEZE".equals(ctx.teacherMatrixStatus)) {
            ctx.teacherMatrixRisk = true;
        }

        ctx.teacherMatrixFreeze = false;
        if ("FREEZE".equals(ctx.teacherMatrixStatus)
                || readInt(c, "teacherMatrixFreezeFlag", 0) == 1) {
            ctx.teacherMatrixFreeze = true;
            ctx.teacherMatrixRisk = true;
        }

        ctx.teacherEvidenceContribution =
                readDouble(
                        c,
                        "teacherEvidenceContribution",
                        readDouble(c, "teacherMatrixEvidenceContribution", 0.0d));
        ctx.teacherCounterContribution =
                readDouble(
                        c,
                        "teacherCounterContribution",
                        readDouble(c, "teacherMatrixCounterContribution", 0.0d));
        ctx.teacherSampleReliability =
                readDouble(
                        c,
                        "teacherSampleReliability",
                        readDouble(c, "teacherMatrixSampleReliability", 0.0d));
        ctx.teacherRecentValidity =
                readDouble(
                        c,
                        "teacherRecentValidity",
                        readDouble(c, "teacherMatrixRecentValidity", 0.0d));
        ctx.teacherActionAdvice =
                readString(
                        c, "teacherActionAdvice", readString(c, "teacherMatrixActionAdvice", ""));

        if (ctx.teacherMatrixSample >= TEACHER_MATRIX_STRONG_SAMPLE
                && ctx.teacherMatrixHitRate >= TEACHER_STRONG_HIT_RATE
                && ctx.teacherMatrixConfidence >= TEACHER_STRONG_CONF) {
            ctx.teacherMatrixStrong = true;
        }

        if (ctx.teacherMatrixComboSample >= TEACHER_MATRIX_COMBO_STRONG_SAMPLE
                && ctx.teacherMatrixComboHitRate >= TEACHER_STRONG_HIT_RATE
                && ctx.teacherMatrixConfidence >= TEACHER_READY_CONF) {
            ctx.teacherMatrixStrong = true;
        }

        if (ctx.teacherMatrixSample >= TEACHER_MATRIX_MIN_SAMPLE
                && ctx.teacherMatrixHitRate >= TEACHER_READY_HIT_RATE
                && ctx.teacherMatrixConfidence >= TEACHER_READY_CONF) {
            ctx.teacherMatrixReady = true;
        }

        if (ctx.teacherMatrixComboSample >= TEACHER_MATRIX_COMBO_MIN_SAMPLE
                && ctx.teacherMatrixComboHitRate >= TEACHER_READY_HIT_RATE
                && ctx.teacherMatrixConfidence >= TEACHER_READY_CONF) {
            ctx.teacherMatrixReady = true;
        }

        if (ctx.teacherMatrixSample >= TEACHER_MATRIX_STRONG_SAMPLE
                && ctx.teacherMatrixHitRate > 0.0d
                && ctx.teacherMatrixHitRate < TEACHER_RISK_HIT_RATE
                && ctx.teacherMatrixConfidence >= TEACHER_READY_CONF) {
            ctx.teacherMatrixRisk = true;
        }

        if (ctx.teacherMatrixRiskPenalty >= 0.60d || ctx.teacherCounterContribution >= 0.66d) {
            ctx.teacherMatrixRisk = true;
        }

        if (ctx.teacherSampleReliability <= 0.0d && ctx.teacherMatrixSample > 0) {
            ctx.teacherSampleReliability = clamp01(((double) ctx.teacherMatrixSample) / 160.0d);
        }

        if (ctx.teacherRecentValidity <= 0.0d) {
            ctx.teacherRecentValidity =
                    ctx.teacherMatrixComboHitRate > 0.0d
                            ? ctx.teacherMatrixComboHitRate
                            : ctx.teacherMatrixHitRate;
        }

        if (ctx.teacherMatrixFreeze) {
            ctx.teacherMatrixStrong = false;
            ctx.teacherMatrixReady = false;
        }

        if (ctx.teacherMatrixStrong) {
            ctx.teacherMatrixReady = true;
            ctx.teacherMatrixRisk = false;
        }
    }

    private void fillV20Context(DecisionContext ctx, Models.ComboCandidate c) {
        if (ctx == null || c == null) {
            return;
        }

        ctx.v20CauseScore =
                maxValue(
                        readDouble(c, "causeScore", 0.0d),
                        readDouble(c, "finalCauseScore", 0.0d),
                        readDouble(c, "dynamicCauseScore", 0.0d),
                        readDouble(c, "v20CauseScore", 0.0d),
                        readDouble(c, "v20DynamicScore", 0.0d));

        ctx.v20CauseRisk =
                maxValue(
                        readDouble(c, "riskScore", 0.0d),
                        readDouble(c, "causeRisk", 0.0d),
                        readDouble(c, "dynamicCauseRisk", 0.0d),
                        readDouble(c, "v20RiskScore", 0.0d));

        ctx.ruleHealthScore = readDouble(c, "ruleHealthScore", 0.0d);
        ctx.ruleHealthRisk = readDouble(c, "ruleHealthRisk", readDouble(c, "ruleRiskScore", 0.0d));
        ctx.ruleHealthPass = readInt(c, "ruleHealthPass", 0);
        ctx.ruleHealthHardCount = readInt(c, "ruleHealthHardCount", 0);
        ctx.ruleHealthSummary = readString(c, "ruleHealthSummary", "");

        ctx.thirdGuardScore = readDouble(c, "thirdGuardScore", readDouble(c, "guardScore", 0.0d));
        ctx.thirdGuardRisk = readDouble(c, "thirdGuardRisk", readDouble(c, "guardRisk", 0.0d));
        ctx.thirdGuardPass = readInt(c, "thirdGuardPass", readInt(c, "guardPass", 0));
        ctx.thirdGuardHard = readInt(c, "thirdGuardHardBlock", readInt(c, "hardBlock", 0));
        ctx.thirdGuardSummary =
                readString(c, "thirdGuardSummary", readString(c, "guardSummary", ""));

        ctx.shadowHealthScore =
                readDouble(c, "shadowHealthScore", readDouble(c, "shadowHealth", 0.0d));
        ctx.shadowHealthRisk = readDouble(c, "shadowHealthRisk", readDouble(c, "shadowRisk", 0.0d));
        ctx.shadowHealthPass = readInt(c, "shadowHealthPass", readInt(c, "shadowPass", 0));
        ctx.shadowPromoteEligible = readInt(c, "shadowPromoteEligible", 0);
        ctx.shadowHealthSummary = readString(c, "shadowHealthSummary", "");

        ctx.falseStrongFreezeRisk =
                maxValue(
                        readDouble(c, "falseStrongFreezeRisk", 0.0d),
                        readDouble(c, "falseStrongRisk", 0.0d),
                        readDouble(c, "falseChampionRisk", 0.0d));
        ctx.falseStrongFreeze = readInt(c, "falseStrongFreeze", 0);
        ctx.falseStrongHardFreeze = readInt(c, "falseStrongHardFreeze", 0);
        ctx.falseStrongFreezeLevel = readString(c, "falseStrongFreezeLevel", "");
        ctx.falseStrongFreezeSummary = readString(c, "falseStrongFreezeSummary", "");

        ctx.counterfactualScore =
                readDouble(
                        c, "counterfactualScore", readDouble(c, "counterfactualRankScore", 0.0d));
        ctx.counterfactualRisk =
                maxValue(
                        readDouble(c, "counterfactualRisk", 0.0d),
                        readDouble(c, "missedStrongRisk", 0.0d),
                        readDouble(c, "nearConflictRisk", 0.0d),
                        readDouble(c, "falseChampionRisk", 0.0d));
        ctx.counterfactualStatus = readString(c, "counterfactualStatus", "");
        ctx.counterfactualSummary = readString(c, "counterfactualSummary", "");

        ctx.teacherNumberScore =
                readDouble(
                        c,
                        "teacherNumberScore",
                        readDouble(
                                c,
                                "numberTeacherScore",
                                readDouble(c, "teacherMatrixScore", 0.0d)));
        ctx.teacherNumberRisk =
                readDouble(
                        c,
                        "teacherNumberRisk",
                        readDouble(
                                c, "numberTeacherRisk", readDouble(c, "teacherMatrixRisk", 0.0d)));
        ctx.teacherNumberConfidence =
                readDouble(
                        c,
                        "teacherNumberConfidence",
                        readDouble(
                                c,
                                "numberTeacherConfidence",
                                readDouble(c, "teacherMatrixConfidence", 0.0d)));
        ctx.teacherNumberStatus =
                readString(c, "teacherNumberStatus", readString(c, "teacherMatrixStatus", ""));
        ctx.teacherNumberSummary =
                readString(c, "teacherNumberSummary", readString(c, "teacherMatrixSummary", ""));

        ctx.positionHealthScore =
                readDouble(c, "positionHealthScore", readDouble(c, "positionCauseScore", 0.0d));
        ctx.positionWrongRisk =
                maxValue(
                        readDouble(c, "positionWrongRisk", 0.0d),
                        readDouble(c, "positionRiskScore", 0.0d),
                        readDouble(c, "positionDriftRisk", 0.0d),
                        readDouble(c, "positionFalseStrongRisk", 0.0d));
        ctx.positionHealthStatus = readString(c, "positionHealthStatus", "");
        ctx.positionHealthSummary = readString(c, "positionHealthSummary", "");

        ctx.seedCauseScore =
                maxValue(
                        readDouble(c, "seedCauseScore", 0.0d),
                        readDouble(c, "seedScore", 0.0d),
                        readDouble(c, "seedExpansionScore", 0.0d),
                        readDouble(c, "seedSimilarityScore", 0.0d));
        ctx.seedCauseRisk =
                maxValue(
                        readDouble(c, "seedCauseRisk", 0.0d),
                        readDouble(c, "falseSeedRisk", 0.0d),
                        readDouble(c, "seedRisk", 0.0d));
        ctx.seedCauseSummary = readString(c, "seedCauseSummary", readString(c, "seedSummary", ""));

        ctx.dynamicCauseChainScore =
                maxValue(
                        readDouble(c, "dynamicCauseChainScore", 0.0d),
                        readDouble(c, "dynamicCauseScore", 0.0d),
                        readDouble(c, "v20SupportScore", 0.0d),
                        readDouble(c, "v20CauseScore", 0.0d),
                        readDouble(c, "chainScore", 0.0d));
        ctx.dynamicCauseChainRisk =
                maxValue(
                        readDouble(c, "dynamicCauseChainRisk", 0.0d),
                        readDouble(c, "dynamicCauseRisk", 0.0d),
                        readDouble(c, "v20RiskScore", 0.0d),
                        readDouble(c, "chainRisk", 0.0d));
        ctx.dynamicCauseChainSummary =
                readString(
                        c,
                        "dynamicCauseChainSummary",
                        readString(c, "v20Summary", readString(c, "dynamicCauseSummary", "")));

        ctx.v20SupportScore = 0.0d;
        ctx.v20SupportScore += ctx.v20CauseScore * 0.20d;
        ctx.v20SupportScore += ctx.ruleHealthScore * 0.14d;
        ctx.v20SupportScore += ctx.thirdGuardScore * 0.12d;
        ctx.v20SupportScore += ctx.shadowHealthScore * 0.08d;
        ctx.v20SupportScore += ctx.counterfactualScore * 0.10d;
        ctx.v20SupportScore += ctx.teacherNumberScore * 0.12d;
        ctx.v20SupportScore += ctx.positionHealthScore * 0.10d;
        ctx.v20SupportScore += ctx.seedCauseScore * 0.06d;
        ctx.v20SupportScore += ctx.dynamicCauseChainScore * 0.08d;
        ctx.v20SupportScore = clamp01(ctx.v20SupportScore);

        ctx.v20RiskScore = 0.0d;
        ctx.v20RiskScore += ctx.v20CauseRisk * 0.18d;
        ctx.v20RiskScore += ctx.ruleHealthRisk * 0.14d;
        ctx.v20RiskScore += ctx.thirdGuardRisk * 0.14d;
        ctx.v20RiskScore += ctx.shadowHealthRisk * 0.08d;
        ctx.v20RiskScore += ctx.falseStrongFreezeRisk * 0.18d;
        ctx.v20RiskScore += ctx.counterfactualRisk * 0.10d;
        ctx.v20RiskScore += ctx.teacherNumberRisk * 0.08d;
        ctx.v20RiskScore += ctx.positionWrongRisk * 0.06d;
        ctx.v20RiskScore += ctx.seedCauseRisk * 0.04d;
        ctx.v20RiskScore = clamp01(ctx.v20RiskScore);
        ctx.v20WatchNeutral =
                isV20WatchOrEmptyNeutralForFinal(c, ctx)
                        && ctx.v20SupportScore <= 0.080d
                        && ctx.v20RiskScore <= 0.120d;
        if (ctx.v20WatchNeutral) {
            ctx.v20RiskScore = 0.0d;
            writeInt(c, "v20WatchNeutralFlag", 1);
        }

        ctx.v20Ready =
                !ctx.v20WatchNeutral
                        && ctx.v20SupportScore >= V20_READY_SCORE
                        && ctx.v20RiskScore <= 0.620d;
        ctx.v20Strong =
                !ctx.v20WatchNeutral
                        && ctx.v20SupportScore >= V20_STRONG_SCORE
                        && ctx.v20RiskScore <= 0.560d
                        && ctx.ruleHealthHardCount <= 0
                        && ctx.thirdGuardHard <= 0
                        && ctx.falseStrongHardFreeze <= 0;

        ctx.v20Elite =
                !ctx.v20WatchNeutral
                        && ctx.v20SupportScore >= V20_ELITE_SCORE
                        && ctx.v20RiskScore <= 0.500d
                        && ctx.ruleHealthPass == 1
                        && ctx.thirdGuardPass == 1
                        && ctx.falseStrongFreeze <= 0;

        ctx.v20RiskHigh = !ctx.v20WatchNeutral && ctx.v20RiskScore >= V20_RISK_HIGH;
        ctx.v20HardRisk = false;
        ctx.v20HardBlockCode = "";

        if (ctx.ruleHealthHardCount > 0) {
            ctx.v20HardRisk = true;
            ctx.v20HardBlockCode = "RULE_HEALTH_HARD";
        }

        if (ctx.thirdGuardHard > 0) {
            ctx.v20HardRisk = true;
            ctx.v20HardBlockCode = "THIRD_GUARD_HARD";
        }

        if (ctx.falseStrongHardFreeze > 0) {
            ctx.v20HardRisk = true;
            ctx.v20HardBlockCode = "FALSE_STRONG_HARD_FREEZE";
        }

        if (ctx.falseStrongFreeze > 0 && ctx.falseStrongFreezeRisk >= 0.720d) {
            ctx.v20HardRisk = true;
            ctx.v20HardBlockCode = "FALSE_STRONG_FREEZE";
        }

        if (ctx.v20RiskScore >= V20_RISK_HARD) {
            ctx.v20HardRisk = true;
            if (ctx.v20HardBlockCode.length() == 0) {
                ctx.v20HardBlockCode = "V20_CAUSE_HARD_RISK";
            }
        }
    }

    private String buildV20Reason(DecisionContext ctx) {
        if (ctx == null) {
            return "V20因链=无";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("V20因链审计=support=");
        sb.append(UiFormatter.probability(ctx.v20SupportScore));
        sb.append("，risk=");
        sb.append(UiFormatter.probability(ctx.v20RiskScore));
        sb.append("，ready=");
        sb.append(ctx.v20Ready);
        sb.append("，strong=");
        sb.append(ctx.v20Strong);
        sb.append("，elite=");
        sb.append(ctx.v20Elite);
        sb.append("，hard=");
        sb.append(ctx.v20HardRisk);

        if (ctx.v20HardBlockCode != null && ctx.v20HardBlockCode.length() > 0) {
            sb.append("，hardCode=");
            sb.append(ctx.v20HardBlockCode);
        }

        sb.append("；因链=");
        sb.append(UiFormatter.probability(ctx.v20CauseScore));
        sb.append("/");
        sb.append(UiFormatter.probability(ctx.v20CauseRisk));

        sb.append("，规则=");
        sb.append(UiFormatter.probability(ctx.ruleHealthScore));
        sb.append("/");
        sb.append(UiFormatter.probability(ctx.ruleHealthRisk));

        sb.append("，第三防线=");
        sb.append(UiFormatter.probability(ctx.thirdGuardScore));
        sb.append("/");
        sb.append(UiFormatter.probability(ctx.thirdGuardRisk));

        sb.append("，影子健康=");
        sb.append(UiFormatter.probability(ctx.shadowHealthScore));
        sb.append("/");
        sb.append(UiFormatter.probability(ctx.shadowHealthRisk));

        sb.append("，伪强=");
        sb.append(UiFormatter.probability(ctx.falseStrongFreezeRisk));
        sb.append(" freeze=");
        sb.append(ctx.falseStrongFreeze);

        sb.append("，反事实=");
        sb.append(UiFormatter.probability(ctx.counterfactualScore));
        sb.append("/");
        sb.append(UiFormatter.probability(ctx.counterfactualRisk));

        sb.append("，教师号码=");
        sb.append(UiFormatter.probability(ctx.teacherNumberScore));
        sb.append("/");
        sb.append(UiFormatter.probability(ctx.teacherNumberRisk));

        sb.append("，位置=");
        sb.append(UiFormatter.probability(ctx.positionHealthScore));
        sb.append("/");
        sb.append(UiFormatter.probability(ctx.positionWrongRisk));

        sb.append("；说明=V20因链只参与最终门证据和风险，不创建BET，不结算，不写真实校准");

        return sb.toString();
    }

    private void applyV20FieldsToDecision(Models.Decision d, Models.ComboCandidate c) {
        if (d == null || c == null) {
            return;
        }

        writeDouble(d, "causeScore", readDouble(c, "causeScore", 0.0d));
        writeDouble(d, "finalCauseScore", readDouble(c, "finalCauseScore", 0.0d));
        writeDouble(d, "riskScore", readDouble(c, "riskScore", 0.0d));

        writeDouble(d, "ruleHealthScore", readDouble(c, "ruleHealthScore", 0.0d));
        writeDouble(d, "ruleHealthRisk", readDouble(c, "ruleHealthRisk", 0.0d));
        writeInt(d, "ruleHealthPass", readInt(c, "ruleHealthPass", 0));
        writeString(d, "ruleHealthStatus", readString(c, "ruleHealthStatus", ""));
        writeString(d, "ruleHealthSummary", readString(c, "ruleHealthSummary", ""));

        writeDouble(d, "thirdGuardScore", readDouble(c, "thirdGuardScore", 0.0d));
        writeDouble(d, "thirdGuardRisk", readDouble(c, "thirdGuardRisk", 0.0d));
        writeInt(d, "thirdGuardPass", readInt(c, "thirdGuardPass", 0));
        writeString(d, "thirdGuardSummary", readString(c, "thirdGuardSummary", ""));

        writeDouble(d, "shadowHealthScore", readDouble(c, "shadowHealthScore", 0.0d));
        writeDouble(d, "shadowHealthRisk", readDouble(c, "shadowHealthRisk", 0.0d));
        writeInt(d, "shadowHealthPass", readInt(c, "shadowHealthPass", 0));
        writeInt(d, "shadowPromoteEligible", readInt(c, "shadowPromoteEligible", 0));
        writeString(d, "shadowHealthSummary", readString(c, "shadowHealthSummary", ""));

        writeDouble(d, "falseStrongFreezeRisk", readDouble(c, "falseStrongFreezeRisk", 0.0d));
        writeInt(d, "falseStrongFreeze", readInt(c, "falseStrongFreeze", 0));
        writeInt(d, "falseStrongHardFreeze", readInt(c, "falseStrongHardFreeze", 0));
        writeString(d, "falseStrongFreezeLevel", readString(c, "falseStrongFreezeLevel", ""));
        writeString(d, "falseStrongFreezeSummary", readString(c, "falseStrongFreezeSummary", ""));

        writeDouble(d, "counterfactualScore", readDouble(c, "counterfactualScore", 0.0d));
        writeDouble(d, "counterfactualRisk", readDouble(c, "counterfactualRisk", 0.0d));
        writeDouble(d, "missedStrongRisk", readDouble(c, "missedStrongRisk", 0.0d));
        writeDouble(d, "nearConflictRisk", readDouble(c, "nearConflictRisk", 0.0d));
        writeDouble(d, "falseChampionRisk", readDouble(c, "falseChampionRisk", 0.0d));
        writeString(d, "counterfactualStatus", readString(c, "counterfactualStatus", ""));
        writeString(d, "counterfactualSummary", readString(c, "counterfactualSummary", ""));

        writeDouble(
                d,
                "teacherNumberScore",
                readDouble(c, "teacherNumberScore", readDouble(c, "teacherMatrixScore", 0.0d)));
        writeDouble(
                d,
                "teacherNumberRisk",
                readDouble(c, "teacherNumberRisk", readDouble(c, "teacherMatrixRisk", 0.0d)));
        writeDouble(
                d,
                "teacherNumberConfidence",
                readDouble(
                        c,
                        "teacherNumberConfidence",
                        readDouble(c, "teacherMatrixConfidence", 0.0d)));
        writeString(
                d,
                "teacherNumberStatus",
                readString(c, "teacherNumberStatus", readString(c, "teacherMatrixStatus", "")));
        writeString(
                d,
                "teacherNumberSummary",
                readString(c, "teacherNumberSummary", readString(c, "teacherMatrixSummary", "")));

        writeDouble(d, "positionHealthScore", readDouble(c, "positionHealthScore", 0.0d));
        writeDouble(d, "positionWrongRisk", readDouble(c, "positionWrongRisk", 0.0d));
        writeString(d, "positionHealthStatus", readString(c, "positionHealthStatus", ""));
        writeString(d, "positionHealthSummary", readString(c, "positionHealthSummary", ""));

        writeDouble(
                d,
                "seedCauseScore",
                readDouble(c, "seedCauseScore", readDouble(c, "seedScore", 0.0d)));
        writeDouble(
                d,
                "seedCauseRisk",
                readDouble(c, "seedCauseRisk", readDouble(c, "falseSeedRisk", 0.0d)));
        writeString(d, "seedCauseSummary", readString(c, "seedCauseSummary", ""));

        writeDouble(
                d,
                "dynamicCauseChainScore",
                readDouble(c, "dynamicCauseChainScore", readDouble(c, "dynamicCauseScore", 0.0d)));
        writeDouble(
                d,
                "dynamicCauseChainRisk",
                readDouble(c, "dynamicCauseChainRisk", readDouble(c, "dynamicCauseRisk", 0.0d)));
        writeString(
                d,
                "dynamicCauseChainSummary",
                readString(
                        c, "dynamicCauseChainSummary", readString(c, "dynamicCauseSummary", "")));
    }

    private double v20QuickScoreBonus(Models.ComboCandidate c) {
        if (c == null) {
            return 0.0d;
        }

        double support = 0.0d;

        support += readDouble(c, "causeScore", 0.0d) * 0.012d;
        support += readDouble(c, "ruleHealthScore", 0.0d) * 0.010d;
        support += readDouble(c, "thirdGuardScore", 0.0d) * 0.010d;
        support += readDouble(c, "shadowHealthScore", 0.0d) * 0.004d;
        support += readDouble(c, "counterfactualScore", 0.0d) * 0.008d;
        support +=
                readDouble(c, "teacherNumberScore", readDouble(c, "teacherMatrixScore", 0.0d))
                        * 0.010d;
        support += readDouble(c, "positionHealthScore", 0.0d) * 0.006d;
        support += readDouble(c, "seedCauseScore", readDouble(c, "seedScore", 0.0d)) * 0.004d;
        support +=
                readDouble(c, "dynamicCauseChainScore", readDouble(c, "dynamicCauseScore", 0.0d))
                        * 0.006d;

        if (readInt(c, "ruleHealthPass", 0) == 1) {
            support += 0.004d;
        }

        if (readInt(c, "thirdGuardPass", 0) == 1) {
            support += 0.004d;
        }

        if (readInt(c, "falseStrongFreeze", 0) == 1) {
            support -= 0.020d;
        }

        if (readInt(c, "falseStrongHardFreeze", 0) == 1) {
            support -= 0.040d;
        }

        return support;
    }

    private double v20QuickRiskPenalty(Models.ComboCandidate c) {
        if (c == null) {
            return 0.0d;
        }

        double risk = 0.0d;

        risk += readDouble(c, "riskScore", 0.0d) * 0.012d;
        risk += readDouble(c, "ruleHealthRisk", 0.0d) * 0.010d;
        risk += readDouble(c, "thirdGuardRisk", 0.0d) * 0.012d;
        risk += readDouble(c, "shadowHealthRisk", 0.0d) * 0.004d;
        risk +=
                readDouble(c, "falseStrongFreezeRisk", readDouble(c, "falseStrongRisk", 0.0d))
                        * 0.016d;
        risk += readDouble(c, "counterfactualRisk", 0.0d) * 0.008d;
        risk +=
                readDouble(c, "teacherNumberRisk", readDouble(c, "teacherMatrixRisk", 0.0d))
                        * 0.006d;
        risk += readDouble(c, "positionWrongRisk", 0.0d) * 0.006d;

        if (readInt(c, "ruleHealthHardCount", 0) > 0) {
            risk += 0.020d;
        }

        if (readInt(c, "thirdGuardHardBlock", 0) > 0) {
            risk += 0.020d;
        }

        return risk;
    }

    private double v20DecisionScoreBonus(Models.ComboCandidate c) {
        if (c == null) {
            return 0.0d;
        }

        double score = v20QuickScoreBonus(c) * 1.60d;

        if (isV20FastAccept(c)) {
            score += 0.030d;
        }

        return score;
    }

    private double v20DecisionRiskPenalty(Models.ComboCandidate c) {
        if (c == null) {
            return 0.0d;
        }

        double risk = v20QuickRiskPenalty(c) * 1.50d;

        if (readInt(c, "falseStrongHardFreeze", 0) == 1) {
            risk += 0.050d;
        }

        if (readInt(c, "ruleHealthHardCount", 0) > 0) {
            risk += 0.040d;
        }

        return risk;
    }

    private boolean isV20FastAccept(Models.ComboCandidate c) {
        if (c == null) {
            return false;
        }

        double support = 0.0d;
        support += readDouble(c, "causeScore", 0.0d) * 0.22d;
        support += readDouble(c, "ruleHealthScore", 0.0d) * 0.16d;
        support += readDouble(c, "thirdGuardScore", 0.0d) * 0.14d;
        support += readDouble(c, "counterfactualScore", 0.0d) * 0.12d;
        support +=
                readDouble(c, "teacherNumberScore", readDouble(c, "teacherMatrixScore", 0.0d))
                        * 0.14d;
        support += readDouble(c, "positionHealthScore", 0.0d) * 0.10d;
        support +=
                readDouble(c, "dynamicCauseChainScore", readDouble(c, "dynamicCauseScore", 0.0d))
                        * 0.12d;
        support = clamp01(support);

        double risk = 0.0d;
        risk += readDouble(c, "riskScore", 0.0d) * 0.22d;
        risk += readDouble(c, "ruleHealthRisk", 0.0d) * 0.16d;
        risk += readDouble(c, "thirdGuardRisk", 0.0d) * 0.16d;
        risk +=
                readDouble(c, "falseStrongFreezeRisk", readDouble(c, "falseStrongRisk", 0.0d))
                        * 0.20d;
        risk += readDouble(c, "counterfactualRisk", 0.0d) * 0.10d;
        risk +=
                readDouble(c, "teacherNumberRisk", readDouble(c, "teacherMatrixRisk", 0.0d))
                        * 0.08d;
        risk += readDouble(c, "positionWrongRisk", 0.0d) * 0.08d;
        risk = clamp01(risk);

        if (readInt(c, "falseStrongFreeze", 0) == 1) {
            return false;
        }

        if (readInt(c, "falseStrongHardFreeze", 0) == 1) {
            return false;
        }

        if (readInt(c, "ruleHealthHardCount", 0) > 0) {
            return false;
        }

        if (readInt(c, "thirdGuardHardBlock", 0) > 0) {
            return false;
        }

        return support >= V20_STRONG_SCORE
                && risk <= 0.520d
                && c.finalP >= 0.390d
                && c.rawP >= RAW_FLOOR_READY
                && c.edge >= EDGE_ALLOW_READY
                && !isRealRiskHigh(c);
    }

    private String buildStateKey(Models.ComboCandidate c) {
        if (c == null) {
            return "STATE_EMPTY";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("P");
        sb.append(c.position);
        sb.append("_");
        sb.append(c.stateMode == null ? "UNKNOWN" : c.stateMode);
        sb.append("_TL");
        sb.append(bucketScore(c.trendLife));
        sb.append("_DR");
        sb.append(bucketScore(c.driftRisk));
        sb.append("_BD");
        sb.append(bucketScore(c.breakDepth));
        sb.append("_RC");
        sb.append(bucketScore(c.recoveryScore));
        sb.append("_FP");
        sb.append(bucketProb(c.finalP));
        sb.append("_G");
        sb.append(bucketProb(c.dynamicGate));

        return sb.toString();
    }

    private String buildNumberShape(Models.ComboCandidate c) {
        if (c == null) {
            return "";
        }

        int low = 0;
        int mid = 0;
        int high = 0;
        int odd = 0;
        int even = 0;
        int min = 99;
        int max = 0;

        int[] nums = new int[] {c.n1, c.n2, c.n3};

        for (int i = 0; i < nums.length; i++) {
            int n = nums[i];

            if (!AppConfig.isValidNumber(n)) {
                continue;
            }

            if (n <= 3) {
                low++;
            } else if (n <= 7) {
                mid++;
            } else {
                high++;
            }

            if (n % 2 == 0) {
                even++;
            } else {
                odd++;
            }

            if (n < min) {
                min = n;
            }

            if (n > max) {
                max = n;
            }
        }

        StringBuilder sb = new StringBuilder();

        sb.append("低");
        sb.append(low);
        sb.append("/中");
        sb.append(mid);
        sb.append("/高");
        sb.append(high);
        sb.append("，奇");
        sb.append(odd);
        sb.append("/偶");
        sb.append(even);

        if (max >= min && max > 0) {
            sb.append("，跨度");
            sb.append(max - min);
        }

        return sb.toString();
    }

    private String buildPredictionLogic(
            Models.ComboCandidate c,
            TrendStateMachine.TrendTrace trace,
            BreakRecoveryEngine.RecoveryResult recovery,
            Models.SimilarResult similar,
            ShapeMemoryEngine.MemorySignal memory) {

        if (c == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        if (c.trendLife >= trendLifeMin()
                && c.driftRisk <= AppConfig.DRIFT_RISK_MAX
                && c.breakDepth <= AppConfig.BREAK_DEPTH_MAX) {
            sb.append("趋势寿命未断，漂移和断点可控，K3按延续层选择");
        } else if (c.recoveryScore >= AppConfig.RECOVERY_SCORE_MIN) {
            sb.append("恢复分有效，K3按恢复链选择");
        } else if (c.driftRisk > AppConfig.DRIFT_RISK_MAX) {
            sb.append("漂移偏高，K3需保守处理旧热层");
        } else if (c.breakDepth > AppConfig.BREAK_DEPTH_MAX) {
            sb.append("断点偏深，K3需等待新层确认");
        } else {
            sb.append("状态中性，K3依赖概率、组合差、学习矩阵、V20因链共振");
        }

        if (similar != null && similar.effectiveSample > 0) {
            sb.append("；相似样本");
            sb.append(similar.effectiveSample);
            sb.append("，相似率");
            sb.append(UiFormatter.probability(similar.similarHitRate));
        }

        if (memory != null) {
            sb.append("；形态记忆");
            sb.append(UiFormatter.probability(memory.recentMemoryRate));
        }

        if (trace != null && trace.hardBlock == 1) {
            sb.append("；趋势层提示硬风险");
        }

        if (recovery != null && recoveryEngine.isSafeRecovery(recovery)) {
            sb.append("；断点恢复层支持");
        }

        return sb.toString();
    }

    private String buildNextAdvice(
            Models.ComboCandidate c,
            TrendStateMachine.TrendTrace trace,
            BreakRecoveryEngine.RecoveryResult recovery,
            Models.SimilarResult similar,
            ShapeMemoryEngine.MemorySignal memory,
            CalibrationEngine.CalibrationSignal calibration) {

        if (c == null) {
            return "";
        }

        String tmStatus =
                readString(c, "teacherMatrixStatus", readString(c, "teacherNumberStatus", ""));
        int falseFreeze = readInt(c, "falseStrongFreeze", 0);
        int falseHard = readInt(c, "falseStrongHardFreeze", 0);
        double v20Risk =
                maxValue(
                        readDouble(c, "riskScore", 0.0d),
                        readDouble(c, "ruleHealthRisk", 0.0d),
                        readDouble(c, "thirdGuardRisk", 0.0d),
                        readDouble(c, "falseStrongFreezeRisk", 0.0d));

        if (falseHard == 1 || falseFreeze == 1) {
            return "伪强冻结生效，同状态不得救援BET，优先SHADOW观察反推";
        }

        if (v20Risk >= V20_RISK_HIGH) {
            return "V20因链综合风险偏高，同状态提高执行门，等待规则健康/第三防线恢复";
        }

        if ("RISK".equals(tmStatus)) {
            return "Teacher Matrix提示风险，同状态优先SHADOW，等待更多正样本";
        }

        if (c.finalP >= c.dynamicGate
                && c.edge >= EDGE_MIN_NORMAL
                && c.rawP >= RAW_FLOOR_NORMAL
                && !isRealRiskHigh(c)) {
            return "同状态可保留真实BET，但不能加倍，只允许单位置K3";
        }

        if (c.driftRisk > AppConfig.DRIFT_RISK_MAX) {
            return "漂移高时提高门槛，优先SHADOW观察";
        }

        if (c.breakDepth > AppConfig.BREAK_DEPTH_MAX) {
            return "断点深时不抢真实BET，等待恢复确认";
        }

        if (similar != null
                && similar.effectiveSample >= AppConfig.EFFECTIVE_SAMPLE_HIGH
                && similar.similarHitRate < 0.330d) {
            return "相似样本命中率弱，同状态转SHADOW";
        }

        if (memory != null && !memoryEngine.memoryAllowsBet(memory)) {
            return "形态记忆不支持真实BET，同状态降低权重";
        }

        if (calibration != null && calibration.error > AppConfig.CALIBRATION_ERROR_MAX) {
            return "校准误差偏高，降低FinalP或提高执行门";
        }

        if (c.profileScore > 0.0d && c.profileScore < LEARNING_WEAK) {
            return "学习画像偏弱，同状态必须提高证据要求";
        }

        if (c.evidenceRisk >= EVIDENCE_RISK_HIGH) {
            return "证据风险偏高，同状态只做观察";
        }

        return "保持当前证据链；低频强信号可出手，高频只轻微抬门";
    }

    private boolean isLearningStrong(Models.ComboCandidate c) {
        if (c == null) {
            return false;
        }

        if (c.profileScore >= LEARNING_STRONG) {
            return true;
        }

        if (c.fallbackProfileScore >= LEARNING_STRONG) {
            return true;
        }

        if (c.promotionScore >= LEARNING_STRONG) {
            return true;
        }

        if (c.aPlusScore >= LEARNING_STRONG) {
            return true;
        }

        if (c.deepProfileHitRate >= 0.390d && c.deepProfileSample >= 18) {
            return true;
        }

        return false;
    }

    private boolean isLearningWeak(Models.ComboCandidate c) {
        if (c == null) {
            return false;
        }

        if (c.profileScore > 0.0d
                && c.profileScore < LEARNING_WEAK
                && c.fallbackProfileScore < LEARNING_WEAK) {
            return true;
        }

        if (c.auditPenalty >= 0.520d) {
            return true;
        }

        if (c.conflictLevel >= 0.760d) {
            return true;
        }

        String tmStatus =
                readString(c, "teacherMatrixStatus", readString(c, "teacherNumberStatus", ""));
        int tmSample = readInt(c, "teacherMatrixSample", readInt(c, "teacherNumberSample", 0));
        double tmHit =
                readDouble(c, "teacherMatrixHitRate", readDouble(c, "teacherNumberHitRate", 0.0d));

        if ("RISK".equals(tmStatus)) {
            return true;
        }

        if (tmSample >= TEACHER_MATRIX_STRONG_SAMPLE
                && tmHit > 0.0d
                && tmHit < TEACHER_RISK_HIT_RATE) {
            return true;
        }

        if (readInt(c, "falseStrongFreeze", 0) == 1) {
            return true;
        }

        if (readDouble(c, "ruleHealthRisk", 0.0d) >= 0.720d) {
            return true;
        }

        if (readDouble(c, "thirdGuardRisk", 0.0d) >= 0.720d) {
            return true;
        }

        return false;
    }

    private boolean isEvidenceStrong(Models.ComboCandidate c) {
        if (c == null) {
            return false;
        }

        if (c.evidenceScore >= EVIDENCE_STRONG && c.evidenceRisk <= 0.620d) {
            return true;
        }

        if (c.evidenceScore >= EVIDENCE_READY && c.evidenceRisk <= 0.560d && c.finalP >= 0.390d) {
            return true;
        }

        if (c.aiJudgeScore >= EVIDENCE_STRONG && c.aiJudgeConfidence >= 0.540d) {
            return true;
        }

        if (c.deepScore >= EVIDENCE_STRONG && c.deepConfidence >= 0.540d) {
            return true;
        }

        if (c.aPlusScore >= EVIDENCE_STRONG && c.evidenceRisk <= 0.620d) {
            return true;
        }

        if (readDouble(c, "causeScore", 0.0d) >= V20_STRONG_SCORE
                && readDouble(c, "ruleHealthScore", 0.0d) >= V20_READY_SCORE
                && readDouble(c, "falseStrongFreezeRisk", 0.0d) < 0.560d) {
            return true;
        }

        return false;
    }

    private boolean isRealRiskHigh(Models.ComboCandidate c) {
        if (c == null) {
            return false;
        }

        if (c.evidenceRisk >= EVIDENCE_RISK_HIGH) {
            return true;
        }

        if (c.driftRisk >= AppConfig.DRIFT_RISK_MAX + 8.0d) {
            return true;
        }

        if (c.breakDepth >= AppConfig.BREAK_DEPTH_MAX + 8.0d) {
            return true;
        }

        if (c.hotReverseRisk >= AppConfig.HOT_REVERSE_RISK_MAX + 8.0d) {
            return true;
        }

        if (c.falseStrongRisk >= AppConfig.FALSE_STRONG_RISK_MAX + 8.0d) {
            return true;
        }

        String tmStatus =
                readString(c, "teacherMatrixStatus", readString(c, "teacherNumberStatus", ""));
        double tmRisk =
                readDouble(
                        c,
                        "teacherMatrixRiskPenalty",
                        readDouble(
                                c, "teacherMatrixRisk", readDouble(c, "teacherNumberRisk", 0.0d)));

        if ("RISK".equals(tmStatus) || tmRisk >= 0.60d) {
            return true;
        }

        if (readInt(c, "falseStrongHardFreeze", 0) == 1) {
            return true;
        }

        if (readInt(c, "ruleHealthHardCount", 0) > 0) {
            return true;
        }

        if (readInt(c, "thirdGuardHardBlock", 0) > 0) {
            return true;
        }

        if (readDouble(c, "falseStrongFreezeRisk", 0.0d) >= 0.780d) {
            return true;
        }

        return false;
    }

    private Models.Decision finishOnlyState(DbStore store, Models.Decision d, long startAt) {
        if (d != null
                && AppConfig.isPlayRuleLegal(d.position, AppConfig.FIXED_K, d.n1, d.n2, d.n3)) {
            PredictionContractEngine.stampDecision(null, d);
            if (AppConfig.DECISION_BET.equals(AppConfig.normalizeDecision(d.decision))) {
                if (!DecisionAuthorizationEngine.isDecisionAuthorized(d)) {
                    DecisionAuthorizationEngine.downgradeUnauthorized(
                            d, "AUTHORIZATION_MISSING_AT_FINAL_EXIT");
                    d.reasonSummary = appendReason(d.reasonSummary, "最终出口缺少有效授权签名，强制SHADOW");
                    d.reasonText = d.reasonSummary;
                }
            } else if (d.authorizationRejectCode == null
                    || d.authorizationRejectCode.length() == 0) {
                authorizationEngine.refuse(
                        store, d, d.blockCode == null ? "NOT_SELECTED_FOR_BET" : d.blockCode);
            }
            OosActionLogEngine.recordDecision(store, d);
        }
        saveDecisionStateOnly(store, d, startAt);
        return d;
    }

    private void saveDecisionStateOnly(DbStore store, Models.Decision d, long startAt) {
        if (store == null || d == null) {
            return;
        }

        long cost = System.currentTimeMillis() - startAt;

        applyDecisionScope(d);
        repairDecisionK3(d);

        try {
            store.setState("analysis_status", "OK");
            store.setState("analysis_progress", "100");
            store.setState("analysis_cost_ms", String.valueOf(cost));
            store.setState("analysis_target_issue", d.targetIssue == null ? "" : d.targetIssue);
            store.setState("analysis_last_decision", d.decision == null ? "" : d.decision);
            store.setState("analysis_last_summary", d.reasonSummary == null ? "" : d.reasonSummary);
            store.setState("analysis_summary", d.reasonSummary == null ? "" : d.reasonSummary);

            store.setState("decision_gate_status", "OK");
            store.setState("decision_gate_summary", d.reasonSummary == null ? "" : d.reasonSummary);
            store.setState("decision_gate_cost_ms", String.valueOf(cost));
            store.setState(
                    "decision_gate_frequency_policy",
                    "V40：频率只进入行动节奏效用；不得写回概率或改变校准；WATCH仅中性，显式风险才否决");
            store.setState(
                    "decision_gate_candidate_policy",
                    "最多40候选逐个检查；Contract+OOS校准+分层后验+Teacher/Dynamic/Adaptive强类型全部通过后才签发BET");

            store.setState("last_decision", d.decision == null ? "" : d.decision);
            store.setState("last_decision_target", d.targetIssue == null ? "" : d.targetIssue);
            store.setState(
                    "last_decision_target_issue", d.targetIssue == null ? "" : d.targetIssue);
            store.setState("last_decision_summary", d.reasonSummary == null ? "" : d.reasonSummary);
            store.setState("last_decision_position", String.valueOf(d.position));
            store.setState("last_decision_numbers", UiFormatter.numberGroup(d.n1, d.n2, d.n3));
            store.setState("last_decision_raw_p", String.valueOf(d.rawP));
            store.setState("last_decision_final_p", String.valueOf(d.finalP));
            store.setState("last_decision_gate", String.valueOf(d.dynamicGate));
            store.setState("last_decision_edge", String.valueOf(d.edge));
            store.setState("last_decision_block", d.blockCode == null ? "" : d.blockCode);
            store.setState("last_decision_scope", d.statScope == null ? "" : d.statScope);

            store.setState(
                    "last_decision_teacher_matrix_sample",
                    String.valueOf(readInt(d, "teacherMatrixSample", 0)));
            store.setState(
                    "last_decision_teacher_matrix_combo_sample",
                    String.valueOf(readInt(d, "teacherMatrixComboSample", 0)));
            store.setState(
                    "last_decision_teacher_matrix_hit_rate",
                    String.valueOf(readDouble(d, "teacherMatrixHitRate", 0.0d)));
            store.setState(
                    "last_decision_teacher_matrix_confidence",
                    String.valueOf(readDouble(d, "teacherMatrixConfidence", 0.0d)));
            store.setState(
                    "last_decision_teacher_matrix_status",
                    readString(d, "teacherMatrixStatus", ""));
            store.setState(
                    "last_decision_teacher_matrix_summary",
                    readString(d, "teacherMatrixSummary", ""));

            store.setState(
                    "last_decision_v20_cause_score",
                    String.valueOf(readDouble(d, "causeScore", 0.0d)));
            store.setState(
                    "last_decision_v20_rule_health",
                    String.valueOf(readDouble(d, "ruleHealthScore", 0.0d)));
            store.setState(
                    "last_decision_v20_third_guard",
                    String.valueOf(readDouble(d, "thirdGuardScore", 0.0d)));
            store.setState(
                    "last_decision_v20_false_freeze",
                    String.valueOf(readInt(d, "falseStrongFreeze", 0)));
            store.setState(
                    "last_decision_v20_counterfactual",
                    String.valueOf(readDouble(d, "counterfactualScore", 0.0d)));

            store.setState("analysis_updated_at", String.valueOf(System.currentTimeMillis()));
            store.setState("decision_updated_at", String.valueOf(System.currentTimeMillis()));
        } catch (Throwable e) {
        }
    }

    private void setGateRunningState(DbStore store, String targetIssue) {
        if (store == null) {
            return;
        }

        try {
            store.setState("analysis_status", "RUNNING");
            store.setState("analysis_target_issue", targetIssue == null ? "" : targetIssue);
            store.setState("analysis_progress", "82");
            store.setState(
                    "analysis_summary",
                    "最终决策门正在裁决目标期 "
                            + (targetIssue == null ? "" : targetIssue)
                            + "；V20_DYNAMIC_CAUSE_DECISION_GATE");

            store.setState("decision_gate_status", "RUNNING");
            store.setState(
                    "decision_gate_summary",
                    "最终门执行：FinalP/RawP/动态门/Edge/状态/硬风险/TeacherMatrix/V20因链风险");
            store.setState("decision_gate_frequency_policy", "频率只软调门，不硬拦；低频强信号允许救援，高频只要求更强");
            store.setState(
                    "decision_gate_candidate_policy", "最多40候选逐个检查；TeacherMatrix为空中性；V20因链不绕过最终门");
        } catch (Throwable e) {
        }
    }

    private void saveGateCostState(DbStore store, GateRuntimeContext runtime, long pickCost) {
        if (store == null || runtime == null) {
            return;
        }

        try {
            store.setState("decision_gate_pick_cost_ms", String.valueOf(pickCost));
            store.setState("decision_gate_context_cost_ms", String.valueOf(runtime.costContextMs));
            store.setState("decision_gate_freq_cost_ms", String.valueOf(runtime.costFreqMs));
            store.setState(
                    "decision_gate_open_trade_cost_ms", String.valueOf(runtime.costOpenTradeMs));
            store.setState(
                    "decision_gate_learning_state_cost_ms",
                    String.valueOf(runtime.costLearningStateMs));
            store.setState(
                    "decision_gate_fast_loop_count", String.valueOf(runtime.fastCheckedCount));
            store.setState(
                    "decision_gate_full_reason_count", String.valueOf(runtime.fullReasonCount));
            store.setState(
                    "decision_gate_full_check_count", String.valueOf(runtime.fullCheckCount));

            store.setState(
                    "decision_gate_cache_summary",
                    "freq100="
                            + UiFormatter.probability(runtime.freq100)
                            + " freq500="
                            + UiFormatter.probability(runtime.freq500)
                            + " freq1000="
                            + UiFormatter.probability(runtime.freq1000)
                            + " openTrade="
                            + runtime.openTradeExists
                            + " fullCheck="
                            + runtime.fullCheckCount
                            + " pickLimit="
                            + PICK_LIMIT
                            + " policy=V20_DYNAMIC_CAUSE_DECISION_GATE_TEACHER_NEUTRAL_IF_EMPTY");
        } catch (Throwable e) {
        }
    }

    private boolean canShadowObserve(
            Models.ComboCandidate candidate, String blockCode, List<Models.Draw> clean) {
        if (candidate == null || !candidate.isValidK3()) {
            return false;
        }

        if (clean == null || clean.size() < AppConfig.MIN_DRAWS_ANALYZE) {
            return false;
        }

        if (AppConfig.BLOCK_DATA_ERROR.equals(blockCode)) {
            return false;
        }

        if (candidate.targetIssue == null || candidate.targetIssue.length() == 0) {
            return false;
        }

        if (candidate.finalP <= 0.0d && candidate.rawP <= 0.0d) {
            return false;
        }

        return true;
    }

    private String shortPassReason(Models.ComboCandidate c) {
        if (c == null) {
            return "最终门通过";
        }

        return "最终门通过：P"
                + c.position
                + " "
                + UiFormatter.numberGroup(c.n1, c.n2, c.n3)
                + " FinalP="
                + UiFormatter.probability(c.finalP)
                + " Gate="
                + UiFormatter.probability(c.dynamicGate)
                + " Edge="
                + UiFormatter.signedProbability(c.edge)
                + " RawP="
                + UiFormatter.probability(c.rawP);
    }

    private String shortBlockReason(Models.ComboCandidate c, String blockCode) {
        StringBuilder sb = new StringBuilder();

        sb.append(
                UiFormatter.blockReasonText(
                        blockCode == null || blockCode.length() == 0
                                ? AppConfig.BLOCK_LOW_PROBABILITY
                                : blockCode));

        if (c != null) {
            sb.append("：P");
            sb.append(c.position);
            sb.append(" ");
            sb.append(UiFormatter.numberGroup(c.n1, c.n2, c.n3));
            sb.append(" FinalP=");
            sb.append(UiFormatter.probability(c.finalP));
            sb.append(" Gate=");
            sb.append(UiFormatter.probability(c.dynamicGate));
            sb.append(" Edge=");
            sb.append(UiFormatter.signedProbability(c.edge));
            sb.append(" RawP=");
            sb.append(UiFormatter.probability(c.rawP));
        }

        return sb.toString();
    }

    private void deny(FinalCheck check, String blockCode, String reason, boolean allowShadow) {
        if (check == null) {
            return;
        }

        check.allowBet = false;
        check.allowShadow = allowShadow;
        check.blockCode =
                blockCode == null || blockCode.length() == 0
                        ? AppConfig.BLOCK_LOW_PROBABILITY
                        : blockCode;

        String prefix = allowShadow ? "SHADOW观察：不满足真实BET，但保留候选用于开奖后反推学习；" : "HOLD：不满足真实BET；";

        check.reason = safeReason(prefix + (reason == null ? "" : reason));
    }

    private boolean isRealScope(String scope) {
        if (scope == null || scope.length() == 0) {
            return false;
        }

        if (AppConfig.STAT_SCOPE_REAL_TRADE.equals(scope)) {
            return true;
        }

        String s = scope.trim().toUpperCase(java.util.Locale.ROOT);

        return "REAL".equals(s)
                || "REAL_TRADE".equals(s)
                || "REALBET".equals(s)
                || "REAL_BET".equals(s);
    }

    private boolean isTerminalBadState(String mode) {
        if (mode == null) {
            return false;
        }

        return AppConfig.MODE_BROKEN.equals(mode)
                || AppConfig.MODE_REVERSING.equals(mode)
                || "REVERSAL".equals(mode);
    }

    private boolean isChaosOnly(String mode) {
        if (mode == null) {
            return false;
        }

        return AppConfig.MODE_CHAOS.equals(mode);
    }

    private String teacherMatrixBlockCode() {
        return readAppString("BLOCK_TEACHER_MATRIX_WEAK", "TEACHER_MATRIX_WEAK");
    }

    private String trendWeakBlockCode() {
        return readAppString("BLOCK_TREND_WEAK", "TREND_WEAK");
    }

    private double breakDepthHardBlock() {
        return readAppDouble("BREAK_DEPTH_HARD_BLOCK", AppConfig.BREAK_DEPTH_MAX + 18.0d);
    }

    private double trendLifeMin() {
        return readAppDouble("TREND_LIFE_MIN", 55.0d);
    }

    private int freqWindowShort() {
        return readAppInt("FREQ_WINDOW_SHORT", 100);
    }

    private int freqWindowMid() {
        return readAppInt("FREQ_WINDOW_MID", 500);
    }

    private int freqWindowLong() {
        return readAppInt("FREQ_WINDOW_LONG", 1000);
    }

    private double minRealBetProbability() {
        return readAppDouble(
                "MIN_REAL_BET_PROBABILITY", readAppDouble("MIN_REAL_BET_PROB", 0.345d));
    }

    private String bucketScore(double v) {
        if (v <= 0.0d) {
            return "0";
        }

        if (v < 30.0d) {
            return "L";
        }

        if (v < 55.0d) {
            return "M";
        }

        if (v < 75.0d) {
            return "H";
        }

        return "X";
    }

    private String bucketProb(double v) {
        if (v <= 0.0d) {
            return "0";
        }

        if (v < 0.340d) {
            return "L";
        }

        if (v < 0.375d) {
            return "S";
        }

        if (v < 0.400d) {
            return "B";
        }

        if (v < 0.430d) {
            return "H";
        }

        return "X";
    }

    private double cleanIncomingGate(double gate, double fallback) {
        if (fallback <= 0.0d || Double.isNaN(fallback) || Double.isInfinite(fallback)) {
            fallback = 0.385d;
        }

        if (gate <= 0.0d || Double.isNaN(gate) || Double.isInfinite(gate)) {
            return fallback;
        }

        if (gate > GATE_RISK_CAP) {
            return fallback;
        }

        if (gate < 0.330d) {
            return fallback;
        }

        return AppConfig.clampProbability(gate);
    }

    private String readAppString(String fieldName, String def) {
        if (fieldName == null || fieldName.length() == 0) {
            return def;
        }

        try {
            Field f = AppConfig.class.getField(fieldName);
            Object v = f.get(null);

            if (v != null) {
                return String.valueOf(v);
            }
        } catch (Throwable e) {
        }

        return def;
    }

    private int readAppInt(String fieldName, int def) {
        if (fieldName == null || fieldName.length() == 0) {
            return def;
        }

        try {
            Field f = AppConfig.class.getField(fieldName);
            Object v = f.get(null);

            if (v instanceof Number) {
                return ((Number) v).intValue();
            }

            if (v instanceof String) {
                return Integer.parseInt((String) v);
            }
        } catch (Throwable e) {
        }

        return def;
    }

    private double readAppDouble(String fieldName, double def) {
        if (fieldName == null || fieldName.length() == 0) {
            return def;
        }

        try {
            Field f = AppConfig.class.getField(fieldName);
            Object v = f.get(null);

            if (v instanceof Number) {
                double d = ((Number) v).doubleValue();

                if (!Double.isNaN(d) && !Double.isInfinite(d)) {
                    return d;
                }
            }

            if (v instanceof String) {
                double d2 = Double.parseDouble((String) v);

                if (!Double.isNaN(d2) && !Double.isInfinite(d2)) {
                    return d2;
                }
            }
        } catch (Throwable e) {
        }

        return def;
    }

    private int readInt(Object obj, String fieldName, int def) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return def;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            Object v = f.get(obj);

            if (v instanceof Number) {
                return ((Number) v).intValue();
            }

            if (v instanceof String) {
                return Integer.parseInt((String) v);
            }
        } catch (Throwable e) {
        }

        return def;
    }

    private double readDouble(Object obj, String fieldName, double def) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return def;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            Object v = f.get(obj);

            if (v instanceof Number) {
                double d = ((Number) v).doubleValue();

                if (Double.isNaN(d) || Double.isInfinite(d)) {
                    return def;
                }

                return d;
            }

            if (v instanceof String) {
                double d = Double.parseDouble((String) v);

                if (Double.isNaN(d) || Double.isInfinite(d)) {
                    return def;
                }

                return d;
            }
        } catch (Throwable e) {
        }

        return def;
    }

    private String readString(Object obj, String fieldName, String def) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return def;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            Object v = f.get(obj);

            if (v == null) {
                return def;
            }

            return String.valueOf(v);
        } catch (Throwable e) {
        }

        return def;
    }

    private void writeInt(Object obj, String fieldName, int value) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            f.set(obj, Integer.valueOf(value));
        } catch (Throwable e) {
        }
    }

    private void writeDouble(Object obj, String fieldName, double value) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            f.set(obj, Double.valueOf(value));
        } catch (Throwable e) {
        }
    }

    private void writeString(Object obj, String fieldName, String value) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            f.set(obj, value == null ? "" : value);
        } catch (Throwable e) {
        }
    }

    private String normalizeDecisionText(String decision, String action) {
        String d = decision == null || decision.length() == 0 ? "" : decision;
        String a = action == null || action.length() == 0 ? "" : action;

        String x = d.length() > 0 ? d : a;

        if (x == null || x.length() == 0) {
            return AppConfig.DECISION_HOLD;
        }

        x = x.trim().toUpperCase(java.util.Locale.ROOT);

        if ("BET".equals(x) || "REAL".equals(x) || "REAL_TRADE".equals(x) || "投注".equals(x)) {
            return AppConfig.DECISION_BET;
        }

        if ("SHADOW".equals(x)
                || "OBSERVE".equals(x)
                || "WATCH".equals(x)
                || "影子".equals(x)
                || "观察".equals(x)) {
            return AppConfig.DECISION_SHADOW;
        }

        return AppConfig.DECISION_HOLD;
    }

    private String normalizeResultText(String result) {
        if (result == null || result.length() == 0) {
            return AppConfig.RESULT_WAIT;
        }

        String r = result.trim();

        try {
            r = AppConfig.normalizeResult(r);
        } catch (Throwable e) {
            r = r.toUpperCase(java.util.Locale.ROOT);
        }

        if (AppConfig.RESULT_WIN.equals(r) || "HIT".equals(r) || "命中".equals(r)) {
            return AppConfig.RESULT_WIN;
        }

        if (AppConfig.RESULT_MISS.equals(r)
                || "LOSE".equals(r)
                || "LOSS".equals(r)
                || "未中".equals(r)
                || "不中".equals(r)) {
            return AppConfig.RESULT_MISS;
        }

        if (AppConfig.RESULT_DATA_ERROR.equals(r) || "DATA_ERROR".equals(r) || "数据异常".equals(r)) {
            return AppConfig.RESULT_DATA_ERROR;
        }

        if (AppConfig.RESULT_VOID.equals(r) || "VOID".equals(r) || "作废".equals(r)) {
            return AppConfig.RESULT_VOID;
        }

        if (AppConfig.RESULT_WAIT.equals(r)
                || "WAIT".equals(r)
                || "WAITING".equals(r)
                || "等待".equals(r)
                || "等待结算".equals(r)) {
            return AppConfig.RESULT_WAIT;
        }

        return r;
    }

    private void repairCandidateK3(Models.ComboCandidate c) {
        if (c == null) {
            return;
        }

        if (!Models.isValidK3Mask(c.k3Mask)
                && AppConfig.isValidNumber(c.n1)
                && AppConfig.isValidNumber(c.n2)
                && AppConfig.isValidNumber(c.n3)) {
            try {
                c.k3Mask = Models.buildK3Mask(c.n1, c.n2, c.n3);
            } catch (Throwable e) {
                c.k3Mask = buildK3MaskLocal(c.n1, c.n2, c.n3);
            }
        }
    }

    private void repairDecisionK3(Models.Decision d) {
        if (d == null) {
            return;
        }

        if (!Models.isValidK3Mask(d.k3Mask)
                && AppConfig.isValidNumber(d.n1)
                && AppConfig.isValidNumber(d.n2)
                && AppConfig.isValidNumber(d.n3)) {
            try {
                d.k3Mask = Models.buildK3Mask(d.n1, d.n2, d.n3);
            } catch (Throwable e) {
                d.k3Mask = buildK3MaskLocal(d.n1, d.n2, d.n3);
            }
        }
    }

    private int buildK3MaskLocal(int n1, int n2, int n3) {
        int mask = 0;

        if (AppConfig.isValidNumber(n1)) {
            mask |= (1 << n1);
        }

        if (AppConfig.isValidNumber(n2)) {
            mask |= (1 << n2);
        }

        if (AppConfig.isValidNumber(n3)) {
            mask |= (1 << n3);
        }

        return mask;
    }

    private String appendReason(String oldText, String add) {
        if (add == null || add.length() == 0) {
            return oldText == null ? "" : oldText;
        }

        if (oldText == null || oldText.length() == 0) {
            return add;
        }

        if (oldText.indexOf(add) >= 0) {
            return oldText;
        }

        return oldText + "；" + add;
    }

    private String safeReason(String text) {
        if (text == null) {
            return "";
        }

        if (text.length() <= MAX_REASON_LEN) {
            return text;
        }

        return text.substring(0, MAX_REASON_LEN);
    }

    private String shortText(String text, int max) {
        if (text == null) {
            return "";
        }

        if (max <= 0) {
            max = 120;
        }

        if (text.length() <= max) {
            return text;
        }

        return text.substring(0, max);
    }

    private double maxValue(double a, double b, double c) {
        double m = a;

        if (b > m) {
            m = b;
        }

        if (c > m) {
            m = c;
        }

        return clamp01(m);
    }

    private double maxValue(double a, double b, double c, double d, double e) {
        double m = maxValue(a, b, c, d);
        if (e > m) {
            m = e;
        }
        return clamp01(m);
    }

    private double maxValue(double a, double b, double c, double d) {
        double m = a;

        if (b > m) {
            m = b;
        }

        if (c > m) {
            m = c;
        }

        if (d > m) {
            m = d;
        }

        return clamp01(m);
    }

    private double clamp(double v, double min, double max) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return 0.0d;
        }
        if (v < min) {
            return min;
        }
        if (v > max) {
            return max;
        }
        return v;
    }

    private double clamp01(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return 0.0d;
        }

        if (v < 0.0d) {
            return 0.0d;
        }

        if (v > 1.0d) {
            return 1.0d;
        }

        return v;
    }

    private static final class GateRuntimeContext {
        public ArrayList<Models.Draw> clean;
        public String baseIssue;
        public String targetIssue;
        public long startAt;

        public boolean openTradeExists;
        public double freq50;
        public double freq100;
        public double freq300;
        public double freq500;
        public double freq1000;
        public Models.FrequencyState frequencyState;
        public double probeFrequency20;
        public double probeFrequency100;
        public int recentLoseCount6;

        public String learningStateText;
        public String learningReasonText;
        public String learningAdviceText;
        public String probabilityAuditStatus;
        public String moduleEffectStatus;
        public String moduleEffectSummary;
        public String shadowPromotionStatus;
        public String shadowPromotionSummary;
        public String dynamicFreezeStatus;
        public String dynamicFreezeSummary;
        public String adaptiveJudgeStatus;
        public String adaptiveJudgeSummary;
        public String diagnosisClosedLoopSummary;

        public long costContextMs;
        public long costOpenTradeMs;
        public long costFreqMs;
        public long costLearningStateMs;

        public int fastCheckedCount;
        public int fullReasonCount;
        public int fullCheckCount;
        public int preparedCandidateCount;

        public GateRuntimeContext() {
            clean = new ArrayList<Models.Draw>();
            baseIssue = "";
            targetIssue = "";
            startAt = 0L;

            openTradeExists = false;
            freq50 = 0.0d;
            freq100 = 0.0d;
            freq300 = 0.0d;
            freq500 = 0.0d;
            freq1000 = 0.0d;
            frequencyState = new Models.FrequencyState();
            probeFrequency20 = 0.0d;
            probeFrequency100 = 0.0d;
            recentLoseCount6 = 0;

            learningStateText = "";
            learningReasonText = "";
            learningAdviceText = "";
            probabilityAuditStatus = "";
            moduleEffectStatus = "";
            moduleEffectSummary = "";
            shadowPromotionStatus = "";
            shadowPromotionSummary = "";
            dynamicFreezeStatus = "";
            dynamicFreezeSummary = "";
            adaptiveJudgeStatus = "";
            adaptiveJudgeSummary = "";
            diagnosisClosedLoopSummary = "";

            costContextMs = 0L;
            costOpenTradeMs = 0L;
            costFreqMs = 0L;
            costLearningStateMs = 0L;

            fastCheckedCount = 0;
            fullReasonCount = 0;
            fullCheckCount = 0;
            preparedCandidateCount = 0;
        }
    }

    private static final class DecisionPick {
        public Models.ComboCandidate candidate;
        public FinalCheck check;
        public double score;

        public DecisionPick() {
            candidate = null;
            check = null;
            score = -999.0d;
        }
    }

    private V20DecisionEvidence buildV20DecisionEvidence(
            Models.ComboCandidate c, List<Models.Draw> history, GateRuntimeContext runtime) {
        V20DecisionEvidence e = new V20DecisionEvidence();

        if (c == null || !c.isValidK3()) {
            e.stateKey = "DECISION_INVALID";
            e.actionAdvice = "HOLD：候选为空或K3非法";
            e.evidenceText = "最终裁决证据矩阵：候选非法";
            e.counterEvidenceText = "反证：候选数据不可用";
            e.hardRisk = true;
            return e;
        }

        V20DecisionWindowStat w12 = calcDecisionWindowStat(c, history, DECISION_WINDOW_12);
        V20DecisionWindowStat w24 = calcDecisionWindowStat(c, history, DECISION_WINDOW_24);
        V20DecisionWindowStat w48 = calcDecisionWindowStat(c, history, DECISION_WINDOW_48);
        V20DecisionWindowStat w96 = calcDecisionWindowStat(c, history, DECISION_WINDOW_96);
        V20DecisionWindowStat w360 = calcDecisionWindowStat(c, history, DECISION_WINDOW_360);

        double teacherScore =
                readDouble(
                        c,
                        "teacherMatrixScore",
                        readDouble(c, "teacherNumberScore", readDouble(c, "teacherScore", 0.0d)));
        double teacherRisk =
                readDouble(
                        c,
                        "teacherMatrixRisk",
                        readDouble(c, "teacherNumberRisk", readDouble(c, "teacherRisk", 0.0d)));
        double teacherConfidence =
                readDouble(
                        c,
                        "teacherMatrixConfidence",
                        readDouble(
                                c,
                                "teacherNumberConfidence",
                                readDouble(c, "teacherConfidence", 0.0d)));
        int teacherSample =
                readInt(
                        c,
                        "teacherMatrixSample",
                        readInt(c, "teacherNumberSample", readInt(c, "teacherSample", 0)));

        double dynamicScore =
                readDouble(
                        c,
                        "dynamicCauseScore",
                        readDouble(c, "dynamicCauseChainScore", readDouble(c, "causeScore", 0.0d)));
        double dynamicRisk =
                readDouble(
                        c,
                        "dynamicRiskScore",
                        readDouble(c, "dynamicCauseChainRisk", readDouble(c, "riskScore", 0.0d)));
        double gateEvidence = readDouble(c, "dynamicGateEvidenceContribution", 0.0d);
        double gateCounter = readDouble(c, "dynamicGateCounterContribution", 0.0d);
        double probabilityEvidence = readDouble(c, "probabilityEvidenceContribution", 0.0d);
        double probabilityCounter = readDouble(c, "probabilityCounterContribution", 0.0d);

        double windowSupport =
                clamp01(
                        w12.hitRate * 0.22d
                                + w24.hitRate * 0.24d
                                + w48.hitRate * 0.22d
                                + w96.hitRate * 0.18d
                                + w360.hitRate * 0.14d);
        double recentSlope =
                clampSigned(
                        (w12.hitRate - w48.hitRate) * 1.20d + (w24.hitRate - w96.hitRate) * 0.60d);
        double windowRisk =
                clamp01(
                        w12.missPressure * 0.28d
                                + w24.missPressure * 0.24d
                                + w48.missPressure * 0.20d
                                + w96.missPressure * 0.16d
                                + w360.missPressure * 0.12d);

        double pEvidence = clamp01((c.finalP - STRONG_MIN_P) * 2.80d);
        double edgeEvidence = clamp01((c.edge + 0.006d) * 18.0d);
        double rawEvidence = clamp01((c.rawP - RAW_FLOOR_STRONG) * 5.0d);
        double shapeEvidence =
                clamp01(
                        (c.trendLife / 100.0d) * 0.20d
                                + (c.recoveryScore / 100.0d) * 0.18d
                                + c.profileScore * 0.16d
                                + c.evidenceScore * 0.18d
                                + c.aPlusScore * 0.10d
                                + c.promotionScore * 0.08d
                                + windowSupport * 0.10d);

        double teacherEvidence =
                clamp01(
                        teacherScore * 0.58d
                                + teacherConfidence * 0.22d
                                + sampleConfidenceV20(teacherSample) * 0.20d);
        double dynamicEvidence =
                clamp01(dynamicScore * 0.68d + gateEvidence * 0.16d + probabilityEvidence * 0.16d);
        double sampleReliability =
                clamp01(
                        sampleConfidenceV20(c.effectiveSample) * 0.30d
                                + sampleConfidenceV20(c.comboSample) * 0.24d
                                + sampleConfidenceV20(teacherSample) * 0.18d
                                + sampleConfidenceV20(w96.sample) * 0.16d
                                + sampleConfidenceV20(w360.sample) * 0.12d);
        double recentValidity =
                clamp01(
                        w12.hitRate * 0.26d
                                + w24.hitRate * 0.26d
                                + w48.hitRate * 0.20d
                                + clamp01(0.50d + recentSlope) * 0.16d
                                + (1.0d - windowRisk) * 0.12d);

        double riskEvidence =
                clamp01(
                        (c.evidenceRisk * 0.22d)
                                + Math.max(0.0d, c.driftRisk - AppConfig.DRIFT_RISK_MAX)
                                        / 100.0d
                                        * 0.18d
                                + Math.max(0.0d, c.breakDepth - AppConfig.BREAK_DEPTH_MAX)
                                        / 100.0d
                                        * 0.18d
                                + teacherRisk * 0.12d
                                + dynamicRisk * 0.14d
                                + windowRisk * 0.10d
                                + gateCounter * 0.03d
                                + probabilityCounter * 0.03d);
        if (readInt(c, "falseStrongHardFreeze", 0) == 1 || readInt(c, "dynamicHardRisk", 0) == 1) {
            riskEvidence = Math.max(riskEvidence, DECISION_COUNTER_RISK_HARD);
        }
        if (readInt(c, "falseStrongFreeze", 0) == 1) {
            riskEvidence = Math.max(riskEvidence, 0.680d);
        }

        double evidence =
                clamp01(
                        pEvidence * 0.20d
                                + edgeEvidence * 0.12d
                                + rawEvidence * 0.10d
                                + shapeEvidence * 0.16d
                                + teacherEvidence * 0.12d
                                + dynamicEvidence * 0.14d
                                + sampleReliability * 0.08d
                                + recentValidity * 0.08d);

        double counter = clamp01(riskEvidence);
        double featureDistance =
                clamp01(
                        Math.abs(0.385d - c.finalP) * 0.65d
                                + Math.abs(0.0d - c.edge) * 8.0d
                                + Math.abs(0.30d - windowSupport) * 0.35d
                                + Math.abs(teacherEvidence - dynamicEvidence) * 0.22d
                                + riskEvidence * 0.20d);

        e.stateKey =
                buildDecisionWindowStateKey(c, w12, w24, w48, w96, w360, recentSlope, windowRisk);
        e.evidenceContribution = evidence;
        e.counterContribution = counter;
        e.sampleReliability = sampleReliability;
        e.recentValidity = recentValidity;
        e.featureDistance = featureDistance;
        e.ready = evidence >= DECISION_EVIDENCE_READY && counter < DECISION_COUNTER_RISK_HIGH;
        e.strong =
                evidence >= DECISION_EVIDENCE_STRONG
                        && counter < 0.560d
                        && sampleReliability >= 0.34d;
        e.elite =
                evidence >= DECISION_EVIDENCE_ELITE
                        && counter < 0.500d
                        && sampleReliability >= 0.48d
                        && recentValidity >= 0.38d;
        e.riskHigh = counter >= DECISION_COUNTER_RISK_HIGH;
        e.hardRisk = counter >= DECISION_COUNTER_RISK_HARD;

        if (e.hardRisk) {
            e.actionAdvice = "HOLD：最终裁决反证为硬风险，禁止真实BET";
        } else if (e.elite) {
            e.actionAdvice = "BET候选：证据矩阵Elite，允许进入最终门最后校验";
        } else if (e.strong) {
            e.actionAdvice = "BET候选：证据强于反证，允许进入最终门判断";
        } else if (e.ready) {
            e.actionAdvice = "SHADOW/BET边缘：证据可用但需FinalP、Edge、动态门共同确认";
        } else if (e.riskHigh) {
            e.actionAdvice = "SHADOW：反证偏高，真实BET需更高FinalP/Edge对冲";
        } else {
            e.actionAdvice = "HOLD/观察：证据不足，继续积累教师矩阵与因链样本";
        }

        e.evidenceText =
                "证据贡献="
                        + UiFormatter.probability(e.evidenceContribution)
                        + "，FinalP证据="
                        + UiFormatter.probability(pEvidence)
                        + "，Edge证据="
                        + UiFormatter.probability(edgeEvidence)
                        + "，窗口支撑="
                        + UiFormatter.probability(windowSupport)
                        + "，教师支撑="
                        + UiFormatter.probability(teacherEvidence)
                        + "，因链支撑="
                        + UiFormatter.probability(dynamicEvidence)
                        + "，样本可信="
                        + UiFormatter.probability(e.sampleReliability)
                        + "，近期有效="
                        + UiFormatter.probability(e.recentValidity);

        e.counterEvidenceText =
                "反证贡献="
                        + UiFormatter.probability(e.counterContribution)
                        + "，窗口压力="
                        + UiFormatter.probability(windowRisk)
                        + "，证据风险="
                        + UiFormatter.probability(c.evidenceRisk)
                        + "，教师风险="
                        + UiFormatter.probability(teacherRisk)
                        + "，因链风险="
                        + UiFormatter.probability(dynamicRisk)
                        + "，特征距离="
                        + UiFormatter.probability(e.featureDistance);

        return e;
    }

    private void applyV20DecisionEvidenceToCandidate(
            Models.ComboCandidate c, V20DecisionEvidence e) {
        if (c == null || e == null) {
            return;
        }

        writeString(c, "decisionWindowStateKey", e.stateKey);
        writeString(c, "finalDecisionWindowStateKey", e.stateKey);
        writeString(c, "decisionEvidenceText", e.evidenceText);
        writeString(c, "decisionCounterEvidenceText", e.counterEvidenceText);
        writeString(c, "decisionActionAdvice", e.actionAdvice);

        writeDouble(c, "decisionEvidenceContribution", e.evidenceContribution);
        writeDouble(c, "decisionCounterContribution", e.counterContribution);
        writeDouble(c, "decisionSampleReliability", e.sampleReliability);
        writeDouble(c, "decisionRecentValidity", e.recentValidity);
        writeDouble(c, "decisionFeatureDistance", e.featureDistance);

        writeInt(c, "decisionEvidenceReady", e.ready ? 1 : 0);
        writeInt(c, "decisionEvidenceStrong", e.strong ? 1 : 0);
        writeInt(c, "decisionEvidenceElite", e.elite ? 1 : 0);
        writeInt(c, "decisionCounterRiskHigh", e.riskHigh ? 1 : 0);
        writeInt(c, "decisionHardRisk", e.hardRisk ? 1 : 0);
    }

    private void applyV20DecisionEvidenceToDecision(Models.Decision d, Models.ComboCandidate c) {
        if (d == null || c == null) {
            return;
        }

        writeString(d, "decisionWindowStateKey", readString(c, "decisionWindowStateKey", ""));
        writeString(
                d,
                "finalDecisionWindowStateKey",
                readString(
                        c,
                        "finalDecisionWindowStateKey",
                        readString(c, "decisionWindowStateKey", "")));
        writeString(d, "decisionEvidenceText", readString(c, "decisionEvidenceText", ""));
        writeString(
                d, "decisionCounterEvidenceText", readString(c, "decisionCounterEvidenceText", ""));
        writeString(d, "decisionActionAdvice", readString(c, "decisionActionAdvice", ""));

        writeDouble(
                d,
                "decisionEvidenceContribution",
                readDouble(c, "decisionEvidenceContribution", 0.0d));
        writeDouble(
                d,
                "decisionCounterContribution",
                readDouble(c, "decisionCounterContribution", 0.0d));
        writeDouble(
                d, "decisionSampleReliability", readDouble(c, "decisionSampleReliability", 0.0d));
        writeDouble(d, "decisionRecentValidity", readDouble(c, "decisionRecentValidity", 0.0d));
        writeDouble(d, "decisionFeatureDistance", readDouble(c, "decisionFeatureDistance", 0.0d));

        writeInt(d, "decisionEvidenceReady", readInt(c, "decisionEvidenceReady", 0));
        writeInt(d, "decisionEvidenceStrong", readInt(c, "decisionEvidenceStrong", 0));
        writeInt(d, "decisionEvidenceElite", readInt(c, "decisionEvidenceElite", 0));
        writeInt(d, "decisionCounterRiskHigh", readInt(c, "decisionCounterRiskHigh", 0));
        writeInt(d, "decisionHardRisk", readInt(c, "decisionHardRisk", 0));
    }

    private void fillDecisionEvidenceContext(DecisionContext ctx, V20DecisionEvidence e) {
        if (ctx == null || e == null) {
            return;
        }

        ctx.decisionWindowStateKey = e.stateKey;
        ctx.decisionEvidenceText = e.evidenceText;
        ctx.decisionCounterEvidenceText = e.counterEvidenceText;
        ctx.decisionActionAdvice = e.actionAdvice;
        ctx.decisionEvidenceContribution = e.evidenceContribution;
        ctx.decisionCounterContribution = e.counterContribution;
        ctx.decisionSampleReliability = e.sampleReliability;
        ctx.decisionRecentValidity = e.recentValidity;
        ctx.decisionFeatureDistance = e.featureDistance;
        ctx.decisionEvidenceReady = e.ready;
        ctx.decisionEvidenceStrong = e.strong;
        ctx.decisionEvidenceElite = e.elite;
        ctx.decisionCounterRiskHigh = e.riskHigh;
        ctx.decisionHardRisk = e.hardRisk;
    }

    private String buildDecisionEvidenceReason(DecisionContext ctx) {
        if (ctx == null
                || ctx.decisionWindowStateKey == null
                || ctx.decisionWindowStateKey.length() == 0) {
            return "最终裁决证据矩阵=未生成，按中性处理";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("最终裁决证据矩阵=");
        sb.append(ctx.decisionWindowStateKey);
        sb.append("，证据=");
        sb.append(UiFormatter.probability(ctx.decisionEvidenceContribution));
        sb.append("，反证=");
        sb.append(UiFormatter.probability(ctx.decisionCounterContribution));
        sb.append("，样本可信=");
        sb.append(UiFormatter.probability(ctx.decisionSampleReliability));
        sb.append("，近期有效=");
        sb.append(UiFormatter.probability(ctx.decisionRecentValidity));
        sb.append("，特征距离=");
        sb.append(UiFormatter.probability(ctx.decisionFeatureDistance));
        sb.append("，ready=");
        sb.append(ctx.decisionEvidenceReady);
        sb.append("，strong=");
        sb.append(ctx.decisionEvidenceStrong);
        sb.append("，elite=");
        sb.append(ctx.decisionEvidenceElite);
        sb.append("，riskHigh=");
        sb.append(ctx.decisionCounterRiskHigh);
        sb.append("，hard=");
        sb.append(ctx.decisionHardRisk);
        if (ctx.decisionEvidenceText != null && ctx.decisionEvidenceText.length() > 0) {
            sb.append("；");
            sb.append(ctx.decisionEvidenceText);
        }
        if (ctx.decisionCounterEvidenceText != null
                && ctx.decisionCounterEvidenceText.length() > 0) {
            sb.append("；");
            sb.append(ctx.decisionCounterEvidenceText);
        }
        if (ctx.decisionActionAdvice != null && ctx.decisionActionAdvice.length() > 0) {
            sb.append("；建议=");
            sb.append(ctx.decisionActionAdvice);
        }
        return sb.toString();
    }

    private V20DecisionWindowStat calcDecisionWindowStat(
            Models.ComboCandidate c, List<Models.Draw> history, int window) {
        V20DecisionWindowStat s = new V20DecisionWindowStat();
        s.window = window;
        if (c == null
                || history == null
                || history.size() == 0
                || !AppConfig.isValidPosition(c.position)) {
            return s;
        }

        int start = history.size() - window;
        if (start < 0) {
            start = 0;
        }

        int lastHitAge = -1;
        for (int i = start; i < history.size(); i++) {
            Models.Draw d = history.get(i);
            if (d == null || !d.isValid()) {
                continue;
            }

            int n = 0;
            try {
                n = d.getNumberAtPosition(c.position);
            } catch (Throwable ex) {
                n = 0;
            }

            if (!AppConfig.isValidNumber(n)) {
                continue;
            }

            s.sample++;

            boolean hit = decisionContainsNumber(c, n);
            if (hit) {
                s.hit++;
                lastHitAge = history.size() - 1 - i;
            } else {
                s.miss++;
            }

            if (n % 2 == 0) {
                s.even++;
            } else {
                s.odd++;
            }

            if (n <= 5) {
                s.small++;
            } else {
                s.big++;
            }

            if (n == c.n1 || n == c.n2 || n == c.n3) {
                s.sameNumber++;
            }

            if (Math.abs(n - c.n1) == 1 || Math.abs(n - c.n2) == 1 || Math.abs(n - c.n3) == 1) {
                s.neighbor++;
            }

            if ((n % 10) == (c.n1 % 10) || (n % 10) == (c.n2 % 10) || (n % 10) == (c.n3 % 10)) {
                s.sameTail++;
            }
        }

        if (s.sample > 0) {
            s.hitRate = ((double) s.hit) / ((double) s.sample);
            s.missPressure = ((double) s.miss) / ((double) s.sample);
            s.parityBias = Math.abs(((double) s.odd - (double) s.even) / ((double) s.sample));
            s.sizeBias = Math.abs(((double) s.big - (double) s.small) / ((double) s.sample));
            s.density =
                    ((double) s.sameNumber + (double) s.neighbor + (double) s.sameTail)
                            / ((double) s.sample * 3.0d);
        }
        s.lastHitAge = lastHitAge < 0 ? 999 : lastHitAge;
        return s;
    }

    private String buildDecisionWindowStateKey(
            Models.ComboCandidate c,
            V20DecisionWindowStat w12,
            V20DecisionWindowStat w24,
            V20DecisionWindowStat w48,
            V20DecisionWindowStat w96,
            V20DecisionWindowStat w360,
            double recentSlope,
            double windowRisk) {
        StringBuilder sb = new StringBuilder();
        sb.append("DEC_P");
        sb.append(c == null ? 0 : c.position);
        sb.append("_K3_");
        sb.append(c == null ? 0 : c.k3Mask);
        sb.append("|W12=");
        sb.append(bucketRate("H", w12 == null ? 0.0d : w12.hitRate));
        sb.append("|W24=");
        sb.append(bucketRate("H", w24 == null ? 0.0d : w24.hitRate));
        sb.append("|W48=");
        sb.append(bucketRate("H", w48 == null ? 0.0d : w48.hitRate));
        sb.append("|W96=");
        sb.append(bucketRate("H", w96 == null ? 0.0d : w96.hitRate));
        sb.append("|W360=");
        sb.append(bucketRate("H", w360 == null ? 0.0d : w360.hitRate));
        sb.append("|SLOPE=");
        sb.append(bucketSigned("S", recentSlope));
        sb.append("|MISS=");
        sb.append(bucketRate("M", windowRisk));
        sb.append("|PAR=");
        sb.append(bucketRate("P", w24 == null ? 0.0d : w24.parityBias));
        sb.append("|SIZE=");
        sb.append(bucketRate("B", w24 == null ? 0.0d : w24.sizeBias));
        sb.append("|DENS=");
        sb.append(bucketRate("D", w24 == null ? 0.0d : w24.density));
        return sb.toString();
    }

    private boolean decisionContainsNumber(Models.ComboCandidate c, int n) {
        if (c == null || !AppConfig.isValidNumber(n)) {
            return false;
        }
        if (n == c.n1 || n == c.n2 || n == c.n3) {
            return true;
        }
        int mask = c.k3Mask;
        if (mask <= 0) {
            mask = buildK3MaskLocal(c.n1, c.n2, c.n3);
        }
        return (mask & (1 << n)) != 0;
    }

    private double sampleConfidenceV20(int sample) {
        if (sample <= 0) {
            return 0.0d;
        }
        if (sample >= 180) {
            return 1.0d;
        }
        return clamp01(((double) sample) / 180.0d);
    }

    private double clampSigned(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return 0.0d;
        }
        if (v < -1.0d) {
            return -1.0d;
        }
        if (v > 1.0d) {
            return 1.0d;
        }
        return v;
    }

    private String bucketRate(String prefix, double v) {
        v = clamp01(v);
        String p = prefix == null ? "V" : prefix;
        if (v >= 0.700d) {
            return p + "70";
        }
        if (v >= 0.620d) {
            return p + "62";
        }
        if (v >= 0.540d) {
            return p + "54";
        }
        if (v >= 0.460d) {
            return p + "46";
        }
        if (v >= 0.380d) {
            return p + "38";
        }
        if (v >= 0.300d) {
            return p + "30";
        }
        return p + "LOW";
    }

    private String bucketSigned(String prefix, double v) {
        String p = prefix == null ? "S" : prefix;
        if (v >= 0.180d) {
            return p + "_UP2";
        }
        if (v >= 0.060d) {
            return p + "_UP1";
        }
        if (v <= -0.180d) {
            return p + "_DOWN2";
        }
        if (v <= -0.060d) {
            return p + "_DOWN1";
        }
        return p + "_FLAT";
    }

    private static final class V20DecisionEvidence {
        public String stateKey;
        public double evidenceContribution;
        public double counterContribution;
        public double sampleReliability;
        public double recentValidity;
        public double featureDistance;
        public String evidenceText;
        public String counterEvidenceText;
        public String actionAdvice;
        public boolean ready;
        public boolean strong;
        public boolean elite;
        public boolean riskHigh;
        public boolean hardRisk;

        public V20DecisionEvidence() {
            stateKey = "";
            evidenceContribution = 0.0d;
            counterContribution = 0.0d;
            sampleReliability = 0.0d;
            recentValidity = 0.0d;
            featureDistance = 1.0d;
            evidenceText = "";
            counterEvidenceText = "";
            actionAdvice = "";
            ready = false;
            strong = false;
            elite = false;
            riskHigh = false;
            hardRisk = false;
        }
    }

    private static final class V20DecisionWindowStat {
        public int window;
        public int sample;
        public int hit;
        public int miss;
        public int odd;
        public int even;
        public int small;
        public int big;
        public int sameNumber;
        public int neighbor;
        public int sameTail;
        public int lastHitAge;
        public double hitRate;
        public double missPressure;
        public double parityBias;
        public double sizeBias;
        public double density;

        public V20DecisionWindowStat() {
            window = 0;
            sample = 0;
            hit = 0;
            miss = 0;
            odd = 0;
            even = 0;
            small = 0;
            big = 0;
            sameNumber = 0;
            neighbor = 0;
            sameTail = 0;
            lastHitAge = 999;
            hitRate = 0.0d;
            missPressure = 0.0d;
            parityBias = 0.0d;
            sizeBias = 0.0d;
            density = 0.0d;
        }
    }

    private static final class DecisionContext {
        public String stateKey;
        public String numberShape;
        public String actualPredictionLogic;
        public String nextAdvice;

        public boolean learningStrong;
        public boolean learningWeak;
        public boolean evidenceStrong;
        public boolean realRiskHigh;

        public String learningStateText;
        public String learningReasonText;
        public String learningAdviceText;

        public int teacherMatrixApplied;
        public int teacherMatrixSample;
        public int teacherMatrixComboSample;
        public double teacherMatrixHitRate;
        public double teacherMatrixComboHitRate;
        public double teacherMatrixConfidence;
        public double teacherMatrixGateAdjust;
        public double teacherMatrixRiskPenalty;
        public String teacherMatrixStatus;
        public String teacherMatrixSummary;
        public boolean teacherMatrixStrong;
        public boolean teacherMatrixReady;
        public boolean teacherMatrixRisk;
        public boolean teacherMatrixFreeze;
        public boolean teacherNoSampleNeutral;
        public double teacherEvidenceContribution;
        public double teacherCounterContribution;
        public double teacherSampleReliability;
        public double teacherRecentValidity;
        public String teacherActionAdvice;

        public double v20CauseScore;
        public double v20CauseRisk;

        public double ruleHealthScore;
        public double ruleHealthRisk;
        public int ruleHealthPass;
        public int ruleHealthHardCount;
        public String ruleHealthSummary;

        public double thirdGuardScore;
        public double thirdGuardRisk;
        public int thirdGuardPass;
        public int thirdGuardHard;
        public String thirdGuardSummary;

        public double shadowHealthScore;
        public double shadowHealthRisk;
        public int shadowHealthPass;
        public int shadowPromoteEligible;
        public String shadowHealthSummary;

        public double falseStrongFreezeRisk;
        public int falseStrongFreeze;
        public int falseStrongHardFreeze;
        public String falseStrongFreezeLevel;
        public String falseStrongFreezeSummary;

        public double counterfactualScore;
        public double counterfactualRisk;
        public String counterfactualStatus;
        public String counterfactualSummary;

        public double teacherNumberScore;
        public double teacherNumberRisk;
        public double teacherNumberConfidence;
        public String teacherNumberStatus;
        public String teacherNumberSummary;

        public double positionHealthScore;
        public double positionWrongRisk;
        public String positionHealthStatus;
        public String positionHealthSummary;

        public double seedCauseScore;
        public double seedCauseRisk;
        public String seedCauseSummary;

        public double dynamicCauseChainScore;
        public double dynamicCauseChainRisk;
        public String dynamicCauseChainSummary;

        public double v20SupportScore;
        public double v20RiskScore;
        public double v20SampleReliability;
        public double v20RecentValidity;
        public String v20ActionAdvice;
        public boolean v20Ready;
        public boolean v20Strong;
        public boolean v20Elite;
        public boolean v20RiskHigh;
        public boolean v20HardRisk;
        public boolean v20WatchNeutral;
        public String v20HardBlockCode;

        public String decisionWindowStateKey;
        public double decisionEvidenceContribution;
        public double decisionCounterContribution;
        public double decisionSampleReliability;
        public double decisionRecentValidity;
        public double decisionFeatureDistance;
        public String decisionEvidenceText;
        public String decisionCounterEvidenceText;
        public String decisionActionAdvice;
        public boolean decisionEvidenceReady;
        public boolean decisionEvidenceStrong;
        public boolean decisionEvidenceElite;
        public boolean decisionCounterRiskHigh;
        public boolean decisionHardRisk;

        public double finalPBeforeDiagnosis;
        public double finalPAfterDiagnosis;
        public double diagnosisAdjustedP;
        public double moduleAdjustedP;
        public double freezeAdjustedP;
        public double shadowPromotionBoost;
        public double probabilityEvidenceContribution;
        public double probabilityCounterEvidence;
        public double probabilitySampleReliability;
        public double probabilityRecentValidity;
        public String probabilityActionAdvice;
        public String probabilityDiagnosisSummary;

        public double qualityGateAdjust;
        public double diagnosisGateAdjust;
        public double moduleEffectGateAdjust;
        public double freezeGateAdjust;
        public double shadowPromotionGateAdjust;
        public String dynamicGateQualitySummary;
        public String dynamicGateFrequencyPolicy;
        public String dynamicGateActionAdvice;

        public String moduleEffectStatus;
        public String moduleEffectSummary;
        public String shadowPromotionStatus;
        public String shadowPromotionSummary;
        public String dynamicFreezeStatus;
        public String dynamicFreezeSummary;
        public String adaptiveJudgeStatus;
        public String adaptiveJudgeSummary;
        public String diagnosisClosedLoopSummary;

        public boolean moduleMisleading;
        public boolean shadowPromotionStrong;
        public boolean freezeMatrixRisk;
        public boolean adaptiveFreeze;
        public boolean adaptiveReject;
        public boolean adaptivePass;
        public boolean adaptiveWatchNeutral;
        public boolean finalDiagnosisRiskHigh;
        public boolean finalDecisionReady;
        public boolean finalDecisionStrong;
        public boolean finalDecisionFreeze;
        public boolean finalDecisionHardRisk;
        public double finalDecisionQualityScore;
        public double finalDecisionRiskScore;
        public String finalDecisionStatus;
        public String finalDecisionActionAdvice;

        public String dynamicClusterId;
        public String dynamicClusterStatus;
        public String dynamicClusterPrototypeKey;
        public int dynamicClusterSample;
        public int dynamicClusterRecentSample;
        public double dynamicClusterHitRate;
        public double dynamicClusterRecentHitRate;
        public double dynamicClusterConfidence;
        public double dynamicClusterDistance;
        public int dynamicClusterMaxLose;
        public double dynamicClusterCounterScore;
        public String dynamicClusterReason;
        public boolean dynamicClusterReady;
        public boolean dynamicClusterStrong;
        public boolean dynamicClusterRisk;

        public double needSupplyMatchScore;
        public double needSupplyThirdQuality;
        public double needSupplyRoleDiversity;
        public double needSupplyRisk;
        public String needSupplySummary;
        public boolean needSupplyReady;
        public boolean needSupplyRiskHigh;

        public int counterfactualRank;
        public double counterfactualRankScore;
        public double counterfactualWhyNotRisk;
        public double causeAblationScore;
        public int causeAblationFalseCauseCount;
        public boolean counterfactualReady;
        public boolean counterfactualRiskHigh;

        public String adaptiveCandidateStatus;
        public String adaptiveCandidateAction;
        public double adaptiveCauseScore;
        public double adaptiveCounterScore;
        public String adaptiveCandidateReason;
        public boolean adaptiveCandidatePass;
        public boolean adaptiveCandidateFreeze;

        public String promotionClusterStatus;
        public String promotionAction;
        public double promotionClusterHitRate;
        public double promotionClusterRecentHitRate;
        public int promotionClusterSample;
        public String promotionReason;
        public boolean promotionReady;
        public boolean promotionFreeze;

        public boolean dynamicCausalFieldsPresent;
        public boolean dynamicCausalReady;
        public boolean dynamicCausalStrong;
        public boolean dynamicCausalRiskHigh;
        public boolean dynamicCausalFreeze;
        public boolean dynamicCausalHardRisk;
        public double dynamicCausalQualityScore;
        public double dynamicCausalRiskScore;
        public String dynamicCausalSummary;
        public String dynamicCausalActionAdvice;

        public DecisionContext() {
            stateKey = "";
            numberShape = "";
            actualPredictionLogic = "";
            nextAdvice = "";

            learningStrong = false;
            learningWeak = false;
            evidenceStrong = false;
            realRiskHigh = false;

            learningStateText = "";
            learningReasonText = "";
            learningAdviceText = "";

            teacherMatrixApplied = 0;
            teacherMatrixSample = 0;
            teacherMatrixComboSample = 0;
            teacherMatrixHitRate = 0.0d;
            teacherMatrixComboHitRate = 0.0d;
            teacherMatrixConfidence = 0.0d;
            teacherMatrixGateAdjust = 0.0d;
            teacherMatrixRiskPenalty = 0.0d;
            teacherMatrixStatus = "";
            teacherMatrixSummary = "";
            teacherMatrixStrong = false;
            teacherMatrixReady = false;
            teacherMatrixRisk = false;
            teacherMatrixFreeze = false;
            teacherNoSampleNeutral = false;
            teacherEvidenceContribution = 0.0d;
            teacherCounterContribution = 0.0d;
            teacherSampleReliability = 0.0d;
            teacherRecentValidity = 0.0d;
            teacherActionAdvice = "";

            v20CauseScore = 0.0d;
            v20CauseRisk = 0.0d;

            ruleHealthScore = 0.0d;
            ruleHealthRisk = 0.0d;
            ruleHealthPass = 0;
            ruleHealthHardCount = 0;
            ruleHealthSummary = "";

            thirdGuardScore = 0.0d;
            thirdGuardRisk = 0.0d;
            thirdGuardPass = 0;
            thirdGuardHard = 0;
            thirdGuardSummary = "";

            shadowHealthScore = 0.0d;
            shadowHealthRisk = 0.0d;
            shadowHealthPass = 0;
            shadowPromoteEligible = 0;
            shadowHealthSummary = "";

            falseStrongFreezeRisk = 0.0d;
            falseStrongFreeze = 0;
            falseStrongHardFreeze = 0;
            falseStrongFreezeLevel = "";
            falseStrongFreezeSummary = "";

            counterfactualScore = 0.0d;
            counterfactualRisk = 0.0d;
            counterfactualStatus = "";
            counterfactualSummary = "";

            teacherNumberScore = 0.0d;
            teacherNumberRisk = 0.0d;
            teacherNumberConfidence = 0.0d;
            teacherNumberStatus = "";
            teacherNumberSummary = "";

            positionHealthScore = 0.0d;
            positionWrongRisk = 0.0d;
            positionHealthStatus = "";
            positionHealthSummary = "";

            seedCauseScore = 0.0d;
            seedCauseRisk = 0.0d;
            seedCauseSummary = "";

            dynamicCauseChainScore = 0.0d;
            dynamicCauseChainRisk = 0.0d;
            dynamicCauseChainSummary = "";

            v20SupportScore = 0.0d;
            v20RiskScore = 0.0d;
            v20SampleReliability = 0.0d;
            v20RecentValidity = 0.0d;
            v20ActionAdvice = "";
            v20Ready = false;
            v20Strong = false;
            v20Elite = false;
            v20RiskHigh = false;
            v20HardRisk = false;
            v20WatchNeutral = false;
            v20HardBlockCode = "";

            decisionWindowStateKey = "";
            decisionEvidenceContribution = 0.0d;
            decisionCounterContribution = 0.0d;
            decisionSampleReliability = 0.0d;
            decisionRecentValidity = 0.0d;
            decisionFeatureDistance = 1.0d;
            decisionEvidenceText = "";
            decisionCounterEvidenceText = "";
            decisionActionAdvice = "";
            decisionEvidenceReady = false;
            decisionEvidenceStrong = false;
            decisionEvidenceElite = false;
            decisionCounterRiskHigh = false;
            decisionHardRisk = false;

            finalPBeforeDiagnosis = 0.0d;
            finalPAfterDiagnosis = 0.0d;
            diagnosisAdjustedP = 0.0d;
            moduleAdjustedP = 0.0d;
            freezeAdjustedP = 0.0d;
            shadowPromotionBoost = 0.0d;
            probabilityEvidenceContribution = 0.0d;
            probabilityCounterEvidence = 0.0d;
            probabilitySampleReliability = 0.0d;
            probabilityRecentValidity = 0.0d;
            probabilityActionAdvice = "";
            probabilityDiagnosisSummary = "";

            qualityGateAdjust = 0.0d;
            diagnosisGateAdjust = 0.0d;
            moduleEffectGateAdjust = 0.0d;
            freezeGateAdjust = 0.0d;
            shadowPromotionGateAdjust = 0.0d;
            dynamicGateQualitySummary = "";
            dynamicGateFrequencyPolicy = "";
            dynamicGateActionAdvice = "";

            moduleEffectStatus = "";
            moduleEffectSummary = "";
            shadowPromotionStatus = "";
            shadowPromotionSummary = "";
            dynamicFreezeStatus = "";
            dynamicFreezeSummary = "";
            adaptiveJudgeStatus = "";
            adaptiveJudgeSummary = "";
            diagnosisClosedLoopSummary = "";

            moduleMisleading = false;
            shadowPromotionStrong = false;
            freezeMatrixRisk = false;
            adaptiveFreeze = false;
            adaptiveReject = false;
            adaptivePass = false;
            adaptiveWatchNeutral = false;
            finalDiagnosisRiskHigh = false;
            finalDecisionReady = false;
            finalDecisionStrong = false;
            finalDecisionFreeze = false;
            finalDecisionHardRisk = false;
            finalDecisionQualityScore = 0.0d;
            finalDecisionRiskScore = 0.0d;
            finalDecisionStatus = "";
            finalDecisionActionAdvice = "";

            dynamicClusterId = "";
            dynamicClusterStatus = "";
            dynamicClusterPrototypeKey = "";
            dynamicClusterSample = 0;
            dynamicClusterRecentSample = 0;
            dynamicClusterHitRate = 0.0d;
            dynamicClusterRecentHitRate = 0.0d;
            dynamicClusterConfidence = 0.0d;
            dynamicClusterDistance = 0.0d;
            dynamicClusterMaxLose = 0;
            dynamicClusterCounterScore = 0.0d;
            dynamicClusterReason = "";
            dynamicClusterReady = false;
            dynamicClusterStrong = false;
            dynamicClusterRisk = false;
            needSupplyMatchScore = 0.0d;
            needSupplyThirdQuality = 0.0d;
            needSupplyRoleDiversity = 0.0d;
            needSupplyRisk = 0.0d;
            needSupplySummary = "";
            needSupplyReady = false;
            needSupplyRiskHigh = false;
            counterfactualRank = 0;
            counterfactualRankScore = 0.0d;
            counterfactualWhyNotRisk = 0.0d;
            causeAblationScore = 0.0d;
            causeAblationFalseCauseCount = 0;
            counterfactualReady = false;
            counterfactualRiskHigh = false;
            adaptiveCandidateStatus = "";
            adaptiveCandidateAction = "";
            adaptiveCauseScore = 0.0d;
            adaptiveCounterScore = 0.0d;
            adaptiveCandidateReason = "";
            adaptiveCandidatePass = false;
            adaptiveCandidateFreeze = false;
            promotionClusterStatus = "";
            promotionAction = "";
            promotionClusterHitRate = 0.0d;
            promotionClusterRecentHitRate = 0.0d;
            promotionClusterSample = 0;
            promotionReason = "";
            promotionReady = false;
            promotionFreeze = false;
            dynamicCausalFieldsPresent = false;
            dynamicCausalReady = false;
            dynamicCausalStrong = false;
            dynamicCausalRiskHigh = false;
            dynamicCausalFreeze = false;
            dynamicCausalHardRisk = false;
            dynamicCausalQualityScore = 0.0d;
            dynamicCausalRiskScore = 1.0d;
            dynamicCausalSummary = "";
            dynamicCausalActionAdvice = "";
        }
    }

    private static final class QualifiedFrequencyFloorResult {
        public boolean allowed;
        public double effectiveGate;
        public double rawEdge;
        public double probeEdge;
        public double maxNegEdge;
        public String betDomain;
        public String decisionPath;
        public String reason;

        public QualifiedFrequencyFloorResult() {
            allowed = false;
            effectiveGate = 0.0d;
            rawEdge = 0.0d;
            probeEdge = 0.0d;
            maxNegEdge = AppConfig.REAL_BET_PROBE_MAX_NEG_EDGE;
            betDomain = "";
            decisionPath = "";
            reason = "";
        }
    }

    private static final class FinalCheck {
        public boolean allowBet;
        public boolean allowShadow;
        public String blockCode;
        public String reason;

        public FinalCheck() {
            allowBet = false;
            allowShadow = false;
            blockCode = AppConfig.BLOCK_LOW_PROBABILITY;
            reason = "";
        }
    }

    private int settledEvidenceSample(Models.ComboCandidate c) {
        if (c == null) return 0;
        int promotion =
                Math.max(
                        c.promotionClusterSample,
                        Math.max(
                                readInt(c, "shadowPromotionSample", 0),
                                Math.max(
                                        readInt(c, "shadowPromotionSampleCount", 0),
                                        readInt(c, "promotionSampleCount", 0))));
        int contract =
                readInt(c, "evidenceSettledOnly", 0) == 1
                        ? readInt(c, "evidenceSampleCount", 0)
                        : 0;
        return Math.max(promotion, contract);
    }

    private double settledEvidenceRate(Models.ComboCandidate c) {
        if (c == null) return 0.0d;
        double promotion =
                Math.max(c.promotionClusterHitRate, readDouble(c, "shadowPromotionHitRate", 0.0d));
        int sample = readInt(c, "evidenceSampleCount", 0);
        int hit = readInt(c, "evidenceHitCount", 0);
        double contract =
                readInt(c, "evidenceSettledOnly", 0) == 1 && sample > 0
                        ? ((double) hit) / ((double) sample)
                        : 0.0d;
        return Math.max(promotion, contract);
    }

    private boolean hasSettledEvidenceProbeSupport(Models.ComboCandidate c, String promotion) {
        if (c == null) return false;
        int sample = settledEvidenceSample(c);
        double rate = settledEvidenceRate(c);
        if (sample >= AppConfig.PROBE_STRONG_MIN_SETTLED_SAMPLE
                && rate >= AppConfig.PROBE_STRONG_MIN_SETTLED_RATE) return true;
        if (sample >= AppConfig.PROBE_READY_MIN_SETTLED_SAMPLE
                && rate >= AppConfig.PROBE_READY_MIN_SETTLED_RATE) return true;
        return sample >= AppConfig.PROBE_WATCH_MIN_SETTLED_SAMPLE
                && rate >= AppConfig.PROBE_WATCH_MIN_SETTLED_RATE;
    }

    private double progressiveProbeMinP(Models.ComboCandidate c, String promotion) {
        int sample = settledEvidenceSample(c);
        double rate = settledEvidenceRate(c);
        if (sample >= 50 && rate >= 0.390d) return 0.345d;
        if (sample >= 30 && rate >= 0.365d) return 0.350d;
        if (sample >= 18 && rate >= 0.340d) return 0.355d;
        return Math.max(0.355d, AppConfig.PROBE_BOOTSTRAP_MIN_P);
    }

    private double progressiveProbeMaxGateGap(Models.ComboCandidate c, String promotion) {
        int sample = settledEvidenceSample(c);
        double rate = settledEvidenceRate(c);
        if (sample >= 50 && rate >= 0.390d) return 0.045d;
        if (sample >= 30 && rate >= 0.365d) return 0.040d;
        if (sample >= 18 && rate >= 0.340d) return 0.035d;
        return Math.min(0.030d, AppConfig.PROBE_BOOTSTRAP_MAX_GATE_GAP);
    }

    private String evidenceProbeLevel73487246(Models.ComboCandidate c) {
        int sample = settledEvidenceSample(c);
        double rate = settledEvidenceRate(c);
        if (sample >= AppConfig.PROBE_STRONG_MIN_SETTLED_SAMPLE
                && rate >= AppConfig.PROBE_STRONG_MIN_SETTLED_RATE) return "L3_MATURE";
        if (sample >= AppConfig.PROBE_READY_MIN_SETTLED_SAMPLE
                && rate >= AppConfig.PROBE_READY_MIN_SETTLED_RATE) return "L2_READY";
        if (sample >= AppConfig.PROBE_WATCH_MIN_SETTLED_SAMPLE
                && rate >= AppConfig.PROBE_WATCH_MIN_SETTLED_RATE) return "L1_BOOTSTRAP";
        return "WATCH_ONLY";
    }

    private int evidenceProbeQuota(Models.ComboCandidate c) {
        String p = readString(c, "shadowPromotionStatus", "");
        String t = readString(c, "teacherMatrixStatus", "");
        String d = readString(c, "dynamicClusterStatus", "");
        if (containsText(p, "STRONG") || containsText(t, "STRONG") || containsText(d, "STRONG"))
            return Integer.MAX_VALUE;
        if (containsText(p, "READY") || containsText(t, "READY") || containsText(d, "READY"))
            return Integer.MAX_VALUE;
        if (containsText(p, "WATCH") || containsText(t, "WATCH") || containsText(d, "WATCH"))
            return Integer.MAX_VALUE;
        return Integer.MAX_VALUE;
    }

    private void stampRollingFrequencyWindowV17(DbStore store, Models.Decision d) {
        if (store == null || d == null) return;
        int previous = store.getRecentValidDecisionCountV17(99);
        int bets = store.getRecentRealBetCountBeforeCurrentV17(99);
        d.frequencyFloorRequiredCount =
                (int)
                        Math.ceil(
                                ((double) (previous + 1)) * AppConfig.REAL_BET_HARD_MIN_FREQUENCY
                                        - 1e-12d);
        d.frequencyFloorTargetCount =
                (int)
                        Math.ceil(
                                ((double) (previous + 1)) * AppConfig.REAL_BET_TARGET_FREQUENCY
                                        - 1e-12d);
        d.frequencyFloorActualCount = bets;
        d.frequencyFloorDebtCount = Math.max(0, d.frequencyFloorRequiredCount - bets);
        d.frequencyTargetDebtCount = Math.max(0, d.frequencyFloorTargetCount - bets);
        d.frequencyFloorNoUpperCap = 1;
        d.frequencyFloorVersion = AppConfig.ALWAYS_ON_RUNTIME_FIX_VERSION;
    }

    // V51_APEX_TYPED_ENTRY
    public static final String V57_TERMINAL_OWNER = "DecisionGateEngine";

    public V51Models.AuthorizedDecision decideV51(
            DbStore store, V51CompiledActionContract compiled) {
        V51DecisionGateCore.Result invariant = V51DecisionGateCore.validate(store, compiled);
        V51Enums.DecisionAction action = V51Enums.DecisionAction.SHADOW;
        V51Enums.RejectCode reject =
                invariant == null ? V51Enums.RejectCode.DATA_INCOMPLETE : invariant.rejectCode;
        if (compiled == null || compiled.selected == null || compiled.selected.action == null) {
            action = V51Enums.DecisionAction.HOLD;
            reject = V51Enums.RejectCode.DATA_INCOMPLETE;
        } else if (invariant != null && invariant.realEligible) {
            action = V51Enums.DecisionAction.BET;
            reject = V51Enums.RejectCode.NONE;
        }
        V51Models.AuthorizedDecision out =
                new DecisionAuthorizationEngine().authorizeV51FrozenCP12(new AuthorizationScopeCP12(compiled),compiled, action, reject);
        if (store != null) {
            try {
                store.setState("v57_b12_bet_owner", V57_TERMINAL_OWNER);
                store.setState("v57_b12_invariant", invariant == null ? "NULL" : invariant.detail);
                store.setState(
                        "v57_b12_invariant_hash", invariant == null ? "" : invariant.checksum);
            } catch (Throwable ignore) {
            }
        }
        OosActionLogEngine.recordV51AuthorizationAudit(store, out, compiled);
        return out;
    }

    // V51_APEX_LEGACY_VIEW_ADAPTER
    /** Compatibility view only; no V51 probability, rank or lane is recalculated. */
    public Models.Decision toLegacyDecisionV51(V51RuntimeResult runtime) {
        Models.Decision out = new Models.Decision();
        if (runtime == null
                || runtime.authorized == null
                || runtime.authorized.selected == null
                || runtime.authorized.selected.action == null
                || runtime.authorized.selected.context == null) {
            out.decision = AppConfig.DECISION_HOLD;
            out.action = AppConfig.ACTION_HOLD;
            out.blockCode = "V51_RUNTIME_RESULT_INCOMPLETE";
            out.finalDecisionRejectCode = out.blockCode;
            out.reasonSummary = "V51 typed runtime result incomplete";
            out.reasonText = out.reasonSummary;
            CanonicalReasonCP12.attachDecision(out);
            return out;
        }
        V51Models.AuthorizedDecision authorized = runtime.authorized;
        V51Models.SelectedActionEnvelope envelope = authorized.selected;
        V51Models.PositionAction selected = envelope.action;
        out.baseIssue = envelope.context.baseIssue;
        out.targetIssue = envelope.context.targetIssue;
        out.createdAt = envelope.context.createdAt;
        out.decision = authorized.action.name();
        out.action = authorized.action.name();
        out.position = selected.position;
        out.n1 = selected.n1;
        out.n2 = selected.n2;
        out.n3 = selected.n3;
        out.k3Mask = AppConfig.buildK3Mask(out.n1, out.n2, out.n3);
        out.rawP = selected.rawActionP;
        out.modelP = selected.rankScore;
        out.rawRankScore = selected.rankScore;
        out.calibratedP = envelope.selectedHitP;
        out.finalP = envelope.selectedHitP;
        out.honestProbability = envelope.selectedHitP;
        out.calibratedHitProbability = envelope.selectedHitP;
        out.dynamicGate = 0.0d;
        out.edge = envelope.selectedHitP;
        out.v46EventProbability = envelope.selectedHitP;
        out.v46ActionLcb = Math.max(0.0d, envelope.selectedHitP - envelope.uncertainty * 0.08d);
        out.v46ProbabilityUncertainty = envelope.uncertainty;
        out.v46ExpectedValue = AppConfig.ODDS * envelope.selectedHitP - AppConfig.BET_COST;
        out.v46FinalRank = selected.rank;
        out.v46FormalCandidateCount = 10;
        out.v46FinalContractHash = envelope.contractHash;
        out.v46ContractHash = envelope.contractHash;
        out.v46Lane = envelope.lane.name();
        out.laneCandidate = envelope.lane.name();
        out.betDomain = envelope.lane.name();
        out.finalDecisionBetDomain = envelope.lane.name();
        out.stateKey = envelope.domain == null ? "" : envelope.domain.domainId;
        out.trendLife =
                runtime.states != null
                                && selected.position <= runtime.states.length
                                && runtime.states[selected.position - 1] != null
                        ? runtime.states[selected.position - 1].runLength
                        : 0.0d;
        out.driftRisk =
                runtime.states != null
                                && selected.position <= runtime.states.length
                                && runtime.states[selected.position - 1] != null
                        ? runtime.states[selected.position - 1].changePointRisk
                        : 1.0d;
        out.buildId = V51BuildManifest.BUILD_ID;
        out.cohortId = V51BuildManifest.COHORT_ID;
        out.schemaVersion = V51BuildManifest.DATABASE_SCHEMA;
        out.featureSchemaVersion = V51BuildManifest.FEATURE_SCHEMA;
        out.rankingVersion = V51ActionRankerArtifact.ARTIFACT_ID;
        out.calibrationVersion = V51SelectedCalibrationArtifact.ARTIFACT_ID;
        out.gatePolicyVersion = V51PolicySurfaceArtifact.ARTIFACT_ID;
        out.pipelineVersion = V51BuildManifest.VERSION_NAME;
        out.v43StrategyVersion = V51BuildManifest.VERSION_NAME;
        out.contractDecisionId = authorized.decisionId;
        out.candidateHash = runtime.contract == null ? "" : runtime.contract.checksum;
        out.snapshotMaxIssue = envelope.context.historyMaxIssue;
        out.trainedUntilIssue = V51NumberModelArtifact.TRAINING_PERIOD_END;
        out.contractValid = 1;
        out.contractStatus = "V51_TYPED_CONTRACT_VALID";
        out.authorizationId = authorized.authorizationHash;
        out.authorizationValid = authorized.action == V51Enums.DecisionAction.BET ? 1 : 0;
        out.authorizedMode = authorized.action.name();
        out.authorizationRejectCode = authorized.rejectCode.name();
        out.authorizedAt = envelope.context.createdAt;
        out.authorizationProbability = envelope.selectedHitP;
        out.authorizationGate = 0.0d;
        out.authorizationEdge = envelope.selectedHitP;
        out.dataOk = runtime.success ? 1 : 0;
        out.settled = 0;
        out.result = AppConfig.RESULT_WAIT;
        out.probabilityMode = "V51_SELECTED_HIT_P";
        out.gateMode = "V51_POLICY_SURFACE_ARTIFACT";
        out.blockCode =
                authorized.rejectCode == V51Enums.RejectCode.NONE
                        ? ""
                        : authorized.rejectCode.name();
        out.finalDecisionRejectCode = out.blockCode;
        out.reasonSummary =
                "V51排列约束Rank1｜lane="
                        + envelope.lane.name()
                        + "｜predictability="
                        + (envelope.predictability == null
                                ? 0.0d
                                : envelope.predictability.probability)
                        + "｜falsifier="
                        + envelope.falsifierScore;
        out.reasonText = out.reasonSummary;
        out.preDrawReasonFingerprint = envelope.contractHash;
        out.preDrawReasonText = out.reasonSummary;
        out.learningSummary = "V51 1/10/100/permutation full-information ledger";
        out.selfDiagnosisText =
                "release="
                        + V51BuildManifest.releaseState()
                        + "|rankArtifact="
                        + V51ActionRankerArtifact.ARTIFACT_ID
                        + "|calibrationArtifact="
                        + V51SelectedCalibrationArtifact.ARTIFACT_ID
                        + "|policyArtifact="
                        + V51PolicySurfaceArtifact.ARTIFACT_ID;
        out.statScope =
                authorized.action == V51Enums.DecisionAction.BET
                        ? AppConfig.STAT_SCOPE_REAL_TRADE
                        : (authorized.action == V51Enums.DecisionAction.SHADOW
                                ? AppConfig.STAT_SCOPE_SHADOW
                                : AppConfig.STAT_SCOPE_HOLD);
        out.realtimeCostMs = runtime.elapsedMs;
        CanonicalReasonCP12.attachDecision(out);
            return out;
    }

    public static final String B12_UNIQUE_DECISION_OWNER = "DecisionGateEngine#decideV51B12";

    // R30_READONLY_BEGIN: decideV51B12
    public V51Models.AuthorizedDecision decideV51B12(
            DbStore store, V51CompiledActionContract compiled) {
        V51Models.AuthorizedDecision original = decideV51B12OriginalR30(store, compiled);
        DecisionExplanationR30.afterV51(store, compiled, original);
        return original;
    }
    // R30_READONLY_END

    private V51Models.AuthorizedDecision decideV51B12OriginalR30(
            DbStore store, V51CompiledActionContract compiled) {
        V51Enums.RejectCode block = b12Reject(store, compiled);
        if (block != V51Enums.RejectCode.NONE) {
            if (compiled == null) return decideV51(store, compiled);
            V51SelectiveRiskController.Result shadow =
                    new V51SelectiveRiskController.Result(
                            V51Enums.Lane.SHADOW, block, null, false, "B12_HARD_BLOCK");
            V51CompiledActionContract tightened =
                    new V51CompiledActionContract(
                            compiled.selected,
                            shadow,
                            compiled.stability,
                            compiled.featureHash,
                            compiled.stateHash,
                            compiled.expertHash,
                            compiled.probabilityHash,
                            compiled.actionHash);
            V51Models.AuthorizedDecision out = decideV51(store, tightened);
            if (out != null && out.action == V51Enums.DecisionAction.BET)
                throw new IllegalStateException("B12_SECONDARY_BET_ESCAPE");
            return out;
        }
        if (compiled != null
                && compiled.policy != null
                && compiled.policy.lane == V51Enums.Lane.CONTROLLED_PROBE
                && compiled.policy.realEligible) {
            V51CandidateRealGateR915.Evaluation candidate =
                    V51CandidateRealGateR915.evaluate(store, compiled);
            if (!candidate.eligible)
                throw new IllegalStateException("R915_CANDIDATE_GATE_DRIFT|" + candidate.code);
            V51Models.AuthorizedDecision controlled =
                    V51AuthorizationCore.authorize(
                            compiled, V51Enums.DecisionAction.BET, V51Enums.RejectCode.NONE);
            if (!V51CandidateRealGateR915.verifyAuthorized(store, compiled, controlled))
                throw new IllegalStateException("R915_CONTROLLED_AUTHORIZATION_FAILED");
            if (store != null) {
                try {
                    store.setState("r915_controlled_authorized_issue",
                            compiled.selected.context.targetIssue);
                    store.setState("r915_controlled_authorized_hash",
                            controlled.authorizationHash);
                } catch (Throwable ignored) {}
            }
            OosActionLogEngine.recordV51AuthorizationAudit(store, controlled, compiled);
            return controlled;
        }
        V51Models.AuthorizedDecision out = decideV51(store, compiled);
        if (out != null && out.action == V51Enums.DecisionAction.BET) {
            if (compiled.policy == null || !compiled.policy.realEligible)
                throw new IllegalStateException("B12_BET_WITHOUT_POLICY_AUTH");
            boolean controlled = compiled.policy.lane == V51Enums.Lane.CONTROLLED_PROBE;
            if (compiled.selected == null
                    || compiled.selected.action == null
                    || !CertifiedPredictionR974.validSelectedProbability(compiled.selected.selectedHitP))
                throw new IllegalStateException("B12_BET_WITHOUT_SELECTED_P");
        }
        return out;
    }

    private static V51Enums.RejectCode b12Reject(DbStore store, V51CompiledActionContract c) {
        if (c == null
                || c.selected == null
                || c.selected.context == null
                || c.selected.action == null
                || c.policy == null) return V51Enums.RejectCode.DATA_INCOMPLETE;
        if (c.policy.realEligible && (c.certifiedPrediction == null
                ||!CertifiedPredictionR974.validSelectedProbability(c.selected.selectedHitP)))
            return V51Enums.RejectCode.MODEL_NOT_PROVEN;
        if (c.certifiedPrediction != null
                && (store == null || !CertifiedPredictionR974.verifyCertificate(
                        store, c.certifiedPrediction).isEmpty()))
            return V51Enums.RejectCode.MODEL_NOT_PROVEN;
        V51Models.PositionAction a = c.selected.action;
        if (a.position < 1 || a.position > 10 || !b12K3(a.n1, a.n2, a.n3))
            return V51Enums.RejectCode.FIXED_K3_CONTRACT_FAILED;
        if (!c.selected.context.historyMaxIssue.equals(c.selected.context.baseIssue)
                || c.selected.context.historyMaxIssue.compareTo(c.selected.context.targetIssue)
                        >= 0) return V51Enums.RejectCode.HISTORY_NOT_POINT_IN_TIME;
        if (c.policy.lane == V51Enums.Lane.CONTROLLED_PROBE) {
            V51CandidateRealGateR915.Evaluation candidate =
                    V51CandidateRealGateR915.evaluate(store, c);
            return candidate.eligible
                    ? V51Enums.RejectCode.NONE
                    : candidate.code.equals("OPEN_TRADE")
                            ? V51Enums.RejectCode.UNSETTLED_TRADE_EXISTS
                            : candidate.code.equals("DUPLICATE")
                                    ? V51Enums.RejectCode.DUPLICATE_TRADE
                                    : V51Enums.RejectCode.MODEL_NOT_PROVEN;
        }
        // R13-P04: a certified selective SU2 lane is governed at run time by its certificate, its frozen
        // ACT decision and the user's explicit switch - not by compile-time constants that are always false.
        if (c.certifiedPrediction != null && c.certifiedPrediction.candidate.startsWith(com.ethk3.novaonepro.strategyv2.CertificateSU2.PREFIX))
            return com.ethk3.novaonepro.strategyv2.RealAuthoritySU2.allows(store, c.certifiedPrediction.candidate,
                    c.certifiedPrediction.certificate, c.certifiedPrediction.target, c.certifiedPrediction.deadlineAt)
                    ? V51Enums.RejectCode.NONE : V51Enums.RejectCode.MODEL_NOT_PROVEN;
        if (!AppConfig.R35_REAL_AUTO_UNLOCK_ENABLED) return V51Enums.RejectCode.MODEL_NOT_PROVEN;
        if (c.policy.realEligible) {
            if (!V51NumberModelArtifact.PROVEN
                    || !V51ExpertGateArtifact.PROVEN
                    || !V51ActionRankerArtifact.PROVEN
                    || !V51ActionRankerArtifact.RANK_ORDER_PASSED
                    || !V51SelectedCalibrationArtifact.PROVEN
                    || !V51PolicySurfaceArtifact.MODEL_GATE_PASSED)
                return V51Enums.RejectCode.MODEL_NOT_PROVEN;
            if (c.selected.domain == null
                    || c.selected.domain.status != V51Enums.DomainStatus.PROVEN)
                return V51Enums.RejectCode.DOMAIN_NOT_PROVEN;
            if (c.selected.predictability == null || !c.selected.predictability.qualified)
                return V51Enums.RejectCode.PREDICTABILITY_NOT_PROVEN;
            if (c.stability == null || !c.stability.stable)
                return V51Enums.RejectCode.RANK_UNSTABLE;
            if (store != null) {
                Models.Trade existing = store.getTradeByTargetIssue(c.selected.context.targetIssue);
                if (existing != null) return V51Enums.RejectCode.DUPLICATE_TRADE;
                if (c.certifiedPrediction==null&&store.hasOpenRealTrade()) return V51Enums.RejectCode.UNSETTLED_TRADE_EXISTS;
            }
        }
        return V51Enums.RejectCode.NONE;
    }

    public static boolean b12K3(int a, int b, int c) {
        return a >= 1 && a <= 10 && b >= 1 && b <= 10 && c >= 1 && c <= 10 && a != b && a != c
                && b != c;
    }

    public static boolean b12CanAnyOtherClassCreateBet() {
        return false;
    }
    /** Only the Gate can create this immutable single-contract signing capability. */
    public static final class AuthorizationScopeCP12 {
        private final V51CompiledActionContract contract;
        private AuthorizationScopeCP12(V51CompiledActionContract c){contract=c;}
        public boolean matches(V51CompiledActionContract c){return contract==c;}
    }
}
