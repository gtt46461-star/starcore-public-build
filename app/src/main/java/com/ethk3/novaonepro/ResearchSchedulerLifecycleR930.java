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
 * Fair 8/24/64 research scheduler and append-only lifecycle V3.
 *
 * <p>Production analysis always has priority. A scheduling wave may evaluate at most eight elite,
 * twenty-four challenger and sixty-four nursery tasks, with family round-robin, deterministic
 * ordering, shared immutable compute and fixed resource ceilings. Promotion consumes a new,
 * content-addressed chronological window exactly once; repeated peeking cannot advance state.</p>
 */
public final class ResearchSchedulerLifecycleR930 {
    public static final String VERSION = "R930_SCHEDULER_LIFECYCLE_V3";
    public static final int ELITE_CAP = 8;
    public static final int CHALLENGER_CAP = 24;
    public static final int NURSERY_CAP = 64;
    public static final boolean MAY_CREATE_REAL = false;

    private ResearchSchedulerLifecycleR930() {}

    public enum Tier {
        ELITE(ELITE_CAP, 100_000_000L),
        CHALLENGER(CHALLENGER_CAP, 50_000_000L),
        NURSERY(NURSERY_CAP, 20_000_000L);
        public final int maximumTasks;
        public final long maximumTaskCostNs;
        Tier(int maximumTasks, long maximumTaskCostNs) {
            this.maximumTasks = maximumTasks;
            this.maximumTaskCostNs = maximumTaskCostNs;
        }
    }

    public enum LifecycleState {
        IDEA,
        NURSERY,
        OBSERVING,
        EVIDENCE_READY,
        CHALLENGER,
        SHADOW,
        CONTROLLED_INFLUENCE,
        CHAMPION,
        REPAIRING,
        QUARANTINED,
        RETIRED_TOMBSTONE
    }

    public enum EventType {
        REGISTER,
        EVALUATE,
        PROMOTE,
        HOLD,
        DEMOTE,
        QUARANTINE,
        REPAIR_START,
        REPAIR_SUCCESS,
        REPAIR_FAILED,
        RESTORE,
        RETIRE,
        RESOURCE_YIELD,
        FAILURE
    }

    public static final class ResearchTask {
        public final String taskId;
        public final String candidateHash;
        public final String familyId;
        public final Tier tier;
        public final int priority;
        public final String targetIssue;
        public final String inputHash;
        public final String graphHash;
        public final String experimentId;
        public final long estimatedCostNs;
        public final int attempt;
        public final String taskHash;

        public ResearchTask(String taskId, String candidateHash, String familyId, Tier tier,
                int priority, String targetIssue, String inputHash, String graphHash,
                String experimentId, long estimatedCostNs, int attempt) {
            this.taskId = required(taskId, "TASK_ID");
            this.candidateHash = requireHash(candidateHash, "TASK_CANDIDATE_HASH");
            this.familyId = required(familyId, "TASK_FAMILY");
            if (tier == null || priority < 0 || priority > 1_000_000
                    || estimatedCostNs < 0L || estimatedCostNs > tier.maximumTaskCostNs
                    || attempt < 0 || attempt > 32)
                throw new IllegalArgumentException("R930_TASK_CONTRACT_INVALID");
            this.tier = tier;
            this.priority = priority;
            this.targetIssue = required(targetIssue, "TASK_TARGET");
            this.inputHash = requireHash(inputHash, "TASK_INPUT_HASH");
            this.graphHash = requireHash(graphHash, "TASK_GRAPH_HASH");
            this.experimentId = required(experimentId, "TASK_EXPERIMENT_ID");
            this.estimatedCostNs = estimatedCostNs;
            this.attempt = attempt;
            this.taskHash = sha256(this.taskId + '|' + this.candidateHash + '|'
                    + this.familyId + '|' + tier.name() + '|' + priority + '|'
                    + this.targetIssue + '|' + this.inputHash + '|' + this.graphHash + '|'
                    + this.experimentId + '|' + estimatedCostNs + '|' + attempt);
        }
    }

    public static final class SharedComputeKey {
        public final String targetIssue;
        public final String inputHash;
        public final String graphHash;
        public final String nodeHash;
        public final String stage;
        public final String keyHash;

