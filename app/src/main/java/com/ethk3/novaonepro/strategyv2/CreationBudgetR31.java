package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/**
 * R31-B07-C03: finite creation budget of one evolution loop. It is a deterministic ledger, persisted in the loop
 * snapshot, so an allocation, its seed, the funnel and operator credit are identical before and after a restart.
 *
 * <ul>
 * <li>Allocation keeps a minimum exploration for every available family inside the round budget; a family that
 * cannot run carries its dependency reason instead of silently starving. Device pressure shrinks the slice,
 * never removes the minimum of a family.</li>
 * <li>The funnel is one row per family: attempts / contract_valid / unique / duplicate / replayed / dev_survivor /
 * confirm_queued, plus deferred and cost units, so a large attempt count cannot hide where candidates stop.</li>
 * <li>Operator credit is paired development gain minus cost, booked once per parent/child experiment key; a sparse
 * operator (support below {@link #MIN_CREDIT_SUPPORT}) stays at the exploration prior (0), never a fabricated gain.</li>
 * <li>Replay admission is charged in deterministic cost units (program size x evaluated rows); the generation total
 * never exceeds {@link #generationCostUnits} and no single slice exceeds {@link #sliceCostUnits}, so an expensive,
 * low-value candidate is deferred with a reason while research continues in the next generation.</li>
 * </ul>
 */
public final class CreationBudgetR31 {
    public static final String VERSION = "R31_CREATION_BUDGET_V1";
    public static final int MIN_CREDIT_SUPPORT = 3;
    public static final String[] STAGES = {"attempts", "contractValid", "unique", "duplicate", "replayed", "deferred", "devSurvivor", "confirmQueued", "costUnits", "rounds"};

    public final long seed;
    public final long generationCostUnits, sliceCostUnits;
    private final Map<String, long[]> funnel = new TreeMap<>();
    private final Map<String, String> dependency = new TreeMap<>();
    private final Map<String, double[]> operator = new TreeMap<>();   // {sum of net gain, support}
    private final Set<String> creditedExperiments = new TreeSet<>();
    private long spentThisGeneration, deferredTotal, duplicateCreditRefusals;
    private int allocationGeneration = -1;
    private final Map<String, Integer> lastAllocation = new TreeMap<>();

    public CreationBudgetR31(long seed, long generationCostUnits, long sliceCostUnits) {
        if (generationCostUnits <= 0 || sliceCostUnits <= 0 || sliceCostUnits > generationCostUnits) throw new IllegalArgumentException("R31_CREATION_BUDGET_LIMITS");
        this.seed = seed; this.generationCostUnits = generationCostUnits; this.sliceCostUnits = sliceCostUnits;
    }

    private long[] row(String family) {
        long[] r = funnel.get(family);
        if (r == null) { r = new long[STAGES.length]; funnel.put(family, r); }
        return r;
    }
    private static int stage(String name) { for (int i = 0; i < STAGES.length; i++) if (STAGES[i].equals(name)) return i; throw new IllegalArgumentException("R31_FUNNEL_STAGE:" + name); }
    public void record(String family, String stage, long amount) { if (amount < 0) throw new IllegalArgumentException("R31_FUNNEL_NEGATIVE"); row(family == null ? "UNKNOWN" : family)[stage(stage)] += amount; }
    public long count(String family, String stage) { long[] r = funnel.get(family); return r == null ? 0 : r[stage(stage)]; }
    public long total(String stage) { int i = stage(stage); long n = 0; for (long[] r : funnel.values()) n += r[i]; return n; }
    public String dependencyReason(String family) { return dependency.getOrDefault(family, ""); }

