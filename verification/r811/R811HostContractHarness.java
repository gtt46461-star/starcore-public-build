package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Dependency-free host checks for R3.6.8-811 behavior that does not require Android hardware. */
public final class R811HostContractHarness {
    private static int assertions;

    public static void main(String[] args) {
        identityAndRedlineContract();
        issueAndBackfillContract();
        realtimePressureAndBudgetContract();
        actionAndAssignmentContract();
        drawCanonicalizationContract();
        dslAndSecureRuntimeContract();
        advancedResearchPlanContract();
        artifactContract();
        neuralArtifactParityContract();
        challengerAndPromotionContract();
        certificationDenominatorContract();
        holdoutContract();
        researchStatisticsContract();
        adaptiveLearnerContract();
        ownerContract();
        System.out.println(
                "R811_HOST_CONTRACTS=PASS|ASSERTIONS="
                        + assertions
                        + "|IDENTITY,ISSUE_ROLLOVER,BACKFILL,REALTIME_PRESSURE,BUDGET,1200_ACTIONS,BIJECTION,DRAW_CONFLICT,DSL_SECURITY,PRIMITIVE_REGISTRY,PERMUTATION,THIRD_BOUNDARY,GROUPED_WALK_FORWARD,BH_FDR,NEURAL_ADAM,EXPERT_SIDECAR,GATE_CHALLENGERS,ARTIFACT_CODEC,CANDIDATE_BOUNDARY,CERTIFICATION_DENOMINATOR,HOLDOUT,ISSUE_CLUSTER,ADAPTIVE_OOS,OWNER_AUTHORITY");
    }

    private static void identityAndRedlineContract() {
        require("V57.16.4-R4.0.4-904".equals(V51BuildManifest.VERSION_NAME), "VERSION_NAME");
        require(V51BuildManifest.VERSION_CODE == 577200904, "VERSION_CODE");
        require(AppConfig.DB_VERSION == 36 && AppConfig.R811_SCHEMA_REVISION == 1, "DB36_REV1");
        require(V51BuildManifest.isIdentityConsistent(), "IDENTITY_CONSISTENT");
        require(!V51BuildManifest.MODEL_PROVEN, "MODEL_NOT_PROVEN");
        require(
                V51BuildManifest.SHADOW_ONLY
                        && V51BuildManifest.REAL_BET_HARD_DISABLED
                        && !V51BuildManifest.CANDIDATE_REAL_AUTH_ENABLED,
                "REAL_HARD_LOCK");
        require(!AppConfig.R36_REAL_BET_UNLOCK && !AppConfig.R35_REAL_AUTO_UNLOCK_ENABLED, "NO_UNLOCK");
        require(!V51BuildManifest.mayAuthorizeRealBet(), "MANIFEST_NO_REAL");
    }

    private static void issueAndBackfillContract() {
        require(
                "202608110001".equals(TargetIssueResolver.next("202608101440").targetIssue),
                "ISSUE_ROLLOVER");
        require(TargetIssueResolver.distance("202608101440", "202608110001") == 1, "ISSUE_DISTANCE");
        require(
                !ApiClient.needsLimitedBackfill("202608110001", "202608101440"),
                "NO_FALSE_ROLLOVER_BACKFILL");
        require(
                ApiClient.needsLimitedBackfill("202608110002", "202608101440"),
                "TWO_ISSUE_BACKFILL");
        require(!ApiClient.needsLimitedBackfill("bad", "202608101440"), "INVALID_BACKFILL_INPUT");
    }

    private static void realtimePressureAndBudgetContract() {
        RealtimePressureGateR811.resetForTests();
        require(!RealtimePressureGateR811.isUnderPressure(), "PRESSURE_RESET");
        RealtimePressureGateR811.beginAnalysis();
        require(RealtimePressureGateR811.isUnderPressure(), "ANALYSIS_PRESSURE");
        RealtimePressureGateR811.endAnalysis();
        RealtimePressureGateR811.endAnalysis();
        require(!RealtimePressureGateR811.isUnderPressure(), "PRESSURE_FLOOR_ZERO");
        RealtimePressureGateR811.beginSettlement();
        require(RealtimePressureGateR811.snapshot().settlementDepth == 1, "SETTLEMENT_PRESSURE");
        RealtimePressureGateR811.endSettlement();
        RealtimePressureGateR811.pulseLiveRequest();
        RealtimePressureGateR811.Snapshot pulse = RealtimePressureGateR811.snapshot();
        require(pulse.underPressure && pulse.lastLivePulseMonoMs > 0L, "LIVE_PULSE");
        require(
                !RealtimePressureGateR811.isUnderPressureAt(
                        pulse.lastLivePulseMonoMs + AppConfig.R811_LIVE_QUIET_MS),
                "QUIET_BOUNDARY");
        RealtimePressureGateR811.resetForTests();
        ResearchBudgetGovernorR811.Metrics good =
                new ResearchBudgetGovernorR811.Metrics(0, 0, true, 10L, false);
        require(ResearchBudgetGovernorR811.canRunSlice(good), "BUDGET_ADMIT");
        ResearchBudgetGovernorR811.Slice slice = ResearchBudgetGovernorR811.beginSlice(good);
        require(slice.admitted(), "SLICE_ADMITTED");
        require(
                ResearchBudgetGovernorR811.shouldYield(
                        slice, AppConfig.R811_RESEARCH_MAX_CANDIDATES_PER_SLICE),
                "SLICE_CANDIDATE_YIELD");
        ResearchBudgetGovernorR811.Result result =
                ResearchBudgetGovernorR811.finishSlice(slice, 1);
        require(result.withinBudget, "SLICE_FINISH");
        require(
                !ResearchBudgetGovernorR811.canRunSlice(
                        new ResearchBudgetGovernorR811.Metrics(2, 0, true, 10L, false)),
                "SETTLEMENT_LAG_BLOCKS_RESEARCH");
        require(
                !ResearchBudgetGovernorR811.canRunSlice(
                                new ResearchBudgetGovernorR811.Metrics(
                                        0, 0, true, 4000L, false))
                        && !ResearchBudgetGovernorR811.canRunSlice(
                                new ResearchBudgetGovernorR811.Metrics(
                                        0, 0, true, 10L, true)),
                "LIVE_P95_AND_STORAGE_BLOCK_RESEARCH");
    }

