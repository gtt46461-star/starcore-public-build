package com.ethk3.novaonepro;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Structural/behavior/residual deduplication and certificate-bounded contribution graph.
 *
 * <p>One defective research node is quarantined with its dependency descendants only. Production
 * baseline scores remain available and unchanged. Every applied contribution requires the R929
 * unified certificate for the exact consumer/candidate/structure/evidence identity and is limited
 * to ranking influence; this class has no REAL writer.</p>
 */
public final class CandidateDiversityContributionR930 {
    public static final String VERSION = "R930_DIVERSITY_CONTRIBUTION_V1";
    public static final boolean MAY_CREATE_REAL = false;

    private CandidateDiversityContributionR930() {}

    public enum SimilarityClass {
        DISTINCT,
        STRUCTURAL_DUPLICATE,
        BEHAVIOR_DUPLICATE,
        RESIDUAL_DUPLICATE,
        NEAR_DUPLICATE,
        FAMILY_REPLACEMENT
    }

    public enum NodeType {
        ATOM,
        FACTOR,
        MOLECULE,
        MATRIX,
        NUMBER_MODEL,
        RANKER,
        CALIBRATION,
        POLICY,
        CERTIFICATE,
        BUNDLE,
        CANDIDATE,
        CONTRIBUTION,
        SELECTION
    }

    public enum NodeState { ACTIVE, SHADOW, QUARANTINED, REVOKED, TOMBSTONE }

    public static final class Signature {
        public final String candidateId;
        public final String familyId;
        public final String structuralHash;
        public final String behaviorHash;
        public final String residualHash;
        private final double[] behaviorVector;
        private final double[] residualVector;
        public final int futureN;
        public final double hitRate;
        public final double coverage;
        public final double ece;
        public final double stability;
        public final long p95CostNs;
        public final String signatureHash;

        public Signature(String candidateId, String familyId, String structuralHash,
                String behaviorHash, String residualHash, double[] behaviorVector,
                double[] residualVector, int futureN, double hitRate, double coverage,
                double ece, double stability, long p95CostNs) {
            this.candidateId = required(candidateId, "SIGNATURE_CANDIDATE_ID");
            this.familyId = required(familyId, "SIGNATURE_FAMILY_ID");
            this.structuralHash = requireHash(structuralHash, "SIGNATURE_STRUCTURAL_HASH");
            this.behaviorHash = requireHash(behaviorHash, "SIGNATURE_BEHAVIOR_HASH");
            this.residualHash = requireHash(residualHash, "SIGNATURE_RESIDUAL_HASH");
            if (behaviorVector == null || residualVector == null
                    || behaviorVector.length == 0 || residualVector.length == 0
                    || behaviorVector.length != residualVector.length || futureN < 0
                    || !metricOrUnknown(hitRate) || !metricOrUnknown(coverage)
                    || !metricOrUnknown(ece) || !metricOrUnknown(stability) || p95CostNs < 0L)
                throw new IllegalArgumentException("R930_SIGNATURE_METRICS_INVALID");
            this.behaviorVector = Arrays.copyOf(behaviorVector, behaviorVector.length);
            this.residualVector = Arrays.copyOf(residualVector, residualVector.length);
            for (double value : this.behaviorVector) if (!finite(value))
                throw new IllegalArgumentException("R930_BEHAVIOR_VECTOR_NONFINITE");
            for (double value : this.residualVector) if (!finite(value))
                throw new IllegalArgumentException("R930_RESIDUAL_VECTOR_NONFINITE");
            this.futureN = futureN;
            this.hitRate = hitRate;
            this.coverage = coverage;
            this.ece = ece;
            this.stability = stability;
            this.p95CostNs = p95CostNs;
            this.signatureHash = sha256(canonical());
        }

        public double[] behaviorCopy() { return Arrays.copyOf(behaviorVector, behaviorVector.length); }
        public double[] residualCopy() { return Arrays.copyOf(residualVector, residualVector.length); }

        String canonical() {
            StringBuilder out = new StringBuilder(candidateId).append('|').append(familyId)
                    .append('|').append(structuralHash).append('|').append(behaviorHash)
                    .append('|').append(residualHash).append('|').append(futureN).append('|')
                    .append(hitRate).append('|').append(coverage).append('|').append(ece)
                    .append('|').append(stability).append('|').append(p95CostNs);
            for (double value : behaviorVector)
                out.append("|b=").append(Long.toHexString(Double.doubleToLongBits(value)));
            for (double value : residualVector)
                out.append("|r=").append(Long.toHexString(Double.doubleToLongBits(value)));
            return out.toString();
        }
    }

