package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Executable Batch-3 training core.
 *
 * <p>The class owns research contracts only. It cannot create a BET, settle an issue or replace
 * DecisionGateEngine. Every learned value is bound to immutable PIT input, a frozen selector and
 * an atomic artifact generation. Missing evidence is explicit and all production checks fail
 * closed.</p>
 */
public final class Batch34TrainingCoreR929 {
    public static final String VERSION = "R929_BATCH34_TRAINING_CORE_V1";
    public static final int FEATURE_COUNT = 96;
    public static final int POSITION_COUNT = 10;
    public static final int ACTIONS_PER_POSITION = 120;
    public static final int ACTION_COUNT = 1200;
    public static final int POSITIVE_ACTIONS_PER_POSITION = 36;
    public static final int POSITIVE_ACTION_COUNT = 360;
    public static final int MIN_EXPANDING_FOLDS = 5;

    private Batch34TrainingCoreR929() {}

    public static final class FeatureSpec {
        public final int index;
        public final String name;
        public final String unit;
        public final double minimum;
        public final double maximum;
        public final int lagIssues;
        public final int windowIssues;
        public final String source;
        public final String missingPolicy;
        public final String costClass;
        public final String version;
        public final boolean postDraw;

        public FeatureSpec(
                int index,
                String name,
                String unit,
                double minimum,
                double maximum,
                int lagIssues,
                int windowIssues,
                String source,
                String missingPolicy,
                String costClass,
                String version,
                boolean postDraw) {
            this.index = index;
            this.name = safe(name);
            this.unit = safe(unit);
            this.minimum = minimum;
            this.maximum = maximum;
            this.lagIssues = lagIssues;
            this.windowIssues = windowIssues;
            this.source = safe(source);
            this.missingPolicy = safe(missingPolicy);
            this.costClass = safe(costClass);
            this.version = safe(version);
            this.postDraw = postDraw;
        }

        public String canonical() {
            return index + "|" + name + "|" + unit + "|" + finite(minimum) + "|"
                    + finite(maximum) + "|" + lagIssues + "|" + windowIssues + "|" + source
                    + "|" + missingPolicy + "|" + costClass + "|" + version + "|" + postDraw;
        }
    }

    public static final class FeatureManifest {
        public final String schema;
        public final String inputStage;
        public final String parentArtifactSha256;
        public final List<FeatureSpec> features;
        public final String manifestSha256;

        public FeatureManifest(
                String schema,
                String inputStage,
                String parentArtifactSha256,
                List<FeatureSpec> features,
                String manifestSha256) {
            this.schema = safe(schema);
            this.inputStage = safe(inputStage);
            this.parentArtifactSha256 = safe(parentArtifactSha256);
            ArrayList<FeatureSpec> copy = new ArrayList<FeatureSpec>();
            if (features != null) copy.addAll(features);
            this.features = Collections.unmodifiableList(copy);
            this.manifestSha256 = safe(manifestSha256);
        }

        public String canonicalWithoutHash() {
            StringBuilder out = new StringBuilder();
            out.append(schema).append('|').append(inputStage).append('|')
                    .append(parentArtifactSha256);
            for (FeatureSpec feature : features) {
                out.append('\n').append(feature == null ? "NULL" : feature.canonical());
            }
            return out.toString();
        }

        public String calculatedSha256() {
            return Batch34EvidenceContractR929.sha256(
                    "D01_FEATURE_MANIFEST|" + canonicalWithoutHash());
        }
    }

    /** Fixed 96-dimensional upstream pre-draw contract. */
    public static FeatureManifest standardFeatureManifest() {
        String[] blocks = {
            "HISTORY_OCCURRENCE", "GAP_RECENCY", "TRANSITION", "TREND", "SHAPE",
            "POSITION_RELATION", "NUMBER_RELATION", "TEACHER_LOGIT", "CAUSAL_CONTEXT",
            "COUNTERFACTUAL", "ATOM_QUALITY", "RUNTIME_CONTEXT"
        };
        int[] windows = {8, 12, 24, 36, 48, 64, 96, 128};
        ArrayList<FeatureSpec> features = new ArrayList<FeatureSpec>();
        int index = 0;
        for (int b = 0; b < blocks.length; b++) {
            for (int slot = 0; slot < 8; slot++) {
                String block = blocks[b];
                features.add(new FeatureSpec(
                        index,
                        String.format(Locale.US, "%s_%02d", block, slot + 1),
                        block.indexOf("LOGIT") >= 0 ? "logit" : "normalized_signal",
                        -32.0d,
                        32.0d,
                        1 + slot % 4,
                        windows[slot],
                        "RAW_PREDRAW_" + block,
                        "EXPLICIT_MASK_AND_REASON",
                        b < 8 ? "FAST" : "BOUNDED",
                        "R929_V1",
                        false));
                index++;
            }
        }
        String parent = Batch34EvidenceContractR929.sha256("R929_RAW_UPSTREAM_STATE_V1");
        FeatureManifest draft = new FeatureManifest(
                Batch34EvidenceContractR929.DATASET_SCHEMA,
                "RAW_HISTORY_BASE_EXPERTS_BEFORE_NUMBER_Q",
                parent,
                features,
                "");
        return new FeatureManifest(
                draft.schema,
                draft.inputStage,
                draft.parentArtifactSha256,
                draft.features,
                draft.calculatedSha256());
    }