    /**
     * Allocates `total` proposals of one generation. Each available family first receives `minimum` (at least one
     * under pressure), the remainder follows completed credit (bounded at 4x minimum per family) and then a fixed
     * seeded rotation, so the result depends only on (seed, generation, inputs). `pressure` in [0,1) shrinks the
     * total proportionally; it never removes a family. Without `fill` the unassigned remainder is left to the
     * operator stage (mutation/crossover/fusion/experts) of the same generation.
     */
    public Map<String, Integer> allocate(int generation, List<String> families, Map<String, String> unavailable, Map<String, Integer> credit, int total, int minimum, double pressure, boolean fill) {
        if (!(pressure >= 0 && pressure < 1)) throw new IllegalArgumentException("R31_CREATION_PRESSURE");
        Map<String, Integer> out = new LinkedHashMap<>(); List<String> available = new ArrayList<>();
        for (String f : families) {
            String reason = unavailable == null ? null : unavailable.get(f);
            if (reason != null && !reason.isEmpty()) { dependency.put(f, reason); out.put(f, 0); row(f); }
            else { dependency.remove(f); available.add(f); }
        }
        int floor = Math.max(1, (int) Math.floor(minimum * (1 - pressure)));
        int budget = Math.max(floor * available.size(), (int) Math.floor(total * (1 - pressure)));
        for (String f : available) out.put(f, floor);
        int rest = budget - floor * available.size(); long creditSum = 0;
        if (credit != null) for (Integer c : credit.values()) creditSum += Math.max(0, c == null ? 0 : c); // denominator: every credited family, control included
        int remaining = rest;
        if (creditSum > 0) for (String f : available) {
            int bonus = (int) Math.min(Math.min(floor * 4L, remaining), Math.round(rest * Math.max(0, credit.getOrDefault(f, 0)) / (double) creditSum));
            out.put(f, out.get(f) + bonus); remaining -= bonus;
        }
        int assigned = 0; for (String f : available) assigned += out.get(f);
        if (fill && !available.isEmpty()) {
            StatsR27.SplitMix rotation = new StatsR27.SplitMix(seed ^ (generation * 0x9E3779B97F4A7C15L));
            int start = rotation.nextInt(available.size());
            for (int i = 0; assigned < budget; i++, assigned++) { String f = available.get((start + i) % available.size()); out.put(f, out.get(f) + 1); }
        }
        for (String f : available) record(f, "rounds", 1);
        allocationGeneration = generation; lastAllocation.clear(); lastAllocation.putAll(out); spentThisGeneration = 0;
        return Collections.unmodifiableMap(out);
    }
    public Map<String, Integer> lastAllocation() { return Collections.unmodifiableMap(lastAllocation); }
    public int allocationGeneration() { return allocationGeneration; }

    /** Deterministic replay cost of one candidate. */
    public static long costUnits(PolicySpecR27 spec, int rows) { return (long) Math.max(1, spec.ast.size()) * Math.max(1, rows); }
    public void beginGeneration() { spentThisGeneration = 0; }
    /** Charges a candidate if it fits the slice and the remaining generation budget; otherwise records a deferral. */
    public boolean admit(String family, long units) {
        if (units <= 0) throw new IllegalArgumentException("R31_CREATION_COST");
        if (units > sliceCostUnits || spentThisGeneration + units > generationCostUnits) { record(family, "deferred", 1); deferredTotal++; return false; }
        spentThisGeneration += units; record(family, "replayed", 1); record(family, "costUnits", units); return true;
    }
    public long spentThisGeneration() { return spentThisGeneration; }
    public long deferredTotal() { return deferredTotal; }

    /** Books one completed paired experiment. Returns false (and adds nothing) for a repeated experiment key. */
    public boolean creditOperator(String op, String parentHash, String childHash, String windowKey, double pairedGain, double costPenalty) {
        if (!Double.isFinite(pairedGain) || !Double.isFinite(costPenalty) || costPenalty < 0) throw new IllegalArgumentException("R31_OPERATOR_CREDIT_VALUE");
        String key = op + "|" + parentHash + "|" + childHash + "|" + windowKey;
        if (!creditedExperiments.add(key)) { duplicateCreditRefusals++; return false; }
        double[] c = operator.get(op); if (c == null) { c = new double[2]; operator.put(op, c); }
        c[0] += pairedGain - costPenalty; c[1]++;
        return true;
    }
    /** Mean net gain once supported; the exploration prior (0) while sparse. */
    public double operatorCredit(String op) { double[] c = operator.get(op); return c == null || c[1] < MIN_CREDIT_SUPPORT ? 0 : c[0] / c[1]; }
    public long operatorSupport(String op) { double[] c = operator.get(op); return c == null ? 0 : (long) c[1]; }
    public long duplicateCreditRefusals() { return duplicateCreditRefusals; }

