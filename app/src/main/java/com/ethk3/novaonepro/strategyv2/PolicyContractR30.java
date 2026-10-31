package com.ethk3.novaonepro.strategyv2;

import java.util.ArrayList;
import java.util.List;

/**
 * R30-B04-01 完整策略合同校验（M21 唯一判定）。纯Java，直接读取现有 PolicySpecR27/PolicyAstR27，不新建第二套策略格式。
 *
 * 一个“完整策略”必须同时具备：可执行打分程序（含至少一个非常数数据叶子、类型合法、范围与因果合法）、
 * 选位（1..10 固定位）、K=3、出手规则及其参数、读出温度、在线更新规则、来源（idea/operator/reason）。
 * 任何一项缺失即为“空壳”，出生门（BirthGateR27）必须拒绝并记录拒绝码，不能进入观察池或资格队列。
 */
public final class PolicyContractR30 {
    private PolicyContractR30() {}

    public static List<String> violations(PolicySpecR27 s) {
        List<String> v = new ArrayList<>();
        if (s == null) { v.add("NULL_POLICY"); return v; }
        if (s.ast == null) v.add("NO_PROGRAM");
        else {
            try { PolicyAstR27.Type t = PolicyAstR27.type(s.ast); if (t == PolicyAstR27.Type.CONST) v.add("PROGRAM_CONSTANT_ONLY"); }
            catch (IllegalArgumentException typeError) { v.add("PROGRAM_" + typeError.getMessage()); }
            String rc = PolicyAstR27.rangeAndCausality(s.ast);
            if (rc != null) v.add("PROGRAM_" + rc);
            int dataLeaves = 0;
            for (PolicyAstR27.Node n : PolicyAstR27.nodes(s.ast)) if (n.leaf() && !"c".equals(n.op)) dataLeaves++;
            if (dataLeaves == 0) v.add("PROGRAM_NO_DATA_LEAF");
            if (s.ast.size() > 64) v.add("PROGRAM_TOO_LARGE:" + s.ast.size());
            if (s.ast.depth() > 8) v.add("PROGRAM_TOO_DEEP:" + s.ast.depth());
        }
        if (s.position < 1 || s.position > 10) v.add("SELECTOR_POSITION:" + s.position);
        if (s.rule == null) v.add("SELECTOR_RULE_NULL");
        else switch (s.rule) {
            case MARGIN: if (!finite(s.threshold) || s.threshold < -0.30 || s.threshold > 0.70) v.add("SELECTOR_MARGIN_THRESHOLD:" + s.threshold); break;
            case RANDOM: case QUANTILE: if (!finite(s.rate) || s.rate <= 0 || s.rate > 1) v.add("SELECTOR_RATE:" + s.rate); break;
            case ALWAYS: break;
            default: v.add("SELECTOR_RULE_UNKNOWN");
        }
        if (!finite(s.temperature) || s.temperature < 0.05 || s.temperature > 20) v.add("READOUT_TEMPERATURE:" + s.temperature);
        if (!finite(s.lr) || s.lr < 0 || s.lr > 1) v.add("UPDATER_LR:" + s.lr);
        if (!finite(s.forget) || s.forget < 0 || s.forget > 1) v.add("UPDATER_FORGET:" + s.forget);
        if (s.meta == null) v.add("PROVENANCE_MISSING");
        else {
            if (blank(s.meta.idea)) v.add("PROVENANCE_IDEA");
            if (blank(s.meta.operator)) v.add("PROVENANCE_OPERATOR");
            if (blank(s.meta.reason)) v.add("PROVENANCE_REASON");
            if (s.meta.generation < 0) v.add("PROVENANCE_GENERATION");
        }
        return v;
    }

    /** RANDOM 规则只能作为对照组（I00/空信号），不得以候选身份进入资格队列。 */
    public static boolean controlOnly(PolicySpecR27 s) { return s != null && s.rule == PolicySpecR27.Rule.RANDOM; }

    static boolean finite(double d) { return !Double.isNaN(d) && !Double.isInfinite(d); }
    static boolean blank(String s) { return s == null || s.trim().isEmpty(); }
}
