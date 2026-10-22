package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

/**
 * Memory-bounded, issue-grouped experiment executor for executable Research formula candidates.
 * It streams exact 1200-row groups, never shuffles issues and never writes Production state.
 */
public final class ResearchExperimentRunnerR811 {
    private static final int MAX_ISSUES_PER_EXPERIMENT = 600;
    private static final double NULL_HIT_RATE = 0.30d;
    private static final double FDR_Q = 0.05d;

    private ResearchExperimentRunnerR811() {}

    /** Executes at most one candidate, so the background sidecar remains cooperative. */
    public static BatchResult runNext(
            DbStore store, String runId, String datasetHash, String dataUntilIssue) {
        if (store == null) return BatchResult.yield("STORE_NULL");
        if (RealtimePressureGateR811.hasActiveCriticalWork())
            return BatchResult.yield("YIELD_REALTIME");
        CandidateRow candidate = nextCandidate(store, runId);
        if (candidate == null) {
            int finalized = finalizeFdrAndStatuses(store, runId);
            return new BatchResult(false, true, finalized, "ALL_RUNTIME_CANDIDATES_EVALUATED");
        }
        List<OutcomePair> slice = streamCandidateSlice(store, candidate, dataUntilIssue);
        if (slice == null) return BatchResult.yield("YIELD_REALTIME_DURING_EXPERIMENT");
        if (!slice.isEmpty()) {
            persistOutcomeSlice(store, candidate, slice);
            return new BatchResult(
                    true,
                    false,
                    slice.size(),
                    "EXPERIMENT_SLICE_CHECKPOINTED|candidate="
                            + candidate.candidateId
                            + "|issues="
                            + slice.size());
        }
        OutcomeLists loaded = loadOutcomes(store, candidate);
        List<Outcome> outcomes = loaded.full;
        List<Outcome> ablated = loaded.ablated;
        if (outcomes.size() < 12 || outcomes.size() != ablated.size())
            return BatchResult.yield("WAIT_COMPLETE_ACTION_LABEL_GROUPS_N_12");
        final List<ExperimentRow> rows = buildRows(candidate, datasetHash, outcomes, ablated);
        if (rows.isEmpty()) return BatchResult.yield("EXPERIMENT_METRIC_BUILD_FAILED");
        persist(store, candidate, rows);
        return new BatchResult(true, false, 1, "EVALUATED:" + candidate.candidateId);
    }

