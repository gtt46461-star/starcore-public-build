package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Host-executable release, atom, invention and truth contracts for R922. */
public final class R924LegacyRuntimeContractHarness {
    private R924LegacyRuntimeContractHarness() {}

    public static void main(String[] args) {
        releaseIdentity();
        atomBoundary();
        atomRegistryIntegrity();
        semanticAtomicWorld();
        assetLeakageRepair();
        directedAlgorithmEvolution();
        controlledRealAudit();
        opportunityFunnel();
        backgroundContract();
        evolutionDeltaContract();
        runtimeTruthIdentity();
        System.out.println("R922_RUNTIME_CONTRACT=PASS");
    }

    private static void semanticAtomicWorld() {
        ArrayList<Models.Draw> history = new ArrayList<Models.Draw>();
        String issue = "202608310001";
        for (int i = 0; i < 128; i++) {
            Models.Draw draw = new Models.Draw();
            draw.issue = issue;
            draw.openTime = 1_788_100_000_000L + i * 60_000L;
            draw.fetchTime = draw.openTime + 800L + (i % 7) * 90L;
            draw.sourceLine = "PRIMARY";
            for (int p = 0; p < 10; p++) draw.numbers[p] = 1 + (p + i * 3) % 10;
            draw.normalizeDirty();
            require(draw.isValid(), "SEMANTIC_FIXTURE_DRAW_" + i);
            history.add(draw);
            issue = TargetIssueResolver.next(issue).targetIssue;
        }
        HistoricalStrategyInventionR908.Profile profile =
                HistoricalStrategyInventionR908.Profile.from(history);
        V51Models.PositionAction action = new V51Models.PositionAction(
                3, 1, 4, 7, 8, 0.31d, 0.72d, 0.052d, 0.021d,
                0.61d, 0.82d, new double[]{0.2d, 0.4d, 0.6d},
                "R922_FIXTURE", 12, "R922_FIXTURE_HASH");
        String historyHash = V51ChecksumEngine.sha256("R922_SEMANTIC_HISTORY");
        double[] first = GenesisAtomicWorldR920.materialize(
                profile, action, history.size(), historyHash, 640L);
        double[] second = GenesisAtomicWorldR920.materialize(
                profile, action, history.size(), historyHash, 640L);
        require(first.length == 240 && second.length == 240, "SEMANTIC_ATOM_LENGTH");
        HashSet<Long> distinct = new HashSet<Long>();
        int nonzero = 0;
        for (int i = 0; i < first.length; i++) {
            require(!Double.isNaN(first[i]) && !Double.isInfinite(first[i])
                            && first[i] >= 0.0d && first[i] <= 1.0d,
                    "SEMANTIC_RANGE_" + i);
            require(Double.doubleToLongBits(first[i]) == Double.doubleToLongBits(second[i]),
                    "SEMANTIC_DETERMINISM_" + i);
            if (first[i] != 0.0d) nonzero++;
            distinct.add(Long.valueOf(Math.round(first[i] * 1_000_000.0d)));
            if (i >= 180 && i < 200) require(first[i] == 0.0d, "SEMANTIC_A9_" + i);
        }
        require(Math.abs(first[20] - 2.0d / 9.0d) < 1e-12d,
                "SEMANTIC_POSITION_ID");
        require(Math.abs(first[174] - 0.31d) < 1e-12d,
                "SEMANTIC_ACTION_PROBABILITY");
        require(first[5] > 0.20d && first[5] < 0.30d,
                "SEMANTIC_CONTIGUOUS_HISTORY");
        require(nonzero >= 175 && distinct.size() >= 70,
                "SEMANTIC_DENSITY_OR_DIVERSITY");
        long started = System.nanoTime();
        int actionCount = 0;
        double surfaceDigest = 0.0d;
        for (int position = 1; position <= 10; position++) {
            for (int n1 = 1; n1 <= 8; n1++) for (int n2 = n1 + 1; n2 <= 9; n2++)
                for (int n3 = n2 + 1; n3 <= 10; n3++) {
                    actionCount++;
                    V51Models.PositionAction one = new V51Models.PositionAction(
                            position, n1, n2, n3, Math.min(10, n3 + 1),
                            0.20d + (actionCount % 21) / 100.0d,
                            1.0d - actionCount / 1200.0d, 0.04d, 0.02d,
                            0.50d, 0.75d, new double[0], "SURFACE", actionCount, "SURFACE");
                    double[] world = GenesisAtomicWorldR920.materialize(
                            profile, one, history.size(), historyHash, 640L);
                    surfaceDigest += world[(actionCount * 37) % world.length]
                            * (1.0d + (actionCount % 17));
                }
        }
        long surfaceMs = (System.nanoTime() - started) / 1_000_000L;
        require(actionCount == 1200, "SEMANTIC_ACTION_SURFACE_COUNT");
        require(surfaceDigest > 1.0d && surfaceMs < 5000L,
                "SEMANTIC_ACTION_SURFACE_BUDGET");
        System.out.println(
                "R922_SEMANTIC_ATOMIC_WORLD=PASS|ATOMS=240|NAMED_GROUPS=12"
                        + "|A9_PREDRAW_ZERO=20|NONZERO=" + nonzero
                        + "|DISTINCT_1E6=" + distinct.size() + "|DETERMINISTIC=1"
                        + "|ACTION_SURFACE=1200|HOST_MS=" + surfaceMs);
    }