    /** Operator weights modulated by supported net credit; sparse operators keep their prior weight. A fixed 25%
     * exploration mass is always retained, so no operator is permanently starved. */
    public double[] weighted(double[] prior, String[] ops) {
        if (prior.length != ops.length) throw new IllegalArgumentException("R31_OPERATOR_WEIGHT_SHAPE");
        double[] raw = prior.clone(); boolean changed = false;
        for (int i = 0; i < ops.length; i++) if (operatorSupport(ops[i]) >= MIN_CREDIT_SUPPORT) { raw[i] *= 1 + Math.max(-.5, Math.min(2, operatorCredit(ops[i]) * 10)); changed = true; }
        if (!changed) return prior;
        double sum = 0; for (double w : raw) sum += Math.max(.001, w);
        double[] out = new double[raw.length]; for (int i = 0; i < out.length; i++) out[i] = .25 / out.length + .75 * Math.max(.001, raw[i]) / sum;
        return out;
    }

    /** Non-champion members without an active job, certificate or audit reference are retired. */
    public static List<String> retirable(Collection<String> members, String championHash, Set<String> referenced) {
        List<String> out = new ArrayList<>();
        for (String h : members) if (!h.equals(championHash) && (referenced == null || !referenced.contains(h))) out.add(h);
        return out;
    }

    public Map<String, Object> json() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("version", VERSION); m.put("seed", seed); m.put("generationCostUnits", generationCostUnits); m.put("sliceCostUnits", sliceCostUnits);
        Map<String, Object> f = new LinkedHashMap<>();
        for (Map.Entry<String, long[]> e : funnel.entrySet()) { Map<String, Object> r = new LinkedHashMap<>(); for (int i = 0; i < STAGES.length; i++) r.put(STAGES[i], e.getValue()[i]); f.put(e.getKey(), r); }
        m.put("funnel", f); m.put("dependency", new LinkedHashMap<String, Object>(dependency));
        Map<String, Object> o = new LinkedHashMap<>(); for (Map.Entry<String, double[]> e : operator.entrySet()) o.put(e.getKey(), Arrays.asList((Object) e.getValue()[0], (Object) e.getValue()[1])); m.put("operator", o);
        m.put("creditedExperiments", new ArrayList<Object>(creditedExperiments));
        m.put("spentThisGeneration", spentThisGeneration); m.put("deferredTotal", deferredTotal); m.put("duplicateCreditRefusals", duplicateCreditRefusals);
        m.put("allocationGeneration", (long) allocationGeneration);
        Map<String, Object> a = new LinkedHashMap<>(); for (Map.Entry<String, Integer> e : lastAllocation.entrySet()) a.put(e.getKey(), (long) e.getValue()); m.put("lastAllocation", a);
        return m;
    }
    @SuppressWarnings("unchecked")
    public static CreationBudgetR31 restore(Object raw) {
        if (!(raw instanceof Map)) throw new IllegalArgumentException("R31_CREATION_BUDGET_SNAPSHOT");
        Map<String, Object> m = (Map<String, Object>) raw;
        if (!VERSION.equals(m.get("version"))) throw new IllegalArgumentException("R31_CREATION_BUDGET_VERSION");
        CreationBudgetR31 b = new CreationBudgetR31(((Number) m.get("seed")).longValue(), ((Number) m.get("generationCostUnits")).longValue(), ((Number) m.get("sliceCostUnits")).longValue());
        for (Map.Entry<String, Object> e : ((Map<String, Object>) m.get("funnel")).entrySet()) { long[] r = b.row(e.getKey()); Map<String, Object> v = (Map<String, Object>) e.getValue(); for (int i = 0; i < STAGES.length; i++) r[i] = ((Number) v.get(STAGES[i])).longValue(); }
        for (Map.Entry<String, Object> e : ((Map<String, Object>) m.get("dependency")).entrySet()) b.dependency.put(e.getKey(), (String) e.getValue());
        for (Map.Entry<String, Object> e : ((Map<String, Object>) m.get("operator")).entrySet()) { List<Object> v = (List<Object>) e.getValue(); b.operator.put(e.getKey(), new double[]{((Number) v.get(0)).doubleValue(), ((Number) v.get(1)).doubleValue()}); }
        for (Object k : (List<Object>) m.get("creditedExperiments")) b.creditedExperiments.add((String) k);
        b.spentThisGeneration = ((Number) m.get("spentThisGeneration")).longValue(); b.deferredTotal = ((Number) m.get("deferredTotal")).longValue(); b.duplicateCreditRefusals = ((Number) m.get("duplicateCreditRefusals")).longValue();
        b.allocationGeneration = ((Number) m.get("allocationGeneration")).intValue();
        for (Map.Entry<String, Object> e : ((Map<String, Object>) m.get("lastAllocation")).entrySet()) b.lastAllocation.put(e.getKey(), ((Number) e.getValue()).intValue());
        return b;
    }
}