    public static final class Similarity {
        public final Signature left;
        public final Signature right;
        public final double behaviorCosine;
        public final double residualCosine;
        public final double behaviorAgreement;
        public final SimilarityClass classification;
        public final String reason;
        public final String similarityHash;

        Similarity(Signature left, Signature right, double behaviorCosine,
                double residualCosine, double behaviorAgreement,
                SimilarityClass classification, String reason) {
            this.left = left;
            this.right = right;
            this.behaviorCosine = behaviorCosine;
            this.residualCosine = residualCosine;
            this.behaviorAgreement = behaviorAgreement;
            this.classification = classification;
            this.reason = reason;
            this.similarityHash = sha256(left.signatureHash + '|' + right.signatureHash + '|'
                    + behaviorCosine + '|' + residualCosine + '|' + behaviorAgreement + '|'
                    + classification.name() + '|' + reason);
        }

        public boolean duplicate() { return classification != SimilarityClass.DISTINCT
                && classification != SimilarityClass.FAMILY_REPLACEMENT; }
    }

    public static Similarity compare(Signature left, Signature right) {
        if (left == null || right == null || left.behaviorVector.length != right.behaviorVector.length)
            throw new IllegalArgumentException("R930_SIMILARITY_INPUT_INVALID");
        double behaviorCosine = cosine(left.behaviorVector, right.behaviorVector);
        double residualCosine = cosine(left.residualVector, right.residualVector);
        int agreement = 0;
        for (int i = 0; i < left.behaviorVector.length; i++) {
            boolean leftPositive = left.behaviorVector[i] >= 0.0d;
            boolean rightPositive = right.behaviorVector[i] >= 0.0d;
            if (leftPositive == rightPositive) agreement++;
        }
        double behaviorAgreement = agreement / (double) left.behaviorVector.length;
        SimilarityClass classification;
        String reason;
        if (left.structuralHash.equals(right.structuralHash)) {
            classification = SimilarityClass.STRUCTURAL_DUPLICATE;
            reason = "EXACT_STRUCTURAL_HASH";
        } else if (left.behaviorHash.equals(right.behaviorHash)
                || (behaviorCosine >= 0.995d && behaviorAgreement >= 0.98d)) {
            classification = SimilarityClass.BEHAVIOR_DUPLICATE;
            reason = "BEHAVIOR_EQUIVALENT";
        } else if (left.residualHash.equals(right.residualHash)
                || residualCosine >= 0.995d) {
            classification = SimilarityClass.RESIDUAL_DUPLICATE;
            reason = "RESIDUAL_ERROR_EQUIVALENT";
        } else if (behaviorCosine >= 0.97d && residualCosine >= 0.97d) {
            classification = SimilarityClass.NEAR_DUPLICATE;
            reason = "JOINT_NEAR_DUPLICATE";
        } else if (left.familyId.equals(right.familyId)
                && dominates(left, right)) {
            classification = SimilarityClass.FAMILY_REPLACEMENT;
            reason = "SAME_FAMILY_STRICTLY_BETTER_SUPPORTED_CANDIDATE";
        } else {
            classification = SimilarityClass.DISTINCT;
            reason = "STRUCTURE_BEHAVIOR_RESIDUAL_DISTINCT";
        }
        return new Similarity(left, right, behaviorCosine, residualCosine,
                behaviorAgreement, classification, reason);
    }

    public static final class DedupeDecision {
        public final Signature proposal;
        public final boolean admit;
        public final Similarity nearest;
        public final String reason;
        public final String decisionHash;

        DedupeDecision(Signature proposal, boolean admit, Similarity nearest, String reason) {
            this.proposal = proposal;
            this.admit = admit;
            this.nearest = nearest;
            this.reason = reason;
            this.decisionHash = sha256(proposal.signatureHash + '|' + (admit ? 1 : 0) + '|'
                    + (nearest == null ? "" : nearest.similarityHash) + '|' + reason);
        }
    }

