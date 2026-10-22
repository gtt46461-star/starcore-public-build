package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Frozen PIT input. Wall-clock runtime is not part of any model feature. */
public final class V51RuntimeInputR36 {
    public final String baseIssue, targetIssue, historyHash;
    public final List<Models.Draw> history;
    /**
     * The immutable PIT frames are materialized exactly once while the input is frozen.  Older
     * revisions discarded these frames after hashing and immediately rebuilt them in the runtime,
     * doubling the live history graph at the worst possible point on a 256 MiB heap.
     */
    private final V51Models.DrawFrame[] frozenFrames;

    private V51RuntimeInputR36(
            String base,
            String target,
            String hash,
            List<Models.Draw> history,
            V51Models.DrawFrame[] frames) {
        baseIssue = base;
        targetIssue = target;
        historyHash = hash;
        final List<Models.Draw> privateHistory = Collections.unmodifiableList(history);
        // Models.Draw is mutable: a read-only List alone did not make the PIT immutable.
        this.history = Collections.unmodifiableList(new java.util.AbstractList<Models.Draw>() {
            @Override public Models.Draw get(int index) { return privateHistory.get(index).copy(); }
            @Override public int size() { return privateHistory.size(); }
        });
        frozenFrames = frames == null ? new V51Models.DrawFrame[0] : frames.clone();
    }

    public static V51RuntimeInputR36 freeze(
            List<Models.Draw> input, AnalysisSingleFlightR36.Request request) {
        if (request == null || !request.valid())
            throw new IllegalArgumentException("R36_INPUT_REQUEST_INVALID");
        ArrayList<Models.Draw> eligible = new ArrayList<Models.Draw>();
        if (input != null)
            for (int i = 0; i < input.size(); i++) {
                Models.Draw d = input.get(i);
                if (d != null
                        && d.isValid()
                        && d.dirty == 0
                        && TargetIssueResolver.isValid(d.issue)
                        && !TargetIssueResolver.isAfter(d.issue, request.baseIssue)) {
                    eligible.add(d.copy());
                }
            }
        HistoryIntegrityVerifierR815.Result verified =
                HistoryIntegrityVerifierR815.verifyAscending(eligible);
        if (verified.hasHardConflict())
            throw new IllegalStateException("R817_HISTORY_CONFLICT|" + verified.summary());
        if (verified.validRows == 0 || !request.baseIssue.equals(verified.maxIssue))
            throw new IllegalStateException("R36_HISTORY_MAX_NOT_BASE");

        /* The device failure contained a recent usable suffix plus an older gap. R815 computed the
         * complete 100/1200 universe first and then rejected it solely because gapCount covered the
         * entire 240-row prefix. Freeze the exact recent suffix before any heavy allocation. */
        List<Models.Draw> suffix = verified.contiguousSuffixAscending();
        if (suffix.size() < AppConfig.R815_MIN_CANONICAL_HISTORY)
            throw new IllegalStateException(
                    "R817_HISTORY_SUFFIX_LT_12|" + verified.summary());
        int from = Math.max(0, suffix.size() - R36AnalysisHistoryPolicyR364.MAX_FROZEN_HISTORY);
        ArrayList<Models.Draw> frozen = new ArrayList<Models.Draw>();
        for (int i = from; i < suffix.size(); i++) frozen.add(suffix.get(i).copy());
        V51Models.DrawFrame[] frames = new V51Models.DrawFrame[frozen.size()];
        for (int i = 0; i < frozen.size(); i++) {
            frames[i] = V51DrawAdapter.convert(frozen.get(i));
            if (frames[i] == null)
                throw new IllegalStateException(
                        "R364_HISTORY_FRAME_INVALID_" + safe(frozen.get(i).issue));
        }
        return new V51RuntimeInputR36(
                request.baseIssue,
                request.targetIssue,
                V51PermutationFeatureEngine.historyHash(frames),
                frozen,
                frames);
    }

    /** DrawFrame is deeply immutable; cloning only the reference array preserves the freeze. */
    V51Models.DrawFrame[] frozenFramesR36() {
        return frozenFrames.clone();
    }

    private static String safe(String x) {
        return x == null ? "" : x;
    }
}
