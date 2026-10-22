package com.ethk3.novaonepro;

/**
 * Host-executable R919 contracts that were not covered by the R915 harness.
 *
 * <p>The goal is not to manufacture a performance claim.  It proves the temporal boundary of the
 * 240-atom language, executable state/score/update semantics, mechanism-derived Chinese names,
 * immutable child evidence and the zero-direct-REAL authority boundary.  Database exactly-once
 * behavior is exercised separately by the SQLite closure test.</p>
 */
public final class R919RuntimeContractHarness {
    private R919RuntimeContractHarness() {}

    public static void main(String[] args) {
        releaseIdentity();
        atomStageBoundary();
        repairedAssetNeverReadsSettlementEvidence();
        algorithmAndAssetEvolution();
        runtimeTruthIdentity();
        System.out.println("R919_RUNTIME_CONTRACT=PASS");
    }

    private static void releaseIdentity() {
        require("V57.20.5-R5.3.5-919".equals(V51BuildManifest.VERSION_NAME),
                "VERSION_NAME");
        require(V51BuildManifest.VERSION_CODE == 577240919, "VERSION_CODE");
        require(!V51BuildManifest.MODEL_PROVEN && !V51BuildManifest.REALTIME_PROVEN,
                "NO_FALSE_PERFORMANCE_PROOF");
        require(!AlgorithmInventionLabR908.MAY_AUTHORIZE_REAL
                        && !AssetGenesisEngineR915.MAY_AUTHORIZE_REAL
                        && !CertifiedResearchMixerR915.MAY_AUTHORIZE_REAL,
                "RESEARCH_ZERO_DIRECT_REAL_AUTHORITY");
        System.out.println(
                "R919_RELEASE_IDENTITY=PASS|VERSION=V57.20.5-R5.3.5-919"
                        + "|CODE=577240919|MODEL_PROVEN=0|DIRECT_REAL_AUTH=0");
    }

    private static void atomStageBoundary() {
        GenesisSchemaRegistryR915.assertContract();
        int predraw = 0;
        int settlement = 0;
        for (GenesisSchemaRegistryR915.AtomSpec atom : GenesisSchemaRegistryR915.atoms()) {
            require(atom.valueRange.length() > 0 && atom.windowSpec.length() > 0,
                    "ATOM_DOMAIN_" + atom.id);
            require(atom.leakagePolicy.length() > 0 && atom.updateTiming.length() > 0,
                    "ATOM_TIME_" + atom.id);
            require(atom.maxLag >= 0 && atom.minSupport >= 1,
                    "ATOM_SUPPORT_" + atom.id);
            if (atom.predrawComputable()) predraw++;
            if (atom.settlementEvidence()) {
                settlement++;
                require(atom.id.startsWith("A9-"), "SETTLEMENT_GROUP_" + atom.id);
                require(!atom.predrawComputable(), "SETTLEMENT_LEAK_" + atom.id);
            }
        }
        require(GenesisSchemaRegistryR915.atomCount() == 240, "ATOM_COUNT");
        require(GenesisSchemaRegistryR915.familyCount() == 72, "FAMILY_COUNT");
        require(predraw == GenesisSchemaRegistryR915.predrawComputableCount(),
                "PREDRAW_COUNT_API");
        require(settlement == 20
                        && settlement == GenesisSchemaRegistryR915.settlementEvidenceCount(),
                "SETTLEMENT_COUNT_API");
        require(GenesisSchemaRegistryR915.validateNumericComposition("A4-01", "A5-01", "MUL"),
                "LEGAL_PREDRAW_COMPOSITION");
        require(!GenesisSchemaRegistryR915.validateNumericComposition("A9-01", "A4-01", "ADD"),
                "A9_COMPOSITION_MUST_BE_REJECTED");
        System.out.println(
                "R919_ATOM_STAGE_CONTRACT=PASS|TOTAL=240|PREDRAW=" + predraw
                        + "|SETTLEMENT_EVIDENCE=" + settlement
                        + "|A9_PREDRAW_COMPOSITION=REJECT");
    }

    private static void repairedAssetNeverReadsSettlementEvidence() {
        int[] deliberatelyLeaking = new int[] {
                180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 0, 20, 40, 80
        };
        GenesisAssetGenomeR915 asset = new GenesisAssetGenomeR915(
                "AF31",
                7,
                "",
                "R919_TARGET_LEAKAGE_REPAIR_PROBE",
                "202608310500",
                GenesisAssetGenomeR915.OutputType.RELATION_FIELD,
                deliberatelyLeaking,
                AlgorithmGenomeR908.seed(0x5239313950524f42L, 7, "R919_PRIMITIVES"));
        require(asset.assetGrade(), "REPAIRED_ASSET_GRADE");
        int groups = 0;
        boolean[] seen = new boolean[12];
        for (int index : asset.atomIndices) {
            GenesisSchemaRegistryR915.AtomSpec atom =
                    GenesisSchemaRegistryR915.atoms().get(index);
            require(atom.predrawComputable(), "REPAIRED_ASSET_LEAK_" + atom.id);
            if (!seen[index / 20]) { seen[index / 20] = true; groups++; }
        }
        require(groups >= 3, "REPAIRED_ASSET_GROUP_DIVERSITY");
        GenesisAssetGenomeR915 decoded = GenesisAssetGenomeR915.parse(asset.canonical());
        require(decoded.structuralHash.equals(asset.structuralHash)
                        && decoded.behaviorHash.equals(asset.behaviorHash),
                "REPAIRED_ASSET_ROUNDTRIP");
        System.out.println(
                "R919_ASSET_LEAKAGE_REPAIR=PASS|NAME=" + asset.mechanismNameZh
                        + "|ATOMS=" + asset.atomIndices.length + "|GROUPS=" + groups
                        + "|A9_SELECTED=0");
    }