    /**
     * Evaluates complete issue groups until the 250 ms Research budget is nearly consumed, then
     * returns a durable checkpoint. A group is never split and the same query can replay safely.
     */
    private static List<OutcomePair> streamCandidateSlice(
            DbStore store, CandidateRow candidate, String untilIssue) {
        ResearchStrategyDslEngine.Node ast = ResearchStrategyDslEngine.parse(candidate.formula);
        String ablatedFeature = ablationFeature(candidate.formula);
        String last = lastCheckpointIssue(store, candidate.candidateId);
        ArrayList<OutcomePair> output = new ArrayList<OutcomePair>();
        Cursor cursor = null;
        String issue = "";
        int expectedOrdinal = 0;
        Winner full = new Winner(), ablated = new Winner();
        long sliceStarted = TimeSourceR811.monoNowMs(), issueStarted = 0L, peakBytes = 0L;
        FeatureView features = new FeatureView();
        boolean stoppedEarly = false;
        try {
            cursor =
                    store.readable()
                            .rawQuery(
                                    "SELECT l.target_issue,l.global_ordinal,s.rank_score,s.raw_action_p,"
                                            + "s.rank_no,s.meta_blob,l.hit_label FROM"
                                            + " v57_research_action_label_r811 l JOIN "
                                            + V51Repository.ACTION_SPACE
                                            + " s ON s.target_issue=l.target_issue AND s.action_hash=l.action_hash"
                                            + " WHERE l.target_issue<=? AND l.target_issue>?"
                                            + " AND l.target_issue IN(SELECT target_issue FROM"
                                            + " v57_research_action_label_r811 WHERE target_issue<=?"
                                            + " GROUP BY target_issue HAVING COUNT(1)=1200 ORDER BY target_issue DESC"
                                            + " LIMIT "
                                            + MAX_ISSUES_PER_EXPERIMENT
                                            + ") ORDER BY l.target_issue ASC,l.global_ordinal ASC",
                                    new String[] {untilIssue, last, untilIssue});
            while (cursor.moveToNext()) {
                if (RealtimePressureGateR811.hasActiveCriticalWork()) return null;
                String current = cursor.getString(0);
                int ordinal = cursor.getInt(1);
                if (!current.equals(issue)) {
                    if (issue.length() > 0 && expectedOrdinal == 1200 && full.valid() && ablated.valid()) {
                        double latency = elapsedMs(issueStarted);
                        output.add(
                                new OutcomePair(
                                        full.outcome(issue, latency, peakBytes),
                                        ablated.outcome(issue, latency, peakBytes),
                                        ablatedFeature));
                    }
                    if (!output.isEmpty()
                            && (TimeSourceR811.elapsed(sliceStarted)
                                            >= AppConfig.R811_RESEARCH_SLICE_BUDGET_MS - 35L
                                    || output.size()
                                            >= AppConfig.R811_RESEARCH_MAX_CANDIDATES_PER_SLICE))
                    {
                        stoppedEarly = true;
                        break;
                    }
                    issue = current;
                    expectedOrdinal = 0;
                    full.reset();
                    ablated.reset();
                    issueStarted = System.nanoTime();
                    peakBytes = usedHeapBytes();
                }
                if (ordinal != expectedOrdinal) {
                    expectedOrdinal = -1;
                    continue;
                }
                expectedOrdinal++;
                features.set(
                        cursor.getDouble(2),
                        cursor.getDouble(3),
                        cursor.getInt(4),
                        cursor.getString(5));
                double fullScore = ResearchStrategyDslEngine.evaluate(ast, features);
                full.offer(
                        ordinal,
                        fullScore,
                        probability(fullScore),
                        cursor.getInt(6),
                        features.regime());
                double original = features.getPrimitive(ablatedFeature);
                features.setPrimitive(ablatedFeature, 0.0d);
                double ablatedScore = ResearchStrategyDslEngine.evaluate(ast, features);
                features.setPrimitive(ablatedFeature, original);
                ablated.offer(
                        ordinal,
                        ablatedScore,
                        probability(ablatedScore),
                        cursor.getInt(6),
                        features.regime());
                peakBytes = Math.max(peakBytes, usedHeapBytes());
            }
            if (stoppedEarly && !output.isEmpty())
                return Collections.unmodifiableList(output);
            if (issue.length() > 0 && expectedOrdinal == 1200 && full.valid() && ablated.valid()) {
                double latency = elapsedMs(issueStarted);
                output.add(
                        new OutcomePair(
                                full.outcome(issue, latency, peakBytes),
                                ablated.outcome(issue, latency, peakBytes),
                                ablatedFeature));
            }
            return Collections.unmodifiableList(output);
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static String lastCheckpointIssue(DbStore store, String candidateId) {
        Cursor cursor = null;
        try {
            cursor =
                    store.readable()
                            .rawQuery(
                                    "SELECT MAX(target_issue) FROM v57_research_candidate_outcome_r811"
                                            + " WHERE candidate_id=?",
                                    new String[] {candidateId});
            return cursor.moveToFirst() ? safe(cursor.getString(0)) : "";
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static void persistOutcomeSlice(
            final DbStore store,
            final CandidateRow candidate,
            final List<OutcomePair> pairs) {
        store.coordinatorR36()
                .runWrite(
                        "R811_RESEARCH_EXPERIMENT_CHECKPOINT",
                        new DatabaseCoordinator.WriteRunnable() {
                            public void run(SQLiteDatabase db) {
                                long now = TimeSourceR811.wallNowMs();
                                for (int i = 0; i < pairs.size(); i++) {
                                    OutcomePair pair = pairs.get(i);
                                    String canonical =
                                            candidate.candidateHash
                                                    + '|'
                                                    + pair.full.canonical()
                                                    + '|'
                                                    + pair.ablated.canonical()
                                                    + '|'
                                                    + pair.ablatedFeature;
                                    ContentValues values = new ContentValues();
                                    values.put("candidate_id", candidate.candidateId);
                                    values.put("target_issue", pair.full.issue);
                                    values.put("selected_ordinal", pair.full.ordinal);
                                    values.put("score", pair.full.score);
                                    values.put("probability", pair.full.probability);
                                    values.put("hit_label", pair.full.hit);
                                    values.put("regime_id", pair.full.regime);
                                    values.put("ablated_feature", pair.ablatedFeature);
                                    values.put("ablated_ordinal", pair.ablated.ordinal);
                                    values.put("ablated_score", pair.ablated.score);
                                    values.put("ablated_probability", pair.ablated.probability);
                                    values.put("ablated_hit_label", pair.ablated.hit);
                                    values.put("ablated_regime_id", pair.ablated.regime);
                                    values.put("latency_ms", pair.full.latencyMs);
                                    values.put("ram_peak_bytes", pair.full.ramPeakBytes);
                                    values.put("prediction_hash", V51ChecksumEngine.sha256(canonical));
                                    values.put("created_at", now);
                                    db.insertWithOnConflict(
                                            "v57_research_candidate_outcome_r811",
                                            null,
                                            values,
                                            SQLiteDatabase.CONFLICT_IGNORE);
                                }
                            }
                        });
    }

    private static OutcomeLists loadOutcomes(DbStore store, CandidateRow candidate) {
        ArrayList<Outcome> full = new ArrayList<Outcome>();
        ArrayList<Outcome> ablated = new ArrayList<Outcome>();
        Cursor cursor = null;
        try {
            cursor =
                    store.readable()
                            .rawQuery(
                                    "SELECT target_issue,selected_ordinal,score,probability,hit_label,regime_id,"
                                            + "ablated_ordinal,ablated_score,ablated_probability,ablated_hit_label,"
                                            + "ablated_regime_id,latency_ms,ram_peak_bytes FROM"
                                            + " v57_research_candidate_outcome_r811 WHERE candidate_id=?"
                                            + " ORDER BY target_issue ASC",
                                    new String[] {candidate.candidateId});
            while (cursor.moveToNext()) {
                String issue = cursor.getString(0);
                double latency = cursor.getDouble(11);
                long ram = cursor.getLong(12);
                full.add(
                        new Outcome(
                                issue,
                                cursor.getInt(1),
                                cursor.getDouble(2),
                                cursor.getDouble(3),
                                cursor.getInt(4),
                                cursor.getString(5),
                                latency,
                                ram));
                ablated.add(
                        new Outcome(
                                issue,
                                cursor.getInt(6),
                                cursor.getDouble(7),
                                cursor.getDouble(8),
                                cursor.getInt(9),
                                cursor.getString(10),
                                latency,
                                ram));
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return new OutcomeLists(
                Collections.unmodifiableList(full), Collections.unmodifiableList(ablated));
    }

    public static boolean allRuntimeCandidatesEvaluated(DbStore store, String runId) {
        Cursor cursor = null;
        try {
            cursor =
                    store.readable()
                            .rawQuery(
                                    "SELECT COUNT(1) FROM v57_research_candidate_r811 c"
                                            + " WHERE c.run_id=? AND c.runtime_executable=1"
                                            + " AND c.candidate_type='FORMULA' AND NOT EXISTS(SELECT 1 FROM"
                                            + " v57_research_experiment_r811 e WHERE e.candidate_id=c.candidate_id"
                                            + " AND e.split_role='FROZEN_TEST')",
                                    new String[] {runId});
            return cursor.moveToFirst() && cursor.getInt(0) == 0;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static CandidateRow nextCandidate(DbStore store, String runId) {
        Cursor cursor = null;
        try {
            cursor =
                    store.readable()
                            .rawQuery(
                                    "SELECT c.candidate_id,c.artifact_hash,c.canonical_spec,c.seed"
                                            + " FROM v57_research_candidate_r811 c WHERE c.run_id=?"
                                            + " AND c.runtime_executable=1 AND c.candidate_type='FORMULA'"
                                            + " AND c.status NOT LIKE 'REJECT%' AND NOT EXISTS(SELECT 1 FROM"
                                            + " v57_research_experiment_r811 e WHERE e.candidate_id=c.candidate_id"
                                            + " AND e.split_role='FROZEN_TEST') ORDER BY c.candidate_id ASC LIMIT 1",
                                    new String[] {runId});
            return cursor.moveToFirst()
                    ? new CandidateRow(
                            cursor.getString(0),
                            cursor.getString(1),
                            cursor.getString(2),
                            cursor.getLong(3))
                    : null;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    /** One cursor and two scalar winners are retained for each issue; no 1200-row group is held. */
    private static List<Outcome> streamCandidate(
            DbStore store, CandidateRow candidate, String untilIssue, boolean ablation) {
        ResearchStrategyDslEngine.Node ast = ResearchStrategyDslEngine.parse(candidate.formula);
        String ablatedFeature = ablation ? ablationFeature(candidate.formula) : "";
        ArrayList<Outcome> output = new ArrayList<Outcome>();
        Cursor cursor = null;
        String issue = "";
        int expectedOrdinal = 0;
        Winner winner = new Winner();
        long issueStarted = 0L;
        long peakBytes = 0L;
        FeatureView features = new FeatureView();
        try {
            cursor =
                    store.readable()
                            .rawQuery(
                                    "SELECT l.target_issue,l.global_ordinal,s.rank_score,s.raw_action_p,"
                                            + "s.rank_no,s.meta_blob,l.hit_label FROM"
                                            + " v57_research_action_label_r811 l JOIN "
                                            + V51Repository.ACTION_SPACE
                                            + " s ON s.target_issue=l.target_issue AND s.action_hash=l.action_hash"
                                            + " WHERE l.target_issue<=? AND l.target_issue IN(SELECT target_issue"
                                            + " FROM v57_research_action_label_r811 WHERE target_issue<=?"
                                            + " GROUP BY target_issue HAVING COUNT(1)=1200 ORDER BY target_issue DESC"
                                            + " LIMIT "
                                            + MAX_ISSUES_PER_EXPERIMENT
                                            + ") ORDER BY l.target_issue ASC,l.global_ordinal ASC",
                                    new String[] {untilIssue, untilIssue});
            while (cursor.moveToNext()) {
                if (RealtimePressureGateR811.hasActiveCriticalWork()) return null;
                String current = cursor.getString(0);
                int ordinal = cursor.getInt(1);
                if (!current.equals(issue)) {
                    if (issue.length() > 0 && expectedOrdinal == 1200 && winner.valid())
                        output.add(
                                winner.outcome(
                                        issue,
                                        elapsedMs(issueStarted),
                                        peakBytes));
                    issue = current;
                    expectedOrdinal = 0;
                    winner.reset();
                    issueStarted = System.nanoTime();
                    peakBytes = usedHeapBytes();
                }
                if (ordinal != expectedOrdinal) {
                    expectedOrdinal = -1;
                    continue;
                }
                expectedOrdinal++;
                features.set(
                        cursor.getDouble(2),
                        cursor.getDouble(3),
                        cursor.getInt(4),
                        cursor.getString(5));
                double original = features.getPrimitive(ablatedFeature);
                if (ablation) features.setPrimitive(ablatedFeature, 0.0d);
                double score = ResearchStrategyDslEngine.evaluate(ast, features);
                if (ablation) features.setPrimitive(ablatedFeature, original);
                winner.offer(
                        ordinal,
                        score,
                        probability(score),
                        cursor.getInt(6),
                        features.regime());
                peakBytes = Math.max(peakBytes, usedHeapBytes());
            }
            if (issue.length() > 0 && expectedOrdinal == 1200 && winner.valid())
                output.add(winner.outcome(issue, elapsedMs(issueStarted), peakBytes));
            return Collections.unmodifiableList(output);
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static List<ExperimentRow> buildRows(
            CandidateRow candidate,
            String datasetHash,
            List<Outcome> full,
            List<Outcome> ablated) {
        int n = full.size();
        int trainEnd = Math.max(1, Math.min(n - 1, (int) Math.floor(n * 0.8d)));
        ArrayList<ExperimentRow> rows = new ArrayList<ExperimentRow>();
        for (int fold = 1; fold <= 4; fold++) {
            int from = fold * trainEnd / 5;
            int to = (fold + 1) * trainEnd / 5;
            if (from <= 0 || to <= from) continue;
            double threshold = threshold(full, 0, from);
            rows.add(
                    metricRow(
                            candidate,
                            datasetHash,
                            "WF_" + fold,
                            "WF_" + fold,
                            full,
                            from,
                            to,
                            threshold,
                            Calibration.identity(),
                            Double.NaN));
        }
        double frozenThreshold = threshold(full, 0, trainEnd);
        rows.add(
                metricRow(
                        candidate,
                        datasetHash,
                        "FROZEN_TEST",
                        "FROZEN_TEST",
                        full,
                        trainEnd,
                        n,
                        frozenThreshold,
                        Calibration.identity(),
                        Double.NaN));
        rows.add(
                metricRow(
                        candidate,
                        datasetHash,
                        "ABLATION",
                        "ABLATE_" + ablationFeature(candidate.formula),
                        ablated,
                        trainEnd,
                        n,
                        threshold(ablated, 0, trainEnd),
                        Calibration.identity(),
                        Double.NaN));
        Calibration calibration = fitPlatt(full, 0, trainEnd);
        rows.add(
                metricRow(
                        candidate,
                        datasetHash,
                        "CALIBRATION",
                        "PLATT_TRAIN_UNTIL_" + full.get(trainEnd - 1).issue,
                        full,
                        trainEnd,
                        n,
                        frozenThreshold,
                        calibration,
                        Double.NaN));

        ArrayList<ExperimentRow> blocks = new ArrayList<ExperimentRow>();
        double[] coverage = new double[4];
        for (int block = 0; block < 4; block++) {
            int from = trainEnd + block * (n - trainEnd) / 4;
            int to = trainEnd + (block + 1) * (n - trainEnd) / 4;
            ExperimentRow row =
                    metricRow(
                            candidate,
                            datasetHash,
                            "BLOCK_STRESS",
                            "BLOCK_" + (block + 1),
                            full,
                            from,
                            to,
                            frozenThreshold,
                            calibration,
                            Double.NaN);
            blocks.add(row);
            coverage[block] = row.coverage;
        }
        double coverageCv = TimeOrderedExperimentLabEngine.coverageCv(coverage);
        ExperimentRow worstBlock = worst(blocks).withCoverageCv(coverageCv);
        rows.add(worstBlock);

        String[] regimes = new String[] {"STABLE", "VOLATILE", "HIGH_ENTROPY"};
        ArrayList<ExperimentRow> regimeRows = new ArrayList<ExperimentRow>();
        for (int i = 0; i < regimes.length; i++) {
            ExperimentRow row =
                    metricRegimeRow(
                            candidate,
                            datasetHash,
                            regimes[i],
                            full,
                            trainEnd,
                            n,
                            frozenThreshold,
                            calibration);
            if (row.n > 0) regimeRows.add(row);
        }
        rows.add(
                regimeRows.isEmpty()
                        ? ExperimentRow.empty(
                                candidate, datasetHash, "REGIME_STRESS", "NO_REGIME")
                        : worst(regimeRows));
        return Collections.unmodifiableList(rows);
    }

    private static ExperimentRow metricRegimeRow(
            CandidateRow candidate,
            String datasetHash,
            String regime,
            List<Outcome> outcomes,
            int from,
            int to,
            double threshold,
            Calibration calibration) {
        ArrayList<Outcome> filtered = new ArrayList<Outcome>();
        for (int i = from; i < to; i++)
            if (regime.equals(outcomes.get(i).regime)) filtered.add(outcomes.get(i));
        if (filtered.isEmpty())
            return ExperimentRow.empty(candidate, datasetHash, "REGIME_STRESS", regime);
        return metricRow(
                candidate,
                datasetHash,
                "REGIME_STRESS",
                regime,
                filtered,
                0,
                filtered.size(),
                threshold,
                calibration,
                Double.NaN);
    }

    private static ExperimentRow metricRow(
            CandidateRow candidate,
            String datasetHash,
            String role,
            String fold,
            List<Outcome> outcomes,
            int from,
            int to,
            double threshold,
            Calibration calibration,
            double coverageCv) {
        from = Math.max(0, Math.min(from, outcomes.size()));
        to = Math.max(from, Math.min(to, outcomes.size()));
        int eligible = to - from, selected = 0, hit = 0, longest = 0, streak = 0;
        double brier = 0.0d;
        double[] binP = new double[10];
        int[] binN = new int[10], binHit = new int[10];
        ArrayList<Double> latencies = new ArrayList<Double>();
        long ramPeak = 0L;
        for (int i = from; i < to; i++) {
            Outcome outcome = outcomes.get(i);
            latencies.add(Double.valueOf(outcome.latencyMs));
            ramPeak = Math.max(ramPeak, outcome.ramPeakBytes);
            if (outcome.score < threshold) continue;
            selected++;
            hit += outcome.hit;
            if (outcome.hit == 0) {
                streak++;
                longest = Math.max(longest, streak);
            } else streak = 0;
            double probability = calibration.apply(outcome.score, outcome.probability);
            double error = probability - outcome.hit;
            brier += error * error;
            int bin = Math.min(9, Math.max(0, (int) Math.floor(probability * 10.0d)));
            binP[bin] += probability;
            binN[bin]++;
            binHit[bin] += outcome.hit;
        }
        double hitRate = selected == 0 ? 0.0d : hit / (double) selected;
        double coverage = eligible == 0 ? 0.0d : selected / (double) eligible;
        double ece = 0.0d;
        for (int i = 0; i < binN.length; i++)
            if (binN[i] > 0)
                ece +=
                        binN[i]
                                / (double) Math.max(1, selected)
                                * Math.abs(binP[i] / binN[i] - binHit[i] / (double) binN[i]);
        double pValue = binomialUpperNormal(hit, selected, NULL_HIT_RATE);
        String start = eligible == 0 ? "" : outcomes.get(from).issue;
        String end = eligible == 0 ? "" : outcomes.get(to - 1).issue;
        return new ExperimentRow(
                candidate,
                datasetHash,
                role,
                fold,
                start,
                end,
                eligible,
                selected,
                hit,
                hitRate,
                coverage,
                wilsonLower(hit, selected),
                betaLower95(hit, selected),
                selected == 0 ? 1.0d : ece,
                selected == 0 ? 1.0d : brier / selected,
                coverageCv,
                longest,
                percentile95(latencies),
                ramPeak,
                pValue);
    }

    private static void persist(
            final DbStore store,
            final CandidateRow candidate,
            final List<ExperimentRow> rows) {
        store.coordinatorR36()
                .runWrite(
                        "R811_RESEARCH_EXPERIMENT",
                        new DatabaseCoordinator.WriteRunnable() {
                            public void run(SQLiteDatabase db) {
                                long now = TimeSourceR811.wallNowMs();
                                for (int i = 0; i < rows.size(); i++) {
                                    ExperimentRow row = rows.get(i);
                                    ContentValues values = row.values(now);
                                    db.insertWithOnConflict(
                                            "v57_research_experiment_r811",
                                            null,
                                            values,
                                            SQLiteDatabase.CONFLICT_REPLACE);
                                }
                                ContentValues candidateValues = new ContentValues();
                                candidateValues.put("status", "EVALUATED_PENDING_FDR");
                                candidateValues.put("updated_at", now);
                                db.update(
                                        "v57_research_candidate_r811",
                                        candidateValues,
                                        "candidate_id=?",
                                        new String[] {candidate.candidateId});
                            }
                        });
    }

    private static int finalizeFdrAndStatuses(final DbStore store, final String runId) {
        final ArrayList<String> ids = new ArrayList<String>();
        final ArrayList<Double> p = new ArrayList<Double>();
        final ArrayList<ExperimentRowLite> frozen = new ArrayList<ExperimentRowLite>();
        Cursor cursor = null;
        try {
            cursor =
                    store.readable()
                            .rawQuery(
                                    "SELECT c.candidate_id,e.p_value,e.eligible_n,e.selected_n,"
                                            + "e.hit_rate,e.coverage,"
                                            + "e.wilson_lower,e.ece,e.brier,e.coverage_cv FROM"
                                            + " v57_research_candidate_r811 c JOIN"
                                            + " v57_research_experiment_r811 e ON e.candidate_id=c.candidate_id"
                                            + " WHERE c.run_id=? AND e.split_role='FROZEN_TEST'"
                                            + " ORDER BY c.candidate_id ASC",
                                    new String[] {runId});
            while (cursor.moveToNext()) {
                ids.add(cursor.getString(0));
                p.add(Double.valueOf(cursor.getDouble(1)));
                frozen.add(
                        new ExperimentRowLite(
                                cursor.getInt(2),
                                cursor.getInt(3),
                                cursor.getDouble(4),
                                cursor.getDouble(5),
                                cursor.getDouble(6),
                                cursor.getDouble(7),
                                cursor.getDouble(8),
                                cursor.getDouble(9)));
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        double[] values = new double[p.size()];
        for (int i = 0; i < values.length; i++) values[i] = p.get(i).doubleValue();
        final boolean[] passed = TimeOrderedExperimentLabEngine.fdr(values, FDR_Q);
        store.coordinatorR36()
                .runWrite(
                        "R811_RESEARCH_FDR",
                        new DatabaseCoordinator.WriteRunnable() {
                            public void run(SQLiteDatabase db) {
                                long now = TimeSourceR811.wallNowMs();
                                for (int i = 0; i < ids.size(); i++) {
                                    ContentValues experiment = new ContentValues();
                                    experiment.put("fdr_pass", passed[i] ? 1 : 0);
                                    db.update(
                                            "v57_research_experiment_r811",
                                            experiment,
                                            "candidate_id=?",
                                            new String[] {ids.get(i)});
                                    ExperimentRowLite metric = frozen.get(i);
                                    String status =
                                            shadowEligible(metric, passed[i])
                                                    ? "SHADOW_ELIGIBLE"
                                                    : (passed[i]
                                                            ? "SCREENED_RESEARCH_ONLY"
                                                            : "REJECT_FDR");
                                    ContentValues candidate = new ContentValues();
                                    candidate.put("status", status);
                                    candidate.put("updated_at", now);
                                    db.update(
                                            "v57_research_candidate_r811",
                                            candidate,
                                            "candidate_id=?",
                                            new String[] {ids.get(i)});
                                }
                            }
                        });
        return ids.size();
    }

    private static boolean shadowEligible(ExperimentRowLite m, boolean fdr) {
        return fdr
                && m.eligibleN >= 48 /* frozen 20% means at least 240 independent total issues */
                && m.hitRate > NULL_HIT_RATE
                && m.wilsonLower > NULL_HIT_RATE
                && m.coverage >= 0.15d
                && m.ece <= 0.10d
                && m.brier <= 0.30d;
    }

    private static double threshold(List<Outcome> values, int from, int to) {
        int n = Math.max(0, to - from);
        if (n == 0) return -Double.MAX_VALUE;
        double[] scores = new double[n];
        for (int i = 0; i < n; i++) scores[i] = values.get(from + i).score;
        java.util.Arrays.sort(scores);
        int index = Math.max(0, Math.min(n - 1, (int) Math.floor(0.80d * (n - 1))));
        return scores[index];
    }

    private static Calibration fitPlatt(List<Outcome> rows, int from, int to) {
        if (to - from < 20) return Calibration.identity();
        double a = 1.0d, b = 0.0d;
        for (int iteration = 0; iteration < 200; iteration++) {
            double ga = 0.0d, gb = 0.0d;
            for (int i = from; i < to; i++) {
                Outcome row = rows.get(i);
                double x = logit(row.probability);
                double predicted = sigmoid(a * x + b);
                double error = predicted - row.hit;
                ga += error * x;
                gb += error;
            }
            double n = to - from;
            ga = ga / n + 1e-4d * a;
            gb /= n;
            a -= 0.05d * ga;
            b -= 0.05d * gb;
            a = clamp(a, -10.0d, 10.0d);
            b = clamp(b, -10.0d, 10.0d);
        }
        return new Calibration(a, b, false);
    }

    private static String ablationFeature(String formula) {
        String[] order =
                new String[] {
                    "rank_score",
                    "raw_action_p",
                    "third_margin",
                    "stability",
                    "entropy",
                    "window_consensus",
                    "assignment_support"
                };
        for (int i = 0; i < order.length; i++) if (formula.indexOf(order[i]) >= 0) return order[i];
        return "rank_score";
    }

    private static ExperimentRow worst(List<ExperimentRow> rows) {
        ExperimentRow worst = rows.get(0);
        for (int i = 1; i < rows.size(); i++) {
            ExperimentRow next = rows.get(i);
            if (next.wilsonLower < worst.wilsonLower
                    || (next.wilsonLower == worst.wilsonLower && next.hitRate < worst.hitRate))
                worst = next;
        }
        return worst;
    }

    private static double probability(double score) {
        return score >= 0.0d && score <= 1.0d ? score : sigmoid(score);
    }

    private static double wilsonLower(int hit, int n) {
        if (n <= 0) return 0.0d;
        double z = 1.959963984540054d, p = hit / (double) n, z2 = z * z;
        return clamp(
                (p
                                + z2 / (2.0d * n)
                                - z
                                        * Math.sqrt(
                                                (p * (1.0d - p) + z2 / (4.0d * n)) / n))
                        / (1.0d + z2 / n),
                0.0d,
                1.0d);
    }

    /** Jeffreys posterior Beta(hit+0.5, miss+0.5) 2.5% quantile. */
    private static double betaLower95(int hit, int n) {
        if (n <= 0) return 0.0d;
        double a = hit + 0.5d, b = n - hit + 0.5d;
        double lo = 0.0d, hi = 1.0d;
        for (int i = 0; i < 64; i++) {
            double mid = (lo + hi) * 0.5d;
            if (regularizedBeta(mid, a, b) < 0.025d) lo = mid;
            else hi = mid;
        }
        return (lo + hi) * 0.5d;
    }

    private static double regularizedBeta(double x, double a, double b) {
        if (x <= 0.0d) return 0.0d;
        if (x >= 1.0d) return 1.0d;
        double front =
                Math.exp(
                        logGamma(a + b)
                                - logGamma(a)
                                - logGamma(b)
                                + a * Math.log(x)
                                + b * Math.log1p(-x));
        if (x < (a + 1.0d) / (a + b + 2.0d)) return front * betaFraction(x, a, b) / a;
        return 1.0d - front * betaFraction(1.0d - x, b, a) / b;
    }

    private static double betaFraction(double x, double a, double b) {
        final double tiny = 1e-300d;
        double qab = a + b, qap = a + 1.0d, qam = a - 1.0d;
        double c = 1.0d, d = 1.0d - qab * x / qap;
        if (Math.abs(d) < tiny) d = tiny;
        d = 1.0d / d;
        double h = d;
        for (int m = 1; m <= 200; m++) {
            int m2 = 2 * m;
            double aa = m * (b - m) * x / ((qam + m2) * (a + m2));
            d = 1.0d + aa * d;
            if (Math.abs(d) < tiny) d = tiny;
            c = 1.0d + aa / c;
            if (Math.abs(c) < tiny) c = tiny;
            d = 1.0d / d;
            h *= d * c;
            aa = -(a + m) * (qab + m) * x / ((a + m2) * (qap + m2));
            d = 1.0d + aa * d;
            if (Math.abs(d) < tiny) d = tiny;
            c = 1.0d + aa / c;
            if (Math.abs(c) < tiny) c = tiny;
            d = 1.0d / d;
            double delta = d * c;
            h *= delta;
            if (Math.abs(delta - 1.0d) < 3e-14d) break;
        }
        return h;
    }

    private static double logGamma(double x) {
        double[] c =
                new double[] {
                    676.5203681218851d,
                    -1259.1392167224028d,
                    771.32342877765313d,
                    -176.61502916214059d,
                    12.507343278686905d,
                    -0.13857109526572012d,
                    9.9843695780195716e-6d,
                    1.5056327351493116e-7d
                };
        if (x < 0.5d)
            return Math.log(Math.PI) - Math.log(Math.sin(Math.PI * x)) - logGamma(1.0d - x);
        x -= 1.0d;
        double a = 0.99999999999980993d;
        for (int i = 0; i < c.length; i++) a += c[i] / (x + i + 1.0d);
        double t = x + c.length - 0.5d;
        return 0.9189385332046727d + (x + 0.5d) * Math.log(t) - t + Math.log(a);
    }

    private static double binomialUpperNormal(int hit, int n, double p) {
        if (n <= 0) return 1.0d;
        double variance = n * p * (1.0d - p);
        if (variance <= 0.0d) return hit >= n * p ? 0.0d : 1.0d;
        double z = (hit - 0.5d - n * p) / Math.sqrt(variance);
        return clamp(0.5d * erfc(z / Math.sqrt(2.0d)), 0.0d, 1.0d);
    }

    private static double erfc(double x) {
        double z = Math.abs(x);
        double t = 1.0d / (1.0d + 0.3275911d * z);
        double polynomial =
                (((((1.061405429d * t - 1.453152027d) * t) + 1.421413741d) * t
                                        - 0.284496736d)
                                * t
                        + 0.254829592d)
                        * t;
        double erf = 1.0d - polynomial * Math.exp(-z * z);
        return x >= 0.0d ? 1.0d - erf : 1.0d + erf;
    }

    private static double percentile95(List<Double> values) {
        if (values.isEmpty()) return 0.0d;
        double[] copy = new double[values.size()];
        for (int i = 0; i < copy.length; i++) copy[i] = values.get(i).doubleValue();
        java.util.Arrays.sort(copy);
        return copy[Math.min(copy.length - 1, (int) Math.ceil(copy.length * 0.95d) - 1)];
    }

    private static double sigmoid(double value) {
        if (value >= 0.0d) return 1.0d / (1.0d + Math.exp(-Math.min(60.0d, value)));
        double exp = Math.exp(Math.max(-60.0d, value));
        return exp / (1.0d + exp);
    }

    private static double logit(double p) {
        p = clamp(p, 1e-9d, 1.0d - 1e-9d);
        return Math.log(p / (1.0d - p));
    }

    private static double clamp(double x, double lo, double hi) {
        if (Double.isNaN(x) || Double.isInfinite(x)) return lo;
        return Math.max(lo, Math.min(hi, x));
    }

    private static double elapsedMs(long started) {
        return Math.max(0.0d, (System.nanoTime() - started) / 1000000.0d);
    }

    private static long usedHeapBytes() {
        Runtime runtime = Runtime.getRuntime();
        return Math.max(0L, runtime.totalMemory() - runtime.freeMemory());
    }

    private static final class CandidateRow {
        final String candidateId, candidateHash, formula;
        final long seed;

        CandidateRow(String id, String hash, String formula, long seed) {
            candidateId = safe(id);
            candidateHash = safe(hash);
            this.formula = safe(formula);
            this.seed = seed;
        }
    }

    private static final class Winner {
        int ordinal, hit;
        double score, probability;
        String regime;

        Winner() {
            reset();
        }

        void reset() {
            ordinal = -1;
            hit = 0;
            score = -Double.MAX_VALUE;
            probability = 0.0d;
            regime = "";
        }

        void offer(int ordinal, double score, double probability, int hit, String regime) {
            if (this.ordinal < 0 || score > this.score || (score == this.score && ordinal < this.ordinal)) {
                this.ordinal = ordinal;
                this.score = score;
                this.probability = probability;
                this.hit = hit;
                this.regime = regime;
            }
        }

        boolean valid() {
            return ordinal >= 0 && (hit == 0 || hit == 1);
        }

        Outcome outcome(String issue, double latency, long ram) {
            return new Outcome(issue, ordinal, score, probability, hit, regime, latency, ram);
        }
    }

    private static final class Outcome {
        final String issue, regime;
        final int ordinal, hit;
        final double score, probability, latencyMs;
        final long ramPeakBytes;

        Outcome(
                String issue,
                int ordinal,
                double score,
                double probability,
                int hit,
                String regime,
                double latency,
                long ram) {
            this.issue = issue;
            this.ordinal = ordinal;
            this.score = score;
            this.probability = probability;
            this.hit = hit;
            this.regime = regime;
            latencyMs = latency;
            ramPeakBytes = ram;
        }

        String canonical() {
            return issue
                    + '|'
                    + ordinal
                    + '|'
                    + score
                    + '|'
                    + probability
                    + '|'
                    + hit
                    + '|'
                    + regime
                    + '|'
                    + latencyMs
                    + '|'
                    + ramPeakBytes;
        }
    }

    private static final class OutcomePair {
        final Outcome full, ablated;
        final String ablatedFeature;

        OutcomePair(Outcome full, Outcome ablated, String feature) {
            this.full = full;
            this.ablated = ablated;
            ablatedFeature = feature;
        }
    }

    private static final class OutcomeLists {
        final List<Outcome> full, ablated;

        OutcomeLists(List<Outcome> full, List<Outcome> ablated) {
            this.full = full;
            this.ablated = ablated;
        }
    }

    /** Allocation-light read-only feature map for the bounded DSL. */
    private static final class FeatureView implements ResearchStrategyDslEngine.FeatureProvider {
        double rankScore, rawActionP, thirdMargin, entropy, stability, consensus, assignment;

        void set(double rankScore, double raw, int rankNo, String meta) {
            this.rankScore = finite(rankScore);
            rawActionP = finite(raw);
            parseMeta(meta);
        }

        public double value(String name) {
            return getPrimitive(name);
        }

        double getPrimitive(String name) {
            if ("rank_score".equals(name)) return rankScore;
            if ("raw_action_p".equals(name)) return rawActionP;
            if ("third_margin".equals(name)) return thirdMargin;
            if ("entropy".equals(name)) return entropy;
            if ("stability".equals(name)) return stability;
            if ("window_consensus".equals(name)) return consensus;
            if ("assignment_support".equals(name)) return assignment;
            return 0.0d;
        }

        void setPrimitive(String name, double value) {
            if ("rank_score".equals(name)) rankScore = value;
            else if ("raw_action_p".equals(name)) rawActionP = value;
            else if ("third_margin".equals(name)) thirdMargin = value;
            else if ("entropy".equals(name)) entropy = value;
            else if ("stability".equals(name)) stability = value;
            else if ("window_consensus".equals(name)) consensus = value;
            else if ("assignment_support".equals(name)) assignment = value;
        }

        String regime() {
            if (entropy > 0.85d) return "HIGH_ENTROPY";
            return stability < 0.35d ? "VOLATILE" : "STABLE";
        }

        private void parseMeta(String value) {
            thirdMargin = entropy = stability = assignment = consensus = 0.0d;
            if (value == null || value.length() == 0) return;
            int field = 0, start = 0;
            for (int at = 0; at <= value.length() && field <= 20; at++) {
                if (at < value.length() && value.charAt(at) != ',') continue;
                if (field == 1 || field == 2 || field == 3 || field == 15 || field == 20) {
                    double parsed = parseDouble(value, start, at);
                    if (field == 1) thirdMargin = parsed;
                    else if (field == 2) entropy = parsed;
                    else if (field == 3) stability = parsed;
                    else if (field == 15) assignment = parsed;
                    else consensus = parsed;
                }
                field++;
                start = at + 1;
            }
        }

        private static double parseDouble(String value, int from, int to) {
            try {
                return finite(Double.parseDouble(value.substring(from, to)));
            } catch (Throwable ignored) {
                return 0.0d;
            }
        }
    }

    private static final class Calibration {
        final double a, b;
        final boolean identity;

        Calibration(double a, double b, boolean identity) {
            this.a = a;
            this.b = b;
            this.identity = identity;
        }

        static Calibration identity() {
            return new Calibration(1.0d, 0.0d, true);
        }

        double apply(double score, double rawProbability) {
            return identity ? rawProbability : sigmoid(a * logit(rawProbability) + b);
        }
    }

    private static final class ExperimentRowLite {
        final int eligibleN, selectedN;
        final double hitRate, coverage, wilsonLower, ece, brier, coverageCv;

        ExperimentRowLite(
                int eligible,
                int selected,
                double hit,
                double coverage,
                double wilson,
                double ece,
                double brier,
                double cv) {
            eligibleN = eligible;
            selectedN = selected;
            hitRate = hit;
            this.coverage = coverage;
            wilsonLower = wilson;
            this.ece = ece;
            this.brier = brier;
            coverageCv = cv;
        }
    }

    private static final class ExperimentRow {
        final CandidateRow candidate;
        final String datasetHash, role, fold, start, end, resultHash;
        final int n, selected, hit, longestLose;
        final double hitRate, coverage, wilsonLower, betaLower, ece, brier, coverageCv, latencyP95, pValue;
        final long ramPeak;

        ExperimentRow(
                CandidateRow candidate,
                String datasetHash,
                String role,
                String fold,
                String start,
                String end,
                int n,
                int selected,
                int hit,
                double hitRate,
                double coverage,
                double wilson,
                double beta,
                double ece,
                double brier,
                double coverageCv,
                int longest,
                double latency,
                long ram,
                double pValue) {
            this.candidate = candidate;
            this.datasetHash = safe(datasetHash);
            this.role = role;
            this.fold = fold;
            this.start = safe(start);
            this.end = safe(end);
            this.n = n;
            this.selected = selected;
            this.hit = hit;
            this.hitRate = finite(hitRate);
            this.coverage = finite(coverage);
            wilsonLower = finite(wilson);
            betaLower = finite(beta);
            this.ece = finite(ece);
            this.brier = finite(brier);
            this.coverageCv = Double.isNaN(coverageCv) ? -1.0d : finite(coverageCv);
            longestLose = longest;
            latencyP95 = finite(latency);
            ramPeak = ram;
            this.pValue = finite(pValue);
            resultHash = V51ChecksumEngine.sha256(canonical());
        }

        static ExperimentRow empty(
                CandidateRow candidate, String datasetHash, String role, String fold) {
            return new ExperimentRow(
                    candidate,
                    datasetHash,
                    role,
                    fold,
                    "",
                    "",
                    0,
                    0,
                    0,
                    0.0d,
                    0.0d,
                    0.0d,
                    0.0d,
                    1.0d,
                    1.0d,
                    -1.0d,
                    0,
                    0.0d,
                    0L,
                    1.0d);
        }

        ExperimentRow withCoverageCv(double cv) {
            return new ExperimentRow(
                    candidate,
                    datasetHash,
                    role,
                    fold,
                    start,
                    end,
                    n,
                    selected,
                    hit,
                    hitRate,
                    coverage,
                    wilsonLower,
                    betaLower,
                    ece,
                    brier,
                    cv,
                    longestLose,
                    latencyP95,
                    ramPeak,
                    pValue);
        }

        String canonical() {
            return "R811_EXPERIMENT_V1|"
                    + candidate.candidateHash
                    + '|'
                    + datasetHash
                    + '|'
                    + role
                    + '|'
                    + fold
                    + '|'
                    + start
                    + '|'
                    + end
                    + '|'
                    + n
                    + '|'
                    + selected
                    + '|'
                    + hit
                    + '|'
                    + hitRate
                    + '|'
                    + coverage
                    + '|'
                    + wilsonLower
                    + '|'
                    + betaLower
                    + '|'
                    + ece
                    + '|'
                    + brier
                    + '|'
                    + coverageCv
                    + '|'
                    + longestLose
                    + '|'
                    + latencyP95
                    + '|'
                    + ramPeak
                    + '|'
                    + pValue;
        }

        ContentValues values(long now) {
            ContentValues values = new ContentValues();
            values.put(
                    "experiment_id",
                    "R811-EXP-"
                            + V51ChecksumEngine.sha256(candidate.candidateId + '|' + role + '|' + fold)
                                    .substring(0, 32));
            values.put("candidate_id", candidate.candidateId);
            values.put("fold_id", fold);
            values.put("split_role", role);
            values.put("start_issue", start);
            values.put("end_issue", end);
            values.put("n", n);
            values.put("eligible_n", n);
            values.put("selected_n", selected);
            values.put("hit_n", hit);
            values.put("hit_rate", hitRate);
            values.put("coverage", coverage);
            values.put("wilson_lower", wilsonLower);
            values.put("beta_lower", betaLower);
            values.put("ece", ece);
            values.put("brier", brier);
            values.put("coverage_cv", coverageCv);
            values.put("longest_lose", longestLose);
            values.put("latency_p95_ms", latencyP95);
            values.put("ram_peak_bytes", ramPeak);
            values.put("p_value", pValue);
            values.put("fdr_pass", 0);
            values.put("result_hash", resultHash);
            values.put("created_at", now);
            return values;
        }
    }

    public static final class BatchResult {
        public final boolean executed, complete;
        public final int count;
        public final String status;

        BatchResult(boolean executed, boolean complete, int count, String status) {
            this.executed = executed;
            this.complete = complete;
            this.count = count;
            this.status = status;
        }

        static BatchResult yield(String status) {
            return new BatchResult(false, false, 0, status);
        }
    }

    private static double finite(double value) {
        return Double.isNaN(value) || Double.isInfinite(value) ? 0.0d : value;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
