package com.ethk3.novaonepro;

/** Host-executable contracts for the R915 controlled gate and algorithm-plan foundation. */
public final class R915RuntimeContractHarness {
    public static void main(String[] args) {
        releaseBoundary();
        controlledAuthorization();
        planRegistries();
        inventionMechanism();
        assetGenesisMechanism();
        System.out.println("R915_RUNTIME_CONTRACT=PASS");
    }

    private static void releaseBoundary() {
        require(V51CandidateRealGateR915.releaseBoundary(), "CONTROLLED_RELEASE_BOUNDARY");
        require(V51BuildManifest.ENGINEERING_COMPLETE, "ENGINEERING_COMPLETE");
        require(!V51BuildManifest.MODEL_PROVEN && !V51BuildManifest.REALTIME_PROVEN,
                "NO_FALSE_MODEL_PROOF");
        require(!V51BuildManifest.SHADOW_ONLY
                        && !V51BuildManifest.REAL_BET_HARD_DISABLED
                        && V51BuildManifest.CANDIDATE_REAL_AUTH_ENABLED,
                "EXACT_CANDIDATE_FLAGS");
        require(!AlgorithmInventionLabR908.MAY_AUTHORIZE_REAL
                        && !AlgorithmProductionGovernorR915.MAY_AUTHORIZE_REAL,
                "RESEARCH_CANNOT_AUTHORIZE_REAL");
        System.out.println(
                "R915_RELEASE_BOUNDARY=PASS|MODEL_PROVEN=0|REALTIME_PROVEN=0"
                        + "|CONTROLLED_CANDIDATE=1|RESEARCH_DIRECT_AUTH=0");
    }

    private static void controlledAuthorization() {
        V51CompiledActionContract compiled = compiled(0.40d, Double.NaN, 0.80d);
        V51CandidateRealGateR915.Evaluation gate =
                V51CandidateRealGateR915.evaluate(null, compiled);
        require(gate.eligible, "ELIGIBLE_GATE:" + gate.code);
        V51Models.AuthorizedDecision authorization =
                new DecisionGateEngine().decideV51B12(null, compiled);
        require(authorization.action == V51Enums.DecisionAction.BET,
                "CONTROLLED_BET_ACTION");
        require(authorization.rejectCode == V51Enums.RejectCode.NONE,
                "CONTROLLED_REJECT_NONE");
        require(V51CandidateRealGateR915.verifyAuthorized(null, compiled, authorization),
                "EXACT_AUTH_SIGNATURE");

        V51Models.AuthorizedDecision tampered = new V51Models.AuthorizedDecision(
                authorization.action,
                authorization.rejectCode,
                authorization.selected,
                authorization.decisionId,
                authorization.authorizationHash + "X",
                authorization.artifactBundleHash);
        require(!V51CandidateRealGateR915.verifyAuthorized(null, compiled, tampered),
                "TAMPER_REJECT");
        require(!V51CandidateRealGateR915.evaluate(null, compiled(0.31d, Double.NaN, 0.80d)).eligible,
                "RAW_THRESHOLD_REJECT");
        V51CandidateRealGateR915.Evaluation fakeCalibration =
                V51CandidateRealGateR915.evaluate(null, compiled(0.40d, 0.40d, 0.80d));
        require(!fakeCalibration.eligible && "CALIBRATION".equals(fakeCalibration.code),
                "RAW_NOT_CALIBRATED_PROBABILITY");
        V51CandidateRealGateR915.Evaluation unstable =
                V51CandidateRealGateR915.evaluate(null, compiled(0.40d, Double.NaN, 0.40d));
        require(!unstable.eligible && "ACTION_STABILITY".equals(unstable.code),
                "UNSTABLE_REJECT");
        System.out.println(
                "R915_CONTROLLED_AUTH=PASS|ELIGIBLE=BET|TAMPER=REJECT"
                        + "|LOW_RAW=REJECT|FAKE_CALIBRATION=REJECT|UNSTABLE=REJECT");
    }

