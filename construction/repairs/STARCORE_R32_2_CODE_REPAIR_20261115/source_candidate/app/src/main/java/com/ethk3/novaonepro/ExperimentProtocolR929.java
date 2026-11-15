package com.ethk3.novaonepro;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Content-addressed experiment protocol for chronological model research.
 *
 * <p>Directory names and archived/active presentation state are intentionally absent from an
 * experiment identity.  Copying a dataset or renaming a family therefore cannot restore access to
 * a previously consumed sealed window.  Every candidate, hyper-parameter search, domain route and
 * repair child consumes the pre-registered hypothesis budget.</p>
 */
public final class ExperimentProtocolR929 {
    public static final String VERSION = "R929_EXPERIMENT_PROTOCOL_V1";
    public static final double DEFAULT_TOTAL_ALPHA = 0.05d;

    private ExperimentProtocolR929() {}

    public enum Stage {
        DATASET,
        NUMBER,
        GATE,
        RANKER,
        CALIBRATION,
        POLICY,
        EVALUATE
    }

    public enum WindowKind { TRAIN, TUNE, CAL, POLICY, SEALED, FUTURE }
    public enum TrialStatus { REGISTERED, RUNNING, FAILED, COMPLETED }

    public static final class IssueRange {
        public final String first;
        public final String last;
        public final String hash;

        public IssueRange(String first, String last) {
            this.first = required(first, "RANGE_FIRST");
            this.last = required(last, "RANGE_LAST");
            if (PitFeatureDagR929.compareIssues(this.first, this.last) > 0)
                throw new IllegalArgumentException("R929_RANGE_REVERSED");
            this.hash = sha256(this.first + '|' + this.last);
        }

        public boolean strictlyBefore(IssueRange other) {
            return other != null && PitFeatureDagR929.compareIssues(last, other.first) < 0;
        }

        public boolean contains(String issue) {
            return PitFeatureDagR929.compareIssues(first, issue) <= 0
                    && PitFeatureDagR929.compareIssues(issue, last) <= 0;
        }

        public String canonical() { return first + ':' + last + ':' + hash; }
    }

    /** One outer chronological fold.  Labels may only move left-to-right. */
    public static final class Fold {
        public final String foldId;
        public final IssueRange train;
        public final IssueRange tune;
        public final IssueRange calibration;
        public final IssueRange policy;
        public final IssueRange sealed;
        public final int purgeIssues;
        public final int embargoIssues;
        public final String foldHash;

        public Fold(String foldId, IssueRange train, IssueRange tune,
                IssueRange calibration, IssueRange policy, IssueRange sealed,
                int purgeIssues, int embargoIssues) {
            this.foldId = required(foldId, "FOLD_ID");
            if (train == null || tune == null || calibration == null || policy == null
                    || sealed == null || purgeIssues < 0 || embargoIssues < 0)
                throw new IllegalArgumentException("R929_FOLD_FIELDS_INVALID");
            if (!train.strictlyBefore(tune) || !tune.strictlyBefore(calibration)
                    || !calibration.strictlyBefore(policy) || !policy.strictlyBefore(sealed))
                throw new IllegalArgumentException("R929_FOLD_TIME_ORDER");
            this.train = train;
            this.tune = tune;
            this.calibration = calibration;
            this.policy = policy;
            this.sealed = sealed;
            this.purgeIssues = purgeIssues;
            this.embargoIssues = embargoIssues;
            this.foldHash = sha256(canonicalWithoutHash());
        }

        String canonicalWithoutHash() {
            return foldId + '|' + train.canonical() + '|' + tune.canonical() + '|'
                    + calibration.canonical() + '|' + policy.canonical() + '|'
                    + sealed.canonical() + '|' + purgeIssues + '|' + embargoIssues;
        }

        public String canonical() { return canonicalWithoutHash() + '|' + foldHash; }
    }

    /** At least five expanding outer folds are required for a production comparison. */
    public static final class SplitPlan {
        public final List<Fold> folds;
        public final IssueRange future;
        public final String labelAvailabilityContract;
        public final String planHash;

