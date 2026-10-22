package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Executable genome for a newly invented analysis asset.
 *
 * <p>An asset is not a renamed formula. It owns a typed L0 observation set, a stateful inference
 * program, a settlement-only update law, an output semantic, explicit failure/falsification rules,
 * lineage, resources and Future-OOS promotion requirements. Legacy assets are readable context
 * only and can never be mutation parents.</p>
 */
public final class GenesisAssetGenomeR915 {
    public static final String VERSION = "R915_ASSET_GENOME_V4_1";
    public static final String NAMESPACE = "GENESIS_NEW";
    public static final int ORGAN_COUNT = 15;
    public static final int MIN_ATOMS = 8;
    public static final int MAX_ATOMS = 24;
    public static final boolean MAY_AUTHORIZE_REAL = false;

    public enum OutputType {
        SCALAR_SCORE("分析评分", false, "1", 0),
        VECTOR_FIELD("号码位置向量", true, "10", 10),
        MATRIX_FIELD("关系矩阵", true, "4x4", 16),
        GRAPH_FIELD("拓扑关系图", true, "10x3", 30),
        TENSOR_FIELD("多尺度证据张量", true, "3x4x4", 48),
        SEQUENCE_FIELD("时序轨迹", true, "16", 16),
        STATE_MACHINE_FIELD("状态机分布", true, "8", 8),
        DISTRIBUTION_FIELD("号码分布", true, "10", 10),
        PROTOTYPE_LIBRARY("动态原型库", true, "6x4", 24),
        RISK_SURFACE("风险曲面", false, "1", 0),
        STABILITY_SURFACE("稳定性曲面", false, "1", 0),
        UNCERTAINTY_SURFACE("不确定性曲面", false, "1", 0),
        OPPORTUNITY_SURFACE("选择机会曲面", false, "1", 0),
        RESIDUAL_FIELD("主链残差场", true, "10", 10),
        RELATION_FIELD("关联耦合场", true, "10", 10),
        TRANSITION_FIELD("迁移转移场", true, "10", 10),
        MOTIF_FIELD("形态母题场", true, "10", 10),
        REGIME_FIELD("区制状态场", true, "3", 3),
        CALIBRATION_RESIDUAL("校准残差", false, "1", 0),
        EVIDENCE_DENSITY("证据密度", false, "1", 0);

        public final String nameZh, shape;
        public final boolean shaped;
        public final int flatSize;
        OutputType(String nameZh, boolean shaped, String shape, int flatSize) {
            this.nameZh = nameZh;
            this.shaped = shaped;
            this.shape = shape;
            this.flatSize = Math.max(0, flatSize);
        }
    }

    public static final class Organ {
        public final int index;
        public final String id, contract;
        Organ(int index, String id, String contract) {
            this.index = index;
            this.id = id;
            this.contract = contract;
        }
        public String canonical() { return index + ":" + id + ':' + contract; }
    }

    public final String assetId, familyId, parentIds, creationReason, trainedUntilIssue;
    public final int generation;
    public final OutputType outputType;
    public final int[] atomIndices;
    public final AlgorithmGenomeR908 kernel;
    public final MolecularToolWorldR947.Executable molecule;
    public final List<Organ> organs;
    public final String structuralHash, behaviorHash, mechanismNameZh, mechanismSpecZh;

    public GenesisAssetGenomeR915(
            String familyId,
            int generation,
            String parentIds,
            String creationReason,
            String trainedUntilIssue,
            OutputType outputType,
            int[] atomIndices,
            AlgorithmGenomeR908 kernel) {
        this(familyId,generation,parentIds,creationReason,trainedUntilIssue,outputType,atomIndices,kernel,null);
    }

