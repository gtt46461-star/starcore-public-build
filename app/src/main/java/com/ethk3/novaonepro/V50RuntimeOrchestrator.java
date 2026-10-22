package com.ethk3.novaonepro;

import android.database.Cursor;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Pure V50 active runtime. Its call graph has no legacy probability, lane, authorization, rescue,
 * diagnostic or Trade writer.
 */
public final class V50RuntimeOrchestrator {
    private final V50NumberFeatureEngine featureEngine = new V50NumberFeatureEngine();
    private final V50StateTrajectoryEngine stateEngine = new V50StateTrajectoryEngine();
    private final V50MixtureOfExpertsEngine mixtureEngine = new V50MixtureOfExpertsEngine();
    private final V50PositionActionBuilder actionBuilder = new V50PositionActionBuilder();
    private final V50ActionRankerEngine ranker = new V50ActionRankerEngine();
    private final V50SelectedActionCalibrationEngine calibration =
            new V50SelectedActionCalibrationEngine();
    private final V50DomainCertificateEngine domainEngine = new V50DomainCertificateEngine();
    private final V50UncertaintyEngine uncertaintyEngine = new V50UncertaintyEngine();
    private final V50HypothesisGraphEngine hypothesisEngine = new V50HypothesisGraphEngine();
    private final V50CounterEvidenceEngine counterEngine = new V50CounterEvidenceEngine();
    private final V50TrueAblationEngine ablationEngine = new V50TrueAblationEngine();
    private final V50AtomicDecisionLedgerEngine ledgerEngine = new V50AtomicDecisionLedgerEngine();
    private final DecisionGateEngine decisionGate = new DecisionGateEngine();

