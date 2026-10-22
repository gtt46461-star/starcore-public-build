package com.ethk3.novaonepro;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Locale;

/** Streams the complete Batch-3/4 contract and current evidence boundary into the full report. */
public final class Batch34ReportR929 {
    public static final String VERSION = "R929_BATCH34_REPORT_V2";

    private static final String[][] TASKS = {
        {"D01", "PIT训练特征、1200动作、位置标签与缺失值合同", "ENGINEERING_IMPLEMENTED|REAL_DATA_PENDING"},
        {"D02", "依赖拓扑、时间分割、多分类训练与晋级", "ENGINEERING_IMPLEMENTED|REAL_TRAINING_PENDING"},
        {"D03", "状态轨迹与Transition Tensor", "ENGINEERING_IMPLEMENTED|REAL_TRAINING_PENDING"},
        {"D04", "动作Ranker离线排序训练", "ENGINEERING_IMPLEMENTED|REAL_TRAINING_PENDING"},
        {"D05", "稀疏专家门控、失败归因与可逆恢复", "ENGINEERING_IMPLEMENTED|REAL_TRAINING_PENDING"},
        {"D06", "选中动作条件概率、OOF校准与风险覆盖", "ENGINEERING_IMPLEMENTED|FUTURE_EVIDENCE_PENDING"},
        {"D07", "不确定性、随机种子与Rank稳定性", "ENGINEERING_IMPLEMENTED|FUTURE_EVIDENCE_PENDING"},
        {"D08", "关系图、残差专家与消融", "ENGINEERING_IMPLEMENTED|FUTURE_ABLATION_PENDING"},
        {"D09", "Predictability、15%频率与40%效果独立闭环", "ENGINEERING_IMPLEMENTED|FUTURE_EFFECT_PENDING"},
        {"D10", "工件原子发布、CAS热切换与整组回滚", "ENGINEERING_IMPLEMENTED"},
        {"C01", "晋级证书V2与必附合同", "ENGINEERING_IMPLEMENTED"},
        {"C02", "配对基线、固定窗口与一次终局决定", "ENGINEERING_IMPLEMENTED"},
        {"C03", "Alpha-spending、序贯停止与层级FDR", "ENGINEERING_IMPLEMENTED"},
        {"C04", "四层捕获覆盖、事务冻结与失败可观测", "ENGINEERING_IMPLEMENTED"},
        {"C05", "结构/行为/残差去重与家族冠军", "ENGINEERING_IMPLEMENTED"},
        {"C06", "稳定域、软状态混合与域证书", "ENGINEERING_IMPLEMENTED|FUTURE_DOMAIN_EVIDENCE_PENDING"},
        {"C07", "逐期共享输入、全候选评估与持久续做", "ENGINEERING_IMPLEMENTED|R943_RUNTIME_REVALIDATION_REQUIRED"},
        {"C08", "生命周期V3与精英修复效果账本", "ENGINEERING_IMPLEMENTED"},
        {"C09", "逐期失败冻结、真模块消融与反事实", "ENGINEERING_IMPLEMENTED|FAILURE_CASE_EVIDENCE_PENDING"},
        {"C10", "240原子缺失/时延/PIT/质量图", "ENGINEERING_IMPLEMENTED|RUNTIME_ATOM_EVIDENCE_PENDING"},
        {"C11", "原子准入、隔离、旧矩阵保留与证书失效", "ENGINEERING_IMPLEMENTED"},
        {"C12", "结构新颖度、可执行Genome与质量目标", "ENGINEERING_IMPLEMENTED|PAIRED_GAIN_PENDING"}
    };

    private Batch34ReportR929() {}

