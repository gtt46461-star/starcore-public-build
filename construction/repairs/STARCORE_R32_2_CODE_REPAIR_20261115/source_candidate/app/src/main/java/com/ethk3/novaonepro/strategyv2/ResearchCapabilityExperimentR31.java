package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/**
 * R31-B07-C05: whole-factory detection-power and false-alarm experiment on synthetic streams.
 *
 * <p>A protocol is registered first (arms, every seed, rows, segment boundaries, target position, signal strength,
 * false-alarm budget and power floor) and its hash binds every trial. Each trial runs the same production stages:
 * every candidate passes {@link BirthGateR27#candidateContract}, development selection replays every candidate with
 * {@link ReplayEvaluatorR27} (all candidates counted, not only the winner), the winner needs a positive independent
 * confirmation segment, and only then feeds the later future segment to {@link SequentialQualificationR27} — the same
 * gate the live pool uses, in its own experiment namespace. A trial is DETECTED, NOT_DETECTED (statistical) or FAILED
 * (fault); faults and non-detections are counted separately. A summary is COMPLETE only when every registered trial
 * is present and none faulted. Synthetic records never carry REAL permission and cannot be written to the natural
 * future-evidence namespace.</p>
 */
public final class ResearchCapabilityExperimentR31 {
    private ResearchCapabilityExperimentR31() {}
    public static final String VERSION = "R31_RESEARCH_CAPABILITY_V1", NAMESPACE = "SYNTHETIC_CAPABILITY_R31", NATURAL_FUTURE_NAMESPACE = "NATURAL_FUTURE";
    /** NULL_UNIFORM: independent uniform permutations. NULL_TIME_SHUFFLED: the signal stream with its rows shuffled in
     * time (marginals kept, dependence destroyed). SIGNAL: position q repeats its previous number with probability
     * `strength`. SIGNAL_DISABLED: the paired stream of the same seed with strength 0. DRIFT: signal in the first half
     * only. GAPS: signal with missing periods. */
    public enum Arm { NULL_UNIFORM, NULL_TIME_SHUFFLED, SIGNAL, SIGNAL_DISABLED, DRIFT, GAPS }
    public enum Status { DETECTED, NOT_DETECTED, FAILED }

    public static final class Protocol {
        public final String id; public final Map<Arm, long[]> seeds; public final int rows, devEnd, confirmEnd, position; public final double strength, farBudget, powerFloor;
        private Protocol(String id, Map<Arm, long[]> seeds, int rows, int devEnd, int confirmEnd, int position, double strength, double farBudget, double powerFloor) {
            this.id = id; Map<Arm, long[]> copy = new EnumMap<>(Arm.class); for (Map.Entry<Arm, long[]> e : seeds.entrySet()) copy.put(e.getKey(), e.getValue().clone());
            this.seeds = Collections.unmodifiableMap(copy); this.rows = rows; this.devEnd = devEnd; this.confirmEnd = confirmEnd; this.position = position;
            this.strength = strength; this.farBudget = farBudget; this.powerFloor = powerFloor;
        }
        public String hash() {
            StringBuilder b = new StringBuilder(VERSION).append('|').append(id).append('|').append(rows).append('|').append(devEnd).append('|').append(confirmEnd).append('|').append(position)
                    .append('|').append(strength).append('|').append(farBudget).append('|').append(powerFloor);
            for (Map.Entry<Arm, long[]> e : seeds.entrySet()) b.append('|').append(e.getKey()).append('=').append(Arrays.toString(e.getValue()));
            return StatsR27.sha256(b.toString());
        }
        public int planned() { int n = 0; for (long[] s : seeds.values()) n += s.length; return n; }
    }
    /** Freezes a protocol before any trial runs. */
    public static Protocol register(String id, Map<Arm, long[]> seeds, int rows, int devEnd, int confirmEnd, int position, double strength, double farBudget, double powerFloor) {
        if (id == null || id.isEmpty() || seeds == null || seeds.isEmpty()) throw new IllegalArgumentException("R31_CAPABILITY_PROTOCOL");
        if (!(64 < devEnd && devEnd < confirmEnd && confirmEnd < rows) || position < 1 || position > 10) throw new IllegalArgumentException("R31_CAPABILITY_SEGMENTS");
        if (!(strength > 0 && strength < 1) || !(farBudget > 0 && farBudget < 1) || !(powerFloor > 0 && powerFloor < 1)) throw new IllegalArgumentException("R31_CAPABILITY_BUDGETS");
        for (long[] s : seeds.values()) { Set<Long> unique = new HashSet<>(); for (long x : s) if (!unique.add(x)) throw new IllegalArgumentException("R31_CAPABILITY_DUPLICATE_SEED:" + x); }
        return new Protocol(id, seeds, rows, devEnd, confirmEnd, position, strength, farBudget, powerFloor);
    }