        public SplitPlan(List<Fold> folds, IssueRange future, String labelAvailabilityContract) {
            if (folds == null || folds.size() < 5 || future == null)
                throw new IllegalArgumentException("R929_AT_LEAST_FIVE_FOLDS_REQUIRED");
            this.labelAvailabilityContract = required(
                    labelAvailabilityContract, "LABEL_AVAILABILITY_CONTRACT");
            ArrayList<Fold> copy = new ArrayList<Fold>(folds);
            Set<String> ids = new LinkedHashSet<String>();
            for (int i = 0; i < copy.size(); i++) {
                Fold fold = copy.get(i);
                if (fold == null || !ids.add(fold.foldId))
                    throw new IllegalArgumentException("R929_FOLD_NULL_OR_DUPLICATE");
                if (i > 0) {
                    Fold previous = copy.get(i - 1);
                    if (PitFeatureDagR929.compareIssues(previous.train.first, fold.train.first) != 0
                            || PitFeatureDagR929.compareIssues(previous.train.last, fold.train.last) >= 0)
                        throw new IllegalArgumentException("R929_TRAIN_NOT_EXPANDING");
                    if (!previous.sealed.strictlyBefore(fold.sealed))
                        throw new IllegalArgumentException("R929_OUTER_FOLD_NOT_CHRONOLOGICAL");
                }
            }
            if (!copy.get(copy.size() - 1).sealed.strictlyBefore(future))
                throw new IllegalArgumentException("R929_FUTURE_NOT_AFTER_SEALED");
            this.folds = Collections.unmodifiableList(copy);
            this.future = future;
            this.planHash = sha256(canonicalWithoutHash());
        }

        String canonicalWithoutHash() {
            StringBuilder out = new StringBuilder(512);
            out.append(labelAvailabilityContract).append('|').append(future.canonical());
            for (Fold fold : folds) out.append('\n').append(fold.canonical());
            return out.toString();
        }

        public String canonical() { return canonicalWithoutHash() + "\nplanHash=" + planHash; }
    }

    public static final class ArtifactDependency {
        public final String role;
        public final String artifactHash;
        public final String schemaHash;
        public final Stage stage;
        public final String dependencyHash;

        public ArtifactDependency(String role, String artifactHash, String schemaHash, Stage stage) {
            this.role = required(role, "DEPENDENCY_ROLE");
            this.artifactHash = requireHash(artifactHash, "DEPENDENCY_ARTIFACT_HASH");
            this.schemaHash = requireHash(schemaHash, "DEPENDENCY_SCHEMA_HASH");
            if (stage == null) throw new IllegalArgumentException("R929_DEPENDENCY_STAGE_REQUIRED");
            this.stage = stage;
            this.dependencyHash = sha256(canonicalWithoutHash());
        }

        String canonicalWithoutHash() {
            return role + '|' + artifactHash + '|' + schemaHash + '|' + stage.name();
        }
    }

    /** Stable trial identity; no path, display name or archive state participates. */
    public static final class TrialIdentity {
        public final String game;
        public final String schemaHash;
        public final String datasetContentHash;
        public final String sourceCodeHash;
        public final String hyperParameterHash;
        public final long seed;
        public final String statisticalFamilyId;
        public final String candidateStructuralHash;
        public final Stage stage;
        public final String splitPlanHash;
        public final List<ArtifactDependency> dependencies;
        public final String trialId;