    public V50Models.RuntimeResult run(
            DbStore store, List<Models.Draw> input, long serviceGeneration) {
        long started = System.currentTimeMillis();
        ArrayList<Models.Draw> allHistory = clean(input);
        int continuousCount = recentContinuousCount(allHistory);
        ArrayList<Models.Draw> history = continuousSuffix(allHistory, continuousCount);
        if (store == null || history.size() < 12) {
            return failed(null, "V50_HISTORY_LT_12_NO_PREDICTION", started);
        }
        boolean fullHistoryReady =
                continuousCount >= HistoricalDrawImportEngine.V50_RUNTIME_MINIMUM_ROWS;
        Models.Draw latest = history.get(history.size() - 1);
        TargetIssueResolver.Resolution issue = TargetIssueResolver.next(latest.issue);
        if (!issue.valid) return failed(null, issue.detail, started);
        String historyHash = historyHash(history);
        long freshnessBase = latest.fetchTime > 0L ? latest.fetchTime : latest.openTime;
        long now = System.currentTimeMillis();
        V50Models.PredictionContext context =
                new V50Models.PredictionContext(
                        issue.baseIssue,
                        issue.targetIssue,
                        latest.issue,
                        historyHash,
                        now,
                        freshnessBase <= 0L ? 0L : Math.max(0L, now - freshnessBase),
                        serviceGeneration,
                        V50BuildManifest.FEATURE_SCHEMA,
                        V50BuildManifest.COHORT_ID);
        V50PerformanceSpanLedger performance = new V50PerformanceSpanLedger(context.targetIssue);
        V50PerformanceSpanLedger.Span root = performance.start("V50RuntimeOrchestrator", null);
        V50Models.NumberFeatureSnapshot features = null;
        V50Models.StateTrajectorySnapshot[] states = null;
        V50MixtureOfExpertsEngine.FusionResult fusion = null;
        V50Models.PositionAction[] actions = null;
        V50Models.SelectedActionEnvelope selected = null;
        V50Models.AuthorizedDecision authorized = null;
        V50Models.DomainCertificate certificate = null;
        V50Models.UncertaintyBreakdown uncertainty = null;
        V50CompiledActionContract compiled = null;
        String status = "V50_RUNTIME_STARTED";
        try {
            store.setState("v50_runtime_history_rows", String.valueOf(history.size()));
            store.setState("v50_runtime_history_total_rows", String.valueOf(allHistory.size()));
            store.setState(
                    "v50_runtime_history_continuity",
                    fullHistoryReady ? "CONTIGUOUS_RECENT_96" : "GAPPED_OR_LT_96");
            store.setState(
                    "v50_runtime_history_mode",
                    fullHistoryReady ? "FULL_PIT_HISTORY" : "COLD_START_SHADOW_ONLY");
            V50Repository.ReadyReport ready = V50Repository.ensure(store);
            if (!ready.ready)
                throw new RuntimeFailure(
                        V50Enums.RejectCode.LEDGER_INCOMPLETE,
                        ready.integrity == null
                                ? "V50_DATABASE_NOT_READY"
                                : ready.integrity.detail);

            V50PerformanceSpanLedger.Span featureSpan =
                    performance.start(V50BuildManifest.NUMBER_FEATURE_OWNER, root);
            features = featureEngine.build(context, history);
            boolean featureOk = features != null && features.hasExactly100Rows();
            performance.finish(
                    featureSpan, root, featureOk, featureOk ? "" : "FEATURE_ROWS_NOT_100");
            if (!featureOk)
                throw new RuntimeFailure(
                        V50Enums.RejectCode.DATA_INCOMPLETE, "V50_FEATURE_ROWS_NOT_100");

            V50PerformanceSpanLedger.Span stateSpan =
                    performance.start(V50BuildManifest.STATE_OWNER, root);
            states = stateEngine.build(features, history);
            boolean stateOk = validStates(states);
            performance.finish(stateSpan, root, stateOk, stateOk ? "" : "STATE_ROWS_NOT_10");
            if (!stateOk)
                throw new RuntimeFailure(
                        V50Enums.RejectCode.DATA_INCOMPLETE, "V50_STATE_ROWS_NOT_10");

            V50PerformanceSpanLedger.Span expertSpan =
                    performance.start(V50BuildManifest.NUMBER_PROBABILITY_OWNER, root);
            fusion = mixtureEngine.fuse(features, states, history, store);
            boolean probabilityOk = validProbability(fusion.numberProbability);
            performance.finish(
                    expertSpan, root, probabilityOk, probabilityOk ? "" : "NUMBER_P_INVALID");
            if (!probabilityOk)
                throw new RuntimeFailure(
                        V50Enums.RejectCode.CONTRACT_MISMATCH, "V50_NUMBER_P_INVALID");

            V50PerformanceSpanLedger.Span rankSpan =
                    performance.start(V50BuildManifest.ACTION_RANK_OWNER, root);
            V50Models.PositionAction[] unranked =
                    actionBuilder.build(
                            context, fusion.numberProbability, features, states, fusion.consensus);
            actions = ranker.rank(unranked);
            V50Models.PositionAction rankOne = ranker.rankOne(actions);
            if (rankOne == null) {
                performance.finish(rankSpan, root, false, "RANK1_MISSING");
                throw new RuntimeFailure(
                        V50Enums.RejectCode.CONTRACT_MISMATCH, "V50_RANK1_MISSING");
            }
            double selectedHitP = calibration.calibrate(rankOne);
            performance.finish(rankSpan, root, true, "");

            V50Models.StateTrajectorySnapshot selectedState = states[rankOne.position - 1];
            V50Models.EffectiveConsensus selectedConsensus = fusion.consensus[rankOne.position - 1];
            certificate = domainEngine.certify(store, selectedState);
            boolean teacherReady = teacherReady(fusion.experts, rankOne.position);
            FingerprintEvidence fingerprint =
                    fingerprintEvidence(store, rankOne.position, rankOne.fingerprint);
            V50HypothesisGraphEngine.Result hypothesis =
                    hypothesisEngine.build(rankOne, selectedState, selectedConsensus, teacherReady);
            V50CounterEvidenceEngine.Result counter =
                    counterEngine.evaluate(
                            rankOne,
                            selectedState,
                            selectedConsensus,
                            teacherReady,
                            fingerprint.runLength,
                            fingerprint.residual);
            uncertainty =
                    uncertaintyEngine.evaluate(
                            rankOne,
                            selectedConsensus,
                            selectedState,
                            certificate,
                            V50SelectedCalibrationArtifact.OOS_ECE);
            V50SelectiveRiskController.Result policy =
                    LanePolicyCompiler.compileV50(
                            selectedHitP,
                            rankOne,
                            certificate,
                            selectedConsensus,
                            uncertainty,
                            counter.assessment);
            if (!fullHistoryReady) {
                policy =
                        new V50SelectiveRiskController.Result(
                                V50Enums.Lane.SHADOW,
                                false,
                                V50Enums.RejectCode.DATA_INCOMPLETE,
                                V50SelectivePolicyArtifact.GLOBAL_SELECTED_P_THRESHOLD,
                                "INCOMPLETE_RECENT_HISTORY_"
                                        + history.size()
                                        + "_CONTINUITY_REQUIRED_"
                                        + HistoricalDrawImportEngine.V50_RUNTIME_MINIMUM_ROWS);
            }
            V50Models.SelectedActionEnvelope draft =
                    new V50Models.SelectedActionEnvelope(
                            context,
                            rankOne,
                            selectedHitP,
                            uncertainty.total,
                            certificate.status,
                            policy.lane,
                            hypothesis.primary,
                            hypothesis.secondary,
                            hypothesis.tertiary,
                            counter.maximumCounterEvidence,
                            hypothesis.necessaryCondition,
                            counter.falsifierCondition,
                            "");
            String contractHash = V50PredictionContract.compileHash(draft);
            selected =
                    new V50Models.SelectedActionEnvelope(
                            context,
                            rankOne,
                            selectedHitP,
                            uncertainty.total,
                            certificate.status,
                            policy.lane,
                            hypothesis.primary,
                            hypothesis.secondary,
                            hypothesis.tertiary,
                            counter.maximumCounterEvidence,
                            hypothesis.necessaryCondition,
                            counter.falsifierCondition,
                            contractHash);
            V50TrueAblationEngine.Result ablation =
                    ablationEngine.recompute(
                            context, features, states, fusion.experts, fusion.reliability, rankOne);
            String stateHash = stateHash(states);
            String expertHash = expertHash(fusion.experts);
            String actionHash = actionHash(actions);
            compiled =
                    new V50CompiledActionContract(
                            selected,
                            policy,
                            certificate,
                            uncertainty,
                            counter,
                            features.checksum,
                            stateHash,
                            expertHash,
                            fusion.checksum,
                            actionHash);

            V50PerformanceSpanLedger.Span gateSpan =
                    performance.start(V50BuildManifest.BET_OWNER, root);
            authorized = decisionGate.decideV50(store, compiled);
            boolean authorizationOk =
                    authorized != null
                            && DecisionAuthorizationEngine.verifyV50(authorized, compiled);
            performance.finish(
                    gateSpan,
                    root,
                    authorizationOk,
                    authorizationOk ? "" : "AUTHORIZATION_INVALID");
            if (!authorizationOk)
                throw new RuntimeFailure(
                        V50Enums.RejectCode.AUTHORIZATION_FAILED, "V50_AUTHORIZATION_INVALID");

            String hypothesisBlob = hypothesisBlob(hypothesis, counter, ablation, compiled);
            String preSaveStatus =
                    authorized.action.name()
                            + '|'
                            + authorized.rejectCode.name()
                            + '|'
                            + (fullHistoryReady ? "FULL_PIT_HISTORY" : "COLD_START_SHADOW_ONLY")
                            + '|'
                            + V50BuildManifest.RELEASE_STATE;
            V50Models.RuntimeResult frozenRuntime =
                    new V50Models.RuntimeResult(
                            true,
                            context,
                            features,
                            states,
                            fusion.experts,
                            fusion.numberProbability,
                            fusion.consensus,
                            actions,
                            selected,
                            authorized,
                            certificate,
                            uncertainty,
                            compiled.checksum,
                            preSaveStatus,
                            Math.max(0L, System.currentTimeMillis() - started),
                            "");
            Models.Decision legacyDecision = decisionGate.toLegacyDecisionV50(frozenRuntime);
            V50PerformanceSpanLedger.Span saveSpan =
                    performance.start("V50AtomicDecisionLedgerEngine", root);
            V50AtomicDecisionLedgerEngine.Result saved =
                    ledgerEngine.persist(
                            store,
                            context,
                            features,
                            states,
                            fusion,
                            actions,
                            selected,
                            authorized,
                            hypothesisBlob,
                            legacyDecision);
            performance.finish(
                    saveSpan, root, saved.success, saved.success ? "" : saved.failureCode.name());
            if (!saved.success) {
                authorized = decisionGate.blockV50(compiled, V50Enums.RejectCode.LEDGER_INCOMPLETE);
                throw new RuntimeFailure(V50Enums.RejectCode.LEDGER_INCOMPLETE, saved.detail);
            }
            status =
                    authorized.action.name()
                            + '|'
                            + authorized.rejectCode.name()
                            + '|'
                            + (fullHistoryReady ? "FULL_PIT_HISTORY" : "COLD_START_SHADOW_ONLY")
                            + '|'
                            + V50BuildManifest.RELEASE_STATE;
            performance.finish(root, null, true, "");
            try {
                performance.persist(store);
            } catch (Throwable ignore) {
            }
            long total = Math.max(0L, System.currentTimeMillis() - started);
            String checksum =
                    V50ChecksumEngine.sha256(
                            context.targetIssue
                                    + '|'
                                    + features.checksum
                                    + '|'
                                    + stateHash
                                    + '|'
                                    + fusion.checksum
                                    + '|'
                                    + actionHash
                                    + '|'
                                    + selected.contractHash
                                    + '|'
                                    + authorized.action.name()
                                    + '|'
                                    + authorized.rejectCode.name()
                                    + '|'
                                    + status);
            return new V50Models.RuntimeResult(
                    true,
                    context,
                    features,
                    states,
                    fusion.experts,
                    fusion.numberProbability,
                    fusion.consensus,
                    actions,
                    selected,
                    authorized,
                    certificate,
                    uncertainty,
                    compiled.checksum,
                    status,
                    total,
                    checksum);
        } catch (RuntimeFailure failure) {
            status = failure.code.name() + '|' + failure.getMessage();
        } catch (Throwable error) {
            status =
                    "V50_RUNTIME_ERROR|"
                            + error.getClass().getSimpleName()
                            + ':'
                            + safe(error.getMessage());
        }
        performance.finish(root, null, false, status);
        try {
            performance.persist(store);
        } catch (Throwable ignore) {
        }
        return new V50Models.RuntimeResult(
                false,
                context,
                features,
                states,
                fusion == null ? null : fusion.experts,
                fusion == null ? null : fusion.numberProbability,
                fusion == null ? null : fusion.consensus,
                actions,
                selected,
                authorized,
                certificate,
                uncertainty,
                compiled == null ? "" : compiled.checksum,
                status,
                Math.max(0L, System.currentTimeMillis() - started),
                V50ChecksumEngine.sha256(status));
    }