        public SharedComputeKey(String targetIssue, String inputHash, String graphHash,
                String nodeHash, String stage) {
            this.targetIssue = required(targetIssue, "COMPUTE_TARGET");
            this.inputHash = requireHash(inputHash, "COMPUTE_INPUT_HASH");
            this.graphHash = requireHash(graphHash, "COMPUTE_GRAPH_HASH");
            this.nodeHash = requireHash(nodeHash, "COMPUTE_NODE_HASH");
            this.stage = required(stage, "COMPUTE_STAGE");
            this.keyHash = sha256(this.targetIssue + '|' + this.inputHash + '|'
                    + this.graphHash + '|' + this.nodeHash + '|' + this.stage);
        }
    }

    public static final class SharedComputeValue {
        private final double[] values;
        private final boolean[] present;
        public final String maxSourceIssue;
        public final long frozenAtMs;
        public final long costNs;
        public final String payloadHash;
        public final String valueHash;

        public SharedComputeValue(double[] values, boolean[] present, String maxSourceIssue,
                long frozenAtMs, long costNs, String payloadHash) {
            if (values == null || present == null || values.length == 0
                    || values.length != present.length || frozenAtMs <= 0L || costNs < 0L)
                throw new IllegalArgumentException("R930_SHARED_COMPUTE_VALUE_INVALID");
            this.values = Arrays.copyOf(values, values.length);
            this.present = Arrays.copyOf(present, present.length);
            for (int i = 0; i < values.length; i++) if (present[i] && !finite(values[i]))
                throw new IllegalArgumentException("R930_SHARED_COMPUTE_NONFINITE");
            this.maxSourceIssue = required(maxSourceIssue, "COMPUTE_MAX_SOURCE");
            this.frozenAtMs = frozenAtMs;
            this.costNs = costNs;
            this.payloadHash = requireHash(payloadHash, "COMPUTE_PAYLOAD_HASH");
            this.valueHash = sha256(canonical());
        }

        public double[] valuesCopy() { return Arrays.copyOf(values, values.length); }
        public boolean[] presentCopy() { return Arrays.copyOf(present, present.length); }
        String canonical() {
            StringBuilder out = new StringBuilder(maxSourceIssue).append('|').append(frozenAtMs)
                    .append('|').append(costNs).append('|').append(payloadHash);
            for (int i = 0; i < values.length; i++)
                out.append('|').append(present[i] ? 1 : 0).append(':')
                        .append(Long.toHexString(Double.doubleToLongBits(values[i])));
            return out.toString();
        }
    }

    public interface SharedComputer {
        SharedComputeValue compute(SharedComputeKey key);
    }

    /** Bounded immutable cache; production pressure prevents new research computation. */
    public static final class SharedComputeCache {
        private final int capacity;
        private final LinkedHashMap<String, SharedComputeValue> cache =
                new LinkedHashMap<String, SharedComputeValue>(16, 0.75f, true);
        private long hitCount;
        private long missCount;

        public SharedComputeCache(int capacity) {
            if (capacity <= 0 || capacity > 4096)
                throw new IllegalArgumentException("R930_CACHE_CAPACITY_INVALID");
            this.capacity = capacity;
        }

        public synchronized SharedComputeValue getOrCompute(SharedComputeKey key,
                SharedComputer computer) {
            if (key == null || computer == null)
                throw new IllegalArgumentException("R930_CACHE_INPUT_REQUIRED");
            SharedComputeValue value = cache.get(key.keyHash);
            if (value != null) { hitCount++; return value; }
            if (RealtimePressureGateR811.hasActiveCriticalWork())
                throw new ResearchYield("PRODUCTION_CRITICAL_WORK_ACTIVE");
            missCount++;
            value = computer.compute(key);
            if (value == null
                    || !PitFeatureDagR929.isStrictPast(value.maxSourceIssue, key.targetIssue))
                throw new IllegalArgumentException("R930_CACHE_VALUE_NOT_PIT");
            cache.put(key.keyHash, value);
            while (cache.size() > capacity) {
                java.util.Iterator<Map.Entry<String, SharedComputeValue>> iterator =
                        cache.entrySet().iterator();
                iterator.next();
                iterator.remove();
            }
            return value;
        }

        public synchronized long hits() { return hitCount; }
        public synchronized long misses() { return missCount; }
        public synchronized int size() { return cache.size(); }
    }

    public static final class ResearchYield extends RuntimeException {
        public final String code;
        ResearchYield(String code) { super("R930_RESEARCH_YIELD:" + code); this.code = code; }
    }