        public TrialIdentity(
                String game,
                String schemaHash,
                String datasetContentHash,
                String sourceCodeHash,
                String hyperParameterHash,
                long seed,
                String statisticalFamilyId,
                String candidateStructuralHash,
                Stage stage,
                String splitPlanHash,
                List<ArtifactDependency> dependencies) {
            this.game = required(game, "TRIAL_GAME");
            this.schemaHash = requireHash(schemaHash, "TRIAL_SCHEMA_HASH");
            this.datasetContentHash = requireHash(datasetContentHash, "TRIAL_DATASET_HASH");
            this.sourceCodeHash = requireHash(sourceCodeHash, "TRIAL_SOURCE_HASH");
            this.hyperParameterHash = requireHash(hyperParameterHash, "TRIAL_HYPER_HASH");
            this.seed = seed;
            this.statisticalFamilyId = required(statisticalFamilyId, "TRIAL_FAMILY");
            this.candidateStructuralHash = requireHash(
                    candidateStructuralHash, "TRIAL_CANDIDATE_HASH");
            if (stage == null) throw new IllegalArgumentException("R929_TRIAL_STAGE_REQUIRED");
            this.stage = stage;
            this.splitPlanHash = requireHash(splitPlanHash, "TRIAL_SPLIT_HASH");
            ArrayList<ArtifactDependency> copy = new ArrayList<ArtifactDependency>();
            if (dependencies != null) copy.addAll(dependencies);
            Collections.sort(copy, new Comparator<ArtifactDependency>() {
                @Override public int compare(ArtifactDependency a, ArtifactDependency b) {
                    int byRole = a.role.compareTo(b.role);
                    return byRole != 0 ? byRole : a.dependencyHash.compareTo(b.dependencyHash);
                }
            });
            this.dependencies = Collections.unmodifiableList(copy);
            this.trialId = sha256(canonicalWithoutId());
        }

        String canonicalWithoutId() {
            StringBuilder out = new StringBuilder(512);
            out.append(game).append('|').append(schemaHash).append('|')
                    .append(datasetContentHash).append('|').append(sourceCodeHash).append('|')
                    .append(hyperParameterHash).append('|').append(seed).append('|')
                    .append(statisticalFamilyId).append('|').append(candidateStructuralHash)
                    .append('|').append(stage.name()).append('|').append(splitPlanHash);
            for (ArtifactDependency dependency : dependencies)
                out.append('\n').append(dependency.dependencyHash);
            return out.toString();
        }

        public String canonical() { return canonicalWithoutId() + "\ntrialId=" + trialId; }
    }

    public static final class TrialRecord {
        public final TrialIdentity identity;
        public final TrialStatus status;
        public final long registeredAtMs;
        public final long startedAtMs;
        public final long finishedAtMs;
        public final String outputArtifactHash;
        public final String failureCode;
        public final boolean sealedTokenConsumed;
        public final String sealedTokenHash;
        public final String recordHash;

        TrialRecord(TrialIdentity identity, TrialStatus status, long registeredAtMs,
                long startedAtMs, long finishedAtMs, String outputArtifactHash,
                String failureCode, boolean sealedTokenConsumed, String sealedTokenHash) {
            if (identity == null || status == null || registeredAtMs <= 0L)
                throw new IllegalArgumentException("R929_TRIAL_RECORD_INVALID");
            this.identity = identity;
            this.status = status;
            this.registeredAtMs = registeredAtMs;
            this.startedAtMs = Math.max(0L, startedAtMs);
            this.finishedAtMs = Math.max(0L, finishedAtMs);
            this.outputArtifactHash = clean(outputArtifactHash);
            this.failureCode = clean(failureCode);
            this.sealedTokenConsumed = sealedTokenConsumed;
            this.sealedTokenHash = clean(sealedTokenHash);
            this.recordHash = sha256(canonicalWithoutHash());
        }

        TrialRecord transition(TrialStatus next, long now, String output, String failure) {
            if (next == null || now <= 0L) throw new IllegalArgumentException("R929_TRANSITION_INVALID");
            if (status == TrialStatus.FAILED || status == TrialStatus.COMPLETED)
                throw new IllegalStateException("R929_TRIAL_TERMINAL");
            if (status == TrialStatus.REGISTERED && next != TrialStatus.RUNNING)
                throw new IllegalStateException("R929_TRIAL_MUST_START");
            if (status == TrialStatus.RUNNING
                    && next != TrialStatus.FAILED && next != TrialStatus.COMPLETED)
                throw new IllegalStateException("R929_TRIAL_BAD_FINISH_STATE");
            long started = next == TrialStatus.RUNNING ? now : startedAtMs;
            long finished = (next == TrialStatus.FAILED || next == TrialStatus.COMPLETED) ? now : 0L;
            if (next == TrialStatus.COMPLETED) requireHash(output, "TRIAL_OUTPUT_HASH");
            if (next == TrialStatus.FAILED && clean(failure).length() == 0)
                throw new IllegalArgumentException("R929_TRIAL_FAILURE_CODE_REQUIRED");
            return new TrialRecord(identity, next, registeredAtMs, started, finished,
                    output, failure, sealedTokenConsumed, sealedTokenHash);
        }

