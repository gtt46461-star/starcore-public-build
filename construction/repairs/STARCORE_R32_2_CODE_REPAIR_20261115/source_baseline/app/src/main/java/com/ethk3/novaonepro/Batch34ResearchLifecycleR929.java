package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * C04/C05/C07-C12 research lifecycle, capture, atom and invention contracts.
 * Production persistence remains owned by the existing DbStore/event-ledger owners.
 */
public final class Batch34ResearchLifecycleR929 {
    public static final String VERSION = "R929_RESEARCH_LIFECYCLE_V3";
    public static final int TIER_A_CAPACITY = 8;
    public static final int TIER_B_CAPACITY = 24;
    public static final int TIER_C_CAPACITY = 64;
    public static final long PER_SLICE_BUDGET_NS = 250_000_000L;
    public static final long INITIAL_TOTAL_BUDGET_NS = 700_000_000L;

    private Batch34ResearchLifecycleR929() {}

    public enum CaptureState {
        SYSTEM_AVAILABLE,
        ELIGIBLE,
        SELECTED,
        EXECUTION_STARTED,
        FROZEN,
        FAILED
    }

    /** One opportunity progresses monotonically. CAS sequence prevents double-freeze. */
    public static final class CaptureRecord {
        public final String issue;
        public final String opportunitySha256;
        public final long createdAtMillis;
        private CaptureState state;
        private long sequence;
        private String frozenPayloadSha256;
        private String failureCode;

        public CaptureRecord(
                String issue, String opportunitySha256, long createdAtMillis, long initialSequence) {
            this.issue = safe(issue);
            this.opportunitySha256 = safe(opportunitySha256);
            this.createdAtMillis = createdAtMillis;
            this.sequence = initialSequence;
            this.state = CaptureState.SYSTEM_AVAILABLE;
            this.frozenPayloadSha256 = "";
            this.failureCode = "";
        }

        public synchronized String transition(
                CaptureState expected, CaptureState next, long expectedSequence) {
            if (expected == null || next == null) return "C04_CAPTURE_STATE_NULL";
            if (sequence != expectedSequence) return "C04_CAPTURE_CAS_CONFLICT";
            if (state != expected) return "C04_CAPTURE_EXPECTED_STATE_MISMATCH";
            if (!allowedCaptureTransition(state, next)) return "C04_CAPTURE_TRANSITION_FORBIDDEN";
            state = next;
            sequence++;
            return "C04_CAPTURE_TRANSITION_COMMITTED";
        }

        public synchronized String freeze(
                long expectedSequence, String payloadSha256, boolean eventLedgerCommitted) {
            if (sequence != expectedSequence) return "C04_FREEZE_CAS_CONFLICT";
            if (state != CaptureState.EXECUTION_STARTED) return "C04_FREEZE_STATE_INVALID";
            if (!Batch34EvidenceContractR929.isSha256(payloadSha256)) {
                return "C04_FREEZE_PAYLOAD_HASH_INVALID";
            }
            if (!eventLedgerCommitted) return "C04_FREEZE_EVENT_NOT_COMMITTED";
            frozenPayloadSha256 = payloadSha256;
            state = CaptureState.FROZEN;
            sequence++;
            return "C04_FREEZE_COMMITTED";
        }

        public synchronized String fail(
                long expectedSequence, String code, boolean eventLedgerCommitted) {
            if (sequence != expectedSequence) return "C04_FAILURE_CAS_CONFLICT";
            if (state == CaptureState.FROZEN || state == CaptureState.FAILED) {
                return "C04_FAILURE_TERMINAL_STATE";
            }
            if (!eventLedgerCommitted) return "C04_FAILURE_EVENT_NOT_COMMITTED";
            failureCode = safe(code).length() == 0 ? "UNKNOWN_CAPTURE_FAILURE" : safe(code);
            state = CaptureState.FAILED;
            sequence++;
            return "C04_FAILURE_COMMITTED";
        }

        public synchronized CaptureState state() { return state; }
        public synchronized long sequence() { return sequence; }
        public synchronized String frozenPayloadSha256() { return frozenPayloadSha256; }
        public synchronized String failureCode() { return failureCode; }

        public String identitySha256() {
            return Batch34EvidenceContractR929.sha256(
                    "C04_CAPTURE|" + issue + "|" + opportunitySha256 + "|" + createdAtMillis);
        }
    }

    public static final class CaptureLedger {
        private final Map<String, CaptureRecord> records =
                new LinkedHashMap<String, CaptureRecord>();