    public static final class WavePlan {
        public final String targetIssue;
        public final List<ResearchTask> elite;
        public final List<ResearchTask> challenger;
        public final List<ResearchTask> nursery;
        public final int distinctFamilies;
        public final long estimatedCostNs;
        public final String planHash;

        WavePlan(String targetIssue, List<ResearchTask> elite,
                List<ResearchTask> challenger, List<ResearchTask> nursery) {
            this.targetIssue = targetIssue;
            this.elite = Collections.unmodifiableList(elite);
            this.challenger = Collections.unmodifiableList(challenger);
            this.nursery = Collections.unmodifiableList(nursery);
            HashSet<String> families = new HashSet<String>();
            long cost = 0L;
            StringBuilder canonical = new StringBuilder(targetIssue);
            for (ResearchTask task : all()) {
                families.add(task.familyId);
                cost = saturatedAdd(cost, task.estimatedCostNs);
                canonical.append('|').append(task.taskHash);
            }
            this.distinctFamilies = families.size();
            this.estimatedCostNs = cost;
            this.planHash = sha256(canonical.toString());
        }

        public List<ResearchTask> all() {
            ArrayList<ResearchTask> output = new ArrayList<ResearchTask>(
                    elite.size() + challenger.size() + nursery.size());
            output.addAll(elite); output.addAll(challenger); output.addAll(nursery);
            return Collections.unmodifiableList(output);
        }
    }

    /** Deterministic family round-robin; one noisy family cannot occupy an entire lane. */
    public static WavePlan planWave(String targetIssue, List<ResearchTask> pending) {
        String target = required(targetIssue, "WAVE_TARGET");
        if (pending == null) throw new IllegalArgumentException("R930_PENDING_TASKS_REQUIRED");
        ArrayList<ResearchTask> eligible = new ArrayList<ResearchTask>();
        HashSet<String> taskIds = new HashSet<String>();
        for (ResearchTask task : pending) {
            if (task == null || !target.equals(task.targetIssue) || !taskIds.add(task.taskId))
                throw new IllegalArgumentException("R930_TASK_TARGET_OR_DUPLICATE");
            eligible.add(task);
        }
        return new WavePlan(target,
                selectTier(eligible, Tier.ELITE),
                selectTier(eligible, Tier.CHALLENGER),
                selectTier(eligible, Tier.NURSERY));
    }

    private static ArrayList<ResearchTask> selectTier(List<ResearchTask> all, Tier tier) {
        LinkedHashMap<String, ArrayList<ResearchTask>> byFamily =
                new LinkedHashMap<String, ArrayList<ResearchTask>>();
        ArrayList<ResearchTask> candidates = new ArrayList<ResearchTask>();
        for (ResearchTask task : all) if (task.tier == tier) candidates.add(task);
        Collections.sort(candidates, TASK_ORDER);
        for (ResearchTask task : candidates) {
            ArrayList<ResearchTask> family = byFamily.get(task.familyId);
            if (family == null) {
                family = new ArrayList<ResearchTask>();
                byFamily.put(task.familyId, family);
            }
            family.add(task);
        }
        ArrayList<String> families = new ArrayList<String>(byFamily.keySet());
        Collections.sort(families);
        ArrayList<ResearchTask> output = new ArrayList<ResearchTask>();
        boolean progressed = true;
        int round = 0;
        while (progressed && output.size() < tier.maximumTasks) {
            progressed = false;
            for (String familyId : families) {
                ArrayList<ResearchTask> family = byFamily.get(familyId);
                if (round < family.size()) {
                    output.add(family.get(round));
                    progressed = true;
                    if (output.size() == tier.maximumTasks) break;
                }
            }
            round++;
        }
        return output;
    }

    private static final Comparator<ResearchTask> TASK_ORDER = new Comparator<ResearchTask>() {
        @Override public int compare(ResearchTask left, ResearchTask right) {
            int byPriority = Integer.compare(right.priority, left.priority);
            if (byPriority != 0) return byPriority;
            int byAttempt = Integer.compare(left.attempt, right.attempt);
            if (byAttempt != 0) return byAttempt;
            return left.taskHash.compareTo(right.taskHash);
        }
    };

