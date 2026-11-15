package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Deterministic evolutionary challenger factory from structured, falsifiable Research findings. */
public final class StrategyChallengerFactoryEngine {
    public enum CandidateType {
        PARAMETER,
        FORMULA,
        FEATURE,
        EXPERT,
        EXPERT_GATE,
        MODEL,
        CALIBRATION,
        CALIBRATOR,
        POLICY,
        RANK_LOSS,
        WINDOW,
        DOMAIN_CERT,
        RUNTIME,
        CACHE,
        SQLITE_INDEX,
        UI
    }

    private StrategyChallengerFactoryEngine() {}

    public static List<CandidateSpec> generate(FindingBundle finding, ResearchDataset dataset) {
        if (finding == null || dataset == null || !dataset.valid())
            return Collections.emptyList();
        if (safe(finding.counterEvidenceRequired).length() == 0)
            return Collections.singletonList(
                    CandidateSpec.rejected(finding, dataset, "COUNTER_EVIDENCE_REQUIRED"));
        // R900: creation is continuous and auditable from the first legal frozen OOS issue.
        // N<120 candidates remain EXPLORATORY_COLLECT_ONLY and are forbidden from any effect claim.
        ArrayList<CandidateSpec> output = new ArrayList<CandidateSpec>();
        output.addAll(generateFormulaCandidates(finding, dataset));
        // Method-family creation is allowed immediately as an auditable design asset.  These
        // plans remain non-executable until a PIT-trained artifact passes future OOS gates.
        output.addAll(generateModelCandidates(finding, dataset));
        /* Exploratory formulas can be generated immediately. Learned gates, calibrators and
         * neural artifacts retain the N=120 screening boundary and never authorize REAL. */
        if (dataset.issueN >= 120) {
            output.addAll(generateExpertCandidates(finding, dataset));
            output.addAll(generateCalibrationCandidates(finding, dataset));
        }
        return Collections.unmodifiableList(output);
    }

    public static CandidateSpec fromHypothesis(
            FindingBundle finding,
            ResearchDataset dataset,
            CandidateType type,
            String canonicalSpec,
            boolean runtimeExecutable,
            long seed) {
        return fromHypothesis(
                finding, dataset, type, canonicalSpec, runtimeExecutable, seed, 0.0d);
    }

    private static CandidateSpec fromHypothesis(
            FindingBundle finding,
            ResearchDataset dataset,
            CandidateType type,
            String canonicalSpec,
            boolean runtimeExecutable,
            long seed,
            double complexity) {
        if (finding == null || dataset == null || type == null)
            throw new IllegalArgumentException("R811_CANDIDATE_INPUT");
        String changed = safe(finding.changedSymbols);
        String forbidden = safe(finding.forbiddenSymbols);
        if (changed.indexOf("AppConfig.R36_REAL_BET_UNLOCK") >= 0
                || changed.indexOf("DecisionGateEngine") >= 0
                || changed.indexOf("SettlementEngine") >= 0)
            return CandidateSpec.rejected(
                    finding, dataset, "FORBIDDEN_PRODUCTION_SYMBOL");
        String canonical =
                type
                        + "|"
                        + safe(canonicalSpec)
                        + "|parent="
                        + finding.parentChampionHash
                        + "|hypothesis="
                        + finding.hypothesis
                        + "|counter="
                        + finding.counterEvidenceRequired
                        + "|changed="
                        + changed
                        + "|forbidden="
                        + forbidden
                        + "|seed="
                        + seed
                        + "|dataset="
                        + dataset.datasetHash;
        String hash = V51ChecksumEngine.sha256(canonical);
        return new CandidateSpec(
                "R811-CAND-" + hash.substring(0, 24),
                hash,
                type,
                finding.parentChampionHash,
                finding.hypothesis,
                finding.counterEvidenceRequired,
                changed,
                forbidden,
                seed,
                dataset.datasetHash,
                safe(canonicalSpec),
                Math.max(0.0d, complexity),
                runtimeExecutable,
                runtimeExecutable
                        ? (dataset.issueN >= 120
                                ? "RESEARCH_READY"
                                : "EXPLORATORY_COLLECT_ONLY")
                        : "BUILD_REQUIRED_OFFLINE_TRAINING",
                0,
                "DIRECT_SPEC",
                safe(finding.parentChampionHash),
                featureDependencies(canonicalSpec));
    }