    /** Draw rows (each a permutation of 1..10 over the ten positions) for one arm and seed. */
    public static int[][] generateStream(Arm arm, long seed, int rows, int position, double strength) {
        StatsR27.SplitMix r = new StatsR27.SplitMix(seed * 0x9E3779B97F4A7C15L + 31);
        double eps = arm == Arm.NULL_UNIFORM || arm == Arm.SIGNAL_DISABLED ? 0 : strength;
        int q = position - 1; int[][] d = new int[rows][];
        for (int t = 0; t < rows; t++) {
            int[] v = ReplayDatasetR27.randomPermutation(r);
            boolean on = eps > 0 && t > 0 && (arm != Arm.DRIFT || t < rows / 2);
            if (on && r.nextDouble() < eps) { int want = d[t - 1][q]; for (int j = 0; j < 10; j++) if (v[j] == want) { v[j] = v[q]; v[q] = want; break; } }
            d[t] = v;
        }
        if (arm == Arm.NULL_TIME_SHUFFLED) for (int t = rows - 1; t > 0; t--) { int j = r.nextInt(t + 1); int[] x = d[t]; d[t] = d[j]; d[j] = x; }
        return d;
    }
    static ReplayDatasetR27 dataset(Arm arm, int[][] draws) {
        List<ReplayDatasetR27.Row> rows = new ArrayList<>(draws.length); long ordinal = 2_000_000L;
        for (int t = 0; t < draws.length; t++) { if (arm == Arm.GAPS && t > 0 && t % 97 == 0) ordinal += 2; rows.add(new ReplayDatasetR27.Row(String.format(Locale.ROOT, "C%011d", ordinal), ordinal, draws[t])); ordinal++; }
        return ReplayDatasetR27.of(rows);
    }