    public static final class WindowEvidence {
        public final String windowId;
        public final String firstIssue;
        public final String lastIssue;
        public final String contentHash;
        public final String baselineHash;
        public final String candidateHash;
        public final int plannedN;
        public final int eligibleN;
        public final int selectedN;
        public final int hitN;
        public final int pairedN;
        public final int baselineHitN;
        public final double brier;
        public final double ece;
        public final double stability;
        public final long p95CostNs;
        public final double adjustedP;
        public final boolean sealed;
        public final String evidenceHash;

        public WindowEvidence(String windowId, String firstIssue, String lastIssue,
                String contentHash, String baselineHash, String candidateHash, int plannedN,
                int eligibleN, int selectedN, int hitN, int pairedN, int baselineHitN,
                double brier, double ece, double stability, long p95CostNs,
                double adjustedP, boolean sealed) {
            this.windowId = required(windowId, "WINDOW_ID");
            this.firstIssue = required(firstIssue, "WINDOW_FIRST");
            this.lastIssue = required(lastIssue, "WINDOW_LAST");
            if (PitFeatureDagR929.compareIssues(this.firstIssue, this.lastIssue) > 0)
                throw new IllegalArgumentException("R930_WINDOW_REVERSED");
            this.contentHash = requireHash(contentHash, "WINDOW_CONTENT_HASH");
            this.baselineHash = requireHash(baselineHash, "WINDOW_BASELINE_HASH");
            this.candidateHash = requireHash(candidateHash, "WINDOW_CANDIDATE_HASH");
            if (plannedN <= 0 || eligibleN < 0 || eligibleN > plannedN || selectedN < 0
                    || selectedN > eligibleN || hitN < 0 || hitN > selectedN
                    || pairedN < 0 || pairedN > plannedN || baselineHitN < 0
                    || baselineHitN > pairedN || !metricOrUnknown(brier)
                    || !metricOrUnknown(ece) || !metricOrUnknown(stability)
                    || p95CostNs < 0L || !metricOrUnknown(adjustedP))
                throw new IllegalArgumentException("R930_WINDOW_METRIC_INVALID");
            this.plannedN = plannedN;
            this.eligibleN = eligibleN;
            this.selectedN = selectedN;
            this.hitN = hitN;
            this.pairedN = pairedN;
            this.baselineHitN = baselineHitN;
            this.brier = brier;
            this.ece = ece;
            this.stability = stability;
            this.p95CostNs = p95CostNs;
            this.adjustedP = adjustedP;
            this.sealed = sealed;
            this.evidenceHash = sha256(this.windowId + '|' + this.firstIssue + '|'
                    + this.lastIssue + '|' + this.contentHash + '|' + this.baselineHash + '|'
                    + this.candidateHash + '|' + plannedN + '|' + eligibleN + '|'
                    + selectedN + '|' + hitN + '|' + pairedN + '|' + baselineHitN + '|'
                    + brier + '|' + ece + '|' + stability + '|' + p95CostNs + '|'
                    + adjustedP + '|' + (sealed ? 1 : 0));
        }

        public double hitRate() { return selectedN == 0 ? Double.NaN : hitN / (double) selectedN; }
        public double coverage() { return selectedN / (double) plannedN; }
        public double pairedDelta() {
            return pairedN == 0 ? Double.NaN : (hitN - baselineHitN) / (double) pairedN;
        }
    }

    public static final class LifecycleEvent {
        public final String candidateHash;
        public final LifecycleState from;
        public final LifecycleState to;
        public final EventType type;
        public final String windowHash;
        public final String parentEventHash;
        public final String reason;
        public final long createdAtMs;
        public final String eventHash;

        public LifecycleEvent(String candidateHash, LifecycleState from, LifecycleState to,
                EventType type, String windowHash, String parentEventHash, String reason,
                long createdAtMs) {
            this.candidateHash = requireHash(candidateHash, "EVENT_CANDIDATE_HASH");
            if (from == null || to == null || type == null || createdAtMs <= 0L)
                throw new IllegalArgumentException("R930_LIFECYCLE_EVENT_INVALID");
            this.from = from;
            this.to = to;
            this.type = type;
            this.windowHash = clean(windowHash).length() == 0 ? ""
                    : requireHash(windowHash, "EVENT_WINDOW_HASH");
            this.parentEventHash = clean(parentEventHash).length() == 0 ? ""
                    : requireHash(parentEventHash, "EVENT_PARENT_HASH");
            this.reason = required(reason, "EVENT_REASON");
            this.createdAtMs = createdAtMs;
            this.eventHash = sha256(this.candidateHash + '|' + from.name() + '|'
                    + to.name() + '|' + type.name() + '|' + this.windowHash + '|'
                    + this.parentEventHash + '|' + this.reason + '|' + createdAtMs);
        }
    }