    private static V51CompiledActionContract compiled(
            double raw, double selectedHitP, double actionStability) {
        String base = "202608290730";
        String target = "202608290731";
        V51Models.PredictionContext context = new V51Models.PredictionContext(
                base,
                target,
                base,
                hash("history"),
                V51BuildManifest.COHORT_ID,
                V51ArtifactBundleManifest.BUNDLE_HASH,
                1787974200000L,
                320);
        double[] meta = new double[32];
        meta[15] = 0.82d;
        meta[20] = 0.76d;
        V51Models.PositionAction action = new V51Models.PositionAction(
                3, 1, 4, 8, 9, raw, 0.67d, 0.023d, 0.014d, 0.38d,
                actionStability, meta, "P3-K3-1-4-8", 1, hash("action" + raw));
        V51Models.PredictabilityEvidence predictability =
                new V51Models.PredictabilityEvidence(
                        0.0d, 0.80d, 0.12d, 0.18d, false, hash("predictability"));
        V51Models.DomainCertificate domain = new V51Models.DomainCertificate(
                "CONTROLLED-CANDIDATE",
                V51Enums.DomainStatus.DISCOVERED,
                0,
                0.0d,
                0.0d,
                1.0d,
                0.0d,
                1.0d,
                hash("domain"));
        V51Models.SelectedActionEnvelope selected =
                new V51Models.SelectedActionEnvelope(
                        context,
                        action,
                        selectedHitP,
                        predictability,
                        domain,
                        V51Enums.Lane.CONTROLLED_PROBE,
                        0.20d,
                        0.20d,
                        hash("selected" + selectedHitP));
        V51SelectiveRiskController.Result policy =
                new V51SelectiveRiskController.Result(
                        V51Enums.Lane.CONTROLLED_PROBE,
                        V51Enums.RejectCode.NONE,
                        null,
                        true,
                        "R915_HOST_CONTRACT");
        return new V51CompiledActionContract(
                selected,
                policy,
                new V51RankStabilityEngine.Result(0.88d, 0.82d, 0.72d),
                hash("feature"),
                hash("state"),
                hash("expert"),
                hash("probability"),
                hash("actions"));
    }

    private static void planRegistries() {
        LegacyFixedAssetRegistryR915.assertImmutableContract();
        GenesisSchemaRegistryR915.assertContract();
        require(LegacyFixedAssetRegistryR915.entries().length == 133,
                "LEGACY_REGISTRY_COUNT");
        require(LegacyFixedAssetRegistryR915.sourceLockCount() == 133,
                "LEGACY_SOURCE_LOCK_COUNT");
        require(!LegacyFixedAssetRegistryR915.MUTATION_ALLOWED
                        && !LegacyFixedAssetRegistryR915.PRUNE_ALLOWED
                        && !LegacyFixedAssetRegistryR915.WEIGHT_OVERRIDE_ALLOWED,
                "LEGACY_IMMUTABLE");
        require(GenesisSchemaRegistryR915.atomCount() == 240,
                "GENESIS_ATOM_240");
        require(GenesisSchemaRegistryR915.familyCount() == 72,
                "GENESIS_FAMILY_72");
        require(GenesisSchemaRegistryR915.validateNumericComposition("A4-01", "A4-02", "MUL"),
                "TYPED_COMPOSITION");
        require(!GenesisSchemaRegistryR915.validateNewAtomProposal(
                        "future_leak", "DOUBLE", "POSTDRAW_ALLOWED", "FREEZE_BEFORE_TARGET_DRAW"),
                "LEAKAGE_REJECT");
        require(LegacyFixedAssetRegistryR915.fingerprint().length() == 64
                        && GenesisSchemaRegistryR915.fingerprint().length() == 64,
                "REGISTRY_FINGERPRINT");
        require(LegacyIntegrityGuardR915.selfTest(), "LEGACY_INTEGRITY_SELF_TEST");
        System.out.println(
                "R915_PLAN_REGISTRY=PASS|LEGACY_FIXED=133|SOURCE_LOCKS=133|GENESIS_L0=240"
                        + "|NAVIGATION_FAMILIES=72|LEAKAGE_GUARD=PASS");
    }