    public static Batch34EvidenceContractR929.Check validateFeatureManifest(FeatureManifest manifest) {
        ArrayList<String> failures = new ArrayList<String>();
        if (manifest == null) {
            failures.add("D01_FEATURE_MANIFEST_NULL");
            return check(failures, "");
        }
        if (!Batch34EvidenceContractR929.DATASET_SCHEMA.equals(manifest.schema)) {
            failures.add("D01_FEATURE_SCHEMA_MISMATCH");
        }
        if (!"RAW_HISTORY_BASE_EXPERTS_BEFORE_NUMBER_Q".equals(manifest.inputStage)) {
            failures.add("D02_INPUT_STAGE_NOT_RAW_UPSTREAM");
        }
        if (!Batch34EvidenceContractR929.isSha256(manifest.parentArtifactSha256)) {
            failures.add("D02_PARENT_ARTIFACT_HASH_INVALID");
        }
        if (manifest.features.size() != FEATURE_COUNT) failures.add("D01_FEATURE_COUNT_NOT_96");
        HashSet<Integer> indexes = new HashSet<Integer>();
        HashSet<String> names = new HashSet<String>();
        for (FeatureSpec feature : manifest.features) {
            if (feature == null) {
                failures.add("D01_NULL_FEATURE");
                continue;
            }
            if (!indexes.add(Integer.valueOf(feature.index))) failures.add("D01_DUPLICATE_FEATURE_INDEX");
            if (feature.index < 0 || feature.index >= FEATURE_COUNT) failures.add("D01_FEATURE_INDEX_RANGE");
            if (feature.name.length() == 0 || !names.add(feature.name)) failures.add("D01_DUPLICATE_OR_EMPTY_FEATURE_NAME");
            if (feature.unit.length() == 0 || feature.source.length() == 0
                    || feature.costClass.length() == 0 || feature.version.length() == 0) {
                failures.add("D01_FEATURE_SEMANTIC_FIELD_EMPTY:" + feature.index);
            }
            if (!Double.isFinite(feature.minimum) || !Double.isFinite(feature.maximum)
                    || feature.minimum >= feature.maximum) failures.add("D01_FEATURE_RANGE_INVALID:" + feature.index);
            if (feature.lagIssues < 1 || feature.windowIssues < feature.lagIssues) {
                failures.add("D01_FEATURE_LAG_WINDOW_INVALID:" + feature.index);
            }
            if (!"EXPLICIT_MASK_AND_REASON".equals(feature.missingPolicy)) {
                failures.add("D01_FEATURE_MISSING_POLICY_INVALID:" + feature.index);
            }
            if (feature.postDraw) failures.add("D01_POSTDRAW_FEATURE_PRESENT:" + feature.index);
        }
        for (int i = 0; i < FEATURE_COUNT; i++) {
            if (!indexes.contains(Integer.valueOf(i))) failures.add("D01_FEATURE_INDEX_MISSING:" + i);
        }
        if (!Batch34EvidenceContractR929.isSha256(manifest.manifestSha256)) {
            failures.add("D01_FEATURE_MANIFEST_HASH_INVALID");
        } else if (!manifest.calculatedSha256().equalsIgnoreCase(manifest.manifestSha256)) {
            failures.add("D01_FEATURE_MANIFEST_HASH_MISMATCH");
        }
        return check(failures, manifest.canonicalWithoutHash());
    }

    public static final class PitSnapshot {
        public final String targetIssue;
        public final String maxSourceIssue;
        public final long sourceReceivedAtMillis;
        public final long frozenAtMillis;
        public final long scheduledDrawAtMillis;
        public final int actionRows;
        public final int positiveLabels;
        public final int explicitMissingValues;
        public final int zeroFilledMissingValues;
        public final int postDrawFeatureCount;
        public final String sourceSha256;
        public final String featureManifestSha256;
        public final String actionSurfaceSha256;
        public final boolean targetJoinMatches;
        public final boolean threePositionViewsPresent;
        public final boolean labelsPending;

        public PitSnapshot(
                String targetIssue,
                String maxSourceIssue,
                long sourceReceivedAtMillis,
                long frozenAtMillis,
                long scheduledDrawAtMillis,
                int actionRows,
                int positiveLabels,
                int explicitMissingValues,
                int zeroFilledMissingValues,
                int postDrawFeatureCount,
                String sourceSha256,
                String featureManifestSha256,
                String actionSurfaceSha256,
                boolean targetJoinMatches,
                boolean threePositionViewsPresent,
                boolean labelsPending) {
            this.targetIssue = safe(targetIssue);
            this.maxSourceIssue = safe(maxSourceIssue);
            this.sourceReceivedAtMillis = sourceReceivedAtMillis;
            this.frozenAtMillis = frozenAtMillis;
            this.scheduledDrawAtMillis = scheduledDrawAtMillis;
            this.actionRows = actionRows;
            this.positiveLabels = positiveLabels;
            this.explicitMissingValues = explicitMissingValues;
            this.zeroFilledMissingValues = zeroFilledMissingValues;
            this.postDrawFeatureCount = postDrawFeatureCount;
            this.sourceSha256 = safe(sourceSha256);
            this.featureManifestSha256 = safe(featureManifestSha256);
            this.actionSurfaceSha256 = safe(actionSurfaceSha256);
            this.targetJoinMatches = targetJoinMatches;
            this.threePositionViewsPresent = threePositionViewsPresent;
            this.labelsPending = labelsPending;
        }

        public String canonical() {
            return targetIssue + "|" + maxSourceIssue + "|" + sourceReceivedAtMillis + "|"
                    + frozenAtMillis + "|" + scheduledDrawAtMillis + "|" + actionRows + "|"
                    + positiveLabels + "|" + explicitMissingValues + "|" + zeroFilledMissingValues
                    + "|" + postDrawFeatureCount + "|" + sourceSha256 + "|"
                    + featureManifestSha256 + "|" + actionSurfaceSha256 + "|"
                    + targetJoinMatches + "|" + threePositionViewsPresent + "|" + labelsPending;
        }
    }