    public static DedupeDecision dedupe(Signature proposal, List<Signature> existing) {
        if (proposal == null) throw new IllegalArgumentException("R930_DEDUPE_PROPOSAL_REQUIRED");
        Similarity nearest = null;
        double nearestScore = -Double.MAX_VALUE;
        if (existing != null) {
            for (Signature candidate : existing) {
                if (candidate == null) continue;
                Similarity similarity = compare(proposal, candidate);
                double score = Math.max(similarity.behaviorCosine, similarity.residualCosine);
                if (nearest == null || score > nearestScore
                        || (score == nearestScore && similarity.right.candidateId.compareTo(
                                nearest.right.candidateId) < 0)) {
                    nearest = similarity;
                    nearestScore = score;
                }
                if (similarity.duplicate())
                    return new DedupeDecision(proposal, false, similarity,
                            "REJECT_" + similarity.classification.name());
            }
        }
        return new DedupeDecision(proposal, true, nearest,
                nearest != null && nearest.classification == SimilarityClass.FAMILY_REPLACEMENT
                        ? "ADMIT_AS_FAMILY_REPLACEMENT_REQUIRES_NEW_CERTIFICATE"
                        : "ADMIT_DISTINCT_RESEARCH_CANDIDATE");
    }

    /** One supported champion per family; ties favor calibration/stability/resource quality. */
    public static Map<String, Signature> familyChampions(List<Signature> signatures) {
        if (signatures == null) return Collections.emptyMap();
        LinkedHashMap<String, Signature> champions = new LinkedHashMap<String, Signature>();
        for (Signature candidate : signatures) {
            if (candidate == null || candidate.futureN < 120) continue;
            Signature current = champions.get(candidate.familyId);
            if (current == null || compareQuality(candidate, current) < 0)
                champions.put(candidate.familyId, candidate);
        }
        return Collections.unmodifiableMap(champions);
    }

    private static int compareQuality(Signature left, Signature right) {
        int byHit = Double.compare(right.hitRate, left.hitRate);
        if (byHit != 0) return byHit;
        int byCoverage = Double.compare(right.coverage, left.coverage);
        if (byCoverage != 0) return byCoverage;
        int byEce = Double.compare(left.ece, right.ece);
        if (byEce != 0) return byEce;
        int byStability = Double.compare(right.stability, left.stability);
        if (byStability != 0) return byStability;
        int byCost = Long.compare(left.p95CostNs, right.p95CostNs);
        if (byCost != 0) return byCost;
        return left.signatureHash.compareTo(right.signatureHash);
    }

    private static boolean dominates(Signature left, Signature right) {
        if (left.futureN < right.futureN) return false;
        if (!metric(left.hitRate) || !metric(right.hitRate)
                || !metric(left.coverage) || !metric(right.coverage)
                || !metric(left.ece) || !metric(right.ece)
                || !metric(left.stability) || !metric(right.stability)) return false;
        boolean noWorse = left.hitRate >= right.hitRate && left.coverage >= right.coverage
                && left.ece <= right.ece && left.stability >= right.stability
                && left.p95CostNs <= right.p95CostNs;
        boolean strict = left.hitRate > right.hitRate || left.coverage > right.coverage
                || left.ece < right.ece || left.stability > right.stability
                || left.p95CostNs < right.p95CostNs;
        return noWorse && strict;
    }

    public static final class ContributionNode {
        public final String nodeId;
        public final NodeType type;
        public final String artifactHash;
        public final NodeState state;
        public final String namespace;
        public final String nodeHash;

        public ContributionNode(String nodeId, NodeType type, String artifactHash,
                NodeState state, String namespace) {
            this.nodeId = required(nodeId, "CONTRIBUTION_NODE_ID");
            if (type == null || state == null)
                throw new IllegalArgumentException("R930_CONTRIBUTION_NODE_INVALID");
            this.type = type;
            this.artifactHash = requireHash(artifactHash, "CONTRIBUTION_ARTIFACT_HASH");
            this.state = state;
            this.namespace = required(namespace, "CONTRIBUTION_NAMESPACE");
            this.nodeHash = sha256(this.nodeId + '|' + type.name() + '|'
                    + this.artifactHash + '|' + state.name() + '|' + this.namespace);
        }

        ContributionNode withState(NodeState next) {
            return new ContributionNode(nodeId, type, artifactHash, next, namespace);
        }
    }