    private static void actionAndAssignmentContract() {
        K3ActionSpaceEngine.ActionIdentity[] identities =
                K3ActionSpaceEngine.enumerateIdentityUniverse();
        require(identities.length == 1200, "ACTION_1200");
        require(identities[0].globalOrdinal == 0, "ACTION_FIRST");
        require(identities[1199].globalOrdinal == 1199, "ACTION_LAST");
        require(K3ActionSpaceEngine.combinationOrdinal(8, 9, 10) == 119, "COMBINATION_120");
        double[][] q = new double[10][10];
        for (int p = 0; p < 10; p++)
            for (int n = 0; n < 10; n++) q[p][n] = 0.02d + (p == n ? 0.50d : n * 0.001d);
        CrossPositionAssignmentResearchEngine.AssignmentSupport support =
                CrossPositionAssignmentResearchEngine.kBestAssignmentSupport(q, 16);
        require(support.assignments.size() == 16, "ASSIGNMENT_K");
        for (CrossPositionAssignmentResearchEngine.Path path : support.assignments)
            require(path.legal(), "ASSIGNMENT_BIJECTION");
        for (int p = 0; p < 10; p++) {
            double row = 0.0d;
            for (int n = 0; n < 10; n++) row += support.support[p][n];
            require(Math.abs(row - 1.0d) < 1e-9d, "ASSIGNMENT_SUPPORT_" + p);
        }
        q[1][0] = 0.31d;
        q[2][0] = 0.44d;
        q[3][0] = 0.27d;
        require(
                Math.abs(CrossPositionAssignmentResearchEngine.collision(q, 1, 1) - 0.44d)
                        < 1e-12d,
                "COLLISION_IS_MAX_OTHER");
        double[][] uniformLocation = new double[10][10];
        for (int p = 0; p < 10; p++)
            for (int n = 0; n < 10; n++) uniformLocation[p][n] = 0.1d;
        require(
                Math.abs(
                                CrossPositionAssignmentResearchEngine.locationEntropy(
                                                uniformLocation, 1)
                                        - 1.0d)
                        < 1e-12d,
                "LOCATION_ENTROPY_FIXTURE");
        double[][] exclusive = new double[10][10];
        for (int p = 0; p < 10; p++) exclusive[p][0] = p == 0 ? 0.52d : 0.02d;
        require(
                Math.abs(
                                CrossPositionAssignmentResearchEngine.exclusivity(
                                                exclusive, 1, 1)
                                        - 0.50d)
                        < 1e-12d,
                "POSITION_EXCLUSIVITY_FIXTURE");
    }

    private static void drawCanonicalizationContract() {
        DrawCanonicalizerR811 canonicalizer = new DrawCanonicalizerR811();
        Models.Draw first = draw("202608100001", 0);
        Models.Draw same = draw("202608100001", 0);
        Models.Draw conflict = draw("202608100001", 1);
        require(
                canonicalizer.observeRaw("LATEST", "raw-a", first).confirmed,
                "DRAW_FIRST_CONFIRMED");
        require(
                canonicalizer.observeRaw("HISTORY", "raw-b", same).confirmed,
                "DRAW_MULTI_SOURCE_CONFIRMED");
        DrawCanonicalizerR811.Resolution quarantined =
                canonicalizer.observeRaw("DATE", "raw-c", conflict);
        require(
                quarantined.conflict
                        && !quarantined.confirmed
                        && "DRAW_CONFLICT_QUARANTINED".equals(quarantined.status),
                "DRAW_CONFLICT_QUARANTINE");
        require(canonicalizer.confirmedDraw(first.issue) == null, "CONFLICT_NOT_CANONICAL");
    }

    private static void dslAndSecureRuntimeContract() {
        RealtimePressureGateR811.resetForTests();
        String formula = "ADD(FEATURE(rank_margin),CONST(0.5))";
        ResearchStrategyDslEngine.Node one = ResearchStrategyDslEngine.parse(formula);
        ResearchStrategyDslEngine.Node two = ResearchStrategyDslEngine.parse(formula);
        require(ResearchStrategyDslEngine.hash(one).equals(ResearchStrategyDslEngine.hash(two)), "DSL_HASH");
        Map<String, Double> features = new HashMap<String, Double>();
        features.put("rank_margin", Double.valueOf(0.75d));
        require(Math.abs(ResearchStrategyDslEngine.evaluate(one, features) - 1.25d) < 1e-12d, "DSL_EVAL");
        ResearchStrategyDslEngine.Node safeDiv =
                ResearchStrategyDslEngine.parse(
                        "SAFE_DIV(FEATURE(rank_margin),CONST(0),CONST(0.01))");
        require(
                Math.abs(ResearchStrategyDslEngine.evaluate(safeDiv, features) - 75.0d)
                        < 1e-12d,
                "DSL_SAFE_DIV_EPS");
        ResearchStrategyDslEngine.Node temporal =
                ResearchStrategyDslEngine.parse("EMA(FEATURE(rank_margin),CONST(12))");
        require(
                Math.abs(ResearchStrategyDslEngine.evaluate(temporal, features) - 0.75d)
                        < 1e-12d,
                "DSL_TEMPORAL_ARITY");
        require(ResearchStrategyDslEngine.complexity(temporal) == 7.0d, "DSL_COMPLEXITY_EXACT");
        require(SecureCodegenGuardR811.validateAst(one).accepted, "DSL_GUARD");
        boolean forbidden = false;
        try {
            ResearchStrategyDslEngine.parse("FEATURE(actual_hit_label)");
        } catch (SecurityException expected) {
            forbidden = true;
        }
        require(forbidden, "DSL_FUTURE_FEATURE_REJECT");
        require(
                !SecureCodegenGuardR811.validateTokens("Runtime.getRuntime().exec(\"x\")").accepted,
                "CODEGEN_RUNTIME_REJECT");
        require(
                SecureCodegenGuardR811.researchPrivatePath("research/generated/Candidate.java")
                        && !SecureCodegenGuardR811.researchPrivatePath("../Production.java"),
                "CODEGEN_PATH");
        ResearchDeterministicRngR811 a = new ResearchDeterministicRngR811(811L);
        ResearchDeterministicRngR811 b = new ResearchDeterministicRngR811(811L);
        for (int i = 0; i < 20; i++) require(a.xorshift() == b.xorshift(), "RNG_" + i);
        double[] mlp =
                ResearchModelCandidateRuntimeR811.inferTinyMlp(
                        new double[] {1.0d, 2.0d},
                        new double[][] {{0.1d, 0.2d}, {-0.2d, 0.3d}},
                        new double[] {0.0d, 0.1d},
                        new double[][] {{1.0d, -1.0d}, {-1.0d, 1.0d}},
                        new double[] {0.0d, 0.0d});
        require(mlp.length == 2 && Math.abs(mlp[0] + mlp[1] - 1.0d) < 1e-12d, "MLP_SOFTMAX");
        RealtimePressureGateR811.beginAnalysis();
        require(
                ResearchModelCandidateRuntimeR811.inferTinyMlp(
                                        new double[] {1.0d},
                                        new double[][] {{1.0d}},
                                        new double[] {0.0d},
                                        new double[][] {{1.0d}},
                                        new double[] {0.0d})
                                .length
                        == 0,
                "MODEL_YIELDS_TO_LIVE");
        RealtimePressureGateR811.endAnalysis();
    }