    private static ArrayList<Models.Draw> clean(List<Models.Draw> input) {
        ArrayList<Models.Draw> out = new ArrayList<Models.Draw>();
        if (input != null) {
            int start = Math.max(0, input.size() - 2880);
            for (int i = start; i < input.size(); i++) {
                Models.Draw draw = input.get(i);
                if (draw != null && draw.isValid() && TargetIssueResolver.isValid(draw.issue))
                    out.add(draw);
            }
        }
        Collections.sort(
                out,
                new Comparator<Models.Draw>() {
                    public int compare(Models.Draw a, Models.Draw b) {
                        return safe(a.issue).compareTo(safe(b.issue));
                    }
                });
        ArrayList<Models.Draw> unique = new ArrayList<Models.Draw>();
        String previous = "";
        for (int i = 0; i < out.size(); i++) {
            if (out.get(i).issue.equals(previous)) continue;
            unique.add(out.get(i));
            previous = out.get(i).issue;
        }
        return unique;
    }

    /**
     * A large bundled seed plus a short live prefix must not masquerade as a complete recent PIT
     * window. Within a day, the last 96 issue suffixes must be consecutive; across a date boundary
     * only a one-day transition is accepted. Incomplete history still produces a visible SHADOW K3.
     */
    static int recentContinuousCount(List<Models.Draw> history) {
        if (history == null || history.size() == 0) return 0;
        int count = 1;
        for (int i = history.size() - 1; i > 0; i--) {
            String previous = safe(history.get(i - 1).issue);
            String current = safe(history.get(i).issue);
            if (previous.length() < 9 || current.length() < 9) break;
            String previousDay = previous.substring(0, 8);
            String currentDay = current.substring(0, 8);
            if (previousDay.equals(currentDay)) {
                long previousSequence = issueSequence(previous);
                long currentSequence = issueSequence(current);
                if (previousSequence < 0L || currentSequence != previousSequence + 1L) {
                    break;
                }
            } else {
                long dayDelta = dayDistance(previousDay, currentDay);
                if (dayDelta != 1L) break;
            }
            count++;
        }
        return count;
    }

