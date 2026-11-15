package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.List;

/** Enforces exact 60 pre-final and 40 formal surface. */
public final class Final40ContractAuditor {
    public static final String MODULE_VERSION = "V48_FINAL40_CONTRACT_AUDITOR_20260721_48072148";

    private Final40ContractAuditor() {}

    public static ArrayList<Models.ComboCandidate> rank(
            DbStore store, List<Models.ComboCandidate> input, String target) {
        int before = input == null ? 0 : input.size();
        V48PipelineLedger.preFinal(store, before);
        ArrayList<Models.ComboCandidate> out =
                FinalCandidateRankingEngine.rank(store, input, target);
        V48PipelineLedger.formal(store, out.size());
        String status =
                before == AppConfig.V48_PROBABILITY_CANDIDATE_COUNT
                                && out.size() == AppConfig.V48_FORMAL_CANDIDATE_COUNT
                        ? "OK"
                        : AppConfig.V48_ROOT_SURFACE;
        try {
            if (store != null) {
                store.setState(
                        "v48_final40_audit", status + "|input=" + before + "|output=" + out.size());
                store.setState("v48_final40_audit_version", MODULE_VERSION);
            }
        } catch (Throwable ignore) {
        }
        return out;
    }
}