    private static void algorithmAndAssetEvolution() {
        AlgorithmGenomeR908 parent =
                AlgorithmGenomeR908.seed(202608310919L, 11, "BOTTOM_UP_PRIMITIVES");
        AlgorithmGenomeR908 child = parent.mutate(202608310920L, 12);
        require(parent.algorithmGrade() && child.algorithmGrade(), "ALGORITHM_GRADE");
        require(!parent.structuralHash.equals(child.structuralHash),
                "ALGORITHM_STRUCTURAL_EVOLUTION");
        require(parent.chineseName().contains("算法·第11代")
                        && child.chineseName().contains("算法·第12代"),
                "ALGORITHM_MECHANISM_NAME");

        AlgorithmGenomeVmR908.State memory = new AlgorithmGenomeVmR908.State();
        double[] x = new double[AlgorithmGenomeR908.INPUTS];
        for (int i = 0; i < 9; i++) x[i] = 0.06d * (i + 1);
        String before = memory.encode();
        double score = AlgorithmGenomeVmR908.score(parent, memory, 4, x);
        require(before.equals(memory.encode()), "PREDRAW_SCORE_READ_ONLY");
        x[9] = 1.0d;
        x[10] = 0.17d;
        x[11] = score;
        x[12] = 0.52d;
        x[13] = 1.0d - score;
        AlgorithmGenomeVmR908.update(parent, memory, 4, x);
        require(memory.updates == 1L && !before.equals(memory.encode()),
                "SETTLEMENT_UPDATE_WRITES_MEMORY");

        ResearchGenomeContractR915 fresh = ResearchGenomeContractR915.freshChild(
                child, "202608310500", "202608310501");
        require(fresh.evidence.futureN == 0
                        && "IDEA_WAIT_FUTURE_OOS".equals(fresh.lifecycleStatus),
                "CHILD_EVIDENCE_RESET");
        require(!fresh.productionInfluenceEligible("202608310501"),
                "UNPROVEN_CHILD_ZERO_INFLUENCE");

        int directed = 0;
        for (AlgorithmGenomeR908.MutationIntent intent
                : AlgorithmGenomeR908.MutationIntent.values()) {
            AlgorithmGenomeR908 repaired = parent.mutateForIntent(
                    202608311000L + intent.ordinal(), 12, intent);
            require(repaired.algorithmGrade(), "DIRECTED_GRADE_" + intent.name());
            require(!repaired.structuralHash.equals(parent.structuralHash),
                    "DIRECTED_STRUCTURE_" + intent.name());
            require(repaired.chineseName().contains("算法·第12代"),
                    "DIRECTED_NAME_" + intent.name());
            directed++;
        }
        System.out.println(
                "R919_EVOLUTION_CONTRACT=PASS|PARENT=" + parent.chineseName()
                        + "|CHILD=" + child.chineseName()
                        + "|STATE_SCORE_UPDATE=PASS|CHILD_FUTURE_N=0"
                        + "|FAILURE_DIRECTED_INTENTS=" + directed);
    }

    private static void runtimeTruthIdentity() {
        require("R919-RUNTIME-TRUTH-V6".equals(RuntimeTruthSnapshotR906.VERSION),
                "RUNTIME_TRUTH_VERSION");
        require("v57_legacy_postsettlement_context_r919".equals(
                        LegacyPredrawContextBridgeR915.POST_TABLE),
                "TEACHER_POST_TABLE");
        RuntimeTruthSnapshotR906 failure = RuntimeTruthSnapshotR906.error("HOST_PROBE");
        require(failure.whyCards.length == 4, "WHY_CARD_COUNT");
        require(failure.stateBoxes.length == 16, "STATE_BOX_COUNT");
        require(failure.status.equals("READ_ERROR")
                        && failure.snapshotId.startsWith("R919-READ-ERROR-"),
                "READ_ERROR_NOT_FALSE_ZERO");
        System.out.println(
                "R919_UNIFIED_RUNTIME_TRUTH=PASS|WHY_CARDS=4|STATE_BOXES=16"
                        + "|TEACHER_POSTSETTLEMENT=1|READ_ERROR_EXPLICIT=1");
    }

    private static void require(boolean condition, String code) {
        if (!condition) throw new IllegalStateException("R919_FAIL:" + code);
    }
}
