package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Sixteen-organ contract around every invented algorithm family.
 *
 * <p>The executable kernel remains {@link AlgorithmGenomeR908}; this contract supplies everything
 * a program-search result needs before it can be called a research algorithm: chronological
 * identity, observation/state/update semantics, objective, uncertainty, falsification, resources,
 * lineage, evidence, lifecycle and a strictly bounded deployment path. A child always starts with
 * zero Future-OOS evidence.</p>
 */
public final class ResearchGenomeContractR915 {
    public static final String VERSION = "R915_RESEARCH_GENOME_V4_1";
    public static final String NAMESPACE = "RESEARCH_ALGORITHM";
    public static final int ORGAN_COUNT = 16;
    public static final int MIN_CERTIFICATE_N = 240;
    public static final int MIN_PAIRED_N = 120;
    public static final boolean MAY_AUTHORIZE_REAL = false;

    public static final class Evidence {
        public final int futureN, hitN, pairedN, maxMiss;
        public final double hitRate, wilsonLower, brier, ece, blockMin, regimeMin,
                coverage, pairedDelta, fdrQ;
        public final String lastIssue, evidenceHash;

        public Evidence(
                int futureN,
                int hitN,
                int pairedN,
                int maxMiss,
                double hitRate,
                double wilsonLower,
                double brier,
                double ece,
                double blockMin,
                double regimeMin,
                double coverage,
                double pairedDelta,
                double fdrQ,
                String lastIssue,
                String evidenceHash) {
            this.futureN = Math.max(0, futureN);
            this.hitN = Math.max(0, Math.min(this.futureN, hitN));
            this.pairedN = Math.max(0, pairedN);
            this.maxMiss = Math.max(0, maxMiss);
            this.hitRate = clamp01(hitRate);
            this.wilsonLower = clamp01(wilsonLower);
            this.brier = Double.isFinite(brier)&&brier>=0?clamp01(brier):-1.0d;
            this.ece = Double.isFinite(ece)&&ece>=0?clamp01(ece):-1.0d;
            this.blockMin = clamp01(blockMin);
            this.regimeMin = clamp01(regimeMin);
            this.coverage = clamp01(coverage);
            this.pairedDelta = finite(pairedDelta);
            this.fdrQ = clamp01(fdrQ);
            this.lastIssue = safe(lastIssue);
            this.evidenceHash = safe(evidenceHash);
        }

        public static Evidence empty() {
            return new Evidence(0,0,0,0,0,0,1,1,0,0,0,0,1,"","");
        }

        public static Evidence parse(String canonical) {
            String[] fields = safe(canonical).split(",", -1);
            if (fields.length != 15)
                throw new IllegalArgumentException("R915_RESEARCH_EVIDENCE_PARTS");
            return new Evidence(
                    integer(fields[0]),
                    integer(fields[1]),
                    integer(fields[2]),
                    integer(fields[3]),
                    fromHex(fields[4]),
                    fromHex(fields[5]),
                    fromHex(fields[6]),
                    fromHex(fields[7]),
                    fromHex(fields[8]),
                    fromHex(fields[9]),
                    fromHex(fields[10]),
                    fromHex(fields[11]),
                    fromHex(fields[12]),
                    fields[13],
                    fields[14]);
        }

        public boolean certificateGrade() {
            return futureN >= MIN_CERTIFICATE_N
                    && pairedN >= MIN_PAIRED_N
                    && hitRate >= CertificateBundleServiceR929.MIN_ENTRY_HIT_RATE
                    && wilsonLower >= 0.30d
                    && brier>=0 && brier <= 0.24d
                    && ece>=0 && ece <= 0.12d
                    && blockMin >= 0.25d
                    && regimeMin >= 0.22d
                    && coverage >= 0.80d
                    && pairedDelta >= 0.02d
                    && maxMiss <= 16
                    && fdrQ <= 0.20d
                    && TargetIssueResolver.isValid(lastIssue)
                    && evidenceHash.length() == 64;
        }

        public String canonical() {
            return futureN + "," + hitN + "," + pairedN + "," + maxMiss + ","
                    + hex(hitRate) + "," + hex(wilsonLower) + "," + hex(brier) + ","
                    + hex(ece) + "," + hex(blockMin) + "," + hex(regimeMin) + ","
                    + hex(coverage) + "," + hex(pairedDelta) + "," + hex(fdrQ) + ","
                    + lastIssue + "," + evidenceHash;
        }
    }

    public static final class Organ {
        public final int order;
        public final String name, contract;
        Organ(int order, String name, String contract) {
            this.order = order;
            this.name = name;
            this.contract = contract;
        }
        public String canonical() { return order + ":" + name + ':' + contract; }
    }