    public static final class ContributionEdge {
        public final String parentNodeId;
        public final String childNodeId;
        public final String semantic;
        public final double maximumWeight;
        public final String certificateHash;
        public final String edgeHash;

        public ContributionEdge(String parentNodeId, String childNodeId, String semantic,
                double maximumWeight, String certificateHash) {
            this.parentNodeId = required(parentNodeId, "EDGE_PARENT");
            this.childNodeId = required(childNodeId, "EDGE_CHILD");
            this.semantic = required(semantic, "EDGE_SEMANTIC");
            if (!unit(maximumWeight) || maximumWeight > 0.30d)
                throw new IllegalArgumentException("R930_EDGE_WEIGHT_INVALID");
            this.maximumWeight = maximumWeight;
            this.certificateHash = clean(certificateHash).length() == 0 ? ""
                    : requireHash(certificateHash, "EDGE_CERTIFICATE_HASH");
            this.edgeHash = sha256(this.parentNodeId + '|' + this.childNodeId + '|'
                    + this.semantic + '|' + maximumWeight + '|' + this.certificateHash);
        }
    }

    public static final class ContributionGraph {
        public final Map<String, ContributionNode> nodes;
        public final List<ContributionEdge> edges;
        public final String graphHash;

        public ContributionGraph(List<ContributionNode> nodes, List<ContributionEdge> edges) {
            if (nodes == null || nodes.isEmpty() || edges == null)
                throw new IllegalArgumentException("R930_CONTRIBUTION_GRAPH_INPUT");
            LinkedHashMap<String, ContributionNode> index = new LinkedHashMap<String, ContributionNode>();
            ArrayList<ContributionNode> sortedNodes = new ArrayList<ContributionNode>(nodes);
            Collections.sort(sortedNodes, new Comparator<ContributionNode>() {
                @Override public int compare(ContributionNode left, ContributionNode right) {
                    return left.nodeId.compareTo(right.nodeId);
                }
            });
            for (ContributionNode node : sortedNodes)
                if (node == null || index.put(node.nodeId, node) != null)
                    throw new IllegalArgumentException("R930_CONTRIBUTION_NODE_DUPLICATE");
            ArrayList<ContributionEdge> sortedEdges = new ArrayList<ContributionEdge>(edges);
            Collections.sort(sortedEdges, new Comparator<ContributionEdge>() {
                @Override public int compare(ContributionEdge left, ContributionEdge right) {
                    int byParent = left.parentNodeId.compareTo(right.parentNodeId);
                    if (byParent != 0) return byParent;
                    return left.childNodeId.compareTo(right.childNodeId);
                }
            });
            HashSet<String> edgeKeys = new HashSet<String>();
            for (ContributionEdge edge : sortedEdges) {
                if (edge == null || !index.containsKey(edge.parentNodeId)
                        || !index.containsKey(edge.childNodeId)
                        || edge.parentNodeId.equals(edge.childNodeId)
                        || !edgeKeys.add(edge.parentNodeId + "->" + edge.childNodeId))
                    throw new IllegalArgumentException("R930_CONTRIBUTION_EDGE_INVALID");
            }
            requireAcyclic(index, sortedEdges);
            this.nodes = Collections.unmodifiableMap(index);
            this.edges = Collections.unmodifiableList(sortedEdges);
            StringBuilder canonical = new StringBuilder(VERSION);
            for (ContributionNode node : sortedNodes) canonical.append('|').append(node.nodeHash);
            for (ContributionEdge edge : sortedEdges) canonical.append('|').append(edge.edgeHash);
            this.graphHash = sha256(canonical.toString());
        }

        public ContributionNode require(String nodeId) {
            ContributionNode node = nodes.get(nodeId);
            if (node == null) throw new IllegalArgumentException("R930_CONTRIBUTION_NODE_UNKNOWN");
            return node;
        }

        /** Returns only the defective node and its descendants; unrelated nodes remain active. */
        public Set<String> quarantineClosure(String defectiveNodeId) {
            String root = required(defectiveNodeId, "QUARANTINE_NODE");
            if (!nodes.containsKey(root))
                throw new IllegalArgumentException("R930_QUARANTINE_NODE_UNKNOWN");
            LinkedHashSet<String> closure = new LinkedHashSet<String>();
            ArrayList<String> queue = new ArrayList<String>();
            queue.add(root);
            for (int cursor = 0; cursor < queue.size(); cursor++) {
                String current = queue.get(cursor);
                if (!closure.add(current)) continue;
                for (ContributionEdge edge : edges)
                    if (edge.parentNodeId.equals(current)) queue.add(edge.childNodeId);
            }
            return Collections.unmodifiableSet(closure);
        }