        TrialRecord consumeSealed(String tokenHash) {
            if (sealedTokenConsumed) throw new IllegalStateException("R929_SEALED_ALREADY_CONSUMED");
            return new TrialRecord(identity, status, registeredAtMs, startedAtMs, finishedAtMs,
                    outputArtifactHash, failureCode, true,
                    requireHash(tokenHash, "SEALED_TOKEN_HASH"));
        }

        String canonicalWithoutHash() {
            return identity.trialId + '|' + status.name() + '|' + registeredAtMs + '|'
                    + startedAtMs + '|' + finishedAtMs + '|' + outputArtifactHash + '|'
                    + failureCode + '|' + (sealedTokenConsumed ? 1 : 0) + '|' + sealedTokenHash;
        }
    }

    /** CAS storage contract used by both SQLite and the deterministic host implementation. */
    public interface TrialStore {
        TrialRecord get(String trialId);
        boolean putIfAbsent(TrialRecord record);
        boolean compareAndSet(String trialId, String expectedRecordHash, TrialRecord replacement);
        List<TrialRecord> all();
    }

    public static final class MemoryTrialStore implements TrialStore {
        private final Map<String, TrialRecord> rows = new LinkedHashMap<String, TrialRecord>();

        @Override public synchronized TrialRecord get(String trialId) { return rows.get(trialId); }

        @Override public synchronized boolean putIfAbsent(TrialRecord record) {
            if (record == null || rows.containsKey(record.identity.trialId)) return false;
            rows.put(record.identity.trialId, record);
            return true;
        }

        @Override public synchronized boolean compareAndSet(
                String trialId, String expectedRecordHash, TrialRecord replacement) {
            TrialRecord current = rows.get(trialId);
            if (current == null || replacement == null
                    || !current.recordHash.equals(expectedRecordHash)
                    || !replacement.identity.trialId.equals(trialId)) return false;
            rows.put(trialId, replacement);
            return true;
        }

        @Override public synchronized List<TrialRecord> all() {
            return Collections.unmodifiableList(new ArrayList<TrialRecord>(rows.values()));
        }
    }

    public static final class Registry {
        private final TrialStore store;

        public Registry(TrialStore store) {
            if (store == null) throw new IllegalArgumentException("R929_TRIAL_STORE_REQUIRED");
            this.store = store;
        }

        public TrialRecord register(TrialIdentity identity, long nowMs) {
            if (identity == null || nowMs <= 0L)
                throw new IllegalArgumentException("R929_REGISTER_INPUT_INVALID");
            TrialRecord created = new TrialRecord(identity, TrialStatus.REGISTERED, nowMs,
                    0L, 0L, "", "", false, "");
            if (store.putIfAbsent(created)) return created;
            TrialRecord existing = store.get(identity.trialId);
            if (existing == null || !existing.identity.canonical().equals(identity.canonical()))
                throw new IllegalStateException("R929_TRIAL_ID_COLLISION");
            return existing;
        }

        public TrialRecord start(String trialId, long nowMs) {
            return transition(trialId, TrialStatus.RUNNING, nowMs, "", "");
        }

        public TrialRecord complete(String trialId, long nowMs, String outputArtifactHash) {
            return transition(trialId, TrialStatus.COMPLETED, nowMs,
                    outputArtifactHash, "");
        }

        public TrialRecord fail(String trialId, long nowMs, String failureCode) {
            return transition(trialId, TrialStatus.FAILED, nowMs, "", failureCode);
        }