    public static Batch34EvidenceContractR929.Check validateSnapshot(PitSnapshot snapshot) {
        ArrayList<String> failures = new ArrayList<String>();
        if (snapshot == null) {
            failures.add("D01_SNAPSHOT_NULL");
            return check(failures, "");
        }
        if (!Batch34EvidenceContractR929.validIssue(snapshot.targetIssue)
                || !Batch34EvidenceContractR929.validIssue(snapshot.maxSourceIssue)
                || Batch34EvidenceContractR929.compareIssue(
                        snapshot.maxSourceIssue, snapshot.targetIssue) >= 0) {
            failures.add("D01_SNAPSHOT_PIT_VIOLATION");
        }
        if (snapshot.sourceReceivedAtMillis < 0L
                || snapshot.frozenAtMillis < snapshot.sourceReceivedAtMillis
                || snapshot.scheduledDrawAtMillis <= snapshot.frozenAtMillis) {
            failures.add("D01_SNAPSHOT_TIME_ORDER_INVALID");
        }
        if (snapshot.actionRows != ACTION_COUNT) failures.add("D01_SNAPSHOT_ACTION_ROWS_NOT_1200");
        if (snapshot.labelsPending) {
            if (snapshot.positiveLabels != 0) failures.add("D01_PENDING_LABEL_WAS_SYNTHESIZED");
        } else if (snapshot.positiveLabels != POSITIVE_ACTION_COUNT) {
            failures.add("D01_POSITIVE_LABEL_COUNT_NOT_360");
        }
        if (snapshot.explicitMissingValues < 0) failures.add("D01_EXPLICIT_MISSING_COUNT_INVALID");
        if (snapshot.zeroFilledMissingValues != 0) failures.add("D01_ZERO_FILLED_MISSING_PRESENT");
        if (snapshot.postDrawFeatureCount != 0) failures.add("D01_POSTDRAW_FEATURE_COUNT_NONZERO");
        if (!Batch34EvidenceContractR929.isSha256(snapshot.sourceSha256)
                || !Batch34EvidenceContractR929.isSha256(snapshot.featureManifestSha256)
                || !Batch34EvidenceContractR929.isSha256(snapshot.actionSurfaceSha256)) {
            failures.add("D01_SNAPSHOT_HASH_INVALID");
        }
        if (!snapshot.targetJoinMatches) failures.add("D01_TARGET_JOIN_MISMATCH");
        if (!snapshot.threePositionViewsPresent) failures.add("D01_THREE_POSITION_VIEWS_MISSING");
        return check(failures, snapshot.canonical());
    }

    public static final class ExpandingFold {
        public final String trainStart;
        public final String trainEnd;
        public final String tuneStart;
        public final String tuneEnd;
        public final String calibrationStart;
        public final String calibrationEnd;
        public final String policyStart;
        public final String policyEnd;
        public final String sealedStart;
        public final String sealedEnd;

        public ExpandingFold(
                String trainStart,
                String trainEnd,
                String tuneStart,
                String tuneEnd,
                String calibrationStart,
                String calibrationEnd,
                String policyStart,
                String policyEnd,
                String sealedStart,
                String sealedEnd) {
            this.trainStart = safe(trainStart);
            this.trainEnd = safe(trainEnd);
            this.tuneStart = safe(tuneStart);
            this.tuneEnd = safe(tuneEnd);
            this.calibrationStart = safe(calibrationStart);
            this.calibrationEnd = safe(calibrationEnd);
            this.policyStart = safe(policyStart);
            this.policyEnd = safe(policyEnd);
            this.sealedStart = safe(sealedStart);
            this.sealedEnd = safe(sealedEnd);
        }

        public String canonical() {
            return trainStart + "|" + trainEnd + "|" + tuneStart + "|" + tuneEnd + "|"
                    + calibrationStart + "|" + calibrationEnd + "|" + policyStart + "|"
                    + policyEnd + "|" + sealedStart + "|" + sealedEnd;
        }
    }

    public static final class SplitPlan {
        public final String sourceSha256;
        public final List<ExpandingFold> folds;
        public final String splitSha256;

        public SplitPlan(String sourceSha256, List<ExpandingFold> folds, String splitSha256) {
            this.sourceSha256 = safe(sourceSha256);
            ArrayList<ExpandingFold> copy = new ArrayList<ExpandingFold>();
            if (folds != null) copy.addAll(folds);
            this.folds = Collections.unmodifiableList(copy);
            this.splitSha256 = safe(splitSha256);
        }

        public String canonicalWithoutHash() {
            StringBuilder out = new StringBuilder(sourceSha256);
            for (ExpandingFold fold : folds) out.append('\n').append(fold == null ? "NULL" : fold.canonical());
            return out.toString();
        }

        public String calculatedSha256() {
            return Batch34EvidenceContractR929.sha256("D03_SPLIT_PLAN|" + canonicalWithoutHash());
        }
    }

    public static Batch34EvidenceContractR929.Check validateSplitPlan(SplitPlan plan) {
        ArrayList<String> failures = new ArrayList<String>();
        if (plan == null) {
            failures.add("D03_SPLIT_PLAN_NULL");
            return check(failures, "");
        }
        if (!Batch34EvidenceContractR929.isSha256(plan.sourceSha256)) {
            failures.add("D03_SPLIT_SOURCE_HASH_INVALID");
        }
        if (plan.folds.size() < MIN_EXPANDING_FOLDS) failures.add("D03_EXPANDING_FOLDS_LT_5");
        String previousTrainEnd = "";
        String previousSealedEnd = "";
        for (int i = 0; i < plan.folds.size(); i++) {
            ExpandingFold fold = plan.folds.get(i);
            if (fold == null) {
                failures.add("D03_NULL_FOLD:" + i);
                continue;
            }
            String[] issues = {
                fold.trainStart, fold.trainEnd, fold.tuneStart, fold.tuneEnd,
                fold.calibrationStart, fold.calibrationEnd, fold.policyStart, fold.policyEnd,
                fold.sealedStart, fold.sealedEnd
            };
            for (String issue : issues) {
                if (!Batch34EvidenceContractR929.validIssue(issue)) {
                    failures.add("D03_FOLD_ISSUE_INVALID:" + i);
                    break;
                }
            }
            if (!(lt(fold.trainStart, fold.trainEnd)
                    && lt(fold.trainEnd, fold.tuneStart)
                    && le(fold.tuneStart, fold.tuneEnd)
                    && lt(fold.tuneEnd, fold.calibrationStart)
                    && le(fold.calibrationStart, fold.calibrationEnd)
                    && lt(fold.calibrationEnd, fold.policyStart)
                    && le(fold.policyStart, fold.policyEnd)
                    && lt(fold.policyEnd, fold.sealedStart)
                    && le(fold.sealedStart, fold.sealedEnd))) {
                failures.add("D03_FOLD_STAGE_ORDER_INVALID:" + i);
            }
            if (previousTrainEnd.length() > 0 && !lt(previousTrainEnd, fold.trainEnd)) {
                failures.add("D03_TRAIN_WINDOW_NOT_EXPANDING:" + i);
            }
            if (previousSealedEnd.length() > 0 && !lt(previousSealedEnd, fold.sealedStart)) {
                failures.add("D03_SEALED_WINDOWS_OVERLAP:" + i);
            }
            previousTrainEnd = fold.trainEnd;
            previousSealedEnd = fold.sealedEnd;
        }
        if (!Batch34EvidenceContractR929.isSha256(plan.splitSha256)) {
            failures.add("D03_SPLIT_HASH_INVALID");
        } else if (!plan.calculatedSha256().equalsIgnoreCase(plan.splitSha256)) {
            failures.add("D03_SPLIT_HASH_MISMATCH");
        }
        return check(failures, plan.canonicalWithoutHash());
    }