        public synchronized String create(CaptureRecord record) {
            if (record == null) return "C04_CAPTURE_NULL";
            if (!Batch34EvidenceContractR929.validIssue(record.issue)) return "C04_CAPTURE_ISSUE_INVALID";
            if (!Batch34EvidenceContractR929.isSha256(record.opportunitySha256)) {
                return "C04_CAPTURE_OPPORTUNITY_HASH_INVALID";
            }
            if (records.containsKey(record.issue)) return "C04_CAPTURE_DUPLICATE_ISSUE";
            records.put(record.issue, record);
            return "C04_CAPTURE_CREATED";
        }

        public synchronized CaptureRecord get(String issue) { return records.get(safe(issue)); }

        public synchronized CaptureSummary summary() {
            int available = records.size();
            int eligible = 0;
            int selected = 0;
            int started = 0;
            int frozen = 0;
            int failed = 0;
            for (CaptureRecord record : records.values()) {
                CaptureState state = record.state();
                if (state == CaptureState.ELIGIBLE
                        || state == CaptureState.SELECTED
                        || state == CaptureState.EXECUTION_STARTED
                        || state == CaptureState.FROZEN) eligible++;
                if (state == CaptureState.SELECTED
                        || state == CaptureState.EXECUTION_STARTED
                        || state == CaptureState.FROZEN) selected++;
                if (state == CaptureState.EXECUTION_STARTED || state == CaptureState.FROZEN) started++;
                if (state == CaptureState.FROZEN) frozen++;
                if (state == CaptureState.FAILED) failed++;
            }
            return new CaptureSummary(available, eligible, selected, started, frozen, failed);
        }
    }

    public static final class CaptureSummary {
        public final int systemAvailable;
        public final int eligible;
        public final int selected;
        public final int executionStarted;
        public final int frozen;
        public final int failed;

        CaptureSummary(
                int systemAvailable,
                int eligible,
                int selected,
                int executionStarted,
                int frozen,
                int failed) {
            this.systemAvailable = systemAvailable;
            this.eligible = eligible;
            this.selected = selected;
            this.executionStarted = executionStarted;
            this.frozen = frozen;
            this.failed = failed;
        }

        public boolean monotonic() {
            return systemAvailable >= eligible
                    && eligible >= selected
                    && selected >= executionStarted
                    && executionStarted >= frozen
                    && failed >= 0
                    && failed <= systemAvailable;
        }

        public String canonical() {
            return systemAvailable + "|" + eligible + "|" + selected + "|" + executionStarted
                    + "|" + frozen + "|" + failed;
        }
    }

    public enum FamilyRole { CHAMPION, CHALLENGER, OBSERVING, QUARANTINED }

    /** Structural, behavioral and residual identity are all part of one family key. */
    public static final class FamilyMember {
        public final String algorithmId;
        public final String structuralSha256;
        public final String probeBehaviorSha256;
        public final String residualSignatureSha256;
        public final String statisticalFamily;
        public final String artifactSha256;
        public final FamilyRole role;
        public final int futureSupport;

        public FamilyMember(
                String algorithmId,
                String structuralSha256,
                String probeBehaviorSha256,
                String residualSignatureSha256,
                String statisticalFamily,
                String artifactSha256,
                FamilyRole role,
                int futureSupport) {
            this.algorithmId = safe(algorithmId);
            this.structuralSha256 = safe(structuralSha256);
            this.probeBehaviorSha256 = safe(probeBehaviorSha256);
            this.residualSignatureSha256 = safe(residualSignatureSha256);
            this.statisticalFamily = safe(statisticalFamily);
            this.artifactSha256 = safe(artifactSha256);
            this.role = role == null ? FamilyRole.OBSERVING : role;
            this.futureSupport = futureSupport;
        }

        public String canonicalFamilyKey() {
            return Batch34EvidenceContractR929.sha256(
                    "C05_FAMILY|" + structuralSha256 + "|" + probeBehaviorSha256 + "|"
                            + residualSignatureSha256 + "|" + statisticalFamily);
        }
    }

    public static final class FamilyRegistry {
        private final Map<String, List<FamilyMember>> families =
                new LinkedHashMap<String, List<FamilyMember>>();