    public GenesisAssetGenomeR915(String familyId,int generation,String parentIds,String creationReason,
            String trainedUntilIssue,OutputType outputType,int[] atomIndices,AlgorithmGenomeR908 kernel,
            MolecularToolWorldR947.Executable molecule) {
        this.familyId = normalizeFamily(familyId);
        this.generation = Math.max(1, generation);
        this.parentIds = safe(parentIds);
        this.creationReason = safe(creationReason);
        this.trainedUntilIssue = safe(trainedUntilIssue);
        this.outputType = outputType == null ? OutputType.SCALAR_SCORE : outputType;
        this.molecule=molecule;
        this.atomIndices = normalizeAtoms(withMoleculeParents(atomIndices,molecule));
        this.kernel = kernel == null
                ? AlgorithmGenomeR908.seed(0x5239313547454eL, this.generation, "ASSET_KERNEL")
                : kernel;
        organs = Collections.unmodifiableList(buildOrgans());
        structuralHash = V51ChecksumEngine.sha256(structureCanonical());
        assetId = "R915-ASSET-" + structuralHash.substring(0, 24);
        mechanismNameZh = chineseName();
        mechanismSpecZh = mechanismDescriptionZh();
        // Structural novelty is insufficient: execute inference and settlement updates on a fixed
        // 40-step PIT probe so differently written but behaviorally equivalent assets collapse.
        behaviorHash = GenesisAssetVmR915.behaviorHash(this);
        validate();
    }

    public boolean assetGrade() {
        if (!structureGrade()) return false;
        return structuralHash.length() == 64
                && behaviorHash.length() == 64
                && mechanismNameZh.endsWith("资产·第" + two(generation) + "代");
    }

    /** R922 certificate boundary: historical proxy-bearing genomes remain readable but cannot promote. */
    public boolean usesOnlyProductionScoreAtoms() {
        for (int index : atomIndices)
            if (!GenesisSchemaRegistryR915.atoms().get(index).productionScoreEligible()) return false;
        return true;
    }

    /** Executable body check used while the constructor is computing the behavior hash. */
    boolean structureGrade() {
        if (MAY_AUTHORIZE_REAL || !NAMESPACE.equals("GENESIS_NEW")) return false;
        if (!kernel.algorithmGrade() || organs.size() != ORGAN_COUNT) return false;
        if (atomIndices.length < MIN_ATOMS || atomIndices.length > MAX_ATOMS) return false;
        Set<Integer> distinct = new HashSet<Integer>();
        int groups = 0, previousGroup = -1;
        for (int index : atomIndices) {
            if (index < 0 || index >= GenesisSchemaRegistryR915.atomCount() || !distinct.add(index))
                return false;
            int group = index / 20;
            if (group != previousGroup) { groups++; previousGroup = group; }
            GenesisSchemaRegistryR915.AtomSpec atom = GenesisSchemaRegistryR915.atoms().get(index);
            if (!atom.predrawComputable()) return false;
        }
        return groups >= 3;
    }

    public GenesisAssetGenomeR915 mutate(long seed, int childGeneration, String reason) {
        Rng rng = new Rng(seed ^ structuralHash.hashCode());
        int[] atoms = atomIndices.clone();
        int edits = 1 + rng.nextInt(Math.max(1, Math.min(4, atoms.length / 3)));
        for (int i = 0; i < edits; i++) atoms[rng.nextInt(atoms.length)] = rng.nextInt(240);
        atoms = repairProductionAtoms(atoms, rng);
        OutputType output = rng.nextInt(5) == 0
                ? OutputType.values()[rng.nextInt(OutputType.values().length)] : outputType;
        AlgorithmGenomeR908 childKernel = kernel.mutate(seed ^ 0x9E3779B97F4A7C15L, childGeneration);
        GenesisAssetGenomeR915 child = new GenesisAssetGenomeR915(
                familyId, childGeneration, assetId, safe(reason), trainedUntilIssue,
                output, atoms, childKernel,molecule);
        if (child.structuralHash.equals(structuralHash))
            return child.mutate(seed ^ 0xD1B54A32D192ED03L, childGeneration, reason);
        return child;
    }