    /** Seed to mutation to crossover produces bounded ASTs, never arbitrary source execution. */
    public static List<CandidateSpec> generateFormulaCandidates(
            FindingBundle finding, ResearchDataset dataset) {
        ResearchCandidateFormulaLibraryR3611.validateAll();
        ArrayList<ResearchStrategyDslEngine.Node> population =
                new ArrayList<ResearchStrategyDslEngine.Node>();
        ArrayList<Integer> generations = new ArrayList<Integer>();
        ArrayList<String> operators = new ArrayList<String>();
        ArrayList<String> parents = new ArrayList<String>();

        // R820: the 60 explicit auditable strategy formulas are the real generation seeds. R819
        // carried the library in source but the live factory silently used only six duplicates.
        for (int i = 0; i < ResearchCandidateFormulaLibraryR3611.SEEDS.length; i++) {
            String[] seed = ResearchCandidateFormulaLibraryR3611.SEEDS[i];
            population.add(ResearchStrategyDslEngine.parse(seed[2]));
            generations.add(Integer.valueOf(0));
            operators.add("SEED_" + seed[0] + '_' + seed[1]);
            parents.add("PRODUCTION_FROZEN");
        }

        long root =
                ResearchDeterministicRngR811.seed(
                        finding.failureLayer
                                + '|'
                                + finding.hypothesis
                                + '|'
                                + dataset.datasetHash);
        // Bounded deterministic evolution beyond the 60 human-auditable seeds.
        int[] mutateIndexes = new int[] {2, 14, 39, 41, 57, 58};
        for (int i = 0; i < mutateIndexes.length; i++) {
            int idx = mutateIndexes[i] % ResearchCandidateFormulaLibraryR3611.SEEDS.length;
            population.add(ResearchStrategyDslEngine.mutate(population.get(idx), root + 1000L + i));
            generations.add(Integer.valueOf(1));
            operators.add("MUTATION");
            parents.add(ResearchCandidateFormulaLibraryR3611.SEEDS[idx][0]);
        }
        int[][] pairs = new int[][] {{2, 39}, {14, 57}, {40, 58}, {28, 52}};
        for (int i = 0; i < pairs.length; i++) {
            int a = pairs[i][0], b = pairs[i][1];
            population.add(
                    ResearchStrategyDslEngine.crossover(
                            population.get(a), population.get(b), root + 2000L + i));
            generations.add(Integer.valueOf(2));
            operators.add("CROSSOVER");
            parents.add(
                    ResearchCandidateFormulaLibraryR3611.SEEDS[a][0]
                            + ","
                            + ResearchCandidateFormulaLibraryR3611.SEEDS[b][0]);
        }

        // R903 structural creation: failure-conditioned, reproducible topologies that are not
        // confined to the fixed seed list or to one cross-position/intersection idea.
        List<ResearchStrategyDslEngine.Node> novel =
                ResearchNovelFormulaSynthesizerR903.create(
                        finding.failureLayer, finding.hypothesis, dataset.datasetHash, 20);
        for (int i = 0; i < novel.size(); i++) {
            population.add(novel.get(i));
            generations.add(Integer.valueOf(1));
            operators.add("R903_STRUCTURAL_SYNTHESIS");
            parents.add("FAILURE_LAYER+DATASET_HASH");
        }

        ArrayList<CandidateSpec> output = new ArrayList<CandidateSpec>();
        java.util.HashSet<String> seen = new java.util.HashSet<String>();
        for (int i = 0; i < population.size(); i++) {
            ResearchStrategyDslEngine.Node ast = population.get(i);
            String canonical = ResearchStrategyDslEngine.canonical(ast);
            if (!seen.add(canonical)) continue;
            CandidateSpec base =
                    fromHypothesis(
                            finding,
                            dataset,
                            CandidateType.FORMULA,
                            canonical,
                            true,
                            root + i,
                            ResearchStrategyDslEngine.complexity(ast));
            output.add(
                    base.withEvolution(
                            generations.get(i).intValue(),
                            operators.get(i),
                            parents.get(i)));
        }
        return output;
    }