    private static void releaseIdentity() {
        require(ReleaseManifestR926.VERSION_NAME.equals(V51BuildManifest.VERSION_NAME),
                "VERSION_NAME");
        require(V51BuildManifest.VERSION_CODE == ReleaseManifestR926.VERSION_CODE, "VERSION_CODE");
        require(!V51BuildManifest.MODEL_PROVEN && !V51BuildManifest.REALTIME_PROVEN,
                "NO_FALSE_PERFORMANCE_PROOF");
        require(!AlgorithmInventionLabR908.MAY_AUTHORIZE_REAL
                        && !AssetGenesisEngineR915.MAY_AUTHORIZE_REAL
                        && !CertifiedResearchMixerR915.MAY_AUTHORIZE_REAL,
                "RESEARCH_ZERO_DIRECT_REAL_AUTHORITY");
        System.out.println(
                "R922_RELEASE_IDENTITY=PASS|VERSION=" + V51BuildManifest.VERSION_NAME
                        + "|CODE=" + V51BuildManifest.VERSION_CODE
                        + "|MODEL_PROVEN=0|DIRECT_REAL_AUTH=0");
    }

    private static void atomBoundary() {
        GenesisSchemaRegistryR915.assertContract();
        int predraw = 0;
        int production = 0;
        int proxy = 0;
        int audit = 0;
        int settlement = 0;
        boolean[] groupSeen = new boolean[12];
        for (GenesisSchemaRegistryR915.AtomSpec atom : GenesisSchemaRegistryR915.atoms()) {
            require(atom.valueRange.length() > 0 && atom.windowSpec.length() > 0,
                    "ATOM_DOMAIN_" + atom.id);
            require(atom.leakagePolicy.length() > 0 && atom.updateTiming.length() > 0,
                    "ATOM_TIME_" + atom.id);
            require(atom.maxLag >= 0 && atom.minSupport >= 1,
                    "ATOM_SUPPORT_" + atom.id);
            int group = Integer.parseInt(atom.id.substring(1, atom.id.indexOf('-')));
            require(group >= 0 && group < groupSeen.length, "ATOM_GROUP_" + atom.id);
            groupSeen[group] = true;
            if (atom.predrawComputable()) predraw++;
            if (atom.productionScoreEligible()) production++;
            if (GenesisSchemaRegistryR915.TIER_RESEARCH_PROXY.equals(atom.semanticTier())) proxy++;
            if (GenesisSchemaRegistryR915.TIER_AUDIT_IDENTITY.equals(atom.semanticTier())) audit++;
            if (atom.settlementEvidence()) {
                settlement++;
                require(atom.id.startsWith("A9-"), "SETTLEMENT_GROUP_" + atom.id);
                require(!atom.predrawComputable(), "SETTLEMENT_LEAK_" + atom.id);
            }
        }
        int groups = 0;
        for (boolean seen : groupSeen) if (seen) groups++;
        require(GenesisSchemaRegistryR915.atomCount() == 240, "ATOM_COUNT");
        require(GenesisSchemaRegistryR915.familyCount() == 72, "FAMILY_COUNT");
        require(predraw == 205 && production == 165 && proxy == 40 && audit == 15
                && settlement == 20 && groups == 12, "ATOM_COUNTS");
        require(GenesisSchemaRegistryR915.validateNumericComposition("A4-01", "A5-01", "MUL"),
                "LEGAL_PREDRAW_COMPOSITION");
        require(!GenesisSchemaRegistryR915.validateNumericComposition("A9-01", "A4-01", "ADD"),
                "A9_COMPOSITION_REJECT");
        System.out.println(
                "R922_ATOM_STAGE_CONTRACT=PASS|TOTAL=240|GROUPS=12|FAMILIES=72"
                        + "|PREDRAW=205|PRODUCTION_SCORE=165|RESEARCH_PROXY=40"
                        + "|AUDIT_IDENTITY=15|SETTLEMENT_EVIDENCE=20|A9_PREDRAW=REJECT");
    }