        public synchronized String register(FamilyMember member) {
            if (member == null || member.algorithmId.length() == 0) return "C05_MEMBER_INVALID";
            if (!Batch34EvidenceContractR929.isSha256(member.structuralSha256)
                    || !Batch34EvidenceContractR929.isSha256(member.probeBehaviorSha256)
                    || !Batch34EvidenceContractR929.isSha256(member.residualSignatureSha256)
                    || !Batch34EvidenceContractR929.isSha256(member.artifactSha256)) {
                return "C05_MEMBER_HASH_INVALID";
            }
            if (member.statisticalFamily.length() == 0) return "C05_STATISTICAL_FAMILY_EMPTY";
            String key = member.canonicalFamilyKey();
            List<FamilyMember> group = families.get(key);
            if (group == null) {
                group = new ArrayList<FamilyMember>();
                families.put(key, group);
            }
            for (FamilyMember old : group) {
                if (old.algorithmId.equals(member.algorithmId)) return "C05_MEMBER_DUPLICATE";
            }
            int champions = member.role == FamilyRole.CHAMPION ? 1 : 0;
            int challengers = member.role == FamilyRole.CHALLENGER ? 1 : 0;
            for (FamilyMember old : group) {
                if (old.role == FamilyRole.CHAMPION) champions++;
                if (old.role == FamilyRole.CHALLENGER) challengers++;
            }
            if (champions > 1) return "C05_ONE_CHAMPION_ONLY";
            if (challengers > 3) return "C05_MAX_THREE_CHALLENGERS";
            group.add(member);
            return "C05_MEMBER_REGISTERED";
        }

        public synchronized int familyCount() { return families.size(); }

        public synchronized List<FamilyMember> family(String key) {
            List<FamilyMember> values = families.get(safe(key));
            if (values == null) return Collections.emptyList();
            return Collections.unmodifiableList(new ArrayList<FamilyMember>(values));
        }
    }

    public enum Tier { A_FULL_1200, B_SHARED_FULL_SCORE, C_PROXY_NURSERY }

    public static final class ScheduledResearch {
        public final String algorithmId;
        public final Tier tier;
        public final long budgetNs;
        public final int rotationSlot;
        public final boolean fullActionSurface;

        ScheduledResearch(
                String algorithmId,
                Tier tier,
                long budgetNs,
                int rotationSlot,
                boolean fullActionSurface) {
            this.algorithmId = algorithmId;
            this.tier = tier;
            this.budgetNs = budgetNs;
            this.rotationSlot = rotationSlot;
            this.fullActionSurface = fullActionSurface;
        }

        public String canonical() {
            return algorithmId + "|" + tier + "|" + budgetNs + "|" + rotationSlot + "|"
                    + fullActionSurface;
        }
    }

    /** Deterministic 8/24/64 scheduler; Tier C rotates fairly and carries no frequency debt. */
    public static List<ScheduledResearch> schedule(
            List<String> tierA,
            List<String> tierB,
            List<String> tierC,
            long issueOrdinal,
            long availableBudgetNs) {
        ArrayList<ScheduledResearch> out = new ArrayList<ScheduledResearch>();
        appendTier(out, tierA, TIER_A_CAPACITY, Tier.A_FULL_1200, true);
        appendTier(out, tierB, TIER_B_CAPACITY, Tier.B_SHARED_FULL_SCORE, true);
        List<String> c = cleanUnique(tierC);
        int cTake = Math.min(TIER_C_CAPACITY, c.size());
        int start = c.isEmpty() ? 0 : (int) Math.floorMod(issueOrdinal, c.size());
        for (int i = 0; i < cTake; i++) {
            String id = c.get((start + i) % c.size());
            out.add(new ScheduledResearch(id, Tier.C_PROXY_NURSERY, PER_SLICE_BUDGET_NS, i, false));
        }
        long budget = Math.min(Math.max(0L, availableBudgetNs), INITIAL_TOTAL_BUDGET_NS);
        ArrayList<ScheduledResearch> admitted = new ArrayList<ScheduledResearch>();
        long used = 0L;
        for (ScheduledResearch item : out) {
            long slice = Math.min(item.budgetNs, PER_SLICE_BUDGET_NS);
            if (used + slice > budget) break;
            admitted.add(new ScheduledResearch(
                    item.algorithmId, item.tier, slice, item.rotationSlot, item.fullActionSurface));
            used += slice;
        }
        return Collections.unmodifiableList(admitted);
    }

    private static void appendTier(
            List<ScheduledResearch> out,
            List<String> raw,
            int capacity,
            Tier tier,
            boolean full) {
        List<String> clean = cleanUnique(raw);
        int n = Math.min(capacity, clean.size());
        for (int i = 0; i < n; i++) {
            out.add(new ScheduledResearch(clean.get(i), tier, PER_SLICE_BUDGET_NS, i, full));
        }
    }

    public enum LifecycleState {
        IDEA,
        NURSERY,
        OBSERVING,
        EVIDENCE_READY,
        CHALLENGER,
        SHADOW,
        INFLUENCE,
        CHAMPION,
        PAUSED,
        QUARANTINED,
        PRUNED
    }

    public static final class LifecycleRecord {
        public final String algorithmId;
        public final String parentAlgorithmId;
        public final String genomeSha256;
        public final String repairTarget;
        public final String falsificationRule;
        private LifecycleState state;
        private int futureSupport;
        private String lastEvidenceSha256;