    private static void advancedResearchPlanContract() {
        List<String> primitives = ResearchStrategyDslEngine.primitiveRegistry();
        require(primitives.size() == 34, "PRIMITIVE_REGISTRY_34");
        require(
                ResearchStrategyDslEngine.primitiveRegistryCanonicalHash().length() == 64,
                "PRIMITIVE_REGISTRY_HASH");
        boolean registryImmutable = false;
        try {
            primitives.add("FORBIDDEN");
        } catch (UnsupportedOperationException expected) {
            registryImmutable = true;
        }
        require(registryImmutable, "PRIMITIVE_REGISTRY_IMMUTABLE");

        ResearchStrategyDslEngine.Node addLeft =
                ResearchStrategyDslEngine.parse("ADD(FEATURE(a),FEATURE(b))");
        ResearchStrategyDslEngine.Node addRight =
                ResearchStrategyDslEngine.parse("ADD(FEATURE(b),FEATURE(a))");
        require(
                ResearchStrategyDslEngine.hash(addLeft)
                        .equals(ResearchStrategyDslEngine.hash(addRight)),
                "DSL_COMMUTATIVE_CANONICAL");
        boolean depthRejected = false;
        try {
            ResearchStrategyDslEngine.parse(
                    "ABS(ABS(ABS(ABS(ABS(ABS(ABS(ABS(FEATURE(x)))))))))");
        } catch (IllegalArgumentException expected) {
            depthRejected = true;
        }
        require(depthRejected, "DSL_DEPTH_REJECT");
        Map<String, Double> canonicalFeatures = new HashMap<String, Double>();
        canonicalFeatures.put("a", Double.valueOf(0.25d));
        canonicalFeatures.put("b", Double.valueOf(0.75d));
        ResearchDslCandidateRuntimeR811.Evaluation canonicalEvaluation =
                ResearchDslCandidateRuntimeR811.evaluateFormulaCandidate(
                        "canonical",
                        ResearchStrategyDslEngine.canonical(addLeft),
                        canonicalFeatures);
        require(
                Math.abs(canonicalEvaluation.score - 1.0d) < 1e-12d
                        && "SHADOW_ONLY".equals(canonicalEvaluation.status),
                "DSL_CANONICAL_RUNTIME_EXECUTABLE");

        int[] identity = new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        int[] rotated = new int[] {2, 3, 4, 5, 6, 7, 8, 9, 10, 1};
        int[] reversed = new int[] {10, 9, 8, 7, 6, 5, 4, 3, 2, 1};
        require(
                CrossPositionAssignmentResearchEngine.numberPosition(rotated, 1) == 10
                        && CrossPositionAssignmentResearchEngine.migrationVelocity(
                                        rotated, identity, 1)
                                == 9
                        && CrossPositionAssignmentResearchEngine.migrationAcceleration(
                                        rotated, identity, identity, 1)
                                == 9,
                "MIGRATION_TRAJECTORY");
        int[] relative =
                CrossPositionAssignmentResearchEngine.relativePermutation(rotated, identity);
        require(Arrays.equals(relative, rotated), "RELATIVE_PERMUTATION");
        CrossPositionAssignmentResearchEngine.CycleFeatures cycle =
                CrossPositionAssignmentResearchEngine.cycleFeatures(relative);
        require(
                cycle.cycleCount == 1
                        && cycle.longestCycle == 10
                        && cycle.fixedPointRate == 0.0d
                        && Math.abs(
                                        CrossPositionAssignmentResearchEngine.kendallTauDistance(
                                                identity, reversed)
                                                - 1.0d)
                                < 1e-12d,
                "PERMUTATION_CYCLE_KENDALL");
        CrossPositionAssignmentResearchEngine.ThirdBoundaryAudit third =
                CrossPositionAssignmentResearchEngine.auditThirdBoundary(
                        new int[] {1, 2, 3}, 4, 4);
        require(
                !third.baselineHit
                        && third.counterfactualSwapHit
                        && third.thirdBoundaryMiss
                        && third.swappedK3.length == 3,
                "THIRD_BOUNDARY_COUNTERFACTUAL");

        double[][][] expertQ = new double[2][10][10];
        expertQ[0][0][0] = 1.0d;
        expertQ[1][0][0] = 1.0d;
        double[][] correlations = new double[2][2];
        correlations[1][0] = 0.5d;
        CrossPositionAssignmentResearchEngine.Vote vote =
                CrossPositionAssignmentResearchEngine.locationVote(
                        expertQ,
                        new double[] {2.0d, 1.0d},
                        correlations,
                        1.0d,
                        1,
                        1,
                        1);
        require(
                vote.supportExperts == 2 && Math.abs(vote.score - 2.0d) < 1e-12d,
                "DIVERSITY_ADJUSTED_LOCATION_VOTE");

        StrategyChallengerFactoryEngine.FindingBundle finding =
                new StrategyChallengerFactoryEngine.FindingBundle(
                        "THIRD",
                        "boundary candidate",
                        "future OOS falsifies",
                        V51ChecksumEngine.sha256("parent"),
                        "ResearchDslCandidateRuntimeR811",
                        "DecisionGateEngine,SettlementEngine");
        List<StrategyChallengerFactoryEngine.CandidateSpec> insufficient =
                StrategyChallengerFactoryEngine.generate(
                        finding,
                        new StrategyChallengerFactoryEngine.ResearchDataset(
                                V51ChecksumEngine.sha256("small"), "202608100011", 11));
        require(insufficient.size() >= ResearchCandidateFormulaLibraryR3611.SEEDS.length,
                "R900_CONTINUOUS_FORMULA_GENERATION_FROM_FIRST_LEGAL_OOS");
        for (int i = 0; i < insufficient.size(); i++) {
            StrategyChallengerFactoryEngine.CandidateSpec candidate = insufficient.get(i);
            if (candidate.type == StrategyChallengerFactoryEngine.CandidateType.FORMULA) {
                require(candidate.runtimeExecutable, "R900_EARLY_FORMULA_EXECUTABLE_" + i);
                require("EXPLORATORY_COLLECT_ONLY".equals(candidate.status),
                        "R900_EARLY_FORMULA_NO_EFFECT_CLAIM_" + i);
            } else {
                require(!candidate.runtimeExecutable, "R903_BLUEPRINT_MUST_NOT_EXECUTE_" + i);
                require("BUILD_REQUIRED_OFFLINE_TRAINING".equals(candidate.status),
                        "R903_BLUEPRINT_MUST_NOT_CLAIM_EFFECT_" + i);
            }
        }
        List<StrategyChallengerFactoryEngine.CandidateSpec> incubator =
                StrategyChallengerFactoryEngine.generate(
                        finding,
                        new StrategyChallengerFactoryEngine.ResearchDataset(
                                V51ChecksumEngine.sha256("incubator"), "202608100119", 119));
        require(incubator.size() >= 6, "HYPOTHESIS_INCUBATOR_FORMULAS");
        for (int i = 0; i < incubator.size(); i++) {
            StrategyChallengerFactoryEngine.CandidateSpec candidate = incubator.get(i);
            if (candidate.type == StrategyChallengerFactoryEngine.CandidateType.FORMULA) {
                require(candidate.runtimeExecutable, "INCUBATOR_RUNTIME_ONLY_" + i);
                require("EXPLORATORY_COLLECT_ONLY".equals(candidate.status),
                        "INCUBATOR_NOT_PROMOTED_" + i);
            } else {
                require(!candidate.runtimeExecutable, "INCUBATOR_BLUEPRINT_NOT_EXECUTABLE_" + i);
            }
        }
        StrategyChallengerFactoryEngine.CandidateSpec candidate =
                StrategyChallengerFactoryEngine.generate(
                                finding,
                                new StrategyChallengerFactoryEngine.ResearchDataset(
                                        V51ChecksumEngine.sha256("dataset"),
                                        "202608100240",
                                        240))
                        .get(0);

        ArrayList<TimeOrderedExperimentLabEngine.Example> examples =
                new ArrayList<TimeOrderedExperimentLabEngine.Example>();
        Map<String, Double> mutableFeatures = new HashMap<String, Double>();
        mutableFeatures.put("a", Double.valueOf(1.0d));
        TimeOrderedExperimentLabEngine.Example immutableExample =
                new TimeOrderedExperimentLabEngine.Example(
                        "202608100001", "A", 1, mutableFeatures);
        mutableFeatures.put("a", Double.valueOf(9.0d));
        require(
                immutableExample.features.get("a").doubleValue() == 1.0d,
                "FROZEN_FEATURE_COPY");
        boolean featureMapImmutable = false;
        try {
            immutableExample.features.put("b", Double.valueOf(1.0d));
        } catch (UnsupportedOperationException expected) {
            featureMapImmutable = true;
        }
        require(featureMapImmutable, "FROZEN_FEATURE_IMMUTABLE");
        for (int issue = 1; issue <= 12; issue++)
            for (int row = 0; row < 3; row++) {
                Map<String, Double> features = new HashMap<String, Double>();
                features.put("a", Double.valueOf(issue));
                examples.add(
                        new TimeOrderedExperimentLabEngine.Example(
                                "20260810" + four(issue),
                                issue % 2 == 0 ? "EVEN" : "ODD",
                                issue % 2,
                                features));
            }
        TimeOrderedExperimentLabEngine.Dataset dataset =
                new TimeOrderedExperimentLabEngine.Dataset(
                        examples, V51ChecksumEngine.sha256("grouped-dataset"));
        TimeOrderedExperimentLabEngine.Split split =
                TimeOrderedExperimentLabEngine.split(dataset);
        require(
                TimeOrderedExperimentLabEngine.sameIssueSamePartition(split),
                "ALL_ROWS_SAME_ISSUE_SAME_PARTITION");
        TimeOrderedExperimentLabEngine.Evaluator evaluator =
                new TimeOrderedExperimentLabEngine.Evaluator() {
                    public TimeOrderedExperimentLabEngine.Prediction predict(
                            TimeOrderedExperimentLabEngine.Example example) {
                        return new TimeOrderedExperimentLabEngine.Prediction(
                                true, example.label, example.label == 1 ? 1.0d : 0.0d);
                    }
                };
        TimeOrderedExperimentLabEngine.ExperimentResult experiment =
                TimeOrderedExperimentLabEngine.run(candidate, dataset, evaluator);
        require(
                TimeOrderedExperimentLabEngine.hardGate(experiment)
                        && experiment.walkForward.size() == 3,
                "NESTED_WALK_FORWARD_HARD_GATE");
        boolean[] fdr =
                TimeOrderedExperimentLabEngine.fdr(
                        new double[] {0.001d, 0.02d, 0.20d}, 0.05d);
        require(fdr[0] && fdr[1] && !fdr[2], "BH_FDR_FIXTURE");
        require(
                TimeOrderedExperimentLabEngine.coverageCv(
                                        new double[] {0.2d, 0.2d, 0.2d, 0.2d})
                                == 0.0d
                        && TimeOrderedExperimentLabEngine.blockMinimum(
                                        new double[] {0.5d, 0.4d, 0.6d, 0.7d})
                                == 0.4d,
                "COVERAGE_CV_BLOCK_MINIMUM");

        double[] xavierOne =
                ResearchModelCandidateRuntimeR811.xavierUniform(4, 3, 12, 811L);
        double[] xavierTwo =
                ResearchModelCandidateRuntimeR811.xavierUniform(4, 3, 12, 811L);
        require(Arrays.equals(xavierOne, xavierTwo), "XAVIER_DETERMINISTIC");
        ResearchModelCandidateRuntimeR811.AdamUpdate adam =
                ResearchModelCandidateRuntimeR811.adamStep(
                        new double[] {1.0d},
                        new double[] {2.0d},
                        new double[] {0.0d},
                        new double[] {0.0d},
                        1,
                        1e-3d);
        require(
                Math.abs(adam.firstMoment[0] - 0.2d) < 1e-12d
                        && Math.abs(adam.secondMoment[0] - 0.004d) < 1e-12d
                        && Math.abs(adam.weights[0] - (1.0d - 0.002d / (2.0d + 1e-8d)))
                                < 1e-12d,
                "ADAM_SINGLE_STEP");
        ResearchModelCandidateRuntimeR811.AdamUpdate clipped =
                ResearchModelCandidateRuntimeR811.adamStep(
                        new double[] {0.0d},
                        new double[] {10.0d},
                        new double[] {0.0d},
                        new double[] {0.0d},
                        1,
                        1e-3d);
        require(
                clipped.gradientNorm == 10.0d && clipped.gradientScale == 0.5d,
                "ADAM_GRADIENT_CLIP");
        double[] attention =
                ResearchModelCandidateRuntimeR811.inferAttention(
                        new double[][] {{1.0d, 0.0d}, {0.0d, 1.0d}},
                        new double[] {1.0d, 0.0d});
        require(
                attention.length == 2
                        && !Double.isNaN(attention[0])
                        && !Double.isInfinite(attention[1]),
                "ATTENTION_SHAPE_FINITE");
        double[][] identityProbability = new double[10][10];
        for (int i = 0; i < 10; i++) identityProbability[i][i] = 1.0d;
        require(
                ResearchModelCandidateRuntimeR811.permutationCrossEntropy(
                                identityProbability, identity, 0.0d, new double[0])
                        == 0.0d,
                "NEURAL_PERMUTATION_LOSS");
        ResearchDslCandidateRuntimeR811.Evaluation ftrl =
                ResearchDslCandidateRuntimeR811.evaluateFtrlCandidate(
                        "ftrl",
                        new double[] {1.0d},
                        new double[] {2.0d},
                        new double[] {0.0d},
                        1.0d,
                        1.0d,
                        0.0d,
                        0.0d);
        require(
                Math.abs(ftrl.score - 1.0d / (1.0d + Math.exp(2.0d))) < 1e-12d,
                "FTRL_HAND_FIXTURE");
        ResearchExpertOutcomeLedgerR811.Loss uniformLoss =
                ResearchExpertOutcomeLedgerR811.computeNumberLoss(new double[10], 1);
        require(
                Math.abs(uniformLoss.pActual - 0.1d) < 1e-12d
                        && Math.abs(uniformLoss.logLoss - Math.log(10.0d)) < 1e-12d
                        && Math.abs(uniformLoss.brier - 0.9d) < 1e-12d
                        && uniformLoss.actualRank == 1,
                "EXPERT_OUTCOME_HAND_FIXTURE");

        ResearchExpertPredrawSnapshotR811.Expert[] experts =
                new ResearchExpertPredrawSnapshotR811.Expert[9];
        for (int expert = 0; expert < 9; expert++) {
            double[][] score = new double[10][10];
            for (int position = 0; position < 10; position++)
                for (int number = 0; number < 10; number++)
                    score[position][number] = number * 0.01d + expert * 0.001d;
            experts[expert] =
                    new ResearchExpertPredrawSnapshotR811.Expert(
                            "EXPERT_" + expert,
                            score,
                            new double[10],
                            new double[10],
                            new double[10]);
        }
        double[][] gateWeight = new double[10][9];
        double[] effective = new double[10];
        for (int position = 0; position < 10; position++) {
            effective[position] = 9.0d;
            for (int expert = 0; expert < 9; expert++) gateWeight[position][expert] = 1.0d / 9.0d;
        }
        ResearchExpertPredrawSnapshotR811.Snapshot snapshot =
                new ResearchExpertPredrawSnapshotR811.Snapshot(
                        "202608100001",
                        "202608100002",
                        V51ChecksumEngine.sha256("history"),
                        V51ChecksumEngine.sha256("canonical"),
                        100L,
                        1,
                        1,
                        2,
                        3,
                        experts,
                        gateWeight,
                        effective,
                        "UNTRAINED_FALLBACK");
        byte[] expertBlob = ResearchExpertPredrawSnapshotR811.encode(snapshot);
        ResearchExpertPredrawSnapshotR811.Snapshot decoded =
                ResearchExpertPredrawSnapshotR811.decode(expertBlob);
        require(
                decoded.experts.length == 9
                        && Arrays.equals(
                                expertBlob,
                                ResearchExpertPredrawSnapshotR811.encode(decoded)),
                "EXPERT_PREDRAW_EXACT9_CODEC");
        Models.Draw expertDraw = draw("202608100002", 0);
        expertDraw.openTime = 200L;
        List<ResearchExpertOutcomeLedgerR811.Outcome> outcomes =
                ResearchExpertOutcomeLedgerR811.settleFromSnapshot(decoded, expertDraw);
        java.util.HashSet<String> outcomeHashes = new java.util.HashSet<String>();
        for (int i = 0; i < outcomes.size(); i++)
            outcomeHashes.add(outcomes.get(i).recordHash);
        require(
                outcomes.size() == 90 && outcomeHashes.size() == 90,
                "EXPERT_OUTCOME_EXACT90_UNIQUE");

        double[] uniform = new double[9];
        double[] quality = new double[9];
        double[] regret = new double[9];
        double[] diversity = new double[9];
        Arrays.fill(uniform, 1.0d / 9.0d);
        Arrays.fill(quality, 1.0d);
        ResearchExpertOutcomeLedgerR811.GateChallengerWeights g0 =
                ResearchExpertOutcomeLedgerR811.challengerWeights(
                        "G0",
                        uniform,
                        quality,
                        regret,
                        diversity,
                        "202608100001",
                        "202608100002");
        ResearchExpertOutcomeLedgerR811.GateChallengerWeights g1 =
                ResearchExpertOutcomeLedgerR811.challengerWeights(
                        "G1",
                        uniform,
                        quality,
                        regret,
                        diversity,
                        "202608100001",
                        "202608100002");
        ResearchExpertOutcomeLedgerR811.GateChallengerWeights g3 =
                ResearchExpertOutcomeLedgerR811.challengerWeights(
                        "G3",
                        uniform,
                        quality,
                        regret,
                        diversity,
                        "202608100001",
                        "202608100002");
        require(
                approxVector(g0.weights, uniform, 1e-12d)
                        && approxVector(g1.weights, uniform, 1e-12d)
                        && g3.hash.equals(
                                ResearchExpertOutcomeLedgerR811.challengerWeights(
                                                "G3",
                                                uniform,
                                                quality,
                                                regret,
                                                diversity,
                                                "202608100001",
                                                "202608100002")
                                        .hash),
                "G0_G1_G3_DETERMINISTIC");
        boolean futureGateRejected = false;
        try {
            ResearchExpertOutcomeLedgerR811.challengerWeights(
                    "G4",
                    uniform,
                    quality,
                    regret,
                    diversity,
                    "202608100002",
                    "202608100002");
        } catch (IllegalArgumentException expected) {
            futureGateRejected = true;
        }
        require(futureGateRejected, "G4_NO_FUTURE_STATE");

        require(
                TimeSourceR811.elapsedAt(100L, 90L) == 0L
                        && TimeSourceR811.leaseExpiredAt(200L, 100L, true)
                        && !TimeSourceR811.leaseExpiredAt(200L, 100L, false),
                "MONOTONIC_AND_SKEW_LEASE");
        require(
                BackgroundRuntimeCapabilityR811.deviceCertificateInvalidated(28, 35)
                        && !BackgroundRuntimeCapabilityR811.deviceCertificateInvalidated(28, 28),
                "TARGET_SDK_INVALIDATES_DEVICE_CERTIFICATE");
        ResearchPromotionGovernanceEngine.ArtifactPointer rollback =
                ResearchPromotionGovernanceEngine.rollbackArtifactPointer(
                        V51ChecksumEngine.sha256("current"),
                        V51ChecksumEngine.sha256("previous"),
                        "regression");
        require(
                rollback.activeArtifactHash.equals(V51ChecksumEngine.sha256("previous"))
                        && rollback.auditHash.length() == 64,
                "ROLLBACK_ARTIFACT_POINTER_RESTORE");
    }

