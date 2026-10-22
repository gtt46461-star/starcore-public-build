package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.List;

/** Host contract for R914 algorithm semantics, evidence lifecycle and two-denominator truth. */
public final class R914RuntimeContractHarness {
    public static void main(String[] args) {
        behaviorNoveltyContract();
        stateAndNameContract();
        futureEvidenceContract();
        coverageContract();
        System.out.println("R914_RUNTIME_CONTRACT=PASS");
    }

    private static void behaviorNoveltyContract() {
        List<AlgorithmGenomeR908.Insn> scoreA = new ArrayList<AlgorithmGenomeR908.Insn>();
        scoreA.add(i(AlgorithmGenomeR908.Op.LOAD_X, 0, 0, 0, 0));
        scoreA.add(i(AlgorithmGenomeR908.Op.LOAD_X, 1, 1, 0, 0));
        scoreA.add(i(AlgorithmGenomeR908.Op.LOAD_S, 2, 0, 0, 0));
        scoreA.add(i(AlgorithmGenomeR908.Op.ADD, 3, 0, 1, 0));
        scoreA.add(i(AlgorithmGenomeR908.Op.ADD, 4, 3, 2, 0));
        scoreA.add(i(AlgorithmGenomeR908.Op.TANH, 5, 4, 0, 0));
        scoreA.add(i(AlgorithmGenomeR908.Op.MIX, 6, 5, 5, 0.5d));
        List<AlgorithmGenomeR908.Insn> scoreB = new ArrayList<AlgorithmGenomeR908.Insn>(scoreA);
        scoreB.add(i(AlgorithmGenomeR908.Op.CONST, 15, 0, 0, 0.777d)); // dead code

        List<AlgorithmGenomeR908.Insn> update = new ArrayList<AlgorithmGenomeR908.Insn>();
        update.add(i(AlgorithmGenomeR908.Op.LOAD_S, 0, 0, 0, 0));
        update.add(i(AlgorithmGenomeR908.Op.LOAD_X, 1, 9, 0, 0));
        update.add(i(AlgorithmGenomeR908.Op.LOAD_X, 2, 0, 0, 0));
        update.add(i(AlgorithmGenomeR908.Op.SUB, 3, 1, 2, 0));
        update.add(i(AlgorithmGenomeR908.Op.MIX, 4, 0, 3, 0.2d));
        update.add(i(AlgorithmGenomeR908.Op.WRITE_S, 4, 0, 0, 0));
        update.add(i(AlgorithmGenomeR908.Op.LOAD_X, 5, 13, 0, 0));

        AlgorithmGenomeR908 a = new AlgorithmGenomeR908(1, "A", scoreA, update, 5);
        AlgorithmGenomeR908 b = new AlgorithmGenomeR908(1, "B", scoreB, update, 5);
        require(a.algorithmGrade() && b.algorithmGrade(), "ALGORITHM_GRADE");
        require(!a.structuralHash.equals(b.structuralHash), "STRUCTURAL_HASH_DISTINCT");
        require(AlgorithmGenomeVmR908.behaviorHash(a).equals(AlgorithmGenomeVmR908.behaviorHash(b)),
                "BEHAVIORALLY_EQUIVALENT_COLLAPSE");
        System.out.println("R914_BEHAVIOR_NOVELTY=PASS|STRUCTURE_DIFFERENT=1|BEHAVIOR_SAME=1");
    }