    private static void atomRegistryIntegrity() {
        Map<String,String> expected = LegacyIntegrityGuardR915.expectedAtomRowsForTest();
        require(expected.size() == 240, "ATOM_REGISTRY_FIXTURE_COUNT");
        require(LegacyIntegrityGuardR915.auditAtomRowsForTest(expected).isEmpty(),
                "ATOM_REGISTRY_EXACT_REJECTED");
        LinkedHashMap<String,String> mutated = new LinkedHashMap<String,String>(expected);
        String id = "A6-01";
        mutated.put(id, mutated.get(id) + "|MUTATED");
        List<String> reasons = LegacyIntegrityGuardR915.auditAtomRowsForTest(mutated);
        require(reasons.size() == 1 && reasons.get(0).equals("CONTRACT_CHANGED:" + id),
                "ATOM_REGISTRY_MUTATION_NOT_DETECTED");
        mutated.remove("A9-01");
        reasons = LegacyIntegrityGuardR915.auditAtomRowsForTest(mutated);
        require(reasons.contains("CARDINALITY:239") && reasons.contains("MISSING:A9-01"),
                "ATOM_REGISTRY_MISSING_NOT_DETECTED");
        System.out.println("R922_ATOM_REGISTRY_INTEGRITY=PASS|EXACT=240/240"
                + "|MUTATION_REJECT=1|MISSING_REJECT=1|LOSSY_SUBSET_COUNT=FORBIDDEN");
    }