    private static void artifactContract() {
        LinkedHashMap<String, double[]> fields = new LinkedHashMap<String, double[]>();
        fields.put("z", new double[] {1.0d, -2.0d});
        fields.put("meta", new double[] {811.0d});
        ResearchArtifactCodecR811.Artifact artifact =
                new ResearchArtifactCodecR811.Artifact("FTRL_SHADOW", "202608100001", fields);
        byte[] encoded = ResearchArtifactCodecR811.encode(artifact);
        String hash = ResearchArtifactCodecR811.hash(artifact);
        require(ResearchArtifactCodecR811.verify(encoded, hash), "ARTIFACT_VERIFY");
        ResearchArtifactCodecR811.Artifact decoded = ResearchArtifactCodecR811.decode(encoded);
        require(
                decoded.type.equals(artifact.type)
                        && decoded.fields.size() == 2
                        && decoded.fields.get("z")[1] == -2.0d,
                "ARTIFACT_ROUNDTRIP");
        byte[] tampered = encoded.clone();
        tampered[tampered.length - 1] ^= 1;
        require(!ResearchArtifactCodecR811.verify(tampered, hash), "ARTIFACT_TAMPER");
    }

    private static void neuralArtifactParityContract() {
        ResearchDeterministicRngR811 rng = new ResearchDeterministicRngR811(811L);
        LinkedHashMap<String, double[]> fields = new LinkedHashMap<String, double[]>();
        fields.put("W1", xavierSequential(rng, 96, 64, 96 * 64));
        fields.put("b1", new double[64]);
        fields.put("W2", xavierSequential(rng, 64, 32, 64 * 32));
        fields.put("b2", new double[32]);
        fields.put("W3", xavierSequential(rng, 32, 100, 32 * 100));
        fields.put("b3", new double[100]);
        fields.put("feature_mean", new double[96]);
        double[] scale = new double[96];
        Arrays.fill(scale, 1.0d);
        fields.put("feature_scale", scale);
        ResearchArtifactCodecR811.Artifact artifact =
                new ResearchArtifactCodecR811.Artifact(
                        "TINY_MLP_96_64_32_100", "202608170001", fields);
        require(
                "642327b904a2a29e6f603ac46cd11dc353d6d8b27b1026e1ce203b0b5a34df11"
                        .equals(ResearchArtifactCodecR811.hash(artifact)),
                "PYTHON_JAVA_NEURAL_ARTIFACT_BYTE_PARITY");
        ResearchModelCandidateRuntimeR811.NeuralInference inference =
                ResearchModelCandidateRuntimeR811.inferTinyMlpArtifact(
                        artifact, new double[96], "202608170002", 1.0d);
        require(
                "SHADOW_ONLY".equals(inference.status)
                        && inference.finiteAndConverged
                        && inference.projection != null
                        && inference.projection.matrix.validForDecision(),
                "TINY_MLP_ARTIFACT_SINKHORN_FINITE");
        double[][] q = inference.projection.matrix.probability;
        for (int position = 0; position < 10; position++) {
            double row = 0.0d;
            for (int number = 0; number < 10; number++) row += q[position][number];
            require(Math.abs(row - 1.0d) < 1e-8d, "TINY_MLP_SINKHORN_ROW_" + position);
        }
        for (int number = 0; number < 10; number++) {
            double column = 0.0d;
            for (int position = 0; position < 10; position++)
                column += q[position][number];
            require(
                    Math.abs(column - 1.0d) < 1e-8d,
                    "TINY_MLP_SINKHORN_COLUMN_" + number);
        }
        boolean pitRejected = false;
        try {
            ResearchModelCandidateRuntimeR811.inferTinyMlpArtifact(
                    artifact, new double[96], "202608170001", 1.0d);
        } catch (IllegalArgumentException expected) {
            pitRejected = true;
        }
        require(pitRejected, "TINY_MLP_TRAINED_UNTIL_PIT_REJECT");
    }

