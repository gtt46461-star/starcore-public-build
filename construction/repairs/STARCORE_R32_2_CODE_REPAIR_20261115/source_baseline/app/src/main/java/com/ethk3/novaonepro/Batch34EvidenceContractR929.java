package com.ethk3.novaonepro;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Batch-3/4 immutable evidence contracts.
 *
 * <p>This class deliberately owns no prediction, settlement, persistence or BET creation. It
 * validates frozen training inputs and atomic artifact bundles before the existing production
 * owners may consume them. Missing evidence always fails closed; a structural PASS is not a
 * claim that a model has reached an effect target.</p>
 */
public final class Batch34EvidenceContractR929 {
    public static final String VERSION = "R929_BATCH34_EVIDENCE_CONTRACT_V2";
    public static final String DATASET_SCHEMA = "R929_PIT_1200_META96_V2";
    public static final String GAME = "Binance1-PK10";
    public static final String LABEL_SCHEMA = "POSITION_NUMBER_CLASS_AND_K3_HIT_V2";
    public static final int MIN_PRODUCTION_ISSUES = 1200;
    public static final int ACTIONS_PER_ISSUE = 1200;
    public static final int POSITIONS = 10;
    public static final int NUMBER_CLASSES = 10;
    public static final int MIN_ACTION_FEATURES = 96;

    private static final String[] REQUIRED_ARTIFACT_IDS = {
        "DATASET",
        "FEATURE_MANIFEST",
        "NUMBER_MODEL",
        "STATE_TRAJECTORY",
        "TRANSITION_TENSOR",
        "ACTION_RANKER",
        "EXPERT_GATE",
        "RELATION_GRAPH",
        "SELECTED_CALIBRATION",
        "UNCERTAINTY",
        "PREDICTABILITY",
        "POLICY",
        "WINDOW_CERTIFICATE_V2",
        "DOMAIN_CERTIFICATE",
        "PARITY_EVIDENCE",
        "ROLLBACK_PLAN"
    };

    private Batch34EvidenceContractR929() {}

    public static final class Check {
        public final boolean passed;
        public final List<String> codes;
        public final String evidenceHash;

        Check(List<String> failures, String canonical) {
            ArrayList<String> copy = new ArrayList<String>();
            if (failures != null) copy.addAll(failures);
            this.codes = Collections.unmodifiableList(copy);
            this.passed = copy.isEmpty();
            this.evidenceHash = sha256(VERSION + "|" + safe(canonical) + "|" + join(copy, ","));
        }

        public String primaryCode() {
            return passed ? "PASS" : codes.get(0);
        }

        public String canonical() {
            return (passed ? "PASS" : "FAIL")
                    + "|codes=" + join(codes, ",")
                    + "|evidence=" + evidenceHash;
        }
    }

    /** Frozen dataset identity with explicit absence rather than zero substitution. */
    public static final class DatasetManifest {
        public final String schema;
        public final String game;
        public final String sourceSha256;
        public final String featureManifestSha256;
        public final String splitSha256;
        public final String datasetSha256;
        public final String startIssue;
        public final String endIssue;
        public final String labelSchema;
        public final int issueCount;
        public final int groupSize;
        public final int actionFeatureCount;
        public final int labelPositions;
        public final int labelClasses;
        public final int pitViolations;
        public final int postDrawFeatureCount;
        public final int synthesizedRows;
        public final int zeroFilledMissingValues;
        public final int explicitMissingValues;
        public final boolean featureManifestFrozen;
        public final boolean upstreamRawAvailable;
        public final boolean gapsPreserved;
        public final boolean chronologicalSplit;
        public final boolean threePositionViewsPresent;