        public LifecycleRecord(
                String algorithmId,
                String parentAlgorithmId,
                String genomeSha256,
                String repairTarget,
                String falsificationRule) {
            this.algorithmId = safe(algorithmId);
            this.parentAlgorithmId = safe(parentAlgorithmId);
            this.genomeSha256 = safe(genomeSha256);
            this.repairTarget = safe(repairTarget);
            this.falsificationRule = safe(falsificationRule);
            this.state = LifecycleState.IDEA;
            this.lastEvidenceSha256 = "";
        }

        public synchronized String transition(
                LifecycleState expected,
                LifecycleState next,
                int addedFutureSupport,
                String evidenceSha256,
                boolean independentPairedImprovement) {
            if (expected == null || next == null) return "C08_STATE_NULL";
            if (state != expected) return "C08_EXPECTED_STATE_MISMATCH";
            if (!allowedLifecycleTransition(state, next)) return "C08_TRANSITION_FORBIDDEN";
            if (addedFutureSupport < 0) return "C08_NEGATIVE_SUPPORT";
            if (next == LifecycleState.EVIDENCE_READY
                    || next == LifecycleState.CHALLENGER
                    || next == LifecycleState.SHADOW
                    || next == LifecycleState.INFLUENCE
                    || next == LifecycleState.CHAMPION) {
                if (!Batch34EvidenceContractR929.isSha256(evidenceSha256)) {
                    return "C08_EVIDENCE_HASH_REQUIRED";
                }
            }
            if ((next == LifecycleState.INFLUENCE || next == LifecycleState.CHAMPION)
                    && !independentPairedImprovement) {
                return "C08_PAIRED_IMPROVEMENT_NOT_PROVEN";
            }
            futureSupport += addedFutureSupport;
            lastEvidenceSha256 = safe(evidenceSha256);
            state = next;
            return "C08_TRANSITION_COMMITTED";
        }

        public synchronized LifecycleState state() { return state; }
        public synchronized int futureSupport() { return futureSupport; }
        public synchronized String lastEvidenceSha256() { return lastEvidenceSha256; }

        /** Children always begin with a new identity and N=0. */
        public LifecycleRecord repairedChild(
                String newAlgorithmId, String newGenomeSha256, String target, String falsification) {
            if (safe(newAlgorithmId).length() == 0 || newAlgorithmId.equals(algorithmId)) {
                throw new IllegalArgumentException("C08_CHILD_ID_MUST_BE_NEW");
            }
            return new LifecycleRecord(newAlgorithmId, algorithmId, newGenomeSha256, target, falsification);
        }
    }

    public static final class FailureCase {
        public final String issue;
        public final String selectedActionSha256;
        public final String frozenInputSha256;
        public final String actualOutcomeSha256;
        public final String failureClass;
        public final String caseSha256;

        public FailureCase(
                String issue,
                String selectedActionSha256,
                String frozenInputSha256,
                String actualOutcomeSha256,
                String failureClass,
                String caseSha256) {
            this.issue = safe(issue);
            this.selectedActionSha256 = safe(selectedActionSha256);
            this.frozenInputSha256 = safe(frozenInputSha256);
            this.actualOutcomeSha256 = safe(actualOutcomeSha256);
            this.failureClass = safe(failureClass);
            this.caseSha256 = safe(caseSha256);
        }

        public String canonicalWithoutHash() {
            return issue + "|" + selectedActionSha256 + "|" + frozenInputSha256 + "|"
                    + actualOutcomeSha256 + "|" + failureClass;
        }

        public String calculatedSha256() {
            return Batch34EvidenceContractR929.sha256(
                    "C09_FAILURE_CASE|" + canonicalWithoutHash());
        }
    }

    public static final class AblationResult {
        public final String failureCaseSha256;
        public final String disabledModule;
        public final String replaySha256;
        public final boolean canonicalWritesSuppressed;
        public final boolean selectionChanged;
        public final double probabilityDelta;
        public final int evidenceCases;

        public AblationResult(
                String failureCaseSha256,
                String disabledModule,
                String replaySha256,
                boolean canonicalWritesSuppressed,
                boolean selectionChanged,
                double probabilityDelta,
                int evidenceCases) {
            this.failureCaseSha256 = safe(failureCaseSha256);
            this.disabledModule = safe(disabledModule).toUpperCase(Locale.US);
            this.replaySha256 = safe(replaySha256);
            this.canonicalWritesSuppressed = canonicalWritesSuppressed;
            this.selectionChanged = selectionChanged;
            this.probabilityDelta = probabilityDelta;
            this.evidenceCases = evidenceCases;
        }
    }