    public enum TrialState { RESERVED, RUNNING, FINALIZED_PASS, FINALIZED_FAIL }

    public static final class Trial {
        public final String trialId;
        public final String game;
        public final String schema;
        public final String sourceSha256;
        public final String sealedStart;
        public final String sealedEnd;
        public final String hyperparameterSha256;
        private TrialState state;
        private String resultSha256;

        public Trial(
                String trialId,
                String game,
                String schema,
                String sourceSha256,
                String sealedStart,
                String sealedEnd,
                String hyperparameterSha256) {
            this.trialId = safe(trialId);
            this.game = safe(game);
            this.schema = safe(schema);
            this.sourceSha256 = safe(sourceSha256);
            this.sealedStart = safe(sealedStart);
            this.sealedEnd = safe(sealedEnd);
            this.hyperparameterSha256 = safe(hyperparameterSha256);
            this.state = TrialState.RESERVED;
            this.resultSha256 = "";
        }

        public String globalKey() {
            return Batch34EvidenceContractR929.sha256(
                    "D03_GLOBAL_TRIAL|" + game + "|" + schema + "|" + sourceSha256 + "|"
                            + sealedStart + "|" + sealedEnd);
        }

        public synchronized TrialState state() { return state; }
        public synchronized String resultSha256() { return resultSha256; }
    }

    /** In-memory contract used by host/device stores; duplicate sealed windows cannot be reset by path copies. */
    public static final class TrialRegistry {
        private final Map<String, Trial> byGlobalKey = new LinkedHashMap<String, Trial>();
        private final Map<String, Trial> byId = new LinkedHashMap<String, Trial>();

        public synchronized String reserve(Trial trial) {
            if (trial == null || trial.trialId.length() == 0) return "D03_TRIAL_INVALID";
            if (!Batch34EvidenceContractR929.GAME.equals(trial.game)
                    || !Batch34EvidenceContractR929.DATASET_SCHEMA.equals(trial.schema)
                    || !Batch34EvidenceContractR929.isSha256(trial.sourceSha256)
                    || !Batch34EvidenceContractR929.isSha256(trial.hyperparameterSha256)
                    || !Batch34EvidenceContractR929.validIssue(trial.sealedStart)
                    || !Batch34EvidenceContractR929.validIssue(trial.sealedEnd)
                    || Batch34EvidenceContractR929.compareIssue(trial.sealedStart, trial.sealedEnd) > 0) {
                return "D03_TRIAL_CONTRACT_INVALID";
            }
            Trial oldId = byId.get(trial.trialId);
            if (oldId != null) {
                return oldId.globalKey().equals(trial.globalKey())
                        ? "D03_TRIAL_RESUME_IDEMPOTENT" : "D03_TRIAL_ID_CONFLICT";
            }
            if (byGlobalKey.containsKey(trial.globalKey())) return "D03_SEALED_WINDOW_ALREADY_CONSUMED";
            byId.put(trial.trialId, trial);
            byGlobalKey.put(trial.globalKey(), trial);
            return "D03_TRIAL_RESERVED";
        }

        public synchronized String begin(String trialId) {
            Trial trial = byId.get(safe(trialId));
            if (trial == null) return "D03_TRIAL_NOT_FOUND";
            synchronized (trial) {
                if (trial.state == TrialState.RUNNING) return "D03_TRIAL_RESUME_RUNNING";
                if (trial.state != TrialState.RESERVED) return "D03_TRIAL_BEGIN_REJECTED_" + trial.state;
                trial.state = TrialState.RUNNING;
                return "D03_TRIAL_RUNNING";
            }
        }

        public synchronized String finalizeOnce(String trialId, boolean passed, String resultSha256) {
            Trial trial = byId.get(safe(trialId));
            if (trial == null) return "D03_TRIAL_NOT_FOUND";
            synchronized (trial) {
                if (trial.state != TrialState.RUNNING) return "D03_TRIAL_FINALIZE_REJECTED_" + trial.state;
                if (!Batch34EvidenceContractR929.isSha256(resultSha256)) return "D03_RESULT_HASH_INVALID";
                trial.resultSha256 = resultSha256;
                trial.state = passed ? TrialState.FINALIZED_PASS : TrialState.FINALIZED_FAIL;
                return "D03_TRIAL_FINALIZED_ONCE";
            }
        }

        public synchronized int size() { return byId.size(); }
    }

    public static final class Action {
        public final int ordinal;
        public final int position;
        public final int n1;
        public final int n2;
        public final int n3;
        public final int label;
        public final String actionId;

        Action(int ordinal, int position, int n1, int n2, int n3, int label) {
            this.ordinal = ordinal;
            this.position = position;
            this.n1 = n1;
            this.n2 = n2;
            this.n3 = n3;
            this.label = label;
            this.actionId = String.format(Locale.US, "P%02d-%02d-%02d-%02d", position, n1, n2, n3);
        }

        public String canonical() {
            return ordinal + "|" + position + "|" + n1 + "|" + n2 + "|" + n3 + "|" + label
                    + "|" + actionId;
        }
    }