    private static void challengerAndPromotionContract() {
        String datasetHash = V51ChecksumEngine.sha256("R811_DATASET");
        StrategyChallengerFactoryEngine.ResearchDataset dataset =
                new StrategyChallengerFactoryEngine.ResearchDataset(
                        datasetHash, "202608100240", 240);
        StrategyChallengerFactoryEngine.FindingBundle finding =
                new StrategyChallengerFactoryEngine.FindingBundle(
                        "CALIBRATION",
                        "rank margin under-confident",
                        "future OOS disproves lift",
                        V51ChecksumEngine.sha256("PARENT"),
                        "ResearchDslCandidateRuntimeR811",
                        "DecisionGateEngine,SettlementEngine");
        List<StrategyChallengerFactoryEngine.CandidateSpec> first =
                StrategyChallengerFactoryEngine.generate(finding, dataset);
        List<StrategyChallengerFactoryEngine.CandidateSpec> second =
                StrategyChallengerFactoryEngine.generate(finding, dataset);
        require(
                first.size() >= 20 && first.size() == second.size(),
                "EVOLUTIONARY_CANDIDATE_FAMILIES");
        for (int i = 0; i < first.size(); i++)
            require(
                    first.get(i).candidateHash.equals(second.get(i).candidateHash),
                    "CANDIDATE_DETERMINISM_" + i);
        CandidateCodeSynthesisEngine.Bundle bundle = CandidateCodeSynthesisEngine.synthesize(first.get(0));
        CandidateCodeSynthesisEngine.Bundle bundleReplay =
                CandidateCodeSynthesisEngine.synthesize(first.get(0));
        require(
                "SOURCE_READY_RUNTIME_DSL_VERIFIED".equals(bundle.status)
                        && bundle.patch.contains("production_mutation=0")
                        && bundle.manifest.contains("production_owner=NONE")
                        && bundle.javaSource.contains("double score(Map<String,Double> features)")
                        && bundle.javaSource.contains("MAY_AUTHORIZE_REAL=false")
                        && bundle.bundleHash.equals(bundleReplay.bundleHash)
                        && bundle.javaSource.equals(bundleReplay.javaSource),
                "CODEGEN_RUNTIME_DSL_VERIFIED");
        StrategyChallengerFactoryEngine.CandidateSpec structural =
                StrategyChallengerFactoryEngine.fromHypothesis(
                        finding,
                        dataset,
                        StrategyChallengerFactoryEngine.CandidateType.RUNTIME,
                        "NEW_JAVA_STRUCTURE",
                        false,
                        811L);
        require(
                "BUILD_REQUIRED_OFFLINE_TRAINING".equals(structural.status)
                        && "OFFLINE_ARTIFACT_REQUIRED".equals(
                                CandidateCodeSynthesisEngine.synthesize(structural).status),
                "STRUCTURAL_JAVA_BUILD_REQUIRED");
        StrategyChallengerFactoryEngine.FindingBundle forbidden =
                new StrategyChallengerFactoryEngine.FindingBundle(
                        "POLICY", "unlock", "counter", "parent", "DecisionGateEngine", "");
        StrategyChallengerFactoryEngine.CandidateSpec rejected =
                StrategyChallengerFactoryEngine.fromHypothesis(
                        forbidden,
                        dataset,
                        StrategyChallengerFactoryEngine.CandidateType.POLICY,
                        "unlock",
                        true,
                        1L);
        require(rejected.status.contains("FORBIDDEN_PRODUCTION_SYMBOL"), "PRODUCTION_SYMBOL_REJECT");
        ResearchCertificationEngineR811.Certificate certificate =
                ResearchCertificationEngineR811.certifyCandidate(
                        first.get(0).candidateId, observations(240));
        require("SHADOW_OBSERVE".equals(certificate.phase), "SHADOW_CERT_PHASE");
        ResearchPromotionGovernanceEngine.Decision shadow =
                ResearchPromotionGovernanceEngine.evaluatePromotion(
                        first.get(0), certificate, true, true);
        require(
                shadow.allowed
                        && "SHADOW_CHALLENGER".equals(shadow.state)
                        && shadow.reason.contains("SHADOW_ONLY"),
                "RESEARCH_TO_SHADOW");
        require(
                !ResearchPromotionGovernanceEngine.promoteToShadow(first.get(0), certificate).allowed,
                "EXPLICIT_CERTS_REQUIRED");
        require(
                !ResearchPromotionGovernanceEngine.evaluateProductionPromotion().allowed,
                "NO_HOT_PRODUCTION_PROMOTION");
        require(
                !ResearchPromotionGovernanceEngine.evaluatePromotion(
                                first.get(1), certificate, true, true)
                        .allowed,
                "CERTIFICATE_CANDIDATE_BINDING");
    }