        public TrialRecord consumeSealedToken(
                String trialId, String sealedRangeHash, String sealedContentHash, long nowMs) {
            TrialRecord current = requireRecord(trialId);
            if (current.status == TrialStatus.FAILED || current.status == TrialStatus.COMPLETED)
                throw new IllegalStateException("R929_SEALED_TOKEN_AFTER_TERMINAL");
            String token = sha256("SEALED_ACCESS|" + trialId + '|'
                    + requireHash(sealedRangeHash, "SEALED_RANGE_HASH") + '|'
                    + requireHash(sealedContentHash, "SEALED_CONTENT_HASH") + '|' + nowMs);
            TrialRecord replacement = current.consumeSealed(token);
            if (!store.compareAndSet(trialId, current.recordHash, replacement))
                throw new IllegalStateException("R929_SEALED_TOKEN_CAS_LOST");
            return replacement;
        }

        private TrialRecord transition(String trialId, TrialStatus status, long nowMs,
                String output, String failure) {
            TrialRecord current = requireRecord(trialId);
            TrialRecord replacement = current.transition(status, nowMs, output, failure);
            if (!store.compareAndSet(trialId, current.recordHash, replacement))
                throw new IllegalStateException("R929_TRIAL_TRANSITION_CAS_LOST");
            return replacement;
        }

        private TrialRecord requireRecord(String trialId) {
            TrialRecord current = store.get(required(trialId, "TRIAL_ID"));
            if (current == null) throw new IllegalArgumentException("R929_TRIAL_NOT_REGISTERED");
            return current;
        }
    }

    public static final class Hypothesis {
        public final String hypothesisId;
        public final String statisticalFamilyId;
        public final String structuralHash;
        public final String purpose;
        public final double allocatedAlpha;
        public final int registeredOrdinal;
        public final String hypothesisHash;

        Hypothesis(String hypothesisId, String statisticalFamilyId, String structuralHash,
                String purpose, double allocatedAlpha, int registeredOrdinal) {
            this.hypothesisId = required(hypothesisId, "HYPOTHESIS_ID");
            this.statisticalFamilyId = required(statisticalFamilyId, "HYPOTHESIS_FAMILY");
            this.structuralHash = requireHash(structuralHash, "HYPOTHESIS_STRUCTURAL_HASH");
            this.purpose = required(purpose, "HYPOTHESIS_PURPOSE");
            if (!(allocatedAlpha > 0.0d) || allocatedAlpha > 1.0d || registeredOrdinal < 0)
                throw new IllegalArgumentException("R929_HYPOTHESIS_BUDGET_INVALID");
            this.allocatedAlpha = allocatedAlpha;
            this.registeredOrdinal = registeredOrdinal;
            this.hypothesisHash = sha256(canonicalWithoutHash());
        }

        String canonicalWithoutHash() {
            return hypothesisId + '|' + statisticalFamilyId + '|' + structuralHash + '|'
                    + purpose + '|' + allocatedAlpha + '|' + registeredOrdinal;
        }
    }

    /** Fixed-batch alpha ledger. Archive/rename operations cannot delete an hypothesis. */
    public static final class MultiplicityLedger {
        private final double totalAlpha;
        private final Map<String, Double> familyAlpha = new LinkedHashMap<String, Double>();
        private final Map<String, Hypothesis> hypotheses = new LinkedHashMap<String, Hypothesis>();
        private final Map<String, Set<Integer>> looks = new HashMap<String, Set<Integer>>();
        private double allocated;

        public MultiplicityLedger(double totalAlpha) {
            if (!(totalAlpha > 0.0d) || totalAlpha > 1.0d)
                throw new IllegalArgumentException("R929_TOTAL_ALPHA_INVALID");
            this.totalAlpha = totalAlpha;
        }

        public synchronized void registerFamily(String familyId, double alpha) {
            String id = required(familyId, "FAMILY_ID");
            if (!(alpha > 0.0d) || alpha > totalAlpha)
                throw new IllegalArgumentException("R929_FAMILY_ALPHA_INVALID");
            Double existing = familyAlpha.get(id);
            if (existing != null) {
                if (Double.doubleToLongBits(existing.doubleValue()) != Double.doubleToLongBits(alpha))
                    throw new IllegalStateException("R929_FAMILY_ALPHA_IMMUTABLE");
                return;
            }
            if (allocated + alpha > totalAlpha + 1.0e-12d)
                throw new IllegalStateException("R929_ALPHA_BUDGET_EXCEEDED");
            familyAlpha.put(id, Double.valueOf(alpha));
            allocated += alpha;
        }