    public static List<Action> enumerateActions(int[] actualNumberByPosition) {
        if (actualNumberByPosition != null && actualNumberByPosition.length != POSITION_COUNT) {
            throw new IllegalArgumentException("D05_ACTUAL_POSITION_COUNT_NOT_10");
        }
        ArrayList<Action> out = new ArrayList<Action>(ACTION_COUNT);
        int ordinal = 0;
        for (int position = 1; position <= POSITION_COUNT; position++) {
            int actual = actualNumberByPosition == null ? 0 : actualNumberByPosition[position - 1];
            if (actualNumberByPosition != null && (actual < 1 || actual > 10)) {
                throw new IllegalArgumentException("D05_ACTUAL_NUMBER_RANGE");
            }
            for (int n1 = 1; n1 <= 8; n1++) {
                for (int n2 = n1 + 1; n2 <= 9; n2++) {
                    for (int n3 = n2 + 1; n3 <= 10; n3++) {
                        int label = actual == 0 ? -1 : (actual == n1 || actual == n2 || actual == n3 ? 1 : 0);
                        out.add(new Action(ordinal++, position, n1, n2, n3, label));
                    }
                }
            }
        }
        return Collections.unmodifiableList(out);
    }

    public static String actionSurfaceSha256(List<Action> actions) {
        StringBuilder canonical = new StringBuilder();
        if (actions != null) {
            for (Action action : actions) canonical.append(action == null ? "NULL" : action.canonical()).append('\n');
        }
        return Batch34EvidenceContractR929.sha256("D05_ACTION_SURFACE|" + canonical.toString());
    }

    public static Batch34EvidenceContractR929.Check validateActionSurface(List<Action> actions, boolean settled) {
        ArrayList<String> failures = new ArrayList<String>();
        if (actions == null || actions.size() != ACTION_COUNT) {
            failures.add("D05_ACTION_COUNT_NOT_1200");
            return check(failures, actions == null ? "NULL" : String.valueOf(actions.size()));
        }
        HashSet<String> ids = new HashSet<String>();
        int positives = 0;
        int[] perPosition = new int[POSITION_COUNT];
        int[] positiveByPosition = new int[POSITION_COUNT];
        for (int i = 0; i < actions.size(); i++) {
            Action action = actions.get(i);
            if (action == null) {
                failures.add("D05_NULL_ACTION");
                continue;
            }
            if (action.ordinal != i) failures.add("D05_ORDINAL_GAP:" + i);
            if (action.position < 1 || action.position > POSITION_COUNT) failures.add("D05_POSITION_RANGE");
            else perPosition[action.position - 1]++;
            if (!(action.n1 < action.n2 && action.n2 < action.n3
                    && action.n1 >= 1 && action.n3 <= 10)) failures.add("D05_K3_ORDER_INVALID");
            if (!ids.add(action.actionId)) failures.add("D05_DUPLICATE_ACTION_ID");
            if (settled) {
                if (action.label != 0 && action.label != 1) failures.add("D05_LABEL_INVALID");
                if (action.label == 1) {
                    positives++;
                    if (action.position >= 1 && action.position <= POSITION_COUNT) {
                        positiveByPosition[action.position - 1]++;
                    }
                }
            } else if (action.label != -1) failures.add("D05_UNSETTLED_LABEL_NOT_ABSENT");
        }
        for (int p = 0; p < POSITION_COUNT; p++) {
            if (perPosition[p] != ACTIONS_PER_POSITION) failures.add("D05_POSITION_ACTION_COUNT_NOT_120:" + (p + 1));
            if (settled && positiveByPosition[p] != POSITIVE_ACTIONS_PER_POSITION) {
                failures.add("D05_POSITION_POSITIVE_COUNT_NOT_36:" + (p + 1));
            }
        }
        if (settled && positives != POSITIVE_ACTION_COUNT) failures.add("D05_POSITIVE_COUNT_NOT_360");
        return check(failures, actionSurfaceSha256(actions));
    }

    public static final class RankerCandidate {
        public final String candidateId;
        public final String modelFamily;
        public final String artifactSha256;
        public final String inputSurfaceSha256;
        public final int futureIssues;
        public final double rank1;
        public final double top3;
        public final double mrr;
        public final double ndcg;
        public final double pairedLower;
        public final boolean all1200Scored;
        public final boolean hostAndroidParity;
        public final boolean deterministicTieBreak;
        public final boolean resourcePassed;

        public RankerCandidate(
                String candidateId,
                String modelFamily,
                String artifactSha256,
                String inputSurfaceSha256,
                int futureIssues,
                double rank1,
                double top3,
                double mrr,
                double ndcg,
                double pairedLower,
                boolean all1200Scored,
                boolean hostAndroidParity,
                boolean deterministicTieBreak,
                boolean resourcePassed) {
            this.candidateId = safe(candidateId);
            this.modelFamily = safe(modelFamily);
            this.artifactSha256 = safe(artifactSha256);
            this.inputSurfaceSha256 = safe(inputSurfaceSha256);
            this.futureIssues = futureIssues;
            this.rank1 = rank1;
            this.top3 = top3;
            this.mrr = mrr;
            this.ndcg = ndcg;
            this.pairedLower = pairedLower;
            this.all1200Scored = all1200Scored;
            this.hostAndroidParity = hostAndroidParity;
            this.deterministicTieBreak = deterministicTieBreak;
            this.resourcePassed = resourcePassed;
        }

        public String canonical() {
            return candidateId + "|" + modelFamily + "|" + artifactSha256 + "|"
                    + inputSurfaceSha256 + "|" + futureIssues + "|" + finite(rank1) + "|"
                    + finite(top3) + "|" + finite(mrr) + "|" + finite(ndcg) + "|"
                    + finite(pairedLower) + "|" + all1200Scored + "|" + hostAndroidParity + "|"
                    + deterministicTieBreak + "|" + resourcePassed;
        }
    }

    public static final class RankerSelection {
        public final RankerCandidate champion;
        public final boolean baselineRetained;
        public final List<String> failures;
        public final String selectionSha256;

        RankerSelection(RankerCandidate champion, boolean baselineRetained, List<String> failures) {
            this.champion = champion;
            this.baselineRetained = baselineRetained;
            this.failures = Collections.unmodifiableList(new ArrayList<String>(failures));
            this.selectionSha256 = Batch34EvidenceContractR929.sha256(
                    "D04_D05_SELECTION|" + (champion == null ? "NULL" : champion.canonical()) + "|"
                            + baselineRetained + "|" + Batch34EvidenceContractR929.join(failures, ","));
        }
    }