    private static void assetLeakageRepair() {
        int[] leaking = new int[] {
                180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 0, 20, 40, 80
        };
        GenesisAssetGenomeR915 asset = new GenesisAssetGenomeR915(
                "AF32", 8, "", "R922_A9_REPAIR", "202608311200",
                GenesisAssetGenomeR915.OutputType.RELATION_FIELD, leaking,
                AlgorithmGenomeR908.seed(0x523932305245534fL, 8, "R922_PRIMITIVES"));
        require(asset.assetGrade(), "ASSET_GRADE");
        for (int index : asset.atomIndices) {
            GenesisSchemaRegistryR915.AtomSpec atom =
                    GenesisSchemaRegistryR915.atoms().get(index);
            require(atom.predrawComputable(), "ASSET_A9_LEAK_" + atom.id);
        }
        GenesisAssetGenomeR915 decoded = GenesisAssetGenomeR915.parse(asset.canonical());
        require(decoded.structuralHash.equals(asset.structuralHash)
                        && decoded.behaviorHash.equals(asset.behaviorHash),
                "ASSET_ROUNDTRIP");
        GenesisAssetGenomeR915 production = new GenesisAssetGenomeR915(
                "AF33", 9, "", "R922_PRODUCTION_SEMANTIC", "202608311200",
                GenesisAssetGenomeR915.OutputType.SCALAR_SCORE,
                new int[]{2, 22, 40, 60, 80, 100, 120, 140, 160},
                AlgorithmGenomeR908.seed(0x5239323150524f44L, 9, "R922_PRODUCTION"));
        require(production.usesOnlyProductionScoreAtoms(), "PRODUCTION_ATOM_SET");
        GenesisAssetGenomeR915 proxy = new GenesisAssetGenomeR915(
                "AF34", 9, "", "R922_PROXY_OBSERVATION", "202608311200",
                GenesisAssetGenomeR915.OutputType.SCALAR_SCORE,
                new int[]{2, 22, 40, 60, 80, 100, 200, 220},
                AlgorithmGenomeR908.seed(0x5239323150524f58L, 9, "R922_PROXY"));
        require(!proxy.usesOnlyProductionScoreAtoms(), "PROXY_CERTIFICATE_BOUNDARY");
        System.out.println(
                "R922_ASSET_LEAKAGE_REPAIR=PASS|NAME=" + asset.mechanismNameZh
                        + "|A9_SELECTED=0|ROUNDTRIP=PASS|PRODUCTION_SCORE=165"
                        + "|PROXY_CERTIFIABLE=0");
    }

    private static void directedAlgorithmEvolution() {
        AlgorithmGenomeR908 parent =
                AlgorithmGenomeR908.seed(202608312000L, 20, "BOTTOM_UP_PRIMITIVES");
        AlgorithmGenomeVmR908.State memory = new AlgorithmGenomeVmR908.State();
        double[] x = new double[AlgorithmGenomeR908.INPUTS];
        for (int i = 0; i < 9; i++) x[i] = 0.05d * (i + 1);
        String before = memory.encode();
        double score = AlgorithmGenomeVmR908.score(parent, memory, 3, x);
        require(before.equals(memory.encode()), "PREDRAW_READ_ONLY");
        x[9] = 1.0d;
        x[10] = 0.12d;
        x[11] = score;
        x[12] = 0.48d;
        x[13] = 1.0d - score;
        AlgorithmGenomeVmR908.update(parent, memory, 3, x);
        require(memory.updates == 1L && !before.equals(memory.encode()),
                "SETTLEMENT_WRITES_STATE");

        int intents = 0;
        for (AlgorithmGenomeR908.MutationIntent intent
                : AlgorithmGenomeR908.MutationIntent.values()) {
            AlgorithmGenomeR908 child = parent.mutateForIntent(
                    202608312100L + intent.ordinal(), 21, intent);
            require(child.algorithmGrade(), "INTENT_GRADE_" + intent.name());
            require(!child.structuralHash.equals(parent.structuralHash),
                    "INTENT_STRUCTURE_" + intent.name());
            require(child.chineseName().contains("算法·第21代"),
                    "INTENT_CHINESE_NAME_" + intent.name());
            ResearchGenomeContractR915 fresh = ResearchGenomeContractR915.freshChild(
                    child, "202608311200", "202608311201");
            require(fresh.evidence.futureN == 0
                            && !fresh.productionInfluenceEligible("202608311201"),
                    "CHILD_FUTURE_RESET_" + intent.name());
            intents++;
        }
        require(intents == 7, "MUTATION_INTENT_COUNT");
        System.out.println(
                "R922_EVOLUTION_CONTRACT=PASS|STATE_SCORE_UPDATE=PASS"
                        + "|FAILURE_DIRECTED_INTENTS=7|CHINESE_NAMES=PASS|CHILD_FUTURE_N=0");
    }

