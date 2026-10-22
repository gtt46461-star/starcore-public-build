package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Complete read-only ownership registry for mature analysis/mainline assets that existed before
 * R915.
 *
 * <p>The registry imports every executable owner in {@link StrategyModuleCatalog}, then adds the
 * V51 production surface, expert family, matrix bridge, immutable 1200-action space and
 * cross-position assets introduced after that catalog was frozen. Every row is bound to the exact
 * source-file SHA-256 in {@link LegacySourceLockManifestR915}; Genesis can read these assets but
 * cannot mutate, cross, prune, rename, replace, migrate or override them.</p>
 */
public final class LegacyFixedAssetRegistryR915 {
    public static final String NAMESPACE = "LEGACY_FIXED_ASSETS";
    public static final boolean MUTATION_ALLOWED = false;
    public static final boolean PRUNE_ALLOWED = false;
    public static final boolean WEIGHT_OVERRIDE_ALLOWED = false;
    public static final int MINIMUM_COMPLETE_OWNER_COUNT = 120;

    public static final class Entry {
        public final String id, nameZh, sourceClass, output, consumer, sourceHash;

        Entry(String id, String name, String owner, String output, String consumer, String hash) {
            this.id = safe(id);
            nameZh = safe(name);
            sourceClass = safe(owner);
            this.output = safe(output);
            this.consumer = safe(consumer);
            sourceHash = safe(hash).toLowerCase(Locale.US);
        }

        public String canonical() {
            return id + '|' + nameZh + '|' + sourceClass + '|' + output + '|' + consumer + '|'
                    + sourceHash;
        }
    }

    private static final class Supplement {
        final String owner, name, output, consumer;
        Supplement(String owner, String name, String output, String consumer) {
            this.owner = owner;
            this.name = name;
            this.output = output;
            this.consumer = consumer;
        }
    }

    private static final Entry[] ENTRIES = buildEntries();

    private LegacyFixedAssetRegistryR915() {}

    public static Entry[] entries() { return ENTRIES.clone(); }
    public static int sourceLockCount() { return LegacySourceLockManifestR915.size(); }

    public static String fingerprint() {
        assertImmutableContract();
        StringBuilder out = new StringBuilder(NAMESPACE).append('|').append(ENTRIES.length);
        for (Entry entry : ENTRIES) out.append('|').append(entry.canonical());
        return V51ChecksumEngine.sha256(out.toString());
    }

    public static void assertImmutableContract() {
        if (MUTATION_ALLOWED || PRUNE_ALLOWED || WEIGHT_OVERRIDE_ALLOWED)
            throw new IllegalStateException("R915_LEGACY_FIXED_MUTATION_AUTHORITY_FORBIDDEN");
        if (ENTRIES.length < MINIMUM_COMPLETE_OWNER_COUNT || ENTRIES.length != sourceLockCount())
            throw new IllegalStateException(
                    "R915_LEGACY_FIXED_INVENTORY_INCOMPLETE:" + ENTRIES.length + '/' + sourceLockCount());
        Set<String> ids = new HashSet<String>();
        Set<String> owners = new HashSet<String>();
        for (Entry entry : ENTRIES) {
            if (entry == null || !ids.add(entry.id) || !owners.add(entry.sourceClass)
                    || entry.nameZh.length() == 0 || entry.output.length() == 0
                    || entry.consumer.length() == 0 || !LegacySourceLockManifestR915.isSha256(entry.sourceHash))
                throw new IllegalStateException("R915_LEGACY_FIXED_REGISTRY_INVALID");
            if (entry.sourceClass.startsWith("Genesis") || entry.sourceClass.endsWith("R915"))
                throw new IllegalStateException("R915_LEGACY_NEW_NAMESPACE_COLLISION:" + entry.sourceClass);
            try {
                Class.forName(
                        "com.ethk3.novaonepro." + entry.sourceClass,
                        false,
                        LegacyFixedAssetRegistryR915.class.getClassLoader());
            } catch (Throwable missing) {
                throw new IllegalStateException("R915_LEGACY_OWNER_CLASS_MISSING:" + entry.sourceClass, missing);
            }
        }
    }

    private static Entry[] buildEntries() {
        LinkedHashMap<String,Entry> rows = new LinkedHashMap<String,Entry>();
        for (StrategyModuleCatalog.Spec spec : StrategyModuleCatalog.all())
            add(rows, spec.name, legacyChineseName(spec.name, spec.role),
                    spec.stage + ':' + spec.role, consumerFor(spec.stage, spec.role));
        for (Supplement item : supplements())
            add(rows, item.owner, item.name, item.output, item.consumer);
        return rows.values().toArray(new Entry[rows.size()]);
    }