    public static void writeContract(Writer out) throws IOException {
        if (out == null) throw new IOException("R929_REPORT_WRITER_NULL");
        Batch34RuntimeGateR929.GateResult gate = Batch34RuntimeGateR929.evaluateCurrent(true);
        out.write("STARCORE_TIANSHU_R929_BATCH34_REPORT_V2\n");
        out.write("Scope=THIRD_AND_FOURTH_BATCH_COMBINED\n");
        out.write("DecisionOwner=DecisionGateEngine_ONLY\n");
        out.write("SettlementOwner=SettlementEngine_ONLY\n");
        out.write("Play=single-position|fixed-K3|max-one-REAL-per-issue\n");
        out.write("Forbidden=K4|dynamicK|multi-position-REAL|chase|martingale|doubling|frequency-debt\n");
        out.write("EngineeringBoundary=SOURCE_AND_CONTRACTS\n");
        out.write("EffectBoundary=REAL_PIT_DATA_PLUS_UNCONSUMED_FUTURE_SEALED_WINDOW\n");
        out.write("CurrentEvidenceStatus=" + Batch34ProductionEvidenceR929.EVIDENCE_STATUS + "\n");
        out.write("GeneratedFromRealData=" + Batch34ProductionEvidenceR929.GENERATED_FROM_REAL_DATA + "\n");
        out.write("FutureOosProven=" + Batch34ProductionEvidenceR929.FUTURE_OOS_PROVEN + "\n");
        out.write("PolicyEffectProven=" + Batch34ProductionEvidenceR929.POLICY_EFFECT_PROVEN + "\n");
        out.write("RealEligible=" + gate.realEligible + "\n");
        out.write("ShadowAllowed=" + gate.shadowAllowed + "\n");
        out.write("ShadowRequiredWhenNotReal=" + gate.shadowRequiredWhenNotReal + "\n");
        out.write("GateCode=" + gate.code + "\n");
        out.write("GateEvidenceSha256=" + gate.evidenceSha256 + "\n");
        out.write("GateFailures=" + Batch34EvidenceContractR929.join(gate.failures, ",") + "\n");

        out.write("\n【R929-1.第三第四批次逐项状态】\n");
        for (String[] row : TASKS) out.write(row[0] + "|" + row[2] + "|" + row[1] + "\n");

        out.write("\n【R929-2.数据与训练硬合同】\n");
        out.write("DatasetSchema=" + Batch34EvidenceContractR929.DATASET_SCHEMA + "\n");
        out.write("ProductionMinimumIssues=" + Batch34EvidenceContractR929.MIN_PRODUCTION_ISSUES + "\n");
        out.write("ActionsPerIssue=" + Batch34EvidenceContractR929.ACTIONS_PER_ISSUE + "\n");
        out.write("ActionFeatureMinimum=" + Batch34EvidenceContractR929.MIN_ACTION_FEATURES + "\n");
        out.write("LabelSchema=" + Batch34EvidenceContractR929.LABEL_SCHEMA + "\n");
        out.write("PITViolationsAllowed=0\nPostDrawFeaturesAllowed=0\nSyntheticRowsAllowed=0\n");
        out.write("ZeroFilledMissingAllowed=0\nChronologicalSplitRequired=true\n");
        out.write("UnconsumedSealedWindowRequired=true\n");

        out.write("\n【R929-3.原子工件与发布】\n");
        List<String> ids = Batch34EvidenceContractR929.requiredArtifactIds();
        out.write("RequiredArtifactCount=" + ids.size() + "\n");
        out.write("RequiredArtifacts=" + Batch34EvidenceContractR929.join(ids, ",") + "\n");
        out.write("MixedGenerationAllowed=false\nAtomicSwitch=COMPARE_AND_SET\n");
        out.write("RollbackScope=WHOLE_GENERATION_GROUP\nPartialPromotionAllowed=false\n");

        out.write("\n【R929-4.统计与效果门】\n");
        out.write("TargetSelectedHitRate=" + pct(Batch34RuntimeGateR929.TARGET_HIT_RATE) + "\n");
        out.write("CandidateEntryHitRate=" + pct(CertificateBundleServiceR929.MIN_ENTRY_HIT_RATE) + "|entry_is_not_future_guarantee\n");
        out.write("MinimumSelectedCoverage=" + pct(Batch34RuntimeGateR929.MIN_SELECTED_COVERAGE) + "\n");
        out.write("MaximumECE=" + pct(Batch34RuntimeGateR929.MAX_ECE) + "\n");
        out.write("MaximumMissStreak=" + Batch34RuntimeGateR929.MAX_MISS + "\n");
        out.write("MinimumDeterministicSeeds=" + Batch34RuntimeGateR929.MIN_SEEDS + "\n");
        out.write("MinimumTop1SeedAgreement="
                + pct(Batch34RuntimeGateR929.MIN_TOP1_SEED_AGREEMENT) + "\n");
        out.write("CertificateDirectBetAuthority=false\nQuotaEnforcementAllowed=false\n");
        out.write("FrequencyDebtAllowed=false\n");

        out.write("\n【R929-5.研究生命周期与原子图】\n");
        out.write("Scheduler=R943_ALL_EXECUTABLE_FULL1200|DURABLE_ROSTER_PAGES|PER_CANDIDATE_COMMIT|DEADLINE_YIELD_WITH_RESUME\n");
        out.write("PerSliceBudgetNs=" + Batch34ResearchLifecycleR929.PER_SLICE_BUDGET_NS + "\n");
        out.write("InitialTotalBudgetNs=" + Batch34ResearchLifecycleR929.INITIAL_TOTAL_BUDGET_NS + "\n");
        out.write("AtomSemanticInventory=240\nAtomBreakdown=PRE_DRAW_165|PROXY_40|POST_DRAW_20|AUDIT_15\n");
        out.write("Lifecycle=IDEA>NURSERY>OBSERVING>EVIDENCE_READY>CHALLENGER>SHADOW>INFLUENCE>CHAMPION\n");
        out.write("SideStates=PAUSED|QUARANTINED|PRUNED\nRepairChildIdentity=NEW_ID_AND_ZERO_SUPPORT\n");
        out.write("LegacyFixedDeletionAllowed=false\nChangedAtomCertificateInvalidationRequired=true\n");

        out.write("\n【R929-6.证明边界】\n");
        out.write("EngineeringCompletionDoesNotEqualModelProof=true\n");
        out.write("SyntheticFixtureDoesNotEqualFutureOOS=true\n");
        out.write("BuildSuccessDoesNotEqualPhysicalDeviceRuntime=true\n");
        out.write("NoRealEvidenceNoRealUnlock=true\n");
        out.write("NonRealAvailableIssueRoute=SHADOW_OR_AUDITED_SYSTEM_REJECTION\n");
        out.write("END_R929_BATCH34_REPORT\n");
    }

    public static String compactCurrent() {
        Batch34RuntimeGateR929.GateResult gate = Batch34RuntimeGateR929.evaluateCurrent(true);
        return "R929_BATCH34|engineering=" + gate.structuralReady
                + "|effect=" + gate.effectReady
                + "|real=" + gate.realEligible
                + "|shadow=" + gate.shadowAllowed
                + "|code=" + gate.code
                + "|hash=" + gate.evidenceSha256;
    }

    private static String pct(double value) {
        return String.format(Locale.US, "%.2f%%", value * 100.0d);
    }
}
