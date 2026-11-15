package com.ethk3.novaonepro;

import java.util.List;

/** Deterministic code/strategy/model/runtime attribution for every target issue. */
public final class RootCauseAttributionEngine {
    public static final String MODULE_VERSION = "V47_ROOT_CAUSE_ATTRIBUTION_20260721_47072147";

    public static final class Result {
        public String code = "";
        public String owner = "";
        public String detail = "";

        public String summary() {
            return code + "|owner=" + owner + "|" + detail;
        }
    }

    private RootCauseAttributionEngine() {}

    public static Result diagnose(List<Models.ComboCandidate> list, long totalMs, String identity) {
        Result r = new Result();
        if (identity != null && identity.indexOf("MISMATCH") >= 0) {
            return set(r, AppConfig.V47_ROOT_SURFACE, "BuildIdentity.java", "版本/批次/Schema不一致");
        }
        if (totalMs > AppConfig.V47_REALTIME_P95_MS) {
            return set(
                    r,
                    AppConfig.V47_ROOT_RUNTIME,
                    "AutoFetchService.java",
                    "完整分析耗时=" + totalMs + "ms，超过P95预算");
        }
        if (list == null || list.size() == 0) {
            return set(
                    r, AppConfig.V47_ROOT_SURFACE, "FinalCandidateRankingEngine.java", "最终正式候选为空");
        }
        if (list.size() < AppConfig.V47_FORMAL_CANDIDATE_COUNT) {
            return set(
                    r,
                    AppConfig.V47_ROOT_SURFACE,
                    "FinalCandidateRankingEngine/AutoFetchService",
                    "正式候选=" + list.size() + "，低于" + AppConfig.V47_FORMAL_CANDIDATE_COUNT);
        }
        boolean[] pos = new boolean[AppConfig.POSITION_COUNT + 1];
        int covered = 0;
        for (int i = 0; i < list.size(); i++) {
            Models.ComboCandidate x = list.get(i);
            if (x != null
                    && x.position >= 1
                    && x.position <= AppConfig.POSITION_COUNT
                    && !pos[x.position]) {
                pos[x.position] = true;
                covered++;
            }
        }
        if (covered < AppConfig.POSITION_COUNT) {
            return set(
                    r,
                    AppConfig.V47_ROOT_SURFACE,
                    "FinalCandidateRankingEngine",
                    "40候选只覆盖" + covered + "/10位置");
        }
        Models.ComboCandidate top = list.get(0);
        if (top.v46FinalRank != 1) {
            return set(r, AppConfig.V47_ROOT_RANK, "FinalCandidateRankingEngine.java", "最终R1标记不一致");
        }
        if (FinalCandidateRankingEngine.eventP(top) < AppConfig.V46_PROBE_MIN_EVENT_P) {
            return set(
                    r,
                    AppConfig.V47_ROOT_MODEL,
                    "NumberDistributionEngineV46/ConditionalCalibrationEngine",
                    "R1 EventP=" + FinalCandidateRankingEngine.eventP(top) + "低于受控探针底线");
        }
        if (FinalCandidateRankingEngine.actionLcb(top) < AppConfig.V46_PROBE_MIN_ACTION_LCB) {
            return set(
                    r,
                    AppConfig.V47_ROOT_CALIBRATION,
                    "ConditionalCalibrationEngine",
                    "EventP可用但ActionLCB不足，不确定性/风险边界过宽");
        }
        return set(
                r, AppConfig.V47_ROOT_POLICY, "LanePolicyCompiler.java", "模型候选达到概率底线，需查看合同具体质量缺口");
    }

    public static void apply(Models.ComboCandidate c, Result r) {
        if (c == null || r == null) return;
        c.v46RootCauseCode = r.code;
        c.v46RootCauseOwner = r.owner;
        c.reasonSummary = append(c.reasonSummary, "V46根因=" + r.summary());
    }

    private static Result set(Result r, String c, String o, String d) {
        r.code = c;
        r.owner = o;
        r.detail = d;
        return r;
    }

    private static String append(String a, String b) {
        if (a == null || a.length() == 0) return b;
        if (b == null || b.length() == 0 || a.indexOf(b) >= 0) return a;
        return a + "｜" + b;
    }
}
