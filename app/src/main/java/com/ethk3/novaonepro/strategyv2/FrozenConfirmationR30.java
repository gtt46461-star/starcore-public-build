package com.ethk3.novaonepro.strategyv2;

import java.util.LinkedHashMap;
import java.util.Map;

/** Confirmation runs after evolution has frozen all policies, readouts, data boundaries and controls. Its result
 * is consumed by persistence / FUTURE registration, never by evolutionary selection or operator adaptation. */
public final class FrozenConfirmationR30 {
    private FrozenConfirmationR30() {}

    public static final class Policy {
        private final String specJson;
        public final String policyHash, experimentId;
        public final double temperature;
        public final boolean control, shuffled;
        public final long labelSeed;
        private Policy(PolicySpecR27 spec, double tau, String kind, long seed) {
            specJson = spec.fullJson(); policyHash = spec.policyHash(); temperature = tau;
            control = !"MAIN".equals(kind); shuffled = "NULL".equals(kind); labelSeed = seed;
            experimentId = control ? "CONTROL_" + kind + ":" + policyHash + ":" + seed + ":" + Double.toHexString(tau) : policyHash;
            if (!Double.isFinite(tau) || tau <= 0) throw new IllegalArgumentException("R30_FROZEN_READOUT");
        }
        /** Return a new object so public AST parameter arrays cannot mutate the frozen packet. */
        public PolicySpecR27 spec() { return PolicySpecR27.parse(specJson); }
        Map<String,Object> json() {
            Map<String,Object> m = new LinkedHashMap<>(); m.put("spec",specJson); m.put("tau",temperature);
            m.put("kind",shuffled ? "NULL" : control ? "I00" : "MAIN"); m.put("seed",labelSeed); return m;
        }
        @SuppressWarnings("unchecked") static Policy parse(Object value) {
            if (value == null) return null; Map<String,Object> m = (Map<String,Object>)value;
            return new Policy(PolicySpecR27.parse((String)m.get("spec")),JsonR27.num(m.get("tau")),(String)m.get("kind"),((Number)m.get("seed")).longValue());
        }
    }

    public static final class Task {
        public final String key, datasetHash, sourceCommit;
        public final int generation, from, to;
        public final long block, frozenAt;
        public final boolean nullRequired;
        public final Policy champion, previous, i00, nullPolicy;
        private Task(String dataset, String source, int gen, long block, int from, int to, long at, boolean needNull, Policy ch, Policy prev, Policy random, Policy nil) {
            datasetHash = dataset; generation = gen; this.block = block; this.from = from; this.to = to;
            sourceCommit = source;
            frozenAt = at; nullRequired = needNull; champion = ch; previous = prev; i00 = random; nullPolicy = nil;
            key = StatsR27.sha256("R30_FROZEN_CONFIRM_V1|" + ch.policyHash + "|" + dataset + "|" + block);
        }
        public String json() {
            Map<String,Object> m = new LinkedHashMap<>(); m.put("dataset",datasetHash); m.put("sourceCommit",sourceCommit); m.put("generation",(long)generation);
            m.put("block",block); m.put("from",(long)from); m.put("to",(long)to); m.put("frozenAt",frozenAt); m.put("nullRequired",nullRequired);
            m.put("champion",champion.json()); m.put("previous",previous == null ? null : previous.json());
            m.put("i00",i00 == null ? null : i00.json()); m.put("null",nullPolicy == null ? null : nullPolicy.json());
            return JsonR27.write(m);
        }
        @SuppressWarnings("unchecked") public static Task parse(String text) {
            Map<String,Object> m = (Map<String,Object>)JsonR27.parse(text);
            return new Task((String)m.get("dataset"),(String)m.get("sourceCommit"),(int)JsonR27.num(m.get("generation")),((Number)m.get("block")).longValue(),
                    (int)JsonR27.num(m.get("from")),(int)JsonR27.num(m.get("to")),((Number)m.get("frozenAt")).longValue(),Boolean.TRUE.equals(m.get("nullRequired")),
                    Policy.parse(m.get("champion")),Policy.parse(m.get("previous")),Policy.parse(m.get("i00")),Policy.parse(m.get("null")));
        }
    }

    public static Task freeze(EvolutionLoopR27 loop, long at) {
        SplitR27 sp = loop.context().split;
        if (loop.champion == null || !sp.holdoutAvailable) return null;
        return new Task(loop.context().data.datasetHash,currentSource(),loop.generation,sp.holdoutBlock,sp.holdoutFrom,sp.holdoutTo,at,loop.cfg.nullControl,
                new Policy(loop.champion,loop.championTau,"MAIN",0),
                loop.previousChampion == null ? null : new Policy(loop.previousChampion,loop.previousTau,"MAIN",0),
                loop.i00Champion == null ? null : new Policy(loop.i00Champion,loop.i00Tau,"I00",0),
                !loop.cfg.nullControl || loop.nullEvolution.champion == null ? null : new Policy(loop.nullEvolution.champion,loop.nullEvolution.championTau,"NULL",loop.nullEvolution.lastSeed));
    }

