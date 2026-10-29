package com.ethk3.novaonepro.strategyv2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** R27-028/029 interruptible evolution loop (docs/05 5.6). One call to {@link #step()} executes exactly one of nine
 * steps; the full state is serializable ({@link #snapshot()} / {@link #restore}) so that a killed process resumes at
 * the last completed step without re-registering births or lineage. All randomness is drawn from a persisted
 * SplitMix64 state.
 *
 * "Stronger than the previous generation" (docs/05 5.1) is only claimed when, on one sealed H_HOLDOUT block, the
 * paired one-sided 95% lower bound of u(champion)−u(previous champion) is &gt; 0 AND the champion is paired-better
 * than both the I00 random-baseline champion and the shuffled-label (NullEvolutionR27) champion. */
public final class EvolutionLoopR27 {
    public static final String[] STEPS = {"", "DATASET", "BIRTH", "GATE", "REPLAY", "HALVING", "HOLDOUT", "RECORD", "PROMOTE", "MIGRATE_ABLATE"};

    public static final class Config {
        public int births = 128, populationCap = 256, liveCap = 32, i00PerGeneration = 8, minPerFamily = 2, migrateEvery = 5, ablateEvery = 20, derivedEvery = 50;
        public double mutationShare = 0.60, crossoverShare = 0.20, fusionShare = 0.10, expertShare = 0.10, nullShare = 0.10;
        public boolean nullControl = true;
        public long seed = 27;
    }

    /** Persistence target. The Android store wraps each step's calls and the cursor in one write transaction; every
     * call is keyed (policy hash / edge key / generation) so a repeated step cannot duplicate a record. */
    public interface Sink {
        void policy(PolicySpecR27 spec, String state, String reason, int generation, String datasetHash);
        void lineage(String child, String parent, String role, String operator, int generation);
        void score(String policyHash, String datasetHash, ReplayScoreR27 score);
        void generation(Map<String, Object> record);
        void promote(PolicySpecR27 spec, double temperature, int generation, String reason);
        void demote(String policyHash, String reason);
        void expert(ExpertProposalsR27.Proposal p, int generation);
        void ablation(int generation, String championHash, List<Object[]> rows);
        void holdoutLook(long block, String policyHash);
    }
    public interface Clock { long now(); }

    public static final IdeaFamilyR27[] FAMILIES = {new IdeaI00RandomBaselineR27(), new IdeaI01FrequencyReversionR27(), new IdeaI02GapCycleR27(), new IdeaI03TransitionChainR27(),
            new IdeaI04CrossPositionR27(), new IdeaI05PermutationConsistencyR27(), new IdeaI06RegimeSwitchR27(), new IdeaI07FragmentRetrievalR27(), new IdeaI08NeuralRepresentationR27(),
            new IdeaI09SymbolicFormulaR27(), new IdeaI10FailureRepairR27(), new IdeaI11ComplementaryEnsembleR27(), new IdeaI12LegacyAssetR27(), new IdeaI13SelectorOnlyR27()};

    /** A population member; its trace is transient and rebuilt deterministically when missing. */
    public static final class Member implements IslandModelR27.Member {
        public PolicySpecR27 spec; public double tau, u, lcb, hr, actRate, brier; public String island; public int[] sig; public int cell;
        transient ReplayEvaluatorR27.Trace trace;
        public String island() { return island; } public double selectU() { return u; } public String hash() { return spec.policyHash(); }
    }
    static final class Pending { PolicySpecR27 spec; String state = "NEW", reason = ""; int[] sig; double tau = Double.NaN; boolean structural; transient ReplayEvaluatorR27.Trace trace; }

    // ------------------------------------------------------------------ state
    public final Config cfg; private Sink sink; private final Clock clock;
    /** The Android runtime binds a fresh per-step sink (one atomic batch per step). */
    public void setSink(Sink s) { sink = s; }
    private EvalContextR27 ctx; private BirthGateR27 gate; private final HoldoutLedgerR27 ledger = new HoldoutLedgerR27();
    public int generation, step = 1; public final StatsR27.SplitMix rng;
    public final List<Member> population = new ArrayList<>();
    public final EliteArchiveR27 archive = new EliteArchiveR27();
    private List<Pending> pending = new ArrayList<>();
    public PolicySpecR27 champion, previousChampion; public double championTau = 1, previousTau = 1;
    public final List<PolicySpecR27> live = new ArrayList<>(); public final Map<String, Double> liveTau = new HashMap<>();
    public final NullEvolutionR27 nullEvolution = new NullEvolutionR27();
    public PolicySpecR27 i00Champion; public double i00Tau = 1;
    public final Map<String, long[]> counters = new LinkedHashMap<>();
    public final Map<String, Integer> familySurvivors = new HashMap<>();
    public double[] opWeights = MutationOpsR27.defaultWeights();
    public int gateMaxDepth = BirthGateR27.MAX_DEPTH, depthCooldown, generationsWithoutVerified;
    public Map<String, Object> lastRecord = new LinkedHashMap<>();
    public List<PolicySpecR27> legacySeeds = Collections.emptyList();
    public Map<String, Double> leafWeight = new HashMap<>();
    public List<PolicyAstR27.Node> derivedLeaves = new ArrayList<>();
    private long startedAt;
    private final Map<String, Object> genStats = new LinkedHashMap<>();
    public static final String[] COUNTERS = {"attempts", "uniquePolicies", "structuralCandidates", "behaviorNovel", "researchAccepted", "prospectiveQualified", "verifiedImprovements"};

    public EvolutionLoopR27(Config cfg, FeatureCacheR27 cache, Sink sink, Clock clock) {
        this.cfg = cfg; this.sink = sink; this.clock = clock; this.rng = new StatsR27.SplitMix(cfg.seed);
        for (String c : COUNTERS) counters.put(c, new long[1]);
        bind(cache);
    }

    private void bind(FeatureCacheR27 cache) {
        SplitR27 split = SplitR27.of(cache.data, ledger.degradedBlocks());
        ctx = new EvalContextR27(cache, split, null);
        gate = new BirthGateR27(cache, split.trainTo); gate.setMaxDepth(gateMaxDepth);
        for (Member m : population) if (m.sig != null) gate.register(m.hash(), m.sig);
    }
    /** daily-close: rebinding to a new dataset happens only between generations (step 1). */
    public void rebind(FeatureCacheR27 cache) {
        if (step != 1) throw new IllegalStateException("R27_REBIND_MID_GENERATION");
        bind(cache);
        for (Member m : population) { m.trace = null; m.tau = ctx.temperature(m.spec); refresh(m); }
    }
    public EvalContextR27 context() { return ctx; }
    // Package-private hooks for HOST tests (H05/H12).
    void gateRegister(String hash, int[] sig) { gate.register(hash, sig); }
    void addPendingForTest(PolicySpecR27 s) { Pending p = new Pending(); p.spec = s; p.structural = s.meta == null || MutationOpsR27.structural(s.meta.operator); pending.add(p); }
    String pendingState(String hash) { for (Pending p : pending) if (p.spec.policyHash().equals(hash)) return p.state; return null; }
    public HoldoutLedgerR27 ledger() { return ledger; }
    private void count(String k, long d) { counters.get(k)[0] += d; }
    public long counter(String k) { return counters.get(k)[0]; }

    // ------------------------------------------------------------------ steps
    public void runGeneration() { do step(); while (step != 1); }

    public void step() {
        switch (step) {
            case 1: stepDataset(); break;
            case 2: stepBirth(); break;
            case 3: stepGate(); break;
            case 4: stepReplay(); break;
            case 5: stepHalving(); break;
            case 6: stepHoldout(); break;
            case 7: stepRecord(); break;
            case 8: stepPromote(); break;
            case 9: stepMigrateAblate(); break;
            default: throw new IllegalStateException("R27_EVOLUTION_STEP:" + step);
        }
        step = step == 9 ? 1 : step + 1;
        if (step == 1) generation++;
    }

    void stepDataset() {
        startedAt = clock.now(); genStats.clear(); pending = new ArrayList<>();
        genStats.put("datasetHash", ctx.data.datasetHash); genStats.put("split", ctx.split.describe());
    }

    IdeaFamilyR27.Context ideaContext() {
        IdeaFamilyR27.Context c = new IdeaFamilyR27.Context();
        c.generation = generation; c.maxDepth = gateMaxDepth; c.champion = champion; c.archive = archive.members(); c.legacySeeds = legacySeeds;
        c.leafWeight = leafWeight; c.derivedLeaves = derivedLeaves;
        Member ch = find(champion); c.championActRate = ch == null ? 0.15 : Math.max(0.01, ch.actRate);
        c.failureCluster = failureCluster();
        return c;
    }

    void stepBirth() {
        IdeaFamilyR27.Context ic = ideaContext(); int total = cfg.births;
        // Fair quota: every family I01..I13 gets the minimum; a bonus (≤4× minimum) follows recent survivors.
        int minTotal = cfg.minPerFamily * 13, bonusPool = Math.max(0, Math.min(total / 4, total - minTotal));
        int survivors = 0; for (int v : familySurvivors.values()) survivors += v;
        List<PolicySpecR27> births = new ArrayList<>();
        for (int f = 1; f <= 13; f++) {
            int n = cfg.minPerFamily;
            if (survivors > 0) n += Math.min(cfg.minPerFamily * 4, (int) Math.round(bonusPool * familySurvivors.getOrDefault(FAMILIES[f].id(), 0) / (double) survivors));
            for (int i = 0; i < n; i++) births.add(FAMILIES[f].generate(rng, ic));
        }
        int rest = Math.max(0, total - births.size());
        boolean haveParents = !population.isEmpty();
        int mut = haveParents ? (int) Math.round(rest * cfg.mutationShare / 1.0) : 0, cx = haveParents ? (int) Math.round(rest * cfg.crossoverShare) : 0;
        int fu = archive.filled() >= 2 ? (int) Math.round(rest * cfg.fusionShare) : 0, ex = haveParents ? (int) Math.round(rest * cfg.expertShare) : 0;
        for (int i = 0; i < mut; i++) {
            Member p = population.get(rng.nextInt(population.size()));
            IdeaFamilyR27 fam = family(p.island);
            PolicySpecR27 c = rng.chance(0.3) && fam != null ? fam.mutate(p.spec, rng, ic) : null;
            if (c == null) c = MutationOpsR27.mutate(p.spec, MutationOpsR27.pick(opWeights, rng), rng, ic, this::pruneContribution);
            if (c != null) births.add(c);
        }
        for (int i = 0; i < cx; i++) {
            Member a = population.get(rng.nextInt(population.size())), b = population.get(rng.nextInt(population.size()));
            PolicySpecR27 c = rng.chance(0.6) ? CrossoverOpsR27.subtree(a.spec, b.spec, rng, generation) : CrossoverOpsR27.input(a.spec, b.spec, rng, generation);
            if (c != null) births.add(c);
        }
        for (int i = 0; i < fu; i++) { PolicySpecR27 c = FusionOpsR27.fuse(archive.members(), ctx, rng, generation); if (c != null) births.add(c); }
        for (int i = 0; i < ex; i++) {
            String e = ExpertProposalsR27.EXPERTS[rng.nextInt(16)][0];
            for (ExpertProposalsR27.Proposal p : ExpertProposalsR27.propose(e, ic, rng)) { sink.expert(p, generation); if (p.spec != null) births.add(p.spec); }
        }
        // Fill any rounding/operator shortfall from generators so every generation has the configured size.
        while (births.size() < total) births.add(FAMILIES[1 + rng.nextInt(13)].generate(rng, ic));
        for (PolicySpecR27 s : births) { Pending p = new Pending(); p.spec = s; p.structural = s.meta == null || MutationOpsR27.structural(s.meta.operator); pending.add(p); }
        count("attempts", births.size());
        genStats.put("births", (long) births.size());
    }

    void stepGate() {
        Set<String> existing = new HashSet<>(); for (Member m : population) existing.add(m.hash());
        Set<String> seenThisGen = new HashSet<>();
        int rejected = 0, noop = 0, dup = 0, structural = 0; Map<String, Integer> reasons = new LinkedHashMap<>();
        for (Pending p : pending) {
            String h = p.spec.policyHash();
            if (existing.contains(h) || !seenThisGen.add(h)) { p.state = "DUPLICATE_BEHAVIOR"; p.reason = "SAME_POLICY_HASH"; dup++; continue; }
            BirthGateR27.Verdict v = gate.check(p.spec);
            if (!v.ok) { p.state = "REJECTED"; p.reason = v.reason + ":" + v.detail; rejected++; reasons.merge(v.reason.name(), 1, Integer::sum); sink.policy(p.spec, "REJECTED", "BIRTH_REJECTED(" + v.reason + "):" + v.detail, generation, ctx.data.datasetHash); continue; }
            p.sig = v.signature;
            if (p.spec.meta != null && p.spec.meta.parents.size() == 1 && !"LEGACY_SEED".equals(p.spec.meta.operator)) {
                int[] parentSig = gate.signatureOf(p.spec.meta.parents.get(0));
                if (parentSig != null && Arrays.equals(parentSig, v.signature)) { p.state = "NO_OP"; p.reason = "SIGNATURE_EQUALS_PARENT"; noop++; continue; }
            }
            String d = gate.duplicateOf(v.signature);
            if (d != null) { p.state = "DUPLICATE_BEHAVIOR"; p.reason = "JACCARD>=0.95:" + d; dup++; continue; }
            p.state = "BORN"; if (p.structural) structural++;
            sink.policy(p.spec, "BORN", p.spec.meta == null ? "" : p.spec.meta.reason, generation, ctx.data.datasetHash);
            if (p.spec.meta != null) for (String parent : p.spec.meta.parents) sink.lineage(h, parent, role(p.spec.meta.operator), p.spec.meta.operator, generation);
        }
        count("uniquePolicies", seenThisGen.size()); count("structuralCandidates", structural); count("behaviorNovel", pending.size() - rejected - noop - dup);
        genStats.put("rejected", (long) rejected); genStats.put("noOp", (long) noop); genStats.put("duplicates", (long) dup); genStats.put("rejectReasons", new LinkedHashMap<String, Object>(reasons));
    }

    static String role(String op) {
        if (op == null) return "PARENT";
        switch (op) { case "F_STACK": return "FUSION_MEMBER"; case "X_SUBTREE": case "X_INPUT": return "CROSSOVER_PARENT"; case "ABI_REBIRTH": return "ABI_REBIRTH"; case "EXPERT_PROPOSAL": return "EXPERT_PARENT"; default: return "PARENT"; }
    }

    /** Optional data-parallel executor for the REPLAY step (the research pool's compute threads). */
    public transient java.util.concurrent.ExecutorService executor;
    public final List<Long> candidateReplayMs = new ArrayList<>();

    void stepReplay() {
        long t0 = System.nanoTime(); candidateReplayMs.clear();
        List<Pending> born = new ArrayList<>(); for (Pending p : pending) if (p.state.equals("BORN")) born.add(p);
        // R27.2: data-parallel replay only while no P0 analysis/settlement is running; otherwise one thread.
        if (executor == null || ResearchWriteQueueR27.p0Writing()) for (Pending p : born) replayOne(p);
        else {
            List<java.util.concurrent.Future<?>> fs = new ArrayList<>();
            for (Pending p : born) fs.add(executor.submit(() -> replayOne(p)));
            for (java.util.concurrent.Future<?> f : fs) try { f.get(); } catch (Exception e) { throw new IllegalStateException("R27_REPLAY_TASK", e); }
        }
        genStats.put("replayMs", (System.nanoTime() - t0) / 1_000_000L);
        List<Long> sorted; synchronized (candidateReplayMs) { sorted = new ArrayList<>(candidateReplayMs); }
        java.util.Collections.sort(sorted);
        genStats.put("candidateReplayP95Ms", sorted.isEmpty() ? null : sorted.get(Math.min(sorted.size() - 1, (int) Math.ceil(sorted.size() * 0.95) - 1)));
    }
    private void replayOne(Pending p) {
        long s = System.nanoTime(); p.tau = ctx.temperature(p.spec); p.trace = ctx.selectTrace(p.spec, p.tau);
        synchronized (candidateReplayMs) { candidateReplayMs.add((System.nanoTime() - s) / 1_000_000L); }
    }

    void stepHalving() {
        List<SuccessiveHalvingR27.Candidate> cands = new ArrayList<>();
        Map<String, Pending> byHash = new HashMap<>();
        for (Pending p : pending) if (p.state.equals("BORN")) {
            if (p.trace == null) { if (Double.isNaN(p.tau)) p.tau = ctx.temperature(p.spec); p.trace = ctx.selectTrace(p.spec, p.tau); }
            cands.add(new SuccessiveHalvingR27.Candidate(p.spec, p.trace, p.tau)); byHash.put(p.spec.policyHash(), p);
        }
        SuccessiveHalvingR27.Result r = SuccessiveHalvingR27.run(cands, ctx.split);
        Map<String, Integer> famSurv = new HashMap<>();
        for (SuccessiveHalvingR27.Candidate c : r.retired) {
            sink.policy(c.spec, "RETIRED", c.retiredRound + ":" + c.retiredReason, generation, ctx.data.datasetHash);
            if (c.select != null) sink.score(c.spec.policyHash(), ctx.data.datasetHash, c.select);
        }
        for (SuccessiveHalvingR27.Candidate c : r.survivors) {
            Pending p = byHash.get(c.spec.policyHash());
            Member m = new Member(); m.spec = c.spec; m.tau = c.temperature; m.trace = c.trace; m.sig = p.sig; m.island = island(c.spec);
            refresh(m); population.add(m); gate.register(m.hash(), m.sig);
            sink.policy(c.spec, "ACTIVE", "SURVIVED_R0_R1_R2", generation, ctx.data.datasetHash); sink.score(m.hash(), ctx.data.datasetHash, c.select);
            famSurv.merge(m.island, 1, Integer::sum);
        }
        count("researchAccepted", r.survivors.size());
        familySurvivors.clear(); familySurvivors.putAll(famSurv);
        // Archive offers and population cap (worst non-elite members leave, archived as RETIRED).
        // Cells are taken against the current champion at offer time so a resumed loop files elites identically (H12).
        for (Member m : population) { m.cell = cellOf(m); archive.offer(m.cell, m.spec, m.u, m.tau); }
        population.sort((a, b) -> a.u != b.u ? Double.compare(b.u, a.u) : a.hash().compareTo(b.hash()));
        int removed = 0;
        for (int pass = 0; pass < 2 && population.size() > cfg.populationCap; pass++)
            for (int i = population.size() - 1; i >= 0 && population.size() > cfg.populationCap; i--) {
                Member m = population.get(i);
                if (pass == 0 && archive.occupies(m.hash())) continue; // elites leave only if nothing else can
                population.remove(i); gate.unregister(m.hash()); removed++; sink.policy(m.spec, "RETIRED", "R3:POPULATION_CAP_WORST_H_SELECT", generation, ctx.data.datasetHash);
            }
        genStats.put("survivors", (long) r.survivors.size()); genStats.put("retired", (long) (r.retired.size() + removed));
        genStats.put("funnel", Arrays.asList((long) r.roundIn[0], (long) r.roundIn[1], (long) r.roundIn[2], (long) r.survivors.size()));
        Map<String, Object> fs = new LinkedHashMap<>(); for (Map.Entry<String, Integer> e : famSurv.entrySet()) fs.put(e.getKey(), (long) e.getValue());
        genStats.put("familySurvivors", fs);
        if (!population.isEmpty()) { Member best = population.get(0); if (champion == null || !best.hash().equals(champion.policyHash())) { previousChampion = champion; previousTau = championTau; champion = best.spec; championTau = best.tau; } }
        // Negative control generation and the I00 random-baseline champion for this generation.
        if (cfg.nullControl) nullEvolution.generation(ctx.cache, ctx.split, FAMILIES, Math.max(4, (int) Math.round(cfg.births * cfg.nullShare)), rng, generation, 26);
        i00Generation();
    }

    void i00Generation() {
        IdeaFamilyR27.Context ic = ideaContext(); double best = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < cfg.i00PerGeneration; i++) {
            PolicySpecR27 s = FAMILIES[0].generate(rng, ic); double tau = ctx.temperature(s);
            ReplayScoreR27 sc = ReplayScoreR27.of(SplitR27.SELECT, ctx.selectTrace(s, tau), ctx.split.selectFrom, ctx.split.selectTo);
            if (sc.pairedUMean > best) { best = sc.pairedUMean; i00Champion = s; i00Tau = tau; }
        }
    }

    void stepHoldout() {
        Map<String, Object> h = new LinkedHashMap<>(); boolean verified = false; String why;
        SplitR27 sp = ctx.split; long block = sp.holdoutBlock;
        if (champion == null) why = "NO_CHAMPION";
        else if (!sp.holdoutAvailable || ledger.degraded(block)) why = "HOLDOUT_UNAVAILABLE_OR_DEGRADED";
        else {
            ReplayEvaluatorR27.Trace ch = look(champion, championTau, null);
            ReplayScoreR27 cs = ReplayScoreR27.of(SplitR27.HOLDOUT, ch, sp.holdoutFrom, sp.holdoutTo);
            h.put("championHoldout", cs.json()); sink.score(champion.policyHash(), ctx.data.datasetHash, cs);
            boolean vsPrev = true, vsI00 = false, vsNull = !cfg.nullControl;
            if (previousChampion != null) {
                ReplayEvaluatorR27.Trace pv = look(previousChampion, previousTau, null);
                StatsR27.Moments d = ReplayScoreR27.paired(ch, pv, sp.holdoutFrom, sp.holdoutTo);
                vsPrev = d.lcb(StatsR27.Z95_ONE_SIDED) > 0; h.put("pairedLcbVsPrevious", fin(d.lcb(StatsR27.Z95_ONE_SIDED)));
                h.put("previousHoldout", ReplayScoreR27.of(SplitR27.HOLDOUT, pv, sp.holdoutFrom, sp.holdoutTo).json());
            }
            if (i00Champion != null) {
                ReplayEvaluatorR27.Trace iv = look(i00Champion, i00Tau, null);
                StatsR27.Moments d = ReplayScoreR27.paired(ch, iv, sp.holdoutFrom, sp.holdoutTo);
                vsI00 = d.lcb(StatsR27.Z95_ONE_SIDED) > 0; h.put("pairedLcbVsI00", fin(d.lcb(StatsR27.Z95_ONE_SIDED)));
                h.put("i00Holdout", ReplayScoreR27.of(SplitR27.HOLDOUT, iv, sp.holdoutFrom, sp.holdoutTo).json());
            }
            if (cfg.nullControl && nullEvolution.champion != null) {
                ReplayEvaluatorR27.Trace nv = look(nullEvolution.champion, nullEvolution.championTau, nullEvolution.labels(ctx.cache, sp));
                StatsR27.Moments d = ReplayScoreR27.paired(ch, nv, sp.holdoutFrom, sp.holdoutTo);
                vsNull = d.lcb(StatsR27.Z95_ONE_SIDED) > 0; h.put("pairedLcbVsNull", fin(d.lcb(StatsR27.Z95_ONE_SIDED)));
                h.put("nullHoldout", ReplayScoreR27.of(SplitR27.HOLDOUT, nv, sp.holdoutFrom, sp.holdoutTo).json());
            } else if (cfg.nullControl) vsNull = false;
            verified = vsPrev && vsI00 && vsNull;
            why = verified ? "VERIFIED_IMPROVEMENT" : "UNVERIFIED:" + (vsPrev ? "" : "NOT_ABOVE_PREVIOUS;") + (vsI00 ? "" : "NOT_ABOVE_I00;") + (vsNull ? "" : "NOT_ABOVE_SHUFFLED_LABEL_CONTROL;");
        }
        h.put("verified", verified); h.put("reason", why); h.put("holdoutBlock", block); h.put("blockExposures", (long) ledger.exposures(block));
        genStats.put("holdout", h);
        if (verified) { count("verifiedImprovements", 1); generationsWithoutVerified = 0; } else generationsWithoutVerified++;
    }

    private ReplayEvaluatorR27.Trace look(PolicySpecR27 s, double tau, int[][] labels) {
        if (ledger.look(ctx.split.holdoutBlock, s.policyHash())) sink.holdoutLook(ctx.split.holdoutBlock, s.policyHash());
        // Shuffled-label control: its online state passes its own (shuffled) H_TRAIN/H_SELECT labels; H_HOLDOUT labels are real.
        int[][] l = labels == null ? ctx.labels : labels;
        return ReplayEvaluatorR27.run(s, ctx.scores(s), ctx.data, l, tau, ctx.split.trainFrom, ctx.split.holdoutTo);
    }

    void stepRecord() {
        @SuppressWarnings("unchecked") Map<String, Object> h = (Map<String, Object>) genStats.get("holdout");
        boolean verified = h != null && Boolean.TRUE.equals(h.get("verified"));
        if (champion != null && previousChampion != null && !champion.policyHash().equals(previousChampion.policyHash()))
            sink.lineage(champion.policyHash(), previousChampion.policyHash(), verified ? "CHAMPION_VERIFIED" : "CHAMPION_UNVERIFIED", "SUCCESSION", generation);
        Map<String, Object> rec = new LinkedHashMap<>(genStats);
        rec.put("generation", (long) generation); rec.put("startedAt", startedAt); rec.put("finishedAt", clock.now());
        Member ch = find(champion);
        rec.put("championHash", champion == null ? null : champion.policyHash()); rec.put("championSelectU", ch == null ? null : ch.u);
        rec.put("championSelectHr", ch == null ? null : fin(ch.hr)); rec.put("championIdea", champion == null || champion.meta == null ? null : champion.meta.idea);
        rec.put("nullChampionHash", nullEvolution.champion == null ? null : nullEvolution.champion.policyHash());
        rec.put("i00ChampionHash", i00Champion == null ? null : i00Champion.policyHash());
        Map<String, Object> cs = new LinkedHashMap<>(); for (String k : COUNTERS) cs.put(k, counters.get(k)[0]); rec.put("counters", cs);
        rec.put("population", (long) population.size()); rec.put("archiveCells", (long) archive.filled()); rec.put("live", (long) live.size());
        rec.put("diagnosis", diagnose());
        lastRecord = rec;
        sink.generation(rec);
    }

    void stepPromote() {
        @SuppressWarnings("unchecked") Map<String, Object> h = (Map<String, Object>) genStats.get("holdout");
        boolean verified = h != null && Boolean.TRUE.equals(h.get("verified"));
        if (verified && champion != null && !liveTau.containsKey(champion.policyHash())) {
            if (live.size() >= cfg.liveCap) { PolicySpecR27 out = live.remove(0); liveTau.remove(out.policyHash()); sink.demote(out.policyHash(), "REPLACED_BY_VERIFIED_CHAMPION_LIVE_CAP_32"); }
            live.add(champion); liveTau.put(champion.policyHash(), championTau);
            sink.promote(champion, championTau, generation, "VERIFIED_IMPROVEMENT_ON_HOLDOUT_BLOCK_" + ctx.split.holdoutBlock);
            count("prospectiveQualified", 1);
        }
    }

    void stepMigrateAblate() {
        if (generation > 0 && generation % cfg.migrateEvery == 0) {
            int moved = 0;
            for (String[] mv : IslandModelR27.migrations(population)) for (Member m : population) if (m.hash().equals(mv[0])) { m.island = mv[1]; moved++; }
            lastRecord.put("migrated", (long) moved);
        }
        if (champion != null && generation > 0 && generation % cfg.ablateEvery == 0) {
            List<Object[]> rows = new ArrayList<>(); List<PolicyAstR27.Node> nodes = PolicyAstR27.nodes(champion.ast);
            ReplayScoreR27 base = ReplayScoreR27.of(SplitR27.SELECT, ctx.selectTrace(champion, championTau), ctx.split.selectFrom, ctx.split.selectTo);
            for (int i = 0; i < nodes.size(); i++) {
                PolicyAstR27.Node n = nodes.get(i);
                if (n.kids.length < 2) continue;
                for (int k = 0; k < 2; k++) {
                    PolicySpecR27 ab = champion.withAst(PolicyAstR27.replace(champion.ast, i, n.kids[1 - k]), null);
                    try {
                        ReplayScoreR27 s = ReplayScoreR27.of(SplitR27.SELECT, ctx.selectTrace(ab, championTau), ctx.split.selectFrom, ctx.split.selectTo);
                        rows.add(new Object[] {(long) i, (long) k, PolicyAstR27.describe(n.kids[k]), s.pairedUMean - base.pairedUMean, s.logloss - base.logloss});
                    } catch (RuntimeException e) { rows.add(new Object[] {(long) i, (long) k, PolicyAstR27.describe(n.kids[k]), null, null}); }
                }
            }
            sink.ablation(generation, champion.policyHash(), rows);
        }
        // Stagnation responses (docs/05 5.8): temporary depth cap and operator re-weighting.
        if (depthCooldown > 0 && --depthCooldown == 0) { gateMaxDepth = BirthGateR27.MAX_DEPTH; gate.setMaxDepth(gateMaxDepth); }
    }

    // ------------------------------------------------------------------ helpers
    void refresh(Member m) {
        if (m.trace == null) m.trace = ctx.selectTrace(m.spec, m.tau);
        ReplayScoreR27 s = ReplayScoreR27.of(SplitR27.SELECT, m.trace, ctx.split.selectFrom, ctx.split.selectTo);
        m.u = s.pairedUMean; m.lcb = s.pairedULcb; m.hr = s.hitRate(); m.actRate = s.actRate(); m.brier = s.brier;
        m.cell = cellOf(m);
    }
    int cellOf(Member m) {
        int[] champSig = champion == null ? null : gate.signatureOf(champion.policyHash());
        return EliteArchiveR27.cell(m.actRate, PolicyAstR27.sourceClass(m.spec.ast), champSig == null || m.sig == null ? 0 : BirthGateR27.jaccard(m.sig, champSig));
    }
    Member find(PolicySpecR27 s) { if (s == null) return null; for (Member m : population) if (m.hash().equals(s.policyHash())) return m; return null; }
    static String island(PolicySpecR27 s) {
        String idea = s.meta == null ? "I09" : s.meta.idea;
        if (idea.startsWith("EXPERT")) return "I10";
        return IslandModelR27.index(idea) == 8 && !idea.equals("I09") ? "I09" : idea;
    }
    static IdeaFamilyR27 family(String id) { for (IdeaFamilyR27 f : FAMILIES) if (f.id().equals(id)) return f; return null; }
    static Object fin(double d) { return Double.isFinite(d) ? (Object) d : null; }

    /** Relative H_SELECT log-loss increase of a pruned program (M_PRUNE contribution measurement). */
    double pruneContribution(PolicySpecR27 pruned, int node) {
        Member ch = find(champion);
        try {
            double tau = ctx.temperature(pruned);
            ReplayScoreR27 s = ReplayScoreR27.of(SplitR27.SELECT, ctx.selectTrace(pruned, tau), ctx.split.selectFrom, ctx.split.selectTo);
            double base = ch == null || ch.trace == null ? s.logloss : ReplayScoreR27.of(SplitR27.SELECT, ch.trace, ctx.split.selectFrom, ctx.split.selectTo).logloss;
            return (s.logloss - base) / Math.max(1e-9, Math.abs(base));
        } catch (RuntimeException e) { return Double.POSITIVE_INFINITY; }
    }

    /** I10 failure cluster: the regime tercile where the champion's H_SELECT hit rate is lowest. */
    String failureCluster() {
        Member ch = find(champion);
        if (ch == null || ch.trace == null) return "";
        double[] reg = ctx.cache.leaf("regime", 0, 0, 128); int[] acts = new int[2], hits = new int[2];
        for (int t = ctx.split.selectFrom; t < ctx.split.selectTo; t++) {
            if (ch.trace.skipped[t] || !ch.trace.act[t] || Double.isNaN(reg[t * 10])) continue;
            int b = reg[t * 10] > 0.1 ? 1 : 0; acts[b]++; hits[b] += ch.trace.hit[t];
        }
        if (acts[0] < 10 || acts[1] < 10) return "";
        return hits[0] / (double) acts[0] <= hits[1] / (double) acts[1] ? "REGIME_LOW" : "REGIME_HIGH";
    }

    /** docs/05 5.8 automatic stagnation diagnosis; applies the listed automatic responses. */
    List<Object> diagnose() {
        List<Object> out = new ArrayList<>();
        long births = num(genStats.get("births")), rej = num(genStats.get("rejected")), noop = num(genStats.get("noOp"));
        if (births > 0 && rej * 2 > births) out.add("出生拒绝率>50%：按拒绝原因告警 " + genStats.get("rejectReasons"));
        if (births > 0 && noop * 10 > births * 3) { out.add("NO_OP比例>30%：降低M_CONST/M_WINDOW权重"); opWeights[0] = Math.max(0.2, opWeights[0] * 0.7); opWeights[1] = Math.max(0.2, opWeights[1] * 0.7); }
        if (generationsWithoutVerified >= 20) {
            out.add("进化停滞：最近" + generationsWithoutVerified + "代没有在留出数据上被证实的提升");
            Member ch = find(champion);
            @SuppressWarnings("unchecked") Map<String, Object> h = (Map<String, Object>) genStats.get("holdout");
            if (ch != null && h != null) out.add("可能原因：拟合噪声/数据无可学信号（训练段与打乱标签对照同样上升而留出无提升）；自动将复杂度上限6→4一个周期做对照");
            if (depthCooldown == 0) { gateMaxDepth = 4; gate.setMaxDepth(4); depthCooldown = 10; }
        }
        List<int[]> champs = new ArrayList<>();
        for (String isl : IslandModelR27.ISLANDS) { Member best = null; for (Member m : population) if (m.island.equals(isl) && (best == null || m.u > best.u)) best = m; if (best != null && best.sig != null) champs.add(best.sig); }
        double sim = IslandModelR27.championSimilarity(champs);
        if (Double.isFinite(sim) && sim > 0.9) out.add("多样性坍缩：岛冠军行为相似度" + sim + ">0.9");
        long replayMs = num(genStats.get("replayMs")), wall = clock.now() - startedAt;
        if (wall > 30 * 60_000L && replayMs * 5 > wall * 4) out.add("算力不足：回放耗时占比>80%且每代>30分钟；建议减少每代出生数");
        lastRecord.put("islandChampionSimilarity", fin(sim));
        return out;
    }
    static long num(Object o) { return o instanceof Number ? ((Number) o).longValue() : 0; }

    // ------------------------------------------------------------------ persistence of the loop itself (r27_evolution_cursor)
    public String snapshot() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("generation", (long) generation); s.put("step", (long) step); s.put("rng", rng.state); s.put("datasetHash", ctx.data.datasetHash);
        s.put("champion", champion == null ? null : champion.fullJson()); s.put("championTau", championTau);
        s.put("previous", previousChampion == null ? null : previousChampion.fullJson()); s.put("previousTau", previousTau);
        s.put("i00", i00Champion == null ? null : i00Champion.fullJson()); s.put("i00Tau", i00Tau);
        s.put("nullChampion", nullEvolution.champion == null ? null : nullEvolution.champion.fullJson()); s.put("nullTau", nullEvolution.championTau); s.put("nullSeed", nullEvolution.lastSeed);
        List<Object> nullPop = new ArrayList<>(); for (SuccessiveHalvingR27.Candidate c : nullEvolution.population) { Map<String, Object> m = new LinkedHashMap<>(); m.put("spec", c.spec.fullJson()); m.put("tau", c.temperature); nullPop.add(m); }
        s.put("nullPopulation", nullPop);
        List<Object> pop = new ArrayList<>();
        for (Member m : population) { Map<String, Object> x = new LinkedHashMap<>(); x.put("spec", m.spec.fullJson()); x.put("tau", m.tau); x.put("island", m.island); pop.add(x); }
        s.put("population", pop);
        List<Object> arc = new ArrayList<>();
        for (Map.Entry<Integer, EliteArchiveR27.Elite> e : archive.snapshot().entrySet()) { Map<String, Object> x = new LinkedHashMap<>(); x.put("cell", (long) e.getKey()); x.put("spec", e.getValue().spec.fullJson()); x.put("u", e.getValue().u); x.put("tau", e.getValue().tau); arc.add(x); }
        s.put("archive", arc);
        List<Object> pend = new ArrayList<>();
        for (Pending p : pending) { Map<String, Object> x = new LinkedHashMap<>(); x.put("spec", p.spec.fullJson()); x.put("state", p.state); x.put("reason", p.reason); x.put("tau", fin(p.tau)); x.put("structural", p.structural); pend.add(x); }
        s.put("pending", pend);
        List<Object> lv = new ArrayList<>(); for (PolicySpecR27 p : live) { Map<String, Object> x = new LinkedHashMap<>(); x.put("spec", p.fullJson()); x.put("tau", liveTau.get(p.policyHash())); lv.add(x); }
        s.put("live", lv);
        Map<String, Object> cs = new LinkedHashMap<>(); for (String k : COUNTERS) cs.put(k, counters.get(k)[0]); s.put("counters", cs);
        Map<String, Object> fs = new LinkedHashMap<>(); for (Map.Entry<String, Integer> e : familySurvivors.entrySet()) fs.put(e.getKey(), (long) e.getValue()); s.put("familySurvivors", fs);
        s.put("opWeights", opWeights); s.put("gateMaxDepth", (long) gateMaxDepth); s.put("depthCooldown", (long) depthCooldown); s.put("noVerified", (long) generationsWithoutVerified);
        Map<String, Object> lg = new LinkedHashMap<>(); for (Map.Entry<Long, Set<String>> e : ledger.snapshot().entrySet()) lg.put(Long.toString(e.getKey()), new ArrayList<Object>(e.getValue())); s.put("ledger", lg);
        s.put("genStats", genStats); s.put("startedAt", startedAt);
        return JsonR27.write(s);
    }

    @SuppressWarnings("unchecked")
    public static EvolutionLoopR27 restore(String json, Config cfg, FeatureCacheR27 cache, Sink sink, Clock clock) {
        Map<String, Object> s = (Map<String, Object>) JsonR27.parse(json);
        EvolutionLoopR27 l = new EvolutionLoopR27(cfg, cache, sink, clock);
        l.generation = (int) JsonR27.num(s.get("generation")); l.step = (int) JsonR27.num(s.get("step")); l.rng.state = ((Number) s.get("rng")).longValue();
        for (Map.Entry<String, Object> e : ((Map<String, Object>) s.get("ledger")).entrySet()) for (Object h : (List<Object>) e.getValue()) l.ledger.restore(Long.parseLong(e.getKey()), (String) h);
        l.bind(cache);
        if (!cache.data.datasetHash.equals(s.get("datasetHash")) && l.step != 1) throw new IllegalStateException("R27_RESTORE_DATASET_MISMATCH_MID_GENERATION");
        l.champion = spec(s.get("champion")); l.championTau = JsonR27.num(s.get("championTau"));
        l.previousChampion = spec(s.get("previous")); l.previousTau = JsonR27.num(s.get("previousTau"));
        l.i00Champion = spec(s.get("i00")); l.i00Tau = JsonR27.num(s.get("i00Tau"));
        l.nullEvolution.champion = spec(s.get("nullChampion")); l.nullEvolution.championTau = JsonR27.num(s.get("nullTau")); l.nullEvolution.lastSeed = ((Number) s.get("nullSeed")).longValue();
        for (Object o : (List<Object>) s.get("nullPopulation")) { Map<String, Object> m = (Map<String, Object>) o; PolicySpecR27 sp = spec(m.get("spec")); l.nullEvolution.population.add(new SuccessiveHalvingR27.Candidate(sp, null, JsonR27.num(m.get("tau")))); }
        for (Object o : (List<Object>) s.get("population")) {
            Map<String, Object> x = (Map<String, Object>) o; Member m = new Member(); m.spec = spec(x.get("spec")); m.tau = JsonR27.num(x.get("tau")); m.island = (String) x.get("island");
            // Behavior signatures are not persisted (size); they are recomputed deterministically on restore.
            BirthGateR27.Verdict v = l.gate.check(m.spec); m.sig = v.ok ? v.signature : new int[0];
            l.population.add(m); l.gate.register(m.hash(), m.sig);
        }
        for (Member m : l.population) l.refresh(m);
        for (Object o : (List<Object>) s.get("archive")) { Map<String, Object> x = (Map<String, Object>) o; l.archive.put((int) JsonR27.num(x.get("cell")), new EliteArchiveR27.Elite(spec(x.get("spec")), JsonR27.num(x.get("u")), JsonR27.num(x.get("tau")))); }
        for (Object o : (List<Object>) s.get("pending")) {
            Map<String, Object> x = (Map<String, Object>) o; Pending p = new Pending(); p.spec = spec(x.get("spec")); p.state = (String) x.get("state"); p.reason = (String) x.get("reason");
            if ("BORN".equals(p.state)) { BirthGateR27.Verdict v = l.gate.check(p.spec); p.sig = v.ok ? v.signature : new int[0]; }
            p.tau = x.get("tau") == null ? Double.NaN : JsonR27.num(x.get("tau")); p.structural = (Boolean) x.get("structural"); l.pending.add(p);
        }
        for (Object o : (List<Object>) s.get("live")) { Map<String, Object> x = (Map<String, Object>) o; PolicySpecR27 sp = spec(x.get("spec")); l.live.add(sp); l.liveTau.put(sp.policyHash(), JsonR27.num(x.get("tau"))); }
        for (Map.Entry<String, Object> e : ((Map<String, Object>) s.get("counters")).entrySet()) l.counters.get(e.getKey())[0] = ((Number) e.getValue()).longValue();
        for (Map.Entry<String, Object> e : ((Map<String, Object>) s.get("familySurvivors")).entrySet()) l.familySurvivors.put(e.getKey(), (int) JsonR27.num(e.getValue()));
        List<Object> w = (List<Object>) s.get("opWeights"); for (int i = 0; i < w.size(); i++) l.opWeights[i] = JsonR27.num(w.get(i));
        l.gateMaxDepth = (int) JsonR27.num(s.get("gateMaxDepth")); l.gate.setMaxDepth(l.gateMaxDepth); l.depthCooldown = (int) JsonR27.num(s.get("depthCooldown")); l.generationsWithoutVerified = (int) JsonR27.num(s.get("noVerified"));
        l.genStats.putAll((Map<String, Object>) s.get("genStats")); l.startedAt = ((Number) s.get("startedAt")).longValue();
        return l;
    }
    static PolicySpecR27 spec(Object o) { return o == null ? null : PolicySpecR27.parse((String) o); }
    @SuppressWarnings("unchecked") static int[] ints(Object o) { if (o == null) return null; List<Object> l = (List<Object>) o; int[] a = new int[l.size()]; for (int i = 0; i < a.length; i++) a[i] = (int) JsonR27.num(l.get(i)); return a; }
}