    private static ArrayList<Models.Draw> continuousSuffix(
            ArrayList<Models.Draw> history, int count) {
        ArrayList<Models.Draw> out = new ArrayList<Models.Draw>();
        if (history == null || count <= 0) return out;
        int start = Math.max(0, history.size() - count);
        for (int i = start; i < history.size(); i++) out.add(history.get(i));
        return out;
    }

    private static long issueSequence(String issue) {
        try {
            return Long.parseLong(issue.substring(8));
        } catch (Throwable error) {
            return -1L;
        }
    }

    private static long dayDistance(String previous, String current) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd", Locale.US);
            format.setLenient(false);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date first = format.parse(previous);
            Date second = format.parse(current);
            if (first == null || second == null) return -1L;
            return (second.getTime() - first.getTime()) / 86400000L;
        } catch (Throwable error) {
            return -1L;
        }
    }

    private static String historyHash(List<Models.Draw> history) {
        StringBuilder value = new StringBuilder("V50_PIT_HISTORY");
        for (int i = 0; i < history.size(); i++) {
            Models.Draw draw = history.get(i);
            value.append('|').append(draw.issue).append(':');
            for (int p = 1; p <= 10; p++) {
                if (p > 1) value.append(',');
                value.append(draw.getNumberAtPosition(p));
            }
        }
        return V50ChecksumEngine.sha256(value.toString());
    }

    private static boolean validStates(V50Models.StateTrajectorySnapshot[] states) {
        if (states == null || states.length != 10) return false;
        for (int p = 0; p < 10; p++) {
            if (states[p] == null || states[p].position != p + 1) return false;
        }
        return true;
    }

    private static boolean validProbability(double[][] matrix) {
        if (matrix == null || matrix.length != 10) return false;
        for (int p = 0; p < 10; p++) {
            if (!V50PredictionContract.normalized10(matrix[p], 1.0e-8d)) return false;
        }
        return true;
    }

    private static boolean teacherReady(V50Models.ExpertEvidence[] experts, int position) {
        if (experts == null || position < 1 || position > 10) return false;
        for (int i = 0; i < experts.length; i++) {
            if (experts[i] != null
                    && "TEACHER_HIERARCHICAL_POSTERIOR".equals(experts[i].expertId)
                    && experts[i].trust.length >= position) {
                return experts[i].trust[position - 1] > 0.0d;
            }
        }
        return false;
    }

    private static FingerprintEvidence fingerprintEvidence(
            DbStore store, int position, String fingerprint) {
        FingerprintEvidence out = new FingerprintEvidence();
        if (store == null) return out;
        Cursor c = null;
        try {
            c =
                    store.readable()
                            .query(
                                    SchemaContractRegistry.TABLE_ACTION,
                                    new String[] {"fingerprint", "raw_action_p", "actual_hit"},
                                    "position=? AND settled=1",
                                    new String[] {String.valueOf(position)},
                                    null,
                                    null,
                                    "target_issue DESC",
                                    "20");
            double residual = 0.0d;
            int count = 0;
            while (c.moveToNext()) {
                if (!safe(fingerprint).equals(safe(c.getString(0)))) break;
                out.runLength++;
                residual += c.getInt(2) - c.getDouble(1);
                count++;
            }
            out.residual = count == 0 ? 0.0d : residual / count;
        } catch (Throwable ignore) {
            return new FingerprintEvidence();
        } finally {
            if (c != null) c.close();
        }
        return out;
    }

    private static String hypothesisBlob(
            V50HypothesisGraphEngine.Result hypothesis,
            V50CounterEvidenceEngine.Result counter,
            V50TrueAblationEngine.Result ablation,
            V50CompiledActionContract compiled) {
        return "primary="
                + hypothesis.primary
                + "|secondary="
                + hypothesis.secondary
                + "|tertiary="
                + hypothesis.tertiary
                + "|alternative="
                + hypothesis.alternative
                + "|necessary="
                + hypothesis.necessaryCondition
                + "|falsifier="
                + counter.falsifierCondition
                + "|counter="
                + counter.maximumCounterEvidence
                + "|ablation="
                + ablation.frozenBlob
                + "|compiled="
                + compiled.checksum;
    }

    private static String stateHash(V50Models.StateTrajectorySnapshot[] states) {
        String[] values = new String[states.length];
        for (int i = 0; i < states.length; i++) values[i] = states[i].checksum;
        return V50ChecksumEngine.hashStrings("V50_STATES", values);
    }

    private static String expertHash(V50Models.ExpertEvidence[] experts) {
        String[] values = new String[experts == null ? 0 : experts.length];
        for (int i = 0; i < values.length; i++) values[i] = experts[i].checksum;
        return V50ChecksumEngine.hashStrings("V50_EXPERTS", values);
    }

    private static String actionHash(V50Models.PositionAction[] actions) {
        String[] values = new String[actions == null ? 0 : actions.length];
        for (int i = 0; i < values.length; i++) values[i] = actions[i].checksum;
        return V50ChecksumEngine.hashStrings("V50_ACTIONS", values);
    }

    private static V50Models.RuntimeResult failed(
            V50Models.PredictionContext context, String status, long started) {
        return new V50Models.RuntimeResult(
                false,
                context,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "",
                status,
                Math.max(0L, System.currentTimeMillis() - started),
                V50ChecksumEngine.sha256(status));
    }

    private static final class FingerprintEvidence {
        int runLength;
        double residual;
    }

    private static final class RuntimeFailure extends Exception {
        final V50Enums.RejectCode code;

        RuntimeFailure(V50Enums.RejectCode code, String message) {
            super(message);
            this.code = code == null ? V50Enums.RejectCode.DATA_INCOMPLETE : code;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