    public static final class LifecycleLedger {
        public final String candidateHash;
        public final LifecycleState state;
        public final List<LifecycleEvent> events;
        public final Set<String> consumedWindowHashes;
        public final String latestEventHash;
        public final String ledgerHash;

        private LifecycleLedger(String candidateHash, LifecycleState state,
                List<LifecycleEvent> events, Set<String> consumedWindowHashes) {
            this.candidateHash = candidateHash;
            this.state = state;
            this.events = Collections.unmodifiableList(new ArrayList<LifecycleEvent>(events));
            this.consumedWindowHashes = Collections.unmodifiableSet(
                    new LinkedHashSet<String>(consumedWindowHashes));
            this.latestEventHash = events.isEmpty() ? "" : events.get(events.size() - 1).eventHash;
            StringBuilder canonical = new StringBuilder(candidateHash).append('|')
                    .append(state.name());
            for (LifecycleEvent event : events) canonical.append('|').append(event.eventHash);
            for (String window : consumedWindowHashes) canonical.append("|w=").append(window);
            this.ledgerHash = sha256(canonical.toString());
        }

        public static LifecycleLedger register(String candidateHash, long nowMs) {
            String candidate = requireHash(candidateHash, "REGISTER_CANDIDATE_HASH");
            LifecycleEvent event = new LifecycleEvent(candidate, LifecycleState.IDEA,
                    LifecycleState.NURSERY, EventType.REGISTER, "", "",
                    "REGISTERED_IN_NURSERY_NO_WEIGHT", nowMs);
            return new LifecycleLedger(candidate, LifecycleState.NURSERY,
                    Collections.singletonList(event), Collections.<String>emptySet());
        }

        public LifecycleLedger apply(WindowEvidence evidence, long nowMs) {
            if (evidence == null || !candidateHash.equals(evidence.candidateHash)
                    || !evidence.sealed)
                throw new IllegalArgumentException("R930_LIFECYCLE_WINDOW_INVALID");
            if (consumedWindowHashes.contains(evidence.contentHash))
                throw new IllegalStateException("R930_WINDOW_ALREADY_CONSUMED");
            Transition transition = decide(state, evidence);
            LifecycleEvent event = new LifecycleEvent(candidateHash, state, transition.to,
                    transition.type, evidence.contentHash, latestEventHash,
                    transition.reason, nowMs);
            ArrayList<LifecycleEvent> nextEvents = new ArrayList<LifecycleEvent>(events);
            nextEvents.add(event);
            LinkedHashSet<String> nextWindows = new LinkedHashSet<String>(consumedWindowHashes);
            nextWindows.add(evidence.contentHash);
            return new LifecycleLedger(candidateHash, transition.to, nextEvents, nextWindows);
        }

        public LifecycleLedger quarantine(String reason, long nowMs) {
            if (state == LifecycleState.RETIRED_TOMBSTONE)
                throw new IllegalStateException("R930_TOMBSTONE_TERMINAL");
            LifecycleEvent event = new LifecycleEvent(candidateHash, state,
                    LifecycleState.QUARANTINED, EventType.QUARANTINE, "", latestEventHash,
                    required(reason, "QUARANTINE_REASON"), nowMs);
            ArrayList<LifecycleEvent> next = new ArrayList<LifecycleEvent>(events);
            next.add(event);
            return new LifecycleLedger(candidateHash, LifecycleState.QUARANTINED,
                    next, consumedWindowHashes);
        }

        public LifecycleLedger startRepair(String repairPlanHash, long nowMs) {
            if (state != LifecycleState.QUARANTINED && state != LifecycleState.CHAMPION
                    && state != LifecycleState.CONTROLLED_INFLUENCE)
                throw new IllegalStateException("R930_REPAIR_STATE_INVALID");
            LifecycleEvent event = new LifecycleEvent(candidateHash, state,
                    LifecycleState.REPAIRING, EventType.REPAIR_START,
                    requireHash(repairPlanHash, "REPAIR_PLAN_HASH"), latestEventHash,
                    "ELITE_PRESERVED_PARENT_REPAIR_CHILD", nowMs);
            ArrayList<LifecycleEvent> next = new ArrayList<LifecycleEvent>(events);
            next.add(event);
            return new LifecycleLedger(candidateHash, LifecycleState.REPAIRING,
                    next, consumedWindowHashes);
        }