    public static Batch34EvidenceContractR929.Check validateAblation(
            FailureCase failure, List<AblationResult> results) {
        ArrayList<String> f = new ArrayList<String>();
        if (failure == null) {
            f.add("C09_FAILURE_CASE_NULL");
            return check(f, "");
        }
        if (!Batch34EvidenceContractR929.validIssue(failure.issue)) f.add("C09_ISSUE_INVALID");
        if (!Batch34EvidenceContractR929.isSha256(failure.selectedActionSha256)
                || !Batch34EvidenceContractR929.isSha256(failure.frozenInputSha256)
                || !Batch34EvidenceContractR929.isSha256(failure.actualOutcomeSha256)
                || !Batch34EvidenceContractR929.isSha256(failure.caseSha256)) {
            f.add("C09_FAILURE_HASH_INVALID");
        }
        if (!failure.calculatedSha256().equalsIgnoreCase(failure.caseSha256)) {
            f.add("C09_FAILURE_CASE_HASH_MISMATCH");
        }
        String[] required = {"FTRL", "TINY_DEEP", "TEACHER", "TREND", "SHAPE", "RELATION", "INVENTED"};
        Set<String> seen = new HashSet<String>();
        if (results != null) {
            for (AblationResult result : results) {
                if (result == null) {
                    f.add("C09_NULL_ABLATION_RESULT");
                    continue;
                }
                if (!failure.caseSha256.equals(result.failureCaseSha256)) {
                    f.add("C09_ABLATION_CASE_MISMATCH");
                }
                if (!seen.add(result.disabledModule)) f.add("C09_DUPLICATE_MODULE_ABLATION");
                if (!Batch34EvidenceContractR929.isSha256(result.replaySha256)) {
                    f.add("C09_REPLAY_HASH_INVALID:" + result.disabledModule);
                }
                if (!result.canonicalWritesSuppressed) {
                    f.add("C09_CANONICAL_WRITES_NOT_SUPPRESSED:" + result.disabledModule);
                }
                if (!Double.isFinite(result.probabilityDelta)) {
                    f.add("C09_PROBABILITY_DELTA_NONFINITE:" + result.disabledModule);
                }
                if (result.evidenceCases < 1) {
                    f.add("C09_WAIT_FOR_FAILURE_EVIDENCE:" + result.disabledModule);
                }
            }
        }
        for (String module : required) {
            if (!seen.contains(module)) f.add("C09_REQUIRED_ABLATION_MISSING:" + module);
        }
        return check(f, failure.canonicalWithoutHash());
    }

    public enum MissingReason {
        NONE,
        SOURCE_ABSENT,
        PIT_BLOCKED,
        TIME_BUDGET,
        INVALID_RANGE,
        NONFINITE,
        DEPENDENCY_MISSING,
        QUARANTINED
    }

    /** C10 value contract; absent is never silently represented as numeric zero. */
    public static final class ValueResult<T> {
        public final T value;
        public final boolean present;
        public final MissingReason missingReason;
        public final String maxSourceIssue;
        public final long costNs;
        public final String valueSha256;

        private ValueResult(
                T value,
                boolean present,
                MissingReason missingReason,
                String maxSourceIssue,
                long costNs,
                String valueSha256) {
            this.value = value;
            this.present = present;
            this.missingReason = missingReason == null ? MissingReason.SOURCE_ABSENT : missingReason;
            this.maxSourceIssue = safe(maxSourceIssue);
            this.costNs = costNs;
            this.valueSha256 = safe(valueSha256);
        }

        public static <T> ValueResult<T> present(
                T value, String maxSourceIssue, long costNs, String canonicalValue) {
            if (value == null) throw new IllegalArgumentException("C10_PRESENT_VALUE_NULL");
            return new ValueResult<T>(
                    value,
                    true,
                    MissingReason.NONE,
                    maxSourceIssue,
                    Math.max(0L, costNs),
                    Batch34EvidenceContractR929.sha256("C10_VALUE|" + safe(canonicalValue)));
        }

        public static <T> ValueResult<T> missing(
                MissingReason reason, String maxSourceIssue, long costNs) {
            if (reason == null || reason == MissingReason.NONE) {
                throw new IllegalArgumentException("C10_MISSING_REASON_REQUIRED");
            }
            return new ValueResult<T>(null, false, reason, maxSourceIssue, Math.max(0L, costNs), "");
        }

        public boolean pitBefore(String targetIssue) {
            return !present
                    || (Batch34EvidenceContractR929.validIssue(maxSourceIssue)
                            && Batch34EvidenceContractR929.validIssue(targetIssue)
                            && Batch34EvidenceContractR929.compareIssue(maxSourceIssue, targetIssue) < 0);
        }
    }