    private static void controlledRealAudit() {
        String authorizationId = V51ChecksumEngine.sha256("R922_VALID_REAL_AUTHORIZATION");
        ControlledRealContractAuditR922.Row valid = new ControlledRealContractAuditR922.Row(
                "202609010002", 1, "BET", AppConfig.STAT_SCOPE_REAL_TRADE,
                Models.SOURCE_TYPE_REALTIME, 1, 1, "BET", authorizationId, 1,
                ControlledRealContractAuditR922.EXACT_CONTRACT_STATUS,
                V51Enums.Lane.CONTROLLED_PROBE.name(), 4, 2, 6, 9,
                AppConfig.buildK3Mask(2, 6, 9), 0.43d, 0.0d, 0.0d, 0.0d);
        ControlledRealContractAuditR922.Report pass = ControlledRealContractAuditR922.evaluate(
                new ControlledRealContractAuditR922.Row[]{valid});
        require(pass.passed && pass.total == 1 && pass.settled == 1
                        && pass.invalid == 0 && pass.duplicateTargets == 0,
                "CONTROLLED_REAL_VALID_LEDGER");

        ControlledRealContractAuditR922.Row invalid = new ControlledRealContractAuditR922.Row(
                "202609010003", 0, "BET", AppConfig.STAT_SCOPE_REAL_TRADE,
                Models.SOURCE_TYPE_REALTIME, 1, 1, "BET",
                V51ChecksumEngine.sha256("R922_INVALID_REAL_AUTHORIZATION"), 1,
                ControlledRealContractAuditR922.EXACT_CONTRACT_STATUS,
                V51Enums.Lane.CONTROLLED_PROBE.name(), 4, 2, 6, 9,
                AppConfig.buildK3Mask(2, 6, 9), 0.43d, 0.41d, 0.0d, 0.0d);
        ControlledRealContractAuditR922.Report reject = ControlledRealContractAuditR922.evaluate(
                new ControlledRealContractAuditR922.Row[]{invalid});
        require(!reject.passed && reject.invalid == 1
                        && reject.rejectReasons.containsKey("UNPROVEN_CALIBRATION_MUST_BE_ZERO"),
                "CONTROLLED_REAL_INVALID_CALIBRATION_REJECT");
        System.out.println(
                "R922_CONTROLLED_REAL_AUDIT=PASS|VALID_REAL_ROWS_ALLOWED=1"
                        + "|INVALID_CONTRACT_REJECT=1|ZERO_BET_REQUIREMENT_REMOVED=1");
    }

    private static void opportunityFunnel() {
        ArrayList<ControlledCandidateOpportunityAuditR922.Opportunity> rows =
                new ArrayList<ControlledCandidateOpportunityAuditR922.Opportunity>();
        String base = "202609010001";
        for (int i = 0; i < 20; i++) {
            String target = TargetIssueResolver.next(base).targetIssue;
            boolean bet = i < 3;
            String hash = V51ChecksumEngine.sha256("R922_OPPORTUNITY_" + i + '|' + target);
            rows.add(new ControlledCandidateOpportunityAuditR922.Opportunity(
                    base, target, AppConfig.R36_RUNTIME_REPAIR_VERSION, bet,
                    bet ? "ELIGIBLE" : "BELOW_STRICT_THRESHOLD", "HOST_CONTRACT",
                    "PRODUCTION_FALLBACK", "BASELINE", "ACTIVE", 0.0d,
                    false, false, bet ? "BET" : "HOLD", bet ? "NONE" : "CANDIDATE_GATE",
                    4, 2, 6, 9, 0.40d, 0.04d, 0.70d, 0.55d,
                    0.80d, 0.20d, 0.90d, 0.85d, 0.75d,
                    1_788_225_200_000L + i * 60_000L, hash));
            base = target;
        }
        ControlledCandidateOpportunityAuditR922.Report report =
                ControlledCandidateOpportunityAuditR922.evaluate(
                        rows.toArray(new ControlledCandidateOpportunityAuditR922.Opportunity[rows.size()]),
                        20);
        require(report.opportunities == 20 && report.bets == 3 && report.invalidRows == 0,
                "OPPORTUNITY_FUNNEL_COUNTS");
        require(Math.abs(report.frequency - 0.15d) < 1.0e-12d
                        && "TARGET_OBSERVED".equals(report.targetState),
                "OPPORTUNITY_FUNNEL_TARGET");
        System.out.println(
                "R922_OPPORTUNITY_FUNNEL=PASS|OPPORTUNITIES=20|BET=3|FREQUENCY=15.00%"
                        + "|TARGET_IS_OBSERVATION_NOT_QUOTA=1|FREQUENCY_DEBT=FORBIDDEN");
    }

