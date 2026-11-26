package com.ethk3.novaonepro;

import com.ethk3.novaonepro.strategyv2.LegacyContinuityR27;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Map;

/** Runs inherited ledger validation using the actual packaged asset and one exact edit. */
public final class R343LedgerAuditCases {
    private R343LedgerAuditCases() {}

    public static Map<String, Object> unresolvedCardCase(String asset) throws Exception {
        JSONObject altered = new JSONObject(asset);
        JSONArray requirements = altered.getJSONArray("fileCards").getJSONObject(0).getJSONArray("requirements");
        if (requirements.length() == 0) throw new AssertionError("Expected a card with real requirement links");
        requirements.put(0, "MISSING-R343-REQUIREMENT-LINK");

        Map<String, Object> result = LegacyContinuityR27.preservation(altered.toString(), Collections.emptyMap());
        require(1832L, result.get("requirements"), "Requirement denominator must remain unchanged");
        require(299L, result.get("fileCards"), "Card denominator must remain unchanged");
        require(1L, result.get("unresolvedCardLinks"), "Exactly the changed reference is missing");
        require(false, result.get("ledgerComplete"), "Unresolved card linkage must fail the inherited audit");
        require("FAIL", result.get("status"), "Unresolved reference cannot look like a complete ledger");
        return result;
    }

    public static Map<String, Object> unreceiptedCompletionCase(String asset) throws Exception {
        JSONObject altered = new JSONObject(asset);
        altered.getJSONArray("requirements").getJSONObject(0).put("closure", "VERIFIED");

        Map<String, Object> result = LegacyContinuityR27.preservation(altered.toString(), Collections.emptyMap());
        require(1832L, result.get("requirements"), "Requirement remains registered");
        require(299L, result.get("fileCards"), "Card remains registered");
        require(1L, result.get("completionClaimsWithoutReceipt"), "Exactly one unsupported claim is detected");
        require(0L, result.get("regressionReceipts"), "Synthetic fixture produces no receipt");
        require("FAIL", result.get("status"), "Unreceipted completion cannot become a parent verification");
        return result;
    }

    static void require(Object expected, Object observed, String message) {
        if (!expected.equals(observed)) throw new AssertionError(
                message + " expected=" + expected + " observed=" + observed);
    }
}