    public static final class TrialRecord {
        public final String protocolHash, namespace, selectedHash, evidenceHash, gateState, faultReason; public final Arm arm; public final long seed; public final Status status;
        public final int candidates, contractRejected; public final boolean realPermission = false;
        TrialRecord(String protocolHash, Arm arm, long seed, Status status, int candidates, int contractRejected, String selectedHash, String evidenceHash, String gateState, String faultReason) {
            this.protocolHash = protocolHash; this.namespace = NAMESPACE; this.arm = arm; this.seed = seed; this.status = status; this.candidates = candidates; this.contractRejected = contractRejected;
            this.selectedHash = selectedHash == null ? "" : selectedHash; this.evidenceHash = evidenceHash == null ? "" : evidenceHash; this.gateState = gateState == null ? "" : gateState; this.faultReason = faultReason == null ? "" : faultReason;
        }
        public Map<String, Object> json() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("protocolHash", protocolHash); m.put("namespace", namespace); m.put("arm", arm.name()); m.put("seed", seed); m.put("status", status.name());
            m.put("candidates", (long) candidates); m.put("contractRejected", (long) contractRejected); m.put("selectedHash", selectedHash); m.put("evidenceHash", evidenceHash);
            m.put("gate", gateState); m.put("faultReason", faultReason); m.put("realPermission", realPermission);
            return m;
        }
    }
    public interface Pipeline { TrialRecord run(Protocol p, Arm arm, long seed, int[][] draws) throws Exception; }

    /** Candidate programs of the factory for target position q. */
    public static List<PolicySpecR27> candidates(int q) {
        List<PolicySpecR27> out = new ArrayList<>();
        for (PolicyAstR27.Node n : new PolicyAstR27.Node[]{PolicyAstR27.leaf("trans1"), PolicyAstR27.leaf("repeat"), PolicyAstR27.leaf("freq", 1, 128), PolicyAstR27.leaf("gap", 4)})
            out.add(new PolicySpecR27(PolicyAstR27.op("zscore", n), q, PolicySpecR27.Rule.ALWAYS, 0, 0, 1, false, 0.01, 0.001, IdeaFamilyR27.meta("I00", "R31_CAPABILITY_FACTORY", 0, 0, "synthetic capability trial; no REAL permission")));
        return out;
    }
    static double meanU(ReplayEvaluatorR27.Trace tr, int from, int to) { double s = 0; int n = 0; for (int t = from; t < to; t++) if (!tr.skipped[t]) { s += tr.u[t]; n++; } return n == 0 ? Double.NEGATIVE_INFINITY : s / n; }

    /** Production stages: birth contract, development replay of every candidate, independent confirmation, sequential gate. */
    public static final Pipeline PRODUCTION_STAGES = (p, arm, seed, draws) -> {
        ReplayDatasetR27 data = dataset(arm, draws); int rejected = 0;
        try (FeatureCacheR27 cache = new FeatureCacheR27(data)) {
            PolicySpecR27 best = null; ReplayEvaluatorR27.Trace bestTrace = null; double bestDev = Double.NEGATIVE_INFINITY; List<PolicySpecR27> all = candidates(p.position);
            for (PolicySpecR27 spec : all) {
                if (!BirthGateR27.candidateContract(spec).ok) { rejected++; continue; }
                ReplayEvaluatorR27.Trace tr = ReplayEvaluatorR27.run(spec, cache, data.draws, spec.temperature, 64, data.size());
                double dev = meanU(tr, 64, p.devEnd);
                if (dev > bestDev + 1e-12) { bestDev = dev; best = spec; bestTrace = tr; }
            }
            if (best == null) return new TrialRecord(p.hash(), arm, seed, Status.FAILED, all.size(), rejected, null, null, null, "NO_CANDIDATE_PASSED_BIRTH_CONTRACT");
            double confirm = meanU(bestTrace, p.devEnd, p.confirmEnd);
            StringBuilder evidence = new StringBuilder(best.policyHash()).append('|').append(Double.doubleToLongBits(bestDev)).append('|').append(Double.doubleToLongBits(confirm));
            if (!(confirm > 0)) return new TrialRecord(p.hash(), arm, seed, Status.NOT_DETECTED, all.size(), rejected, best.policyHash(), StatsR27.sha256(evidence.toString()), "CONFIRM_NOT_POSITIVE", null);
            SequentialQualificationR27.State gate = new SequentialQualificationR27.State(best.policyHash(), 1, 1.0);
            for (int t = p.confirmEnd; t < data.size() && SequentialQualificationR27.ACCUMULATING.equals(gate.state); t++) {
                if (bestTrace.skipped[t]) continue;
                gate.observe(data.ordinals[t], bestTrace.act[t], bestTrace.hit[t] == 1); evidence.append('|').append(t).append(bestTrace.hit[t]);
            }
            Status status = SequentialQualificationR27.QUALIFIED.equals(gate.state) ? Status.DETECTED : Status.NOT_DETECTED;
            return new TrialRecord(p.hash(), arm, seed, status, all.size(), rejected, best.policyHash(), StatsR27.sha256(evidence.toString()), gate.state, null);
        }
    };

    /** Runs one registered trial; a thrown fault is recorded as FAILED with its reason, never dropped. */
    public static TrialRecord runPipeline(Protocol p, Arm arm, long seed, Pipeline pipeline) {
        boolean registered = false; long[] seeds = p.seeds.get(arm); if (seeds != null) for (long s : seeds) registered |= s == seed;
        if (!registered) throw new IllegalArgumentException("R31_CAPABILITY_TRIAL_NOT_REGISTERED:" + arm + ":" + seed);
        try {
            TrialRecord r = pipeline.run(p, arm, seed, generateStream(arm, seed, p.rows, p.position, p.strength));
            if (r == null || !p.hash().equals(r.protocolHash) || r.arm != arm || r.seed != seed) throw new IllegalStateException("R31_CAPABILITY_RECORD_IDENTITY");
            return r;
        } catch (Exception | StackOverflowError fault) {
            return new TrialRecord(p.hash(), arm, seed, Status.FAILED, 0, 0, null, null, null, fault.getClass().getSimpleName() + ":" + fault.getMessage());
        }
    }
    public static List<TrialRecord> runAll(Protocol p, Pipeline pipeline) {
        List<TrialRecord> out = new ArrayList<>();
        for (Map.Entry<Arm, long[]> e : p.seeds.entrySet()) for (long seed : e.getValue()) out.add(runPipeline(p, e.getKey(), seed, pipeline));
        return out;
    }

    /** The natural future-evidence namespace accepts no synthetic record. */
    public static void requireNaturalFutureNamespace(TrialRecord r, String targetNamespace) {
        if (NATURAL_FUTURE_NAMESPACE.equals(targetNamespace) && (NAMESPACE.equals(r.namespace) || !r.realPermission)) throw new IllegalStateException("R31_SYNTHETIC_HAS_NO_REAL_PERMISSION");
    }

    public static final class Summary {
        public final String status, protocolHash; public final Map<Arm, long[]> counts; // {planned, present, detected, notDetected, failed}
        public final long nullTrials, falseAlarms, signalTrials, detections; public final double farUpper95, powerLower95; public final boolean farWithinBudget, powerWithinRegistered;
        public final List<String> missing;
        Summary(String status, String hash, Map<Arm, long[]> counts, long nullTrials, long falseAlarms, long signalTrials, long detections, double farUpper, double powerLower, boolean far, boolean power, List<String> missing) {
            this.status = status; protocolHash = hash; this.counts = counts; this.nullTrials = nullTrials; this.falseAlarms = falseAlarms; this.signalTrials = signalTrials; this.detections = detections;
            farUpper95 = farUpper; powerLower95 = powerLower; farWithinBudget = far; powerWithinRegistered = power; this.missing = Collections.unmodifiableList(missing);
        }
    }
    public static Summary summarize(Protocol p, List<TrialRecord> records) {
        Map<Arm, long[]> counts = new EnumMap<>(Arm.class); Map<String, TrialRecord> byKey = new HashMap<>(); List<String> missing = new ArrayList<>(); boolean duplicate = false;
        for (TrialRecord r : records) { if (!p.hash().equals(r.protocolHash)) throw new IllegalArgumentException("R31_CAPABILITY_FOREIGN_RECORD"); duplicate |= byKey.put(r.arm + ":" + r.seed, r) != null; }
        long failed = 0;
        for (Map.Entry<Arm, long[]> e : p.seeds.entrySet()) {
            long[] c = new long[5]; c[0] = e.getValue().length;
            for (long seed : e.getValue()) {
                TrialRecord r = byKey.get(e.getKey() + ":" + seed);
                if (r == null) { missing.add(e.getKey() + ":" + seed); continue; }
                c[1]++; if (r.status == Status.DETECTED) c[2]++; else if (r.status == Status.NOT_DETECTED) c[3]++; else { c[4]++; failed++; }
            }
            counts.put(e.getKey(), c);
        }
        String status = duplicate ? "DUPLICATE_TRIAL" : !missing.isEmpty() ? "INCOMPLETE" : failed > 0 ? "FAULTED" : "COMPLETE";
        long nullTrials = 0, falseAlarms = 0, signalTrials = 0, detections = 0;
        for (Arm a : new Arm[]{Arm.NULL_UNIFORM, Arm.NULL_TIME_SHUFFLED, Arm.SIGNAL_DISABLED}) { long[] c = counts.get(a); if (c != null) { nullTrials += c[2] + c[3]; falseAlarms += c[2]; } }
        long[] s = counts.get(Arm.SIGNAL); if (s != null) { signalTrials = s[2] + s[3]; detections = s[2]; }
        double farUpper = nullTrials == 0 ? 1 : clopperPearsonUpper(falseAlarms, nullTrials, .05), powerLower = signalTrials == 0 ? 0 : clopperPearsonLower(detections, signalTrials, .05);
        boolean complete = "COMPLETE".equals(status);
        return new Summary(status, p.hash(), counts, nullTrials, falseAlarms, signalTrials, detections, farUpper, powerLower, complete && nullTrials > 0 && farUpper <= p.farBudget, complete && signalTrials > 0 && powerLower >= p.powerFloor, missing);
    }
    static double binomialCdf(long k, long n, double p) {
        if (k < 0) return 0; if (k >= n) return 1; if (p <= 0) return 1; if (p >= 1) return 0;
        double sum = 0, logP = Math.log(p), logQ = Math.log1p(-p);
        for (long i = 0; i <= k; i++) sum += Math.exp(logChoose(n, i) + i * logP + (n - i) * logQ);
        return Math.min(1, sum);
    }
    static double logChoose(long n, long k) { double s = 0; for (long i = 1; i <= k; i++) s += Math.log(n - k + i) - Math.log(i); return s; }
    /** One-sided exact (Clopper-Pearson) upper bound: the largest p with P(X <= x | n, p) >= alpha. */
    public static double clopperPearsonUpper(long x, long n, double alpha) {
        if (x >= n) return 1; double lo = x / (double) n, hi = 1;
        for (int i = 0; i < 200; i++) { double mid = (lo + hi) / 2; if (binomialCdf(x, n, mid) > alpha) lo = mid; else hi = mid; }
        return hi;
    }
    public static double clopperPearsonLower(long x, long n, double alpha) { return x <= 0 ? 0 : 1 - clopperPearsonUpper(n - x, n, alpha); }
}