        public LifecycleLedger retire(String tombstoneReason, long nowMs) {
            LifecycleEvent event = new LifecycleEvent(candidateHash, state,
                    LifecycleState.RETIRED_TOMBSTONE, EventType.RETIRE, "", latestEventHash,
                    required(tombstoneReason, "TOMBSTONE_REASON"), nowMs);
            ArrayList<LifecycleEvent> next = new ArrayList<LifecycleEvent>(events);
            next.add(event);
            return new LifecycleLedger(candidateHash, LifecycleState.RETIRED_TOMBSTONE,
                    next, consumedWindowHashes);
        }
    }

    private static final class Transition {
        final LifecycleState to;
        final EventType type;
        final String reason;
        Transition(LifecycleState to, EventType type, String reason) {
            this.to = to; this.type = type; this.reason = reason;
        }
    }

    private static Transition decide(LifecycleState current, WindowEvidence evidence) {
        if (current == LifecycleState.RETIRED_TOMBSTONE)
            throw new IllegalStateException("R930_TOMBSTONE_CANNOT_PROMOTE");
        if (current == LifecycleState.QUARANTINED || current == LifecycleState.REPAIRING)
            return new Transition(current, EventType.HOLD, "REPAIR_REQUIRES_CHILD_LEDGER");
        double hit = evidence.hitRate();
        double coverage = evidence.coverage();
        double delta = evidence.pairedDelta();
        boolean integrity = metric(evidence.brier) && metric(evidence.ece)
                && metric(evidence.stability) && evidence.p95CostNs > 0L;
        boolean quality = integrity && evidence.selectedN > 0 && hit >= 0.40d
                && coverage >= 0.15d && evidence.ece <= 0.04d
                && evidence.stability >= 0.50d;
        boolean paired = evidence.pairedN >= 120 && finite(delta) && delta > 0.0d
                && metric(evidence.adjustedP) && evidence.adjustedP <= 0.05d;

        if (!integrity)
            return new Transition(current, EventType.HOLD, "EVIDENCE_INCOMPLETE");
        if (current == LifecycleState.NURSERY) {
            if (evidence.plannedN < 24)
                return new Transition(current, EventType.HOLD, "NURSERY_NEEDS_24");
            return new Transition(LifecycleState.OBSERVING, EventType.PROMOTE,
                    "NEW_24_WINDOW_OBSERVED");
        }
        if (current == LifecycleState.OBSERVING) {
            if (evidence.plannedN < 48)
                return new Transition(current, EventType.HOLD, "OBSERVING_NEEDS_48");
            return new Transition(LifecycleState.EVIDENCE_READY, EventType.PROMOTE,
                    "NEW_48_WINDOW_EVIDENCE_READY");
        }
        if (current == LifecycleState.EVIDENCE_READY) {
            if (evidence.plannedN < 120)
                return new Transition(current, EventType.HOLD, "CHALLENGER_NEEDS_120");
            if (!quality)
                return new Transition(current, EventType.HOLD,
                        "JOINT_HIT_COVERAGE_CALIBRATION_STABILITY_NOT_MET");
            return new Transition(LifecycleState.CHALLENGER, EventType.PROMOTE,
                    "NEW_120_WINDOW_JOINT_QUALITY_PASS");
        }
        if (current == LifecycleState.CHALLENGER) {
            if (evidence.plannedN < 120 || !quality)
                return new Transition(current, EventType.HOLD, "INDEPENDENT_SHADOW_WINDOW_NOT_PASS");
            return new Transition(LifecycleState.SHADOW, EventType.PROMOTE,
                    "INDEPENDENT_NEW_120_SHADOW_PASS");
        }
        if (current == LifecycleState.SHADOW) {
            if (evidence.plannedN < 240 || !quality || !paired)
                return new Transition(current, EventType.HOLD,
                        "L6_NEEDS_NEW_240_PAIRED_MULTIPLICITY_PASS");
            return new Transition(LifecycleState.CONTROLLED_INFLUENCE, EventType.PROMOTE,
                    "L6_CONTROLLED_WEIGHT_MAX_5_PERCENT");
        }
        if (current == LifecycleState.CONTROLLED_INFLUENCE) {
            if (evidence.plannedN < 480 || !quality || !paired)
                return new Transition(current, EventType.HOLD,
                        "CHAMPION_NEEDS_NEW_480_PAIRED_WINDOW");
            return new Transition(LifecycleState.CHAMPION, EventType.PROMOTE,
                    "CHAMPION_INDEPENDENT_NEW_480_PASS");
        }
        if (current == LifecycleState.CHAMPION) {
            if (!quality || !paired)
                return new Transition(LifecycleState.REPAIRING, EventType.REPAIR_START,
                        "CHAMPION_DRIFT_MINIMAL_REPAIR_REQUIRED");
            return new Transition(current, EventType.HOLD, "CHAMPION_RECONFIRMED");
        }
        return new Transition(current, EventType.HOLD, "STATE_UNCHANGED");
    }