    public enum AtomCategory { PRE_DRAW, PROXY, POST_DRAW, AUDIT }
    public enum AtomState { ACTIVE, WATCH, SHADOW_ONLY, QUARANTINED }
    public enum EdgeType {
        INPUT_TO_EXPRESSION,
        EXPRESSION_TO_METHOD,
        METHOD_TO_STATE,
        STATE_TO_SCORE,
        SCORE_TO_DIAGNOSTIC
    }

    public static final class AtomDescriptor {
        public final String atomId;
        public final AtomCategory category;
        public final AtomState state;
        public final String semanticType;
        public final String implementationSha256;
        public final String contractSha256;
        public final boolean legacyFixed;
        public final int observed;
        public final int missing;
        public final int pitViolations;
        public final long p95CostNs;
        public final double stability;
        public final double incrementalGain;

        public AtomDescriptor(
                String atomId,
                AtomCategory category,
                AtomState state,
                String semanticType,
                String implementationSha256,
                String contractSha256,
                boolean legacyFixed,
                int observed,
                int missing,
                int pitViolations,
                long p95CostNs,
                double stability,
                double incrementalGain) {
            this.atomId = safe(atomId);
            this.category = category;
            this.state = state;
            this.semanticType = safe(semanticType);
            this.implementationSha256 = safe(implementationSha256);
            this.contractSha256 = safe(contractSha256);
            this.legacyFixed = legacyFixed;
            this.observed = observed;
            this.missing = missing;
            this.pitViolations = pitViolations;
            this.p95CostNs = p95CostNs;
            this.stability = stability;
            this.incrementalGain = incrementalGain;
        }

        public String canonical() {
            return atomId + "|" + category + "|" + state + "|" + semanticType + "|"
                    + implementationSha256 + "|" + contractSha256 + "|" + legacyFixed + "|"
                    + observed + "|" + missing + "|" + pitViolations + "|" + p95CostNs + "|"
                    + finite(stability) + "|" + finite(incrementalGain);
        }
    }

    public static Batch34EvidenceContractR929.Check validateAtomRegistry(
            List<AtomDescriptor> atoms, Map<String, String> previouslyCertifiedHashes) {
        ArrayList<String> f = new ArrayList<String>();
        int pre = 0, proxy = 0, post = 0, audit = 0;
        Set<String> ids = new HashSet<String>();
        if (atoms != null) {
            for (AtomDescriptor atom : atoms) {
                if (atom == null) {
                    f.add("C10_NULL_ATOM");
                    continue;
                }
                if (atom.atomId.length() == 0 || !ids.add(atom.atomId)) {
                    f.add("C10_DUPLICATE_OR_EMPTY_ATOM");
                }
                if (atom.category == null || atom.state == null) f.add("C10_ATOM_ENUM_MISSING");
                else if (atom.category == AtomCategory.PRE_DRAW) pre++;
                else if (atom.category == AtomCategory.PROXY) proxy++;
                else if (atom.category == AtomCategory.POST_DRAW) post++;
                else if (atom.category == AtomCategory.AUDIT) audit++;
                if (!Batch34EvidenceContractR929.isSha256(atom.implementationSha256)
                        || !Batch34EvidenceContractR929.isSha256(atom.contractSha256)) {
                    f.add("C10_ATOM_HASH_INVALID:" + atom.atomId);
                }
                if (atom.observed < 0 || atom.missing < 0 || atom.missing > atom.observed) {
                    f.add("C10_ATOM_COUNT_INVALID:" + atom.atomId);
                }
                if (atom.pitViolations != 0) f.add("C10_ATOM_PIT_VIOLATION:" + atom.atomId);
                if (atom.p95CostNs < 0) f.add("C10_ATOM_COST_INVALID:" + atom.atomId);
                if (!Double.isFinite(atom.stability) || !Double.isFinite(atom.incrementalGain)) {
                    f.add("C10_ATOM_QUALITY_NONFINITE:" + atom.atomId);
                }
                String old = previouslyCertifiedHashes == null
                        ? null : previouslyCertifiedHashes.get(atom.atomId);
                if (old != null && !old.equalsIgnoreCase(atom.implementationSha256)
                        && atom.state != AtomState.QUARANTINED) {
                    f.add("C11_CHANGED_ATOM_CERTIFICATE_NOT_INVALIDATED:" + atom.atomId);
                }
            }
        }
        if (pre != 165) f.add("C10_PRE_DRAW_ATOM_COUNT_NOT_165");
        if (proxy != 40) f.add("C10_PROXY_ATOM_COUNT_NOT_40");
        if (post != 20) f.add("C10_POST_DRAW_ATOM_COUNT_NOT_20");
        if (audit != 15) f.add("C10_AUDIT_ATOM_COUNT_NOT_15");
        if (ids.size() != 240) f.add("C10_TOTAL_ATOM_COUNT_NOT_240");
        return check(f, "pre=" + pre + "|proxy=" + proxy + "|post=" + post + "|audit=" + audit);
    }