    private static void certificationDenominatorContract() {
        ArrayList<ResearchCertificationEngineR811.Observation> rows =
                new ArrayList<ResearchCertificationEngineR811.Observation>();
        rows.add(new ResearchCertificationEngineR811.Observation("202608100001", "A", true, true, 0.5d, 1));
        rows.add(new ResearchCertificationEngineR811.Observation("202608100002", "A", true, false, 0.5d, 0));
        rows.add(new ResearchCertificationEngineR811.Observation("202608100003", "B", false, true, 0.5d, 1));
        require(Math.abs(ResearchCertificationEngineR811.overallCoverage(rows) - 0.5d) < 1e-12d, "OVERALL_COVERAGE");
        require(Math.abs(ResearchCertificationEngineR811.domainCoverage(rows, "A") - 0.5d) < 1e-12d, "DOMAIN_COVERAGE");
        require(Math.abs(ResearchCertificationEngineR811.actionFrequencyCoverage(rows) - 0.5d) < 1e-12d, "ACTION_FREQUENCY_DENOMINATOR");
        ResearchCertificationEngineR811.Certificate empty =
                ResearchCertificationEngineR811.certifyCandidate("empty", new ArrayList<ResearchCertificationEngineR811.Observation>());
        require(empty.sampleN == 0 && "INSUFFICIENT_N".equals(empty.phase), "NO_FAKE_ZERO_PROOF");
        ArrayList<ResearchCertificationEngineR811.Observation> grouped =
                new ArrayList<ResearchCertificationEngineR811.Observation>();
        for (int i = 0; i < 1200; i++)
            grouped.add(
                    new ResearchCertificationEngineR811.Observation(
                            "202608100001", "GROUPED", true, true, 1.0d, 1));
        ResearchCertificationEngineR811.Certificate groupedCertificate =
                ResearchCertificationEngineR811.certifyCandidate("grouped", grouped);
        require(groupedCertificate.sampleN == 1, "ONE_ISSUE_1200_ROWS_PROOF_N_ONE");
        ResearchCertificationEngineR811.Certificate unverified =
                ResearchCertificationEngineR811.certifyCandidate("future", provenObservations(480));
        require(
                !"RESEARCH_PROVEN_45".equals(unverified.phase)
                        && "NOT_PROVEN".equals(unverified.proofStatus),
                "NO_PROVEN_45_WITHOUT_ALL_GUARDS");
        ResearchCertificationEngineR811.Certificate proven =
                ResearchCertificationEngineR811.certifyCandidate(
                        "future",
                        provenObservations(480),
                        new ResearchCertificationEngineR811.ProofGuards(
                                0.45d,
                                0.10d,
                                0.10d,
                                0.50d,
                                true,
                                true,
                                true,
                                true,
                                true,
                                true));
        require(
                "RESEARCH_PROVEN_45".equals(proven.phase)
                        && "PROVEN".equals(proven.proofStatus),
                "PROVEN_45_ALL_AND_GATE");
        require(
                AutonomousResearchEvolutionEngine.requiresExternalEvidence(
                                AutonomousResearchEvolutionEngine.ResearchState.WALK_FORWARD)
                        && !AutonomousResearchEvolutionEngine.requiresExternalEvidence(
                                AutonomousResearchEvolutionEngine.ResearchState.GENERATE),
                "RESEARCH_STATE_MACHINE_FAIL_CLOSED");
        ResearchCertificationEngineR811.Certificate n360 =
                ResearchCertificationEngineR811.certifyCandidate(
                        "n360", hitRateObservations(360, 162));
        ResearchCertificationEngineR811.Certificate n480 =
                ResearchCertificationEngineR811.certifyCandidate(
                        "n480", hitRateObservations(480, 216));
        require(
                n360.wilsonLower > 0.3990d
                        && n360.wilsonLower < 0.4000d
                        && n480.wilsonLower > 0.4055d
                        && n480.wilsonLower < 0.4067d,
                "WILSON_N360_N480_FIXTURE");
        require(
                ResearchCertificationEngineR811.eligibleIssuesForSelectedProof(480, 0.15d)
                                == 3200
                        && ResearchCertificationEngineR811.eligibleIssuesForSelectedProof(
                                        480, 0.20d)
                                == 2400,
                "PROOF_PROGRESS_15_20_PERCENT");
    }