    private static void add(
            LinkedHashMap<String,Entry> rows,
            String owner,
            String name,
            String output,
            String consumer) {
        if (rows.containsKey(owner)) return;
        String digest = LegacySourceLockManifestR915.digest(owner);
        if (!LegacySourceLockManifestR915.isSha256(digest))
            throw new IllegalStateException("R915_LEGACY_SOURCE_LOCK_MISSING:" + owner);
        String id = "LEGACY-" + V51ChecksumEngine.sha256(owner).substring(0, 16).toUpperCase(Locale.US);
        rows.put(owner, new Entry(id, name, owner, output, consumer, digest));
    }

    private static String legacyChineseName(String owner, String role) {
        String key = safe(role).toUpperCase(Locale.US);
        String mechanism;
        if (key.contains("TEACHER")) mechanism = "教师矩阵";
        else if (key.contains("CAUS") || key.contains("CAUSE")) mechanism = "因果推理";
        else if (key.contains("COUNTERFACT") || key.contains("FALS")) mechanism = "反推反事实";
        else if (key.contains("TREND") || key.contains("TRANSITION")) mechanism = "趋势转移";
        else if (key.contains("SHAPE") || key.contains("PATTERN") || key.contains("MOTIF")) mechanism = "形态母题";
        else if (key.contains("NUMBER") || key.contains("DISTRIBUTION")) mechanism = "号码属性";
        else if (key.contains("RELATION") || key.contains("ASSOCIATION")) mechanism = "关联耦合";
        else if (key.contains("CALIBRATION") || key.contains("PROBABILITY")) mechanism = "概率校准";
        else if (key.contains("RISK") || key.contains("FREEZE") || key.contains("HEALTH")) mechanism = "风险稳定";
        else if (key.contains("LEARN") || key.contains("MODEL") || key.contains("FOREST")) mechanism = "学习模型";
        else if (key.contains("EVIDENCE") || key.contains("ATTRIBUTION")) mechanism = "证据归因";
        else if (key.contains("ACTION") || key.contains("CANDIDATE") || key.contains("RANK")) mechanism = "动作排序";
        else if (key.contains("SETTLEMENT") || key.contains("OOS")) mechanism = "结算回测";
        else if (key.contains("DIAGNOSIS") || key.contains("REASON")) mechanism = "诊断解释";
        else mechanism = "固定分析";
        return mechanism + "·" + owner;
    }

    private static String consumerFor(String stage, String role) {
        String key = (safe(stage) + '|' + safe(role)).toUpperCase(Locale.US);
        if (key.contains("SETTLEMENT")) return "SettlementEngine唯一结算与Research只读反哺";
        if (key.contains("DECISION")) return "DecisionGateEngine唯一出手边界";
        if (key.contains("AUTHORIZATION")) return "DecisionAuthorizationEngine签名边界";
        if (key.contains("PROBABILITY") || key.contains("CALIBRATION")) return "唯一主链概率与校准";
        if (key.contains("RISK") || key.contains("POLICY")) return "唯一主链风险与选择性策略";
        if (key.contains("DIAGNOSIS")) return "真值诊断与界面解释";
        if (key.contains("BACKGROUND") || key.contains("OOS")) return "后台学习与Future-OOS审计";
        return "唯一主链赛前只读证据";
    }

