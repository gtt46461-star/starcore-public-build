package com.ethk3.novaonepro.strategyv2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Repair diagnostic, never a qualification score. Counterfactual position replays use the parent's unchanged
 * program/selector/updater and H_TRAIN temperature fit. The worst trailing window is entirely in H_SELECT.
 * No H_CONFIRM labels, ledger exposures, or future outcomes participate. */
public final class FailureProfileR30 {
    private static final int MIN_WINDOW_ACTIONS=8;
    public final String status, datasetHash, parentHash;
    public final String fromIssue,throughIssue,failuresHash;
    public final int position, window, from, to, nAct, hits;

    private FailureProfileR30(String status, String dataset, String parent, int position, int window, int from, int to, int acts, int hits,String fromIssue,String throughIssue,String failuresHash) {
        this.status = status; datasetHash = dataset; parentHash = parent; this.position = position;
        this.window = window; this.from = from; this.to = to; nAct = acts; this.hits = hits;
        this.fromIssue=fromIssue;this.throughIssue=throughIssue;this.failuresHash=failuresHash;
    }
    public boolean ready() { return "READY".equals(status); }
    public double hitRate() { return nAct == 0 ? Double.NaN : hits / (double)nAct; }

    public static FailureProfileR30 forParent(PolicySpecR27 parent, IdeaFamilyR27.Context ctx) {
        return ctx == null || ctx.evaluation == null ? missing(parent) : ctx.evaluation.failureProfile(parent);
    }
    static FailureProfileR30 missing(PolicySpecR27 parent) {
        return new FailureProfileR30("NOT_RUN", "", parent.policyHash(), 0, 0, 0, 0, 0, 0,"","","");
    }

    static FailureProfileR30 measure(PolicySpecR27 parent, EvalContextR27 ctx) {
        SplitR27 sp = ctx.split;
        FailureProfileR30 worst = new FailureProfileR30("INSUFFICIENT",ctx.data.datasetHash,parent.policyHash(),0,0,sp.selectFrom,sp.selectTo,0,0,"","","");
        for (int pos = 1; pos <= 10; pos++) {
            PolicySpecR27 probe = parent.withSelector(pos,parent.rule,parent.threshold,parent.rate,parent.meta);
            ReplayEvaluatorR27.Trace trace = ctx.selectTrace(probe,ctx.temperature(probe));
            for (int w : FeatureCacheR27.WINDOWS) {
                if (w > sp.selectTo - sp.selectFrom) continue;
                int from = sp.selectTo - w, acts = 0, hits = 0;
                for (int t = from; t < sp.selectTo; t++) if (!trace.skipped[t] && trace.act[t]) { acts++; hits += trace.hit[t]; }
                if (acts < MIN_WINDOW_ACTIONS) continue;
                if (!worst.ready() || hits * (long)worst.nAct < worst.hits * (long)acts ||
                        hits * (long)worst.nAct == worst.hits * (long)acts && acts > worst.nAct)
                {
                    StringBuilder misses=new StringBuilder();
                    for(int row=from;row<sp.selectTo;row++)if(!trace.skipped[row]&&trace.act[row]&&trace.hit[row]==0)
                        misses.append(ctx.data.issues[row]).append('|');
                    worst=new FailureProfileR30("READY",ctx.data.datasetHash,parent.policyHash(),pos,w,from,sp.selectTo,acts,hits,
                        ctx.data.issues[from],ctx.data.issues[sp.selectTo-1],StatsR27.sha256(misses.toString()));
                }
            }
        }
        return worst;
    }

    public Map<String,Object> json() {
        Map<String,Object> m = new LinkedHashMap<>(); m.put("status",status);
        m.put("method","COUNTERFACTUAL_POSITION_TRAILING_SELECT_WINDOW"); m.put("role",SplitR27.SELECT);
        m.put("datasetHash",datasetHash); m.put("parentHash",parentHash); m.put("position",(long)position);
        m.put("window",(long)window); m.put("from",(long)from); m.put("to",(long)to);
        m.put("fromIssue",fromIssue);m.put("throughIssue",throughIssue);m.put("failedIssuesHash",failuresHash);
        m.put("minimumWindowActions",MIN_WINDOW_ACTIONS);
        m.put("nAct",(long)nAct); m.put("hits",(long)hits); m.put("hitRate",nAct == 0 ? null : hitRate());
        return m;
    }
    public String reason() { return "failureProfile=" + JsonR27.write(json()); }

    /** Prefer leaves reading the failed position/window. Subtrees containing them also qualify, with a small
     * size penalty so a matching leaf is repaired before the entire program is replaced. */
    public List<Integer> prioritize(List<PolicyAstR27.Node> nodes, List<Integer> candidates) {
        if (!ready() || candidates.isEmpty()) return candidates;
        double best = 0; List<Integer> out = new ArrayList<>();
        for (int i : candidates) {
            double score = priority(nodes.get(i));
            if (score > best) { best = score; out.clear(); }
            if (score > 0 && score == best) out.add(i);
        }
        return out.isEmpty() ? candidates : out;
    }
    private double priority(PolicyAstR27.Node n) {
        double best = 0;
        for (PolicyAstR27.Node leaf : PolicyAstR27.nodes(n)) {
            int source = sourcePosition(leaf), slot = MutationOpsR27.windowed(leaf);
            double score = (source == position ? 2 : 0) + (slot >= 0 && leaf.ints[slot] == window ? 1 : 0);
            best = Math.max(best,score);
        }
        return best - 0.01 * (n.size() - 1);
    }
    static int sourcePosition(PolicyAstR27.Node n) {
        switch (n.op) {
            case "freq": case "fdecay": case "freqnb": case "gap": case "gaprank": case "meangap":
            case "cross": case "crosscond": case "neighbor": case "oddeven": case "bigsmall": return n.ints[0];
            default: return 0;
        }
    }
    static boolean legal(PolicySpecR27 s) {
        try { return s != null && PolicyContractR30.violations(s).isEmpty(); }
        catch (IllegalArgumentException | IndexOutOfBoundsException malformed) { return false; }
    }
}