        public DatasetManifest(
                String schema,
                String game,
                String sourceSha256,
                String featureManifestSha256,
                String splitSha256,
                String datasetSha256,
                String startIssue,
                String endIssue,
                String labelSchema,
                int issueCount,
                int groupSize,
                int actionFeatureCount,
                int labelPositions,
                int labelClasses,
                int pitViolations,
                int postDrawFeatureCount,
                int synthesizedRows,
                int zeroFilledMissingValues,
                int explicitMissingValues,
                boolean featureManifestFrozen,
                boolean upstreamRawAvailable,
                boolean gapsPreserved,
                boolean chronologicalSplit,
                boolean threePositionViewsPresent) {
            this.schema = safe(schema);
            this.game = safe(game);
            this.sourceSha256 = safe(sourceSha256);
            this.featureManifestSha256 = safe(featureManifestSha256);
            this.splitSha256 = safe(splitSha256);
            this.datasetSha256 = safe(datasetSha256);
            this.startIssue = safe(startIssue);
            this.endIssue = safe(endIssue);
            this.labelSchema = safe(labelSchema);
            this.issueCount = issueCount;
            this.groupSize = groupSize;
            this.actionFeatureCount = actionFeatureCount;
            this.labelPositions = labelPositions;
            this.labelClasses = labelClasses;
            this.pitViolations = pitViolations;
            this.postDrawFeatureCount = postDrawFeatureCount;
            this.synthesizedRows = synthesizedRows;
            this.zeroFilledMissingValues = zeroFilledMissingValues;
            this.explicitMissingValues = explicitMissingValues;
            this.featureManifestFrozen = featureManifestFrozen;
            this.upstreamRawAvailable = upstreamRawAvailable;
            this.gapsPreserved = gapsPreserved;
            this.chronologicalSplit = chronologicalSplit;
            this.threePositionViewsPresent = threePositionViewsPresent;
        }

        public String canonical() {
            return schema + "|" + game + "|" + sourceSha256 + "|" + featureManifestSha256
                    + "|" + splitSha256 + "|" + datasetSha256 + "|" + startIssue + "|"
                    + endIssue + "|" + labelSchema + "|" + issueCount + "|" + groupSize
                    + "|" + actionFeatureCount + "|" + labelPositions + "|" + labelClasses
                    + "|" + pitViolations + "|" + postDrawFeatureCount + "|" + synthesizedRows
                    + "|" + zeroFilledMissingValues + "|" + explicitMissingValues + "|"
                    + featureManifestFrozen + "|" + upstreamRawAvailable + "|" + gapsPreserved
                    + "|" + chronologicalSplit + "|" + threePositionViewsPresent;
        }
    }

    public static Check validateDataset(DatasetManifest m, boolean production) {
        ArrayList<String> f = new ArrayList<String>();
        if (m == null) {
            f.add("D01_DATASET_MANIFEST_NULL");
            return new Check(f, "");
        }
        if (!DATASET_SCHEMA.equals(m.schema)) f.add("D01_SCHEMA_MISMATCH");
        if (!GAME.equals(m.game)) f.add("D01_GAME_MISMATCH");
        if (!LABEL_SCHEMA.equals(m.labelSchema)) f.add("D01_LABEL_SCHEMA_MISMATCH");
        if (!isSha256(m.sourceSha256)) f.add("D01_SOURCE_HASH_INVALID");
        if (!isSha256(m.featureManifestSha256)) f.add("D01_FEATURE_MANIFEST_HASH_INVALID");
        if (!isSha256(m.splitSha256)) f.add("D01_SPLIT_HASH_INVALID");
        if (!isSha256(m.datasetSha256)) f.add("D01_DATASET_HASH_INVALID");
        if (!validIssue(m.startIssue) || !validIssue(m.endIssue)
                || compareIssue(m.startIssue, m.endIssue) >= 0) {
            f.add("D01_ISSUE_RANGE_INVALID");
        }
        int minimum = production ? MIN_PRODUCTION_ISSUES : 1;
        if (m.issueCount < minimum) f.add("D01_SUPPORT_LT_" + minimum);
        if (m.groupSize != ACTIONS_PER_ISSUE) f.add("D01_GROUP_SIZE_NOT_1200");
        if (m.actionFeatureCount < MIN_ACTION_FEATURES) f.add("D01_ACTION_FEATURES_LT_96");
        if (m.labelPositions != POSITIONS) f.add("D01_LABEL_POSITION_COUNT_NOT_10");
        if (m.labelClasses != NUMBER_CLASSES) f.add("D01_LABEL_CLASS_COUNT_NOT_10");
        if (m.pitViolations != 0) f.add("D01_PIT_VIOLATION");
        if (m.postDrawFeatureCount != 0) f.add("D01_POSTDRAW_FEATURE_PRESENT");
        if (m.synthesizedRows != 0) f.add("D01_SYNTHETIC_ROW_PRESENT");
        if (m.zeroFilledMissingValues != 0) f.add("D01_ZERO_FILLED_ABSENCE");
        if (m.explicitMissingValues < 0) f.add("D01_MISSING_COUNT_INVALID");
        if (!m.featureManifestFrozen) f.add("D01_FEATURE_MANIFEST_NOT_FROZEN");
        if (!m.gapsPreserved) f.add("D01_GAPS_NOT_PRESERVED");
        if (!m.chronologicalSplit) f.add("D01_SPLIT_NOT_CHRONOLOGICAL");
        if (!m.threePositionViewsPresent) f.add("D01_THREE_POSITION_LABEL_VIEWS_MISSING");
        if (production && !m.upstreamRawAvailable) f.add("D01_UPSTREAM_RAW_BLOCK_UNAVAILABLE");
        return new Check(f, m.canonical());
    }