    private static List<Supplement> supplements() {
        ArrayList<Supplement> out = new ArrayList<Supplement>();
        s(out,"FullInformationFeatureGraphEngine","全信息因子组合图","固定特征图/因子组合","唯一主链赛前只读证据");
        s(out,"K3ActionSpaceEngine","固定单位置K3动作空间","1200个不可删除动作","V51动作构建与排序");
        s(out,"CrossPositionAssignmentResearchEngine","多号码多位置交叉交集研究","assignment_support/assignment_gap","V51动作元特征与Research基线");
        s(out,"NumberSurfaceTensorEngine","号码概率曲面张量","号码@位置概率曲面","V51号码专家只读上下文");
        s(out,"RelationTensorEngine","号码位置关系张量","转移/关系/跨位张量","V51关系专家只读上下文");
        s(out,"PointInTimeNumberFeatureEngine","赛前号码原子特征","严格PIT号码特征","V51号码专家与Research");
        s(out,"V51PermutationFeatureEngine","V51排列赛前特征","排列/轨迹/位置迁移特征","V51唯一主链");
        s(out,"V51TrajectoryFeatureEngine","V51走势轨迹特征","位移/周期/邻接轨迹","V51状态向量");
        s(out,"V51ObservableStateVectorEngine","V51可观测状态向量","严格PIT状态向量","V51状态轨迹");
        s(out,"V51StatePrototypeEngine","V51状态原型","状态原型与距离","V51状态轨迹");
        s(out,"V51StateTrajectoryEngine","V51状态轨迹","状态原型/变化点/生命周期","V51号码模型与解释");
        s(out,"V51LifecycleEngine","V51状态生命周期","出现/延续/衰减/消失","V51状态轨迹");
        s(out,"V51ChangePointEngine","V51变化点","区制切换/失稳信号","V51状态轨迹");
        s(out,"V51ExpertGateEngine","V51专家健康门","专家权重/冻结/恢复","V51号码概率模型");
        s(out,"V51PermutationNumberModelEngine","V51多专家号码模型","10位置×10号码概率面","V51动作空间");
        s(out,"V51PositionActionBuilder","V51位置动作构建","1200动作与原始概率","V51动作排序");
        s(out,"V51ActionMetaFeatureEngine","V51交叉交集动作特征","交叉支持/一致性/号码风险","V51动作排序");
        s(out,"V51ActionRankerEngine","V51全动作排序","1200动作分值/排序","V51选择性策略");
        s(out,"V51SelectedActionCalibrationEngine","V51选择动作校准","选择后命中概率/原始概率","V51选择性策略");
        s(out,"V51PredictabilityEngine","V51可预测性评估","稳定性/不确定性/可出手性","V51选择性策略");
        s(out,"V51DomainCertificateEngine","V51支持域证书","区制/覆盖/分布外证据","V51选择性策略");
        s(out,"V51CrossBlockDomainCertificateEngine","V51跨块支持域证书","分块稳定/跨块覆盖","V51选择性策略");
        s(out,"V51SelectiveRiskController","V51选择性风险控制","SHADOW/REAL候选车道","DecisionGateEngine");
        s(out,"V51PolicySurfaceEngine","V51策略曲面","覆盖/风险/稳定门槛面","V51选择性风险控制");
        s(out,"V51HierarchicalPriorNumberExpert","V51层级先验专家","位置号码层级先验","V51号码模型");
        s(out,"V51MultiScaleTrendNumberExpert","V51多尺度趋势专家","短中长窗趋势后验","V51号码模型");
        s(out,"V51ContextTreeNumberExpert","V51上下文树专家","变阶上下文后继","V51号码模型");
        s(out,"V51StateDurationNumberExpert","V51状态时长专家","状态驻留与风险率","V51号码模型");
        s(out,"V51MotifNumberExpert","V51母题后继专家","相似排列母题后继","V51号码模型");
        s(out,"V51RelationGraphNumberExpert","V51关系图专家","号码位置迁移关系图","V51号码模型");
        s(out,"V51TeacherPosteriorNumberExpert","V51教师后验专家","教师后验矩阵","V51号码模型");
        s(out,"V51OfflineResidualNumberExpert","V51离线残差专家","冻结离线残差","V51号码模型");
        s(out,"V51OnlineResidualNumberExpert","V51在线残差专家","严格结算后在线残差","V51号码模型");
        s(out,"V51TianshuEvidenceBridge","教师推理反推因果反哺桥","十六项赛前/赛后真值状态","V51解释与Research只读上下文");
        s(out,"V51FalsifierExpert","V51反证专家","反事实/失效/否决证据","V51风险门");
        s(out,"V51RawScoreModelEngine","V51原始分数融合","专家原始号码分数","V51双随机投影");
        s(out,"V51SinkhornEngine","V51双随机概率投影","行列守恒号码概率矩阵","V51号码模型");
        s(out,"V51EffectiveConsensusEngine","V51有效共识","专家互补/冲突共识","V51动作排序");
        s(out,"V51ExpertRegretEngine","V51专家后悔度","Future-OOS专家后悔度","V51专家健康门");
        s(out,"V51RankStabilityEngine","V51动作稳定性","排序扰动与动作稳定","V51选择性风险控制");
        s(out,"V51UncertaintyEngine","V51号码风险与不确定性","熵/边际/OOD风险","V51选择性风险控制");
        s(out,"V51TransitionTensorEngine","V51转移张量","位置号码转移张量","V51关系图专家");
        s(out,"V51WindowPosteriorEngine","V51窗口后验","多尺度窗口后验","V51趋势专家");
        s(out,"V51MotifMatrixProfileEngine","V51矩阵母题剖面","相似母题距离与后继","V51母题专家");
        s(out,"V51FeatureContractRegistry","V51特征语义合同","公式/窗口/范围/owner","构建与运行审计");
        s(out,"V51FieldOwnerRegistry","V51唯一字段owner合同","可影响出手字段单owner","授权与构建审计");
        return out;
    }

    private static void s(List<Supplement> out, String owner, String name, String output, String consumer) {
        out.add(new Supplement(owner, name, output, consumer));
    }

    private static String safe(String value) { return value == null ? "" : value.trim(); }
}