    public GenesisAssetGenomeR915 crossover(
            GenesisAssetGenomeR915 other, long seed, int childGeneration, String reason) {
        if (other == null || !NAMESPACE.equals(other.namespace()))
            return mutate(seed, childGeneration, reason);
        Rng rng = new Rng(seed ^ structuralHash.hashCode() ^ other.structuralHash.hashCode());
        ArrayList<Integer> merged = new ArrayList<Integer>();
        for (int value : atomIndices) if (rng.nextBoolean()) merged.add(Integer.valueOf(value));
        for (int value : other.atomIndices) if (rng.nextBoolean()) merged.add(Integer.valueOf(value));
        while (merged.size() < MIN_ATOMS) merged.add(Integer.valueOf(rng.nextInt(240)));
        while (merged.size() > MAX_ATOMS) merged.remove(rng.nextInt(merged.size()));
        int[] atoms = new int[merged.size()];
        for (int i = 0; i < atoms.length; i++) atoms[i] = merged.get(i).intValue();
        atoms = repairProductionAtoms(atoms, rng);
        AlgorithmGenomeR908 childKernel = kernel.crossover(other.kernel, seed, childGeneration);
        String family = rng.nextBoolean() ? familyId : other.familyId;
        OutputType output = rng.nextBoolean() ? outputType : other.outputType;
        return new GenesisAssetGenomeR915(
                family, childGeneration, assetId + ',' + other.assetId, safe(reason),
                later(trainedUntilIssue, other.trainedUntilIssue), output, atoms, childKernel,molecule);
    }

    public String namespace() { return NAMESPACE; }

    public String canonical() {
        return VERSION
                + "|family=" + familyId
                + "|generation=" + generation
                + "|parents=" + parentIds
                + "|reason=" + creationReason
                + "|trained=" + trainedUntilIssue
                + "|output=" + outputType.name()
                + "|atoms=" + atomCsv(atomIndices)
                + (molecule==null?"":"|molecule="+molecule.canonical)
                + "|kernel=" + kernel.canonical();
    }

    public static GenesisAssetGenomeR915 parse(String canonical) {
        String value = safe(canonical);
        if (!value.startsWith(VERSION + '|')) throw new IllegalArgumentException("R915_ASSET_SPEC_PREFIX");
        int kernelAt = value.indexOf("|kernel=");
        if (kernelAt < 0) throw new IllegalArgumentException("R915_ASSET_SPEC_KERNEL");
        String head = value.substring(0, kernelAt);
        String kernel = value.substring(kernelAt + 8);
        return new GenesisAssetGenomeR915(
                field(head, "family"),
                parseInt(field(head, "generation"), 1),
                field(head, "parents"),
                field(head, "reason"),
                field(head, "trained"),
                OutputType.valueOf(field(head, "output")),
                parseAtoms(field(head, "atoms")),
                AlgorithmGenomeR908.parse(kernel),
                field(head,"molecule").isEmpty()?null:MolecularToolWorldR947.Executable.parse(field(head,"molecule")));
    }

    public String structureCanonical() {
        StringBuilder out = new StringBuilder(VERSION)
                .append('|').append(NAMESPACE)
                .append('|').append(familyId)
                .append('|').append(outputType.name())
                .append('|').append(atomCsv(atomIndices))
                .append('|').append(kernel.programStructureCanonical());
        for (Organ organ : organs) out.append('|').append(organ.canonical());
        if(molecule!=null)out.append("|molecule=").append(molecule.canonical).append('|').append(molecule.hash);
        return out.toString();
    }

    private static int[] withMoleculeParents(int[] atoms,MolecularToolWorldR947.Executable molecule){
        if(molecule==null)return atoms;
        java.util.TreeSet<Integer> merged=new java.util.TreeSet<Integer>();
        if(atoms!=null)for(int i:atoms)merged.add(i);
        Set<Integer> required=new HashSet<Integer>();for(int i:molecule.parents()){required.add(i);merged.add(i);}
        while(merged.size()>MAX_ATOMS){Integer remove=null;for(Integer i:merged.descendingSet())if(!required.contains(i)){remove=i;break;}if(remove==null)throw new IllegalArgumentException("R958_MOLECULE_PARENT_BUDGET");merged.remove(remove);}
        int[] out=new int[merged.size()];int i=0;for(Integer value:merged)out[i++]=value;return out;
    }