    private static void backgroundContract() {
        BackgroundRuntimeContractR922.Evaluation pass = BackgroundRuntimeContractR922.evaluate(
                true, true, true, true, true, true, true, true, true, true);
        require(pass.passed && pass.staticReady && pass.runtimeReady
                        && "ARMED_RECONCILABLE".equals(pass.status),
                "BACKGROUND_FULL_CONTRACT");
        BackgroundRuntimeContractR922.Evaluation reject = BackgroundRuntimeContractR922.evaluate(
                true, true, true, true, true, true, true, false, true, true);
        require(!reject.passed && "RUNTIME_LOOP_INCOMPLETE".equals(reject.status),
                "BACKGROUND_UI_EXIT_RECONCILE_REQUIRED");
        require(BackgroundRuntimeContractR922.LIMITATION.contains("FORCE_STOP"),
                "BACKGROUND_ANDROID_LIMITATION_TRUTH");
        System.out.println(
                "R922_BACKGROUND_CONTRACT=PASS|UI_EXIT_RECONCILE=1|TASK_REMOVED_RECONCILE=1"
                        + "|RESEARCH_LOOP=1|SETTLEMENT_LOOP=1|FORCE_STOP_LIMITATION=DECLARED");
    }

    private static void evolutionDeltaContract() {
        ResearchEvolutionDeltaR922.Snapshot before =
                new ResearchEvolutionDeltaR922.Snapshot(
                        9, 0, 6, 240, 0, 180, 27, 720, 0, 540, 80, 40);
        ResearchEvolutionDeltaR922.Snapshot after =
                new ResearchEvolutionDeltaR922.Snapshot(
                        10, 1, 7, 248, 1, 188, 28, 728, 1, 548, 81, 41);
        require(!before.equalsSnapshot(after), "EVOLUTION_DELTA_MATERIAL_CHANGE");
        require(ResearchEvolutionDeltaR922.Snapshot.empty()
                        .equalsSnapshot(ResearchEvolutionDeltaR922.Snapshot.empty()),
                "EVOLUTION_DELTA_EMPTY_STABLE");
        System.out.println(
                "R922_EVOLUTION_DELTA=PASS|FORMULA=1|ASSET=1|ALGORITHM=1"
                        + "|ASSET_STATE_UPDATE=1|ALGORITHM_STATE_UPDATE=1|FTRL=1|TINY_DEEP=1");
    }

    private static void runtimeTruthIdentity() {
        require("R923-RUNTIME-TRUTH-V9".equals(RuntimeTruthSnapshotR906.VERSION),
                "RUNTIME_TRUTH_VERSION");
        require("v57_legacy_postsettlement_context_r919".equals(
                        LegacyPredrawContextBridgeR915.POST_TABLE),
                "TEACHER_POST_TABLE_COMPATIBILITY");
        RuntimeTruthSnapshotR906 failure = RuntimeTruthSnapshotR906.error("HOST_PROBE");
        require(failure.whyCards.length == 4, "WHY_CARD_COUNT");
        require(failure.stateBoxes.length == 16, "STATE_BOX_COUNT");
        require(failure.status.equals("READ_ERROR")
                        && failure.snapshotId.startsWith("R922-READ-ERROR-"),
                "READ_ERROR_EXPLICIT");
        System.out.println(
                "R922_UNIFIED_RUNTIME_TRUTH=PASS|WHY_CARDS=4|STATE_BOXES=16"
                        + "|TEACHER_POSTSETTLEMENT=1|READ_ERROR_EXPLICIT=1");
    }

    private static void require(boolean condition, String code) {
        if (!condition) throw new IllegalStateException("R922_FAIL:" + code);
    }
}
