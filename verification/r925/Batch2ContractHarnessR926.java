package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Host-only deterministic fault gates for B05/B08/B09/B10/B11. */
public final class Batch2ContractHarnessR926 {
    private Batch2ContractHarnessR926() {}

    public static void main(String[] args) {
        fetchFaults();
        learningExactlyOnceIdentity();
        backlogContinuity();
        gateProvenance();
        latencyEvidence();
        System.out.println("BATCH2_CONTRACT_HARNESS_PASS");
    }

    private static void fetchFaults() {
        eq("PAYLOAD_REJECTED_DIRTY_JSON",
                Batch2ContractCoreR926.classifyFetch(true, 200, false, "dirty_json"), "F08 dirty200");
        eq("PAYLOAD_REJECTED_WRONG_GAME",
                Batch2ContractCoreR926.classifyFetch(true, 200, false, "wrong_game"), "F08 wrong game");
        eq("PAYLOAD_REJECTED_NUMBER_PATTERN",
                Batch2ContractCoreR926.classifyFetch(true, 200, false, "number_pattern"), "F08 pattern");
        eq("CONFLICT_QUARANTINED",
                Batch2ContractCoreR926.classifyFetch(true, 200, false, "same_issue_different_outcome"), "F09 conflict");
        eq("OUT_OF_ORDER_REJECTED",
                Batch2ContractCoreR926.classifyFetch(true, 200, false, "out_of_order"), "B08 out of order");
        eq("CLOCK_SKEW_QUARANTINED",
                Batch2ContractCoreR926.classifyFetch(true, 200, false, "clock_skew_quarantined"),
                "B08 clock jump");
        eq("TRANSPORT_TIMEOUT",
                Batch2ContractCoreR926.classifyFetch(false, 0, false, "socketTimeout"), "B08 timeout");
        eq("TRANSPORT_TIMEOUT",
                Batch2ContractCoreR926.classifyFetch(false, 0, false, "Read timed out"), "B08 Android read timeout");
        eq("TRANSPORT_HTTP_503",
                Batch2ContractCoreR926.classifyFetch(false, 503, false, "HTTP_503"), "B08 503");
        eq("TRANSPORT_NETWORK_UNAVAILABLE",
                Batch2ContractCoreR926.classifyFetch(false, 0, false, "airplane"), "B08 airplane");
        eq("TRANSPORT_NETWORK_UNAVAILABLE",
                Batch2ContractCoreR926.classifyFetch(false, 0, false,
                        "Unable to resolve host api.example: No address associated with hostname"),
                "B08 Android offline DNS");
        eq("ACCEPTED_DUPLICATE",
                Batch2ContractCoreR926.classifyFetch(true, 200, true, "duplicate_idempotent"), "B08 legal duplicate");
    }

    private static void learningExactlyOnceIdentity() {
        Set<String> ids = new HashSet<String>();
        for (int i = 0; i < 10; i++) {
            ids.add(Batch2ContractCoreR926.learningKey(
                    "Binance1-PK10", "202609090001", "FTRL_R811_V1", "STRICT_OOS_V1", hex('a')));
        }
        check(ids.size() == 1, "F11 duplicate label must converge to one learning identity");
        String changed = Batch2ContractCoreR926.learningKey(
                "Binance1-PK10", "202609090001", "FTRL_R811_V1", "STRICT_OOS_V2", hex('a'));
        check(!ids.contains(changed), "label version must be part of identity");
    }

    private static void backlogContinuity() {
        List<String> expected = new ArrayList<String>();
        Set<String> full = new HashSet<String>();
        for (int i = 100; i <= 120; i++) {
            String issue = String.valueOf(i);
            expected.add(issue);
            if (i < 110 || i > 119) full.add(issue);
        }
        Batch2ContractCoreR926.Continuity c =
                Batch2ContractCoreR926.evaluateContinuity(expected, full, 0);
        check(c.holes == 10, "F12 hidden middle holes");
        eq("109", c.continuousWatermark, "F12 continuous watermark");
        eq("120", c.latestFull, "F12 latest FULL may be ahead of watermark");
        check(!c.closure, "F12 latest FULL cannot hide hole");
        Batch2ContractCoreR926.Continuity bad =
                Batch2ContractCoreR926.evaluateContinuity(expected, new HashSet<String>(expected), 1);
        check(!bad.closure && bad.unrecoverable == 1, "F13 unrecoverable snapshot must fail closure");
    }