    public String chineseName() {
        String family = familyName(familyId);
        String observation = observationTitle();
        String memory = kernel.mechanismCell().contains("G0") ? "位置记忆" : "全局位置双记忆";
        String prefix = new String[] {"天工", "星衍", "知微", "玄衡", "曜枢", "辰创"}
                [(structuralHashSeed() & 0x7fffffff) % 6];
        return prefix + "·" + family + '·' + observation + memory + outputType.nameZh
                + "资产·第" + two(generation) + "代";
    }

    public String mechanismDescriptionZh() {
        return "命名依据=" + chineseName()
                + "；命名空间=" + NAMESPACE
                + "；观察原子=" + atomNames()
                + "；输出语义=" + outputType.nameZh
                + "；推断学习=" + kernel.mechanismDescriptionZh()
                + "；赛前只读=是；结算后更新=是；Future-OOS晋级=必需"
                + "；可直接授权REAL=否；十五器官=" + organsCanonical();
    }

    private List<Organ> buildOrgans() {
        ArrayList<Organ> out = new ArrayList<Organ>(ORGAN_COUNT);
        out.add(new Organ(1,"IdentityNamespace","GENESIS_NEW/结构身份/行为身份/版本"));
        out.add(new Organ(2,"AtomicProvenance","全部输入可追溯到240个L0赛前原子"));
        out.add(new Organ(3,"TemporalBoundary","trainedUntil<target且freeze发生在目标开奖前"));
        out.add(new Organ(4,"PersistentState","位置记忆+全局记忆+更新次数+checkpoint"));
        out.add(new Organ(5,"ObservationProgram","类型化原子选择/缺失处理/范围压缩"));
        out.add(new Organ(6,"InferenceProgram","状态参与的可执行评分程序"));
        out.add(new Organ(7,"SettlementUpdate","命中/概率残差驱动的开奖后更新程序"));
        out.add(new Organ(8,"OutputSemantics",outputType.name()+"/shape="+outputType.shape+"/非伪概率/范围[0,1]"));
        out.add(new Organ(9,"QualityContract","确定性/有限值/全1200动作/无部分冻结"));
        out.add(new Organ(10,"FailureContract","泄漏/乱序/重复结算/资源超限立即拒绝"));
        out.add(new Organ(11,"Falsification","消融/置换零假设/行为去重/分块失稳"));
        out.add(new Organ(12,"ResourceBudget","低优先级/限时/限内存/P0让路"));
        out.add(new Organ(13,"Lineage","父代/代数/创造原因/算子/子代证据归零"));
        out.add(new Organ(14,"EvidenceLifecycle","Future-OOS/Wilson/FDR/Brier/ECE/paired delta"));
        out.add(new Organ(15,"PromotionExplainability","中文机制名/证书/限幅接入/一键撤销"));
        return out;
    }

    private void validate() {
        if (!assetGrade()) throw new IllegalArgumentException("R915_ASSET_NOT_EXECUTABLE_GRADE");
        if (parentIds.contains("LEGACY-"))
            throw new IllegalArgumentException("R915_LEGACY_CANNOT_BE_GENESIS_PARENT");
        if (TargetIssueResolver.isValid(trainedUntilIssue) && trainedUntilIssue.length() != 12)
            throw new IllegalArgumentException("R915_TRAINED_UNTIL_INVALID");
    }

    private int structuralHashSeed() {
        try { return (int)Long.parseLong(structuralHash.substring(0, 8), 16); }
        catch (Throwable ignored) { return structuralHash.hashCode(); }
    }