    public final AlgorithmGenomeR908 genome;
    public final String contractId, trainedUntilIssue, earliestUseIssue, lifecycleStatus;
    public final Evidence evidence;
    public final List<Organ> organs;
    public final String contractHash;

    public ResearchGenomeContractR915(
            AlgorithmGenomeR908 genome,
            String trainedUntilIssue,
            String earliestUseIssue,
            String lifecycleStatus,
            Evidence evidence) {
        if (genome == null) throw new IllegalArgumentException("R915_RESEARCH_GENOME_NULL");
        this.genome = genome;
        this.trainedUntilIssue = safe(trainedUntilIssue);
        this.earliestUseIssue = safe(earliestUseIssue);
        this.lifecycleStatus = normalizeStatus(lifecycleStatus);
        this.evidence = evidence == null ? Evidence.empty() : evidence;
        organs = Collections.unmodifiableList(buildOrgans());
        contractHash = V51ChecksumEngine.sha256(structureCanonical() + "|evidence=" + this.evidence.canonical());
        contractId = "R915-RG-" + V51ChecksumEngine.sha256(structureCanonical()).substring(0, 24);
        validate();
    }

    public static ResearchGenomeContractR915 freshChild(
            AlgorithmGenomeR908 child, String trainedUntilIssue, String earliestUseIssue) {
        return new ResearchGenomeContractR915(
                child, trainedUntilIssue, earliestUseIssue, "IDEA_WAIT_FUTURE_OOS", Evidence.empty());
    }

    /** Strict round-trip parser used by the Production consumer; partial or forged contracts fail. */
    public static ResearchGenomeContractR915 parse(String canonical) {
        String value = safe(canonical);
        String prefix = VERSION + "|trained=";
        if (!value.startsWith(prefix))
            throw new IllegalArgumentException("R915_RESEARCH_CONTRACT_VERSION");
        int earliestAt = value.indexOf("|earliest=", prefix.length());
        int statusAt = value.indexOf("|status=", earliestAt + 10);
        int evidenceAt = value.indexOf("|evidence=", statusAt + 8);
        int genomeAt = value.indexOf("|genome=", evidenceAt + 10);
        if (earliestAt < 0 || statusAt < 0 || evidenceAt < 0 || genomeAt < 0)
            throw new IllegalArgumentException("R915_RESEARCH_CONTRACT_PARTS");
        String trained = value.substring(prefix.length(), earliestAt);
        String earliest = value.substring(earliestAt + 10, statusAt);
        String status = value.substring(statusAt + 8, evidenceAt);
        Evidence evidence = Evidence.parse(value.substring(evidenceAt + 10, genomeAt));
        AlgorithmGenomeR908 genome = AlgorithmGenomeR908.parse(value.substring(genomeAt + 8));
        ResearchGenomeContractR915 parsed =
                new ResearchGenomeContractR915(genome, trained, earliest, status, evidence);
        if (!parsed.canonical().equals(value))
            throw new IllegalArgumentException("R915_RESEARCH_CONTRACT_NON_CANONICAL");
        return parsed;
    }

    public ResearchGenomeContractR915 withEvidence(Evidence next, String status) {
        return new ResearchGenomeContractR915(
                genome, trainedUntilIssue, earliestUseIssue, status, next);
    }

    public boolean productionInfluenceEligible(String targetIssue) {
        return !MAY_AUTHORIZE_REAL
                && evidence.certificateGrade()
                && "CHALLENGER_FUTURE_OOS".equals(lifecycleStatus)
                && TargetIssueResolver.isValid(trainedUntilIssue)
                && TargetIssueResolver.compare(trainedUntilIssue, targetIssue) < 0
                && (earliestUseIssue.length() == 0
                        || (TargetIssueResolver.isValid(earliestUseIssue)
                                && TargetIssueResolver.compare(earliestUseIssue, targetIssue) <= 0));
    }

    public String canonical() {
        return VERSION
                + "|trained=" + trainedUntilIssue
                + "|earliest=" + earliestUseIssue
                + "|status=" + lifecycleStatus
                + "|evidence=" + evidence.canonical()
                + "|genome=" + genome.canonical();
    }

    public String structureCanonical() {
        StringBuilder out = new StringBuilder(VERSION).append('|').append(NAMESPACE)
                .append('|').append(genome.programStructureCanonical())
                .append("|trained=").append(trainedUntilIssue)
                .append("|earliest=").append(earliestUseIssue);
        for (Organ organ : organs) out.append('|').append(organ.canonical());
        return out.toString();
    }

    public String truthZh() {
        return "研究算法=" + genome.chineseName()
                + "；十六器官=完整"
                + "；Future-OOS=" + evidence.futureN + '/' + evidence.hitN
                + "；命中率=" + percent(evidence.hitRate)
                + "；Wilson下界=" + percent(evidence.wilsonLower)
                + "；FDR-q=" + percent(evidence.fdrQ)
                + "；paired=" + evidence.pairedN + "/" + percent(evidence.pairedDelta)
                + "；状态=" + lifecycleStatus
                + "；直接出手权限=无；证书影响主链="
                + (evidence.certificateGrade() ? "可候选" : "不可");
    }