    public interface Evidence {
        ReplayEvaluatorR27.Trace load(Task task, Policy policy);
        /** Commit raw outcomes before returning, so a later comparison can reuse evidence without another look. */
        void save(Task task, Policy policy, ReplayEvaluatorR27.Trace trace);
    }
    public static final class Result {
        public final Map<String,Object> report;
        public final boolean verified;
        Result(Map<String,Object> report) { this.report = new LinkedHashMap<>(report); verified = Boolean.TRUE.equals(report.get("verified")); }
        public String json() { return JsonR27.write(report); }
        @SuppressWarnings("unchecked") public static Result parse(String text) { return new Result((Map<String,Object>)JsonR27.parse(text)); }
    }
    public static Result unavailable(Task task, String reason) {
        Map<String,Object> h = base(task); h.put("status","INSUFFICIENT"); h.put("verified",false); h.put("reason",reason); return new Result(h);
    }
    private static Map<String,Object> base(Task task) {
        Map<String,Object> h = new LinkedHashMap<>(); h.put("taskKey",task.key); h.put("holdoutBlock",task.block);
        h.put("datasetHash",task.datasetHash); h.put("frozenAt",task.frozenAt); h.put("confirmationOutsideEvolution",true); return h;
    }

    public static Result evaluate(Task task, EvalContextR27 ctx, HoldoutLedgerR27 ledger, Evidence evidence) {
        if (!currentSource().equals(task.sourceCommit)) return unavailable(task,"FROZEN_SOURCE_CHANGED");
        if (!task.datasetHash.equals(ctx.data.datasetHash) || task.block != ctx.split.holdoutBlock || task.from != ctx.split.holdoutFrom || task.to != ctx.split.holdoutTo)
            throw new IllegalStateException("R30_CONFIRM_DATASET_OR_RANGE_MISMATCH");
        if (!ctx.split.holdoutAvailable || ledger.degraded(task.block)) return unavailable(task,"HOLDOUT_UNAVAILABLE_OR_DEGRADED");
        Map<String,Object> h = base(task);
        ReplayEvaluatorR27.Trace ch = read(task,task.champion,ctx,ledger,evidence);
        h.put("championHoldout",ReplayScoreR27.of(SplitR27.HOLDOUT,ch,task.from,task.to).json());
        boolean vsPrev = task.previous == null, vsI00 = false, vsNull = !task.nullRequired;
        if (task.previous != null) {
            ReplayEvaluatorR27.Trace prev = read(task,task.previous,ctx,ledger,evidence);
            double lcb = ReplayScoreR27.paired(ch,prev,task.from,task.to).lcb(StatsR27.Z95_ONE_SIDED);
            vsPrev = lcb > 0; h.put("pairedLcbVsPrevious",finite(lcb)); h.put("previousHoldout",ReplayScoreR27.of(SplitR27.HOLDOUT,prev,task.from,task.to).json());
        }
        if (task.i00 != null) {
            ReplayEvaluatorR27.Trace random = read(task,task.i00,ctx,ledger,evidence);
            double lcb = ReplayScoreR27.paired(ch,random,task.from,task.to).lcb(StatsR27.Z95_ONE_SIDED);
            vsI00 = lcb > 0; h.put("pairedLcbVsI00",finite(lcb)); h.put("i00Holdout",ReplayScoreR27.of(SplitR27.HOLDOUT,random,task.from,task.to).json());
        }
        if (task.nullRequired && task.nullPolicy != null) {
            ReplayEvaluatorR27.Trace nil = read(task,task.nullPolicy,ctx,ledger,evidence);
            double lcb = ReplayScoreR27.paired(ch,nil,task.from,task.to).lcb(StatsR27.Z95_ONE_SIDED);
            vsNull = lcb > 0; h.put("pairedLcbVsNull",finite(lcb)); h.put("nullHoldout",ReplayScoreR27.of(SplitR27.HOLDOUT,nil,task.from,task.to).json());
        }
        boolean verified = vsPrev && vsI00 && vsNull;
        h.put("verified",verified); h.put("status","COMPLETED");
        h.put("reason",verified ? "VERIFIED_IMPROVEMENT" : "UNVERIFIED:" + (vsPrev ? "" : "NOT_ABOVE_PREVIOUS;") + (vsI00 ? "" : "NOT_ABOVE_I00;") + (vsNull ? "" : "NOT_ABOVE_SHUFFLED_LABEL_CONTROL;"));
        h.put("blockExposures",(long)ledger.exposures(task.block)); h.put("controlExposures",(long)ledger.controlExposures(task.block));
        return new Result(h);
    }

    private static ReplayEvaluatorR27.Trace read(Task task, Policy policy, EvalContextR27 ctx, HoldoutLedgerR27 ledger, Evidence evidence) {
        ReplayEvaluatorR27.Trace cached = evidence.load(task,policy);
        if (cached != null) {
            if (!ledger.confirmed(policy.experimentId,task.datasetHash,task.block) || cached.position != policy.spec().position ||
                    Double.doubleToLongBits(cached.temperature) != Double.doubleToLongBits(policy.temperature) || cached.from > task.from || cached.to != task.to)
                throw new IllegalStateException("R30_CONFIRM_INVALID_CACHED_EVIDENCE");
            return cached;
        }
        if (!policy.control && ledger.remaining(task.block) == 0 && !ledger.confirmed(policy.experimentId,task.datasetHash,task.block))
            throw new IllegalStateException("R30_CONFIRM_BUDGET_EXHAUSTED");
        EvalContextR27 view = policy.shuffled ? new EvalContextR27(ctx.cache,ctx.split,ReplayEvaluatorR27.shuffledLabels(ctx.data,ctx.split.trainFrom,ctx.split.selectTo,policy.labelSeed)) : ctx;
        ReplayEvaluatorR27.Trace trace = ReplayEvaluatorR27.confirmTrace(policy.spec(),view,policy.temperature,ledger,policy.experimentId,policy.control);
        evidence.save(task,policy,trace);
        return trace;
    }
    private static Object finite(double n) { return Double.isFinite(n) ? n : null; }
    private static String currentSource() { return com.ethk3.novaonepro.BuildConfig.SOURCE_COMMIT_R26; }
}