    private String observationTitle() {
        int[] group = new int[12];
        for (int index : atomIndices) group[index / 20]++;
        int first = 0, second = 1;
        for (int i = 0; i < group.length; i++) {
            if (group[i] > group[first]) { second = first; first = i; }
            else if (i != first && group[i] > group[second]) second = i;
        }
        return groupName(first) + groupName(second);
    }

    private String atomNames() {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < atomIndices.length; i++) {
            if (i > 0) out.append(',');
            GenesisSchemaRegistryR915.AtomSpec atom = GenesisSchemaRegistryR915.atoms().get(atomIndices[i]);
            out.append(atom.id).append(':').append(atom.name);
        }
        return out.toString();
    }

    private String organsCanonical() {
        StringBuilder out = new StringBuilder();
        for (Organ organ : organs) {
            if (out.length() > 0) out.append('/');
            out.append(organ.id);
        }
        return out.toString();
    }

    private static int[] normalizeAtoms(int[] values) {
        Rng rng = new Rng(values == null ? 915L : java.util.Arrays.hashCode(values));
        int[] input = values == null ? new int[0] : values.clone();
        if (input.length < MIN_ATOMS) {
            int[] expanded = new int[MIN_ATOMS];
            for (int i = 0; i < expanded.length; i++) expanded[i] = i < input.length ? input[i] : rng.nextInt(240);
            input = expanded;
        }
        if (input.length > MAX_ATOMS) input = java.util.Arrays.copyOf(input, MAX_ATOMS);
        return repairAtoms(input, rng);
    }

    private static int[] repairAtoms(int[] values, Rng rng) {
        HashSet<Integer> used = new HashSet<Integer>();
        for (int i = 0; i < values.length; i++) {
            int value = Math.floorMod(values[i], 240);
            if (!GenesisSchemaRegistryR915.atoms().get(value).predrawComputable())
                value = randomPredrawAtom(rng);
            while (!used.add(Integer.valueOf(value))) value = randomPredrawAtom(rng);
            values[i] = value;
        }
        java.util.Arrays.sort(values);
        int groups = groupCount(values);
        for (int g = groups; g < 3; g++) {
            int at = values.length - 1 - g;
            int value = ((g + 7) % 12) * 20 + rng.nextInt(20);
            if (!GenesisSchemaRegistryR915.atoms().get(value).predrawComputable())
                value = randomPredrawAtom(rng);
            while (contains(values, value)) value = randomPredrawAtom(rng);
            values[Math.max(0, at)] = value;
            java.util.Arrays.sort(values);
        }
        return values;
    }

    /** New generations may not inherit A10/A11 proxy semantics into a certifiable score program. */
    private static int[] repairProductionAtoms(int[] values, Rng rng) {
        HashSet<Integer> used = new HashSet<Integer>();
        int[] out = values == null ? new int[MIN_ATOMS] : values.clone();
        for (int i = 0; i < out.length; i++) {
            int value = Math.floorMod(out[i], GenesisSchemaRegistryR915.atomCount());
            if (!GenesisSchemaRegistryR915.atoms().get(value).productionScoreEligible()
                    || !used.add(Integer.valueOf(value))) {
                do { value = randomProductionAtom(rng); }
                while (!used.add(Integer.valueOf(value)));
            }
            out[i] = value;
        }
        java.util.Arrays.sort(out);
        for (int attempts = 0; groupCount(out) < 3 && attempts < 96; attempts++) {
            int at = out.length - 1 - attempts % Math.max(1, out.length);
            int value = randomProductionAtom(rng);
            if (contains(out, value)) continue;
            out[Math.max(0, at)] = value;
            java.util.Arrays.sort(out);
        }
        if (groupCount(out) < 3) throw new IllegalStateException("R922_PRODUCTION_ATOM_DIVERSITY");
        return out;
    }

    private static int groupCount(int[] values) {
        HashSet<Integer> groups = new HashSet<Integer>();
        for (int value : values) groups.add(Integer.valueOf(value / 20));
        return groups.size();
    }

    private static boolean contains(int[] values, int value) {
        for (int one : values) if (one == value) return true;
        return false;
    }

    private static int randomPredrawAtom(Rng rng) {
        for (int attempt = 0; attempt < 480; attempt++) {
            int value = rng.nextInt(240);
            if (GenesisSchemaRegistryR915.atoms().get(value).predrawComputable()) return value;
        }
        for (int value = 0; value < 240; value++)
            if (GenesisSchemaRegistryR915.atoms().get(value).predrawComputable()) return value;
        throw new IllegalStateException("R919_NO_PREDRAW_COMPUTABLE_ATOM");
    }

    private static int randomProductionAtom(Rng rng) {
        for (int attempt = 0; attempt < 480; attempt++) {
            int value = rng.nextInt(GenesisSchemaRegistryR915.atomCount());
            if (GenesisSchemaRegistryR915.atoms().get(value).productionScoreEligible()) return value;
        }
        for (int value = 0; value < GenesisSchemaRegistryR915.atomCount(); value++)
            if (GenesisSchemaRegistryR915.atoms().get(value).productionScoreEligible()) return value;
        throw new IllegalStateException("R922_NO_PRODUCTION_SCORE_ATOM");
    }

    private static String atomCsv(int[] values) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) out.append(',');
            out.append(values[i]);
        }
        return out.toString();
    }

    private static int[] parseAtoms(String csv) {
        String[] parts = safe(csv).split(",");
        int[] out = new int[parts.length];
        for (int i = 0; i < out.length; i++) out[i] = parseInt(parts[i], i);
        return out;
    }

    private static String field(String source, String name) {
        String token = "|" + name + '=';
        int at = source.indexOf(token);
        if (at < 0) return "";
        int start = at + token.length();
        int end = source.indexOf('|', start);
        return end < 0 ? source.substring(start) : source.substring(start, end);
    }

    private static String normalizeFamily(String id) {
        String value = safe(id);
        for (GenesisSchemaRegistryR915.FamilySpec family : GenesisSchemaRegistryR915.families())
            if (family.id.equals(value)) return value;
        return GenesisSchemaRegistryR915.families().get(0).id;
    }

    private static String familyName(String id) {
        for (GenesisSchemaRegistryR915.FamilySpec family : GenesisSchemaRegistryR915.families())
            if (family.id.equals(id)) return family.nameZh;
        return "未知结构";
    }

    private static String groupName(int group) {
        String[] names = {"时点真值", "排列身份", "时间记忆", "关系图", "数值变换", "信息统计",
                "状态反馈", "风险不确定", "动作几何", "证据谱系", "资源健康", "进化互补"};
        return names[Math.max(0, Math.min(names.length - 1, group))];
    }

    private static String later(String a, String b) {
        if (!TargetIssueResolver.isValid(a)) return safe(b);
        if (!TargetIssueResolver.isValid(b)) return safe(a);
        return TargetIssueResolver.compare(a, b) >= 0 ? a : b;
    }

    private static int parseInt(String value, int fallback) {
        try { return Integer.parseInt(safe(value)); }
        catch (Throwable ignored) { return fallback; }
    }

    private static String two(int value) { return String.format(Locale.US, "%02d", value); }
    private static String safe(String value) { return value == null ? "" : value.trim(); }

    private static final class Rng {
        private long state;
        Rng(long seed) { state = seed == 0L ? 0x9E3779B97F4A7C15L : seed; }
        long nextLong() {
            long x = state;
            x ^= x >>> 12; x ^= x << 25; x ^= x >>> 27;
            state = x;
            return x * 2685821657736338717L;
        }
        int nextInt(int bound) { return bound <= 1 ? 0 : (int)Math.floorMod(nextLong(), bound); }
        boolean nextBoolean() { return (nextLong() & 1L) != 0L; }
    }
}
