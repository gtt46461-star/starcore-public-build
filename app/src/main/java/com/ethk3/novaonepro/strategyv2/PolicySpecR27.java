package com.ethk3.novaonepro.strategyv2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/** R27-021 complete executable strategy: inputs + model + readout + selector + updater (+ meta).
 * policyHash = SHA-256 of the canonical JSON of the five executable sections; meta (lineage, reason, idea family)
 * never changes the identity, so two births with identical behavior-defining text are the same policy. */
public final class PolicySpecR27 {
    public static final String SCHEMA = "R27_POLICY_SPEC_1";
    public enum Rule { MARGIN, ALWAYS, RANDOM, QUANTILE }

    public final PolicyAstR27.Node ast;
    /** Selector: target position, act rule and its threshold (MARGIN: top-3 mass − 0.30 ≥ threshold; RANDOM: act w.p. rate;
     * QUANTILE: act when the top-3 mass is within the top `rate` share of its own previous 200 masses — a PIT rule). */
    public final int position; public final Rule rule; public final double threshold, rate;
    /** Readout: initial temperature, and whether the temperature is fitted on H_TRAIN. */
    public final double temperature; public final boolean fitTemperature;
    /** Updater: online per-number bias learning rate and forgetting (both fixed at birth; applied identically in replay and live). */
    public final double lr, forget;
    public final Meta meta;

    public static final class Meta {
        public final String idea, operator, reason; public final List<String> parents; public final int generation; public final long seed;
        public Meta(String idea, String operator, List<String> parents, int generation, long seed, String reason) {
            this.idea = idea; this.operator = operator; this.parents = parents == null ? Collections.<String>emptyList() : Collections.unmodifiableList(new ArrayList<>(parents));
            this.generation = generation; this.seed = seed; this.reason = reason == null ? "" : reason;
        }
    }

    public PolicySpecR27(PolicyAstR27.Node ast, int position, Rule rule, double threshold, double rate, double temperature, boolean fitTemperature, double lr, double forget, Meta meta) {
        this.ast = ast; this.position = position; this.rule = rule; this.threshold = threshold; this.rate = rate;
        this.temperature = temperature; this.fitTemperature = fitTemperature; this.lr = lr; this.forget = forget; this.meta = meta;
    }

    public PolicySpecR27 withAst(PolicyAstR27.Node a, Meta m) { return new PolicySpecR27(a, position, rule, threshold, rate, temperature, fitTemperature, lr, forget, m); }
    public PolicySpecR27 withSelector(int p, Rule r, double th, double rt, Meta m) { return new PolicySpecR27(ast, p, r, th, rt, temperature, fitTemperature, lr, forget, m); }
    public PolicySpecR27 withUpdater(double l, double f, Meta m) { return new PolicySpecR27(ast, position, rule, threshold, rate, temperature, fitTemperature, l, f, m); }
    public PolicySpecR27 withMeta(Meta m) { return new PolicySpecR27(ast, position, rule, threshold, rate, temperature, fitTemperature, lr, forget, m); }

    /** Windows and leaf kinds the program reads (docs/04 4.1 "inputs"). */
    public Map<String, Object> inputs() {
        TreeSet<String> leaves = new TreeSet<>(); TreeSet<Long> windows = new TreeSet<>();
        for (PolicyAstR27.Node n : PolicyAstR27.nodes(ast)) if (n.leaf() && !n.op.equals("c")) {
            leaves.add(n.op);
            switch (n.op) { case "freq": case "freqnb": case "meangap": case "oddeven": case "bigsmall": windows.add((long) n.ints[1]); break; case "regime": windows.add((long) n.ints[0]); break; default: break; }
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("leaves", new ArrayList<Object>(leaves)); m.put("windows", new ArrayList<Object>(windows)); m.put("missing", "SKIPPED_GAP");
        return m;
    }

    public Map<String, Object> executable() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema", SCHEMA);
        root.put("inputs", inputs());
        Map<String, Object> model = new LinkedHashMap<>(); model.put("ast", ast.toString()); root.put("model", model);
        Map<String, Object> readout = new LinkedHashMap<>(); readout.put("temperature", temperature); readout.put("fitTemperature", fitTemperature); readout.put("surface", "TARGET_POSITION_SOFTMAX_OTHERS_UNIFORM"); root.put("readout", readout);
        Map<String, Object> sel = new LinkedHashMap<>(); sel.put("position", (long) position); sel.put("rule", rule.name()); sel.put("threshold", threshold); sel.put("rate", rate); sel.put("k", 3L); root.put("selector", sel);
        Map<String, Object> up = new LinkedHashMap<>(); up.put("lr", lr); up.put("forget", forget); up.put("rule", "PER_NUMBER_BIAS_SGD_V1"); root.put("updater", up);
        return root;
    }

    public String canonical() { return JsonR27.write(executable()); }
    private String hash;
    public String policyHash() { if (hash == null) hash = StatsR27.sha256(canonical()); return hash; }

    public Map<String, Object> metaJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        if (meta == null) return m;
        m.put("idea", meta.idea); m.put("operator", meta.operator); m.put("parents", new ArrayList<Object>(meta.parents));
        m.put("generation", (long) meta.generation); m.put("seed", meta.seed); m.put("reason", meta.reason);
        return m;
    }
    public String fullJson() { Map<String, Object> m = new LinkedHashMap<>(executable()); m.put("meta", metaJson()); return JsonR27.write(m); }

    @SuppressWarnings("unchecked")
    public static PolicySpecR27 parse(String json) {
        Map<String, Object> m = (Map<String, Object>) JsonR27.parse(json);
        if (!SCHEMA.equals(m.get("schema"))) throw new IllegalArgumentException("R27_POLICY_SCHEMA");
        Map<String, Object> model = (Map<String, Object>) m.get("model"), readout = (Map<String, Object>) m.get("readout"), sel = (Map<String, Object>) m.get("selector"), up = (Map<String, Object>) m.get("updater");
        Meta meta = null;
        Map<String, Object> mm = (Map<String, Object>) m.get("meta");
        if (mm != null && !mm.isEmpty()) {
            List<String> parents = new ArrayList<>(); for (Object o : (List<Object>) mm.get("parents")) parents.add((String) o);
            meta = new Meta((String) mm.get("idea"), (String) mm.get("operator"), parents, (int) JsonR27.num(mm.get("generation")), ((Number) mm.get("seed")).longValue(), (String) mm.get("reason"));
        }
        return new PolicySpecR27(PolicyAstR27.parse((String) model.get("ast")), (int) JsonR27.num(sel.get("position")), Rule.valueOf((String) sel.get("rule")),
                JsonR27.num(sel.get("threshold")), JsonR27.num(sel.get("rate")), JsonR27.num(readout.get("temperature")), (Boolean) readout.get("fitTemperature"),
                JsonR27.num(up.get("lr")), JsonR27.num(up.get("forget")), meta);
    }

    public String describeZh() {
        String act = rule == Rule.ALWAYS ? "每期出手" : rule == Rule.RANDOM ? "随机出手(率" + rate + ")" : rule == Rule.QUANTILE ? "置信度处于近200期前" + rate + "时出手" : "前三概率和超出0.30达" + threshold + "才出手";
        return "第" + position + "位 K3；打分=" + PolicyAstR27.describe(ast) + "；" + act + "；在线学习率" + lr + "，遗忘" + forget;
    }
}