    /** G0 through G5 preserve every expert; they only challenge Research-side weighting. */
    public static List<CandidateSpec> generateExpertCandidates(
            FindingBundle finding, ResearchDataset dataset) {
        ArrayList<CandidateSpec> output = new ArrayList<CandidateSpec>();
        long root =
                ResearchDeterministicRngR811.seed(
                        "EXPERT_GATE|" + finding.hypothesis + '|' + dataset.datasetHash);
        for (int i = 0; i <= 5; i++)
            output.add(
                    fromHypothesis(
                            finding,
                            dataset,
                            CandidateType.EXPERT_GATE,
                            "G"
                                    + i
                                    + "[experts=9,drop=0,regret=predraw_only,diversity="
                                    + (i == 5)
                                    + "]",
                            false,
                            root + i));
        return output;
    }

    public static List<CandidateSpec> generateCalibrationCandidates(
            FindingBundle finding, ResearchDataset dataset) {
        ArrayList<CandidateSpec> output = new ArrayList<CandidateSpec>();
        long root = ResearchDeterministicRngR811.seed("CAL|" + dataset.datasetHash);
        output.add(
                fromHypothesis(
                        finding,
                        dataset,
                        CandidateType.CALIBRATOR,
                        "PLATT[time_ordered=true,l2=1e-4]",
                        false,
                        root));
        output.add(
                fromHypothesis(
                        finding,
                        dataset,
                        CandidateType.CALIBRATOR,
                        "TEMPERATURE[grid=0.75,1.0,1.25,1.5]",
                        false,
                        root + 1));
        return output;
    }

    public static List<CandidateSpec> generateModelCandidates(
            FindingBundle finding, ResearchDataset dataset) {
        ArrayList<CandidateSpec> output = new ArrayList<CandidateSpec>();
        List<ResearchMethodGraphSynthesizerR903.Blueprint> blueprints =
                ResearchMethodGraphSynthesizerR903.create(
                        finding.failureLayer,
                        finding.hypothesis,
                        dataset.datasetHash,
                        dataset.issueN);
        long seed = ResearchDeterministicRngR811.seed("METHOD_GRAPH|" + dataset.datasetHash);
        for (int i = 0; i < blueprints.size(); i++) {
            ResearchMethodGraphSynthesizerR903.Blueprint blueprint = blueprints.get(i);
            CandidateSpec base =
                    fromHypothesis(
                            finding,
                            dataset,
                            CandidateType.MODEL,
                            blueprint.canonicalSpec,
                            false,
                            seed + i,
                            40.0d + i);
            output.add(
                    base.withEvolution(
                            1,
                            "R903_METHOD_GRAPH_SYNTHESIS_" + blueprint.family,
                            "FAILURE_HYPOTHESIS+METHOD_GRAMMAR"));
        }
        return output;
    }

    public static final class FindingBundle {
        public final String failureLayer,
                hypothesis,
                counterEvidenceRequired,
                parentChampionHash,
                changedSymbols,
                forbiddenSymbols;

        public FindingBundle(
                String layer,
                String hypothesis,
                String counter,
                String parent,
                String changed,
                String forbidden) {
            failureLayer = safe(layer);
            this.hypothesis = safe(hypothesis);
            counterEvidenceRequired = safe(counter);
            parentChampionHash = safe(parent);
            changedSymbols = safe(changed);
            forbiddenSymbols = safe(forbidden);
        }
    }

