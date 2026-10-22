package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.List;

/**
 * Dependency-free R815 acceptance checks.  These test business-issue ordering, duplicate versus
 * conflict handling, immutable 96-feature projection, research binding, and non-negotiable
 * SHADOW-only release boundaries without pretending to be a physical-device test.
 */
public final class R815HostContractHarness {
    private static int assertions;

    public static void main(String[] args) {
        releaseIdentityContract();
        issueAndHashContract();
        historyIntegrityContract();
        primitiveAndFeatureContract();
        datasetAndAvailabilityContract();
        ablationContract();
        System.out.println(
                "R815_HOST_CONTRACTS=PASS|ASSERTIONS="
                        + assertions
                        + "|IDENTITY,SHADOW_LOCK,ISSUE_ROLLOVER,V2_HASH,NO_OPEN_TIME_IDENTITY,"
                        + "DUPLICATE_VS_CONFLICT,HISTORY_12,PRIMITIVE_100_1013,FEATURE_96,"
                        + "VECTOR_CODEC,DATASET_BINDING,AVAILABILITY,ISSUE_LEVEL_ABLATION");
    }

    private static void releaseIdentityContract() {
        require(ReleaseIdentityR815.isConsistent(), "IDENTITY_CONSISTENT");
        require("V57.16.4-R4.0.4-904".equals(ReleaseIdentityR815.VERSION_NAME), "VERSION_NAME");
        require(ReleaseIdentityR815.VERSION_CODE == 577200904, "VERSION_CODE");
        require(ReleaseIdentityR815.DB_VERSION == 36 && ReleaseIdentityR815.SCHEMA_REVISION == 3, "DB36_REV3");
        require(
                AppConfig.R815_SHADOW_ONLY
                        && !AppConfig.R815_MODEL_PROVEN
                        && AppConfig.R815_REAL_BET_HARD_DISABLED,
                "R815_SHADOW_LOCK");
        require(SourceManifestR815.valid(), "GENERATED_SOURCE_MANIFEST");
        require(BuildProvenanceR815.fromGeneratedManifest().valid, "GENERATED_PROVENANCE_READY");
    }

    private static void issueAndHashContract() {
        TargetIssueResolver.Resolution rollover = TargetIssueResolver.next("202608241440");
        require(rollover.valid && "202608250001".equals(rollover.targetIssue), "MIDNIGHT_ROLLOVER");
        Models.Draw sameBusinessOne = draw("202608241440", 0, 100L);
        Models.Draw sameBusinessTwo = draw("202608241440", 0, 999999L);
        require(
                DrawHashR36.sha256V2(sameBusinessOne).equals(DrawHashR36.sha256V2(sameBusinessTwo)),
                "V2_IGNORES_OPEN_TIME");
        require(!DrawHashR36.sha256(sameBusinessOne).equals(DrawHashR36.sha256(sameBusinessTwo)), "V1_LEGACY_OPEN_TIME");
        IssueFreshnessResolverR815.Result fresh =
                IssueFreshnessResolverR815.select(
                        "202608241440", sameBusinessOne, draw("202608250001", 1, 1L));
        require(fresh.advanced() && "BACKUP".equals(fresh.source), "FRESHNESS_BY_ISSUE_NOT_TIME");
        IssueFreshnessResolverR815.Result duplicate =
                IssueFreshnessResolverR815.select("202608241440", sameBusinessTwo, null);
        require(
                duplicate.status == IssueFreshnessResolverR815.Status.NO_NEW_ISSUE,
                "SAME_ISSUE_NOT_NEW");
    }

    private static void historyIntegrityContract() {
        List<Models.Draw> rows = history12();
        rows.add(draw("202608240012", 2, 777L));
        HistoryIntegrityVerifierR815.Result deduplicated = HistoryIntegrityVerifierR815.verifyAscending(rows);
        require(deduplicated.validRows == 12, "HISTORY_EXACT_DUPLICATE_REMOVED");
        require(
                deduplicated.duplicateCount == 1
                        && deduplicated.conflictCount == 0
                        && deduplicated.contiguousSuffix == 12
                        && deduplicated.minimalReady(),
                "HISTORY_12_MINIMUM_READY");
        List<Models.Draw> conflictRows = history12();
        conflictRows.add(draw("202608240012", 3, 1L));
        HistoryIntegrityVerifierR815.Result conflict =
                HistoryIntegrityVerifierR815.verifyAscending(conflictRows);
        require(conflict.hasHardConflict() && !conflict.minimalReady(), "HISTORY_CONFLICT_BLOCKS_OUTPUT");
        require(
                DrawHashR36.matchesPersisted(DrawHashR36.sha256(history12().get(0)), 1, history12().get(0)),
                "LEGACY_HASH_READ_COMPATIBILITY");
    }