    /** Selects only independently superior candidates; otherwise keeps the declared baseline. */
    public static RankerSelection selectRanker(
            RankerCandidate baseline, List<RankerCandidate> candidates) {
        ArrayList<String> failures = new ArrayList<String>();
        if (!validRankerCandidate(baseline, failures, "BASELINE")) {
            return new RankerSelection(null, true, failures);
        }
        RankerCandidate best = baseline;
        if (candidates != null) {
            for (RankerCandidate candidate : candidates) {
                ArrayList<String> local = new ArrayList<String>();
                if (!validRankerCandidate(candidate, local, "CANDIDATE")) {
                    failures.addAll(local);
                    continue;
                }
                boolean independentlyBetter = candidate.pairedLower > 0.0d
                        && candidate.rank1 >= best.rank1
                        && candidate.ndcg > best.ndcg
                        && candidate.futureIssues >= best.futureIssues;
                if (independentlyBetter) best = candidate;
            }
        }
        boolean retained = best == baseline;
        if (retained) failures.add("D04_NO_INDEPENDENTLY_SUPERIOR_CANDIDATE_BASELINE_RETAINED");
        return new RankerSelection(best, retained, failures);
    }

    private static boolean validRankerCandidate(
            RankerCandidate candidate, List<String> failures, String prefix) {
        if (candidate == null) {
            failures.add("D04_" + prefix + "_NULL");
            return false;
        }
        boolean valid = true;
        if (candidate.candidateId.length() == 0 || candidate.modelFamily.length() == 0) {
            failures.add("D04_" + prefix + "_IDENTITY_EMPTY"); valid = false;
        }
        if (!Batch34EvidenceContractR929.isSha256(candidate.artifactSha256)
                || !Batch34EvidenceContractR929.isSha256(candidate.inputSurfaceSha256)) {
            failures.add("D04_" + prefix + "_HASH_INVALID"); valid = false;
        }
        if (candidate.futureIssues < 1) { failures.add("D04_" + prefix + "_SUPPORT_EMPTY"); valid = false; }
        if (!probability(candidate.rank1) || !probability(candidate.top3)
                || !probability(candidate.mrr) || !probability(candidate.ndcg)
                || !Double.isFinite(candidate.pairedLower)) {
            failures.add("D04_" + prefix + "_METRIC_NONFINITE_OR_RANGE"); valid = false;
        }
        if (!candidate.all1200Scored) { failures.add("D05_NOT_ALL_1200_SCORED"); valid = false; }
        if (!candidate.hostAndroidParity) { failures.add("D05_HOST_ANDROID_PARITY_FAILED"); valid = false; }
        if (!candidate.deterministicTieBreak) { failures.add("D05_TIE_BREAK_NONDETERMINISTIC"); valid = false; }
        if (!candidate.resourcePassed) { failures.add("D04_RESOURCE_BUDGET_FAILED"); valid = false; }
        return valid;
    }

    public static final class SelectedCalibrationModel {
        public final String selectorSha256;
        public final double[] upperRawScore;
        public final double[] probability;
        public final int selectedSupport;
        public final String modelSha256;

        SelectedCalibrationModel(
                String selectorSha256,
                double[] upperRawScore,
                double[] probability,
                int selectedSupport,
                String modelSha256) {
            this.selectorSha256 = selectorSha256;
            this.upperRawScore = upperRawScore.clone();
            this.probability = probability.clone();
            this.selectedSupport = selectedSupport;
            this.modelSha256 = modelSha256;
        }

        public double predict(double rawScore, String runtimeSelectorSha256) {
            if (!selectorSha256.equalsIgnoreCase(safe(runtimeSelectorSha256))) {
                throw new IllegalStateException("D06_SELECTOR_HASH_MISMATCH_RECALIBRATION_REQUIRED");
            }
            if (!Double.isFinite(rawScore)) throw new IllegalArgumentException("D06_RAW_SCORE_NONFINITE");
            if (rawScore <= upperRawScore[0]) return probability[0];
            for (int i = 1; i < upperRawScore.length; i++) {
                if (rawScore <= upperRawScore[i]) {
                    double leftRaw = upperRawScore[i - 1];
                    double rightRaw = upperRawScore[i];
                    if (!(rightRaw > leftRaw)) return probability[i];
                    double fraction = (rawScore - leftRaw) / (rightRaw - leftRaw);
                    double calibrated = probability[i - 1]
                            + fraction * (probability[i] - probability[i - 1]);
                    return Math.max(0.0d, Math.min(1.0d, calibrated));
                }
            }
            return probability[probability.length - 1];
        }

        public String canonicalWithoutHash() {
            return selectorSha256 + "|" + selectedSupport + "|"
                    + Arrays.toString(upperRawScore) + "|" + Arrays.toString(probability);
        }
    }