    public static final class ResearchDataset {
        public final String datasetHash, dataUntilIssue;
        public final int issueN;

        public ResearchDataset(String hash, String until, int n) {
            datasetHash = safe(hash);
            dataUntilIssue = safe(until);
            issueN = Math.max(0, n);
        }

        boolean valid() {
            return datasetHash.length() == 64
                    && TargetIssueResolver.isValid(dataUntilIssue)
                    && issueN > 0;
        }
    }

    public static final class CandidateSpec {
        public final String candidateId,
                candidateHash,
                parentChampionHash,
                hypothesis,
                counterEvidenceRequired,
                changedSymbols,
                forbiddenSymbols,
                datasetHash,
                canonicalSpec,
                status,
                geneticOperator,
                parentFormulaIds,
                featureDependencies;
        public final CandidateType type;
        public final long seed;
        public final double complexity;
        public final boolean runtimeExecutable;
        public final int generation;

        CandidateSpec(
                String id,
                String hash,
                CandidateType type,
                String parent,
                String hypothesis,
                String counter,
                String changed,
                String forbidden,
                long seed,
                String dataset,
                String spec,
                double complexity,
                boolean runtime,
                String status,
                int generation,
                String operator,
                String parentFormulaIds,
                String featureDependencies) {
            candidateId = id;
            candidateHash = hash;
            this.type = type;
            parentChampionHash = safe(parent);
            this.hypothesis = safe(hypothesis);
            counterEvidenceRequired = safe(counter);
            changedSymbols = safe(changed);
            forbiddenSymbols = safe(forbidden);
            this.seed = seed;
            datasetHash = safe(dataset);
            canonicalSpec = safe(spec);
            this.complexity = Math.max(0.0d, complexity);
            runtimeExecutable = runtime;
            this.status = status;
            this.generation = Math.max(0, generation);
            geneticOperator = safe(operator);
            this.parentFormulaIds = safe(parentFormulaIds);
            this.featureDependencies = safe(featureDependencies);
        }

        CandidateSpec withEvolution(int generation, String operator, String parents) {
            return new CandidateSpec(
                    candidateId,
                    candidateHash,
                    type,
                    parentChampionHash,
                    hypothesis,
                    counterEvidenceRequired,
                    changedSymbols,
                    forbiddenSymbols,
                    seed,
                    datasetHash,
                    canonicalSpec,
                    complexity,
                    runtimeExecutable,
                    status,
                    generation,
                    operator,
                    parents,
                    featureDependencies);
        }

        static CandidateSpec rejected(
                FindingBundle finding, ResearchDataset dataset, String reason) {
            String hash =
                    V51ChecksumEngine.sha256(
                            "REJECT|"
                                    + safe(finding == null ? "" : finding.hypothesis)
                                    + '|'
                                    + safe(dataset == null ? "" : dataset.datasetHash)
                                    + '|'
                                    + reason);
            return new CandidateSpec(
                    "R811-REJECT-" + hash.substring(0, 16),
                    hash,
                    CandidateType.RUNTIME,
                    "",
                    finding == null ? "" : finding.hypothesis,
                    finding == null ? "" : finding.counterEvidenceRequired,
                    "",
                    "",
                    0L,
                    dataset == null ? "" : dataset.datasetHash,
                    "",
                    0.0d,
                    false,
                    "REJECT:" + reason,
                    0,
                    "REJECT",
                    "",
                    "");
        }
    }

    private static String featureDependencies(String specification) {
        String[] names =
                new String[] {
                    "rank_score",
                    "raw_action_p",
                    "third_margin",
                    "entropy",
                    "stability",
                    "window_consensus",
                    "assignment_support"
                };
        StringBuilder output = new StringBuilder();
        String value = safe(specification);
        for (int i = 0; i < names.length; i++)
            if (value.indexOf(names[i]) >= 0) {
                if (output.length() > 0) output.append(',');
                output.append(names[i]);
            }
        return output.length() == 0 ? "SPEC_DEFINED" : output.toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
