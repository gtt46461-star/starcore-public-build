package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.List;

/**
 * Typed bridge between the older CandidateK3 strategy engines and the live ComboCandidate pipeline.
 * It preserves every same-name public field in both directions and validates identity before
 * copying results back.
 */
public final class LegacyCandidateBridge {
    public static final String VERSION = AppConfig.V43_UNIFIED_STRATEGY_VERSION;

    private LegacyCandidateBridge() {}

    public static ArrayList<Models.CandidateK3> toCandidateK3(List<Models.ComboCandidate> source) {
        ArrayList<Models.CandidateK3> out = new ArrayList<Models.CandidateK3>();
        if (source == null) return out;
        for (int i = 0; i < source.size(); i++) {
            Models.ComboCandidate c = source.get(i);
            if (c == null || !c.isValidK3()) continue;
            Models.CandidateK3 k = new Models.CandidateK3();
            Models.copySharedPublicFieldsV20(c, k);
            k.baseIssue = c.baseIssue;
            k.targetIssue = c.targetIssue;
            k.position = c.position;
            k.n1 = c.n1;
            k.n2 = c.n2;
            k.n3 = c.n3;
            k.k3Mask = c.k3Mask;
            k.rank = c.rank;
            k.rawScore = c.rawRankScore != 0.0d ? c.rawRankScore : c.evidenceScore;
            k.rawK3P =
                    c.modelP > 0.0d
                            ? c.modelP
                            : (c.rawP > 0.0d ? c.rawP : AppConfig.RANDOM_K3_BASELINE);
            k.finalP = c.finalP;
            k.dynamicGate = c.dynamicGate;
            k.edge = c.edge;
            out.add(k);
        }
        return out;
    }

    public static void copyBack(List<Models.CandidateK3> from, List<Models.ComboCandidate> to) {
        if (from == null || to == null) return;
        for (int i = 0; i < from.size(); i++) {
            Models.CandidateK3 k = from.get(i);
            if (k == null) continue;
            Models.ComboCandidate c = find(to, k);
            if (c == null) continue;
            String before = identity(c);
            Models.copySharedPublicFieldsV20(k, c);
            /* Identity is immutable even if an old engine exposes same-name fields. */
            c.baseIssue = k.baseIssue;
            c.targetIssue = k.targetIssue;
            c.position = k.position;
            c.n1 = k.n1;
            c.n2 = k.n2;
            c.n3 = k.n3;
            c.k3Mask = k.k3Mask;
            if (!before.equals(identity(c))) {
                c.hardBlockFlag = 1;
                c.hardBlockSource = "V43_LEGACY_BRIDGE_IDENTITY_MUTATION";
                c.v43HardRisk = 1;
                c.v43InvariantStatus = "BLOCK:LEGACY_IDENTITY_MUTATION";
            }
        }
    }

    private static Models.ComboCandidate find(
            List<Models.ComboCandidate> list, Models.CandidateK3 k) {
        for (int i = 0; i < list.size(); i++) {
            Models.ComboCandidate c = list.get(i);
            if (c == null) continue;
            if (c.position == k.position
                    && c.k3Mask == k.k3Mask
                    && safe(c.targetIssue).equals(safe(k.targetIssue))) return c;
            if (c.position == k.position && same(c.n1, c.n2, c.n3, k.n1, k.n2, k.n3)) return c;
        }
        return null;
    }

    private static boolean same(int a, int b, int c, int x, int y, int z) {
        int[] p = new int[] {a, b, c}, q = new int[] {x, y, z};
        java.util.Arrays.sort(p);
        java.util.Arrays.sort(q);
        return p[0] == q[0] && p[1] == q[1] && p[2] == q[2];
    }

    private static String identity(Models.ComboCandidate c) {
        return safe(c.targetIssue) + "|" + c.position + "|" + c.n1 + "|" + c.n2 + "|" + c.n3;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