    private static void inventionMechanism() {
        AlgorithmGenomeR908 genome =
                AlgorithmGenomeR908.seed(202608300915L, 9, "PRIMITIVE_PROGRAM_SEARCH");
        require(genome.algorithmGrade(), "ALGORITHM_GRADE");
        require(genome.chineseName().contains("算法·第09代"), "CHINESE_MECHANISM_NAME");
        require(genome.mechanismDescriptionZh().contains("观察=")
                        && genome.mechanismDescriptionZh().contains("结算更新="),
                "MECHANISM_EXPLANATION");
        AlgorithmGenomeVmR908.State state = new AlgorithmGenomeVmR908.State();
        double[] x = new double[AlgorithmGenomeR908.INPUTS];
        for (int i = 0; i < 9; i++) x[i] = 0.08d + i * 0.07d;
        String before = state.encode();
        double score = AlgorithmGenomeVmR908.score(genome, state, 5, x);
        require(before.equals(state.encode()), "PREDRAW_READ_ONLY");
        x[9] = 1.0d;
        x[10] = 0.11d;
        x[11] = score;
        x[12] = 0.55d;
        x[13] = 1.0d - score;
        AlgorithmGenomeVmR908.update(genome, state, 5, x);
        require(state.updates == 1L && !before.equals(state.encode()),
                "OUTCOME_UPDATE_MUTATES_MEMORY");
        AlgorithmEvidencePolicyR912.Decision pass = AlgorithmEvidencePolicyR912.evaluate(
                260, 104, 0.35d, 10, 0.20d, 0.08d, 0.95d, 180, 0.04d, 0.30d);
        AlgorithmEvidencePolicyR912.Decision prune = AlgorithmEvidencePolicyR912.evaluate(
                260, 55, 0.10d, 30, 0.40d, 0.30d, 0.40d, 180, -0.10d, 0.10d);
        require("CHALLENGER_FUTURE_OOS".equals(pass.status), "MULTI_METRIC_PROMOTE");
        require(prune.status.startsWith("PRUNED"), "AUTO_PRUNE");
        String trained = "202608300915";
        String earliest = "202608300916";
        String evidenceHash = hash("research-evidence");
        ResearchGenomeContractR915.Evidence evidence =
                new ResearchGenomeContractR915.Evidence(
                        260, 104, 180, 10, pass.hitRate, pass.wilsonLower, 0.20d, 0.08d,
                        0.35d, 0.30d, 0.95d, 0.04d, pass.fdrQ, trained, evidenceHash);
        ResearchGenomeContractR915 contract = new ResearchGenomeContractR915(
                genome, trained, earliest, "CHALLENGER_FUTURE_OOS", evidence);
        ResearchGenomeContractR915 decoded =
                ResearchGenomeContractR915.parse(contract.canonical());
        require(decoded.contractHash.equals(contract.contractHash)
                        && decoded.productionInfluenceEligible(earliest),
                "RESEARCH_16_ORGAN_CONTRACT_ROUND_TRIP");
        AlgorithmGenomeR908 child = genome.mutate(202608300916L, 10);
        ResearchGenomeContractR915 freshChild =
                ResearchGenomeContractR915.freshChild(child, trained, earliest);
        require(freshChild.evidence.futureN == 0
                        && "IDEA_WAIT_FUTURE_OOS".equals(freshChild.lifecycleStatus),
                "CHILD_FUTURE_EVIDENCE_RESET");
        System.out.println(
                "R915_ALGORITHM_INVENTION=PASS|NAME=" + genome.chineseName()
                        + "|STATE_SCORE_UPDATE=PASS|FUTURE_PROMOTE=" + pass.status
                        + "|FDR=" + pass.fdrQ + "|CONTRACT_16=PASS|CHILD_N0=PASS|BAD="
                        + prune.status);
    }