        public ContributionGraph withQuarantine(String defectiveNodeId) {
            Set<String> closure = quarantineClosure(defectiveNodeId);
            ArrayList<ContributionNode> next = new ArrayList<ContributionNode>();
            for (ContributionNode node : nodes.values())
                next.add(closure.contains(node.nodeId)
                        ? node.withState(NodeState.QUARANTINED) : node);
            return new ContributionGraph(next, edges);
        }
    }

    private static void requireAcyclic(Map<String, ContributionNode> nodes,
            List<ContributionEdge> edges) {
        HashMap<String, ArrayList<String>> children = new HashMap<String, ArrayList<String>>();
        for (String id : nodes.keySet()) children.put(id, new ArrayList<String>());
        for (ContributionEdge edge : edges) children.get(edge.parentNodeId).add(edge.childNodeId);
        HashMap<String, Integer> color = new HashMap<String, Integer>();
        for (String id : nodes.keySet()) visit(id, children, color);
    }

    private static void visit(String id, Map<String, ArrayList<String>> children,
            Map<String, Integer> color) {
        Integer state = color.get(id);
        if (state != null && state.intValue() == 2) return;
        if (state != null && state.intValue() == 1)
            throw new IllegalArgumentException("R930_CONTRIBUTION_GRAPH_CYCLE:" + id);
        color.put(id, Integer.valueOf(1));
        for (String child : children.get(id)) visit(child, children, color);
        color.put(id, Integer.valueOf(2));
    }

    public static final class ScoreSurface {
        public final String targetIssue;
        public final String inputHash;
        public final String actionSpaceHash;
        private final double[] scores;
        public final String surfaceHash;

        public ScoreSurface(String targetIssue, String inputHash, String actionSpaceHash,
                double[] scores) {
            this.targetIssue = required(targetIssue, "SCORE_TARGET");
            this.inputHash = requireHash(inputHash, "SCORE_INPUT_HASH");
            this.actionSpaceHash = requireHash(actionSpaceHash, "SCORE_ACTION_SPACE_HASH");
            if (scores == null || scores.length != 1200)
                throw new IllegalArgumentException("R930_SCORE_SURFACE_CARDINALITY");
            this.scores = Arrays.copyOf(scores, scores.length);
            StringBuilder canonical = new StringBuilder(this.targetIssue).append('|')
                    .append(this.inputHash).append('|').append(this.actionSpaceHash);
            for (double score : this.scores) {
                if (!finite(score)) throw new IllegalArgumentException("R930_SCORE_NONFINITE");
                canonical.append('|').append(Long.toHexString(Double.doubleToLongBits(score)));
            }
            this.surfaceHash = sha256(canonical.toString());
        }

        public double score(int ordinal) { return scores[ordinal]; }
        public double[] scoresCopy() { return Arrays.copyOf(scores, scores.length); }
        public int rankOneOrdinal() {
            int best = 0;
            for (int i = 1; i < scores.length; i++)
                if (scores[i] > scores[best]) best = i;
            return best;
        }
    }

    public static final class ContributionResult {
        public final ScoreSurface baseline;
        public final ScoreSurface research;
        public final ScoreSurface output;
        public final ResearchPlatformStoreR929.Authorization authorization;
        public final boolean applied;
        public final double weight;
        public final int beforeRankOne;
        public final int afterRankOne;
        public final String reason;
        public final String resultHash;
        public final boolean mayCreateReal;

        ContributionResult(ScoreSurface baseline, ScoreSurface research,
                ScoreSurface output, ResearchPlatformStoreR929.Authorization authorization,
                boolean applied, double weight, int beforeRankOne, int afterRankOne,
                String reason) {
            this.baseline = baseline;
            this.research = research;
            this.output = output;
            this.authorization = authorization;
            this.applied = applied;
            this.weight = weight;
            this.beforeRankOne = beforeRankOne;
            this.afterRankOne = afterRankOne;
            this.reason = reason;
            this.mayCreateReal = false;
            this.resultHash = sha256(baseline.surfaceHash + '|'
                    + (research == null ? "" : research.surfaceHash) + '|'
                    + output.surfaceHash + '|' + authorization.authorizationHash + '|'
                    + (applied ? 1 : 0) + '|' + weight + '|' + beforeRankOne + '|'
                    + afterRankOne + '|' + reason + "|directREAL=0");
        }
    }