        public synchronized Hypothesis registerHypothesis(String hypothesisId, String familyId,
                String structuralHash, String purpose) {
            String id = required(hypothesisId, "HYPOTHESIS_ID");
            Hypothesis existing = hypotheses.get(id);
            if (existing != null) {
                if (!existing.structuralHash.equals(requireHash(
                        structuralHash, "HYPOTHESIS_STRUCTURAL_HASH")))
                    throw new IllegalStateException("R929_HYPOTHESIS_ID_REUSED");
                return existing;
            }
            Double alpha = familyAlpha.get(required(familyId, "FAMILY_ID"));
            if (alpha == null) throw new IllegalStateException("R929_FAMILY_NOT_REGISTERED");
            int familyCount = 0;
            for (Hypothesis item : hypotheses.values())
                if (item.statisticalFamilyId.equals(familyId)) familyCount++;
            // A pre-registered geometric sequence keeps the sum within the family allocation even
            // when additional children are appended: alpha/2, alpha/4, alpha/8, ...
            double assigned = alpha.doubleValue() / Math.pow(2.0d, familyCount + 1.0d);
            Hypothesis created = new Hypothesis(id, familyId, structuralHash, purpose,
                    assigned, hypotheses.size());
            hypotheses.put(id, created);
            return created;
        }

        public synchronized void registerLook(String hypothesisId, int lookId) {
            if (!hypotheses.containsKey(hypothesisId) || lookId <= 0)
                throw new IllegalArgumentException("R929_LOOK_INVALID");
            Set<Integer> registered = looks.get(hypothesisId);
            if (registered == null) {
                registered = new LinkedHashSet<Integer>();
                looks.put(hypothesisId, registered);
            }
            if (!registered.add(Integer.valueOf(lookId)))
                throw new IllegalStateException("R929_DUPLICATE_LOOK");
        }

        public synchronized TestResult conservativeResult(
                String hypothesisId, int lookId, double rawP) {
            Hypothesis hypothesis = hypotheses.get(required(hypothesisId, "HYPOTHESIS_ID"));
            if (hypothesis == null || !finiteProbability(rawP))
                throw new IllegalArgumentException("R929_TEST_INPUT_INVALID");
            Set<Integer> registered = looks.get(hypothesisId);
            if (registered == null || !registered.contains(Integer.valueOf(lookId)))
                throw new IllegalStateException("R929_LOOK_NOT_PREREGISTERED");
            // Spend the hypothesis allocation over a fixed geometric look sequence.
            double lookAlpha = hypothesis.allocatedAlpha / Math.pow(2.0d, lookId);
            double adjusted = Math.min(1.0d, rawP / Math.max(1.0e-300d, lookAlpha));
            return new TestResult(hypothesis, lookId, rawP, adjusted, -1.0d,
                    rawP <= lookAlpha, "FIXED_BATCH_GEOMETRIC_BONFERRONI");
        }

        public synchronized List<TestResult> benjaminiHochbergDiagnostic(
                Map<String, Double> rawPByHypothesis) {
            if (rawPByHypothesis == null || rawPByHypothesis.isEmpty())
                return Collections.emptyList();
            ArrayList<PEntry> entries = new ArrayList<PEntry>();
            for (Map.Entry<String, Double> item : rawPByHypothesis.entrySet()) {
                Hypothesis hypothesis = hypotheses.get(item.getKey());
                if (hypothesis == null || item.getValue() == null
                        || !finiteProbability(item.getValue().doubleValue()))
                    throw new IllegalArgumentException("R929_FDR_INPUT_INVALID");
                entries.add(new PEntry(hypothesis, item.getValue().doubleValue()));
            }
            Collections.sort(entries, new Comparator<PEntry>() {
                @Override public int compare(PEntry a, PEntry b) {
                    int by = Double.compare(a.rawP, b.rawP);
                    return by != 0 ? by : a.hypothesis.hypothesisId.compareTo(
                            b.hypothesis.hypothesisId);
                }
            });
            int m = entries.size();
            double running = 1.0d;
            double[] q = new double[m];
            for (int i = m - 1; i >= 0; i--) {
                double candidate = entries.get(i).rawP * m / (double) (i + 1);
                running = Math.min(running, candidate);
                q[i] = Math.min(1.0d, running);
            }
            ArrayList<TestResult> result = new ArrayList<TestResult>();
            for (int i = 0; i < m; i++) {
                PEntry entry = entries.get(i);
                result.add(new TestResult(entry.hypothesis, 0, entry.rawP, -1.0d, q[i],
                        false, "BH_FDR_DIAGNOSTIC_NOT_DEFAULT_AUTHORITY"));
            }
            return Collections.unmodifiableList(result);
        }

