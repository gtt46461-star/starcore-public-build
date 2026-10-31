package com.ethk3.novaonepro.strategyv2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * R30-B07-03 每期主链决定的逐项解释（M28/M30 报告口径）。纯Java，HOST可测。
 *
 * 本类不是 REAL 的授权者：唯一授权者仍是 DecisionGateEngine。本类只把 DecisionGateEngine 已有的检查结果
 * 汇总成固定顺序的“通过/拒绝原因”，供报告“有证书但没出手”章节逐期列出。检查顺序固定，第一项失败即停止后续“允许”判定，
 * 但所有项都要求值（便于报告完整列出）。
 *
 * 安全默认：用户 REAL 开关默认关闭；任何一项未知（null）按拒绝处理并写明 UNKNOWN。
 * 禁止：追期、加倍、马丁、亏损后加注、频率补配额（15% 是观察目标，不是配额）。
 */
public final class DecisionChainR30 {
    private DecisionChainR30() {}

    public static final class Facts {
        public Boolean dataHealthy;          // 本期开奖源实时、无缺期、无冲突
        public Boolean freezeComplete;       // 本期赛前冻结已提交且余量≥5000ms（PitGuardR30 通过）
        public Boolean modelAvailable;       // 冻结引用的模型版本存在且 hash 匹配
        public Boolean certificateActive;    // 证书 ACTIVE、未撤销、未过期、版本与 hash 匹配
        public Boolean selectorConfirmed;    // 主链选择器已确认（未确认组合不能 REAL）
        public Boolean onePerIssue;          // 本期尚无 REAL 意向（每期最多一笔）
        public Boolean userRealEnabled;      // 用户 REAL 开关（默认 false）
        public Boolean actRuleFired;         // 策略自身出手条件成立
        public Boolean stakeFixed;           // 金额为固定单位；任何基于亏损/连败的调整都为 false
    }

    public static final String[] ORDER = {"dataHealthy", "freezeComplete", "modelAvailable", "certificateActive",
            "selectorConfirmed", "onePerIssue", "stakeFixed", "actRuleFired", "userRealEnabled"};

    public static final class Verdict {
        public final boolean allowReal; public final String firstBlocker; public final Map<String, String> items;
        Verdict(boolean a, String f, Map<String, String> i) { allowReal = a; firstBlocker = f; items = i; }
    }

    public static Verdict explain(Facts f) {
        Boolean[] vals = {f.dataHealthy, f.freezeComplete, f.modelAvailable, f.certificateActive, f.selectorConfirmed,
                f.onePerIssue, f.stakeFixed, f.actRuleFired, f.userRealEnabled};
        Map<String, String> items = new LinkedHashMap<>();
        String first = null;
        for (int i = 0; i < ORDER.length; i++) {
            String state = vals[i] == null ? "UNKNOWN" : vals[i] ? "PASS" : "FAIL";
            items.put(ORDER[i], state);
            if (first == null && !"PASS".equals(state)) first = ORDER[i] + ":" + state;
        }
        return new Verdict(first == null, first, items);
    }

    /** 报告用：统计一段期间内“有ACTIVE证书但未出手”的首要拒绝原因分布。 */
    public static Map<String, Integer> blockerHistogram(List<Verdict> verdicts) {
        Map<String, Integer> h = new LinkedHashMap<>();
        for (Verdict v : verdicts) if (!v.allowReal) h.merge(v.firstBlocker, 1, Integer::sum);
        return h;
    }

    public static List<String> orderedNames() { List<String> l = new ArrayList<>(); for (String s : ORDER) l.add(s); return l; }
}