    public enum NoveltyClass { PARAMETER_VARIANT, STRUCTURAL_VARIANT, NEW_STATISTICAL_FAMILY }

    public static final class MethodGraphEdge {
        public final String from;
        public final String to;
        public final EdgeType type;

        public MethodGraphEdge(String from, String to, EdgeType type) {
            this.from = safe(from);
            this.to = safe(to);
            this.type = type;
        }

        public String canonical() { return from + "->" + to + ":" + type; }
    }

    /** Executable invention genome with dependencies, examples and failure conditions. */
    public static final class Genome {
        public final String genomeId;
        public final String statisticalFamily;
        public final NoveltyClass noveltyClass;
        public final List<String> atomDependencies;
        public final String expressionDsl;
        public final List<MethodGraphEdge> graph;
        public final String stateUpdateDsl;
        public final List<String> examples;
        public final List<String> failureConditions;
        public final String typeContract;
        public final String rangeContract;
        public final long worstCaseCostNs;
        public final boolean pitSafe;
        public final boolean deterministic;
        public final boolean hardConstraintsPassed;
        public final String genomeSha256;

        public Genome(
                String genomeId,
                String statisticalFamily,
                NoveltyClass noveltyClass,
                List<String> atomDependencies,
                String expressionDsl,
                List<MethodGraphEdge> graph,
                String stateUpdateDsl,
                List<String> examples,
                List<String> failureConditions,
                String typeContract,
                String rangeContract,
                long worstCaseCostNs,
                boolean pitSafe,
                boolean deterministic,
                boolean hardConstraintsPassed,
                String genomeSha256) {
            this.genomeId = safe(genomeId);
            this.statisticalFamily = safe(statisticalFamily);
            this.noveltyClass = noveltyClass;
            this.atomDependencies = immutableStrings(atomDependencies);
            this.expressionDsl = safe(expressionDsl);
            ArrayList<MethodGraphEdge> edges = new ArrayList<MethodGraphEdge>();
            if (graph != null) edges.addAll(graph);
            this.graph = Collections.unmodifiableList(edges);
            this.stateUpdateDsl = safe(stateUpdateDsl);
            this.examples = immutableStrings(examples);
            this.failureConditions = immutableStrings(failureConditions);
            this.typeContract = safe(typeContract);
            this.rangeContract = safe(rangeContract);
            this.worstCaseCostNs = worstCaseCostNs;
            this.pitSafe = pitSafe;
            this.deterministic = deterministic;
            this.hardConstraintsPassed = hardConstraintsPassed;
            this.genomeSha256 = safe(genomeSha256);
        }

        public String canonicalWithoutHash() {
            StringBuilder out = new StringBuilder();
            out.append(genomeId).append('|').append(statisticalFamily).append('|')
                    .append(noveltyClass).append('|')
                    .append(Batch34EvidenceContractR929.join(atomDependencies, ",")).append('|')
                    .append(expressionDsl).append('|').append(stateUpdateDsl).append('|')
                    .append(Batch34EvidenceContractR929.join(examples, ",")).append('|')
                    .append(Batch34EvidenceContractR929.join(failureConditions, ",")).append('|')
                    .append(typeContract).append('|').append(rangeContract).append('|')
                    .append(worstCaseCostNs).append('|').append(pitSafe).append('|')
                    .append(deterministic).append('|').append(hardConstraintsPassed);
            for (MethodGraphEdge edge : graph) {
                out.append("\n").append(edge == null ? "NULL" : edge.canonical());
            }
            return out.toString();
        }

        public String calculatedSha256() {
            return Batch34EvidenceContractR929.sha256("C12_GENOME|" + canonicalWithoutHash());
        }
    }