    /** Denied/failed research always returns the exact baseline surface. */
    public static ContributionResult authorizeAndBlend(DbStore store,
            CertificateBundleServiceR929.Consumer consumer, String candidateId,
            String structuralHash, String evidenceHash, double requestedWeight,
            ScoreSurface baseline, ScoreSurface research) {
        if (baseline == null)
            throw new IllegalArgumentException("R930_BASELINE_SCORE_REQUIRED");
        if (research == null || consumer == null
                || !baseline.targetIssue.equals(research.targetIssue)
                || !baseline.inputHash.equals(research.inputHash)
                || !baseline.actionSpaceHash.equals(research.actionSpaceHash)) {
            ResearchPlatformStoreR929.Authorization denied =
                    ResearchPlatformStoreR929.Authorization.denied("RESEARCH_SURFACE_INVALID");
            return new ContributionResult(baseline, research, baseline, denied,
                    false, 0.0d, baseline.rankOneOrdinal(), baseline.rankOneOrdinal(),
                    denied.reason);
        }
        ResearchPlatformStoreR929.Authorization authorization =
                ResearchPlatformStoreR929.authorize(store, consumer, candidateId,
                        structuralHash, evidenceHash, baseline.targetIssue, requestedWeight);
        if (!authorization.authorized) {
            return new ContributionResult(baseline, research, baseline, authorization,
                    false, 0.0d, baseline.rankOneOrdinal(), baseline.rankOneOrdinal(),
                    authorization.reason);
        }
        double weight = authorization.grantedWeight;
        double[] output = new double[1200];
        for (int i = 0; i < output.length; i++)
            output[i] = (1.0d - weight) * baseline.score(i) + weight * research.score(i);
        ScoreSurface blended = new ScoreSurface(baseline.targetIssue,
                baseline.inputHash, baseline.actionSpaceHash, output);
        return new ContributionResult(baseline, research, blended, authorization,
                true, weight, baseline.rankOneOrdinal(), blended.rankOneOrdinal(),
                "CERTIFICATE_BOUNDED_RANKING_INFLUENCE_ONLY");
    }

    private static double cosine(double[] left, double[] right) {
        double dot = 0.0d, leftNorm = 0.0d, rightNorm = 0.0d;
        for (int i = 0; i < left.length; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }
        if (leftNorm < 1.0e-18d || rightNorm < 1.0e-18d)
            return leftNorm < 1.0e-18d && rightNorm < 1.0e-18d ? 1.0d : 0.0d;
        return Math.max(-1.0d, Math.min(1.0d, dot / Math.sqrt(leftNorm * rightNorm)));
    }

    private static boolean metricOrUnknown(double value) { return metric(value) || value == -1.0d; }
    private static boolean metric(double value) { return finite(value) && value >= 0.0d && value <= 1.0d; }
    private static boolean unit(double value) { return metric(value); }
    private static boolean finite(double value) { return !Double.isNaN(value) && !Double.isInfinite(value); }
    private static String clean(String value) { return value == null ? "" : value.trim(); }
    private static String required(String value, String code) {
        String clean = clean(value);
        if (clean.length() == 0) throw new IllegalArgumentException("R930_" + code + "_REQUIRED");
        return clean;
    }
    private static String requireHash(String value, String code) {
        String clean = required(value, code).toLowerCase(java.util.Locale.US);
        if (clean.length() != 64) throw new IllegalArgumentException("R930_" + code + "_NOT_SHA256");
        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')))
                throw new IllegalArgumentException("R930_" + code + "_NOT_SHA256");
        }
        return clean;
    }
    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder output = new StringBuilder(64);
            for (byte item : bytes)
                output.append(String.format(java.util.Locale.US, "%02x", item & 255));
            return output.toString();
        } catch (Exception failure) {
            throw new IllegalStateException("R930_SHA256_UNAVAILABLE", failure);
        }
    }
}