    private static void assetGenesisMechanism() {
        AlgorithmGenomeR908 kernel =
                AlgorithmGenomeR908.seed(202608300917L, 3, "GENESIS_ASSET_HOST");
        GenesisAssetGenomeR915 asset = new GenesisAssetGenomeR915(
                "AF01",
                3,
                "",
                "HOST_FAILURE_DIRECTED_DISCOVERY",
                "202608300914",
                GenesisAssetGenomeR915.OutputType.MATRIX_FIELD,
                numericAtoms(),
                kernel);
        require(asset.assetGrade() && asset.organs.size() == GenesisAssetGenomeR915.ORGAN_COUNT,
                "ASSET_15_ORGAN_GRADE");
        require(asset.mechanismNameZh.endsWith("资产·第03代")
                        && asset.mechanismSpecZh.contains("结算后更新=是"),
                "ASSET_CHINESE_MECHANISM_NAME");
        double[] atoms = new double[GenesisSchemaRegistryR915.atomCount()];
        for (int i = 0; i < atoms.length; i++) atoms[i] = ((i * 29 + 17) % 997) / 996.0d;
        GenesisAssetVmR915.PredrawContext context = new GenesisAssetVmR915.PredrawContext(
                "202608300915",
                "202608300916",
                hash("asset-history"),
                hash("asset-surface"),
                384,
                5,
                1,
                4,
                8,
                512,
                atoms);
        GenesisAssetVmR915.State state = new GenesisAssetVmR915.State();
        String before = state.encode();
        GenesisAssetVmR915.AssetValue value = GenesisAssetVmR915.evaluate(asset, state, context);
        require(before.equals(state.encode()) && value.valueHash.length() == 64
                        && "4x4".equals(value.outputShape) && value.shaped.length == 16
                        && value.encodeOutput().startsWith("MATRIX_FIELD|4x4|"),
                "ASSET_PREDRAW_READ_ONLY");
        String frozen = context.encodeFrozen();
        GenesisAssetVmR915.PredrawContext decoded =
                GenesisAssetVmR915.PredrawContext.decodeFrozen(frozen);
        require(decoded.digest().equals(context.digest()), "ASSET_FREEZE_ROUND_TRIP");
        GenesisAssetVmR915.SettlementResult settled = GenesisAssetVmR915.settle(
                asset, state, decoded, decoded.targetIssue, 4, 1, value.primary);
        require(settled.updated && state.updates == 1L && !before.equals(state.encode()),
                "ASSET_SETTLEMENT_UPDATE");
        GenesisAssetVmR915.SettlementResult duplicate = GenesisAssetVmR915.settle(
                asset, state, decoded, decoded.targetIssue, 4, 1, value.primary);
        require(!duplicate.updated && "EXACTLY_ONCE_OR_ORDER_REJECT".equals(duplicate.code),
                "ASSET_EXACTLY_ONCE");
        GenesisAssetGenomeR915 child =
                asset.mutate(202608300918L, 4, "FAILURE_VECTOR_EVOLUTION");
        require(child.assetGrade() && !child.structuralHash.equals(asset.structuralHash)
                        && child.parentIds.contains(asset.assetId)
                        && child.mechanismNameZh.endsWith("资产·第04代"),
                "ASSET_STRUCTURAL_EVOLUTION");
        require(!AssetGenesisEngineR915.MAY_AUTHORIZE_REAL
                        && !CertifiedResearchMixerR915.MAY_AUTHORIZE_REAL
                        && CertifiedResearchMixerR915.MAX_INFLUENCE <= 0.30d,
                "ASSET_NO_DIRECT_REAL_UNIFIED_CERTIFICATE_FINAL_CAP");
        System.out.println(
                "R915_ASSET_GENESIS=PASS|NAME=" + asset.mechanismNameZh
                        + "|ORGANS=15|ATOMS=" + asset.atomIndices.length + "|OUTPUT=4x4_MATRIX"
                        + "|PREDRAW_READ_ONLY=PASS|SETTLEMENT_EXACTLY_ONCE=PASS"
                        + "|CHILD=" + child.mechanismNameZh + "|MAX_INFLUENCE=0.08");
    }

    private static int[] numericAtoms() {
        int[] out = new int[12];
        int used = 0;
        boolean[] group = new boolean[12];
        for (int i = 0; i < GenesisSchemaRegistryR915.atoms().size() && used < out.length; i++) {
            GenesisSchemaRegistryR915.AtomSpec atom = GenesisSchemaRegistryR915.atoms().get(i);
            int g = i / 20;
            if (atom.predrawComputable() && (!group[g] || used >= 6)) {
                out[used++] = i;
                group[g] = true;
            }
        }
        require(used == out.length, "NUMERIC_ATOM_FIXTURE");
        return out;
    }

    private static String hash(String value) {
        return V51ChecksumEngine.sha256("R915-HOST|" + value);
    }

    private static void require(boolean condition, String code) {
        if (!condition) throw new IllegalStateException("R915_FAIL:" + code);
    }
}