    private static void primitiveAndFeatureContract() {
        HistoryIntegrityVerifierR815.Result history =
                HistoryIntegrityVerifierR815.verifyAscending(history12());
        String target = "202608240013";
        CrossPositionPrimitiveCacheR815.Binding cacheBinding =
                new CrossPositionPrimitiveCacheR815.Binding(
                        target,
                        history.historyHash,
                        ResearchFeatureSchemaR815.schemaSha256(),
                        history.contiguousSuffix);
        double[][] q = new double[10][10];
        for (int position = 0; position < 10; position++) {
            for (int number = 0; number < 10; number++) {
                q[position][number] = position == number ? 0.91d : 0.01d + number * 0.001d;
            }
        }
        CrossPositionPrimitiveCacheR815.clear();
        CrossPositionPrimitiveCacheR815.Snapshot cache = CrossPositionPrimitiveCacheR815.build(cacheBinding, q);
        require(cache.dpCalls == 1 && cache.voteCalls == 100, "PRIMITIVE_COMPUTE_BUDGET");
        require(cache.subsetCount() == 1013, "ALL_POSITION_SUBSETS");
        for (int position = 1; position <= 10; position++) {
            require(cache.assignedNumber(position) >= 1 && cache.assignedNumber(position) <= 10, "ASSIGNMENT_" + position);
        }
        ResearchFeatureSchemaR815.assertIntegrity();
        require(ResearchFeatureSchemaR815.SIZE == 96 && ResearchFeatureSchemaR815.indexes().size() == 96, "FEATURE_SCHEMA_96");
        String datasetHash = V51ChecksumEngine.sha256("R815_HOST_DATASET");
        ResearchFeatureVectorR815.Binding vectorBinding =
                new ResearchFeatureVectorR815.Binding(
                        ReleaseIdentityR815.GAME_CODE,
                        DatasetIdentityGateR815.Scope.SEALED_OOS,
                        target,
                        history.historyHash,
                        datasetHash,
                        ResearchFeatureSchemaR815.schemaSha256());
        ResearchFeatureVectorR815 vector =
                ResearchFeatureMaterializerR815.materialize(
                        vectorBinding,
                        cache,
                        new ResearchFeatureMaterializerR815.ActionIdentity(target, 1, 1, 2, 3, 1));
        for (int i = 0; i < ResearchFeatureSchemaR815.SIZE; i++) {
            require(vector.available(i), "FEATURE_AVAILABLE_" + i);
        }
        ResearchFeatureVectorR815 roundTrip = ResearchFeatureVectorR815.decode(vector.encode());
        require(vector.hash().equals(roundTrip.hash()) && roundTrip.value(95) > 0.0d, "FEATURE_VECTOR_CODEC");
    }

    private static void datasetAndAvailabilityContract() {
        String datasetHash = V51ChecksumEngine.sha256("R815_DATASET");
        DatasetIdentityGateR815.Manifest manifest =
                new DatasetIdentityGateR815.Manifest(
                        ReleaseIdentityR815.GAME_CODE,
                        DatasetIdentityGateR815.Scope.SEALED_OOS,
                        "202608240001",
                        "202608240012",
                        12L,
                        datasetHash,
                        ResearchFeatureSchemaR815.schemaSha256(),
                        "abcdef123456");
        DatasetIdentityGateR815.requireForProof(manifest);
        require(DatasetIdentityGateR815.artifactBinding(manifest).length() == 64, "DATASET_PROOF_BINDING");
        DatasetIdentityGateR815.Manifest legacy =
                new DatasetIdentityGateR815.Manifest(
                        "Legacy-PK10",
                        DatasetIdentityGateR815.Scope.LEGACY_CROSS_GAME_HYPOTHESIS_ONLY,
                        "202608240001",
                        "202608240012",
                        12L,
                        datasetHash,
                        ResearchFeatureSchemaR815.schemaSha256(),
                        "abcdef123456");
        require(DatasetIdentityGateR815.allowHypothesis(legacy), "LEGACY_HYPOTHESIS_ONLY");
        require(
                PredictionAvailabilityR815.CANONICAL_READY_SHADOW.hasNumbers()
                        && !PredictionAvailabilityR815.CANONICAL_READY_SHADOW.probabilityAvailable(true)
                        && !PredictionAvailabilityR815.CANONICAL_READY_PROVEN.realAuthorized(true, false),
                "AVAILABILITY_NEVER_AUTHORIZES_REAL");
        require(
                PredictionAvailabilityR815.fromPersisted("unknown")
                        == PredictionAvailabilityR815.PRODUCER_FAILED,
                "AVAILABILITY_SAFE_DEFAULT");
    }

    private static void ablationContract() {
        ResearchAblationR815.Result result =
                ResearchAblationR815.compare(
                        new ResearchAblationR815.Pair[] {
                            new ResearchAblationR815.Pair("202608240011", false, true, 0.40d, 0.20d),
                            new ResearchAblationR815.Pair("202608240012", true, true, 0.30d, 0.10d)
                        });
        require(
                result.issues == 2
                        && result.candidateWins == 1
                        && result.baselineWins == 0
                        && result.ties == 1
                        && result.hitDelta > 0.0d
                        && result.brierDelta < 0.0d
                        && result.checksum.length() == 64,
                "ISSUE_LEVEL_ABLATION");
    }

    private static List<Models.Draw> history12() {
        ArrayList<Models.Draw> out = new ArrayList<Models.Draw>();
        for (int sequence = 1; sequence <= 12; sequence++) {
            out.add(draw("20260824" + four(sequence), sequence % 10, sequence * 1000L));
        }
        return out;
    }

    private static Models.Draw draw(String issue, int shift, long openTime) {
        Models.Draw draw = new Models.Draw();
        draw.issue = issue;
        draw.openTime = openTime;
        for (int position = 0; position < 10; position++) draw.numbers[position] = ((position + shift) % 10) + 1;
        if (!draw.isValid()) throw new AssertionError("HOST_DRAW_INVALID:" + issue);
        return draw;
    }

    private static String four(int value) {
        return value < 10 ? "000" + value : value < 100 ? "00" + value : value < 1000 ? "0" + value : String.valueOf(value);
    }

    private static void require(boolean condition, String code) {
        assertions++;
        if (!condition) throw new AssertionError("R815_HOST_ASSERTION_FAILED:" + code);
    }
}