    private static void holdoutContract() {
        ResearchHoldoutRegistryR811 registry = new ResearchHoldoutRegistryR811();
        ResearchHoldoutRegistryR811.Holdout holdout =
                registry.seal("candidate", V51ChecksumEngine.sha256("holdout"), "202608101439");
        require("SEALED".equals(holdout.status), "HOLDOUT_SEALED");
        require(
                ResearchHoldoutRegistryR811.eligibleFutureLive("202608101440", "202608101439"),
                "FUTURE_LIVE");
        registry.consume(holdout.id, "202608101440");
        require(registry.auditReuse().passed, "HOLDOUT_ONCE");
        boolean reuseRejected = false;
        try {
            registry.consume(holdout.id, "202608110001");
        } catch (IllegalStateException expected) {
            reuseRejected = "R811_HOLDOUT_ALREADY_CONSUMED".equals(expected.getMessage());
        }
        require(reuseRejected, "HOLDOUT_REUSE_FAIL_CLOSED");
        require(!registry.auditReuse().passed, "HOLDOUT_REUSE_DETECTED");
    }

    private static void researchStatisticsContract() {
        List<ResearchStatisticsGuardR811.Row> rows =
                Arrays.asList(
                        new ResearchStatisticsGuardR811.Row("202608100001", 1.0d, true),
                        new ResearchStatisticsGuardR811.Row("202608100001", 0.0d, true),
                        new ResearchStatisticsGuardR811.Row("202608100002", 1.0d, true),
                        new ResearchStatisticsGuardR811.Row("invalid", 1.0d, true));
        require(ResearchStatisticsGuardR811.proofSampleN(rows) == 2, "ISSUE_IS_PROOF_UNIT");
        ResearchStatisticsGuardR811.Bootstrap one =
                ResearchStatisticsGuardR811.blockBootstrap(rows, 100, 811L);
        ResearchStatisticsGuardR811.Bootstrap two =
                ResearchStatisticsGuardR811.blockBootstrap(rows, 100, 811L);
        require(
                one.issueN == 2
                        && one.estimate == two.estimate
                        && one.lower == two.lower
                        && one.upper == two.upper,
                "BLOCK_BOOTSTRAP_DETERMINISTIC");
    }