    /** Pool-adjacent-violators fit on selected actions only. Raw score is never relabelled as p(hit). */
    public static SelectedCalibrationModel fitSelectedCalibration(
            double[] rawSelectedScores, int[] selectedHitLabels, String selectorSha256) {
        if (!Batch34EvidenceContractR929.isSha256(selectorSha256)) {
            throw new IllegalArgumentException("D06_SELECTOR_HASH_INVALID");
        }
        if (rawSelectedScores == null || selectedHitLabels == null
                || rawSelectedScores.length == 0
                || rawSelectedScores.length != selectedHitLabels.length) {
            throw new IllegalArgumentException("D06_SELECTED_CALIBRATION_SAMPLE_INVALID");
        }
        Integer[] order = new Integer[rawSelectedScores.length];
        for (int i = 0; i < order.length; i++) {
            if (!Double.isFinite(rawSelectedScores[i])) throw new IllegalArgumentException("D06_RAW_SCORE_NONFINITE");
            if (selectedHitLabels[i] != 0 && selectedHitLabels[i] != 1) {
                throw new IllegalArgumentException("D06_HIT_LABEL_INVALID");
            }
            order[i] = Integer.valueOf(i);
        }
        Arrays.sort(order, new Comparator<Integer>() {
            public int compare(Integer a, Integer b) {
                int c = Double.compare(rawSelectedScores[a.intValue()], rawSelectedScores[b.intValue()]);
                return c != 0 ? c : Integer.compare(a.intValue(), b.intValue());
            }
        });
        // Equal raw scores are one statistical knot. Keeping them as separate knots would make
        // prediction depend on input row order and could create a zero-width interpolation span.
        double[] groupedUpper = new double[order.length];
        double[] groupedSum = new double[order.length];
        int[] groupedWeight = new int[order.length];
        int groups = 0;
        for (int sorted = 0; sorted < order.length; sorted++) {
            int source = order[sorted].intValue();
            double raw = rawSelectedScores[source];
            if (groups > 0 && Double.compare(raw, groupedUpper[groups - 1]) == 0) {
                groupedSum[groups - 1] += selectedHitLabels[source];
                groupedWeight[groups - 1]++;
            } else {
                groupedUpper[groups] = raw;
                groupedSum[groups] = selectedHitLabels[source];
                groupedWeight[groups] = 1;
                groups++;
            }
        }
        double[] upper = new double[groups];
        double[] sum = new double[groups];
        int[] weight = new int[groups];
        int blocks = 0;
        for (int group = 0; group < groups; group++) {
            upper[blocks] = groupedUpper[group];
            sum[blocks] = groupedSum[group];
            weight[blocks] = groupedWeight[group];
            blocks++;
            while (blocks >= 2
                    && sum[blocks - 2] / weight[blocks - 2] > sum[blocks - 1] / weight[blocks - 1]) {
                upper[blocks - 2] = upper[blocks - 1];
                sum[blocks - 2] += sum[blocks - 1];
                weight[blocks - 2] += weight[blocks - 1];
                blocks--;
            }
        }
        double[] finalUpper = new double[blocks];
        double[] probability = new double[blocks];
        for (int i = 0; i < blocks; i++) {
            finalUpper[i] = upper[i];
            probability[i] = sum[i] / weight[i];
        }
        String canonical = selectorSha256 + "|" + rawSelectedScores.length + "|"
                + Arrays.toString(finalUpper) + "|" + Arrays.toString(probability);
        return new SelectedCalibrationModel(
                selectorSha256,
                finalUpper,
                probability,
                rawSelectedScores.length,
                Batch34EvidenceContractR929.sha256("D06_SELECTED_ISOTONIC|" + canonical));
    }