    public static final class ArtifactNode {
        public final String id;
        public final String generationId;
        public final String sha256;
        public final String contractSha256;
        public final List<String> dependencies;
        public final String trainedThroughIssue;
        public final String evaluatedFromIssue;
        public final String evaluatedThroughIssue;
        public final int support;
        public final boolean trained;
        public final boolean parityPassed;
        public final boolean futureOosPassed;
        public final boolean deterministicReplayPassed;

        public ArtifactNode(
                String id,
                String generationId,
                String sha256,
                String contractSha256,
                List<String> dependencies,
                String trainedThroughIssue,
                String evaluatedFromIssue,
                String evaluatedThroughIssue,
                int support,
                boolean trained,
                boolean parityPassed,
                boolean futureOosPassed,
                boolean deterministicReplayPassed) {
            this.id = safe(id).toUpperCase(Locale.US);
            this.generationId = safe(generationId);
            this.sha256 = safe(sha256);
            this.contractSha256 = safe(contractSha256);
            ArrayList<String> d = new ArrayList<String>();
            if (dependencies != null) {
                for (String value : dependencies) d.add(safe(value).toUpperCase(Locale.US));
            }
            this.dependencies = Collections.unmodifiableList(d);
            this.trainedThroughIssue = safe(trainedThroughIssue);
            this.evaluatedFromIssue = safe(evaluatedFromIssue);
            this.evaluatedThroughIssue = safe(evaluatedThroughIssue);
            this.support = support;
            this.trained = trained;
            this.parityPassed = parityPassed;
            this.futureOosPassed = futureOosPassed;
            this.deterministicReplayPassed = deterministicReplayPassed;
        }

        public String canonical() {
            return id + "|" + generationId + "|" + sha256 + "|" + contractSha256 + "|"
                    + join(dependencies, ",") + "|" + trainedThroughIssue + "|"
                    + evaluatedFromIssue + "|" + evaluatedThroughIssue + "|" + support + "|"
                    + trained + "|" + parityPassed + "|" + futureOosPassed + "|"
                    + deterministicReplayPassed;
        }
    }

    public static final class BundleManifest {
        public final String generationId;
        public final String createdAtUtc;
        public final String previousBundleSha256;
        public final String bundleSha256;
        public final String rollbackGroup;
        public final List<ArtifactNode> nodes;

        public BundleManifest(
                String generationId,
                String createdAtUtc,
                String previousBundleSha256,
                String bundleSha256,
                String rollbackGroup,
                List<ArtifactNode> nodes) {
            this.generationId = safe(generationId);
            this.createdAtUtc = safe(createdAtUtc);
            this.previousBundleSha256 = safe(previousBundleSha256);
            this.bundleSha256 = safe(bundleSha256);
            this.rollbackGroup = safe(rollbackGroup);
            ArrayList<ArtifactNode> copy = new ArrayList<ArtifactNode>();
            if (nodes != null) copy.addAll(nodes);
            this.nodes = Collections.unmodifiableList(copy);
        }

        public String canonicalWithoutHash() {
            StringBuilder out = new StringBuilder();
            out.append(generationId).append('|').append(createdAtUtc).append('|')
                    .append(previousBundleSha256).append('|').append(rollbackGroup);
            for (ArtifactNode node : nodes) out.append("\n").append(node == null ? "NULL" : node.canonical());
            return out.toString();
        }

        public String calculatedSha256() {
            return sha256("R929_ATOMIC_BUNDLE|" + canonicalWithoutHash());
        }
    }

    public static List<String> requiredArtifactIds() {
        return Collections.unmodifiableList(
                Arrays.asList((String[]) REQUIRED_ARTIFACT_IDS.clone()));
    }