        public synchronized double allocatedAlpha() { return allocated; }
        public synchronized int hypothesisCount() { return hypotheses.size(); }
        public double totalAlpha() { return totalAlpha; }
    }

    private static final class PEntry {
        final Hypothesis hypothesis;
        final double rawP;
        PEntry(Hypothesis hypothesis, double rawP) {
            this.hypothesis = hypothesis;
            this.rawP = rawP;
        }
    }

    public static final class TestResult {
        public final Hypothesis hypothesis;
        public final int lookId;
        public final double rawP;
        public final double fwerAdjustedP;
        public final double fdrQ;
        public final boolean pass;
        public final String protocol;
        public final String resultHash;

        TestResult(Hypothesis hypothesis, int lookId, double rawP, double fwerAdjustedP,
                double fdrQ, boolean pass, String protocol) {
            this.hypothesis = hypothesis;
            this.lookId = lookId;
            this.rawP = rawP;
            this.fwerAdjustedP = fwerAdjustedP;
            this.fdrQ = fdrQ;
            this.pass = pass;
            this.protocol = protocol;
            this.resultHash = sha256(hypothesis.hypothesisHash + '|' + lookId + '|' + rawP
                    + '|' + fwerAdjustedP + '|' + fdrQ + '|' + (pass ? 1 : 0) + '|' + protocol);
        }
    }

    /** Fails all downstream artifacts after any parent, schema or stage identity changes. */
    public static void requireDependencies(
            List<ArtifactDependency> expected, Map<String, ArtifactDependency> actualByRole) {
        if (expected == null || actualByRole == null)
            throw new IllegalArgumentException("R929_DEPENDENCY_SET_REQUIRED");
        if (expected.size() != actualByRole.size())
            throw new IllegalArgumentException("R929_DEPENDENCY_CARDINALITY");
        for (ArtifactDependency dependency : expected) {
            ArtifactDependency actual = actualByRole.get(dependency.role);
            if (actual == null || !dependency.dependencyHash.equals(actual.dependencyHash))
                throw new IllegalArgumentException("R929_DEPENDENCY_DRIFT:" + dependency.role);
        }
    }

    private static boolean finiteProbability(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value)
                && value >= 0.0d && value <= 1.0d;
    }

    private static String clean(String value) { return value == null ? "" : value.trim(); }

    private static String required(String value, String code) {
        String clean = clean(value);
        if (clean.length() == 0) throw new IllegalArgumentException(code + "_REQUIRED");
        return clean;
    }

    private static String requireHash(String value, String code) {
        String clean = required(value, code);
        if (clean.length() != 64) throw new IllegalArgumentException(code + "_NOT_SHA256");
        for (int i = 0; i < clean.length(); i++) {
            char c = Character.toLowerCase(clean.charAt(i));
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')))
                throw new IllegalArgumentException(code + "_NOT_SHA256");
        }
        return clean.toLowerCase(java.util.Locale.US);
    }

    public static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(64);
            for (byte item : bytes)
                out.append(String.format(java.util.Locale.US, "%02x", item & 255));
            return out.toString();
        } catch (Exception failure) {
            throw new IllegalStateException("R929_SHA256_UNAVAILABLE", failure);
        }
    }
}