    public static Batch34EvidenceContractR929.Check validateGenome(
            Genome genome, Set<String> activeAtomIds) {
        ArrayList<String> f = new ArrayList<String>();
        if (genome == null) {
            f.add("C12_GENOME_NULL");
            return check(f, "");
        }
        if (genome.genomeId.length() == 0) f.add("C12_GENOME_ID_EMPTY");
        if (genome.statisticalFamily.length() == 0) f.add("C12_STATISTICAL_FAMILY_EMPTY");
        if (genome.noveltyClass == null) f.add("C12_NOVELTY_CLASS_MISSING");
        if (genome.atomDependencies.isEmpty()) f.add("C12_ATOM_DEPENDENCIES_EMPTY");
        for (String atom : genome.atomDependencies) {
            if (activeAtomIds == null || !activeAtomIds.contains(atom)) {
                f.add("C12_ATOM_DEPENDENCY_UNAVAILABLE:" + atom);
            }
        }
        if (genome.expressionDsl.length() == 0) f.add("C12_EXPRESSION_DSL_EMPTY");
        if (genome.graph.isEmpty()) f.add("C12_METHOD_GRAPH_EMPTY");
        if (genome.stateUpdateDsl.length() == 0) f.add("C12_STATE_UPDATE_DSL_EMPTY");
        if (genome.examples.isEmpty()) f.add("C12_EXAMPLES_EMPTY");
        if (genome.failureConditions.isEmpty()) f.add("C12_FAILURE_CONDITIONS_EMPTY");
        if (genome.typeContract.length() == 0) f.add("C12_TYPE_CONTRACT_EMPTY");
        if (genome.rangeContract.length() == 0) f.add("C12_RANGE_CONTRACT_EMPTY");
        if (genome.worstCaseCostNs <= 0 || genome.worstCaseCostNs > PER_SLICE_BUDGET_NS) {
            f.add("C12_COST_CONTRACT_FAILED");
        }
        if (!genome.pitSafe) f.add("C12_PIT_NOT_SAFE");
        if (!genome.deterministic) f.add("C12_NONDETERMINISTIC");
        if (!genome.hardConstraintsPassed) f.add("C12_HARD_CONSTRAINTS_FAILED");
        if (!Batch34EvidenceContractR929.isSha256(genome.genomeSha256)) {
            f.add("C12_GENOME_HASH_INVALID");
        } else if (!genome.calculatedSha256().equalsIgnoreCase(genome.genomeSha256)) {
            f.add("C12_GENOME_HASH_MISMATCH");
        }
        return check(f, genome.canonicalWithoutHash());
    }

    private static boolean allowedCaptureTransition(CaptureState from, CaptureState to) {
        if (from == CaptureState.SYSTEM_AVAILABLE) {
            return to == CaptureState.ELIGIBLE || to == CaptureState.FAILED;
        }
        if (from == CaptureState.ELIGIBLE) {
            return to == CaptureState.SELECTED || to == CaptureState.FAILED;
        }
        if (from == CaptureState.SELECTED) {
            return to == CaptureState.EXECUTION_STARTED || to == CaptureState.FAILED;
        }
        return false;
    }

    private static boolean allowedLifecycleTransition(LifecycleState from, LifecycleState to) {
        if (to == LifecycleState.PAUSED || to == LifecycleState.QUARANTINED
                || to == LifecycleState.PRUNED) return from != LifecycleState.PRUNED;
        if (from == LifecycleState.IDEA) return to == LifecycleState.NURSERY;
        if (from == LifecycleState.NURSERY) return to == LifecycleState.OBSERVING;
        if (from == LifecycleState.OBSERVING) return to == LifecycleState.EVIDENCE_READY;
        if (from == LifecycleState.EVIDENCE_READY) return to == LifecycleState.CHALLENGER;
        if (from == LifecycleState.CHALLENGER) return to == LifecycleState.SHADOW;
        if (from == LifecycleState.SHADOW) return to == LifecycleState.INFLUENCE;
        if (from == LifecycleState.INFLUENCE) return to == LifecycleState.CHAMPION;
        if (from == LifecycleState.PAUSED || from == LifecycleState.QUARANTINED) {
            return to == LifecycleState.OBSERVING;
        }
        return false;
    }

    private static List<String> cleanUnique(List<String> raw) {
        ArrayList<String> out = new ArrayList<String>();
        Set<String> seen = new HashSet<String>();
        if (raw != null) {
            for (String value : raw) {
                String x = safe(value);
                if (x.length() > 0 && seen.add(x)) out.add(x);
            }
        }
        Collections.sort(out);
        return out;
    }

    private static List<String> immutableStrings(List<String> raw) {
        ArrayList<String> out = new ArrayList<String>();
        if (raw != null) for (String value : raw) out.add(safe(value));
        return Collections.unmodifiableList(out);
    }

    private static Batch34EvidenceContractR929.Check check(
            List<String> failures, String canonical) {
        return new Batch34EvidenceContractR929.Check(failures, VERSION + "|" + canonical);
    }

    private static String finite(double value) {
        return Double.isFinite(value) ? String.format(Locale.US, "%.12g", value) : "NONFINITE";
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