    public static Check validateBundle(BundleManifest bundle, boolean production) {
        ArrayList<String> f = new ArrayList<String>();
        if (bundle == null) {
            f.add("D10_BUNDLE_NULL");
            return new Check(f, "");
        }
        if (bundle.generationId.length() == 0) f.add("D10_GENERATION_EMPTY");
        if (bundle.rollbackGroup.length() == 0) f.add("D10_ROLLBACK_GROUP_EMPTY");
        if (!isSha256(bundle.bundleSha256)) f.add("D10_BUNDLE_HASH_INVALID");
        if (bundle.previousBundleSha256.length() > 0 && !isSha256(bundle.previousBundleSha256)) {
            f.add("D10_PREVIOUS_BUNDLE_HASH_INVALID");
        }
        if (!bundle.calculatedSha256().equalsIgnoreCase(bundle.bundleSha256)) {
            f.add("D10_BUNDLE_HASH_MISMATCH");
        }

        LinkedHashMap<String, ArtifactNode> byId = new LinkedHashMap<String, ArtifactNode>();
        for (ArtifactNode node : bundle.nodes) {
            if (node == null) {
                f.add("D10_NULL_ARTIFACT");
                continue;
            }
            if (node.id.length() == 0 || byId.containsKey(node.id)) {
                f.add("D10_DUPLICATE_OR_EMPTY_ARTIFACT:" + node.id);
                continue;
            }
            byId.put(node.id, node);
            if (!bundle.generationId.equals(node.generationId)) {
                f.add("D10_MIXED_GENERATION:" + node.id);
            }
            if (!isSha256(node.sha256)) f.add("D10_ARTIFACT_HASH_INVALID:" + node.id);
            if (!isSha256(node.contractSha256)) f.add("D10_CONTRACT_HASH_INVALID:" + node.id);
            if (node.support < 0) f.add("D10_NEGATIVE_SUPPORT:" + node.id);
            if (production) {
                if (!node.trained) f.add("D10_UNTRAINED:" + node.id);
                if (!node.parityPassed) f.add("D10_PARITY_NOT_PASSED:" + node.id);
                if (!node.futureOosPassed) f.add("D10_FUTURE_OOS_NOT_PASSED:" + node.id);
                if (!node.deterministicReplayPassed) {
                    f.add("D10_REPLAY_NOT_DETERMINISTIC:" + node.id);
                }
            }
        }

        for (String required : REQUIRED_ARTIFACT_IDS) {
            if (!byId.containsKey(required)) f.add("D10_REQUIRED_ARTIFACT_MISSING:" + required);
        }
        for (ArtifactNode node : byId.values()) {
            for (String dependency : node.dependencies) {
                if (!byId.containsKey(dependency)) {
                    f.add("D10_DEPENDENCY_MISSING:" + node.id + "->" + dependency);
                }
                if (node.id.equals(dependency)) f.add("D10_SELF_DEPENDENCY:" + node.id);
            }
        }
        if (hasCycle(byId)) f.add("D10_DEPENDENCY_CYCLE");

        for (ArtifactNode node : byId.values()) {
            if (node.trained
                    && validIssue(node.trainedThroughIssue)
                    && validIssue(node.evaluatedFromIssue)
                    && compareIssue(node.trainedThroughIssue, node.evaluatedFromIssue) >= 0) {
                f.add("D02_TEMPORAL_LEAKAGE:" + node.id);
            }
            if (node.evaluatedThroughIssue.length() > 0
                    && (!validIssue(node.evaluatedThroughIssue)
                    || compareIssue(node.evaluatedFromIssue, node.evaluatedThroughIssue) > 0)) {
                f.add("D02_EVALUATION_RANGE_INVALID:" + node.id);
            }
        }
        return new Check(f, bundle.canonicalWithoutHash());
    }

    public static final class AtomicSwitchToken {
        public final String expectedCurrentBundleSha256;
        public final String nextBundleSha256;
        public final String nextGenerationId;
        public final String rollbackGroup;
        public final long monotonicSequence;
        public final String tokenSha256;

        public AtomicSwitchToken(
                String expectedCurrentBundleSha256,
                String nextBundleSha256,
                String nextGenerationId,
                String rollbackGroup,
                long monotonicSequence,
                String tokenSha256) {
            this.expectedCurrentBundleSha256 = safe(expectedCurrentBundleSha256);
            this.nextBundleSha256 = safe(nextBundleSha256);
            this.nextGenerationId = safe(nextGenerationId);
            this.rollbackGroup = safe(rollbackGroup);
            this.monotonicSequence = monotonicSequence;
            this.tokenSha256 = safe(tokenSha256);
        }

        public String canonicalWithoutHash() {
            return expectedCurrentBundleSha256 + "|" + nextBundleSha256 + "|" + nextGenerationId
                    + "|" + rollbackGroup + "|" + monotonicSequence;
        }

        public String calculatedSha256() {
            return sha256("R929_ATOMIC_SWITCH|" + canonicalWithoutHash());
        }
    }

