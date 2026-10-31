package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/** Read-only diagnoses of the same detached facts used by all twelve report chapters.
 * Recommendations never change a gate, issue a certificate or schedule product work. */
public final class DiagnosisRulesR30 {
    private DiagnosisRulesR30() {}
    static final String ROOT = "app/src/main/java/com/ethk3/novaonepro/";
    public enum Mode { VIOLATION_COUNT, REJECTED_INPUT_COUNT, RECORDED_COUNT }
    public static final class Rule {
        public final String id, metric, field, source, card, file, method, problem, repair, collect, verification;
        public final Mode mode;
        Rule(String id, String metric, String field, String source, String card, String file, String method,
             Mode mode, String problem, String repair, String collect, String verification) {
            this.id=id;this.metric=metric;this.field=field;this.source=source;this.card=card;
            this.file=ROOT+file+".java";this.method=method;this.mode=mode;this.problem=problem;
            this.repair=repair;this.collect=collect;this.verification=verification;
        }
    }
    static Rule r(String id,String metric,String field,String source,String card,String file,String method,
                  Mode mode,String problem,String repair,String collect,String verification) {
        return new Rule(id,metric,field,source,card,file,method,mode,problem,repair,collect,verification);
    }
    public static List<Rule> rules() {
        return Collections.unmodifiableList(Arrays.asList(
            r("WRITER_HARD","M11","writerHardViolations","r30_writer_hold.over_hard","R30-B01-04",
              "strategyv2/ResearchWriteQueueR27","submit",Mode.VIOLATION_COUNT,
              "写锁窗口存在硬超限；按窗口的 operation 定位实际写者",
              "读取 writerWindows.operation/critical，按 B01-04 的幂等分片模板拆对应操作；计算移到事务外，保留写队列与实时优先级",
              "在目标设备运行实际写者并收集 WriterHoldLedgerR30.record 的窗口、operation、count 和 hold/wait 分位数",
              "R30WriterHoldLedgerTest、R30ChunkedWriterTest；目标设备复测相同 operation 的硬超限与实时结算"),
            r("TASK_STALLED","M12","taskStalled","r26_task -> TaskLivenessR30.summary.m12","R30-B01-05",
              "strategyv2/ResearchPoolR27","loop",Mode.VIOLATION_COUNT,
              "有积压的任务至少五期未提交",
              "按 taskLiveness.tasks.taskId 对照 tasks 的 wait_kind/blocked_reason/cursor，检查租约和依赖；修复对应任务的分片提交，保留游标",
              "采集每个任务 pending、last_committed_seq 和当前真实期号；补齐被限量展示省略的任务，依赖等待与等待新期分别记录",
              "R30TaskLivenessTest；有积压五期告警、等待新期不误报、缺字段保持 UNKNOWN"),
            r("RECEIPT_STATE","M32","receiptStateMismatches","r29_qual_receipt / r27_eprocess.n","R30-B03-06",
              "strategyv2/QualificationInboxR29","rebuild",Mode.VIOLATION_COUNT,
              "实际回执数与序贯状态 n 不一致",
              "逐 experiment_id 对照原始冻结、回执与游标，沿原 rebuild 幂等恢复；保留冲突账，不改 n/alpha 或补造冻结",
              "导出 ACTIVE 正永久序号实验的全部 receipt、eprocess、cursor 及原始冻结；缺表先记录 SchemaR27.ensure 的实际失败",
              "QualInboxR29HostTest 的重试/回滚/重建场景；同一快照 n 与连续回执一致"),
            r("QUAL_CONFLICT","M27","openQualificationConflicts","r29_qual_conflict.resolved_at=0","R30-B03-02",
              "strategyv2/QualificationInboxR29","plan",Mode.VIOLATION_COUNT,
              "资格证据仍有未解决的开奖冲突",
              "逐条核对 conflict.detail 与原始开奖来源，保留隔离及审计；经原源治理流程解决后再验证证书，不覆盖旧证据",
              "采集 r29_qual_conflict、draws、原始来源和对应冻结记录及其水位",
              "QualInboxR29HostTest 冲突隔离；错误来源修正后重算并确认旧证书已撤销"),
            r("UNSAFE_DECISION","M28","unsafeDecisionMismatches","r30_decision_mismatch ORIGINAL_MORE_PERMISSIVE","R30-B07-03",
              "strategyv2/RealAuthoritySU2","allows",Mode.VIOLATION_COUNT,
              "原主链允许而逐项解释拒绝，存在授权边界不一致",
              "按 issue/entry_point 对照原 allow、first_blocker、完整 freeze、证书/源版本和两本 REAL 账；修复原授权条件并重跑正反对照，禁止用解释器放宽授权",
              "采集 r30_decision_explain、r30_decision_mismatch、完整冻结/证书/intent/REAL 与来源代号",
              "R30DecisionChainAndroidTest 正对照贯通、过期/撤销/缺冻结/重复 REAL 拦截"),
            r("PIT_REJECTIONS","M18","pitViolations","r30_pit_violation (guard rejections, not proven leakage)","R30-B03-01",
              "strategyv2/PitGuardR30","check",Mode.REJECTED_INPUT_COUNT,
              "时点守卫有拒绝记录，需要区分注入夹具与产品来源；拒绝数不等于泄漏次数",
              "按 policy_hash/target_ordinal/code/source 定位来源，修复 availability/trainedThrough/history 边界并保持拒绝；分别导出注入和非注入反证，不将拒绝误报为成功泄漏",
              "采集守卫的真实 code/source 和原冻结输入；运行当前及未来标签扰动反证，明确运行环境与夹具范围",
              "R30 PIT 与 FreezeSourceR30HostTest 标签投毒/源竞态；既有 predraw 输出不变"),
            r("CONFIRM_EVIDENCE","M19","confirmedPackets","r30_frozen_confirm DONE verified=1","R30-B05-01",
              "strategyv2/FrozenConfirmationR30","evaluate",Mode.RECORDED_COUNT,
              "独立确认没有已完成且已验证的数据包；已有数据包也不代表自然优势",
              "检查 task_key/state 与已登记确认曝光，恢复原冻结确认任务；禁止回读 H_CONFIRM 调参或重置曝光预算",
              "导出 r30_frozen_confirm 与 r30_confirm_read 的冻结候选/数据hash、预约和完成状态",
              "FrozenConfirmationR30HostTest 中断恢复、重复曝光拒绝、原始证据重用与登记一致"),
            r("MODEL_STATE","M20","modelCheckpoints","r29_model_checkpoint ATOMNET_TEN_R30/ATOMNET_FTRL","R30-B05-03",
              "strategyv2/ModelArtifactR29","decode",Mode.RECORDED_COUNT,
              "当前 NN checkpoint 不足以证明可恢复全状态训练",
              "核对 owner/ABI/dataset/state_hash，完整解码权重/优化器/RNG/游标后重跑中断恢复；保留旧 ABI 模型，不用缓存冒充模型",
              "经原诊断导出 checkpoint 元数据与解码结果、实际训练任务状态；完整模型仅保留在原受保护产物内",
              "TenPositionR30HostTest 位置5杀进程后6–10恢复一致；旧模型、失败提交和游标保留"),
            r("MODEL_CONSUMER","M24","modelCalls","r30_model_call","R30-B05-04",
              "strategyv2/ModelSurfaceProviderR30","predict",Mode.RECORDED_COUNT,
              "模型消费尚无可追溯调用；调用行存在仍需匹配实际冻结",
              "逐调用对照 model_hash/target/position 和 I08 冻结来源；缺模型保持 BLOCKED，禁止以热缓存或事后模型替代",
              "采集 r30_model_call、r30_model_surface、模型元数据和原冻结引用的同一期配对",
              "ModelSurfaceR30HostTest valid/missing/target/race；实际引用一致且缺失拒绝"),
            r("CALIBRATION","Q05","calibratedPolicies","r27_calibration / calibration-r30 cache","R30-B06-01",
              "strategyv2/LiveShadowsR27","calibrate",Mode.RECORDED_COUNT,
              "校准存在性不证明满足 Q05；缺样本时不得报告有效校准",
              "核对真实验证行、滚动拟合截止/embargo、等宽/等质量 ECE、Brier/BSS 与支持范围；样本200/ECE0.04/支持0.90门槛保持原值",
              "采集原校准行与 calibration-r30 同数据集缓存的 validationRows/fitEnd/validateStart/支持范围；缺失仍为 UNKNOWN/INSUFFICIENT",
              "CalibrationR30HostTest 严格时序、恒定概率与失败提交；自然 Q05 单独验收"),
            r("NATIVE_MATRIX","M25","nativeMatrixRows","research_matrix_snapshot_r930","R30-B06-03",
              "MatrixSnapshotFeedbackR930","registryId",Mode.RECORDED_COUNT,
              "原生矩阵缺少已存快照；有快照也不能称有效资产",
              "按矩阵 kind 对照真实 producer/consumer、目标期和原 hash；保留所有原快照及身份",
              "采集20对象清单与原矩阵来源表的 count/latest issue/hash，缺来源保持 null",
              "MatrixRegistryR30HostTest 原生19类和已有SU2对象的实际绑定与三态"),
            r("SU2_MATRIX","M25","su2MatrixRows","su2_matrix_snapshot","R30-B06-03",
              "strategyv2/MatrixSnapshotSU2","capture",Mode.RECORDED_COUNT,
              "SU2矩阵/教师对象缺少已存快照；checkpoint序列不得当作日历期号",
              "对照实际SU2 producer/consumer、checkpoint_sequence与数据集，禁止伪造第20个原生枚举或有效性",
              "采集 su2_matrix_snapshot 的真实数量、checkpoint_sequence、updated_at 与20对象注册来源",
              "MatrixRegistryR30HostTest SU2来源和checkpoint水位；qualityEffective仍需独立证据"),
            r("ABI_CONTINUITY","M20","abiChecks","r27_abi_continuity","R30-B03-05",
              "strategyv2/ModelArtifactR29","decode",Mode.RECORDED_COUNT,
              "ABI连续性缺少回执；已有行必须逐项核验结果，不能仅数行判通过",
              "核对 old_build/new_build/result/first_diff_issue 和模型feature ABI，按原转接规则续训并保留旧产物",
              "采集当前源版本对应的 ABI 检查行、完整checkpoint解码和中断恢复差异",
              "ModelArtifact 全状态恢复回归；源版本与数据集相同的恢复比较"),
            r("ABLATION","Q06","ablationRows","r27_ablation","R30-B08-03",
              "strategyv2/UpgradeDiagnosisSU2","render",Mode.RECORDED_COUNT,
              "消融没有已存同源对照；有行也不能代替父子同一期配对区间",
              "检查 generation/champion/node/branch 及同一期收益差，区间下界不为正时保持未验证；报告显示缺样本原因和采集动作",
              "采集原消融行和父子同一期机会配对明细、样本数与单侧95%区间；禁止选好窗口",
              "空库无空章；有数据不丢原始结果；同一期配对下界验证 Q06"),
            r("NATURAL_SAMPLE","Q01","independentFutureIssues","r29_qual_receipt DISTINCT target_issue","R30-B10-02",
              "strategyv2/LiveShadowsR27","certify",Mode.RECORDED_COUNT,
              "独立未来期数只能说明已存样本，不能宣称自然命中达标",
              "按事前登记、ACT和自然期号去重审核真实样本及证书，保留原统计门槛；回放/合成/重复策略样本另列",
              "在目标设备采集事前登记到自然FUTURE结算的原始冻结、ACT命中、时间块、校准和序贯证据",
              "B10自然Q01–Q07逐项验收；样本不足为INSUFFICIENT，未测不填0/PASS"),
            r("SNAPSHOT_CAPTURE","M32","captureErrors","ReportSnapshotR30.capture sourceErrors","R30-B08-01",
              "strategyv2/ReportSnapshotR30","capture",Mode.VIOLATION_COUNT,
              "报告快照采集失败或部分来源不可用",
              "按 sourceErrors 与来源表水位定位只读采集失败；保持单快照及schema版本检查，不改成逐章重新查库",
              "保留真实异常和当前 schema_version/来源表元数据，启动后重新导出同一快照；不可读值保持未知",
              "ReportSnapshotR30HostTest 并发写入、缺源、UTF8、水位及冲突原样显示")
        ));
    }
    static Long count(Object value) {
        if (!(value instanceof Number)) return null;
        double d=((Number)value).doubleValue();
        return Double.isFinite(d)&&d>=0&&d==Math.rint(d)&&d<Long.MAX_VALUE?((Number)value).longValue():null;
    }
    static Object taskStalled(Map<String,Object> data) {
        Object live=data.get("taskLiveness");if(!(live instanceof Map))return null;
        Object summary=((Map<?,?>)live).get("summary");return summary instanceof Map?((Map<?,?>)summary).get("m12"):null;
    }
    public static Map<String,Object> evaluate(Map<String,Object> facts,Map<String,Object> data,
                                               Map<String,Object> watermark,List<String> errors) {
        List<Map<String,Object>> findings=new ArrayList<>();int actionable=0,unknown=0,detected=0,insufficient=0;
        for(Rule rule:rules()) {
            Object raw=facts.get(rule.field);
            if(rule.field.equals("taskStalled"))raw=taskStalled(data);
            else if(rule.field.equals("captureErrors")) {
                raw=null;
                if(!errors.isEmpty()||watermark.get("factsReadAt") instanceof Number)raw=Integer.valueOf(errors.size());
            }
            Long value=count(raw);String state;
            if(value==null) {state="UNKNOWN";unknown++;}
            else if(rule.mode==Mode.RECORDED_COUNT) {state=value==0?"INSUFFICIENT":"RECORDED_UNVALIDATED";if(value==0)insufficient++;}
            else if(value==0)state="CLEAR_IN_CAPTURED_SCOPE";
            else {state=rule.mode==Mode.REJECTED_INPUT_COUNT?"REVIEW_GUARD_REJECTIONS":"DETECTED";detected++;}
            Map<String,Object> evidence=new LinkedHashMap<>();evidence.put("field",rule.field);evidence.put("value",value);
            evidence.put("source",rule.source);evidence.put("snapshotWatermark",watermark);
            if(rule.field.equals("captureErrors"))evidence.put("sourceErrors",errors);
            Map<String,Object> action=new LinkedHashMap<>();action.put("file",rule.file);action.put("method",rule.method);
            action.put("code",rule.file+":"+rule.method);action.put("card",rule.card);
            action.put("instruction",value==null||state.equals("INSUFFICIENT")?rule.collect:rule.repair);
            action.put("verification",rule.verification);
            Map<String,Object> finding=new LinkedHashMap<>();finding.put("id",rule.id);finding.put("metric",rule.metric);
            finding.put("condition",rule.field+(rule.mode==Mode.RECORDED_COUNT?" == 0 / > 0; existence is not validation":" > 0; missing is UNKNOWN"));
            finding.put("status",state);finding.put("conclusion",value==null?"UNKNOWN：缺少可判定的同快照证据":state.equals("CLEAR_IN_CAPTURED_SCOPE")?"本次采集范围未记录该问题；不外推为自然质量或真机通过":state.equals("RECORDED_UNVALIDATED")?"已有来源记录，尚不能据行数判定有效；"+rule.problem:rule.problem);
            finding.put("strength",value==null?"NONE":"DIRECT_SNAPSHOT_COUNT_NOT_CAUSAL_OR_QUALITY_PROOF");
            finding.put("evidence",evidence);finding.put("action",action);finding.put("collectionAction",rule.collect);
            finding.put("refutedBy",rule.mode==Mode.VIOLATION_COUNT?"同一范围原始来源恢复一致且对应回归通过":"对应原始配对数据和独立验证回执，不能仅凭行数");
            finding.put("scope","DETACHED_CAPTURE_ONLY; NO_AUTOMATIC_MUTATION; NOT_PHYSICAL_OR_NATURAL_ACCEPTANCE");
            findings.add(finding);if(!rule.method.isEmpty()&&!rule.collect.isEmpty()&&!rule.verification.isEmpty())actionable++;
        }
        Map<String,Object> out=new LinkedHashMap<>();out.put("version","R30_DIAGNOSIS_1");out.put("findings",findings);
        out.put("expectedRules",findings.size());out.put("actionableRules",actionable);out.put("actionCoverage",findings.isEmpty()?null:(double)actionable/findings.size());
        out.put("unknown",unknown);out.put("detectedOrReview",detected);out.put("insufficient",insufficient);
        out.put("snapshotWatermark",watermark);out.put("coverageDefinition","Registered diagnostic rules carry evidence, strength, file:method, card, validation and a collection action; not a claim that all product defects are known.");
        return out;
    }
}