    public static final class RepairLedgerEntry {
        public final String parentCandidateHash;
        public final String childCandidateHash;
        public final String planHash;
        public final String parentEvidenceHash;
        public final String childEvidenceHash;
        public final boolean success;
        public final String reason;
        public final long createdAtMs;
        public final String entryHash;

        public RepairLedgerEntry(String parentCandidateHash, String childCandidateHash,
                String planHash, String parentEvidenceHash, String childEvidenceHash,
                boolean success, String reason, long createdAtMs) {
            this.parentCandidateHash = requireHash(parentCandidateHash, "REPAIR_PARENT_HASH");
            this.childCandidateHash = requireHash(childCandidateHash, "REPAIR_CHILD_HASH");
            if (this.parentCandidateHash.equals(this.childCandidateHash))
                throw new IllegalArgumentException("R930_REPAIR_CHILD_EQUALS_PARENT");
            this.planHash = requireHash(planHash, "REPAIR_PLAN_HASH");
            this.parentEvidenceHash = requireHash(parentEvidenceHash, "REPAIR_PARENT_EVIDENCE");
            this.childEvidenceHash = requireHash(childEvidenceHash, "REPAIR_CHILD_EVIDENCE");
            this.success = success;
            this.reason = required(reason, "REPAIR_LEDGER_REASON");
            if (createdAtMs <= 0L) throw new IllegalArgumentException("R930_REPAIR_LEDGER_TIME");
            this.createdAtMs = createdAtMs;
            this.entryHash = sha256(this.parentCandidateHash + '|' + this.childCandidateHash + '|'
                    + this.planHash + '|' + this.parentEvidenceHash + '|'
                    + this.childEvidenceHash + '|' + (success ? 1 : 0) + '|'
                    + this.reason + '|' + createdAtMs);
        }
    }

    public static final class IssueFailure {
        public final String taskHash;
        public final String candidateHash;
        public final String targetIssue;
        public final String stage;
        public final String code;
        public final String detail;
        public final boolean retryable;
        public final boolean productionFallbackSucceeded;
        public final long elapsedNs;
        public final String failureHash;

        public IssueFailure(String taskHash, String candidateHash, String targetIssue,
                String stage, String code, String detail, boolean retryable,
                boolean productionFallbackSucceeded, long elapsedNs) {
            this.taskHash = requireHash(taskHash, "ISSUE_FAILURE_TASK_HASH");
            this.candidateHash = requireHash(candidateHash, "ISSUE_FAILURE_CANDIDATE_HASH");
            this.targetIssue = required(targetIssue, "ISSUE_FAILURE_TARGET");
            this.stage = required(stage, "ISSUE_FAILURE_STAGE");
            this.code = required(code, "ISSUE_FAILURE_CODE");
            this.detail = clean(detail);
            this.retryable = retryable;
            this.productionFallbackSucceeded = productionFallbackSucceeded;
            if (elapsedNs < 0L) throw new IllegalArgumentException("R930_ISSUE_FAILURE_TIME");
            this.elapsedNs = elapsedNs;
            this.failureHash = sha256(this.taskHash + '|' + this.candidateHash + '|'
                    + this.targetIssue + '|' + this.stage + '|' + this.code + '|'
                    + this.detail + '|' + (retryable ? 1 : 0) + '|'
                    + (productionFallbackSucceeded ? 1 : 0) + '|' + elapsedNs);
        }
    }

    private static long saturatedAdd(long left, long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) return Long.MAX_VALUE;
        return left + right;
    }
    private static boolean metricOrUnknown(double value) { return metric(value) || value == -1.0d; }
    private static boolean metric(double value) { return finite(value) && value >= 0.0d && value <= 1.0d; }
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