    public static Check validateSwitch(
            String observedCurrentBundleSha256,
            BundleManifest next,
            AtomicSwitchToken token,
            long lastCommittedSequence) {
        ArrayList<String> f = new ArrayList<String>();
        if (next == null) f.add("D10_SWITCH_NEXT_NULL");
        if (token == null) {
            f.add("D10_SWITCH_TOKEN_NULL");
            return new Check(f, safe(observedCurrentBundleSha256));
        }
        if (!isSha256(token.expectedCurrentBundleSha256)) f.add("D10_SWITCH_EXPECTED_HASH_INVALID");
        if (!isSha256(token.nextBundleSha256)) f.add("D10_SWITCH_NEXT_HASH_INVALID");
        if (!isSha256(token.tokenSha256)) f.add("D10_SWITCH_TOKEN_HASH_INVALID");
        if (!token.calculatedSha256().equalsIgnoreCase(token.tokenSha256)) {
            f.add("D10_SWITCH_TOKEN_HASH_MISMATCH");
        }
        if (!token.expectedCurrentBundleSha256.equalsIgnoreCase(safe(observedCurrentBundleSha256))) {
            f.add("D10_SWITCH_CAS_CONFLICT");
        }
        if (token.monotonicSequence <= lastCommittedSequence) f.add("D10_SWITCH_SEQUENCE_REPLAY");
        if (next != null) {
            if (!token.nextBundleSha256.equalsIgnoreCase(next.bundleSha256)) {
                f.add("D10_SWITCH_BUNDLE_HASH_MISMATCH");
            }
            if (!token.nextGenerationId.equals(next.generationId)) {
                f.add("D10_SWITCH_GENERATION_MISMATCH");
            }
            if (!token.rollbackGroup.equals(next.rollbackGroup)) {
                f.add("D10_SWITCH_ROLLBACK_GROUP_MISMATCH");
            }
            Check bundleCheck = validateBundle(next, false);
            if (!bundleCheck.passed) {
                for (String code : bundleCheck.codes) f.add("D10_SWITCH_" + code);
            }
        }
        return new Check(f, token.canonicalWithoutHash());
    }

    private static boolean hasCycle(Map<String, ArtifactNode> byId) {
        HashMap<String, Integer> marks = new HashMap<String, Integer>();
        for (String id : byId.keySet()) {
            if (cycleFrom(id, byId, marks)) return true;
        }
        return false;
    }

    private static boolean cycleFrom(
            String id, Map<String, ArtifactNode> byId, Map<String, Integer> marks) {
        Integer mark = marks.get(id);
        if (mark != null && mark.intValue() == 1) return true;
        if (mark != null && mark.intValue() == 2) return false;
        marks.put(id, Integer.valueOf(1));
        ArtifactNode node = byId.get(id);
        if (node != null) {
            for (String dependency : node.dependencies) {
                if (byId.containsKey(dependency) && cycleFrom(dependency, byId, marks)) return true;
            }
        }
        marks.put(id, Integer.valueOf(2));
        return false;
    }

    public static boolean isSha256(String value) {
        String x = safe(value);
        if (x.length() != 64) return false;
        for (int i = 0; i < x.length(); i++) {
            char c = x.charAt(i);
            if (!((c >= '0' && c <= '9')
                    || (c >= 'a' && c <= 'f')
                    || (c >= 'A' && c <= 'F'))) return false;
        }
        return true;
    }

    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(safe(value).getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(64);
            for (byte b : bytes) out.append(String.format(Locale.US, "%02x", b & 0xff));
            return out.toString();
        } catch (Exception error) {
            throw new IllegalStateException("R929_SHA256_UNAVAILABLE", error);
        }
    }

    static boolean validIssue(String issue) {
        String x = safe(issue);
        if (x.length() == 0) return false;
        for (int i = 0; i < x.length(); i++) {
            char c = x.charAt(i);
            if (c < '0' || c > '9') return false;
        }
        return true;
    }

    static int compareIssue(String a, String b) {
        String x = safe(a);
        String y = safe(b);
        if (x.length() != y.length()) return x.length() < y.length() ? -1 : 1;
        return x.compareTo(y);
    }

    static String join(Iterable<String> values, String separator) {
        StringBuilder out = new StringBuilder();
        if (values != null) {
            for (String value : values) {
                if (out.length() > 0) out.append(separator);
                out.append(safe(value));
            }
        }
        return out.toString();
    }

    static String safe(String value) {
        return value == null ? "" : value;
    }
}