    private static void gateProvenance() {
        Batch2ContractCoreR926.GateInput valid = fullGate(hex('9'), hex('e'));
        Batch2ContractCoreR926.GateCheck ok = Batch2ContractCoreR926.validateGate(valid);
        check(ok.valid && ok.isBet && "BET_PROVENANCE_VALID_V2".equals(ok.code),
                "B11 valid full provenance");

        Batch2ContractCoreR926.GateInput legacyBet = new Batch2ContractCoreR926.GateInput(
                "BET", true, true, 1, 3, 1, 4, 7,
                hex('1'), hex('2'), hex('3'), hex('4'), hex('5'), hex('6'), hex('7'), hex('8'));
        eq("BET_DATA_HASH_MISSING", Batch2ContractCoreR926.validateGate(legacyBet).code,
                "B11 legacy partial evidence must fail closed");

        Batch2ContractCoreR926.GateInput missingSourceBoundData = fullGate("", hex('e'));
        eq("BET_DATA_HASH_MISSING",
                Batch2ContractCoreR926.validateGate(missingSourceBoundData).code,
                "B11 missing SourceEvent-bound data evidence rejects BET");

        Batch2ContractCoreR926.GateInput missingCertificate = fullGate(hex('9'), "");
        eq("BET_CERTIFICATE_HASH_MISSING",
                Batch2ContractCoreR926.validateGate(missingCertificate).code,
                "B11 missing certificate evidence rejects BET");

        Batch2ContractCoreR926.GateInput duplicateNumbers = new Batch2ContractCoreR926.GateInput(
                "BET", true, true, 1, 3, 1, 1, 7,
                hex('1'), hex('2'), hex('3'), hex('4'), hex('5'), hex('6'), hex('7'), hex('8'),
                hex('9'), hex('a'), hex('b'), hex('c'), hex('d'), hex('e'), hex('f'), hex('0'),
                "config=full");
        check(!Batch2ContractCoreR926.validateGate(duplicateNumbers).valid,
                "B11 fixed K3 must be three distinct numbers");

        Batch2ContractCoreR926.GateInput hold = new Batch2ContractCoreR926.GateInput(
                "HOLD", false, false, 0, 0, 0, 0, 0,
                "", hex('2'), hex('3'), hex('4'), hex('5'), hex('6'), hex('7'), hex('8'));
        check(Batch2ContractCoreR926.validateGate(hold).valid,
                "B11 HOLD does not require BET provenance");
    }

    private static Batch2ContractCoreR926.GateInput fullGate(
            String dataHash, String certificateHash) {
        return new Batch2ContractCoreR926.GateInput(
                "BET", true, true, 1, 3, 1, 4, 7,
                hex('1'), hex('2'), hex('3'), hex('4'), hex('5'), hex('6'), hex('7'), hex('8'),
                dataHash, hex('a'), hex('b'), hex('c'), hex('d'), hex('e'), certificateHash, hex('f'),
                "game=Binance1-PK10|play=single-position|fixed-K3|max-one-REAL-per-issue");
    }

    private static void latencyEvidence() {
        List<Long> samples = Arrays.asList(100L, 200L, 300L, 400L, 500L, 600L, 700L, 800L, 900L, 6000L);
        Batch2ContractCoreR926.LatencyStats s =
                Batch2ContractCoreR926.latencyStats(samples, Batch2ContractCoreR926.REQUEST_TO_DURABLE_BUDGET_MS);
        check(s.n == 10 && s.p50 == 500L && s.p95 == 6000L && s.p99 == 6000L && s.max == 6000L,
                "B05 percentile evidence");
        check(s.overBudget == 1, "B05 over-budget count");
    }

    private static String hex(char c) {
        char[] x = new char[64];
        Arrays.fill(x, c);
        return new String(x);
    }

    private static void eq(String expected, String actual, String name) {
        if (!expected.equals(actual)) throw new AssertionError(name + " expected=" + expected + " actual=" + actual);
    }

    private static void check(boolean ok, String name) {
        if (!ok) throw new AssertionError(name);
    }
}