    private static void stateAndNameContract() {
        AlgorithmGenomeR908 genome = AlgorithmGenomeR908.seed(202608291337L, 3, "PRIMITIVE_SEARCH");
        require(genome.algorithmGrade(), "SEED_GRADE");
        require(genome.chineseName().contains("算法·第03代"), "CHINESE_GENERATION_NAME");
        require(genome.mechanismDescriptionZh().contains("观察="), "MECHANISM_OBSERVATION");
        require(genome.mechanismDescriptionZh().contains("结算更新="), "MECHANISM_UPDATE");
        AlgorithmGenomeVmR908.State state = new AlgorithmGenomeVmR908.State();
        double[] x = new double[AlgorithmGenomeR908.INPUTS];
        for (int n = 0; n < 9; n++) x[n] = 0.1d + n * 0.07d;
        String before = state.encode();
        double p = AlgorithmGenomeVmR908.score(genome, state, 4, x);
        require(before.equals(state.encode()), "PREDRAW_READ_ONLY");
        x[9] = 1.0d;
        x[10] = 0.18d;
        x[11] = p;
        x[12] = 0.44d;
        x[13] = 1.0d - p;
        AlgorithmGenomeVmR908.update(genome, state, 4, x);
        require(state.updates == 1L && !before.equals(state.encode()), "SETTLEMENT_UPDATE");
        require(state.encode().startsWith("R914STATE2|"), "THREE_LEVEL_STATE_CODEC");
        AlgorithmGenomeVmR908.State roundtrip = AlgorithmGenomeVmR908.State.decode(state.encode());
        require(state.encode().equals(roundtrip.encode()), "STATE_ROUNDTRIP");
        System.out.println("R914_ALGORITHM_STATE_NAME=PASS|NAME=" + genome.chineseName()
                + "|CELL=" + genome.mechanismCell());
    }

    private static void futureEvidenceContract() {
        AlgorithmEvidencePolicyR912.Decision good = AlgorithmEvidencePolicyR912.evaluate(
                260, 104, 0.35d, 10, 0.20d, 0.08d, 0.95d, 180, 0.04d, 0.30d);
        AlgorithmEvidencePolicyR912.Decision badEce = AlgorithmEvidencePolicyR912.evaluate(
                260, 104, 0.35d, 10, 0.20d, 0.30d, 0.95d, 180, 0.04d, 0.30d);
        AlgorithmEvidencePolicyR912.Decision badCoverage = AlgorithmEvidencePolicyR912.evaluate(
                260, 104, 0.35d, 10, 0.20d, 0.08d, 0.40d, 180, 0.04d, 0.30d);
        require("CHALLENGER_FUTURE_OOS".equals(good.status), "MULTI_METRIC_CHALLENGER");
        require("PRUNED_EVIDENCE_R912".equals(badEce.status)
                && "ECE_UNCALIBRATED".equals(badEce.reason), "ECE_PRUNE");
        require("OBSERVING_FUTURE_OOS".equals(badCoverage.status)
                && "SYSTEM_CAPTURE_GAP_DO_NOT_PRUNE".equals(badCoverage.reason), "SYSTEM_COVERAGE_OBSERVATION");
        System.out.println("R914_FUTURE_EVIDENCE=PASS|GOOD=" + good.status
                + "|BAD_ECE=" + badEce.reason + "|BAD_COVERAGE=" + badCoverage.reason);
    }

    private static void coverageContract() {
        SettlementCoverageTruthR906 truth = new SettlementCoverageTruthR906(
                100, 96, 10, 1, 4, 110, 106, 4);
        require(truth.drawTaskTotal == 110 && truth.drawTaskTerminal == 106
                && truth.drawTaskOpen == 4, "DRAW_TASK_DENOMINATOR");
        require(truth.predictedEligible == 100 && truth.settledPredictions == 96
                && truth.openEligible == 4, "PREDICTION_DENOMINATOR");
        require(truth.canonical().contains("drawTaskTotal=110")
                && truth.canonical().contains("predictedEligible=100"), "COVERAGE_CANONICAL");
        System.out.println("R914_TWO_DENOMINATOR_TRUTH=PASS|" + truth.canonical());
    }

    private static AlgorithmGenomeR908.Insn i(
            AlgorithmGenomeR908.Op op, int dst, int a, int b, double k) {
        return new AlgorithmGenomeR908.Insn(op, dst, a, b, k);
    }

    private static void require(boolean condition, String code) {
        if (!condition) throw new IllegalStateException("R914_FAIL:" + code);
    }
}