    public static Batch34EvidenceContractR929.Check validateCalibration(
            SelectedCalibrationModel model, String expectedSelectorSha256) {
        ArrayList<String> failures = new ArrayList<String>();
        if (model == null) {
            failures.add("D06_CALIBRATION_MODEL_NULL");
            return check(failures, "");
        }
        if (!Batch34EvidenceContractR929.isSha256(model.selectorSha256)
                || !model.selectorSha256.equalsIgnoreCase(safe(expectedSelectorSha256))) {
            failures.add("D06_CALIBRATION_SELECTOR_MISMATCH");
        }
        if (model.selectedSupport < 1 || model.upperRawScore.length == 0
                || model.upperRawScore.length != model.probability.length) {
            failures.add("D06_CALIBRATION_SHAPE_INVALID");
        }
        double lastRaw = Double.NEGATIVE_INFINITY;
        double lastP = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < model.upperRawScore.length; i++) {
            if (!Double.isFinite(model.upperRawScore[i]) || model.upperRawScore[i] <= lastRaw) {
                failures.add("D06_RAW_BREAKS_NOT_STRICTLY_INCREASING");
            }
            if (!probability(model.probability[i]) || model.probability[i] < lastP) {
                failures.add("D06_PROBABILITY_NOT_MONOTONIC");
            }
            lastRaw = model.upperRawScore[i];
            lastP = model.probability[i];
        }
        if (!Batch34EvidenceContractR929.isSha256(model.modelSha256)) {
            failures.add("D06_CALIBRATION_HASH_INVALID");
        } else {
            String expectedModelSha256 = Batch34EvidenceContractR929.sha256(
                    "D06_SELECTED_ISOTONIC|" + model.canonicalWithoutHash());
            if (!expectedModelSha256.equalsIgnoreCase(model.modelSha256)) {
                failures.add("D06_CALIBRATION_HASH_MISMATCH");
            }
        }
        return check(failures, model.canonicalWithoutHash());
    }

    public static Batch34RuntimeGateR929.StabilityEvidence evaluateStability(
            int[][] rankedActionOrdinals,
            double[] selectedProbabilities,
            String uncertaintySha256,
            String rankStabilitySha256) {
        if (rankedActionOrdinals == null || selectedProbabilities == null
                || rankedActionOrdinals.length != selectedProbabilities.length
                || rankedActionOrdinals.length == 0) {
            throw new IllegalArgumentException("D07_STABILITY_SAMPLE_INVALID");
        }
        int seeds = rankedActionOrdinals.length;
        int length = rankedActionOrdinals[0].length;
        if (length < 2) throw new IllegalArgumentException("D07_RANKING_TOO_SHORT");
        HashMap<Integer, Integer> topCounts = new HashMap<Integer, Integer>();
        for (int s = 0; s < seeds; s++) {
            if (rankedActionOrdinals[s] == null || rankedActionOrdinals[s].length != length) {
                throw new IllegalArgumentException("D07_RANKING_SHAPE_MISMATCH");
            }
            HashSet<Integer> unique = new HashSet<Integer>();
            for (int value : rankedActionOrdinals[s]) {
                if (!unique.add(Integer.valueOf(value))) throw new IllegalArgumentException("D07_DUPLICATE_RANK_ORDINAL");
            }
            if (!probability(selectedProbabilities[s])) {
                throw new IllegalArgumentException("D07_SELECTED_PROBABILITY_INVALID");
            }
            Integer top = Integer.valueOf(rankedActionOrdinals[s][0]);
            Integer count = topCounts.get(top);
            topCounts.put(top, Integer.valueOf(count == null ? 1 : count.intValue() + 1));
        }
        int consensusTop = rankedActionOrdinals[0][0];
        int consensusCount = 0;
        for (Map.Entry<Integer, Integer> entry : topCounts.entrySet()) {
            if (entry.getValue().intValue() > consensusCount
                    || (entry.getValue().intValue() == consensusCount
                            && entry.getKey().intValue() < consensusTop)) {
                consensusTop = entry.getKey().intValue();
                consensusCount = entry.getValue().intValue();
            }
        }
        double tauSum = 0.0d;
        int pairs = 0;
        for (int a = 0; a < seeds; a++) {
            for (int b = a + 1; b < seeds; b++) {
                tauSum += kendallTau(rankedActionOrdinals[a], rankedActionOrdinals[b]);
                pairs++;
            }
        }
        double[] sortedP = selectedProbabilities.clone();
        Arrays.sort(sortedP);
        double width = percentile(sortedP, 0.975d) - percentile(sortedP, 0.025d);
        double[] displacement = new double[seeds];
        for (int s = 0; s < seeds; s++) {
            int rank = length;
            for (int i = 0; i < length; i++) {
                if (rankedActionOrdinals[s][i] == consensusTop) { rank = i; break; }
            }
            displacement[s] = rank;
        }
        Arrays.sort(displacement);
        return new Batch34RuntimeGateR929.StabilityEvidence(
                seeds,
                consensusCount / (double) seeds,
                pairs == 0 ? 1.0d : tauSum / pairs,
                width,
                percentile(displacement, 0.95d),
                uncertaintySha256,
                rankStabilitySha256,
                allRankingsIdentical(rankedActionOrdinals),
                true);
    }

    private static double kendallTau(int[] left, int[] right) {
        HashMap<Integer, Integer> rightRank = new HashMap<Integer, Integer>();
        for (int i = 0; i < right.length; i++) rightRank.put(Integer.valueOf(right[i]), Integer.valueOf(i));
        long concordant = 0L;
        long discordant = 0L;
        for (int i = 0; i < left.length; i++) {
            Integer ri = rightRank.get(Integer.valueOf(left[i]));
            if (ri == null) continue;
            for (int j = i + 1; j < left.length; j++) {
                Integer rj = rightRank.get(Integer.valueOf(left[j]));
                if (rj == null) continue;
                if (ri.intValue() < rj.intValue()) concordant++; else discordant++;
            }
        }
        long total = concordant + discordant;
        return total == 0L ? 0.0d : (concordant - discordant) / (double) total;
    }

    private static boolean allRankingsIdentical(int[][] rankings) {
        for (int i = 1; i < rankings.length; i++) {
            if (!Arrays.equals(rankings[0], rankings[i])) return false;
        }
        return true;
    }

    private static double percentile(double[] sorted, double q) {
        if (sorted.length == 1) return sorted[0];
        double index = Math.max(0.0d, Math.min(1.0d, q)) * (sorted.length - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);
        if (lower == upper) return sorted[lower];
        double fraction = index - lower;
        return sorted[lower] * (1.0d - fraction) + sorted[upper] * fraction;
    }

    /** Atomic issue-boundary activation. A partial artifact generation can never become current. */
    public static final class AtomicPublicationStore {
        private Batch34EvidenceContractR929.BundleManifest current;
        private Batch34EvidenceContractR929.BundleManifest previous;
        private Batch34EvidenceContractR929.BundleManifest staged;
        private long sequence;

        public synchronized String stage(Batch34EvidenceContractR929.BundleManifest bundle) {
            Batch34EvidenceContractR929.Check check =
                    Batch34EvidenceContractR929.validateBundle(bundle, false);
            if (!check.passed) return "D10_STAGE_REJECTED:" + check.primaryCode();
            staged = bundle;
            return "D10_STAGE_VALIDATED";
        }

        public synchronized String activate(
                String observedCurrentSha256,
                Batch34EvidenceContractR929.AtomicSwitchToken token,
                boolean issueBoundary) {
            if (!issueBoundary) return "D10_ACTIVATION_NOT_AT_ISSUE_BOUNDARY";
            if (staged == null) return "D10_NO_STAGED_BUNDLE";
            String actual = current == null
                    ? Batch34EvidenceContractR929.sha256("D10_EMPTY_BUNDLE") : current.bundleSha256;
            if (!actual.equalsIgnoreCase(safe(observedCurrentSha256))) return "D10_OBSERVED_CURRENT_MISMATCH";
            Batch34EvidenceContractR929.Check check = Batch34EvidenceContractR929.validateSwitch(
                    actual, staged, token, sequence);
            if (!check.passed) return "D10_ACTIVATION_REJECTED:" + check.primaryCode();
            previous = current;
            current = staged;
            staged = null;
            sequence = token.monotonicSequence;
            return "D10_ACTIVATED_ATOMIC_GENERATION";
        }

        public synchronized String rollback(
                String expectedCurrentSha256, long nextSequence, boolean issueBoundary) {
            if (!issueBoundary) return "D10_ROLLBACK_NOT_AT_ISSUE_BOUNDARY";
            if (current == null || previous == null) return "D10_ROLLBACK_HISTORY_MISSING";
            if (!current.bundleSha256.equalsIgnoreCase(safe(expectedCurrentSha256))) {
                return "D10_ROLLBACK_CAS_CONFLICT";
            }
            if (nextSequence <= sequence) return "D10_ROLLBACK_SEQUENCE_REPLAY";
            Batch34EvidenceContractR929.BundleManifest oldCurrent = current;
            current = previous;
            previous = oldCurrent;
            sequence = nextSequence;
            return "D10_ROLLED_BACK_WHOLE_GENERATION";
        }

        public synchronized String currentSha256() {
            return current == null ? "" : current.bundleSha256;
        }

        public synchronized long sequence() { return sequence; }
    }

    private static boolean lt(String a, String b) {
        return Batch34EvidenceContractR929.validIssue(a)
                && Batch34EvidenceContractR929.validIssue(b)
                && Batch34EvidenceContractR929.compareIssue(a, b) < 0;
    }

    private static boolean le(String a, String b) {
        return Batch34EvidenceContractR929.validIssue(a)
                && Batch34EvidenceContractR929.validIssue(b)
                && Batch34EvidenceContractR929.compareIssue(a, b) <= 0;
    }

    private static boolean probability(double value) {
        return Double.isFinite(value) && value >= 0.0d && value <= 1.0d;
    }

    private static Batch34EvidenceContractR929.Check check(
            List<String> failures, String canonical) {
        return new Batch34EvidenceContractR929.Check(failures, VERSION + "|" + safe(canonical));
    }

    private static String finite(double value) {
        return Double.isFinite(value) ? String.format(Locale.US, "%.12g", value) : "NONFINITE";
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