    private static void adaptiveLearnerContract() {
        RealtimePressureGateR811.resetForTests();
        AdaptiveShadowLearnerEngine learner = new AdaptiveShadowLearnerEngine();
        AdaptiveShadowLearnerEngine.Update invalid =
                learner.updateAfterSettlement(
                        new AdaptiveShadowLearnerEngine.OosRow(
                                "202608100001", 100L, 200L, false, 1, new double[] {1.0d}));
        require(!invalid.updated && "INVALID_OOS".equals(invalid.status), "LEARNER_REQUIRES_STRICT_OOS");
        AdaptiveShadowLearnerEngine.OosRow row =
                new AdaptiveShadowLearnerEngine.OosRow(
                        "202608100001", 100L, 200L, true, 1, new double[] {1.0d, 0.5d});
        require(learner.updateAfterSettlement(row).updated, "LEARNER_UPDATE");
        require("REPLAY_IGNORED".equals(learner.updateAfterSettlement(row).status), "LEARNER_NO_REPLAY");
        ResearchArtifactCodecR811.Artifact artifact = learner.snapshotResearchArtifact();
        require(
                "FTRL_SHADOW".equals(artifact.type)
                        && "202608100001".equals(artifact.trainedUntilIssue),
                "LEARNER_RESEARCH_ARTIFACT");
    }

    private static void ownerContract() {
        ProductionOwnerCatalogR811.Validation validation = ProductionOwnerCatalogR811.validate();
        require(validation.passed, "OWNER_CATALOG");
        int real = 0, settlement = 0, oos = 0, research = 0;
        for (ProductionOwnerCatalogR811.Owner owner : ProductionOwnerCatalogR811.all()) {
            if ("REAL".equals(owner.role) && "DecisionGateEngine".equals(owner.path)) real++;
            if ("Settlement".equals(owner.role) && "SettlementEngine".equals(owner.path)) settlement++;
            if ("OOS".equals(owner.role) && "OosAfterDrawWriterR36".equals(owner.path)) oos++;
            if ("Research".equals(owner.role) && "RESEARCH_ONLY".equals(owner.authority)) research++;
        }
        require(real == 1 && settlement == 1 && oos == 1 && research == 1, "SOLE_AUTHORITIES");
    }

    private static List<ResearchCertificationEngineR811.Observation> observations(int count) {
        ArrayList<ResearchCertificationEngineR811.Observation> rows =
                new ArrayList<ResearchCertificationEngineR811.Observation>();
        for (int i = 1; i <= count; i++)
            rows.add(
                    new ResearchCertificationEngineR811.Observation(
                            "20260810" + four(i),
                            "HOST",
                            true,
                            true,
                            (i & 1) == 0 ? 0.05d : 0.95d,
                            (i & 1) == 0 ? 0 : 1));
        return rows;
    }

    private static List<ResearchCertificationEngineR811.Observation> provenObservations(
            int count) {
        ArrayList<ResearchCertificationEngineR811.Observation> rows =
                new ArrayList<ResearchCertificationEngineR811.Observation>();
        for (int i = 1; i <= count; i++) {
            int label = (i & 1) == 0 ? 0 : 1;
            rows.add(
                    new ResearchCertificationEngineR811.Observation(
                            "20260810" + four(i),
                            "HOST",
                            true,
                            true,
                            label == 1 ? 1.0d : 0.0d,
                            label));
        }
        return rows;
    }

    private static List<ResearchCertificationEngineR811.Observation> hitRateObservations(
            int count, int hits) {
        ArrayList<ResearchCertificationEngineR811.Observation> rows =
                new ArrayList<ResearchCertificationEngineR811.Observation>();
        for (int i = 1; i <= count; i++) {
            int label = i <= hits ? 1 : 0;
            rows.add(
                    new ResearchCertificationEngineR811.Observation(
                            "20260810" + four(i),
                            "HOST",
                            true,
                            true,
                            label == 1 ? 1.0d : 0.0d,
                            label));
        }
        return rows;
    }

    private static Models.Draw draw(String issue, int offset) {
        Models.Draw draw = new Models.Draw();
        draw.issue = issue;
        draw.openTime = 1786400000000L;
        for (int i = 0; i < 10; i++) draw.numbers[i] = (i + offset) % 10 + 1;
        require(draw.isValid(), "DRAW_FIXTURE");
        return draw;
    }

    private static String four(int value) {
        String out = String.valueOf(value);
        while (out.length() < 4) out = '0' + out;
        return out;
    }

    private static double[] xavierSequential(
            ResearchDeterministicRngR811 rng,
            int fanIn,
            int fanOut,
            int count) {
        double limit = Math.sqrt(6.0d / (fanIn + fanOut));
        double[] output = new double[count];
        for (int i = 0; i < output.length; i++)
            output[i] = (rng.nextDouble() * 2.0d - 1.0d) * limit;
        return output;
    }

    private static boolean approxVector(double[] left, double[] right, double tolerance) {
        if (left == null || right == null || left.length != right.length) return false;
        for (int i = 0; i < left.length; i++)
            if (Math.abs(left[i] - right[i]) > tolerance) return false;
        return true;
    }

    private static void require(boolean condition, String code) {
        assertions++;
        if (!condition) throw new IllegalStateException("R811_HOST_" + code);
    }
}