    private List<Organ> buildOrgans() {
        ArrayList<Organ> out = new ArrayList<Organ>(ORGAN_COUNT);
        out.add(new Organ(1,"Identity","结构/行为/数据/版本四重身份"));
        out.add(new Organ(2,"Observation","严格PIT原子与只读Legacy上下文"));
        out.add(new Organ(3,"Representation","位置/全局/跨位多层表示"));
        out.add(new Organ(4,"PersistentMemory","位置8槽+全局8槽+peer记忆"));
        out.add(new Organ(5,"InferenceProgram","低级原语生成的可执行评分程序"));
        out.add(new Organ(6,"SettlementUpdate","只读冻结输入+真实结果更新程序"));
        out.add(new Organ(7,"Objective","命中/校准/稳定/覆盖/复杂度多目标"));
        out.add(new Organ(8,"Calibration","原始评分不伪装校准概率"));
        out.add(new Organ(9,"RiskUncertainty","OOD/熵/最大连挂/动作稳定"));
        out.add(new Organ(10,"RegimeNiche","分区制证据与专长适用域"));
        out.add(new Organ(11,"Falsification","未来块/消融/置换/FDR/行为去重"));
        out.add(new Organ(12,"ResourceBudget","移动端时延/内存/队列/P0让路"));
        out.add(new Organ(13,"Lineage","父代/代数/变异/交叉/子代N=0"));
        out.add(new Organ(14,"FutureEvidence","N/Wilson/Brier/ECE/paired/coverage"));
        out.add(new Organ(15,"Lifecycle","IDEA→OBSERVING→CHALLENGER或PRUNED"));
        out.add(new Organ(16,"DeploymentExplainability","中文机制名/限幅影响/可回滚/无REAL权力"));
        return out;
    }

    private void validate() {
        if (MAY_AUTHORIZE_REAL || !genome.algorithmGrade() || organs.size() != ORGAN_COUNT)
            throw new IllegalArgumentException("R915_RESEARCH_GENOME_ORGANS_INVALID");
        if (trainedUntilIssue.length() > 0 && !TargetIssueResolver.isValid(trainedUntilIssue))
            throw new IllegalArgumentException("R915_RESEARCH_TRAINED_ISSUE_INVALID");
        if (earliestUseIssue.length() > 0 && !TargetIssueResolver.isValid(earliestUseIssue))
            throw new IllegalArgumentException("R915_RESEARCH_EARLIEST_ISSUE_INVALID");
        if (TargetIssueResolver.isValid(trainedUntilIssue) && TargetIssueResolver.isValid(earliestUseIssue)
                && TargetIssueResolver.compare(trainedUntilIssue, earliestUseIssue) >= 0)
            throw new IllegalArgumentException("R915_RESEARCH_TEMPORAL_BOUNDARY_INVALID");
        // freshChild() is the only construction path used by insertion and always writes N=0.
        // Once future outcomes arrive the candidate may still be in IDEA while N<24; parsing that
        // legitimate intermediate evidence must not invalidate the persisted contract.
    }

    private static String normalizeStatus(String value) {
        String status = safe(value).toUpperCase(Locale.US);
        if (status.equals("IDEA_WAIT_FUTURE_OOS") || status.equals("OBSERVING_FUTURE_OOS")
                || status.equals("CHALLENGER_FUTURE_OOS") || status.startsWith("PRUNED")
                || status.startsWith("REVOKED") || status.startsWith("PAUSED")) return status;
        return "IDEA_WAIT_FUTURE_OOS";
    }

    private static double finite(double value) { return Double.isNaN(value) || Double.isInfinite(value) ? 0.0d : value; }
    private static double clamp01(double value) { return Math.max(0.0d, Math.min(1.0d, finite(value))); }
    private static String hex(double value) { return Long.toHexString(Double.doubleToLongBits(finite(value))); }
    private static double fromHex(String value) {
        try {
            return finite(Double.longBitsToDouble(Long.parseUnsignedLong(safe(value), 16)));
        } catch (Throwable failure) {
            throw new IllegalArgumentException("R915_RESEARCH_EVIDENCE_NUMBER", failure);
        }
    }
    private static int integer(String value) {
        try { return Integer.parseInt(safe(value)); }
        catch (Throwable failure) {
            throw new IllegalArgumentException("R915_RESEARCH_EVIDENCE_INTEGER", failure);
        }
    }
    private static String percent(double value) { return String.format(Locale.US, "%.2f%%", 100.0d * value); }
    private static String safe(String value) { return value == null ? "" : value.trim(); }
}
