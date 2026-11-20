package com.ethk3.novaonepro.strategyv2;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/** Consumes only a trace assembled by the pinned-original reader. Never grants qualifications. */
final class DiagnosisProofR33 {
    private static final Pattern HASH = Pattern.compile("[0-9a-f]{64}");
    private DiagnosisProofR33() {}

    private static boolean hash(Object value) {
        return value instanceof String && HASH.matcher((String) value).matches();
    }
    private static boolean text(Object value) {
        return value instanceof String && !((String) value).trim().isEmpty();
    }
    static String classify(Map<?,?> t, String group, String id, Object commit, Object snapshot) {
        if (t.isEmpty()) return "UNCONFIRMED_REPLAY_REQUIRED";
        if (!text(commit) || snapshot == null || !Objects.equals(commit,t.get("sourceCommit"))
                || !Objects.equals(snapshot,t.get("readWatermark"))
                || !group.equals(t.get("group")) || !id.equals(t.get("objectId")))
            return "TRACE_IDENTITY_MISMATCH";
        if ("PERSISTED_OWNER_OBSERVATION".equals(t.get("traceKind"))) {
            if (!DiagnosisTraceReaderR33.validTrace(t)) return "UNCONFIRMED_ORIGINAL_ENVELOPE_INVALID";
            return "EXACT_EXPERT_TASK_ID".equals(t.get("attribution"))
                ? "OBSERVED_EXPERT_OWNER_REASON_REPLAY_UNCONFIRMED"
                : "OBSERVED_SHARED_OWNER_REASON_NOT_OBJECT_ROOT_CAUSE";
        }
        if (!Boolean.TRUE.equals(t.get("originalReadVerified")) || !text(t.get("recordLocator"))
                || !hash(t.get("expectedInputSha256"))
                || !t.get("expectedInputSha256").equals(t.get("actualInputSha256"))
                || !text(t.get("firstFailingCall")) || !text(t.get("reasonCode")))
            return "UNCONFIRMED_ORIGINAL_OR_INPUT_MISSING";
        // A report-object map is not an execution authority. In particular, caller supplied
        // booleans and equal caller supplied hashes cannot prove an isolated numeric rerun.
        // The separate R335 replay executor emits a byte-bound receipt; until its original
        // receipt is verified by a dedicated reader, the app must keep this unconfirmed.
        return "OBSERVED_BLOCKER_REPLAY_UNCONFIRMED";
    }
}
